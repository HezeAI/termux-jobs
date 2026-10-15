> Исторический отчёт локальной сборки. Текущий публичный выпуск: [v1.2.0](RELEASE-v1.2.0.md).

# Проверка локальной v1.1.11

APK `Termux-Jobs-v1.1.11-arm64.apk`, 36466568 bytes; SHA-256 `ff7af49a4de08de1a82b594fea69fe2363ea74f1e572e6609021f88c53ed2053`.
com.termux, versionName1.1.11/code1068 >1067, min24/target28, ARM64, non-debuggable.
Сертификат SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` сохранён, APKv2 проверен.
ZIP CRC, отсутствие дубликатов/опасных путей, stored alignment4, совпадение исходников
Java/resources/assets с build stage проверены. Native-библиотеки и агент идентичныv1.1.10.
Ограниченный скан приватных ключей/токенов/имён auth/keystore ничего не выявил;
он не является универсальной гарантией отсутствия секретов.

371 JVM-тест:0 failures/errors/skipped. Включены политика точного loopback,
безопасные сообщения ошибок, новая геометрия кнопок и выбор режима задачи.
15 Python-тестов: OK. Проверены собственный runner, блокировки/отключение,
валидация, symlink, чужие cron/unit записи, ошибки установки, изменение расписания,
ограниченные журналы и остановка собственного дочернего процесса без затрагивания
контрольного соседнего процесса. Cron/systemctl подставные, настоящий сервер не менялся.
Release build successful. Localization581 PASS. Полный чистый lint не заявляется:
прежние несовместимости lintVital JNA/aarch64/Kotlin metadata/custom lint сохраняются.

Ревью: HTTP ограничен127.0.0.1, внешнего разрешения нет; raw ошибки/URL не выводятся.
Прослежены create/enable/disable/edit/delete, неясный remote commit, user-systemd,
cron reread и runner process-group ownership. Не используется остановка по сохранённомуPID.
Отключение не убивает текущую задачу, удаление running запрещено. Внешний редактор
crontab не участвует в app lock: не обещается атомарность относительно него.
Подробные ограничения: [расписания](SCHEDULES.md).

Не выполнены установка APK, Android-жесты/раскладка, реальное воспроизведение,
живой Cron/systemd/SSH и reboot/linger. Нужна ручная проверка пользователем.
Доказательства: `.verification/servers-v1.1.11/` (build-final2.log,
jvm-final3.log, schedules-python.log, signature.txt, badging.txt, artifact.json).
