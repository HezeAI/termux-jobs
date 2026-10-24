# Backup, restore and storage cleanup audit — 2026-10-23

Private2.0.5-test/code1334, including the preceding
[agent-chat](AUDIT-AGENT-CHATS-2026-10-23.md) and
[environment/X11](AUDIT-ENVIRONMENTS-X11-2026-10-23.md) fixes.
Public stable remains v2.0.2/code1331. This is an isolated source/build audit,
not authorization to publish, install an APK or mutate the developer's data.

## Findings and fixes

**The shared storage-operation lock could expire before the worker exited.**
On an output/parse failure the Java engine sent termination, waited at most eight
seconds and released its global BUSY flag even if Python was still alive. That
could admit another backup, restore or cleanup while the old worker was waiting
for its owned native/archive child. Cleanup now waits for actual process exit,
including through Java thread interruption, before removing the private request
folder and releasing the lock. An unresponsive owned process keeps new storage
operations blocked; elapsed time is not evidence of successful shutdown.

**Errors were delivered before cleanup.** The retryable terminal error callback
was queued inside catch, before the finally block had finished the old process.
Both success and failure are now queued after cleanup/unlock. The package-private
process-starter seam permits lifecycle tests without starting installed Termux
executables; production continues to require and launch its normal Python.

**Repeated termination could interrupt Python's own child cleanup.** Cancel and
finally could both call destroy on the same Process. A single atomic termination
request now guards those paths, including cancellation racing process creation.
There is no foreign PID search, process-group inference, new SIGKILL escalation,
APT lock removal or forced shutdown of another session. Python's existing owned
child handling remains unchanged.

**A late Cancel could silently drop a completed result.** Success delivery used a
live cancelled flag and could disappear between completion and main-thread
dispatch. Delivery now captures one terminal outcome. A tap after the worker has
completed cannot suppress its queued result or the restore UI's follow-up import.
This is not transactional rollback or a promise that cancellation reverses writes.

**Cancellation dismissed the progress UI immediately.** Cancellable operations
now disable the Cancel button, show Cancelling / Stopping operation and wait for
the terminal callback before dismissing. Pending progress cannot overwrite that
status. Back/outside taps do not dismiss these worker progress windows. Native
environment removal retains its existing non-cancellable behavior after consent.
Scan forms may close while their cancelled worker retains the shared lock.

## Scope and evidence

| Area | Checks | Limits |
| --- | --- | --- |
| Archive handling | SHA-256/fingerprint changes, compression corruption, duplicate/absolute/traversal entries, symlink/hardlink escape, existing destination refusal, separate restore staging | Synthetic archives and temporary files; no full user backup restored |
| Environment restore/removal | Source alias retained, fresh jobs-* aliases, architecture checks, provenance/inode matching, native lock refusal, native remove failure with no unsafe fallback | Fake proot command/metadata fixtures; installed rootfs untouched |
| Selection and snapshots | Disjoint HOME/PREFIX/rootfs sets, current SQLite WAL, selected scripts treated as data, inaccessible/unselected files, changed selected files, cache invalidation and no cached archive bytes | Private fixture trees; no private keys or account contents inspected |
| Cleanup | Explicit acknowledgment and selection, changed/stale plans, no-follow deletion, external targets and unselected files retained, native/APT busy locks, partial cancellation, large disk-backed inventory and sparse40GiB file | Temporary files inside this repository; no actual40GiB allocation or user-data removal |
| Python children | Real temporary fake compression/native/APT executables, owned-child cleanup on failure/cancel and waiting for a synthetic APT transaction | No package installation or actual proot command |
| New Java lifecycle | Delayed exit beyond eight seconds, parse failure, late Cancel after queued success, repeated Cancel and interrupted cleanup wait, exactly one terminal callback | Controlled Process objects; Android environment function shadowed in JVM |
| Progress UI | Cancel stays visible/disabled, late events ignored, outside tap refused, completion can dismiss | Robolectric; actual phone gestures/rotation/process death unverified |

Python baseline: **113 tests passed** across backup38, cleanup29,
compression16 and selection30. Source Python workers were not changed.

