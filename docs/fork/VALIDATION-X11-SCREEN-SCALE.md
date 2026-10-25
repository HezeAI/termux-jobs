# Historical private candidate validation: test.3

Test.4 changes aspect fitting to full-area filling; see
[follow-up validation](VALIDATION-X11-SCREEN-FILL.md).

Candidate 2.0.5-wayland-test.3 / code 1369, following the developer-tested code 1368.
No stable release or GitHub publication is included.

The developer's two screenshots show the app toolbar and desktop menu after
rotation back: the Android full-screen window had closed correctly, while the
manual 1296×600 framebuffer remained a letterboxed landscape strip. The fix uses
the phone's configuration orientation for both the Android presentation frame and
the service's logical display mode. It transposes the configured size pair in
portrait and applies X11 width alignment after transposition. Keyboard height is
not a rotation signal. Auto retains the existing sizing logic and zero-margin
behavior. The normal full-screen controller and process lifecycle are unchanged.

Per-environment interface scale 50–150% is saved beside geometry. It is passed as
a validated integer through private IPC and the guarded X11 child's environment,
including each selected PRoot guest. It is captured for a desktop launch; changing
it while running schedules it for the next launch. Component check/install and
Wayland do not apply this XFCE setting.

The new `x11-scale.py` runs before XFCE starts, using the existing safe XML helpers.
It restores the previous overlay before theme changes, then scales font DPI,
desktop/cursor icons and known panel dimensions. Original properties are recorded
before writes; later user edits survive restoration. Unrelated properties and
other HOMEs remain unchanged. Repeated starts do not compound scale. Returning to
100% restores the unmodified baseline. Default 100% with no previous overlay does
not create or edit XFCE configuration. Invalid scale, XML declarations, symlinks
and malformed properties are refused before the relevant writes. There is no
background process, global shell setting or package/dependency installation.

Verification:

- 302 selected Java/Robolectric cases pass, no failures/errors/skips. Covers saved
  settings, invalid input, portrait/manual alignment and keyboard geometry, profile
  isolation and service component cancellation/owned cleanup. The component worker
  retains its existing start signature; scale is only supplied to desktop runs.
- 7 isolated scale cases pass: 100% unchanged defaults (including a custom config
  link), repeated 75% launches and 100% restoration, theme changes, distribution
  defaults, preservation of manual edits and rejection of malformed/link input.
- 29 existing theme cases and 15 desktop launcher cases pass. Launcher fixtures
  verify the selected scale reaches each guest command without changing displays.
-Modern lint across five modules: 0 errors, 213 reviewed warnings, unchanged from
  test.2. Shell syntax and whitespace checks pass.

APK identity/integrity/native/source asset/privacy verification is recorded in
`.verification/x11-screen-scale/artifact.json` after the final build. Same package,
certificate, schema 8 and ARM64; higher versionCode. Existing native libraries and
Wayland inputs are unchanged. The new helper is an asset, not an installed daemon.

Device verification remains: rotate manual 1300×600 to portrait and back; show/hide
IME; tab/back navigation and pointer targeting; 75% after Stop/start; another launch
at 75%; 100% restoration; Auto/100% regression. Source tests do not establish actual
Android SurfaceControl, gesture or DPI behavior. Third-party applications may use
fixed pixel sizes or their own zoom. Test.2's full KDE GUI launch gap remains.

Compatibility: no automatic data reset or rootfs migration. Scaling edits only the
selected environment's user XFCE settings, with restoration state below its HOME.
For rollback to an older APK that lacks the helper, restore 100% and restart X11
first. KDE installation still performs a full Arch package upgrade; an APK rollback
does not undo package changes.
