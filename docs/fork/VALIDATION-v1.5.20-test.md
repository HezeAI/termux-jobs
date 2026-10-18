# v1.5.20-test validation

Private build; no GitHub publication. Scope: managed optional llama.cpp LoRA import/selection/attachment, removal of the duplicated main CPU/GPU hint, shared native tool descriptions and workflow regression tests. No actual LoRA training or new model weights. No changes to running environments, shared SDK, Bridge or user sessions.

## Tests and review

Final full Java suite (tests-final4.log, full-results/, test-counts.json): **691 app +145 terminal-emulator**, zero failures/errors/skips. Includes managed import/copy/hash/default-Off/session-copy/invalid-ID/cancellation checks and LoRA UI availability; both native-protocol round trips for screenshots/APK paths; dedicated-Ubuntu shell/dependency/build routing; opt-in Termux requirement; read/project shell rejection even with a selected adapter; controlled read-mode install gate. These are adapter/authorization tests, not evidence of real model understanding or actual apt/Gradle execution in the user's local-AI environment. The test's initial direct JSON-string comparison wrongly depended on key order; fixed to compare keys/values, final suite passes. Earlier overlapping staging preparation produced test-helper class-loading failures; final runs were sequential and clean. Managed-import tests now use a self-contained header fixture, not an ignored external file.

25 real LiteRT SDK class/fake-inference checks pass against final sources (sdk-results-final.log): SDK structured-tool/cache/lifecycle behavior. This does not measure NPU inference or prove model quality. LiteRT/vendor binaries are unchanged; its LoRA control is disabled.

Six CPU binaries rebuilt: baseline/fast JNI and baseline/fast Termux/Ubuntu runners. Bionic dlopen test verifies CPU gate, both JNI namespaces including the new adapter symbol, and hidden internal llama exports (jni-loader.log). All four runner smoke checks pass generation/cache/cancel/recovery/benchmark/quit. All four runner LoRA checks pass baseline cache, attachment/cache invalidation, generation, repeated-attach rejection and quit (runner-lora.log).

Real upstream native API test with stories260K and handcrafted rank-2 adapters passes: zero delta adapter changes logits by0; nonzero fixture changes maximum logit by0.335198402; removal restores baseline; incompatible shape rejects. These fixtures are explicitly untrained, tiny-model checks, **not Qwen adapters or training/quality evidence**. No GGUF/JSONL or fixtures are embedded in the APK.

A Java-host functional JNI probe was blocked by Android linker namespace after Java canonicalized the repository path to /root (jni-test-final.log). No linker/installed-app changes attempted. Symbol loading, direct native API and runner tests pass separately; the installed app's embedded JNI attachment still needs a device test with a genuine matching adapter. External runners were tested as isolated executables, not by changing/starting the user's installed dedicated Ubuntu.

Review: adapter ID is validated; service derives managed paths, verifies base association/size/hash/header. Registry is separate from model catalog; base GGUF untouched. Base association is user-declared: shape compatibility cannot establish exact training provenance. Import cleanup affects only newly generated partial/destination files. Max256MiB and bounded metadata parsing. Full payload/tensor compatibility is native-loader responsibility. UI resets adapter on base switch; adapter change participates in reload. Off is backward-compatible optional field. CPU-only gate rejects LiteRT/GPU attachment. Failed external attachment closes only its owned runner; failed JNI attachment frees context/adapter/base. Native free order is context → adapter → base; attachment clears cache; switching adapter reloads. Existing tool authorization and selected-container routing remain authoritative, independent of adapter or prompt. PRoot is not a security sandbox.

No new persistent rights or chat format changes. Existing session/preset/history data is retained; optional localLora defaults empty. Codex/Gemini/Claude/OpenCode assets and unrelated native payloads compare unchanged. No direct hot-loading into an existing foreign session, no tool execution from imported adapter files, no automatic fallback on adapter error.

## Artifact

Final ZIP CRC/unique paths/package/ABI/version/private flag/v2 signature/limited secret scan pass (artifacts.json, 1.5.20-test/). All six rebuilt packaged CPU binaries match independent strips of the new native outputs. QNN/LiteRT/OpenCL and unrelated native payloads remain unchanged. Packaged DEX required-runtime inventory passes (runtime-types.json). Changed production Java matches the APK staging exactly (source-hashes.json). Successful build retains existing Kotlin metadata warnings; no new runtime compatibility claim is inferred from those warnings.

PACKAGE=com.termux; versionName=1.5.20-test; versionCode=1157 >1156; minSDK26; ABI=arm64-v8a. Certificate: dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644.
APK bytes: 104926136. SHA-256: 2e1510a56c232a315c2bbbab67abdb89e73b86c09054d3ebd555601c55eeb6cd.

## Remaining device/training checks

- Existing Qwen and NPU Gemma tasks without LoRA: latest screenshot discovery, actual chosen APK metadata and allowed deeper commands in jobs-local-ai. File-card/keyboard/gesture behavior needs device observation.
- Genuine matching trained Qwen adapter: load/reload/Off, same-chat retention, actual tool quality, memory and speed for Android/Termux/dedicated Ubuntu. This build provides loader infrastructure, not trained behavior.
- Draft22 synthetic episodes (11train/11validation) have schema-checked tool arguments, no personal data. Need expansion/manual review, original base/revision identification, tokenizer/template/loss-mask parity, actual training and held-out base-vs-adapter evaluation. Training has not been started; adapter not distributed.
- No promised speedup, universal command-following, new Qwen vision or NPU LoRA support.
