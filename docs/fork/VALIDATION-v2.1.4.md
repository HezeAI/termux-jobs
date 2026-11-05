# Release validation — v2.1.4 /1492

Compared with the last published stable release, **v2.1.3 /1485**, verified through
the GitHub latest-release API before preparation. Published assets/tags/Components
remain immutable. Same com.termux/certificate, ARM64, min26/target28/compile35 and
schema8. No data reset, environment reinstall, APK installation or working-session
stop is performed. [Release](RELEASE-v2.1.4.md).

## Scope and phone source

**Developer report, 2026-11-05, 2.1.4-test6 /1491:** the agreed history, file window,
search, cards and JPG/GIF gallery export work on the phone; accepted for stable.
Earlier test5 acceptance of previous-message loading is preserved separately in
[its report](VALIDATION-2.1.4-test5.md). No assertion that all four agents, every
Android release or every image format was individually tried on the phone.

The source changes actually packaged since v2.1.3 are current-context meters and
request timers, explicit Codex compaction, notes-only star menus, both file-window
dates, Files/Attachments/Changes, late native history cursor discovery, safe local
original-image gallery export and exact four common templates. Full composition
is in the release notes. Preparation/authorization/backup fixes already shipped
in v2.1.3 are not announced as new. Image accumulation remains unfixed.

## Checked anew

Stable compilation/package/signing, enabled release-certificate and X11 guards,
final APK ZIP/identity/certificate/SHA/DEX/native/assets audit, exact template archive,
localized updater metadata and full modern Lint. Fork CI runs the broad JVM suites
and an internal debug build on the release source; its result/run is recorded in
the release technical details and validation.json before publication.

Local Node tests use synthetic data; no real account login or working chat.
The final-file identity and detailed check counts are appended after the build.

## Reused, not rerun as phone tests

[Accepted candidate](VALIDATION-2.1.4-test6.md): 372 JVM cases (38 suites) and four
final Android-permission dispatcher cases, all passed with no skips. The stable
preparation changes only version metadata/documentation; production/test/resource
identities are compared with accepted source d8e0b07f. The release's broad CI is
fresh; these prior local cases are labelled reused rather than freshly executed.

Earlier native CLI/context probes retain their original versions, kernel-fenced
fixture/provider conditions and limitations. No new model-account experiment is
performed for release preparation. Seven final ELF payloads match v2.1.3 bytes;
95 native/config input identities are checked before verified reuse. A previous
unchanged-source/different-compiler native mismatch remains documented in the
[candidate history](VALIDATION-2.1.4-test1.md), not silently overwritten.

## Archive, privacy and licensing

AI-INSTRUCTIONS.zip is extracted only from these final APK assets:
agent-instructions-ubuntu-ai.md, agent-instructions-ubuntu-ai.en.md,
agent-instructions-claude.md and agent-instructions-claude.en.md. Each entry is
compared with the APK and agreed source; only an English README.txt is added.
Exact member allowlist, UTF-8, secret-pattern review and absence of project/memory/
private instructions are checked. No saved user instructions or authorization are
read. Hashes and the ZIP digest are recorded; SHA256SUMS covers release assets.

No new third-party library/native/component is introduced. Existing notices and
corresponding-source scope remain as in v2.1.3 and the
[license review](LICENSE-REVIEW-2026-11-03.md). Final APK notices are checked;
Components metadata is snapshotted before/after publication without mutating assets.
This is not a new exhaustive legal audit.

## Remaining and excluded

Unfixed accumulated-image disconnections; no new automatic compaction, CLI fork or
experimental context window mode. Low-level Android Gemini key handoff/peer
ownership/Keystore/cancel/crash recovery and device backup export/restore remain
unverified by agreement because no separate Android instance is available.
Functional artificial-key transfer, API rejection and normal Gemini retry retain
the user's earlier accepted status; real API/Vertex login is not established.
Older profiles/all routing combinations, heavy WebGL/GIF rendering, Android8–9
permissions and uncommon gallery formats retain their device-specific gaps.
A hard kill during gallery export may leave a hidden pending row for OS cleanup;
handled error cleanup tests do not establish crash cleanup on a real device.
MCP and plugins are excluded. Working folders/instructions/approvals are not an
OS sandbox. Full traffic/drivers are separate directions. No independent security
certification is claimed. [Audit matrix](SECURITY-AUDIT-STAGE2.md).

