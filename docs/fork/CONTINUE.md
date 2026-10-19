# Релиз v1.7.0 / code1215

Авторизована публикация stable v1.7.0 с окружениями, пакетами и бэкапами.
Итоговые проверки — VALIDATION-v1.7.0.md. 668 JVM/146 Python проходят,
APK проверен; меню удаления новых восстановленных копий требует устройства.
Исходная Ubuntu/AI Agents общая, не мигрировать. Неподтверждённые jobs-копии
не удалять из приложения по одному имени. Архивы при удалении копии сохранять.
Статус публикации будет уточнён после проверки GitHub assets.

Ниже история разработки; старые «не публиковать»/ожидания проверок относятся
к соответствующим промежуточным версиям и не отменяют текущую авторизацию.

---

# Текущая проверочная сборка: 1.6.8-test/code1204

Исправлен Void OCI missing /bin/bash: ограниченный переход на native run с явным
/bin/sh, общий для SessionShell/ProjectFiles/команды входа. Старые Ubuntu/AI пути
сохранены. APK в Download; GitHub не публиковать. Проверить запуск/файлы Void.
Arch скачан в .verification и проверен: SHA совпадает, root shell /usr/bin/bash,
ELF AArch64. В рабочую среду Arch не устанавливался, ждёт теста разработчика.
VALIDATION-v1.6.8-test.md содержит факты и ограничения; особенно восстановление
Void без OCI-манифеста пока не проверено. Будущей версии нужен code>1204.
Старые APK ROLLBACK имеют меньший код, не передавать их как обновление.

---

# Текущая проверочная сборка: 1.6.7-test

Void/Arch добавлены в современный установщик, см. ENVIRONMENTS.md.
Feature code1203 заменяет прежний code1201: разработчик случайно установил
rollback1.6.6/code1202; подпись и хеш установленной APK подтвердили именно откат.
code1203 позволяет обновиться поверх без очистки. Будущий stable code>1203.
Старый rollback1202 уже не устанавливается поверх1203; при необходимости
подготовить новый откат с ещё большим кодом. Не передавать rollback как обновление.
Оба APK для Download; GitHub не публиковать без отдельного указания.
Результаты базовых проверок разработчика для Debian/Alpine/Fedora/openSUSE
записаны в VALIDATION-v1.6.6-test.md; Void/Arch ждут реального запуска.
Ubuntu и агенты остаются общими, миграцию в ubuntu-ai не выполнять.

---

# Текущая незавершённая публикация: 1.6.6-test

Проверочная сборка и APK отката в Download. GitHub пока не публиковать: ожидается
проверка разработчиком. [Изменения и ограничения](ENVIRONMENTS.md),
[проверки](VALIDATION-v1.6.6-test.md). Feature code1199, rollback stable1.6.5 code1200.
Будущему стабильному выпуску требуется code>1200 и update.json.
Ubuntu и агенты остаются общими; ubuntu-ai — только будущий план без миграции сейчас.
Не затрагивать design/codex-session-status-concept.png: ранее созданный отдельный макет.

---

## Latest private delivery: v1.5.19-test (1156)

## Current private rollback — v1.5.21-test

Developer cancelled the LoRA route. Production app/native sources restored exactly to6a9ab547 (v1.5.19-test), code1158/name1.5.21-test to update over1157. LoRA loader/training draft commits7c6d6182/9ffa1501 preserved in Git history, not current feature. Owned checkpoint downloader stopped; no LoRA training steps or trained adapter. Isolated preparation/partial downloads retained in ignored .tools/lora-training and .verification/lora-training; do not assume they are installed app models. No GitHub authorization in this task. design/ untouched. See RELEASE/VALIDATION-v1.5.21-test.md for final results. Main next direction is intentionally undecided by the developer.


