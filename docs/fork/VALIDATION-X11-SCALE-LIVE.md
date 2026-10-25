# Private candidate: immediate X11 scale and re-entry state handling

Historical candidate **2.0.5-wayland-test.6 / code1372**, following test.5/code1371.
Includes preceding geometry, compact keys, auto-keyboard gate and driver
preselection work. No stable release/publication is implied.

HezeAI subsequently reported this candidate works; see the Auto setup follow-up for
the requested reset-control change. This report retains its original verification scope.

The X11 screen Apply button now waits for live-scale acknowledgement before
saving the settings and relayouting resolution/margins. If no desktop is started,
settings are saved for its next launch. A running XFCE is updated through its
existing private session bus and public libxfconf API. The selected environment's
private descriptor pins the target, display, HOME, authority path and launch nonce.
Stale/wrong-target requests fail; no authorization file contents are read or copied.
No new desktop, bus, package installation or resident scale daemon is created.

The one-shot settings child has a separate guardian, timeout and lifecycle;
it never replaces the stored desktop child or kills processes by name. Desktop
shutdown closes both independently owned jobs. State remains running while scale
is applied. Native Wayland/KDE scaling and agent sessions are unchanged.
A recovery journal records old/new scale values before writes. Repeated updates
use original values; failed updates roll back owned changes; interruption can be
recovered at the next launch. Concurrent/manual edits and newer records are not
blindly overwritten. XML is not edited while the live settings daemon owns it.

Open, normal Thunar clients on this DISPLAY receive proportional size requests,
without moving/focusing them or synthesizing input. Maximized, hidden/full-screen
windows are excluded. Some open applications cache zoom. Existing Thunar folder
icons may need the folder/window reopened; folder-specific overrides are preserved.
The implementation does not promise immediate pixel-size changes in every toolkit.

The developer reported a long restart followed by a long cancellation. The current
phone process tree/service state was not accessible in this tool environment; the
developer subsequently reported cancellation completed. Source inspection found a
matching race: a new check can receive an already-running desktop snapshot, leaving
the preparation UI waiting for an installed state that cannot arrive. It now attaches
to the already launched desktop. Cancelling in the check/start transition sends Stop
when a desktop is already started, instead of cancelling an absent check. The normal
installer cancellation cleanup remains unchanged. This race is covered by isolated
state-transition tests; it is not proof of the screenshot's precise runtime cause.

Verification:

- 346 distinct Java/Robolectric cases pass (merged broad and final affected suites):
  Apply acknowledgement/error preservation, wrong-target/invalid/unready/Wayland
  rejection, running-desktop re-entry, cancellation race and existing presentation.
  The new cancellation fixture initially lacked its Activity view tree; it was
  corrected to exercise the hidden-tab state path, without changing product logic.
- 35 Python/shell cases pass, including live update/100% restore, repetition,
  manual edits, rollback, crash recovery, session nonce/target checks and scoped
  one-shot routes. The routing case additionally covers all seven guest environments
  with isolated command fixtures, without invoking installed package managers.
- A real native Termux Python/libxfconf + fresh private D-Bus/xfconfd fixture passes
  75→150→75→100 with numeric and string properties, using HOME/config/cache/runtime
  exclusively under `.verification/`. This probe caught an FFI string-pointer
  lifetime bug before APK assembly; the correction and regression probe pass.
  Native dependencies are read from the existing installation; the fixture does
  not connect to any existing user's desktop bus. Its daemon logs a harmless
  inability to increase its fd limit, then activates and exits normally.
- Modern lint passes with0errors/213warnings (unchanged). Shell syntax and
  `git diff --check` pass. Build/artifact evidence lives under
  `.verification/x11-scale-live/` and is appended after verification.

No app data reset, schema migration or new permission. Package/signing identity is
preserved and code increases. Native libraries/renderers are unchanged. The prior
KDE package-upgrade/rollback limitation still applies. Restore scale100% once before
rolling back to an APK without overlay restoration support.

Device checks still needed: live75/150/100 changes on a visible Termux desktop;
Thunar normal/maximized windows and folder icons; pointer/IME during and after the
change; real stop/re-entry/cancel transitions. The native library fixture does not
prove Android UI, window-manager resizing or every distribution's GUI behavior.
Full KDE GUI remains subject to its separately documented device-check gap.

Final APK checks passed: package `com.termux`, versionCode1372, ARM64, stable
signing certificate, ZIP integrity, source assets, reviewed native hashes, required
DEX/JNI definitions and no secret-pattern findings. Legacy bundled-lint Kotlin
metadata/check-jar compatibility warnings remain; the separate modern lint gate passes.

SHA-256: `b8042a1d068d1cbbc2bd1dca35bf45baac4b1e1e76cf77540d057ce763597159`.

Follow-up: [test.7 Auto setup](VALIDATION-X11-AUTO-SETUP.md).
