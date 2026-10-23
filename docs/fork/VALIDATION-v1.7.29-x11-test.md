# Private v1.7.29-x11-test / code1244 · finalize XFCE test

Public stable remains v1.7.5. com.termux and signing identity unchanged; code1243
→1244. No data reset/history migration or GitHub publication in this task.

## Firefox: diagnosis and explicit compatibility

Developer confirmed that ordinary Firefox launches but tabs crash. The new log
from /root/firefox-memory.log in Test Ubuntu shows parent `read-only dup failed
(Permission denied); not using memfd`, then child `shm is shrinkable??`, the unsafe
PlatformHandle IPC error and signal11 exits. A repository-local descriptor probe
also found memfd creation/sealing succeeds, but read-only reopening through
/proc/self/fd is denied. Mozilla's current SharedMemoryPlatform_posix.cpp explains
Android SELinux can deny that reopen; parent/GPU processes fall back whereas
child processes still use memfd capability checks. This matches the observed
seal validation mismatch, not a general XFCE failure.

Primary source: https://github.com/mozilla-firefox/firefox/blob/main/ipc/glue/SharedMemoryPlatform_posix.cpp
The installed libxul.so contains the specific diagnostic strings and
MOZ_SHM_NO_SEALS option. Current upstream source was inspected; it was not rebuilt
or asserted byte-identical to the installed Firefox package.

Developer then confirmed a one-launch `MOZ_SHM_NO_SEALS=1 firefox --no-remote`
test opens the site successfully. This bypass weakens shared-memory seal
protection; it must not be described as security-neutral or as a fully tested
sandbox. No entire Firefox sandbox disabling is introduced.

Programs → Recommended AND Installed now contain the same Firefox · PRoot
compatibility action. It is OFF by default, requires an explicit warning/Enable
confirmation, a stopped desktop/no package operation and jobs-x11-ubuntu at
UI/Binder/process/shell boundaries. Firefox must first be installed.
Enable creates a user-local executable wrapper and a same-ID Firefox .desktop
override, preserving localized descriptions and action arguments. Only this
wrapper sets MOZ_SHM_NO_SEALS; no desktop-global environment, shell RC, system
browser binary, packaged launcher or ordinary agent Ubuntu changes. Desktop-menu
Firefox launches (including private/new window actions) use it. A plain `firefox`
shell command remains unchanged. Disable removes only matching app-managed files.
Local existing overrides/edits, unexpected Exec fields and symlink ancestors are
refused. A durable hash journal precedes file writes, so interrupted enable can
be disabled safely. User browser profiles/preferences are never changed.
The Firefox description now points to enabling compatibility for tab crashes,
instead of saying PRoot requires testing.

## Retired LXQt shortcuts

The temporary Remove LXQt card/backend/package-removal helper are removed.
Read-only audit of Test Ubuntu Desktop, local applications and autostart found
three remaining references: computer.desktop, network.desktop, user-home.desktop.
On next Test desktop start, maintenance removes only byte-identical known
PCManFM-Qt-generated copies, only if /usr/bin/pcmanfm-qt is absent, without making backup copies, as explicitly requested by the developer. Supports normal,
Russian and safe user-dirs desktop locations. Unrelated files, edited shortcuts,
links, XFCE's own icons and custom backgrounds are retained. Cleanup exceptions
are reported but do not prevent a desktop launch. No installed rootfs was edited
by the assistant during development; cleanup is performed by the app at the next
user-requested desktop start. Repair launcher permissions remains in Installed.

## Startup investigation and bounded improvement

The startup path does not contain a mandatory splash sleep. Manager/desktop
polling exits on readiness; keyboard extraction already has a persistent marker.
A synchronous wallpaper helper did extra diagnostic work before xfwm4 started:
mode/workspace queries, style/cycle lookups and GdkPixbuf/ctypes decoder loads for
all stored monitor backgrounds. The normal startup now uses --quick-start:
only expected monitor properties and existing restoration/fallback logic run.
Explicit --inspect retains full diagnostics. The fixture with four custom
backgrounds goes from15 xfconf calls +4 decode calls to5 +0, with identical
fallback writes in a separate test. Custom image choices remain unchanged.
This is a reduction in startup work, not a measured phone speedup; no percentage
or number of seconds is claimed. The working desktop was not restarted for tests.

## Evidence and remaining device checks

Focused fixture tests cover enable/idempotence/disable, unchanged system launcher,
custom-file and symlink refusal, partial-enable recovery, exact shortcut removal without backup,
preserving edits/application-installed shortcuts, quick/full wallpaper equivalence,
and existing APT/guest/repair/observer paths. JVM checks cover both UI entry points,
warning/cancellation, stopped-desktop enforcement, error return navigation and
Binder target boundaries. Final results and APK audit: .verification/x11-finalize.
Inherited Lint/Kotlin metadata loader warnings are not a clean Lint result.

Next developer checks: update over1.7.28, stop/start XFCE and verify three retired
icons disappear while custom background and XFCE icons remain; enable Firefox
compatibility from either menu and verify menu-launched browsing/private actions;
disable and verify its ordinary launcher is restored. Compare cold desktop start
subjectively or with screen recording. These new UI/gesture/browser integration
checks are not implied by the previous command-line browser success.

## Final verification

36 focused JVM tests and58 Python tests passed. ARM64 release build and required
runtime definition audit passed. Final APK ZIP, package/code/ABI, signature, native
JNI contract, asset identity and private-marker checks passed. Native core remains
identical to the pinned version. Final artifact:

/sdcard/Download/Termux-Jobs-v1.7.29-x11-test-arm64.apk

SHA-256: `f6d4563df9fe0f195ea1271416378423599eb2665dfe143db593c85ca54df039`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

Source scope: X11/helper/menu/tests/docs/version only; no working processes,
installed rootfs or system packages were modified.
