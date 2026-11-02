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

The global settings menu contains **Agent instructions → Codex → Ubuntu AI**.
It edits files inside that installed environment. It is shared by its projects,
not a per-chat prompt. Other agents and ordinary Ubuntu/Termux targets are not
changed by this first integration. A chat supplies only a suggested project path.

## Sources and precedence

The helper enters Ubuntu AI, sources its Bash initialization as the normal
session does, and resolves its actual `CODEX_HOME` (default `~/.codex`). General
edits target the first non-empty global `AGENTS.override.md`, otherwise AGENTS.md.
No fixed `/root/.codex/AGENTS.md` is assumed. The selected path is displayed.

Project offers AGENTS.md and AGENTS.override.md in the chosen directory. The
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

## Importing instruction text

General's menu offers **Import instructions**; Project offers **Import AGENTS.md**.
Both use the existing built-in environment/phone-storage file dialog, not Android's
system picker. The existing file reader accepts regular, unlinked UTF-8 text up to
256KiB, rejects binary/malformed text and detects source changes during reading.
If the editor has a draft, Save / Discard / Cancel comes first; a failed draft save
does not proceed to the picker. Cancelling a preview does not modify the target.

The preview shows unmodified content and the exact resolved destination. General
imports into the currently active source in the actual CODEX_HOME and explicitly
labels an active AGENTS.override.md. Project always imports into AGENTS.md in the
selected folder, including when its override was the edit target. An active project
override is retained, with a warning that it continues to mask AGENTS.md. These
operations do not change CODEX_HOME, configuration, authorization or other projects.

Only **Import and save** writes. It uses the same revision-checked atomic backend
and previous-version history, so stale source/target changes refuse replacement.
After success the editor/source state and normal application-timing notice refresh.
No conversation is restarted. The source reader is task-owned and is closed with
the editor; late callbacks cannot save or reopen a dismissed editor. History records
and imported/user text are not translated when changing the interface language.

The storage-section addition remains in General's menu, with its RU/EN preview;
the duplicate main-screen offer has been removed. The full-template review offer
remains separate.

## When edits apply

The save notice says:

> Сохранено. Выполняющийся запрос не изменяется. Общие правила могут примениться уже к следующему запросу. Для повторного чтения общих и проектных правил перезапустите агента с продолжением диалога.

Real Codex **0.163.0 and 0.164.0** app-server processes were exercised with isolated
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
