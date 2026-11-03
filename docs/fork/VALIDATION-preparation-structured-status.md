# Preparation status without raw process logs

Date: 2026-11-03. Local, unpublished remediation. No APK was produced or replaced.
This is separate from the security investigation of published v2.1.2 at
`c0073b462311bba5fd2dcc301b048d69e407f3f1`. It does not retrospectively fix that APK.

## Scope and consumers

The previous `AgentPreparationOperation.Job.output` accepted merged preparation
stdout/stderr. Its bounded 65,536-character `log` supplied expandable selectable UI
text and `EnvironmentInstallProgress.failureLine`. An arbitrary last line also
became `item`, visible outside the expanded log. `saveLog` wrote the last report
per environment/agent under `.agent-preparation/operations/` every two seconds
and at completion. This was bounded last-attempt retention, not unlimited history.

Code search found no application reader, exporter or uploader for those operation
files. The preparation window only consumed the live Job, not its saved file.
The generic `EnvironmentInstallLogUi` Download exporter belongs to another flow.
The existing setup-directory guest bind includes the operations directory, making
old files locally accessible to agents able to read it. This is not an automatic
network upload; an agent/tool could read and subsequently transmit file content.
Old files and the bind remain unchanged in this remediation.

The inventory check `AgentPreparation.query` previously retained up to 32 KiB of
output and used a cleaned tail as `Plan.reason`, displayed by `summary`. Removing
only `saveLog` would leave this and the live UI exposure intact.

## Change

- Remove Job.log, last-log timer, saveLog, arbitrary item text and failureLine use.
- Remove expandable preparation details; no new operation log or temp log is written.
- Store fixed stage/component identifiers, bounded measured image progress,
  numeric exit codes and fixed error identifiers with safe RU/EN descriptions.
- Preserve the last known stage on failure. Unknown exceptions never contribute
  their message, class details, command, URL, response or traceback to the UI.
- Stream inventory into finite READY/MISSING component sets, completion/auth flags
  and exit code. Remove raw report buffering and Plan.reason.
- Share a bounded stream parser: 2,048-character read buffer, at most 256 characters
  in a candidate record. Discard oversized records through their delimiter and
  ignore incomplete EOF records. Continue draining merged stdout/stderr to EOF.
- Preserve the process wait/ownership logic, serialized queue, deduplication,
  independent component/account readiness and active-operation reattachment.
  A failed window offers explicit Retry check; closing it does not stop packages.

Parsing necessarily uses temporary process/read buffers. This change prevents
retention in UI/operation state and operation files; it does not promise secure
zeroization of all copies in the JVM, kernel, producer or CLI memory. Delimited
allowlisted strings may be emitted by any child; this protocol does not prove
producer authenticity. Installation success still requires exit/final inventory.

Unchanged: four-agent selection, recipes/versions, downloads/install commands,
existing rootfs, credentials, old operation logs, generic environment installer
logs, chat history, ordinary terminal and native CLI logging. No old file cleanup,
new telemetry/export, process cancellation or automatic session restart is added.

## Diagnostic trade-off

The preparation dialog no longer provides APT/dpkg details, maintainer-script
output, traceback, arbitrary CLI/version errors, HTTP response bodies/headers,
URLs or a transcript for copy/export. Unknown failures show a safe description,
the last known stage and a numeric exit code when available. Exact HTTP statuses
are not invented from a generic failure code. Future richer diagnostics would
need a separately designed typed, bounded protocol, not a raw-output fallback.

## Verification

Evidence directory: `.verification/preparation-structured-status/`.

Checked anew:

- 16 targeted JVM/Robolectric tests passed with no failures or skips
  (`:app:testDebugUnitTest`, internal debug test variant) for inventory, preparation lifecycle, structured
  output, fixed errors, stale-log preservation, window closing/reattachment,
  retry availability, queue/deduplication, measured progress and four-agent keys.
- `scripts/tests/test_agent_preparation_output.py`: real task-owned shell child
  and pipe; production prepare, stream reader and Job. Synthetic Context/Handler,
  paths, checker and fake proot executable; no Ubuntu/package/CLI/model invocation.
  Seven scenarios: multiline stdout, split/incomplete stderr, 2.4 MB output drained,
  unknown exit 42, known download error, child exit 130, then successful retry.
  No fixture-secret marker remained in Job string fields; no new operation file;
  the pre-existing sentinel log stayed byte-for-byte unchanged.
- One-character reader boundaries, oversized/no-newline input, unfinished records
  and read failure are covered separately. Inventory only retains permitted IDs.
- Java/resources compilation, RU/EN localization checker and diff/whitespace review.
- Modern Lint 7.4.2 (`lintRelease`) passed: all five modules, zero errors,
  234 warnings; the compile-SDK/AGP compatibility warning remains. Analysis
  stages match final production source bytes. This is not release APK validation.

Reused: existing local SDK/JDK/Gradle and dependency caches. Test/analysis results
are executed anew; cached compiler inputs are not device evidence.

Not checked: real Ubuntu/package installation (recipes and command construction
are unchanged), Android phone UI/process behavior and real cancellation by signal.
Exit 130 is a synthetic interrupted-child result, not an app stop mechanism.
The initial check's cancel suppresses its callback; package preparation remains
non-cancellable by closing its window. No working process was signalled.
No new APK, release APK functional claim or GitHub CI run is part of this patch.

## Remaining audit scope

The v2.1.2 artificial-data reproduction showed private-key body fragments surviving
linewise redaction, reaching Job state/file, and truncated-window redaction losing
its opening delimiter in a separate generic installer flow. No real secret was
used. A stock unmodified recipe printing a real multiline key was not established.
This patch removes new retention in agent preparation only. It does not remediate
old files, generic EnvironmentSetup logs or all application diagnostic channels.

The published APK remains SHA-256
`2f8b97b2d9083a5b7dcc3966eca54ea6d843fec3e253ba6e279c123921eeddab`.
Public release metadata/assets, Components, tag and update channel were not edited.