Delivered APK/README/SHA in Download. LiteRT retains full canonical expected append vs fitted actual SDK history; exact historical edits/rights/key/runtime-prefix/reply cap/media/error/cancel checks retained. SDK25 fake-inference tests cover compacted reuse, changed dropped history and reporting-failure cleanup. Both local engines: explicit requested file-operation claim check using current-turn successful tool kinds, one correction then unconfirmed output; no constant prompt/inference overhead. No universal target/content/order/intent verifier; arbitrary successful commands opaque. Invalid claims repair-marked, excluded from future context/UI; permissions/approval/cancel/loops unchanged. Fixed noun vs past-verb/punctuation false positives; no replay of old successes. Full684app+145emulator, final88focused and25SDK checks pass; final build/runtime/APK/signature/native equality/limitedsecretscan/DownloadSHA pass. APK smaller due ZIP gap padding, not removed native payload. No persistence resets, model/native/environment/process/GitHub changes. Repeat Qwen task and two Gemma requests. LoRA/index work not started. See RELEASE/VALIDATION-v1.5.19-test.md. Next code>=1157. design/ untouched.

## Latest private delivery: v1.5.18-test (1155)

Delivered APK/README/SHA to Download. LiteRT optional current progress tried before canonical compaction; fixed reset/retention reason codes. Qwen installed template explicitly permits prose before calls: bounded plain prefix (inline filename allowed), single terminal complete typed block; reject recognized examples/fences/multiple/trailing/malformed, existing authorization/approval retained. No universal intent-detection claim. Read-only exported GGUF metadata/tensor directory reinspection,335tensors,1.94Belements, weights untouched.674app+145emulator pass,23actualSDK/fakeinference pass, final build/runtime/APK/signature/ABI/limitedsecretscan/DownloadSHA pass. No native/model/environment/session changes. Repeat same Qwen and Gemma device tests; speed not established for this build. See RELEASE/VALIDATION-v1.5.18-test.md. No publication; design/ untouched. Next code>=1156.

## Latest private delivery: v1.5.17-test (1154)

Delivered /sdcard/Download/Termux-Jobs-v1.5.17-test-arm64.apk + README/SHA. Llama JNI+4CPU runners: explicit final transient-feedback index and conservative exact-token checkpoint before its role boundary replaces tail64; existing fullsystem/chat/owner/prefix/budget128MiB/cancellation checks unchanged. All6JNI/CPU binaries rebuilt together; old libs mustnotmix. Native tiny test trimdisabled:505vs406/901cached, logits matchcold; actualQwen gainpending. Old batch reports found: CPU6 256/128 vs512/256 differences overlap, so baseline andselectedthreads retained; configured batch/ubatch/thread diagnostics added, no newbenchmarkmenu. LiteRT isolatedSDKbenchmark enabled beforeinitialize, optionalmetrics +tokenCount reported without invented cachehits. Same-owned exactsemantic-one-message append retainsConversation; currentfeedback merged into incoming user/toolresult, priorruntimefeedback fullybudgeted, originalsnotpersisted. Drops on changedrights/chat/schema/sampler/replycap, media, compaction/nonprefix, errors/cancel/leaks. External history annotations sanitized. SDKautoloadtoolsfalse, appactionpermissions unchanged. Dropdown restoresv1.5.11 sizes (340dpbound,wrapheightmin48,14sp,13/8padding,12corners) withcurrent22dpvectors/model/activehighlight; topselector/serverbuttonunchanged. Final670app+145emulator+22SDK/fakeinference pass; all4runnerssmoke+JNIloader+snapshotchecks pass; APK/runtime/signature/nativehash/limitedsecretscan/DownloadSHA pass. RealNPUmetrics/reuse, Qwenlatency andDPI need developercheck. NoGithub/env/process/weights changes. See RELEASE/VALIDATION-v1.5.17-test.md. Nextcode>=1155. design/untouched.

## Performance investigation after1.5.16 (no new APK)

