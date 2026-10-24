# X11 landscape presentation review — 2026-10-24

**Historical code1346 review.** The developer's subsequent phone screenshot
showed the desktop still embedded after rotation. The original window fixtures
manually changed Activity resources and missed the localized-context/system-event
path. See the [rotation fix review](AUDIT-X11-ROTATION-FIX-2026-10-24.md) and
[current validation](VALIDATION-v2.1.0.md). The results below retain their original
artifact scope; they are not proof that code1346's phone fullscreen mode worked.

Version2.1.0/code1346 includes the preceding private source audits through1345.
Release preparation is authorized; publication is held for the new device check.
Evidence is in `.verification/landscape-20261024/` inside the repository.

## Implementation and boundaries

Each supported desktop channel owns its fullscreen presenter. While an X11
display or preparation screen is visible, it temporarily enables full sensor
rotation. Landscape moves the same View to a full-window Dialog; portrait or
Back restores its parent, child index and LayoutParams. Existing programs and
the native renderer are not restarted. The presenter's API contains no process
start, stop or session mutation command. Leaving X11 restores the Activity's
previous orientation policy. Background/service error/session-close paths remove
only the corresponding presentation; the existing desktop-stop rules remain.

Only the two requested controls appear above the native surface, in a separate
nonfocusable PopupWindow. They invoke the existing per-channel keyboard/settings
actions. Cutout insets keep the controls reachable while the desktop can extend
under a landscape cutout. The existing IME observer is attached to the actual
fullscreen window as well as the embedded host. Unknown insets during a host move
do not masquerade as a keyboard dismissal. Explicit IME insets handle fullscreen
windows that overlay rather than resize. Transient navigation bars do not change
the immersive desktop mode.

Back suppresses fullscreen re-entry until the next portrait turn, so a state poll
cannot immediately undo the user's action. Cancel/dismiss listeners verify the
originating Dialog. Review found that an unguarded queued cancel could close a
replacement host; it is guarded and covered by a regression test. Closed/removed
hosts are not reinserted by a late restoration callback. Listeners, popup and
window ownership are released on close; there is no new frame timer or global
static Activity cache.

The native adapter receives a logical landscape screen scaled for density and
bounded to1440px width, with aligned width and proportional height. Portrait mode
is retained. The pinned upstream renderer source at
`fa3a8b430e2896a19f44c99a9cb056254615ae06` was inspected: expected screen size,
physical viewport and keyboard-hidden pixels are already separate inputs, and
the returned source geometry drives pointer mapping. No JNI declaration, native
core, guest package, driver selection or rootfs file is changed for rotation.
The same adapter/presenter path serves all eight supported desktop targets.

## Checks and evidence limits

Window tests use an Activity, surrogate display View and actual Dialog/PopupWindow
APIs in Robolectric. They cover automatic entry/portrait restoration, exact View
identity/layout restoration, Back and suppression, control callbacks, fullscreen
window flags, inactive/closed ownership, stale cancel isolation and repeated
refresh without rebuilding windows. Geometry tests cover portrait, dense/small/
extreme landscape, aspect/width bounds and overlay-versus-resize keyboard crop.
Keyboard-state tests cover unknown insets without losing a visible manual input.

An initial scoped test asserted synchronous delivery of Dialog's cancel listener;
the test now drains its main queue. The first full suite exhausted the default
test JVM heap in Robolectric's framework-resource cache. App test workers now
have a768MiB heap, one concurrent worker and recycle after40 classes. This limits
the test infrastructure's cache lifetime; it is not an app memory setting or a
production workaround, and no tests are excluded.

Final confirmation passed1,222 JVM cases/184classes, eight lint-checker and six
X11-contract cases. Modern lint:0 errors/209 warnings; native/assets match1345,
signed APK/runtime/identity/source-stage and bounded secret checks pass.
Final test counts, lint and signed-APK checks are recorded in
[release validation](VALIDATION-v2.1.0.md). Source/build checks do not prove real
SurfaceControl reparenting, physical gestures, camera-cutout geometry, IME focus,
GPU composition or sensor rotation on a phone. The developer confirmed the
preceding audit build's device checks; the new rotation feature must be checked
separately. Minimum check: open a desktop/program, rotate, use both controls,
rotate back, repeat with Android Back, switch targets and confirm normal agent
input/orientation. Cover the currently used renderer and at least one guest
desktop in addition to base Termux. No installed environment, live session,
server or bot was altered by this verification.
