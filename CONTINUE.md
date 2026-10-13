# Termux Job’s v1.0.4

Пользователь одобрил APK и прямо запросил GitHub-релиз. Выпуск соответствует тегу
v1.0.4, versionName 1.0.4/versionCode 1041, com.termux, прежнему сертификату,
Android 7+/ARM64; формат библиотеки v8 не меняется. APK и SHA256SUMS в release assets.
README/CHANGELOG/FORK/USAGE/DESIGN/ARCHITECTURE/LOCALIZATION, release notes,
validation и инструкции участникам актуализированы.

Основное после v1.0: APT/dpkg менеджер Termux/Ubuntu по долгому тапу сессии,
версии/downgrade/description/Подробнее, подтверждение выбранной свободной Bash
либо новой сессии. Пакеты остаются в окружении, SSH inventory не реализован.
Файловый редактор после Preview: UTF-8<=256КБ, Markdown, Snapshot hash/stat,
права/владелец/temp/mv, конфликт и сохранение буфера при ошибке, dirty close.
Не поддержаны links; optimistic check/rename не CAS для внешних writers,
нет autosave/crash recovery буфера. ProjectFiles использует stdin payload.

Действия команд/заметок в openCommand/openNote, троеточия этих записей убраны.
categoryField предлагает подсказки и отдельную ▾ кнопку полного списка при new/edit,
новое значение вводится свободно. 254 tests, release/lintVital, localization472
и финальные APK checks прошли. Подробный протокол устройства не предоставлен;
не выдавать mock tests за реальную проверку IME/DPI/SSH/Android lifecycle.
Детали SHA-256/границ: docs/fork/VALIDATION-v1.0.4.md.

Рабочие пользовательские среды/сессии/проекты агентом не менялись. При следующем
релизе обновлять всю применимую документацию по docs/fork/RELEASING.md, исторические
отчёты сохранять. Последующие публикации/установки определяются текущей задачей,
в Git не добавлять secrets, локальные инструменты, Alpha и пользовательские данные.
