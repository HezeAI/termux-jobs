# Private X11 follow-up · 1.7.21-x11-test / code1236

Stable public release remains v1.7.5. No publication or app installation is part of
this task. Same com.termux/signing identity, increased versionCode 1235 -> 1236.
No reset, environment replacement, removed user file or cleared history/cache.

## Confirmed issues / changes

Screenshot Screenshot_20261020_174457.jpg shows the long diagnostic message consuming
the dialog, hiding export/actions. This was a layout error in 1.7.20. The dialog now
uses the existing bounded DiagnosticsPanel shared with sessions. X11-specific Source
hooks retain default behavior for ordinary sessions/server reports. Export is via
the existing off-thread report writer and shared-storage permission flow.

The developer reports a black desktop after a tab change. Source inspection finds
a permitted old/new native socket cleanup race when native renderers are destroyed
and recreated on tab switches. Instead the service now retains one renderer/socket
across host detach/reattach; the detached surface is suspended and gestures cancelled.
Input without a live matching host is rejected. The renderer is explicitly destroyed
at service shutdown. Standalone renderer ownership is unchanged. This prevents that
connection churn but is not a phone-confirmed resolution of every black-screen case.

Native initial screen name and adapter window-change name were inconsistent. Keep
screen stable and copy the prior valid selected wallpaper to missing/default canonical
settings on a new desktop launch. Existing custom destination selections, including
unavailable paths, and all legacy settings are preserved. Missing default fallback
continues. The helper does not delete images or replace desktop environments.

## Review / lifecycle

Same UID/private service/Binder authorization and cookie boundaries; no new listener,
process or privilege. Native binary, keyboard, pointer gestures, compositor setting,
process ownership/termination and unrelated agents/terminal paths stay unchanged.
On detach/token death: drop host/input attachment, suspend native window, retain one
renderer connection. On a valid new host: reuse renderer, update geometry, reattach
surface. Stale detach/death callbacks cannot affect a different current attachment.
On actual service shutdown: release host and native renderer, then the existing own
process shutdown path. Renderer retention keeps bounded state while hiding the desktop;
phone checks are needed to confirm redraw and idle behavior on this Android version.

## Verification

142 JVM/UI checks passed, with zero failures/errors, including existing session/server
diagnostics behavior, X11 header/keyboard/viewport and agent chat checks. New checks
cover compact diagnostics export outside scroll ancestry, auto-refresh without
replacing actions, service-retained renderer detach and exactly-once explicit release,
unchanged release-on-detach for nonretained renderers, and fragmented/bounded
backdrop startup metadata retained independently of later log rollover. Only native methods are
stubbed for ownership tests; actual SCVH/GPU transfer remains a device check.

54 isolated Python/shell checks passed, including non-destructive legacy backdrop
selection copying, preservation of custom selections, fallback, decode failure and
existing guardian/program/environment/startup paths. Fixtures do not change installed
rootfs or processes. Final isolated ARM64 release build passed (113 tasks). Runtime definition audit
passed: 13,363 defined classes, 31 remaining reviewed optional/platform types. ZIP
integrity, package/version/ABI/minSdk, v2 signature and identity, pinned native/keyboard
assets, exact script/image assets, JNI 28/1/5 contract and private-marker checks passed.
Download destination was rehashed after copying.

APK: `Termux-Jobs-v1.7.21-x11-test-arm64.apk` · 38,970,565 bytes.

SHA-256: `52f66679fdf1080cf69cf3608e0a65ea82b3a45f71fb4d0906253316a36434b4`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.


## Device checks

Update over the current test APK. Fresh desktop start is needed for the fixed monitor
identity/backdrop selection. Test selected wallpaper, Applications, keyboard and Return.
Switch to Home/another tab and back several times; verify display remains visible and
pointer/input work. Diagnostic Overview/Events must scroll independently of visible
Download/Copy/Copy path/Close controls. Save to Download through the permission flow.
Capture the report soon after reproducing; the X11 observer runs for the first three
minutes. Actual GPU frames, SCVH transfer, Android permissions and wallpaper visibility
are not established by JVM/mocked tests.
