# v2.0.9 — aliases and chat presentation

Environment alias writes use the owned helper and explicit destination whitelist;
transfer parses literal data, previews selected records and confirms replacements.
Per-session chatScale changes native View geometry without restarting agents or
changing request snapshots. See [aliases](ALIASES.md) and
[chat scale](AGENT-CHAT-SCALE.md) for persistence, trust boundaries and limitations.

# v2.0.8 — agent chat workflow

This release builds on v2.0.7. Native X11 inputs, desktop/environment launchers
and renderer payloads retain the established implementation.

All four agent transcripts share native Markwon/CommonMark rendering. Markdown
is presentation only: persisted raw messages and provider protocols remain intact.
Top-level code and tables use reusable block views; unchanged preceding blocks
are retained during streaming. Link activation permits explicit HTTP(S) navigation,
without WebView, remote-image fetching or command execution.

Output cards project bounded metadata for explicitly linked local regular files.
Opening/saving uses the existing per-environment file transport; generic files do
not trigger APK hashing, installation or archive extraction. Gallery selection
uses Activity-owned URI callbacks and private bounded copies; original provider
files remain unchanged. Preparation and drafts retain the owning chat run, while
late callbacks cannot attach to a newly selected run. Shared file sorting is a
presentation preference and does not rewrite paths or mutate listing metadata.

The Stop control sends the existing chat-interrupt operation to the selected
native session. It does not terminate the server or neighbouring agents. Its
activity animation paints into a temporary parent overlay and retains the old
one-pixel layout height. A single Choreographer callback drives a 60 Hz target;
geometry is cached without per-frame strings and draw does not schedule another
redraw itself. Only the scheduled frame invalidates the line/overlay. Idle, hidden and detached states release that overlay.
The accepted-send callback clears/hides the composer only when the same terminal
and unchanged submitted draft are still selected; failed requests keep the draft.

---

# Current graphics architecture — v2.0.7

Optional archives are fixed-version downloads from graphics-20261025, mesa-26.2.5 and x11-extras-20261025. A content-addressed verified archive cache supports reuse; each target retains its own activated runtime, driver selection, operation lock and X11 display. The bridge validates bundle identity and allowlisted file hashes before using the selected target's runtime. Changed bundles activate by staged replacement only under that target's exclusive lock; active desktops retain shared locks. No guest/system library is replaced.

The optional helper accepts Android's /data/data and /data/user/0 private-directory aliases by filesystem identity while retaining exact PREFIX and per-target checks. Driver removal keeps descriptor-based no-follow deletion, protected defaults and rollback of selected metadata on failure. Package-manager dependencies remain separate from mirrored renderer archives. Existing desktop process ownership, shutdown, permissions and authentication paths are preserved. See [graphics components](GRAPHICS-COMPONENTS.md).

---

# Historical architecture — v2.0.6

The Wayland/KDE experiment is removed. DesktopProfiles accepts XFCE; old Plasma selection is ignored and unsupported profile requests are rejected before runtime preparation. Arch uses the standard X11 program flow. Shared Wayland/Qt package dependencies remain independent of that retired desktop.

Each PRoot desktop generates a mode0600 private D-Bus configuration including its distro session policy and allowing only the actual Android application UID. The existing launch identity, display and control directory isolate settings operations. EXTERNAL authentication remains enabled; no anonymous/TCP bus or installed system-policy edit is introduced. This allows a settings client in a separate PRoot invocation to reach its owned Xfconf daemon. Native Termux uses its unchanged launch path.

Pinned graphics archives use the separate component repository. Bionic-for-glibc preparation copies only Python helpers into a private job directory, verifies a bounded HTTPS archive, atomically caches it and activates a staged allowlisted runtime under that target’s existing exclusive lock. A valid runtime is reused without network; failed/cancelled preparation preserves the previous runtime and driver selection. Active desktops retain their shared locks. Android JNI and guest runtime payloads are unchanged.

The X11 host preserves geometry when the IME opens. Chat file transport is streamed and size bounded. See [graphics distribution](GRAPHICS-COMPONENTS.md), [screen design](X11-SCREEN.md) and [validation](VALIDATION-v2.0.6.md). Earlier named release sections are historical.

