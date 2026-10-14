# Передача Astra: OC-001, диагностика без изменения оригинала

## Задача
Найти доказанную причину мерцания/рывков OpenCode в Termux Job’s. Сначала только
диагностика и минимальный план проверки. Не применять патчи и не собирать APK
до отдельного поручения пользователя. Не считать предположение установленной причиной.

## Среда и границы
Оригинал: /root/termux-fork, Ubuntu/proot на Android. Прочитать AGENTS.md.
Разрешено читать необходимые исходники и документацию оригинала. Диагностические
черновики/копии/результаты размещать в отдельном каталоге внутри .verification/ или
.tools/ проекта, не заменяя существующую рабочую сборочную копию .verification/arm64.
Не изменять оригинальные исходники, установленный Termux, Ubuntu, SDK, системные
пакеты, .codex, авторизацию, Bridge и другие проекты. Не останавливать процессы,
терминалы или текущую сессию Codex. Не запускать параллельные сборки в общей копии.
Не устанавливать APK и не публиковать GitHub. Не печатать секреты. Рабочее дерево
содержит существенные незакоммиченные изменения — нельзя reset/clean/checkout поверх них.

## Симптомы, подтверждённые пользователем
- После возвращения из другой сессии в OpenCode экран дёргается/мерцает.
- Также при клавиатуре и изменении размера терминального окна.
- Примерно через секунду отображение будто возвращается в исходное состояние.
- Есть в новом чате без истории, при индикаторе и выводе ответа.
- Переоткрытие при смене модели с сохранением текущего чата работает: не ломать.
- Возврат просмотра вниз при новых сообщениях пользователь просил оставить.
- v1.0.10 не устранила проблему на телефоне. Работа ранее отложена как OC-001.

Запись: /sdcard/Pictures/Screenshots/Screenrecorder-20261014-014531.mp4.
Это запись до пробной v1.0.10, а не доказательство поведения новых буферов.
Доступен локальный декодер в /root/termux-fork/.tools/video-inspection/python:
PYTHONPATH можно задавать только для отдельной команды. Уже извлечённые кадры и
contact.png лежат в .tools/video-inspection. Не предполагать отсутствия доступа
к телефону: проверить read-only, если путь изменился.

## Текущая реализация
Локальная v1.0.10/code1047, public GitHub release остаётся v1.0.4.
OpenCode mini v2.0.28, официальный native replay сохранён. Все assets/native libs
APK v1.0.10 совпадают с v1.0.9. CLI/bridge не переписаны ради мерцания.

В v1.0.10 добавлено ONLY OpenCode:
- SynchronizedOutputBuffer группирует байты между DEC2026 begin/end; сохраняет
  исходные байты, ограничивает frame примерно1MiB. Missing end/prefix flush1000ms.
- TerminalSession resize batching: earliest draw350ms, quiet100ms, hard limit1200ms,
  bounded RAM; одинаковый PTY размер/метрики повторно не устанавливаются.
- Opt-in задаётся WorkspaceController при create/refresh по aiAgent=opencode.
- Codex/default непосредственный путь вывода; TerminalView/autoscroll не изменены.

277 тестов и сборка прошли, но НЕ подтвердили устранение графического дефекта на
телефоне. Native PTY probe проверял историю/модель, emulator parsing и resize,
а не реальные Android кадры, IME, измерения layout и gestures.

Отвергнутый прототип --no-replay + ручной вывод истории перед mini не входит в APK:
мини-интерфейс затирал такой вывод при запуске. Не повторять без проверки результата
на настоящем emulator, а не только наличия текста в захваченном stdout.

## Патч-кандидат другого агента — НЕ ПРИМЕНЁН
/path/to/review/.verification/opencode-patch/opencode-resize-batching-disable.patch

Разрешено читать этот конкретный файл, не остальной чужой проект.
Патч добавляет mResizeBatchingEnabled=true и setter; только OpenCode-пути вызывают
setResizeBatchingEnabled(false). Он убирает resize batching, оставляя DEC2026 и
его1000ms timeout. git apply --check на текущем оригинале прошёл; патч не применён,
не собран и не проверен на телефоне. Это гипотеза для сравнения, не доказанное решение.
Мерцание было ДО добавления буферов: нельзя объяснить весь исходный дефект только ими.

## Прочитать
AGENTS.md, CONTINUE.md, docs/fork/KNOWN-ISSUES.md,
docs/fork/VALIDATION-v1.0.9.md, docs/fork/VALIDATION-v1.0.10.md,
docs/fork/AI-AGENTS.md.

Ключевые исходники:
- app/src/main/java/com/termux/app/workspace/WorkspaceController.java
  renderTabs/renderCodexStatus/showSessionStack/refresh/create.
- app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionClient.java
  setCurrentSession/onTextChanged.
- terminal-view/src/main/java/com/termux/view/TerminalView.java
  attachSession/updateSize/onSizeChanged/onScreenUpdated.
- terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java
  updateSize, appendProcessOutput, appendVisibleOutput, frame/resize timers.
- terminal-emulator/src/main/java/com/termux/terminal/SynchronizedOutputBuffer.java
- app/src/main/assets/ai-agents-control.js — owned mini/server lifecycle.
- .tools/ai-agents/model-control/upstream/runtime.ts — upstream v2.0.28 copy:
  RESIZE_DELAY=250, onResize/refreshTheme/replayOnResize/resetForReplay.

## Что выяснить
1. Фактическая цепочка: переключение/layout/карточка/IME → изменение PTY → SIGWINCH
   → native reset/replay → обработка вывода → Android draw. Где возникают повторные
   размеры, повторный replay или запоздалый показ старого содержания?
2. Чем объясняется новый пустой чат без истории? Отделить redraw индикатора,
   DEC2026, frame timeout от восстановления истории.
3. Кто инициирует возврат через ~1s? Сопоставить время с1000/1200ms, но учесть,
   что исходная запись предшествует введению этих таймеров.
4. Не скрывается ли часть проблемы внутри самого OpenCode/OpenTUI? Сравнить с
   обычным терминалом при том же CLI/геометрии, если понадобится последующий
   согласованный тест; сейчас не запускать и не менять пользовательские сессии.

## Требуемый отчёт
- Подтверждённые факты отдельно от гипотез, ссылки на файлы/участки кода.
- Причинная последовательность событий; какие данные ещё нужны с телефона.
- Оценка конкретного патча: что он устраняет/не устраняет, возможные регрессии.
- Самая маленькая проверка, позволяющая различить ведущие гипотезы.
- Минимальное исправление ONLY OpenCode, сохраняющее историю, смену модели,
  автопрокрутку и поведение остальных терминалов. Не менять код до согласования.
Не объявлять баг исправленным без проверки на телефоне.
