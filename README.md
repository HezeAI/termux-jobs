# Termux Job’s

**RU:** Терминал с интерфейсом для повседневной работы: сохраняйте команды и
заметки, организуйте проекты и файлы, запускайте Linux-окружения и общайтесь
с AI-агентами. Команды выполняются в выбранном Termux, окружении или на сервере.

**EN:** A terminal workspace for saved commands and notes, projects and files,
Linux environments, agent chats and server tools. Commands execute in the selected
Termux session, environment or server.

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
- **AI-чаты / Agent chats:** Codex, Claude Code, Gemini and OpenCode, with attachments and action approvals supported by each CLI.
- **Серверы / Servers:** SSH connections and commands in the selected remote environment.

## Current release — 2.1.7 /1536

Fixes native RTK measurement matching for supported single, sequential and yielded
command calls. Unknown baselines remain unknown. Also fixes removal of a residual
Arch Wayland directory and recovery of disconnected agent chats through restart.
RTK jobs.1 is unchanged. No data reset or environment reinstall is required.
[Release notes](docs/fork/RELEASE-v2.1.7.md) · [Validation](docs/fork/VALIDATION-v2.1.7.md).


## Historical v2.1.5 — Redesigned workspace

Changes since the last published stable release, v2.1.4.

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

**In-app updates install the new interface, code 1520.** A separate manual old-interface
rollback APK (1521) is available in the release. Disable Auto/Bypass in every saved Claude
profile first. Device rollback/data retention is unverified. After rollback, returning
to the new interface requires a future main APK above code 1521; do not uninstall.

[Release notes](docs/fork/RELEASE-v2.1.5.md) · [Validation](docs/fork/VALIDATION-v2.1.5.md)
· [CHANGELOG](CHANGELOG.md)
· [AI instructions ZIP](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.5/AI-INSTRUCTIONS.zip)

## Проверки и ограничения / Checks and limitations

**RU:** Для актуального релиза опубликованы результаты автоматических проверок, проверки
подписанного APK и сценариев, принятых разработчиком на телефоне. Эти результаты
разделены в [отчёте выпуска](docs/fork/VALIDATION-v2.1.6.md).
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

**EN:** [Release validation](docs/fork/VALIDATION-v2.1.6.md) distinguishes automated
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
- [Release evidence](docs/fork/VALIDATION-v2.1.5.md) and [audit scope/results](docs/fork/SECURITY-AUDIT-STAGE2.md).
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


Private test19 restores standalone image-view actions, corrects empty input history and makes the Stop square red. The reported Claude original-preview failure remains under investigation; diagnostic messages are more specific. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test19.md).

Private test18 removes the secondary toolbar input page, adds a tall vector Enter after PgUp/PgDn and renames input history. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test18.md).

Previous Private test17 centers terminal Notes, moves Enter before PgUp/PgDn, highlights active modifiers in yellow and refines general settings. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test17.md).

Previous Private test16 preserves chat insets across terminal transitions, refines typography/creation controls and adds Enter above PgUp. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test16.md).

Previous Private test15 keeps chat behind its sliding terminal, shares limit data, updates composer/menu sizes and centers agent creation. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test15.md).

Previous Private test14 adds grouped sidebar branches, uniform80% content sizing and caption-fitted panel width; attachment-menu items are reduced. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test14.md).

Previous private test13 removes voice input and its models/permission, restores the clockwise terminal-attention segment, adds desktop-stop feedback, shows current model/reasoning values and enlarges attachment-menu items. [Checks and phone acceptance limits](docs/fork/VALIDATION-2.1.5-test13.md).

### Historical RTK test builds

The private 2.1.6-test2 build expands partial, per-chat RTK Mode in Ubuntu AI.
The 2.1.6-test3 UI calls this integration **RTK Lite** and the native integration
**RTK native** (**RTK штатный** in Russian). Saved mode identifiers do not change.
Lite filters verified literal command families for Claude and Codex; native mode
delegates to RTK OSS1.1.0. Neither mode estimates subscription savings. See [behavior and scope](docs/fork/RTK-MODE.md)
and [validation](docs/fork/VALIDATION-2.1.6-test2.md). This is not a new stable release.

Private test3/1524 adds stopped saved-chat files and cached limits/context, plus
RTK Lite/native selection before startup. Native mode uses agent-specific permission
warnings and keeps native output recovery. [Checks and limitations](docs/fork/VALIDATION-2.1.6-test3.md).

Private test4/1525 adds persistent sidebar cards, content-sized right user bubbles,
calmer arrivals and a manual-only chat scroll indicator.
[Verification and phone checklist](docs/fork/VALIDATION-2.1.6-test4.md).

Private test5/1526 aligns sidebar card fills with the menu background and preserves
arrival progress across timeline reordering and late image metadata.
Private test6/1527 adds versioned measured RTK output/recall counters, keeps legacy
estimates separate, and gently pulses only the gray Stop outline during work.
Unknown native baselines remain unknown. New result accounting needs the next chat
start; no automatic restart. [Checks and limitations](docs/fork/VALIDATION-2.1.6-test6.md).
Private test7/1528 makes RTK dialog buttons compact and centered, with multiline
labels and at least 48 dp touch height. Progress remains full width.
[Checks and limitations](docs/fork/VALIDATION-2.1.6-test7.md).
Private test8/1529 simplifies the RTK block to connection state and a selected-mode
estimated-token summary. Exact byte counters, recall and mode explanations remain
in collapsed Details. [Checks](docs/fork/VALIDATION-2.1.6-test8.md).
Private test9/1530 adds RTK OSS1.1.0 component revision jobs.1 with measured native
ls/Git/search/read output, versioned upgrade preservation and display-only legacy
cleanup. That historical APK embedded the candidate archive. Test10 and stable
2.1.6 instead download the published component; they contain no RTK archive.
[Validation and timings](docs/fork/VALIDATION-2.1.6-test9.md).

Private test10/1531 downloads RTK 1.1.0-jobs.1 from the published
[Termux Job’s Components release](https://github.com/HezeAI/termux-jobs-components/releases/tag/rtk-1.1.0-jobs.1).
It removes the test-only bundled archive. Existing modes, statistics and recall
remain; old upstream1.1.0 can be updated explicitly. The selected sidebar chat or
terminal uses the same black resource as the chat background for visual review.
[Checks and device verification](docs/fork/VALIDATION-2.1.6-test10.md).
