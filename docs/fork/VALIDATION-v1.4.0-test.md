# v1.4.0-test — local validation, not a published release

Final package com.termux, versionCode 1101, minSdk 26, targetSdk 28,
arm64-v8a only. APK: 51,042,892 bytes. SHA-256:
`86c450c36cf324c10d6da729080dadd7fceaa70a621e99f925ee0269eed0647f`.
Signing identity matches previous builds (certificate SHA-256
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`).

## Evidence

- Full JVM suite: 456 tests, zero failures/errors/skips. Includes model import,
  cancellation, private history, optional v8 persistence and separate virtual-tab drafts.
- Release build and required runtime-definition checks passed. Legacy Lint emits
  modern Kotlin metadata compatibility diagnostics; this is not a clean-Lint claim.
- Actual llama.cpp CPU smoke: tiny GGUF loaded, prompt decoded, 16 tokens generated,
  resources released. This does not validate Qwen/Gemma models or Android UI.
- Actual LiteRT-LM Java API/configuration compatibility checked, without inference.
- APK ZIP CRC, package/version/ABI/signature, limited secret-pattern scan and
  exact Java/resource/asset staging comparison passed.
- Existing Termux native libraries and server-agent asset remain byte-identical.
- Reviewed cancellation ownership and serialized model disposal; local inference
  service is nonexported and separate from PTY processes, but shares the app UID.
- Rollback payload verified byte-identical to pre-feature v1.3.4 except manifest
  versionCode and signing metadata. Its versionCode is 1102.

## Required device checks and limits

Keyboard/layout/SAF permissions, actual catalogue model inference, GPU drivers,
Android process death and low-memory behavior remain device checks. Start with
Qwen3.5 2B on CPU; test stop, restart and switching to existing terminal tabs.
LiteRT-LM GPU falls back to CPU in Auto mode; GPU availability is not guaranteed.
NPU support is blocked because the Qualcomm dispatch/QAIRT runtime is not bundled.
llama.cpp GPU, MTP, tools, image/audio inputs are not implemented.
JNI smoke through the host Java process was blocked by proot/linker namespace;
only the separate native CPU smoke and Java API check are claimed.
Models and local-chat files are not included in the current backup-manager scope.

## Update / rollback compatibility

No automatic data reset. Existing library schema remains v8 with optional local-AI
fields. Old profiles are preserved. Rollback cannot open local chats; editing the
library in the old version may discard optional local-AI profile fields. Model/chat
files remain on disk. After rollback, a subsequent APK must have versionCode at
least 1103. Do not uninstall or clear data to switch builds.

Evidence: `.verification/local-ai-v1.4.0/` (ignored local logs and artifact.json).
