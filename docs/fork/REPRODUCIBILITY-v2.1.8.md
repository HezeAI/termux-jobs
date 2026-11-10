# Release reproducibility — v2.1.8

Separate local release reproduction is being prepared. No CI reproduction or third-party
security audit is claimed. Results and exact source identity will be filled before publication.

## Repeatable procedure

Use the existing local ARM64 toolchain documented for [v2.1.7](REPRODUCIBILITY-v2.1.7.md).
Run from the canonical checkout, with an absent task-specific reproduction source directory:

1. Run `python3 scripts/release-reproduction-v2.1.8/prepare.py` using tag v2.1.8.
   During pre-publication validation, set REPRO_SOURCE_REF to the recorded source commit.
2. Run `python3 scripts/release-reproduction-v2.1.8/rebuild-glycin.py`.
3. Copy its sibling unsigned.init.gradle into `.verification/release-reproduction-v2.1.8/`.
4. Run `python3 scripts/release-reproduction-v2.1.8/build.py`.
5. Run compare.py and compare-container.py with the final signed APK, unsigned
   reproduction APK and separate output JSON paths. Never alter the signed APK.

The scripts archive exact sources, rebuild our JNI and glycin code and use pinned external
binaries. The external Xlorie library, runtime packages and bootstrap packages are not
rebuilt from source. The unsigned variant preserves release settings without a signing key;
production signing guards remain enabled. Compare every ZIP entry and the container after
removing only the validated signing block and adjusting the ZIP directory offset.
