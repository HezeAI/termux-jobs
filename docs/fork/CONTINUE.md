# Current handoff

See [the repository handoff](../../CONTINUE.md) and [v2.0.8 validation](VALIDATION-v2.0.8.md).

---

# Historical v2.0.4 handoff

# Published v2.0.4 /1366 — verified stable/latest

Release:https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.4
Tag/source commit:d5040dac37ee0143dcc44af21f25d7123a19fbf7.
Previous public:v2.0.3/code1352; previous private:1365. Same com.termux/signer/
schema8/ARM64, min26/target28/compile35, X11 min30; no reset/account/rootfs migration.
APK bytes:51859681; SHA256:56ef4e034467bc1030a6ccb29c891ce97938baad3b5575a898e7e41abcd18493.
Signer SHA256:dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644.

1251JVM/187classes,168scoped Python tests and localization584 pass. Modern lint
five modules0errors/210reviewed warnings; runtime definitions/package/ABI/signing/
secret checks pass. All APK runtime assets and Android native libraries match
private1365. Source diff whitespace findings are limited to unified-patch blank
context and verbatim upstream license/notice lines; product-source checks pass.

Nine public assets:APK,update.json,SHA256SUMS,validation,full bridge source export,
small bridge runtime,891.11.zip,Turnip_Gen8_V38.zip and driver-source notes.
GitHub sizes/digests match local files. All nine downloaded anonymously and match
SHA256; anonymous latest-stable API/latest-download updater resolve to2.0.4.
Seven tagged docs/README content checks pass; release body and final credit match.
No tag move or APK rebuild is required for these publication notes.

Six ordinary glibc desktops use per-target Bionic-for-glibc caches/processes. New
extra keys move/resize and Jobs GPU Test is optional in all eight targets. Theme
metrics are about8% smaller. Turnip developer reports are successful. QCOM891.11
passes private native tests, but one concurrent GLX interval-transition timeout
remains unresolved; retries pass. Full891.11 GUI, other GPUs/firmware/fresh-device
installations and new theme/popup/panel gesture acceptance remain explicit checks.
Reference driver ZIPs match StevenMXZ; The412Banner is an additional download
source. Corresponding native source archives/patches/licenses are in the release.
Runtime/probes are not arbitrary Xorg/Wayland compatibility guarantees.

Do not modify installed sessions/rootfs, install APKs or move the published tag.
Future work needs a new task/authorization. GitHub release descriptions remain
English; RU/EN updater compatibility and the exact credit line are retained:
AI development assistance: OpenAI Codex.

---

# Historical v2.0.3 /1352 publication

Release: https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.3
Source/tag commit:5e71d93072fe609cfcd77d129078f65fd11a44d9.
Previous public:v2.0.2/code1331. Same com.termux/signer/schema8, Android8+/ARM64,
X11 Android11+; no valid-data reset, account/rootfs migration or reinstall needed.
APK Termux-Jobs-v2.0.3-arm64.apk,45,882,874bytes,
SHA256 `5c2deb38156453cf76f3d6bf87f2eb913b2dc5eb8fcde7034b93ba6e2debfaa0`.
Signer SHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

HezeAI confirmed the final candidate works, including Fedora MP4, then authorized
this publication. Source/resources/runtime assets/native libraries match that
candidate; release version fields changed. Fresh1229JVM/184classes, Python579run/
578pass/1intentional retired-overlay skip,31Node fixture programs passed.
Five-module modern lint:0errors/210 reviewed warnings. Localization,681 staged
source/test/resource matches, APK/ZIP/runtime/ABI/signer/secret checks passed.
No installed app, working environment or user terminal/server session was changed.

Five public assets:APK,update.json,SHA256SUMS,VALIDATION-v2.0.3.md,AUDIT-v2.0.3.md.
GitHub sizes/digests match; all five downloaded anonymously and match local bytes.
Anonymous latest stable API and latest/download/update.json resolve to this release.
Release body/credit and every linked tagged document are publicly verified. RU/EN
updater compatibility warns before install: HOME-only SAF, exclusive outputs,
read-only malformed duplicate-ID libraries and uncertain remote Copy/Move after
interruption. Existing account stores and schema8 remain. Do not move this tag.
Evidence:.verification/release-v2.0.3/. Future APK code must exceed1352.

Remaining work:target-SDK/package-ID/sharedUserId architecture,accessibility,
fresh-phone/other-driver combinations,live-provider recovery,force-kill durability
and remote descendant ownership. Device/GUI evidence and automated fixture results
remain separate in the public audit/validation. No current task requires more tests,
an APK installation or another publication. Release descriptions remain English,
RU/EN updater notes retained; credit line:AI development assistance: OpenAI Codex.

# Historical private source — 2.0.16-test/code1345

The [lint and final source-audit pass](AUDIT-LINT-2026-10-24.md) adds a repeatable
AGP7.4.2/Gradle7.5.1/JDK11 analysis stage covering all five modules. It fixes 11
newly visible errors, shared-locale handling, API26 constant access, notification
immutability and explicit unchanged component exports. Thumbnail parsing uses
AndroidX EXIF1.4.2 (previously1.3.6 transitively present). Attachment Download uses
exclusive creation; image taps support performClick without turning drag into a
click. Native assets/process-stop mechanisms, package/signer/schema8 and SDK/ABI
requirements stay unchanged. Public stable remains 2.0.2/code1331; no reset/migration.

Verified APK: `build/apk/Termux-Jobs-v2.0.16-test-arm64.apk`, 45,849,832 bytes;
SHA-256 `e671773d958d071f7b002a6752fcbccfda4b247b96fad851b45446943144d893`.
Final 1,209 JVM cases/182 classes passed, no failures/errors/skips (app: 1,055 and
emulator: 154, freshly executed). One initial weak-callback test fixture was fixed,
then repeated under the full suite; product animation was unchanged. Eight new
Python checker cases pass. Modern lint: 0 errors/209 warnings, no registry/Kotlin
loading failures; findings are [cataloged](LINT-WARNINGS-2026-10-24.md), not hidden.
Localization584 plus app/shared parity, source/stage, ZIP/identity/signing/runtime
and bounded secret-pattern checks pass. Required missing types: 0; 31 optional
references reviewed. Assets/native libraries match 1344. Runtime Python/Node
suites were not repeated for unchanged assets. Evidence:
`.verification/lint-audit-20261024/`.

Use [the modern lint command](LINT.md) before release; legacy/vital4.2 output
alone is incomplete. AGP7.4's compileSDK33-tested warning with project compile35
is retained, so exhaustive API35/latest-tool coverage is not claimed. Full APK
build-tool/target-SDK/package-ID/sharedUserId migration is separate. All main
stages of the agreed source-audit sequence are complete; device/manual, fresh-
install, native-driver and remote recovery limits remain explicitly recorded.
Minimum phone checks: photo preview/zoom, attachment Download, notification
open/wake-lock actions and agent/terminal/X11 switching. A later stable release
still needs authorization, code above 1345, localized compatibility notes and
update.json from its final APK. No private source audit publishes a release.

# Earlier private source — 2.0.15-test/code1344

The [locale/provider audit](AUDIT-PROVIDERS-2026-10-24.md) fixes canonical root boundaries,
symlink/search cycles, unsafe document names/root deletion, long metadata sizes,
encoded share URIs, invalid-action fallback and locale-dependent MIME/search.
A shared helper validates opened regular-file descriptors before truncation,
handles Android aliases by directory device/inode identity and pins parent
handles for creation/deletion. Legacy allow-external-apps and manifest guards
remain; property files and hard-link aliases are read-only. The service change
is limited to Locale.ROOT for two lock tags. Workspace browsing, installed
rootfs, agent bridges, assets, native libraries and process-stop code are unchanged.

Verified APK: `build/apk/Termux-Jobs-v2.0.15-test-arm64.apk`, 45,847,810 bytes;
SHA-256 `44aa5a1b01d94edb3be0d6e01a17dc9901311783e793ce494dac4425aa2fc2f8`.
All 42 scoped JVM cases in 5 classes pass; three old-code controls fail as expected.
Built-in lint: 0 errors/118 warnings, six DefaultLocale reports removed. Existing
AndroidX/Kotlin incompatibilities still prevent a full clean-lint claim.
Localization: 584 plus app/shared parity, ZIP/package/version/ARM64/signing/runtime,
source/stage and bounded secret-pattern checks pass. Native/assets match1343;
required missing classes: 0, reviewed optional: 31. Evidence:
`.verification/provider-audit-20261024/`. Earlier full-suite results remain bound
to their own recorded artifacts, not a new full-suite rerun here.

Public stable remains 2.0.2/code1331; package/signer/schema8/min26/target28/ARM64
and X11 min30 unchanged. No valid-data reset or migration. Intentional access
restrictions: outside-HOME SAF links/IDs and missing/special legacy share files
are rejected; SAF creation is still available. Existing ordinary IDs/alias names
remain stable. Include this change in both localized highlights before a later
stable release above 1344, with final update.json and release documentation.

The syscall model uses private real files and explicit Robolectric stand-ins,
not physical Android Binder/SELinux/FUSE. Phone verification of HOME SAF create/
edit and shared-storage termux-open, mount aliases and actual permissions remains
necessary. Parent pinning is not a snapshot or isolation from trusted concurrent
shell/root renames after validation. No running session/account/server was changed.

**One main source audit stage remains:** lint-toolchain alignment and remaining
warning triage. Remote descendant/durable recovery, target-SDK/package-ID design
and fresh-device/manual GUI checks remain separately recorded limitations.

# Earlier private source — 2.0.14-test/code1343

The [settings lifecycle audit](AUDIT-SETTINGS-2026-10-24.md) removes12 static
preference-adapter singletons that retained their first screen. Each fragment
creates/binds its own adapter; seven unused duplicate Context fields are removed.
Current-screen language/toasts, live-Context package-error routing and the same
SharedPreferences classes/packages/files/keys/modes are preserved. There is no
Application-context substitution, weak global cache, data reset or migration.
No session/process/rootfs/account mechanism changes. Earlier private fixes remain
included; public stable is still v2.0.2/code1331, same com.termux/signer/schema8,
ARM64/min26/target28 and embedded-X11 min30.

Verified APK: `build/apk/Termux-Jobs-v2.0.14-test-arm64.apk`, 45,845,766 bytes;
SHA-256 `daf7d4d3515034b082d1211da6b95a3d0c05193a9fd763aa5d8e5828fbdb84b9`.
All15 scoped JVM tests in4 classes passed, no failures/errors/skips. Seven new
cases cover all12 adapters, destroyed/reopened screens, absent static roots,
real Fragment/SwitchPreference persistence, independent owners/shared values,
all five logging adapters' new-language feedback, missing-package recovery and
exact package routing. Existing language4/keyboard2/diagnostic-UI2 cases also pass.
Corrected old-code reproduction failed6/7 cases; the first fixture additionally
called an intentionally unsupported setter and was corrected separately.

Executed release lint: 0 errors/124 warnings, all12 settings StaticFieldLeak
reports removed without suppressing them. The existing AndroidX-registry/Kotlin
incompatibilities remain; complete clean lint is not claimed. ZIP, package/code/
ARM64, signer continuity, runtime references, bounded secret-pattern and source/
stage checks passed; assets/native libraries match1342, required missing types: 0,
reviewed optional types31. Production delta:12 settings files plus version metadata.
Evidence: `.verification/settings-audit-20261024/`. The full1172-JVM/560-Python/
31-Node results below remain specific to1342, not a full1343 rerun.

The source/reference tests establish removal of the demonstrated singleton roots,
not physical RAM/GC timing or all possible framework retention. Test package
contexts/preferences are private; the error shadow records the live Context and
does not verify a physical error dialog or exit an app. Real addon permissions,
window tokens, physical widgets and installed upgrade data survival need device
checks. No installed environments, active terminals, user accounts or server/bot
were changed by this work.

Minimum phone check: change a keyboard preference, close/reopen settings and
verify it; reopen log settings after switching app language and check feedback.
Two main planned source audit topics remain: locale/document-provider paths,
then lint-toolchain alignment/warning triage. Existing remote descendant/durable
job recovery and target-SDK/application-ID design limits are separate and remain
explicit. A later stable release must exceed1343, include localized compatibility
notes and update.json, and follow the authorized release workflow.

# Earlier private source — 2.0.13-test/code1342

The [cumulative regression audit](AUDIT-REGRESSION-2026-10-24.md) reruns all JVM
module tests and Python/Node fixture programs, reviews built-in lint and fixes its
21 reported errors through named constants, explicit API30 X11 guards and one
scoped exception for the deliberate platform icon widget. Badge Paint is reused;
window/button dimensions, styling, sessions, process-stop paths and data formats
remain unchanged. Documentation now distinguishes Android8+ app from Android11+
embedded X11 and names the correct2.0.2 stable download. Earlier audit fixes remain
included. Public stable is still v2.0.2/code1331; same com.termux/signer/schema8,
no valid-data reset, account migration or environment replacement is required.

Verified APK: `build/apk/Termux-Jobs-v2.0.13-test-arm64.apk`, 45,846,118 bytes;
SHA-256 `d4644ed709480c6cb2e2f3376be70e604f173954b89c9c23529cf4c6f269931c`.
Final1172 JVM tests/177 classes passed (1018 app,154 terminal-emulator; no JVM
sources in terminal-view/shared/x11 themselves). Python:560 run/559 passed/1
intentional retired-Garcon-overlay skip, across52 files plus import assertions.
Node:31 successful fixture programs, including isolated configured real-CLI runs.
Localization:584 workspace strings plus app/shared settings/format parity.
Executed lint checks:0 errors/136 warnings; AndroidX custom-registry/Kotlin
incompatibilities prevent a full clean-lint claim. A120-second private runner
limit and its missing import path were corrected with explicit scoped reruns;
the unchanged large cleanup case passed in130.926 seconds. An intermediate
missing constant qualification was corrected before final compilation/tests.

Production changes since1341 are the16 UI/controller files recorded in the source
comparison and version metadata. No new production class, asset or native change.
ZIP, package/version/ARM64/signing continuity, required runtime classes, source/
stage matching and bounded secret-pattern checks passed. Required missing classes0;
reviewed optional types31. Evidence: `.verification/regression-audit-20261024/`.
Earlier reports below retain their own artifact-specific evidence.

Next work: review settings singleton context retention, locale/document-provider
path handling, then align lint tooling and triage remaining warnings. Do not
blindly replace proot storage aliases or change widget themes to silence warnings.
Remote/guest descendant cancellation, durable job recovery, target-SDK/application-
ID migration and store distribution remain separate unresolved design/verification
areas; no new promise is made for them. No user's installed rootfs, session,
account, server or Telegram bot was changed/contacted by the fixture runs.

Minimum phone checks after an explicit chosen update: agent input focus while
messages arrive, terminal/X11 switching and Android keyboard open/close, plus a
small disposable Copy/Move/export. Fresh-device setup and isolated SSH failure
recovery remain device/network checks; do not erase working environments for a
fixture. A stable release still needs explicit publication authorization, a code
above1342, release/docs review, both localized compatibility notes and update.json
from its final APK; no stable release was published by this private audit.

# Earlier private source — 2.0.12-test/code1341

The [file recovery audit](AUDIT-FILE-RECOVERY-2026-10-24.md) requires ordered
per-invocation progress, a final receipt and successful transport exit to confirm
Copy/Move. Failures show received acknowledgements as a lower bound: additional
work may have committed or still be running. No automatic write retry is added.
ProjectFiles SSH uses protocol keepalives; host-key/auth policy and agent tunnels
remain unchanged. Earlier private audit fixes remain included. Public stable
remains v2.0.2/code1331, same package/signer/schema8; no valid-data reset, account
migration or environment replacement is required.

Verified APK: `build/apk/Termux-Jobs-v2.0.12-test-arm64.apk`, 45,846,106 bytes;
SHA-256 `ad1aa3dd1907adacf665f44ffcf49c4881aa43e1942d3187cb1432c667ac5d5a`.
All94 scoped JVM tests in11 classes passed, no failures/errors/skips. These include
18 receipt/recovery tests, an additional uncertainty-message fixture and existing
transport/file/export/editor/selection/server-choice checks. A private real job
continues to its second file after its consumer loses the result channel; tests
also discard replies from already completed Copy/Move, truncate progress, inject
failed transport after a final receipt, and fail a second copy. None replay the
mutating command automatically. The first scoped60-test run also passed.

Production delta since1340: ProjectFileTransfer, ProjectFiles, browser failure
message and new ProjectFileReceipt, apart from version metadata/documentation.
Process shutdown code, native libraries and assets match1340. ZIP, signer/version/
ARM64, required runtime types, tested-source/stage and bounded secret-pattern
checks passed; required missing classes0, reviewed optional types31. Existing
custom-lint/Kotlin metadata warnings and test-runtime allocation/dispose warnings
remain; clean lint/warning-free execution is not claimed. Evidence:
`.verification/file-recovery-audit-20261024/`. Earlier full/scoped results below
belong to their stated artifacts; this was not a full app/module rerun.

Minimum phone checks: Copy/Move in a disposable folder; then, using a disposable
SSH folder/account only, verify uncertainty after a lost connection and inspect
both paths before any manual retry. Receipt/keepalive tests do not establish a
live SSH deadline, Android Process behavior, every guest tool or device UI. Local
SSH/proot exit still does not prove every descendant stopped; no rollback or
durable resume journal is promised. Installed environments, accounts, user files,
active sessions and the user's server/bot were not changed or contacted.
Next stage: cumulative source/module regression and remaining-limit review before
release planning. A safe remote guardian needs separate design/proof; no broad
process/PID-name cleanup is introduced to manufacture that guarantee.

# Earlier private source — 2.0.11-test/code1340

The [file transport audit](AUDIT-FILE-TRANSPORT-2026-10-24.md) gives each command/
read stream its own direct-child ownership, byte bound and cleanup barrier.
Old stream close cannot stop a newer child; overlapping work reports BUSY before
launch. Timeout/owner close schedules cleanup even without a reading consumer.
No user's terminal process or numeric/process-group PID is signaled. All previous
audit fixes remain included. Public stable remains v2.0.2/code1331; same package,
signer and schema8, no valid-data reset, account migration or rootfs replacement.

Verified APK: `build/apk/Termux-Jobs-v2.0.11-test-arm64.apk`, 45,844,094 bytes;
SHA-256 `7670562fa4ef0c084b46ed5806a5f609f0192675982703bc734829f0d490c1d2`.
All75 scoped JVM tests in9 classes passed, no failures/errors/skips. These include
18 new transport tests with real private host children and injected I/O failures,
plus57 confirmation/file/export/selection/editor/server-choice tests. They are
not physical Android Process, gesture, storage or live SSH recovery verification.
An expanded fixture first failed compilation on a checked-exception Runnable;
that fixture was corrected before successful reviewed/final runs.

Only ProjectFiles and new ProjectFileTransport differ in production since1339,
apart from version metadata. Native libraries/assets and signing identity match
1339. ZIP/package/version/ARM64, required runtime references, tested-source/APK-
stage consistency and bounded secret-pattern checks passed. Required missing
runtime classes:0; reviewed optional types:31. Existing custom-lint/Kotlin metadata
warnings remain; a clean-lint result is not claimed. Evidence:
`.verification/file-transport-audit-20261024/`. Earlier full/scoped results below
belong to their stated artifacts; the full app suite was not rerun for1340.

