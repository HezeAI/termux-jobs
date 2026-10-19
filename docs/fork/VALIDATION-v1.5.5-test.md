# Historical private build records — v1.5.5-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.5-test validation

Private APK: 104876276 bytes, com.termux, version1.5.5-test, code1142, minSDK26, arm64-v8a. SHA-256 df4a64272415cbfa01414836211f10d767aaa25a105d8157a547b55bf2d63e5a. Certificate dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644, v2 verified. ZIP CRC, duplicates/path traversal, DEX build flags/runtime definitions, bounded secret-marker/filename scan passed. Existing native libraries, QNN payload and external CLI/server-agent assets unchanged. Final Download copy independently hashed.

Full application suite:598 tests passed; terminal-emulator suite145 passed/cached, terminal-view/termux-shared have no unit tests. Final targeted suite111 passed after final source changes, including reserve adaptation/no current-task truncation, summary archive preservation, behavior flag persistence/UI disable-restore, disabled GPU selection/CPU restoration, permission enforcement, and protection against false write-success claims. Real SDK0.17.0 interface probe:11 passed using actual configuration/messages/providers and fake inference engine on Java21. Source diff check passed. Initial test run had old assertions for the formerly selectable GPU/hidden LiteRT mode and the old estimate label; updated to new intended behavior and verified.

Review: local inference/service cancellation unchanged; no running sessions or external environments changed. Tool schemas and behavior are not authorization. Summary generation has no tool providers/actions, retains originals and treats its output as untrusted historical data. Reserve fallback does not edit saved history/current request. New persistence fields are optional and existing library fields are retained.

Device checks pending: actual Gemma/Qwen answers for Standard/Brief/Tools only, summary correctness across a long chat, UI and keyboard, adapted reserve notice, video retained-frame counts/timestamps, audio recognition and cancellation. LiteRT finish reason lacks reliable token-limit detail. Preview token counts remain estimates. No claim of quality equivalence between instruction modes or exhaustive media validation.

Evidence: .verification/local-ai-v155/{tests-final.log,full-suite-summary.json,tests-targeted.log,targeted-summary.json,build.log,sdk-tests.log,context-budget.txt,1.5.5-test/artifact.json}. No publication performed.


---

## Subsequent development record

# Частная проверка v1.5.5-test — 2026-10-18

Исправление локальной v1.5.4-test/code1166. Публичным выпуском остаётся v1.5.3;
публикации и установки из этой задачи не выполнялись.

## Проблема и исправления

Разработчик подтвердил по скриншотам работающий Gemini API-ответ, реальную модель
и числовую статистику; выбор модели завершался неверным сообщением о входе.

- Исправлен порядок Android LocalSocket: connect, затем setSoTimeout. По AOSP
  constructor откладывает создание fd до connect, а setOption на отсутствующем
  fd бросает `IOException("socket not created")`. Прежний Node-тест не проверял
  Java-клиент Android. Путь ошибки теперь имеет безопасную категорию, без вывода
  произвольного текста исключений/путей/авторизации.
- Наблюдение за streamingState перенесено с временного потребителя контекста
  в постоянный producer в useGeminiStream. useEffect передаёт только состояние
  после React commit; исчезновение ShowMoreLines больше не оставляет последнее
  наблюдение единственным источником состояния. Очередь модели ждёт native idle.
- Открытый диалог статистики обновляется по тому же локальному снимку: меняется
  текст и доступность кнопки; диалог/терминал/поле ввода не пересоздаются.
- Node22 module.register через --import заменил experimental-loader. Предупреждения
  Node глобально не отключались; реальный CLI --version вернул пустой stderr.
- Показан накопленный totalTokenCount API текущего дочернего запуска CLI. Кэш —
  подмножество входа, повторно не прибавляется. Части показаны отдельно; отсутствие
  total в старом снимке не заменяется выдуманной суммой. При смене модели счётчики
  сохраняются; при новом дочернем запуске обнуляются. Это не квота аккаунта и не
  длина контекста, повторно отправляемая история учитывается в расходе API снова.
