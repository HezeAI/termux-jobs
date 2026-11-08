# Release validation — 2.1.6 /1533

Candidate for the authorized stable release, relative to published v2.1.5 /1520.
The developer accepted test10 /1531 on the phone and subsequently authorized the
selected-environment/Arch deletion fixes and component-download recovery.
635 of 638 packaged production source files match test10.
Only the environment removal helper, image downloader and environment error UI
changed functionally. Two asset payloads changed; JNI, chat/RTK code and data schema
remain unchanged. Stable metadata uses code1533, above main1520/rollback1521/test10 1531.

Final signed APK: **40,673,325 bytes**, `com.termux`, ARM64-only, non-debuggable.
SHA-256: `3ada4628499f765bbd3842b3bf96cc8686ded68a2aa0d6801c97e5ff74183f6c`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
No rollback APK, automatic installation, user-session restart or data reset.

## Checked anew

- Final release build and APK gate: ZIP, package/version, signature/certificate,
  ARM64 ELF/DEX/runtime, all packaged assets, notices, no embedded RTK archive,
  no stale outer ZIP records, signer/X11 guards, source identity and SHA-256.
- Full Modern Lint 7.4.2, all five modules: **0 errors /290 warnings**, no rule-loading
  failures. Existing compileSDK/AGP warning retained. Old AGP Kotlin messages are
  not counted as the modern Lint result.
- Local row-selection/error UI and environment-choice JVM tests passed.
  Final generated `update.json` passed the production updater test in RU/EN;
  final targeted run: **19 passed, 0 skipped**, including12 updater cases.
- Environment helper: **19 local passed**; native Termux **18 passed /1 skipped**
  because Android rejects native hardlinks. Downloader suites: **39 passed**
  (includes inherited duplicate baseline cases, not39 unique scenarios).
  Build-input preparer: **5 passed**. Locale check passed.
- Native Termux installation supervisor: **11 passed**, no skips. Local supervision
  suite also11 passed. Cancellation is checked at completion using a descendant
  release barrier, separately from the allowed cleanup grace period.
- All10 Components base archives: pinned size/SHA verified through the production
  fallback and independent public API transfers. Direct-only GitHub routes were
  also tested;8 returned504, while Debian/Alpine verified directly. These failures
  are recorded, not reported as direct-route successes. The final app download
  paths recover through verified API responses. Actual HTTPS partial/resume tests
  passed for cancellation and injected disconnection, with HTTP206 and final SHA.
- All10 environments: native CLI install, basic shell/os-release check, cancellation,
  removal and neighbour preservation in a task-owned prefix. Arch/Manjaro use
  login and production keyring setup; Ubuntu AI uses pinned installer5.9.0.
- Published v2.1.5 helper reproduces Arch SCAN_ACCESS_DENIED/13 in native Termux.
  Fixed empty/nonempty sealed cases, explicit unscanned-size warning, cancellation,
  source/path protection and neighbour preservation pass. This is a prior defect.
  [Detailed environment report](VALIDATION-2.1.6-environments.md).

## CI — passed

