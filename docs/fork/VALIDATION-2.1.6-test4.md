# 2.1.6-test4 /1525 — sidebar cards and chat presentation

Private test build. No automatic installation, publication, model requests or
working-session restarts. Same com.termux/signing identity/schema as test3/1524.
Evidence: `.verification/sidebar-cards/`. Final identity is recorded in artifact.json.

## Implemented behavior

Four cards preserve the existing item order and callbacks: terminal sessions,
agent chats, servers, application. The approved local HTML Cards variant supplies
background #1a1e1c, edge #303b34,15dp corners,10dp spacing and tinted headers/icons.
Active rows use #25372b/#4e6956. The Android menu retains its established content
scale and width cap; the mockup is not treated as a complete list of functions.

Section choices use independent stable SharedPreferences keys. Opening applies
saved choices plus an opening-only override for the on-screen session. It does
not persist the override or open every background session. Manual collapse remains
through status updates. Returning from a child file window preserves the current
menu state; a new explicit opening applies the active override again. The active
session's nested actions and highlighting remain available.

User text uses a stable content-sized surface aligned right, bounded to85% of the
available width on every measure. Long text wraps; selection/copy and full-text
access remain. Delivery updates reuse the message text view. Attachment cards and
original-opening callbacks remain unchanged.

New messages/action groups use520ms,10dp upward movement from below, .985 initial
scale with a small overshoot, and opacity. Existing history/hidden-tab restoration
does not replay arrivals. Logical action groups reuse their view across rekeying.
Timeline height changes have a separate240ms transition; updating a group does
not reset its arrival animator. Reading anchors and bottom-follow behavior are
preserved; a manual gesture/inertia takes precedence. System-disabled animations
use final positions immediately.

The default ScrollView scrollbar is disabled. A decorative thumb is driven only
by drag/wheel scroll events or native inertial scroll frames. It waits300ms after
the last manual movement then fades over220ms. Content/layout/scrollTo updates
neither display it nor extend its deadline. Hiding/detaching the chat clears it.

## Verification scope

Local JVM checks cover sidebar preferences/recreation/active overrides/manual
collapse and action routing; narrow/enlarged text measurements; short/long/unbroken
bubble text; selection/delivery; arrival progress, group updates/rekeying, history,
scroll anchors, manual input and indicator timeout. Full Modern Lint and the final
signed APK gate are required separately. Native inputs reused only after hashing.
Initial legacy sidebar test assumed the section header was a direct body child;
its expectation was updated for the requested card wrapper. Original logs retained. The initial gesture fixture had a zero-height plain child
inside ScrollView (confirmed by diagnostic geometry); it was corrected to have
a3000px intrinsic height. This fixture failure was not counted as a passing gesture
check or attributed to the production scroll implementation.

**Device visual checks not performed.** No APK was installed. Smoothness, bounce,
rapid event arrival, thumb position/fading, selection handles, actual enlarged-font
layout and lifecycle restoration need acceptance on the phone. JVM/layout tests
are not device evidence. No claim of visual approval or universal smoothness.

Suggested phone pass: collapse all four sections; reopen with a chat, then terminal,
then no active terminal screen. Manually collapse the active section and wait for
status changes. Restart the app and verify inactive choices. Send a short message,
multiline text and a long unbroken string; attach an image. While actions update,
read above the end and then return to the end. Verify only manual drag/inertia
shows the thumb and that arrivals do not replay when switching terminal/chat.

## Final results

- 137 unique targeted JVM tests pass across the broad and focused rechecks;
  see java-results.json for the exact evidence per class. This is not the full
  project suite rerun. The final gesture/persistence fixtures pass.
- Full Modern Lint:0 errors,289 warnings, all five modules. Final Java/XML inputs
  were compared with the analysis stage. Legacy lintVital still reports its known
  Kotlin-metadata compatibility diagnostics; it is not the Modern Lint result.
- Reused after checks:95 native build inputs, JNI payloads and unchanged agent/RTK
  assets from1524. No new model/agent or GPU execution test is claimed.
- Final ZIP, package/version/ABI, signer, runtime DEX, native/assets and source
  equality passed. Signature matches1524; versionCode increases to1525.
- APK: `/sdcard/Download/Termux-Jobs-v2.1.6-test4-arm64.apk` (40662521 bytes).
- SHA-256: `33fbc4e699f234baf1bbed25455a73b8d2faada606b8f1fa5e8de9ccf96ae316`.
- Not installed or published. Phone visual acceptance remains pending.
