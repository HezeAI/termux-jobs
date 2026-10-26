# v2.0.8 — Developer chat style

Assistant replies use mint accents on dark native cards. Fenced code has a
language label, syntax colours, separate line numbers, horizontal scrolling and
an outlined Copy action with pressed feedback for the exact code. Long blocks expand in place; tables scroll
horizontally. Read-only task markers remain display-only. Streaming reuses stable
blocks and preserves the composer rather than rebuilding the chat.

The shared file browser adds a vector sort button beside Home, with folders first
and remembered name/size/date ordering. Long press enters chat multi-selection;
explicit confirmation stages up to four attachments, including existing drafts.
The source menu adds the system device gallery for photos/videos. Selection does
not send a message. Arbitrary regular files retain compact type/name/size cards.

A red filled hand between the agent limits and Server + Agent interrupts current
work; idle shows a disabled grey crossed hand. While busy, the existing activity
line travels around the hand through its right half first, then the left, and
continues along the line using one animation. Android VSync drives a 60 FPS target,
without recreating the chat; physical frame pacing remains a device check.
Successful acknowledged message submission hides the keyboard unless the user
has switched sessions or edited a new draft. Shell commands retain their existing
input behaviour. Device gallery, IME and animation acceptance remain phone tests.

---

# Current component design — v2.0.7

Wrapper driver libraries show a deferred built-in preset even before installation, and a confirmed trash action for imported rows. The preset is protected. A missing selection resolves to the default on desktop preparation; it never overrides an existing explicit import. The preparation area shows optional-theme download progress through the existing cancellation flow. Valid cache hits skip network access.

Driver removal handles auxiliary files and nested caches without following symlinks outside the selected directory. Live XFCE scale restoration treats unset scalar properties as unset values. Portrait host dimensions use the measured surface without inventing hidden-bottom navigation space. There is no new compositor or rotation mode. See [usage](USAGE.md) and [architecture](ARCHITECTURE.md).

---

# Historical desktop design — v2.0.6

The desktop menu contains the existing X11 switch, renderer/import, screen, theme and keyboard controls. Wayland rows and the separate Arch program chooser are removed. Header branding remains v2.0.

Choose Bionic for glibc and a saved/imported trusted driver before launching. If missing, the bridge downloads through the existing renderer-preparation progress/cancel flow and uses that selection. Valid installed components are reused. Renderer replacement remains locked while that desktop is active. Error messages show a bounded actionable download/integrity error instead of raw progress logs or private paths.

Screen Apply uses the same owned Xfconf service across the PRoot boundary, keeping rollback on failure. Native Termux’s working settings path is unchanged. Earlier sections describe their named historical releases.

---

# v2.0.5 — desktop screen controls

Per-environment X11 screen form groups resolution, scale and margins under one
Apply. Auto setup dims manual fields and returns canonical defaults; unchecking
prefills the requested 1600×720/75%/5dp form, without saving until Apply. Saved
manual values are not overwritten by reopening. Extra keys start at 400×104dp.
Wayland/KDE and all of its Bionic/Linux driver controls remain visible but gray
and disabled, with a testing explanation. X11 rendering and imports remain
available. The app header keeps its existing v2.0 branding.

---

# v2.0.4 — per-environment bridge and desktop controls

The Bionic-for-glibc choice appears only for six supported ordinary glibc targets. A folder imports trusted ZIPs; the row below shows the selected saved driver. Running/busy desktops disable replacement. These controls reuse the existing renderer/installation flows and do not redirect sessions to a different rootfs. Termux keeps its existing Wrapper; agents and Alpine/musl are excluded.

X11 extra keys have one per-host panel, also reparented into fullscreen. Move/resize tabs stay within the visible host; resize reflows existing keys and preserves current modifiers, while hide/detach releases held keys. Existing Android keyboard state remains separate. The optional theme uses approximately8% smaller rounded metrics and scoped Whisker bottom padding; actual popup acceptance remains a device check.

---

# v2.0.3 — landscape desktop presentation

Rotation is the entry action: a visible X11 desktop fills the screen in landscape,
without the application header, tabs or input row. The existing host is moved,
not duplicated. The only overlay is the compact top-right existing Interactive input `|` icon,
offset from a display cutout and slightly below the top edge. Its fill shows the
existing interactive-input enabled state. No new
permanent toolbar uses desktop height. Portrait or Android Back restores the same
host in its original app position. Back does not stop X11 or select Home/Terminal.
With an open IME, Android may first consume Back to hide the keyboard.

