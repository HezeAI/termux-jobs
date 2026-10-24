> Historical code1350 evidence. The developer reported the result good overall,
> but Fedora MP4 playback still failed and the input overlay needed further positioning/transparency.
> Superseded by code1351.

# v2.1.0 release-candidate validation — 2026-10-24

Public stable remains v2.0.2 until publication. Current candidate1350 includes
the cumulative private audit, X11 landscape, the stateful input control and the
optional style in all eight current X11 environments. Evidence:
`.verification/all-styles-20261024/`.

## Device feedback and current scope

Developer feedback on1346 led to1347's localized-configuration fix; feedback on
1347 led to1348's smaller landscape sizing and visible input window. Code1349
added a lower/stateful input button and the style in ordinary Ubuntu. The developer
accepted that work and requested the remaining desktops and v2.0 header branding.
Historical evidence: [1346](VALIDATION-v2.1.0-code1346.md),
[1347](VALIDATION-v2.1.0-code1347.md), [1348](VALIDATION-v2.1.0-code1348.md),
[1349](VALIDATION-v2.1.0-code1349.md).

Code1350 supports separate optional styles for Termux, Ubuntu, Debian, Alpine,
Fedora, openSUSE, Void and Arch Linux ARM. It keeps distribution menu plugins and
supports openSUSE's system XFConf panel channel and inherited empty properties.
The app header again reads Termux Job’s v2.0; APK/update version is still2.1.0.
The current candidate's GUI checks remain separate from source/fixture evidence.

## Final APK

| Field | Verified result |
| --- | --- |
| File | `build/apk/Termux-Jobs-v2.1.0-arm64.apk` |
| Version | 2.1.0 /1350 |
| Package / ABI | com.termux / arm64-v8a only |
| Size | 45,854,020 bytes |
| SHA-256 | `fca4e0e339cb896bbc0691d069f76f985ea649d74edd87ff20ee12b24ae8e3db` |
| Signer SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| Android | min26 / target28 / compile35; embedded X11 requires30 |
| Packaging | ZIP CRC/signature valid, nondebuggable; same package/signer, code above1349 |
| Runtime | 13,655 definitions; required missing0, reviewed optional31 |
| Native / pack | Native libraries, pinned style ZIP and all other assets match1349 |
| Asset delta | x11-desktop.sh, x11-session.sh, x11-style.py only; packaged copies match source |

Bounded auth/key filename, private-key/token and configured-private-server pattern
checks found no matches in packaged assets/DEX. This is not a universal secret
guarantee. All494 main files match source-test, modern-lint and ARM64 build stages.
The final APK, update.json and SHA256SUMS are in the evidence release directory.

## Automated verification

- Fresh **110 JVM tests in9 classes** pass, no failures/errors/skips. Covers the
  exact v2.0 header with stable controls, fullscreen lifecycle/input fill/crop,
  separate selections and busy-change guards for all eight desktops, correct
  per-environment services, wrong-rootfs refusal and localized configuration.
- Fresh **61 Python checks** pass: pinned style assets, apply/repeat/restore via
  real helper subprocesses in seven isolated guest HOMEs, unrelated HOME/prefix
  preservation, exact guest flag forwarding, script/session/rootfs isolation,
  clean-install package matrix and six native integration contracts.
- Panel cases include classic defaults, the system XFConf channel, inherited
  type=empty properties, explicit local values/array order, Whisker Menu/custom
  panel IDs, favorites/plugin preservation and restoration. Static cursor and
  image assets load through available native Xcursor/Pixbuf libraries without
  starting a desktop. This does not prove each guest's GUI/library version.
- Fresh five-module lint7.4.2: **0 errors /209 warnings**, unchanged categories,
  no metadata/rule-loading failures. CompileSDK35 exceeds AGP7.4's tested33;
  latest/API35 lint coverage is not claimed. [Warnings](LINT-WARNINGS-2026-10-24.md).
- Localization:584 workspace strings plus app/shared key/format parity. Shell
  syntax checks pass. Prior full JVM1224/184 is1347 evidence, not a fresh full
  execution on1350. Unrelated runtime Python/Node suites were not repeated.

## Code review and logical inspection

Eight main files differ from1349: header text, UI style policy, shared style
eligibility, service/launcher guards and three runtime helpers. No new permission,
renderer flags, native core, process ownership/stop, account store, workspace
schema or mandatory package changes. Exact aliases exclude Ubuntu AI, retired
roots and unknown targets. Per-target preferences retain the old Termux key;
no other environment is automatically opted into the theme.

Launch copies the same pinned assets into the selected service's control directory.
The shell validates a fixed0/1 flag before its own guest command. Each guest uses
its own HOME and /etc/xdg, ignoring inherited Termux PREFIX. Writes/restoration
records stay HOME-scoped with the existing archive pin/path/symlink checks and
preservation of user-edited assets/properties. Busy style changes are refused.

A read-only inspection found openSUSE's panel channel in the system XFConf
directory and inherited empty properties in its local panel file. Style discovery
now combines system metadata with explicit local overrides. Existing local XML
is not replaced wholesale by this effective tree; original local nodes are what
restoration saves. Whisker/distribution menu plugins and IDs are retained.
Other mounted guest templates used the classic panel/default.xml path. These
installed templates informed path handling, not a claim of fresh-device testing.

## Remaining device check and compatibility

Surrogate/fixture tests do not validate physical SurfaceControl composition, IME,
cutouts or every guest GUI. In each newly enabled desktop, stop it, select its
Termux-job's style and relaunch; inspect wallpaper, icons, cursor, window borders,
panel/menu and input. Disable after stopping and verify restoration. Confirm
another environment's selection and menus stay unchanged. Also check the v2.0
header and the existing fullscreen/Back/input behavior. Other devices/renderers,
Mali/Dimensity and fresh-device setup retain separate validation limits.

Same package/signer/schema8; no valid-data reset, environment/account migration
or reinstallation required by source/data review. Finish important operations
before updating and restart agents as documented. HOME-only SAF access, malformed
library read-only handling, exclusive outputs and uncertain remote Copy/Move
outcomes remain in both localized update notes and [release notes](RELEASE-v2.1.0.md).
Local SSH/proot exit is not proof of descendant termination.

Stable publication awaits the new phone result and [release bookkeeping](RELEASING.md).
No public release/update asset has been replaced by this preparation.
