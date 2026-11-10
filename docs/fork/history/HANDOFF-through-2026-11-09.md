# История основного CONTINUE до 2026-11-09

Исторический архив, зафиксирован 2026-11-09 при реорганизации документации.
Исходные формулировки «текущий», «активно», «пауза», разрешения и запреты ниже
относятся к своим датам и задачам. Они не задают текущий статус и не разрешают
новые действия. Это сохранённые свидетельства, а не вторая память проекта.
Текущая цель: [основной CONTINUE](../../../CONTINUE.md).
Правила: [AGENTS](../../../AGENTS.md). Память: `/root/termux-fork/LEARNING.md`.

Сохранены все исходные записи, включая непроверенные сценарии и отказы.
Архивация не закрывает вопросы и не означает повторной проверки результатов.
Относительные Markdown-ссылки перенесены с сохранением прежних целей. Пути
в кодовых фрагментах имеют прежнюю базу: `/root/termux-fork/.verification/rollback-live-actions/source`.

## Указатель исходных записей

- [Active: X11 notification cleanup — 2026-11-09](#record-001)
- [Release reproduction check — 2026-11-08](#record-002)
- [Published stable 2.1.7 /1536 — complete](#record-003)
- [Active: private2.1.7-test2 /1535 — RTK receipt matching](#record-004)
- [Активно: частный 2.1.7-test1 /1534, проверка телефона ожидается](#record-005)
- [Опубликовано: стабильная 2.1.6 /1533](#record-006)
- [Активно: релиз 2.1.6 /1533, проверки завершены — публикация](#record-007)
- [ПАУЗА — пользователь меняет модель](#record-008)
- [Текущая публикация — stable2.1.6 /1532](#record-009)
- [Готов test10 /1531 — RTK из Components и чёрная выбранная строка](#record-010)
- [Исторические записи ниже — не текущие инструкции или разрешения](#record-011)
- [Готов test9 /1530 — RTK jobs.1, ожидается проверка пользователя](#record-012)
- [Актуальная задача RTK — читать LATEST.txt](#record-013)
- [Важное текущее состояние RTK — private test9 /1530 (не выдан)](#record-014)
- [Текущая задача — RTK 1.1.0-jobs.1, точные измерения и компонент](#record-015)
- [Текущая задача — краткий блок RTK, test8 /1529](#record-016)
- [Текущая задача — компактные кнопки диалогов RTK](#record-017)
- [Текущая задача — test6 /1527, статистика RTK и пульсация Стоп](#record-018)
- [Текущая задача — test5 /1526, заливка меню и появление карточек](#record-019)
- [Текущая задача — карточки меню, облачка, анимации и ручной индикатор](#record-020)
- [Текущая задача — тестовый APK 2.1.6-test3 /1524](#record-021)
- [Текущая задача — расширение частичного RTK, test2 /1523](#record-022)
- [Текущая выдача — RTK Mode, 2.1.6-test1 /1522](#record-023)
- [Опубликован стабильный v2.1.5 /1520 и ручной rollback /1521](#record-024)
- [Текущая выдача — test24 /1519](#record-025)
- [Текущая выдача — test23 /1518: режимы Claude](#record-026)
- [Текущая выдача — test22 /1517](#record-027)
- [Текущая выдача — основной1516/test21](#record-028)
- [Текущая выдача — основной1515/test20](#record-029)
- [Тяжёлый PNG в активном fake-Claude](#record-030)
- [Уточнение запуска fake-Claude](#record-031)
- [Дополнение — готов fake-Claude для проверки PNG](#record-032)
- [Текущая выдача — основной1514/test19](#record-033)
- [Текущая выдача — основной1513/test18](#record-034)
- [Предыдущая выдача — основной1512/test17](#record-035)
- [Предыдущая выдача — основной1511/test16](#record-036)
- [Предыдущая выдача — основной1510/test15](#record-037)
- [Предыдущая выдача — основной1509/test14](#record-038)
- [Предыдущая выдача — основной1508/test13](#record-039)
- [Предыдущая выдача — основной1507/test12](#record-040)
- [Текущая выдача — основной1506/test11](#record-041)
- [Предыдущая выдача — основной1505/test10](#record-042)
- [Предыдущая выдача — основной1504/test9](#record-043)
- [Предыдущая выдача — основной1503/test8](#record-044)
- [Предыдущая выдача — основной1502/test7](#record-045)
- [Предыдущая выдача — основной1501/test6](#record-046)
- [Предыдущая выдача — основной1500](#record-047)
- [Историческая выдача — основной1498 и резерв1499 сохранены](#record-048)
- [Предыдущая выдача1496/1497 (сохранена)](#record-049)
- [Текущая работа — основной1496 готов к проверке телефона, резерв1497 сохранён](#record-050)
- [Предыдущая выдача (историческая запись, визуально отклонена)](#record-051)
- [Текущая работа — тестовый v35 /1494 готов к проверке телефона](#record-052)
- [Историческое состояние до v35 (не текущая ветка/цель)](#record-053)
- [Резерв1495 сохранён в Download; следующий основной планируется1494](#record-054)
- [Приватный 2.1.5-test1 /1493 принят в проверенных пользователем сценариях](#record-055)
- [Текущий результат — опубликован стабильный v2.1.4 /1492](#record-056)
- [Приостановленное исследование — накопление вложений и обрывы Codex](#record-057)
- [Контрольная точка — stable v2.1.3 / 1485](#record-058)
- [Согласованный состав 2.1.4-test1](#record-059)

---

<a id="record-001"></a>

# Active: X11 notification cleanup — 2026-11-09

Private APK 2.1.8-test1 /1537 is ready in Download, not installed or published. See
`docs/fork/VALIDATION-X11-NOTIFICATIONS.md` and `.verification/x11-notification-audit/`.
Read-only Android observation found Arch foreground service PID7631, notification8086,
zero clients. This is not a notification without a service. Test ownership/start-time
identity could not be established; no live service/process was stopped.
Wording changed to Рабочий стол / Desktop and Termux Job’s. Idle control-only
service cleanup added with exact private-process guard; running desktops retained.
Modern Lint passed all5modules (0errors,290warnings); 63 targeted tests passed (0failures/errors/skips).
Phone acceptance/recreation and existing-service cleanup remain unverified.
APK SHA256 e403a25184b5f2d778415c62235317425f92f171357a609acfbf9d6b2b5d6e74.
Final ZIP/signature/package/version/ABI/DEX/assets checks passed; Download copy matches.
Await phone acceptance; no installation or publication performed.

---

<a id="record-002"></a>

# Release reproduction check — 2026-11-08

v2.1.7 source/payload verification complete locally: all1140 ZIP entries match;
unsigned container exactly equals the published APK after removing only its8192B
signing block and fixing the central-directory offset. Own4native libraries rebuilt;
glycin requires historical GCC recipe, not tag default Clang recipe. External3libs
not rebuilt. No signing key copied, no APK installed, no release/assets changed.
Report/procedure: docs/fork/REPRODUCIBILITY-v2.1.7.md.
Evidence: .verification/release-reproduction-v2.1.7.
Private vulnerability reporting enabled; APIreadback true. SECURITY retains original
2026-11-04 audit scope. Publication of report/procedure and SECURITY authorized.

---

<a id="record-003"></a>

# Published stable 2.1.7 /1536 — complete

Release: https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.7
Tag commit: f3f57668fca31302062bf46f3be6a20d885ede1d; release ID 438213343, latest stable, no prerelease/rollback.
APK SHA256 f321c19652d80dd91cf96a46e4fcefffd1f309faebb61e3cc64ab4f0fe5bbebb.
Public ordinary-URL downloads of all4assets match; downloaded signature/package/ABI,
metadata and5taggedsource/docfiles verified. Old38releases/145assets preserved.
CI42476694520 on997ceb4d PASS:1940passed/9skips; firstCI42475835531 stale test
expectation failed, corrected onlytest. Signed functionalcode matches phone-accepted
2.1.7-test2. LocalModernLint0errors/290warnings;16updater and22regression testsPASS.
No Components publication, no auto-install, no user chat/environment stop/delete.
Evidence .verification/release-v2.1.7; docs/fork/VALIDATION-v2.1.7.md.
Do not move tag or replace released APK/update.json. Future changes need nextrelease.

---

<a id="record-004"></a>

# Active: private2.1.7-test2 /1535 — RTK receipt matching

User authorized fixes, live checks and test APK; app release waits phone confirmation.
Observer patched;61testsPASS. Live current Codex chat: search120480→14612B,
head225→225, sequence1350→1350, yielded sleep/cat1125→1125, recall+800.
Pipeline remains unknown. Binary jobs.1 unchanged; do not republish component.
Temporary profile-scoped installed bridge is no longer active: current helper hash
matches original backup. Old event records preserved. No sessions restarted.
Final APK and Download copy verified. ModernLint0errors/290warnings,505finalJava/XML
inputs match;638APKsource files match;all7ELFsARM64;same signer/com.termux.
APK /sdcard/Download/Termux-Jobs-v2.1.7-test2-arm64.apk
SHA256 39b13ee632b1d326657d2a7f9a58f4c501f87c18f19956bd4fe7553d1d13909c
Claude2.1.296 SDK synthetic provider: search23781→1526, sequence17294→17294,
other RTK-off profile unaffected. Not a cloud-Claude test.
Evidence .verification/rtk-current-audit. Await phone verification. No push/release/install.
Docs: docs/fork/VALIDATION-2.1.7-test2.md. Original216→242 diagnosis in same evidence.

---

<a id="record-005"></a>

# Активно: частный 2.1.7-test1 /1534, проверка телефона ожидается

После2.1.6 пользователь сообщил остаточный run/termux-jobs-wayland mode000 и
неработающий restart при потере связи. Исправления в текущем checkout; публикация
запрещена до проверки пользователем. Рабочие среды/чаты не удалять и не останавливать.
Диагноз/границы: docs/fork/VALIDATION-2.1.7-test1.md; evidence .verification/fix-1534.
Native Arch fixture: active bind blocked; normal exit removes placeholder;
residual seeded from observed state, old1533fails13, new cancel/remove/neighboursPASS.
Native helper21passed/1hardlinkskip; local22pass. ModernLint0errors/290warnings.
APK completed: /sdcard/Download/Termux-Jobs-v2.1.7-test1-arm64.apk, 40673809 bytes.
SHA256 74bc0096420fdcbf5f52f65a5f1c128da1b9aa159473f0021ebf4dd421b50564.
Final APK gate PASS;40JVM passed; native launcher fixture PASS; locale PASS.
No install/push/release. Await user's phone verification; do not claim GUI acceptance.

---

<a id="record-006"></a>

# Опубликовано: стабильная 2.1.6 /1533

https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.6
Tag ->217310be741108a369dda62bccaa5ad2927143b8; CI42470194304 success.
APK SHA256 3ada4628499f765bbd3842b3bf96cc8686ded68a2aa0d6801c97e5ff74183f6c.
Published four assets, no rollback/buildheaders. Old37releases/141assets preserved.
Public unsigned API downloads match all prepared bytes. Downloaded APK signature/
package/code/ABI pass. Direct SHA URL works; direct APK/update/validation URLs504
on this route, including native Termux update.json check. Updater parser accepts
metadata; actual updater network download is NOT confirmed. Do not claim it passed.
No automatic APK install. Full scope: docs/fork/VALIDATION-v2.1.6.md publication section.
Draft temporary tag association corrected to existing v2.1.6; final latest/tag checked.
No tags or binaries to replace. Any later docs commit does not move release tag.

---

<a id="record-007"></a>

# Активно: релиз 2.1.6 /1533, проверки завершены — публикация

Пользователь разрешил исправления Arch/загрузок и публикацию после успешных проверок.
Финальный APK собран, Modern Lint 0 errors/290 warnings, final APK gate PASS.
APK /sdcard/Download/Termux-Jobs-v2.1.6-arm64.apk, 40673325 bytes,
SHA256 3ada4628499f765bbd3842b3bf96cc8686ded68a2aa0d6801c97e5ff74183f6c.
Не установлен. App draft ID438132321 ещё не опубликован; тег v2.1.6 ещё не создан.

Arch SCAN_ACCESS_DENIED/13 воспроизведён helper из опубликованного v2.1.5 APK:
это прежний дефект; Ubuntu — hardcoded текст, alias archlinux передаётся правильно.
Native Termux через same-UID app TermuxService: UID10620, TracerPid0, mode000.
Узкий fix только точный sealed systemd path; unscanned warning, rmdir только пустого,
nonempty восстанавливает alias. Другие отказы не скрыты. Native tests18 pass/1 skip
(запрет Android hardlinks), Linux/PRoot19 pass. Real Arch sealed reproduction PASS.
Все10окружений: install/shell/cancel/remove/neighbour PASS в isolated-prefix.
Рабочие среды/чаты не изменены. Телефонный GUI финальных диалогов не проверен.
Downloads всех10: production fallback + API size/SHA PASS; direct-only8 вернули504,
Debian/Alpine direct PASS. Cancel+disconnect live Alpine stream/resume206/SHA PASS.
39 image tests (есть inherited duplicates),11 native/local supervisor,5 prep tests PASS.
19 final JVM/updater cases PASS; final update.json generated with1533/SHA.
635/638 production files unchanged vs test10; only environment-image.py,
agent-environment.py,AgentEnvironmentManager.java changed. 95nativeinputs match.

CI source 050c7a7 passed env tests but Gradle original URL timeout (attempts1/2).
Source75d8329c adds verified Gradle7.2-all cache seed from official public API,
SHA a8da5b02437a60819cad23e10fc7e9cf32bcb57029d9cb277e26eeff76ce014b.
Run42469611398 passed Gradle download but failed bootstrap primaryURL504.
Source d22ab5d0f7cffda5b78f852860c3e67701924370 seeds exact original bootstrap
ZIPs through public API; checked against app/build.gradle hashes.
Run42470194304 SUCCESS. 1943cases:1934passed/9skipped/0errors.
8SFTP fixture skips,1updater env skip (final local updater12passed).
Debug APK assembly PASS. No APK production change.
Prior cancellation timer assertion was ambiguous: corrected barrier proves no
writes after cancellation returns; old timing wrote0.08s after request, before4.1s return.
Production supervisor unchanged (matchesv2.1.5). Preserve old failures.

Gh credential wrapper .verification/release-v2.1.6/gh.py works; always explicit -R.
Git push needs ephemeral GH_TOKEN + git -c credential.helper= -c
'credential.helper=!gh auth git-credential' (capture credential in memory, never print).
Do not use short source_ref for actions/checkout; use full40char SHA.
Build headers remain only Components, published x11-build-headers-musl-1.2.6-r2;
inner APK exactly previous ca72..., outer bb06...; API asset723443781.
RTK jobs.1 remains published, loader unchanged, no embedded archive.

Next: CI reports downloaded/validated, validation.json generated.
Finalize publication snapshot and link checks,
commit docs only, prove source/artifact/CI identity, tag/push, uploadAPK/SHA/update/
validation to draft438132321, publish latest stable, public downloads/checksums/
updater/old-assets preservation. No rollbackAPK, no dependencyarchive appasset.
Do not repeat already passed native/fullLint/build without meaningful source changes.
Main evidence .verification/release-v2.1.6 and environment-audit subdirectory.
Earlier paused/1532 notes below are historical, not current instructions.

---

<a id="record-008"></a>

# ПАУЗА — пользователь меняет модель

Не продолжать без возобновления. Точное состояние и следующие шаги:
`.verification/release-v2.1.6/PAUSED.txt`.
Релиз приложения не опубликован; локальный тест загрузки остановлен.
В исходниках начато исправление резервной загрузки Components и code1533; APK1533 ещё не собран.

---

<a id="record-009"></a>

# Текущая публикация — stable2.1.6 /1532

Пользователь принял test10 /1531 на телефоне; позднее разрешил только metadata-only
пересборку в2.1.6 /1532 для совместимости WorkspaceUpdates. Функциональные638файлов
совпадают,135assets/nativeentries совпали с test10. Новый APK SHA
060a12de8e1d8d15f60dd7acac4617e5535bd730f5a68efbcaa34878db650ad7,40671985B.
Артефакты .verification/release-v2.1.6/. ModernLint0errors/290warnings PASS;
finalAPKgate PASS; targetedactualmanifest updater PASS. FullJVM initial обнаружил
устаревшие4ожидания вUi1504/AgentContextMeter/WorkspaceDocumentation; исправлены толькотесты.
User explicitly asks CItoo: сейчасготовится commit/push наwork/redesign-v35-1494,
workflow_dispatch сsource_ref черезfork/base. ПослеCI/локальныхпроверок финализировать
доки/commit/tagv2.1.6, publishlatestAPK+SHA+update+validation. БезrollbackAPK/auto-install;
старые releases/APK ирезерв1499 сохранить. RTKjobs.1 ужеопубликован, SHAпроверен,
встроенногоархиванет. Исторические записи ниже не текущие инструкции.

---

<a id="record-010"></a>

# Готов test10 /1531 — RTK из Components и чёрная выбранная строка

Выдан /sdcard/Download/Termux-Jobs-v2.1.6-test10-arm64.apk,40671881B.
SHAfbea07c7149fa0fb89995e74bdcd0b34dd5da918108e22c48fbef684d66a33e6.
Отчёт docs/fork/VALIDATION-2.1.6-test10.md; доказательства .verification/rtk-remote/.
Компонент опубликован с разрешения пользователя (commitc882d44):
https://github.com/HezeAI/termux-jobs-components/releases/tag/rtk-1.1.0-jobs.1
Публичная загрузка/установка/SHA/runtime проверены;8старых releases/132assets сохранены.
В APK нет встроенного архива/частной ветки; revisionjobs.1 и SHA отличают upstream1.1.0.
25Python installation/state/upgrade,15statistics,3Lite families,2real-binary integrations PASS;
19JVM, ModernLint0errors/290warnings, finalAPK gate/copySHA PASS.
Lite capturedstdout: grep160893→1308,head470→470,recall+525,нетdoublecount.
Native ls4410→1056,head25→25,recall+600;изоляция/циклon-off-on PASS.
Selected sidebar fill и chat background используют общий workspace_chat_background (#000000).
Настройки/счётчики/recall сохраняются; старый бинарник не заменяется во время команды.
На телефоне ещё проверить установку/RTKupdate/перезапуск выбранного чата/чёрную заливку.
APK не устанавливался автоматически, apprelease/update.json не публиковались/не менялись.
Публикацию самого приложения выполнять только после проверки пользователя.

---

<a id="record-011"></a>

# Исторические записи ниже — не текущие инструкции или разрешения


<a id="record-012"></a>

# Готов test9 /1530 — RTK jobs.1, ожидается проверка пользователя

Пользователь сообщил об обновлении. В текущем рабочем чате проверены runtime1.1.0-jobs.1
и modeadvanced. Живые команды: grep61892→1730B; head5 310→310B; recall+4109B/1call.
Доказательство .verification/rtk-user-check/result.json; это проверка рабочего чата,
а не fake provider. Визуальное состояние окна на телефоне помощник не проверял.

Выдан /sdcard/Download/Termux-Jobs-v2.1.6-test9-arm64.apk.
SHAe43ba011e1a63cd910a8d57be92f0c4a987a608535f762a88a2a4f09ada3fc6d.
Итоговый отчёт docs/fork/VALIDATION-2.1.6-test9.md; артефакты .verification/rtk-native-bytes/.
Нативный компонент upstream1.1.0/revisionjobs.1, binarySHA3b8c5b69791e86222db36b104bce10da2975b92adaaa86f82e1c893ae3035619.
Точные LS/Git/search/read/head/tail байты, recall отдельно; unknown для неподдержанных/обрезанных CLI результатов.
Native tests12cases, реальные CLI с fake provider12пар+untrusted, Python15/34/23/3,
JVM12 и Modern Lint0errors/290warnings PASS. Final APK gate и copy checksum PASS.
Benchmark final: read3430000B official45.22ms/custom_off44.32/custom_on66.93 (+22.61ms).
Проверены симлинк/descriptor ownership и многозахватный отказ, SHA variants5case.
Старый unsafe кандидат не выдавался; rejected-sidecar/ и pre-* APK сохранены.

Только тестовый APK содержит архив .bin: APK46267932B, +5598123B к test8.
Финальный вариант должен скачивать из существующего HezeAI/termux-jobs-components.
URL/версия/ревизия/хеши загрузчика готовы; публикация и реальное публичное скачивание НЕ выполнены.
Компонентный repo clone: .verification/rtk-native-bytes/components, только README/catalog/newset изменены.
Публикация после предоставленного отчёта/подтверждения; ничего не push/tag/release/install автоматически.
Следующие шаги: проверка test9 на телефоне, подтверждение публикации компонента,
commit/tag/Release binary+sources+SHA/patch/licenses, публичный download/hash check,
убрать private-test канал и встроенный .bin из финального APK, проверить реальную загрузку.
Рабочие чаты/авторизация/установленный RTK не менялись; старые режимы/статистика/recall сохраняются.

---

<a id="record-013"></a>

# Актуальная задача RTK — читать LATEST.txt

Текущие цель, разрешения, состояние сборок и все следующие шаги подробно:
`.verification/rtk-native-bytes/LATEST.txt` (новее PLAN.txt и старых записей ниже).
Последний запрос: расширить точные байты Git/search/read и измерить задержку
upstream vs custom на больших данных. Rust build-release-candidate.log (session27620)
после финальных расширений/notice-комментариев; benchmark.py запускать после сборки.
Тестовый APK test9/1530 ещё НЕ выдан. Пересобрать с финальным компонентом и новой SHA.
Публикация/установка/рабочие перезапуски не выполнялись и пока не разрешены.

---

<a id="record-014"></a>

# Важное текущее состояние RTK — private test9 /1530 (не выдан)

Публикация только после отчёта/подтверждения; использовать HezeAI/termux-jobs-components.
Обнаружен дефект изолированного измерителя: в PRoot O_NOFOLLOW не защитил synthetic symlink.
Пользовательских файлов не касалось. Старый кандидат НЕ выдавать; архивы в rejected-sidecar/,
APK .pre-symlink сохранён. Новый Rust измеритель: create_new + held descriptor, сейчас build-verified.log
(session66107). Все актуальные пути/проверки/следующие шаги — .verification/rtk-native-bytes/PLAN.txt,
обязательно читать перед продолжением. Нужна повторная проверка бинарника/обновления/моделей/APK.
Никаких установок/публикаций/рабочих перезапусков не было.

---

<a id="record-015"></a>

# Текущая задача — RTK 1.1.0-jobs.1, точные измерения и компонент

Пользователь разрешил собственную сборку и измерение внутри RTK без повторного запуска.
Последнее: использовать существующий HezeAI/termux-jobs-components; сначала проверить
структуру, подготовить отдельный versioned component и показать результаты ДО публикации.
Публикацию пока не выполнять. Старые компоненты/работающие чаты/авторизацию не менять.
Текущий подробный план: .verification/rtk-native-bytes/PLAN.txt.
Сборка Rust: .verification/rtk-native-bytes/build-local.sh, build.log; session98503.
Исходники + патч внутри .verification/rtk-native-bytes/rtk-753a3b0e939f2e4490fc24e3a1984010f40a103c.
App backend экспериментально изменён (ещё не APK): sidecar measurements → matching post tool.
UI date/legacy скрыты, данные не удалены. JVM/Lint до backend-изменений PASS.
Следующее: закончить сборку/достоверные тесты, installer/versioned upgrade+preservation,
сохранить исходники/патч/лицензии/build recipe в components clone sets/rtk-1.1.0-jobs.1.

---

<a id="record-016"></a>

# Текущая задача — краткий блок RTK, test8 /1529

Только RtkMode/RtkStatistics UI: состояние, summary выбранного режима,
короткое описание; байты/recall/оба режима/legacy и различия в Details (default GONE).
Оценочные токены bytes/4 только для отображения; unknown/partial не дают процент.
Backend/счётчики не изменены. Предупреждение штатного RTK сохранено.
Готов /sdcard/Download/Termux-Jobs-v2.1.6-test8-arm64.apk /1529.
7 JVM PASS, Modern Lint0/290, final APK gate PASS.
Отчёт docs/fork/VALIDATION-2.1.6-test8.md; .verification/rtk-summary.
На телефоне не проверено; не install/publish.

---

<a id="record-017"></a>

# Текущая задача — компактные кнопки диалогов RTK

RtkMode.dialogButton применяется к action/cancel всех этапов одного диалога.
WRAP_CONTENT, CENTER_HORIZONTAL, padding16/8dp, minHeight48dp, minWidth0,
multiline без ellipsize; progress MATCH_PARENT сохранён. Backend не изменён.
Проверки: .verification/rtk-compact-buttons/REPORT.txt; 4 JVM PASS, Modern Lint 0/290.
На телефоне перенос текста не проверен; Robolectric не подтверждает реальные font metrics.
Готов 2.1.6-test7 /1528: /sdcard/Download/Termux-Jobs-v2.1.6-test7-arm64.apk.
Final APK gate PASS; отчёт docs/fork/VALIDATION-2.1.6-test7.md. Не install/publish.

---

<a id="record-018"></a>

# Текущая задача — test6 /1527, статистика RTK и пульсация Стоп

Реализован statistics-v2: captured Lite stdout + сопоставленный PostToolUse,
stock native base unknown кроме доказанного byte-exact head/tail defaultnone.
Claude trimEnd сопоставляется по хешу и применяется к обеим сторонам.
Recall измеряется на выходе tool result; bytes подтверждены captured provider body.
Старые measurements/history.db сохранены, legacy snapshot отдельно; новыйepoch.
Новый scoped PostToolUse требует следующего запуска; текущие чаты не перезапускать.
Stop compact gray outline использует общий clock, cosine2500ms, красный квадрат прежний.
8 nativeCLI scenarios/12 command results passed с synthetic local provider+guard,
10 stats units,34 state/install units,3 filter-family tests и Claude unit прошли.
38 JVM tests PASS; final Stop/clock recheck PASS; full Modern Lint 0 errors/290 warnings.
Final signed APK gate PASS; .verification/rtk-statistics. Не install/publish.
APK: /sdcard/Download/Termux-Jobs-v2.1.6-test6-arm64.apk.
Отчёт: docs/fork/VALIDATION-2.1.6-test6.md. Следующий шаг — проверка на телефоне.
Test5 APK уже сохранён отдельно; телефонная проверка обеих сборок не выполнена.

---

<a id="record-019"></a>

# Текущая задача — test5 /1526, заливка меню и появление карточек

Заливка Section теперь WorkspaceDialogs.BG, как drawer; оформление и prefs сохранены.
Новая задача пользователя: не обрывать появление при быстрых обновлениях/миниатюрах.
Перестановка retained rows через detach/attach внутри родителя, без window detach.
Общий AgentCardSize для timeline, вложений и отдельных действий; не меняет arrival.
Расширен список появляющихся карточек, pivot сверху для меняющейся высоты.
Test5 готов:58 JVM PASS, Modern Lint0/290, финальный APK gate PASS.
APK /sdcard/Download/Termux-Jobs-v2.1.6-test5-arm64.apk. Доказательства .verification/chat-card-arrival.
Не устанавливать и не публиковать. Телефонная визуальная проверка ещё не проведена.

---

<a id="record-020"></a>

# Текущая задача — карточки меню, облачка, анимации и ручной индикатор

Следующий test4/1525 после выданного test3/1524. Макет: /root/artifacts/sidebar-sections-v2/sidebar-sections.html, вариант cards.
Четыре раздела с persisted user expansion; active section override только при открытии,
без перезаписи выбора, background sessions не active. Manual collapse переживает refresh.
Облачка user wrap-content END max85%, сохранить delivery/attachments/selection.
520ms мягкое появление, отдельная высота timeline без повторного появления.
Индикатор только touch/wheel/inertia; programmatic/layout/reopen не показывают.
Тестовый APK1525 готов и скопирован в /sdcard/Download/Termux-Jobs-v2.1.6-test4-arm64.apk.
137 целевых JVM проверок прошли с focused rechecks; Modern Lint0/289; final APK gate прошёл.
Ожидается ручная визуальная проверка телефона; отчёт docs/fork/VALIDATION-2.1.6-test4.md.
Доказательства .verification/sidebar-cards. No install/publish. Phone checks не выполнены.

---

<a id="record-021"></a>

# Текущая задача — тестовый APK 2.1.6-test3 /1524

Реализованы offline Files/attachments/Git и Limits/context для сохранённого чата.
Нет запуска агента при открытии. Native readonly journal projection + cache refs.
Последние лимиты/контекст сохраняются с временем; compaction disabled offline.
RTK Lite/basic и RTK штатный/advanced взаимоисключающие; offline preparation сохраняет
выбор до запуска. Native hooks делегируют OSS1.1.0, recovery включён, per-chat DB/config.
Предупреждения Codex/Claude сохранены; permission UI этой задачей не меняется.
Для старого процесса без chat-bin dispatcher требуется явное согласие на restart.
Рабочие чаты/авторизация/правила не менялись. No install/publish.

JVM и Modern Lint прошли; Python state/install/cancel/deferred и readonly histories прошли.
Production native paired tests:4 сценария/24 хода + Codex untrusted approval check.
Это CLI с фиктивным провайдером, НЕ настоящая модель. Телефон ещё не проверен.
Доказательства .verification/rtk-offline; отчёт docs/fork/VALIDATION-2.1.6-test3.md.
Финальный APK gate прошёл после Gemini short-ID fix. APK скопирован в /sdcard/Download/Termux-Jobs-v2.1.6-test3-arm64.apk; SHA в .verification/rtk-offline/artifact.json. Ожидается ручная проверка телефона; установка/публикация не выполнялись.
Предыдущие APK1523 и резерв1499 сохраняются.

---

<a id="record-022"></a>

# Текущая задача — расширение частичного RTK, test2 /1523

Пользователь подтвердил работу первого этапа в текущем чате и потребовал расширить
Git-only покрытие. Добавлены literal поиск/чтение/тесты и Gradle stdout preview.
Это не полный RTK и не dedicated Gradle parser. Перенаправления/compound не меняются.
Сохранены исходный argv, stderr, pipefail, prefix rules. Существующий установленный
helper обновляется атомарно при открытии настройки; режимы/статистика не сбрасываются.

Проверены Python coverage/state и native Claude/Codex с synthetic provider.
Gradle под guard не запускает daemon (нет usable local IP), поэтому отдельно
реальный локальный Gradle и replay его stdout через native CLI. Не смешивать проверки.
Тестовая сборка1523 готова; финальный APK gate, Modern Lint (0/287), Java3, Python26+3 прошли. Публикация/установка не выполнялись. Следующий шаг — телефонная проверка новой матрицы и обновления установленного helper.
Доказательства .verification/rtk-expand; отчёт docs/fork/VALIDATION-2.1.6-test2.md.

---

<a id="record-023"></a>

# Текущая выдача — RTK Mode, 2.1.6-test1 /1522

Тестовый APK собран и проверен; установка, публикация и рабочие перезапуски не выполнялись.
Пользователь разрешил только реализацию, изолированные проверки и тестовую сборку.
RTK OSS1.1.0: одна установка в Ubuntu AI, независимые настройки/статистика чатов.
Начальное покрытие — простые literal git status/diff/log. Claude SDK и Codex hooks
передают сокращённый stdout до провайдера; stderr остаётся обычным путём.

Последнее требование пользователя заменило прежний opt-in обхода: финальный pipeline
без 2>&1 сохранил проверенные prefix rules, предупреждение об обходе убрано.
Правила, approval и sandbox не меняются. Native CLI + synthetic provider проверили
запреты, untrusted decline/accept и цикл включить/выключить/включить. Это не модель.
Read-only/workspace-write пока не подтверждены: bubblewrap mount error стенда.

TerminalModeRow сокращает подпись по фактической ширине без уменьшения шрифта.
В закрытии терминала убрана галочка; сохранён прежний default сохранения истории.
Кнопка — «Сохранить и закрыть», рабочие процессы не сохраняет.

Финальный Modern Lint: 0 ошибок /287 предупреждений, все пять модулей.
Java51 и повторные3 RTK после lifecycle fix прошли; Python26 и Node-регрессии прошли.
Финальный APK gate прошёл; точные SHA и ограничения в
[VALIDATION-2.1.6-test1.md](../VALIDATION-2.1.6-test1.md).
Доказательства .verification/rtk-mode. Следующий шаг — ручная проверка телефона
и настоящих моделей; не выдавать fixture за такие проверки. Никаких автоустановок.

---

<a id="record-024"></a>

# Опубликован стабильный v2.1.5 /1520 и ручной rollback /1521

Релиз: https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.5
Тег основного исходника: 61e40cef03d0a748f4ccb3c756e1937c168538c3.
Тег v2.1.5-rollback: 2352ee808ad23203e92195be60a00e1d3c82b5c5.
Обновлятор направлен только на новый интерфейс1520. Rollback1521 доступен вручную;
перед ним выключить Auto/Bypass во всех сохранённых профилях Claude. Установка,
откат и сохранность данных на устройстве не проверены. Резерв1499 не тронут.
Основной CI42387377279: 1909 прошли /8 пропущены; rollback CI42385931862:
1774 /8. Локальные102, включая8SFTP, прошли. Полный Modern Lint обоих деревьев:
0 ошибок, предупреждения281/250. Подпись, ZIP, runtime и SHA обоих APK проверены.
Все8 публичных assets скачаны анонимно и совпали с локальными файлами; подписи,
версии, latest/update.json, теги и ссылки проверены. Старые stable assets и8
Components releases неизменны. Первоначальная проверка скачивания остановилась
из-за старого литерала версии в проверяющем скрипте; после исправления сравнения
те же скачанные файлы прошли. Это не ошибка APK/метаданных. Журнал сохранён.
Отчёт docs/fork/VALIDATION-v2.1.5.md; доказательства .verification/release-v2.1.5.
Подтверждён отчёт HezeAI: issue55730#issuecomment-6645086819. WebSocket не исправлен;
реальные решения Auto и ограничения телефонных сценариев остаются непроверенными.
Установки и рабочих перезапусков не было. Следующий шаг — обратная связь о релизе;
новая основная сборка должна иметь versionCode выше1521 для возврата после rollback.

---

<a id="record-025"></a>

# Текущая выдача — test24 /1519

Claude: чекбоксы в «Подробности прав» создания и под обычным режимом в правах.
Работающий Claude теперь открывает тот же редактор следующего запуска.
Обычный режим блокируется при Auto/Bypass; недоступный Bypass выключен.
Старые информационные «Подробности» удалены, команда подключения сохранена.
История/Команды/Алиасы/Заметки: заголовки по центру; описание истории рядом с очисткой.
116 тестов широкого прогона прошли, после последней правки повторены 9 целевых.
4 Node-набора прошли; Modern Lint 0 ошибок /281 предупреждение; финальный APK-аудит прошёл.
Native Claude2.1.296: Auto control ACK принят без модели; Bypass UID0 отклонён.
Настоящая модель/аккаунт и телефонный UI не проверены; ADB без устройств.
Установки, публикации и рабочих перезапусков нет. Следующий шаг — проверка пользователем.
Перед откатом на test22 или старше вернуть обычные режимы Claude.
APK /sdcard/Download/Termux-Jobs-v2.1.5-test24-arm64.apk
SHA256 aa8a25631890708c57d975fa84ca743e17b4997293f1beb8e5ff6a8799a4b39d.
Отчёт docs/fork/VALIDATION-2.1.5-test24.md; доказательства .verification/test-2.1.5-test24.

---

<a id="record-026"></a>

# Текущая выдача — test23 /1518: режимы Claude

Auto/Bypass: квадратныечекбоксы, взаимоисключение, сохранение прежнего обычного
режима. Применение только на следующем запуске, без рабочихперезапусков.
Native2.1.296: AutoACK принят; Bypass запрещён приroot/proot. Настоящейавторизации
нет, реальныйклассификатор не проверен. Fakeстенд готов отдельно:
/root/termux-fork/.verification/claude-mode-phone; manual26/Auto27/nativeBypassRefusal.
79unitPASS, 4Node suitesPASS, ModernLint0errors283warnings, finalAPKgatePASS.
ТелефонныйUI не проверен; установки/публикации нет. Перед откатом наtest22/старше
вернуть обычныйрежим уClaude: старыйвалидатор отвергаетauto/bypassPermissions.
APK /sdcard/Download/Termux-Jobs-v2.1.5-test23-arm64.apk
SHA256 17d3a5dfb97c2c35ae1ed5d936009ede36f324fa6d8206bc77f645738726f497.
Отчёты docs/fork/VALIDATION-2.1.5-test23.md и CLAUDE-MODE-PHONE-FIXTURE.md.

---

<a id="record-027"></a>

# Текущая выдача — test22 /1517

Компактные Файлы/Вложения/Изменения; настоящие миниатюры и исправленный доступ
к managed-вложениям; оригинал/Download без перезаписи; меню языка вниз.
Повторные переходы выбора агента/формы: одно окно, общий фон, блокировка нажатий.
Проверки: основной прогон115 с одной ошибкой размера тестового якоря; итоговый
прогон13/13pass, остальные102pass. ModernLint0errors280warnings; APK-аудит прошёл.
ТелефонныйUI/реальныйrestart не проверены, adb без устройств. Публикация ждёт
проверки пользователем и подтверждения. Установки/рабочихперезапусков нет.
APK /sdcard/Download/Termux-Jobs-v2.1.5-test22-arm64.apk
SHA256 24fdbf7ea64d098d9b737c5fcdf144f710cb100f3184cdc477943d282b7bbc19.
Отчёт docs/fork/VALIDATION-2.1.5-test22.md; доказательства .verification/test-2.1.5-test22.

---

<a id="record-028"></a>

# Текущая выдача — основной1516/test21

Вернули компактную «Очистить» справа в строке прежнего section(), без повторного
заголовка. Источник сравнения a4c6f1d9^; пользователь отклонил широкую кнопкуtest20.
Тест совпадения границ при300dp и отмены/очистки прошёл; ModernLint0errors279warnings,
APK-аудит прошёл. ТелефонныйUI не проверен. Установки/публикации нет.
APK /sdcard/Download/Termux-Jobs-v2.1.5-test21-arm64.apk
SHA256 7252053d6af2827d9002a3f6d4dc061bdd5b18d161aecbb0d384681d7157cecf.
Отчёт docs/fork/VALIDATION-2.1.5-test21.md.

---

<a id="record-029"></a>

# Текущая выдача — основной1515/test20

Задание: Нет/Да для сохранённого чата; убрать дублирующую документацию из Программ;
постоянная кнопка очистки истории; немедленное обновление имени в боковом меню.
Готово: VALIDATION-2.1.5-test20.md в docs/fork; CHANGELOG обновлён.
APK /sdcard/Download/Termux-Jobs-v2.1.5-test20-arm64.apk
SHA256 907dd8698ccfcc67ba946da8378577ecd1a64a548ed6ef8ff3c2dd11c40a8976.
Тесты, ModernLint0errors279warnings, сборка и барьер финальногоAPK прошли.
ТелефонныйUI этого обновления не проверен; установки/публикации/рабочихостановок нет.
Пользователь подтвердил открытие большогоPNG в fake-Claude. Стенд сохранён.

---

<a id="record-030"></a>

# Тяжёлый PNG в активном fake-Claude

По сообщению пользователя небольшой PNG открывается из миниатюры.
IMAGE TEST теперь создаёт PNG4096×3072,30822506байт. NativeRead/плашка и Pillow прошли.
Открывать НОВУЮ плашку; старый текст ответа provider1280×720 захардкожен,
реальные сведения в image-info.txt. Телефонный результат большого файла ожидается.
Подробности docs/fork/CLAUDE-IMAGE-PHONE-FIXTURE.md.

<a id="record-031"></a>

# Уточнение запуска fake-Claude

TESTCLAUDE запущен с chat=id; UUID отсутствует в отдельном fakeHOME.
Native-проба воспроизвела No conversation found + exit1; новыйчат повторно прошёл.
Нужен новый профиль с launcher ДО первого запуска. Рабочийчат не перезапускали.
Подробности: docs/fork/CLAUDE-IMAGE-PHONE-FIXTURE.md, раздел Existing-profile failure.

<a id="record-032"></a>

# Дополнение — готов fake-Claude для проверки PNG

Пользователь явно запросил прежний fake-login стенд. Он подготовлен и native-проверен.
[Инструкция и доказательства](../CLAUDE-IMAGE-PHONE-FIXTURE.md).
НовыйClaude/Sonnet/manual/UbuntuAI; папка:
/root/termux-fork/.verification/claude-image-phone/project
Команда подключения (исполняемыйпуть безаргументов):
/root/termux-fork/.verification/claude-image-phone/launch-claude
Сообщение IMAGE TEST. Нужен Allowonce для fixturePython, затем nativeReadPNG.
Fakeprovider получил1imageblock; snapshot1imageaction/completed/exactpath; stopACK/processExited.
Kernelguard проверен заново. UI проверяет пользователь; реальнаяавторизация не нужна.
/root/termux-fork/.verification/claude-image-phone — symlink на собственныйfixture;
код .verification/claude-image-fixture-code. НЕ очищать, пользователь будет запускать.
/sdcard/Download/Claude-fake-image-test.txt заменяет прежнююинструкцию с реальнымвходом.
Новая сборка не нужна;1514 остаётся текущей. СтарыйPNGдругоготелефона не получен.

---

<a id="record-033"></a>

# Текущая выдача — основной1514/test19

[Карта/проверки](../VALIDATION-2.1.5-test19.md).
APK /sdcard/Download/Termux-Jobs-v2.1.5-test19-arm64.apk
SHA256 e255dd15e2f1602f86f022dd0c34c9fbf82638d2c640d4b7ee34a7b0eb31c13b.
История: исправлены пустые тексты, внутренний повтор заголовка убран.
Стоп: только квадрат #FF5252, размеры/условия/обработчики прежние.
Image actions снова отдельные плашки всех4агентов, без дубля внутри хронологии.
Claude Read(PNG) получает подпись Просмотр изображения. Детали/thumbnail/original прежние.
94Java testsPASS +4повторные усиленные viewer testsPASS,5JSfixturesPASS.
FullModernLint0errors/279warnings5modules, finalAPKgatePASS,620sourcefilesmatch,
7ELFstable2.1.4identity. InitialClaude test expectation corrected declined/nativeStatusfailed;
permission production untouched. Evidence .verification/test-2.1.5-test19/.

НЕ ЗАКРЫТО: ошибка оригинала threads-menu.png на другом телефоне со старымUI.
Пользовательское видео1001643106 показывает thumbnail, generic error иsave notifications.
PNG пока не прислан; rootcause не установлен. Раздельная диагностика в1514 не
объявляется исправлением декодирования. Codex/Claude actions идут через общий
bitmap320/2048. Markdown-link entry дополнительно fingerprint/localFileLink.
Попытка отдельногоCLIчата (--setting-sources empty) остановиласьNot logged in;
это не статус авторизации пользовательского профиля/другого телефона.
ADBdevices пуст, screenrecord ERROR:no display; физическоеUIне проверено.
Пользователь согласился проверить сам; готовый вход/задание:
/sdcard/Download/Claude-image-check-test19.txt.
Отдельный каталог /root/termux-fork/.verification/claude-image-check-test19 создан,
TASK.txt подготовлен. Сам чат в приложении не создан: нужен новыйClaudeчерезформу.
Следующий шаг: пользователь сравнивает карточку/ссылку одногоPNG, присылаетошибку
иоригинал; воспроизвести320/2048decode, установитьпричину, затемисправить.
Без установки/публикации/рабочих перезапусков; резерв1499 не читать/не менять.

---

<a id="record-034"></a>

# Текущая выдача — основной1513/test18

[Карта/проверки](../VALIDATION-2.1.5-test18.md).
APK /sdcard/Download/Termux-Jobs-v2.1.5-test18-arm64.apk
SHA256 7114891d3ed787df711a871eef87177d959d5ddd569a2803010dad970d1c9fff.
Toolbar pager только1страница; второе EditText не создаётся.
Enter послеPgUp/PgDn занимает2ряда; customAppCompatButton рисует vector↵,
contentDescription Enter. Сохранены модификаторы/жёлтый цвет/штатные keycodes.
История поля ввода: название меню/окна/шапки и пояснение, storage прежний.
47тестовPASS, ModernLint0errors/279warnings, финальныйAPKgatePASS.
ПервыйlintAppCompatCustomView исправлен,2устаревших теста обновлены,
исходные свидетельства сохранены; промежуточныйAPK не выдавался.
Телефонная проверка свайпов/IME/поворота/реальных клавиш и истории не выполнена.
Без автоматической установки/публикации/рабочих перезапусков.
Резерв1499 не читать/не менять/не пересобирать. ПрежниеAPK сохранены.

---

<a id="record-035"></a>

# Предыдущая выдача — основной1512/test17

[Карта/проверки](../VALIDATION-2.1.5-test17.md).
APK /sdcard/Download/Termux-Jobs-v2.1.5-test17-arm64.apk
SHA256 fbe1537249300211a464cd9481ec5048e012fb839a9c4621a189911db9f45055.
Терминальные Notes центрированы. Enter перед колонкой PgUp/PgDn, верхней
строки больше нет. Модификаторы жёлтые при нажатии/активации/фиксации;
снятие возвращает обычный цвет, reload не затирает активный цвет. Stroke убран.
Бренд над настройками удалён; описание масштаба12.8sp.
16тестовPASS, ModernLint0errors/277warnings, finalAPKgatePASS.
Телефонная визуальная/клавиатурная приёмка не выполнена; следующий шаг —
проверка Notes/ripple/Enter/PgUp/PgDn/Ctrl и настроек на устройстве.
Без автоматической установки/публикации/рабочих перезапусков.
Резерв1499 не читать/не менять/не пересобирать. СтарыеAPK сохранены.

---

<a id="record-036"></a>

# Предыдущая выдача — основной1511/test16

[Карта/проверки](../VALIDATION-2.1.5-test16.md).
APK /sdcard/Download/Termux-Jobs-v2.1.5-test16-arm64.apk
SHA256 92777a948fc8db75d5daa0cbc4a2022fd847ff2f6af35491c8511b57c94ef595.
composerViewport использует keepsChatPresentation, не обнуляет нижний inset
фонового чата. Сохраняется место перенесённых extra keys. Типографика/строка
терминальных библиотек/общие настройки обновлены. Компактный chooser и Back
с сохранением формы, светлые круги/тёмные vector arrows в oval buttons.
Каталог вынесен рядом с EditText. Enter отдельным верхним рядом над PgUp;
исходные ряды сохранены, существующий Enter не дублируется.
41целевой тест без оставшихся отказов (37из финального набора+4повторно после
исправления поиска chooser в тесте), ModernLint0errors/277warnings, APKgatePASS.
Телефонная приёмка подскролла/IME/кадров/реальногоEnter и создания не выполнена.
Следующий шаг — пользовательская проверка на телефоне. Не устанавливать/
публиковать/перезапускать рабочие сессии автоматически. Резерв1499 не трогать.

---

<a id="record-037"></a>

# Предыдущая выдача — основной1510/test15

[Карта/проверки](../VALIDATION-2.1.5-test15.md).
APK /sdcard/Download/Termux-Jobs-v2.1.5-test15-arm64.apk
SHA256 5a4f3503c848f31256424b53a20d52c7dfe79c27b04da80bce2b2d9d1c4148dd.
Чат и composer сохраняются под sliding terminal; общий источник лимитов.
Noteslabel/plus21dp stroke2; popup0.8, more1.3. Центральный chooser и
sliding параметры, header submit делегирует старому валидатору.
46тестовPASS, ModernLint0errors/276warnings, finalAPKgatePASS.
Следующий шаг — телефонная приёмка переходов/IME/позиции истории и реального
создания/запуска. Не объявлять отсутствие чёрных кадров подтверждённым без телефона.
Не устанавливать/публиковать/перезапускать рабочие сессии автоматически.
Резерв1499 не читать/не менять/не пересобирать; прежниеAPK сохранить.

---

<a id="record-038"></a>

# Предыдущая выдача — основной1509/test14

[Карта/проверки](../VALIDATION-2.1.5-test14.md).
APK /sdcard/Download/Termux-Jobs-v2.1.5-test14-arm64.apk
SHA256 0b414dcaafb7db3e182d593f5477b1cdc9d938e125b0db8c5d39056f9ae9bc57.
Вложенность меню, содержание0.8, адаптивная ширина по действиям с исключением
имён/путей, уменьшенное меню«+». Остановка/индикаторX11 не переписаны;
пункт сразу после открытия, прежние состояния/защиты сохранены.
30тестовPASS, ModernLint5модулей0errors/274warnings, финальныйAPKgatePASS.
Телефонная визуальная/касательная приёмка не выполнена; следующий шаг —
ручная проверка меню/ширины/IME/возвратов/индикатора на устройстве.
Не устанавливать/публиковать/перезапускать рабочие сессии автоматически.
Резерв1499 не читать/не менять/не пересобирать. ПрежниеAPK сохранены.

---

<a id="record-039"></a>

# Предыдущая выдача — основной1508/test13

[Карта/проверки](../VALIDATION-2.1.5-test13.md).
APK /sdcard/Download/Termux-Jobs-v2.1.5-test13-arm64.apk, 40612510байт.
SHA256 56d7bf648863a39f2892b8a0fc1a1bf85b8acaa2bf5aad5dcac4f81f560c8944.

Голос полностью удалён из приложения по новому заданию: микрофон, Vosk/JNA,
model ZIP, код/скрипт загрузки/распознавания и RECORD_AUDIO/queries. Вынос моделей
в GitHub Components отменён. Старые файлы privatecache удаляются только по полному
manifest/hash/marker; неизвестное сохраняется. Прежнее место модели/рассуждения
сохранено пассивным44.5dp промежутком; ComposerLimits не менялся.

Нужна именно прежняя «змейка» CW по круглой обводке, НЕ пульсация значка.
WorkspaceIconButton рисование прежнее. Причина зависания воспроизведена: оборачивание
текущего background в новый ripple отключало callback/visibility прежнего фона.
Теперь headerAttention сразу делает один ripple-root с кругом/обводкой; follow(View)
ведёт phase через ValueAnimator/INFINITE/2sec и invalidate кнопки. Возврат/ожидание/
reduced motion обработаны. Android Canvas подтвердил CW-геометрию; это не запись UI.

X11 stop pending/grey/spinner, per-owner guards, error/timeout/retry, success после
подтверждения/отключения своего сервиса. Старый service shutdown/7native неизменны.
Все4модели показывают текущие значения; unsupported Gemini reasoning скрыт даже
при metadata options. Меню«+»:24sp/36dp/88dp и18dp padding, ограничение экрана/scroll.

120тестов0fail/error/skip в18классах. После одной дополнительной capability-проверки
Gemini повторены13целевых тестов,0fail/error/skip; прогоны пересекаются. Другие
результаты120 переиспользованы с source-delta proof. Lint5modules0errors/274warnings,
checker8/0. APK ZIP/package/version/cert/ABI/runtime/126assets/7ELF — PASS;
616production files совпали по3stages. Доказательства .verification/test-2.1.5-test13.
Промежуточные отказы/ограничения старого Robolectric clock сохранены в отчёте.

Телефонная приёмка и запись исправленного UI НЕ выполнены. Присланный ролик
подтвердил исходное замирание, но не исправление. screenrecord из текущего доступа
вернул exit254 ERROR: no display; это не отсутствие записи на самом телефоне.
Следующий шаг пользователя: вручную установить APK и записать6–10сек движения
без касаний, возврат/окончание ожидания; отдельно проверить X11 stop, модели/меню/IME.
Автоматической установки, публикации, push/tags, рабочих остановок/перезапусков нет.
Резерв1499 не читать/не менять/не пересобирать. Прежние APK сохранить.

---

<a id="record-040"></a>

# Предыдущая выдача — основной1507/test12

[Карта/проверки](../VALIDATION-2.1.5-test12.md),
[подтверждённая причина/новый offline backend](../DICTATION-OFFLINE-2026-11-07.md).
APK Termux-Jobs-v2.1.5-test12-arm64.apk,89543162байт, SHA256:
c5ea0c24eac94468546dcde4aebd16b51dbb9f286807b1d725fe833c4395d5a8.
Путь выдачи /sdcard/Download; без автоматической установки.

Новый Vosk0.3.75/JNA5.18.1/Russian-small0.22, собственный AudioRecord для всех4agents.
Нет Google/чужих Activity/системного provider. Runtime RECORD_AUDIO, подготовка,
partial preview/stop/cancel, draft-owner/epoch guards. ModelZIP generated ignored;
подготовка scripts/prepare-dictation-model.py, preBuild SHA guard. Asset/лицензии
вAPK. Нативные7 прежние;2новые pinnedARM64/16KiB. Движок/production-extractor
и реальная модель запущены через Android helper, но только на тишине.
Настоящий микрофон/фраза в исходном черновике НЕ проверены и остаются приёмкой телефона.

38тестов0fail/error/skip, ModernLint5modules0errors/272warnings. APK final gate
package/signature/ABI/ZIP/runtime/130assets/9ELF,3stage623files — PASS.
Сведения о первой неудаче helper linker namespace и inventory4AWT сохранены;
исправленные проверки не маскируют их. Доказательства .verification/test-2.1.5-test12,
.verification/offline-dictation, .verification/dictation-1507. Последние стили/терминал1506
сохранены; функциональную приёмку телефона не объявлять выполненной.

Claude public APK RecognitionService.onStartListening сразуcallback.error(5).
Явный helperbind: proxytrue/start1/error5/cleanupaftercallback. Androiddefaultnull
такжеerror5доconnect; isRecognitionAvailable=true толькоdiscovery. Target speech
logcat пуст; это ограничение, не доказательство отсутствия подключения. Yandex
имеет внутренний SpeechKit, но не экспортирует Android RecognitionService.
Никаких auth/private browser data/APIkeys не читалось и не использовалось.

Следующий шаг: пользователь вручную ставит APK и диктует русскую фразу в черновик.
При отказе нужен точный stage/exception из окна. Не устанавливать/публиковать,
не менять системную службу и не перезапускать рабочие сессии. Резерв1499 не трогать.
Исследование AndroidHome при скролле/изображений/обрывов остаётся отдельно.

---

<a id="record-041"></a>

# Текущая выдача — основной1506/test11

[Состав/карта/проверки](../VALIDATION-2.1.5-test11.md),
[устройство/Яндекс/ограничения диктовки](../DICTATION-DEVICE-2026-11-07.md).
APK `/sdcard/Download/Termux-Jobs-v2.1.5-test11-arm64.apk`,40611413байт.
SHA256 `fa486f2df803746b89b74f011db25001dea56f9d7d02d9251106c88ca395246c`.
Checksum рядом; exclusive copy/readback двух Android-путей — PASS.

Два изменения: terminal history под прозрачной шапкой при auto-follow, без первого
жеста; cursor safe viewport/hit testing. Диктовка request RECORD_AUDIO → default
create/start; exact onError N/name/stage и параметры из Activity вместо раннего отказа.
Default/provider автоматически не меняется. Черновик/owner/epoch/partial/final прежние.

Baseline терминала3/3:2paint-gate,1fixture безsession; повтор31/0. Объединённый60/0,
финальный54/0; не суммировать. Lint5модулей0errors/272warnings, locale587/checker8.
APK gate ZIP/package/version/signature/ABI/runtime/125assets/4templates/7ELF — PASS.
630production files совпали по3stages,95native inputs/X11/ComposerLimits прежние.
КоммитID `.verification/test-2.1.5-test11/source-commit.txt`.

**Следующий шаг пользователя:** вручную установить1506, нажать микрофон в исходном
чате, выдать право Android, продиктовать. Диктовка не завершена до текста в черновике.
При отказе нужен полный onError/этап/снимок из окна. На SDK36 UID10620 право было
DENIED, availability=true, onDevice=false, доступна ClaudeRecognitionService.
Буквальное secure setting вне зарегистрированного UI прочитать не удалось.
Яндекс26.10.3.50 содержит SpeechKit, но не предоставляет Android RecognitionService;
его поиск не подключается как системный provider. Не выбирать Claude автоматически.
Телефон: auto-output/первый жест/return, низ cursor/IME/keys/rotation требуют проверки.
Не ставить APK автоматически; не перезапускать рабочие сессии; резерв1499 не трогать.
Изображения/обрывы и Android Home при листании остаются отдельными открытыми задачами.

---

<a id="record-042"></a>

# Предыдущая выдача — основной1505/test10

[Состав, карта6пунктов, проверки и остатки](../VALIDATION-2.1.5-test10.md).
APK `/sdcard/Download/Termux-Jobs-v2.1.5-test10-arm64.apk`,40610229байт.
SHA256 `670d711f62d62303267e54ed35d8560ed9512c3dbe976a3982fa9aba9662171b`.
Checksum рядом; exclusive copy/readback двух Android-путей — PASS.

Sidebar80%,20,5/16/22sp, новые читаемые строки/иконки; круглые шапки48dp без центра;
X11 input/extra circles по центру; legacy title Toast скрыт; summary card toggle целиком;
first-DOWN IME и сохранён click/accessibility; floating viewport без первого жеста.

Broad284/4; повторы76/2,20/1; финальный6/0. Старые отказы сохранены. Новый first-DOWN
contract/click/focus/draft и controller search DOWN/UP проверены; native Editor popup
Robolectric сломан также наAPI29, настоящая клавиатура не подтверждена. Остальные
переходы/resize/history/links/X11 fixtures прошли. Engine154/0 из1504 — reuse.
Lint5модулей0errors/272warnings, localization587/checker8; APK gate ZIP/version/ABI/
подпись/runtime/125assets/4templates/7ELF — PASS.630production files совпали по3stages,
95native inputs/X11/ComposerLimits неизменны. КоммитID в `.verification/test-2.1.5-test10/source-commit.txt`.

Следующее: пользовательская проверка6строк карты при одинаковых font/display settings;
реальные IME, курсор/выделение, terminal scroll/поворот/клавиши/X11/прозрачность/типографика.
Настоящий микрофон/default speech service тоже не подтверждены. Android Home при долгом
листании — исследование, решения нет; изображения/обрывы — отдельная задача.
Резерв1499 не читать/менять/пересобирать; старые APK сохранить. Нет установки,
публикации, push/тегов и перезапусков рабочих сессий.

---

<a id="record-043"></a>

# Предыдущая выдача — основной1504/test9

[Карта десяти пунктов и проверки](../VALIDATION-2.1.5-test9.md).
APK `/sdcard/Download/Termux-Jobs-v2.1.5-test9-arm64.apk`,40609605байт.
SHA256 `e2f5194fd39f05a10a8e4a7dd39b94bcd1517910065491c2d94620f98495c869`.
Checksum рядом; exclusive copy/readback обоих Android-путей — PASS.

Default SpeechRecognizer без Activity/chooser, inline partial/Stop/Cancel и сохранение
черновика. Прозрачные общие шапки, restart→terminal/computer, верхний inset и hit mapping
терминала, согласованная высота input card, базовая Termux без Session settings.
X11 header всегда black/opaque, return + два режима ввода; обработчики прежние.
Sidebar12,2sp/18sp. Центральные библиотеки с IME по tap остаются поверх прежнего меню.

Broad158/2; оба отказа закрыты итоговым targeted28/0; card-height8/0. Engine154/0 --rerun-tasks.
Изначальные compile/70-19/остановленный layout-прогон сохранены; не засчитаны как PASS.
Lint5модулей0 errors/274 warnings; локализация587строк и checker8 — PASS.
Финальный APK gate: package/version/ZIP/подпись/ABI/runtime/125assets/4templates/7ELF.
630 production-файлов совпали по3stages;95native inputs/ComposerLimits/X11 неизменны.
Локальный commit ID — `.verification/test-2.1.5-test9/source-commit.txt`.

Телефонные mic/IME/шапки/поворот/Surface/GPU требуют проверки. Не заявлять приёмку
или работу настоящей диктовки по JVM. Android Home при листании остаётся исследованием.
Резерв1499 не читать/менять/пересобирать. Старые APK сохранены. Нет установки,
публикации, push/тегов и перезапусков рабочих сессий.

---

<a id="record-044"></a>

# Предыдущая выдача — основной1503/test8

[Состав, карта семи пунктов и ограничения](../VALIDATION-2.1.5-test8.md).
APK `/sdcard/Download/Termux-Jobs-v2.1.5-test8-arm64.apk`,40609053байт.
SHA256 `d70dc988e80de6119853945eb55c6a6e4c270d941a23a724ca58ea44727f5b77`.
Checksum рядом; exclusive copy и readback двух Android-путей — PASS.

Реализованы все7: скрытие launch selector существующих записей, SpeechRecognizer
fallback с выбором службы/RECORD_AUDIO, чёрный status bar, compact agent header,
slide терминала/обратно, минимальная шапка терминала, fade центральных библиотек.
Read-only manifest установленного Claude: exported recognition service,
BIND_RECOGNITION_SERVICE. Настоящие bind/микрофон/распознавание не подтверждены.

Финальный targeted7/0; ранее height6/0 и широкие53/55 до двух исправлений.
Исходные52/7,55/2,55/2 и все повторы сохранены. Полный app-suite не повторялся.
Lint5модулей0 errors/275 warnings. APK ZIP/package/version/подпись/ABI/runtime/
125assets/4templates/7ELF,630production files по3stages,95native inputs — PASS.
Доказательства `.verification/ui-1503/`, `.verification/test-2.1.5-test8/`.
ID локального коммита — `.verification/test-2.1.5-test8/source-commit.txt`.

Следующее — проверка телефона по отчёту, особенно выбранная Claude recognition
service, её разрешения/ошибки, смена чата, первый кадр IME и header/terminal переходы.
Исследование Android Home при долгом листании остаётся открытым, исправление не заявлено.
Резерв1499 не читать/менять/пересобирать. Старые APK сохранены. Нет установки,
публикации, push/тегов и рабочих перезапусков.

---

<a id="record-045"></a>

# Предыдущая выдача — основной1502/test7

APK: `/sdcard/Download/Termux-Jobs-v2.1.5-test7-arm64.apk`,40603693байт.
SHA256: `e4e60caedee3bbd92ad52daca83ae54d71e40858a988825bba5ba8d4cd115819`.
Файл создан без замены; checksum рядом; readback через /sdcard и /storage совпал.
[Карта](../REDESIGN-v35.md), [проверки и ограничения](../VALIDATION-2.1.5-test7.md).

Включены: заливка терминального renderer и внешней поверхности, прозрачная шапка
агентских чатов с измеренным отступом истории, полная ширина portrait с сохранением
landscape/insets, меню+ с последовательной анимацией, composer44,5dp/12,5sp/gap8,
появление новых user/timeline330ms, центральные библиотеки без Back, lifecycle диктовки.
Строка лимитов, native/X11, PTY/CLI/история не менялись. X11 получает новую геометрию
через существующий host. Нет установки, публикации, push, тегов или рабочих перезапусков.
Резерв1499 не читать, не менять и не пересобирать. Старые APK сохранены.

Последний целевой прогон45/0; предыдущие целевые результаты и отрицательные
проверки сохранены. Modern Lint5 модулей:0 errors/273 warnings. Финальный APK:
ZIP/package1502/test7/подпись/ABI/runtime/125assets/4шаблона/7ELF — PASS.
628 production-файлов совпадают canonical/JVM/Lint/ARM64;95native inputs неизменны.
Исходники и отчёт сохранены локальным коммитом; его ID —
`.verification/test-2.1.5-test7/source-commit.txt`.

Диктовка: прежний visibility-owner воспроизвёл отказ результата до onResume;
новый путь прошёл тесты четырёх агентов. Read-only PM телефона не нашёл Activity
ACTION_RECOGNIZE_SPEECH. Найденная RecognitionService — иной интерфейс, её работа
не проверена. Новый backend не добавлен. Без совместимого Activity-провайдера
кнопка показывает понятное ограничение; настоящий микрофон/распознавание не подтверждены.
Отмена/ошибка/смена чата сохраняют черновик, автоотправки нет.

Следующее — пользовательская проверка1502: визуальная геометрия/поворот/IME,
настоящий терминал/X11, библиотеки и диктовка с доступным Android-провайдером.
Исследование выхода на Android Home при долгом листании Codex остаётся открытым:
причина не установлена, исправление не заявлено. Остальные ограничения1501 сохранены.

---

<a id="record-046"></a>

# Предыдущая выдача — основной1501/test6

[Карта18 пунктов](../REDESIGN-v35.md), [отчёт](../VALIDATION-2.1.5-test6.md).
Реализованы2–18: шапка/restart, читаемость/геометрия composer, нижние библиотеки,
Back из diff, возврат в sidebar, существующий X11 без запуска, чёрный output/keys,
однократное появление новых user/timeline. Телефонная приёмка1501 ожидается.

APK: `/sdcard/Download/Termux-Jobs-v2.1.5-test6-arm64.apk`.
SHA256: `aacbf808bcf035dd3177db30e828eb2940c1dc29bfaf37b71b7d464db83bcec0`.
40601649байт; SHA-файл рядом. ZIP/package/version/подпись/ABI/runtime/125assets/
4шаблона/7ELF stable проверены; Download создан без замены и прочитан обратно.
627 production-входов совпали между canonical/JVM/Lint/ARM64.
Full app1694:11 первоначальных отказов старых ожиданий; targeted-4:75 PASS.
Targeted-5:26/1 отказ новой fixture; targeted-6/7 уточнили ограничения метрик
Robolectric; targeted-8:7 PASS. Все исходные отказы закрыты повтором;
журналы/XML/соответствие сохранены, полный app набор повторно не запускался.
Terminal-emulator154 PASS; terminal-view/shared standalone NO-SOURCE.
Modern Lint5 модулей0 errors/272 warnings; локализация587строк PASS.
Исходники/отчёт сохраняются локальным коммитом; никаких push/тегов/публикации.

Пункт1 остаётся только исследованием: долгое листание Codex → Android Home.
Crash buffer пустой; причина/воспроизведение не подтверждены. Исправление симптома
в1501 не заявлено и не включено. Нужны время повторения и lifecycle/crash сведения.
Резерв1499 не трогать, не читать и не пересобирать. Старые APK сохранены.
Установка и рабочие перезапуски не выполнялись. Следующее — проверка телефона
пользователем по сценариям отчёта; стенд не доказывает шрифты/IME/жесты/GPU/binding.

---

<a id="record-047"></a>

# Предыдущая выдача — основной1500

2.1.5-test5 / versionCode1500. Все15 замечаний обработаны;
[карта и ограничения](../REDESIGN-v35.md), [отчёт](../VALIDATION-2.1.5-test5.md).
APK: `/sdcard/Download/Termux-Jobs-v2.1.5-test5-arm64.apk`.
SHA256: `6c8794cbf6e093afbf766a9f23e06c521cee9598a2cca0077921d0736fbb83c9`.

Проверены новая финальная подпись/ZIP/версия/ABI/runtime/assets/шаблоны/7ELF stable,
Download readback. Lint5 модулей:0 errors/270 warnings; emulator154 PASS.
App1685:14 первоначальных устаревших ожиданий, все закрыты успешными целевыми
повторами. Все исходные отказы сохранены. Стенд не равен телефонной приёмке1500.
Инструкции/навыки редактора поддержаны дляCodex/Claude; ограничения остальных
агентов указаны в отчёте. Настоящие resume/restart/авторизация/GPU не проверялись.

Резерв1499 НЕ читать, не менять, не пересобирать; нового резерва не создавать.
Старые APK сохранены. Установка, публикация, push/теги и рабочие перезапуски
не выполнялись и этой задачей не разрешены. Следующий шаг — проверка1500
пользователем по сценарию отчёта, без автоматических действий агента.

---

<a id="record-048"></a>

# Историческая выдача — основной1498 и резерв1499 сохранены

Canonical checkout `/root/termux-fork/.verification/rollback-live-actions/source`,
ветка `work/redesign-v35-1494` (историческое имя). Основной2.1.5-test4/code1498
включает все уточнения1–15 и отдельные окна прав/настроек сессии.
Исходный commit: `19952b796aeaa686962c0b1532d8f70215e6dcad`.

Основной: `/sdcard/Download/Termux-Jobs-v2.1.5-test4-arm64.apk`, SHA256
`f02ec13279067c2304b40e200c06da464f8c4bf0c03e5b29308b406432cde50a`.
Резерв2.1.5-backup3/code1499 из принятого старого UI, source dfbf111f:
`/sdcard/Download/Termux-Jobs-v2.1.5-backup3-1499-arm64.apk`, SHA256
`c32c75c389879c8e4c4d74f9dce9a2a6301aeb9cd4e90bb18d2ab7b8c47469ca`.
[Отчёт основного](../VALIDATION-2.1.5-test4.md),
[отдельные проверки резерва](../VALIDATION-backup-1499.md).
Старые1493–1497 сохранены; резерв не является копией данных и не ставится заранее.

Уточнение15 заменило левый открывающий свайп: detector/exclusions удалены,
меню кнопкой раскрывает текущую запись без переключения/запуска. Закрытие осталось.
Права отдельно от формы сессии: native live switches Claude/Gemini/OpenCode,
unique approvals/scope, честные next-launch/No data и защита параллельных изменений.

Проверки: полный app1670/4 первоначальных отказа + успешный targeted-5:19/0,
terminal-emulator154/0; современные Lint5 модулей0 errors/268 warnings;
locale, scoped Stop и свежий финальный APK gate пройдены. Отказы, причины,
источники и границы записаны в отчёте и `.verification/redesign-1498/`.
Установка, рабочие перезапуски, push/tag/publication не выполнялись и не разрешены.

Следующий шаг — телефонная приёмка нового интерфейса по сценарию отчёта.
HTML/снимки/стенд не равны Android-приёмке; приёмка старого1493 не переносится.
[Актуальная карта функций](../REDESIGN-v35.md).

---

<a id="record-049"></a>

## Предыдущая выдача1496/1497 (сохранена)

<a id="record-050"></a>

# Текущая работа — основной1496 готов к проверке телефона, резерв1497 сохранён

Checkout `/root/termux-fork/.verification/rollback-live-actions/source`, ветка
`work/redesign-v35-1494` (имя историческое). Исходники основного APK:
`46b88f87c4852f7db1990475a9e93fc8ffea5e6c`, версия2.1.5-test3/code1496.
[Состав, проверки, ограничения и телефонный сценарий](../VALIDATION-2.1.5-test3.md).
[34 области функций](../REDESIGN-v35.md). [HTML и параметры оформления](../DESIGN.md).

Основной: `/sdcard/Download/Termux-Jobs-v2.1.5-test3-arm64.apk`.
SHA256 `1969cfbd1e546fbab8d9856a25bed9885aa620a0c03329b5be681b2bf5b462d2`.
Резерв1497 из принятого старого интерфейса: source269a63cf,
`/sdcard/Download/Termux-Jobs-v2.1.5-backup2-1497-arm64.apk`.
SHA256 `749c56226b9d58953edad8cb30a3eab30510253d9637e2d2655ba3768de6e3f7`.
[Отдельные проверки резерва](../VALIDATION-backup-1497.md).
Не устанавливать резерв заранее: после установленного1497 основной1496 не обновит
его обычным способом. APK не копирует данные; совместимость будущего отката не проверена.

1494 визуально отклонён пользователем. Старые1493/1494/1495 сохранены. Прежняя
приёмка относится к проверенным пользователем сценариям старого интерфейса;
она не перенесена на1496/1497. HTML отрисован отдельно,15 состояний/440CSSpx.
Android-рендер нового интерфейса недоступен: внешний вид/IME/жесты/поворот/X11
ожидают телефона. Локально:1655 app tests с одним языковым отказом фикстуры,
исправленный случай и затронутые сценарии28/28; Lint0ошибок/265предупреждений.
Финальный файл проверен: подпись/версия/ZIP/static runtime,604входа,
4точных шаблона и7ELF, Download readback. 154 теста терминального модуля повторены отдельно: PASS.

Ближайший шаг: получить визуальную приёмку телефона по сценарию test3. Не устанавливать APK, не останавливать
сессии, не выполнять push/tag/publication. Изображения/обрывы, crash/backup,
непроверенные внешние маршруты остаются открыты; MCP/plugins исключены.
README сохранён; RELEASING дополнен только согласованной парой1496/1497.

---

<a id="record-051"></a>

## Предыдущая выдача (историческая запись, визуально отклонена)

<a id="record-052"></a>

# Текущая работа — тестовый v35 /1494 готов к проверке телефона

Ветка `work/redesign-v35-1494`, checkout `/root/termux-fork/.verification/rollback-live-actions/source`.
Исходники APK: `78e013a3b9f23cceda3035c3f4645892571753bd`. Версия2.1.5-test2, code1494.
APK: `/sdcard/Download/Termux-Jobs-v2.1.5-test2-arm64.apk`.
SHA-256: `e719a57dd531a4ddf0c23b22f586ce10366e2e0385bae57a89b318a1a10e6a9c`.

Принятый1493 и резерв1495 неизменны. Исключение1494 согласовано; установленный1495
не заменяется1494 обычным обновлением. Установку, push/tag/publication и рабочие
перезапуски не выполнять. README/RELEASING сохранены.

Реализация/34 строки: [карта](../REDESIGN-v35.md).
Состав/проверки/остаток/телефонные шаги: [отчёт](../VALIDATION-2.1.5-test2.md).
Общий regression-2:1799 результатов/1 отказ; исправленный отказ и размер текста
подтверждены11 целевыми PASS.154 теста terminal-emulator повторены отдельно.
Изначальные отказы сохранены. Lint:5 модулей,0 ошибок,258 предупреждений.
Финальный APK прошёл подпись/версию/ZIP/runtime/шаблоны/ELF/SHA; копия Download совпала.
Нового телефона/эмулятора не было: промежуточный достоверный рендер недоступен.

Ближайший шаг: получить телефонную сверку drawer/чата/терминала, X11/поворота/IME,
крупного текста и жестов; исправлять только подтверждённые проблемы новым APK.
Изображения/обрывы, Android crash/backup и реальные подключения этим UI не закрыты.

---

<a id="record-053"></a>

## Историческое состояние до v35 (не текущая ветка/цель)



<a id="record-054"></a>

# Резерв1495 сохранён в Download; следующий основной планируется1494

Ветка `work/backup-1495`, checkout ниже. Поручение пользователя2026-11-06: сохранить принятую версию, собрать резерв с кодом1495 и выдать в Download. 1495 заранее не устанавливается. Порядок1493 → основной1494 → резерв1495 при необходимости; после установленного1495 следующий пакет должен быть выше1495. Исключение записано в RELEASING.md. Без push/публикации, установки и рабочих перезапусков.

Поведение заморожено на принятом `83c233ef`; оригинальный APK1493/SHA/отчёт и приёмка `2288ea36` сохраняются. Меняются только версия и документация. Проверка возврата/будущих форматов данных не выполнена; это резерв приложения, не пользовательских данных. [Состав/план/ограничения](../VALIDATION-backup-1495.md).

Готово:15 update/signer tests; Lint0 errors/250 warnings; финальные подпись, version/package/ABI, runtime/DEX,124 assets/7ELF/resources и Download-копия проверены. Файл `/sdcard/Download/Termux-Jobs-v2.1.5-backup1-1495-arm64.apk`; SHA `766f508d51b69d262c3f3bdf825d2cf112a76a42feea5da99aa0b815c1c55344`; source `11aa991a`. Рядом SHA256 и памятка. Не установлен. Ближайший шаг — отдельное поручение о глобальном1494 в новой рабочей ветке; резервные исходники не менять. Предыдущую приёмку не распространять на бинарный1495/откат. Вложения, Claude и согласованные непроверенные Android/backup области остаются открытыми; MCP/плагины исключены.

---

<a id="record-055"></a>

# Приватный 2.1.5-test1 /1493 принят в проверенных пользователем сценариях

Ветка `work/action-timeline`, checkout ниже. Пользователь сообщил2026-11-06: текущее тестовое обновление проверено на телефоне, всё работает. Приёмка ограничена проверенными им сценариями; попунктный перечень не дан. Это сообщение пользователя, отдельно от стенда. Push/публикация, установка помощником и рабочие перезапуски не разрешены.

- Хронология четырёх агентов и её привязка/прокрутка реализованы. Широкий JVM-набор:433 PASS; финальная локальная поправка метаданных доставки отдельно перепроверена6 тестами. Отдельного попунктного результата телефона в новом сообщении нет.
- Уточнения Claude: два нативных сценария и Android replay подтвердили разницу очереди следующего хода и включения при продолжении инструмента. Локально исправлены подпись очереди и чтение человеческих queued_command. Два одинаковых текста с разными UUID сохраняются отдельно. Исходный эпизод пользователя без его UUID/событий не закрыт. Коммит `ca60f83f`; [условия/доказательства](../CLAUDE-ANDROID-CHAT.md).
- Лимиты Claude: нативный get_usage подтверждён на2.1.296, четыре контрольных запроса и повторный запуск без модели/сети провайдера. Искусственный API возвращает отсутствие лимитов; реальная подписка/права профиля не проверены. Условный индикатор читает только штатные окна; неизвестное не заменяется процентами. Коммит `2bfa6dec`.
- Ссылки сброса Claude/Codex: `62e7b413`, только официальный браузерный переход. Состояние не обнуляется; отдельного подтверждения каждого браузерного/шрифтового сценария нет.
- [Полный согласованный состав/ограничения/сценарии](../VALIDATION-2.1.5-test1.md). Нативные95 входов совпали со stable v2.1.4; финальный APK собран и проверен. SHA256 `b841cf4cc8bb6ff6e392da2d714ffb5dfc13e5bf01b4ef81e072feea014e5778`; source commit `83c233ef`. APK: `.verification/test-2.1.5-test1/Termux-Jobs-v2.1.5-test1-arm64.apk`.
- Вложения/обрывы остаются открытыми, исследование не расширять в этой сборке. MCP/плагины исключены; согласованные ограничения Android/резервных копий сохраняются.

Ближайший шаг: сохранить принятый кандидат и ожидать отдельного поручения; не начинать публикацию или повторные проверки без причины. Общая приёмка телефона записана в отчёте, неподтверждённые детали и ограничения остаются открытыми. Lint финальных исходников0 errors/250 warnings; финальный APK/подпись/runtime/7ELF/4шаблона проверены заново. Без публикации; новые разрешения на stable нужны отдельно. Хронология: `748c5414`, подготовка/версия: `83c233ef`; прежний отчёт проверок: `fb953783`. Исходный APK и build-time evidence сохранены без изменения; новый user acceptance хранится отдельно.

---

<a id="record-056"></a>

# Текущий результат — опубликован стабильный v2.1.4 /1492

Checkout: `/root/termux-fork/.verification/rollback-live-actions/source`.
Локальная ветка `fork/release-v2.1.4`; опубликованная — `fork/base`.
Релизный коммит/тег: `384fd197ca30948164fbded3e465f15c2b9318bf`, `v2.1.4`.
[Релиз и публичные проверки](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.4).
Обычный stable/Latest; семь файлов скачаны анонимно и совпали. APK:
`0e63a72139e60ade77efc912190756fbd6ad121f7b2b1a04f11524ed02c874c2`.
AI-INSTRUCTIONS.zip: только четыре точных APK templates + EN README, SHA в SHA256SUMS.

Пользовательская приёмка — сообщение 2026-11-05 о test6 /1491: история,
файловое окно, поиск, карточки, JPG/GIF → галерея. Отдельно от стенда/CI.
CI 42002718204: 1755 passed /8 skipped SFTP (нет Termux пути на Ubuntu runner).
Все восемь SFTP отдельно прошли локально на искусственных файлах. Node26,
Python69, современный Lint0 errors/246 warnings; candidate JVM372+4 переиспользованы
по совпадению исходников. Final APK/public ZIP/signature/DEX/runtime/7ELF/123assets,
update.json и четыре шаблона проверены. Старые APK/теги/Components сохранены.
[Полный итог](../VALIDATION-v2.1.4.md); `.verification/release-v2.1.4/`.
Постпубликационное дополнение отчёта сохраняется отдельным локальным doc-коммитом;
приложение/ресурсы и опубликованный тег не меняются, повторный CI не запускается.

Установка APK и рабочие остановки не выполнялись. Дальнейшая публикация требует
нового поручения. Накопление вложений/обрывы остаются открытыми; исследование
приостановлено на 30141820. Следующий отдельный цикл — согласовать решение
по [контексту](../MEDIA-CONTEXT-INVESTIGATION.md), не включать автоматически
CLI fork/экспериментальные режимы/сокращение и не менять рабочие чаты/оригиналы.
Низкоуровневые Android handoff/crash/backup и настоящий Gemini API/Vertex вход
не закрыты: [аудит](../SECURITY-AUDIT-STAGE2.md). MCP/плагины исключены.

---

Ниже — замороженный исследовательский снимок, не текущий порядок сборки.

<a id="record-057"></a>

# Приостановленное исследование — накопление вложений и обрывы Codex

Ветка `work/media-prompt-replay`; база приложения v2.1.3 / 1485 (`308c8895`).
README.md — правка HezeAI, сохранять отдельно, не включать в исследовательские коммиты.
Единственная память: `/root/termux-fork/LEARNING.md`, обязательна при начале работы.

Локально проверены pinned Codex 0.166.0: A/B промптов; native manual compaction,
сохранение требований/исходников/старой API-истории; дальнейший рост после view_image;
парный private chat/exec на native forks; HTTP/WS передача и управляемые обрывы.
WS на живом соединении не пересылает старые изображения, но cold reopen/reconnect
возвращает полный набор. HTTP A/B не переносить на каждый запрос WS.
[Текущие числа, доказательства, ограничения](../MEDIA-CONTEXT-INVESTIGATION.md#2026-11-04--native-compaction-transport-boundaries-and-request-replay).
Автоматическое сокращение не включать; рабочую историю не сокращать.

Телефон жены недоступен; диагностический туннель выключен пользователем, его не ждать.
Тестировать с доступного устройства через прежний серверный маршрут. Найден старый
локальный запускатель `.tools/codex-geohide-hysteria/agent-via-hysteria` в корне workspace.
Проверить изоляцию от MCP/плагинов до реального запуска; не читать/копировать авторизацию.
Версии и реальные отличия устройства/маршрута указывать. Исправление обрывов не объявлено.

Сравнение Git-релизов завершено на фиксированном CLI 0.166.0/fake provider:
шесть адаптеров, 24 случая, отдельно v2.0.5/v2.0.6 — восемь случаев.
Подтверждённое различие modelPath в `5a1fa804` уменьшает изображение;
роста обычного cold replay 20 PNG между этими адаптерами не выявлено.
Это не запуск старого Java/APK и не сравнение CLI 0.163.0/0.166.0.
Все 18 тегов сохраняют fallback 0.163.0 и предпочитают существующий PATH CLI.
[Доказательства и ограничения](../MEDIA-CONTEXT-INVESTIGATION.md#stable-version-comparison--checked-anew-synthetic-local-provider).

Первый реальный запуск с доступного устройства: 20 localImage, cold ответ
146.26 с после четырёх WS повторов и HTTPS fallback; cold compaction не завершился
за 240.04 с. `.verification/media-context/external-current-device/experiment-ae116ce6/`.
Это не измерение байтов, полученных провайдером, и не проверка телефона жены.
Кандидат `experiment-live-a02cea06` завершён: реальные 20 view_image, foreground
compact 15.16 с; cold reopen 132 270 байт / 0 изображений вместо ~28.79 МБ / 20.
Три требования и метка сохранены, все старые ходы и оригиналы неизменны.
Четыре новых просмотра снова дают 5 111 491 байт / 4 изображения при reopen.
Несокращённая ветка ответила за 87.73 с с четырьмя повторами и HTTPS fallback.
Это доступное устройство/обычный аккаунт, не телефон жены; размеры native JSON,
не подтверждённые байты провайдера. Автоматическое сокращение не включено.
Все наблюдавшиеся процессы теста завершены, рабочие туннели не останавливались.

Повышение лимитов v2.0.5 → v2.0.6 проверено отдельно: 20 → 100 МиБ оригинала
изображения, 256 МиБ → 5 ГиБ прочих файлов. В Codex picker до 20 МиБ по-прежнему
оригинал; выше — prepared JPEG. Native CLI дополнительно меняет 2400px PNG в
1600px PNG, одинаково на старых/новых адаптерах. Запрос 12 589 183 байта во всех
трёх версиях; бинарная карточка 300 МиБ — 58 955 байт без чтения инструментом.
`replay-boT4al/codex-dad3fda5/limits-result.json`. Android guards — проверка исходников;
Pillow JPEG — приближение, не реальные байты Bitmap. Лимиты не менялись.
[Полная таблица/ограничения](../MEDIA-CONTEXT-INVESTIGATION.md#raised-attachment-limits--checked-anew-admission-versus-model-bytes).

Серия размера/транспорта завершена: 15 холодных попыток (по три на ~10/20/25/30/35 МБ).
В пределах 240 с ответы: 3/3, 1/3, 3/3, 3/3, 3/3. Только 10 МБ прошли без повторов;
все десять успешных больших попыток завершились после штатного HTTP fallback.
Сериализованный native JSON — не TLS/сжатые байты и не подтверждённые receipts провайдера.
Артефакты `external-current-device/size-sweep-4ade3cfb`, все оригиналы/наблюдавшиеся
процессы проверены. Работающий чат: безопасные exact-thread события подтверждают
responses_websocket, PID/start не изменились. Для builtin openai принудительный
HTTPS переключатель не найден: override игнорируется, старые flags удалены.
Чистого forced-transport A/B нет; custom-provider не подменяет этот результат.

Старый large-history fix сохранён, установленные скрипты совпадают с checkout.
Отдельный native/Node probe после серии: один ход, 24 настоящих view_image,
внешний JSON 34 696 986 байт, самая большая локальная страница 7 508 байт.
Оба открытия успешны; 32 МиБ не сработали. `replay-cX3PdI/codex-22936fca`.
Локальный Message too large и внешний stream reset — разные пути отказа.
Лимит не повышали; телефон жены/произвольные огромные text-tool страницы не закрыты.
[Таблица и доказательства](../MEDIA-CONTEXT-INVESTIGATION.md#2026-11-04--repeated-size-sweep-and-the-separate-local-history-limit).

Решение 2026-11-05: HTTPS-first не принят как окончательное решение накопления.
Выбран следующий кандидат — нативная проекция контекста запроса с сохранением
полного архива/оригиналов, требований и явным повторным доступом к деталям.
Форк CLI не реализован; до реализации зафиксирован объём поддержки/риски.
Рабочий режим, настройки, лимиты и истории не менять. Неподдерживаемый режим
дальше не исследовать. Не включать автоматическое сокращение.

Stock CLI 0.166.0 уже разделяет архив/активное окно: локальная guarded фикстура
`replay-Sz0snO/codex-c609d493` дала 2 945 815 / 2 изображения → 59 559 / 0 при
reopen; старый ход и оригиналы сохранены, повторный view_image работает.
Но три явных требования отсутствуют в новом запросе; это характеристика механизма,
не готовое исправление. TokenBudget включался только в новом fake HOME без аккаунта.
Каталог gpt-6.1-sol: supports_experimental_context=false, model token_budget.enabled=false.
Не подменять это серверным отказом аккаунту: реальные alpha history/notes не проверялись.
Stock image-store оставляет bytes inline; fileId в схеме не означает upload-once в нашей конфигурации.
[Итоговое сравнение и объём CLI](../MEDIA-CONTEXT-INVESTIGATION.md#2026-11-05--decision-retain-the-archive-and-control-native-request-context).

Следующий шаг после выбора реализации — изолированный прототип request projection
для обоих источников изображений и всех initial/resume/retry ветвей, со сбросом
несовместимого WS state, сохранением условий задачи и проверкой old-image retrieval.
Без реализации форка в текущем шаге, сборки APK, установки или публикации.

MCP/плагины исключены. Новая публикация/установка не разрешены. Общие/проектные
инструкции пока не менялись. Подробности завершённого выпуска — ниже.

---

<a id="record-058"></a>

# Контрольная точка — stable v2.1.3 / 1485

Checkout: `/root/termux-fork/.verification/rollback-live-actions/source`.
Релизная ветка `fork/base`, тег `v2.1.3`; разработка сохранена из `work/opencode-declined-status`.
Единственная память: `/root/termux-fork/LEARNING.md`; читать при начале работы.

Test8 принят HezeAI на телефоне 2026-11-04: всё работает, прозрачность устраивает.
Разрешены подготовка/публикация stable, commit/push/tag и загрузка APK.
Установка APK и перезапуск рабочих сессий не выполняются. Старые APK/теги/Components
не заменяются. Основа сравнения — последний опубликованный stable v2.1.2.

Контрольная точка выпуска: подписанный APK, RU/EN update.json, SHA256SUMS,
коммит/тег v2.1.3. Точный публичный статус и результаты скачивания — на
[странице релиза](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.3).
Порядок: [RELEASING](../RELEASING.md); артефакт/проверки:
[VALIDATION-v2.1.3](../VALIDATION-v2.1.3.md).

Принятые phone permissions всех четырёх агентов и fake-key/API/retry Gemini
не повторять без изменения значимых условий. [Текущий аудит](../SECURITY-AUDIT-STAGE2.md)
отделяет стенд и сообщения разработчика. Android handoff/Keystore/crash и
export/restore копий на устройстве остаются непроверенными по согласованному охвату.
Настоящий Gemini API/Vertex вход и все legacy/manual/TUI пути не закрыты целиком.

Следующий цикл после релиза — накопление вложений/контекста: [исследование](../MEDIA-CONTEXT-INVESTIGATION.md).
Проблема не исправлена, не удалять историю/оригиналы. MCP/плагины исключены.
Перед тематической работой читать только относящиеся разделы и навигатор AGENTS.md.

<a id="record-059"></a>

## Согласованный состав 2.1.4-test1

Дата изменения файлов во встроенном файловом менеджере и выборе вложений:
`04.11.2026 15:42:08`, часовой пояс телефона. При обновлении заново читать
метаданные; при отсутствии времени — «Дата неизвестна» / «Date unknown».
Сохранить имя, размер, сортировку и выбор. Проверить оба окна: создание,
перезапись и обновление списка, длинные имена и отсутствие даты.
Реализация и проверки выполняются в текущей приватной тестовой версии.

Общие шаблоны для следующего тестового APK: использовать **четыре готовых файла**
из ZIP `9fed99b2185d4bb8b03a2ec090d92882`, задание `0c098410ad674181a29e663092d2c045`.
Копии и проверенные размеры/SHA-256:
`.verification/instruction-template-next/supplied-four/manifest.json`.
Эти файлы заменяют прежний одиночный источник 28 729 байт как основу обновления.

- `app/src/main/assets/agent-instructions-ubuntu-ai.md` — Codex RU, 28 861 байт.
- `app/src/main/assets/agent-instructions-ubuntu-ai.en.md` — Codex EN, 15 499 байт.
- `app/src/main/assets/agent-instructions-claude.md` — Claude RU, 28 873 байт.
- `app/src/main/assets/agent-instructions-claude.en.md` — Claude EN, 15 511 байт.

Этап сборки согласован пользователем; применение пользовательских файлов не разрешено.
Сверить смысл RU/EN и все согласованные требования. Codex использует AGENTS.md,
Claude — CLAUDE.md; LEARNING.md и CONTINUE.md одинаковы для обоих агентов.
Это обновление встроенного предпросмотра с явным применением, без автоматической
замены сохранённых общих/проектных инструкций, включая существующие пустые файлы.
Не подставлять шаблон в AGENTS.md/CLAUDE.md репозитория; отдельный проектный шаблон
не менять. Использовать только искусственные файлы, без рабочих чатов/авторизации/процессов.
Проверить выбор агента/языка, полный предпросмотр, явное сохранение, отмену,
конфликт внешнего изменения и сохранность пользовательских файлов.
Включить обновление в описание следующего согласованного тестового APK и сверить
точные тексты четырёх assets в финальном APK. Результаты — в отчёте 2.1.4-test1.
Не объявлять ускорение или исправление накопления изображений из-за инструкций.
Установка, push и публикация этой тестовой версии не разрешены.

## Прежний изменяемый блок AGENTS.md

Историческая запись; не действующая авторизация. Заменена указателем на CONTINUE.

- Сейчас: stable v2.1.4 опубликован; приватный 2.1.5-test1 принят пользователем
  в проверенных им сценариях (2026-11-06). 1494 визуально отклонён; выдан основной1506 (2.1.5-test11); новая телефонная проверка ожидается. Замечания телефона1505 и незавершённая настоящая диктовка сохранены в отчёте1506. Терминальный фон1501 отклонён пользователем; исправление1502 пока проверено локально. Резерв1499 не трогать и не пересобирать; новый резерв не создавать. Старые APK сохранить.
  Push, публикация, установка APK и рабочие перезапуски этой задачей не разрешены.
  Старые APK, опубликованные теги, update.json и бинарные Components не заменяй.
- MCP и плагины исключены. Вложения/обрывы остаются открытыми до следующего
  цикла после релиза. Диктовка вновь разрешена заданием v35: только в черновик, без автоотправки.