After Back in landscape, embedded mode remains until the next portrait turn.
Landscape logical resolution follows the physical screen up to 2880 pixels wide,
retaining proportions; it does not install a replacement theme.
Outside X11, existing orientation and agent/chat behavior are retained. The app
brand retains italic v2.0 with the existing dimensions.

The optional Kali-inspired Termux-job's appearance is independently available in
all eight current X11 environments. A checkbox below Switch to desktop applies on
next launch and is disabled during desktop/installer work. Distribution menu
plugins/entries are retained, including Whisker Menu and custom panel IDs. Ubuntu
AI is excluded; per-target settings never redirect to another rootfs.

# v2.0.2 — Agent file workflow

A folder beside Shell opens the environment/phone-storage picker. Selected files
remain a per-chat draft with optional text; remove uses a slightly larger red ×
without enlarging its touch target. Submitted history/output cards support compact
image/video/text viewing, saving and an explicit APK installer action. The local
file browser decodes visible photo/video thumbnails with bounded caches.

Codex sign-in is a clickable chat notice only when native authorization is absent.
Gemini's masked optional key field appears directly under the API-auth choice and
explains saved/blank behavior. Keys are never shown in chat. Context-compaction
notices use a content-sized outside-dismissed dialog without a Back button; long
reasoning/tool output retains its paginated viewer. The header places italic v2.0
immediately beside the equally styled app name without changing control sizes.

See [usage](USAGE.md), [chat details](CODEX-ANDROID-CHAT.md) and
[validation](VALIDATION-v2.0.2.md). Earlier design sections are historical.

# v2.0 — X11 and session UI

The existing header dimensions and controls remain; the brand is bold italic
with a compact separate v2.0 label. Session settings move into visible gears
on the selected-session card and running-session rows.

Ordinary local sessions expose Terminal/Desktop plus a separate Programs card.
The selected target owns its surface; terminal and X11 are not rendered at once.
Central check/install/startup stages carry progress and a consent prompt, with
cancellation below. Programs use inline progress in the corresponding row,
not a dismissible operation window. Per-target catalog state survives list
navigation; the animated card invalidates its own element.

Agent chat input stays idle until touched; incoming cards/messages must not
rebuild/focus the composer or briefly render behind Shell. Keyboard/extra-key
state is scoped to the selected session. Optional style applies only to base
Termux XFCE and keeps normal pointer/window handling.

Current scope and device limits: [release](RELEASE-v2.0.1.md),
[validation](VALIDATION-v2.0.1.md). Older sections below are historical.

# v1.7.5: очистка, история и выбор версий

Обзор очистки расположен под настройками приложения. Кнопка «Обновить»
заменяется spinner на время сканирования; под ней одна строка прогресса.
Обзор, подтверждение и процесс не закрываются случайным outside tap/back.
Ничего заранее не выбрано; без явной галочки удаления оно не запускается.
PREFIX view-only; активные окружения защищены. Детали — STORAGE-CLEANUP.md.

В установленных пакетах «История» слева от «Закрыть» без изменения размеров окна.
Выбор версии APK/DNF/Zypper показывает источник и предупреждение до действия.
Чат сохраняет кэш/черновик; «Предыдущие сообщения» показывает загрузку/повтор,
старые строки остаются после polling. Явный выбор нового Codex-чата применяется
при следующем запуске и не заменяется статусом текущего процесса.

---

# Интерфейс v1.7.0

Окружения Termux открывают список конкретных систем. Название слева, справа
Install/Установить либо серое неактивное Installed/Установлено; строка установленной
системы остаётся выбираемой. Размеры основного навигатора/чатов не менялись.
Бэкап без метки testing; в нём отдельный список удаления восстановленных копий.
Неопределённое происхождение — серая строка. Удаление требует явного подтверждения
и закрытой среды; окно операции не закрывается случайным тапом, отмены после
подтверждения нет. Архивы и исходные системы не выбираются этим списком.

Ниже — история дизайна прежних версий.

# Интерфейс v1.6.5

Компактные раскрываемые карточки хронологии общие для четырёх агентов:
внутри действия и исходные подробности; рассуждения/подтверждения остаются
на своих местах. Навык подписывается только при распознаваемом чтении/подключении.
Ошибки команд отличаются от ошибки приложения; неизвестный статус не становится
успехом. Автопрокрутка не переводит фокус с поля ввода на ответ.


