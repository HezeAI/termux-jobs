# Local test release 2.1.3-test3

2026-11-03. VersionCode 1479. Local test APK; no push, installation or publication.
The developer's test1 acceptance remains historical and does not accept test3.

## Changes and boundaries

The existing serialized preparation job now builds a milestone plan from the
initial inventory: check, Termux prerequisites when missing, image and extraction
when Ubuntu AI is missing, missing agent components, and final verification.
Known-ready work is excluded. The count denotes completed stages, not estimated
remaining time. Only a successful final inventory fills the bar. An inventory
failure keeps it incomplete. Stages already satisfied by another queued operation
can be skipped by the existing host checks; the plan then advances past them.

Details show both the overall count/bar and the current component/stage. Download
bytes determine the displayed percent; the current-stage bar is indeterminate
otherwise. A settings card observes the selected agent's existing job through
main-thread notifications, including queued, failed and ready states. It unsubscribes
on detach/close. Clicking/reopening details joins that same job; explicit Retry
creates a new operation after termination. The first-launch consent explicitly
starts/reuses preparation. Window closure does not cancel work. Gemini API/Vertex
retain distinct identities within one shared queue. No recipes, component pins,
network policy, process collection or existing-environment replacement rules change.
No raw stdout/stderr retention, export or expandable journal returns.

The opaque horizontal star / paperclip group derives its fixed height from the
single-line field's font metrics, padding and minimum height. It remains at the
field's bottom as multiline text grows. The draft, caret, attachments, two ripple
targets, Send position and remaining panel transparency are retained.

Only action/command timeline-group headers receive a right-hand HH:mm clock, in
the device timezone. The title wraps beside a separate clock cell. The first
step supplies the group's event time. Unknown times stay hidden; user messages,
answers and attachments receive no new clocks. Claude native message timestamps
and OpenCode tool start times are projected when present. Live Codex/Claude tool
notifications and newly observed Gemini tools during an active response after a
ready baseline retain immutable observation times in the existing private timing
metadata store. Snapshot/opening time never timestamps an old event. Gemini old
or idle-loaded records without metadata remain unknown. Native history files are
not modified. Existing completion captions retain their separate semantics.

## Checked anew

- **75 targeted JVM/UI-model tests**, 0 failures/errors/skips: five preparation
  profiles (Codex, Claude Code, Gemini API, Gemini Vertex, OpenCode), clean/partial/
  ready inventory plans, final-check failure and explicit retry, measured bytes,
  current-stage indeterminate progress, one queue, selected-agent switching,
  close/reopen, listener cleanup and the actual settings-card connection.
- The same set checks narrow/large-font fixed composer height against a real
  single-line EditText measurement, multiline bottom alignment, preserved draft/
  caret and independent actions. Clock tests check known/unknown first events,
  stable card identity/clock across output updates, device timezone changes and
  non-overlapping header cells at widths 240/280/360 with enlarged text.
- **61 additional chat regression JVM tests**, 0 failures/errors/skips: shared
  chat UI, scaling, narrow layout, history and Gemini scroll behavior. These are
  Android models, not physical keyboard/gesture checks.
- **35 real artificial-child pipeline cases** across all five profiles use the
  production prepare/drain/Job code with fake pkg/proot/guest tools. They cover
  success, output/error/cancel/retry and preserve old fixture logs. Synthetic
  multiline stdout/stderr secrets are absent from retained state/files. No real
  package/rootfs/agent installation is performed by these fixtures.
- Node adapter suites for Codex, Claude, Gemini and OpenCode, completion timing,
  plus the new four-agent action-clock test. These cover unknown history, received
  event stability, refresh/restart and session isolation using artificial data.
- Localization: 584 workspace strings and settings/format arguments. JavaScript
  syntax and diff whitespace checks passed.
- Full modern Lint 7.4.2, all five modules: **0 errors, 236 warnings**; no report
  infrastructure errors. Includes existing compile SDK/toolchain limitations.
- Source review: typed/limited UI state, safe failure mapping, main-thread observer
  lifecycle, copied session context, no process/shell access in the card, no chat
  clock assigned during history rendering, no native-history writes.

The standalone Java stub initially lacked MUTED after the dialog gained a secondary
label; that test compilation failure is retained. The stub was corrected and all
35 cases rerun. An initial successful APK candidate preceded the final Gemini
idle-history guard and was not delivered. Final packaging includes that guard.
Old D8 Kotlin metadata diagnostics remain in the successful build log; final DEX
inventory is independently checked below. No skipped test is counted as successful.

Evidence: `.verification/preparation-plan-test3/` and
`.verification/release-2.1.3-test3/` in the active checkout.

## Reused

All 95 recorded native source/config hashes match the v2.1.2/test1 build identity;
compile/link flags are unchanged. Three primary native libraries and the X11 asset
payloads were reused byte-for-byte. No native compilation or GPU execution is
claimed. Java/resources/script assets were repackaged. The existing SDK, Gradle
cache and pinned runtime input cache were reused, not clean-room downloads.
The previous full 1608-test run is not claimed as fresh validation of test3.

## Final APK

- Package `com.termux`; versionName `2.1.3-test3`; versionCode **1479**.
- **40,459,633 bytes**, ARM64 only, non-debuggable, APK v2 signature verified.
- File SHA-256: `779f22d693f519f1d17be7d22d25718f40b738bdf08c5f890bce1dfba5b96d6b`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP integrity and absence of stale local ZIP headers passed. All seven ELF
  payloads are ARM64. All 120 assets match current source/pinned runtime bytes.
  558 selected production Java/XML/script/document inputs match the packaging
  stage; native input identity is verified separately above.
- DEX inventory: **14,187 definitions**, 31 reviewed optional unresolved types;
  required runtime definitions present. No removed voice/diagnostic code,
  microphone permission or unintended key/cache/temp files included.
- `:x11:verifyX11Inputs` and `:app:verifyReleaseSigner` ran and passed in final
  release packaging. The existing pinned certificate matched; no key generated.
- Package/signer match v2.1.2/test1/test2 and versionCode increases. No reset or
  schema migration is introduced. Actual update/data preservation needs a phone.
- Published v2.1.2 APK/tag, Components and stable update channel remain unchanged.

## Not checked / phone plan

No real Termux/pkg/APT/rootfs/CLI installation or new phone acceptance. Working
environments/sessions remain untouched. Fixtures do not prove a clean install,
real mobile transfer, authentication or model availability. No independent audit.

1. For each of the five profiles, check the environment card from session settings
   and first-launch consent. Close/reopen details, repeat taps and switch agents.
   Confirm one operation and correct selected-agent queued/stage/error/ready state.
2. In a separate clean test install, check prerequisites, measured image bytes,
   extraction, missing components and final check. Existing test Ubuntu AI must
   not download or reinstall the image. Fail/retry only a test-owned operation.
3. Confirm that the overall bar fills after final success and never implies time
   remaining. Check compact card updates while the details window is closed.
4. With keyboard, larger font and multiline draft, check fixed bottom-aligned
   star / clip height, separate touch feedback, draft/attachments and scroll position.
5. Check action-group HH:mm in the phone timezone during a live tool action,
   refresh, history paging, chat switching and restart. Unknown old events have
   no invented time; message/answer/attachment presentation remains unchanged.
