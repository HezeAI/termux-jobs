# Private test29 — individual agent glyph animation

com.termux; 2.1.1-test29; versionCode1464. Active checkout:
`/root/termux-fork/.verification/rollback-live-actions/source`.
Includes the already delivered test28 work, including Agent skills and instruction
editors. The helper experiment and user-facing migration remain excluded.

## Change and scope

Only AgentStopButton's drawing changes: no flying point, orbit trail, interior
fall or bounce geometry. Codex has a smoothly rotating short arc and upright >_
glow; Claude has sequential ray illumination/gentle scale; Gemini has four fixed
lobes with moving illumination/gentle scale; OpenCode's fixed21-pixel OC never
vanishes while illumination advances. These are app-designed animations, not
claimed official agent animations. No glyph rotates over time.

Working, waiting, reconnecting, error and idle still come from the existing native
snapshot reducer. Local CLI readiness is not model connectivity. Waiting stops
individual motion and only breathes slowly; reconnect stays amber with existing
retry banner/details; error/idle are static. Reduced motion receives frame0 and
renders static cues. The existing Choreographer/cadence clock remains the sole
frame owner. No timers, polling, network calls, history writes or layout resizing
are added. Paint/path/bounds objects are cached; no per-frame layout recreation.

Stop request ownership and process signals are untouched. Request dots, terminal
attention, transfer borders and ordinary terminal/SSH lines stay independent.

## Checked anew

- Relevant model suite: AgentChatUi, WorkspaceActivityRoute, AgentIndicatorState,
  AgentProgressView, SessionActivityUi and ServerTransferUi. Covers all four agents,
  five states, bounded/continuous brightness and scale, several button sizes,
  unchanged hit area, native-style accessibility class, hidden/window-gone frame
  cancellation and return to current state, 60Hz cadence and static animation-off.
- AgentChatUi checks the four-agent stop flow: left Yes/right No, cancellation,
  no duplicate dialogs, completion while open, stale request token and changing
  chats. No real process signals are used by these mocked transport tests.
- Added four-agent terminal attention test: requiresTerminal → active outline;
  minimize → no stop; reopen → outline active until the requirement clears.
- Existing request-dot test checks stable height, active/wait/reconnect/idle/error
  presentation and static caption. Existing transfer tests confirm hidden transfer
  progress and quick return, independent uploads/downloads and bounded callbacks.
- Fresh compile/resources, full existing modern Lint and exact APK checks are
  recorded below. They do not prove real device smoothness or touch usability.

## Reused

31 relevant launch/control/state/helper source files match recorded test28 hashes,
including AgentChatUi, AgentIndicatorState, AgentProgressView, WorkspaceActivityLine,
WorkspaceTransferBorder, WorkspaceController, WorkspaceServers, SessionShell and
native agent/helper assets. Native outputs and existing local SDK/JDK/dependency
caches are reused; final packaged native bytes are compared again. This is not a
new functional test of X11, drivers, environment installers or external networking.

No settings/session format or migration changed; user instructions are untouched.
Project AGENTS.md was measured:19,109 bytes, below the restructuring trigger.
Memory stays at `/root/termux-fork/LEARNING.md`; one confirmed fixture lesson was
added there, without creating another memory file.

## Initial failures and limits

The new frame test initially used an attached/shown Robolectric view whose window
remained GONE, preventing scheduling as intended. The fixture now explicitly models
window visibility and asserts the positive frame path before hiding/restoring it.
The terminal fixture initially closed its executor before the posted dialog-dismiss
callback drained, causing RejectedExecutionException between fixture iterations.
It now waits for normal dialog dismissal before disposing each fixture. No product
lifecycle fix is claimed from that fixture change. Logs/XML from failed attempts
are preserved and not counted as passes. Closing an owner simultaneously with a
queued dismiss is outside this animation change and not claimed verified on phone.

## Not checked

Phone smoothness, perceived brightness, glyph recognition at actual device density,
TalkBack speech/touch and navigation during real agent work require device acceptance.
Model tests do not prove CPU/power improvements. Working sessions, servers/tunnels,
installed environments, user auth and process ownership were not modified. No
external connection repair is claimed. No real desktop/install/transfer operations
were started for this visual change.

Evidence: `.verification/agent-icons-test29/`, scope plan, source identity comparisons,
JUnit summaries, original failures, build/Lint and final-file reports.

## Final results and artifact

- **69 relevant JVM/UI cases passed**, no failures/errors/skips. The final visibility-
  corrected route/static-motion suite was then rerun: **9 passed** (part of the69,
  not9 additional unique cases). It checks static animation-off for all four agents
  in an explicitly visible window. No APK rebuild was needed for that test-only edit.
- Full modern Lint7.4.2, all five modules: **0 errors, 235 warnings**, no infrastructure
  diagnostics. Existing SDK/plugin/Kotlin metadata toolchain warnings remain visible.
- Final APK ZIP, package/version, ARM64-only ABI, non-debuggable manifest, signature
  and expected single certificate passed. Runtime inventory:14,114 defined classes;
  31 unresolved types have explicit existing review notes, no missing required types.
- All114 packaged assets and all three native libraries match test28 byte for byte.
  No private auth/key filenames were found (inventory, not a universal secret scan).
  Download copy matches the verified file's SHA-256.

Download: `/sdcard/Download/Termux-Jobs-test29.apk`\
com.termux · 2.1.1-test29 · versionCode1464 · 40,009,725 bytes · ARM64\
SHA-256: `5035f5af67407607fdabda456bc1e63af6aeeba2ee9020dcf60b3157cc890e7b`\
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

VersionCode exceeds test28/1463 and previous reserves. Same package/signer and no
settings/data-schema changes; this does not substitute for device update acceptance.
