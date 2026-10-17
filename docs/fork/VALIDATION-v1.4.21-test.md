# Validation — v1.4.21-test / code1127

Relevant native test uses a real tiny GGUF: complete length matrix, forward/reverse
thread/batch plans, callback cancellation, temporary-context cleanup, unchanged
working-context positions/logits and continued decoding afterward. Loader test
verifies nativeBenchmark exports in both independent CPU JNI namespaces. It does
not establish Qwen throughput or old-device behavior.

Java coverage: report counters, process-vs-wall timings, exact throughput, unknown/
malformed metadata rejection, stopped partial results/reset, explicit test modes;
benchmark gate early/active/late stop, unrelated-token stop, overlap rejection,
and100 concurrent stop/finish races that must leave the next chat uncancelled.
Existing generic diagnostic/server pause/export UI paths remain in the full suite.

Logical review: native benchmark takes only an owned model handle/mode/thread count;
no chat/system/tool data enters it. New per-trial context uses shared model weights,
separate sequence memory and RAII cleanup. Main context is not cleared/decoded.
Engine uses the same owning CPU namespace for benchmark and normal operations.
Private service validates mode/token/owner; cancellation gate serializes activation,
stop and final flag reset. Early stops are remembered; late stops do not set the
chat cancellation flag. Normal CANCEL behavior is unchanged. UI guards loaded/
idle ownership, blocks send/settings/model changes, and queues close after work.
No process signals, user Linux environment or shared SDK modifications introduced.

Metadata is bounded and separately atomically persisted off the UI thread. Closing
the report leaves collection running; Stop explicitly requests batch-boundary
cancellation. Other DiagnosticPanel sources keep their existing default controls.

Device gaps: actual Qwen length/thread/batch timings, Android lifecycle/keyboard/
dialog rendering and OOM/old-device behavior require in-app checking. Temporary
contexts use additional RAM while weights are shared. A native process crash may
lose the loaded local model; unrelated terminals are not a benchmark target.

Final verification: 651 tests passed, no failures/errors/skips. APK build and runtime dependency gate passed (15912 definitions, 30 reviewed unresolved types). ZIP CRC, package com.termux, versionCode 1127, arm64 ABI, existing signing certificate, staged source correspondence and limited secret scan passed.
SHA-256: `c0478d4e62c41d4a00e80443e46737716f33d0a90c9916e445ab26fbdf2517b8`.
Device Qwen benchmark remains pending.
