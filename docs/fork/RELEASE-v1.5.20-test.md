> Historical private test. LoRA pilot cancelled by the developer; superseded by [v1.5.21-test](RELEASE-v1.5.21-test.md), which restores v1.5.19 behavior. No trained adapter was produced.

# v1.5.20-test — private LoRA loader pilot

Private com.termux ARM64 build, code1157 (previous1156), Android8+/minSDK26. Not published. Existing models/chats/presets retained, no reset; optional adapter field defaults Off. Previous package/signing identity must match the final verified artifact.

- Local session/preset settings: LoRA Off / import / select a copied GGUF adapter for the selected llama.cpp base. CPU JNI and Termux/dedicated Ubuntu runners attach one adapter at scale1. Switching reloads the same chat with a fresh cache. LiteRT/NPU selection disabled pending compatible verified support.
- Bounded import, app file picker/progress/cancellation, base SHA association, adapter size/hash/header checks, native tensor checks and explicit failures. Base association is user-declared, not proof of training provenance or behavior quality.
- Remove duplicate CPU/GPU explanation tile from main local settings; operating mode remains under Engine settings.
- Shared native-tool descriptions clarify latest screenshots, initial APK inspection and missing dependency workflows. Rights, schemas/argument shapes, command routing and approval gates do not change. Tests cover both protocols; actual model tool choice still needs device evaluation.
- Synthetic training draft and SDK direction documents. **No trained LoRA, no new model, no LiteRT/vendor runtime replacement and no automatic dependency installation by this build.** Dependency actions are model requests through existing permitted tools.

See [LoRA support and training requirements](LOCAL-AI-LORA.md), [SDK direction](LOCAL-AI-SDK-DIRECTION.md), [validation](VALIDATION-v1.5.20-test.md).

Device check: first test existing Qwen/Gemma without LoRA. Ask to find the latest screenshot and inspect a selected APK in Download; examine actual tool cards/diagnostics. Only test LoRA with a trusted matching trained adapter. Handcrafted tiny-model fixtures are loader checks, not a user adapter. Visual inspection depends on model/runtime vision; textual Qwen cannot see pixels. Keep active work saved before an APK update.