Minimum phone checks: small preview/export and SFTP upload; close/reopen a file
window during a larger read. Remote/detached descendants are not proven stopped
when local SSH/proot exits, and completed Copy/Move work is not rolled back.
Inspect both paths before a retry; no automatic retry is added. The next focused
stage is remote/guest job lifetime and safe recovery in disposable fixtures,
followed by phone verification; this direct-child patch is not a whole-tree claim.
Installed environments, accounts, user files and active sessions were not changed.

# Earlier private source — 2.0.10-test/code1339

The [file-manager audit](AUDIT-FILE-MANAGER-2026-10-24.md) binds transfer choices
and preflight to current selection, refuses replay, exclusively creates Download
exports, checks cooperative close/interruption and cleans owned export staging.
All preceding audit fixes are retained. Public stable remains v2.0.2/code1331;
valid workspace/account/environment data needs no reset or migration. Existing
batch/transport shutdown is unchanged; whole-tree cancellation is not established.

Verified APK: `build/apk/Termux-Jobs-v2.0.10-test-arm64.apk`,45,839,590bytes;
SHA-256 `49d5a62dd0fdacfc2adf94c3d303e82fa5cce91dde213f3aa88b8a3ea4fedf1c`.
All48 scoped JVM tests in7 classes passed, with no failures/errors/skips. These
include real private file/shell fixtures plus controlled Android/executor models;
physical storage permissions, FUSE and Share recipient behavior need device checks.
The prior-behavior reproduction failed6/13 cases after correcting the fixture seam.
Only WorkspaceFileBrowser and new ProjectFileExport differ in production since1338.
All runtime assets/native libraries and signing identity match1338; versionCode
increases to1339. ZIP, package/ARM64/version, runtime references, source/stage and
bounded secret-pattern checks passed. There are no required missing runtime types;
31 reviewed optional types remain. Build succeeded with the existing custom-lint/
Kotlin metadata warnings; this is not a clean-lint claim. Evidence lives in
`.verification/file-manager-audit-20261023/` (started before the local date changed).
Earlier full/scoped test results below belong to their stated artifacts.

Minimum phone check: save the same small file to Download twice, cancel/reopen a
transfer confirmation with a new selection, and Share a file to an external app.
Closing the browser cooperatively cancels an unfinished Java Download copy; a
completed copy can remain. Rootfs, accounts, user files and working sessions were
not modified by these isolated checks. The next focused review is ProjectFiles
transport lifetime on connection loss/forced close, not a rollback promise.

# Earlier private source — 2.0.9-test/code1338

The [installer audit](AUDIT-INSTALLERS-2026-10-23.md) keeps the installer leader
unreaped until final group cleanup, waits for process exit after Java output/
callback/interrupt errors, isolates invocation scripts, and resolves shell links
inside the selected guest rootfs. All previous audit fixes remain included.
Public stable remains v2.0.2/code1331. Valid account/workspace/environment data
needs no reset or automatic migration. Broken-shell rootfs now fails readiness;
repair/removal remains explicit. No running session or installed system changed.

Verified APK: `build/apk/Termux-Jobs-v2.0.9-test-arm64.apk`,45,837,706bytes;
SHA-256 `33e376e92bdeba4069ce3e28024efd1ccc26b1301cae8b9a8cee7a98fff542d4`.
All52 scoped JVM tests in9 classes and80 Python tests in8 modules passed, with no
failures/errors/skips. Prior reproductions:5/36 Python failures, a separate output-
failure reproduction, and4/5 Java lifecycle failures. Final tests include real
short-lived process groups, isolated shell/rootfs layouts, mocked package managers
and a loopback APT acquire fixture. They do not establish live mirror speed,
fresh-device installation or physical Android UI behavior.

ZIP, package/version/ARM64, same signer, required runtime references, source/stage
consistency and bounded secret-pattern checks passed. Native libraries and all
assets except the two intended environment runtime/supervisor scripts match1337.
Only EnvironmentSetup and those two scripts changed in production since1337;
there are no new production Java classes. No required runtime types are missing;
31 reviewed optional types remain. Release build succeeded with the existing
custom-lint/Kotlin metadata warnings; lint is not reported clean. Evidence:
`.verification/installers-audit-20261023/`. Earlier full/scoped results below
belong to their stated artifacts and are not relabeled as a full1338 suite.

Minimum device follow-up: on a future consented installation, verify inline
Stop/Stopping/Resume and retained native downloads, and check environment shell
readiness. Termux Python non-reaping wait, prerequisite preparation and Activity/
process-death limitations are described in the audit. No working rootfs needs to
be removed just to manufacture a fresh-install test.

# Earlier private source — 2.0.8-test/code1337

The [update/external-file audit](AUDIT-UPDATES-ENTRYPOINTS-2026-10-23.md) prevents
incoming path escape/silent replacement, closes owned streams/transports, and
rechecks cached APK bytes/current package metadata in a background worker before
installer handoff. All previous audit fixes remain included. Public stable remains
v2.0.2/code1331. No valid-data reset or migration is required; incoming duplicate
names now require renaming. Earlier duplicate-ID read-only handling remains.

Verified APK: `build/apk/Termux-Jobs-v2.0.8-test-arm64.apk`,45,836,026bytes;
SHA-256 `32821ff38c881ca0fa73e5302d06c136fee0b92e8a5148b2994d33a4c528515f`.
All35 scoped tests in7 classes passed (no failure/error/skip). Exclusive-create
host checks, staged/tested production consistency, ZIP/signature/package/version/
ARM64 checks, runtime-reference and bounded secret-pattern checks passed. Same
signer, assets and native libraries as1336; versionCode increased to1337. No
required runtime definitions are missing;31 reviewed optional types remain.
The release build succeeded with the existing custom-lint/Kotlin metadata warnings;
this is not a claim that lint is clean. Actual phone share/permission/update flows,
installation and data survival remain separate developer checks. Full938-test
results below belong to1336; they are not relabeled as a1337 full-suite run.

Evidence: `.verification/update-entrypoints-audit-20261023/`. The prior-code
receiver reproduction failed5/6 cases. Fixed fixtures explicitly model exclusive
Android open semantics because Robolectric4.4 ignores those flags by default;
the separate POSIX check is host-level evidence. APK UI uses synthetic ZIP/mock
package metadata, not an actual installer/account/network. See the audit report
for synchronous large incoming copies and pending cache/lifecycle limitations.

# Earlier private source — 2.0.7-test/code1336

The [persistence audit](AUDIT-PERSISTENCE-2026-10-23.md) shares workspace writes
across overlapping Activity lifetimes, preserves pending snapshots and rejects
late closed-store work and duplicate record IDs. Earlier audit fixes are retained.
Public stable remains v2.0.2. Valid schema8 libraries require no reset/migration;
malformed duplicate-ID files are retained read-only. Phone process-death and
upgrade behavior remain separate from source/JVM/build verification.

Verified APK: `build/apk/Termux-Jobs-v2.0.7-test-arm64.apk`,45,832,954bytes;
SHA-256 `c85d63636459e5983e98b647949e87e9ae262110ac4004a3ded64c78b3920fe4`.
All938 app tests in149 classes passed, with no failures/errors/skips. ZIP,
signature/version/ABI, runtime references, staged-source consistency and bounded
secret-pattern checks passed. Signer/assets/native libraries match1335; versionCode
is higher. No user-data reset, installed-library inspection or live migration was
performed. Current valid schema8 data is compatible; malformed duplicate-ID input
now remains intact/read-only.

# Earlier private source — 2.0.6-test/code1335

The [transfer/attachment audit](AUDIT-TRANSFERS-2026-10-23.md) adds guarded local
publication and generation-bound server choices to the preceding three audits.
Public stable remains v2.0.2; no data reset or account migration is required by
these source changes. The report distinguishes fixture checks from phone/network
verification.

Verified APK: `build/apk/Termux-Jobs-v2.0.6-test-arm64.apk`,45,831,354bytes;
SHA-256 `e6dffe1d11be005f6f32f8da81a5de444ada865dced5dfe587a0e0e01d31699c`.
143 scoped JVM tests,49 Python tests and three Node attachment suites passed.
Package/signer/ABI and all runtime assets/native libraries match1334; versionCode
is higher. ZIP/runtime/source/secret-pattern checks passed. Live SSH interruption,
phone storage permissions and provider inference remain unverified in this stage.

# Earlier private source — 2.0.5-test/code1334

The [storage audit](AUDIT-STORAGE-2026-10-23.md) extends the preceding
[environment/X11](AUDIT-ENVIRONMENTS-X11-2026-10-23.md) and agent-chat fixes.
It holds the shared storage-job lock until worker exit, preserves terminal
callbacks and shows cancellation progress. Public stable remains v2.0.2.
Verified private APK: `build/apk/Termux-Jobs-v2.0.5-test-arm64.apk`,45,830,966bytes,
SHA-256 `38df559c325d9a536d6e1e0cabcf31e0a0551a5709676c6fd758c1e86f7815dd`.
Python113 passed; broad app917 gave916 passes plus one scheduling-fixture failure;
final37-test follow-up passed after pausing that test's looper. ZIP/signature,
version/ABI, runtime references and bounded secret scanning passed. No phone
backup/restore/cleanup or Activity/process-death test is claimed.
Earlier private/public records below describe their own artifacts.

# Unreleased agent-chat audit — 2026-10-23

Earlier source2.0.3-test/code1332 contains isolated agent-chat fixes. Public stable
remains v2.0.2/code1331; no new publication is authorized by this audit request.
See AUDIT-AGENT-CHATS-2026-10-23.md for the final artifact/evidence and remaining
phone checks. Keep existing account/environment data and sessions untouched.

# v2.0.2 — Agent chat improvements

Developer authorized publication after confirming OpenCode attachment sending.
Public versionName2.0.2/code1331 supersedes private builds1326–1330 without changing
package/signer/schema8. The earlier public version is2.0.1. No data reset or automatic
environment migration. Device/profile-bound Gemini key duplicate/import requires
re-entry; restart agents with saved chats after updating. HezeAI confirmed Codex
image input and OpenCode sending; remaining live-provider/login/device limits are
in VALIDATION-v2.0.2.md. Source/features match the tested private1330 build apart
from release version metadata. Release description is English with the agreed
OpenAI Codex assistance credit. Do not publish private filenames as separate releases.

Published and verified2026-10-23: [v2.0.2 — Agent chat improvements](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.2) is stable/latest, not draft/prerelease.
Source tag7aecf5af is unchanged. All four uploaded assets were downloaded and
byte-compared; APK/metadata size/hash and code1331 agree. Anonymous latest API and
update.json work, and six tagged documentation files match their local sources.
Release title/body match the prepared English notes and include the assistance
credit. This documentation-only record does not rebuild the APK or move the tag.
Earlier entries below are historical.

# v2.0.1 — Fix Wrapper

Published stable/latest v2.0.1 — Fix Wrapper; code1325, unchanged
com.termux/signer/schema8. Updates over recovery1324. Exact verified APK size:45794876 bytes, SHA256
`a0a2e86abfb3da4f91e24d3e3c824ecc6f0ca537d2f71fa04fb8f86361ebb7ca`.
See RELEASE-v2.0.1.md and VALIDATION-v2.0.1.md. Source/build checks are complete;
all four uploaded assets were downloaded and compared byte-for-byte.
Anonymous latest API/update.json and four tagged documentation files passed.
Release: https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.1
Source tag98445d2a; do not move the published tag. This publication record
is documentation-only and does not rebuild or replace the APK.

Fixes: clipped native Present-copy fallback for non-composited GPU desktops;
independent restored preset/history identities and saved-content checks;
authoritative Turnip/VirGL checks and explicit staged Turnip cache repair.
Release descriptions stay short and in English; in-app highlights remain RU/EN.
No data reset or automatic environment migration. Device graphics/performance
coverage is limited to explicitly reported results, not inferred from fixtures.

---
Historical release state follows.

# v2.0.0 — release continuation

Published stable/latest v2.0.0: Termux-job's X11 AVAILABLE.
URL: https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.0
Source tag commit2dbb4c5b; APK45794028bytes,
SHA256492626100bb3500145fa84f28d030b8cac3e00841967ceff64cf080fae6b5dda.
All four assets downloaded/compared; APK signature/package/version/ZIP and
update.json coherence verified. Anonymous metadata and seven tagged docs return200.
Source tag is immutable; this documentation follow-up does not rebuild the APK.
Developer-authorized release: Termux-job's X11 AVAILABLE. App version2.0.0,
code1320, header italic Termux Job’s plus v2.0. Release descriptions and future
public release notes default to English; in-app updates remain RU/EN.
Developer reports testing on RedMagic10Pro and SamsungS26Ultra, with other
phones requiring verification. See RELEASE-v2.0.0.md and VALIDATION-v2.0.0.md
for final scope, assets, checks and remaining device coverage. Older entries
below are historical; do not treat temporary variants as current features.

## Частная v1.7.104-x11-test · GPU INFO Ubuntu VirGL

Разработчик подтвердил запуск Ubuntu XFCE и glmark2 через VirGL; GPU INFO
падал BadMatch/X_GetImage. Для Ubuntu VirGL применён тот же ограниченный по
времени EGL-запрос сведений, который уже используется другими Linux-целями.
Рендер Ubuntu ANGLE GL, glmark2, CPU/Turnip/wrapper и прочие цели сохранены.
[Проверки](VALIDATION-v1.7.104-x11-test.md).

## Частная v1.7.103-x11-test · явный клиент VirGL

После v102 разработчик подтвердил Arch и openSUSE. Debian по-прежнему зависал
до оконного менеджера. Read-only стек xfce4-session с соответствующими debug
символами Mesa25.0.7 показал GLX → Kopper → Zink → ожидание mutex при очистке.
Выбранный GALLIUM_DRIVER=virpipe сам по себе не исключал эту раннюю ветку GLX.

Общий helper VirGL для всех восьми X11-целей выбирает swrast-loader с драйвером
virpipe и отключает Kopper. Это пересылка рисования Android GPU, а не llvmpipe.
Termux VirGL использует ANGLE Vulkan и fork на соединение; остальные backend
сохранены. Turnip и wrapper очищают новые переменные VirGL перед своим запуском.
CPU, настройки агентов, авторизация и данные чатов не меняются.
[Проверки и ограничения](VALIDATION-v1.7.103-x11-test.md).

Проверка разработчика после103: Debian XFCE запустился, GPU INFO virgl/ANGLE
Vulkan Adreno830, glmark2 показывает сцену и проходит этапы. Termux GPU INFO
показывает Accelerated yes/virgl ANGLE Vulkan; разработчик подтвердил работу
GPU-теста. Скриншоты и границы доказательств сохранены в отчёте103.
Ubuntu/VirGL подтверждена Screenshot_20261023_114203.jpg: XFCE и glmark2 работают,
первые этапы201–214FPS, renderer virgl/ANGLE Adreno830. GPU INFO падает
BadMatch/X_GetImage; v104 добавляет Ubuntu в уже используемый EGL info helper.

## Частная v1.7.102-x11-test · Debian/Arch/openSUSE VirGL

GPU INFO через EGL расширен на Debian, Arch и openSUSE. Debian и Arch используют
ANGLE Vulkan; Arch также получает fork на подключение. Рабочий backend openSUSE
сохранён. Другие рендеры и уже проверенные Alpine/Fedora/Void не меняются.
Настоящие изолированные EGL-запросы и отрисовка прошли для трёх целей.
Запуск полного Debian XFCE и Arch glmark2 после изменения требуют проверки;
переключение backend — кандидат на исправление Debian, не доказанный результат.
[Проверки и ограничения](VALIDATION-v1.7.102-x11-test.md).

## Частная v1.7.101-x11-test · GPU INFO и Fedora VirGL

Alpine/Fedora/Void получают сведения VirGL через ограниченный по времени EGL-запрос
вместо падающего glxinfo. Fedora VirGL переведена на ANGLE Vulkan. CPU и Turnip
сохраняют прежние пути; отдельные проверки это подтверждают. 48 JVM, 32 Python
проверки, три настоящих EGL-запроса и три пробы отрисовки прошли. Полный GUI и
Fedora glmark2 после обновления ещё требуют проверки разработчиком.
[Проверки и ограничения](VALIDATION-v1.7.101-x11-test.md).

## Частная v1.7.100-x11-test · исправления Alpine/Void VirGL

В APK включён отдельный минимальный Mesa-клиент Alpine: upstream пакет не содержит
virpipe. Void использует ANGLE Vulkan; пять новых систем — fork на соединение,
совместимый также со старым протоколом openSUSE. Готовность проверяет драйвер.
68 JVM + 14 Python проверок, 10 реальных изолированных EGL-проб, ARM64 build
и аудит APK прошли. Полный GUI/glmark2 и чистый другой телефон требуют проверки;
в отдельных GL-пробах остаётся сообщение EGL_BAD_ACCESS при очистке контекста.
[Проверки и ограничения](VALIDATION-v1.7.100-x11-test.md).

## Частная v1.7.99-x11-test · Kali и VirGL

Touch Workspace удалён; активный старый выбор переходит на Kali. Галочка стиля
по-прежнему выключается для обычного XFCE. Ресурсы Kali и нативное ядро прежние.
VirGL добавлен к Debian, Alpine, Fedora, openSUSE и Void с установкой Mesa в
выбранную систему и отдельным локальным Android/Termux процессом каждого стола.
80 JVM + 29 Python проверок, ARM64 build и аудит APK прошли. Реальная отрисовка
новых вариантов требует проверки разработчиком; релиз пока не публикуется.
[Проверки](VALIDATION-v1.7.99-x11-test.md).

## Частная v1.7.98-x11-test · обычная мышь в Touch Workspace

По выбору разработчика возвращено управление как в обычном XFCE, оформление
карточек сохранено. Для собственного окна Touch добавлен X11 background pixmap
со снимком своих виджетов: восстановление участков при перемещении окон без
композитора, без постоянного таймера и чтения чужого экрана.
[Устройство](TOUCH-WORKSPACE.md), [проверки](VALIDATION-v1.7.98-x11-test.md).

## Частная v1.7.97-x11-test · касания и окна Touch Workspace

Двойной тап со смещением остаётся в точке первого щелчка; время согласовано
с GTK (500мс). Удержать и потянуть — перемещение/размер окна; удержать и
отпустить без движения — правая кнопка. Зоны захвата рамок Touch расширены
с6 до20px при тонком контуре. Kali и остальные окружения сохраняют прежние
жесты и рамки. [Проверки](VALIDATION-v1.7.97-x11-test.md).

## Частная v1.7.96-x11-test · Touch Workspace

Ниже Kali добавлена взаимоисключающая тема Touch Workspace для базового
Termux XFCE: крупные карточки, свой GTK-экран и нижняя панель, прямые тапы.
Переключение до запуска, исходные настройки восстанавливаются. Прочие rootfs
и агентские чаты не переводятся в новый вид.
[Использование и устройство](TOUCH-WORKSPACE.md),
[проверки](VALIDATION-v1.7.96-x11-test.md).

## Частная v1.7.95-x11-test · Kali-inspired вместо XP

Под той же галочкой стиля базового Termux XFCE: тёмные окна и панели,
синие значки/выделения, тонкие рамки, круглые сглаженные кнопки и фон с драконом.
Фотофон адаптирован к вытянутому экрану. Возврат первого оформления сохранён.
Другие окружения не изменены. [Устройство и ресурсы](X11-STYLE.md),
[проверки/ограничения](VALIDATION-v1.7.95-x11-test.md).

