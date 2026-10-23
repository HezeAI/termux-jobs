# v2.0 — embedded X11 and environment routing

The x11 Android module integrates the pinned upstream Termux:X11 native core.
Each supported local target has a private service/process/display; the original
TestUbuntu service remains only for legacy compatibility, not new-session choice.
No separate X11 application or new Linux kernel is installed. Surface lifecycle,
keyboard bridge and selected-session state stay separate from agent chat rendering.

DesktopProcess extracts current app helpers into target-local control paths.
Guardians own only their children; close/cancel/error paths must not discover
and kill arbitrary terminal/rootfs PIDs. Renderer setup is explicit: CPU, optional
per-target pinned Turnip+Zink, or per-desktop VirGL server/socket. Wrapper is
Termux-only. VirGL POSIX helper selects swrast-loader plus virpipe, disables
Kopper and clears conflicting paths; Turnip/wrapper clear VirGL flags on entry.
Alpine receives its private musl client; other guest Mesa ABIs remain separate.

Program transactions and catalog/resume state are target-scoped. Base installation
requests an explicit full runtime set; optional application fixes attach to the
application. Checks reject absent required executables. Agent rootfs routing is
centralized in AgentEnvironments; Ubuntu AI migration uses bounded source/target
locks, scan tokens, read-back hashes and staged publication. No general schema8
reset or silent agent-environment switch is introduced.

Personal connection launchers/configs remain outside APK/assets. The optional
absolute-path wrapper only changes the new agent process's environment/route.
See [agent environments](AGENT-ENVIRONMENTS.md), [X11](X11-INTEGRATION.md),
[renderers](X11-GPU.md), [sources](THIRD-PARTY.md) and
[validation](VALIDATION-v2.0.0.md). Earlier chapters are historical.

# 1.7.5-test: явный выбор следующего чата

Редактор запоминает явный выбор чата отдельно от фонового статуса. При сохранении
настроек существующего профиля сохраняется codexBindingBlockedRun — идентификатор
старого bridge, которому запрещено автоматически менять new→id. Это защищает и
от уже запущенного чтения статуса, и от следующих опросов прежнего процесса.
После нового запуска новый socket отличается: автоматическая привязка снова
разрешена, когда история materialized. Guard сохраняется вместе с библиотекой,
но не копируется в независимый профиль. Действующий процесс не перезапускается.
Схема8 сохраняется; старые библиотеки получают пустой guard.

# 1.7.5-test: history paging without changing chat opening

AgentChatUi ставит нажатие Earlier messages в очередь, если normal snapshot уже
читается; после его завершения запускается ровно один previous request. Во время
чтения кнопка показывает loading и отключена. Смена выбранной сессии/верхней
вкладки сбрасывает очередь и блокирует поздние callbacks прежним generation.
Ошибка older request оставляет текущий чат доступным и предлагает повтор.

Codex snapshot отличает live-tail от страницы history. Native turns/list возвращает
один turn; он может содержать больше100 item groups. Новая snapshot pagination
отдаёт local окна по100 groups, затем native nextCursor. Старые native pages
проецируются отдельно от ограниченного live-buffer; их не обрезает bound перед
отображением и они не меняют latest native frontier. Если bound уже выбросил
часть initial/live turn, специальный head cursor перечитывает canonical history.
Local cursor включает границы окна/next frontier, поэтому normal polling не
подменяет более полное историческое окно с тем же последним message id.

Window cache: не более8 окон, ориентир2MiB projection data; одно текущее большее
окно временно допускается, его native источник ограничен существующим32MiB
wire limit. Устаревший локальный cursor даёт historyReset и новый frontier,
а не вечную загрузку/разрыв чата. Полные body для деталей остаются memory-only
LRU до800/2MiB. Нативные истории/файлы авторизации не изменяются.

partialHistory=true сообщает Android, что knownMessages содержит частичный
каталог. Отсутствие старого id в latest tail не является удалением: ранее
подгруженные строки сохраняются. При prepend существующие строки не перемещаются
из-за повторно пришедших страниц. Scroll anchor сохраняется; если прежний чат
целиком помещался в viewport, показывается начало добавленной страницы.

AgentChatCache сохраняет прежние лимиты/ключи/немедленное восстановление view и
черновика. Новые flags exhausted/trimmed — только presentation metadata. Если
LRU действительно обрезал старые строки, при возврате pagination frontier
восстанавливается из свежего snapshot. Эти флаги не записываются в библиотеку.
Native session mismatch, permissions refresh, run identity и generation checks
остаются прежними. Изменение JS применяется новым процессом bridge после
перезапуска соответствующей сессии с её историей, не hot-patch работающей сессии.

