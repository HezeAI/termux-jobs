# Private test4: Kali Glycin/XFCE startup

## Observed device failure

The developer's two Kali screenshots (2026-10-29, 00:54) show exit 134, a
libwnck default-icon assertion, and GTK failing to decode Adwaita's
`image-missing.svg`. The Glycin SVG subprocess is invoked through Bubblewrap
and exits with status 1. This is a graphical runtime failure after package
installation. Earlier successful package transactions and executable/font/D-Bus
checks must not be represented as successful XFCE GUI startup.

## Narrow source fix

Enable the existing Glycin ABI2 compatibility module for Kali at all three
startup layers: Java asset extraction, guest environment before the private
D-Bus session, and XFCE's owned session tree. Log the active Kali decoder policy.
Fedora and Arch retain their existing policy. Other environments are not added.
Kali also follows the Debian compositor policy for Turnip and Bionic for glibc
(disabled), retaining compositing for CPU; VirGL remains disabled as before.
The module uses the Glycin constructor selectors for image loading/creation;
it does not change filesystem access checks, system libraries, Android
permissions, browser sandbox settings, or unrelated running sessions.

No rootfs or Components asset needs replacement. The next app startup stages
the updated helper scripts and module. An installed Kali environment does not
need to be deleted or reinstalled for this change.

References: [Glycin architecture](https://github.com/GNOME/glycin) and
[libglycin ABI2](https://gnome.pages.gitlab.gnome.org/glycin/libglycin/).

## Verification and limits

Ten focused tests passed: constructor selectors with RTLD_LOCAL/RTLD_GLOBAL,
executed shell scoping (including Kali, Manjaro and Ubuntu AI), missing-module
failure, owned-session policy, keyboard wiring and Alpine compatibility.
Real GdkPixbuf/Glycin SVG decoding, PNG encoding and reloading passed with the
shim and a private HOME/cache. A second probe used the pinned Kali GdkPixbuf
and libglycin with existing Ubuntu AI transitive dependencies/loaders. Baseline
also passed in this mixed runtime, so these probes do **not** reproduce the
complete device failure or prove a clean Kali desktop launch.

At the developer's request, further checks must include an isolated graphical
XFCE startup, not only a binary-readiness check. Private Xvfb tooling is staged
inside the repository, never installed into Termux or a user environment.
Full graphical fixture results and any remaining gaps must be recorded before
preparing the final candidate. The broader clean-install queue was paused:
Kali and Alpine completed package transactions; Manjaro's interrupted fixture
is not a passing result and remaining targets have not been completed.

An intermediate APK `2.0.10-test4`/1435 was built before the developer clarified
that the build should follow verification. It is not a final candidate and has
not been delivered or published. No device GUI success is claimed here.

## Resumed isolated checks

The private authenticated Xvfb server/client smoke passed. Its Unix socket,
authority file, logs and temporary directories stay inside the repository; TCP
is disabled. The resumed serial queue requires a production `x11-session.sh`
launch, a mapped XFCE panel and desktop, and 25 seconds of survival after mapping
before a fixture can pass and be removed. This is software rendering on Xvfb,
not validation of Android surfaces, phone input, GPU drivers or screen rotation.

Queue order: Kali, Manjaro, Arch, Ubuntu, Debian, Fedora, openSUSE, Void, Alpine.
Kali is rechecked with the updated startup policy. Previous completed Kali package
evidence is preserved separately. Remaining installations are pending, not passed.
No final APK is prepared/delivered until verification is finished, per the
developer's instruction.

## PRoot failure and developer-directed device trial

The resumed Kali package transaction completed successfully in 1661.4 seconds.
Both native readiness checks passed (5.7 and 5.4 seconds). At the graphical
phase the developer reported PRoot's `ptrace/wait.c:352` assertion
`signal == 0`, followed by signal 6 and a disconnected session. The fixture
controller and graphical worker were subsequently absent; a scan of owned
serial-test log descriptors found no surviving test processes to stop. The
queue is paused and no other environment was checked in that resumed run.
Do not treat this as a successful graphical test or infer that the reported
ptrace crash is an XFCE/GPU failure. The isolated server/client/capture smoke
passed earlier; the full Kali XFCE graphical result remains unverified.

The developer then requested an APK and elected to validate Kali on the phone.
Final focused tests, build and artifact checks are recorded separately below.
The previous requirement to wait for the broader queue before building was
superseded by that explicit request. No failed fixture or installed root is
automatically cleared to make the device trial work.

## Final private APK

The final source passed 10 focused tests in 17.902 seconds. Release assembly
completed successfully in 58 seconds. Legacy AGP4 lint emitted its known
`com.android.tools.lint.client.api.Vendor` detector incompatibility, so that
output is not claimed as a complete lint pass. An independent modern lint
check is run separately and must be recorded with its actual result.

Package `com.termux`, versionName `2.0.10-test4`, versionCode 1435.
Size 39831566 bytes; ARM64 only; non-debuggable.
SHA-256 `714cc8e4d0eb7a5fb6bfc0334be87b872a085408382c170e9fe5fcb4e28f5030`.
Signing certificate SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` matches the stable identity.

Archive integrity, all 85 current source assets, generated runtime assets,
required runtime classes and absence of secret-pattern findings passed. The
three Android JNI libraries match the retained rollback APK byte for byte.
Workspace schema8 remains unchanged; no reset or data migration is introduced.
Installed-data migration/device GUI compatibility is not exhaustively tested.

Independent lint7.4.2 completed successfully across all five modules: zero
errors, 223 existing warnings, no infrastructure diagnostics. The final private
APK passed artifact verification; it is offered for the developer-directed Kali
GUI trial. No application GitHub release is published.

## Developer device screenshots after test4

The developer supplied two screenshots on 2026-10-29 (07:10/07:11 device time).
Kali shows a visible XFCE4.20 desktop, panels, desktop icons and the About window.
Manjaro ARM shows a visible XFCE4.18 desktop, panels, icons and the About window.
Both screenshots identify llvmpipe software rendering. This is developer device
evidence of successful CPU desktop startup after the fix, not validation of
Turnip, Bionic for glibc, VirGL, stress stability or a separately witnessed
fresh-app installation. The Kali crash seen in the earlier screenshots is not
present in these two images. The isolated test queue remains paused.
