# v2.0.2 — Agent chat improvements: release validation

Prepared2026-10-23 for developer-authorized publication after phone checks.
This public release combines private builds1326–1330 under the next public version
following2.0.1. Source/runtime behavior matches tested private1330 apart from release
version metadata. A stale environment-routing test fixture was corrected to use a
valid profile UUID; production validation was not relaxed.

## Artifact and update compatibility

- APK: `build/apk/Termux-Jobs-v2.0.2-arm64.apk`
- Package: com.termux; versionName2.0.2, versionCode1331; Android8+/ARM64; schema8.
- Size: 45828682 bytes
- SHA-256: `436a7803f9b4c93ad2c7c0dd25bf0bc22b9565c6f269a0d2e9278016301e1571`
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
- ZIP integrity, non-debuggable package/version, ARM64-only ABI, unchanged signer
  and higher code than public1325/private1330 passed. Every assets/ and native lib/
  entry byte-matches the developer-tested1330 APK. All changed packaged helper
  files equal current source. No X11/driver/environment runtime change is included.
- DEX runtime inventory:13600 definitions, all required definitions present,31
  reviewed optional references. Release Gradle build passed; existing custom lint
  registry/Kotlin metadata warnings still limit lint coverage.
- Bounded assets/DEX key-pattern and private-file/development-path scans passed;
  these do not claim exhaustive secret detection.
- SHA256SUMS and final-APK-derived update.json are generated in the release evidence
  directory. Updater source uses versionCode for availability: public1331 can update
  private1330 even though its public versionName is numerically lower.

No data reset, automatic environment migration or auth-store replacement is
required. New optional Gemini ciphertext preserves schema8 defaults and is bound
to device/profile: duplicate/import clears it and requires re-entry. Restart agent
sessions with saved chats after updating to load helpers. Android may stop app
processes during an update; finish important work first. Matching package/signer
is source/artifact evidence, not a complete real-device migration test.

## Automated tests

- Broad JVM run:897 app tests,896 passed and one failed because an old environment
  fixture used the literal profile ID fixture. It now uses a valid UUID while
  retaining the deliberately short synthetic Codex socket ID. Final targeted
  AgentEnvironments/GeminiCli/GeminiApiKeyVault run:15 passed, zero failures/errors.
  The full897-test run was not repeated after this test-data-only correction.
- Full terminal-emulator suite:154 passed, zero failures/errors. Earlier final
  shared-chat/auth/backup checks passed122 scoped JVM tests, including per-agent
  attachment ownership, file-only sends/ACK retention, media/APK card handling,
  read-only FileProvider, Codex login-terminal polling and Gemini encryption.
  Thumbnail/file-manager/media tests and limits are preserved in historical records.
-21 adapter/native-fixture suites passed during implementation. Final release
  reruns of the production OpenCode Unix controller, Codex device-login protocol,
  Gemini private handoff and shared all-agent attachments all passed.
- Real Codex0.163.0 app-server used a private fixture account/config and loopback
  provider to verify localImage->input_image, streaming and persistent history.
- Real Claude2.1.296 used a private fixture HOME and loopback provider; native image
  blocks reached its request, history resumed and Android projection omitted base64.
- Real OpenCode2.0.28 admitted/decoded a local PNG through the production root factory
  and persisted the native inbox entry with resume=false, without model inference.
- OpenCode integration regression: helpers extracted from private1329 rejected
  an attachment before HTTP admission. Corrected production Unix/HTTP wiring passed
  nested connection/receipt paths, file-only/mixed sends, >2MiB media replies/inbox,
  bounded cards, dedup, foreign/missing/Shell refusal, ordinary-send recovery,
  unchanged non-media response limit and owned controller/server shutdown.
- Gemini pinned UI hooks, Unix/IPC attachment routing and native @ reference
  generation passed isolated checks. They do not prove actual model interpretation.

No real model/account requests or reading of live authorization content were used
for these fixtures. Tests use synthetic keys/data; actual Android Keystore,
permissions, gestures, display and account eligibility are separate phone checks.

## Review and state tracing

Reviewed stage/export destinations, regular-file/realpath and conversation claims,
per-run drafts, explicit message versus Shell transport, dedup, failed/ambiguous
ACK retention and retained submitted originals for history. Traced background
thumbnail/copy callbacks across selection changes and viewer dismissal. Returned
local cards never fetch remote URLs or automatically install; APK installation
uses an explicit read-only cached FileProvider URI and Android consent.

Codex account/read gates first login, URL/code prints only in its terminal, and
completion/cancel is scoped to the owned login ID. Existing account-free providers
retain their path. Gemini keys use a separate AES-GCM Keystore alias/profile AAD,
private one-time0600 handoff in0700 directory, read/unlink before CLI spawn and
child environment transport. Keys do not enter shell args, chat or status/settings
JSON. Cleanup handles synchronous/setup/proot failures. An abrupt kill before read
can leave a private handoff until next same-profile launch; private runtime backups
remain sensitive. Equivalent app/agent-environment access is not an isolation
boundary against the configured agent. No unrelated user processes were stopped.

## Developer phone evidence and remaining limits

HezeAI confirmed Codex image input and, after the root correction, OpenCode file
sending works. These are developer-reported device results, not inferred from mocks.
The developer also reviewed the attachment/preview workflow while testing private
builds. This does not establish every file/model/device combination.

Fresh Codex device login, Gemini encrypted-key replacement/fallback on real Android,
Gemini/Claude live-provider media, and the full storage-permission/gesture/DPI matrix
remain unverified. Video uses at most12 sampled timestamped frames, no audio; short
inter-frame events may be missed. Binary/APK originals are supplied by path for
agent tools rather than decoded as model text. Codex output cards cover images/APKs;
other updated agents also project supported local file links. Native model and CLI
permission limits still apply. No fresh X11/renderer revalidation is claimed for
this agent-chat release.

Evidence under .verification/release-v2.0.2: tests.log, full-initial-results/,
tests-final-targeted.log, node-final.log, build.log, apk-runtime.json,
apk-badging.txt, apk-signature.txt, artifact.json, verify-final.log, update.json.
Earlier fixture evidence remains in all-agent-attachments/, opencode-attachments-fix/
and the archived private reports under docs/fork/development/chat-2026-10-23/.
Publication round-trip verification is recorded separately after upload; no source
tag movement or APK replacement is required for that documentation record.
