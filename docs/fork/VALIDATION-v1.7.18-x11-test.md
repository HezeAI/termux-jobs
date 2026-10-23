# Private X11 check · 1.7.18-x11-test / code1233

Public stable remains v1.7.5. Private developer test APK.

## Isolate the competing Android IME endpoint

The 1.7.17 developer video still shows Applications submenu highlight/disappearance
while the surrounding window remains stable; a visible cursor over File Manager
can coincide with selection of a different item. The earlier keyboard workaround
is not a reliable fix. The exact cause remains unproven.

The embedded renderer previously advertised itself as an Android text editor,
created an InputConnection, and restarted that editor from native mouse-button
callbacks, despite the separate Activity-owned X11InputView handling text/keys.
LorieView now uses its existing standalone MainActivity callback identity to distinguish
these modes. With no standalone callback (EmbeddedX11Host's Context constructor):
onCheckIsTextEditor returns false, onCreateInputConnection returns null before
mutating EditorInfo, and resetIme returns before posting/restarting input.
Standalone MainActivity mode retains its existing editor/connection/restart behavior.

Activity-owned IME, Fcitx focus detection, focus gain/loss handoff, touchpad/physical
pointer gestures, hardware/Binder key and text forwarding, viewport/cropping,
compositor and pinned native library/JNI descriptors are unchanged. Process ownership,
cancellation/stop behavior and unrelated terminal/agent input paths are unchanged.
This removes a competing endpoint; it does not establish that menu flicker is fixed.

The separate Epiphany bwrap/dbus-proxy launch failure and missing GTK4 IM-module
warning in the screenshot are unresolved application compatibility issues, not
proof of the Applications cause. Black wallpaper also remains unresolved.

## Persistent per-environment program lists

Available catalog and installed-program snapshots now have independent caches per
supported environment (ubuntu and jobs-x11-ubuntu). On opening a list, a worker first
uses its immutable memory snapshot or validated private disk snapshot; a cache hit
starts no guest process, APT command, AppStream scan or network request. First use
without a valid snapshot scans as before. No timer/background scan/expiry is added.
Search/pages are computed from the corresponding immutable snapshot.

The existing Refresh catalog/Check installed programs buttons explicitly bypass the
cache. Successful complete results replace the same files atomically; failed or
cancelled scans cannot replace a complete disk snapshot with partial results.
Installation/removal invalidates only that environment's status before executing
its package operation; stale disk snapshots are overwritten with an invalid marker
so a later app restart cannot resurrect pre-mutation status. Successful removal
refreshes the installed list. Other stale lists rescan when next opened. Changes
made externally in a terminal require manual refresh.

Files: app-private filesDir/workspace-x11-catalog/<target>-catalog.jsonl and
<target>-installed.jsonl. At most four snapshot files are used for the two current
targets; Android AtomicFile commit removes its temporary/backup file. Records contain
only package/name/summary/installed/available/Snap metadata. Programs, icon images,
user files, credentials and environments are not copied. Size depends on entries
and descriptions; no exact installed-device size was measured. Each stored snapshot
has a 32 MiB safety ceiling, in addition to the existing 10,000-entry/record bounds.

Unknown targets, symlinked cache paths, incomplete, malformed and excessive files
are ignored; normal first-load behavior recovers them. Writes use a fixed private
filename and atomic replacement. Deletion still requires an installed entry for the
exact selected environment, protected-component checks in service/guest and normal
dpkg dependency checks. All disk and guest work runs on the program worker, not the
terminal Activity's UI thread. A cache write failure does not turn a successful
package operation into an error or change user data.

Package/signing identity is retained; versionCode increases from 1232 to 1233.
No user-data reset or migration is required by these source changes; existing
programs and environments are retained. A private metadata directory is added.

## Evidence and device checks

117 focused JVM/Robolectric tests passed (zero failures/errors/skips). Tests using native
and Android-service stubs verify actual Java endpoint routing, not device menus.
Cache fixtures and mocked guest boundaries verify persistence, environment/list
separation, replacement, corruption/symlink rejection, no guest on hits and forced
refresh. Physical keyboard behavior and menu rendering still require device tests.

Minimum developer checks: open Applications and its submenu before opening any
text field; leave the cursor over a menu item briefly, then move to another category.
Repeat after opening/closing the keyboard. Verify automatic/manual keyboard opening,
live Russian/English text and Return in the browser. Open both program lists twice,
restart the app when convenient, reopen, and use manual refresh once; check that
installed/deleted program status updates without affecting the other environment.

## Final build/artifact verification

- Release ARM64 build passed; 13,359 packaged definitions and 31 reviewed optional/
  tooling unresolved types; required runtime definitions verified.
- ZIP integrity/duplicate-entry check, package com.termux, minSdk26, ARM64 only,
  non-debuggable, versionName 1.7.18-x11-test / code1233 verified.
- Existing signing certificate and v2 signature verified; pinned native/keyboard
  assets and JNI 28/1/5 contracts verified. Desktop assets match current sources.
- Baseline unrelated assets/native libraries remain identical to stable 1.7.5,
  apart from the previously intentional environment-runtime.sh change.
- Private authorization/signing filenames and private-key/token markers absent.
- git diff --check passed. Existing build tooling/Kotlin metadata warnings remain;
  no standalone Lint-clean or device menu/keyboard result is claimed.
- Verification used isolated source/build files and mocked native/guest boundaries.
  Unchanged script fixtures retain the 42 passing checks recorded for 1.7.17;
  no new script changes or installed-environment tests were made in this increment.

APK: `Termux-Jobs-v1.7.18-x11-test-arm64.apk` (38,961,599 bytes).

SHA-256: `b8ed11634ff28dde52fb22a043a3ac0a556f49dea5890969f20e990cf669ac10`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Evidence: `.verification/x11-single-ime/tests.log`, `build.log`, `apk-audit.json`.
APK and SHA-256 companion copied to Download and the destination hash verified.

## Developer follow-up after delivery

The developer reports Applications submenu flicker still present in 1.7.18. Removing
the competing embedded IME endpoint did not resolve the symptom; do not report it
as the established cause. Screenshots 20261020_170427/170431 show the normal startup
placeholder followed by the readiness timeout/manual-reveal fallback, not an explicit
server crash. The developer also notices slight desktop scrolling/panning.

Code inspection confirms X11Viewport estimates full height from window bounds/insets
and treats every positive difference from observed view height as hiddenBottom. The
prior device diagnostic (1947 full vs 1944 visible) therefore supplies a 3-pixel crop
even without a visible keyboard. Pinned Renderer::redrawLocked regards any positive
crop as bottomHidden and follows the pointer via panToCursor. This explains a possible
small automatic pan, but does not establish that it causes Applications highlighting
or disappearance. No viewport fix/build was made during this follow-up. Future
comparison must distinguish actual IME crop from persistent layout/inset discrepancy,
without reintroducing keyboard-driven mode changes. Readiness detection is a separate
path requiring its own check, rather than treating timeout as a graphics failure.