---

# v2.0.5 — scoped screen settings and public preview gate

X11ScreenSettings owns validated per-target geometry/scale preferences.
RemoteDesktopView sends logical dimensions/orientation to the selected host;
physical viewport/pointer translation remains scoped to that host. A separate
bounded scale job validates launch identity and uses the running private Xfconf
service, with recovery/restoration rather than a persistent daemon. Focus checks
read allowlisted window/input-context metadata; no text/title/key payload is read.
The public build defaults WAYLAND_PREVIEW to false. UI guards disable selection
and driver imports; retained Plasma preference resolves to XFCE without deletion.
DesktopProfiles.validate, WaylandProcess and WaylandRuntime reject disabled
preview operations before preparation. Private builds explicitly opt in through
-PtermuxWaylandPreview=true. No runtime public unlock, installed-rootfs edit or
change to agent environments is introduced. See [screen design](X11-SCREEN.md)
and [experimental architecture](ARCH-WAYLAND.md).

---

# v2.0.4 — private Bionic/glibc rendering

Six ordinary glibc targets share one reviewed bundled runtime but each owns its imported-driver/cache/lock/display/service state. The original Ubuntu cache path is retained. Private Mesa26.1.6 Zink loads our ICD through the guest Vulkan loader; libhybris loads the explicit Bionic HAL and Android buffer APIs. Process-only environment/binds leave installed guest libraries, Termux Wrapper and agent sessions untouched. See [native sources, patches, safety and limitations](BIONIC_GLIBC.md) and [Present audit](X11_PRESENTATION_AUDIT.md).

X11ExtraKeys routes events to the owned host; DesktopExtraKeys handles bounded floating geometry. Hiding/switching/detaching releases modifiers. Fullscreen moves the existing host and coordinates sibling controls, not another desktop/server. Jobs GPU Test is an optional per-target program and keeps reports in that environment. Driver imports are validated executable code, not sandboxed plugins.

---

# v2.0.3 — landscape presentation

The locale context overrides language/layout direction only. It must not pin a
copy of startup orientation, bounds, density or night mode. TermuxActivity forwards
each system onConfigurationChanged event to the locale helper, which applies its
current geometry and selected language to the existing Activity resources. No
workspace, composer or session is recreated. Fullscreen detection and the X11
viewport's WindowMetrics consequently use the current window configuration.
Tests deliver configuration events through the actual TermuxActivity callback;
manually setting orientation on a plain Activity is insufficient coverage.

Each WorkspaceDesktop channel owns a DesktopFullscreen presenter. Only a visible
X11/preparation host temporarily requests FULL_SENSOR; leaving restores the
Activity's captured orientation policy. A layout transition to landscape moves
the existing host into a full-window Dialog, not a second X11 connection/process.
Portrait/Back restores the original parent, index and LayoutParams. Back suppresses
re-entry until the next portrait turn. Cancel/dismiss callbacks must match their
originating Dialog, so queued callbacks cannot close a replacement host. Tab,
session, background and close paths release only this presentation.

A small sibling application Dialog carries the input button above the desktop
window and its Z-on-top native surface. It is nonfocusable, does not dim and passes
outside touches to the desktop; it needs no overlay permission. Both windows are
dismissed together. The desktop Dialog registers the existing keyboard-visibility
observer on its own window; visibility is read from the actual RemoteDesktopView. IME
insets also bound the visible crop for fullscreen windows that do not resize.
No per-frame polling or competing native hierarchy is introduced.

X11DisplayMode maps physical landscape dimensions to a logical screen capped
at2880px wide, using the existing native expected-size/viewport API. This doubles
the first candidate's logical dimensions on dense phones to halve enlargement.
Pointer mapping follows the reported source geometry; keyboard crop does not
change the logical desktop mode. Portrait uses the prior mode sizes. JNI/core
and renderer selection remain unchanged. Window/geometry tests use surrogate
views, not hardware SurfaceControl/IME validation.

# Optional style in every current X11 environment (v2.0.3)

