# Historical private build records — v1.5.12-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.12-test validation

## Evidence and scope

Source reproduction before the fix: `.verification/litert-tool-diagnosis/AdapterReviewProbe.java` and `adapter-review-results.txt`. The original short Russian request with a Gemma fragment yielded one generation, zero executions, no repair flags, and unchanged raw output. This is a coordinator reproduction with synthetic inference, not a claim to reproduce native generation.

16 standalone real-SDK interface tests pass (`.verification/local-ai-v1512/sdk-results.log`). Inference is fake: the actual SDK Message/Config classes are used. The added adapter test feeds leaked native syntax through callbacks, verifies `unparsed_native_tool`, disabled automatic execution, closed conversation and unchanged draft. This does not test NPU hardware or establish why the model omitted its closing marker.

Automated recovery cases cover the reported wording, exact marker classification, quoted/prose examples, hidden draft presentation without execution, correct fresh SDK response, two-retry bound, chat/read permissions, approval, cancellation, Qwen/ordinary-text behavior, rejection of unstructured JSON in native mode and feedback function identification. Native retry context excludes broken assistant fragments and fake tool-result messages; original user request stays intact. Final suite results are recorded below after completion.

## APK

Release ARM64 build and required runtime-class audit pass. Existing Kotlin metadata compatibility diagnostics remain in build output; device compatibility is not inferred from successful DEX compilation.

ZIP CRC, duplicate/traversal checks, com.termux identity, minSDK26, versionCode1149, ARM64-only ABI, private local-AI flag, v2 signature/certificate, limited embedded-secret scan, seven QNN/LiteRT payloads, unchanged earlier native payloads and agent assets pass. Certificate matches previous private delivery. No reset/migration. No publication, user-process changes or installed environment changes performed.

SHA-256: `54571ddb341361072f37e48252935eab8a34987953cf36aa8bbe32c94caec516`
Size: 104888496 bytes.

## Remaining device checks

Repeat the original natural-language guide request in a fresh Gemma Native chat at the same reply cap. Export diagnostics and verify the resulting file. Check ordinary follow-up, cancellation during repair, and permission/approval UI. Recovery can stop clearly after two retries; it does not guarantee the model will generate a complete call. No token-limit diagnosis or weight changes claimed.

## Final suite and delivery

Full final Java suite: 640 app cases, 145 terminal-emulator cases; zero failures/errors/skips. All ten Gemma recovery cases pass. Logs: `.verification/local-ai-v1512/tests-verified.log`, `test-counts.json`. Earlier compilation checks caught Java escaping and a test exception declaration; both corrected before this passing run. Final APK Download copy independently SHA-256 verified.


---

## Subsequent development record

# v1.5.12-test — проверочный возврат resize replay OpenCode

Частная сборка code1174 для проверки разработчиком.
Основной публичный релиз остаётся v1.5.3/code1165.

## Причина и изменение

Разработчик обнаружил в1.5.11-test перемешивание сообщений/ответа и нижней панели
OpenCode после изменений геометрии. Скриншот подтверждает нарушение отображения;
повреждение сохранённой native истории этим не доказано.

Адаптер снова задаёт `TERMUX_JOBS_DISABLE_RESIZE_REPLAY=0`. Штатный механизм
OpenCode mini сбрасывает footer и восстанавливает историю при изменении высоты,
как уже делал для ширины. Immutable jobs2 bundle, его hash и исходный патч прежние.
Дельта production против1.5.11: флаг, поясняющий комментарий и версия.
Карточки подтверждений/записи разрешений OpenCode/Gemini, Android-стабилизация
геометрии и подавление resize скрытой/неизменённой вкладки сохранены.

Это откат опасного отключения восстановления. Он может вернуть видимый replay
при клавиатуре; исправление рывков не заявляется. Нет inference для replay,
повторной отправки пользовательских запросов или изменения политики разрешений.

## Воспроизводимые проверки

Материалы: `.verification/opencode-order-v1.5.12/` (игнорируются Git).

