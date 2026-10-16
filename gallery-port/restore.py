import hashlib
import json
import lzma
from pathlib import Path
import bsdiff4

root = Path(__file__).parent
meta = json.loads((root / 'candidate.json').read_text())
payload = b''.join(p.read_bytes() for p in sorted((root / 'payload').glob('part-*.bin')))
assert len(payload) == meta['payload_size']
assert hashlib.sha256(payload).hexdigest() == meta['payload_sha256']
base = lzma.decompress(payload)
assert hashlib.sha256(base).hexdigest() == meta['base_sha256']
patch = b''.join((root / part).read_bytes() for part in meta['patches'])
assert hashlib.sha256(patch).hexdigest() == meta['patch_sha256']
apk = bsdiff4.patch(base, patch)
assert len(apk) == meta['file_size']
assert hashlib.sha256(apk).hexdigest() == meta['sha256']
(root / 'Gallery-Nubia.apk').write_bytes(apk)
print('v4 candidate restored and SHA-256 verified')
