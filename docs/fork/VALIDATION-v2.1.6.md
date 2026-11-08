# Release validation — 2.1.6 /1532

Release candidate based on the developer-accepted 2.1.6-test10 /1531.
The developer authorized a stable metadata-only rebuild so the existing updater
can accept numeric versionName2.1.6. Only app/build.gradle version metadata changes;
638 production files and135 packaged asset/native entries match accepted test10.
No new functional changes, rollback APK, installation or user-session restart.

Final signed APK:40671985 bytes, com.termux, ARM64-only, non-debuggable.
SHA-256: `060a12de8e1d8d15f60dd7acac4617e5535bd730f5a68efbcaa34878db650ad7`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Code1532 exceeds previous main1520, rollback1521 and test10 1531.
No data reset or environment reinstall required; storage schema unchanged.
Actual phone installation over rollback is not separately verified.

## Checked anew

- Signed release build, signer/X11 guards, ZIP/package/version/ABI/signature,
  DEX runtime, source/assets, notices, no embedded RTK archive and SHA-256.
- Full Modern Lint7.4.2, all5 modules:0errors/290warnings; no rule-loading failures.
  Legacy Kotlin metadata warnings in the old APK lint chain are not treated as
  a successful Lint gate. The compileSDK/AGP support warning remains documented.
-34 Node suites;34 RTK state/installer cases,15 statistics,3 Lite families,
  7offline-file cases,8Lint-checker cases; locale resources.
- Actual public RTK jobs.1 download, archive/binary SHA-256, runtime startup.
  Published binary archive5621920 bytes, SHA8a43d2bf…; no component republish needed.
- Real-binary Lite/native production-hook fixtures: Lite grep160893→1308B,
  head5 470→470B, recall+525B; native ls4410→1056B,head5 25→25B,recall+600B.
  No rerun for accounting; no Lite native double counting; second chat unchanged.
- Full JVM baseline and targeted final-metadata/updater cases are being finalized.
  Old sidebar/header and context-position assertions were found and aligned in
  tests only. The first failed log is preserved, not counted as successful.
- GitHub debug CI will run on the candidate source commit before publication.

## Reused with identity proof

The test10 installer/upgrade tests, including actual HTTPS install, reuse without
network, live old binary preservation, error/cancel and mode/stat/recall snapshots,
remain valid: production helpers/pins/binary hashes match. Native CLI fixtures use
Codex0.166.0 full/never and untrusted; Claude2.1.296 SDK manual/default with synthetic
providers/network guards. They are not real-cloud-model tests. Native metrics
cover captured ls/Git/search/read and selected windows; unmeasured/ambiguous/CLI-cut
results remain unknown. Recall is separately returned text, not saved file size.
See [accepted-build evidence](VALIDATION-2.1.6-test10.md),
[RTK limits](RTK-MODE.md), and [native-byte investigation](VALIDATION-2.1.6-test9.md).

Unchanged JNI reused after95 native-input checks and final output hash equality.
No GPU/X11 execution or global component installation is claimed.

## Phone / not checked

The developer reports phone acceptance of test10. Individual scenarios were not
itemized; this does not establish exhaustive multi-device coverage. The stable
version-only rebuild has not been separately installed on a phone by the assistant.
No APK is installed automatically. Low-level Android key-handoff/crash/backup gaps
remain in [security audit scope](SECURITY-AUDIT-STAGE2.md). Known Codex WebSocket
image-history replay remains unfixed; the published report is linked in release notes.
Auto Claude real-model classification and other RTK permission modes are unverified.

## Publication status

Candidate; final CI/local outcomes, source commit/tag and anonymous public download
checks will be recorded before declaring publication verified. No old assets are
replaced. New release contains no rollback APK. Update metadata comes from the
final signed APK with localized compatibility notes.

Local evidence: `.verification/release-v2.1.6/` and initial full-JVM evidence
`.verification/release-v2.1.6-test10/`. The older directory name reflects the
superseded exact-file publication plan, not the final stable version.
