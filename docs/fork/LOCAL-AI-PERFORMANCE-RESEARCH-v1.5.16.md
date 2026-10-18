> Historical experimental feature: removed from the current v1.5.22-test source/APK. This document is retained as research evidence, not current functionality. See [removal notes](RELEASE-v1.5.22-test.md).

# Local engine performance investigation after v1.5.16-test

Research snapshot: 2026-10-18. Original engine/app sources, installed environments, model files and running sessions were not modified. No APK or GitHub release for this investigation. Reproduction artifacts are under `.verification/engine-research-v1516/`.

## Evidence from developer reports

Qwen35-2b / llama / CPU ARM dotprod+fp16+i8mm /6threads /Ubuntu /8192context /512reply:

- Before: report806945256034412762,181406ms total, five passes, all cached0, sum prefill166964ms.
- After stable-system change: report7281398394140515414,95777ms total, five passes, first cold then1423tokens reused via prefix_snapshot, sum prefill82799ms. Later passes still recalculate818/912/1006/1101tokens. These are observed runs with different histories/model actions, not a controlled benchmark guaranteeing1.89× speedup.
- Current test action order is imperfect: two reads precede the write; no read-back after write. Final file independently matches expected text. Speed improvement does not establish complete instruction adherence.

## 1. Conversation snapshot boundary: strongest next cache candidate

Current native cache has a base snapshot at the stable system boundary and another snapshot64tokens before the end of the complete input. Updated app progress is appended to each runtime input but not retained as a model conversation message. A long progress block makes the near-end snapshot include changing feedback. The next prompt can reuse only the base snapshot. Qwen hybrid/recurrent memory cannot freely remove an arbitrary suffix beyond its configured rollback capacity; snapshots are therefore significant.

A tokenizer-only probe uses the repository's actual Qwen35 vocabulary, current production template helper and synthetic tool history. With app progress shaped like the actual application, the current tail snapshot does not match the next request. A checkpoint immediately before the feedback role boundary matches the real common prefix:

| Step | Next input | Base snapshot | Current tail (does not match) | Candidate checkpoint / exact common prefix |
|---|---:|---:|---:|---:|
|1|492|297|378|351|
|2|542|297|428|401|
|3|592|297|478|451|
|4|642|297|528|501|

Logs: prefix-results.jsonl; source: prefix-analysis.cpp. This is a synthetic token-boundary test, not the developer's prompt or a full Qwen forward pass.

A separate real-inference test uses the tiny stories260K repository model and the production jobs_prefix_cache with trimming forcibly disabled to exercise snapshot fallback. Current tail64 restores406/901tokens; checkpoint before feedback restores505/901tokens. Both restored results match a fresh next-input evaluation (max observed logit difference0). It does not execute model tools. Source/results: checkpoint-probe.cpp /checkpoint-results.txt. The first test harness exceeded its configured n_batch; its test context batch was corrected to2048 before the successful final comparison. No production batch value changed.

Minimal implementation to test next: supply an explicit transient-feedback boundary to JNI and external runner; compute a conservative token prefix before that role boundary, capture/replace the existing tail snapshot there, and reuse only with exact token-prefix matching plus unchanged owner/chat/system. Keep128MiB snapshot budget, cancellation/reset and permission-change invalidation. Fall back to current safe behavior when no such feedback exists. Never reuse based only on message counts, strings or positions. Validate on real Qwen hybrid memory after small-model tests. This reduces repeated input work; it does not raise raw cold-prefill throughput.

## 2. Raw cold-prefill computation: measured candidates, no speed claim yet

Production CPU contexts use n_batch256/n_ubatch128 and one thread setting for both decode and prefill. Fast builds already enable ARMv8.2 dotprod/fp16/i8mm. GGML_NATIVE/OPENMP/KLEIDIAI are disabled; GGML has its own thread pool, so OPENMP=OFF is not evidence of single-threaded execution.

Two isolated benchmarks link the existing fast Ubuntu GGML archives. They use representative Qwen recurrent dimensions (state128,16heads from earlier exported model metadata), and a synthetic Q4_K2048×2048 matrix multiply. They run32/128/256token shapes and4/6/8threads, forward and reverse, with one warmup and median of three timed repeats. Output is finite with matching reported checksums across thread counts. These are individual CPU operators with graph/thread-pool overhead, synthetic data and no model weights; they do not identify the dominant operator in a whole-model prefill.

