# Private test14 validation — Advanced menu and agent form help

Date: 2026-11-01. Developer phone acceptance build.
[CHANGELOG](../../CHANGELOG.md) · [Project requirements](../../AGENTS.md).
HezeAI is the developer; Codex assisted with implementation.

## Final artifact

| Field | Value |
| --- | --- |
| File / delivery | Termux-Jobs-test14.apk / /sdcard/Download/Termux-Jobs-test14.apk |
| Package ID | com.termux |
| versionName / versionCode | 2.1.1-test14 / 1446 (previous test13:1445) |
| ABI / debug | arm64-v8a only / non-debug |
| Bytes | 39,938,320 |
| SHA-256 | 44fb5b1db1cbad3e8f691be2362756ab1667351399e2588be6c3e814b3faab0e |
| Certificate SHA-256 | dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644 |
| Compatibility | same package/signer, workspace schema8, min26/target28/compile35 |

The final signed file passed ZIP/CRC, package/version/ABI/debug, signature and
certificate, SHA-256, DEX/runtime definitions, packaged native/assets and private-data
checks. All 99 production source assets match the APK. All 110 asset/native entries
match delivered test13 byte-for-byte. Runtime audit: 14,018 definitions; remaining
31 optional missing types have existing explicit review notes. No environment,
account or history migration or data reset was added. Android update acceptance
and actual device behavior remain separate.

## Checked anew / Проверено заново

- **165 JVM tests, zero failures/errors/skips**, including new actual menu UI tests
  in RU/EN: exact main/submenu order, no duplicate moved entries, last About Intent
  retaining SettingsActivity, all four transferred windows, child Close → Advanced,
  negative Back/system Back/cancel → original main menu, repeated opens and parent
  dismissal. Existing header/navigation/instruction-entry tests passed.
- Maintenance engine starts are intercepted only in the menu tests. Every recorded
  request must be read-only inventory (cleanup-inventory or inventory); any mutation
  request fails the test. No cleanup, backup restoration, environment upgrade,
  maintenance worker or service is launched by these fixtures. This is window/
  navigation verification, not validation of the underlying maintenance operation.
- Agent new-session/preset context switches and existing-session/preset forms for
  Codex/Gemini/Claude/OpenCode: debug toggle/hint hidden, ordinary toggle/value
  retained, unrelated generic terminal/SSH help hidden, actual environment/CWD
  summaries updated (including legacy Termux), technical details collapsed and
  expandable. Codex scope and approval controls update their own help independently
  without mutating the saved profile before Save. RU/EN presentation checked.
- Legacy serialized agent debug=true is normalized off; copy and serialization
  enforce the same policy, as do effective diagnostic/native launch flags. Ordinary
  profile debug=true survives read/copy/serialization. Gemini command receives
  JOBS_GEMINI_DEBUG=0 even with a stale in-memory true flag. Base START/error/exit
  collection, manual temporary detail controls and diagnostic UI tests passed.
- Existing agent preparation, connection, Codex/Gemini/Claude/OpenCode launch and
  environment routing, workspace persistence and owned-process lifecycle tests
  passed. Save checks retain access levels, project-scope flags, confirmations,
  launcher path and working folder. Permission/approval/connection/stop helpers
  and maintenance implementations have unchanged source hashes.
- Localization consistency: **584 workspace strings**, settings keys and format
  arguments, plus targeted new inline RU/EN UI checks. Compile/resources passed.
- Full compatible modern Lint runner across all five modules: **0 errors /
  230 warnings**, no infrastructure/rule-loading failures.
  Four added SetTextI18n warnings concern explicitly bilingual/dynamic labels; no
  blanket suppression was introduced. Existing categories and the compileSDK35/
  AGP-tested33 support warning remain. Legacy APK-toolchain lint registry/Kotlin
  warnings in build logs are not used as a successful static-analysis gate.
- All **778 source files** match both packaged-build and
  modern-Lint stages, including new tests. Source/logical review traced dialog
  hierarchy and callbacks: navigation is dismissed before explicit terminal
  insertion/package launch, while passive child closing retains its parent.

Initial failures were retained: a test-only SettingsActivity import was corrected;
updated label assertions now reflect the shorter Read label and the existing
English Language / Язык label. A nullable test content-description comparison was
corrected. The final complete selected run above passes; no production validation,
permission or ownership gate was relaxed.

## Previously saved profiles and active sessions

Agent extended-debug=true is ignored immediately when decoding the profile; the
next normal save/export writes false. Copy and effective launch guards also cover
stale in-memory/imported flags. Ordinary terminal profiles retain their original
flag and behavior. Baseline error classification and required structured records
remain. Explicit temporary Detailed diagnostic collection is separate from the
removed persisted startup option and does not enable Gemini native --debug.

Already running processes keep their launch flags until the user restarts them;
this build does not restart/reconfigure chats or claim to remove a native flag
from a live CLI process. Existing reports/log files are retained. User-configured
commands or custom launcher scripts can independently request their own logging;
this change governs the app's managed extended-debug option.

## Reused / Переиспользовано

Repository-local SDK/JDK/Gradle/dependency caches, verified bootstrap and unchanged
JNI/X11 payloads were reused. Task-start inventory checks 951 existing
production/test/script inputs plus the two new tests. The eight changed existing
inputs are version metadata, WorkspaceController/Store/Diagnostics/GeminiCli and
three relevant test files. No helper/assets/native source was changed. Reuse is
source/binary identity evidence, not a new graphics/install/performance claim.
Evidence is in `.verification/settings-menu-test14/`: plan, baseline/reuse hashes,
JVM/build/Lint/localization logs, final APK/runtime audit and Download read-back.

## Not checked / Не проверено

Developer phone acceptance remains for actual touch/Back behavior, keyboard,
scrolling and expanded Details sizing; confirm update without data reset on the
installed app. Tests do not establish device gestures or every native CLI version.
Real cleanup, backup restoration and environment upgrades were deliberately not
performed for menu verification. No X11/driver/guest installation tests were needed
or run. No working chat/server session was stopped, no system tools were installed,
and no full SDK/rootfs copy was created. Publication is outside this private task.

AI development assistance: OpenAI Codex
