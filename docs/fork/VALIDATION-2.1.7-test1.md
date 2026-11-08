# 2.1.7-test1 /1534 — Arch residual endpoint and offline chat restart

Private test based on published 2.1.6 /1533. No application release, automatic APK
installation or working-environment deletion is authorized or performed here.

## Arch diagnosis and correction

Read-only native Termux evidence: UID10620, TracerPid0. The working Arch path
`rootfs/run/termux-jobs-wayland` is a directory, mode000, not a symlink. No matching
active process or kernel mount was observed. A read attempt returned errno13.
A separate PRoot read-only inspection found the residual directory empty; that
fake-root read is not counted as a successful native permission check.

Historical source92733c64 (2.0.5) bound the app Wayland runtime to this exact path.
Commit5a1fa804 (2.0.6) removed that launcher. A fresh separate Arch reproduces a
mode000 placeholder during the same PRoot bind; normal exit removes it. The exact
historical interruption that left the user's placeholder is not established.
The fixture then explicitly recreates the observed residual state; the unchanged
2.1.6 helper fails with the same SCAN_ACCESS_DENIED/13 and relative path.

The new deletion-only policy recognizes this precise sealed endpoint alongside
systemd/dissect-root. The plan lists both paths and explicitly excludes their
unknown contents from the size. Only kernel-confirmed empty directories can be
removed. A nonempty directory restores the container and any earlier empty
placeholder, with mode000 retained; no file payload is removed. Restoring an empty
placeholder may give that empty directory a new inode. No blanket /run exception,
new recursive chmod, external bind-source traversal or hidden access error is added.
Readable endpoints are scanned normally; symlinks remain links and are not followed.

Native app-service tests: fresh Arch install, real legacy-shaped bind, active
ENVIRONMENT_BUSY refusal, normal unbind, residual-state reproduction, cancel,
remove, external runtime and neighbouring-container preservation. The actual
application service ran the native helper in a task-owned prefix. This does not
claim a manual click through the new deletion dialog on the phone.

## Chat restart

The live-terminal branch previously required a successful chat-snapshot reply.
It now falls back to the selected profile's recorded conversation ID when the
transport cannot provide one. The captured profile/run/terminal identity is checked
again before stopping. The existing owned stop path is retained; replacement uses
the existing finished-session path. Only that profile is replaced. Files, stored
settings and native conversation IDs are not deleted or reset.

Ended terminals can use the same replacement path and still show the restart
button. Pending requests and captured identities reject repeated/stale actions.
A process ending during snapshot retrieval can continue through the ended path.
Codex `new` mode cannot borrow a stale old ID. If neither a native reply nor a
recorded ID identifies the conversation, restart stays blocked with a specific
message; no arbitrary new conversation is invented.

UI fixtures intercept terminal creation/stops. They cover all four agents with a
recorded ID, disconnection, stale/repeated confirmations, startup retry, an ended
process, a process ending during the reply, peer selection, stored records and a
synthetic history/attachment file. They are not real-model/network or phone GUI
acceptance. The unchanged owned Codex launcher is also exercised with task-owned
fixture processes: startup errors, retry, stop, child exit and unrelated survival.

## Verification

Final signed APK: **2.1.7-test1 /1534**, com.termux, ARM64 only, non-debuggable,
40,673,809 bytes. SHA-256:
`74bc0096420fdcbf5f52f65a5f1c128da1b9aa159473f0021ebf4dd421b50564`.
Certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
ZIP, signature, package/version/ABI, runtime DEX, seven ELF payloads, all assets,
notices, source/stage equality (638 files) and unchanged native-input reuse passed.
Download copy matches. No embedded RTK archive; component pins remain unchanged.

Final targeted JVM/UI result: **40 passed,0 failures/errors/skips** across restart,
environment selection and error UI. The unchanged owned Codex launcher process
fixture passed. Localization:587 workspace strings and settings/format checks passed.
The signed release build passed; debug JVM tests are a separate check. No GitHub CI
or publication was launched for this private build. The prior release's CI is not
counted as verification of the changed paths.
Full Modern Lint7.4.2: all five modules,0 errors/290 warnings, no infrastructure
errors. Local Python22 passed; native Termux21 passed/1 skipped because Android
rejects native hardlinks. That case passed in the local Linux/PRoot context.
The initial native harness incorrectly expected an endpoint after clean exit;
its failure is retained. The corrected test distinguishes clean unbind from the
explicitly seeded residual state. One UI test initially asserted before processing
its queued click; the corrected fixture waits for the UI callback.

## Phone checklist

1. On a separate disposable Arch with the residual path, open removal, check the
   exact selected alias and listed unscanned paths, cancel, then confirm removal.
2. Check that other environments remain present. A nonempty protected endpoint
   must refuse removal; it must not silently delete its contents.
3. For a saved chat with its recorded ID, reproduce connection failure and press
   restart. Check the same history/settings/attachments and only one new run.
4. Repeat after startup failure and process exit; press rapidly and cancel once.
   Check that a second chat continues unchanged.

These GUI scenarios on the final APK remain for the developer's phone acceptance.
No full desktop/GPU or real-cloud-model test is claimed. Evidence:
`.verification/fix-1534/` (native probes/cycles, helper tests, JVM reports, build,
Lint and final signed-file audits).
