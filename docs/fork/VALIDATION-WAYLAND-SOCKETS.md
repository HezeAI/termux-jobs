> Historical v2.0.5 experiment. Wayland/KDE was retired in v2.0.6. These checks do not describe a current desktop mode.

# Private candidate: Wayland temporary sockets and Qt stderr

Candidate **2.0.5-wayland-test.9 / code 1375**, following test.8/code 1374.
The developer reported a repeated automatic Plasma stop. The newer test.8 report
contains final phase error, actual desktop exit 0, guardian cleanup 137 and
WAYLAND_SESSION_ENDED_BEFORE_READY at about 21 seconds. An earlier report was an
intermediate snapshot; it must not be treated as proof of a still-running desktop.

Verified observations and limits:

- FUSE permission failure comes from a document portal. This alone does not
  establish why KWin/Plasma ended.
- Forcing Qt stderr in an isolated run revealed previously absent KWin messages.
  KDE classic boot and Linux EGL initialization were observed. Successful EGL or
  an inner socket is not evidence of a complete working desktop.
- A fresh temporary directory caused the real KWin wrapper to report missing
  `.X11-unix` and failure to create Xwayland connection sockets. Explicit socket
  directories removed that complaint in a subsequent isolated run. The installed
  Arch rootfs inspected separately already has a socket directory; the fixture
  finding must not be presented as the proven cause of the developer's stop.
- Full isolated Plasma runs used repository-private HOME/config/tmp/runtime and
  the existing Arch executables/libraries. They were bounded and cleaned up;
  complete KDE readiness/rendering was not established. No package installation,
  user-session stop, APK installation or rootfs configuration editing occurred.

Implementation: Wayland run preparation creates `.X11-unix` and `.ICE-unix`
in a short private `wt-<16 UUID hex digits>` directory under the selected
control directory. A fixed owner marker rejects a colliding launch rather than
adopting another runtime. Ownership/no symlinks are checked; socket-directory
permissions are 1777 under an owner-only parent. A path-length check leaves room
for X11/ICE sockets in sockaddr_un. The initial full-UUID tmp layout was rejected
before delivery because its X11 socket path was already 108 bytes. The selected run
mounts that tmp and keeps the control/audio sub-bind. TMPDIR is explicitly /tmp.
Check/install paths retain their previous mounts. Qt stderr is forced only in
the selected Plasma session. No native binary, renderer selection, X11 path,
process guardian or permission request changes. Existing bounded/redacted errors
capture the redirected Qt failures; ordinary terminal text is not exported.

15 isolated Python Wayland tests passed, including absent directory preparation,
repeat preparation, symlink rejection without changing an outside directory,
run-only mount scope, production Unix-path lengths, collision rejection, Qt stderr
ordering and existing component/driver/lock tests.
Shell syntax checks pass. Java logic is unchanged from test.8 (64 passing focused
cases recorded there); build/version changes are verified in the final artifact.
This is not device GUI verification or a confirmed fix for the reported stop.

Next phone check: launch Arch Wayland once; if it stops, save diagnostics after
its outcome and review the redacted errors before sharing. Auto/BGRA/RGBA for X11
remains a requested follow-up after resolving startup.

Same package/signing identity, ARM64, settings schema and increased versionCode.
No migration or automatic data clearing. Final artifact evidence follows.

The first full probe without socket directories logged Xwayland-socket creation
failure. A later private run with those directories no longer logged that
complaint, but full KDE readiness was not established before the 150-second bound.
All owned probe processes were checked absent after cleanup. This remains a
confirmed fixture prerequisite fix and improved diagnosis, not proof of the
cause or resolution of the developer's installed-app stop.

Final verification: modern lint 7.4.2 passed with zero errors and 212 unchanged
warnings. Release assembly, runtime class inventory and JNI callbacks passed.
Final APK ZIP integrity, `com.termux`, versionName `2.0.5-wayland-test.9`,
versionCode 1375, ARM64 ABI and unchanged signing identity verified.
Existing stable JNI and Wayland native pins are unchanged; packaged source assets
match final source. Private-file-name/secret-pattern scans found no matches
(not a guarantee against every possible secret).

APK: `Termux-Jobs-v2.0.5-wayland-test.9-arm64.apk` (52721161 bytes).
SHA-256: `d885b43225e613294efdabe58a479d082a5b07e2bd37ad445b124bf0dc943d68`.
Evidence: `.verification/wayland-sockets/`; isolated probe logs remain private
under `.verification/wayland-startup/`. No GitHub publication is part of this run.
