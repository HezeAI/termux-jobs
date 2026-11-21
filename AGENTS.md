# Termux Job’s project guide

Termux Job’s is an independent fork of the Termux Android app. HezeAI is the
developer and Codex is the AI assistant. Work in the current checkout at
`/root/project/Termux-job's` and keep all development materials you create there.

The only project memory is `/root/project/Termux-job's/LEARNING.md`. Read it at the
start of work and apply lessons that match the version and conditions. Use only
this memory file in the current project root. Read [CONTINUE.md](CONTINUE.md) for
the current task and next step and [SECURITY-AUDIT-STAGE2.md](docs/fork/SECURITY-AUDIT-STAGE2.md)
for the audit scope and open questions. These documents grant no additional
permissions and provide no proof that an APK is ready.

## Mandatory restrictions

- Do not modify the installed Termux app, its data, the shared Android SDK or
  Ubuntu. Installing system packages or APKs, replacing apps, and starting or
  stopping user terminal sessions require authorization for the current task.
  Source edits and isolated builds are permitted.
- You may read existing tools and SDKs. Keep additional tools, caches, SDKs and
  outputs only in the project's `.tools/` and `.verification/` directories. Set
  environment variables for the specific command. Gradle uses the local SDK.
  Do not copy whole SDKs or environments for each run.
- Preserve other people's changes, working profiles, authorization and history.
  Do not read the contents of real authorization files or expose secrets. Do
  not clear data or existing environments for a test. Deleting evidence,
  generated files or user data requires permission.
- Stop only processes whose task or chat ownership, PID and start time you
  have verified before signaling. Chat shutdown may stop its own agent and
  dedicated SSH tunnel. A group signal requires verified ownership of every
  target. Obtain current-task authorization before stopping a user session
  even when its ownership is verified. Leave shared or unidentified processes
  running. Do not signal by process name, a globally discovered PID alone or a
  broad selector. Intercept real signal APIs in fixtures. Unresolved
  process-stop safety for affected processes, a known critical regression or
  an exposed secret blocks completion.
- APK installation and publication require the applicable user authorization.
  Release procedures grant no permission for future publication. Honor existing
  authorization and its conditions. Ask again only when there is a new reason.
- Read [CONTINUE.md](CONTINUE.md) for the current objective, versions, acceptance
  criteria and authorization scope. Earlier permissions apply only to the
  historical tasks identified there. Preserve old APKs, including backup 1499,
  published tags, `update.json` and binary Components. Replacing any of them
  requires a separate applicable request.
