# Historical private build records — v1.5.10-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# Validation — v1.5.10-test

Evidence: `.verification/local-ai-v1510/`. Relevant JVM tests cover unchanged baseline schema constraints/availability across24 configurations (excluding description text), exact shared descriptions reaching both native schema sets including Tools only, custom behavior preservation, permissions, native parsing, context/history and video. A real SDK interface probe additionally inspects parsed write/command descriptions, using fake inference and no hardware execution.

One-off synthetic prompt sizing uses identical settings to1.5.9; estimates include native schemas but omit history/media/progress addenda. Description expansion intentionally costs context; it does not prove improved model routing. The one-off helper is retained only in the evidence directory after the run.

Final90 relevant JVM cases passed, including one-off sizing, 24-configuration schema fingerprints, descriptions in both native schema sets, parsing/permission/context/video regressions. Initial run89/90 passed; an existing feedback test required the explicit phrase `Execute Bash`, retained in the command description without changing its meaning. Final14 interface tests with real SDK0.17.0 and fake inference passed, including inspection of the SDK-parsed write/command and parameter descriptions. Both builds succeeded; final log is build-final.log. Models, hardware inference and installed apps were not used by these checks. No source/unit/SDK test constitutes a real NPU model, Android input or filesystem-side-effect test.

Synthetic estimated service input1.5.9 ->1.5.10 (full access, example project, same schemas/settings):

| Protocol | Standard | Brief | Tools only |
|---|---:|---:|---:|
| json | 2245 -> 2272 | 2000 -> 2395 | 1746 -> 2140 |
| qwen-native | 2542 -> 2936 | 2157 -> 2551 | 1902 -> 2297 |
| litert-native | 2223 -> 2629 | 2155 -> 2560 | 1927 -> 2332 |

LiteRT descriptors add about405 estimated tokens, traded for explicit task-selection guidance. This is not a speed or model-quality improvement claim. Actual clean-chat developer report4828324737620970227 from1.5.9 had no previous history/compaction,91 estimated current-user tokens, and still selected command for a guide; stale history was not necessary for that failure. New model-routing behavior remains unverified until developer testing.

Final APK 108691324bytes, SHA-256 `287b4643ed501816e5ecd487ca0aa2ab95c9ce956ee8f9426c4a3d2891f8771d`. ZIP CRC/path/duplicate checks, com.termux/1147/1.5.10-test, minSDK26, ARM64, private feature flag, v2 signature and matching existing certificate verified. Limited secret filename/marker scan passes; existing native payloads and controller/server-agent assets match baseline. Download copy independently hashed. Full schema reference exported from final compiled classes.


---

## Subsequent development record

# Частная проверка v1.5.10-test

## Изменение

Разработчик подтвердил устранение мерцания и прокрутки при поступлении вывода
OpenCode в v1.5.9-test. На записи Screenrecorder-20261019-010009.mp4 остаётся
перерисовка/прокрутка при возвращении во вкладку и открытии/закрытии клавиатуры.

По исходникам: скрытый TerminalView остаётся INVISIBLE, тогда как соседние
элементы становятся GONE. Меняется его измеренная высота и прежний путь отправлял
промежуточные размеры в PTY. Native mini заново выводит существующий чат после
SIGWINCH. Это не доказывает дублирование сохранённой истории.

Только OpenCode получает stableResize:

- Не отправлять размер скрытой вкладки/окна.
- Не отправлять повторный размер существующей сессии при одинаковой геометрии.
- Объединять видимые изменения:96ms тишины, максимум320ms от первого события.
- Отменять ожидающую операцию при скрытии, уходе приложения, detach, закрытии
  workspace и смене сессии; старый callback не меняет новую сессию.
- Первичное создание PTY не задерживать.
- Для height-only primary-buffer сохранять положение чтения по фактическому
  смещению строк в transcript. Ширина/alternate-screen/mouse tracking сохраняют
  прежнюю политику сброса.
- Пока ожидается окончательный размер, рисовать видимый конец прежнего буфера,
  а не обрезанное начало. Перед касанием/прокруткой применить размер сразу,
  чтобы координаты выделения соответствовали отображению.

