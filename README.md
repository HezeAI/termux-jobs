# Termux Job’s

**RU:** Termux Job’s — независимый неофициальный форк Termux, который поддерживает
[HezeAI](https://github.com/HezeAI). Проект не связан с командой Termux и не одобрен ею.
[Исходники](https://github.com/HezeAI/termux-jobs/tree/fork/base) ·
[Релизы](https://github.com/HezeAI/termux-jobs/releases).

**EN:** Termux Job’s is an independent unofficial Termux fork maintained by
[HezeAI](https://github.com/HezeAI). It is not affiliated with or endorsed by the
Termux team. [Source](https://github.com/HezeAI/termux-jobs/tree/fork/base) ·
[Releases](https://github.com/HezeAI/termux-jobs/releases).

A terminal workspace for saved commands/notes, aliases, presets, files, Linux
environments, agent chats and server tools. Commands execute in the selected real
environment. [Security, data flows and reporting / Безопасность и данные](SECURITY.md).

## v2.1.2

Changes since stable v2.1.1. See the [release](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.2)
and [validation](docs/fork/VALIDATION-v2.1.2.md).

- Manage Codex and Claude Code instructions, shared project memory and optional
  project-file creation with previews, revision history and conflict-safe saves.
- Browse, import and create agent skills; review exact destinations and source
  commits, update managed skills explicitly and restore their previous version.
- Prepare missing components in the shared Ubuntu AI with inline progress.
  New environments use a pinned GitHub Components image and private local-archive
  installer; existing environments and the system manager remain in place.
- Read older Claude messages in pages, retain the reading position and open full
  long answers without loading the entire native history at once.
- Open local HTML/GIF links and cards through the same isolated animated viewer
  using the original chat's environment. Download the original when needed.
- Use taller instruction/skill managers, direct session access, a vertical
  paperclip/star stack and clearer individual agent status animations.

[Release notes](docs/fork/RELEASE-v2.1.2.md) ·
[Validation and limits](docs/fork/VALIDATION-v2.1.2.md) · [CHANGELOG](CHANGELOG.md)

## Installation and compatibility

**RU:** Релизный APK — ARM64, приложение Android8+, X11 Android11+.
Обновление проверяется с предыдущими версиями именно **Termux Job’s**, когда
совпадают package ID `com.termux` и сертификат и увеличивается versionCode.
Установка поверх официального Termux из GitHub, F-Droid или Google Play не
подтверждена: совпадения `com.termux` недостаточно. Не удаляйте приложение как
обычный шаг обновления: это может удалить приватные данные. Сброс или переустановка
сред для этого релиза не требуются; APK не заменяет резервную копию.

**EN:** Use this fork's release APK: ARM64, app Android8+, X11 Android11+.
Update compatibility is checked with previous **Termux Job’s** versions having the
same package ID and certificate and a lower versionCode. Compatibility over official
GitHub/F-Droid/Google Play Termux builds is not verified; `com.termux` alone is
insufficient. Uninstalling is not a normal update step and can remove private data.
No reset/environment reinstallation is required by this release. An APK is not a
backup; save important work before updating.

**Release signing-certificate SHA-256 / SHA-256 сертификата:**
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

**APK file SHA-256 / SHA-256 файла:** see the release's `SHA256SUMS` and `update.json`.
These are distinct checks. Neither a digest nor a valid signature guarantees safety.

Agent capabilities depend on the installed CLI version. Existing processes retain
loaded adapters until an explicit agent restart with conversation continuation.
Instruction changes do not rewrite an executing request or override service rules.
A native acknowledgement does not prove that a model has incorporated a message.
No claim is made that another phone's transport interruptions are fixed.

## Workspace and documentation

- [Usage](docs/fork/USAGE.md): terminals, presets, commands, files and agent controls.
- [Agent instructions](docs/fork/AGENT-INSTRUCTIONS.md): actual sources, import,
  precedence, revisions, conflict handling and application timing.
- [Agent skills](docs/fork/AGENT-SKILLS.md): scopes, imports, creation, managed updates and discovery.
- [Agent environments](docs/fork/AGENT-ENVIRONMENTS.md): shared Ubuntu AI preparation
  and retained legacy bindings. The legacy manual migration action is removed.
- [Environment updates](docs/fork/ENVIRONMENT-UPDATES.md) and
  [pinned initial environments](docs/fork/PINNED-ENVIRONMENTS.md): explicit upgrades,
  replacement and rollback. Installed distributions can still use their repositories.
- [Architecture](docs/fork/ARCHITECTURE.md), [build notes](FORK.md),
  [testing policy](docs/fork/TESTING.md) and [release procedure](docs/fork/RELEASING.md).

Instruction text, chats and presets belong to the user. Basic-settings backups have
limited coverage; optional timing/admission metadata does not expand their format.
Phone checks for execution dots, answer timing and website cards were confirmed by
HezeAI. Other automated or modeled checks are identified separately in validation.
No independent security audit of this version is confirmed. Source availability and
test counts are not safety guarantees. See [SECURITY.md](SECURITY.md) for data flows,
permission limits, diagnostics/backups and the existing reporting channel.

## Лицензия и благодарности / License and acknowledgements

Developed by HezeAI. Based on [Termux](https://github.com/termux/termux-app) and the
upstream components documented in this repository. See [LICENSE.md](LICENSE.md)
and component notices for applicable licenses. Graphics components and their
sources/licenses are listed in [termux-jobs-components](https://github.com/HezeAI/termux-jobs-components).

Termux original code remains under GPL-3.0-only and its documented exceptions;
original copyright notices are retained. HezeAI maintains the fork changes.
See [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES.md),
[upstream exceptions](termux-shared/LICENSE.md) and
[dated license review](docs/fork/LICENSE-REVIEW-2026-11-03.md) for exact versions,
notices, source coverage and unresolved questions. Third-party authors retain their
rights; component distribution does not imply official endorsement.

Исходный код Termux сохраняет GPL-3.0-only и указанные исключения. HezeAI
поддерживает изменения форка. Лицензии библиотек, образов, шрифтов, моделей и
инструментов не заменяются лицензией Termux Job’s. Незакрытые проверки указаны
в отчёте; полное соблюдение всех условий заранее не заявляется.

AI development assistance: OpenAI Codex
