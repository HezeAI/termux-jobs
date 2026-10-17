# v1.5.0 public / v1.5.1-test handoff (2026-10-17)

Public release preparation: v1.5.0 code1137, local models disabled while testing, existing local data retained. Private v1.5.1-test code1138 enables all local models and experimental Qualcomm SM8750 NPU runtime. Same com.termux/signing; next compatible update code must exceed1138. Publication/delivery status is recorded below after final verification; do not infer completion from this preparation note.

Shared update: keep-history checkbox on unsaved close, presets/history separation and supported Codex history manager; clearer agent permissions; server long-press selection, recursive SFTP download to chosen Termux/proot/device destination (10GiB/file,20k entries,depth64,256GiBbatch,6h transport); exclusive UUID batch destination, random per-file temp names, completed files retained on interruption. Same-server nonoverwriting Move/Move here, self/collision checks, partial batch not rolled back. Final UI: left-pointing parent arrow; two equal lower panels ↑ upload / ↓ download or move separated by8dp, replacing wide upload; no extra action after search. No user app installation/service restart; source/build-only work and isolated real server fixture.

724 Java tests passed in full configuration. 153 public-specific tests passed; final artifacts/report at .verification/server-download-npu-v31/. Read docs/fork/RELEASE-v1.5.0.md and RELEASE-v1.5.1-test.md. Existing CPU native libraries/CLI/server-agent assets unchanged from v1.4.30-test. NPU requires actual device testing; SDK presence is not proof of FastRPC/app namespace/model support. Model verified on server under /root/models/gemma4-e2b-sm8750/, use app Download then Import. SDK tools/binaries remain ignored .tools; public APK excludes QAIRT/dispatch.

Still planned separately, NOT implemented here: bounded command large-output logging/preview/continuation instead of misleading64KiB TOO_MANY_ENTRIES failure. Never auto-repeat a command because output overflow may follow side effects. Applies shared local-model tools when implemented, not external CLI agent tools.

---

# Agreed next update: shared local-AI command output handling

User explicitly asks to record for NEXT update only; do not start implementation/build from this confirmation. Current command output above64KiB throws misleading TOO_MANY_ENTRIES and loses captured result; user screenshot curl GitHub HTML demonstrates this is not proof of missing internet.

Agreed behavior for shared LocalAiTools command path (all local llama.cpp/LiteRT models, not independent Codex/Claude/Gemini/OpenCode CLI tools): stream full output into an app-owned bounded log, continue draining output so large output alone does not fail/kill/block command; return compact beginning/end preview, actual exit/completion/cancel/timeout status, explicit truncation and retrievable result reference. Allow later bounded reading/search of retained output under existing permissions/trust boundaries. Define disk retention/cap behavior explicitly; never claim unlimited/full retention after cap. Large output error must not imply action rollback. Never automatically rerun command due to oversized output, because side effects may already have occurred. Keep previews within model context; no general model-context increase implied. For webpages prefer saved page/API/extracted relevant text rather than injecting full HTML.

GPU for Qwen3.5 2B set aside by user after UI lag; remain CPU ARM/6threads for this model. NPU investigation remains separate. Strict native call vs prose errors are a different issue; raw diagnostics now expose them.

# Device regression report after v1.4.30-test

User reports severe UI lag/jank from first GPU message, clarifies not total freeze. No fresh v30 diagnostic or screenshot available at inspection. Read-only current view saw Ubuntu/CPU runner7154 (no OpenCL mapping), GPU busy16%; this does not capture prior GPU incident or exclude unseen app processes. Source: OpenCL queue uses default priority, GPU1024/512 batches; UI inference is off main thread. GPU/UI resource contention is plausible, NOT established from trace. Lightweight device query confirms cl_qcom_priority_hint support, no cl_khr_priority_hints/throttle_hints. Probe source updated in .verification/local-ai-gpu-probe/probe.c; no model/session stopped or changed. Next GPU responsiveness experiment: investigate Qualcomm low-priority context/queue hint, smaller batches/short yields; keep CPU unchanged, don't promise hints remove jank. User only clarified lag in latest message; no new APK produced yet.

# Delivered v1.4.30-test / code1136

GPU integrated as optional localCompute=cpu(default)/gpu, separate OpenCL Bionic runner. Engine settings Operating mode CPU/GPU; GPU hides CPU variant/threads preserving values, manual CPU return restores CPU location too. Initial gate catalogqwen35-2b+SM8750+ARM capability; GPUAndroidexecution independent of Ubuntu tools. Model selection blocks unsupported until CPU chosen. CPU benchmark blocked inGPU; request diagnostics actualbackendGPU. Existing native CPU/control payload byte-identical v29; one newOpenCLrunner, vendorlibnotbundled. Model cache/cancel/quit ownership retained. No automatic CPU fallback after GPU error; selectCPU manually.

NativeGPU IPC realmodel tests passed load/count/generate/repeat/pre-cancel/active-cancel/post-cancel count/benchmark rejection/quit.706 Java tests passed; release/runtime15990defs/30reviewed, package1136/cert/ZIP/SHA/stage/limited secrets passed. APK Termux-Jobs-v1.4.30-test-arm64.apk 61162136 bytes SHA 65005b933b86c96b1dce976e43bdc7144ccdac5fc542b507465e6192115e5068; Download APK+README+SHA. No install/GitHub. Device APK GPU namespace/coldstart/keyboard/tools still developer checks.

Validation cards now raw original call+named invalid/duplicate parameter+tool-specific schema; model feedback omits redundant raw (assistant already contains original). Strict prose/examples/multiple calls still reject. Emptywrite result says didNOTdelete; guide rm+absence verification only whenallowed, HTTPquote/statusguidance. Model choice can still be wrong; don't promise full semanticfix. Oldcards originalnotrecorded.

