# 2.1.4-test5 / 1490 — history cursor regression and project panel

2026-11-05. Private test APK, branch `work/history-cursor-panel-search`, canonical
checkout `/root/termux-fork/.verification/rollback-live-actions/source`.
Base: issued test4 `386984071b86b581446be3cbb865e5fb23a1d2c7`.
Last published stable release remains v2.1.3 / 1485 (`308c8895`). No push, tag,
publication, installation by the assistant or working-session restart.
README.md and RELEASING.md are preserved separate user edits.

## User report and read-only diagnosis / Диагностика

The developer **installed test4** and reported that the existing CodexSupport
chat showed only its latest pair of messages. The clarification and screenshot
identify the missing **Earlier messages / Предыдущие сообщения** button. This
is a phone UI observation, not evidence that native messages were deleted.

Before source changes, the assistant performed these read-only checks:

- Located the current native thread from its non-secret thread ID and existing
  scalar status records. No authorization/configuration file was opened.
- Streamed a bounded prefix of that thread's original JSONL: **1,861,527,795
  bytes, 250,043 complete records**, no invalid record at the captured boundary,
  timestamps from **2026-10-11** through **2026-11-05**. Its existing inode
  remained the same; normal append by the active session continued. Only counts,
  timestamps, file identity and a prefix hash were saved, not transcript contents.
  Prefix SHA-256: `d1abfea8553039b5aa57c42fdb82a7f43ee6e8b7577a3aacd4d0f942e6e23148`.
  This confirms retained old records, not a comparison of every message with a
  historical backup. The session metadata's creation CLI version is not assumed
  to be the current running binary version.
- Connected a separate diagnostic reader to the existing local app-server.
  Allowed calls: `initialize`, `thread/read(includeTurns:false)` and two
  `thread/turns/list(limit:1, itemsView:full)` calls. No resume/start/send/compact,
  permission/settings changes, stop, data/cache cleanup or CLI journal writes.
- The first native page returned **28 items, 257,449 bytes**, a **131-byte** next
  cursor. Reading that cursor returned a different older turn with **416 items,
  1,207,394 bytes** and another cursor. The server can still read older history.
  Raw bodies/cursors were not copied into the report; IDs are hashed in evidence.
  Sizes are serialized UTF-8 JSON results, excluding RPC/WebSocket framing;
  they are not model-request sizes.

Compact local evidence: `.verification/history-regression-test4/`:
`native-history-metadata.json`, `native-read-pages.json`, deployed helper hashes,
`exact-merge-reproduction.json` and its standalone reproduction source.
The initial failed reproduction attempt used an incorrect local JDK path; it
made no application change and is not a test pass.

## Confirmed defect / Подтверждённая причина

`AgentChatUi.mergeHistoryRows` in test4 assigned `historyExhausted=true` after
**any** accepted empty head snapshot. A head snapshot can arrive before native
startup/history initialization has finished. Later snapshots update the visible
latest rows, but their real cursor is ignored by:

`previous || older.isEmpty() && !historyExhausted`

`AgentChatView.rows` shows the button only when `older` is nonempty. The result
is a latest pair with no way to request the existing older page. This is an
Android presentation/cursor regression introduced in `6bf0a5bc`; the CLI model
request, transport and original JSONL are separate paths.

The standalone harness compiles the **exact extracted Java merge method** from
test4 with artificial rows. Empty early snapshot → real cursor produced two
visible rows, empty `older`, `historyExhausted=true`, hidden button. Restoring the
previous assignment guard produced the same two rows with the real cursor and
visible button. This is independent of any model/network request.
The phone's initial in-memory snapshot timing was not directly traced; the
reported symptom, surviving native cursor and exact deterministic reproduction
identify the matching code defect. No claim of Android memory inspection.

Local fix commit: `21d69154c0e2d6b8b23054a03e8674fb1aa55550`. Exhaustion is now committed
only when an explicit older-page read reaches its end. A separate ephemeral
`historyRefreshRequired` marks a trimmed presentation cache until a fresh head
snapshot arrives. It does not seal history, change CLI journals or persist
profile settings. Ordinary later head polls do not reopen already exhausted
older pages. Source/session generation guards remain.

