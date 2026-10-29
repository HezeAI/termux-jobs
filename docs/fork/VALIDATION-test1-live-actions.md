# Private test1 rollback: live agent actions

Private developer test build, not a published release. Based on commit
`3696ea43` (the pre-Local-AI, pre-virtual-machine test1 rollback).
VersionName `2.0.10-test1`, versionCode `1430`.

## Behavior

- Expanded timeline actions have individual thin outlines and pressed states.
  Running actions open immediately, with the real command/output and native
  status updated in the same dialog. Unknown or interrupted states are not
  presented as successful completion.
- The reader can select text or scroll upward without being forced back to the
  bottom. Long plain tool output can follow a bounded latest-output page; the
  Full output button loads the original native output in bounded pages.
- Image actions use the image path or saved attachment supplied by that action.
  Their thumbnail opens the existing image viewer. No unrelated latest image is
  substituted.
- Web metadata describes queries, page titles/domains and text searches. Links
  use the existing copy/browser confirmation. The UI does not fetch pages or
  generate screenshots.
- File changes show native paths, operation types and expandable colored diffs,
  including line counts when available. Unknown metadata retains the original
  tool text. Plain Files labels become File operations.
- All new labels support Russian and English. Commands, paths and tool output
  remain in their original language.

The shared action UI supports Codex, Claude, Gemini and OpenCode. Streaming
availability depends on their native events: a tool that supplies output only
when finished cannot show earlier output. This change does not create a
second agent request or rerun an action.

## Automated evidence

Evidence is stored locally under `.verification/live-actions/`:

- `node-tests.json` and `node/`: isolated native bridge/projection tests,
  attachment integration, pagination, session isolation and recovery.
- `node-final.json`: native Claude, Gemini and OpenCode checks repeated after
  the final output projection changes.
- `test_agent_action_preview.cjs`: bounded previews, URL validation, exact image
  paths, file diffs, missing metadata and snapshot preview budgets.
- `test_action_live_events.cjs`: real bridge event transitions from start through
  output and completion/failure, stable action keys, foreign-thread isolation.
- `tests-final.log` and JVM XML: live action dialog updates across all four
  providers, stable timeline entries, selection, reader position, image callbacks,
  bilingual labels, file diff expansion and native detail pagination.
- `lint.log` and `.verification/modern-lint-report/`: independent Android lint.
- `build.log`, `artifact.json`, `runtime.json`: isolated ARM64 release build and
  final APK verification. Consult actual generated evidence for results.

Automated result: **114 JVM tests passed**, no failures/errors/skips. All 19
recorded bridge test scripts passed, including the metadata test. The additional
live-event regression passed; three native adapters passed again after the final
projection adjustments. Android lint passed with **0 errors and 223 warnings**.
The three new warnings concern a programmatic-only view constructor and the
read-only touch observer used to pause output following; ScrollView retains its
native touch handling. They are not evidence of a device accessibility audit.

## Review and compatibility

Projection helpers do not execute commands, fetch URLs or enumerate files.
Image access uses the existing per-session attachment loader. Detail requests
remain scoped to the selected native session; stale callbacks are rejected on
session changes, dismissal and closing. Native permission and process-stop paths
are unchanged. Preview size and paged output are bounded.

Package `com.termux`, signing identity and workspace schema8 are retained.
There is no automatic data reset. X11/native libraries and desktop settings are
unchanged from the rollback APK. Local AI and VM implementations are absent;
previously created experimental disk files are not automatically removed.
An existing agent process needs a normal restart with its saved chat to load
updated bridge files. Installation does not restart the current agent for you.

## Device check still required

Automated UI tests do not establish real phone behavior. On the private APK,
open a running shell action, watch output and completion without reopening the
window, scroll upward, then inspect one image action and one file diff. Check
one other agent and English labels. No APK installation or public publication
is part of this validation run.

## Final artifact

The isolated release build passed. ZIP integrity, `com.termux`, ARM64 ABI,
versionCode1430, versionName2.0.10-test1 and the original signing certificate
were verified. All source assets match the packaged assets; only the four chat
adapters and the new presentation helper differ from the rollback asset set.
Resources, Android permissions/SDK requirements and the three native libraries
match the original rollback APK. No VM, Local AI or Wayland experiment payloads
were found; the packaged asset/DEX secret-pattern scan reported no findings.
The DEX inventory reports no missing required or unreviewed types; its optional
API inventory is not a real-device compatibility test.

SHA-256: `0c8aece790f956350b333462c134299b3b3ee16b9cf999e73285e84a87d77487`.
