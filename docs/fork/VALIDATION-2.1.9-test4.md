# Termux Job’s 2.1.9-test4 validation

Private ARM64 build, 2026-11-21. Developer: HezeAI.
AI development assistance: OpenAI Codex.

## Gemini creation form

The developer's test3 screenshot showed three headings with empty spaces
underneath. The new-chat layout moved hidden legacy Gemini CLI controls into
the Antigravity form. It now shows the CLI default model, new chat, and the
Antigravity permission settings as read-only values in Russian and English.
The permissions still follow Antigravity CLI. The form requires a chat name
before creating the profile. Existing Gemini profiles retain their original
settings and launch route.

## Checked anew

- Compared the file lists and bytes for 656 source inputs in the release, JVM,
  and Lint stages. The controller was the only changed production source in
  that set. The release metadata advanced from 1543 to 1544.
- Localization check passed for 587 workspace keys and the app/shared
  locale resources.
- All 16 selected JVM tests passed. The creation fixture checks the three
  values in both languages, controls after switching from Gemini to Codex,
  draft retention and cancellation without saving or starting a session.
  The runner also exercised the Gemini profile and launch command, and the
  preparation and retry matrix.
- Offline Antigravity preparation and Node protocol fixtures passed. They
  cover consent, integrity failures, interruption, sign-in handoff and retry,
  history preservation, and owned process control.
- ARM64 `assembleRelease` passed in 2m 50s. The pinned release signer,
  packaged X11 inputs and runtime dependency guards ran. Modern Lint 7.4.2
  completed with 0 errors and 298 warnings. The app was reanalyzed and the
  unchanged dependency modules used cached analysis. The legacy Lint
  diagnostics in the assembly log did not count as a pass.
- The final APK passed ZIP integrity, alignment for 408 stored entries, SDK
  and package metadata, non-debuggable status, signature, ARM64 inventory,
  DEX runtime verification and checks for unintended private files. The
  DEX contains 14,663 classes. The 31 unresolved types have review notes.
  All assets, manifest permission declarations and three native libraries
  match test3. Each native library has 16 KiB ELF LOAD alignment.

## Reused and not checked

The build reused the three verified ARM64 binaries after comparing their
native sources and build settings with the recorded baseline. This change
did not rerun graphics or desktop checks, the test3 chat timeline suite, or
device acceptance. Installation, an actual Antigravity launch in Ubuntu AI,
Google sign-in, and provider replies remain unverified on the phone.

## Delivered APK

- `/sdcard/Download/Termux-Jobs-v2.1.9-test4-arm64.apk` (40,687,575 bytes).
- `com.termux`, 2.1.9-test4 / 1544, arm64-v8a, min SDK 26, target SDK 28,
  compile SDK 35. The package and certificate match test3 and the version
  code increases. The form change does not alter persisted data formats.
- SHA-256 `e7aa283489a840260d0bf814af5f38f997ad0e2adb3a8643cecbbc3fa4fc192a`.
  The copy in Download matches the verified project file.
- [Verification plan](../../.verification/test4-gemini/PLAN.md) and
  [assembly log](../../.verification/test4-gemini/build.log). The JVM,
  analysis, and final-file logs are in the same directory.