У новых сессий агентов общий Android-чат; обычный ответ остаётся текстом,
рассуждения/действия раскрываются из компактных плашек. Подтверждения и вопросы
привязаны к конкретному запросу. Верхняя карточка модели/лимитов сохраняет размеры;
возможности и доступность выборов определяются нативным backend.

Робот открывает компактный терминал, внизу крупные дополнительные клавиши/Enter.
Отдельная кнопка «Вернуться в чат [агент]» размещена над нижними кнопками,
чтобы длинное название не пересекалось с «Свернуть» и Enter. В чате поясняется,
почему ещё требуется действие в терминале. Клавиатура вызывается по тапу даже
для TUI, перехватывающего прикосновения как события мыши.

Файловые менеджеры отделяют навигацию от выделения. Долгий тап/галочки выбирают;
обычный тап открывает. Выбрать всё действует по видимому фильтру. «× N» и кнопки
передачи имеют отдельные места и не перекрываются. Подключение сервера и его
автообновление данных — разные понятные действия. Новых пунктов обратной связи нет.

## Prior v1.5.3 design (historical)

---

# Интерфейс v1.5.3

Корзина — векторная кнопка после Home; обводка показывает выделение и
доступность действий. Активные серверные передачи занимают строку над Open
только пока нужны, с прогрессом и восстановлением окна. Тонкая линия активности
терминала1dp и рамка Server + Agent имеют собственные кадры, примерно60fps,
при скрытии останавливаются. Вывод страницы ими не пересоздаётся.
У AI Agents в списке открытых сессий робот. Верхние кружочки промежуточного
эксперимента отменены, прежний Toast заголовка сохранён.

В бэкапах custom title содержит название слева, Refresh/spinner справа
и одну строку текущего сканирования снизу. Метод и уровень сжатия разделены.
Действия заблокированы до оценки, поздние callbacks закрытого окна игнорируются.

Локальных AI-чатов/настроек в текущем интерфейсе нет. Описания таких элементов
ниже исторические; итог текущего выпуска имеет приоритет.

---

# Интерфейс и поведение v1.2.1

## Дополнения v1.3.0

Общие настройки получили офлайн-справочник с поиском, фильтрами и карточками
RU/EN; внутри видны применимость, статус доказательств, источники и действия
без автоматической отправки в терминал. Источник проекта отделён от upstream.
Общий менеджер имеет режимы выбора ключа, до50 файлов и папки скачивания.
Для памяти устройства до открытия проверяется доступ. Меню файла сервера
сохраняет быстрый Download рядом с новым выбором папки. [Подробности](LOCAL-FILE-SOURCES.md).


## Сервер + Agent

Кнопка106dp под пакетами/папкой справа от AI-карточки в общей строке, видна и без
AI. Основные плитки навигации не меняются. Экран сервера: профиль, CPU/RAM/диск
без графиков, четыре раздела с отступами, стабильные контейнеры/прокрутка.
Опрос меняет значения на месте. Векторные кнопки файлов48dp; звёздочка и лупа,
горизонтальная прокрутка узкой панели. Избранное/поиск/просмотр/действия — диалоги.
Журнал задачи обновляет текст и доступные действия без пересоздания страницы.
Передача показывает объём/скорость/ETA с отменой, сообщения об ошибках не выводят
URL медиатокена. Названия/пояснения доступны RU/EN.


Документ описывает текущий основной форк. Исходный [референс](reference.png) и ранние макеты отражают разработку, а не точный интерфейс релиза.

## Навигация и оформление

Сверху компактные плитки с иконками главной, команд/заметок и терминала, общая шестерёнка с языком и настройками. Нижней навигации нет. Чёрный фон, бирюзовые акценты, скруглённые тёмные панели. Терминал использует исходный шрифт и интервалы; отдельной настройки интервалов нет. Смена title обновляет заголовки и вкладки без пересоздания страниц. Карточки уведомлений о фоновых событиях пока не реализованы.

Окна редактирования, просмотра и действий имеют общий тёмный стиль. Полный текст команд и заметок доступен во внутренней прокрутке.

## Адаптация размеров

