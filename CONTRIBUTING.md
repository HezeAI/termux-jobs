# Участие в Termux Job’s

## Пополнение справочника

Предлагайте новые карточки «Ошибки и решения» через Issues: точный симптом,
ОС и способ запуска, предварительная проверка, решение, ограничения и ссылка
на официальный источник либо подтверждённое обсуждение. Не прикладывайте
секреты и личные логи без очистки. Одинаковый симптом с другой причиной лучше
объединить с существующей карточкой. Нерешённые случаи отмечаем отдельно.

## Ошибки и предложения

Используйте [Issues этого форка](https://github.com/HezeAI/termux-jobs/issues).
Для ошибок загрузки графических компонентов укажите имя архива, безопасный
HTTP/код ошибки и была ли это первая установка или восстановление. Архивы и
их хеши перечислены в [отдельном репозитории компонентов](https://github.com/HezeAI/termux-jobs-components);
не прикладывайте авторизацию, личные пути или импортированный драйвер без проверки его лицензии.
Перед созданием записи проверьте, нет ли похожей. Укажите версию APK,
источник установки, Android, модель устройства, среду Termux/Ubuntu/SSH и агента Codex/OpenCode/Claude/Gemini,
шаги воспроизведения и ожидаемый результат. Для Linux укажите alias из настроек
и версию proot-distro; для пакетов — менеджер и вкладку, для бэкапа — полный/
выборочный режим и компонент, для удаления — используется ли копия другими сессиями. Для проблем разметки добавьте размер системного шрифта/масштаб экрана, состояние клавиатуры и режим окна (обычный, поворот, разделённый экран).

Для сценария запуска приложите команды без секретов и сообщите, остаётся ли
первый шаг интерактивным. Для ошибки обновления важно указать прежний источник
APK: совпадение названия приложения не означает совпадение подписи.

Для чата v2.0 укажите, воспроизводится ли проблема в чате приложения
или окне робота, после «Свернуть» или «Вернуться в чат», а также состояние клавиатуры.
Для выбора файлов: навигация/галочки/Выбрать всё, фильтр, исходные и целевая среды,
частичный результат (без личных путей и содержимого).

Для Codex укажите версию CLI и Node.js, выбранную среду и режим доступа, новый чат или продолжение по ID (сам ID можно скрыть), точный безопасный текст ошибки карточки и состояние терминального меню.

Для AI Agents укажите CLI/Node версии, способ входа без данных авторизации,
модель/режим, новый чат/выбор/ID (ID допустимо скрыть), папку и состояние
расширенного журнала. Отчёт: шестерёнка сессии → диагностика → Download.
Уточните, был ли debug включён до запуска; цитата ошибки не доказывает сбой.
Различайте чат приложения и терминальный интерфейс CLI; не смешивайте проблему отрисовки с ошибкой входа.

Для вложений чата укажите агента/версию CLI, тип и размер файла, источник
(память телефона/окружение), отправку с текстом или без, остался ли черновик и
появилась ли карточка. После обновления проверьте перезапуск с сохранённым чатом.
Не отправляйте личные исходные файлы для воспроизведения — используйте безопасный
тестовый пример. Для входа/ключа укажите способ и этап без кода входа/API-ключа.

Для X11 укажите окружение, рендер, включён ли стиль, вертикальный/горизонтальный
вид и состояние клавиатуры. Для MP4 — плеер, кодеки и установку «Видео и аудио»;
не прикладывайте личные видео. Отличайте чистую установку от обновления.

Не публикуйте auth.json, токены, приватные ключи, credentials и keystore.
Для проблем пакета сначала отделите их от поведения интерфейса форка;
исходники пакетов находятся в termux/termux-packages.

## Изменения кода

Рабочая ветка — `fork/base`. Границы локальной разработки описаны в
[AGENTS.md](AGENTS.md); архитектура — в [ARCHITECTURE](docs/fork/ARCHITECTURE.md).
Не включайте в PR ключ подписи, APK, локальный SDK, кеши и пользовательские данные.
Не меняйте package ID без отдельного плана совместимости bootstrap и пакетов.

В PR опишите проблему, итоговое поведение и выполненные проверки.
Добавляйте тесты для изменённой логики хранения или выполнения команд;
для документации достаточно проверки ссылок и соответствия реализации.
Сборка и проверки описаны в [FORK.md](FORK.md) и
[VALIDATION](docs/fork/VALIDATION.md).

## Подготовка релиза

Перед каждой публикацией выполните
[порядок подготовки релиза](docs/fork/RELEASING.md).
Документация обновляется вместе с версией, включая README и ограничения,
а не только название APK. Не выдавайте непроверенное поведение за проверенное.

Для ошибок перевода укажите язык интерфейса (System / RU / EN), системный язык и точную надпись. Переводятся только надписи приложения, не команды, пользовательские названия или протокольные маркеры Codex. Порядок дополнения переводов: [LOCALIZATION.md](docs/fork/LOCALIZATION.md).

Для ошибки менеджера пакетов укажите окружение, выбранный пакет/версию, действие,
цель запуска (выбранная или новая сессия) и безопасный текст ошибки APT.
Для редактора — тип/кодировку/размер файла, права родительской папки, наличие ссылок,
внешних изменений и среду. Содержимое личных файлов и секреты не прикладывайте.
Для категорий укажите команду или заметку, создание или редактирование и состояние клавиатуры.

Для бэкапов укажите распознанный формат, версии исходного/целевого Termux и
proot-distro, компоненты, этап и код ошибки. **Не прикладывайте полный бэкап**:
он может содержать авторизацию и приватные ключи. Для обновлений — установленный
versionCode, release tag, результат проверки/ошибку установщика без секретов.

Для выборочного бэкапа дополнительно укажите полную копию или «По выбору»,
группы, gzip/pigz/уровень и стадию. Не присылайте имена секретных файлов или их
содержимое, если это не нужно для воспроизведения. Для автопроверки — состояние
галочки «Авто», открытие/возврат/поворот и работает ли ручная проверка.

## Проблемы серверного раздела

Укажите версию приложения, ОС сервера/Python, способ входа без ключей/паролей,
действие (метрики/файлы/плеер/SFTP/задача/Cron/systemd), безопасный код ошибки,
проблему прокрутки/размер экрана. Для плеера — контейнер/кодек и размер файла;
для расписаний — backend, часовой пояс/CRON_TZ и состояние user-manager/linger.
Не публикуйте ссылки с медиатокеном, команды с секретами, приватные журналы,
профиль JSON/Keystore. TCP-проверка не доказывает авторизацию, наличие agent.py
не доказывает работоспособность агента. См. docs/fork/SERVERS.md.

## Current stable build configuration (v2.0.9)

Local AI and its runtimes/dependencies were removed, not just gated. No
TERMUX_PUBLIC_RELEASE flag or local-model preparation is required. Use the
isolated environment, SDK copy and ARM64 overrides from FORK.md and build scripts.
Stable versionName2.0.9/code1417; same release certificate is required for updates.
Historical protocols, source links and SDK/LoRA experiments are documented in
[LOCAL-AI-RESEARCH.md](docs/fork/LOCAL-AI-RESEARCH.md). Do not reintroduce model
weights, vendor SDK binaries, private prompts/chats or credentials in a PR.

For transfer issues provide app version, Download/Upload/Move, file sizes/count, destination environment, completion/cancel/error state, free space and a sanitized diagnostics report. A failed batch can contain completed files; never retry destructive operations blindly.

For action-card/input issues, specify the agent, native CLI version, history size,
streaming/idle state, composer focus versus text loss, keyboard and last safe
action status. Never include full private chats, credentials or skill secrets.

For cleanup issues include selected categories/paths without private names, busy-session state, scan/consent/cancel stage and partial-result summary. For package history/version selection include manager, available repository variants and confirmed command/result.

## X11 reports and upstream sources

For v2.0 X11 issues, specify phone/Android, distribution and version, renderer,
desktop startup versus program install, keyboard mode and clean/existing rootfs.
Export session diagnostics in X11 mode. Remove secrets, usernames and paths you
do not want public. Tested phones are RedMagic10Pro and SamsungS26Ultra; report
other devices explicitly. See [sources/credits](docs/fork/THIRD-PARTY.md) before
reusing third-party components. Public release descriptions default to English;
in-app update/compatibility notes remain RU/EN.


For Bionic-for-glibc reports, include the target/glibc version, GPU/Android API, renderer, exact driver ZIP name/SHA-256/source, Jobs GPU Test scene/size/VSync and whether the desktop or only a private probe was tested. Do not attach account files or private logs; see [known bridge limits](docs/fork/BIONIC_GLIBC.md).

For v2.0.8 chat reports, include agent/CLI, Markdown block type, streaming state,
Copy/expand behaviour, file type/size/count and source (environment/phone/gallery),
sort order and draft-versus-sent state. For Stop/activity issues include whether
the agent was working, which session was selected, screen refresh rate and whether
Server + Agent/robot indicators differ. Do not include private chat contents.

For v2.0.9 alias reports, include the selected environment, shell, operation and
sanitized error marker; never include alias bodies containing secrets. For chat
scale reports, include the agent, percentage, system font/display scale, streaming
state and whether the issue concerns messages, cards or input restoration.
