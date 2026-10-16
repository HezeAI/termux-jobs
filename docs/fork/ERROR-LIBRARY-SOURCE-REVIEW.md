# Сверка распространённых проблем и Reddit — 2026-10-16

Редакционная выборка, не статистический рейтинг частоты по всему Reddit.
Просмотрены повторяющиеся вопросы r/termux, тематические обсуждения SSH,
теги Question/Help и формулировки ошибок в поиске. Метки и голоса не считаются
официальным принятием ответа. Сильнее свидетельство — ответ автора о результате,
но оно относится только к описанному случаю. Числа голосов меняются и не зашиты
в приложение. Рекомендации дополнительно сверены с первичными источниками.

## Уже покрыто в 70 карточках

Ошибка репозитория/Release file, пакет не найден, apt/dpkg lock и прерванный dpkg,
404/зависимости APT, подписи pacman/zypper, noexec общей памяти и Android-доступ,
glibc против Bionic/musl, systemd и отсутствие настоящего root в proot,
SSH publickey/host key/refused/timeout, TLS/время, Git-конфликты и блокировки,
Python venv/externally-managed, npm EACCES/ERESOLVE, Gradle/JDK/SDK,
Cargo/linker/target, CMakeCache, заполненный диск и read-only FS.
Повторные карточки с тем же исправлением не добавлены ради количества.

## Добавлено: 8 карточек, всего 78

| Карточка | Свидетельство из обсуждения | Первичный источник / граница |
|---|---|---|
| termux-signal-nine | [Ветка с отметкой SOLVED автора](https://www.reddit.com/r/termux/comments/1msr3ew/): ограничения Android | [Termux README](https://github.com/termux/termux-app): SIGKILL также бывает из-за OOM/ручной остановки, причина не определяется по одному номеру сигнала |
| termux-api-companion | [Автор подтвердил работу после установки CLI-пакета](https://www.reddit.com/r/termux/comments/1vnh4qy/termuxapi_not_working_properly/) | [Termux:API](https://github.com/termux/termux-api#installation): нужен пакет и совместимое дополнение; у нашего сертификата нет автоматической совместимости с официальным плагином |
| shell-command-path | [Обсуждение PATH с ответами автора](https://www.reddit.com/r/termux/comments/pyesqt/) | [Bash Command Search](https://www.gnu.org/software/bash/manual/html_node/Command-Search-and-Execution.html); не скопированы ошибочные примеры с пробелами вокруг = и неподходящим /bin/bash |
| shell-unclosed-quote | [В той же ветке замечена лишняя кавычка и объяснён Ctrl+C](https://www.reddit.com/r/termux/comments/pyesqt/) | [Bash Quoting](https://www.gnu.org/software/bash/manual/html_node/Quoting.html); отмена только ожидающего ввода, не работающей задачи |
| termux-x11-window-manager | [Автор дважды подтвердил, что помог оконный менеджер](https://www.reddit.com/r/termux/comments/1wjh3q5/please_help_termuxx11/) | [Termux:X11](https://github.com/termux/termux-x11#running-graphical-applications); Android fullscreen и X11 maximize различаются |
| ssh-tmux-stale-display | [Автор поблагодарил за решение с обновлением окружения](https://www.reddit.com/r/ssh/comments/1aurs0x/) | [tmux manual](https://man.openbsd.org/tmux): окружение pane не равно автоматически обновлённому окружению tmux; слепой eval не предлагается |
| termux-pip-upgrade | Повторяющиеся [issues](https://github.com/termux/termux-packages/issues/15010), без утверждения о Reddit-подтверждении | [Защитный патч pip](https://github.com/termux/termux-packages/blob/master/packages/python-pip/install_py_preventing_pip_from_installing.patch): обновление системного pip менеджером Termux |
| proot-x11-display | Частая тема обсуждений X11; универсальное Reddit-исправление не заявляется | [Официальная инструкция](https://github.com/termux/termux-x11#using-with-proot-environment): DISPLAY и общий tmp; зависит от версии proot-distro |

GNU-страницы через web-инструмент дали timeout; полный официальный Bash manual
прочитан через HTTPS с сохранением локальной копии в .verification/. Непрочитанные
страницы не выдаются за проверенные.

## Что отсеяно

- [Чёрный экран XFCE](https://www.reddit.com/r/termux/comments/1pp7lic/):
  неоднозначные результаты, противоречивые комментарии про TCP и переустановку.
  Не превращено в гарантированный совет переустановить дистрибутив.
- [APT DNS в proot](https://www.reddit.com/r/termux/comments/1inqm1a/):
  нет подтверждённого исправления; перезапись resolv.conf не добавлена наугад.
- [Зависание дистрибутива](https://www.reddit.com/r/termux/comments/1whkx2j/distribution_stuck/):
  нет основания рекомендовать смену ядра или выдавать зависание за phantom kill.
- Советы выключить TLS/X-аутентификацию/SELinux, рекурсивно менять права или
  удалить окружение не перенесены как общие решения.

Это проверка текста и источников. Все описанные ошибки на всех ОС/версиях
не воспроизводились. Источники доступны из каждой карточки, рекомендации не
выполняются автоматически, новые сетевые запросы или сбор терминального вывода
не добавлены. Reddit не опрашивается приложением в фоне.
