# Выпуск v1.1.4 — улучшенный менеджер резервных копий

Пользователь одобрил v1.1.3 и явно разрешил публикацию. Добавлены: одна плашка
проверки обновлений, checkbox «Авто»/Auto справа с пояснением, одна попытка при
каждом foreground открытии/возврате, вместо10часов. Сбой без повтора до нового
открытия; disabled/busy opening тоже потребляет gate. Configuration change не
сбрасывает. Ручной check остаётся. Gate: WorkspaceUpdates.Opening, lifecycle
через WorkspaceController.onStop +TermuxActivity.onStop. Сессии/PTY не менялись.

Релиз com.termux/v1.1.4/code1061, прежний cert, libraryv8, manifestv1/v2 без resets.
311 JVM passed; backup helper тот же, что при60 Python passed в1.1.3.
APK 37851023bytes SHA256 ad05f1ea6e4bf5147a89a0eece568abb9be3bbd9495955e82c3657123d25bb0c.
Receipts .verification/release-v1.1.4; docs/fork/VALIDATION-v1.1.4.md.
Главный акцент README/релиза/update.json: улучшенный backup manager (тестирование).
Публичный1.1.0 понимает metadata schema1; до update имеет прежнюю10h схему.
Реальное обновление/новый UI не устанавливались агентом. Пакеты/APK не ставить,
Bridge/процессы не трогать. Design PNGs остались untracked, не включать случайно.

Опубликован stable Latest: https://github.com/HezeAI/termux-jobs/releases/tag/v1.1.4
Source tag commit 8b395f6a10840ac27c840fb9e2e52cc13d19b1ee; тег не передвигать.
Публичный latest API вернулv1.1.4; APK/SHA256SUMS/update.json скачаны обратно
без GitHub auth и совпали побайтно. Published proof:
.verification/release-v1.1.4/published/verification.json.
APK также /sdcard/Download/Termux-Jobs-v1.1.4-arm64.apk. Публикация завершена.
Проверка нового Auto UI/lifecycle и установки APK на Android остаётся пользователю.

---

# Локальная v1.1.3 — прогресс и сжатие бэкапа

Публичная версия всё ещё v1.1.0. Новая сборка сначала пользователю в Download;
GitHub только после проверки/новой авторизации. Не устанавливать APK, не менять
сессии/Bridge/рабочую среду. Реальная установка pigz агентом НЕ запускалась.

Добавлены WorkspaceBackupProgress, JSON byte/page/current progress, таймер,
защита всех backup dialogs от outside taps через scoped Builder. Явная отмена
сохранена, прочие меню приложения не меняются. gzip/pigz, уровни1/3/9 default3,
pigz максимум4 потока. Предложение install-pigz только после согласия, фиксированный
apt-get install -y pigz в worker; APT locks не обходятся. На время установки
создание бэкапа блокируется; UI показывает статус. Не отменять dpkg автоматически.
Архивы gzip совместимы; современный proot-distro backup --compress none stdout
сжимается выбранным методом, legacy native compression остаётся без обещания уровня.

Проверено: 309 JVM +60 Python, release успешен. APK 34381132bytes,
SHA256 57cf072dbc55e50c438868a15ac32ca6e7d9c63cfbed321ccee71ca96a521473, прежний cert. ZIP/package/version/ABI/secret-pattern scan выполнены.
Download: /sdcard/Download/Termux-Jobs-v1.1.3-arm64.apk.
update.json подготовлен локально, НЕ опубликован. Физическая проверка нужна.
Проверки/артефакт: docs/fork/VALIDATION-v1.1.3.md, receipts
.verification/backup-progress-v1.1.3. Изменения до v1.1.2 ниже — история.

---

# Историческая локальная v1.1.2 — исправления «По выбору»

Пользователь подтвердил SOURCE_UNREADABLE в1.1.1, полный бэкап проходит.
Просил: название «По выбору», Create неактивна до согласия в ОБОИХ режимах,
явный заголовок папки назначения, индикатор обновления. Всё реализовано.
Сначала APK в Download, GitHub ТОЛЬКО после проверки пользователем; публикации нет.