Дополнительно воспроизведён отказ -32600 thread not loaded: пустой новый thread
доступен в создавшем его app-server, но другой app-server не читает его до
появления файла истории. После двух fixture turns другой сервер читает его.
Статус теперь передаёт materialized — только boolean существования непустого
обычного файла thread.path, без копирования пути или содержимого в статус.
CodexProjects.bind не меняет new→id при explicit materialized=false; отсутствие
поля у старого bridge сохраняет прежнюю совместимость. Это фильтр ещё не записанной
истории, не fsync/проверка целостности и не гарантия всех сценариев восстановления.

Диагностика thread not loaded/not materialized получает безопасную причину и
локализованную инструкцию выбрать New chat либо другой saved conversation.
Существующий неработающий id не заменяется автоматически, истории не удаляются.
Не изменяются auth/config/model/send/approval policies. Регистр cwd сохраняется;
lowercase используется только в поисковом фильтре каталога, не при запуске.

---

# 1.7.4-test: native whole-environment deletion / inline scan

cleanup-inventory c overview=true добавляет в приватный plan группы environment:
concrete alias, контейнер/rootfs dev+inode и оценка объёма из обзора. UI передаёт
лишь выбранные id/acknowledged. На execute заново выводятся доверенные modern
container roots, сверяются inode и supported-lock implementation, затем вызывается
штатный proot-distro remove alias без force/shell/самодельного rmtree. Native
exclusive lock берёт сам менеджер, чтобы не конфликтовать с собственной внешней
блокировкой. Нативный код удаления read-only инспектирован; работающие среды
при разработке не удаляются. Проверки пути/inode не являются изоляцией от
произвольных изменений другим процессом с тем же UID.

Пересекающийся выбор целой среды и её внутренних категорий блокируется до первого
удаления. Рабочие сессии проверяются в UI до подтверждения и перед worker, а
внешние CLI блокирует native lock. Если план включает whole environment, UI
не предлагает отмену native процесса, явно предупреждая об этом заранее.
Ошибки дают частичный результат без fallback; успешные aliases перечисляются
в removedEnvironments. Пресеты и внешние backup архивы сами не удаляются.

Начальный анализ и «Обновить» используют WorkspaceBackupScanHeader прямо в окне.
Во время scan тело/подтверждение отключены; обновляются spinner и одна строка,
не содержимое терминальных вкладок. Закрытие через Отмену отменяет owned worker;
поздние callbacks игнорируются. Повторное успешное чтение создаёт свежий план и
сбрасывает прежний выбор. Старый отдельный пункт restored-removal из cleanup UI
удалён: восстановленные aliases выбираются среди всех окружений.

---

# 1.7.3-test: component overview / selected HOME and library data

cleanup-inventory с overview=true возвращает read-only размеры/identity
HOME, PREFIX/систем и приватных workspace-sessions. Это оценка, не план удаления.
cleanup-prepare-home принимает конкретный известный scope и верхнеуровневые
имена/identity, повторно выводит корень из доверенных путей, строит snapshot
выбранных элементов и отдаёт приватный token. home-data и app-history сохраняют
корень, удаляют выбранные регулярные файлы/сами ссылки и пустые выбранные папки.
Системные scopes не принимаются в prepare/execute; связи и спецфайлы не
используются как путь к цели. Для HOME действуют прежние проверки занятости и
native блокировки; для app-history UI проверяет каждый ID перед подтверждением
и worker. Новые каталоги/файлы не удаляются вне снимка. /home гостя использует
ту же блокировку контейнера, что /root.

Коллекции библиотеки очищаются на UI thread по актуальному снимку коллекции,
по выбранным id; сохранение через существующую очередь WorkspaceStore/AtomicFile.
save(Runnable) сообщает успех после finishWrite; обычный save() сохраняет старое
поведение. Preset cleanup сохраняет базовый Terminal и несохранённые сессии,
не удаляет файлы проекта или нативные разговоры. Истории Codex используют прежний
браузер с дополнительным guarded режимом только при входе из очистки.

---

# 1.7.2-test: cleanup / package history

WorkspaceStorageCleanup использует общий WorkspaceBackupEngine BUSY и отдельный
workspace-cleanup.py; запускать cleanup-код из чужого архива нельзя. План с
группами/идентичностью хранится в приватном каталоге. В запросе удаления —
только токен плана, выбранные ids и acknowledged=true. Cache roots повторно
вычисляются, записи проверяются до unlink; no-follow fd traversal и штатные
блокировки препятствуют очистке занятой среды. Отмена завершает только свой
Python worker, не пользовательские процессы. Новый backup attempt получает
приватный ownership receipt, чтобы распознать будущую незавершённую копию.

