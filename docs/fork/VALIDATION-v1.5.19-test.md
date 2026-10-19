# Historical private build records — v1.5.19-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.19-test validation

Final private artifact verified and delivered. Device tests below remain required.

Review scope: Java-owned SDK history retention and shared local-agent completion check. No native SDK/runner/weights/UI layout changes. No other assistant sessions or environments modified. No GitHub publication.

SDK state tracing: preserve full canonical expected input plus output independently of the fitted actual SDK input plus output. Compare the next full canonical source exactly before expanding actual runtime. A dropped old message edited later invalidates reuse. Budget the expanded actual history, retain current prefix/reply cap checks, clear on errors/cancellation/media/changed rights. Compaction is now an explicit canonical-to-actual mapping, not permission to accept an arbitrary suffix or ignore historical edits.

Completion: ledger is new per run and records successful actual tool callbacks only. Wording check runs on a final non-tool answer for an action request, not in the dispatcher. One correction, no invented action. A corrected tool call passes the same authorization, approval and cancellation checks. A repeated unsupported claim returns an unconfirmed result. Invalid model success claims are marked repair and excluded from future model context. They are not relabeled as successful tool steps. No automatic semantic verifier, shell interpretation, target/content/order verification or extra unconditional inference is claimed.

Initial broad tests exposed a noun/verb false positive ("перед удалением"), trailing punctuation on "Done.", and an existing test expecting the old immediate failed-write wording. Tightened Russian finite forms and completion punctuation handling; added discussion/state regression coverage. Failed-write test now requires one bounded correction, then unconfirmed completion after the model repeats its unsupported claim. Existing history-recall behavior must pass unchanged. Initial SDK harness getter corrected to actual ConversationConfig.getInitialMessages; no SDK source/binary change.

SDK25 real-class/fake-inference checks pass (sdk-results-final.log): new actual-fitted history retention, exact full canonical continuation, changed dropped-message invalidation, no automatic tool execution and closure on reporting failure. Retention eligibility is now a candidate until metrics/staging/cache bookkeeping succeed; callback failures close the conversation rather than leave SDK state ahead of adapter metadata. This error-path change is exercised by the SDK harness against final production sources.

Full suite after completion-check corrections:684 app +145 emulator tests, no failures/errors/skips (tests-final.log, test-counts.json, full-results/). Nine new completion checks cover current-turn evidence, failure/deletion, negation/discussion/examples/prose writing, generic completion, both native coordinator protocols, bounded false success, locations-only, permissions/approval/cancellation and chat-only mode. Canonical-vs-actual cache mapping unit test includes historical edit/rights invalidation. Existing memory recall passes unchanged. A final focused rerun covers the current agent/cache/timing sources after diagnostic-phase UI wiring; its final result is recorded below.

## Final artifact

Focused final-source88 tests passed (tests-focused.log, focused-counts.json), with final source hashes identical in test and APK staging. Build, packaged runtime definition audit and final ZIPCRC/package/ABI/version/private-flag/v2-signature/limited-secret scan pass. Code1156 exceeds1155 and certificate identity matches. All native payloads and agent assets unchanged; every native library also compared byte-for-byte with v1.5.18. No native binary rebuild or model inference benchmark claimed. ZIP native alignment properties preserved. APK size decrease is due to removal of roughly3.8MB of inter-entry ZIP padding versus prior package; compressed payloads are unchanged except DEX growth. See size-comparison.txt and alignment-comparison.json. Existing Kotlin metadata warnings remain in successful build logs.

APK bytes:104918676. SHA-256:`4d3dcfa22282ca1451d3a30c806e94a476b48b869ebe1ba989b89d2019978b51`. Certificate:`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`. Download copy atomically written and independently hashed.

No database schema or preset/history migration/reset. Running user environments, shared SDK, Bridge, model files and processes unchanged. No GitHub publication. Remaining: actual Qwen/Gemma task performance, SDK reuse rates, quality of claims, ambiguous wording and UI/device interactions. Wording guard does not prove exact targets/content/order or arbitrary shell side effects. The actual SDK tests use fake inference and do not prove NPU speed.


---

## Subsequent development record

# v1.5.19-test — clear chat connection/errors and early native terminal access

Private update to1.5.18; built/audited/copied to Download. Copy hash verified.
Package com.termux, version1.5.19-test/code1181, minSdk26, ARM64 only,
36587686 bytes. Certificate matches1.5.18/code1180, versionCode increased.
SHA-256 `e3301d1a66b858517c81d09235fc73b1a429eb2ed7a5abb23723a5f39a9f7515`. No data reset or history migration.

- Empty connected OpenCode/Gemini chat explicitly shows local session connected /
  send a message. It is not proof of remote API access. Pending startup explains
  what to check in the robot terminal. No-connection notice describes automatic
  retries and offers a read-only snapshot retry on tap.
- Native API error rows receive RU/EN guidance with original details retained on
  tap. Known region/model/auth/quota/context/overload/network errors are distinct;
  unknown errors are not guessed. Legacy native F12 hint is explained for chat.
- Unconfirmed action feedback remains across polls and keeps the user's draft;
  actions/prompts are never automatically resent. Ambiguous results may still
  arrive, so the notice asks users to check execution before retrying actions.
- Robot replaces the packages button immediately for selected Gemini/OpenCode,
  before capability resolution. Window opens without a successful control RPC,
  with full-size native keys and original footer. Bridge-specific native open
  happens asynchronously; unavailable bridge shows a local terminal explanation.
  Close queues owned terminal-close after open. No process restart or auth change.

Final focused JVM suite passes65 tests, zero failures/errors/skips. New tests cover
error guidance and unchanged original details in both agents, native hint text,
empty-ready status/failure-recovery, unconfirmed-send persistence/no automatic
resend, immediate robot window with blocked/unavailable transport for both agents
and large keys present/restored. Existing Shell/session-generation/permission/key
restoration and Gemini bridge tests remain green. Mocked tests are not phone tests.

Review traced control vs provider-error states, safe read-only retry, stale-owner
responses, action dedup behavior, asynchronous window open/close order, local view
and native-key restoration. No bridge assets or policies change. These edits do
not correct a provider region refusal or make an unavailable model accessible.

Device checks: start/preset with no bridge yet; robot should appear/open immediately;
confirm large arrows/Enter and return. Empty connected chat should invite a message.
Provider errors should be readable and show original details on tap. Confirm a
working reply and Shell; then switch sessions/keyboard with the native window open.
These authenticated/device-dependent scenarios remain for developer testing.

Release assembly succeeds in51s. Required runtime definitions pass,31 optional
missing types retain explicit review notes. Existing Lint Kotlin/UAST/JNA tooling
warnings remain; not a clean-Lint claim. Final staged changed sources match the
originals. ZIP/package/version/ABI/v2 signature and private-file/secret-marker
checks pass. All assets/native libraries are byte-identical to1.5.18; no agent
bootstrap or transport scripts changed. No APK installation/publication.
Evidence: .verification/chat-status-v1.5.19/{tests.log,tests.json,build.log,
staged-source.json,apk-audit.json,signature.txt,badging.txt,delivery.json}.
