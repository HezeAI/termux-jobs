# Архитектура v1.0.4

## Модули

| Модуль | Назначение |
| --- | --- |
| `app` | Android UI, жизненный цикл, сервис, установка окружения |
| `terminal-view` | Отрисовка терминала, ввод, жесты |
| `terminal-emulator` | Эмулятор терминала, буфер экрана, PTY/JNI |
| `termux-shared` | Константы, настройки, общие утилиты |

Начальная точка: `app/src/main/java/com/termux/app/TermuxActivity.java`.
`TermuxService.java` управляет процессами, `TermuxInstaller.java` распаковывает
bootstrap. События терминала находятся в `app/src/main/java/com/termux/app/terminal/`.
Пути и идентификаторы определяет `TermuxConstants` из `termux-shared`.

## Рабочее пространство

Код: `app/src/main/java/com/termux/app/workspace/`.

- `WorkspaceController` строит экраны, формы, меню и вкладки, связывает профили
  с активными терминалами и обрабатывает вставку текста.
- `WorkspaceStore` хранит команды, заметки, профили и журнал: AtomicFile, последовательная
  запись в фоне и защита исходного файла при ошибке чтения.
- `SessionShell` формирует rc-файлы Bash, отдельный HISTFILE и последовательности
  запуска; экранирует пути и создаёт флаг отмены следующих шагов.

- `WorkspaceDialogs` и стиль `WorkspaceDialog` задают тёмные окна, меню действий и карточки выбора.
- `AdaptiveWorkspaceLayout` измеряет фиксированные элементы и выделяет каждой панели минимум 160dp с увеличением при системном fontScale > 1. На просторном экране списки прокручиваются независимо; при тесном окне доступна внешняя прокрутка.

`TermuxActivity` управляет жизненным циклом контроллера. События приходят
из `TermuxTerminalSessionClient`. Изменение title обновляет только заголовки
и вкладки вместо пересоздания страниц. Исходные эмулятор, PTY/JNI и foreground
service используются как основа; интеграция ввода и управления сессиями адаптирована.

## Данные и совместимость

Пути ниже относительно приватного каталога `files` Android-приложения:

| Данные | Расположение |
| --- | --- |
| Команды, заметки, профили, журнал | `workspace-v1.json` |
| История и rc-файлы профиля | `workspace-sessions/<UUID>/` |
| Termux HOME | `home/` |
| Termux PREFIX | `usr/` |

Имя JSON-файла историческое; формат текущего содержимого — v8.
Читаются v1–v8. Отсутствующее поле notes в v5–v8 считается повреждением; в старых форматах добавляется пустой список заметок. Записи без `saved` считаются сохранёнными для совместимости.
Новые обычные сессии имеют `saved=false`; сохранение явно меняет флаг.
Журнал ограничен 50 записями  и содержит текст и время отправки.
Повреждённый или неизвестный формат не перезаписывается.

Настройка интерактивного ввода хранится в SharedPreferences `workspace-input`,
вне каталога `files`. Поэтому копия только HOME не является полной резервной
копией пользовательского состояния приложения.

Снимки вывода больше не создаются и не читаются интерфейсом. Сохранённый профиль
не является сериализацией процесса. Откат на старую версию не гарантирует
возможность записи более нового формата библиотеки.

## Выполнение шагов

Termux запускается с подготовленным rc-файлом. Для Ubuntu `proot-distro login`
подключает каталог профиля в гостевую систему и запускает Bash с гостевым rc.
Готовность определяется первым вызовом Bash `PROMPT_COMMAND`, а не поиском
текста приглашения на экране или фиксированной задержкой.

В режимах Termux/Ubuntu шаги выполняются через `eval` в одной оболочке:
`cd` и `export` сохраняются между шагами. Проверяется код завершения;
файл `cancel-startup` отменяет оставшиеся шаги. При подготовке нового запуска
флаг удаляется. Интерактивный шаг должен завершиться перед следующим.
Режим `legacy` сохраняет прежнее поведение без проверки флага отмены.

## Сборка и границы