See LOCAL-AI-PERFORMANCE-RESEARCH-v1.5.16.md; evidence under .verification/engine-research-v1516/. Developer report7281398394140515414 proves Qwen prefix_snapshot1423 after first pass, total95777ms vs previous181406ms, prefill82799vs166964ms (different histories/actions, no controlled speed guarantee). Screenshot action order still wrong: two reads before write, no readback after. Next cache candidate: current tail64 snapshot lands inside long mutable appFeedback; capture exact token prefix before feedback role instead, preserving scope/system/budget checks. Actual Qwen tokenizer shows candidate commonprefix grows with history while currenttailmisses; real tiny-model forced-snapshot inference restores505vs406tokens with cold logits matching. No production patch yet. CPU GDN and Q4_K2048x2048 microbenchmarks forward/reverse:6threads better than8, larger blocks sometimes improve pertoken work; not wholemodel profile. Native fastARM already enabled, OpenMPoff doesnotmean1thread. Pinned LiteRT0.17SDK HAS getBenchmarkInfo/getTokenCount and ExperimentalFlags.enableBenchmark; our adapter currently doesnotcollectmetrics and recreatesconversation. Actual NPU metrics/lifecycle reuse untested; cancellationrollback caveat. Read-only upstream MIT/Apache licenses/source fetched afterweb403. Separateengineartifactsfeasible, vendorbinaries/weights separate terms, noGithubauthorizednow. No sources/model/session/environment modifications. design/untouched.

## Latest private delivery: v1.5.16-test (1153)

Delivered /sdcard/Download/Termux-Jobs-v1.5.16-test-arm64.apk + README/SHA. Root cause of Qwen cold every tool: progress changes fullsystem, native cache correctly invalidates onfullsystem. Llama only now keeps base system stable; progress/correction are transient trailing appFeedback counted before fitting and protected from history removal. No source transcript mutation/falsecompaction, native scope/system checks unchanged. LiteRT existing path unchanged. Tiny-model integration through production Java request builder+packaged runner reused650/683tokens with changed progress, resets on chat and rights; real Qwen/hybrid latency still pending. CPU fixed/dimmed forbuilt-inqwen35-2b, noGPU picker; oldGPU selection normalized only ineditableEngineSettings, appliedwithsave. Dropdown slightly larger200×52dp/popup216; mainselector/serverbuttonunchanged. Final664app+145emulator+17SDK/fakeinference pass, APK/runtime/signature/secret-scan/DownloadSHA pass. NoGitHub/process/environment/modelchanges. See RELEASE/VALIDATION-v1.5.16-test.md. Next code>=1154. design/ untouched.

## Latest private delivery: v1.5.15-test (1152)

Delivered /sdcard/Download/Termux-Jobs-v1.5.15-test-arm64.apk + README/SHA. User clarified ONLY dropdown rows smaller: about180×48dp, popup196dp, text12sp/vector18dp. Main selector and Server+Agent unchanged. Highlight/model/robot/tap/long-tap retained. Full660 app+145 emulator tests, build/runtime audit/APK audit/Download SHA passed; real DPI/readability pending. No inference/permissions/history changes or GitHub. See RELEASE/VALIDATION-v1.5.15-test.md. Next code>=1153. design/ untouched.

## Latest private delivery: v1.5.14-test (1151)

Delivered /sdcard/Download/Termux-Jobs-v1.5.14-test-arm64.apk + README/SHA. Both engines: separated app progress from base instructions/required correction, shorten only service progress before reducing reply reserve; preserve rights/schemas/current task; appFeedback no longer splits user turns. Capacity failure after completed tools ends clearly with context_budget_stop and saved cards, not false task completion. First oversized request still errors. Dropdown row ovals shorter, active session highlighted, local rows model name + robot vector, uniform dimensions; creation/presets/main selector unchanged. Full660 app+145 emulator and17 real-SDK/fake-inference checks pass, build/runtime audit/APK audit/Download SHA pass. Device/model checks pending. No GitHub, environment/process changes or data reset. See RELEASE/VALIDATION-v1.5.14-test.md. Next code >=1152. design/ untouched.