## Частная v1.7.94-x11-test · значок Menu и рамки Retro XP

В Menu — собственный значок Termux >_ вместо мыши XFCE; меню и пункты прежние.
Исправлен рисунок рамок: сплошные края вместо пунктира, скруглённые кнопки
закрытия/сворачивания/разворачивания и отсутствие квадратной подложки под
значком приложения. Только opt-in стиль базового Termux, прочие системы и
настройка подключения не меняются. [Проверки](VALIDATION-v1.7.94-x11-test.md).

## Частная v1.7.93-x11-test · Retro XP вместо прежнего стиля

Под прежней галочкой «Стиль Termux-job’s» теперь Retro XP, только Termux XFCE:
синие рамки, светлые окна, крупные цветные базовые значки, увеличенная нижняя
панель с Menu и новый фон. Первое сохранённое оформление остаётся обратимым.
«Сервер + Агент» во всех сессиях получил высоту/форму соседней кнопки Программы;
ширина прежняя, бегущая линия передачи сохранена. Команда подключения агентов
из v1.7.92 остаётся. [Устройство темы](X11-STYLE.md),
[проверки и ограничения](VALIDATION-v1.7.93-x11-test.md).

## Частная v1.7.92-x11-test · команда подключения агента

Необязательное поле над расширенным дебаг-журналом: путь к своему исполняемому
запускателю в Ubuntu/Ubuntu AI. Пусто — прежний запуск. Настройка отдельна для
сессии/пресета, применяется после перезапуска; текущие процессы не меняются.
Личные туннели, ключи и адреса не включены в APK. При ошибке запускателя
скрытого запуска напрямую нет. Старый APK игнорирует поле и запускается обычно.
[Назначение и контракт](AGENT-CONNECTION.md), [проверки](VALIDATION-v1.7.92-x11-test.md).

## Частная v1.7.91-x11-test · стиль Termux-job's

Только для базового Termux: галочка под переключением рабочего стола,
применяемая при следующем запуске. Тёмное оформление, увеличенные элементы,
курсивный фон и статичные чёрные курсоры с белой обводкой. Базовые значки
Adwaita, значки приложений сохраняются. Отключение возвращает прежние свойства,
сохраняя последующие ручные правки. Другие окружения/агенты не изменяются.
[Устройство стиля](X11-STYLE.md), [проверки и ограничения](VALIDATION-v1.7.91-x11-test.md).

Макеты XFCE в стиле Termux Job’s созданы отдельно после сборки: classic/touch/minimal.
PNG и исходные промпты встроенного image_gen: design/x11-desktop-concepts-v1.7.90/.
Копии PNG — Download/Termux-Jobs-X11-{classic,touch,minimal}.png. Это концепты,
не установленные темы; изображения не включены в APK и не меняют XFCE.

## Частная v1.7.90: GPU & GPU INFO на рабочих столах X11

В Программы → Рекомендуемые добавлен английский пункт GPU & GPU INFO.
После согласия пакетный менеджер выбранной системы устанавливает инструменты,
затем создаются два ярлыка GPU и GPU INFO в её XDG Desktop (по умолчанию ~/Desktop).
Базовая установка стола не получает эти необязательные программы автоматически.
Если команды уже установлены вручную, следующий запуск стола создаёт ярлыки.

| Окружение | Пакеты | GPU |
|---|---|---|
| Ubuntu, Debian | glmark2-x11, mesa-utils | glmark2 (X11/GLX) |
| Termux | glmark2, mesa-demos | glmark2 |
| Fedora | glmark2, glx-utils | glmark2 |
| openSUSE | glmark2, Mesa-demo-x | glmark2-es2 (X11/EGL) |
| Void | glmark2, glxinfo | glmark2 |
| Arch | glmark2, mesa-utils | glmark2 |
| Alpine stable | mesa-utils | glxgears -info, без балла glmark2 |

GPU INFO запускает glxinfo -B. В стабильной Alpine glmark2 отсутствует;
edge/testing не подключается. Для openSUSE выбран ранее проверенный разработчиком
EGL-вариант, а не проблемный GLX glmark2. Баллы разных тестов/API/настроек
не считаются сопоставимыми. На CPU инструменты показывают программный рендер,
ярлык сам не переключает на GPU. Драйверы, DISPLAY и библиотеки наследуются от
своего рабочего стола; xfce4-terminal --disable-server исключает чужой старый
terminal server. --hold оставляет результат/ошибку в окне после завершения.

При удалении бинарника оставшийся ярлык покажет подсказку об установке группы.
Пользовательские изменения ярлыков/одноимённые файлы сохраняются; вместо их
перезаписи установка может сообщить о конфликте. Для отслеживания собственных
файлов используется ~/.local/share/termux-jobs/gpu-tools/shortcuts.json.
Повторный запуск не переписывает неизменённые файлы; ссылки и пути Desktop
за пределами выбранного home отклоняются. Shell eval пользовательских путей нет.

Пункты состояния/время/прогресс/кнопки установки локализованы ru/en;
имена GPU, GPU INFO, GPU & GPU INFO остаются английскими. Сырой вывод пакетного
менеджера не переводится. Программы ставятся обычным существующим механизмом
с фоном, отменой/продолжением. У агентов/Ubuntu AI нет новых ярлыков.
Старый кэш рекомендуемых без gpu-tools автоматически отклоняется и проверяется
снова, без сброса остальных каталогов. Пакет/сертификат прежние, code1304→1305.
Сброса пользовательских данных нет; GitHub не публикуется.

Проверки и пробелы: [VALIDATION-v1.7.90-x11-test.md](VALIDATION-v1.7.90-x11-test.md).

## Частная v1.7.89: журнал неудачной установки каждого окружения

EnvironmentSetup добавляет «Подробнее» перед «Продолжить» только после
неуспешного/остановленного install. Пока установка активна и после успеха
кнопка скрыта. Механизм общий для Ubuntu/Ubuntu AI/Debian/Alpine/Arch/Fedora/
openSUSE/Void и окружений, устанавливаемых тем же списком. Termux/bootstrap
не устанавливается этим механизмом. Путь входа: создание/настройки сессии или
пресета → Окружения Termux → выбор окружения → Подробнее.

Хранится последняя неудачная попытка каждого alias в приватной
`.environment-control/logs/<alias>.txt`. Следующая ошибка заменяет файл,
успех удаляет прежний отчёт. Экспортированные пользователем TXT не удаляются.
Нативный вывод ограничен последними 262144 Java chars с явной отметкой
усечения; в карточку прогресса по-прежнему передаются только последние12000.
Это журнал установщика, не переписка и не терминальные команды пользователя.
Адреса URL/распознаваемые секреты скрываются эвристически до сохранения;
предупреждение предлагает проверить текст перед отправкой.

Подробнее открывает статический snapshot без polling или повторного install.
Сохранение TXT в Download проходит через штатный storage permission gate,
в фоне; ошибка сохранения позволяет повторить, окно не закрывается. После
перезапуска приложения журнал читается из приватного файла в worker. Вызовы
proot-distro, докачка, остановка процессов, рабочие столы, рендеры и агенты
не менялись. Причина ошибки другого пользователя по двум скринам не установлена:
на них только последняя подсказка CLI, предыдущая причина не видна.

Пакет/сертификат прежние, code1303→1304; автоочистки/миграции пользовательских
окружений нет. APK не публикуется. Итоги проверок и пробелы фиксируются в
VALIDATION-v1.7.89-x11-test.md.

## Частная v1.7.88: сохранённые драйверы Wrapper

Под строкой рендера Wrapper вместо info-текста — отдельная карточка с именем
выбранного Bionic-драйвера. Нажатие открывает локальный список импортированных
драйверов, текущий подсвечивается; несовместимый minApi недоступен. Папка справа
от рендера по-прежнему импортирует ZIP. При CPU/Turnip+Zink/VirGL обе карточки
драйвера/папки скрыты. Настройка по-прежнему только для native Termux.

Новые импорты сохраняют остальные версии; повтор того же ZIP переиспользует
каталог по SHA256. У каждой версии library.json с проверенными при импорте
метаданными/хешами. Переключение без ZIP и сети заново сверяет хеши выбранных
библиотек и атомарно обновляет current.json. Runtime Wrapper/Loader не меняется
и не скачивается повторно. Flock работающего стола запрещает import/select;
ошибка/повреждение/отмена до публикации сохраняют текущий выбор.

Оставшийся выбранный драйвер v1.7.87 автоматически получает library.json при
открытии списка или перед следующим импортом; current.json при просмотре
не меняется. Изолированно проверен именно прежний V38 без descriptor/minApi:
он появляется в списке и выбирается без повторного открытия ZIP. Версии,
которые v1.7.87 уже удалила при импорте следующего ZIP, восстановить из кэша
невозможно. С v1.7.88 удаления старых копий при новом импорте больше нет.

Проверены18 Python-тестов и37 JVM-тестов; свежая GUI-проверка выбора нескольких
реальных драйверов и перезапуска с переключённым драйвером ещё нужна.
Рабочие APK v1.7.86 и v1.7.87 закреплены в build/baselines без повышения кода.

После v1.7.87 скрин191833 подтвердил custom driver V38, Adreno830/MESA_TURNIP,
Zink, Accelerated yes, OpenGL3.2/ES2.0. Разработчик сообщил о результате glmark2
около3000; одинаковые условия/окончательный Score отдельно не подтверждены.
Arch194519: Adreno830/Zink, OpenGL4.6/ES3.2; glmark2 ещё отсутствовал.
Parole Arch сначала не загрузил libvdpau/gssdp с mirror.archlinuxarm.org:
10с low-speed timeout. После повторной попытки разработчик подтвердил установку;
программные исправления/сетевые переключения в эту сборку не добавлялись.

## Частная v1.7.87: Wrapper только для Termux

Рабочая v1.7.86/code1301 закреплена отдельно в build/baselines/v1.7.86;
разработчик подтвердил исправление зависания Thunar Arch. Резервная версия
не повышалась: повышение versionCode для отката выполняется только при надобности.

В Termux добавлен четвёртый рендер Wrapper · Bionic (тест). Выбор возвращает
в меню рабочего стола и ничего не скачивает/запускает. Справа появляется папка:
наш файловый менеджер открывает память телефона после проверки Android-доступа.
ZIP драйвера (meta.json/driver.json + ARM64 Bionic .so, до64МБ) импортируется
в отдельный приватный cache; не устанавливается из sdcard. Перед импортом
есть предупреждение о выполнении доверенного native-кода с правами приложения.
Формат/хеш не являются доказательством безопасности или поддержки GPU.

На запуске после обычной проверки/подтверждённой установки XFCE готовится
опциональный Wrapper Lee Gao0.0.5r5 и generic Vulkan Loader1.4.365 (~3.43МБ),
отдельно от прежнего Mesa/Turnip-кэша. Используется готовая Termux-упаковка
sabamdarif; libvulkan_wrapper.so совпадает поSHA256 с релизом самого Lee Gao.
Это собственная интеграция запуска/импорта, не самостоятельное авторство Mesa
и не утверждение воспроизводимой сборки upstream binary. Источники/хеши:
third-party/bionic-wrapper/provenance.json, X11-GPU.md и notice в assets.
Venus, VM, удалённый сервер и Wine не добавляются.

Рендер: Termux Mesa/Zink -> Wrapper -> AdrenoTools -> выбранный Bionic Turnip.
Только дочернее дерево Termux XFCE получает переменные/приватный loader.
Недостающие явные Termux-зависимости запрашиваются лишь при Wrapper и только
после согласия на установку. Выбор/импорт запрещены при работающем столе.
Ранее выбранный импорт сохраняется при ошибке нового ZIP; успешная замена
удаляет старую управляемую копию. Runtime загружается по закреплённомуSHA256,
проверяется повторно перед стартом; нет молчаливого отката на CPU/другой драйвер
на уровне приложения. Эффективный GPU/драйвер нужно проверить glxinfo/glmark2;
сам upstream AdrenoTools может отказать в подмене на конкретной прошивке.

Ubuntu/Debian/Alpine/Fedora/openSUSE/Void/Arch: по-прежнему три выбора,
Wrapper отвергается Java-службой, DesktopProcess и shell до запуска guest.
Их driver-кэши/rootfs/композитинг и агентские сессии не меняются.

Архив разработчика Turnip_Gen8_V38.zip прошёл проверку ZIP/ELF и реальный
импорт в изолированную папку: meta.json, minApi28, libraryName
libvulkan_freedreno.so, имя Turnip Gen8 V38. Библиотека не запускалась.
GUI, загрузка AdrenoTools и производительность нового пути требуют проверки
на устройстве; успешные fixtures не являются проверкой чистого телефона.

## Частная v1.7.86: корень Arch / цикл Thunar

Скриншоты16:28:51 и16:29:04: пустое окно Thunar и предупреждение XFWM4
«не отвечает». Отчёт2722871493744434857: X11running/desktopReadytrue,
Turnipconnected, desktop-ready6091мс. Это не повтор падения Glycin134.
Read-only анализ /proc выбранного Thunar показал CPU-цикл main/gmain,
повтор faccessat/fstatat/statx по `/`. Наблюдение rootfs:11045 событий
IN_ATTRIB|IN_ISDIR за2.5с. Права fstat чередуются0555/0755, PRoot5.1.107.96
временно добавляет владельцу доступ и восстанавливает его, вызывая
повторные обновления Thunar. Снимки /proc ограничены выбранной сессией;
содержимое документов/ключей и текст ввода не считывались.

У X11 Arch добавлена проверка owner-rwx корня до guest-проверки и запуска.
Если их нет, предлагается прежняя подтверждаемая установка с явным
пояснением исправления прав. Внутри этого согласованного install native
Termux Python открывает только корневой каталог Arch через O_NOFOLLOW,
проверяет владельца и fchmod добавляет только недостающие owner700;
групповые/прочие/специальные биты сохраняются, потом повторная проверка.
Делается до и после пакетной транзакции. Контент не обходится рекурсивно,
соседние окружения не меняются; check/run только проверяют. Наблюдение
файлов/GIO/inotify не отключается. Если native Python отсутствует, он
устанавливается лишь после согласия на install. Права ярлыков отдельно.

На временном rootfs0555 воспроизведены IN_ATTRIB от чтений; после repair
60 повторных проверок дали0 новых таких событий. Живая система/сессии
при разработке не исправлялись и не останавливались. После v1.7.86
разработчик подтвердил работу Thunar на телефоне.

## Частная v1.7.85: Arch Glycin / значки XFCE

v1.7.84: разработчик подтвердил успешную установку Arch X11, после неё
XFCE завершился134 через~26с. Отчёт7945539684196378061: компонентыsuccess,
rendererConnected/hostAttachedtrue, Turnip-Zink выбран и подключён.
Скриншот16:17:13 показывает failed load image-missing.svg, glycin-svg,
bwrap status1, GTK ensure_surface_for_gicon и Wnck default_icon_at_size abort.
ICE disconnect в конце — следствие завершения, отдельной причиной не объявляется.

Для Arch добавлена ранее используемая в Fedora совместимость Glycin2:
приватный preload перед dbus-run-session и в дереве процессов рабочего стола.
Она выбирает NotSandboxed для Glycin loader/creator: декодеры изображений
в этой сессии не используют bubblewrap, который не работает внутри PRoot.
Это уменьшает изоляцию декодирования изображений; права Android остаются
действующими. Установленные библиотеки/ярлыки/конфиги не заменяются.
Остальные targets сохраняют прежние способы совместимости.

Реальный readelf установленного Arch: libglycin-2.so.0 экспортирует оба
конструктора и sandbox-selector; GdkPixbuf2.44.8 обращается к gly_loader_new
и gly_creator_new. Upstream io-glycin-utils.c2.44.8 не сбрасывает выбранный
режим обратно в Auto. Активная совместимость строится на той же ABI2.
Внутреннее имя asset x11-fedora-glycin.so сохранено, но копия и preload
привязаны к отдельному каталогу X11 Arch; Fedora rootfs не используется.

Добавлены обезличенные категории GLYCIN_IMAGE_SANDBOX_FAILED и
DESKTOP_ICON_LOAD_FAILED. Следующая проверка — запуск рабочего стола Arch
на телефоне, значки/меню и затем GPU. Успех GUI этой сборки ещё не подтверждён.

## Частная v1.7.84: подробности ошибки установки Arch

В v1.7.83 разработчик сообщил обрыве скачивания libcups при установке X11
Arch. Диагностика подтвердила downloading/child exit1; причина Pacman
`failed retrieving file` не сохранялась из-за фильтра download/fetch.
Она теперь включается в ограниченный обезличенный сборщик, а категории
помечают retrieving/download-library как PACKAGE_DOWNLOAD_FAILED.

После ошибки в центральной области доступны «Подробнее» и нажатие
на строку: отдельное окно показывает сохранённый хвост установщика
(до8KiB), переносит длинные строки, поддерживает прокрутку, выделение
и явное копирование. Автоматического открытия или копирования нет.
Экспорт диагностики сохраняет обезличенные transfer-ошибки, а не raw-хвост.
Компоненты установки, сеть/IPv4/IPv6, rootfs и драйверы не меняются.

Изолированный curl из rootfs Arch с его loader/libs скачал проблемный
libcups целиком с HTTP200 и совпадающим SHA256: автоматический выбор
4.09с и IPv4 3.47с. Это не полноценный вход в Arch: часть системных путей
остаётся текущей Ubuntu, поэтому не доказывает работу всего Pacman.
Причина прежнего обрыва пока не определена; принудительный IPv4 не добавлен.
Следующий шаг: повторить установку, при отказе открыть «Подробнее» или
сохранить свежую диагностику X11. Работа UI на телефоне этой сборки ещё
не подтверждена; автоматические проверки описаны в validation.

## Частная v1.7.83: Arch Linux X11

Arch Linux ARM подключён к собственному процессу `:jobs_x11_archlinux`,
экрану `:105` и каталогу `workspace-x11-archlinux`. Используется выбранный
rootfs `archlinux`; Ubuntu и окружение AI Agents не используются.
Доступны CPU, Turnip + Zink и экспериментальный VirGL. Композитинг XFWM4
для обоих GPU-режимов Arch отключён. Изменение режима — после остановки стола.

Программы используют Pacman и репозитории этого Arch. Установка XFCE,
программ и подготовка каталога выполняют `pacman -Syu --needed` внутри
выбранного окружения с предупреждением в подтверждении: обновление rolling
системы требуется для согласованности библиотек. Повторная установка использует
кэш Pacman; подготовленный повтор использует `-Su`. Подписи и проверка
зависимостей сохраняются. Не применяются `--nodeps`, `--overwrite`,
`--noscriptlet` или очистка кэша. Ошибка не удаляет db.lck автоматически.
Удаление `pacman -R` не включает рекурсивное удаление зависимостей.

Полный обязательный набор базовых компонентов запрашивается явно, включая
xfdesktop, панель, xfconf, Fcitx5 GTK/Qt, D-Bus, Python, сертификаты, шрифты,
значки, libpulse, GStreamer, Mesa/Vulkan и совместимый LLVM22.
Проверка отвергает отсутствие обязательных исполняемых файлов.
Рекомендуемые программы устанавливаются отдельно: Mousepad, Xarchiver,
Ristretto/Tumbler, Firefox, LibreOffice Fresh, Evince и Parole/кодеки.