WorkspacePackageHistory хранит baseline и последние300 событий под хешем
(scope, canonical root, dev, inode): пресеты не создают отдельные истории.
PackageOperationJournal оборачивает только явно подтверждённую пакетную команду
POSIX subshell, возвращает исходный exit code, пишет уникальный numeric receipt
с noclobber в уже существующий session bind. Нет PTY parsing/tee/traps/таймеров.
Результат переносится в приватную историю; повторный сбор дедуплицируется по id.
При закрытии сессии отсутствие результата отмечается unknown, не failure.
Runtime wrapper и intent не сериализуются/не копируются в пресеты; исходный startup
остаётся обычной пакетной командой. Пока свой result receipt не получен, baseline
не пересчитывается, чтобы не записать промежуточное состояние собственной установки.
Ручные изменения обнаруживаются по установленному списку; это не полный native audit.

# 1.7.1-test: отдельный адаптер версий пакетов

EnvironmentPackageVersions отвечает только за APK/DNF/Zypper: ограниченное
чтение cached native metadata, точное имя, версия/архитектура/источник,
дедупликация без потери разных версий. Zypper XML запрещает DOCTYPE;
DNF использует буквальные TAB и не ограничивает выбор последней версией;
APK policy исключает installed-only варианты и скрывает URI-авторизацию.
Команды строятся только из проверенных полей с shell quoting, без force/yes
и обхода подписей. ProjectFiles сохраняет native guest PATH, alias и isolation;
сами мутации проходят прежний подтверждаемый путь WorkspacePackages.
APT и неподдерживаемые в новом выборе XBPS/Pacman остаются прежними.

# Current architecture: v1.7.0

Ordinary proot sessions bind to explicit prootAlias/prootIsolated fields in schema8;
legacy startup remains fail-closed. Existing shared Ubuntu/AI startup unchanged.
Native package backends use guest-only PATH, strict output parsing and quoted
confirmed commands; an alias marker prevents accidental host-shell execution.

Backup protocol versions1/2 retained. Full restore repacks only rootfs to a fresh
jobs-*; regenerated PAX/sparse records, minimal generated OCI launch metadata and
private empty .l2s avoid carrying source host commands or stale paths. Architecture
includes regular BusyBox ELF; literal systemd hex escapes are never decoded.

Restored removal accepts exact jobs-hex aliases with local restore provenance
(legacy HOME result receipts or private per-attempt registry). Inventory records
dev:ino; removal rechecks it. Native proot exclusive nonblocking lock refuses
shared active session locks. No force/kill/manual rmtree fallback. Known lock
capability is required; unsupported native code fails closed. Successful removal
marks a tombstone to prevent old reports reauthorizing a reused alias, then UI
removes linked presets. Archives/reports/originals retained. New partial attempts
have provenance; old unconfirmed copies remain disabled. UI cannot cancel destructive
removal after confirmation. Cancellation/OS process death can leave a partial copy;
it is not treated as rollback or success. Receipt is retained for retry.

Receipt directory walks use O_NOFOLLOW descriptors. JSON records are bounded;
unknown aliases and symlink containers cannot become removal targets. Same-UID
malicious processes can already manipulate app data; this is not a sandbox boundary.
No change to agent transports, server tasks or user-session process stopping.

Historical v1.6.5 architecture follows.

# Current architecture: v1.6.5

Shared stable action cards use typed native events, preserving original detail
IDs and message/receipt/reasoning boundaries. Codex history uses one-turn pages
and a chat-only32MiB response limit; ordinary control clients retain2MiB. Streaming
bottom-follow uses scrollTo to avoid moving keyboard focus. OpenCode read-only
inventory matches its bootstrap's private PATH inside a subshell. No changes to
agent policy, process ownership, schema8, server store or backup formats.


AgentChatUi/AgentChatView render native agent histories/events independently from
TerminalView. Each backend retains its own private transport/auth/history:
Codex app-server, Gemini native CLI adapter, Claude SDK/native handoff, OpenCode
HTTP/SSE projection. No cross-agent shared protocol/state or prompt override.
Bounded per-run/thread caches and stable row IDs avoid wholesale UI replacement.
Robot dialogs reparent one terminal view and restore its parent/keys on dismissal;
Minimize is distinct from explicit return. Codex UI-child shutdown uses PID/start
identity and waits for active-turn completion; it does not stop the app-server.

