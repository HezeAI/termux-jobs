> Historical code1348 evidence. The developer accepted the fullscreen correction,
> then requested a lower/stateful input button and the same optional style in Ubuntu.
> Superseded by code1349.

# v2.1.0 release-candidate validation — 2026-10-24

Public stable remains v2.0.2 until publication. This candidate includes the
cumulative private source-audit fixes through1345 and X11 landscape presentation.
Current evidence: `.verification/landscape-ui-20261024/`.

## Device feedback and correction

The developer reported that1346 stayed embedded on rotation. Code1347 corrected
localized Activity configuration delivery. The developer then confirmed fullscreen
switching, but reported excessive desktop enlargement and neither overlay button
visible. Screenshot `Screenshot_20261024_082545.jpg` records that intermediate
result. Code1348 doubles logical dimensions on the dense-phone landscape path,
halving enlargement, removes the settings gear and moves the Interactive input |
button from a PopupWindow to a small sibling application Dialog. The control is
nonfocusable, not touch-modal and not dimmed. No overlay permission is needed.
It is dismissed together with the fullscreen presentation.

Historical results: [1346](VALIDATION-v2.1.0-code1346.md),
[1347](VALIDATION-v2.1.0-code1347.md),
[rotation fix](AUDIT-X11-ROTATION-FIX-2026-10-24.md).
The new control's physical composition/IME behavior still needs a phone check.

## Final APK

| Field | Verified result |
| --- | --- |
| File | `build/apk/Termux-Jobs-v2.1.0-arm64.apk` |
| Version | 2.1.0 /1348 |
| Package / ABI | com.termux / arm64-v8a only |
| Size | 45,853,076 bytes |
| SHA-256 | `d5bb1d91665bb96f1f08dd7ac32f48e4e298eea4bb39567d229b643f9e0b0c43` |
| Signer SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| Android | min26 / target28 / compile35; embedded X11 requires30 |
| Packaging | ZIP CRC/signature valid, nondebuggable; same package/signer, code above1347 |
| Runtime | 13,653 definitions; required missing0, reviewed optional31 |
| Native / assets | Byte-identical to1347; no desktop/driver download for this correction |

Bounded auth/key filename, private-key/token and configured-private-server pattern
checks found no matches in packaged assets/DEX. This is not a universal secret
guarantee. Main Java/resources/runtime assets match the source-test, modern-lint
and ARM64 APK stages. Native core/process-stop code is unchanged.

## Automated evidence and review

- Fresh scoped JVM run: **39 tests in5 classes**, zero failures/errors/skips.
  Covers fullscreen host/window lifecycle, same-host return, stale callbacks,
  the sole nonfocusable input window and outside-touch policy, size/aspect bounds,
  IME crop, keyboard state and localized configuration delivery.
- Prior full JVM run on1347:1,224 tests/184 classes passed. It was not rerun as a
  full suite for this narrow correction; do not report that count as fresh1348
  execution. [Historical evidence](VALIDATION-v2.1.0-code1347.md).
- Fresh five-module lint7.4.2: **0 errors /209 warnings**, unchanged categories,
  no rule-loading/metadata failures. CompileSDK35 exceeds AGP7.4's tested33;
  latest/API35 lint coverage is not claimed. [Warnings](LINT-WARNINGS-2026-10-24.md).
- Six X11/native integration-contract cases pass. Localization:584 workspace
  strings plus app/shared key/format parity. Python/Node runtime suites were
  not rerun for unchanged runtime assets.
- Code/logical review: no new process launch/stop, auth, permission, rootfs, schema
  or renderer change. Only presentation geometry, the control window, constructor
  wiring and version metadata changed from1347. The existing native expected-size
  API carries the logical mode. Keyboard occlusion changes only visible crop.
  Portrait/Back, environment replacement, switching away and close use the same
  presenter teardown; stale callbacks cannot close replacement hosts.
- `update.json` and `SHA256SUMS` match this exact APK; RU/EN highlights retain five
  compatibility entries per language. No public release asset has been replaced.

## Remaining phone check and compatibility

Window tests use surrogate views: they do not prove native SurfaceControl
composition, sensors, touch routing, cutout placement or IME behavior. Minimum
check: rotate a running desktop, confirm the smaller scale and visible | button,
open/type/hide the keyboard, then rotate back and use Android Back. With IME open,
Android normally consumes the first Back to hide it; the next returns the desktop
to the embedded window. Check a guest desktop and an agent tab, same open windows
and restored normal orientation policy. Other devices/renderers, Mali/Dimensity
and fresh-device installation retain their separate validation limits.

No valid-data reset, rootfs/account migration or environment reinstallation is
required by source/data review. HOME-only SAF access, malformed-library read-only
handling, exclusive file names and uncertain remote Copy/Move outcomes remain
explicit in both localized update notes and the [release notes](RELEASE-v2.1.0.md).
Local SSH/proot exit is not proof of descendant termination.

Stable publication awaits this candidate's phone result and final bookkeeping
under [RELEASING.md](RELEASING.md).
