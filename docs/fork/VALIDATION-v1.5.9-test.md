# Historical private build records — v1.5.9-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# Validation — v1.5.9-test

Evidence directory: `.verification/local-ai-v159-optimization/`.

Baseline was recorded from1.5.8 source/compiled classes before production changes. Synthetic prompt measurements cover JSON, Qwen native and LiteRT native, each with Standard/Brief/Tools only, identical full-access settings and no user data. LiteRT numbers include native schemas; memory/progress/media/history addenda are omitted identically on both sides. Counts use LocalAiContext.estimate, not exact model tokens. No real-model speed claim follows from this comparison.

Schema fingerprints cover24 baseline combinations of chat/read/write, project-only, confirmations, and additional Termux commands. Only description strings are ignored: all names/order, fields, argument types, required lists, bounds, enums and additionalProperties remain part of the comparison. Application authorization/parser/runner paths are unchanged.

Broad run:627 app cases (626 passed; one wording assertion expected the previously explicit `NEVER deletes`). That wording was retained in the shared write description. Final99 relevant cases passed (including a one-off size-measurement case);145 emulator cases passed/cached in the broad run. Final13 interface tests with real SDK0.17.0 and fake inference passed. No real CPU/NPU model was invoked. The one-off size helper is retained under the evidence directory rather than as an ordinary repository test with an absolute report path. Final production source matches the build stage. See full-suite-summary.json, targeted-summary.json, targeted-final.log, sdk-final.log and build-final.log. Device-specific checks in RELEASE-v1.5.9-test.md remain pending. Passing JVM tests does not establish model understanding, rendering/keyboard/gestures or real throughput.

Synthetic estimated service input (before -> after), full access, project-only OFF, confirmations OFF, extra Termux ON, example project:

| Protocol | Standard | Brief | Tools only |
|---|---:|---:|---:|
| JSON | 2531 -> 2245 | 2165 -> 2000 | 1929 -> 1746 |
| Qwen native | 3383 -> 2542 | 2420 -> 2157 | 2184 -> 1902 |
| LiteRT native | 2533 -> 2223 | 2175 -> 2155 | 1965 -> 1927 |

Brief/Tools only LiteRT were already compact, so their savings are small. Complete schemas were not pruned to manufacture a lower count; new deliverable/language/failure guidance is included in Standard/Brief. These are not exact model tokenizer counts or real request timings. See prompt-size-comparison.json.

Final APK: 108689671bytes, SHA-256 `216ee9d139afd16adbff745fe7fcbdfbb841bf4e879f2aa9d07c065903623913`. ZIP CRC/path/duplicate checks pass; com.termux/1146/1.5.9-test, minSDK26, ARM64 only, private Local AI enabled. Existing certificate matches and v2 signature verifies. Limited secret filename/marker scan passes; existing native payloads and controller/server-agent assets match baseline. No model is bundled or modified. Download copy is independently hashed.


---

## Subsequent development record

# Частная проверка v1.5.9-test — 2026-10-19

## Запрос разрешения Gemini

Разработчик подтвердил устранение мерцания Gemini в v1.5.8-test. Для нового запроса
на скриншоте добавлена карточка над нижней частью терминала:
«Разрешить разово», «На эту сессию» (только если предложено CLI), «Нет».
Есть просмотр полного текста команды/диффа. Описание объясняет срок разрешения:
разово — текущее действие; сессионное правило может охватывать похожие действия
или командные префиксы и сбрасывается после завершения Gemini. Права пресета
не меняются, постоянное разрешение не предлагается.

После совпавшего native ack карточка сворачивается: «Разрешено один раз»,
«Разрешено на эту сессию без повторного запроса» или «Отклонено».
Ошибка или несовпавший ack выводят «Ответ не подтверждён», а не ложный успех.

Используется actual confirm из useToolActions и native ToolConfirmationMessage
после React commit. Java не исполняет shell и не вводит цифры/Enter.
Непредсказуемый nonce связан с конкретным native запросом. Проверяются Config,
проект, неизменность текста, допустимый вариант и актуальное доверие для session.
Повторный ответ, старый nonce, замена запроса, ответ клавиатурой CLI или другой
запуск не разрешают повторное выполнение через карточку. Native callback awaited;
ответ не считается выданным до подтверждения доставки native callback.

Поддержаны обычные exec/edit/info. Build/untrusted/deceptive-URL предупреждения,
ask-user, exit-plan, MCP, sandbox expansion и oversized/нестандартные запросы
остаются в CLI со штатными предупреждениями и вариантами. Нативный запрос не
удаляется из TUI; интерактивное управление остаётся доступным.

Текст передаётся через private0600 Unix socket/IPC и остаётся в памяти.
В status JSON сохраняется только nonce; команда не добавляется карточкой
в workspace/presets/истории приложения. История/диагностика native CLI имеет
прежнее поведение. Никаких inference-запросов для UI не добавлено.
Parent и Android проверяют форму ответа и соответствие nonce/outcome.

