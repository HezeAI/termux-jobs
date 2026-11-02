# v2.1.1 — Agent instructions and clearer chat control

- **Agent instructions:** edit the actual Codex/Ubuntu AI global instructions and a project's AGENTS.md or AGENTS.override.md from general settings. Preview RU/EN templates, import text through the built-in file browser, inspect instruction sources, and restore earlier revisions. Atomic saves detect editing conflicts and preserve custom instructions during updates.
- **Interactive Codex questions:** answer supported native questions with options or free text in the ordinary chat mode. Replies use the original question channel and do not require stopping the request or silently entering Plan mode.
- **Safer message delivery:** retain unaccepted text and attachments, distinguish API admission, CLI queueing and current-request admission, and attach compact acknowledgements to the correct user message. Claude supports follow-up input through its native queue. Admission is not proof that a model has incorporated a message; ambiguous requests are not automatically resent.
- **Clearer request state:** agent-specific circular indicators, request-scoped Stop confirmation, native reconnect details and transient execution dots distinguish running work, user actions, reconnects and errors. Native-terminal attention borders remain available. Final answers can show a stable request duration and completion time when reliable data exists; tapping reveals the full date and timezone.
- **More reliable editing and navigation:** command, note, alias, session and preset forms close only after a confirmed save; failed saves retain drafts. Advanced groups backup, environment updates, storage cleanup and troubleshooting; About remains last. Agent forms show concise access/approval guidance and omit the extended-debug switch.
- **Files and action details:** recognize image formats consistently across files, attachments and previews, with an original-file fallback when decoding is unavailable. Clickable web-page previews have padded outlines and retain existing viewing, copying and browser actions. Basic-settings backup descriptions clarify their actual scope and exclusions.

## Compatibility

Same com.termux package, signing identity and workspace schema8; ARM64; versionCode1454. No data reset or environment reinstallation is required by these changes. No incompatible settings migration is introduced. Native history and user-defined instructions are retained; optional timing metadata does not expand existing backup formats. Finish important work before updating. Existing agent processes load changed adapters only after an explicit restart with conversation continuation. CLI capabilities depend on the installed version.

The legacy user-facing Ubuntu-to-Ubuntu-AI migration action is removed; ordinary environment selection, preparation, import, backups and internal update compatibility remain.

## Verification and limits

Selected automated checks cover the changed chat, editor, persistence and safety paths. The final APK and full five-module modern Lint are checked separately; see the [validation report](https://github.com/HezeAI/termux-jobs/blob/v2.1.1/docs/fork/VALIDATION-v2.1.1.md) for exact results and remaining warnings. HezeAI confirmed execution dots, response timing and website cards on a phone; other scenarios are not attributed to that device check. No fix for the other phone's network interruptions is claimed.

Publication note: released on 2026-11-02 at 04:02:35 UTC. Anonymous downloads of all four assets matched the prepared files; APK SHA-256 and update.json were verified. The attached validation.json `prepared-not-published` status and tagged report are pre-publication snapshots. See the [publication clarification](https://github.com/HezeAI/termux-jobs/blob/fork/base/docs/fork/VALIDATION-v2.1.1.md#publication-clarification). APK, assets and tag remain unchanged.

AI development assistance: OpenAI Codex
