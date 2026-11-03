# test38 — Codex speech diagnostics and Android recognition window

Date:2026-11-03. Delivered file: `/sdcard/Download/Termux-Jobs-test38.apk`.

| Property | Verified value |
| --- | --- |
| Package | `com.termux` |
| Version | `2.1.1-test38` / **1473** |
| Bytes | **40,441,493** |
| SHA-256 | `c3801792f5c73b91defc9e14fdb60f048757ed07b43241790efa833fc1f96e41` |
| Certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| ABI / debug / signing | ARM64 only, ELF64 AArch64 / nondebuggable / v2 verified |

Checksum sidecar: `/sdcard/Download/Termux-Jobs-test38.apk.sha256`.
VersionCode exceeds all inspected issued/reserve APKs in Download, previous max1472.

## Included

Codex microphone normal tap retains embedded recognition; hold opens Embedded
recognition / Android recognition window / Voice input diagnostics. The bounded,
copyable journal reports configured/visible services, permission at startup and
ordered create/start/callback/cancel/destroy with reasons, without audio, dictation,
drafts, chat IDs or exception messages. Configuration does not prove binding.
Native results/errors now release without an extra cancel; explicit interruption
still cancels then destroys. No automatic retry or simultaneous owned recognizers.

Explicit Android recognition activity returns final text only to the original
draft, never sends it. Cancellation, missing handlers and chat changes preserve
text. Backgrounding for the activity preserves the pending result and blocks a
second launch. The handler owns its recording/permissions. All prior test37 Send
alignment, attachments, indicators, stop confirmation, instruction/skills and
runtime assets remain. Slash work stays deferred; no robot mockup or migration.
Helper cache is instructions-1473, separate from saved user instruction files.

## Checked anew

- 80JVM/Robolectric cases passed after version bump:15speech cases, controller
  and microphone menu, Activity routing, transcript, keyboard/navigation, Send
  geometry and instruction-cache behavior. Fixtures establish application logic,
  not microphone/provider success.
- Release compilation/packaging including existing runtime/X11 input guards.
  Known legacy AGP/Kotlin-metadata and filesystem-watch diagnostics remain in the
  successful build log; no new suppression was added.
- Exact final APK: ZIP, package/version growth, previous signer/v2, ARM64/debug,
  source/build/Download byte equality,117assets source/pinned-cache equality,
  three native payloads ELF64 AArch64, DEX/runtime required definitions and reviewed
  optional references. Speech/control/resource classes and diagnostic strings,
  actual vector paths, RECORD_AUDIO, optional microphone hardware, speech-service
  and Android recognition-activity visibility queries are packaged.
- Source/security/lifecycle review and `git diff --check`: no model/shell call,
  agent process signalling, auth-file read or persistent data-format change.

## Reused with identity proof

Immediately preceding full modern Lint7.4.2:0errors,234warnings, all five modules,
no infrastructure diagnostics. All580production main files match its analyzed
stage. Build configuration matches after documented stage transforms and version
normalization only; changed generated constants were compiled/tested anew. Lint
was not rerun solely for the version bump. Local SDK/JDK/Gradle/dependency caches
were reused. Native source/X11 inventory95files matches prior evidence; all three
payloads match signed test37, two Termux JNI files restored into isolated stage.
This is packaging reuse, not a fresh native/graphics run. No guest install or
working-session restart was required. Evidence: `.verification/agent-voice-test38/`.

## Phone check / publication hold

The developer confirmed test37 Send alignment but still reported code5. Its cause
remains unknown; an accessible historical speech log was unavailable. This build
adds evidence collection and an alternative, not a proven provider fix.

On the phone: try normal mic tap; then hold → Android recognition window. Verify
returned text preserves the draft and requires manual Send. If recognition fails,
hold → Voice input diagnostics → Copy, after the failed attempt. Test Ask every
time, repeat, cancellation and source-chat switching. Actual recording/provider,
IME/touch and appearance are not confirmed by model tests. Stable publication with
voice input is held until the developer performs this check; any later publication
still requires authorization for that release.

[Source checks and limits](VALIDATION-voice-launch-diagnostics.md).