Inventory теперь collects issues, disabled неполные группы; копирование выбранного
источника строго abort на ошибке, без скрытых пропусков/изменения прав. .l2s excluded,
поддерживаемые backing files materialized regular с pinned O_NOFOLLOW descriptors;
выходы за разрешённое хранилище blocked. Точный путь/errno из старого скриншота
не установлен: не утверждать, что конкретно L2S вызвал этот SOURCE_UNREADABLE.
Старое callback-поведение доказано, regression с ошибкой каталога покрыта тестами.
Read-only directory scan из Ubuntu не воспроизвёл ошибку; реальная проверка в App
нужна. Полный native backup/restore не переделан; ничего в работающих средах не
восстанавливалось, Bridge и пользовательские процессы не трогались.

bindConsent wired полная форма +финальное подтверждение, тесты toggle/reopen.
UpdateProgress checking/download percent+bytes/verifying, отмена/hash/signer прежние.
1.1.2/code1059/com.termux/v8/прежний cert. 306 JVM+46 Python checks, сборка прошла.
APK34375592bytes SHA256e943e22e240ff3b616ae0fd63ffb50f7ff2868244fd9fe0e4b8f96da115ff2e8.
Receipts .verification/selective-v1.1.2; update.json подготовлен НЕ опубликован.
Проверки/ограничения docs/fork/VALIDATION-v1.1.2.md. Старые локальные APK не удалять.

---

# Историческая локальная v1.1.1 — выборочный бэкап

Пользователь одобрил концепт и реализацию; договорились СНАЧАЛА APK в Download,
ПОТОМ публикация на GitHub после проверки. Публикации этой задачи нет.
Публичная версия остаётся v1.1.0. Не устанавливать APK и не восстанавливать
пользовательскую среду самостоятельно; не останавливать сессии.

Код: WorkspaceBackupUi режим полной/выборочной копии, раскрываемые карточки
Termux/Ubuntu, группы/размеры/добавление папок HOME, подтверждение состава и
зависимостей. workspace-backup.py: selection-inventory, классификация относительных
путей, без следования symlinks; SQLite Codex online backup+quick_check; manifest2
для выбранных данных, чтение1/2. Ubuntu с системой — fresh native proot restore;
data-only/Termux — отдельное извлечение с ручной активацией, README-SELECTED.txt.
Inspect перепроверяет целевые среды/наличие Node.js/архитектуру, без запуска CLI.
Старые полные архивы/библиотекаv8 сохранены, очистка данных не требуется.

1.1.1/code1058, прежние com.termux/сертификат. Сборка прошла, 302 JVM+40 Python
checks. Подробнее docs/fork/VALIDATION-v1.1.1.md. Receipts и APK:
.verification/selective-v1.1.1. update.json подготовлен, НЕ опубликован.
Lint task завершён, но JNA на ARM64 выводит upstream Unsupported OS/arch;
не считать это полностью чистым lint-анализом. Новые формы/реальное восстановление
проверяет пользователь на втором телефоне. Не обещать автоматической активации
частичной .codex поверх существующего окружения.

APK37831238bytes; SHA2566f8f34abd90608f28a840a89714b162aa3b43b845387ca1a05f2592d0699c968.
Группа Codex read-only на телефоне:555файлов/1305607540байт, auth/config/history/
CLIpackage найдены; Node/Git/SSH найдены. Полный пересчёт из proot прекращён
из-за длительности только для собственного worker, не считать полную инвентаризацию
всех категорий проверенной. Данные пользователя не архивировались/не восстанавливались.

История предыдущей публикации ниже сохранена как историческая.

---

# Текущий выпуск: v1.1.0 — обновления и backup beta

Публикация: https://github.com/HezeAI/termux-jobs/releases/tag/v1.1.0
Тег исходников: fd580488f4ca27a01b35fa2b832cb5c153a78e82; draft=false, prerelease=false.
Git HTTPS получил403/SSHtimeout; GitHub Git Database API загрузил точное дерево
5eb1f00e83ffd1c97ec7905b7c802551e1818abb. Локальные commit/tag объекты согласованы
с API; отличие сериализации конечного newline проверено, дерево исходников не менялось.

Пользователь разрешил реализацию, сборку и публикацию на GitHub, хочет обновиться
через установленную1.0.19. В релизе главный акцент на бета бэкапов из шестерёнки
справа сверху; отдельно выделить in-app updater. Реальное restore/установку
агенту не запускать. Исходные среды, пакеты и процессы не менять.

