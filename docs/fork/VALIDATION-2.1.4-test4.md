# 2.1.4-test4 / 1489 — private release APK validation

2026-11-05. Checkout `/root/termux-fork/.verification/rollback-live-actions/source`,
branch `work/agent-project-panel`. Base: private test3
`60e578e70e6b36193ce94915fdf5ab1890e85460`. Last published stable release:
v2.1.3 / 1485, `308c88955fd3b0691ded560c78ee3f945538381e`.
Local implementation commits: panel/history `6bf0a5bc3048876d5a7ab5461c9b37af1dafc0b3`; preview limits
`f65f2c5a79fe42e0ab10b0f33f9155443abac70d`. Final source commit is recorded in the artifact directory
`SOURCE-COMMIT.txt` and `build-identity.json`.
No installation, working-session restart, push, tag or publication. README.md
and RELEASING.md remain the separate, byte-preserved user edits.

## Included / Состав

The existing folder button in **Codex, Claude, Gemini and OpenCode structured
chats** opens **Files and changes / Файлы и изменения**. The ordinary terminal
keeps its existing browser. A terminal-only/not-yet-connected agent route is not
silently presented as a supported structured chat.

- **Files:** the original WorkspaceFileBrowser, including file actions,
  timestamps, sort, selection and refresh. It is embedded, not reimplemented.
- **Attachments:** a derived view of existing current-chat history, with All /
  From you / From agent filters. Name, format, known size, message date, sender,
  availability, Open, Go to message and local Show location. Missing originals
  retain their records and message navigation. No automatic remote download.
- **Changes:** the whole selected Git project against a captured **HEAD** hash,
  with staged/unstaged, untracked/deleted and binary entries. Text additions are
  green and deletions red. Changes may belong to the user or other chats. There
  are no revert, delete, stage, commit or other write actions in this tab.

Preview-only caps change from HTML 4 to **32 MiB**, GIF 16 to **64 MiB**. The
GIF cap remains **16,000,000 pixels**. The single-document HTTPS origin, CSP,
network/other-file blocking, no native bridge, renderer-error handling and WebView
destruction remain. RU/EN error messages use the new limits. Original-file
sending limits and model context are unchanged.

All agreed test3 changes remain: modification times in both file windows, exact
four supplied Codex/Claude common templates, Codex/Claude/Gemini context displays,
confirmed Codex compaction, per-user-request timers in all four chats, notes-only
star menu. See [test3 coverage](VALIDATION-2.1.4-test3.md). Saved user instructions,
profiles and native history are not migrated or replaced.

## Local reads, not model input / Локальное чтение

| User action | Existing/local mechanism | Model operation |
| --- | --- | --- |
| Open window / Files tab | existing ProjectFiles directory metadata | none |
| Attachments / filters | loaded row metadata, stat-only availability | none |
| Load older history | one `chat-snapshot` request with its existing cursor | none |
| Read remaining long message | one `chat-detail` request with its offset | none |
| Preview local attachment | existing captured-profile file materialization and viewer | none; no draft staging |
| Go to message | existing stable row key and local scroll | none |
| Show location | same file manager in captured source environment | none |
| Changes / diff | local Git status/diff via existing ProjectFiles transport | none |

The panel's narrow Chat interface has no send/turn/compact methods. Its
AgentChatUi source emits only `chat-snapshot` and `chat-detail`. These existing
adapter handlers read history/API records; they do not invoke a model. The
four adapters now project metadata before display-text truncation through the
shared pure `fileReferences` helper. Neither this projection nor the catalog
creates another transcript store or edits CLI journals. Existing background
status polling continues at its original frequency; no new model polling is added.
Tests intercept every transport operation and fail on non-history operations;
preview tests also assert that the composer has no newly staged attachment.
This is source/fixture evidence, not a measurement of all third-party CLI traffic.

