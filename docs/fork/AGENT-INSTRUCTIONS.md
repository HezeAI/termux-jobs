# Project template — local change after test7

Во вкладке **Проектные** Codex и Claude Code появилась плашка
**Шаблон проектных инструкций**, непосредственно над **Открыть инструкцию проекта**.
Она использует полный предоставленный разработчиком русский текст без маркеров:
[встроенный шаблон](../../app/src/main/assets/agent-instructions-project.md).
Codex сохраняет его в **AGENTS.md**, Claude — в **CLAUDE.md** выбранного проекта
и показанной среды. Текст правил одинаковый. Шаблон сам по себе не разрешает
push, публикацию или выполнение перечисленных в нём действий.

Сначала выберите проект явно. Предложение из активного терминала не считается
выбором. Просмотр показывает точный путь и ничего не записывает. «Редактировать
шаблон» переносит его в существующий редактор как несохранённый черновик.
«Сохранить» показывает итоговый текст и путь; существующий файл заменяется
только отдельным подтверждением **Заменить AGENTS.md / CLAUDE.md**. Можно вместо
этого открыть существующий файл. Отмена сохраняет прежний файл; отмена последнего
подтверждения оставляет черновик. Ошибка записи сохраняет введённый текст.
Изменение файла другим процессом вызывает конфликт редакции без перезаписи.
Общие инструкции, другие проекты и LEARNING.md не меняются.

Both Codex and Claude Code expose **Project instructions template** immediately
above **Open project instruction** on Project. The supplied Russian rules are
identical; destinations are the selected project's **AGENTS.md** and **CLAUDE.md**,
respectively, in the displayed environment. Project selection is explicit.
Preview writes nothing. Editing uses the existing editor and unsaved-draft guard.
Saving reviews the exact path and edited text; replacement needs a separate
confirmation. Cancellation, failed writes and revision conflicts preserve the
original file and edited draft. General rules and other projects are untouched.
The English interface explains that the supplied template itself is in Russian.

## Verification scope

Проверено заново на искусственных проектах отдельно для обоих агентов:
расположение плашки RU/EN, просмотр/отмена, создание, открытие существующего файла,
отказ/подтверждение замены, конфликт другой редакции, ошибка записи и сохранение
черновика. UI: Robolectric, не телефон. Хранилище: настоящий Python helper с
временными проектами и ошибкой atomic replace; CLI-диагностика подменена.
Полный текст: 7 882 байта, SHA-256
`3554f8178cfbc032b50bc07f3e937236a66a10dea420da84f3b1a6ed7c6a39e4`.
Тесты: `AgentInstructionsUiTest`, `AgentInstructionsTest`,
`AgentSessionResourcesUiTest`, `scripts/tests/test_project_instruction_template.py`.
Современный Lint: 0 ошибок, 236 предупреждений. APK не собирался.
Телефон ещё не проверен: для каждого агента выбрать отдельную тестовую папку,
открыть плашку, отменить, сохранить изменённый текст и проверить соответствующий
файл; затем отказаться от замены существующего файла и проверить его сохранность.
Доказательства: локальная `.verification/project-instructions-template/`.

---

# Session access and file refresh

В шестерёнке сессии сразу после перезапуска доступны **AGENTS.md & SKILL.md**
для Codex и **CLAUDE.md & SKILL.md** для Claude Code. Окно показывает агента,
фактическую привязку среды и рабочую папку исходной сессии. Эти значения
не меняются при переключении чата. Раздел **Инструкции** открывает этот же
редактор на вкладке **Проектные**; доступны и **Общие**, диагностика,
редактирование, копирование, история, импорт и сохранение. Раздел **Навыки**
открывает [тот же менеджер навыков](AGENT-SKILLS.md). Для Gemini/OpenCode
эти пункты не подменяются файлами Codex/Claude.

Значок **Обновить** справа в закреплённом заголовке перечитывает выбранный
файл в «Общих» или «Проектных». При несохранённом тексте сначала требуется
явно отказаться от черновика; отмена сохраняет текст. В диагностике значок скрыт.
Из меню удалены «Открыть файл заново» и «Дополнение»; остались история,
восстановление предыдущей редакции и импорт. Если проектного файла нет,
редактор сообщает об этом. **Открыть инструкцию проекта** предлагает существующую
форму создания; новая плашка шаблона описана выше.
Открытие ничего не создаёт и не перезапускает сессию. Наличие файла не означает,
что работающий агент уже применил его; правила применения описаны ниже.

The session gear places **AGENTS.md & SKILL.md** for Codex or **CLAUDE.md & SKILL.md**
for Claude Code immediately after Restart. The window shows the original agent,
actual environment binding and working folder; chat switching cannot retarget it.
**Instructions** opens the existing editor on **Project**, retaining General,
diagnostics, editing, copying, history, import and saving. **Skills** opens the
[same full manager](AGENT-SKILLS.md). Gemini/OpenCode never open another agent's files.

The vector **Refresh** action in the fixed title reloads the selected file on
General or Project. Unsaved text requires explicit discard; cancelling retains it.
Diagnostics hides Refresh. Reload file and Addition are removed from the menu;
history, previous revision and import remain. Missing project files point to
**Open project instruction**, which offers the existing creation form. Opening writes nothing and never restarts the
session. Files on disk do not confirm that the running agent applied them.

Included in test35; the delivered test34 APK is unchanged. See [APK validation](VALIDATION-test35-agent-resources.md) and the [preceding source checks](VALIDATION-instruction-navigation.md).

# App template preview and runtime binding

