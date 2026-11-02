# Private test18 — agent indicator and request summaries

Date: 2026-11-02. Version `2.1.1-test18` / `1452`, package `com.termux`.
Active checkout: `/root/termux-fork/.verification/rollback-live-actions/source`.
Baseline: local `cf6d0ed28a680d6c13c56215277d0d4694e26db7` plus this task's
modified/new files. Evidence: `.verification/agent-indicator-test18/`.
This is a targeted private build, not a whole-application audit or public release.

## Final behavior

Four monochrome glyphs replace the heart: Codex `>_`, Claude rays, Gemini sparkle,
pixel OC. The fixed44dp hit target and request/session-scoped Stop confirmation
are retained. Agent movement is entirely inside the circular indicator: short
orbit trail, six asymmetric inner contacts, contact flashes and continuous return.
User-wait pulses calmly; native retry has an amber dashed orbit; idle is muted;
error uses a red contour. The robot terminal button no longer has a second moving
agent border. Server transfer indicators and ordinary terminal/SSH activity stay.

Only the existing Choreographer/60Hz cadence is used. Geometry is cached by size;
frames do not serialize state, start processes or rebuild layout. System
animation-off leaves a distinguishable static glyph/outline/arc. Native model retry
banner, reported counters/reasons and timing/retry policy are unchanged. Local CLI
availability and message admission are not model-connectivity proofs. Native
questions, approvals, terminal actions and errors remain distinct.

One summary appears below the final answer of one completed request, for example
`Работал 16 с · 05:03` or `Worked for 16s · 5:03 AM`. App RU/EN and the device's
12/24-hour preference/timezone apply; the examples do not force one clock format.
A summary may wrap on a narrow/large-font display. Tap it for full date/time and
an explanation that elapsed request time includes waits and is not CPU usage.
No timer is started by opening history. Missing start/duration means time only;
missing reliable completion data means no invented summary. Stream updates retain
native message keys and cannot create an additional summary for the same request.

## Timing contracts

- **Codex0.166.0:** generated local official app-server schema confirms
  `Turn.startedAt` and `Turn.completedAt` are Unix seconds; `Turn.durationMs` is
  milliseconds between turn start/completion. Use them on live completed turns
  and native historical completed turns. Prefer explicit `final_answer`; legacy
  unknown-phase turns use their last assistant answer after confirmed completion.
  Commentary and `partial_answer` are not final answers. If timing fields are
  absent, a matched observed `turn/started` → `turn/completed` provides elapsed time.