Разметка учитывает плотность экрана и системный шрифт; параметры DPI Android не меняются. На просторном экране панели прокручиваются независимо. Если клавиатура или малое окно оставляют мало места, панели сохраняют минимальную высоту и появляется общая прокрутка. Фильтры допускают две строки, кнопки + остаются справа. При смене вкладки клавиатура скрывается. Это не гарантирует идеальный вид на всех моделях и при любых системных настройках.

## Главная

Журнал и сохранённые сессии имеют собственные прокручиваемые панели. Журнал содержит до 50 отправленных кнопкой команд: новые сверху, повторы записываются отдельно. Есть время, поиск и очистка с подтверждением. Троеточий у записей нет: нажатие открывает просмотр, вставку, сохранение в библиотеку и копирование. Вставка и прямой ввод в TerminalView не добавляют записи.

«+» у сохранённых сессий создаёт профиль без запуска. Обычные терминалы появляются в сохранённых только после явного сохранения.

## Команды и заметки

Общий поиск проверяет название, текст и категорию в обоих списках. Каждый список имеет свои фильтры «Все команды/Все заметки», «Избранное», «Категории» и кнопку + справа.

Команда хранит название, многострочный текст, категорию, избранное. Нажатие открывает полный просмотр с кнопкой вставки; при нескольких терминалах можно выбрать получателя. В окне просмотра: избранное, редактирование, удаление с подтверждением. Отдельных кнопок вставки, копирования и дублирования на карточке нет.

Заметка имеет название, текст, категорию и избранное. Нажатие открывает полный просмотр; меню позволяет изменить избранное, отредактировать или удалить.

Звёздочка у ввода открывает вкладки «Сохранённые команды» и «Заметки». В каждой свой поиск, избранные первыми. Запросы сохраняются при переключении внутри открытого окна. Выбор заменяет текст поля текущего терминала, не отправляет его и не меняет каталог. «Сохранить ввод» сохраняет команду.

## Сессии и меню

Профиль: имя, каталог, до трёх команд и среда. Карточка запускает профиль или открывает активную вкладку. Изменение имени применяется сразу, каталога и команд — при следующем запуске. В окне новой сессии «Сохранённые» открывает выбор профиля.

| Место | Действия |
| --- | --- |
| Троеточие сохранённой карточки | Закрыть сессию, настройки, удалить сессию |
| Долгий тап по активной вкладке | Перезапустить, настройки, закрыть; сохранить сессию, если она ещё не сохранена |
| Шестерёнка терминала | Общий выбор языка и настроек приложения; интерактивный ввод и дополнительные клавиши — отдельные векторы терминала |

Закрытие останавливает процесс и убирает вкладку. У сохранённой сессии профиль и история Bash остаются. Для обычной предлагаются «Сохранить и закрыть» или «Закрыть без сохранения», без кнопки отмены. Во втором случае её профиль и история Bash удаляются после остановки. История Codex хранится отдельно и закрытием вкладки не удаляется. Профиль не восстанавливает живой процесс или прежний экран; снимков вывода нет.

## Ввод и запуск

Enter в поле ввода переносит строку; отправка — отдельной кнопкой. «Интерактивный ввод» включает прямой ввод в TerminalView, поле остаётся доступным. Шаги запуска Termux/Ubuntu ждут готовности Bash, выполняются последовательно, ошибка останавливает оставшиеся шаги. Интерактивная программа должна завершиться до следующего шага. Отмена запуска не прерывает уже работающую программу. Подробности — [USAGE](USAGE.md).

## Codex и ввод v0.2.17

Карточка под терминальными вкладками показывает модель, поведение, состояние и лимиты. Выбор проходит три шага: модель → поведение → подтверждение. Поиск в звёздочке открывает клавиатуру по нажатию, без автоматического открытия при показе окна. Черновик нижнего поля отдельный для каждого терминала, с сохранением выделения, только в памяти.

## Локальный интерфейс v0.2.18

Горизонтальные вкладки терминалов заменены одной строкой: раскрывающийся выбор сессии слева, + рядом, Файлы справа. Стопка прокручивается вертикально; длинные названия не расширяют общий экран. Компактная карточка модели/поведения/лимитов находится под строкой с выравниванием слева. Поле ввода, Enter и отдельные черновики сохраняют прежнее поведение.

