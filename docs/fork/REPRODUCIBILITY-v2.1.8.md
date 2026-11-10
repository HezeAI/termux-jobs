# Release reproducibility — v2.1.8

Checked locally on 2026-11-10, Ubuntu/PRoot ARM64. This is source/build
reproduction, not third-party verification or an independent security audit.

## Result

All **1,141 ZIP entries** match byte-for-byte. No entry was excluded, added or removed.
The complete unsigned container also matches after removing only the validated
8,192-byte APK signing block and adjusting the ZIP central-directory offset in memory.
The signed release APK was not modified. A signing key was never copied into the
reproduction source/build tree. Full signed-APK byte reproduction is not claimed.

[Comparison](reproduction-v2.1.8/comparison.json) ·
[Container comparison](reproduction-v2.1.8/container-comparison.json) ·
[Every payload SHA-256](reproduction-v2.1.8/payload-sha256.json).

Signed APK SHA-256: `3a2198eb0bf6f69d5f00d63cba65229042aee2b0189013f4d473ee712cc344a7`.
Unsigned APK SHA-256: `4c0fc5086e76b4ea45ce86be24b835833eecee92d7b40cc51c9b9dcc49a0c0de`.

Exact source snapshot: `0468c0b4`, recorded in full in
[source identity](reproduction-v2.1.8/source-identity.json). Later release commits
only add validation/documentation; the tag must retain identical production inputs.
The source tree and project outputs were clean; Gradle used --no-build-cache and
--rerun-tasks. Downloaded dependency caches and existing tools were reused, not prior
project object files, libraries, classes or DEX. SDK files were exposed read-only by
local path convention; this was isolated source/output storage, not a separate machine.

Our JNI libraries and Alpine/Fedora shims were rebuilt. The Fedora glycin shim uses
the documented historical GCC recipe; its SHA-256 was verified before staging.
External Xlorie, codec libraries, keyboard data and bootstrap packages were pinned
binary inputs, not rebuilt from their upstream sources. No claim of their source
reproducibility is made. See [external libraries](reproduction-v2.1.8/external-libraries.json),
[X11 input hashes](reproduction-v2.1.8/x11-preparation.json),
[resolved dependencies](reproduction-v2.1.8/resolved-dependencies.json),
[toolchain hashes](reproduction-v2.1.8/toolchain.json),
[native recipe](reproduction-v2.1.8/native-commands.json) and
[GCC recipe](reproduction-v2.1.8/glycin-historical-recipe.json).
Tools: Gradle 7.2, AGP 4.2.2, local OpenJDK 11, Clang/LLD 21.1.8,
GCC 15 and existing Android 35 platform/NDK 28.1 sysroot for the JNI recipe.
The exact resolved paths and hashes above take precedence over tool-family names.

All 112 reproduction tasks executed. No source/content mismatch occurred.
SDK metadata/platform-tools warnings remain in the local build log; no emulator/device
execution was part of this comparison. Debug CI is separate; CI release reproduction
is not implemented.

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