Git uses optional locks disabled, no pager, no external diff/textconv/fsmonitor,
and disabled clean/smudge/process filters. Tests use hostile artificial repository
filters to verify they do not execute. Raw comparison can differ from normal
filtered-worktree comparison; the UI states this. Git index bytes remain unchanged
in the fixture. NO_GIT, non-project and unborn HEAD have explicit empty states.
Diff output is bounded to the existing 256 KiB preview limit; failures are visible.
Normal file actions in the Files tab retain their pre-existing write capabilities.

## History coverage, errors and ownership / Охват истории

Only explicit native upload records/descriptions and concrete Markdown file links
in agent text are catalogued. Arbitrary paths, plain web pages and every file the
agent may have created are not indexed. Conservative direct remote-file links
can be listed; opening requires a separate confirmation and external browser.
Opening the tab itself makes no remote download. Local original paths are decoded
once; unsupported schemes are not launched. Availability uses file metadata,
not authorization-file contents.

| Agent, unchanged pinned CLI | Metadata/date source | Scope |
| --- | --- | --- |
| Codex 0.166.0 | native message/file rows; genuine known completion time where present | existing cursor pages and full-message details; unknown dates stay unknown |
| Claude Code 2.1.296 | native message/file rows; timestamp/at when supplied | existing partial-history/cursor merge, no replacement of older loaded rows |
| Gemini CLI 0.64.0 | native user/assistant rows; genuine known completion time where present | existing snapshot/detail availability; no invented reopen time |
| OpenCode 2.0.28 | native user/assistant rows; `message.time.created` when supplied | existing native session API pages/details |

There is no promise to discover inaccessible CLI archive records. A complete
state means **the history made available by the current chat adapter**, not every
file in a private CLI archive. Historical helpers may expose less metadata.
Dates without reliable message timestamps show Date unknown / Дата неизвестна.

Opening uses already loaded rows. Additional history is explicit **Load more**,
one existing page per action. Remaining long bodies are separate bounded detail
chunks (24,000 characters, Codex 30,000); an 8,192-character overlap preserves
ordinary links split at a chunk boundary. Only derived file metadata/cursor/tail
is retained by the window, not a second full transcript. A newer longer message
invalidates its completed-detail state. A trimmed presentation cache with no saved
cursor remains incomplete until a fresh snapshot confirms its accessible end. Initial per-message projection is bounded
at 100 references, with an additional 60,000-byte metadata budget across each
snapshot. Omitted references mark the row incomplete (`more`); explicit full-message
reading recovers remaining supported links. Canonical rows are not mutated.
Overlong/nonstandard Markdown and arbitrary prose paths remain outside the parser.

- Unread pages/messages: no empty-history claim; **Load more** remains available.
- Read failure/malformed cursor: visible load error and **Retry**, same operation
  and cursor; already found records remain. Failure is not successful completion.
- No attachments: only when available history is exhausted, no detail chunks
  remain, no read is pending and no read error exists; the current filter applies.
- Same message/path is deduplicated; independent messages retain their records.

The window captures profile/run/native-session ownership. Close/chat switch cancels
its Future and closes only sockets registered by its scoped history worker. Local
Git/stat reads use a separate owned ProjectFiles transport, closed with the window.
Generation/session/epoch checks reject late replies. Shared agent/current user
request is not stopped. An already started bounded adapter-side page may finish
its local read after client disconnection; there is no next-page loop and no signal
to the shared CLI. Existing preview preparation can finish its bounded cache read
after dismissal; its late UI result is discarded and no WebView is attached.

## Checks / Проверки

Checked anew by the assistant on artificial data:

- Each of four chat routes independently reaches the new panel; ordinary terminal
  stays on the original route. RU/EN panel tests cover both senders, filters,
  missing files, message/location/open actions and coloured Git differences.
- Each of four actual browser wrappers retains metadata, unknown dates, sorting,
  refresh and constrained 280 × 400 fixture layout. No claim of device acceptance.
- Each of four AgentChatUi sources uses one native cursor page, preserves older
  references on reopening, reads long details across a link boundary and deduplicates.
  Read error/retry and malformed non-progressing cursor preserve incomplete state.
  Window-close and chat-switch tests cancel owned reads and reject late replies;
  a foreign unscoped socket remains untouched.
