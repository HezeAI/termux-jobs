# v2.0.5 — X11 screen controls and input fixes

Configure each X11 desktop's resolution, interface scale and safe margins from
**Terminal / Desktop → X11 screen**. A fixed resolution follows phone rotation
and fills the embedded or landscape view, apart from the selected margins.
**Auto setup** restores automatic sizing, 100% scale and zero margins together.
Disabling it prefills **1600×720, 75% scale and 5 dp margins**. Existing saved
manual values are preserved when reopening the dialog. Apply saves the changes;
Cancel leaves the current settings unchanged.

Interface scaling also covers XFCE panels, fonts, desktop/cursor icons and new
Thunar windows, folder/sidebar icons and sidebar width. Apply updates the running
desktop through its own configuration service; some open programs/views may need
to be reopened. No persistent scale daemon is added.

Automatic keyboard requests now check the active application's window and Fcitx
input context. Hidden, minimized, desktop and dock windows no longer qualify as
normal text fields. If an application does not provide the required metadata,
use the existing manual **Interactive input |** button.

The floating extra-key panel is more compact: **400×104 dp** initially, smaller
keys and move/resize handles, with the existing Esc, Enter, arrows and modifiers.
Keys reflow when resized and held modifiers release when the panel closes or
the desktop changes.

**Wayland / KDE Plasma and its driver controls are gray and disabled in this
public build while testing continues.** Saved preview settings/imports are kept;
Arch uses X11/XFCE in this release. This is enforced in both the UI and backend.
X11 CPU, Turnip + Zink, VirGL and supported Bionic wrappers remain available.

### Compatibility and validation

Same **com.termux**, signing identity, ARM64, schema8 and Android requirements:
Android8+ for the app; Android11+ for embedded desktops. No valid-data reset,
account migration or environment reinstallation is required. Existing commands,
presets, agent accounts and imported drivers are retained. Finish important work
before installing: Android can stop application processes during an update.

Version code **1376** is above public v2.0.4/code1366. It intentionally equals the
developer's existing private test.10 code, so that test build is not offered this
public build as a newer in-app update. The private APK remains separate.
Selecting an older APK cannot undo Linux packages already upgraded during an
experimental KDE installation. Existing Bionic-driver/SDK/file-access limitations
from v2.0.4 remain.

The developer checked the preceding X11 candidates. The public Wayland-disable
change is covered by automated UI/backend checks; fresh-device GUI, uncommon
applications and all device/driver combinations are not exhaustively verified.
See [validation](https://github.com/HezeAI/termux-jobs/blob/v2.0.5/docs/fork/VALIDATION-v2.0.5.md),
[screen controls](https://github.com/HezeAI/termux-jobs/blob/v2.0.5/docs/fork/X11-SCREEN.md)
and [source attribution](https://github.com/HezeAI/termux-jobs/blob/v2.0.5/docs/fork/THIRD-PARTY.md).

AI development assistance: OpenAI Codex
