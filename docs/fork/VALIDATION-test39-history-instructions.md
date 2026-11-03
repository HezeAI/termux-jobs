# test39 — Claude history and project-instruction creation

Date:2026-11-03. Final APK: `/sdcard/Download/Termux-Jobs-test39.apk`.

| Property | Verified value |
| --- | --- |
| Package | `com.termux` |
| Version | `2.1.1-test39` / **1474** |
| Bytes | **40,444,892** |
| SHA-256 | `88e8b4b3c380fa3718902ed55be45bad31a26032da591f92252307cc81a8764e` |
| Certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| ABI / debug / signature | ARM64 ELF64 AArch64 / nondebuggable / v2 verified |

Sidecar: `/sdcard/Download/Termux-Jobs-test39.apk.sha256`. VersionCode exceeds
all inspected issued/reserve APKs in Download; previous maximum1473. Test38's
voice-only APK remains unchanged; test39 contains the subsequent fixes below.

## Findings: Codex versus Claude

Codex already requests native `thread/turns/list` pages and marks snapshots
partial. Its page cursor, common Android merge/cache/position logic and full-text
viewer are the comparison baseline. The Codex adapter source remains unchanged.

Claude's previous snapshot selected20message groups (not20complete user/model
turns), kept only the final200rows, and shared60000characters among previews.
Those limits did not themselves delete native history. Further read-side horizons
were the last64MiB,8000lines and500chain messages; a2MiB live cache and a200000char
text helper further restricted bodies. Its snapshot lacked partialHistory, allowing
the common UI to prune old loaded messages absent from the live window. A dense
message lost its earlier rows without a continuation. chat-detail depended on the
same clipped/evicted map, so it could not always recover complete older answers.

The new read-only JSONL format adapter follows main parent links, including
same-file logical compaction parents, in64KiB reverse-read blocks. A page normally
contains at most20visible records or2MiB, with one larger record allowed up to32MiB.
That is a per-record safety bound, not a whole-history preload. Row cursors preserve
all omitted parts of a dense message; previews stay bounded to200rows and a350000byte
row payload/60000character body budget. Stable native record UUIDs distinguish
records sharing a provider message ID. Explicit replacement removes provisional
stream rows. partialHistory retains older groups; partialMessages retains earlier
parts of a group split across snapshots. Historical tool rows are not marked
running merely because the current request is busy. Full details reread the relevant
native record when no longer cached and return24000character chunks.

The Android earlier-history control, pending-page retry/queue, keyed merge,
scroll anchoring, bounded presentation cache and full-text viewer remain shared.
A failed detail response now retains the current text and offers retry. No model
request, new polling timer or native history write was added.

## Findings: creation-window lifecycle

The previous project-creation handler returned silently if a read was in progress.
The button stayed reachable, so an early tap disappeared. There was also no tracked
child dialog to coalesce repeated opens. An isolated pre-fix test reproduced the
lost early tap; the failed log is retained. This establishes that code defect,
not the exact visual mechanism of every reported phone flash.

One request is now queued for the captured agent/environment/project. After the
read it opens one form or focuses the existing base file. Repeated taps preserve
that form and its inputs. A confirmed missing-file revision can open directly
without a second parent-editor rebuild/read. Failures show the existing sanitized
reason, reset pending state and allow retry. Draft confirmation, exact-path review,
CAS/atomic save, import/history behavior and no implicit LEARNING.md creation remain.
Both settings and session entry use the same editor. No artificial delay was added.

## Checked anew during this work

-181distinct shared JVM/Robolectric cases passed across the final relevant runs:
 51chat UI,17view/anchor,2cache,65instruction UI,5instruction core,4session resources,
 6manager layout,29navigation and2keyboard. These include Claude/Codex queued paging,
 retained older and within-message rows, provisional replacement, cached reopening,
 nonbottom anchor/offset under prepend, streaming update and clear/repopulate at
 150percent scale. Creation covers both agents and entries, early/repeat taps,
 missing/existing files, legacy environment, failed/delayed read and retry,
 unsaved drafts/form fields, preview, conflicts and saving.
- On version1474,10additional executions passed: instruction core5,ClaudeCode4 and
 ClaudeCodeUi1, including actual packaged-asset preparation. The17view cases were
 subsequently rerun without changing packaged production inputs. This is186distinct
 JVM cases total across work/build; the core5 overlap, so execution counts are not
 mislabelled as distinct tests.
- Node history fixtures: more than8000records and500visible messages,680tools and
 680results,520parts in one response, an exact long Unicode body exceeding the old
 200000char cap, cursor replay/reset, append/incomplete tail, helper recreation,
 repeated provider IDs, same-file compaction linkage and unchanged read checksum.
 A separate23-record file exceeds64MiB (over69MiB); the reader returns it one large
 record per page without loading all bodies together. Only isolated generated
 fixture files are appended/replaced for fault injection.
