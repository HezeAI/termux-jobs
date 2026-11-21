# Confirmed lessons for Termux Job’s

This is the project's sole memory. Apply each entry only to the versions and
conditions it describes. Use [CONTINUE][current] for the current goal and
[AGENTS][rules] for mandatory rules. Memory serves as a reference. Task history,
authorization and evidence of readiness for a new APK belong in their respective
records. Update existing entries by topic, retain dates, conditions and evidence,
and mark conclusions that later evidence supersedes. Keep detailed logs in
topical reports.

On 2026-11-09, overlapping entries were consolidated. The [historical archive][notes]
retains the original observations, measurements, failures and limitations, along
with an index of all entries from that period. Use it as historical evidence.

## Build, release and evidence

- **2026-11-03, 2026-11-08 — Inspect the bytes being distributed.** Check for
  licenses inside the final APK even when the Maven JAR contains them. The
  optimizer renames resources, so look them up through the resource table.
  Incremental packaging can leave extra local ZIP records absent from the
  central directory. Inspect the final container, notices and assets. [Licenses][licenses],
  [ZIP checks][testing], [resources][composer].
- **2026-11-05/08 — Record the toolchain for native reproducibility.**
  Fedora Glycin was rebuilt with Clang 21.1.8 in place of GCC 15.2 even though
  all 95 source and configuration hashes matched. The historical GCC recipe
  reproduced the v2.1.7 ELF. Record the compiler, recipe, inputs and final
  hashes. For v2.1.7, all 1,140 files matched and the container matched after
  removal of the signing block and adjustment of ZIP metadata. Four project
  libraries were rebuilt and three external libraries were pinned binary
  inputs. The scope was local reproducibility. CI release reproduction and
  security auditing were outside that check. [Procedure][repro].
- **2026-11-03/08 — Verify source and workflow independently.** An old source
  commit built by newer CI has two identities. Use Git ancestry to verify
  inclusion of the release tag in the fork and base. An up-to-date README
  leaves that ancestry unverified. In November, the branch
  lacked the musl-dev r2 source fix. Merge `b91078ac` restored it and CI run
  42486622750 passed. Check ancestry, versions and CI against the final main
  branch. [CI][ci], [release procedure][releasing].
- **2026-11-07/08 — Run the full release suite.** Six classes retained outdated
  expectations after the redesign. In 2.1.7, an old test hid the restart path
  for a finished agent. Confirm intended behavior before changing code to
  satisfy an outdated test. Record Termux-path SFTP skips as skipped and
  report local checks separately from CI results. [2.1.5][v215], [2.1.7][v217].
- **2026-11-03/08 — Check signing and GitHub assets independently.** The build
  stops when the previous signing key is unavailable. Preserve the existing
  signer. In 2.1.6, PATCHing the draft returned an untagged `tag_name`. In
  2.1.7, draft lookup by tag returned 404 even though the release ID existed.
  Check the ID, `tag_name`, URL and public bytes. These observations apply
  to the two releases checked. Authorization for backup pairs 1493–1500 was
  historical. Retain backup 1499 and obtain a separate request for any new
  rollback. [Signer gate][signer], [release rules][releasing],
  [decision history][backuphistory].
- **2026-11-21 — Gallery tests on ARM64 OpenJDK 11.** The local Linux
  JDK's JPEG plugin depends on the Ubuntu ARM64 libjpeg.so.8. In Ubuntu AI,
  supply the verified library from a project-local directory to the test
  process. The unchanged gallery-copy test then passes. The Android APK does
  not need this host library. [Release validation](docs/fork/VALIDATION-v2.1.9.md).
- **2026-11-20 — Local ARM64 build tools.** In Ubuntu on aarch64, the old system
  AAPT2 failed to read the SDK 35 resource table. Android AAPT2 from the
  verified DevKit source kit built successfully with its complete set of
  library dependencies. AGP requires the override path to end in `aapt2`.
  In `JAVA_TOOL_OPTIONS`, wrap a temporary path containing an apostrophe in
  double quotes. Keep the Ubuntu-library `LD_LIBRARY_PATH` out of Android
  Python's environment because it breaks `pyexpat` loading.
  [Build evidence and limits][devkittimeline].
