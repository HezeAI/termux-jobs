# v2.0.5 development validation — attachments and sign-in

Unreleased developer build, 2026-10-23. Published stable remains v2.0.1.
Android8+/ARM64, com.termux, unchanged signer/schema8, code1329.
No data reset or automatic environment migration. Optional encrypted Gemini keys
are profile/device-bound; duplicate/import clears them and requires re-entry.

## Relevant tests

122 scoped JVM tests passed, zero failures/errors/skips. Covers shared attachment
staging, binary/APK input, distinct agent namespaces, managed APK materialization,
read-only FileProvider access, file-only sends/ack retention, native-session scoping,
Codex first-login terminal/completion polling, conditional auth fields, JCE encryption
with profile/domain AAD, randomized ciphertext, duplicate clearing, JSON roundtrip,
private one-time handoff, failed-environment cleanup and unrelated-file retention.
Also includes existing chat/focus/history, CLI, preparation and backup-import checks.
The last header placement edit was subsequently compiled in the release build;
its actual dimensions/appearance are a phone check.

21 adapter/native-fixture suites passed in node-tests.log. After final Gemini root
routing changes, the Unix/IPC attachment test and all3 pinned hook transformations
were rerun and passed. Additional real CLI fixtures passed:

- Claude2.1.296 against a loopback-only synthetic provider: image content reached
  the native provider request, Android projection omitted base64, resume retained
  the answer; permissions, dedup, Shell and owned-stop checks also passed.
- OpenCode2.0.28 isolated owned server: a local file URI was decoded as native PNG
  and persisted in the durable inbox with resume=false, without model inference.
- The baseline includes Codex0.163.0 native image/streaming/history fixtures.

First-login tests use native protocol mocks, not a real account login. Gemini
checks cover generated @ references, pinned bindings and controller IPC; they do
not establish real model image interpretation or account eligibility. Keystore JCE
and UI checks are Robolectric/source evidence, not an Android Keystore device test.
No real account API requests or credential-content reads were used for these tests.

## Review and state tracing

Reviewed per-agent staging roots and control binds, regular-file/realpath checks,
atomic conversation claim, stale native-session rejection, send dedup and ambiguous
ACK retention. Traced draft removal versus retained submitted history, output card
materialization, async owner checks and explicit install permission/confirmation.
Images remain native media; log samples are bounded and original paths retained.
Video frames have timestamps/no audio; files are never silently sent through Shell.

Reviewed Codex account/read -> explicit terminal login -> owned completion/cancel
-> reconnect -> dedicated-terminal dismissal. Codes/URLs do not enter chat snapshots,
model context or the new status projection. Existing account-free providers remain
supported. Normal terminal and owned-process stop mechanisms retain their boundaries.

Reviewed Gemini masked edit -> optional ciphertext -> launch-private handoff ->
read/unlink -> child environment. No plaintext key enters generated shell args or
CLI settings/status JSON. Scoped cleanup handles bootstrap/setup/proot failures;
synchronous launch rollback removes the handoff. Abrupt Android kill before read
can leave a private handoff until the next same-profile launch; private runtime
backups remain sensitive. Agents/tools with equivalent app/environment access are
not a separate security boundary. No active session was reconfigured for testing.

Evidence: .verification/all-agent-attachments/{tests-final.log,node-tests.log,
gemini-socket-final.log,gemini-hooks-final.log,claude-native.log,opencode-native.log}.

## Phone checks still needed

Check file-only and text+image/log/video/APK sends in each updated helper, previews,
permission prompts, cancel/removal and resumed history. Verify Codex first login on
an unauthed profile without clearing current auth; confirm Gemini key storage,
replacement/removal/blank fallback and wrong-key terminal explanation. Check the
red cross, compact context window outside dismissal and header at actual screen DPI.
No current user sessions/environments were stopped or modified to simulate a clean
installation. X11/renderer/native libraries are outside this change.

## Final artifact

- APK: `build/apk/Termux-Jobs-v2.0.5-arm64.apk`
- Size: 45828398 bytes
- SHA-256: `b9c6aec2ff795eae7f6c60175016c049071766fa3ec9d3ae3fe4f17176c17a49`
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
- ZIP integrity, package/version, non-debuggable flag and ARM64-only ABI passed.
  Signing identity matches v2.0.4 and code1329 exceeds1328.
- All changed packaged adapter/shared assets equal source; all native .so files
  equal v2.0.4. Bounded assets/DEX key-pattern and private-file/development-path
  scans reported no findings; this is not exhaustive secret detection.
- Runtime inventory:13600 definitions, required definitions present,31 previously
  reviewed optional references. Release Gradle build passed; existing custom lint
  registry/Kotlin metadata warnings still limit lint coverage.
- Final-APK-derived update metadata: `.verification/all-agent-attachments/update.json`.
  No GitHub publication is part of this task.
- Download copy matches the final SHA-256. No data migration/device-update test is
  inferred from matching package/signature or source unit tests.

Artifact evidence: build.log, apk-runtime.json, apk-badging.txt, apk-signature.txt,
artifact.json and verify-final.log under the same verification directory.
