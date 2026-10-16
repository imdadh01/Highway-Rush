import hashlib,json,lzma
from pathlib import Path
root=Path(__file__).parent
m=json.loads((root/'candidate.json').read_text())
payload=b''.join(p.read_bytes() for p in sorted((root/'payload').glob('part-*.bin')))
assert len(payload)==m['payload_size']
assert hashlib.sha256(payload).hexdigest()==m['payload_sha256']
apk=lzma.decompress(payload)
assert len(apk)==m['file_size'] and hashlib.sha256(apk).hexdigest()==m['sha256']
(root/'Gallery-Nubia.apk').write_bytes(apk)
print('Candidate restored and SHA-256 verified')
