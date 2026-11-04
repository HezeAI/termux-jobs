# Choosing verification scope

This policy applies to development checks, intermediate APKs and public releases.
It supplements [AGENTS.md](../../AGENTS.md), [build notes](../../FORK.md),
[Lint](LINT.md) and [release requirements](RELEASING.md). It does not authorize
installing an APK, changing a user's environment, stopping sessions or publishing.

## Start with the change and its dependencies

Before editing, identify the affected behavior, entry points, callers, shared
helpers, build/runtime dependencies and meaningful risks. Include indirect
dependencies: chat layout can share host geometry with X11; an instruction editor
can share persistence or permission/command execution code. A filename or feature
label alone is not a sufficient scope decision. Record a short verification plan.

Run tests for those paths and meaningful failures. Use the full relevant suite
for broad changes, changes to common mechanisms, or dependencies that cannot be
bounded confidently. Expand the plan when review or a failure exposes another
affected path. A full automated suite does not mean launching every desktop,
installing every distribution or exercising every driver without relevance or
authorization.

Do not launch X11, environment installation, GPU/driver or component integration
checks solely because an instruction editor or chat formatting changed. Include
them when a traced dependency warrants them. Build-time verification of packaged
X11 hashes is different from running an X11 server or a GPU test; do not remove
packaging guards to avoid an unrelated functional test.

Changes to process ownership, privileges, cancellation, termination, recovery or
session isolation retain their meaningful security and lifecycle checks, including
unrelated-process survival and error/cancel races. A small diff does not reduce
these obligations. Known critical regressions or unresolved termination safety
block completion.

## Intermediate checks and static analysis

| Change | Minimum scope to assess and record |
| --- | --- |
| Documentation only | Review wording against implementation, links, diff/whitespace and preservation of existing edits. No APK rebuild or Lint run solely for prose. |
| Local Java/UI/resource behavior | Compile affected code/resources, relevant JVM/UI tests, source/security review, applicable locale/resource checks. Choose compatible targeted Lint where its coverage is understood; otherwise use the existing full modern Lint runner. |
| Python/Node/shell helper | Relevant helper tests and syntax/static checks; include callers, trust boundaries and packaging when changed. No unrelated desktop/driver execution. |
| Manifest, Gradle, shared dependencies, runtime or common lifecycle | Broaden to affected consumers and variants, dependency/runtime review and appropriate modern Lint. Use the full relevant suite when impact is broad or uncertain. |
| Native/desktop/installer/driver behavior | Relevant build, archive/ABI and integration checks, including clean fixtures and failure/cancel cases where applicable. Distinguish fixtures from real device graphics. |

For each intermediate build, state which static checks were selected and why,
what they cover, and what remains unchecked. Successful compilation is not Lint.
The current modern-Lint runner always analyzes all five modules; it is a valid
conservative choice, not a claimed incremental analyzer. An unavailable or omitted
check is recorded as unverified, never as PASS.

Before every public release, run the full compatible modern Lint gate described
in [LINT.md](LINT.md), including all five modules, dependency analysis and report
infrastructure checks. Targeted intermediate analysis and old Lint reports do
not replace this gate. Preserve warnings and toolchain limitations in the report.

## Reusing evidence

Reuse a previous result only after confirming equality of the inputs significant
to that result: relevant production and test/fixture/helper sources, resources,
manifests, configuration and build flags, resolved dependencies, toolchain and
native inputs, ABI/variant, and relevant execution conditions. Include dirty and
new files; commit ID, timestamps, Gradle UP-TO-DATE or an old success log alone
do not prove this equality. Use small input inventories with paths/hashes and
sanitized tool/condition summaries rather than copying an SDK or a rootfs.

For example, a JNI output can be reused if its sources, headers/sysroot, compiler,
linker, flags and input archives match its recorded build and its output hash
still matches. Functional graphics evidence additionally depends on renderer,
driver, server, environment and device conditions. Identical sources cannot prove
that an upstream repository, network, Android permission or live server is still
unchanged. Recheck such state when the current claim depends on it.

When significant inputs cannot be established, rerun the affected check or record
the gap. Never copy a successful result into a new run as though it executed.
Use these separate labels in every validation summary:

