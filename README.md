# Termux Job’s

**RU: Программируйте с AI-агентами прямо на телефоне.**
Termux Job’s объединяет терминал, Linux-окружения и удобный интерфейс работы
с Codex, Claude Code, Gemini и OpenCode. Обсуждайте задачи, работайте с файлами
проекта, просматривайте изменения и запускайте сборки на Android. Сохраняйте
команды и заметки, управляйте сессиями и подключайтесь к серверам.

**EN: Code with AI agents directly on your phone.**
Termux Job’s brings together a terminal, Linux environments and a convenient
interface for Codex, Claude Code, Gemini and OpenCode. Discuss tasks, work with
project files, review changes and run builds on Android. Save commands and notes,
manage sessions and connect to servers.

**Создано на телефоне / Built on a phone:** этот форк создан и развивается
на Android-телефоне с помощью AI-агентов. Разработчик использует Termux Job’s
для работы над самим проектом.
This fork was created and continues to be developed on an Android phone with
AI assistance. Its developer uses Termux Job’s to work on the project itself.

**[Скачать APK / Download APK — v2.1.7](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.7/Termux-Jobs-v2.1.7-arm64.apk)**
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
- **Программирование с AI / AI-assisted coding:** Codex, Claude Code, Gemini and OpenCode; project files, attachments, action timelines and approvals supported by each CLI.
- **Серверы / Servers:** SSH connections and commands in the selected remote environment.

## v2.1.7 — RTK accounting and recovery fixes

**RU:** Исправлен учёт вывода RTK для поддерживаемых одиночных, последовательных
и фоновых вызовов после получения итогового результата. Вывод recall учитывается
отдельно; неподтверждённая база остаётся неизвестной. Также исправлены удаление
остаточного каталога Wayland в старом Arch и перезапуск чата при потере связи.
Бинарник RTK jobs.1 не изменился. После обновления откройте «Лимиты и контекст»,
чтобы обновить обработчик приложения.

**EN:** RTK output accounting now matches supported single, sequential and yielded
calls when their final result arrives. Recall output is tracked separately;
unconfirmed baselines remain unknown. This release also fixes removal of a
residual Wayland directory in old Arch environments and restarting disconnected
agent chats. The RTK jobs.1 binary is unchanged. Open **Limits and context** after
updating to refresh the application's observer.

Android 8+, ARM64; versionCode **1536**. No environment reinstall or data reset is
required. No rollback APK is included in this release. RTK figures describe command
output, not subscription quota or the entire context.

[Release notes](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.7) ·
[Validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.7/docs/fork/VALIDATION-v2.1.7.md) ·
[Current changelog](https://github.com/HezeAI/termux-jobs/blob/v2.1.7/CHANGELOG.md)

## Исторический выпуск / Historical release: v2.1.5 — Redesigned workspace

Historical changes in v2.1.5 compared with v2.1.4; not new v2.1.7 changes.

- New chat/terminal/sidebar/settings design, nested project and session actions.
- Centered creation forms, smooth page transitions and preserved drafts/reading position.
- Updated input rows and extra keys, compact action/file windows and attachment Download export.
- Original-image opening, action timelines, input history and saved-chat closing improvements.
- Claude Auto/Bypass preferences, availability checks and explicit next-launch behavior.

**Known issue:** image-heavy Codex cold replay can retry/disconnect. The failure was
reproduced without this UI; the remote closing component was not established.
[Upstream issue](https://github.com/openai/codex/issues/55730) ·
[Published HezeAI report](https://github.com/openai/codex/issues/55730#issuecomment-6645086819).
This release does not fix it. Auto's real-model classifier is unverified; Bypass is
unavailable under root in Ubuntu/PRoot.

**In v2.1.5, in-app updates installed the new interface, code 1520.** A separate manual old-interface
rollback APK (1521) is available in the release. Disable Auto/Bypass in every saved Claude
profile first. Device rollback/data retention is unverified. After rollback, returning
to the new interface requires a main APK above code 1521; current v2.1.7 has code 1536. Do not uninstall.

[Release notes](docs/fork/RELEASE-v2.1.5.md) · [Validation](docs/fork/VALIDATION-v2.1.5.md)
· [CHANGELOG](CHANGELOG.md)
· [AI instructions ZIP](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.5/AI-INSTRUCTIONS.zip)

## Проверки v2.1.5 и ограничения / v2.1.5 checks and limitations

Исторический отчёт v2.1.5 приведён ниже; результаты текущего релиза —
в [отчёте v2.1.7](https://github.com/HezeAI/termux-jobs/blob/v2.1.7/docs/fork/VALIDATION-v2.1.7.md).
The v2.1.5 checks below are historical; use the v2.1.7 report for current release validation.

**RU:** Для v2.1.5 опубликованы результаты автоматических проверок, проверки
подписанного APK и сценариев, принятых разработчиком на телефоне. Эти результаты
разделены в [отчёте выпуска](docs/fork/VALIDATION-v2.1.5.md).

- Накопление вложений и связанные обрывы чатов остаются неисправленными.
- Низкоуровневые проверки передачи ключа и аварийного восстановления Gemini
  на Android, а также экспорт/восстановление копий на устройстве не выполнены.
  Точный непроверенный объём указан в [плане аудита](docs/fork/SECURITY-AUDIT-STAGE2.md).
- MCP и плагины исключены из проведённого этапа аудита.
- Рабочая папка, инструкции и подтверждения действий сами по себе не создают
  файловую песочницу. Агент может передавать доступные ему данные своему провайдеру.
- Архивы резервных копий не зашифрованы и могут содержать авторизацию,
  историю и снимки проектов. [Состав и ограничения копий](docs/fork/BACKUP-RESTORE.md).

**EN:** [Release validation](docs/fork/VALIDATION-v2.1.5.md) distinguishes automated
checks, signed-APK verification and developer-reported phone acceptance. Attachment accumulation
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
- [Current release evidence](https://github.com/HezeAI/termux-jobs/blob/v2.1.7/docs/fork/VALIDATION-v2.1.7.md) and [audit scope/results](docs/fork/SECURITY-AUDIT-STAGE2.md).
- [AGENTS.md](AGENTS.md): repository instructions for coding agents; not the user guide.

## Лицензия и благодарности / License and acknowledgements

Developed by HezeAI. Thanks to the original Termux authors and contributors.
Based on [Termux](https://github.com/termux/termux-app) and the
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


## Исторические тестовые сборки / Historical test builds

These notes describe earlier private builds, not the current release.

Private test19 restores standalone image-view actions, corrects empty input history and makes the Stop square red. The reported Claude original-preview failure remains under investigation; diagnostic messages are more specific. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test19.md).

Private test18 removes the secondary toolbar input page, adds a tall vector Enter after PgUp/PgDn and renames input history. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test18.md).

Previous Private test17 centers terminal Notes, moves Enter before PgUp/PgDn, highlights active modifiers in yellow and refines general settings. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test17.md).

Previous Private test16 preserves chat insets across terminal transitions, refines typography/creation controls and adds Enter above PgUp. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test16.md).

Previous Private test15 keeps chat behind its sliding terminal, shares limit data, updates composer/menu sizes and centers agent creation. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test15.md).

Previous Private test14 adds grouped sidebar branches, uniform80% content sizing and caption-fitted panel width; attachment-menu items are reduced. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test14.md).

Previous private test13 removes voice input and its models/permission, restores the clockwise terminal-attention segment, adds desktop-stop feedback, shows current model/reasoning values and enlarges attachment-menu items. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test13.md).
