# Private v1.7.52-x11-test / code1267

## Changed behavior

New agent profiles already default to Ubuntu AI. Host preparation now installs that
selected alias when absent regardless of ordinary Ubuntu. Image ubuntu is installed
under ubuntu-ai using v5 --name or legacy --override-alias. Migration is separate and
optional. Explicit existing ordinary Ubuntu profiles still use/install ordinary Ubuntu.
Standalone helper defaults to AI; Java launchers continue to pass the selected alias.
AgentEnvironmentManager.choose now only records the alias. It no longer forces migration
when ordinary Ubuntu is present, nor installs at selection time. A dedicated UI test
checks selection without install/migration side effects or an extra dialog.

Existing container presence is checked separately from login success, for both v5 and
legacy layouts, including incomplete containers. Failed login never triggers reinstall,
reset, migration or fallback. After installation the same selected alias must pass login
before the agent starts. Inventory failure on an existing root returns nonzero instead
of falsely planning Ubuntu installation. UI explains that selected environment may be
busy or unavailable and asks to retry. Missing-only preparation confirmation retained.

## Evidence and review

31 isolated Python cases passed: 13 agent-host cases plus18 installer regression cases.
Host cases cover clean AI installation alongside ordinary Ubuntu with marker preserved,
AI default on empty installation, existing/legacy AI, busy and incomplete roots without
install/fallback, explicit ordinary selection, no consent, failed installation stopping
before login, read-only missing inventory, failed existing inventory, invalid alias and
legacy --override-alias installation. All package/proot operations are stubs in fixtures
under this repository; no installed root or system packages changed.

Existing isolated agent-dependency fixtures passed (read-only checks, install consent,
missing-only packages, component failure, checksum/auth/privacy guards). 37 distinct selected JVM
tests passed: AgentPreparation5, AgentEnvironments6, CodexSessionShell4, AgentEnvironmentUi6,
StartupEnvironment5, WorkspaceEnvironments7, EnvironmentInstallProgress2, EnvironmentInlineInstall2. Bash syntax
and source diff whitespace checks passed. Lifecycle review follows new editor and saved
profile -> check -> confirmation -> missing alias installation -> login -> agent prepare.
Cancellation, running sessions, migration and process shutdown mechanisms are unchanged.
No automatic environment deletion or overwriting path was added.

The developer screenshot showed a busy ubuntu-ai and MISSING:shell. Read-only native
inspection found executable bash and sh in both current roots. This does not prove why
the earlier check failed or resolve every external lock. Existing login failures now
remain failures and do not falsely invoke installation. Do not delete a working AI root
to test clean installation. Real first installation needs a separate device/absent root;
after update minimally retry existing Gemini environment check and launch.

## Inline installation and cancellation

EnvironmentSetup keeps alias-scoped job state outside the chooser and binds only the
current visible card. UI updates are throttled to output events (at most5 per second),
without rebuilding the list or terminal. Detached row bindings are replaced and dismissed
chooser bindings are removed. Dialog dismissal and tab switching never cancel a job.
Only Stop creates the job's UUID marker. Python is installed as a prerequisite only when
absent and approved; cancellation during that command waits until it finishes.

New environment-install.py supervisor creates its own child process session and PTY.
It never reads user terminal input and checks a live unreaped child before signaling
its process group. TERM then bounded KILL affects its group only. Final stream drain is
bounded after parent exit; exceptional paths clean up their own live child. No alias/PID
from a profile is interpreted as a signal target. No container reset or deletion added.

7 real supervisor fixtures passed under Ubuntu Python and the native Termux interpreter
through proot. They cover PTY output/progress, exact failure status, cancellation before
launch, cancellation of installer descendants while an unrelated process survives,
SIGTERM handling, a TERM-ignoring installer, and refusal of an external control path.
Native fixtures use native Bash and isolate guest/host loader variables. Initial fixture
loader failures were corrected in fixtures; they were not represented as passing evidence.

Measured percentages and phase ETA reset between phases; no fabricated overall ETA.
Additional JVM tests cover reset/unknown progress and UI lifecycle, including close/reopen,
Stop marker, readiness, and non-readiness on cancellation. Final results follow below.

## Artifact

Build succeeded. Final ZIP integrity/duplicates, package com.termux, version1.7.52/code1267,
minSdk26, ARM64-only and nondebuggable checks passed. Certificate retained, v2 signature
verified, native baseline/JNI/runtime contracts verified. Exact assets and staged changed
sources matched, including new supervisor/progress class. Bounded private marker scans
passed; they are not an exhaustive secret audit. Inherited vendor Kotlin metadata/build
tool warnings remain; a clean Lint result is not claimed. No installed app/environment
mutation, APK installation or GitHub publication was performed for this update.

APK: `Termux-Jobs-v1.7.52-x11-test-arm64.apk`, 39038665 bytes. Delivered copy SHA-256 verified.

SHA-256: `4eddb7a1dbc916ec06384702aa91205723d858cce87d4f5697873dd2a221a1ff`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Developer still needs to test first agent installation/API login and inline environment
installation/Stop/reopening on Android. Mock UI fixtures do not validate gestures, DPI,
keyboard, storage permissions, registry availability or a full real installation.
