# Локальная проверка v1.0.15 — Gemini CLI

com.termux, versionName1.0.15/code1052, Android7+/ARM64, 34293352 bytes.
SHA256 `676da5d7836dc5854eda8ab9b3acba6961aaa4ae943e8b04b04acffa1dd01b39`.
Certificate SHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` — прежний, code повышен относительно
v1.0.14/1051. ZIP CRC, APKv2 signature, package/version/minSDK/nondebuggable/ABI и
stored4byte alignment проверены. SHA Download-копии перечитан и совпадает.

## Тесты и сборка

279 JUnit/Robolectric тестов: failures/errors/skipped0; AssembleRelease/lintVital
прошли. 560 RU/EN workspace strings/format args, JS/bash syntax, git diffcheck.
Новые5Gemini cases: JSON/settings/history UUID, независимая copy, invalid inputs
не заменяют библиотеку, собственный Ubuntu mount/control env, legacy profiles,
UI строго new/choose/id и mode. Общая проверка Claude UI сначала попадала в скрытый
Gemini control с тем же названием; finder в тесте теперь пропускает GONE views,
повторный полный suite прошёл. Это исправление тестовой выборки, не обход ошибки UI.

Bootstrap offline test использует реальные official archives, но заменяет curl
локальным копированием: npm integrity SHA512, package SHA256 и entry SHA256;
установка/переиспользование всех bundle chunks через manifest, собственный Node22,
повреждённый download не публикует runtime, повреждённый установленный bundle
сохраняется и отказывается запускаться. Права0700/private files, исходные системные
пакеты не менялись. В APK нет самого Gemini runtime: лишь2наших script assets.

Node-control tests: точные CLI flags, reject unsafe values, Unix socket0600,
обычное и принудительное завершение только собственного child, parent-loss,
spawn failure, official restart199/auth-type handoff и неизменённый unrelated
тестовый процесс. Никаких пользовательских PID не останавливали. Native launcher
вложенного unmanaged child не создаёт: GEMINI_CLI_NO_RELAUNCH=1, loop управляется
нашим owner. Stop блокирует restart, ограничение5быстрых перезапусков предотвращает
бесконечный цикл. Уникальный run UUID захватывается до async stop, не направляет
запрос в более новую вкладку. Намеренно detached tool jobs не обещаны остановленными.

Официальный Gemini0.64.0 реально выполняет --version/--help в Ubuntu/proot.
Native PTY probe с пустой приватной папкой: доверие только этой тестовой папке,
реальный exit199, restart под нашим controller и появление Google login option;
затем собственный probe остановлен, socket удалён. Вход не отправлялся. Native
--list-sessions без auth ожидаемо завершился41 с требованием настроить auth;
это НЕ выдаётся за проверку реального resume. Все config/history paths этих
проверок внутри .tools/gemini-cli; чужие аккаунты/настройки не читались для входа.

## OpenCode и другие агенты

Автоистория OpenCode восстановлена по последней инструкции. Тесты: стабильный
alias при повторных запусках, сохранение selected resume/legacy bookmark после
JSON/load, независимая copy, отсутствие ручного save-chat action у всех4агентов.
Сохранение пресета сохраняет alias; --new explicit/connection UI получает freshID.
Аргументы тех же alias проходят в неизменённый bridge, где session-links разрешает
alias в native ID и GET находит прежний чат. Native DB не изменялась агентом.

Manual save-chat handler/action удалены; optional old aiSavedThread оставлен для
совместимости старых ручных закладок. UI-дефолтnew больше не генерирует alias при
каждом prepareLaunch, existing resume не сбрасывается на decode. Удаление
несохранённого profile не удаляет native history. Termux snapshots не добавлялись.

Пояснения доступа добавлены Codex/Claude/OpenCode/Gemini в список выбора и под
выбранным режимом, RU/EN. Runtime permission flags первых3агентов неизменны.
Claude/Codex/OpenCode assets и все .so побайтово равны v1.0.14; только Gemini2assets
новые. Нативные terminal/view/emulator screen/resize механизмы не изменялись.
Новые optional Gemini fields в libraryv8; old reader структурно видит обычный
Ubuntu profile, Gemini сам в старой сборке не запускается. Фактический update
и downgrade Android агентом не выполнялись.

## Осталось проверить на телефоне

Установить update, AI Agents → Gemini CLI → создать → при необходимости доверить
папке → Login with Google личным аккаунтом → пройти browser sign-in. Бесплатный
уровень/модели зависят от аккаунта/региона; подписка не покупалась и billing не
включался. Вход, inference, tool execution и настоящий resume не проверены.

Минимум: короткий диалог в своей папке → закрыть → та же папка + Выбрать чат
или UUID → продолжить. Проверить клавиатуру/переключение/жесты (mock tests этого
не доказывают). У OpenCode сохранить пресет → закрыть/перезапустить и проверить
историю; explicitNew должен быть независимым. Остальные агенты запустить как прежде.
Плашка Gemini/Claude показывает At launch, живые telemetry/model/quotas не заявлены.

GitHub не публиковался/APK не устанавливался. Installed Termux, sharedSDK,
пользовательские Bridge/Codex/OpenCode процессы и Ubuntu packages не менялись.
Artifacts: .verification/gemini-v1.0.15: build-verified.log, test-results/,
bootstrap-check.log,runtime-check.log,native-interactive.txt,badging.txt,
signature.txt,artifact.json. До-login PTY probe не выдаётся за работу на устройстве.
