# Private v1.7.41-x11-test / code1256

Package com.termux and signing identity retained, code1255→1256. No user-data reset,
rootfs replacement, system package installation, APK installation or publication by
this verification. Build uses repository-owned staging/SDK/tools. Installed files were
only read to investigate the reported failures. Phone runtime checks below remain for
the developer; this report does not claim they were performed by automated tests.

## Changes and evidence

Program installation now updates its own card: Stop, horizontal progress, measured
stage ETA/download volume and a single marquee line for the current package/file.
No package-manager file-count is treated as bytes. Unknown timing/volume stays unknown;
install-stage text hides stale download volume. No top-level chat/terminal reconstruction
is used for these updates. Unchanged progress/completion snapshots do not rebuild the
catalog adapter. Background task ownership stays in the existing foreground X11 service;
closing the list, navigating or switching tabs does not issue cancellation. Reopening
reconstructs the active card. Android force-stop/service shutdown is not covered by
that continuation guarantee. Stop retains the existing exact owned-process cancellation;
package repair may be required after interruption.

Installed cards show status plus a right-side trash button with confirmation. The
recommended shortcut targets only its explicitly named main application package;
shared dependencies/documents and other applications remain. LibreOffice shortcut
outside Termux names Writer rather than implicitly deleting Calc/Impress. Server allows
only verified same-target recommended mappings or its installed catalog. Core-package
protection, dependency refusal and package-name validation remain. Tests reject unknown,
protected and wrong-environment deletes without launching a guest.

Recommended/installed navigation stays enabled during catalog refresh and read-only
checks. Cached lists are read without cancelling/restarting the active job. Missing
lists are scanned only when the previous operation completes. Another package mutation
cannot start while a job is active.

Desktop menu has Switch/Return + Programs, and conditional Stop while started in both
terminal and graphical views. Components/Installed rows are removed. Switch checks and
launches if ready, otherwise offers explicit installation and then launches. Only an
acknowledged check/install request can complete the pending launch intent; stale polled
snapshots cannot bypass checking. Session close clears its pending intent. Header text
is simply Terminal/Desktop (localized). Lifecycle/owner rules and native server remain.

Evince screenshot105048/105056 shows ghostscript network read failure from selected
Termux mirror, not proven PDF incompatibility. Desktop APT invocations now use command-
local Acquire::Retries=3 and HTTP/HTTPS idle Timeout=30; APT reuses available archives.
No outer install/dpkg retry, repository switch, global configuration write, signature
or TLS bypass. Real loopback apt-helper fixture verifies503 retry then success and no
retry on SHA256 mismatch. This does not establish better mirror throughput. APK Alpine
automatic retries are not added: unsafe repetition of partial transactions was avoided.

Alpine Tumbler/plugins are present; thumbnail cache is empty in the read-only snapshot.
Private dbus-run-session previously started before Alpine LD_PRELOAD was set. Real
isolated D-Bus activation fixture proves late shell environment does not reach the bus-
activated service. Export now occurs before the owned Alpine bus, with idempotent session
fallback. Other environments/buses/agents are not preloaded. Binary is unchanged; inherited
reduced Glycin decoder isolation from1.7.36 remains. This is a code-path correction and
plausible thumbnail fix, not a captured live Tumbler crash or verified phone result.

## Checks

140 relevant JVM tests passed: X11 service/protocol/process/catalog/input tests and
WorkspaceDesktop UI, header, install UI, programs and multi-environment facade. New cases
cover inline Stop, close/reopen without cancel, current-card completion/trash positioning,
progress without catalog recreation, navigation while checking/refreshing, stale check
snapshots, explicit missing-component prompt, and closed-owner launch cancellation.
Two old fixtures were adapted to current nested button row/facade types. Final label-only
text change followed those functional checks; final APK build includes that text.

94 Python tests passed (92 X11 routing/programs/readiness/compatibility checks and2 APT
retry tests), including isolated D-Bus inheritance and narrow compatibility ABI/fallback.
No tests install packages or affect the developer's desktop.

Final ARM64 build/runtime, archive/signature/source-assets/native/JNI and destination
hash results are recorded below after audit. Inherited Kotlin metadata/Lint dependency
warnings remain; this is not a clean Lint claim. Limited private-marker scan is not a
claim of exhaustive security analysis.

## Minimum developer checks

- Install a small recommended program, navigate across program sections and app tabs,
  reopen its card; confirm progress remains and only Stop cancels.
- Refresh catalog and check recommended/installed lists; navigate back/forward without
  interrupting them. Confirm existing lists stay readable and no duplicate job starts.
- Confirm installed status/trash, correct named-package confirmation and protected items.
- Switch to an installed desktop directly; missing components must prompt before install.
  Stop must be present in terminal and graphical views only while the desk is started.
- Retry Termux Evince with usable network. Network bandwidth/mirror health remains external.
- Restart Alpine desktop and test new image thumbnails. Test gestures, keyboard, scrolling
  and tab/last-owner close; mocked UI tests do not validate those phone behaviors.

## Final artifact

ARM64 release build passed. Runtime definitions13386, optional reviewed unresolved31.
ZIP integrity, package/version1256/minSdk26/ABI, original v2 certificate, source assets,
pinned renderer/compatibility binary, Java staging, JNI28/1/5 and limited private-marker
checks passed. Destination digest matched. No data-reset migration added.

APK: `/sdcard/Download/Termux-Jobs-v1.7.41-x11-test-arm64.apk`

Size: 39006573 bytes.

SHA-256: `620503869e87e61b99c32cf0133a5ae01598019deafde7c2727b429f617ce097`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
