# Private X11 diagnostics · 1.7.20-x11-test / code1235

Public stable stays v1.7.5. No GitHub publication is authorized by this task.

## Findings and scope

The developer reports Applications flicker persists with compositor-on in 1.7.19. A separate full-header rebuild is confirmed in code: the X11 keyboard-state callback called renderHeader(), which removes/recreates the title and surrounding controls. It now updates only the existing interactive-input button. This fixes that unnecessary reconstruction; visual impact and Applications behavior require device observation.

A screenshot alone does not determine live X11 focus loss versus menu-window recreation. The new bounded startup recorder distinguishes observable X11 popup window lifecycle and sampled focus changes. GTK widget-only rebuilds, pointer grabs, very short transitions and the rendering pipeline remain possible gaps. Existing 1.7.19 has no such recorder, and no existing process was injected, restarted or reconfigured for a live trace. Local mounts expose Termux home/usr but not the app's private Xauthority file, so this session cannot safely attach the new authenticated observer to the already-running desktop. The next fresh launch with this APK supplies the observer inside the app-owned authenticated guest session.

Wallpaper screenshot records failure opening the missing built-in xubuntu-wallpaper.png. Read-only settings inspection finds chosen shared-storage images exist at their direct host paths; prefixing those with rootfs incorrectly makes them appear missing. Selected files are retained. New GdkPixbuf diagnostics distinguish unreadable/invalid/skipped/successful decoding and identify workspace/style/cycle metadata without image paths. The fallback only replaces absent built-in selections, not selected custom images, and does not claim to fix every black-background case.

## Compatibility and review

Same com.termux/signing identity, versionCode 1234 -> 1235. No reset, data migration, native-core/renderer, viewport, pointer gesture, Fcitx input or compositor setting change. Existing environments and agents retain their behavior; only the X11 input callback uses button-local highlighting. New helper assets stay in private control storage and run inside the existing owned guardian group. No listening service, public component, system package install or shared SDK/rootfs mutation by the build process. Startup fallback settings changes occur only when the developer starts the updated desktop.

Observer counters/IDs contain no keys, titles, pixels or secret values. Raw desktop stderr already contains paths and is retained as such. Export is user-triggered to a unique text file via existing storage permission handling; no network upload. No new process-kill paths. Helpers stop with the same owned group; observer exits itself after 180 seconds. Failed decoder/observer paths do not abort the desktop.

## Verification

53 isolated Python/shell tests passed: popup hide/remap vs destroy/new, sampled
focus changes, bounded metadata, missing-display failure without secret-bearing
payload, invalid images, real GdkPixbuf decode of the shipped PNG, failed settings
reads and preservation of custom choices, missing-default replacement, existing
launch/readiness/guardian/program/environment fixtures. No installed desktop or
rootfs is used by these fixtures.

120 JVM/UI checks passed across 20 suites. New checks assert stable header/title/
navigation/button identities on the X11 keyboard callback, bounded Android event
history, diagnostic refresh without dismissing and presence of the Download
export action. An initial test-harness SDK30 configuration was unsupported by the
local Robolectric setup; the metadata-only trace check was rerun with supported
SDK28, and the final suite has zero failures/errors. These tests do not prove
physical IME behavior, Xlib event capture on the phone, Android storage permission
flow, visual menu stability or final wallpaper rendering.

Final isolated ARM64 release build passed (113 tasks). Runtime definition audit
passed: 13,367 defined classes; 31 remaining optional/platform types have explicit
review notes. Final ZIP integrity, package/version/minSdk/ABI, v2 signing identity,
unchanged pinned native core/keyboard/JNI contract, exact new helper assets and
private-marker scans passed. The delivered Download copy's SHA-256 was verified.

APK: `Termux-Jobs-v1.7.20-x11-test-arm64.apk` · 38,969,129 bytes.

SHA-256: `f900a6eb0474fd1dba05536bfced041b073cf2668e85290d4adf5df772fd98c9`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.


## Minimum device check

Update over the current test APK, stop only the graphical desktop using its app control, and start Ubuntu X11 Test again. During the first three minutes reproduce Applications flicker with the pointer stationary, then toggle interactive input once, show/hide the keyboard and try the selected wallpaper. Open Diagnostics X11 and export To Download immediately. Android dialog focus events at export are expected. Retest working resize, pointer and Return; do not clear app data or close agent sessions. Menu cause and custom wallpaper rendering are not declared fixed before this observation.
