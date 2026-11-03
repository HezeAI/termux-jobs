# v2.1.2 release validation

Date:2026-11-03. Comparison baseline: public stable v2.1.1/code1454, confirmed through
GitHub Releases latest API. The new release code1476 exceeds all inspected issued
and reserve APKs. The branch/tag and final APK are checked together before upload;
post-publication anonymous verification is recorded in the GitHub release body and
local publication evidence without moving the immutable release tag.

| Final property | Verified value |
| --- | --- |
| APK | Termux-Jobs-v2.1.2-arm64.apk |
| Package / version | com.termux / 2.1.2 /1476 |
| Bytes | 40,452,595 |
| APK file SHA-256 | `2f8b97b2d9083a5b7dcc3966eca54ea6d843fec3e253ba6e279c123921eeddab` |
| Signing-certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| ABI / signature / debug | ARM64 ELF64 AArch64 / verified v2 / nondebuggable |
| Runtime / assets | 14,173 defined DEX classes;119 assets matched source/pinned inputs |

The certificate matches previous Termux Job’s releases, including v2.1.1. This
is not a promise of compatibility with official GitHub/F-Droid/Google Play Termux.
Matching com.termux alone is insufficient. No app-data reset, rootfs replacement
or incompatible schema migration is introduced; workspace schema8 remains. An
APK is not a user-data backup. Uninstalling can delete private data and is not an
ordinary update step. See [security and data](../../SECURITY.md).

## Проверено заново / Checked anew

- Release compilation/package/signing with the existing isolated local toolchain.
  Final source/resource/asset bytes are compared with the build and modern-Lint
  stages and then with the committed release tree. Tool/key/cache directories
  are not published as source. Native payload reuse is separately recorded below.
- **1598 distinct JVM cases** are covered by the broad app/terminal-emulator run
  and successful targeted reruns of corrected fixtures. The initial run recorded
  14 failures: stale menu/expansion/child-count and async-save expectations, long
  fixture socket paths and a renderer test treating supported modes as unsupported.
  Fixtures were corrected, all affected classes repeated, and final results merged
  by class identity. Socket-length production guards and application behavior were
  retained. The final About/notice change had a further **82-case** fresh route,
  settings and instruction regression pass; these are repeats, not1598additional
  cases. Terminal-view/shared test tasks have no standalone cases in this setup;
  shared consumers are exercised by app tests.
- **185 Python tests** passed on the final helper behavior: Codex/Claude instructions,
  skill create/import/update/rollback/conflicts/archive safety, environment routing,
  missing/existing/incomplete Ubuntu AI, transfer/error/cancel/retry/scope handling,
  component-only setup and Lint-report validation. Four initial host fixtures still
  used the old installer boundary; their isolated downloader/install stubs and
  expectations were updated, then the complete selected185test set repeated.
- **13 Node fixture suites** passed: Claude bounded history/active input/lifecycle,
  Codex chat/pagination/history wire, scoped Stop across four adapters, request
  timing, file outputs and Gemini/OpenCode chat/recovery. The history fixture covers
  >8000records, >500messages,69+MiB JSONL,680tools/results,520parts in one response,
  full Unicode detail, append/partial tails, restart, replay/reset and read-only SHA.
- Real **Claude2.1.296**, disposable HOME/config/project and loopback provider:
  supported shared/project/local instructions were observed; saves applied after
  restart/resume and in a new conversation, with native history retained. Same-process
  requests retained old instructions. Skills personal/project scope, name priority
  and explicit reload were checked with harmless fixtures. AGENTS fallback was not
  observed in this release rerun although an earlier probe observed it; diagnostics
  treat that mechanism as conditional on plugin/settings. The old unconditional
  fixture assertion was replaced with an explicit observation, not a false fallback
  PASS. No working session/account or real provider model was used.
- Localization check passed:584workspace strings plus app/shared keys/format args.
- **Full modern Lint7.4.2**, all five modules/dependencies: **0errors,234warnings**,
  no rule-loading/infrastructure diagnostics. Existing compileSDK35 beyond AGP's
  tested range remains reported. Legacy packaging-Lint Kotlin/registry diagnostics
  in build logs are not used as the successful Lint gate. No warning baseline or
  blanket suppression was added for this release.