Готовый код: auto check onResume10h + toggle/badge, ручная проверка сохраняется.
Backup engine Python -I -S; create/inspect/selective restore (HOME/PREFIX staging,
Ubuntu fresh native proot restore + isolated launch preset); libraries/aliases/
scripts, per-preset history, safe defaults/review before launch/recovery snapshot.
Package comparison Termux only, APT simulation then confirmed new terminal.
Полные ограничения docs/fork/BACKUP-RESTORE.md, UPDATES.md и validation.

Версия1.1.0/code1057/com.termux/v8, прежний signer. 302 JVM tests +24 Python tests
прошли, BUILD SUCCESSFUL. APK37806077bytes SHA25680030ff0e099391840761fad13fc745a52810025233adfb29623a3f38772654b.
Receipts .verification/backup-v1.1.0, update.json с compatibility text внутри
RU/EN changes для старого v1.0.19 клиента. Документация обновлена. Опубликован Latest, APK/SHA256SUMS/update.json скачаны
обратно и побайтно совпали; latest API подтвердил stable v1.1.0.
Для всех будущих релизов обязательны update.json/повышенный code и предупреждения
о несовместимости/потерях/очистке **до установки**. Ничего не очищать автоматически.

---

## Предыдущая локальная работа и история

# Текущая работа: v1.0.19 — локальный APK для проверки

Пользователь согласовал ручное обновление через общую шестерёнку, сначала APK в
Download, GitHub после его проверки/отдельного указания. Ничего пока не публиковать.
Добавлен WorkspaceUpdates, публичный latest+update.json, скачивание/проверки/Android
installer confirmation; подробности docs/fork/UPDATES.md. Общий пункт аккаунтов/API
OpenCode удалён; session/preset пути сохранены. Logger default OFF; прежний NORMAL
однократно переводится в OFF при запуске новой версии, дальнейший выбор сохраняется.
Журнал сессий независим. Код1056/1.0.19/com.termux, прежний сертификат, v8.
295 тестов прошли; BUILD SUCCESSFUL; финальный APK34321240bytes, SHA256 e2aaaa7a55d0f0dd9fc7a5f80b2df42579669a8c4378fa11e96f75594d593843.
Receipts .verification/updates-v1.0.19; manifest там же update.json для будущего
релиза. Новый генератор scripts/create-update-manifest.py; notes RU/EN в
UPDATE-NOTES-v1.0.19.json. Не создавать fake higher-version release для теста.
Нужна пользовательская проверка UI; полный in-app upgrade — на следующем релизе.
APK не устанавливался, исходная работающая среда/процессы не изменялись.

---

## Предыдущий опубликованный выпуск (история)

# Termux Job’s — выпуск v1.0.18

Опубликован как Latest: https://github.com/HezeAI/termux-jobs/releases/tag/v1.0.18
Тег исходников e5bc875c9a6b12533d3f335dd2e0ff20143d3078. GitHub API: draft=false,
prerelease=false. APK и SHA256SUMS скачаны обратно и совпали; публикация проверена.

Пользователь прямо запросил GitHub-релиз со всеми изменениями после v1.0.4.
Тег v1.0.18,1.0.18/code1055,com.termux,Android7+/ARM64, прежний сертификат,
библиотекаv8. APK36637078 bytes, SHA256:
30b11320d082f8a8f462daea8c635e53a3588351d9cac53e14bec185bdf1f54b.

Основное: Codex/OpenCode/Claude/Gemini в AI Agents (экспериментально), частные
компоненты Ubuntu, история/native режимы, пресеты, исправление фокуса клавиатуры.
Общая шестерёнка с языком/настройками, terminal-only векторы interactive/extra keys.
Диагностика по долгому тапу, расширенный checkbox в сессиях/пресетах, независимый
сбор/пауза/Download. Отмена следующих custom Termux/Ubuntu шагов — в плашке,
вектор/список/Да-Нет, не SSH/AI, текущая программа продолжает работать.

288 локальных тестов,release/lintVital,581 RU/EN строк и APK checks прошли.
Full device/OAuth/models/history matrix не проверена. OC-001 открыт; OpenCode
screen/resize эксперименты отменены; автоматическая история возвращена. Gemini
individual Google OAuth отключён поставщиком; API UI открыт, ответ не подтверждён.
Antigravity исследован, не встроен. Источники/границы: RELEASE/VALIDATION-v1.0.18.md,
AI-AGENTS.md,KNOWN-ISSUES.md. Пользовательские процессы/среда не менялись,
APK агентом не устанавливался. Публикация выполняется по текущему запросу;
итог загрузки проверяется отдельно через GitHub API и SHA скачанных assets.

