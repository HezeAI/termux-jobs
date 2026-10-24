# File transfer recovery audit — 2026-10-24

Private2.0.12-test/code1341 follows the
[direct-child transport audit](AUDIT-FILE-TRANSPORT-2026-10-24.md), retaining all
previous private fixes. Public stable remains v2.0.2/code1331. Evidence lives in
`.verification/file-recovery-audit-20261024/`. Changes are confined to ProjectFiles,
ProjectFileTransfer, the browser failure message and new ProjectFileReceipt, plus
version metadata/documentation. Installed environments, accounts, active terminal
sessions, the user's server and Telegram bot were not changed or contacted.

## Findings and changes

**A received progress count is not the actual completed count after connection
loss.** The previous failure message counted occurrences of DONE and presented
them as completed objects. A remote/guest command may commit an action whose
reply never reaches the app, or continue after the local transport is lost.
The isolated fixture confirms this ambiguity without using a user's server.

Each batch now gets a fresh validated32-character hexadecimal ID. The unchanged
copy/move stages emit ordered per-item acknowledgements and a final completion
record for this invocation. ProjectFileReceipt bounds the response to8KiB, counts
only complete ordered lines for its own ID, and requires the matching final
record. Fragmented input is supported; old DONE output, foreign IDs, malformed,
repeated, out-of-order and truncated own records cannot confirm success. The
maximum50-item batch fits this limit. Unrelated bounded diagnostic lines are
ignored. No user filename/content is embedded in the receipt protocol.

ProjectFileTransfer.execute also requires its runner to return successfully;
ProjectFiles supplies the existing transport with its zero-exit check. Receiving
a final line before a transport failure therefore does not silently become a
success. All failure paths expose the acknowledged prefix as a lower bound, with
a Russian/English explanation that more actions may have completed or may still
be executing. Read-only directory refresh remains; the mutating command is not
automatically replayed. No new retry, queue, persistent journal or window is added.

**An unresponsive SSH peer could leave a long batch waiting on the long overall
deadline.** ProjectFiles now adds ServerAliveInterval=15 and ServerAliveCountMax=2
for its independent SSH file commands. These protocol keepalives preserve strict
host-key checking, credential handling and the existing90-second query/six-hour
batch deadlines. They do not reconnect, stop remote work, change the user's agent
SSH tunnel or affect Telegram-bot connections. Actual detection timing depends
on the SSH implementation/network; an exact wall-clock guarantee is not claimed.

## Evidence

The first scoped run passed60 tests. Final source passed **94 scoped JVM tests
in11 classes**, no failures/errors/skips. The18 receipt/recovery tests include:

- Fragmented bytes, zero received progress, every truncation point of progress/
  completion, old/foreign IDs, duplicate/reordered/early/following own records,
  invalid IDs/ordinals/counts, bounded noise/overflow and a50-item response.
- Real private Bash Copy and POSIX-sh Move with the final receipt and zero exit.
- A committed Copy/Move with all replies discarded: acknowledged0 while the
  destination exists, and Move has removed its source. The runner executes once.
- Two committed items with only the first acknowledgement delivered: lower
  bound1, despite both destinations existing.
- A complete receipt followed by an injected transport exception: no false
  success/replay, even with all item acknowledgements present.
- A private command paused before its second Copy. Its consumer receives the
  first acknowledgement and loses the result channel; the test confirms the
  command is still alive, then releases it and observes the second destination.
  This proves the possibility of continued work, not real SSH recovery.
- A failed second Copy delivered through ProjectFileTransport: acknowledgement1,
  nonzero exit, first destination retained, second absent and owned stage cleaned.

A queued Android-widget fixture additionally checks the English uncertainty
message, acknowledged count, no automatic requeue, busy release and rejection of
an old confirmation. Existing source/transfer/transport/export/editor/selection
and server-choice tests remain included; SSH command tests check the two new
options, existing key/host protections and absence of ServerAlive options on
local commands. All real file/process fixtures stay inside the repository.

