# Choosing verification scope

This policy applies to development checks, intermediate APKs and public releases.
It supplements [AGENTS.md](../../AGENTS.md), [build notes](../../FORK.md),
[Lint](LINT.md) and [release requirements](RELEASING.md). It does not authorize
installing an APK, changing a user's environment, stopping sessions or publishing.

## Start with the change and its dependencies

Before editing, identify the affected behavior, entry points, callers, shared
helpers, build/runtime dependencies and meaningful risks. Include indirect
dependencies: chat layout can share host geometry with X11; an instruction editor
can share persistence or permission/command execution code. A filename or feature
label alone is not a sufficient scope decision. Record a short verification plan.

Run tests for those paths and meaningful failures. Use the full relevant suite
for broad changes, changes to common mechanisms, or dependencies that cannot be
bounded confidently. Expand the plan when review or a failure exposes another
affected path. A full automated suite does not mean launching every desktop,
installing every distribution or exercising every driver without relevance or
authorization.

Do not launch X11, environment installation, GPU/driver or component integration
checks solely because an instruction editor or chat formatting changed. Include
them when a traced dependency warrants them. Build-time verification of packaged
X11 hashes is different from running an X11 server or a GPU test; do not remove
packaging guards to avoid an unrelated functional test.

Changes to process ownership, privileges, cancellation, termination, recovery or
session isolation retain their meaningful security and lifecycle checks, including
unrelated-process survival and error/cancel races. A small diff does not reduce
these obligations. Known critical regressions or unresolved termination safety
block completion.

For chat shutdown changes, verify that closing one chat stops its own agent and
dedicated SSH tunnel while other chats and shared sessions or tunnels keep
running. Exercise stale identities and shutdown races where applicable. Use
captured ownership, PID and start time to check the targets at signal time.
Intercept signal APIs in fixtures and assert the selected targets and the absence
of signals when ownership is uncertain.

## Source and build stages

From the repository root, review `git status --short` and the working-tree and
staged diffs before editing. Inspect relevant untracked files separately.
Preserve unrelated changes. For a commit in scope, review the staged file list
and full `git diff --cached`, and run `git diff --cached --check`. Keep private
data, signing material, APKs, tool caches and temporary build outputs out of the
commit. Publishing and rewriting history require applicable authorization.

Keep source edits in the checkout. When using an isolated build or analysis
stage, synchronize task files through the established staging path. Compare
relative paths and file hashes against the checkout for the source inputs used
by the build and checks. Include modified, new and deleted inputs. Record
expected stage-specific transformations and generated inputs, and resolve
unexpected differences before relying on the result.

## Intermediate checks and static analysis

When a failure comes only from the test environment, preserve the first failure
and fix the relevant test configuration. Rerun the failed check and the checks
that depend on the changed script or configuration. Run the full applicable
suite when a common mechanism changes or when release rules require it.

| Change | Minimum scope to assess and record |
| --- | --- |
| Documentation only | Review wording against implementation, links, diff/whitespace and preservation of existing edits. No APK rebuild or Lint run solely for prose. |
| Local Java/UI/resource behavior | Compile affected code/resources, relevant JVM/UI tests, source/security review, applicable locale/resource checks. Choose compatible targeted Lint where its coverage is understood; otherwise use the existing full modern Lint runner. |
| Python/Node/shell helper | Relevant helper tests and syntax/static checks; include callers, trust boundaries and packaging when changed. No unrelated desktop/driver execution. |
| Manifest, Gradle, shared dependencies, runtime or common lifecycle | Broaden to affected consumers and variants, dependency/runtime review and appropriate modern Lint. Use the full relevant suite when impact is broad or uncertain. |
| Native/desktop/installer/driver behavior | Relevant build, archive/ABI and integration checks, including clean fixtures and failure/cancel cases where applicable. Distinguish fixtures from real device graphics. |

For each intermediate build, state which static checks were selected and why,
what they cover, and what remains unchecked. Successful compilation is not Lint.
The current modern-Lint runner always analyzes all five modules; it is a valid
conservative choice, not a claimed incremental analyzer. An unavailable or omitted
check is recorded as unverified, never as PASS.