NPU requested as next separate experiment, not enabled here. OtherQwenGPU requires validation/allowlist expansion; runner architecture generic. v29 source checkpoint retained .verification/checkpoints/v1.4.29-test-code1135, APK kept build/apk; rollback update needs highercode>=1137 samecert. New optional field defaultsCPU, no autodatareset.

# Next build scope added by user after GPU experiment

User agrees GPU option in current Processor mode selector; suggested UI name Computation mode, preserving ARM(auto)/Compatible CPU and adding GPU(OpenCL,experimental), CPU default unchanged. Integration still pending; standalone result does not establish app namespace/runner integration. Keep model tools environment Ubuntu independent of GPU Android/Bionic execution; explain placement instead of silently relabeling Ubuntu/glibc as GPU.
Three latest screenshots inspected:203249 benchmark events,210721 native tool sequence with validation errors interleaved with executed curl/jq/Python requests,210729 opened error Native call must be one complete block without prose. This proves envelope rejection, not lack of network permission; raw call currently hidden so exact malformed shape not known. Executed command card alone does not prove HTTP2xx/task success (curl -s can exit0 on404; displayed URL query appears unquoted with ampersands; target termux/termux may be mistaken). Original embedded Qwen template permits optional reasoning before call, strict adapter does not accept arbitrary surrounding prose; do not loosen execution to arbitrary examples to mask issue. Next fix should show raw output/exact error and tool-specific repair. Deletion(emptywrite!=delete) remains pending too.

# Completed isolated GPU experiment, 2026-10-17

Real Qwen35-2B Q4_K_M ran on Adreno830/OpenCL3, b9222 separate Android/Bionic runner. Exact exported model copy SHA unchanged. All builds/probes in .verification/local-ai-gpu-probe, no default native payload/app/session changes. docs/fork/EXPERIMENT-QWEN-GPU.md and results.json record timings. CPU6thread2048tok22.184s; GPU batch256/12832.739s,512/25624.676s,1024/51217.461s and repeat17.304s. BestGPU~21–22% less prefill time than single CPU control.16token greedy generation CPU0.730s, bestGPU0.997s; same replyprefix. GPU not universally faster; keepCPUdefault, optional futureexperimentalGPU. Native OpenCL lacks fusedGDN; fallback graph works. Shader/context/load excluded; app namespace/permissions/tool integration not tested. GPU is NOT integrated into APK. User deletion/validation fixes remain agreed next build; don't claim fixed from these compute tests.

# Performance follow-up 2026-10-17

User-approved isolated GPU capability probe compiled under .verification/local-ai-gpu-probe and executed without loading models/changing live app. OpenCL platform/device discovery succeeded: QUALCOMM Adreno830, OpenCL3.0. This proves device enumeration in Ubuntu/proot shell only, not app linker namespace access, model compatibility, kernels or throughput. No GPU backend integrated yet. Existing llama OpenCL source has Q4_K paths and SSM_CONV/SOLVE_TRI; full Qwen35 op/shape coverage still needs checking.
Latest threads report6063779124222160485 duplicated as5529871243138086124: 2048tok,2threads44.14/58.91sec;4threads27.90/32.35;6threads28.29/25.17;8threads26.80/26.00. Difference6vs8mean~1.25%, no robust gain. Keep6default. User asked to run existing batch comparison256/128vs512/256 and send report; no APK needed. Current mode2 benchmark already supports it.

# Agreed next build after v1.4.29-test

Add exact invalid/duplicate native parameter name, original model call and tool-specific repair schema to validation cards/feedback (currently generic message plus list/read schema even for command). Do not accept ambiguous duplicates. Device test showed list works, but delete request used write text="" and falsely claimed deleted; /sdcard/Download/Тест.txt confirmed 0-byte existing. Do not classify deletion/commands as device-verified. User now requests read-only investigation of model/engine speed, not another build yet.

# Delivered local test: v1.4.29-test / code1135

Opt-in Qwen3.5 2B native tool adapter (localProtocol=json default / qwen-native). Schema and strict one-call XML-like parser from verified installed Qwen3.5 2B template; not universal Jinja. Engine count/generate share rendered history, service IPC carries mode. Recorded toolRequest metadata only, grouped tool_response and untrusted boundary escaping. Existing permission/approval/runner path preserved. UI Engine settings third selector; only catalog2B/llama supported; reload same chat, custom rights scope stale on switch. Native libraries and control assets byte-identical v28. 698 tests passed; ARM64 build/runtime15987defs/30reviewed passed; final ZIP/package1135/cert/SHA/stage/limited secret checks passed. SHA 92e30c560b60a867c1e8b6e9cd2a6a2283a3f6b4ff195f9db904915774725ee4. Download APK/README/SHA delivered, no install/GitHub. Device adherence remains unverified. Compare fresh native vs JSON chat, same CPU6/ARM/Ubuntu/rights/context8192recommended. Need versionCode>=1136 next.

Latest screenshot Screenshot_20261017_195840.jpg: model false access denial, tool not called, unsafe/wrong oldest-file ls/sort suggestion; screenshot does not prove permission failure or context-compaction cause. User informed.

# Delivered local test: v1.4.20-test / code1126

