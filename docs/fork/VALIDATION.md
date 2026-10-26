# Current release validation — v2.0.9

See [VALIDATION-v2.0.9.md](VALIDATION-v2.0.9.md) for the final artifact, tests,
lint findings, compatibility and device-specific limits. Earlier reports retain
the scope of their named builds.

---

# Historical release validation — v2.0.8

See [VALIDATION-v2.0.8.md](VALIDATION-v2.0.8.md) for the final APK, relevant tests, lint findings, update compatibility and device limits. Earlier reports retain the scope of their named builds; prior full-suite counts are not presented as new checks for this release.

---

# Historical release validation — v2.0.7

See [VALIDATION-v2.0.7.md](VALIDATION-v2.0.7.md) for final APK identity, checks, compatibility and remaining limits. Component URL/size/SHA-256 verification covers every pinned download in this release. Device evidence is attributed to the developer; source checks do not establish universal driver or clean-device GUI compatibility.

---

# Historical release validation — v2.0.6

See [VALIDATION-v2.0.6.md](VALIDATION-v2.0.6.md) for final APK identity, relevant automated and isolated native checks, developer-reported device evidence, compatibility and remaining limits. The Wayland/KDE experiment is retired. Historical release evidence below retains its original scope.
All four public v2.0.6 assets were downloaded anonymously and matched local sizes/SHA-256. The anonymous latest API and updater metadata, release body/credit and tagged documents were verified. The separate components release had all20assets verified, plus real download/preparation checks through production helpers. See the current handoff for publication evidence.

---

# v2.0.5 release validation

X11 resolution/scaling/margins, stricter automatic keyboard focus checks and
compact extra keys. Public Wayland/KDE and driver controls disabled in both UI
and backend; private test.10 remains separate. Same com.termux/signer/schema8,
code1376; no data reset/migration. See [current validation](VALIDATION-v2.0.5.md)
for artifact identity, tests, source/build checks and device limits.
All four public assets were downloaded anonymously and matched local SHA-256.
Latest stable API/updater, release body/credit and three tagged documents were
verified after publication. The published tag is immutable.

---

# Historical v2.0.4 validation

# v2.0.4 release validation

Bionic-for-glibc support in six ordinary glibc desktops; separate caches/processes,
X11 extra keys and optional Jobs GPU Test. Same com.termux/signer/schema8,
code1366, no data reset/rootfs migration. The [current validation](VALIDATION-v2.0.4.md)
records final APK identity, automated/native checks and driver/device limits.
All nine public assets were downloaded anonymously and matched local SHA-256;
latest stable API/updater and seven tagged documents were verified after publication.
QCOM891.11 remains experimental, including one unresolved concurrent stress
timeout. Historical reports below retain their original scope.

---

# Historical v2.0.3 release validation

Final APK: code1352, ARM64/com.termux, same signing identity/schema8; no valid-data
reset or rootfs/account migration. [Validation](VALIDATION-v2.0.3.md) records the
final metadata, checks and remaining limits; [final audit](AUDIT-RELEASE-v2.0.3.md)
links the focused reviews. HezeAI confirmed the tested candidate, including
Fedora MP4 playback, and authorized publication. Runtime assets/native libraries
and all494 production files match that candidate. Lint:0errors/210warnings.
The stable updater payload carries RU/EN compatibility notes before installation.

Historical private results below remain tied to their original builds.

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

# Unreleased audit

The earlier agent-chat source was private2.0.3-test/code1332. See the
[agent-chat audit](AUDIT-AGENT-CHATS-2026-10-23.md) for results and remaining
device/provider checks. Public stable metadata below remains v2.0.2.

# Current validation — v2.0.2

[Final artifact, checks and limits](VALIDATION-v2.0.2.md). Android8+/ARM64,
com.termux/signer/schema8 unchanged, code1331 above stable/private predecessors.
No reset/environment migration. Gemini key duplicate/import requires re-entry;
restart agents with saved chats after updating. HezeAI confirmed Codex image input
and OpenCode attachment sending. Scoped/full-fixture evidence and unverified live
login/provider/device cases are distinguished in the report.