Default metadata/ARM64 helper приведены к1.0.18; effective APK уже собирался с
явным override1.0.18, исполняемый код после проверенной сборки не менялся.
Не включать .tools/.verification/.signing/cache/личные данные в Git.

## Архив предыдущих этапов

Ниже исторические записи разработки. При расхождениях текущее состояние выше
и в документации v1.0.18 имеет приоритет.

# Termux Job’s v1.0.4

Пользователь одобрил APK и прямо запросил GitHub-релиз. Выпуск соответствует тегу
v1.0.4, versionName 1.0.4/versionCode 1041, com.termux, прежнему сертификату,
Android 7+/ARM64; формат библиотеки v8 не меняется. APK и SHA256SUMS в release assets.
README/CHANGELOG/FORK/USAGE/DESIGN/ARCHITECTURE/LOCALIZATION, release notes,
validation и инструкции участникам актуализированы.

Основное после v1.0: APT/dpkg менеджер Termux/Ubuntu по долгому тапу сессии,
версии/downgrade/description/Подробнее, подтверждение выбранной свободной Bash
либо новой сессии. Пакеты остаются в окружении, SSH inventory не реализован.
Файловый редактор после Preview: UTF-8<=256КБ, Markdown, Snapshot hash/stat,
права/владелец/temp/mv, конфликт и сохранение буфера при ошибке, dirty close.
Не поддержаны links; optimistic check/rename не CAS для внешних writers,
нет autosave/crash recovery буфера. ProjectFiles использует stdin payload.

Действия команд/заметок в openCommand/openNote, троеточия этих записей убраны.
categoryField предлагает подсказки и отдельную ▾ кнопку полного списка при new/edit,
новое значение вводится свободно. 254 tests, release/lintVital, localization472
и финальные APK checks прошли. Подробный протокол устройства не предоставлен;
не выдавать mock tests за реальную проверку IME/DPI/SSH/Android lifecycle.
Детали SHA-256/границ: docs/fork/VALIDATION-v1.0.4.md.

Рабочие пользовательские среды/сессии/проекты агентом не менялись. При следующем
релизе обновлять всю применимую документацию по docs/fork/RELEASING.md, исторические
отчёты сохранять. Последующие публикации/установки определяются текущей задачей,
в Git не добавлять secrets, локальные инструменты, Alpha и пользовательские данные.

CI после release tag: первый запуск упал на Java 11/SDK tools16 до тестов; отдельный workflow fix использует Java17 для SDK и Java11 для Gradle. Проверенный APK не пересобирается; cloud CI статус проверять отдельно.

## Текущая локальная работа: AI Agents v1.0.5

Пользователь разрешил первый этап Codex/OpenCode и резервную сборку с высоким кодом.
Новый APK 1.0.5/code1042; резерв исходников HEAD18e88e8 и APK1.0.4-rollback/code1100
в .tools/ai-agents/rollback. Не публиковать/не устанавливать без новой инструкции.
После использования резервного APK будущий update должен иметь code>1100.
Код/границы/неполные части: docs/fork/AI-AGENTS.md. Итоговые проверки фиксируются
в docs/fork/VALIDATION-v1.0.5.md. Собственные runtime-проверки без inference/личных
ключей, все их данные внутри .tools/ai-agents/runtime.

## Текущая локальная v1.0.6 / code1043

FreeTierError воспроизведён на старом адаптере, штатный CLI/исправленный адаптер
получают реальные ответы бесплатной Ling. Создание через native API без ручного ID,
приватный session-links alias, сохранение actual ID. Старые ID не переписываются.
Bootstrap при Create+Launch: свой Node22 при необходимости, свой OpenCode2.0.28,
SHA-256/flock/temp, без npm и замены системного Node. Установочная ветка helpers
APT условная, не выполнялась в рабочей среде при проверках. 261 tests, build и APK
checks прошли; APK в Download, устанавливает пользователь. Резервcode1100 сохранён.
Не публиковать без инструкции. См. docs/fork/VALIDATION-v1.0.6.md и AI-AGENTS.md.