Вместо выбора Ubuntu / Ubuntu AI используется кнопка **Посмотреть шаблон приложения**. Она открывает прокручиваемый встроенный шаблон выбранного Codex / Claude Code, по умолчанию на языке приложения. Просмотр не меняет файл, черновик или язык приложения. RU/EN можно посмотреть без записи. Внизу предпросмотра находятся прежние кнопки **Применить шаблон** и **Отмена**; текст прокручивается над ними. Применение выполняется только явным нажатием с историей и проверкой конфликтов. Отдельного пункта применения в меню больше нет. Кнопка просмотра и применение общего шаблона доступны только во вкладке «Общие». В «Проектных» и «Диагностике» этой кнопки нет; действия проекта не направляются в общий файл. Создание инструкций проекта доступно отдельной кнопкой в «Проектных», без применения общего шаблона. Перед нажатием проверьте показанный точный путь.

По умолчанию оба менеджера используют Ubuntu AI. Небольшая подпись показывает фактическую среду. При открытии из старой агентской сессии сохраняется обычная Ubuntu либо прямой Codex в Termux; переключение агента не меняет среду. Файловый транспорт, выбор проекта и запросы чтения/сохранения используют эту же привязку. Ничего из обычной Ubuntu не копируется в Ubuntu AI. Если выбранный агент не поддержан в сохранённой среде, показывается ограничение без перенаправления.

The **View app template** button replaces runtime selection. It opens the selected agent's bundled template, initially in the app language, without writing or replacing the draft. The existing **Apply template** and **Cancel** buttons are fixed below the scrollable preview; application remains explicit with revision history and conflict checks. The separate menu item is removed. Viewing and applying the general template are available only on General. Project and Diagnostics have no such button; project actions never redirect to general instructions. Project instruction creation uses its separate Project button and never applies the general template. Check the disclosed exact destination before applying. Both managers default to Ubuntu AI and visibly retain legacy agent-session bindings. Switching agents does not silently switch environments; file operations target the displayed environment. No instructions are migrated or overwritten automatically.

Updated in test34; previous APKs remain unchanged. See [APK verification](VALIDATION-test34-agent-managers.md) and the [preceding test33 report](VALIDATION-test33-agent-managers.md).

# Current instruction editor layout

Общие настройки → «Инструкции агентов». Вкладки «Общие / Проектные / Диагностика»
и заголовок закреплены. Выбор агента, подпись среды, параметры файла и редактор находятся
в одной вертикальной прокрутке до нижнего блока «Меню», «Сохранить», «Отмена / Закрыть».
При узкой ширине вкладки можно прокрутить горизонтально; выбранная плашка выделена.
Редактор использует доступную высоту и растёт с текстом, без внутреннего лимита14 строк.
Клавиатура уменьшает середину; подписи нижних действий при необходимости переносятся.
Явная «Отмена / Закрыть» сохраняет прежний смысл; Back, тап снаружи и переходы
с несохранённым черновиком сохраняют существующую защиту. Сообщения сохранения/ошибки
раскрываются в прокручиваемой области и не изменяют черновик.

Both agent managers now use a shared library-style dialog. Title/tabs/footer stay
fixed; content owns one vertical scroll. The editor expands and has no separate
14-line viewport. Native available-window resizing replaces a fixed screen-height
percentage. File loading/saving/history/import/conflict rules below are unchanged.
See [validation and remaining phone checks](VALIDATION-test31-agent-managers.md).

---

# Codex and Claude Code instruction editor — test27

General settings → **Agent instructions** offers **Codex / Claude Code** above
**General / Project / Diagnostics**. The selected agent remains highlighted.
Codex retains Ubuntu AI and its actual CODEX_HOME. Claude offers Ubuntu AI/Ubuntu;
a current Claude chat suggests its environment/project. Switching environments
resets the suggested project to /root. No environment is installed by this editor.
Choose another project through the existing folder dialog or an absolute path.
The exact edit target remains visible; a file selector never changes CLI settings.

## Claude file semantics and limits

The application's existing bootstrap pins Claude **2.1.296** and an independent
native binary. It runs in the selected proot Ubuntu, after the same Bash startup,
using the selected project's working directory. It inherits CLAUDE_CONFIG_DIR;
the default is ~/.claude. The editor uses that environment and working directory,
not a hardcoded /root/.claude. It does not run the install/bootstrap script or
inspect authentication. A custom connection executable can change configuration;
diagnostics describes standard startup, not a running process introspection.

General edits CLAUDE_CONFIG_DIR/CLAUDE.md. Project offers CLAUDE.md,
CLAUDE.local.md and .claude/CLAUDE.md, plus shared LEARNING.md. Claude's regular
files combine; local is not a Codex-style override. The UI warns that creating a
project Claude file can disable default AGENTS.md fallback. Root/ancestor files,
managed /etc/claude-code/CLAUDE.md, user/project rules, imports, CLI settings and
exclusions can additionally affect context. Diagnostics records file candidates,
configuration presence and only whitelisted instruction-mode settings; it does
not expand arbitrary imports or read credentials. Rules directories are marked,
not presented as proof of every active conditional rule. Actual CLI loading can
therefore differ when nonstandard launch/settings/policy are used.

