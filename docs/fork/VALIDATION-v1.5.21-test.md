# Historical private build records — v1.5.21-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.21-test validation

Private rollback build, not published. Production app/native source paths exactly match pre-LoRA checkpoint6a9ab547; only app version/name differs. Proof: baseline-source-check.json and removal-check.json. LoRA class, profile field, UI, IPC and added native entrypoints removed. Owned checkpoint downloader stopped; no LoRA training steps or trained adapter. No user process, installed environment, Bridge/shared SDK or server service modified. Preparation data retained under ignored repository directories.

Full final Java suite (tests-final2.log, full-results/, test-counts.json):685 app +145 emulator tests, zero failures/errors/skips. New compatibility case reads the optional localLora key from a newer profile, preserves session identity/name/model/chat reference/rights/threads/location/dedicated tool environment, and omits the removed key on serialization. First fixture used the generic default Ubuntu PTY environment for a local-AI profile, which is invalid; corrected fixture to the actual local-AI host environment termux with dedicated tools proot:jobs-local-ai. No production change made to bypass validation.

Native CPU libraries restored from the already verified v1.5.19 APK, not newly recompiled. Final packaged ALL native libraries compare byte-for-byte to v1.5.19 (payload-check.json); no changed vendor/runtime binaries. Original v1.5.19 and restored v1.5.21 packaged DEX contain no LocalAiLora class or LoRA menu. Final production Java matches APK staging. Existing Codex/Gemini/Claude/OpenCode/server assets unchanged. No new native performance claim or repeated inference benchmark.

Artifact: com.termux, versionName1.5.21-test, versionCode1158 >1157, minSDK26, ARM64. ZIP CRC/unique paths/version/ABI/private flag/v2 signature/limited secret scan and packaged runtime inventory pass. APK bytes:104918560. SHA-256:ae20c62e9808cc3c40b1b95ec01653b37d0151c840f23aad65d1cef08b1f5002. Certificate SHA-256:dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644. See artifacts.json and .verification/arm64/apk-runtime.json. Download copies independently hashed.

Data format remains8, no reset/migration/deletion. Existing model files/chat files untouched. Optional removed adapter field ignored; unrelated saved settings retained. This verifies the exercised profile round-trip, not every possible installed data state. Current implementation restores prior behavior and does not fix the known model's inaccurate negative claim about a completed write. Actual chat/preset opening, keyboard, gestures and model inference still require a developer device check on this APK. Check absence of the LoRA setting after update and unchanged previous chat/environment/rights. No GitHub publication authorized/performed.


---

## Subsequent development record

# v1.5.21-test — Claude Android chat

Private test update to the developer-accepted v1.5.20-test/code1182 baseline.
The baseline APK and a restore map of affected baseline sources are preserved.
No GitHub release or installed-app change is included in this task.

## Resulting behavior

New Claude sessions use the shared Android transcript and bounded presentation
cache with their own native stream-json controller. Replies identify Claude;
reasoning/tool/approval receipts use inspectable pills, and supported native
questions use forms. Native modes/model changes update the captured profile after
acknowledgement. Status includes actual last-request input/output tokens, without
claiming subscription limits. Main input invokes native structured user messages.
Interactive-only Shell executes a direct Ubuntu command and saves app-side output;
it does not insert that output into the model's native conversation.

Robot opens the existing compact native terminal window with full-size keys,
Back left and Enter right. Unauthenticated launches use native auth login; after
login finishes the SDK reconnects. Full CLI is deliberately not killed by Back:
exit it through /exit or Ctrl+D before sending in the Android chat again. A visible
RU/EN explanation remains in the terminal window, with a corresponding main-chat
notice while native CLI owns the session. Busy model/tool/background work prevents
handoff. No absence-of-hooks shortcut is used to terminate a native process.

Old running Claude retains the original controller until explicitly restarted
with the same native conversation. No native history-format migration/data reset
is introduced. Accounts, auth paths, pinned CLI, bootstraps and unrelated agents'
assets are unchanged. See [CLAUDE-ANDROID-CHAT.md](CLAUDE-ANDROID-CHAT.md).

## Evidence and review

Evidence is under `.verification/claude-chat-v1.5.21/`.
Node unit tests cover streamed projection, request dedup, nonce/session/epoch
permission ownership, once/deny/cancel, questions, bounded receipts, main history
chain, foreign/subagent exclusion, busy handoff and private metadata.

The actual pinned Claude Code 2.1.296 was run against a loopback-only synthetic
Anthropic Messages API with isolated HOME/config/project and a synthetic key.
Initialize, model/mode control, native streamed text/tools, denial with no file
creation, allow-once with native test-file creation, duplicate admission suppression,
Shell without model inference, persisted JSONL and reopening the exact native chat
passed. No actual cloud model query or account authentication is claimed.

A separate lifecycle test uses real owned process groups and deliberately
SIGTERM-resistant synthetic workers/descendants. Back preserves native workers
with and without hook confirmation; forged hook tokens are rejected. Simulated
explicit native exit resumes the SDK. Explicit stop removes owned children/socket
while an unrelated process survives. No user session was used as a test fixture.
The unchanged legacy Claude controller and OpenCode/Gemini protocol, receipts,
permission, native frame and mode tests also pass.

JVM checks cover separate Claude transport/session identity, approval nonce/session,
cache isolation from Gemini, send scope, full native keys, busy handoff notices,
visible /exit instructions and existing OpenCode/Gemini behavior. An initial new
notice assertion used exact-text lookup with a substring and was corrected; final
results are recorded below. Previous native synthetic-provider loop failures were
fixture errors (repeated tool requests after denial), corrected before the passing
native protocol and resume runs.

Review: process ownership and stop/parent-loss paths; late worker launch guards;
fresh permission controls and identity checks; native history/path bounds; profile
ACK ownership; tab switching/cache isolation; auth data excluded from status;
Shell output kept separate; unchanged unrelated assets/native libraries.

Device checks still required: Claude account sign-in/browser handoff, real provider
response/errors, arrows/Enter and IME, native full-CLI exit, switching while streaming,
scroll retention and permission/question interaction. Source, mock UI and synthetic
provider checks do not establish physical phone behavior or real-model quality.

## Final artifact

Final focused JVM suite:82 tests, zero failures/errors/skips. Twelve Node regression
entrypoints pass, in addition to the real-native protocol/resume and lifecycle
checks above. Release assembly and required runtime-definition verification pass;
31 optional missing types retain explicit review notes. Existing Lint Kotlin/UAST/
JNA tooling warnings remain; this is not a clean-Lint claim.

Package com.termux, version1.5.21-test/code1183, minSdk26, ARM64 only.
Same signing certificate as1.5.20/code1182, increased versionCode. ZIP integrity,
v2 signature, staged-source identity and private-file/secret-marker checks pass.
All existing packaged assets and native libraries are byte-identical to1.5.20;
only three new Claude controller/chat/hook assets were added. No local AI runtime.

APK: `Termux-Jobs-v1.5.21-test-arm64.apk`, 36609110 bytes.
SHA-256: `256c594fcebd508f92909c85810343fd91cd1d2360934450a638174ba255a77b`.
Copied into Download and copy hash verified. Existing baseline1.5.20 remains.
Evidence: tests.json, java-delivery-verified.log, node-delivery.json,
native-resume-verified.log, native-fOgbsm/result.json, lifecycle-verified.log,
build.log, staged-source.json, apk-audit.json, signature.txt, badging.txt,
delivery.json in the evidence directory.
