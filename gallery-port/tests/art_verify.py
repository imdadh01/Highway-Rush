"""Exercise real Android 13 DEX loading without requiring ARM native libraries."""
import json
from pathlib import Path
import subprocess
import sys

out = Path('test-results')
out.mkdir(exist_ok=True)
device_path = '/data/local/tmp/gallery-candidate.apk'
subprocess.run(['adb', 'wait-for-device'], check=True, timeout=90)
subprocess.run(['adb', 'push', sys.argv[1], device_path], check=True, timeout=90)
subprocess.run(['adb', 'shell', 'chmod', '444', device_path], check=True)
result = subprocess.run(['adb', 'shell', 'CLASSPATH=' + device_path, 'app_process',
                         '/system/bin', 'com.miui.gallery.compat.DexCheck'],
                        text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                        timeout=90)
(out / 'art-dex-loading.txt').write_text(result.stdout)
print(result.stdout)
passed = result.returncode == 0 and result.stdout.count('PASS: ART linked ') == 4
(out / 'dex-result.json').write_text(json.dumps({
    'passed': passed, 'android_api': 33, 'test': 'DEX loading and critical class linking',
    'full_gallery_ui_tested': False, 'returncode': result.returncode,
}, indent=2))
assert passed, 'Android ART rejected the candidate; inspect art-dex-loading.txt'
