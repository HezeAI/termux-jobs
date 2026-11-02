# test16: Codex connection state and adaptive instruction UI

Date: 2026-11-01. Target: `2.1.1-test16`, versionCode **1449**, package
`com.termux`, existing ARM64 signer. Active checkout:
`/root/termux-fork/.verification/rollback-live-actions/source`, HEAD
`58fbdb411e6a2bd8fbfd4d86700d3dcc05a7a3fe` plus the recorded dirty/new files.
This report distinguishes this source change from the delivered test15/1447.

## Observed network behavior

The actual Codex **0.164.0** app-server and production adapter were run with a
private CODEX_HOME, synthetic project and loopback Responses provider. The child
received an explicit environment whitelist; the fixture did not use cloud
credentials, user configuration, a shared proxy/tunnel, or a working chat. Only
its own provider response sockets were destroyed. Default native retry timing
and retry limits were retained. Cleanup targeted only fixture-owned children.

Before the fix:

- Short interruption: `turn/steer` acknowledged a clarification while the model
  stream was broken. Snapshot still reported local `connected=true`, `busy=true`,
  with no explanatory error row. Native CLI emitted `Reconnecting... 1/5` with
  `willRetry=true`. The adapter discarded that retry event.
- Restoring the loopback provider allowed the **same turn** to finish. A repeated
  admission ID returned the stored result and caused exactly one native steer
  RPC in total. Acknowledgement itself did not establish recovery or model uptake.
- Long interruption: native retries exhausted, followed by `willRetry=false` and
  a failed turn. `busy` became false. Restoring the provider did not restart the
  failed turn. An explicitly sent next request completed in the same CLI process.
- This did **not** reproduce an indefinitely busy turn after connectivity was
  restored. It demonstrates a missing explanation during retry, not the cause
  of the other device's reported hang. It does not test Hysteria/QUIC, remote
  routing, a real cloud model, or the other phone.

After the fix, repeated actual-CLI short/long interruption probes expose retry
state in the snapshot, clear it on resumed model output/completed success or native interruption, and
retain the terminal error after failure. Native user `clientId` values associate
admission captions with the corresponding message, including identical texts.
The duplicate native final-error notification/completed-turn pair produces one
history error, rather than two identical entries. Retry progress itself is only
presentation state and is never appended as a history message.

Compact evidence (ignored; no SDK/rootfs copies):
`.verification/network-recovery-test16/`. Raw native events, RPC calls, synthetic
provider requests and timestamped snapshots are retained in the fixture paths
recorded by `packaged-short.log`, `packaged-exhausted.log` and `final-cancel.log`. Binary SHA, source
baseline, font measurements and verification inputs are recorded separately.

## Implementation and boundaries

- A chat/request-scoped top banner reports native retry number/limit, when
  supplied. Missing limits remain missing. Tap opens native reason/details and
  the request identifier. Local CLI readiness remains a distinct existing field;
  a living process or successful steer does not establish model connectivity.
- Only reported retry events start this banner. Normal tool execution, questions
  and approval waits do not imply reconnecting. Model output/reasoning or a
  successful native turn completes recovery; a local RPC failure makes model
  state unknown. The presentation cache retains the last reported state per chat
  and fresh snapshots replace it. Polling frequency/network retry policy are unchanged.
- Successful **Codex message** admission moves from the persistent bottom notice
  to a small caption on the native user message or its attachment. Tap explains
  that CLI admission is not model uptake. Mapping uses native `clientId`, never
  message text or the latest bubble. Rejection/unconfirmed notices and drafts
  remain visible. Shell notices and other agents' delivery protocols are unchanged.
  These captions are live adapter presentation, not a new persisted history schema;
  an adapter restart can discard old live admission metadata.
- Instruction tabs had three equal weighted columns and 16 dp padding on each
  side. At a modeled 280 dp viewport the text area was about 50 dp, while the
  local Android Roboto measurement of «Диагностика» at 15sp was about 92 dp,
  rising to 137 dp at fontScale1.5. This explains forced mid-word wrapping.
- Tabs and AGENTS file choices now use natural single-line widths in horizontal
  scroll containers. Selection styling/accessibility and the independent actual
  instruction-source label remain. Project explanations/editor scroll centrally;
  save/menu/close controls remain outside that area. Editor text retains its own
  bounded multiline viewport. Long status text can scroll within a bounded area.
- Preview language buttons keep natural widths. Paths/warnings scroll with the
  preview; RU/EN switches and apply/cancel controls remain available. No global
  font reduction, automatic translation, instruction overwrite, or configuration
  migration was introduced.

## Verification status

**Checked anew**

- Native Codex short/long fault probes, retry/restore/final refusal/explicit next
  request, native IDs and same-ID steer deduplication, as described above.
