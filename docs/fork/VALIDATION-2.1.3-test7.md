# Private test APK 2.1.3-test7

2026-11-04. VersionCode 1483. Signed release, no push/publication/device install.
Includes test6's audit/backup changes. Published v2.1.2, its tag, update metadata,
Components and the delivered test6 binary remain unchanged.

**Gemini stays at 0.64.0. No working CLI or authorization was upgraded.**
**Attachment accumulation/disconnections remain unfixed and outside this APK.**
The [unified audit plan](SECURITY-AUDIT-STAGE2.md) remains open for phone acceptance.

Local runtime commits: `9b881f3a` (settings/canonical path) и `e78f855d`
(notification persistence). Build/version/docs commit recorded with the artifact.

## Fixes and cause

Native Gemini defaults enable both automatic installation and notifications.
Our earlier run policy selected auth only; installation detection can fall back
to global npm. The native asynchronous updater adds a failed-update error to UI
history, independently of session startup. That explains its classification;
it does not establish the cause of a particular phone's startup failure.

The supported per-run system policy now enforces `general.enableAutoUpdate=false`
and `general.enableAutoUpdateNotification=true`. Native precedence is checked,
including conflicting fake user/project settings and all four auth modes.
User files are unchanged. Auto auth has no forced auth type. External system
policy is refused rather than silently replaced. Updates require explicit action;
the pinned runtime is not replaced just because another version is advertised.

Updater callback items are tagged at their source and use native `type:info`
before recording. Native recording drops unknown metadata, so the info type is
required for reopen. Arbitrary error text does not decide classification. Genuine
API/auth/tool errors remain errors. Historical untagged error records are not
rewritten or guessed to be update notices; use a new test profile for acceptance.

The installer extracts a directory; it does not itself create a runtime symlink.
But runtime/HOME aliases are accepted by the launch path. A logical path could
mismatch Node's canonical module URL, skipping our loader hooks. The controller
now resolves one canonical binary path for argv and the loader environment.
The phone's actual path has not been inspected; a symlink there is not assumed.

## Checked anew

Evidence: `.verification/gemini-managed-updates/`, including failed initial
probes; `.verification/release-2.1.3-test7/` for the final artifact.

- Supported settings: native Gemini schema/loadSettings, 4 modes × first/repeat,
  system precedence and unchanged fake user/project files. Exact native update
  check still advertises synthetic 0.66.0; automatic installation spawn count is 0.
- Notification/error distinction: exact updater callback, three pinned UI layouts,
  native useHistory/recordMessage and native history-to-UI conversion. A recorded
  update remains info; a recorded synthetic HTTP 401 remains error on reopen.
- Ten primary Node suites plus the native-replay and existing chat projection checks: update/settings, missing handlers, auth modes, broker, IPC chat,
  recovery, permissions, live modes and hooks; owned-listener kernel guard enforced.
- Real bootstrap/controller/Gemini CLI in a PTY: normal path and runtime alias,
  first and repeat (4 launches), without a canonical-path workaround in the alias
  harness. Fake broker delivered the run-bound artificial key; native chat accepted
  a message; the local provider received that key and replied HTTP 401. The refusal
  stayed visible as error. Stop acknowledgment follows the owned CLI child's exit;
  the controller also exited. Installed runtime hashes remained unchanged.
- The native “Authenticated with gemini-api-key” banner and `authError=false`
  identify selected mode, not server acceptance. These four real requests failed
  as intended. This is not successful Google login or phone handoff acceptance.
- Real controller normal/alias path, absent UI binding and unsupported-layout
  fixtures reject chat-send with `NOT_READY` and never return accepted. An alive
  process alone does not establish readiness. Unsupported paths do not weaken policy.
- Gemini API/Vertex new-profile native shell/write No/Allow regression: 16 cases,
  one explicit JVM integration method after the final notice change. See final
  result XML/log. Debug is internal testing; the delivered APK is release.
- Syntax, RU/EN notices, 586 mapped localization strings and format contracts pass.
- Full modern Lint 7.4.2 for all five modules: 0 errors, 236 warnings, no rule-loading
  failure. Compile SDK beyond AGP's tested range remains a limitation.

