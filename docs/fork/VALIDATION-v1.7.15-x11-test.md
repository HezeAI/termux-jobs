# Private X11 check · 1.7.15-x11-test / code1230

Public stable remains v1.7.5. Local build for developer checks, no GitHub release.
The developer confirms automatic/manual live keyboard input and stable desktop
scale in 1.7.14. This build preserves that renderer, viewport and IME path.

## Programs and progress

The X11 card contains **Desktop programs / Программы рабочего стола**. Rows use
name + localized description + Install / disabled Installed. Opening or refreshing
checks configured packages in the selected Ubuntu; neither absence nor check
failure silently installs anything. An explicit confirmation installs one group:

| Group | Ubuntu packages |
| --- | --- |
| Files | thunar |
| Terminal | xfce4-terminal |
| Text / Markdown editor | mousepad |
| Archives | xarchiver, unzip, zip, 7zip |
| Images | ristretto |
| Fonts and icons | fonts-dejavu-core, fonts-liberation, adwaita-icon-theme |
| Lightweight browser | netsurf-gtk |
| Office | libreoffice-writer, libreoffice-calc, libreoffice-impress, libreoffice-gtk3 |
| PDF viewer | evince |

NetSurf has limited support for complex sites/JavaScript; this limitation appears
in its row. No browser sandbox-disabling flags, Snap workaround, external APT
repository or security-policy changes are introduced. LibreOffice needs more
storage; the confirmation includes dependencies and network/storage requirements.
A package status is **not** proof that a GUI program launches correctly in proot.

Each group is checked with dpkg-query, requiring `install ok installed` for all
members. Archives accept installed legacy p7zip-full or current real 7zip;
p7zip-full is virtual in Ubuntu 26.04. The current X11 test rootfs reports Ubuntu
26.04.1 LTS; that release metadata was inspected read-only.

Programs use a separate single-job executor and guardian process group, not the
long-running desktop child/executor. Checking/installing programs does not rotate
the Xauthority cookie or reinitialize native X11. Concurrent desktop setup or
another program install is rejected; reopening a running program operation for
the same target returns its existing status. An active desktop blocks installing
into a different target. Package names, operations and targets are fixed allowlists;
no profile command, ID or UI description is evaluated as arbitrary shell code.

The actual target alias is captured when opening the list. An already running
Ubuntu desktop can remain visible behind the install window. Program installs
do not start native rendering or the desktop just to check packages. Ordinary
Ubuntu remains shared with AI Agents; its list includes the existing warning and
recommends Ubuntu X11 Test. Other environments are not redirected to Ubuntu.

