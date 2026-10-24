# v2.0.3 — current X11 integration

Current supported desktops: **Termux, Ubuntu, Debian, Alpine, Fedora, openSUSE,
Void Linux and Arch Linux ARM**. Each target has its own private service,
display and runtime paths; only one desktop per target. The standalone Termux:X11
app is not required. The native core is unmodified upstream code, not a new Linux
kernel. Explicit source/provenance/license credits: [THIRD-PARTY.md](THIRD-PARTY.md).

Landscape rotation moves the selected desktop into a full-screen window. Rotate
back or use Android Back to restore the same desktop inside the app. The top-right
Interactive input | control remains available. All eight target services share
this presentation and logical-size adapter. The optional bundled style is now
independently supported in all eight current desktops, with per-target preferences
and HOME-scoped restoration. The style retains distribution menu plugins/defaults. Renderers, packages and process
ownership are unchanged. See [usage](USAGE.md) and the separate
[v2.0.3 validation](VALIDATION-v2.0.3.md) for the new rotation/IME checks.

All desktop installers target a clean bootstrap/rootfs and explicitly request
the complete required runtime set. Missing required binaries must fail readiness.
Optional program dependencies and compatibility fixes follow their application.
Automatic/manual keyboard, audio, storage permission shortcuts, program management
and renderer choices are described in [usage](USAGE.md) and [graphics](X11-GPU.md).

**Tested on RedMagic 10 Pro and Samsung S26 Ultra. Other devices require testing.**
These reports do not establish every renderer/program/clean-install combination
on both phones. Full final Ubuntu GPU INFO needs a device recheck; Mali/Dimensity
and long-duration stability remain unconfirmed. See [validation](VALIDATION-v2.0.0.md).

v2.0.1 separates selected-renderer validation from desktop component readiness.
A missing optional driver is repaired after consent; it is not a request to
reinstall the whole desktop. Non-composited GPU presentation uses the native
clipped-copy fallback; see [graphics](X11-GPU.md). Native code and service/process
ownership are unchanged.

Older version-specific sections below document the development history.

## Частная v1.7.86: корень Arch / цикл Thunar

Скриншоты16:28:51 и16:29:04: пустое окно Thunar и предупреждение XFWM4
«не отвечает». Отчёт2722871493744434857: X11running/desktopReadytrue,
Turnipconnected, desktop-ready6091мс. Это не повтор падения Glycin134.
Read-only анализ /proc выбранного Thunar показал CPU-цикл main/gmain,
повтор faccessat/fstatat/statx по `/`. Наблюдение rootfs:11045 событий
IN_ATTRIB|IN_ISDIR за2.5с. Права fstat чередуются0555/0755, PRoot5.1.107.96
временно добавляет владельцу доступ и восстанавливает его, вызывая
повторные обновления Thunar. Снимки /proc ограничены выбранной сессией;
содержимое документов/ключей и текст ввода не считывались.

У X11 Arch добавлена проверка owner-rwx корня до guest-проверки и запуска.
Если их нет, предлагается прежняя подтверждаемая установка с явным
пояснением исправления прав. Внутри этого согласованного install native
Termux Python открывает только корневой каталог Arch через O_NOFOLLOW,
проверяет владельца и fchmod добавляет только недостающие owner700;
групповые/прочие/специальные биты сохраняются, потом повторная проверка.
Делается до и после пакетной транзакции. Контент не обходится рекурсивно,
соседние окружения не меняются; check/run только проверяют. Наблюдение
файлов/GIO/inotify не отключается. Если native Python отсутствует, он
устанавливается лишь после согласия на install. Права ярлыков отдельно.

На временном rootfs0555 воспроизведены IN_ATTRIB от чтений; после repair
60 повторных проверок дали0 новых таких событий. Живая система/сессии
при разработке не исправлялись и не останавливались. Полное устранение
зависания Thunar на телефоне ждёт следующей проверки разработчиком.

## Частная v1.7.85: Arch Glycin / значки XFCE

v1.7.84: разработчик подтвердил успешную установку Arch X11, после неё
XFCE завершился134 через~26с. Отчёт7945539684196378061: компонентыsuccess,
rendererConnected/hostAttachedtrue, Turnip-Zink выбран и подключён.
Скриншот16:17:13 показывает failed load image-missing.svg, glycin-svg,
bwrap status1, GTK ensure_surface_for_gicon и Wnck default_icon_at_size abort.
ICE disconnect в конце — следствие завершения, отдельной причиной не объявляется.

Для Arch добавлена ранее используемая в Fedora совместимость Glycin2:
приватный preload перед dbus-run-session и в дереве процессов рабочего стола.
Она выбирает NotSandboxed для Glycin loader/creator: декодеры изображений
в этой сессии не используют bubblewrap, который не работает внутри PRoot.
Это уменьшает изоляцию декодирования изображений; права Android остаются
действующими. Установленные библиотеки/ярлыки/конфиги не заменяются.
Остальные targets сохраняют прежние способы совместимости.

Реальный readelf установленного Arch: libglycin-2.so.0 экспортирует оба
конструктора и sandbox-selector; GdkPixbuf2.44.8 обращается к gly_loader_new
и gly_creator_new. Upstream io-glycin-utils.c2.44.8 не сбрасывает выбранный
режим обратно в Auto. Активная совместимость строится на той же ABI2.
Внутреннее имя asset x11-fedora-glycin.so сохранено, но копия и preload
привязаны к отдельному каталогу X11 Arch; Fedora rootfs не используется.

Добавлены обезличенные категории GLYCIN_IMAGE_SANDBOX_FAILED и
DESKTOP_ICON_LOAD_FAILED. Следующая проверка — запуск рабочего стола Arch
на телефоне, значки/меню и затем GPU. Успех GUI этой сборки ещё не подтверждён.

## Частная v1.7.84: подробности ошибки установки Arch

В v1.7.83 разработчик сообщил обрыве скачивания libcups при установке X11
Arch. Диагностика подтвердила downloading/child exit1; причина Pacman
`failed retrieving file` не сохранялась из-за фильтра download/fetch.
Она теперь включается в ограниченный обезличенный сборщик, а категории
помечают retrieving/download-library как PACKAGE_DOWNLOAD_FAILED.

После ошибки в центральной области доступны «Подробнее» и нажатие
на строку: отдельное окно показывает сохранённый хвост установщика
(до8KiB), переносит длинные строки, поддерживает прокрутку, выделение
и явное копирование. Автоматического открытия или копирования нет.
Экспорт диагностики сохраняет обезличенные transfer-ошибки, а не raw-хвост.
Компоненты установки, сеть/IPv4/IPv6, rootfs и драйверы не меняются.

Изолированный curl из rootfs Arch с его loader/libs скачал проблемный
libcups целиком с HTTP200 и совпадающим SHA256: автоматический выбор
4.09с и IPv4 3.47с. Это не полноценный вход в Arch: часть системных путей
остаётся текущей Ubuntu, поэтому не доказывает работу всего Pacman.
Причина прежнего обрыва пока не определена; принудительный IPv4 не добавлен.
Следующий шаг: повторить установку, при отказе открыть «Подробнее» или
сохранить свежую диагностику X11. Работа UI на телефоне этой сборки ещё
не подтверждена; автоматические проверки описаны в validation.

