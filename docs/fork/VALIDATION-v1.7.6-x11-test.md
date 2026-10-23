# Проверки1.7.6-x11-test / code1221 — этап1

Для проверки разработчиком, без GitHub-публикации. Это встроенное тестовое X11
окно в одном APK, пока без Ubuntu/XFCE и без замены TerminalView.

## Артефакт

- com.termux,1.7.6-x11-test/code1221, minSDK26, ARM64-v8a, non-debug.
- Размер: 38890526 байт (примерно +2.06MiB к1.7.5).
- SHA-256: `c0f420d17b81fb647c4f75de8334305a6be31e9bd5676cfec810630634ffddff`.
- Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Проверены ZIP/CRC/пути, v2 подпись, package/version/ABI, runtime definitions,
  известные секрет-маркеры/отсутствие приватных файлов. Все прежние assets/native
  идентичны1.7.5; добавлены только X11 core, keyboard ZIP и notices/licence.
- Новый libXlorie.so: SHA8ad76408710c21b9a61d3db07900e5bb4e3f88efb1670ac0ff3a2166f403567d,
  pinned official APK/source commit fa3a8b430e2896a19f44c99a9cb056254615ae06.
  Системные ELF dependencies, ARM64 и16KiB LOAD alignment просмотрены.
  Собственная нативная пересборка Xorg не выполнялась; источник и pin документированы.
- Оба Android-компонента не экспортированы и работают только в :jobs_x11.
- APK/SHA переданы в Download, обе копии сверены. Исходный1.7.5/code1220
  скопирован в Download/Termux-Jobs-1.7.5-BACKUP, SHA совпал с публичным релизом.
  По указанию разработчика code бэкапа не повышался; для overlay-отката понадобится
  отдельная пересборка1.7.5 с code выше установленного. Данные удалять не требуется.

## Доказательства

-707 JVM-тестов,0 failures/errors/skipped. Новые проверки: допустимые сессии,
  запрет native загрузки сервиса в основном процессе, guard точного process name,
  keyboard path traversal, X11 auth handshake/padding, окно из server setup,
  noncontiguous resource mask, ошибки/размеры и metadata-only keyboard events.
-6 Python-проверок: native declarations/Activity hook, private component wiring,
  ARM64/input SHA, отсутствие X11 в Application и kernel bind guard.
-Compiled JNI descriptor audit сверяет28 методов LorieView,1 MainActivity,
  5 CmdEntryPoint с настоящими native registration tables pinned исходника.
  Проверка обнаружила дополнительный обязательный Activity hook; он добавлен
  до передачи APK. Финальный build и повторный ABI audit успешны.
-Общая JVM suite прошла до финального группирования UI-кнопок в меню/переименования
  и добавления ABI-only declaration; финальная presentation/native linkage
  пересобрана и проверена ABI-аудитом. Повтор всей неизменной suite не заявляется.
-ARM64 release BUILD SUCCESSFUL. Runtime:13226 definitions/31 reviewed unresolved
  types. Существующие Kotlin metadata diagnostics остаются; чистый полный Lint
  не заявляется. git diff --check пройден.

## Логическая инспекция и границы

Прежние TermuxApplication/TermuxService/CLI bridges не менялись. Library загружается
только private X11 экраном/сервисом. Сервис foreground/NOT_STICKY, собственный
runtime, Unix socket/cookie; TCP display выключен, discovery INADDR_ANY blocked
non-listening loopback guard. Нет shell-loader, exported receiver или capture
ввода других приложений; clipboard выключен. Остановка сверяет имя собственной
VM и использует только свой PID; notification stop учитывает bound Activity.
Возврат/rotation освобождают renderer/view, оставляя сервер/протокольный клиент;
повторный экран подключается к нему. При подготовке возможна явная ошибка,
существующая пользовательская среда не подменяется и не очищается.

## Что ещё проверить

На Android графическое ядро в этой задаче ещё не запускалось. JVM/ABI/kernel
fixtures не доказывают отрисовку, IME/Unicode/жесты, resize/возврат из фона,
уведомление, загрузку native на всех поддерживаемых API, отсутствие фактических
внешних listener и влияние process policy. Не заявляется измеренная экономия
ресурсов. Первая ручная проверка — X11-INTEGRATION.md, пункты1–6.

Schema8/пакет/подпись прежние, автоматического сброса/миграции нет. Все новые
файлы возникают в private workspace-x11 только при открытии X11. Не проверялся
полный Android update/rollback cycle. APK не устанавливается автоматически;
сессии разработчика, общий SDK, системные пакеты и Ubuntu не изменялись.
