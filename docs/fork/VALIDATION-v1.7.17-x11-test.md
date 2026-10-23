# Private X11 check · 1.7.17-x11-test / code1232

Public stable remains v1.7.5. Private APK for the developer; no GitHub publication.

## Changes

Desktop and program installer log views follow new output at the bottom. Scrolling
up preserves the reading position; returning to the bottom resumes following.
Unchanged polling output no longer replaces selectable text. Text selection/focus
cannot scroll the log back to the old cursor during a programmatic update.
Following occurs after layout, not before Android measures the new log height.
Pending layout work is removed when dismissed or interrupted by a user gesture.
Outside taps and Back continue to leave the installation window open; cancellation
still uses the existing explicit Cancel action and acknowledgement path.

Catalog page navigation uses centered vector ImageButtons with 48 dp hit regions,
localized accessibility labels and the existing disabled-state appearance.

Repeated identical keyboard requests no longer restart IME or transfer focus again
when the Activity editor already has view and window focus. Actual show/hide
transitions and recovery of lost view/window focus still execute. Detaching the
embedded surface clears the applied-state flag, so reattachment reapplies a pending
keyboard request. This is a targeted removal of redundant calls, not proof that the
Applications menu defect is fixed.

No changes to native X11 rendering, pointer gestures, the focus gain/loss handoff
itself, desktop process ownership, terminals or AI Agent sessions in this increment.
No user data migration/reset, environment modification or new package installation
is performed by updating the APK. Desktop baseline/catalog prerequisites introduced
in 1.7.16 still apply only through the existing confirmed environment setup actions.

## Installed programs and explicit removal

A new Installed programs entry sits between Recommended and Refresh catalog.
It reads configured packages owning visible .desktop application launchers from
/usr/share/applications in the selected rootfs. This includes installed Recommended
apps and other installed graphical applications, not every library/CLI package.
Localized names/comments are read as data; Exec fields are never executed. Hidden,
unowned, unconfigured and symlink entries are skipped. Package status and ownership
use two batched dpkg-query calls; this path requires only Python's standard library,
not an AppStream download, APT bindings or network. Python3 is now checked/installed
as part of explicit desktop setup. Already-configured environments need no reset.

The list has search/pages and a right-hand vector trash button. Baseline desktop,
IME and required catalog/runtime components have disabled removal controls. The
same guard runs in the service and guest. Deletion additionally requires the
package to be in a successfully completed installed-program snapshot belonging to
that exact environment. A dialog identifies name/package/environment and asks for
confirmation; tapping outside cannot confirm/dismiss it. Progress/cancellation
uses the existing owned-job lifecycle.

Removal uses dpkg --remove with normal dependency and essential-package checks,
without force, purge or automatic dependency cleanup. The manager does not remove
other packages to satisfy a deletion request. Shared helpers such as Tumbler remain.
Close the selected application before removing it. A dependency refusal is reported
in the progress log; installed programs can be checked again. Cache invalidation
prevents a completed/failed/cancelled removal from retaining deletion authorization;
a successful operation refreshes the installed list and Recommended status.

## Developer observations and remaining checks

The developer confirmed Return in the browser as well as XFCE Terminal. Other
individual applications are not yet independently verified for Return.
Applications menu/submenus flicker both from the panel and desktop menu, while
Home submenus do not. Opening a text input and closing the Android keyboard removed
the symptom in the current run. This suggests focus interaction but does not prove
the cause or a durable fix. No speculative pointer or renderer change is included; the redundant IME request guard above needs device verification.
The supplied X11 screenshot shows Fcitx focus events, AT-SPI service warnings and
no default x-www-browser command; these do not establish the flicker cause.
Black wallpaper remains unresolved. AppStream catalog preparation and application
compatibility require their own device checks.

Next device checks: installed programs includes the tested Recommended apps; remove/reinstall an optional app such as Mousepad, check the protected core controls and list refresh. Installation output follows the latest line; scrolling up
allows reading old output without jumps; page arrows remain centered and usable.
Recheck Applications from a fresh desktop start, then compare before/after opening
and closing a text input. Do not treat that workaround as a permanent repair.

## Automated evidence

- 109 focused JVM/Robolectric tests passed, including log following/manual reading,
  vector navigation, installed-list routing, protected controls, explicit deletion
  confirmation, empty installed data, backend deletion authorization, keyboard
  request transitions and existing desktop/agent-card regression checks.
- 42 Python/shell fixture checks passed, including localized installed launchers,
  configured-package ownership, ignoring Exec fields, read-only listing, protected
  removal, preserving shared helpers, dependency refusal and rejected input.
  These fixtures use private fake commands, not installed environments.
- Release ARM64 build passed; packaged DEX runtime definitions verified:
  13,358 definitions, 31 previously reviewed optional/tooling unresolved types.
- APK ZIP integrity/duplicate-name checks passed; package com.termux,
  versionName 1.7.17-x11-test, versionCode 1232, minSdk26, ARM64 only, non-debuggable.
  Inline X11 remains API30+. Existing signing certificate and v2 signature verified.
- Pinned X11 native/keyboard assets and JNI contracts (28/1/5) verified. Unrelated
  baseline assets/native libraries remain identical to stable 1.7.5, apart from the
  previously intentional environment-runtime.sh change. X11 assets match sources.
- Private auth/signing filenames and private-key/token markers absent in APK.
- git diff --check passed. Known build tooling/Kotlin metadata warnings persist;
  no standalone Lint-clean claim is made.
- No installed APK/environment/session/process was modified by verification.
  This does not replace the developer device checks listed above.

Artifact: `Termux-Jobs-v1.7.17-x11-test-arm64.apk` (38,959,587 bytes).

SHA-256: `ff76e91d19169821feabd2bfb7056943b6a03112d2e3c41c28b669a4635d5cfe`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Evidence: `.verification/x11-log-navigation/tests-final.log`, `script-tests.log`,
`build.log`, `apk-audit.json`; delivered APK and checksum are in Download.
