# Historical private build records — v1.5.14-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.14-test validation

## Relevant testing

Full Java suite passed: 660 app tests and 145 terminal-emulator tests, zero failures/errors/skips. Ten added cases cover separated service progress, preserving mandatory instructions/correction/current request, preferring shorter service progress before a smaller reply reserve, oversized protected input, application feedback boundaries, capacity stop after completed tools versus first-request error, separate generator arguments, service-budget diagnostics and session-dropdown selection/layout.

17 real LiteRT SDK interface checks with fake inference passed, including reduced service progress in the actual conversation configuration, unchanged base system field, retained current request, automatic tool calling disabled, and no stale progress on the next request. These checks do not run a model or NPU hardware.

Evidence: `.verification/local-ai-v1514/tests-final.log`, `test-counts.json`, `sdk-results.log` and `build.log`.

## Code and state review

Both local engines receive base instructions, app-owned progress and required correction separately. Only the explicitly supplied app progress can be abbreviated/omitted; no marker-based stripping of user/custom instructions. Schemas, rights and current request remain mandatory. Existing history/tool-result fitting still uses its previous excerpt policy. Application feedback is not mistaken for a new user task when fitting or summarizing history. Each generate request overwrites the separated service fields, including empty defaults for other callers.

If context capacity fails after tools completed, the coordinator checks cancellation first, ends the turn with context_budget_stop and preserves recorded cards/results without claiming task completion. Capacity failure before any successful action remains an error. No command execution, approval, path scope, model weight, sampler, context-capacity or process-shutdown change.

Session dropdown rows alone receive the shorter shape, explicit vector icon, model subtitle and active-session highlight. Popup width, session creation and preset labels are retained. Normal terminals and external-agent assets are unchanged. No user-data reset or migration.

## Final APK

ARM64 release build and required runtime-class audit passed. Existing Kotlin metadata diagnostics remain in the successful build log. ZIP CRC, unique safe entries, package com.termux, minSDK26, private versionCode1151, ARM64-only ABI, v2 signing certificate, private local-AI flag, seven QNN/LiteRT libraries, limited embedded-secret scan, unchanged existing native payloads and external-agent assets passed.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK SHA-256: `de2d38682c268bea9beed394517eb54f87eaf62da5728b49f7ead1a51e89b592`.
Size: 104892684 bytes.

Download copy independently SHA-256 verified. No publication or changes to running sessions/installed environments.

## Device checks needed

Repeat the original locations/write/read task in a new Gemma chat with the previous settings. Inspect final file, cards and diagnostics; if capacity is exhausted, confirm an explicit stop without false completion or repeated actions. Check the corresponding llama.cpp path separately. Verify dropdown row spacing, robot/model subtitle, active selection, taps and long taps at the developer's DPI. Automated UI and fake-inference tests do not establish device layout or real-model behavior.


---

## Subsequent development record

# v1.5.14-test — private OpenCode chat UI verification

Built and audited private APK; not a GitHub release.

- Package `com.termux`, versionName `1.5.14-test`, versionCode1176, minSdk26.
- ARM64 only, 36575234 bytes; ZIP integrity and v2 signature verified.
- Certificate unchanged from v1.5.13-test; versionCode increased from1175.
- SHA-256 `8678a7da2a2a3f0bd97fa4acc0df7d205307b85330b481013797b4f93e3942e1`.
- No user-data reset/migration. Runtime definitions and asset privacy audit pass.
- Unrelated agent/bootstrap assets and native libraries are byte-identical to1.5.13.
- Full JVM suite:560 tests (406app+154emulator), zero failures/errors/skips.
  After the final legacy-capability/UI-notification refinement, all8 targeted
  AgentChatView/OpenCodeChatUi tests pass, including parent/key restoration,
  stale-owner protection, API delivery, following/reading history and legacy fallback.
- Node projection tests pass, including synthetic shell notification display and
  duplicate suppression; JavaScript syntax and git diff whitespace checks pass.
- Release assemble succeeds; existing Lint Kotlin/UAST/JNA infrastructure warnings
  remain, so this report does not claim a warning-free Lint run.
- APK copied to Download and its copied SHA-256 verified.

Restart OpenCode with the existing chat after updating so the new projection
helper is loaded; no automatic conversion of an already-running bridge.
Evidence: `.verification/opencode-chat-v1.5.14/` (java-verified.log, ui-final.log,
node.log, build.log, apk-audit.json, signature.txt).

Changes from v1.5.13-test:

- Removed the Stop row and redundant working/ready label, expanding the transcript.
- Covers the selected OpenCode PTY while determining its chat capability. Legacy
  sessions reveal their terminal only after a successful legacy status response.
- Acknowledged sends follow the latest message after layout; reading history
  retains its row anchor and touch input cancels pending automatic scrolling.
- Native mini dialog is 92% screen width / 62% height, cannot be dismissed by
  outside taps, enables normal terminal keyboard input immediately, and reuses
  the existing configured extra-keys view. Dismissal restores both view parents,
  layouts, extra-keys visibility and the previous terminal keyboard setting.
- The global interactive-input control focuses the separate direct Shell field
  in Android chat; it does not send commands to the hidden headless process.
- Native synthetic shell notices become inspectable pills, with duplicate
  notifications suppressed when their native shell record is on the same page.

Review boundaries: chat changes are scoped to OpenCode; other agents retain
native terminal interfaces. Owned process shutdown/API permission behavior is
unchanged. No package installation, APK installation or release publication.

Phone verification required: returning to an OpenCode session without a terminal
flash, sending while scrolled up, scrolling during a reply, configured extra-key
navigation/modifiers, normal keyboard input in the smaller native window,
Shell focus through interactive input, and restoration after Back/session switch.
Automated UI checks do not establish Android IME/gestures/DPI behavior.
