# v2.0.4 release validation

## Artifact

- APK: `Termux-Jobs-v2.0.4-arm64.apk`, versionName2.0.4, versionCode1366.
- Package:com.termux; ABI:arm64-v8a; release/nondebuggable.
- Bytes:51859681; SHA-256:`56ef4e034467bc1030a6ccb29c891ce97938baad3b5575a898e7e41abcd18493`.
- Certificate SHA-256:`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Same signer/package/schema8; code exceeds public1352 and private1365.
  No automatic data reset, rootfs/account migration or driver deletion.
- Android8+ app / Android11+ X11; min26/target28/compile35 unchanged.
- Archive CRC, package/ABI/version/signature, source-asset parity and bounded
  secret-pattern checks pass. All runtime assets and Android native libraries
  match candidate1365 byte for byte; stable version fields are updated.
- Runtime-definition verification:13,677 definitions;31 optional external
  references retain explicit review notes, no required unresolved definitions.

## Automated source/build results

- JVM:1251 tests in187 classes across app and terminal-emulator;
  no failures/errors. Counts per module are retained in private release evidence.
- Python:168 relevant cache/import, native-dependency, program/resume, theme,
  wrapper, keyboard, renderer, scope and clean-install-matrix tests pass.
- Localization:584 strings and existing app/shared key/format parity pass.
- Five-module modern lint7.4.2:0 errors,210 reviewed warnings, no checker-loading
  failures. The SDK/AGP tool-version limitation and existing warnings are retained,
  not suppressed with a new global baseline. See [lint limits](LINT.md) and
  [warning inventory](LINT-WARNINGS-2026-10-24.md).
- Isolated ARM64 release build passes. Existing APK-toolchain Kotlin metadata
  diagnostics remain tooling limitations; they do not replace modern lint.

## Native bridge checks and code review

Private authenticated no-Activity X11 processes verify GPU execution and RGB
readback through Bionic Turnip and Qualcomm891.11,24-frame swapchain reuse,
application intervals1→0→1, window clipping, odd widths/resize/front-buffer,
300ms gated GPU writes in all three modes,5.5s timeout/device-loss behavior,
expected allocation failure and private-server interruption. Independent
Qualcomm/Turnip fixtures have separate caches, PIDs and displays; one shutdown
does not stop its sibling. These are protocol/rendering checks without a visible
Android surface, not measured phone refresh or glmark2 scores.

Review traces import/selection, required package routing, runtime hash/lock
checks, process-local environments, native BGRA import size/format/type checks,
Present init/cleanup and modifier release on panel close/switch. Vendor code is
explicitly user-imported executable code; validation does not sandbox it. Existing
other renderer, Termux Wrapper, account/agent and rootfs paths are preserved.
Source pins, corresponding archives/patches, licenses and hashes accompany the
bridge export. Reference driver ZIPs remain separate unchanged release assets;
local copies match their upstream GitHub release digests.

## Device evidence and remaining checks

The developer reported working glibc bridge desktops, roughly2× FPS with working
VSync in the tested setup, and successful openSUSE/Void follow-up. These reports
are not an exhaustive device matrix. Earlier v2.0 baseline testing on RedMagic10
Pro and SamsungS26Ultra does not establish current891.11 compatibility on both.

Qualcomm891.11 private probes use API36/Adreno830. One concurrent stress run hit
the external deadline during a GLX interval transition. Its cause is unresolved;
a paired retry and three sequential runs passed. Proprietary QCOM support stays
experimental. Visible891.11 XFCE/browser/game use, other GPUs/APIs/firmware,
fresh-device installation, the new8% theme/openSUSE popup clearance and all panel
move/resize/IME gestures need device acceptance. Automated UI tests do not prove
physical keyboard, DPI, gesture or Android-permission behavior.

Update compatibility is source-reviewed and APK identity verified; actual Android
installer/update/data-migration behavior is not inferred from those checks.
The updater metadata includes explicit RU/EN compatibility notes before install.
Existing target-SDK/package-ID architecture and the documented warnings remain
separate work. No full-device stability or universal driver compatibility claim.
