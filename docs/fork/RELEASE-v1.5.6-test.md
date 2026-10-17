# v1.5.6-test — private local-AI loop fix

ARM64 / Android8+, com.termux, versionCode1143; existing certificate retained. No automatic data reset or library schema change. Not published to GitHub. Model/engine files and external CLI/server-agent assets unchanged.

Local-AI coordinator keeps compact current-turn completed-action metadata independently of raw history compaction. It is sent to both llama.cpp and native LiteRT; the native prompt replacement explicitly preserves it separately from correction feedback. It is historical metadata, never permission. A bounded recent-step excerpt conserves context; original records remain in chat history.

Cycle detection compares exact normalized calls AND result fingerprints across repeated sequences of2..8 steps containing a mutating tool. After two identical cycles, the next restart is withheld and the model receives a reminder. If it immediately proposes the same cycle again with no new executed action, the request stops with a clear message. Different paths/arguments, changed results, ordinary read-only sequences and numeric requested repetitions are separately tested. Explicit repeat counts in supported Russian/English forms permit that count; this heuristic is not a universal understanding of every monitoring/retry request and is not a general tool-call limit. Existing cached-read protection remains, adjusted for explicit repetition counts.

Both local-AI chats show one compact expandable step card per user request. It updates as tools finish, persists with history, and opens a scrollable list of recent executed actions/results. It retains up to64 metadata entries with an omitted-count notice; detailed tool records stay in history. The card does not enter model context. Individual tool cards remain.

Context shortening notices now use structural comparison rather than JSON key ordering, avoiding false notices for unchanged data.

Developer check: in a new Gemma native chat repeat the same create/read/delete/verify request with Brief behavior, then inspect the step card and diagnostics. A normal final answer and a cycle-guard stop are different outcomes; device results must establish whether progress metadata resolves the model's restart behavior. Do not describe a stopped loop as a fully completed task. llama.cpp normal file workflows and explicit multiple checks should also be exercised.
