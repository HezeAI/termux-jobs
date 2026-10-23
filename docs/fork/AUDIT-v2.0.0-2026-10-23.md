# v2.0 audit — application stability and X11 renderers

Date: 2026-10-23. Developer: HezeAI. Audit assistance: Codex.

Follow-up: the findings below describe the original v2.0 snapshot.
Implementation changes are tracked in [v2.0.1](RELEASE-v2.0.1.md); device
verification remains separate.

This is an audit report, not a release validation or a statement that the
findings below have been fixed. No new APK was installed or published, and no
installed environment, desktop or agent session was started/stopped for testing.

## Baseline and reproducibility

- Stable source: `a60e0356`, incorporating the v2.0 release implementation at
  `2dbb4c5b`; versionName `2.0.0`, versionCode `1320`.
- Audited recovery artifact: `Termux-Jobs-v2.0.0-recovery.2-arm64.apk`, code
  `1324`. Its 76 assets/native-library entries are byte-identical to the stable
  release APK.
- An isolated stable `git archive HEAD` snapshot was used for Java/source
  verification: `.verification/audit-v2.0/source`.
- Python fixtures and application/X11 assets were compared with the stable
  snapshot before using the shorter working-tree path for the final Python run.
  Only generated `__pycache__` bytecode differed. Node test/helper inputs were
  also compared with the stable snapshot.
- Evidence lives in `.verification/audit-v2.0/`. Toolchains, temporary files and
  test outputs stayed inside the repository. The snapshot build harness used
  the existing repository-local SDK and a shorter repository-local temp path.

## Findings

### A1 — GPU copies ignore occluding windows

Priority: high for the next graphics fix. Status: developer-observed GUI defect,
with an independently reproduced native copy-region defect. Not fixed here.

The developer reports that selecting a terminal window does not bring its
contents visually above the GPU test when using Termux / Wrapper / imported
Turnip. Screenshot `Screenshot_20261023_153004.jpg` shows the test image
overlapping the selected terminal.

The pinned, unmodified Termux:X11 source contains a matching defect:

- Commit `fa3a8b430e2896a19f44c99a9cb056254615ae06`.
- `lorie/src/main/cpp/patches/xserver.patch`, around lines 340–372, calls
  `lorieTryScheduleGpuCopy` with the client update before intersecting it with
  `window->clipList`. The later intersection only adjusts damage notification;
  the copy queue already contains its rectangles.
- `lorie/src/main/cpp/lorie/InitOutput.c:911` copies the supplied rectangles
  directly. With no update region it queues the entire source pixmap.
- `lorie/src/main/cpp/lorie/renderer.cpp:772` consumes those queued rectangles
  without access to the window's current visible region.
- Our `x11-session.sh:62` intentionally disables the compositor for Termux
  Wrapper. In that configuration, GPU copies can write directly into the root
  pixmap rather than a compositor-owned per-window backing pixmap.

An isolated C probe compiled the actual pinned `lorieTryScheduleGpuCopy`
function with stubbed Android/X11 dependencies. A 100×100 client update whose
right half is occluded still queues `[0,0,100,100]`, rather than the visible
`[0,0,50,100]`. Evidence: `native-present-probe.c` and
`native-present-probe.json`. This verifies the incorrect queued region; it is
not an on-device execution of the native renderer or a performance measurement.

The defect is in the shared native core, not a rule that always keeps the GPU
window above other windows. Device confirmation currently covers Wrapper only;
other renderer paths may use different buffer types and must not be assumed
either affected or immune.

Minimal next check: in a separate test build, compare Wrapper with only native
GPU Present-copy offload disabled (`-disable-gpu-present`). This leaves GPU
rendering of the application enabled, but can increase frame-copy CPU cost.
Then verify focus/overlap, move/resize, minimize/restore, desktop switching and
frame pacing. A native fix must apply correct clipping/coordinate transforms
before publication and review deferred-copy ordering against window changes.
Blindly enabling the compositor globally is not an established fix and could
regress the previously observed GPU black-screen paths.

