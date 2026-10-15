> Исторический отчёт локальной сборки. Текущий публичный выпуск: [v1.2.0](RELEASE-v1.2.0.md).

# Проверка локальной v1.1.10

Локальная сборка, не опубликована. Установки APK/агента/пакетов и вмешательства
в работающие серверы/задачи/терминалы не выполнялись.

- `Termux-Jobs-v1.1.10-arm64.apk`: 36449120 bytes; SHA-256 `8274cecbf08df3ec143951e71cc627677fa55c153104cd7ece2a3095def30e4f`.
- com.termux, versionName1.1.10, code1067 >1066, min24/target28, ARM64,
  non-debuggable. Прежний cert SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`;
  APKv2 проверен apksigner. aapt2 package/version/ABI checked.
- ZIP CRC/no duplicates/traversal, stored4byte alignment, source Java/resources/
  assets == stage проверены. Agent/native identical v1.1.9. Ограниченный скан
  private-key/token patterns и приватных auth/keystore имён не выявил секретов;
  не является универсальной гарантией отсутствия секретов.
- 367 JVM tests, 0 failures/errors/skipped. 40 ServerWorkspaceTest включают
  новое меню и порядок items, grouping Check/Remove/Components без remote calls,
  подтверждение установленного/отсутствующего агента с верной видимостью действий,
  ошибку сети без ложной установки/сброса cached presence, Python<3.8 без install,
  duplicate-install guard до создания соединения,48dp targets/8dp gap !/overflow.
  Прежние terminal/process isolation, scroll/cards, SFTP/editor checks также прошли.
- Release build successful; полный чистый lint не заявляется: старый lintVital
  имеет прежние несовместимости JNA Linux/aarch64/Kotlin metadata/custom lint.
- localization581 существующих строк/settings/format arguments PASS; новые RU/EN
  строки проверены вручную и targeted UI tests. git diff --check PASS.
- update.json создан из финального APK, не опубликован. Реального Android install
  и end-to-end SSH/Python state check на сервере разработчик не выполнял.

## Ревью/инспекция

Agent settings UI только группирует действия. Проверка состояния — прежний
read-only Python probe поверх pinned SSH, не импортирует/не запускает agent.py,
не ставит пакеты и не меняет файлы. Результат содержит обязательный boolean agent;
ошибка/неполный ответ остаётся неизвестным состоянием и скрывает Install.
При Python<3.8 установка не предлагается. Успешный status protocol1/metrics
подтверждает наличие агента; новый probe уточняет текущее состояние. Cache
ephemeral по profileid, invalidated при изменении/удалении профиля.
Подтверждённое отсутствие сбрасывает readiness/snapshot и не оставляет автоопрос
несуществующего агента. Устаревшие ответы блокируются существующим generation.

Install guard предотвращает повторное предложение при известном installed до
создания SSH; серверный atomic no-overwrite installer прежний и остаётся защитой
если файл появляется между проверкой и установкой. Само открытие меню ничего не
устанавливает. Remove доступен отдельно и из installed-state dialog; прежнее
подтверждение/agent-side active/unmanaged task checks не изменены. Known-absent
не запускает RPC удаления. SFTP/player/editor и агентный process management не менялись.

Disconnect — прежняя операция с новым названием. Закрывает owned connection/
tunnel, прекращает polling; не посылает stop агенту/задачам/службам и не касается
отдельных SSH терминалов. Между !/overflow8dp, touch targets48dp. Изменений формата
libraryv8/профилей/пресетов/AI/данных нет, reset/migration не предусмотрены.

## На устройстве ещё проверить

Открыть меню сервера: Settings first, Check/Remove/Components в подменю, !/overflow
разделены. Живые installed/missing/network failure/Python cases, повторная проверка,
закрытие диалога до ответа, отмена install/remove подтверждения. Реальное удаление
с активной задачей проверять только отдельно по явному согласию — разработчик
не удалял рабочий агент. Disconnect/reconnect и отсутствие влияния на server task/
обычный SSH, RU/EN, DPI/жесты требуют ручной проверки. JVM tests не доказывают
реальную отрисовку или внешний SSH. Сохранность данных после установки проверяет
пользователь; подпись/пакет и повышение code проверены. Перед обновлением завершите
важные операции: Android может закрыть процесс приложения.

Cron/systemd timer согласованы только как следующее обновление и в этот APK
не входят. Server agent переустанавливать не нужно. GitHub не обновлялся.

Receipts: .verification/servers-v1.1.10/jvm-final.log, build-final.log,
signature.txt, badging.txt, audit.py, artifact.json, SHA256SUMS, update.json,
localization.txt.
