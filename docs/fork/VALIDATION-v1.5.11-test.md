# v1.5.11-test validation

- Full Java verification passed: app 630 cases, terminal-emulator 145 cases; zero failures/errors/skips. Logs: `.verification/local-ai-v1511/tests-final.log`, `test-counts.json`.
- 15 standalone real-SDK interface tests passed with fake inference. Added checks for thinking/channels and greedy sampler settings, then restoration of ordinary defaults. This does not prove native hardware inference or model obedience.
- Three synthetic-run tests cover five-case sequencing, capture without dispatch, errors/cancellation/restoration and read-only rejection. Report access uses synchronized snapshots. Diagnostic workflow has no Runner/tool dispatcher. Remote overrides reset per generation; original settings restored in finally. No permission bypass added.
- Release ARM64 build and required runtime-class audit passed. Build emits existing Kotlin metadata compatibility diagnostics; successful DEX/class audit does not establish device UI compatibility.
- ZIP CRC, duplicates/traversal, com.termux identity, versionCode1148, minSDK26, ARM64, private AI flag, v2 signing certificate, limited embedded-secret scan, existing native libraries/agent assets and seven QNN/LiteRT payloads checked.
- No weight, installed environment, user session or external-agent changes. No reset/migration introduced. Package/certificate match previous delivery. No publication.
- Device checks pending: run all five cases on Gemma, export/reopen report, Stop/cancel followed by ordinary chat, and verify navigation/report UI. Synthetic requested actions are never executed. A correct tool choice is not proof of successful file writing.

APK SHA-256: `5cb645290eb923b50a90caa4fdeeb30ec97c6d0df979bb9f468ce309c0e8e3ff`
Size: 104887496 bytes. Download copy verified independently.
