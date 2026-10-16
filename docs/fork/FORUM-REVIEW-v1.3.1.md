# Сверка форумных материалов v1.3.1

Справочник вырос со114 до126 карточек:12 новых,8 дополнений без новых дублей.

Доступ к4PDA возвращал403, включая страницы2/3/400 и форму входа. Поисковый
индекс позволил прочитать лишь часть обсуждений. Полный обход темы и всех ссылок
шапки не заявлен. Найденные заголовки сами по себе не являются проверкой цели.

Технические основания сверены с документацией и исходниками авторов проектов.
Это не выполнение каждого совета на каждом устройстве. Плагины форка требуют
совместимого сертификата; установщики GUI и команды с форума не запускались.
Карточки содержат текстовые инструкции, новые12 не имеют исполняемого блока.

| Тема / карточка | Отбор | Первоисточник |
| --- | --- | --- |
| Не срабатывает автозапуск после перезагрузки | новая | [Источник](https://github.com/termux/termux-boot#how-to-use) |
| Служба установлена, но сама не запускается | новая | [Источник](https://github.com/termux/termux-services) |
| Подключение к самому Termux по SSH | новая | [Источник](https://github.com/termux/termux-packages/discussions/18318) |
| Старый sshd требует абсолютный путь | новая | [Источник](https://github.com/openssh/openssh-portable/blob/V_8_4_P1/sshd.c) |
| source: not found в скрипте автозапуска | новая | [Источник](https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html#tag_18_18) |
| rish не связывается с терминальным приложением | новая | [Источник](https://github.com/RikkaApps/Shizuku/blob/master/manager/src/main/assets/rish) |
| Android14+: rish отказывается загружать dex | новая | [Источник](https://github.com/RikkaApps/Shizuku/issues/371) |
| После rish пропали команды и PATH Termux | новая | [Источник](https://github.com/RikkaApps/Shizuku-API/blob/master/rish/README.md) |
| Termux:X11: чёрный экран с курсором | новая | [Источник](https://github.com/termux/termux-x11#running-graphical-applications) |
| Termux:X11: перепутаны цвета | новая | [Источник](https://github.com/termux/termux-x11#running-graphical-applications) |
| Имена файлов в ZIP из Windows выглядят неверно | новая | [Источник](https://github.com/libarchive/libarchive/blob/master/libarchive/archive_read_set_options.3) |
| Samba завершается или циклически перезапускается под supervisor | новая | [Источник](https://www.samba.org/samba/docs/current/man-html/smbd.8.html) |
| termux-storage | дополнение | [Источник](https://github.com/termux/termux-packages/wiki/Termux-execution-environment) |
| termux-shared-exec | дополнение | [Источник](https://github.com/termux/termux-packages/wiki/Termux-execution-environment) |
| termux-linker | дополнение | [Источник](https://github.com/termux/termux-packages/wiki/Termux-execution-environment) |
| proot-root | дополнение | [Источник](https://github.com/termux/proot-distro) |
| proot-version | дополнение | [Источник](https://github.com/termux/proot-distro) |
| proot-x11-display | дополнение | [Источник](https://github.com/termux/termux-x11#running-graphical-applications) |
| jobs-backup-source-unreadable | дополнение | [Источник](https://www.gnu.org/software/tar/manual/html_node/Ignore-Failed-Read.html) |
| jobs-apk-update-identity | дополнение | [Источник](https://github.com/termux/termux-boot#how-to-use) |

Не включены как готовые решения: произвольная подмена tar, отключение SELinux,
перезапись /system, гостевой SMB без пароля и снятие проверки dex из rish.
PulseAudio, специфичные драйверы, ALT-установщики и клавиатуры требуют отдельной
проверки. Копия FAQ не подменяет условия применимости.

Открытая проблема OC001 остаётся открытой. Отметка AI — редакционная подпись,
а не фоновый агент, гарантия правильности или новая сетевая функция.
