# File manager confirmation/export audit — 2026-10-24

Private2.0.10-test/code1339 extends the preceding
[installer audit](AUDIT-INSTALLERS-2026-10-23.md), retaining all earlier fixes.
Public stable remains v2.0.2/code1331. Evidence is in
`.verification/file-manager-audit-20261023/` (work began before the local date
changed). All file/process fixtures stay inside the repository; installed
rootfs, accounts, user files and active sessions were not altered.

## Findings and changes

**Old transfer dialogs could enqueue another operation.** A dismissed/replaced
Copy/Move menu or confirmation retained callbacks with captured source entries.
After a completed action, invoking the same confirmation again could reuse them.
Changing the selection did not invalidate its earlier menu.

The browser now binds both confirmation stages and their preflight result to a
single choice generation. Dismissal, replacement, selection/navigation change,
acceptance and browser close invalidate or consume that choice. A delayed result
cannot reopen a confirmation for a stale choice. Accepted work retains the
captured entries, destination and Copy/Move mode and can enqueue only once.
The package-private TransferBackend seam delegates to the same ProjectFiles
methods in production and supplies a no-I/O fixture backend in tests. Layout,
selection/filter behavior and the underlying transfer shell script are unchanged.

**Download naming could loop on a dangling link.** The previous filename helper
used File.exists(), which followed a dangling symbolic link and reported its
name free. createNewFile() then refused that occupied name, so retry picked the
same name again. A separate empty-file creation followed by FileOutputStream
also left a destination substitution window before opening with truncation.

ProjectFileExport now creates the chosen destination with NIO CREATE_NEW/WRITE/
NOFOLLOW_LINKS in one open. Files, directories and links are collisions, including
dangling links; names retain the existing -2/-3 extension convention. A collision
race retries without truncating the competing entry, with a finite attempt bound.
The old free-name helper is removed. The copy enforces a byte limit and checks
browser close/interruption around chunk reads. Ordinary read/limit/cancel failure
closes and attempts to remove only the newly created partial output. Closing the
browser is visible to the worker through a volatile flag. This is cooperative
export cancellation, not a new rollback/stop mechanism for running shell batches.

**Export cache cleanup missed some early/error exits.** The private temporary
export and its unique directory now have a cleanup boundary for download failure,
completion and closure. A successful Share intentionally retains its private cache
file for the recipient's later read-only content URI access. A queued Share that
finds the browser closed, or a missing chooser application, releases that copy.
No user source file is deleted by Download or Share.

## Evidence

Prior behavior with the fixture backend failed six of13 cases: five obsolete/
replayed choice scenarios and the dangling-name lookup. The first test attempt
could not subclass final ProjectFiles; the corrected fixture uses the explicit
TransferBackend seam. Pending-dialog teardown also wrapped failed assertions in
Robolectric's queued-looper warning; callbacks/close are now drained explicitly.
These fixture corrections are separate from the reproduced queue/name failures.

Final code passed **48 scoped JVM tests in7 classes**, with no failures/errors/
skips. APK `build/apk/Termux-Jobs-v2.0.10-test-arm64.apk`:45,839,590bytes,
SHA-256 `49d5a62dd0fdacfc2adf94c3d303e82fa5cce91dde213f3aa88b8a3ea4fedf1c`.
ZIP integrity, package/version/ARM64, signing continuity, required runtime types,
tested/staged source consistency and bounded secret-pattern checks passed. All
assets/native libraries match1338. Only the browser and new export helper differ
in production; no manifest, installed environment or transport script changed.
The build succeeded with the existing custom-lint/Kotlin metadata warnings; clean
lint is not claimed. See VALIDATION.md for limits and historical evidence.

The tests cover:

- Dismissed/replaced menus and confirmations, cleared selection, replay after
  completion, captured Move destination/entries, and close during preflight.
  A controlled executor/backend runs no installed shell, SSH or provider process.
- Real temporary-file exports: existing file/directory/dangling/live links,
  concurrent same-name creation, read failure, byte limit, cancellation during
  read, invalid names, pre-cancel and preserved worker interruption.
- Existing file selection/filter/search/all-selection/limit behavior and source
  environment routing. Android widget models do not prove physical gestures.
- Real private shell fixtures: quoting, files/directories/link preservation,
  recursive/overlapping selections, name conflicts, extension retention and
  preflight before a batch. Added injected destination-race and cp-failure cases
  preserve existing data and remove the owned staging directory.
- Existing bounded UTF-8 editor checks, revision conflict detection and quoted
  same-directory saves. No installed project file was opened or edited.

## Review and logical inspection

Traced selection → operation menu → preflight → confirmation → accepted job →
result/refresh, including asynchronous dismissal/replacement and close. Old dialog
onDismiss callbacks cannot invalidate a newer dialog: cleanup checks identity and
generation. Choice state is owned by the main thread. Worker cancellation reads
only the volatile close flag and the worker's interrupt status.

Traced cache export → exclusive public destination open → bounded copy → close/
cleanup → UI completion, and the separate Share cache handoff. Export errors clean
private staging without removing the user source or a pre-existing destination.
Share keeps its existing read-only URI/ClipData grants and nonexported provider.
Package/signer/schema/environment identities and SSH host-key/auth settings are
not changed. No new process/PID signaling is introduced by this patch.

## Limits and next checks

This stage establishes confirmation ownership and cooperative Download copying;
it is not a complete transaction/cancellation rewrite for ProjectFiles transports.
Running Copy/Move batches remain noncancelable through the browser as before;
already completed actions persist on failure. Forced Activity/process shutdown,
SSH disconnection and termination of every local/remote descendant were not
established here. Existing direct-child transport shutdown needs its own focused
review before claiming whole-tree cancellation. No such guarantee is made.

The unchanged shell fixtures use host GNU tools, not every guest BusyBox/coreutils
version. Android storage permission denial, FUSE behavior, physical gestures,
external Share recipient timing and installed upgrades remain device checks.
Public Download storage can be modified/deleted by other authorized actors;
exclusive leaf creation is not a lock over its parent or protection against an
actor replacing an entry later. Failure cleanup is best effort, not crash/power-
loss durability. Successful shares remain subject to ordinary Android cache eviction.

Minimum phone check: Download the same small file twice, verify both complete
copies/renaming, cancel a confirmation and make a fresh selection, and Share a
small file to an external app. For a larger Download, closing the browser should
cancel its unfinished Java copy; an already completed file can remain. No user
data reset or environment migration is required.

Future stable compatibility wording:
- EN: No workspace/account/environment data reset or migration. Existing Download
  names remain intact; new exports use numbered names. Canceled/obsolete file
  confirmations must be reopened for the current selection.
- RU: Сброс данных и миграция рабочих областей, аккаунтов и окружений не нужны.
  Существующие имена в Download сохраняются, новые копии получают номер.
  После отмены или смены выделения подтверждение операции нужно открыть заново.

Developer: HezeAI. AI development assistance: OpenAI Codex
