# Private test20 — 1515 — saved chat closure and input history

Date: 2026-11-07. com.termux, ARM64, same signer/schema8/min26/target28.
No migration, install, publishing, native-code change, X11 start, or user-session stop.

## Changes and traced dependencies

Saved agent profiles get No/Yes, with the existing finishClose persistence and
captured-run checks. No cancels without changing profile or stopping anything.
Yes retains keepHistory and saved=true; retirement clears only the live handle.
No repeated bindChatForSave or offer to save an already-saved chat. Unsaved agents
keep both original options and the native-thread binding callback.
The shared path covers Codex, Claude, Gemini and OpenCode.

Programs removes its neutral Documentation button; sidebar environment docs and
all program callbacks remain unchanged.

The input-history clear handler already existed but was conditional on nonempty
recentCommands. It is now always present at the same location above search and
is disabled when empty. It keeps confirmation, clears only recentCommands, and
uses renderContent → renderLibraryWindow to update the open sheet immediately.
The confirmation now names input history. Store format and clearRecentCommands
are unchanged; commands, aliases, notes and conversation files are independent.

Sidebar rename follow-up: settings already calls refresh after successful save,
but WorkspaceSidebar captured a fixed String title. The controller now supplies
`() -> p.name`; refresh reads that value for visible title, tooltip/accessibility
and the ellipsis description. Existing String overloads remain compatible.
Rows/disclosure are not rebuilt. Added regression covers all four agents and
ordinary terminals, unchanged row identity, expanded branch and current session.
Final tests, lint and APK are rerun for this added scope. Earlier successful
build before this follow-up is not delivered.

## Verification

Evidence directory: `.verification/test-2.1.5-test20/`.
Native termination/provider APIs are intercepted by WorkspaceRestartUiTest;
synthetic saved records, native thread IDs and transcript bytes are checked.
Real native agent reopening and phone layout are separate acceptance scenarios.

### Checked anew

- Initial targeted JVM/UI run: 68 tests, zero failures/errors/skips.
- Full modern Lint 7.4.2: zero errors, 279 warnings, all five modules; no loading failures.
  Existing compileSDK35 beyond AGP tested range warning retained.
- Enhanced populated-library history suite: 14 tests, zero failures/errors/skips;
  cancellation retains history, confirmation clears entries/count immediately,
  populated commands/aliases/notes and session data remain byte-equivalent in serialization.
- Final rename/sidebar suite: 23 tests, zero failures/errors/skips.
  Covers all four agent names and ordinary terminal, visible label/accessibility,
  same row/expanded branch and selected session. Earlier tests remain evidence for
  unchanged programs/storage code; final close/history tests ran again with rename.
- Final modern Lint rerun after rename: zero errors, 279 warnings, all five modules.
- APK ZIP/signature v2/certificate, com.termux/version1515/test20, ARM64-only,
  debuggable=false, runtime/DEX/assets/notices/private-data checks passed.
  620 production files match packaged staging; seven ELF files match stable2.1.4.
- Legacy lintVital during AGP4.2 packaging logs unsupported-platform/registry/Kotlin
  metadata diagnostics. It is not counted as successful static analysis; the separate
  compatible full modern Lint run supplies that check. APK assembly completed.
- git diff --check passes. No actual handset is connected via ADB.

Final APK size: 40620630 bytes. SHA-256:
`907dd8698ccfcc67ba946da8378577ecd1a64a548ed6ef8ff3c2dd11c40a8976`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

### Reused

All recorded native inputs were freshly compared against test10/native-inputs.json
and match. Existing three JNI payload hashes are verified before use; Java/resources
are rebuilt. This does not reuse device graphics or live-session test results.

## Device checks not performed by the assistant

No current device UI result is claimed for saved close, button positions,
programs/sidebar navigation, renaming or history clearing. No APK installed automatically.
Manual acceptance: No then Yes for a saved chat, reopen its history; compare an
unsaved chat; open Programs and sidebar docs; cancel then confirm input-history
clear and inspect count/empty state plus saved commands, aliases and notes.

User separately reported that both small and large synthetic Claude PNGs open
successfully on their phone. That report predates these UI changes and does not
establish the original failure's cause on the other phone.