Кнопка папки в форме заполняет существующее поле каталога. Чаты проекта выбираются с поиском и подтверждением; новый/сохранённый диалог не смешиваются. Файловый браузер — отдельное окно с ограниченным списком и фильтрами, не новая вкладка навигации. Terminal закреплён как базовая оболочка и не предлагается для удаления.

## Уточнения локальной v0.2.19

Справа в терминале только векторная кнопка папки 48dp, без подписи. Icon-only TextView рисует значок по центру реальных границ независимо от шрифта/compound padding. Звезда, плюс, папка, вверх и дом используют согласованные векторы. Над карточкой Codex отступ6dp, под заголовком стопки10dp, между карточками6dp. Файловые фильтры имеют постоянное выделение выбранного состояния.

Окно истории: Все чаты по умолчанию, Чаты этой папки дополнительно. В каждой карточке виден исходный каталог; перенос рабочего каталога требует отдельного подтверждения с обеими папками и объяснением сохранения прежнего контекста.

## v1.0: язык и плитки

Плитки навигации 56 dp высотой, прежние веса ширины и отступы; только вектор 28 dp по центру, без видимого текста и длинного тапа. Глобус 48×48 dp рядом с шестерёнкой открывает три языковых варианта. Выделенный вариант языка использует общий стиль WorkspaceDialogs. Переводы не изменяют положение ввода и процессов. Проверка реального DPI/переносов текста требует устройства.

## Пакеты

Вход через долгий тап на название сессии → Пакеты, без нового пункта шестерёнки.
Заголовок окружения, название удержанной сессии и описание сохранности пакетов.
Три фильтра Установленные/Доступные/Обновления, поиск имён, прокручиваемый список
с постепенным показом строк; Перечитать список и Обновить каталог разделены.
Карточка выбранного пакета открывает подтверждение команды и её окружения.
В подтверждении два радиопункта: выбранная сессия (по умолчанию) и новая сессия того же окружения. Предупреждение требует свободный Bash; если выполняется команда или Codex, пользователь выбирает новую. После подтверждения приложение показывает выбранное назначение.

## Выбор версии и размеры кнопок

Пакеты: кнопки фильтров и двух действий имеют высоту 56dp, центрированные подписи
в одну строку с autosize 10–15sp. Это устраняет изменение высоты из-за переноса
последней буквы. Карточка пакета содержит Выбрать версию; выбор открывает обычное
подтверждение с дополнительным предупреждением при downgrade.
Плашка шестерёнки использует PANEL/BORDER/radius 12 как глобус, оба размера 48dp,
отступ 8dp между ними; верхние навигационные плитки не меняются.

## Описания и окна библиотеки

Карточка пакета: название/версия, описание до трёх строк, Подробнее, существующие
действия. Полный текст имеет фиксированное ограничение высоты и внутреннюю прокрутку.
Команда/заметка: единое окно текста и действий в стиле недавней команды, без отдельного
троеточия списка. Редактор категории — тёмное поле с подсказками и свободным вводом.

Поле категории и отдельная кнопка ▾ 48dp с отступом 8dp. Выбор использует стандартное тёмное окно списка; поле остаётся редактируемым.

Редактор v1.0.4: после Просмотреть, путь/имя, моноширинное многострочное поле с
внутренней прокруткой, UTF-8/лимит, Сохранить/Отмена. Ошибка показывается внутри
редактора без удаления буфера; назад/отмена с изменениями требуют подтверждения.

## Дополнения v1.0.18

Долгий тап сессии содержит диагностику. Её окно показывает структурированные
события, auto-refresh, pause/resume и экспорт; сбор независим от видимости окна.
Пресеты запуска имеют опцию расширенного журнала. Вектор отмены будущих шагов
внутри плашки текущей сессии, с подтверждением и списком всех настроенных команд,
не переносится в popup списка. Два вектора интерактивного ввода/дополнительных
клавиш — только в терминале,48dp, selected-fill как у фильтров. Поле ввода неизменно.
AI Agents имеют native режимы, отдельные подключения и явно экспериментальный статус.

## Базовый updater (введён в локальной v1.0.19)

Common settings adds Check for updates / Проверить обновления between Language
and app settings. The update dialog shows localized highlights and download
action; download shows percentage and cancel; successful verification presents
a separate Install action. Android owns installation confirmation and unknown
source permission. Session controls, input and diagnostics UI remain unchanged.

## v1.1.0

