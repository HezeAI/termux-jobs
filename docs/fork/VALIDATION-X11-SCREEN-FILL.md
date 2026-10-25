# Private candidate validation: full-area X11 and compact extra keys

Historical candidate **2.0.5-wayland-test.4 / code 1370**, following test.3/code 1369.
Superseded by [test.5 folder scale, keyboard and driver selection](VALIDATION-X11-SCALE-FOCUS.md).
No GitHub publication is included.

The developer's screenshots show a manual 1300×600 mode and 75% interface scale
with empty side strips in the embedded portrait host. The presentation helper
previously fitted the framebuffer's aspect ratio into the Android host. It now
uses the complete host rectangle at every interface scale, subtracting only the
explicit horizontal/vertical safety margins. Zero margins mean full coverage in
both the app window and landscape full screen. Native viewport drawing already
scales independently along both axes; native binaries are unchanged. Shapes can
stretch when framebuffer and host aspect ratios differ, as requested.

Logical manual resolution, phone-based orientation, interface scale preferences,
next-launch scale application and safe-margin bounds remain unchanged. Auto
continues its existing behavior. The revised bilingual screen-settings explanation
says the image fills the desktop area, except for chosen margins.

The desktop extra-key overlay starts at 400×104dp (previously 478×128), clamped to
the visible desktop. Font size 12 sp, smaller padding/handles and smaller reflow
thresholds make it compact in portrait and landscape. All 16 keys, the close key,
modifier latches, held-key release, left resize handle and right move handle remain.
Resizing reflows the existing views. It remains a non-focusable sibling window;
no keyboard ownership, touch routing or process lifecycle changes were made.
Agent terminal toolbars use their existing separate controls.

Checks:

- 305 selected Java/Robolectric cases pass: screen preferences, invalid bounds,
  fill at 50/75/100/125/150%, manual orientation independent of keyboard geometry,
  explicit margins, existing X11 lifecycle/ownership, and pointer corner mapping
  from stretched portrait/landscape native viewports to logical coordinates.
- 33 desktop presentation/keyboard cases pass, including compact layout coverage,
  every key remaining in the grid, modifier/cancel release, dragging, resizing,
  clamping, full-screen return, IME-control state and repeated host reuse.
- Shell/scale logic and native inputs are unchanged from the validated test.3
  candidate; this follow-up does not rerun package installations or hardware probes.
- Modern lint, release build and final APK checks are recorded alongside this file
  in repository-private `.verification/x11-screen-fill/` outputs. Native/source
  asset hashes, package/version, ARM64, ZIP integrity, signing identity, runtime DEX
  definitions and personal-key/token pattern checks must pass before delivery.

Review: the change is confined to desktop presentation and its extra-key overlay.
No installer, environment, driver, guardian stop behavior or agent chat code changes.
No timer or background worker is added. Geometry changes reuse the current host;
the overlay keeps its attribute-change guard and releases held keys when closed.
No reset, preference migration or new Android permission is required. Same package,
signer and workspace schema 8; versionCode increases above the installed candidate.

Device checks remain: full coverage in portrait and landscape with manual mode at
75% and another scale; zero margins and explicit margins; mouse targeting near
all edges; keyboard show/hide; drag/resize the compact key overlay and check Esc,
arrows, Enter, Ctrl/Alt and closing while a key is held. Surrogate UI tests do not
prove physical Android SurfaceControl, pointer or IME behavior. Full KDE GUI launch
retains its previously documented device-check gap.

Final APK verification passed: package `com.termux`, versionCode 1370, ARM64,
stable signing certificate, archive integrity, reviewed native hashes, source
assets and required runtime/JNI definitions. No secret-pattern findings.

SHA-256: `ee4825468adc0ea7e7518f24c819b5105b2683797c94922d5ab3bb52fa0d7b67`.
