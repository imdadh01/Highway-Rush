#!/usr/bin/env bash
# Dependency-free release build using the installed Android SDK (no tests).
set -euo pipefail
project_dir=$(cd "$(dirname "$0")/.." && pwd)
sdk_dir=${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}
key_dir=${1:?Pass the private signing directory}
version_code=${APP_VERSION_CODE:-1}
version_name=${APP_VERSION_NAME:-1.0.0}
bt="$sdk_dir/build-tools/35.0.0"
platform="$sdk_dir/platforms/android-35/android.jar"
build_dir="$project_dir/app/build/direct"
mkdir -p "$build_dir/classes" "$build_dir/dex" "$build_dir/gen" "$project_dir/app/build/outputs/apk/release"
python3 - "$project_dir/app/src/main/AndroidManifest.xml" "$build_dir/AndroidManifest.xml" <<'PY'
import sys
from pathlib import Path
s=Path(sys.argv[1]).read_text().replace('<manifest ', '<manifest package="com.imdadh.merahisaab" ',1)
Path(sys.argv[2]).write_text(s)
PY
"$bt/aapt2" compile --dir "$project_dir/app/src/main/res" -o "$build_dir/resources.zip"
"$bt/aapt2" link -I "$platform" --manifest "$build_dir/AndroidManifest.xml" --java "$build_dir/gen" --min-sdk-version 26 --target-sdk-version 35 --version-code "$version_code" --version-name "$version_name" -A "$project_dir/app/src/main/assets" -o "$build_dir/unsigned.apk" "$build_dir/resources.zip"
java com.sun.tools.javac.Main -source 8 -target 8 -classpath "$platform" -d "$build_dir/classes" "$project_dir/app/src/main/java/com/imdadh/merahisaab/MainActivity.java"
java sun.tools.jar.Main cf "$build_dir/classes.jar" -C "$build_dir/classes" .
java -cp "$bt/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$platform" --output "$build_dir/dex" "$build_dir/classes.jar"
python3 - "$build_dir" <<'PY'
import sys,zipfile
from pathlib import Path
p=Path(sys.argv[1])
with zipfile.ZipFile(p/'unsigned.apk','a',compression=zipfile.ZIP_DEFLATED) as z:
 for d in (p/'dex').glob('*.dex'):z.write(d,d.name)
PY
"$bt/zipalign" -f -p 4 "$build_dir/unsigned.apk" "$build_dir/aligned.apk"
java -jar "$bt/lib/apksigner.jar" sign --ks "$key_dir/mera-hisaab-release.jks" --ks-key-alias mera-hisaab --ks-pass "file:$key_dir/key-password.txt" --out "$project_dir/app/build/outputs/apk/release/app-release.apk" "$build_dir/aligned.apk"
printf '%s\n' 'Signed release APK built. No tests run.'
