# Private test5: Kali Evince and Manjaro LibreOffice

## Device findings

Kali's installed Evince49~alpha.1-2 uses GTK4.24.1/libadwaita. Its black window
was reported while the Kali desktop used Bionic for glibc/Zink. The developer
confirmed that `GSK_RENDERER=cairo evince` opens the interface normally. This
confirms the per-app workaround; the precise GPU backend/presentation defect
inside GTK/driver integration was not independently established.

Manjaro's installed LibreOffice24.2 had its core libraries and executable bits.
A read-only ELF/dependency inspection and a headless version probe found no
missing core library/version requirement. Optional Qt/KDE/GTK4 plug-ins have
uninstalled dependencies; those are not the observed startup failure.
The developer's terminal screenshot shows the actual failure: `sed` is absent,
so LibreOffice's symlink-resolution shell code computes `/usr/sbin` as its
program directory and cannot find `oosplash`. The developer confirmed that
`/usr/lib/libreoffice/program/soffice --writer` opens Writer normally, without
a forced GTK plug-in.

## Implementation and lifecycle

A small owned application-launcher directory is prepared inside each selected
private X11 control directory, only for Kali and Manjaro. It is added to that
desktop's PATH; there is no global GTK, GPU or shell-profile setting.
Kali's `evince` command sets Cairo only for that process and executes its distro
binary. Manjaro's `libreoffice`/`soffice` commands use the canonical installed
shell script, avoiding symlink resolution when repairing an existing install.
The next normal desktop start enables this for already-installed programs.

An empty launcher directory is available in the desktop PATH before optional
apps are installed. Successful explicit application installation/removal
refreshes the same directory, so a fresh app install during an active desktop
receives the workaround. Missing applications do not get executable launchers.
Manjaro's recommended Office group and `pkg:libreoffice-fresh` both request
`sed` explicitly. Read-only catalog/status checks do not prepare launchers or
install dependencies. Other environments remain on their existing paths.

The helper uses an owned lock, no-follow regular-file checks and atomic writes.
Edited launchers, hard links, symlinks and unexpected executables are rejected
and preserved. Only exact owned launcher contents are removed on uninstall.
System executables, package .desktop entries, documents, user profiles and
distribution images remain unchanged. No environment reinstall/data reset is
introduced, and the Components archives do not need replacement for these fixes.
Direct absolute executable paths bypass the private PATH launcher intentionally.

## Validation boundary

Developer device confirmation above used manual commands, before installation
of this APK. It is not a claim of a fresh-phone installation or a completed
post-update menu/PDF-association test. Those checks remain for the developer.
No nested PRoot graphical fixture is resumed after the earlier ptrace assertion.
Automated tests, build and artifact results are recorded below when completed.

Primary references: [GTK renderer selection](https://gnome.pages.gitlab.gnome.org/gtk/gtk4/running.html#GSK_RENDERER)
and [LibreOffice soffice launcher](https://git.libreoffice.org/core/+/refs/heads/master/desktop/scripts/soffice.sh).
The path-resolution test reproduces the installed LibreOffice shell fragment;
its source includes MPL2.0 and Apache2.0 notices.

## Separate Manjaro GPU work

The installed Manjaro base has glibc2.35/GCC12.1 runtime. Existing private
Turnip26.2.5/bridge components declare newer glibc/C++ requirements. They are
not enabled by this application workaround. Adding those renderers requires
a separately built and verified compatible runtime; it must not silently
upgrade the user's system libc or change other environments' working components.

## Automated source verification

51 focused tests passed in 25.952 seconds. The real LibreOffice shell
path-resolution fragment fails through a symlink with sed absent and succeeds
through the generated canonical-path launcher. Generated launchers are executed
with spaced filenames and command-like arguments; per-process Cairo does not
change parent GPU settings. Lifecycle cases include absence, installation while
the desktop directory is already in PATH, repeat preparation and removal.
Symlinks, hard links, user edits, unmanaged executables and mismatched CLI
environment requests are preserved/rejected. Package tests cover Manjaro office
and direct libreoffice-fresh installs requesting sed; other native targets
retain their existing package groups. A legacy expected-target list was updated
to include the intentionally added Manjaro environment.

## Built private candidate

VersionName `2.0.10-test5`, versionCode 1436, package
`com.termux`; 39833415 bytes, ARM64 only, non-debuggable.
SHA-256 `c027695fd3253c795ec3d5d1ebd8e3dd52f560a0c1c502d01af9525c4d79dbb4`.
Stable signing certificate SHA-256
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` retained.

Release assembly passed in 1m26s. Independent lint7.4.2 analyzed all five
modules with zero errors, 223 existing warnings and no infrastructure diagnostics.
APK integrity, signature, required runtime classes, all 86 source assets,
generated runtime assets and secret-pattern checks passed. All three Android
JNI libraries match the retained rollback byte for byte. Legacy AGP4 lint
detector incompatibility is superseded by the independent modern lint result.
Workspace schema8/package identity is unchanged and no data reset is introduced.
This is a private candidate; no application GitHub release is published.

Developer's minimal follow-up: update APK, restart each affected desktop, then
open Evince/Writer from their menus and open a PDF/document. The manual commands
were confirmed, but this post-update GUI/association flow still needs checking.