APK45828682bytes, SHA-256 `436a7803f9b4c93ad2c7c0dd25bf0bc22b9565c6f269a0d2e9278016301e1571`. Every assets/native-lib entry equals tested private1330.
Build/package/version/ZIP/signature/runtime checks passed. Broad897 app test run
had one stale invalid-ID fixture, corrected and covered by15 green targeted tests;
154 emulator tests passed. Final four Node suites and earlier native fixtures pass.
Published and verified2026-10-23: [v2.0.2 — Agent chat improvements](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.2) is stable/latest, not draft/prerelease.
Source tag7aecf5af is unchanged. All four uploaded assets were downloaded and
byte-compared; APK/metadata size/hash and code1331 agree. Anonymous latest API and
update.json work, and six tagged documentation files match their local sources.
Release title/body match the prepared English notes and include the assistance
credit. This documentation-only record does not rebuild the APK or move the tag.

Historical validation records follow.

# Historical validation — v2.0.1

[Artifact, tests and limits](VALIDATION-v2.0.1.md). Full Java869 app+154 emulator
passed; final JSON correction covered by35 backup/store tests. Full Python546
with one skip, then303 final X11 tests passed; all21 Node suites passed.
ARM64 release APK, ZIP/runtime/package/version/signature/hash checks passed.
Android8+/ARM64, code1325, schema8, same package/signer, no data reset.
APK45794876bytes, SHA256
`a0a2e86abfb3da4f91e24d3e3c824ecc6f0ca537d2f71fa04fb8f86361ebb7ca`.
GPU overlap/performance remains a device check; the report distinguishes native
fragment tests from Android GUI tests and records lint-toolchain limitations.
Publication verified2026-10-23: [v2.0.1 stable/latest](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.1),
source tag98445d2a. APK, SHA256SUMS, update.json and validation asset were
retrieved and byte-compared; APK package/code/signer/ZIP and updater size/hash
match. Anonymous latest API/update.json and four tagged documentation files
were checked. The release body matches the prepared short English description.
The published tag and APK were not moved or rebuilt for this record.

Historical validation records follow.

# Historical validation — v2.0.0

[Final artifact, tests and limits](VALIDATION-v2.0.0.md).
862 app +154 emulator JVM tests pass. Full Python discovery544 with one skip;
three stale fixture expectations were corrected and34 affected tests rerun green.
Android8+/ARM64, unchanged signer/package, code1320, schema8, no general reset.
Developer reports RedMagic10Pro and SamsungS26Ultra checks; other devices require
verification. Final Ubuntu VirGL GPU INFO and all-combination fresh-device/GUI
coverage remain limited as described in the report. Sources: [THIRD-PARTY](THIRD-PARTY.md).


Publication verified2026-10-23: [v2.0.0 stable/latest](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.0).
All four assets were downloaded and compared. APK SHA256
`492626100bb3500145fa84f28d030b8cac3e00841967ceff64cf080fae6b5dda`,
45794028bytes; package/code/signature/ZIP and updater size/hash match. Public
unauthenticated update.json equals the generated metadata; seven tagged current
documentation links return200. No APK rebuild or tag movement for this record.

The previous sections below are historical release records.

# Текущие проверки — v1.7.5

[Итоговый отчёт](VALIDATION-v1.7.5.md):697 JVM-тестов финального поведения,
171 Python-тест неизменённого Python-кода; Node/native fixtures, release APK,
ZIP/runtime/package/version/ABI/подпись/SHA-256 проверены. Разработчик подтвердил
подгрузку истории и новый запуск Codex на1.7.5-test. Не заявляется полный
device coverage очистки/удаления/миграций. Code1220, Android8+/ARM64, schema8.
[Изменения и совместимость](RELEASE-v1.7.5.md).
Stable/latest v1.7.5 опубликован: APK, SHA256SUMS, update.json и отчёт скачаны
обратно и совпали; release body, документы в теге, анонимные latest API и
update.json проверены. Подготовленный отчёт в теге фиксирует состояние до
публикации; эта запись добавляет результаты обратной проверки.

Ниже исторические отчёты, не результаты текущей версии.

# Исторические проверки — v1.7.0

[Итоговый отчёт](VALIDATION-v1.7.0.md):668 JVM-тестов и146 Python-тестов,
release-сборка и runtime/ZIP/package/version/ABI/подпись/SHA-256 проверены.
Пакеты и полный цикл бэкапа/отдельного восстановления шести систем проверены
разработчиком. Удаление защищено штатными блокировками; новый Android-экран
удаления требует проверки на ненужной копии. Совместимость и ограничения —
в [описании релиза](RELEASE-v1.7.0.md). code1215, Android8+/ARM64, schema8.
Stable/latest опубликован: четыре assets скачаны обратно и совпали;
анонимные latest API/update.json и три публичные ссылки документации проверены.