Карточка — overlay внутри новой FrameLayout-обёртки TerminalView с прежними
размерами и полями. Перестройка карточки не меняет геометрию PTY и не пересоздаёт
страницу. На смене сессии/экрана или уходе приложения карточка скрывается;
поздний ответ другой вкладки не отображается в текущем чате. Повторный тап блокируется.

## OpenCode

Обработка DEC2026 кадров и primary transcript anchoring теперь включены
для Gemini И OpenCode. Другие агенты/SSH/shell остаются на старом пути.
Сохраняется граница256KiB, аварийный timeout200ms без продления repeat begin;
обычный вывод и завершённые кадры не ждут таймер. Resize/reset/exit снимают
ожидание. Никаких historical1200ms resize captures не возвращено.

Изолированный native OpenCode mini с новым frame parser, private HOME/XDG,
синтетическим чатом и proxy, запрещающим inference:53 begin/end,24567bytes,
шесть resize, тестовая история остаётся, owned stop прошёл. Начальная история
присутствовала и в raw output, и в emulator. Действующие пользовательские чаты,
ключи и серверы не использовались. Alternate/mouse scroll и updateSize не
переписываются. Подтверждение протокола не является проверкой Android-жестов.

## Автоматические проверки

- 535 JVM tests:381 app +154 terminal-emulator; failures/errors/skipped0.
  Новые4 UI cases: once/session/deny collapse, scope explanation, overlay без смены
  высоты terminal, повторный тап, поздний ack другой сессии, malformed/permanent
  choices, неправильный ack. Новые2 scope cases: флаги только Gemini/OpenCode и
  снятие флагов при другом профиле. Сохранены проверки атомарного emulator update,
  timeout/лимита/UTF-8/обычных ANSI, scroll anchoring и других сессий.
- test_gemini_permissions.cjs: once/session/deny, single-use/native consume,
  stale/replacement/config/project/body/trust guards, concurrent ответы,
  warnings/unsupported fallback, callback failure, scalar privacy, reply allowlist;
  исполняются точные новые UI hooks трёх pinned layouts, SDK unchanged.
- check-gemini-live.js: настоящий контроллер с синтетическим child/private socket:
  получение разрешения, совпавший ack, stale/permanent rejection, body absent
  from status file, native model/rights/status and owned stop.
- check-gemini-runtime.js, check-gemini-auth.js, test_gemini_live_modes.cjs,
  test_gemini_frame_render.cjs пройдены. Вход/рестарты/parent loss/owned shutdown
  и защита чужих процессов сохранены. Реальные ключи не использовались.
- Реальный Gemini0.64.0 --version с loader/private HOME:exit0,stderr пуст.
- Локализация584 строк и git diff --check пройдены.
- Изолированная ARM64 release build успешна. Прежние AGP/aapt2/deprecated API
  предупреждения остаются; отсутствие всех Lint-предупреждений не заявляется.
- ZIP/package/version/minSdk/ABI/nondebug/signature/SHA проверены. Private
  auth/workspace/key файлы и известные secret markers в APK отсутствуют.
- Native библиотеки, Codex/Claude/OpenCode/server/backup/bootstrap assets
  побайтно совпадают с v1.5.8-test. Gemini controller/preload/loader совпадают
  с итоговыми исходниками. Runtime12911 defined classes, required/unreviewed
  missing отсутствуют,31 прежний optional type; не полная reflection/linkage проверка.

## Совместимость и проверка на устройстве

com.termux,versionCode1171>1170,Android8+/arm64-v8a,прежний сертификат.
Формат библиотеки8/status schema1 сохраняются. Код не требует сброса пресетов,
истории, входа или окружений; все варианты данных на устройствах не проверены.
Нужен новый запуск Gemini для новых hooks. OpenCode проверить в новом запуске
с прежней историей. Следующий стабильный выпуск должен иметь code>1171.
GitHub-публикация и установка APK не выполнялись.

Проверка разработчиком: карточка запроса Shell, просмотр полного текста,
разовый/session/отказ и точный срок в результате; повторные команды; ответ
из native CLI; смена вкладки с ожидающим запросом; клавиатура, узкий экран/DPI.
Для OpenCode — листать историю при ответе, сменить вкладку и открыть/закрыть
клавиатуру. Наш transcript fix ограничен новым выводом; resize использует прежний
путь, поэтому отсутствие всех прыжков на телефоне не заявляется. Изолированные
и Robolectric tests не доказывают реальную клавиатуру/жесты/генерацию Android.

## Артефакт

- `Termux-Jobs-v1.5.9-test-arm64.apk` — 36545960 bytes.
- SHA-256: `311a9379037d4ecd41ff9dd12a44b2c3b884eb7a7341634f35b48b5ca114f2d1`
- Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
