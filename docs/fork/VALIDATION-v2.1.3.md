# v2.1.3 release validation

Date: 2026-11-04. Baseline: **last published stable v2.1.2 / 1476**, confirmed
through GitHub Releases latest API before changes. Baseline source:
`c0073b462311bba5fd2dcc301b048d69e407f3f1`.
Release source is identified by the immutable **v2.1.3** tag on **fork/base**;
final commit/CI identities and public-download results are recorded in the release
page and `validation.json`. Release comparison excludes intermediate private-build
history and defects that existed only during that development.

| Final property | Verified value |
| --- | --- |
| APK | Termux-Jobs-v2.1.3-arm64.apk |
| Package / version | com.termux / 2.1.3 / 1485 |
| Bytes | 40,481,588 |
| APK SHA-256 | `787a4c5dbf162490891620bbb58e899f9ae3097d46889857b6237f27af75fa3d` |
| Certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| ABI / signature / debug | ARM64 / APK Signature Scheme v2 verified / nondebuggable |
| Runtime / assets | 14,214 defined DEX classes; 122 source-matched assets; 7 ARM64 ELF payloads |

Same Termux Job’s package/certificate and code above all issued candidates,
including accepted code 1484. Official Termux update compatibility is unverified;
com.termux alone is insufficient. No app-data reset, rootfs replacement or
incompatible workspace-schema migration is introduced (workspace schema 8).
OpenCode connection backup manifest v3 needs the new reader; old v1/v2 archives
remain readable. Uninstalling can delete private data and is not an update step.
Backups are unencrypted; see [exact scope](BACKUP-RESTORE.md).

## Changes and audit outcome

[Release notes](RELEASE-v2.1.3.md) describe changes since v2.1.2. Local fixes now
included in the signed APK: structured preparation status instead of raw logs;
finite download wait/retry limits; pinned release signer gate; supported native
approvals for new Codex profiles; run-owned Gemini key handoff; managed Gemini
updates; explicit backup warnings/opt-in OpenCode state and legacy restore;
separate OpenCode directory/action decisions and proven-decline display.
Existing profiles, accounts and environments are not migrated automatically.

New project templates and common RU/EN context rules, preparation plan progress,
chat overlays/composer and action-group event clocks are included. Saved
instructions are not replaced without explicit saving. All 122 assets, including
all four common templates and the full 7,882-byte supplied project template,
match the accepted candidate exactly. Native/system component versions did not change.

**Claude history comparison:** `claude-history.cjs` is byte-identical to v2.1.2.
Bounded history paging, retention and full long-answer access were already shipped;
they are not announced as new fixes. Added Claude projection data is known event
time for tools/shell groups, consumed by the shared HH:mm group header. User
messages, answers and attachments are outside that clock change.

## Checked anew / Проверено заново

- Local release compile/package/signing using repository-local JDK 11, Gradle 7.2,
  AGP 4.2.2 and SDK. `verifyReleaseSigner` checks the actual existing public
  certificate; `verifyX11Inputs` remains enabled. No system package or working
  rootfs/session modification is performed.
- Full relevant local JVM request: app and terminal-emulator, plus the other three
  modules. Coverage after corrections: **1,666 distinct cases, 0 failures, 0 skipped**.
  App: 1,512 (including two new measured-view regressions); terminal-emulator: 154. The latter task was UP-TO-DATE and is classified
  as reused, not freshly executed. Other modules had NO-SOURCE and are not counted
  as passed test cases. Initial broad app run had **4 failures**: three stale
  launcher/approval-label expectations, plus a two-pixel composer-height discrepancy.
  An intermediate targeted attempt still had 2 failures (nested shell quoting and
  measured font rounding); these are retained in logs. Final **91-case targeted
  rerun passed**, replacing cases by exact class/method identity in the result map.
  Unchanged passing cases are not counted a second time.
- The actual editable one-line layout can differ from a fresh StaticLayout in this
  Robolectric/API 28/font setup. The initial text-layout-only correction passed locally but failed on the clean CI runner. The composer now retains the complete measured one-line View height
  across multiline drafts and invalidates it on style changes. Targeted checks
  cover narrow widths, fonts, draft preservation, alignment, header insets/touches,
  all four controller routes and reading anchors. This final small metric correction
  is automatically checked, not separately accepted on the phone.
- **137 Python unittest cases**, plus the invoked synthetic process/download
  probes, passed: instruction storage/templates, backups/legacy readers,
  preparation/status/stages/downloads, pinned-input helper and Lint-report logic.