Before every public release, run the full compatible modern Lint gate described
in [LINT.md](LINT.md), including all five modules, dependency analysis and report
infrastructure checks. Targeted intermediate analysis and old Lint reports do
not replace this gate. Preserve warnings and toolchain limitations in the report.

## Native tools and packaging alignment

Before declaring a native tool supported, check the executable's ABI, interpreter
and dependency closure in the environment that will run it. Inspect ELF metadata
and verify that the loader can resolve required libraries, including indirect
dependencies. Account for Android/Bionic and Ubuntu/glibc separately. Run a small
representative operation in the selected environment and check its exit status
and expected output. Record the tool identity, environment and any unavailable
checks.

On the final APK, verify applicable ZIP data-offset alignment for stored
entries. Inspect packaged ELF `LOAD` segments against applicable page-size
requirements. Also verify the required ZIP page alignment for uncompressed
native libraries loaded directly from the APK. Apply 16 KiB checks where
required by the target and packaging mode. Record ELF and ZIP results separately.

## Reusing evidence

Reuse a previous result only after confirming equality of the inputs significant
to that result: relevant production and test/fixture/helper sources, resources,
manifests, configuration and build flags, resolved dependencies, toolchain and
native inputs, ABI/variant, and relevant execution conditions. Include dirty and
new files; commit ID, timestamps, Gradle UP-TO-DATE or an old success log alone
do not prove this equality. Use small input inventories with paths/hashes and
sanitized tool/condition summaries rather than copying an SDK or a rootfs.

For example, a JNI output can be reused if its sources, headers/sysroot, compiler,
linker, flags and input archives match its recorded build and its output hash
still matches. Functional graphics evidence additionally depends on renderer,
driver, server, environment and device conditions. Identical sources cannot prove
that an upstream repository, network, Android permission or live server is still
unchanged. Recheck such state when the current claim depends on it.

When significant inputs cannot be established, rerun the affected check or record
the gap. Never copy a successful result into a new run as though it executed.
Use these separate labels in every validation summary:

- **Checked anew:** check, tested input identity, result.
- **Reused:** original evidence, compared significant inputs
  and conditions, equality proof, and limits of the carried result.
- **Not checked:** missing/omitted check, reason and smallest
  remaining check. An out-of-scope check is not a failure or an implied success.

## Every delivered APK

