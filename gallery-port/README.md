# Gallery port for nubia Neo 2 (Android 13)

Gallery files and workflow are isolated here from the repository's other apps.

## v4 correction

The device rejected v3 before starting the application:
`Direct/virtual method ... MiCloudSettings;.<init> not in expected list 0`.
All 33 generated constructors in 31 compatibility classes lacked the DEX
constructor access flag. Correcting the declarations places them in the direct
method list. The generator now emits the flag as well.

The structural check reports 33 constructor errors for v3 and zero for the
unmodified original and corrected v4. CI also loads the candidate's DEX in the
real Android 13 runtime and links GalleryApp, FeatureParser, MiCloudSettings and
HomePageActivity. This test does not initialize Gallery or test the UI or native
ARM64 features. The available x86_64 emulator cannot perform the full ARM64
Gallery smoke test; `tests/smoke.py` remains available for a compatible device.

The original compressed candidate payload is retained as an immutable base.
`restore.py` applies a SHA-256 checked binary delta to reconstruct signed v4.
No signing private key is published. Device testing is still required.
