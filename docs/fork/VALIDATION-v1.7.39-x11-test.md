# Private v1.7.39-x11-test / code1254

Same com.termux package/certificate;1253→1254. No data reset, package changes,
rootfs recreation, installed-app changes or publication. Firefox status changes
are scoped to desktop program management; agent chats and terminal sessions unchanged.

DNS accepts both fixed relative .mozilla/firefox and .config/mozilla/firefox roots.
Read-only inspection confirmed Alpine uses the latter. External profiles, symlinks,
malformed journals and user edits retain existing protections. DNS remains opt-in.
Error message no longer suggests an installed desktop is missing.

Non-package-changing service jobs retain known recommended-program items and DNS
state for the same target. UI errors retain known items unless the failed/cancelled
operation could change packages. Installation/removal/catalog-install failure still
invalidates uncertain statuses. New UI regression verifies a DNS error keeps known
installed and missing states; package-changing failure invalidates them.

8 targeted JVM UI tests and43 Python tests passed (profiles/maintenance/native/program
routing). Includes new XDG-only and simultaneous legacy/XDG profile restore cases.
Source call/state review confirms previous item bundles are copied, not shared,
across async jobs, and cross-target states are excluded. No runtime session changed.

ARM64 release build and APK runtime definitions audit passed:13384 definitions,
31 explicitly reviewed optional unresolved types. ZIP, package/code/minSdk26/ARM64,
original signature, source assets, pinned libXlorie and JNI28/1/5 verified. Private
marker scan passed with its usual limited scope. Inherited metadata/deprecation
warnings remain; no clean Lint claim.

Developer screenshots of1.7.38 show native Termux Firefox YouTube works with PRoot
compatibility on and DNS HTTPS off. This does not prove a causal fix or all environments.
Developer reports automatic desktop show still fails across environments; **this
build does not fix automatic show**. That mechanism is under separate investigation.
New profile/status behavior still requires device verification: cause a browser
setting error and confirm installed/missing buttons remain, then enable/disable DNS
after closing Firefox. No claim of exhaustive desktop verification.

APK: `/sdcard/Download/Termux-Jobs-v1.7.39-x11-test-arm64.apk`

SHA-256: `592f384d5e5b096f6e6afba969b1b1dc8d1124e388aec5bd8b1c0cd8b5bc04e8`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