Verify the actual final APK after its last packaging/signing operation, including
intermediate builds: archive integrity, package ID, versionName/versionCode, ABI,
debug status, signing certificate and signature, SHA-256, applicable packaged
runtime/DEX/native/asset requirements, and applicable
[ELF and ZIP alignment](#native-tools-and-packaging-alignment). Compare
package/certificate and increasing versionCode for an update. Inspect for
unintended private data and credentials. Functional evidence for unchanged
components does not waive this final-file gate.

`scripts/verify-apk-runtime.py` inventories DEX definitions/references and reviewed
optional types; it does not verify all APK identity/signing properties or prove
method linkage/device behavior. Use it on the final file where applicable alongside
archive, manifest, ABI and signature inspection. `build-debug.sh` and
`build-local-arm64.sh` do not implement the whole delivery checklist themselves.
Release updater metadata and checksums must refer to that same verified file.

## Existing entry points and minimal separation proposal

The following describes the inspected scripts, not an implemented new scheduler.
Scripts and their integrity gates remain unchanged by this documentation update.

| Entry point | Current unconditional work and implication |
| --- | --- |
| `scripts/check.sh` | Requests the four modules' JVM suites. Useful as a broad entry point; too broad as the automatic response to a narrow change. It does not cover every Python/Node/native/device check. |
| `scripts/verify-java.sh` | Runs `prepare-x11.py`, recreates the Java source stage, then requests the four modules' JVM suites. Even a narrow chat test therefore prepares graphics inputs and runs unrelated JVM cases. |
| `scripts/prepare-x11.py` | Validates cached/downloaded inputs but also regenerates keyboard/style outputs, extracts headers/codecs and recompiles compatibility shims on each call. It verifies optional bridge archives even when their outputs are not bundled. It does not launch a desktop or install guest packages. |
| `scripts/build-local-arm64.sh` | Always prepares X11, stages sources and compiles/links both Termux JNI libraries, then packages and audits DEX runtime. Unchanged native inputs could avoid rebuilding once their full build identity is proven. It also deletes/replaces named packaging outputs; this is not a general cleanup authorization. |
| `scripts/prepare-java-verification.py` | Recreates staged module `src` trees and copies project sources, preserving build caches. It does not copy an SDK or Linux environment, and does not itself execute tests. Do not restage while a build uses that stage. |
| `scripts/verify-modern-lint.sh` | Recreates the analysis source stage and runs the complete five-module modern Lint check. Required before public release; potentially broader than an intermediate change needs. |
| `.github/workflows/fork-ci.yml` | Requests toolchain setup, localization, broad JVM tests and a debug APK for every matching push/PR, including documentation-only changes. Its debug artifact is not a fully verified release APK. |

Minimal follow-up: keep input preparation, source staging, selected test execution,
APK packaging, final artifact audit and full release Lint as explicit steps.
For an already prepared, input-verified stage, invoke existing Gradle test tasks
with class filters (`--tests`) or the relevant Python/Node test file. Preparing
all graphics assets need not be a side effect of selecting a chat test. When
changing automation, add a fail-closed input/output identity check for reuse;
missing/mismatched inputs trigger explicit preparation or an actionable failure.
Keep required packaging/runtime checks active. Do not introduce unconditional
skip switches, fabricated PASS records or a complex new verification framework.

Reuse the existing repository-local SDK/JDK, dependency caches and verified input
archives across runs. Add only missing required SDK components in the permitted
local tool directory. Do not create full SDK/rootfs copies for each check. Keep
compact input hashes, commands, tool versions, check results, artifact identity
and essential failure excerpts. Preserve existing evidence, generated files and
user data; do not run cleanup or stage/output deletion merely to apply this policy.

<a id="чистая-установка-как-основа-для-каждого-окружения"></a>

# Clean installation as the baseline for every environment

The developer requires installers to start from a clean app and bootstrap and
a new instance of each selected environment. Determine required components from
that baseline, regardless of what happens to be installed on the developer's
phone or in an existing rootfs. Maintain an explicit set of required desktop and
runtime packages for every supported target, including components absent from the
distribution metapackage. Request the entire required set in the package manager
transaction even if some packages are already installed. Let the package manager
determine installed state and resolve dependencies. Keep optional application
dependencies in the installer for the corresponding application.

Fail verification when a required desktop executable is missing. The presence of
a metapackage, panel or window manager is insufficient. Cover missing components
in isolated tests. Successful runs on an already configured phone establish
regression coverage only. Report installation on a fresh device and GUI testing
separately. Clearing user data or changing installed environments to simulate a
clean installation requires authorization.

# End-to-end checks for new installation behavior

New environment or component installation paths require successful real package
manager transactions in disposable, initially clean project fixtures. Archive
checks, dependency simulations, mocks and negative tests alone are insufficient.
Check both empty download caches and cached installs, required executables,
reruns, cancellation and resumption. An installed phone rootfs or development
packages cannot serve as evidence of a clean baseline. Distinguish fixture and
package checks from GUI checks on a fresh device. Record unavailable checks
before delivering the result.

<a id="регрессионные-проверки-интеграции-x11"></a>

# X11 integration regression checks

Optional component helpers must accept valid Android private directory aliases
(`/data/data` and `/data/user/0`) by filesystem identity and reject a different
directory with the same basename. Before delivering an APK, check the actual
helper entry point with aliased paths. Repository cache tests alone do not cover
Android `Context.filesDir` and `PREFIX` differences.

Check driver removal against a previously used driver directory containing helper
files and nested runtime caches, in addition to a fresh import of libraries and
metadata. Preserve checks for the protected preset, desktop locks in every
environment, and deletion without following symlinks. Verify that external
symlink targets, other drivers and original ZIP files on the phone are preserved.

Live XFCE scaling checks must restore the original leaf properties with XML type
`empty` (unset), including DPI and cursor size. Check 75/100/150/100 with a real
isolated xfconf rather than relying solely on a permissive mock. Restore an empty
leaf as an unset scalar and preserve other properties without recursive clearing.

Rotation changes must cover configuration events received before layout,
intermediate embedded and fullscreen sizes, delayed surface callbacks, rotation
in both directions and IME clipping. Apply the logical mode together with host
geometry. Use the new surface dimensions when computing the new orientation.
Cancel pending geometry callbacks when the host detaches or closes. Restrict a
requested single-environment trial to that environment in both the Activity and
its dedicated service until device verification.

Coalescing geometry changes alone cannot establish X11 frame readiness after
rotation. When changing fullscreen presentation, count View attach/detach
events, verify parent and window token continuity, and restore layout and window
policy on Back, return to portrait, switching and closing. A failed device test
remains a failure even when unit tests pass. Native surface and buffer timing
still requires separate device verification.

Surface reuse changes must distinguish resize-only callbacks from replacement of
the Surface or control identity or a change of format. Use public APIs and
conservatively rebind when surface identity is unknown. Check rebinding on
replacement and after surface loss. Preserve synchronous release of native
resources on suspend and destroy. Keep environments outside the trial on the
existing path until the limited trial passes device acceptance.

## Native agent permission chains (explicit security check)

Run `bash scripts/test-native-permission-chain.sh` when checking the connection
between the app's permission buttons and actual Gemini/OpenCode tool effects.
It requires the existing pinned Gemini 0.64.0 bundle, OpenCode 2.0.28 executable,
local Java/Android verification toolchain and gcc. It does not install prerequisites.
The native test Java source lives in `scripts/tests/native`, outside normal app
unit tests. Missing prerequisites or kernel enforcement is a failure, never a skip
reported as success. The explicit runner disables test-task output/cache reuse.

Before any native CLI is launched, the runner builds the test-only seccomp user
notification supervisor and runs enforcement probes. Only listeners created by
that filtered process tree are reachable, on loopback or owned abstract Unix
endpoints. External addresses, a host-side localhost sentinel, UDP/packet sockets,
direct native syscalls and exec inheritance are checked. A configured baseURL,
proxy, preload hook, or a successful `unshare` command is insufficient evidence.
The runner does not change the host firewall, application connections or rootfs.

The chain exercises real Robolectric permission buttons and production approval
adapters with real native tools. HTTP replaces only the Android/proot control
transport. For OpenCode outside-project file actions, separate native directory
consent is an explicit fixture precondition; native directory rejection is tested
separately. Gemini's automatic reads and path validation are recorded separately
from a user refusal. See [stage two audit](SECURITY-AUDIT-STAGE2.md) for evidence,
the failed early isolation probe, limits and the device checklist. This is not a
filesystem sandbox, a complete traffic audit, or proof of phone behavior.


## Lesson from 2026-11-02 — Native CLI indicators represent different states

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-02 — Native CLI indicators represent different states

- **Context.** The investigation examined built-in indicators for the agent
  button while keeping the working interface in place.
- **Finding.** In Codex 0.166.0, the working status line uses a dot and a text
  shimmer. The Braille indicator from `loading_glyph` follows a separate code
  path. In Gemini CLI 0.64.0 and 0.65.0, the colored `GeminiRespondingSpinner`
  appears only in `Responding` when no hook is active. When adapting an
  indicator, preserve what the event means or explicitly label the change in
  meaning.
- **Applicability.** This applies to the listed versions and the built-in
  components reviewed. Compare the source for other versions or configurations.
  A built-in glyph alone leaves the existence of an official logo animation
  unverified.
- **Evidence.** The assistant read the official sources at the exact tags. The
  Gemini components matched byte for byte across both versions. The links,
  hashes, and preview limitations are in
  `.verification/agent-native-previews/README.md`, `sources.json`, and
  `validation.json` in the checkout. The check covered source analysis. Phone
  integration remained unverified.
- **User decision.** The user rejected the built-in CLI indicator previews in
  `.verification/agent-native-previews` as the design for this update. The
  findings serve as technical reference. Treat the previews as a rejected
  proposal for the button design.


## Lesson from 2026-11-02 — Verify that HTML and GIF links open from chat

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-02 — Verify that HTML and GIF links open from chat

- **Context.** The investigation covered local animated mockups and the
  installed Termux Job’s test29 / 1464.
- **Finding.** In test29, an underlined absolute path receives a `URLSpan`, but
  `AgentMarkdown` accepts only HTTP(S) and silently drops the local path. An
  attachment card can still lack interactive viewing. Codex HTML output has
  type `binary`, and a GIF displays as a static `Bitmap`. When delivering a
  result like this, check the full path from the tap to the viewer, including
  the file's existence and contents.
- **Applicability.** The confirmed limitation applies to test29. A claim that
  animation works from a local link requires a corresponding fix. Test30
  changed the route and viewers. The fixture checks are described below.
  Acceptance of Android WebView behavior still requires a phone check.
- **Evidence.** The assistant inspected the installed APK and actual Ubuntu AI
  files, ran four isolated JVM cases, checked a repeat tap with selectable
  text, and examined projection code that made zero CLI calls. The report is
  `.verification/local-file-link-diagnosis/REPORT.md` in the checkout. The
  physical tap was not instrumented. The report predates the fix.

- **Test30 follow-up.** Bind the local `URLSpan` and card to a copy of the
  profile during rendering so a deferred handler uses the original chat
  environment. The shared viewer uses independent file transport. HTML and
  GIF each get a single virtual document with the app bridge and file access
  disabled. Start loading the WebView once it has a nonzero viewport. The
  assistant checked isolated links and cards for all four agents, profile
  switching, errors, and WebView cleanup. JavaScript and animation on a phone
  remained unverified. The report is `docs/fork/VALIDATION-test30-local-files.md`
  in the checkout. The test29 findings describe the old implementation.


## Lesson from 2026-11-02 — Reach the edge of the upper platform before descending

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-02 — Reach the edge of the upper platform before descending

- **Context.** This was a standalone mockup of the roaming Codex robot. App
  integration had yet to begin.
- **Finding.** If cleaning is interrupted, a jump from an interior point on
  the upper tab directly to the lower one creates a parabolic path that
  intersects the upper tab. Move along its edge to a safe descent point
  before descending. The new request activates immediately.
- **Applicability.** This applies to routes across DOM or View platforms and
  any later integration with the same geometry. Checking all collisions on a
  phone requires separate verification.
- **Evidence.** The assistant ran `collision guard rejects diagonal descent through a tab but permits edge-first return`
  and interruption checks for walking and wiping in
  `.verification/codex-roaming-robot/check.cjs`. The README and `checks.json`
  in the checkout record the results.


## Lesson from 2026-11-03 — Partial Claude snapshots and opening the form after a read

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-03 — Partial Claude snapshots and opening the form after a read

- **Context.** Claude history disappeared after a refresh, and the Create
  project instructions form (`Создание инструкций проекта`) sometimes failed
  to open.
- **Finding.** Pass a limited snapshot to the shared `AgentChatUi` as
  `partialHistory` and include cursors for hidden rows, including hidden parts
  of a single message. Reserve complete `knownMessages` for full snapshots.
  Recover the full body of an old response from the native JSONL because the
  live cache is limited. Claude uses its native `uuid` as the record key
  because the provider `message.id` can repeat. Claude and Codex share the
  same scrolling, cache, and viewing behavior.
- **Conditions.** This covers the main Claude 2.1.296 path with `parentUuid`
  and same-file `logicalParentUuid`. Leave unknown cross-file relationships
  unresolved and preserve native files.
- **Evidence.** The assistant checked large JSONL fixtures, a file larger than
  64 MiB, a full Unicode response, and the real CLI in isolation with a local
  provider. The comparison covered 52 native records and 14 messages with no
  omissions. Shared Claude and Codex tests preserved the anchor and offset
  when prepending, streaming, and reopening. The report is
  `docs/fork/VALIDATION-test39-history-instructions.md` in the checkout.
- **Form.** An early tap was lost because the busy path returned immediately.
  Bind the single queued request to the agent, environment, and project. Keep
  a reference to the open child dialog to prevent a duplicate form. Android
  button tests must run the main-thread queue. That test requirement supplies
  no basis for adding product delays. Opening the form and checking for
  flicker still require phone acceptance.


## Lesson from 2026-11-03 — Existing project files open in the editor

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-03 — Existing project files open in the editor

- **Context.** In the latest video, Codex was already showing an existing
  `AGENTS.md`, while Claude opened the form for a missing `CLAUDE.md`. The user
  confirmed this explanation.
- **Finding.** Match the action label to whether the base file exists, including
  when viewing an override or local file. For an existing file, show Edit
  (`Редактировать`) and focus the editor without adding a status line. For a
  missing file, open the creation form. The evidence established which screen
  opened and did not establish a window closing.
- **Applicability.** This applies to the shared `AgentInstructionsUi` and the
  responses from `agent-instructions.py`. The draft and write revision are
  tied to the original context. The user explicitly directed the removal of
  voice input. Historical reports grant no basis for restoring it.
- **Evidence.** The assistant compared the latest video, Java branching, and a
  read-only call to the real Python backend. The root `AGENTS.md` existed and
  remained untouched. `CLAUDE.md` was absent. The evidence in
  `.verification/instruction-window-trace/` documents the observed behavior.
  Phone verification of a new APK was outside that check.


## Lesson from 2026-11-03 — Incremental packaging can retain unused ZIP records

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-03 — Incremental packaging can retain unused ZIP records

- **Context.** Required notices were being added to a stable APK candidate.
- **Finding.** After the DEX changed, the APK grew by 3 MB while its file list
  changed very little. An offset check found a 3,096,057-byte gap between
  active entries with the signature of an old local ZIP header. Verify gaps
  between entries in addition to inspecting the ZIP and DEX listings. The assistant
  preserved only the task-owned `packageRelease` state and output before
  packaging again in a fresh output directory. The final APK measured
  40,452,595 bytes. The payload hashes were rechecked and remained the same.
- **Applicability.** This occurred with AGP 4.2.2 incremental packaging in the
  checked environment. Execution of old DEX or exposure of a secret was never
  established. Preserve the original evidence, leave the SDK, caches, and user
  data in place, and verify the exact final bytes.
- **Evidence.** See `.verification/release-v2.1.2/incremental-zip-gaps.json`,
  `before-fresh-packaging`, `fresh-packaging.log`, the final audit, and
  `docs/fork/VALIDATION-v2.1.2.md` in the checkout.


## Lesson from 2026-11-03 — An initial Gemini snapshot can leave old action times unknown

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-03 — An initial Gemini snapshot can leave old action times unknown

- **Context.** Version 2.1.3-test3 displayed `HH:mm` only in timeline group
  headings.
- **Finding.** The first `observe` can contain empty history before the CLI
  restores it. Assign observed times only after establishing a ready baseline
  and while a response is active. This avoids marking all tool IDs that first
  appear in the next snapshot with the current time. Keep saved timestamps on
  refresh and restart. Hide old timestamps when their values are unknown.
- **Applicability.** This covers the existing Gemini React history projection
  and private timing metadata store. Native action times for other CLIs need
  independent evidence.
- **Evidence.** `scripts/tests/test_action_event_times.cjs` covers four agents
  and a separate idle-loaded baseline. `CodexActionTimelineTest` checks time
  zones, refresh, and narrow headings. The report is
  `docs/fork/VALIDATION-2.1.3-test3.md` in the checkout. A phone check remains
  separate.


## Lesson from 2026-11-03 — Check connectivity and working context separately

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-03 — Check connectivity and working context separately

- **Context.** At the user's request, setup continued on a second phone. The
  official Codex 0.166.0 was installed alongside 0.163.0. npm integrity was
  checked, and the main binary matched the one working on the first phone.
  Bootstrap prefers `codex` on `PATH`, so the standard `/usr/local/bin/codex`
  takes priority over the private 0.163.0 fallback. The old directory remained
  intact.
- The independent Hysteria and SSH launchers used the existing
  `provider=openai`. Hysteria produced `timeout/no recent network activity`
  with the user's VPN and worked with the VPN off. Both routes completed two
  genuine successive responses with the VPN off. That observation applies to
  the tested setup.
- The first SSH probes on the large history did not finish within 180 seconds.
  Codex ran compaction automatically during a successful direct VPN probe.
  The initial statement that compaction was unnecessary (`не пришлось сокращать`)
  was inaccurate and was corrected. The normal compact and start sequence over
  Hysteria then finished, and the response in the same chat took 6.75 seconds.
  A new process over SSH resumed the same chat and completed its response in
  20.57 seconds including startup. The effects of route, version, and context
  were not isolated. The role of images remained unresolved.
- Compaction preserved the old conversation in the checked Codex 0.166.0
  legacy JSONL. The original 32,761,712-byte prefix remained byte-for-byte
  identical, and compaction appended a record with replacement history. Measure
  the model request separately from the new JSONL size. Chat age and the sizes
  of locally linked ZIP or STL files do not determine the model's context size.
  Track image-view results separately.
- Run live account tests only with the user's permission, using short
  diagnostic input and no project tools. The stdio `app-server` check covers
  the protocol path. The UI requires separate acceptance. Keeping the old
  binary leaves database compatibility after an upgrade unverified.
- **Evidence.** See `.verification/wife-route-20261103/phone-fix/REPORT.txt`,
  `compaction-timeline.json`, `final-history-preservation.json`,
  `model-*-two-turns.jsonl`, and `model-ssh-postcompact.jsonl`. All ten test
  tunnels ended, and the shared server services retained their identities.
  The diagnostic code did not read account keys.


## Lesson from 2026-11-03 — CLI network checks require system call restrictions

Moved on 2026-11-04 with the historical results intact. The evidence reflects
conditions on the check date. See the [main plan](SECURITY-AUDIT-STAGE2.md) for
current status.

### 2026-11-03 — CLI network checks require system call restrictions

- **Context.** During the second audit stage, an early OpenCode fixture
  configured the legacy provider and settings incorrectly and received an
  external provider's HTTP 403. The request body, exact URL or IP address, and
  full file-open trace were not retained. The transmitted data is therefore
  unknown. The investigation did not read real credentials.
- **Evidence for this environment.** `unshare --user --map-root-user --net`
  exited with status 0, but the namespace remained the same and the child
  reached the parent's sentinel. This observation applies to the checked
  environment.
- **Fixture setup.** The explicit runner `scripts/test-native-permission-chain.sh`
  applies seccomp notification and `ADDFD` to the CLI process tree. It allows
  only the runner's registered loopback and abstract endpoints. Negative probes
  covered direct system calls, IPv6, UDP and packet sockets, SCTP-like streams,
  Fast Open, and descendants launched via `exec`. The host firewall and app
  settings stayed in place. The fixture leaves file isolation and resistance
  to a hostile same-UID process unverified.
- **Evidence.** The checks covered 25 native scenarios and 18 actual UI taps
  in Robolectric. Source and output freshness alone is insufficient. A separate
  init script disables reuse and caching for the explicit native task. The
  guard observed and blocked additional OpenCode probes to localhost ports
  1234, 11434, and 8000. See `docs/fork/SECURITY-AUDIT-STAGE2.md` and
  `.verification/security-stage2-followup` in the checkout.

## Project verification requirements moved from AGENTS.md on 2026-11-04

This material was moved on 2026-11-04 with its historical results intact. The
results apply to the dates of the checks. See the
[main audit plan](SECURITY-AUDIT-STAGE2.md) for current status.

# Verification in proportion to the change

Before editing, identify affected functions, callers, shared helpers,
dependencies, and significant risks, including indirect effects. Write a short
verification plan. Run the relevant behavior and error checks. Run the full
applicable suite for broad changes, changes to shared mechanisms, or changes to
dependencies whose scope you cannot confidently determine. A change limited to
the instructions editor or chat styling does not by itself require starting X11, installing
environments, or running driver or component integration checks. Trace shared
layout, storage, permission, and execution paths before ruling out these checks.

Apply the clean installation and X11 regression requirements when the change
affects the corresponding behavior or its dependencies. Checking APK
inputs and hashes is a separate step from starting graphics or installing guest
systems. Keep all applicable checks for process ownership, safety, cancellation,
and recovery, even for a small diff. Unresolved process-stop safety still blocks
completion.

Reuse earlier results only after confirming that the relevant production and
test sources, configuration, resolved dependency versions, build tools and
inputs, and execution conditions match. Account for changed and new files. An
old successful log, commit ID, or `UP-TO-DATE` message alone is insufficient.
Report **Checked anew**, **Reused** (with evidence that the relevant inputs and
conditions match), and **Not checked** (with the reason and the remaining check)
separately.

For every delivered APK, including interim builds, verify the final file's ZIP
integrity, package ID, `versionName` and `versionCode`, ABI, debug status,
signature and certificate, SHA-256, and applicable checks for the included
runtime, DEX, native libraries, and assets. These checks remain mandatory when
reusing functional results. For updates, compare the package ID and signer and
verify that `versionCode` increases. Check for inadvertently included private data.

For interim builds, explicitly select and justify the static checks. Include
compilation and any relevant syntax, localization, and resource checks. Use
compatible targeted Lint when coverage is clear, or full modern Lint when the
impact is broad or unclear. Count only completed, successful checks as passes.
Full modern Lint for all five modules is mandatory before a public release. For
documentation-only changes, check the text, links, and diff. The text alone
requires neither an APK rebuild nor a Lint run.

Review the existing scripts before choosing an entry point. If an entry point
always performs unrelated work, propose the smallest separation of preparation,
selected tests, packaging, final-file audit, and release Lint. Keep broad skip
flags, fictitious passes, and complex verification infrastructure out of the
project. Reuse the existing local SDKs, tools, and caches without copying whole
SDKs or environments for each run. Keep compact records of input identifiers
and results. Obtain permission before deleting existing evidence, generated
files, or user data. See [verification scope and script review](TESTING.md), [Lint](LINT.md), and
[the release procedure](RELEASING.md).

# Evidence for completing a refactor

Before declaring a refactor complete, assess each of these areas independently.

- Run tests for the changed behavior and significant error cases. A passing
  existing suite alone leaves coverage of the new path unproven.
- Review code for regressions, handling of secrets, permission and trust
  boundaries, and effects on unrelated terminal sessions and processes.
- Trace the applicable call paths and state transitions. Cover startup,
  switching, closing, cancellation, error handling, and recovery.

Do not invent a numerical confidence percentage. When asked for an assessment,
use 40/30/30 if helpful as qualitative priorities for tests, code review, and
tracing behavior. These weights carry no calibrated probability and do not
substitute for evidence.

A known critical regression, exposure of a secret, or unresolved safety issue
when stopping an affected process blocks completion regardless of how many other
checks passed. If evidence is insufficient, report what you checked, the main
gaps, and the smallest remaining checks. Report source and build checks
separately from device checks. UI mocks alone cannot verify keyboard behavior,
gestures, or other device-dependent behavior on a phone. Choose checks in
proportion to the change. Avoid redundant tests and broad unrelated refactors
undertaken solely to improve the assessment. User authorization for APK
installation or publication remains separate from technical verification.


<a id="реорганизация-рабочего-контекста--2026-11-04"></a>

## Working context reorganization on 2026-11-04

At the developer's direction, `AGENTS.md` was shortened to the mandatory
restrictions, paths, and a guide that specifies when to read each document.
The sole memory file remained `/root/termux-fork/LEARNING.md`. It was updated
in place outside the checkout and was not added to Git as a second copy.
The reorganization moved 58 blocks of historical results and lessons into
existing topical documents without changing their contents. The detailed
verification requirements remain above. The current audit plan was separated
from the historical record. `CONTINUE.md` describes the active goal. Token and
time savings were not measured.

| File | Before (bytes) | After (bytes) |
| --- | ---: | ---: |
| AGENTS.md | 19,535 | 15,214 |
| LEARNING.md | 92,904 | 25,144 |
| SECURITY-AUDIT-STAGE2.md | 187,778 | 17,013 |
| CONTINUE.md | 1,564 | 5,119 |

The assistant verified byte-for-byte inclusion of all 58 moved blocks, checked
83 links in the current entry documents, fixed two incoming anchor links, and
ran `git diff --check`. The evidence is in
`.verification/context-organization/{before.json,relocations.json,check.json}`.
The reorganization retained mandatory memory reading, restrictions on the
environment, processes, and secrets, authorization, final APK and Lint checks,
X11 and clean installation requirements, updater and compatibility requirements,
CHANGELOG, RU/EN, and attribution requirements, and the conditions for reading
`RELEASING.md`. The general template's size thresholds still apply. The audit
plan remains the source for current audit status.
