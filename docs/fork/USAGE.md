# Agent chats, aliases and files — v2.0.9

The native Developer Markdown layout is shared by Codex, Claude, Gemini and
OpenCode. Code cards show the language, syntax colours and separate line numbers;
Copy has its own outlined button with pressed feedback and copies the complete
original code without those numbers. Long blocks can be
expanded. Tables scroll horizontally inside the message. Existing raw history is
retained. Web links open only after a tap; Markdown does not execute HTML/scripts
or automatically fetch remote images.

Explicit local file links in assistant answers can produce downloadable cards for
any regular non-empty file within the existing limits, including ZIP/7Z/RAR/GZ,
3MF/STL, EXE/BAT and unknown/no-extension files. Tap the card and save to Download.
APK installation remains its own Android-confirmed action; other files are not
executed or unpacked. File formats are not a claim that every model can interpret
their contents. An updated native agent process is needed for new output helpers.

In the attachment picker, an ordinary tap chooses one file. A long press enters
checkbox multi-selection; confirm the selection to stage it in the draft. Nothing
is sent until Send is pressed. The four-attachment limit is shared by existing
and newly selected files. Images remain limited to100MiB, other originals to5GiB.

From device gallery opens the system photo/video selector, with a document-picker
fallback on older Android. Only explicitly selected content URIs are copied into
the selected chat's private staging area; no full-storage permission is needed
for this route. Phone storage retains its existing storage-access request.
Gallery media processing and codecs still depend on Android support and the
existing pixel/duration/file bounds.

The sorting icon beside Home is shared by the file browser and its picker modes.
Name, size and date have ascending/descending choices; folders stay first and
unknown dates stay last. The selected display order is saved without changing
files or transfer selections.

The hand button between model/limits and Server + Agent interrupts the current
agent request. It is red while available, grey and crossed out while idle. The
activity cue enters the right half of its circle, crosses the top toward the
left and continues right along the line. Its redraw is synchronized with Android
display frames, targeting 60 FPS; actual smoothness still needs device verification.
Completed file changes are not rolled back.
Sending a chat message hides the keyboard only after acknowledgement, while a
failed send or a different selected session retains its current input state.

Device-specific gallery, IME and animation limits are recorded in [release validation](VALIDATION-v2.0.9.md).

---

# Current desktop and driver behavior — v2.0.7

Choose the renderer before starting the desktop. Wrapper and Bionic for glibc offer a protected **a6xx-a7xx-a8xx** default when no imported selection exists; selecting it alone does not download. Startup verifies/prepares the selected runtime and default ZIP as needed. Existing imported selections remain available. A trash button removes an imported driver after confirmation while that environment's desktop is stopped; the original phone ZIP and unrelated drivers remain.

Enabling Kali style prepares its verified optional archive before desktop launch. Alpine VirGL prepares its own musl client; other distribution packages still use their native repositories. Downloads use the components repository, exact sizes and SHA-256. Matching verified caches can be reused offline. Updating the app does not hot-replace libraries in an active desktop: stop/start it to adopt changed components. Live scale Apply/reset restores unset defaults correctly. See [component behavior](GRAPHICS-COMPONENTS.md) and [release](RELEASE-v2.0.7.md).

---

# Historical desktop and file behavior — v2.0.6

