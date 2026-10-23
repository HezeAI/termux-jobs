# Private v1.7.42-x11-test / code1257

com.termux/signing identity retained,1256→1257. No data resets, rootfs replacements,
installed package/mirror configuration writes, APK installation or publication during
verification. All builds, copied probe components, private bus, test image and generated
thumbnail stay inside the repository. Installed processes/environment were read only.

## Changed behavior and tests

Separate Programs chip beside Terminal/Desktop; ordinary Programs menu opens whether
busy or idle. The switch menu retains switch/return and conditional Stop in both views.
Programs Back closes its own UI. No agent session rendering or toolbar layout changed.

An unchanged WorkspaceTransferBorder overlays only Programs while installation continues
with its window closed. Existing main poll toggles active state; drawable frame callbacks
invalidate themselves, never rebuild parent UI.16ms scheduling matches the server button.
Detach/hide/onStop disables it; foreground selected environment resumes read-only snapshot
polling after dialog close/return. Hidden channel UI polling stops; service jobs do not.
No extra package scans or model/SSH requests. Actual screen frame rate is device dependent.

47 relevant JVM tests passed: desktop UI/programs/header/install/facade/cache and new bar
cases. Verify independent button actions while busy, retained child identity and layout,
16ms scheduling/unschedule/hide/detach, selected-environment state, visible-only polling,
compact error UI with full separate details, existing owner/cancel and navigation behavior.
Legacy Robolectric PathMeasure is replaced by a test-only fixed-length path for scheduling;
this tests callbacks, not rendered pixels or phone frame rate. No clean Lint claim.

3 Python compatibility tests passed, including real compiler/ABI tests with fake exact
Glycin symbols in RTLD_LOCAL/GLOBAL scopes, filesystem fallback and isolated private-bus
inheritance. Loader and creator hooks both verified.

## Alpine thumbnail evidence

Read-only process inspection confirms live Thunar/Tumbler share their bus and receive
the preload; image libraries/plugins are loaded but thumbnail caches are empty. It did
not send requests to the live user's bus or steal pipe output.

Copied binaries/dependencies/plugins under .verification plus an owned private D-Bus and
repository default test.png reproduce the thumbnail error: loader runs without sandbox,
then PNG-save creator attempts Bubblewrap and signals Error. Adding gly_creator_new hook
with its separate selector produces Ready +Finished and a64x64 PNG289bytes, with all chunk
CRCs valid and Thumb::URI=file:///test.png metadata. Private-bus teardown warnings are
expected when that test bus exits. Original environment and its running Tumbler untouched.

This establishes an isolated regression/fix, not phone rendering verification. Inherited
Glycin isolation reduction now explicitly covers PNG encoding as well as decoding inside
Alpine's owned desktop. Native X11 renderer, Android/Firefox sandboxes and other distros
are unchanged. Restart the Alpine desktop after updating before the phone check.

## Termux LibreOffice screenshots

Developer screenshots113610/113614 show HUST read errors on MariaDB/PostgreSQL downloads;
APT retry options are already present. It did not complete installation. This is no proof
of LibreOffice runtime incompatibility. No repository switch/TLS/signature bypass applied.
Raw8KiB log no longer stretches the whole Programs page: compact reason +Details scroll.

## Minimum phone checks

Check both chips on a narrow display, in terminal/desktop views. During installation close
the Programs window; confirm border animation and unchanged terminal input, switch tabs,
return and verify current job. Opening Programs must always show ordinary navigation.
Confirm line stops on completion/Stop/error and is inactive on other environments/agents.
Restart Alpine desktop and inspect image thumbnails. Check failed download Details and
menu navigation. Phone input, actual frame pacing and these behaviors remain unverified
by UI mocks. APK final identity/hash/audit evidence follows after verification.

## Final APK audit

Build/runtime audit passed:13389 definitions,31 reviewed optional unresolved types.
ZIP integrity/package/code1257/minSdk26/ARM64/original v2 signing certificate, source
assets +changed Java staging, pinned renderer, JNI28/1/5 and limited private-marker
scan passed. Inherited Kotlin metadata/Lint dependency warnings remain.
The scoped Alpine compatibility binary is4080bytes, SHA256 `8d894105fc8e326b7c2dd9a2baaab869218b4a9c5f3b4b063dd5f09db07cfe17`.
No schema changes/data resets were introduced. Destination digest matched.

APK: `/sdcard/Download/Termux-Jobs-v1.7.42-x11-test-arm64.apk`

Bytes: 39007533

SHA-256: `0db0207264d78f44e951367a8a48d1fdf0e63fdcaf75196f63ab3966fbaa4abe`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
