> Historical private candidate1394. Superseded by [v2.0.6 validation](VALIDATION-v2.0.6.md); the artifact and checks below retain their original scope.

# X11-only private candidate — validation and compatibility

Date:2026-10-25. Candidate `2.0.5-x11.1`, versionCode1394. This is a private
recovery candidate, not a published GitHub release. Latest published baseline is
[v2.0.5](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.5), code1376.

## Changes against the public baseline

- Chat files:256MiB →5GiB per file; original images:20MiB →100MiB.
  Larger originals remain available to tools; the model receives a bounded
  prepared image. APK cards also accept files above the previous256MiB limit.
- Large chat exports stream with a bounded six-hour deadline instead of the
  ordinary90-second query deadline. Size validation, cancellation and partial
  export cleanup remain. Other file-manager operations keep their own limits.
- Custom X11 resolution retains stable host geometry when the Android keyboard
  opens, instead of squeezing the desktop into the remaining portrait height.
  HezeAI confirms the keyboard correction on the phone.
- Wayland/KDE has been retired at the developer's request. Its compositor,
  native libraries, build inputs, installers, driver controls and separate Arch
  program groups are removed. Ordinary X11/XFCE renderers and programs remain.

## Source, build and artifact checks

- 237 relevant JVM/Robolectric tests pass:212 X11/profile/program tests and25
  attachment/transport tests. Retired profiles reject, stale saved Plasma
  selection resolves to XFCE, all eight target menus retain X11 controls, and
  ordinary program flows remain. These are source tests, not phone GUI proof.
- 111 Python checks pass for desktop/native installation, programs/resume,
  renderer environment, Bionic/glibc integration, GPU tools and GPU Test.
- Both Node attachment suites pass, including size bounds/prepared-image paths.
- Release build passes; final DEX runtime-definition audit passes (13700 defined
  classes;31 optional/framework unresolved types have existing review notes).
- Modern lint passes across all five modules:0 errors,210 existing warnings,
  no infrastructure diagnostics. This is not a claim of zero code warnings.
- ZIP integrity, com.termux package, code1394/name, ARM64-only ABI, non-debuggable
  flag and unchanged signing certificate pass. All74 source assets and retained
  X11 runtime assets match the final source/runtime inputs. The three Android
  JNI libraries match the published baseline byte for byte. Wayland/DroidDeck
  classes, libraries, resources and the KDE archive are absent. The reviewed
  secret/credential filename and content-pattern scan has no findings.

## Separately authorized local cleanup

The developer explicitly requested retirement of the installed experiment.
Native Termux execution held the existing Arch desktop lock; no running desktop
was stopped. A reviewed pacman recursive removal plan was rechecked before the
transaction. The remaining Konsole/KDE-only closure was removed; KWin/Plasma6.8.1
had already been removed during the prior authorized cleanup. The private6.7.5
runtime, archive, Mesa/Turnip overlay, driver imports, namespaced configuration,
cache and Wayland GPU-test shortcuts were then removed from this phone.

All five core XFCE binary hashes and all33 X11 control-file hashes are unchanged.
XFCE packages, fcitx5-qt and qt6-base versions match their pre-cleanup values;
pacman -Dk passes. Shared Qt/Wayland libraries needed by other programs are not
mistaken for the retired desktop and are not forcibly removed. No agent rootfs,
account files, other environment, bot or user documents were part of cleanup.

The repository's heavy Wayland build/runtime/component caches, two obsolete
private APKs and the exact checksum-verified Download installer archive were
removed. The stable v2.0.5 APK remains available for rollback. Historical textual
reports retain their original scope and are marked retired.

This one-off cleanup is not embedded in the APK. Installing the candidate never
automatically deletes user data, projects, package databases or imported drivers.

## Compatibility and smallest remaining checks

Same com.termux/signer/schema8, Android8+ app/Android11+ embedded X11, ARM64.
Code1394 exceeds public1376 and the previous private1393. Existing workspace,
agent authentication, presets, X11 preferences and imports use the same data
format. The retired Plasma preference is ignored; no schema reset/migration.
Package/signing compatibility does not prove every application-data scenario.

Phone follow-up: install the candidate, verify Arch opens the single ordinary
Programs menu, launch XFCE with its existing renderer, check input/rotation and
open an existing X11 program. The keyboard fix itself is developer-confirmed;
these final cleanup regressions and fresh-device setup are not claimed tested.
An end-to-end5GiB transfer on a physical phone is also not claimed by boundary
fixtures. Provider/model attachment limits remain separate from the local limit.

## Artifact

- File:`Termux-Jobs-v2.0.5-x11.1-arm64.apk`
- Bytes:51882945 (51.9MB /49.5MiB)
- SHA-256:`19bb1de1903339cc1dba65f2a87f86f2c18021dff507a2402a2f97f9565d68b1`
- Signing certificate SHA-256:`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

Compared with the published APK52721789B, this candidate is838844B smaller.
Machine-readable reports and guarded local cleanup scripts are under ignored
`.verification/wayland-removal/`; the private APK is under `build/apk/`.
