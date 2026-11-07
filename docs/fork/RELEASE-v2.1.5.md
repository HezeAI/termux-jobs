# Termux Job’s v2.1.5 — A redesigned workspace

A major interface update for agent chats, terminals, the sidebar and settings.
The new layout brings project tools and session controls closer to each chat,
with smoother navigation and more compact file and attachment windows.

**[Download APK — new interface, versionCode 1520](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.5/Termux-Jobs-v2.1.5-arm64.apk)**

## What changed since v2.1.4

- Redesigned chats, terminal screens, floating headers, nested sidebar groups and general settings. Long chat names and paths are shortened to keep the sidebar compact.
- Centered agent selection and chat creation, forward/back page transitions, retained form values and a separate working-folder picker. Session names refresh immediately in the sidebar.
- Agent terminals slide over the visible chat. Drafts, reading position and bottom anchoring are preserved; switching views does not restart the agent.
- Updated input rows, Notes labels, compact plus menus, larger session action menus and red Stop symbols. Terminal extra keys have one tall Enter key beside PgUp/PgDn, yellow active modifiers and no second swipe-to-input page.
- Compact Files, Attachments and Changes tabs. Available attachments show thumbnails and open originals; attachment actions include Go to message, Save to Download and Location. Exports keep the original file and avoid overwriting names.
- Clearer original-image errors, separate image-view actions and expandable action timelines. Claude image references retain the environment/path needed to open the original.
- Input history has clearer empty-state text, a compact confirmed Clear action and centered library headings. Clearing input history leaves conversations, commands, aliases and notes intact.
- Saved chats close with Yes/No while retaining the saved conversation. Desktop stop keeps its progress indicator, blocks repeated taps and disappears after confirmed shutdown.
- Claude subscription windows use native usage data; official subscription links are available. Native action timelines are grouped without hiding image-view actions. Mid-turn Claude message handling retains native IDs.
- Claude Auto and Bypass permissions are explicit, mutually exclusive checkboxes. The prior ordinary mode is restored when both are off. Active permissions and next-launch settings are shown separately; saving never restarts a working chat.

## Known Codex WebSocket issue — not fixed in this release

Image-heavy Codex conversations can retry or disconnect after reopening or reconnecting,
even when the new prompt is only text. Our Codex 0.166.0 investigation reproduced failures
with direct `codex exec`, without the Termux Job’s Android renderer or Node adapter.
The reproduced failure therefore does not require this interface. Native cold replay
can resend accumulated inline images; warm WebSocket continuation can remain small.

See the existing **[upstream issue #55730](https://github.com/openai/codex/issues/55730)** and **[HezeAI’s published report](https://github.com/openai/codex/issues/55730#issuecomment-6645086819)**.
The public attachment was downloaded and verified byte-for-byte against our report.

Our real-service series recorded retries in all 12 cold attempts at approximately
20–35 MB of request JSON. Ten completed after automatic HTTP fallback; two reached
the 240-second observation deadline. Three approximately 10 MB controls completed
without retries. We did **not** capture Close 1009 in that series, independently
verify the upstream 16 MiB boundary, or identify which remote component closed every
connection. Route and backend variation remain possible; these findings do not
attribute every WebSocket error to Codex or rule out all connection problems.

Automatic HTTP fallback recovered some requests. Manual native compaction reduced
one tested image context, but new image views accumulated again; summaries are not
lossless visual memory. Neither is a general fix. The pinned built-in provider does
not offer the tested config override as a reliable forced-HTTPS switch. No image-history
rewriting, automatic deletion or transport fix is introduced here.

## Requirements, permissions and update compatibility

- ARM64; Android 8+ for the app, Android 11+ for X11. Same `com.termux` package and release certificate; schema 8 remains in use. No data reset or environment reinstall is required. An APK is not a backup; save important work before updating.
- The in-app updater installs **only the new-interface APK, code 1520**. Compatibility over official Termux/F-Droid/Google Play builds is not established. Do not uninstall as an update step.
- Existing agents keep loaded adapters until an explicit restart of that chat. Settings marked for the next launch do not change a running process.
- Claude Auto is exposed by installed CLI 2.1.296 and its control-channel acknowledgement was checked. Genuine model/account classifier decisions were **not** verified. Runtime rejection is shown; unsupported modes are not presented as enabled.
- Bypass permissions is unavailable when Claude runs as root inside Ubuntu/PRoot. The native restriction is not bypassed. Working directories and approval modes are not an OS sandbox.
- Device-specific UI, Android permission/installer, crash recovery and backup export/restore gaps remain documented. The image-opening scenario was reported working by the developer, not independently reproduced on every phone or agent. No independent security certification is claimed.

## Optional manual rollback — old interface

**[Download old-interface rollback — code 1521](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.5/Termux-Jobs-v2.1.5-rollback-1521-arm64.apk)**

This separate APK contains the accepted interface from before the redesign, with a
higher versionCode so Android can offer installation over code 1520. It is never the
in-app update target. Existing reserve1499 is unchanged.

**Before rollback, disable Auto/Bypass in every saved Claude profile and save an ordinary
mode.** The legacy validator does not understand those new mode values. The rollback
is not a copy of your data, and actual device downgrade/data retention is unverified.
After installing code 1521, code 1520 cannot update it normally; a future main APK must
have a code above 1521. Do not uninstall or clear data as a workaround.

[Rollback sources](https://github.com/HezeAI/termux-jobs/tree/v2.1.5-rollback) · [Rollback conditions](https://github.com/HezeAI/termux-jobs/blob/v2.1.5-rollback/docs/fork/ROLLBACK-v2.1.5.md)

<details>
<summary>Build, validation, source and remaining checks</summary>

The main signed release APK and the optional rollback are built and checked locally.
GitHub CI checks JVM suites and an internal debug APK separately; it does not sign or
validate the published release APK. Local fixtures do not count as phone tests.

- [Release validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.5/docs/fork/VALIDATION-v2.1.5.md)
- [Security audit scope](https://github.com/HezeAI/termux-jobs/blob/v2.1.5/docs/fork/SECURITY-AUDIT-STAGE2.md)
- [SHA256SUMS](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.5/SHA256SUMS)
- [Updater metadata — main APK only](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.5/update.json)
- [Machine-readable verification](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.5/validation.json)
- [Third-party notices](https://github.com/HezeAI/termux-jobs/blob/v2.1.5/THIRD_PARTY_NOTICES.md)

- Main [CI](https://github.com/HezeAI/termux-jobs/actions/runs/42387377279): 1,909 JVM cases passed, 8 SFTP cases skipped; internal debug build passed. Rollback [CI](https://github.com/HezeAI/termux-jobs/actions/runs/42385931862): 1,774 passed, 8 skipped; debug build passed.
- Local signed APK checks passed for both files. Full Modern Lint: main 0 errors / 281 warnings; rollback 0 errors / 250 warnings. Local verification: 102 targeted JVM cases (including the eight SFTP cases), 33 Node suites and 59 Python cases passed.
- Initial CI download failures and 14 outdated test expectations are retained in the validation report. The later successful runs do not erase those results. Phone/real-model limits are listed above.

No new third-party library or native component is introduced. Published component
binaries and corresponding-source resources remain unchanged. Existing native payloads
are reused only after input and output identity checks. MCP/plugins, independent
security certification and the documented low-level Android audit gaps are not claimed.

</details>

AI development assistance: OpenAI Codex