Нет захвата истории для последующего replay. DEC2026 буфер и его пределы остаются
от v1.5.9. Gemini/Codex/Claude/SSH/shell не включают стабилизацию геометрии.

## Доказательства

Изолированный native OpenCode2.0.28 mini, private HOME/XDG, синтетический чат,
proxy запрещает inference:13992bytes,32 сбалансированных кадра, два окончательных
resize19x110→35x110, история видима на итоговом эмуляторе, остановка собственного
child прошла. Маркер чата записан один раз до resize, три раза после двух resize.
Проверка показывает совместимость окончательных размеров; Java coalescer проверен
отдельно. Это не замер Android IME и не обещание отсутствия всех native redraw.

JVM-проверки используют настоящий TerminalEmulator, подменённую JNI-границу без
процессов и Robolectric28. Синтетические размеры шрифта и видимость окна заданы
явно: Robolectric4.4 оставляет bare Activity window GONE и имеет нулевые font
metrics. Первые прогоны выявили эти проблемы fixture; они не считаются успешными
проверками. Тесты не доказывают реальную работу шрифта, DPI или жестов.

Полный итоговый scripts/verify-java.sh успешен:548 JVM-тестов,394 app +154
terminal-emulator, без failures/errors/skipped. В app входят13 новых тестов
OpenCodeResizeTest: скрытие/возврат, объединение и предел ожидания, отмена при
смене сессии/detach/завершении, отсутствие duplicate SIGWINCH, первичный запуск,
сохранение текста при height resize, columns/alternate fallback, временный viewport,
рисование и преобразование координат, применение размера перед касанием.
AgentOutputScopeTest подтверждает OpenCode-only geometry opt-in и сброс флагов
для другого профиля. Проверены пути startup/switch/hide/close и default legacy.

Node regression: test_gemini_frame_render.cjs и test_gemini_permissions.cjs
пройдены: frame markers/payload, native confirmation, stale/config/trust guards,
сессионные разрешения и fallback.584 строки локализации и git diff --check пройдены.

## Совместимость

com.termux, versionName1.5.10-test, versionCode1172, minSdk26, arm64-v8a.
Форматы workspace8/status1 и сохранённые данные не меняются. Очистка данных не
требуется по изменениям исходников; обновление поверх проверяется по package,
сертификату и повышению versionCode. Миграция реальных пользовательских данных
на устройстве этим аудитом не проверяется. Следующий stable нужен с code>1172.

Публикация GitHub и установка APK не выполняются в рамках этой частной проверки.

## Минимальная проверка на устройстве

В OpenCode с историей: вернуться Главная→Терминал, открыть/закрыть клавиатуру
внизу и при чтении более раннего текста; проверить выделение после resize.
Повторить смену OpenCode→другая сессия во время открытия клавиатуры. Проверить
обычную сессию и Gemini на прежнее поведение. Настоящий окончательный resize
по-прежнему может вызвать native redraw; устранение всех скачков на телефоне
до этой проверки не заявляется.

## Итоговый APK

Изолированная ARM64 assembleRelease успешна. Runtime-проверка DEX:12912 defined
classes; обязательные определения присутствуют,31 прежний reviewed optional type.
Lint/UAST вывел прежние сообщения JNA и несовместимости Kotlin metadata; чистый
полный Lint-анализ не заявляется. Deprecation/AGP/aapt2 warnings сохранены.

ZIP цел, package/version/minSdk/ABI/nondebug, v2 signature и SHA-256 проверены.
Все agent/bootstrap/server/backup assets из списка аудита и обе native библиотеки
побайтно совпадают с v1.5.9-test. Gemini live assets соответствуют исходникам и
также неизменны относительно1.5.9. В APK нет private auth/workspace/key файлов
и проверяемых secret markers. Полный аудит в ignored verification, не в assets.

Файл: `Termux-Jobs-v1.5.10-test-arm64.apk`

Размер: 36546524 bytes.

SHA-256: `e779a1523331296a802527822b6ed481bd0daac33a51b50f1aa5c7d7d37d51cd`

Сертификат SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
