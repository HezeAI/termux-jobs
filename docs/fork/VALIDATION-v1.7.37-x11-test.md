# Private v1.7.37-x11-test / code1252

Same package/signature, code1251→1252. No data resets, package changes, new rootfs,
installed-app manipulation or publication. Developer reported Alpine desktop now
starts after1.7.36; no claim that every Alpine application was verified. The prior
Glycin decoder isolation reduction remains as documented in1.7.36.

## Testing, review and logical inspection

Developer screenshots show the native Termux startup reaching WM-ready then timing
out, and Alpine program lists failing with invalid APK file ownership. Source confirms
an exact-name one-shot readiness check. The exact live missed condition was not
observed: standalone X11 queries were rejected because the authority file is not
visible through this Ubuntu mount; no authentication was bypassed.

A startup-only helper now tests WM self-reference and visible EWMH DOCK/DESKTOP types,
using WM_CLASS candidate hints for override-redirect clients missing from client lists.
It is bounded by command timeout, per-snapshot deadline, client counts and overall
grace. No permanent observer/timer, no title/input logging. Guardian controls the
whole child group. At first timeout manual Show appears; later READY is still accepted,
clears timeout and uses the existing attach path. Stop/session-close/error ownership
remains the previous implementation; agents/rendering/IME are unchanged.

Fixtures cover localized names, type/viewability, mismatched WM IDs, query errors,
child timeouts, override-redirect windows, readiness after initial timeout and bounded
never-ready termination. Existing focus/token/fragmentation, UI, lifecycle, catalog,
cache, installation and cancellation fixtures are included in selected JVM checks.

Read-only real Alpine APK DB inspection reproduced a too-restrictive package-name
pattern: libSvtAv1Enc and ssl_client were rejected. Corrected code reads386 packages,
37 desktop owners and37 checksums, without modifying anything. New tests cover
uppercase, underscore, one-character names, preserved case and rejected option/path/
shell arguments. Ownership path/checksum validation is retained; every package command
still uses fixed argv. The shared Java catalog parser accepts the same safe ASCII
names, while native APT backends keep their own package rules.

Initial test maintenance: the retired inline-readiness asset assertion was updated
for helper delegation, and an older keyboard/component fixture received fake audio
components required by the current base check. No real packages were installed.

Device checks pending: automatic show for native Termux and Alpine, late startup,
manual Show followed by readiness, stop/close and a quick Ubuntu/Debian smoke test.
In Alpine, refresh catalog/recommended/installed lists and try one program install.
A working full desktop is not inferred from mocked window metadata.

Final automated checks: **79 JVM +98 Python tests passed**. ARM64 release and
runtime definitions verified (13372 definitions,31 optional unresolved types with
existing explicit review notes). Final audit passed ZIP integrity, com.termux/code1252/
minSdk26/ARM64, original signing identity, pinned native/JNI28/1/5, matching source
assets including x11-ready.py, absent retired observers and limited private-marker
scan. Alpine compatibility module is byte-identical to1.7.36. Inherited build metadata/
deprecation warnings remain; no clean Lint claim. Download digest verified.

APK: `/sdcard/Download/Termux-Jobs-v1.7.37-x11-test-arm64.apk`

SHA-256: `111ee483e562af237ff15020b0ee5c6739a5df59d148ad7e8636dfc15df1ce45`

Certificate: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

Evidence: `.verification/x11-ready/`. Device automatic-show and program-list tests
for this patch remain pending; no APK installation or GitHub release performed.