Это подготовка к проверке на устройстве, не подтверждение чистой установки
или GUI/GPU Arch. Разработчик подтвердил работу рабочего стола и Turnip + Zink
Void на v1.7.82; это отдельный проверенный сценарий.

## Текущая локальная сборка: v1.7.82-x11-test / code1297

Для управляемых команд XBPS (Void) задаётся IPv4 и снимается FORCE_IPV6 только у дочерней команды. Охвачены рабочий стол, программы и обычный менеджер пакетов. Другие окружения/родительская среда не меняются. No route to host добавлен в структурированную диагностику. Сбой воспроизведён реальным клиентом/библиотеками Void из текущей Ubuntu: IPv6 не имеет маршрута, по IPv4 подпись успешно скачалась с TLS. 43 Python- и38 JVM-тестов прошли, APK проверен; полная установка стола на устройстве ещё требует проверки. Предварительный загрузчик окружений не возвращается. Подробности: `docs/fork/VALIDATION-v1.7.82-x11-test.md` (из docs/fork — `VALIDATION-v1.7.82-x11-test.md`). GitHub не публиковался.

## Текущая локальная сборка: v1.7.81-x11-test / code1296

Удалён предварительный загрузчик environment-download.py и адаптер внутреннего OCI API. Все установки окружений/подготовка агентов используют штатный CLI. Arch ARM сохраняет фиксированный проверяемый архив. Стоп/прогресс и повтор остаются; незавершённый слой может скачиваться заново, целые слои переиспользует native-кэш. Кнопка Resume хранит отметку попытки по alias; старые скачанные части автоматически не удаляются. 42 Python- и21 JVM-тест прошли; APK проверен. Чистый Samsung ещё требует проверки. Корзина для незавершённых установок — следующая задача с учётом общего native-кэша. Подробности: `docs/fork/VALIDATION-v1.7.81-x11-test.md` (из docs/fork — `VALIDATION-v1.7.81-x11-test.md`). GitHub не публиковался.

## Текущая локальная сборка: v1.7.80-x11-test / code1295

В отчёт X11 добавлены ограниченные очищенные строки ошибок скачивания, с сохранением причины после адреса репозитория. Очистка эвристическая: отчёт нужно просматривать перед отправкой. Первый и повторный запуск установки всех OCI-окружений проверяет интерфейс адаптера и при необходимости запрашивает обновление Python/proot-distro. Сообщение внутри Termux уточнено: отдельное приложение не нужно. 59 профильных JVM-тестов и40 Python-проверок прошли, APK проверен; Samsung ещё требует проверки. Сетевой сбой Void не объявляется устранённым. Подробности: `docs/fork/VALIDATION-v1.7.80-x11-test.md` (из docs/fork — `VALIDATION-v1.7.80-x11-test.md`). GitHub не публиковался.

## Текущая локальная сборка: v1.7.79-x11-test / code1294

Ошибки проверки/установки X11 включены в отчёт отдельно от запуска стола: этап, результат, коды и безопасные сетевые/пакетные категории. В Termux явно добавлен отсутствовавший xfdesktop; проверки всех X11 требуют компоненты фона, панели и настроек. Правило чистой установки закреплено в AGENTS.md. 82 JVM-теста и63 Python-проверки прошли; APK проверен. Чистый Samsung ещё требует проверки. Сетевой сбой Void пока не устранён. Подробности: `docs/fork/VALIDATION-v1.7.79-x11-test.md` (из docs/fork — `VALIDATION-v1.7.79-x11-test.md`). Публикации на GitHub нет.

## Текущая локальная сборка: v1.7.78-x11-test / code1293

Исправлены зависимости чистой установки Termux X11, границы команд/библиотек гостевых окружений и привязка необязательных исправлений к программам. Диагностика различает ошибки ABI и коды дочернего стола/guardian. 112 JVM-тестов и профильные Python-проверки прошли; release APK проверен. Чистый Samsung и GUI этой сборки ещё требуют проверки. APK сам не обновляет уже установленные пакеты. Подробности: `docs/fork/VALIDATION-v1.7.78-x11-test.md` (из docs/fork — `VALIDATION-v1.7.78-x11-test.md`). GitHub не публиковался.

## Текущая локальная сборка: v1.7.77-x11-test / code 1292

Английские закладки Phone storage / Downloads / Home для всех X11 и запрос доступа к памяти перед новым запуском без разрешения. «Пока нет» продолжает запуск, вопрос повторяется при следующем запуске. Клавиатура и компоненты не менялись. Подробные проверки: `docs/fork/VALIDATION-v1.7.77-x11-test.md` (из docs/fork — `VALIDATION-v1.7.77-x11-test.md`). Публикация на GitHub не выполнялась.

# Частная v1.7.74-x11-test / code1289

В сборке добавлен Void X11 с собственным сервисом/display104/портом8004,
XFCE, клавиатурой, звуком, программами XBPS и приватным Turnip+Zink (glibc/LLVM18).
Void не использует Ubuntu. VirGL доступен для первого теста в Termux и Ubuntu:
нативные пакеты virglrenderer-android/ANGLE/Mesa устанавливаются после согласия,
у каждого стола свой сервер/сокет в его группе процессов. Пока VirGL не проверен
на реальном рабочем столе телефона; CPU и Turnip+Zink остаются доступны.

Для программ всех подключённых X11 добавлено продолжение: Стоп → Продолжить.
Незавершённые запросы хранятся по окружению после выхода/ошибки; загрузка сама
не возобновляется. Успешное обновление индексов не повторяется при продолжении,
архивы остаются в штатном пакетном кэше. Проверки подписи/зависимостей/блокировки
сохраняются. Побайтовая докачка зависит от менеджера и сервера; отмена во время
настройки пакетов может потребовать штатного восстановления. Докачка окружений
уже существовала и этим изменением не заменяется.

openSUSE: Evince устанавливается вместе с evince-plugin-pdfdocument. Parole
получает приватную настоящую OpenH264-библиотеку Debian2.6.0; RPM-заглушка и
временное исключение недоступного codec-репозитория сохраняются. Приватный
ярлык меню задаёт библиотеку только Parole; пользовательские правки защищены.
Старой установке нужна повторная установка группы «Видео и аудио» и перезапуск
плеера. Старые пользовательские ярлыки могут обходить новый запуск.
Изолированный тест GStreamer декодировал синтетический H264 High Profile/B-frames
до EOS; реальную запись и GUI нужно проверить разработчику на новой сборке.

Частота X11 остаётся 60Гц. GL4ES, свой Turnip и 60/120Гц отложены. Обычный
openSUSE glmark2/GLX пока не исправлен; разработчик подтвердил glmark2-es2/EGL
на Adreno830/Zink, без финального Score. Агенты и их Ubuntu AI не меняются.
Пакет/сертификат сохраняются, code1288→1289. Автоочистки и миграции данных нет.
Тесты и окончательные APK-проверки — в VALIDATION-v1.7.74-x11-test.md.
Публикации на GitHub нет.

# Частная v1.7.73-x11-test / code1288

Повторное открытие рекомендуемых и установленных X11-программ использует
кэш выбранного окружения; главное меню не запускает каталог автоматически.
Ручная проверка обходит кэш, установка/удаление помечают прежние данные
устаревшими даже при частичном сбое. Кэш заменяется, без таймера/накопления.
Объём загрузки выводится после оставшегося времени в той же строке, только
при наличии измеренных байтов; счётчики файлов не превращаются в мегабайты.

В журнале openSUSE найден отказ доступа к mozilla-openh264 с сервера кодеков.
Программы устанавливаются с временным представлением репозиториев, в котором
исключён только codecs.opensuse.org/openh264. Системные .repo не меняются,
подписи/зависимости проверяются. Это не добавляет сам кодек OpenH264; покрытие
форматов зависит от штатных декодеров. Dry-run Parole/LibreOffice успешен;
реальную установку и запуск нужно проверить в новой сборке.

Пакет/сертификат прежние, code1287→1288; пользовательские данные не сбрасываются.
Рабочие столы, GPU/композитор, агенты и остановка процессов не менялись.
Импорт своего Turnip и преобразование драйверов в эту сборку не входят.
Публикации на GitHub нет.

# Частная v1.7.72-x11-test / code1287

В базовую установку openSUSE X11 добавлен xfce4-pulseaudio-plugin: штатная
панель больше не должна требовать удаления недостающего плагина громкости.
Для существующего стола проверка запуска предложит доустановить компонент.
Звук/PulseAudio и конфигурация панели пользователя не заменяются.

Прогресс Zypper сохраняет текущий пакет отдельно от последних8192символов
журнала; точки/спиннеры его не затирают. Разбираются Preloading/Retrieving и
Installing с порядковыми счётчиками. Активный пакет не считается завершённым
до [done]; прогресс не достигает100% до штатной проверки установки. Объём
загрузки не выводится из количества файлов. При отсутствии измерения процент
и время остаются неопределёнными. Меняются только поля существующего вида.

Пакет/подпись прежние, code1286→1287; форматы данных не меняются, сброса нет.
Рендеры, библиотеки, окружения агентов, способы остановки процессов не менялись.
Публикации нет.

[Проверки](VALIDATION-v1.7.72-x11-test.md).

# Частная v1.7.71-x11-test / code1286

Добавлен X11 в имеющуюся openSUSE Leap16.0: собственные процесс/каталог/display103,
XFCE, клавиатура, звук, Programs с Zypper/RPM и кэшем каталога, рекомендации,
установка/удаление, диагностика и выбор CPU/Turnip+Zink. Mesa устанавливается
в отдельную приватную папку. Для openSUSE используется проверенный Debian
bundle и приватная libsensors5; системные библиотеки не заменяются.
GPU прошёл внеэкранную EGL-проверку на Adreno830 с библиотеками openSUSE;
полный рабочий стол и glmark2 предстоит проверить разработчику на телефоне.
Engrampa используется только в openSUSE; у остальных остаётся Xarchiver.

Fedora: совместимость Firefox учитывает lib64 и org.mozilla.firefox.desktop;
сохраняется текущий MOZ_SHM_NO_SEALS без отключения песочницы/принудительного
CPU браузера. Пользовательские ярлыки/правки защищены. Журнал понимает два
точных имени ярлыков, не произвольный путь. Полное устранение падений вкладок
ещё не подтверждено. PolicyKit скрыт временным XDG override в текущем запуске
Fedora; пользовательские/системные автозапуски не редактируются. В базовые
компоненты Fedora добавлен xfce4-about для пункта «Об Xfce» над выходом.

Пакет/подпись прежние, code1285→1286. Формат данных не сбрасывается.
Новый Firefox-журнал сохраняет SHA файлов; старые варианты продолжают читаться.
Модели/агенты и установленные системы не мигрируются. Публикации нет.

[Проверки](VALIDATION-v1.7.71-x11-test.md).

# Частная v1.7.70-x11-test / code1285

Исправлена загрузка PNG/SVG в Fedora X11: Glycin пытался запустить bwrap,
из-за чего XFCE падал в libwnck с кодом134. Отдельный небольшой glibc-модуль
включает NotSandboxed только для Glycin в дереве рабочего стола Fedora,
до создания приватной D-Bus-сессии. Это снимает дополнительную песочницу
декодеров изображений; Android sandbox и права приложения не меняются.
Alpine/musl использует прежний отдельный модуль, остальные системы не меняются.
Код системных пакетов/библиотек не перезаписывается. Исправления68/69 сохранены.
Пакет/подпись прежние, code1284→1285; сброса данных нет.

[Проверки](VALIDATION-v1.7.70-x11-test.md).

# Частная v1.7.69-x11-test / code1284

Установка рабочего стола Fedora больше не зависит от ответа Cisco OpenH264:
репозиторий fedora-cisco-openh264 отключён только в двух командах DNF этой
установки (makecache/install). Системные настройки репозиториев не меняются.
Штатный resolver выбирает официальный noopenh264 для ABI-зависимости libheif.
Это заглушка, она не добавляет H.264-декодирование. XFCE/ввод/рендер остаются
в полном плане; проверки подписей и зависимостей не отключены.
Команды установки отдельных программ и общий пакетный менеджер не изменены.
Пакет/подпись прежние, code1283→1284, сброса данных нет.

[Проверки](VALIDATION-v1.7.69-x11-test.md).

# Частная v1.7.68-x11-test / code1283

Исправлен отказ ENVIRONMENT_DOWNLOAD_PermissionError в новой докачке67:
Android разрешает проход через /data к файлам приложения, но запрещает чтение
системного каталога. Предки теперь закрепляются O_PATH, приватный конечный
каталог открывается для чтения. O_NOFOLLOW и проверки SHA-256 сохранены.
Общий загрузчик исправлен для Ubuntu/Ubuntu AI/Debian/Alpine/Fedora/openSUSE/Void
и отдельного архива Arch. Установленные системы не заменяются.
Пакет/подпись прежние, code1282→1283; общего сброса данных нет.
Это исправление файлового доступа, отдельный Fedora/OpenH264403 не устранён.

[Проверки](VALIDATION-v1.7.68-x11-test.md).

# Частная v1.7.67-x11-test / code1282

Исправлены пустые несохранённые записи после неудачного запуска: профиль
регистрируется при создании терминала, старые пустые записи убираются при
первой сверке после открытия приложения. Истории/сохранённые пресеты сохраняются.
Такие записи больше не блокируют удаление окружения; штатные проверки живых
сессий и блокировки proot-distro не отключены.

Добавлена докачка окружений для проверенного Python API proot-distro5:
Стоп/сетевая ошибка сохраняют один .part на SHA-256, затем «Продолжить».
Все OCI-слои сначала проверяются до распаковки; готовые файлы переходят в штатный
кэш без отдельной постоянной копии. Arch ARM использует закреплённый SHA.
Стоп по-прежнему ждёт завершения процессов; незавершённый rootfs не заменяется
автоматически. Старый shell proot-distro4 использует прежнюю установку.

Сбой Fedora OpenH264403 от Cisco — отдельная проблема источника; обход проверки
подписей/обязательных зависимостей не добавлен. Изменения66 сохранены.
Пакет/подпись прежние, code1281→1282, общего сброса данных нет. Публикации нет.

[Проверки](VALIDATION-v1.7.67-x11-test.md).

# Частная v1.7.66-x11-test / code1281

В чатах агентов убран автоматический фокус ввода при переходе и смене сессии;
ручной тап и кнопка интерактивного ввода сохраняют своё действие.

Исправлена установка Fedora X11: xorg-x11-utils отсутствует в Fedora44;
в базовую установку/проверку добавлены актуальные xprop и xwininfo.
DNF использует свежий кэш без принудительного refresh при каждой попытке
и до6 параллельных загрузок; retries3/timeout30, подписи пакетов сохранены.

Неудачная установка X11 теперь остаётся ошибкой с причиной, без повторного
предложения установить. Карточки окружений при сбое показывают причину
ERROR/MISSING вместо CHECK:done. Незавершённые системы не удаляются/не заменяются.
Выбор рендера/запуск из65 сохранены. Пакет/подпись прежние, code1280→1281,
сброса данных нет; APK не публикуется.


При отмене под прогрессом показывается «Остановка установщика…», повторный
запуск заблокирован до фактического завершения. Supervisor даёт собственной
группе процессов время на очистку через SIGINT, затем TERM/KILL. Лидер группы
не reaped до завершения эскалации, чтобы не терять дочерние процессы/идентичность
PGID; посторонние группы не затрагиваются. Неполная система после принудительной
остановки всё ещё возможна, автоматического удаления пользовательских сред нет.

Подготовка удаления HOME/историй теперь пишет снимок в два приватных потоковых
файла, а не в один JSON со всеми записями: прежние пределы 60000 записей/8 MiB
не применяются к этому списку. Перед первым удалением сверяются все сохранённые
идентичности, сохраняются запрет перехода по ссылкам, блокировка окружения,
TTL плана и явное подтверждение. Временные списки удаляются после операции;
брошенные старше суток убираются при создании следующего плана.

[Проверки](VALIDATION-v1.7.66-x11-test.md).

# Частная v1.7.65-x11-test / code1280

Выбор CPU/Turnip + Zink теперь только сохраняет режим для выбранного
окружения: не проверяет/не устанавливает рабочий стол, не загружает драйвер
и не запускает X11. Выбирать можно и при отсутствующем рабочем столе;
при работающем столе смена по-прежнему блокируется.

«Переключиться на рабочий стол» берёт сохранённый режим, проверяет стол,
предлагает установить недостающее, затем подготавливает выбранный рендер
и запускает стол. Установка стола/драйвера остаётся в центральном экране
с прогрессом и отменой. Повторно выбирать рендер после установки не нужно.
Отказ/ошибка загрузки не переключают на другой режим и не запускают стол.
Предпочтение сохраняется для повторной попытки. Исправление видимых карточек
установки окружений из64 сохранено. Автоматического удаления/миграций нет.

[Проверки](VALIDATION-v1.7.65-x11-test.md).

# Частная v1.7.64-x11-test / code1279

Исправлено обновление прогресса установки окружений в списке: ListView
создавал временные строки при измерении и они перехватывали подписку
видимой карточки. Теперь обновления получают только присоединённые к окну
строки; при отсоединении/закрытии списка подписки удаляются. При возвращении
карточка сразу показывает текущее состояние установки.

Исправление отображения общее для всех карточек окружений, включая Ubuntu,
Ubuntu AI и Debian; механизм их установки/остановки и данные не менялись.
Fedora/X11 из63 сохранены. APK не публикуется на GitHub.

Выбор рендера теперь сначала проверяет рабочий стол выбранного окружения.
Если стол установлен, меняется только рендер без запуска. Если отсутствует,
после подтверждения экран в центре показывает установку стола, подготовку
уже выбранного рендера и запуск. Загрузка GPU-компонентов также отображается
там с прогрессом и отменой; проверка SHA-256 сохранена. Закрытые сессии
и устаревшие операции не могут поздним callback изменить рендер/запустить стол.

[Проверки](VALIDATION-v1.7.64-x11-test.md).

# Частная v1.7.63-x11-test / code1278

К существующей Fedora добавлен свой XFCE/X11: отдельные процесс/служба,
управляющий каталог, дисплей102 и канал UI. Новая Fedora/Ubuntu не создаётся.
Сохраняются общие проверенные ввод, звук, экран подготовки, меню программ,
Firefox PRoot/DNS HTTPS и диагностика сессии X11.

Установка/проверка через DNF/RPM с правильными Fedora-пакетами; каталог
читает кеш DNF, установленные программы сопоставляются с RPM-владельцами.
Удаление одной программы через rpm -e без --nodeps/force, без автоматического
удаления зависимостей; ядро рабочего стола/пакетный менеджер защищены.
Исправление прав ярлыков допускает только файлы, совпавшие с RPM-digest.

Добавлен отдельный pinned Turnip+Zink для Fedora44; по умолчанию CPU.
Для Fedora/GPU compositor=off, другие системы сохраняют проверенные режимы.
Первый запуск и программы Fedora на телефоне ещё требуют проверки.

[Проверки](VALIDATION-v1.7.63-x11-test.md).

# Частная v1.7.62-x11-test / code1277

Контрольная сборка по решению разработчика: v1.7.62-x11-test/code1277.
APK в Download; SHA-256: a253672b47b5e0392aeae6aef0ee47e4c5d6a18b1586e2e004514dfed5033b85.
Это зафиксированная частная сборка, не новая публикация GitHub.

