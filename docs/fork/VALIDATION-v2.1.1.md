# v2.1.1 release candidate validation — 2026-11-02

Status: prepared for review; not published. Working checkout:
`/root/termux-fork/.verification/rollback-live-actions/source`.
The baseline is GitHub HezeAI/termux-jobs v2.1.0, published
2026-10-29T08:34:33Z, commit ad3b63f8266f688a9bb8e3d4fcca566d346072d0.
Both tracked changes and new source files were included in the comparison.
The release summary describes final behavior, not private-test history.

## Final artifact

- File: Termux-Jobs-v2.1.1-arm64.apk; 39,960,289 bytes.
- Package: com.termux; versionName 2.1.1; versionCode 1454, above test19/1453 and reserve1451.
- ABI: arm64-v8a only; non-debuggable; ZIP integrity and APK v2 signature verified.
- SHA-256: `4e740444565bbc3211d797d1b8940de6ca3052f837b0dcfba4ecf06f2df045a1`.
- Signing certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Runtime inventory: 14,048 defined classes; no required or unreviewed missing types. The 31 reviewed optional/metadata references are not a claim of universal runtime compatibility.
- Helper implementation is excluded. Native libraries and assets match delivered test19 byte for byte.
- update.json was generated from this APK; its package, version, size, hash and RU/EN notes were verified. SHA256SUMS covers APK and updater manifest.

## Checked anew

- Release compilation and final artifact checks above.
- Modern Lint 7.4.2 across all five modules: zero errors, 234 warnings, no infrastructure diagnostics. Existing warnings remain visible; compileSdk exceeds the build plugin's tested range. Older Kotlin metadata diagnostics occurred in the successful APK build.
- Selected Java/UI scope: 196 distinct cases for instructions, persistence, delivery, active settings, forms, menus, chat, narrow layout and navigation. Initial run: 195 passed, one test checked state before the UI write acknowledgement. The test-only wait was corrected; the entire navigation class passed on rerun. Initial results are retained.
- Additional indicator state and localized time suites: 5 tests passed. An initial invocation unnecessarily entered the bootstrap-download stage; only its verified task-owned Gradle processes were stopped. The scoped rerun excluded that unchanged preparation stage and passed. No working sessions were affected.
- Python instruction/environment fixtures: 48 tests passed. No real environment installation or removal.
- Native Codex 0.166.0 local-provider turns, history and completion/duration; native Claude 2.1.296 active-response/tool follow-ups, ordering, finish race, cancellation and reconnection passed. These use synthetic loopback providers, not a cloud-account/network reliability test.
- Four-adapter regression checks, pinned Gemini hooks, scoped Stop safety and completion-time persistence fixtures passed. Fake-process tests block real signals.
- Documentation/diff, localized updater metadata and final source inventory are checked separately from device behavior.

563 production source files were compared with the compilation stage; no differences were found. APK private-file-name checks and current release-document local links passed.

## Reused

Existing SDK, dependency caches and prepared JNI/bootstrap inputs were reused. No full SDK or rootfs copies were created.
`test19-input-identity.json` records matching production/test inputs before release version changes; generated Python bytecode is excluded from source identity. The navigation test-only wait correction was rerun separately. Final APK native/assets identity was checked against the frozen delivered test19 artifact.
Unchanged graphics/environment payload identity does not constitute a fresh desktop execution or clean-device installation test. Prior detailed evidence remains in the named private-build reports.

## Device evidence and remaining limits

HezeAI confirmed execution dots, answer timing and website cards on a phone. No other phone checks are attributed to that report.
Actual keyboard, large-font/narrow-screen interaction, animation smoothness, instruction import/restore usability and all four agents' live transitions still need device acceptance. No fresh full environment installation or X11/driver execution was performed for these chat/editor changes.
Existing processes load changed adapters after an explicit normal restart with conversation continuation. CLI features depend on the installed version. Restarting an adapter can lose old transient delivery captions. Timing metadata does not expand the existing backup format; old history without reliable timestamps remains untimed.
Workspace schema8, package and signer are retained; no incompatible settings migration or automatic reset is introduced. This is not a guarantee for arbitrary manual downgrade or interrupted installation.
No fix for the other phone's transport interruptions is claimed. CLI admission does not prove model uptake.

## Evidence

Compact local logs, hashes, initial and rerun test results, baseline metadata and source identities are under `.verification/release-v2.1.1/`. Local evidence is excluded from release sources. Public files: APK, update.json, SHA256SUMS, validation.json and this report. Release notes: [RELEASE-v2.1.1.md](RELEASE-v2.1.1.md).
