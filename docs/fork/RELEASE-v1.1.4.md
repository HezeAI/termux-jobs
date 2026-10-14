# Termux Job’s v1.1.4 — улучшенный менеджер резервных копий

## Главное: резервное копирование (тестирование)

**Шестерёнка справа сверху → Резервное копирование (тестирование).**
Улучшения относительно последней публичной v1.1.0:

- Полная копия либо «По выбору»: раздельные компоненты Termux/Ubuntu — Codex с
  авторизацией/историей, другие AI-агенты, SSH/Git, проекты/папки, система и инструменты.
- Размеры до сжатия, проверка зависимостей перед созданием/восстановлением.
  Недоступные пути отмечаются; неполные компоненты отключены, ошибки не скрываются.
- Codex SQLite: согласованное копирование каждой базы, исключение временных IPC,
  locks/WAL/SHM. Весь бэкап не является атомарным снимком работающей среды.
- Время выполнения, полоса текущего этапа и сменяющееся имя файла в одной строке.
  Процент только при известном объёме; иначе анимация, без выдуманного ETA.
- gzip/pigz, уровни1/3/9 (по умолчанию gzip3). pigz до4 потоков, установка в общей
  среде Termux только после отдельного согласия, в фоне приложения с проверкой успеха.
  Современная полная Ubuntu сохраняется штатным proot-distro stdout и выбранным
  компрессором; legacy proot-distro использует штатное сжатие с пояснением.
- Все окна бэкапа и подменю защищены от случайного закрытия тапом снаружи.
  Явная отмена сохранена. Создание доступно только после согласия, папка назначения
  явно подписана. После успеха: «Бэкап создан и проверен» и путь.

Проверка чужого архива, SHA-256/структуры/сжатия, выбор компонентов, отдельное
восстановление Ubuntu, импорт библиотек/aliases/scripts и сравнение пакетов сохранены.
HOME/PREFIX/data-only извлекаются отдельно, работающая среда не заменяется.
[Подробная инструкция](https://github.com/HezeAI/termux-jobs/blob/v1.1.4/docs/fork/BACKUP-RESTORE.md).

## Обновления — одна плашка

В шестерёнке «Проверить обновления»: название запускает ручную проверку,
справа квадратная галочка **«Авто»**, под названием — **«Проверять при открытии приложения»**.
Одна фоновая попытка при каждом открытии/возврате вместо интервала10часов.
При ошибке повтор только при следующем открытии; вкладки/поворот экрана не повторяют.
Автоматическую проверку можно отключить, ручная остаётся. Без popup, скрытой
загрузки APK и установки. Проверка/скачивание отображают индикатор, объём/процент,
затем выполняются SHA-256/package/version/signer checks и подтверждение Android.

## Совместимость и данные

**Очистка команд, заметок, пресетов и истории не требуется.** com.termux,
прежняя подпись, versionCode1061, библиотекаv8, Android7+/ARM64. Устанавливайте поверх,
не удаляя приложение; перед обновлением завершите важные задачи, поскольку Android
может остановить процессы. Новая установка pigz меняет пакеты/зависимости Termux
только с отдельного согласия; само обновление APK не устанавливает pigz.

Последняя публичная v1.1.0 получает релиз через свой updater: ручная проверка
сразу после публикации, прежняя автопроверка с интервалом10часов. Новая схема
при каждом открытии начнёт действовать после установки v1.1.4. Старые сборки
без updater требуют ручной установки. Специальный резервный code1100 не понижается.

Полные manifestv1 поддерживаются; выбранные данные имеют manifestv2, требуют
принимающего клиента v1.1.1+ (публичный v1.1.4) либо доверенного restore.sh из бэкапа.
Стандартный .tar.gz одинаково восстанавливается без pigz. Отдельно извлечённым
данным нужны ручная активация и проверка зависимостей/авторизации. Процессы,
Android Keystore и внешние цели ссылок не переносятся; не обещается перенос1:1.
Оставляйте приложение открытым до окончания бэкапа/установки pigz: foreground
service/wake lock нет. AI Agents экспериментальны; мерцание OpenCode не исправлялось.

[Проверки и оставшиеся сценарии](https://github.com/HezeAI/termux-jobs/blob/v1.1.4/docs/fork/VALIDATION-v1.1.4.md) · [Обновления](https://github.com/HezeAI/termux-jobs/blob/v1.1.4/docs/fork/UPDATES.md).

## English highlights

**Main feature: improved backup manager (testing)** in the top-right settings gear.
Full or selective Termux/Ubuntu components, projects/Codex history/auth/SSH,
dependency review, individual online SQLite snapshots, unreadable-item reporting,
elapsed time/stage progress/replacing filename, gzip/pigz levels1/3/9, optional
consented pigz installation, and outside-tap protection throughout backup dialogs.

A single update tile combines manual checking and an explained Auto checkbox.
One silent attempt per opening/foreground return; failures retry after reopening.
No automatic download/install. Existing public v1.1.0 can update through its updater;
its old10-hour interval applies until updated, or use a manual check now.

No command/note/preset/history reset. Same com.termux signer, code1061, libraryv8.
Selective manifestv2 requires a newer reader; fullv1 remains supported. Standard
gzip archives restore without pigz. Staged data needs manual activation; processes
and Android Keystore do not migrate. Keep the app open during backup/pigz setup.
Not universal1:1 restore; on-device verification of the new update UI is pending.
