# Termux Job’s v1.2.1 — быстрее отправка файлов

- **Ускорена отправка на сервер по SFTP:** до8блоков в полёте через одно SSH-соединение вместо ожидания каждого отдельно.
- **Разделены очереди:** поиск/диагностика и мониторинг меньше задерживают обычные серверные действия. Устаревшие ответы не перезаписывают новые результаты.
- **Прежние защиты сохранены:** подтверждения записи, временный файл, проверка перед заменой, отмена и очистка.

Разработчик HezeAI проверил отправку на устройстве и отметил ускорение примерно
в **4–5 раз на своём подключении**. Результат зависит от задержки сети и сервера;
это не обещание такого выигрыша для всех. Скачивание использует прежний потоковый
транспорт и не получило отдельного ускорения.

**Обновление поверх без очистки данных:** прежние пакет/подпись, code1072 >1071.
Агент не изменён, переустановка не нужна. Обновление доступно через приложение.
Старый SSH-вход, профили, команды, пресеты, истории AI и окружения сохраняются.
Перед установкой завершите важные операции: Android может остановить процессы.

## English summary

Faster SFTP uploads using up to8in-flight blocks on a single SSH connection.
Separate bounded queues for monitoring and search/diagnostics keep ordinary
server actions responsive. Existing acknowledgement, replacement and cancellation
protections are retained. HezeAI observed approximately4–5x faster uploads on
his connection; gains depend on network latency and the server. Download transport
is unchanged. Same package/signer, code1072, no data reset or agent update required.
In-app updates supported. Finish important operations before updating.

## Проверки

388 Java-тестов прошли; ZIP, пакет/версия/ABI, подпись и SHA-256 финального APK
проверены. Разработчик проверил ускорение отправки на устройстве. Отмена при
реальных сетевых разрывах, нагрузка и расход батареи требуют отдельной проверки.

[Подробные проверки](https://github.com/HezeAI/termux-jobs/blob/v1.2.1/docs/fork/VALIDATION-v1.2.1.md) · [Серверы](https://github.com/HezeAI/termux-jobs/blob/v1.2.1/docs/fork/SERVERS.md).
