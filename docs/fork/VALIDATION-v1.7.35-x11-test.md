# Private v1.7.35-x11-test / code1250 · X11 lifecycle

Same package/signature, code1249 →1250. No data reset/migration, new rootfs, system
package installation or publication. Changes are confined to X11 service/controller
lifecycle, status and tests. Agent sessions, backup and package backends are retained.

## Findings and fix

Developer screenshots showed ended137 with instructions to stop X11 although no
desktop was visible. The completed/error callbacks left started=true; another target
was blocked while its menu did not expose the original target's Stop. This proves a
state-management defect, not the original cause of desktop exit. GLib faccessat
warnings do not by themselves prove the cause. Guardian deliberately kills its own
group and may return137; use the actual child exit marker when available.

Completion/startup error now retires the dedicated VM automatically. Readiness,
started and input focus clear immediately; new work is blocked during retirement.
Explicit Stop checks the target, acknowledgements drive UI hiding, and late callbacks
or a stale disconnected Binder cannot reactivate/clear the current desktop. Cancelling
preparation also locks startup. Terminal sessions and unrelated processes are not
stopped. Existing notification stop and session-close owner/handle checks remain.

## Verification boundaries

Isolated JVM fixtures cover all targets (Ubuntu, Ubuntu X11 Test, Debian, Alpine,
Termux): wrong-target stop rejection, stop acknowledgement, startup/cancel/end
transitions, automatic release scheduling, duplicate completion and late readiness.
Real guardian/routing-script tests use fake distro/DBus desktop binaries and an actual
process tree: EOF terminates the owned children, including TERM-resistant descendants,
while a separate sentinel survives. These tests do not install or stop real desktops.

A live device check is still needed: start/stop each desktop while keeping its terminal
open, then check/start the next environment. Check session-close, return-to-terminal
without stopping, and retry following a startup failure. No simultaneous independent
servers were added. No diagnosis of why the original desktop exited is claimed.

Final checks: **66 JVM +85 Python tests passed**. An initial JVM compilation attempt
used unavailable Mockito; it was replaced by the existing Robolectric shadow mechanism,
then the final suite passed. ARM64 release/runtime audit passed (13370 defined classes;
31 optional unresolved types with existing review notes). ZIP integrity, package/ABI,
versionCode1250/minSdk26, original signing certificate, pinned native/JNI28/1/5,
source assets, absent retired observers and limited private-marker scan verified.
Inherited Gradle/Kotlin/Lint metadata warnings remain; no clean Lint claim.

APK: `/sdcard/Download/Termux-Jobs-v1.7.35-x11-test-arm64.apk`

SHA-256: `14c24aa3d00afe9dc6027e24d6586f2793787fc8b963d17c156b6adba7e3ba64`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

Device behavior for this patch remains pending. Build and digest-verified delivery
change no installed application, user session, package or rootfs. Evidence is under
`.verification/x11-stop/`.
