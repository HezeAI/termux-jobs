# Проверка v1.7.90-x11-test / code1305

Частная сборка: необязательные GPU-инструменты и два ярлыка для каждого X11.

## Поведение

Программы → Рекомендуемые → GPU & GPU INFO → Установить. После согласия
устанавливаются пакеты своей системы и создаются английские ярлыки GPU и
GPU INFO. Установка использует прежнюю плашку/фон/отмену/продолжение; этапы,
кнопки и прогресс локализованы, вывод PM остаётся исходным. Базовый установщик
XFCE не получает эти необязательные приложения. При наличии инструментов,
установленных ранее вручную, следующий старт стола также создаёт ярлыки.

GPU INFO — glxinfo -B; GPU — glmark2, кроме openSUSE (glmark2-es2/EGL) и
Alpine stable (glxgears -info, с явным пояснением, что это не балл glmark2).
Wayland/DRM-программы не используются. Подробная таблица пакетов — X11-GPU.md.
В Alpine не подключаются edge/testing. Результаты разных API, размеров окна,
рендеров и устройств не объявляются сопоставимыми. GPU ярлык не меняет рендер:
на CPU тестирует программный Mesa, выбранный в сессии. Успешный launcher сам
по себе не доказывает аппаратное ускорение.

Exec содержит массив аргументов для xfce4-terminal --disable-server --hold,
без shell/eval пользовательских путей. Сохраняются DISPLAY/XAUTHORITY и
библиотеки выбранного рендера. Результат остаётся в терминальном окне после
завершения; при отсутствии команды/дисплея есть понятная подсказка.

XDG_DESKTOP_DIR читается как ограниченные данные внутри выбранного HOME,
по умолчанию ~/Desktop. Одноимённые пользовательские файлы/правки сохраняются;
коллизия может остановить создание ярлыков с ошибкой. Управляемые файлы и их
SHA256 — ~/.local/share/termux-jobs/gpu-tools/{gpu-tools.py,shortcuts.json};
ярлыки — termux-jobs-gpu.desktop и termux-jobs-gpu-info.desktop. Повторный старт
не переписывает неизменённые файлы. Символические ссылки/пути вне HOME/FIFO и
слишком большие файлы не принимаются. Рендеры/остановка процессов не меняются.
Агенты/Ubuntu AI не получают эту функцию.

После удаления самой программы ярлык может остаться и предлагает установить
GPU & GPU INFO. Автоматического удаления пользовательских ярлыков нет. Старый
кэш рекомендуемых без нового gpu-tools не принимается и перепроверяется;
пакетные каталоги/остальные данные не сбрасываются. Журнал ошибок v1.7.89 сохранён.
Пакет/сертификат прежние, code1304→1305. GitHub не публикуется.

## Проверки

- 66 Python-тестов: 12 новых GPU-tools (матрица шести native менеджеров,
  отдельные HOME, команды всех target, missing-tool/display, неизменность
  renderer environment, XDG/escaping, ссылки/правки, повтор без перезаписи),
  routing/install APT для Ubuntu/Debian, готовность по бинарникам; регрессии
  программ/докачки/границ окружений/bookmarks. APK/APT/DNF/Zypper/XBPS/Pacman
  проверялись изолированными fixture, без установки в телефонные окружения.
- 55 JVM-тестов: WorkspaceDesktopPrograms19, X11ProgramOutput3,
  DesktopCatalogCache5, DesktopCatalog6, WorkspaceDesktopPreparation22.
  Английское имя, локализованное описание, удаляемый главный пакет,
  recommended/status/progress/parser, отклонение прежнего кэша и подготовка.
- Логическая инспекция: согласие → свои пакеты → проверка бинарников → ярлыки;
  уже установленные инструменты → создание при следующем старте; повтор без
  inode churn; сохранение renderer environment/вывода; нет установки при обычном
  старте/проверке. Остановка и процессы остались в существующем механизме.

Не проверено на устройстве: фактическая установка новой группы во всех восьми
системах, доверие XFCE к новым desktop entry, запуск через ярлыки с выбранными
драйверами, живой GPU score. Минимальный следующий тест: установить группу в
Termux, открыть GPU INFO, затем GPU, проверить результат и сохранность после
перезапуска стола. Позже повторить остальные окружения; Alpine не сравнивать
с glmark2. Установка на настроенном телефоне не заменяет проверку чистого
окружения.

## Источники пакетной матрицы

- Ubuntu: https://packages.ubuntu.com/resolute/glmark2-x11
- Debian: https://packages.debian.org/trixie/glmark2-x11
- Fedora glxinfo: https://packages.fedoraproject.org/pkgs/mesa-demos/glx-utils/
- Termux: https://github.com/termux/termux-packages/blob/master/x11-packages/glmark2/build.sh
- Void: https://github.com/void-linux/void-packages/blob/master/srcpkgs/glmark2/template
  и https://github.com/void-linux/void-packages/blob/master/srcpkgs/mesa-demos/template
  (glxinfo — отдельный subpackage).
- Alpine: ранее сохранённый официальный community APKINDEX v3.24/aarch64
  содержит mesa-utils9.0.0-r6 с cmd:glxinfo/glxgears/eglinfo; стабильный glmark2
  отсутствует. https://pkgs.alpinelinux.org/package/edge/testing/aarch64/glmark2
  показывает его в testing, который здесь не добавляется.
- Arch/glmark2 и openSUSE/glmark2/Mesa-demo-x: сохранённые ранее проверки
  каталогов/пакетов и результаты разработчика, перечисленные в X11-GPU.md.
  В openSUSE EGL-вариант ранее проверен на устройстве; запуск новой ссылки
  ещё не проверен. Новая реальная установка пакетов этим отчётом не заявляется.

## APK

- `Termux-Jobs-v1.7.90-x11-test-arm64.apk`, 39549582 байт.
- `com.termux`, ARM64, minSDK26, v1.7.90-x11-test/code1305.
- SHA256: `9dd094a1e496ee4e3ae1846b10d6cc30e2fd832d5951ad588a19fdc2c1721c3f`.
- Сертификат: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`; APK v2 проверен.
- ZIP, подпись, package/version/ABI/minSDK/debug flag/JNI проверены.
- 13547 DEX definitions,31 прежний reviewed unresolved type.
- Native-библиотеки идентичны v1.7.89. Новый asset — x11-gpu-tools.py,
  изменены только x11-native-programs.py, x11-programs.sh и x11-session.sh.
  Проверено отсутствие личных токенов/ключей/архивов драйверов и совпадение
  изменённых staged sources с исходниками.

Логи: .verification/gpu-shortcuts/{python.log,jvm.log,jvm-results.json,
build.log,audit.log,apk-audit.json}. Макеты оформления — отдельные концепты;
они не меняют XFCE или тему установленного приложения.