## Текущая локальная v1.0.7 / code1044

Критическая жалоба IME только в Dialog окнах: найден глобальный ALT_FOCUSABLE_IM
от старого KeyboardUtils. Workspace.prepareKeyboardWindow + lifecycle/toggle
маршрутизация из TermuxTerminalViewClient, explicit field clicks, menu dismissal
перед показом. Preferences/Android IME настройки/работающие сессии не менялись.
263 теста, release/APK checks прошли, Download Termux-Jobs-v1.0.7-arm64.apk.
Код/данные агентов, assets/native/авторизация не менялись. Ручную проверку клавиатуры
на телефоне ждём от пользователя. GitHub публикация не выполнялась/не авторизована.
Подробности docs/fork/VALIDATION-v1.0.7.md. Резервcode1100 остаётся выше1044.

## Текущая локальная v1.0.8 / code1045

Исправлен только OpenCode popup stale model label: server readback, view/profile
обновление, generation guard. Добавлены aiAccess full/ask/read в v8, права при new/
resume, запрет смены у live profile. Native 30 permission decisions и bridge resume
проверены. Build/Plan описания и уровни RU/EN, default ask. «Основное подключение»,
«Аккаунты и API-ключи», без отрицательной кнопки в двух списках подключений;
system Back/outside и cancel секретов/полного доступа сохранены. 265 tests,
release/APK проверки прошли; Download Termux-Jobs-v1.0.8-arm64.apk. Codex без изменений.
GitHub не публиковался. Пользователь проверяет UI. docs/fork/VALIDATION-v1.0.8.md.

## Текущая локальная v1.0.9 / code1046

После проверки пользователя выявлен откат модели OpenCode при отправке: mini держал
свой прежний выбор. Теперь подтверждённая смена заменяет только mini frontend,
server/chat ID сохраняются, replay истории включён, active/inbox проверяются до и
после выхода mini. Неотправленный mini draft может потеряться: UI предупреждает.
Пресеты запуска / Launch presets, меню «Добавить в пресеты запуска»; формат v8 прежний.
Codex и мерцание не менялись. GitHub не публиковался. Проверки описаны
в docs/fork/VALIDATION-v1.0.9.md; финальный APK в Download.

## Текущая локальная v1.0.10 / code1047

Мерцание: optional bounded DEC2026 transport buffer only OpenCode; native replay retained,
resize output burst grouped (350ms earliest draw, 100ms quiet, 1200ms maximum). Frame timeout
1000ms, memory bounds, identical PTY size/metrics do not issue duplicate native resize.
Codex/default output immediate. Native frontend/model/history adapter identical v1.0.9.
Prototype no-replay/manual-history discarded after true emulator exposed startup overwrite.
Autoscroll kept per latest user instruction; TerminalView source unchanged.
UI packages now box tile instead of +, long tap packages removed; first stack row creates
new session, then open sessions. Titles Create preset (Home) / Create session (terminal).
No GitHub publishing or APK installing. Verify on phone: empty OpenCode/new stream,
keyboard resize, away/back session; docs/fork/VALIDATION-v1.0.10.md and Download APK.

## ВАЖНО: OC-001 не исправлен после проверки v1.0.10

Пользователь подтвердил: мерцание/рывки OpenCode остались. По его просьбе пока
оставить как есть, пометить важным для дальнейшего исправления. Причина не установлена;
native replay/DEC2026 — наблюдения кода, не доказанная полная причина. Новые APK,
откат буфера или изменения кода сейчас не делать. Подробности: docs/fork/KNOWN-ISSUES.md.

## Локальная v1.0.11 / code1048 — проверяем OC-001

Пользователь после read-only диагностики разрешил проверочный APK. Только OpenCode:
attachSession ожидает final layout/preDraw перед initial PTY size; resize batching
выключен, DEC2026 и native replay сохранены. Native adapter/model/history неизменны.
282 теста/build/APK checks прошли; Download Termux-Jobs-v1.0.11-arm64.apk.
Эффект на телефоне ещё неизвестен. Прыжок при IME и особенности выделения отдельно
не исправлялись; пользователь просит после проверки обсудить оформление OpenCode
карточки/меню настроек как у Codex. GitHub не публиковался, APK не устанавливался.
Подробнее docs/fork/VALIDATION-v1.0.11.md.