- **Проверено заново / Checked anew:** check, tested input identity, result.
- **Переиспользовано / Reused:** original evidence, compared significant inputs
  and conditions, equality proof, and limits of the carried result.
- **Не проверено / Not checked:** missing/omitted check, reason and smallest
  remaining check. An out-of-scope check is not a failure or an implied success.

## Every delivered APK

Verify the actual final APK after its last packaging/signing operation, including
intermediate builds: archive integrity, package ID, versionName/versionCode, ABI,
debug status, signing certificate and signature, SHA-256, and applicable packaged
runtime/DEX/native/asset requirements. Compare package/certificate and increasing
versionCode for an update. Inspect for unintended private data and credentials.
Functional evidence for unchanged components does not waive this final-file gate.

`scripts/verify-apk-runtime.py` inventories DEX definitions/references and reviewed
optional types; it does not verify all APK identity/signing properties or prove
method linkage/device behavior. Use it on the final file where applicable alongside
archive, manifest, ABI and signature inspection. `build-debug.sh` and
`build-local-arm64.sh` do not implement the whole delivery checklist themselves.
Release updater metadata and checksums must refer to that same verified file.

## Existing entry points and minimal separation proposal

The following describes the inspected scripts, not an implemented new scheduler.
Scripts and their integrity gates remain unchanged by this documentation update.

| Entry point | Current unconditional work and implication |
| --- | --- |
| `scripts/check.sh` | Requests the four modules' JVM suites. Useful as a broad entry point; too broad as the automatic response to a narrow change. It does not cover every Python/Node/native/device check. |
| `scripts/verify-java.sh` | Runs `prepare-x11.py`, recreates the Java source stage, then requests the four modules' JVM suites. Even a narrow chat test therefore prepares graphics inputs and runs unrelated JVM cases. |
| `scripts/prepare-x11.py` | Validates cached/downloaded inputs but also regenerates keyboard/style outputs, extracts headers/codecs and recompiles compatibility shims on each call. It verifies optional bridge archives even when their outputs are not bundled. It does not launch a desktop or install guest packages. |
| `scripts/build-local-arm64.sh` | Always prepares X11, stages sources and compiles/links both Termux JNI libraries, then packages and audits DEX runtime. Unchanged native inputs could avoid rebuilding once their full build identity is proven. It also deletes/replaces named packaging outputs; this is not a general cleanup authorization. |
| `scripts/prepare-java-verification.py` | Recreates staged module `src` trees and copies project sources, preserving build caches. It does not copy an SDK or Linux environment, and does not itself execute tests. Do not restage while a build uses that stage. |
| `scripts/verify-modern-lint.sh` | Recreates the analysis source stage and runs the complete five-module modern Lint check. Required before public release; potentially broader than an intermediate change needs. |
| `.github/workflows/fork-ci.yml` | Requests toolchain setup, localization, broad JVM tests and a debug APK for every matching push/PR, including documentation-only changes. Its debug artifact is not a fully verified release APK. |

Minimal follow-up: keep input preparation, source staging, selected test execution,
APK packaging, final artifact audit and full release Lint as explicit steps.
For an already prepared, input-verified stage, invoke existing Gradle test tasks
with class filters (`--tests`) or the relevant Python/Node test file. Preparing
all graphics assets need not be a side effect of selecting a chat test. When
changing automation, add a fail-closed input/output identity check for reuse;
missing/mismatched inputs trigger explicit preparation or an actionable failure.
Keep required packaging/runtime checks active. Do not introduce unconditional
skip switches, fabricated PASS records or a complex new verification framework.

Reuse the existing repository-local SDK/JDK, dependency caches and verified input
archives across runs. Add only missing required SDK components in the permitted
local tool directory. Do not create full SDK/rootfs copies for each check. Keep
compact input hashes, commands, tool versions, check results, artifact identity
and essential failure excerpts. Preserve existing evidence, generated files and
user data; do not run cleanup or stage/output deletion merely to apply this policy.

# Чистая установка как основа для каждого окружения

