> Historical experimental feature: removed from the current v1.5.22-test source/APK. This document is retained as research evidence, not current functionality. See [removal notes](RELEASE-v1.5.22-test.md).

> Historical proposal/test notes, superseded by [v1.5.21-test](RELEASE-v1.5.21-test.md). LoRA was cancelled. Referenced training scripts belong to archived commit9ffa1501 and are not present in the current app source.

# Local AI SDK direction (proposal, not an implemented new SDK)

Current layers already form a useful boundary: LocalAiUi owns selection/chat; LocalAiRemote talks to the owned LocalAiService; LocalAiEngine chooses llama JNI/external runner or Android LiteRT; the tool broker enforces actual rights and runs commands separately. Reuse this boundary rather than rewriting working engines.

A future small internal API can standardize capabilities, load/generate/cancel/release, model/adapter identities, structured events, timing and cancellation ownership. Capability negotiation must distinguish text, images/audio, structured tools, LoRA, context limits and real metrics. Cache identity must include loaded base/adapter, chat, system/rights/protocol and settings; switching adapters currently reloads the engine. Never advertise unsupported LiteRT LoRA or copy a Qwen adapter to Gemma.

The tool side should preserve exact selected environment and permissions. For local development, shell/APK/build/dependency operations use dedicated jobs-local-ai Ubuntu; optional Termux commands require explicit enablement. Shared storage is mounted only where Android access permits. Neither engine selection nor a trained adapter grants Android root, vision, filesystem permissions or working-Ubuntu access.

An internal API can share fixes and make tests clearer. It does not itself accelerate NPU prefill, remove tool-schema context, train a LoRA or make every model obey. Distributing engine updates later also requires versioned ABI/protocol, hashes/signatures, known model compatibility, cancellation/recovery checks and rollback. No runtime downloader or unsigned hot-patch mechanism is introduced here. Codex/Gemini/Claude/OpenCode and normal PTY sessions stay outside this local-engine API.
