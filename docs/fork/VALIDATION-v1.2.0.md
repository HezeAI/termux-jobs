# Проверки v1.2.0

Финальный APK `Termux-Jobs-v1.2.0-arm64.apk`: 36476761 bytes (~36.5МБ), SHA-256
`d270bfd36a7515ab6facbb21586db7087ea2870a253b227cb315cbe4202a70e8`. com.termux/versionName1.2.0/code1071 >1061 публичнойv1.1.4
и >1070 локальнойv1.1.13. Android min24/target28, толькоARM64, non-debuggable.
Сертификат SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`, прежний для публичнойv1.1.4
и локальных сборок. apksigner APKv2 PASS. ZIP CRC/duplicate/traversal/stored4byte
alignment PASS. Java/resources/assets совпадают с release stage. Native побайтно
идентичны публичнойv1.1.4; agent.py идентичен локальнымv1.1.12/13.
Ограниченный скан приватных ключей/токенов/auth/keystore не выявил совпадений;
он не является универсальной гарантией отсутствия секретов.

## Проверки кода

- 379 JVM tests,0 failures/errors/skipped: финальный код проверен на локальной
  v1.1.13; вv1.2.0 менялись только versionName/code и документация. Повторный
  прогон неизменённого поведения не заявляется. Лог jvm-final2.log сохранён
  из .verification/servers-v1.1.13; исходники Java/resources сверены со stagev1.2.0.
- 6 Python search +20 agent tests PASS на финальных неизменённых helpersv1.1.13.
- 15 Python schedules tests повторно PASS при подготовкеv1.2.0.
- Включены profile roundtrip/старые поля/изоляция, подтверждения без remote actions,
  UI-кнопки, Filename search limits/literal/Unicode/symlink/partial, progress/ETA,
  finished-record deletion с запретом active и без сигналов/удаления результатов,
  runner locks/enable/disable/edit/delete/failure, fakecron/systemd, собственные
  дочерние процессы, editor revision-conflict/несохранённый текст, existing terminal tests.
- Release build успешен. Localization581 PASS наv1.1.13, новые RU/EN проверены
  в исходниках/целевых тестах. git diff --check и проверка документационных ссылок.
- Полный чистый Lint не заявляется: прежние lintVital/JNA aarch64/Kotlin metadata/
  custom lint несовместимости. Итоговую сборку это не заблокировало.

## Ревью и границы

Прослежены профиль/подключение/hostpin/password, раздельное владение SSH-процессами,
cancel/close/download cleanup, agent admission/unmanaged/forget, scheduler create/
edit/disable/delete/running locks, внешняя конкуренция crontab, player lifecycle/
loopback-only HTTP и безопасные ошибки. Использованы текущие права SSH, без
автоматического sudo/linger. Agent и расписания не удаляют чужие проекты/службы.
Смена endpoint сохраняет local favorites/actions, запуск показывает новую цель.
Новые поля необязательные; библиотекаv8/AI/окружения без миграции и сброса.

## Что не доказано локальными тестами

Разработчик HezeAI установил и проверил промежуточные сборки на устройстве.
Разработка велась с помощью AI-ассистента; локальные автоматические проверки
описаны выше. Отдельной проверки в целевой среде требуют разрешения, клавиатура,
жесты/DPI, фактическая скорость и отмена SSH/SFTP, поддержка конкретных кодеков,
настоящие cron daemon/user-manager/linger/reboot и полное восстановление.
Автоматические проверки cron/systemd используют подставные команды.

## Обновлятор и публикация

schema1 update.json создан из финального APK, RU/EN highlights содержат совместимость.
Пакет/подпись/код/размер/SHA согласованы. Latest должна содержать APK/SHA256SUMS/
update.json, автоматически проверяется при открытии при включённой автопроверке;
ручная проверка сохранена. Это предложение обновления, не push/автоустановка.
Сверка загруженных assets/тега/ссылок фиксируется отдельной квитанцией публикации.
Доказательства: .verification/release-v1.2.0 и servers-v1.1.13.

[Квитанция фактической публикации](PUBLICATION-v1.2.0.md).
