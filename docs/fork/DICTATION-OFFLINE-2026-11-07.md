# Offline field dictation — investigation and private test12

## Confirmed system-provider failure

The developer's Codex-chat screenshots report RECORD_AUDIO granted,
isRecognitionAvailable=true, readable voice_recognition_service=<null>, and
SpeechRecognizer.onError=5 (ERROR_CLIENT). One installed service is visible:
com.anthropic.claude/.bell.assist.ClaudeRecognitionService. The same service is
returned when including disabled components. Availability alone means discovery,
not successful dictation.

Read-only inspection of that public installed APK shows that its
RecognitionService.onStartListening immediately invokes callback.error(5).
It records no audio and returns no transcription. onCancel/onStopListening do
nothing. Explicitly selecting it cannot supply field dictation for any agent.
A separate Android helper under real UID10620 connected explicitly: service proxy
present, one start, onError5, cleanup only after the callback. It did not record
speech, launch a foreign activity or call an agent. This excludes premature
client cancellation in that probe; it does not reconstruct every prior UI run.

Read-only inspection of this phone's Android36 SpeechRecognizerImpl also shows
that a missing default produces error5 before createSession. Exceptions while
reading Settings must be distinguished from a successfully read null. The
old screenshots' <null> is the latter. Targeted available/live/UID speech logcat
had no matching records, including during the developer's repeat. This is a
logging limitation, not proof that connection did not occur. Evidence remains
in `.verification/dictation-1507/` (provider class, framework classes, probe event
order and result, targeted log collection). No auth/private app files were read.

The public Yandex Browser APK contains internal SpeechKit/Chromium recognition
code but does not expose an Android RecognitionService. Its search microphone
cannot be selected by our system-recognizer client. No runtime request chain,
keys, cookies or proprietary recognition endpoint was copied.

## New app-owned backend

Private test12 uses Vosk Android0.3.75, JNA5.18.1, Russian small model0.22.
The same AgentDictation owner/permission wrapper serves Codex, Claude, Gemini and
OpenCode. This backend neither uses Google nor launches another application.
AudioRecord supplies 16kHz mono PCM16 to the local recognizer. Audio is held in
memory, not saved/uploaded by this backend. Existing agent networking is separate.

The first press requests RECORD_AUDIO if necessary. Model extraction/preparation
and recognition run on a worker. The composer shows preparing/listening/partial
text and provides Stop/Cancel. The first recognized utterance or Stop completes
recognition; final text appends once, with spacing, to the unchanged source draft.
No message is sent. Switching owner, editing the draft, cancelling or hiding the
chat invalidates late callbacks. Capture cleanup runs on its worker; serialized
capture prevents overlapping app-owned microphones. Errors identify preparation,
microphone or decoding stage. A duplicate permission result cannot start twice.

The model is a pinned compressed APK asset, extracted atomically into app-private
cache. Extraction verifies SHA-256, ZIP CRC, allowed paths, entry/expanded-size
limits and required model files. It honours cancellation and removes only its
own incomplete temporary directory. It does not delete an existing incomplete
cache or install/configure an Android recognition service. Android may evict the
cache; a later use recreates it from the bundled asset without a download.

## Inputs, licenses and limits

Maven AARs/source JARs verified against upstream SHA-1, with local SHA-256 recorded.
Model: official HTTPS download, ZIP CRC and local pinned SHA-256; no independent
upstream signature/hash was available in this check. Exact identities are in
`.verification/offline-dictation/{inputs,maven-provenance}.json`.
The build rejects an absent or different model asset. Prepare it with
`scripts/prepare-dictation-model.py [--source verified-local-zip]`.

Vosk and Russian model: Apache-2.0; JNA distributed under its offered Apache-2.0
option. Source artifacts/links and full license texts are bundled under
assets/offline-dictation and listed in THIRD_PARTY_NOTICES.md. No matching Vosk
0.3.75 Git tag was found. The native library is the published AAR binary; an exact
native-source reconstruction is not claimed. Earlier component-source coverage
limitations are not resolved by this addition.

Only Russian is bundled. Model ZIP:46,236,750 bytes; extracted files:91,289,240.
ARM64 Vosk library:10,042,800; JNA dispatch:176,520 bytes before APK compression.
Speech accuracy, accented speech, long dictation and microphone contention require
phone evaluation. This model does not imply universal language support.

## What the Android helper actually checked

The production DictationModel.extract processed the actual bundled archive,
verified it, then org.vosk.Model/Recognizer loaded on this phone's Android runtime.
A one-second all-zero PCM buffer produced an empty transcription. Exit0; initial
uncached run approximately3.2 seconds. Both new ARM64 ELF libraries loaded.
An earlier helper attempt lacked its native-library search path; linker namespace
rejected loading and process exited137. That attempt was not accepted as PASS.
Adding the library directory to the helper's Java library search path succeeded.
Installed APK loading uses Android's own packaged-native directory, not that helper
path. This is an ABI/model test, **not microphone dictation acceptance**.

Real speech into the draft still requires manual installation and phone testing.
No automatic APK installation, service changes, working-session restarts, publication
or reserve1499 actions are authorized or performed.