AgentPreparation performs read-only bounded checks before explicit setup consent;
additional helpers live in private per-agent locations. Base Ubuntu dependencies
may be shared; existing tools/rootfs are not wholesale replaced. VertexConnection
stores nonsecret project/location and uses per-run request/response handshakes.
Native credentials remain with the CLI; no credentials are bundled in the APK.

ProjectFileTransfer uses the existing chosen-environment file transport, preflight,
private staging/no-clobber publication and explicit partial-failure results.
Selections are browser-scoped, survive same-profile navigation and are bounded;
server groups preserve source-parent deletion validation. schema8 is unchanged;
new persisted fields are optional with defaults. Server store/agent and backup
formats remain unchanged. Feedback relay is not implemented.

[Codex](CODEX-ANDROID-CHAT.md) · [Gemini](GEMINI-ANDROID-CHAT.md) ·
[Claude](CLAUDE-ANDROID-CHAT.md) · [OpenCode](OPENCODE-ANDROID-CHAT.md) ·
[Files](FILE-SELECTION.md).

## Prior v1.5.3 architecture (historical)

---

# Current architecture: v1.5.3

Local-AI engines/services/JNI/vendor payloads and UI were removed. RetiredModelData
cleans fixed app-private files/local-ai and cache/local-ai without following links,
plus local diagnostics preferences. Old local profiles are skipped. HOME/PREFIX,
external agent auth/history, other libraries and proot distributions are not deleted.
Historical source links and implementation boundaries: [research](LOCAL-AI-RESEARCH.md).

Server deletion uses a separate Python helper over existing pinned SSH with
batch preflight and descriptor-relative no-follow traversal; not a remote-agent
upgrade. Progress UI retains owned upload/download jobs and restores the same
window; no foreground-service or process-death resume guarantee is introduced.
Terminal activity and server border use independent views with bounded frame
callbacks. Existing terminal/emulator and external-agent assets are retained.

BackupInventoryCache stores bounded process-memory metadata estimates, never
Activity/process objects or file content. Manual Refresh bypasses it; archive
creation always performs fresh preflight/reads/validation. Refresh controls have
owned callbacks/cancellation; a destroyed dialog cannot reopen from late results.
Compression engine, worker count and level are validated independently.

## Historical implementation notes

The detailed intermediate-version notes below preserve development evidence;
references to removed local-AI functionality are historical.

---

## Historical private v1.5.27-test: inline inventory and memory cache

BackupInventoryCache stores bounded full/custom JSON estimates and custom folder
lists by app-files/PREFIX key, never an Activity/worker/file contents. Reads do not
extend age; reclassification preserves the original age; fresh scans renew it.
Restore/import clears estimates. No scheduled job/timer or persistent cache DB.
Existing Python aggregate-cache validation and fresh create/read checks remain.
A missing requested full environment now fails before output directory creation.

WorkspaceBackupScanHeader is a custom title: title-left, Refresh/spinner-right,
one bounded status line underneath. WorkspaceBackupUi uses owned Engine callbacks
inline, disables the form/create while scanning, preserves disabled component
states, and cancels only that scan on dismissal. Closed headers ignore callbacks.
After success only the backup dialog is rebuilt with selections/destination;
consent is deliberately not inherited. No terminal/header/server page rerenders.

Compression method and level are independent controls. Requests carry engine,
level and strictly validated threads (gzip1, pigz2/4); older requests default to
prior behavior. No dependency added. Metadata records threads; old readers ignore
this optional field. Native legacy proot compression remains explicitly separate.

## Historical private v1.5.26-test: bounded inventory aggregates

Full inventory uses iterative scandir and one no-follow entry.stat; regular file
sizes only, symlink targets excluded as before. Selective inventory reuses walker
metadata in selection_size. The compiled prefix owner table preserves longest
path/custom-folder tie/earliest-rule precedence and is built once per traversal.

Per-dialog JSON inventory may carry bounded directory aggregates keyed by parent
and baseline winning prefix. Only regular/backing-file counts are aggregated;
custom directory ownership can be recomputed from representative paths, preserving
more-specific built-in groups. Error paths remain separately classified. The
snapshot holds metadata only, uses root dev/inode and baseline rules checks,
expiry and global serialized-payload/row budgets, and is not a process-wide/background cache.
UI only passes it on add-folder reclassification; Refresh sizes requests a fresh
scan. It is never included in the fresh create request. Actual creation recomputes
plans/sizes and reopens files with existing checks; integrity checks are unchanged.

Scroll has its own default TerminalViewClient callback, distinct from navigation;
it shares preserve-active/ignore-idle redraw filtering with keyboard resizing.
No change to actual scroll, PTY, input, permissions or network poll frequency.

