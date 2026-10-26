# Current release — v2.0.9 (code1417)

Persistent aliases in eight standard terminal environments, explicit shell/JSON
import and export, and per-session50–150% chat scaling for all four agents.
Ubuntu AI is excluded from alias destinations. Same com.termux/certificate,
schema8, ARM64, min26/target28/compile35, Android8+ app and Android11+ X11.
No data reset or account/rootfs migration. Build tools and pinned native inputs
remain repository-local. X11 assets, drivers and JNI are unchanged from v2.0.8.
See [release](docs/fork/RELEASE-v2.0.9.md),
[validation](docs/fork/VALIDATION-v2.0.9.md), [aliases](docs/fork/ALIASES.md)
and [chat scale](docs/fork/AGENT-CHAT-SCALE.md).

---

# Historical release — v2.0.8 (code 1413)

Developer Markdown, arbitrary file cards, shared file sorting, attachment multi-selection and system media selection are native chat UI changes across four providers. A scoped Stop control and VSync-driven activity line target60FPS; acknowledged message sends dismiss the keyboard. Existing com.termux/signer/schema8, ARM64/Android 8+ app and Android 11+ X11 requirements remain. No data reset, account/rootfs migration or new graphics payload. Build tools and pinned native inputs remain repository-local. See [release](docs/fork/RELEASE-v2.0.8.md), [validation](docs/fork/VALIDATION-v2.0.8.md), [usage](docs/fork/USAGE.md) and [Markdown licenses](docs/fork/THIRD-PARTY.md).

---

# Historical release — v2.0.7 (code1410)

Mesa 26.2.5 and on-demand components extend the existing graphics repository distribution. The accepted desktop/input behavior, Android8+/ARM64 app, Android11+ X11, com.termux/signer/schema8 and min26/target28/compile35 remain. No data reset, account migration or rootfs reinstallation. Build inputs are repository-local; optional theme/Alpine VirGL ZIPs are prepared separately and excluded from APK assets. See [release](docs/fork/RELEASE-v2.0.7.md), [validation](docs/fork/VALIDATION-v2.0.7.md) and [component sources](docs/fork/GRAPHICS-COMPONENTS.md).

---

# Historical release — v2.0.6 (code1395)

X11-only source: the retired Wayland/KDE experiment and its build flag/runtime inputs are removed. The app retains Android8+/ARM64, X11 Android11+, min26/target28/compile35, com.termux, the existing signing identity and workspace schema8. No data reset or rootfs migration is required.

All seven PRoot desktops receive a private session-bus configuration allowing the actual Android app UID; native Termux retains its existing startup. Graphics archives are pinned downloads from the separate components repository. The unchanged Bionic-for-glibc payload remains in source for review/rebuilding but is excluded from the APK and downloaded on first use when missing. Run `python3 scripts/prepare-x11.py` to prepare/verify repository-local build inputs. See [release](docs/fork/RELEASE-v2.0.6.md) and [validation](docs/fork/VALIDATION-v2.0.6.md).

---

# Historical v2.0.5 — code1376

X11 screen geometry/scaling, automatic keyboard focus filtering and compact extra
keys. Same package, signer, schema8, min26/target28/compile35/ARM64; no reset or
rootfs migration. Public builds disable experimental Wayland in UI/backend by
default. Only private development builds explicitly pass
`-PtermuxWaylandPreview=true`; this is not a user-facing runtime toggle. Build
native inputs with the existing X11 preparation plus `scripts/prepare-wayland.py`
inside the repository. Pinned source, license and runtime checks remain required.
See [release](docs/fork/RELEASE-v2.0.5.md) and
[validation](docs/fork/VALIDATION-v2.0.5.md).

---

# v2.0.4 — code1366

Bionic-for-glibc loading in six ordinary glibc desktops, with independent driver caches. X11 extra keys and Jobs GPU Test; no valid-data reset/migration or installed-library replacement. Android/SDK/package/signing/schema requirements remain those of2.0.3. Native core unchanged; bridge binaries are isolated assets with corresponding sources, patches and licenses. See [release](docs/fork/RELEASE-v2.0.4.md), [bridge](docs/fork/BIONIC_GLIBC.md) and [validation](docs/fork/VALIDATION-v2.0.4.md).

---

# Historical v2.0.3 — code1352

Full-screen landscape X11 reuses each selected target's existing display and
desktop, with screen-adapted logical sizing and one floating input button. Portrait
or Back returns to the embedded host. All cumulative source-audit fixes below
are included. Android8+/ARM64, X11 Android11+, package/signer/schema8 and
min26/target28/compile35 are retained. Build instructions and pinned native inputs
are unchanged; use the separate [modern lint gate](docs/fork/LINT.md).
No data reset/rootfs migration. See [release notes](docs/fork/RELEASE-v2.0.3.md)
for intentional file-access restrictions and remaining validation.
The optional Kali-inspired style supports all eight current X11 environments
with independent checkbox/state and HOME-scoped restoration. Distribution menu
plugins and inherited defaults are preserved. The fullscreen | button is
lower and follows the existing keyboard enabled state.
Fedora Parole gets a private pinned decoder/plugin through Video and audio,
with runtime-readiness checks and no system-library replacement. The fullscreen
input window is65% opaque and another24dp lower.
Historical pre-publication note: v2.0.2 was current before v2.0.3 was published.

# Historical private source-audit completion — 2.0.16-test/code1345

Public stable remains v2.0.2/code1331. Separate compatible lint covers all five
modules; the APK toolchain, native inputs, signer/package/schema8 and SDK/ABI
requirements remain. Source fixes address X11 guards, locale handling, immutable
notification actions, EXIF parsing and exclusive attachment saving. No valid-data
reset or account/rootfs migration. [Evidence](docs/fork/AUDIT-LINT-2026-10-24.md)
and [lint setup/limits](docs/fork/LINT.md) separate 1,209 passing JVM cases from
manual/native/device gaps and retain 209 warnings without a blanket baseline.

# Private locale/provider audit — 2.0.15-test/code1344

Public stable remains v2.0.2/code1331. Canonical roots and descriptor/parent guards
harden the two legacy Android providers. Search/MIME are locale independent;
share paths are encoded and metadata sizes remain long. Permissions/policy,
ordinary IDs, signer/package/schema8 and SDK/ABI requirements stay unchanged.
Outside-HOME SAF links and missing/special legacy share files are rejected;
valid data needs no reset or environment/account migration. Device-level provider
checks remain separate from 42 passing JVM cases and 0/118 executed lint results.
[Evidence and limitations](docs/fork/AUDIT-PROVIDERS-2026-10-24.md).

# Earlier private settings lifecycle audit — 2.0.14-test/code1343

Public stable remains v2.0.2/code1331. The private build removes static settings-
adapter ownership of the first screen without changing preference files/keys,
package/signer/schema8 or SDK/ABI requirements. No data reset or migration.
[Settings evidence and device limits](docs/fork/AUDIT-SETTINGS-2026-10-24.md).
Previous cumulative verification remains specific to its recorded artifact.

# Earlier private cumulative audit — 2.0.13-test/code1342

Public stable remains v2.0.2/code1331 below. The private audit keeps the same
package/signer/schema8, ARM64/Android8+ app minimum and Android11+ embedded-X11
minimum. No reset or environment migration. SDK28 targeting/package separation
remain separate architectural work; this audit does not claim to solve them.
Cumulative automated evidence and remaining tooling/device gaps are in
[the regression report](docs/fork/AUDIT-REGRESSION-2026-10-24.md). Existing build
inputs are reused and byte-checked; no shared SDK/system package/installed rootfs
is changed. New public releases still require increasing versionCode, localized
compatibility highlights and update.json from their final verified APK.

# v2.0.2 — Agent chat improvements

Current release: versionName2.0.2/code1331, Android8+/ARM64, unchanged
com.termux/signature/schema8. Combines private chat builds1326–1330 under the next
public version after2.0.1; Android update ordering uses the increasing versionCode.
Direct attachments, native agent media input, thumbnails/output cards and sign-in
changes are described in [release notes](docs/fork/RELEASE-v2.0.2.md) and
[validation](docs/fork/VALIDATION-v2.0.2.md). No reset or environment migration.
Gemini preset keys are device/profile-bound and need re-entry on duplicate/import.
Build tools, pinned native inputs and requirements are unchanged from v2.0.1.
Native libraries are byte-compared with the verified previous APK. Keep additional
tools/caches/build outputs inside ignored repository directories.

Historical build/release records follow.

# Historical v2.0.1 — Fix Wrapper

Current maintenance release: versionName2.0.1/code1325, Android8+/ARM64,
unchanged com.termux/signature/schema8. Fixes GPU window overlap, backup
preset/history identity and renderer readiness/repair. No data reset.
[Release notes](docs/fork/RELEASE-v2.0.1.md) ·
[Validation](docs/fork/VALIDATION-v2.0.1.md).
Build tools, pinned native inputs and requirements are unchanged from v2.0.

Historical build/release records follow.

# v2.0.0 — release build

Current release: **Termux-job's X11 AVAILABLE**, versionName2.0.0/code1320,
Android8+/ARM64, unchanged signer and com.termux. The app header uses an italic
name and a separate v2.0 label without changing its control sizes.
See [release notes](docs/fork/RELEASE-v2.0.0.md),
[validation](docs/fork/VALIDATION-v2.0.0.md),
[upstream sources/licenses](docs/fork/THIRD-PARTY.md).

Build prerequisites and pinned X11/native/keyboard inputs are prepared through
scripts/prepare-x11.py; scripts/build-local-arm64.sh invokes it automatically.
Additional build tools/cache stay in ignored .tools/ and .verification/. The
minimal private Alpine Mesa archive is a reviewed vendored build input in
third-party/virgl-client, with upstream source hashes, notices and build options.
The optional style requires design/x11-kali/wallpaper.png. Use the documented
ARM64 build, not a Java-only stage, to package both terminal JNI libraries.

The sections below are historical development records, not current release
validation or claims that every intermediate experiment remains enabled.

## Частная v1.7.104-x11-test · GPU INFO Ubuntu VirGL

Разработчик подтвердил запуск Ubuntu XFCE и glmark2 через VirGL; GPU INFO
падал BadMatch/X_GetImage. Для Ubuntu VirGL применён тот же ограниченный по
времени EGL-запрос сведений, который уже используется другими Linux-целями.
Рендер Ubuntu ANGLE GL, glmark2, CPU/Turnip/wrapper и прочие цели сохранены.
[Проверки](docs/fork/VALIDATION-v1.7.104-x11-test.md).

## Частная v1.7.103-x11-test · явный клиент VirGL

После v102 разработчик подтвердил Arch и openSUSE. Debian по-прежнему зависал
до оконного менеджера. Read-only стек xfce4-session с соответствующими debug
символами Mesa25.0.7 показал GLX → Kopper → Zink → ожидание mutex при очистке.
Выбранный GALLIUM_DRIVER=virpipe сам по себе не исключал эту раннюю ветку GLX.

Общий helper VirGL для всех восьми X11-целей выбирает swrast-loader с драйвером
virpipe и отключает Kopper. Это пересылка рисования Android GPU, а не llvmpipe.
Termux VirGL использует ANGLE Vulkan и fork на соединение; остальные backend
сохранены. Turnip и wrapper очищают новые переменные VirGL перед своим запуском.
CPU, настройки агентов, авторизация и данные чатов не меняются.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.103-x11-test.md).

## Частная v1.7.102-x11-test · Debian/Arch/openSUSE VirGL

GPU INFO через EGL расширен на Debian, Arch и openSUSE. Debian и Arch используют
ANGLE Vulkan; Arch также получает fork на подключение. Рабочий backend openSUSE
сохранён. Другие рендеры и уже проверенные Alpine/Fedora/Void не меняются.
Настоящие изолированные EGL-запросы и отрисовка прошли для трёх целей.
Запуск полного Debian XFCE и Arch glmark2 после изменения требуют проверки;
переключение backend — кандидат на исправление Debian, не доказанный результат.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.102-x11-test.md).

## Частная v1.7.101-x11-test · GPU INFO и Fedora VirGL

Alpine/Fedora/Void получают сведения VirGL через ограниченный по времени EGL-запрос
вместо падающего glxinfo. Fedora VirGL переведена на ANGLE Vulkan. CPU и Turnip
сохраняют прежние пути; отдельные проверки это подтверждают. 48 JVM, 32 Python
проверки, три настоящих EGL-запроса и три пробы отрисовки прошли. Полный GUI и
Fedora glmark2 после обновления ещё требуют проверки разработчиком.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.101-x11-test.md).

## Частная v1.7.100-x11-test · исправления Alpine/Void VirGL

В APK включён отдельный минимальный Mesa-клиент Alpine: upstream пакет не содержит
virpipe. Void использует ANGLE Vulkan; пять новых систем — fork на соединение,
совместимый также со старым протоколом openSUSE. Готовность проверяет драйвер.
68 JVM + 14 Python проверок, 10 реальных изолированных EGL-проб, ARM64 build
и аудит APK прошли. Полный GUI/glmark2 и чистый другой телефон требуют проверки;
в отдельных GL-пробах остаётся сообщение EGL_BAD_ACCESS при очистке контекста.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.100-x11-test.md).

## Частная v1.7.99-x11-test · Kali и VirGL

