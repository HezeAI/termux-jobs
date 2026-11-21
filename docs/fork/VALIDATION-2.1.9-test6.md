# Termux Job’s 2.1.9-test6 validation

Private ARM64 build, 2026-11-21. Developer: HezeAI.
AI development assistance: OpenAI Codex.

## Scope

- Includes the [test5 Antigravity startup and updater fix](VALIDATION-2.1.9-test5.md).
- Uses English common templates for Codex, Claude and Gemini. The runtime uses
  a durable marker per agent and instruction root and publishes a missing
  common file without replacement. Existing, empty and edited files survive.
  Project instructions and LEARNING.md remain explicit file actions.
  [Instruction forms and file paths](AGENT-INSTRUCTIONS.md#first-launch-and-gemini-files--219-test6).
- Adds Antigravity user and project skills in Ubuntu AI via the existing
  manager. Compatibility directories are read-only. Legacy Gemini can edit
  its instructions while its skill flow remains outside this integration.
  [Skill roots and verification](AGENT-SKILLS.md#sources-and-scope).
- The preparation dialog remains visible on outside touches or Android Back
  while preparing. Close dismisses only the dialog. Retry check is visible
  after failure, and successful preparation leaves only Close. Readiness
  delivery still happens once through the existing launch callback.

## Checked anew

- The instruction backend passed 76 filesystem tests on native Termux, including
  nonreplacement publication, existing and empty files, concurrent launch,
  no recreation after deletion, revision conflicts, recovery and symlink refusal.
- The skill manager passed 44 synthetic filesystem tests. Antigravity coverage
  includes user and project targets, compatibility roots, collision checks,
  cancellation and unsafe source refusal. The Android-hosted Python test fixture
  used `ln` when `os.link` was unavailable.
- The exact pinned 1.5.13 binary reached sign-in before listing either synthetic
  skill in headless or interactive mode. The probe used an isolated home and a
  verified local-only syscall guard. Native skill discovery remains unverified.
  [Headless evidence](../../.verification/test6-instructions/agy-native-skills/headless.json)
  and [PTY evidence](../../.verification/test6-instructions/agy-native-skills/tui.json).
- Localization passed for the 587 shared workspace strings and app/shared
  settings resources.
- The instruction, skill, preparation and CLI forms passed 127 JVM tests across
  11 classes before the late dialog change. The unrelated test3 timeline suite
  was outside this scope. The 657 final staged source files match by path and
  byte content across the release, JVM and modern analysis stages.
- All five preparation tests passed after the dialog change. The tests cover
  outside touch, Android Back, explicit Close, continuation of the preparation
  job after Close, failure retry and a single ready callback. Modern Lint 7.4.2
  on the final source reported 0 errors and 298 warnings. The app was reanalyzed
  and dependency modules used matching cached results.
- Final ARM64 release assembly completed in 3m 17s after the preparation
  dialog change. The package, version, signature and SHA-256 were verified
  against the actual output. The three native libraries and Android manifest
  permission declarations match test5. The six new or updated APK assets
  match their respective source files. ZIP integrity and alignment for 408
  stored entries, 16 KiB native ELF LOAD alignment, non-debuggable status,
  private-file scans and DEX runtime dependency checks passed. The 31
  unresolved DEX types have existing review notes.

## Unverified

Phone installation and dialog interaction, actual Antigravity authorization,
inference, and native discovery of managed skills or active instruction use
require device acceptance. The stock terminal supports `/resume`, `/model`, and
`/usage`. The Android app has yet to integrate these as native chat selectors.
[Google CLI reference](https://antigravity.google/docs/cli/reference/).

## Delivered artifact and logs

- `/sdcard/Download/Termux-Jobs-v2.1.9-test6-arm64.apk`, 40,699,759 bytes.
- `com.termux`, 2.1.9-test6 / 1546, arm64-v8a, minimum SDK 26, target SDK 28,
  compile SDK 35. The signer matches test4 and test5. The version code increases.
- SHA-256 `fcca35541aef671445bb9b109d2fde2df27c7164b2583ec24feb7ef819a4e4bb`.
  The Download copy matches the verified project artifact.
- [Verification plan](../../.verification/test6-instructions/PLAN.md),
  [assembly](../../.verification/test6-instructions/build.log),
  [JVM suite](../../.verification/test6-instructions/jvm.log),
  [dialog retry](../../.verification/test6-instructions/jvm-preparation-dialog.log),
  [Lint summary](../../.verification/test6-instructions/modern-lint-summary.json),
  [instruction tests](../../.verification/test6-instructions/python-instructions.log),
  [skill tests](../../.verification/test6-instructions/agent-skills-python.log).
