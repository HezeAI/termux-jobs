# v1.5.2-test — compact local-assistant instructions

Private test build, versionCode1139; public v1.5.0 unchanged.

Removed repetition from the bundled default behavior instructions shared by llama.cpp and LiteRT-LM. Permission/tool schemas and code authorization are unchanged; user custom instructions are not overwritten. Default first-turn LiteRT estimates previously exceeded the4096-token context budget before inference even for “привет”. New regression coverage includes bundled behavior, permissions, runtime text-only notice, request and output reserve together across chat/read/full-access, project scope and confirmation settings.

Same package/certificate, no data reset. Existing chats/presets retained. Imported custom behavior prompts remain unchanged and can still exceed small contexts. This fixes the reproduced application-side budget failure; actual NPU initialization/generation requires device verification. No engine/runtime/model binaries changed.
