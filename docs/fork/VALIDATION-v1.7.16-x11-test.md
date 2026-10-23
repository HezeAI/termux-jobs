# Private X11 check · 1.7.16-x11-test / code1231

Public stable remains v1.7.5. Private test APK; no GitHub publication.
The developer reports successful Mousepad text save/reopen, Ristretto image viewing,
Xarchiver archive operations, NetSurf browsing, LibreOffice use and Evince PDF viewing
in 1.7.15. These are specific developer checks, not exhaustive application certification.
Reported gaps: missing image thumbnails, Enter ineffective in graphical applications,
black wallpaper after changing it, and repeated startup fallback after tab return.

## Recommended programs and repository catalog

Desktop programs now opens a Recommended submenu above a paged searchable catalog.
The submenu contains Mousepad, Xarchiver, Ristretto + Tumbler, NetSurf, LibreOffice
and Evince. Thunar, XFCE Terminal, fonts and icons are baseline desktop components;
the installer and readiness check now include these explicitly. Existing desktop
setups without those components can use the normal confirmed setup action. No
packages or environments are reset automatically during the APK update.

Ristretto's configured-package group requires Tumbler. An existing Ristretto without
Tumbler is incomplete, so Install delivers the missing component. The same grouping
applies when selecting Ristretto from the general catalog. Actual D-Bus thumbnail
service activation/preview generation remains for device checks.

Catalog setup explicitly offers appstream, python3-gi, gir1.2-appstream-1.0 and
python3-apt **in the selected Ubuntu**. Opening a missing catalog is read-only and
does not install prerequisites. Confirmed setup installs these packages, then runs
APT update again so the distro AppStream APT configuration fetches authenticated
DEP-11 indices. No external application store, new repository, Flatpak or Snap
runtime is silently added. Metadata can increase the selected rootfs/cache size;
it is not embedded in the APK. Reading an existing catalog does not fetch the web.

A private Python helper uses AppStream OS catalog data only (no Flatpak or directory
monitor flags), selects desktop-application components, and matches package names
to the selected rootfs's native APT cache. Names/descriptions use Russian or English
metadata when available, with AppStream fallback; no machine translation is claimed.
Configured state is checked using APT's low-level installed-state constant. A missing
candidate is unavailable. Direct dependencies on snapd are marked unavailable;
this is not an exhaustive runtime compatibility test. Generic entries explicitly
say they have not been tested in our proot/X11 environment.

The helper and service bound names/descriptions/record sizes and refuse incomplete,
malformed or excessive catalogs (10,000 records safety ceiling). Catalog rows reach
the main process 30 at a time, keeping Binder payloads bounded. Search uses local
case-insensitive terms across name/package/summary, is debounced by 250 ms, and
updates only the ListView. Late pages for earlier queries are ignored, preserving
the Android search field and focus. Job IDs prevent stale operation replies from
replacing a newer install's state. No new continuous catalog polling is introduced.

Generic installs require a valid package name **and membership in the completed,
installable catalog for the requested target**; a syntactically valid arbitrary
package sent over Binder is rejected. Query text never becomes shell code. An
explicit per-program confirmation precedes native APT installation with --no-remove,
in the existing independent pipe-owned job. Existing progress, ETA limitations,
outside-tap/Back protection and cancellation behavior are retained. Stopping the
private X11 service also closes its owned program job; it never adopts terminal
or agent PIDs.

