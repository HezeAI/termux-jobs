# Release reproducibility — v2.1.9

The clean unsigned ARM64 build matched all **1,145 ZIP entries** of the signed
Termux Job’s APK for `com.termux` 2.1.9, versionCode 1549. The full container
also matched after removing the verified 8,192-byte APK Signing Block in
memory and correcting the ZIP central directory offset. Both APKs had zero
unregistered bytes between their ZIP records. The signed file was
preserved, and its APK v2 signature verified against the pinned certificate.

| APK | Bytes | SHA-256 |
| --- | ---: | --- |
| Signed release | 40,705,872 | `04b532d9c592c18abb2c3213e96ba9c5c6a56aa3923331d464094dc9aaf44271` |
| Clean unsigned build | 40,697,680 | `93eae96a28687f745739a7240dba5e4f6812b384c73d673fb746c2c866a05312` |

[Comparison](reproduction-v2.1.9/comparison.json) ·
[Every payload SHA-256](reproduction-v2.1.9/payload-sha256.json).

## Source and environment

OpenAI Codex ran the local comparison on 2026-11-21 in Ubuntu 26.04.1 under
PRoot on Android 16 ARM64. The frozen production inputs match source commit
`91b45863ac4f97dccdac73a31748258c08acfcd1` across 748 checked
production files. The release tag can include later release documentation with
the same production inputs. The two final Claude scripts were staged before
asset merging. Their merged and packaged bytes match the commit. The source
record documents the unused historical development signing fixture.
[Source identity](reproduction-v2.1.9/source-identity.json).

The build used Gradle 7.2, AGP 4.2.2, R8 8.9.35, OpenJDK 11.0.33.1, Android
API 35, Build Tools 30.0.3 and Android ARM64 AAPT2 2.20. All 112 Gradle tasks
executed in separate project, output, Java home and Gradle user home directories.
The build accessed existing SDK files and pinned dependency artifacts for
reads. No previously built project-native libraries, classes, DEX or private
signer entered the reproduction tree.
[Tool hashes](reproduction-v2.1.9/toolchain.json) ·
[Resolved library artifact hashes](reproduction-v2.1.9/resolved-dependencies.json).

## Repeatable procedure

1. Archive the production commit into a new source directory. Exclude the
   unused `app/dev_keystore.jks` development fixture during archive creation.
   Expose the pinned local SDK and use `python3 scripts/prepare-java-verification.py --arm64`
   to create the isolated Android stage. Keep signing material out of both
   directories.
2. Retrieve the pinned ARM64 Termux bootstrap and musl headers using the public
   asset IDs and digest checks in the existing [CI recipe](../../.github/workflows/fork-ci.yml).
   Run `scripts/prepare-x11.py` from the clean checkout with Termux Clang 21.1.8
   to rebuild the Alpine shim and prepare verified X11 inputs. The resulting
   Xlorie, OpenH264, GStreamer codec and keyboard data come from the pinned
   upstream binaries. The bootstrap ZIP embeds prebuilt Termux packages.
   [External inputs and native hashes](reproduction-v2.1.9/native-inputs.json).
3. Use the [ARM64 JNI build commands](../../scripts/build-local-arm64.sh) for
   the two project JNI libraries. Compile with Termux Clang and LLD 21.1.8
   against the official Android NDK r28b sysroot at API 24, using its ARM64
   linker inputs. The recorded run obtained the common and ARM64 sysroot
   headers and four linker members through verified range requests, and
   matched the four historical linker member hashes. Assemble the pinned
   bootstrap ZIP into the embedding library and link both libraries with
   16 KiB ELF alignment. Stage those two fresh outputs for packaging.
4. Rebuild the Fedora Glycin shim with Ubuntu GCC 15.2.0-16ubuntu1 and GNU
   Binutils 2.46-3ubuntu2. Fetch and extract the recorded packages into a
   private tool directory and point the tool invocation at its sysroot,
   library directory, GNU assembler and GNU linker. The historical flags
   are shown below. Stage the matching GCC output in place of the fresh
   Clang output from the general X11 preparation. Their distinct hashes
   are recorded in the native report.
5. Use the [unsigned Gradle init script](../../scripts/release-reproduction-v2.1.8/unsigned.init.gradle)
   with the clean Android stage and a fresh Gradle user home. Set
   `TERMUX_APP_VERSION_NAME=2.1.9` and `TERMUX_APK_VERSION_TAG=v2.1.9-arm64`.
   Set `TERMUX_SPLIT_APKS_FOR_RELEASE_BUILDS` and
   `TERMUX_SPLIT_APKS_FOR_DEBUG_BUILDS` to `0`, and leave the private project
   Git fallback unset.
   Set `REPRO_EVIDENCE` to a writable evidence directory. Point Gradle to
   the pinned AAPT2 with `-Pandroid.aapt2FromMavenOverride` and expose the
   pinned dependency cache with `GRADLE_RO_DEP_CACHE`. Load the init script
   with `-I` and assemble using `--offline --no-daemon --no-build-cache
   --rerun-tasks -x :app:downloadBootstraps :app:assembleReproduction`.
   The init script preserves release settings and the generated release build
   type while excluding the production signing path. The signed build uses
   its normal signer check.
6. Verify the signed APK with `apksigner verify --print-certs`. Run
   [compare.py](../../scripts/release-reproduction-v2.1.8/compare.py) to check
   every entry and [compare-container.py](../../scripts/release-reproduction-v2.1.8/compare-container.py)
   to compare the complete unsigned container. No ZIP entries are removed
   during comparison.

```sh
aarch64-linux-gnu-gcc-15 -fPIC -shared -nostdlib -O2 -Wall -Wextra -Werror \
  -Wl,-z,now,-z,relro,-z,noexecstack -Wl,-soname,x11-fedora-glycin.so \
  x11/src/main/cpp/x11-fedora-glycin.c -o x11-fedora-glycin.so
```

The clean build emitted legacy packaging Lint compatibility messages and
SDK metadata and native-symbol warnings. The byte comparison covers local
source-to-APK correspondence. Modern Lint, phone behavior, CI reproduction,
independent security review, third-party source builds and full signed-APK
reproduction require separate evidence.
