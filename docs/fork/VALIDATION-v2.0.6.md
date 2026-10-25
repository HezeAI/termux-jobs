# v2.0.6 validation

Date:2026-10-25. Baseline:public v2.0.5/code1376. Final version2.0.6/code1395 exceeds the developer’s retained private1394. Package, signer, ABI, SDK requirements and workspace schema8 are unchanged. No valid-data reset, rootfs/account migration or automatic package removal is introduced.

## Artifact

| Property | Result |
| --- | --- |
| APK | `Termux-Jobs-v2.0.6-arm64.apk` |
| Bytes | 45953627 (46.0MB /43.8MiB) |
| SHA-256 | `a61d7b8db2c5a690a25572cd9d2c21422ab7a6d9d4a00973e0386d5d24c8a27b` |
| Package / version | com.termux /2.0.6 /1395 |
| Signing certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| ABI / debug | arm64-v8a only /non-debuggable |
| Requirements | Android8+ app, Android11+ embedded X11; min26/target28/compile35 |

The final ZIP and signature verify. All75 retained source assets and generated X11 runtime assets match the final source/build inputs. Android’s three JNI libraries match public v2.0.5 byte for byte. The optional Bionic-for-glibc ZIP and retired Wayland/DroidDeck classes, compositor/native libraries and KDE assets are absent. Reviewed credential filenames and secret-content patterns have no findings; this is a bounded scan, not a claim that all possible secrets were detected.

## Relevant tests and code review

- **338 JVM/Robolectric tests** pass with zero failures/errors/skips: desktop UI/profiles, lifecycle, startup, scale/viewport/input, GPU/program flows, attachments and file transport. They cover unsupported retired profiles, stale preferences resolving to XFCE, all eight X11 target menus, bounded errors and large-file boundaries.
- **114 scoped Python tests** pass for GPU/Wrapper/Bionic cache integrity, target isolation, preparation/cancellation, archive safety, focus/session policy and GPU tools/test setup. New bridge-download cases cover missing runtime, exact size/hash, atomic cache publication, offline reuse, failed/cancelled downloads preserving the prior installation/selection, symlink refusal and extraction of the network helper without the old APK ZIP.
- **Both Node attachment suites** pass for agent/model attachment routing, bounded prepared images, original files, cross-thread/path/symlink refusal and output cards. Boundary fixtures do not constitute an actual5GiB phone transfer.
- **584 localization checks** pass, including app/shared keys and format parameters.
- **Modern lint7.4.2 across all five modules:**0errors,210 existing warnings, no infrastructure/rule-loading diagnostics. Existing SDK/tooling support and warning categories are documented in [LINT.md](LINT.md); no broad suppression/baseline was added.
- **Release build and final DEX audit:**pass;13,700 defined classes and31 optional/framework unresolved types with existing explicit review notes.

The changed call paths were reviewed for target/launch identity, locks, startup/preparation, live settings, cancellation/error recovery and unrelated-session boundaries. Production PRoot session-bus configuration retains normal EXTERNAL authentication and distro policy, adds only the actual app UID, is private/mode0600 and atomic, and does not edit installed system configuration. Native Termux’s launch/settings branch remains unchanged.

Graphics downloads use HTTPS without disabling certificate checks, permit HTTPS redirects only, enforce pinned size/SHA-256 and bounded reads/time, and prepare only the allowlisted private runtime. A valid bridge runtime returns before network access. Failed download/integrity/cancellation does not activate a partial runtime or replace the selected driver. Imported drivers remain native code with application privileges; validation is not a sandbox. Existing active-desktop shared locks prevent replacement. No agent authentication or personal files are uploaded to the component repository.

## Native and device evidence

An isolated real settings-service matrix started private D-Bus/Xfconf instances with fixture HOME/cache/state/runtime directories in each installed PRoot rootfs. A second independent PRoot invocation applied75%,50%,100% and read72,48,96DPI respectively in **Ubuntu, Debian, Alpine, Fedora, openSUSE, Void and Arch**. All seven passed. No user desktop was restarted and no installed package was changed by this matrix. D-Bus printed the existing unprivileged file-descriptor-limit warning without preventing the checks. These are real process/protocol checks, not full GUI or clean-device acceptance.

HezeAI confirmed the scoped live Arch scaling correction and the custom-resolution keyboard correction on the phone. Earlier renderer/device results retain their original scope. A separate Adreno740 screenshot showed manually installed Turnip rendering vkcube; it does not prove the new app download path or every Wrapper ZIP on740/750.

## Distribution, compatibility and remaining checks

[The separate component repository](https://github.com/HezeAI/termux-jobs-components) hosts eleven pinned runtime archives, SHA-256, notices and corresponding source assets. The unavailable Vulkan Loader1.4.365 is replaced by1.4.366 verified against official Termux package metadata. Other graphics payloads retain their prior versions/hashes, including the bridge. Native package-manager dependencies remain outside the mirror. See [distribution design](GRAPHICS-COMPONENTS.md).

All20 public component release assets (including SHA256SUMS) were downloaded over HTTPS and matched their local bytes/hashes. The production app helpers then downloaded and prepared the bridge, Termux Wrapper and Termux Turnip in isolated repository fixtures, using a synthetic driver only for import validation and never executing it. Exact pins/manifest URL coverage, selected-driver preservation and verified bridge offline reuse passed. These checks establish download/unpack/cache behavior, not actual GPU rendering or Android UI acceptance.

Updater metadata is generated from this final APK with RU/EN compatibility highlights. Missing Bionic-for-glibc runtime now requires internet for initial preparation (6,053,026bytes); existing valid runtimes work offline. Existing presets, imports and valid workspace data retain their format. Retired Plasma selection is ignored; old experimental packages on other devices are not automatically deleted. A separately authorized one-off cleanup of this developer phone is historical evidence, not an APK migration behavior. APK rollback cannot reverse Linux package upgrades.

Specific remaining physical-device checks: fresh app/environment renderer preparation, new component-download progress/Cancel through Android UI, Wrapper with the chosen ZIP on Adreno740/750, GUI scale/rotation/keyboard regressions across all device/renderer combinations and an actual multi-gigabyte transfer. No universal GPU compatibility or exhaustive GUI testing is claimed.

Public release download/hash verification is recorded after publication in the repository handoff; source/build results above are distinct from Android installer/device acceptance.
