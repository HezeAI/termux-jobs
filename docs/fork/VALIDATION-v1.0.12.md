# Проверочная v1.0.12 — OpenCode resize replay

com.termux, versionName1.0.12/versionCode1049, ARM64/Android7+.
APK 105064357 bytes; SHA256 `1b1bf9b98428d297c425893ff65764e93d486de8100ba65f25895ff5882666cf`.
Certificate `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` прежний. ZIP CRC, signaturev2, package/version,
ABI/nondebuggable/stored4-byte alignment, повторный SHA Download проверены.
Прежние lib/ .so неизменны. Мини-bundle содержит только проверенный ELF ARM64 и
MIT LICENSE; содержимое archive/binary SHA совпадает с manifest. Личных auth,SSH,
keystore,data files нет; сборка frontend только из upstream commit и локального patch.

## Проверки

283 JUnit/Robolectric tests, failures/errors/skipped0; assembleRelease/lintVital;
531 localization keys/format arguments; JS/Bash syntax и git diff --check.
Новый Java test проверяет кеширование immutable archive при повторном prepare.
31 upstream runtime/queue/boot tests проходят. CLI tsgo-b typecheck проходит.
Root upstream lint (oxlint,ast-grep,sdk-docs) проходит с официальным Node22.22.1
из ранее скачанного project cache. Initial lint не проходил на текущем Node без
TypeScript support; инструменты среды не заменялись. Full monorepo typecheck
не выдаётся за выполненный — проверен CLI и его build dependencies.

Native production bridge + separate frontend в private synthetic chat: resume
искусственной истории, busy/refusal и admission-race logic, переоткрытие модели с
тем же native ID, следующий prompt выставляет новую модель. HTTP proxy блокировал
prompt/generate/command/shell: inference/личных токенов не использовалось.
Через6 реальных PTY resize с ответами настоящего TerminalEmulator history marker
остаётся, но не повторяется в raw output; CSI3J после resize отсутствует. Это
не Android pixel/gesture test. Все собственные тестовые процессы завершены.

Mini shell: real private install/reuse без замены, corrupt bundle rejected, existing
corrupt directory preserved, no staging left. Проверены binary/archive SHA и версия.
Первый APK прогон выявил Gradle transparent .gz expansion (missing requested asset);
расширение изменено на .bundle, финальный APK содержит ровно один correct bundle.
Gradle ARM64 FS-watching warning не мешал build.

## Инспекция и ограничения

Изменения3 upstream source files: optional host replayOnResize, private CLI envflag,
ранний выход только из resize replay. Начальная historyhydrate, streaming, модель,
доступ, ID, native server и данные не менялись. Adapter выбирает новый frontend
только для mini; server/request/proxy используют прежний runtime. Flag выставлен
только child env, не shell config. '--replay' при запуске/модели сохранён.

Bundle атомарно копируется в private app files один раз; script защищает установку
flock и SHA, own staging cleanup, не перезаписывает другие/повреждённые папки.
No new stop/signals added in production. Codex/ordinary paths unchanged.
Source patch/manifest retained; generated bundle ignored in Git. Будущая release/CI
должна предусмотреть проверенный bundle; GitHub сейчас не обновлялся.

История native chat не мигрировалась. User отметил v1.0.11 частично лучше; v1.0.12
ещё ждёт проверки IME, Home/Commands return, session switch, empty/old chats иstreaming.
Автопрокрутка и выделение отдельно не менялись. При смене ширины отключён полный
markdown/table re-render истории; reflow emulator остаётся, complex formatting
может сохранять прежнюю ширину. Нельзя объявлять все flicker/scroll issues исправленными.

APK передан в Download, не установлен; code1049 ниже reserved rollback1100.
Обновление с1.0.11 соответствует package/cert/code, фактическую установку делает
пользователь. After update reopen OpenCode to activate frontend.

Receipts .tools/ai-agents/resize-source: build.log,unit.log,typecheck-final.log,
lint-official-node.log,probe-production.log,install-probe.json,apk-build-final.log,
apk-artifact.json,apk-badging.txt,apk-signature.txt,source-receipt.json.
Production native probe run .tools/ai-agents/reopen/run-* recorded in probe-production.log.
Подробнее OPENCODE-MINI.md и patches/opencode-v2.0.28-resize-replay.patch.
