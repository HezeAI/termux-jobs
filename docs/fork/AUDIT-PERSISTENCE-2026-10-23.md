# Workspace persistence audit — 2026-10-23

Private2.0.7-test/code1336 includes the preceding
[agent-chat](AUDIT-AGENT-CHATS-2026-10-23.md),
[environment/X11](AUDIT-ENVIRONMENTS-X11-2026-10-23.md),
[storage](AUDIT-STORAGE-2026-10-23.md) and
[transfer/attachment](AUDIT-TRANSFERS-2026-10-23.md) fixes.
Public stable remains v2.0.2/code1331. This audit uses source and repository-private
fixtures. No installed library, account, environment or running session was changed.

## Findings and changes

**Activity recreation could overlap independent AtomicFile writers.** Every
WorkspaceStore previously owned an independent executor. Close allowed its queued
writes to finish, but a new screen could read the prior on-disk snapshot and start
another writer for the same file. AtomicFile alone does not serialize them, so the
old screen could overwrite newer edits or interfere with backup recovery.

WorkspacePersistence now shares one FIFO writer per canonical workspace path.
Immutable pending bytes are registered before disk work starts. A new store reads
the latest pending snapshot rather than opening AtomicFile during an active write.
When no bytes are pending, loading retains AtomicFile's existing .bak recovery.
Disk writes and callbacks run outside the coordinator registry lock. An owner can
close without blocking the UI; queued writes continue. The writer/registry entry
is retired after the last owner and pending snapshot are gone.

**Late work could crash or call a destroyed screen.** Save after close previously
submitted to a shut-down executor; a queued success callback also ran after close.
Closed stores now reject saves/recent-command or mode mutations and suppress their
UI callbacks. A write failure notifies current live owners, including a replacement
screen when the originating one has closed. Closed owners are removed; callbacks
check close again on main-thread delivery. No permanently running writer/cache was
added for a closed, drained workspace.

**Duplicate IDs were accepted as valid library records.** Commands, sessions and
notes each now require unique normalized IDs within their own collection on both
load and encode. Validation completes before replacing loaded lists. Duplicate-ID
input remains intact/read-only, and an invalid outgoing snapshot cannot overwrite
a valid committed library. IDs are not silently reassigned, records are not
removed and user data is not reset. Valid schema8 serialization is unchanged.

## Evidence

The corrected prior-code reproductions covered overlapping writes across a close/
reopen, late save after close, a completion already queued at close and duplicate
IDs in all three collections. Six of seven original new tests failed for those
reasons; the seventh confirmed that existing AtomicFile partial-write rollback and
retry already worked. Initial constructor injection only supplies a controlled
AtomicFile inside a test fixture; normal production storage paths are unchanged.

The first scoped fixed-code run passed **77 tests** across persistence, store,
backup/import, recent history, package history, Gemini mode and chat cache. A final
follow-up added routing a failed old write to the current owner. Final production
code passed the **entire938-test application suite in149 classes**, with no failed
or skipped tests. Its new persistence class contains10 tests, including the live
owner failure route. This full run includes the preceding audits' tests; previous
results are not relabeled or added to inflate this count.

| Area | Performed check | Limit |
| --- | --- | --- |
| Screen overlap | Real AtomicFile/temp files; first write held on a latch, second store opened/edited, overlap detected before fix, newest snapshot preserved after fix | JVM lifecycle model, not a physical Android rotation |
| Queued writes and release | Close while write pending; double close, final owner release, pending write completes, registry entry disappears | Executor/file fixtures; not OS killing the process |
| Failed writes | Synthetic partial write rolls back to committed bytes, explicit retry succeeds; replacement screen receives failure while closed one does not | Simulated IOException, not physical fsync/power failure |
| Duplicate records | Load of duplicate commands/sessions/notes is read-only and retains original bytes; invalid encode cannot replace a committed file | Synthetic malformed libraries; no developer file inspected |
| Compatibility | Existing schema/version migration, saved flags, command/note/recent history, connection fields, agent modes/runs, restore review and backup import | JVM tests; installed-upgrade and provider login not exercised |
| Package history | Per-root identity, bounded/deduplicated operation records, changed-version reconciliation, symlink/key refusal and own shell receipt fixtures | Repository-private files and shell children, no package manager used |
| Chat cache | Bounded presentation entries, ownership transfer, recent-row trimming and preserved native history boundary | JVM/source inspection; no new native provider session |