## Historical private v1.5.25-test: separate terminal resize signal

TerminalViewClient.onTerminalSizeChanged is distinct from viewport navigation.
Actual updateSize uses it; attach/doScroll still use onTerminalViewportChanged.
The app preserves already-active output on resize and accepts further output
immediately. For idle handles it clears stale output and suppresses redraw1600ms
only for that handle. Resize never refreshes the real output timestamp and does
not keep an otherwise finished session busy. All callbacks are paint-only signals;
PTY dimensions, input, cancellation, lifecycle and scroll behavior are unchanged.

## Historical private v1.5.24-test: viewport activity filtering

TerminalViewClient.onTerminalViewportChanged is a default no-op notification,
implemented by the app to clear output-only activity and ignore redraw for1600ms.
It runs before doScroll (including fling/mouse), session attach and actual resize;
workspace screen transitions also clear it. It never consumes input, changes
scroll position, sends PTY data or schedules a later synthetic output event.
Known Codex active state/local foreground-task signals remain separate.
The header activity component and its title callback are removed; original
other-session title Toast restored. Historical1523 implementation follows.

# Архитектура v1.2.1

## Historical private v1.5.23-test: isolated presentation updates

ServerTransferPresentation owns only main-thread window visibility/state, never
transport cancellation. Background hides the existing dialog; foreground restores
it. Worker/connection/stream ownership remains with existing upload/download
controllers. Throttled progress callbacks update only mapped fixed-height card
rows and one drawable flag; no WorkspaceController/Servers page render call.
Rows are GONE without an active matching transfer and appear above Open and in
the selected-server page. WorkspaceTransferBorder draws a CW PathMeasure segment
on the existing tile background. Android services/manifest permissions unchanged.

WorkspaceActivityLine reserves3dp across idle/active states and only invalidates
itself. Per-handle timestamps are bounded64; optional proc foreground-group reads
run off UI on the existing reader with one pending probe, visible activity only.
No process enumeration or new model RPC. SessionActivitySignal is best-effort:
remote SSH busy state and unknown interactive/AI states are not inferred from a
long-lived process alone. Existing native Codex active state is used only while
fresh. Background activity uses current in-memory terminal titles/output and a
reused26dp canvas slot; crossfade events are throttled6s, duplicates suppressed,
and stop after2.4s. Events/animation do not relayout the terminal or rebuild header.
No title/event content is added to diagnostics or persistent history.


## Historical private v1.5.23-test: confirmed server deletion

WorkspaceServers shares the existing per-profile/per-directory selection with
trash/download/move. A separate boolean preserves an empty delete-selection mode;
context changes clear it. Confirmation snapshots entries, checks the server and
current directory before dispatch, and submits one foreground SSH operation.
workspace-server-delete.py is passed as a fixed quoted Python program, with all
paths in JSON stdin. No agent update, shell-interpolated filename or system service.
The helper scans the complete batch before mutation, opens every ancestor with
O_DIRECTORY|O_NOFOLLOW and uses fd-relative lstat/unlink/rmdir. It refuses mount
crossings, changed object identities and special files, and caps depth/count/time.
It closes owned directory descriptors in finally blocks. Completed roots and
removed-node count plus an allowlisted error report partial completion; no retry
or rollback. Shared pinned SSH transport/timeout/disconnect still apply. A lost
reply cannot establish deletion outcome; UI requests a listing refresh.


## Historical private v1.5.22-test: retired local-model implementation

LocalAi classes, separate service/process, inference native libraries/AARs,
model catalog/instructions and all local session UI hooks are removed.
WorkspaceStore schema8 skips legacy localAi=true objects and ignores removed
local-only optional properties on other profiles. Shared keepHistory and CLI
permission fields are retained. WorkspaceChatHistories now catalogs Codex only.
RetiredModelData is a narrowly scoped asynchronous removal migration for the
fixed local-ai child of Context files/cache, with NOFOLLOW directory traversal;
it never resolves stored profile paths or deletes HOME/PREFIX/proot roots.
The isolated build stages recreate disposable src trees so removed native
payloads/assets cannot survive incrementally into the new APK.


## Дополнения v1.3.0