Touch Workspace удалён; активный старый выбор переходит на Kali. Галочка стиля
по-прежнему выключается для обычного XFCE. Ресурсы Kali и нативное ядро прежние.
VirGL добавлен к Debian, Alpine, Fedora, openSUSE и Void с установкой Mesa в
выбранную систему и отдельным локальным Android/Termux процессом каждого стола.
80 JVM + 29 Python проверок, ARM64 build и аудит APK прошли. Реальная отрисовка
новых вариантов требует проверки разработчиком; релиз пока не публикуется.
[Проверки](docs/fork/VALIDATION-v1.7.99-x11-test.md).

## Частная v1.7.98-x11-test · обычная мышь в Touch Workspace

По выбору разработчика возвращено управление как в обычном XFCE, оформление
карточек сохранено. Для собственного окна Touch добавлен X11 background pixmap
со снимком своих виджетов: восстановление участков при перемещении окон без
композитора, без постоянного таймера и чтения чужого экрана.
[Устройство](docs/fork/TOUCH-WORKSPACE.md), [проверки](docs/fork/VALIDATION-v1.7.98-x11-test.md).

## Частная v1.7.97-x11-test · касания и окна Touch Workspace

Двойной тап со смещением остаётся в точке первого щелчка; время согласовано
с GTK (500мс). Удержать и потянуть — перемещение/размер окна; удержать и
отпустить без движения — правая кнопка. Зоны захвата рамок Touch расширены
с6 до20px при тонком контуре. Kali и остальные окружения сохраняют прежние
жесты и рамки. [Проверки](docs/fork/VALIDATION-v1.7.97-x11-test.md).

## Частная v1.7.96-x11-test · Touch Workspace

Ниже Kali добавлена взаимоисключающая тема Touch Workspace для базового
Termux XFCE: крупные карточки, свой GTK-экран и нижняя панель, прямые тапы.
Переключение до запуска, исходные настройки восстанавливаются. Прочие rootfs
и агентские чаты не переводятся в новый вид.
[Использование и устройство](docs/fork/TOUCH-WORKSPACE.md),
[проверки](docs/fork/VALIDATION-v1.7.96-x11-test.md).

## Частная v1.7.95-x11-test · Kali-inspired вместо XP

Под той же галочкой стиля базового Termux XFCE: тёмные окна и панели,
синие значки/выделения, тонкие рамки, круглые сглаженные кнопки и фон с драконом.
Фотофон адаптирован к вытянутому экрану. Возврат первого оформления сохранён.
Другие окружения не изменены. [Устройство и ресурсы](docs/fork/X11-STYLE.md),
[проверки/ограничения](docs/fork/VALIDATION-v1.7.95-x11-test.md).

## Частная v1.7.94-x11-test · значок Menu и рамки Retro XP

В Menu — собственный значок Termux >_ вместо мыши XFCE; меню и пункты прежние.
Исправлен рисунок рамок: сплошные края вместо пунктира, скруглённые кнопки
закрытия/сворачивания/разворачивания и отсутствие квадратной подложки под
значком приложения. Только opt-in стиль базового Termux, прочие системы и
настройка подключения не меняются. [Проверки](docs/fork/VALIDATION-v1.7.94-x11-test.md).

## Частная v1.7.93-x11-test · Retro XP вместо прежнего стиля

Под прежней галочкой «Стиль Termux-job’s» теперь Retro XP, только Termux XFCE:
синие рамки, светлые окна, крупные цветные базовые значки, увеличенная нижняя
панель с Menu и новый фон. Первое сохранённое оформление остаётся обратимым.
«Сервер + Агент» во всех сессиях получил высоту/форму соседней кнопки Программы;
ширина прежняя, бегущая линия передачи сохранена. Команда подключения агентов
из v1.7.92 остаётся. [Устройство темы](docs/fork/X11-STYLE.md),
[проверки и ограничения](docs/fork/VALIDATION-v1.7.93-x11-test.md).

## Частная v1.7.92-x11-test · команда подключения агента

Необязательное поле над расширенным дебаг-журналом: путь к своему исполняемому
запускателю в Ubuntu/Ubuntu AI. Пусто — прежний запуск. Настройка отдельна для
сессии/пресета, применяется после перезапуска; текущие процессы не меняются.
Личные туннели, ключи и адреса не включены в APK. При ошибке запускателя
скрытого запуска напрямую нет. Старый APK игнорирует поле и запускается обычно.
[Назначение и контракт](docs/fork/AGENT-CONNECTION.md), [проверки](docs/fork/VALIDATION-v1.7.92-x11-test.md).

## Частная v1.7.91-x11-test · стиль Termux-job's

Только для базового Termux: галочка под переключением рабочего стола,
применяемая при следующем запуске. Тёмное оформление, увеличенные элементы,
курсивный фон и статичные чёрные курсоры с белой обводкой. Базовые значки
Adwaita, значки приложений сохраняются. Отключение возвращает прежние свойства,
сохраняя последующие ручные правки. Другие окружения/агенты не изменяются.
[Устройство стиля](docs/fork/X11-STYLE.md), [проверки и ограничения](docs/fork/VALIDATION-v1.7.91-x11-test.md).

## Частная v1.7.90-x11-test · GPU & GPU INFO

В рекомендуемые X11-программы добавлена группа GPU & GPU INFO с пакетами
своего окружения и двумя английскими ярлыками на его рабочем столе.
Для Alpine используется glxgears, openSUSE — glmark2-es2/EGL, остальные — glmark2.
GPU INFO показывает glxinfo -B. Рендер не меняется; установка только после согласия.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.90-x11-test.md).

## Частная v1.7.89-x11-test · журналы ошибок установки окружений

При неудачной установке любого окружения слева от «Продолжить» появляется
«Подробнее». Окно показывает журнал последней попытки с прокруткой и кнопкой
«В Download» (TXT, с запросом разрешения хранения). Ошибка proot-distro
`Failed to install:` больше не скрывается последней строкой справки.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.89-x11-test.md).

## Частная v1.7.88-x11-test · выбор сохранённых Bionic-драйверов

Под рендером Wrapper добавлена карточка выбранного драйвера и список ранее
импортированных версий. Переключение без повторного ZIP/скачивания; новые импорты
сохраняют предыдущие версии. Текущий V38 из v1.7.87 подхватывается автоматически.
Карточка и папка видны только при Wrapper, настройка только для Termux.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.88-x11-test.md).

## Частная v1.7.87-x11-test · Wrapper для Termux

Добавлен экспериментальный рендер Wrapper · Bionic только для Termux.
ZIP своего Turnip выбирается нашим файловым менеджером из памяти телефона;
драйвер хранится отдельно и подключается при запуске Termux XFCE.
Остальные окружения сохраняют прежние рендеры. Рабочая v1.7.86 сохранена;
разработчик подтвердил исправление Thunar Arch.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.87-x11-test.md).

## Частная v1.7.86-x11-test · зависание Thunar Arch

Добавлена проверка прав корня Arch и точечное восстановление доступа
владельца после подтверждения установки X11. Исправляет обнаруженный
источник атрибутных уведомлений PRoot/Thunar; остальные файлы и rootfs
сохраняются. Автообновление файлового менеджера не отключается.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.86-x11-test.md).

## Частная v1.7.85-x11-test · запуск Arch XFCE

Добавлена совместимость Glycin2 для процессов рабочего стола Arch:
исправление отказа image loaders/bubblewrap при загрузке значков XFCE.
Системные библиотеки не заменяются; GPU и установщик сохранены.
Добавлены категории диагностики декодера и значков.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.85-x11-test.md).

## Частная v1.7.84-x11-test · подробности ошибок X11

Исправлен пропуск ошибок Pacman `failed retrieving file` в диагностике.
При ошибке установки доступны «Подробнее», прокрутка сохранённого хвоста
и копирование полной строки. Сетевые настройки и установщик не меняются;
причина обрыва скачивания Arch ещё требует подтверждения.
[Проверки](docs/fork/VALIDATION-v1.7.84-x11-test.md).

## Частная v1.7.83-x11-test · Arch Linux X11

Добавлен собственный XFCE Arch Linux ARM: CPU, Turnip + Zink и VirGL,
программы через Pacman, ввод/звук/диагностика. Установка и обновление каталога
Arch также обновляют пакеты выбранного Arch с предупреждением. Драйверы
не заменяют системную Mesa.105 Python и66 JVM-проверок прошли, APK проверен;
GUI/чистая установка/GPU Arch ждут проверки разработчиком.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.83-x11-test.md).

## Текущая локальная сборка: v1.7.82-x11-test / code1297

Для управляемых команд XBPS (Void) задаётся IPv4 и снимается FORCE_IPV6 только у дочерней команды. Охвачены рабочий стол, программы и обычный менеджер пакетов. Другие окружения/родительская среда не меняются. No route to host добавлен в структурированную диагностику. Сбой воспроизведён реальным клиентом/библиотеками Void из текущей Ubuntu: IPv6 не имеет маршрута, по IPv4 подпись успешно скачалась с TLS. 43 Python- и38 JVM-тестов прошли, APK проверен; полная установка стола на устройстве ещё требует проверки. Предварительный загрузчик окружений не возвращается. Подробности: `docs/fork/VALIDATION-v1.7.82-x11-test.md` (из docs/fork — `VALIDATION-v1.7.82-x11-test.md`). GitHub не публиковался.

## Текущая локальная сборка: v1.7.81-x11-test / code1296

Удалён предварительный загрузчик environment-download.py и адаптер внутреннего OCI API. Все установки окружений/подготовка агентов используют штатный CLI. Arch ARM сохраняет фиксированный проверяемый архив. Стоп/прогресс и повтор остаются; незавершённый слой может скачиваться заново, целые слои переиспользует native-кэш. Кнопка Resume хранит отметку попытки по alias; старые скачанные части автоматически не удаляются. 42 Python- и21 JVM-тест прошли; APK проверен. Чистый Samsung ещё требует проверки. Корзина для незавершённых установок — следующая задача с учётом общего native-кэша. Подробности: `docs/fork/VALIDATION-v1.7.81-x11-test.md` (из docs/fork — `VALIDATION-v1.7.81-x11-test.md`). GitHub не публиковался.

## Текущая локальная сборка: v1.7.80-x11-test / code1295

В отчёт X11 добавлены ограниченные очищенные строки ошибок скачивания, с сохранением причины после адреса репозитория. Очистка эвристическая: отчёт нужно просматривать перед отправкой. Первый и повторный запуск установки всех OCI-окружений проверяет интерфейс адаптера и при необходимости запрашивает обновление Python/proot-distro. Сообщение внутри Termux уточнено: отдельное приложение не нужно. 59 профильных JVM-тестов и40 Python-проверок прошли, APK проверен; Samsung ещё требует проверки. Сетевой сбой Void не объявляется устранённым. Подробности: `docs/fork/VALIDATION-v1.7.80-x11-test.md` (из docs/fork — `VALIDATION-v1.7.80-x11-test.md`). GitHub не публиковался.

## Текущая локальная сборка: v1.7.79-x11-test / code1294

Ошибки проверки/установки X11 включены в отчёт отдельно от запуска стола: этап, результат, коды и безопасные сетевые/пакетные категории. В Termux явно добавлен отсутствовавший xfdesktop; проверки всех X11 требуют компоненты фона, панели и настроек. Правило чистой установки закреплено в AGENTS.md. 82 JVM-теста и63 Python-проверки прошли; APK проверен. Чистый Samsung ещё требует проверки. Сетевой сбой Void пока не устранён. Подробности: `docs/fork/VALIDATION-v1.7.79-x11-test.md` (из docs/fork — `VALIDATION-v1.7.79-x11-test.md`). Публикации на GitHub нет.

## Текущая локальная сборка: v1.7.78-x11-test / code1293

Исправлены зависимости чистой установки Termux X11, границы команд/библиотек гостевых окружений и привязка необязательных исправлений к программам. Диагностика различает ошибки ABI и коды дочернего стола/guardian. 112 JVM-тестов и профильные Python-проверки прошли; release APK проверен. Чистый Samsung и GUI этой сборки ещё требуют проверки. APK сам не обновляет уже установленные пакеты. Подробности: `docs/fork/VALIDATION-v1.7.78-x11-test.md` (из docs/fork — `VALIDATION-v1.7.78-x11-test.md`). GitHub не публиковался.

## Текущая локальная сборка: v1.7.77-x11-test / code 1292

Английские закладки Phone storage / Downloads / Home для всех X11 и запрос доступа к памяти перед новым запуском без разрешения. «Пока нет» продолжает запуск, вопрос повторяется при следующем запуске. Клавиатура и компоненты не менялись. Подробные проверки: `docs/fork/VALIDATION-v1.7.77-x11-test.md` (из docs/fork — `VALIDATION-v1.7.77-x11-test.md`). Публикация на GitHub не выполнялась.

# Частная v1.7.74-x11-test / code1289

В сборке добавлен Void X11 с собственным сервисом/display104/портом8004,
XFCE, клавиатурой, звуком, программами XBPS и приватным Turnip+Zink (glibc/LLVM18).
Void не использует Ubuntu. VirGL доступен для первого теста в Termux и Ubuntu:
нативные пакеты virglrenderer-android/ANGLE/Mesa устанавливаются после согласия,
у каждого стола свой сервер/сокет в его группе процессов. Пока VirGL не проверен
на реальном рабочем столе телефона; CPU и Turnip+Zink остаются доступны.

