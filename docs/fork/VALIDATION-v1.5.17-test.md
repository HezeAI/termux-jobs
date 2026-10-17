# v1.5.17-test validation

## Evidence and remaining checks

Private build. Device Qwen/NPU performance and UI/DPI checks remain pending; automated fake inference does not establish model correctness or hardware speed.

22 checks using the installed real LiteRT SDK classes with fake inference passed. Five added integration cases exercise retained structured-tool continuation, labeled counted feedback, actual API metrics, rights/chat/schema/sampler/history/reply-limit invalidation, unavailable metrics, SDK errors, cancellation/closure and leaked-call rejection. Existing17 schema/native-role/protocol/recovery/media-configuration checks also pass. Log: `.verification/local-ai-v1517/sdk-results.log`.

Native builds for compatible/fast Android JNI and Termux/Ubuntu runners passed. A real tiny-model test forces snapshot fallback without suffix trimming: before-feedback checkpoint restores505/901tokens vs406/901for the old tail64 location; both match fresh next-input logits (observed max difference0). Invalid feedback positions are rejected. Test source: app/src/test/cpp/local-ai-feedback-cache-test.cpp; log: feedback-cache-results.txt. This test does not prove real Qwen hybrid speedup. Fast Ubuntu runner smoke passed token count/generation/warm cache/early cancellation/recovery/benchmark/quit; additional runner/loader results are recorded below after completion.

## Review and state tracing

llama path: only app-produced transient feedback is marked at the final message; role/token probing provides a conservative checkpoint, and reuse still compares full token prefixes plus owner/chat/system. Same128MiB snapshot budget. No change to executing tools, source model weights, permission enforcement or stopping user processes. JNI argument and JSON runner changes are packaged together.

LiteRT: only same-key exact one-message append may reuse. Expected canonical history and actual SDK history are kept separately, because prior app feedback remains inside actual SDK context and must be budgeted. Original user/tool text is preserved via internal runtime metadata. External/history input is sanitized before comparison. Compaction/non-prefix history, reply-reserve/settings changes and media recreate state. Error/cancellation paths close or invalidate retained state; normal close unloads it. SDK auto tool calling remains false and all actions still route through app validation/approval. SDK metric failures are isolated from inference; numbers are bounded metadata only.

UI: only dropdown row style/size restored, retaining vector icons and correct selected state. Main selector/server button/creation and preset labels unchanged. No persistence schema change, reset or deletion.

## Corrections during verification

Initial broad runs found assertions expecting the previous compact row dimensions and the previous three-field runner envelope. Updated the tests to cover restored natural height/screen-bounded width and the explicit feedback position. A follow-up UI expectation needed the same narrow-screen bound used by the popup. These were test expectation corrections for intentional UI/protocol changes; final results must be confirmed below before delivery.

## Final verification and delivery

Final full Java suite passed:670 app +145 terminal-emulator cases, zero failures/errors/skips. Five SDK-cache unit cases cover exact append/runtime feedback budgeting, changed history/media, required correction/tool roles, invalid finishes and forged input annotations. Added per-pass SDK/CPU diagnostic coverage; restored dropdown checks pass with narrow-screen width bound. Logs: tests-verified.log and test-counts.json.

Final22 real-SDK/fake-inference checks pass after the last source change (sdk-results-final.log). Compatible/fast Termux and Ubuntu runner smoke passed, plus CPU capability gate/loading/namespaces/hidden exports checks (runners-other-smoke.log, runner-smoke.log, jni-loader.log). No running user model/session was used for these isolated probes.

Release build and required packaged runtime-class audit passed. Existing Kotlin metadata and static glibc-link diagnostics remain in successful logs; no new unresolved runtime definitions. ZIPCRC, safe entries, com.termux, minSDK26, code1154, ARM64-only ABI, private local AI flag, v2 certificate and limited embedded-secret scan passed. All six rebuilt JNI/CPU runners match independently stripped source-build outputs; unrelated native payloads, QNN libraries and external-agent assets unchanged.

APK SHA-256: `caac912a2871aa228d1005dc0032380c6c37ab61f9cc5903e7544e3b91cd7744`. Size:108729347 bytes. Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`. Final Download copy independently hashed. No persistence migration/reset. No GitHub publication.
