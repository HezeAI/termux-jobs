# Private v1.7.25-x11-test / code1240 · LXQt comparison and GIO observation

Stable public remains v1.7.5. Same com.termux package and signing identity;
code1239 ->1240. No reset, history migration, data deletion or environment replacement.
This is a private APK; no GitHub publication or installed-app modification.

## Behavior and boundary

Only Ubuntu X11 Test (jobs-x11-ubuntu) gets the XFCE / LXQt · Openbox selector.
XFCE remains the default; a saved selection survives app restarts. Change only
when the desktop and component operation have stopped. The service independently
validates choice/target, rejects shell syntax and disallows switching a live desktop.
Readiness is cached per target + desktop; XFCE readiness cannot launch LXQt.

Both desktops share the same Test Ubuntu rootfs, packages, documents and application
configuration root. Installed applications do not need reinstalling. LXQt has its
own lxqt/session.conf; an absent file is seeded with Openbox only, existing edits
and XFCE settings are not overwritten. LXQt and XFCE never start together through
this launcher. LXQt readiness waits for its panel and PCManFM-Qt desktop.

Install/check reuse existing progress/cancellation/return-to-card flow. APT uses
--no-remove, no host upgrade/reset. LXQt adds lxqt-core, Openbox, QTerminal and
Qt6 Fcitx frontend; common X11/fonts/input dependencies are checked too. Package
availability and actual LXQt input/rendering require developer device checks.
Ubuntu shared with agents retains its existing XFCE path and cannot select LXQt.
No agent chat/session/permissions or ordinary environment code was changed here.

## XFCE diagnostic evidence

Report8696630881087439301 from v1.7.24 established a Garcon reload/removal storm:
desktop305 reload /3027 removal, panel369 /2193 over35 seconds. The trigger was
not established. GTK draw/add zero does not imply absent drawing/insertion;
not every GTK internal path is covered by the generic signals.

New optional GFileMonitor::changed emission hook only runs in opted-in panel and
xfdesktop processes in Test. When manual recording is off, it does not inspect
file notifications. It counts standard GIO event enum values, suffix categories
.menu/.desktop/.directory/other, polling/inotify backend types when detectable,
and Garcon qdata-tagged monitor events. Not all Garcon monitors carry that tag;
other monitor events in these processes are counted too. last-file-id is a
noncryptographic path hash for local correlation, not a security guarantee.
No raw filename/path, text payload, title, pixels or coordinates are logged.
No file contents are read, monitor events suppressed, menu settings changed,
or existing installed libraries replaced. Per-event work is bounded to the
existing path and fixed counters; output is coalesced to the existing 1-second
sampling with bounded report tails, not one log record per event.
GModule residency and atomic inode publication remain; new module needs a desktop
restart. The fixed control protocol/capture serial boundary and guardian/private
process shutdown are unchanged. All file counters reset on a new capture.

## Checks

33 focused JVM/Robolectric checks passed before final report-label addition:
selector persistence/live guard, readiness separation, rejection of unsupported
choices, exact owner close, diagnostic controls, atomic module publication.
72 Python/shell/native checks passed; 11 routing/session tests repeated after the
application-config preservation adjustment. Real GLib/GIO exercised GValue object
and enum ABI with a real file monitor and synthetic notifications, alongside the
existing real GObject reload signal, GModule lifetime, opt-in and control tests.
Mock GTK does not establish installed GTK/LXQt rendering or Android input behavior.
Final build/APK audit evidence is appended after completion.

## Minimum device checks

Stop the existing Test desktop. Select LXQt, check components, approve install,
launch. Check menu/submenus, window move/resize, browser, keyboard auto/manual
show/hide, Return, tab detach/return, stop and owner-session close. Existing XFCE
and installed programs must remain available. Then stop, select XFCE, restart,
start diagnostic recording, reproduce flicker with a stationary pointer for10–15s,
stop recording and export; compare phase=files with reload/removal counters.
Disappearance of flicker in LXQt would narrow the cause, not prove the final
GTK/Garcon fix or a faster native X11 renderer.

## Primary sources

- https://github.com/lxqt/lxqt/wiki/Window-managers-%28X11%29
- https://github.com/lxqt/lxqt/wiki/Launching-LXQt-sessions
- https://github.com/lxqt/lxqt-session/blob/master/startlxqt.in
- https://packages.ubuntu.com/noble/fcitx5-frontend-qt6
- GNOME/glib 2.84.0 gio/gioenums.h, gobject/gvalue.h and earlier gsignal.h.
- xfce-mirror Garcon4.20.0 core menu monitoring source, previously saved snapshots.
Source copies, test/build logs and audit live in .verification/x11-lxqt.


## Final APK evidence

Final build succeeded (48s); report identifies selected desktop explicitly.
ZIP integrity, duplicate entries, package/version/code/minSDK26, ARM64 ABI and
nondebuggable status verified. Signing v2 certificate matches code1239 and stable
baseline: dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644.
Required runtime definitions present:13,372 classes;31 reviewed optional missing
types, no unreviewed missing definitions. Pinned native/keyboard/JNI28/1/5 and
legacy assets checked, expected source assets match final APK. Private key/token
marker and forbidden personal-file scans passed (not an exhaustive security proof).
Existing Kotlin metadata/Lint-loader warnings remain; clean Lint is not claimed.
Code review traced stopped/busy/live selection, target validation, per-desktop
readiness, start/session/progress cancellation, retained guardian ownership,
observer optional loading, counter reset/coalescing and unchanged signal delivery.
Native X11 renderer and keyboard code are unchanged in this version.

APK: Termux-Jobs-v1.7.25-x11-test-arm64.apk (38990284 bytes)
SHA-256: e3b3affa1a06ea9cb686a6035a424cf14aa83ce09de4c590947e5d71bda34bc7
Copied to Download and destination hash verified; SHA companion provided.
LXQt package installation, menus, keyboard, actual visual behavior and persistence
across desktop switching still require device validation. This build does not
claim to fix the Garcon reload storm.