## Latest private delivery: v1.5.13-test (1150)

Delivered /sdcard/Download/Termux-Jobs-v1.5.13-test-arm64.apk + README/SHA. Both local engines: repeated identical successful write guard independent of changing listings, <=2 writes by default then warning; actual read/final allowed, further duplicate stops. Explicit repeat counts honored; reset on distinct/failed write, command/install, changed fresh target read; cache annotations ignored. After warning repeated same metadata query also bounded, fresh queries/reads allowed. Not universal semantic loop detection. Compact pending approvals, then localized granted/denied/interrupted oval with read-only details. Fixed false Not executed duplicate across approval/UI history records (nearest exact tool/output matching with boundaries). Full650 app+145 emulator and16 SDK/fake-inference cases pass; APK audit+Download SHA pass. Device checks pending. No publication, no model/permissions/output-cap/default changes. See RELEASE/VALIDATION-v1.5.13-test.md. Next code >=1151. design/ untouched.

## Latest private delivery: v1.5.12-test (1149)

Delivered /sdcard/Download/Termux-Jobs-v1.5.12-test-arm64.apk with README/SHA. Gemma leading native fragments classified unparsed_native_tool, hidden in chat, routed through <=2 validation retries only for action requests; fragments never parsed/executed. Native-mode plain JSON without SDK provenance rejected. Native repair messages exclude broken assistant fragments and fake tool responses. Reported напиши/положи action wording recognized. Rights/approval/cancel retained. No sampler/thinking/reply cap/weights changes. Full640 app+145 emulator cases and16 SDK/fake-inference cases pass; final APK audit and Download SHA pass. Actual Gemma behavior still requires developer test of original short request in new chat. See RELEASE/VALIDATION-v1.5.12-test.md. No publication. Next code >=1150. design/ untouched.

## Latest private delivery: v1.5.11-test (1148)

Delivered /sdcard/Download/Termux-Jobs-v1.5.11-test-arm64.apk with SHA/README. Five synthetic LiteRT Native comparisons (system role, write only, all current, greedy, thinking128); max512, no tool dispatch, no chat history, overrides restored. Normal inference unchanged. Found hardcoded disabled thinking/channels, not proven causal. Full630 app +145 emulator cases and15 SDK/fake-inference tests passed; APK audit passed. Device comparison/report/cancel pending. See RELEASE/VALIDATION-v1.5.11-test.md. No publication. Next code >=1149. User design/ remains untouched.

## Latest private delivery: v1.5.10-test (1147)

Delivered /sdcard/Download/Termux-Jobs-v1.5.10-test-arm64.apk and Local-AI-tools-v1.5.10.txt. See RELEASE/VALIDATION-v1.5.10-test.md. All13 registry descriptions explain purpose/contrasts; write saves guides, command executes requested actions, install handles approved dependencies. write.text and command.command include explicit routing descriptions. Shared native schemas and schema-based JSON benefit; legacy JSON text also clarifies guides. No name/type/bounds/availability or authorization change; baseline24 combinations pass. Final90 JVM cases +14 real-SDK/fake-inference cases pass. SDK-parsed descriptions verified. Adds~405 estimated LiteRT input tokens; real-model routing remains pending. Developer clean-chat1.5.9 report4828324737620970227 disproved stale-history-only explanation (91 current tokens, no prior history, wrong installation action). No publication. Next versionCode>=1148.

## Latest private delivery: v1.5.9-test (1146)

Delivered /sdcard/Download/Termux-Jobs-v1.5.9-test-arm64.apk. See RELEASE-v1.5.9-test.md and VALIDATION-v1.5.9-test.md. Shorter default/brief prompts and shared native descriptions/settings; all24 schema configurations retain structure/constraints. Built-in behavior distinguishes saving an instruction from installing it and follows user language. User custom files and Tools only semantics retained. Tool/progress/context cards are rounded left-aligned pills with expandable details. Final99 relevant cases +13 SDK interface cases pass; broad-run evidence/correction documented. Estimated Standard LiteRT2533->2223, Qwen3383->2542; Brief LiteRT savings only20 estimated tokens. No real model speed/obedience claim. Private only, no publication. Next versionCode>=1147.

