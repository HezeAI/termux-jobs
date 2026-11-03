# Private test22 — first-access instruction-size assessment

VersionName 2.1.1-test22; versionCode1457. Only two built-in Markdown templates
and version metadata change relative to delivered test21. This task does not
reorganize project AGENTS.md or change any saved user instruction file.

## Checked anew

- Exact single replacement in RU and equivalent EN. Reversing that substitution reproduces each previous complete template, preserving all other sections. No duplicate headings.
- The rule requires measuring bytes on first access per task, assessing splitting above 20,480 bytes, no repeat measurement while unchanged, demonstrated benefit before splitting and a brief reason for deferral.
- 44 Python tests passed: localized rule semantics, storage-addition boundaries in both actual templates, custom-file preservation on template updates, initialization, history and conflicts.
- 11 Java/UI/static cases passed: template preview, RU/EN switching, explicit apply/cancel, storage addition and narrow/large-font/reduced-height preview. These are model UI checks, not real phone keyboard tests.
- Final release compilation succeeded. Final APK ZIP integrity, com.termux, 2.1.1-test22/1457, ARM64-only ABI, non-debuggable status, signing certificate/signature, SHA-256, assets and runtime inventory passed. No required or unreviewed missing runtime types.
- Packaged RU/EN templates match source files. Other assets and three native libraries match test21; native payloads also match v2.1.1. No private authentication/key filenames are packaged.
- Current general AGENTS.md, project AGENTS.md and shared LEARNING.md hashes are unchanged across this task. git diff --check passed.

## Artifact

Termux-Jobs-test22.apk: 39971432 bytes.
SHA-256: `5dfe542b49089a77c050cc8db4c86fa581cbfb2f5ddebb0d59189684b0d7def9`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (same signer as v2.1.1/test21).
Existing data formats, package and explicit template-apply/history/conflict logic
remain. No automatic replacement of user instructions is introduced.

## Reused

Editor and helper code, resources, dependencies, toolchain and native inputs match
the delivered test21 source identities; only Markdown and version metadata change.
The earlier modern five-module Lint result (0 errors, 234 warnings) is reused for
identical analyzed production code/resources, not presented as a fresh test22 run.
Intermediate static coverage here is compilation and targeted rule/preview tests.
Existing SDK/caches/native inputs were reused without SDK/rootfs copies. No new
Lint engine or dependency was introduced. See reuse-identity.json and artifact.json
in .verification/instructions-template-test22/ for compact comparisons.

## Not checked and deferral

Real device keyboard, touch, font shaping and accessibility remain phone checks.
No graphics, installation, environment update, live agent restart or external
network test was needed or performed for this prose-only update. APK was not
installed or published.

At first access, project AGENTS.md measured 32,925 UTF-8 bytes, above the threshold.
Its multiple subsystem requirements could be assessed for topical organization;
reorganization is deferred because the user explicitly excludes it from this task.
No claim of token or time savings is made. Historical reports are preserved.
