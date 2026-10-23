# Проверка v1.7.87-x11-test / code1302

Частная сборка: Wrapper только в native Termux. Это не публикация GitHub.
Рабочая v1.7.86/code1301 сохранена в build/baselines/v1.7.86 без повышения версии;
разработчик подтвердил исправление зависания Thunar Arch после этой сборки.

## Изменения и границы

Четвёртый выбор Wrapper · Bionic (тест) есть только у Termux. Выбор возвращает
в меню рабочего стола; папка справа открывает наш менеджер в /sdcard, ограниченный
этой памятью, после стандартного запроса Android-доступа. Импортируются ZIP с
meta.json/driver.json и ARM64 shared libraries; до64МБ архива/192МБ распаковки,
до16 библиотек, без обхода путей/ссылок/исполнения установочных скриптов.
Перед импортом предупреждается о выполнении доверенного native-кода с правами
приложения. Файл копируется в приватный каталог; проверка формата/хеша не даёт
гарантии безопасности, ABI или поддержки GPU.

Импорт атомарно публикует current.json после проверки библиотек. Ошибка/ранняя
отмена сохраняют прежний выбор; успешная замена удаляет только старые управляемые
копии драйверов. При поздней отмене уже завершённая публикация может сохраниться.
Окно прогресса нельзя закрыть тапом снаружи. Есть файловая блокировка импорта:
запущенный Termux desktop удерживает shared flock; import/setup требуют exclusive
nonblocking lock. Поэтому замена не удалит используемые им файлы даже при гонке
между UI и службой. Остальные процессы/сессии не останавливаются.

Путь: Termux Mesa/Zink -> Bionic Wrapper -> AdrenoTools -> выбранный Turnip.
Опциональные готовые Wrapper0.0.5r5 и generic Vulkan Loader1.4.365 скачиваются
после подтверждения (~3.43МБ), закреплены размером/SHA256 и хранятся отдельно.
Главная библиотека в Termux-упаковке sabamdarif совпадает с digest релиза Lee Gao.
Воспроизводимая самостоятельная сборка upstream binary не заявляется.
Источники/кредиты: third-party/bionic-wrapper/provenance.json, X11-GPU.md,
assets/licenses/Mesa-GPU-NOTICE.txt.

Выбранный mode и драйвер меняются только при остановленном столе. Рендерерные
переменные получает дочернее дерево Termux XFCE; renderer Wrapper использует
compositor=off. Обычные терминалы/агенты не получают эти переменные. Для Wrapper
явно запрашиваются mesa, libandroid-shmem, libc++, libdrm, libx11, libxcb,
libxshmfence, libwayland, zlib, zstd, util-linux; только при согласованной установке
стола Termux. Проверка не считает отсутствующие зависимости готовыми.

Ubuntu/Debian/Alpine/Fedora/openSUSE/Void/Arch сохраняют прежние три выбора.
Java service, DesktopProcess и shell отвергают Wrapper для guest до выполнения
его команд. Venus/Wine/VM/удалённый сервер не добавлены. Системные драйверы/Mesa
не заменяются. Старый Turnip+Zink и его driver cache сохранены.

## Автоматические проверки

- 46 Python-тестов: новый import/runtime, успешная замена, ошибочный/неподходящий
  ZIP, minApi, пути/ссылки/дубликаты, отмена, проверка digest, повреждение runtime,
  кэш без повторной загрузки, блокировка работающего стола, Termux-only маршруты,
  scoped environment и отказ без перехода на CPU; существующие GPU/VirGL,
  clean-install matrix, owner root repair Arch и session policy.
- 74 JVM-теста:5 новых Wrapper,9 GPU,22 подготовки,23 UI,2 desktop,13 startup log.
  Выбор/возврат в меню, папка/недоступность при работе, гостевые отказы,
  сохранение настроек остальных целей, отсутствие инсталлятора без ZIP,
  ограниченная диагностика идентификатора драйвера и классов ошибок.
- Настоящие Wrapper ZIP и Loader DEB распакованы в изолированную папку проекта,
  проверены hashes/ready и повторное использование. ELF dependencies прочитаны;
  библиотеки/GPU при этих проверках не запускались.
- Turnip_Gen8_V38.zip разработчика: ZIP целый; meta.json, ARM64 ET_DYN, minApi28,
  имя Turnip Gen8 V38. Пробный импорт/проверка копии выполнены в project fixture;
  исходный ZIP и установленное окружение не менялись.
- Release APK собран успешно; required DEX definitions прошли проверку:
  13525 определений,31 оставшийся тип с существующими review notes.

## Финальный APK

- Файл: `Termux-Jobs-v1.7.87-x11-test-arm64.apk`; 39538905 байт.
- Package: `com.termux`; versionName `1.7.87-x11-test`; versionCode1302; minSDK26; ARM64.
- SHA256: `2fdef57270cabe0eae11d6a6601b8c7298227800601ee68174ffbd8c5774abf0`.
- Сертификат: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`; APK v2 подтверждён.
- ZIP/уникальность записей/package/version/ABI/подпись/JNI descriptors проверены.
- Native libraries совпадают с v1.7.86. Единственный новый asset: x11-wrapper.py.
  Изменены x11-desktop.sh, x11-native-desktop.sh, x11-session.sh, Mesa-GPU-NOTICE.
- Личный ZIP/импортированный драйвер/токены/ключи в APK не включены;
  сохранённый baseline APK повторно проверен по SHA256.

## Совместимость данных и оставшиеся проверки

Пакет и сертификат сохранены, versionCode повышен с1301 до1302. Новое состояние
добавляется в workspace-x11-wrapper и renderer:termux, очистка/миграция пользовательских
данных не выполняется. Старые рендеры других окружений сохраняются. При возврате
к v1.7.86 с позднее повышенным versionCode неизвестный mode wrapper читается как CPU;
новый приватный cache не удаляется автоматически. Полный сценарий отката требует
отдельной проверки на устройстве.

Новый Wrapper ещё не подтверждён на телефоне: проверить реальное подключение
AdrenoTools/выбранного Bionic Turnip, glxinfo -B/GLX/glmark2 с окном, XFCE,
возврат во вкладку, остановку, повторный запуск и возврат к CPU/Turnip+Zink.
Upstream AdrenoTools может не выполнить подмену на конкретной прошивке;
проверять фактический renderer, не считать один импорт доказательством ускорения.
Обязательна отдельная проверка чистого bootstrap/Termux с установкой всех зависимостей.
Fixture и JVM-тесты не доказывают работу Android storage permissions, GUI/клавиатуры
или GPU на устройстве. Ни поддержка всех Adreno8xx, ни повышение скорости не заявлены.

Логи: .verification/termux-wrapper/{python-all.log,jvm.log,jvm-results.json,
user-driver-check.json,runtime-fixture-check.json,build.log,audit.log,apk-audit.json}.
