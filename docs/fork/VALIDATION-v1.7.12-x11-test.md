# Private X11 check · 1.7.12-x11-test / code1227

Public stable remains v1.7.5; this APK is for the developer's device checks.

## Evidence and diagnosis

The developer's Screenrecorder-20261020-122710.mp4 shows selecting Thunar's address
field without automatic IME opening, then opening Android IME with the upper
interactive-input button. Typed text becomes visible when the keyboard closes.
Extracted frames are under `.verification/x11-live-input/` (not packaged).

Source inspection proves two relevant defects:

- X11InputView stored setComposingText contents without sending them to X11, only
  forwarding commitText/finishComposingText. This accounts for a keyboard that
  composes a word until dismissal. It is distinct from X11 renderer readiness.
- DesktopKeyboardState ignored text-field focus until manually enabled, preventing
  automatic focus from invoking the otherwise working Activity-owned endpoint.

Read-only checks found fcitx5, dbus-monitor, XFWM4 and Thunar running; the frontend
module is installed and mapped by Thunar, the D-Bus frontend is mapped by fcitx5,
and the three relevant processes use the same private bus and DISPLAY :97.
Only component/whitelisted environment metadata was printed, no auth data. These
facts rule out simple absence of those modules; they do not prove every field
focus event reaches the Android adapter. No installed environment was changed.

## Changes

- Keep a single Activity-owned IME bridge. Both real Fcitx text-field focus and the
  upper manual button call RemoteDesktopView.keyboard. No second IME window, native
  library replacement, overlay permission or system keyboard service was added.
- Fresh field focus now permits automatic opening; unknown initial focus does not
  hide the manual keyboard. Manual dismissal suppresses stale/repeated focus polls.
  A new field transition or a fresh tap on the still-focused text field can reopen it.
- Only a genuine single-finger left tap triggers that retap path: moving, dragging,
  right clicks and two-finger scrolling do not. Recheck text focus after 200ms,
  allowing focus-out/debounce to settle, and ignore when desktop is not ready.
- Immediately mirror the current IME composing word and update its changed suffix
  through ordinary text/Backspace events. Finish/commit no longer replay the word.
  Keep an Android editable buffer for IME queries; request character-oriented,
  no-suggestions, no-personalized-learning input. Latin/Cyrillic/basic surrogate-pair
  replacement is covered by tests. Complex grapheme/IME behavior still needs device
  checks because Linux widgets can differ in Backspace/selection semantics.
- On changing desktop field/retapping, restart the input connection and invalidate
  old callbacks without replaying or deleting its previous word in the new field.
  Manual close still releases held keys. Back/volume remain Android-owned.
- X11 details now include whether text field focus was detected and the metadata
  event serial. Text contents, key values and pointer coordinates are not added
  to the report. Input is still same-UID, attachment-bound and bounded per request.
- Existing startup spinner, XFWM4 without compositing, renderer isolation, gesture
  handling and process/group stop logic remain. Normal terminals/AI input paths,
  credentials and user data have no migration/reset changes.

## Verification and limits

Evidence directory: `.verification/x11-live-input/`. Relevant JVM checks cover live
composition, corrections, duplicate prevention, Delete/Enter, Unicode codepoints,
stale connection/field changes, automatic/manual opening policy, settled retaps,
no retap from drag/scroll, existing X11 ownership/lifecycle/menu and agent UI tests.
Shell fixtures cover launch readiness/timeout and component/guardian behavior.
Mocks are not proof of Samsung/Gboard typing, Linux selection, DPI or native frames.

Device check: launch Ubuntu X11 Test, select an editable Thunar/terminal field,
verify automatic IME and button highlight; type abc/привет without dismissing;
close by swipe, check no duplicate text or spontaneous reopen, retap the field;
try a second field and manual button, then ordinary terminal and agent sessions.
Unsupported toolkit applications retain the manual button fallback.

References inspected:

- https://raw.githubusercontent.com/fcitx/fcitx5-gtk/master/gtk3/fcitximcontext.cpp
- https://raw.githubusercontent.com/fcitx/fcitx5/master/src/frontend/dbusfrontend/dbusfrontend.cpp
- https://raw.githubusercontent.com/d-bus/dbus/master/tools/dbus-monitor.c
  D-Bus profile stdout is line-buffered; an unverified buffering hypothesis was
  rejected rather than adding an unnecessary stdbuf dependency.

Final test/build/APK evidence follows after assembly.

## Final artifact evidence

- 75 focused JVM tests, zero failures/errors; 29 Python checks passed.
- Release build passed; required runtime definitions present (13,312 classes, 31 reviewed optional unresolved types).
- Existing Lint tooling dependency warnings remain; standalone clean Lint is not claimed.
- ZIP integrity, package/versionCode/ABI/minSDK, signing certificate, pinned native inputs, exact JNI descriptors and private marker checks passed.
- `Termux-Jobs-v1.7.12-x11-test-arm64.apk`, 38929796 bytes, com.termux/code1227, ARM64, minSDK26.
- SHA-256: `4356b9c3644741e3949067c96e1ebcfc56c5c0954b28aa0d12e91d839a5573f3`.
- Certificate SHA-256 unchanged: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Baseline native payloads/unrelated assets unchanged; versionCode increased from 1226. No migration/reset of user data.
- Delivered to `/sdcard/Download` with companion checksum; destination SHA-256 verified.
- Device checks above remain necessary; no GitHub publication was performed as part of this build task.