DesktopPrograms defines the exact eight supported style targets; the UI, private
service and process launcher share that policy. The shell/Python guards accept
the same bounded set. Ubuntu AI, retired test roots and unknown aliases are excluded.
The old termux-style preference stays Termux-only; every guest uses its own
termux-style:<target> key. Running/busy desktops cannot switch styles. The launcher
copies the pinned style helper/pack into that target's control directory on run,
including disabled runs so previously applied properties can be restored.
The shell validates the 0/1 flag before placing it in the fixed guest command;
the guest session exports its own control directory. The helper uses Termux PREFIX
only for Termux and root / for each guest's system defaults, while writes and rollback
records stay inside the selected HOME. It retains the existing property-level
restoration, archive pin/path checks and preservation of user edits. There are no
new package dependencies, downloads, renderer flags or agent changes.

Panel defaults can come from /etc/xdg/xfce4/xfconf/xfce-perchannel-xml/xfce4-panel.xml
(openSUSE) or the classic /etc/xdg/xfce4/panel/default.xml. A fresh HOME is seeded
from the selected system template. Existing local type=empty properties inherit
system values for layout/plugin discovery only; explicit local values and array
orders win. This effective tree is not written wholesale into existing user XML.
Snapshots/restoration use the actual local tree, preserving inherited placeholders,
favorites and other plugin properties. The style never replaces a Whisker Menu
with Applications Menu. Window/icon/cursor/theme assets remain the same pinned pack.

The fullscreen input control is offset48dp from the top plus cutout safety,8dp
from the right, with window alpha0.65. It reads the existing per-channel keyboard state and changes its
selected/fill state only when that value changes. The existing pre-draw observer
provides automatic-input/Back updates; stable frames do not allocate new button
backgrounds or recreate the windows.

# Fedora Parole codec setup (v2.0.3)

Media/pkg:parole requests fixed Fedora dependencies and command-local Cisco
repo exclusion, matching the base installer's no-stub-download policy. DNF still
checks signatures/TLS and never receives allowerasing/nogpgcheck. The shared
OpenH264 helper retains the unchanged three-file openSUSE mode; Fedora adds a
pinned unmodified GStreamer1.26+ ARM64 bridge from Debian's package to its private
HOME, never installs the Debian package or replaces an RPM library/plugin.

The private launcher prepends only its codec and GST_PLUGIN_PATH_1_0, retaining
inherited paths and renderer/audio settings. A dedicated GST registry prevents
its private plugin registration from entering other applications' normal cache.
The helper validates every destination against owned hashes, writes a pending
proof, commits tracked files, creates a real decoder and checks GStreamer H.264,
AAC and MP4 demux factories. Only then is a versioned digest-bound proof committed.
Readiness requires that proof and exact file hashes; failed/interrupted setup
cannot become ready merely because files exist. Retry repairs missing owned
files; user edits/symlinks are preserved. A previous three-file managed setup can
upgrade to Fedora's four-file setup after ownership validation. Check paths do
not install codecs implicitly. Existing Parole processes must be relaunched.
[Sources](THIRD-PARTY.md) and [focused verification](AUDIT-FEDORA-MP4-2026-10-24.md).

# X11 component cleanup barrier (included in v2.0.3)

`X11Service` retains `cancelling` until its component worker closes and reaps the
pipe-owned guardian. State/detach/scoped close/Stop remain available; new package,
renderer and desktop operations are rejected. A matching epoch alone may publish
`cancelled`. Normal/error completions are also delivered after cleanup. Worker-only
`DesktopProcess.closeAndWait` preserves interruptions without abandoning an owned
group; native process identity/kill protocols are unchanged. The UI treats this
phase as busy. Ordinary Ubuntu joins the launcher-maintenance allowlist without
admitting Ubuntu AI or rewriting saved presets.
[Audit evidence](AUDIT-ENVIRONMENTS-X11-2026-10-23.md).

# Agent-chat recovery and run ownership (included in v2.0.3)

OpenCode adds optional schema8 string `Session.aiSocketId`. Missing/empty retains
the legacy profile-ID socket until the next explicit launch; each prepareLaunch
allocates a new UUID. Decode rejects invalid/non-string IDs atomically. Copies and
backup imports do not inherit a live socket; connection IDs and native history
bookmarks are independent. Older app versions ignore this optional field, so
downgrading requires restarting OpenCode rather than attaching to a newer run.
No user data is reset and no stored auth content is migrated.