## Локальная v1.0.12 / code1049 — отделили startup history от resize replay

После v1.0.11 пользователь подтвердил улучшение, но повторный вывод истории при
IME/возвращении вTerminal (сверхHome/Commands идругихсессий) остался. Разрешил правку.
Из upstreamv2.0.28 commit026b106bc5d41b679ffc413264229c340871e4d3 создан отдельный
mini frontend jobs1 (Bun1.4.2). Optional MiniHost flag отключает replayOnResize,
startup '--replay' и модельное reopen сохранены. Только mini использует bundled
binary; официальныйserver/OpenCode/Codex/auth не заменялись. Bundle71MB/runtime178MB.
APK1.0.12/code1049 ~105MB вDownload.283 Java/Android +31 upstream tests, CLI typecheck,
root lint with official cachedNode, production native resume/model+6 no-replayresizes,
install/reuse/corrupt cases и artifactchecks прошли. Full monorepo typecheckнеclaimed.
Generated .bundle ignored inGit; patch+manifest вdocs/fork/patches; futureCI/release
needs runtime bundle generation/distribution. Docs OPENCODE-MINI.md,VALIDATION-v1.0.12.md.
APK не установлен/не опубликован. После обновления reopenOpenCode. AndroidIME/pixels
проверяет пользователь; auto-scroll/textselectionне изменены. Orientation/formatted
history may retain previous width (no native fullreplay). Окно настроеквстилеCodex
ещё не менялось, обсудить после проверки.

## Актуальная локальная v1.0.13 / code1050 — rollback OpenCode + manual bookmark

Пользователь выбрал основуv1.0.9, оставил текущие надписи/иконки/пресеты/nav.
Все последующие экранные эксперименты убраны: terminal sources восстановлены,
sync/resize/deferred code и tests архивированы в .verification/rollback-v1.0.13;
control asset точноv1.0.9, separatejobs1 неупаковывается/не используется.
OpenCode longtap пункт EXACT «Сохранить чат сессии». SavednativeID optional
aiSavedThread, save включаетprofile.savedдлядоступности; SavePreset тольконастройки.
Unremembered nextlaunchnew; explicitselectedchat one-time. Legacyauto-resume
не включаетсяавтоматически, IDs/native DB retained; usercan choose/pinoldchat.
Active live socketstatus, stale-target guards; no separatetemporaryserverforSaveChat.
Codex unchanged.269tests/nativeModel+history/reopen probe/checks passed.
APK Download Termux-Jobs-v1.0.13-arm64.apk~34MB,certsame,code1050.
DocsVALIDATION-v1.0.13.md; deviceverification pending. GitHubnotpublished,APKninstalled.
Currentmetadata rootv8 oldreadersignoreoptional field. BaselineOC-001 notrepairedclaimed.


## Локальная v1.0.14/code1051 — Claude Code

Claude-only control/bootstrap/scripts, optional `claudeAgent` boolean + model/mode/
chat/thread/socket fields in libraryv8. Empty aiAgent + ubuntu preserve old-reader
readability (old reader launches Bash, not Claude). Modes manual/acceptEdits/plan,
chat new/choose/id only. Choose invokes native terminal picker; no live card RPC.
Runtime official2.1.296 pinned SHA256 in bootstrap, private install on user launch.
No native Claude bundled, no account/model request by developer. stop captures
per-run UUID, own child only SIGTERM + bounded SIGKILL, no name/process-tree kill.
OpenCode/Codex adapters and screen code untouched from v1.0.13. Current card
shows At launch values; native commands handle changes. Same Ubuntu/project
files are shared, runtime separation does not create security isolation.
Artifacts/evidence .verification/claude-v1.0.14; device auth/resume checks pending.
GitHub publishing and APK install not authorized for this local test.


## Локальная v1.0.15/code1052 — Gemini, access descriptions, OC auto history

