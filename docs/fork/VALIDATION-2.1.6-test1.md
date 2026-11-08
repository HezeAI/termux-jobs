# 2.1.6-test1 / 1522 — private test APK

Prepared 2026-11-07. No APK installed or published. Evidence is retained in
`.verification/rtk-mode/` in the active checkout.

## Final APK

- Package: `com.termux`; version `2.1.6-test1`, code `1522`; ARM64 only, non-debuggable.
- Size: 40643310 bytes.
- SHA-256: `90d3522f00331ca86f208f9c15ea3970d12fdda3a39b7934ac294c1dccee9596`.
- Signing certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (same release signer).
- ZIP, v2 signature, runtime/DEX, RTK assets and Apache license checked anew.
  All 629 staged production sources and 131 packaged assets match current inputs.
- Package/signer are compatible with an update from stable1520 or rollback1521.
  An actual installed update and data retention on a phone were not tested.

## Checked anew — local

- Full Modern Lint 7.4.2, all five modules: zero errors, 287 warnings.
  Final rebuild and analysis include the RTK window lifecycle correction.
- Targeted Java run: 51 tests passed. After the last lifecycle change, all three
  RTK dialog tests passed again, including cancellation and parent-view detachment.
  Other layout/composer/navigation suites are recorded in the earlier task logs.
- Python installer/state suite: 26 executions passed (10 inherited cases repeated).
  Covers nonce cancellation, corruption, short download, insecure redirects,
  per-chat isolation, installation reuse and bounded output fallback.
- Official RTK archive downloaded into an isolated fixture; pinned archive,
  binary and license hashes, ARM64 ELF and real startup verified.
- Node adapter/regression suites passed after fixture dependency updates.
  The initial chat-file-output fixture lacked agent-attachments; its corrected
  rerun passed. Initial failures remain in logs, not counted as passes.
- TerminalModeRow tests cover measured full/compact labels, narrow widths and
  font scales. RTK dialog tests are Robolectric, not phone interaction.
- Initial Java socket tests failed because the fixture temporary path exceeded
  the Unix socket limit. Rerunning with the project's short temporary path passed.

## Checked anew — real CLI binaries, synthetic provider

These checks ran actual Claude 2.1.296 and Codex 0.166.0 executables, with isolated
settings and fake credentials. A kernel network guard blocked external access.
They do not test decisions by real cloud models or production authentication.

- Claude production SDK adapter: reduced Git diff reached the provider and native
  denial remained effective. Final evidence: `native-sdk-final.log`, native-278c6f9a.
- Codex app-server and production RTK hook configuration: forbidden prefixes for
  git status/diff/log stayed blocked in danger-full-access + approval=never.
- Codex untrusted approval: decline blocked execution; explicit acceptance allowed
  execution/filtering; explicit forbidden rules still blocked.
- Same-process RTK enabled/disabled/enabled cycle: filtering true/false/true;
  the forbidden command remained denied in all three states.
- Git error outside a repository preserved stderr and exit status 128.
- Saved full stdout matches the Git output byte-for-byte. One measured fixture
  reduced stdout from 11,840 to 2,409 bytes, including the full-output path hint.
  This is not a context-total, token-billing or subscription saving measurement.

The rejected candidate used `2>&1`. Codex's plain-command parser rejected that AST,
so prefix matching fell back to shell argv. The final pipeline removes redirection
and retains pipefail. No saved rules, approval policy or sandbox setting changed.
No bypass warning is shown for this corrected implementation. Results apply to
these commands, versions and tested modes; they are not a universal policy proof.

## Reused with input verification

Native APK payloads were retained from the established build. All 95 relevant
native inputs were rehashed; packaged ELF bytes match the earlier stable artifact.
Java, resources and application assets were rebuilt. No X11/GPU phone test was run.

## Not checked / blocked by the fixture

- Successful Codex read-only/workspace-write execution: native bubblewrap rejected
  a fixture socket-directory host mount. Forbidden rules still blocked, but command
  execution and RTK integration in those sandbox modes remain unconfirmed.
- Real Claude/Codex model decisions, account permissions and cloud requests.
- Phone installation, first/reused RTK setup, network loss, cancellation at each
  download stage, approved chat restart, application restart and UI restoration.
- Narrow phone, increased system font, orientation and both terminal types.
- Actual phone confirmation of the terminal close dialog. The checkbox was removed;
  code preserves its former default history-retention action, not running processes.
- Gemini/OpenCode integration is unavailable with an explanatory UI message.

## Manual next check

Install this test APK manually. In an Ubuntu AI Claude/Codex test chat, open Limits
and context → RTK Mode. Confirm installation and, only if offered, restart that
specific test chat. Ask for a supported Git diff/status/log command on synthetic
files; inspect the byte statistic and retained full-output path. Disable RTK and
repeat. Verify the phone scenarios above before any release decision.
