# Private v1.7.33-x11-test / code1248 · Parole and desktop audio

Same com.termux/signing identity, code1247 →1248. No resets, renamed/created
rootfs, parallel desktops or publication. Earlier certificate/progress changes
are retained. The developer confirmed v1.7.32 works; that does not establish
this audio bridge or the new player on a device.

## Final change

Only a normal **Parole** Recommended program card is added. It has description,
Install/Installed and the existing confirmation/progress flow. Its optional package
group includes GStreamer codecs and X/audio plugins. No separate player menus,
settings, shortcuts or tabs. The same package group works in Debian and Ubuntu.

The desktop install/check now includes guest sound clients and native Termux
PulseAudio where missing. An already installed desktop can check and install the
new components without rebuilding its rootfs. The private audio process starts
with the desktop, is foreground/non-daemonized and belongs to the same guardian
process group. It has its own runtime/cookie/socket, loads playback only and does
not listen on TCP. Existing daemon configs, PID files, processes and terminal/agent
sessions are not adopted or stopped. Return to terminal keeps the desktop alive;
explicit stop or owning-session close stops its group. Audio startup failure is
logged and does not silently connect to a user's unrelated audio server.

## Verification and review

Tests use fake host binary/modules and fake guest/APT. Relevant cases include
complete media-group installation vs missing codecs, exact rootfs routing,
program output completeness with the new ID, readiness, private cookie permissions,
no TCP/microphone/adoption flags, owner-pipe EOF, cleanup and an unrelated surviving
sentinel process. Existing cancellation, cross-target guard, UI installation/log,
certificate and byte-progress checks remain applicable. Reviewed launch, socket
failure, switch/hide, explicit stop, owner close and process-group boundaries.
No installed package, active desktop/audio process, shared SDK or user data was
changed during development. Only fixture subprocesses were started/stopped.

Next device checks: stop the old graphical desktop; check/install missing desktop
components, restart it, install Parole from Recommended. Open a known MP4 and an
audio file, verify sound and seek/pause, then hide/return and stop the desktop.
Repeat in Ubuntu after stopping Debian's desktop. Verify unrelated sessions and
any separate sound process remain functional. Real sound, codec support, latency,
software decode speed and device shutdown behavior are not proven by mock tests.
Build may retain inherited Kotlin/Lint loader warnings; no clean-Lint claim.

Automated results: **56 JVM tests and 74 Python tests passed**. After adding
protection for the base audio client packages, the 12 program-routing/install/
removal tests were repeated and passed. The fake-daemon lifecycle test confirms
socket/cookie setup, EOF termination, private runtime cleanup and preservation of
an unrelated process. Logs: `.verification/x11-media/`.

Final ARM64 release and packaged runtime checks passed. Final ZIP integrity,
com.termux/code1248/minSdk26/ARM64, original signing identity, baseline native/JNI
contract and source-asset checks, and private-marker scan passed.

APK: `/sdcard/Download/Termux-Jobs-v1.7.33-x11-test-arm64.apk`

SHA-256: `c697fa94f24b1812286d292f9645d94a51b9859adf9dac2885e567b53fdf2616`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