Для программ всех подключённых X11 добавлено продолжение: Стоп → Продолжить.
Незавершённые запросы хранятся по окружению после выхода/ошибки; загрузка сама
не возобновляется. Успешное обновление индексов не повторяется при продолжении,
архивы остаются в штатном пакетном кэше. Проверки подписи/зависимостей/блокировки
сохраняются. Побайтовая докачка зависит от менеджера и сервера; отмена во время
настройки пакетов может потребовать штатного восстановления. Докачка окружений
уже существовала и этим изменением не заменяется.

openSUSE: Evince устанавливается вместе с evince-plugin-pdfdocument. Parole
получает приватную настоящую OpenH264-библиотеку Debian2.6.0; RPM-заглушка и
временное исключение недоступного codec-репозитория сохраняются. Приватный
ярлык меню задаёт библиотеку только Parole; пользовательские правки защищены.
Старой установке нужна повторная установка группы «Видео и аудио» и перезапуск
плеера. Старые пользовательские ярлыки могут обходить новый запуск.
Изолированный тест GStreamer декодировал синтетический H264 High Profile/B-frames
до EOS; реальную запись и GUI нужно проверить разработчику на новой сборке.

Частота X11 остаётся 60Гц. GL4ES, свой Turnip и 60/120Гц отложены. Обычный
openSUSE glmark2/GLX пока не исправлен; разработчик подтвердил glmark2-es2/EGL
на Adreno830/Zink, без финального Score. Агенты и их Ubuntu AI не меняются.
Пакет/сертификат сохраняются, code1288→1289. Автоочистки и миграции данных нет.
Тесты и окончательные APK-проверки — в VALIDATION-v1.7.74-x11-test.md.
Публикации на GitHub нет.

# Частная v1.7.73-x11-test / code1288

Повторное открытие рекомендуемых и установленных X11-программ использует
кэш выбранного окружения; главное меню не запускает каталог автоматически.
Ручная проверка обходит кэш, установка/удаление помечают прежние данные
устаревшими даже при частичном сбое. Кэш заменяется, без таймера/накопления.
Объём загрузки выводится после оставшегося времени в той же строке, только
при наличии измеренных байтов; счётчики файлов не превращаются в мегабайты.

В журнале openSUSE найден отказ доступа к mozilla-openh264 с сервера кодеков.
Программы устанавливаются с временным представлением репозиториев, в котором
исключён только codecs.opensuse.org/openh264. Системные .repo не меняются,
подписи/зависимости проверяются. Это не добавляет сам кодек OpenH264; покрытие
форматов зависит от штатных декодеров. Dry-run Parole/LibreOffice успешен;
реальную установку и запуск нужно проверить в новой сборке.

Пакет/сертификат прежние, code1287→1288; пользовательские данные не сбрасываются.
Рабочие столы, GPU/композитор, агенты и остановка процессов не менялись.
Импорт своего Turnip и преобразование драйверов в эту сборку не входят.
Публикации на GitHub нет.

# Частная v1.7.72-x11-test / code1287

В базовую установку openSUSE X11 добавлен xfce4-pulseaudio-plugin: штатная
панель больше не должна требовать удаления недостающего плагина громкости.
Для существующего стола проверка запуска предложит доустановить компонент.
Звук/PulseAudio и конфигурация панели пользователя не заменяются.

Прогресс Zypper сохраняет текущий пакет отдельно от последних8192символов
журнала; точки/спиннеры его не затирают. Разбираются Preloading/Retrieving и
Installing с порядковыми счётчиками. Активный пакет не считается завершённым
до [done]; прогресс не достигает100% до штатной проверки установки. Объём
загрузки не выводится из количества файлов. При отсутствии измерения процент
и время остаются неопределёнными. Меняются только поля существующего вида.

Пакет/подпись прежние, code1286→1287; форматы данных не меняются, сброса нет.
Рендеры, библиотеки, окружения агентов, способы остановки процессов не менялись.
Публикации нет.

[Проверки](docs/fork/VALIDATION-v1.7.72-x11-test.md).

# Частная v1.7.71-x11-test / code1286

Добавлен X11 в имеющуюся openSUSE Leap16.0: собственные процесс/каталог/display103,
XFCE, клавиатура, звук, Programs с Zypper/RPM и кэшем каталога, рекомендации,
установка/удаление, диагностика и выбор CPU/Turnip+Zink. Mesa устанавливается
в отдельную приватную папку. Для openSUSE используется проверенный Debian
bundle и приватная libsensors5; системные библиотеки не заменяются.
GPU прошёл внеэкранную EGL-проверку на Adreno830 с библиотеками openSUSE;
полный рабочий стол и glmark2 предстоит проверить разработчику на телефоне.
Engrampa используется только в openSUSE; у остальных остаётся Xarchiver.

Fedora: совместимость Firefox учитывает lib64 и org.mozilla.firefox.desktop;
сохраняется текущий MOZ_SHM_NO_SEALS без отключения песочницы/принудительного
CPU браузера. Пользовательские ярлыки/правки защищены. Журнал понимает два
точных имени ярлыков, не произвольный путь. Полное устранение падений вкладок
ещё не подтверждено. PolicyKit скрыт временным XDG override в текущем запуске
Fedora; пользовательские/системные автозапуски не редактируются. В базовые
компоненты Fedora добавлен xfce4-about для пункта «Об Xfce» над выходом.

Пакет/подпись прежние, code1285→1286. Формат данных не сбрасывается.
Новый Firefox-журнал сохраняет SHA файлов; старые варианты продолжают читаться.
Модели/агенты и установленные системы не мигрируются. Публикации нет.

[Проверки](docs/fork/VALIDATION-v1.7.71-x11-test.md).

# Частная v1.7.70-x11-test / code1285

Исправлена загрузка PNG/SVG в Fedora X11: Glycin пытался запустить bwrap,
из-за чего XFCE падал в libwnck с кодом134. Отдельный небольшой glibc-модуль
включает NotSandboxed только для Glycin в дереве рабочего стола Fedora,
до создания приватной D-Bus-сессии. Это снимает дополнительную песочницу
декодеров изображений; Android sandbox и права приложения не меняются.
Alpine/musl использует прежний отдельный модуль, остальные системы не меняются.
Код системных пакетов/библиотек не перезаписывается. Исправления68/69 сохранены.
Пакет/подпись прежние, code1284→1285; сброса данных нет.

[Проверки](docs/fork/VALIDATION-v1.7.70-x11-test.md).

# Частная v1.7.69-x11-test / code1284

Установка рабочего стола Fedora больше не зависит от ответа Cisco OpenH264:
репозиторий fedora-cisco-openh264 отключён только в двух командах DNF этой
установки (makecache/install). Системные настройки репозиториев не меняются.
Штатный resolver выбирает официальный noopenh264 для ABI-зависимости libheif.
Это заглушка, она не добавляет H.264-декодирование. XFCE/ввод/рендер остаются
в полном плане; проверки подписей и зависимостей не отключены.
Команды установки отдельных программ и общий пакетный менеджер не изменены.
Пакет/подпись прежние, code1283→1284, сброса данных нет.

[Проверки](docs/fork/VALIDATION-v1.7.69-x11-test.md).

# Частная v1.7.68-x11-test / code1283

Исправлен отказ ENVIRONMENT_DOWNLOAD_PermissionError в новой докачке67:
Android разрешает проход через /data к файлам приложения, но запрещает чтение
системного каталога. Предки теперь закрепляются O_PATH, приватный конечный
каталог открывается для чтения. O_NOFOLLOW и проверки SHA-256 сохранены.
Общий загрузчик исправлен для Ubuntu/Ubuntu AI/Debian/Alpine/Fedora/openSUSE/Void
и отдельного архива Arch. Установленные системы не заменяются.
Пакет/подпись прежние, code1282→1283; общего сброса данных нет.
Это исправление файлового доступа, отдельный Fedora/OpenH264403 не устранён.

[Проверки](docs/fork/VALIDATION-v1.7.68-x11-test.md).

# Частная v1.7.67-x11-test / code1282

Исправлены пустые несохранённые записи после неудачного запуска: профиль
регистрируется при создании терминала, старые пустые записи убираются при
первой сверке после открытия приложения. Истории/сохранённые пресеты сохраняются.
Такие записи больше не блокируют удаление окружения; штатные проверки живых
сессий и блокировки proot-distro не отключены.

Добавлена докачка окружений для проверенного Python API proot-distro5:
Стоп/сетевая ошибка сохраняют один .part на SHA-256, затем «Продолжить».
Все OCI-слои сначала проверяются до распаковки; готовые файлы переходят в штатный
кэш без отдельной постоянной копии. Arch ARM использует закреплённый SHA.
Стоп по-прежнему ждёт завершения процессов; незавершённый rootfs не заменяется
автоматически. Старый shell proot-distro4 использует прежнюю установку.

Сбой Fedora OpenH264403 от Cisco — отдельная проблема источника; обход проверки
подписей/обязательных зависимостей не добавлен. Изменения66 сохранены.
Пакет/подпись прежние, code1281→1282, общего сброса данных нет. Публикации нет.

[Проверки](docs/fork/VALIDATION-v1.7.67-x11-test.md).

# Частная v1.7.66-x11-test / code1281

В чатах агентов убран автоматический фокус ввода при переходе и смене сессии;
ручной тап и кнопка интерактивного ввода сохраняют своё действие.

Исправлена установка Fedora X11: xorg-x11-utils отсутствует в Fedora44;
в базовую установку/проверку добавлены актуальные xprop и xwininfo.
DNF использует свежий кэш без принудительного refresh при каждой попытке
и до6 параллельных загрузок; retries3/timeout30, подписи пакетов сохранены.

Неудачная установка X11 теперь остаётся ошибкой с причиной, без повторного
предложения установить. Карточки окружений при сбое показывают причину
ERROR/MISSING вместо CHECK:done. Незавершённые системы не удаляются/не заменяются.
Выбор рендера/запуск из65 сохранены. Пакет/подпись прежние, code1280→1281,
сброса данных нет; APK не публикуется.


При отмене под прогрессом показывается «Остановка установщика…», повторный
запуск заблокирован до фактического завершения. Supervisor даёт собственной
группе процессов время на очистку через SIGINT, затем TERM/KILL. Лидер группы
не reaped до завершения эскалации, чтобы не терять дочерние процессы/идентичность
PGID; посторонние группы не затрагиваются. Неполная система после принудительной
остановки всё ещё возможна, автоматического удаления пользовательских сред нет.

Подготовка удаления HOME/историй теперь пишет снимок в два приватных потоковых
файла, а не в один JSON со всеми записями: прежние пределы 60000 записей/8 MiB
не применяются к этому списку. Перед первым удалением сверяются все сохранённые
идентичности, сохраняются запрет перехода по ссылкам, блокировка окружения,
TTL плана и явное подтверждение. Временные списки удаляются после операции;
брошенные старше суток убираются при создании следующего плана.

[Проверки](docs/fork/VALIDATION-v1.7.66-x11-test.md).

# Частная v1.7.65-x11-test / code1280

Выбор CPU/Turnip + Zink теперь только сохраняет режим для выбранного
окружения: не проверяет/не устанавливает рабочий стол, не загружает драйвер
и не запускает X11. Выбирать можно и при отсутствующем рабочем столе;
при работающем столе смена по-прежнему блокируется.

«Переключиться на рабочий стол» берёт сохранённый режим, проверяет стол,
предлагает установить недостающее, затем подготавливает выбранный рендер
и запускает стол. Установка стола/драйвера остаётся в центральном экране
с прогрессом и отменой. Повторно выбирать рендер после установки не нужно.
Отказ/ошибка загрузки не переключают на другой режим и не запускают стол.
Предпочтение сохраняется для повторной попытки. Исправление видимых карточек
установки окружений из64 сохранено. Автоматического удаления/миграций нет.

[Проверки](docs/fork/VALIDATION-v1.7.65-x11-test.md).

# Частная v1.7.64-x11-test / code1279

Исправлено обновление прогресса установки окружений в списке: ListView
создавал временные строки при измерении и они перехватывали подписку
видимой карточки. Теперь обновления получают только присоединённые к окну
строки; при отсоединении/закрытии списка подписки удаляются. При возвращении
карточка сразу показывает текущее состояние установки.

Исправление отображения общее для всех карточек окружений, включая Ubuntu,
Ubuntu AI и Debian; механизм их установки/остановки и данные не менялись.
Fedora/X11 из63 сохранены. APK не публикуется на GitHub.

Выбор рендера теперь сначала проверяет рабочий стол выбранного окружения.
Если стол установлен, меняется только рендер без запуска. Если отсутствует,
после подтверждения экран в центре показывает установку стола, подготовку
уже выбранного рендера и запуск. Загрузка GPU-компонентов также отображается
там с прогрессом и отменой; проверка SHA-256 сохранена. Закрытые сессии
и устаревшие операции не могут поздним callback изменить рендер/запустить стол.

[Проверки](docs/fork/VALIDATION-v1.7.64-x11-test.md).

# Частная v1.7.63-x11-test / code1278