- Real Git in artificial temporary projects covers staged+unstaged, new/deleted,
  binary, quoted/injection-like paths, absence of a project/HEAD and hostile filters.
  No working project or authorization is read.
- Exact HTML/GIF caps: one byte below/equal/above, empty file, GIF pixel boundary,
  corrupt GIF, missing file, readable original after preview error, network/file
  rejection, renderer failure, close before deferred layout and repeated release.
- Sequential full run: **337 cases in 32 nonempty suites**, zero failures/errors/
  skips. After the RU/EN preview-limit message correction, **28 cases** across
  viewer/history/file UI were rerun. After correcting trimmed-cache exhaustion,
  **74 cases** across shared chat/cache/panel/history were rerun. Unaffected
  results are reused after checking the precise source/input delta, not counted
  as a new full run. Final history-only run: **19 cases**. Each of the four agents independently
  traverses 350 artificial links in **three** 24,000-character fixture chunks
  (offsets 0 → 24,000 → 48,000), starting with only seven projected references.
  Every page advances and retains earlier records; the final exact set contains
  all 350 unique paths. No further read is issued after completion. All transport
  calls remain snapshot/detail only. This verifies the shared reader against
  bounded native-compatible responses, not a real model or phone. The native
  Codex adapter can return 30,000-character chunks; the reader follows its `next`.
  Across the runs: **343 distinct cases / 32 nonempty suites**, no failures,
  errors or skips; not 343 newly rerun cases after each small correction.
- **26 Node suites** pass on the final adapter/helper assets: the original 19 plus
  action times/events, delivery, stop ownership, Codex launch/connection and the
  new pure metadata projection/budget suite. Existing native-probe scripts whose
  copied dependency list changed received syntax checks; they are not claimed
  as new real-model or phone runs. Python fixture changes pass AST parsing.
- Final APK identity and Lint are recorded below.

The first attempted final JVM run overlapped another invocation in the shared
stage and failed writing XML reports. That run is **not** a pass. Sequential runs
replace it. Initial fixture problems (locale/JSON/shadow setup) are not app or
phone results.

Large-file evaluation is deliberately limited: artificial padded HTML with a tiny
inline vertex array and header-valid GIF bytes exercise streamed preparation,
not a representative WebGL scene or animated GIF renderer. Streaming-only fixture measurements were HTML 33,554,432 bytes / 32 ms and GIF
67,108,864 bytes / 44 ms, both with a coarse JVM heap delta of zero. These are not
peak-memory measurements or rendering timings. Timing and coarse JVM
heap deltas are local evidence only. No real Chromium/Android renderer was available.
A 16-million-pixel RGBA frame alone is about 64,000,000 bytes; decoder/renderer/cache
allocations are additional. Real peak memory, animation, scene interactivity,
Android lifecycle/keyboard and narrow-phone usability remain **not checked**.

