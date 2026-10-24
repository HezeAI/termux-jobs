# Termux Job’s v2.0.3 — X11 landscape and stability fixes

Rotate your phone to use an X11 desktop across the screen, without app tabs or
the terminal input row. Rotate back or press Android Back to return the same
desktop to its embedded app window. Open programs stay running. A small `|` button
at the top right provides interactive keyboard input, with a filled enabled state.
It sits slightly below the top edge to leave more clearance.

Landscape presentation is shared by Termux, Ubuntu, Debian, Alpine, Fedora,
openSUSE, Void and Arch Linux ARM. Logical landscape resolution follows the
physical display, capped at 2880 pixels wide while retaining its aspect ratio.
Portrait resolution is retained.
Keyboard occlusion changes the visible crop rather than the desktop mode.
No desktop package, driver or rootfs migration is required for this feature.
The app's localized Activity now applies incoming system window configuration
while preserving the chosen language, so rotation is not read from launch-time
orientation/bounds. This updates the existing screen without recreating sessions.

All eight X11 environments now offer the same optional Kali-inspired Termux-job's
style: dark windows, blue accents, bundled wallpaper/icons and a black/white cursor.
Each has its own checkbox below Switch to desktop. Stop the desktop before changing
it; changes apply at next launch. Turning it off restores tracked appearance
properties while preserving later manual edits. Each guest uses only its own HOME
and system defaults. Distribution menus/plugins are retained, including Whisker
Menu and inherited openSUSE panel defaults. No extra theme download or package
installation is required.

Fedora's Video and audio setup now includes a private bundled H.264 decoder and
GStreamer bridge for Parole, with explicit runtime dependencies, a dedicated
registry and decoder/factory checks before reporting ready. Retry Video and audio
setup after updating, then close Parole and relaunch from the applications menu.
Only the two private codec assets are bundled; regular Fedora packages still need
internet. System RPM libraries and other environments are retained. H.264/AAC MP4
was decoded to EOS in an isolated native test; this does not imply every MP4/HEVC
format or a fresh-phone Parole GUI check. The fullscreen input control is lower
and 65% opaque, retaining its enabled fill.

This release also incorporates the stability-audit fixes developed since v2.0.2:

- **Agent chats:** isolate OpenCode runs and stale responses; improve Codex login
  retry and ownership of pending work without changing existing account stores.
- **Installers and X11:** keep cancellation in progress until the owned worker has
  finished; prevent overlapping component operations and accidental cross-target
  work; preserve per-environment desktop/process isolation.
- **Backups and storage:** bind confirmation to the current selection, protect
  cancelled/closed jobs and final file commits, and serialize workspace snapshots
  through atomic persistence. Invalid duplicate-ID libraries are kept read-only.
- **Transfers and attachments:** use exclusive local outputs, preserve existing
  files, validate transfer completion and report uncertain outcomes after an
  interrupted operation without automatically replaying Copy/Move.
- **Updates and external files:** validate updater HTTPS routes, downloaded APK
  identity and signing continuity; reject unsafe shared-file destinations.
- **Settings and Android file access:** remove Activity-retaining preference
  singletons; validate document names, root boundaries and opened file handles;
  reject outside-HOME SAF links and special/missing legacy share files.
- **Images, notifications and locale:** update thumbnail EXIF parsing, fix image
  click accessibility, make fixed notification actions immutable and avoid
  device-locale changes to protocol values and keyboard shortcut matching.
- **Development checks:** add a reproducible five-module lint gate and publish
  categorized warnings and the audit reports. Remaining findings are documented,
  rather than hidden behind a blanket suppression.

## Updating and compatibility

- Android 8+ / ARM64; embedded X11 requires Android 11+. Package `com.termux`, the
  signing identity and workspace schema8 are retained. VersionCode1352 supports forward updates from v2.0.2 and the tested candidate builds. No valid-data reset, account migration or
  environment reinstallation is required.
- Finish important operations before updating. Android can terminate app
  processes during an update; live operations are not migrated. Save agent chats
  and restart agents explicitly after updating to load the corrected helpers.
- Android's Termux documents provider is now confined to HOME. Existing
  outside-HOME links/grants are intentionally refused; choose phone storage or
  the appropriate environment in the workspace file manager instead. Workspace
  browsing and authorized agent shell access are not restricted to HOME by this.
- Existing Download outputs are not overwritten: attachment exports use numbered
  names, while legacy incoming shares require another name on a collision.
- Malformed libraries containing duplicate IDs open read-only without an automatic
  reset. Keep a backup before repairing such a library.
- Interrupted SSH/proot Copy/Move can have an uncertain result: inspect source and
  destination before retrying. A disconnected local transport does not guarantee
  that every remote/detached child stopped, and completed changes are not rolled back.

## Validation and limits

The developer checked the cumulative changes, landscape desktop presentation and
Fedora MP4 playback before approving publication. The final release keeps the same
production sources, resources and runtime assets as that checked candidate;
release version fields are updated. Automated results and artifact details are
recorded in the [validation report](https://github.com/HezeAI/termux-jobs/blob/v2.0.3/docs/fork/VALIDATION-v2.0.3.md).
The final JVM run passed1,229 tests in184 classes without failures or skips;
578 Python cases passed with one intentionally retired-overlay case skipped;
all31 agent/protocol fixture programs passed. Five-module lint reports0 errors
and210 [reviewed warnings](https://github.com/HezeAI/termux-jobs/blob/v2.0.3/docs/fork/LINT-WARNINGS-2026-10-24.md).
The [final audit](https://github.com/HezeAI/termux-jobs/blob/v2.0.3/docs/fork/AUDIT-RELEASE-v2.0.3.md)
links the focused findings, regression evidence and remaining limitations.

The supported device baseline includes developer-reported work on RedMagic 10 Pro
and Samsung S26 Ultra; this is not a claim that the new landscape feature or
every renderer/clean installation has been tested on both devices. Other devices,
Mali/Dimensity graphics, fresh-device setup and interrupted remote work require
additional testing. Target SDK28/package-ID/sharedUserId modernization is separate.
The lint tool's compileSDK support warning remains documented.

Sources and upstream credits:
[third-party components](https://github.com/HezeAI/termux-jobs/blob/v2.0.3/docs/fork/THIRD-PARTY.md),
[Termux](https://github.com/termux/termux-app),
[Termux:X11](https://github.com/termux/termux-x11).
HezeAI is the fork developer.

AI development assistance: OpenAI Codex
