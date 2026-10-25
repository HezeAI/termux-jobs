> Historical v2.0.5 experiment. Wayland/KDE was retired in v2.0.6. These checks do not describe a current desktop mode.

# Private candidate: KWin GPU timer queries and X11 manual preset

Candidate **2.0.5-wayland-test.10 / code 1376** follows test.9/code 1375.
This is not a published stable release.

## Startup evidence

The developer's test.9 report records an automatic Plasma startup end at roughly
21 seconds. Isolated probes used existing Arch binaries/libraries with private
HOME, configuration, runtime, bus and temporary directories under the repository.
No package installation or installed rootfs configuration edit was performed.
A copied KDE application-index cache avoided a separate slow cold-cache rebuild
in the nested-proot fixture; this is not a clean-install startup-speed result.

Separate checks loaded Qt's xdg-shell and layer-shell plugins successfully. A
raster Qt client connected directly to the Android parent and produced the
expected RGB test frame. That validates the parent path, not full KDE.

The decisive failure was reproduced with direct KWin: SIGTRAP in the pinned Linux
Turnip library, reached from Zink through KWin's GLRenderTimeQuery constructor,
which calls glGetInteger64v(GL_TIMESTAMP). A probe-only signal handler captured
the stack; the handler and debug executables are ignored verification files and
are not packaged. Baseline shell wait recorded status 133. Earlier bare proot
status 0 therefore must not be treated as proof of a clean KWin exit.

KWin's built-in KWIN_NO_TIMER_QUERY=1 skips GPU timer queries. Applying it in the
private KWin launch shim preserves GPU OpenGL compositing. In a subsequent
isolated run, KWin stayed alive until owned probe cleanup, exposed its inner
xdg_wm_base/wl_output/layer-shell globals, and a Qt GUI client connected and exited
0. The parent produced a first-frame callback and gray window pixels; this is
not proof that the expected client content or a full usable Plasma desktop was
rendered. Full installed-app startup, input, stop/restart and frame pacing remain
device checks. Existing startup-error redaction and ownership/guardian paths are
unchanged.

Private evidence is under .verification/wayland-startup/probe/:
- t-1792894416387692246: captured Turnip trap and GLRenderTimeQuery stack.
- n-1792894541479943709: scoped no-timer KWin inner protocol/client check.
- v-1792894542534498396: full Plasma with the scoped timer switch stayed running
  until the 170-second bound but did not report desktop readiness. Portal/bus
  startup remained incomplete in this nested-proot fixture. This is an unresolved
  validation gap; the candidate does not claim complete Plasma startup success.

## X11 form behavior

On request, unchecking Auto setup now prefills the developer's screenshot values:
1600×720, 75% interface scale, and horizontal/vertical margins 5 dp on each side.
Apply is still required. Reopening saved manual settings preserves their values;
Auto setup still restores automatic resolution, 100% scale and zero margins.
The X11 mode/scaling mechanisms and per-environment preferences are unchanged.

## Checks and compatibility

16 isolated Python Wayland tests passed, including execution of the generated
KWin shim with an incoming conflicting timer value, preservation of launch
arguments and rejection of an invalid output size before executing KWin.
UI/geometry tests, lint, final build and APK verification are recorded below when
complete. Tests of the dialog do not prove physical phone DPI, rotation or IME.

Review: the workaround is scoped to a newly created private KWin launch shim;
no global shell variables or installed libraries change. No new network access,
secret collection, process-stop mechanism or data migration. Existing X11
renderers and agent sessions are outside the changed launch path. Runtime
ownership and shared desktop locking remain unchanged. Same com.termux package,
ARM64 ABI and stable signing identity, with increased versionCode. No automatic
data clearing. No GitHub publication is included.

## Final candidate verification

All 21 focused JVM/Robolectric UI/geometry cases passed (9 dialog, 9 settings,
3 scaling cases), including the screenshot preset, saving only on Apply,
reopening a saved manual mode, invalid input, auto reset, cancellation and live
apply failure. The 16 Python Wayland cases passed. Python parsing and shell
syntax checks passed. Review found no changes to stop/cancel ownership or global
renderer/environment configuration.

Modern lint 7.4.2 passed with zero errors and 215 warnings. Three additional
SetTextI18n warnings are the deliberately language-independent numeric form
literals (1600, 720 and 75); user-facing explanatory prose remains bilingual.
The other 212 warnings match the preceding candidate. No new fatal/infrastructure
lint error was introduced.

Final release assembly and runtime class/JNI inventory passed. All remaining
31 optional unresolved runtime types have existing explicit review notes.
ZIP CRC, com.termux, versionName 2.0.5-wayland-test.10, versionCode 1376, ARM64,
unchanged stable signer, unchanged existing stable JNI and pinned Wayland native
libraries, final packaged source assets and license payloads were verified.
Private-file-name and credential-pattern scans found no matches; this is not a
guarantee against every possible secret.

APK: `build/apk/Termux-Jobs-v2.0.5-wayland-test.10-arm64.apk`, 52,721,281 bytes.
SHA-256: `c938988fbdc5ad8d211d860702e69833392ead36a2bdffe28e8dba50ea16a47f`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Evidence: `.verification/wayland-timer/`.

A separate fixture-only QT_NO_XDG_DESKTOP_PORTAL trial also remained incomplete
at its 170-second bound; it did not establish a further startup fix and was not
added to the application. All owned probe groups were cleaned up; no matching
probe leaders remained afterward. FUSE/portal warnings remain separate from the
reproduced timer-query trap. Complete installed Plasma readiness and physical
phone interaction remain unverified for this candidate. X11 Auto/BGRA/RGBA is
still queued after startup verification.
