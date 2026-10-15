# Проверки v1.2.1

APK `Termux-Jobs-v1.2.1-arm64.apk`, 36479781 bytes. SHA256 `a546c80a8660b0585b964150c4f471280b1d19e7be4f93d074262eaa2801cbaa`.
com.termux/1.2.1/code1072 >1071, min24/target28, толькоARM64, non-debuggable.
Прежний certSHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`, apksigner APKv2 PASS.
ZIP CRC/no duplicate/traversal/stored4byte alignment, Java/resources/assets == stage,
native/agent identicalv1.2.0, ограниченный скан ключей/token/auth/keystore PASS.
Скан не является универсальной гарантией отсутствия секретов.

388 JVM tests,0 failures/errors/skipped. Новые тесты:
- заполнение/ограничение8WRITE и ответы в другом порядке, ack-based progress;
- ошибка записи с pending replies: drain/cleanup без публикации;
- неверный id: transport closed/cleanup path retained;
- обрыв источника с3pending WRITE и cancel сpending без rename;
- заблокированные helpers не задерживают foreground/monitor;
- ограничение очереди, отмена queued Future/освобождение capacity, shutdown;
- поздний monitor response после ручного действия/смены профиля игнорируется.
Существующие OpenSSH SFTP-tests по локальным pipes проверяют bytes/Unicode,
явную замену/отсутствие скрытой перезаписи, revision-conflict, zero-length,
cleanup/cancel. Payload расширен до19полных блоков+1003байта, несколько окон.
Существующие terminal/process/UI регрессионные проверки прошли.

Build successful. git diff --check PASS. Full clean Lint не заявляется из-за
прежних lintVital/JNA aarch64/Kotlin metadata/custom lint несовместимостей.

Ревью/инспекция: shared transport serial, monitor/helper transport private;
close/back/disconnect/profile edit/generation guards и manualEpoch;
helper cancellation removes queued work, shutdown cancels queued futures,
auxiliary closes own connections. Server-side task admission/cancel неизменны.
Проверены write ids/status/drain/error/cancel, handle-close/targetstat/rename,
external cleanup path. Протокольная ошибка закрывает канал без его повторного
использования. Новые материалы не содержат пользовательских секретов.

Benchmark before/after: по3прогона на10/50мс simulated RTT, итоговые CSV/исходник
в .verification/servers-v1.2.1/benchmark. Оригинальный ServerSftp взят из HEADv1.2.0;
новый компилировался из текущего исходника. Готовность ответа отсчитывается от
отправки запроса, поэтому одновременно ожидающие блоки моделируются корректно.
Диски/шифрование/сеть/Android отсутствуют. Холодные0ms прогоны не используются
для утверждений о скорости. Доказательстваjvm-final.log/build-final.log/artifact.json.

Сборка предназначена для проверки разработчиком на устройстве: поиск и короткие
действия/метрики, закрытие/повторное открытие поиска, смена сети/профиля,
отправка большого файла/замена/отмена/права/заполненный диск, неизменность скачивания.
Фактические ускорение, нагрузка, расход батареи и сетевые разрывы ещё не измерены.

Разработчик HezeAI проверил APK на устройстве и подтвердил ускорение отправки
примерно4–5раз на своём подключении. Это не измерение всех сетей/сценариев.
