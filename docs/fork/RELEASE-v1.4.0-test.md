# v1.4.0-test — Local AI preview (not published)

Adds Local AI to new sessions and launch presets: five model choices, six pinned
variants, catalogue downloads with progress/SHA-256/resume, custom GGUF/LiteRT-LM
imports, native text chat, saved history and independent input drafts.
Native inference runs in its own non-exported application process. No shell or
project-file tools are exposed to the models. Existing cloud agents remain separate.
Includes the unreleased v1.3.4 stable-search/browser-resource changes.

**Limits:** llama.cpp CPU only; LiteRT-LM GPU/CPU requires device verification.
Qualcomm NPU is not operational: its dispatch/QAIRT runtime is not bundled.
MTP, image/audio input and agent tools are not implemented. No speed claim.
One loaded model; 4096-token runtime context, up to 768 response tokens; full local
history stays on disk even when bounded context is replayed.

No reset is required for existing commands, notes, terminal environments or
cloud-agent histories. Uninstall/clear data removes private local models/chats.
See [Local AI guide and rollback details](LOCAL-AI.md).

Test APK code1101. Recovery is pre-feature v1.3.4 with manifest code1102 and the
same signer. Old UI does not understand local-AI presets and may discard optional
local preset fields on save; local chat/model files remain. A following update over
recovery needs code1103 or higher. Do not clear app data to roll back.

The developer still needs to check real catalogue models, GPU drivers, imports,
streaming, keyboard, DPI, cancellation, app backgrounding and device updates.
