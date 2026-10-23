# Termux-job's X11 AVAILABLE

**v2.0 — integrated Linux desktops, graphics renderers and desktop applications.**
APK version: **2.0.0**, versionCode **1320**. This major release brings together
all development since the public **v1.7.5** release.

## X11 desktops inside the app

Run an XFCE desktop directly in your session: **Termux, Ubuntu, Debian, Alpine,
Fedora, openSUSE, Void Linux and Arch Linux ARM**. A separate Termux:X11 app is
not required. This integrates the upstream Termux:X11 core; it does not replace
Android's Linux kernel or turn PRoot into a virtual machine.

- Switch between **Terminal** and **Desktop** without rendering both views at once.
- Each supported target has its own X11 service/process, display and runtime paths.
  One desktop per target prevents duplicate desktops for the same environment.
- Central startup screen: desktop check, consented installation, renderer
  preparation and startup, with progress, current step and cancellation.
- Closing the owning session also stops its desktop. Stopping one target must
  not terminate unrelated terminals, agents or other targets' desktops.
- Automatic keyboard checkbox, manual interactive input, responsive text entry,
  Return/Enter handling, pointer input, moving and resizing windows.
- Shared-storage permission prompt and **Phone storage / Downloads** shortcuts
  make Android storage easier to reach where directory enumeration is restricted.
- PulseAudio integration, fonts, certificates and an explicit desktop runtime set
  for each distribution. Optional application dependencies stay with that app.
- Session settings are accessible through visible gear buttons, including in the
  running-session list.

## Choose your graphics renderer

**CPU**, **Turnip + Zink**, and **VirGL** are available across the eight X11
systems. **Wrapper** is an additional experimental option for **Termux only**.
Choose before startup; required components are offered during desktop launch.
Renderer changes are disabled while the desktop is running.

- **Turnip + Zink:** optional, pinned Mesa packages for each target's ABI/libc,
  downloaded into private app-managed storage. Uses the Adreno GPU on compatible
  Snapdragon devices; it is not a universal driver for every GPU or firmware.
- **VirGL:** a dedicated Android/Termux rendering server per desktop, private
  socket and the selected environment's Mesa client. Debian/Termux startup fixes,
  explicit virpipe routing and compatible ANGLE backends are included.
- **Wrapper:** import a compatible Bionic Turnip ZIP with the in-app file manager,
  then select from previously imported drivers. Validates archives and keeps
  drivers separate from the installed system libraries. No Venus integration.
- **GPU & GPU INFO:** optional recommended tools install the distribution's
  packages and create desktop shortcuts. GPU INFO uses a bounded EGL query on
  Linux VirGL targets affected by GLX/X_GetImage errors; the output identifies
  this as EGL. GPU benchmarks themselves remain separate from information queries.

Termux/Debian/Fedora/Void/Arch VirGL use ANGLE Vulkan; Ubuntu/Alpine/openSUSE
retain ANGLE GL. This is an explicit compatibility selection, not an automatic
promise that Vulkan or VirGL works on all Mali/Dimensity devices.

## Desktop programs and appearance

The **Programs** button sits next to the Terminal/Desktop selector. It includes
**Recommended**, **Installed programs**, and **Search and install programs**.

- File manager, terminal, text editor, archive manager, image viewer/thumbnails,
  browser, office, PDF viewer, video/audio and GPU utilities, with native package
  choices for each distribution. Availability depends on its repositories.
- Installation progress stays inside the program card: Stop, progress line,
  download amounts and remaining-time estimates when the package manager reports
  them. Installed cards expose removal; pending operations can offer Continue.
- Switching tabs or closing the program list does not itself cancel an operation.
  Saved resume state can survive an app restart, but Android may kill the process;
  package-manager repair/revalidation may be required. No uninterrupted download
  or byte-range resume is promised for every package manager.
- Program catalog cache is stored per environment and replaced on manual refresh.
  Refreshing a catalog does not block the other program sections.
- Distribution-specific Firefox compatibility/HTTPS settings, PDF/media fixes,
  OpenH264 for the openSUSE media launcher, and Alpine thumbnail handling.
- Optional **Termux-job's style** for the base Termux desktop: Kali-inspired dark
  XFCE, larger controls and a branded wallpaper. This is a theme, not Kali Linux
  or a penetration-testing tool bundle. Other desktops retain their own style.
- The app header now displays the italic **Termux Job’s** name with **v2.0**.

## Independent Ubuntu AI and installation improvements

- **Ubuntu AI** is the default for newly created agent sessions/presets. Missing
  selected environments and agent components are offered for installation.
- Optional **agent environment migration** copies the existing Ubuntu environment
  to Ubuntu AI, including projects, tools, CLI history and credentials located
  inside that environment. It is a bulk copy for the agents, not just one CLI.
  External mounted phone files are not duplicated. Review the secret-copy notice.
- Migration checks source/destination activity, free space, source changes and
  copied file hashes. The original environment is not automatically deleted.
  Busy sources block migration rather than forcibly stopping running sessions.
- Files, package operations and checks follow the selected agent environment.
  Existing profiles keep their selection until you change it or confirm migration.
  PRoot environments still share the Android app's permissions; this is not a
  security sandbox between agents and your phone.
- Environment cards show inline installation progress and Stop/Continue state.
  Error details can be exported for troubleshooting. Native proot-distro handles
  supported image installation; complete verified cached layers may be reused.
- Environment removal and storage cleanup use the selected rootfs and reject
  busy targets. Backups and presets are separate from the running desktop.
