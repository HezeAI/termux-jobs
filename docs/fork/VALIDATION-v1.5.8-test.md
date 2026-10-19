# Historical private build records — v1.5.8-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# Validation — v1.5.8-test

Private test build. Evidence: `.verification/local-ai-v158/`. A broad run executed623 app tests:621 passed and2 new continuation assertions failed due to escaped path presentation.145 terminal-emulator tests passed. After correcting path presentation, a targeted run exposed that an optional anchor could displace an otherwise fitting exchange. Anchor insertion was moved after ordinary compaction. The final71 relevant tests all pass (zero failures/errors), including all6 new continuation cases and existing context, summary, coordinator, LiteRT and video tests. Final tested source matches the APK build stage. Initial failed logs are retained; they are not presented as successful checks.

Changed paths: runtime-only context fitting, optional history argument normalization, and local-chat context notice presentation. Review covers unchanged system permission strings, current request preservation, archive immutability, nullable vs invalid arguments, per-turn notice boundaries/reload, and exclusion of notice data from model input.

Device checks to perform: continue a file-specific task after compaction; ask for history with normal permissions; expand the single context card, switch tabs and reopen the chat. Verify ordinary tools and video still work. Passing JVM tests does not establish real model interpretation, Android UI behavior or exhaustive regression coverage.

Final build/identity evidence is recorded in `build-verified.log`, `audit.json` and `1.5.8-test/artifact.json`. Device-specific checks below remain pending.

Final APK: 104884568bytes; SHA-256 `ff3caea6258ded12111ba7f15c64950a7cd73fc9d647e892477618f60fac032e`. ZIP CRC and path/duplicate checks pass, com.termux/1145/1.5.8-test, minSDK26, ARM64 only, private Local AI flag enabled, v2 signature and existing certificate match. Limited secret filename/marker scan passed; existing native payloads and controller/server-agent assets match baseline. Download copy independently hashed.


---

## Subsequent development record

# Частная проверка v1.5.8-test — 2026-10-19

## Причина и изменение

В записи Gemini строка ввода исчезала на7–18ms. Отдельный запуск извлечённой native
функции Ink createStandard показал: изменение только spinner вызывает erase/repaint
неизменённой нижней панели, без DEC2026 в legacy main-screen path. У эмулятора
не было обработки этих границ; TerminalSession публиковал каждое чтение до4096bytes.
Синтетическое воспроизведение показывает промежуточный чёрный фон до конца redraw.
Это совпадающий механизм; живые PTY-байты из записи не захватывались.

- Hash-pinned in-memory loader помечает записи native log-update в трёх проверенных
  renderer chunks Gemini0.64.0. Внутренние байты и порядок не меняются. Уже помеченный
  вывод не оборачивается повторно; глобальный stdout/stderr не перехватывается.
- Gemini-only SynchronizedFrameBuffer собирает помеченный кадр перед изменением
  emulator, затем применяет целиком. Поэтому сторонний draw тоже не видит частично
  стёртую панель. Обычный вывод проходит сразу, завершённый кадр не ждёт таймер.
- Поддержаны разорванные маркеры/UTF-8, несколько кадров и хвостовые байты.
  Ограничение256KiB, аварийное освобождение через200ms от первого удержанного байта;
  повторный begin не продлевает срок. Disable/reset/resize/exit освобождают остаток
  и снимают callback. Таймер существует только при наличии удержанных байтов.
- Новый фоновой сервис, сохранение кадров истории и прежние resize-batches с1.2с
  не добавлены. Процессы, разрешения, аутентификация, история и SDK не изменяются.
- Другие сессии по умолчанию идут прежним непосредственным путём.

## OpenCode

Проверены исходник владельца и установленный бинарник mini read-only. Затем
запущен отдельный native mini с private HOME/XDG, синтетической историей и proxy,
блокирующим inference. Работающие чаты и пользовательские ключи не использовались.
В захваченном выводе23751bytes:50 begin и50 end DEC2026. В тесте не включались
alternate screen или mouse tracking; активны обычный экран и focus/bracketed-paste.
Шесть смен размеров окна прошли, тестовая история сохранилась, owned stop прошёл.

