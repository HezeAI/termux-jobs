# Post-test36 source fix: speech permission and composer geometry

Scope: Send alignment, Codex Android SpeechRecognizer permission continuation,
Activity pause/resume, original-chat ownership and safe diagnostics. No slash,
agent protocol, stop-process, environment, X11 or ordinary terminal behavior changes.
No APK produced by this source-fix task.

## Findings

- Developer screenshots show Ask every time and recognition error5, with Send
  raised above the bottom composer row. Those screenshots do not establish a
  permission denial or the provider-side cause.
- Android documents ERROR_CLIENT=5 and ERROR_INSUFFICIENT_PERMISSIONS=9:
  [SpeechRecognizer API](https://developer.android.com/reference/android/speech/SpeechRecognizer#ERROR_CLIENT).
- The previous permission-result handler converted every local RuntimeException
  to ERROR_CLIENT. The screenshot alone cannot distinguish that application path
  from Android RecognitionListener.onError(5).
- Read-only scoped logcat returned no speech entries; querying the selected speech
  service was denied by Android, including explicit user0. See task-local
  `.verification/voice-permission-layout/device-readonly.json`. No provider setting,
  permission or running session was modified. The original error cause is unknown.

## Changed behavior

Agent composer disables baseline alignment while in agent mode and restores the
original flag for ordinary terminals. Send stays at the bottom beside the field;
the microphone remains in its independent overlay.

A pending permission request survives permission UI pause/stop. Granted recognition
waits until Activity resume, then validates original chat and current permission
before starting once. Repeat taps during the permission prompt do not duplicate
requests. Switching chats, cancellation and late recognition callbacks preserve
drafts and never insert into another chat. Active recording cancels on background.
Local startup exceptions no longer masquerade as native error5. RU/EN status
explains client error5 without asserting denial. Safe log records distinguish
recognizer callback/code from local startup exception/class; no speech, draft,
chat IDs, audio or exception messages are recorded.

## Verification status

Checked anew:60 distinct JVM/Robolectric tests, including10 speech tests,
2 composer geometry tests, controller routing, keyboard, navigation and transcript
regressions. Both test runs passed. The added geometry test reproduces the previous
raised Send with baseline alignment enabled, then checks fixed bottom alignment
at280/360 widths,14/24/36sp text, single/multiple lines and microphone on/off.
Permission tests cover grant while paused, resume exactly once, repeated prompt
taps, a revoked one-time grant, denial, source-chat changes, background cancellation,
late callbacks and local-startup/native-error separation in RU/EN.

Checked anew: full modern Lint7.4.2 across all five modules —0errors,
234warnings; no infrastructure diagnostics. Source and both verification stages
match by SHA-256 for the changed production Java. `git diff --check` passed.
See `.verification/voice-permission-layout/` for summaries, input hashes and logs.

Review/logical trace: pending permission cannot start recording in background;
resume revalidates owner and permission; chat switches invalidate pending ownership;
ordinary terminal restores its original baseline setting. No model request,
shell command, CLI polling or process signalling was added.

Reused: existing local SDK/toolchain and build caches only. No previous functional
PASS is substituted for these checks. Unrelated environment/driver/X11 functional
checks are outside the traced change; no guest installation was run.

Not checked: actual system provider, Ask-every-time permission UI, audio capture,
real IME/touch/visual acceptance. Reproduce grant → listening → final text → repeat,
then deny/cancel/switch-chat and background-return on the phone. Current source
checks cannot prove the original provider error is fixed.