Schema errors (filter txt) returned to model with executed=false feedback, at most2
schema retries/turn. Invalid calls never run, corrected calls still authorize and
approve normally; security exceptions not retried. Prose not promoted, cancel
checked, internal finish=tool programmer errors not hidden. Failed validation card
records excluded future model context. Prompt distinguishes filter category vs
extension. Native engine unchanged. Auto UI renamed ARM acceleration (when supported),
stored enum auto unchanged. Trusted toolCached history flag labels actual read-cache
reuse; after2 cached repeats a further same-path non-refresh read stops with honest
no-final-answer message. Explicit refresh/other loops not covered.642 Java tests
passed, final APK audit passed. Download APK+SHA+README. No install/GitHub. Both
CPU libs byte-identical19; user tested ARM optimization: cold1263tok~9.9sec vs
old~55sec, but tests not controlled. User manualstop invalidatescache, next4096tok
report66sec could include build/thermal load. New real model workflow needs device
check; tests do not guarantee model understanding. Next code>=1127.

# Delivered local test: v1.4.19-test / code1125

Engine selector in local sessions/presets: auto vs compatible. Optional localCpu
field defaults auto, strict persistence/copy/IPC; busy settings blocked and idle
reload preserves chat and owner/cancel guards. Native generic jobs_llama probes
hardware; fast jobs_llama_fast targets armv8.2-a+dotprod+fp16+i8mm. Check dotprod,
FPHP/ASIMDHP, atomics, RDM, CRC32 and i8mm before loading fast. Separate nested
FastCpu JNI namespace, internal exports hidden; all operations stay in owninglib.
Missing fast library at load-time falls back; no native crash-recovery promise.
CPU4/batches/context/rights/Ubuntu/agents unchanged. Diagnostic backend shows
actual variant and threads.635 full Java tests+50 final UI/limits passed; CPU
loader, guard policy, both cache and real tiny GGUF decode tests passed with same
greedy tokens and expected compiled feature flags. APK audit passed, Download
APK+SHA+comparison instructions. No install/GitHub. Qwen2B actual cold speed and
older-device fallback remain device checks; tiny tests aren't throughput evidence.
Next code>=1126. User has Snapdragon8Elite and wants MODEL tool autonomy, not
more phrase heuristics. v1.4.18 prior-result excerpt/routing changes included.

# Delivered local test: v1.4.18-test / code1124

Folderless latest-file requests clarify folder without inference. Screenshot phone
aliases/text-only real path result; vision-capable sessions retain model flow.
Earlier large tool results excerpted only in next-turn runtime context; archive,
current task/results/rights preserved, chat notice added.632 Java tests and APK
assembly/runtime/signature/archive/source checks passed. Download APK delivered.
No GitHub or installation. Native engine unchanged. User now prioritizes MODEL
tool autonomy and investigating CPU engine performance, not more phrase templates.
CPU baseline generic ARM64, max4 threads, no GPU, no explicit dotprod/i8mm.
Host exposes asimddp/i8mm. Actual installed model is not visible via mounted
/data/data/com.termux/files (only home/usr), so tiny-model CPU tests cannot establish
Qwen gains. Proposed future setting: Auto vs compatible CPU; diagnostic variant/
threads. No engine settings implemented yet. Next code>=1125.

# Delivered local test: v1.4.17-test / code1123

Per-model-call timing records in existing local diagnostics. Metadata only, RU/EN,
64-record bound, active/completed/interrupted status, pause archival behavior.
All local engines use reports; native exact cache/tokens llama-only, LiteRT unknown.
626 Java tests passed; assembly/runtime/artifact checks passed. Native payload and
agent assets identical to1.4.16. No inference/permission/tool behavior changes.
APK and instructions in Download. No publication or installation. Ask for exported
multi-pass report to explain expensive third prefill. Evidence .verification/local-ai-v1.4.17.
Next code>=1124. Data/history unchanged.

# Delivered local test: v1.4.16-test / code1122

Native llama prefill progress, separate setup/cache/first-step timing; RAM prefix
reuse scoped to service owner/chat/exact system, final-token replay for logits,
bounded snapshots for hybrid partial-rollback fallback. Native tiny-model cache
logit/restore/reset tests passed;624 Java tests passed; ARM64 build/runtime/artifact
audit passed. APK in Download. No publication or installation. Qwen3.5 real warm
speed and snapshot support remain device checks. Evidence .verification/local-ai-v1.4.16.
Next code>=1123. Existing session data/history/settings preserved.

# Delivered local test: v1.4.15-test / code1121

User authorized build of local context/reply selectors, progress placeholders, local diagnostic timing/export, and RU/EN fast find-latest-and-read. llama contexts4096/8192; LiteRT4096; reply caps256/512/768/1024/2048. Optional v8 localContext/localMaxTokens defaults4096/768. Safe idle-only reload preserves chat and checks cancel/panel identity before prepare. Native result encodes emitted tokens and finish bit, matched by new Java caller. Stage/metrics IPC replies never complete RPC. Timing metadata only, capped/persisted worker reports with no prompt/tool content. Native build/tests passed. First final suite/build passed prior small idle-label/UI-thread-I/O fixes; rerun final delivery tests/build before audit. Evidence .verification/local-ai-v1.4.15. No installation or publication.

# Delivered: v1.4.14-test / code1120

User approved context reduction with in-chat notice, current-chat history tool, cached reads and immediate literal display of short latest-text requests. Native exact token counting rebuilt with shared prompt-token helper; LiteRT estimated budget plus one overflow retry. Full history is unchanged; latest user/system permissions never silently cut. Mode chat disables history tool. No external agent changes. New optional history flags contextEvent/literal. Source/build evidence in .verification/local-ai-v1.4.14. Final test/build/audit still pending. No GitHub/install authorization.

# Delivered local test: v1.4.13-test / code1119