WorkspaceErrorLibrary валидирует встроенный schema1 JSON по запросу:114 записей,
ограничение512КБ, ID/RU/EN/HTTPS/metadata. WorkspaceErrorLibraryUi не читает PTY
и не делает сетевую диагностику. Project evidence и статусы не заменяют тесты.
WorkspaceLocalFilePicker переиспользует WorkspaceFileBrowser; WorkspaceStorageAccess
принадлежит Activity и проверяет фактический доступ. proot:<alias> — временный
профиль выбора, пользовательский формат сессий не меняется.
SshKeyFiles импортирует подтверждённую копию0700/0600; broker передаёт passphrase
только запросу выбранного ключа, шифротекст остаётся привязан к endpoint.
Upload читает отдельный поток через ProjectFiles в существующую очередь SFTP.
LocalDownloadFile записывает собственный .part в выбранной среде и публикует
после успеха прежнего SSH-download; отмена закрывает только принадлежащие
операции процессы. Серверный агент и нативные библиотеки не изменены.


## v1.2.1 — ускорение отправки и очередей сервера

SFTP отправляет до8блоков по одному SSH-соединению; изолированы очереди
мониторинга и поиска/диагностики. Разработчик подтвердил ускорение отправки
примерно4–5раз на своём подключении; результат зависит от сети и сервера.
Защиты файлов/отмены сохранены, данные без сброса, агент прежний.

[Изменения](RELEASE-v1.2.1.md) · [Проверки](VALIDATION-v1.2.1.md).


## Серверный workspace v1.2.0

WorkspaceServers координирует отдельный ServerProfiles AtomicFilev1, pinned
ServerConnection/OpenSSH RPC и стабильные ServerScrollView/MetricCards. Profile
не связан с WorkspaceStore/AI-сессиями; password ciphertext привязан к Android
Keystore. Необязательные favorites/favoriteFiles/actions читают старые профили.

workspace-server-agent.py — устанавливаемый с подтверждением агент, /proc metrics,
SQLite history, собственные Popen задачи, user-level service operations и
loopback media leases. Удаление/stop не используют сохранённый PID как право
на сигнал. ServerSftp/Uploads/Downloads владеют отдельными SSH-процессами,
поэтому отмена не закрывает активные терминалы или мониторинг.

workspace-server-schedules.py передаётся pinned SSH без изменения agent.py;
Cron/user-systemd и runner в отдельном приватном root. root/job flock защищают
собственные изменения/overlap; внешние crontab-редакторы общей блокировки не имеют.
workspace-server-search.py — read-only JSONstdin helper, bounded traversal имён,
не shell interpolation. Диагностика probe не запускает агента.

Media3 1.5.1 загружается при просмотре, системные кодеки; player close/onStop
освобождают собственный туннель. Network security допускает cleartext только
точному127.0.0.1, base deny. Серверный участок SSH, токен временный. Текстовый
редактор использует прежние revision-guard scripts через pinned SSH, без autosave.
Подробные границы/ограничения: [SERVERS](SERVERS.md), [SCHEDULES](SCHEDULES.md).


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

## Manual updater (introduced in local v1.0.19)

`WorkspaceUpdates` uses a single background worker per check/download, public
GitHub HTTPS endpoints and a narrowly scoped FileProvider cache path. No shell
commands or access to HOME/PREFIX/agent credentials. Package/version/signer checks
precede explicit Android installer confirmation. Metadata schema, cancellation,
logging migration and trust limits: [UPDATES](UPDATES.md).

## Backup beta and automatic checks v1.1.0

WorkspaceBackupUi owns explicit actions; WorkspaceBackupEngine runs only the
embedded workspace-backup.py via isolated Python (-I -S), not a user PTY. A single
owned worker communicates bounded JSON events, never copies arbitrary subprocess
stderr into reports, and cancellation targets only that worker/its own tool child.
Python uses streaming TAR validation, hashes and private archive copies; new
files use no-follow dirfd traversal. PREFIX is never replaced live; rootfs is
repacked under a fresh alias before native proot-distro restore. Foreign host
config scripts/manifests are not executed.

WorkspaceBackupImport validates library v1–8 before mutation, removes live handles
and unportable SSH ciphertext, generates IDs/sockets, marks imported presets for
review (optional v8 field restoreReview). Active/base sessions survive replacement.
Original JSON is retained privately, then AtomicFile writes are checked. Restoring
multiple components is not globally atomic; limitations in [BACKUP-RESTORE](BACKUP-RESTORE.md).

WorkspaceUpdates stores auto enabled/available code in app prefs. A process-local
Opening gate allows one attempt per foreground opening. onStop resets it unless
configuration changes; onResume consumes it even when disabled/busy. Failure does
not retry until reopening. The old persisted attempt timestamp is ignored. No
service/alarm is scheduled. The
icon draws a badge without altering navigation geometry. Terminal rendering and
agent controller assets remain unchanged.

