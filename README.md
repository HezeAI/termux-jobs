# Termux Job’s

Форк Termux для повседневной работы: команды и заметки под рукой, пресеты запуска,
проекты и файлы рядом с терминалом, внешние AI Agents и управление серверами.
Команды по-прежнему выполняются в настоящем Termux или выбранном окружении;
интерфейс помогает организовать работу и не требует писать свою оболочку скриптов.

## v2.0 — Termux-job's X11 AVAILABLE

**[Download v2.0.0](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.0)** —
integrated XFCE desktops, graphics renderers and desktop applications.

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

Android8+/ARM64, unchanged `com.termux` package and signer, versionCode1320.
No general reset from v1.7.5; existing agents keep their selected environment
until explicit migration/change. The old local-AI feature remains removed.
The Termux:X11 core is integrated; a separate X11 app is not needed.
Desktop packages and optional drivers are downloaded after consent.

[Release notes and compatibility](docs/fork/RELEASE-v2.0.0.md) ·
[Validation](docs/fork/VALIDATION-v2.0.0.md) ·
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

Скачайте **Termux-Jobs-v2.0.0-arm64.apk** из
[GitHub Releases](https://github.com/HezeAI/termux-jobs/releases/latest).
Требования: **Android8.0+/API26, ARM64**. Пакет **com.termux**.
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

**English:** Android8+/ARM64, com.termux, unchanged signer, code1320, schema8.
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
