# Historical private build records — v1.5.7-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.7-test validation

Full application suite615 tests passed. Final targeted suite111 passed after the final functional changes: joint3-frame pass, fallback after dropped joint frames/context failure, all3 sequential passes, restoration after exceptions/cancellation, rejection of missing-frame/native-tool results, pass-through of image/unsupported media, preservation of original descriptions vs compact preview, protected video observations with adaptive reserve, and ordinary local-AI/context/UI behavior. terminal-emulator145 tests passed/cached; terminal-view/termux-shared have no unit tests. The final wording-only video tool description was also covered by the real-SDK probe.

Real SDK0.17.0 interface probe13 tests passed using actual SDK configuration/providers and fake inference: no-tool conversation followed by restored normal tool conversation, automatic SDK execution disabled. These checks do not run NPU/GPU vision. Final release build and required DEX runtime definitions check passed. Source diff check passed.

APK108690633bytes, com.termux, version1.5.7-test, code1144, arm64-v8a, minSDK26. SHA256004bf423b58ac1ed5547300e4837764d925d2de51bb8d959b584b5805ef940c7. Existing certificate dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644, v2 verified. ZIP CRC/path/duplicate scan, private gate, limited secret-marker/filename scan, unchanged native/QNN/external-controller/server-agent payloads verified. Download copy independently hashed. No installed app, user service, environment or remote server was altered.

Review: tool authorization/file access stays in existing LocalAiTools before sub-analysis. Temporary schemas and cache scope are restored, internal text cannot dispatch actions, failed/dropped joint observations are discarded, incomplete single-frame inference is not marked successful, cached temporary media is cleaned by the existing tool owner. Cancellation stops the existing owned local-model workflow; process shutdown mechanism is unchanged. Result descriptions are untrusted data, not permission. Saved history/rights are not replaced by sub-analysis.

Pending device validation: actual multi-frame SDK/model consumption and visual accuracy, sequential fallback behavior on real context limits, progress/keyboard/gestures, ordinary post-video tools, and Stop/Start after cancellation/backend failure. Current llama.cpp has no visual projector path. No expanded NPU context or new model/runtime support claimed. Evidence .verification/local-ai-v157/{tests.log,full-suite-summary.json,tests-final.log,targeted-summary.json,build-final.log,sdk-tests.log,audit-final.json,1.5.7-test/artifact.json}. Not published.


---

## Subsequent development record

# Частная проверка v1.5.7-test — 2026-10-18

## Изменения

- Строка «Режим прав: … ▾» в окне Gemini открывает три штатных варианта
  столбиком: подтверждать действия, разрешать редактирование, планирование.
  После выбора возвращается исходное окно с фактически применённым режимом.
- Используется native callback useApprovalModeIndicator: политики Config,
  индикатор и штатный onApprovalModeChange. Ввод команд в терминал не эмулируется.
  YOLO не предлагается; ограничения доверия и доступности plan сохраняются.
- Во время ответа или ожидания подтверждения изменение ждёт idle. Ручная смена
  режима, смена проекта или контроллера отменяет устаревший запрос. Приложение
  не одобряет уже ожидающие инструменты автоматически.
- Только совпавшее подтверждение запуска/request ID/режима обновляет сессию
  и её сохранённый пресет. Несохранённая сессия не становится пресетом.
  Параллельно отредактированные настройки не перезаписываются.
- Проверка подтверждения продолжается при переходе в другую вкладку через чтение
  локального JSON. Нового сервиса или запросов к модели для этого нет.
  При уничтожении UI неподтверждённый результат не записывается как выполненный.
- Для Gemini включено сохранение позиции основного transcript при новом выводе:
  scrollCounter сдвигает позицию вместе с текстом, предел истории ограничивается.
  В конце чата сохраняется обычное следование за выводом.
- Флаг принадлежит конкретной TerminalSession, по умолчанию выключен. Остальные
  сессии, alternate screen, mouse routing и алгоритм выделения сохраняют прежние
  пути. Изменения размера и клавиатуры используют прежний updateSize.
- OpenCode оставлен без этой правки; его прокрутка отложена на следующее обновление.

## Проверки

- 517 JVM tests: 372 app + 145 terminal-emulator, failures/errors/skipped 0.
  Новые проверки: сохранение только подтверждённого режима нужного запуска,
  сохранённый/несохранённый профиль, другая выбранная вкладка, неверный ack,
  кликабельная строка и возврат в исходное окно; позиция настоящего эмулятора
  при новых строках, конец истории, отдельные сессии и alternate screen.
- check-gemini-live.js: приватный протокол, native модель/права, валидация запроса,
  scalar status без содержимого чата/ключа, изоляция каталога и собственных процессов.
- test_gemini_live_modes.cjs: очередь при ответе/подтверждении, idle apply,
  ручное переопределение, замена очереди, отказ native политики, plan capability,
  смена проекта, cleanup; три hash-pinned UI hook исполняются с fake React.
- check-gemini-runtime.js и check-gemini-auth.js: запуск, отказ неверным данным,
  graceful/forced owned stop, parent-loss, restart/auth handoff, сохранение Google
  auth mode, чужие процессы не затрагиваются. Данные авторизации синтетические.
- Проверка локализации и git diff --check пройдены.

## Совместимость и проверка на устройстве

Пакет com.termux, versionCode1169 выше1168, прежняя подпись, Android8+/arm64-v8a.
Формат библиотеки8 и status schema1 сохраняются. Очистка данных, истории и входа
не требуется кодом; полная матрица миграции на устройствах не проверена.
Новые hooks требуют нового запуска Gemini после обновления APK.

Проверить на устройстве: открыть строку прав, выбрать доступный режим, увидеть
применение и тот же выбор в настройках пресета; повторить при ответе и переходе
на другую вкладку. Пролистать Gemini вверх во время генерации, затем вернуться
в конец. Реальное управление CLI, сенсорные жесты, клавиатура и DPI не доказаны
Robolectric/fake-server тестами. При выходе из plan штатный CLI может добавить
служебное уведомление в историю; чат не пересоздаётся. Права Gemini не являются
root Android или системной песочницей.

Работающие пользовательские сессии, Ubuntu и общий SDK не изменялись.
GitHub-публикация и установка APK не выполнялись. Следующий стабильный выпуск
должен иметь versionCode >1169 для обновления этой тестовой сборки.

## Артефакт

Изолированная ARM64 release build успешна. Сохраняются прежние предупреждения
AGP/aapt2 и deprecated API; отсутствие всех Lint-предупреждений не заявляется.
Проверены ZIP, пакет/версия/minSdk/ABI, отсутствие debug-флага, сертификат и SHA-256.
Изменённые Gemini assets совпадают с исходниками; Codex/Claude/OpenCode, server/backup,
bootstrap assets и native библиотеки побайтно совпадают с v1.5.6-test.
Проверка личных auth/workspace файлов и известных secret markers пройдена.
Runtime inventory:12896 defined classes, required/unreviewed missing отсутствуют,
остаются31 прежний рассмотренный optional type. Это не полная проверка reflection.

- `Termux-Jobs-v1.5.7-test-arm64.apk` — 36539360 bytes.
- SHA-256: `4401ebbc8bd73bd53095a0217bd6034011fd540e69f8efeb6ae91ec659dd4cfc`
- Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
