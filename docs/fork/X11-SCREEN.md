# Current behavior — v2.0.7

Live scale restoration supports original unset scalar properties, including DPI and cursor size, without clearing unrelated settings. Without IME, the measured portrait surface is the full host; navigation gaps are not treated as a keyboard crop. With IME, the prior full host remains stable while visible height changes. Rotation follows the existing presentation path; no experimental retention, compositor or direct-buffer mode is added.

---

# X11 screen settings

Each supported X11 environment has an **X11 screen** entry in its desktop menu.
**Auto setup** restores automatic resolution, 100% interface scale and zero
margins together. While checked, width, height, scale and both margin inputs are
disabled and dimmed. The resolution inputs display Auto rather than a fixed size.
The checkmark changes the form; **Apply** sends the defaults through the existing
live-update acknowledgement path. Cancel leaves stored/running settings unchanged.
Older automatic-resolution settings with a custom scale or margin open in manual
mode rather than being reset silently. No saved settings are migrated on app launch.

Disabling **Auto setup** prefills **1600 × 720**, **75%** scale and **5 dp**
horizontal/vertical margins on each side. These are editable form values, saved
only by Apply. Reopening an already saved manual mode preserves its own values.
Auto setup itself still restores Auto/100%/zero margins.
For example,
1300 × 600 is accepted as input; the X11 server's CVT mode uses **1296 × 600**
because horizontal dimensions are aligned down to eight pixels. In portrait,
that pair becomes 600 × 1300. The physical phone orientation controls this rotation;
showing the keyboard cannot accidentally swap the dimensions. The configured
landscape width is displayed with native alignment in the menu. Limits: width 320–4096, height 200–4096, at most
8,388,608 pixels. Manual mode follows the phone orientation while preserving its size pair across
tab changes and keyboard visibility. It stretches to fill the entire available area in both the embedded view and
landscape full screen, at every interface scale. Only explicitly chosen margins
reserve space; a different framebuffer aspect ratio can stretch shapes. This
replaces test.2/test.3 aspect fitting that left black strips around manual modes.

Keyboard visibility also preserves the full Android host geometry in manual
mode, as it does in Auto. The keyboard obscures the lower part of the desktop;
it does not squeeze the entire custom-resolution image into the remaining height.
This applies to embedded portrait and landscape presentation. HezeAI confirms
the keyboard correction on the phone. Rotation, margins and alternate-device
regressions remain part of subsequent device checks.

Horizontal and vertical margins are specified in Android dp **on each side**,
from 0 to 96. They reserve real space for rounded corners and cutouts, independently
of the framebuffer resolution. On small windows each margin is capped to a
quarter of that window dimension. They apply in embedded and landscape full-screen
views. Automatic mode still follows the available presentation area.

**Interface scale**, below resolution, accepts 50–150%. Try 75% for 1300 × 600.
It adjusts XFCE font DPI, desktop icons, cursor and panel pixel sizes when **Apply** succeeds. The same percentage also scales new Thunar window width/height and
sidebar width, folder-view zoom and sidebar icons. Thunar supports discrete icon
sizes: the nearest supported size is selected. Maximized windows still fill their
workspace; other applications may remember their own window geometry. It does not change the framebuffer resolution or browser page zoom;
applications with their own fixed sizes may need their own controls. Resolution,
margins and scale are applied by the same Apply button. If X11 is stopped, these
settings are saved for its next launch. While applying to a running desktop, the
button waits for an acknowledgement and keeps the previous preferences on failure.
Some applications cache their view zoom: already open Thunar folder icons may need
re-entering the folder or reopening its window. Folder-specific zoom overrides are
preserved. Normal open Thunar windows are resized proportionally; maximized, hidden
and full-screen windows are left to the window manager.

