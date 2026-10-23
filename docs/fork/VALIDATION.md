# Current validation — v2.0.1

[Artifact, tests and limits](VALIDATION-v2.0.1.md). Full Java869 app+154 emulator
passed; final JSON correction covered by35 backup/store tests. Full Python546
with one skip, then303 final X11 tests passed; all21 Node suites passed.
ARM64 release APK, ZIP/runtime/package/version/signature/hash checks passed.
Android8+/ARM64, code1325, schema8, same package/signer, no data reset.
APK45794876bytes, SHA256
`a0a2e86abfb3da4f91e24d3e3c824ecc6f0ca537d2f71fa04fb8f86361ebb7ca`.
GPU overlap/performance remains a device check; the report distinguishes native
fragment tests from Android GUI tests and records lint-toolchain limitations.
Publication/download verification will be recorded after the release upload.

Historical validation records follow.

# Historical validation — v2.0.0

[Final artifact, tests and limits](VALIDATION-v2.0.0.md).
862 app +154 emulator JVM tests pass. Full Python discovery544 with one skip;
three stale fixture expectations were corrected and34 affected tests rerun green.
Android8+/ARM64, unchanged signer/package, code1320, schema8, no general reset.
Developer reports RedMagic10Pro and SamsungS26Ultra checks; other devices require
verification. Final Ubuntu VirGL GPU INFO and all-combination fresh-device/GUI
coverage remain limited as described in the report. Sources: [THIRD-PARTY](THIRD-PARTY.md).


Publication verified2026-10-23: [v2.0.0 stable/latest](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.0).
All four assets were downloaded and compared. APK SHA256
`492626100bb3500145fa84f28d030b8cac3e00841967ceff64cf080fae6b5dda`,
45794028bytes; package/code/signature/ZIP and updater size/hash match. Public
unauthenticated update.json equals the generated metadata; seven tagged current
documentation links return200. No APK rebuild or tag movement for this record.

The previous sections below are historical release records.

# Текущие проверки — v1.7.5

[Итоговый отчёт](VALIDATION-v1.7.5.md):697 JVM-тестов финального поведения,
171 Python-тест неизменённого Python-кода; Node/native fixtures, release APK,
ZIP/runtime/package/version/ABI/подпись/SHA-256 проверены. Разработчик подтвердил
подгрузку истории и новый запуск Codex на1.7.5-test. Не заявляется полный
device coverage очистки/удаления/миграций. Code1220, Android8+/ARM64, schema8.
[Изменения и совместимость](RELEASE-v1.7.5.md).
Stable/latest v1.7.5 опубликован: APK, SHA256SUMS, update.json и отчёт скачаны
обратно и совпали; release body, документы в теге, анонимные latest API и
update.json проверены. Подготовленный отчёт в теге фиксирует состояние до
публикации; эта запись добавляет результаты обратной проверки.

Ниже исторические отчёты, не результаты текущей версии.

# Исторические проверки — v1.7.0

[Итоговый отчёт](VALIDATION-v1.7.0.md):668 JVM-тестов и146 Python-тестов,
release-сборка и runtime/ZIP/package/version/ABI/подпись/SHA-256 проверены.
Пакеты и полный цикл бэкапа/отдельного восстановления шести систем проверены
разработчиком. Удаление защищено штатными блокировками; новый Android-экран
удаления требует проверки на ненужной копии. Совместимость и ограничения —
в [описании релиза](RELEASE-v1.7.0.md). code1215, Android8+/ARM64, schema8.
Stable/latest опубликован: четыре assets скачаны обратно и совпали;
анонимные latest API/update.json и три публичные ссылки документации проверены.

Ниже исторические отчёты, не результаты текущей версии.

# Исторические проверки — v1.6.5 fix