- Final APK: valid ZIP/CRC, package/version/increasing code, ABI, nondebuggable,
  v2 signature/certificate and SHA. Runtime audit has no required/unreviewed missing
  types. All119assets and all three native payloads match source/pins. Required
  third-party notices, including the exact checker-qual MIT text, are embedded and
  reachable through About. Source tar archives are not APK or auto-install payloads.
  An incremental packaging candidate contained unreferenced old ZIP local records;
  its task-owned package state was preserved, then packaging was repeated from a
  fresh output. Only the final clean artifact identified above is published.
- Source/permission/process review: no new model request, auth-file diagnostic read,
  session stop, APK installation, environment reset or implicit instruction/skill
  application is introduced. Original-session file routing and safe atomic saves/
  revision/draft guards remain. Native Stop scope tests intercept real signal APIs.

## Sources, notices and Components

[License/source review](LICENSE-REVIEW-2026-11-03.md) distinguishes confirmed and
unverified coverage. Original GPL/copyright notices are preserved and the previously
missing checker-qual MIT notice is included. README/About separate HezeAI's fork
from upstream Termux and do not claim endorsement or universal safety.

The already published Ubuntu AI image has a new **separate** corresponding-source
resource:87binary packages,58exact source versions, all127DSC-referenced files fully
downloaded and length/SHA checked. DSC Source/Version matches were checked;
DSC signatures and package rebuilds were not independently verified. All129existing
Components asset identities/URLs/names/lengths/SHA-256 remain unchanged. All10pinned
image catalog entries match public asset sizes/digests. The production image
helper fetched the41,144,320byte original Ubuntu AI image into a fresh isolated cache
and verified c08828893609894e5ab6d80f146961961148afde007ce904292418d8fcd011e0 after
the source addition. This did not install a rootfs or alter system proot-distro.
New source/manifest/checksum assets passed anonymous full download/SHA verification.
Other images/XFCE/imported-driver subcomponents are not claimed covered by that result.

## Переиспользовано / Reused with input identity

The three native libraries are reused from the accepted candidate after95recorded
relevant source/configuration hashes matched and their final ELF/byte hashes were
checked again. SDK/JDK/Gradle/dependency caches are reused, not copied wholesale.
No native graphics code or pinned core changed. This is not a fresh desktop/GPU run.
Earlier device/native observations retain the scope of their dated evidence.

## Проверки разработчика / Developer phone evidence

HezeAI confirmed Claude working on another phone after switching new Ubuntu AI
installation to the GitHub image. That is developer-reported evidence, not an
independent run, and does not diagnose the earlier Docker Hub failure. HezeAI also
confirmed Send alignment during development. The supplied project-editor video
showed an existing AGENTS.md already open for Codex and a missing CLAUDE.md for
Claude; the misleading action was clarified. These observations are not expanded
into acceptance of every new screen, animation or device.

## Не проверено / Not checked and current limits

- Independent security/legal audit: none confirmed. Tests, source availability,
  checksum and signer checks are not safety/compliance guarantees.
- Actual phone/IME/font/gesture behavior for the final release, real HTML/GIF
  animation and long-history scroll acceptance on additional devices remain
  separate checks. Model UI fixtures do not establish them.
- Native clean-device setup of all four agents and every distro/GPU/driver was not
  repeated. The production downloader and isolated installer/control tests do not
  establish universal Android installation or rendering success. Existing Ubuntu AI
  is reused; incomplete installations are preserved and reported.
- CLI/provider policy and versions determine capabilities. Component readiness is
  separate from authorization/model access. Changed adapters require an explicit
  idle restart/resume; no user session restarts automatically. Instructions/skills
  present on disk are not automatically labeled applied/discovered.
- Claude native history retains a32MiB per-record safety bound and cannot infer
  unresolved cross-file parents. Native user history was not modified.
- Exact-source/license verification of the other named Components remains open
  as detailed in the license review; no blanket compliance claim is made. Network
  traffic for every native CLI/third-party dependency was not independently audited.

Evidence: `.verification/release-v2.1.2/` holds initial/final logs, merged JVM
results, Python/Node/native evidence, final Lint summary, immutable source identities,
APK/resources/signature/runtime audits, source checks, catalog/downloader results and
publication proof. Public release checksums/update.json refer only to the final file
above, not the intermediate packaging candidates.
