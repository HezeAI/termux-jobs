# Private v1.7.43-x11-test / code1258

Same com.termux/signing identity,1257→1258. No data schema changes/resets, environment
replacement, mirror edits, system package installation, APK installation or publication
during this verification. Source/build/test outputs stay in repository-owned staging.

## Navigation and catalog preparation

Programs main exposes Recommended, Installed, Search and install programs in that order.
The searchable paged list lives only in the new third section. Existing per-card install,
Stop, completed status/trash, background ownership, error details and separate header button
are retained. Back returns from a section to main; closing UI does not cancel the service.
Late page messages are ignored outside list views. Delayed search callbacks and old adapter/
button references are cleared when changing views. No renderer/input/agent frontend changes.

One constant Prepare and refresh catalog button uses the existing allowlisted catalog-install
request after confirmation. Guest APT flow checks all configured catalog tool packages before
installing. Prepared tools skip installation and refresh once. Missing tools install normally
then refresh again so AppStream's APT configuration provides metadata. Failures cannot emit
READY. Termux/Alpine update native indexes only, not AppStream. No trust/TLS/mirror bypass,
outer transaction retry or new arbitrary command API. Existing APT retries remain.

44 relevant JVM UI tests passed: new main layout/order, search/list absence on main,
search/debouncing/late-page behavior, one preparation action in ready/missing states,
Back/navigation while busy, progress/reopen/Stop, cache protection, header/border lifecycle
and prior desktop behavior.33 Python tests passed: routed guest/native/catalog/retry cases,
conditional install-vs-refresh and preparation failure. The guest interpreter is virtualized
only in fixtures; production still pins /usr/bin/python3 for the APT catalog reader.

## Evince finding and fix

Developer clarified failing app is Document Viewer/Evince; Mousepad works. A read-only
version probe fails before main:
CANNOT LINK EXECUTABLE evince: libandroid-complex-math.so not found, needed by libsynctex.so.
Installed dpkg status has evince48.4-1 and libsynctex1:2026.0-2 configured; the latter's Depends
contains zlib without the required math helper.67 Termux ELF files were read; the only missing
library name in that graph was libandroid-complex-math.so. No GUI or installed session was
launched or changed. Official Termux package sources also identify this helper:
https://github.com/termux/termux-packages/blob/master/packages/texlive-bin/build.sh

Termux PDF group and pkg:evince alias now request evince +libandroid-complex-math. Incomplete
readiness exposes Install in Recommended, permitting normal dependency completion without
uninstalling Evince or documents. Alpine/Ubuntu/Debian PDF groups are unchanged. Native
mapping is tested; actual package fetch/linker/GUI result after repair remains for developer
phone validation. No claim that all future Evince launch failures are resolved.

## Minimum developer checks

Open Programs: three sections, no full list/search on main. Enter Search, use one Prepare
and refresh button for a new/prepared catalog. Check scrolling, search, install/Stop,
section Back, and navigation during refresh. Recheck separate Programs border while a
program installation continues in background. In Termux run Recommended check and install
the incomplete PDF group, then open Document Viewer/PDF. No need to uninstall it first.
Final APK build/identity/hash audit follows below.

## Final artifact

Build/runtime check passed:13389 definitions,31 reviewed optional unresolved references.
ZIP integrity, package/version1258/minSdk26/ARM64, original v2 certificate, source assets
+changed Java staging, pinned X11 renderer, JNI28/1/5, unchanged Alpine helper from1.7.42
and limited private-marker scan passed. Inherited Kotlin/Lint dependency warnings remain;
not a clean Lint claim. Destination digest verified.

APK: `/sdcard/Download/Termux-Jobs-v1.7.43-x11-test-arm64.apk`

Bytes: 39007661

SHA-256: `1c0c6d8033679153ed9837782fbfa2a3dbdb5273ad3de0289d8b993059f281b1`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