- Real pinned Claude Code2.1.296 ran against an isolated loopback provider with
 synthetic credentials. Native initialize/stream/tools/image/permission, saved
 continuation/history and owned-process checks passed. A read-only comparison of
 its JSONL found52records,14main-chain messages,
 15projected rows and no missing native message; SHA-256 remained
 `6f50719decf2858cc7d479991d329fe1dab8102a05a2b2207fdb324d6c13e546`. This is native-format evidence, not a phone UI check or cloud-model test.
- Codex pagination regression passed with250current and1200historical items,
 cursor expiry/recovery, bounded windows and no frontier advance from polling.
 Codex chat/wire/attachment regressions also passed. Claude unit/lifecycle/active
 input, delivery and completion-time checks passed. A scoped Claude attachment
 fixture with current lifecycle ACK/negative-receipt semantics passed.
- Full modern Lint7.4.2 for the changed shared Java/resources:0errors,234warnings,
 all five modules, no infrastructure diagnostics. `git diff --check` passed.
- Actual final APK: ZIP, package/version growth, same signer/v2, ABI/debug, source/
 build/Download byte equality,118assets equal source/pinned cache, all3native
 payloads ELF64 AArch64, 14186DEX classes with requiredMissing and
 unreviewedMissingTypes empty. Speech vectors/permissions/queries and diagnostic
 strings remain, and claude-history.cjs plus its Java preparation reference exist.

## Initial failures and corrections

The new pre-fix creation test failed because the early tap never opened a form.
The first corrected retry test then asserted before Android's queued button callback;
explicit main-looper draining corrected that test, with no product delay. The full
65-case editor run subsequently passed. An early history fixture exposed cached
native rows being reinserted outside the anchored page; only unpersisted live rows
now overlay the disk page, and the full pagination test passes without duplicates.

The unchanged shared all-agent attachment fixture has stale ACK/throw-only
expectations: its first failure reproduced against the signed test38 Claude asset.
An attempted current-contract run also reached an unrelated Gemini8-versus4 card
expectation. No Gemini production behavior or original fixture was changed for this
work. The affected Claude path was checked in a task-local fixture with actual
ACK/refusal contracts, plus real native image input; Codex attachments passed
separately. These distinctions are preserved rather than claiming that legacy
all-agent fixture passed unchanged.

## Reused with input proof

Successful shared checks were not rerun solely for packaging. Source/test/resource
identities match the verified stage; final scripted helper edits were verified
separately. For test39, all448Java/resource/manifest Lint inputs and transformed
Gradle configuration match after version normalization. CJS is not parsed by Lint;
its checks are the Node runs above. Local SDK/JDK/Gradle/dependency caches are reused.
Native/X11 source inventory95files matches prior evidence; all3payloads equal
signed test38, with the two Termux JNI files restored into the isolated stage.
This is packaging reuse, not a new native compilation or graphics run. Unchanged
agent/environment/driver checks retain their prior limits; no guest was installed.

## Not checked / phone acceptance

Actual phone scroll/touch/IME, history reopening and creation-window flicker/opening
remain to be checked after installation. On an idle Claude session, explicitly
restart with the saved conversation to load the new adapter; there is no automatic
restart or native-history migration. Check previous messages beyond the old window,
full long response, prepend position, poll while responding, switch/reopen/resume,
then both creation entries for missing/existing files and a draft. Records above
32MiB or unresolved/cross-file parent links are not reconstructed by inventing
history; damaged/missing native data is not repaired or overwritten.

The prior bounded presentation cache remains; memory eviction is not native deletion,
and earlier pages remain available through the shared control. This does not claim
unlimited simultaneous Android views or persistence of an evicted off-screen view.

Test38 voice diagnostics/system-window alternative and test37 Send alignment stay.
Actual microphone/provider success remains unconfirmed, error5cause unknown. Stable
voice publication remains held until the developer's requested phone check and a
later release authorization. No publication or working-session restart was performed.

Evidence: `.verification/claude-history/`, `.verification/instruction-create-window/`
and `.verification/history-instructions-test39/`: plans, original failures, fixture/
native/Node and JVM results, input/reuse proofs, Lint and exact artifact audits.

## Subsequent video finding

The developer later supplied Screenrecorder-20261103-095607.mp4. Codex already
showed /root/termux-fork/AGENTS.md and its existing-file status after pressing
Create; Claude showed a missing CLAUDE.md and opened the creation form. The reported
Codex no-form case is the existing-file branch, not a demonstrated immediate child
closure. A subsequent change distinguishes Edit from Create and avoids adding a
status row merely to focus an already-open file. Test39's original APK is unchanged.