The first attempted Java reproductions failed before reaching their child process
because an Android hidden-API helper is unavailable in the host JVM. Those results
are retained as fixture failures and are not evidence of the product regression.
After shadowing only the Android environment builder, all four lifecycle tests
failed on the prior engine for the intended reasons: callback too early,
eight-second unlock, dropped success and three termination requests instead of one.
The fixed engine and related backup/consent/progress tests passed: **35 tests in
nine classes**.

The completed broad application run covered **917 tests:916 passed, one failed**.
The failure was the pre-existing DesktopSessionBar frame-scheduling fixture:
automatic Robolectric drawing could leave a pending frame before its manual
callback observation (`next == -1`). Its explicitly rendered test now uses a
paused looper; no application animation code changed. The final follow-up covered
that complete class plus all storage/backup classes: **37 tests passed**. Thus all
917 unique application tests have passing evidence across the runs, rather than a
claim that one unchanged full run passed. An earlier interrupted full launch has
no completed test result and is not counted. Unrelated suites were not rerun again
after the test-only looper adjustment.

## Review and logical inspection

Traced create/inspect/restore/scan/cleanup/removal, pre-launch cancellation,
output failure, process exit, private-directory cleanup, shared lock release,
main-thread result dispatch, late cancellation and UI dismissal. Backup extraction
and deletion trust boundaries are unchanged. Imported presets retain the preceding
audit's runtime/auth reset behavior. Existing data, accounts, terminal sessions,
package managers and native desktop processes were not stopped or modified.

The worker receives private JSON input and script copies, launches without a shell
and suppresses stderr from UI reports. Existing uppercase-only error-code filtering
is preserved. No account authorization is copied to another system. No archive
format/schema migration or automatic cleanup of user data was added.

Compatibility: com.termux, signer, ARM64/Android8+ and schema8 remain unchanged;
private code1334 supersedes1333. No data reset or environment reinstall is required
by these source changes. This does not prove all upgrade/runtime behavior on a
phone; installed-app and fresh-device checks remain separate.

## Build verification

Verified APK: [Termux-Jobs-v2.0.5-test-arm64.apk](../../build/apk/Termux-Jobs-v2.0.5-test-arm64.apk),
45,830,966 bytes, com.termux, code1334, version2.0.5-test, ARM64, not debuggable.
SHA-256: `38df559c325d9a536d6e1e0cabcf31e0a0551a5709676c6fd758c1e86f7815dd`.
Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Signer matches1333 and versionCode is higher. ZIP, signature and runtime-reference
checks passed; all native libraries and packaged assets match1333 byte for byte.
Bounded secret/private-file scanning found no matches. Required runtime
references are resolved;31 optional/guarded references retain existing review
notes. Build logs retain pre-existing custom-lint/Kotlin metadata warnings;
this is not a clean full-lint claim.

Evidence is in `.verification/storage-audit-20261023/`: Python results/logs,
corrected before/after lifecycle tests, broad/follow-up XML results, production
source hashes, build log, APK runtime report and artifact metadata.

The isolated build uses
a repository-local SDK and reuses unchanged native libraries. APK verification
checks ZIP integrity, com.termux/version/ARM64 identity, signer continuity,
increased versionCode, runtime references, matching packaged assets/native
libraries and bounded secret/private-file patterns. These are build checks,
not installed-app GUI or migration tests.

## Remaining checks

On the phone, use a small disposable backup: create it, cancel a second attempt,
wait for cancellation to finish, then retry and inspect the completed archive.
Restore only that disposable backup into its separate destination; verify its
files/preset. For cleanup select only a disposable test folder and verify an
unselected sentinel survives. Native environment removal, rotation/background
activity recreation and Android killing the app mid-operation were not exercised
on the installed app. Terminal callbacks are suppressed for a destroyed Activity;
this change does not add durable result/import delivery across Activity or process
death. Reinspect the separate restore destination rather than assuming its UI
follow-up completed. A hung owned process intentionally blocks subsequent
storage operations until it exits; the UI does not claim rollback.

AI development assistance: OpenAI Codex