Основной пакет остаётся `com.termux`: пути bootstrap и пакетов зависят от PREFIX.
Простая смена applicationId не создаёт совместимую параллельную среду.
Отдельная экспериментальная Desktop Alpha не входит в основной релиз.

Подробности: [FORK.md](../../FORK.md), [проверки](VALIDATION.md),
[интерфейс](DESIGN.md). Ключ подписи, кеши, локальные инструменты и личные
данные не входят в исходники выпуска.

## SSH в локальной v0.2.7

`SshSessionShell` проверяет адрес/пользователя/порт, формирует один аргумент
удалённой команды с экранированием для локальной и удалённой оболочки. Локальный
`SessionShell` запускает единственный SSH-процесс после готовности Bash. Все
пользовательские команды исполняются в удалённой функции с проверкой кода
завершения; каталог разрешается относительно удалённого HOME. Секреты остаются
в существующей SSH-конфигурации; профили формата 6 содержат только параметры
подключения и переключатель интерактивной оболочки.

## Авторизация в локальной v0.2.8

`SshPasswordVault` хранит AES/GCM ключ через AndroidKeyStore; библиотека формата 7
содержит только ciphertext, режим авторизации и путь ключа. AAD связывает секрет
с ID профиля/host/user/port. `SshPasswordBroker` держит одноразовый секрет в памяти,
принимает запросы через случайный abstract Unix socket и проверяет UID peer.
Подтверждение host key требует ответа пользователя, неизвестные запросы отклоняются.
`SshAskpassMain` запускается отдельным `app_process` из того же APK и передаёт
ответ OpenSSH через stdout. Helper-скрипт имеет права 700 и не содержит секретов.
Передача пароля завершает broker, а таймер 120 секунд очищает незавершённый вход.
Автоматический/ключевой режимы не используют broker и сохраняют интерактивные
запросы OpenSSH в терминале. Keystore не переносится обычным архивом файлов.

## Вложенные списки в локальной v0.2.9

`WorkspacePanelScrollView` запрещает родителям перехват касаний в
`dispatchTouchEvent` при DOWN, до передачи события дочерним карточкам.
Обычный OnTouchListener панели не получает события, уже обработанные дочерней
карточкой, поэтому прежняя защита могла не сработать в прокручиваемой внешней
области. При UP/CANCEL запрет снимается. Nested scrolling для этих четырёх
панелей выключен: остаток свайпа у края списка не передаётся наружу.
`AdaptiveWorkspaceLayout` и его расчёт размеров не изменены.


## Codex-сессии (локальная v0.2.10)

WorkspaceStore v8 добавляет environment=codex и codexEnvironment/access/chat/thread.
CodexSessionShell готовит явный запуск Codex через отдельный app-server с Unix
WebSocket. Приватная директория .codex-control имеет 0700; имя сокета — отдельный UUID запуска, сохранённый в профиле для переподключения UI.
При явном закрытии/перезапуске проверяются UID peer и его cmdline перед SIGTERM
собственному app-server; чужие или неподтверждённые процессы не сигнализируются.
В Ubuntu она явно связывается --bind с /tmp/termux-jobs-codex. Токены в скрипты
не передаются; Codex пользуется своей авторизацией среды. Старые обычные сессии
не переподключаются и не изменяются.

CodexRpc: JSONL/stdio для просмотра истории (отдельный собственный процесс с
таймаутом), RFC6455 через Android LocalSocket для статуса активного сервера.
Проверяются WebSocket accept, Android UID peer, размеры кадров, таймауты. Сетевой
порт не открывается. Запросы initialize, account/rateLimits/read, thread/loaded/list,
thread/read не запускают model turn. Фоновый опрос только в терминале раз в 30 секунд;
страницы/списки приложения не пересоздаются из-за обновления статуса.

Внешний thread/resume не управляет настройками живого TUI надёжно в Codex 0.162.1;
он не используется для смены модели. Кнопки карточки подготавливают /model с защитой
черновика ввода, далее параметры выбирает штатный клиент Codex. Вывод терминала
не разбирается, неизвестные проценты не подставляются. Эти экспериментальные
возможности протокола требуют проверки на установленной пользовательской версии.


