# Building Termux Job’s

Current stable source: **v2.1.8 / versionCode 1540**. `fork/base` includes this
release line and current documentation. Use tag `v2.1.8` for the exact published
release source; later documentation/CI commits do not replace that APK.
[Release](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.8) ·
[Artifact and validation](docs/fork/VALIDATION-v2.1.8.md).

## Toolchain and isolation

Read [AGENTS.md](AGENTS.md), [testing rules](docs/fork/TESTING.md) and
[release procedure](docs/fork/RELEASING.md) before changing or distributing a build.
Keep tools, SDK and caches inside this checkout; do not modify the installed app,
working environments or a shared SDK. No APK is installed by these instructions.

- Gradle wrapper **7.2**, project JDK **11**. CI uses Java **17** for Android SDK
  tools, then switches to Java 11 for the project.
- Compile SDK **35**, Build Tools **30.0.3 / 35.0.0**, NDK **22.1.8171670**.
  The workflow overrides aapt2 with Build Tools 35.0.0. ARM64 hosts require a
  compatible aapt2 and clang/lld; do not run x86 SDK executables on ARM64.
- App min SDK **26**, target SDK **28**, published APK **ARM64**.
  X11 requires Android 11+. See [gradle.properties](gradle.properties).
- [build-env.sh](scripts/build-env.sh) uses `.tools/sdk`, `.tools/gradle-home`
  and `.tools/tmp`. `FORK_ANDROID_HOME` must resolve inside the checkout;
  `FORK_JAVA_HOME` selects the project JDK. Do not put a shared SDK in `sdk.dir`.

## Verified inputs and CI

[Fork CI](.github/workflows/fork-ci.yml) is the executable clean-runner recipe.
It prepares pinned X11 inputs, checks environment archives/helpers, runs JVM tests
and builds an internal debug APK. CI records both source and workflow commits.
A successful debug build is not validation of the published signed release APK.

[prepare-x11.py](scripts/prepare-x11.py) and [CI-X11](docs/fork/CI-X11.md) describe
pinned native inputs. The unchanged `musl-dev-1.2.6-r2.apk` build headers come from
[Components](https://github.com/HezeAI/termux-jobs-components/releases/tag/x11-build-headers-musl-1.2.6-r2)
after removal from Alpine. This is a build input, not an Alpine runtime update.
The workflow also verifies pinned Gradle/bootstrap bytes from public asset APIs.
Do not replace archives or checksums to bypass a download failure.

## Checks and local builds

Choose checks by changed behavior and dependencies using [TESTING](docs/fork/TESTING.md).
For a prepared local toolchain, existing entry points include:

```sh
python3 scripts/check-localization.py
bash scripts/verify-java.sh
bash scripts/verify-modern-lint.sh
```

`verify-java.sh` prepares isolated sources and X11 inputs before JVM checks; it is
not a lightweight documentation check. Full Modern Lint of all five modules is
mandatory before a public application release. Documentation-only changes require
text/link/diff checks, not an APK rebuild. Read [LINT](docs/fork/LINT.md) for the
modern runner and SDK warning boundaries.

For an authorized ARM64 release build, prepare `FORK_NATIVE_SYSROOT` using an
existing read-only Android NDK sysroot and set `FORK_NATIVE_CLANG`, `FORK_NATIVE_LLD`
and `FORK_AAPT2` to compatible tools. Then run:

```sh
TERMUX_APP_VERSION_NAME=2.1.8 bash scripts/build-local-arm64.sh
```

This sets the APK tag to `v2.1.8-arm64`; the output is
`build/apk/Termux-Jobs-v2.1.8-arm64.apk`. Set the version explicitly: the script's
legacy fallback is not the current release version. VersionCode comes from
[app/build.gradle](app/build.gradle). A new release needs its own authorized
metadata and checks; never overwrite the published 2.1.8 artifact.

## Signing and publication

Release requires the existing authorized key at `.signing/fork-debug.jks`
(historical filename). The build refuses a missing or mismatched release signer;
it does not generate a replacement key. Never commit keys or credentials.
Independent debug builds do not update the signed application.

Package ID remains `com.termux`. Update compatibility needs the same certificate
and a suitable versionCode; package name alone is insufficient. The public signer
pin is [release-certificate.sha256](gradle/release-certificate.sha256).
Perform final ZIP/package/version/ABI/debug/signature/runtime/assets/SHA-256 and
updater checks under [RELEASING](docs/fork/RELEASING.md). Do not infer device,
GPU/X11 or migration behavior from source hashes or CI alone.

## Sources and history

HezeAI maintains this unofficial Termux fork with AI development assistance.
Keep upstream authorship, licenses and notices: [LICENSE](LICENSE.md),
[THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES.md), [architecture](docs/fork/ARCHITECTURE.md).
Old versions, private tests and former build recipes are preserved in the
[historical build notes](docs/fork/history/FORK-through-v2.1.7.md),
[CHANGELOG](CHANGELOG.md) and version-specific validation reports.