Только кнопка отмены на центральном экране подготовки оформлена как плашка
«Сервер + Agent»: тёмная заливка, рамка1dp, скругление12dp, текст13sp,
высота области нажатия не меньше48dp и ripple. Сохранены подпись
«Отменить установку», положение под прогрессом и прежний обработчик.
Запуск/проверка/установка/отмена и другие кнопки не менялись.

[Проверки](VALIDATION-v1.7.62-x11-test.md).

# Частная v1.7.61-x11-test / code1276

Проверка и установка X11 после «Переключиться на рабочий стол» перенесены
в область рабочего стола. Каждый канал окружения имеет своё состояние и экран;
оформление одинаковое: курсивные Termux Job’s, +, X11, ниже этап и прогресс.
Если компонентов нет — подтверждение Да/Нет. Установка показывает процент,
объём/ETA при наличии измерений и последнюю строку вывода. Под прогрессом
«Отменить установку». После установки автоматически начинается запуск.

Возврат из терминального вида к активной установке не запускает повторную
проверку. Смена вкладки не отменяет установку; чужой результат не подменяет
состояние текущего окружения. Нет/отмена/ошибки не запускают стол автоматически.
Установка не закрывается тапом по фону. Обслуживание программ сохраняет прежний UI.

[Проверки](VALIDATION-v1.7.61-x11-test.md).

# Частная v1.7.60-x11-test / code1275

Для Debian/GPU и Alpine/GPU отключён композитинг XFWM, как в Ubuntu/GPU.
Termux/GPU и все CPU-режимы сохраняют compositor=on. В отчётах59 XFCE
дошёл до ready/idle с5 клиентами за4976мс (Debian) и5862мс (Alpine),
но разработчик сообщил, что рабочие столы не отображаются. Метаданные
готовности не доказывают отрисовку. Эффект отключения требует проверки.
Обёртка запуска/общий исправленный менеджер XFCE сохранены.

[Проверки](VALIDATION-v1.7.60-x11-test.md).

# Частная v1.7.59-x11-test / code1274

Добавлен Turnip + Zink для Debian13 и Alpine3.24, отдельные закреплённые
сборки glibc/musl от того же поставщика Mesa. GPU-кэш, пути библиотек,
проверка версии/зависимостей и подключение используют выбранное окружение.
Композитинг остаётся включённым для Debian/Alpine/Termux; выключен только
Ubuntu/GPU. Общий исправленный запуск XFCE из58 сохранён.

34 Python-теста и11 JVM-тестов прошли. Хеши обоих реальных архивов совпали
с GitHub release API; безопасная распаковка и полная проверка готового кэша
прошли внутри репозитория. Библиотечные зависимости найдены read-only
в установленных Debian/Alpine. Работа GPU на этих столах ещё требует
проверки разработчиком на телефоне. Системы/пакеты/сессии не изменялись.

[Проверки](VALIDATION-v1.7.59-x11-test.md).

# Частная v1.7.58-x11-test / code1273

Убрано дублирование xfwm4: оконный менеджер запускает и регистрирует сам XFCE.
Отдельный предварительный запуск мешал штатному failsafe-учёту клиентов и
согласуется с тремя наблюдаемыми ожиданиями регистрации по8с. Для сохранения
композитинга используется частная обёртка команды xfwm4 в runtime текущего стола:
UbuntuGPU/off, остальные режимы/on. Файлы xfconf и сохранённые сессии не меняются.
Готовность WM проверяет существующий x11-ready.py после запуска менеджером сессии.
Изменение общее для подключённых X11-сред; агентские сессии не затронуты.

Отчёт7855605907232132252 подтвердил доступную шину с3 зарегистрированными
клиентами: гипотеза неисправной авторизации UID для этого запуска не подтвердилась.
Проверка разработчиком на телефоне: Ubuntu/Turnip готов за4687мс вместо29808мс
(примерно6,36 раза быстрее);5 клиентов зарегистрированы, xfwm4 sm=1,
таймаута нет. Другие окружения и повторяемость ещё требуют проверки.
Шестерёнки меню сессий и расширенная диагностика из57 сохранены.

[Проверки](VALIDATION-v1.7.58-x11-test.md).

# Частная v1.7.57-x11-test / code1272

Общая диагностика X11 расширена на весь запуск XFCE: времена первого наблюдения
и возраст процессов, CPU/state/wait, происхождение native/proot, регистрация
клиентов менеджера сессии, доступность D-Bus EXTERNAL для гостевого/ядрового UID,
число/длительность/ошибки запросов готовности, категории ошибок SM/ICE/D-Bus.
Данные ограничены текущим приватным запуском и фиксированными ролями, без argv,
путей, содержимого окружения/полей и данных авторизации. Сбор заканчивается
при готовности или таймауте запуска; постоянного фонового мониторинга нет.

Меню сессии перенесено с долгого тапа на векторную шестерёнку справа внутри
верхней плашки и каждой строки раскрытого списка. Размеры плашек сохранены,
выбор сессии отделён от открытия её меню.

Применяется общим механизмом всех подключённых X11-сред. Для ещё не подключённых
обычных окружений сохранён раздел поддержки и общий путь будущего подключения.
Агенты/Ubuntu AI исключены. Запуск, настройки XFCE/GPU/Firefox и критерии
готовности не изменены. Точная первопричина 8-секундных ожиданий ещё проверяется.

[Проверки](VALIDATION-v1.7.57-x11-test.md).

# Частная v1.7.56-x11-test / code1271

В карточках Firefox PRoot и DNS HTTPS доступны во всех подключённых X11-средах:
Termux, Ubuntu, Debian и Alpine. Обычная Ubuntu больше не исключается старым
ограничением тестового окружения. Для APT-сред добавлены применение/отключение
профильного DNS через HTTPS и проверка состояния; системный DNS не меняется.
Устаревшая рекомендация Ubuntu X11 Test из окна программ удалена.
В диагностику запуска добавлены первые наблюдения панели/фона и состояния
менеджера сессии XFCE через существующую проверку, без нового опроса.

Настройки применяются по подтверждению только в выбранном окружении. Перед
переключением Firefox следует закрыть; перед DNS первым запуском создать профиль.
Готовность стола и задержка запуска этой правкой не меняются.

[Проверки](VALIDATION-v1.7.56-x11-test.md).

# Частная v1.7.55-x11-test / code1270

Для проверки чёрного экрана Ubuntu с Turnip + Zink композитинг XFWM отключён
только при сочетании target=ubuntu и renderer=turnip. В Ubuntu CPU он включён;
Termux и другие окружения сохраняют прежний запуск. Используется аргумент
процесса, сохранённые настройки XFCE не редактируются.
В отчёт X11 добавлена отметка JOBS_X11_COMPOSITOR=on/off с временем.
Результат на экране требует повторной проверки; причина чёрного экрана пока
не считается подтверждённой или исправленной.

[Проверки](VALIDATION-v1.7.55-x11-test.md).

# Частная v1.7.54-x11-test / code1269

В диагностику обычных сессий добавлена вкладка X11: состояние службы/подключения,
выбранный и запущенный рендерер, этапы запуска с относительным временем и категории
графических ошибок. Обычный/Подробный компактно слева под Сводкой; X11 справа.
На вкладке X11 режимы журнала серые; нижние кнопки и экспорт сохранены.
Отчёт включает X11 независимо от выбранной вкладки. Для неподключённых окружений
показан статус поддержки. Агентские сессии и Ubuntu AI исключены.

Чтение диагностики не запускает/останавливает стол и не создаёт канал службы.
Получение свежего состояния выполняется по открытию вкладки/экспорту, через уже
существующее подключение. Новый постоянный опрос не добавлен. В отчёт не попадает
сырой вывод программ: сохраняются ограниченные этапы и категории ошибок.

Причина чёрного экрана Ubuntu GPU ещё не установлена. Эта сборка добавляет
наблюдение для следующего воспроизведения; настройки GPU и композитора не меняет.

[Проверки](VALIDATION-v1.7.54-x11-test.md).

# Частная v1.7.53-x11-test / code1268

В меню «Терминал / Рабочий стол» добавлен выбор рендеринга: программный CPU
и экспериментальный Turnip + Zink для Termux и обычной Ubuntu 26.04.
Выбор отдельный для каждого окружения и доступен только при остановленном столе.
Компоненты загружаются по подтверждению в частный общий каталог приложения,
проверяются по SHA-256 и не заменяют системную Mesa. CPU остаётся по умолчанию.

На Adreno 830 успешно выполнены изолированные GPU-тесты отрисовки и чтения пикселя
для обоих вариантов через тот же установщик и настройки, которые вошли в APK.
Вывод окон через X11, glmark2 и одновременная работа двух столов требуют проверки
разработчиком. Совместимость других Snapdragon пока не подтверждена устройствами.
Ubuntu AI, агенты и механизм остановки столов этой правкой не меняются.

Подробности: [GPU](X11-GPU.md), [проверки](VALIDATION-v1.7.53-x11-test.md).

# Частная v1.7.52-x11-test / code1267

Ubuntu AI устанавливается независимо от наличия обычной Ubuntu при подтверждённой
подготовке выбранного агента. Миграция остаётся отдельным добровольным действием.
Занятость или ошибка входа в существующее окружение больше не считаются его
отсутствием: проверка завершается ошибкой, установка/сброс не запускаются.
После установки проверяется вход именно в выбранную среду; переключения на другую нет.
Ранее сохранённые пресеты и выбранные окружения сохраняются. Рабочие сессии не менялись.
В списке окружений установка отображается в своей карточке: Стоп, прогресс этапа
и оставшееся время. Закрытие списка/переход по вкладкам установку не отменяют.

Подробности: [проверки](VALIDATION-v1.7.52-x11-test.md).

# Частная v1.7.51-x11-test / code1266

Новые сессии и пресеты AI-агентов по умолчанию используют Ubuntu AI. Существующие
настройки сохраняются. При выбранной Ubuntu AI миграция недоступна и отображается
серой. В списке окружений добавлена подпись «Используется для AI-агентов».
Ubuntu X11 Test убрана из стандартного каталога; совместимость старых записей сохранена.

Разработчик подтвердил успешную миграцию и работу всех агентов. Текущая рабочая
среда проверена как Ubuntu AI. По явному поручению удалён оставшийся после ошибки
скрытый контейнер Ubuntu X11 Test; обычная Ubuntu и Ubuntu AI сохранены. Исправлено
удаление закрытых каталогов и недоступных для stat файлов внутри собственного
временного контейнера без перехода по символьным ссылкам.

Общий X11-рендерер и исправления сохранены. Рабочий стол и программы обычной Ubuntu
устанавливаются отдельно в её собственное окружение. Новую сборку и обычную Ubuntu
с X11 ещё нужно проверить на устройстве. GitHub-публикация не выполнялась.

[Проверки](VALIDATION-v1.7.51-x11-test.md).

# Current private1.7.50 / code1265

Screenshot144851 on49 identified exact APK denial opening
rootfs/tmp/termux-fork-session-001136f4-58c5-44b1-94da-4690fd10ec7f.
SessionShell defines it as an app workspace-sessions bind, not saved preset refusing copy.
Added mount_placeholder for strict UUID session directory endpoints and exact rootfs/tmp
termux-jobs-{setup,gemini,claude,ai,codex}. Skip payload, preserve empty placeholder.
No global tmp exclusion, source deletion/chmod, history/preset deletion or broad failure
skipping. Nearby lookalike projects still copy; unknown denied tmp directories still fail.
Existing Android mount exclusions retained, exact failed path diagnostics retained.

Actual native Termux Python (through current Ubuntu/proot) read-only /tmp walk completed
1935 entries without errors. Full walk reached725237 then SOURCE_CHANGED at empty mode000 rootfs/workspace. Separate rootfs walk excluding root home/workspace passed152440. User explicitly authorized deletion; only that empty directory removed via fd-relative rmdir, no recursion/chmod. Follow-up system scan excluding only root home completed152439/29.2s without errors; workspace absent. Final private50 APK built/audited/delivered, real migration pending developer test. Production does not exclude arbitrary workspace dirs. Details in validation report.
49 Python fixtures and31 native migration fixtures passed. Automatic prepare-migration token now enables only empty mode000 workspace rmdir under lock/idle/cancel check before snapshot; normal/removal plans do not. Nonempty/ordinary/link paths retained; cleanup reported in plan UI. Follow-up actual system walk excluding root home passed152439. APK not installed by assistant,
no real source clone/environment-remove/locks or live session change performed. Sole installed change: explicit user-authorized empty rootfs/workspace rmdir above. No GitHub publication.

---

# Current private1.7.49 / code1264

Developer screenshots143640/143648: still SCAN_ACCESS_DENIED13 after48; only baseTermux
open. Previous exclusions did not fix all APK-context failures. Cannot assume keys/busy
source. Read-only installed exported script path lookup from this proot returned ENOENT;
not proof of absent app helper (private host files need not be mapped into guest).

Existing walker scan_path/scan_operation were discarded on serialization.49 carries
bounded root-relative path and fixed operation to local UI, sanitizes control/bidi and
refuses outside/parent paths; includes actual BuildConfig version. No file content or raw
exception text, no persistent/network diagnostics. Failures still block copy. This is
precise diagnosis, NOT a proven migration fix. Need Android-context path from next retry.
Native26+Python44 fixtures verify helper, final JVM/APK results in validation. No installed
APK, source chmod, session stop, native real migration/removal or publication performed.

---

# Current private1.7.48 / code1263

Read-only fd-relative scan in Ubuntu/proot reproduced errno13 at rootfs/apex,
rootfs/data/app and rootfs/data/dalvik-cache. Separate absolute directory reads showed
all three empty; native proot-distro sources explicitly bind those Android host endpoints.
This matches the app SCAN_ACCESS_DENIED stage, but exact Android process path and successful
actual migration still require device retry. No source locks/clone/chmod/removal performed.

Fixed exact Android mount directories are retained as empty placeholders by scan/copy,
never descended or opened again for source xattrs. Similar project names/non-directory
objects retain normal handling. UI discloses exclusions before confirmation. No general
selection UI added; user was discussing a possible later selective mode. Ordinary errors
still fail closed and keys are not excluded. Termux Python lacks os.link and os.*xattr;
fd Bionic xattr and linkat fallbacks added and tested with actual Termux Python fixtures.
Keyboard explanation embeds the exact workspace_interactive_input vector from the left button in the top-right toolbar group. See VALIDATION-v1.7.48-x11-test.md.

---

# Current private1.7.47 / code1262

Active issue: screenshot134207 shows FILES_ACCESS_DENIED errno13 during migration.
Cause not proven. Read-only partial native interpreter walk under current Ubuntu/proot:
28681 directories / 25s, no errors or owner-read/search-restricted directories in visited
portion. Not native Android app context; no real locks/snapshot/clone/remove performed.
New fixed access stages identify locks, /proc listing, source structure, snapshot and copy;
no personal names/exception text emitted. Do not relax source-busy or no-follow checks.
Next device action: retry migration and inspect exact stage before changing permissions.

All supported desktop menus now contain automatic keyboard checkbox, enabled by default,
saved per environment. Disabled focus/tap events are consumed without keyboard actions;
manual toggle remains unconditional. No X11 native/server changes or agent chat changes.
See VALIDATION-v1.7.47-x11-test.md for checks and final artifact.

---

# Current private1.7.46 / code1261

Screenshot132252 shows OPERATION_FAILED, not a confirmed missing dependency. Read-only
Termux Python probe (through Ubuntu/proot) found Python3.14.8/proot-distro5.9.0 and expected
module constants. This is not the actual Android app execution context; exact cause not
proven yet. Fix root-to-base traversal to use O_PATH for ancestors, O_RDONLY only at final
base, retaining O_NOFOLLOW. Android searchable ancestors need not be listable. Native
errors now distinguish storage/process/files access, missing source and errno; generic UI
no longer implies missing Python/proot for every failure. Missing binary/import contract
can explicitly prepare dependencies and retry; no installation suggested for access errors.

Plan now locks only selected source; clone still locks ubuntu+ubuntu-ai. Independent AI
may remain running while an unused old Ubuntu is removed. All clone safety guards remain.
Private code1260→1261, same identity. No real installed migration/remove/install/stop or
GitHub publication authorized/performed. Native tests use repo fixtures. Device retry needed.
See VALIDATION-v1.7.46-x11-test.md for final checks/artifact.

---

# Current private1.7.45 / code1260

Ubuntu AI runtime is selected for all four CLI agents via existing persisted
codexEnvironment=ubuntu|ubuntu-ai (legacy Codex termux retained). Existing profiles default
to their old Ubuntu. New editor chooses AI when AI exists or normal Ubuntu is absent;
otherwise defaults to normal Ubuntu until explicit migration. Fresh AI uses the existing
confirmed environment installer under the ubuntu-ai alias, never resets an existing root.

AgentEnvironmentManager drives user-confirmed native Python plan/clone/remove. Only modern
proot-distro5 containers/lock contract accepted for destructive operations/migration.
No real rootfs clone/removal/package install or APK installation performed by assistant.
Native tests use repository-owned synthetic roots. Read-only descriptor compatibility was
checked on the real base/ubuntu/rootfs/manifest paths without scanning or locking them.
90 JVM +31 Python tests pass; isolated agent-preparation regression script also passes.

See AGENT-ENVIRONMENTS.md and VALIDATION-v1.7.45-x11-test.md. No GitHub publication.
Do not remove Ubuntu X11 Test or normal Ubuntu automatically. All ordinary X11 features,
Evince1.7.44 fix and existing display/service owner logic are retained.

---

# Current private1.7.44 / code1259

Fix dpkg requested-selection vs actual-package-status confusion throughout X11 program,
installed-catalog, desktop-component and Firefox checks. A failed remove can leave
`deinstall ok installed`: this is configured, as is hold; reinstreq/unpacked/config-files
remain rejected. Termux Evince removal includes only installed evince-help +evince,
explicitly disclosed before confirmation. Other reverse dependencies still cause dpkg
refusal. No force/autoremove/new general removal plan, shared libraries are preserved.
Post-remove verifies actual absence. No installed package/selection/session modifications
by assistant, no migration of AI agents or deletion of Ubuntu X11 Test in this task.

Read-only version check after developer's dependency repair now returns GNOME Document
Viewer48.4. This confirms the missing math-library fix, not GUI/PDF rendering on device.
Main Programs navigation/inline jobs/animated button and Alpine thumbnail fix retained.
See VALIDATION-v1.7.44-x11-test.md for final evidence.

---

# Current private1.7.43 / code1258

Programs main has three navigation entries; catalog/search/pagination/install live in
Search and install programs below Installed. Main retains active-job status but no full
catalog widget. Subsection Back returns to main; closing main does not cancel jobs.
One fixed Prepare and refresh catalog action uses catalog-install after confirmation.
APT guest script checks configured catalog-tool packages before installing; prepared
tools cause only one index refresh, missing tools install then refresh DEP-11 metadata.
Native Termux/Alpine perform index refresh without AppStream. Existing typed target/id
validation, APT retry/trust/dependency/cancel semantics remain.