The initial fixture had an unregistered Unix root; a later alias probe skipped
hooks. A temporary harness-only realpath isolated that mechanism, then was removed
in favor of the production fix. The missing-handler fixture initially omitted
native Config.setModel and got STALE_SESSION; corrected and repeated. Those
failures remain recorded and do not count as passes. An initial nonfinal APK was
retained locally before the notification persistence correction; it was not issued.

## Reused with input checks

- 21 unchanged Java/Robolectric vault/broker/editor/launch-builder methods from the
  22-method run. Java/test hashes match in reused-java-inputs.json; native-profile
  integration reruns separately after the final loader change. Existing SDK,
  dependencies and compile settings reused; no phone/Keystore claim follows.
- The three native libraries match the verified v2.1.2 payloads. All 95 recorded
  native/config inputs and compile/link script match. Java/resources/assets rebuild.
  No fresh GPU execution or dependency clean install is claimed.
- OpenCode clean export/restore, legacy v1/v2 and passive receipts checks retain
  the scope of e676dd42; backup production/tests are unchanged from test6. Other
  agents were not rerun because the fix is Gemini-only.
- Developer Codex test5 phone acceptance is retained with its reported scope.

## Final release APK

- Package `com.termux`; **2.1.3-test7 / 1483**, ARM64 only, non-debuggable.
- Size **40,471,430 bytes**; APK v2 signature verifies.
- APK SHA-256: `1150542d8c71674ee6137554c3bcf1e7edc7c91db56e470e5fd2f55bf0163dc3`.
- Public certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP CRC/outer-container checks, all 121 packaged assets and seven ARM64 ELF
  payloads pass. 573 production Java/resource/asset inputs match packaging;
  14,202 DEX definitions, 31 reviewed optional unresolved types.
- verifyReleaseSigner and verifyX11Inputs ran and passed on the final release.
  Legacy packaging Lint/Kotlin metadata diagnostics remain in final-build.log;
  they are not counted as a successful Lint result. The separate modern gate passes.
- Sources/commit and Download copy are checked after the final local commit.
  SDK/Gradle/bootstrap caches reused. No GitHub CI or public channel change.

## Developer phone acceptance — 2026-11-04

Source: HezeAI's report in this conversation for **test7 / 1483**, built from
`0a51e24cddbcc3e33de2a8869446111854552679`. This is developer-performed acceptance,
not a new test independently run by the assistant.

| Phone check | Reported result |
| --- | --- |
| Start a test Gemini API profile with an artificial key | Passed: profile starts. |
| Send a message | API rejects it with **HTTP 400 / API_KEY_INVALID**. |
| Stop that session normally and launch again | Passed: the same invalid-key response is received. |
| Update error interference | The developer reports it no longer interferes. |

The reported functional artificial-key path reaches an explicit API rejection
on both launches. Phone HTTP 400 is distinct from the local fixture's HTTP 401;
neither response is successful authentication. No packet/key-byte trace or
independent socket/Keystore inspection is inferred from this report.
The report does not establish profile editor save/reopen, mid-handoff cancellation,
new-version notice persistence or the phone's normal/symlink path.

## Remaining phone checks

- Actual API/Vertex sign-in with a separate test account, Android peer credentials,
  lower-level broker ownership/Keystore behavior and mid-handoff cancellation.
- Abnormal app/process termination and recovery on an isolated instance without
  working sessions. Normal session stop/relaunch is not a crash-recovery test.
- Gemini outside-project actions and separate outside-file preservation checks.
  Subsequent developer messages accept inside-project shell/direct Write Deny and
  Allow once; see the dated acceptance below. The invalid-key check alone does not
  cover permissions.
- New-version notification stays informational across history reopen; the current
  report confirms absence of interference, not that specific presentation check.
- SAF/export/clean backup restore and other open items in the unified plan.

Do not repeat the accepted startup/normal-stop/relaunch checks without a relevant
change or new failure. Keep real authorizations and working profiles unchanged.
No independent audit or private-key custody verification is established.
The assistant's preceding CLI tests used local providers and blocked external
connections; they do not describe the developer's untraced phone network route.


## Subsequent developer phone permission acceptance — 2026-11-04

