# Historical private build records — v1.5.17-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.17-test validation

## Evidence and remaining checks

Private build. Device Qwen/NPU performance and UI/DPI checks remain pending; automated fake inference does not establish model correctness or hardware speed.

22 checks using the installed real LiteRT SDK classes with fake inference passed. Five added integration cases exercise retained structured-tool continuation, labeled counted feedback, actual API metrics, rights/chat/schema/sampler/history/reply-limit invalidation, unavailable metrics, SDK errors, cancellation/closure and leaked-call rejection. Existing17 schema/native-role/protocol/recovery/media-configuration checks also pass. Log: `.verification/local-ai-v1517/sdk-results.log`.

Native builds for compatible/fast Android JNI and Termux/Ubuntu runners passed. A real tiny-model test forces snapshot fallback without suffix trimming: before-feedback checkpoint restores505/901tokens vs406/901for the old tail64 location; both match fresh next-input logits (observed max difference0). Invalid feedback positions are rejected. Test source: app/src/test/cpp/local-ai-feedback-cache-test.cpp; log: feedback-cache-results.txt. This test does not prove real Qwen hybrid speedup. Fast Ubuntu runner smoke passed token count/generation/warm cache/early cancellation/recovery/benchmark/quit; additional runner/loader results are recorded below after completion.

## Review and state tracing

llama path: only app-produced transient feedback is marked at the final message; role/token probing provides a conservative checkpoint, and reuse still compares full token prefixes plus owner/chat/system. Same128MiB snapshot budget. No change to executing tools, source model weights, permission enforcement or stopping user processes. JNI argument and JSON runner changes are packaged together.

LiteRT: only same-key exact one-message append may reuse. Expected canonical history and actual SDK history are kept separately, because prior app feedback remains inside actual SDK context and must be budgeted. Original user/tool text is preserved via internal runtime metadata. External/history input is sanitized before comparison. Compaction/non-prefix history, reply-reserve/settings changes and media recreate state. Error/cancellation paths close or invalidate retained state; normal close unloads it. SDK auto tool calling remains false and all actions still route through app validation/approval. SDK metric failures are isolated from inference; numbers are bounded metadata only.

UI: only dropdown row style/size restored, retaining vector icons and correct selected state. Main selector/server button/creation and preset labels unchanged. No persistence schema change, reset or deletion.

## Corrections during verification

Initial broad runs found assertions expecting the previous compact row dimensions and the previous three-field runner envelope. Updated the tests to cover restored natural height/screen-bounded width and the explicit feedback position. A follow-up UI expectation needed the same narrow-screen bound used by the popup. These were test expectation corrections for intentional UI/protocol changes; final results must be confirmed below before delivery.

## Final verification and delivery

Final full Java suite passed:670 app +145 terminal-emulator cases, zero failures/errors/skips. Five SDK-cache unit cases cover exact append/runtime feedback budgeting, changed history/media, required correction/tool roles, invalid finishes and forged input annotations. Added per-pass SDK/CPU diagnostic coverage; restored dropdown checks pass with narrow-screen width bound. Logs: tests-verified.log and test-counts.json.

Final22 real-SDK/fake-inference checks pass after the last source change (sdk-results-final.log). Compatible/fast Termux and Ubuntu runner smoke passed, plus CPU capability gate/loading/namespaces/hidden exports checks (runners-other-smoke.log, runner-smoke.log, jni-loader.log). No running user model/session was used for these isolated probes.

Release build and required packaged runtime-class audit passed. Existing Kotlin metadata and static glibc-link diagnostics remain in successful logs; no new unresolved runtime definitions. ZIPCRC, safe entries, com.termux, minSDK26, code1154, ARM64-only ABI, private local AI flag, v2 certificate and limited embedded-secret scan passed. All six rebuilt JNI/CPU runners match independently stripped source-build outputs; unrelated native payloads, QNN libraries and external-agent assets unchanged.

