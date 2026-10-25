# Reproducible source analysis

Use `bash scripts/verify-modern-lint.sh` before a release. It stages the same Java,
resources and manifests in `.verification/modern-lint`, uses AGP7.4.2/Gradle7.5.1/
JDK11 and enables dependency analysis. The five modules are app, terminal-emulator,
terminal-view, termux-shared and x11. This does not replace the APK build's AGP4.2.2,
Gradle7.2, native inputs or signer. It does not raise minSDK/targetSDK or install an app.

## Local tools and invocation

Use the repository-local JDK/SDK/cache setup from `scripts/build-env.sh` and FORK.md.
The SDK must be a copy inside this repository; automatic component installation
must never write to the shared Android SDK. Prepare the existing pinned X11 inputs
with the documented repository tooling. All stage files and reports remain ignored.

Download [Gradle7.5.1 binary distribution](https://downloads.gradle.org/distributions/gradle-7.5.1-bin.zip)
into `.tools/downloads/gradle-7.5.1-bin.zip`. Before extraction, verify SHA-256
`f6b8596b10cce501591e92f229816aa4046424f3b24d771751b06779d58c8ec4`
against the [official checksum](https://downloads.gradle.org/distributions/gradle-7.5.1-bin.zip.sha256).
Extract only inside `.tools/` so `.tools/gradle-7.5.1/bin/gradle` exists. The script
uses existing tools; it does not install system packages. Its aapt2 override is the
local Android/Termux executable used by this phone's isolated build workflow.

```sh
bash scripts/verify-modern-lint.sh
python3 scripts/tests/test_lint_report.py
```

Outputs are `.verification/modern-lint-report/run.log`, `lint.xml`, `lint.html`
and `summary.json`. A nonblocking flock prevents simultaneous stage replacement.
Old success reports are removed before starting. The checker rejects nonzero
Gradle exit, missing/malformed/wrong-engine XML, missing module analysis, lint
errors and custom-rule/Kotlin loading failures. An unavailable check is never
reported as a successful zero-error analysis. A bare legacy `:app:lintRelease`
can still print AndroidX/Kotlin failures; it is not the release analysis gate.

`python3 scripts/prepare-java-verification.py --modern-lint` only creates the
analysis stage. It does not execute lint. The `--arm64` APK and `--modern-lint`
stages are mutually exclusive. Only the copied plugin version and dependency-
analysis setting differ; production source/resource bytes must match the tested
and APK stages. The production build configuration is not silently upgraded.

## Scope and limitations

The previous AGP4.2 lint could not load AndroidX registries or newer Kotlin
metadata. The initial compatible run reproduced four Media3 opt-in errors and
other source findings; after fixes the same checks load and pass. There is no new
blanket suppression, warning baseline or filtering of dependency findings.
[AGP7.4 documentation](https://developer.android.com/build/releases/agp-7-4-0-release-notes)
requires Gradle7.5/JDK11; the newer-lint override documented by Google is available
only with AGP7+, so it cannot directly repair the old APK plugin.

This is a pinned compatible analysis path, not the newest Android toolchain.
AGP7.4 was tested up to compileSDK33; this project compiles against35. Its warning
is retained in logs and `compileSdkBeyondAgpTestedWarning` in the summary, not
suppressed. Do not claim exhaustive Android35 or future-SDK coverage. A complete
APK plugin/Gradle/JDK/native migration requires its own verification and is not
performed by this stage. Lint also does not prove JNI/driver compatibility, actual
Binder/permissions/gestures, UI frame timing, fresh rootfs setup or network recovery.

Current findings and the remaining warning catalog are recorded in
[AUDIT-LINT-2026-10-24.md](AUDIT-LINT-2026-10-24.md) and
[LINT-WARNINGS-2026-10-24.md](LINT-WARNINGS-2026-10-24.md).

## v2.0.6

Modern lint was rerun after the final X11-scale, renderer-download and chat changes across all five modules. Result: zero errors, 210 existing warnings, no infrastructure/rule-loading diagnostics. No broad baseline or suppression was added. The existing compileSdk/AGP support warning and the issue categories above remain. See [release validation](VALIDATION-v2.0.6.md).