Разработчик требует, чтобы логика установки ориентировалась на чистое приложение/
bootstrap и новый экземпляр каждого выбранного окружения. Никогда не определяй
обязательные компоненты по тому, что случайно установлено на телефоне разработчика
или в существующей rootfs. Поддерживай явные обязательные пакеты рабочего стола/
runtime для каждой поддерживаемой цели, включая компоненты, отсутствующие в
метапакете дистрибутива. Запрашивай весь обязательный набор в транзакции пакетного
менеджера, даже если часть пакетов уже установлена; определение установленного
состояния и зависимостей оставляй менеджеру. Зависимости необязательных приложений
держи в установке соответствующего приложения.

Проверки должны отвергать отсутствие обязательного исполняемого компонента
рабочего стола, а не считать установку завершённой лишь по наличию метапакета,
панели или оконного менеджера. Покрывай пути отсутствующих компонентов
изолированными тестами. Успешные прогоны на настроенном телефоне считай только
доказательствами отсутствия регрессии; установку на новом устройстве и проверку
GUI описывай отдельно. Не очищай данные пользователя и не меняй установленные
окружения ради имитации чистой установки без разрешения.

# Сквозные проверки нового поведения установки

Новые пути установки окружений/компонентов требуют успешных реальных транзакций
пакетного менеджера в одноразовых, исходно чистых фикстурах проекта, а не только
проверки архивов, симуляции зависимостей, моков или отрицательных тестов. Проверяй
пустые кеши загрузки наряду с установкой из кеша, обязательные исполняемые
компоненты, повторный запуск, отмену и продолжение. Не используй установленную
rootfs телефона или пакеты разработки как доказательство чистой основы.
Явно различай проверку фикстур/пакетов и GUI на новом устройстве; недоступные
проверки фиксируй до выдачи результата.

# Регрессионные проверки интеграции X11

Помощники необязательных компонентов должны принимать допустимые псевдонимы
приватного каталога Android (`/data/data` и `/data/user/0`) по идентичности файловой
системы, отвергая другой каталог с тем же базовым именем. Перед выдачей APK проверяй
реальную точку входа помощника с путями-псевдонимами; тесты кеша репозитория сами
по себе не покрывают различия Android Context.filesDir/PREFIX.

Удаление драйвера проверяй на использовавшемся каталоге драйвера со вспомогательными
файлами и вложенными кешами runtime, а не только на свежем импорте библиотек и
метаданных. Сохраняй проверки защищённого пресета, блокировок рабочего стола для
каждого окружения и удаления без перехода по ссылкам. Проверяй сохранность внешних
целей символьных ссылок, других драйверов и исходных ZIP на телефоне.

Проверки живого изменения масштаба XFCE должны включать восстановление исходных
листовых свойств с XML-типом `empty` (не задано), включая DPI и размер курсора.
Проверяй 75/100/150/100 на реальном изолированном xfconf, а не только на допускающем
всё моке; восстанавливай пустой лист как незаданный скаляр, не очищая рекурсивно
другие свойства.

Изменения поворота должны покрывать конфигурацию, пришедшую до layout, промежуточные
размеры встроенного/полноэкранного вида, задержанные callbacks поверхности, оба
направления поворота и обрезку IME. Применяй логический режим вместе с геометрией
хоста; не пересчитывай новую ориентацию по прежнему размеру поверхности. Отменяй
ожидающие callbacks геометрии при отсоединении/закрытии хоста. Запрошенный пробный
режим только для одного окружения ограничивай этим окружением и в Activity, и в
его выделенном сервисе до проверки на устройстве.

Объединение изменений геометрии не доказывает готовность X11-кадра после поворота.
При изменении полноэкранного представления считай события View attach/detach,
проверяй непрерывность parent/window-token и восстанавливай layout/политику окна
при Back, возврате в вертикальную ориентацию, переключении и закрытии. Неудачный
тест на устройстве считай неудачным, даже если unit-тесты прошли; тайминги нативных
поверхностей/буферов по-прежнему требуют отдельной проверки на устройстве.

Изменения переиспользования поверхности должны отличать callbacks только размера
от замены идентичности Surface/control или формата. Используй публичные API и
консервативно перепривязывай поверхность, если её идентичность неизвестна.
Проверяй перепривязку при замене и после потери поверхности, сохраняй синхронное
освобождение нативных ресурсов при suspend/destroy, а окружения вне пробного
режима оставляй на прежнем пути до принятия ограниченного теста на устройстве.