## Publication gate

Publish only after successful applicable checks and CI, final APK/updater/archive
consistency and user authorization (given with candidate phone acceptance).
The publication record is attached to the release and validation.json: commit,
run, hashes, anonymous downloads and Latest. Tagged preparation evidence remains
unchanged; publication never replaces an older APK/tag/Components resource.

## Final file identity and local results

- APK: `Termux-Jobs-v2.1.4-arm64.apk`, **40,527,462 bytes**.
- SHA-256: `0e63a72139e60ade77efc912190756fbd6ad121f7b2b1a04f11524ed02c874c2`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`; v2 signature verified.
- `com.termux`, **2.1.4 /1492**, ARM64 only, minSDK26/targetSDK28, not debuggable.
- ZIP CRC and outer-record gaps passed. DEX/runtime required missing classes: **0**.
  This is a final-file audit, not an Android installation/runtime test.
- **584** production inputs and **123** assets matched; seven ELF payloads equal
  stable bytes. Release signer and X11 packaging guards were enabled.
- Fresh modern Lint 7.4.2: **0 errors /246 warnings**, all five modules, no
  rule-loading errors. Four added API29-constant warnings are reviewed modern-only
  MediaStore branches; warnings are retained. Old packaging Lint Kotlin metadata
  diagnostics are not used as the successful gate; compileSDK35 exceeds AGP's tested range.
- Fresh **26 Node suites** and **69 Python unittest cases** passed with synthetic
  data. Reused local candidate JVM **372 +4** cases are labelled separately above;
  the full CI outcome remains in the release verification record.
- AI-INSTRUCTIONS.zip SHA-256: `c95cbb4e4bba49cf9b399d8e07469d45e13da80612180863e7cb71d0afd4a7dd`;
  four exact APK templates + English README.txt, no other members. Codex RU is
  28,861 bytes; exact per-member hashes are in validation.json.
- update.json schema1, code1492, APK size/hash/version and 13 RU/EN lines including
  explicit compatibility are validated. SHA256SUMS includes the archive.

Relevant executable/resource/test/fixture diff from accepted d8e0b07f is empty;
only version metadata and documentation change for stable. No model/CLI limits,
transport or working-session settings are changed. Previous issued APK hashes
are checked unchanged. New templates are bundled assets, not saved-user migrations.

## Publication completion — 2026-11-05

Published ordinary stable [v2.1.4](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.4),
Latest, draft=false/prerelease=false. Branch fork/base and annotated tag resolve to
**384fd197ca30948164fbded3e465f15c2b9318bf**. Release assets and tag are unchanged
after publication; only release text was updated to record completed public checks.

[CI 42002718204](https://github.com/HezeAI/termux-jobs/actions/runs/42002718204)
passed: **1,755 tests** (app 1,601; terminal-emulator 154), **8 skipped**; internal
independently signed debug APK build passed. Skips are ServerSftpTest's explicit
assumption of the Termux-specific sftp-server path, unavailable on Ubuntu CI.
These are not passed CI cases. All eight separately **passed locally** using the
existing real subsystem over pipes and artificial task files, without remote SSH,
working profiles or authorization. Source/test code is unchanged from v2.1.3.
Other test modules with NO-SOURCE are not counted as passed cases.

All seven draft uploads first matched the local files. Then all seven public
resources were downloaded anonymously and matched byte-for-byte. SHA256SUMS,
update.json version/code/size/hash, ZIP members versus the public APK, source tag/
branch and release documentation links passed. The downloaded public APK passed
signature, version and DEX/runtime checks again. Public Latest and final release
body were checked after adding the publication note. Old v2.1.3 asset identities,
Components metadata and all issued local APK hashes remain unchanged.

Evidence: `.verification/release-v2.1.4/public-verification.json`, `public/`,
`public-signature.txt`, `public-runtime.json`, `ci-final.json`, `ci-reports/`,
`sftp-jvm/` and `validation.json`. The uploaded validation.json records the verified
pre-publication snapshot; the completed public-download record is in the release
body and this local post-publication report. This documentation-only completion
is saved locally for continuation; it does not move the published source tag,
replace any resource or require an APK rebuild. No install or working-session stop.
