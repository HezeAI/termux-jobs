# Проверочная v1.6.14-test — восстановление Linux и плашки окружений

com.termux, code1210, Android8+/ARM64, 36687777 байт.
SHA-256: `d88264d5a3fdf3430ae1c3c5083b005715681e71c89f0a849041f9eb7fc1a1e6`.
Сертификат: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` — прежний.

Изменение: rootfs репакуется только в новый jobs-*; добавляется созданный нами
минимальный manifest.json (null Entrypoint, Cmd=/bin/sh, WorkingDir=/, Linux PATH).
OCI-метаданные источника не переносятся; rootfs/passwd не редактируются. Это
сохраняет возможность безопасного native run у Void без Bash. Восстановление
возвращает явную команду run --isolated --work-dir / jobs-* -- /bin/sh -i.
В плане восстановления есть пояснение RU/EN. В списке окружений справа отдельные
Install/Установить и disabled gray Installed/Установлено; выбор строки сохраняется.

28 Python-тестов бэкапа прошли, включая новый missing-Bash Void, сохранность
passwd/ссылок/архива источника, отказ от чужих Entrypoint/Env/image_ref, замену
foreign manifest symlink безопасным regular manifest и отклонение rootfs записи
через символьную ссылку до native restore. Native run из установленного proot
проверен через AST-extracted функцию с dependency mocks: созданный helper-ом
манифест разрешает явный /bin/sh -i и сохраняет isolated/work-dir. Ни пользовательский
proot-процесс, ни реальное восстановление не запускались этой проверкой.

32 целевых Java-теста прошли (backup forms/consent/progress/import/cache,
WorkspaceEnvironments/StartupEnvironment). Подтверждены безопасный выбор
отсутствующей среды с запросом установки и сохранение выбранного alias.
Мок UI не доказывает размещение новых плашек/области нажатия на устройстве.
Сборка и DEX runtime прошли. ZIP/package/version/minSdk/ARM64/non-debuggable,
v2-подпись/прежний сертификат/SHA проверены. Из assets изменён только
workspace-backup.py; agent/server/bootstrap/control assets и native совпали1.6.13.
Проверяемые приватные файлы и секретные маркеры отсутствуют. git diff --check проходит.

Ревью: записи ограничены новым alias и private work; не копируются OCI Entrypoint,
переменные и путь образа; tar/symlink/hardlink ограничения сохранены; запись manifest
не зависит от содержимого rootfs. Текущие среды не заменяются. Изменений механизмов
остановки процессов нет. Логическая инспекция: inventory выбора alias, native backup,
проверка архива/архитектуры/контрольных сумм, репак/restore, environmentBindings,
импорт isolated пресета, динамический выбор login/run и файл-транспорт.
Формат библиотеки8 и протоколы бэкапа1/2 не менялись, очистки данных нет.
Code1210 выше1.6.13/code1209, пакет/сертификат сохранены. Копия Download сверена.

Пробелы: фактическое создание/проверка/восстановление полного Debian, сохранность
исходной среды, чтение файлов/tree и повторный старт restored Void ещё не проверены
на устройстве. Сравнение пакетов бэкапа остаётся Termux APT-only; для Linux полная
система переносит собственные установленные пакеты. Прежние базовые package install/run
шести систем подтверждены разработчиком; это не доказывает их восстановление.
GitHub не публиковался. Следующая минимальная проверка — только Debian без HOME/PREFIX
и библиотеки, затем manifest.json → отдельная восстановленная копия, tree/файлы.


## Реальная проверка разработчиком после установки
Бэкап только Debian создан: папка Termux-Jobs-20261019-195137-d09fc64b, Linux
архив70349734 байта; SHA и размер соответствуют manifest.json, исходный alias
 debian, arch aarch64, compatible. Присутствует также штатный packages.txt Termux,
при restore он не выбран. Native restore остановился (PROOT_DISTRO_FAILED).
Read-only осмотр: jobs-6a650d598a1d частично извлечён, без manifest.json. Точка
остановки перед первым PAX path115 символов; isolated репак одного TarInfo
доказал, что старый pax_headers.path возвращает имя debian/rootfs поверх нового
jobs имени. Исправление1.6.15: сброс extended headers и регенерация путей.
Не считать восстановление на устройстве успешным по тестам1.6.14.