## Native agent permission chains (explicit security check)

Run `bash scripts/test-native-permission-chain.sh` when checking the connection
between the app's permission buttons and actual Gemini/OpenCode tool effects.
It requires the existing pinned Gemini 0.64.0 bundle, OpenCode 2.0.28 executable,
local Java/Android verification toolchain and gcc. It does not install prerequisites.
The native test Java source lives in `scripts/tests/native`, outside normal app
unit tests. Missing prerequisites or kernel enforcement is a failure, never a skip
reported as success. The explicit runner disables test-task output/cache reuse.

Before any native CLI is launched, the runner builds the test-only seccomp user
notification supervisor and runs enforcement probes. Only listeners created by
that filtered process tree are reachable, on loopback or owned abstract Unix
endpoints. External addresses, a host-side localhost sentinel, UDP/packet sockets,
direct native syscalls and exec inheritance are checked. A configured baseURL,
proxy, preload hook, or a successful `unshare` command is insufficient evidence.
The runner does not change the host firewall, application connections or rootfs.

The chain exercises real Robolectric permission buttons and production approval
adapters with real native tools. HTTP replaces only the Android/proot control
transport. For OpenCode outside-project file actions, separate native directory
consent is an explicit fixture precondition; native directory rejection is tested
separately. Gemini's automatic reads and path validation are recorded separately
from a user refusal. See [stage two audit](SECURITY-AUDIT-STAGE2.md) for evidence,
the failed early isolation probe, limits and the device checklist. This is not a
filesystem sandbox, a complete traffic audit, or proof of phone behavior.


## Урок: 2026-11-02 — Не смешивать нативные индикаторы разных состояний CLI

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-02 — Не смешивать нативные индикаторы разных состояний CLI

- Контекст: исследование штатных индикаторов для кнопки агента, без замены рабочего интерфейса.
- Вывод: в Codex0.166.0 рабочая строка использует точку/перелив текста; Braille из loading_glyph — другой путь. В Gemini CLI0.64.0/0.65.0 цветной GeminiRespondingSpinner ограничен Responding без активного hook, а не любым busy. При адаптации сохранять смысл события или явно обозначать изменённую семантику.
- Применимость: указанные версии и проверенные стандартные компоненты; иные версии/настройки требуют сверки. Штатный символ не доказывает наличие официальной анимации логотипа.
- Подтверждение: самостоятельно прочитаны официальные исходники точных тегов; компоненты Gemini обеих версий побайтно совпали. Ссылки/хеши и ограничения предпросмотра: `.verification/agent-native-previews/README.md`, `sources.json`, `validation.json` в актуальном checkout. Это анализ источников, не проверка интеграции на телефоне.
- Решение пользователя: предпросмотры штатных CLI-индикаторов из `.verification/agent-native-previews` отклонены как оформление текущего обновления. Не считать их согласованным дизайном кнопки; технические сведения выше остаются справочными.


## Урок: 2026-11-02 — Готовый HTML/GIF не доказывает открытие ссылки в чате

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-02 — Готовый HTML/GIF не доказывает открытие ссылки в чате

- Контекст: локальные анимированные макеты и установленный Termux Job’s test29/1464.
- Вывод: в этой версии подчёркнутый абсолютный путь получает URLSpan, но AgentMarkdown пропускает только HTTP(S) и молча отбрасывает локальный путь. Отдельная карточка не равна интерактивному просмотру: выходной HTML Codex имеет тип binary, а GIF показывается через статичный Bitmap. При выдаче таких результатов проверять весь путь открытия, а не только существование файла и его содержимое.
- Применимость: подтверждённое ограничение test29; до соответствующего исправления не обещать работу анимации по локальной ссылке. В test30 маршрут и просмотрщики изменены; модельная проверка описана ниже, приёмка Android WebView на телефоне остаётся отдельной.
- Подтверждение: самостоятельная сверка установленного APK, реальных файлов Ubuntu AI,4 изолированных JVM-кейсов и повторного тапа с выделяемым текстом, плюс проекционный код с0вызовов CLI. `.verification/local-file-link-diagnosis/REPORT.md` в актуальном checkout. Аппаратный тап не инструментировался; на этапе этого отчёта исправление ещё не выполнялось.

