# Локальная проверка v1.0.16 — выбор авторизации Gemini

com.termux, versionName1.0.16/code1053, Android7+/ARM64, 34297160 bytes.
SHA256 `0e1ca73e9e4bc757d8879fba8137510f6772f4568b7095799443ede63417ce4d`.
Certificate SHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`, прежний, code1053>1052.
ZIP CRC/signaturev2/package/version/nondebuggable/ABI/stored4-byte alignment прошли.
Download SHA перечитан и совпадает. Личные ключи/аккаунты/auth/config файлы не включены.
Из native/assets v1.0.15 изменён только gemini-control.js, остальные совпадают.

## Диагноз и пределы доказательств

На телефоне Google selection/restart переходил к API prompt. Read-only проверка
показала selectedType=gemini-api-key. Контроллер v1.0.15 не отправляет клавиши;
первоначальная причина выбора второго пункта НЕ установлена. Чистая native
проверка с Google, затем обычный и bracketed paste фиктивного неверного кода
сохраняла oauth-personal и показывала ожидаемую ошибку. Настоящие аккаунты,
коды и токены для проверок не использовались. Рабочие сессии не тронуты.

v1.0.16 задаёт явное намерение пользователя: google/api/auto. Это фиксация native
способа входа, а не исправление доказанной ошибки ввода. После новых проверок
нельзя утверждать, что авторизация аккаунта уже прошла или account eligibility
доказана. Для этого нужна проверка пользователя на телефоне.

## Проверено

- 280 JUnit/Robolectric tests, failures/errors/skipped0; AssembleRelease/lintVital.
  GeminiAuth round-trip/copy, reject unknown auth, missing field старого профиля
  остаётся auto. Остальные проверки агентов/истории прошли.
- 568 RU/EN workspace strings и форматные аргументы; JS/bash syntax/git diffcheck.
- Node auth tests: Google/API/native choice, 0600 overlay, отсутствие изменений
  caller env, отказ при внешних system settings, два последовательных restart199
  с конфликтующим auth-type IPC не переводят fixedGoogle в API. Auto сохраняет
  прежнюю IPC-логику. Удаление собственного overlay/socket после owned stop.
- Прежний Gemini runtime suite: launch flags/input rejection, graceful/forced
  close только своего child, parent loss, spawn error, restart/auth handoff,
  unrelated process выжил. Никаких сигналов пользовательским процессам.
- Native official0.64.0 probe: заранее записанный API preference в тестовом
  пользовательском settings.json, запуск fixedGoogle, OAuth code prompt без
  API-поля. Старый settings-файл остался API, т.е. контроллер его не переписал.
  Авторизация не отправлена, процесс остановлен собственным probe.

## Реализация/ревью

New Gemini profiles=google; decode absent=auto, optional field formatv8.
Для смены режима у запущенного Gemini нужен close; новых скрытых изменений режима
у существующих профилей нет. GeminiCli.command добавляет auth arg, остальные
агенты его не получают. Google/API используют native selectedType+enforcedType
в собственном per-run settings-файле с mode0600 и wx; path только в env child.
Ни перехвата, ни очистки очереди клавиш не добавлено. Google/account/country
проверки остаются штатными, API billing не включается контроллером.

Существующие systemsettings/defaults env или /etc/gemini-cli блокируют fixed mode:
предлагается auto, их policies/default-discovery не подменяются. Созданный overlay
не копирует исходные user settings или секреты. Ошибка before-listen очищает
созданный overlay; finish удаляет только свой file/socket; уникальный run ID
защищает другую новую сессию. Root/service/process permissions не расширяются.
Остальные subprocess stop paths, Codex/Claude/OpenCode и terminal input unchanged.

## Минимальная проверка устройства

Обновить поверх v1.0.15, вручную закрыть тестовую Gemini, создать новую с входом
Google либо изменить auth у закрытого профиля. У старого профиля auto сохранится
до явного выбора Google. Пройти browser flow и ввести полученный код в CLI.
В fixedGoogle альтернативного API выбора быть не должно; реальная Google ошибка
должна оставаться видимой. Затем проверить короткий диалог/закрытие/resume.
Если есть внешний system config, fixed mode корректно откажет; выбрать auto.
Клавиатура/жесты/Google account login/inference ещё не проверены на устройстве.

GitHub не публиковался/APK не устанавливался. Evidence: before.tar, build.log,
artifact.json,badging.txt,signature.txt,test-results/,auth-check.log,
runtime-check.log,google-*-probe.txt и private probes. Reusable auth test:
scripts/check-gemini-auth.js. Рабочая Ubuntu/.gemini не модифицировалась агентом.
