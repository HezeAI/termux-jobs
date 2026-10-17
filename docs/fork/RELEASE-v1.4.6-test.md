# v1.4.6-test — local AI session fixes

Private test build, versionCode1112. No GitHub publication.

- Unsaved local sessions offer Add to launch presets; Delete preset is shown only for saved profiles.
- Local AI settings include a validated session-name field, including active launch presets. Rename preserves chat identity/history and loaded model.
- Streaming uses focus-neutral scrollTo instead of fullScroll; hidden local panels do not update streaming message views or scroll. Background state changes do not redraw an ordinary terminal's status header. No terminal input lock was added; sending a second local request while busy remains rejected and preserves the draft.
- Settings for an unchanged chat no longer reload history. Updated action/scope/approval settings are used on the next turn without model restart; changing model/backend still stops the previous model. Settings remain unavailable during an in-flight operation.
- Current capability instructions explicitly identify real local file tools and override stale assistant denials in history. A stale cloud-only/access denial receives the existing single repair attempt; model compliance is not guaranteed.
- Folder button opens the existing file manager at the local session's selected environment/project (Termux, Ubuntu or installed proot alias). Shared-storage paths use the existing permission request. Manual browsing does not change the AI's permissions.

Compatibility: same package/certificate, increased code, no reset; no history deletion. The permissions policy from1.4.4 is unchanged. Local models/chats still are not included in the existing app backup manager. Old rollback1110 cannot update this1112 build; no new recovery build requested.

Device checks: type and edit a draft during local streaming, switch to another terminal and type, return and inspect drafts/keyboard/scroll; rename active and saved local sessions; change access in the same chat then give a concrete file task; open project folder in each available environment and test storage permission flow.
