# File transport lifetime audit — 2026-10-24

Private2.0.11-test/code1340 extends the
[file-manager audit](AUDIT-FILE-MANAGER-2026-10-24.md). Public stable remains
v2.0.2/code1331. All earlier private audit fixes remain included. Evidence is in
`.verification/file-transport-audit-20261024/`. Only ProjectFiles and its new
ProjectFileTransport helper differ in production from code1339, apart from the
version bump. Checks use private files/processes inside the repository; installed
environments, accounts, terminal sessions and the Telegram bot were not changed.

## Findings and changes

The old ProjectFiles process slot could be replaced by openLocalRead while a
previous operation still owned a stream. Closing that earlier stream closed the
entire ProjectFiles instance and destroyed whichever child the shared slot now
held. Its size check used the initial entry metadata rather than the bytes read;
it also had no deadline. Command cleanup destroyed a child without waiting for
exit, so returning/releasing serialization did not establish child completion.

ProjectFileTransport reserves one operation before starting its child. Commands
and upload input share that admission point; overlap returns BUSY without starting
a second process. An operation owns its Process object, pipes, deadline, stop
fallback and completion latch. EOF requires a zero exit status; read, stdin-close,
output-consumer failure and interruption all clean that operation before admission
is released. Repeated close/late timeout of an old operation cannot affect a newer
one. Stream close no longer closes the entire ProjectFiles instance.

Cancellation first requests Process.destroy. After one second the still-owned
child receives Process.destroyForcibly. Cleanup waits for direct-child exit and
closes its pipes before releasing the slot, preserving the calling thread's
interrupt flag. Owner close only requests cancellation and schedules cleanup; it
does not wait on the UI thread. InputStream.close itself waits for this cleanup.
A launch/close race retains ownership until the starter returns and its child
is cleaned. A timeout also schedules cleanup when the consumer has abandoned the
stream. No stored numeric PID, broad pkill, process-group signal, extra system
package, Python dependency or new terminal-session command is introduced.

Actual reads and skip enforce the byte bound; reset is unsupported. Empty files
and unsigned single-byte values, including zero/255, are handled correctly.
Deadlines remain90 seconds for queries/exports and six hours for batch transfers;
local upload input now has a six-hour bound. Timeouts and BUSY have Russian and
English messages on the command path. Existing source-unavailable/limit messages,
SSH strict host-key checking, known-hosts pinning, askpass authentication and
Termux/proot/SSH routing are preserved. Password copy/wipe uses a short lock and
never logs the secret. No automatic retry of write operations is introduced.

## Evidence

Final source passed **75 scoped JVM tests in9 classes**, no failures/errors/skips.
These include18 new transport tests using real short-lived host Bash children;
fault wrappers inject stdin-close/read failure while delegating stop/wait to their
own real child. They cover binary reads, actual byte limits, skip, empty/excess
sources, BUSY admission, launch failure, nonzero exit, blocked read, close during
launch, interruption, payload delivery, ignored TERM, EOF before exit, failing
output consumers and timeout cleanup without a consumer. An unrelated private
sleep process remains alive when the tested child is canceled.

The other57 tests cover current confirmation ownership, file selection, exclusive
exports, local source routing, bounded text editing, transfer shell behavior and
server transfer choice ownership. Existing GNU-shell and controlled Android models
are not every guest distribution or a physical phone. The first12-test transport
run passed. An expanded fixture initially did not compile because its Runnable
closed an InputStream with a checked exception; it was changed to a Callable.
Subsequent reviewed and final suites passed. This is not a claim of a prior-code
runtime reproduction or a rerun of the earlier full938-test app suite.

APK `build/apk/Termux-Jobs-v2.0.11-test-arm64.apk`: 45,844,094 bytes,
SHA-256 `7670562fa4ef0c084b46ed5806a5f609f0192675982703bc734829f0d490c1d2`.
ZIP integrity, com.termux/code1340/name2.0.11-test/ARM64, signing continuity,
required runtime classes, tested/staged source consistency and bounded secret-
pattern checks passed. All assets and native libraries match1339. There are no
required missing runtime types;31 reviewed optional types remain. The release
build succeeded with the existing custom-lint/Kotlin metadata warnings; this is
not a clean-lint claim. No installation, publication or live-server check is
implied by these source/build results.

## Review and logical inspection

Traced command → reservation → child attach → stdin → bounded stdout → status →
cleanup → admission release, and local upload open → reads/skip → EOF/close.
Owner cancellation before/during/after start, blocked read, scheduled timeout,
ignored TERM, failed reads/writes, repeated old close and a new operation were
reviewed. Background cancellation waits for the launch latch before cleanup;
all child signaling retains the originating Process object. The force timer is
canceled after cleanup and cannot be redirected to a later operation.

Traced browser/editor/package close and the server disconnect path: server
browser/text owners close their ProjectFiles; no user's PTY is signaled. Consumers
retain the same destinations, file limits and profile snapshots. Existing export
failure cleanup and partial-transfer warnings remain. No manifest, exported
component, provider root, cleartext policy, native library, runtime asset, account
schema or environment identity is changed in this stage.

## Limits and next checks

This stage establishes **direct-child** ownership, not a local/remote process-tree
supervisor. Killing a local SSH/proot process does not prove that every guest,
remote shell or detached descendant has stopped. A partially completed Copy/Move
batch is not rolled back; inspect both folders before retrying. Existing transfer
staging may remain if a remote/guest shell cannot execute its cleanup trap. No
whole-tree cancellation, remote idempotency or process-death durability is claimed.
A safe remote-job protocol/guardian needs separate work before that stronger claim.

These process fixtures run on the host Java/Bash runtime, not Android's Process
implementation, every proot guest or a real dropped SSH connection. Existing
source routing/quoting tests do not establish live SSH recovery. Fresh-bootstrap
file reads add no dependency beyond the already-required host Bash, but physical
storage permissions/FUSE, installed upgrade data survival and UI timing remain
phone checks. InputStream.close can wait for its child; kernel-level stalls are
not given a hard wall-clock guarantee. Abrupt app/process death is not cooperative
close and is not covered by these waits.

Minimum phone checks: preview/export a small local file, upload it over SFTP,
close the file window during a larger read, and reopen it. In a disposable remote
folder only, check an SSH disconnection during Copy/Move and inspect both paths
before a manual retry. This stage did not send commands to a user's server.

Compatibility EN: same com.termux/signing identity/schema8; no reset, account
migration or environment replacement. Existing data remains valid; live operation
recovery after installing an update is not promised.

Совместимость RU: тот же com.termux, подпись и схема8; сброс данных, перенос
аккаунтов и замена окружений не требуются. Существующие данные остаются валидными;
восстановление выполнявшейся операции после обновления не гарантируется.