В общей шестерёнке добавлены автопроверка10ч и резервное копирование · бета.
При доступном обновлении маленькая точка на gear; нет автоматического popup.
Backup UI: создание → выбор состава/каталога/подтверждение секретов; проверка →
компоненты/совместимость и пакеты; восстановление → выбор/план/подтверждение.
Диалоги RU/EN, системные пути и имена пакетов не переводятся. Терминальный ввод
и отображение агентов этим изменением не переделываются.

## Выборочный бэкап v1.1.1

Полная/выборочная копия выбирается до формы. Выборочная форма — раскрываемые
карточки Termux/каждой Ubuntu с чекбоксами и размерами до сжатия, добавление папки
HOME, затем отдельное подтверждение состава/зависимостей. Добавление системы
возвращает к обновлённому выбору для повторного просмотра размера. При импорте
показываются обнаруженные целевые среды и предупреждения о runtime/архитектуре.
Стили карточек/действий общие WorkspaceDialogs; тексты RU/EN, Android dp/sp.

В локальной v1.1.2 режим называется «По выбору». Недоступные компоненты отключены
с подписью неполного размера и причинами в карточке окружения. Кнопки создания
неактивны/приглушены до согласия в обоих режимах. Над путём — «Папка создания
резервной копии». Проверка обновления/проверка APK имеют indeterminate-индикатор,
скачивание — горизонтальную полосу, процент и объём. Все новые подписи RU/EN.

### v1.1.3 backup dialogs

Backup titles use (тестирование)/(testing). All nested backup dialogs reject
outside-tap dismissal; other menus retain their existing behavior. Progress shows
stage, stage bar, one ellipsized current path, amount and elapsed time. No ETA or
invented global percentage. Creation finishes with a verified message and path.
Compression uses protected list dialogs rather than unprotected spinner popups;
gzip/pigz explanations, levels1/3/9, optional confirmed pigz installation with
inline status. Product descriptions refer to target environments, not test phones.

### v1.2.0 update tile

One settings tile replaces two update rows. Title opens a manual check; the right
square checkbox, labelled Auto, persists the automatic preference without closing
the menu or checking immediately. A muted inline hint says Check when opening
the app. Both Russian and English are localized. Existing gear badge remains.


## Диагностика v1.2.2

Общий DiagnosticsPanel для сессий и серверов: сводка/события, ограниченное кольцо, пауза и экспорт. Серверные события типизированы, без команд/путей/полезной нагрузки; получение отчёта не создаёт RPC. Состояние паузы и опроса вне вкладки сохраняется отдельно в профиле. [Подробности](DIAGNOSTICS.md).

## Локальная v1.3.1

В звёздочке preview использует прежнюю панель полного текста с внутренней
прокруткой и существующие векторные действия. Для команды/заметки доступны
редактирование, вставка в текущую сессию и подтверждаемое удаление.
Заголовок справочника — горизонтальный ряд: название с весом1 слева,
локализованная AI-подпись с ограниченной шириной справа; текст переносится.

### Возврат из просмотра звёздочки — v1.3.2

Кнопка «Назад» / Back возвращает существующий список, сохраняя вкладку,
поисковый запрос и позицию. Системная отмена ведёт туда же. Кнопка закрытия
самого списка остаётся прежней. Изменение не касается других просмотров.

### Локальная v1.3.4

Поиск команд/заметок сохраняет объекты панелей и фильтров; обновляются только карточки, без задержки ввода.

Local AI adds an optional native chat panel inside the terminal workspace, reusing
the session selector and multiline input. It keeps a stable message container and
throttles streaming updates; only the selected local model is loaded.

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


## v1.5.0 selection interaction

Server rows enter checkbox selection on long press. Two equally sized centered vector panels replace the single upload button: left ↑ uploads, right ↓ opens Download / Move. A gap separates their touch areas; the parent-directory vector now points left without changing its action. Its menu offers Download / Move / Select all / Clear; pending same-profile move uses a folder-arrow vector and Move here / Cancel move, preserved across directory navigation. Directory navigation clears ordinary selection. Public session/preset environment chooser retains a disabled Local AI row with reduced opacity; existing local-preset launch is denied with an explanatory dialog, not deletion.


## Environment aliases (v2.0.9)

See [Environment aliases](ALIASES.md) for the separate library, category and star
pickers, environment-scoped shell files, write safeguards and restart semantics.
This feature excludes Ubuntu AI and does not alter running sessions.
