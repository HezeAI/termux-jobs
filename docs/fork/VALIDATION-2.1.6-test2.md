# RTK partial coverage expansion — 2.1.6-test2 /1523

2026-11-08. Private APK; not installed or published by the assistant.
The user confirmed RTK worked in their current chat before this expansion. That
confirmation applies to the previous Git-only build, not these new command groups.

## Why the first stage was limited

The bridge deliberately accepted only three Git families while testing native
command policy. Earlier redirection hid Git prefixes from Codex; the corrected
pipeline keeps original commands visible and filters stdout only. This release
expands that partial implementation. It does not claim complete RTK coverage.

[Exact command matrix and fallback behavior](RTK-MODE.md#expanded-coverage-in-216-test2-1523).
Gradle uses RTK read on captured stdout, plus its final 40 lines, not the standalone
RTK Gradle parser. That parser is absent from RTK1.1.0's pipe filters. No build is
executed inside RTK. Terminal file reads are previews with a full-output path.
Unsupported/compound invocations remain ordinary. Existing rules are not changed.

## Checked anew — local

- 26 installer/state test executions passed (including inherited repeat cases).
- Three coverage tests passed: classification/refusal cases; 10 filter families
  each with exit codes 0/1/2/17/128 and exact stderr; full-output retention;
  diagnostic passthrough; atomic bridge refresh. This includes synthetic outputs
  for Cargo, Go, CTest and Vitest, not installed-runner integration for those tools.
- Eight real local commands compared with their RTK pipelines: grep match/no-match/
  missing file, cat existing/missing file, actual Gradle success/failure tasks,
  actual pytest assertion failure. Exit codes match, stderr preserved; failure
  diagnostics checked. Gradle tasks use an isolated no-dependency build, not the
  application's full test suite. See real-execution-results.json.
- Representative measured stdout: grep 21,682 → 928 bytes; cat 17,290 → 4,801;
  Gradle 12,419 → 4,075. Recovery hints are included. Not context/token/quota savings.
- Full Modern Lint7.4.2 across five modules: zero errors, 287 warnings.
- Three RTK dialog JVM tests passed again, including cancellation/lifecycle.
- Final Java/resource/APK compilation succeeded. The build's legacy Lint/Kotlin
  diagnostics are not counted as a passing analyzer; the separate Modern Lint is.
- Python syntax/import, git diff whitespace and final staged source identity checked.

## Checked anew — actual agent binaries, synthetic provider

Claude2.1.296 uses the production SDK adapter. Codex0.166.0 uses app-server and the
production hook setup. Isolated HOME, fake credentials, external network blocked.
No production account authorization or real cloud-model requests were used.

28 asserted cases in native-summary.json cover the four requested groups:
- grep with filename/line output, cat and pytest: compressed stdout reaches both
  providers; Claude explicit decline and Codex forbidden-prefix rules block execution.
- Gradle captured stdout replay: both providers receive the preview and native
  denials block the original ./gradlew test command. Replay is not an actual build.
- Codex untrusted declines for search, reading, Gradle and tests block execution.
- Missing search/read/test targets exercise native error paths. Exact local stderr
  and exit preservation are separately verified in real-execution-results.json.

Initial grep -n on one file supplied no filename: RTK did not compress that format.
The filename-qualified grep -H -n case passed. No hidden flag change was added.

Actual Gradle under the network guard failed before running tasks: the daemon
could not determine a usable local IP. That is a fixture limitation, not a passed
Gradle-agent integration test. Actual local Gradle runs and replay through agents
were checked separately. Earlier Codex short-yield fixture returned a running tool
handle before command completion; the final replay used a longer native wait.
Initial logs remain available and are not counted as successful executions.

## Reused, with matching inputs

95 native build inputs rehashed; established ELF payloads reused without changes.
Final APK assets and 629 production inputs match the staged build. The preceding
build's broad Java tests are historical context, not new passes for this version.
No X11 graphics or unrelated environment installation was performed.

## Final APK

- Package com.termux; version2.1.6-test2/code1523; ARM64 only; non-debuggable.
- Bytes: 40644282.
- SHA-256: `b07ba5120266f0b39a3f8a6647badb50a220978a17ca37b8868428b5fe18552b`.
- Same release certificate: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP, v2 signature, DEX/runtime, packaged source/assets and native ABI checked.
- Package/signer/code permit updating test1; actual installed migration not tested.
- Old APKs, stable update metadata, reserve1499 and published artifacts unchanged.

## Not checked

Phone UI/update, opening settings to refresh an existing installed bridge on the
phone, application restart, and real-model behavior remain untested in this build.
Do not infer them from CLI fixtures. Read-only/workspace-write successful execution
remains unconfirmed due the previously recorded bubblewrap fixture mount failure.
Other sandbox/mode/version combinations are not covered universally.
Cargo/Go/CTest/Vitest checks cover filter output samples; real projects for these
runners and their individual native-agent policy combinations remain untested.

## Manual use

After installing test2 yourself, open Limits and context → RTK Mode once to refresh
the installed bridge. This does not reset the toggle/statistics or restart the chat.
In a test project try grep -H -n, cat, a Gradle task and pytest, then inspect the
full-output path and compare with RTK disabled. Keep using ordinary execution for
commands outside the documented matrix.
