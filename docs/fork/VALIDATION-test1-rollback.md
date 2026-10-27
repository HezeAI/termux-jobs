# Private test1 rollback validation — 2026-10-27

The developer cancelled the Local AI experiment and requested the original
chat-link test1 with a higher versionCode. Base: `49e4f070`. Only application
change: code1418 → code1425; versionName remains2.0.10-test1. This can update
private test7/code1424 with the same com.termux package and signing certificate.

The Local AI runtime, catalogue, UI, JNI and DSP kernels are absent from this
source and must be absent from the final APK. No local models are launched for
rollback verification. Experimental model files/history already downloaded are
not deleted or migrated. The schema remains8. Current experimental model profiles
were serialized as plain termux profiles with empty shell startup and optional
localRuntime metadata: the original reader ignores those fields. They can appear
as ordinary empty Termux profiles after rollback. Agent profiles, connection
commands, aliases, notes and chats keep their existing storage mechanisms.

Verification results and final artifact hash are recorded below when complete.
The original test1 device behavior and limits are documented in
[the historical report](VALIDATION-chat-links-test1.md). No exhaustive on-device
rollback/data-preservation claim is made from source tests alone.

## Completed checks

- 237 JVM cases in26 classes: zero failures/errors/skips. Includes schema8
  test7 optional-metadata compatibility, existing agent/link/alias/backup coverage.
- Five-module modern lint7.4.2: zero errors,220 existing warnings.
- Isolated release build and runtime definition checks passed.
- Final ZIP, package/version/ABI, signature, source asset and secret-pattern
  checks passed. Only3 original JNI libraries remain; no model/DSP payloads.
- Original test1 and rollback APK have the exact same ZIP entry set. Every asset,
  resource and JNI file is byte-identical. Only AndroidManifest.xml and classes2.dex
  differ; four DEX payload bytes encode code1418 → code1425 in constants/build
  metadata (DEX checksum/signature also change).

Final artifact: `build/apk/Termux-Jobs-test1-rollback.apk`, **39800325bytes**.
SHA-256: `c265f45b329d0e962f6a52eb8bb1150fb22861560382cc3689e9ad520b3ddc61`.
Certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Packagecom.termux, versionName2.0.10-test1, code1425, ARM64,
min26/target28/compile35. The package/signing identity matches test7 and stable.
Actual installation and device rollback acceptance remain for the developer.
No APK was published. Existing downloaded model files were not removed.