Android snapshots the run before chat/status/queued control operations. OpenCode
request validation checks the resolved native session before dedup/admission.
Codex queued stop captures its socket file before scheduling; Gemini/Claude
queued stop captures both run ID and environment. Existing owned-process/marker
shutdown protocols remain in use; there is no process-name or foreign-PID kill.

Permission failure restores only the still-mounted request buttons in the same
UI generation. Gemini evicts an ID reservation only at guards before invoking
native callbacks; ambiguous execution keeps dedup protection.
[Scope and evidence](AUDIT-AGENT-CHATS-2026-10-23.md).

# v2.0.2 — Agent attachment and authentication boundaries

ChatAttachmentUi owns per-run drafts; ChatAttachments stages originals in separate
per-agent control roots using the existing environment bind. Shared agent media
projection validates regular files and conversation scope before native admission.
Codex uses localImage, Claude native multipart, OpenCode file URIs and Gemini native
@ references. Logs contain a bounded sample plus original path; generic binaries
are tool-accessible originals. Sampled video has timestamps and no audio.

OpenCode resolves staging beside its control socket, independently of nested
connection/receipt paths. Native media responses have a scoped bounded limit;
Android receives the compact projection, not raw base64. Dedup and retained drafts
protect ambiguous admissions. Output Markdown links are local regular-file cards;
APK installation uses an explicit read-only FileProvider copy and Android consent.

Codex auth reuses its app-server/account store; an explicit first-login action keeps
codes in the terminal and only reacts to its owned completion ID. Gemini keys are
AES-GCM encrypted with a separate Android Keystore alias and profile AAD. A private
one-time handoff is consumed/unlinked before CLI spawn; no key enters shell args,
chat or status JSON. Duplicate/import clears the nonportable ciphertext. Abrupt
kill before consumption can leave a private runtime handoff until next same-profile
launch; private backups remain sensitive. Full app/environment access is not an
isolation boundary against the configured agent. No global CLI config/proxy or X11
runtime change is included. [Details](CODEX-ANDROID-CHAT.md).

Earlier architecture sections are historical.

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

In v2.0.1, desktop checking reports base and renderer readiness separately.
Only matching authoritative Turnip/VirGL success can skip renderer preparation;
launch retains independent validation. Non-composited GPU targets disable only
native GPU Present-copy offload to retain X window clipping. Native binaries,
compositor choices, selected GPU drivers and process ownership are unchanged.
Backup import retains fresh preset IDs/history paths and verifies serialized
preset content, including numerically equivalent JSON integer types.

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


## Unreleased storage worker lifecycle (2.0.5-test)

WorkspaceBackupEngine owns one private Python process per operation. The shared
backup/restore/cleanup lock stays held until that process has actually exited and
its private request/script directory is cleaned up. Cleanup waits through Java
thread interruption and restores the interrupted flag afterward. Cancellation
sends at most one termination request to that owned Process; it adds no PID search,
foreign-process signal or timer-based unlock. Python retains responsibility for
its native/archive children. An unresponsive owned process keeps the operation
blocked rather than allowing overlapping storage mutations.

One terminal UI callback is queued after cleanup/unlock. It captures the completed
result/error: Cancel arriving after completion cannot silently suppress a restore
result and its preset registration. Progress stops changing while cancellation is
pending; the explicit Cancel control disables and the window stays visible until
the terminal callback. Native environment removal still has no cancellation
control after confirmation. Scanning forms can close independently, but their
cancelled worker retains the same lock until exit. Archive format, extraction
boundaries, storage consent, schema and installed environments are unchanged.


## Unreleased transfer ownership and publication (2.0.6-test)

Upload/download/move choices carry a controller-local generation. A newer choice,
disconnection, confirmation dismissal or accepted start invalidates the old
generation; closed/running/disconnected checks are repeated before queueing.
Dismissing an old dialog cannot invalidate a newer generation. Per-operation
connection snapshots copy only SSH identity/settings and diagnostics preference;
move paths are copied before confirmation. No automatic replay, global transport
restart or cross-profile authentication fallback is added.