Reused: genuine pinned native context-source investigations from test3, not rerun
against real accounts for this catalog-only change. Adapter projections changed
here, so their relevant Node/history/UI regressions are checked anew. See the
[test3 field meanings and limitations](VALIDATION-2.1.4-test3.md#native-field-meanings--источники).
No unchanged-source claim substitutes for the final APK gate.

## Final artifact / Финальный файл

- Package `com.termux`; version **2.1.4-test4 / 1489**; min SDK 26,
  target SDK 28; ARM64 only; non-debuggable; **40,515,346 bytes**.
- APK SHA-256: `9d3a86aec5fbcbbc685b1b633910482ec6fc1d1b7e562c4d966c03801798fe78`.
- Signer certificate SHA-256 (distinct from APK hash):
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Final ZIP, APK v2 signature, version/ABI/debug flags and runtime/DEX gate pass.
  No required or unreviewed missing runtime types; optional/hidden-library inventory
  remains explicitly reviewed, not a guarantee of all device behavior.
- All **7 ELF payloads** match stable byte-for-byte. Reuse is supported by **95**
  unchanged native source/config hashes. Bootstrap unchanged; verifyX11Inputs and
  verifyReleaseSigner remain enabled. No new native rebuild or component version.
- **123 assets** match their final source inputs. All four common templates match
  the supplied ZIP exactly; the separate project template is unchanged. Only the
  expected 16 assets differ from stable (including the shared reference projector).
- Notices are present; no keystore/.env/development logs, test fixture files or
  stale outer ZIP local headers were found by the artifact checks. This bounded
  inspection does not assert a new complete independent security audit.
- Stable and issued private test1/test2/test3 APK hashes remain unchanged.
  `update.json`, published tags and Components are not changed.
- Modern **Lint 7.4.2**, all five modules: **0 errors, 240 warnings**; successful
  engine analysis with no infrastructure diagnostic. Legacy packaging Lint is
  not substituted for this separate modern gate.

The final file was rebuilt after correcting both preview-limit error strings.
Its signature/runtime/source/assets/ZIP checks were repeated on this new APK;
checks of the preceding intermediate APK were not carried forward automatically.
Evidence directory: `.verification/test-2.1.4-test4/`. It contains test XML/logs,
modern Lint, runtime/DEX inventory, packaged-source hashes, template identity,
native reuse proof, SHA256SUMS and certificate fingerprint. These development
artifacts and test fixtures are not APK contents.

## Phone scenario / Проверка телефона — pending

Use separate artificial projects/chats and harmless files, no real keys or work.
The assistant does not install the APK or restart working sessions.

1. In each of Codex, Claude, Gemini and OpenCode, open the folder next to the robot.
   Expect Files / Attachments / Changes. Switch RU/EN once. Check long names and
   small-screen scrolling; existing file actions, dates and Refresh remain usable.
2. In each artificial chat with a user upload and an agent's concrete file link,
   check all three filters, Open, Go to message, Show location. Delete/move only
   an artificial original: the record/message link stays, availability changes.
3. Open a long artificial chat and load older/full messages explicitly. Already
   listed files stay without duplicates. If a read fails, expect load error + Retry,
   never No attachments. Close during reading and switch chat: no old-chat files
   should appear in the other chat. Opening/reading should not start a response.
4. In a disposable Git project, prepare staged, unstaged, new/deleted and binary
   changes. Check HEAD hash, green/red diff and binary status. No write buttons
   should exist in Changes. A non-Git folder should explain its state.
5. Preview artificial HTML/GIF near the caps, then over the caps. Try close during
   loading and reopen repeatedly. Expect readable errors/original saving, no
   network or other-file access. Report size, pixel dimensions, scene/animation,
   responsiveness and any crash; avoid oversized real private files.

On error report agent, action, expected/actual result, file size and a redacted
screenshot. Do not send authorization or private logs. Source and Robolectric
results do not close these phone checks.

## Remaining limits / Ограничения

Attachment accumulation/disconnections are **not fixed**. No forked CLI,
experimental context mode, transport switch or new automatic-compaction policy
is enabled. Full display history and original files remain intact. Research is
paused at its [existing checkpoint](MEDIA-CONTEXT-INVESTIGATION.md).
The [audit matrix](SECURITY-AUDIT-STAGE2.md) retains earlier user acceptance with
its original versions/sources. Low-level Android Gemini handoff/cancellation,
crash restoration and Android backup export/restore remain unverified; functional
artificial-key refusal and normal retry accepted on test7 are not erased. MCP
and plugins are excluded. This private test build does not publish a stable release.

## 2026-11-05 — subsequent phone report: missing history cursor

After this report, the developer installed test4 and reported a missing Earlier
messages button in an existing Codex chat, with only the latest pair visible.
Read-only diagnostics found retained original records and functioning native
history cursors. The unconditional head-snapshot exhaustion assignment in test4
can suppress a later real cursor. The earlier fixture checks did not cover this
startup transition; they do **not** establish absence of this regression.
See [diagnosis, separate fix and pending phone acceptance](VALIDATION-2.1.4-test5.md).
No native history, app data/cache or working session was reset to diagnose it.
