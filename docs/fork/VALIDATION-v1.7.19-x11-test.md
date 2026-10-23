# Private X11 comparison · 1.7.19-x11-test / code1234

Public stable remains v1.7.5. Private test APK, no GitHub publication.

## Single behavior change from 1.7.18

x11-session.sh starts its owned XFWM4 with --compositor=on instead of off.
The separate window-manager startup/readiness sequence is retained; the change
does not remove XFWM4 or its window resizing/decoration responsibilities.
The developer's saved use_compositing preference was already true, but the explicit
off launch option disabled the compositor regardless of that saved setting.
Native X11 core/renderer, keyboard bridge and competing-IME guard, pointer gestures,
viewport/cropping, Fcitx, readiness timeout, program manager/cache, process ownership,
stop/cancel lifecycle, ordinary terminal and AI Agent paths are unchanged.

This is a controlled comparison of an unproven hypothesis. The developer reported
Applications submenu flicker remains in 1.7.18. Its exact cause, the black wallpaper
and the readiness timeout are not declared fixed. Enabling the compositor may change
rendering cost/behavior; physical device observations are required. No graphics
driver, package, system environment, shared SDK or installed rootfs is changed by
the source/build work.

The argument applies when the app starts a new graphical desktop session. Updating
the APK or reattaching an existing view is not evidence that an already-running
XFWM4 changed its launch mode. For comparison, use the app's Stop graphical desktop
control for Ubuntu X11 Test, then launch that desktop again. Do not close ordinary
terminal/agent sessions for this check. The previous 1.7.18 APK remains preserved;
Android cannot install its lower versionCode over this version without a future
code-increased rollback build. Do not uninstall/clear data as a comparison step.

Package/signing identity retained; code increases 1233 -> 1234. No data reset or
migration is introduced. Existing environments, presets, files and cached program
metadata are retained by the change.

## Verification

42 isolated Python/shell checks passed. The isolated session fixture verifies the
actual XFWM4 argument, helper startup, metadata-only Fcitx stream and readiness/
timeout paths. The owned-child PID fixture now waits for complete numeric file
contents rather than racing creation of an initially empty PID file; production
process/stop code is unchanged. The prior 117 passing JVM/UI checks from 1.7.18 apply to unchanged
Java behavior; they are not a substitute for compositor/menu checks on the device.

Developer checks: fresh Ubuntu X11 Test desktop launch, Applications from panel and
desktop, hovering/selection across submenus, selected wallpaper, window-edge resize,
keyboard open/close and Return. Report whether behavior changes and whether desktop
interaction becomes slower. No automatic repair or environment reset is included.

## Final artifact

Isolated ARM64 release build passed (113 tasks). Runtime definition audit passed: 13,359 defined classes; 31 remaining optional/platform types have explicit review notes. Archive integrity, package/version, ARM64 ABI, signing identity, pinned native/JNI contracts and private-marker checks passed.

APK: `Termux-Jobs-v1.7.19-x11-test-arm64.apk` (38,961,599 bytes).

SHA-256: `93e1fc278247478a80c1991736afa403665ee0a0122f68bfb838f0343c156a33`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Delivered to Download with destination SHA-256 verified. No GitHub publication or APK installation performed by this task.

## Developer comparison result · 2026-10-20

The developer reports Applications menu flicker persists after desktop launch in
1.7.19. The compositor-on comparison did not resolve the reported symptom.
Screenshot Screenshot_20261020_172253.jpg shows readiness TIMEOUT, XFCE ready=false,
text focus not detected, and xfdesktop failure opening the missing default
/usr/share/xfce4/backdrops/xubuntu-wallpaper.png. Earlier image-readable=true
markers do not demonstrate successful rendering of that selected image. The
GLib object/file assertions occur with this backdrop failure; neither they nor
the screenshot prove a cause for Applications flicker. Source inspection of
xfdesktop 4.20.1 confirms the null filename in this error log is taken from
rdata->image_file, while the propagated error identifies the attempted default
path. No further production changes or installed-environment mutations made
for this observation.
