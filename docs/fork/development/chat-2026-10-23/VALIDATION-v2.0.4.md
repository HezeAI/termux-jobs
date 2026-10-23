# v2.0.4 development validation — APK menu

Unreleased developer build, 2026-10-23. Published stable remains v2.0.1.
Android8+/ARM64, unchanged com.termux/signer/schema8; code1328 exceeds v2.0.3's
1327. No reset, stored-data migration or environment changes.

## Artifact

- APK: `build/apk/Termux-Jobs-v2.0.4-arm64.apk`
- Size: 45819412 bytes
- SHA-256: `df148307a349f69e92eb3e284268284eacc5419802974b54a11d0119f38cf87f`
- Signing certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
- ZIP integrity, non-debuggable package/version, ARM64-only ABI, signature match,
  native-library equality with v2.0.3 and packaged-helper/source equality passed.
- APK assets/DEX bounded secret-pattern scan and private-file/development-directory
  filename scan passed. This is not an exhaustive secret detection claim.
- Runtime definitions:13597 classes, required definitions present;31 previously
  reviewed optional references. Release Gradle build passed; existing custom lint
  registry/Kotlin metadata warnings still limit lint coverage.
- Final-APK-derived updater metadata: `.verification/apk-menu/update.json`.

## Relevant tests and review

46 scoped JVM tests passed, zero failures/errors/skips: AgentChatUiTest,
AgentChatViewTest, ChatApkTest and ChatMediaBoundsTest. Revised assertions check
that the APK card has only filename/metadata to the right of its badge, retains
its open target and identity, has no Close action, has enabled left Download and
right Install actions after loading, closes on an outside touch, and opens no
Android activity automatically. Existing compact image viewer, focus/scroll,
read-only installer URI and other agent chat checks passed.

Traced APK loading/success/error, outside dismissal before/after background
completion, install permission/settings return and Download button disable/restore.
The save callback now owns its actual button identifier, so moving APK Download
to the left does not accidentally disable/re-enable Install. Image/video/text
viewers retain positive Save and negative Close. Installer/FileProvider policy,
Codex helper, media processing, native libraries and process shutdown are unchanged.
No extra Node run was required for this UI-only change; the packaged helper equals
the source and v2.0.3 already records the six passing Node suites.

Source/build evidence: `.verification/apk-menu/tests.log`, `build.log`,
`apk-runtime.json`, `apk-badging.txt`, `apk-signature.txt`, `artifact.json`.

## Phone checks still needed

Open an APK card and check visual left/right action alignment, outside-tap close,
Download and explicit Android installer launch/permission return. The outside
motion-event check uses Robolectric; actual Android layout/gestures and installer
UI are separate device checks. Existing v2.0.3 helper supports these cards without
a protocol change; older helpers still require a session restart with saved chat.