User approved common latest-file discovery after local model searched /sdcard instead of Download for latest TXT. LocalAiRecent parses conservative short RU/EN requests, runs locations → scoped list with exact extension and mtime sorting → text read only for open/read intents. Questions/paths/named files/compound requests stay model-driven. All normal auth/environment/path checks remain. Details show checkedPath. No native/runtime/external-agent changes. Tests/build/audit evidence: .verification/local-ai-v1.4.13. No installation/publication authorized.

# Текущая проверка: v1.4.12-test / code1118

LocalAiNames: варианты написания/транслитерации + явные variants от модели (не универсальный перевод). LocalAiSearchResult: только absolute file candidates, exact validation, multiple/approx single requires choice. Events.chooseFile + awaitFileChoice/FileChoiceCard: явный выбор, cancel/Stop не продолжают модель; restored read-only. Friendly results + Technical data. Контроллеры внешних агентов не менялись. Проверки .verification/local-ai-v1.4.12. Следующий code>=1119. Без GitHub.

# Текущая проверка: v1.4.11-test / code1117

Приветствия/вежливые фразы screenshot discovery; LocalAiToolText — только presentation, не executable. Смешанный JSON+проза вызывает один repair для action request, только standalone JSON через authorize/approval исполняется. Карточка preview unconfirmed с raw details, key-cache; пользовательский текст/история не меняются. Все локальные модели, внешние агенты не менялись. Проверки .verification/local-ai-v1.4.11. Следующий code>=1118. Без GitHub.

# Текущая проверка: v1.4.10-test / code1116

Настройки внешних AI: Codex read/write + write scope + optional approval; миграция null сохраняетстаруюполитику. OpenCode opt-in project/worktree scope denies shell/external/unknown. Claude/Gemini scope недоступен честно. Применение при следующем запуске, работающие процессы не трогаются. Новый immutable bridge-v0.2.20, старый не перезаписывается. Проверки .verification/agent-permissions-v1.4.10; следующий code>=1117. No GitHub. Downgrade ignores optional new fields and may relax new scope/approvals: warn users.

# Текущая проверка: v1.4.9-test / code1115

Переносимый screenshots tool для всех локальных моделей: несколько Android storage aliases/папок, смешанные форматы изображений, сортировка mtime, dedup dev/inode, отдельные статусы missing/denied/scope. Короткие прямые запросы «3 последних скрина» запускают поиск до генерации. Расширены locations, сохранены выбранная среда/права. CLI других агентов не менялись. Проверки .verification/local-ai-v1.4.9, следующий code>=1116. Без GitHub.

# Текущая проверка: v1.4.8-test / code1114

Выбор окружения инструментов: Основной Termux или выделенная Ubuntu jobs-local-ai. Проверка/предложение первой установки, прогресс по этапам, Python3/Git, владельческие маркеры, --isolated и только общая память, без fallback на рабочую Ubuntu. Устаревшие значения локальных профилей сохранены, но требуют повторного выбора. Обычные сессии не меняются; выделенная маршрутизация через ephemeral backend local-ai-ubuntu. Установка реальной среды во время разработки не выполняется. Проверки `.verification/local-ai-v1.4.8`.

# Текущая проверка: v1.4.7-test / code1113

Готово в Download; 534 теста прошли, SHA/подпись/ABI/ZIP/runtime/source-stage проверены. Следующий APK code>=1114. Важные пробелы: реальные Gemma vision/видеокодеки/APT проверяются разработчиком на устройстве; см. VALIDATION-v1.4.7-test.md.

Локальные инструменты метаданных/папок/ошибок/APK, экспериментальные изображения Gemma LiteRT и кадры видео, фиксированная установка недостающего инструмента после плана APT и Да/Нет, в том числе Read при Confirm actions ON. Qwen всё ещё без визуального проектора. Названия «Права локального ИИ» и «Работать только в пределах проекта», смысл/хранимые поля прежние. Без установки APK/пакетов/моделей и GitHub. Доказательства `.verification/local-ai-v1.4.7`; реальные vision/APT сценарии проверяет разработчик.

# Текущая проверка: v1.4.6-test / code1112

Локальные сессии: название в настройках, потоковая прокрутка без фокуса/скрытого UI, менеджер проекта; актуальные права без перезагрузки модели, один retry ложного отказа. Без GitHub/установки. Проверки `.verification/local-ai-v1.4.6`.

# Текущая проверка: v1.4.5-test / code1111

Локальный чат: независимая анимация точек в строке управления, раскрываемые карточки инструментов с фактическим статусом, бирюзовая линия у пользователя. Разрешения без изменений. Текущие доказательства `.verification/local-ai-v1.4.5`; без публикации/установки. Старый recovery1110 ниже новой сборки; новую recovery не запрашивали.

# Текущая проверка: v1.4.4-test / code1109

Три независимые настройки Local AI: режим действий, Проверка путей, подтверждения.
Chat: нет tools. Read: list/read/search. Full: +write/command, НО command запрещён
при Path checks ON (до approval и в script). При OFF пути вне проекта разрешены
в пределах прав Android/среды; write/command всё ещё требуют Full access.
Отключение Path checks через UI требует явного положительного ответа предупреждения;
до него checkbox и Selection остаются true. Старые false-профили сохраняются.
Без отдельной изоляции, без GitHub/установки APK. Rollback1.3.4 code1110,
после него следующая сборка должна иметь1111+.

## Историческая запись

# Текущая проверка: v1.4.3-test / code1107

