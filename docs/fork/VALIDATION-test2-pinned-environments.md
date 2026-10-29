# Historical private test2: Kali, Manjaro and pinned initial baselines

**Device trial failed:** the developer reported a fresh Kali APT installation
failing with `Pathname to install is not absolute`. Archive and solver checks
did not exercise actual file acquisition/dpkg execution. This candidate must
not be treated as a passing clean-install baseline. See the test3 follow-up.

Candidate: `2.0.10-test2`, versionCode 1433, package `com.termux`.
APK: `build/apk/Termux-Jobs-test2.apk`, 39828614 bytes.
SHA-256: `055f19d204abdb48a0a2462c6059aec09b202970444f0fed522f73706a00b1e8`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
ABI: arm64-v8a. Package/signing identity matches the retained rollback baseline;
versionCode increases from 1432. Workspace schema8 is unchanged, with no automatic
reset or environment replacement. This is source/build evidence, not proof of
all upgrade/data workflows on a device.

Changes: separate Kali and Manjaro targets, processes/displays 106/107 and the
existing desktop controls/program workflows. Manjaro exposes CPU and VirGL only.
Ordinary ARM64 images use `rootfs-arm64-20261028`; desktop/fonts/runtime closures
use `xfce-base-20261028` in Termux Jobs Components. Ubuntu AI retains upstream
installation policy. A complete Arch transaction includes pinned base-package
updates to avoid a rolling partial upgrade. Alpine includes the Mesa DRI
metapackage required by its VirGL component check. Optional apps remain separate.

Pins define initial installation and repair, not permanent package holds. Normal
launches test readiness rather than exact version equality. Newer user-installed
packages are retained during repair. Dependency conflicts are reported rather
than forcing removals/downgrades. Official package repositories stay available.
The agreed update/snapshot/rollback UI is a later stage and is not implemented.

## Verification

- 119 targeted JVM tests; 0 failures, 0 errors.
- 53 targeted Python checks passed: environment/image setup, clean required
  component matrix, archive safety, interrupted-stage reuse, legitimate Android
  filesDir aliases, impostor/link rejection, newer-package retention and
  environment-specific offline dispatch.
- Modern lint 7.4.2 analyzed all five modules: zero errors or infrastructure
  failures; 223 existing warnings. The compile-SDK/toolchain compatibility
  warning remains. Native libraries and rendering/surface/rotation code were
  not rebuilt or changed by this work.
- Twelve package bundles passed full archive SHA-256, internal manifest,
  per-file hash/size and package architecture validation. Native dependency
  planning/download-only checks passed for the twelve targets/subsets. Arch and
  Manjaro additionally passed package SHA-256 and official public-key gpgv
  checks. Pacman print-mode uses matching guest ARM64 binaries with explicit
  disposable root/db/cache/keyring/log paths; its verification-only config
  omits DownloadUser because host NSS differs. Nested-proot checks are not
  represented as full phone installations.
- Native proot-distro extraction-library checks passed for all nine images.
  The public CLI could not be tested from inside the already active proot.
- Final APK ZIP integrity, release signature, package/code/name, non-debuggable
  status, ARM64 ABI, 85 source assets and runtime class definitions checked.
  libXlorie/libtermux/libtermux-bootstrap bytes match the retained baseline.
  No Local AI/VM/Wayland payloads or private-key/token/credential-pattern findings.

## Remaining device checks

Fresh-device GUI and package-script execution are not proven by solver checks.
openSUSE dry-run resolved dependencies but skipped installed-file conflict lists
for its 375 new packages. Verify clean Kali/Manjaro installation, CPU and VirGL,
Kali's supported private renderers, screen resolution/scale/rotation, storage
permissions/keyboard, and two simultaneous desktops. Test update over an existing
installation with user data. Existing phone environments were not reset for QA.

Component publication evidence and immutable asset digests are retained under
`.verification/kali-manjaro`; final publication state is recorded in RUN-STATE.md.
