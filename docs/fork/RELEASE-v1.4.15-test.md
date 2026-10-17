# v1.4.15-test / code1121

Local AI sessions and launch presets now save **Available history (context)** and
**Maximum response** separately. llama.cpp catalogue/imported GGUF models offer
4096/8192 context; LiteRT keeps the verified4096 option. Response maxima are
256/512/768/1024/2048 tokens for both engines. They are ceilings, not a promise of
long output. Output space is reserved from the context; a large answer limit
leaves less room for input/instructions, particularly on4096. Unsupported budgets
are rejected rather than silently substituted. Architecture/RAM compatibility of
individual imported models still requires device checks.

Defaults for existing profiles remain4096/768. Answer limit changes apply to the
next request without reloading. Context/model/backend changes on an idle loaded
session reload only its local engine with the same chat; history remains stored.
Busy settings changes are blocked until the response is completed/stopped. Reload
completion checks cancellation and panel identity before restarting, preventing
an unwanted restart after Stop/close. Actual budgets pass through the private IPC
to service and native/SDK generation, not just UI labels.

Empty AI placeholders are replaced by Preparing context / Finding files / Reading
file / Waiting for model text / Generating response. Actual answer text replaces
the status. The existing top activity indicator remains. No timer-driven chat
rebuilding is introduced.

Long-press a local session → Session diagnostics. The metadata-only report shows
model, engine/acceleration, configured context/reply limit, current stage and live
elapsed time, completed stage durations, time to first model text, generation
count and last-generation token counts/speed where available. llama.cpp reports
exact emitted-token counts; throughput is an observed approximation, not a
benchmark. LiteRT token counts/speed show unavailable, never guessed. Text can
include tool JSON: first model text is not necessarily the final answer. Multiple
model generations between tools are reported separately by stage timeline.

Copy/report path/Download export use the existing diagnostics dialog. Last-request
report is stored app-privately; opening diagnostics after a completed request
retains it. Event recording can be paused/resumed, with the choice remembered.
No prompt, file content, command text, tokens/credentials or tool paths are added
to these timings. Current stage remains observable while event recording is paused.

“Find the latest text file in Downloads and read it” (RU/EN, including the tested
«привет найди ласт тхт в довнолоад и прочитай») now uses the same authorized fast
locations → list → read → literal display path without intermediate model calls.
Analysis/edit/ambiguous compound tasks remain model-driven. Existing context
shortening, current-chat history search and rights apply to all local models.
External Codex/OpenCode/Claude/Gemini and ordinary terminals are unchanged.

Same com.termux/signing identity, increased code1121, optional v8 profile fields;
no reset or history/model deletion. Downgrades ignore the new budgets and revert
to4096/768. New native return format ships with its matching Java caller. This is
a local test build, not a GitHub release. Real model reloads,8192 memory/latency,
LiteRT response lengths, keyboard/scroll/status rendering need device validation.
