# Termux Job’s

Форк Termux для повседневной работы: команды и заметки под рукой, пресеты запуска,
проекты и файлы рядом с терминалом, внешние AI Agents и управление серверами.
Команды по-прежнему выполняются в настоящем Termux или выбранном окружении;
интерфейс помогает организовать работу и не требует писать свою оболочку скриптов.

## v2.0.9 — Environment aliases & agent chat scale

Create persistent **Alias** for Termux, Ubuntu, Debian, Alpine, Arch Linux, Fedora,
openSUSE and Void. Organize commands and aliases with separate categories,
favourites and searches; use **Commands / Alias / Notes** in the star picker.
Import selected literal aliases from shell files or JSON/backups, preview them,
confirm replacements and export shell files or JSON. Ubuntu AI is excluded from
managed alias targets. New terminal shells load the definitions; restart the
persistent Termux tab after adding or removing its aliases.

All four agent chats — **Codex, Claude, Gemini and OpenCode** — gain a per-session
**50–150% chat scale**, default100%, with live changes and a100% reset. Messages,
Markdown, cards, thumbnails and input rows resize together. Lower values fit more
content on screen. Same package, signer, schema8 and Android requirements;
**no data reset or environment reinstallation**.

[Download v2.0.9](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.9) ·
[Release notes](docs/fork/RELEASE-v2.0.9.md) · [Validation](docs/fork/VALIDATION-v2.0.9.md) ·
[Alias guide](docs/fork/ALIASES.md) · [Chat scale](docs/fork/AGENT-CHAT-SCALE.md).

## Historical v2.0.8 — Developer Markdown & agent chat improvements

Working with AI agents through CLI directly in Termux on your phone is now even more convenient. Read formatted replies, attach project files and receive ready-to-use results in the chat.

All four agent chats gain native **Developer-style Markdown**: code cards with syntax colours, line numbers and an outlined Copy button, headings, lists, quotes and horizontally scrollable tables. Work with archives, 3MF/STL, EXE/BAT and other regular files through downloadable chat cards. Long-press selects several attachments; the system gallery adds photos/videos into the draft. The shared file browser remembers name/size/date sorting.

A scoped **Stop** hand button interrupts current agent work, with a display-synchronized activity line targeting 60 FPS. The keyboard hides after an acknowledged message send while failed sends and new drafts retain their input. Same com.termux/signer/schema8/ARM64 and Android requirements; **no data reset or environment reinstallation**. X11/native libraries and renderer payloads are unchanged. An existing agent process may need a normal session restart with its saved chat to load new attachment helpers.

[Download v2.0.8](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.8) · [Release notes](docs/fork/RELEASE-v2.0.8.md) · [Validation](docs/fork/VALIDATION-v2.0.8.md) · [Usage](docs/fork/USAGE.md).

## Historical v2.0.7 — Mesa 26.2.5 and on-demand X11 components

