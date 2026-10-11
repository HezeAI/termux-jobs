# Карта исходников

| Модуль | Назначение |
| --- | --- |
| `app` | Android UI, жизненный цикл, сервис, установка окружения |
| `terminal-view` | Отрисовка терминала, ввод, жесты |
| `terminal-emulator` | Эмулятор терминала, буфер экрана, PTY/JNI |
| `termux-shared` | Константы, настройки и общие утилиты |

Начальная точка UI: `app/src/main/java/com/termux/app/TermuxActivity.java`.
Сессии и фоновые процессы: `TermuxService.java` в той же папке.
Установка bootstrap: `TermuxInstaller.java`.
Ввод и события терминала: `app/src/main/java/com/termux/app/terminal/`.
Ресурсы: `app/src/main/res/`.
Общие пути и идентификаторы:
`termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java`.

Рабочее пространство находится в `app/src/main/java/com/termux/app/workspace/`:

- `WorkspaceController`: нативные экраны, редакторы, вкладки и связь с сервисом.
- `WorkspaceStore`: модели команд/профилей, версионированный JSON, AtomicFile,
  последовательная запись в фоне и защита исходного файла при ошибке чтения.
- `SessionShell`: отдельный rc-файл Bash, HISTFILE и безопасное экранирование путей.

`TermuxActivity` управляет жизненным циклом контроллера. События обновления,
переключения и завершения сессий передаются из `TermuxTerminalSessionClient`.
`TerminalView`, эмулятор, JNI и foreground service остаются исходными.
Новые панели встроены в `activity_termux.xml`; терминал остаётся измеренным
при показе Главной, чтобы фоновые процессы продолжали работать.

Файловый менеджер, темы и экспорт из дополнительных примеров референса
не включены в текущие три приоритетные возможности.
