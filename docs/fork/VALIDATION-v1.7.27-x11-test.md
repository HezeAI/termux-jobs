# Private v1.7.27-x11-test / code1242 · launcher permissions / retire Garcon trial

Public stable remains v1.7.5. com.termux identity and signing key unchanged;
code1241 ->1242. No data reset/history migration. These are source/build checks;
phone validation of this APK remains the developer's next step. No publication.

## Device failure and corrected diagnosis

Developer reports7551134827890682297 (filter off) and6813707509476186232 (on)
reported frozen desktops. Off was an invalid baseline: the private library still
ran synchronous checksum diagnosis while recording. Snapshot work could amplify
a file-notification storm; the reports do not isolate the entire freeze cause.
The on report already had28,843 process-lifetime suppressions before capture3,
but no evidence of a responsive menu, and the developer confirmed the freeze.
The trial is failed. Historical automated Garcon tests are not proof of device
responsiveness. It is not offered as a usable fix in this APK.

The repeated path hash580e082d84153a47 was matched by read-only enumeration to
/usr/share/applications/libreoffice-writer.desktop in jobs-x11-ubuntu. Its contents
match the libreoffice-writer installed dpkg md5sums manifest. This is an integrity
comparison against the local package database, not antivirus/authenticity proof.
Its mode was observed0600 during one probe and0000 later, consistent with temporary
permission manipulation, not proof of the original writer or archive mode.

Isolated real GIO/inotify tests under the current PRoot reproduced:
-30 reads, mode0600/0644:0 file notifications.
-30 reads, mode0444:59 (repeat60) ATTRIBUTE_CHANGED notifications.
-30 reads, mode0000:60 ATTRIBUTE_CHANGED notifications; mode restored0000.
Temporary files were under repository .verification only. No user launcher,
installed package permissions or running session was changed by those probes.
Upstream termux/proot fake_id0.c contains temporary owner-read/write chmod and
mode restoration for root permission emulation. That upstream source was not
version-matched to the installed executable; the local reproduction is stronger
behavioral evidence. This supports the permission/read feedback mechanism, not
an exhaustive explanation of all earlier graphics symptoms or a malware verdict.

## Implementation

The experimental libgarcon-1.so.0 is omitted from APK assets and from DesktopProcess
copying. The Test session script never sets its LD_LIBRARY_PATH. The UI disables
the old filter control, Binder/state use false and the child environment forces0,
regardless of saved preferences. Original installed Garcon is used. Its package
file is not replaced. Historical experiment source/license remain for reference.
Existing GTK observation counts notifications without checksum snapshots; failed
trial callback pointers are absent because its library is not loaded.

Only Ubuntu X11 Test gets a new explicit Repair launcher permissions action.
It requires confirmation, a stopped desktop and no component operation in progress.
Service and shell independently restrict repair to jobs-x11-ubuntu. The operation
uses the existing owned guardian/progress/cancel/return-to-card flow. Ordinary
Ubuntu, agents, Termux and other distributions have no permission-repair action.
No user session shutdown or installed-environment mutation is performed by the
build. The developer activates the repair explicitly after installing the APK.

Guest helper scans regular .desktop files in /usr/share/applications and
/usr/local/share/applications only. It preserves contents and execute/group/other
bits, adds owner read/write only when absent, skips symlinks and modified/unowned
launchers (local package md5 manifest comparison). Before chmod it saves original
mode/device/inode in ~/.local/state/termux-jobs/launcher-permissions.json through
atomic replacement, then checks descriptor identity and uses fchmod with nofollow.
Backup links/ancestor links are rejected; repeat operation is idempotent. It is
not a package verifier/sandbox, does not grant Android root/storage permissions,
and does not make a launcher trusted/executable. No automatic post-install repair
is added: newly installed problem launchers require another explicit operation.

Cancellation can leave a repaired subset; original modes remain in the backup.
No automatic rollback of permissions is attempted. Underlying kernel/proot races
and actual Test guest fchmod behavior still require device validation. Rootfs
contents, program scripts and user documents are not rewritten by this helper.
LXQt GVFS/icon/manual-IME changes from1.7.26 are preserved.

## Checks

35 JVM/Robolectric checks passed, including disabled unsafe toggle and explicit
repair confirmation, existing owner/private-process/diagnostic guards.84 script/
native checks passed before retiring the generated trial library, including3 new
permission-helper checks: dry-run/idempotence/original modes/content preservation,
symlink/unrelated-file exclusion, unowned/changed checksum exclusion. Tests of
historical Garcon source do not validate this APK's device behavior; its runtime
library is absent. Frozen existing processes were inspected read-only, not stopped.
Final source/build/ABI/signing audit evidence is appended below.

## Minimum phone check

Stop the currently frozen desktop through its own card. Install this private APK.
Open Ubuntu X11 Test desktop card -> Repair launcher permissions -> confirm.
Wait for completion; launch XFCE, open Applications and submenus, /root and an
existing application. Check that scrolling/pointer/keyboard remain responsive.
The old filter cannot be enabled. No need to repeat the failed filter comparison.
If restoring original launcher modes is required, the per-file original values
are retained; automatic restoration is intentionally not part of this operation.


## Final APK evidence

Final build succeeded (58s);35 relevant JVM/Robolectric tests and84 scripting/
native tests passed.13,376 defined classes, required runtime definitions present,
31 reviewed optional missing types. Inherited Kotlin metadata/Lint-loader warnings
remain; clean Lint is not claimed. ZIP integrity/no duplicate entries, package
com.termux, code1242/minSDK26/ARM64/nondebuggable status, signing v2 certificate
and SHA256 checked. Certificate dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644 matches the previous
private/stable identity; pinned X11/keyboard and JNI28/1/5 unchanged. The failed
Garcon executable library is absent; the permission helper matches final source.
Historical source archive/license remain; forbidden personal files/private-key/
token marker scans passed (not an exhaustive security audit).

APK: Termux-Jobs-v1.7.27-x11-test-arm64.apk (39674247 bytes)
SHA256: 3bed08e144d4b1bbc0e01a81f5fd31c207c9370c7525714140f5c5c314b761f9
Copied to Download; destination digest verified, SHA companion written. No new
data migration or reset. Device validation must confirm repair counts, ordinary
menu responsiveness and absence of flicker; those outcomes are not established
by the isolated reproduction or mocked UI. The existing user desktop and package
permissions were not modified during these read-only checks/builds.
