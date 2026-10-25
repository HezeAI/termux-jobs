> Public v2.0.5: Wayland/KDE and associated driver controls are disabled and gray.
> The following describes private preview development. Enable only when building
> an explicitly private APK with `-PtermuxWaylandPreview=true`. Retained settings
> and driver imports are preserved; the public UI selects X11/XFCE instead.

# Experimental direct Arch Wayland desktop

This private candidate adds a second desktop profile to the existing Arch rootfs:
**Wayland · KDE Plasma**, alongside the existing **X11 · XFCE**. It does not add
another distribution, Wine, FEX, Steam or Venus.

Files, packages and the terminal are shared by the two Arch profiles. KDE uses
private configuration/cache/state directories. Only one Arch desktop profile is
allowed at a time; a shared process lock also covers transitions between profiles.
Other environments retain their own services and X11 rendering settings.

## Rendering and startup

An app-private Android service hosts the native Wayland compositor derived from
[DroidDeck](https://github.com/Droid-Deck/DroidDeck), pinned at
`22eac0831d10671f6bc2ad914a93a0da5046c7df` (0.6.0). Android presentation uses a
user-imported **Bionic Driver · AdrenoTools** ZIP from a catalog separate from
X11's catalog. Use a compatible ARM64 Bionic Turnip driver for the first test.
Bionic selection and ZIP import are available before KDE is installed or its
service is connected. A running desktop or active operation still locks changes.
Imported drivers execute with the app's privileges; import trusted code only.

KWin and KDE run in the existing Arch rootfs using the pinned **Lfdevs Linux
Turnip + Zink, Mesa 26.3.0-devel** build already used for Arch X11. The Linux Driver
row is disabled in this candidate. The two driver roles are independent: Android
presents the buffers; the Linux driver renders KDE and guest programs.

A process-local PRoot device view, KGSL identity and a guest preload derived from
DroidDeck expose the render device needed by stock KWin. This does not replace
Android's kernel, change system devices globally, or install a global preload.
The native compositor listens on a private Unix socket, not a public TCP port.

The component check requires the explicit package/binary/font/certificate/runtime
closure in `wayland-components.sh`. Consent to KDE installation runs a normal
**full Arch package upgrade** (`pacman -Syu --needed`); no partial upgrade or
forced file overwrite. An APK rollback does **not** undo those package changes.
The v2.0.4 checkpoint protects the APK/source, not a rootfs snapshot. Back up Arch
before testing if its current package state must be recoverable.

Select Arch's Wayland profile in its desktop menu, complete the component setup,
import/select a compatible Bionic Turnip ZIP, then start the desktop. Profile and
driver changes require the active desktop/operation to stop. Returning to the
terminal or another tab hides the host without stopping KDE. Stop or closing the
final owner ends only its guarded process group and dedicated service VM.

## Jobs adaptation and verification

The original source snapshot and hash inventory are in
`third-party/droiddeck-wayland/`; `scripts/prepare-wayland.py` applies Jobs patches
to an isolated build copy. Changes include private SurfaceControl embedding,
Android keyboard/extra-key integration, surface lifecycle handling, startup/error
states, imported-driver locks and hash checks, exact KGSL feedback identity,
no production test-input FIFO, and removal of upstream automatic shared-storage
session logs and payload logging. Runtime inputs are pinned by
`x11/wayland-inputs.json`; Gradle rejects missing or changed native inputs.

Repo-private hardware probes verified Linux Turnip DMA-BUF export, delivery to
the Android compositor, the expected pixel on an Android ImageReader surface,
matching render-device feedback, the shipped keyboard map and key events. A
separate Arch-library probe verified hardware GBM/EGL/OpenGL and buffer FD export.
Invalid Bionic drivers fail without opening a misleading ready socket. Probe
children are started and cleaned up by exact ownership; installed desktops,
agent sessions and services are not part of those probes.

**KDE GUI startup on the installed candidate remains unverified.** These probes
are not a substitute for a fresh Arch installation and actual KDE checks. Test
startup, errors, stop/restart, return/tab navigation, landscape, pointer, Android
IME, extra keys and Xwayland programs. The initial synthesized keyboard path
covers English/Russian; other scripts and Xwayland IME behavior need device
verification. The native presentation callback is limited to an average 60 fps
while its Android host is visible. Runtime directories are private; stale runtime
metadata after abnormal process death is not currently garbage-collected.

## Startup diagnostics (private test.8)

HezeAI reported that test.7's Plasma launch ends before desktop readiness. Its
exported timeline shows parent compositor readiness followed by guest startup,
a permission-denied category, then child exit 0. This does not identify the
original cause: the old report omitted raw errors and the UI kept only a tail.
Systemd activation warnings alone are not evidence that classic boot is disabled.

The Wayland-only startup collector now retains up to eight first and eight recent
redacted failure lines. It excludes ordinary output and input-context frames;
URLs, quoted values, paths and credential-like values use the existing transfer
failure redactor. Redaction is best effort: review reports before sharing. This
is bounded launch logging, not a system log, terminal transcript or background
recorder. A new launch replaces the collection. Fixed KWin-wrapper stage markers
and selected KDE/KWin startup logging help locate the failing transition. Reports
and end messages identify Wayland rather than calling this profile X11. A Plasma
exit before readiness is an explicit startup failure, including child exit 0;
normal ready-session exits retain their normal end state.

This diagnostic follow-up does not claim to fix Plasma startup. Fresh installed
KDE GUI startup, input and stop/restart still need device verification. It does
not change imported drivers, native compositor code or the existing X11 profiles.

Pending follow-up requested by the developer, after resolving Wayland startup:
add **X11 surface format: Auto / BGRA / RGBA**. Auto must preserve the current
format-selection mechanism. This is not part of test.8; the native X11 surface,
CPU/GPU import paths and both wrappers need compatibility verification first.

## Private test.9: temporary runtime and Qt output

KDE's Qt messages are now forced to the existing stderr pipe with
`QT_FORCE_STDERR_LOGGING=1` and `QT_ASSUME_STDERR_HAS_CONSOLE=1`, scoped to the
Arch Plasma process tree. Selecting logging categories alone did not reliably
route those messages into the app's captured pipe. See the
[KDE logging guide](https://community.kde.org/Guidelines_and_HOWTOs/Debugging/Using_Error_Messages).

Before a Wayland run, preparation creates a short private temporary directory
(`wt-` plus 16 UUID hex digits) in the selected control directory, with
`.X11-unix` and `.ICE-unix` socket directories (1777), rejecting symlinks. A fixed
owner marker prevents adopting a colliding launch. Paths are checked against the
Unix socket limit; placing these sockets below the full runtime UUID is too long.
That directory is bound as the selected guest's `/tmp`, alongside the existing
control/audio sub-bind. `TMPDIR=/tmp` is explicit. Check/install operations keep
their original mounts. No global Termux tmp, rootfs tmp permissions, Ubuntu,
other X11 service or installed application configuration is changed. This fixes
a fresh-runtime prerequisite observed in an isolated KWin-wrapper test, where
absent `.X11-unix` prevented Xwayland connection socket creation.

The developer's second test.8 report confirms a premature Plasma exit after
roughly 21 seconds. FUSE failure belongs to the document portal and is not proof
of the primary compositor failure. That report did not contain KWin's Qt error
messages. Full phone startup is still unresolved; test.9 is not a verified fix
for the reported stop. X11 Auto/BGRA/RGBA remains queued after Wayland startup.

## Private test.10: KWin GPU timing workaround

A bounded, repository-isolated run reproduced a SIGTRAP in Linux
`libvulkan_freedreno.so`, called through Zink from
`KWin::GLRenderTimeQuery` / `glGetInteger64v(GL_TIMESTAMP)` during the first
composition. This is more specific evidence than the secondary Qt shell-plugin
failure seen in the installed-app report. Qt's xdg/layer-shell plugins loaded
successfully in separate checks.

The generated launch shim now exports **KWIN_NO_TIMER_QUERY=1** only before
executing the private KWin wrapper. This uses KWin's existing capability switch;
it does not replace GPU rendering, change native binaries, disable compositing,
alter X11 renderers or modify installed guest/system configuration. See
[KWin's timer-query capability check](https://github.com/KDE/kwin/blob/v6.8.1/src/opengl/eglcontext.cpp)
and [the GL timestamp call](https://github.com/KDE/kwin/blob/v6.8.1/src/opengl/glrendertimequery.cpp).
The switch suppresses GPU-duration measurements used by KWin's frame scheduling;
performance and complete Plasma GUI behavior still require phone checks.

With this switch, the isolated KWin exposed `xdg_wm_base`, `wl_output` and
`zwlr_layer_shell_v1`; a Qt client connected and exited normally. The observed
native parent frame does not establish a complete usable Plasma desktop.
Full-session startup is assessed separately in the
[test.10 validation record](VALIDATION-WAYLAND-TIMER.md).
