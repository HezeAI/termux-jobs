Termux Job’s 2.1.3 improves agent preparation and approval handling, adds reviewed project templates, and makes backup scope and chat controls clearer.

**[Download APK](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.3/Termux-Jobs-v2.1.3-arm64.apk)**

ARM64; Android 8+ for the app, Android 11+ for X11. Update from Termux Job’s with the matching package and certificate; no reset or environment reinstallation is required. Installing over official Termux is unverified. **Do not uninstall as an update step: private data can be lost. Backup archives are unencrypted and may contain credentials, history and project snapshots.**

Changes since the last published stable release, [v2.1.2](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.2):

- **Preparation:** actual Termux/Ubuntu/component phases, plan completion and measured download progress. A shared queue reuses existing Ubuntu AI and installs only missing components. Raw stdout/stderr is no longer accumulated or exported; downloads have connection/stalled-transfer limits and finite retries.
- **Agent control:** new Codex profiles confirm untrusted commands and direct edits through native approvals; saved profiles keep their policy. Gemini automatic CLI installation is disabled while version notices remain informational. App-entered Gemini keys use a run-owned one-shot handoff instead of a new plaintext handoff file.
- **Backups:** explicit unencrypted-archive warnings and separate, default-off OpenCode connection-data selection. Native history/configuration can be restored through the new component; legacy archives remain readable. Restored action receipts neither execute commands nor transfer one-time permissions.
- **Instructions:** the full editable project template targets Codex AGENTS.md or Claude CLAUDE.md, with exact paths, replacement confirmation and revision-conflict protection. Common RU/EN templates add context-management rules; saved instructions are changed only explicitly.
- **Chat:** transparent areas around the upper controls and lower composer, a horizontal filled star / paperclip pair and bottom-aligned Send. Known event times appear only in action/command group headers, including Claude; unknown times stay hidden. Proven OpenCode user denials are neutral, with distinct directory/action receipts retained after reopen; unrelated errors remain visible.

### Compatibility and limits

- Same `com.termux` package and signing certificate, increasing versionCode; ARM64, workspace schema 8. Existing profiles, accounts, projects, histories and environments are not reset or migrated automatically. Loaded adapters change only after an explicit idle-agent restart with conversation continuation.
- Backups containing OpenCode connection data use manifest v3 and require the new reader. Previous v1/v2 archives remain readable. Excluding connection data can leave restoration incomplete; Android Keystore keys are not transferred by copying an archive.
- **Attachment accumulation and related disconnections remain unfixed.** Investigation continues in the next cycle; existing images/files/history are retained. This release does not claim another phone's transport failures are resolved.
- Legacy preparation logs and older Gemini handoff files are not removed automatically. Review such material privately; the new mechanisms do not sanitize all historical files.
- A working folder is not an OS sandbox. Permitted shell tools may access files outside the project. Approval tests cover named tools and routes, not every CLI/TUI/manual launch.
- Low-level Android key handoff/Keystore/crash recovery and on-device backup export/restore remain unverified by agreement because no isolated Android instance is available. Real Gemini API/Vertex login is not confirmed by artificial-key rejection. MCP and plugins are excluded and unverified; full driver and third-party CLI traffic audits remain separate.

<details>
<summary>Verification, source identity, audit scope and technical details</summary>

[Release validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.3/docs/fork/VALIDATION-v2.1.3.md) · [Current audit matrix](https://github.com/HezeAI/termux-jobs/blob/v2.1.3/docs/fork/SECURITY-AUDIT-STAGE2.md) · [Backup scope](https://github.com/HezeAI/termux-jobs/blob/v2.1.3/docs/fork/BACKUP-RESTORE.md) · [Security and data](https://github.com/HezeAI/termux-jobs/blob/v2.1.3/SECURITY.md)

The first [CI run](https://github.com/HezeAI/termux-jobs/actions/runs/41819215609) for source `ab82d334` failed one exact composer-height assertion, with 8 conditional skips; its debug build was skipped. It remains a failed run. The correction uses actual measured field height and retains the strict equality test. Final-source CI identity and status are recorded with this release.

The final signed **release APK is built and verified locally**. Full modern Lint covers all five modules. Local JVM tests use the debug variant; GitHub Fork CI likewise runs internal debug tests/builds and does not validate this signing key or replace the final release-APK checks. No debug APK is required as a release asset. Skipped tests are reported separately, never counted as passed.

HezeAI accepted the candidate on the phone, explicitly including transparency. Earlier named approval tests and the Gemini artificial-key/API-refusal/normal-restart checks remain attributed to developer reports. Fixtures do not close the agreed Android-device gaps. No independent security audit or universal safety guarantee is claimed.

The final APK is **40,481,508 bytes**, `com.termux` **2.1.3 / 1485**. APK SHA-256: `c96127a70424936155ef8a76e8fdaf02b882dd98411ade1c4c3ecc18c1ad931c`. Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`. SHA256SUMS and RU/EN update.json refer to this same artifact. Release signing checks the pinned public certificate; a matching certificate proves continuity, not the historical secrecy of the private key. Source and required notices are in this tag. Pinned native payload reuse and toolchain limitations are recorded in validation; existing published APKs, tags and binary Components are unchanged.

[Third-party notices](https://github.com/HezeAI/termux-jobs/blob/v2.1.3/THIRD_PARTY_NOTICES.md) · [License/source review and remaining coverage](https://github.com/HezeAI/termux-jobs/blob/v2.1.3/docs/fork/LICENSE-REVIEW-2026-11-03.md) · [SHA256SUMS](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.3/SHA256SUMS) · [update.json](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.3/update.json)

</details>

AI development assistance: OpenAI Codex