К существующей Fedora добавлен свой XFCE/X11: отдельные процесс/служба,
управляющий каталог, дисплей102 и канал UI. Новая Fedora/Ubuntu не создаётся.
Сохраняются общие проверенные ввод, звук, экран подготовки, меню программ,
Firefox PRoot/DNS HTTPS и диагностика сессии X11.

Установка/проверка через DNF/RPM с правильными Fedora-пакетами; каталог
читает кеш DNF, установленные программы сопоставляются с RPM-владельцами.
Удаление одной программы через rpm -e без --nodeps/force, без автоматического
удаления зависимостей; ядро рабочего стола/пакетный менеджер защищены.
Исправление прав ярлыков допускает только файлы, совпавшие с RPM-digest.

Добавлен отдельный pinned Turnip+Zink для Fedora44; по умолчанию CPU.
Для Fedora/GPU compositor=off, другие системы сохраняют проверенные режимы.
Первый запуск и программы Fedora на телефоне ещё требуют проверки.

[Проверки](docs/fork/VALIDATION-v1.7.63-x11-test.md).

# Частная v1.7.62-x11-test / code1277

Контрольная сборка по решению разработчика: v1.7.62-x11-test/code1277.
APK в Download; SHA-256: a253672b47b5e0392aeae6aef0ee47e4c5d6a18b1586e2e004514dfed5033b85.
Это зафиксированная частная сборка, не новая публикация GitHub.

Только кнопка отмены на центральном экране подготовки оформлена как плашка
«Сервер + Agent»: тёмная заливка, рамка1dp, скругление12dp, текст13sp,
высота области нажатия не меньше48dp и ripple. Сохранены подпись
«Отменить установку», положение под прогрессом и прежний обработчик.
Запуск/проверка/установка/отмена и другие кнопки не менялись.

[Проверки](docs/fork/VALIDATION-v1.7.62-x11-test.md).

# Частная v1.7.61-x11-test / code1276

Проверка и установка X11 после «Переключиться на рабочий стол» перенесены
в область рабочего стола. Каждый канал окружения имеет своё состояние и экран;
оформление одинаковое: курсивные Termux Job’s, +, X11, ниже этап и прогресс.
Если компонентов нет — подтверждение Да/Нет. Установка показывает процент,
объём/ETA при наличии измерений и последнюю строку вывода. Под прогрессом
«Отменить установку». После установки автоматически начинается запуск.

Возврат из терминального вида к активной установке не запускает повторную
проверку. Смена вкладки не отменяет установку; чужой результат не подменяет
состояние текущего окружения. Нет/отмена/ошибки не запускают стол автоматически.
Установка не закрывается тапом по фону. Обслуживание программ сохраняет прежний UI.

[Проверки](docs/fork/VALIDATION-v1.7.61-x11-test.md).

# Частная v1.7.60-x11-test / code1275

Для Debian/GPU и Alpine/GPU отключён композитинг XFWM, как в Ubuntu/GPU.
Termux/GPU и все CPU-режимы сохраняют compositor=on. В отчётах59 XFCE
дошёл до ready/idle с5 клиентами за4976мс (Debian) и5862мс (Alpine),
но разработчик сообщил, что рабочие столы не отображаются. Метаданные
готовности не доказывают отрисовку. Эффект отключения требует проверки.
Обёртка запуска/общий исправленный менеджер XFCE сохранены.

[Проверки](docs/fork/VALIDATION-v1.7.60-x11-test.md).

# Частная v1.7.59-x11-test / code1274

Добавлен Turnip + Zink для Debian13 и Alpine3.24, отдельные закреплённые
сборки glibc/musl от того же поставщика Mesa. GPU-кэш, пути библиотек,
проверка версии/зависимостей и подключение используют выбранное окружение.
Композитинг остаётся включённым для Debian/Alpine/Termux; выключен только
Ubuntu/GPU. Общий исправленный запуск XFCE из58 сохранён.

34 Python-теста и11 JVM-тестов прошли. Хеши обоих реальных архивов совпали
с GitHub release API; безопасная распаковка и полная проверка готового кэша
прошли внутри репозитория. Библиотечные зависимости найдены read-only
в установленных Debian/Alpine. Работа GPU на этих столах ещё требует
проверки разработчиком на телефоне. Системы/пакеты/сессии не изменялись.

[Проверки](docs/fork/VALIDATION-v1.7.59-x11-test.md).

# Частная v1.7.58-x11-test / code1273

Убрано дублирование xfwm4: оконный менеджер запускает и регистрирует сам XFCE.
Отдельный предварительный запуск мешал штатному failsafe-учёту клиентов и
согласуется с тремя наблюдаемыми ожиданиями регистрации по8с. Для сохранения
композитинга используется частная обёртка команды xfwm4 в runtime текущего стола:
UbuntuGPU/off, остальные режимы/on. Файлы xfconf и сохранённые сессии не меняются.
Готовность WM проверяет существующий x11-ready.py после запуска менеджером сессии.
Изменение общее для подключённых X11-сред; агентские сессии не затронуты.

Отчёт7855605907232132252 подтвердил доступную шину с3 зарегистрированными
клиентами: гипотеза неисправной авторизации UID для этого запуска не подтвердилась.
Проверка разработчиком на телефоне: Ubuntu/Turnip готов за4687мс вместо29808мс
(примерно6,36 раза быстрее);5 клиентов зарегистрированы, xfwm4 sm=1,
таймаута нет. Другие окружения и повторяемость ещё требуют проверки.
Шестерёнки меню сессий и расширенная диагностика из57 сохранены.

[Проверки](docs/fork/VALIDATION-v1.7.58-x11-test.md).

# Частная v1.7.57-x11-test / code1272

Общая диагностика X11 расширена на весь запуск XFCE: времена первого наблюдения
и возраст процессов, CPU/state/wait, происхождение native/proot, регистрация
клиентов менеджера сессии, доступность D-Bus EXTERNAL для гостевого/ядрового UID,
число/длительность/ошибки запросов готовности, категории ошибок SM/ICE/D-Bus.
Данные ограничены текущим приватным запуском и фиксированными ролями, без argv,
путей, содержимого окружения/полей и данных авторизации. Сбор заканчивается
при готовности или таймауте запуска; постоянного фонового мониторинга нет.

Меню сессии перенесено с долгого тапа на векторную шестерёнку справа внутри
верхней плашки и каждой строки раскрытого списка. Размеры плашек сохранены,
выбор сессии отделён от открытия её меню.

Применяется общим механизмом всех подключённых X11-сред. Для ещё не подключённых
обычных окружений сохранён раздел поддержки и общий путь будущего подключения.
Агенты/Ubuntu AI исключены. Запуск, настройки XFCE/GPU/Firefox и критерии
готовности не изменены. Точная первопричина 8-секундных ожиданий ещё проверяется.

[Проверки](docs/fork/VALIDATION-v1.7.57-x11-test.md).

# Частная v1.7.56-x11-test / code1271

В карточках Firefox PRoot и DNS HTTPS доступны во всех подключённых X11-средах:
Termux, Ubuntu, Debian и Alpine. Обычная Ubuntu больше не исключается старым
ограничением тестового окружения. Для APT-сред добавлены применение/отключение
профильного DNS через HTTPS и проверка состояния; системный DNS не меняется.
Устаревшая рекомендация Ubuntu X11 Test из окна программ удалена.
В диагностику запуска добавлены первые наблюдения панели/фона и состояния
менеджера сессии XFCE через существующую проверку, без нового опроса.

Настройки применяются по подтверждению только в выбранном окружении. Перед
переключением Firefox следует закрыть; перед DNS первым запуском создать профиль.
Готовность стола и задержка запуска этой правкой не меняются.

[Проверки](docs/fork/VALIDATION-v1.7.56-x11-test.md).

# Частная v1.7.55-x11-test / code1270

Для проверки чёрного экрана Ubuntu с Turnip + Zink композитинг XFWM отключён
только при сочетании target=ubuntu и renderer=turnip. В Ubuntu CPU он включён;
Termux и другие окружения сохраняют прежний запуск. Используется аргумент
процесса, сохранённые настройки XFCE не редактируются.
В отчёт X11 добавлена отметка JOBS_X11_COMPOSITOR=on/off с временем.
Результат на экране требует повторной проверки; причина чёрного экрана пока
не считается подтверждённой или исправленной.

[Проверки](docs/fork/VALIDATION-v1.7.55-x11-test.md).

# Частная v1.7.54-x11-test / code1269

В диагностику обычных сессий добавлена вкладка X11: состояние службы/подключения,
выбранный и запущенный рендерер, этапы запуска с относительным временем и категории
графических ошибок. Обычный/Подробный компактно слева под Сводкой; X11 справа.
На вкладке X11 режимы журнала серые; нижние кнопки и экспорт сохранены.
Отчёт включает X11 независимо от выбранной вкладки. Для неподключённых окружений
показан статус поддержки. Агентские сессии и Ubuntu AI исключены.

Чтение диагностики не запускает/останавливает стол и не создаёт канал службы.
Получение свежего состояния выполняется по открытию вкладки/экспорту, через уже
существующее подключение. Новый постоянный опрос не добавлен. В отчёт не попадает
сырой вывод программ: сохраняются ограниченные этапы и категории ошибок.

Причина чёрного экрана Ubuntu GPU ещё не установлена. Эта сборка добавляет
наблюдение для следующего воспроизведения; настройки GPU и композитора не меняет.

[Проверки](docs/fork/VALIDATION-v1.7.54-x11-test.md).

# Частная v1.7.53-x11-test / code1268

В меню «Терминал / Рабочий стол» добавлен выбор рендеринга: программный CPU
и экспериментальный Turnip + Zink для Termux и обычной Ubuntu 26.04.
Выбор отдельный для каждого окружения и доступен только при остановленном столе.
Компоненты загружаются по подтверждению в частный общий каталог приложения,
проверяются по SHA-256 и не заменяют системную Mesa. CPU остаётся по умолчанию.

На Adreno 830 успешно выполнены изолированные GPU-тесты отрисовки и чтения пикселя
для обоих вариантов через тот же установщик и настройки, которые вошли в APK.
Вывод окон через X11, glmark2 и одновременная работа двух столов требуют проверки
разработчиком. Совместимость других Snapdragon пока не подтверждена устройствами.
Ubuntu AI, агенты и механизм остановки столов этой правкой не меняются.

Подробности: [GPU](docs/fork/X11-GPU.md), [проверки](docs/fork/VALIDATION-v1.7.53-x11-test.md).

# Частная v1.7.52-x11-test / code1267

Ubuntu AI устанавливается независимо от наличия обычной Ubuntu при подтверждённой
подготовке выбранного агента. Миграция остаётся отдельным добровольным действием.
Занятость или ошибка входа в существующее окружение больше не считаются его
отсутствием: проверка завершается ошибкой, установка/сброс не запускаются.
После установки проверяется вход именно в выбранную среду; переключения на другую нет.
Ранее сохранённые пресеты и выбранные окружения сохраняются. Рабочие сессии не менялись.
В списке окружений установка отображается в своей карточке: Стоп, прогресс этапа
и оставшееся время. Закрытие списка/переход по вкладкам установку не отменяют.

Подробности: [проверки](docs/fork/VALIDATION-v1.7.52-x11-test.md).

# Частная v1.7.51-x11-test / code1266

Новые сессии и пресеты AI-агентов по умолчанию используют Ubuntu AI. Существующие
настройки сохраняются. При выбранной Ubuntu AI миграция недоступна и отображается
серой. В списке окружений добавлена подпись «Используется для AI-агентов».
Ubuntu X11 Test убрана из стандартного каталога; совместимость старых записей сохранена.

Разработчик подтвердил успешную миграцию и работу всех агентов. Текущая рабочая
среда проверена как Ubuntu AI. По явному поручению удалён оставшийся после ошибки
скрытый контейнер Ubuntu X11 Test; обычная Ubuntu и Ubuntu AI сохранены. Исправлено
удаление закрытых каталогов и недоступных для stat файлов внутри собственного
временного контейнера без перехода по символьным ссылкам.

Общий X11-рендерер и исправления сохранены. Рабочий стол и программы обычной Ubuntu
устанавливаются отдельно в её собственное окружение. Новую сборку и обычную Ubuntu
с X11 ещё нужно проверить на устройстве. GitHub-публикация не выполнялась.

[Проверки](docs/fork/VALIDATION-v1.7.51-x11-test.md).

# Частная v1.7.50 / code1265: служебные подключения сессий при переносе

Скриншот144851 уточнил реальный отказ приложения: открытие
rootfs/tmp/termux-fork-session-001136f4-58c5-44b1-94da-4690fd10ec7f, errno13.
Это точка --bind, созданная SessionShell, а не запрет сохранённого пресета.

В сканировании и копировании исключено содержимое точек /tmp/termux-fork-session-<UUID>
(строго UUID и только каталог), а также точных подключений подготовки, Codex, Gemini,
Claude и OpenCode из текущего кода. В копии остаются пустые каталоги. Подключения
создаются при запуске сессий; данные приложения, включая пресеты/историю, сохраняются.
Другие файлы /tmp и похожие имена переносятся. Исходные файлы и права не меняются. В подготовке переноса допускается удаление только пустого /workspace с правами 000 через rmdir, без рекурсии; непустой каталог остаётся.
Это не общее игнорирование ошибок доступа. Условия описаны перед подтверждением.