## Частная v1.7.83: Arch Linux X11

Arch Linux ARM подключён к собственному процессу `:jobs_x11_archlinux`,
экрану `:105` и каталогу `workspace-x11-archlinux`. Используется выбранный
rootfs `archlinux`; Ubuntu и окружение AI Agents не используются.
Доступны CPU, Turnip + Zink и экспериментальный VirGL. Композитинг XFWM4
для обоих GPU-режимов Arch отключён. Изменение режима — после остановки стола.

Программы используют Pacman и репозитории этого Arch. Установка XFCE,
программ и подготовка каталога выполняют `pacman -Syu --needed` внутри
выбранного окружения с предупреждением в подтверждении: обновление rolling
системы требуется для согласованности библиотек. Повторная установка использует
кэш Pacman; подготовленный повтор использует `-Su`. Подписи и проверка
зависимостей сохраняются. Не применяются `--nodeps`, `--overwrite`,
`--noscriptlet` или очистка кэша. Ошибка не удаляет db.lck автоматически.
Удаление `pacman -R` не включает рекурсивное удаление зависимостей.

Полный обязательный набор базовых компонентов запрашивается явно, включая
xfdesktop, панель, xfconf, Fcitx5 GTK/Qt, D-Bus, Python, сертификаты, шрифты,
значки, libpulse, GStreamer, Mesa/Vulkan и совместимый LLVM22.
Проверка отвергает отсутствие обязательных исполняемых файлов.
Рекомендуемые программы устанавливаются отдельно: Mousepad, Xarchiver,
Ristretto/Tumbler, Firefox, LibreOffice Fresh, Evince и Parole/кодеки.

Это подготовка к проверке на устройстве, не подтверждение чистой установки
или GUI/GPU Arch. Разработчик подтвердил работу рабочего стола и Turnip + Zink
Void на v1.7.82; это отдельный проверенный сценарий.

## Частная v1.7.71: openSUSE

Добавлен отдельный сервис OpenSuse (:jobs_x11_opensuse), display103/порт8003,
каталог workspace-x11-opensuse. Rootfs opensuse используется напрямую; агенты
и другие X11 процессы не перенаправляются. Общий lifecycle владельцев/отмены
и диагностика сохранены. Fedora временно скрывает PolicyKit через runtime XDG
override; системные и пользовательские .desktop не заменяются.

<!-- Private X11 v1.7.44 -->

X11 package checks use actual configured state independently of dpkg's requested action
(install/hold/deinstall/purge). Broken or partial states remain incomplete. In Termux,
Evince's removal confirmation explicitly includes installed evince-help; all other
reverse dependencies still prevent removal. Close the program before removal. Removing
an ordinary application does not require stopping the entire desktop.

Normal Ubuntu shares its packages with AI agents currently using that same rootfs.
X11 uses a private session bus/display, not a separate package database. Installing
programs can add/update shared dependencies. A future ubuntu-ai migration requires
separate preparation, credentials/history/project handling and validation before cleanup;
this private fix does not migrate or delete environments.

# Private1.7.40: XFCE completion signal

Startup helper queries org.xfce.Session.Manager.GetState on /org/xfce/SessionManager, destination org.xfce.SessionManager, inside the desktop-owned dbus-run-session. Idle=1 means startup completed per upstream D-Bus API and xfsm_manager_signal_startup_done. Requires self-consistent EWMH window manager as well; unavailable/starting/shutdown/invalid replies do not trigger ready. Existing property/viewability checks remain fallback. Queries and overall lifetime remain bounded; helper exits after success. No permanently running readiness observer. READY is newline-framed after a source marker; no activation, mutations, titles or input logging. Official source: https://github.com/xfce-mirror/xfce4-session/blob/master/xfce4-session/xfsm-manager-dbus.xml and xfsm-manager.c. This corrects a reliance on window heuristics; the actual device-specific failed condition has not been captured.

---

# Private X11 v1.7.38: environment service slots

Termux preserves :jobs_x11/display97/workspace-x11; Ubuntu, Test Ubuntu, Debian and Alpine receive fixed separate processes and displays98..101, guard ports7998..8001 and control directories. No profile-supplied process names/PIDs/displays. Native libXlorie remains shared APK code, loaded separately in each private process. One channel/server per environment; only selected channel owns a visible Android surface. Guardians own each desktop/audio process tree; shutdown kills only its own dedicated VM after exact process-name verification. Distinct XDG runtime directories and auth files avoid reuse. Concurrent environments consume additional memory; multiple tabs of one environment reuse its desktop. Rootfs/programs are retained. Older implementation notes below are historical.

---

# X11 follow-up · private 1.7.21

Diagnostics now reuse DiagnosticsPanel, with the same fixed-height body as session
reports, Overview/Events, bounded scrolling and pinned export/copy actions. Optional
Source hooks default to existing session/server behavior. X11 hides an unsupported
pause action, reports its own bounded log capacity text and requests shared-storage
access before Download export. The full report remains chronological; event previews
are recent first. Backdrop startup results are retained separately (4 KiB)
from the ordinary 8 KiB stderr tail, so later menu events do not evict them.
The metadata parser bounds incomplete lines to 1 KiB and stores only BACKDROP
markers; it neither modifies focus nor records typed input. Copy path uses the existing private atomic report writer. No new
polling loop besides the existing panel's 2-second refresh is added.

The old dialog combined an unbounded Android message panel with a custom actions
view; device evidence showed that the message displaced both custom export and
standard action buttons. The replacement uses the already bounded session layout.

The pinned upstream getXConnection queues registration of a new global conn_fd, while
an old handleLorieEvents X_NOTIFY_ERROR clears conn_fd and registered buffers without
checking whether its fd is still current. Replacing renderer/socket pairs on each
Android tab switch therefore permits an old/new cleanup race. The new code retains
one LorieView/native renderer and connection per X11 service/display while releasing
only the SurfaceControlViewHost on detach/token death. A new host reuses that renderer
and its connection. Detached rendering is suspended with a null native window;
gestures are cancelled. Empty/stale attachment input is rejected. Explicit service
shutdown still releases the renderer exactly once, and standalone LorieViews keep
their existing detach/release lifecycle. No native binary or process kill path changes.
Actual SurfaceControlViewHost reattachment and visual recovery require device checks.

The native server initially names its RandR output screen. Previously the first
renderer resize changed it to Termux Jobs X11, while XFCE backdrop settings depended
on monitor identity. This is now kept at screen. At fresh session launch the helper
restores a valid custom image from monitorTermuxJobsX11 to monitorscreen only if the
canonical image is absent or the built-in/fallback default. It retains existing
custom choices, even unreadable ones, and legacy settings; style/cycling are copied
only into missing destination settings. The two identities are not renamed/deleted.
Success of custom wallpaper rendering and its relationship to menu flicker remain
unverified until the developer tests this APK.

