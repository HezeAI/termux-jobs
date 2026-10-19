# Historical private build records — v1.5.6-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.6-test validation

609 application tests passed in the final full suite; terminal-emulator145 tests passed/cached, terminal-view/termux-shared have no unit tests. Added coverage: repeating create/read/delete/list replay stops before third creation, different files/read-only sequences/changed results are allowed, explicit five repetitions and new-turn reset, native progress forwarding, progress retained independently of compacted raw history, persisted expandable UI card excluded from model input, and no shortening notice due solely to JSON field order. Initial run had one old invariant expecting identical system text after tools; changed it to require unchanged rights plus newly recorded progress, then final suite passed.

Real LiteRT SDK0.17.0 interface probe:12 tests passed, using fake inference engine (not NPU hardware validation). Release build and required DEX runtime definitions check passed. Final APK code1143, version1.5.6-test, com.termux, ARM64, minSDK26, 108681787bytes. SHA256 7a414bde5b45f2ce75a57ffb3382dde70dd3a4c81e47b8559667b6116fc7d509. Certificate dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644, v2 verified. ZIP integrity/path/duplicate check, feature flag, bounded secret-marker/file scan, unchanged native/QNN/external controller/server-agent payload checks passed. Download copy independently rehashed. Compressed entry payload grew only3844bytes from1.5.5; larger total APK includes packaging overhead rather than new runtime libraries.

Review: no installed app, live process, service, user environment or remote server was modified. Changes affect local-AI coordinator/UI only. Existing tool authorization/approval precedes loop handling; no extra tool action is inferred by the progress record. Repeated-call reminders do not execute the held action. Cancellation and actual process shutdown are unchanged. Current-user task and full saved transcript are preserved.

Device checks pending: Gemma finishes original workflow or transparently stops the loop; quality impact of the compact progress record; real long tasks/intentional polling; step-card appearance/restoration and gestures. Detection is deliberately bounded and cannot prove absence of every possible model loop. Evidence .verification/local-ai-v156/{tests-final.log,final-suite-summary.json,build-final.log,sdk-tests.log,1.5.6-test/artifact.json}. Source diff check passed. No GitHub publication.


---

## Subsequent development record

# Частная проверка v1.5.6-test — 2026-10-18

Разработчик сообщил по скриншотам: в Codex Pro недельное окно отображалось как
пятичасовое; отсутствующее secondary рисовалось второй пустой строкой. На втором
скриншоте Gemini предположил наличие x-ratelimit-remaining-* HTTP headers.
Это предположение модели не принято за подтверждённый API-контракт.

## Изменения

- UI Codex использует windowDurationMins: 300→5ч,10080→7д, произвольные окна —
  фактическая длительность, неизвестные — нейтральная подпись. Название не выводится
  из primary/secondary, подписки или оставшегося до resetsAt времени.
- Показываются только существующие окна; подпись «осталось» уточняет смысл процента.
  Окно primary может быть недельным и единственным. Отсутствие usage не означает0%.
- Отдельная строка подписки Codex из account/read(refreshToken:false).
  Неизвестное имя не подменяется Pro/Plus. Вход API-ключом отмечен отдельно.
  Записываются только валидированные plan enum и auth type; email/ID/токены не
  сохраняются. Optional account/read failure не отключает остальные данные.
- Worker выбирает codex pool из rateLimitsByLimitId, если он есть, иначе rateLimits.
  Не выполняются model inference, turn/start, thread/resume или изменения прав.
- Новый immutable bridge-v0.2.21.js: v20/v19 не переписываются. Для плана в снимке
  нужен новый запуск Codex; исправленные подписи работают на прежних снимках,
  если в них уже есть windowDurationMins. Работающие процессы не менялись.
