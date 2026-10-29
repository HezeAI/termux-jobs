# Private test1 follow-up: compact image actions and visible running steps

VersionName `2.0.10-test1`, versionCode `1431`. Builds on the private
[1430 rollback/action UI](VALIDATION-test1-live-actions.md).

## Changed behavior

Image and file-change action dialogs use wrap-content height around their existing
thumbnail/cards, status and caption. Long file lists and expanded diffs scroll
within a capped viewport. They have no Back button; Android Back and tapping outside
close the dialog. Ordinary image attachment viewers retain their compact media
bounds and Save to Download action, with their redundant Close button removed.
Command/output action dialogs retain a bounded scrollable height and their Back
button. No image loading, file access or native process behavior changed.

An active timeline opens once when it first receives a running action. Explicit
manual collapse is respected on subsequent output/status updates. Historical
completed cards still start collapsed. Action keys and the real native running,
completed and failed statuses remain unchanged.

## Diagnosis

A read-only snapshot of the active Codex bridge confirmed a real `inProgress`
command row during execution. The UI initially hid step rows inside a collapsed
card. The new default exposes those rows. The existing local polling interval
remains 500 ms while busy and 2 s while idle: very short commands may complete
between polls. No artificial running phase is shown for an already finished
command. No working session was restarted or patched for this check.

## Verification

Targeted JVM tests cover image-dialog wrap-content sizing, removal of Back,
outside-touch dismissal, unchanged scrolling for command dialogs, automatic
expansion and manual-collapse retention, live output/status changes across four
providers, media sizing/zoom and attachment safety. Results are recorded in
`.verification/live-actions/tests-1431.log`; final build, lint, runtime and APK
checks are in `.verification/compact-1431/`.

All four adapters now attach a bounded latest-output line from real native
output. Running timeline entries display that line; long command/preview text is
ellipsized to keep the status and latest line visible. The original full command
and output remain available in details. Native shell output redirected to a file
is not automatically followed or read by the UI. A direct five-line streaming
probe confirmed output growth before completion on the active Codex bridge.

Finishing a manual Full output page for a running command resumes live following
when the reader is at the bottom; unfinished pagination, reading upward and text
selection retain their existing protections. Completed actions retain manual
full-output browsing.

Baseline bridge/native tests from build1430 are supplemented with tail projection
and native event regressions for this follow-up. Real-device visual checks remain required for the
new compact dialog and running-card presentation.

## Compatibility

Same `com.termux` package, signing identity, schema8, ARM64 ABI and Android
requirements. No data reset. X11, renderer/native libraries and model/VM absence
retain the rollback baseline. This is a private test build; no publication or APK
installation is performed by this task.

Automated final result: **121 JVM tests passed**, no failures/errors/skips.
Six targeted bridge/projection scripts passed. Android lint passed with
**0 errors and 223 warnings**, retaining the previously documented warnings.

Final release build and APK verification passed: versionCode1431,
versionName2.0.10-test1, ARM64, original package/signing certificate, ZIP integrity,
required DEX types and packaged assets. Native libraries, resources, SDK
requirements and permissions match the rollback baseline. No secret-pattern
findings were reported in packaged assets/DEX; experimental VM/Local AI/Wayland
payloads remain absent. See artifact.json and runtime.json for exact evidence.

SHA-256: `dcd6f57f7f398c85decd090f96ebdb12d29a9923d07cf463e98502d045b2f303`.
