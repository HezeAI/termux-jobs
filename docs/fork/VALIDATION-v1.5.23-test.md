# Historical private build records — v1.5.23-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# Проверки v1.5.23-test

Приватная сборка для проверки разработчиком; публикация не выполнялась.

## Артефакт

- APK: `Termux-Jobs-v1.5.23-test-arm64.apk`.
- Package: `com.termux`, versionName `1.5.23-test`, versionCode `1160` (>1159).
- Android 8+ (minSdk26), ABI `arm64-v8a`.
- Размер: 36511341 байт.
- SHA-256: `16a6c358c960d39387ccde0253f3eb954a6f829fad4d6a04f090daeac4ec9524`.
- SHA-256 сертификата: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` — совпадает с v1.5.22-test.

ZIP CRC, подпись v2, package/version/ABI и runtime inventory проверены на финальном APK.
Осталось 31 отсутствующее определение типов с существующими явными пояснениями
в runtime-аудите; обязательные определения присутствуют. Ограниченное сканирование
артефакта на секреты прошло; оно не является доказательством отсутствия любого
возможного секрета. В APK не вернулись классы/библиотеки локальных ИИ.
Прежние native-библиотеки и assets внешних агентов совпадают с v1.5.22-test.

## Автоматические проверки

- 337 тестов приложения: без ошибок, падений и пропусков.
- 145 тестов неизменённого terminal-emulator: успешный ранее полученный результат,
  Gradle UP-TO-DATE, повторное выполнение не заявляется.
- 8 Python-тестов удаления: проверка всей выборки до изменения, рекурсивные папки,
  сохранение целей символьных ссылок, изменение объекта/родителя, лимиты,
  частичная ошибка без повтора, запуск помощника через stdin.
- UI-тесты покрывают переключение выбора/отмену/подтверждение,
  независимые строки загрузки/скачивания, сохранение идентичности View при прогрессе,
  скрытие и возврат того же окна, геометрию индикаторов и выбор иконок агента.
- Локализация: 584 строки, ключи и аргументы формата согласованы.
- Изолированная release-сборка, runtime-проверка и git diff --check прошли.

Логи/результаты: `.verification/server-delete-v1523/`.
При инспекции прослежены выбор/смена папки/подтверждение/ошибка удаления,
переключение вкладок, скрытие/возврат/завершение передач и жизненный цикл анимации.
Прогресс и анимации обновляют собственные View/Drawable, не пересоздают терминал.
Удаление передаёт фиксированный Python-helper через существующий pinned SSH,
не добавляет shell-интерполяцию имён и не меняет установленного агента.

## Совместимость и границы проверки

Schema8 и текущие данные сохранены, автоматического сброса нет. Для обновления
с версии до1.5.22 сохраняется преднамеренное удаление приватных локальных моделей,
их историй/настроек; см. RELEASE-v1.5.22-test.md. Истории внешних CLI-агентов
не затрагиваются. Пакет/сертификат допускают обновление поверх, что само по себе
не заменяет проверку каждого сценария миграции.

Реальное удаление на пользовательском сервере не выполнялось: использованы
изолированные файловые fixtures. Нужна проверка на ненужном файле/папке,
пустого выбора и отказа «Нет». Обрыв удаления может оставить частичный результат;
автоповтора/отката нет. Поддерживается Linux/Python3, установленные лимиты описаны
в RELEASE-v1.5.23-test.md.

На устройстве проверить обе передачи с «В фон», переключение вкладок, возврат
к прогрессу/отмене, клавиатуру/ввод и прокрутку во время анимации. Robolectric
не подтверждает жесты, DPI и Android-жизненный цикл. Передачи продолжаются
в живом приложении, не защищены от уничтожения процесса и не имеют автопродолжения.
Индикаторы показывают доступную активность вывода/локальной задачи, а не точную
занятость всех агентов или тихого удалённого SSH-процесса.


---

## Subsequent development record

# v1.5.23-test — selection-only Select all controls

Private update to1.5.22-test/code1184. Session file browser: Select all appears in
the left footer, opposite Close, only after selection begins. It adds visible
entries using the same search/type filter as the displayed list; selected entries
from other folders remain.50-entry cap retained with explanation. Dialog stays open.
File/directory/import/upload pickers do not acquire this selection action.

Server: `× N` and a vector Select all icon share the transfer row, with8dp spacing
and independent measured slots. Transfer arrows narrow to share the available
width. The selection group disappears when selection is
empty. It chooses current listing entries, keeps earlier folder selections and
reports the500-entry cap. The all action is unavailable while a move is already
prepared; it cannot silently replace the pending move. Row opening, checkboxes,
last-checkbox cancellation and the pending move's clear action are retained.

No new I/O, poller, credentials, permission, copy/move/delete implementation or
server-agent change. Selection uses bounded existing in-memory collections. No
installed environment or user/server files are used for verification.

Focused tests include: initial/cleared action hiding; visible filter/search matching;
retention from another folder; selection caps; non-dismissing footer; shared
server-row parentage and measured non-overlap at296dp usable width (320dp with
side padding), enlarged count text and at least48dp arrow targets;
download admission validating the entire selection (including an oversized second
entry), plus existing file operation, picker, server and agent-chat regressions. Mock geometry
is evidence for that layout case, not physical gesture/DPI certification.

Review: zero/active/busy/pending states, preserving picker-specific neutral buttons,
shared visibility predicate, no selection-triggered mutations, bounded additions,
server-profile isolation and independent layout slots. No data reset or schema
migration is introduced. Developer should check the server row on the phone and
select-all with a filter and after navigation.

136 focused JVM tests pass, zero failures/errors/skips. Initial two new session
button tests ran before the dialog's onShow callback; the fixture now drains its
main looper before clicking. Final tests include the actual configured listener.
Evidence: .verification/select-all-v1.5.23/tests-delivery.log and tests.json.

Download data-flow review: select-all populates the same downloadSelection map as
individual checkbox selection; the download action snapshots all map values, and
the existing downloader collects every path before asking for a destination. Its
per-file/batch limits and confirmation remain. No transfer starts on Select all.

## Final artifact

`Termux-Jobs-v1.5.23-test-arm64.apk`, 36615662 bytes. Package com.termux,
version1.5.23-test/code1185, minSdk26, ARM64. Same signing certificate as1.5.22,
increased code, no data reset/schema migration.
SHA-256: `e5f8c94e3de5d1aa76948fdf90c61a46cf2aeb8df54dd384d7d68889229a3ff2`.

Assembly, packaged required runtime definitions, ZIP integrity, package/ABI,
v2 signature, private-file/secret-marker and staged-source/resource checks pass.
All packaged assets and native libraries are byte-identical to1.5.22; the vector
is an Android resource. Existing Lint Kotlin/UAST/JNA infrastructure warnings and
31 reviewed optional missing types remain; not a clean-Lint claim. Robolectric
CloseGuard/Gradle filesystem-watch notices remain in test logs.

Download copy hash verified. Evidence in .verification/select-all-v1.5.23 includes
build.log, staged-source.json, apk-audit.json, signature.txt, badging.txt,
delivery.json. No GitHub publication. Next stable versionCode must exceed1185.
