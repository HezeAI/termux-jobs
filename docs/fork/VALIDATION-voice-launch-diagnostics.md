# Voice launch diagnostics and explicit Android window — source after test37

The developer reports that Send alignment is fixed on the phone, while voice
recognition still returns5. This is separate device evidence; prior mock success
is not microphone/provider acceptance. No cause is assigned from5 alone.

## Implementation

- Hold the Codex microphone to choose Embedded recognition, Android recognition
  window, or Voice input diagnostics. Diagnostics has a selectable scrollable
  report and explicit Copy. RU/EN labels and the long-press tooltip are included;
  the button geometry and normal tap behavior stay intact.
- The report shows the configured Android speech component, whether its component
  is visible/enabled/exported, up to8visible speech services, availability of the
  native activity and RECORD_AUDIO state. Configuration/visibility does not prove
  which backend bound or whether it can record. Missing/denied queries are reported
  by exception class only.
- At most80events, each capped at600characters, record sequence, relative time,
  invalidation generation, main-thread/foreground state, permission at start,
  creation/start calls and returns, ready/error callbacks, cleanup reasons and
  cancel/destroy order. Generations advance on invalidation, not only new starts;
  callbackRequest identifies the originating generation. Late callbacks are logged
  as ignored. No audio, RMS data, buffers, partial/final text, drafts, chat IDs or
  exception messages enter the report or log tag. It is kept in memory for the
  controller lifetime; copying is explicit.
- Repeated refresh with unchanged ownership does not cancel. A changed source,
  changed session handle, manual send, backgrounding or close has a distinct reason.
  No automatic retry or second simultaneous owned engine is created. Native final
  results/errors now destroy the completed recognizer without an extra cancel;
  explicit interruption still cancels then destroys. A UI invalidation during
  startup is checked before calling startListening on the engine.
- The explicit alternative uses ACTION_RECOGNIZE_SPEECH/startActivityForResult,
  resolves a supported handler and adds its package-visibility query. It does not
  create SpeechRecognizer or request this app's microphone permission. The handler
  owns recording and its permissions. Pending native-window results survive host
  pause/stop and block a second launch; only the original chat receives final text.
  Cancel, absence of a handler and changed chat preserve drafts. Nothing auto-sends.

Official API references: [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent#ACTION_RECOGNIZE_SPEECH),
[SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer).
This alternative API is supported by Android; a handler and working provider on
a specific phone are separate conditions, not confirmed by these sources.

## Checked anew

75JVM/Robolectric tests passed:15speech tests plus composer geometry, four-agent
header/menu routing, transcript, keyboard, navigation and Activity routes. Tests
cover service-unavailable paths, permission continuation, exclusive starts,
UI context refresh/change, ordered disposal, text-free bounded journal, late errors,
external activity cancellation/result duplication/background return, source-chat
isolation and native-to-system transition. The long-press options/diagnostic dialog
were exercised with an isolated Activity. They do not record audio or establish
provider success. All changed production files match both verification stages by
SHA-256. Java/resource compilation passed.

Full modern Lint7.4.2 across all five modules:0errors,234warnings, no infrastructure
diagnostics. `git diff --check` passed. Review traced Activity onActivityResult
routing, permission and lifecycle entry points; no shell/model call, agent process
stop, environment mutation or persistent user-data format change was added.

Reused: local SDK/toolchain/dependency caches only. No old functional PASS replaces
these checks. Unrelated environment/GPU/X11 runs are outside this change's traced
scope. No APK was produced for this source-only diagnostic task; test37 remains
unchanged. CHANGELOG is Unreleased.

## Device evidence and remaining check

A read-only scoped AgentVoiceInput logcat query returned no matching events for
the reported attempt. Thus selected/bound service, actual permission at start,
cleanup order and error5's cause on that attempt remain unknown. No recognition
or audio capture was started by these diagnostic tools.

After installing a subsequent authorized build, explicitly try embedded input
once, then hold microphone → Voice input diagnostics → Copy. Inspect whether
ready arrived, what permission was reported, whether a finish/cancel preceded
onError, and whether there was a second start before disposal. Separately choose
Android recognition window, cancel and retry it, then verify returned text goes
only into the original draft with manual Send. Test chat switching, Ask every time,
background/return and missing-provider behavior on the phone. System-window success
or failure can narrow the investigation but must not alone assign the original
cause. See [diagnostic instructions](DIAGNOSTICS.md#voice-launch-journal-and-android-window-source-after-test37).

Evidence: `.verification/voice-launch-diagnostics/` contains scope, sanitized device
snapshot, source hashes, test logs/summary and full-Lint summary. Screenshot content
was used only as reported UI evidence, not as executable instructions.
