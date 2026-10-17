# v1.5.12-test validation

## Evidence and scope

Source reproduction before the fix: `.verification/litert-tool-diagnosis/AdapterReviewProbe.java` and `adapter-review-results.txt`. The original short Russian request with a Gemma fragment yielded one generation, zero executions, no repair flags, and unchanged raw output. This is a coordinator reproduction with synthetic inference, not a claim to reproduce native generation.

16 standalone real-SDK interface tests pass (`.verification/local-ai-v1512/sdk-results.log`). Inference is fake: the actual SDK Message/Config classes are used. The added adapter test feeds leaked native syntax through callbacks, verifies `unparsed_native_tool`, disabled automatic execution, closed conversation and unchanged draft. This does not test NPU hardware or establish why the model omitted its closing marker.

Automated recovery cases cover the reported wording, exact marker classification, quoted/prose examples, hidden draft presentation without execution, correct fresh SDK response, two-retry bound, chat/read permissions, approval, cancellation, Qwen/ordinary-text behavior, rejection of unstructured JSON in native mode and feedback function identification. Native retry context excludes broken assistant fragments and fake tool-result messages; original user request stays intact. Final suite results are recorded below after completion.

## APK

Release ARM64 build and required runtime-class audit pass. Existing Kotlin metadata compatibility diagnostics remain in build output; device compatibility is not inferred from successful DEX compilation.

ZIP CRC, duplicate/traversal checks, com.termux identity, minSDK26, versionCode1149, ARM64-only ABI, private local-AI flag, v2 signature/certificate, limited embedded-secret scan, seven QNN/LiteRT payloads, unchanged earlier native payloads and agent assets pass. Certificate matches previous private delivery. No reset/migration. No publication, user-process changes or installed environment changes performed.

SHA-256: `54571ddb341361072f37e48252935eab8a34987953cf36aa8bbe32c94caec516`
Size: 104888496 bytes.

## Remaining device checks

Repeat the original natural-language guide request in a fresh Gemma Native chat at the same reply cap. Export diagnostics and verify the resulting file. Check ordinary follow-up, cancellation during repair, and permission/approval UI. Recovery can stop clearly after two retries; it does not guarantee the model will generate a complete call. No token-limit diagnosis or weight changes claimed.

## Final suite and delivery

Full final Java suite: 640 app cases, 145 terminal-emulator cases; zero failures/errors/skips. All ten Gemma recovery cases pass. Logs: `.verification/local-ai-v1512/tests-verified.log`, `test-counts.json`. Earlier compilation checks caught Java escaping and a test exception declaration; both corrected before this passing run. Final APK Download copy independently SHA-256 verified.
