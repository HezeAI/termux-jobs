# GTK observation · private1.7.24-x11-test / code1239

Stable public remains v1.7.5. Same com.termux package/certificate, code1238 ->1239.
No data reset, environment migration, removed history or replaced configuration.
Restart the graphical desktop after updating to load this module; existing running
processes are not injected/modified. No GitHub publication requested.

## Evidence and scope

Developer report4440210790194811352 showed persistent damage on a stable panel
menu, including t0..11 without sampled pointer/root lifecycle changes. XDamage
is not pixel comparison or a measured GPU frame rate and cannot name the caller.
This version instruments GTK menu draw, item add/remove, Garcon reload request
and pointer crossing/motion. The ordinary Ubuntu launch explicitly passes flag0;
only jobs-x11-ubuntu passes1. Module also checks its opt-in and program whitelist.
No ordinary session, agent transport/chat/permissions, package system or other
distribution logic changes. Native X11 renderer and compositor are unchanged.

The shared-library asset is loaded by new owned test desktop processes only;
it does not replace installed GTK/Garcon. Metadata excludes widget labels, event
payloads, keys, image contents and pointer coordinates. Hooks never claim/consume
a GTK signal. The normal Garcon callback is attached once per observed core menu
without retaining that object. Counters use atomics for possible cross-thread
signal delivery. New records reject earlier capture serials.
GModule residency retains callback code until process exit; a one-second source
polls the fixed private control even while recording is off. This small idle
cost is explicit. Stop propagation to the GTK counters can lag by one second.
Existing guardian and exact private-process shutdown are unchanged.

## Device checks needed

Restart Ubuntu X11 Test desktop. Start observation, open Applications, leave the
pointer stationary while flickering for10–15seconds, stop observation, wait briefly
and export. Look for source=panel/desktop, hooks mask and core-hooks, then compare
draw with add/remove/reload and pointer counters. Missing GTK rows or absent hook
coverage means that layer is not measured; zero reload alone is not conclusive.
The module does not establish that GTK caused the flicker and does not fix it.
Real GTK rendering, gestures, settings preservation and record stop timing need
phone validation. A mocked widget is not a test of the installed GTK/desktop.

## Primary source review

GLib signal/query/emission hook ABI checked against GNOME/glib2.84.0 gsignal.h.
GTK3.24.52 module loader and widget signals checked against GNOME/gtk sources.
Garcon4.20.0 menu property and reload-required (NO_HOOKS) inspected from official
xfce-mirror source snapshots. Copies and URLs retained under .verification
x11-gtk-observation and x11-garcon-review.

## Automated evidence

31 JVM/Robolectric tests passed, including stale GTK capture rejection, existing
private process/owner guards and desktop diagnostics, plus atomic module publication
that leaves a previously open inode unchanged and rejects temporary symbolic links.
69 Python/shell/native tests passed. Five native module tests include opt-in gate,
control on/off/restart/replay/invalid identity, metadata-only counting, missing GTK
safe return, real GLib/GObject emission-hook ABI and normal NO_HOOKS reload signal
connection, handler propagation, and real GModule residency after reference close.
GTK widgets in the GObject probe are simulated; real GLib types/GValues/signals,
timers and GModule are used. This does not establish installed GTK drawing behavior.
Routing fixture executes the run command for ordinary Ubuntu and Test without
starting any real guest and confirms only Test gets flag1. Previous controller,
XDamage, wallpaper, catalog, guardian and environment checks also passed.

Build completed successfully. Existing Kotlin metadata/Lint loader warnings remain;
a clean Lint run is not claimed. Packaged DEX:13,369 defined classes, required
definitions present,31 reviewed optional types and no unreviewed missing types.
Evidence: .verification/x11-gtk-observation/{script-tests,jvm-final-tests,build}.log.

## Copy/lifetime review

The module asset is copied through AtomicFile, with destination/.new/.bak symlink
checks. It is never truncated in place while an earlier desktop may map its inode.
A running desktop keeps its prior code mapping; restart loads the new module.
No working process is injected, adopted or restarted by preparation/build.
GModule pins callback code until process exit. Existing guardian owns shutdown.
No installed guest configuration/package, APK or running terminal was modified.

The glibc asset requires GLIBC2.34 or newer (verified version references2.17/2.34),
suitable for the current Test Ubuntu. Older custom guests are not validated; the
ordinary Ubuntu path does not enable the module. The asset is not an Android JNI
library and does not introduce another Android native library or JNI declaration.

Final artifact audit follows below.


## Final artifact

APK Termux-Jobs-v1.7.24-x11-test-arm64.apk, 38987176 bytes. ZIP integrity and duplicate-member check,
com.termux/version1.7.24-x11-test/code1239, ARM64/minSdk26/non-debuggable manifest,
v2 signature and unchanged signing certificate verified.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK SHA-256: `3f6f09a72a58ebe14913c231e260a313ef7aa52f4d338c687907c38638d7ab7e`.
Guest module SHA-256: `22c98f9a30125b823c519d48207ca503adfa7d7d102781d8e628b27f95750369`.
Android native baseline and pinned X11 native/keyboard inputs unchanged, JNI
contracts28/1/5 verified. New .so appears only as an asset and matches the final
compiled module. Known secret-marker and sensitive-filename scan passed; not proof
of absence of every possible secret pattern. Download copy hash verified.
Audit: .verification/x11-gtk-observation/apk-audit.json and signature.txt.