Downloaded files still stream to exclusive UUID temporary names under a separate
batch root. Publication uses mv -T -n, so a directory or directory symlink appearing
between the no-overwrite check and rename cannot absorb the temporary file.
Success also requires the original temporary path to disappear. Existing regular
files are retained. SFTP packet/window/host-key behavior and local writer lifecycle
are unchanged. See the transfer audit for fixture coverage and live-network limits.


## Unreleased workspace file coordination (2.0.7-test)

WorkspacePersistence coordinates WorkspaceStore leases by canonical file path in
the app process. A shared single writer orders snapshots across Activity lifetimes.
Pending immutable bytes are registered before any disk write, so a new store can
read the newest queued state without opening AtomicFile during an active write.
AtomicFile read/recovery is used when no snapshot is pending. The registry lock
covers ownership/pending-state transitions; background disk writes and callbacks
run outside it. Queued writes survive lease close. A writer is shut down and its
registry entry retired only when no owner or pending snapshot remains.

Closing WorkspaceStore refuses later saves, recent-command/mode changes and UI
completion delivery. A failed write attempts AtomicFile rollback and
notifies current live leases, including a new screen replacing the original
writer's owner. This is in-process coordination, not a multi-process database,
a durable queue or a merge of independently edited store snapshots. SIGKILL or
power loss can still discard writes not yet committed. No X11 process instantiates
WorkspaceStore; its processes/renderers remain independent.

Command, session and note IDs must each be unique within their own collection on
encode/decode. Validation finishes before loaded lists replace current lists.
Malformed duplicate-ID libraries are retained read-only; an invalid outgoing
snapshot cannot replace committed data. Schema8 and existing credential/profile
serialization remain unchanged. See the persistence audit for coverage and limits.

## Unreleased updater/receive boundaries (2.0.8-test)

UpdateConnection owns one operation's transport across allowlisted HTTPS redirects.
Cancellation and connection publication share a lock; network work/disconnect run
outside it, and response/redirect/error paths release ownership. Pre-install byte
revalidation runs in a worker with the updater busy gate, then returns to the
main thread for a read-only FileProvider URI handoff. TermuxFileReceiverActivity
accepts only basenames and uses atomic exclusive/no-follow create with0600 mode.
Existing names/links survive; failed new output and owned streams are cleaned up.
See AUDIT-UPDATES-ENTRYPOINTS-2026-10-23.md for the trust model and remaining
synchronous incoming-copy and cache/lifecycle limits.

## Unreleased installer ownership (2.0.9-test)

EnvironmentSetup stages scripts and cancel markers in separate private job-*
directories, retaining the existing control root for download metadata/cache.
Its output collector waits for process exit on stdin/read/callback/wait errors
before publishing a terminal result, and restores interruption after draining.
The Python supervisor uses waitid/WNOWAIT to keep the session leader unreaped
until final group signals on success/failure/cancel/output error. Only then is
wait() allowed to release the leader identity. No borrowed PID signaling is used.
Guest shell readiness resolves symlinks component by component inside the guest
root, with a finite link limit and a regular executable final target.
See AUDIT-INSTALLERS-2026-10-23.md for trust boundaries, process/Activity death,
prerequisite preparation and detached-session limitations.

## Unreleased file choices and exports (2.0.10-test)

WorkspaceFileBrowser owns one generation-bound transfer choice across mode
selection, preflight and confirmation. Acceptance consumes ownership; dismissal,
replacement, selection/navigation changes and close invalidate it. Identity checks
prevent delayed onDismiss from clearing a newer dialog. The TransferBackend seam
delegates to ProjectFiles and permits no-I/O queued-work tests. ProjectFileExport
uses CREATE_NEW/WRITE/NOFOLLOW_LINKS for a single destination open, retries finite
name collisions, enforces a stream byte bound and checks close/interruption.
Download/failed export staging is released; successful Share cache is retained
for read-only URI access. Existing ProjectFiles process termination is unchanged;
whole-tree shutdown and batch rollback are not established by this patch.


