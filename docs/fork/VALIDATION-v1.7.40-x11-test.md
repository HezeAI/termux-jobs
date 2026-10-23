# Private v1.7.40-x11-test / code1255

com.termux/original certificate retained. Code1253→1255 (intermediate1254 also
compatible). No data reset, rootfs changes, system package installation, APK
installation, publication or manipulation of user processes during this verification.
Includes all status/profile fixes documented in VALIDATION-v1.7.39-x11-test.md.

## Readiness investigation and change

Developer reports manual Show remains necessary across environments. Earlier bounded
window-property probe was not enough; its exact failed device condition remains
unobserved. Old Download reports predate this failure and no current service/auth file
is visible from this mount. No X11 authentication bypass was attempted.

Official XFCE source confirms GetState=1/Idle denotes completed startup:
https://github.com/xfce-mirror/xfce4-session/blob/master/xfce4-session/xfsm-manager-dbus.xml
and xfsm_manager_signal_startup_done in xfsm-manager.c. Probe now reads that method on
the fresh desktop's private session D-Bus. Combined with WM self-reference verification,
Idle completes startup without requiring panel/background EWMH type guesses. Existing
window-property readiness remains fallback if D-Bus is unavailable. Startup/shutdown/
invalid replies cannot produce Idle readiness. No bus activation, process changes,
logging of titles/text, or permanent poller. Subprocess2s/bus1.5s/snapshot3s and overall
180s bounds remain; first20s timeout still provides emergency manual access. Ready
marker has explicit leading line boundary and a source marker for troubleshooting.

Read-only filesystem checks confirm dbus-send exists in all five installed targets.
This establishes availability, not that live D-Bus reports Idle or auto-show works.
Device verification is required; no claim of a proven device fix or speed improvement.

## Tests, audit and limits

70 relevant Python tests passed: readiness, keyboard wiring, desktop/guardian routing,
Firefox profiles/maintenance/native programs. New cases cover Idle without assumed
panel/desktop types, non-Idle/malformed/unavailable replies, missing WM, late success
and bounded failure.8 relevant JVM UI tests for status preservation passed in the
same unchanged Java implementation before this script-only follow-up. Source/lifecycle
review retains per-slot ownership, last-close/Stop/cancel rules; no new shutdown code.

ARM64 release/runtime audit passed (13384 definitions,31 reviewed optional unresolved
references), ZIP/package/code1255/minSdk26/ABI/signature/source assets/pinned native/
JNI28/1/5 checks passed. Limited private-marker scan passed. Inherited metadata and
deprecation warnings remain; no clean Lint claim. Destination digest verified.

Developer screenshots094043/094046 show Alpine YouTube page loads. Read-only state
check found no managed PRoot or DNS journal/wrapper there; native Termux has a managed
PRoot wrapper/journal and no DNS journal. No causal attribution to toggle clicks or
other networking changes is established. No installed browser preferences changed
by verification. DNS remains optional.

Minimum next device checks: launch a supported desktop without clicking manual Show;
confirm automatic reveal after readiness, repeat in another environment. Also cause
a browser setting error and confirm known program buttons remain; test Firefox XDG
profile DNS enable/disable with browser closed. Recheck tab switch, Stop and last-owner
close. These were not inferred from mocks.

APK: `/sdcard/Download/Termux-Jobs-v1.7.40-x11-test-arm64.apk`

SHA-256: `cddfd6f9a33cc17dc6eb09a676fae0e54cfa00a68a872e06f6dbd2850fdbca78`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