APK SHA-256: `caac912a2871aa228d1005dc0032380c6c37ab61f9cc5903e7544e3b91cd7744`. Size:108729347 bytes. Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`. Final Download copy independently hashed. No persistence migration/reset. No GitHub publication.


---

## Subsequent development record

# v1.5.17-test — Gemini Android chat and single native key panel

Private APK built, audited and copied to Download. No GitHub publication.
Package `com.termux`, versionName `1.5.17-test`, versionCode1179, minSdk26,
ARM64 only, 36583330 bytes. ZIP integrity and v2 signature verified. Certificate
matches v1.5.16-test/code1178; versionCode increased. No user-data reset or
history-format migration. SHA-256:
`e7cd24f29501e482b75631650ecfc33bc1395bfc20946ce6938f9676bc5aa33a`.
Delivery-copy hash verified.

## Changed behavior

- Gemini now uses the shared Android chat view through its own native integration.
  Main input invokes native model submit directly, with no terminal paste/Enter
  simulation. Interactive-only Shell uses native shell execution lifecycle.
- Upper Gemini model/stats/rights card remains. The former packages button becomes
  the same-size robot opening its native terminal. Auth/complex dialogs remain
  native. Connecting/failure/retry appears inside the chat and drafts are retained.
- Replies have the Gemini prefix. Reasoning summaries/tools/permission receipts
  use inspectable pills. Existing native confirmation/policy checks remain.
- Both OpenCode and Gemini native windows retain the configured large extra keys,
  including arrows. The duplicate compact Esc/Tab/arrow row is removed. Back to
  chat stays left and Enter right; Enter does not dismiss. Outside taps do not
  dismiss. Closing Gemini's window keeps its owned native CLI alive.
- Native resume invalidates old acknowledgements; clear/rewind removes obsolete
  projected rows. Native history/authentication storage formats are unchanged.

## Automated evidence

Evidence directory: `.verification/gemini-chat-v1.5.17/`.

Full JVM suite before final refinements passed566 tests (412app+154emulator).
Final focused suite passed58 tests, zero failures/errors/skips, including Gemini
native-session routing and stale send guard, Shell/main isolation, window/key
ownership restoration, connection failures/recovery, native clear and batched
bounded UTF-8 socket reads. Mocked UI tests are not phone gesture/IME tests.

Ten Node scripts pass: Gemini chat projection, all3 pinned native hook variants,
real Unix/child IPC with synthetic callbacks, existing live model/mode/privacy/
owned-stop integration, Gemini permissions/receipts/frame modes, shared receipts
and OpenCode chat regression tests. Syntax checked on transformed pinned bundles;
original SDK bytes remain unchanged. No production model inference requested.

Isolated actual pinned Gemini CLI authentication-screen probe passes: native chat
hook mounted, auth fallback requested, opening/closing window control retains the
owned CLI. No user's keys or running sessions used.

Release assembly succeeds in51s. Existing Lint Kotlin/UAST/JNA infrastructure
warnings remain; this is not a warning-free Lint claim. APK runtime definitions
verified,31 optional missing types have explicit review notes. Final staged Java
sources match source files. Package/version/ABI/signature, duplicate/path checks,
private-file and secret-marker scans pass. Native libraries and unrelated assets
(including OpenCode, Codex, Claude, bootstrap, server and backup helpers) are byte
identical to v1.5.16. Gemini adds gemini-chat.cjs and updates its3 bridge assets.
Whitespace check passes.

## Review and remaining device checks

Reviewed independent agent transport/auth/history ownership, native callback
permission gates, native-session and generation changes, retry/dedup admission,
window open/close restoration and bounded projection/transport. Codex/Claude/SSH
and ordinary shells do not acquire the new chat frontend. Gemini CLI/Ink still
runs internally; hiding the Android terminal view is not a measured removal of
all native rendering overhead. Display limits never delete native saved history.

Restart Gemini with its existing chat after updating to load new copied assets.
Developer should check an authenticated streamed answer, tools and once/deny
confirmation, Shell with interactive mode off/on, robot-window large arrows and
Enter/Back, login/folder-trust fallback, keyboard/scroll, switching tabs and
resuming the existing chat. These phone-dependent checks remain unverified.