## Latest private delivery: v1.5.8-test (1145)

Delivered /sdcard/Download/Termux-Jobs-v1.5.8-test-arm64.apk. See RELEASE-v1.5.8-test.md and VALIDATION-v1.5.8-test.md. Bounded untrusted task/target evidence added only after dropping previous turns and only if space remains; optional null history fields omitted; one expandable context notice per user turn. Both local engines, no semantic history search or external-agent changes. Final71 targeted tests pass; broad-run results/corrections documented. ZIP/identity/signature/limited-secret scan and Download SHA verified. No publication. Next versionCode >=1146.

Developer verification of1.5.7: diagnostics4159294689667868535 shows joint video analysis3prepared/3attached/0omitted, followed by final normal chat; developer supplied successful ordinary file read in the same chat. Visual accuracy and cancellation are not established by that sequence.

## Latest private delivery: v1.5.7-test (1144)

Delivered /sdcard/Download/Termux-Jobs-v1.5.7-test-arm64.apk. See RELEASE-v1.5.7-test.md and VALIDATION-v1.5.7-test.md. view_video uses no-tool/no-history sub-analysis, joint then per-frame fallback, restores schemas/scope and returns text observations to normal chat. Only compatible vision LiteRT currently; llama remains text-only. Full615 app tests, final111 targeted, real-SDK13 interface tests passed. Hardware video and normal follow-up/cancel tests need developer verification. No publication. Next versionCode >=1145.

## Latest private delivery: v1.5.6-test (1143)

Delivered /sdcard/Download/Termux-Jobs-v1.5.6-test-arm64.apk. See RELEASE-v1.5.6-test.md and VALIDATION-v1.5.6-test.md. Current-turn progress survives context compaction and native LiteRT prompt replacement. Exact repeated mutation cycles trigger a reminder and then stop if proposed unchanged again. Expandable persistent step card for both local engines. Structural context comparison avoids false shortening notices. 609 app tests and12 real-SDK interface tests passed; hardware/model outcome requires developer check. Not published. Next versionCode >=1144.

## Latest private delivery: v1.5.5-test (1142)

Delivered /sdcard/Download/Termux-Jobs-v1.5.5-test-arm64.apk; see RELEASE-v1.5.5-test.md and VALIDATION-v1.5.5-test.md. Behavior menu updated inside existing settings location; Standard/Brief/Custom plus independent Tools only. Reply reserve adapts, current-chat summaries retain originals, diagnostics include media counts and finish reason. GPU unavailable; pinned LiteRT NPU mode fixed. New optional behavior fields preserve legacy defaults. Private only; no publication. Next versionCode >=1143. Device quality/media/context checks and expanded-context export research remain.

## Latest private delivery: v1.5.4-test (1141)

Delivered /sdcard/Download/Termux-Jobs-v1.5.4-test-arm64.apk. See RELEASE-v1.5.4-test.md and VALIDATION-v1.5.4-test.md. Native tool repair, current-chat recall, styled environment tiles, explicit server connect/disconnect with owned transport closure. Not published. Next versionCode >=1142. Next requested UI: unavailable GPU tile for prior llama model; fixed disabled NPU mode tile for LiteRT. Investigate context instruction/schema cost; hardware image/video/audio checks remain.



## Latest local delivery: v1.4.21-test (1127)
APK delivered to /sdcard/Download/Termux-Jobs-v1.4.21-test-arm64.apk. Manual llama prefill benchmark: lengths512/1024/2048/4096, threads2/4/6/8, batches256/128 vs512/256, forward/reverse; separate temporary contexts and cancellation gate. Normal chat remains4threads. 651 Java tests and tiny-GGUF native benchmark/cancellation/context-preservation checks passed. Final artifact audit: .verification/local-ai-v1.4.21/artifact.json. Real Qwen/device measurements pending; not published. Next build code >=1128.


