# v1.4.12-test / code1118

Shared local-model file-name discovery now accepts case/suffix matches and approximate Russian/Latin spellings. For example факторио can match Factorio_v2_mod.apk. Explicit search variants (up to eight known names) can be supplied by the model. No dictionary is downloaded, filenames/paths are never rewritten, and arbitrary Russian-to-English title translation is not guaranteed. Candidate entries mark approximate matches; uncertain results require user choice. Scope/mounts/Android permissions still apply; search is not an exhaustive device scan.

Named search (or a directory list with an explicit name query) producing multiple files pauses the turn. One approximate match also requires selection. The chat card opens a scrollable list with name, full path, size and modification date. Only an exact returned candidate path may be selected; directories and relative paths are not selectable files. The next model context receives the selected file, not a silently chosen first candidate. Selection never authorizes opening/executing/installing/writing, nor broadens project permissions. Subsequent tools pass normal validation/approval.

Cancel or Stop ends waiting without continuing the model against a file. A restored chooser is read-only; pending selections do not execute or resume after restart. Picker windows close with the UI. File listings and screenshot details are readable text with matched/shown counts, limited-list notice, folder status summary and a separate Technical data button for raw JSON. Locating an image does not imply vision support.

All local engines, including imported models, share this code; Codex/Claude/Gemini/OpenCode controller assets are unchanged. Package/signing identity remain com.termux and the existing certificate; code increases to1118. No schema reset or history deletion. Older versions may show chooser history as text rather than the new card, but do not gain automatic selection/resume. Existing downgrade limitations for v1.4.10 permissions remain.

Private test build: no GitHub publication or installation. Device validation is needed for actual model search choices, Cyrillic/Latin candidates, picker/Stop, keyboard/scroll and shared-storage mounts.

The single local-model runtime is not replaced while another local turn is busy or awaiting input/selection. Normal terminal sessions remain usable. Saved file_selection protocol messages do not render as misleading command cards; the read-only chooser event represents the selection in history.
