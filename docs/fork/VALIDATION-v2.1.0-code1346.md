# Superseded candidate — code1346

The developer reported that physical landscape rotation left X11 embedded.
The checks below did not cover configuration delivery through the localized
Activity context. See [current validation](VALIDATION-v2.1.0.md) and the
[rotation fix review](AUDIT-X11-ROTATION-FIX-2026-10-24.md). The old APK is
archived locally under `.verification/landscape-20261024/release/`; its hash
below identifies that superseded artifact. It was not published as stable.

# v2.1.0 release-candidate validation — 2026-10-24

Public stable remains v2.0.2 until publication. The candidate includes all private
source-audit fixes through1345 plus the new X11 landscape presentation. Evidence:
`.verification/landscape-20261024/`. Earlier audit results remain available in
their individual historical reports, with their original artifact scope.

## Final APK

| Field | Verified result |
| --- | --- |
| File | `build/apk/Termux-Jobs-v2.1.0-arm64.apk` |
| Version | 2.1.0 /1346 |
| Package / ABI | com.termux / arm64-v8a only |
| Size | 45,852,976 bytes |
| SHA-256 | `fc48da9e9c4e7cc24a4bdebf87862774c5d9ed4e17555033c5615742320ca027` |
| Signer SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| Android | min26 / target28 / compile35; embedded X11 requires30 |
| Packaging | ZIP CRC/signature valid, nondebuggable; package/signer retained, code above1345 |
| Runtime | 13,653 definitions; required missing0, reviewed optional31 |
| Native / assets | Byte-identical to1345; no new desktop/driver download for rotation |

Bounded auth/key filename, private-key/token and configured-private-server pattern
checks found no matches in packaged assets/DEX. They are not a universal secret
guarantee. Main sources/resources match JVM, modern-lint and APK stages. Runtime
and native packaged files are compared with their verified predecessor; native
core/process-stop code is unchanged by landscape presentation.

## Automated verification

Final full JVM confirmation passed **1,222 tests in184 classes**: app1,068/165
classes and terminal-emulator154/19, no failures/errors/skips. Both suites ran
fresh; emulator execution was explicitly forced. The final run covers the last
unknown-insets and cutout refinements. The first full run exhausted the default
Robolectric heap; after bounding cache/worker lifetime, the intermediate full run
and final confirmation passed. No test was excluded to obtain this result.

- Five-module lint7.4.2: **0 errors /209 warnings**, no rule-loading/metadata
  failures. [Warning categories](LINT-WARNINGS-2026-10-24.md) are unchanged.
  AGP7.4's compileSDK33-tested warning with compile35 remains visible; complete
  latest/API35 coverage is not claimed. The production APK toolchain is retained.
- Eight lint-report checker cases and six X11/native integration-contract cases
  pass. Localization:584 workspace strings plus app/shared key/format parity.
- Window/geometry/keyboard tests cover the new presentation lifecycle, late
  callbacks, host identity, orientation restoration and size/crop limits. These
  use surrogate views; they do not run native SurfaceControl or phone sensors.
- Python/Node runtime suites from the cumulative audit were not rerun for this
  unchanged asset set. Their prior results remain specific to the recorded audit.
- `update.json` is generated from this exact APK with RU/EN highlights and five
  explicit compatibility entries per language. The release folder includes the
  same APK and `SHA256SUMS`; public download/updater checks await publication.

## Device results and publication status

HezeAI confirmed the preceding audit build's phone checks before requesting
landscape mode. The new mode's physical rotation, native surface reattachment,
keyboard/gesture behavior, cutout placement and renderer composition remain to
be checked on the candidate APK. Do not count the older RedMagic 10 Pro/Samsung
S26 Ultra desktop checks as validation of this newly added presentation.

Minimum new check: rotate a running Termux desktop with an open program, use
interactive input/settings, rotate back and return with Android Back, then
switch a guest desktop and an agent tab. Confirm the same windows remain, input
works, no app tabs remain in fullscreen and normal app orientation returns after
leaving X11. Check a currently used renderer; other devices/driver combinations,
Mali/Dimensity and fresh-device installation retain their separate limits.

No data reset/rootfs/account migration is required by the source/data review.
HOME-only SAF access, malformed-library read-only handling, exclusive file names
and uncertain remote Copy/Move outcomes are disclosed in both localized update
notes and the [English release notes](RELEASE-v2.1.0.md). Completed remote work
is not rolled back; local SSH/proot exit is not proof of descendant termination.

Stable publication is pending the new phone result and final release bookkeeping
under [RELEASING.md](RELEASING.md). No remote tag, updater asset or public release
has been replaced by this preparation.