[Итоговый отчёт](VALIDATION-v1.6.5.md):644 JVM-теста,119 Python-тестов и20 наборов
Node/native fixtures. Проверены фокус ввода, карточки/навыки, загрузка большой
истории Codex и поиск компонентов OpenCode. Финальная release-сборка,
runtime/ZIP/package/version/ABI/подпись/SHA и schema1 update.json проверены.
Assets/native совпадают с проверенной разработчиком1.6.5-test; code1198.
Подтверждения разработчика отделены от автоматических проверок. Полный чистый
Lint, все устройства/жесты и real-account Claude/Vertex не заявляются.
[Совместимость и ограничения](RELEASE-v1.6.5.md).
Stable/latest v1.6.5 опубликован: четыре файла скачаны обратно и совпали,
анонимные latest/update.json и публичная документация тега проверены.

Ниже исторические отчёты; они не являются проверками текущего выпуска.

# Исторические проверки — v1.6.0

[Итоговый отчёт](VALIDATION-v1.6.0.md):630 JVM-тестов полного запуска,
дополнительная проверка совместимости schema8,119 Python-тестов и19 наборов
Node/native-CLI fixtures. Release-сборка и runtime/ZIP/package/version/ABI/
подпись/SHA-256 проверены. Все assets/native libraries совпадают с проверенной
разработчиком v1.5.33. Чистый полный Lint не заявляется; реальные Cloud-сценарии
Claude/Vertex и полный набор Android UI/обновления не объявляются проверенными.
Stable/latest v1.6.0 и четыре скачанных assets проверены: metadata/hash/size/code
совпадают, публичные ссылки доступны.
[Совместимость](RELEASE-v1.6.0.md) · [исследование удалённых local AI](LOCAL-AI-RESEARCH.md).

Ниже исторические отчёты; они не являются проверками текущего выпуска.

## Previously published v1.5.3 validation (historical)

# Текущие проверки — v1.5.3

[Итоговый отчёт](VALIDATION-v1.5.3.md):496 JVM-тестов (неизменённый emulator
UP-TO-DATE),119 Python-тестов,11 сценариев прав Codex. Release-сборка,
пакет/версия/ABI/подпись/SHA-256 и runtime inventory проверены.
Есть прежние ограничения части Lint; полный чистый Lint не заявляется.
Проверки кода/сборки отделены от проверки на устройстве. Stable/latest v1.5.3
проверена: четыре скачанных release assets совпали, подпись/метаданные верны,
ссылки доступны; исходный тег и APK не менялись.

[Исследование удалённых локальных ИИ](LOCAL-AI-RESEARCH.md).
Предупреждение о приватных локальных данных: [совместимость](RELEASE-v1.5.3.md).

Ниже исторические отчёты; они не являются проверками текущего выпуска.

---

# Исторические проверки — v1.5.0

[Отчёт](VALIDATION-v1.5.0.md):724 теста полной конфигурации,153 адресная проверка публичной, реальная проверка SFTP на отдельных тестовых файлах. Итоговые APK/подпись/версия/SHA-256 проверены. Проверки кода и сборки отделены от сценариев на устройстве.

Ниже сохранены исторические отчёты; они не заменяют проверки v1.5.0.

# Исторические проверки — v1.3.3

[Отчёт](VALIDATION-v1.3.3.md):440 JVM-тестов, исправление Guava, проверка
runtime-классов в APK и файловых операций. Android8.0+/API26.
[Предыдущий отчёт](VALIDATION-v1.3.2.md).

Ниже — исторический отчёт v1.2.0/v1.2.1; его результаты не являются проверкой новых сценариев.

# Проверки Termux Job’s v1.2.0

## v1.2.1 — ускорение отправки и очередей сервера

SFTP отправляет до8блоков по одному SSH-соединению; изолированы очереди
мониторинга и поиска/диагностики. Разработчик подтвердил ускорение отправки
примерно4–5раз на своём подключении; результат зависит от сети и сервера.
Защиты файлов/отмены сохранены, данные без сброса, агент прежний.

[Изменения](RELEASE-v1.2.1.md) · [Проверки](VALIDATION-v1.2.1.md).



