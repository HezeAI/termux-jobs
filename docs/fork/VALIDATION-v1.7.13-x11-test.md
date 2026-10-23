# Private X11 check · 1.7.13-x11-test / code1228

Stable public release remains v1.7.5. This is a local device-test build.

## New device evidence

The developer reports that manual keyboard opening and live typing now work in
1.7.12, but tapping a desktop text field still does not open the IME. Screenshot
20261020_125204 shows `Text field focus: detected · 24`. Therefore Fcitx metadata
reaches X11Service and the app UI. It does not prove which later IME gating/focus
step rejected the automatic request. Missing wallpaper/system-bus warnings in
that screenshot do not show failure of the private Fcitx session-bus path.

## Minimal correction and limits

- Before an automatic IME request when the Activity editor lacks window focus,
  use public View focus APIs to produce an actual SurfaceView gain/loss cycle,
  ending on the Activity-owned editor. Android SurfaceView.onFocusChanged(false)
  requests return of embedded-window focus to the host window. A nonfocusable
  SurfaceView flag alone does not actively reclaim an already focused child.
- Only request IME when the editor has both View and window focus. Wait for its
  onWindowFocusChanged(true) callback if the window-manager notification is late.
  Guard by keyboard generation, desired visibility, closed state and deduplication
  so callbacks cannot reopen a dismissed keyboard or affect another session.
- When the main window already has focus (the working manual-button path), skip
  the focus cycle and retain the direct editor focus request. Live composition
  handling from 1.7.12 is unchanged.
- Automatic input now requires the actual attached X11 input endpoint, rather
  than the optional XFCE startup-window readiness marker. This fixes the logical
  case where Show X11 screen opens a valid screen after a startup-probe timeout
  but desktopReady remains false forever. That fallback is not proven to be the
  cause on this device; the screenshot lacks the old readiness flag.
- X11 details include XFCE readiness and the last IME request's metadata-only
  viewFocus/windowFocus/showAccepted values. No field contents, keys, credentials
  or pointer coordinates are added. showAccepted is not proof of visible IME.
- No hidden APIs, overlay windows/permissions, native changes, renderer rebuild,
  extra polling, process shutdown changes or user-data migration/reset. Only the
  selected visible desktop path changes; ordinary terminal/agent input stays put.

## Evidence and remaining device check

Evidence directory: `.verification/x11-auto-focus/`. Focused tests exercise the
real View gain/loss callback order, editor focus and surface flags restoration,
window gain callback, existing composition/manual/automatic policy, lifecycle,
private process/gesture checks and agent UI regressions. Android SDK mock tests
cannot prove actual cross-process window-manager focus or Samsung/Gboard IME.

After updating, tap the Thunar address field and check automatic opening, live
Latin/Cyrillic typing, Back/swipe dismissal, retap and manual-button fallback.
If opening still fails, capture X11 details again with the new focus metadata.

Android implementation inspected (no upstream source copied into the app):
https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-15.0.0_r1/core/java/android/view/SurfaceView.java
`onFocusChanged` calls `requestEmbeddedFocus`, which delegates to the window
session's grantEmbeddedWindowFocus. Actual behavior still needs device evidence.

## Final verification

- 77 focused JVM tests passed, zero failures/errors. Unchanged shell fixtures previously passed in 1.7.12; they were not rerun just for documentation/focus code.
- Release assembly passed; required runtime definitions verified (13,315 defined classes, 31 reviewed optional unresolved types). Existing Lint tooling dependency warnings remain; clean standalone Lint is not claimed.
- ZIP integrity, com.termux/ARM64/minSDK26/version1.7.13-x11-test/code1228, signature/certificate, private marker scan and exact native JNI contracts passed.
- Baseline native libraries and unrelated assets remain unchanged; desktop scripts match current source. User data has no reset/migration.
- APK: `Termux-Jobs-v1.7.13-x11-test-arm64.apk`, 38930700 bytes.
- SHA-256: `29f67a902b9527d13e0512332082380a29e3a97840229419f21008ee8aa5efc3`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (unchanged).
- Delivered to `/sdcard/Download` with checksum companion; destination SHA-256 verified.
- Cross-process automatic IME opening remains to be confirmed on the device. No GitHub publication.