The first focused run passed **26 history/cache cases**, including late cursor
appearance and actual button click for each of four adapters, original row
merging, page failure/retry and no reopening of an exhausted cursor. These are
Robolectric fixtures. Phone acceptance of the fix requires the next APK.

Final measured-size update: `92e431b35bacf1f9164de2210306df0c3559b7a0`.

Panel implementation commit: `5a84426ffd97fdb78ce04e3aee410a262727eb92`.

## Panel changes / Изменения окна

For each of Codex, Claude, Gemini and OpenCode:

- **Attachments** and **Changes** have a first **Refresh** button below the tabs,
  then their search field and clear cross, then list contents. The existing
  **Files** tab, its folder, actions and refresh placement are unchanged.
- Active selection uses a contrasting panel fill, accent frame/marker, bold
  foreground and Android selected state for all three tabs. It is not indicated
  by text colour alone. Workspace dialogs retain their existing fixed dark
  palette under both supported application themes.
- Attachments searches file names case-insensitively in the already loaded
  metadata, combined with All / From you / From agent. It explicitly states the
  available scope. No matches in loaded history is distinct from no matches
  after all adapter-accessible pages/details have been read.
- Search never requests an older page. **Load more** remains explicit and applies
  the current query to each arriving portion. The bounded reader and deduplication
  from test4 remain; no second history store or model-context mutation.
- Attachment refresh issues **one existing head `chat-snapshot`**, retaining the
  older-page cursor/loaded records, and rechecks visible availability metadata.
  It neither loops through the archive nor starts a model turn. Search/filter
  remain. A read error retains found records and offers Retry.
- Attachment cards form a single vertical column with intrinsic height. Existing
  format vectors appear on the left; no originals are fetched for thumbnails.
  Multi-line name, format/known size, sender/date and availability are separate.
  A successful stat refresh updates the size line for linked files too.
  Available Open, message navigation and known local Location actions are below
  the facts. Missing local originals hide Open while retaining record/navigation.
- Change cards also form a single column: multi-line name, relative path, status
  (including renamed/conflict), real working-file modification time and native
  Git numstat additions/removals. Deleted/unavailable dates remain unknown, never
  commit/refresh time. Binary numstat shows Binary file instead of line counts.
  Rename numstat uses native rename detection. Only visible cards are enriched,
  at most 30 initially, with explicit Show more; queued stale metadata reads are
  skipped after a search/refresh/diff change. Reads remain local and bounded.
- Changes searches file name/relative project path from the loaded Git listing.
  Refresh rereads Git status and an open file's diff; queries remain. Busy guards
  prevent duplicate operations. Failed refresh retains the previous listing/diff
  with a visible structured error. HEAD attribution, binary handling and read-only
  Git safeguards remain; this is the entire project's work, not an agent-only diff.
- Closing/switching chat retains cancellation/late-result ownership checks. No
  working history, authorization, profile, permissions, context policy, components
  or file sending limits are changed by these controls.

## Checks and final APK / Проверки

The final signed **release** artifact (not a published release):

- `com.termux`, **2.1.4-test5 / 1490**, ARM64, min SDK 26 / target SDK 28,
  non-debuggable; **40,522,066 bytes**.
- APK SHA-256: `656cf9da76beda019eca5aba68d1d54f12fe08ad6965034a1453049dc8b0ca68`.
- Certificate SHA-256 (separate from file hash):
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Final ZIP, v2 signature, manifest/version, static DEX/runtime inventory pass;
  no required/unreviewed missing runtime types. This is not a device runtime test.
- All seven ELF payloads match stable byte-for-byte, with 95 matching native
  source/config inputs. Bootstrap/components and their versions remain unchanged.
  `verifyX11Inputs` and `verifyReleaseSigner` remain enabled.
- All 123 packaged assets match source; exact four supplied templates and the
  separate project template are checked again. All adapter/helper/template assets
  match test4, supported by an explicit hash manifest (`reused-inputs.json`).