## Частная 1.6.9-test / code1205
Подключены пакеты шести менеджеров к конкретному proot alias; 29 тестов и сборка
прошли. См. VALIDATION-v1.6.9-test.md. Пользователю передаётся один UPDATE APK.
Далее проверить каталоги/пакетные действия на устройстве, затем бэкапы; особенно
Void после restore без OCI manifest (ещё не исправлен). На GitHub не публиковать
без нового разрешения. Старые rollback1200/1202 ниже текущего versionCode.


## Исправление Alpine — 1.6.10-test/code1206
На скриншотах18:33 APT обнаружен через Termux PATH fallback; реальный Alpine
содержит sbin/apk и не содержит usr/bin/apt-get, usr/bin/dpkg-query. Гостевые
пакетные операции теперь используют Linux PATH в subshell; общий PATH сессий и
агенты не менялись. Повторить пакетные тесты Alpine после обновления APK.
Debian update/install/tree --version подтверждены разработчиком на1.6.9.


## Alpine catalogue — 1.6.11-test/code1207
Developer confirmed apk update and native manager in1.6.10. Offline --available
filter returned empty while plain offline apk list returned repository rows.
Remove that filter for available tab only; refresh/install still require device
verification. No GitHub publication. Next test: Alpine list, install tree, version.


## Fedora DNF5 — 1.6.12-test/code1208
Alpine tree2.3.2 installed/runs, developer confirmed. Fedora makecache succeeds;
repoquery output has literal backslash-t, causing parser rejection. Queryformat
now passes actual TAB for all DNF tabs. No parser relaxation or manager changes.
Next device check: Fedora reread, install tree, tree --version. Remaining managers
and backups still pending; no GitHub authorization.


## Void versions — 1.6.13-test/code1209
Fedora/openSUSE tree2.2.1 installed/runs, developer confirmed. XBPS remote index
contains14204 packages incl13 letter-leading versions rejected by old parser.
New XBPS-only regex requires _revision and accepts letters; entire index checked
read-only in memory. Next device test: Void reread/install/tree --version, then
Arch. Backups and openSUSE initial query/refresh busy remain separate checks.


## Все шесть пакетных менеджеров — проверка разработчиком завершена для install
Void tree2.2.1 installed/runs (19:29). Arch full upgrade completed, service hooks
skipped in nonbooted proot; tree2.1.0 installed/runs (19:38). Together with Debian,
Alpine, Fedora, openSUSE, basic catalogue/refresh/install/run paths checked by
developer. Removal and backup/restore not yet verified. Next: backup stage, notably
Void restore without OCI manifest; do not claim all restores supported or publish.


## Backups — 1.6.14-test/code1210
restore_ubuntu now adds only generated minimal manifest to freshly renamed
rootfs; foreign OCI metadata discarded, rootfs/passwd preserved. Native AST
command_run accepted generated config with explicit sh (mocks, no process).
28 Python backup tests passed including fresh alias/content preservation and
malicious manifest symlink / rootfs write-through rejection. Environment chooser
now separate right Install / disabled gray Installed badges; row selection kept.
Actual backup/restore not run. Next developer check: backup Debian only, inspect,
restore separate jobs alias, tree/files, original still intact; then Void. Do not
publish. Package comparison remains Termux APT only. Stable rollback assets have
lower version codes; don't offer old ones as overlay rollback.


