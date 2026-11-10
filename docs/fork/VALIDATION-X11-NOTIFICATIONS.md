# X11 notification lifecycle — 2.1.8-test1 / 1537

This private candidate follows published 2.1.7. No APK has been installed or
published by the assistant. Phone observation and fixture checks are distinct.

## Phone observation

A read-only Android probe ran as the application's UID 10620 through an isolated
TermuxService background task. ActivityManager reported:

- `com.termux:jobs_x11_archlinux`, PID 7631, foreground service
  `com.termux.x11.X11Service$ArchLinux`, zero connected clients.
- Notification ID 8086: `Termux Job’s · X11 · archlinux`, text
  `Проверочная графическая сессия`.
- The normal TermuxService notification ID 1337 still had the upstream `Termux`
  title. Its session/task count changed normally during the diagnostic task.

Thus a live Android service backs the X11 notification; it is not merely a
notification entry left after the service disappeared. Both expected filesystem
X105 sockets refused connection with ENOENT. No authenticated X11 connection,
window command or display modification was attempted; the authority file was not
read. These observations do not prove whether a desktop child is still alive.

The notification's old wording was unconditional, including production desktops.
It does **not** establish test-session ownership. Android `peekService` returned
no published binder; a standalone app-process bind was rejected because its
application thread is not registered as the app. `/proc` did not expose the
reported Android PIDs, even from a native same-UID task. Consequently a start-time
identity and the internal desktop owner could not be verified. **No existing
X11 or other working process was stopped, and no notification was manually
cancelled.** The existing phone session still requires user-controlled inspection
or an installed, tested correction; this report does not claim it was cleaned up.

Probe evidence and source: `.verification/x11-notification-audit/` in the working
checkout (`android-state.json`, `native-probe.json`, `NotificationProbe.java`).
The initial standalone Java probe was killed before producing evidence when it
inherited the shell library environment. Removing LD_PRELOAD/LD_LIBRARY_PATH
allowed the bounded probe to finish; the exact killing mechanism was not proved.
The old D8 also failed on the new SDK; the existing local R8 8.9.35 compiled the
final diagnostic helper. No system package, SDK or application was replaced.

## Confirmed source issue and correction

`onStartCommand(controlOnly=true)` published a foreground notification before
component checks/program operations. Completion, errors and detachment of the
last UI client had no idle-notification/service cleanup. A controller can therefore
remain alive with a misleading desktop notification even without a running desktop.
This is a reproducible code path, not a claim that the inaccessible live service's
internal state was independently read.

The correction checks actual work before retaining the notification. A component
check/install/cancellation, program operation, preparing/running native desktop or
pending child cleanup retains foreground status. An idle service hides its own
notification; when its last client is gone, it uses the existing private-process
shutdown path. Its exact process-name/UID-context guard is preserved. No PID from
notification text or saved profiles is used for termination.

A working desktop survives UI detachment/rebinding. Desktop exit/startup error
keeps the existing bounded delivery interval before its own shutdown. A null
restart intent does not create an unintended native test server.

User-visible wording changes:

- X11 notification body: `Рабочий стол` / `Desktop`.
- Normal service notification title: the application's `application_name`
  resource (`Termux Job’s`), instead of the upstream protocol/name constant.

## Verification scope

Lifecycle fixtures exercise the real service with mocked signal delivery and no
native graphics/package installation. They cover idle control-only startup,
completion/error/cancellation, UI detachment/rebinding, own-notification cleanup,
null restart intents, process-identity mismatch and active program work.
Existing X11 integration/ownership tests cover desktop exit and startup failure.
The notification action test also checks the branded title and unchanged targets.

The first fixture run failed because the custom service shadow did not inherit
ShadowService and its cleanup used an unsupported scheduler API. Those are test
harness errors, not evidence of a product regression; the original log/XML remain.

Checked anew on 2026-11-09:

- Regression reproduction against the original lifecycle: 9 new tests, 5 failures,
  4 passes, no errors/skips (`before-repro.xml`, `before-repro.log`).
- Corrected sources: **63 tests passed**, no failures/errors/skips: 9 notification
  lifecycle, 41 X11 integration, 2 component lifecycle, 9 desktop ownership,
  2 notification locale/action tests. Gradle exit 0 (`after-tests.log`).
- Full Modern Lint 7.4.2: all five modules analyzed, **0 errors, 290 warnings**,
  exit 0, no infrastructure diagnostics. Final X11Service source matches the
  analyzed staging copy. Compile SDK beyond AGP tested range warning remains.
- Workspace locale validation: 587 strings; diff whitespace check passed.

The corrected fixture also had a compilation-only retry: ShadowProcess exposes
final static killProcess, so the signal interception uses its own shadow class.
No real signal was sent by these fixtures. Test-only Gradle includes restrict
class scanning to the five named classes; production build settings are unchanged.
Results are local Robolectric checks, not Android service lifecycle acceptance.

Phone UI acceptance of the modified app, real Android process recreation, and
cleanup of the reported existing service are **not checked**. No APK was installed,
no new release was made, and working sessions/environments were not stopped.

## Private APK preparation

Version 2.1.8-test1 / 1537, package com.termux. The version code increases from
stable 2.1.7 / 1536. No storage format, migration or reset was changed.
Actual installation/update and desktop behavior on the phone remain pending.

Reused checks: the 63 passing tests cover identical 811 Java/XML source inputs;
Modern Lint covers identical 505 production Java/XML inputs. Only version metadata
and release documentation changed after those checks. The first comparison also
included a later test-shadow edit in the Lint stage; production inputs match.
Localization was checked again (587 workspace strings).

Unchanged native libraries are reused from the verified stable APK. Native sources
and build recipes match v2.1.7; library hashes are checked during packaging.
This candidate is not a fresh native rebuild or a new reproduction experiment.
Build recipe, input identities and artifact checks: `.verification/x11-notification-audit/apk/`.

Phone checklist after manual installation:

1. Normal terminal notification shows Termux Job’s; X11 shows Рабочий стол.
2. A desktop component check that finishes/errors/cancels leaves no idle notification
   after leaving the screen.
3. An intentionally running desktop remains active when the app UI is closed/reopened.
4. Stopping a desktop removes its notification and preserves other sessions.
5. Reopening the app does not create an unintended desktop.

The old Arch service was not stopped: its test ownership remains unconfirmed.
An Android package update can terminate app processes; finish work before installing.

## Final artifact — checked anew

- Signed release variant: **2.1.8-test1 / 1537**, `com.termux`, ARM64 only,
  not debuggable. Size: **40,675,545 bytes**.
- SHA-256: `e403a25184b5f2d778415c62235317425f92f171357a609acfbf9d6b2b5d6e74`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`, same as 2.1.7.
- ZIP, APK signature v2, runtime DEX references, 638 staged production files,
  packaged assets and seven ARM64 ELF files checked. No stale outer ZIP entries.
- X11 title and lifecycle markers found in DEX; old notification body absent.
- RTK public component pin unchanged; no embedded test RTK archive.
- Download copy matches final APK hash. No APK installed or release published.

File: `/sdcard/Download/Termux-Jobs-v2.1.8-test1-arm64.apk`.
