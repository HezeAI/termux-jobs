# DevKit timeline and Codex templates — 2.1.9-test1 /1541, 2026-11-20

## Result

The shared native chat view now follows the active DevKit 0.1.57 rendering path:
individual compact actions in chronological order, standalone subagent/context
rows, separate web queries or explicit URLs, and expandable image thumbnails.
Running labels use the reference shimmer and text transition. Selecting a row
opens a centered library dialog with bounded scrolling, Copy and Close actions.
Native event keys, commands, message/part identifiers and full output remain the
detail source. Proven paired Claude results are suppressed once.

Codex subagent events display launch, message, wait, follow-up, completion and
interruption metadata in RU/EN. Explicitly referenced children are read through
`thread/read` and paginated `thread/items/list`; parent relation is verified
before content is read. Only an unsupported-method reply allows the legacy
`thread/read(includeTurns=true)` fallback. History reading never starts or resumes
an agent. Full received messages are kept within bounded paging; incomplete or
unavailable history is identified. Previous messages survive a failed refresh.
A concurrent live completion retains its newer prompt/status/response.

The common Codex RU/EN templates now come from the same source kit. Adaptation
covers Termux Job’s session environments, Bionic/glibc compatibility, package
managers and the project’s chosen APK build method. DevKit-specific commands
are removed. The source’s delegation, reasoning effort, memory, documentation,
permissions and communication requirements are retained. Existing Codex and
Claude user instruction files are not migrated; preview/explicit save and
revision conflict protection remain in the existing editor.

## Source provenance

The Termux Job’s baseline was imported from
`Termux-Jobs-v2.1.8-source-20261111.tar.gz` under
`/mnt/sdcard/Source-project/Termux-job's/`. All 2,169 baseline files were checked
by size and SHA-256 before editing. Project rules and memory came from
`Termux-Jobs-instructions-20261111.tar.gz`. The original archives were preserved.
The imported working tree has no Git database. The source inventory and comparison
patch are in `.verification/devkit-timeline/`.

User-provided archive:
`/mnt/sdcard/Source-project/DevKit/исходники DevKitJobs-0.1.57-preview-arm64(last)/DevKit-0.1.57-SOURCES.zip`.
SHA-256: `a354b53a1c212ce4246e00471a931248e55232ab41af6c34af6131c1c72742ca`.
Archive metadata reports source commit `c9692928530091857b53f820577d9c02ba761b22`.
Developer: HezeAI; AI development assistance: OpenAI Codex.

The active `ChatTimelineView` → `TimelineModel` path was traced. The archive also
contains an older grouped `CodexActionsView`; that unused grouping is not the
reference chat layout. Shared compact row widgets and the human-readable
subagent renderer were adapted by package/host binding. Remote detail paging
uses the existing Termux Job’s transport. No runtime binaries or third-party
libraries were added. Original source archives remain untouched.

## Checked anew

- Reference archive SHA-256 verified before extraction.
- 44 real production Java UI/helper files compiled against Android SDK 35 with
  Java 8 source/target settings on a project-local OpenJDK 21.
- 36 Robolectric 4.4/API28 tests passed, zero failed/ignored: individual ordering,
  stable views, four-agent adapter fixtures, paired results, RU/EN, native details,
  image disclosure and chat cache isolation, original command preservation,
  selection, composer focus/composition and reading anchors under live updates.
- Nine Node suites passed: Codex adapter, subagents, history pagination, login,
  context, model connection, action previews, live action events and event clocks.
  The new subagent suite covers 160,000-character prompts, full output paging,
  foreign events/parents, read-only methods, legacy fallback, failed refreshes,
  repeated cursors and the live-completion race.
- 46 instruction storage/template tests passed with a test-only syscall bridge.
  This environment’s Android Python 3.14 omits `os.link`; the bridge uses real
  `linkat`, including failure behavior, rather than simulating successful writes.
  The initial unadapted run is retained in the logs. Production instruction
  storage code was not changed for the test environment.
- Three common-template tests passed directly: adaptation, explicit save/conflict
  and preservation of existing/empty rules across both agents and locales.
- Localization check passed: 587 workspace strings and app/shared placeholders.

UI verification uses real timeline, row, output, scaling, language, dialog and
scroll implementations. Unrelated file/navigation/store/terminal boundaries have
test stubs. AppCompatTextView is represented by a platform TextView shim, and
R constants are generated for legacy resource loading. The test-only upstream
ProxyMaker adapter permits Robolectric 4.4 to run on Java 21. These conditions
bound the evidence; this is not a full Gradle application build.

