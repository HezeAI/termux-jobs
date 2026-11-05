# 2.1.5-test1 /1493 — private preparation, not issued

No APK has been issued for this version. No install, working restart, push or publication is authorized or performed.

## Agreed APK scope before assembly

New relative to last published stable **v2.1.4 /1492**: four-agent action timeline summary/details/state/scroll handling; native Claude subscription-window reader and separate two-window meter; Claude/Codex official usage links; explicit Claude next-task queue caption and read-only restoration of absorbed human queued_command messages with UUID/time identity. All have RU/EN text. No application behavior outside these traced paths is intentionally changed.

Retained from stable, not presented as new: earlier-history loading; Files/Attachments/Changes, bounded catalogue, filters/search/refresh/cards/per-file actions and Git HEAD diff; dates in both file windows; original image/gallery saving; HTML32MiB/GIF64MiB previews with16million-pixel/network/file-access protections; four exact Codex/Claude RU/EN common templates plus separate project templates and explicit/conflict-safe save; Codex/Claude/Gemini context and existing Codex manual compaction; four-chat whole-request timers and notes menu; transparent agent-chat status region. User files/instructions are not migrated or replaced.

Deferred/unverified: attachment accumulation/disconnections, CLI fork/experimental contexts/new automatic compaction; original developer clarification incident linkage; real Claude subscription/account/profile-scope values; low-level Android handoff/crash recovery, device backup export/restore, real Gemini API/Vertex sign-in; new Claude OS sandbox. MCP/plugins are excluded. Existing phone audit acceptance remains its historical result, not acceptance of this new APK.

Phone work: four-agent timeline update/collapse/scroll/earlier-page/session switching; Claude queue/current-task inputs and cold reopen; native subscription values or No data; official usage links/return at narrow width/large font. Continue using isolated test chats. Complex Android/backup tests remain excluded from device work by agreement; their stand results are separate.

Same com.termux/signer/ARM64, min26 target28 compile35, schema8, higher code1493. CLI and binary Components unchanged. New helpers become active on a future explicitly initiated agent launch; no working launch is restarted by preparation. Final signed APK checks are mandatory and pending until the final file exists. No install/push/publication.

## Subscription website links — Codex and Claude

The expanded status shows **Reset limit ↗** beside the subscription line, wrapping below the full name when width/font size requires it. The collapsed chip is unchanged. The explanation states that availability and conditions are on the website; the link is available even if native subscription data is missing.

Fixed destinations:

- Claude: https://claude.ai/settings/usage
- Codex: https://chatgpt.com/codex/settings/usage

Navigation reuses the existing external HTTPS/browser chooser. The shared browser function validates HTTPS and handles missing/restricted browser activity. It passes only the fixed URL, with no credentials or provider requests. There is no reset/purchase operation or return callback that alters counters. Native status polling remains unchanged; only fresh native data can change existing indicators.

This feature does **not** establish reset eligibility, subscription type, or availability of Claude five-hour/weekly windows. Those native fields and their actual chat route remain a separate investigation. The context meter stays separate. Clarification delivery/order is another independent task.

### Verification scope

Fresh JVM/UI checks cover both real expanded-menu entry points, fixed chooser destinations, unchanged synthetic current-request/status/limit values, RU/EN, 280dp with double font scale, wrapping full names, separate touch target, wider layout and reopened Claude menu. The existing documentation browser tests cover the shared browser path and rejected non-HTTPS addresses. Real browser/account actions are not performed.

Evidence: `.verification/test-2.1.5-test1/subscription-links-tests.log`, `subscription-first-results/`, `subscription-links-recheck.log`; modern Lint results are recorded separately when complete. The first wrapping assertions failed because Robolectric 4.4's font advances do not reproduce physical glyph widths, even in REALISTIC line-layout mode. The final fixture deliberately uses an overlong name that overflows in either renderer, retaining the 280dp width and double font scale. It verifies button relocation, full TextView text, unlimited label lines, no ellipsis and touch bounds. ShadowPaint measures String.length(); the API28 shadow also returned a zero line end for the nonempty label. These native-font layout results cannot verify physical multiline typography. Production layout thresholds remain unchanged. Actual short-label typography remains a phone check; preserved failed runs are not counted as passes.

Fresh modern Lint: all five modules analyzed, 0 errors /247 warnings. One additional ViewConstructor warning concerns this programmatic-only row, which is never inflated from XML. Report: `.verification/modern-lint-report/summary.json`; no Lint suppression or gate relaxation.

Across the targeted runs, **48 unique JVM/UI scenarios passed**, including the final layout-bound test. Original failed attempts remain preserved and are not counted as passes. Unchanged context/limit/menu tests from earlier targeted runs are reused only for the same production source bytes; four affected Java hashes match the modern-Lint stage. Compact identities/results: `.verification/test-2.1.5-test1/subscription-source-hashes.json` and `subscription-verification-summary.json`. The final focused layout run is `subscription-layout-checked.log`. These results do not close the separate timeline/controller regression check or any device check.

