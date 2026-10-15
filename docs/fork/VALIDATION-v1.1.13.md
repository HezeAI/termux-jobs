> Исторический отчёт локальной сборки. Текущий публичный выпуск: [v1.2.0](RELEASE-v1.2.0.md).

# Проверка локальной v1.1.13

APK `Termux-Jobs-v1.1.13-arm64.apk`, 36476773 bytes, SHA256 `ae757e429a0f208b1ec6777ff6b140e729c97c08dea2dc5fdb9c38c0abef02fa`.
com.termux/code1070 >1069, min24/target28, толькоARM64, non-debuggable.
Прежний certSHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`, APKv2 checked.
ZIP CRC/duplicate/path/alignment4, совпадение исходных Java/resources/assets со
stage проверены. Native и агент идентичныv1.1.12. Ограниченный скан приватных
ключей/токенов/auth/keystore не выявил совпадений; не универсальная гарантия.

379 JVM-тестов,0 failures/errors/skipped; 6 Python search и20 agent tests PASS.
Новые проверки: сохранение/изоляция избранного и команд по профилям; старые
профили без полей; кнопки/диалоги без запуска SSH до явного действия;
подтверждение pinned command; файлы через меню; честная неопределённость диагностики;
unknown-size/rate/ETA; активные записи не забываются, завершённые записи/логи
удаляются без сигналов процессам и удаления результатов команды.
Поиск: подпапки/регистр/Unicode, отсутствие поиска содержимого, literal query,
symlink/cycle skip, лимиты/неполный результат, invalid query/root.
Существующие тесты редактора/UTF8/revision-conflict/несохранённого текста прошли.
20 тестов агента используют только собственные временные fixtures/дочерние процессы,
пользовательские сервер/агент/службы не менялись. Расписания/Python-helper не менялись.

Release build successful. Localization581 PASS, новые RU/EN тексты проверены
в исходниках/целевых тестах. git diff --check PASS. Полный чистый lint не
заявляется: прежние JNA/aarch64/Kotlin metadata/custom lint несовместимости.

Ревью: новые поля профиля необязательные, save atomic и rollback UI при ошибке;
редактирование профиля сохраняет избранное/команды. Все команды подтверждаются
с endpoint/каталогом/текстом; pin/save не запускает процессы. Agent forget уже
существовал, новые stop/kill протоколы не добавлены. Текущие действия журнала
обновляются при изменении состояния. Поиск передаёт JSONstdin и фиксированный
Python, не подставляет query в shell; читает имена, ограничивает traversal.
Отмена search/download закрывает собственный SSH, не shared monitoring/tunnel.
Download cleanup удаляет только созданный им новый UUID-файл; ошибка очистки
показывает путь. Upload retains прежние conflict/temp/cancel protections.

Android/UI/клавиатура/малыеэкраны/живойSSH/SFTP, отмена скачивания с настоящим SSH,
скорость/ETA реального файла и изменения удалённого файла во время чтения
не проверялись на устройстве. Следующие проверки: звёздочка/лупа, pinned confirm,
удаление завершённой записи, download/upload+cancel, узкий экран/keyboard.
Точность ETA не гарантируется; unknownsize не получает процент/ETA.
См. [функции и ограничения](RELEASE-v1.1.13.md).
Доказательства `.verification/servers-v1.1.13/`: jvm-final2.log, build-final.log,
search-python.log, agent-python-final.log, signature.txt, badging.txt, artifact.json.