## Выборочный бэкап v1.1.1

Python selection-inventory классифицирует относительные пути внутри HOME/PREFIX
Termux или rootfs Ubuntu. Более специфичный путь имеет приоритет; каждый файл
учитывается один раз. Пользовательские пути ограничены HOME, без обхода symlinks.
create пересобирает план по реальным корням, не доверяет путям из JSON UI.
Полный режим остаётся native backup, выборочный сериализует только выбранные
группы. Rootfs с системой регистрируется через прежний native restore; payload
без системы только извлекается. Manifest v2 отделяет этот формат от прежнего v1.
Codex SQLite online backup/quick_check выполняются в приватном job-каталоге,
без остановки чужих процессов. Каждая база согласована отдельно, не весь бэкап.
inspect проверяет descriptor против фактической структуры TAR и повторно ищет
окружения/Node.js на целевом устройстве. Ничего из архива не запускается.

### Исправления v1.1.2

Инвентаризация собирает ограниченные issues и отключает затронутые группы;
строгий обход копирования не подавляет ошибки. .l2s storage не обходится,
поддерживаемые внутренние backing-файлы сохраняются regular entries с pinned
O_NOFOLLOW-descriptors. Существующие native backup/restore не изменены.
Общий bindConsent используется обеими формами создания. UpdateProgress содержит
только состояние визуального индикатора; скачивание/доверие/установка остаются
в прежних worker/verify/offerInstall путях.

### v1.1.3: bounded progress and owned compression workers

Backup JSON progress carries stage/processed/total/current/unit. Filenames are
sanitized and bounded, not file contents or subprocess output. UI replaces one
line and updates elapsed time; unknown totals remain indeterminate. Scoped
WorkspaceDialogs.Builder.blockOutsideDismiss protects backup trees only.
Streaming tar writes pass through gzip(level1/3/9) or an owned pigz pipe, capped
at4 threads. Compression failure prevents completion. Modern native proot stdout
is compressed directly; legacy uses its supported --output path. Only owned
compression/proot child processes are terminated on worker cancellation.
Explicit install-pigz uses a fixed argv, requires UI consent, retains APT locking
and verifies the executable. Its UI does not expose cancellation: the worker waits
for APT rather than deliberately killing dpkg. Android process death is not prevented.

### v1.2.0 combined update tile

Title and Auto checkbox have separate click paths: manual check vs persisted
preference only. The inline hint describes the per-opening automatic behavior.
The gate does not change installer/hash/signer validation or terminal lifecycle.


## Диагностика v1.2.2

Общий DiagnosticsPanel для сессий и серверов: сводка/события, ограниченное кольцо, пауза и экспорт. Серверные события типизированы, без команд/путей/полезной нагрузки; получение отчёта не создаёт RPC. Состояние паузы и опроса вне вкладки сохраняется отдельно в профиле. [Подробности](DIAGNOSTICS.md).

## Локальная v1.3.1

Выбор в chooseInputCommand вызывает openInputEntry, а не insertIntoCurrent.
Только явная кнопка вставки использует прежний путь текущего черновика.
Preview переиспользует previewBody; операции библиотеки защищены isWritable,
удаление проходит через confirm. Отправка PTY/журнал команд не вызываются.
Asset остаётся schema1, пользовательские форматы не меняются.

### Star preview navigation (v1.3.2)

The preview receives a callback that re-shows the existing picker and restores
its ListView position. Tab/query state stays in that picker; no persistent data
or PTY calls are added. Positive Back and preview cancellation share this path.
Insert/edit/delete retain their existing separate actions. The catalog adds
three sourced static entries; no runtime internet search or automatic fixes.

### Runtime dependency gate (v1.3.3)

Android8.0/API26 minimum. Guava requests use the same JRE variant, including
Media3 transitive edges, because upstream FileUtils uses MoreFiles. preBuild
checks debug/release runtime jars; the APK gate inspects DEX definitions and
inventories unresolved types against compileSDK. Explicit optional-type review
notes are not proof of method/API/device compatibility. Data schemas and the
upstream FileUtils implementation are unchanged.

### Локальная v1.3.4

Closed package/file browsers notify their owners once and clear loaded catalogs. Owners close a snapshot of browser registrations. Library search updates existing result containers; diagnostics and Codex polling are unchanged.

Local AI preview uses `LocalAiUi` + atomic `LocalAiFiles` storage and a non-exported
`LocalAiService` in `:local_ai`, accessed via bounded Messenger IPC. Native handles
are owned/closed by one service worker; cancellation is signalled without touching
PTYs or external agents. This is process separation, not a separate UID sandbox.

