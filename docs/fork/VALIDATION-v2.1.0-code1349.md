> Historical code1349 evidence: Ubuntu style and the stateful fullscreen input button.
> The developer then requested the style in every remaining X11 desktop and v2.0 header branding.
> Superseded by code1350.

# v2.1.0 release-candidate validation — 2026-10-24

Public stable remains v2.0.2 until publication. This candidate includes the
cumulative private source-audit fixes through1345 and X11 landscape presentation.
Current evidence: `.verification/ubuntu-style-20261024/`.

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
The developer accepted1348 and requested a slightly lower control, filled enabled
state and the same optional style in ordinary Ubuntu. Code1349 moves the control
16dp lower and binds its fill to the existing keyboard mode, including automatic
input/Back transitions. Unchanged frames do not recreate backgrounds or windows.
The bundled style is independently selectable in exact targets Termux/Ubuntu;
Ubuntu uses its own HOME and /etc/xdg defaults, never inherited Termux PREFIX.
Other environments/Ubuntu AI do not gain a style option in this candidate.
[1348 evidence](VALIDATION-v2.1.0-code1348.md) remains historical. The new fill/offset
and Ubuntu GUI appearance require their own phone check.

## Final APK

| Field | Verified result |
| --- | --- |
| File | `build/apk/Termux-Jobs-v2.1.0-arm64.apk` |
| Version | 2.1.0 /1349 |
| Package / ABI | com.termux / arm64-v8a only |
| Size | 45,853,688 bytes |
| SHA-256 | `508bb50faee6084f57653751b9fe6e527d0d286b60d0716e6a73e361d94dff78` |
| Signer SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| Android | min26 / target28 / compile35; embedded X11 requires30 |
| Packaging | ZIP CRC/signature valid, nondebuggable; same package/signer, code above1348 |
| Runtime | 13,655 definitions; required missing0, reviewed optional31 |
| Native / assets | Native libraries and bundled style pack match1348; three style-routing scripts changed |

Bounded auth/key filename, private-key/token and configured-private-server pattern
checks found no matches in packaged assets/DEX. This is not a universal secret
guarantee. Main Java/resources/runtime assets match the source-test, modern-lint
and ARM64 APK stages. Native core/process-stop code is unchanged.

## Automated evidence and review

- Fresh scoped JVM run: **108 tests in8 classes**, zero failures/errors/skips.
  Covers fullscreen host/window lifecycle, same-host return, stale callbacks,
  the sole nonfocusable input window, enabled-fill changes and unchanged-frame
  background reuse; size/crop bounds, keyboard state and localized configuration;
  independent Ubuntu/Termux settings, busy-change guards and private-service routing.
- Prior full JVM run on1347:1,224 tests/184 classes passed. It was not rerun as a
  full suite for this narrow correction; do not report that count as fresh1348
  execution. [Historical evidence](VALIDATION-v2.1.0-code1347.md).
- Fresh five-module lint7.4.2: **0 errors /209 warnings**, unchanged categories,
  no rule-loading/metadata failures. CompileSDK35 exceeds AGP7.4's tested33;
  latest/API35 lint coverage is not claimed. [Warnings](LINT-WARNINGS-2026-10-24.md).
- Fresh **54 Python checks** pass: style assets/application/restoration, shell
  desktop routing, selected-rootfs isolation, session policy and six X11/native
  contracts. A real helper subprocess applies/repeats/restores the style in a
  fixture Ubuntu HOME while preserving a separate Termux HOME/prefix. It is not
  a guest GUI test. Missing helpers/unsupported style targets fail before launch.
  Localization:584 workspace strings plus app/shared key/format parity. Unrelated
  Python/Node runtime suites were not repeated.
- Code/logical review: seven main files differ from1348: presentation/control
  wiring, style eligibility/copying in UI/service/launcher and three shell/Python
  style-routing helpers. No new permissions, process ownership/stop protocol,
  renderer, auth, schema or package dependencies. Runtime asset delta is exactly
  x11-desktop.sh/x11-session.sh/x11-style.py; every other asset/native library
  matches1348. Those three packaged helpers match production source. The bundled
  style pin/path/symlink guards and property-level restoration remain. Ubuntu
  cannot use inherited Termux PREFIX for defaults; writes stay in selected HOME.
  A strict 0/1 flag is checked before shell interpolation. Busy style changes are
  refused. Fullscreen teardown and stale-callback protection remain in scope.
- `update.json` and `SHA256SUMS` match this exact APK; RU/EN highlights retain five
  compatibility entries per language. No public release asset has been replaced.

## Remaining phone check and compatibility

Window tests use surrogate views: they do not prove native SurfaceControl
composition, sensors, touch routing, cutout placement or IME behavior. Minimum
check: rotate a running desktop, confirm the lower | button, its enabled fill,
manual/automatic input and Back hiding the keyboard, then return to embedded view.
Stop ordinary Ubuntu, opt into Termux-job's style and launch it; verify wallpaper,
menu/panel, window borders, icons, cursor and input. Stop it, disable the style and
verify restoration. Termux selection must stay unchanged. Other devices/renderers,
Mali/Dimensity and fresh-device setup retain separate validation limits.

No valid-data reset, rootfs/account migration or environment reinstallation is
required by source/data review. HOME-only SAF access, malformed-library read-only
handling, exclusive file names and uncertain remote Copy/Move outcomes remain
explicit in both localized update notes and the [release notes](RELEASE-v2.1.0.md).
Local SSH/proot exit is not proof of descendant termination.

Stable publication awaits this candidate's phone result and final bookkeeping
under [RELEASING.md](RELEASING.md).