Commands, dependency setup and detailed logs are in
`.verification/devkit-timeline/` (relative to the project root).

## Private APK build and final-file checks

Built 2.1.9-test1 /1541 with Gradle 7.2, AGP 4.2.2, local OpenJDK 11,
compileSdk35 / targetSdk28 / minSdk26. Host: Ubuntu 26.04 aarch64. The ARM64
suffix names the application ABI; it does not replace the Ubuntu build environment.
The newer Android ARM64 AAPT2 from the verified DevKit source kit was used only
as a build tool. It is not packaged in the app. The distribution's older AAPT2
could not read SDK35; initial failures and logs are preserved separately.
Both build and analysis use repository-local SDK/JDK/cache locations.

The full application Java/resources/DEX/package build succeeded. The release
signer guard and pinned X11 input guards ran. The old APK plugin's legacy vital
Lint emitted rule-loading diagnostics; that output is not counted as a Lint pass.
The independent full modern Lint gate is recorded separately below.

The original private signer was imported from the user-provided encrypted
source/signing archive, kept outside distributable files and never logged.
No agent authentication files were imported. No new signing key was generated.

Final APK:
`build/apk/Termux-Jobs-v2.1.9-test1-arm64.apk`.
Bytes: 40699523. SHA-256:
`ff77f763b1271fc7dc45003f79cd2664c928c56ef31b615dbd8725aba0b2f8d0`.

Checked anew on that file:

- ZIP integrity, no duplicate entries and four-byte alignment of all 368 stored
  payloads other than signature metadata.
- `com.termux`, 2.1.9-test1 /1541, arm64-v8a only, non-debuggable;
  unchanged SDK levels, package and certificate; versionCode increases from1540.
- APK Signature Scheme v2 verified. Certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`,
  also verified against the supplied 2.1.8 APK.
- 14,645 DEX definitions; required file APIs present. All 31 unresolved types
  have existing explicit optional/API compatibility notes. This inventory does
  not establish runtime method linkage or phone acceptance.
- New timeline widget classes present in DEX. All four changed assets match
  the current production sources byte for byte. No baseline assets/native
  entries missing; no signing/authentication files packaged.
- The 657 production Java/resource/manifest/asset inputs match between the
  root source tree, APK stage and modern analysis stage (excluding native
  build sources and Python caches, which the staging script omits).

Modern Lint 7.4.2: all five modules requested, zero errors, 301 warnings,
no rule-loading/Kotlin diagnostics and the existing compileSdk35/AGP support
warning retained. The first run found AppCompatCustomView in the imported action
label; it now extends AppCompatTextView. The full application was rebuilt and
all 36 UI tests rerun after that fix. Unchanged module analysis was reused after
matching source/configuration/tool/dependency inputs; app analysis ran again.
No warning baseline or blanket suppression was added.

The Gradle analysis succeeded. Report postprocessing initially inherited an
obsolete Ubuntu-library LD_LIBRARY_PATH, which prevented this environment's
Android Python from loading pyexpat. The unchanged gate checker was rerun with
the normal environment: PASS. Both the original wrapper failure and corrected
result are preserved in `.verification/apk-build/`; the analysis was not skipped.

Delivered `/sdcard/Download/Termux-Jobs-v2.1.9-test1-arm64.apk` and its `.sha256`
sidecar; the copied file's SHA-256 matches the checked artifact above.
Logs/JSON: `.verification/apk-build/` and `.verification/modern-lint-report/`.

## Reused

No earlier functional PASS result was counted as a new run. The existing SDK
JAR and UI dependency JARs were read in place; no shared SDK was modified.

Unchanged ARM64 native payloads and generated X11 assets were taken from the
supplied 2.1.8 APK after verifying SHA-256
`3a2198eb0bf6f69d5f00d63cba65229042aee2b0189013f4d473ee712cc344a7`.
Native source inputs were compared with the imported baseline and unchanged.
All three packaged native libraries match the baseline bytes and are compressed
in the APK; no native compilation or driver functionality is claimed.
The only changed assets are the two Codex templates, codex-chat.cjs and notices.
Existing Claude templates, runtime/desktop/driver payloads and storage formats
remain unchanged. Full reuse hashes: `reused-native-inputs.json` and
`apk-validation.json` in `.verification/apk-build/`.

## Not checked

APK installation/publication, real-device geometry/animation, live CLI subagent
history, clipboard/export on Android and migration acceptance remain unverified.
Tests use a fake RPC, not a real model/provider. The private APK checks above
complement source fixtures; neither establishes
real-device acceptance. Existing user instruction files are not automatically
replaced. Installation and publication have not been performed.