[Pinned upstream source](https://github.com/termux/termux-x11/tree/fa3a8b430e2896a19f44c99a9cb056254615ae06).

### A2 — Backup merge silently conflates distinct presets

Priority: high for backup correctness. Status: reproduced against stable Java.

`WorkspaceBackupImport.java:58` deduplicates presets using only saved state,
name, environment, proot alias/isolation, first startup command and directory.
It omits agent identity, model, permissions, Ubuntu selection, further startup
commands, SSH endpoint and other meaningful settings.

Two presets sharing the compared fields can therefore collapse into one even
when one is OpenCode and the other Gemini. The incoming preset ID is changed to
the existing ID, and `persisted()` checks only ID presence, reporting success
although the incoming configuration was not imported.

Audit-only Robolectric reproduction:
`AuditV20ReproTest.distinctAgentPresetsAreCollapsedAndReportedPersisted`.
An explicitly same-named pair is required; the normal Duplicate action adds a
name suffix, so that action by itself is not the trigger.

Suggested correction: define semantic preset equality covering persistent
configuration, or keep distinct incoming presets by default. Verification of
the saved result must confirm content, not merely ID presence. Do not merge
different agent/SSH/access configurations silently.

### A3 — Preset deduplication detaches imported history

Priority: high for restore correctness. Status: reproduced against stable Java.

`WorkspaceBackupImport.prepare()` copies history to the newly generated preset
ID before merge resolution. `apply()` can then replace that ID with an existing
preset ID without moving/merging the copied history. The restored file remains
on disk under an ID no preset references; `persisted()` still reports success.

Reproduction:
`AuditV20ReproTest.dedupLeavesImportedHistoryAtAnUnreferencedNewId` verifies the
actual copy, ID substitution, orphaned file and missing destination history.

Suggested correction: resolve final preset identities before copying history,
and define how existing histories are preserved or explicitly combined. Include
partial-copy failure and cleanup handling without deleting existing history.

### A4 — Renderer preparation accepts stale readiness markers

Priority: medium. Status: Turnip false-positive reproduced; VirGL discrepancy
confirmed by source inspection. This is a recovery/readiness inconsistency,
not evidence that corrupt libraries bypass launch validation.

`WorkspaceDesktopGpu.java:22` checks only whether `ready.json` is a
file. An empty `{}` marker with no driver libraries counts as installed, so the
preparation UI skips installation/repair. The Python `x11-gpu.py.ready()`
validator correctly rejects the same state. Crucially,
`x11-desktop.sh:144` invokes that validator at launch and fails closed. A stale
marker can thus cause repeated failed launches rather than automatic repair.

Reproduction:
`AuditV20ReproTest.emptyTurnipMarkerCountsAsInstalledWithoutAnyDriverLibrary`,
plus `turnip-readiness-probe.json` for the Python result.

VirGL has a related mismatch: `VirglInstaller.installed()` checks its marker,
host executable and ANGLE EGL library, whereas `x11-virgl-setup.sh` additionally
checks ANGLE GLES, host packages/options and selected-guest Mesa capabilities.
Launch runs the stronger check and rejects missing components, but preparation
can skip their repair based on the weaker result. No installed host files were
removed or altered to simulate that case.

Suggested correction: use the authoritative, selected-target check before
deciding preparation can be skipped, then offer the existing consented repair
path on missing components. Preserve fail-closed launch validation.

### A5 — Verification scripts depend on path/layout state

Priority: low, developer workflow. Status: reproduced, workaround isolated.

Several Node fixtures assume versioned `.verification` parent directories
already exist. Deep checkout paths also exceed Unix socket path limits in
Java/Node fixtures. Two Python assertions depend on checkout text/length:
a symlink size threshold and a substring rejection of `source` in PulseAudio
arguments that also contain a filesystem path. These fail in the nested audit
snapshot despite the successful equivalent shorter-path run.

The Gemini frame fixture additionally requires a particular installed CLI bundle
read-only. Tests should create their own parent directories, use short private
socket/temp paths, inspect arguments structurally, and declare installed-tool
prerequisites. Initial harness failures were not counted as product regressions.

## X11 matrix review

All eight supported targets were checked in the existing executable fixtures
for selected-rootfs routing, mandatory-component absence, renderer environment
cleanup and owned-process shutdown. These are isolated fixtures, not fresh
distribution installs or graphical tests on eight real systems during this audit.

| Target | Display | CPU compositor | Turnip compositor | VirGL ANGLE backend | Wrapper |
| --- | --- | --- | --- | --- | --- |
| Termux | 97 | On | On | Vulkan | Supported; compositor off |
| Ubuntu | 98 | On | Off | GL | Unavailable |
| Debian | 100 | On | Off | Vulkan | Unavailable |
| Alpine | 101 | On | Off | GL | Unavailable |
| Fedora | 102 | On | Off | Vulkan | Unavailable |
| openSUSE | 103 | On | Off | GL | Unavailable |
| Void | 104 | On | Off | Vulkan | Unavailable |
| Arch Linux ARM | 105 | On | Off | Vulkan | Unavailable |

VirGL compositor is off for every target. The legacy hidden X11-test Ubuntu
slot uses display 99; its compatibility entry is not a second active default
desktop. Ubuntu AI is not a supported desktop target.

Reviewed invariants:

- Each target has a fixed private service/process, display, control directory
  and VirGL socket. Profile-supplied process names/ports are not accepted.
- GPU selection is per environment. Entry into VirGL clears stale Zink/Vulkan
  variables; Turnip selects its target-specific pinned ICD. Wrapper is Termux
  only and validates imported archives and runtime contents.
- Base installers request the explicit required component sets. Missing desktop
  binaries, audio or certificates must fail readiness; optional application
  fixes remain selected-application/selected-rootfs scoped.
- Guardian tests cover EOF, cancellation, session close and child/server
  failure while preserving an unrelated process/desktop. Source review did
  not identify a new confirmed cross-session shutdown defect.
- X11 uses an authorization cookie and private components. The adapter reserves
  a non-listening loopback discovery port to block the upstream wildcard
  discovery listener. No public proxy or network service was opened by this audit.
- Imported driver libraries are executable native code with application-level
  privileges. PRoot distributions are not a security boundary against those
  privileges, irrespective of distribution-specific library directories.

## General application checks

Review covered preset persistence/import, chat lifecycle/history/input,
environment installation/migration, file transfer/cleanup, server operations,
updates and X11 service lifecycle. Existing tests exercise archive traversal,
symlink refusal, partial failures, cancellation and selected-environment paths.
The backup defects above demonstrate why passing older tests alone was not
treated as sufficient evidence.

Atomic library persistence remains in place. Server SSH paths retain strict
host-key checking and disabled agent forwarding. The updater restricts download
hosts to its HTTPS allowlist and checks artifact identity/integrity. Imported
presets retain their review-before-launch flag. No production configuration,
global shell/hosts setting or authorization content was changed/read out.

## Verification results

- Stable Java verification: **862 app + 154 terminal-emulator tests passed**;
  no failures/errors. Other requested Java modules have no test sources.
  Evidence: `java-baseline.log`, `java-baseline-results.json`.
- Full Python discovery: **544 tests, zero failures, one skipped**.
  Evidence: `python-working-tree.log`, `python-input-comparison.json`.
  The nested-snapshot run had the two path-dependent assertion failures
  described in A5; this is recorded in `python-tests.log`.
- **21 Node suites passed**, including native CLI loopback-provider tests for
  streaming/history/permissions and all four agents' chat protocol fixtures.
  Evidence: `node-baseline/results.json` and suite logs. Synthetic providers
  do not prove actual paid-account/cloud authentication or network reliability.
- **Three additional audit-only Java characterizations passed**, reproducing
  A2/A3/A4 rather than asserting fixed behavior. Evidence:
  `reproductions-java.json`, `reproductions-java.log`, and the isolated stage's
  `AuditV20ReproTest.java`. The native clipping probe for A1 also passed.
- Localization: **584 workspace mappings**, plus app/shared keys and format
  arguments. Shell syntax: **19 scripts**, no errors.
- No fresh APK compilation was necessary for this documentation/audit task.
  Source verification compiled/tested the isolated stable Java stage; archive
  verification inspected the already-built recovery APK separately.

Recovery artifact verification:

- Package `com.termux`; version `2.0.0-recovery.2`; code `1324`; ARM64.
- Size `45794032` bytes; ZIP CRC and unique entries passed.
- SHA-256 `3e24dd0c099054fa2053ddec3709f14bef411a408054ad901ca8f9e3df519f07`.
- Valid APK v2 signature. Certificate SHA-256
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`,
  matching stable v2.0. Version code is greater than stable 1320.
- Runtime inventory: 13552 defined classes; required definitions present;
  31 already-reviewed optional unresolved types, no new unreviewed types.
- Limited private-key/token-signature and sensitive-filename scan: no matches.
  This does not establish that every possible arbitrary secret is detectable.
- Artifact identity checks do not independently prove Android installation,
  data migration or every device/API-level runtime path.

## Remaining device verification

The new overlap defect prevents claiming Wrapper graphics are fully correct.
First verify a narrowly scoped mitigation/fix against the same overlapping GPU
and terminal windows. Then compare CPU, Turnip, VirGL and Wrapper where supported,
including background/foreground, keyboard open/close and stop/restart.

Fresh-phone package installation, touch/IME/DPI, Mali/Dimensity compatibility,
long sessions and real Claude/Vertex account workflows were not newly verified
by these isolated tests. Historical RedMagic 10 Pro / Samsung S26 Ultra reports
remain attributed to the developer and do not imply every matrix cell was tested
on both devices. No exhaustive security or hardware-stability guarantee follows
from this audit.
