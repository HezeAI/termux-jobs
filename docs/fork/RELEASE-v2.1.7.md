# Termux Job’s 2.1.7

This update fixes RTK output accounting and improves recovery when removing an old Arch environment or restarting a disconnected agent chat.

[Download APK](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.7/Termux-Jobs-v2.1.7-arm64.apk)

## Changes since 2.1.6

- **Native RTK statistics:** measurements now match supported single commands, quoted search patterns, sequential shell commands and yielded/background calls when their final result is received. Unconfirmed baselines remain unknown; commands are not rerun to estimate savings.
- Recall output is counted separately. Recall help and list commands do not count as recovered output. Existing statistics and recovery files remain available.
- **Arch removal:** handle the residual, sealed `run/termux-jobs-wayland` directory while retaining path checks, active-environment protection and checks that protect neighbouring environments. Nonempty directories still cause a clear refusal.
- **Agent restart:** the restart button can recover a disconnected chat using its validated local conversation ID and the existing replacement path. Repeated/stale requests are guarded; other chats and existing conversation data are preserved.

## Compatibility and limitations

Android 8 or newer, ARM64. Same `com.termux` package and signing certificate; versionCode **1536** exceeds 2.1.6 and the accepted test build. No data reset or environment reinstall is required. Settings and statistics formats are unchanged.

The **RTK 1.1.0-jobs.1 binary is unchanged**. No new component download is needed. Open **Limits and context** after updating to refresh the application's observer. Working chats are not restarted automatically.

RTK pipelines, expansions and other calls without a reliable match keep an unknown baseline. A completed, polled command is different from a detached shell job. Estimated tokens describe command output, not subscription quota or the entire context. Native RTK permission caveats, Claude root/Ubuntu restrictions and the known Codex image-history WebSocket issue remain unchanged.

The developer checked the release candidate on a phone and approved this stable release. Only release metadata changed afterward. Claude coverage includes the actual CLI with a synthetic local provider; it does not establish real Anthropic model behaviour or every permission mode.

No rollback APK is included. Previous releases and their assets remain available.

<details>
<summary>Verification and build details</summary>

The signed ARM64 release APK is built and verified locally with the established release key. Checks cover version/package, signature, ABI, ZIP integrity, runtime/DEX/assets, SHA-256 and updater metadata. [Debug CI](https://github.com/HezeAI/termux-jobs/actions/runs/42476694520) passed:
1,940 tests passed, 9 skipped. It is separate from the signed release APK checks.
Full local Modern Lint: 0 errors and 290 warnings. Sixteen local updater tests
validated the final metadata and package checks.

An initial CI run failed a stale test expectation that the restart button disappears
after an agent exits. The test now checks the accepted recovery behaviour; no
production code or APK bytes changed for this correction. Skips and the original
failure are documented in the validation report.

APK SHA-256: `f321c19652d80dd91cf96a46e4fcefffd1f309faebb61e3cc64ab4f0fe5bbebb`.

The accepted code includes live Codex checks for search reduction, unchanged head, sequential and yielded commands, recall and unknown baselines. Claude CLI fixture results and phone-check attribution are separated in the reports. No new cloud-Claude or all-permission-mode claim is made.

[Release validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.7/docs/fork/VALIDATION-v2.1.7.md) · [RTK verification](https://github.com/HezeAI/termux-jobs/blob/v2.1.7/docs/fork/VALIDATION-2.1.7-test2.md) · [Arch and restart checks](https://github.com/HezeAI/termux-jobs/blob/v2.1.7/docs/fork/VALIDATION-2.1.7-test1.md) · [Checksums](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.7/SHA256SUMS)

</details>

AI development assistance: OpenAI Codex