- **Claude2.1.296:** native successful result finalizes the last complete assistant
  answer. Matched command-lifecycle start/result provides observed elapsed time;
  completion time is the persisted local receipt time. Assistant creation/JSONL
  `timestamp` and API-call durations are not substituted. The official
  [SDK result contract](https://code.claude.com/docs/en/agent-sdk/typescript#sdkresultmessage)
  exposes duration fields, but this build conservatively uses matched observations
  rather than assume their scope across a long-lived stream. Terminal-only old
  history without a recorded summary remains unlabelled.
- **Gemini:** matched committed native responding → idle lifecycle and committed
  final history entry; handle idle arriving before the final React commit. An
  observed request epoch is separate from individual API/tool calls and pending
  input. Restored old entries alone cannot acquire a new time. Confirmed chat
  cancellation does not promote a partial answer into a successful summary.
- **OpenCode:** native execution start/success events with the final text message;
  interrupted/failed executions do not fabricate a final summary. Cached native
  schema confirms assistant `time.created` is separate from `time.completed`;
  only final `finish: stop/length` with completion data can label old native history.
  Provider `time.streamed` and message creation are not request duration.

`agent-completion-times.cjs` stores small immutable per-request summary records in
an append-only per-agent/conversation journal inside the existing private control
mount's `completion-times/`. Control mounts are backed by application files, not
an ephemeral host `/tmp` directory. No native chat history is rewritten. Records
load once per adapter and are looked up in memory during projection; one journal
append/fsync happens at completion. A damaged trailing record is ignored without
losing earlier valid records. Unreadable/nonregular/oversized journals fail closed
for additional timing metadata, without changing message execution; the reader
safety bound is32MiB per journal. There is no automatic cleanup or data migration.
Existing backup/export formats do not include this optional presentation journal.

The four normal launch installers deploy the added module. Already running
adapters retain loaded code until an explicit normal restart with conversation
continuation. This task does not restart working chats or change authorization.
The archived helper stays excluded; user-facing rootfs migration stays removed.
Settings/schema8 and ordinary native histories are retained. Reserve1451 is below
this1452 build and is not an ordinary higher-version rollback over it; a later
reserve would need an explicitly prepared higher version. An APK never restores
previous user data by itself.

## Checked anew

The final scope contains135 distinct JVM cases across the broad and corrected targeted runs. The broad run had134 cases with two fixture-path failures; the corrected launcher/timing run passed, and the final positive-dimension narrow-caption run passed. Raw failed logs remain separate from the successful reruns.

- Four-adapter isolated timing fixtures: final-only placement, no streaming/old
  creation-time substitution, native Codex duration, unknown start, repeated
  projection/reopen, persistence, late Gemini commit, truncated journal tail and
  agent/conversation isolation.
- Existing Codex, Claude, Gemini and OpenCode adapter regressions; real Codex and
  Claude CLIs against private synthetic loopback providers, including actual
  timing summaries. These are protocol fixtures, not authenticated cloud inference.
- Scoped-stop fixtures block real process/signal APIs and retain all four native
  refusal guards. Existing question/confirmation UI tests include Yes-left/No-right,
  chat switching and stale completion. Native delivery/retry and pagination checks
  retain their earlier contract and counters, including no invented recovery on ACK.
- JVM/UI scope: four glyphs/five states, orbit geometry and six contacts at several
  radii, closed-contour seam, static reduced-motion mode, hit dimensions, frame
  cadence, timing RU/EN/12-24 preference and missing fields; chat/view/navigation,
  narrow/large-font/reduced-height question/stop models and added asset installers.
- Source identity confirms unchanged terminal-line shader, frame cadence and
  existing stop confirmation/scope. Modern Lint uses all five modules; source,
  resource, locale and runtime composition checks are separate from phone acceptance.

Intermediate failures are retained in task logs: a JSON test initially lacked
Robolectric; a raw stage Gradle copy restored NDK configuration; one combined
Gradle command attached test filters to assemble; long temporary paths hit an
existing socket-path guard. These fixture/command issues were corrected without
weakening production guards or installing an NDK. No failed run is a successful test.

## Reused

The native/X11 source inventory, existing Termux JNI binaries and bootstrap archive
match the preceding verified inputs by SHA-256 (`native-reuse.json`). They are
reused prebuilt payloads, not a fresh native compilation or graphics execution.
Previously compiled caches are retained. Final APK inspection remains mandatory.

## Not checked on a phone

Actual glyph readability, flashes, orbit/bounce fluidity, touch/scroll, TalkBack,
real keyboard and all device font/scale combinations need developer acceptance.
The reduced-height model does not claim to open an actual Android IME. Real-provider
network failures and the other phone's earlier transport problem are not fixed or
proven by these fixtures. No real desktop, environment install/update, driver
operation, user-data cleanup or working-session restart was run.

## Final artifact and gates

- Download: `/sdcard/Download/Termux-Jobs-test18.apk`.
- Version: `com.termux` / `2.1.1-test18` / `1452`, non-debuggable, ARM64 only.
- Size: 39954359 bytes.
- SHA-256: `ecde761d2b94b09522684d3093769c4dc9c405d0fdadcebc81c81b6a796a708a`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`; APK v2 signature verifies.
- ZIP integrity passed; all1105 entries inspected. Three JNI libraries match the
  preceding test16 payloads byte-for-byte. Packaged changed assets match current
  sources; no helper class or unintended private-auth filename was found.
- DEX runtime inventory:14045 definitions; no required missing types and no
  unreviewed missing types. The31 reviewed optional types retain explicit notes.
  This inventory does not prove reflective/method compatibility on a phone.
- Full modern Lint7.4.2: five modules analysed, zero errors,234 warnings and no
  infrastructure diagnostics. Warnings remain visible; compileSdk35 is beyond
  the older APK AGP's tested range. APK packaging also emits the pre-existing
  Kotlin1.8 metadata vs older embedded-tool diagnostics; the build and modern
  analysis complete successfully. Toolchain limitations are not hidden.
- Localization consistency:584 workspace strings and settings format keys passed.
- ARM64 packaging/lint production inputs match the final source hashes. Final
  Download copy was read back and matches the verified APK SHA-256.

A final narrowed test invocation accidentally omitted the existing
`:app:downloadBootstraps` exclusion and downloaded other-ABI bootstrap inputs
inside the reused test stage. They were not installed or used in this ARM64 APK.
The extra inputs were retained; no cleanup or environment changes followed.

Compact final evidence: `artifact.json`, `delivery.json`, `apk-runtime.json`,
`signature.txt`, `badging.txt`, `native-timing-contract.json`, `native-reuse.json`,
`unchanged-mechanisms.json`, `final-summary.json` and the named test/Lint logs in
`.verification/agent-indicator-test18/`. APK installation and publication were not
performed; working chats and routes were not changed.