Реальный read-only обход /tmp прошёл все1935 записей без ошибок. Проверка полного
дерева и результаты сборки: [проверки](docs/fork/VALIDATION-v1.7.50-x11-test.md).
Пакет и сертификат сохранены; публикация на GitHub не выполняется.

---

# Частная v1.7.49 / code1264: точный путь отказа миграции

Скриншот143640 после v1.7.48 снова показывает SCAN_ACCESS_DENIED/13; разработчик
сообщил, что открыта только базовая вкладка Termux. Это не доказательство занятости
Ubuntu. Воспроизведение в текущей Ubuntu/proot отличается от процесса приложения.
Исправление новых исключений не устранило все отказы в приложении.

Сканер уже сохранял путь и действие отказа внутри исключения, но UI их терял.
Теперь локальное окно показывает относительный путь, операцию и версию проверки.
Содержимое файлов, ключей и исходный текст исключения не выводятся. Путь ограничен
по длине; управляющие символы отображаются как escape-последовательности. Сведения
не записываются в отдельный журнал и не отправляются в сеть. Исключения и проверки
копирования не ослаблены. Это диагностическая доработка, не подтверждение исправления
миграции. [Проверки](docs/fork/VALIDATION-v1.7.49-x11-test.md).

---

# Частная v1.7.48 / code1263: перенос Ubuntu без Android-точек подключения

При самостоятельном обходе воспроизведён errno13 на rootfs/apex, rootfs/data/app и
rootfs/data/dalvik-cache. Отдельная проверка показала пустые каталоги. proot-distro
использует эти пути для Android-подключений; их содержимое не является данными Ubuntu.
Это согласуется с ошибкой SCAN_ACCESS_DENIED на устройстве, но успешный перенос после
исправления ещё должен быть проверен разработчиком.

Служебные Android-точки подключения исключаются заранее по точному фиксированному
списку; в копии сохраняются пустые каталоги. Исключения не распространяются на похожие
имена в проектах или на обычные файлы. Исходные права и данные не меняются. Ошибки доступа
в других каталогах останавливают копирование; проекты, ключи и настройки не пропускаются.
Исключения описаны перед подтверждением. Отдельный выборочный режим здесь не добавлялся.

На Python Termux дополнительно выявлено отсутствие os.link и os.*xattr: сохранение
жёстких ссылок и расширенных атрибутов использует доступные fd-функции Bionic через
ctypes. Существующие файлы не перезаписываются, security.selinux не копируется.
В пояснении клавиатуры используется тот же векторный значок, что у кнопки интерактивного ввода (дальше от шестерёнки), а не текстовая имитация.

[Проверки](docs/fork/VALIDATION-v1.7.48-x11-test.md). Пакет/сертификат сохранены,
данные автоматически не очищаются. Публикация на GitHub не выполняется.

---

# Частная v1.7.47 / code1262: клавиатура X11 и уточнение отказа миграции

Во всех поддерживаемых сессиях в меню «Терминал / Рабочий стол» добавлена
«Автоматическая клавиатура» с галочкой справа и пояснением. По умолчанию включена.
Выбор сохраняется для каждого окружения. При отключении события фокуса/тапа поля X11
не вызывают клавиатуру; ручная кнопка «Интерактивный ввод» остаётся доступной.
Отключение не закрывает уже открытую вручную клавиатуру и не изменяет чаты агентов.

Отказ миграции со скриншота FILES_ACCESS_DENIED/13 ещё не локализован в Android-контексте.
Новая сборка различает доступ к блокировкам, процессам, структуре среды, сканированию
и копированию. Выводятся только фиксированный код этапа и errno, без имён личных файлов,
содержимого ключей или текста системного исключения. Права исходной среды не меняются.
Проверка из Ubuntu ограниченно обошла 28681 каталог за 25 секунд без отказов; это не
полная проверка дерева и не подтверждение прав процесса Android-приложения.

[Проверки](docs/fork/VALIDATION-v1.7.47-x11-test.md). Пакет и сертификат сохраняются,
данные не очищаются. Требуется повторная проверка миграции на устройстве.

---

# Частная v1.7.46 / code1261: подготовка миграции и причины ошибок

Исправлен обход пути к хранилищу: промежуточные каталоги открываются через O_PATH,
что не требует чтения их списка на Android; конечный каталог читается обычным fd.
O_NOFOLLOW и защита от подмены ссылками сохранены. Общая ошибка из скриншота не доказывала
отсутствие зависимостей: доступны Python3.14.8 и proot-distro5.9.0. Точный сбой в Android-
контексте ещё нужно подтвердить повторным запуском; выполнение из Ubuntu не доказывает
права Android-приложения.

При отсутствующих инструментах или модуле блокировок предлагается «Подготовить
автоматически»: после подтверждения проверяются Python/proot-distro в Termux,
устанавливаются только необходимые пакеты и повторяется операция. Готовые компоненты
не переустанавливаются. Ошибки доступа/пути/проверки процессов/файловой системы выводятся
отдельно с безопасным системным кодом; они не предлагают установку пакетов.

Удаление старой Ubuntu теперь не требует блокировки независимой запущенной Ubuntu AI.
Клонирование по-прежнему требует обе эксклюзивные блокировки, исходная Ubuntu должна
быть остановлена. Базовая оболочка Termux не мешает; никакие процессы автоматически
не останавливаются. [Проверки](docs/fork/VALIDATION-v1.7.46-x11-test.md).

---

# Частная v1.7.45 / code1260: Ubuntu AI и управление окружениями

В настройках всех четырёх агентов появились выбор Ubuntu / Ubuntu AI и кнопка
«Миграция среды агента» сразу под проверкой среды запуска. Проверка показывает
выбранную систему и её путь. Старые профили остаются на прежней среде. Новая
установка без обычной Ubuntu использует отдельную Ubuntu AI; существующую общую
Ubuntu можно явно скопировать со всеми агентами, проектами, инструментами,
историей, настройками и секретами. Внешние монтирования и память телефона не копируются.

Миграция требует proot-distro 5 с containers/блокировками и остановленной исходной
Ubuntu. Есть предварительный объём/свободное место, подтверждение, прогресс и отмена.
Копия публикуется под ubuntu-ai только после проверки; исходная система сохраняется.
После копирования выбирается переключение текущей формы или всех агентских пресетов.
CLI, история Codex, запросы OpenCode, остановка Gemini/Claude, файлы и пакеты используют
выбранную среду. В интерфейсе списка окружений есть корзинки для установленных систем,
включая импортированные. Работающие/связанные с пресетами системы удалить нельзя.

Проверки:90 JVM,31 Python и отдельные сценарии подготовки CLI. Это не проверка полной
миграции реальной Ubuntu на устройстве. Не устанавливать старую сборку после привязки
профилей к ubuntu-ai без предварительного возврата привязок на Ubuntu: старый читатель
библиотеки не знает этот вариант. Данные автоматически не очищаются.
[Устройство и ограничения](docs/fork/AGENT-ENVIRONMENTS.md),
[проверки](docs/fork/VALIDATION-v1.7.45-x11-test.md).

---

# Частная X11 v1.7.44 / code1259

Исправлена проверка пакетов X11: намерение dpkg «удалить» или «удерживать» больше не
считается отсутствием фактически настроенной программы. Незавершённая установка,
остатки конфигурации и состояние reinstreq по-прежнему не проходят проверку. Это
касается программ, списка установленных, проверки компонентов стола и Firefox.

В Termux удаление Evince после подтверждения включает установленную справку evince-help;
это явно указано в подтверждении. Другие обратные зависимости по-прежнему блокируют
удаление: нет принудительного удаления, autoremove или удаления общих библиотек.
После удаления проверяется фактическое отсутствие программы. Данные/схемы не меняются.
[Проверки](docs/fork/VALIDATION-v1.7.44-x11-test.md).

---

# Частная X11 v1.7.43 / code1258

На общей странице «Программы» остаются «Рекомендуемые», «Установленные программы» и
«Поиск и установка программ». Поиск, прокручиваемый каталог, страницы и установка
перенесены в третий раздел. В нём одна постоянная кнопка «Подготовить и обновить каталог»:
недостающие компоненты AppStream/Python/APT устанавливаются после подтверждения,
готовые не переустанавливаются; затем обновляются метаданные. Termux/Alpine обновляют
свои пакетные индексы без AppStream. Прогресс, фоновые задачи и отдельная кнопка программ
с контуром из1.7.42 сохранены.

Evince/Document Viewer в установленном Termux не запускался из-за отсутствующей
libandroid-complex-math.so, требуемой libsynctex. В набор PDF добавлена явная зависимость;
«Рекомендуемые» определяют её отсутствие и предлагают дополнить установленный Evince,
без удаления программы. Для других окружений состав PDF не менялся. Фактический запуск
после дополнения пакета должен проверить разработчик. Данные и зеркала не меняются.
[Проверки](docs/fork/VALIDATION-v1.7.43-x11-test.md).

---

# Частная X11 v1.7.42 / code1257

«Программы» — отдельная кнопка справа от «Терминал / Рабочий стол». В прежнем меню
остались переключение и остановка запущенного стола. Пока окно программ закрыто и
продолжается установка, по контуру кнопки идёт линия, тем же Drawable и с тем же
интервалом16мс, что у «Сервер + агент». Нажатие всегда открывает обычное меню программ.
Кадры обновляют только фон кнопки, а не содержимое терминала/чата. В скрытой вкладке
или остановленной Activity анимация выключена; установка остаётся в службе.

Для Alpine исправлено не только чтение изображения, но и сохранение PNG-миниатюры
через отдельный Glycin creator. На изолированной копии Tumbler до правки выдавал
ошибку Bubblewrap, после — Ready и проверенный PNG в кэше. На устройстве нужно
перезапустить стол Alpine. Известное ограничение изоляции Glycin из1.7.36 сохраняется.

После ошибки скачивания меню больше не заполняется сырым журналом: краткая причина
и «Подробнее» с полным доступным выводом. Скриншоты установки LibreOffice Termux
показывают сетевую ошибку зеркала HUST на MariaDB/PostgreSQL даже с повторами APT;
работа LibreOffice после установки этим не проверена. Репозитории не изменялись.
Данные и окружения не очищаются. [Проверки](docs/fork/VALIDATION-v1.7.42-x11-test.md).

---

# Частная X11 v1.7.41 / code1256

Меню X11 упрощено: переключение и программы; остановка видна только у запущенного
стола, в обоих видах. Переключение проверяет компоненты, запускает готовый стол или
предлагает установку. Рекомендуемые/установленные доступны во время обновления и
проверок; навигация не отменяет задачу. Сохранённые списки читаются без нового запуска.

Установка программ отображается в собственной карточке: «Стоп», полоса прогресса,
оставшееся время этапа, доступные измеренные объёмы и одна прокручиваемая строка
текущего файла/пакета. После установки — «Установлено» и корзина справа с подтверждением.
Закрытие окна и смена вкладки не отменяют служебную задачу. Для APT добавлены три
сетевых повтора и таймаут бездействия соединения30с; уже полученные файлы использует
сам APT. Зеркала и проверка подписи не меняются. Это не обещание увеличения пропускной
способности. Для APK Alpine автоматические повторы пока не добавлены.

Совместимость Alpine передаётся в её собственную D-Bus-сессию до запуска шины, чтобы
её получал и активируемый Tumbler. На устройстве нужно повторно проверить миниатюры
после перезапуска стола. Данные/окружения не очищаются; GPU/native X11 и агенты не менялись.
[Проверки](docs/fork/VALIDATION-v1.7.41-x11-test.md).

---

# Частная X11 v1.7.40 / code1255

Включены исправления сброса статусов программ, сообщений об ошибках и расположения профилей Firefox из1.7.39. Автопоказ дополнен проверкой завершения запуска самого XFCE через его приватную сессионную шину D-Bus: состояние Idle плюс проверенный оконный менеджер. Прежняя проверка типов видимых окон остаётся запасной, ручной показ — только при таймауте/сбое. Изменение требует проверки на устройстве; точная причина отказа прежней проверки пока не подтверждена. Данные/пакеты/окружения не сбрасываются.

---

# Частная X11 v1.7.39 / code1254

Исправлен сброс статусов всех программ после ошибки настройки Firefox. Известные состояния «Установлено»/«Установить» сохраняются при сбое DNS/совместимости и других действий, не меняющих пакеты. Ошибка установки/удаления по-прежнему требует повторной проверки. Firefox DNS HTTPS находит профили и в новой .config/mozilla/firefox, и в прежней .mozilla/firefox; переключатель остаётся необязательным. Сообщение об ошибке больше не предлагает устанавливать уже установленный стол. Данные и окружения не пересоздаются.

---

# Частная X11 v1.7.38 / code1253

Один рабочий стол на окружение; отдельные частные процессы, display и управление для Termux, Ubuntu, Ubuntu X11 Test, Debian и Alpine. Несколько вкладок одного окружения используют один стол. Закрытие последней подключённой вкладки останавливает его; явная остановка действует на выбранное окружение. Исправлен переключатель совместимости Firefox Alpine; для Alpine/Termux добавлен добровольный DNS HTTPS в Firefox с подтверждением и восстановлением настроек. Раннее подключение рендера под экраном запуска требует проверки на устройстве. Данные не сбрасываются, новые rootfs не создаются. Ограничение изоляции Glycin Alpine из1.7.36 сохранено.