Финальный APK `Termux-Jobs-v1.2.0-arm64.apk`: 36476761 bytes (~36.5МБ), SHA-256
`d270bfd36a7515ab6facbb21586db7087ea2870a253b227cb315cbe4202a70e8`. com.termux/versionName1.2.0/code1071 >1061 публичнойv1.1.4
и >1070 локальнойv1.1.13. Android min24/target28, толькоARM64, non-debuggable.
Сертификат SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`, прежний для публичнойv1.1.4
и локальных сборок. apksigner APKv2 PASS. ZIP CRC/duplicate/traversal/stored4byte
alignment PASS. Java/resources/assets совпадают с release stage. Native побайтно
идентичны публичнойv1.1.4; agent.py идентичен локальнымv1.1.12/13.
Ограниченный скан приватных ключей/токенов/auth/keystore не выявил совпадений;
он не является универсальной гарантией отсутствия секретов.

## Проверки кода

- 379 JVM tests,0 failures/errors/skipped: финальный код проверен на локальной
  v1.1.13; вv1.2.0 менялись только versionName/code и документация. Повторный
  прогон неизменённого поведения не заявляется. Лог jvm-final2.log сохранён
  из .verification/servers-v1.1.13; исходники Java/resources сверены со stagev1.2.0.
- 6 Python search +20 agent tests PASS на финальных неизменённых helpersv1.1.13.
- 15 Python schedules tests повторно PASS при подготовкеv1.2.0.
- Включены profile roundtrip/старые поля/изоляция, подтверждения без remote actions,
  UI-кнопки, Filename search limits/literal/Unicode/symlink/partial, progress/ETA,
  finished-record deletion с запретом active и без сигналов/удаления результатов,
  runner locks/enable/disable/edit/delete/failure, fakecron/systemd, собственные
  дочерние процессы, editor revision-conflict/несохранённый текст, existing terminal tests.
- Release build успешен. Localization581 PASS наv1.1.13, новые RU/EN проверены
  в исходниках/целевых тестах. git diff --check и проверка документационных ссылок.
- Полный чистый Lint не заявляется: прежние lintVital/JNA aarch64/Kotlin metadata/
  custom lint несовместимости. Итоговую сборку это не заблокировало.

## Ревью и границы

Прослежены профиль/подключение/hostpin/password, раздельное владение SSH-процессами,
cancel/close/download cleanup, agent admission/unmanaged/forget, scheduler create/
edit/disable/delete/running locks, внешняя конкуренция crontab, player lifecycle/
loopback-only HTTP и безопасные ошибки. Использованы текущие права SSH, без
автоматического sudo/linger. Agent и расписания не удаляют чужие проекты/службы.
Смена endpoint сохраняет local favorites/actions, запуск показывает новую цель.
Новые поля необязательные; библиотекаv8/AI/окружения без миграции и сброса.

## Что не доказано локальными тестами

Разработчик HezeAI установил и проверил промежуточные сборки на устройстве.
Разработка велась с помощью AI-ассистента; локальные автоматические проверки
описаны выше. Отдельной проверки в целевой среде требуют разрешения, клавиатура,
жесты/DPI, фактическая скорость и отмена SSH/SFTP, поддержка конкретных кодеков,
настоящие cron daemon/user-manager/linger/reboot и полное восстановление.
Автоматические проверки cron/systemd используют подставные команды.

## Обновлятор и публикация

schema1 update.json создан из финального APK, RU/EN highlights содержат совместимость.
Пакет/подпись/код/размер/SHA согласованы. Latest должна содержать APK/SHA256SUMS/
update.json, автоматически проверяется при открытии при включённой автопроверке;
ручная проверка сохранена. Это предложение обновления, не push/автоустановка.
Сверка загруженных assets/тега/ссылок фиксируется отдельной квитанцией публикации.
Доказательства: .verification/release-v1.2.0 и servers-v1.1.13.

## Исторические проверки

[v1.1.4](https://github.com/HezeAI/termux-jobs/blob/v1.1.4/docs/fork/VALIDATION.md) · [v1.1.13](VALIDATION-v1.1.13.md).