Document Viewer is Evince, not Mousepad. Read-only evince --version fails at the Android
linker: missing libandroid-complex-math.so required by libsynctex. Status says evince48.4-1
and libsynctex1:2026.0-2 configured; libsynctex Depends names only zlib. Read-only ELF graph
checks67 Termux binaries/libs and finds just this unresolved library name. Added explicit
libandroid-complex-math to Termux PDF group and pkg:evince alias. Missing helper makes
Recommended PDF incomplete; developer can select Install to add it without removing
Evince. Other distributions unchanged. No installed package was changed by verification.

44 relevant JVM UI tests and33 Python routing/catalog/retry tests passed. New tests cover
three-section navigation, single preparation action whether ready/missing, no duplicate
install of configured catalog tools, failed preparation, native refresh without AppStream,
and Termux-only Evince dependency mapping. Actual PDF GUI after repair still unverified.
No release/publication requested. Keep1.7.42 Alpine creator fix and frame-only border.

---

# Current private1.7.42 / code1257

Separate Programs button next to Terminal/Desktop, ordinary menu action in all job
states. Switch menu now contains only switch/return and conditional Stop. Programs
main Back closes the dialog instead of opening the switch menu. A shared server-border
Drawable animates only that button,16ms schedule, while the selected environment has
an installing/cancelling program job and its Programs dialog is closed. Existing main
activity poll updates only its active flag. Selected visible channel polls job state
while installation continues even with no desktop surface/dialog; sync resumes this
read-only polling when returning. Hidden channels/Activity do not animate; service
installation ownership/cancellation remains unchanged. No agent frontend edits.

New Alpine finding: live Tumbler and Thunar share a private bus, both have the existing
preload. Decoder compatibility works, but saving the PNG thumbnail invokes separate
Glycin creator with its own sandbox selector. Isolated copied Alpine Tumbler/private
bus reproduces decoder success followed by Bubblewrap creator failure. Adding exact
libglycin-2 creator hook produces Ready +64x64 PNG289bytes with valid chunk CRCs and
fixture URI metadata. Installed environment/processes were read only, not patched.
Restart Alpine desktop after APK update for phone verification. Existing scoped
Glycin sandbox reduction remains; Android, Firefox and other distributions unaffected.

Latest Termux LibreOffice screenshots113610/113614 show failed downloads from HUST
for MariaDB/PostgreSQL. APT retry flags are present, but mirror/network errors persist.
No mirror/TLS/signature setting was changed. Error UI now keeps a compact summary and
an explicit Details window for the bounded full log, preserving navigation.

47 relevant JVM tests and3 compiler/ABI/private-bus Python tests passed. Real isolated
thumbnail regression evidence is in .verification/x11-programs-button. Actual frame
rate, touch layout, background/resume and thumbnails still require developer phone
checks. No publication authorized; use this private APK over code1256 without resets.

---

# Current private1.7.41 / code1256

Latest additions: recommendations/installed navigation remains enabled during catalog
refresh and checks; cached pages can be read without starting another job. Desktop menu
contains switch + programs, and Stop only while started (terminal and graphical views).
Switch checks components then launches or offers explicit installation. Only the acknowledged
check/install reply can complete that intent; old snapshots and closed owners cannot.

Inline per-program install progress with Stop; window dismissal/tab switches leave
X11ProgramInstaller running in its existing foreground service. On reopen, the active
program card is reconstructed from the service snapshot; it does not start a second job.
Installed status includes a right-side trash action and explicit confirmation. Recommended
shortcuts remove only their named main package (e.g. LibreOffice Writer outside Termux),
retaining shared dependencies and other applications; confirmation names that package.
Server authorizes a fixed recommended package only with previously verified same-target
installed group state, or an entry from its installed catalog. Existing core protection,
dependency checking and exact owned-process cancellation remain.

APT gets command-local retries3 and HTTP/HTTPS idle timeout30s in desktop/program/Firefox
flows. No mirror switch, global config edit, signature/TLS bypass or outer transaction
retry. Alpine APK retries are not implemented: avoid unsafe transaction retries; the
reviewed apk-tools3.0.8 cache-predownload mechanism is recorded in ignored evidence for
future work. Failed package jobs still require a fresh installed-state check.

Alpine preload now starts before its private D-Bus, with idempotent session fallback.
Real isolated bus fixture proves environment inheritance; device thumbnails still need
verification. Inherited reduced Glycin decoder isolation remains documented in1.7.36.
No installed session/environment changed by verification. Device checks: install Evince
Termux, switch tabs/close and reopen program list mid-download, Stop, installed trash,
and Alpine image thumbnails after desktop restart. No publication authorized.

---

# Current private1.7.40 / code1255

Includes1.7.39 status/profile fixes. Adds read-only bounded XFCE GetState Idle readiness + verified WM; property-based window checks stay fallback. Earlier automatic-show issue not claimed device-fixed. Read-only state check after developer screenshots094043/094046: Alpine has no managed PRoot launcher/journal or HTTPS DNS journal; Termux has PRoot wrapper/journal but no HTTPS DNS journal. Alpine YouTube page opens, cause unconfirmed; do not attribute it to toggles. No installed session was altered. Collect device automatic-show and failure-status results before publication.

---

# Current private X11 v1.7.39 / code1254

Follow-up profile-path and status-reset fixes are included in this build. Same package/certificate,1253→1254, no data resets. Service retains known program items/DNS state for same-target non-package-changing jobs; UI retains known items on browser-setting/check errors. Package-changing failures invalidate uncertain states. Generic error no longer suggests desktop absence. DNS supports fixed legacy and XDG profile roots; symlink/external path/journal checks remain. Developer confirmed native Termux YouTube works with PRoot on and HTTPS DNS off. No new device verification is inferred. Previous pending notes below are historical.

---

## Pending follow-up after private1.7.38 (not yet in delivered APK)

Developer screenshots093511/093515 confirm native Termux Firefox loads YouTube
with PRoot compatibility enabled and DNS HTTPS disabled. Keep DNS checkbox optional;
this does not establish Alpine behavior or a causal DNS/PRoot relationship.
Alpine profile is read-only confirmed under /root/.config/mozilla/firefox rather than
/root/.mozilla/firefox. DNS helper now accepts both fixed relative roots, with10
profile tests passing. Generic program error text no longer suggests desktop missing.
Browser setting errors retain known installed/missing program status in service/UI;
package-changing failure/cancel still invalidates uncertain statuses. Relevant UI
suite passed after adding regression coverage. No new APK built/installed for these
follow-up source changes; collect remaining developer findings before next build.

# Current private X11 v1.7.38 / code1253

Fixed service slots separate Termux/Ubuntu/Test Ubuntu/Debian/Alpine processes, displays97..101 and private control paths. One desktop per environment, shared by exact session handles; last attached close stops only its slot. Firefox Alpine direct /usr/lib launcher is accepted; optional per-profile HTTPS DNS for native Termux/Alpine uses Mozilla Cloudflare endpoint with ordinary DNS fallback, local journal and edit-preserving undo. Hidden early Surface renderer attach is device-unverified. No rootfs reset or agent changes. See VALIDATION-v1.7.38-x11-test.md; earlier entries below are historical.

---

<!-- Private X11 readiness and Alpine APK catalog fix 2026-10-21 -->
Текущая частная сборка: **v1.7.37-x11-test / code1252**.
Автоматический показ X11 использует типы и видимость окон, а не их заголовки.
После первого таймаута остаётся ручной показ, а ограниченная проверка продолжается
реже и открывает готовый стол автоматически. Постоянного наблюдателя нет.
Исправлен разбор законных имён APK Alpine с заглавными буквами, подчёркиванием и
одной буквой; каталог, рекомендуемые и установленные программы снова могут читаться.
Опасные аргументы/пути по-прежнему отклоняются. Данные не очищаются, пакеты не меняются.
Совместимость декодера Alpine из1.7.36 сохранена вместе с её ограничением изоляции.
[Проверки и ограничения](VALIDATION-v1.7.37-x11-test.md). Ниже — исторические этапы.

<!-- Private Alpine startup compatibility and startup UI 2026-10-21 -->
Текущая частная сборка: **v1.7.36-x11-test / code1251**.
Исправлена воспроизведённая ошибка загрузки PNG/SVG и встроенной иконки libwnck
в Alpine/PRoot. Малый модуль совместимости подключается только к дереву её стола;
системные библиотеки не заменяются. Он восстанавливает проверку существования файла
и запускает декодер Glycin без недоступной песочницы Bubblewrap. Это ослабляет
изоляцию обработки изображений в Alpine; Android и песочница браузера не отключаются.
Экран запуска всех столов: эмблема X11, заголовок, полоса реальных этапов и подпись.
Один стол одновременно. Данные не сбрасываются, новые окружения не создаются.
[Проверки и ограничения](VALIDATION-v1.7.36-x11-test.md). Ниже — исторические этапы.

<!-- Follow-up investigation, no new APK -->
Alpine v1.7.35 startup abort134 confirmed in developer screenshots; automatic X11
release worked. See [bounded diagnosis and pending checks](X11-ALPINE-STARTUP.md).
Next startup-stage UI is agreed, not yet implemented. No rootfs changes performed.

<!-- Private X11 lifecycle fix 2026-10-21 -->
Текущая частная сборка: **v1.7.35-x11-test / code1250**.
После завершения стола или ошибки запуска X11 освобождается автоматически.
«Остановить графический рабочий стол» оставляет базовую терминальную сессию открытой.
Остановка проверяет окружение; во время освобождения новые операции не запускаются.
Код завершения XFCE берётся из сообщения дочернего процесса, когда оно доступно,
а не из ожидаемого SIGKILL защитной группы. Один графический сервер одновременно;
независимые одновременные столы не реализованы. Данные и окружения не очищаются.
[Проверки и ограничения](VALIDATION-v1.7.35-x11-test.md). Ниже — исторические этапы.

