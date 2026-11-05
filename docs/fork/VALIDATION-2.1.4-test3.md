# 2.1.4-test3 / 1488 — private release APK validation

2026-11-05. Checkout `/root/termux-fork/.verification/rollback-live-actions/source`,
branch `work/claude-gemini-context`. Local implementation commits:
context `4cd22229f146161d43ed1d3bb64a41f3f71a8d59`,
notes `af26c94964dd52eef7ac97b4bce136227094a609`. Last published stable baseline: v2.1.3 / 1485
(`308c88955fd3b0691ded560c78ee3f945538381e`). Private test1/test2 remain unchanged.
This report separates synthetic native CLI checks, Android UI fixtures and pending
phone acceptance. No APK installation, working-session restart, push or publication.

## Included changes / Состав

Claude and Gemini now share a passive context presenter at the bottom of their
existing expanded status dialogs. Collapsed chips, buttons and scrolling retain
their behavior. There are no new model queries, polling loops or compaction
buttons. Existing local status refresh delivers the additional scalar fields.

Known used/window: bar, rounded percentage and used/available tokens. Known used,
unknown window: estimate only, with no percentage/bar. Missing/invalid usage:
«Нет данных» / “No data”. Real zero is accepted only from a valid native counter.
The model is named explicitly. Cached/reopened/pending estimates are stale;
model changes invalidate mismatched estimates. A new CLI event refreshes the value.
Cache is never subtracted from context. Subscription limits and megabytes remain
separate. No automatic-compaction threshold is inferred.

The star in all four structured chats now opens **Notes** directly, without
Commands/Alias/Notes tabs. It reuses the existing picker, search, full preview,
edit/insert/confirmed-delete actions and note editor. “Save input” opens a new
note editor with the composer text; only explicit Save persists it. Cancel after
editing that form creates no note, changes no library bytes and retains the
original composer text. Existing command/alias data and the main library/ordinary
terminal routes remain available. No profile/history schema migration occurs.

All test2 features remain: file modification timestamps in both windows, exact
four supplied common RU/EN instruction templates, Codex context/manual compaction,
and one continuous user-request timer in all four chats. Saved user instructions
are not migrated automatically. See [test2 scope](VALIDATION-2.1.4-test2.md).

## Native field meanings / Источники

| Agent/version | Used context | Capacity | Deliberately excluded |
| --- | --- | --- | --- |
| Claude Code 2.1.296 | latest root SDK assistant usage: `input_tokens + cache_creation_input_tokens + cache_read_input_tokens`; the native status line uses this same sum | `result.modelUsage[exactActualModel].contextWindow`, capacity metadata only | output tokens, cumulative result usage/model costs, replay and subagent usage |
| Gemini CLI 0.64.0 | raw `usageMetadata.promptTokenCount` for `role === main`, actual request model | `tokenLimit(actualModel)` only for an explicit native switch entry, using current runtime model constants | total session spend, utility router/summarizer usage, normalized zero when raw usage is absent, tokenLimit's generic fallback |

Claude's pinned native status code (`dHn`, `Y1e`) establishes the cache-inclusive
formula. Its native `dm(model, Xf())` supplies contextWindow metadata using effective
settings/catalog. The application does not hardcode 200,000 or 1,000,000.
The model map is process-local and cleared on worker/model changes.

Gemini's native `_logApiResponse` still has raw metadata before the telemetry
normalizer defaults missing counters to zero. The version/hash-checked loader
observes it at that point. CachedContentTokenCount is already a subset of prompt
usage. Actual Auto routing is observed on main calls, not inferred from “Auto”.
The native tokenLimit default is **not evidence** for unknown names. Example:
`gemini-2.5-flash` and routed `gemini-3.1-pro-preview` had no explicit matching
entry in these probes and displayed an estimate without a window. Runtime
`setFlashModels` can change known model constants; the comparison uses their
current values. Native catalog metadata is not an independent measurement of
provider/account entitlement or server acceptance of that many tokens.

After native compaction, Claude accepts valid SDK `compact_metadata.post_tokens`
when reported; `pre_tokens` is never substituted. Without a post count, the old
estimate remains stale. Summarizer assistant usage does not overwrite the result.
Gemini observes `info.newTokenCount` only after successful native history replacement
(COMPRESSED/CONTENT_TRUNCATED), never a compression log emitted before acceptance.
Failed/inflated compression does not produce a fresh successful estimate.
The next ordinary main-model response supplies the next context estimate.

