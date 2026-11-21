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

**[Скачать APK / Download APK — v2.1.9](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.9/Termux-Jobs-v2.1.9-arm64.apk)**
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

## v2.1.9 — agent chats, CLI updates and project Git

**RU:** Компактная хронология чатов, действия субагентов Codex, Claude и
Antigravity и обновление CLI из бокового меню. Новые Gemini-чаты в Ubuntu AI
используют Antigravity.
При создании чата приложение подготавливает Git для выбранной папки проекта
и показывает изменения до первого коммита.

**EN:** Chats have a compact timeline with subagent details for Codex, Claude
and Antigravity. The sidebar manages CLI updates. New Gemini chats on Ubuntu AI
use Antigravity. Chat creation
prepares Git for the selected project folder and shows changes before the first
commit.

Android 8+, ARM64. Version code **1549**. No data reset or environment reinstall
is required by the updater. Finish important work before installing an app update.

[Release notes](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.9) ·
[Validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/docs/fork/VALIDATION-v2.1.9.md) ·
[Current changelog](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/CHANGELOG.md)

## Известные ограничения / Known limitations

- **Codex WebSocket:** большая история изображений может вызывать повторные подключения и обрывы. Компонент, закрывающий соединение, пока не установлен. Image-heavy histories can cause retries and disconnections; the closing component is not established. [Upstream issue](https://github.com/openai/codex/issues/55730) · [Диагностика / Diagnostic report](https://github.com/openai/codex/issues/55730#issuecomment-6645086819).
- **Claude:** Auto зависит от версии CLI и аккаунта; принятие параметра не подтверждает работу классификатора модели. Bypass недоступен, если Claude отклоняет root внутри Ubuntu/PRoot. Auto depends on CLI/account support; accepting the option does not validate the model classifier. Bypass is unavailable when Claude rejects root execution in Ubuntu/PRoot.
- **Данные и права / Data and permissions:** рабочая папка и подтверждения не создают файловую песочницу. Агенты могут передавать доступные данные своему провайдеру. A working directory and approvals do not create filesystem isolation. Agents can send accessible data to their providers. [Подробнее / Details](SECURITY.md).
- **Резервные копии / Backups:** архивы не зашифрованы и могут содержать авторизацию, историю и снимки проектов. Они не сохраняют работающие процессы. Archives are unencrypted and may contain credentials, history and project snapshots; running processes are not preserved. [Состав и ограничения / Coverage and limitations](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/docs/fork/BACKUP-RESTORE.md).

**Границы проверок:** полный запуск на чистом телефоне, работа всех провайдеров и режимов прав остаются непроверенными. Низкоуровневая передача ключей и аварийное восстановление Gemini на Android, экспорт/восстановление копий через интерфейс телефона остаются непроверенными; MCP и плагины не входили в соответствующий аудит. Это границы проверки, а не подтверждённые дефекты.

**Verification scope:** Clean-phone startup and live use of every provider and permission mode remain unverified. Low-level Android Gemini key handoff and crash recovery, and backup export/restore through the phone UI remain unverified; MCP/plugins were outside that audit. These are verification gaps, not confirmed defects.
[Проверки 2.1.9 / 2.1.9 validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/docs/fork/VALIDATION-v2.1.9.md) · [Границы аудита / Audit scope](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/docs/fork/SECURITY-AUDIT-STAGE2.md).

Version 2.1.9 removes the built-in RTK integration. Existing Ubuntu AI
installations keep their RTK files and earlier chat history. New agent launches
use their normal command environment and preserve user-configured hooks.

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

- [CHANGELOG](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/CHANGELOG.md) · [Releases](https://github.com/HezeAI/termux-jobs/releases): история изменений и опубликованные версии / change history and published versions.

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
- [Current release evidence](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/docs/fork/VALIDATION-v2.1.9.md) and [audit scope/results](docs/fork/SECURITY-AUDIT-STAGE2.md).
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