# X11 diagnostics · private 1.7.20

A startup-owned, 180-second read-only Xlib observer records root child lifecycle counters and sampled keyboard focus. Override-redirect windows are classified as popups, which include menus and tooltips. New/destroyed XIDs distinguish X11 window recreation from remapping; internal GTK widget recreation and pointer-only grabs are not proven by this recorder. Sampling is up to 250 ms while a popup exists, 1 second otherwise; rapid focus transitions can be missed. No input events, window titles, image data, authority or clipboard data are requested. A bounded 2,048-event batch and 2,048-child scan, 4,096 recent XIDs and 8 recent transitions prevent history growth. Output is coalesced to at most once per second plus start/finish. No permanent daemon, new package, listener, automatic restart or changed process shutdown is introduced.

The observer and backdrop helper join the existing pipe-owned desktop guardian group. Missing libX11/display is a fixed diagnostic error and does not abort XFCE. Backdrop helper uses xfconf in the fresh private session; only missing built-in defaults in this adapter's four workspace properties are assigned the shipped PNG. Existing custom paths, including unreadable ones, are retained. A failed listed-property read or failed channel listing does not trigger replacement. GdkPixbuf verifies supported image decoding at 64px, bounded to 32 MiB/40 million pixels; reports never include selected paths. This is a startup snapshot, not proof of the final visible root surface.

RemoteDesktopView records at most 24 metadata callbacks with monotonic offsets and counters: surface attach/destroy, geometry, keyboard request and Android window focus. Recording schedules no timers or invalidation. Current IME visibility and editor/window focus accompany these events. Opening the Android diagnostic dialog itself changes focus, which must not be confused with an earlier menu defect. The X11 keyboard callback now changes the existing header button only; it does not detach the title/navigation or rebuild the header.

The diagnostics dialog offers Refresh, Copy and To Download. Export requests the existing shared-storage access flow, takes a current report snapshot, writes a unique UTF-8 .txt off the UI thread and deletes an incomplete file after failure. Raw pre-existing XFCE stderr can contain paths; the metadata observer's privacy guarantee does not sanitize those existing log lines. The existing bounded service log remains the export source, so old lines may be evicted. No automatic upload occurs.

# Проверка X11 · 1.7.19-x11-test