LocalAiAgent общий для llama/LiteRT/импорта. Full access по умолчанию авто.
Опция Подтверждать действия включает карточки Да/Нет для write/command; не расширяет
режим read/chat. Проверка путей независима. stop отменяет ожидание карточки.
Reasoning — плашка → отдельное окно с собственной прокруткой.
Исправлен CPU Qwen non-thinking suffix по официальному chat_template.jinja;
JNI nativeGenerate теперь возвращает int finish (eos/token_limit). Не смешивать
старую .so с новым Java. SDK finish — sdk_done, не выдумывать EOS/причину лимита.
Один внутренний retry для reasoning-only/предложения вместо действия; сырые
рассуждения/проза не исполняются. Выполнение конкретными моделями проверить на устройстве.
Без GitHub/установки. Baseline rollback1.3.4 code1108, следующий APK после него>=1109.

## Историческая запись

# Текущая проверка: v1.4.2-test / code1105

Запросы инструментов из блоков json/tool/без подписи распознаются. Структура, режим
и переключатель путей проверяются как прежде; текст/произвольный shell-блок не исполняется.
Долгий тап: Новый чат / Выбрать чат / Настройки. Модель и доступ — в Настройках.
Из списка сессий три дублирующих пункта удалены. Без GitHub и установки APK.
Rollback прежней v1.3.4 обновлён до1106; следующая сборка после него требует1107+.

## Историческая запись

# Текущая проверка: v1.4.1-test / code1103

Разрешены локальная сборка и передача в Download, без GitHub/установки.
Локальный ИИ: Markdown, свёрнутые think, компактное управление, три режима доступа.
Полный доступ — автоматические запись/команды без подтверждений. Галочка
«Проверка путей» отдельно ограничивает файловые инструменты проектом; команды
не являются песочницей. Исходники/истории остальных ассистентов не сбрасывать.
Свежий rollback прежней v1.3.4: code1104; после него нужен code>=1105.
Ограничения и проверки: LOCAL-AI.md, RELEASE/VALIDATION-v1.4.1-test.md.

## Историческая запись

# Текущая работа: локальный ИИ v1.4.0-test / code1101

Пользователь разрешил сборку для проверки, БЕЗ GitHub/установки APK. Явно разрешён
новый rollback текущей v1.3.4 с code1102 (это отменяет прежний запрет создавать его).
Checkpoint: .verification/local-ai-v1.4.0/checkpoint (исходники+Git bundle+patch).
Новая функциональность/ограничения: docs/fork/LOCAL-AI.md. NPU runtime не включён,
llama.cpp CPU, LiteRT GPU/CPU требуют проверки на устройстве. Не выдавать за полную
реализацию NPU/MTP/инструментов агента. Пользователь просит итоговый список пробелов.
После recovery следующая сборка обязана иметь code>=1103. Старые параметры/истории
Codex/Termux не удалять. Документы ниже — исторические записи.

Финальная локальная1.3.4:443 JVM-теста прошли, APK проверен и передан в Download.
SHA-256 d0d97d0ef84c89aae10e18569a5ac7d4742acbfc9ab1f080de2a64db3e2a5eef.
Публикацию1.3.4 пользователь пока не запрашивал, GitHub остаётся1.3.3.
Нужна проверка поиска/прокрутки/IME и повторного открытия окон на устройстве.

# Локальная v1.3.4/code1087

Пользователь разрешил освобождение закрытых окон и стабильный поиск. Диагностику
не менять, опрос Codex тоже не входит в эту работу. GitHub остаётся v1.3.3.
После тестов/сборки APK в Download; новой rollback-сборки не создавать.
Проверки .verification/ui-performance-v1.3.4. Без установки APK/работы с сессиями.

Исторические записи ниже.

# Опубликовано v1.3.3 — Обязательное обновление

https://github.com/HezeAI/termux-jobs/releases/tag/v1.3.3
Source/tag dbc90d7f. Latest stable, draft=false/prerelease=false.
APK/SHA256SUMS/update.json скачаны обратно и совпали; публичные metadata и
документы проверены. Android8.0/API26 минимум. 440 JVM-тестов прошли.
Финальный APK в Download, SHA-25672ef607dc8d1005abeeefa014ae985ec107394fc63fe8850d60e0335227592a7.
Подпись/пакет прежние, код1086. Нужна проверка разработчиком закрытия/запуска
после обновления. Ожидаем обратную связь; не устанавливать APK самостоятельно.
Старые локальные записи ниже исторические; новый откат не создавать.

Финальный APK SHA-256: 72ef607dc8d1005abeeefa014ae985ec107394fc63fe8850d60e0335227592a7

# Срочное исправление v1.3.3/code1086

Пользователь разрешил минимумAndroid8.0/API26 и публикацию с названием
«Обязательное обновление», с объяснением нашей ошибки Guava. Отдельного
разрешения публикации не спрашивать; APK не устанавливать.
Штатный FileUtils восстановлен; собственный NioFileDeleter отсутствует в финальном APK (проверено). Guava33.3.1-jre в обоих модулях + согласование транзитивных запросов.
440 JVM-тестов прошли. Проверки .verification/crash-v1.3.3. Важен аудит отсутствия
устаревшего класса в финальном APK и повышение minSDK в RU/EN metadata.
Новой rollback-сборки не создавать; установленную среду/процессы не менять.

Ниже исторические записи.

# Релиз v1.3.2/code1085

Опубликовано: https://github.com/HezeAI/termux-jobs/releases/tag/v1.3.2
Source/tag ae002f7e; latest stable, draft=false/prerelease=false.
APK, SHA256SUMS и update.json скачаны обратно и совпали. Публичные metadata/документы проверены.
APK неизменён относительно переданного в Download.
437 JVM-тестов прошли. Данные без миграций/сброса; пакет/подпись прежние.
Синхронизация отложена. Новой rollback-сборки не создавать.
Публикация: .verification/release-v1.3.2; исходные проверки .verification/xda-v1.3.2.
Ниже исторические записи локальной подготовки.

