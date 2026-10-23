# Private v1.7.26-x11-test / code1241 · Garcon trial and LXQt integration

Stable public remains v1.7.5. Same com.termux package/signing identity, code1240
->1241. No reset, history migration or replacement of an installed environment.
This is build/source evidence for a manually installed private update; developer
phone verification of this APK remains pending. No GitHub publication or changes
to running user desktop/session processes are part of this build.

## Evidence motivating the change

Report7771150927172066570 is XFCE, not LXQt. It captured18,355 desktop and16,009
panel notifications in roughly6s, all ATTRIBUTE_CHANGED on .desktop files,
Garcon-tagged inotify monitors (poll0). Garcon reload counters reached30/39 and
then stopped increasing when the notifications stopped. This establishes the
notification storm and correlated rebuilds, not the identity of the writer.
Do not sum repeated cumulative rows or duplicated stderr tails as new events.

## Scoped trial

A separately built Garcon4.20.0 with the same SONAME/API is an app asset. It is
selected through LD_LIBRARY_PATH only for newly owned Test XFCE process trees,
only when the installed package reports4.20.0-*. Ordinary Ubuntu, agents, Termux,
other distributions and LXQt do not get this loader path. APT-managed files are
not replaced. A version mismatch uses the installed library and emits a marker.
New libraries are published by AtomicFile; existing mappings are not truncated.
Guardian ownership/private-process shutdown and native X11 rendering are unchanged.

The stopped-desktop card has a persistent XFCE menu stabilization toggle, default
on. Off is a behavioral baseline with the private upstream build still loaded,
no event suppression; it is not a claim that the system library itself was loaded.
Both states keep the same menu code and permit metadata diagnosis when recording.
Toggle changes while a desktop/component operation is live are rejected.

Only ATTRIBUTE_CHANGED for regular .desktop files is eligible. First/unknown
files, other event types, symlinks, read errors, oversized files, unstable reads,
identity/mode/ownership/size/mtime/content changes pass through. Every candidate
gets a bounded SHA256 read, including equal metadata: an early test caught that
same-size writes with restored mtime can evade a stat-only fast path under this
filesystem. That optimization was removed before building the delivered APK.
The filter keeps no file content:512 cached path/stat/digest entries, max256KiB
per file, bounded reset at capacity, a lock around cache/statistics. ctime-only
changes with equal content and significant attributes are eligible duplicates;
atime is ignored. This is not a filesystem sandbox or permission bypass.

## Additional diagnosis

Existing GIO notification/reload/XDamage recording remains. New phase=attributes
rows show first observations, equal metadata/content, content/mode/owner/identity/
mtime changes and unsupported/unreadable cases, plus a noncryptographic path ID.
Counter totals are since process start, not since capture. guard-filtered-total
counts actual suppression; off may detect identical snapshots but suppresses0.
With filtering off and recording off, snapshot work returns immediately. With
filtering enabled it runs even outside diagnostic recording, as required for the
mitigation. Record on/off is shared through the existing private control protocol.
No raw filenames, file contents, coordinates, titles, key text or sender PID are
logged; inotify does not directly identify the process which changed attributes.
The new SHA256/stat work has a cost and needs device latency/load comparison.

## LXQt

Check/install additionally verifies GVFS/backends, Breeze/Oxygen icons, LXQt themes
and Qt6 SVG libraries/plugins. Existing applications remain in the same rootfs.
The Qt platform theme is explicit; only an absent LXQt icon configuration is seeded,
never overwriting user edits. Fcitx FocusIn is no longer treated as proof of a text
field in LXQt: automatic keyboard activation is temporarily disabled there. The
manual top interactive-input button remains; XFCE automatic input is unchanged.
These changes require rechecking/reinstalling missing LXQt components through the
card and a fresh desktop launch. Virtual Computer/Network availability and icons
are not established by package checks alone; device verification is required.