GeminiCli.java + gemini-{bootstrap.sh,control.js}, optional v8 fields geminiAgent/
model/mode/chat/thread/socket. Empty aiAgent + ubuntu for old-reader readability.
Official0.64.0 bundle package, private Node22, pinned SHA; no system npm installs.
Native default/auto_edit/plan; new/choose/id; choose uses -i /resume AFTER auth,
never bare --resume (which would silently load latest). New UUID each new launch.
Card At launch only; account auth/native conversation still for device user.
Own controller GEMINI_CLI_NO_RELAUNCH=1 + handles official restart199 and native
IPC auth-selected-type, bounded5quickrestarts; stop disables restart, child-only
SIGTERM/SIGKILL. Private probe reached Google-auth menu after actual trust restart.
No model request/login submitted; no user agents touched.
Descriptions all4agents RU/EN added to picker + selected detail; runtime permissions
of existing agents unchanged. OpenCode auto history RESTORED per latest instruction:
prepareLaunch no random reset, stable alias allows bridge to resume same DB chat;
explicit New/connection switch/duplicate gets fresh alias; old explicit resumes
retained. Removed manual save-chat action+handler; aiSavedThread remains for old
bookmark compatibility. No OC terminal/screen changes. 279tests passed, APK signed
same cert; receipt .verification/gemini-v1.0.15/artifact.json. Device login/chat pending.
GitHub not published, APK not installed. Download delivery v1.0.15.


## Локальная v1.0.16/code1053 — fixed Gemini sign-in method

Phone v15Google auth→restart→API prompt. Controller never sends selection keys.
Read-only /root/.gemini/settings.json selectedType api; no user config modified.
Clean native probes Google choice + normal/bracketed invalid code stay Google:
root cause NOT proven. v16 adds geminiAuth google/api/auto; new=google, decode
missing=auto to preserve old profiles. GeminiCli.command passes sixth auth arg.
Own per-run0600 auth overlay native selectedType+enforcedType blocks wrong method,
not keyboard interception. No original settings/token copy. Refuses pin when external
systemsettings/defaults env or /etc/gemini-cli exists; preserves policy discovery.
Auto untouched. Own stop deletes overlay; restart199 retains pinned method and
ignores conflicting auth-type IPC only in explicit mode. Tests fake2restarts and
native pinnedGoogle with oldAPI preference reach OAuth code prompt; no credentials
or account requests submitted (only synthetic invalid code earlier). Evidence in
.verification/gemini-auth-v1.0.16. Source/build/device checks distinct. No publish/install.


## Локальная v1.0.17/code1054 — private Gemini auth path

Screenshot proved native CLI rejects v16 overlay under insecure /tmp ancestor.
Moved overlay to HOME/.local/share/termux-jobs/gemini-cli/auth (dir0700/file0600),
socket stays unchanged; JOBS_GEMINI_AUTH_DIR permits isolated probe location.
No chmod /tmp, no user settings/auth or other agent paths changed.
Evidence: .verification/gemini-auth-v1.0.17; VALIDATION-v1.0.17.md.
Actual Google account OAuth remains user verification; no install/publish.


## Local v1.0.18/code1055 — session diagnostics (awaiting device check)

WorkspaceDiagnostics: private structured ring events, 2MiB/profile, 64 profile cap;
basic launch/finish/prepare failure and observed error categories; extended output
activity timeline + native Gemini --debug. No terminal transcript/input/raw env or
custom commands stored; only allowlisted codes/categories. Collector callback runs
before Activity visibility check; dialog visibility not required. Pause/resume is
collection-only, not process control; native debug verbosity survives pause.
WorkspaceDiagnosticsUi: auto refresh2s, private report path/copy, unique Download TXT.
Optional extendedDebug v8 field survives preset encode/decode/copy, missing=false.
Other agents unchanged runtime flags. Memory ring does not survive process death;
explicitly saved reports do. API success/Google eligibility not assumed.
Do not publish until user checks journal and confirms; future release needs full
README/changelog/release docs with AI Agents experimental status clearly stated.

Final v18 UI decisions: no gear on any page. Globe long-press opens existing app
settings; terminal-only vector interactive/extra-key toggles, selected fill same
as filters. Diagnostics moved to session long-press, including base Terminal.
Cancel-startup vector is inside current selector at right, only supported custom
Termux/Ubuntu steps (not auto-generated AI, SSH or legacy startup); hidden while
selector popup is open and absent in popup entries. Cancels future steps only.

Superseding final decision (user clarified): one COMMON gear on all tabs with
Language + existing app settings, standalone globe removed. Two terminal-only
vector toggles precede common gear; diagnostics stays in session long-press.
Cancel shows all configured steps, Yes/No, only writes cancellation marker after
Yes. Does not kill running command. Extended checkbox remains in both editors.
