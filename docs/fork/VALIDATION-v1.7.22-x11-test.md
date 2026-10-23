# Private X11 observation · 1.7.22-x11-test / code1237

Public stable remains v1.7.5. Same com.termux package/signing identity; versionCode
increases from 1236 to 1237. No data reset, environment replacement or history deletion.

## Changed behavior and review

Manual start/stop/restart replaces the automatic three-minute observer. Session/serial
checks reject stale acknowledgements. Writes are atomic fixed-path data with symlink
checks. Acknowledgement failure remains explicit. The recorder and event backlog are
bounded; only a new capture resets its memory, not exported reports. Closing diagnostics
does not stop recording. Desktop shutdown uses the existing owned guardian process path.
Read-only metadata snapshots and monitor subprocesses are stopped through their own
Popen handles with bounded waits; no user-supplied PID or shell command controls them.

Metadata excludes raw window classes/titles, image paths/contents, typed keys and pointer
coordinates. Ordinary program stderr remains separate and can contain program paths.
Backdrop --inspect performs no configuration repair; default startup migration remains.
Surface cancellation releases physical mouse buttons and delayed touchpad gestures.
No GPU, compositor, native library, agent or ordinary session rendering change is made.

## Automated evidence

148 JVM/Robolectric tests passed (0 failures); includes control parsing, atomic file
validation, capture reset/bounds, touchpad cancellation, surface button release and
existing session/server diagnostics and chat checks.
60 Python/shell tests passed; includes manual no-deadline controller start/stop/restart,
invalid controls, read-only backdrop inspection, pointer transitions, private PTY
notification delivery, metadata privacy and existing desktop/guardian/environment paths.
Logs: .verification/x11-manual-observation/{jvm-tests,script-tests}.log.

## Device checks still needed

Update and restart the graphical desktop to load the new helper. Start observation,
reproduce Applications flicker, change wallpaper/move icons, stop observation and export
to Download. Confirm manual stop/restart, button state after surface/keyboard/tab changes
and backdrop timestamps. Automated tests do not establish phone keyboard, gestures or
visual smoothness. The flicker cause is not yet proven; no resolution is claimed.

## Build

Release assembly completed successfully. Existing Kotlin metadata/Lint loader warnings
remain in the build log; this is not a claim of a clean Lint run. Final packaged
DEX/runtime and artifact audit are recorded below after completion.


## Final artifact

ZIP integrity, package com.termux, code1237/name1.7.22-x11-test, ARM64 ABI, minSdk26,
non-debuggable manifest and v2 signature verified. Certificate unchanged:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
SHA-256: `478ec4ef029ead9dc54edac4abe810db3d6a438fb84305d866a576d1552f9ebc`. Size: 38976849 bytes.
Pinned X11 native library/keyboard archive and JNI contracts 28/1/5 verified.
Baseline native/assets comparison passed (expected environment-runtime and new X11
assets only). Private key/token marker and sensitive filename scan passed; this scan
is not a proof of absence of every possible secret. Runtime inventory: 13,372 defined
classes, no required or unreviewed missing types; 31 previously reviewed optional types.
Evidence: .verification/x11-manual-observation/apk-audit.json and
.verification/arm64/apk-runtime.json.