- Modern Lint 7.4.2 analysed all five modules: **0 errors, 240 warnings**, no engine
  infrastructure diagnostics. Legacy packaging Lint output is not this gate.
- Stable and private test1–test4 APK hashes remain unchanged. Existing published
  releases/tags/Components and update.json are untouched. Unrelated README and
  RELEASING edits retain their original hashes.

Checked anew: **354 JVM/UI cases in 34 nonempty suites**, zero failures,
errors or skips, in one sequential full run before the final linked-file size-line correction.
After that correction, the **10 affected panel cases** passed again, including
all four agents, both themes and both languages. The remaining full-run results
are reused after reviewing that single production-file delta. The APK was rebuilt
and every final-file gate and modern Lint repeated; the old APK result was not
carried forward automatically. This includes the new history
cursor transition tests and each of four panel routes separately in RU/EN and
both themes. Search/filter/load-more, refresh deduplication/loading/error retention,
file-tab preservation, single-column card structure, actual Git date/numstat/rename/
binary cases, owned-read cancellation and late-result rejection are covered.
The 47-case candidate run preceded final small source/test additions and does
not replace this full run.
Declared-palette contrast: active text **11.14:1**, inactive text **7.38:1**,
active frame **7.77:1**. Narrow 280-pixel fixture layout has nonzero usable
search/tab widths. These are configuration/layout measurements, not visual phone
acceptance or a keyboard screenshot.
Reused: **26 Node suites** and earlier genuine native CLI/context-source probes,
only after matching all relevant final assets, bootstrap/helper and pinned inputs.
Their original fixture/model/phone scope is retained; no new real-account or
external-route success is claimed. Production Java and UI are checked anew.
Initial fixture runs used convenience Typeface/Drawable methods not fully
implemented by Robolectric 4.4: final assertions inspect the stored bold style
and the actual declared state drawable/fill/frame. Selected state and the visual
configuration remain asserted, not disabled. Intermediate failed compilations
and test logs remain local evidence and are not counted as passes.

Task evidence: `.verification/test-2.1.4-test5/` contains final logs/XML, Lint,
static runtime inventory, native/source/template checks, SHA256SUMS, certificate
fingerprint and final source identity. Test fixtures and diagnostic metadata
are not APK contents.
Source/fixture checks are separate from real Android acceptance.
No CLI/context/network research is resumed by this task. Existing native CLI
version/authorization investigations keep their original scope.

## Phone scenario / Проверка телефона — pending

1. After the user's own update, reopen CodexSupport without resetting data,
   clearing cache, compacting or restarting the CLI. Expect Earlier messages to
   appear after the real history cursor arrives. Tap it: older records should
   appear while the latest pair remains. Report visible date range and any load
   error; do not export private transcript/authorization.
2. In a separate harmless chat for each agent, open Files and changes. Verify
   clear active tabs in both application themes. Files keeps its existing refresh
   location and current folder.
3. In Attachments, search a known file name and combine each sender filter.
   Load another portion manually: the query remains and new matching records
   appear without duplicates. Refresh and clear with ×. Failed read must retain
   records and offer Retry, not say No attachments.
4. In a disposable Git project, search name and relative path. Refresh list and
   opened diff; search stays and no write action appears. On a narrow screen and
   with the keyboard open, confirm readable tabs/search and usable close controls.

The assistant does not install or restart working sessions. New UI/phone results
remain unverified until reported by the developer.

## Retained scope and limitations

All agreed test4 features remain, including exact four common templates, file
timestamps, context indicators/manual Codex compaction, four request timers,
notes-only star menu and HTML/GIF preview caps 32/64 MiB with the 16-million-pixel
GIF/single-document sandbox. Original APKs remain unchanged.
Attachment accumulation/disconnections remain **unfixed**; no experimental mode,
CLI fork or new automatic compaction. Real heavy WebGL/GIF rendering remains a
phone task. Existing [audit](SECURITY-AUDIT-STAGE2.md) low-level Android handoff,
crash recovery and device backup checks remain unverified in the agreed scope.
MCP/plugins remain excluded. No stable-release acceptance is inferred from this
private test build.