Developer screenshot201723 looked mixed. Read-only process inspection found XFCE
session/panel/desktop, no LXQt session/panel; PCManFM-Qt was launched by Thunar in
the current XFCE group. Shared Desktop launchers can invoke it (user-home.desktop
in the screenshot), and are not proof of simultaneous desktop sessions. Those
user files/configuration were not deleted, moved, or marked executable/trusted.

## Automated validation

35 JVM/Robolectric tests passed: exact owner/private process/record controls,
atomic library publication, readiness isolation and live-choice guard, persistent
stabilization toggle and LXQt focus rejection. Device keyboard/gestures are not
proven by Robolectric.80 Python/shell/native checks passed after the stat-fast-path and per-consumer
cache fixes, including8 filter tests and the real-Garcon integration test.
The full original Garcon menu parser read a temporary fixture; actual inotify
callbacks from repeated chmod were filtered (passed1, filtered2); changing the
.desktop Name from Alpha to Bravo remained visible. All fixture file writes were
under repository .verification, no working desktop was attached or modified.
Unchanged upstream public symbols: all162 original definitions present in the
private build. Existing GObject/GIO hook/residency/control tests also passed.
Final build/APK audit evidence is appended after completion.

## Minimum developer check

Stop current desktop before updating/restarting. In Test XFCE turn stabilization
off, start recording, reproduce Applications flicker for10–15s, stop/export. Stop
XFCE, toggle on, relaunch and repeat. Confirm installing/removing a program still
updates the menu. For LXQt stop XFCE, select LXQt, check/install missing components,
then launch; check /root, icons, Computer/Network errors and manual keyboard.
No claim that the flicker is fixed until this comparison. Shared Desktop files
remain shared; separate per-desktop document directories are outside this change.

## Inputs and evidence

Pinned upstream source and Ubuntu build-only header inputs are recorded in
third-party/garcon/provenance.json. Rebuild instructions and corresponding source
are in third-party/garcon/README.md and APK assets/x11-garcon-source.zip. Original
LGPL/copyright material is preserved in assets/licenses/Garcon-COPYING.txt.
Logs, isolated fixtures, signature/ABI audits: .verification/x11-garcon-fix.


## Final evidence

Final APK build succeeded (28s);13,373 defined classes, required runtime classes
present,31 reviewed optional missing types, no unreviewed missing definitions.
Inherited Kotlin metadata/Lint-loader warnings remain; clean Lint is not claimed.
ZIP integrity/no duplicates, com.termux, version/code1241, minSDK26, ARM64-only,
nondebuggable status, signing v2 certificate and SHA256 verified. Certificate
dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644 matches the previous private build/stable identity.
Pinned X11 native/keyboard assets and JNI28/1/5 match; expected source assets and
new Garcon library/source/license match final files. Forbidden personal-file
and private-key/token marker scans passed, not an exhaustive security proof.
Corresponding source ZIP was inspected: upstream source hash, final guard/header,
patch/build script and pins match; all162 original public symbols remain present.
The overlay has NODELETE, expected SONAME, no RPATH/RUNPATH or bundled GLib.

Review additionally found that a process-global per-path cache could prevent a
second menu from receiving a real change. Final keys include the menu consumer;
weak callbacks remove entries on consumer destruction, avoiding stale pointer
reuse. The512-entry bound is global across consumers. A dedicated two-consumer
test confirms each menu sees first and changed content. NODELETE keeps registered
weak/diagnostic code pointers valid until the owned process exits.

APK: Termux-Jobs-v1.7.26-x11-test-arm64.apk (39731711 bytes)
SHA256: 467771e6c1c75dec19863404084a23f626fd011e34d1ec019a8236fc6cd952e6
Same signing identity, higher code; no new data migration/deletion code. Copied
to /sdcard/Download with a SHA companion; destination digest verified. Actual
flicker/load, keyboard/manual input, icon/GVFS behavior and menu updates after
package changes still require the developer's device comparison.
