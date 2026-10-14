# Termux Job’s — выпуск v1.0.18

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
