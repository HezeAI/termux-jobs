# Проверочная v1.0.13 — возврат и ручное сохранение чата

com.termux, versionName1.0.13/versionCode1050, ARM64/Android7+. APK34255556 bytes,
SHA256 `54e7cc2fa54bd8f30136954457d3fd043b545541eed5be82a8cc631f7039f939`, certificate `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` прежний.
ZIP CRC/signaturev2/package/version/nondebuggable/ABI/stored4-byte alignment и
перечитанный SHA Download копии проверены. Assets и native libs побайтово равны
v1.0.9; bundled frontend отсутствует. В DEX нет SynchronizedOutputBuffer или
mSessionSizeDeferred. Личные auth/SSH/keystore файлы не включены.

## Код/проверки

269 JUnit/Robolectric tests, failures/errors/skipped0. Отключённые экспериментальные
тесты архивированы вместе с удалёнными механизмами, не выдаются за проверки нового
поведения. Новые тесты: разные ID для новых запусков, explicit resume только один раз,
ручная привязка переживает JSON/load и duplicate её не наследует, отсутствие поля
не включает old auto-resume, unsafe bookmark отвергается без замены библиотеки.
UI test: сохранение пресета не сохраняет текущий чат; SaveChat толькоOpenCode,
без запущенной сессии действие не сохраняет случайный ID.

AssembleRelease/lintVital,534RU/EN strings/format args,JS syntax/git diffcheck прошли.
TerminalSession,TerminalEmulator,TerminalView,TermuxTerminalSessionClient побайтово
восстановлены в состояние до screen experiments (HEAD code этих файлов совпадал
с v1.0.9). Adapter asset извлечён из проверенного v1.0.9 APK, полностью совпадает.
UI nav/presets/packages tile и надписи сохранены.

Native private synthetic probe с ImmediateHarness без транспортных buffers: native
история при старте/reopen, busy/refusal и admission race logic, модель после
следующей отправки,6 resize и own stop. Proxy блокировал inference; пользовательские
процессы/чаты/ключи не использовались. Первый fixed2s readiness probe не дождался
появления history marker на загруженном телефоне; повтор с bounded25s condition
прошёл. Это не выдаётся за исправление baseline replay, графики или IME.

## Состояния/границы

Optional aiSavedThread в formatv8. Legacy auto-resume становится new, ID остаётся
доступен и native DB не удаляется. Explicit selection записывается в rc до onCreated
сброса aiChat кnew; successful manual save хранит native ID и profile.saved=true.
Обычный SavePreset и close-save больше не запрашивают/усваивают current AI history.
Копия/новый/другой чат/другое подключение не наследуют bookmark.

SaveChat читает status через live profile Unix socket; отдельный server не создаёт.
Перед сохранением повторно проверяются current thread/connection, profile existence,
same live target и running. Ошибка/закрытие/смена не pin-ит stale ID. Изменения
SQLite, авторизации, CLI tools или пользовательских процессов агентом не делались.
В production process-stop flow не добавлены новые сигналы/остановки. Codex поведение
и его история не менялись. Действие сохраняет resume bookmark, не отключает запись
сообщений самимOpenCode. save-chat включает сохранённый preset для его доступности.

## Устройство/ограничения

Проверить фактическую установку, новый против сохранённого чата, смену модели, IME,
nav/session switch и выделение. Mock/UI/source проверки не подтверждают phone gestures.
Baseline reset/replay при resize восстановлен по просьбе пользователя, OC-001
не объявлен исправленным. Старые jobs1 files в установленной Ubuntu не удалялись,
rollback adapter их не использует. Package/cert/code позволяют update с1.0.12;
actual update не выполнялся агентом. Reserved rollback1100 выше1050.
GitHub не публиковался/APK не устанавливался.

Receipts .verification/rollback-v1.0.13: before.tar,removed/,removed-paths.json,
build-delivery.log,probe-final.log,artifact.json,badging.txt,signature.txt.
