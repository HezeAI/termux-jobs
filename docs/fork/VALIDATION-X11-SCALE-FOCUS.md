# Private candidate: folder scale, keyboard gate and driver preselection

Historical candidate **2.0.5-wayland-test.5 / code 1371**, following test.4/code1370.
Private APK; publication and installation are separate actions. This includes the
full-area X11 presentation and compact additional keys documented in
[the preceding candidate](VALIDATION-X11-SCREEN-FILL.md).

## Changed behavior

The existing per-environment 50–150% scale now additionally overlays Thunar's new
window dimensions, sidebar divider width, three folder-view zoom modes and two
sidebar icon sizes. Thunar's supported discrete icon sizes are used; no folder
scan or per-file metadata rewrite. Maximized windows still occupy their workspace.
Other applications' remembered pixel geometry and page zoom remain application
specific. All scale properties share the original compare-before-restore mechanism;
returning to 100% restores unchanged originals, and repeated launches do not
compound scaling. No second scale control was added. Apply on the next X11 launch.

X11's Fcitx forwarding helper now validates automatic input against the current
normal/dialog/utility window, non-hidden normal WM state, matching window and
D-Bus process identity, and Fcitx Disable/NoOnScreenKeyboard capabilities. Desktop,
dock and minimized windows are rejected. A window switch discards a previously
bound context; an expired background request cannot revive on a later activation.
The request has one 200ms settling deadline. The helper otherwise blocks on X11
and private D-Bus events, without a periodic polling timer. Window titles, keys,
surrounding text, clipboard and file contents are never subscribed to or forwarded.
Capabilities and typed window metadata are the only new observations. Storage is
bounded. Missing metadata or helper failure falls back to manual keyboard control.
Desktop rename/type-ahead without a normal app window also requires manual input.

The gate uses the selected desktop's DISPLAY and new private session bus, inside
its existing guardian group. The DBus monitor is a directly owned child, terminated
and reaped on helper cleanup. No new process-name killing or stop mechanism.
Native Wayland input/text-input behavior is unchanged in this follow-up.

X11 renderer choice, driver-library choice and ZIP import, plus Arch Wayland Bionic
choice/import, no longer require an already connected desktop service. Merely
choosing a renderer neither installs nor starts the desktop. Actual renderer setup
still requires the connected backend; started/checking/installing/cancelling/
preparing/stopping and driver-library operations remain locked. Existing native
shared/exclusive driver locks prevent replacement while used. The separate style
checkbox retains its prior connection guard. Wayland selection widgets update
availability from service state without recreating the complete menu.

## Verification

- 340 distinct Java/Robolectric cases pass in the final merged suite results.
  The first broad run exposed one existing style-disconnection expectation when
  driver preselection was enabled. Style's prior guard was restored and the
  affected style, renderer, profiles and screen suites were rerun successfully.
  Results: `.verification/x11-scale-focus/final-results.json` and `final-xml/`.
- 79 Python/shell cases pass in `python-final.log`: scale restoration/repeated
  startup/theme switching/new and legacy Thunar enums/user edits/unknown enums,
  active and hidden-window gating, stale focus expiry, disabled contexts, bounded
  metadata framing, real isolated D-Bus monitor method framing and owner-PID lookup,
  private session wiring, guardian ownership/cancellation and Wayland driver isolation.
  The real bus fixture confirms nonmatching text calls are absent from monitor output.
- `git diff --check` and session shell syntax checks pass.
- Modern lint, release assembly and final APK verification are recorded separately
  in `.verification/x11-scale-focus/`. Final identity/hash evidence is appended
  after successful artifact checks, never inferred from source tests.

## Review and remaining device checks

Changes stay in repository source and isolated tooling. No installed environments,
agent sessions, Telegram service, global shell/DNS configuration or private auth
files are changed by development checks. No new Android permission or data reset,
workspace-schema change or rootfs migration. APK package and signing identity must
match the existing app, and code1371 is higher than all preceding private candidates.
Scale configuration remains per-environment HOME. Restore 100% once before rolling
back to an APK that lacks scale-overlay restoration. Installing KDE still upgrades
Arch packages; APK rollback does not undo that previously documented package change.

Mocked UI/window metadata tests do not prove phone IME or SurfaceControl behavior.
Developer checks needed: restart X11 at75% and100%, open a fresh Thunar window and
compare folder/sidebar icons; check empty desktop, minimize/switch apps, actual text
fields, Back suppression, retap and manual input with auto on/off. Check Bionic
import and renderer selection before installing a desktop, then locked controls
while running it. Different toolkits may omit PID/capability metadata and require
manual input. Full KDE GUI launch retains its previously documented device gap.

Final checks passed: modern lint0errors/213warnings (unchanged warning count);
release assembly; ZIP integrity; package `com.termux`; ARM64 only; stable signing
certificate; source assets; unchanged stable JNI; reviewed Wayland native pins;
required DEX/JNI runtime definitions; no secret-pattern findings. The APK build’s
legacy bundled lint still reports its previously reviewed Kotlin-metadata/check-jar
incompatibilities; the separate current lint gate passed.

APK: `Termux-Jobs-v2.0.5-wayland-test.5-arm64.apk`, versionCode1371.

SHA-256: `3fadf2d0479e2ffb0c55df671eb759125d241b8845c1e86cbdc9d8c5c4c7aae7`.

Superseded by [test.6 live scale and startup-state handling](VALIDATION-X11-SCALE-LIVE.md).
