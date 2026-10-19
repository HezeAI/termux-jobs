# Historical private build records — v1.5.4-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# Private v1.5.4-test validation

Final APK: code1141, com.termux, ARM64, minSDK26, 104864440 bytes. SHA-256: d681359bd9641571c8545df484ef0d50c36be0c84a35185bfe4c42594ebc3b8a. Existing certificate dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644, v2 signature verified. ZIP CRC, package/version/ABI, DEX private gate, reviewed runtime definitions, limited secret-file/marker scan and unchanged pre-existing native/external CLI/server-agent payloads passed. Download copy independently hashed.

Full application run: 592 tests, 591 passed and one new history test failed on JSON slash escaping. A prior new server test also needed its fixture profile registered in the saved-profile list. Test-only fixes followed: LocalAiContextTest and ServerWorkspaceTest passed together, with terminal-emulator and terminal-view suites; termux-shared has no tests. See targeted-summary.json. Real SDK0.17.0 interface probe: 10 passed under Java21, fake engine/conversation, no hardware inference. Source diff check passed.

Review covered native invalid-argument validation and correction, own-chat recall without inferred filesystem actions, disconnect profile/activity isolation, blocking late callbacks and newly queued transports, picker callback invalidation, explicit reconnect, and keeping independent terminals/remote tasks outside local shutdown. No working user process or remote server was manipulated for these checks.

Device checks remain: connect/disconnect/reconnect, transfer/media interruption, delayed Android storage permissions, actual keyboard/tile appearance, native model repair/recall and actual image/video/audio inference. SDK/unit tests do not establish NPU or media hardware behavior. Compiled NPU context remains4096. No automatic data reset or published release.

Evidence: .verification/litert-servers-v154/{build.log,tests-rerun.log,tests-targeted.log,full-suite-summary.json,targeted-summary.json,sdk-tests.log,context-budget.txt,1.5.4-test/artifact.json}. Context counts are character-based estimates, not actual tokenizer counts.


---

## Subsequent development record

# Частная проверка v1.5.4-test — 2026-10-18

Локальный APK для проверки разработчиком. Публикация GitHub не выполнялась.
Стабильным публичным выпуском остаётся v1.5.3.

## Что изменено

- Новые профили Codex используют Ubuntu, отдельный переключатель среды убран.
  Старые профили Termux сохраняют среду/пути/историю; редактор сообщает об этом.
- Для Gemini нижняя кнопка отправки делает вставку + один отложенный Enter160мс.
  Переключение сессии/режима, закрытие и изменение текста отменяют отложенный Enter.
  Интерактивная клавиатура управляется прежним переключателем. Другие send-пути не изменены.
- Локальная живая карточка Gemini показывает модель, режим, состояние, запросы,
  токены/кэш/рассуждения и время текущего запуска. Остаток квоты недоступен.
- Выбор модели использует временный native Config.setModel: ожидание idle,
  отмена при ручной смене в CLI, сохранение чата и авторизации.
- Наблюдение за pinned CLI0.64.0 добавляется в память через загрузчик;
  файлы установленного SDK не переписываются. Несовместимость отключает карточку.
  Частный socket/скалярный JSON не содержат ключей и текстов диалога.

## Выполненные проверки

- 501 JVM-тест: 356 app +145 terminal-emulator; failures/errors/skipped0.
  Проверены свежесть/UUID снимка, недопустимые модели, отложенный единичный Enter,
  отмена и двойная отправка, новый и прежний Termux-профиль Codex.
- Node check-gemini-live: приватность, native setter/очередь/отмена, изоляция каталога,
  отсутствие редиректов ключа, шестёрка точных layouts, fallback несовместимости,
  реальные controller/socket/status/model с синтетическим owned child и остановка.
- check-gemini-runtime: собственные graceful/forced stop, parent loss, restart,
  сохранность постороннего процесса, ошибки запуска и аргументы.
- check-gemini-auth: штатный/Google/API, права0600, отказ перезаписи внешней политики,
  авторизация при двух restart и противоречивом IPC, owned stop cleanup.
- Реальный установленный CLI0.64.0 --version с загрузчиком, изолированным HOME;
  без inference-запросов и изменений его исходников.
- Локализация: 584 workspace-строки и параметры настроек app/shared.
- ARM64 release-сборка успешна. Проверены ZIP, package/version/minSdk/ABI/debug,
  APK v2-подпись/сертификат, SHA-256, отсутствие приватных файлов и маркеров секретов.
- Нативные библиотеки, bootstrap Gemini, control-assets Codex/Claude/OpenCode,
  server agent и backup asset побайтно совпадают с v1.5.3.
- Runtime inventory: 12883 определённых класса, обязательные определения на месте;
  31 прежний рассмотренный optional type, новых непроверенных missing types нет.
- Есть прежние предупреждения AGP/aapt2/Kotlin metadata и Node experimental loader;
  сборка и проверки завершились успешно. Полная физическая проверка из этого не следует.

## Совместимость

com.termux, code1166 >1165, Android8+ / arm64-v8a. Подпись как у v1.5.3.
Формат библиотеки8 сохранён; данные, ключи, история и окружения автоматически
не очищаются и не переносятся. Новых системных разрешений/служб нет.
Возможность обновления поверх подтверждена идентичностью package/certificate
и большим code; все сценарии сохранности пользовательских данных на устройстве
этими проверками не доказываются.

Новая карточка требует нового запуска Gemini; старые работающие контроллеры
не переписывались и продолжат работать без неё. Остальные агенты не перезапускались.
Реальное переключение моделей/доступность Google API-каталога, отправка одного/
многострочного текста, клавиатура/жесты/DPI и поведение при auth/ответе должны быть
проверены разработчиком в новом Gemini-чате на устройстве. Для отмены в160мс
уже вставленный текст остаётся в CLI: отправьте его интерактивно без повторной вставки.
Модель в карточке считается применённой после native setter; backend ещё может
отклонить её при следующем запросе. Цены/лимиты из каталога не выводятся.

## Артефакт

- APK: `Termux-Jobs-v1.5.4-test-arm64.apk`
- Размер: 36529928 bytes.
- SHA-256: `973cafe6fe3ca1dcdec3839549ce873e78e147d4f45eb2db6ab0809816ee3e7e`
- Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