- **2026-11-21 — Run Robolectric with the normal JDK 11 test entry point.**
  The direct JDK 21 fixture probes needed a compatibility ProxyMaker and a
  generated legacy R. The Gradle 7.2 runner uses the normal Android test
  resources with JDK 11. Gradle 7.2's bare worker classpath line in its Java
  argument file failed when the checkout contained an apostrophe. Pass that
  classpath as one literal argument on the affected path. Use the short
  `.cx/` test directory before launching the JVM to fit Codex sockets.
  [Runner and retry commands](FORK.md#checks-and-local-builds).
- **2026-11-21 — Antigravity 1.5.13 startup and update probes.** In Ubuntu AI
  the official Linux ARM64 binary returns the version on stdout and successful
  help on stderr. The pinned installer and managed updater both rejected the
  binary when help verification discarded stderr. Capture both streams for the
  help probe and keep output limits, exit status and required flags checked.
  [Validation](docs/fork/VALIDATION-2.1.9-test5.md).
  The same binary can report missing sign-in or emit its native initialization
  frame with stdin open and empty. Start its existing chat worker early and
  wait for initialization before forwarding any draft, because native stdin
  can also be used for authorization. Keep authentication diagnostics private.
  [Initial connection checks](docs/fork/VALIDATION-2.1.9-test7.md).
- **2026-11-21 — Git in the project Changes tab.** An existing `.git` folder
  can coexist with a missing executable in the detached file worker. The
  worker enters the selected environment with its own command path. Keep the
  Git tool selection explicit, bound to that environment, and separate from
  the directory of the repository being listed. [Git verification](docs/fork/VALIDATION-2.1.9-test8.md).

- **2026-11-21 — Project directory names with trailing spaces.** A phone
  report showed that the Ubuntu AI browser kept the exact folder name while the
  test8 session editor trimmed it on save. Preserve the browser result through
  existing-chat lookup, profile serialization and the quoted startup directory.
  A saved truncated path needs the original folder selected again.
  [Release validation](docs/fork/VALIDATION-v2.1.9.md).

## Android UI, history and processes

- **2026-11-02/05 — Verify physical typography on the phone.** In Robolectric
  4.4 on API 28, `visible()` and `isShown()` could indicate visibility
  before the window reached `VISIBLE`, and `ShadowPaint.measureText` could
  return the string length. Use the fixture for geometry and deterministic
  overflow. Verify typography, IME and smoothness on the phone. Check the
  actual HTML mockup viewport. A request for 440 px produced 500 px without
  a separate iframe. An SVG bounding box measured after clipping can hide
  overflow. [Design][design], [text checks][test1].
- **2026-11-03/07 — Composer and dialogs.** `Gravity.BOTTOM` leaves
  `baselineAligned` in effect. Measure the whole view, including font
  padding, and let it shrink after multiline text is cleared. Populate a
  custom dialog before `show()` and call `setMinimumHeight` from layout
  only when the value changes. The first `ACTION_UP` can focus the field
  without calling `performClick`, so check the IME path beyond `OnClick`.
  [Composer][composer], [test9][test9], [test10][test10].
- **2026-11-05/07 — Preserve the operation owner's identity independently of
  the UI.** `Session.copy()` creates new profile and run IDs. Capture the
  original IDs, handle and socket for an operation, then use that snapshot
  to assign delayed results. For the terminal overlay,
  `keepsChatPresentation` controls visibility and geometry, while
  `handlesComposer` controls input. Using the same condition for both
  changed padding and the history position. [Snapshot][context2], [overlay][test16].
- **2026-11-06/08 — Preserve views and callbacks to keep animations continuous.**
  Removing and re-adding a view resets arrival and height animations. A late
  thumbnail can split a group. Reuse the logical card and animator, keep
  height easing independent and use the top pivot. Reusing the old background
  inside a new `RippleDrawable` caused `setBackground` to detach the callback.
  Keep the progress indicator loop available when the header is hidden.
  Identify manual scrolling from input and inertia because layout and restore
  also trigger `onScrollChanged`. [Arrival][arrival], [ripple][test13],
  [indicator][test4].
- **2026-11-06/07 — The terminal draws its own surface.** Transparent default
  cells let the Activity background show through despite the selected palette.
  Paint the canvas and draw the upper transcript when `mTopRow` is 0 as
  well as when it is negative. Incoming text left it at 0 until a gesture.
  Reloading ExtraKeys must preserve the active and locked color. Keep empty
  cells off the two-column Enter key. The old pager created a second input
  field. [Background][test7], [history drawing][test11], [extra keys][test18].
- **2026-11-07/08 — Sidebar.** Read a dynamic name through a `Supplier`
  because a captured `String` retains its old value on refresh. Store manual
  collapse independently of automatic expansion for the current visible
  session and apply the override only when the sidebar opens. Keep long
  names from widening the sidebar. Preserve the position of the compact
  Clear action when removing its repeated heading. [Name][test20],
  [cards][sidebar], [historical layout][test21].
- **2026-11-07 — Chat creation transitions.** Showing a hidden dialog again
  after a new chooser can change the z-order. Use one container with the
  opaque form as its last layer to preserve dimming and the transition, and
  block taps during animation. [Checks][test22].
  **2026-11-21, Antigravity in test3/test4.** The compact form moves a control
  into a slot with a separate heading. Moving a hidden legacy Gemini control
  leaves its heading visible above an empty slot. Put visible read-only values
  in the Antigravity slots and check both languages and an agent change in the
  actual chooser fixture. [Validation](docs/fork/VALIDATION-2.1.9-test4.md).
- **2026-11-03/07 — History and attachments.** Keep partial Claude snapshots
  distinct from complete `knownMessages`. An empty head before initialization
  leaves pagination open. Restore it when the native cursor arrives. Extract
  attachments before truncating text. ProjectFiles needed a bind for the
  selected `chat-attachments` so existing JPEGs could be found. Keep
  `socketroot` out of the mounts. Bind local `URLSpan` before retaining
  reveal text. Identify image views before grouping tools, preserving their
  key and suppression of the matching `tool_result`. [History][history5],
  [attachments][test22], [reveal][test2], [image cards][test19].
- **2026-11-06/09 — Opening an existing X11 session requires a live service
  with a verified owner.**
  The open-existing path atomically checks started state, target and owner,
  and discovers services without `AUTO_CREATE`. In 2.1.7, the test-session
  label appeared on every desktop. Android APIs found a live Arch service
  with no UI clients even though its PID could not be opened under `/proc`.
  Test ownership remained unverified and no signal was sent. The control-only
  path kept the service in the foreground after the component check.
  The 2.1.8-test1 fix passed local checks. Phone acceptance remained open.
  [Open-existing path][test6], [notifications][x11note].
- **2026-11-20 — DevKit timeline rendering.** DevKit 0.1.57 still includes the
  grouped `CodexActionsView`. The active `ChatTimelineView` and
  `TimelineModel` display individual actions through
  `AgentCompactActionView`, with separate web and subagent rows. Trace the
  actual call path before transferring UI design. The user requested this
  timeline and the shared RU/EN Codex prompt. The reference archive was
  verified by SHA-256. [Implementation and validation limits][devkittimeline].

## Agents, permissions and environments

- **2026-11-20 — Ubuntu AI updater host and runtime selection.** On this device
  the installed proot-distro entry point has a Python shebang. Invoke it as an
  executable with separate arguments. A Bash script operand would interpret
  the wrong language. Codex npm 0.171.0 uses a same-package Linux ARM64 tag,
  while Claude Code 2.1.330 references a separate Linux ARM64 package. Their
  native version and help probes passed in isolated Ubuntu/aarch64 homes.
  [Inputs and verification](docs/fork/VALIDATION-2.1.9-test2-cli-update.md).

- **2026-11-20 — Codex CLI in Ubuntu AI.** In this Ubuntu 26.04/aarch64
  environment, the agent PATH includes private Node 22.22.1 and pinned Codex
  0.163.0. A fresh Ubuntu PATH has no Node. After installing Codex 0.171.0
  under `/usr/local`, its npm launcher exited with status 127 until the
  shebang pointed to the existing private Node executable. That was the only
  changed line. Version and app-server help passed with the fresh PATH.
  Existing chat processes and the pinned CLI were preserved. The source
  bootstrap prefers an available global Codex on a new launch before a managed
  update is activated.
  [Local status](.verification/cli-update/status.json).
- **2026-11-02/03 — Instruction and skill loading depends on the CLI.** Claude
  2.1.296 does not guarantee that the same process rereads saved rules. Codex
  0.166.0 discovers `.agents/skills` and the legacy `CODEX_HOME/skills`
  through `forceReload`. Viewing an existing AGENTS or CLAUDE file uses a
  different path from creating a missing one. The shared manager viewport
  needs to preserve pinned actions. [Instructions][instructions], [skills][skills].
- **2026-11-03/04 — Verify permissions on each route.** Codex 0.166.0 rejected
  `untrusted` via CLI and TOML, while typed `thread/start` accepted it.
  Shell and file approvals follow different routes. Gemini file-tool
  restrictions leave the allowed shell available. Use a kernel network guard
  for CLI fixtures because an early OpenCode failure still reached the
  external provider. Test ownership requires more evidence than a directory.
  [Matrix and initial failures][permissions].
- **2026-11-03/04 — Gemini and OpenCode state extends beyond HOME.** OpenCode
  XDG paths and private binds contain authorization, logs and snapshots.
  Bind a Gemini handoff to the run UUID, kernel UID, PID and process start
  identity, alongside the profile. `app_process` under PRoot with UID 0
  used UID 10620 in Binder. Preserve the distinction between a Settings read
  failure and a null result. [Backup scope][backup], [handoff][auth7],
  [Android probe][dictdevice].
- **2026-11-05/07 — Gemini history and usage.** `--list-sessions` and
  `Storage.initialize` can trigger cleanup, authorization and migration.
  Build a read-only catalog from the stored format. An idle event before
  `isContinuation` leaves the request open. Tie timing to the acknowledged
  `prompt_id`. `promptTokenCount` already includes cached tokens. A fallback
  `tokenLimit` leaves an unknown model's context limit unverified. After
  compaction, use only confirmed `post_tokens`. [Catalog][test5],
  [timing][context2], [usage][context3].
- **2026-11-05/07 — Claude 2.1.296.** A queue or stdin event confirms submission
  only. Recover a user attachment by `source_uuid` because multiple messages
  can contain the same text. The SDK exposes `get_usage`, while an API
  profile with no usage windows leaves subscription status unverified. An
  Auto initialization ACK from a synthetic provider covers the protocol
  only. Bypass was rejected when run as root under PRoot. Preserve the actual
  UID. Live and stopped UI had different permission editors. Display the
  mode chosen for the next run separately from the active `system.init`
  value. [History][claude], [usage][test1], [modes][test23], [shared editor][test24].
- **2026-11-07 — Claude image fixture.** Set the launcher with a separate
  HOME and config before the profile's first run. Using a real conversation
  UUID in a fake HOME returns `No conversation found`. The native PNG-to-Read
  path checks the adapter. The phone viewer and cloud model require their own
  checks. [Repeatable fixture][claudefixture].
- **2026-11-02/03 — Environment preparation.** The generic OCI unauthorized
  hint treated distinct 401 and 403 responses as private-image failures.
  The “Checking Ubuntu” status could include pkg/proot preparation. Emit
  the stage before the blocking command and align the allowlist, parser and
  UI. The 25-second inventory timeout does not bound preparation downloads.
  Removing `saveLog` leaves `Job.log`, the current action and `Plan.reason`
  intact. [Environments][env], [logs][preparation].
- **2026-11-08 — Check Arch deletion with real permissions.** Native Termux
  UID 10620 got EACCES for the mode-000 `systemd/dissect-root`, while PRoot's
  simulated root could read it. The old Wayland bind created a different
  specific mode-000 endpoint. Normal exit removed it, and the cause of the
  historical leftover remains unknown. Record the exact unscanned path in
  the deletion plan, use `rmdir` to check emptiness, reject and recover if
  nonempty, and protect neighboring environments. After cancellation, check
  for late writes once the supervisor has returned and the grace period is
  over. Offline restart requires the native conversation ID from the same
  run. Socket and profile UUIDs identify different objects.
  [2.1.6 environments][env216], [2.1.7 Wayland][restart].
- **2026-11-07 — Historical dictation findings.** Service discovery succeeded,
  then `ClaudeRecognitionService` immediately returned error 5. The silent
  Vosk probe did not exercise actual dictation. The user later canceled voice
  input and the model Components. The removal decision supersedes the
  earlier backend work. [Historical investigation][dictoffline],
  [current condition][releasing].

## AgentsNative historical experiment

The AgentsNative checkout and its evidence are absent from the Termux Job’s
source bundle. Paths in this section are historical locators relative to the
AgentsNative checkout. Phone results reflect the original Android reports or
the user's feedback.

- **2026-11-11 — Target 36 host execution, PRoot cleanup and AAB packaging.**
  Pass host scripts from writable files as arguments to `/system/bin/sh`.
  Android mksh supports `set -A`, which is outside portable POSIX sh. The PRoot
  loader lives in the AgentsNative APK and every launch path sets
  `PROOT_LOADER`. Upstream PRoot ignores SIGTERM, so `Process.destroy()` alone
  leaves tracee shutdown unverified. Test6 routed SIGTERM to cleanup. Cancellation
  under the Android app UID remained open. Evidence in the historical checkout
  is `docs/fork/VALIDATION-AgentsNative-test6.md`.

  Test7 found an overlooked ProjectFiles native-host path and changed its
  direct `bash -c` invocation to `/system/bin/sh -c`. Check installers and
  file commands that use the shared transport. Guest Bash through PRoot
  remains supported. In the test11 AAB,
  legacy packaging in `app/build.gradle` left bundle `uncompressNativeLibraries`
  enabled. Local bundletool output contained uncompressed `.so` files at
  4 KiB offsets while the ordinary APK compressed them. Inspect generated
  splits, ELF and the ordinary APK independently. `extractNativeLibs=true`
  alone leaves 16 KiB startup unverified. Setting
  `android.bundle.enableUncompressedNativeLibs=false` produced compressed
  libraries with `extractNativeLibs=true` in the generated split and retained
  the entire `base/` payload. This check covered packaging. A device with
  16 KiB pages was outside its scope. Evidence in the historical checkout is
  `docs/fork/AGENTSNATIVE-PLAY-READINESS.md`.
- **2026-11-11 — Check Android paths and PTYs at their respective layers.**
  Guest PRoot remaps paths. A guest path such as `/data/data/com.agentsnative`
  can be a fixture. Matching hashes for the installed APK and native libraries
  leave live bind configuration unverified. Use a report produced under the
  Android app UID. The test6 Android report dated 2026-11-11 confirmed the
  independent UID, rootfs and Ubuntu PRoot, with no bind from `com.termux`.
  The guest binds `/sdcard` under its own name. It can be accessible even
  when guest `/storage/emulated/0` is absent. Historically in test5, removing
  terminal sessions from the UI left agent PTYs active through
  WorkspaceController → TermuxService → TerminalSession → JNI
  createSubprocess/resize/wait/close. Test6 switched this mechanism to
  ProcessBuilder. Historical reports are
  `docs/fork/VALIDATION-AgentsNative-test7.md` and
  `docs/fork/AGENTSNATIVE-RUNTIME-AUDIT.md`.
- **2026-11-10 — Package identity and Java namespaces.** Per the user's
  request, AgentsNative uses package `com.agentsnative`, its own persistent
  key and its own data. The Java classes retained `com.termux`. The runtime
  uses its own PRoot and Ubuntu/Debian. RTK was excluded. These changes are
  specific to AgentsNative. The historical design is in
  `docs/fork/AGENTSNATIVE.md`.

  The compact creation form rebuilds its list of views. A field added only to
  the shared form is lost there. Check the final form for each agent, as in
  `AgentsNativeCreationTest` on 2026-11-11. Test7 showed that Back opened a
  separate `AgentCreationSteps` list that still included removed agents.
  Check the back transition and settings list too. The historical report is
  `docs/fork/VALIDATION-AgentsNative-test7.md`.
- **2026-11-11 — Login terminal and shared storage require explicit wiring.**
  Displaying `TerminalView` alone leaves Codex device login unstarted. Use
  `terminal-open` when `authRequired` is set. Keep login errors visible
  during preparation. Android storage permission leaves PRoot's `/sdcard`
  bind configuration unchanged. Check the Java and host dispatcher launch
  paths. A new bind takes effect on a new process. The historical report is
  `docs/fork/VALIDATION-AgentsNative-test3.md`.
- **2026-11-11 — Preserve the old chat identity on preparation failure.**
  `create()` changed socket and thread IDs before process registration. If
  preparation failed, the new marker was absent and the Codex UI could
  select the finished terminal. Restore captured IDs only while the original
  handle is still in place. Preserve any registered replacement process.
  The cause of the phone's `PRIVATE_STORAGE` error remained open. The
  historical report is `docs/fork/VALIDATION-AgentsNative-test4.md`.
- **2026-11-11 — A template opened after a failed read has no write revision.**
  The test4 phone report confirmed an NPE in `Editor.write` when
  `document=null` after `PRIVATE_STORAGE_DIRECTORY`. Require a path and
  revision before writing, keep the template view available and expose an
  explicit retry after a successful read. Exercise the failed-read case as
  well as successful writes. During `.codex-control` preparation, set
  mode 0700 directly on directories owned by the app UID and avoid
  recursive permission changes.

  On the ARM64 fixture, Robolectric returned `S_IFDIR=262144` while
  Android/Linux used 16384. The real directory mode was 16832. Use an
  explicit Android/POSIX shim for syscall tests and preserve production
  validation. Historical evidence includes
  `docs/fork/VALIDATION-AgentsNative-test5.md` and
  `test5-directory-diagnostic.log`.
- **2026-11-10 — Inspect physical ZIP records after removing assets.** With
  the AGP 4.2.2 configuration used here, `sourceSets.main.assets.exclude`
  left an archive in the APK. After asset removal, incremental
  `packageRelease` left unregistered ZIP records between live entries.
  AgentsNative added `:app:clean` before packaging. Inspect gaps up to
  the signing block in addition to the central directory. Test6's AAB held
  a separate empty local header and a late manifest. Normalizing all
  entries while preserving their bytes and rechecking the JAR eliminated
  the `JarInputStream` warning. This repackaging would invalidate an APK
  v2 signature. Historical evidence includes
  `.verification/final-artifact/container-gaps.json` and
  `.verification/native-api36/bundle-normalization.json`.
- **2026-11-11 — Keep Activity lifetime independent of the last session.**
  The inherited `removeFinishedSession` called Activity finish when the
  process list became empty. After removing the base terminal, use an
  explicit transition to the start page with `attachSession(null)`.
  Returning from settings with no selected chat also opens the start page.
  Test9 separated screen visibility from `getCurrentSession` because a
  background chat can remain the selected process. Refresh must preserve
  the current screen, as must closing another process. Historical reports
  are `docs/fork/VALIDATION-AgentsNative-test8.md` and
  `docs/fork/VALIDATION-AgentsNative-test9.md`.
- **2026-11-11 — Preserve transparent menu styling and exercise the actual
  restart handler.** In test9, the shared window-styling helper painted the
  dialog decor black after the background became transparent. The sidebar
  keeps transparent decor and its existing scrim without recreating the
  chat. On initial CLI installation, initialize shared instructions
  independently for each agent and environment and record completion.
  Ordinary launches must respect a file the user has removed. Restart
  has a distinct `restartEndedSession` path. Its unconditional dialog
  survived in test10 because the test stubbed the entire restart handler.
  For UI checks, stub process creation and run the real completion handler,
  as in test11. The historical report is
  `docs/fork/VALIDATION-AgentsNative-test10.md`.
- **2026-11-11 — Inspect cgroup state during background diagnostics on
  nubia NX789J/API 36.** AgentsNative test11 observed
  `cgroup.freeze` and `frozen` at 1 when the device was locked while
  charging. The service remained foreground and DeviceIdle remained ACTIVE.
  On return, `frozen=0` and PRoot/Codex had the same process start ticks.
  SSH was recreated, and the user confirmed that a response arrived.
  Check the cgroup and process identity as well as foreground service status.
  The initiator of the freeze was undetermined, and the observation covers
  that cycle only. The historical report is
  `docs/fork/AGENTSNATIVE-BACKGROUND-DIAGNOSTICS.md`.
- **2026-11-11 — Diagnose floating-window resizing at the root layout.** On
  nubia/API 36/test11, the decor view stayed at 1,886 while the root
  alternated between 1,886 and 1,762, even on the main screen with the
  composer set to `GONE`. This made the input appear doubled without
  establishing the presence of two `EditText` instances. The legacy
  global-layout bottom-margin correction uses screen and window
  coordinates. Test12 disabled it in multi-window mode and reset the
  pending margin. On 2026-11-11 the user confirmed the floating-window
  fix. Other IME and split-screen combinations remained outside that
  acceptance. The historical report is
  `docs/fork/VALIDATION-AgentsNative-test12.md`.
- **2026-11-11 — AgentsNative scope changed and work moved.** The user
  requested replacement of all inherited code, including code beyond
  runtime dependencies and names, with work continuing in AgentsNative.
  The test12 package and its separate private signature were preserved in
  `Download/исходники/AgentsNative-test12-handoff`. The handoff directs
  work through `TASK.md`. Test12 predates the requested removal of all
  inherited code. Include PRoot in the provenance audit because its own
  `AGENTS.md` identifies its source as a fork for Termux. Independent
  FLOSS libraries may be retained after checking the provenance of the
  specific inputs being used.

## RTK and model context

- **2026-11-21 — RTK removal supersedes the integration decisions below.**
  The unreleased source removes app-managed hooks, dispatch, UI and asset
  installation. Older mode files, measurements and recovery data stay in
  Ubuntu AI. The earlier integration used launch-scoped hooks and paths, so
  new launches do not require edits to saved user configuration.
  [Historical storage and integration](docs/fork/RTK-MODE.md).

- **2026-11-07/08 — RTK naming, scope and readiness.** The UI labels are
  RTK Lite and RTK native (RU “RTK штатный”). Keep the persisted
  `basic` and `advanced` values. Lite covers a verified subset.
  Native follows stock RTK 1.1.0 OSS coverage and only reduces supported
  output. Each environment has one shared component. Chat mode,
  configuration, tracking and recall are per chat. Native activation
  requires a ready dispatcher or an agreed restart. The saved choice and
  live connection state are independent. The user's decision enabled
  full-output recovery and superseded earlier `RTK_RECALL=0` experiments.
  [Isolation and recovery report][rtkresearch], [Lite expansion][rtkexpand].
- **2026-11-07/08 — RTK permission results are specific to the tested modes.**
  In Codex 0.166.0 with full access and approval set to never, `2>&1`
  changed the AST and prefix matching. Removing it restored Git prefix
  checks for Lite pipelines, kept stderr separate and preserved pipefail.
  Stock hooks change the original prefix, so a forbid rule may stop
  matching. Full access with untrusted approval still prompted and honored
  decline. These checks used synthetic providers and apply to the tested
  versions and modes. Keep resolved hooks, hashes and keys scoped to
  their owners. Leave `bypass_hook_trust` disabled. The RTK research report
  records the isolation and permissions results. The historical raw
  `.verification/rtk-offline/pair-assertions.json` is absent from the source
  bundle. [Original matrices][notes], [native RTK research][rtkresearch].
- **2026-11-08 — Measure RTK against the output the original command would
  produce.** Stock `read --head-lines 45` used the full file as the
  baseline even though head and RTK each produced 2,160 bytes. Equal outputs
  represent zero additional savings. Jobs.1 measures existing buffers
  before filtering. Read/head/tail count the selected window, and Git diff
  excludes the separate stat decoration. Use the results from a single
  execution. Lite keeps its own captured-stdout measurement. A clean
  child environment excludes `JOBS_RTK_BYTES_FILE` and `JOBS_RTK_PID`
  and prevents a duplicate sidecar. [Jobs.1 and checks][rtkbytes].
- **2026-11-08 — Match RTK observations to the correct command and result.**
  The old `literal_words` lost quoted regexes and globs and shell sequences
  despite existing sidecars. The APK observer was fixed independently of
  jobs.1. Live Codex checks covered search at 120,480→14,612 bytes, head
  at 225→225 bytes, sequences and yielded results. Claude SDK checks used a
  synthetic provider. Downstream head changes the baseline in a pipeline,
  so avoid summing stage baselines. Leave the baseline unknown when output
  is truncated or the receipt remains unconfirmed. Final `PostToolUse`
  can arrive after polling. Measure recall from the delivered tool result.
  Claude removes the final newline, so
  apply that confirmed normalization on both sides. [2.1.7 fix][rtkfix],
  [original calculation][rtkstats].
- **2026-11-08 — RTK loader and diagnostic files.** Status refreshes the
  app-owned `rtk-mode.py` and can remove a temporary wrapper. Verify its
  hash before restoring it and preserve an unexpected helper for review.
  Observer-only APK changes can reuse the existing component revision.
  AAPT stripped `.gz` from a test `tar.gz` asset. Verify the filename
  and its bytes inside the final ZIP. The PRoot fixture did not get the
  expected protection from `O_NOFOLLOW`. Exclusive creation with
  `create_new`, rejection of existing paths and retention of the descriptor
  passed the name-replacement test. [Evidence][rtkbytes].
- **2026-11-03/05 — Codex 0.166.0 context and transport.** Provider ID affects
  resume. Under PRoot, `/proc/PID/exe` can point to the loader. Check heavy
  conversation continuation separately from DNS and short HTTPS probes.
  Warm WebSocket requests sent a small delta. Cold and reconnected requests
  sent accumulated images. The 32 MiB local history cap and the model request
  are different measurements. HTTP fallback in the 10–35 MB series leaves
  a universal limit and the cause of individual disconnects unestablished.
  Compact retained the checked requirements while `new_context` dropped
  them. New `view_image` calls accumulate data again. Overriding
  `supports_websockets` on the built-in `openai` provider failed to switch
  it to HTTPS. Relay drain timeout was confirmed for a separate case.
  The HezeAI report was published after the earlier failure.
  The historical content verification file
  `.verification/release-v2.1.5/upstream-report-verification.json` is absent
  from this checkout. The release validation retains the attachment size
  and SHA-256. [Measurements and limits][media], [historical notes][notes],
  [release verification][v215],
  [published report](https://github.com/openai/codex/issues/55730#issuecomment-6645086819).

[current]: CONTINUE.md
[rules]: AGENTS.md
[notes]: docs/fork/history/INVESTIGATION-NOTES-through-2026-11-09.md
[licenses]: docs/fork/LICENSE-REVIEW-2026-11-03.md
[testing]: docs/fork/TESTING.md
[composer]: docs/fork/VALIDATION-test40-composer-instructions.md
[repro]: docs/fork/REPRODUCIBILITY-v2.1.7.md
[ci]: docs/fork/CI-X11.md
[releasing]: docs/fork/RELEASING.md
[v215]: docs/fork/VALIDATION-v2.1.5.md
[v217]: docs/fork/VALIDATION-v2.1.7.md
[signer]: docs/fork/VALIDATION-release-signer-gate.md
[backuphistory]: docs/fork/VALIDATION-2.1.5-test5.md
[design]: docs/fork/DESIGN.md
[context2]: docs/fork/VALIDATION-2.1.4-test2.md
[context3]: docs/fork/VALIDATION-2.1.4-test3.md
[arrival]: docs/fork/VALIDATION-2.1.6-test5.md
[sidebar]: docs/fork/VALIDATION-2.1.6-test4.md
[history5]: docs/fork/VALIDATION-2.1.4-test5.md
[x11note]: docs/fork/VALIDATION-X11-NOTIFICATIONS.md
[instructions]: docs/fork/AGENT-INSTRUCTIONS.md
[skills]: docs/fork/AGENT-SKILLS.md
[permissions]: docs/fork/PERMISSIONS-COMPATIBILITY-TEST5.md
[backup]: docs/fork/BACKUP-RESTORE.md
[auth7]: docs/fork/VALIDATION-2.1.3-test7.md
[dictdevice]: docs/fork/DICTATION-DEVICE-2026-11-07.md
[claude]: docs/fork/CLAUDE-ANDROID-CHAT.md
[claudefixture]: docs/fork/CLAUDE-IMAGE-PHONE-FIXTURE.md
[env]: docs/fork/AGENT-ENVIRONMENTS.md
[preparation]: docs/fork/VALIDATION-preparation-structured-status.md
[env216]: docs/fork/VALIDATION-2.1.6-environments.md
[restart]: docs/fork/VALIDATION-2.1.7-test1.md
[dictoffline]: docs/fork/DICTATION-OFFLINE-2026-11-07.md
[rtkexpand]: docs/fork/VALIDATION-2.1.6-test2.md
[rtkbytes]: docs/fork/VALIDATION-2.1.6-test9.md
[rtkfix]: docs/fork/VALIDATION-2.1.7-test2.md
[rtkstats]: docs/fork/VALIDATION-2.1.6-test6.md
[media]: docs/fork/MEDIA-CONTEXT-INVESTIGATION.md
[test1]: docs/fork/VALIDATION-2.1.5-test1.md
[test2]: docs/fork/VALIDATION-2.1.5-test2.md
[test4]: docs/fork/VALIDATION-2.1.5-test4.md
[test5]: docs/fork/VALIDATION-2.1.5-test5.md
[test6]: docs/fork/VALIDATION-2.1.5-test6.md
[test7]: docs/fork/VALIDATION-2.1.5-test7.md
[test9]: docs/fork/VALIDATION-2.1.5-test9.md
[test10]: docs/fork/VALIDATION-2.1.5-test10.md
[test11]: docs/fork/VALIDATION-2.1.5-test11.md
[test13]: docs/fork/VALIDATION-2.1.5-test13.md
[test16]: docs/fork/VALIDATION-2.1.5-test16.md
[test18]: docs/fork/VALIDATION-2.1.5-test18.md
[test19]: docs/fork/VALIDATION-2.1.5-test19.md
[test20]: docs/fork/VALIDATION-2.1.5-test20.md
[test21]: docs/fork/VALIDATION-2.1.5-test21.md
[test22]: docs/fork/VALIDATION-2.1.5-test22.md
[test23]: docs/fork/VALIDATION-2.1.5-test23.md
[test24]: docs/fork/VALIDATION-2.1.5-test24.md
[rtkresearch]: docs/fork/RTK-NATIVE-RESEARCH.md
[devkittimeline]: docs/fork/VALIDATION-devkit-timeline-2026-11-20.md


2026-11-21 · Codex CLI 0.171.0, per-thread context. The app-server accepts
`model_context_window` and `model_auto_compact_token_limit` in the config map
of `thread/start` and `thread/resume`. A real CLI with a loopback provider
reported 171,000 tokens with 180,000 configured, 258,400 without overrides,
and 190,000 after resuming with 200,000 configured. The global config was
unchanged. Use `thread/tokenUsage/updated` for the displayed effective window.
This check used an isolated DevKit process on Android ARM64. Live account and
Ubuntu AI behavior remain separate checks. [Handoff](CONTINUE.md).
