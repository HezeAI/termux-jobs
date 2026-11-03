# Access from a session

В шестерёнке Codex откройте **AGENTS.md & SKILL.md → Навыки**,
у Claude Code — **CLAUDE.md & SKILL.md → Навыки**. Окно показывает исходные
агента, среду и папку проекта. Общие настройки и вход из сессии используют
один менеджер и те же хранилища; переход в другой чат не меняет источник.
В менеджере доступны «Общие / Проектные» и все прежние действия: GitHub,
папка, ZIP, создание, предпросмотр, обновление, откат и удаление по тем же правилам.
Перед установкой проверьте выбранную область и точный путь. Сессии не
перезапускаются автоматически, обнаружение навыка не объявляется по наличию файла.

From the Codex gear choose **AGENTS.md & SKILL.md → Skills**; for Claude Code,
choose **CLAUDE.md & SKILL.md → Skills**. The window displays the original agent,
environment and project folder. This entry and General settings use one manager
and the same stores; switching chats never retargets the source. General/Project
and all existing GitHub/folder/ZIP/create, preview, update, rollback and delete
actions remain available under the same rules. Check the disclosed scope and
exact destination before installing. No session restarts automatically and files
alone do not prove agent discovery.

Included in test35; see [APK validation](VALIDATION-test35-agent-resources.md) and the [preceding source checks](VALIDATION-instruction-navigation.md).

# Навыки агентов / Agent skills

Общие настройки → **Навыки агентов**, рядом с инструкциями. Выберите Codex или
Claude Code и вкладку **Общие / Проектные**. Выбор папки проекта доступен только в «Проектных»,
через встроенный файловый диалог. Среда по умолчанию Ubuntu AI; старая агентская сессия сохраняет фактическую привязку с пояснением. Выбора среды здесь нет; запросы и файловый диалог используют показанную среду.
Рабочие чаты не запускаются и не перезапускаются при открытии менеджера.

General settings → **Agent skills**, beside Agent instructions. Select Codex or
Claude Code and **General / Project**. The project-folder selector is available only on Project,
using the existing folder browser. The current chat only supplies a suggested folder.
Opening this manager does not start, restart or query a working model session.

## Window layout / Компоновка окна

Заголовок, вкладки и нижние действия закреплены. Выбор агента, Ubuntu AI,
фактические каталоги и список находятся в одной прокручиваемой области до нижних
кнопок. Папка проекта показана только в «Проектных». Длинные подписи кнопок
переносятся без уменьшения шрифта. Клавиатура уменьшает доступную середину окна;
проверка настоящего IME и касаний на телефоне описана отдельно в отчёте.

Title, tabs and bottom actions stay fixed. Agent/environment, actual source paths
and skills share one vertical viewport. The dialog follows available window space
rather than a physical-screen height percentage. The instruction manager uses the
same layout; real keyboard/device acceptance remains separate.

[Layout validation](VALIDATION-test31-agent-managers.md).

## Sources and scope

Paths come from the selected Ubuntu's actual HOME, CODEX_HOME and
CLAUDE_CONFIG_DIR, using the same environment preparation as the instruction editor:

| Agent | New personal installs | New project installs | Other inspected sources |
| --- | --- | --- | --- |
| Codex | `$HOME/.agents/skills` | `<project>/.agents/skills` | `$CODEX_HOME/skills`, ancestor project roots, `/etc/codex/skills`, config plugin cache |
| Claude Code | `$CLAUDE_CONFIG_DIR/skills` (default `$HOME/.claude/skills`) | `<project>/.claude/skills` | ancestor project roots, config plugin cache |

General lists shared/legacy/system/plugin roots with their actual paths and does not search project or ancestor skill directories. Its list and install target are independent of the selected project. Project lists project/ancestor skills and retains collision information from shared roots. No existing skill is moved by this UI rename. Existing sources are not moved or duplicated. Only packages
recorded by this manager at the selected install root can be updated or removed.
External, bundled and plugin packages remain read-only. Unknown source/version is
shown as unknown, never inferred from its name. Custom launcher environments and
unlisted enterprise/plugin layouts may require native CLI inspection; the list is
not an exhaustive active-process catalog. Symlinked sources are not traversed.