## Restore failure/PAX — 1.6.15-test/code1211
Developer created Debian-only backup /sdcard/Download/Termux-Jobs-20261019-195137-d09fc64b.
Real SHA/size and helper inspect compatible. Restore failed: first115-char APT
path carries pax_headers.path=debian/rootfs/... overriding rewritten jobs name.
Proved via one-member in-memory repack. New code clears pax_headers/sparse and
materializes file data normally. Native invocation still untouched; full real
repack in repository with intercepted native run is verification, not live restore.
Installed partial jobs-6a650d598a1d exists without manifest; don't delete or start
it, installed environment modifications not authorized. Source Debian unchanged.
Next user test: SAME manifest, Linux only, fresh restore. No GitHub publication.


## Device restore confirmations after1.6.15
Debian restored jobs-5b9572bd2e44: developer terminal Debian13/trixie13.7,
tree2.2.1, jobs-env-test.txt, file manager confirmed. Void restored
jobs-2fadee2c8257 (source void via RESTORE-RESULT), developer confirms startup,
commands/test file/file manager after requested checks. Do not claim all other
restore modes verified. Remaining: original environments recheck, other four
full restores if needed, selective-data flows, process-close/reopen. No release
authorization. Partial jobs-6a650d598a1d remains; don't modify installed state.


## Alpine backup arch — 1.6.16-test/code1212
Developer confirms both restored presets and original Debian/Void restart.
Alpine backup /sdcard/Download/Termux-Jobs-20261019-201520-e38b386d is blocked
because only regular ELF is bin/busybox (machine183); sh/env are symlinks to it.
Add busybox to bounded ELF scan + primary paths. Real helper inspect now marks
alpine/aarch64 compatible. New tests enforce wrong/unknown arch still blocked.
No actual Alpine restore done by assistant. User should re-open same manifest
in updated app, select Linux only and restore, then verify preset/tree/files.


## Alpine restored — developer confirmation
After1.6.16 developer confirms full Alpine restore, os-release/tree/files check,
file manager, close/reopen. Debian/Void/Alpine cycles confirmed. Next full restore:
Fedora, then openSUSE and Arch. No GitHub authorization; don't delete installed
partial jobs-6a650d598a1d or alter user environments.


## Arch archive — 1.6.17-test/code1213
Developer confirmed Fedora full restore works like previous. Arch creation
failed unsafe path in native TAR. Existing incomplete folder in Download:
Termux-Jobs-20261019-202550-abf32c48, ubuntu-archlinux.tar.gz374164991 bytes,
no manifest. Three ordinary systemd filenames contain literal backslash-xHH;
old clean rejected all backslashes. Allow only literal hex sequences without
decoding; arbitrary backslash/traversal still blocked. Full scan now37022 entries,
aarch64. Recreate Arch backup after updating to confirm complete creation, then
restore/test. openSUSE restore not confirmed yet. Do not clear installed partial
jobs-6a650d598a1d or any existing incomplete archives without authorization.


## Restored Arch .l2s — 1.6.18-test/code1214
Developer confirms Arch restore/file/commands/restart. Warning .l2s seen in
jobs-72e1416e65be. Read-only actual root0555 and absent .l2s; original Arch0555
has it. Native login makedirs_at catches OSError and safely unsets PROOT_L2S_DIR,
falling back to adjacent intermediates. Likely app-UID write restriction; exact
errno not captured, assistant/root fixture can mkdir despite0555. New repack
adds absent .l2s0700 before native restore applies root modes; rejects file/link,
keeps existing directory unduplicated. No live filesystem edits. Need user repeat
restore same Arch backup to new copy, confirm no warning. openSUSE restore still
not explicitly confirmed. Do not publish or delete any copies without authorization.


## All six full environment restore cycles confirmed by developer
After1.6.18 developer confirmed Arch restored launch WITHOUT .l2s warning.
Then confirmed openSUSE full cycle. Debian/Alpine/Fedora/openSUSE/Void/Arch Linux
ARM now have developer-reported backup, separate restore, tool/test-file/file
manager and reopen verification. Do not infer exhaustive selective/foreign/cross-
device/live-database coverage. No GitHub publication authorized. Latest delivered
APK1.6.18-test/code1214; metadata audit in .verification/backups-environments-v1.6.18.
