# Release APK/source verification — v2.1.7

Verification date: 2026-11-08. This is a build/content comparison, **not an
independent security audit**. It does not amend the original release validation.
The published APK, tag, update metadata and Components were not replaced.

## Result

**All 1,140 ZIP entries match byte-for-byte**, including every DEX, Android
manifest, resource, asset, license and native library. No entries were excluded,
added or missing. [Final comparison](reproduction-v2.1.7/final-comparison.json) ·
[all payload hashes](reproduction-v2.1.7/payload-sha256.json).

The verification APK is unsigned: 40,667,025 bytes, SHA-256
`177d9a1c2d2c33faf9f2a2f8eb6f9a744125012378f0f5fbb0347f86b43d789f`.
Removing **only** the validated 8,192-byte APK Signing Block from the published
APK in memory, then correcting the ZIP central-directory offset, yields exactly
these bytes. No ZIP entries were rewritten. [Container comparison](reproduction-v2.1.7/container-comparison.json).
This establishes reproducibility of the unsigned container with the documented
historical native recipe. The full **signed** APK was not independently reproduced;
its hash differs because the verification build has no production signature.
The original signature was independently verified with the public certificate.
A fresh anonymous [public download](reproduction-v2.1.7/public-download.json)
returned HTTP 200 and the published size/SHA-256.

This establishes source/payload correspondence under the recorded toolchain and
prebuilt-input assumptions. It does not prove the upstream libraries' source
correspondence or general security. The tag's default Clang shim recipe alone does
not reproduce the historical GCC shim; that limitation is detailed below.

## Scope and identities

- Published [v2.1.7](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.7),
  `com.termux`, **2.1.7 /1536**, ARM64, non-debuggable.
- Exact source commit: `f3f57668fca31302062bf46f3be6a20d885ede1d`.
- Published APK: 40,675,217 bytes; SHA-256
  `f321c19652d80dd91cf96a46e4fcefffd1f309faebb61e3cc64ab4f0fe5bbebb`.
- Published signing certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
  [Fresh signature verification](reproduction-v2.1.7/published-signature.txt).

The existing GitHub CI builds debug APKs and runs tests. It does not establish
release reproducibility. The original local release used native payloads from an
older APK after source/hash identity checks. That was reuse, not an independent
native rebuild. This verification closes that specific evidence gap.

## Environment and separation

A fresh `git archive` of the exact commit supplied all project sources. New task
source/output directories, Gradle user home, Java user home and temporary directory
were used. No prior application classes, DEX or own native objects were build
inputs. The pinned external X11 APK and historical keyboard-data APK were used
only for their declared third-party inputs. Existing pinned raw downloads, public Maven dependency cache and installed
toolchain were reused. Gradle used the dependency cache read-only and ran offline
with `--no-build-cache --rerun-tasks`. This is a separate local build on the same
Ubuntu/PRoot ARM64 host, **not a new VM, clean OS or independent third-party run**.

No production keystore was copied or opened by this build. Its environment omitted
account/signing variables. The archived source and staging directory had no
`.signing` directory. No APK was installed and no working session was restarted.

Actual inputs: [source identity](reproduction-v2.1.7/source-identity.json),
[toolchain hashes](reproduction-v2.1.7/toolchain.json),
[resolved dependency hashes](reproduction-v2.1.7/resolved-dependencies.json).
The local JNI rebuild used Clang/LLD 21.1.8 with the existing NDK **28.1.13356709
sysroot**, Android target API 24. This is not the CI's NDK 22.1 toolchain.
Java used local JDK 11, Gradle 7.2, the tag's Android plugin/dependencies and the
same ARM64 staging script/build-tools/AAPT2 setup as the original local release.
SDK paths were reused for reads, not mounted with OS-enforced read-only isolation.

## Own native code

All four libraries were rebuilt from the **v2.1.7 source**, not extracted from the
published APK. The published APK served only as the comparison target.

| Library | Result and build |
| --- | --- |
| `libtermux.so` | Byte-identical; Clang/LLD, Android API 24 |
| `libtermux-bootstrap.so` | Byte-identical; fresh C/assembly build with the exact pinned bootstrap ZIP |
| `x11-alpine-compat.so` | Byte-identical; tag's Clang/musl preparation recipe |
| `x11-fedora-glycin.so` | Byte-identical with the historical GCC recipe; tag's newer Clang recipe differs |

[Native commands](reproduction-v2.1.7/native-commands.json).
[First native comparison](reproduction-v2.1.7/native-comparison.json) records the
Clang mismatch rather than concealing it. Published glycin is 67,656 bytes,
SHA-256 `1caad510d1f79e0d7823621191c294b64f3e105a228ccc23e7a1ab5631ddac61`.
The tag's Clang recipe produces 3,624 bytes,
`10b2f2a8daf295eb20d5bf51cde4bdde15250b33d740339ccff35337efdb9fb9`.
The difference includes executable sections, not just timestamps or padding.

The published ELF `.comment` names GCC 15.2.0. The historical preparation recipe
before commit `36e9f5a31e6f7763f621b967ac1b2e0e23500ede` used `cc -fPIC -shared
-nostdlib -O2 -Wall -Wextra -Werror`, hardening linker flags and the same SONAME.
Reapplying that recipe to **the tag's current C source**, with Ubuntu GCC
15.2.0-16ubuntu1 and GNU ld 2.46, reproduced the published bytes exactly.
[Recorded command/result](reproduction-v2.1.7/glycin-gcc-comparison.json).
Thus the tag's default fresh preparation and the release's reused historical shim
are different build paths. Running the tag's preparation unmodified does **not**
reproduce that published shim. No change to the published source/tag is implied.