- **20 Node suites** passed: Codex launch/policy, receipts/timing, Gemini broker
  partial/stall/owner-death/retry and handler/update/auth modes, chat recovery,
  OpenCode denial stages/reopen/next request, Claude bounded history and Codex chat.
  These are synthetic data/owned processes; no real model login or working chat.
- **8 synthetic signer checks** passed: missing key/pin, wrong certificate/password/
  alias, correct existing fixture key and repeat. No real private-key entry or
  password is exported; production signing uses the existing key normally.
- Localization: **586 workspace strings**, app/shared keys and format arguments passed.
- Final-source **full modern Lint 7.4.2**, all five modules/dependencies:
  **0 errors, 236 warnings**, no infrastructure/rule-loading errors. Gradle 7.5.1 /
  JDK 11. compileSDK 35 exceeds this AGP's tested SDK range; warning retained.
  Legacy packaging-Lint Kotlin/registry diagnostics are not used as the successful
  gate. No blanket warning baseline/suppression is added.
- Final APK: ZIP CRC, no stale outer local ZIP headers in gaps, package/version/code,
  minSDK/ABI/debug, v2 signature/certificate, SHA; 122 assets and 575 production
  source/resource inputs match staged bytes. Runtime gate has no required/unreviewed
  missing definitions; 31 reviewed optional types remain. Notices and vector
  paperclip are embedded, voice/microphone/old window diagnostics are absent.
- RU/EN update.json schema 1 generated from that exact APK: URL/size/hash/code/package
  and compatibility in both highlights (13 lines per language) and dedicated fields
  checked for older updater compatibility. No uninstall/data-reset workaround.
- Source review: changed tracked files, no key/cache/tool/temporary artifacts, local
  documentation links and diff whitespace. Four private-key-header pattern hits
  are explicitly artificial multiline-secret test fixtures, not real keys.

## Reused / Переиспользовано

- Three JNI libraries from the verified v2.1.2 release: 95 relevant native/config
  source inputs, flags/tool baseline and payload hashes match. The seven final ELF
  files are checked as ARM64. This is not a fresh graphics/driver execution.
- Terminal-emulator's 154 cached cases retain unchanged production/tests/dependency
  configuration. Their task result is not described as newly executed.
- Earlier guarded native permission chains, OpenCode real export/clean restore and
  legacy/no-replay checks are retained with their version/scope in the
  [audit matrix](SECURITY-AUDIT-STAGE2.md) and [backup report](BACKUP-RESTORE.md).
  Relevant unchanged helper/CLI inputs are recorded; fixtures are not phone evidence.
- SDK/JDK/dependency caches are reused, not copied as new environments. Preparation
  build inputs are verified; no guest or driver installation is implied.

## Phone acceptance and unverified scope

HezeAI reported **test8 / 1484** working on the phone, explicitly accepted the
transparency, and authorized stable publication on 2026-11-04. This is developer
phone evidence, not assistant testing or an instrumented trace of every case.
Previous named No/Allow shell/direct-file and outside-project results for all four
agents, and Gemini fake-key/API-refusal/normal-restart acceptance, are retained.
The stable version bump and final measured-height refinement have artifact/
automatic checks; this release task does not install or restart working sessions.

**Not checked on device, deferred by agreement:** low-level Android key handoff,
Keystore/ownership/cancel/crash recovery and Android backup export/clean restore,
including old archives/receipt behavior. No isolated Android instance is available.
RU backup screenshots cover only part of the selective form. Real Gemini API/Vertex
login is not confirmed by artificial-key rejection. All legacy/manual/TUI routes
and chat-switch-during-permission cases are not comprehensively phone-tested.
PRoot/cwd is not an OS sandbox; permitted shell access outside a project remains
a mode limitation, not proof of a denial bypass. No universal security guarantee.

**Attachment accumulation/disconnections remain unfixed**, deferred to the next
cycle after release. Existing originals/history are retained. MCP/plugins are
excluded and unverified. Full third-party CLI traffic, drivers, key custody/history
and independent security audit remain outside confirmed coverage. Legacy logs/
older handoff files are not automatically deleted or retroactively sanitized.

## GitHub CI versus local release APK

Fork CI on the published source commit prepares pinned X11 inputs on a clean
x86_64 runner, runs debug JVM tasks and builds an internal debug APK. Exact run/
workflow/source identity and conclusions are linked in the release page and
validation.json after push. CI failures/skips are not reclassified as success.
The published release APK and persistent signer are verified **locally**; debug CI
neither checks that signer nor replaces final signed release verification. No
debug APK is included as a stable release asset. Historical v2.1.2 CI failures
retain their original status and are not rewritten by this release.

## License/source and published-resource preservation

