# Проверки v1.7.85-x11-test / code1300

Частная сборка исправления запуска Arch XFCE. Публикации GitHub нет.

## Причина из отчёта и скриншотов

Разработчик подтвердил успешную установку Arch X11. Отчёт
Termux-Jobs-diagnostics-7945539684196378061: componentresultsuccess,
componentchildexit0, rendererConnected/hostAttachedtrue, Turnip-Zink,
композиторoff. XFCE завершился134 примерно через26с; guardian137 относится
к завершению принадлежащей запуску группы и сам по себе не причина.

Скриншоты16:17:10/16:17:13 показывают Glycin SVG loader, bwrap status1,
ошибку загрузки image-missing.svg, GTK ensure_surface_for_gicon и
Wnck default_icon_at_size assertions; xfce4-session Aborted. Это соответствует
отказу image-decoder sandbox внутри PRoot. Последующий ICE disconnect
не трактуется как первоначальная ошибка авторизации. GPU уже подключён;
отчёт не подтверждает сбой Turnip.

## Исправление

К точному targetarchlinux добавлен использующийся в Fedora приватный модуль
Glycin2. Файл x11-fedora-glycin.so копируется в контрольный каталог этого
окружения и preloads перед его dbus-run-session, затем в дереве XFCE.
Ни системные библиотеки, ни repo-конфиги/ярлыки не заменяются.
Сама бинарная реализация модуля не менялась. Fedora сохраняет прежний путь;
другие targets модуль не получают.

Модуль выбирает NotSandboxed для loader и creator Glycin2. Декодирование
изображений в этой сессии проходит без несовместимого bubblewrap:
это уменьшает изоляцию декодеров изображений, сохраняя ограничения Android
и выбранного PRoot rootfs. Это не добавляет Android root/права приложению
и не меняет настройки Firefox/агентов. Альтернативы sandbox среды здесь
не реализованы. Работа с недоверенными файлами остаётся риском такого режима.

В диагностике добавлены GLYCIN_IMAGE_SANDBOX_FAILED и
DESKTOP_ICON_LOAD_FAILED, без raw-путей. Предупреждение Failed to fetch
_NET_CURRENT_DESKTOP больше не помечается как PACKAGE_DOWNLOAD_FAILED:
fetch в данном случае относится к свойству X11, а не скачиванию пакета.

## Проверка ABI и источники

Readelf реальных libglycin-2.so.0 и GdkPixbuf2.44.8 из установленного Arch:
Glycin экспортирует gly_loader_new/gly_creator_new и оба set_sandbox_selector;
GdkPixbuf обращается к этим функциям. В установленном header есть
GLY_SANDBOX_SELECTOR_NOT_SANDBOXED. Это read-only инспекция; библиотечные
файлы не изменялись. Свидетельство .verification/arch-glycin/abi.json.

Upstream io-glycin-utils.c2.44.8 проверен: после конструктора он не сбрасывает
selector обратно в Auto; отдельные image utilities также выбирают NotSandboxed.
Источники:
- https://github.com/GNOME/gdk-pixbuf/blob/2.44.8/gdk-pixbuf/io-glycin-utils.c
- https://archlinuxarm.org/packages/aarch64/glycin
- https://gnome.pages.gitlab.gnome.org/glycin/libglycin/method.Loader.set_sandbox_selector.html

## Автоматические проверки и артефакт

32 Python-теста основного набора пройдены: Glycin, Arch, clean-matrix,
native и keyboard. Дополнительно1 новый scope-тест (при повторе3 Glycin-тестов)
пройден: исполняется реальный shell-блок с10 targets; preload только в
Fedora/Arch, отсутствие asset отклоняет запуск. C-фикстуры проверяют loader
и creator selector3 с RTLD_LOCAL/RTLD_GLOBAL и отсутствие filesystem override.
Всего33 разных Python-теста.

47 JVM-тестов пройдены:12 StartupLog,3 DesktopProcessAsset,32 Integration.
Проверены категории/обезличивание, отсутствие ложной network-категории,
ARM64 asset, отказ извлечению по ссылке, приватные процессы, разделение
rootfs, stop/cancel/владельцы и отказ чужим запросам. Всего80 проверок.
Это не GUI-тест на Android и не полная проверка Glycin-декодирования в PRoot.

Release сборка успешна55с. DEX13502 определения/31 явно рассмотренный
неразрешённый тип. ZIP/package/minSDK26/arm64/code1300/cert/v2/SHA256 проверены.
JNI34 дескриптора сверены; все native assets и библиотеки идентичны v1.7.84.
Изменены только2 ожидаемых shell-assets, Java сверены со staging.
Поиск известных ключевых/авторизационных маркеров пройден; он не даёт
исчерпывающей гарантии отсутствия любого секрета. bash/sh-n, diff-check пройдены.

Свидетельства .verification/arch-glycin/{abi.json,python.log,scope.log,jvm.log,
jvm-results.json,build.log,audit.log,apk-audit.json}.
Пакет com.termux/сертификат сохранены; code1300 выше1299. Форматы данных
не меняются, автоматических сбросов/миграций нет. Установленные rootfs,
рабочие сессии, системные пакеты и установленный APK при разработке не менялись.

## Следующий телефонный тест

Обновить APK и повторить запуск Arch с прежним рендером. Проверить появление
панели/значков, меню, открытие картинки и последующее закрытие сессии.
При отказе сохранить Диагностика сессии→X11. GUI/стабильность/скорость этой
сборки на телефоне пока не подтверждены; GLX/glmark2 и VirGL Arch отдельно
ждут проверки разработчиком.

APK `Termux-Jobs-v1.7.85-x11-test-arm64.apk` —39522198байт.
SHA256 `5c5de7e74bc9fdf9523d2f1564c1cbc05058b022eae65698862d4b856df12539`.
Сертификат SHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
