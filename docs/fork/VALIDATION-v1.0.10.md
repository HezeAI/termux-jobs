# Проверки локальной v1.0.10 — OpenCode и быстрые действия

APK `Termux-Jobs-v1.0.10-arm64.apk`, 36628378 байт; SHA-256 `42e090806fa135b0f27ac231fb69ccc61f79298f07d70ae32cd4257abf50959c`.
com.termux, versionName 1.0.10, versionCode 1047, ARM64, Android 7+/target28.
Сертификат SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` совпадает с v1.0.9; подпись v2 проверена.
ZIP CRC, ABI, отсутствие debug-флага, 4-byte alignment и SHA Download-копии проверены.
Все assets и native .so побайтово совпадают с v1.0.9; private keystore/auth/SSH/баз нет.
APK не устанавливался, GitHub не обновлялся.

## Выполнено

- 277 JUnit/Robolectric тестов: failures/errors/skipped = 0. AssembleRelease/lintVital
  прошли; 531 workspace RU/EN строка, общие настройки, JS/bash/Python syntax,
  git diff --check проходят. Gradle предупреждения об ARM64 FS watching/ABI пустых
  альтернативных split-файлов не выдаются за ошибки финального ARM64 APK.
- Buffer tests: промежуточная очистка/текст не видны до end frame; любая граница
  разбиения UTF-8/ANSI сохраняет все исходные байты. Таймаут освобождает незавершённый
  кадр/marker, repeated begin не продлевает ожидание, max-size fallback не теряет данные.
  Random fragmentation/control-string cases проверяют byte-for-byte equivalence.
- Реальная TerminalSession + emulator под Robolectric: opt-in не показывает clear
  до нового текста; end отменяет timeout; missing end освобождается через 1000ms;
  disable применяет pending output. Обычный default-путь по-прежнему немедленный.
- Resize branch: clear и история приходят в emulator одним обновлением после quiet;
  непрерывный поток не блокирует показ дольше 1200ms; default resize batching — no-op;
  same-size OpenCode не вызывает JNI. Реальный JNI resize не выдаётся за проверенный
  mock-тестом: ветка запуска burst прослежена логически, native PTY probe отдельно ниже.
- Native OpenCode 2.0.28 в private test root: модель меняется с переоткрытием, native ID
  прежний, следующий prompt использует новую модель. Локальный proxy блокировал
  inference, generate/command/shell; платных/личных запросов нет.
- Native output feeding настоящему Java TerminalEmulator с ответами на DSR: сохранённая
  искусственная история отображается при старте/переоткрытии и остаётся после шести
  высот/ширин окна с реальным SIGWINCH. Это тест native output и parser, не проверка
  Android кадров/клавиатуры. Штатный replay сохранён и может выполняться при resize:
  исправление группирует показ его промежуточных шагов на Android, не отменяет историю.
- UI tests проверяют создание через первый пункт stack popup, заголовки «Создать
  пресет»/«Создать сессию», новый packages tile вместо +, отсутствие packages в
  долгом тапе обычной/базовой сессии и доступность selector без активной вкладки.

## Ревью и логическая инспекция

Opt-in включается по aiAgent=opencode при создании/refresh профиля, не по глобальной
настройке всех терминалов. Native bridge/bootstrap/model access не менялись, а byte
поток остаётся исходным: ANSI/OSC не вырезаются и не приобретают новые полномочия.
Frame marker в control-string может добавить краткую задержку, но байты не изменятся.
Все callbacks работают на main thread, буферы только в RAM; это не snapshots на диске.
Размер frame ограничен 1MiB, resize output выпускается при пороге 1MiB; совместный
flush может временно включать несколько bounded порций. Timeout не продлевается
потоком повторных begin. После resize earliest draw350ms, quiet100ms, hard limit1200ms.

Прослежены complete/end, prefix, overflow, frame timeout, resize quiet/deadline, повторный
resize, disable, explicit reset (discard pending перед reset), process exit (flush перед
cleanup), переключение/закрытие. Новые timer callbacks отменяются при reset/exit/disable;
никаких новых сигналов, остановки чужих процессов или сохранённых файлов не добавлено.
Сравнение размеров включает cell pixel metrics, поэтому реальные DPI изменения не
подавляются. TerminalView source/auto-scroll не изменены; возврат вниз оставлен по
последней просьбе пользователя. Package shortcut читает актуальную выбранную сессию,
далее использует прежний held-target/confirmation путь, не устанавливает самовольно.

## Проверка на телефоне ещё нужна

Откройте пустую OpenCode-сессию, отправьте сообщение и посмотрите индикатор/поток текста.
В существующем чате откройте/закройте клавиатуру и перейдите в другую сессию и обратно;
история/ответ должны остаться, промежуточная очистка должна стать менее заметной.
Нативный replay сам по себе остаётся; фактическое устранение всех рывков на телефоне
не подтверждено. Отображение при resize может отстать до1,2s, ввод/агент не задерживаются.
Дефектный/слишком большой незавершённый кадр сбрасывается по fallback, без вечной паузы.

Проверьте packages в новой кнопке, создание сессии через первый пункт selector и
заголовки Home/terminal. Моки не проверяют Android DPI/IME/gestures или графику пикселей.

Свидетельства: `.tools/ai-agents/resize/build-delivery.log`, `artifact.json`, `badging.txt`,
`signature.txt`, `probe-native-final.log`, native `run-*/screen.txt`/`terminal.txt`,
JUnit XML .verification/arm64. NativeTerminalHarness находится только в ignored .tools.
Предыдущий v1.0.9 APK и резервный rollback code1100 остаются; новый code1047 ниже1100.

## Результат последующей проверки пользователем

**Мерцание не устранено на телефоне.** Прохождение тестов выше подтверждает
проверенные свойства транспорта, но не исправление OC-001. Пользователь попросил
пока оставить текущую реализацию и отметить проблему важной.
[Наблюдения и дальнейшая диагностика](KNOWN-ISSUES.md).
