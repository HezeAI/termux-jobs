# Private v1.7.44-x11-test / code1259

Same com.termux/signing identity,1258→1259. No data schema migration/reset, no installed
packages/rootfs/selection changes, agent relocation, live session changes or publication.

## Diagnosis and implementation

Developer screenshots show evince removal refused because evince-help depends on it,
then APT reports evince and libandroid-complex-math already newest but our check fails.
Read-only dpkg inspection finds evince `deinstall ok installed`: failed removal changed
requested selection, while actual package is configured. libandroid-complex-math is now
present; read-only evince --version exits0 / GNOME Document Viewer48.4. GUI not asserted.

Python/shell X11 checks now ignore selection and require error flag ok +status installed.
This includes native/guest program groups, installed desktop catalogs, base components,
Firefox and certificate readiness. No unpacked/config-files/reinstreq acceptance.
Termux Evince removal includes only fixed evince-help companion if actually installed;
confirmation discloses it. dpkg still checks all reverse dependencies and preserves
settings/shared dependencies. No force/autoremove/recursive dependency traversal. Removal
failure propagates, never retried or reported success; post-removal checks real absence.
Alpine/guest removal commands unchanged apart from corrected post-state checks.

## Review and minimum device checks

Changes scoped to X11 source and its UI confirmation. No agent launcher, chat, keyboard,
renderer, owner shutdown or background-job implementation edits. Target routing, fixed
argument lists, core guard and dependency refusal retained. Existing paused/hidden job
semantics unchanged. No arbitrary companion inferred from repository/package metadata.

On device: Termux Recommended PDF should read installed after check; open Evince/PDF.
Close Evince, remove through trash: confirmation mentions evince-help; reinstall PDF
normally and reopen. Other packages depending on a selected application must still block
removal. No need to stop X11 for this ordinary application, close the app itself first.
Normal Ubuntu shares package database with current agents; X11 Test is separate. No
claim installation into shared Ubuntu cannot update agents' common libraries.

Final tests and artifact audit recorded below.

## Final evidence

60 Python tests passed (58 routing/native/catalog/Firefox/lifecycle +2 explicit shell/
Firefox selection-state cases);44 JVM UI tests, zero failed/errors/skipped. New cases
include hold/deinstall/purge configured states, broken/unpacked/config-files rejection,
fixed companion presence/absence/unrelated apps, dependency refusal with no retry/force,
and post-remove still-installed failure. Tests use isolated fixtures, never installed
package transactions. Existing UI confirmation build compiles; device layout not asserted.

Release build/runtime check passed:13389 definitions,31 reviewed optional unresolved.
Inherited Kotlin metadata/Lint-vendor warnings remain; no claim of warning-free build.
Archive integrity, com.termux/code1259/minSdk26/ARM64, original v2 certificate, source assets
and Java staging, pinned X11 JNI28/1/5, unchanged renderer/Alpine helper, limited private
marker scan passed. No exhaustive secret scan claim. Evidence .verification/x11-package-state.

APK bytes:39007825; SHA-256:94a2642a5f5918fc1b8e2d1873d74434007ad4fbbb2a6bfd5266a182e23e934b
Certificate SHA-256:dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644
Delivered to /sdcard/Download/Termux-Jobs-v1.7.44-x11-test-arm64.apk; destination hash verified. No APK installation or GitHub release.
