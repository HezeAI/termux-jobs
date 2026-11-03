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
