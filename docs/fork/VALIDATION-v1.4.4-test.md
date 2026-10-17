# v1.4.4-test — local validation

Package com.termux, versionName1.4.4-test/code1109, minSdk26/targetSdk28,
arm64-v8a. Certificate unchanged: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK 53015819 bytes, SHA-256 `e6a6281a38871d7ad519169b1e80c262916d4f0a9eee5416265c37e86ef5cdcb`.

## Verified

- Final JVM suite: **500 tests, no failures/errors/skips**. New coverage exercises
  the 3-mode × 2-scope × 5-tool policy matrix, command rejection including pwd/git/
  python/bash, refusal before approval/runner, model instructions matching available
  tools, warning cancellation/acceptance/repeated toggling/outside dismissal.
- Existing file tests cover in-project relative/absolute paths and symlink escape;
  OFF supports outside file paths while Read continues to reject writing/commands.
- Release build and packaged DEX runtime-definition gate passed. Legacy Lint
  emits modern Kotlin metadata compatibility diagnostics; no clean-Lint claim.
- ZIP CRC, duplicate/path checks, package/version/ABI/signature and limited private-
  file/secret-pattern scan passed. Exact final source/stage Java/resources/assets/
  local-C++ comparison passed. All native libraries equal v1.4.3-test byte-for-byte;
  server-agent and baseline Termux native payload also remain unchanged.
- Reviewed both authorization sites: coordinator gates before approval, script
  construction gates before ProcessBuilder. Allowed structured file tools remain
  quoted fixed scripts; no executable-name allowlist or new isolation is used.
- UI keeps the effective selection/checkbox enabled while the warning is pending.
  Only explicit positive confirmation changes it to OFF; modes are not changed.
- Baseline1.3.4 recovery code1110 payload matches the original except manifest/
  signing metadata. Recovery SHA-256 `8264b3f45570b91405b9573f436427d7e8e440a54aface152df4b887151a9e4e`.

## Compatibility and remaining checks

No reset/migration: action mode, scope and confirmation persist independently.
Saved OFF profiles stay OFF. Behavior change: ON profiles can no longer run shell
commands even in Full access. To run commands, explicitly turn Path checks OFF
and accept the warning; Full access still required. This does not grant Android
root/storage permissions, expand Read/Chat, or create a security sandbox.
Canonical file checks remain subject to same-UID concurrent directory races;
separate isolation was explicitly excluded from this task.

Device checks still required for warning layout/Back/touch dismissal and actual
model behavior in both engines. In a disposable project, check project-only reads/
writes and a blocked command, warning Cancel/accept, Read+OFF external reads but
blocked writes/commands, Full+OFF allowed actions and independent Yes/No setting.
Existing keyboard/import/scroll/model/backend limitations remain in LOCAL-AI.md.
NPU runtime, llama GPU, image/audio/MTP and local model/chat backup-manager coverage
are not added. No effect on Codex/SSH/other terminal action modes is intended or
claimed device-tested. No GitHub publication.

Recovery cannot open local chats and may discard optional preset fields on save.
Future APK after recovery must have versionCode>=1111. Evidence is in the ignored
`.verification/local-ai-v1.4.4/` directory.