Финальная1.3.2:437 JVM-тестов прошли, APK собран/проверен, SHA-256
76bb3399711ca5cd4845246784280e2ca73414b8cd0c395436bb9d1845f4da6d.
Пользователь проверяет APK в Download; GitHub публиковать после подтверждения.

## Текущая локальная работа v1.3.2/code1085

Кнопка Back в просмотре звёздочки возвращает к тому же picker, вкладке/поиску/позиции.
Добавлены3 XDA/primary-source карточки (всего129), уточнены signal9/proot-root.
Синхронизация отложена. GitHub остаётся1.3.0. Новая rollback-сборка не нужна.
Доказательства: .verification/xda-v1.3.2; исследование .verification/xda-research-20261016.

# Локальная v1.3.1/code1084 — проверка интерфейса и справочника

GitHub latest остаётся1.3.0/code1083. Пользователь разрешил добавить исследованное
и согласованный просмотр в звёздочке; текущего разрешения публиковать1.3.1 нет.
Новый rollback НЕ создавать. Основной APK1.3.1 в Download, подпись/package прежние.

Терминал → звёздочка: открывается полный текст без вставки. Три действия:
редактировать, вставить в текущий черновик без отправки, удалить с подтверждением.
openInputEntry переиспользует previewBody и прежние редакторы. Основные просмотры
команд/заметок и недавних команд не переделаны. Поиск/переключение списков сохранены.

Справочник126 карточек:12 новых из проверенных первоисточников,8 дополнены без
дублей. Справа от заголовка «Собрано с помощью AI!» / «Compiled with AI!».
435 JVM прошли, финальная сборка/подпись/ZIP/ABI/hash/stage проверены.
На устройстве проверить длинный текст, действия, IME и заголовок RU/EN при DPI.
Доказательства .verification/star-library-v1.3.1/, VALIDATION-v1.3.1.md.

4PDA741456: прямой доступ403, включая st20/st40/st7980 и страницу авторизации.
Поисковый индекс дал часть обсуждений/шапку; все страницы/цели ссылок не прочитаны.
Первичные документы проверены отдельно. Данные для входа не передавались сайту
и не записывались в проект; не переносить учётные данные в контекст/отчёты/коммиты.
Отбор .verification/4pda-741456/ и docs/fork/FORUM-REVIEW-v1.3.1.md.

Прежняя публикация1.3.0: исходникиb73fb904, receipt/audit .verification/release-v1.3.0/.
Релиз опубликован,3asset сверены, updater schema1 совместим. Telegram-пост с3частями
и APK отправлен через существующего бота в настроенную тему; настройки/состояние
серверного бота не менялись. Повторно ничего не отправлять без команды.

Не менять установленный Termux/Ubuntu/общий SDK/Bridge/пользовательские процессы,
не устанавливать APK. Инструменты/кэши только в репозитории. design/ предшествует
задаче. Разработчик HezeAI; Codex помощник. Для публикации RELEASING.md.

Final v1.4.8-test: 545 tests passed; APK audited and delivered to Download. SHA-256 1a009cee2751e1913d55bacfd59cf03b35899a83dadfbe15b109a200e9fb12f8. Next build code >=1115. Device setup testing pending.

Final v1.4.9-test delivered in Download: 554 tests passed, ARM64 APK/source/CRC/signature/SHA verified. SHA-256 dec36b0f35a60a2646185e27def007a5fdced46c7feb02d96c135827cbc34813. Dark local settings included. Next code>=1116. Device checks pending; no publication.

Final v1.4.10-test delivered in Download. 563 Java tests + 11 fake RPC scenarios passed, ZIP/package/version/ABI/signature/SHA/runtime/source-stage checked. SHA-256 a97b4f88630043a5eaa5b86eb6ff678b293decfeb0ef78ebd1c19105234b51ca. Device enforcement tests pending; see VALIDATION-v1.4.10-test.md. Next code>=1117. No publication/installation.

Deferred by developer: next update generic file search with name variants/transliteration, suffix matching and candidate selection (факторио/factorio). Do not claim implemented in v1.4.11-test.

Final v1.4.11-test delivered to Download: 572 tests passed; final APK/source/ZIP/package/ABI/signature/SHA/runtime checks passed. SHA-256 ece3ffb035e57fff4a04b25d0d04073aa5a5ade45cab7c38c8778082be8b2dc1. Native/external agent controller assets unchanged from v1.4.10-test. Device checks pending; next code>=1118. Generic filename alternatives deferred. No GitHub publication.

Final v1.4.12-test delivered to Download. 584 tests passed; APK/source/package/version/ABI/ZIP/signature/SHA/runtime verified. SHA-256 752bbcd954a60a983c1ba947a8a94743497a3f8de268f4723e37768f32c3e949. Native/external agent assets unchanged. Device checks pending, next code>=1119. No GitHub publication or APK installation. Generic spelling variants + choice implemented; universal translation remains unsupported.

Final v1.4.13: 593 tests passed; build/runtime/ZIP/package/ABI/cert/source/limited-secret checks passed. Download APK/README/SHA delivered and hashes verified. SHA256 aeb0992e89e3a9e65515d88d0ab94418cabc386c9b8928b1b523007869c123bd. Next versionCode >=1120. Device tests pending; no GitHub publication.

Final v1.4.14: 613 Java tests passed; native real-vocabulary count/policy tests, build/runtime/ZIP/package/ABI/signature/source/limited-secret audit passed. Download APK/README/SHA delivered and verified. SHA256 bb6e8cc53323e7376bb6f4dcaa6eb208f383bc12e95b26bb6d27afcb132dd216. Next versionCode >=1121. Device checks pending. User deferred configurable context/output limits to next update: keep4096/768 now. No GitHub publication or APK install.