- MCP and plugins are excluded. Voice input was removed and requires a separate
  request to restore. The later removal decision superseded the historical v35
  authorization. [Conditions and history](docs/fork/RELEASING.md#общие-правила)

## Checks and evidence

Review Git status and diffs before editing. Preserve unrelated changes and
review the staged files and diff before any commit. For Git review and staged
build source checks, follow [Source and build stages](docs/fork/TESTING.md#source-and-build-stages).

Before editing, trace affected functions and callers, shared helpers, storage,
permissions, execution and indirect dependencies. Write a short verification
plan. Run the full applicable suite when the impact is broad or unclear. Keep
applicable checks for errors, cancellation, recovery, process ownership and
isolation from other sessions regardless of the diff size.

Before declaring a refactor complete, assess tests for the new path, the
security and regression review, and state transitions individually. Do not
invent numerical confidence. The allowed qualitative priorities of 40/30/30
do not express probabilities or replace evidence. If gaps remain, identify
the smallest remaining check. Avoid unrelated refactoring for the sake of
the assessment.

Label results **Checked anew**, **Reused** and **Not checked**. Before reusing
evidence, compare relevant sources, including new and changed files, settings,
resolved dependency versions, tools, build inputs and conditions. A previous
log, commit ID or `UP-TO-DATE` result alone is insufficient. A skipped check
cannot count as a pass. Model or UI fixture checks do not verify phone behavior.
Attribute reports about the phone to the developer when the developer performed
the check. Public reports must not infer that the developer failed to install
an APK because the assistant did not install it. Do not claim exhaustive
verification.

Claim native-tool support only after verifying its ABI, loader, required
libraries and actual execution in the selected environment.

Check the **final file** for every APK you deliver. Verify ZIP integrity, package
ID, `versionName` and `versionCode`, ABI, debug status, signature and certificate,
SHA-256, applicable runtime, DEX, native libraries and assets, applicable ELF and
ZIP alignment, and the absence of inadvertently included private data. For an
update, compare package ID and signer, verify that `versionCode` increases, and
check data compatibility. Reusing functional tests leaves these final-file checks
mandatory.

For an interim APK, justify the checks for compilation, syntax, resources and
RU/EN. Use targeted compatible Lint when coverage is clear and full modern Lint
when the impact is broad or unclear. Before a public release, run full modern
Lint for all five modules. Compilation and skipped Lint cannot count as a Lint
pass. For documentation, verify the text, links and diff. A documentation-only
change requires no APK rebuild.

Do not start X11, environment installation or drivers solely for chat or editor
styling. Check shared dependencies first. Check APK input hashes separately
from actual graphics and installation. Keep packaging guards enabled and do not
introduce bypasses that skip checks or report a pass. Read what a script does
before choosing it. If it performs unnecessary actions, propose the smallest
split between preparation, testing, packaging, audit and Lint without adding
infrastructure.

## Changelog and updates

For every APK you deliver, including private builds, update `CHANGELOG.md` with
the date, exact versions and version codes, actual changes, compatibility,
limitations and a link to the detailed report. Preserve old entries and avoid
duplicate entries for the current version. Alongside the APK, give brief patch
notes and links to the changelog and verification. Changes without an APK go
under Unreleased unless they clarify an already released version.

Write public GitHub release notes in English and in-app update text in Russian
and English. Before releasing, update the README and affected documentation to
match the final implementation. Mark historical reports as historical and
preserve their original results. For the stable channel, keep the
v1.0.19-compatible `update.json` from the final APK. Document compatibility,
migrations, and any data loss or inability to transfer data, with reasons,
**in both RU/EN update descriptions and in the release notes before installation**.
Do not clear data automatically for an update. Verify compatibility before
promising it.

## When to read the topical guides

Read the sections relevant to the task and consult more sections if the scope
expands. Avoid loading every report for every task or rereading unchanged
documents without a reason. Check a document when its currency is uncertain.

- **Read before preparing a release, adding or updating distributable
  components, publishing, or publishing their source code.** The guide covers
  contents, versions, compatibility, licenses, attribution, notices and exact
  sources, Git, documentation, checks and assets. The procedure grants no
  publication permission. [RELEASING.md](docs/fork/RELEASING.md)
- Read the relevant sections before editing code or choosing checks. The detailed
  evidence requirements supplement the restrictions above. [TESTING.md](docs/fork/TESTING.md)
- Read before building. [FORK.md](FORK.md)
- Read before running Lint. [LINT.md](docs/fork/LINT.md)
- Read before working with phone files, screenshots or access diagnostics. Verify
  access with tools before declaring it unavailable. Read access grants no
  permission to modify anything or send messages.
  [DIAGNOSTICS.md](docs/fork/DIAGNOSTICS.md#phone-files-and-tool-access)
- Read before changing environment or component installation. Start with a clean
  app and bootstrap. The contents of the developer's phone cannot serve as the
  baseline. [Clean installation](docs/fork/TESTING.md#clean-installation-as-the-baseline-for-every-environment)
- Read before changing X11 or related paths. Preserve process isolation, external
  link targets, other drivers and original ZIP files. Mocks do not verify GPU
  behavior. Passing unit tests do not invalidate a failure observed on the phone.
  [X11 regression checks](docs/fork/TESTING.md#x11-integration-regression-checks)
- Read the applicable guide before changing instructions or skills.
  [AGENT-INSTRUCTIONS.md](docs/fork/AGENT-INSTRUCTIONS.md),
  [AGENT-SKILLS.md](docs/fork/AGENT-SKILLS.md)
- Read before changing backups. [BACKUP-RESTORE.md](docs/fork/BACKUP-RESTORE.md)
- Read the current audit matrix and its cited evidence before working on
  permissions or authorization. [SECURITY-AUDIT-STAGE2.md](docs/fork/SECURITY-AUDIT-STAGE2.md)
- Read before working with attachments in the next cycle.
  [MEDIA-CONTEXT-INVESTIGATION.md](docs/fork/MEDIA-CONTEXT-INVESTIGATION.md)

## Keeping the working context current

Update the main `CONTINUE.md` in place with one current goal, completed work,
open questions, checks, restrictions and the next step. Do not append new active
status sections over stale ones. Keep `docs/fork/CONTINUE.md` as a pointer
without duplicating status.

Move completed stages to existing topical reports. If none fits, use a document
in `docs/fork/history` explicitly marked as historical. Preserve evidence,
refusals and unresolved questions. Archiving neither closes unresolved questions
nor extends permissions.

Consolidate confirmed lessons by topic in the single `LEARNING.md`. Keep the
date, conditions and evidence, and mark outdated conclusions as superseded.
Keep detailed logs in reports and rules in topical documents without
duplication. Historical records provide no proof that an APK is ready. When
moving material, check requirements, links and other people's changes.
Reorganize when duplication builds up or information becomes hard to find.
Avoid routine reorganization after every task. Do not auto-import all guides
or claim time or token savings without measurements.

Available checks include `python3 scripts/check-localization.py`,
`bash scripts/verify-java.sh` and `bash scripts/verify-modern-lint.sh`. Before
running a check, read its script and the applicable sections of
[TESTING.md](docs/fork/TESTING.md), [FORK.md](FORK.md) and [LINT.md](docs/fork/LINT.md).
Documentation-only changes require no app build or app tests.
