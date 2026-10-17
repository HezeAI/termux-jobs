# Validation — v1.4.18-test / code1124

Changed paths: LocalAiRecent folderless parsing/clarification, LocalAiAgent
prior-result runtime copy/screenshot routing, LocalAiContext excerpt helper and
LocalAiUi actual-model vision capability. Native inference/settings unchanged.

Meaningful cases added: RU/EN folderless/phone latest-file requests clarify without
model or filesystem probes; exact paths/questions/negations/compound actions retain
existing routing; phone screenshot question discovers actual paths without inference
for text-only sessions; generic questions/negations do not match; vision-capable
sessions retain model flow; older large tool output is excerpted with source/tail,
current task and current-turn results are preserved and saved history unchanged.
Existing chat-only, scope-before-probe and normal environment tests remain required.

Review: fast paths still call ordinary authorization/runner; no access-policy change.
Clarification performs no read. Screenshot location does not claim visual inspection.
Only the runtime copy of previous tool-result text is shortened, never the actual
saved result, user message or system permissions. Excerpts are marked untrusted and
point to current-chat history/fresh reads. A UI-only context notice is saved/excluded
from model input. Model-capability check uses selected model metadata, not a guess
based on filename. Source path is metadata, not an executable action.

Automated and artifact results are recorded in .verification/local-ai-v1.4.18.
Device routing, keyboard, actual input reduction and measured latency still need
an in-app check. This change does not prove general reasoning/tool reliability of
small models or accelerate a completely cold CPU prompt by itself.

Final:632 Java tests passed,0 failures/errors/skips. ARM64 assembly/runtime gate
passed. Archive CRC/paths/duplicates, com.termux/code1124/1.4.18-test/minSdk26/ARM64,
unchanged signing identity, source/stage match and limited secret scan passed.
Native payload and external agent assets match1.4.17. No native CPU optimization
is included. SHA-256: 3415bb8afbbb557141547a04a28ef9d79b9465729f3d17945b870ae455de7932