- Уточнение test30: локальный URLSpan и карточку нужно связывать с копией профиля при рендере, а не искать среду текущего чата в позднем обработчике. Общий просмотрщик использует независимый файловый транспорт; HTML/GIF получают только один виртуальный документ без app bridge/file access. Загрузку WebView начинать после ненулевого viewport. Проверены изолированные ссылки и карточки всех четырёх агентов, смена профиля, ошибки и освобождение WebView; JavaScript/анимацию на телефоне эта проверка не подтверждает. Подтверждение: самостоятельно выполненные проверки и отчёт `docs/fork/VALIDATION-test30-local-files.md` в актуальном checkout. Исторические выводы test29 выше не означают сохранение прежнего ограничения в новом коде.


## Урок: 2026-11-02 — Возврат с верхней опоры требует выхода к краю

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-02 — Возврат с верхней опоры требует выхода к краю

- Контекст: автономный макет перемещающегося робота Codex; это ещё не реализация в приложении.
- Вывод: при прерывании уборки нельзя прыгать напрямую с внутренней точки верхней плашки на нижнюю. Параболическая траектория пересекает верхнюю плашку. Сначала пройти по её краю к безопасному спуску, затем спускаться; новый запрос при этом активируется сразу.
- Применимость: маршруты по DOM/View-опорам и последующее внедрение при сохранении такой геометрии. Не считать модельную проверку доказательством всех коллизий на телефоне.
- Подтверждение: самостоятельно выполнены `collision guard rejects diagonal descent through a tab but permits edge-first return` и прерывания walking/wiping в `.verification/codex-roaming-robot/check.cjs`; отчёт README и checks.json в актуальном checkout.


## Урок: 2026-11-03 — частичные снимки Claude и открытие формы после чтения

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## 2026-11-03 — частичные снимки Claude и открытие формы после чтения

- Контекст: история Claude исчезала после обновления, а «Создание инструкций проекта» иногда не открывалась.
- Вывод: ограниченный снимок нельзя выдавать общему AgentChatUi как полный knownMessages. Нужны partialHistory и курсоры для скрытых строк, включая части одного сообщения. Полное тело старого ответа восстанавливается из нативного JSONL, а не из ограниченного live-кеша. Для Claude ключ записи — native uuid; provider message.id может повторяться. Общие прокрутка/кеш/просмотр остаются теми же, что у Codex.
- Условия: основной путь Claude2.1.296 с parentUuid и same-file logicalParentUuid. Не восстанавливать неизвестные межфайловые связи выдумыванием. Нативные файлы не изменять.
- Подтверждение: самостоятельно проверены большие JSONL-фикстуры, файл более64МиБ, полный Unicode-ответ, реальный изолированный CLI с локальным провайдером и сравнение52нативных записей/14сообщений без пропусков. Общие Claude/Codex тесты удерживают anchor/offset при prepend/stream/reopen. См. docs/fork/VALIDATION-test39-history-instructions.md в активном checkout.
- Форма: подтверждённый ранний тап терялся из-за busy-return. Очередь одного запроса привязывается к агенту/среде/проекту, а ссылка на открытый дочерний диалог исключает повторную форму. В тестах Android-кнопки требуют выполнения очереди главного потока; это не основание добавлять задержки в продукт. Мигание и открытие на телефоне всё ещё требуют отдельной приёмки.


## Урок: 2026-11-03 — Существующий проектный файл отличается от формы создания

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Существующий проектный файл отличается от формы создания

- Контекст: в последнем видео Codex уже показывал существующий AGENTS.md, а Claude открывал форму для отсутствующего CLAUDE.md. Пользователь подтвердил это объяснение.
- Вывод: подпись действия должна соответствовать наличию базового файла, включая просмотр override/local. Для существующего файла показывать «Редактировать», фокусировать редактор без добавления строки статуса. Для отсутствующего — открывать форму создания. Не диагностировать закрытие несуществовавшего окна как установленную причину.
- Применимость: общий AgentInstructionsUi и ответы agent-instructions.py. Черновик и редакция записи привязаны к исходному контексту. Голосовой ввод по прямому решению пользователя удалён; не восстанавливать его из исторических отчётов.
- Подтверждение: самостоятельно сопоставлены последнее видео, ветвление Java и read-only вызов реального Python backend: корневой AGENTS.md существует, CLAUDE.md отсутствует, AGENTS.md не изменён. Доказательства в .verification/instruction-window-trace/ актуального checkout; это анализ наблюдаемого поведения, а не проверка нового APK на телефоне.


