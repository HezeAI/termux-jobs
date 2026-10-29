# Private test3: actual pinned installation follow-up

The developer's test2 Kali trial failed at APT execution after the archive had
passed hash verification and dependency simulation. Earlier reports must not be
read as full clean-device installation validation.

Fix: allow APT's local-file acquisition step, disabling repository sources and
binary package caches for this invocation rather than using `--no-download`.
Native APT/dpkg tests with an empty package database and empty download cache
reproduce the former non-absolute-path error, install a real fixture package
with the fixed production helper, repeat successfully and fail safely on an
unavailable dependency. No installed phone rootfs is used by those tests.

Installed-package discovery reads dpkg/pacman/RPM databases once instead of
starting a manager for every missing package. Exact package-name/native Debian
architecture lookup and preservation of newer installed versions are covered.
The image downloader reports connection/cache/integrity stages; available bytes
are consumed without waiting to fill a large read buffer, and interrupted data
is retained for resuming. Russian/English environment cards display those
stages and measured download volume.

Earlier automated evidence: 125 targeted JVM checks passed; see the updated
coverage below for subsequent source changes. Modern lint previously reported
zero errors/infrastructure failures and 223 existing warnings. Fresh-phone GUI
validation remains separate.

Full package transactions in disposable imported image fixtures are still in
progress. Results and bounded range download samples are recorded under
`.verification/test3-pinned-install`. The developer requested removing downloaded
test images/rootfs copies after verification, retaining reports. Cleanup must
wait for each owning process to exit. Installed environments, user downloads and
rollback APKs are outside that cleanup.

## Follow-up findings and execution mode

A real interrupted post-install script reproduced `dpkg was interrupted` on
restart. The helper now asks native dpkg to consolidate its interrupted state by
configuring only packages in the verified target manifest; it does not remove
journals/locks, run `--configure -a`, force dependencies or permit downgrades.
The subsequent offline APT transaction supplies remaining dependencies and
reports persistent errors. A real native test verifies successful restart.

The public local-archive proot-distro install code was inspected: its common
post-extraction fixups configure DNS/hosts/Android IDs, not pacman's keyring.
Fresh Arch creation now initializes/populates the original local ARM keyring;
the desktop helper also initializes a missing Arch/Manjaro keyring without
turning off signature checks. First-install and existing-environment paths are
covered by isolated tests; full clean image transactions remain the GUI/runtime
validation boundary.

The parallel fixture run hit harness time limits: Kali/Manjaro 1800 seconds,
Fedora/openSUSE 2400 seconds. Those results are **not passing installations**.
Other research jobs were drained/stopped using exact fixture ownership checks
and pidfds; the existing user sessions/tunnel were not changed. At the developer's
request, the replacement run uses one clean imported image at a time, with a
10800-second bound and a required completion marker plus two readiness checks.
Old unfinished fixture trees were removed; logs and immutable package bundles
were retained. Alpine had already passed a complete transaction and two native
readiness calls; its fixture and downloaded test image were deleted.

Current targeted Python suite: 55 tests pass, including 3 real isolated native
APT/dpkg transaction tests. The two changed Java progress suites pass (13 X11
progress tests and 6 environment-progress tests); the prior broader 125-test
run is retained as earlier regression evidence. No final APK/device check or
public application release is claimed by these source-level results.

## Built private candidate

Package `com.termux`, versionName `2.0.10-test3`, versionCode 1434.
APK: `build/apk/Termux-Jobs-test3.apk`, 39831534 bytes.
SHA-256: `1e49ea1ca3da1bc46bd73f4f9fedcb56533e8f80e9529bdfeb96c34d523510be`.
Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Package/signer identity matches the retained rollback baseline; code increases
from1433. Schema8 is unchanged and no user-data reset is introduced. This does
not establish every device upgrade workflow. ZIP integrity, signature, ARM64
ABI, non-debuggable release metadata, 85 source assets and required runtime
classes passed verification. All three JNI libraries match the retained1432
baseline byte-for-byte; no LocalAI/VM/Wayland payload or secret-pattern findings.
The APK has not been published as an application GitHub release.

Serial Kali completed all336 package inputs: native install exit0 (1547.8s in
nested PRoot), then two required-package/binary/font/private-D-Bus checks exit0.
Its disposable rootfs and test image were removed. Alpine had passed the same
two readiness checks after its full transaction before serial scheduling.
Manjaro is now processing its local package keyring; other serial targets are
pending. Do not represent the old parallel timeouts as successful checks. These
fixture runtimes are not a phone-installation speed benchmark.
