# v1.4.27-test validation

Change: trusted APK asset loading, cached without retaining Activity, included before authoritative current settings on every local agent turn. No arbitrary project-file discovery or new runtime permissions. Asset is fixed-name, bounded to16KiB, required nonempty; load failures propagate through existing turn error handling. No file content becomes a system instruction.

Relevant tests cover packaged asset availability/content, injection on initial/corrective/post-tool generations, unchanged read/chat rejection of destructive commands, full command access only with project-only off. Tests use fake model/tool responses and do not prove actual Qwen compliance.

Review: no changes to authorization, approvals, process cancellation, environment creation, native code or stored history/model formats. Existing user data needs no migration/reset. Instructions cost additional input tokens; changing system text starts a new prefix cache scope. No promise of faster generation or reliable deletion selection.

First verification attempt failed to compile a new test because this project's older Robolectric uses RuntimeEnvironment.application, not getApplication(). Corrected the fixture API; results of the final run follow below. Device checks required: full-access task with confirmations off, read rejection of writes/deletes, project restriction, actual Qwen tool protocol, and existing-chat behavior. No exhaustive device claim.

Final Java suite:670 tests,0 failures/errors/skips, including3 new instruction integration/authorization cases. Evidence: `.verification/local-ai-v1.4.27/full-suite/` and `tests-final.log`.

Final build/runtime gate passed (15926 definitions,30 reviewed unresolved types). ZIP CRC, package com.termux/versionCode1133/versionName1.4.27-test, ARM64, minSDK26, existing certificate, source-stage correspondence, packaged instruction content and limited-secret scan passed. All native entries/payloads and external CLI/server adapters unchanged from v1.4.26. SHA-256: `928588abea13cd73d8887a118cd7c9857175c52b557a3d418b03e99f8ef8ba37`. Delivery copy hash verified.
