# Происхождение и лицензии

Termux Job’s — неофициальный форк Termux, поддерживаемый HezeAI.
Основа: [termux/termux-app](https://github.com/termux/termux-app),
[v0.118.3](https://github.com/termux/termux-app/tree/v0.118.3),
commit 5b657c6adf4304e5198951ce815fe0205dcac29c.
Оригинальная история, заголовки copyright и уведомления сохранены.

Основная лицензия: **GPL-3.0-only**, см. [LICENSE.md](../../LICENSE.md)
и полный текст [COPYING](../../COPYING).
Terminal Emulator for Android и части библиотек имеют отдельные лицензии,
включая Apache-2.0; сохранены исходные уведомления и
[termux-shared/LICENSE.md](../../termux-shared/LICENSE.md).
Лицензии зависимостей не заменяются основной лицензией форка.

Изменения HezeAI: интерфейс рабочего пространства, библиотека команд,
журнал, сохранённые профили и последовательности запуска, управление
терминалами и настройки ввода. Описание и даты разработки:
[FORK.md](../../FORK.md), [VALIDATION.md](VALIDATION.md) и Git history.

Bootstrap: неизменённый официальный архив Termux
[bootstrap-2025.03.28-r1+apt-android-7](https://github.com/termux/termux-packages/releases/tag/bootstrap-2025.03.28-r1%2Bapt-android-7).
Пакеты и рецепты сборки: [termux/termux-packages](https://github.com/termux/termux-packages),
исходники пакетов — через ссылки и версии в соответствующих recipes.
SHA-256 bootstrap проверяется scripts/build-local-arm64.sh.

Каждый опубликованный APK привязан к тегу исходников, включающему скрипты
сборки. Самостоятельная сборка использует собственный ключ; локальный ключ
подписи опубликованных APK и личные данные разработчика не распространяются.
