# Проверки v1.1.0

Package com.termux, versionName1.1.0, versionCode1057, minSDK24/target28,
ARM64 only, release nondebuggable. APK37806077bytes.

SHA-256: `80030ff0e099391840761fad13fc745a52810025233adfb29623a3f38772654b`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Совпадает с v1.0.18 и локальной v1.0.19; code1057 выше1055/1056.
Новые поля optional v8, очистка библиотек/истории для обновления не нужна.

## Проверки кода и сборки

- Изолированная release-сборка и lintVital: BUILD SUCCESSFUL.
- 302 JUnit/Robolectric tests: failures0/errors0/skipped0.
- Новые сценарии: десятичасовой gate/откат часов; settings на трёх вкладках;
  metadata/URL/package/ABI/hash/размер/signatures/downgrade updater; default-log
  migration; импорт пресетов с review/new IDs/no live handles/no SSH ciphertext;
  замена без удаления active/base; merge/dedup; некорректная библиотека не очищает
  текущую; aliases только по выбору; scripts требуют восстановленных файлов;
  отдельный isolated launch preset восстановленной Ubuntu.
- 24 Python fixture-теста: HOME/aliases/scripts без выполнения; старый/новый/nested
  proot layout, ELF arch mismatch/unconfirmed; traversal/absolute paths, duplicate
  members, symlink writes в обоих порядках, external symlink без dereference,
  unsafe hardlinks, повреждённый gzip trailer, modes/content/links; no overwrite;
  manifest path/hash/INCOMPLETE; смена исходного архива до restore; HOME staging
  оставляет текущий файл прежним; запрет destination PREFIX; create→inspect;
  APT-аргументы/симуляция; пакеты Ubuntu не смешиваются с Termux.
- Python и Bash syntax, git diff --check. Native/agent JS не менялись.
- ZIP CRC, stored alignment4, APK badging/version/ABI/nondebuggable, v2 signature,
  signer hash и SHA-256 финального файла.
- Все изменённые core Java/XML совпадают с изолированной staging-копией. Новый
  asset workspace-backup.py совпадает с исходником. Остальные assets/native .so
  побайтно прежние. В APK нет __pycache__/pyc или пользовательских auth/key файлов;
  адресный скан содержимого на известные secret patterns совпадений не дал.
- update.json сгенерирован из финального APK. Schema1/name/tag/size/hash/ABI/minSDK
  и versionCode соответствуют требованиям установленного v1.0.19 reader.
  Совместимость включена в RU/EN changes, отображаемые старым updater.

## Ревью и логическая инспекция

Рассмотрены запуск/повторный resume auto-check, ручная проверка без10ч-ограничения,
тихая ошибка сети, cached badge, отсутствие APK download без выбора, отмена и
потеря Activity. Проверки пакета/сигнатуры/повышения code перед installer; Android
делает финальную проверку подписи. Нет shell credentials в запросах GitHub.

Backup: нет автоматических действий из простого открытия меню. Отдельный worker,
Python -I -S, progress/cancel, subprocess output не превращается в отчёт. Stop
только принадлежащего worker/его дочернего инструмента, без pkill/чужих сессий.
Восстановление снова проверяет fingerprints и приватные copies; no-follow dirfd
extraction в новый каталог. Существующие HOME/PREFIX/rootfs не заменяются.
Для Ubuntu перепаковка fresh alias, native restore, без foreign host config scripts;
пресет isolated, не VM. APT plan только simulation, фактическая установка — новый
терминал после отдельного выбора с подтверждением APT. Library decode прежде
мутации, private recovery snapshot, защита active/base, проверка persisted IDs.
Рассмотрены failure/partial paths; компоненты не глобальная транзакция.

## Практические ограничения и непроверенные сценарии

**Бэкапы — бета.** Реальное многогигабайтное копирование/перенос среды, Android
storage permissions, иной UID/дистрибутив/версия proot, живые SQLite-базы, авторизация
и история на чистом устройстве не проверялись. Python tests используют временные
fixtures и mocked native proot/APT; это не успешное восстановление пользователя.
HOME/PREFIX только staging; Keystore/permissions/processes/xattrs/ACL не переносимы
автоматически. Старый host proot runtime не поддерживает отдельную Ubuntu в этой
бете. Полный состав/лимиты: [BACKUP-RESTORE](BACKUP-RESTORE.md).

Новые окна, keyboard/DPI/жесты/индикатор на реальном Android не подтверждены
мок-тестами. Сам installer не запускался; первый настоящий upgrade v1.0.19→v1.1.0
проверит пользователь. Известное мерцание OpenCode не исправлялось. Агент не
создавал/восстанавливал реальные бэкапы пользователя, не ставил APK, не менял
installed Termux/Ubuntu, пакеты или работающий Bridge.

Локальные receipts: .verification/backup-v1.1.0/. Первые проверки обнаружили
ошибку fixture-ожидания is_dir для external symlink (путь существует на телефоне);
ожидание исправлено на is_symlink. Ревью добавило раздельный учёт nested Ubuntu
packages и удаление generated pycache из assets; финальные проверки выше прошли.
Warning JNA filesystem layer unsupported Linux/aarch64 — предупреждение host lint,
не тест Android-устройства; сборка завершилась успешно.

## Проверка публикации

Опубликован [v1.1.0](https://github.com/HezeAI/termux-jobs/releases/tag/v1.1.0) как Latest/stable: draft=false, prerelease=false.
Тег исходников fd580488f4ca27a01b35fa2b832cb5c153a78e82. Дерево исходников GitHub API совпало с локальным
5eb1f00e83ffd1c97ec7905b7c802551e1818abb; обычный GitHTTPS403/SSHtimeout
обойдены штатным Git Database API. Это не обход пользовательских разрешений:
публикация явно авторизована в текущей задаче. APK/SHA256SUMS/update.json скачаны
обратно, побайтно совпали; hash/size/version metadata согласованы. Верифицирован
статус всех assets и latest endpoint. Receipts в .verification/backup-v1.1.0/.
Прямая загрузка/установка из Android-приложения всё ещё ожидает пользовательской
проверки; успешная загрузка через gh не заменяет её.