- Наблюдаемая модель основного API-ответа имеет приоритет над alias/ID конфигурации,
  учитывая возможный backend fallback; новая смена модели сбрасывает старое наблюдение.
- При пустом каталоге штатных опций остаются проверенные native aliases auto/flash/pro.
  Каталог по-прежнему не гарантирует доступность модели/тариф.

Область изменения — Gemini CLI и любые модели в нём. Адаптеры/запуск/счётчики
Codex, Claude, OpenCode и обычные терминалы не используют изменённые классы.
Авторизация и история не сбрасываются, installed SDK/working sessions не менялись.

## Проверки

- 505 JVM tests: 360 app +145 terminal-emulator, failures/errors/skipped0.
  Новые: контракт Android fd до timeout, native total без повторного кэша,
  обновление уже открытого диалога с responding на idle, безопасное сообщение
  ошибки соединения без ложного требования повторного входа.
- check-gemini-live.js: queue/apply/cancel, фактическая модель ответа,
  native total/cached counters, каталог/приватность/фиксированный HTTPS host,
  запрет redirect ключа, fallback пустых опций, шесть точных layouts + syntax,
  переходы из вставленного producer hook после commit, настоящий owned Node
  controller/socket/model/status integration и остановка без ExperimentalWarning.
- check-gemini-runtime.js: graceful/forced/parent-loss stop, владение процессом,
  независимый посторонний процесс, restart/auth handoff, ошибки запуска/аргументов.
- check-gemini-auth.js: режимы входа, private0600, внешняя политика,
  два restart с конфликтующим IPC, очистка после owned stop.
- Реальный pinned CLI0.64.0 --version с module.register, изолированным HOME,
  без ключей/входа/inference: exit0, stdout0.64.0, stderr пустой.
- Локализация584 workspace strings +app/shared settings и аргументы.
- ARM64 release build успешен; есть прежние предупреждения AGP/aapt2 и
  Kotlin metadata; это не утверждение об отсутствии всех Lint-предупреждений.
- Финальный ZIP/package/version/minSdk/ABI/debug/cert/SHA проверены. APK v2 signature
  действительна; приватных файлов и известных маркеров ключей в артефакте нет.
- Bootstrap Gemini, Codex/Claude/OpenCode control-assets, server/backup assets и
  нативные библиотеки побайтно совпадают с проверенной v1.5.4-test.
- Runtime inventory: 12885 defined types; requiredMissing/unreviewedMissingTypes пусты,
  остаются31 прежний рассмотренный optional type. Это не полная проверка linkage/API.
- Исходники установленного CLI читаются для проверки layout; его entry SHA неизменен.

Первоисточник платформенного контракта:
[LocalSocket](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/net/LocalSocket.java),
[LocalSocketImpl](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/net/LocalSocketImpl.java).

## Совместимость и проверка разработчиком

com.termux, arm64-v8a, Android8+; code1167>1166, прежний сертификат. Формат библиотеки8
сохранён, новых разрешений/служб нет. Автоматического удаления или переноса данных нет.
Технические условия обновления поверх соблюдены; полная матрица данных/устройств
не считается проверенной только по подписи и тестам.

Нужно заново запустить Gemini для новых producer hooks и register loader. На устройстве
проверить: список/применение модели (включая после очереди), возврат в ready после
реального ответа, обновление открытого окна статистики, сохранность чата/входа,
отправку и интерактивную клавиатуру. Backend-доступность всех моделей не проверялась.
Автоматические тесты Android-клиента используют строгий fake/socket contract, а не
нативный сокет установленного приложения; Node integration не выдаётся за device test.

Следующая стабильная версия должна иметь code>1167 для обновления этой тестовой сборки.

## Артефакт

- `Termux-Jobs-v1.5.5-test-arm64.apk`
- Размер: 36532156 bytes.
- SHA-256: `ce03505eac02677260bd943a067f3999b4feff31ab758e61f098488b15c03358`
- Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
