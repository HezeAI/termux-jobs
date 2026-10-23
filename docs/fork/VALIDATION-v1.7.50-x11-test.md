# Private v1.7.50-x11-test / code1265

## Confirmed app failure and fix

Developer screenshot144851 identifies an actual APK denial at
rootfs/tmp/termux-fork-session-001136f4-58c5-44b1-94da-4690fd10ec7f (opening dir,errno13).
SessionShell explicitly binds app workspace-sessions there. Added exact-depth strict UUID
session dir policy and fixed app agent/preparation endpoints from launcher code. Retain
empty placeholders, do not open/copy external payload, do not re-open source attrs there.
Unknown tmp files/dirs retain ordinary scan/copy/error behavior; source remains intact.
No new permissions, data reset, session stop, installed changes or public release performed.
Same package/cert retained. Metadata path diagnostics from49 preserved.

## Checks

46 Python fixtures passed (migration/removal/installer), including exact app endpoints,
private original source payload retained, copied target empty, unrelated tmp and prefix
lookalikes copied, denied known session endpoint not opened, unknown denied project refuses
copy. 28 migration tests also passed with native Termux Python through Ubuntu/proot against
repository-only fixtures. Not a native APK process test.

Real native interpreter read-only /tmp walk completed1935 entries/0.9s without error.
Full source walk result and final JVM/build/artifact evidence follow. Actual copy into
Ubuntu AI and subsequent agent launches still require developer device validation.

## Real-tree follow-up and explicit source exception

Full native-interpreter fd-relative walk reached725237 entries/276s then SOURCE_CHANGED.
A narrowed diagnostic walk identified rootfs/workspace, empty with mode000; read/open in
Ubuntu/proot changes observed identity. It did not establish a chat-history mutation.
Rootfs excluding root home and workspace then completed152440 entries/27.7s. These are
read-only traversal observations, not a successful snapshot or actual APK migration.

After explicit developer instruction «удали», only that empty rootfs/workspace was
removed with fd-relative os.rmdir (no recursion, no chmod, refuses nonempty, requires
plain directory still mode000). Output confirmed removal. This is the sole authorized
installed-root change in this task; no processes or sessions stopped. No global workspace
exclusion was added to production code: legitimate user workspace/projects remain subject
to normal copy/access safeguards. Follow-up system-tree result below.

10 selected JVM agent environment/UI tests passed; git diff --check passed.

Follow-up rootfs excluding only root home completed152439 entries/29.2s, with workspace
confirmed absent. This confirms the system portion, not an atomic full snapshot of live
user files. No further permission error was observed in that scope.

## Requested automatic preparation

Developer then explicitly requested automatic empty-dir removal. Implemented only for
migration-marked ubuntu plan (not ordinary/removal plan), under source lock/idle check
and initial cancel tick. Literal plain mode000 workspace only, fd-relative rmdir with
ENOTEMPTY/EEXIST protection and no recursion/chmod. Snapshot occurs afterward. Marker is
reported in confirmation. Successful preparation may persist even if copy is declined;
no files are deleted. Existing protected/nonempty paths retain error handling.

49 Python tests and31 actual Termux Python migration fixture tests passed, including new
empty/nonempty/ordinary-mode/symlink cases, source shared lock refusal and pre-preparation
cancel. No native real root was recreated or deleted to test automation: uses repository
fixtures. Manual rmdir above was separately explicitly authorized.

## Final artifact

Final selected JVM environment/UI tests passed after automatic preparation changes.
Final isolated ARM64 build succeeded47s. APK ZIP integrity/no duplicates, package com.termux,
1.7.50-x11-test/code1265, minSdk26, non-debuggable, ARM64 ABI, v2 signature and retained
certificate checked. Final Java staging/assets match source. X11/Alpine native helpers and
JNI28/1/5 unchanged. Runtime definitions13427 with31 reviewed optional unresolved types.
Limited private-key/token marker audit passed. Inherited Kotlin metadata/Lint dependency
warnings remain; clean Lint is not claimed. No APK installation or public release done.

APK `Termux-Jobs-v1.7.50-x11-test-arm64.apk`: 39033770 bytes.
SHA-256 `2213309bde55f39f1906f4bc463c6c88984d714161d8296feed40e65988862f8`.
Certificate SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Download copy rehashed and `.apk.sha256` supplied.

Actual stopped-source migration, copied agent login/history and project checks remain
required on device; successful real copy is not claimed from fixture/traversal checks.
