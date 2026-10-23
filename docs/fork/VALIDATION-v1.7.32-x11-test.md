# Private v1.7.32-x11-test / code1247 · certificates and install volume

Same com.termux/signing identity; code1246 →1247. No resets, migrations, additional
rootfs or simultaneous desktops. Public stable unchanged; no publication requested.

## Evidence and change

Developer reported Debian desktop, photo/PDF viewers and keyboard working. Latest
screenshot Screenshot_20261020_231946.jpg showed Mozilla HTTPS certificate verify
failure and Firefox candidate unavailable. Read-only inspection found Debian's
ca-certificates package and bundle absent; Test Ubuntu already had them. Desktop
installation/check now explicitly includes the package and nonempty trust store.
The Firefox flow also bootstraps/repairs it, so an existing desktop can retry Firefox
without reinstalling XFCE. Cached native APT indices avoid an unnecessary retry of
the already-configured failed Mozilla source before certificates are installed.
TLS and APT signature verification stay enabled. Failure cannot report success.

Both common install windows now show measured download volume after percentages.
Official APT acquire debug output supplies rounded decimal byte sizes; dlstatus's
first number is correctly treated as a file count, not a volume. Parsing is bounded
and locale-fixed inside the private guest installers. Unknown totals are omitted;
volume resets with a new phase and hides during package installation/configuration.
Existing guardian ownership, cancellation and dialog outside-tap protection remain.

## Checks and limits

Tests cover missing/configured/empty trust stores, cached candidate bootstrap,
bootstrap failure, fragmented measured records, invalid/unknown/late totals,
file-count distinction, and shared install UI visibility. Existing target routing,
cross-environment rejection, cancel/progress UI and file safety checks run too.
No installed package, active session or shared SDK is changed by these tests.

Phone verification still needed: retry Firefox installation in existing Debian,
watch its volume during download, then enable PRoot compatibility if tabs crash.
Check the same volume display during another program install. Confirm desktop
check becomes ready with certificates present. Recorded code/build results do not
prove phone keyboard/rendering or real APT network success. Build may retain
inherited Kotlin/Lint metadata warnings; no clean-Lint claim.

A multi-transaction fixture also checks that completing CA installation does not
freeze progress at 99% during the following Mozilla update/download. The final parser was checked with five standalone JUnit tests after adding
this scenario. The preceding integrated run passed 48 JVM tests; 71 Python tests
also passed. Final build/audit evidence is in .verification/x11-certificates.

Final ARM64 release/runtime verification passed. ZIP integrity, com.termux,
version/code1247, minSdk26/ARM64, original certificate, baseline native assets
and JNI contracts (28/1/5), and private-marker scan passed.

APK: `/sdcard/Download/Termux-Jobs-v1.7.32-x11-test-arm64.apk`

SHA-256: `0214ecc4d13e49c75afcf613fd928de5d6adbedd146856ff4f2de6b526c2f635`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