### Изменение транспорта в v0.2.11

Java запускает Node-адаптер codex-control.js через proot-distro login с тем же bind,
что у TUI. Адаптер проверяет WebSocket accept, размер сообщений, маскирует клиентские
кадры и обрабатывает фрагменты/ping. Android читает JSONL через обычные pipes,
поэтому подключение к Ubuntu-сокету не зависит от Android LocalSocket и пути/UID
через proot. Файл адаптера берётся из APK, хранится только в приватном каталоге.
Встроенный codex app-server proxy не использован: на отдельном сервере 0.162.1 он
не ответил на проверенный обмен.

При запуске сохранённого чата отдельный адаптер выполняет thread/resume с явными
правами и excludeTurns, проверяет sandbox/approvalPolicy и возвращает только ID.
Сервер в этот момент свежий; этот подготовительный вызов отличается от отклонённой
попытки менять модель уже работающего TUI. Новое управление моделью не добавлено.
Маркер остановки именуется UUID сокета; watcher в скрипте знает свой PID сервера.
Android не вычисляет и не сигнализирует PID через proot.

### Передача статуса в v0.2.13

Node-помощник запускается из собственного shell-скрипта Codex после готовности сервера. В этом же proot-контексте выполняются initialize, account/rateLimits/read, thread/loaded/list и thread/read с includeTurns=false. Только thread ID, доступные модель/effort/status и числовые окна лимитов записываются атомарно в UUID.sock.state.json в приватном .codex-control. Клиент Android читает ограниченный по размеру файл, проверяет схему и срок 90 секунд. Маркер UUID.sock.refresh запрашивает раннее обновление; обычный период 30 секунд. Trap останавливает только принадлежащий запуску PID помощника.

Android больше не использует File.exists Unix-сокета как признак активности и не запускает proot-distro для карточки. Код stdio RPC остаётся для списка истории. Созданные раньше терминалы не получают нового помощника без нового запуска. --no-daemon не считается гарантией отсутствия symlink: изолированная проверка обнаружила alias и с этим флагом.

### Совместимость CLI в v0.2.14

Клиент: codex --remote unix://... (без --no-daemon). Codex 0.162.1 запрещает сочетание этих двух флагов, что отдельно воспроизведено в PTY. --no-daemon остаётся у собственного сервера. Отказы RPC категоризируются по тексту в памяти, но сырой текст не выводится: наружу передаются только code и фиксированная безопасная категория. Код возобновления не выполняет fallback с расширением разрешений.

### Native picker adapter v0.2.15

statusWorker получает picker-visible каталог model/list один раз при старте (с пагинацией) и переносит только идентификаторы, названия и уровни усилий в приватный файл метаданных. Android читает его без повторного входа в Ubuntu.

CodexModelSelection — ограниченный автомат выбора через уже существующий TerminalSession. Проверяется idle, свободный native composer, DEC cursor visibility, распознанный заголовок меню, числовая строка с маркером выделения и название модели. Каждый шаг повторно читает текущий видимый экран; цифры и порядок каталога не используются как неподтверждённые индексы TUI. Arrow keys ищут нужный пункт, Enter открывает reasoning picker, s выполняет только session action. Max/Ultra используют More reasoning. Нет turn/start, RPC fallback с расширением разрешений, смены глобального config.toml приложением или записи /model в библиотеку/журнал команд. Стороннее дополнительное меню не подтверждается автоматически.

Только новая строка Model changed to после выбора подтверждает результат. Для текущего socket/thread сохраняется временный подтверждённый выбор, чтобы старые метаданные thread/read не отменяли отображение до следующего turn. Данные терминала читаются на UI thread, учитываются закрытие Activity, отмена и завершение процесса. Контроль ожидания 5 минут и автоматизации 15 секунд; ошибки предлагают ручное завершение. Применение модели подтверждено пользователем на его устройстве; совместимость других версий TUI не гарантируется.

### Ввод и диалоги v0.2.17

