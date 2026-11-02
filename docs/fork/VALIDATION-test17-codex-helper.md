> Historical private test17 experiment. Development was stopped on 2026-11-02.
> The helper is excluded from the next public release. This document describes
> the archived experiment, not the active source or a future APK.
> See [separation and preserved fixes](HELPER-EXPERIMENT-PAUSED.md).

# Private test17 — Codex helper, migration UI removal and heart route

Date: 2026-11-02. Active checkout:
`/root/termux-fork/.verification/rollback-live-actions/source`, Git HEAD
`58fbdb411e6a2bd8fbfd4d86700d3dcc05a7a3fe` plus the existing modified/untracked
files and this task's changes. Evidence is in `.verification/codex-helper-test17/`.
This is a private test build, not a public release or a whole-application audit.

## Reserve prepared before implementation

The installed APK was read through `pm path com.termux` and verified as
`com.termux / 2.1.1-test16 / 1449`, SHA-256
`67fce840fde3d1c5b3f758ce5d5216f5b08cd1ca82d4c986c06fd0069d89f3bb`.
The reserve is `Termux-Jobs-test16-reserve-1451.apk`, versionName unchanged,
manifest versionCode1451, SHA-256
`7456bc269d180acf4857fdba43a2326842ec45c19e852654cd74648011f67bdd`.
Package/signing identity is retained; only versionCode and APK signing/container
metadata changed. All1103 other ZIP entries, including DEX/resources/assets/JNI,
are byte-identical. Restoring the old versionCode makes the binary manifests equal.
Internal BuildConfig stays1449. Reserve runtime/ZIP/signature/ABI checks passed.

Test17 uses1450, below reserve1451; every subsequent build must account for both
and use1452 or greater. The reserve was copied/read back in Download before any
helper/migration-removal changes. It does not restore earlier user data.

## Implemented paths and boundaries

- `CodexHelper` is an asynchronous atomic sidecar and a small FIFO coordinator over
  existing `CodexChatControl`, not another model/chat engine. It stores explicit
  links, run/thread/message/admission/turn IDs and actual delivery outcomes.
- `CodexHelperUi` is reached from Codex session/preset settings and the structured
  Codex transcript. New helpers clone a saved preset into an ordinary separate
  saved session. Existing helpers require explicit selection, preserve the native
  ID/history, reuse a running channel and refuse a stopped profile with no saved
  thread. Existing history selection uses the normal catalog/metadata verification.
- `WorkspaceController.prepareHelper` returns the existing live session before
  considering launch. Launch checks other active/unknown owners, and a per-profile
  start guard covers asynchronous profile save/create. Preparation, privileges,
  connection launcher and native resume use the existing mechanisms.
- Native conversation ownership is checked by environment plus actual thread ID,
  not merely by profile ID; two presets referencing one conversation cannot claim
  two links. Link epochs reject delayed preparation/context/admission callbacks
  after disable/re-enable. Unchanged link saves retain the pending work/scope;
  explicit selections and helper-only upgrade invalidate prior work.
- The compact panel reuses `AgentChatView`, native pagination, presentation scale
  and control requests. Minimize/Back keep history and drafts; personal sends clear
  only on admission. Scoped Stop uses the existing native interrupt route. Questions,
  permissions, sign-in and native-terminal handoff require opening the ordinary
  helper session; the compact window never grants them automatically.
- Review context is bounded and frozen for its original worker. Native task metadata
  provides a preview explicitly marked potentially incomplete, avoiding a full
  early turn with large images. Recent clarifications/results and selected text are
  included within UTF-8 budgets; files/log directories are not swept automatically.
- `codex-chat.cjs` enforces idle-only helper review admission, reports its turn ID,
  and exposes an exact-admission/turn result only after successful completion with
  a final-answer phase. Commentary, incomplete, failed/interrupted/mixed turns are
  not automatically forwarded. Other personal input cannot steer into an owned
  helper review through this adapter.
- Final feedback uses ordinary confirmed native start/steer delivery with an
  explicit advisory envelope. It is shown as Helper with its source even when the
  worker retains an older adapter. CLI admission is not proof of model uptake.
  Repeat unconfirmed clicks say that nothing was resent, not that a new request
  was queued. A worker reply never starts another helper request.
- Disabling invalidates pending work and blocks subsequent unsent forwarding;
  already-submitted delivery cannot be undone. No history is deleted. Application
  process restoration cancels/marks unknown pending records rather than replaying.
- Old adapters report an explicit capability limitation. A user-chosen helper-only
  restart resumes its same ID/history through the ordinary owned-session restart;
  the worker is not restarted and old reviews are not replayed.

## Removal and retained shared functionality

The migration button and clone-only Java/Python preparation/copy code were removed.
Normal Ubuntu/Ubuntu AI selection, missing-root preparation, package install,
resume/import/backup and shared environment deletion remain. The removal scan,
native locking, no-follow quarantine and source-change/refusal paths remain tested.
Prerequisite preparation for shared deletion is named `agent-operation-tools`.
There is no new environment install path or application-data migration.
Current instructions/links were updated; old release/validation records remain
historical, with no current migration entry point. No user's cached files, rootfs,
profiles, conversations or authorization were removed.

## Heart trajectory

`WorkspaceActivityLine.HeartRoute` builds one deterministic path using six
asymmetric contacts on the inner circle. Incoming motion enters without a separate
center target. The last contact connects to the existing outer circle, then the
chat edge, without a new contour or restarting the frame clock. Contact distances
strengthen the existing pulse. Geometry is cached per layout and scales with the
actual button radius. Stop/confirmation/ownership semantics are unchanged.
Robolectric legacy graphics lacks `PathMeasure.getPosTan`; geometry checks use
`Path.approximate` interpolation, and do not claim native phone rendering.