Claude 2.1.296 contains a separate built-in AGENTS.md mechanism. Its documented
default selects project AGENTS.md when no project/ancestor Claude file is found,
but loading also depends on plugin/settings availability. The release rerun did
not observe AGENTS.md reaching the loopback request in its fresh-profile probe;
therefore file presence or a nominal default is not confirmation of loading.
Use CLAUDE.md for the supported editor path. A user-scope file is distinct from
project/ancestor files.
AGENTS.override.md is not supported. Plugin/settings changes can alter fallback;
the editor never changes those settings. See official
[Claude memory rules](https://code.claude.com/docs/en/memory) and
[AGENTS.md selection](https://code.claude.com/docs/en/memory#agentsmd).
These upstream descriptions were checked alongside the actual pinned binary;
future versions must not inherit a blanket behavior guarantee.

## Saving, import, memory and application

Claude uses the same revision-checked atomic save and path-bound history engine.
General/project template application and import require preview/explicit action;
existing files are never replaced by simply opening the editor or launching a
Claude chat. Import from Project targets CLAUDE.md when editing either Claude
instruction file; LEARNING import targets the chosen shared memory file. Cancel,
errors and conflicts preserve the file/draft. Agent/env switches use the existing
draft guard, and stale previews cannot write to another agent or environment.

The memory path preference is shared between agents within the same environment
and project, including the previously selected Codex path. Literal instruction
references and explicit existing-file selection are supported. No per-agent
LEARNING copy is created. Memory history remains under the selected editor's
configuration directory; the physical memory file is shared. Native Claude
MEMORY.md auto memory is separate and is not disabled/reconfigured by the editor.
The common LEARNING template names both AGENTS.md and CLAUDE.md; no user memory
file is changed by this template adjustment.

Real pinned Claude + the existing app controller used an isolated HOME/config
and a synthetic loopback provider. Global/project edits went through the actual
Python save backend, not merely file-presence assertions:

| Scenario | Confirmed provider input |
| --- | --- |
| First request | Global/project/local/.claude markers all present; override and unimported LEARNING absent |
| Save then next request, same process | Original global/project markers; new markers absent |
| Restart and resume same native history | New global/project markers present; old history retained |
| Fresh conversation/new process | New markers present |
| No project Claude files, fresh process | Earlier probe observed fallback; release rerun did not. AGENTS.override marker absent in both. Treat fallback as conditional, not confirmed for a working session. |

Thus the Claude save notice recommends explicit restart/resume; no live-update
claim is borrowed from Codex. Retained history can still contain earlier text.
This proves instruction delivery, not a real model's semantic compliance. Unknown
CLI versions are labeled unverified. No working chat is restarted by saving.
Codex's existing message/restart guidance, sources and configuration stay intact.

---

## test26 memory-update guidance

Only the RU/EN project-memory Updating subsection changes. Before finishing a
project task, assess useful confirmed lessons across code, documents, 3D models,
data, tools and workflows. Record a lesson when it prevents recurrence or materially
helps future work, with project-permitted creation/update, date, context, conclusion,
conditions and evidence. Do not log ordinary typos, one-off external failures or
unqualified common advice. No useful lesson means no file change and no routine
absence report. Evidence/privacy/currentness/permission and no-background-training
constraints remain. This is agent guidance; the app does not create memory files
or overwrite user instructions automatically.

## test25 reorganization target

The complete approved RU/EN documentation section retains the 20,480-byte trigger
and adds a target of at most 15,360 bytes. Keep project purpose, current paths,
memory location, mandatory restrictions and required-reading links in AGENTS.md.
Detailed topics belong in suitable documents; links require reading before related
work. New details stay in topical docs, confirmed lessons in LEARNING.md. Target
size never permits dropping requirements or reducing mandatory-rule visibility.
Report before/after size and checks; explanations are required only for obstacles
or inability to reach the target safely. The application does not rewrite saved
user instructions automatically; explicit template preview/apply remains required.

## test24 mandatory reorganization rule

The built-in RU/EN documentation section now directs the agent to reorganize
project AGENTS.md above 20,480 bytes before finishing the task, without separate
approval. Five steps preserve mandatory restrictions and navigation in AGENTS.md,
place detailed requirements in appropriate documents and verify all conditions
and links. Reconciling unique safety conditions is part of the work. Only explicit
prohibitions, unavailable write access or a concrete unresolved requirements
conflict can stop it. The result reports before/after size, documents and checks.
This changes agent guidance, not application background behavior. Saved user rules
are not automatically replaced; explicit template review/apply remains required.
Earlier dated sections below describe the named historical template versions.

## test23 structural-assessment rule

RU/EN templates now separate first-access byte measurement from mandatory
four-step structural assessment above 20,480 bytes. The agent must identify
mandatory entry-point restrictions, topical requirements and existing documents,
benefits and preservation risks, then perform a beneficial split or give a concrete
deferral reason. “Outside scope” alone is insufficient; explicit user/project
prohibitions remain binding. Report size, decision and basis without claiming
assessment from measurement alone. Markdown lists have a separating blank line.
Other template sections and saved user files are unchanged.

## test22 size-assessment clarification

The RU/EN built-in rule now requires measuring project AGENTS.md in bytes on its
first access within a task. Above 20,480 bytes, assess the need for topical documents.
Do not remeasure an unchanged file. Split only for demonstrated benefit; briefly
explain a deferral in the result. All other rules are retained. This is an agent
behavior instruction, not automatic file splitting or disk monitoring. User text
still changes only after template preview and explicit Apply. No project AGENTS.md
reorganization is performed by this update.

## test21 template revision

The full approved RU template comes from TASK-update-general-template.txt, not the
older instructions-review file. EN preserves all 77 bullet rules, matching section
order and restrictions. Documentation reorganization is permitted within a current
project task unless prohibited by project rules; all requirement meanings and
applicability conditions must survive. 20 KiB (20,480 UTF-8 bytes) is a review trigger, not
an automatic split or a claimed performance gain. This update does not reorganize
this project's AGENTS.md or change any user's saved instructions. Template preview,
explicit apply, history and conflict safeguards are unchanged. RU/EN storage
previews end before the next second-level heading.

# test20: project memory and refreshed templates

General RU/EN templates now include the agreed task scope, safety, storage and
LEARNING.md rules. Existing files, including empty user files, remain untouched
on upgrade. Review and explicit Apply are required to replace custom text.
RU content retains the reviewed source plus the explicitly supplied project-documentation organization section; EN preserves the same meaning and restrictions.
The storage addition is bounded to its own section, not the later memory rules.

Project offers AGENTS.md, AGENTS.override.md and LEARNING.md. All three have
single-line scrollable selectors, visible selection and independent explanations.
Memory is reference material read when the project directs the agent to read it;
it is not in Codex's instruction-priority chain and is not automatic training.
An absent memory file is shown without creation. Create from template previews
its content and exact path before an explicit atomic save. Save, import and
revision recovery use the existing path-bound history and conflict checks.

By default memory is the selected project's LEARNING.md. Literal references in
project instruction files are read as data, never executed: absolute Markdown
paths and backticked relative paths are supported. A unique reference selects
that path; multiple references require explicit selection. Do not assume arbitrary
natural-language path descriptions can be resolved. The UI asks the user to check
the project rules before creating memory and offers the built-in file chooser.
Choose an existing LEARNING.md in Ubuntu AI to edit the original, rather than
importing a copy. That explicit per-project path is retained in editor preferences;
it does not change CODEX_HOME, agent launches or instruction precedence.
For this termux-fork workspace, the shared memory is `/root/termux-fork/LEARNING.md`.
No second memory file is created in the working checkout.

With LEARNING.md selected, Import writes only the resolved memory file. With
AGENTS.md/AGENTS.override.md selected, the existing project import still targets
AGENTS.md and warns about an active override. General import is unchanged.
Draft guards, atomic writes, history and stale-revision rejection apply to memory
too. No model request, session restart or background self-learning is started by
the editor. Real phone keyboard/touch checks remain separate from layout models.

---

## Private test16 adaptive editor

Scope and file selectors keep single-line labels in horizontally scrollable
rows, with persistent selection/accessibility and the independent effective-file
label. Natural widths replace equal thirds; system fonts are preserved. Project
explanations and the bounded multiline editor scroll in the center, with
save/menu/close controls outside that content. Long save/error status text has a
bounded scroll area. Preview paths/warnings scroll with the text while RU/EN and
apply/cancel stay accessible. No file or language changes occur merely by scrolling.

Narrow/large-font/reduced-height framework fixtures and real local Android font
metrics are documented in [test16 validation](VALIDATION-test16-chat-recovery-layout.md).
Real IME, device font shaping and touch remain phone acceptance checks.

## Private test15 editor behaviour

The selected General/Project/Diagnostics tab and edited AGENTS.md/override button
keep accent outline/fill/text after release and expose accessibility selection.
Editing a file is separate from choosing the file Codex reads; the existing actual
source label remains authoritative. Back/outside dismissal of a dirty editor asks
to discard or continue editing. Explicit Cancel/Close discards intentionally.
Saving prevents dismissal; Back closes a visible keyboard first. Actual device
IME, font-size layout and accessibility remain phone acceptance checks.

See [test15 validation](VALIDATION-test15-audit-fixes.md).

# Agent instructions — Codex in Ubuntu AI

The global settings menu contains **Agent instructions**; the editor heading is **Codex → Ubuntu AI**.
It edits files inside that installed environment. It is shared by its projects,
not a per-chat prompt. Other agents and ordinary Ubuntu/Termux targets are not
changed by this first integration. A chat supplies only a suggested project path.

## Общие инструкции и инструкции проекта

| Вкладка | Область действия | Что записывать |
| --- | --- | --- |
| **Общие** | Проекты Codex в этой Ubuntu AI, использующие тот же `CODEX_HOME` | Язык ответов, общий порядок работы и обращения с файлами. |
| **Проект** | Выбранная папка и применимая к запуску цепочка каталогов проекта | Команды проверок, структуру проекта, ограничения сборки и выпуска. |

Например, «Отвечай по-русски» подходит для **Общие**. «Не собирай APK без запроса» подходит для **Проект** конкретного приложения.
Общие инструкции служат основой. Проектные инструкции дополняют и уточняют их. При противоречии более близкие к рабочей папке проектные инструкции имеют приоритет над более общими.
Это не расширяет права агента и не отменяет ограничения сервиса, среды или указания пользователя.

`AGENTS.override.md` заменяет `AGENTS.md` только в том же каталоге, если содержит непустой текст. Он не отключает всю цепочку общих и родительских инструкций.
Пустой override не перекрывает `AGENTS.md`. Кнопка файла выбирает текст для редактирования, а не включает этот файл для агента.

Выбор папки в редакторе задаёт место редактирования. Он не переносит туда чат и не привязывает правила к одному диалогу.
Чтобы агент использовал правила нужного проекта, проверьте рабочую папку его запуска. Вкладка **Диагностика** помогает проверить стандартную цепочку файлов.
Строка **Codex читает в этой папке:** показывает источник только выбранного каталога, а не полный набор правил работающего чата.

`LEARNING.md` хранит справочные уроки проекта. Это не ещё один уровень инструкций; агент читает память по указанию в инструкциях проекта.
Сохранение не меняет выполняющийся запрос. Общие правила могут обновиться уже к следующему запросу текущего диалога.
Для повторного чтения общих и проектных правил перезапустите агента с продолжением диалога.
Подробности и ограничения версий приведены в [When edits apply](#when-edits-apply).

## Выбор папки проекта и открытие инструкций

1. Нажмите общую шестерёнку приложения, затем **Инструкции агентов**. Вверху окна указано **Codex → Ubuntu AI**; это подпись среды, а не ещё два вложенных меню. Ubuntu AI должна быть установлена и доступна с Python 3.
2. Откройте вкладку **Проект** и проверьте строку **Проект:**. Чат Ubuntu AI лишь предлагает свою рабочую папку; иначе начальное значение — `/root`. Это ещё не выбор папки вашего проекта.
3. Нажмите **Выбрать другой проект**. В окне **Проект Ubuntu AI** выберите **Папка**, перейдите в существующий каталог встроенным файловым диалогом и нажмите **Выбрать папку**. Либо введите абсолютный путь внутри Ubuntu AI, например `/root/projects/demo`, и нажмите **Открыть**. Не вводите `~` или путь из другой среды. Выбор не создаёт каталог и не меняет рабочую папку чата.
4. Нажмите **AGENTS.md** или **AGENTS.override.md**. Если название не видно, прокрутите ряд плашек горизонтально. Проверьте точный путь в строке **Редактирование:**. Выбор плашки открывает файл для редактирования; он не переключает действующие правила Codex. Отдельная строка **Codex читает в этой папке:** показывает выбранный источник по стандартному правилу приоритета. Непустой override перекрывает AGENTS.md своей папки; общие и родительские правила сохраняют свою область действия.
5. Прочитайте или измените текст. Если файла нет, одно открытие его не создаёт. Для записи нажмите **Сохранить** и дождитесь подтверждения. При конфликте черновик остаётся в редакторе: скопируйте его, откройте файл заново и сравните версии. **Меню** содержит импорт, историю и восстановление. Проектный импорт инструкций направляется в AGENTS.md даже при выбранном override; перед записью показаны путь и предупреждение о перекрытии.

**Общие** редактируют файл фактического CODEX_HOME этой Ubuntu AI, а не файл выбранного проекта. Выбор папки проекта не меняет общие инструкции.
Сохранение не перезапускает агента и не меняет выполняющийся запрос. Общие правила могут примениться уже к следующему запросу текущего диалога. Для повторного чтения общих и проектных правил выполните явный перезапуск агента с продолжением диалога; подробности зависят от версии CLI и приведены в разделе [When edits apply](#when-edits-apply).

В test20/test21 рядом доступен **LEARNING.md**; публичная v2.1.1 ещё не содержит этого выбора. Память не является третьим уровнем приоритета инструкций. Проверьте показанный путь до создания или импорта. Если проект использует общий файл памяти вне своей папки, выберите **Выбрать существующий файл памяти**, затем Ubuntu AI и нужный LEARNING.md. Для этого termux-fork путь — `/root/termux-fork/LEARNING.md`; дубликат в рабочем checkout создавать не нужно.

## Choose a project folder and open its instructions

1. Open the application's general gear menu → **Agent instructions**. **Codex → Ubuntu AI** is the editor's environment heading, not two additional menu choices. Ubuntu AI must already be installed and accessible with Python 3.
2. Open **Project** and check **Project:**. An Ubuntu AI chat only suggests its working directory; otherwise the initial suggestion is `/root`.
3. Select **Choose another project** → **Folder**, navigate with the built-in browser and confirm **Select folder**. Alternatively enter an existing absolute Ubuntu AI path, such as `/root/projects/demo`, and press **Open**. Do not use `~` or another environment's path. Selection neither creates a directory nor changes the chat's working directory.
4. Select **AGENTS.md** or **AGENTS.override.md**; scroll the selector row horizontally if needed. Check **Editing:** for the exact destination. This selects the edit target, not the active rules. **Codex reads in this folder:** independently shows standard file precedence. A nonempty override masks this folder's AGENTS.md; shared and ancestor rules retain their scope.
5. Read or edit the text. Opening an absent file does not create it. Press **Save** and wait for acknowledgement. On conflict, copy the retained draft, reload and compare. **Menu** offers import, history and recovery. Project instruction import targets AGENTS.md even when editing its override; review the destination and masking warning before confirming.

**General** edits this Ubuntu AI's actual CODEX_HOME source, independently of the project folder. Saving does not restart agents or modify an executing request. Shared rules may apply to the next request in the current conversation. Explicitly restart the agent and resume the conversation to reread both shared and project rules; see [When edits apply](#when-edits-apply) for version-specific limits.

**LEARNING.md** is available beside the instruction files in test20/test21, not public v2.1.1. It is not an instruction-priority level. Review its exact path before creation/import. For shared memory outside the project folder, use **Choose existing memory file** → Ubuntu AI → the existing LEARNING.md. This termux-fork uses `/root/termux-fork/LEARNING.md`, without a checkout duplicate.

## Sources and precedence

The helper enters Ubuntu AI, sources its Bash initialization as the normal
session does, and resolves its actual `CODEX_HOME` (default `~/.codex`). General
edits target the first non-empty global `AGENTS.override.md`, otherwise AGENTS.md.
No fixed `/root/.codex/AGENTS.md` is assumed. The selected path is displayed.

Project offers AGENTS.md, AGENTS.override.md and LEARNING.md in the chosen directory. The
picker is bound to Ubuntu AI; an absolute path can also be entered. An active
override is identified when it masks AGENTS.md. Directly below the file buttons,
a compact RU/EN explanation describes precedence and continued shared rules.
A separate line identifies the non-empty standard file selected in this folder,
or reports no project instructions here. This state is calculated from both files
at read/save; selecting an edit target does not switch the effective rules. It does
not describe an already running request or negate parent-directory instructions.
Diagnostics lists the standard
Git-root-to-directory instruction chain, effective global path, source hashes,
configuration-setting presence and CLI version. Paths/details are expandable.
Custom fallback names, trust-dependent project configuration, profile-specific
configuration and arbitrary user connection executables can additionally affect
a launch; the editor does not pretend to introspect every running process.
A connection executable that changes CODEX_HOME may differ from the standard
Ubuntu AI shell inspected by the editor. Check that custom launch separately.

AGENTS files and `developer_instructions`/`model_instructions_file` configuration
are different mechanisms. The editor changes only the selected AGENTS file.
Diagnostics reads relevant configuration metadata and reports setting names,
never arbitrary values, provider credentials or auth files. It does not read the
contents of arbitrary replacement/fallback files. Codex's own size limits still
apply; editing the full source does not disable instruction truncation limits.
Internal service/model rules and sandbox/approval restrictions are not editable.

## Exact saves, history and conflicts

Text is UTF-8 without trimming or newline normalization. Saving uses a same-folder
atomic replacement, with a private previous revision first. An advisory lock
serializes app writers; the saved revision/source is compared again to detect
parallel and external edits before publication. A conflict keeps the editor draft
and does not overwrite the other revision. Reload is explicit. Files with unsafe
links/types are refused. Close/cancel does not save a draft. A save already in
progress completes as its own operation; controls are disabled while it runs.

History resides under the resolved `CODEX_HOME/.termux-jobs-instructions/history`.
Records are bound to the exact target path. Menu actions preview a prior version
before explicit restore; restoration also preserves the current file as a new
history record. History can contain private user instructions and stays private.
There is no automatic history pruning, export, cleanup or disk quota.

## New installations and template updates

At a user-launched Codex startup in Ubuntu AI, the built-in RU/EN template is
created only if neither global AGENTS file exists. Even an empty existing file
is preserved. This initialization runs inside the same connection wrapper and
shell as the following Codex process, so its CODEX_HOME follows that launch.
Missing Python is included in the existing consented Codex Ubuntu AI preparation;
no environment/package installation is triggered by opening this editor.

Existing text is not replaced on APK update. General offers a full built-in
preview with explicit Apply and save, plus a separate preview of **File and
temporary data storage**. The latter appends the displayed section to the current
editor text, retaining its prefix and storing the previous file revision. Cancel
leaves both file and draft unchanged. Template, storage-addition and history
previews have a window-bounded scrolling middle panel with fixed bottom Apply/
Cancel controls. RU/EN selectors stay above the scroll area and default to the UI
language on each preview opening. Both versions come from the shipped assets;
selecting another language affects only the preview, not the app locale or user
instructions. The selected language and exact target path are shown before the
explicit Apply template / Append and save action. A successful application shows the regular saved notice; failures
retain the existing conflict/error behavior. The standard storage section is not appended when either its Russian or English
heading already exists in the editor text, even after switching languages. Existing
manually created duplicates are not deleted or translated. Other unrelated text is
not treated as that section solely because it mentions file storage.
Project-specific requirements remain in the project's AGENTS files; the general
template menu is not offered as a project-file reset.

These storage instructions guide agent behavior for every project. They do not
implement disk monitoring, automatic cleanup, retention deadlines or hard limits.
Existing SDKs, caches, data and environments are not deleted by this feature.

## Импорт инструкций

1. Откройте **Общие** или выберите папку на вкладке **Проект**. Проверьте файл для редактирования.
2. Нажмите **Меню** → **Импортировать инструкции** для общих правил или **Импортировать AGENTS.md** для проекта.
3. Если есть несохранённый текст, выберите **Сохранить черновик**, **Отказаться от черновика** или **Отмена**. Ошибка сохранения не открывает выбор файла. Отмена сохраняет черновик в редакторе. После отказа от черновика отмена выбора файла его не возвращает.
4. Во встроенном диалоге выберите окружение или **Память устройства**, затем текстовый файл. Нужен обычный файл UTF-8 без ссылок и бинарных данных, размером до 256 КиБ (262 144 байта).
5. В предпросмотре проверьте текст, строку **Файл назначения:** и предупреждения. Импорт заменяет содержимое целиком, без перевода и объединения.
6. Нажмите **Импортировать и сохранить** и дождитесь подтверждения. **Отмена** в предпросмотре не меняет файл назначения.

В **Общие** импорт заменяет фактический общий файл в CODEX_HOME этой Ubuntu AI. Если это AGENTS.override.md, предпросмотр предупреждает о его замене.
В **Проект** импорт инструкций всегда записывает AGENTS.md выбранной папки, даже если открыт AGENTS.override.md. Override сохраняется. Если он действует, импортированный AGENTS.md остаётся перекрытым; предпросмотр показывает предупреждение. После успешного импорта редактор открывает AGENTS.md.

В версиях с выбором **LEARNING.md** пункт называется **Импортировать LEARNING.md**. Он копирует текст в выбранный файл памяти. Для редактирования исходного файла без копирования используйте **Выбрать существующий файл памяти**.

Импорт сохраняет предыдущий текст существующего файла в истории при изменении содержимого. Изменение источника во время чтения или конфликт назначения блокирует замену. Импорт не перезапускает чат. Сроки применения правил описаны в [When edits apply](#when-edits-apply).

## Восстановление предыдущей редакции

1. Откройте нужную вкладку и файл. Для проекта проверьте папку и строку **Редактирование:**. История относится к точному пути файла.
2. Если нужен несохранённый черновик, сначала сохраните или скопируйте его. Восстановление заменяет текст редактора; отдельного выбора сохранения черновика перед ним нет.
3. Нажмите **Меню** → **Восстановить предыдущую версию**. Откроется предпросмотр самой новой записи истории. Для более ранней записи выберите **Меню** → **История редакций**, затем строку с датой, временем и началом SHA-256.
4. В окне **Предыдущая редакция** сравните текст и **Точный путь:**. Нажмите **Восстановить и сохранить** только после проверки. **Отмена** оставляет файл и черновик без изменений.
5. Дождитесь подтверждения сохранения. При внешнем изменении файла восстановление отклоняется; сохраните нужный черновик отдельно, откройте файл заново и сравните версии.

При отсутствии записей появится **Предыдущих редакций нет.** История показывает до 100 последних записей выбранного пути. Это сохранённые версии перед заменой, а не история каждого нажатия клавиши или всех внешних правок.
Восстановление записывает текст в выбранный файл. При изменении содержимого текущая сохранённая версия также попадает в историю. Несохранённый черновик в историю не попадает. Повторное восстановление может вернуть текст, который только что заменили.

История AGENTS.md и AGENTS.override.md раздельная. Восстановление AGENTS.md не отключает действующий override. Для **LEARNING.md** действуют те же операции с историей выбранного пути памяти.

## Importing instruction text

1. Open **General**, or choose the folder under **Project**. Check the edit target.
2. Choose **Menu** → **Import instructions** for shared rules, or **Import AGENTS.md** for project instructions.
3. For unsaved text, choose **Save draft**, **Discard draft** or **Cancel**. A failed save stops the import. Cancel retains the draft. Cancelling the picker after discarding does not recover that draft.
4. In the built-in dialog, choose an environment or **Device storage**, then a text file. Use a regular, unlinked UTF-8 file without binary data, up to 256 KiB (262,144 bytes).
5. Review the text, **Destination file:** and warnings. Import replaces the entire contents without translation or merging.
6. Press **Import and save** and wait for acknowledgement. **Cancel** in the preview leaves the destination unchanged.

General targets the actual shared source in this Ubuntu AI's CODEX_HOME. The preview warns when replacing an active AGENTS.override.md.
Project instruction import always targets the selected folder's AGENTS.md, even when editing AGENTS.override.md. The override remains intact. An active override still masks the imported file, as the preview warns. After a successful import, the editor opens AGENTS.md.

Versions offering **LEARNING.md** instead show **Import LEARNING.md** when memory is selected. This copies text into the selected memory path. Use **Choose existing memory file** to edit the original without copying it.

Changed existing contents are retained in history. Source changes during reading or destination conflicts refuse replacement. Import does not restart a conversation. See [When edits apply](#when-edits-apply).

## Restoring a previous revision

1. Select the intended tab and file. For Project, check the folder and **Editing:** path. History belongs to that exact file path.
2. Save or copy any draft you need first. Restoration replaces the editor text without a separate draft-save prompt.
3. Choose **Menu** → **Restore previous version** to preview the newest history record. For an older record, choose **Menu** → **Revision history**, then a timestamp and SHA-256 prefix.
4. In **Previous revision**, review the text and **Exact path:**. Press **Restore and save** to confirm. **Cancel** retains both the file and draft.
5. Wait for acknowledgement. External file changes reject restoration. Copy any needed draft, reload the file and compare revisions.

An empty history reports **No previous revisions.** The list shows up to 100 latest records for this path. These are saved contents before replacement, not every keystroke or every external edit.
Restoration writes to the selected file. If contents change, the current saved version enters history too. Unsaved drafts do not enter history. Repeating restoration can bring back the text just replaced.

AGENTS.md and AGENTS.override.md have separate histories. Restoring AGENTS.md does not disable an active override. The same history operations apply to the selected LEARNING.md path.

## Когда сохранённые инструкции начинают действовать

Сначала нажмите **Сохранить** и дождитесь подтверждения. Черновик в редакторе ещё не меняет файл инструкций.
Сохранение, импорт и восстановление записывают файл. Они не перезапускают агента и не отправляют правила отдельным сообщением.

| Ситуация | Что происходит |
| --- | --- |
| Запрос уже выполняется | Сохранение не заменяет инструкции этого запроса. |
| Следующий запрос текущего диалога | Общие правила могут обновиться. В проверенных Codex 0.163.0 и 0.164.0 общие правила обновились, а проектные остались прежними. |
| Текущий диалог после перезапуска агента с продолжением | В проверенных версиях новый процесс перечитал общие и проектные правила. История диалога сохранилась, включая прежние инструкции. |
| Новый диалог при новом запуске агента после сохранения | Агент читает актуальные общие и применимые проектные правила для рабочей папки запуска. Проверенные версии передали оба обновлённых набора в запрос модели. |

Для повторного чтения обоих наборов в текущем диалоге перезапустите агента с продолжением этого диалога.
Обычное следующее сообщение и повторное открытие окна редактора не заменяют такой перезапуск.
Не нужно удалять историю или создавать новый диалог ради продолжения текущего.
Старые инструкции остаются в истории; повторное чтение не означает очистку контекста.

Новый диалог должен запускаться в нужной рабочей папке и с тем же `CODEX_HOME`, который редактировался.
Выбор папки в редакторе не меняет рабочую папку запуска. Непустой `AGENTS.override.md` по-прежнему перекрывает `AGENTS.md` своего каталога.
Один выбор нового диалога не доказывает перезапуск процесса; проверка ниже относится к новому процессу и новому диалогу.
Для другой версии CLI обновление правил без перезапуска не подтверждено.

`LEARNING.md` применяется иначе: агент читает его по указанию в инструкциях проекта.
После изменения памяти можно явно попросить агента перечитать файл по точному пути.
Сохранение памяти само по себе не запускает чтение, запрос модели или обучение.

## When edits apply

Save and wait for acknowledgement first. Saving, importing and restoring update a file;
they do not restart an agent or send a separate instruction message. An editor draft
has no effect until saved. An executing request keeps its existing instructions.
Shared rules may refresh on the next turn of the current conversation; project
rules did not refresh on that path in the tested versions. To reread both sets,
restart the agent and resume the same conversation. Its history, including older
instructions, remains. A new agent process starting a fresh conversation after
saving reads current shared and applicable project rules for its launch directory.
Use the intended working directory and the edited CODEX_HOME; choosing a folder in
the editor does not change the launch directory or override precedence. Selecting
a new conversation alone does not establish a process restart. Live refresh in
other CLI versions is unverified. LEARNING.md is read when project instructions
direct it; explicitly request a reread by exact path after editing memory if needed.
Saving memory does not itself start reading, a model request or training.

The save notice says:

> Сохранено. Выполняющийся запрос не изменяется. Общие правила могут примениться уже к следующему запросу. Для повторного чтения общих и проектных правил перезапустите агента с продолжением диалога.

As recorded in the [test12 validation report](VALIDATION-test12-agent-instructions.md),
real Codex **0.163.0 and 0.164.0** app-server processes were exercised with isolated
CODEX_HOME/project fixtures and a local fake Responses provider. The actual file
editor wrote marker changes; captured outgoing model requests showed:

| Scenario | Shared marker | Project marker |
| --- | --- | --- |
| First process, initial thread | original | original |
| Same thread/process, next request after edits | updated | original |
| New process resuming the saved thread | updated, with older history retained | updated, with older history retained |
| New process, fresh thread | updated | updated |

This proves instruction delivery by those CLI versions, not semantic obedience
by a real model or every future CLI version. A running request is not rewritten.
Saving files does not depend on CLI version or sign-in. Diagnostics obtains the
actual executable/version through the selected environment's PATH. Unknown or
missing versions remain editable and are labeled unverified for live reloading.
The existing missing-only bootstrap preserves an installed Codex instead of
replacing it with the pinned fallback. Nothing restarts a user's working chat.

Official references: [instruction discovery](https://learn.chatgpt.com/docs/agent-configuration/agents-md),
[configuration](https://learn.chatgpt.com/docs/config-file/config-reference),
[app-server protocol](https://learn.chatgpt.com/docs/app-server). Version-specific
local evidence takes precedence over assuming live refresh from general docs.

## Implementation and remaining checks

`AgentInstructionsUi` is a global-settings dialog; `AgentInstructions` stages the
small versioned helper/template assets in the private Codex control mount and
runs bounded, task-owned requests in Ubuntu AI. `agent-instructions.py` performs
source resolution, history, conflict detection and exact file updates. The existing
Codex sockets, RPC, provider adapters, connection wrapper, permissions and workspace
schema are retained. Only Codex Ubuntu AI adds first-run instruction preparation.

Automated storage/UI fixtures are separate from phone acceptance. Check keyboard,
scrolling, long text/rotation, actual file visibility, and persistence after app
restart on the phone. Do not infer those from mocked View tests. Installed session
and user environment state were not cleared to simulate a clean installation.

## Создание инструкций проекта / Create project instructions

Во вкладке **Проектные** на месте общего предпросмотра показана кнопка
**Создание инструкций проекта**. Существующий AGENTS.md (Codex) или CLAUDE.md
(Claude Code) открывается для редактирования. Текущий черновик того же файла
сохраняется. При смене файла действует обычная защита несохранённого текста.
Для отсутствующего файла форма предлагает назначение, пути, ограничения,
проверки, документацию и расположение LEARNING.md. Все поля необязательны;
пустые разделы не включаются. Неизвестные сведения и общие Ubuntu AI правила
не подставляются. **Просмотреть** показывает текст и точный путь выбранного
проекта; только **Создать и сохранить** записывает файл с проверкой редакции
и атомарной записью. Конфликт сохраняет форму. Выход с введёнными полями
требует отказа от черновика. LEARNING.md и дополнительные документы не создаются.

On **Project**, **Create project instructions** occupies the General preview's
position. Existing AGENTS.md (Codex) or CLAUDE.md (Claude Code) opens for editing;
the same file's current draft is retained. Switching files uses the draft guard.
For an absent file, optional fields cover purpose, paths, constraints, verification,
documentation and LEARNING.md location. Empty sections are omitted; no unknown
facts or shared Ubuntu AI rules are inserted. **Review** shows the generated text
and exact project destination. Only **Create and save** performs the existing
atomic revision-checked write. Conflicts retain the form; closing a populated
form requires explicitly discarding it. No memory or additional documents are created.


### Project-creation loading and repeated taps (test39)

RU: Одно нажатие «Создание инструкций проекта» во время загрузки запоминается
для выбранных агента, среды и проекта. После чтения открывается одна форма либо
существующий файл в редакторе. Повторное нажатие не создаёт вторую форму и не
очищает введённые поля. Загрузка и ошибки видны в редакторе; при ошибке можно
повторить действие. Несохранённый текст требует явного решения. Открытие формы
не создаёт файл; просмотр точного пути и сохранение используют прежнюю проверку
редакции и атомарную запись. Это относится к настройкам и входу из сессии.

EN: A Create project instructions tap during loading queues one request bound to
the selected agent, environment and project. After reading, the editor opens one
form or retains the existing file for editing. Repeated taps reuse the open form
and preserve its fields. Loading/errors remain visible and failures can be retried.
Draft confirmation, exact-path review, revision checks and atomic save stay in place;
opening never creates files. Both settings and session entry use this editor.

The old early tap was silently discarded by the busy guard; there was also no
reference preventing a second creation dialog. Tests reproduce the former on
the pre-fix source and cover the corrected flow without an artificial delay.
Phone flicker/opening acceptance remains separate from these model checks.


### Existing versus missing project file (after test39)

RU: Кнопка зависит от наличия основного файла проекта. «Редактировать AGENTS.md»
или «Редактировать CLAUDE.md» открывает существующий файл в текущем редакторе,
переводит к полю и показывает клавиатуру. Черновик и выделение сохраняются.
При отсутствии файла остаётся «Создание инструкций проекта» с отдельной формой.
Это правило действует и при выборе локального/приоритетного файла в соседней
плашке: кнопка относится к основному AGENTS.md/CLAUDE.md выбранного проекта.

EN: The action reflects the project's base file. Edit AGENTS.md / Edit CLAUDE.md
opens the existing base in the current editor and focuses the field/keyboard,
keeping draft and selection. A missing base retains Create project instructions
and its separate form. Selecting an override/local file does not change which
base that action targets. Both settings and session entry use this behavior.

The reported no-window Codex case was confirmed on the developer's video: the
base file already existed and was displayed. The misleading Create label and
extra status-row resize are corrected; no new diagnostic menu or artificial delay
is used. [Read-only reproduction](DIAGNOSTICS.md#открытие-проектных-инструкций--opening-project-instructions).
