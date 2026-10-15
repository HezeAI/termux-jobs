> Исторический отчёт локальной сборки. Текущий публичный выпуск: [v1.2.0](RELEASE-v1.2.0.md).

# Проверка локальной v1.1.5 — Сервер + Agent

На GitHub не опубликовано. APK агентом не установлен.

- Termux-Jobs-v1.1.5-arm64.apk, 37889441 bytes.
- SHA-256 `2f52dda6cda3ecb824d13ef9e41d82c9ae0f6c6291a533b6abc24017d721cd2c`.
- com.termux, versionName1.1.5, versionCode1062 >1061, min24/target28, только ARM64.
- Сертификат SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`; сверено с APKv1.1.4.
- ZIP CRC/дубликаты/пути и 4-byte alignment stored entries проверены. v2 signature
  проверена apksigner. Native/прежние assets идентичны v1.1.4; добавлен agent.py.
- Бандл агента побайтно совпал с проверенным исходником. Проверены известные
  credential patterns/private-key headers и отсутствие приватных auth/keystore/pyc
  файлов; это не универсальное доказательство отсутствия любого секрета.

## Выполнено

321 JVM tests: 0 failures/errors/skipped. Включают новые проверки хранения профилей,
read-only при повреждении, SSH pinned keys/argv, отсутствия автоматического
подключения/установки, пояснения агента и lifecycle паузы.
19 Python tests: очередь/валидация/права файлов/защита от перезаписи, атомарная
установка с SHA/размером, HTTP Range и изменённые файлы, ограничения потоков,
удаление с блокировкой задач, сохранение задачи после выхода клиента.
Реальный локальный daemon: task execution, SIGTERM-ignoring группа получает SIGKILL,
соседняя задача продолжает работу, метрики пишутся, затем агент штатно удаляется.
Стенд внутри .verification; сервер пользователя и его процессы не трогались.

Release build успешен. lintVitalRelease выполнен, но legacy JNA сообщает
Unsupported OS/arch Linux/aarch64. Полный чистый lint не заявляется.
Локализация прежних581 строк проверена; новый экран имеет RU/EN текст отдельно.
Синтаксис Python, bash -n build script, git diff --check пройдены.

## Ревью и логическая инспекция

Нет изменений SessionShell/SshSessionShell/WorkspaceStore: прежний SSH пункт
сохранён. Библиотекаv8 без миграций/очистки. Новые профили отдельно, без импорта
в export библиотеки; пароль Keystore с binding к id/host/user/port.
SSH host scan требует ручной независимой сверки. StrictHostKeyChecking=yes,
отдельный known_hosts, forwarding агента/локальные SSH-команды отключены.
RPC структурированный; команды задач выполняются только после UI-подтверждения.
Процессами управляет только владеющий Popen демон, без PID из метаданных.
Перед reap отменённой задачи группа получает завершающий сигнал; старые running
и starting записи после аварии становятся unmanaged, без автоматического replay.
Удаление агента блокируется queued/starting/running/unmanaged, удаляет только ROOT.
Мониторинг onStop приостановлен; явный медиатуннель имеет отдельный срок30мин.

## Не проверено на устройстве

Реальный SSH-пароль/Keystore/ключи, доверие хосту, установка/удаление агента
через Android UI, доступ к Download, DPI/клавиатура/жесты/плеер и background
ограничения Android требуют ручной проверки. Реальное обновление APK не выполнялось.
Агент требует Linux/Bash/Python3.8+, не получает права ниже SSH-учётной записи
автоматически и не устанавливает системный автозапуск. Задачи не переживают
перезагрузку ОС и не рассчитаны на отделение процессов через &/nohup.
Подробно: SERVERS.md. Локальный update.json подготовлен, не опубликован.

Доказательства: .verification/servers-v1.1.5/ — build-final.log, jvm-final.log,
python.log, badging.txt, signature.txt, baseline-signature.txt, artifact.json.