Codex 0.166.0's native `skills/list` confirmed user, legacy and project discovery,
same-name entries listed separately, and `forceReload` finding a newly added skill.
Claude 2.1.296's native initialize catalog and a loopback synthetic provider confirmed
personal/project discovery and personal priority for a same-name project skill.
A newly added skill was absent until `/reload-skills`; that command then made it
available in the same process. Different versions/settings can differ.

**Files installed**, **agent discovered the skill**, and **functionality checked**
are different facts. This manager does not introspect or restart working CLI
processes. It conservatively shows discovery unconfirmed and functionality not
checked. In Claude's native terminal use `/reload-skills` when necessary. For Codex,
allow its list refresh; if absent, explicitly restart only the intended agent and
resume its conversation. Previously loaded instructions can remain in history
following an update or removal. Import never changes authorization or agent rights.

## Add and inspect

1. Press **Add** and choose public GitHub, local Ubuntu AI folder or ZIP.
2. For GitHub enter repository/folder URL, optional branch/tag/commit and subdirectory.
   For branch names containing `/`, use the separate revision and subdirectory fields.
3. The manager resolves and records the actual commit, then prepares a private snapshot.
4. Select one or several candidates. Each gets its own preview and explicit install.
5. Read the SKILL.md, declarations, file names/checksums and exact destination.
6. Confirm **Install skill**. Closing/canceling a preview does not install it.

Локальные папки и ZIP выбираются нашим файловым диалогом внутри Ubuntu AI, включая
доступную ей память телефона. Для файла из другого окружения сначала явно перенесите
его в выбранную Ubuntu AI или общую память. Системный Android picker не используется.
Перед применением видны источник, commit и точный путь; каждый выбранный навык
устанавливается отдельно. При ошибке уже подтверждённые другие навыки не откатываются.

Local folders/ZIPs use the built-in Ubuntu AI browser, including phone storage
visible there. Files private to another environment must first be explicitly
transferred to Ubuntu AI/shared storage. No Android system picker or auth prompt.
Multiple selected packages are individually confirmed transactions, not one global
all-or-nothing repository installation.

The preview includes SKILL.md frontmatter, optional `agents/openai.yaml` declarations,
and the script/executable list. Scripts are **never executed**, dependency declarations never authorize installs.
Dependencies are shown as unverified unless independently checked; no broad tool
scan is performed. SKILL.md supports a deliberately conservative safe YAML scalar
subset (name, description, quoted/block scalars); unsupported YAML is rejected,
not evaluated. Description is required by this importer, even where a CLI accepts
less metadata. Names use 1–64 lowercase letters/digits/hyphens with an alphanumeric
first character. Arbitrary declarations are displayed, not executed or certified.

Limits: downloaded ZIP ≤32 MiB, one extracted/copied package tree ≤32 MiB,
512 files, 512 directories, individual file ≤2 MiB, SKILL.md ≤128 KiB, depth≤16,
64 candidates and aggregate preview text plus declarations≤1 MiB. ZIP central-directory entries≤10,000;
compression ratio protection applies. Selected GitHub subfolders are bounded too.
Very large repositories may exceed download limits before a subfolder can be used.
Archives with traversal, duplicate entries, links, special/encrypted files or
invalid CRC are rejected. Local symlinks/hardlinks and common secret filenames are refused; `.git`
directories are excluded from folder snapshots without reading their contents. These guards are not a full malware or secret-content scan;
review every third-party package. No skill runs automatically on installation.

## Update, rollback and deletion

For an app-managed skill, **Update from source** repeats preparation and preview,
then shows changed file names and SKILL.md diff before **Apply update**.
Saved GitHub revision/subdirectory fields can be changed explicitly; the resolved
commit is always shown. Local modifications block update/rollback/deletion.
No silent name overwrite is possible, including replacement of an empty directory.