Final v1.4.15 delivered to Download:623 full-suite tests +44 final UI/limits checks passed; native/build/runtime/ZIP/package/ABI/certificate/source/limited-secret checks passed. SHA256 29dc4d416fdc1f0950ad4addae7b57e61ecc198513b79c196b4bacc9bd7c4e0e. Next versionCode>=1122. User device checks pending. No publication or APK install. Settings context/reply wired through saved presets/privateIPC/engine; per-engine context choice (llama4096/8192,LiteRT4096); reply max256..2048. Diagnostics long tap local session, Copy/Download export, status placeholders. No further code changes after final audited build.

## Latest delivery v1.4.22-test /1128
Delivered /sdcard/Download/Termux-Jobs-v1.4.22-test-arm64.apk. Engine settings contains only CPU mode and thread selection; localThreads optional integer 0/2/4/6/8, Auto preserves max4; service clamps to available CPUs. Store/copy/create/preset/settings/reload/IPC paths updated. 653 tests and final audit passed (.verification/local-ai-v1.4.22/artifact.json). No APK installation/publication. Developer should select6 threads, save, test normal Qwen chat and export timing. Next versionCode>=1129.

## Latest delivery v1.4.23-test /1129
Delivered /sdcard/Download/Termux-Jobs-v1.4.23-test-arm64.apk. Input composition after context fitting via loaded llama tokenizer, total exact, leave-one-group-out approximate components: system/tool definitions, previous messages, tool requests/results, current user message, formatting residual. LiteRT estimated. Metadata only via existing performance callback; per-pass timing reports. Attribution failures nonfatal/unavailable; cancellation propagated. Button now simply Engine settings.656 tests/final audit passed (.verification/local-ai-v1.4.23/artifact.json). User should export new-chat greeting and old-chat greeting reports. No device claim/no publication. Next code>=1130.

## Latest delivery v1.4.24-test /1130
Delivered /sdcard/Download/Termux-Jobs-v1.4.24-test-arm64.apk. Shorter tool/system instructions preserving schemas/options and authorization boundaries. Earlier-turn tool outputs >256 chars now128head/48tail plus source/history marker; current-turn outputs and saved archive untouched by this layer. Local saved preset menu Close/Settings/Delete; model/import picker cards use settingsButton palette.658 tests and final artifact audit passed (.verification/local-ai-v1.4.24/artifact.json). Native libraries unchanged. Real model quality, UI and timing require developer checks; asked same-message old/new chat reports with6threads and a file lookup. No install/publication. Next code>=1131.

## Latest delivery v1.4.25-test /1131
Delivered /sdcard/Download/Termux-Jobs-v1.4.25-test-arm64.apk. Shared local-agent list/search/screenshots context projection to columns/rows after real execution/selection; originals retained in history/UI; all requested rows preserved, no8000char cut on valid compactlists. Instructions recommend filter/extension/sort/limit1 for newestTXT, not forcedrewrite. Applies llama/LiteRT, not externalCLI. Added local-vs-web search/build-tool clarification and clearer draft status (not approval).661 cases passing:fullsuite then targeted39 workspace cases after two stale UI-label assertion updates; see validation evidence. Final artifact audit passed; all native payload unchanged. No install/publication. Ask developer to repeat latestTXT task/report and inspect listingrequest params. Next code>=1132.

Next update discussed: engine-location experiment Android embedded(default)/nativeTermux/Ubuntu for localAI, independent of tool environment. Compare same llama commit/model/input/thread/batch first; no promise Ubuntu faster. Do not use working Ubuntu or touch active Codex/Bridge. No engine-location implementation yet.

## Latest delivery v1.4.26-test /1132
Delivered /sdcard/Download/Termux-Jobs-v1.4.26-test-arm64.apk (64618337 bytes), SHA256 9e0bd3f74c33c2e1c66355bfca0101eefb3a319a67c3d9f0cdde785c0944b6cd. Environment settings now has Tools (Termux/dedicated jobs-local-ai) and Engine (android default/termux/ubuntu) cards. Same dedicated Ubuntu for both, never working Ubuntu. localLocation optional v8 field defaults android; copy/store/creation/settings/reload/IPC/instructions updated. LiteRT Android-only. Engine settings still CPU mode+threads. Four bundled local stdin/stdout runners, baseline+fast per Bionic/staticGNU platform. Same GGUF directory mounted into Ubuntu; no duplicate weights, HTTP endpoint or remote calls. Prefix cache/tokenization/sampler/batches/bench share maintained C++ helpers. Own-process request-ID cancellation, EOF/quit, parent-death SIGKILL. Native binaries/source/buildscript: app/src/main/cpp/local-ai/runner and scripts/build-local-ai-runners.sh. Linux static flags must be -static-pie WITHOUT explicit -static-libgcc/-static-libstdc++ (bad combination caused missing interpreter+DT_NEEDED crash, corrected).667 Java tests and all four raw/packaged runners tinyGGUF + cancellation/lifecycle tests passed. Isolated proot smoke passed, not actual installed proot-distro wrapper/Qwen/device UI. Final audit .verification/local-ai-v1.4.26/artifact.json. No install/publication. Developer should test Termux/Ubuntu startup then same benchmark length with6threads across three locations; report actual speed unknown. Models location/copy/move discussed but not implemented: same-file bind already sufficient for engine comparison; physical adjacency is not a compute accelerator. Next code>=1133.

