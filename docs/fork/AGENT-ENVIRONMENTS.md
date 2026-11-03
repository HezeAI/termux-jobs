# Agent Ubuntu environments

## v2.1.2: shared Ubuntu AI and retained bindings

New AI Agents sessions use Ubuntu AI. Their form explains that Termux supplies
launch and component installation; it no longer offers ordinary-Ubuntu versus
Ubuntu-AI selection. Existing ordinary-Ubuntu and direct-Termux Codex profiles
keep their actual bindings during edit, save and launch. Managers opened from a
session use that original environment/project and do not silently redirect files.

When Ubuntu AI is missing, the common preparation flow downloads the fixed clean
ARM64 image from GitHub Components and installs the local archive using a bundled,
private proot-distro5.9.0. It never downgrades the system manager. Ready environments
are reused without downloading an image; incomplete/busy roots are preserved and
reported. All four agents share one Ubuntu AI and add only missing components
through existing recipes, without automatic CLI upgrades or model conversations.

The explicit launch-environment check shows stages, measured image progress and a
redacted log; reopening joins the existing serialized job. Closing its window
does not cancel preparation. Component readiness does not prove authorization or
model availability. See [installation](ENVIRONMENTS.md) and
[validation](VALIDATION-v2.1.2.md).

The former manual Ubuntu-to-Ubuntu-AI migration action remains removed. Rootfs,
authorization, history and project data are not copied or reset automatically.
Ordinary environment import, backup and explicit update operations remain separate.

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