## Урок: 2026-11-03 — Инкрементальная упаковка может сохранять неиспользуемые ZIP-записи

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Инкрементальная упаковка может сохранять неиспользуемые ZIP-записи

- Контекст: добавление обязательных уведомлений к кандидату стабильного APK.
- Вывод: после изменения DEX размер APK вырос на3МБ при почти неизменном списке файлов. Измерение смещений обнаружило3 096 057байт между активными записями с сигнатурой старого локального ZIP-заголовка. Список ZIP/DEX сам по себе не доказывает отсутствие таких остатков. Сохранив только принадлежащие задаче packageRelease state/output, повторили упаковку в свежий output. Финальный APK40 452 595байт, неизменные payload-хеши проверены заново.
- Применимость: AGP4.2.2 incremental packaging в этой среде. Это не доказательство исполнения старого DEX или утечки секрета. Не чистить весь SDK, кеши или пользовательские данные; сохранять исходные доказательства и проверять точные финальные байты.
- Подтверждение: .verification/release-v2.1.2/incremental-zip-gaps.json, before-fresh-packaging и fresh-packaging.log; итоговый аудит и docs/fork/VALIDATION-v2.1.2.md в активном checkout.


## Урок: 2026-11-03 — Снимок Gemini не задаёт время старого действия

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Снимок Gemini не задаёт время старого действия

- Контекст: часы HH:mm только в заголовках групп хронологии 2.1.3-test3.
- Вывод: первый observe может содержать пустую историю до её восстановления. Не отмечать все новые для следующего снимка tool IDs текущим временем. Для наблюдаемого времени нужен прежний готовый baseline и активный ответ; сохранённая отметка не меняется при обновлении или перезапуске. Неизвестные старые отметки скрываются.
- Применимость: существующая проекция React history Gemini и приватное хранилище timing metadata; это не доказательство наличия нативного времени у всех CLI.
- Подтверждение: scripts/tests/test_action_event_times.cjs, четыре агента, отдельный idle-loaded baseline; CodexActionTimelineTest проверяет timezone/обновление/узкие заголовки. См. docs/fork/VALIDATION-2.1.3-test3.md в активном checkout; проверка телефона остаётся отдельной.


## Урок: 2026-11-03 — Подключение и рабочий контекст проверять раздельно

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Подключение и рабочий контекст проверять раздельно

- Контекст: продолжение настройки второго телефона по прямому поручению. Установлен официальный Codex0.166.0 рядом с0.163.0; npm integrity проверена, основной бинарник совпал с работающим на первом телефоне. Bootstrap предпочитает найденный в PATH codex, поэтому штатный /usr/local/bin/codex выбирается раньше приватного fallback0.163.0 без замены старого каталога.
- Два независимых запускателя Hysteria/SSH используют прежний provider=openai. Hysteria с пользовательским VPN получила timeout/no recent network activity; без VPN заработала. Оба маршрута без VPN прошли по два настоящих последовательных ответа. Это ограниченное наблюдение, не универсальный запрет совмещать VPN и Hysteria.
- Большая история не завершила первые SSH-пробы за180с. При успешной прямой VPN-пробе Codex автоматически выполнил compaction; первоначальное сообщение «не пришлось сокращать» оказалось неточным и исправлено. Затем штатный compact/start через Hysteria завершился, ответ в том же чате занял6.75с. Новый процесс через SSH продолжил тот же чат и завершил ответ за20.57с вместе с запуском. Не отделено влияние маршрута, версии и контекста; изображения не объявлены единственной доказанной причиной.
- Compaction не удаляет старую переписку в проверенном legacy JSONL0.166.0:32761712байт прежнего префикса побайтно сохранились; добавлена запись с replacement history. Новый размер JSONL не равен размеру запроса модели. Возраст чата и размер локальных ZIP/STL-ссылок не определяют объём модельного контекста; результаты просмотра изображений учитывать отдельно.
- Реальные тесты аккаунта запускать только по разрешению пользователя, с коротким диагностическим вводом, без проектных инструментов. Проверку stdio app-server не выдавать за приёмку UI. Сохранённый старый бинарник сам по себе не гарантирует обратную совместимость базы после обновления.
- Доказательства: `.verification/wife-route-20261103/phone-fix/REPORT.txt`, compaction-timeline.json, final-history-preservation.json, model-*-two-turns.jsonl, model-ssh-postcompact.jsonl. Все десять тестовых туннелей завершились; общие службы сервера сохранили идентичность. Ключи аккаунта диагностическим кодом не читались.


