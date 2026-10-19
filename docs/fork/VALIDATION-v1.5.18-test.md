# Historical private build records — v1.5.18-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.18-test validation

Private artifact verified and copied to Download. Real model/device checks below remain required.

Changed paths: owned LiteRT fit priority and metadata diagnostics; Qwen generated native-call prefix parsing. No schema/permission/approval/path/loop bypass. No source model weights, running sessions, installed environment, Bridge, shared SDK or external-agent assets changed. No GitHub publication authorized.

Review: optional progress may be dropped only while preserving canonical messages, schema/system/correction and chosen response reserve within existing budget. Fallback fitting and conservative runtime-prefix validation remain. Cache diagnostic reason strings are fixed codes, not raw keys. Qwen prefix parsing is not an intent detector; native blocks are executable proposals only through the existing authorization pipeline. Multiple/trailing/nested/ill-typed calls remain errors. Tests must include example rejection, approval denial and read-only command denial.

Initial test launch used the x86 SDK AAPT2 default and failed its daemon startup on ARM64 before tests. Rerun uses the existing ARM64 AAPT2 via command-local FORK_AAPT2; no SDK files changed.

## Final verification

Full Java suite:674 app +145 terminal-emulator tests, zero failures/errors/skips (tests-verified.log; test-counts.json). Added coverage for optional-progress-before-history, reset reason codes and per-pass report output, template-compatible Qwen leading explanation including inline filename formatting, rejection of examples/fences/trailing/multiple/malformed calls, approval denial and read-only command denial.23 actual LiteRT SDK class/fake-inference tests pass, including real callback metrics/reuse/rights invalidation reasons before pass closure (sdk-results-final.log). Final cancellation-reason adjustment was compiled and exercised by this SDK harness after the full-suite staging; it changes only reported metadata, not cancellation semantics.

Initial isolated SDK harness required Java21, Android35 API and Gson2.14, matching existing pinned SDK dependencies; incomplete-classpath/compiler attempts were corrected locally without system installation. They were harness failures, not NPU results.

Final release build and required packaged runtime definitions audit pass (build-final.log, arm64/apk-runtime.json). Existing Kotlin metadata warnings remain in successful builds. Final staged Java source hashes match all edited production classes. All native libraries and external-agent assets are unchanged; no native rebuild or model benchmark is claimed. APK ZIPCRC/entry safety, com.termux, minSDK26, ARM64-only, private local AI, code1155, v2 signature, limited embedded-secret scan pass. Certificate matches1154; Download atomic copy independently hashed. No persistence/schema reset or migration introduced. Existing histories/presets/rights retained.

APK bytes:108729563. SHA-256:`37c50d666a7789bb56a74b6725fb11f469e50bfd3722f0fdfdfea2228c893afd`. Certificate:`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Remaining device checks: repeat original Qwen write/read and two Gemma requests, inspect reset codes, latency and content. Parser formatting tests do not establish model judgment; prefix example detection is conservative syntax/wording filtering, not a universal intent detector. Actual NPU caching/speed and keyboard/gestures require device checks. No process stop, model modification or GitHub publication performed.


---

## Subsequent development record

# v1.5.18-test — restore full-size native terminal keys

Private test update to1.5.17; no GitHub publication. Built/audited and copied
to Download; copied hash verified. Package com.termux, code1180, version1.5.18-test,
minSdk26, ARM64, 36583538 bytes. Same signing certificate as1.5.17/code1179.
SHA-256 `0fa4422b25f597ca7fffdd27f109b49397c923b42a85953d20a18ea954893b09`.

The shared OpenCode/Gemini window previously skipped keys when the hidden toolbar
pager had not created its ExtraKeysView. An existing but empty view also provided
no buttons. The dialog now constructs a populated full-size native panel in either
case, using configured keys or the two-row ESC/Tab/Ctrl/Alt/arrows/Home/End/Page
fallback. Layout height uses the actual row count. It temporarily registers this
panel for native modifier reads and restores the exact original reference on close;
a populated existing panel keeps the previous reparent/restore path.

Back to chat remains left, Enter right without dismissal, outside taps disabled.
No small duplicate arrow row. Shared change applies to both OpenCode and Gemini.
Bridge assets, native permission rules, model selection and history formats are
unchanged. Gemini's reported unavailable model and duplicate Thinking text are
outside this change; no user's running session/configuration was changed.

Regression tests cover cold startup with no toolbar view for both agents, actual
14 populated buttons/all4 arrows/native key client/80dp2-row panel, no compact row,
footer and dismissal reference restoration. The existing empty-panel/window test
also verifies populated buttons and original parent/keyboard restoration.
Mocked tests do not prove physical key interaction, IME or device clipping.

Device check: reopen both robot terminal windows with the main key toolbar hidden,
verify large arrows select a native menu item, Enter accepts it, Back returns to
chat, then verify the ordinary session's key panel and Ctrl/Alt after return.
No data reset or history migration is introduced.

Final focused JVM suite:59 tests, zero failures/errors/skips. Release assembly
and required DEX runtime checks pass. All packaged assets and native libraries are
byte-identical to1.5.17; final staged AgentChatUi matches source. ZIP integrity,
package/version/ABI, v2 signature and private-file/secret-marker audits pass.
Existing Lint Kotlin/UAST/JNA infrastructure warnings remain; no clean-Lint claim.
Evidence: .verification/terminal-keys-v1.5.18/{tests-verified.log,tests.json,build.log,
apk-audit.json,signature.txt,badging.txt,staged-source.json,delivery.json}.
Review traced missing/empty/existing panel creation, native modifier registration,
async window dismissal, original pointer/parent/client restoration and agent
isolation. Initial test required draining Android's asynchronous dismiss callback;
final test verifies state after that callback. Device key/IME tests remain above.