Ниже исторические отчёты, не результаты текущей версии.

# Исторические проверки — v1.6.5 fix

[Итоговый отчёт](VALIDATION-v1.6.5.md):644 JVM-теста,119 Python-тестов и20 наборов
Node/native fixtures. Проверены фокус ввода, карточки/навыки, загрузка большой
истории Codex и поиск компонентов OpenCode. Финальная release-сборка,
runtime/ZIP/package/version/ABI/подпись/SHA и schema1 update.json проверены.
Assets/native совпадают с проверенной разработчиком1.6.5-test; code1198.
Подтверждения разработчика отделены от автоматических проверок. Полный чистый
Lint, все устройства/жесты и real-account Claude/Vertex не заявляются.
[Совместимость и ограничения](RELEASE-v1.6.5.md).
Stable/latest v1.6.5 опубликован: четыре файла скачаны обратно и совпали,
анонимные latest/update.json и публичная документация тега проверены.

Ниже исторические отчёты; они не являются проверками текущего выпуска.

# Исторические проверки — v1.6.0

[Итоговый отчёт](VALIDATION-v1.6.0.md):630 JVM-тестов полного запуска,
дополнительная проверка совместимости schema8,119 Python-тестов и19 наборов
Node/native-CLI fixtures. Release-сборка и runtime/ZIP/package/version/ABI/
подпись/SHA-256 проверены. Все assets/native libraries совпадают с проверенной
разработчиком v1.5.33. Чистый полный Lint не заявляется; реальные Cloud-сценарии
Claude/Vertex и полный набор Android UI/обновления не объявляются проверенными.
Stable/latest v1.6.0 и четыре скачанных assets проверены: metadata/hash/size/code
совпадают, публичные ссылки доступны.
[Совместимость](RELEASE-v1.6.0.md) · [исследование удалённых local AI](LOCAL-AI-RESEARCH.md).

Ниже исторические отчёты; они не являются проверками текущего выпуска.

## Previously published v1.5.3 validation (historical)

# Текущие проверки — v1.5.3

[Итоговый отчёт](VALIDATION-v1.5.3.md):496 JVM-тестов (неизменённый emulator
UP-TO-DATE),119 Python-тестов,11 сценариев прав Codex. Release-сборка,
пакет/версия/ABI/подпись/SHA-256 и runtime inventory проверены.
Есть прежние ограничения части Lint; полный чистый Lint не заявляется.
Проверки кода/сборки отделены от проверки на устройстве. Stable/latest v1.5.3
проверена: четыре скачанных release assets совпали, подпись/метаданные верны,
ссылки доступны; исходный тег и APK не менялись.

[Исследование удалённых локальных ИИ](LOCAL-AI-RESEARCH.md).
Предупреждение о приватных локальных данных: [совместимость](RELEASE-v1.5.3.md).

Ниже исторические отчёты; они не являются проверками текущего выпуска.

---

# Исторические проверки — v1.5.0

[Отчёт](VALIDATION-v1.5.0.md):724 теста полной конфигурации,153 адресная проверка публичной, реальная проверка SFTP на отдельных тестовых файлах. Итоговые APK/подпись/версия/SHA-256 проверены. Проверки кода и сборки отделены от сценариев на устройстве.

Ниже сохранены исторические отчёты; они не заменяют проверки v1.5.0.

# Исторические проверки — v1.3.3

[Отчёт](VALIDATION-v1.3.3.md):440 JVM-тестов, исправление Guava, проверка
runtime-классов в APK и файловых операций. Android8.0+/API26.
[Предыдущий отчёт](VALIDATION-v1.3.2.md).

Ниже — исторический отчёт v1.2.0/v1.2.1; его результаты не являются проверкой новых сценариев.

# Проверки Termux Job’s v1.2.0

## v1.2.1 — ускорение отправки и очередей сервера

SFTP отправляет до8блоков по одному SSH-соединению; изолированы очереди
мониторинга и поиска/диагностики. Разработчик подтвердил ускорение отправки
примерно4–5раз на своём подключении; результат зависит от сети и сервера.
Защиты файлов/отмены сохранены, данные без сброса, агент прежний.