No new/updated third-party dependency, component image, font/icon library or native
payload is introduced. Notices and original copyrights match v2.1.2 and are checked
inside the APK and separate release notice asset. Exact source/pin/build materials
remain in this tag and the unchanged component source supplements.
[License/source review](LICENSE-REVIEW-2026-11-03.md) retains its coverage limits:
Ubuntu AI 87 packages / 58 exact sources / 127 DSC-referenced files were verified;
this is not a full licensing audit of every published image/driver. DSC signatures
and package rebuilds were not independently verified. No known new distribution
blocker is identified by this unchanged-component review; full compliance is not
asserted. Components binaries/names/URLs/sizes/digests are preserved.

## Evidence and public verification

Task-local records: `.verification/release-v2.1.3/` includes release snapshots,
source/privacy review, JVM initial XML and corrected map, Node/Python/localization,
negative signer, modern-Lint summary, native/build/source identities, full final
APK audit, metadata and public-download verification. These local records are not
represented as public GitHub files. Public summary assets and this report preserve
artifact identity and scope. The initial unissued packaging recipe used the wrong
local proof-directory path and failed before assembly; its log and pre-final
metric candidate are retained, not uploaded as release assets.

Publication procedure: push fork/base, verify CI, create/push immutable v2.1.3,
upload APK/SHA256SUMS/update.json/notices/certificate/validation to a draft, then
publish a normal non-prerelease release as Latest. Anonymous downloads and hashes,
latest-channel metadata, source/tag refs, documentation URLs and old resource
identities are checked afterward. Post-publication findings are recorded in the
release body without moving the tag or replacing binary assets.


### Exact 61/59 px regression and first CI failure

The [first run](https://github.com/HezeAI/termux-jobs/actions/runs/41819215609) tested
`ab82d3344f7cba282d09d16ea8c213a2331e3670`: one composer assertion failed,
8 conditional cases were skipped, and debug APK assembly was skipped. This run
remains failed; it is not reclassified by later results. No release/tag existed.

Confirmed algorithm error: text Layout height plus compound padding is not the
complete measured EditText height. An ordinary focused old-code run passed locally
with 59/59, which did not reproduce the runner. A separate controlled View measurement
adds two pixels absent from its text Layout: old `ab82d334` code records field and
measured height 61, Layout 39, padding 20, and incorrectly gives tools height 59.
The exact equality assertion fails. The fixed code caches actual one-line field
height 61, not a text estimate, and retains it for multiline under matching style.
The original strict font/size test is retained, with additional metadata in failure
messages; no tolerance, skip or delay is added. Temporary metric traces exist only
in an ignored staging copy of the old implementation, never in the APK.
Evidence: `.verification/release-v2.1.3/font-cause/`.

The complete correction resolves native intrinsic View measurement before the
row's first measure pass, rather than relying on a late layout callback. It caches
the whole one-line field height, includes decoration, and accounts for maxLines
when a long draft is restored. Two controlled regressions retain exact equality:
extra measured View height and an initial 21-line draft capped at 6 visible lines.
Five composer cases and the controller/header consumer checks pass locally.
An initial new-fixture NPE was caused by measuring an unparented EditText without
LayoutParams before changing its text; assigning its real parent parameters fixed
the fixture without weakening assertions. No production exception was suppressed.


### Capped restored draft and complete transition

The [second CI run](https://github.com/HezeAI/termux-jobs/actions/runs/41823554173)
for `436c40f2abdf3b2d5390803100a820269b629a27` passed the original 61/59 case,
but failed the new restored-draft case (63/61). It remains failed, with 8 skips
and debug assembly skipped. The retained first failure and this failure are distinct.

Android 9/API 28 TextView's getDesiredHeight caps DynamicLayout/BoringLayout by
getLineTop(maxLines); that truncation omits the full layout's last-line bottom
font padding. The baseline reconstruction must restore the actual reported bottom
padding when capped, rather than assume zero. See [matching AOSP source](https://android.googlesource.com/platform/frameworks/base/+/android-9.0.0_r1/core/java/android/widget/TextView.java).
Production uses Layout.getBottomPadding(), not a literal 2-pixel correction.

The strict test now checks one line → 21-line draft → new controls reopened with
that draft → one line, retaining the original measured baseline, text, selection
and lower alignment. The cache remains immutable for unchanged style, avoiding a
stale multiline measured height being recaptured during shrink. Local API 28
metadata: one=61, capped field=236, Layout=739, 21 total/6 visible lines, bottom
padding=0, reopened tools=61. CI records its actual metrics independently. This
is a View/controller fixture, not Android process recovery or device acceptance.