At128tokens:

| Operator |4threads forward/reverse ms|6threads|8threads|
|---|---:|---:|---:|
|Gated Delta Net|8.346 /9.479|7.335 /7.245|17.065 /16.502|
|Q4_K matrix multiply|10.042 /9.730|7.492 /7.080|19.964 /19.285|

Files: gdn-bench.cpp, gdn-results-forward-reverse.jsonl, matmul-bench.cpp, matmul-results.jsonl. More threads are not an automatic improvement. Larger token blocks show better amortized operator work in some cases, but full-model temporary memory, attention/recurrent scheduling and correctness must be measured. Existing in-app input benchmark mode2 already compares256/128 against512/256 on the actual loaded model; mode1 compares threads. Do not change all models' defaults based on these microbenchmarks.

Next raw-compute work: profile the actual model's prefill operators, compare existing batch settings on identical cold input, then evaluate optimized ARM matrix kernels/KleidiAI in an isolated build if the relevant quantized multiplies dominate. A kernel rewrite requires correctness/logit tests as well as speed measurements. Do not modify quantization/weights as a supposed transparent engine optimization.

Current upstream GGML CPU GDN was fetched for comparison. Differences include recurrent snapshot/state layout; they are not evidence of a straightforward faster drop-in kernel. Do not copy that kernel alone into the pinned backend.

## 3. LiteRT-LM/NPU is a separate optimization path

The app retains the engine but creates/closes Conversation per inference with full input history. The installed0.17.0 SDK exposes Conversation.getBenchmarkInfo(), getTokenCount() and ExperimentalFlags.enableBenchmark. BenchmarkInfo includes last prefill/decode token counts and rates. This corrects the earlier broad statement that such metrics are unavailable: they are not currently collected by our adapter; actual NPU backend support is still untested.

Evidence: sdk-cache-api.txt and engine-bytecode.txt from the installed AAR's classes. Current upstream Conversation source also documents these APIs. The experimental benchmark switch is global to that SDK, read when initializing the engine, and must not be toggled casually around active sessions. A diagnostic build should enable it in the dedicated inference process before initialization, collect metrics before closing a conversation and handle unsupported/error results without breaking inference.

Potential reuse: retain a conversation within an unchanged owner/chat/system/tools configuration and send only verified appended messages/tool responses. Recreate on non-prefix history, changed rights/schema/settings, compaction, media mode changes, errors, cancellation or stop. Native tool-call history and auto-tool execution boundaries must remain intact. Current SDK cancelProcess comments mention rollback limitations; cancelled state must not be reused blindly. First verify the pinned SDK lifecycle with fake-inference tests and then actual NPU. Do not assume a faster Gemma/NPU run proves cache hits or equivalent model quality.

## Distribution approach

A separately versioned open-source engine adaptation repository is feasible. Prefer reproducible source patches/build scripts, Android ARM64 JNI/AAR, Termux Bionic runner and Ubuntu/glibc runner artifacts, plus ABI/API/CPU requirements, protocol version, SHA-256 and third-party notices. Keep compatible CPU fallback and explicit accelerated variants. No credentials, histories, private projects or model files belong in such releases. Direct in-app download/execution needs a separate Android loading/signature/compatibility design; shipping a runtime as part of the signed APK remains the current mechanism.

Verified upstream source notices: llama.cpp MIT; LiteRT-LM Apache2.0. Keep attribution/license/notice and mark changes. NPU vendor binaries (Qualcomm/QNN/QAIRT) have separate terms and are not automatically covered by either open-source license. Model licensing is separate too; this investigation does not authorize publishing vendor binaries or weights.

Primary sources fetched read-only to the ignored research folder (SHA-256 saved in upstream-SHA256.json):

- https://github.com/ggml-org/llama.cpp/blob/master/LICENSE
- https://github.com/ggml-org/llama.cpp/blob/master/ggml/src/ggml-cpu/ops.cpp
- https://github.com/google-ai-edge/LiteRT-LM/blob/main/LICENSE
- https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/java/com/google/ai/edge/litertlm/Conversation.kt

Web search tool returned HTTP403; these upstream files were retrieved directly from raw.githubusercontent.com. Current-branch source is used as research, not substituted for verification of our pinned versions.