[Изменения](RELEASE-v1.2.1.md) · [Проверки](VALIDATION-v1.2.1.md).



Финальный APK `Termux-Jobs-v1.2.0-arm64.apk`: 36476761 bytes (~36.5МБ), SHA-256
`d270bfd36a7515ab6facbb21586db7087ea2870a253b227cb315cbe4202a70e8`. com.termux/versionName1.2.0/code1071 >1061 публичнойv1.1.4
и >1070 локальнойv1.1.13. Android min24/target28, толькоARM64, non-debuggable.
Сертификат SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`, прежний для публичнойv1.1.4
и локальных сборок. apksigner APKv2 PASS. ZIP CRC/duplicate/traversal/stored4byte
alignment PASS. Java/resources/assets совпадают с release stage. Native побайтно
идентичны публичнойv1.1.4; agent.py идентичен локальнымv1.1.12/13.
Ограниченный скан приватных ключей/токенов/auth/keystore не выявил совпадений;
он не является универсальной гарантией отсутствия секретов.

## Проверки кода

- 379 JVM tests,0 failures/errors/skipped: финальный код проверен на локальной
  v1.1.13; вv1.2.0 менялись только versionName/code и документация. Повторный
  прогон неизменённого поведения не заявляется. Лог jvm-final2.log сохранён
  из .verification/servers-v1.1.13; исходники Java/resources сверены со stagev1.2.0.
- 6 Python search +20 agent tests PASS на финальных неизменённых helpersv1.1.13.
- 15 Python schedules tests повторно PASS при подготовкеv1.2.0.
- Включены profile roundtrip/старые поля/изоляция, подтверждения без remote actions,
  UI-кнопки, Filename search limits/literal/Unicode/symlink/partial, progress/ETA,
  finished-record deletion с запретом active и без сигналов/удаления результатов,
  runner locks/enable/disable/edit/delete/failure, fakecron/systemd, собственные
  дочерние процессы, editor revision-conflict/несохранённый текст, existing terminal tests.
- Release build успешен. Localization581 PASS наv1.1.13, новые RU/EN проверены
  в исходниках/целевых тестах. git diff --check и проверка документационных ссылок.
- Полный чистый Lint не заявляется: прежние lintVital/JNA aarch64/Kotlin metadata/
  custom lint несовместимости. Итоговую сборку это не заблокировало.

## Ревью и границы

Прослежены профиль/подключение/hostpin/password, раздельное владение SSH-процессами,
cancel/close/download cleanup, agent admission/unmanaged/forget, scheduler create/
edit/disable/delete/running locks, внешняя конкуренция crontab, player lifecycle/
loopback-only HTTP и безопасные ошибки. Использованы текущие права SSH, без
автоматического sudo/linger. Agent и расписания не удаляют чужие проекты/службы.
Смена endpoint сохраняет local favorites/actions, запуск показывает новую цель.
Новые поля необязательные; библиотекаv8/AI/окружения без миграции и сброса.

## Что не доказано локальными тестами

Разработчик HezeAI установил и проверил промежуточные сборки на устройстве.
Разработка велась с помощью AI-ассистента; локальные автоматические проверки
описаны выше. Отдельной проверки в целевой среде требуют разрешения, клавиатура,
жесты/DPI, фактическая скорость и отмена SSH/SFTP, поддержка конкретных кодеков,
настоящие cron daemon/user-manager/linger/reboot и полное восстановление.
Автоматические проверки cron/systemd используют подставные команды.

## Обновлятор и публикация

schema1 update.json создан из финального APK, RU/EN highlights содержат совместимость.
Пакет/подпись/код/размер/SHA согласованы. Latest должна содержать APK/SHA256SUMS/
update.json, автоматически проверяется при открытии при включённой автопроверке;
ручная проверка сохранена. Это предложение обновления, не push/автоустановка.
Сверка загруженных assets/тега/ссылок фиксируется отдельной квитанцией публикации.
Доказательства: .verification/release-v1.2.0 и servers-v1.1.13.

## Исторические проверки

[v1.1.4](https://github.com/HezeAI/termux-jobs/blob/v1.1.4/docs/fork/VALIDATION.md) · [v1.1.13](VALIDATION-v1.1.13.md).
