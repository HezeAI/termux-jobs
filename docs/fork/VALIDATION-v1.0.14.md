# Локальная проверочная v1.0.14 — Claude Code

Не опубликованный GitHub-релиз. com.termux, versionName1.0.14/versionCode1051,
ARM64, Android7+. APK 36638136 bytes.
SHA256 `5ea7e65d02eaf7d9e6221265ac47ffb0f871f886f24927874180b00f4f9faaa5`.
Сертификат `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` совпадает с v1.0.13; code1051 выше1050.
ZIP CRC, подписьv2, package/version/minSDK/nondebuggable/ABI и stored4-byte
alignment проверены; SHA256 Download-копии перечитан и совпадает.

## Проверено

- Полный запуск app + terminal-emulator: 274 JUnit/Robolectric теста,
  failures/errors/skipped0. 5 новых Claude tests: сохранение и чтение настроек,
  отдельный новый чат у копии, отказ unsafe fields без замены библиотеки,
  отдельный mount/command, legacy profile не превращается в Claude, UI ровно
  new/choose/id и применение permission-mode. Первая UI-проверка не дождалась
  onShow listener в mock looper; исправлено ожидание looper в тесте, не код UI.
- AssembleRelease/lintVital, 548 RU/EN строк, форматные аргументы, JS/bash syntax,
  git diffcheck. ARM64 JNI и все прежние assets побайтово равны v1.0.13;
  добавлены только claude-control.js и claude-bootstrap.sh. Эксперименты
  OpenCode screen/resize не возвращены. Бинарник Claude в APK не включён.
- Официальный native Claude2.1.296 --version/--help запускается в Ubuntu/proot.
  npm SHA512 integrity и SHA256 архива/бинарника проверены. Модельных запросов нет.
- Offline private bootstrap probe: реальные официальные байты, SHA и tar;
  download подменён локальным копированием. Установка и повторное использование,
  повреждённый существующий binary сохраняется, повреждённый download не
  публикуется, private Node22 fallback устанавливается отдельно от system Node.
  Найденный ранним probe дефект подавления Bash errexit при function || return
  исправлен; повторные проверки прошли. Системные инструменты не заменялись.
- Node controller probe: правильные flags для3chat modes, reject unsafe input,
  права Unix socket0600, graceful stop и forced kill только собственного child,
  cleanup при parent loss и spawn failure. Посторонний test process пережил оба
  сценария stop. Все процессы созданы проверкой, пользовательские сессии не тронуты.
- Отдельный targeted test фактического старого reader до AI Agents прошёл:
  текущий JSON с Claude-профилем читается как Ubuntu, Codex/команды/заметки
  сохраняются. Проверка stage-only, не меняла production reader или APK.
- Ревью: per-run UUID захватывается перед async close; stop старого запуска
  не адресует socket нового. Нет pkill, сигналов по имени или process-tree kill.
  Исключения stop завершают только захваченный TerminalSession, не текущую другую
  вкладку. Native detached jobs не объявлены гарантированно завершёнными вместе
  с CLI. Открытые сессии других агентов не имеют Claude control paths.
- ZIP filenames/added-assets/source equality проверены: личные auth/SSH/keystore
  файлы в новые assets не включены; секреты не копировались и не печатались.

## Состояния и ограничения

Optional Claude fields в formatv8; aiAgent остаётся пустым, environmentubuntu.
Прежний reader может читать библиотеку как обычный Ubuntu-профиль без Claude
startup. Новый preset сохраняет настройки; chatnew создаёт новый UUID при каждом
запуске. Copy не наследует UUID. Native --resume работает с native history;
Android-окно выбора истории пока отсутствует — choose открывает terminal picker.
Native auth/history/settings отдельны от Codex/OpenCode. Ubuntu и проектные файлы
общие: это разделение компонентов, не защита от агента с доступом к общим файлам.

Card показывает **при запуске**, не текущую модель после /model. Живое управление
моделью/разрешениями и лимиты не реализованы. Платный Claude account/API нужен
для диалога; ChatGPT Plus не покрывает доступ. Вход, native picker, реальный resume,
разговор, IME, жесты и закрытие настоящего CLI на телефоне ещё не проверены.
Официальный LinuxARM64 работает в version/help probe, но Android/proot не
заявляется официально поддерживаемым. Permission modes не дают root Android.

APK не устанавливался агентом; GitHub не публиковался. Текущие runtime/auth,
процессы Codex/OpenCode/Bridge и shared SDK не изменялись. Минимальная проверка
пользователя: update → Create session/AI Agents/Claude Code → дождаться установки
→ login → короткий диалог → закрыть → Выбрать чат/ID → продолжить. Затем открыть
Codex/OpenCode и проверить прежний запуск. Подписка не нужна для version/help,
но бесплатный inference режим не заявлен.

Evidence: `.verification/claude-v1.0.14`: before.tar, build-verified.log,
runtime-check.log, bootstrap-check.log, badging.txt, signature.txt, artifact.json.