All eight ordinary environments use X11/XFCE; no Wayland/KDE entry remains. Open Programs in Arch directly for its X11 applications. Select the renderer and, for Wrapper/Bionic for glibc, import or choose a trusted saved Bionic driver before starting the desktop. Missing pinned graphics components download from the [components repository](https://github.com/HezeAI/termux-jobs-components) through the existing preparation/progress flow. Cancellation or a bad checksum does not activate a partial runtime. Valid installed components need no repeat download; ordinary Linux packages still use each system’s package manager.

Apply interface scale live in all seven PRoot desktops; Termux retains its working path. Manually sized X11 no longer squeezes when the keyboard opens. Chats accept files up to5GiB and original images up to100MiB; a separate bounded image is prepared for model input. Provider/model capabilities still limit what an agent can inspect.

No data reset or environment migration is required. Updating does not automatically delete previously installed experimental KDE packages. See [release notes](RELEASE-v2.0.6.md) and [screen controls](X11-SCREEN.md). Earlier sections are historical.

---

# X11 screen and keyboard controls — v2.0.5

Open Terminal / Desktop → X11 screen. Set resolution, scale and margins, then
Apply; Cancel does not save. Auto setup resets all three together. Unchecking it
prefills 1600×720/75%/5dp; reopening a saved manual mode keeps its own settings.
See [screen controls](X11-SCREEN.md) for alignment, rotation and live scaling.
The compact extra-key panel retains independent manual input, move/resize and
modifier release. Automatic keyboard requests require a supported active normal
application input context; use | otherwise. Arch Wayland and related driver
controls are disabled in this public release pending further tests.

---

# Bionic drivers and X11 tools — v2.0.4

Stop the selected ordinary glibc desktop, open Terminal / Desktop → Rendering, select Bionic for glibc and use the folder to import a trusted matching Bionic driver ZIP. The row below selects saved imports. Start normally; each target prepares its own native dependencies/cache. CPU/Turnip+Zink/VirGL remain selectable while stopped. See [bridge requirements and limits](BIONIC_GLIBC.md), [driver downloads](DRIVER-SOURCES-v2.0.4.md) and [release](RELEASE-v2.0.4.md).

The keyboard icon toggles X11 extra keys independently of Interactive input. In fullscreen it is beside the | action. Right tab moves the panel, left tab resizes it; held modifiers release on closing/switching. Programs → Recommended → Jobs GPU Test installs the optional diagnostic, dependencies and shortcut in the selected target. Saved reports identify the scene, renderer, size and interval. The optional theme has rounded metrics about8% smaller on the next desktop launch; theme-off restoration remains.

---

# X11 landscape in v2.0.3

Open the selected environment's desktop, then rotate the phone horizontally.
The same desktop fills the screen without app tabs or the terminal input row.
At the top right, `|` is the existing Interactive input action. Desktop settings
and automatic keyboard behavior remain in the embedded view's existing menu. Rotate vertically or use Android Back to return the same desktop
to its embedded app window; open programs keep running. If the keyboard is open,
Android may consume the first Back to hide it, as with other text fields.

After returning with Back while still horizontal, the app stays embedded until
the next portrait-to-landscape turn. Landscape sizing follows physical screen dimensions, capped at 2880 pixels wide
without stretching one axis. The normal app orientation
policy is restored when leaving the desktop view. This applies to each of the
eight supported environments and does not change renderer selection or packages.
Physical rotation/IME/driver validation is recorded in
[v2.0.3 validation](VALIDATION-v2.0.3.md).

## Optional Termux-job's style

Termux, Ubuntu, Debian, Alpine, Fedora, openSUSE, Void and Arch Linux ARM each
offer their own **Termux-job's style** checkbox below
Switch to desktop. Stop that environment's desktop before changing the checkbox;
the selection applies at its next launch. The Kali-inspired pack includes dark
windows, blue accents, a dragon wallpaper, larger icons and a black/white cursor.
It is bundled in the APK. Each environment keeps its own selection and saved XFCE
properties; turning it off restores tracked properties that were not manually
changed. Existing custom theme assets are not silently overwritten. Each Linux
guest uses its own HOME and /etc/xdg defaults, never Termux's PREFIX. The theme
preserves distribution menu plugins and entries, so menus can differ between
systems. Ubuntu AI and retired/unknown aliases do not gain this theme option.

In fullscreen the translucent | button sits48dp below the top safe edge and fills when
interactive input is enabled. The fill follows manual toggles and the existing
automatic keyboard/Back state; it does not reopen the keyboard on its own.

## Fedora Parole and MP4

Use Programs → Recommended → Video and audio → Install again after updating if
Parole was already present. The corrected group requests Parole, GStreamer tools,
libav/base/good/bad-free plugins and the C++ runtimes explicitly, then installs
bundled, pinned OpenH264 and its GStreamer bridge in that Fedora HOME. It checks
real decoder creation and openh264dec/avdec_aac/qtdemux before reporting ready.
Package presence alone is insufficient. A failed check stays missing for retry.
Normal Fedora package downloads need internet; these two codec assets are in the
APK and do not depend on Cisco being reachable. GPG/TLS checking remains enabled.

Close any old Parole window and relaunch from the applications menu or the updated
application association. The private launcher owns the codec/plugin path and a
separate registry; direct /usr/bin/parole and copied old launchers do not gain this
environment. System RPM codec files and other environments are not replaced.
Validated decoding is H.264 video plus AAC audio in a test MP4; MP4 can contain
other codecs, so this does not promise every MP4/HEVC format or hardware decoding.
[Focused evidence](AUDIT-FEDORA-MP4-2026-10-24.md).

# Agent files (introduced in v2.0.2)

Tap the folder beside Shell, choose Environments or Phone storage and select a file.
Phone storage requests Android access when needed. Review the draft, optionally
add text, and press Send; × removes an unsent file. You do not need to type its path
on the phone. The original is made available in the selected agent environment.
Models/CLI permissions still determine which media and tool operations are possible.

Ask the agent to work on the attached project/log/APK or build a project. Local
output links can appear as cards in chat: tap to preview or save, and tap an APK
card for Download/Install. The app does not automatically install returned APKs.
Codex output cards currently cover images/APKs; the other updated agents also
project supported local file links as cards.
Images use native media input; video supplies sampled timestamped frames without
audio, and binary files are handled by agent tools. No automatic archive unpacking,
full video/audio ingestion or guaranteed model interpretation is implied.

After updating, restart agents with their saved chats. The Codex sign-in notice
opens its official terminal device-login flow when needed. For Gemini API, an
optional masked key field is below the auth choice: blank retains an existing key
or the CLI's normal auth/terminal fallback. Changes apply next launch. Stored keys
are device/profile-bound; re-enter on preset duplicate/import.

The sections below record earlier features and private implementation stages;
private labels2.0.3–2.0.6 are not later public releases. Current release evidence is
[VALIDATION-v2.0.2.md](VALIDATION-v2.0.2.md).

# v2.0 — desktop and agent quick guide

Create/select a local **Termux, Ubuntu, Debian, Alpine, Fedora, openSUSE, Void
or Arch Linux ARM** session. Use its **Terminal / Desktop** card, select a
renderer before launch, then **Switch to desktop**. Missing desktop/renderer
components are offered with consent; installation/startup progress appears in
the central area. Use **Programs** for Recommended, Installed programs and
Search and install programs. Programs use that selected system's package manager.
Android may terminate background processes; Continue can reuse package-manager
caches but does not guarantee byte-range resumption of every download.

v2.0.1 backup restoration creates independent preset copies, including when names
match existing presets; repeating an import creates further copies. Each imported
history stays with its own preset. Existing histories are not overwritten.
Restart an X11 desktop after updating to apply presentation and renderer checks.

Keyboard: allow automatic input where supported, or use the top interactive-input
button manually. Phone storage and Downloads desktop shortcuts require Android
storage access. Permission refusal does not grant hidden storage access.

Graphics: CPU is the fallback; Turnip+Zink requires compatible Adreno/KGSL;
VirGL uses a private Android server and explicit target-specific ANGLE backend.
Wrapper and custom Bionic Turnip ZIP import are **Termux only**. GPU & GPU INFO
are optional recommended tools. Some Linux VirGL information queries use EGL;
that is labelled in output and does not alter the benchmark command.

The optional Kali-inspired style is only for the base Termux desktop. Apply
before startup; it is not a Kali distribution. Session gear opens settings.
Stop the desktop through its menu; closing its owner session also stops it.

New agents default to Ubuntu AI. Existing agent selections remain until an
explicit change/confirmed migration. Migration copies the Ubuntu environment
(including its credentials), not external mounted phone files; source Ubuntu
is not automatically removed. Check paths and make backups before deletion.
The optional connection command is an absolute executable wrapper path for
launching an agent through **your own server/tunnel/proxy**. Empty means ordinary
startup. Restart the chosen agent/chat to apply it; do not start an interactive
Codex inside the chat's noninteractive Shell command field.

**Tested on RedMagic 10 Pro and Samsung S26 Ultra; other devices require testing.**
Coverage varies by phone. Read [release notes](RELEASE-v2.0.0.md),
[validation](VALIDATION-v2.0.0.md) and [source credits](THIRD-PARTY.md).

All older version-labelled entries below are historical development records.

## Private1.7.46: автоматическая подготовка миграции

Если нет Python/proot-distro с нужным модулем блокировок, кнопка «Подготовить автоматически»
предложит подтвердить подготовку в Termux. В Ubuntu AI устанавливать proot-distro не нужно.
Готовые компоненты не переустанавливаются. После успешной подготовки проверка повторяется.
При отказе Android в доступе, отсутствии пути или ошибке файловой системы выводится
конкретная причина и системный код; устанавливать пакеты вместо исправления доступа
приложение не предлагает. Базовую вкладку Termux закрывать не требуется, но все процессы
исходной Ubuntu должны быть завершены пользователем до миграции. Автоматической остановки
сессий нет. Окно копирования по-прежнему закрывается только явной отменой с очисткой.

## Private1.7.45: агентские окружения

В создании/настройках сессии или пресета AI Agents выберите «Окружение агента»:
Ubuntu либо Ubuntu AI. «Проверка среды запуска» проверяет именно выбранную систему.
«Миграция среды агента» расположена сразу под проверкой. При отсутствии исходной Ubuntu
предлагается чистая отдельная установка Ubuntu AI. Если общая Ubuntu уже есть, закройте
все её сессии, рабочий стол, установки и внешние CLI; затем запустите миграцию, проверьте
объём и подтвердите копирование. Копируются все находящиеся внутри файлы, включая
историю и секреты всех агентов. Общая память и внешние монтирования остаются общими.

После копирования можно переключить текущую форму (сохранить обычной кнопкой) либо
все агентские пресеты. Старую систему проверьте перед удалением. В списке окружений
корзинка доступна у установленных систем, включая импортированные. Ссылки сессий/
пресетов и действующие native-lock не дадут удалить используемую систему. Сначала
переключите или удалите связанные пресеты. Корзинка не удаляет архивы бэкапов, общую
память или базовый Termux. Произвольные команды с вписанным именем системы автоматически
не распознаются. Удаление требует отдельного подтверждения и после начала не отменяется.

Вход вручную: `proot-distro login ubuntu-ai`. Обычный `proot-distro login ubuntu`
по-прежнему открывает обычную Ubuntu. Пакеты и файлы агента следуют выбранной системе;
доступ к общей памяти/Termux не становится отдельной защитной песочницей. Жёсткие пути
в пользовательских скриптах и инструкциях могут потребовать корректировки.

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

# Частная X11 v1.7.40

После завершения запуска XFCE рабочий стол должен показаться автоматически: приложение проверяет собственное состояние готовности XFCE и его оконный менеджер. При таймауте ручная кнопка остаётся как запасной доступ. На телефоне нужно проверить новый запуск без нажатия кнопки. Сборка также содержит исправления профилей Firefox и сохранения статусов программ после ошибок.

---

# Частная X11 v1.7.39

Ошибка переключателя браузера не сбрасывает известные статусы программ. Если прервана установка или удаление пакетов, повторная проверка нужна, поскольку состав пакетов мог измениться. DNS HTTPS необязателен: при работающем браузере без него включать не требуется. Поддерживаются оба расположения профилей Firefox: .mozilla/firefox и .config/mozilla/firefox. Перед изменением DNS закройте браузер.

---

# Частная X11 v1.7.38

Каждое поддерживаемое окружение имеет свой X11, один рабочий стол на окружение. Вторая вкладка подключается к тому же столу; закрытие последней подключённой вкладки останавливает его. Кнопка «Остановить» действует только на выбранное окружение. Для Firefox в Alpine/Termux: закройте браузер, откройте «Программы рабочего стола → Рекомендуемые → Браузер». «Совместимость с PRoot» теперь принимает системный ярлык Alpine. При ошибках DNS можно отдельно включить «DNS HTTPS»: после подтверждения запросы имён Firefox идут Cloudflare по HTTPS с обычным DNS как запасным вариантом. Firefox нужно запустить хотя бы один раз для создания профиля. Системный DNS/TLS не меняется; отключение восстанавливает управляемые настройки и сохраняет сторонние изменения. Новую скорость запуска и параллельные столы необходимо проверить на устройстве.

---

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
[Проверки и ограничения](VALIDATION-v1.7.34-x11-test.md). Ниже — исторические этапы.

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
[Проверки и ограничения](VALIDATION-v1.7.33-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 certificate/progress update 2026-10-20 -->
Текущая частная сборка: **v1.7.32-x11-test / code1247**.
В базовую установку и проверку X11 Debian/Ubuntu добавлены `ca-certificates` и
проверка непустого хранилища доверенных сертификатов. Firefox также подготавливает
их перед HTTPS-репозиторием Mozilla; проверка TLS и подписей не отключается.
Окна установки рабочего стола и программ показывают после процентов полученный
объём и общий размер, если APT сообщает их. Это округлённые измерения APT, не
пересчёт числа файлов в байты. При распаковке объём скрыт, прогресс этапа остаётся.
Несколько одновременных столов и отдельные новые окружения в эту сборку не входят.
[Проверки и ограничения](VALIDATION-v1.7.32-x11-test.md). Ниже — исторические этапы.

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
[Проверки и ограничения](VALIDATION-v1.7.31-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная сборка: **v1.7.30-x11-test / code1245**.
Совместимость PRoot — галочка в карточке Firefox под «Установить / Установлено»
в рекомендуемых и установленных. Нажатие открывает пояснение и подтверждение.
Закройте только Firefox: рабочий стол для переключения останавливать не нужно.
Новых значков, панельных ярлыков и команд не добавлено. Сохранён прежний запуск
через меню Firefox с подтверждённым обходом seals; обычный бинарник не изменяется.
[Проверки и ограничения](VALIDATION-v1.7.30-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная сборка: **v1.7.29-x11-test / code1244**.
Firefox → «Совместимость с PRoot» доступна и в рекомендуемых, и в установленных.
После установки браузера остановите рабочий стол, откройте этот пункт и подтвердите
включение. Пояснение описывает ослабление защиты общей памяти; параметр применяется
только при запуске Firefox через его ярлык. Там же можно отключить режим.
Временное удаление LXQt убрано; три известных остаточных ярлыка без резервных копий
убираются при следующем запуске Test-стола. Исправление прав ярлыков сохранено.
Обычный запуск пропускает лишнее декодирование фона, не меняя выбранные картинки.
[Проверки и ограничения](VALIDATION-v1.7.29-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная проверка: **v1.7.28-x11-test / code1243**.
Оставлен XFCE; выбор LXQt, диагностика и плашка Garcon убраны с панели.
«Программы рабочего стола → Установленные»: исправление прав ярлыков и временное
удаление LXQt с подтверждением после остановки стола. Общие библиотеки и настройки
сохраняются. В рекомендуемых — Firefox из подписанного репозитория Mozilla без
Snap; запуск в PRoot ещё проверяется. [Проверки и ограничения](VALIDATION-v1.7.28-x11-test.md).
Предыдущие сведения ниже относятся к историческим этапам.

## Ubuntu X11 Test · repair launcher permissions

Stop desktop -> desktop card -> Repair launcher permissions -> confirm -> wait -> launch. Only matching package .desktop launchers get owner read/write; original modes are backed up under ~/.local/state/termux-jobs. Existing program contents/execute bits stay intact. Run again manually if a later installation introduces readonly launchers. Experimental Garcon filter is unavailable.

## Test XFCE stabilization / LXQt keyboard

Stop desktop before toggling XFCE menu stabilization; compare off/on after relaunch. Diagnostics work in both modes. LXQt needs component check/install again for GVFS/icon/SVG additions; invoke keyboard with the top interactive-input button. Existing shared Desktop shortcuts are preserved.

## Ubuntu X11 Test · XFCE / LXQt

Stop desktop → desktop card → Desktop: XFCE / LXQt · Openbox → check/install → launch. Existing applications stay installed in the same Ubuntu. For XFCE flicker, restart desktop after update, enable diagnostics, reproduce, stop and export. LXQt rendering/input still needs device verification.

# Счётчики GTK · 1.7.24-x11-test

Для Ubuntu X11 Test добавлен небольшой диагностический модуль панели и рабочего стола: draw — рисование меню, add/remove — изменение пунктов, reload — запрос перезагрузки Garcon, enter/leave/motion — события указателя. Сбор счётчиков включается существующей кнопкой наблюдения; текст пунктов и содержимое событий не читаются. Модуль подключается только к новым процессам панели и рабочего стола тестового окружения, не заменяет GTK/Garcon и не меняет настройки. Это диагностика причины мерцания; исправление пока не заявляется. [Проверки и ограничения](VALIDATION-v1.7.24-x11-test.md). Ниже — предыдущие этапы.

# Наблюдение X11 и закрытие сессий · 1.7.23-x11-test

X11 запоминает сессию и конкретный запуск, из которого запущен рабочий стол. При её закрытии или завершении оболочки останавливаются графический стол и его программы. Закрытие другой сессии, смена вкладки и возврат в терминал стол не останавливают. Перед закрытием добавлено предупреждение о несохранённой работе. Наблюдение дополнено счётчиками изменений содержимого меню XDamage и движения указателя, без чтения изображений и текста ввода. Причина мерцания пока не установлена. Убрано наследование допклавиш и их подсветки между чатами агентов и обычным терминалом; Согласование прокрутки с новой высотой сообщений перенесено перед отрисовкой кадра для всех чатов агентов, без передачи фокуса из поля ввода. [Проверки и ограничения](VALIDATION-v1.7.23-x11-test.md). Ниже — предыдущие этапы.

# Наблюдение X11 · 1.7.22-x11-test

В «Диагностике X11» запись запускается и останавливается вручную, без прежнего ограничения в три минуты. Закрытие панели запись не останавливает; новый запуск заменяет текущую запись в памяти. Добавлены время событий, категории всплывающих окон, состояние кнопок мыши и отдельный последний снимок настроек фона. При потере поверхности отменяются удерживаемые кнопки и отложенное перетаскивание. Это диагностика, устранение мерцаний пока не подтверждено. [Проверки и ограничения](VALIDATION-v1.7.22-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.21-x11-test

Диагностика X11 переведена на компактную панель сессий: «Сводка»/«События», закреплённые действия копирования и экспорта в Download. При смене вкладки сохраняется соединение рендерера, пересоздаётся только Android-хост поверхности. Для фона используется постоянное имя X11-экрана screen; прежняя выбранная картинка переносится только при отсутствии собственного выбора у этого имени. [Проверки и ограничения](VALIDATION-v1.7.21-x11-test.md). Ниже — предыдущие этапы.

# Диагностика X11 · 1.7.20-x11-test

Исправлена полная пересборка верхней панели при смене состояния X11-ввода: обновляется только подсветка кнопки, название и навигация сохраняются. Добавлены ограниченный журнал событий всплывающих X11-окон/фокуса и метаданные Android-поверхности/клавиатуры. Это диагностика причины Applications, а не заявление об устранении мерцания. Проверяется декодирование выбранных фонов; только отсутствующий штатный фон заменяется встроенным нейтральным PNG. Выбранные пользовательские файлы сохраняются. В «Диагностике X11» есть обновление, копирование и экспорт в Download. [Проверки и ограничения](VALIDATION-v1.7.20-x11-test.md). Ниже — предыдущие этапы.

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

# Текущая проверка: 1.7.10-x11-test/code1225

Клавиатура X11: ручное переключение, снятие подсветки после закрытия и
пробные сигналы фокуса Fcitx5 для GTK3/Qt5. Добавлены явный запуск XFWM4 без
композитинга, детали запуска и возврат в плашку после проверки/установки.
[Состав и проверка1.7.10](VALIDATION-v1.7.10-x11-test.md).
Ниже — предыдущие записи. GitHub остаётся v1.7.5.

# Текущая проверка: 1.7.9-x11-test/code1224

Управление X11 пальцем как тачпадом вместо постоянно зажатой левой кнопки.
XFCE предыдущей1.7.8 запущен и подтверждён разработчиком. Подробности:
[проверка1.7.9](VALIDATION-v1.7.9-x11-test.md).
Ниже — предыдущие записи. GitHub остаётся v1.7.5.

# Текущая проверка: 1.7.8-x11-test/code1223

Исправлена проверка служебной папки X11 (`CONTROL_PATH`): сравнение файловой
идентичности вместо текста путей Android. Внутреннее меню X11 и окна установки
используют общую тему и карточки агентов. Состав и проверка:
[VALIDATION-v1.7.8-x11-test.md](VALIDATION-v1.7.8-x11-test.md).
Ниже — записи предыдущих сборок; GitHub остаётся v1.7.5.

# Текущая проверочная сборка: 1.7.7-x11-test

Рабочий стол в области выбранной сессии; явная Ubuntu X11 Test
(`jobs-x11-ubuntu`), проверка/подтверждённая установка XFCE с прогрессом,
возврат в терминал и отдельная остановка. Пакеты — APT выбранной Ubuntu;
терминал и рабочий стол не отрисовываются одновременно. Реальный XFCE этого
этапа ещё проверяется на устройстве; прочие дистрибутивы не перенаправляются.
Подробности: [VALIDATION-v1.7.7-x11-test.md](VALIDATION-v1.7.7-x11-test.md).
Стабильный GitHub остаётся v1.7.5. Ниже — предыдущие записи.

# Проверочная1.7.6: встроенный X11

Плашка «Рабочий стол X11» у обычных локальных сессий открывает проверочный
графический экран в том же приложении. Ubuntu/XFCE пока не устанавливаются.
Меню плашки X11 сверху: «Вернуться в терминал» сохраняет графическую сессию,
«Остановить графический рабочий стол» завершает её после подтверждения.
Порядок проверки/ограничения: [X11-INTEGRATION.md](X11-INTEGRATION.md).

---

# 1.7.5-test: предыдущие сообщения

«Предыдущие сообщения» показывает загрузку и подгружает старую страницу выше
текущих строк. При занятом фоне нажатие ждёт текущего чтения, а не теряется.
Следующее обновление не убирает старые Codex-строки. После конца истории кнопка
скрывается. Ошибка подгрузки оставляет чат открытым и позволяет повторить.
Если локальное окно истории истекло, кнопка предлагает загрузить предыдущие
заново с обновлённого cursor. Основной чат/черновик сразу возвращаются из кэша
как прежде; подтверждения действий заново читаются из действующей сессии.

Правку адаптера Codex получает новый процесс сессии: после обновления APK
перезапустите соответствующую сессию с сохранённой историей. Рабочие процессы
автоматически не перезапускаются. Проверить на телефоне: подгрузить несколько
страниц, переключить вкладку и вернуться, убедиться в сохранности ввода/чата.

Если новая вкладка выдаёт thread/read -32600 для ID без файла истории, откройте
настройки этой сессии и выберите «Новый чат» либо другой сохранённый разговор.
Приложение не подменяет выбранный ID автоматически. Новые пресеты больше не
привязываются автоматически к new thread при explicit materialized=false в статусе.
Сохранённый выбор «Новый чат» теперь защищён от статуса прежнего запуска.
Он применяется при следующем запуске этой сессии; текущий разговор остаётся
действующим до обычного закрытия/перезапуска пользователем.
Файл/папка проекта и регистр имени при этом не изменяются.

---

# 1.7.4-test: удаление целых окружений и сканирование в окне

В очистке можно отметить целиком одно или несколько Linux-окружений: исходные
и восстановленные. Либо открыть «/ · Система» и нажать «Удалить это окружение
целиком». Удаляется весь контейнер с HOME и пакетами, через proot-distro remove;
нужен менеджер с поддерживаемыми блокировками. Занятые среды не удаляются,
сессии не останавливаются. Пресеты и внешние архивы остаются. Перед запуском —
обязательное согласие и предупреждение, что native removal нельзя отменить.
Отдельный пункт для восстановленных копий убран из очистки: они в общем списке.
Сам PREFIX Termux по-прежнему только для просмотра, как runtime приложения.

Сканирование теперь прямо в окне, как в бэкапе: «Обновить» справа от заголовка
заменяется кружком, под ним одна строка прогресса. Отмена закрывает окно и отменяет
только принадлежащий ему сканер. После завершения доступен новый состав.
Подробности — [STORAGE-CLEANUP.md](STORAGE-CLEANUP.md).

---

# 1.7.3-test: полный обзор очистки

В «Очистить хранилище» теперь доступны отдельные разделы команд, заметок,
пресетов, недавних команд, историй оболочек и HOME/PREFIX/системной части каждого
окружения. Нажмите раздел, чтобы увидеть состав, размеры и выбрать данные.
HOME — выбор папок и файлов; библиотека — конкретных записей. PREFIX и системные
деревья доступны для просмотра: полный сброс работающего runtime не выполняется.
Кэши/отчёты/архивы остаются ниже. Удаление восстановленных окружений теперь только
здесь, из меню бэкапа дублирующая строка убрана. Подробности, предупреждения и
границы — [STORAGE-CLEANUP.md](STORAGE-CLEANUP.md).

---

# Проверочная сборка 1.7.2-test: очистка и история пакетов

Шестерёнка → «Очистить хранилище» находится под «Настройки приложения».
Выберите найденные категории → «Продолжить» → проверьте список и отметьте
«Данные могут быть потеряны навсегда» → «Удалить». Без галочки удаление
заблокировано. Окна закрываются кнопкой «Отмена», не тапом снаружи/Back.
Отмена во время очистки не возвращает уже удалённое. Подробности/границы —
[STORAGE-CLEANUP.md](STORAGE-CLEANUP.md).

В окне пакетов на вкладке «Установленные» слева от «Закрыть» — «История».
Размеры самого окна пакетов сохранены; журнал открывается отдельно.
История общая для сессий/пресетов одного конкретного rootfs и начинается с
первого снимка установленного списка в этой версии. Последние300 записей.
Действия нашего менеджера показывают код завершения, ошибку/отмену либо
отсутствие результата. Код1 некоторых менеджеров не различает отказ и ошибку;
поэтому это не выдаётся за однозначную причину. Raw terminal output не сохраняется.
Прочие установки/удаления/смены версий обнаруживаются при перечитывании
установленного списка; дата означает время обнаружения. Ошибки произвольных
команд вне нашего менеджера так восстановить нельзя. Результаты пакетных команд
собираются при открытии истории/списка и завершении сессии, без постоянного polling.

Выбор версий APK/DNF/Zypper из1.7.1-test включён. Ниже — предыдущие сведения.

# Проверочная сборка 1.7.1-test: версии пакетов

Откройте «Установка пакетов» выбранной сессии → пакет → «Выбрать версию».
Поддержаны APT и теперь APK/DNF/Zypper. Выберите точный вариант из локального
каталога, проверьте команду/предупреждения и выберите свободную текущую оболочку
либо новую сессию того же окружения. Пакетный менеджер покажет план в терминале.
Для Alpine точная версия закрепляется в world. Если версия одна или отсутствует,
это указано явно; устаревший каталог обновляется отдельной кнопкой.
Void и Arch сохраняют прежнее поведение. Подробнее — ENVIRONMENTS.md.

Ниже документация стабильной1.7.0 и история прежних изменений.

# v1.7.0: окружения, пакеты и удаление восстановленных копий

Создание сессии/пресета → «Окружения Termux» → «Окружение»: выберите систему.
Справа «Установить» предлагает загрузку с подтверждением и прогрессом; серое
«Установлено» не мешает выбору строки. «Проверка среды запуска» проверяет наличие
системы и оболочки. Ниже показана точная команда ручного входа из Termux.
Ubuntu остаётся общей с AI Agents; разделение ubuntu-ai не выполнялось.

«Установка пакетов» в сессии читает репозитории выбранного экземпляра. Основные
менеджеры: APT/APK/DNF/Zypper/XBPS/Pacman. Перед действием показывается команда,
затем выбирается свободная текущая или новая сессия того же окружения.
У Arch «Обновить систему» и установка используют полный pacman -Syu — просмотрите
план. Выбор конкретной версии в панели пока APT-only.

Шестерёнка → «Резервное копирование»: Linux выбирается по экземпляру. Восстановление
создаёт jobs-* и отдельный пресет; исходная система не заменяется. Путь HOME
относится к отчётам/staging, rootfs размещается в хранилище proot. Новые launch
метаданные минимальны: OCI Entrypoint/Env не импортируются, rootfs-файлы сохраняются.

«Удаление восстановленных окружений» показывает подтверждённые копии, источник,
размер и отметку незавершённого восстановления. Подтвердите необратимое удаление.
Закройте её сессии: штатный инструмент блокирует занятую среду, без force/kill.
После успеха убираются связанные пресеты. Архивы Download, исходники и отчёты
сохраняются. Старые copies без записи о происхождении видны, но удаление недоступно;
для нового restore происхождение записывается до native операции. Если proot-distro
не имеет поддерживаемых session/removal locks, удаление блокируется до обновления.
После подтверждения удаления отмены нет; окно защищено от случайного закрытия.

[Окружения](ENVIRONMENTS.md) · [Бэкапы](BACKUP-RESTORE.md).

Ниже сохранено описание прежних функций.

# v1.6.5: чаты агентов и выбор файлов

Нажмите «Хронология»/«В работе», чтобы раскрыть действия, затем нужный шаг —
для исходных подробностей. Ошибка конкретной команды не означает сбой приложения.
Плашка навыка появляется только при распознаваемом чтении или подключении;
Gemini не получает выдуманный навык из имени инструмента. Во время ответа можно
продолжать набирать текст: обновление чата не должно забирать фокус ввода.
Для OpenCode готовность CLI проверяется в тех же каталогах, что используются
запуском. Разработчик подтвердил исправление повторного ложного предупреждения.


Откройте/создайте сессию агента или запустите сохранённую историю: новые запуски
Codex в Ubuntu, Gemini, Claude и OpenCode используют чат приложения. Карточка
модели/режима остаётся сверху; раскрываемые плашки содержат рассуждения/действия.
Робот открывает терминал с крупными клавишами. «Свернуть» скрывает окно;
«Вернуться в чат [агент]» возвращает управление без ручного /exit. Во время
активной работы завершение терминального интерфейса может ждать окончания
запроса или потребовать повторить действие после завершения. Ввод Shell включается
интерактивной кнопкой и выполняет команду напрямую с правами окружения, не модели.

Перед запуском проверяется наличие компонентов; их установка предлагается с
подтверждением. Галочка готовности не означает успешный вход/доступную квоту.
Gemini Vertex требует отдельные Google Cloud project/IAM/API/billing. Claude и
Vertex полностью с реальными аккаунтами ещё не проверены.

Файлы: долгий тап включает выбор, галочки меняют его, обычный тап открывает папку.
«× N» снимает выделение, «Выбрать всё» учитывает текущий поиск/фильтр. Стрелка
назначения предлагает копировать/переместить сюда. Это одно окружение/профиль;
существующие объекты автоматически не заменяются. На сервере общий выбор
применяется к скачиванию/перемещению/удалению. Частичный результат не откатывается.
[Подробно](FILE-SELECTION.md) · [агенты](AI-AGENTS.md) · [релиз](RELEASE-v1.6.5.md).

## Earlier usage notes

---

# Использование v1.5.3

Стабильный интерфейс описан в [описании выпуска](RELEASE-v1.5.3.md).
Локальных ИИ больше нет; перед обновлением сохраните нужные приватные модели/чаты.
Внешние AI Agents и обычные окружения остаются.

**Сервер:** корзина после «Домой» включает выделение. Повторный пустой тап
отменяет режим; выбранные объекты удаляются только после Да/Нет. Папки удаляются
с содержимым, частичное удаление не откатывается. Долгий тап подходит также для
скачивания/перемещения. «В фон» скрывает прогресс передачи, не отменяя её;
строка над «Открыть» возвращает окно. Живой процесс приложения необходим.

**Бэкап:** в полном и «По выбору» справа от названия есть «Обновить».
В процессе видны кружок и одна строка текущего файла/объёма. Повторный вход
может использовать сохранённую в памяти оценку; ручное обновление всегда свежее.
Выбор компонентов/папка сохраняются, согласие подтвердите заново.
Способ gzip1/pigz2/pigz4 выбирайте отдельно от уровня1/3/9.
Старый proot-distro может использовать штатное сжатие полного архива Ubuntu.
Создание архива всегда проверяет файлы заново. См. [бэкапы](BACKUP-RESTORE.md).

Линия над терминалом показывает вывод/доступные признаки активности,
не процент готовности. Её анимация не пересоздаёт страницы.

## Исторические дополнения

Ниже сохранено описание развития предыдущих версий. Ссылки на локальные ИИ
относятся к удалённому эксперименту, см. [исследование](LOCAL-AI-RESEARCH.md).

---

## Historical private v1.5.27-test: обновление размеров и сжатие

«Обновить» находится справа в шапке полного и выборочного бэкапа.
Во время сканирования текст заменяется кружком; под шапкой меняется одна
строка текущего файла/обработанного объёма. Отдельное окно сканирования не открывается.
При повторном входе используется последняя действующая оценка из памяти
приложения; «Обновить» всегда запрашивает свежий обход. Закрытие окна отменяет
его сканирование. Выбор компонентов и папка назначения сохраняются при обновлении.

Способ сжатия выбирается отдельно: gzip1 поток / pigz2 потока / pigz4 потока.
Ниже — уровень: быстрое / сбалансированное / максимальное. При отсутствии
pigz остаётся предложение установить его с подтверждением. Все варианты — .tar.gz.

Первая оценка может занимать время. Действующий кеш ускоряет повторный вход
в пределах живого процесса приложения; после завершения процесса нужна новая
оценка. При изменениях файлов запрашивайте «Обновить». Создание архива всегда
проверяет исходные файлы заново. Для старого proot-distro полный архив Ubuntu
по-прежнему использует штатное сжатие независимо от выбранного режима.

## Historical private v1.5.26-test: оценка размеров

Ускорено сканирование полного и выборочного бэкапа: меньше повторных
чтений метаданных, скомпилированный поиск категории, временная ограниченная
сводка для пересчёта при добавлении папки без повторного обхода всех файлов.
Кнопка «Обновить размеры» всегда запускает свежую оценку. Кеш не является
фоновым сервисом и не используется для чтения/проверки файлов архива.
Прокрутка терминала теперь сохраняет уже активную линию, а при простое
не запускает её одной перерисовкой. Оба индикатора остаются примерно60fps.

## Historical private v1.5.25-test: клавиатура

При открытии/закрытии клавиатуры уже активная полоса больше не сбрасывается:
изменение размера отделено от прокрутки/смены сессии. Новый вывод активной
сессии учитывается без паузы1600мс. Сам resize не продлевает активность;
в простаивающей сессии краткая перерисовка всё ещё фильтруется.
Толщина1dp, серверные полосы прогресса и остальные функции1524 сохранены.

## Historical private v1.5.24-test: полоса активности и передач

Над терминалом — полоса1dp. Прокрутка и смена вкладок/размера фильтруют
краткий вывод перерисовки; это не влияет на вывод, команды и прокрутку.
Новые данные после1600мс снова включают сигнал. Полоса не является процентом
выполнения задачи. Верхние кружочки убраны, прежние Toast других заголовков
возвращены. В карточке активной передачи сервера теперь есть заполняющаяся
полоса; при неизвестном размере она показывает неопределённый прогресс.
Нажатие карточки возвращает прежнее окно с отменой и «В фон».

# Руководство Termux Job’s

## Фоновые передачи и индикаторы v1.5.23-test

В окне скачивания или отправки нажмите «В фон», чтобы продолжить пользоваться
терминалом и другими вкладками, сохранив передачу. В карточке сервера над
«Открыть» появится строка соответствующего направления с текущим прогрессом;
внутри выбранного сервера такие строки тоже доступны. Нажатие возвращает то же
окно, где остаются отмена и «В фон». При завершении строка исчезает. Рамка
«Сервер + агент» движется по часовой, пока идут передачи. Можно одновременно
отправлять и скачивать; для каждой стороны действует прежний один пакет операций.

Это фон внутри живого процесса приложения: новый Android foreground service
не добавлен. Закрытие/уничтожение Activity или процесса не гарантирует продолжение
и не возобновляет передачу автоматически. Отключение профиля останавливает её.
Успешное фоновое завершение сообщает кратко; ошибки и запросы совпадающих имён
по-прежнему требуют внимания. Лимиты передачи и проверка SSH-ключа хоста прежние.

Над чатом терминала — тонкая линия активности. Для обычной оболочки учитывается
доступный локальный foreground job; для всех типов — недавний вывод PTY, в том
числе анимации CLI. У SSH и агентов без общего API это сигнал активности вывода,
не точное доказательство выполнения или процента готовности. Тихая удалённая
команда может не дать сигнала. Дополнительных запросов к ИИ ради полоски нет.

Над вкладками по центру — другие сессии: кружочки, краткая плавная смена на
событие заголовка (без частых дублей), затем возврат к индикатору. По нажатию —
список других сессий с текущими заголовками и переходом. Старый серый Toast
изменения заголовка убран. Робот в списке запущенных означает профиль AI Agents,
а не наличие установленной локальной модели.


## Локальная v1.5.23-test: корзина в файлах сервера

Корзина находится сразу после домика. Нажмите её, чтобы включить выбор:
нажатия по файлам и папкам теперь выделяют их. Если ничего не выбрано,
повторное нажатие корзины отменяет режим. Если выбрано — откроется список
объектов и подтверждение «Да / Нет». Выделять можно и прежним долгим тапом.
При выбранных объектах обводка корзины и кнопки скачивания/перемещения
подсвечена; у пустого режима выбора подсвечена только корзина.

Удаление безвозвратное: папки включают всё содержимое. Символические ссылки
удаляются без удаления их целей. До 500 выбранных объектов/20 000 вложенных
записей/64 уровней; проверка и удаление ограничены25 секундами. Нужен Linux
с Python3 и права SSH-пользователя. Обновлять установленный агент не нужно.
При ошибке/обрыве часть файлов может уже отсутствовать: проверьте обновлённый
список, автоматического повтора нет.


## Локальная v1.5.22-test: удаление локальных ИИ

В этой сборке больше нет локальных моделей и их сессий. Перед обновлением
закройте их и экспортируйте нужные локальные чаты/модели: приватные данные
этого раздела удаляются при первом запуске. Остальные сессии, команды,
заметки, серверы и истории CLI-агентов сохраняются. Пункт «Истории чатов»
продолжает работать с поддерживаемыми историями Codex.
Отдельная Ubuntu `jobs-local-ai` удалена на устройстве разработчика вручную;
APK не удаляет установленные proot-окружения других пользователей.


## Локальная v1.3.1: звёздочка

В терминале звёздочка → команда/заметка → полный текст с прокруткой.
«Вставить в сессию» заменяет поле текущей вкладки, не отправляя текст.
«Редактировать» открывает существующий редактор; «Удалить» требует подтверждения.
Закрытие просмотра оставляет черновик без изменения. Поиск и переключение списков
сохранены. Справочник в общих настройках содержит126 записей, с AI-подписью.

## Новое в v1.3.0

Общие настройки → «Ошибки и решения»: поиск и фильтры114 карточек RU/EN,
источники и условия применения. База будет пополняться в обновлениях APK;
автоисправлений и фонового сканирования нет. [Справочник](ERRORS-AND-SOLUTIONS.md).
В серверном профиле кнопка папки выбирает ключ, passphrase вводится отдельно.
Для передачи файлов выбирайте Termux, установленное окружение или память
устройства. До50 файлов за отправку; скачать можно в Download или другую папку.
[Порядок выбора и разрешения](LOCAL-FILE-SOURCES.md).


## v1.2.1 — ускорение отправки и очередей сервера

SFTP отправляет до8блоков по одному SSH-соединению; изолированы очереди
мониторинга и поиска/диагностики. Разработчик подтвердил ускорение отправки
примерно4–5раз на своём подключении; результат зависит от сети и сервера.
Защиты файлов/отмены сохранены, данные без сброса, агент прежний.

[Изменения](RELEASE-v1.2.1.md) · [Проверки](VALIDATION-v1.2.1.md).


## Сервер + Agent (v1.2.0)

Откройте Терминал → «Сервер + Agent» справа под пакетами/папкой. Создайте профиль,
подключитесь и сверьте отпечаток SSH. Агент устанавливается отдельным подтверждением
через настройки сервера → настройки агента → проверить состояние. Нужен OpenSSH
в Termux и Linux/Bash/Python3.8+ на сервере. Обычный вход «Создать сессию → SSH ·
сервер» доступен как прежде без агента.

Проекты/Задачи/Файлы/Службы доступны в выбранном сервере. В файлах звёздочка
управляет избранными папками/файлами: добавьте текущую папку или выберите файл →
В избранное. Лупа ищет имена в каталоге/подпапках с ограничением объёма. На узком
экране строка значков прокручивается. Просмотр текста → Редактировать; сохранение
откажется перезаписывать изменённый файл. Передача/видео запускаются явно.

Задачи: запуск с подтверждением, журнал, закреплённые команды, удаление завершённой
записи. «По расписанию» использует Cron/user-systemd без установки пакетов/sudo/linger.
Расписания остаются после удаления агента/профиля. Перед удалением профиля управляйте
ими через Задачи → Расписания. Отключиться закрывает мониторинг/туннель, не задачи.

[Полное руководство](SERVERS.md) · [Расписания](SCHEDULES.md) ·
[Все изменения и ограничения](RELEASE-v1.2.0.md).


Текущий выпуск: v1.2.0 (Android versionName1.1.4/versionCode1071). AI Agents экспериментальны; диагностика, пресеты и обновлённое управление включены.

## Язык и навигация в v1.0

Общая шестерёнка на всех вкладках содержит «Язык / Language» и настройки приложения. В выборе языка: «Системный», «Русский», English. Отдельного глобуса нет. При системном русском используется русский; для остальных языков — английский. Выбор запоминается и действует при следующем запуске. Терминалы и их процессы не перезапускаются, незавершённый ввод остаётся в каждой вкладке.

Три верхние плитки сохранили размер: дом — главная, книга — команды/заметки, терминальный значок — терминал. Подписи и подсказки по долгому тапу не отображаются. Системное описание кнопок для доступности остаётся невидимым.

Интерфейс и сообщения приложения локализованы, в том числе лимиты, выбор модели и уровни рассуждения Codex. Имена моделей, сохранённые пользователем имена/категории, заметки, команды, пути, вывод терминала и ответы Codex остаются исходными. Журнал записывает отправленный кнопкой текст, а не черновики или прямой интерактивный ввод.

## Звёздочка и недавние команды в v0.2.17

Звёздочка у поля ввода открывает вкладки «Сохранённые команды» и «Заметки». В обоих списках избранные идут первыми. Поиск проверяет название, содержимое и категорию, без учёта регистра. Каждый поисковый запрос сохраняется при переключении вкладок внутри открытого окна. При новом открытии окно начинается с команд и пустого поиска.

Нажатие на элемент заменяет содержимое поля ввода текущего терминала полным текстом команды или заметки, сохраняя переносы строк. Самостоятельно текст не отправляется и в журнал не попадает. «Сохранить ввод» сохраняет текущий ввод как команду.

У недавних команд на главной нет отдельного троеточия: нажмите на карточку для просмотра полного текста, вставки, копирования или сохранения в библиотеку.

## Адаптация интерфейса в v0.2.17

Размеры учитывают плотность экрана и системный шрифт. На просторном экране оба списка прокручиваются независимо. Если клавиатура или маленькое окно оставляют мало места, появляется общая прокрутка, а панели сохраняют минимальную высоту. Фильтры могут занимать две строки. При переходе между вкладками клавиатура скрывается; поиск открывает её после нажатия на поле.

## Окна и меню в v0.2.17

Создание и настройки сессий, редакторы команд и заметок оформлены тёмными
скруглёнными окнами. Строки действий подсвечиваются при нажатии.
Карточка команды или недавней записи открывает полный текст с прокруткой:
«Вставить в терминал» переносит его в поле, отправку выполняет пользователь.
В недавней записи также доступны «Сохранить в команды» и «Скопировать».

Звёздочка у поля ввода открывает поиск «Найти команду» по названию, тексту и
категории. Избранные показаны первыми, поиск не вызывает клавиатуру до нажатия.
Выбор вставляет текст в текущий терминал; журнал не пополняется при просмотре
или вставке. «Сохранить ввод» доступно для непустого текста поля.

## Команды и заметки

Верхняя вкладка «Команды/Заметки» открывает две постоянно видимые панели
с независимой прокруткой. Общий поиск «Поиск команд и/или заметок» ищет по
названиям, содержимому и категориям обеих панелей. У каждой панели строка
«Все команды» / «Все заметки», «Избранное», «Категории» и кнопка «+» справа.
Категории и избранное у команд и заметок независимы.

Заметка содержит название, многострочный текст, категорию и признак избранного.
Нажатие на карточку показывает текст целиком; действия в этом окне позволяют изменить
избранное, редактировать или удалить с подтверждением. Заметки не выполняются
в терминале. Многострочный редактор команд и вставка через звёздочку прежние.

Библиотека обновляется до формата v8, старые v1–v8 читаются. Возврат к более
старому APK после создания заметок не поддерживается без резервной копии:
старый код не умеет читать новый формат (исходный файл не перезапишет).

## Сохранить и вставить команду

1. Откройте «Команды/Заметки» и нажмите «+» справа от фильтров команд.
2. Укажите название, текст, категорию и при необходимости избранное.
3. Нажмите на карточку, затем «Вставить в терминал»; выберите терминал, если их несколько.
4. Проверьте текст в поле ввода, измените при необходимости и отправьте кнопкой.

Звёздочка возле поля ввода выбирает команду для текущего терминала.
В окне просмотра можно изменить избранное, отредактировать или удалить запись.
Сохранённая команда не запускает новый терминал. Переносы строк сохраняются.

Для длинной автоматизации используйте собственный скрипт, а в библиотеке
сохраните его запуск. Пресеты выполняются с правами текущей оболочки.

## Создать сценарий сессии

Нажмите «+» у пресетов запуска на главной. Задайте имя, рабочий каталог,
среду и до трёх стартовых команд. Создание профиля само по себе не запускает его;
для запуска нажмите карточку. Если сессия уже работает, карточка открывает её вкладку.

Чтобы сохранить обычную открытую сессию, удерживайте её вкладку в разделе
«Терминал» и выберите «Добавить в пресеты запуска». Без этого действия обычные вкладки
не попадают в сохранённые автоматически. В окне новой сессии кнопка «Пресеты»
слева от «Отмена» открывает список профилей для запуска.

### Пример для Termux

Среда: Termux. Каталог: существующий каталог проекта в Termux HOME.
Шаги, если Python уже установлен:

```sh
python --version
```

```sh
python main.py
```

Каждый блок — отдельное поле команды. Шаги выполняются в одной оболочке,
поэтому изменения `cd` и `export` действуют на следующие шаги.
Каталог должен существовать; приложение не скачивает проект и не устанавливает зависимости.

### Пример для Ubuntu

Ubuntu должна быть установлена через `proot-distro` под именем `ubuntu`,
внутри неё должен быть `/bin/bash`. Выберите среду Ubuntu, укажите существующий
гостевой каталог, например `/root/projects/my-project`, и команду `codex`,
если Codex уже установлен и доступен в PATH.

Приложение само входит в Ubuntu. При выборе этой среды не добавляйте
`proot-distro login ubuntu` первым шагом. Каталог Termux и гостевой каталог
Ubuntu могут иметь разные пути.

### Как работает ожидание

В режимах Termux/Ubuntu запуск начинается после инициализации Bash.
Команды идут последовательно. Ненулевой код завершения останавливает дальнейшие шаги.
Если программа остаётся интерактивной, следующий шаг ждёт её выхода.
Это не механизм автоматического ввода в любой интерфейс или запрос пароля.

Кнопка отмены внутри плашки текущей сессии отменяет будущие шаги. Текущую
программу он не останавливает. Новый запуск профиля сбрасывает отмену.
Для предсказуемой последовательности используйте Termux/Ubuntu;
режим «Как раньше» не проверяет флаг отмены.

## Закрытие, перезапуск и удаление

| Действие | Результат |
| --- | --- |
| Закрыть сохранённую | Останавливает процесс, закрывает вкладку, сохраняет профиль и историю Bash |
| Сохранить и закрыть | Сохраняет обычную сессию и её историю Bash, затем останавливает и закрывает терминал |
| Закрыть без сохранения | Останавливает терминал, затем удаляет его профиль и историю Bash |
| Перезапустить | Завершает прежний процесс, запускает профиль заново |
| Удалить сохранённую сессию | Удаляет профиль и его историю; активный терминал сначала нужно закрыть |

Закрытие и перезапуск требуют подтверждения. Для несохранённой сессии доступны
«Сохранить и закрыть» и «Закрыть без сохранения», без кнопки «Отмена».
Закрытие не удаляет историю Codex: она хранится отдельно в `.codex`.
Снимки терминала не создаются. Сохранение профиля не сохраняет
живой процесс: после завершения работы нужно запустить сценарий заново.

## Журнал, клавиатура и ввод

Журнал на главной записывает отправки кнопкой из поля ввода. Лимит — 50 записей в v0.2.17. Повторы записываются отдельно.
Можно искать, вставлять повторно, копировать, сохранять в библиотеку
и очистить журнал с подтверждением. Библиотека при очистке остаётся.
Прямой ввод в TerminalView не попадает в журнал приложения.

Enter в поле ввода переносит строку; отправка выполняется кнопкой.
Если программе нужен прямой ввод, включите векторную кнопку интерактивного ввода вверху терминала.
Например, это требуется для интерактивных редакторов и некоторых запросов пароля.
Не сохраняйте пароли и токены в пресеты или журнал команд.

## Если что-то не работает

| Симптом | Что проверить |
| --- | --- |
| APK не обновляет установленный Termux | Совпадают ли пакет и подпись; чужая подпись не подходит |
| Ubuntu не открывается | Наличие proot-distro, контейнера `ubuntu`, Bash и гостевого каталога |
| Программа не найдена | Установлена ли она именно в выбранной среде и доступна ли в PATH |
| Второй шаг не запускается | Завершился ли первый; не вернул ли ошибку; не включена ли отмена |
| Сессии нет среди сохранённых | Выполнено ли явное сохранение через долгий тап или «+» на главной |
| В журнале нет команды | Была ли она отправлена кнопкой, а не введена напрямую |
| После перезапуска нет прежнего процесса | Профиль хранит параметры; запустите его заново |

Не удаляйте приложение для диагностики без резервной копии.
Сведения об установке — в [README](../../README.md), отчёт об ошибке —
через [Issues](https://github.com/HezeAI/termux-jobs/issues).

## SSH · сервер (с v0.2.7)

При создании или редактировании сессии выберите «SSH · сервер». Укажите адрес
(без `user@` и порта), пользователя и порт (обычно 22). «Рабочий каталог»
относится к серверу: пустое поле и `~` означают домашний каталог пользователя.
`~/project` и абсолютные пути поддерживаются, включая пробелы.

Введите до трёх команд именно для сервера, без начального `ssh`. Они выполняются
в одной удалённой оболочке Bash, сохраняют `cd` и переменные между шагами.
Следующий шаг ждёт завершения предыдущего; при ненулевом коде дальнейшие шаги
останавливаются. Долгоживущая программа, вложенный SSH или интерактивный Codex
также задержат следующие шаги до своего выхода.

«Оставить подключение открытым» включено по умолчанию: после команд открывается
интерактивный Bash, в том числе после ошибки для диагностики. При отключённом
переключателе SSH завершается и возвращает локальный терминал. При ошибке входа
или недоступном каталоге команды на сервере не запускаются.

Требуется установленный локальный SSH-клиент (`pkg install openssh`, если его нет)
и Bash на сервере. Используются существующие ключи и настройки `~/.ssh/config`.
Пароль, парольная фраза ключа и проверка нового ключа сервера запрашиваются SSH
в терминале; приложение их не хранит и не подтверждает автоматически.
Отмена будущих команд через локальное меню недоступна для удалённого сценария.
История интерактивной оболочки сервера хранится на сервере по его настройкам.
Закрытие сохранённой сессии сохраняет профиль, но не работающий SSH-процесс.

Формат библиотеки повышен до 6. Данные форматов 1–5 читаются; откат на старый APK
не поддерживает редактирование нового формата и потребует резервной копии библиотеки.

## Авторизация SSH (с v0.2.8)

В SSH-профиле «Авторизация» предлагает три варианта:

- «Автоматически»: прежнее поведение SSH, существующие ключи/конфигурация; пароль при необходимости вводится в терминале.
- «Файл SSH-ключа»: путь `/путь/к/ключу` или `~/…`. Кнопка «Выбрать ключ» показывает файлы из локальной `~/.ssh`; путь можно ввести вручную. Приложение не копирует ключ и не хранит его содержимое. Парольная фраза ключа запрашивается SSH в терминале.
- «По паролю»: поле пароля, «Показать пароль» и «Запомнить пароль». При выключенном запоминании пароль хранится только в памяти до одного подключения; при повторном запуске появится новый запрос. Создание сохранённого профиля без пароля допустимо — запрос появится при запуске.

При включённом запоминании в профиле сохраняется AES-GCM ciphertext, а ключ
находится в Android Keystore. Пароль привязан к ID профиля, адресу, пользователю
и порту: после изменения подключения укажите его заново. Имя и команды можно
менять без повторного ввода. Отключение запоминания удаляет сохранённый ciphertext.
После удаления приложения, переноса или восстановления без прежнего ключа
Keystore пароль необходимо ввести заново. Копия профиля не получает сохранённый пароль.

Пароль передаётся OpenSSH через SSH_ASKPASS и одноразовый локальный сокет с
проверкой Android UID; в командной строке, окружении, истории и временных файлах
пароля нет. Канал закрывается после передачи или через 120 секунд. При неверном
пароле выполняется одна попытка, затем подключение завершается без запуска команд.
Этот режим использует SSH password authentication; keyboard-interactive/OTP
не поддерживаются им — используйте «Автоматически» для таких серверов.

При первом входе по паролю новое подтверждение ключа сервера показывается
отдельным окном с запросом SSH. Сверьте отпечаток и нажмите «Доверять» либо
«Отмена». Подтверждение не происходит автоматически. При неверном/изменившемся
ключе сервера действуют проверки OpenSSH. Для поля пароля допустимы до 256
символов без переносов строк.

Формат библиотеки v8 читает версии v1–v8. Старые APK до v0.2.18 не поддерживают редактирование v8.

## Прокрутка при поиске (с v0.2.9)

С открытой клавиатурой списки журнала, пресетов запуска, команд и заметок
сохраняют внутреннюю прокрутку, в том числе при свайпе по карточке. Жест, начатый
внутри списка, остаётся у него и на границе списка не прокручивает всю страницу.
При тесном окне общая прокрутка по-прежнему доступна вне панелей, чтобы добраться
до нижних элементов. Размеры и системное масштабирование не менялись.


## Codex: запуск, история и управление

В окне новой сессии выберите «Среда запуска → AI Agents → Codex», затем Termux или Ubuntu, имя и рабочий каталог. В выбранной среде заранее должны быть установлены и авторизованы Codex CLI и Node.js; для Ubuntu нужны proot-distro и установленная Ubuntu. Авторизация и история берутся из выбранной среды, приложение их не копирует и не поставляет в APK.

Режимы доступа:

- **Полный доступ**: `danger-full-access`, подтверждения `never`.
- **В пределах проекта**: `workspace-write`, подтверждения `on-request`.
- **Только чтение**: `read-only`, подтверждения `on-request`.

Полный доступ относится к доступной Linux-среде и не даёт root Android. Поддержка ограниченного sandbox зависит от Codex и Android/proot; при отказе приложение не переключает режим на полный автоматически.

Для чата доступны **Новый чат**, **Выбрать чат** из истории и **Ввести ID чата**. Продолжение по ID возвращает историю конкретного диалога Codex. Автоматический «Последний» убран: старый профиль с таким вариантом нужно отредактировать. Закрытие вкладки не удаляет историю `.codex`; сохранённый профиль приложения хранит параметры запуска отдельно от истории диалога.

Под терминальными вкладками карточка показывает модель, поведение, состояние и доступные пятичасовой/недельный лимиты. Лимиты относятся к аккаунту, а не к отдельному чату. Статус считывается примерно раз в 30 секунд; «Обновить» запрашивает чтение сейчас. Отсутствующие или устаревшие поля отображаются как «нет данных». Чтение статуса не отправляет запрос модели и не расходует лимит диалоговых ответов. Доступность моделей и значения лимитов определяются сервисом Codex; приложение не увеличивает и не сбрасывает их.

Нажмите модель → выберите модель → выберите поведение → проверьте параметры → **Применить**. Список берётся из Codex, поэтому доступные варианты зависят от аккаунта и версии CLI. Приложение ждёт завершения текущего ответа, затем управляет штатным меню `/model` в терминале. Выбор применяется только к текущему запуску сессии через действие `s session`; незавершённый текст в терминале не заменяется. Успех подтверждается новым штатным сообщением Codex. Неизвестное меню, дополнительное предупреждение или таймаут требуют ручного завершения; отмена останавливает дальнейшие клавиши, уже открытое меню может остаться.

Интеграция проверена с Codex CLI **0.162.1**. Нужны поддержка `app-server --listen unix://…`, `--remote` и Node.js в выбранной среде. Произвольный ручной запуск Codex в обычной Bash-вкладке автоматически не подключает карточку. После обновления со старого выпуска запустите новый терминал типа Codex: уже работающий процесс не получает помощника автоматически. Чат можно продолжить по прежнему ID.

## Черновики терминальных вкладок

У каждой вкладки собственный черновик нижнего поля ввода, включая положение курсора и выделение. Переключение терминала возвращает его текст; переход между разделами и обычное обновление той же сессии не очищают поле. После отправки черновик очищается, при закрытии вкладки удаляется. Черновики хранятся только в памяти и не восстанавливаются после перезапуска приложения.

## Проекты и файлы

В форме сессии кнопка папки рядом с «Рабочий каталог» открывает браузер выбранной среды. Выберите каталог либо «Создать папку»: создаётся пустая папка без автоматического Git-репозитория или кода. Путь можно вводить вручную. Для Ubuntu/Codex Ubuntu он относится к Ubuntu, для SSH — к серверу. Изменение папки выбранного Codex-чата требует подтверждения исходного и нового каталога при сохранении; история не копируется и не сбрасывается автоматически.

«Выбрать чат» показывает все чаты выбранной среды, с поиском и отдельным фильтром папки. «Ввести ID» проверяет доступность истории; если каталог отличается, нужно подтвердить изменение. При новом запуске Codex помощник передаёт ID/каталог/название активного чата; профиль привязывается к этому ID без копирования истории. Сохранённый профиль предлагает продолжить либо создать отдельную несохранённую сессию с новым чатом в той же папке. Сохранение новых профилей по-прежнему через долгий тап или + на главной. Для сохранения уже работающего нового чата дождитесь получения его ID; при отсутствии ID приложение объяснит, что нужно подождать, вместо сохранения неподтверждённого продолжения. У одного каталога может быть несколько чатов.

В терминале слева выбранная сессия раскрывает вертикальную стопку; долгий тап открывает действия. В локальной v1.0.10 рядом кнопка пакетов, а создание новой — первый пункт списка сессий. Справа кнопка папки открывает начальный каталог профиля. Последующий cd внутри оболочки не меняет этот каталог. Браузер не печатает команды в работающий терминал: используются отдельные ограниченные операции, поэтому Codex/сборка продолжают работать.

Фильтры: Все, Текст, Изображения, APK. Папки доступны при любом фильтре. Нажмите файл для просмотра или действий: Поделиться, Копировать путь, В Download. Предпросмотр текста ограничен 256 КБ, изображения — 16 МБ с уменьшением разрешения; экспорт — 256 МБ. Неизвестные бинарные файлы доступны для экспорта. Download требует разрешения Android на хранение; при совпадении имён выбирается другое имя. Передача использует временную копию в приватном кеше и content URI с временным разрешением чтения, не автоматическую установку APK.

SSH использует параметры профиля и отдельное подключение. По паролю — сохранённый пароль Keystore либо отдельный запрос на время браузера; секрет не пишется в команды. Сначала подтвердите ключ сервера обычным SSH-входом: файловый браузер не доверяет неизвестным ключам автоматически. Защищённый пароль/ключ с парольной фразой может потребовать предварительной настройки SSH-agent или входа в терминале. Операции завершаются при закрытии браузера; рабочая SSH-сессия не закрывается.

Terminal — базовая оболочка Termux: нельзя сохранить/удалить/закрыть её через наше меню, доступен перезапуск с подтверждением. Она занимает одну из восьми терминальных сессий. Если после обновления уже заняты все восемь мест, базовая создастся после освобождения места. Выход из Bash вызывает новый запуск; несколько быстрых отказов прекращают автоматический цикл и предлагают ручной перезапуск. Это не защита от принудительного завершения приложения Android или намеренного выхода из всего Termux.

Для привязки новых чатов используется помощник bridge-v0.2.19, запускаемый с новой Codex-сессией после обновления; уже работающие процессы не заменяются. Старый чат можно продолжить по его ID в исходной папке. Совместимость путей сравнивается с нормализованными абсолютными каталогами; при алиасах/символических ссылках выбирайте фактический каталог через браузер.

## История и другой рабочий каталог

«Выбрать чат» теперь открывает Все чаты выбранной среды независимо от выбранной папки. «Чаты этой папки» — дополнительный фильтр, поиск работает по названию и исходному каталогу. Termux и Ubuntu по-прежнему имеют отдельную историю/авторизацию. У каждого чата показаны исходная папка и дата.

Выберите чат → Продолжить выбранный чат. Если папка отличается, проверьте исходный и новый каталоги и подтвердите «Продолжить в новой папке». ID чата и история остаются прежними, файлы не перемещаются; в переписке остаются прежние задачи и пути. Ввод ID проверяется так же. Изменение папки в настройках уже выбранного чата больше не сбрасывает историю на новый чат: при сохранении надо подтвердить соответствующее изменение.

Новая папка применяется при новом запуске, не изменяет рабочий каталог уже запущенного процесса. Сначала создайте/выберите существующую папку. Backend разрешает несовпадение только с сохранённым подтверждением конкретной исходной папки и проверяет эффективный cwd и права ответа app-server. Если история между выбором и запуском стала относиться к третьей папке, потребуется новое подтверждение в настройках. Для нового запуска используется bridge-v0.2.19; старые процессы не заменяются.

Справа в строке сессий — кнопка папки без надписи (доступное описание Файлы проекта). Векторные значки центрированы в кнопках; в выборе папки при создании сессии используются те же стрелка вверх/дом. Выбранный фильтр файлов обозначен фоном, бирюзовой рамкой и жирным текстом, а не одним цветом надписи. Поле ввода, Enter, черновики, базовая Terminal и операции файлов сохраняют прежнее поведение.

## Пакеты окружения

Нажмите кнопку коробки рядом с выбранной сессией во вкладке Терминал. В прежней v1.0.4 пакеты открывались долгим тапом → «Пакеты». Для базовой
Terminal этот пункт также доступен. Диагностика находится в меню сессии,
общая шестерёнка содержит язык и настройки приложения.
Менеджер использует окружение профиля удержанной сессии, даже если активна другая:
Termux, Ubuntu либо среда Termux/Ubuntu выбранного Codex. Ручной вход в Ubuntu
из обычного Bash не меняет окружение профиля; для пакетов Ubuntu используйте
профиль Ubuntu или Codex с окружением Ubuntu. SSH пока не поддерживается.

Установленные пакеты сохраняются в выбранном окружении и доступны всем его
сессиям. Закрытие сессии без сохранения не удаляет пакеты. Termux и Ubuntu
имеют отдельные базы пакетов; установка в одно не устанавливает пакет в другое.

Три списка: Установленные (dpkg-query), Доступные и Обновления (APT).
Поиск фильтрует имена, карточка показывает версию. Список читается из локальных
индексов настроенных репозиториев, без встроенного фиксированного каталога.
Если индексы устарели или отсутствуют, нажмите «Обновить каталог».
«Перечитать список» только перечитывает локальные данные и не скачивает обновления.
После завершения установки вернитесь в менеджер и перечитайте список.

Установка, обновление конкретного пакета, удаление и обновление каталога требуют
подтверждения. В подтверждении выберите «В выбранной сессии» (по умолчанию) либо
«В новой сессии того же окружения». Для выбранной сессии пользователь сам проверяет,
что там свободная оболочка Bash нужного окружения без незавершённого ввода. Если
выполняется команда, работает Codex или редактор, выберите новую сессию.
Приложение не распознаёт автоматически, какая программа сейчас принимает ввод.
Команда отправится именно в удержанную сессию, даже если активна другая.
Если она закрыта, перезапущена или её окружение изменено, отправка блокируется;
откройте менеджер заново либо выберите новую сессию.

При выборе новой сессии команда автоматически запускается в новой, несохранённой
вкладке того же окружения. Затем APT показывает зависимости и при необходимости
спрашивает подтверждение. Нет автоматического -y, purge, autoremove или обхода
блокировок APT. Удаление пакета может повлиять на программы всех сессий этого
окружения. Новая вкладка учитывает общий лимит восемь терминалов и закрывается
обычным способом. Закрытие окна менеджера отменяет только его собственное чтение.

Русский/английский интерфейс сохраняется; имена пакетов и версии не переводятся.
Редактор файлов описан ниже.

## Выбор версии пакета

Нажмите пакет → Выбрать версию → нужная версия → подтвердите место запуска.
Список читается через apt-cache madison выбранного окружения; это доступные
версии подключённых репозиториев для текущей архитектуры. Не архив всех когда-либо
выпущенных версий. Если список пуст, обновите каталог. Репозитории автоматически
не добавляются; установленная версия, исчезнувшая из репозитория, не предлагается
для повторной установки как доступная.

Для версии ниже установленной показывается предупреждение. Команда имеет вид
`apt-get install -- 'имя=версия'`, без -y и обхода подтверждений APT. Зависимости
и возможность понижения определяет APT; если выбранная версия перестала быть
доступной после чтения списка, APT сообщит ошибку. Не применяется автоматический
hold/pin: обычное последующее обновление может снова установить новую версию.

Ровные кнопки менеджера сохраняют одну строку, автоматически подстраивая шрифт.
В v1.0.18 выбор языка перенесён в общую шестерёнку; отдельного глобуса нет.

## Описания пакетов и действия библиотек

Нажатие пакета показывает описание выбранного окружения. В карточке максимум три
строки, Подробнее открывает полный текст с внутренней прокруткой. Для установленного
пакета используется dpkg metadata; для доступного/обновления — APT metadata конкретной
показанной версии. Информация может отсутствовать: действия установки/версии/удаления
остаются доступны. Описание загружается только при открытии, не для всего каталога.
Это текст автора, не автоматически созданная справка: язык и примеры зависят от
репозитория; примеры запуска отдельно не добавляются и ничего автоматически не выполняют.

Команды/Заметки: нажмите запись, чтобы прочитать текст и выбрать В избранное /
Убрать из избранного, Редактировать или Удалить. Удаление требует подтверждения;
Вставить в терминал у команды сохранено и не отправляет её. Троеточий у этих записей
больше нет. Журнал главной, сохранённые сессии и звёздочка терминала не менялись.

В создании/редактировании команда предлагает категории существующих команд,
заметка — категории существующих заметок. Поле редактируемое: нажмите для списка
или вводите для поиска подсказок, новую категорию можно вписать полностью.
Пустое значение по-прежнему сохраняется как Общее/General согласно языку приложения.

## Выбор категории

В редакторе команды/заметки нажмите кнопку ▾ справа от категории. Откроется
полный список соответствующей библиотеки, даже если в поле Общее или другой
текст. Выбранная категория заполняет поле. Ввод новой категории по-прежнему
доступен; не требуется выбирать только из существующих. При отсутствии категорий
окно поясняет, что новую можно ввести в поле. Отмена не меняет введённый текст.

## Редактор файлов

Файлы → распознанный текстовый файл → Просмотреть / Редактировать. Поддержаны .md,
.txt, .json, .sh, .py, .java, .kt, .rs и другие типы существующего текстового фильтра.
Редактор читает UTF-8 до 256 КБ; Enter переносит строку, текст моноширинный.
Нажмите Сохранить, чтобы записать файл в выбранном окружении Termux/Ubuntu/SSH.
Отмена/назад при изменённом тексте предлагает продолжить редактирование либо закрыть
без сохранения. Ошибка чтения не открывает пустой документ. Ошибка записи оставляет
окно и ваш текст для копирования/повторной попытки.

Сохранение требует доступа к файлу и записи в родительскую папку: используются
временные файлы в той же папке, копирование прав/владельца и атомарная замена.
Ссылки и файлы с несколькими hardlinks не редактируются, чтобы не изменить их
семантику. Нужны Bash и GNU-совместимые sha256sum/stat/mktemp/cp/mv; SSH-сервер
может не предоставлять эти инструменты или нужные права. Файловые метаданные
ограничены возможностями среды, особенно общей памяти Android.

При изменении файла внешним процессом после открытия запись блокируется по
снимку содержимого и metadata. Это оптимистическая проверка перед заменой,
не эксклюзивная блокировка всех программ: одновременно редактировать тот же
файл в Codex/другом редакторе не рекомендуется. Между последней проверкой и
rename существует короткое окно гонки с несотрудничающим внешним процессом.
Текст не автосохраняется в app state/cache; уничтожение приложения теряет
несохранённый буфер. Смена схемы библиотеки и работающих терминалов не требуется.

## AI Agents — локальная v1.0.6

[Новые подключения, история, установка OpenCode и откат](AI-AGENTS.md). Публичный
релиз v1.0.4 этих пунктов ещё не содержит.

## Локальная v1.0.10: быстрые действия терминала

Кнопка с коробкой рядом с выбранной сессией открывает пакеты её окружения. Выбор
целевой сессии/новой вкладки и подтверждения установки прежние. При долгом тапе на
названии сессии пакетного пункта больше нет. Нажмите название текущей сессии:
**Создать новую сессию** — первый пункт, ниже открытые вкладки; окно называется
**Создать сессию**. + на главной у пресетов открывает **Создать пресет**. Эти два
пути сохраняют прежнюю логику: создание обычной вкладки не делает её сохранённой.
Названия и предупреждения есть на русском/английском.


## Локальная v1.0.18: диагностика сессии

На вкладке терминала долгий тап на название сессии → «Диагностика сессии».
Коллектор работает без открытого окна, окно обновляется каждые 2 секунды.
«Остановить журнал» прекращает сбор; «Продолжить журнал» собирает новые события,
но не восстанавливает пропущенный интервал. Терминал эти действия не останавливают.
Базовый сбор активен сразу для новых/подхваченных профилей. События до подключения
коллектора не восстанавливаются, доступен только ограниченный текущий буфер вывода.

При создании сессии или пресета включите «Расширенный дебаг-журнал» до запуска:
появится временная шкала активности; Gemini также получит штатный --debug.
Для других агентов внутренний сетевой trace не подключён: остаются события/коды.
Изменение переключателя на работающей сессии применяется при следующем запуске.
Остановка коллектора не меняет уже запущенный native verbose-режим Gemini.

Путь к отчёту создаёт свежий приватный .txt в данных приложения и копирует его
путь. Экспорт в Download сохраняет отдельный файл с уникальным именем; старые
выгрузки не перезаписываются. При ошибке доступа показана ошибка сохранения.
В отчёте нет полного диалога/вывода и содержимого секретов: только структурированные
события и allowlist кодов. Это не полный HTTP trace. Упоминание ошибки в тексте
может быть цитатой, поэтому запись TERMINAL_SIGNAL не доказывает сбой запроса.

Лимит2МиБ на профиль и32МиБ суммарно; старые записи заменяются новыми. В окне показывается до64КиБ,
в файл сохраняется весь накопленный журнал. В памяти хранятся до64 последних
известных профилей; при смерти процесса приложения журнал в памяти теряется,
сохранённые отчёты остаются. Закрытие только окна диагностики сбор не прекращает.

Верхний ряд терминала: интерактивный ввод → дополнительные клавиши → общая шестерёнка. Два переключателя имеют векторные значки и бирюзовую заливку при
включении, обычный тёмный фон при выключении. На главной и в командах их нет.
Единая шестерёнка на всех вкладках открывает язык и настройки приложения.
Отдельный глобус убран.
Отмена следующих шагов находится справа внутри плашки выбранной сессии и
появляется только для пользовательских команд Termux/Ubuntu, не для AI/SSH.
Перед отменой показаны все настроенные команды и выбор Да/Нет; текущий процесс
не останавливается. Кнопка — центрированный вектор.
В списке выбора сессий кнопки нет, при открытом списке она также скрыта.
Отдельная команда клавиатуры удалена: используйте поле ввода или терминал
с включённым интерактивным вводом.

## Проверка обновлений (локальная v1.0.19)

Общая шестерёнка → «Проверить обновления». Приложение показывает изменения
стабильного релиза, скачивает и проверяет APK, затем предлагает открыть системный
установщик. Первый переход на v1.0.19 выполняется вручную. Подробности и сохранение
данных: [UPDATES](UPDATES.md). Отсутствие update.json в релизе сообщается явно.

## v1.1.0 — бэкапы и автообновления

Шестерёнка справа сверху → «Резервное копирование · бета». Подробный порядок
создания, проверки и выбора компонентов: [BACKUP-RESTORE](BACKUP-RESTORE.md).
Автопроверка GitHub один раз при каждом открытии/возврате, отключается галочкой «Авто» в плашке проверки обновлений;
ручной пункт остаётся. Доступная версия отмечается точкой. APK только по нажатию.
Обновление не требует очистки библиотек/истории.

## Выборочный бэкап v1.1.1 (локальная проверка)

Шестерёнка → Резервное копирование · бета → Создать → По выбору.
Раскройте Termux/Ubuntu, выберите Codex, проекты, SSH/Git или другие компоненты,
проверьте состав и зависимости. Для самостоятельной Ubuntu отметьте систему;
данные без системы извлекаются отдельно и активируются вручную.
[Состав, ограничения и совместимость manifest v2](BACKUP-RESTORE.md#выбранные-данные-v111).

В локальной v1.1.2 кнопка создания активна только после согласия в обоих режимах.
Папка назначения подписана явно. В «По выбору» недоступные пути видны в карточке,
их компоненты отключены; можно выбрать остальные. Если выбранные файлы изменятся
или станут недоступны при копировании, копия не будет помечена готовой.

### В локальной v1.1.3

В резервном копировании можно выбрать gzip/pigz и уровень сжатия;
gzip3 выбран по умолчанию. Для отсутствующего pigz предложена отдельная установка
в Termux с согласия пользователя. Дождитесь завершения при открытом приложении.
Окна бэкапа не закрываются случайным тапом за рамками. Время и полоса показывают
ход текущего этапа; неизвестный объём отображается анимацией. После успеха показан
путь проверенного бэкапа. Подробности и ограничения — [BACKUP-RESTORE](BACKUP-RESTORE.md).

### Обновления v1.2.0

В шестерёнке одна плашка «Проверить обновления»: нажатие названия запускает
ручную проверку, квадратная галочка «Авто» справа переключает автоматическую.
Пояснение внутри плашки: «Проверять при открытии приложения». При ошибке
следующая попытка при следующем открытии/возврате; постоянных повторов нет.
При отключённой галочке ручная проверка остаётся доступна.


## Диагностика v1.2.2

[Диагностика и настройка опроса серверов](DIAGNOSTICS.md).

### Возврат из просмотра в звёздочке (v1.3.2)

«Назад» в просмотре команды или заметки возвращает к списку с прежним поиском
и выбранной вкладкой. Системный Back также возвращает к списку. Для выхода из
самого списка остаётся «Закрыть». Просмотр не отправляет команду в терминал.

### Локальная v1.3.4

Поиск по-прежнему работает при вводе каждой буквы и учитывает текущие фильтры обеих библиотек. Новых настроек нет.

Local AI preview: see [model downloads, imports, history and current limitations](LOCAL-AI.md).

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


## Experimental Qwen protocol (v1.4.29-test)

Local AI → Settings → Engine settings → Model communication format: Current JSON (default) or Native Qwen format (testing). Only catalog Qwen3.5 2B / llama.cpp is verified for the new adapter. It preserves existing permissions and reloads the same chat. For comparison create a new chat; tool schemas take context space. This is a text/function-call adapter, not a general Jinja evaluator or a model modification. See [release notes](RELEASE-v1.4.29-test.md) for limits.


## v1.4.30-test operating mode

Engine settings → Operating mode → CPU / GPU (OpenCL, testing). CPU is default; its ARM/compatible settings and threads reappear unchanged after returning from GPU. Initially only catalog Qwen3.5 2B on Snapdragon8EliteSM8750 is supported. GPU inference runs in an Android child process; Ubuntu tool environment remains selected independently. Select CPU if GPU initialization fails. Compare actual request diagnostics; CPU prefill benchmark is unavailable while GPU selected. Error cards now contain original calls and precise schema feedback. Empty write truncates, never deletes. See [release notes](RELEASE-v1.4.30-test.md).


## v1.5.0 server selection and histories

Long-press a server file/folder to select; the lower-right ↓ panel offers Download, Move, Select all, Clear. Download opens the local destination picker (including access request for device storage). Files preserve names/hierarchy in a new download-UUID folder, max10GiB/file. Quick Download remains. Move prepares a same-server selection: navigate to destination, use Move here, confirm. No overwriting or cross-filesystem copy/delete fallback. Completed transfers/moves survive partial batch failure; stop affects only the transfer connection. No persistent retry queue after app exit. Unsaved-session closing has Keep history and Close / Add preset and close. CLI-native chat history is independent; supported Codex chat viewing/deletion is in General settings → Chat histories. Local models are disabled in the public build while testing continues; existing experimental data remains.

## Проверочная сборка после v1.6.5

[Окружения Termux](ENVIRONMENTS.md): вложенный выбор Linux, команда входа,
подтверждённая установка, проверка наличия оболочки и особенности отката.
У AI Agents в долгом тапе по сессии на вкладке терминала — «Установка пакетов»;
терминальное окно по-прежнему открывается роботом над чатом.

## Частная X11 v1.7.35: остановка

Возврат в терминал скрывает рабочий стол, сохраняя его работу. «Остановить графический
рабочий стол» завершает X11 и его графические процессы, оставляя терминальную вкладку
открытой. Закрытие сессии-владельца также останавливает её стол. После самостоятельного
завершения стола освобождение X11 выполняется автоматически; перед новым запуском
дождитесь завершения остановки. Сейчас доступен один стол одновременно, у каждой
системы собственные установленные программы и настройки.

## Частная X11 v1.7.36

При запуске под эмблемой X11 показаны «Запуск рабочего стола», полоса этапов и текущий
шаг. Продвижение означает готовность компонентов, а не процент прошедшего времени.
Если запуск затянулся, остаётся возможность показать экран вручную.

Для Alpine применяется локальная совместимость с PRoot: загрузчик изображений Glycin
работает без внутренней песочницы Bubblewrap. Это снижает изоляцию декодирования
недоверенных изображений в окружении Alpine; права Android и песочница Firefox не
отключаются. Переустановка окружения или пакетов для этого изменения не требуется.

## Частная X11 v1.7.37

После предупреждения о долгом запуске можно показать экран вручную. Если стол ещё
готовится, приложение некоторое время продолжит редкую проверку и покажет его
автоматически после готовности. Для Alpine исправлена ошибка «invalid APK file
ownership» при открытии программ; после обновления повторите проверку программ
или обновление каталога. Переустанавливать окружение и пакеты не требуется.


## Частная X11 v1.7.41: установка в карточке

При установке программа показывает кнопку «Стоп». Под описанием — полоса прогресса,
оценка оставшегося времени текущего этапа, измеренный объём загрузки/остаток (когда
менеджер пакетов их сообщает) и одна строка текущей операции с прокруткой длинного текста.
Если точные данные ещё не получены, отображается неопределённый прогресс и расчёт времени.
Можно закрыть окно программ или перейти в другую вкладку: установка продолжается в
службе. При возвращении показывается её текущее состояние. Это не гарантирует продолжения
после принудительной остановки Android или остановки самого X11-сервиса.

После установки справа от «Установлено» появляется корзина. Подтверждение называет
конкретный удаляемый пакет. Общие зависимости, документы и остальные приложения не
удаляются автоматически. В рекомендуемых наборах удаляется основная программа:
например, Ristretto, но не Tumbler; LibreOffice Writer, но не Calc/Impress. Остальные
программы доступны отдельно в установленных. Пакетный менеджер может отказать в удалении
при зависимостях. После «Стоп» может понадобиться восстановление незавершённой установки.

APT повторяет временно неудачные сетевые запросы до трёх раз; при ошибке подписи,
зависимостей или нехватке места установка не запускается заново автоматически.
В Alpine APK повторы пока штатные, отдельная обвязка повторов не добавлена.
Для исправления миниатюр Alpine после обновления перезапустите её рабочий стол.


В меню X11 остаются «Переключиться на рабочий стол» и «Программы рабочего стола».
Отдельные «Проверка»/«✓ Установлено» убраны. Переключение проверяет наличие компонентов,
запускает готовый стол либо предлагает его установку с подтверждением. После установки
запуск продолжается автоматически. В графическом виде вместо переключения — возврат
в терминал. Пока стол запущен, в обоих видах доступна его остановка.

«Рекомендуемые» и «Установленные» открываются и во время обновления каталога/проверки.
Если уже есть сохранённый список, он доступен до окончания обновления; без сохранённого
списка потребуется дождаться проверки. Выход из раздела не останавливает операцию и
повторный вход не создаёт её копию. Изменяющие пакеты действия во время другой операции
по-прежнему недоступны.


## Частная X11 v1.7.42: отдельная кнопка программ

Справа от «Терминал / Рабочий стол» находится «Программы». Она всегда открывает обычный
список: каталог, рекомендуемые и установленные. В меню вида больше нет пункта программ;
возврат из списка закрывает его. Переключение и остановка стола остаются в меню вида.

Когда окно программ закрыто, а установка продолжается, по контуру кнопки идёт линия.
Она исчезает после завершения/ошибки/отмены. При скрытой вкладке или сворачивании Activity
анимация не работает; сама установка продолжает выполняться в службе. При возвращении
вкладки состояние перечитывается. Кнопка показывает только выбранное окружение.

При сетевой ошибке остаётся краткое пояснение; полный доступный журнал открывается
кнопкой «Подробнее». Если зеркало недоступно даже после повторов, потребуется проверить
связь или выбрать другое зеркало. Эта версия сама зеркала не меняет.

Для проверки исправления миниатюр Alpine остановите и снова запустите её рабочий стол.
Настройка совместимости теперь действует и на кодировщик PNG в приватном дереве стола.


## Частная X11 v1.7.43: поиск отдельным разделом

«Программы» → «Поиск и установка программ» открывает поиск с прокручиваемым каталогом
и прежними карточками установки. На главной остаются рекомендуемые, установленные и
переход к поиску. Возврат из подраздела ведёт на главную, выход не отменяет текущую задачу.

Перед поиском одна кнопка «Подготовить и обновить каталог». После подтверждения она
добавляет недостающие компоненты и обновляет метаданные. Уже готовые компоненты не
переустанавливаются. В Ubuntu/Debian используются AppStream и Python/APT, в Termux/Alpine
— родные индексы пакетов. Каталог может читаться из сохранённого списка; эта кнопка
выполняет явное обновление, без изменения выбранных репозиториев.

Если Evince/Document Viewer в Termux уже установлен, но не открывается: после обновления
проверьте «Рекомендуемые». При отсутствии необходимой libandroid-complex-math появится
«Установить» у PDF; нажатие дополнит комплект, не удаляя Evince и документы. Это исправляет
выявленную ошибку загрузчика, а фактическое открытие PDF нужно проверить на устройстве.

В меню «Терминал / Рабочий стол» пункт «Автоматическая клавиатура» управляет
появлением клавиатуры при выборе поддерживаемого поля X11. Галочка справа включает
автовызов; без галочки остаётся ручной вызов через «Интерактивный ввод» | вверху.
Настройка включена по умолчанию и сохраняется отдельно для каждого окружения.

При переносе Ubuntu в Ubuntu AI служебные Android-точки подключения (/apex,
/data/app, /data/dalvik-cache и другие Android-подключения из фиксированного списка)
исключаются заранее. В копии сохраняются пустые каталоги для подключения при запуске.
Проекты, настройки, ключи и пакеты Ubuntu копируются; исходная система сохраняется.

Служебные подключения сессий и агентов в /tmp не переносятся как данные Ubuntu.
Приложение подключает их заново при запуске. Пресеты и история в приложении сохраняются;
обычные временные файлы Ubuntu продолжают переноситься. Исходная Ubuntu не очищается.

Подготовка миграции автоматически удаляет только пустой каталог /workspace с правами
000, если он мешает сканированию. Файлы и ссылки не удаляются; обычные права сохраняются.
rmdir откажется удалять непустой каталог. После подготовки окно укажет, было ли это
сделано. Подготовка может остаться выполненной, даже если затем отказаться от копирования.

## Private 1.7.51: Ubuntu AI defaults and test environment removal

New agent sessions/presets select Ubuntu AI by default; saved choices are preserved.
The migration button is disabled when Ubuntu AI is selected. Its environment-list row
says “Used for AI agents”. Ubuntu X11 Test is no longer a built-in catalog entry;
installed legacy aliases remain discoverable for compatibility. Removing its root does
not remove the APK X11 renderer or the ordinary Ubuntu desktop configuration/code.
Desktop packages must be installed separately in ordinary Ubuntu.

## Private 1.7.52: independent agent Ubuntu installation

Selecting Ubuntu AI in an agent session/preset includes installation of that selected
root when absent, after the existing preparation confirmation. Ordinary Ubuntu's
presence does not block it. Migration is a separate optional action, never an automatic
prerequisite. Existing profiles retain their chosen alias, including ordinary Ubuntu.

Host preparation checks container existence independently of login success. It installs
only missing roots through environment-runtime.sh, retaining v5 --name and legacy
--override-alias support. It verifies login after installation; failures abort without
fallback, reset or reinstall. Inventory on an existing but inaccessible/busy root now
returns a check error instead of a missing-Ubuntu plan. Retry after installation finishes.
The standalone host helper defaults to Ubuntu AI; launchers still explicitly pass the
selected alias. No existing environments, live sessions or credentials are changed by
this source/build work. No migration/reset is required to update from1.7.51.

## Inline environment installation (private1.7.52)

The environments chooser stays open when installation is confirmed. The chosen card
shows Stop, a phase progress bar, measured phase ETA when available, and the latest
output line. Missing percentage/throughput is displayed as calculating, not an invented
overall completion time. Successful readiness switches to disabled Installed. Failure
or cancellation does not claim readiness. Tap an installed row to select it explicitly.
Closing the chooser or switching app tabs does not cancel the worker; reopening binds
the same active state. Only the current alias card is updated; the list and terminal are
not rebuilt for progress events. One setup operation runs at a time.

Stop writes a unique private control marker. A native Python supervisor owns a new
installer session/process group, polls that marker, sends TERM only to its own live
child group, then KILL after3 seconds if necessary. No user session, proot container
or unrelated PID is selected for termination. A private PTY exposes real installer
progress without borrowing the user's terminal or stdin. If native Python is absent,
explicitly approved preparation installs it first; a Stop request during that prerequisite
is honored after the package command finishes, before the environment installer starts.
Stopped installations can leave incomplete roots; no root is automatically removed.
No foreground-service persistence across Android killing the app is claimed.

The agent environment selector only records Ubuntu/Ubuntu AI; it no longer starts
migration or installation implicitly on selection. Missing-root preparation happens
at explicit launch, and migration remains on its own button.


## Частная v1.7.53: GPU-рендеринг

В меню «Терминал / Рабочий стол» добавлен выбор CPU / Turnip + Zink.
GPU пока доступен только для Termux и обычной Ubuntu 26.04 и меняется при
остановленном столе. Компоненты загружаются отдельно с SHA-256 в частный кэш;
переменные драйвера действуют только в графическом процессе.
[Установка, ограничения и тест glmark2](X11-GPU.md),
[проверки сборки](VALIDATION-v1.7.53-x11-test.md).


## Диагностика X11 в v1.7.54

Долгий тап по обычной сессии → Диагностика → X11. Обычный/Подробный меняют
только журнал сессии, поэтому при открытом X11 отключены. Копирование/сохранение
выгружает полный отчёт вместе с X11. Для Fedora/openSUSE/Void/Arch и других ещё
неподключённых сред указан статус поддержки. Ubuntu AI и агенты не включены.
После воспроизведения чёрного экрана сохранить отчёт до остановки стола. Этапы
показывают время от начала запуска; после готовности время запуска не растёт.
Новый сбор требует запуска на этой сборке; старые отсутствующие этапы не восстанавливаются.


## Firefox: PRoot и DNS HTTPS во всех подключённых X11-средах

В карточке Firefox у Termux, Ubuntu, Debian и Alpine доступны обе галочки.
PRoot включает прежний обход ошибки seals только для Firefox из ярлыка меню.
DNS HTTPS отправляет DNS-запросы Firefox Cloudflare по HTTPS с обычным DNS при
недоступности; это не отключение проверки сертификатов. Настройка профильная,
системный DNS не меняется. Перед изменением закрыть Firefox. Для DNS профиль
должен уже существовать после первого запуска. Отключение восстанавливает
записанные значения, посторонние правки и внешние/символьные пути не перезаписываются.
Состояние неизвестно при повреждённых/изменённых служебных записях — галочка недоступна.


## Меню работающей сессии

Нажать шестерёнку справа внутри её верхней плашки или справа в строке раскрытого
списка. Название открывает список/выбирает сессию, шестерёнка открывает действия.
Долгий тап для этих плашек больше не требуется.

### Подготовка рабочего стола (частная v1.7.61)

«Переключиться на рабочий стол» открывает экран выбранной системы:
проверка → при необходимости подтверждение Да/Нет и установка → запуск.
Под эмблемой Termux Job’s + X11 показаны этап, прогресс, измеренный объём
загрузки/оставшееся время и одна строка текущего действия.
Неизвестный прогресс обозначается движущимся индикатором, не выдуманным процентом.
«Отменить установку» находится под прогрессом. Отказ возвращает в терминал;
смена вкладки установку не останавливает. После успешной установки запуск
продолжается автоматически. Установка может требовать интернет и место.

### Рабочий стол существующей Fedora (частная v1.7.63)

Откройте обычную сессию установленной Fedora и выберите «Переключиться
на рабочий стол». При согласии компоненты XFCE/ввода/звука и Mesa
устанавливаются в эту Fedora через DNF; отдельная система не создаётся.
Меню программ, проверка, установка, отмена и диагностика работают как
у остальных подключённых сред. Комплект GPU доступен для Fedora44.


## Codex chat attachments (v2.0.2 development build)

Tap the folder beside the Shell field, then choose Environments or Phone storage.
The latter requests Android storage access when needed. Choose an image, text/log
file or supported video, review the attachment and send with optional message text.
Use × to remove an unsent attachment. Tap a history card to open its preview and
Save to Download. Video analysis uses up to12 timestamped frames, without audio;
short events between frames may be missed. Restart the Codex session with its
saved chat after updating; older running helpers do not support attachments.


## Local media thumbnails (v2.0.3 development build)

Photo/video rows in the local file browser show a thumbnail; videos have a play
badge. This is shared by file browsing and local file pickers. Only visible rows
are decoded. Unsupported or unreadable media keep a placeholder and remain
selectable; thumbnails do not grant storage access or send attachments. SSH and
the separate server listing do not gain automatic previews in this build.
Phone/Termux media are read directly; thumbnails inside proot are prepared from
a temporary private copy (images up to20 MiB and videos up to64 MiB). Larger proot
files remain selectable. Chat media opens in an aspect-ratio-sized dialog with
zoom/playback and the existing save action.

### Local APK output cards (v2.0.3)

Explicit local Markdown links such as `[APK](/absolute/build.apk)` (also
`[APK](</absolute/path with (spaces).apk>)`) in Codex assistant messages become
APK cards. The helper only stats regular files, up to256 MiB and four per message;
missing files, symlink files and remote URLs do not become APK cards. It never
reads/hashes the APK on the polling thread. Metadata is cached by native item.
Android copies the artifact through the selected environment on background workers
and shows filename, size and the private copy's full SHA-256 beside an APK badge.
Private artifact cache is bounded to512 MiB, separate from image previews.
Opening a card shows the same metadata and explicit Install / Save to Download.
No installer or Android settings activity starts just by receiving/opening a card.

Installation shares only that cached file through a read-only FileProvider URI;
it does not expose arbitrary private app files. If Android's install-source
permission is missing, an explanation offers Android settings. Return and press
Install again; Android's package installer owns final consent and package checks.
The checksum identifies the file and is not a publisher/signature trust claim.
This does not add APKs to the outgoing image/log/video picker, remote downloads,
background installation or a new agent execution path. Active helpers need a
Codex session restart with the saved chat after updating.

In v2.0.4 APK cards open on tap without the Open/install hint. Their menu uses
To Download on the left and Install on the right. It has no Close button;
tap outside or use Android Back to dismiss. Other media viewers keep their actions.


## All-agent attachments and sign-in (v2.0.5 development)

The folder picker/send/history flow now also supports Claude, Gemini and OpenCode.
Select an original from an environment or Phone storage, review the draft, then
send it with optional text. The larger red × removes only that draft; its hit area
is unchanged. APKs and other binary originals are supplied by path for agent tools,
not treated as text. Model-specific media and native file permissions still apply;
Gemini may request access to the staged @ file in its native terminal.

For a Codex profile without native auth, tap Sign in to Codex in the chat notice.
The native terminal shows the official device-login URL/code; follow that flow and
return automatically after success. Minimizing the terminal keeps the pending
login attempt. Return to the Codex chat explicitly cancels that owned attempt;
existing logged-in profiles use their saved authorization.

For Gemini API authentication, an optional masked key field appears immediately
below the auth choice. Blank preserves a saved key; if none was set, existing CLI
auth is used or Gemini prompts in its terminal (robot button). New text replaces
the saved key, and Remove saved key clears it when settings are saved. Changes take
effect on next launch. Keys are device/profile-bound: duplicated/imported presets
require re-entry. There is no automatic restart of an active session.

Codex context-compaction notices open as compact text windows: dismiss by outside
tap or Android Back. The normal reasoning/full-output viewer retains pagination.


v2.0.6 fixes OpenCode attachment admission. Restart that session with its saved chat
after updating; reselect the file and use Send, with optional text. No history reset
or new provider login is needed for this helper fix.


## Unreleased agent-chat recovery (2.0.3-test)

After a failed permission reply, the same pending Yes/No prompt can be answered
again. Check the agent state before retrying an uncertain action; no permission
is granted automatically. Gemini permits retry with the same message ID only
when readiness was lost before calling the native submit/Shell callback. An
uncertain callback result remains reserved to avoid duplicate execution.

OpenCode rejects messages, details, forms and interrupts from another native
conversation. Each new launch has its own private control socket; history and
provider connection choices remain independent of that run identifier. Queued
close operations capture their owner before scheduling, including the Codex
stop marker and Gemini/Claude environment. Existing legacy OpenCode runs keep
their original socket until an explicit next launch. No session is restarted
automatically. For packaged helpers to change, save the chat and explicitly
restart the affected agent after updating.

[Audit evidence and phone checks](AUDIT-AGENT-CHATS-2026-10-23.md).


## Unreleased X11 installation/repair follow-up (2.0.4-test)

After Cancel, wait for Cancelling to finish before retrying or installing another
program. Closing the owned pipe initiates group cleanup; new operations remain
blocked until it exits. Ordinary Ubuntu now also offers Programs → Installed
programs → Repair launcher permissions. Stop its desktop and confirm the action;
only checksum-verified launchers are eligible, with original permission backups.
No repair is performed automatically. See the
[environment/X11 audit](AUDIT-ENVIRONMENTS-X11-2026-10-23.md) for evidence and limits.


## Unreleased backup and cleanup cancellation (2.0.5-test)

For cancellable backup, restore and storage scan/file cleanup operations, Cancel
shows Cancelling / Stopping operation and keeps the progress window open until
the worker finishes. Wait for that result before starting another storage job.
Native environment deletion remains non-cancellable after its confirmation.
Cancellation is not rollback: files already deleted stay deleted, and partial
backup/restore destinations must not be treated as complete. A late Cancel does
not hide a successfully completed operation. No user-data reset, archive migration
or environment reinstall is required. See the
[storage audit](AUDIT-STORAGE-2026-10-23.md) for performed checks and phone limits.


## Unreleased server transfer follow-up (2.0.6-test)

After dismissing a confirmation, choosing another server/destination or
disconnecting, make a fresh selection before starting a transfer. An old
confirmation cannot start a new upload, download or move. Reconnecting does not
reactivate it. Existing in-progress background controls remain available.
Downloaded filenames are not merged into a directory which appeared during
publication; the operation fails and preserves existing destination contents.
Already completed files remain retained on cancellation/error. Transfers are not
automatically replayed after a network failure: check the destination if a
publication result is uncertain. No account, archive or environment migration is
required. See [audit evidence](AUDIT-TRANSFERS-2026-10-23.md).


## Unreleased workspace saving follow-up (2.0.7-test)

Commands, notes, presets and recent-command history retain the same controls.
Pending saves now stay ordered when a screen is recreated; an old closed screen
cannot queue another save or receive a completion callback. Queued disk writes
still finish after close. Write errors are shown to the current open screen when
one exists. Do not treat an in-memory change as proof of a completed disk commit.
Valid libraries need no reset or migration. If a library contains duplicate IDs
inside commands, sessions or notes, loading becomes read-only and preserves the
original file; no automatic deletion, ID rewrite or reset is performed.
[Evidence and phone checks](AUDIT-PERSISTENCE-2026-10-23.md).

## Unreleased receive/update follow-up (2.0.8-test)

Shared incoming files require a simple basename. Existing names and links are
never overwritten: choose a new name. Failed new copies are removed. Update
Cancel requests disconnection and waits for worker exit before another check can
start. Install repeats size/hash and current package/version/signer validation
in the background after any Android permission settings visit. Cleared/changed
cache requires a fresh download. No workspace/account reset is needed.

## Unreleased installer follow-up (2.0.9-test)

Environment checks resolve shell links within the selected rootfs. A directory
with a missing/broken/nonexecutable shell is not considered ready. Existing
working systems and persistent native download caches are not replaced. Each
check/install stages its own scripts; errors and interrupted waits retain the
operation slot until its process exits. Cancellation stops only its owned
installer group. Initial Python package preparation may finish before Stop can
be observed; this does not promise an immediate prerequisite cancellation.
See AUDIT-INSTALLERS-2026-10-23.md for device checks and recovery limitations.

## Unreleased file manager follow-up (2.0.10-test)

After canceling/replacing a Copy/Move confirmation or changing its selection,
open a new confirmation; old buttons cannot replay it. Download preserves
existing names (including links) and creates a new numbered file exclusively.
Unfinished Java exports check browser closure/interruption between chunks;
completed files can remain if closure happens after completion. Successful Share
keeps its private cache copy for a recipient's later read. Running shell batches
still retain completed actions on error and do not provide whole-batch rollback.
See AUDIT-FILE-MANAGER-2026-10-24.md for device/transport limits.


## Unreleased file transport follow-up (2.0.11-test)

A file operation reports timeout separately from an unavailable source. Local
upload input is bounded while it is read, including a source that grows after
selection. An unfinished stream owns its child until exit/cleanup; closing an
older stream cannot stop the next one. A new overlapping operation reports busy.
Closing a file window requests cancellation without waiting on the UI thread;
input-stream close itself waits for its directly owned child.

After a network failure during Copy/Move, inspect both source and destination
before retrying. Completed items remain and remote/detached descendants are not
proven stopped just because the local SSH transport exited. No automatic replay
of a partially completed transfer is introduced. See
AUDIT-FILE-TRANSPORT-2026-10-24.md for the scoped tests and remaining phone checks.


## Unreleased transfer recovery messages (2.0.12-test)

Copy/Move success now requires the current batch's final completion receipt as
well as a successful transport exit. After an unconfirmed result, the displayed
number is received acknowledgements rather than an exact count of files changed.
More items may already have completed, and the command may still be running in
the selected environment/server. Check both source and destination and finish or
stop that work before manually repeating the batch. There is no automatic retry.
Existing files still are not intentionally replaced; completed items are not
rolled back. SSH keepalives detect an unresponsive file-operation connection;
they do not automatically reconnect it or stop remote descendants.

The confirmation, selection, Copy/Move controls and window sizes are unchanged.
No accounts, environment migration, rootfs replacement or data reset is required.
See AUDIT-FILE-RECOVERY-2026-10-24.md for isolated test evidence and phone checks.


## Unreleased cumulative audit (2.0.13-test)

The application/terminal remains Android8+/API26, ARM64; the embedded graphical
X11 desktop requires Android11+/API30. This clarifies the existing support split,
not a new minimum-version increase. Button sizes, layouts, keyboard controls,
renderers, account data and environment selection do not change in this pass.
After an explicit upgrade, check an agent chat's input focus and switch an X11
session between terminal/desktop, including showing/hiding the keyboard. Those
physical checks remain separate from the automated fixture results in
AUDIT-REGRESSION-2026-10-24.md. No data reset or environment replacement is needed.


## Unreleased settings reopen fix (2.0.14-test)

Reopening settings creates adapters for the current screen while keeping the
same saved preferences. Keyboard/margin switches and log levels do not need to
be reconfigured. A new logging-settings screen uses its current language for
feedback, instead of the first screen's cached Context. A failed package lookup
is not retained as a global settings adapter; existing lookup/error behavior
still applies. No addon is installed automatically by this change.

For a phone check, open keyboard settings, change one switch, close and reopen
settings, then verify that value. Reopen logging settings after changing app
language and check the log-level feedback. Real addon permission/error dialogs
and physical memory/GC behavior remain device checks. See
AUDIT-SETTINGS-2026-10-24.md; no data reset or environment replacement is required.

## Unreleased Android file-sharing hardening (2.0.15-test)

Android's Termux document source exposes HOME. Normal saved document IDs remain
usable, including internal links and their display names. Links resolving outside
HOME are no longer listed/opened through this source: choose the destination
through Android's own storage source instead. This does not remove files or
change the application's project/environment file browser or shell access.
The source supports creating a new file/folder and refuses root deletion or
path-like names; an existing name receives a numeric suffix rather than overwrite.

termux-open keeps its existing allow-external-apps requirement and read-only
outgoing URI grants. The legacy file provider serves existing regular files from
Termux files/shared storage; it no longer creates missing paths through openFile.
Property files cannot be made writable through a hard-link alias. Unicode/#/?/%
share names are encoded correctly, uppercase extensions are language independent
and sizes above2 GiB remain 64-bit. No reset/account/rootfs migration is required.
Android storage permissions and actual external-editor behavior still need phone
verification. See AUDIT-PROVIDERS-2026-10-24.md for the tested scope and limits.

## Unreleased attachment and notification hardening (2.0.16-test)

Attachment Download preserves existing files and links, and uses a numeric suffix
before the extension if a name is occupied, consistently with file-manager export.
Closing its owner cancels an incomplete local copy; completed copies remain.
Photo miniatures use the updated AndroidX EXIF reader. Zoom/drag and viewer window
sizes are retained; simple image taps now issue the standard click/accessibility
event. Check actual pinch/drag behavior on the phone after a chosen update.

Terminal/report notification actions keep their existing targets and labels with
immutable PendingIntents. Launch/share entry points and existing permissions remain;
no reset, environment replacement or new sign-in is required. Native driver/UI and
physical notification behavior remain device checks. See AUDIT-LINT-2026-10-24.md.


## Environment aliases (v2.0.9)

See [Environment aliases](ALIASES.md) for the separate library, category and star
pickers, environment-scoped shell files, write safeguards and restart semantics.
This feature excludes Ubuntu AI and does not alter running sessions.

Agent status panels also provide [Chat scale](AGENT-CHAT-SCALE.md),50–150% per
session, with the original100% default and a reset action.