## Unreleased file transport ownership (2.0.11-test)

ProjectFiles delegates admission and child lifetime to ProjectFileTransport.
Reservation precedes ProcessBuilder.start; a launch/close race retains ownership
until the returned child exits. One operation owns its Process, pipes, deadline,
forced-stop fallback and completion latch. No numeric PID or process-group signal
is used. A canceled or expired operation is cleaned on a daemon worker even if
its consumer never reads/closes the stream; owner close only requests cancellation.
An operation releases admission after direct-child wait and stream cleanup.
Repeated close of a completed stream cannot close the ProjectFiles instance or
signal a newer child. Concurrent commands fail BUSY instead of sharing a slot.

Reads/skip enforce an actual byte bound, accept zero bytes/unsigned single-byte
values, and require zero child status at EOF. Reset is unavailable. Deadlines stay
90 seconds for queries/exports and six hours for copy batches/local upload input.
A direct child first receives Process.destroy; after one second a still-running
child receives Process.destroyForcibly. Cleanup retains the wait barrier and the
calling thread's interrupt flag. File-manager, editor and package workers remain
separate from PTYs and installed terminal sessions. Existing SSH host-key pinning,
askpass secrets and environment routing are preserved.

This is direct-child ownership, not a process-group/remote-job supervisor. A remote
shell or detached descendant can survive its local transport; no whole-batch
rollback or remote-idempotency guarantee is added. See
AUDIT-FILE-TRANSPORT-2026-10-24.md for evidence and remaining device/network checks.


## Unreleased file transfer recovery (2.0.12-test)

ProjectFileTransfer.execute creates a fresh ProjectFileReceipt for each accepted
batch. The generated shell prints numbered per-item progress after its existing
copy/move and owned-stage cleanup, then one final completion line after the loop.
The ID is a validated32-character lowercase hex nonce. Receipts use fixed ASCII
records and LF boundaries; no path or file content is inserted into the protocol.
Readiness still uses the unchanged READY query and performs no mutation.

The8KiB response collector accepts only ordered progress for its own ID and the
matching final count; incomplete records, repeated/reordered own records and a
missing completion line cannot confirm success. Unrelated bounded diagnostic
lines/foreign IDs do not count. The runner must also return successfully after
ProjectFileTransport's zero-exit check. On failure the collector supplies only the
received acknowledged prefix. WorkspaceFileBrowser explains that actual work may
have progressed further or still be running and that no automatic replay occurs.
A subsequent read-only directory refresh is not a retry of the batch.

ProjectFiles SSH adds ServerAliveInterval=15 / ServerAliveCountMax=2 alongside its
existing connect timeout, strict known-hosts policy and credential handling. This
bounds detection of an unresponsive SSH peer after connection establishment;
it does not reconnect, terminate a live remote command, alter agent/tunnel SSH
settings or change overall command/batch deadlines. No PID signaling or process-
shutdown mechanism changes in this stage. Receipts are notifications, not fsync,
a durable journal, cryptographic attestation or a remote cancellation protocol.
See AUDIT-FILE-RECOVERY-2026-10-24.md for fixture and device boundaries.


## Unreleased cumulative regression pass (2.0.13-test)

The app retains minSDK26; embedded RemoteDesktopView requires API30. The controller
now checks API30 explicitly before X11 IME diagnostics and focus-driven keyboard
calls, alongside the existing guarded construction/sync/service paths. It does
not create a second desktop, stop process groups, change routing or change input
behavior on the supported graphical path. Orientation values use framework named
constants with the same integer values. WorkspaceIconButton deliberately retains
platform TextView and custom icon tint/drawing; a narrow AppCompatCustomView lint
exception documents this. Badge Paint is retained per widget instead of allocated
during every badge draw. Layout sizes, padding and icon placement are unchanged.

See AUDIT-REGRESSION-2026-10-24.md for cumulative evidence and the limits of older
lint/Kotlin tooling. Passing Java/widget fixtures does not establish Android IME,
real frame timing, renderer compatibility or every fresh-device rootfs path.


## Unreleased settings lifecycle (2.0.14-test)

