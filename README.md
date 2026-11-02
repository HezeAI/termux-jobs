# Termux Job’s

Android terminal workspace based on Termux: saved commands and notes, aliases,
launch presets, projects, files, Linux environments, agent chats and server tools.
Commands run in the selected real terminal environment, not in a simulated shell.

## v2.1.1

Changes since public v2.1.0. See the [release](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.1) and [validation report](docs/fork/VALIDATION-v2.1.1.md).

- Edit actual Codex/Ubuntu AI global and project instructions, with import,
  localized template previews, revision recovery and conflict-aware atomic saves.
- Answer supported native Codex questions and send Claude follow-ups through its
  native queue. Per-message delivery evidence distinguishes admission from model uptake.
- Observe request execution, native reconnects and user waits with compact agent
  indicators, transient dots, scoped Stop confirmation and reliable answer timing.
- Save forms only after confirmed writes, protect instruction drafts and navigate
  backup/update/cleanup/help through Advanced. About remains last.
- Use consistent image recognition/fallback and outlined clickable website previews.

[Release notes](docs/fork/RELEASE-v2.1.1.md) ·
[Validation and limits](docs/fork/VALIDATION-v2.1.1.md) · [CHANGELOG](CHANGELOG.md)

## Installation and compatibility

Use the APK from this project's GitHub releases. The prepared candidate is ARM64,
Android8+ for the app; X11 requires Android11+. Package `com.termux` and signing
identity remain unchanged. Do not uninstall an existing compatible installation
just to update it: uninstalling can remove its private data. Finish important work
before updating. No data reset or environment reinstallation is required by this
release. An APK is not a backup of user data.

Agent capabilities depend on the installed CLI version. Existing processes retain
loaded adapters until an explicit agent restart with conversation continuation.
Instruction changes do not rewrite an executing request or override service rules.
A native acknowledgement does not prove that a model has incorporated a message.
No claim is made that another phone's transport interruptions are fixed.

## Workspace and documentation

- [Usage](docs/fork/USAGE.md): terminals, presets, commands, files and agent controls.
- [Agent instructions](docs/fork/AGENT-INSTRUCTIONS.md): actual sources, import,
  precedence, revisions, conflict handling and application timing.
- [Agent environments](docs/fork/AGENT-ENVIRONMENTS.md): Ubuntu/Ubuntu AI selection
  and preparation. The legacy manual migration action is removed.
- [Environment updates](docs/fork/ENVIRONMENT-UPDATES.md) and
  [pinned initial environments](docs/fork/PINNED-ENVIRONMENTS.md): explicit upgrades,
  replacement and rollback. Installed distributions can still use their repositories.
- [Architecture](docs/fork/ARCHITECTURE.md), [build notes](FORK.md),
  [testing policy](docs/fork/TESTING.md) and [release procedure](docs/fork/RELEASING.md).

Instruction text, chats and presets belong to the user. Basic-settings backups have
limited coverage; optional timing/admission metadata does not expand their format.
Phone checks for execution dots, answer timing and website cards were confirmed by
HezeAI. Other automated or modeled checks are identified separately in validation.

## Sources and credits

Developed by HezeAI. Based on [Termux](https://github.com/termux/termux-app) and the
upstream components documented in this repository. See [LICENSE.md](LICENSE.md)
and component notices for applicable licenses. Graphics components and their
sources/licenses are listed in [termux-jobs-components](https://github.com/HezeAI/termux-jobs-components).

AI development assistance: OpenAI Codex
