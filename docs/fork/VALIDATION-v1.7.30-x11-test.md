# Private v1.7.30-x11-test / code1245 · Firefox compatibility checkbox

Public stable remains v1.7.5. Same com.termux/signing identity, code1244 →1245,
no data reset/history migration or publication in this task.

## Final scope

Replace the separate Firefox compatibility row with a compact PRoot checkbox in
Firefox's own action column, below Install/Installed. Both Recommended and the
Installed Firefox card have it; Installed also retains the existing delete action.
Click restores the current visual value immediately, opens the seals-protection
explanation and Enable/Disable confirmation, and only updates after backend success.
Cancel/busy/failure do not falsely toggle the displayed state. Actual journal/file
hashes from the selected rootfs determine the checkbox, including on catalog cache
hits. Read errors, incomplete or edited state disable the checkbox rather than
claiming the setting is known. State reads run on the program worker, never per
frame or in terminal/agent-session drawing. Weak widget references avoid retaining
old adapter rows.

The previous maintenance entry needlessly required stopping XFCE. Firefox-only
configuration now uses the independent owned program worker while the same Test
desktop remains running. The user only closes Firefox before applying the change
and relaunches it through its existing menu entry. No desktop stop or process kill
is sent. The normal package/desktop operation guards and cancellation still apply.
The permission-repair action retains its stopped-desktop requirement.

By the developer's final direction, no additional Firefox icons, panel shortcuts,
PATH command wrappers, shell aliases or commands are added. The existing consented
browser-only ~/.local/bin/termux-jobs-firefox wrapper and same-ID menu override are
retained. Plain/absolute binary launches remain outside this menu-only workaround;
existing Firefox instances need to be closed before enabling/relaunching. Seals
protection is weakened only for the wrapped browser, not a global environment.
No full sandbox-disable flag, browser preference change or new package dependency.

## Device evidence and limits

The developer's latest screenshot showed a Reddit Firefox window after running
the existing wrapper. The shared-memory rejection was absent in the displayed
output; separate media metadata and unsupported GL_EXT_shader_texture_lod warnings
were visible. Those are not treated as solved browser video/rendering scenarios.
Read-only inspection found the two managed files/journal present but an earlier
ordinary Firefox process lacked MOZ_SHM_NO_SEALS. This establishes bypass/reuse of
the wrapped launch, not the exact initiating UI action. The final change removes
the incorrect stop requirement and clarifies existing scope; no extra launch
routes were introduced after the developer narrowed scope.

## Verification

Fixture Python tests retain prior compatibility file safety, partial-state recovery,
shortcut cleanup without backups and background restoration checks. JVM tests cover
checkbox position/state/confirmation cancellation with a running desktop, the
Installed status/checkbox/delete card, and actual rootfs state hash/edit/symlink
handling. Existing Binder/repair/owned-job checks remain. Final logs and APK audit
are in .verification/x11-checkbox. Build may emit inherited Lint/Kotlin metadata
loader warnings; this is not a claim of a clean Lint run.

Next device check: keep XFCE running, close Firefox, open Programs → Recommended
or Installed, use the PRoot checkbox under Firefox's status button, confirm and
verify it updates. Open Firefox from its menu entry; repeat a toggle cancellation
and reopen the dialog to verify state persistence. These new device checks are
not implied by mocked UI tests or the prior successful command-line launch.

Final verification:38 JVM and59 Python tests passed. ARM64 release/runtime checks
passed. ZIP integrity, com.termux/code1245/ARM64/minSdk26, original signing
certificate, baseline native/JNI and source-asset identity checks passed.

APK: /sdcard/Download/Termux-Jobs-v1.7.30-x11-test-arm64.apk

SHA-256: `770d67bb2b2455569845baeed56ec668889c1f728d5d321003de3e61c7a97e18`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