- В окне Gemini кнопка «Квоты AI Studio» открывает официальную
  https://aistudio.google.com/rate-limit. В URL нет ключа, project ID или личных данных.
  Остаток квоты проекта не выдумывается из расхода текущего CLI. Автоматическая
  интеграция Cloud Monitoring/OAuth и встраивание аккаунта Google не добавлялись.
- Режимы Gemini стандартный/auto_edit/plan по-прежнему задаются в настройках запуска;
  отдельное управление ими из живой карточки только обсуждалось.

## Источники

- [Codex app-server](https://developers.openai.com/codex/app-server/): account/read,
  account/rateLimits/read, primary/secondary с длительностью и account.planType.
- [Gemini API rate limits](https://ai.google.dev/gemini-api/docs/rate-limits):
  актуальные квоты проекта предлагается смотреть в AI Studio.
- [Cloud Monitoring projects.timeSeries/list](https://cloud.google.com/monitoring/api/ref_v3/rest/v3/projects.timeSeries/list):
  OAuth scopes для чтения метрик, отдельно от обычного Gemini API-ключа.

## Проверки

- 511 JVM tests:366 app +145 terminal-emulator, failures/errors/skipped0.
  Новые6 кейсов: Pro weekly-only primary, обратный порядок/произвольные окна,
  отсутствующий период без угадывания, invalid usage, безопасный plan из снимка,
  API auth/unknown plan. Проверка immutable helper сохраняет прежние v20/v19.
- Node test_codex_status_limits.cjs: собственный fake websocket server + настоящий
  status worker; смена Pro→Plus, одно/два окна, optional account/read failure,
  API auth, некорректный plan, редактирование refresh snapshot; email/ID не выходят
  в файл, записи/turn/inference/permissions RPC отсутствуют.
- 11 прежних Codex permission RPC scenarios passed, включая несовпадение разрешений
  и недопустимое подтверждение. Штатный запуск/политики и завершение не менялись.
- Локализация584 workspace strings +app/shared settings/format arguments; diff check.
- Изолированная ARM64 release build успешна. Известные прежние сообщения AGP/aapt2
  и Kotlin metadata остаются; не заявляется отсутствие всех Lint-предупреждений.
- Финальный APK: ZIP/package/version/ABI/minSdk/debug/cert/SHA проверены. Личные
  auth/workspace/private-key файлы и известные secret markers не найдены.
- Assets Gemini control/preload/loader/bootstrap, Claude/OpenCode, server/backup
  и обе native библиотеки побайтно совпадают с v1.5.5-test. Изменённый Codex control
  в APK совпадает с итоговым исходником.
- Runtime inventory:12886 defined types, required/unreviewed missing отсутствуют;
  остаются31 прежний рассмотренный optional type. Это не доказательство всех
  reflection/linkage/API/device scenarios.

## Совместимость и оставшаяся проверка

com.termux, code1168>1167, Android8+/arm64-v8a, прежний сертификат; формат библиотеки8
и status schema1 сохраняются. Не очищаются данные, история и авторизация; не меняются
системные разрешения, окружения, серверы или установленные SDK. Совпадение подписи
и кода позволяет обновление поверх, но не доказывает все сценарии данных на устройстве.

Проверить в новой Codex-сессии с нужным сохранённым ID: план, один недельный лимит
и дату сброса, обновление карточки, сохранность модели/прав/истории. Реальная подписка
в этой задаче не опрашивалась инструментами: fixture tests отделены от device check.
Для Gemini проверить открытие официальной страницы квот в браузере и выбор проекта.
Полная матрица аккаунтов/браузеров/клавиатур/DPI здесь не проверялась.

Следующая стабильная версия должна иметь code>1168 для обновления этой тестовой сборки.
GitHub-публикация и автоматическая установка не выполнялись.

## Артефакт

- `Termux-Jobs-v1.5.6-test-arm64.apk`
- Размер: 36534724 bytes.
- SHA-256: `201577fba813df790977bf0839392886f14e2577e4d4368d6b6b2367a010d92a`
- Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
