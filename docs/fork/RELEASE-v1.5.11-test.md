# v1.5.11-test — LiteRT tool-selection diagnosis

Private diagnostic build; not a GitHub release. Local AI remains enabled.

The local AI diagnostics now offer Engine tests → LiteRT tool selection. Five fresh synthetic conversations compare system-role adherence, a write-only schema, all permitted tools with current sampling, greedy sampling, and thinking with a 128-token budget. Each reply is capped at 512 tokens, including thinking. The guide request explicitly asks not to install Python; this isolates the basic tool contract and is not an exact replay of the original ambiguous request.

Generated tool calls are recorded, never dispatched. No synthetic guide is written and no chat history is added. Previous schema, scope and diagnostic overrides are restored after completion, error or cancellation. Stopping can unload the model; use Start before continuing if needed. The saved report is separate from ordinary chat diagnostics and can be exported through its report window.

Ordinary generation settings, model weights, permissions and llama.cpp behavior remain unchanged. No data migration/reset is required. Package/signing identity is retained; versionCode increases to 1148. Hardware inference and the suspected cause remain unverified until the developer runs the test on the device.
