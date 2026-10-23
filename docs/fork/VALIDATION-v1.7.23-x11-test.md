# Private X11 follow-up · 1.7.23-x11-test / code1238

Public stable remains v1.7.5. Compatible package/signing identity, code1237 ->1238;
no reset, migration, removed user data or replacement environment. Existing X11
service launches from older APKs have no owner identity: stop/restart the graphical
desktop once after updating to exercise the new ownership behavior.

## Scope and lifecycle review

Closing the launching shell session now stops its desktop via the existing private
service/owned guardian path. Exact session ID + handle + environment matching
prevents a stale or unrelated close from stopping a different desktop. A second
viewer does not take ownership; closing the owner closes its graphical programs
for all viewers. Explicit close warns about unsaved graphical work. Normal shell
exit also requests stop; switching tabs or Return to terminal does not.
Pending unsent launches are cleared when their owner closes; sent requests stay
ordered on the existing single executor. Failure to confirm close is reported,
not silently treated as success. No arbitrary PID or new process-kill mechanism.

Manual observation measures popup content updates through optional XDamage. At most
128 owned damage regions, bounded event drain and one-second log rate. Initial
attachment counts as an update; no pixels are copied/read. On missing extension
window/focus observation remains operational with damage=unavailable.
Pointer position changes are counted internally; coordinates, input text and raw
window titles/classes are not emitted. Adapter start/current counters count input
dispatch and viewport callbacks. No additional animation or input injection.
The native library, compositor setting and graphics backend remain unchanged.
Agent chat extra-key state now belongs to each chat owner (bounded32-entry memory),
and synchronization hides the shared terminal toolbar even if visibility did not
transition. Disabled top buttons no longer inherit the ordinary toolbar highlight.
Native agent permissions/transports remain unchanged. Shared chat viewport changes
are coalesced and settled after layout, before drawing, so a newly inserted/growing
row is not painted at the previous scroll position for one frame. User touch is observed before selectable children receive it and cancels
pending automatic following; saved/older history preserves its anchor. Focus is never
moved by fullScroll. Message text is still received in native chunks; no typewriter
animation or higher polling frequency is introduced. Applies to all four agents.
Developer recording Screenrecorder-20261020-184239.mp4 was inspected via sampled
frames: input stayed fixed and text arrived in chunks. Developer clarified new text
appears below the Shell boundary before rising into view. Inspection found posted
scroll adjustments in the unchanged baseline chat implementation; this ordering
can paint a frame before adjustment. Exact device timing and resolution still need
visual comparison; sampled frames alone do not prove complete causal attribution.

## Evidence and limits

Developer report3406079304740189105 confirms visible flicker during30 seconds with
unchanged recorded root window lifecycle/focus/background. It does not establish
GTK repaint, compositor or pointer motion as cause: prior recorder lacked those
measurements. New source/build checks and final artifact evidence follow below.

Device validation: restart graphical desktop after update, record stationary
Applications flicker for10–15seconds, stop and export. Check tab changes preserve
work, closing an unrelated session preserves desktop, closing launching session
stops graphical programs, reopening that session gives a new desktop launch.
Save graphical work before close. Real keyboard/gestures/visual behavior remain
device checks, not established by mocked tests. No GitHub publication requested.


Additional phone check: enable extra keys in a terminal, switch to agent chat and
back, switch between agents, then open the robot terminal and check its own keys.
No extra row should transfer into structured chat or show a false active button.


Chat phone check: send a message and watch both the new user row and streamed reply/
action cards at the Shell boundary. Scroll upward while the agent answers and type
a draft; history position, keyboard focus and draft must remain stable. Try an
Earlier messages load and switch away/back. These checks apply to Codex, Gemini,
Claude and OpenCode because the shared transcript owns this adjustment.


## Automated verification

63 Python/shell tests passed, including optional XDamage failure, bounded owned
region lifecycle, stale damage event rejection/rearm, pointer-coordinate privacy
and previous controller/guardian/wallpaper/environment checks.
The wider168-test JVM selection passed all X11/diagnostics/ownership tests and
exposed a failing new chat touch-cancellation scenario: selectable text could
bypass the parent OnTouchListener. The handler was moved to dispatchTouchEvent
without intercepting selection. Final focused43 JVM tests passed (0 failures),
covering all AgentChat suites and desktop header after the final chat change.
Includes first-paint tail placement for user/text/tool rows without posted task
execution, draft/focus retention, manual touch cancellation, cached/older history,
extra-key isolation and repeated shared-toolbar hiding. Ownership tests cover
wrong session/environment, stale terminal handle, invalid owner, and cancellation
of a launch still awaiting Binder connection.
Evidence: .verification/x11-content-observation/script-tests.log,
jvm-before-touch-fix.log and jvm-final-chat-tests.log.

Release assembly completed successfully. Existing Kotlin metadata/Lint loader
warnings remain in the build log; a clean Lint run is not claimed. Packaged DEX
contains13,369 defined classes; required definitions verified,31 reviewed optional
missing types and no unreviewed types. Final artifact audit follows below.


## Final artifact audit

APK: Termux-Jobs-v1.7.23-x11-test-arm64.apk (38978973 bytes). ZIP integrity/no duplicate members,
package com.termux, version1.7.23-x11-test/code1238, ARM64, minSdk26 and
non-debuggable manifest verified. APK v2 signature valid, unchanged certificate:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
SHA-256: `48b04befc42e53b4b0b680961178fc7c9acb2bc21340237a24afdcf7091b4e78`.
Pinned X11 native and keyboard assets unchanged; compiled JNI contracts28/1/5
verified. Baseline native/assets comparison passed with expected X11/runtime
additions. Sensitive filename and known private key/token marker scans passed
(not a proof that every conceivable secret pattern is absent). Destination
Download copy hash verified. Evidence: apk-audit.json, signature.txt,
badging.txt, build.log in .verification/x11-content-observation.
