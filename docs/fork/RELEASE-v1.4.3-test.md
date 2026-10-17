# v1.4.3-test — local test build

Shared action coordination for all local backends and custom models: clear requests
should call an allowed tool immediately rather than offer to act. Instructions
preserve explicit paths; no code rewrites a user path to the selected project.
One corrective generation handles thinking-only answers or unexecuted offers.
This improves the protocol but does not guarantee every model will choose a correct
action. Prose and reasoning are never executed as code.

A native template regression was reproduced with the official Qwen3.5 template:
legacy llama_chat_apply_template renders ChatML but omits the enable_thinking=false
suffix. The bridge now restores that suffix for matching Qwen-compatible ChatML
only, not Gemma. CPU EOS/token-budget finish reason is sent to the client; LiteRT
callback completion is labelled sdk_done without inventing a stop cause.

Reasoning is a pill opening an independent scrollable dialog, not an expansion
inside the chat. Raw text remains in local history. Tool results are labelled.

Optional per-session Confirm actions: a write/command request pauses at a real
Yes/No card. Decline/cancellation do not execute it; stored cards are read-only
and pending cards become interrupted after reopening. Read/chat permissions cannot
be expanded by approval. Default stays automatic in full access; Path checks and
diagnostic logging remain independent. Only local AI is affected.

Package/certificate retained; code1107. No data reset; optional consent flag defaults
false, preserving prior automatic full access. Refreshed baseline recovery code1108,
future APK after recovery needs1109+. Old recovery code1106 cannot replace1107.
See LOCAL-AI.md and VALIDATION-v1.4.3-test.md. No GitHub publication.