---

<!-- Private X11 readiness and Alpine APK catalog fix 2026-10-21 -->
Текущая частная сборка: **v1.7.37-x11-test / code1252**.
Автоматический показ X11 использует типы и видимость окон, а не их заголовки.
После первого таймаута остаётся ручной показ, а ограниченная проверка продолжается
реже и открывает готовый стол автоматически. Постоянного наблюдателя нет.
Исправлен разбор законных имён APK Alpine с заглавными буквами, подчёркиванием и
одной буквой; каталог, рекомендуемые и установленные программы снова могут читаться.
Опасные аргументы/пути по-прежнему отклоняются. Данные не очищаются, пакеты не меняются.
Совместимость декодера Alpine из1.7.36 сохранена вместе с её ограничением изоляции.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.37-x11-test.md). Ниже — исторические этапы.

<!-- Private Alpine startup compatibility and startup UI 2026-10-21 -->
Текущая частная сборка: **v1.7.36-x11-test / code1251**.
Исправлена воспроизведённая ошибка загрузки PNG/SVG и встроенной иконки libwnck
в Alpine/PRoot. Малый модуль совместимости подключается только к дереву её стола;
системные библиотеки не заменяются. Он восстанавливает проверку существования файла
и запускает декодер Glycin без недоступной песочницы Bubblewrap. Это ослабляет
изоляцию обработки изображений в Alpine; Android и песочница браузера не отключаются.
Экран запуска всех столов: эмблема X11, заголовок, полоса реальных этапов и подпись.
Один стол одновременно. Данные не сбрасываются, новые окружения не создаются.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.36-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 lifecycle fix 2026-10-21 -->
Текущая частная сборка: **v1.7.35-x11-test / code1250**.
После завершения стола или ошибки запуска X11 освобождается автоматически.
«Остановить графический рабочий стол» оставляет базовую терминальную сессию открытой.
Остановка проверяет окружение; во время освобождения новые операции не запускаются.
Код завершения XFCE берётся из сообщения дочернего процесса, когда оно доступно,
а не из ожидаемого SIGKILL защитной группы. Один графический сервер одновременно;
независимые одновременные столы не реализованы. Данные и окружения не очищаются.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.35-x11-test.md). Ниже — исторические этапы.

<!-- Private Alpine/Termux X11 update 2026-10-21 -->
Текущая частная сборка: **v1.7.34-x11-test / code1249**.
Переход на X11 и существующая плашка управления добавлены для **Alpine и Termux**.
Alpine использует свой APK и musl; Termux запускает стол напрямую в своём PREFIX,
без PRoot/Ubuntu. Проверка, установка, XFCE, Fcitx5, программы и звуковой мост общие
по интерфейсу, но команды и пакеты соответствуют выбранной системе.
Диагностический наблюдатель и GTK-модуль убраны из запуска и APK для всех четырёх
систем. Сохранены необходимые сигналы готовности стола и фокуса ввода. Диагностики
в плашке нет. Архив исходников экспериментов остаётся в проекте вне ресурсов APK.
Работает один стол одновременно; отдельные одновременные серверы сюда не входят.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.34-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 media update 2026-10-21 -->
Текущая частная сборка: **v1.7.33-x11-test / code1248**.
В «Программы рабочего стола → Рекомендуемые» добавлен обычный пункт **Parole**:
описание, «Установить / Установлено», без отдельного меню плеера. Установка включает
декодеры GStreamer для распространённых видео и аудиофайлов; поддержка зависит от
кодека. Плеер не входит в базовую установку рабочего стола.
В базовую установку Debian/Ubuntu включены звуковые компоненты. После обновления
проверьте стол и установите недостающие компоненты; затем перезапустите стол.
Звуковой мост запускается вместе со столом через приватный UNIX-сокет с cookie,
без TCP/микрофона. Остановка принадлежит тому же владельцу, что и графический стол.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.33-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 certificate/progress update 2026-10-20 -->
Текущая частная сборка: **v1.7.32-x11-test / code1247**.
В базовую установку и проверку X11 Debian/Ubuntu добавлены `ca-certificates` и
проверка непустого хранилища доверенных сертификатов. Firefox также подготавливает
их перед HTTPS-репозиторием Mozilla; проверка TLS и подписей не отключается.
Окна установки рабочего стола и программ показывают после процентов полученный
объём и общий размер, если APT сообщает их. Это округлённые измерения APT, не
пересчёт числа файлов в байты. При распаковке объём скрыт, прогресс этапа остаётся.
Несколько одновременных столов и отдельные новые окружения в эту сборку не входят.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.32-x11-test.md). Ниже — исторические этапы.