- Native interruption during retry: the interrupted request becomes non-busy,
  the retry banner clears, and an explicit next request works after restoration.
- Adapter events: explicit/unknown retry counts, stale turn events, steer ACK
  without recovery, response recovery, terminal failure deduplication, a normal
  waiting tool, local RPC failure and two separate controller instances.
- Actual Codex ordinary-mode questions: options/free text/multiple questions,
  original reply channel and same-turn continuation; no silent Plan switch.
- Affected JVM/UI behavior and model layouts: per-chat reconnect cache, caption
  ownership, duplicate text, failure drafts, stop/request races and question reply
  channel; narrow widths 280/320/360 dp, system fontScale1/1.3/1.5, RU/EN,
  reduced 360 dp available height, and chat scale100/150 for question/stop layouts.
  Final counts and static/build/artifact results are recorded below after execution.
- Real local Android font metrics were measured separately with Pillow/FreeType.
  Robolectric's legacy text model did not reproduce the old glyph wrapping: its
  initial regression assertion failed and is retained in the first-run evidence.
  The fixture now checks allocation/one-line/scroll/control bounds; it is not
  represented as a real-phone glyph or keyboard test.

**Reused**

- Existing checkout, local SDK/JDK/Gradle caches and JNI/bootstrap payloads. Source
  and artifact identities are compared by `check-native-reuse.py`, not inferred
  from an old PASS log. No new native graphics execution is claimed.
- Unaffected environment, driver/component, instruction backend persistence and
  account/connection-launch mechanisms were not reinstalled or reconfigured.
  Their changed-input inventories are retained; the APK gate still inspects the
  final packaged assets/native/runtime rather than trusting the previous APK.

**Not checked / phone acceptance**

- The hanging chat and tunnel on the other phone. The supplied process screenshot
  establishes only process presence; ordinary terminal PATH lacks `codex` and
  `ss` is unavailable there. It does not establish model health or locate a fault.
- Real IME visibility/Back, OS display size, device fonts/glyph shaping, touch
  scrolling, TalkBack, caption taps and phone layout across orientation. Reduced
  height JVM fixtures are not an actual keyboard test.
- Real provider websocket/HTTP fallback and other versions' event contracts.
  Unknown states/counts are not guessed. No authenticated cloud outage was induced.
- Working desktops, package installations, updates and cleanup were outside this
  task. No working session, shared tunnel or the current phone's network was
  restarted or interrupted. No APK installation or public release is part of this task.

test15 phone feedback: the developer reported native questions working and the
agreed visual changes looking correct. That feedback does not cover the new
test16 layouts or the failure on the other device.

## Final verification record

- The task's full affected JVM run completed **111 tests, zero failures/errors**.
  A final selected-tab visibility change was followed by **four focused layout
  checks**, all passing; the entire instruction test class was then rechecked
  after packaging: **42 tests, zero failures/errors/skips**. These are repeated
  checks, not additional distinct tests. Reduced-height fixtures do not operate
  a real IME.
- Compatible modern Lint7.4.2 analyzed all five modules: **zero errors, 232
  warnings**, the same count as test15, with no rule-loading/infrastructure
  diagnostics. The existing compileSdk35/AGP support warning remains.
- Final helper syntax and RU/EN localization checks passed. Native retry,
  duplicate-send, cancellation and question probes retain exact source/binary
  identities in their fixture directories.
- Version1449 is higher than test15/1447 and the old reserve1448. The latter is
  still retained but is not a direct ordinary-update rollback over1449.
- The final artifact identity, signer, archive/ABI/runtime, exact packaged assets
  and Download readback are recorded in `artifact.json`, `runtime.json`, and
  `delivery.json` in the compact evidence directory.
- Final APK: `Termux-Jobs-test16.apk`, **39,955,680 bytes**, non-debug,
  `com.termux` / `2.1.1-test16` / `1449`, ARM64 only.
  SHA-256: `67fce840fde3d1c5b3f758ce5d5216f5b08cd1ca82d4c986c06fd0069d89f3bb`.
  Certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
  ZIP/signature and 99 source assets matched; DEX runtime audit found 14,051
  definitions and 31 explicitly reviewed optional unresolved types. All three
  native libraries match the previous verified payloads. The Java/resources/
  manifests in the APK build and Lint stages match final sources; their counts
  are in `final-stage-identity.json`.
- Final-source native probes: short outage recovered in the same turn (6,498ms
  fixture duration), exhaustion ended the turn and required an explicit next
  request (30,900ms), and interruption cancelled only its own turn (7,794ms).
  These are fixture timings, not measurements of production network recovery.
  All three retained one native steer RPC despite a repeated admission ID.