Desktop and program installation share a modal horizontal progress indicator,
status details and **estimated remaining time for the current measured stage**.
APT::Status-Fd reports are parsed with bounded lines, partial-chunk handling and
numeric validation. Stages: package lists, downloading, dpkg installation. The
combined bar uses stage ranges (0–10/10–55/55–99%), an estimate of workflow progress,
not byte-exact progress across all stages. ETA uses observed percentage/time
within the current stage, resets on stage/progress resets and disappears when
stale or unmeasurable. Environment/rootfs preparation is indeterminate and says
calculating; no invented overall ETA. Success/100% requires completed verification.
The status-fd machine interface is documented by [APT](https://github.com/Debian/apt/blob/main/doc/progress-reporting.md).

Both install windows reject outside taps and Android Back. Only Cancel requests
termination through the owned guardian pipe; program progress remains visible
until that child/group exits. Cancellation can leave unpacked/unconfigured
packages, so the UI asks for another check and warns that repair may be needed.
No automatic dpkg repair, removal, upgrade-all or data clearing is performed.
APT --no-remove prevents resolving an install by removing existing packages.

The Stop graphical desktop row is absent before runtime startup. The private
snapshot explicitly reports `started`, separate from `ready` and package status.
The row stays available for the same environment after returning to the terminal,
during startup and after a runtime failure/end requiring native cleanup. An idle
check/installation or another environment cannot make that row appear. Existing
notification/service stop ownership and native process guard remain unchanged.

## Verification scope

Evidence is in `.verification/x11-desktop-programs/`. Shell fixtures use fake
proot-distro, dpkg-query and APT inside repository temporary directories; they
exercise target routing, read-only checks, partial/missing groups, current/legacy
archiver providers, allowlisted installs, failed APT, command-injection rejection
and wrong control directories. Existing guardian fixtures verify own-group EOF
cancellation while an unrelated sentinel process survives.

JVM/Robolectric checks cover bounded progress/ETA parsing, stages, stale reports,
complete successful program-result validation, rejected program IDs, cross-target
service guards, row status/confirmation/cancellation, non-dismissable progress,
and Stop visibility before/after startup, along with previous X11 IME/viewport/
gesture/service/menu and agent UI checks. Mocked views do not verify physical IME,
DPI, desktop application startup or actual apt/proot installation on Android.

Logical review traces opening before launch, fresh checks, one-group confirmation,
progress polling, complete/failure/cancellation, reopening an active same-target
operation, a different target, service disconnect, controller close and service
shutdown. A separate owned job is closed during service teardown. No installed
packages, working sessions, shared SDK/environment or APK installation are changed
by these source/build checks. No user-data reset/migration is introduced.

Official package references: [NetSurf](https://packages.ubuntu.com/resolute/netsurf-gtk),
[Evince](https://packages.ubuntu.com/resolute/evince),
[7zip provider](https://packages.ubuntu.com/resolute/p7zip-full).

## Device checks after installing the APK

1. Before desktop launch, verify no Stop row; check installed desktop and open
   Programs. Existing Thunar/Terminal should be recognized in Ubuntu X11 Test.
2. Install Mousepad, check progress/estimated stage time and reject outside taps;
   after completion open it from XFCE, type Cyrillic/Latin and save a test TXT/MD.
3. Test Xarchiver, Ristretto, PDF viewing, office document editing and browser
   navigation individually. Check LibreOffice's real space use and proot behavior.
4. Cancel one optional program installation; verify unrelated terminal/agent
   sessions and desktop remain working, then recheck package status.
5. Repeat one install with desktop shown: no automatic return to terminal;
   after completion return to the program list. Verify Stop appears only after
   startup and remains accessible after returning to terminal.
6. Keyboard opening/hiding, window resizing and session switching should retain
   the developer-confirmed 1.7.14 behavior.

Actual GUI/program compatibility and stage ETA quality remain for device testing.

## Artifact results

- 94 focused JVM/UI tests passed, zero failures/errors/skips. This includes the
  added Android-below-11 guard against a permanently checking program list.
- 34 shell/Python fixtures passed; Bash syntax and git diff whitespace checks passed.
- Release build passed in 45 seconds. Runtime scan: 13,334 defined classes,
  31 reviewed optional unresolved types; required definitions present. Existing
  Lint toolchain dependency warnings remain; no clean standalone Lint claim.
- ZIP integrity, package/version/ABI/minSDK, unchanged signing identity, pinned
  native/keyboard data, JNI declarations and private-marker checks passed.
- APK `Termux-Jobs-v1.7.15-x11-test-arm64.apk`, 38943396 bytes, com.termux/code1230,
  minSDK26, ARM64; inline X11 still requires Android 11+.
- SHA-256: `89e57e71d50e33dee7f9d66982edc52bba20197fe2d3cab9ff891dba5e343f69`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Source assets match the final APK. Baseline native libraries/unrelated assets
  remain identical; no renderer/JNI change from developer-validated 1.7.14.
- VersionCode increased from 1229 to 1230 with the same package/certificate.
  Source review found no data reset, schema migration, profile/auth change or
  environment replacement; actual optional APT installs require developer checks.
- Copied into `/sdcard/Download` with checksum; destination SHA verified.
- No GitHub publication. Actual application startup/GUI and installation progress
  behavior on the phone remain for the device checks listed above.