### Local AI v1.4.1-test

Compact header controls, offline Markdown and collapsed reasoning; per-session
project tools and independent path-check toggle. Full access is automatic and
shell commands are not project-sandboxed. See [current scope](LOCAL-AI.md).

### Local AI v1.4.3-test

All local backends share action coordination, optional non-replaying Yes/No
approvals and a separate reasoning dialog. Full access stays automatic by default;
path checks remain independent. See [scope and limitations](LOCAL-AI.md).

### Local AI v1.4.4-test

Action mode, scope and confirmation are independent. Path checks ON restricts
file tools to the project and rejects all arbitrary command requests before
approval/execution. OFF needs a warning acceptance; external file access remains
subject to mode/Android permissions. No new isolation. See LOCAL-AI.md.


## Optional Qwen3.5 protocol adapter (v1.4.29-test)

Session.localProtocol defaults to json and crosses LOAD IPC into LocalAiEngine. Only catalog qwen35-2b/llama supports qwen-native. LocalAiQwenProtocol generates permission-specific schemas; LocalAiPrompts appends authoritative current settings after user instructions. Engine.renderMessages is used by both exact prompt counting and generation after context fitting. Recorded toolRequest history becomes native assistant blocks; recorded toolResult becomes grouped user tool_response blocks. Historical assistant examples are not inferred as actions.

The strict native parser translates one standalone block into the existing JSONObject tool request, preserving actual authorization, confirmation, environment dispatch and execution. Application-originated initial tools keep their internal JSON path. No dynamic Jinja interpreter or native library/model changes are introduced. Model adherence and hardware behavior require device checks.


## v1.4.30-test compute mode and validation evidence

Optional Session.localCompute defaults cpu and crosses Selection/WorkspaceStore/LOAD IPC. CPU native libraries/runners are retained unchanged. GPU guards catalog Qwen3.5 2B, SM8750 and ARM feature support, then starts separate Bionic OpenCL runner through the owned-process wrapper. It uses Android model paths and1024/512 batches; tool environment remains independent. Saved CPU location/variant/threads are not rewritten. GPU uses6 CPU assistance threads, not user-configurable GPU threads. CPU benchmark is rejected in GPU mode. NPU remains separate.

LocalAiToolErrors records original invalid text and exact parser reason, selects the schema for the detected available tool without executing it, and removes redundant original text from model feedback. Cards format cause/original/expected schema as readable text. Empty-write results explicitly state no deletion. Authorization still gates all requests.


## v1.5.0 SFTP batches and distribution gate

ServerDownloadPlan inventories bounded trees read-only via LSTAT/REALPATH/READDIR; links/special files skipped. ServerSftp implements bounded eight-request 32KiB downloads with response IDs, ordered writes and before/after metadata checks; short/invalid replies fail closed. WorkspaceServerDownloads serializes one owned batch, snapshots download profile, uses a six-hour download transport bound (existing SFTP upload default unchanged), exclusive local UUID destination and per-file temporary publish. Move uses v3 nonoverwriting RENAME with target/self/source checks, no cross-device copy fallback or automatic partial rollback. ServerDiagnostics adds FILE_MOVE; no command/path payload enters diagnostics. BuildConfig.LOCAL_AI_ENABLED is false for TERMUX_PUBLIC_RELEASE=1, guarded in new session/preset selection, existing launch/edit/menu and inference service. Private build retains local AI and adds optional Qualcomm runtime; public APK excludes QAIRT/dispatch runtime, and no user data migration/reset is performed.

Линия над терминалом и бегущая рамка передачи сервера обновляются примерно60 раз/с. Частота данных сервера не менялась.

У каждого индикатора не более одного ожидающего кадра. Скрытие/отключение
отменяет кадр; рамка сервера останавливается вне терминала и при сворачивании.
Это не останавливает передачу файлов и не меняет частоту сетевых запросов.

## Concrete local environments (unreleased 1.6.6-test)

Ordinary proot sessions retain storage environment `termux` for old-reader
compatibility, with optional `prootAlias` and `prootIsolated`. Execution and file
operations resolve the concrete alias; do not infer the system from its display
name or route these sessions into the host working directory. Original Ubuntu
and AI launch routes stay unchanged. `prootStartup` preserves the real commands,
while legacy startup fields contain a fail-closed unsupported-version guard.
An older app can read the library, but its resave drops new preset metadata.
Restore metadata maps source aliases to fresh jobs-* instances for ordinary
presets only; restored sessions retain --isolated and mandatory settings review.
See [environments](ENVIRONMENTS.md) for setup and rollback constraints.
