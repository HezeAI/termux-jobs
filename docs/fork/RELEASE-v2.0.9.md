# v2.0.9 — Environment aliases & agent chat scale

- **Persistent Alias** for eight installed terminal environments: Termux, Ubuntu, Debian, Alpine, Arch Linux, Fedora, openSUSE and Void. Choose the name, command, environment, category and favourite status. Ubuntu AI is excluded.
- **Separate command and alias organization:** independent categories, favourites and searches, plus **Commands / Alias / Notes** in the star picker. Preview, insert into a matching terminal draft, edit or delete an alias without executing it automatically.
- **Import/export Alias** in Backup and restore: import from a shell file or JSON/backup; export to a shell file or JSON. Preview and select records before applying, confirm replacements and preserve metadata through JSON. Shell import reads supported literal declarations without executing the file; shell export handles one environment per file. Existing manually defined aliases are not automatically scanned into the library.
- **Chat scale for Codex, Claude, Gemini and OpenCode:** live **50–150%**, default **100%**, saved separately for each session, with a **100% reset**. Messages, Markdown/code/tables, tool/file cards, thumbnails and input rows scale together. Smaller values fit more content on screen; app navigation and the system keyboard keep their normal size.

Aliases become available in new terminal shells. Restart the persistent Termux tab after adding or removing its aliases; existing shells retain already loaded definitions until reopened. Imported files are command data: review the full preview before confirming.

Same `com.termux`, signing identity, ARM64 and workspace schema8; Android8+ app / Android11+ X11; versionCode **1417**. **No data reset, account migration or environment reinstallation is required.** Existing histories, presets, projects, environments, commands and notes are retained. Old sessions default to100% chat scale. Finish important work before updating. X11 renderers, drivers and native libraries are unchanged.

Relevant automated checks cover alias installation, literal transfer and rollback boundaries, persistence and live chat-layout scaling. Modern lint reports zero errors; remaining warnings and device-specific limits are documented in [validation](https://github.com/HezeAI/termux-jobs/blob/v2.0.9/docs/fork/VALIDATION-v2.0.9.md).

[Alias guide](https://github.com/HezeAI/termux-jobs/blob/v2.0.9/docs/fork/ALIASES.md) · [Chat scale](https://github.com/HezeAI/termux-jobs/blob/v2.0.9/docs/fork/AGENT-CHAT-SCALE.md) · [Sources and licenses](https://github.com/HezeAI/termux-jobs/blob/v2.0.9/docs/fork/THIRD-PARTY.md)

AI development assistance: OpenAI Codex
