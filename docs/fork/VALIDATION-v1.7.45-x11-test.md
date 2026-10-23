# Private v1.7.45-x11-test / code1260

Source-only implementation and isolated builds. No installed Ubuntu/Termux changes,
real migration/removal, system package install, active session changes, APK installation
or publication. Read-only no-follow opens succeeded for actual proot base, Ubuntu container,
rootfs and regular manifest. No actual environment lock was acquired for inspection.

## Relevant tests

90 JVM tests passed, including all four Ubuntu AI launch shells, old default preservation,
Codex history/control/project paths, package profiles, store round-trip/invalid value
rejection, operation gate, installed/imported/base trash UI, row selection, active and
referenced-system refusal, plus existing Codex/store/file/environment/preparation tests.
31 Python tests passed: full rootfs copy, private mode/content, new inode independence,
hardlinks, l2s prefix mapping, external symlink preservation, live native shared locks,
cancellation and stage cleanup, source mutation before/during copy, existing destination,
atomic no-replace race, linked source, low-space refusal, guest os-release link, required
package database, special-file refusal, safe environment deletion, identity swap restore,
read-only staged-directory cleanup, existing environment installer regressions and clean
ubuntu-ai installation with no ordinary Ubuntu created or replaced.
Isolated test_agent_preparation.py also passes component/consent/key-privacy/failure cases.

These are isolated source tests, not a full device migration or cloud-auth validation.
No UI screenshot/frame/DPI proof is inferred from Robolectric. No renderer changes.

## Review and logical inspection

Aliases are fixed/validated and token-bound; direct argv avoids injection. Symlinks,
source mutation, target overwrite and native lock conflict fail closed. Regular files
are copied/read-back checked, source retained. Environment deletion is explicit, guarded
by library/live references and native locks and has no process stop path. Background
worker emits only fixed error codes/counters, not file contents/keys. Launch routing was
traced through all four CLIs, editor queries, Codex history and file/package operations.
Existing IPC binds and per-run identifiers are retained. General app-global account
connection management is not a per-rootfs boundary. See AGENT-ENVIRONMENTS.md limitations.

## Minimum developer device validation

Install the APK normally. Check old presets still show Ubuntu. Open an agent's settings:
Ubuntu selector, launch check with selected path, migration immediately below check.
While the old Ubuntu/desktop is live, migration must refuse without stopping it.
After closing those sessions, scan/confirm/clone. Check byte progress, outside-tap guard
and explicit cancellation on a small first trial if desired. Switch one preset first,
verify files/packages/history/auth in Ubuntu AI for the agents in use, then switch others.
Do not delete either source or Ubuntu X11 Test until actual post-copy validation. Verify
trash for an unused imported test environment; references/live root must block deletion.
Fresh-system path also needs device validation on a separate clean installation.

Final build and APK audit follow below.

## Final artifact

Final build succeeded; runtime13418 definitions,31 reviewed optional unresolved types.
Inherited Kotlin metadata/Lint-vendor warnings remain; not a warning-free lint claim.
90 final JVM tests and31 Python tests passed, plus isolated agent-preparation script.
New global account-backend preference check covers Ubuntu AI after source removal.
ZIP integrity/no duplicates, package com.termux/code1260/version1.7.45-x11-test/minSdk26/
ARM64/original v2 certificate, source asset/staging Java equality, unchanged pinned X11
renderer/Alpine helper, JNI28/1/5 and limited private-key/token marker scan passed.
This does not claim an exhaustive artifact privacy audit or device scenario coverage.
No actual runtime migration or removal was performed. Native tests use synthetic roots.

Bytes:39026174
SHA-256:0869ce337ed2f2252aa2d09e91eeed44f7269c2e09cae6034a30cbcae56b6882
Certificate SHA-256:dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644
Delivered:/sdcard/Download/Termux-Jobs-v1.7.45-x11-test-arm64.apk; copied artifact hash verified. No APK installation/publication.
