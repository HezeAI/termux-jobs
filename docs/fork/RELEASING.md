# Подготовка релиза

Этот порядок применяется к каждому новому выпуску Termux Job’s на GitHub.
Авторизация публикации определяется текущей задачей пользователя;
этот документ сам по себе не является разрешением отправлять изменения.

## Версия и совместимость

1. Определить итоговый набор изменений и обновить versionName/versionCode.
2. Сохранить пакет и сертификат для совместимого обновления; отдельно описать
   любые изменения совместимости, формата данных или требований Android.
3. Убедиться, что ключ подписи, личные данные, Alpha и локальные инструменты
   не попадают в коммит или assets.

## Документация перед публикацией

GitHub release titles/descriptions and new public release notes must be written
in English unless the developer explicitly requests another language. Keep the
in-app update highlights and compatibility notes localized in both Russian and
English. This is the developer's standing preference for future updates.

End every GitHub release description with this exact credit line, as requested
by HezeAI: `AI development assistance: OpenAI Codex`. Keep HezeAI credited as the fork developer;
this line identifies AI assistance.

For the separate `HezeAI/termux-jobs-components` repository, keep component details,
compatibility, source credits and licenses in its English README and exact
versions/sizes/hashes in `components.json`. Update that overview when components
are added or revised. Component release descriptions may stay brief: the set
identifier, a README link and the credit line above. Keep published tags and
binary assets immutable. This does not shorten application release notes or
replace the application's localized updater/compatibility requirements.

Обязательно проверить и обновить по фактическому состоянию:

- `README.md`: текущая версия, возможности, установка, ссылки и ограничения.
- `CHANGELOG.md`: изменения относительно предыдущего публичного выпуска.
- `docs/fork/RELEASE-<tag>.md`: самостоятельное описание нового релиза.
- `docs/fork/VALIDATION.md`: итоговый APK, размер, SHA-256, сертификат,
  выполненные проверки и непроверенные сценарии.
- `FORK.md`: версия и актуальные требования сборки.
- `docs/fork/USAGE.md`, `DESIGN.md`, `ARCHITECTURE.md`: изменившееся поведение,
  интерфейс, хранение и выполнение команд.
- `CONTINUE.md`: актуальный контекст продолжения работы.
- Шаблоны Issues и `CONTRIBUTING.md`: актуальные вопросы и ссылки.

Не переписывать историю прежних релизов как новые результаты. Старые отчёты
помечать историческими. Если документ не требует изменений, проверить его
соответствие релизу; бессодержательные правки не нужны.

## Проверки и публикация

1. Выполнить подходящие тесты и собрать релиз в изолированной среде.
   Перед публикацией пройти `bash scripts/verify-modern-lint.sh` по [LINT.md](LINT.md):
   анализ всех пяти модулей, отсутствие ошибок загрузки правил и блокирующих
   находок. Старый lint из APK-цепочки сам по себе этого не подтверждает.
   Неразобранные предупреждения и ограничения версии SDK указывать в отчёте;
   не скрывать их общей baseline/disable-настройкой.
2. Проверить пакет, версию, ABI, debug-флаг, подпись и целостность APK.
3. Вычислить SHA-256 именно финального APK и сформировать `SHA256SUMS`.
4. Проверить ссылки, отсутствие секретов и `git diff --check`.
5. Создать коммит и тег, связывающий опубликованный APK с исходниками.
6. Опубликовать APK, `SHA256SUMS` и описание выпуска при наличии разрешения.
7. Проверить загруженные файлы, хеши, ссылки и статус draft/prerelease.
8. Обновить контекст продолжения: опубликованная версия и оставшиеся проверки.

Последующее исправление документации в основной ветке не требует пересборки
неизменённого APK или переноса уже опубликованного тега.

## In-app updater (since v1.0.19)

Alongside APK and SHA256SUMS, upload **update.json** to each stable release.
Generate it from the final verified APK, not a preliminary build:

```sh
python3 scripts/create-update-manifest.py \
  --apk .verification/release-vVERSION/Termux-Jobs-vVERSION-arm64.apk \
  --aapt2 /path/to/existing/aapt2 \
  --notes docs/fork/UPDATE-NOTES-vVERSION.json \
  --output .verification/release-vVERSION/update.json
```

Notes contain `ru`/`en` change arrays and a required `compatibility` object with
`ru`/`en` arrays. The generator appends compatibility to highlights, up to20 total
lines per language, so existing v1.0.19 clients display it before installation. Use the same
APK/hash/version as the release assets. Upload metadata before publishing the
release. Download all three assets back and verify consistency. The updater uses
GitHub's latest stable release, requires increasing versionCode and exact current
signer/package match, and never offers uninstall as an update workaround. Missing
metadata produces an explicit unsupported-release message. A successful source
build does not establish end-to-end Android installer or data-migration behavior.
Do not publish an artificial higher-version release solely to test the updater.

For every future stable release, explicitly document data compatibility in RU/EN
update highlights as well as release notes. Include any required clearing of
commands/presets, loss of data, incompatible formats or migrations and their
reasons **before** installation. If no reset is required, say that explicitly.
Keep the metadata schema readable by installed v1.0.19 clients. Never silently
reset user data or publish without the updater assets.

## Ссылки в описании GitHub Release

В публикуемом release body используйте полные HTTPS-ссылки на документацию,
например https://github.com/HezeAI/termux-jobs/blob/vVERSION/docs/fork/BACKUP-RESTORE.md.
Относительный BACKUP-RESTORE.md из страницы releases/tag не ведёт в docs/fork.
Перед публикацией проверьте наличие файлов в соответствующем теге, после неё —
публичную доступность каждой ссылки и совпадение release body с подготовленным.
Для исправления только текста/ссылок редактируйте release notes и документацию,
не пересобирайте APK, не передвигайте опубликованный тег и не меняйте update.json.

## Проверка runtime-зависимостей (v1.3.3+)

Прохождение JVM-тестов не доказывает состав Android APK: варианты зависимостей
могут различаться. preBuild проверяет файловые API Guava в debug/release runtime.
ARM64 build запускает scripts/verify-apk-runtime.py на финальном APK. Не удаляйте
ошибку проверки добавлением в список исключений без определения происхождения
типа, его вызывающих путей и доступности на поддерживаемом Android.
Сверяйте определения DEX, а не только строки ссылок. Staging не должен содержать
удалённых Java-исходников. Изменение minSDK указывать в RU/EN metadata до установки.
