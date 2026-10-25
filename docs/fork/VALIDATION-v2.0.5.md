# v2.0.5 validation — 2026-10-25

## Final artifact

- APK: `Termux-Jobs-v2.0.5-arm64.apk`
- Package: `com.termux`; versionName `2.0.5`; versionCode **1376**.
- ABI: ARM64 only; min26/target28/compile35; embedded desktops require Android11+.
- Size: 52,721,789 bytes.
- SHA-256: `84d02da0a03bd70a684b86e59342dd424a443d70bdb13a3447faf122f03001ad`.
- Signing certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (unchanged).
- ZIP integrity, release/debug flag, identity, runtime class inventory, JNI
  callback names and pinned native hashes verified. Existing stable X11/Termux
  JNI bytes are unchanged. Source assets and 417 production Java/XML files match
  the final build stage. Packaged license payloads match pinned inputs.
- Public release generated BuildConfig has `WAYLAND_PREVIEW=false`. Private
  development builds must explicitly pass `-PtermuxWaylandPreview=true`.

## Relevant tests

The release-default JVM run completed successfully: **1297 tests in
195 classes, 4 intentional private-preview-only skips,
zero failures/errors. App and terminal-emulator suites ran; terminal-view and
termux-shared currently declare no unit-test source tasks. Coverage includes
screen bounds/rotation/pointer mapping, form preset/reset/save/cancel/live failure,
scale restoration, extra keys/modifier release, keyboard gates and shared
service/installer/updater/agent/storage behavior.

Public gate tests verify gray disabled Wayland launch, Bionic import/list and
Linux-driver controls, ignored-but-preserved saved preview preference, and
rejection of service check/install/launch plus direct process/runtime preparation
before files are created. X11 renderer choices remain available across all eight
supported targets. A separate opt-in private build passed its 12 focused test
cases (10 executed, 2 public-only skips), including the original enabled-preview
selection/import/binder behavior. The private test.10 APK remains byte-identical:
`c938988fbdc5ad8d211d860702e69833392ead36a2bdffe28e8dba50ea16a47f`.

The full Python invocation discovered **667 tests**: 665 passed, one intentional
retired-overlay skip, and one stale source-layout assertion failed because the
new keyboard gate replaces the old inline profile monitor. That assertion was
updated to check the new session-scoped focus helper and its bus monitor; all
**30 native/focus tests** then passed. No production code changed after the APK
for this test-only correction. All previously passing Python cases remain
unchanged; the entire 667-case invocation was not repeated afterward.
All **31 Node agent fixture programs** passed.

Modern lint7.4.2 analyzed all five modules: **0 errors / 212 warnings**, with no
rule-loading/infrastructure errors. The 212 warnings match the preceding private
candidate after numeric form literals were formatted consistently. Known SDK,
accessibility, deprecated API and native-code-location warnings remain tracked;
this is not a target-SDK/store-eligibility resolution. The old APK toolchain's
Kotlin metadata diagnostics do not replace this independent lint gate.

## Review, state transitions and trust boundaries

Reviewed startup, selected-target geometry, Apply acknowledgment/failure,
automatic reset, orientation/IME, scaling recovery and extra-key closure paths.
Scale is a bounded owned one-shot process, separate from the desktop child; its
closure does not select arbitrary sessions/processes. Settings remain per target,
and restoration avoids silently replacing later user edits. The keyboard helper
consumes only allowlisted active-window/input-context metadata, without titles,
text, clipboard or keys. Missing metadata retains manual | input.

Wayland's build gate is enforced in UI selection/import availability and backend
validation/preparation. It does not rely only on disabled row styling. No runtime
public unlock, automatic KDE install, rootfs migration, live-session manipulation
or agent-account change is introduced. Gray preview descriptions make testing
status explicit. Existing native X11/bridge/driver payloads are unchanged.

The final APK private-file/credential-pattern scan found no matches. The staged
source scan found no private files/credentials; one synthetic private-key header
in a redaction unit test was reviewed (no key block). This is best-effort scanning,
not a guarantee against every secret. Sources/licenses for the retained disabled
preview are pinned under third-party/droiddeck-wayland and documented separately.

## Compatibility, device evidence and limits

Same signer/package/ARM64/schema8. No valid-data reset, account migration or
rootfs reinstallation is required. Code1376 exceeds public2.0.4/code1366 and
intentionally equals the developer's private test.10. The updater rejects equal
or lower codes before showing an update, so that private build is not offered
this public APK as newer. RU/EN updater notes state this and carry compatibility
before installation, including for v1.0.19 updater clients. No user data is cleared.

HezeAI reported successful checks of the preceding X11 candidates and requested
this publication. Final public gate changes are covered by source/UI/backend
checks, not a fresh-phone GUI claim. Physical rotation/DPI/IME, unusual toolkits,
all driver/firmware combinations and clean-device installation are not exhaustive
acceptance results. Some already open applications cache their sizes/icons and
need reopening. Existing v2.0.4 native-driver/file-access restrictions remain.
Wayland full Plasma readiness is unresolved and **disabled in this public build**.
Installing an older APK does not undo Linux package upgrades made during private
KDE tests. No APK installation or user terminal/bot process change is part of
release preparation.

README, changelog, release notes, localized update notes, FORK, continuation,
usage/design/architecture and attribution were updated. CONTRIBUTING and issue
templates were reviewed; their existing environment/version/privacy questions
remain applicable. Historical private reports retain their original scope.
Pristine vendored input bytes were checked against all 141 source pins in the
staged Git index. Scoped attributes preserve those bytes and exempt only the
upstream snapshot's pre-existing whitespace; owned changes pass diff --check.
Evidence: `.verification/release-v2.0.5/`.
