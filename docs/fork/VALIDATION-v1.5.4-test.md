# Private v1.5.4-test validation

Final APK: code1141, com.termux, ARM64, minSDK26, 104864440 bytes. SHA-256: d681359bd9641571c8545df484ef0d50c36be0c84a35185bfe4c42594ebc3b8a. Existing certificate dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644, v2 signature verified. ZIP CRC, package/version/ABI, DEX private gate, reviewed runtime definitions, limited secret-file/marker scan and unchanged pre-existing native/external CLI/server-agent payloads passed. Download copy independently hashed.

Full application run: 592 tests, 591 passed and one new history test failed on JSON slash escaping. A prior new server test also needed its fixture profile registered in the saved-profile list. Test-only fixes followed: LocalAiContextTest and ServerWorkspaceTest passed together, with terminal-emulator and terminal-view suites; termux-shared has no tests. See targeted-summary.json. Real SDK0.17.0 interface probe: 10 passed under Java21, fake engine/conversation, no hardware inference. Source diff check passed.

Review covered native invalid-argument validation and correction, own-chat recall without inferred filesystem actions, disconnect profile/activity isolation, blocking late callbacks and newly queued transports, picker callback invalidation, explicit reconnect, and keeping independent terminals/remote tasks outside local shutdown. No working user process or remote server was manipulated for these checks.

Device checks remain: connect/disconnect/reconnect, transfer/media interruption, delayed Android storage permissions, actual keyboard/tile appearance, native model repair/recall and actual image/video/audio inference. SDK/unit tests do not establish NPU or media hardware behavior. Compiled NPU context remains4096. No automatic data reset or published release.

Evidence: .verification/litert-servers-v154/{build.log,tests-rerun.log,tests-targeted.log,full-suite-summary.json,targeted-summary.json,sdk-tests.log,context-budget.txt,1.5.4-test/artifact.json}. Context counts are character-based estimates, not actual tokenizer counts.