## Latest delivery v1.4.27-test /1133
Delivered /sdcard/Download/Termux-Jobs-v1.4.27-test-arm64.apk. SHA256 928588abea13cd73d8887a118cd7c9857175c52b557a3d418b03e99f8ef8ba37. Fixed bundled local-ai-agent-instructions.md read/cached via LocalAiInstructions, injected before current settings through Events on every local llama/LiteRT agent turn. No project-file instruction loader. Guidance chat/read/full rights, real access/result checks, missing dependencies, oldest/newest target checks, filtered listings not whole directories. No rights/approval/native/schema/CLI/process changes. Dedicated install tool still requires Confirm actions and explicit plan; full unscoped commands may install using selected env package manager.670 tests/full build/runtime/artifact audit passed. First test compile failed due old Robolectric getApplication API, corrected to application. Device/Qwen compliance unverified. Need test existing/new chat actual tool behavior; no guaranteed compliance/speed; added system tokens reset prefix cache. README/SHA/instructions copied Download; no install/publication. Next code>=1134. Evidence .verification/local-ai-v1.4.27/.

## Latest delivery v1.4.28-test /1134
Delivered /sdcard/Download/Termux-Jobs-v1.4.28-test-arm64.apk, SHA256 f4eda64c97532163e4b5f4fcd6799e271d4018be6a7a5aaba13509aa84b62756.686 final Java tests passed; build/runtime/artifact audit passed (.verification/local-ai-v1.4.28/). Native payload/adapters unchanged fromv27. No install/publication/environment/process changes. Settings custom behavior/rights, explicit optional Termux commands, new-local Ubuntu tool/llama default and LiteRT Android; legacy sessions preserved. LocalAiPrompts validates TXT/MD UTF8 max12KiB; stores copied text in optional v8 fields, stale custom rights scope blocks until review/reset; app real settings still enforced. LocalAiTools command-only optional environment ubuntu|termux, original authorization then derived Termux HOME, no fallback. UI direct shared-storage root-bounded picker with access request. Standard prompt action examples, any dependencies through allowed full shell, local-vs-web distinction, bounded continuation after promised next step. Close unsaved local/normal checkbox Keep history defaulton, Close/Addpreset+close; CLI-native histories independent. LocalAiUi serial close+delete after writer tasks, shared active-chat/closing guards. History manager global settings local+Codex MainTermux/workingUbuntu, native thread/delete only, read includeTurns with bounded preview; unsupported methods fail, no archive or file fallback. Parent dismissal does not cancel successful persistent deletion updates. Store binding cleared only after successful deletion. Codex protocol inspected via installed CLI-generated schema (official docs fetch403), no user app-server/history operated during development. Device checks needed particularly local/Termux routing, keyboard/storage/DPI, plain-language Qwen behavior, Codex version/API/history support. Existing external CLI chats need user close too (cannot exhaustively detect outside app). Docs/readme/changelog/release/validation updated. Previous v27 checkpoint retained. Next code>=1135.

## Post-v1.4.28 diagnosis and next agreed experiment
User exported installed qwen35-2b.gguf to /sdcard/Download/Qwen3.5-2B-installed-copy.gguf. Actual full SHA25657a1085840f497d764a7fc5d346922dbde961efb54cc792ea81d694fd846a1d8 and1396198496bytes match pinned catalog; first16MiB upstream identical. Independent verified copy .verification/local-ai-v1.4.28/diagnosis/models/qwen35-2b-original.gguf, do not alter original installed/exported copy. Parsed actual metadata/chat-template and delivered JSON/TXT Download. Source Jinja uses tools/<tool_call><function=...><parameter=...> and <tool_response>, current C legacy ChatML+plainJSON differs; causal effect on refusals unproven. Think-disabled suffix matches source.3 isolated navigation probes passed; switching views/onStop do not unload, loading another local model intentionally unloads previous. Activity destruction closes engine. Observed live PID16411 with qwen model mapping, not a continuity test. Next update agreed: Engine settings -> Format of communication with model -> Current JSON / Native model template (testing), Qwen first, keep fallback/rights unchanged. NOT IMPLEMENTED. User asks copy inspection fulfilled after export; original source/APK/model/process unchanged by diagnosis, except this note and verification artifacts. Need compare disposable create/read/delete tasks with native vs current protocol, not more permission expansion.


## Publication 2026-10-17: v1.5.0 / private v1.5.1-test
Public stable v1.5.0 (1137) published at https://github.com/HezeAI/termux-jobs/releases/tag/v1.5.0 from commit981cb020. APK63573526bytes SHA2569f550c5f0cead995bce7b353b8e3a128997781e1015c7f18fd00ce31b4f51b9c. GitHub asset digest matches final APK; update.json and SHA256SUMS included. Public local-model entry intentionally disabled for testing; existing data preserved. Private enabled/NPU testing APK /sdcard/Download/Termux-Jobs-v1.5.1-test-arm64.apk code1138 SHA2564aeba58f2b776eb75976903f1039f35f317a541531cec6d73ec3123beb286d78. Same package/certificate, next update code must exceed1138. 724 Java tests,153 public targeted tests,41 Python tests, native permissions test, real isolated SFTP smoke and final artifact checks passed; actual NPU inference remains unverified on device.

Next update ONLY, agreed server controls: replace background-poll checkbox with Connect/Disconnect button, status Connected/Disconnected, rename polling Auto-update server data. Disconnect must close this app's connections to selected server and stop auto-update, without stopping agent, remote tasks or independent SSH sessions. Reconnect restores panel connection and updates. Preserve current10s inside/30s outside cadence while app active; implement owned-connection cancellation safely. Not implemented in v1.5.0/private1.5.1.

Post-publication: downloaded all three public release assets as read-only HTTPS streams and independently verified SHA256; GitHub latest stable confirms v1.5.0.
