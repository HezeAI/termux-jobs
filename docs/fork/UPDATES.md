# GitHub updates — v1.3.0

Выпуск v1.3.0/code1083 сохраняет пакет/сертификат и schema1 update.json для
прежних клиентов. Код выше публичной1.2.2/1073 и локальных1.2.11/1082.
APK, SHA256SUMS, update.json загружаются вместе. RU/EN highlights описывают
справочник, файлы, SSH и совместимость до установки; очистка не требуется.
Автоматическая проверка при открытии и ручная проверка в настройках сохранены.
Установка подтверждается пользователем, данные автоматически не очищаются.

## Историческая реализация (v1.1.4, механизм сохранён)

# GitHub updates — v1.1.4

Common gear has one update tile. Press its title for a manual check; the square
checkbox labelled Auto on the right enables/disables automatic checks. A visible
hint explains: Check when opening the app. Default is on; existing preference
is preserved. One silent attempt per foreground opening/return, including failures.
No timer or retries within the same opening. Tab switching and configuration
recreation do not reset the opening gate; onStop resets it only outside a
configuration change. Failed requests can retry on the next opening. Disabled
automatic checks never disable the manual action. A busy updater skips the
automatic attempt for that opening rather than running another concurrent worker.
No service/alarm while closed, popup, automatic APK download or installation.
The gear dot/menu version indicates an available update.
Only the public HezeAI/termux-jobs latest stable release is consulted;
no GitHub login, CLI token, shell command or user data is sent. GitHub receives
ordinary network request metadata such as the IP address.

A release must include update.json (schema 1), the ARM64 APK and SHA256SUMS.
The updater compares Android versionCode, displays localized highlights, checks
Android/ABI compatibility and downloads on demand into a private cache. Size and
SHA-256 must match metadata; package, actual APK version and signer must match the
installed fork, with a strictly increasing code. Redirects are HTTPS-only and
restricted to GitHub API/release asset hosts. Android performs the final APK
signature verification and asks the user to confirm installation. Matching a
hash from the same GitHub release is an integrity check, not an independent trust
anchor; the installed signing certificate and Android installer are the boundary.

Cancelling a check/download stops its result; a blocking network read may finish
or time out before the worker exits. Failed/partial downloads are removed. There
is one worker at a time; no service, wake lock or terminal process is started.
Leaving the activity does not make installation automatic. Verified APKs remain
in cache until the next download or Android cache cleanup; no reports/projects
are removed. Pending Android installer flows should be finished before starting
another download. Android 8+ may require allowing installs from this app; then
return and press Install again. The permission is not granted silently.

In-place updates normally preserve HOME, PREFIX, Ubuntu, presets, projects and
agent authorization; uninstall/clear-data does not. No destructive data migration is added in v1.1.4; clearing libraries/history is
not required. Installation may terminate running app processes, so finish important
jobs first. The app never uninstalls itself or promises universal migration
compatibility. A fork with another signing key cannot update this build.

First install of v1.0.19 is manual; older builds cannot acquire this feature
without an APK update. A release missing metadata is reported explicitly.
End-to-end upgrade still needs a genuine later release and device confirmation.

The duplicate OpenCode connection entry is removed from global app settings;
session/preset connection paths remain. App logger default changes to OFF, and a
one-time startup migration changes the previous NORMAL level to OFF. Explicit
DEBUG/VERBOSE levels survive, and later user choices are preserved. This does not
stop Android's system logging or session diagnostics. Key event logging is a
separate existing preference and is not enabled by this change.

Each release must include explicit compatibility highlights in both languages.
The generator merges compatibility text into changes for existing v1.0.19 readers.
The user confirmed the public updater works. The new v1.1.4 opening/toggle UI still requires on-device verification.

## Download UI — local v1.1.2

Manual checks show an indeterminate indicator. APK downloads show a horizontal
bar, percentage and downloaded/expected size, then an indeterminate verification
state. Cancellation and all package/version/hash/signature checks are unchanged.
Automatic checks remain silent. Android download/installer behavior needs device
verification; progress widget tests do not simulate a genuine public release.

Локальная v1.1.3/code1060 сохраняет com.termux, сертификат, библиотекуv8 и чтение
manifestv1/v2. Очистка команд/заметок/пресетов/истории не нужна. Доступен обычный
.tar.gz независимо от gzip/pigz; для восстановления pigz не требуется. update.json
готовится из финального APK, публикация ожидает проверки пользователем.

## Compatibility for installed v1.1.0

The public v1.1.0 updater can discover/install v1.1.4 from schema1 update.json:
com.termux, same signer, code1061 >1057. Until updated it retains its previous
10-hour automatic interval; manual checks can discover the release immediately.
After updating, automatic checks use the per-opening behavior. No library/history
reset, no package/env replacement. Finishing jobs before APK installation remains
necessary because Android can stop app processes. Public GitHub API limits or
network outages may fail a check; automatic errors are silent, manual errors visible.

## v1.2.1

Latest v1.2.1/code1072, schema1, прежние пакет/сертификат, сброс данных не нужен.
Ускорение SFTP и очереди описаны в RU/EN update highlights. Ручная и отключаемая
автопроверка сохранены; скачивание/установка требуют действий пользователя.
