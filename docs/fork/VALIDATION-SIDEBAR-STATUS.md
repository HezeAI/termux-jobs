# Sidebar session state labels — 2026-11-10

Private candidate 2.1.8-test2 / 1538, based on 2.1.8-test1 / 1537.
The user reports already having the notification fixes from test1. New behavior
in test2 is limited to sidebar labels/colors. No automatic installation/publication.

## Behavior and source

WorkspaceController retains liveFor(profile) and TerminalSession.isRunning() as
the source of running state. Selected-row state only controls selection/disclosure.
The controller now supplies environment/agent and path without a presentation status.
WorkspaceSidebar appends one status and colors only that suffix using a text span.
Refresh, initial construction and saved-menu callbacks use the same update function.

- Terminals: Запущена / Не запущена; green / red respectively.
- Agent chats: Запущен / Не запущен; green / red respectively.
- Base terminal: Базовая / Base, neutral subtitle color per the later clarification.
- English running labels: Running / Not running.
- Saved marker in the title, name, path, icons, selected background and actions stay.
  Saved is no longer used as a terminal state after the path.

The change neither launches nor stops a process and does not change stored profiles.
A registered but exited terminal must still report not running. Opening or expanding
menu rows only reads the running supplier; it does not assign process state.

## Checks

Plan: compile and run SidebarTreeTest/SidebarSectionsTest, inspect spans and row
selection across state changes and menu reconstruction; verify controller output
for Codex, Claude, Gemini, OpenCode and terminals; RU/EN base label; localization,
modern Lint and diff review. No real agent or desktop execution is needed.

Test fixtures change a supplied running state; these verify UI behavior rather than
starting user processes. Phone rendering and real start/stop acceptance remain
unverified. The earlier notification tests do not validate this new presentation.

Evidence: `.verification/sidebar-status/` in the checkout.
Checked anew: 9 JVM tests passed (7 SidebarTreeTest, 2 SidebarSectionsTest),
zero failures/errors/skips. RU/EN localization check passed (587 workspace strings).
The first run preceded the Base clarification; the final run includes it.
Production compile and resource processing succeeded. Modern Lint 7.4.2 passed all five modules: 0 errors, 290 warnings, exit 0.
Final production sources match both tested and analyzed staging copies.
The existing compileSdk-beyond-AGP-tested-range warning remains.

Review: process-state lookup and launch/stop handlers are unchanged. renderTabs
refreshes the existing menu; constructing a menu reads the same live supplier.
Selected state changes only background/disclosure. No persisted fields changed.
Other tracked files preserve their pre-task hashes (preservation.json).


## Test APK preparation

Only release version metadata changed after the passing sidebar tests and Lint.
All 811 test-stage Java/XML inputs and 505 production Lint-stage inputs match.
Native source/build recipes match v2.1.7; unchanged native libraries reused with
verified hashes. This is not a fresh native reproduction or graphics test.
Final package/signature/ZIP/ABI/DEX/assets and Download-copy checks passed. Package and signer remain compatible; no data migration/reset added.

Phone checklist: check base label, running/stopped terminals and agents, selection
without a status change, refresh after stop/start, reopen the menu, and ensure
only the status words are green/red. Name/path/selected background should stay.

## Final APK

- Version **2.1.8-test2 / 1538**, com.termux, ARM64, release variant, not debuggable.
- Size **40,675,313 bytes**; certificate matches test1 and stable 2.1.7.
- SHA-256: `17d68e8b3e3682ff5888788291cee46a4d6f7741b9e6aebd7a98d22125fd24fc`.
- ZIP/signature/runtime checks passed; 638 production inputs match staging.
- Comparison to test1 production manifest: only WorkspaceController.java and
  WorkspaceSidebar.java differ. Version metadata also updated; seven ELF unchanged.
- DEX contains new state labels and setSessionText; Download copy matches hash.
- Legacy packaging Lint prints known Kotlin metadata incompatibility diagnostics;
  it is not counted as successful analysis. Separate Modern Lint 7.4.2 passed,
  with identical 505 production Java/XML inputs and 290 warnings.
- File: `/sdcard/Download/Termux-Jobs-v2.1.8-test2-arm64.apk`. No auto-install or publication.

## User acceptance — 2026-11-10

The user accepted test2 on the phone before requesting the action-card changes.
This is user-reported acceptance, not a new independently executed device matrix.
