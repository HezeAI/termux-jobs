# XDA и первоисточники — дополнение v1.3.2

Исследование2026-10-16. Прямые страницы XDA частично возвращали403;
использованы доступные индексированные обсуждения и первоисточники.
Полный обход форума не выполнялся. Советы не проверены на всех устройствах.

| Карточка | Основание | Ограничение |
|---|---|---|
| termux-apt-nosplit | [XDA](https://xdaforums.com/t/termux.4758702/), [Termux issue8138](https://github.com/termux/termux-packages/issues/8138) | Зеркало помогло части пользователей; не универсальное исправление кастомных ROM |
| termux-netlink-denied | [Termux issue2993](https://github.com/termux/termux-app/issues/2993), [packages12758](https://github.com/termux/termux-packages/issues/12758) | Дополнительная находка из первоисточников, не отдельный доказанный случай XDA |
| termux-adb-paired-offline | [XDA](https://xdaforums.com/t/adb-what-is-the-adb-syntax-to-connect-wirelessly-to-android-by-unique-serial-number-instead-of-by-wi-fi-lan-ip-address-random-port-assignment.4476319/), [Android](https://developer.android.com/tools/adb) | Версии Android/ADB различаются; сопряжение и соединение различаются |

Уточнены существующие termux-signal-nine и proot-root: причина SIGKILL не
определяется одной строкой, а OCI-образы не предоставляют Docker/KVM/права ядра.
[Termux](https://github.com/termux/termux-app/discussions/3387),
[proot-distro](https://github.com/termux/proot-distro#limitations).

Все три новые карточки двуязычные, command пустой. База126→129.
Предложение синхронизации rsync отложено, функциональные зависимости не добавлены.