Source: the developer's phone messages, «все прошло» after the four AUDIT steps,
and pasted final marker values. Shell and direct Write Deny/Allow once inside the
artificial project are accepted in that scope. Intermediate denial states are
reported by the developer, not independently observed by the assistant. This
continues test7 testing; no new APK identity was measured in that message.
[Exact evidence and limits](PERMISSIONS-COMPATIBILITY-TEST5.md#developer-reported-gemini-phone-acceptance--2026-11-04).
The two outside marker values are recorded as displayed; broader preservation,
outside-project actions, Google login, broker/Keystore, abnormal recovery and
backups remain unverified on the phone. Native fixtures remain separate assistant
results and are not attributed to the developer. No APK was rebuilt for this note.


## История этапа 2: проход 7 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Gemini: исправление аварийного остатка

Затронуты `GeminiApiKeyVault.prepare`, `GeminiKeyBroker`, `GeminiCli.command`,
`SessionShell`, `WorkspaceController.create` и `gemini-control.js.takeApiKeySocket`.

Новые запуски не создают `keys/<profile>.key`. Хранилище ciphertext, alias Keystore,
AAD profile binding и сохранённые профили не мигрируют. Новый broker получает
снимок ID/ciphertext и расшифровывает его только после проверки потребителя.
До этого, в том числе во время длительной подготовки пакетов, plaintext не нужен.

Передача одноразовая. Проверяются run UUID, kernel UID клиента, принадлежность
его PID дереву конкретного терминала и время старта владельца из `/proc/<pid>/stat`.
Неизвестная принадлежность, умерший владелец и повторное использование PID приводят
к отказу. Завершение старого broker не закрывает broker другого запуска.

- До привязки созданного терминала предусмотрено ожидание до 30 секунд.
- Нулевой PID ещё не запущенного терминала не считается живым подтверждённым
  владельцем и не разрешает выдачу ключа.
- Пока потребитель не пришёл, watcher проверяет владельца раз в секунду.
- Запрос канала и потребитель имеют ограничение ожидания 10 секунд.
- Ошибка создания терминала закрывает именно локальный объект этой попытки.
  Закрытие окна само по себе не завершает broker активного запуска.
- При отказе Keystore передаётся только фиксированный код, без исходного сообщения.
  Закрытие во время расшифровки не разрешает позднюю передачу; массивы очищаются.
- Авария приложения не оставляет plaintext handoff на диске: этот путь записи
  отсутствует. После передачи ключ всё ещё нужен в памяти/окружении самого CLI.

Проверены fake UID/PID/start identity, чужой/устаревший запрос, один потребитель,
непоявление decryption при отказе, ошибка vault, отмена во время decryption, watcher,
отказ до старта и повтор. Проверен реальный abstract Unix transport Node и crash
собственного тестового server-процесса. Android socket/peer credentials в JVM
заменены shadows; настоящий Keystore и Android/proot здесь не проверены.

Старые key-файлы не перечислялись, не читались и не удалялись. Legacy consumer
оставлен для прежнего протокола; новый generated launch его не выбирает.
Это не утверждение об удалении всех прежних остатков или хранилищ самих CLI.


## История этапа 2: проход 19 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Gemini после test6: обновления, путь CLI и искусственный ключ

**Исторический статус до приёмки test7: телефонный handoff не был принят.**
Пользователь сообщил: Gemini 0.64.0 в test6
пытается обновиться до 0.66.0; ошибка обновления отображается красной плашкой
и в подробностях. Это сообщение не доказывает отказ передачи ключа или блокировку
запуска. Точный ответ npm/серверов телефона этой проверкой не перехвачен.

### Подтверждённый механизм и локальное исправление

В native schema `general.enableAutoUpdate` и `enableAutoUpdateNotification` по
умолчанию true. Прежний `authEnvironment` создавал только `security.auth` для
API/Google/Vertex, а auto не создавал policy. Значения обновления не задавались.
`getInstallationInfo` не знает наш приватный формат установки и имеет fallback
`npm install -g @google/gemini-cli@latest`. Это не управляемое обновление нашего
закреплённого runtime. Установка при неудаче испускает `update-failed`;
`setUpdateHandler` добавляет `type:error` в native UI history.

Проверка запускается асинхронно после создания React UI и не ожидается перед
чтением ввода. Её ошибку нельзя саму по себе объявлять причиной отказа запуска.
Native relaunch отдельно может ждать незавершённое обновление до 30 секунд.
Воспроизведён exact updater error callback с искусственным spawn, без запуска npm.

Локальная policy использует **поддерживаемые настройки**:
`general.enableAutoUpdate=false`, `general.enableAutoUpdateNotification=true`.
Последняя инструкция пользователя сохраняет проверку версий; ранний вариант
«отключить обе» заменён. Системный per-run слой native merge выше user/workspace:
проверено настоящим `loadSettings`, включая противоречащие искусственные файлы.
Файлы пользователя не переписываются. В auto отсутствует `security.auth`, прежний
способ входа сохраняется. Внешняя system policy не подменяется; конфликт даёт
явный безопасный отказ. Установленные CLI не обновлялись.

Существующий hash-pinned loader отмечает сообщения **только callback обновления**
`jobsNotice:cli-update` и задаёт `type:info` до native addItem. Native recorder
сохраняет только type/content и теряет дополнительный признак; настоящий
useHistory/recordMessage/reopen проверен отдельно. Поэтому будущие уведомления
после открытия истории тоже не становятся error. `GeminiChat` проецирует их как RU/EN уведомления и не
считает ошибками сессии/ответа. Текст произвольной ошибки не служит признаком
обновления. Auth error и обычные ошибки остаются ошибками, даже если затем пришло
уведомление. Сохранённая native история вручную не редактируется.

### Обычный путь и ссылки

Установщик делает tar extraction + mv; сам он ссылку runtime не создаёт.
Но `$HOME`, его предок или runtime могут быть ссылкой: штатный bootstrap передаёт
логический путь, Node загружает канонический URL. Прежний loader сравнивал эти
каталоги буквально и пропускал инструментацию при несовпадении.
Наличие ссылки на телефоне пользователя не установлено; это подтверждённый
условный дефект контроллера, не объявленная причина его конкретного телефона.

Контроллер теперь передаёт **один fs.realpathSync runtime** и в argv, и в
`JOBS_GEMINI_BINARY` дочернего процесса. Файлы CLI не меняются. Стенд больше
не исправляет alias через realpath сам. Проверяются обычный файл и runtime alias,
новый и повторный запуск. При отсутствии UI binding/неподдерживаемой разметке
controller chat-send явно возвращает `NOT_READY`, accepted отсутствует;
модельный запрос не выполняется. Работающий процесс не признаётся готовым чатом.

Свежие проверки: 22 JVM-метода без отказов/ошибок/пропусков, включая один
явный native-метод с 16 API/Vertex shell/write No/Allow сценариями; 10 Node suites,
из них отдельные native settings/update и handler-guard проверки. Повтор после
канонизации обоснован изменением startup path, остальные агенты не перезапускались.

### Ключ: границы доказательства

Настоящий bootstrap, controller, Gemini CLI 0.64.0 и Unix/IPC chat выполнены с
искусственным HOME/проектом/ключом. Native Node и runtime переиспользованы;
способ запуска контроллера соответствует приложению, но Android/proot mount,
Java broker credentials и Keystore не воспроизводятся PTY-стендом.
Kernel network guard разрешает только listeners тестового дерева. Native context
filename искусственный, Git discovery ограничен тестовым корнем.

В четырёх случаях (обычный путь/ссылка × первый/повторный запуск) broker получил
точный run handshake, controller получил ключ, native chat принял сообщение,
локальный provider получил этот искусственный ключ и вернул **HTTP 401**.
Отказ виден в error-строке; CLI остался жив, owned controller завершён штатно.
Скаляр `authError` остаётся false: выбор API-key режима и надпись native
«Authenticated with gemini-api-key» **не означают проверку ключа сервером**.
Отказ проявился на реальном запросе. Исчезновение update-плашки не является
доказательством исправления запуска. Ни реальный Google-вход, ни handoff телефона
этими результатами не закрываются.

### Проверки и оставшееся действие

Доказательства: `.verification/gemini-managed-updates/`, свежий result.json
физического `gla-…` fixture; exact native source hashes в Node result.json.
Ранний отказ guard для неразрешённого Unix root сохранён. Следующий запуск
через runtime alias не подключил hooks; первоначальный канонический workaround
показал механизм, но не считался исправлением. Финальный тест использует
исправленный production controller без workaround. Ошибка первого missing-handler
fixture была в отсутствии обязательного Config.setModel; исправлена в фикстуре,
а STALE_SESSION не переименован в успешный NOT_READY.

Воспроизводимый manual runner: `bash scripts/test-gemini-managed-updates.sh`.
Требует прежний закреплённый локальный runtime; не скачивает CLI и не использует
реальные авторизации. Java native profile regression запускается отдельно только
для Gemini API/Vertex, не повторяет остальных агентов без нового изменения.

**Остаётся разработчику на телефоне:** новая тестовая сборка, отдельный профиль с
искусственным ключом, первый/повторный запуск и запрос до ожидаемого отказа API.
Сообщить режим, этап, код/безопасный текст и порядок действий без ключа.
Проверка нового обновления — обычное уведомление; автоматической установки нет.
Опубликованные APK/тег/Components и рабочие сессии не менялись.


### Финальная проверка test7

Исправление управления обновлениями и canonical startup: `9b881f3a`.
Дополнение persistence: `e78f855d` — info до native записи; настоящий 401 не скрывается.
[Отчёт test7 и короткая проверка телефона](VALIDATION-2.1.3-test7.md).
На момент сборки телефонный broker оставался непроверенным. Последующая
пользовательская проверка ниже принимает только функциональный fake-key путь;
Android ownership/Keystore и настоящий вход остаются непроверенными.


### Пользовательская проверка Gemini test7 — 2026-11-04

Источник — сообщение HezeAI, не самостоятельная проверка помощника. На test7 / 1483
тестовый API-профиль Gemini запускается; сообщение получает **HTTP 400 / API_KEY_INVALID**.
После штатной остановки и повторного запуска результат тот же. Ошибка автообновления
не мешает. Эти пункты приняты как функциональная проверка на телефоне.
Локальный fixture HTTP 401 и пользовательский HTTP 400 не смешиваются.

Не закрыты: настоящий вход API/Vertex, аварийное восстановление, Gemini shell/file
разрешения, mid-handoff отмена/ownership/Keystore и прочие пункты единого плана.
Сохранение/повторное открытие редактора профиля, normal/alias путь телефона и вид
уведомления после history reopen из сообщения не выводятся. Этап 2 остаётся открыт.
[Точная приёмка и остаток](VALIDATION-2.1.3-test7.md#developer-phone-acceptance--2026-11-04).
Обновлена только документация; APK, исходники приложения, авторизации и рабочие
сессии не изменялись. Push/публикация не выполнялись.


## История этапа 2: проход 20 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## 2026-11-04 — A-AUTH-01: обрыв после начала передачи и зависший брокер

**Проверено заново помощником**, независимо от приёмки test7. Исходная база
`97fed729`; production `gemini-control.js` не менялся. Node **v22.22.1**, настоящий
`takeApiKeySocket`, локальные abstract Unix sockets, только искусственные ключи.
Внешняя сеть запрещена существующим kernel guard; перед пробой заново пройдены
его отрицательные сценарии. Рабочие процессы, профили и авторизации не читались.

- Брокер отправил заголовок длины и один байт, затем оставил ответ открытым.
  Настоящий таймер потребителя **10 000 мс** сработал через **10 008 мс**.
  Ошибка строго `Gemini API handoff unavailable`, без подстановки полученных байтов.
  Реальный клиентский сокет уничтожен и имеет `readyState=closed`.
- Во время ожидания независимый искусственный запуск получил свой ключ успешно.
  После отказа новый запуск также прошёл. Нет общего зависшего consumer-состояния.
- Вторая проба дождалась получения потребителем пяти байтов частичного ответа;
  затем завершила только собственный дочерний fake broker через сохранённый
  объект процесса. Подтверждены его exit/SIGKILL, безопасный отказ потребителя,
  работоспособность другого брокера и новый успешный запуск.

Первоначальная версия **теста** завершилась внешним лимитом 40 секунд: она
ожидала событие закрытия серверного сокета с `allowHalfOpen=true`, хотя клиент
уже закрыл свою сторону. Это некорректный критерий фикстуры, не обнаруженное
зависание приложения. Исправленный тест проверяет настоящий клиентский сокет;
серверный half-open peer закрывает отдельно как собственный тестовый ресурс.
Первый timeout, диагностический проход и финальный успешный результат сохранены.

Тест `scripts/tests/test_gemini_key_broker_interrupted.cjs` добавлен в существующий
guarded runner `scripts/test-gemini-managed-updates.sh`. Запущен именно новый тест
под guard; весь ранее принятый набор не повторялся. Syntax-check Node/Bash и
проверка ссылок/diff пройдены. Production-код, APK и опубликованные ресурсы не менялись.
Доказательства: `.verification/gemini-handoff-followup/` в audit worktree,
`summary.json`, `verified.log`, `verified.network.jsonl`, `enforcement.log`.

**Не закрыто:** Android LocalServerSocket/peer credentials, фактический Keystore,
отмена посередине Android handoff, гибель процесса приложения, интерфейс и
реальная авторизация. Node fake broker не заменяет Java broker и проверку
принадлежности Android-запуску. Проверки ключей, разрешений и восстановления
телефона остаются в едином плане выше. Новая находка/исправление приложения этим
проходом не заявляется; подтверждено дополнительное покрытие существующего исправления.


## Урок: 2026-11-03 — Gemini: одноразовая передача принадлежит запуску, не профилю

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Gemini: одноразовая передача принадлежит запуску, не профилю

- Контекст: устранение нового plaintext handoff после crash. Локальный 68413a86 заменяет файл broker-каналом; старые файлы не обследует и не удаляет.
- Вывод: проверять run UUID, kernel UID, дерево PID и start identity владельца. Расшифровывать только для проверенного потребителя, не держать plaintext во время подготовки пакетов. Отмена одного объекта не должна закрывать другой запуск. Ошибка/позднее завершение decryption не возвращает содержимое в диагностику.
- Подтверждение: текущие 122 JVM и Node abstract-transport/crash tests; Android socket/credentials и настоящий Keystore ещё требуют телефона. Не выдавать shadows или отсутствие нового file-write пути за проверку полного Android lifecycle. См. отчёт второго этапа.


## Урок: 2026-11-04 — Gemini 0.64.0: pin, canonical path и тип уведомления

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-04 — Gemini 0.64.0: pin, canonical path и тип уведомления

- Контекст: test6 показывал native automatic-update failure как ошибку чата. Закреплённый каталог сам по себе не выключает updater: general.enableAutoUpdate и enableAutoUpdateNotification по умолчанию true, installationInfo может выбрать global npm fallback.
- Решение: поддерживаемая private per-run system policy задаёт autoUpdate=false, notification=true по последнему поручению; системный слой выше user/project. Auto auth не получает security.auth. Внешняя policy не заменяется. Реальный Google-вход не подтверждён.
- Node canonicalizes module URLs: argv runtime и JOBS_GEMINI_BINARY должны содержать один realpath, иначе runtime/HOME symlink оставляет loader без hooks. Нормализация в стенде скрывала штатный дефект; production fix проверен без неё, normal/alias × 2.
- Native useHistory сохраняет произвольные metadata только в UI; recordMessage пишет type/content, поэтому jobsNotice теряется после reopen. Только updater callback получает type:info до записи; обычный HTTP401 остаётся error. Проверено настоящими useHistory и convertSessionToHistoryFormats с искусственными данными.
- Доказательства: SECURITY-AUDIT-STAGE2.md / VALIDATION-2.1.3-test7.md; .verification/gemini-managed-updates. Настоящие bootstrap/controller/CLI 0.64.0 дошли до локального provider401 с fake key; owned stop ack после child exit. Native banner Authenticated и authError=false обозначают режим, а не принятие ключа сервером. Android/Keystore/handoff телефона остаются открытыми.

- Дополнение к Gemini test7, 2026-11-04, источник — отчёт пользователя: тестовый API-профиль запускается, запрос получает HTTP400/API_KEY_INVALID, после штатной остановки и повторного запуска результат тот же; updater не мешает. Функциональный fake-key путь принят пользователем, отдельно от самостоятельного fixture401. Настоящий вход, аварийное восстановление, права Gemini и низкоуровневые ownership/Keystore проверки этим не закрыты. См. приёмку в VALIDATION-2.1.3-test7.md; не повторять принятые пункты без нового изменения.