For PRoot desktops, the private session-bus configuration now includes the distro’s session policy and allows the actual Android application UID with ordinary EXTERNAL authentication. The separate settings invocation can therefore reach that environment’s owned Xfconf service. Startup validates the target and launch token; the private config is atomic and mode0600. No installed system policy or other environment is modified. Native Termux keeps its original working path. HezeAI confirmed the live Arch correction; isolated real settings-bus fixtures passed75%/50%/100% in Ubuntu, Debian, Alpine, Fedora, openSUSE, Void and Arch. Full GUI coverage on every device remains separate.

The scale overlay records original properties in the selected environment's HOME.
A live update uses the selected desktop’s already running private Xfconf service;
it never edits XML underneath xfconfd or restarts applications. Startup restores the previous overlay before applying/removing the optional Jobs
theme, then applies the selected scale. Repeated starts therefore do not multiply
scaling. Returning to 100% restores unmodified original properties; later manual
edits are preserved. XML outside HOME, symlinks and malformed input are rejected.
No environment scan, background scale daemon, system files or agent rootfs changes
are involved. Defaults are Auto/100%/zero margins. Before rolling back to an older
APK that lacks scale restoration, select 100% here and restart X11 once.

Settings persist per environment, so two sessions of the same environment use
the same screen settings. Applying settings to a visible X11 desktop relayouts
its Android surface and sends a bounded logical mode to that environment's
private service; it does not restart Linux, alter Android display settings or
change another environment's renderer. Invalid input leaves the dialog open
without saving. Cancel preserves the previous settings. Missing or malformed
stored geometry falls back to automatic mode.

The additional-key panel is independent of XFCE interface scale. Its compact
default size is 400 × 104 dp, clamped to the visible host; the left tab resizes it
and the right tab moves it. All keys reflow without losing held modifier state.

These controls apply to X11/XFCE. The Wayland/KDE experiment has been retired.
No workspace schema or
rootfs migration is required; old APKs ignore the new preference keys.

Automated tests cover geometry, native alignment, bounds, default behavior,
per-environment preferences, invalid input and UI save/cancel contracts.
Actual SurfaceControl rendering, pointer targeting near the reserved edges,
rotation and IME transitions require the developer's phone checks.

X11 automatic keyboard requests are now gated by the active window, normal WM
state, window type, Fcitx capability flags and matching client process identity.
Desktop/dock/hidden windows and disabled input contexts cannot automatically open
the keyboard. Window switches invalidate an old bound context. Manual input remains
available; if a client omits required metadata or the helper fails, use the manual
button. There is no polling timer: the helper waits for selected X11 property
changes and narrowly matched private-bus Fcitx calls; a focus request has one short
settling deadline. No titles, text, surrounding text, keys or clipboard are read.
Desktop rename/type-ahead contexts without a normal application window require
manual input.

X11 driver choice/import is available before desktop
installation or service binding. The choice itself does not install or launch a
desktop. Active desktop, setup and driver-library operations keep their locks.

Scale property references: [Thunar preferences](https://github.com/xfce-mirror/thunar/blob/master/thunar/thunar-preferences.c),
[Thunar enums](https://github.com/xfce-mirror/thunar/blob/master/thunar/thunar-enum-types.c).
Keyboard metadata references: [Fcitx input-context methods](https://github.com/fcitx/fcitx5/blob/master/src/frontend/dbusfrontend/dbusfrontend.cpp),
[Fcitx capabilities](https://github.com/fcitx/fcitx5/blob/master/src/lib/fcitx-utils/capabilityflags.h).

Live scaling is a bounded one-shot job, without a persistent scale daemon. The
selected environment records its private bus address and launch nonce in its
private control directory; a new process in that same environment validates them
before applying allowlisted properties. It has its own guardian and deadline,
separate from the desktop child. A small recovery journal restores interrupted
changes on the next launch and preserves later manual edits. Applying scale does
not install packages or create a new desktop/bus.

The immediate-update mechanism uses the public [Xfconf channel API](https://github.com/xfce-mirror/xfconf/blob/master/xfconf/xfconf-channel.h).
Thunar's existing [view-zoom binding](https://github.com/xfce-mirror/thunar/blob/master/thunar/thunar-standard-view.c)
explains why already displayed folder icons can require a folder/window refresh.
