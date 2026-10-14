# Проверки локальной v1.0.8 — меню OpenCode и доступ

APK `Termux-Jobs-v1.0.8-arm64.apk`, 36618656 байт; SHA-256 `f798f048920e6d6f81758e1fb777bfbb933588364293632e48503661574827b3`.
Package com.termux, versionName 1.0.8, versionCode1045, Android7+/target28, ARM64.
Прежний сертификат SHA-256
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
ZIP CRC, debug-флаг, ABI, выравнивание, подпись v2, отсутствие личных keystore/auth/
баз и точная Download-копия проверены. Прежние assets кроме адаптера и native libs
побайтово совпадают с v1.0.7. CodexSessionShell/Rpc/Status/ModelSelection не менялись.

## Выполнено

- 265 локальных JUnit/Robolectric тестов: 0 failures/errors/skipped.
  Assemble release и lintVital прошли; 521 workspace RU/EN строка, bash/JS syntax,
  git diff --check. Аппаратные IME/жесты не выдаются за проверенные этими тестами.
- Воспроизведено исходное различие на собственном native OpenCode: API меняет модель
  Ling → Exo/high, а текст Android popup раньше обновлялся только при открытии.
- Новый адаптер возвращает applied лишь после GET подтверждения provider/id/variant.
  UI применяет подтверждённый результат к видимой строке и профилю одним путём;
  unit проверяет actual label/profile/save и отказ на неправильном ответе.
  Generation guard защищает от позднего первоначального статуса.
- Persistence/copy/read: aiAccess хранится в необязательном v8 поле; отсутствие
  поля даёт ask, неизвестное значение не заменяет исходную библиотеку.
- Production accessRules из настоящего asset проверены через native API permission
  decision для build/plan × full/ask/read × read/edit/shell/subagent/arbitrary plugin:
  full=allow, ask=ask, read: чтение allow, остальные deny. Это 30 проверок решений,
  без выполнения файловых/командных действий и без вызова модели.
- Production bridge устанавливает read/full/ask в новой сессии и при resume того
  же native ID. GET permissions совпадает с выбранной политикой. Серверы только
  собственного probe остановлены; пользовательские сессии не менялись.
- Смена доступа существующей работающей вкладки отклоняется до изменения профиля;
  выбранные разрешения применяются на следующем запуске/возобновлении. Full требует
  подтверждения, default остаётся ask, Codex access не затронут.

## Границы

Это ограничения инструментов OpenCode, не Linux/Android sandbox. Plan не является
самостоятельным запретом edits при выбранном full. Read запрещает субагентов/MCP
и неизвестные действия, чтобы не отдавать обходным исполнителям расширенные права.
Сохранённые Allow always могут менять ask-поведение проекта; deny должен оставаться
ограничением по официальной модели OpenCode. Не проверены реальные плагины/MCP и
обход ограничения из-за дефекта самого стороннего инструмента.

Проверка модели не отправляла inference; права проверялись локальными API-запросами.
Вывод mini на телефоне, Android UI popup, язык/клавиатура этих новых текстов и actual
инструментные задачи требуют ручной проверки. Факт readback не обещает неизменность
модели при последующем ручном выборе в CLI другим пользователем/клиентом.
APK не устанавливался и GitHub не обновлялся. Резервcode1100 выше1045 сохранён.

Свидетельства: .tools/ai-agents/model-control/build-final.log, probe-fixed.log,
permissions-production.log, bridge-access-resume.log, artifact.json, badging.txt,
signature.txt и JUnit XML .verification. [Использование](AI-AGENTS.md).
Официальная семантика [Permissions v2](https://opencode.ai/v2/docs/permissions/).

## Уточнение после проверки пользователем

Пользователь обнаружил, что mini возвращает свою старую модель при следующей отправке.
Проверка readback выше не покрывала отправку из mini и не доказывала синхронизацию его
локального выбора. В v1.0.9 реализовано подтверждаемое переоткрытие frontend с историей;
см. [новые проверки](VALIDATION-v1.0.9.md).
