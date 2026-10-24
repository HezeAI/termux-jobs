# Lint alignment and final source-audit pass — 2026-10-24

Private **2.0.16-test / code1345** retains earlier audit fixes. Public stable
remains v2.0.2/code1331. Package com.termux, signing identity, schema8, ARM64,
minSDK26/target28/compile35 and embedded-X11 minimum API30 stay unchanged.
Evidence: `.verification/lint-audit-20261024/`.

## Analysis coverage and actual findings

The old AGP4.2.2 lint could not load AndroidX custom registries (Vendor missing)
and newer Kotlin metadata. Its previous0/118 output was a partial analysis.
A separate AGP7.4.2/Gradle7.5.1/JDK11 stage now analyzes all five modules and
project dependencies. No source/resource bytes are rewritten for lint. The
initial compatible run produced **11 errors and 230 warnings**, including four
Media3 UnsafeOptInUsageError findings proving those checks were loaded.

Final compatible analysis: **0 errors / 209 warnings**, no registry/class/Kotlin
loading diagnostics. No new blanket suppression or baseline hides findings.
The remaining 22 categories are explicitly cataloged in
[LINT-WARNINGS-2026-10-24.md](LINT-WARNINGS-2026-10-24.md), with per-location
machine-readable evidence in warning-triage.json. Remaining warnings include
real localization/accessibility/maintenance debt, optional artwork/layout work,
intentional Termux/proot/native packaging and separate SDK/identity decisions;
they are not all declared false positives.

Fixes and boundary review:

- Add explicit API30 guards to X11 diagnostic/renderer callbacks, matching the
  existing embedded-X11 requirement. Correct three misleading indentations with
  unchanged execution scope, including the existing unconditional custom-log-level
  assignment. Native launch, owned-child shutdown and cancellation code is retained.
- Acknowledge the wrapped Media3 unstable factory API with a constructor-scoped
  OptIn annotation; playback URL restrictions and networking/lifetime are unchanged.
- Make seven shared/emulator protocol conversions independent of device locale.
  CTRL+I no longer lowercases to dotless-i in Turkish. FileTime explicitly keeps
  the user's default display locale. Use the Linux O_DSYNC constant without an
  API27 Java-field reference on the API26 baseline. No JNI method/signature changes.
- Mark seven app/report notification PendingIntents immutable while retaining their
  explicit targets/actions and existing update flags. External plugin-owned result
  PendingIntents and their fill-in callbacks are untouched. Three components now
  explicitly declare exported=true, preserving their previous intent-filter default;
  other export, MANAGE_DOCUMENTS, RUN_COMMAND and URI-grant boundaries stay intact.
