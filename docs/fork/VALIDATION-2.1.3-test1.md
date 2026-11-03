# Local test release 2.1.3-test1

2026-11-03. Private test APK; no GitHub publication, push, tag change or update-channel change.
The published v2.1.2, its APK, tag and Components remain unchanged.

## Final APK

- Package: `com.termux`; versionName `2.1.3-test1`; versionCode `1477` (previous stable: `1476`).
- Size: 40,457,069 bytes; ARM64 only; not debuggable; APK v2 signature verified.
- **File SHA-256:** `f4765f3fd0d7f12b2478ddc1cafca31b1d6a3c804377658f7d649323b38fffbf`.
- **Certificate SHA-256:** `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Matching package/certificate permits Android update compatibility with the checked
  Termux Job’s v2.1.2. It does not establish compatibility with official Termux.
  User data migration on a phone and downgrade to an older versionCode were not tested.

## Changes and separate local commits

| Change | Local commit | Detailed validation |
| --- | --- | --- |
| F-LOG-01: bounded agent preparation status, no full output retention | `8c5f88e6ff7baad50e92dcdd496b331801891db3` | [Preparation](VALIDATION-preparation-structured-status.md) |
| F-NET-01: finite transfer/connection/low-speed limits and owned-child queue lifetime | `ed59dbe9e262cd5d969e334f9b49fc24ad932444` | [Download bounds](VALIDATION-preparation-download-bounds.md) |
| F-LOG-02: EnvironmentSetup collection/result/UI/export use structured status only | `9045f699e2727fec2935b9b9d8486683abb04e5d` | [Environment status](VALIDATION-environment-structured-status.md) |
| F-SIGN-01: release requires existing pinned signer; no automatic key generation | `d3ea24ae8d91fa948a8aa1fc10a440ffec5df355` | [Signer gate](VALIDATION-release-signer-gate.md) |
| Transparent agent composer; horizontal star / paperclip; measured scroll clearance | `de324cc989d903338c9eed81e3df26f0858d3445` | [Composer](VALIDATION-chat-composer-overlay.md) |

No chat-history, terminal/CLI journal, existing log-file deletion, agent permission
profile, environment replacement, robot or voice-input change is included.
Environment details/export now contain only the current bounded structured report;
legacy raw logs remain on disk and are not loaded into that UI. Unknown process
output and unfinished stream records are discarded, not shown as errors.

## Checked anew

- Release Java/resources/assets were compiled and packaged locally with Gradle 7.2 /
  AGP 4.2.2 and the existing ARM64 toolchain. `verifyX11Inputs` remained enabled and passed.
  The new signer gate ran and the final independent APK signer check matched the pin.
- Final ZIP integrity, manifest/package/version/ABI/debug state, public signer and
  file digest passed. All 120 packaged assets matched their final inputs. All seven
  ELF payloads, including asset libraries, were ELF64 AArch64. No obsolete local ZIP
  headers were found in inactive packaging gaps. Required third-party notices remain.
- DEX runtime check: 14,180 defined classes; 31 reviewed optional missing types.
  The new structured-status class exists; the obsolete raw Capture class is absent.
  Voice permission/declarations/classes remain absent. Runtime checks do not prove
  every method linkage or behavior on Android.
- Full modern Lint 7.4.2 across all five modules: **0 errors, 234 warnings**.
  The known compileSdk/AGP support warning remains; no infrastructure failure was counted as PASS.
- F-LOG-02: 69 targeted JVM/UI checks and seven artificial real-child/pipe checks.
  Artificial multiline/oversized/unfinished secrets are absent from current report
  and export. Old-file byte preservation, errors, cancellation and retry are covered.
- Download regressions rerun after the structured-status change: all nine real local
  TLS/process/queue scenarios passed, including a stopped body, slow successful body,
  finite attempts, owned interruption, cleanup before queue release and next success.
  Only policy durations changed in the fixture copy; no external service was called.
- F-SIGN-01: eight Gradle checks with an artificial JKS, including missing key,
  wrong signer/password/alias/pin and successful retry. Separate entry-point fixture
  proves missing key cannot generate a replacement. The actual public signer passed.
- Composer/history/controller: 114 targeted JVM/Robolectric checks, including narrow
  width, larger text, simulated keyboard resizing, multiline drafts, anchors and footer.
  They are model checks, not phone acceptance.

Initial full
run failed five tests because a long fixture directory exceeded the guarded Codex
socket-path limit. All five errors explicitly reported `Codex socket path too long`.
The guard was preserved. The repeat uses an existing short real canonical temp
path, not a symlink. Original logs/XML remain. A mistakenly broad repeat started
bootstrap preparation and was interrupted before tests; it is not counted as PASS.
The final repeat uses the existing verified bootstrap inputs.

Final full JVM result: **1608 cases, 0 failures, 0 errors, 0 skipped**. App and terminal-emulator test tasks ran; terminal-view, termux-shared and X11 have no separate test sources (NO-SOURCE, not additional passed tests). Results/logs are retained separately from the initial failed run.


## Reused with input checks

No native source or native build flag changed. All 95 pinned native input hashes
matched the previous release evidence; the relevant compiler/linker flag block
also matched. The three principal native libraries were reused byte-for-byte from
verified v2.1.2, rather than described as freshly compiled. X11 runtime assets and
bootstrap were hash-checked against their pinned inputs. This task did not require
or perform GPU execution or a new native-toolchain installation.

Release build warnings from the older D8/Kotlin metadata toolchain are preserved
in build.log; packaging succeeded and final DEX/APK checks passed. Debug JVM tests
are internal verification, not a build/validation of the published v2.1.2 release
APK. This task builds and checks a separate signed **release** test APK.

Evidence: `.verification/release-2.1.3-test1/`,
`.verification/environment-structured-status/`, `.verification/release-signer-check/`
and `.verification/chat-composer-overlay/`. The audit matrix and historical source
snapshots remain in `.verification/security-v2.1.2-c0073b46/`; published-source findings
are distinguished from locally fixed code. No independent external audit is claimed.

## User-reported phone acceptance — 2026-11-03

The developer/user confirmed acceptance of **2.1.3-test1 on a phone**:
chat layout, keyboard, buttons, scrolling and the check of an **existing Ubuntu AI**
work. This is the user's device report, not a device check independently performed
by the assistant or an independent audit. The report does not specify individual
agent/device configurations or every edge case below.

This acceptance does **not** verify a clean Ubuntu AI installation, missing-component
installation, interruption/recovery, network failures or production-duration network
timeouts. It does not change the historical results for published v2.1.2.

## Remaining detailed phone check plan

The general UI and existing-environment check above are accepted. The following
list remains a detailed checklist; unreported edge cases are not counted as PASS.

1. Update the existing compatible Termux Job’s installation manually; do not uninstall
   as an update step. Confirm existing profiles, history, projects and authorization remain.
2. In each agent chat, verify star / paperclip independent feedback/actions and Send
   on the right. Scroll messages behind transparent areas; keep the field readable.
3. Open the keyboard, enter several lines and increase font size on a narrow screen.
   Raise the last message fully above the composer; check attachment/extra-key footer.
4. Switch chats and return. Check draft, selection, attachments and scroll position.
   Open ordinary terminal and confirm its input geometry. Check stop confirmation unchanged.
5. On an isolated test environment, check typed preparation stages, real byte progress,
   errors, cancel/resume/retry and structured export. Confirm no new raw logs and no
   deletion/loading of existing logs. Closing preparation must not cancel its operation.
6. Separately test a genuinely clean Ubuntu AI install and an existing Ubuntu AI with
   missing components. This task used artificial process fixtures, not a real install.
7. Separately check mobile-network transfer limits: 45 s connect, low speed below
   1 byte/s over 180 s, 7200 s per attempt, at most three attempts with 2/4 s backoff.
   Slow/stalled/exhaustion/queue cases passed with reduced test constants; production
   durations were not waited on a phone. These are download limits, not an overall
   environment-preparation timeout. Model conversation/authentication is a separate check.

Remaining architecture risks are unchanged: shared Android UID/proot boundary,
explicit agent file/command privileges, Gemini crash handoff residue uncertainty,
trust in native drivers, sensitive user-selected backups and unknown past key custody.
The automated audit used artificial data and did not access real credentials or working
sessions. The separate user-reported check used the existing Ubuntu AI as described above.
