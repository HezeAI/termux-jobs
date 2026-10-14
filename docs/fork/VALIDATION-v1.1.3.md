# Проверка локальной v1.1.3

APK для пользовательской проверки, GitHub не опубликован. Публичная Latest v1.1.0.

- Файл: Termux-Jobs-v1.1.3-arm64.apk, 34381132 bytes.
- com.termux, versionName1.1.3, versionCode1060, minSDK24/target28, non-debuggable.
- Только arm64-v8a. Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- SHA-256 APK: `57cf072dbc55e50c438868a15ac32ca6e7d9c63cfbed321ccee71ca96a521473`.
- ZIP CRC, выравнивание stored entries, ABI, отсутствие debug flag проверены.
  Подпись v2 проверена apksigner; пакет/сертификат совпали с v1.1.2, код выше1059.
- assets/native сравнены с v1.1.2: изменён только workspace-backup.py; bootstrap
  и .so прежние. Изменённые Java исходники совпали с изолированной build-копией.
- Просканированы имена APK entries и текст/DEX на известные credential patterns;
  не найдено auth/SSH/keystore файлов, pyc или совпадений проверенных secret patterns.
  Сканирование не является универсальным доказательством отсутствия любых секретов.
- update.json собран из финального APK, RU/EN compatibility без требования сброса.
  Ссылка future v1.1.3 не опубликована и сейчас не является доступным обновлением.

## Тестирование

309 JVM/JUnit/Robolectric checks, 0failures/errors/skipped; release build successful.
24 базовые +22 selective +14 compression/progress Python checks, все прошли.
Новые проверки: уровни1/3/9 и roundtrip, gzip/pigz protocol, ошибка компрессора,
освобождение только owned child при отмене, штатный proot stdout/legacy fallback,
фиксированная установка только pigz, ошибки APT и ожидание owned APT вместо
намеренного прерывания dpkg при отмене worker. Все процессы — собственные fixtures.
Проверены промежуточные байты большого файла, bounded filenames без file contents,
чтение gz/bz2/xz, UI known/unknown totals, pages, таймер, замена одной строки,
защита трёх форм диалогов от outside tap и работа явной Cancel.
Первый прогон выявил старое ожидание слова «бета» и непроработанную очередь
нажатия Cancel в новом тесте; исправлены ожидание и обработка main looper.
Повторный прогон прошёл. Производственный код для этих двух сбоев не менялся.

## Ревью и логическая инспекция

Scoped outside protection активна только в BackupUi; все её builders проверены.
Явная отмена бэкапа останавливает только own worker/компрессор/proot subprocess.
Установка требует отдельного согласия, команда фиксирована, APT locks сохраняются.
Во время установки создание бэкапа блокируется; после успеха доступность проверяется.
При отсутствии/ошибке pigz доступен gzip. Ошибка потока не снимает INCOMPLETE.
Новые optional compression metadata не меняют manifestv1/v2; libraryv8 сохранена.
Содержимое subprocess stdout/stderr не попадает в JSON/отчёт (кроме намеренного
TAR stdout от штатного proot, направленного только в архив). Progress — имена путей,
числа и фиксированные стадии. Компрессор ограничен4 потоками.

## Ограничения доказательств

На реальной рабочей среде APT/pigz installation, backup/restore и установка APK
не запускались; сессии/Bridge не изменялись. Нет замера скорости реального pigz:
fixture имитирует его gzip-протокол, а не доказывает ускорение/нагрев. Native
proot fixtures проверяют маршрут stdout, но не полноценную работу всех версий
proot-distro. UI tests не заменяют проверку жестов/DPI/клавиатуры на Android.
Нужен пользовательский прогон: отказ/согласие установки pigz, фоновой статус,
выбор/создание/проверка архива, таймер, известный/неизвестный объём, outside tap
в подменю и явная отмена. Не закрывайте приложение до завершения APT/бэкапа.
Upstream lintVital печатает Unsupported OS/arch: Linux/aarch64 (JNA), хотя Gradle
сборка успешна; это не подтверждение полноценного чистого lint-прогона.
Нет foreground service/wake lock/ETA; Android может прервать процессы. Не1:1 restore.

Receipts: .verification/backup-progress-v1.1.3/ (build, Python, XML, APK, signature,
badging, artifact.json, SHA256SUMS, update.json). Никакие результаты не публиковались.
