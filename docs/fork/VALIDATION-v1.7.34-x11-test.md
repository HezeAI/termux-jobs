# Private v1.7.34-x11-test / code1249 · Alpine and Termux X11

Same package/signature, code1248 →1249. No data reset/migration, rootfs creation,
parallel desktop servers or publication. Developer confirmed Ubuntu/Debian desktop,
programs and sound in v1.7.33; Alpine/Termux remain unverified on a device.

## Scope and review

Expose the existing desktop card and switching in Alpine and native Termux.
Platform-specific package checks/install/programs use APK or native Termux APT;
no Ubuntu redirection. Only reviewed fixed arguments and validated package names
reach package tools. Base and recommended names were checked against official
Alpine v3.24 main/community and Termux main/x11 repository metadata, cached only
under .verification/x11-alpine. Installed Alpine release/repositories were read-only.

Existing desktop lifecycle, guardian ownership, hidden/display switching, keyboard
focus/readiness, audio cookie/private UNIX socket and cancellation are retained.
Existing Debian/Ubuntu data/commands are preserved. All four targets now omit the
completed diagnostic GTK/Python observers, their executable APK assets and optional
capture executor/startup; diagnostic commands are rejected. Historical source stays
in the repository. Necessary keyboard and desktop startup signals remain.

Program catalog for the two native backends includes programs and libraries, as
explained in UI, while Installed is ownership-checked .desktop applications. Parser
limit40,000 accommodates Alpine; disk cache remains bounded32MiB and page size30.
APK removal refuses plans deleting extra packages; this may prevent removing an
app with newly orphaned dependencies rather than silently deleting those too.
Unknown file checksums/edited launchers are preserved by permission repair.

## Verification and device limits

Fixture tests cover exact native package mapping, APK ownership/checksums, versioned
package names, install verification, protected/core and widened removal plans,
native PREFIX/home Firefox wrapper, Alpine-vs-Termux routing, direct Termux runtime/
audio path and retained readiness/focus without retired observers. JVM tests cover
exact target selection, cross-desktop rejection, separate installation state, APK
stage ratios without fake byte counts, observer-command rejection and absent retired
assets, plus the prior installation/cancel/log/catalog/file-state behavior.

Build/tests do not verify real native desktop startup, APK network/package scripts,
IME gestures/Return, sound, every codec, musl GTK applications, native LibreOffice
or performance. No installed packages, rootfs, user process or shared SDK changed
by development. Build may retain inherited Kotlin/Lint loader warnings.

Device sequence: stop the currently running desktop; open existing Alpine, check
and confirm component install, then start XFCE. Test keyboard/Return, window resize,
background, one recommended program, PDF, Firefox and audio/video. Stop Alpine's
X11; repeat from the ordinary Termux session. Verify returning to terminal hides
without stopping and closing the owning session stops only that desktop. Ubuntu
and Debian should receive a quick keyboard/desktop smoke check after removal of
observers. All four use one server at a time; simultaneous desktops are deferred.

Final automated checks: **62 JVM and84 Python tests passed**. This includes the
new target/observer-retirement tests and the archived recorder's standalone fixtures;
the latter are historical algorithm tests, not a bundled observer. An initial
Robolectric check found source asset access bypassing Gradle's asset exclusion;
the retired Python recorder/license were moved outside source assets and the
absence test then passed. Final packaging is checked independently in the APK.

Final ARM64 release/runtime and APK audit passed: ZIP integrity, com.termux,
code1249/minSdk26/ARM64, original signature, native/JNI contracts28/1/5, source
assets, retired diagnostic asset absence and private-marker scan verified.

APK: `/sdcard/Download/Termux-Jobs-v1.7.34-x11-test-arm64.apk`

Bytes: 38993000

SHA-256: `0b03d9c08588dba4d2f445c5f8a8f0f5071f98df8dca19d456749f2b12691e86`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