`agent-context.cjs` stores at most 4 KiB of scalar metadata per agent/native session
in a private `context-usage` checkpoint (mode 0600, atomic replacement, regular-file
and no-follow read checks). It stores no transcript, image, tool output or key.
Reopen always marks the checkpoint stale; this does not modify native history.

## Checked anew / Проверено заново

- Genuine pinned CLI launches through the existing bootstrap/controllers, new
  synthetic HOME/projects, artificial keys and local providers. Kernel seccomp
  supervisor restricts networking; guard self-tests check denial paths. Public
  binary/bundle hashes and final adapter identities are recorded locally. Working
  profiles, authorization and chats are not read. Blocked background network
  attempts remain recorded; local-provider success does not verify a real account.
- Claude: two Read calls in one user request; last input 300 + cache create 20 +
  cache read 50 = **370**. Model change obtains the new exact model/window. Native
  manual compact emits a boundary/post count; a follow-up refreshes normal input
  usage. Cold reopen is stale. A second artificial chat starts without the first
  chat's values; switching back restores only the original chat's stale checkpoint.
- Gemini: two read_file calls yield last prompt **333**, not 263 or 403 after
  mishandling its cache subset of 70. A selected 2.5 Pro reports its native window.
  Auto invokes the utility classifier and then actual main `gemini-3.8-flash` in
  this isolated run; utility usage does not replace the main value. Native
  `/compress` reduced the controlled estimate **40,000 → 452**; the meter used the
  native successful result. Missing raw statistics preserve a stale estimate.
  Follow-up, cold reopen, second chat and switching back retain session ownership.
- Early fixture attempts are not passes: the initial Auto provider lacked the
  pinned numerical router's JSON schema and retried; its synthetic response was
  corrected. An earlier tiny-history compression was rejected as inflated;
  preserved stale counts demonstrate the failure path. These results do not
  establish model-quality preservation or fix attachment accumulation.
- **289 JVM/UI cases in 24 nonempty suites**, zero failures/errors/skips,
  checked anew on the final Java/resources/tests after the shared controller
  change. Earlier context-only runs had 249 cases; the final full run supersedes
  those results for this build.
  Both actual expanded menus are tested independently, including bottom placement,
  unchanged collapsed text, unknown/known windows, RU/EN, model refresh, stale data,
  close/reopen and foreign-run guards. Star menu coverage separately exercises
  Codex, Claude, Gemini and OpenCode in RU/EN, persistence/reload, favourites/search,
  long preview, edit/insert, cancel Save input after editing the form, unchanged
  library bytes/draft, and rejection of deletion. Existing terminal menu and
  library/navigation suites check other entry points. The first terminal-only
  assertion used an inherited test locale; the fixture now explicitly selects
  its expected locale. These are Robolectric fixtures, not phones.
- **19 Node suites**, including shared bounded checkpoints, per-agent/session
  ownership, cache formulas, cumulative/foreign events, model invalidation,
  compression success/missing metadata and the existing four-agent regressions.
  Each of the three pinned Gemini core bundles is hash-checked, transformed and
  syntax-checked. All three existing UI bundle transformations also pass.
- **Reused after the notes-only Java change:** the successful native CLI/context
  probes and Node results above. Final adapter, bootstrap/helper and pinned runtime
  identities match their tested inputs (`native-adapter-inputs.json`). The star
  change does not modify CLI policy, transport or context events. Final JVM/UI,
  release packaging, Lint and the APK gate were rerun after that change.

- Full modern **release Lint**, all five modules: **0 errors / 240 warnings**.
  One additional ViewConstructor warning concerns this programmatic-only meter,
  which has no XML inflation path. Compile SDK 35 is beyond AGP 7.4.2's tested
  range; the existing warning/limitations remain visible. Legacy AGP packaging
  emits Kotlin/lint compatibility diagnostics; it is not the accepted Lint result.
  No GitHub CI or device execution is claimed for this private APK.

