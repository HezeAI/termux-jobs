# Instruction refresh and session instruction/skill access

Historical source-check report. These changes were subsequently packaged in [test35](VALIDATION-test35-agent-resources.md); the results below describe the earlier source-only checkpoint.

Date:2026-11-02. **Source changes after test34**, not included in the previously delivered test34 APK. No new APK packaging is part of this task; versionName/versionCode are unchanged in source pending the next requested build.

## Result

- General and Project instruction tabs have a vector Refresh control at the right of the fixed window title. Its48dp target, RU/EN content description and tooltip remain available above scrolling content. It uses the original selected-file reload and dirty-draft confirmation. Diagnostics hides it.
- Reload file and Addition are removed from the instruction menu. History, previous revision and localized import remain. General template preview/application and project creation remain in their respective tabs.
- Codex session gears show AGENTS.md & SKILL.md; Claude Code gears show CLAUDE.md & SKILL.md immediately after Restart. Their session context is captured when the gear menu opens. The resource window shows the source agent, environment binding and working folder; subsequent chat/profile changes do not retarget it.
- Instructions opens the existing Editor on Project, including General, diagnostics, editing/copying, save, history/import and absent-file creation. Skills opens the existing Screen with General/Project and every existing action. No second editor, skill store or installer is introduced. General settings retain those same managers and backend.
- Ubuntu AI, legacy Ubuntu and native-Termux Codex keep their bindings. Unsupported environments are refused; unsupported agents have no misleading Codex/Claude entry. Existing Claude-in-Termux limitations remain explicit without redirecting.
- Skill creation preview explicitly includes scope and exact SKILL.md destination. Import/update previews retain scope, destination and revision checks. Files are not described as applied instructions or discovered skills solely because they exist. No session restarts or model calls are added.

## Checked anew

Targeted JVM checks cover the editor, instruction transport, skills manager, shared layout, ordinary/agent session menus and the new resource window. Coverage includes RU/EN labels, title refresh/cancel/discard and selected override/local file retention; General versus Project entry; session snapshots after source mutation; correct agent/environment/project requests; save/copy and General access; absent-file creation offer; unsupported-agent refusal; actual resource-window button dispatch to existing managers; GitHub/folder/ZIP preparation and creation; scope switching; install/update/rollback/delete request routing and confirmation paths.

The skills UI uses an isolated backend. File-picker completion is represented by the existing prepare handoff with selected paths; these model checks do not claim Android picker taps or live GitHub availability. The real storage helper is covered separately by95 Python fixture tests for skills, instructions and Claude semantics. These exercise folder/ZIP/GitHub fixtures, staged/atomic writes, conflicts, history and validation. Eight Lint-report infrastructure tests also passed.

Full modern Lint7.4.2 analyzes all five modules as a conservative static check for the common menu/UI changes. Compilation and document-link/diff checks are separate gates. Results: **90 unique JVM cases passed**, including the corrected localized-menu check; the final refresh/menu pair was rerun after the SDK guard removal. **95 Python helper tests +8 infrastructure tests passed. Lint:0 errors, 234 warnings**. Production bytes in both build/analysis stages match source; summaries retain the initial failed assertion separately.

## Reused / unchanged scope

Existing local toolchains and dependency caches were reused. The file helpers and instruction templates, environment/component recipes, process ownership, agent protocols, runtime/native/X11 source and user-data formats are unchanged. No installation, desktop/driver run, working-session stop or authorization-file inspection was required. No APK audit or packaging result is claimed for these source-only changes.

After the initial JVM run, one stale localized menu-title expectation was corrected and rerun. The final refresh/menu checks were repeated after removing a redundant API guard: the project's minimum Android API26 already supports tooltips. Other passing same-task checks retain their result; the changed statement affects only that header setup, with no change to transport, storage, hub or skills paths.

## Not checked / device acceptance

Phone taps, the actual Android file picker, clipboard/keyboard interaction, narrow/large-font appearance and fixed-header behavior under the real IME remain device checks. Robolectric geometry and mocked backend dispatch do not prove them. Native CLI discovery/application is not retested by opening these windows; existing version-specific reload/restart guidance and conservative unconfirmed status remain in effect. Working agents are not inspected or restarted.

## Evidence and initial failures

`.verification/instruction-navigation/` contains scope, source identities/delta versus test34, JVM/Python/Lint logs and summaries. The initial JVM failure expected the old Revisions and template title; the UI now correctly says Revisions and import. The assertion was updated and the affected check rerun. The first Lint pass identified the unnecessary SDK26 guard; it was removed rather than suppressed, and Lint rerun. Earlier logs remain as historical evidence.