### Phone scenario — not checked

1. Open Codex status, then Claude status. Check the subscription line and explanation.
2. At large system font size, confirm the name remains complete and the button wraps without clipping.
3. Tap the link: verify the matching official host and usage page in the browser. No reset/purchase is required for this check.
4. Return to the chat. Confirm the agent continues and counters were not immediately reset by navigation.
5. Close and reopen the status. Missing data must remain missing until fresh native status arrives.

## Other pending changes

The four-agent timeline controller scroll/session-switch check now passes in the targeted run; the final broad regression gate and APK checks remain pending. Claude clarification placement and subscription-window source verification remain independent. Final APK compilation/signature/package/version/runtime/native/template checks will apply to the actual new final file; old APK verification does not carry over.

Existing scope restrictions remain: attachment accumulation/disconnections are unresolved; low-level Android handoff/crash/device-backup checks and real Gemini API/Vertex login remain unverified; MCP/plugins excluded.

## Claude native subscription source

Checked pinned Claude2.1.296 in the actual app stream-json backend, fresh artificial API authorization and kernel loopback-only fixture. Native `get_usage` with `skip_behaviors:true` worked on four controls across startup/restart; no model requests or provider HTTP requests occurred. Final adapter status exposed sanitized `available:false` with null windows, correctly reflecting the API fixture. This does not establish availability for the developer's subscription/account/profile scope.

The native CLI embeds an experimental structured `get_usage` contract: `rate_limits_available`, optional `subscription_type`, independent `five_hour`/`seven_day` with **used utilization0–100** and ISO reset timestamps. It is distinct from terminal [statusline fields](https://code.claude.com/docs/en/statusline#rate-limit-usage). The adapter requests only these native controls, at most once/minute while idle and as part of existing status reads, with no new timer or model turn. `skip_behaviors:true` avoids the optional scan of other transcripts. Unsupported responses disable retry until worker replacement; optional statistics failures do not turn into session errors.

Only explicit available native plan windows feed the two remaining-percentage bars. No API headers or cumulative token costs are substituted. Unknown plan name does not suppress valid windows. Missing/invalid values are No data; cached age, disconnect and elapsed reset mark stale. Reset passage never creates100%; fresh native results replace values. Auth generation/worker replacement rejects stale replies and clears prior scope; nothing is persisted across accounts. Native schema is experimental and must be revalidated on CLI updates.

Evidence: `.verification/claude-quota-native/root.txt`, `status.log` and pointed private fixture identity/result/network files; `test_claude_subscription.cjs`; `ClaudeSubscriptionMeterTest`; `quota-timeline-targeted.log`. Positive subscription values, individual missing fields and reset/stale/scale conditions are synthetic contract/UI tests; **real subscription percentages/resets remain unverified**. No real authorization was read or changed.

## Claude clarification delivery and restoration

Native reproduction and separate Android replay established two cases: next-turn queued clarification can move below the preceding final answer, while a clarification at a tool boundary can join the current task. The local fix labels the queue explicitly and restores human native queued_command records by source UUID and original timestamp without inventing delivery status. Two identical texts with distinct UUIDs survive cold reopen separately. [Full evidence, conditions and limits](CLAUDE-ANDROID-CHAT.md).

The developer's original episode is **not conclusively diagnosed** without its native event/UUID evidence. Phone acceptance of these new changes remains pending. The clarification task is not closed by timeline/quota tests.

## Final source gates before assembly

Checked anew: broad targeted Java/UI suite **433 passed /0 failed /0 skipped** (49 suites). A late presentation-only `queuedWhileBusy` field was then added so the initial idle input is not labelled after a nonexistent task; its affected path was checked anew in **6 delivery/replay tests** plus four targeted Node delivery/active-input/history/projection scripts. The earlier broad log is not described as a rerun after this refinement. All native-process/control paths retained their identity checks.

Node:27 baseline/current affected suites passed, plus new quota normalization and clarification-history checks. Native quota: four controls over startup/restart, explicit API absence and zero model/provider requests. Native clarification: initial, delayed-text, delayed-first-byte and fixed/two-identical-input variants, all under private HOME/config and external-network denial. Actual synthetic native JSONL hash remained identical around the final cold reader.

Final compatible Modern Lint: all five modules, **0 errors /250 warnings**. Existing warnings retained; the new programmatic view constructor warning and SetTextI18n warnings are disclosed rather than suppressed. Current source hashes will be compared with both Lint and APK stages. Previous temporary text-layout fixture failures, first timeline controller timing failures and superseded broad-test failures are retained; they are not counted as successful runs. Physical glyph metrics remain a phone check.

No new third-party dependency or native component is added.95 source/config inputs match stable v2.1.4; three JNI payloads will be reused byte-for-byte and all seven outer APK ELF payloads compared with stable. X11 packaging and release signer guards remain enabled; no X11/server/GPU execution is performed for this chat UI task.
