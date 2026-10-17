> Исторический отчёт локальной сборки. Текущий публичный выпуск: [v1.2.0](RELEASE-v1.2.0.md).

# Проверка локальной v1.1.8

Историческая неопубликованная сборка. Автоматические проверки ниже не подтверждают сценарии на устройстве.

- APK `Termux-Jobs-v1.1.8-arm64.apk`: 36441003 bytes; SHA-256 `3684d58af5ed2fbf66584cb17506c6cd2d3baf22ebf8571e415c225847ff53c6`.
- com.termux / versionName1.1.8 / code1065 >1064. ARM64, min24/target28,
  non-debuggable. Прежний certificate SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`,
  APK v2 signature проверена apksigner.
- ZIP CRC, отсутствие duplicate/traversal entries, stored4byte alignment,
  package/version/ABI и совпадение финальных source/build-stage проверены.
- Agent asset и native libraries побайтно прежние. Переустановка агента не нужна.
- Ограниченное сканирование APK не нашло private-key headers/token patterns
  и приватных auth/keystore/профилей/pyc имён. Это не универсальный детектор секретов.
- Всего 352 JVM tests, failures/errors/skipped=0 на финальных зависимостях.
  Включены восемь тестов с настоящим локальным OpenSSH SFTP subsystem по pipes:
  Unicode/quotes/space/backslash, несколько блоков/ACK progress, явная замена,
  нулевой файл, предотвращение неявной перезаписи и обнаружение смены файла,
  ошибка input, отказ symlink/unsafe names, отмена и отдельная очистка temporary,
  отказ повреждённых пакетов. Все fixture-файлы внутри verification stage.
- Robolectric: menu View без подключения, OPEN_DOCUMENT flags/request/captured
  endpoint/cancel, loopback capability URL validation, обновление журнала на месте,
  удержание одинакового text buffer, stale inline result cleanup. Старые проверки
  редактора/ревизий и изоляции терминальных/AI путей также прошли.
- 581 existing localization strings/settings keys/format arguments: PASS.
  Новые серверные строки имеют RU/EN; git diff --check: PASS.
- Release build успешен. lintVital запускался, но старый lint engine несовместим
  с JNA Linux/aarch64, metadata Kotlin современных AndroidX и их custom lint
  (Vendor class). Чистый полный lint НЕ заявляется. Компиляция Java/D8 и упаковка
  успешны. Первая попытка D8 с Guava JVM-вариантом не прошла; исправлен выбор
  Android variant33.3.1 с повторными успешными тестами/сборкой, minSdk не повышен.
- Media3 1.5.1 закреплён под текущий сборщик, compileSDK35 build-only.
  Apache2 license включена в assets/media3-LICENSE.txt. Программных декодеров нет.
- Agent.py не менялся; 19 Python tests исторической v1.1.5 относятся к тому же
  байтовому asset, повторное выполнение не заявляется.
- update.json создан из финального APK, локальный, не опубликован.

## Инспекция и границы

Viewer/editor закрывают собственный backend; не сохраняют drafts автоматически.
Сохранение использует прежнюю TextFileEdits ревизию/atomic save. Повторные открытия
не удерживают старые backend/snapshot после закрытия. Изменённый общий editor
сохраняет прежний constructor/API; его прошлые тесты прошли.

Media3 не создаётся при открытии списка серверов или обычного терминала. Создаётся
по явному открытию media и готовому tunnel. onStop/close освобождают decoder/view
и только свой tunnel; external handoff удерживает bounded tunnel. Stale results
после навигации закрывают свой tunnel. Никаких сигналов чужим процессам/PID нет.

SFTP использует отдельный pinned SSH child и отдельный password broker в тех же
границах SSH-пользователя. URI/name никогда не превращаются в shell command.
Файлы не staging-копируются в общую память. Temp O_EXCL0600; publish no-clobber
либо явный posix-rename с optimistic size/mtime/mode проверкой. Это НЕ CAS:
сторонний писатель может изменить файл между проверкой и rename. Замена меняет
владельца на SSH-пользователя, не копирует ACL/xattrs/ownership; это описано в UI.
Отмена закрывает только upload child/input, cleanup повторяется для известного
случайного temp path. При недоступном сервере путь показывается; force-kill может
оставить temp. Неопределённость commit при обрыве требует проверки каталога,
автоматического повторного overwrite нет. Документы, ключи и auth не публикуются.

Обычные терминалы/AI/пресеты/библиотекаv8/профили без миграции или сброса.
HelpActivity больше не вызывает удалённый SDK AppCache setter; min24/target28
и прочие настройки WebView прежние. Установленное окружение/сервер/бот не менялись.

## Ещё проверить на устройстве

Встроенный плеер: реальный MP4/аудио/неподдерживаемый codec, seek/pause, Close,
фон/возврат, external handoff/Download и отсутствие затронутых терминалов.
Реальный SSH/SFTP password/key auth, Android picker/provider grants, Unicode,
collision rename/replace/skip, cancel/обрыв и содержимое/права destination.
Редактор: .md, клавиатура, изменение сервера между read/save, отмена draft.
Журналы: реальное отсутствие мерцания, выделение и scroll/DPI/жесты.
JVM/pipes не доказывают физический рендер или аппаратное воспроизведение.

Перед ручной установкой завершите важные операции: Android может завершить
приложение при обновлении. Подпись/пакет совпадают, code выше, сброс не предусмотрен;
реальное сохранение данных после этого обновления подтверждает пользователь.

Receipts: .verification/servers-v1.1.8/jvm-final-android-guava.log,
build-final2.log, signature.txt, badging.txt, audit.py, artifact.json,
SHA256SUMS, update.json. Предыдущие неуспешные попытки сохранены отдельно.
