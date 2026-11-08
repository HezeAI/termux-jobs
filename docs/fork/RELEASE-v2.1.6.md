# Termux Job’s 2.1.6 — RTK and smoother chats

A refined chat and sidebar interface, optional per-chat RTK output reduction, and
measured output statistics. Changes below are relative to the published v2.1.5.

**[Download APK — versionCode 1532](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.6/Termux-Jobs-v2.1.6-arm64.apk)**

## Updated interface

- Four collapsible sidebar cards for terminals, agent chats, servers and the application. Section choices persist. Opening the sidebar expands the session currently on screen; status updates respect a manual collapse.
- Selected chat and terminal rows share the chat area's black background. Thin borders, corners, icons and selection behavior remain.
- Compact right-aligned user bubbles, gentler message/action/image-card arrivals, and height updates that retain arrival progress. History restoration does not replay appearance animations.
- The chat scroll indicator appears for manual scrolling and inertia, not automatic content changes. The Stop outline pulses gently while work is active; reduced-motion settings remain supported.
- Terminal input switches stay on one row and shorten the extra-keys label when needed. Terminal closing uses “Save and close” without the misleading history checkbox.

## RTK Lite / RTK native («RTK штатный»)

- Optional, mutually exclusive modes for Codex and Claude in Ubuntu AI. RTK Lite keeps the app's limited verified command handling. RTK native uses the selected RTK OSS version's native rewriting and broader supported command set; it does not promise reduction for every command.
- One component installation per Ubuntu AI environment, with separate selection, statistics and recovery per chat. Preparation has verified downloads, progress and cancellation. Working chats are never restarted automatically.
- [RTK 1.1.0-jobs.1](https://github.com/HezeAI/termux-jobs-components/releases/tag/rtk-1.1.0-jobs.1) downloads from Termux Job’s Components with pinned revision and SHA-256 checks. This APK contains no embedded test archive. Existing compatible jobs.1 is reused; an explicit versioned upgrade from upstream 1.1.0 retains the old binary and chat data.
- Output accounting uses captured bytes before filtering and matched text actually returned to the agent, without rerunning commands. Head/tail windows are compared with the requested window, not the whole file. Unmeasured or altered results remain unknown.
- Full-output recovery stays available. Additional recall text is counted separately; merely saving a recovery file is not counted as model input. Lite does not double count native measurements.
- A short selected-mode summary uses estimated tokens for display. Exact bytes, recall and command counters remain in collapsed Details. This measures command output, not total context or subscription quota savings. Old saved estimates are retained without displaying them as current measurements.

## Saved chats and compatibility

- Saved, stopped chats can open project files, attachments/originals, Download export, Git changes, and Limits and context without starting the agent. Cached limits are labelled with their time; compaction requires a running chat. RTK can be prepared for the next launch.
- Android 8+ application, ARM64, same `com.termux` package and signing certificate. X11 retains its Android 11+ requirement. RTK jobs.1 requires Ubuntu AI ARM64 with glibc 2.39 or newer.
- Code 1532 exceeds published main code 1520 and manual rollback code 1521. No data reset or environment reinstall is required by this update; no storage schema migration was added. Actual installation over rollback and every historical profile combination have not been tested on a phone. Do not uninstall or clear data as an update workaround.
- This release rebuilds the developer-tested application code with stable version metadata only. No rollback APK is included. Previous releases remain available.
- Existing chats need an explicit restart to load changed adapters/hooks. A saved RTK choice is distinct from a confirmed connection. Other chats are not restarted.

## Known limitations

- RTK native can change prefix-rule behavior after rewriting. Some Codex original-command forbids and Claude Bash deny/approval rules may no longer match. The enable dialog explains this; saved rules are not deleted. Codex untrusted confirmations were retained in the tested configuration. Results do not cover every CLI version or permission mode.
- Lite keeps stderr and exit codes, with unsupported calls following the ordinary path. Its Gradle support is a captured-output preview, not a dedicated native Gradle parser. Native streaming/complex paths and CLI-truncated results can have an unknown comparison baseline.
- Claude Auto availability remains version/account dependent; acceptance of the CLI setting does not verify the real-model classifier. Bypass remains unavailable when Claude rejects root inside Ubuntu/PRoot. Filesystem sandboxing is not implied by a working directory or approval mode.
- The known image-history Codex WebSocket issue is not fixed. See [upstream #55730](https://github.com/openai/codex/issues/55730) and [HezeAI’s published diagnostic report](https://github.com/openai/codex/issues/55730#issuecomment-6645086819). The reproduced direct-CLI failure did not require this interface; the remote closing component and a universal size boundary were not established.
- Low-level Android permission/key-handoff, crash recovery and backup restore gaps remain documented. No independent security certification or real-cloud-model RTK coverage is claimed.

<details>
<summary>Exact APK, sources, checks and device acceptance</summary>

The developer confirmed phone acceptance of 2.1.6-test10 /1531 and authorized
a stable rebuild with version metadata changed to 2.1.6 /1532 only. Individual phone scenarios were
not enumerated; this is not a claim of complete multi-device coverage. The assistant
did not install an APK automatically. Functional source inputs match the accepted file.

- APK SHA-256 and final size are recorded in the release validation and SHA256SUMS.
- ARM64 only, non-debuggable, existing certificate.
- A separate [pinned musl build-header archive](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.6/x11-musl-build-headers-1.2.6-r2-aarch64.tar.gz) preserves the original CI input removed by Alpine. It includes upstream copyright/license and SHA-256 metadata, is not an Android installer, and changes no runtime library in this APK.
- [SHA256SUMS](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.6/SHA256SUMS), [updater metadata](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.6/update.json), [verification JSON](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.6/validation.json).
- [Release validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.6/docs/fork/VALIDATION-v2.1.6.md), [accepted-build checks](https://github.com/HezeAI/termux-jobs/blob/v2.1.6/docs/fork/VALIDATION-2.1.6-test10.md), [RTK implementation and limits](https://github.com/HezeAI/termux-jobs/blob/v2.1.6/docs/fork/RTK-MODE.md), [security audit scope](https://github.com/HezeAI/termux-jobs/blob/v2.1.6/docs/fork/SECURITY-AUDIT-STAGE2.md), [third-party notices](https://github.com/HezeAI/termux-jobs/blob/v2.1.6/THIRD_PARTY_NOTICES.md).
- Release APK identity/signature/ZIP/DEX/native/assets and exact-byte checks run locally. Full Modern Lint and broad local JVM/helper results are recorded in validation. Local JVM tests use debug libraries; they do not replace final signed-release checks. GitHub CI checks the final candidate sources and an internal debug APK separately; results and exact source/workflow commits are recorded in validation.
- Native Codex/Claude integration evidence uses real CLI executables with isolated synthetic providers, not real cloud models. Exact checked permission modes and reused test inputs are listed in validation. GPU/X11 execution and real device rollback are not claimed.

</details>

AI development assistance: OpenAI Codex