## Урок: 2026-11-03 — Проверка сети CLI требует ограничения системных вызовов

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Проверка сети CLI требует ограничения системных вызовов

- Контекст: продолжение второго этапа аудита. Ранняя фикстура OpenCode неверно задала legacy provider/settings и получила внешний provider HTTP 403. Тело, точный URL/IP и полный file-open trace не сохранены; нельзя утверждать минимальный состав передачи. Разбор не читал настоящие credentials.
- Подтверждение этой среды: unshare --user --map-root-user --net завершился кодом 0, но namespace остался прежним и child достиг sentinel родителя. Не распространять это наблюдение на все системы.
- Решение для тестов: explicit runner scripts/test-native-permission-chain.sh применяет seccomp notification/ADDFD к дереву CLI. Он разрешает только собственные зарегистрированные loopback/abstract endpoints; прямые syscall, IPv6, UDP/packet, SCTP-like stream, Fast Open и exec-потомки проверены отрицательными пробами. Host firewall и настройки приложения не меняются. Это не файловая изоляция и не независимый аудит против враждебного процесса того же UID.
- Подтверждение: 25 native-сценариев, 18 реальных нажатий UI в Robolectric; Source/outputs-up-to-date для такого теста недостаточно. Отдельный init запрещает reuse/cache explicit native task. В guard наблюдались и блокировались дополнительные localhost-пробы OpenCode 1234/11434/8000. См. docs/fork/SECURITY-AUDIT-STAGE2.md и .verification/security-stage2-followup активного checkout.


## Проектные требования проверок — перенос из AGENTS.md 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

# Проверки соразмерно изменениям

До редактирования определи затронутые функции, вызывающий код, общие помощники,
зависимости и значимые риски, включая косвенные последствия. Запиши краткий план
проверок. Выполняй относящиеся к изменению проверки поведения и ошибок; полный
применимый набор нужен при широких изменениях, изменении общих механизмов или
зависимостей, границы которых нельзя уверенно определить. Не запускай X11,
установку окружений, интеграционные проверки драйверов или компонентов только
потому, что изменён редактор инструкций или оформление чата. До исключения этих
проверок проследи общие пути layout, хранения, разрешений и выполнения.

Приведённые ниже требования к чистой установке и X11-регрессиям применяются,
когда затронуто соответствующее поведение или его зависимости. Проверка входных
данных/хешей APK отличается от запуска графики или установки гостевых систем.
Сохраняй все применимые проверки принадлежности процессов, безопасности, отмены
и восстановления; не сокращай их из-за малого diff. Нерешённая безопасность
остановки процессов по-прежнему блокирует завершение.

Переиспользуй прежние результаты только при подтверждённом совпадении значимых
производственных и тестовых исходников, конфигурации, фактически выбранных версий зависимостей,
инструментария/входных данных сборки и относящихся к проверке условий выполнения.
Учитывай изменённые и новые файлы. Старого успешного лога, ID коммита или сообщения
UP-TO-DATE недостаточно. Отдельно отмечай **Проверено заново / Checked anew**,
**Переиспользовано / Reused** (с доказательствами совпадения) и
**Не проверено / Not checked** (с причиной и оставшейся проверкой).

Каждый выдаваемый APK, включая промежуточные сборки, должен пройти проверки
именно финального файла: целостность ZIP, package ID, versionName/versionCode,
ABI, статус debug, подпись/сертификат, SHA-256 и применимые проверки включённых
runtime/DEX/native/assets. Переиспользование функциональных результатов не
отменяет этот барьер. Для обновлений сравнивай package/подписанта и проверяй рост
versionCode; ищи случайно включённые приватные данные.

