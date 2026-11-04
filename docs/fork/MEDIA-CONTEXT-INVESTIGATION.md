# Media and tool-result context — investigation in progress

2026-11-04. **No disconnection fix is complete or included in 2.1.3-test4.**
The [test APK](VALIDATION-2.1.3-test4.md) contains the separate audit changes.
No working history, authorization or session was edited. No native history was
manually stripped. No APK/component was installed, replaced or published here.

## Baseline and acceptance

Historical phone evidence: Codex 0.166.0, Hysteria → Selectel → GeoHide,
20 PNGs totalling 20,815,040 original bytes / 27,753,412 base64 bytes in history.
Continuous batches eventually answered; cold reopen did not answer within
240.31 seconds and showed retries through 5/5. That is a bounded failed baseline,
not proof of a terminal CLI failure, a sole root cause or a completed solution.
See the preserved `.verification/wife-route-20261103/png-stress/REPORT.txt` under
`/root/termux-fork` and the project memory. Phone Codex versions differed (0.163.0
working session versus the 0.166.0 probe); no universal version claim follows.

Acceptance still requires a candidate that continues, reopens and recovers after
new media/tool results **without manual compaction on the previous external route**.
A local provider's immediate reply cannot meet that criterion. A read-only `ssh ...
true` availability check succeeded during this work; no external model request was
sent by this new probe and no real authorization file was inspected or copied.

## What is shared, and what is not

`ChatAttachments.java` preserves originals. It limits selection to four files and
samples up to 12 video frames. It prepares JPEG previews at a 1280-pixel long edge
in some paths. In particular, a Codex project-file PNG/JPEG/WebP/GIF at ≤ 20 MiB
can bypass that conversion; gallery and other-agent paths generally prepare a
preview. This source-level difference does not cover native tool-opened images.

The shared `agent-attachments.cjs` emits bounded text samples (8192 bytes), original
paths/metadata, Claude image blocks, Gemini `@` references and OpenCode file URIs.
Codex has its own corresponding admission/serialization path in `codex-chat.cjs`.
ZIP/STL/BLEND cards use binary metadata/path; the card path does not itself read and
send the binary bytes. The corrected adapter fixtures check that contract. It does
not restrict a later shell, file or MCP tool from reading/encoding that file.

Android transcript limits and `trimPreviews` are presentation limits. They do not
limit the CLI's next model request. `withoutImageData` removes media from Claude's
Android projection, not its native session/model context. Codex resumes with
`excludeTurns`, then pages turns; Gemini retains native history and limits its extra
projection; OpenCode pages the native message API. None of those facts proves a
small inference request. A stored-history size is also not a network-request size.

## Native local-provider measurements

Artificial v1 dataset: 20 deterministic 600 × 600 PNGs, **21,642,207 bytes**;
one large JPEG, one large WebP, 24 small PNGs, large code/log/JSON files and opaque
binary stubs. The stubs do not validate real ZIP/STL/BLEND parsing. The preserved
v2 generator adds valid ZIP/STL and small JPEG/WebP; generation is not a passed test.
All recorded v1 originals and the four selected runs' project copies retain their
manifest hashes.

Fresh kernel-fence checks rejected external/foreign loopback destinations,
UDP/packet/non-TCP traffic, deferred/Fast Open, raw-syscall bypass and descendant
exec bypass. Native subprocesses received fake HOME/config/XDG locations and
explicit synthetic credentials. A separate project-root marker stopped ancestor
project discovery. This is a network fence and separate fixtures, **not a kernel
filesystem jail or a complete trace of every file open**. Host network settings
and working connections were not changed.

Every successful response below came from a scripted artificial provider. Times
measure the local fixture operation, including native processing and observations;
they are not mobile-network, real-model inference or vision-quality results.
Artificial token usage is unsuitable for validating real automatic-compaction
thresholds. Counts below are completed cases, not universal scenario coverage.

| Runtime and scope | Completed local cases | Cold reopen after 20 PNGs: request | Saved history / API view | Local completion |
| --- | ---: | ---: | ---: | ---: |
| Codex 0.166.0 app-server | 23 | 28,920,720 bytes; 20 images | 28,996,591 bytes native history | 0.98 s |
| Claude Code 2.1.296 + production SDK adapter | 26 | 8,790,495 bytes; 20 images | 8,898,974 bytes native history | 0.94 s |
| Gemini 0.64.0 core + actual Google SDK, API mode | 26 | 28,858,452 bytes; 20 images | 28,862,439 bytes native session | 0.72 s |
| OpenCode 2.0.28 serve/API, custom compatible provider | 10 | 14,461,431 bytes; 10 images | 28,865,961 bytes API history projection, not DB size | 16.64 s |

Claude's actual cold-reopen values are confirmed in the evidence summary; its
native image preparation differs from Codex/Gemini. The OpenCode reduction is an
observed native projection in this configuration, not an application fix or a
verified global media budget. Its automatic-pruning trigger still needs analysis.
It persisted across serve restart in this probe. Do not declare all four runtimes
affected by the same defect or fixed by the same change.

Codex also replayed the large mixed context after native restart. Three actual
`view_image` calls for the **same file** each added one image to the next request:
46 → 47 → 48 → 49, with approximately 1.44 MB extra per read in this dataset.
The subsequent mixed request reached 39,733,279 bytes. The native large-log command
returned bounded text to the next request; original log bytes stayed on disk.

Claude and Gemini exercised native image/file-content paths, large text results,
frame payloads, continuation and native-history reopening. Gemini uses the native
recording parser; this is not a full interactive CLI/process/Android restart.
Their synthetic stream-break cases retried locally and completed. Codex's explicit
stream break also led to two requests and a successful local reply. These do not
show recovery from the original external service failure.

OpenCode's 12-frame payload reached the provider after the fixture's 20-second
native HTTP timer expired. That case is **not a pass** and not an established
application timeout. The earlier fixture also misread an inbox as a completed-state
log; the native inbox contains pending entries. Completion now uses active state
and a new completed assistant record. A keep-alive socket race in the observation
client was removed by using separate HTTP connections. Initial failures remain
in evidence; their cause is not assigned to production without another reproduction.

## Why view_image was missed, and the actual file route

The first probe searched only the outer request's `tools`. For this model,
Responses Lite places tool declarations in an `additional_tools` item in `input`.
They contain the `functions.exec` code-mode catalogue, including nested
`tools.view_image` and `tools.exec_command`. Absence of a top-level definition was
not evidence of an unavailable tool. The collector now reads both representations.
This is corroborated by upstream [client.rs](https://github.com/openai/codex/blob/6ff1e1ca3b30fc8b914720681fcf5fe62e95f856/codex-rs/core/src/client.rs#L1057), at tag `rust-v0.166.0`, peeled commit
`6ff1e1ca3b30fc8b914720681fcf5fe62e95f856` (inspection, not a rebuilt-binary attestation).

The actual app-server model catalogue reported `gpt-6.1-sol` with `text` and `image`
input modalities. The corrected probe executes native handlers and observes their
results in the **next HTTP request**, rather than treating a tool-call emission as
proof of execution. In the separate latest-file case:

1. A scripted provider requested a native command to find the newest image inside
   the artificial media directory. It consumed the returned path, then invoked
   native `view_image`. Three requests completed; the final request contained one
   image (3,744,925 bytes for the whole request).
2. A native command found the latest artificial MP4. Another native command ran
   real FFmpeg to extract a frame. Native `view_image` opened that frame. Four
   requests completed; the final one contained the retained first image and new
   frame (3,765,683 bytes total).

No paperclip attachment was used in either case. A real model's tool planning,
Android latest-file UI and the previous external route are not covered by this
scripted sequence. The native handlers, path handoff, decoding and serialized image
are covered. Evidence retains the model modalities, tool catalogue and request counts.

## Remaining coverage (not successful tests)

| Scenario | Codex | Claude | Gemini | OpenCode |
| --- | --- | --- | --- | --- |
| Large PNG + accumulation/reopen | Local native measured | Local native measured | Core/SDK measured | Local native measured; observed pruning |
| One large JPEG/WebP | Local native measured | Local native measured | Core/SDK measured | Local native measured |
| Many small PNG | Local native measured | Local native measured | Core/SDK measured | Not reached in completed run |
| Many small JPEG/WebP | Not run | Not run | Not run | Not run |
| Native latest-image/video discovery | Native handlers measured | Not run | Not run | Not run |
| Sampled frame accumulation | Latest MP4 decoding separately; large payload pending | Synthetic 12-frame payload measured | Synthetic 12-frame payload measured | Fixture timed out; incomplete |
| Large code/log/JSON | Native command/log and text measured | Native Read measured | Native content/converter measured | Not reached |
| Documents/PDF and full document tool path | Incomplete | Incomplete | Incomplete | Incomplete |
| Repeated image tool reads | Native view_image measured | One native Read; repeats incomplete | One native file-result conversion; repeats incomplete | Incomplete |
| MCP results and repeated MCP reads | Not run | Not run | Not run | Not run |
| Metadata-only binary cards | Adapter fixture | Adapter fixture | Adapter fixture | Adapter fixture |
| Subsequent genuine binary parsing/shell/MCP | Incomplete | Opaque stub Read only | Opaque stub content reader only | Incomplete |
| Mixed chat + deliberate disconnect | Local native | Local native | Core/SDK | Incomplete |
| Automatic/manual native compaction under real token accounting | Not validated | Not validated | Not validated | Trigger not established |
| Previous external route after a fix | Not run; no fix yet | Not run | Not run | Not run |

## Implementation direction, not an implemented fix

A complete candidate needs a bounded **request projection immediately before model
serialization**, covering user media, tool/MCP image blocks and tool text. It must
leave originals/native history intact, keep stable source references and tool-call
pairing, deduplicate repeated data, retain selected recent previews, and substitute
explicit references for older media or oversized text. Details need bounded crops,
pages or line ranges on demand. Critical system/project instructions must not be
silently discarded to fit a byte budget. Oversized irreducible requests need a
clear error. Budget values still require measured mobile/external-route validation.

App-side preview preparation, frame/selection budgets and truthful metadata can
improve admission; they cannot control an independent native `view_image`, shell
or MCP result. Increasing local bridge buffer sizes or automatic compaction alone
is not the proposed solution. Native request projection requires a distinct,
verified integration for each CLI:

- Codex: current app-server adapter admits turns and observes events. A central
  native serialization hook/patch must cover both direct and code-mode tool images.
  Its experimental `unified_image_budget` must not be enabled as an assumed fix;
  `compaction_image_budget` is already enabled upstream and is not proof of a
  serialized-byte cap or a remedy for this baseline.
- Gemini: the installed core has a BeforeModel path that can accept modified
  contents. Its typed request projection is a promising integration, to test across
  user/tool/MCP parts and API/Vertex without mutating retained history. Current
  [BeforeModel documentation](https://geminicli.com/docs/hooks/reference/#beforemodel)
  is supporting documentation, not proof that an application fix has been wired.
- Claude: current [PostToolUse documentation](https://code.claude.com/docs/en/hooks#posttooluse-decision-control)
  describes schema-matched `updatedToolOutput`, including built-ins, and a legacy
  MCP-only field. This needs verification on pinned 2.1.296. Replacing a fresh tool
  result does not by itself manage old user attachments or the whole next request.
- OpenCode: investigate its observed native image pruning and pinned 2.0.28
  provider/plugin boundary before adding a second limiter. Current general plugin
  documentation does not establish the required hook for that binary version.

No experimental switch, proxy, new CLI binary or provider/auth setting was silently
installed here. There is no completed cross-agent patch yet. The external-route
acceptance, full matrix and implementation work remain open.

## Evidence and reproducibility

Local evidence: `.verification/media-context/initial-summary.json`, v1
`data/manifest.json`, per-agent result pointers, `codex-latest-reference.json`, native
HTTP logs and network-enforcement results. The original 240-second phone baseline
stays separate. Manual source probes and a fresh-fixture runner are in
[scripts/tests/media-context](../../scripts/tests/media-context/README.md).
Their [native inputs](../../scripts/tests/media-context/native-inputs.json) record
actual inspected bytes/versions. The runner itself requires existing prerequisites
and tests the kernel fence before execution; see its limitations before running.

The pre-existing common attachment test had stale simulated admission behavior:
Claude needed a native replay acknowledgment and explicit negative replies rather
than always throwing; Gemini needed the native input/session identity tags. Those
fixture contracts were corrected. The common adapter test now passes. That is a
test repair, not a media-disconnection fix. All application production inputs remain
identical to the delivered audit APK's input manifest.

The newly preserved guarded runner was executed for `codex-latest` with a fresh
v2 dataset (`replay-UJEqWS`): fence negative checks and all three native cases
passed. Other preserved runner variants were syntax-checked; their earlier native
measurements and limitations are recorded above, not relabelled as fresh runner
passes. Node adapter/history baseline: five unchanged suites passed; the stale
common attachment suite passed after the test-contract repair described above.


## Урок: 2026-11-03 — Codex0.166.0 связывает прежний чат с идентификатором провайдера

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Codex0.166.0 связывает прежний чат с идентификатором провайдера

- Контекст: обход обрыва WebSocket на втором телефоне с сохранением прежнего чата.
- Подтверждение: нативный app-server0.166.0 в изолированной фикстуре отказал в resume
  openai-чата при выборе termux_jobs_https: modelProviderMismatch. В списке такого
  провайдера прежний чат также отсутствовал. Реальные аккаунты не использовались.
- Вывод: отдельный HTTP/SSE-провайдер нельзя выдавать как прозрачный запускатель
  прежнего чата. Сетевой запускатель без смены провайдера этого конфликта не создаёт;
  это не подтверждает устранение сетевого обрыва. Условия других версий проверять отдельно.
- Доказательства: .verification/wife-route-20261103/app-server-fixture/result-resume.json
  и .verification/wife-route-20261103/ssh88/REPORT.md. В сетевых пробах были и успехи,
  и TLS-тайм-ауты; не записывать SSH88 как подтверждённое исправление телефона.


## Урок: 2026-11-03 — Context Mode: MCP и полный плагин проверяются отдельно

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Context Mode: MCP и полный плагин проверяются отдельно

- Контекст: исследование context-mode 1.0.169 в Ubuntu ARM64, Node 22.22.1; рабочие аккаунты и сессии не использовались.
- Вывод: Codex 0.166.0 обнаружил MCP через app-server и выполнил индексирование/поиск; Claude 2.1.296 сообщил connected и 11 инструментов через потоковый CLI; Gemini 0.64.0 сообщил Connected через mcp list. Это не подтверждает работу hooks, compaction, реальной модели или интерфейса приложения.
- Установка: start.mjs повторно запускал npm install better-sqlite3 и не уложился в ожидание стенда. Прямой неизменённый server.bundle.mjs работал. Не переносить автоматический self-heal/обновление в запуск каждой сессии без отдельной проверки; подготовку зависимостей отделять от запуска MCP.
- Настройки: в Gemini 0.64.0 основной Storage добавляет /.gemini к GEMINI_CLI_HOME. Изолированный профиль должен учитывать это; первая фикстура в неверном каталоге не обнаружила сервер. В рабочем приложении не подменять HOME/авторизацию ради подключения MCP.
- Границы: дополнительные proot-bind в стенде осложняли завершение процессов; один tracing-stop процесс потребовал адресного завершения. Это не установлена ошибка штатного запуска. При внедрении повторить проверки завершения в настоящих контроллерах.
- Подтверждение: `.verification/context-mode-20261103/REPORT.txt`, `codex-final-result.json`, `claude-result.json`, `gemini-result.json`, `isolation-result.json`. Отдельные CONTEXT_MODE_DIR разделили базы двух одновременных экземпляров одного проекта. Внедрения и APK ещё нет.


## Урок: 2026-11-03 — Короткий drain-timeout CONNECT relay обрывает медленный поток

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Короткий drain-timeout CONNECT relay обрывает медленный поток

- Контекст: server relay SHA-256 `6d770234913a6c2e281fab38a2ccd1e4fbc56b51b8a81d7b818db7b7cf0542bb`, большой поток через GeoHide.
- Подтверждено: `pipe` ограничивает `writer.drain()` 15 сек независимо от TCP-прогресса; искусственный прогрессирующий транспорт воспроизвёл TimeoutError ровно через 15 сек. Кандидат использует общий ограниченный idle_timeout; четыре сценария и 13 прежних сетевых/маршрутных проверок прошли. Позднее настоящий loopback с паузой получателя 18 секунд подтвердил обрыв старого кода и полную передачу 2 MiB новым.
- Граница вывода: серверные счётчики и RST согласуются с этим механизмом; логи не содержат ID запроса и типа исключения, поэтому конкретный запрос телефона и все другие ошибки этим не доказаны. GeoHide также показал отдельные TCP/TLS-отказы коротких публичных HTTPS-проб.
- Применение: не считать короткое ожидание drain контролем отсутствия передачи; большой буфер может медленно освобождаться. Проверять прогресс, конечный бюджет и освобождение соединений. Не менять общий работающий маршрут ради испытания.
- Доказательства исследования: `.verification/wife-route-20261103/recheck-html/REPORT.txt`, `drain-result.json`, `observe-close.json`, `candidate-tests.json`.
- Применение по разрешению пользователя: отдельный relay `wife88` на 18759 обновлён; его исторический маршрут только .88 заменён порядком общего relay .67/.68/.88. Отдельная Hysteria 8443 теперь разрешает также 18759; прежний 18749 сохранён. Перед рестартом этих двух служб проверены PID и отсутствие TCP-соединений; QUIC-сессии отдельно не перечислялись. Общие службы 443/18749 сохранили PID и время активации. Приёмка телефона не выполнена. Прежнее утверждение «кандидат не развёрнут» заменено этим состоянием.
- Выдан отдельный установщик `/root/selectel-fixed` без apt/автозапуска. Восемь проверок установщика и два коротких HTTPS через 8443→18759 прошли; это не проверка большого запроса модели или доступности 8443 с другого телефона. См. `.verification/wife-route-20261103/drain-fix/REPORT.txt`. Общий маршрут 443 по-прежнему содержит старый timeout.
- Повторная проверка после переключения второго телефона: пользователь сообщил reconnect; сервер подтвердил трафик через 8443→18759 и правильный хеш кода. Большой поток продолжался свыше106сек, около66кБ/с, с rwnd_limited≈95%; два других upstream-сокета получили удалённый FIN. CPU≈4.4%, память доступна, прироста CPU-throttling нет в измеренном интервале. Это подтверждает применение фикса, но не устранение проблемы телефона. Конкретный запрос со скриншота не привязан к TCP-потоку; причина за узлом GeoHide не установлена. См. `.verification/wife-route-20261103/reconnect-194104/REPORT.txt`.
- Проверка на втором телефоне по временному SSH: реально запущен штатный Codex0.163.0, наш процесс использует0.166.0. Базовые компоненты присутствуют, watchdog не перезапускал туннель. После разрешённого просмотра истории установлено: около27.8МБ из29.3МБ —20 PNG-результатов view_image; большие ZIP/STL представлены ссылками, а не пользовательскими вложениями. Размер истории не считать размером каждого запроса. CLI также сообщает timeouts списка моделей/лимитов: нельзя объяснять все ошибки только изображениями. См. `.verification/wife-route-20261103/phone-access/PHONE-REPORT.txt`; причина различия поведения версий не проверена.


## Урок: 2026-11-03 — /proc/PID/exe внутри PRoot может быть загрузчиком

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — /proc/PID/exe внутри PRoot может быть загрузчиком

- Контекст: диагностика native Codex в Ubuntu AI на другом телефоне.
- Вывод: ссылка /proc/PID/exe у работающего Codex указывает на Termux libexec/proot/loader. Её отдельный запуск с --version завершился сигналом11; рабочий процесс не затронут. Для проверки версии сначала установить настоящий исполняемый файл по mapped path/пакету и запускать именно его. argv0 может быть только codex без абсолютного пути.
- Подтверждение: штатный бинарник из codex-cli/0.163.0 дал codex-cli0.163.0; это совпало с метаданными истории. Отдельный SSH-login также не воспроизводит дополнительные bind-пути приложения в /tmp. См. `.verification/wife-route-20261103/phone-access/PHONE-REPORT.txt`.


## Урок: 2026-11-03 — DNS и короткий HTTPS не подтверждают восстановление Codex

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — DNS и короткий HTTPS не подтверждают восстановление Codex

- Контекст: второй телефон после переключения с Hysteria на пользовательский VPN. DNS обеих Ubuntu одинаковый, 8.8.8.8/8.8.4.4; настоящий CLI сообщил failed to lookup address information. Поздние DNS-пробы прошли, HTTPS с обходом DNS сначала также не устанавливал TCP.
- После переподключения короткие публичные запросы дали HTTP200/ожидаемый401 без ключа, но новый запрос CLI продолжил Broken pipe/WebSocket close, а история не росла. Проверять свежий turn и завершение ответа отдельно от health/robots.txt; не считать одинаковый resolv.conf доказательством одинаковой доступности DNS.
- Граница: это наблюдение текущего телефона/маршрута, не доказательство дефекта VPN-продукта, CLI или изображений. Полный текст tracing span может содержать случайные цифры502; искать код HTTP по точному сообщению ошибки, не по наличию подстроки.
- Подтверждение: `.verification/wife-route-20261103/vpn-reconnect-2/REPORT.txt`, снимки предыдущей проверки в `vpn-check-1724`. Настройки телефона не изменялись.


## Урок: 2026-11-03 — Большие PNG: проверять продолжение и повторное открытие чата отдельно

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Большие PNG: проверять продолжение и повторное открытие чата отдельно

- Контекст: второй телефон, настоящий Codex 0.166.0, установленные JS-активы, маршрут Hysteria → Selectel → GeoHide. Отдельные тестовые чаты; рабочий Molds не запускался.
- Подтверждено: короткий контроль до/после нагрузки ответил за 8,61/8,63 сек. Первые четыре исходных PNG дали повтор WebSocket и ответ через 231,92 сек. Накопление до 20 PNG порциями прошло; повторное открытие с 20 PNG вызвало 5/5 и не дало ответа за 240 сек. Это предел стенда, не окончательный отказ CLI.
- Граница: PNG поданы как localImage через установленный адаптер, а не через Android-подготовку или исходный tool-output путь. История загрузила все 20 картинок и шесть ответов; Android-отрисовка не проверялась. Серверный TCP показал медленную отправку и ограничение окном получателя, но внутренний источник закрытия соединений не установлен.
- Применение: не считать успешный короткий запрос или непрерывный чат проверкой восстановления большого контекста. Различать передачу модели и небольшую проекцию карточек для интерфейса; размер истории не равен размеру каждого сетевого запроса.
- Доказательства: `.verification/wife-route-20261103/png-stress/REPORT.txt`, матрица `result.json`, пагинация и TCP-снимки. SSH с равным объёмом PNG и полный UI остаются непроверенными.
- Дополнение 2026-11-04: настоящий `codex exec` 0.166.0 без адаптера воспроизвёл тот же WebSocket close с сохранёнными 20 PNG. Через Hysteria/GeoHide ответил за 32,94 сек; ближайшая проверка адаптера — 31,32 сек с повторами. Через VPN Android без наших прокси — 94,89 сек с пятью ошибками, затем ответ; короткие контроли около 6 сек. Compaction не было. Совсем прямой маршрут без VPN не прошёл даже короткий контроль (есть HTTP 403, конечная причина turn.failed не установлена). Не приписывать ошибку только интерфейсу или только GeoHide. Официальный APK Termux и TUI этим не проверены. См. `.verification/wife-route-20261103/terminal-compare-20261104/REPORT.txt`.


## Урок: 2026-11-04 — Codex Responses Lite: отсутствие верхнего tools не означает отсутствия инструмента

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-04 — Codex Responses Lite: отсутствие верхнего tools не означает отсутствия инструмента

- Контекст: отдельные искусственные профили, Codex 0.166.0 / gpt-6.1-sol и локальный провайдер под проверенным kernel network guard. Не телефон и не проверка внешнего сервиса.
- Причина ложного вывода стенда: каталог Responses Lite расположен в `input` → `additional_tools`, включая описание вложенных инструментов code mode. Проверка только верхнего `tools` ошибочно исключала view_image.
- Подтверждение: native model/list объявляет text/image; настоящий exec_command нашёл последний файл, view_image передал изображение в следующий HTTP-запрос. Для MP4 отдельно выполнены поиск, настоящий FFmpeg и просмотр кадра. Три повторных view_image одного PNG в смешанном чате добавили три изображения в последующие запросы этого стенда.
- Применение: проверять объявление инструмента и фактическое выполнение отдельно; не заменять путь инструментов вложением. Историю, размер файла и размер запроса измерять раздельно. Наблюдения Codex не переносить на другие CLI.
- Доказательства и незакрытые области: активный checkout, `docs/fork/MEDIA-CONTEXT-INVESTIGATION.md`, `.verification/media-context/`. Исправление обрывов не готово и не входит в test4; внешний маршрут после исправления ещё не проверен.