Этот результат подтверждает использование протокола кадров самим mini и показывает
применимость проверки общего пути transcript. Наш onScreenUpdated сбрасывает обычный
transcript в конец на выводе, если Gemini-only opt-in не включён. Это отдельный
механизм от кадров; жесты Android и работа при реальной генерации OpenCode ещё не
проверены. В этой сборке оба Gemini-only флага на OpenCode не распространяются.

Первый пробный запуск со старым Java harness не прошёл из-за удалённого исторического
SynchronizedOutputBuffer; подготовлен отдельный harness для текущего emulator.
После этого один ранний snapshot не содержал seed даже в raw output, а следующий
запуск его показал: это не доказательство потери истории или успешной проверки всех
startup timing scenarios. Старый полный reopen-test поэтому не объявляется пройденным.
Успешный отдельный frame/resize probe не выполнял inference или смену модели.
Старый ASTRA-отчёт описывает исторические буферы, а не текущую реализацию.

## Обязательная проверка на устройстве

После обновления заново запустить Gemini, чтобы загрузился новый renderer hook.
Отправить запрос, дождаться длительного spinner и проверить нижнюю серую строку;
затем прокрутку во время ответа, клавиатуру, смену вкладок и остановку сессии.
Сравнить с предыдущей v1.5.7-test. Мок-тесты и source probes не доказывают отсутствие
мерцания/задержек на реальном Android. Native full-terminal/static-output paths
и разные настройки CLI могут вести себя иначе; полная матрица не заявляется.

## Совместимость

com.termux, Android8+, arm64-v8a, versionCode1170 выше1169; прежний сертификат.
Формат библиотеки8 и status schema1 остаются. Код не требует очистки истории,
пресетов, входа или окружений; все варианты пользовательских данных не проверены.
Следующий стабильный выпуск должен иметь code>1170. Частная сборка; публикация
на GitHub и установка APK не выполнялись.

## Автоматические проверки и артефакт

- 529 JVM tests:375 app +154 terminal-emulator, failures/errors/skipped0.
  Новые9 тестов parser: split markers/UTF-8, обычные ANSI, несколько кадров,
  anchored timeout/repeated begin, partial marker flush, oversized frame,
  переполнение внутри неполного escape без дублирования, изоляция сессий,
  случайные размеры фрагментов. Новые3 session tests: отсутствие промежуточной
  мутации настоящего emulator, timeout/disable/reset, старый путь других сессий.
- test_gemini_frame_render.cjs исполняет извлечённый renderer всех3 layouts;
  проверены frame boundaries, byte-identical payload, отсутствие повторной обёртки,
  fallback на hash mismatch, синтаксис полных modified modules, SDK unchanged.
- check-gemini-live.js, test_gemini_live_modes.cjs, check-gemini-runtime.js,
  check-gemini-auth.js пройдены: status/model/rights, очередь и отмена, приватность,
  owned stop/restart/parent loss, синтетическая авторизация. Реальные ключи не нужны.
- Настоящий Gemini0.64.0 --version с loader: exit0, stderr пуст, private HOME,
  no inference. Локализация584 strings и git diff --check пройдены.
- Изолированная ARM64 release build успешна. Прежние предупреждения AGP/aapt2 и
  deprecated API сохраняются; отсутствие всех Lint-предупреждений не заявляется.
- ZIP/package/version/minSdk/ABI/nondebug/cert/SHA проверены. Известные private
  auth/workspace/key файлы и secret markers в APK отсутствуют.
- Native библиотеки, Codex/Claude/OpenCode, server/backup/bootstrap assets,
  Gemini controller и preload побайтно совпадают с v1.5.7-test. Изменённый loader
  совпадает с исходником. Runtime inventory12900 defined classes, required/
  unreviewed missing отсутствуют,31 прежний рассмотренный optional type.
  Это не проверка всех reflection/linkage/device scenarios.

- `Termux-Jobs-v1.5.8-test-arm64.apk` — 36541496 bytes.
- SHA-256: `de8a5e6548336b765ffcfc4d172024a76a1616b35dbee0f38c238c63a4b307e1`
- Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
