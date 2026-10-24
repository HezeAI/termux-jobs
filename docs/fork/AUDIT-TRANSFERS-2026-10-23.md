# Server transfer and chat attachment audit — 2026-10-23

Private2.0.6-test/code1335 includes the preceding
[storage](AUDIT-STORAGE-2026-10-23.md),
[environment/X11](AUDIT-ENVIRONMENTS-X11-2026-10-23.md) and
[agent-chat](AUDIT-AGENT-CHATS-2026-10-23.md) fixes.
Public stable remains v2.0.2/code1331. No server deployment, account login,
installed-app mutation or release publication is part of this audit.

## Findings and changes

**A raced-in directory could absorb a completed download.** Local publication
checked that a filename was absent, then used mv -n without -T. If a directory
appeared between those steps, mv placed the temporary file inside it. The original
temporary path disappeared, so publication reported success for the wrong path.
A directory symlink could similarly redirect that move. Publication now uses
mv -T -n and retains the postcondition that the original temporary file is gone.
It refuses the raced-in destination and never treats it as a target directory.
This is a no-overwrite/directory-interpretation fix, not a directory-descriptor
sandbox against every hostile same-account ancestor-directory replacement.

**Old confirmations could queue work after their selection was invalidated.**
Download confirmations had no selection-generation check at accepted start. Move
confirmation callbacks also lacked closed/running/disconnected admission checks,
allowing submission to a shut-down executor after controller close. Upload
confirmation referenced mutable current profile/documents, so an older window
could start a later profile with the previous selection. Reconnection could
reactivate an old confirmation.

Upload/download/move choices now have a generation checked before queueing.
Replacement, dismissal, disconnection and accepted start invalidate that choice.
A dismiss callback only invalidates its own generation, so closing an older
window cannot cancel a fresh selection. Closed/running/disconnected checks remain
at final admission. Fresh choices after reconnect still work. Connection settings
are copied into per-operation snapshots, and selected move paths are copied before
confirmation. No account migration, automatic replay or fallback SSH identity is
introduced.

## Performed checks

| Area | Evidence | Limit |
| --- | --- | --- |
| New publication race | Real local shell, temporary files and an mv wrapper introducing a directory or directory symlink between precheck and rename; failed publication retains temporary data and leaves the raced target empty | All fixture paths remain inside the repository; no actual phone download destination used |
| New confirmation guards | Actual Java controllers/UI; queued-work recorder replaces the executor before any job can run; old/new profile, disconnect/reconnect, dismiss and closed-controller cases | No SSH or user file read; native Android touch ordering not exercised |
| Connection snapshot | Original profile edited after snapshot; SSH identity/settings remain fixed, mutable UI collections not copied | Synthetic values only; no credential decryption needed |
| SFTP | Window bounds/out-of-order replies, short read, cancellation, remote metadata changes, collision/self-move refusal, upload temporary/rename/cleanup protocol fixtures | Fake peer; no live server, bandwidth measurement or actual network-loss test |
| Download/local file handling | Batch path validation and overlap/link refusal, selected destination, binary bytes, existing file/link preservation, real copy/move shell fixtures | No installed rootfs or shared-storage permissions modified |
| Server controller/queues | Foreground/monitor/helper isolation, bounded queue cancellation, background progress/card visibility, selection, diagnostics privacy and shutdown admission | JVM fixtures; Android background process retention not proven |
| Server helper | RPC/path validation, managed task/deletion rules, schedules and search |49 Python tests with temporary files and local fixture subprocesses, not the developer's server |
| Attachments | Codex and shared Claude/Gemini/OpenCode manifests, image/video/log/APK routes, reference/session scope, traversal/link/foreign-thread refusal, deduplication and native local socket admission | Three Node fixture suites plus JVM tests; no provider inference/login or phone codec/SAF test |

Python server suites: **49 passed** (agent20, delete8, search6, schedules15).
Node attachment suites: **three passed**. No attachment adapter/runtime asset was
changed in this phase.

After correcting test harness setup, the prior production code failed all seven
new reproductions: wrong directory publication, four stale upload/download
confirmation cases, stale move selection and move callback after close. Earlier
harness bring-up failures (an unavailable Robolectric helper and an undrained
paused dialog callback) are retained separately and are not counted as product
reproductions. The first scoped run after the fixes passed141 tests. Final
production code, including explicit confirmation-dismissal guards, passed
**143 JVM tests in19 classes**, including the added directory-symlink and
connection-snapshot checks. Full917-test application evidence belongs to the
preceding storage audit; it was not rerun or relabeled as a fresh full run here.

## Review and logical inspection

Traced chooser → permission callback → confirmation → worker admission, active
background transfer, stop/disconnect, completion, new choice and controller close.
Confirmed that owner identity remains stable while a job runs and only a fresh
choice can start after reconnect. A recorded old click cannot submit to a closed
executor. The native upload-conflict dialog remains independent of the initial
confirmation generation.

Pinned host keys, disabled agent/connection forwarding, separate owned SSH
transports, password-broker handling and typed/bounded diagnostic records are
unchanged. Connection snapshots copy encrypted credential references rather than
plaintext secrets. No SSH keys, authorization files or bot/project data were read,
modified or copied to another system. Existing completed files remain retained;
partial remote temporary cleanup and uncertain publication warnings are unchanged.
No automatic network-failure replay was added, because publication may already
have completed when its response is lost.

Source hashes confirm all earlier production Java/assets remain unchanged except
LocalDownloadFile, ServerProfiles and the two transfer controllers. Schema8,
server-profile format, archive format, com.termux and account storage remain
unchanged. Private versionCode increases from1334 to1335. No data reset or
environment reinstall is required by these source changes. This is not a live
upgrade/migration test on a phone.

## Artifact and build checks

Verified private APK: [Termux-Jobs-v2.0.6-test-arm64.apk](../../build/apk/Termux-Jobs-v2.0.6-test-arm64.apk),
45,831,354bytes, com.termux, version2.0.6-test/code1335, ARM64, not debuggable.
SHA-256: `e6dffe1d11be005f6f32f8da81a5de444ada865dced5dfe587a0e0e01d31699c`.
Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
ZIP/signature/runtime-reference checks passed. Signer matches1334 and versionCode
is higher. Native libraries and all packaged assets match1334 byte for byte;
tested/staged production source hashes agree. Required runtime references resolve;
31 optional/guarded references retain existing review notes. Bounded private-file
and secret scanning found no matches. These are artifact/source checks, not proof
of installed-app migration or GUI behavior on a phone.

The isolated build uses a
repository-local SDK and unchanged native libraries. Verification covers archive
integrity, version/package/ABI, signer continuity, runtime references, source/asset
consistency and bounded secret/private-file patterns. Existing lint/Kotlin
metadata tool warnings do not constitute a clean full-lint result.

Evidence directory: `.verification/transfers-audit-20261023/` contains Python/Node
logs and results, corrected before/after JVM XML, source hashes, build/runtime
reports and final artifact metadata.

## Remaining phone/network checks

Use disposable files and a scratch server directory: upload/download a small file,
background and reopen progress, stop a transfer, then make a fresh selection and
retry. Verify destination contents if interruption coincides with publication.
Try an attachment from phone storage and a chosen environment, preview it and send
it with text. Live SSH loss/recovery, provider inference, Android file permissions,
media codecs, Activity/process death and a hostile replacement of an ancestor
directory during an operation were not exercised in this audit. Transfers still
require the app process to stay alive; there is no new persistent resume service.

AI development assistance: OpenAI Codex
