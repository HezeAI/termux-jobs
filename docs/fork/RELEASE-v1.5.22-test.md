# v1.5.22-test — удаление локальных ИИ

Приватная сборка для проверки, com.termux, code1159 >1158, Android8+/ARM64,
прежний сертификат. Публикация на GitHub не выполняется.

Удалены локальные ИИ полностью: llama.cpp, LiteRT-LM, NPU-библиотеки,
отдельная служба, модели в каталоге, импорт, инструменты и настройки,
локальные истории и пресеты. При чтении библиотеки старые локальные профили
пропускаются и не превращаются в обычные Bash-сессии.

**Потеря данных этого раздела преднамеренна:** первый запуск нового APK удаляет
только приватные `files/local-ai`, `cache/local-ai` и настройки локальной
диагностики. В этих каталогах находятся скачанные/импортированные копии моделей,
локальные чаты, инструкции и настройки среды. Если они нужны, экспортируйте
их до обновления. Внешние исходники импортированных моделей не удаляются.
Очистка выполняется в фоне, без следования символьным ссылкам.

Формат общей библиотеки остаётся8. Не требуется очищать данные приложения,
команды, заметки или остальные пресеты. Сохранены текущие интерфейсные изменения,
истории и правила закрытия обычных сессий, Codex/Gemini/Claude/OpenCode и их
настройки прав, файловый менеджер/редактор/плеер, сервер+агент/SFTP, пакеты,
справочник, бэкапы и обновлятор. Нативные истории CLI-агентов не удаляются.

На устройстве разработчика по его запросу удалён ровно отдельный контейнер
`jobs-local-ai` (304125KiB выделенного места). Перед удалением проверены
расположение, маркеры, отсутствие активного proot/runner и вложенных mount points;
проверено сохранение рабочей `ubuntu`. APK сам не удаляет чужие proot-окружения.

English: complete removal of on-device AI and its local chats/presets/runtimes.
First launch intentionally removes the retired app-private models/history/cache
and diagnostics preferences only. Export needed data before update. Other app
libraries, native CLI histories, regular environments and server features remain;
no app-data reset is required. Legacy local-AI profiles are skipped, not launched
as shells. Private code1159, same package/certificate; no GitHub publication.

[Проверки и итоговый файл](VALIDATION-v1.5.22-test.md).
