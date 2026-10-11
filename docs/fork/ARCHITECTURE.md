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

До получения требований новые экраны, заглушки функций и архитектурные слои
не добавляются. Изменения UI следует начинать в app и terminal-view,
сохраняя жизненный цикл сессий и работу фонового сервиса.
