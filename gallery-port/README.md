# Gallery compatibility testing

This directory is isolated from the Highway Rush and Mera Hisaab apps.
The candidate is a personal-use Xiaomi Gallery compatibility build for Android 13.
It remains unverified until the runtime workflow passes.

The compressed candidate is stored in payload parts. `restore.py` reconstructs
the APK and verifies both compressed and APK SHA-256 hashes. No signing keys are included.

The workflow tests installation, home startup, rendering two generated local photos,
opening a photo, album navigation, and stability on an Android 13 emulator.
Passing this test does not verify every Gallery feature or the physical Nubia firmware.
