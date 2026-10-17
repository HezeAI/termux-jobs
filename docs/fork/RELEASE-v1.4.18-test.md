# v1.4.18-test / code1124

Short latest-file requests such as «покажи ласт тхт», «посмотри ласт тхт на
телефоне» and “show latest apk on my phone” are recognized without a folder.
The app immediately asks which folder to check instead of passing this ambiguous
request to inference or silently assuming Download. With an explicit known folder,
existing authorized discovery and direct text display remain. Negations, explicit
paths/names and compound actions keep their existing paths; chat-only has no tools.

Screenshot discovery recognizes the tested phone/latest question and RU/EN phone
suffixes. Text-only runtimes return actual discovered paths and an explicit inability
to visually inspect images, without an unnecessary model generation or invented
Screenshots tab. Vision-capable model configurations retain their tool/model flow.
Search still uses selected environment and normal scope, rights and cancellation.

At the next user turn, earlier tool-result texts longer than1024 characters use a
clearly marked head/tail excerpt in the runtime context. Source path is retained
where available; the marker points to current-chat history recall or a fresh file
read. Full tool output and literal document display remain saved and unchanged.
User messages, current-turn tool results, permissions and system instructions are
not cut by this rule. A chat notice identifies use of previous-result excerpts.
This helps prevent a displayed report from adding thousands of input tokens to
every subsequent question; it does not make CPU cold prefill intrinsically faster.

All local models use the coordinator/context behavior. Native llama cache/timing and
LiteRT inference settings are unchanged. General small-model understanding is not
guaranteed by these targeted fast paths. No silent automatic installation or new
access to directories is introduced.

Same package/certificate,1.4.18-test/code1124 above1123. No library schema/history
reset. Local test delivery only; no GitHub publication. Device check: ask latest
TXT without folder (expect clarification), then specify Download (expect actual
file); ask latest screenshots (text model: paths, no vision claim); export the
next ordinary-question diagnostic and compare input volume after reading a report.
