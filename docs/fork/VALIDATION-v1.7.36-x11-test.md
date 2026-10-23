# Private v1.7.36-x11-test / code1251

Package/signing identity retained, code1250→1251, no reset/migration/new rootfs.
Private test build, no publication. Alpine-only execution compatibility plus shared
X11 startup UI; other environments/agents/package managers/backup remain unchanged.

## Relevant tests, review and logical inspection

Real copied Alpine/musl libraries reproduce: GIO existing-file false with faccessat
EINVAL, and Glycin's Bubblewrap image loader failure. Corrected PNG, SVG and exact
libwnck built-in PNG resource all decode successfully in an isolated repo-local PRoot.
The direct ctypes lookup of libc.faccessat intentionally bypasses interposition and
still reports EINVAL; GIO and normal application calls pass through the correction.

Real host-built preload fixtures verify RTLD_LOCAL and RTLD_GLOBAL constructor ABI,
selection of Glycin's NotSandboxed enum, existing/dangling/missing paths, and preservation
of other access modes/flags. Routing tests ensure no other environment enables it.
The module is an asset, never Android JNI; no filesystem/permission changes or process
kills are added. Underlying musl headers are pinned by SHA-256, repo-local build only.

Decoder isolation is intentionally weakened within the Alpine desktop. A vulnerability
in a malicious image could affect files its processes can access. Android UID/root
permissions and browser sandbox flags are not changed. Security consequence was
explained to the developer before continuing the build. This is not a zero-risk claim.

Startup UI fixtures check milestone-only progress, no timer-driven advancement,
completion requiring readiness, timeout/retry; bounded metadata tests retain the
window-manager readiness marker through fragmented/rolled-over output. Existing
shutdown/cancel/session-owner/late callback tests remain applicable. No recurring
observer or new polling loop. UI sizes/IME/gestures and actual XFCE need device checks.

Initial fixture corrections: an RTLD_NEXT implementation failed for local dependencies
and was corrected to exact ABI handle lookup; the first host test used Android clang
and was switched to native cc. The project's Robolectric supports API28 rather than30,
so the loading panel was extracted into a directly testable view using the existing
API28 runner. These are test/build corrections, not device validation.

Device checks: start Alpine desktop, inspect Application menu/icons, keyboard/Return,
open a PNG/SVG, and Stop while keeping terminal open. Smoke-check Ubuntu/Debian/Termux
startup/stop and indicator. No installed rootfs, shared SDK, app or user processes
were modified by the build/probe. The probe starts only repo-owned copies/processes.

Final selected checks: **50 JVM +87 Python tests passed**. Actual copied-library
PNG/SVG/libwnck-resource probes passed. ARM64 release build/runtime checks passed:
13372 definitions,31 optional missing types with inherited explicit review notes.
Final APK audit: ZIP, com.termux/code1251/minSdk26/ARM64, original signing identity,
pinned native/JNI28/1/5, source assets, absent retired observers and limited private
marker scan passed. Compatibility ELF is3704bytes, ARM64/musl, asset hash
`59a4b9fa6084f702ba23052671f48e2f4466ae40ed32e35b81738fbf8f009d43`. Build retains inherited metadata/deprecation warnings;
no clean Lint claim. Download copy verified byte-for-byte by SHA-256.

APK: `/sdcard/Download/Termux-Jobs-v1.7.36-x11-test-arm64.apk`

SHA-256: `3dd89ada090c4121cbf243c365bff01a40a7d39811954f36809da5e7f6d9e0f8`

Certificate: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

Evidence: `.verification/x11-alpine-crash/`. Phone XFCE startup/input/window/stop
checks for this patch remain pending. No publication or installed-app update performed.