<!-- Private Alpine/Termux X11 update 2026-10-21 -->
Текущая частная сборка: **v1.7.34-x11-test / code1249**.
Переход на X11 и существующая плашка управления добавлены для **Alpine и Termux**.
Alpine использует свой APK и musl; Termux запускает стол напрямую в своём PREFIX,
без PRoot/Ubuntu. Проверка, установка, XFCE, Fcitx5, программы и звуковой мост общие
по интерфейсу, но команды и пакеты соответствуют выбранной системе.
Диагностический наблюдатель и GTK-модуль убраны из запуска и APK для всех четырёх
систем. Сохранены необходимые сигналы готовности стола и фокуса ввода. Диагностики
в плашке нет. Архив исходников экспериментов остаётся в проекте вне ресурсов APK.
Работает один стол одновременно; отдельные одновременные серверы сюда не входят.
[Проверки и ограничения](VALIDATION-v1.7.34-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 media update 2026-10-21 -->
Текущая частная сборка: **v1.7.33-x11-test / code1248**.
В «Программы рабочего стола → Рекомендуемые» добавлен обычный пункт **Parole**:
описание, «Установить / Установлено», без отдельного меню плеера. Установка включает
декодеры GStreamer для распространённых видео и аудиофайлов; поддержка зависит от
кодека. Плеер не входит в базовую установку рабочего стола.
В базовую установку Debian/Ubuntu включены звуковые компоненты. После обновления
проверьте стол и установите недостающие компоненты; затем перезапустите стол.
Звуковой мост запускается вместе со столом через приватный UNIX-сокет с cookie,
без TCP/микрофона. Остановка принадлежит тому же владельцу, что и графический стол.
[Проверки и ограничения](VALIDATION-v1.7.33-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 certificate/progress update 2026-10-20 -->
Текущая частная сборка: **v1.7.32-x11-test / code1247**.
В базовую установку и проверку X11 Debian/Ubuntu добавлены `ca-certificates` и
проверка непустого хранилища доверенных сертификатов. Firefox также подготавливает
их перед HTTPS-репозиторием Mozilla; проверка TLS и подписей не отключается.
Окна установки рабочего стола и программ показывают после процентов полученный
объём и общий размер, если APT сообщает их. Это округлённые измерения APT, не
пересчёт числа файлов в байты. При распаковке объём скрыт, прогресс этапа остаётся.
Несколько одновременных столов и отдельные новые окружения в эту сборку не входят.
[Проверки и ограничения](VALIDATION-v1.7.32-x11-test.md). Ниже — исторические этапы.

<!-- Private Debian X11 update 2026-10-20 -->
Текущая частная сборка: **v1.7.31-x11-test / code1246**.
Рабочий стол XFCE добавлен в существующий **Debian**: откройте его обычную сессию,
затем «Рабочий стол X11 → Проверить» и подтвердите установку компонентов.
Новые окружения не создаются. Пакеты, каталог программ, настройки и совместимость
Firefox принадлежат выбранному Debian. Доступны те же переключение видов, ввод,
прогресс/отмена установки, рекомендуемые/установленные программы и исправление
прав ярлыков. Перед переходом между рабочими столами остановите предыдущий.
Разработчик подтвердил работу Firefox в Ubuntu через Applications и заново созданный
ярлык рабочего стола. Debian X11 ещё требует проверки на устройстве.
[Проверки и ограничения](VALIDATION-v1.7.31-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная сборка: **v1.7.30-x11-test / code1245**.
Совместимость PRoot — галочка в карточке Firefox под «Установить / Установлено»
в рекомендуемых и установленных. Нажатие открывает пояснение и подтверждение.
Закройте только Firefox: рабочий стол для переключения останавливать не нужно.
Новых значков, панельных ярлыков и команд не добавлено. Сохранён прежний запуск
через меню Firefox с подтверждённым обходом seals; обычный бинарник не изменяется.
[Проверки и ограничения](VALIDATION-v1.7.30-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная сборка: **v1.7.29-x11-test / code1244**.
Firefox → «Совместимость с PRoot» доступна и в рекомендуемых, и в установленных.
После установки браузера остановите рабочий стол, откройте этот пункт и подтвердите
включение. Пояснение описывает ослабление защиты общей памяти; параметр применяется
только при запуске Firefox через его ярлык. Там же можно отключить режим.
Временное удаление LXQt убрано; три известных остаточных ярлыка без резервных копий
убираются при следующем запуске Test-стола. Исправление прав ярлыков сохранено.
Обычный запуск пропускает лишнее декодирование фона, не меняя выбранные картинки.
[Проверки и ограничения](VALIDATION-v1.7.29-x11-test.md). Ниже — исторические этапы.

<!-- Private X11 update 2026-10-20 -->
Текущая частная проверка: **v1.7.28-x11-test / code1243**.
Оставлен XFCE; выбор LXQt, диагностика и плашка Garcon убраны с панели.
«Программы рабочего стола → Установленные»: исправление прав ярлыков и временное
удаление LXQt с подтверждением после остановки стола. Общие библиотеки и настройки
сохраняются. В рекомендуемых — Firefox из подписанного репозитория Mozilla без
Snap; запуск в PRoot ещё проверяется. [Проверки и ограничения](VALIDATION-v1.7.28-x11-test.md).
Предыдущие сведения ниже относятся к историческим этапам.

## Private X11 v1.7.27

Stop failed desktop, use Repair launcher permissions in Ubuntu X11 Test card, confirm, relaunch XFCE. Old filter is retired; no new off/on comparison. Read-only package-manifest check matches the LibreOffice Writer launcher. See VALIDATION-v1.7.27-x11-test.md.

## Private X11 v1.7.26

Device comparison pending: Test XFCE stabilization off first, then on, restarting between trials. Extra attributes counters diagnose content/permission/identity changes. LXQt dependencies updated, automatic Qt IME focus disabled temporarily. See VALIDATION-v1.7.26-x11-test.md.

## Private X11 v1.7.25

Compare LXQt in the same Test Ubuntu; keep XFCE for file-event/reload capture. Stop the desktop before selecting another. No other distribution/agent environment changes. See VALIDATION-v1.7.25-x11-test.md.

# Счётчики GTK · 1.7.24-x11-test

Для Ubuntu X11 Test добавлен небольшой диагностический модуль панели и рабочего стола: draw — рисование меню, add/remove — изменение пунктов, reload — запрос перезагрузки Garcon, enter/leave/motion — события указателя. Сбор счётчиков включается существующей кнопкой наблюдения; текст пунктов и содержимое событий не читаются. Модуль подключается только к новым процессам панели и рабочего стола тестового окружения, не заменяет GTK/Garcon и не меняет настройки. Это диагностика причины мерцания; исправление пока не заявляется. [Проверки и ограничения](VALIDATION-v1.7.24-x11-test.md). Ниже — предыдущие этапы.

# Наблюдение X11 и закрытие сессий · 1.7.23-x11-test

X11 запоминает сессию и конкретный запуск, из которого запущен рабочий стол. При её закрытии или завершении оболочки останавливаются графический стол и его программы. Закрытие другой сессии, смена вкладки и возврат в терминал стол не останавливают. Перед закрытием добавлено предупреждение о несохранённой работе. Наблюдение дополнено счётчиками изменений содержимого меню XDamage и движения указателя, без чтения изображений и текста ввода. Причина мерцания пока не установлена. Убрано наследование допклавиш и их подсветки между чатами агентов и обычным терминалом; Согласование прокрутки с новой высотой сообщений перенесено перед отрисовкой кадра для всех чатов агентов, без передачи фокуса из поля ввода. [Проверки и ограничения](VALIDATION-v1.7.23-x11-test.md). Ниже — предыдущие этапы.

# Наблюдение X11 · 1.7.22-x11-test

В «Диагностике X11» запись запускается и останавливается вручную, без прежнего ограничения в три минуты. Закрытие панели запись не останавливает; новый запуск заменяет текущую запись в памяти. Добавлены время событий, категории всплывающих окон, состояние кнопок мыши и отдельный последний снимок настроек фона. При потере поверхности отменяются удерживаемые кнопки и отложенное перетаскивание. Это диагностика, устранение мерцаний пока не подтверждено. [Проверки и ограничения](VALIDATION-v1.7.22-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.21-x11-test

Диагностика X11 переведена на компактную панель сессий: «Сводка»/«События», закреплённые действия копирования и экспорта в Download. При смене вкладки сохраняется соединение рендерера, пересоздаётся только Android-хост поверхности. Для фона используется постоянное имя X11-экрана screen; прежняя выбранная картинка переносится только при отсутствии собственного выбора у этого имени. [Проверки и ограничения](VALIDATION-v1.7.21-x11-test.md). Ниже — предыдущие этапы.

# Диагностика X11 · 1.7.20-x11-test

Исправлена полная пересборка верхней панели при смене состояния X11-ввода: обновляется только подсветка кнопки, название и навигация сохраняются. Добавлены ограниченный журнал событий всплывающих X11-окон/фокуса и метаданные Android-поверхности/клавиатуры. Это диагностика причины Applications, а не заявление об устранении мерцания. Проверяется декодирование выбранных фонов; только отсутствующий штатный фон заменяется встроенным нейтральным PNG. Выбранные пользовательские файлы сохраняются. В «Диагностике X11» есть обновление, копирование и экспорт в Download. [Проверки и ограничения](VALIDATION-v1.7.20-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.19-x11-test

Точечное сравнение с 1.7.18: XFWM4 запускается с композитингом (`--compositor=on`). Остальные механизмы рабочего стола сохранены. Проверяются Applications, чёрный фон и изменение размеров окон; устранение дефектов ещё не подтверждено. [Состав и проверки](VALIDATION-v1.7.19-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.18-x11-test

Во встроенном X11 отключён второй Android IME: ввод обслуживает поле приложения. Это точечная проверка причины мерцаний Applications, результат нужен на устройстве. Каталог и установленные программы сохраняются отдельно по окружениям между запусками; ручное обновление заменяет снимок, копии не накапливаются. [Состав и проверки](VALIDATION-v1.7.18-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.17-x11-test

«Установленные программы» с подтверждаемым удалением и защитой базовых компонентов. Журнал установки следует за последней строкой, сохраняя позицию при ручной прокрутке вверх; стрелки страниц каталога — векторные. Убраны лишние повторные запросы IME при уже полученном фокусе. Разработчик подтвердил Return в браузере. Мерцание Applications исчезает после открытия/закрытия ввода; причина пока не установлена, исправление не заявлено. [Состав и проверки](VALIDATION-v1.7.17-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.16-x11-test

«Рекомендуемые» и каталог AppStream с поиском, Return из текстовых IME-событий, Tumbler с Ristretto, базовые шрифты/значки и возврат к уже показанному столу. «Диагностика X11»; чёрные обои ещё исследуются. [Состав и проверки](VALIDATION-v1.7.16-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.15-x11-test

Программы рабочего стола с описаниями и проверкой установки, отдельная установка по кнопке, прогресс и оценка оставшегося времени этапа. Остановка скрыта до запуска. Масштаб и ввод сохраняют подтверждённое поведение 1.7.14. [Состав и проверки](VALIDATION-v1.7.15-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.14-x11-test

Стабильное разрешение рабочего стола при появлении клавиатуры; видимая часть обрезается без смены масштаба. Разработчик подтвердил автоматическую клавиатуру в 1.7.13. [Состав и проверки](VALIDATION-v1.7.14-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.13-x11-test

Возврат фокуса основному окну для автоматической клавиатуры и диагностика вызова. Разработчик подтвердил ручной ввод без задержек в 1.7.12. [Состав и проверки](VALIDATION-v1.7.13-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.12-x11-test

Автоматическое открытие через Fcitx использует наш ввод, ручная кнопка сохранена. Набор передаётся во время ввода, с защитой от повторения при закрытии. [Состав и проверки](VALIDATION-v1.7.12-x11-test.md). Ниже — предыдущие этапы.

# Проверка X11 · 1.7.11-x11-test

Клавиатура через окно основного приложения и индикатор запуска в области рабочего стола. [Состав и проверки](VALIDATION-v1.7.11-x11-test.md).

# Текущая проверка: 1.7.10-x11-test/code1225

Клавиатура X11: ручное переключение, снятие подсветки после закрытия и
пробные сигналы фокуса Fcitx5 для GTK3/Qt5. Добавлены явный запуск XFWM4 без
композитинга, детали запуска и возврат в плашку после проверки/установки.
[Состав и проверка1.7.10](VALIDATION-v1.7.10-x11-test.md).
Ниже — предыдущие записи. GitHub остаётся v1.7.5.

# Текущая проверка: 1.7.9-x11-test/code1224

Управление X11 пальцем как тачпадом вместо постоянно зажатой левой кнопки.
XFCE предыдущей1.7.8 запущен и подтверждён разработчиком. Подробности:
[проверка1.7.9](VALIDATION-v1.7.9-x11-test.md).
Ниже — предыдущие записи. GitHub остаётся v1.7.5.

# Текущая проверка: 1.7.8-x11-test/code1223

Исправлена проверка служебной папки X11 (`CONTROL_PATH`): сравнение файловой
идентичности вместо текста путей Android. Внутреннее меню X11 и окна установки
используют общую тему и карточки агентов. Состав и проверка:
[VALIDATION-v1.7.8-x11-test.md](VALIDATION-v1.7.8-x11-test.md).
Ниже — записи предыдущих сборок; GitHub остаётся v1.7.5.

# Текущая проверочная сборка: 1.7.7-x11-test

Рабочий стол в области выбранной сессии; явная Ubuntu X11 Test
(`jobs-x11-ubuntu`), проверка/подтверждённая установка XFCE с прогрессом,
возврат в терминал и отдельная остановка. Пакеты — APT выбранной Ubuntu;
терминал и рабочий стол не отрисовываются одновременно. Реальный XFCE этого
этапа ещё проверяется на устройстве; прочие дистрибутивы не перенаправляются.
Подробности: [VALIDATION-v1.7.7-x11-test.md](VALIDATION-v1.7.7-x11-test.md).
Стабильный GitHub остаётся v1.7.5. Ниже — предыдущие записи.

# В работе: встроенный X11, этап 1

Разработчик разрешил первый этап встроенного X11 и тесты. Стабильный1.7.5
сохранён в Download/Termux-Jobs-1.7.5-BACKUP с исходным code1220; увеличивать его
только при необходимости отката. Тестовый APK code1221, пакет/подпись прежние.
Плашка у локальных неагентных сессий на месте лимитов; без пункта шестерёнки.
Первый экран — private X11 Activity/service и реальное протокольное тестовое
окно; не XFCE, не замена TerminalView пока. Меню: «Вернуться в терминал» /
«Остановить графический рабочий стол». План/границы: docs/fork/X11-INTEGRATION.md.
Не устанавливать APK, не менять пользовательские среды/процессы, не публиковать.
Готов первый тестовый APK/SHA в Download, копии сверены.707 JVM/6 Python,
release build/runtime/signature/ABI audit успешны. Native SDK готовый официальный,
не собственная пересборка. VALIDATION-v1.7.6-x11-test.md содержит границы.
Ожидается первый device test графики/ввода/возврата/остановки разработчиком.
Ниже история предыдущих задач.

---

# Релиз v1.7.5 / code1220

Пользователь разрешил GitHub-релиз текущего поведения. Stable сохраняет code1220
проверочной1.7.5-test: не повышать для повторного обновления разработчика.
README/changelog/release/validation/update notes обновлены. Stable собран,
APK audit пройден;697 JVM+171 Python/Node/native evidence подробно в VALIDATION.
Разработчик подтвердил предыдущие сообщения и запуск нового Codex-чата.
Опубликован stable/latest v1.7.5, тег bc97f9ea. Четыре assets скачаны обратно
и побайтно/по SHA совпали; release body совпадает с подготовленным. Анонимные
latest API и update.json проверены; документы доступны в теге. APK SHA-256:
a92a97f42bc0c5d077ff3c72ca15dadb07d56fdadb2528f3d7931a59320c5861.
Пользовательские процессы не менялись; code1220 одинаков с1.7.5-test.
Новые задачи пока не согласованы.
Ниже исторические этапы; их запрет публикации относился к тем задачам.

---

# Текущая сборка: 1.7.5-test / code1220

Исправление Earlier messages: queued click во время polling, отдельные страницы
Codex вместо возвращения tail100, local windows внутри большого native turn,
сохранение old rows при partial knownMessages, правильный null cursor/конец
истории и прокрутка prepend. Открытие чата из AgentChatCache/лимиты/run-generation
сохранены; metadata exhausted/trimmed лишь управляет пагинацией.
Проверить combined cache reopen+history тесты: пользователь особенно просит
не вернуть сбой открытия чата. Код bridge применяется только новым native
процессом; рабочую сессию не останавливать и не patch на месте.
Готово:697 JVM-тестов без ошибок, сборка и аудит APK прошли. APK/SHA в Download,
копии сверены. VALIDATION-v1.7.5-test.md содержит доказательства/ограничения.
Ожидается проверка разработчиком. GitHub не публиковать.

Скриншот нового Codex показал thread/read -32600. Read-only диагностика: процесс
продолжал id, для которого в sessions/archived_sessions не было rollout-файла;
путь проекта с нужным регистром существовал. Отдельный fixture воспроизводит
thread not loaded на втором app-server до появления истории, после turns читает.
Добавлены materialized boolean в статус/отказ автоматического bind new→id при
false, понятная локализованная ошибка. Не создавать новый разговор автоматически
для existing bad id и не трогать процессы; разработчику выбрать New chat вручную.
Уточнение разработчика: demo-chat — название чата; папка DEMO-PROJECT.
Найден дополнительный путь: старый status после явного New в редакторе мог
снова привязать старый thread. codexBindingBlockedRun защищает сохранённый
выбор до следующего запуска с новым socket, включая reload библиотеки.

---

# Текущая сборка: 1.7.4-test / code1219

Разработчик уточнил: в очистке нужен выбор удаления целых окружений, не только
просмотр. Добавлены галочки environment в общем выборе и кнопка из системного
состава; штатный remove поддерживаемых modern контейнеров, path/dev+inode,
проверки занятости/нативных locks, согласие, без force/kill/filesystem fallback.
Пресеты остаются. Во время native remove отмена не предлагается, предупреждение
есть до запуска. Сам PREFIX Termux остаётся read-only.
Сканирование перенесено внутрь окна с тем же header/spinner, что у бэкапа.
Восстановленные копии теперь в общем выборе, отдельный cleanup action убран.
STORAGE-CLEANUP.md содержит ограничения. Сборка/аудит прошли: 690 JVM и171 Python тестов успешны.
APK/SHA переданы в Download и сверены. VALIDATION-v1.7.4-test.md содержит
результаты/ограничения. Ожидается проверка на устройстве. Никаких реальных удалений пользовательских сред и
публикации на GitHub в этой задаче.

---

# Текущая сборка: 1.7.3-test / code1218

По замечанию разработчика очистка теперь показывает полный состав HOME/PREFIX,
систем окружений и данных библиотеки. HOME/истории оболочек выбираются по
верхнеуровневым элементам и свежему защищённому снимку. Команды/заметки/пресеты/
недавние записи — по id с проверкой неизменности коллекции, без удаления всего
workspace-v1.json. PREFIX/системные деревья показаны read-only с пояснением;
полный сброс исходных систем из работающего приложения не реализован.
Дубликат удаления восстановленных окружений из меню бэкапа убран.
Сценарии/ограничения описаны в STORAGE-CLEANUP.md. Пользовательские данные
во время разработки не очищать; публикация на GitHub не авторизована.
Финальная сборка/аудит прошли; 690 JVM и166 Python тестов успешны.
APK/SHA в Download, копии сверены. VALIDATION-v1.7.3-test.md содержит результаты.
Ожидается проверка разработчиком на устройстве.

---

# Текущая сборка: 1.7.2-test / code1217

Одобрены очистка под «Настройки приложения», обязательное согласие перед
удалением, проверки связанных сессий/блокировок и закрытие только через кнопку
«Отмена». Категории/ограничения — STORAGE-CLEANUP.md. Никакие установленные
окружения/пользовательские данные в рамках разработки не очищать.
Добавлена «История» слева от «Закрыть» только в установленном списке пакетов;
размеры исходного окна не менять. Общая по rootfs история300 записей: observed
изменения + результат команд нашего менеджера, включая ошибку/отмену/unknown.
Предыдущий выбор версий APK/DNF/Zypper сохранён. GitHub пока не публиковать.
APK/SHA переданы в Download, копии сверены. 686 JVM + 159 Python тестов прошли,
финальный APK/подпись/runtime проверены. VALIDATION-v1.7.2-test.md содержит
результаты и ограничения. Ожидается проверка UI/жестов разработчиком.

---

# Текущая работа: 1.7.1-test / code1216

Разработчик одобрил выбор версии только для Fedora/DNF, openSUSE/Zypper и
Alpine/APK. Void/Arch не расширять. Добавлен EnvironmentPackageVersions,
новый выбор/источник/предупреждения, прежний подтверждаемый запуск команд.
APK и SHA-256 в Download, аудит/676 JVM-тестов прошли. VALIDATION-v1.7.1-test.md
содержит факты/ограничения. Проверить выбор/установку на телефоне во всех трёх
системах. Не публиковать на GitHub без следующего указания.
Перенос AI Agents в отдельную Ubuntu отложен пользователем; не выполнять.

---

# Релиз v1.7.0 / code1215

Авторизована публикация stable v1.7.0 с окружениями, пакетами и бэкапами.
Итоговые проверки — VALIDATION-v1.7.0.md. 668 JVM/146 Python проходят,
APK проверен; меню удаления новых восстановленных копий требует устройства.
Исходная Ubuntu/AI Agents общая, не мигрировать. Неподтверждённые jobs-копии
не удалять из приложения по одному имени. Архивы при удалении копии сохранять.
Stable/latest v1.7.0 опубликован: тег v1.7.0 указывает на2b39c5d6.
Все четыре assets скачаны обратно и идентичны оригиналам. Анонимные latest API
и update.json подтверждают code1215/хеш/размер. Ссылки документации тега HTTP200.
Уточнение о независимости систем от основной Ubuntu внесено в release body
и документацию ветки отдельным коммитом без переноса тега/пересборки APK.
По отдельной просьбе пользователя штатно удалена незавершённая Debian-копия
jobs-6a650d598a1d без манифеста; другие записи окружений сохранены.
Не удалять другие данные без отдельного задания.

Ниже история разработки; старые «не публиковать»/ожидания проверок относятся
к соответствующим промежуточным версиям и не отменяют текущую авторизацию.

---

# Текущая проверочная сборка: 1.6.8-test/code1204

Исправлен Void OCI missing /bin/bash: ограниченный переход на native run с явным
/bin/sh, общий для SessionShell/ProjectFiles/команды входа. Старые Ubuntu/AI пути
сохранены. APK в Download; GitHub не публиковать. Проверить запуск/файлы Void.
Arch скачан в .verification и проверен: SHA совпадает, root shell /usr/bin/bash,
ELF AArch64. В рабочую среду Arch не устанавливался, ждёт теста разработчика.
VALIDATION-v1.6.8-test.md содержит факты и ограничения; особенно восстановление
Void без OCI-манифеста пока не проверено. Будущей версии нужен code>1204.
Старые APK ROLLBACK имеют меньший код, не передавать их как обновление.

---

# Текущая проверочная сборка: 1.6.7-test

Void/Arch добавлены в современный установщик, см. ENVIRONMENTS.md.
Feature code1203 заменяет прежний code1201: разработчик случайно установил
rollback1.6.6/code1202; подпись и хеш установленной APK подтвердили именно откат.
code1203 позволяет обновиться поверх без очистки. Будущий stable code>1203.
Старый rollback1202 уже не устанавливается поверх1203; при необходимости
подготовить новый откат с ещё большим кодом. Не передавать rollback как обновление.
Оба APK для Download; GitHub не публиковать без отдельного указания.
Результаты базовых проверок разработчика для Debian/Alpine/Fedora/openSUSE
записаны в VALIDATION-v1.6.6-test.md; Void/Arch ждут реального запуска.
Ubuntu и агенты остаются общими, миграцию в ubuntu-ai не выполнять.

---

# Текущая незавершённая публикация: 1.6.6-test

Проверочная сборка и APK отката в Download. GitHub пока не публиковать: ожидается
проверка разработчиком. [Изменения и ограничения](ENVIRONMENTS.md),
[проверки](VALIDATION-v1.6.6-test.md). Feature code1199, rollback stable1.6.5 code1200.
Будущему стабильному выпуску требуется code>1200 и update.json.
Ubuntu и агенты остаются общими; ubuntu-ai — только будущий план без миграции сейчас.
Не затрагивать design/codex-session-status-concept.png: ранее созданный отдельный макет.

---

## Latest private delivery: v1.5.19-test (1156)

## Current private rollback — v1.5.21-test

Developer cancelled the LoRA route. Production app/native sources restored exactly to6a9ab547 (v1.5.19-test), code1158/name1.5.21-test to update over1157. LoRA loader/training draft commits7c6d6182/9ffa1501 preserved in Git history, not current feature. Owned checkpoint downloader stopped; no LoRA training steps or trained adapter. Isolated preparation/partial downloads retained in ignored .tools/lora-training and .verification/lora-training; do not assume they are installed app models. No GitHub authorization in this task. design/ untouched. See RELEASE/VALIDATION-v1.5.21-test.md for final results. Main next direction is intentionally undecided by the developer.


Delivered APK/README/SHA in Download. LiteRT retains full canonical expected append vs fitted actual SDK history; exact historical edits/rights/key/runtime-prefix/reply cap/media/error/cancel checks retained. SDK25 fake-inference tests cover compacted reuse, changed dropped history and reporting-failure cleanup. Both local engines: explicit requested file-operation claim check using current-turn successful tool kinds, one correction then unconfirmed output; no constant prompt/inference overhead. No universal target/content/order/intent verifier; arbitrary successful commands opaque. Invalid claims repair-marked, excluded from future context/UI; permissions/approval/cancel/loops unchanged. Fixed noun vs past-verb/punctuation false positives; no replay of old successes. Full684app+145emulator, final88focused and25SDK checks pass; final build/runtime/APK/signature/native equality/limitedsecretscan/DownloadSHA pass. APK smaller due ZIP gap padding, not removed native payload. No persistence resets, model/native/environment/process/GitHub changes. Repeat Qwen task and two Gemma requests. LoRA/index work not started. See RELEASE/VALIDATION-v1.5.19-test.md. Next code>=1157. design/ untouched.

## Latest private delivery: v1.5.18-test (1155)

Delivered APK/README/SHA to Download. LiteRT optional current progress tried before canonical compaction; fixed reset/retention reason codes. Qwen installed template explicitly permits prose before calls: bounded plain prefix (inline filename allowed), single terminal complete typed block; reject recognized examples/fences/multiple/trailing/malformed, existing authorization/approval retained. No universal intent-detection claim. Read-only exported GGUF metadata/tensor directory reinspection,335tensors,1.94Belements, weights untouched.674app+145emulator pass,23actualSDK/fakeinference pass, final build/runtime/APK/signature/ABI/limitedsecretscan/DownloadSHA pass. No native/model/environment/session changes. Repeat same Qwen and Gemma device tests; speed not established for this build. See RELEASE/VALIDATION-v1.5.18-test.md. No publication; design/ untouched. Next code>=1156.

## Latest private delivery: v1.5.17-test (1154)

Delivered /sdcard/Download/Termux-Jobs-v1.5.17-test-arm64.apk + README/SHA. Llama JNI+4CPU runners: explicit final transient-feedback index and conservative exact-token checkpoint before its role boundary replaces tail64; existing fullsystem/chat/owner/prefix/budget128MiB/cancellation checks unchanged. All6JNI/CPU binaries rebuilt together; old libs mustnotmix. Native tiny test trimdisabled:505vs406/901cached, logits matchcold; actualQwen gainpending. Old batch reports found: CPU6 256/128 vs512/256 differences overlap, so baseline andselectedthreads retained; configured batch/ubatch/thread diagnostics added, no newbenchmarkmenu. LiteRT isolatedSDKbenchmark enabled beforeinitialize, optionalmetrics +tokenCount reported without invented cachehits. Same-owned exactsemantic-one-message append retainsConversation; currentfeedback merged into incoming user/toolresult, priorruntimefeedback fullybudgeted, originalsnotpersisted. Drops on changedrights/chat/schema/sampler/replycap, media, compaction/nonprefix, errors/cancel/leaks. External history annotations sanitized. SDKautoloadtoolsfalse, appactionpermissions unchanged. Dropdown restoresv1.5.11 sizes (340dpbound,wrapheightmin48,14sp,13/8padding,12corners) withcurrent22dpvectors/model/activehighlight; topselector/serverbuttonunchanged. Final670app+145emulator+22SDK/fakeinference pass; all4runnerssmoke+JNIloader+snapshotchecks pass; APK/runtime/signature/nativehash/limitedsecretscan/DownloadSHA pass. RealNPUmetrics/reuse, Qwenlatency andDPI need developercheck. NoGithub/env/process/weights changes. See RELEASE/VALIDATION-v1.5.17-test.md. Nextcode>=1155. design/untouched.

## Performance investigation after1.5.16 (no new APK)

See LOCAL-AI-PERFORMANCE-RESEARCH-v1.5.16.md; evidence under .verification/engine-research-v1516/. Developer report7281398394140515414 proves Qwen prefix_snapshot1423 after first pass, total95777ms vs previous181406ms, prefill82799vs166964ms (different histories/actions, no controlled speed guarantee). Screenshot action order still wrong: two reads before write, no readback after. Next cache candidate: current tail64 snapshot lands inside long mutable appFeedback; capture exact token prefix before feedback role instead, preserving scope/system/budget checks. Actual Qwen tokenizer shows candidate commonprefix grows with history while currenttailmisses; real tiny-model forced-snapshot inference restores505vs406tokens with cold logits matching. No production patch yet. CPU GDN and Q4_K2048x2048 microbenchmarks forward/reverse:6threads better than8, larger blocks sometimes improve pertoken work; not wholemodel profile. Native fastARM already enabled, OpenMPoff doesnotmean1thread. Pinned LiteRT0.17SDK HAS getBenchmarkInfo/getTokenCount and ExperimentalFlags.enableBenchmark; our adapter currently doesnotcollectmetrics and recreatesconversation. Actual NPU metrics/lifecycle reuse untested; cancellationrollback caveat. Read-only upstream MIT/Apache licenses/source fetched afterweb403. Separateengineartifactsfeasible, vendorbinaries/weights separate terms, noGithubauthorizednow. No sources/model/session/environment modifications. design/untouched.

## Latest private delivery: v1.5.16-test (1153)

Delivered /sdcard/Download/Termux-Jobs-v1.5.16-test-arm64.apk + README/SHA. Root cause of Qwen cold every tool: progress changes fullsystem, native cache correctly invalidates onfullsystem. Llama only now keeps base system stable; progress/correction are transient trailing appFeedback counted before fitting and protected from history removal. No source transcript mutation/falsecompaction, native scope/system checks unchanged. LiteRT existing path unchanged. Tiny-model integration through production Java request builder+packaged runner reused650/683tokens with changed progress, resets on chat and rights; real Qwen/hybrid latency still pending. CPU fixed/dimmed forbuilt-inqwen35-2b, noGPU picker; oldGPU selection normalized only ineditableEngineSettings, appliedwithsave. Dropdown slightly larger200×52dp/popup216; mainselector/serverbuttonunchanged. Final664app+145emulator+17SDK/fakeinference pass, APK/runtime/signature/secret-scan/DownloadSHA pass. NoGitHub/process/environment/modelchanges. See RELEASE/VALIDATION-v1.5.16-test.md. Next code>=1154. design/ untouched.

## Latest private delivery: v1.5.15-test (1152)

Delivered /sdcard/Download/Termux-Jobs-v1.5.15-test-arm64.apk + README/SHA. User clarified ONLY dropdown rows smaller: about180×48dp, popup196dp, text12sp/vector18dp. Main selector and Server+Agent unchanged. Highlight/model/robot/tap/long-tap retained. Full660 app+145 emulator tests, build/runtime audit/APK audit/Download SHA passed; real DPI/readability pending. No inference/permissions/history changes or GitHub. See RELEASE/VALIDATION-v1.5.15-test.md. Next code>=1153. design/ untouched.

## Latest private delivery: v1.5.14-test (1151)

Delivered /sdcard/Download/Termux-Jobs-v1.5.14-test-arm64.apk + README/SHA. Both engines: separated app progress from base instructions/required correction, shorten only service progress before reducing reply reserve; preserve rights/schemas/current task; appFeedback no longer splits user turns. Capacity failure after completed tools ends clearly with context_budget_stop and saved cards, not false task completion. First oversized request still errors. Dropdown row ovals shorter, active session highlighted, local rows model name + robot vector, uniform dimensions; creation/presets/main selector unchanged. Full660 app+145 emulator and17 real-SDK/fake-inference checks pass, build/runtime audit/APK audit/Download SHA pass. Device/model checks pending. No GitHub, environment/process changes or data reset. See RELEASE/VALIDATION-v1.5.14-test.md. Next code >=1152. design/ untouched.

## Latest private delivery: v1.5.13-test (1150)

Delivered /sdcard/Download/Termux-Jobs-v1.5.13-test-arm64.apk + README/SHA. Both local engines: repeated identical successful write guard independent of changing listings, <=2 writes by default then warning; actual read/final allowed, further duplicate stops. Explicit repeat counts honored; reset on distinct/failed write, command/install, changed fresh target read; cache annotations ignored. After warning repeated same metadata query also bounded, fresh queries/reads allowed. Not universal semantic loop detection. Compact pending approvals, then localized granted/denied/interrupted oval with read-only details. Fixed false Not executed duplicate across approval/UI history records (nearest exact tool/output matching with boundaries). Full650 app+145 emulator and16 SDK/fake-inference cases pass; APK audit+Download SHA pass. Device checks pending. No publication, no model/permissions/output-cap/default changes. See RELEASE/VALIDATION-v1.5.13-test.md. Next code >=1151. design/ untouched.

## Latest private delivery: v1.5.12-test (1149)

Delivered /sdcard/Download/Termux-Jobs-v1.5.12-test-arm64.apk with README/SHA. Gemma leading native fragments classified unparsed_native_tool, hidden in chat, routed through <=2 validation retries only for action requests; fragments never parsed/executed. Native-mode plain JSON without SDK provenance rejected. Native repair messages exclude broken assistant fragments and fake tool responses. Reported напиши/положи action wording recognized. Rights/approval/cancel retained. No sampler/thinking/reply cap/weights changes. Full640 app+145 emulator cases and16 SDK/fake-inference cases pass; final APK audit and Download SHA pass. Actual Gemma behavior still requires developer test of original short request in new chat. See RELEASE/VALIDATION-v1.5.12-test.md. No publication. Next code >=1150. design/ untouched.

## Latest private delivery: v1.5.11-test (1148)

Delivered /sdcard/Download/Termux-Jobs-v1.5.11-test-arm64.apk with SHA/README. Five synthetic LiteRT Native comparisons (system role, write only, all current, greedy, thinking128); max512, no tool dispatch, no chat history, overrides restored. Normal inference unchanged. Found hardcoded disabled thinking/channels, not proven causal. Full630 app +145 emulator cases and15 SDK/fake-inference tests passed; APK audit passed. Device comparison/report/cancel pending. See RELEASE/VALIDATION-v1.5.11-test.md. No publication. Next code >=1149. User design/ remains untouched.

## Latest private delivery: v1.5.10-test (1147)

Delivered /sdcard/Download/Termux-Jobs-v1.5.10-test-arm64.apk and Local-AI-tools-v1.5.10.txt. See RELEASE/VALIDATION-v1.5.10-test.md. All13 registry descriptions explain purpose/contrasts; write saves guides, command executes requested actions, install handles approved dependencies. write.text and command.command include explicit routing descriptions. Shared native schemas and schema-based JSON benefit; legacy JSON text also clarifies guides. No name/type/bounds/availability or authorization change; baseline24 combinations pass. Final90 JVM cases +14 real-SDK/fake-inference cases pass. SDK-parsed descriptions verified. Adds~405 estimated LiteRT input tokens; real-model routing remains pending. Developer clean-chat1.5.9 report4828324737620970227 disproved stale-history-only explanation (91 current tokens, no prior history, wrong installation action). No publication. Next versionCode>=1148.

## Latest private delivery: v1.5.9-test (1146)

Delivered /sdcard/Download/Termux-Jobs-v1.5.9-test-arm64.apk. See RELEASE-v1.5.9-test.md and VALIDATION-v1.5.9-test.md. Shorter default/brief prompts and shared native descriptions/settings; all24 schema configurations retain structure/constraints. Built-in behavior distinguishes saving an instruction from installing it and follows user language. User custom files and Tools only semantics retained. Tool/progress/context cards are rounded left-aligned pills with expandable details. Final99 relevant cases +13 SDK interface cases pass; broad-run evidence/correction documented. Estimated Standard LiteRT2533->2223, Qwen3383->2542; Brief LiteRT savings only20 estimated tokens. No real model speed/obedience claim. Private only, no publication. Next versionCode>=1147.

## Latest private delivery: v1.5.8-test (1145)

Delivered /sdcard/Download/Termux-Jobs-v1.5.8-test-arm64.apk. See RELEASE-v1.5.8-test.md and VALIDATION-v1.5.8-test.md. Bounded untrusted task/target evidence added only after dropping previous turns and only if space remains; optional null history fields omitted; one expandable context notice per user turn. Both local engines, no semantic history search or external-agent changes. Final71 targeted tests pass; broad-run results/corrections documented. ZIP/identity/signature/limited-secret scan and Download SHA verified. No publication. Next versionCode >=1146.

Developer verification of1.5.7: diagnostics4159294689667868535 shows joint video analysis3prepared/3attached/0omitted, followed by final normal chat; developer supplied successful ordinary file read in the same chat. Visual accuracy and cancellation are not established by that sequence.

## Latest private delivery: v1.5.7-test (1144)

Delivered /sdcard/Download/Termux-Jobs-v1.5.7-test-arm64.apk. See RELEASE-v1.5.7-test.md and VALIDATION-v1.5.7-test.md. view_video uses no-tool/no-history sub-analysis, joint then per-frame fallback, restores schemas/scope and returns text observations to normal chat. Only compatible vision LiteRT currently; llama remains text-only. Full615 app tests, final111 targeted, real-SDK13 interface tests passed. Hardware video and normal follow-up/cancel tests need developer verification. No publication. Next versionCode >=1145.

## Latest private delivery: v1.5.6-test (1143)

Delivered /sdcard/Download/Termux-Jobs-v1.5.6-test-arm64.apk. See RELEASE-v1.5.6-test.md and VALIDATION-v1.5.6-test.md. Current-turn progress survives context compaction and native LiteRT prompt replacement. Exact repeated mutation cycles trigger a reminder and then stop if proposed unchanged again. Expandable persistent step card for both local engines. Structural context comparison avoids false shortening notices. 609 app tests and12 real-SDK interface tests passed; hardware/model outcome requires developer check. Not published. Next versionCode >=1144.

## Latest private delivery: v1.5.5-test (1142)

Delivered /sdcard/Download/Termux-Jobs-v1.5.5-test-arm64.apk; see RELEASE-v1.5.5-test.md and VALIDATION-v1.5.5-test.md. Behavior menu updated inside existing settings location; Standard/Brief/Custom plus independent Tools only. Reply reserve adapts, current-chat summaries retain originals, diagnostics include media counts and finish reason. GPU unavailable; pinned LiteRT NPU mode fixed. New optional behavior fields preserve legacy defaults. Private only; no publication. Next versionCode >=1143. Device quality/media/context checks and expanded-context export research remain.

## Latest private delivery: v1.5.4-test (1141)

Delivered /sdcard/Download/Termux-Jobs-v1.5.4-test-arm64.apk. See RELEASE-v1.5.4-test.md and VALIDATION-v1.5.4-test.md. Native tool repair, current-chat recall, styled environment tiles, explicit server connect/disconnect with owned transport closure. Not published. Next versionCode >=1142. Next requested UI: unavailable GPU tile for prior llama model; fixed disabled NPU mode tile for LiteRT. Investigate context instruction/schema cost; hardware image/video/audio checks remain.



## Latest local delivery: v1.4.21-test (1127)
APK delivered to /sdcard/Download/Termux-Jobs-v1.4.21-test-arm64.apk. Manual llama prefill benchmark: lengths512/1024/2048/4096, threads2/4/6/8, batches256/128 vs512/256, forward/reverse; separate temporary contexts and cancellation gate. Normal chat remains4threads. 651 Java tests and tiny-GGUF native benchmark/cancellation/context-preservation checks passed. Final artifact audit: .verification/local-ai-v1.4.21/artifact.json. Real Qwen/device measurements pending; not published. Next build code >=1128.


## Частная 1.6.9-test / code1205
Подключены пакеты шести менеджеров к конкретному proot alias; 29 тестов и сборка
прошли. См. VALIDATION-v1.6.9-test.md. Пользователю передаётся один UPDATE APK.
Далее проверить каталоги/пакетные действия на устройстве, затем бэкапы; особенно
Void после restore без OCI manifest (ещё не исправлен). На GitHub не публиковать
без нового разрешения. Старые rollback1200/1202 ниже текущего versionCode.


## Исправление Alpine — 1.6.10-test/code1206
На скриншотах18:33 APT обнаружен через Termux PATH fallback; реальный Alpine
содержит sbin/apk и не содержит usr/bin/apt-get, usr/bin/dpkg-query. Гостевые
пакетные операции теперь используют Linux PATH в subshell; общий PATH сессий и
агенты не менялись. Повторить пакетные тесты Alpine после обновления APK.
Debian update/install/tree --version подтверждены разработчиком на1.6.9.


## Alpine catalogue — 1.6.11-test/code1207
Developer confirmed apk update and native manager in1.6.10. Offline --available
filter returned empty while plain offline apk list returned repository rows.
Remove that filter for available tab only; refresh/install still require device
verification. No GitHub publication. Next test: Alpine list, install tree, version.


## Fedora DNF5 — 1.6.12-test/code1208
Alpine tree2.3.2 installed/runs, developer confirmed. Fedora makecache succeeds;
repoquery output has literal backslash-t, causing parser rejection. Queryformat
now passes actual TAB for all DNF tabs. No parser relaxation or manager changes.
Next device check: Fedora reread, install tree, tree --version. Remaining managers
and backups still pending; no GitHub authorization.


## Void versions — 1.6.13-test/code1209
Fedora/openSUSE tree2.2.1 installed/runs, developer confirmed. XBPS remote index
contains14204 packages incl13 letter-leading versions rejected by old parser.
New XBPS-only regex requires _revision and accepts letters; entire index checked
read-only in memory. Next device test: Void reread/install/tree --version, then
Arch. Backups and openSUSE initial query/refresh busy remain separate checks.


## Все шесть пакетных менеджеров — проверка разработчиком завершена для install
Void tree2.2.1 installed/runs (19:29). Arch full upgrade completed, service hooks
skipped in nonbooted proot; tree2.1.0 installed/runs (19:38). Together with Debian,
Alpine, Fedora, openSUSE, basic catalogue/refresh/install/run paths checked by
developer. Removal and backup/restore not yet verified. Next: backup stage, notably
Void restore without OCI manifest; do not claim all restores supported or publish.


## Backups — 1.6.14-test/code1210
restore_ubuntu now adds only generated minimal manifest to freshly renamed
rootfs; foreign OCI metadata discarded, rootfs/passwd preserved. Native AST
command_run accepted generated config with explicit sh (mocks, no process).
28 Python backup tests passed including fresh alias/content preservation and
malicious manifest symlink / rootfs write-through rejection. Environment chooser
now separate right Install / disabled gray Installed badges; row selection kept.
Actual backup/restore not run. Next developer check: backup Debian only, inspect,
restore separate jobs alias, tree/files, original still intact; then Void. Do not
publish. Package comparison remains Termux APT only. Stable rollback assets have
lower version codes; don't offer old ones as overlay rollback.


## Restore failure/PAX — 1.6.15-test/code1211
Developer created Debian-only backup /sdcard/Download/Termux-Jobs-20261019-195137-d09fc64b.
Real SHA/size and helper inspect compatible. Restore failed: first115-char APT
path carries pax_headers.path=debian/rootfs/... overriding rewritten jobs name.
Proved via one-member in-memory repack. New code clears pax_headers/sparse and
materializes file data normally. Native invocation still untouched; full real
repack in repository with intercepted native run is verification, not live restore.
Installed partial jobs-6a650d598a1d exists without manifest; don't delete or start
it, installed environment modifications not authorized. Source Debian unchanged.
Next user test: SAME manifest, Linux only, fresh restore. No GitHub publication.


## Device restore confirmations after1.6.15
Debian restored jobs-5b9572bd2e44: developer terminal Debian13/trixie13.7,
tree2.2.1, jobs-env-test.txt, file manager confirmed. Void restored
jobs-2fadee2c8257 (source void via RESTORE-RESULT), developer confirms startup,
commands/test file/file manager after requested checks. Do not claim all other
restore modes verified. Remaining: original environments recheck, other four
full restores if needed, selective-data flows, process-close/reopen. No release
authorization. Partial jobs-6a650d598a1d remains; don't modify installed state.


## Alpine backup arch — 1.6.16-test/code1212
Developer confirms both restored presets and original Debian/Void restart.
Alpine backup /sdcard/Download/Termux-Jobs-20261019-201520-e38b386d is blocked
because only regular ELF is bin/busybox (machine183); sh/env are symlinks to it.
Add busybox to bounded ELF scan + primary paths. Real helper inspect now marks
alpine/aarch64 compatible. New tests enforce wrong/unknown arch still blocked.
No actual Alpine restore done by assistant. User should re-open same manifest
in updated app, select Linux only and restore, then verify preset/tree/files.


## Alpine restored — developer confirmation
After1.6.16 developer confirms full Alpine restore, os-release/tree/files check,
file manager, close/reopen. Debian/Void/Alpine cycles confirmed. Next full restore:
Fedora, then openSUSE and Arch. No GitHub authorization; don't delete installed
partial jobs-6a650d598a1d or alter user environments.


## Arch archive — 1.6.17-test/code1213
Developer confirmed Fedora full restore works like previous. Arch creation
failed unsafe path in native TAR. Existing incomplete folder in Download:
Termux-Jobs-20261019-202550-abf32c48, ubuntu-archlinux.tar.gz374164991 bytes,
no manifest. Three ordinary systemd filenames contain literal backslash-xHH;
old clean rejected all backslashes. Allow only literal hex sequences without
decoding; arbitrary backslash/traversal still blocked. Full scan now37022 entries,
aarch64. Recreate Arch backup after updating to confirm complete creation, then
restore/test. openSUSE restore not confirmed yet. Do not clear installed partial
jobs-6a650d598a1d or any existing incomplete archives without authorization.


## Restored Arch .l2s — 1.6.18-test/code1214
Developer confirms Arch restore/file/commands/restart. Warning .l2s seen in
jobs-72e1416e65be. Read-only actual root0555 and absent .l2s; original Arch0555
has it. Native login makedirs_at catches OSError and safely unsets PROOT_L2S_DIR,
falling back to adjacent intermediates. Likely app-UID write restriction; exact
errno not captured, assistant/root fixture can mkdir despite0555. New repack
adds absent .l2s0700 before native restore applies root modes; rejects file/link,
keeps existing directory unduplicated. No live filesystem edits. Need user repeat
restore same Arch backup to new copy, confirm no warning. openSUSE restore still
not explicitly confirmed. Do not publish or delete any copies without authorization.


## All six full environment restore cycles confirmed by developer
After1.6.18 developer confirmed Arch restored launch WITHOUT .l2s warning.
Then confirmed openSUSE full cycle. Debian/Alpine/Fedora/openSUSE/Void/Arch Linux
ARM now have developer-reported backup, separate restore, tool/test-file/file
manager and reopen verification. Do not infer exhaustive selective/foreign/cross-
device/live-database coverage. No GitHub publication authorized. Latest delivered
APK1.6.18-test/code1214; metadata audit in .verification/backups-environments-v1.6.18.
