"""Check DEX class-data method lists, including the reported ART rejection.

This is a structural regression check, not a replacement for Android ART.
"""
import argparse
import hashlib
import json
import struct
import zipfile
import zlib


def check_dex(data):
    errors, count = [], 0
    u32 = lambda offset: struct.unpack_from('<I', data, offset)[0]

    def leb(offset):
        value = shift = 0
        for _ in range(5):
            byte = data[offset]
            offset += 1
            value |= (byte & 127) << shift
            if not byte & 128:
                return value, offset
            shift += 7
        raise ValueError('Invalid ULEB128')

    assert data[:4] == b'dex\n'
    assert u32(32) == len(data)
    assert u32(8) == zlib.adler32(data[12:]) & 0xffffffff
    assert data[12:32] == hashlib.sha1(data[32:]).digest()
    strings = []
    for index in range(u32(56)):
        offset = u32(u32(60) + index * 4)
        _, start = leb(offset)
        strings.append(data[start:data.index(b'\0', start)].decode('utf-8', errors='replace'))
    types = [strings[u32(u32(68) + index * 4)] for index in range(u32(64))]
    methods = []
    for index in range(u32(88)):
        owner, proto, name = struct.unpack_from('<HHI', data, u32(92) + index * 8)
        methods.append((types[owner], strings[name], proto))
    constructors = 0
    for index in range(u32(96)):
        definition = struct.unpack_from('<8I', data, u32(100) + index * 32)
        owner = types[definition[0]]
        offset = definition[6]
        if not offset:
            continue
        sizes = []
        for _ in range(4):
            size, offset = leb(offset)
            sizes.append(size)
        for is_static, size in zip((True, False), sizes[:2]):
            for _ in range(size):
                _, offset = leb(offset)
                flags, offset = leb(offset)
                if bool(flags & 8) != is_static:
                    errors.append(f'{owner}: field is in wrong static/instance list')
        seen = set()
        for direct, size in zip((True, False), sizes[2:]):
            method_index = 0
            for position in range(size):
                delta, offset = leb(offset)
                method_index += delta
                flags, offset = leb(offset)
                code_offset, offset = leb(offset)
                method_owner, name, proto = methods[method_index]
                label = f'{method_owner}->{name}'
                count += 1
                if position and delta == 0 or method_index in seen:
                    errors.append(f'{label}: duplicate/unsorted method')
                seen.add(method_index)
                if method_owner != owner:
                    errors.append(f'{label}: method belongs to another class')
                if bool(flags & (0x2 | 0x8 | 0x10000)) != direct:
                    errors.append(f'{label}: wrong direct/virtual method list')
                constructor_name = name in ('<init>', '<clinit>')
                if bool(flags & 0x10000) != constructor_name:
                    errors.append(f'{label}: constructor access flag/name mismatch')
                if constructor_name:
                    constructors += 1
                    if bool(flags & 8) != (name == '<clinit>'):
                        errors.append(f'{label}: incorrect static constructor flag')
                    return_type = u32(u32(76) + proto * 12 + 4)
                    if types[return_type] != 'V':
                        errors.append(f'{label}: constructor must return void')
                if bool(flags & (0x100 | 0x400)) == bool(code_offset):
                    errors.append(f'{label}: native/abstract/code offset mismatch')
    return {'methods': count, 'constructors': constructors, 'errors': errors}


def check_apk(path):
    results = {}
    with zipfile.ZipFile(path) as archive:
        assert archive.testzip() is None
        for name in archive.namelist():
            if name.startswith('classes') and name.endswith('.dex') and '/' not in name:
                results[name] = check_dex(archive.read(name))
    assert results, 'No DEX files'
    return results


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('apk')
    parser.add_argument('--json')
    args = parser.parse_args()
    results = check_apk(args.apk)
    if args.json:
        from pathlib import Path
        Path(args.json).write_text(json.dumps(results, indent=2))
    for name, result in results.items():
        print(name, result['methods'], 'methods,', result['constructors'],
              'constructors,', len(result['errors']), 'errors')
        for error in result['errors'][:6]:
            print('  ', error)
    raise SystemExit(int(any(item['errors'] for item in results.values())))