syncTerminalInput сравнивает mHandle TerminalSession. При смене сохраняет строку и границы выделения в памяти WorkspaceController и восстанавливает черновик нового handle. При доступном сервисе удаляются записи закрытых вкладок; отправка очищает поле и запись. Файлы/SharedPreferences для черновиков не используются. При смене поля перезапускается IME, если оно было в фокусе.

У звёздочки после show снимается FLAG_ALT_FOCUSABLE_IM, мешающий EditText в customTitle; tap/focus запрашивает клавиатуру, начальный фокус остаётся на заголовке. Списки моделей/поведения переходят далее нажатием строки; только Применить в отдельном подтверждении запускает guarded TUI adapter.

## Проекты/файлы и базовая оболочка v0.2.18

CodexProjects нормализует абсолютные пути и ~/ в выбранной среде, проверяет совпадение cwd и связывает только новый профиль с метаданными своего запуска. Асинхронный ответ проверяет socketId/handle до изменения профиля. В текущей v1.0 список thread/list включает все чаты выбранной среды; произвольный ID проверяется thread/read. Node preflight читает метаданные и требует подтверждённый origin для другого каталога (подробности ниже). bridge-v0.2.19 переносит только ID, cwd, короткое название, статус, параметры и лимиты — не текст диалога. Формат библиотеки v8 сохранён.

ProjectFiles работает отдельно от PTY пользователя: Bash в Termux, отдельный вход proot-distro в Ubuntu или SSH -T. Аргументы экранируются; список использует NUL-разделители и ограничения размера/числа записей. stderr не показывается как сырой вывод; ошибки SSH/доступа безопасные. Операции ограничены временем, закрытие браузера завершает только принадлежащий ему процесс. Для пароля повторно применяется same-UID одноразовый SshPasswordBroker; снимок профиля не меняется при редактировании исходного. WorkspaceFileBrowser ограничивает размеры предпросмотра/экспорта и разрешение изображений. FileProvider exported=false открывает только workspace-share внутри кеша, с временным URI-grant при явной передаче файла.

BASE_TERMINAL_ID — зарезервированный UUID, распознаётся по ID, а не имени. Профиль не попадает в список сохранённых. Создание происходит после bootstrap/подключения сервиса, без остановки других сессий; учитываются лимит 8 и wantsToStop. onFinished восстанавливает только его оболочку с проверкой актуального handle, задержкой и ограничением быстрых отказов. Удаление через Workspace и removeFinishedSession блокируется для базовой вкладки; выход из приложения не блокируется.

## Подтверждённый cwd и иконки v0.2.19

CodexChatCatalog запрашивает метаданные истории без cwd-фильтра сервера; поиск и дополнительный projectOnly-фильтр выполняются на клиенте. Выбор перечитывает thread/read без turns и требует подтверждения для разных каталогов. Поле codexSourceDirectory (необязательное в v8) фиксирует подтверждённый origin. После изменения папки в форме сохранение требует новой проверки; backend сопоставляет реальный metadata cwd с target либо с подтверждённым origin, затем проверяет runtime cwd и sandbox/approvalPolicy ответа thread/resume. Не подтверждённый/устаревший origin не разрешает произвольную смену каталога. Никаких turn/start или скрытых сообщений модели при подтверждении не отправляется.

WorkspaceIconButton наследует TextView для существующих обработчиков, но рисует одиночный drawable в центре viewport кнопки. Текстовые подписи остаются обычным TextView-рисованием; цвет распространяется на tint. Общий файловый браузер применяет те же иконки для навигации и явный selected-state для фильтров.

## v1.0: локализация

WorkspaceText сопоставляет только явные надписи приложения с устойчивыми Android string-ресурсами (default English / values-ru). Пользовательские данные не проходят через карту. WorkspaceLanguage хранит выбор в workspace-language/language, создаёт locale-context для TermuxActivity/SettingsActivity и применяет язык к текущим ресурсам без recreate/остановки сервиса. Locale.setDefault и окружение shell не меняются. Отображение перерисовывается, существующий EditText/карта черновиков сохраняются. Протокольные идентификаторы, англоязычные маркеры меню Codex и данные библиотеки не переводятся. Синтаксис генерируемых shell-команд проверяется для двух языков.

## Менеджер пакетов (v1.0.4)

