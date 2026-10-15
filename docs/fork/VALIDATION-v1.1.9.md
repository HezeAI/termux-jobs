> Исторический отчёт локальной сборки. Текущий публичный выпуск: [v1.2.0](RELEASE-v1.2.0.md).

# Проверка локальной v1.1.9

Не опубликована, APK не установлен разработчиком. Работающий сервер/агент,
окружения, сессии и системные пакеты не изменялись.

- `Termux-Jobs-v1.1.9-arm64.apk`: 36445600 bytes; SHA-256 `a8a2431ff48ba7ce08ba9fd63d0711a5bf8c57a4798157149788e1387a5c85f4`.
- com.termux / versionName1.1.9 / code1066 >1065, ARM64, min24/target28,
  non-debuggable. Прежний сертификат SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
  APKv2 подпись проверена apksigner; пакет/версия/ABI проверены aapt2.
- ZIP CRC, duplicate/traversal entries, stored4byte alignment, совпадение
  финальных Java/resources/assets с build stage проверены. Agent/native byte-identical
  v1.1.8. Ограниченный secret-pattern/name scan не нашёл ключей/tokens/auth/keystore.
- 362 JVM tests, failures/errors/skipped=0 на финальном исходнике.
  5 ServerScrollViewTest: folder-like replacement/focus reveal, normal focus after
  update, natural short-content clamp, nested updates, user gesture overriding
  pending restore, explicit full render restore.
  4 ServerMetricCardsTest: actual percent/used-total, progress ratio/clamp,
  unavailable data shown as —, stable card/text instances, RU units, fit280dp
  and square vector icons. Обsolete graph-only test removed with MeterGraph.
  35 ServerWorkspaceTest: existing behavior plus stable section/monitor/scroll/tab
  objects while selecting all4 sections (queued fake worker: no SSH calls),
  vector-only terminal/square touch target/gap/click handler, shortened ! content
  and phone-profile explanation. Existing SFTP/editor/player isolation checks pass.
- Release build successful. Repeat packaging from cleared generated packaging
  state removed unused incremental ZIP gaps; source unchanged, final audit repeated.
  Старый lintVital по-прежнему несовместим с Linux/aarch64 JNA и современными
  Kotlin metadata/custom lint (Vendor class); полный чистый lint не заявляется.
- 581 existing localization strings/settings keys/format args PASS; новые RU/EN
  строки проверены в исходнике/целевых тестах; git diff --check PASS.
- update.json создан из финального APK, локально, не опубликован.

## Ревью и логическая инспекция

Section slots отделены от мониторинга и табов: их объекты не пересоздаются при
навигации; только explicit connect/profile actions оставляют полный render.
ServerScrollView ограничен экраном сервера. На mutation сохраняет y, блокирует
программный scroll/focus reveal до ближайшего layout; parent focus chain сохраняется.
В onLayout восстанавливает y с native clamp по новой высоте. Новый touch-down/
generic-motion отменяет отложенное восстановление; старого post callback, который
мог бы прокрутить новый ScrollView, нет. Обычные терминалы/другие file browsers
этот контейнер не используют. No new timers/network/process/permissions changes.

File rows и live task/service updates используют тот же guard; loading нового
асинхронного раздела временно сохраняет старую высоту, до его заполнения.
Короткий итоговый список может законно ограничить прежнюю позицию. Это не
обещание удерживать позицию за пределами реального контента.

ServerMetricCards не использует/не рисует history. CPU null/невалидные totals
не выдаются за нулевую нагрузку; полосы bounded0..1000, исходные usage цифры
не скрываются. Сбор agent30с и UI polling10/30с/onStop прежние.
Agent/history на сервере не удаляются. Действие icon-only terminal — прежний
openTerminal с проверкой pinning и существующим launch callback.

Профили/библиотекаv8/AI/пресеты и данные не мигрируют/не сбрасываются. Описание !
на RU/EN начинается установкой и не содержит старого intro/history metrics;
права/ограничения/удаление сохранены. Нижний текст различает локальный профиль
и удалённые данные. Авторизация/SSH ключи в артефакт не добавлены.

## На устройстве ещё проверить

1. Прокрутить сервер к вкладкам, открыть разные папки/назад/короткую папку;
   переключить Projects/Tasks/Files/Services, дождаться async result и polling.
   Нет нежелательного перехода к мониторингу; short-content clamp допустим.
2. Свайп во время обновления, обычная keyboard/accessibility navigation, поворот,
   большие шрифты/DPI; JVM layout не доказывает физические жесты/отрисовку.
3. Три карточки без графика, реальные usage цифры/цвета/обновление без моргания,
   вектор терминала и старое SSH-действие, оба объяснения RU/EN.
4. После ручного обновления проверить данные/терминалы/плеер/редактор/SFTP.

Пакет/сертификат совпадают, code выше, сброс не требуется по текущей модели данных;
реальная установка/сохранность после Android update проверяются пользователем.
Завершите важные операции перед обновлением: Android может закрыть процесс.
Agent переустанавливать не нужно. GitHub публикации не было.

Receipts: .verification/servers-v1.1.9/jvm-final-profile.log,
build-final-profile.log, build-clean-package.log, signature.txt, badging.txt,
audit.py, artifact.json, SHA256SUMS, update.json, localization.txt.
