# Private X11 check · 1.7.14-x11-test / code1229

Public stable remains v1.7.5. Local test build, no GitHub publication.

## Device evidence and code diagnosis

The developer confirms that 1.7.13 opens the Android keyboard automatically and
supports live typing. Opening/closing IME changes the apparent icon sizes and
hiding it produces a small blink. No device timing/renderer trace was collected,
so the precise origin of every transient blink is not established.

The previous code forwarded each Android SurfaceView height change to the remote
SurfaceControlViewHost. LorieView then advertised that new height to X11 with
sendWindowChange, prompting a desktop mode/client relayout. This is a confirmed
source path capable of causing the reported resizing.

## Change

- X11Viewport separates full desktop size from the currently visible height.
  Use current WindowMetrics bounds, stable system-bar/cutout bottom inset and
  the surface's position within the window to estimate unobscured height. Keep
  the larger observed baseline for edge-to-edge layouts while geometry is stable.
  This also supports attaching when the keyboard is already shown.
- Current width, window bounds, top position and stable inset form the geometry
  identity: rotation, split-window resizing, navigation/header geometry changes
  replace the baseline; an IME-only visible-height change preserves it.
- Send full width/height and a validated visibleHeight over the private Binder.
  Reject invalid crops before host mutation. Deduplicate identical payloads and
  host relayouts, resetting transport counters on detachment.
- Preserve the remote host/renderer surface dimensions while IME obscures the
  bottom. Pass hiddenBottom to the pinned renderer's existing setViewport API.
  Its native implementation crops source and destination proportionally, keeping
  scale, and pans the visible region to the mouse pointer. It follows the pointer,
  not a separately tracked text caret. Main Android SurfaceView still follows its
  available UI area, so this does not prove elimination of all Android surface blink.
- LorieView only advertises X11 mode changes for actual logical-size changes;
  reset that cache when connecting a new display FD so first/reconnected clients
  still receive their mode. No native library or JNI descriptor changes.
- Existing keyboard focus bridge, live composition, toolbar and terminal/agent
  preferences are untouched. No global soft-input mode, Linux packages, running
  sessions, process-stop code, user data, permissions or authentication changes.
- X11 details now include full viewport size and visible height to compare a
  keyboard-open and keyboard-closed report, without input contents/coordinates.

## Relevant verification

Evidence: `.verification/x11-stable-viewport/`. Focused JVM checks cover keyboard
open/hide geometry sequences, initially visible IME, real rotation/split-window
changes, header/inset changes, edge-to-edge baseline, floating IME without resize,
bounded geometry and invalid crop rejection, alongside prior IME/focus/gesture,
service ownership, lifecycle/menu and agent UI tests.

Logical inspection traced initial attach, size-only RPCs, no-op remote relayout,
hidden viewport updates, native callback coordinate mapping, actual mode change,
reconnected FD, detach/reset and late events after close. The pinned renderer was
inspected at its existing hiddenBottom crop and pan logic. No native rebuild or
new background polling/timer is introduced. Unchanged shell fixtures are not
rerun just for this Java-only geometry change.

Device check: compare desktop icons/window sizes before IME, while it is visible,
and after hiding; type into fields near the lower edge, move/tap the pointer after
panning; repeat automatic/manual opening and closing, switch terminal/agent tabs,
and rotate once if convenient. Actual clipping, touch transforms, IME animation,
DPI and any remaining blink require device evidence, not just mock tests.

## Final artifact results

- 86 focused JVM tests passed, zero failures/errors.
- Release build passed; 13,318 defined classes, 31 reviewed optional unresolved types; required definitions present. Existing Lint tooling dependency warnings remain; clean standalone Lint is not claimed.
- ZIP integrity, package/versionCode/minSDK/ABI, unchanged signing identity, pinned inputs/JNI declarations and private-marker checks passed.
- APK `Termux-Jobs-v1.7.14-x11-test-arm64.apk`, 38932428 bytes; com.termux/code1229, minSDK26, ARM64.
- SHA-256: `8a36a50ba3dc8e06ae7ad307bc78f5ac5c5bec5b51e60260f9fb218fe7dc83ed`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Baseline native libraries/unrelated assets unchanged; current desktop scripts match the APK. VersionCode increased from 1228; no data reset/migration.
- Copied to `/sdcard/Download` with companion checksum; destination SHA-256 verified.
- Actual stable scale, panning/touch and blink reduction remain for device checks. No GitHub publication.
