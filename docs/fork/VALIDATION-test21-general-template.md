# Private test21 — approved general template and structural review threshold

VersionName 2.1.1-test21; versionCode1456. This update replaces built-in RU/EN
Markdown only, retaining the test20 instruction/memory editor. The source is the
full marked text in TASK-update-general-template.txt, with the subsequently
requested single threshold replacement: 20 KiB / 20,480 UTF-8 bytes. The older
instructions-review source is not the final authority.

## Checked anew

- RU matches the marked attachment except the authorized threshold substitution. No task wrapper/markers included. EN preserves all 77 bullet rules, section order, restrictions and documentation reorganization conditions.
- No duplicate headings or old threshold in either current template or the final packaged assets. The threshold prompts structural assessment; it is not mandatory splitting. Memory's AGENTS.md-edit rule explicitly permits the same documentation reorganization without changing requirement meaning.
- 44 Python tests pass: storage-preview boundaries in both real localized templates, correct threshold/units, no forced split, missing-only initialization, preservation of custom files on template updates, history and stale-write safety.
- 11 Java/UI template, language, storage and narrow-preview cases passed after the full template replacement. The final threshold-only substitution was followed by Python checks and final asset/hash verification; those UI cases were not rerun for a number-only prose change.
- Release compilation/package succeeded. Final ZIP integrity, package com.termux, versionName/code, non-debuggable ARM64-only ABI, signature/certificate, SHA-256, embedded assets and DEX/runtime inventory passed. No required or unreviewed missing runtime types.
- Current global user AGENTS.md hash is unchanged across this task. No project AGENTS.md splitting was performed. git diff --check passes. Previous versioned validation reports are unchanged.

## Artifact

Termux-Jobs-test21.apk, 39971308 bytes.
SHA-256: `e5dba958afe92d5bf2ca47cbe39c7bc5af29c45cae076566e5e7c50f145b70bd`.
Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (unchanged).
Package com.termux, versionCode1456, compatible signing identity; no data-format
change or automatic instruction replacement. Existing files require explicit
preview/apply to adopt this new text, using existing history/conflict safeguards.

## Reused

Compared with delivered test20, production code/resources/dependencies are
unchanged; app/build.gradle changes version metadata only. Only two Markdown
assets differ in the final APK. All three native libraries and other assets match.
The 49 test20 editor/UI checks and modern five-module Lint result (0 errors,
234 warnings) apply to identical editor code/resources; they are not new runs for
test21. Compact source comparisons are in reuse-identity.json. New intermediate
static coverage is compilation plus targeted template/backend tests; fresh full
Lint is not required for this Markdown/metadata-only intermediate update.
Existing SDK, native inputs and caches were reused, without rootfs/SDK copies.

## Not checked

Real phone keyboard, font shaping, touch and accessibility acceptance remain
separate. No desktop, installation, environment-update or network-recovery tests
were repeated for this template-only change. No claim of model adherence, token
savings or time savings. APK was not installed or published.

Evidence: .verification/instructions-template-test21/ (build, tests, artifact,
signature/runtime and input-identity records). Older reports retain their original
scope and thresholds. User instructions and authorization were not imported or
modified by this task.
