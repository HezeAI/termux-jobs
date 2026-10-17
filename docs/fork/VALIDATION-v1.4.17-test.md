# Validation — v1.4.17-test / code1123

Scope: metadata-only per-model-call records in LocalAiTiming. No inference,
permission, tool, native cache or external agent behavior changed.

Relevant cases: retain earlier token/cache/timing records across a tool boundary;
show active call separately; completed vs interrupted calls; error/finish without
duplicate archive; new request clears records; RU/EN labels; private output absent
from report; unknown LiteRT counters;64-record cap and omission count; pause skips
archival. Existing latest-call summary remains available.

Logical review: context_preparation closes an unfinished previous attempt before
resetting its counters; postprocessing archives a completed attempt before tools;
finish archives any still-active attempt once. Live report adds only an open
attempt. Synchronized methods serialize callback/report updates. Reports retain
metadata only and reuse existing private persistence/export. No Android view
recreation, timers, installation or process-control paths introduced.

Final automated and artifact results are recorded in
.verification/local-ai-v1.4.17/artifact.json and tests.log/build.log.
Actual in-app cold/warm multi-tool reports on Qwen/LiteRT still require checking;
unit tests do not establish device timing or solve the slow third pass.

Final result:626 Java tests passed,0 failures/errors/skips (47 local limits/UI
checks included in that total). ARM64 assembly and runtime gate passed. ZIP CRC,
package com.termux, minSdk26,1.4.17-test/code1123, ARM64-only payload and unchanged
signing certificate verified. All native libraries and external-agent assets match
v1.4.16-test. Main source files match final build stage. Limited secret scan passed.
Only classes2.dex and AndroidManifest.xml changed among shared APK entry names.
SHA-256: 3aa2b45dde2e19abcdaf082c5314d9018260cd76d3b3928ac261a4b31706211f