WorkspacePackages получает копию удержанного профиля через sessionMenu, а не текущий
терминал. ProjectFiles.packageQuery выполняет ограниченное чтение (16 МиБ, 90 секунд)
в отдельном процессе окружения. PackageCatalog разбирает dpkg-query/apt list,
проверяет имена пакетов и строит команды из фиксированных операций.
UI создаёт максимум 100 строк за страницу; фильтр ищет по всему прочитанному каталогу.
Ответы предыдущей вкладки отбрасываются по номеру запроса. Закрытие окна завершает
только его собственный процесс чтения и очередь заданий.

Подтверждённая операция с выбранным назначением «новая сессия» создаёт новый независимый профиль Termux/Ubuntu с одним
startup, пустым handle и saved=false. Используется существующий путь launchNew /
SessionShell, без PTY.write в исходную сессию, без переноса Codex/SSH полномочий
или истории исходного профиля. Внешний процесс APT остаётся в видимом терминале;
состояние установленного пакета принадлежит окружению, не workspace-v1.json.
Автоматическое изменение репозиториев, установка зависимостей менеджера,
обход dpkg lock и привилегии Android не предусмотрены.

Для назначения «выбранная сессия» подтверждение содержит предупреждение о свободном
Bash; автоматической детекции prompt/программы нет. Проверяются running, прежний
handle удержанного терминала и неизменное окружение профиля. Затем приложение
переключается именно на него и отправляет единственную проверенную команду
посредством emulator.paste + Enter (или write до инициализации emulator).
Текст поля ввода и журнал последних команд не изменяются. Отмена ничего не отправляет.

## Версии пакетов (v1.0.4)

ProjectFiles.packageVersions выполняет отдельный ограниченный запрос APT madison
(1 МиБ, тайм-аут существующего транспорта). Установленный статус/версия читаются
dpkg-query; dpkg --compare-versions определяет флаг downgrade для каждой записи.
Для неустановленного/удалённого пакета сравнение не применяется. Pipefail сохраняет
ошибку APT, пустой список трактуется как отсутствие доступных версий.
PackageCatalog проверяет имя/версию, удаляет дубликаты, ограничивает число версий;
операция фиксирована `apt-get install --` с одним quoted аргументом name=version.
Version Entry хранит предупреждение о понижении; выполнению предшествует обычное
подтверждение с выбором цели. Отмена не устанавливает пакет.

## Описания пакетов и библиотеки (v1.0.4)

Описание запрашивается через существующий ProjectFiles transport (256 КиБ/тайм-аут)
в executor только при открытии карточки. dpkg ${Description} для installed либо
apt-cache show --no-all-versions name=version для available/updates. Парсер сохраняет
продолжения/параграфы Debian Description и не смешивает записи разных версий.
Ответ применим только к открытой карточке; закрытый менеджер игнорирует поздний ответ.
Отсутствие metadata показывает отдельное сообщение и не блокирует остальные действия.
Описание — обычный текст TextView, не исполняемый shell и не HTML/WebView.

openCommand/openNote используют существующие previewBody/previewAction/formScroll;
старые commandMenu/noteMenu/contextHeading удалены. Обработчики сохраняют проверку
writable, подтверждение удаления и write/save/render. Журнал/звёздочка не изменяются.
categoryField — AutoCompleteTextView с текущими дедуплицированными категориями
соответствующего списка. Введённый свободный текст хранится как раньше; схема v8 прежняя.

Локальная v1.0.4: categoryField дополнен отдельным picker из исходного списка категорий; он не использует отфильтрованный AutoComplete adapter. Выбор setText(value,false), отмена без изменения. Сохранение и writable checks прежние.

## Текстовый редактор v1.0.4

WorkspaceTextEditor хранит буфер только в памяти (EditText saveEnabled=false),
использует shared worker/ProjectFiles выбранного браузера. TextFileEdits считывает
snapshot SHA-256 + stat device/inode/uid/gid/mode/size/mtime/ctime/link count,
проверяет повторную revision и соответствие hash реальному содержимому, строгий
UTF-8, лимит 256 КиБ. Неудачный process/read не создаёт редактор пустого файла.

