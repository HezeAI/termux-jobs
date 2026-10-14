# Проверка v1.1.4 — менеджер бэкапов и обновления

Подготовлен релиз относительно публичной v1.1.0. Пользователь одобрил локальную
v1.1.3; добавлен объединённый переключатель/новая схема проверки обновлений.
[Опубликован как Latest](https://github.com/HezeAI/termux-jobs/releases/tag/v1.1.4): stable, не draft/prerelease.
APK, SHA256SUMS и update.json скачаны обратно анонимно по публичным HTTPS URL,
побайтно совпали с локальными; latest API указывает v1.1.4. Metadata/hash/size
связаны с опубликованным APK. Тег фиксирует `8b395f6a10840ac27c840fb9e2e52cc13d19b1ee`.

- APK: Termux-Jobs-v1.1.4-arm64.apk, 37851023bytes.
- SHA-256: `ad05f1ea6e4bf5147a89a0eece568abb9be3bbd9495955e82c3657123d25bb0c`.
- com.termux, versionName1.1.4, versionCode1061, minSDK24/target28, non-debuggable.
- Только arm64-v8a. ZIP CRC и выравнивание stored entries проверены.
- APK Signature Scheme v2 проверена. Certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
  Отдельно проверена подпись публичного v1.1.0 APK: совпадает; code1061 >1057,
  также выше локальной v1.1.3/code1060. Это проверка условий обновления,
  не выполненная агентом установка на Android.
- Содержимое assets/native идентично локальной v1.1.3, исходники Java совпали
  с изолированной build-копией. Backup helper в APK совпал с рабочим исходником.
- Имена APK entries и text/DEX проверены на известные credential patterns:
  совпадений проверенных токенов/private-key headers нет, auth/SSH/keystore/pyc
  файлов не найдено. Это не универсальный сканер любых возможных секретов.
- update.json schema1 с17 highlights на язык (включая5 compatibility lines),
  пакет/версия/размер/хеш связаны с финальным APK. Reader публичного v1.1.0
  понимает этот формат; release assets публично доступны и проверены обратной загрузкой.

## Тестирование

311 JVM/JUnit/Robolectric tests: 0failures/errors/skipped, release build successful.
Новые проверки: одна попытка на открытие, повтор после background/ошибки,
отсутствие повтора при configuration change, отключение/повторное включение,
раздельные клики title/checkbox, сохранение preference, английские Auto/hint,
новая общая плашка на всех трёх вкладках. Вкладки не меняют opening gate.

Backup helper не менялся после v1.1.3 (совпадает asset побайтно). Сохранены
доказательства60 Python tests (24base+22selection+14compression/progress),
прошедших для этого исходника. [Подробности v1.1.3](VALIDATION-v1.1.3.md).
Изменённые UI/обновления проверены новым JVM прогоном. Первый прогон1.1.4
прошёл; после пожелания видимой подписи Auto/hint выполнен итоговый прогон.

## Ревью и логическая инспекция

Opening gate process-local, без ссылок на Activity. onStop сбрасывает только
вне configuration change; onResume потребляет одну попытку. Disabled/busy
opening не повторяет попытку; сбой worker не сбрасывает latch. Auto preference
сохранена, старый attempt timestamp игнорируется. Включение галочки само не
запускает запрос; действие действует на следующее открытие, ручное доступно.
Auto/manual используют прежний BUSY; hash/package/version/signer/installer
проверки не ослаблены. Галочка не вызывает ручную проверку и не закрывает меню.
Изменение lifecycle не затрагивает PTY/агентов/остановку пользовательских процессов.
Библиотекаv8/manifestv1/v2 без миграций или сброса; совместимость описана RU/EN.

## Непроверенное и ограничения

Новые плитка/foreground lifecycle требуют проверки на Android. Robolectric не
доказывает жесты/DPI, фоновые ограничения, реальную установку pigz или APK.
Агент не устанавливал APK/пакеты и не запускал восстановление пользовательских
данных, не менял Bridge/терминальные сессии. В UI бэкапов остаётся (тестирование).
Native proot/pigz тестировались fixtures; нет замера реального ускорения/нагрева.
Нет foreground service/wake lock/ETA; оставляйте приложение открытым при бэкапе/APT.
Фоновые GitHub ошибки тихие; rate limits/сеть могут отказать, ручная проверка
показывает ошибку. У v1.1.0 до обновления сохраняется прежний10-часовой интервал.
Upstream lintVital/JNA на Linux/aarch64 выдаёт предупреждение об unsupported host;
Gradle успешен, но полный чистый lint не заявляется. Мерцание OpenCode не исправлялось.

Receipts: .verification/release-v1.1.4/ — build, JUnit XML, badging, signature,
public baseline certificate, artifact.json, APK, SHA256SUMS, update.json.

## Исправление документации после публикации

Три ссылки в release body заменены на полные адреса blob/v1.1.4/docs/fork;
все страницы проверены анонимными HTTPS GET: HTTP200. Body перечитан с GitHub
и совпал с локальным документом. About теперь ведёт на releases/latest форка,
вместо F-Droid оригинального Termux. Другие публичные release bodies проверены:
таких относительных ссылок нет. APK/metadata/assets и тег не менялись;
повторная сборка для исправления ссылок не выполнялась.
