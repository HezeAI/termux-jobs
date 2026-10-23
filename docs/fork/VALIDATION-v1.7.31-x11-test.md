# Private v1.7.31-x11-test / code1246 · existing Debian X11

Public stable remains v1.7.5. Same com.termux/signing identity, code1245 →1246.
No data reset, history migration, environment rename or GitHub publication.

## Behavior and scope

Existing Debian sessions now expose the same X11 card as Ubuntu: check/install,
switch to graphical view/terminal, program catalog, Recommended, Installed,
remove supported programs and stop the owned desktop. The existing renderer,
Fcitx text focus/manual keyboard, Return key, window handling, wallpaper repair,
startup indicator and cancellable installation progress are reused.

`debian` is routed directly to its existing rootfs. An absent Debian is refused
before installation work and again before the create-rootfs branch. It is never
created or replaced by this feature; ordinary Ubuntu/agent paths retain their
previous behavior. Desktop packages are installed only after in-app consent.
Program and installed metadata caches are separate per target. Cross-environment
commands are blocked while another desktop is running. Owner/handle and target
checks for closing remain in effect; no broad process-name killing added.

Debian gets checksum-constrained owner read/write launcher repair (stopped desktop
required), and the same opt-in Firefox PRoot checkbox as Test Ubuntu. Mozilla's
signed ARM64 APT flow, same-ID menu override and wrapper are reused. The warning
about weakened shared-memory seals remains; no whole-sandbox disable is added.
Ubuntu's obsolete LXQt shortcut cleanup does not run on Debian.

## Evidence and remaining device tests

Read-only inspection identified the existing Debian GNU/Linux 13 (trixie) rootfs.
No installed rootfs, user session, package or shared SDK was changed during work.
The developer confirmed Ubuntu Firefox opens through Applications and a recreated
desktop shortcut; this is Ubuntu evidence only.

New fixture checks cover Debian check/install/run/maintenance routing, refusal to
create missing rootfs, native program installation and compatibility without APT,
separate catalog caches, the Debian desktop card and Firefox/repair actions, and
Binder rejection of cross-environment commands. Prior lifecycle, cancellation,
file hash/edit/symlink safety, wallpaper and native contract tests remain relevant.

Device test: stop Ubuntu's graphical desktop, open an ordinary Debian session,
check/install its desktop, then launch XFCE. Verify keyboard/manual/automatic
activation, Return in an application, window resize, background and menu stability.
Install Mousepad or Ristretto, open Installed/Recommended/catalog and re-open them
to check cache behavior. Test Firefox with explicit PRoot consent if needed, and
close the owning session to confirm its desktop stops. These are not inferred
from the automated fixtures. Inherited build Lint/Kotlin loader warnings may
remain; a successful build is not a claim of clean Lint or device GUI testing.

Automated checks passed: **44 JVM tests and 67 Python tests**. A final rerun of
13 routing/lifecycle tests passed after tightening the missing-rootfs guard.
The ARM64 release build and packaged runtime-definition check passed. Build logs
contain inherited Kotlin/Lint metadata loader warnings; no clean-Lint claim.
Artifact evidence: `.verification/x11-debian/` (tests, build, audit).

Final ZIP, package com.termux, version/code, minSdk26/ARM64, certificate and SHA-256
checks passed. Baseline native libraries and packaged JNI contracts (28/1/5)
remain verified. The artifact private-marker scan passed.

APK: `/sdcard/Download/Termux-Jobs-v1.7.31-x11-test-arm64.apk`

SHA-256: `088a6e97610370ff7caa5902f5389f02e4aff335f79d656a51698025d9cd33d6`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
