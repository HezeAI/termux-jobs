# Historical private build records — v1.5.11-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.11-test validation

- Full Java verification passed: app 630 cases, terminal-emulator 145 cases; zero failures/errors/skips. Logs: `.verification/local-ai-v1511/tests-final.log`, `test-counts.json`.
- 15 standalone real-SDK interface tests passed with fake inference. Added checks for thinking/channels and greedy sampler settings, then restoration of ordinary defaults. This does not prove native hardware inference or model obedience.
- Three synthetic-run tests cover five-case sequencing, capture without dispatch, errors/cancellation/restoration and read-only rejection. Report access uses synchronized snapshots. Diagnostic workflow has no Runner/tool dispatcher. Remote overrides reset per generation; original settings restored in finally. No permission bypass added.
- Release ARM64 build and required runtime-class audit passed. Build emits existing Kotlin metadata compatibility diagnostics; successful DEX/class audit does not establish device UI compatibility.
- ZIP CRC, duplicates/traversal, com.termux identity, versionCode1148, minSDK26, ARM64, private AI flag, v2 signing certificate, limited embedded-secret scan, existing native libraries/agent assets and seven QNN/LiteRT payloads checked.
- No weight, installed environment, user session or external-agent changes. No reset/migration introduced. Package/certificate match previous delivery. No publication.
- Device checks pending: run all five cases on Gemma, export/reopen report, Stop/cancel followed by ordinary chat, and verify navigation/report UI. Synthetic requested actions are never executed. A correct tool choice is not proof of successful file writing.

APK SHA-256: `5cb645290eb923b50a90caa4fdeeb30ec97c6d0df979bb9f468ce309c0e8e3ff`
Size: 104887496 bytes. Download copy verified independently.


---

## Subsequent development record

# Частная проверка v1.5.11-test

## Изменения и факты

Разработчик на записи Screenrecorder-20261019-012819.mp4 подтвердил, что возврат
во вкладку больше не вызывает прежнего скачка после1.5.10. Клавиатура и дополнительные
клавиши всё ещё вызывали повторный показ более ранних сообщений OpenCode.
Справка pinned mini и runtime показывают: --replay восстанавливает историю при
входе И resize. Android coalescing не устраняет необходимый окончательный resize.

Отдельный frontend jobs2 сохраняет initial/model-reopen replay и отключает replay
только при изменении высоты. Width replay оставлен намеренно: первый изолированный
прогон без него потерял отображаемую историю после изменения ширины. После разделения
ширины/высоты итоговый native probe сохраняет историю. Официальный serve/runtime,
модели, доступ, данные подключений и авторизация не заменяются.

OpenCode pending карточка: native request + UUID приложения, точный recheck полного
запроса/owner, once/deny POST и native ack. Повторные, конкурирующие, устаревшие или
изменённые запросы отклоняются. Native complex/security scope/MCP/form остаются
в CLI; проектные постоянные разрешения предлагаются только native интерфейсом.
Получение/ответы используют прямой private LocalSocket без proot-процесса на опрос.

Для OpenCode/Gemini после подтверждения overlay скрывается. Native transcript
содержит компактную запись решения; касание открывает подробности текущего чата.
Обычный, shell и compact Gemini пути используют одну native обёртку. Успешный native
callback и permission.replied означают разрешение, а не успех команды. Текст команды
не вставляется во ввод PTY приложением и не исполняется нажатием на запись.
Details сохраняются приватно по chat ID, mode0600, максимум200/2MiB. Они могут
содержать пути/команды; в scalar status и пресеты не входят. Старое сессионное
разрешение Gemini не выдаётся повторно при просмотре записи после рестарта.
Удаление native чата сторонним CLI не гарантирует удаления sidecar; для OpenCode
запроса без tool source восстановление облачка после reopen не гарантируется.

Codex/Claude проверены по текущим мостам: нет интеграции native approval событий
с app UI. Их штатные запросы и full-access/no-confirm поведение сохранены. Новые
карточки этим двум агентам не приписываются.

## Проверки

- Итоговый verify-java.sh:552 теста,398 app +154 terminal-emulator,0 failures/errors/
  skipped. Отдельно21 адресная проверка новых UI/resize/tap путей пройдена.
- UI: owner/routing Gemini/OpenCode, поздний ответ после ухода и возврата, native ack
  скрывает overlay без изменения размеров, неверный ack не выдаёт ложный успех;
  touch mouse-mode касание записи не отправляет native mouse click, обычное касание
  сохраняет старое поведение. Реальный JNI не запускался в Robolectric: используются
  synthetic font/window geometry и transport/JNI doubles.
