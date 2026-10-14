# Проверка локальной v1.0.6

APK `Termux-Jobs-v1.0.6-arm64.apk`: 36612563 байт, SHA-256 `b98a6166ace648416e5523a6dac16c9ae917fc9be0e9498232bef5b97caa6a99`.
Package com.termux; versionName 1.0.6 / code1043; Android 7+, target28, ARM64.
Подпись v2, прежний сертификат SHA-256
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
ZIP CRC, debug flag, ABI, выравнивание и отсутствие личных ключей/баз проверены.
Прежние assets кроме адаптера и нативные библиотеки побайтово сохранены.
Два изменённых/добавленных assets совпадают с финальными исходниками.
Download-копия перечитана, SHA-256 совпадает. APK не устанавливался/не публиковался.

## Проверки

- 261 локальный тест, failures/errors/skipped = 0, assembleRelease/lintVital успешны.
  Включена дополнительная изолированная проверка фактическим reader v1.0.4.
- RU/EN: 513 workspace strings; bash/JS синтаксис и git diff --check.
- OpenCode v2.0.28: создание native ID, status, stop, alias → actual ID после
  закрытия для сохранения Android-профиля, resume того же ID после рестарта.
- A/B на одной бесплатной Ling 3.1 Flash Free, без личных ключей: штатный CLI
  отвечает OK, исправленный серверный адаптер отвечает OK., прежний адаптер
  воспроизводит FreeTierError / HTTP403. Это не гарантия постоянной доступности сервиса.
  Проба использовала штатный opencode run вместо интерактивного mini; UI/IME
  и ответы через mini на телефоне требуют проверки пользователем.
- Bootstrap в папке проекта с явным корнем, без изменения HOME/системных пакетов:
  установка закреплённого официального OpenCode tarball при имеющемся Node,
  готовые компоненты без скачивания, ошибка загрузки с очисткой только собственной
  временной папки, отсутствие/старый Node с установкой настоящего private ARM64 Node.
  Архив OpenCode дополнительно сверён с npm integrity SHA-512; Node SHA-256 —
  с официальными SHASUMS256. Загрузки в bootstrap-тесте заменены копированием
  уже проверенных официальных архивов; реальные HTTPS-загрузки проверены отдельно.
- Проверено разделение Codex/OpenCode: CodexSessionShell/Rpc/Status/ModelSelection
  не изменены, штатный Codex asset сохранён. Bootstrap запускается только для aiAgent=opencode.

## Границы

Заранее установленная Ubuntu обязательна. APT ветка установки отсутствующих
вспомогательных curl/tar/xz/flock/coreutils не запускалась на рабочей Ubuntu;
Node и OpenCode устанавливаются отдельно, без глобального npm/update.
Сначала нужно запустить сессию для компонентов, потом выбирать провайдера/модель
в случае их отсутствия. Первый запуск на чистом телефоне, IME, Android lifecycle,
сигналы во время установки, SIGKILL/OOM и установочный откат не проверены.
Старые ID v1.0.5 не переписываются: для free проверки создайте новый чат.
История и авторизация Codex не копируются/не удаляются адаптером.

Локальные свидетельства: .tools/ai-agents/free-diagnostic/ — build-final.log,
bootstrap-final.log, native-final.log, official-run.log, adapter-inference.log,
old-adapter-inference.log, badging.txt, signature.txt, artifact.json.
[Использование и откат](AI-AGENTS.md). Резерв v1.0.4/code1100 выше новой версии1043.