The manager retains **one previous version**, outside directories searched for
skills: `<skills-parent>/.termux-jobs-skills/<name>/previous`. Atomic Linux rename
exchange replaces a full package. If that primitive is unavailable, update fails
without falling back to a partial overwrite. After interruption, an explicit
**Recover interrupted operation** action finalizes or discards the recorded staged
transaction according to verified content. It never guesses when content conflicts.
Rollback exchanges the current and previous version, preserving one recovery copy.
Deletion confirms the exact path and removes only that managed package/recovery
copy; external symlink targets and other skills are not followed or removed.

Private previews live under
`$HOME/.local/share/termux-jobs/skill-previews/<token>`. Cancel/close removes that
preview; interrupted previews can be explicitly discarded in the list. At most
8 pending previews are allowed; there is no background/global disk cleanup.
Skill management records are separate from existing session/settings formats;
this change does not migrate sessions, presets, history, instructions or auth.

## Acceptance limits

Automated isolated tests check package operations, conflicts, malformed archives,
interruption/recovery, external-file preservation and native CLI discovery. Native
model-protocol checks use only a loopback synthetic provider and harmless fixture
skills. They do not prove arbitrary third-party skill safety or real model quality.
Phone keyboard/touch, narrow/large-font usability, visual selection and repeated
navigation require separate device acceptance. No network transport fix is claimed.

Official behavior references:
[Codex skills](https://learn.chatgpt.com/docs/build-skills),
[Codex app-server](https://learn.chatgpt.com/docs/app-server),
[Claude Code skills](https://code.claude.com/docs/en/skills).
Pinned native evidence takes precedence over assumptions from newer documentation.

Existing app-settings backup is not expanded by this feature. Backing up skills
requires including their native folders and the adjacent manager state through
the appropriate project/environment backup. Do not assume a settings-only export
contains installed packages or rollback copies.

Included in test33: runtime selection is a plain environment caption. New/global use defaults to Ubuntu AI; legacy agent-session environments are retained and explained. Listing, imports, mutations and project browsing use that same environment. No files are relocated. The old test32 APK remains unchanged. [APK checks](VALIDATION-test33-agent-managers.md).

## Создание навыка / Create skill

В меню **Добавить → Создать навык** заполните имя каталога, краткое описание
(для чего и когда использовать) и инструкции. Имя: 1–64 символа `a–z`, `0–9`,
дефис; первый символ — буква или цифра. Описание обязательно, до1024символов.
Нажмите **Просмотреть**, проверьте полный путь и итоговый SKILL.md, затем
**Создать навык**. Отмена предпросмотра возвращает к форме. Ошибка или конфликт
не очищает введённый текст. Совпадающий каталог автоматически не заменяется.

Файл использует выбранных Codex / Claude Code, показанную среду, вкладку
«Общие / Проектные» и выбранный проект. Создание использует тот же проверенный
этап подготовки и атомарной установки, что импорт. Список обновляется после записи.
Источник отмечается `created-in-app`; версия и commit не выдумываются.
Обнаружение остаётся неподтверждённым, пока его не подтвердит агент штатным способом.
Создание не запускает навык, скрипты, зависимости или запрос к модели.

Choose **Add → Create skill**. Enter a lowercase directory name, a description
of what the skill does and when to use it, and instructions. Names have1–64
lowercase letters, digits or hyphens and start with a letter or digit. The required
description has at most1024characters. **Review** shows the exact SKILL.md path
and content; only **Create skill** installs it. Cancelling the preview returns
to the form. Errors and existing-name conflicts retain the entered text.
The selected agent, displayed environment, General/Project scope and project
choose the destination. Creation reuses staged, atomic installation and refreshes
the list. Source is `created-in-app`, with no invented version or commit. Files
alone do not confirm agent discovery; creation never executes a skill or model call.