ProjectFiles.run добавляет bounded stdin payload: закрывает stdin после передачи,
чтения без payload получают EOF. Содержимое файла не становится частью shell script
или argv. Сохраняющий script потребляет stdin в mktemp, проверяет snapshot/ссылки,
создаёт второй mktemp и cp -p исходного файла, записывает новый текст и проверяет
uid/gid/mode, повторяет revision, затем mv -fT в том же каталоге. Trap удаляет только
собственные временные имена. Не снимает права и не останавливает посторонние процессы.
Безопасность stdin подтверждена тестом текста $(...) и quoted специальных путей.

Протокол SAVED/CONFLICT; ошибка/конфликт сохраняет пользовательский буфер. Атомарная
замена защищает от частичной записи, но revision-check не является атомарным CAS
для внешних writers: возможна гонка check/rename. Не применяется глобальная блокировка.
Отмена недоступна во время записи; lifecycle закрывает только собственный transport,
уже выполненная rename при разрыве подключения не откатывается автоматически.

## Локальное расширение AI Agents v1.0.5

AiAgents/AiAgentsUi и ai-agents-control.js изолируют подключения OpenCode по UUID.
WorkspaceStore сохраняет необязательные ai-поля в v8; environment=ubuntu для
OpenCode позволяет старому reader открыть обычную Ubuntu без скрытого запуска.
Codex остаётся environment=codex и использует прежний CodexSessionShell/Rpc.
Каждая вкладка OpenCode имеет собственные сервер и Unix socket, запросы ограничены
allowlist, ответы моделей очищены от secrets/settings. Подробные границы:
[AI-AGENTS](AI-AGENTS.md).

## Локальная v1.0.10: bounded output batching

WorkspaceController включает optional SynchronizedOutputBuffer только в OpenCode
профилях. TerminalSession не передаёт в emulator части DEC2026 frame до end/timeout;
normal sessions используют прежний путь. Native input/ANSI байты не фильтруются.
При resize объединяются несколько коротких выводов, с hard deadline и лимитом RAM.
Same PTY geometry/metrics skip применяется только opt-in, а native replay/модель/БД
OpenCode остались. Timer completion/reset/exit/disable рассмотрены в validation.

Packages открываются отдельным tile через актуальный профиль. Session stack первым
действием создаёт обычную сессию, Home + создаёт сохранённый пресет; format v8 прежний.


## Local v1.0.18 diagnostics

WorkspaceDiagnostics owns a structured in-process ring per workspace profile, keyed
by profile ID and current terminal handle, 2MiB/profile,32MiB global and64-profile cap.
Launch/exit/error classification is independent of dialog lifecycle. Both Activity
and service-only TerminalSessionClient callbacks observe output; source scans are
limited to100 rows,400 columns and once/2s. No output replays, PTY resize or writes,
stdin interception, environment dumps or process control are added by diagnostics.
Allowlisted categories and numeric HTTP codes are extracted; raw terminal text is
never appended. A terminal quote can produce a signal, so signals are not treated as
confirmed request failures. Extended mode adds activity heartbeat and native Gemini
--debug; it does not promise internal HTTP traces for every agent.

WorkspaceDiagnosticsUi refreshes only while visible, pauses collection independently
from process execution, and saves private0600 reports on the diagnostics IO worker.
Explicit Download exports have unique names and contain only the structured report.
The memory ring does not survive application process death; explicitly saved files do.
Session.extendedDebug is an optional v8 field; old profiles default false, copies
inherit preference, changing preference does not mutate an already running agent.

Shared settings gear on all tabs: language + existing SettingsActivity. Terminal-only
vector switches use selected-filter fill and stable48dp slots. Diagnostics lives in
long-press session context (base Terminal included). Startup cancellation uses the
existing per-profile marker, with explicit Yes/No and visible configured commands;
it never signals the current process. Only supported custom Termux/Ubuntu sequences
expose the vector in the active selector. It is omitted from selector popup rows and
hidden while the popup is open. Native SSH/legacy/auto-generated AI launch commands
are not presented as cancellable sequences.
