# v2.0.6 development validation — OpenCode attachment fix

Unreleased developer build, 2026-10-23. Public stable remains v2.0.1. The developer
plans a public v2.0.2 titled Agent chat improvements after phone verification; that
release will require its own higher versionCode and final artifact metadata.

## Artifact

- APK: `build/apk/Termux-Jobs-v2.0.6-arm64.apk`
- VersionName2.0.6, versionCode1330; com.termux, Android8+/ARM64, schema8.
- Size: 45828682 bytes
- SHA-256: `7b7055f70ad204883d8ba433ff586e1f0ba4f4cef949956f7c8e09cf38638bd7`
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
- ZIP integrity, package/version, non-debuggable flag, ARM64-only ABI, signature
  equality with v2.0.5 and increased versionCode passed. Native .so files are
  byte-identical to v2.0.5. Changed packaged helpers match source.
- Bounded assets/DEX key-pattern and private-file/development-path scans passed;
  this is not exhaustive secret detection.
- Release Gradle build and runtime-definition inventory passed:13600 definitions,
  required definitions present,31 reviewed optional references. Existing custom lint
  registry/Kotlin metadata warnings limit lint coverage.
- Final-APK update.json and SHA256SUMS are in the evidence directory. Download copy
  matches the final hash. No GitHub publication is part of this build.

## Tests, review and logical inspection

Reproduced the v2.0.5 bug using helpers extracted from the actual previous APK.
The production Unix controller rejected a file-only send before making an HTTP
prompt request because its attachment root was derived from nested receipts.

The corrected controller passed the same nested connection/socket/staging layout.
The fixture exercises file-only and mixed image/log/video-frame/binary sends,
native file URIs, >2MiB prompt/inbox replies, small projected cards, duplicate
suppression, foreign-session/missing-file/Shell refusal, ordinary-send recovery,
unchanged non-media response limit, and owned controller/server shutdown.
This uses a synthetic authenticated loopback HTTP peer, not model inference.

The OpenCode2.0.28 real native CLI fixture now constructs the adapter through the
production controller factory with nested receipt directories. It decoded the
local PNG URI and admitted it into the durable inbox with resume=false, without
calling any model/account service. Existing shared attachment, OpenCode chat and
settings suites also passed. Both changed production JavaScript assets passed
syntax checks. Java UI code is unchanged from v2.0.5, whose validation records122
scoped passing JVM tests; those tests were not needlessly rerun for this JS fix.

Reviewed socket/connection/receipt namespaces, scope claims and file validation.
The explicit root comes from the app-owned control socket, not a client-supplied
file path. Receipt locations and history IDs are unchanged. Traced send validation,
admission/failed ACK retention, pending inbox projection, repeated messages and
restart loading. Native media HTTP replies are capped at40MiB only on scoped
prompt/inbox/history routes; other HTTP replies retain2MiB, Unix/projection bounds
remain unchanged and media base64 is not forwarded to Android's transcript.
No installed user environment, auth file or running user session was modified.

Evidence: .verification/opencode-attachments-fix/{regression-old.log,socket.log,
native.log,adapters.log,build.log,apk-runtime.json,artifact.json,verify-final.log}.

## Remaining phone check

Update, restart OpenCode with its saved chat, select an image and press Send with
no text; repeat with text and a log/video. Confirm draft/card behavior and the
selected model's actual interpretation. Source/socket/native-admission evidence
does not establish the Android button/gesture or real provider behavior. No reset
or environment migration is required; inherited Gemini key portability restrictions
are unchanged and disclosed in localized update notes.
