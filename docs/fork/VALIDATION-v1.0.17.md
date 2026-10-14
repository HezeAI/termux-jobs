# Локальная проверка v1.0.17 — приватный путь Gemini auth

com.termux, versionName1.0.17/code1054, Android7+/ARM64, 34297428 bytes.
SHA256 `ff65bba9f707a2ba658738142c538eef7205ca46e48f13b6c8bbdd787879630c`.
Certificate SHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Прежний сертификат и пакет, code1054>1053; допускают обновление поверх v1.0.16.
Установка и миграция данных на устройстве агентом не выполнялись.

## Доказанный дефект

Скриншот Screenshot_20261014_095631.jpg показывает отказ Gemini читать settings
v1.0.16 из-за world-writable предка /tmp (41777). Native security check проверяет
родителей, поэтому приватные права самого файла и папки сокетов недостаточны.
Первоначальная причина переключения в v1.0.15 этим ещё не доказана.

Policy перенесён в ~/.local/share/termux-jobs/gemini-cli/auth: каталог0700,
UUID.json0600, создание wx, очистка по завершении контроллера. Неприватный каталог
или symlink отклоняется без chmod. /tmp, пользовательские settings, токены и
рабочие процессы не менялись. Сокет остаётся в прежнем месте. Для изолированных
проб поддержан JOBS_GEMINI_AUTH_DIR; production использует приватный HOME-путь.

## Проверки

- Официальный CLI0.64.0 в изолированном PTY: старый контроллер + тестовый предок
  сокета1777 воспроизводит Skipping system settings file. Новый контроллер с тем
  же небезопасным местом сокета и отдельным приватным policy достигает OAuth code
  prompt без предупреждения и API prompt. Старый тестовый selectedType=API не
  изменился; собственный policy удаляется. Все временные файлы в репозитории;
  сам /tmp не модифицировался. Пробы не использовали реальных кодов/аккаунтов.
- Node auth suite: Google/API/auto,0600, отказ для небезопасной папки/внешней
  политики, два restart199 с конфликтующим IPC, очистка своего файла и сокета.
- Node runtime suite: own graceful/forced stop, parent loss, spawn failure,
  auth/restart handoff, сохранение чужого процесса.
- 280 JUnit/Robolectric tests: failures/errors/skipped0; assembleRelease/lintVital.
- JS/bash syntax,568 RU/EN strings/форматы, git diff --check.
- Финальный APK: ZIP CRC, package/version, ARM64-only, nondebuggable, подписьv2,
  сертификат, stored-entry alignment4, SHA256 повторно после копирования Download.
- Из assets/native относительно v1.0.16 изменён только gemini-control.js.
  Проверены исходный asset и отсутствие файлов auth/key/keystore в APK.

Источники доказательств: .verification/gemini-auth-v1.0.17 (build.log, test-results,
artifact.json, signature.txt, badging.txt, probe-private.py, probe-insecure.py).
Первые пробы с чрезмерно длинным Unix socket path завершились до запуска CLI;
исправленный короткий тестовый путь использован для финального сравнения.
Проверка v1.0.16 использовала безопасных предков и не покрывала /tmp — её
положительный результат не доказывал работу policy в реальном app-монтировании.

## Осталось проверить пользователю

Установить APK поверх, закрыть прежнюю Gemini-вкладку, явно выбрать Google
и запустить заново. Проверить отсутствие warning и полный OAuth после ввода кода.
Реальный вход/ответ модели, доступность аккаунта и Android lifecycle не проверены.
APK не установлен и не опубликован на GitHub. Другие агенты и история не изменены.
