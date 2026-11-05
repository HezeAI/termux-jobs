# Termux Job’s v2.1.4 — Files, context and original images

Browse chat files and project changes, see working-context estimates and save original images.

**[Download APK](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.4/Termux-Jobs-v2.1.4-arm64.apk)** · [AI instructions ZIP](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.4/AI-INSTRUCTIONS.zip)

Changes since the last published stable release, **v2.1.3**:

- **Files and changes in all four agent chats.** The existing file manager stays available. Attachments has sender filters, local file-name search, availability, original-message navigation and per-file actions. Earlier history loads explicitly in bounded portions. Changes shows the entire selected Git project against HEAD, including staged/unstaged, new/deleted and binary files, with coloured text diffs and no write operations.
- **Readable file browsing.** Both file windows show refreshed modification dates. File cards wrap long names; compact refresh/search and separately outlined actions leave more room for content. Late native history cursors keep earlier messages accessible. Browsing, previews and Git inspection do not start an agent turn or add model context.
- **Original images to the gallery.** Save user/agent images to Pictures/TermuxJobs through MediaStore, including animated GIF, without re-encoding. Repeated names create new files. Save to Download remains available. Formats/rendering depend on Android and the gallery; original metadata is retained.
- **Native working-context indicators.** Expanded Codex, Claude and Gemini status uses current-context data, including cached tokens, rather than cumulative conversation spending. Missing, stale or unknown-window data is explicit. Codex adds confirmed manual compaction and waits for completion; history remains, but summaries can omit details.
- **Whole-request timers and focused notes.** All four chats keep one timer across tools, approvals and retries. The star opens notes directly; cancelling Save input leaves the draft intact.
- **Updated common instructions.** Four exact Codex/Claude RU/EN templates apply through preview and explicit saving. The optional AI-INSTRUCTIONS.zip contains these common templates and an English guide, not project instructions or memory files. Saved custom instructions are not replaced automatically.
- **Larger isolated previews.** HTML up to 32 MiB and GIF up to 64 MiB, with the existing 16-million-pixel GIF limit, network/file isolation and WebView cleanup. Attachment sending and model-context limits are unchanged.

## Requirements, compatibility and limitations

ARM64; Android 8+ app, Android 11+ X11. Same Termux Job’s `com.termux` package/signing certificate, schema 8; versionCode 1492. No reset or environment reinstallation is required. Saved profiles, authorization, history and custom instructions are not automatically migrated/replaced. Already-running CLI processes retain their loaded adapter until an explicit idle-session restart with conversation continuation; this publication performs no installation or working-session restart.

Installing over official Termux is unverified: a matching package alone is insufficient. Uninstalling can delete data and is not an ordinary update step. A working directory is not an OS sandbox; allowed shell commands may access other paths. Backups are unencrypted and may contain authorization, history and project snapshots.

**Attachment accumulation/disconnections remain unfixed.** No CLI fork, experimental context mode or new automatic-compaction policy is enabled. Low-level Android Gemini key handoff/ownership/cancel/crash recovery, device backup export/restore and real Gemini API/Vertex login remain unverified. Legacy-device/gallery-format and heavy renderer cases retain their documented limits. MCP/plugins are excluded. No independent security certification is claimed.

<details>
<summary>Verification, phone acceptance and artifact details</summary>

- **Local release:** final signed APK identity, ZIP, certificate, DEX/runtime, exact four templates and seven unchanged stable ELF payloads are checked separately from debug tests. Full modern Lint covers all five modules. Warnings/toolchain limits remain documented.
- **CI debug:** Fork CI tests the tagged source and builds an internal debug APK. Its signer/artifact is separate from the locally signed published release. The exact run, outcome and test counts are attached in validation.json and the published release's verification details.
- **Phone, developer-reported:** 2026-11-05 acceptance of 2.1.4-test6 /1491 covers agreed history/earlier messages, the file window, search, cards and JPG/GIF gallery saving. This is the user's observation, not an assistant device test or proof of every agent/Android/format combination.
- **Not checked:** the Android handoff/crash/backup and account-login areas listed above are not converted to PASS. Synthetic providers, fixtures and local success do not establish a fix for external image-related disconnections.
- **Archive:** AI-INSTRUCTIONS.zip contains only four common templates extracted byte-for-byte from the final APK plus README.txt. SHA256SUMS covers it and the APK/update metadata. No project AGENTS.md/CLAUDE.md, LEARNING.md or private project instructions are included.
- [Release validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.4/docs/fork/VALIDATION-v2.1.4.md) · [Audit scope](https://github.com/HezeAI/termux-jobs/blob/v2.1.4/docs/fork/SECURITY-AUDIT-STAGE2.md) · [Image-context investigation](https://github.com/HezeAI/termux-jobs/blob/v2.1.4/docs/fork/MEDIA-CONTEXT-INVESTIGATION.md) · [Backup contents](https://github.com/HezeAI/termux-jobs/blob/v2.1.4/docs/fork/BACKUP-RESTORE.md) · [Third-party notices](https://github.com/HezeAI/termux-jobs/blob/v2.1.4/THIRD_PARTY_NOTICES.md).

</details>

AI development assistance: OpenAI Codex
