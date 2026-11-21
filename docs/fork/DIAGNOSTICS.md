# Диагностика сессий и серверов (v1.2.2, локальная проверка)

Диагностика сессии относится к выбранной вкладке Termux, Ubuntu, SSH или AI.
Сводка показывает версию приложения, известное состояние процесса, время запуска,
события и наблюдаемые ошибки. Работающий процесс не подтверждает авторизацию AI;
отсутствие вывода не означает зависание.

В окне можно переключить обычный и подробный журнал без перезапуска. Подробности
записываются с момента включения: старый вывод не восстанавливается. Переключатель
не меняет права агента и параметры уже запущенной CLI. Нативная подробная
диагностика Gemini по-прежнему задаётся при запуске.

Одна кнопка останавливает или возобновляет запись. Доступны копирование отчёта,
копирование пути к сохранённому отчёту и экспорт в Download. Полный отчёт сохраняется
в файл; слишком большой текст для буфера обмена сокращается с уведомлением.

Диагностика сервера открывается кнопкой слева от «!». Она собирает локальные
числовые сведения о существующих запросах: очередь, длительность SSH/операции,
разбор ответа, размер, результат и скорость завершённых передач. Дополнительных
запросов на сервер ради журнала нет. Команды, пути, адреса, полезная нагрузка и
необработанные сообщения исключений в этот технический журнал не записываются.
Остановка журнала сохраняется отдельно для каждого профиля и не отключает агент,
задачи или мониторинг. Кольцо ограничено 2 МиБ на профиль / 32 МиБ суммарно;
в окне последние 200 событий. После завершения процесса приложения кольцо теряется,
экспортированные файлы сохраняются.

## Опрос

После подключения выбранный сервер опрашивается каждые 10 секунд. Галочка справа
от адреса в карточке разрешает продолжение опроса каждые 30 секунд в списке серверов,
терминале и остальных разделах открытого приложения. Без галочки вне выбранного
сервера опрос выключен. Галочка сохраняется в профиле; сама по себе она не подключает
новый сервер. После перезапуска приложения требуется подключение профиля.

Маленькая жёлтая точка означает включённый опрос, красная — выключенный. Это не
состояние питания сервера и не доказательство доступности агента. Ошибка запроса
отображается отдельно. При сворачивании приложения новые опросы приостанавливаются.
Опросы выполняются последовательно, без накопления параллельных запросов.

Пользовательские команды и проекты не изменяются. Новые поля профиля необязательны;
старые профили загружаются с выключенным опросом вне выбранной вкладки и включённым
обычным журналом. Формат библиотеки команд/пресетов остаётся прежним.

## Phone files and tool access

Identify the active Termux or Ubuntu AI/PRoot environment before accessing phone
files. Available tools, sandbox policy, Android permissions and PRoot mounts
control access. This guide grants no additional access.

On this phone, Ubuntu AI can read shared Android storage through `/sdcard`.
Verify the path in the active session. The same path may have different access
on other phones. Termux-private files require their own accessible mount and
app permissions.

- Screenshots are under `/sdcard/Pictures/Screenshots`.
- Downloads, including requested APK or PNG deliveries, are under `/sdcard/Download`.
- `/storage/emulated/0` may be another path to the same shared storage.

For the latest screenshot, find the newest suitable image by modification time
and exclude video files. For an exact filename, use that file. Check access
with a read-only command and report the actual error if it fails. Use the image
viewer for visual inspection. Report a missing viewer separately from a file
access failure. Ask the user to attach the image when the required file access
or viewer is unavailable.

Use the shell and file tools for authorized local inspection, project changes
and isolated builds. Read access grants no permission to change files. APK
installation, changes to working sessions, external messages and publication
require applicable user authorization. Keep tokens, private keys and the contents
of authorization files out of reports.

## Открытие проектных инструкций / Opening project instructions

Видео Screenrecorder-20261103-095607.mp4 показало разные состояния одного проекта:
AGENTS.md существует и уже открыт в редакторе Codex; CLAUDE.md отсутствует.
Обработчик Codex завершался переводом фокуса к существующему файлу и добавлял
сообщение состояния, меняющее размеры окна. Форма создания в этой ветке не
создавалась. Настоящий файловый обработчик подтвердил оба состояния без записи.

Кнопка теперь показывает «Редактировать AGENTS.md/CLAUDE.md», если основной файл
существует. Она переводит к полю и открывает клавиатуру, сохраняя текст и выделение.
«Создание инструкций проекта» показывается при отсутствии файла. Пока идёт чтение,
используется нейтральное «Открыть инструкции проекта». Отдельная диагностика окон
не добавлена: для показанного случая причина установлена по видео и коду.

The video showed an existing AGENTS.md and a missing CLAUDE.md. The existing-file
branch focused the current editor and added a status row; it never created a new
form. The actual file handler reproduced both states read-only. The action now
says Edit AGENTS.md/CLAUDE.md when the base exists and focuses that editor/keyboard
without a new status row. Missing files retain Create project instructions;
loading uses Open project instructions. No separate window-diagnostics feature
is added. Device acceptance of the clearer action remains distinct from model tests.