## Review and logical inspection

Traced construction/load, pending/committed snapshots, enqueue order, close and
reopen, write failure/rollback, error routing, callback delivery and idle writer
retirement. State transitions and owner registration use one registry lock; all
writers for a path share the same executor. Pending bytes clear only when the
completed write still owns the newest snapshot. Old close/dismissal cannot retire
a state with a new owner or pending write. No wait for disk writes was added to
Activity close, and no process/session stop mechanism changed.

All current WorkspaceStore callers run in the main app process (controller and
backup validation). X11's separate processes do not instantiate this repository.
The coordinator is not a multi-process lock/database, does not merge independently
edited old snapshots and does not make uncommitted bytes durable. SIGKILL/power
loss can discard queued changes; recovery is to the last committed AtomicFile.
The existing public mutable lists remain a main-thread usage contract, not a new
thread-safe collection API. No promise of crash-proof persistence is made.

Credentials retain their existing device/profile-bound encrypted serialization.
Pending bytes are process-private; errors remain generic and do not log library
contents or exception paths. Native agent histories/authorization files, shell
startup/history hooks, server-profile persistence and SharedPreferences formats
were not rewritten. Presentation caches remain separate from authoritative history.

## Compatibility

Package/signer, ARM64/Android8+, schema8 and file locations are unchanged; private
versionCode rises from1335 to1336. Valid libraries require no reset, migration or
environment reinstall. Malformed duplicate-ID files now refuse loading/writing
instead of being treated as valid; their original bytes are retained. This change
must be mentioned in both localized updater highlights if included in a stable
release. The public update manifest remains unchanged in this private audit.

English compatibility wording: Valid libraries keep their data without a reset.
Libraries with duplicate record IDs are retained read-only; no automatic deletion
or ID rewrite is performed.

Russian compatibility wording: Корректные библиотеки сохраняют данные без сброса.
Библиотеки с повторяющимися ID записей остаются в режиме только чтения; исходный
файл сохраняется, автоматического удаления или изменения ID нет.

## Build evidence

Verified private APK: [Termux-Jobs-v2.0.7-test-arm64.apk](../../build/apk/Termux-Jobs-v2.0.7-test-arm64.apk),
45,832,954bytes, com.termux, version2.0.7-test/code1336, ARM64, not debuggable.
SHA-256: `c85d63636459e5983e98b647949e87e9ae262110ac4004a3ded64c78b3920fe4`.
Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
The signer matches1335 and versionCode is higher. ZIP/signature/runtime checks
passed. All packaged assets and native libraries match1335 byte for byte; staged
production sources match the tested hashes. Required runtime types resolve;
31 optional/guarded references retain existing review notes. Bounded private-file
and secret scanning found no matches. These checks do not establish actual
installed-upgrade or GUI behavior on a phone.
Builds use a repository-local SDK and unchanged native libraries. ZIP, identity,
version/ABI, signer, runtime references, production-source/asset consistency and
bounded secret/private-file scans are checked separately from phone behavior.
Existing lint/Kotlin metadata tool warnings do not prove a clean full-lint result.

Evidence: `.verification/persistence-audit-20261023/` contains prior/fixed JVM XML,
full test log/results, source hashes and APK/build/runtime metadata.

## Remaining phone checks

Create a disposable note and preset, edit them and reopen their screens; restart
the app normally and confirm contents and recent-command history. Confirm existing
presets and saved agent chats still open. Actual rotation, Activity/process death,
physical disk-full/power failure and an installed update were not exercised here.
Queued changes seen in memory are not proof of completed disk commit. No installed
library was inspected or modified as part of the source audit.

AI development assistance: OpenAI Codex