Turnip + Zink in the seven Linux environments and the Bionic-for-glibc Mesa frontend now use **Mesa 26.2.5**. The Kali theme, Alpine VirGL client and default **a6xx-a7xx-a8xx** driver are prepared on demand from [Termux Job’s Components](https://github.com/HezeAI/termux-jobs-components#readme), with fixed versions, sizes and SHA-256. Existing valid caches work offline; running desktops adopt new libraries after stop/start. Distribution packages continue using their native repositories.

Driver libraries have a protected built-in default and trash buttons for imported drivers. Theme preparation, cleanup of used drivers, live scale restoration and portrait viewport behavior are corrected. Same com.termux/signature/schema8/ARM64 and Android requirements; **no data reset or environment reinstall**.

[Download v2.0.7](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.7) · [Release notes](docs/fork/RELEASE-v2.0.7.md) · [Validation](docs/fork/VALIDATION-v2.0.7.md) · [Graphics components](docs/fork/GRAPHICS-COMPONENTS.md).

## Historical v2.0.6 — X11 scaling, renderer setup and chat files

Live interface-scale Apply now reaches the owned XFCE settings service in all seven PRoot environments; native Termux retains its working path. The keyboard no longer squeezes a manually sized X11 desktop. Agent chats accept files up to **5 GiB** and original images up to **100 MiB**, with bounded prepared images for model input.

Optional graphics archives now use the separate [Termux Job’s Components repository](https://github.com/HezeAI/termux-jobs-components), with fixed versions, SHA-256 and upstream credits. This fixes the missing Termux Vulkan Loader download affecting Turnip and Wrapper setup. Bionic for glibc downloads its unchanged runtime only when needed; valid existing installations continue working offline. Distro packages still come from their native repositories.

The Wayland/KDE experiment is discontinued and removed. All eight ordinary environments retain X11/XFCE; Arch Programs returns to the standard X11 catalogue. Same package/signer/schema8/ARM64, code1395; no user-data reset or rootfs migration. Previously installed experimental packages are not automatically deleted.

[Download v2.0.6](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.6) · [Release notes](docs/fork/RELEASE-v2.0.6.md) · [Validation](docs/fork/VALIDATION-v2.0.6.md) · [Graphics downloads](docs/fork/GRAPHICS-COMPONENTS.md).

## Historical v2.0.5 — X11 screen controls and input fixes

Per-environment X11 resolution, interface scale and safe margins, with live Apply
and one-click Auto setup. Disabling Auto setup prefills 1600×720, 75% and 5 dp.
Compact movable/resizable extra keys and stricter automatic-keyboard focus checks.
Wayland/KDE and its driver controls are gray and disabled while testing continues;
the source retains the private preview behind a build-time switch.
Same package/signer/schema8/ARM64; no valid-data reset or environment migration.
Public code1376 intentionally equals the developer's retained private test.10.

[Download v2.0.5](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.5) ·
[Release notes](docs/fork/RELEASE-v2.0.5.md) ·
[Validation](docs/fork/VALIDATION-v2.0.5.md) ·
[Screen settings](docs/fork/X11-SCREEN.md).

## v2.0.4 — Bionic drivers for glibc desktops

Optional **Bionic for glibc (libhybris + Jobs bridge)** in Ubuntu, Debian, Fedora,
Arch Linux, openSUSE and Void. Import matching Bionic Turnip or supported Qualcomm
proprietary ZIPs, retain multiple saved drivers, and run desktops with separate
caches/displays. Termux retains its AdrenoTools Wrapper; Alpine/musl is excluded.
Private Mesa26.1.6 Zink + pinned libhybris/WSI and our ICD avoid replacing installed
system/guest libraries. QCOM support is experimental; see the known891.11 stress
timeout and API36/A8xx requirement before testing it.

Includes movable/resizable X11 extra keys, optional Jobs GPU Test with reports
and shortcuts in eight systems, and an approximately8% smaller optional theme.
Same package/signer/schema8, code1366; no valid-data reset or rootfs migration.

[Download v2.0.4](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.4) ·
[Release notes](docs/fork/RELEASE-v2.0.4.md) ·
[Validation](docs/fork/VALIDATION-v2.0.4.md) ·
[Bridge source/build](docs/fork/BIONIC_GLIBC.md) ·
[Driver sources](docs/fork/DRIVER-SOURCES-v2.0.4.md).

## v2.0.3 — X11 landscape and stability fixes

Rotate a running X11 desktop to landscape for a full-screen view without app tabs
or terminal input. Rotate back or press Android Back to return the same desktop
to the app window. The Interactive input `|` button remains at the top right. Landscape sizing
keeps the desktop near physical resolution across all eight X11 targets.
Corrected localized-window configuration handling keeps rotation geometry current.
The input button highlights enabled input. Optional Kali-inspired Termux-job's
style is independently selectable in all eight X11 environments. Fedora Parole media setup now includes a private
H.264 decoder/plugin with runtime checks; restart Parole after preparing Video
and audio. The fullscreen input control is lower and translucent.

Includes the cumulative stability-audit fixes for agent chats, installer
cancellation, backups/cleanup, persistence, transfers, updates, Android file
providers, settings, images and notifications. Android 8+/ARM64; X11 Android 11+.
Same package/signer/schema8, code1352; no valid-data reset or rootfs migration.
Outside-HOME Android SAF links are intentionally refused; ordinary workspace and
authorized shell access remain available. Read the compatibility notes before
updating. [Release notes](docs/fork/RELEASE-v2.0.3.md) ·
[Validation](docs/fork/VALIDATION-v2.0.3.md) · [Usage](docs/fork/USAGE.md).

Download: [v2.0.3 release](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.3).

## Historical private stability-audit stages

Private `2.0.16-test` / code1345 completes the agreed source-audit sequence with
compatible five-module lint analysis, explicit X11 API guards, immutable
notification actions and locale-independent shared values. Thumbnail EXIF uses
AndroidX1.4.2; attachment saving is exclusive and image taps support accessibility.
Final full JVM confirmation: 1,209 cases passed. Modern lint: 0 errors/209 cataloged
warnings, with the compileSDK/tool-version and device limits recorded. Existing
valid data and prior fixes remain; stable is still v2.0.2.
[Lint audit](docs/fork/AUDIT-LINT-2026-10-24.md) ·
[Repeatable analysis](docs/fork/LINT.md) ·
[Locale and provider audit](docs/fork/AUDIT-PROVIDERS-2026-10-24.md) ·
[Settings lifecycle audit](docs/fork/AUDIT-SETTINGS-2026-10-24.md) ·
[Combined regression audit](docs/fork/AUDIT-REGRESSION-2026-10-24.md) ·
[File transfer recovery](docs/fork/AUDIT-FILE-RECOVERY-2026-10-24.md) ·
[File transport lifetime](docs/fork/AUDIT-FILE-TRANSPORT-2026-10-24.md) ·
[File manager confirmations/exports](docs/fork/AUDIT-FILE-MANAGER-2026-10-24.md) ·
[Environment/package installers](docs/fork/AUDIT-INSTALLERS-2026-10-23.md) ·
[Updates/external files](docs/fork/AUDIT-UPDATES-ENTRYPOINTS-2026-10-23.md) ·
[Persistence audit](docs/fork/AUDIT-PERSISTENCE-2026-10-23.md) ·
[Transfers/attachments](docs/fork/AUDIT-TRANSFERS-2026-10-23.md) ·
[Storage](docs/fork/AUDIT-STORAGE-2026-10-23.md) ·
[Environment/X11](docs/fork/AUDIT-ENVIRONMENTS-X11-2026-10-23.md) ·
[Agent chats](docs/fork/AUDIT-AGENT-CHATS-2026-10-23.md).

## v2.0.2 — Agent chat improvements

**[Download v2.0.2](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.2)**.
Send files straight to Codex, OpenCode, Claude and Gemini chats without typing their
paths on the phone. Work on projects in the conversation and receive supported local output
files/builds as cards for preview, saving or explicit APK installation. Adds local
photo/video thumbnails, compact previews, Codex first-login assistance and an
optional protected Gemini API-key field. Fixes OpenCode attachment admission.

Android 8+/ARM64, code1331, unchanged com.termux/signer/schema8. No reset or environment
migration. Restart agents with saved chats after updating. Gemini keys are device/
profile-bound; duplicate/import requires re-entry. Video uses sampled frames without
audio; model/CLI permissions still apply. HezeAI confirmed Codex image input and
OpenCode sending; other device/provider combinations need testing.
[Release notes](docs/fork/RELEASE-v2.0.2.md) ·
[Validation](docs/fork/VALIDATION-v2.0.2.md) · [Usage](docs/fork/USAGE.md).

## v2.0.1 — Fix Wrapper

**[Download v2.0.1](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.1)**.
Fixes GPU window overlap, backup preset/history restoration and renderer component
validation/repair. Android 8+/ARM64; same package/signature and schema8, code1325.
No data reset. Repeated backup imports create separate preset copies.
[Changes](docs/fork/RELEASE-v2.0.1.md) · [Validation](docs/fork/VALIDATION-v2.0.1.md).

## v2.0 — Termux-job's X11 AVAILABLE

Integrated XFCE desktops, graphics renderers and desktop applications.

- **X11 in the app:** Termux, Ubuntu, Debian, Alpine, Fedora, openSUSE, Void and
  Arch Linux ARM, with separate per-target desktop services and runtime paths.
- **Graphics:** CPU, optional Turnip + Zink and VirGL; experimental custom
  Bionic Turnip ZIP loading through Wrapper for Termux only.
- **Programs:** per-distribution recommended/installed/search lists, inline
  installation progress, Stop/Continue, removal and a cached catalog.
- **Input and appearance:** automatic/manual Android keyboard, pointer/window
  handling, audio, storage shortcuts and an optional Kali-inspired Termux theme.
- **Ubuntu AI:** separate default environment for new agents, optional verified
  migration from existing Ubuntu, environment-aware files/packages and checks.
- **App polish:** session gears, chat/focus/input fixes, installation diagnostics,
  optional agent connection command, italic app name and a compact v2.0 label.

**Tested on RedMagic 10 Pro and Samsung S26 Ultra. Other devices require testing.**
Coverage differs by device; this is not a claim that every renderer and clean
installation was tested on both. Mali/Dimensity compatibility is not confirmed.

Android 8+/ARM64, unchanged `com.termux` package and signer, versionCode1325.
No general reset from v1.7.5; existing agents keep their selected environment
until explicit migration/change. The old local-AI feature remains removed.
The Termux:X11 core is integrated; a separate X11 app is not needed.
Desktop packages and optional drivers are downloaded after consent.

[Release notes and compatibility](docs/fork/RELEASE-v2.0.1.md) ·
[Validation](docs/fork/VALIDATION-v2.0.1.md) ·
[Sources and acknowledgements](docs/fork/THIRD-PARTY.md) ·
[X11 guide](docs/fork/X11-INTEGRATION.md) · [Graphics](docs/fork/X11-GPU.md).

## Возможности

- Главная: недавние команды с поиском и пресеты запуска с ручным сохранением.
- Команды/заметки: отдельные прокручиваемые списки, общий поиск, избранное,
  категории с выбором существующей или созданием новой, просмотр и редактирование.
- Терминал: несколько сессий, рабочие каталоги/проекты, черновик ввода для каждой
  сессии, интерактивный ввод и дополнительные клавиши, тонкий индикатор активности.
- Пресеты: до3 команд запуска; для поддерживаемых сред ожидание готовности Bash.
  Пресет хранит настройки запуска и не является снимком работающего процесса.
- Внешние AI Agents: Codex, Claude Code, Gemini CLI, OpenCode; поддерживаемые
  варианты нового/выбранного чата/ID, пояснения прав и карточки возможностей.
  Чаты приложения показывают сообщения, рассуждения/действия и подтверждения;
  робот открывает терминал со стрелками/Enter и возвратом в чат. Проверка среды
  предлагает недостающие компоненты с подтверждением. Возможности зависят от
  CLI/backend; Claude и Vertex не прошли полный реальный цикл с аккаунтом. [Документация](docs/fork/AI-AGENTS.md).
- Истории чатов: отдельный просмотр/удаление поддерживаемых записей; закрытие
  несохранённой сессии предлагает выбор сохранения истории. История не сохраняет
  состояние Bash/память процесса; поддержка зависит от типа сессии.
- X11: встроенные XFCE-столы восьми систем, рендеры, программы и диагностика;
  [источники и лицензии](docs/fork/THIRD-PARTY.md).
- Окружения: Termux/Ubuntu/Ubuntu AI и отдельные proot-дистрибутивы; для каждого Linux
  экземпляра — свои пакеты, файлы и отдельное восстановление. [Подробнее](docs/fork/ENVIRONMENTS.md).
- Файловый менеджер: проекты и доступные окружения/память устройства,
  просмотр и защищённое редактирование текста, выбор SSH-ключей и файлов.
  Долгий тап/галочки, выбрать всё по фильтру, копировать/переместить в текущую
  папку без автоматического перезаписывания; выбор отделён от перехода.
- Пакеты: каталоги поддерживаемых менеджеров, история операций, выбор версий
  APT/APK/DNF/Zypper и действия с подтверждением
  в выбранной или новой сессии того же окружения. Пакеты остаются в окружении.
- Сервер + агент: SSH-профили, CPU/RAM/диск, проекты/задачи/службы, Cron/systemd,
  файлы, избранное и поиск, текстовый редактор, медиаплеер, диагностика.
  SFTP-очередь, выбор/рекурсивное скачивание, отправка, перемещение и удаление;
  активные передачи можно скрыть в фон приложения. Обычная SSH-сессия сохранена.
- Бэкапы: полные и по выбору, анализ совместимости чужих архивов,
  выбор компонентов, отдельное восстановление Ubuntu, сравнение пакетов,
  импорт распознанных библиотек/aliases/scripts с подтверждением.
  gzip/pigz, уровни сжатия, прогресс и проверка созданного архива.
- Очистка: обзор размеров, выборочное удаление с обязательным согласием;
  занятые среды защищены, PREFIX только для просмотра. [Ограничения](docs/fork/STORAGE-CLEANUP.md).
- Ошибки и решения: пополняемая библиотека с источниками; диагностика сессий
  и сервера с экспортом отчёта. Перед публикацией отчёта проверьте личные данные.
- Русский/английский интерфейс, ручное обновление из GitHub и отключаемая автопроверка.

## Установка и обновление

Скачайте **Termux-Jobs-v2.0.2-arm64.apk** из
[GitHub Releases](https://github.com/HezeAI/termux-jobs/releases/latest).
Требования: **Android8.0+/API26, ARM64**. Пакет **com.termux**.
Встроенный графический рабочий стол X11 требует **Android 11+/API30**;
минимум Android8 относится к приложению и терминалу, а не к графическому виду.
Для предыдущих APK Termux Job’s с тем же сертификатом доступно обновление
поверх, в том числе через **шестерёнку → Проверить обновление**.
Каждый стабильный выпуск содержит APK, SHA256SUMS и update.json.
APK из другой сборки Termux с другой подписью нельзя обновить поверх или
установить рядом с тем же package ID. Не удаляйте приложение без бэкапа данных.

**Совместимость v2.0:** общий сброс сv1.7.5 не требуется. Прежние пакет,
сертификат и schema8 сохранены; команды, заметки, пресеты, серверы, окружения,
авторизация и истории автоматически не очищаются. Code1320 выше публичной1.7.5
и проверочной1.7.104. Миграция в Ubuntu AI необязательна; исходная Ubuntu не
удаляется автоматически. Завершите важные задачи перед установкой: Android
может завершить процессы. Новые адаптеры/скрипты применяются при новом запуске
соответствующего агента/стола. Перед удалением окружений сделайте бэкап.

**При переходе с версий до v1.5.3:** сохраняется удаление приватных моделей, чатов,
инструкций/настроек и диагностики прежней локальной AI-функции; старые локальные
профили пропускаются. Сохраните нужное заранее. Внешние оригиналы моделей и
proot-дистрибутивы автоматически не удаляются. Другие данные не очищаются.
[Совместимость и ограничения](docs/fork/RELEASE-v2.0.0.md).

**English:** Android 8+/ARM64, com.termux, unchanged signer, code1320, schema8.
No general reset from1.7.5. Agent migration is optional and does not delete the
original Ubuntu. Existing data/auth/history remain. Save active work before
updating; Android may terminate running jobs. Upgrades from before1.5.3 retain
previously announced retired local-AI cleanup; external models/proot are not
automatically deleted. See the English release notes above.

## Почему форк, если можно сохранить shell-скрипты

Скрипты остаются полезны. Здесь к ним добавлены общие команды/заметки, пресеты,
проектные файлы и редактор, пакетный менеджер, переключение сессий и карточки
AI Agents, серверные операции и бэкапы в одном Android-интерфейсе.
Сам терминал и обычные команды остаются доступны.
Пресет не обещает восстановить завершённый процесс, а серверные передачи в фоне
не гарантируют продолжение после уничтожения приложения Android.

Локальные ИИ больше не входят в APK. Мы сохранили
[исследование адаптеров, Native и всей обвязки](docs/fork/LOCAL-AI-RESEARCH.md)
со ссылками на исходники до удаления: возможно, оно пригодится другим разработчикам.
Проверенная обученная LoRA не выпускалась.

## Документация и участие

[Использование](docs/fork/USAGE.md) · [Серверы](docs/fork/SERVERS.md) ·
[Бэкапы](docs/fork/BACKUP-RESTORE.md) · [Архитектура](docs/fork/ARCHITECTURE.md) ·
[Сборка](FORK.md) · [Изменения](CHANGELOG.md) · [Участие](CONTRIBUTING.md) ·
[Порядок релиза](docs/fork/RELEASING.md).

Разработчик форка — **HezeAI**; разработка ведётся при помощи Codex/GPT.
Идеи собраны из собственных задач, отзывов пользователей и разных открытых источников.
Форк основан на **Termux v0.118.3**, лицензия [GPLv3](LICENSE.md).
[Авторы и компоненты](docs/fork/ATTRIBUTION.md).
Это независимый проект, не официальный продукт Termux, OpenAI, Anthropic или Google.

Implementation notes: [experimental glibc desktop bridge (Ubuntu, Debian, Fedora, Arch, openSUSE and Void) and X11 extra keys](docs/fork/BIONIC_GLIBC.md), including the current-frame GPU synchronization and Present event-reader initialization correction, Qualcomm Android-buffer import investigation, and a roughly 8% smaller optional Kali-inspired desktop theme. See the v2.0.4 release limitations.

Desktop feature: [Jobs GPU Test](docs/fork/JOBS_GPU_TEST.md), an optional renderer diagnostic with desktop shortcuts for the eight X11 environments. Device evidence and remaining limits are recorded in v2.0.4 validation.