- The optional **connection command** is intended for running an agent through
  **your own server**, such as your own SSH tunnel/proxy launcher. Enter the
  absolute path of an executable wrapper script inside the selected environment,
  not a server IP or a free-form shell command. The script receives the agent
  command/arguments and must execute them. Leave it empty for ordinary startup.
  It applies to a new launch, does not silently reroute an existing chat, and
  does not modify global shell settings. Personal SSH/proxy configuration and
  keys are not included in the APK.

## Agent chat and interface fixes

- Fixed messages/events appearing beneath the shell input before reaching the
  chat and protected input focus/drafts during incoming updates.
- Interactive input and extra-key state remain scoped to the current session;
  switching tabs must not carry another session's enabled controls.
- Session settings, working-directory/file-manager routing and status controls
  account for the selected environment. Desktop/transfer activity indicators
  update their own elements instead of recreating the whole terminal interface.
- Expanded **X11 diagnostics** in session diagnostics: startup/component/renderer
  state, bounded error categories and export. These reports do not contain every
  application log and should still be reviewed before sharing.

## Tested devices and remaining coverage

**Tested on RedMagic 10 Pro and Samsung S26 Ultra. Other devices require testing.**
These are developer-reported device checks, not a claim that every renderer,
application and clean-install combination was tested on both phones.

The developer confirmed desktop/GPU operation during the distribution-by-
distribution checks. Recent VirGL confirmations cover all eight desktop targets;
Termux GPU test and Debian/Ubuntu 3D scenes were confirmed. The final Ubuntu
GPU INFO EGL adjustment still needs a device recheck. Full benchmark scores,
long-duration stability, every clean-device install and Mali/Dimensity support
are not claimed as verified. CPU, Turnip and Wrapper should be retested on the
recipient device before relying on a particular workload.

## Updating and data compatibility

**Android 8.0+ / API26, ARM64 only, package `com.termux`, unchanged signing key.**
VersionCode1320 is higher than public v1.7.5 and the private v1.7.104 builds.
Compatible existing Termux Job's installations can update in place; do not
uninstall the app to update. Android may terminate active jobs during APK
installation, so save important work first.

No general data reset is required. Commands, notes, presets, chat history,
servers and existing environments are retained; schema8 remains readable.
Agent migration is optional and does not automatically delete the original
Ubuntu. Back up credentials/projects before deliberately copying or deleting
an environment. Private Touch Workspace selections fall back to the retained
Kali-inspired style; the retired Ubuntu X11 Test is not offered for new sessions.

When updating from releases before v1.5.3, the previous removal of local-AI
private models/chats/settings still applies; those old profiles are skipped.
External model originals and user distributions are not automatically deleted.
The old local-AI feature is not reintroduced in v2.0.

## Sources, authors and acknowledgements

**HezeAI develops this fork; Codex assists with implementation and investigation.**
The X11 core, graphics libraries, applications and distributions belong to their
upstream authors. Our work integrates them with the session UI, installation,
input, environment routing and compatibility handling.

- [Termux app](https://github.com/termux/termux-app),
  [Termux packages](https://github.com/termux/termux-packages) and
  [proot-distro](https://github.com/termux/proot-distro) — Termux contributors.
- [Termux:X11](https://github.com/termux/termux-x11/tree/fa3a8b430e2896a19f44c99a9cb056254615ae06)
  — Termux:X11 contributors; pinned original native core, GPL-3.0.
- [XFCE](https://gitlab.xfce.org/xfce) and
  [Mesa](https://gitlab.freedesktop.org/mesa/mesa) — upstream desktop, Turnip,
  Zink and VirGL-client authors.
- [lfdevs/mesa-for-android-container](https://github.com/lfdevs/mesa-for-android-container)
  — the optional Linux-container Mesa/Turnip package builds used here.
- [virglrenderer](https://gitlab.freedesktop.org/virgl/virglrenderer) and
  [ANGLE](https://chromium.googlesource.com/angle/angle) — renderer/translation
  projects, distributed for the Android server through Termux packages.
- [leegao/bionic-vulkan-wrapper](https://github.com/leegao/bionic-vulkan-wrapper),
  [bylaws/libadrenotools](https://github.com/bylaws/libadrenotools) and
  [sabamdarif/termux-desktop](https://github.com/sabamdarif/termux-desktop/releases/tag/bionic-vulkan-wrapper-0.0.5r5)
  — wrapper code, driver-loading work and the pinned prebuilt package.
- [Cisco OpenH264](https://github.com/cisco/openh264/tree/v2.6.0), packaged by
  [Debian](https://sources.debian.org/src/openh264/2.6.0%2Bdfsg-2/), and
  [xkeyboard-config](https://gitlab.freedesktop.org/xkeyboard-config/xkeyboard-config)
  — codec and keyboard data, with their notices included in the APK.

Detailed versions, source/build links, provenance, licenses and what is embedded
versus downloaded are documented in
[Sources and acknowledgements](https://github.com/HezeAI/termux-jobs/blob/v2.0.0/docs/fork/THIRD-PARTY.md).

[Usage](https://github.com/HezeAI/termux-jobs/blob/v2.0.0/docs/fork/USAGE.md) ·
[X11 integration](https://github.com/HezeAI/termux-jobs/blob/v2.0.0/docs/fork/X11-INTEGRATION.md) ·
[Graphics](https://github.com/HezeAI/termux-jobs/blob/v2.0.0/docs/fork/X11-GPU.md) ·
[Validation](https://github.com/HezeAI/termux-jobs/blob/v2.0.0/docs/fork/VALIDATION-v2.0.0.md)