- Настоящий OpenTUI0.5.17 в собственном PTY, split-footer/captured stdout,
  Termux TerminalEmulator: 40 начальных строк, уменьшение35→19, последующий
  блок USER_TWO/ANSWER_TWO, увеличение19→35 и блок USER_THREE/ANSWER_THREE.
  Без reset/replay блок TWO теряется; с reset/replay все маркеры идут в порядке.
  Это дифференциальная проверка рендерера, а не ответ настоящей модели.
- Настоящий OpenCode2.0.28 serve и jobs2 mini в отдельном HOME/XDG/каталоге
  репозитория: начальная native история, шесть изменений высоты/ширины,
  native pending→once→receipt, ещё два изменения высоты, наличие записи после
  них, просмотр подробностей, переоткрытие модели с тем же чатом, owned stop.
  Итог:64 DEC2026 frames до reopen,9 выводов seed до reopen,10 после;
  native ID/история сохранились. Proxy запрещал inference.
- Отдельная попытка вызвать session shell во время native smoke не вывела
  ожидаемые маркеры; model reopen получил busy. Эта попытка НЕ считается
  успешной проверкой продолжающегося ответа. Собственные тестовые процессы
  завершены через обработчик остановки; пользовательские сессии не затронуты.
-75 upstream runtime/transport tests прошли, включая буферизацию событий при
  replay и отсутствие повторного suffix. Исходники frontend в этой версии
  не менялись; полный upstream check не повторялся. Ограничение oxlint loader
  предыдущей проверки описано в VALIDATION-v1.5.11-test.md.
- Node: receipt persistence/privacy/stale/double/foreign guards; все три
  pinned Gemini receipt wrappers; check-gemini-live; синтаксис адаптера — успешно.

## Проверки сборки

552 JVM теста:398 app +154 terminal-emulator,0 failures/errors/skips.
Gradle verify-java завершился успешно; присутствовали прежние предупреждения
SDK/UAST и dispose тестового ресурса. Release ARM64 build — BUILD SUCCESSFUL. Runtime:12925 defined classes,
31 отсутствующий optional тип с прежними review notes. Lint инфраструктура
по-прежнему выводит Linux/aarch64 JNA, Vendor/UAST и Kotlin metadata сообщения;
это не заявляется как чистый полноценный Lint.

Финальный APK:

- `Termux-Jobs-v1.5.12-test-arm64.apk` — 113533859 bytes.
- SHA-256: `14e7510c82fd50ce4209e7f32f244ced8e1ffb169a7f4993b27e18c329c0e5c1`.
- Package com.termux,1.5.12-test/code1174,minSdk26,только arm64-v8a.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP, подпись v2, версии и ABI проверены. Package/certificate прежние, код выше1173.
- Все остальные assets и native библиотеки побайтово совпали с1.5.11.
- Bundle manifest/binary hash, ELF ARM64, MIT license, отсутствие приватных
  файлов и известных маркеров секретов проверены.
- Копия в Download проверена по SHA-256; рядом файл .sha256.

## Границы проверки

Сценарий реального streaming ответа с клавиатурой и изменением extra keys требует
проверки разработчиком на устройстве. Сначала открыть новую OpenCode-сессию,
получить несколько ответов, открыть/закрыть клавиатуру и продолжить диалог.
Проверить порядок текста, отсутствие footer внутри ответа, сохранность облачка
разрешения и возвращение в чат с другой вкладки. Один resize может перерисовать
историю; если текст всё ещё смешивается, сравнить jobs2 со штатным mini при
одинаковом сценарии. Исправление всех resize-сценариев не заявляется.

Существующий процесс OpenCode не заменяется обновлением адаптера на диске;
проверять новый запуск/переоткрытие сессии. Codex, Claude, Gemini, SSH и shell
не получают этот флаг. Данные/пресеты не очищаются, миграций нет. Package/signing
identity и повышение кода подтверждаются аудитом APK. Следующий стабильный код
должен превышать1174. Установку выполняет пользователь; GitHub не публикуется.
