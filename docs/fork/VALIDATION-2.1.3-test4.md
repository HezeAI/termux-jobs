# Private test APK 2.1.3-test4

2026-11-04. VersionCode 1480. Signed release variant for local phone acceptance.
No push, tag, publication or APK installation is authorized by this build task.
The published v2.1.2 APK, tag, update metadata and Components remain unchanged.

**The PNG accumulation/disconnection fix is not included.** The previous Codex
20-image cold-reopen result without an answer within 240 seconds is the baseline
for a separate investigation, not a successful fix or a conclusion for other agents.

## Included changes

- New Codex profiles use native `untrusted` command approval. Existing profiles
  preserve their saved policy. This does not isolate the project or promise that
  every file tool requires confirmation. The matching RU/EN label says so.
- Gemini API-key handoff uses a one-shot abstract local socket bound to a run and
  verified terminal process identity/ancestry. The vault decrypts lazily for the
  matching consumer. No new plaintext handoff file is written. Old files are not
  removed. Existing authorization storage and API/Vertex versions remain unchanged.
- Both full and selected backup forms explicitly warn in RU/EN that the backup
  is unencrypted and may contain secrets. Consent and archive format are preserved.

These are the separate local fixes recorded in the
[second-stage audit](SECURITY-AUDIT-STAGE2.md). No old profile, working authorization,
agent permissions, native history or installed Ubuntu AI is rewritten by this work.

## Verification scope

Evidence: `.verification/release-2.1.3-test4/` in the active checkout. Final results
and artifact identity are recorded below after packaging. JVM tests use the debug
variant internally; only the signed release APK is delivered. No debug APK is
published. Fixtures use artificial data and do not prove real model authorization.

Native UI-chain evidence from stage two is reused only with matching recorded
source hashes (13 recorded production/harness inputs). Its 25 cases include 18
actual application Allow/No cases, native schedulers/tools, synthetic files and a
kernel-enforced local-provider network guard. Native Codex/Claude observations
remain historical with the limitations stated in the audit. This build does not
repeat those CLI/model executions or claim independent security certification.

The 95 recorded native source/configuration inputs match the previous release.
Three native libraries are reused byte-for-byte; X11 packaged runtime inputs retain
their mandatory hash check. Java/resources/script assets are repackaged using the
existing local SDK, Gradle and dependency caches. No new third-party component,
license, binary URL or component version is introduced. Existing notices must be
present in the final APK. No clean-room dependency download or GPU execution is claimed.

## Checked anew and final APK

- 122 scoped JVM tests, 0 failures/errors/skips; task executed (not UP-TO-DATE).
  Covers saved/new profile policies, generated launch/resume commands, Gemini
  vault/broker ownership, cancellation/retry, settings labels and backup consent.
- Node abstract-socket transport: artificial key, run binding, malformed/truncated
  reply, unavailable/crashed broker, successful new run. Legacy handoff compatibility
  and Vertex auth/config tests pass. No real credentials or model network calls.
- Eight synthetic release-signer cases pass: missing key/pin/alias, wrong password,
  certificate mismatch, correct existing fixture key and mismatch/retry. An initial
  unittest invocation imported the standalone script but discovered zero tests;
  that invocation is not counted. The script was rerun directly and succeeded.
- Full modern Lint 7.4.2, five modules: **0 errors, 236 warnings**, no rule-loading
  or infrastructure error. The compile SDK beyond AGP's tested range remains.
- Final signed release: `com.termux`, `2.1.3-test4`, versionCode **1480**,
  **40,461,417 bytes**, ARM64 only, non-debuggable, APK v2 signature verified.
- File SHA-256: `64222843dbd54ccd41cb2992f29875502aba6c2124c6585f2cdd3a466291ea04`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP CRC and outer-container structure pass; seven ELF payloads are ARM64.
  All 120 assets match source/pinned bytes; 571 selected Java/resource/asset source
  files match the packaging stage. DEX has 14,196 definitions including
  GeminiKeyBroker, required runtime classes and 31 reviewed optional unresolved
  types. Notices remain included; no removed microphone feature or test key/harness
  is packaged. No key or cache files are admitted by the package inventory.
- `verifyX11Inputs` and `verifyReleaseSigner` executed and passed in release build.
  Same package/public certificate as test3 and stable v2.1.2; higher versionCode.
  This identity comparison does not replace actual update/data acceptance.

The first verifier invocation hit a non-executable local SDK wrapper; the same
apksigner JAR was then run successfully with the local JDK. A preliminary ZIP
scanner counted nested bootstrap headers; the corrected check examines gaps only
in the outer APK. Neither preliminary check is counted as a pass. Existing legacy
D8 Kotlin metadata diagnostics remain in the successful packaging log; modern Lint
is the independent static-analysis gate, with final DEX inventory checked separately.

## Short phone acceptance plan

Use separate test profiles and disposable directories containing artificial data.
Do not change old profiles or working authorization. A fake key cannot establish
successful provider login; never paste a real key into diagnostics or this report.

1. **Gemini authorization:** start an API test profile with an artificial key.
   Check that handoff succeeds or gives a clear ownership/vault error. Provider
   rejection of the fake key is expected. Separately check the unchanged Vertex
   sign-in state. Actual successful login needs the user's separate test account.
2. **Cancel and retry:** cancel a test-owned Gemini start before and during handoff,
   then retry. Check that only that run stops and a second test run keeps its own
   channel. Do not delete old key files. No new plaintext handoff file should appear.
3. **Process recovery:** only in an isolated test app instance without working
   sessions, finish its process during handoff. Reopen the test profile and retry.
   A stale channel must fail closed; a fresh run must establish its own channel.
   This specifically tests real Android sockets, peer identity, Keystore and proot.
4. **Agent approvals:** in new artificial profiles verify offered Allow/No cards
   for shell and direct file tools, inside/outside the test project. Compare file
   bytes after rejection and permission. A command rejection alone does not prove
   file-tool protection. Codex untrusted and native auto-allowed/rejected paths
   are distinct; OpenCode may first request its own external-directory consent.
5. **Backups:** check RU and EN, full and selected modes. Confirm the unencrypted
   warning and explicit consent. Export only test data, restore into a disposable
   target, and compare markers. Check cancellation without altering working data.

Real phone lifecycle/transport, actual successful authorization, complete SAF/rootfs
restore and full third-party CLI traffic remain unverified. This APK does not close
the separate attachments/tool-context task or constitute a full security audit.
