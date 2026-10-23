# Private v1.7.48-x11-test / code1263

## Findings and scope

Independent read-only fd-relative walk in current Ubuntu/proot: 417536 entries /51700
directories /160s (incomplete), failures opening rootfs/apex, rootfs/data/app and
rootfs/data/dalvik-cache with errno13. Separate absolute reads found all three directories
empty; installed proot-distro login sources document host Android binds at these paths.
No private file content was read. This is a matching local reproduction, not proof of
the exact failing Android app process path or successful migration after correction.

Fixed Android host bind directories are excluded from descent, recreated as empty target
directories, and disclosed before copy confirmation. Snapshot retains their own identity;
normal file/project paths retain full validation. Source is not chmodded or modified.
No permissions bypass, automatic key omission or catch-and-continue for general failures.
No selective-components UI has been added. Cancellation/source-busy/no-replace publication
and read-back verification remain. No native X11/rendering/agent chat changes.

Actual Termux Python inspection found missing os.link and xattr APIs, but available Bionic
fd functions. Added bounded ctypes fallback preserving xattrs and hardlinks without path
following or replacement. This avoids future AttributeError after the scan. Keyboard
help embeds the actual workspace_interactive_input vector, not a text approximation. Tests and final artifact results follow below.

Device checks needed: retry migration (source Ubuntu must be stopped), then verify copied
project, agent login/history and tools before switching additional presets. Source remains.

## Automated checks

43 Python fixture tests passed (migration/removal/installer suites), including reserved
Android/pseudo-filesystem placeholders, normal similarly named project data, access error
refusal, native xattr/hardlink fallbacks, no-replacement hardlink failure, source retention,
copy read-back, mutation, cancellation, cleanup and publication races. 25 migration tests
also passed with actual installed Termux Python against repository-only fixtures. These
native interpreter tests ran through current Ubuntu/proot, not the Android APK process.

145 JVM tests in 21 selected suites passed, including agent environment/store/routing,
Codex behavior, desktop/program UI and automatic keyboard state/persistence. This does not
prove phone IME/gesture behavior. git diff --check passed. Final artifact results below.

## Read-only source walk after correction

New policy walk, actual native Termux Python through current Ubuntu/proot: progress reached
612897 entries/225s without the previous access failures; later aborted with SOURCE_CHANGED.
No successful complete snapshot is claimed. Source was live during ongoing development/
builds, so this result is not a migration validation. No real source locks were acquired,
no clone/remove was run, and source permissions were not changed. A stopped-source retry
from the app is required. Internal failed-walk path context is not serialized in errors.

After replacing the textual symbol with the exact toolbar vector, 31 keyboard/UI
tests were rerun and passed. The test checks the rendered ImageSpan and persistence.

## Final artifact

Isolated final ARM64 release build succeeded in39s. ZIP integrity/duplicates, package
com.termux, version1.7.48-x11-test/code1263, minSdk26, non-debuggable, sole ARM64 ABI,
v2 signature, retained certificate and matching source/assets/staging verified. X11 native
library and Alpine helper unchanged; JNI contracts28/1/5 unchanged. Runtime definitions13427,
31 reviewed optional unresolved types. Limited key/token marker audit passed. Inherited
Kotlin metadata/Lint dependency warnings remain; clean Lint is not claimed.

APK: `Termux-Jobs-v1.7.48-x11-test-arm64.apk`, 39032358 bytes.
SHA-256: `b38bc5f3f7cc0dd222830b7583c57b10cb8939e2bdf9a45fe443387b127b122a`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Download APK copied, rehashed and accompanied by `.apk.sha256`.
No APK installation, GitHub publication, actual source clone or session stop performed.
