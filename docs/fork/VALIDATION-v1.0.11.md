# Проверочная v1.0.11 — размеры OpenCode

com.termux, versionName1.0.11/versionCode1048, ARM64, Android7+.
APK 34256528 bytes; SHA256 `02271597440c21547d7d5c21d6f19b778c84f79af41ae672a989c1ed462c5d03`.
Certificate SHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` совпадает с v1.0.10.
ZIP CRC, package/version, non-debuggable, ARM64 ABI, stored-entry4-byte alignment,
подпись и перечитанный SHA Download-копии проверены. Assets/native .so побайтово
равны v1.0.10; пользовательские auth/SSH/keystore файлы отсутствуют.

## Код и проверки

282 JUnit/Robolectric tests, failures/errors/skipped0; assembleRelease/lintVital
прошли. Новые проверки: opt-out resize показывает поток без дополнительной задержки,
DEC2026 atomic сохранён; выключение активного batch освобождает вывод и отменяет
таймеры. Deferred attach ждёт final layout, пропускает промежуточную геометрию,
переключение берёт последнюю сессию, скрытая View ждёт появления; immediate default
API отменяет ожидание и работает как прежде.

Первый прогон новых View tests падал из-за нулевых font metrics в legacy Robolectric
Paint. В тестовом fixture заданы deterministic cell metrics; финальный прогон прошёл.
Это не проверка шрифтов/IME/DPI/жестов на реальном Android. Локализация531 строк и
git diff --check прошли. Gradle ARM64 FS-watching предупреждение не блокировало build.

## Инспекция

App выбирает deferred attach только для OpenCode (sync-enabled terminal); обычный
attach остаётся immediate. PreDraw observer удаляется при переключении/отсоединении;
при повторном присоединении pending ожидание возвращается. Только показанная View
с завершённым layout и ненулевыми размерами выполняет initial updateSize. Нет новых
таймеров redraw, логирования текста/секретов или сигналов/остановки процессов.

Resize opt-out задаётся по профилю; default batch остаётся для совместимости, но
обычные/Codex сессии sync buffer не используют. Native bridge, модель, история и
bootstrap не менялись; data formatv8 тот же. Native replay250ms и DEC2026 fallback1000ms
остаются. Автопрокрутка/выделение отдельно не менялись.

## Ограничения

Это эксперимент после диагностики OC-001, не доказанное устранение мерцания.
Нужна телефонная проверка переключения, IME, пустого/старого чата и streaming;
новые наблюдения про прыжок вниз при IME и неподвижное выделение отдельно не исправлены.
Одинаковый package/cert и рост code допускают update с1.0.10; миграция данных не
изменялась, но реальная установка не выполнялась агентом. Резервный code1100 выше1048.
GitHub не публиковался, APK не устанавливался.

Receipts: .verification/oc001-build/build-final.log, artifact.json, badging.txt,
signature.txt; исходный checkpoint before-v1.0.11.tar. Исследование native replay —
.verification/oc001-diagnosis/RESULTS.md.