No new test failures required production corrections. The final test log contains
Robolectric allocation/dispose warnings; source/build success is not a claim of
physical-device allocation behavior or a clean warning-free run. These are scoped
results, not a rerun of the earlier full app/module suites.

Verified APK: `build/apk/Termux-Jobs-v2.0.12-test-arm64.apk`, 45,846,106 bytes;
SHA-256 `ad1aa3dd1907adacf665f44ffcf49c4881aa43e1942d3187cb1432c667ac5d5a`.
ZIP integrity, com.termux/code1341/name2.0.12-test/ARM64, signing continuity,
required runtime types, tested-source/APK-stage consistency and bounded secret-
pattern checks passed. All assets/native libraries and ProjectFileTransport
match1340. There are no required missing runtime classes;31 reviewed optional
types remain. The release build succeeded with the existing custom-lint/Kotlin
metadata warnings; this is not a clean-lint claim. These are source/build results,
not an APK installation, live network recovery or release publication.

## Review and logical inspection

Traced confirmation consumption → batch ID/script → mutation → progress → final
record → transport exit → success, and failures before/during/after progress.
Only exact ordered own records count. A missing final LF/record or failed runner
cannot reach browser success, and no failure path invokes the runner again.
Subsequent directory listing reads paths only. Existing preflight/no-overwrite
operations, quoting, staging paths, profile snapshots and UI choice invalidation
are retained. Receipt IDs distinguish accidental stale/replayed output; they are
not an authentication secret or a defence against a malicious trusted SSH peer.

ProjectFileTransport and all native/assets/process-stop logic match1340. No raw
PID/process-group kill, new helper/package dependency, exported component, provider
root, certificate, cleartext policy or file permission is introduced. Protocol
records contain no credentials, file paths or file data. The8KiB collector and
fixed50-item selection bound limit response memory independently of output shape.
The SSH keepalive options are confined to ProjectFiles.command's SSH branch;
strict known-hosts and password/key choices are preserved.

## Limits and next checks

This is **uncertain-outcome handling and liveness detection**, not guaranteed
remote cancellation or exactly-once filesystem execution. Local SSH/proot exit
still does not prove every guest/remote/detached descendant stopped. Completed
work is not rolled back; temporary staging may remain when a shell cannot run its
cleanup trap. A receipt is a process notification, not fsync/power-loss durability
or proof that no other actor changed a file afterward. Abrupt app death loses
in-memory receipts; no durable job-status/resume record is introduced.

The continued-work fixture uses a private process and deliberately lost delivery,
not TCP, sshd, a live server account, every proot shell or Android's Process API.
Command-option assertions do not prove an actual SSH keepalive deadline. GNU
copy/move fixtures do not establish every guest tool version. No physical phone
permission/UI, connection-loss recovery or installed upgrade was claimed tested.

Minimum phone checks: a small Copy and Move in a disposable directory; verify the
normal success message. With a disposable SSH account/folder only, interrupt a
batch connection and verify the acknowledged/uncertain message, then inspect
both folders and wait for or explicitly stop remaining work before a manual retry.
Do not use a live development project as a destructive failure fixture. A stronger
remote guardian/job protocol needs a separate design and proof before promising
whole-tree cancellation; broad PID/name-based cleanup is intentionally absent.
Next audit stage: cumulative source/module regression checks and review of the
remaining explicit limitations before release planning.

Compatibility EN: same com.termux/signing identity/schema8; no reset, account
migration or environment replacement. Valid existing data remains usable. Avoid
updating during a running file batch; live-operation migration is not established.

Совместимость RU: тот же com.termux, подпись и схема8; сброс данных, перенос
аккаунтов и замена окружений не требуются. Существующие данные остаются валидными.
Не обновляйте приложение во время файловой операции: её перенос между версиями
не подтверждён.
