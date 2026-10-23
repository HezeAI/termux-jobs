# Private X11 test 1.7.8/code1223

Developer screenshot Screenshot_20261020_112424.jpg shows the Ubuntu X11 Test
shell running, but the desktop component check fails with ERROR:CONTROL_PATH
and DESKTOP_CHILD_EXIT=2. This proves rejection by our own directory guard before
component detection; it does not prove missing XFCE or a failed Ubuntu shell.

The guard previously compared canonical path text. Android aliases/bind mounts
can give different names for the same directory; the new guard requires both
directories to exist and Bash `-ef` to match their filesystem identity. A different
existing directory is still rejected. Hermetic fixtures cover the accepted alias,
rejected unrelated directory, and independence from `pwd` output. Actual alias
identity on the device is not claimed measured from the screenshot.

The X11 menu now uses the exact AgentStatusUi body/row/scroll helpers used by
agent cards, and WorkspaceDialogs theme for related confirmations/progress/errors.
The Installed row stays disabled; outside-tap prevention and explicit Cancel are
preserved. The top chip's dimensions and switching behavior are unchanged.

Validation evidence: `.verification/x11-desktop/path-fix-tests.log`,
`tests-path-fix.log`, `build-path-fix.log`, `audit-result-path-fix.log`.
The prior full-suite evidence remains in the 1.7.7 validation report; this patch
uses focused tests of changed presentation and existing X11/agent behavior.

Device check: update over the test APK; open the Ubuntu X11 Test chip, check
components, confirm installation if missing, then switch to desktop. Successful
XFCE launch, rendering and keyboard interaction still require device verification.
No components/APK were installed by source/build verification. GitHub is unchanged.

Same com.termux, ARM64, minSDK26 and certificate; versionCode increases to1223.
No data reset or workspace migration. Existing stable APK backup is preserved.

Final verification: 41 focused JVM tests and 27 Python fixtures passed; release build and APK/runtime audit passed. Package, code1223, ARM64 and unchanged certificate verified. Size 38916497 bytes; SHA-256 `56c8464f2158c94493ddd59c2fd97844298f970768ba762ca4248a79cc1f5446`. Delivered file hash rechecked in Download.
