# Private test 1.7.10/code1225: keyboard focus and window manager

## Behavior

- Header interactive-input button now toggles the graphical session's keyboard
  separately from ordinary terminal/agent interactive preferences. It manually
  opens the keyboard and arms text-focus auto-show. Manual Back/swipe dismissal
  disarms automatic reopening and clears the button. Re-enable with the button.
- A focus-loss-driven automatic hide leaves the feature armed for the next field.
  Unknown initial focus does not close a keyboard manually opened on the desktop.
  Leaving the graphical surface clears this local state; it cannot open another
  session's keyboard. A pre-draw observer reads existing IME visibility; it does
  not schedule animation frames or replace the Activity's insets listeners.
- The selected Ubuntu's fresh private desktop D-Bus session starts Fcitx5 with
  GTK3/Qt5 modules. A dbus-monitor --profile helper subscribes only to FocusIn,
  FocusOut and DestroyIC methods. Profile headers contain no typed/preedit data.
  Bounded per-launch-tagged frames go through the existing owned stdout pipe;
  no network endpoint/exported Android component/AccessibilityService is added.
- FocusOut from another context is ignored; short text-to-text transitions are
  debounced. XIM-only clients, GTK4 and apps bypassing the configured modules are
  not promised automatic recognition. The header button remains the fallback.
- In-app component consent now names Fcitx5. Check verifies its binaries plus
  installed GTK3/Qt5 package state. Install uses only the selected Ubuntu's APT;
  no profile scripts or existing agent shells are rewritten. Existing test desktop
  installations need another Check/Install to add the new packages.
- After check/install success, missing packages, cancellation or a reported error,
  the originating X11 card reopens, provided the same session remains selected.
  Error status/details stay there. Launching the graphical view itself still
  proceeds to the desktop. Installation retains outside/back dismissal protection.
- X11 details can show bounded startup output. Neither secrets nor input field
  content are deliberately collected by the new focus mechanism.

## Window resizing

Screenshot Screenshot_20261020_115728.jpg shows Thunar without a normal titlebar
or border. Missing/failed XFWM4 is a hypothesis; package/runtime evidence on the
phone is still needed. Ubuntu noble's xfce4 metapackage already depends on xfwm4,
so absence is not established. No screenshots alone prove a missing package.

The installer/check now explicitly include xfwm4 and xprop (x11-utils). A fresh
private desktop starts XFWM4 with --compositor=off, then probes root and supporting
window properties before XFCE startup. No existing desktop is replaced, no
saved sessions are deleted, and no global compositor preference is rewritten.
Off is a documented compatibility mode; it preserves WM move/resize operations.
The startup result is included in X11 details. Actual resize recovery is unverified
until the developer tries this APK on the phone.

## Verification and limits

Evidence: `.verification/x11-keyboard/`. Isolated real D-Bus fixture confirmed
focus method header format without its synthetic string body. Hermetic scripts
exercise module detection, metadata-only relay, toolkit environment and WM args.
JVM tests cover bounded framing, wrong tags/unsupported operations, stale context
focus, keyboard disarming/auto-hide, and menu return outcomes alongside earlier
X11/gesture/agent regressions. Android keyboard visibility in this cross-process
surface and actual GTK/Qt focus still require device checks.

Same com.termux/certificate/ARM64/minSDK26; versionCode1225. Private test only.
No user data migration/reset, agent authorization changes or GitHub publication.
All assistant-run checks used repository-local builds and isolated fixture processes;
Ubuntu components and the APK are to be installed through the developer's explicit
app actions. The working environment and running sessions were not modified.

## Device check

1. Update, open Ubuntu X11 Test → X11 → Check → Install missing components.
   Check that completion returns to the X11 card and Installed is disabled.
2. Switch to desktop. Check for titlebars/borders; move and resize a Thunar window.
   If absent, open X11 details and save the startup failure text.
3. Open an editable GTK field, manually enable the upper interactive-input button.
   Type, close with Back/swipe, check that the button clears and does not reopen
   the keyboard until explicitly enabled again.
4. Re-enable; move between editable/noneditable areas and rename a file to test
   focus-driven show/hide. Try a terminal/editor and an unsupported app manually.
5. Switch top tabs and another agent session; check that no background focus event
   steals its input, then return and explicitly enable the desktop keyboard.

## Sources and next stage

- https://github.com/fcitx/fcitx5/blob/master/src/frontend/dbusfrontend/dbusfrontend.cpp
- https://github.com/fcitx/fcitx5-gtk/blob/master/gtk3/fcitximcontext.cpp
- https://docs.xfce.org/xfce/xfwm4/introduction
- https://docs.xfce.org/xfce/xfwm4/faq
- https://packages.ubuntu.com/noble/xfce4
- https://packages.ubuntu.com/noble/arm64/fcitx5-frontend-gtk3
- https://packages.ubuntu.com/de/noble/arm64/fcitx5-frontend-qt5

Agreed future desktop installer: distinguish installed/configured/running, verify
an actual test window and WM behavior, include base file manager, terminal, text
editor, archiver and fonts. Plan per selected environment; retain independent
rootfs and separate optional desktop apps rather than silently installing a large
bundle now. Compositing may become an explicit per-desktop setting after testing.

## Final artifact verification

- 59 focused JVM tests and 29 Python tests passed; release build passed.
- ZIP integrity, package/version/ABI, signing certificate, JNI contracts and private-marker checks passed.
- APK: `Termux-Jobs-v1.7.10-x11-test-arm64.apk`, 38,922,472 bytes, versionCode1225.
- SHA-256: `8dc961f2f629bc808d6af4aed6ad881a2d78983410b1234103ff495c2ad47bd3`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Copied to `/sdcard/Download` with a SHA-256 companion; destination hash verified.
- Device scenarios above remain to be checked. No GitHub publication.
