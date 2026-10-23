# 1.7.7-x11-test · code 1222 · private device test

This is not a public release. Stable GitHub/update.json remains v1.7.5/code1220.
The original stable APK remains in Download/Termux-Jobs-1.7.5-BACKUP. Rolling
back over this test requires rebuilding the stable sources with a higher code
and the existing certificate; do not uninstall or clear app data.

## Implemented scope

- Added explicit `Ubuntu X11 Test` option (`jobs-x11-ubuntu`). Existing Ubuntu
  remains its original alias and is not redirected or replaced.
- X11 appears inside the selected local session under the existing header.
  Terminal rendering/input controls are hidden while the graphical surface is
  mounted. Switching back releases the remote renderer/view, keeps the desktop
  running, and restores the terminal. Header/navigation/files/packages remain.
- Native server/renderer stay in private `:jobs_x11`, connected through same-UID
  bounded Binder and Android 11+ SurfaceControlViewHost. Main-process death
  releases its remote view. Linux processes remain a separately owned group.
- The same status-chip menu contains check → install → disabled installed state,
  switch/return and confirmed desktop stop. Installation has indeterminate
  progress plus bounded live output; outside taps/back cannot dismiss it;
  Cancel requests shutdown of only its owned installation group. Interrupted
  package operations may need a repair/retry; no reset/removal is automatic.
- Real XFCE backend currently supports Ubuntu and Ubuntu X11 Test. Other local
  systems show a clear next-stage message and never fall back to another rootfs.
  This test supports one active graphical backend at a time; stop it before
  starting another environment's desktop.
- XFCE is installed through the selected Ubuntu's APT. Terminal and GUI in the
  same environment use the same packages. Files/packages retain the selected
  alias. A shared-Ubuntu installation explicitly warns that agents use it too.
- `DISPLAY=:97`, cookie authority and no X11 TCP listener. Desktop socket uses
  Termux PREFIX/tmp; only that directory and the private control directory are
  explicitly bound into the selected proot login. Clipboard sync remains off.
- A pipe-owned setsid guardian checks its own process group, observes child exit,
  and stops only that group on cancellation/owner EOF. No foreign PIDs, killall,
  environment reset or modification of existing agent shells is used.

## Verification evidence

Logs/artifact audit are kept under `.verification/x11-desktop/`.
Relevant JVM tests cover local-session eligibility, explicit alias routing for
files, protocol/runtime safeguards, and agent polling not uncovering the terminal
under an external display. Hermetic shell fixtures cover selected-alias check,
separate Ubuntu installation, completed/failed children, cancellation by pipe EOF,
refusal of a nonprivate process group and survival of an unrelated sentinel.
Native registration/input hashes and APK runtime definitions are audited too.
Final APK identity, SHA-256 and signature evidence is recorded in apk-audit.json.

Existing first-stage 1.7.6 graphics/input/return/stop were checked on the device
by the developer. This new inline XFCE installation and Android surface/IME
behavior still require the following device checks; automated tests do not prove
these behaviors.

## Device check

1. Update over the current APK. Create an environment session: Ubuntu X11 Test.
   If absent, explicitly install that environment through the existing prompt.
   Its login is `proot-distro login jobs-x11-ubuntu`, not `login ubuntu`.
2. Open the X11 chip → Check X11 desktop → Install. Confirm the selected alias.
   Check live progress and that an outside tap does not close the installation.
3. After Installed, choose Switch to desktop. Check XFCE, touch and keyboard
   (header interactive-input icon opens its IME), including hiding the keyboard.
4. Return to terminal; run `cat /etc/os-release`. Use Files/Packages and check
   that both still target Ubuntu X11 Test. Switch back; the desktop should remain.
5. Switch to an AI session and other top tabs, return; verify no double surface,
   focus/input regression or unintended terminal/environment changes.
6. Confirm Stop graphical desktop, then Check and restart. Existing terminals
   and agents must keep running. Test cancellation separately during installation
   only when prepared to repair the test environment's interrupted package state.

## Compatibility

Same com.termux, ARM64, certificate and minSDK26; versionCode raised to1222.
Inline desktop requires Android11+, and the menu explains this on older devices.
No workspace/history/auth schema or automatic data deletion was introduced.
Only requested environment/component installation writes Linux packages, after
in-app consent. The assistant's verification used repository-local tools/fixtures
and did not perform that installation in the developer's working environments.

## Final recorded results

- Full JVM suite: 713 tests, zero failures/errors; final focused verification:
  41 tests, zero failures/errors, including late service callback teardown.
- Hermetic Python fixtures: 26 tests, all passed. Shell syntax and diff whitespace
  checks passed. Final isolated release build succeeded.
- APK runtime audit: 13,288 defined classes; required definitions present;
  31 optional unresolved types retain the existing explicit review notes.
- APK ZIP, same package/certificate, code1222, minSDK26, ARM64, exact pinned native
  and keyboard bytes, built-in desktop scripts and all34 registered JNI methods
  were checked. Baseline native libraries are unchanged. No private auth/store/
  keystore files or reviewed secret markers were found in the packaged content.
- APK size: 38916357 bytes. SHA-256:
  `04f236069049ae411df8683272cba88b37b9df3a5c958e78d0b9ee325e94ffb1`.
- Delivered to `/sdcard/Download/Termux-Jobs-v1.7.7-x11-test-arm64.apk` with SHA-256 text file;
  copied artifact hash matched. No GitHub publication in this task.