Current source/workflow: `d22ab5d0f7cffda5b78f852860c3e67701924370`.
[Successful run](https://github.com/HezeAI/termux-jobs/actions/runs/42470194304).
All five module test tasks and debug APK assembly completed successfully.
XML totals: **1,943 cases; 1,934 passed, 9 skipped, 0 failures/errors**.
Eight SFTP cases require the local fixture; the final-release-manifest case needs
its supplied APK manifest. That manifest case passed in the separate final local
12-case updater run. Skips are not reported as CI passes.
Fresh CI inputs verified Gradle7.2, all four original bootstrap ZIPs and Alpine
rootfs/XFCE with378 package members. Source and workflow identities match above.
The later release commit changes documentation only; packaged-source hashes remain
identical to these CI inputs and the signed release build.
Earlier failed runs/logs remain preserved: removed Alpine r2 URL, gateway504,
wrong draft/short-ref attempts, Gradle/bootstrap gateway failures, and the initial
cancellation timing assertion. CI now verifies the same pinned Gradle/bootstrap
bytes through their official public release asset APIs; no toolchain pin changed.
None of these runs is rewritten as successful.

## Reused with identity proof

Earlier local34 Node suites;34 RTK state/installer,15 statistics,3 Lite family,
7 offline-file and8 Lint-checker cases retain their unchanged inputs. Test10 public
RTK install/update/no-network-reuse/error/cancel checks retain identical helper,
pin and binary hashes. Native Codex0.166.0 full/never and untrusted, Claude2.1.296
SDK manual/default used real CLI binaries with synthetic providers. This does not
verify cloud-model behavior or every permission mode.

Lite: grep160893→1308B, head5 470→470B, recall+525B; native: ls4410→1056B,
head5 25→25B, recall+600B. No command rerun or Lite double accounting. Unknown or
CLI-truncated bases remain unknown. Old estimates remain stored but hidden.
See [test10 evidence](VALIDATION-2.1.6-test10.md), [RTK limits](RTK-MODE.md),
[native measurement checks](VALIDATION-2.1.6-test9.md).

All95 unchanged native inputs retain verified hashes; seven packaged ELF files
match the earlier reviewed release. No X11/GPU execution is claimed by hash reuse.
The full initial local JVM run found four stale UI assertions, corrected in tests;
its failed results remain preserved. Final all-module coverage comes from CI,
not from treating that earlier run as passed.

## Phone and remaining limits

- **Phone UI:** developer acceptance of test10; individual scenarios were not
  enumerated. Final1533 and its changed dialogs were not automatically installed
  or independently accepted through phone UI.
- **Native Termux on the phone:** UID10620, TracerPid0, real base installs/shells and
  removal in isolated storage, plus read-only metadata diagnosis. These are app-
  service background tasks, not root-shell or GUI evidence.
- **Isolated local/CI:** JVM, Python, Node, network and final-artifact checks above.
- No full factory-reset bootstrap, desktop/GPU exercise, rollback phone install,
  real-cloud-model RTK/classifier test or independent security certification.
  Existing low-level handoff/crash/backup gaps remain in
  [security scope](SECURITY-AUDIT-STAGE2.md). Codex image-history WebSocket issue
  remains open; the verified published report is linked in release notes.

## Publication state

This tagged report records the verified candidate before public upload. No old
release or asset is replaced. The upload contains the APK, SHA256SUMS, localized update.json and validation.json;
no rollback or build-header archive. Public download/hash and updater consistency
checks follow publication. Evidence: `.verification/release-v2.1.6/`;
earlier baseline evidence: `.verification/release-v2.1.6-test10/`.

## Publication verification

Published as latest stable [v2.1.6](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.6),
release ID438132321, draft=false/prerelease=false. Annotated tag resolves to
`217310be741108a369dda62bccaa5ad2927143b8`; later report-only commits do not move it.
The signed APK, SHA256SUMS, update.json and validation.json were downloaded
anonymously and matched the prepared bytes. Downloaded APK package/code1533,
ARM64 ABI and certificate were independently rechecked. All37 older releases and
141 assets retain their IDs, names, sizes, digests and URLs. Components' prior
9 releases/142 assets also remain; the new build-header component is separate.

The ordinary SHA256SUMS URL succeeded. Ordinary APK/update/validation URLs returned
HTTP504 on the tested route, so those exact bytes were independently fetched via
the public asset API. Native Termux UID10620/TracerPid0 also received504 for the
ordinary update.json URL. The production updater parser accepts the final metadata
and latest stable identity, but successful end-to-end updater download on this
route is **not confirmed**. This is a transport observation, not proof of its remote
cause or a universal GitHub outage. No APK was installed automatically.

The draft API response briefly associated this release with a temporary untagged
name. The association was corrected to the already verified v2.1.6 tag; final
latest/tag/asset URLs are checked. No prior tag or binary was replaced. Published
validation.json remains the immutable pre-upload snapshot; this section records
the subsequent checks and their limitations.

Evidence: `public-download-verification.json`, `native-public-check.json`,
`public/signature.txt`, `public/badging.txt`, `old-releases-preserved.json`,
`components-preserved-final.json` under `.verification/release-v2.1.6/`.
