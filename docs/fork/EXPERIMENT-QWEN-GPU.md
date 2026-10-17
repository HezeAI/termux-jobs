# Qwen3.5 2B: isolated OpenCL GPU experiment

2026-10-17. Snapdragon8Elite/Adreno830, Android OpenCL3.0, llama.cpp b9222, exact verified Q4_K_M copy. ARM-optimized6-thread CPU, context8192. All test processes exited successfully. Main app/running sessions/Ubuntu packages/default native payload unchanged. This does not add a GPU option to the APK.

| Run | Prefill2048 tokens |
|---|---:|
| CPU256/128 |22.184s|
| GPU256/128 |32.739s|
| GPU512/256 |24.676s|
| GPU1024/512 |17.461s|
| GPU1024/512 repeat |17.304s|

Best GPU took about21–22% less prefill time than this CPU control. Default-size GPU batch was slower; do not generalize device enumeration or GPU presence into speed. Best GPU compute buffer493MiB vsCPU124MiB; weights/OpenCL1321MiB plus CPU mapped embeddings also present.

Generation microtest:37-token prompt counting1..20; greedy16-token response truncated intentionally. CPU prefill0.422s/generate0.730s; GPU1024/512 prefill0.308s/generate0.997s. Same visible prefix1,2,3,4,5,6. CPU remains faster in this short generation test. Earlier GPU256/128 generation1.582s. Do not equate16-token microtest with sustained response throughput. Simple GPU2+2 test also returned4.

Mechanism: weights offloaded26/26layers; OpenCL lacks fused Gated Delta Net paths so fallback graph is used, while CPU fused paths are enabled. Candidate cause, not isolated causal proof. Larger GPU batches helped substantially.

Limits: one CPU control, two best GPU prefill runs, live-phone scheduling/thermal conditions uncontrolled; shader/model/context initialization excluded; no native Qwen tools/history/permission/GUI integration test, no sustained long generation/4k-input test. APK namespace/driver access and external Ubuntu/glibc runner compatibility not established; Bionic experimental executable only. Future integration should use separate opt-in GPU backend with CPU default/fallback, model/capability checks and existing authorization retained. No APK built/installed or GitHub publication.

Evidence: .verification/local-ai-gpu-probe/ (sources, pinned Khronos headers, build logs, runtime logs, results.json). Verified model copy SHA-256 unchanged: 57a1085840f497d764a7fc5d346922dbde961efb54cc792ea81d694fd846a1d8