- Read thumbnail EXIF using AndroidX1.4.2 instead of the platform parser. The
  [official AndroidX release](https://developer.android.com/jetpack/androidx/releases/exifinterface)
  documents parser fixes. APK version markers establish that an existing transitive
  AndroidX EXIF1.3.6 definition is upgraded to1.4.2; this is not an entirely new
  large library added to an APK previously without EXIF. Thumbnail matrices,
  limits/cache names and original image contents are preserved.
- Save chat attachments through the existing exclusive/no-follow ProjectFileExport
  implementation, closing the create-then-reopen window. Existing files/links are
  not overwritten; collisions use the file manager's numeric suffix convention.
  Closing the owner cancels an incomplete export. There is no automatic replay.
- Image-preview taps call performClick; drag, multi-touch and cancellation do not
  become clicks. Pan/zoom geometry and window dimensions are retained. Surface/
  terminal accessibility needs a separate phone/TalkBack check.

## Repeatable checks and tool limits

[The lint command](LINT.md) is `bash scripts/verify-modern-lint.sh`. It locks the
stage, clears stale success artifacts before running, and requires valid pinned-
engine XML, successful Gradle exit and all five module analyses. The report
checker rejects infrastructure failures and lint errors, not just command status.
Eight Python tests cover positive warnings, missing analysis, registry/metadata
failures, error/fatal incidents, bad engine/XML, stale missing-report output,
nonzero Gradle exit and visible SDK-support warnings. Concurrent execution was
also rejected before stage replacement. Both staging modes and invalid combined
arguments were exercised; shell syntax/localization/diff checks passed.

The [documented AGP7.4 combination](https://developer.android.com/build/releases/agp-7-4-0-release-notes)
uses Gradle7.5/JDK11. The downloaded7.5.1 archive was checked against its official
SHA-256 before extraction inside .tools. This is a pinned compatible analysis
path, not the latest toolchain. AGP7.4 is tested up to compileSDK33 while this
project uses35. That warning remains visible in logs/summary; exhaustive API35/
future-SDK coverage is not claimed. The APK build keeps AGP4.2.2/Gradle7.2 and may
still print legacy lint-vital metadata diagnostics. Its output is not the release
analysis gate; RELEASING.md now requires the separate compatible analysis. Full
APK plugin/JDK/native migration remains a deliberate separate task.

## JVM and artifact evidence

The final full confirmation passed **1,209 JVM tests in 182 classes**: app: 1,055/
163 classes and terminal-emulator: 154/19, no failures/errors/skips. The emulator
was explicitly forced to execute, not reported from UP-TO-DATE cache. New cases
cover exclusive attachment export/cancellation/name handling, real EXIF metadata
reading/orientation without changing the source, image tap versus drag/cancel,
Turkish protocol/shortcut behavior and immutable explicit notification actions.
Existing thumbnail, player diagnostics, agent/input, provider and X11 lifecycle
fixtures are included in the full app run.

The first full app run had 1 fixture failure: the synthetic Drawable.Callback had
only the weak reference held by Drawable, unlike a live View owner. The test now
holds its owner through its last assertion; a seven-case scoped rerun and then
full-suite confirmation pass. Runtime animation was not changed. A first JPEG
fixture tried the host JDK's optional native libjpeg, absent from this isolated
JDK; it was replaced with a small EXIF container for the actual metadata parser.
Existing real PNG thumbnail decoding remains tested. This does not prove native
Android JPEG decoding, touchscreen gestures or GPU behavior. An intermediate test
constant path and an initial isolated Gradle init-script receiver were corrected
before the successful final runs; their failure logs remain in evidence.

Production delta versus 1344: 16 Java sources, three explicit manifest export
attributes and version/EXIF dependency metadata. No new production Java file.
Main Java/resources match JVM, modern-lint and APK stages. Runtime assets and
native libraries match 1344 byte-for-byte. Python/Node runtime asset fixture
suites from1342 were not rerun: these assets are unchanged; only the new lint
checker has eight fresh Python cases. Earlier results retain their artifact scope.

Verified APK: `build/apk/Termux-Jobs-v2.0.16-test-arm64.apk`, **45,849,832 bytes**. SHA-256:
`e671773d958d071f7b002a6752fcbccfda4b247b96fad851b45446943144d893`.
ZIP/package/name/code1345/ARM64/non-debuggable checks, signer continuity/increased
code, DEX runtime definitions, tested-source/stage review and bounded secret-pattern
checks passed. Required missing types: 0; 31 optional references remain reviewed.
No configured private server address, auth/key file or bounded secret pattern
was found in the artifact. A bounded scan is not a universal secret guarantee.

## Compatibility and remaining validation

No valid-data reset, account/rootfs migration, cache purge, package split or SDK
requirement change is required. Prior intentional outside-HOME SAF restrictions
remain. No working terminal/agent session, environment or server/bot is modified
by this source audit. A later stable release must exceed1345, publish update.json
from its final APK and disclose accumulated compatibility changes in RU/EN
highlights and English release notes. This private audit does not publish a release.

The agreed source-audit sequence is complete with the limits recorded above.
This is not a declaration that every app defect or warning is resolved. Device
validation still includes photo preview/zoom, attachment Download with a collision,
notification open/wake-lock actions, agent input focus and terminal/X11 switching.
Actual storage/Binder permissions, fresh-device environment setup, native drivers,
remote descendant cancellation/durable recovery and future target-SDK/package-ID/
sharedUserId migration remain separately scoped verification/design work.
