# Private v1.7.28-x11-test / code1243 · XFCE / Firefox

Public stable remains v1.7.5. Package com.termux and signing identity retained;
code1242 →1243, no data/history reset. No GitHub publication in this task.

## Changes and compatibility

- Desktop card keeps switching, component installation, programs and conditional stop.
  Desktop selector, diagnostic entry and disabled Garcon trial row are removed.
- XFCE is the only supported desktop. Old desktop-test preference is ignored.
  LXQt launch/install/IME paths are removed and Binder/shell reject retired modes.
  Existing LXQt packages are not removed automatically when updating or launching.
- Programs → Installed contains Repair launcher permissions, unchanged checksum and
  owner-read/write repair with an original-mode backup. Developer confirmed the
  previous repair stopped application-menu flickering on the phone.
- A temporary Remove LXQt card is also in Installed. It requires confirmation,
  stopped desktop, no other installation and jobs-x11-ubuntu at UI/Binder/shell.
  Fixed package-name rules select LXQt, its dedicated libraries/portal and supplied
  Openbox/PCManFM-Qt/QTerminal applications. dpkg --no-act dependency preflight runs
  before the explicit removal group. No force, purge, autoremove, shared Qt/GVfs
  removal, user-file deletion or edits to the agents' ordinary Ubuntu.
  Local settings and unneeded shared dependencies remain intentionally.
  This temporary card is scheduled for removal after developer cleanup.
- Both maintenance actions return to Installed after completion, failure or cancel
  while the same session is selected. Failure is first shown with the child output
  in a separate error dialog, then Back returns to Installed. Package catalog caches are invalidated for
  LXQt removal; the Installed page is refreshed when returning.
- Recommended browser is now Firefox. Existing NetSurf is retained.

## Firefox source and installation

Official Mozilla installation guidance: https://support.mozilla.org/en-US/kb/install-firefox-linux
Repository: https://packages.mozilla.org/apt / suite mozilla / ARM64.
On 2026-10-20, official Release/Packages metadata advertised ARM64 Firefox
156.0.1~build1. Its 78,214,948-byte DEB was downloaded only under repository
.verification and inspected: /usr/lib/firefox/firefox and firefox-bin are ARM64
ELF binaries; a normal firefox.desktop launcher is included. This artifact
inspection is not an installation or device launch test.

Bundled public signing key: https://packages.mozilla.org/apt/repo-signing-key.gpg
Fingerprint: 35BAA0B33E9EB396F59CA838C0BA5CE6DC6315A3 (gpg --show-keys, repository-local home).
SHA-256: 3ecc63922b7795eb23fdc449ff9396f9114cb3cf186d6f5b53ad4cc3ebfbb11f
No private key or account credential is included.

After explicit program-install confirmation, only the selected Ubuntu receives
termux-jobs-mozilla.sources, a Signed-By public key and narrow Firefox/l10n APT
preferences. Other Mozilla packages have priority -1. Firefox has priority1001
so Ubuntu's epoch-prefixed Snap transition package cannot win. The selected APT
candidate must match the official Mozilla ARM64 index; install specifies that
exact version and --no-remove. --allow-downgrades permits replacing a Snap
transition package, never a general distro downgrade. APT signature validation
remains enabled; no trusted=yes, downloaded shell script or sandbox-disabling
browser flags. Existing differing configuration or symlink targets are refused.
Configuration persists after cancellation and supports normal signed APT updates.
Installed status requires configured firefox and the actual executable ELF,
not Ubuntu's Snap launcher. Firefox itself is downloaded by the installer,
not included in the APK.

## Validation evidence

35 focused JVM tests and31 Python tests pass. ARM64 release build and runtime
definition checks pass. See .verification/x11-firefox for final build/tests/audit
evidence. Focused Python
fixtures cover signed-repository publication/idempotence/conflict/symlink/key
failure, official-origin candidate checking, Snap wrapper rejection, allowlisted
cleanup and dependency-preflight refusal, plus existing guest routing and launcher
repair. Focused JVM checks cover simplified menus, ignored old desktop selection,
maintenance placement/confirmation/live-desktop refusal/return navigation and
Binder trust boundaries. Runtime APK audit checks package/code/ABI/certificate,
asset contents, baseline JNI/native preservation and private-key/token markers.
Inherited Lint/Kotlin loader warnings must not be described as a clean Lint run.

## Developer's next device checks

1. Update over v1.7.27; stop desktop. Programs → Installed → Remove LXQt.
   Confirm, verify return to Installed and absence of retired launcher entries
   after restarting XFCE. A dependency refusal must be reported rather than forced.
2. Recommended → Firefox → Install. Check progress/cancellation behavior, installed
   status, launcher, keyboard/Return and a JavaScript website. Browser operation
   inside PRoot remains unverified; do not infer it from DEB structure or mock tests.
3. Verify compact desktop card and Repair launcher permissions remains available
   in Installed. No claim of exhaustive device testing or migration compatibility.

Final APK: /sdcard/Download/Termux-Jobs-v1.7.28-x11-test-arm64.apk

SHA-256: `4f2414bd6fedc8feb8cea3b23095af1397b5053a43d0b2d980d68a2da7b678e9`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

ZIP integrity, package/code/minSdk/ABI, signing certificate, asset identity, baseline
native/JNI preservation and private-marker audit passed on this final APK.
35 JVM and31 Python checks passed. No device Firefox/LXQt-removal success is claimed.
