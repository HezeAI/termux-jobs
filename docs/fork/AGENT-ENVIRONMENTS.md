# Agent Ubuntu environments

## Current source after private test17

The user-facing Ubuntu → Ubuntu AI migration feature has been removed. There is
no migration button, rootfs clone command or automatic switching of saved agent
profiles. Existing sessions, environments, histories and authorization are retained.
This removal does not disable internal application-data compatibility on update.

All four agents can still explicitly select Ubuntu or Ubuntu AI. New profiles
default to Ubuntu AI; existing selections remain unchanged. Selection itself does
not install a rootfs. Explicit launch uses the existing preparation confirmation
and creates only a missing selected environment, without replacing an installed
system or falling back to another alias. `proot-distro login ubuntu-ai` enters the
separate AI environment; `proot-distro login ubuntu` enters ordinary Ubuntu.

Normal environment import, backup, history/resume and package installation remain
independent features. The optional connection launcher stays an absolute executable
path without arguments; empty means ordinary launch. Authorization is read by the
CLI in its selected environment and is not copied to another rootfs.

## Shared environment deletion

Explicit environment deletion still checks references from saved/attached
sessions, active processes and native proot-distro locks. The bounded scan token
pins the reviewed tree. Deletion uses no-follow descriptors and a private
quarantine name; it does not follow guest symlinks into shared storage or other
systems. Once destructive deletion starts it cannot be cancelled; failures report
a possible quarantined remainder. No deletion runs merely by opening a menu.

Python and proot-distro prerequisites for this shared operation can still be
prepared after confirmation through the fixed `agent-operation-tools` branch of
`environment-runtime.sh`. This does not restore the removed rootfs migration
feature. Permission failures are reported as permission failures, not missing
packages. No installed user environment is used as a clean-install test fixture.

## Совместимость

Пользовательская миграция Ubuntu → Ubuntu AI удалена. Выбор среды, обычный запуск,
продолжение диалогов, импорт, резервное копирование и безопасное удаление окружений
сохранены. Уже существующие файлы и авторизация не переносятся и не удаляются.
Внутренняя совместимость данных при обновлении APK продолжает действовать.

See [usage](USAGE.md) and the [helper separation report](HELPER-EXPERIMENT-PAUSED.md).
The optional helper is paused and excluded from the next public release. Earlier
release notes and validation reports describe their historical version, not the
current migration UI or the composition of a future APK.
