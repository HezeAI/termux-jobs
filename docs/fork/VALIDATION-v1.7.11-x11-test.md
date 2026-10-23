# Private X11 check · 1.7.11-x11-test / code1226

Stable public release remains v1.7.5. No GitHub publication is authorized here.

## Device evidence from the preceding build

The developer confirmed XFWM4 window-edge resizing now works in 1.7.10. Screenshot
20261020_121543 shows window titlebars and an enabled interactive-input button,
but the developer reports neither manual nor field-driven Android keyboard opening.
The screenshot does not alone establish the Android IME's internal failure cause.

## Change and rationale

- The embedded renderer previously requested IME from a display/service context.
  The host SurfaceView had its own focus and could transfer window focus to the
  embedded hierarchy. That path did not open the keyboard on the tested device.
- Keep the host SurfaceView nonfocusable and add a 1×1 Activity-owned editor
  endpoint. Only an explicit interactive toggle or an armed Fcitx field-focus
  event enables it. Android InputMethodManager and WindowInsetsController requests
  now use this Activity's view/window; no forced keyboard flag, popup or overlay
  permission. Renderer/native core remain exclusively in :jobs_x11.
- Committed text and bounded key events use the existing same-UID private Binder,
  bound to the current attachment ID. Text chunks are <=1024 UTF-16 units without
  splitting surrogate pairs. No text/key payloads are logged or persisted.
- Back/volume keys remain Android-owned. Disabling/closing releases held modifier
  keys, invalidates old input connections and clears focus. Auto-hide animation
  frames no longer turn automatic field-focus hiding into manual disarming.
- Ordinary terminal/agent keyboard and toolbar settings retain their paths. The
  X11 interactive flag resets on detachment; the normal terminal preference is
  not overwritten. X11's extra-key toolbar is not introduced in this build.
- The desktop area displays a spinner and localized Starting desktop label while
  the private X server/XFCE starts. Installation/checks retain their progress dialog.
  A nonce-tagged bounded header announces readiness after xwininfo finds both
  xfce4-panel and xfdesktop windows. This is component/window readiness, not proof
  that a first compositor frame has completed. No artificial minimum splash delay.
- A bounded startup probe (~20s plus command execution and preceding WM wait)
  exposes a Show X11 screen fallback and details guidance if components are slow
  or have different window classes. Attachment failure remains a visible error;
  polling does not repeatedly hide/recreate the surface. Launch failures return
  to the terminal and show the startup error.
- No Linux package installation, active session manipulation, process termination
  or APK installation was performed by these source/build checks. The existing
  own-process/group shutdown mechanism is unchanged. No data reset or migration.

## Relevant checks

Evidence: `.verification/x11-ime-fix/`. Focused JVM checks cover text composition,
old connection rejection after disable/re-arm, Android Back handling, bounded
Delete, Enter, held modifier release, focus animation policy, nonce/readiness
parsing, existing lifecycle/gesture/menu and agent UI regressions. Shell fixtures
cover ready and timeout paths, focus metadata-only output, component checks and
existing guardian/environment behavior. Final build/artifact results follow below.

Device checks still needed: keyboard opens from upper button, types Latin/Cyrillic
text into terminal/rename, Back/swipe clears button, re-enable and focus changes,
switch between desktop/terminal/agent, spinner transitions, attach failure recovery.
Mock tests do not prove Samsung IME behavior, touch focus, renderer frames or DPI.

## Android reference used for focus ownership

- https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility
- https://android.googlesource.com/platform/cts/+/23e8991573a/tests/framework/base/windowmanager/src/android/server/wm/SurfaceControlViewHostTests.java
  `testFocus` checks that making the host SurfaceView nonfocusable transfers focus
  back to the host window. Public setView(view,width,height) is used; the LayoutParams
  overload is hidden/TestApi, so this implementation does not depend on it.

## Final results

- 66 focused JVM tests: zero failures/errors. 29 Python tests passed.
- Release assembly passed. Runtime audit: 13,309 defined classes, 31 reviewed optional unresolved types; required definitions present.
- Existing lint-tool dependency warnings (Kotlin metadata/lint Vendor class loader) remain; a clean standalone Lint run is not claimed.
- ZIP integrity, com.termux/code1226/version1.7.11-x11-test/minSDK26/ARM64, certificate and JNI contract checks passed.
- Baseline native libraries and unrelated assets remain byte-identical. Source desktop scripts match the APK. Private-key/auth-file markers checked.
- APK bytes: 38927480. SHA-256: `76e9d8b9151a3402a60490cb9033186ec0084fabe69b26005b14fa5b4233bd8b`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Delivered to `/sdcard/Download/Termux-Jobs-v1.7.11-x11-test-arm64.apk` with SHA-256 companion; destination hash verified.
- Keyboard/startup display still require the device checks above. No GitHub publication.