References: [AppStream CLI/data](https://www.freedesktop.org/software/appstream/docs/re01.html),
[upstream APT configuration](https://github.com/ximion/appstream/blob/main/contrib/apt-conf/50appstream),
[Python APT states/cache](https://apt-team.pages.debian.net/python-apt/library/apt_pkg.html).
No runtime metadata/setup installation was performed by the isolated build checks.

## X11 input and tab reattachment

Source diagnosis: the main Activity's IME endpoint already forwards Android key
and editor-action Enter, but committed text/newline flowed into the native Unicode
text-event path. The pinned key table maps Android Enter to Linux KEY_ENTER; no
native rebuild is needed to express a Return key. The new shared text bridge splits
LF/CR/CRLF and Tab into key down/up pairs, preserving other text and one Return for
CRLF. Both the Activity endpoint and renderer text fallback use the bridge. Current
composition is not replayed. Ordinary terminal/agent input paths are unchanged.
This covers a plausible IME path for the reported difference; actual Return behavior
in the developer's IME/browser/editor must be rechecked on the device.

A manual reveal after the startup probe timeout was local to a RemoteDesktopView.
Switching tabs detaches/releases that view and recreated one lost the reveal state,
causing the startup fallback to reappear even with XFCE running. The controller now
remembers successful attachment per target for its current service lifetime. A new
view can display the already attached desktop when native server ready is still true.
Readiness is never borrowed for another target or a disconnected/restarted backend;
error/end/cancel, controller close and service disconnect clear remembered targets.
The genuine first-start waiting/probe/fallback is retained. Physical tab switching
and IME interactions still require device evidence.

## Wallpaper findings and diagnostics

Read-only inspection confirmed xfdesktop/xfwm4/xfce4-session processes, saved backdrop
properties for monitorTermuxJobsX11/workspace0, image-style 5, disabled cycling after
the developer's retry, and an accessible valid RGB JPEG (1280x960). No personal image
path/content or authentication data is included here. The cause of the black
wallpaper is **not established**; it is not claimed fixed in this build. Similar
external issue descriptions alone are not proof of the same cause.

The installed XFCE metapackage is 4.20.1, xfce4-session 4.20.4-1 and xfdesktop4
4.20.1-1ubuntu2. The current Ubuntu test environment reports 26.04.1 LTS.
The running desktop/session/configuration was not changed by the inspection.
Private runtime authority paths were not accessible through the current proot
mounts, so a live authenticated X11 property query was not performed.

Details is renamed **X11 diagnostics / Диагностика X11**, including the dialog title.
Startup diagnostics record XFCE version, current/total X11 workspace numbers,
backdrop style and image-readable boolean from the desktop's own D-Bus session.
They do not print image paths or file contents and do not write xfconf properties.
READY/TIMEOUT is emitted before these optional diagnostics so they cannot delay
revealing the screen. Existing 60 Hz mode, stable viewport and IME focus bridge
remain unchanged. Wallpaper and true startup-probe reliability remain open checks.

## Verification and minimal device checks

Evidence directory: `.verification/x11-catalog/`. Relevant tests cover committed
newline/Tab/CRLF vs hardware/editor-action keys, composition, successful bounded
catalogs, failed/incomplete/duplicate/injected rows, pagination/query bounds,
package membership and target guards, UI recommended navigation, stale search
pages and search-view identity, plus prior X11 keyboard/geometry/gesture/menu/agent
checks. Python fixtures use fake APT/proot and AppStream/cache objects; they cover
Tumbler completion, missing catalog prerequisites requiring consent, native APT
filtering, old/new component-array APIs and Snap flags. These tests do not prove
actual GI metadata loading or package installation inside Android proot.

After installing over 1.7.15:
1. Enter in Mousepad inserts one newline; Enter in NetSurf loads the typed URL;
   an XFCE terminal and ordinary app terminal still submit commands normally.
2. Returning from main/notes/another terminal to a previously shown desktop does
   not require Show X11 screen again. Initial start and a real Stop/start still
   perform their own readiness checks.
3. Recommended contains six optional groups; already installed Ristretto without
   Tumbler offers Install. Install missing Tumbler, then test image thumbnails.
4. Prepare the catalog after consent, inspect results, search in either language
   and page back/forward. Install one small untested optional program. Status
   indicates package installation; test the actual GUI separately.
5. Cancel an optional install; verify other terminal/agent sessions continue and
   recheck package state. No outside tap/Back should cancel an installation.
6. If wallpaper remains black, capture X11 diagnostics after the next desktop start
   and a screenshot. No forced reload or speculative renderer patch was applied.

## Final artifact evidence

- 102 focused JVM/UI tests passed, zero failures/errors/skips. Includes returning
  from Recommended while its read-only check is pending and then refreshing the
  main catalog, avoiding a blank list on that navigation path.
- 38 Python/shell fixtures passed; shell syntax and git diff whitespace checks passed.
- Final release build passed in 45 seconds. Runtime scan: 13,353 defined classes,
  31 reviewed optional unresolved types; required runtime definitions verified.
  Existing Kotlin/Lint tooling warnings remain; clean standalone Lint is not claimed.
- ZIP integrity, com.termux identity, version 1.7.16-x11-test / code1231, minSDK26,
  ARM64, unchanged certificate, pinned native/keyboard inputs, JNI contract and
  private-marker checks passed. Source desktop/catalog assets match the final APK.
- APK `Termux-Jobs-v1.7.16-x11-test-arm64.apk`, 38953475 bytes.
- SHA-256: `e97cd9396372e59561a81a90bdb98a5f87f63468ac2bb5c3ada9ae44d6989909`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- VersionCode increases from 1230; same package/signature. Source review found no
  data reset, profile/auth schema change or environment replacement. Optional APT
  changes occur only after the relevant in-app confirmation.
- Delivered to `/sdcard/Download` with checksum; destination digest verified.
- Developer checks listed above concern 1.7.15. Physical Return, reattachment,
  Tumbler service and AppStream setup/catalog loading in 1.7.16 remain to be checked
  using the concrete sequence above. Wallpaper is still an open issue.
- No APK installation or process/session manipulation is part of these automated
  checks. No GitHub publication.
