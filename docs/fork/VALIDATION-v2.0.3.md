# v2.0.3 release validation — 2026-10-24

This stable release includes the cumulative audit fixes since v2.0.2, landscape
X11, optional per-environment styles and Fedora MP4 playback setup. HezeAI reported
that the latest candidate works and authorized final checking and publication.
All494 production source/resource files and every packaged runtime asset/Android
JNI library match that developer-tested candidate. Only release version fields
change; physical installation of the release-labelled APK is a separate check.

## Final artifact

| Field | Verified value |
| --- | --- |
| APK | `Termux-Jobs-v2.0.3-arm64.apk` |
| Version | 2.0.3 / code1352 |
| Package | com.termux |
| Size | 45,882,874 bytes |
| SHA-256 | `5c2deb38156453cf76f3d6bf87f2eb913b2dc5eb8fcde7034b93ba6e2debfaa0` |
| Signer SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| ABI / debug flag | arm64-v8a only / nondebuggable |
| Android | min26 / target28 / compile35; embedded X11 requires30 |
| Runtime | 13,655 definitions,0 required missing /31 explicitly reviewed optional references |
| ZIP/signature | CRC and Android signature verification passed |

Same package and signer as public v2.0.2/code1331; code1352 also exceeds the
checked private code1351. APK runtime assets and JNI libraries are byte-identical
to the developer-tested candidate. Bounded auth/key filename and credential/private
server pattern scans found no matches in APK assets/DEX. Source findings were
limited to pre-existing synthetic test markers in four fixtures; not real keys.
This bounded scan is not a universal secret-detection guarantee.

`update.json` is schema1, generated from this exact APK. Its name, size, hash,
package/version and localized compatibility lines agree with the release payload.
`SHA256SUMS` covers the APK and update.json. Documentation changes do not trigger
another build of identical source. Evidence directory: `.verification/release-v2.0.3/`.

## Final verification

- Fresh full JVM run: **1,229 tests /184 classes**,0 failures/errors/skips
  (app1,075; terminal-emulator154). Terminal-view, shared and X11 participate in
  compilation; X11/controller tests also live in the app suite.
- Fresh Python discovery: **579 run /578 passed /1 intentional skip**,0 failures
  or errors. The skip is the retired Garcon trial overlay, absent from build/APK
  inputs. The large private cleanup fixture is included; no user storage was
  deleted. Synthetic duplicate-archive metadata produces an expected warning.
- Fresh **31 Node fixture programs** passed without timeout, using private homes,
  loopback providers and mock peers. No real-account model request is inferred.
  Each runner only cleans its own unreaped fixture process group.

- All681 source/test/resource files match source-test, ARM64 and modern-lint
  stages. All494 production files still match the developer-tested candidate.
- Independent lint7.4.2 across all five modules: **0 errors /210 warnings**,
  no rule-loading infrastructure failures. [Categorized warnings](LINT-WARNINGS-2026-10-24.md).
- Localization584/key/format parity, manifest/provider/network guards,
  final APK/runtime/signature/ZIP/source-pattern checks and git diff checks pass.
- Earlier isolated native Fedora H.264/AAC MP4 test: baseline stub fails with
  0video/0audio buffers; private decoder/plugin reaches EOS with5video/47audio
  buffers. Actual helper install/repeated ready checks pass in a private fixture
  HOME. HezeAI subsequently confirmed playback of the reported MP4 in the app.

The APK chain retains its existing AGP4.2/Kotlin metadata diagnostics. Independent
lint uses aligned AGP7.4.2, checks all modules and has no registry-loading errors.
Its compileSDK35 exceeds AGP7.4's tested33: exhaustive latest-API lint coverage is
not claimed. Warnings are documented rather than blanket-suppressed. Historical
reports refer to their own builds and counts, not fresh final-release execution.

## Review, compatibility and limits

[Final audit](AUDIT-RELEASE-v2.0.3.md) traces startup, tab/window changes, closing,
cancellation, failure and recovery across the affected mechanisms. It links
focused findings and retained boundaries. No renderer binary/protocol or rootfs/
account-store migration is introduced; schemas and signing identity are retained.
No valid-data reset is required by source/data review. Installed Android upgrade
and data survival are not established by a JVM test or APK signature alone.

Intentional compatibility behavior is disclosed in RU/EN updater highlights and
[release notes](RELEASE-v2.0.3.md): HOME-only Android SAF boundaries, exclusive
outputs, read-only malformed duplicate-ID libraries and uncertain remote Copy/Move
results after interruption. Local transport exit is not proof that every detached
remote/proot descendant has stopped; inspect both paths before manual retry.

Developer checks cover the latest candidate's UI/X11/MP4 behavior. Earlier device
work includes RedMagic10Pro and SamsungS26Ultra; not every new combination was
retested on both. Fresh-phone package downloads and GUI, other MP4/HEVC codecs,
Mali/Dimensity, driver combinations, live-provider-specific recovery, accessibility,
force-kill/power-loss durability and package-ID/target-SDK modernization remain
separate work. Source clean-component fixtures cannot prove fresh-device GUI.
