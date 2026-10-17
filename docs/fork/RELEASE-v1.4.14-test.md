# v1.4.14-test / code1120

All local catalogue/imported models share runtime context budgeting, an in-chat
shortening notice, current-chat history search, read reuse and direct text display.
External Codex/OpenCode/Claude/Gemini sessions are unchanged.

Context is a working copy, not a replacement of saved history. Budgeting reserves
space for the answer and keeps current system permissions and the current user
request. Older reasoning and complete turns are removed first; large tool results
may become explicitly marked excerpts. The latest task is never silently cut. Older images/frames may be omitted with an explicit note in the working input; at least the latest image is retained.
An oversized current request/instructions may still require a shorter request.
Currently all local runtimes load with 4096 tokens; this code uses loaded capacity,
not an architecture's advertised maximum, and message count varies with content.
llama.cpp uses the same actual model tokenizer/chat template for counting and
generation. LiteRT uses a conservative estimate (including image allowance), with
one bounded retry on a recognized context overflow; it is not an exact tokenizer.
This does not guarantee every vision/input configuration will fit.

The chat shows “Shortening context… Chat history will be kept”, then a completion
notice explaining that earlier details are no longer all sent to the model.
Service notices do not enter model context. A failed fit is not labelled completed.
This is context reduction, not an AI-generated memory summary. Full saved
transcripts remain on disk, subject to the existing 8 MiB chat loading limit.

The history tool searches only this chat's turn-start snapshot, by literal
case-insensitive query and optional zero-based start index. Up to 10 excerpts are
returned; each has its original index for pagination. It never opens another chat,
filesystem path or external agent's history. Past messages are untrusted data and
do not change current permissions. Tool use remains disabled in Chat-only mode.
Semantic recall is model-dependent; literal search is not universal meaning search.

Successful identical reads within a turn reuse the saved result; writes, commands
and installations invalidate that cache. A new turn reads afresh; read refresh:true explicitly bypasses the cache. For supported
short latest-text open/read requests, the app displays the exact retrieved text
immediately, without model inference or repeated reads. TXT, Markdown, JSON, LOG,
CSV, XML, YAML and supported configuration extensions are included. The displayed
file is literal text: embedded tool JSON/HTML/think markers are not instructions,
execution cards or hidden reasoning. Complex analysis/edit requests still use the
model; binary media/APKs are not automatically read as text or executed.

Same com.termux and signing identity, code1120, no data reset. New optional history
fields are ignored by old versions, but their UI may show file/service messages
as ordinary text or unconfirmed tool previews when a file contains tool JSON; these are not automatic executions, and runtime-compaction features are absent after downgrade. Existing
agent permission downgrade limitations continue. No APK installation or GitHub
publication is part of this local test delivery. Device validation is pending.