Для промежуточных сборок явно выбирай и обосновывай статические проверки:
компиляцию, относящиеся к изменению проверки синтаксиса/локализаций/ресурсов и
совместимый целевой Lint при понятном покрытии либо полный современный Lint при
широком или неясном влиянии. Отсутствующая или пропущенная проверка не является
PASS. Полный современный Lint всех пяти модулей остаётся обязательным перед
публичным релизом. Изменения только документации требуют проверки текста, ссылок
и diff, а не пересборки APK или запуска Lint исключительно ради текста.

До выбора точек входа изучи существующие скрипты. Если точка входа безусловно
делает постороннюю работу, предложи минимальное разделение подготовки, выбранных
тестов, упаковки, аудита финального файла и релизного Lint. Не добавляй общие
skip-флаги, фиктивные PASS или сложную систему проверок. Переиспользуй существующие
локальные SDK, инструменты и кеши; не копируй целиком SDK или окружения для каждого
прогона. Храни компактные идентификаторы входных данных и результаты. Без
разрешения не удаляй существующие доказательства, созданные файлы или данные
пользователя. См. [область проверок и аудит скриптов](docs/fork/TESTING.md),
[Lint](docs/fork/LINT.md) и [выпуск релизов](docs/fork/RELEASING.md).

# Доказательства завершения рефакторинга

Перед объявлением рефакторинга завершённым оцени три независимые области:

- Относящиеся к изменению тесты: проверь изменённое поведение и значимые ошибки.
  Один успешный существующий набор не доказывает покрытие нового пути.
- Ревью кода: проверь регрессии, обработку секретов, границы разрешений и доверия,
  влияние на посторонние терминальные сессии и процессы.
- Логическая проверка: проследи применимые пути вызовов, переходы состояний, запуск,
  переключение, закрытие, отмену, обработку ошибок и восстановление.

Не выдумывай числовой процент уверенности. Если запрошена оценка, можно использовать
40/30/30 для тестирования/ревью/логической проверки как качественные приоритеты,
а не калиброванную вероятность или замену доказательствам.

Известная критическая регрессия, раскрытие секрета или нерешённая безопасность
остановки затронутых процессов блокирует завершение независимо от числа других
успешных проверок. Если доказательств недостаточно, сообщи, что проверено, каковы
основные пробелы и минимальные следующие проверки. Различай проверку исходников/
сборки и устройства: клавиатуру, жесты и другое зависящее от устройства поведение
нельзя объявлять проверенными на телефоне только на основании мок-тестов UI.
Выбирай проверки соразмерно изменению; не добавляй избыточные тесты или широкий
посторонний рефакторинг ради улучшения оценки. Разрешения пользователя на установку
APK или публикацию остаются отдельными от технических проверок.


## Реорганизация рабочего контекста — 2026-11-04

По поручению разработчика AGENTS.md сокращён до обязательных ограничений, путей
и условного навигатора. Единственная память остаётся `/root/termux-fork/LEARNING.md`;
она обновлена на месте вне checkout и не добавляется вторым файлом в Git.
58 блоков исторических результатов и уроков перенесены без изменения содержания
в существующие тематические документы; подробные требования проверок сохранены
выше. Текущий план аудита отделён от истории, CONTINUE описывает актуальную цель.
Сокращение не означает измеренную экономию токенов или времени.

| Файл | До, байт | После, байт |
| --- | ---: | ---: |
| AGENTS.md | 19,535 | 15,214 |
| LEARNING.md | 92,904 | 25,144 |
| SECURITY-AUDIT-STAGE2.md | 187,778 | 17,013 |
| CONTINUE.md | 1,564 | 5,119 |

Проверки помощника: побайтовое включение всех 58 перенесённых блоков, 83 ссылки
текущих входных документов, исправление двух входящих якорных ссылок, git diff --check.
Доказательства: `.verification/context-organization/{before.json,relocations.json,check.json}`.
Сохранены обязательное чтение памяти, ограничения среды/процессов/секретов,
авторизация, финальный APK/Lint, X11/чистая установка, updater/совместимость,
CHANGELOG/RU/EN/авторство и условное чтение RELEASING. Пороговые правила размеров
общего шаблона не отменены. Память не заменяет актуальный статус аудита.
