# Проверки локальной v1.0.5 — AI Agents

Не опубликованный GitHub-релиз. Package com.termux, Android 7+/ARM64,
minSdk 24, targetSdk 28, release без android:debuggable.
Новая сборка: versionName 1.0.5 / versionCode 1042.
Резерв: versionName 1.0.4-rollback / versionCode 1100.

Обе подписи: APK Signature Scheme v2; сертификат SHA-256
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Он совпадает с публичной v1.0.4. APK ZIP CRC и выравнивание несжатых записей
проверены; bootstrap/native libraries и прежние assets побайтово сохранены.
Новый asset — только ai-agents-control.js. APK не содержит auth.json, пользовательских
keystore/.codex/SQLite-баз и локальных инструментов проверки. В новые assets
не добавлялись пользовательские ключи или секреты.

| APK | Байт | SHA-256 |
| --- | ---: | --- |
| Termux-Jobs-v1.0.5-arm64.apk | 34233649 | `4adcc889bcd7a5c7f127eab25d60a73917f4f80f18cea8df995511b71b875a51` |
| Termux-Jobs-v1.0.4-rollback-code1100.apk | 34185548 | `6c4375f52789a5a966f4cfde3c5c446b9da0c5a86b9c3f2566af2f7c476b2725` |

## Выполнено

- Полный локальный прогон: 260 тестов, 0 failures/errors/skipped.
- Release assemble/lintVital успешны. RU/EN: 513 workspace strings,
  остальные app/shared ключи и форматные аргументы проверены.
- Node.js синтаксис ai-agents-control.js; bash синтаксис build-local-arm64.sh.
- Изолированная проверка официального OpenCode v2.0.28 ARM64 в текущей Ubuntu:
  каталог моделей с очищенным выводом; создание сессии, status и stop;
  повторное открытие того же ID после перезапуска сервера. Inference и личные
  ключи не использовались. После проверки собственные OpenCode-процессы отсутствуют.
- Дополнительный Robolectric-тест с фактическим WorkspaceStore прежнего коммита
  18e88e8794b3be670d4c368a2e3046c0b2b2ab87 принимает новую v8 библиотеку:
  Codex thread, команды/заметки сохранены, OpenCode-профиль читается как Ubuntu.
  Это проверка reader, не установка/проверка Android миграции на телефоне.
- CodexSessionShell/Rpc/Status/ModelSelection побайтово не изменены; регрессионные
  тесты выбора доступа/истории и изоляции новых профилей проходят.
- Проверены stdin-передача ключа, allowlist live-сокета, отсечение полей settings,
  отключение сохранения состояния поля API-ключа, неперенос чужих ambient ключей
  в именованные подключения. История с неподдерживаемым ID не подменяется новым чатом.
- Финальные файлы в /sdcard/Download перечитаны; SHA-256 совпадают.

## Не проверено на установленном приложении

Авторизация/настоящий inference провайдеров, установка через npm на чистой Ubuntu,
клавиатура OpenCode mini, жесты/DPI этих диалогов, Android process lifecycle,
SIGKILL/OOM и установочный откат на устройстве. Нельзя выдавать mock/native-probe
за успешные проверки этих сценариев. Первая ручная проверка: Codex прежнего
профиля; OpenCode установка/ключ/модель/сообщение; сохранить/закрыть/продолжить
тот же чат; проверить соседнюю вкладку при закрытии.

Отдельные Claude Code и Gemini CLI, OAuth OpenCode, custom baseURL и Android
permission-card из PNG не входят в первый этап. Лимиты OpenCode не выдумываются.
Ключи OpenCode хранятся его локальным механизмом, не Android Keystore.
Полные границы и откат: [AI-AGENTS.md](AI-AGENTS.md).

## Локальные свидетельства

.tools/ai-agents/build.log, full-suite.json, rollback-reader.log/json,
runtime/probe-shipped.log, artifacts.json, badging.txt, signature.txt.
Резерв исходников и журналы первой сборки — .tools/ai-agents/rollback.
Эти файлы и личные/генерированные дизайн-артефакты не публиковались.
