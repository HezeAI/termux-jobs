# Historical private build records — v1.5.20-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

> Historical private test. LoRA pilot cancelled by the developer; superseded by [v1.5.21-test](RELEASE-v1.5.21-test.md), which restores v1.5.19 behavior. No trained adapter was produced.

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


---

## Subsequent development record

# v1.5.20-test — preserve OpenCode/Gemini chat while switching sessions

Private update to1.5.19, built/audited/copied to Download; copy hash verified.
Package com.termux, version1.5.20-test/code1182, minSdk26, ARM64,36590750 bytes.
Same signing certificate as1.5.19/code1181; versionCode increased. No data reset
or history-format migration. SHA-256:
`e4c150ca824a92d06d88d9e88d69511b8bab1443bbb7f01058f19c623581bf07`.

The prior shared view cleared rows and scroll whenever the selected terminal
changed, producing a blank Connecting screen while reading the same native chat
again. A bounded in-memory presentation cache now restores the exact profile +
thread/run + terminal-handle presentation synchronously. Native refresh follows
without clearing it. Upper workspace tabs retain the same row views. Native agent
process/history/subscription lifecycle is unchanged; no hidden polling is added.

The cache retains rows/model/status/Shell draft/scroll anchor or bottom position.
It has at most8 inactive entries,512Ki serialized characters of rows per entry,
2Mi serialized characters overall plus small metadata. Trimming/eviction affects
display only, not saved native history. No disk cache or model-context change.
Pending permission/form buttons are never cached and require fresh native state.
Real transport errors still show a notice next to retained rows; cached connection
metadata is explicitly last-known. Gemini actual native-session changes replace
old restored rows. Unsupported native interfaces still reveal their legacy view.

Review traces: ownership/run keys, cache transfer and bounded eviction, late async
result generation guards, Gemini resume invalidation, temporary dialog dismissal
and key restoration, after-layout scroll restoration/gesture cancellation and
fresh-only permission controls. No native policy/credentials/bootstrap changes.

Device checks: return between OpenCode/Gemini/ordinary sessions and upper tabs;
old rows should be visible immediately without a blank Connecting screen. Read
older text, switch away and return, then verify streaming does not force bottom.
Repeat while model replies in background; verify fresh additions, actual failure
notices and fresh permission cards. Authenticated/physical IME/gesture behavior
remains developer testing; mocked UI tests do not establish all phone behavior.

Claude migration is the next stage after this check, not included in this APK.

Final targeted JVM suite passes73 tests, zero failures/errors/skips: immediate
return with blocked transport for both agents, Shell draft preservation, fresh-only
permission controls, same-row upper-tab return with no hidden polling/stop, actual
Gemini resume invalidating old cached rows, above-bottom and bottom restoration,
cache ownership transfer/eviction and global character budget. Existing connection,
Shell, generation, error, large native-key/window and Gemini tests stay green.
Initial cache helper tests needed the existing Android/Robolectric JSON runner;
corrected final suite passes. No native/model inference test was added or claimed.

Release assembly/runtime required-definition checks pass;31 optional missing types
retain explicit review notes. Existing Lint Kotlin/UAST/JNA infrastructure warnings
remain, not a clean-Lint claim. Final staged changed sources match originals.
ZIP/package/version/ABI/v2 signature and private-file/secret-marker audits pass.
All assets/native libraries are byte-identical to1.5.19. No APK installation or
GitHub publication. Evidence: .verification/chat-cache-v1.5.20/{tests-final.log,
tests.json,build.log,staged-source.json,apk-audit.json,signature.txt,badging.txt,
delivery.json}.

## Developer acceptance

The developer confirmed this build as the working baseline after testing
("отлично!!! фиксируем!!"). This records acceptance of the current switching
behavior; it is not evidence that every device/scenario above was tested.
Next stage is Claude Android chat integration. No new release is authorized.