- 107 upstream frontend runtime/queue/boot/permission/transport тестов прошли.
  TUI и CLI typecheck прошли. Canonical bun run check запущен, но полный lint/typecheck
  pipeline блокирован прежней ошибкой oxlint JS-plugin: ERR_UNKNOWN_FILE_EXTENSION
  для TypeScript anti-slop/index.ts в текущем Node loader. Чистый upstream lint не
  заявляется; новые системные зависимости не устанавливались ради обхода.
- Native mini: private HOME/XDG, synthetic chat, proxy запрещает inference. Четыре
  height resize не повторяют историю; два width resize восстанавливают её. Native
  permission create/list/answer даёт строку в scrollback и scoped details RPC;
  model reopen сохраняет native chat ID/историю; owned stop прошёл. Реальных ключей
  или платных запросов в probe нет. Это не запись кадров с Android IME.
- ReceiptStore: persistence/reload,0600, dedup, control-character sanitization,
  invalid outcomes/path tokens, foreign/changed/stale/double/concurrent permissions.
- Gemini receipt: запись только после native callback, сохранение при React cleanup,
  восстановление по chat/call ID, scope/warning guards, privacy scalar status;
  все3 pinned receipt компонента выполняются в проверке. Hash mismatch сохраняет
  native fallback. SDK исходники не переписываются.
- check-gemini-live/runtime/auth и frame-render/live-modes/permissions suites прошли:
  вход, native model/mode callbacks, рестарты, private socket, own shutdown/parent loss,
  чужие процессы не затрагиваются. Тесты используют собственные синтетические child.
- Installer jobs2: установка внутри репозитория, SHA, reuse без перезаписи,
  отказ при повреждённом binary. Подготовка стадий сначала столкнулась с особенностью
  PRoot link2symlink после hardlink test fixture; source assets переведены в обычные
  отдельные файлы. Итоговые stages/build используют эти финальные файлы.
- Локализация584 строк, git diff --check пройдены.
- ARM64 assembleRelease успешна. Runtime audit:12925 defined classes, обязательные
  определения присутствуют,31 reviewed optional type. Прежние JNA/UAST/Kotlin
  metadata/deprecation предупреждения build tooling не означают чистый полный Lint.

## Совместимость и состав

com.termux, versionName1.5.11-test, versionCode1173, minSdk26, arm64-v8a.
Пакет/сертификат прежние, код выше1.5.10/1172. Workspace8/status1 не меняются;
очистка данных по изменениям исходников не требуется. На устройстве миграция всех
существующих данных этим аудитом не проверяется. Следующий stable нужен с code>1173.

ZIP/пакет/версия/ABI/minSdk/nondebug/v2 signature/SHA проверены. Bundle содержит только
ARM64 ELF frontend и MIT LICENSE, hashes совпадают с generated manifest. Обе JNI
библиотеки, Codex/Claude/bootstrap/server/backup assets из списка аудита побайтно
совпадают с1.5.10. Изменённые controller/loader/helpers соответствуют финальным
исходникам. Private auth/workspace/key файлы и проверяемые secret markers отсутствуют.

Финальный APK стал больше из-за отдельного frontend. Тот заменяет штатный mini,
не работает параллельно с ним; официальный server сохраняется. Редкие записи и
опрос разрешений1500ms ограничены текущей видимой OpenCode-сессией. Замеры реальной
плавности/потребления на телефоне не выполнены этими автоматическими тестами.

## Проверка на устройстве

После обновления нужен новый запуск OpenCode/Gemini для hooks/frontend. Проверить:
клавиатура/интерактивный ввод/дополнительные клавиши, переходы Home/Commands/Terminal,
чтение старой истории и выделение; поворот/изменение ширины отдельно (replay допустим).
В режиме подтверждений запросить безопасную команду: разово и отказ, затем прокрутка
облачка, нажатие для details, переключение между чатами. Отдельно Gemini shell/compact
и обычный инструмент. Full access не должен создавать искусственные запросы.
Переоткрытие после смены модели сохраняет историю в native probe; Android путь ещё
нужно подтвердить. Внешняя мышь, DPI и все разновидности CLI security prompts не
проверены физически. Публикация GitHub не входит в эту частную сборку.

APK: `Termux-Jobs-v1.5.11-test-arm64.apk`

Размер: 113533795bytes.

SHA-256: `712c71282300985233b32b1619292cd454587ed2c727ace2acf8c3cb2cc063b5`

Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

Frontend bundle: 77286955bytes, SHA-256 `f404aafb7d78aa9fae870d556b55a3484457d9444e71f5e31bcc4667ce920d0b`.

Native probe: 24030bytes/44frames до reopen; после reopen маркер истории выведен 4раза (initial +2 width +reopen).
