# Private Garcon attribute filter

Garcon4.20.0, LGPL-2.0-or-later. Original source/hash and Ubuntu build-only header
inputs are pinned in provenance.json. No build-only dependency is installed.

Run `python3 scripts/prepare-garcon.py` on ARM64 glibc with a C compiler and the
GLib/GIO runtime. It extracts sources/development headers under .tools/garcon-build
and compiles .tools/x11-runtime/assets/libgarcon-1.so.0. No RPATH or bundled
replacement GLib/xfce4util. Source archive, patch, guard sources and these build
instructions are included in assets/x11-garcon-source.zip.

Patch calls the guard inside garcon_menu_app_dir_changed while the original lock
is held. Only duplicate attribute events for stable regular .desktop files can
be suppressed. Unknown first events, other events, permission/ownership/identity/
size/mtime/content changes, races, symlinks and files above256KiB pass through.
Cache has at most512 entries, no stored file contents; every candidate gets a bounded SHA256 read, even when stat metadata is equal;
atime is ignored. At capacity, the cache is reset. Failure is open.
The environment opt-in defaults off in the library. The app enables the private
path only for Test XFCE, exact Garcon4.20.0 package version, and exposes a stopped-
desktop toggle to compare against the system library. Existing GTK diagnostic
guard counters are process-lifetime totals, independent of recording resets.

The compile preserves upstream public API and SONAME. License and original
copyright notices remain in the source. This is a trial mitigation; the writer
of the underlying attribute changes has not been identified.

With the toggle off (v1.7.27+), the loader uses the installed system library and
snapshot diagnostics are disabled. A direct call to the private guard with its
opt-in off also returns before path/stat/hash work, even while recording. The
older v1.7.26 diagnostic baseline was not clean: full reads ran with suppression
off and could amplify a notification storm. Lightweight GIO counts remain in
the independent GTK observer. No installed library is modified.

Cache keys include the menu consumer. A notification first seen by one menu
never suppresses another menu's first delivery. Weak notifications remove keys
on consumer destruction, preventing reused object addresses from inheriting
state. The global512-entry ceiling applies across all consumers. NODELETE keeps
registered callbacks/diagnostic pointers valid until process exit.