## External ready-made libraries

[Complete external library inventory](reproduction-v2.1.7/external-libraries.json)
records source URLs, exact versions/commit, archive size/SHA-256, extracted library
SHA-256 and comparison with the APK.

- `libXlorie.so`: Termux:X11 commit `fa3a8b430e2896a19f44c99a9cb056254615ae06`,
  extracted from the pinned APK in Components. Its upstream commit marker and
  archive pin were checked by the tag's preparation script.
- `x11-openh264.so`: Debian `libopenh264-8`, `2.6.0+dfsg-2`, ARM64.
- `x11-gst-openh264.so`: Debian `gstreamer1.0-plugins-bad`,
  `1.26.2-3+deb13u3`, ARM64.

All three extracted libraries match the published APK. **None was rebuilt from
upstream sources.** Source-to-binary equivalence for these upstream artifacts is
outside this verification. The [embedded bootstrap ZIP](reproduction-v2.1.7/bootstrap.json) likewise uses prebuilt
Termux packages; recompiling its embedding library does not rebuild those packages.
Maven dependencies and toolchains are also prebuilts. License/copyright assets were
retained and included in the complete APK payload comparison.
The original [upstream provenance](../../third-party/termux-x11/provenance.json)
and [build pins](../../scripts/x11-build-inputs.json) remain unchanged.

## Unsigned release-equivalent build and comparison method

The production release signer guard remains unchanged. A verification-only init
script adds an unsigned `reproduction` build type initialized from `release`, with
release library fallbacks and the generated `BUILD_TYPE` constant set to `release`.
It asserts release properties, rejects unhandled release-specific source sets or
dependencies, and rejects production signing tasks. Functional source is unchanged.
This is an explicitly adjusted verification build variant, not execution of the
production signing task with its checks disabled.

Every ZIP entry is compared by uncompressed SHA-256; duplicate names or damaged
ZIPs fail. No native library, DEX, resource, manifest, metadata or license file is
excluded. Full APK byte equality is evaluated separately from entry equality.
The published APK has a verified v2 signature in an 8,192-byte APK Signing Block,
including its signing/padding records. It has no additional JAR-signature entries.
[Signing block structure](reproduction-v2.1.7/signing-block.json).
Without the private key the verification APK is intentionally unsigned.

The initial completed build matched 1,138 of 1,140 entries. Its two differences
were the Clang glycin shim and `classes2.dex` containing the verification variant's
`BUILD_TYPE`. [Initial comparison](reproduction-v2.1.7/initial-comparison.json).
A diagnostic dependency-inventory attempt failed on ambiguous Gradle project
variants; the inventory was restricted to external module artifacts. Two attempts
to set the generated task property failed because AGP finalizes that property.
The final recipe uses the standard build-type `buildConfigField` configuration.
These were verification-harness failures, not successful APK comparisons or app
regressions; local attempt logs are retained.

## Repeatable procedure

The version-specific [verification scripts](../../scripts/release-reproduction-v2.1.7/)
retain the actual local tool paths. Inspect/configure the paths for another host;
do not assume a different compiler produces identical bytes. The task output
`source` folder must not exist before preparation; preserve previous results separately.
All commands run from a checkout containing the tag and existing local tools.

1. Verify tag commit and the public APK SHA-256 given above. Keep the comparison
   APK outside all source/build directories.
2. Run `python3 scripts/release-reproduction-v2.1.7/prepare.py`. It archives the
   exact tag, verifies cached download pins, prepares X11 and rebuilds JNI code.
   The tag's historical Alpine URL may be unavailable: supply its **exact pinned
   file** from the already published Components build-header archive. Do not
   substitute a new musl revision or install a package. See [CI inputs](CI-X11.md).
3. Run `python3 scripts/release-reproduction-v2.1.7/rebuild-glycin.py` for the
   documented historical GCC recipe. Retain the tag's initial Clang result/hash.
4. Copy `unsigned.init.gradle` from that script directory into
   `.verification/release-reproduction-v2.1.7/`, then run
   `python3 scripts/release-reproduction-v2.1.7/build.py`.
5. Run `compare.py PUBLISHED.apk REBUILT.apk REPORT.json` from that script directory.
   Rebuilt APK: `.verification/release-reproduction-v2.1.7/source/.verification/arm64/app/build/outputs/apk/reproduction/app-reproduction-unsigned.apk`.
6. Run `compare-container.py PUBLISHED.apk REBUILT.apk CONTAINER.json` to compare
   whole unsigned-container bytes after the narrowly defined signing-block removal.
   This never changes either APK and is separate from cryptographic verification.
7. Verify public signature with `apksigner verify --verbose --print-certs`; inspect
   both APKs with `aapt2 dump badging`. Review **all** changed/missing/extra entries
   before making a content-equivalence claim. Never sign with or copy the private
   production key merely to make the comparison pass.

The separate debug CI/tests remain unchanged. This task does not rerun phone/UI,
agent/network behavior, full Modern Lint or the historical security audit, because
it makes no application changes and distributes no installable release APK.

## Reporting configuration

GitHub private vulnerability reporting was enabled and an API read-back confirmed
`enabled: true`; [recorded response](reproduction-v2.1.7/reporting-after.json).
[SECURITY.md](../../SECURITY.md) now links to the private reporting form. No test
vulnerability was submitted. The original security-audit date was not advanced.
