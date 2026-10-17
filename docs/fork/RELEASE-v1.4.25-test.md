# v1.4.25-test — compact file-list context

Shared local-AI instructions recommend explicit type/extension filters, modified_desc and requested count (limit1 for a single newest TXT). This is model guidance, not forced command rewriting: users can still request larger lists.

After actual execution and file selection, list/search/screenshot metadata sent to inference uses columns/rows: full path, type, sizeBytes, modifiedMillis, match. Duplicate basename and repeated object keys/notices are removed. Keep checkedPath/status/checkedLocations, selectedPath, sort/filter, query variants, matched and truncated information. Unknown metadata uses null. All returned rows/order are preserved, including200-entry lists; compact JSON is not cut at the old8000-character preview boundary. Existing engine context limits remain applicable.

Raw tool results are unchanged in saved chat and UI; selection/permissions use the original result before compaction. Read-file contents, failure outputs, other tools and malformed listings retain existing behavior. No new searching heuristics or permission changes.

Applies to all local llama.cpp and LiteRT models using the shared LocalAiAgent. No change to external Codex/OpenCode/Claude/Gemini CLI sessions. LiteRT token counts remain estimated; native llama CPU/kernel/cache changes are not part of this update.

Compatible update: com.termux/versionCode1131/existing certificate; no reset or schema change. Not published to GitHub. Model compliance and actual speed require developer device verification.

Draft tool previews now say Draft tool call — not executed and clarify that they are not approval cards. Instructions explicitly distinguish local filename search from web/Play Store and advise checking available build/archive tools through an allowed command instead of inventing blanket limitations. Mixed prose is still not executed as a tool call.
