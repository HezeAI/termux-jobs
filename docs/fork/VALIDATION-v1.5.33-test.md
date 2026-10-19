# v1.5.33-test — native terminal return and keyboard

Private APK, not a GitHub release. com.termux, versionCode1191, Android8+/ARM64.
Compatible package/certificate expected with private v1.5.32/code1190. No schema
migration or user data clearing is introduced.

The developer successfully resumed the existing conversation in v1.5.32's native
Android Codex chat. Two subsequent device screenshots showed the full native CLI
remaining attached after hiding its dialog, blocking Android chat sends; tapping
the CLI composer also did not open the Android keyboard.

## Final behavior

All agent terminal dialogs have Minimize and Return to [agent] chat, with Enter
and existing large arrow/extra keys retained. Return uses each agent's control
channel; arbitrary /exit text is not injected into a shell or permission prompt.
Minimize retains the native interface for Codex/Claude. The chat explains that an
action remains in the terminal and offers both Open terminal and Return to chat.

Codex: explicit terminal-exit closes only the controller-owned native TUI tree,
using captured PID/start-time checks; app-server, history, RPC connection and
other sessions remain running. A native turn in progress is allowed to complete
before this UI closes. Minimize/normal dialog dismissal sends terminal-close,
which does not close the CLI. A repeated return is idempotent.

Claude: explicit terminal-exit returns an idle full native CLI to the existing
SDK chat through its existing resumeChat lifecycle. It refuses during a native
request; user is told to wait and tap again. Minimize remains visibility-only.
OpenCode/Gemini reuse their existing terminal-close visibility/control operation.

Keyboard: dialog IME-blocking flags are cleared. A mouse-tracking TUI consumes
normal taps before the usual keyboard path; a default no-op client callback now
allows the Android app to open IME specifically for touch in the reparented
terminal dialog. Main-window terminals retain existing behavior. Hardware mouse,
selection/scroll gestures and native mouse click delivery remain unchanged.

## Evidence and limits

See .verification/codex-chat-v1.5.33 for final JVM, Node fixture, build, runtime and
APK audits. JVM checks verify IME eligibility/request and parent/key restoration,
not Samsung keyboard behavior. Device checks still needed: tap native composer,
Minimize then explicit Return, send a follow-up, and preserve an active request
while switching windows. Claude account-backed operation remains unverified;
existing developer account/billing constraints are unchanged.

Running user sessions, installed environments, credentials and packages were not
modified during development. APK delivery does not install it or publish it.

## Final APK audit

- File: `Termux-Jobs-v1.5.33-test-arm64.apk`, 36663950 bytes.
- SHA-256: `d5c85e59dc2a85c99b8182bf577e697c955750ea599a6fe60a0a2d709dd45a24`.
- com.termux / v1.5.33-test / code1191 / minSdk26 / arm64-v8a.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (unchanged).
- ZIP integrity, unique entries, intended ABI, runtime definitions, final source
  staging match, bundled private-data/secret-marker checks passed. Only the Codex
  and Claude chat assets changed relative to 1.5.32; other assets and native
  libraries are identical. This is a targeted audit, not a proof of every runtime
  scenario. Existing build/Lint warnings remain; no clean-Lint claim.
- Node Codex adapter, Claude adapter/lifecycle, and existing Codex limits fixtures
  pass. The isolated process cleanup fixture leaves an unrelated process alive.

The first UI test run after changing the return instructions failed because a
Claude test still expected the old /exit wording. Its expected visible guidance
was updated to the user-approved controls; final JVM result is recorded separately.

Final JVM run: **151 tests, zero failures/errors/skips**. APK and checksum copied
to Download and destination hash verified.
