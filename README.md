# Termux Job’s

**RU:** Терминал с интерфейсом для повседневной работы: сохраняйте команды и
заметки, организуйте проекты и файлы, запускайте Linux-окружения и общайтесь
с AI-агентами. Команды выполняются в выбранном Termux, окружении или на сервере.

**EN:** A terminal workspace for saved commands and notes, projects and files,
Linux environments, agent chats and server tools. Commands execute in the selected
Termux session, environment or server.

**[Скачать APK / Download APK — v2.1.4](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.4/Termux-Jobs-v2.1.4-arm64.apk)**
· [Установка / Installation](#installation-and-compatibility)
· [Релизы / Releases](https://github.com/HezeAI/termux-jobs/releases)
· [Безопасность и данные / Security and data](SECURITY.md)

**О проекте / About:** независимый неофициальный форк Termux, разработчик —
[HezeAI](https://github.com/HezeAI), ИИ-помощник при разработке — OpenAI Codex.
Проект не связан с командой Termux и не одобрен ею.
An independent unofficial fork maintained by HezeAI with development assistance
from OpenAI Codex; not affiliated with or endorsed by the Termux team.

## Возможности / Features

- **Команды и заметки / Commands and notes:** saved commands, aliases and launch presets.
- **Проекты и файлы / Projects and files:** files and project instructions alongside the terminal.
- **Linux-окружения / Linux environments:** terminal and X11 desktop workflows.
- **AI-чаты / Agent chats:** Codex, Claude Code, Gemini and OpenCode, with attachments and action approvals supported by each CLI.
- **Серверы / Servers:** SSH connections and commands in the selected remote environment.

## v2.1.4

Changes since the last published stable release, v2.1.3.

- Files and changes in all four agent chats: local attachment search, sender filters,
  explicit bounded loading, original-message navigation and whole-project Git diffs against HEAD.
- Refreshed file modification dates; readable cards and compact search/refresh controls.
- Save original chat images, including animated GIF, to Pictures/TermuxJobs without re-encoding.
- Native context estimates for Codex, Claude and Gemini; confirmed manual Codex compaction.
- One elapsed timer per user request in all four chats, including tools, approvals and retries.
- Notes-only star menus; exact updated Codex/Claude RU/EN common templates, explicitly applied.
- Late native history cursors remain discoverable when an early snapshot is empty.

[Release notes](docs/fork/RELEASE-v2.1.4.md) · [Validation](docs/fork/VALIDATION-v2.1.4.md)
· [CHANGELOG](CHANGELOG.md)
· [AI instructions ZIP](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.4/AI-INSTRUCTIONS.zip)

## Проверки и ограничения / Checks and limitations

**RU:** Для v2.1.4 опубликованы результаты автоматических проверок, проверки
подписанного APK и сценариев, принятых разработчиком на телефоне. Эти результаты
разделены в [отчёте выпуска](docs/fork/VALIDATION-v2.1.4.md).
Независимый аудит безопасности этой версии не подтверждён.

- Накопление вложений и связанные обрывы чатов остаются неисправленными.
- Низкоуровневые проверки передачи ключа и аварийного восстановления Gemini
  на Android, а также экспорт/восстановление копий на устройстве не выполнены.
  Точный непроверенный объём указан в [плане аудита](docs/fork/SECURITY-AUDIT-STAGE2.md).
- MCP и плагины исключены из проведённого этапа аудита.
- Рабочая папка, инструкции и подтверждения действий сами по себе не создают
  файловую песочницу. Агент может передавать доступные ему данные своему провайдеру.
- Архивы резервных копий не зашифрованы и могут содержать авторизацию,
  историю и снимки проектов. [Состав и ограничения копий](docs/fork/BACKUP-RESTORE.md).

**EN:** [Release validation](docs/fork/VALIDATION-v2.1.4.md) distinguishes automated
checks, signed-APK verification and developer-reported phone acceptance. No
independent security audit of this version is confirmed. Attachment accumulation
and disconnections remain unfixed. Low-level Android key handoff/crash and device
backup export/restore checks have not been performed; MCP/plugins are outside the audit.
A working directory, instructions and approvals alone do not provide filesystem
isolation. Agents can send accessible data to their providers. Backup archives are
unencrypted and can include credentials, history and project snapshots.

Source availability, test counts and a valid signature are not safety guarantees.
[Data flows, permission limits and reporting / Данные, права и сообщения о проблемах](SECURITY.md).

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

## Пользователям / For users

- [Usage](docs/fork/USAGE.md): terminals, presets, commands, files and agent controls.
- [Agent instructions](docs/fork/AGENT-INSTRUCTIONS.md): actual sources, import,
  precedence, revisions, conflict handling and application timing.
- [Agent skills](docs/fork/AGENT-SKILLS.md): scopes, imports, creation, managed updates and discovery.
- [Agent environments](docs/fork/AGENT-ENVIRONMENTS.md): shared Ubuntu AI preparation
  and retained legacy bindings. The legacy manual migration action is removed.
- [Environment updates](docs/fork/ENVIRONMENT-UPDATES.md) and
  [pinned initial environments](docs/fork/PINNED-ENVIRONMENTS.md): explicit upgrades,
  replacement and rollback. Installed distributions can still use their repositories.
- [Backups](docs/fork/BACKUP-RESTORE.md): selected data, unencrypted archives and restore limits.
- [Report a problem / Сообщить о проблеме](https://github.com/HezeAI/termux-jobs/issues):
  include the app version and steps to reproduce; do not attach credentials or private chat logs.

Instruction text, chats and presets belong to the user. Basic-settings backups have
limited coverage; OpenCode connection data is a separate optional component.

## Разработчикам / For developers

- [Contributing](CONTRIBUTING.md) and [architecture](docs/fork/ARCHITECTURE.md).
- [Build notes](FORK.md), [testing policy](docs/fork/TESTING.md) and [release procedure](docs/fork/RELEASING.md).
- [Release evidence](docs/fork/VALIDATION-v2.1.4.md) and [audit scope/results](docs/fork/SECURITY-AUDIT-STAGE2.md).
- [AGENTS.md](AGENTS.md): repository instructions for coding agents; not the user guide.

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


Private test14 adds grouped sidebar branches, uniform80% content sizing and caption-fitted panel width; attachment-menu items are reduced. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test14.md).

Previous private test13 removes voice input and its models/permission, restores the clockwise terminal-attention segment, adds desktop-stop feedback, shows current model/reasoning values and enlarges attachment-menu items. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test13.md).