## Checked anew

The final summary files record actual commands, selected tests, failures/earlier
iterations, identities and final successful results. Do not add counts from repeated
runs as if they were distinct tests.

- Final JVM scope: coordinator, compact UI, environment menus, navigation, heart
  geometry/pulse/cadence/static mode. This includes new/existing helper selection,
  preset permission preservation, running-session reuse via a fake Host, no empty
  history fallback, sign-in refusal, minimize/draft preservation, RU/EN settings,
  exact ownership, FIFO/different IDs, duplicate/unconfirmed delivery, retained final
  answer, failed-result refusal, app restoration, disable/re-enable, stale epochs
  and dispatch-boundary cancellation. These are isolated fixtures, not phone GUI.
- Python:40 real isolated filesystem/locking/removal and fake-install prerequisite
  tests passed. They preserve external symlink targets and independent systems;
  no real installed environment was deleted or installed.
- Actual Codex CLI0.166.0 with private synthetic local provider: personal exchange
  and review share one history; exact final turn result; active worker steer and
  continuation; same-ID deduplication; helper cancellation leaves worker active;
  cancelled final is unavailable; helper-only restart retains ID/earlier history,
  refuses old admissions and confirms a fresh review. The old adapter asset was
  taken from the frozen test16 APK. No working profiles/account auth/cloud route
  were used. CLI executable and fixture/packaged-adapter hashes are recorded.
- Fake-RPC adapter regressions, all-four-agent scoped-stop and message-delivery
  checks passed without signalling real processes in those fake-process checks. Existing Shell
  ambiguity/deduplication remains unchanged.
- RU/EN resource consistency, Java compilation, code/lifecycle review and modern
  Lint all five modules. Latest Lint has0 errors and233 warnings; the new
  SetTextI18n warning is the localized helper-name prefix plus a user-provided name,
  reviewed as cosmetic. No Lint infrastructure/rule-loading failure was accepted.
- Final-file ZIP/package/version/ABI/debug/signature/certificate/SHA-256, required
  DEX definitions, packaged asset equality and narrow secret-pattern inventory.
  The exact DEX definition and reviewed optional-type counts are recorded in the final runtime inventory.

Earlier broad JVM runs of204 cases passed during development; they are not a claim
that the whole app or every phone behavior was retested for this APK. Earlier failed
logs are retained; missing imports/syntax, outdated async-form assertions, explicit
locale setup and the synthetic cancellation provider were corrected before final
verification. No failed check is counted as PASS.

## Reused with identity proof

- Workspace/session/preset serialization, persistence and Codex launcher sources
  match test16 exactly. Workspace stays schema8; helper state is a separate file.
- Packaged JNI/X11 libraries are byte-identical to the installed test16 payload.
  Native/X11 sources, cached bootstrap and prebuilt identities are checked in
  `native-reuse.json`, with final APK payload comparison. No new native compilation
  or device graphics measurement is claimed.
- Existing repository-local SDK/Gradle/JDK/caches and existing source stages were
  used. There are no full new project, SDK, rootfs or environment copies. Compact
  old reports and unpublished intermediate APK candidates were preserved.

## Not checked / device acceptance

- Real overlay size/keyboard/Back/outside behavior, long labels/large system font,
  helper stop confirmation and touch/accessibility; real visual heartbeat, contrast,
  entry/exit and bounce smoothness on different icon sizes. Model checks do not
  establish actual device FPS.
- Live cloud models, long-stream relay failures on the other phone, real sign-in,
  every CLI version (the cached bootstrap0.163 binary was unavailable), Android
  process killing and actual update/reserve installation. Unsupported exact-final
  results fail visibly rather than forwarding an unverified reply.
- No X11 desktop/driver/environment installation, graphics benchmark, live backup
  restore/cleanup or public publication was performed. Relevant graphics inputs are
  unchanged; UI helpers only mount in structured Codex chat/dialog surfaces.

## Data compatibility and limits

There is no reset or incompatible migration. Reserve test16 ignores
`files/codex-helper-v1.json`; ordinary saved helper sessions and native histories
remain standard. Old versions can show the advisory envelope as ordinary user
text. Existing backup/export formats do not include the helper-link ledger.
The ledger admits at most256 review records and does not auto-delete history.
Unknown admissions are not blindly retried; inspect helper history and use explicit
personal conversation/manual forwarding when recovery is necessary. Re-enabling
or restoring the app never sends old reviews automatically.

## Final artifact

Final file: `/sdcard/Download/Termux-Jobs-test17.apk` — **2.1.1-test17 / 1450**,
**39,981,972 bytes**, package `com.termux`, ARM64 only, non-debug.

SHA-256: `6e3a26ae50f69e774a2e9e7abba6d0041a0cbe2e988e6e273803b0426f0496fd`.

Certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

The delivered copy passed independent ZIP, package/version and signing verification
and SHA-256 read-back. Final runtime inventory:14143 definitions,31 explicitly
reviewed optional unresolved types; all99 source assets and the three unchanged JNI
payloads match. Final selected JVM run: **72 tests,0 failures/errors**. Python:
**40 tests**. Modern Lint all five modules: **0 errors,233 warnings**. The report
records fixture/phone limits rather than claiming device acceptance.

See `artifact.json`, `delivery.json`, `final-summary.json`, `dispatch-jvm-summary.json`,
`delivery-lint-summary.json`, `native-cancel-result.json`, `native-reuse.json` and
reserve records in `.verification/codex-helper-test17/`.