All12 preference adapters in app/fragments/settings are created for their screen
rather than retained by static mInstance fields. Fragments bind the adapter once
through PreferenceManager; SharedPreferences remains responsible for values and
its ordinary file/value cache. Seven unused duplicate Context fields are removed.
Five logging adapters intentionally use their owning screen's Context for current
locale/toasts, and all package preference factories still receive that live UI
Context for existing error dialogs. There is no application-context substitution,
weak singleton or custom retention registry. Closing/recreating a screen obtains
a fresh adapter while retaining the same backend class, package, files and keys.

The tests establish screen-adapter identity, absence of static adapter/Context
roots, real preference-widget value persistence and current-context error/language
routing in a controlled Android model. They are not a phone heap/GC measurement
or a guarantee about every Android/framework/native retention path. See
AUDIT-SETTINGS-2026-10-24.md for before/after and error-path evidence.

## Unreleased legacy provider guards (2.0.15-test)

ProviderFiles centralizes canonical component boundaries, Locale.ROOT extension
lookup and descriptor ownership for TermuxDocumentsProvider and the legacy
TermuxOpenReceiver.ContentProvider. Open without create/truncate or final-link
following; validate the descriptor's kernel path/regular type, then truncate if
requested. Mount aliases require positive directory device/inode identity.
NONBLOCK avoids opening a substituted FIFO as a blocking stream. The Linux
CLOEXEC value preserves API26 support without a Java API27 field reference.

SAF HOME creation/deletion pins a parent descriptor and uses only a basename
under /proc/self/fd. Exclusive creation preserves existing/dangling-link names;
root deletion is forbidden. Internal link deletion unlinks its original name.
Search uses visited directory targets and candidate/result limits, retaining
ordinary document IDs and alias names. Canonical errors reject access.
Legacy sharing keeps its files/shared-storage roots, manifest RUN_COMMAND gate,
URI grants and allow-external-apps policy. Protected property inodes are read-only,
including hard links. Metadata/type requests use the same boundary checks;
sharing encodes URI paths and keeps64-bit sizes. SAF and generic workspace
browsing are separate mechanisms. There is no filesystem snapshot or isolation
from an already-trusted shell moving directories after validation.

See AUDIT-PROVIDERS-2026-10-24.md for exact fixtures, Robolectric syscall-model
limits, Android storage/manual checks and intentional access restrictions.

## Unreleased analysis and remaining source fixes (2.0.16-test)

The APK keeps its AGP4.2.2/Gradle7.2/native pipeline. Independent modern-lint staging
uses AGP7.4.2/Gradle7.5.1/JDK11 with dependency analysis and identical production
Java/resources/manifests. A stage lock, stale-output removal, Gradle-exit/XML/
engine/module/infrastructure checks prevent incomplete zero-error reports.
The compileSDK35-versus-AGP33-tested warning remains; see LINT.md and the audit.

X11 diagnostic/renderer calls have explicit API30 guards; native process ownership
and stop/cancel paths are unchanged. Media3's wrapped factory API has scoped OptIn.
Shared protocol values use Locale.ROOT while human date formatting keeps default
locale. Fixed-target notification/report intents are immutable; externally supplied
plugin result PendingIntents retain their own contract. Existing intent-filter
exports are explicit, without broadening component permissions.

Thumbnail EXIF uses AndroidX1.4.2 rather than the platform parser, preserving matrix
logic/cache/original files. Attachment Download delegates to ProjectFileExport's
exclusive creation, numeric collision suffix and cancellation/partial cleanup.
ZoomImage distinguishes tap from drag/pinch/cancel for performClick; sizing and
render transforms are retained. No persistent data format or account migration.


## Environment aliases (v2.0.9)

See [Environment aliases](ALIASES.md) for the separate library, category and star
pickers, environment-scoped shell files, write safeguards and restart semantics.
This feature excludes Ubuntu AI and does not alter running sessions.

[Chat scale](AGENT-CHAT-SCALE.md) is optional per-session schema8 presentation
metadata. It uses baseline Android text/layout snapshots, stable transcript rows,
an explicit composer container and normal scroll anchors rather than transforms,
root reconstruction or native agent RPCs.
