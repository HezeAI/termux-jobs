> Historical v2.0.5 experiment. Wayland/KDE was retired in v2.0.6. These checks do not describe a current desktop mode.

# Private candidate: Wayland startup diagnosis

Candidate **2.0.5-wayland-test.8 / code 1374**, following test.7/code 1373.

The developer's report confirms failure before Plasma readiness, not a working
KDE desktop. The Android parent compositor was prepared at roughly 574 ms; guest
startup followed, a permission-denied category appeared at 3251 ms, and child exit
0 at 19816 ms. The previous category-only export and 8 KiB UI tail cannot locate
the original permission error. No root cause or startup fix is asserted here.

Changes are limited to Wayland launch diagnostics/status plus a shared, unchanged
redaction implementation extracted from installer-error handling. The Wayland
collector preserves eight first and eight recent failure lines (8192-character
input limit, 1024-character sanitized output limit per line), rejects oversized
input and excludes normal program output and input-context protocol frames. It
is attached only to the selected Plasma child's output. No logcat or whole-device
log export is introduced. Each launch starts a new bounded collection. The fixed
wrapper marker and selected KDE/KWin categories improve launch tracing. Normal
X11 service snapshots do not export Wayland errors. A Plasma child exit before
readiness is reported as a startup failure even with code 0. Stopping/cleanup
ownership logic is unchanged. Wayland end dialogs, toast and transport name no
longer use the X11 label.

Isolated investigations reused existing Arch executables/libraries with temporary
HOME/config/runtime and a private ImageReader parent inside the repository. A
standalone KWin created an inner socket and remained alive during a bounded run;
a socket alone does not prove rendering/readiness. A Wayland-info query timed
out. Full Plasma investigation was time-bounded and did not reproduce the exact
reported failure before cleanup; these probes are not an installed-app check.
Probe children were cleaned up by validated ownership. No package installation,
APK installation, user session stop or rootfs configuration edit was performed.

Automated evidence: 64 Java/Robolectric tests passed (zero failures/errors/skips),
covering bounded first/recent errors, chunking/oversize/ANSI/deduplication, secret
redaction, report inclusion, premature Plasma exit 0, normal ready-session exit,
X11 isolation, existing transfer-error redaction, profile locks and startup/cancel
regressions. Ten isolated Python Wayland checks passed. Shell syntax and
`git diff --check` passed. These checks are not evidence of physical KDE rendering.
Final lint/build/artifact evidence is appended below after checks.
Required next phone check: launch Arch Wayland once on test.8, allow the outcome,
and export the session report. Review the redacted error section before sharing.
Existing X11 settings/Auto setup and prior Wayland limitations are retained.
Same package/signing identity, ARM64 and settings schema; increased versionCode.
No new permission, automatic data clearing or environment migration.

Modern lint 7.4.2: passed, zero errors, 212 warnings (unchanged from test.7), no
new lint suppression. Release assembly and runtime class/JNI callback checks
passed. Final APK ZIP integrity, package `com.termux`, versionName
`2.0.5-wayland-test.8`, versionCode 1374, ARM64 ABI and unchanged
signing certificate were verified. Stable JNI libraries and Wayland native pins
are unchanged; source asset checks passed. Private-file-name and secret-pattern
checks found no matches; this is not proof against every possible secret.

APK: `Termux-Jobs-v2.0.5-wayland-test.8-arm64.apk` (52720693 bytes).
SHA-256: `79025f7fd2ab0fb7a755f42dabdcacb3007ce0c97e5e6df354e9cb14a86431e5`.

Evidence: `.verification/wayland-startup/` (private logs, test XML/results,
lint summary/XML, build log, runtime inventory and artifact metadata). No GitHub
release is being published. The requested X11 Auto/BGRA/RGBA surface-format
selection remains a follow-up after Wayland startup diagnosis.