Local evidence: `.verification/test-2.1.4-test3/` contains native-results and
native-adapter-inputs manifests, kernel logs/self-tests, CLI fixture identities,
JVM full/refreshed XML and input hashes, Node logs, Lint XML/summary, packaging and
artifact/runtime/template/source manifests. Tests: `scripts/tests/test_agent_context.cjs`,
`test_gemini_context_hooks.cjs`, `test_context_agents_native.py`, and
`AgentContextMeterTest.java`. The native harness refuses an unguarded launch.

## Manual compaction assessment — no new buttons

| Agent | Confirmed native route | Remaining UI lifecycle work |
| --- | --- | --- |
| Claude | SDK user `/compact`; native compact_boundary, optional post_tokens; confirmed in the artificial session | capture original run/session and operation, admission versus completion, duplicates, cancellation/error/disconnect and reopen reconciliation; handle missing post count honestly |
| Gemini | interactive `/compress` → tryCompressChat → accepted native newHistory/info | current bridge has no complete UI operation ID/owner/completion/error/cancel protocol; add that separately before exposing a button |

This is a support assessment, not authorization to compact working chats or a
claim that a safe manual control for these two agents is implemented.

## Final APK gate

Signed **release**, `com.termux`, ARM64, Android 8.0+ (min26/target28),
`2.1.4-test3` / **1488**, debuggable false. Existing release signer gate and
verifyX11Inputs remain enabled. Public certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

ZIP integrity, signature, DEX/runtime dependencies, all packaged assets and
production source identities are checked on the final delivered file. All seven
ELF payloads, including Fedora helper, match stable v2.1.3 byte for byte; 95 native
source/config inputs match before reuse. Existing reviewed Java/ARM64 staging
transforms are retained; no drivers/environments are rebuilt or installed.
All four template bytes equal the agreed ZIP and source assets. No new third-party
dependency/license is introduced; existing notices remain in the APK.
Final APK: **40,498,478 bytes**. SHA-256:
`f016798cc08c6c9cc96bc3e5ad55da3e22bb31e8668eae358aa10c52d6f1f5e0`.
APK and checksums are in `.verification/test-2.1.4-test3/`.
The two earlier, unissued candidates remain separate; their checks are not used
as the final delivered APK result.

Prior test1/test2 APK hashes remain unchanged. Published APKs, tags, Components,
update.json and working settings are untouched. The separate user README.md and
RELEASING.md edits retain their original hashes and are excluded from these commits.

## Phone scenario / Проверка телефона — pending

Use separate test profiles, with harmless test files and the usual explicit
permission prompts. No working chat needs a restart or compaction for this check.

1. Open an unused Claude chat, expand its status. Before native usage, expect
   «Нет данных». Send a request to read two harmless files. After completion,
   expect the latest cache-inclusive estimate and the native model/window if known.
2. Repeat in Gemini with an explicit available model, then Auto if available to
   the test connection. Expect the **actual model** beside the estimate; unknown
   windows have no percentage. Do not change real authorization for this test.
3. Change model while idle and send a new request. Old-model data must not appear
   fresh under the new model. Switch to another test chat and back: values stay
   attached to their own chats. Close/reopen status and check existing buttons.
4. If testing native CLI compaction, use only the disposable chat. After a genuine
   completion, expect a new native post count, or stale previous data if the CLI
   provides none; the following normal request must refresh it. No Claude/Gemini
   compaction button is added by this APK.
5. In each of the four test chats, tap the star: only Notes should open, with no
   section tabs. Check a note's preview, search, edit and insertion. Enter a draft,
   choose Save input, modify the note form and cancel: no new note and the original
   composer text must remain. Repeat with explicit Save, then reopen Notes. Check
   RU/EN; Commands/Alias remain accessible in the main library/ordinary terminal.
6. Report agent, selected/actual model, step and displayed count/stale label on
   mismatch. Never send keys or private session logs.

## Preserved limitations

Attachment accumulation/disconnections are **not fixed**. No new automatic-compaction policy,
experimental context mode or CLI fork is enabled. No phone acceptance of this new
meter is claimed. Low-level Android handoff/crash recovery and backup export/restore
on a device remain unverified in the agreed audit scope. MCP and plugins remain
excluded. See [audit matrix](SECURITY-AUDIT-STAGE2.md) and the paused
[attachment investigation](MEDIA-CONTEXT-INVESTIGATION.md).
