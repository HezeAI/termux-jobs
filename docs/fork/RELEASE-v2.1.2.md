# v2.1.2 — Agent skills, Claude instructions and Ubuntu AI setup

Changes since the previous stable release, v2.1.1.

## New capabilities

- **Claude Code instructions:** manage CLAUDE.md and its complementary CLAUDE.local.md alongside the Codex instruction editor. Agent-specific explanations and diagnostics describe actual file sources and when changes take effect. Both agents can use the same project LEARNING.md and retain a selected custom memory path.
- **Agent skills:** browse general and project skills for Codex and Claude Code; add skills from a public GitHub repository, a local folder or ZIP, or create a SKILL.md in the app. Review instructions, files, source revision and exact destination before confirming. App-managed skills support explicit updates, local-conflict detection, rollback and removal. External/plugin-owned skills remain distinguishable.
- **Project instruction creation:** fill optional project purpose, paths, constraints, checks and documentation links, then review the text and destination before saving AGENTS.md or CLAUDE.md. Existing files open for editing. No unknown commands or paths are invented, and opening the form creates no files.
- **Session access:** Codex and Claude session menus open Instructions and Skills with the original agent, environment and project. Switching chats does not redirect an open manager.

## Changed behavior

- New AI Agents sessions use the shared Ubuntu AI. Existing ordinary-Ubuntu and direct-Termux Codex profiles keep their actual bindings when edited or launched.
- A missing Ubuntu AI is installed from a fixed, SHA-256-checked clean ARM64 image hosted in the separate Components repository. A bundled, private proot-distro5.9.0 installs the local archive without Docker Hub. Existing Ubuntu AI and the system proot-distro are not replaced. All four agents share the environment and add missing components through their existing recipes.
- Check launch environment now prepares missing components and shows stages, measured download progress and an expandable redacted log in the window. Repeated opening joins the current operation; closing the window does not cancel preparation. Component readiness remains separate from account authorization and model access.
- Instruction and skill managers use the available window height, keep tabs/actions accessible and scroll content between them. Instruction refresh is a header icon; shared-template preview and explicit application live together in General. Updated RU/EN templates retain project-memory and documentation rules, and never replace custom instructions automatically.
- Agent chat places the paperclip above the star inside a shared outline, with separate touch targets and press feedback. The message field uses the remaining width; Send stays at the bottom right. The separate command-without-model field is removed, and the interactive-input header control is disabled in agent chat. Native terminal access and ordinary terminal input remain available.
- About and README clearly identify the independent unofficial fork and distinguish upstream authorship/support. Required third-party license notices are included in the APK; the license/source review records exact coverage and remaining questions.
- Codex, Claude, Gemini and OpenCode have clearer individual icon animations while working. Waiting, reconnecting, error and idle states remain distinct; scoped Stop confirmation and reduced-motion behavior are retained. These are application-designed effects.

## Fixes

- **Claude history:** bounded native-history pages replace the previous recent-snapshot horizon. Earlier loaded messages survive refreshes; paging preserves the reading position, and Show fully retrieves complete long responses. Stable record identities prevent duplicates when continuing or reopening a conversation. Dense tool logs continue across pages instead of losing older rows. Native history files are read only.
- **Local file previews:** links and cards use the same file viewer for all four agents and the originating chat's Ubuntu AI, even after switching chats. HTML supports JavaScript controls in an isolated viewer; GIFs animate. Missing/unsupported files show a reason, and downloading the original remains available. HTML has no application command bridge or arbitrary environment-file access.

## Compatibility and limits

Update compatibility is checked with previous **Termux Job’s** releases using package `com.termux`, the same verified signing certificate and increasing versionCode1476; workspace schema8 and ARM64 remain. App Android8+, X11 Android11+. No data reset, incompatible settings migration or environment reinstallation is required. Existing user instructions, native history, projects, CLI installations and legacy environment bindings are not automatically replaced. Existing incompatible or incomplete installations are reported and preserved rather than silently deleted or upgraded. An APK is not a backup; finish important work before updating. Compatibility over official Termux from GitHub, F-Droid or Google Play is not verified; matching package ID alone is insufficient. Uninstalling can delete private data and is not a normal update step.

Existing agent processes require an explicit idle restart with conversation continuation to load changed adapters. Saving instruction files or installing a skill does not prove that a running agent has loaded them; discovery status and agent-specific application timing are shown separately. Skills are third-party instructions: import does not run their scripts or install declared dependencies. Private GitHub repositories and a broad online catalog are not included.

Claude history supports the pinned native format with a32MiB per-record safety bound. Unresolvable cross-file parent links cannot be reconstructed automatically. Actual keyboard/layout, preview animation and scrolling behavior on additional devices remain separate acceptance checks.

HezeAI confirmed Claude working on another phone after the GitHub-image installation change. The cause of that phone's earlier Docker Hub failure remains unknown; this release does not claim a diagnosed network fix or universal clean-device installation success.

The signing-certificate SHA-256 is `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`; the separate APK file digest is in SHA256SUMS/update.json. Neither signature verification nor test counts guarantee safety. No independent security audit of this version is confirmed. The [license/source review](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/docs/fork/LICENSE-REVIEW-2026-11-03.md) distinguishes verified APK/Ubuntu source coverage from remaining Components checks; it is not a blanket compliance certificate. See [security and data flows](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/SECURITY.md) for local storage, provider/agent access, networks, diagnostics/backups and reporting.

See the [validation report](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/docs/fork/VALIDATION-v2.1.2.md), [instructions guide](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/docs/fork/AGENT-INSTRUCTIONS.md) and [skills guide](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/docs/fork/AGENT-SKILLS.md) for checks and limits.

AI development assistance: OpenAI Codex
