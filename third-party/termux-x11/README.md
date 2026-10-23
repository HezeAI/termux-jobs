# v2.0 integration status

The native library is the unmodified pinned upstream Termux:X11 core described
below. Java adapters now route eight local desktop targets to separate private
X11 processes/displays, rather than the initial single-process experiment.
Original source, license and hashes are retained; this is not a claim of a native
rebuild. Current behaviour and limits: ../../docs/fork/X11-INTEGRATION.md and
../../docs/fork/THIRD-PARTY.md. The following first-integration notes are historical.

# Termux:X11 dependency — first integration experiment

Upstream: https://github.com/termux/termux-x11
Pinned source/JNI ABI: fa3a8b430e2896a19f44c99a9cb056254615ae06.
Licence: GPL-3.0; original licence is in LICENSE and in the APK assets.

For this private test we reuse the ARM64 libXlorie.so from the official nightly
APK rather than claiming to have rebuilt Xorg's native dependencies on-device.
The APK SHA-256 is checked against the GitHub asset digest and provenance.json;
the full source commit embedded in its DEX must match. Java/Android adapters in
x11/ are built by this repository. JNI declarations retain upstream names/types,
including CriticalNative/FastNative; the final compiled descriptors are audited.

Keyboard data comes from the pinned Termux xkeyboard-config package, not from
an installed user environment. It is unpacked into a bounded deterministic ZIP
without installing the DEB. HPND/MIT copyright notices are included inside it.

Run `python3 scripts/prepare-x11.py` before a conventional Gradle build. Existing
ARM64/Java verification scripts do this automatically. Inputs/cache stay in
.tools/; only the verified library, keyboard ZIP and notices enter the APK.
No system package, shared SDK or installed environment is changed. The library
Gradle task refuses missing/modified inputs. Nightly URLs are mutable: a future
upstream change will fail closed against the pin; it must never silently update.

The original shell-loader, APK identity, exported receivers, accessibility
service and preference broadcasts are not imported. The host keeps its own
Application class. Both private components run in :jobs_x11. This stage exposes
one private, authenticated display :97 and a minimal X11 protocol test window.
Native source and submodule references for any future distributable rebuild are
available at the pinned upstream commit; do not describe this cached binary as
our own native rebuild or release a source-incomplete native modification.

Security adaptation: upstream's discovery TCP listener binds INADDR_ANY even
with -nolisten tcp. The adapter reserves tagged port7997 using an exclusively
bound, non-listening loopback socket for the entire server lifetime; the pinned
core takes its existing no-listener fallback. There is no discovery broadcast
or external binder. The actual display uses a private filesystem Unix socket
and a fresh MIT-MAGIC-COOKIE-1 authority. The X11 TCP transport is disabled.
Kernel bind behaviour, compiled JNI ABI and component visibility are checked;
a full on-device socket/graphics test is still required before broader use.

Only this exact dedicated process can terminate itself. A native failure also
belongs to this VM; no terminal-session PID, killall, pkill or shell kill is used.
Android process policy can still affect an app as a whole; isolation is not a
promise of uninterrupted user jobs under every system condition.
