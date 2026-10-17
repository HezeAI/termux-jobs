# Validation — v1.4.20-test / code1126

Changed behavior: schema-error feedback/correction, cached-read metadata and bounded
repeat guard, CPU selector wording. Native libraries are unchanged from1.4.19.

Relevant model-coordinator cases: invalid filter followed by corrected list, actual
returned-path read and final answer; retry exhaustion with zero invalid execution;
a corrected write still denied in read mode; cancellation after feedback prevents
a further generation; embedded invalid prose is never executed; repeated successful
read performs one physical runner read, marks two cache hits and stops before a
fourth read. UI cached label uses explicit execution metadata, not attacker-controlled
file text. Existing explicit refresh, cache invalidation on mutation, schema strictness,
permission/scope, context, native budgets and external-agent tests remain included.

Review: catch only request-validation IllegalArgumentException; generated internal
fast-path requests still expose programmer errors, chat-only still denies tools,
authorization failures are not retried. Invalid calls are recorded failed/unexecuted,
not sent to runner. Corrected calls take ordinary authorization and approval paths.
Correction records are excluded from future turn context/recall. Model data and
execution labels do not confer new permissions. Cancellation is checked at turn-loop
boundaries; no native/process shutdown changes.

Retry count is per user turn and bounded to2 corrections. Read guard applies only
to successful same-path non-refresh cached reads: two cached repeats are allowed,
then turn stops with an explicit missing-final-answer message. Fresh read/mutation
resets relevant accounting. It is not a general proof against every possible agent
loop; explicit refresh and other tools remain separate paths.

Final automated/artifact results are in .verification/local-ai-v1.4.20. Real model
correction quality, Android card rendering and actual file workflow still require
user device checking. Unit tests do not establish that Qwen will always choose
valid parameters or complete an arbitrary multi-step task.

Final:642 Java tests passed,0 failures/errors/skips (51 local UI/limits tests are
included). ARM64 APK/runtime gate passed with15899 definitions and30 reviewed
unresolved types. Existing Kotlin/build warnings remain. APK identity com.termux,
1.4.20-test/code1126/minSdk26/ARM64, unchanged certificate, CRC/paths/duplicates,
source-stage match and limited secret scan passed. Both CPU libraries, other native
payload and external-agent assets match1.4.19. SHA-256: 0bc7abae7d2d1f3b7f5eff3e35d8ce5156866007da9fca717fb1edb859b94098
