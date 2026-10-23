# Private v1.7.51-x11-test / code1266

## Behavior and compatibility

New editors default to Ubuntu AI. Existing saved ordinary Ubuntu, AI and legacy Termux
selections retain their value. Migration is disabled/grayed for AI and guarded at click.
Environment list shows the AI subtitle and omits uninstalled Test Ubuntu from its built-in
catalog; legacy installed aliases/routing remain compatible. No automatic user-data reset.
The global account fallback to ordinary Ubuntu when AI is absent remains unchanged.

## Removal repair and authorized device cleanup

Only internally generated .jobs-ai/.jobs-delete UUID trees may enter cleanup. Directories
are pinned no-follow before owner traversal permissions are added to the pinned inode.
fd-safe rmtree remains required. EPERM/EACCES on metadata permits unlink of that entry
itself, never following its target; unrelated failures still abort. Source clone checks,
locks, process guards, cancellation, atomic publication and alias restrictions retained.
Fixtures verify sealed directories, refusal of normal aliases/symlink roots, external
symlink/hardlink targets and modes retained, unstatable entries and error paths.

Developer explicitly authorized completion of failed Test Ubuntu deletion. Its original
alias was already absent. Exactly the verified hidden quarantine inode3465579 was removed
under runtime lock and process guards: 40449 entries. Ordinary Ubuntu container2625031
and Ubuntu AI container3619972 retained their identities. Shared X11 APK files were not
deleted. No other environment or Android cache cleanup was performed.

## Evidence

52 Python tests passed; 34 migration/removal tests also passed under native Termux Python
through current Ubuntu/proot against isolated repository fixtures. 43 selected JVM tests
passed. These do not establish Android touch, keyboard or actual application UI behavior.
Source/staging equality checked for all changed UI files including WorkspaceEnvironments.
Build succeeded. APK ZIP, package, version, ARM64, minSdk26, nondebuggable, signing,
JNI/runtime contracts and bounded private-key/token marker checks passed. Inherited
build-tool warnings are not represented as a clean Lint result. Native X11 unchanged.

Developer reported all agents working after migration on the previous build. Current
assistant environment was independently identified as Ubuntu AI. New subtitle/defaults,
removal UI and ordinary Ubuntu X11 launch/programs need developer verification after update.
No APK installation or GitHub publication is part of this change.

## Final artifact

APK: `Termux-Jobs-v1.7.51-x11-test-arm64.apk`; 39034206 bytes. Package `com.termux`, code1266.

SHA-256: `045574b06ca2fb0d0b15e8bff29feaf0743d91f224088cc6ef23bb94a763aa71`.

Certificate: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (retained).