<!-- Private Debian X11 update 2026-10-20 -->
Текущая частная сборка: **v1.7.31-x11-test / code1246**.
Рабочий стол XFCE добавлен в существующий **Debian**: откройте его обычную сессию,
затем «Рабочий стол X11 → Проверить» и подтвердите установку компонентов.
Новые окружения не создаются. Пакеты, каталог программ, настройки и совместимость
Firefox принадлежат выбранному Debian. Доступны те же переключение видов, ввод,
прогресс/отмена установки, рекомендуемые/установленные программы и исправление
прав ярлыков. Перед переходом между рабочими столами остановите предыдущий.
Разработчик подтвердил работу Firefox в Ubuntu через Applications и заново созданный
ярлык рабочего стола. Debian X11 ещё требует проверки на устройстве.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.31-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная сборка: **v1.7.30-x11-test / code1245**.
Совместимость PRoot — галочка в карточке Firefox под «Установить / Установлено»
в рекомендуемых и установленных. Нажатие открывает пояснение и подтверждение.
Закройте только Firefox: рабочий стол для переключения останавливать не нужно.
Новых значков, панельных ярлыков и команд не добавлено. Сохранён прежний запуск
через меню Firefox с подтверждённым обходом seals; обычный бинарник не изменяется.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.30-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная сборка: **v1.7.29-x11-test / code1244**.
Firefox → «Совместимость с PRoot» доступна и в рекомендуемых, и в установленных.
После установки браузера остановите рабочий стол, откройте этот пункт и подтвердите
включение. Пояснение описывает ослабление защиты общей памяти; параметр применяется
только при запуске Firefox через его ярлык. Там же можно отключить режим.
Временное удаление LXQt убрано; три известных остаточных ярлыка без резервных копий
убираются при следующем запуске Test-стола. Исправление прав ярлыков сохранено.
Обычный запуск пропускает лишнее декодирование фона, не меняя выбранные картинки.
[Проверки и ограничения](docs/fork/VALIDATION-v1.7.29-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная проверка: **v1.7.28-x11-test / code1243**.
Оставлен XFCE; выбор LXQt, диагностика и плашка Garcon убраны с панели.
«Программы рабочего стола → Установленные»: исправление прав ярлыков и временное
удаление LXQt с подтверждением после остановки стола. Общие библиотеки и настройки
сохраняются. В рекомендуемых — Firefox из подписанного репозитория Mozilla без
Snap; запуск в PRoot ещё проверяется. [Проверки и ограничения](docs/fork/VALIDATION-v1.7.28-x11-test.md).
Предыдущие сведения ниже относятся к историческим этапам.

## Private X11 v1.7.27

Failed Garcon runtime trial disabled and omitted. Explicit Test-only launcher-permission repair (confirmed, package checksum matching, original-mode backup) addresses a reproduced PRoot read/attribute feedback mechanism. Public stable remains v1.7.5. See docs/fork/VALIDATION-v1.7.27-x11-test.md.

## Private X11 v1.7.26

Test XFCE adds a reversible duplicate-attribute filter in a private Garcon4.20.0 overlay and snapshot diagnosis, without replacing installed libraries. LXQt adds GVFS/icon/SVG dependencies and uses manual keyboard activation. Stable public remains v1.7.5. See docs/fork/VALIDATION-v1.7.26-x11-test.md.

## Private X11 test · v1.7.25

Ubuntu X11 Test adds XFCE / LXQt · Openbox selection with independent component checks, shared installed programs and new metadata-only GIO file-monitor counters for XFCE/Garcon diagnosis. Stable public remains v1.7.5. See docs/fork/VALIDATION-v1.7.25-x11-test.md.

# Счётчики GTK · 1.7.24-x11-test

Для Ubuntu X11 Test добавлен небольшой диагностический модуль панели и рабочего стола: draw — рисование меню, add/remove — изменение пунктов, reload — запрос перезагрузки Garcon, enter/leave/motion — события указателя. Сбор счётчиков включается существующей кнопкой наблюдения; текст пунктов и содержимое событий не читаются. Модуль подключается только к новым процессам панели и рабочего стола тестового окружения, не заменяет GTK/Garcon и не меняет настройки. Это диагностика причины мерцания; исправление пока не заявляется. [Проверки и ограничения](docs/fork/VALIDATION-v1.7.24-x11-test.md). Ниже — предыдущие этапы.

# Наблюдение X11 и закрытие сессий · 1.7.23-x11-test

X11 запоминает сессию и конкретный запуск, из которого запущен рабочий стол. При её закрытии или завершении оболочки останавливаются графический стол и его программы. Закрытие другой сессии, смена вкладки и возврат в терминал стол не останавливают. Перед закрытием добавлено предупреждение о несохранённой работе. Наблюдение дополнено счётчиками изменений содержимого меню XDamage и движения указателя, без чтения изображений и текста ввода. Причина мерцания пока не установлена. Убрано наследование допклавиш и их подсветки между чатами агентов и обычным терминалом; Согласование прокрутки с новой высотой сообщений перенесено перед отрисовкой кадра для всех чатов агентов, без передачи фокуса из поля ввода. [Проверки и ограничения](docs/fork/VALIDATION-v1.7.23-x11-test.md). Ниже — предыдущие этапы.

# Наблюдение X11 · 1.7.22-x11-test

В «Диагностике X11» запись запускается и останавливается вручную, без прежнего ограничения в три минуты. Закрытие панели запись не останавливает; новый запуск заменяет текущую запись в памяти. Добавлены время событий, категории всплывающих окон, состояние кнопок мыши и отдельный последний снимок настроек фона. При потере поверхности отменяются удерживаемые кнопки и отложенное перетаскивание. Это диагностика, устранение мерцаний пока не подтверждено. [Проверки и ограничения](docs/fork/VALIDATION-v1.7.22-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.21-x11-test

Диагностика X11 переведена на компактную панель сессий: «Сводка»/«События», закреплённые действия копирования и экспорта в Download. При смене вкладки сохраняется соединение рендерера, пересоздаётся только Android-хост поверхности. Для фона используется постоянное имя X11-экрана screen; прежняя выбранная картинка переносится только при отсутствии собственного выбора у этого имени. [Проверки и ограничения](docs/fork/VALIDATION-v1.7.21-x11-test.md). Ниже — предыдущие этапы.

# Диагностика X11 · 1.7.20-x11-test

Исправлена полная пересборка верхней панели при смене состояния X11-ввода: обновляется только подсветка кнопки, название и навигация сохраняются. Добавлены ограниченный журнал событий всплывающих X11-окон/фокуса и метаданные Android-поверхности/клавиатуры. Это диагностика причины Applications, а не заявление об устранении мерцания. Проверяется декодирование выбранных фонов; только отсутствующий штатный фон заменяется встроенным нейтральным PNG. Выбранные пользовательские файлы сохраняются. В «Диагностике X11» есть обновление, копирование и экспорт в Download. [Проверки и ограничения](docs/fork/VALIDATION-v1.7.20-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.19-x11-test

Точечное сравнение с 1.7.18: XFWM4 запускается с композитингом (`--compositor=on`). Остальные механизмы рабочего стола сохранены. Проверяются Applications, чёрный фон и изменение размеров окон; устранение дефектов ещё не подтверждено. [Состав и проверки](docs/fork/VALIDATION-v1.7.19-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.18-x11-test

Во встроенном X11 отключён второй Android IME: ввод обслуживает поле приложения. Это точечная проверка причины мерцаний Applications, результат нужен на устройстве. Каталог и установленные программы сохраняются отдельно по окружениям между запусками; ручное обновление заменяет снимок, копии не накапливаются. [Состав и проверки](docs/fork/VALIDATION-v1.7.18-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.17-x11-test

«Установленные программы» с подтверждаемым удалением и защитой базовых компонентов. Журнал установки следует за последней строкой, сохраняя позицию при ручной прокрутке вверх; стрелки страниц каталога — векторные. Убраны лишние повторные запросы IME при уже полученном фокусе. Разработчик подтвердил Return в браузере. Мерцание Applications исчезает после открытия/закрытия ввода; причина пока не установлена, исправление не заявлено. [Состав и проверки](docs/fork/VALIDATION-v1.7.17-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.16-x11-test

«Рекомендуемые» и каталог AppStream с поиском, Return из текстовых IME-событий, Tumbler с Ristretto, базовые шрифты/значки и возврат к уже показанному столу. «Диагностика X11»; чёрные обои ещё исследуются. [Состав и проверки](docs/fork/VALIDATION-v1.7.16-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.15-x11-test

Программы рабочего стола с описаниями и проверкой установки, отдельная установка по кнопке, прогресс и оценка оставшегося времени этапа. Остановка скрыта до запуска. Масштаб и ввод сохраняют подтверждённое поведение 1.7.14. [Состав и проверки](docs/fork/VALIDATION-v1.7.15-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.14-x11-test

Стабильное разрешение рабочего стола при появлении клавиатуры; видимая часть обрезается без смены масштаба. Разработчик подтвердил автоматическую клавиатуру в 1.7.13. [Состав и проверки](docs/fork/VALIDATION-v1.7.14-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.13-x11-test

Возврат фокуса основному окну для автоматической клавиатуры и диагностика вызова. Разработчик подтвердил ручной ввод без задержек в 1.7.12. [Состав и проверки](docs/fork/VALIDATION-v1.7.13-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.12-x11-test

Автоматическое открытие через Fcitx использует наш ввод, ручная кнопка сохранена. Набор передаётся во время ввода, с защитой от повторения при закрытии. [Состав и проверки](docs/fork/VALIDATION-v1.7.12-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.11-x11-test

Клавиатура через окно основного приложения и индикатор запуска в области рабочего стола. [Состав и проверки](docs/fork/VALIDATION-v1.7.11-x11-test.md).

# Текущая проверка: 1.7.10-x11-test/code1225

Клавиатура X11: ручное переключение, снятие подсветки после закрытия и
пробные сигналы фокуса Fcitx5 для GTK3/Qt5. Добавлены явный запуск XFWM4 без
композитинга, детали запуска и возврат в плашку после проверки/установки.
[Состав и проверка1.7.10](docs/fork/VALIDATION-v1.7.10-x11-test.md).
Ниже — предыдущие записи. GitHub остаётся v1.7.5.

# Текущая проверка: 1.7.9-x11-test/code1224

Управление X11 пальцем как тачпадом вместо постоянно зажатой левой кнопки.
XFCE предыдущей1.7.8 запущен и подтверждён разработчиком. Подробности:
[проверка1.7.9](docs/fork/VALIDATION-v1.7.9-x11-test.md).
Ниже — предыдущие записи. GitHub остаётся v1.7.5.

# Текущая проверка: 1.7.8-x11-test/code1223

Исправлена проверка служебной папки X11 (`CONTROL_PATH`): сравнение файловой
идентичности вместо текста путей Android. Внутреннее меню X11 и окна установки
используют общую тему и карточки агентов. Состав и проверка:
[docs/fork/VALIDATION-v1.7.8-x11-test.md](docs/fork/VALIDATION-v1.7.8-x11-test.md).
Ниже — записи предыдущих сборок; GitHub остаётся v1.7.5.

# Текущая проверочная сборка: 1.7.7-x11-test

Рабочий стол в области выбранной сессии; явная Ubuntu X11 Test
(`jobs-x11-ubuntu`), проверка/подтверждённая установка XFCE с прогрессом,
возврат в терминал и отдельная остановка. Пакеты — APT выбранной Ubuntu;
терминал и рабочий стол не отрисовываются одновременно. Реальный XFCE этого
этапа ещё проверяется на устройстве; прочие дистрибутивы не перенаправляются.
Подробности: [docs/fork/VALIDATION-v1.7.7-x11-test.md](docs/fork/VALIDATION-v1.7.7-x11-test.md).
Стабильный GitHub остаётся v1.7.5. Ниже — предыдущие записи.

# Текущая проверка: 1.7.6-x11-test / code1221

Разработчик разрешил первый этап встроенного X11 и тесты. Стабильный1.7.5
сохранён в Download/Termux-Jobs-1.7.5-BACKUP с исходным code1220; увеличивать его
только при необходимости отката. Тестовый APK code1221, пакет/подпись прежние.
Плашка у локальных неагентных сессий на месте лимитов; без пункта шестерёнки.
Первый экран — private X11 Activity/service и реальное протокольное тестовое
окно; не XFCE, не замена TerminalView пока. Меню: «Вернуться в терминал» /
«Остановить графический рабочий стол». План/границы: docs/fork/X11-INTEGRATION.md.
Не устанавливать APK, не менять пользовательские среды/процессы, не публиковать.
Ниже история предыдущих задач.

---

# Текущий стабильный выпуск: v1.7.5 / code1220

Очистка хранилища, история пакетов, версии APK/DNF/Zypper и исправления
предыдущих сообщений/привязки Codex.1.7.5-test имеет тот же code1220: отдельное
повышение для тестировщика не требуется. Подпись/package/schema8 сохранены.
Релиз: docs/fork/RELEASE-v1.7.5.md; проверки: docs/fork/VALIDATION-v1.7.5.md.
Ниже датированные этапы проверок, их запрет публикации относился к тем задачам.

# Проверочная сборка 1.7.5-test / code1220

Исправление подгрузки предыдущих сообщений в Android-чате: очередь при занятом
polling, страницы Codex и сохранение прочитанной истории при автопроверке.
Сохраняется немедленное открытие из кэша/черновики.
Явный выбор нового чата защищён от статуса старого запуска; новый ID
автоматически привязывается только после появления файла истории. Пакет/подпись/schema8 прежние;
истории и авторизация не очищаются, рабочие процессы автоматически не меняются.
GitHub пока не публиковать.

# Проверочная сборка 1.7.4-test / code1219

Целые исходные и восстановленные Linux-окружения доступны для удаления через
штатный proot-distro с проверками занятости/идентичности. PREFIX Termux остаётся
runtime только для просмотра. Сканирование с inline кружком как в бэкапе.
Подпись/пакет/schema8 прежние; автоматически данные не удаляются.
GitHub пока не публиковать.

# Проверочная сборка 1.7.3-test / code1218

Очистка расширена до обзора HOME/PREFIX/окружений и данных приложения,
с отдельным выбором HOME, библиотек и историй оболочек. Системные деревья доступны
для просмотра, без разрушительного сброса runtime. Дубликат удаления
восстановленных окружений из бэкапа убран. Подпись/пакет/schema8 прежние,
автоматического удаления/миграции данных нет. GitHub пока не публиковать.

# Проверочная сборка 1.7.2-test / code1217

Очистка хранилища с обязательным согласием/блокировками и история пакетов.
Выбор версий из1.7.1-test сохранён. Пакет/сертификат/schema8 прежние,
сброса/миграции пользовательской библиотеки нет. GitHub не публиковать пока.

# Проверочная сборка 1.7.1-test / code1216

APK/DNF/Zypper: выбор точной доступной версии пакета. Void/Arch без изменений.
Пакет/сертификат/schema8 прежние; сброса и миграции данных нет.
GitHub пока не публиковать: APK для проверки разработчиком.

# Termux Job’s — сборка v1.7.0

Пакет com.termux, versionName1.7.0, versionCode1215, Android8+/ARM64.
Прежний сертификат выпуска, библиотекаschema8. Локальные ИИ удалены;
специальный TERMUX_PUBLIC_RELEASE больше не нужен и ничего не включает.
[Совместимость](docs/fork/RELEASE-v1.7.0.md),
[проверки](docs/fork/VALIDATION-v1.7.0.md),
[исследование удалённой функции](docs/fork/LOCAL-AI-RESEARCH.md).

Нужны существующие JDK11, Gradle7.2, SDK и ARM64 AAPT2/clang/ld.lld/NDK sysroot
для локального ARM64-хоста. scripts/build-env.sh задаёт локальные кеши и SDK
в .tools; не указывайте общий изменяемый SDK. Дополнительные компоненты держите
внутри проекта. На стандартном хосте применяются обычные Android build tools.

```sh
FORK_AAPT2=/path/to/arm64/aapt2 bash scripts/verify-java.sh
python3 -m unittest discover -s scripts/tests -p 'test_*.py'
FORK_AAPT2=/path/to/arm64/aapt2 \
FORK_NATIVE_SYSROOT=/path/to/ndk/sysroot \
TERMUX_APP_VERSION_NAME=1.7.5-test bash scripts/build-local-arm64.sh
```

Результат build/apk/Termux-Jobs-v1.7.2-test-arm64.apk. Скрипт использует
изолированный staging, проверенный bootstrap и финальный runtime-аудит.
Нужны локальные инструменты, описанные в scripts/build-env.sh/build-local-arm64.sh;
готовая установленная Termux/Ubuntu не заменяется. Для идентичной подписи
требуется прежний секретный ключ выпуска; независимая сборка с новым ключом
не обновит официальный APK форка. Ключ, кеши, SDK и APK не коммитятся.
Порядок публикации: docs/fork/RELEASING.md.

## Историческая справка

Текст ниже сохранён для истории. Номера версий, прежние публичные flags,
планы и промежуточные элементы UI не описывают v1.6.5. В частности, верхние
кружочки отменены, локальные ИИ удалены. Актуальны сведения выше и release notes.

---

# Termux Job’s

## Historical private v1.5.23-test — server transfers and session activity

В этой приватной сборке также добавлены:

- «В фон» для скачивания и отправки: сохраняется та же передача и окно,
  строки прогресса появляются в карточке сервера только при активных операциях.
  Нажатие возвращает прогресс/отмену; рамка «Сервер + агент» анимируется по часовой.
- Тонкая линия активности во всех терминальных сессиях, с постоянной высотой
  области и без пересоздания терминала при анимации.
- Вместо Toast при изменении заголовка другой сессии — кружочки над вкладками,
  краткий плавный текст события и окно других сессий по нажатию.
- В списке открытых AI Agents — векторный робот; у обычных сессий остаётся терминал.

A vector trash button follows Home in server Files. Tap it to select files/folders;
tap again with no selection to cancel that mode. With selected entries it opens a
scrollable permanent-deletion confirmation with Yes/No. Long-press selection is
shared with download/move; both action cards highlight while entries are selected.
Deletion uses existing pinned SSH/Python, without changing the installed agent.
Folders include their contents; links are unlinked without following targets.
Interrupted deletion may be partial and is never retried automatically.
Same package/signature, code1160; private APK delivery only, no GitHub publication.
[Changes](docs/fork/RELEASE-v1.5.23-test.md) · [Checks](docs/fork/VALIDATION-v1.5.23-test.md).


## Historical private v1.5.22-test — local AI removed

The preceding private update was **v1.5.22-test / code1159**, not a public release.
On-device AI (llama.cpp, LiteRT-LM, NPU runtime, local chats and presets) is removed.
The first launch removes only app-private `files/local-ai`, `cache/local-ai` and
local diagnostics preferences. This intentionally deletes downloaded local models,
local chat history and their settings; export anything needed before updating.
Old local-AI profiles are skipped without resetting the rest of the library.
Codex/Gemini/Claude/OpenCode, regular environments, servers, files, commands,
notes, backups and the shared history controls remain. No GitHub publication.
See [changes](docs/fork/RELEASE-v1.5.22-test.md) and
[checks](docs/fork/VALIDATION-v1.5.22-test.md).

**RU:** локальные ИИ полностью убраны. При первом запуске удаляются их модели,
локальные чаты и приватные настройки. Прочие данные приложения не сбрасываются.
Рабочая Ubuntu сохраняется. Отдельная `jobs-local-ai` удалена на устройстве
разработчика по его запросу; APK не удаляет произвольные proot-окружения.


## v1.5.0

Public code1137; private test code1138. com.termux, same signing identity, Android8+/ARM64, library v8 with optional fields. Local models disabled in public builds while testing continues. History controls, agent permissions and server multi-selection/download/move are available. No automatic reset or environment replacement. [Release](docs/fork/RELEASE-v1.5.0.md).


## v1.3.3 — Обязательное обновление

Исправлена ошибка зависимостей Guava, вызывавшая вылеты при очистке файлов и
обработке crash-log. Минимум Android8.0/API26; Android7 больше не поддерживается.
Прежние пакет и сертификат, code1086. Обновление поверх без очистки данных.
[Изменения](docs/fork/RELEASE-v1.3.3.md) · [Проверки](docs/fork/VALIDATION-v1.3.3.md).

## v1.3.2 — просмотр библиотеки и дополнение справочника

Звёздочка: полный просмотр, редактирование, вставка, удаление и «Назад» к списку.
129 карточек ошибок (+15 к1.3.0). com.termux, прежний сертификат, code1085;
форматы данных не меняются, сброс не требуется.
[Изменения](docs/fork/RELEASE-v1.3.2.md) · [Проверки](docs/fork/VALIDATION-v1.3.2.md).


## v1.3.0 — «Ошибки и решения» и удобный выбор файлов

В общих настройках появился офлайн-справочник **«Ошибки и решения»**:
**114 карточек RU/EN** с поиском, фильтрами по системе, способу запуска и категории.
Покрывает Termux и популярные Linux-окружения: пакеты, доступ к файлам, SSH,
запуск, Git, Python/Node.js и сборки. Есть объяснения, проверки, решения,
ограничения и ссылки. Включён опыт разработки самого форка и ошибки помощника;
нерешённые случаи отмечены отдельно. **База будет пополняться** — пока вместе
с обновлениями приложения. Фонового сканирования и автоисправлений нет.

Общий файловый менеджер позволяет выбирать SSH-ключи, отправлять до **50 файлов**
на сервер и выбирать папку скачивания: Termux, установленные proot-distro
окружения или память устройства. Для общей памяти проверяется и запрашивается
доступ. Быстрое **«Скачать в Download»** сохранено. Добавлено поле passphrase
для зашифрованного ключа с защищённым хранением и подтверждаемым импортом ключа.

Обновление через приложение поддерживается: com.termux, прежний сертификат,
code1083. Форматы данных не меняются; очистка данных и обновление агента не нужны.
Android7+/ARM64. [Полные изменения](docs/fork/RELEASE-v1.3.0.md) ·
[Проверки](docs/fork/VALIDATION-v1.3.0.md).


## v1.2.2 — обновлённая диагностика сессий и серверов

Сводка и события, обычный/подробный журнал, остановка/возобновление,
копирование и экспорт в Download. Диагностика сервера использует существующие
операции без дополнительных запросов. Галочка справа от адреса разрешает
опрос вне вкладки каждые 30 секунд, внутри — 10 секунд; в фоне приложения пауза.
Маленькие жёлтая/красная точки обозначают состояние опроса, не доступность сервера.
Пакет/подпись прежние, code1073, очистка данных и обновление агента не требуются.

[Изменения](docs/fork/RELEASE-v1.2.2.md) · [Проверки](docs/fork/VALIDATION-v1.2.2.md).

## Серверный раздел v1.2.0

Работа со своими Linux-серверами через pinned OpenSSH: отдельные профили,
опциональный Python-agent, метрики, проекты, файлы/Media3, SFTP, задачи/службы,
Cron/user-systemd. Старый SSH-вход сохранён. Профилиv1 получили необязательные
избранные пути и закреплённые команды, библиотекаv8 без миграции/сброса.
Media3 1.5.1 использует системные кодеки; лицензия в assets/media3-LICENSE.txt.
Целевой Android API28, min24, compile35, ARM64. Подробности: docs/fork/SERVERS.md.


Неофициальный форк [Termux](https://github.com/termux/termux-app), основанный на
теге `v0.118.3`, коммите `5b657c6adf4304e5198951ce815fe0205dcac29c`.
Ветка разработки — `fork/base`; `upstream` указывает на оригинал.
Текущий выпуск — `v1.3.3`, versionCode1086, Android8.0+/ARM64, com.termux.
Менеджер резервных копий: выбор компонентов,
прогресс, сжатие, защита окон. [BACKUP-RESTORE](docs/fork/BACKUP-RESTORE.md).
Ручное/автоматическое обновление объединено: [UPDATES](docs/fork/UPDATES.md).
Менеджер пакетов, текстовый редактор файлов, действия библиотек в просмотре и выбор категорий включены в релиз.
Оригинальные терминал, PTY/JNI, bootstrap, сервис и лицензии сохранены.

## AI Agents и диагностика v1.0.18

AI Agents: Codex/OpenCode/Claude Code/Gemini CLI. Интеграции экспериментальны;
реальные аккаунты/модели/история проверены не полностью, мерцание OpenCode открыто.
Подготовка компонентов в уже установленной Ubuntu, отдельные каталоги агентов;
авторизация Codex/Bridge не переносится и не подменяется. Диагностика и расширенный
журнал описаны в [AI-AGENTS](docs/fork/AI-AGENTS.md) и [USAGE](docs/fork/USAGE.md).
Пакет/сертификат прежние, библиотекаv8. Специальный резервный v1.0.4/code1100 выше
кода этого релиза1072 и обычное понижение не поддерживает.

## Интерфейс и команды

Навигация «Главная / Команды/Заметки / Терминал» находится сверху. Терминалы переключаются
вкладками. Главная содержит независимо прокручиваемые панели журнала и сохранённых
сессий. Чёрный фон, бирюзовые акценты и скруглённые панели.

Библиотека команд хранит имя, текст, категорию и избранное. Есть поиск и фильтр
категорий. Нажатие на карточку открывает полный текст; кнопка вставки переносит команду в выбранный терминал, не отправляя
её и не создавая новый. Если терминалов несколько, можно выбрать получателя.
Звёздочка у поля ввода предлагает команды и заметки с отдельным поиском и вставляет в текущую сессию. Библиотека содержит две прокручиваемые панели с общим поиском и отдельными фильтрами. При малой высоте окна панели сохраняют минимум, появляется общая прокрутка. Окно просмотра содержит
избранное, редактирование и удаление. Каталог команды при вставке не меняет
рабочий каталог активного терминала.

Журнал хранит последние 50 команд, отправленных кнопкой из поля ввода: новые
сверху, старые вытесняются. Есть время отправки, поиск и очистка с подтверждением.
Меню записи позволяет вставить текст, сохранить в библиотеку или скопировать.
Вставка в поле ввода не считается отправкой. Прямой интерактивный ввод не
попадает в этот журнал.

## Сессии

Обычная новая сессия не появляется среди сохранённых автоматически. Два пути
сохранения: «+» секции сохранённых на главной и долгий тап по вкладке терминала
→ «Добавить в пресеты запуска». Старые записи библиотеки сохраняются при обновлении.
Параметры: имя, рабочий каталог, до трёх команд запуска и среда выполнения.

Карточка сохранённой сессии запускает её либо открывает уже работающий терминал.
Троеточие карточки: закрыть сессию, настройки, удалить сессию. Долгий тап по
вкладке терминала: перезапустить, настройки, закрыть; для несохранённой вкладки
добавляется сохранение. Закрытие и перезапуск требуют подтверждения. Закрытие
останавливает процесс и убирает вкладку, но не удаляет сохранённую заготовку.
Удаление профиля и его истории — отдельное действие; работающий терминал сначала
нужно закрыть. Изменение имени применяется сразу, каталога и стартовых команд —
при следующем запуске. Сервис поддерживает до восьми терминалов.

Сохранённая сессия хранит параметры запуска, а не работающий процесс. После
перезапуска приложения можно заново запустить её команды. История Bash хранится
в отдельном HISTFILE профиля. Функций снимка вывода, истории снимков и
автоматического создания снимков нет.

Для среды Ubuntu нужен `proot-distro` с контейнером `ubuntu` и Bash. Приложение
входит в Ubuntu автоматически, каталог относится к гостевой системе. В режимах
Termux/Ubuntu команды стартуют после готовности Bash, последовательно в одной
оболочке, при ошибке следующие шаги отменяются. Интерактивная программа
задерживает следующие шаги до выхода. «Отменить команды запуска» отменяет
оставшиеся шаги, не прерывая текущий процесс; при новом запуске отмена сбрасывается.
В старом режиме «Как раньше» этот флаг не проверяется.

Библиотека: приватный `workspace-v1.json`, формат JSON v8. Читаются v1–v8;
неизвестный или повреждённый файл не перезаписывается. Параметры обычных сессий
хранятся для управления активными терминалами, но с `saved=false` не показаны
среди сохранённых. Старые записи без флага читаются как сохранённые.

## Ввод

Поле ввода многострочное; Enter переносит строку, отправка — отдельной кнопкой.
Вставка сохраняет переносы. Исходный механизм paste и bracketed paste сохранён.
По умолчанию касание вывода терминала не открывает клавиатуру. «Интерактивный
ввод» включает прямой ввод в TerminalView для редакторов, паролей и других
интерактивных программ; нижнее поле остаётся доступно. Настройка сохраняется.
Шрифт вывода и интервалы исходные; отдельной настройки интервалов нет.

## Сборка и проверка

Нужны JDK 11, SDK Platform 30, Build Tools 30.0.3, Gradle 7.2. Кеши и локальный
SDK находятся в `.tools/`. Установленное приложение и его данные не используются
как выход сборки. Для стандартного x86-хоста нужен NDK 22.1.7171670:

```sh
./scripts/check.sh
./scripts/build-debug.sh
```

На ARM64-хосте `scripts/build-local-arm64.sh` собирает обе JNI-библиотеки локально
из оригинального C/ASM под API 24. Ему нужны существующий Android NDK sysroot,
clang, ld.lld и aapt2 через переменные `FORK_NATIVE_SYSROOT`, `FORK_NATIVE_CLANG`,
`FORK_NATIVE_LLD`, `FORK_AAPT2`. Скрипт использует изолированную копию исходников,
официальный ARM64 bootstrap с проверкой SHA-256 и задачу `assembleRelease`.
Итог — `build/apk/Termux-Jobs-v1.0.18-arm64.apk`. Режим отладки отключён;
минификация отключена для сохранения проверенного кода.

Изолированные проверки Java/ресурсов: `scripts/verify-java.sh`. Проверяются
библиотека, навигация, сохранение сессий и эмулятор без запуска реальных
сервисов или установки APK. Подробности — [VALIDATION.md](docs/fork/VALIDATION.md).
GitHub Actions Fork CI выполняет тесты и создаёт отдельный debug artifact.

## Пакет, подпись и происхождение

Пакет остаётся `com.termux`: bootstrap и пакеты зависят от абсолютного PREFIX.
Это позволяет обновлять предыдущие APK Termux Job’s без удаления данных.
Нельзя установить поверх Termux с другой подписью или рядом с другим `com.termux`.

Release и локальные debug-сборки используют прежний ключ
`.signing/fork-debug.jks` (историческое имя). Сертификат не заменяется, чтобы
сохранялась совместимость обновлений. Ключ исключён из Git; для выпуска он
должен уже находиться локально. Скрипт создаёт новый ключ только для независимой
сборки; такой APK не обновит опубликованный. Сохраняйте ключ вне репозитория.
CI не имеет ключа выпуска, его artifact подписан иначе.

Исходники выпуска соответствуют тегу `v1.0.18`. Лицензия — [GPLv3](LICENSE.md).
[Авторство компонентов](docs/fork/ATTRIBUTION.md),
[карта исходников](docs/fork/ARCHITECTURE.md),
[описание выпуска](docs/fork/RELEASE-v1.0.md).

## Codex: запуск, история и управление

В окне новой сессии выберите «Среда запуска → Сессия Codex», затем Termux или Ubuntu, имя и рабочий каталог. В выбранной среде заранее должны быть установлены и авторизованы Codex CLI и Node.js; для Ubuntu нужны proot-distro и установленная Ubuntu. Авторизация и история берутся из выбранной среды, приложение их не копирует и не поставляет в APK.

Режимы доступа:

- **Полный доступ**: `danger-full-access`, подтверждения `never`.
- **В пределах проекта**: `workspace-write`, подтверждения `on-request`.
- **Только чтение**: `read-only`, подтверждения `on-request`.

Полный доступ относится к доступной Linux-среде и не даёт root Android. Поддержка ограниченного sandbox зависит от Codex и Android/proot; при отказе приложение не переключает режим на полный автоматически.

Для чата доступны **Новый чат**, **Выбрать чат** из истории и **Ввести ID чата**. Продолжение по ID возвращает историю конкретного диалога Codex. Автоматический «Последний» убран: старый профиль с таким вариантом нужно отредактировать. Закрытие вкладки не удаляет историю `.codex`; сохранённый профиль приложения хранит параметры запуска отдельно от истории диалога.

Под терминальными вкладками карточка показывает модель, поведение, состояние и доступные пятичасовой/недельный лимиты. Лимиты относятся к аккаунту, а не к отдельному чату. Статус считывается примерно раз в 30 секунд; «Обновить» запрашивает чтение сейчас. Отсутствующие или устаревшие поля отображаются как «нет данных». Чтение статуса не отправляет запрос модели и не расходует лимит диалоговых ответов. Доступность моделей и значения лимитов определяются сервисом Codex; приложение не увеличивает и не сбрасывает их.

Нажмите модель → выберите модель → выберите поведение → проверьте параметры → **Применить**. Список берётся из Codex, поэтому доступные варианты зависят от аккаунта и версии CLI. Приложение ждёт завершения текущего ответа, затем управляет штатным меню `/model` в терминале. Выбор применяется только к текущему запуску сессии через действие `s session`; незавершённый текст в терминале не заменяется. Успех подтверждается новым штатным сообщением Codex. Неизвестное меню, дополнительное предупреждение или таймаут требуют ручного завершения; отмена останавливает дальнейшие клавиши, уже открытое меню может остаться.

Интеграция проверена с Codex CLI **0.162.1**. Нужны поддержка `app-server --listen unix://…`, `--remote` и Node.js в выбранной среде. Произвольный ручной запуск Codex в обычной Bash-вкладке автоматически не подключает карточку. После обновления со старого выпуска запустите новый терминал типа Codex: уже работающий процесс не получает помощника автоматически. Чат можно продолжить по прежнему ID.

## Проекты, файлы и язык в v1.0.4

Включены все изменения после v0.2.17: проектные папки/привязка чатов, файловый браузер, стопка сессий, базовая Terminal, подтверждение другого каталога истории Codex, выравнивание иконок и взаимоисключающие фильтры. Формат библиотеки v8, необязательный codexSourceDirectory, прежние пакет и сертификат. Русский/английский интерфейс и глобус рядом с шестерёнкой, верхние плитки без подписей. Сборочные требования прежние.

Проверка переводов: `python3 scripts/check-localization.py`. Результаты и ограничения: [VALIDATION-v1.0.md](docs/fork/VALIDATION-v1.0.md). Исторические локальные отчёты v0.2.18–v0.2.20 сохранены отдельно.

GitHub CI использует Java 17 для подготовки Android SDK command-line tools и переключается на Java 11 перед Gradle. Локальная сборка проекта остаётся на JDK 11.
