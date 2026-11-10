# Release validation — 2.1.8 /1540

Final local gates passed. Public release identity and GitHub CI are recorded in the
release asset [validation.json](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.8/validation.json).

## Scope and acceptance

Changes relative to v2.1.7: timeline title/command layout and local classification,
native description forwarding by four adapters, sidebar states, permissions shield,
notification names and idle X11 control-service cleanup. Data formats, RTK component,
component loaders, permissions, authentication and command execution are unchanged.

The developer accepted test2 /1538 session labels and test3 /1539 action cards on the
phone, including a live 20-second command showing Running. The test3 APK SHA-256 is
`cb63f6294e47f5c814aad04b0b3a341f259866b3d6294eae541a63031b1cbeec`.
Subsequent production changes are only the requested exclamation mark in the shield
and release version metadata. The final stable artifact requires separate checks.

[Timeline checks](VALIDATION-TIMELINE-ACTIONS.md), [session labels](VALIDATION-SIDEBAR-STATUS.md),
[notification lifecycle](VALIDATION-X11-NOTIFICATIONS.md) preserve detailed evidence.
The original Arch service was not stopped: ownership was not established. Its final
state is not claimed. No working environment or chat is stopped by this release work.

## Verification

Checked anew locally: 188 targeted JVM/Robolectric tests passed, zero failures,
errors or skips. This includes action cards, sidebar states, X11 notification lifecycle,
desktop ownership and the updater against the actual final update.json in both languages.
Seven Node adapter/event suites and localization validation passed.
Modern Lint 7.4.2 analyzed all five modules: zero errors, 291 warnings, no infrastructure
errors. The extra ViewConstructor warning is for a programmatically constructed ActionRow.
Compile SDK/AGP range warnings remain. Legacy Lint Kotlin-metadata diagnostics in the
assembly log are not counted as a pass; the modern check is separate.

Final APK: com.termux, 2.1.8 /1540, minSdk 26, ARM64 only, non-debuggable.
Size: 40,677,933 bytes. SHA-256:
`3a2198eb0bf6f69d5f00d63cba65229042aee2b0189013f4d473ee712cc344a7`.
Certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
ZIP, signature, DEX/runtime classes, 640 staged production inputs, packaged assets,
all seven ARM64 ELF files, notices and unchanged public RTK jobs.1 pin passed.
VersionCode exceeds stable 1536, the previous rollback 1521 and all issued tests through 1539.
No data format, reset or migration change. Android installation can stop application processes.

The accepted source manifest matches every production file except workspace_shield.xml
(the requested exclamation mark). Version metadata also changed. Native source/build
inputs match v2.1.7; the signed build reuses their previously verified binaries.
The separate reproduction rebuilds our native code rather than reusing those outputs.
See [reproduction procedure/results](REPRODUCIBILITY-v2.1.8.md).

GitHub debug CI is distinct from local release verification. Its exact run and source
commit, plus the public-download results, accompany the published release in validation.json.
No CI release reproduction is implemented or claimed.

## Limits

Phone feedback is attributed to the developer. No new real-provider/model test,
all-permission-mode test, GPU matrix or full security audit is claimed. The shield
exclamation mark and release metadata have not had a separate phone acceptance.
The original security-audit dates remain unchanged. Existing known limitations
are retained in README and thematic documentation.