Точечное сравнение с 1.7.18: XFWM4 запускается с композитингом (`--compositor=on`). Остальные механизмы рабочего стола сохранены. Проверяются Applications, чёрный фон и изменение размеров окон; устранение дефектов ещё не подтверждено. [Состав и проверки](VALIDATION-v1.7.19-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.18-x11-test

Во встроенном X11 отключён второй Android IME: ввод обслуживает поле приложения. Это точечная проверка причины мерцаний Applications, результат нужен на устройстве. Каталог и установленные программы сохраняются отдельно по окружениям между запусками; ручное обновление заменяет снимок, копии не накапливаются. [Состав и проверки](VALIDATION-v1.7.18-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.17-x11-test

«Установленные программы» с подтверждаемым удалением и защитой базовых компонентов. Журнал установки следует за последней строкой, сохраняя позицию при ручной прокрутке вверх; стрелки страниц каталога — векторные. Убраны лишние повторные запросы IME при уже полученном фокусе. Разработчик подтвердил Return в браузере. Мерцание Applications исчезает после открытия/закрытия ввода; причина пока не установлена, исправление не заявлено. [Состав и проверки](VALIDATION-v1.7.17-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.16-x11-test

«Рекомендуемые» и каталог AppStream с поиском, Return из текстовых IME-событий, Tumbler с Ristretto, базовые шрифты/значки и возврат к уже показанному столу. «Диагностика X11»; чёрные обои ещё исследуются. [Состав и проверки](VALIDATION-v1.7.16-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.15-x11-test

Программы рабочего стола с описаниями и проверкой установки, отдельная установка по кнопке, прогресс и оценка оставшегося времени этапа. Остановка скрыта до запуска. Масштаб и ввод сохраняют подтверждённое поведение 1.7.14. [Состав и проверки](VALIDATION-v1.7.15-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.14-x11-test

Стабильное разрешение рабочего стола при появлении клавиатуры; видимая часть обрезается без смены масштаба. Разработчик подтвердил автоматическую клавиатуру в 1.7.13. [Состав и проверки](VALIDATION-v1.7.14-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.13-x11-test

Возврат фокуса основному окну для автоматической клавиатуры и диагностика вызова. Разработчик подтвердил ручной ввод без задержек в 1.7.12. [Состав и проверки](VALIDATION-v1.7.13-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.12-x11-test

Автоматическое открытие через Fcitx использует наш ввод, ручная кнопка сохранена. Набор передаётся во время ввода, с защитой от повторения при закрытии. [Состав и проверки](VALIDATION-v1.7.12-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.11-x11-test

Клавиатура через окно основного приложения и индикатор запуска в области рабочего стола. [Состав и проверки](VALIDATION-v1.7.11-x11-test.md).

# Текущая проверка 1.7.10-x11-test

Клавиатура через фокус Fcitx5, проверка запуска XFWM4 и возврат из установки в плашку X11: [состав и проверки](VALIDATION-v1.7.10-x11-test.md). Работа клавиатуры и рамок окон требует проверки на устройстве.
Ниже сохранены предыдущие этапы.

# Историческая проверка 1.7.9-x11-test

XFCE запущен в1.7.8; исправление жестов пальцем: [проверка1.7.9](VALIDATION-v1.7.9-x11-test.md).
Ниже — предыдущие этапы.

# Текущая проверка 1.7.8-x11-test

Исправление CONTROL_PATH и меню в стиле агентов: [проверка 1.7.8](VALIDATION-v1.7.8-x11-test.md).
Ниже — описание предыдущих этапов.

# Встроенный X11 · текущая проверка 1.7.7-x11-test

Текущий этап: встроенный вид и настоящий XFCE в явно выбранной Ubuntu.
[Состав, границы и проверка 1.7.7](VALIDATION-v1.7.7-x11-test.md).
Первую 1.7.6 разработчик проверил: графика, ввод, возврат и остановка работают.
Ниже сохранено историческое описание первого этапа, оно не описывает новый UI.

## Исторический этап 1.7.6-x11-test

Статус: интеграция и изолированные проверки; запуск на устройстве ещё требуется.
На GitHub эта сборка не публикуется. Стабильный выпуск остаётся1.7.5.

## Согласованные этапы

1. Встроить X11 в один APK: графический экран, касания/клавиатура, управление
   временем жизни и проверка влияния на обычные сессии. Без второго X11 APK.
2. Подключить выбранную Ubuntu и XFCE: проверка/подтверждённая установка
   компонентов, общие tmp/DISPLAY, корректный запуск. Не использовать все среды
   как одну и не вмешиваться в уже работающую сессию агента.
3. Сессии/пресеты: терминал либо рабочий стол с сохранением настроек.
4. Итоговый рабочий стол заменяет область терминала той же сессии, с сохранением
   состояния при возврате; проверить масштаб, IME, поворот/фон и остановку.
5. Расширять на другие дистрибутивы после проверки первой Ubuntu.

## Что в первой сборке

У работающих локальных сессий без AI Agents появилась плашка «Рабочий стол X11»
на месте карточки модели/лимитов. У агентов прежние карточки, у SSH X11 здесь
не предлагается. В шестерёнке временного пункта нет.

Плашка открывает встроенный экран того же приложения. Это пока отдельная Activity,
а не замена поверхности TerminalView. На экране настоящее native X11-окно с
цветными прямоугольниками; минимальный клиент на X11-протоколе получает события
клавиатуры/мыши и показывает только счётчики. Это не XFCE и не снимок рабочего
стола. Один общий проверочный дисплей не выдаётся за независимый рабочий стол
каждого окружения. Ubuntu/пакеты/сессии пользователя не запускаются автоматически.

Нажатие плашки X11 вверху графического экрана открывает:

- «Вернуться в терминал»: закрыть экран, сохранить проверочный X11-сервис.
- «Остановить графический рабочий стол»: отдельное подтверждение; завершить
  только выделенный процесс X11. Из уведомления остановка также доступна.

Кнопка «Клавиатура» показывает IME; сначала нажмите на цветную панель. Счётчики
проверяют прохождение событий, не сохраняют введённый текст. Полное управление
жестами, вставкой/буфером и настройки рабочего стола относятся к следующим этапам.
Синхронизация системного clipboard сейчас выключена. На первом запуске в
приватных файлах готовятся данные клавиатуры; установок в Termux/Ubuntu нет.

## Изоляция и ограничения

X11Service и MainActivity не экспортированы, оба в :jobs_x11; Application/CLI
агентов/настройки окружений и схемы библиотеки прежние. Нативное ядро не загружается
в основной процесс и не запускается при обычном использовании терминала.
Сервис foreground/NOT_STICKY; обновление/завершение Android не обещает продолжение.
Завершение сверяет точное имя собственного процесса и использует только свой PID.
При недоступности правильного процесса загрузка запрещается.

TMPDIR, authority и keyboard находятся только в private files/workspace-x11.
Архив клавиатуры проверяется SHA, извлечение ограничено объёмом/числом/путями;
существующие каталоги/ссылки не заменяются произвольным удалением.
Native display :97 работает с cookie; TCP X11 выключен. Неиспользуемый upstream
listener обнаружения блокируется заранее связанным non-listening loopback socket
на7997; проверка описана в third-party/termux-x11/README.md. Перед расширением
нужна проверка сокетов и жизненного цикла на устройстве.

На этот этап взято официальное готовое ARM64-ядро, проверенное по SHA и полному
commit ID. Нативная пересборка Xorg в этой задаче не заявляется. Приватная тестовая
сборка добавляет зависимость; исходники/лицензии/ABI и source-build план должны
оставаться документированы перед любым публичным выпуском.

## Бэкап и совместимость

Исходный релизный1.7.5/code1220 сохранён в Download/Termux-Jobs-1.7.5-BACKUP,
вместе с хешем и README. Это APK, не резерв пользовательских данных. По указанию
разработчика версия бэкапа сейчас не повышалась. Для отката поверх code1221 нужно
пересобрать1.7.5 с более высоким code и прежней подписью; приложение не удалять.

Проверочная1.7.6-x11-test/code1221 сохраняет com.termux/сертификат, minSDK26 и
ARM64. Schema8 не меняется, данные/истории/авторизация автоматически не очищаются.
Дополнительные private X11-файлы создаются только после открытия нового экрана;
1.7.5 ими не пользуется. Полный Android update/rollback cycle ещё не подтверждён.

## Первая проверка разработчиком

1. Установить APK поверх текущего, открыть обычный Terminal/Ubuntu (не агента).
2. Нажать «Рабочий стол X11». Проверить появление цветного окна/статус подключения.
3. Нажать на панели, открыть «Клавиатура», ввести несколько символов: проверить
   рост счётчиков. Убрать/показать клавиатуру, повернуть экран.
4. Через X11 ▾ выбрать «Вернуться в терминал»; проверить исходный терминал и
   AI-чат, затем повторно открыть X11. Окно должно сохраниться.
5. Через меню остановить графический рабочий стол, убедиться, что обычные
   сессии не остановлены. Проверить повторный старт X11.
6. При сбое сохранить скриншот/системную ошибку. Native rendering/IME и Android
   permissions не считаются проверенными одними JVM/ABI-тестами.


## Manual observation (private 1.7.22)

The private same-UID service accepts observation controls only for the active ready
desktop target. A fixed AtomicFile contains session identity, monotonic command serial
and on/off data; it never supplies a PID or command. A helper acknowledges matching
identity/serial. Timeout reports unknown state rather than successful stop.

Recording defaults off and has no three-minute cutoff. Closing diagnostics or hiding
the desktop does not stop a recording. Stop closes the observer's Xlib connection and
its own xfconf/snapshot subprocesses; it does not stop XFCE. The existing guardian
remains responsible for desktop process ownership. A new capture replaces the bounded
32 KiB event tail and 4 KiB backdrop snapshot. No network export happens automatically.

Window classes are reduced to known owner categories; window titles, pointer
coordinates, keys, wallpaper paths and pixels are not recorded by the observer.
Mouse button masks and window IDs distinguish remapping from recreation. Current
backdrop metadata is read after monitored changes using --inspect (no repair writes).
An internal PTY makes xfconf notifications line buffered; no verbose property values
are requested. Monitor errors are visible and do not imply successful change tracking.

Surface loss cancels delayed touchpad hold and releases tracked mouse buttons once.
The native library, compositor selection, renderer ownership and agents are unchanged.


## Owner session and content observation (private 1.7.23)

The launch request supplies session ID plus terminal handle; the service records it
only when starting a new desktop. Other sessions may display the existing desktop
without taking its ownership. session-close requires exact owner and environment;
a delayed close for an old handle or another session is ignored. Closing the owner
uses the existing requestStop/guardian shutdown, without new PID selection.
UI tab changes, surface detach and Return to terminal keep the desktop alive.
A launch waiting for Binder connection is cancelled if its owner closes; already
queued launch and close requests retain their single executor ordering. Explicit
close warns about unsaved graphical work; shell exit also releases its desktop.

Manual observation optionally uses libXdamage/XDamageReportNonEmpty for at most128
visible override-redirect popup windows. Notifications contain IDs/counts, no pixels.
Acknowledging a notification subtracts only the observer's own damage region.
Initial attachment itself produces a notification, so a single count is not proof
of flicker. Missing extension/library is explicit; window observation continues.
Regions are released when no longer relevant and on capture stop. Event drains
and one-second output rate remain bounded. Sampled pointer coordinates are used
only to count changes and are never emitted. Android pointer dispatch counters
and viewport callback counts provide start/current totals; no keyboard content,
absolute positions or native rendering library modifications are introduced.

XDamage indicates invalidated content, not completed GPU frames or presentation
timing. Equal counter values cannot rule out changes between pointer samples.
The next device experiment correlates visible flicker with content and pointer
counts; it is not a claimed fix for the visual defect.


Agent chat keyboard follow-up: the shared toolbar is explicitly hidden whenever
structured chat covers the terminal, including synchronization without a visibility
transition. In-memory extra-key choices are keyed by exact agent chat owner and
bounded to32 entries, not transferred to another chat. Disabled header button
background no longer uses ordinary terminal toolbar visibility. Terminal popup
keyboard mounting/restore remains the existing behavior; streaming cadence is unchanged. Shared chat row updates now settle their pending
viewport at pre-draw after final layout, avoiding painting new row height with the
old scroll position. Pending following is cancelled by touch; history anchor and
composer focus are preserved. This also covers permission/interactive request rows.


## GTK metadata module (private 1.7.24)

Only jobs-x11-ubuntu run commands receive JOBS_X11_GTK_OBSERVE=1; ordinary Ubuntu
receives0. The private new x11-session exports GTK_MODULES with the repository-built
glibc observer asset only when that flag is1. The module checks the flag again and
returns immediately for any program name other than xfce4-panel/xfdesktop. Existing
user processes, agents, Termux, other distributions and configuration files are not
modified. GTK settings/themes, Garcon files and system libraries are not replaced.

Public GLib emission hooks count GtkMenu draw/map/unmap/style, container add/remove
and menu/menu-item pointer events; they always return TRUE to retain the hook and
do not change event propagation or any signal result. No signal parameters beyond
the object reference are inspected. Source categories and last XID are emitted,
never labels, window titles, pointer coordinates, keys or image data. Garcon's
reload-required has G_SIGNAL_NO_HOOKS: use a normal connection on the root core
menu, deduplicated with private qdata, dropping the temporary object reference.
core-hooks counts successful connections over the process lifetime; reload=0
without core-hooks>0 is not evidence that no reload happened.

Counts are enabled/reset by the same fixed token/serial/on-off control as the
independent observer. Each selected GTK process checks the control once a second
and emits at most one coalesced sample per second (plus transition summaries).
Stop can take up to one timer interval to reach GTK counters. No child process,
listener or network is added. The idle module retains bounded counter state, its
hooks and one low-frequency GLib source until the owned desktop process exits.
GModule residency prevents callbacks becoming dangling pointers if a GTK module
reference is closed. It keeps this small library mapped until process exit, not
a separate service. Stale capture lines are rejected by the app report parser.

The asset is an ARM64 glibc .so for the guest, not an Android JNI library. It is
built by an ARM64 glibc C compiler in prepare-x11.py with strict warnings and
RELRO/NOW; final GLIBC requirements and hash are audited. The packaged Android
X11 native library/JNI remain pinned and unchanged. Build using a matching ARM64
glibc compiler; no system package installation is performed by the build script.

The guest module is published via AtomicFile, never truncating an inode an older
desktop may still map. Destination and helper symlinks are rejected. The build
asset's required glibc versions are2.17/2.34; current Test Ubuntu is the supported
validation target. Android native/JNI declarations do not change.


## v1.7.25 desktop choice and GIO evidence

Test-only selection is persisted in workspace-x11 preferences. Binder validates
xfce/lxqt, confines LXQt to jobs-x11-ubuntu and separates verification by target
and desktop. Runtime passes a whitelisted positional choice, never profile shell
text. LXQt starts with its own session and Openbox, no XFCE wallpaper repair or
GTK observer module, same private X11/display/guardian and Fcitx input bridge.
The program catalog/rootfs remain shared; default application configuration root
is preserved. File-monitor telemetry in XFCE is optional and metadata-only; it
counts all GIO notifications in panel/desktop, marking identifiable Garcon-owned
monitors separately, and never changes event propagation. See validation25.


## v1.7.26 private Garcon guard

A pinned upstream4.20.0 core build is shipped as a glibc asset, same SONAME and
public symbols, with one conservative app-dir callback guard. It is loaded only
for Test XFCE and exact package version, never installed over APT files. Native
renderer and process-guardian behavior are unchanged. A toggle controls actual
filtering; off retains snapshot telemetry only during manual recording. Stats
are process-lifetime aggregates; path IDs are hashes, not cryptographic privacy.
Corresponding source/patch/build inputs/license travel in the APK. LXQt bypasses
the overlay, adds GVFS/icon/SVG dependencies, and temporarily uses manual IME.
See VALIDATION-v1.7.26-x11-test.md for limitations and boundary review.


## v1.7.27 trial retirement and explicit launcher repair

Device freezes invalidate the v1.7.26 trial. Its executable shared library is
omitted and never loaded; saved filter preferences cannot reactivate it. Only
historical source/license remain. Off also means no checksum snapshot work.
An explicit confirmed Test-only operation repairs owner read/write bits on
package-checksum-matching system .desktop launchers, preserving originals in an
atomic manifest and user contents/execute bits. Independent target validation
and the existing guardian/progress cancellation apply. This follows isolated
PROot/GIO reproduction of attribute events from ordinary reads of readonly
files, not a claim of device validation or a replacement graphics renderer.
See VALIDATION-v1.7.27-x11-test.md for evidence and remaining checks.


## v1.7.28: XFCE only and Mozilla browser

The LXQt experiment is retired; only explicit one-time removal knows its package
names. Normal GUI/IME paths support XFCE only. Maintenance controls live under
Programs → Installed and require a stopped private Test desktop. Original Garcon
remains in use. Diagnostic entry is hidden; underlying bounded observation code
is retained for future investigation. Firefox uses a fixed signed Mozilla APT
installer with narrow pinning and no Snap/sandbox overrides. See
[validation/provenance](VALIDATION-v1.7.28-x11-test.md).


## v1.7.29: opt-in Firefox compatibility / retired shortcuts / startup

See [validation](VALIDATION-v1.7.29-x11-test.md). A new fixed Test-only maintenance
helper journals a Firefox menu override and process-scoped wrapper after explicit
seals-protection warning/consent. Disable preserves edits and reverses matching
managed files. Both program sections share this action. No global environment
changes. Startup also deletes byte-identical retired PCManFM-Qt desktop shortcuts without backups
when that binary is absent. Package-removal operation is retired. Wallpaper startup
now runs its restoration in brief mode, without expensive image decoding/metadata
queries; explicit inspection keeps full output. Native X11 core/input unchanged.


## v1.7.30: checkbox / live desktop configuration

The independent program worker applies the existing Firefox compatibility action
without stopping XFCE. A checkbox in Firefox's action column replaces the separate
row. Actual rootfs journal/file hashes are read in the worker even for cached
catalog requests. Cancellation keeps the visual state unchanged. No new Firefox
icons, PATH wrappers or shell aliases. See [validation](VALIDATION-v1.7.30-x11-test.md).


## v1.7.31: existing Debian desktop

Extend the exact target allowlist with `debian`, not a new alias or Ubuntu redirect.
The same inline XFCE renderer, Fcitx keyboard, readiness gates, guardian and session
owner handling are reused. App installation itself does not modify the rootfs.
The desktop installer refuses an absent Debian before any host package/install
operation, with a second refusal in the rootfs-install branch. It never installs
or replaces Debian. An existing Debian 13 trixie rootfs was identified read-only.
All application/program commands retain target-specific login and bind routing.

`DesktopPrograms.supportsTarget` centralizes Java routing;
`supportsMaintenance` allows Debian and the existing Test Ubuntu alias, preserving
the old exclusion of the shared agent Ubuntu. Debian gets explicit checksum-bound
launcher permission repair and opt-in Firefox PRoot compatibility. The latter
remains the menu-only Mozilla binary wrapper with a warning about weaker seals
protection; its state is read from the selected rootfs. Ubuntu-only retired LXQt
shortcut cleanup is deliberately not run against existing Debian user shortcuts.
AppStream/installed catalogs remain separately cached by exact environment alias.

APT groups and the official signed Mozilla ARM64 flow are shared by these APT
systems. Debian package metadata for `fcitx5-frontend-qt5`,
`gir1.2-appstream-1.0` and `7zip` was checked on packages.debian.org/trixie.
No new binary libraries, JNI changes or agent/terminal changes are needed.
A running desktop from another rootfs blocks checks, installs and program actions;
stop it before switching. No simultaneous rendering of terminal and desktop.
Debian package installation, graphical input, programs and browser must still be
verified by the developer on the device; Ubuntu's checks do not prove Debian's.


## v1.7.32: certificate bootstrap and measured download volume

Debian's first Firefox install failed certificate verification. Read-only rootfs
inspection found no configured ca-certificates package or CA bundle in Debian;
Test Ubuntu already had them. X11 component check now requires both the configured
package and a nonempty /etc/ssl/certs/ca-certificates.crt. The consented APT desktop
install explicitly includes ca-certificates and regenerates the store. This applies
to existing Debian and both Ubuntu targets; it does not modify agent bootstraps.
Firefox installs missing certificates before adding/updating Mozilla's HTTPS source,
reuses a cached signed distro CA candidate when available, or first refreshes native
APT indices. Existing configured but empty stores are regenerated, then verified.
Failure is propagated; no insecure TLS, unauthenticated package or trusted=yes flags.

APT Status-Fd dlstatus contains a FILE count, percent and human message, not bytes.
The optional Debug::acquire::progress record reports rounded CurrentBytes/TotalBytes
in decimal units. The format was checked in official Debian/apt acquire.cc (main
and 3.0.3) and strutl.cc SizeToStr. All private guest APT calls enable that narrow
record and LC_ALL=C, leaving user shell locales alone. The bounded existing parser
accepts finite, positive totals and sane units, handles fragmented lines, and resets
sizes on stage changes. No sizes are inferred from percent or number of files.
Fields cross the existing snapshot/Bundle path to the common DesktopInstallUi;
volume appears only for package lists/download stages and hides during unpack/setup.
Existing cancel, log follow, percent and stage-ETA behavior is retained. This does
not enable concurrent desktops or create a new rootfs.

A new APT update transaction resets the prior transaction's mapped percentage,
so a completed CA bootstrap cannot leave the later Firefox download stuck at 99%.
Stage volumes and ETA reset together; success still waits for the owned guardian.


## v1.7.33: Parole recommendation and desktop-owned audio

The existing Debian and Ubuntu targets share the same optional `media` package
group: parole, gstreamer1.0-libav, plugins-good, plugins-bad, pulseaudio and x output
plugins. Only Parole's ordinary Recommended card is added, with the existing
install/status/confirmation/progress behavior. Catalog `pkg:parole` uses the same
group. No new player settings, tab, shortcut code or Android player is introduced.
Existing installed-app removal keeps shared dependencies, like other programs.

The consented desktop install adds guest libpulse0/gstreamer1.0-pulseaudio and,
only when the host binary/modules are absent, installs Termux's pulseaudio package.
The component check includes them. No guest audio daemon is installed for this
bridge. `x11-audio.sh` is sourced by the already setsid/guardian-owned host child.
It allocates a fresh mode0700 runtime, generates a mode0600 256-byte cookie, and
runs a foreground native PulseAudio with isolated runtime/state paths, no PID
file, no default module script, no shared memory, and only SLES playback and a
cookie-authenticated UNIX protocol module. No TCP listener, microphone module,
--start/adoption or --kill command. Modules/exiting cannot be requested by clients.

Bounded socket readiness passes only the private guest PULSE_SERVER/PULSE_COOKIE
paths through the already validated rootfs bind. Missing/start-failed audio logs
an explicit marker and an unavailable route; it does not attach to another daemon
or prevent desktop display. Source inspection of upstream PulseAudio main.c puts
setsid/fork under daemonize; --daemonize=no retains the existing owned group.
Existing guardian EOF/owner-session close stops that group, including PulseAudio.
Return to terminal only hides the desktop and leaves its audio playing. Runtime
cleanup removes only the newly allocated private tree; no user Pulse configs or
other daemons are changed. Abrupt SIGKILL can leave a small temporary runtime;
normal owner closure is fixture-tested to remove it.

Package/desktop tests use fake daemons and private fixtures; no real host audio
server was started/stopped during development. Device checks still need real MP4
video, audio output, volume, switching and owner closure on Debian and Ubuntu.
No guarantee of hardware-accelerated decode, every codec or browser DRM playback.


## v1.7.34: native Alpine and Termux desktop paths; diagnostics retired

The exact allowlist now includes existing `alpine` and direct `termux`. No new
rootfs is created. Alpine shares the fixed PRoot login/bind route but uses native
APK packages, Firefox and musl libraries. Termux bypasses PRoot entirely: its XFCE,
programs, certificates, fonts and Fcitx5 use the native PREFIX. The existing guardian,
X server, surface, owner close and audio bridge are reused, one desktop at a time.
Termux control/runtime/wallpaper paths use the app's private workspace-x11, not a
nonexistent native /tmp or /usr. Guest paths remain the prior fixed bind paths.

`x11-native-desktop.sh` fixes/checks platform-specific base package groups;
`x11-native-programs.py` fixes recommended groups and dispatches APK or Termux APT.
All base and recommended names are checked against the fetched official Alpine
v3.24/aarch64 APKINDEX and Termux main/x11 aarch64 metadata. Read-only inspection
identified the installed Alpine 3.24.2 and its existing main/community repositories;
those selections are not replaced. Native Firefox comes from those distro repos,
not the Ubuntu Mozilla bootstrap. Native Termux LibreOffice is one package.

Alpine/Termux catalog lists native packages (including libraries) rather than an
unavailable cross-platform AppStream pool; UI explains this. Installed remains a
.desktop ownership-based graphical list with the existing delete controls. Full
Alpine metadata contains about 28,561 packages, so the bounded parser/page offset
limit increases to40,000; the independent32MiB disk snapshot limit remains. UI pages
remain30 rows. Descriptions come from native repository metadata, installed state
from native package databases, and caches remain separate by target. Package names
and program IDs are validated before fixed argv invocation; no arbitrary shell.

Alpine removal first simulates APK's plan and refuses if it would remove anything
beyond the selected package; no dependency/autoremove consent is inferred from
single-program removal. Termux retains dpkg --remove with dependency checks.
Launcher repair accepts Alpine Q1/SHA1 package-file checksums or native Termux dpkg
MD5 manifests, preserving unverified/edited files and original modes. The Firefox
menu-only compatibility wrapper accepts native install/home prefixes, retains local
edit/symlink checks, and reads actual target-specific state. No global sandbox flag.

APK's numeric progress is a stage ratio, not assumed to be downloaded bytes. It
uses the same progress UI without invented volumes; APT retains measured byte sizes.
APK's stage/size text must still be verified on a device. Main/Ubuntu/Debian package
paths remain the previous APT flow. Termux may need to enable the standard x11-repo
package after the desktop installation confirmation.

All four session launches now omit the Python X11 metadata observer and GTK module.
The APK omits x11-observe.py, x11-gtk-observe.so and the retired Garcon source archive/
license assets. The module is no longer compiled by prepare-x11. Source experiments
are preserved in the repository/third-party/x11-diagnostics only. Diagnostic RPCs
are rejected, the observer I/O executor/capture/control-file startup is removed.
The Fcitx focus bus subscription, bounded desktop readiness/window-manager checks,
wallpaper repair and normal bounded child error output remain functional machinery,
not an optional observer. Native X11 JNI/renderer libraries are unchanged.

## v1.7.35 private lifecycle correction

A completed/failed desktop now clears started, render readiness and input focus,
then retires the dedicated X11 VM after a bounded1.2s window for delivering the
failure snapshot. The native server has no teardown/restart API; it is never reused
with a different rootfs while retirement is pending. Startup errors take this path
as well. Explicit Stop checks the exact target and does not hide the UI before
acknowledgement; cancellation while preparing also locks pending startup. Late
readiness/focus callbacks cannot reactivate a retired desktop. Responses from a
previous disconnected Binder cannot clear the current desktop selection.

Owned desktop processes still terminate through EOF of the guardian pipe and its
private process group. No profile-provided PID, global kill or terminal-session
termination is introduced. The notification Stop acts only inside its own service
process. Actual desktop exit status is taken from DESKTOP_CHILD_EXIT when present;
guardian137 alone is not evidence of an XFCE OOM/crash. Every supported target shares
this lifecycle. Separate simultaneous servers remain out of scope.

## v1.7.36 Alpine-only compatibility and startup milestones

See [Alpine failure evidence](X11-ALPINE-STARTUP.md) and
[validation](VALIDATION-v1.7.36-x11-test.md). A3.7KiB musl/ARM64 compatibility asset
repairs the specific F_OK/faccessat EINVAL fallback and selects Glycin2 NotSandboxed
for image decoding. No system libraries or repository versions are replaced. Only
Alpine's owned desktop launch enables LD_PRELOAD, after explicit target routing;
Ubuntu/Debian/native Termux never load it. This weakens Alpine image-decoder isolation,
not Android/browser sandbox configuration. Read/write permission checks are preserved.

X11StartupView replaces the indeterminate spinner with an X11 emblem, fixed title,
a milestone bar and RU/EN current-stage text. Service startupStep progresses on native
preparation, server startup, server readiness, window-manager readiness and desktop
readiness. No new timer or observer is added. Window-manager readiness is retained
from bounded existing output even after stderr rolls over. Timeout retains manual
Show screen; only desktop readiness allows the bar's completion. Surface/IME geometry,
renderer JNI, shutdown ownership and agents remain unchanged.

## v1.7.37 readiness and APK-name fixes

The session launches a startup-only x11-ready.py child in the existing owned group.
It verifies the WM self-check, mapped EWMH DOCK and DESKTOP window types. Client IDs
come from _NET_CLIENT_LIST; class hints from the tree cover override-redirect clients
missing there. Hints still require type/viewability validation. Localized titles do
not decide readiness. X queries have2s subprocess timeouts, a3s snapshot budget and
bounded client counts. No titles, text, paths or authority cookies are logged.

At about20s it emits the existing TIMEOUT marker plus one flags-only WAIT summary,
then continues at2s intervals until readiness or an approximately3-minute overall
limit (plus bounded query overhead). Success exits immediately. It is not a permanent
observer and shares guardian shutdown ownership. The same current session token
protects READY/TIMEOUT. Late READY clears timeout state and automatically attaches
using existing UI logic. Manual Show remains available and does not restart the desk.

Alpine package parsing/catalog request validation now accepts ASCII uppercase,
underscore and one-character names, preserving case. Read-only installed database
reproduction previously failed on libSvtAv1Enc, then ssl_client; corrected parsing
finds386 packages and37 desktop owners/checksums. These are observed snapshot counts,
not a hardcoded inventory. Package requests remain fixed argv; options, spaces,
slashes, traversal and shell delimiters are rejected. APT tools retain their own
validation; package/cache formats and unrelated environments are not migrated.


## v1.7.41 inline program jobs, APT acquire and Alpine activation

DesktopProgramProgress is a per-card view, not a job owner. WorkspaceDesktopPrograms
updates only that card's progress/status while busy; unchanged snapshots do not notify
the catalog adapter. Closing its dialog removes UI callbacks, never sends programs-cancel.
Opening reconstructs the active program from the selected service snapshot; Stop alone
sends cancel. No second modal installer is opened for action=install. Other maintenance,
removal and catalog preparation retain their existing confirmation/progress flows.

Recommended deletion is allowed only for fixed main-package mappings with verified
same-target installed group state, or the existing installed catalog. Core and dependency
checks still apply. No group expansion or implicit dependency removal was introduced.
Main-package-only semantics are explicit in the confirmation and usage documentation.

APT acquire options are invocation-local: Retries3 and HTTP/HTTPS Timeout30. They retry
network acquisition, not dpkg scripts/the outer transaction; authentication and selected
repositories remain unchanged. Real loopback apt-helper tests verify503→retry→success
and no retry on SHA256 mismatch, with no package install or system config mutation.
APK Alpine is unchanged; merely repeating add after failure was deliberately avoided.

Alpine's existing compatibility module is exported in the selected guest before its
private dbus-run-session exec. Thus D-Bus-activated Tumbler inherits the same image-loader
compatibility as directly started apps. Session fallback is idempotent. Other distro
buses, Android and agent processes are unaffected. Existing decoder sandbox limitation
is unchanged; the native compatibility binary and pinned X11 renderer are unchanged.


Catalog/recommended/installed navigation is independent of busy job state. Existing pages
are read-only while the job continues; no duplicate scan/install is issued by navigation.
Completion reloads relevant cached pages; a missing installed/recommended snapshot may
start its own required read-only scan only after the previous job completes.

Desktop switching now records a launch-after-check intent. Only the actual check/install
request acknowledgement enables its completion; an older polled installed snapshot cannot
skip verification. Missing components offer explicit installation; success launches the
original session's desktop. Closing that session clears the intent. Started desktops keep
switch/return and conditional Stop; the old components/installed menu row is removed.


## v1.7.42 programs entry, animation and thumbnail encoder

DesktopSessionBar owns two independent chips. Programs uses the unchanged
WorkspaceTransferBorder Drawable also used by the server button (16ms scheduling).
Its frame callback invalidates only the drawable. It never invokes renderCodexStatus,
layout recreation or terminal/chat adapters. Detach/hide/Activity stop cancels animation;
existing700ms activity polling only toggles the active flag. Border is enabled only for
selected-target installing/cancelling snapshots with the Programs dialog closed.

WorkspaceDesktop retains selected-visible installation state polling at the existing
400ms interval after dialog close, even without a graphical surface. Hidden environments
stop this UI polling; sync resumes it when returning. Jobs remain owned by the existing
service, with no new workers/scans/cancellation paths. State polling reads cached Bundles,
not package lists. Programs is removed from the switch menu and opens directly through
the facade; its Back action simply closes the ordinary Programs dialog.

Glycin has separate loader and creator selectors. Existing loader hook cannot prevent
the PNG encoder from attempting Bubblewrap when Tumbler saves a thumbnail. The scoped
Alpine helper now wraps gly_creator_new, resolving only already-loaded exact ABI
libglycin-2.so.0 and applying selector3 before use. Failure/null handling mirrors the
loader hook; error pointer is passed untouched. This remains confined to the opted-in
Alpine bus/desktop tree; it is not a global system library or renderer replacement.
No additional Android permissions or Firefox sandbox flags were introduced.

Private copied Tumbler + private D-Bus fixture reproduces this exact sequence, then
verifies Ready/Finished and a cache PNG with valid CRCs/URI metadata after the new hook.
Fake ABI tests cover loader + creator in both RTLD_LOCAL and GLOBAL modes and the existing
narrow filesystem fallback. Installed desktop is not used as a test target.


## v1.7.43 catalog navigation and conditional preparation

WorkspaceDesktopPrograms separates main/recommended/installed/catalog views. Main has
three navigation entries and no search/list adapter. Catalog pages are accepted/requested
only in catalog or installed views; leaving removes delayed search callbacks and stale
view references. Late pages cannot restore a list on main. Background service job ownership,
Stop/cancel, error Details, inline cards and visibility-gated border remain unchanged.

The catalog view has one fixed Prepare and refresh action. Existing catalog-install
protocol is retained; APT shell checks ready catalog-tools before its install branch.
Already configured tools skip update+install branch and only refresh indexes once. Missing
tools require initial update/install and a post-install update to enable DEP-11 metadata.
Native catalog-install already updates APK/APT indexes without installing AppStream.
All arguments remain fixed/validated; no new package execution or repository override API.

Termux PDF group explicitly requires evince +libandroid-complex-math; pkg:evince maps to
that group too. Readiness recognizes the missing dependency as incomplete. This works
around the observed libsynctex dependency omission, retaining normal signed APT acquisition.
Other distributions, browser configuration and the native renderer are unchanged.


## Частная v1.7.53: GPU-рендеринг

В меню «Терминал / Рабочий стол» добавлен выбор CPU / Turnip + Zink.
GPU пока доступен только для Termux и обычной Ubuntu 26.04 и меняется при
остановленном столе. Компоненты загружаются отдельно с SHA-256 в частный кэш;
переменные драйвера действуют только в графическом процессе.
[Установка, ограничения и тест glmark2](X11-GPU.md),
[проверки сборки](VALIDATION-v1.7.53-x11-test.md).


## v1.7.54: structured session diagnostics

X11StartupLog retains at most48 deduplicated categories, bounded partial lines,
monotonic relative stage timing, and a frozen elapsed duration after desktop readiness.
The existing child stderr stream supplies markers/errors; raw stdout, focus tokens,
paths and program contents are not exported. X11Service adds startupDiagnostic and
startupElapsedMs to its existing state snapshot. Startup completion/stop logic is unchanged.
WorkspaceDesktops exposes cached diagnostics without creating a channel; explicit X11
view/export refresh asks only an already-bound service for state. No new periodic task.
DiagnosticsPanel adds optional X11 content and disables journal mode controls in that tab.
Unsupported local environments get a support message; agents and ubuntu-ai are excluded.


## v1.7.57: startup-wide diagnostics

StartupTrace in x11-ready.py samples fixed process roles bearing the private launch
UUID, with proc age/CPU/wait categories and no argv/environment export. Existing
query durations/failures are aggregated; ListClients is queried at most every5s
only after GetState replies, within the existing deadline. Unix-only auth probes
record result categories, never replies/credentials. Tracing ends with readiness
or the existing timeout. X11StartupLog retains first/latest bounded numeric records
separately from stages. Selection, display ownership and stop controls are unchanged.


## v1.7.58: XFCE owns its manager startup

Remove independent pre-launch of xfwm4: XFCE's Failsafe would spawn a duplicate,
which can leave registration bookkeeping pending after failed startup. The existing
readiness helper now reports WM-ready on observed root property; it no longer blocks
launching the session. A private runtime PATH wrapper supplies compositor mode when
XFCE invokes xfwm4; saved configuration is untouched. Original binary is resolved
before PATH modification, argv remain quoted, links at wrapper destinations are refused.
Stop/guardian/display ownership are unchanged. Kernel CLOCK_BOOTTIME replaces PRoot's
container-relative /proc/uptime for process age diagnostics; unavailable means unknown.

## Частная v1.7.61: экран подготовки в выбранном окружении

WorkspaceDesktop хранит preparationProfile отдельно в канале соответствующего
окружения. Во время проверки/установки вместо SurfaceView создаётся обычный
X11StartupView, без подключения графического renderer. После подтверждённой
установки отправляется launch; при preparing/running обычный экран подготовки
заменяется RemoteDesktopView с прежними критериями готовности. Два renderer
одновременно не создаются. Эмблема одинакова в обоих видах подготовки.

Требование установки хранится до возвращения в исходную сессию; подтверждение
не показывается поверх другого окружения. Ответ Нет удаляет ожидание и
возвращает терминал. Отмена очищает автоматический запуск и отправляет cancel
выбранной службе (stop для уже начатого запуска). До связывания отмена удаляет
неотправленную команду. Закрытие сессии удаляет её ожидание; backend/lifecycle
установки и владение процессами не менялись. Ошибки показываются в центре
с возвратом в терминал. Обслуживание/исправления ярлыков используют прежнее окно.

## Частная v1.7.63: существующая Fedora

Fedora получает частную службу X11Service$Fedora в :jobs_x11_fedora,
workspace-x11-fedora, display102/guard port8002. Rootfs не создаётся
и не перенаправляется в Ubuntu. x11-native-desktop.sh устанавливает
Fedora-пакеты через DNF, x11-native-programs.py использует RPM/DNF для
программ, метаданных и проверенного ремонта ярлыков. Общие пути запуска,
ввода, звука, диагностики и UI подготовки сохраняются.
