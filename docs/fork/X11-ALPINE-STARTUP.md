# Alpine X11 startup finding · 2026-10-21

Developer screenshots 081849/081855/081857 from v1.7.35 show WINDOW_MANAGER=ready,
then Gtk pixbuf warnings, libwnck default_icon_at_size assertion(base), Aborted and
DESKTOP_CHILD_EXIT=134. X11 stops automatically and the terminal continues: lifecycle
recovery works in this observed failure. This is distinct from prior stale started.

Read-only installed-rootfs inspection: Alpine has gdk-pixbuf2.44.7-r1,
libglycin/glycin-image-rs/glycin-svg2.1.5-r0, bubblewrap0.12, adwaita-icon-theme50,
shared-mime-info2.4, libwnck43.3. MIME cache/theme indices exist. loaders.cache has
legacy XPM only; do NOT call that corruption: GdkPixbuf2.44 can include builtin Glycin
handlers for PNG/SVG. Installed components alone do not prove decoding works.

Upstream libwnck43.3 xutils.c1504+ calls gdk_pixbuf_new_from_resource on its own
/org/gnome/libwnck/default_icon.png, then asserts base. Thus the shown fatal failure
is in decoding/loading even a built-in icon, not proof of missing wallpaper.
https://github.com/GNOME/libwnck/blob/43.3/libwnck/xutils.c

GLib2.88.1 glocalfile.c1288+ uses faccessat with AT_EACCESS|AT_SYMLINK_NOFOLLOW;
EINVAL prints the exact developer warning and query_exists returns false. Need
separate PRoot/musl compatibility validation. Do not suppress all GLib logging or
assume these warnings alone caused the crash.
https://github.com/GNOME/glib/blob/2.88.1/gio/glocalfile.c

Glycin sandbox/bubblewrap compatibility in PRoot is a hypothesis, not yet confirmed.
No installed rootfs/process/package was modified or launched for this investigation.
Next minimal checks: isolated PNG/resource decoding with actual loader error; validate
filesystem existence semantics; only then choose an Alpine-scoped fix. Keep Ubuntu,
Debian, Termux and application security settings unchanged. Do not globally disable
sandboxes or downgrade repository packages as an unverified workaround.

Next UI update agreed: compact X11 emblem, title «Запуск рабочего стола», stage-based
fill bar, current startup step below. Real milestones, no fictitious duration/percent.

## v1.7.36 correction and evidence

Both failures reproduced using copied real Alpine libraries/binaries under a fresh
repo-local PRoot tree; no installed rootfs was launched or modified. g_file_query_exists
returned false for an existing PNG (EINVAL on the exact flags). Ordinary GdkPixbuf PNG
loading failed because the Glycin Bubblewrap process exited. gdk-pixbuf-csource worked
because upstream intentionally disables its sandbox for build tools: that initial
success alone was not sufficient evidence for desktop applications.

The Alpine-only module interposes faccessat, falling back ONLY after EINVAL on F_OK
with AT_EACCESS|AT_SYMLINK_NOFOLLOW. fstatat retains dangling-link/existence semantics;
read/write/effective-UID access modes and every other flag/error remain untouched.
The Glycin2 loader constructor selects its documented NotSandboxed enum. Lookup uses
an existing exact libglycin2 handle, so RTLD_LOCAL Python/GI/plugins are supported.
This removes decoder isolation within Alpine desktop processes: malicious-image bugs
could access files available to those processes. Android permissions and browser
sandbox flags are unchanged. Do not describe this as a security-neutral change.

After correction, real copied libraries successfully report existing files and decode
PNG, SVG and /org/gnome/libwnck/default_icon.png. Source remains in our repo; pinned
musl headers are extracted only to .tools for building, never installed. Helper is
packaged as an asset (not Android JNI) and enabled by an Alpine-only launch flag.
A complete live XFCE/keyboard/browser/stop smoke test remains pending developer checks.
