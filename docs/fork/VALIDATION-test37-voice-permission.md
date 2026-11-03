# test37 — Send alignment and Codex voice permission continuation

Date:2026-11-03. Final APK: `/sdcard/Download/Termux-Jobs-test37.apk`.

| Property | Verified value |
| --- | --- |
| Package | `com.termux` |
| Version | `2.1.1-test37` / **1472** |
| Bytes | **40,438,893** |
| SHA-256 | `64ea2ab2f70bfd59a0c82162452c2c939f48a72542a9e552732adb49ad3f9da8` |
| ABI / debug | ARM64 only; ELF64 AArch64 / nondebuggable |
| Signature | v2 verified; previous signer retained |
| Certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |

Sidecar: `/sdcard/Download/Termux-Jobs-test37.apk.sha256`.
VersionCode exceeds all inspected issued/reserve APKs in Download; previous max1471.

## Included

- Send stays at the bottom right beside the message field. Agent-row baseline
  alignment is disabled; ordinary terminals restore their original setting.
  The independent microphone overlay keeps its48dp target and does not move Send.
- Codex permission requests survive the permission UI pause/stop. Granted
  recognition waits for foreground resume, validates current permission and the
  source chat, and starts once. Repeated taps while permission is pending do not
  request again. Cancel/background/chat changes reject late callbacks and keep
  the draft. Final text never sends automatically.
- Native SpeechRecognizer client error5 and local startup exceptions are separate
  RU/EN error paths, with safe diagnostic stage/code/class records. No draft,
  recognition candidate, audio, chat identifier or exception message enters logs.
- Retain test36 attachments, other agent/terminal controls, stop confirmation,
  instruction/skill managers and existing runtime assets. Slash changes remain
  deferred. No robot mockup, environment conversion or automatic CLI update.
  Helper cache is instructions-1472, separate from saved user instructions.

## Checked anew

- **65 JVM/Robolectric tests** passed on version1472: speech permission/denial,
  grant while paused and continuation once, repeated prompt taps, subsequent
  revoked grant, local exception versus native5, draft/late-callback isolation;
  Send geometry at280/360 widths and14/24/36sp with multiple lines and mic on/off;
  all-four-agent header routing, keyboard/navigation/transcript regressions,
  and version-dependent instruction-cache behavior. Fixtures do not record audio
  or launch working agents.
- Release compilation and packaging, including existing verifyGuavaRuntime and
  verifyX11Inputs guards. Known legacy AGP/Kotlin-metadata and filesystem-watch
  diagnostics remain in the successful build log; no new suppression was added.
- Actual delivered APK: ZIP, package/version/debug/ABI, v2 signature and previous
  certificate, SHA-256, source/build/Download byte equality, all117assets matched
  source/pinned cache, all3native payloads ELF64 AArch64, runtime/DEX inventory
  (14178 defined classes; requiredMissing and unreviewedMissingTypes
  empty). Speech/control/session-resource classes, real vector resource paths,
  RECORD_AUDIO, optional microphone hardware and service visibility are present.
- Source/security review: source-chat ownership and generation guards remain;
  permission UI does not record in background; no shell/model request, CLI poll,
  process signal or persistence-format change. `git diff --check` passed.

## Reused with identity proof

- Immediately preceding full modern Lint7.4.2: **0errors,234warnings**, all five
  modules, no infrastructure diagnostics. All580production main files match the
  analyzed stage. Gradle/configuration matches after explicit staging transforms
  and version-only normalization. Generated version constants changed; their
  compilation and affected tests were checked anew. Lint was not run again merely
  for that version bump. Evidence: lint-production-inputs.json and lint-reuse.json.
- Existing local SDK/JDK/Gradle/dependency caches; no full environment copy.
  Native source/X11 input inventory (95files) matches prior evidence. Three final
  payloads match the verified signed test36 APK; two Termux JNI files were restored
  from it into the isolated stage. This is packaging reuse, not a new native
  compilation or graphics check.
- Against the930-entry test36 inventory, only the four intended production Java
  files, two targeted tests and app version configuration changed. Agent assets,
  environment recipes, manager storage and stop mechanisms remain unchanged.
  Their old functional reports keep their original scope; no fresh helper or
  environment PASS is claimed here.

## Not checked / phone acceptance

Actual Android Ask-every-time permission UI/provider behavior, speech/audio,
IME/touch and visual acceptance require the device. Check grant → listening →
final text → repeat, then denial/cancel/source-chat switch and background-return.
Confirm Send stays below the microphone with the keyboard open and large text.
The original error5 cause remains unknown: its speech log was not available and
old code could synthesize5 for a local startup exception. The screenshot does not
prove permission denial. See the [source diagnosis](VALIDATION-voice-permission-layout.md).

No unrelated guest installation, model request, session restart or X11/driver
functional run was needed. Package/signer compatibility is verified; this does not
claim exhaustive device migration acceptance or fix the previous Docker Hub cause.

Evidence: `.verification/agent-composer-test37/` contains scope, prior versions,
source/config/native equality, build/test logs, Lint reuse, manifest/resource/ELF
and DEX audits, signer, artifact and checksum records. Earlier APKs remain intact.

## Subsequent developer device report

After delivery of test37, the developer confirmed Send alignment is fixed, but
voice recognition still returns5. This does not identify the speech service or
the failing start/bind/cancel stage. The successful model tests above therefore
do not establish microphone/provider success. Later source adds a bounded launch
journal and an explicit Android recognition activity alternative; it is not part
of this unchanged test37 APK. See [speech diagnostics](DIAGNOSTICS.md#voice-launch-journal-and-android-window-source-after-test37).
