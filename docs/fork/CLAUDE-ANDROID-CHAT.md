# 2026-11-05 — clarification delivery/order, native reproduction and local fix

Status: reproduced locally with **Claude Code2.1.296**, published adapter **v2.1.4/384fd197** before the fix, then current local adapter for **2.1.5-test1 /1493**. Android controller replay passed separately. This is not a phone/cloud-account check and does not prove which branch occurred in the developer's screenshot.

## Confirmed cases

| Scenario | First request containing clarification | Visible order | Cold reopen |
| --- | --- | --- | --- |
| First provider byte delayed10s; clarification submitted while original request runs; response then held5s | Next user turn, after original result/completed; original result associates only original UUID | During text: original user → clarification → first answer. After final/replay: original user → first answer → clarification → next answer. Reproduced in native snapshot and Android controller | Same native consumption order; exactly one clarification |
| Native Write permission pending5s; clarification arrives before permission answer | Next tool-continuation request of **same original task**; result associates original and clarification UUIDs | Clarification precedes final answer | Baseline lost it; fixed reader restores its source UUID/time before final |
| Two identical clarification texts, different UUIDs, during Write permission | Both occur in same continuation request | Two separate user messages before final | Exactly two records, one per source UUID; no invented delivery receipt |

The first case is a confirmed ordering mechanism, **not** proof that all busy messages wait for the next task. The tool case proves mid-task inclusion is supported. Provider request membership is recorded separately from application admission, stdin write, queued/started lifecycle, user replay and final result association. No cloud inference was performed; the synthetic provider confirms request contents, not semantic model understanding.

The queued case changes time when Claude writes/replays the user record after the preceding turn. The adapter sorts on native timestamp; the Android controller reflects that order. We keep that native order and label a proven Claude queue **“В очереди — после текущей задачи” / “Queued — after the current task”**. The details explain that later native events may confirm inclusion during continuation. Generic current admission no longer claims steering of the original task. The explicit next-task label additionally requires `queuedWhileBusy`, derived from the original admission state; the first idle input is not described as waiting for a nonexistent task.

The absorbed case persists as native `attachment` / `queued_command`, not an ordinary user record. Baseline `ClaudeHistory.visible` excluded it. The local read-only projection accepts only human prompt records with a valid `source_uuid`, uses that UUID for UI identity and the original attachment timestamp for placement, and keeps raw record UUID/parent links for bounded ancestry traversal. It never rewrites the native JSONL or creates another history store. This record alone yields **no** delivery/current-task status.

## Identifiers and evidence

Final native repeat session: `33333333-3333-4333-8333-333333333333` (artificial only).

| Client ID | Native user UUID | Initial receipt |
| --- | --- | --- |
| `msg_stream_first` | `3d17882d-d9fd-4d02-8886-ff5c240b03d8` | queued / command-lifecycle:queued |
| `msg_stream_followup` | `451294e5-a385-439a-91ab-9c748c561f9c` | queued / command-lifecycle:queued |
| `msg_tool_first` | `5c4c7190-cb3b-41a0-bb4d-d36b47343c29` | queued / command-lifecycle:queued |
| `msg_tool_followup` | `2fdc97e6-e77e-4112-825e-ac258cd12675` | queued / command-lifecycle:queued |
| `msg_tool_repeat` | `a6bc6521-e767-43be-9abd-92ff0cebc1ef` | queued / command-lifecycle:queued |

Full ordered events, native timestamps, provider byte counts/marker membership, snapshots and native-parent IDs: `.verification/t/deliv-bkZEBB/result.json` under `/root/termux-fork`. Source/binary identities and guard events are alongside it. Earlier unfixed runs are retained in `.verification/claude-delivery-investigation/{first,second,third}-root.txt` (checkout-relative pointer files).

Native executable SHA256: `f1f6e96e0d8342b9dbf41d7e88255397a6a52ce3d8736ad6a4c6b59c9b62fefa`. Exact production stream-json flags and actual Unix adapter requests were used. Alias `opus` resolved to request model `claude-opus-5-5`; the local provider returned synthetic content without running that cloud model. Test-only differences: fresh HOME/config/Git project, artificial API key, loopback provider, strict empty MCP config and user/project-only settings, kernel external-network denial. Only owned fixture processes were stopped after PID/start-time verification. Working sessions, messages, authorization and original history were untouched.

Regression checks: `test_claude_clarification_history.cjs` (45 identical strings/distinct UUIDs across bounded pages, three cold readers, no duplicates/status inference, native SHA unchanged); `test_claude_history.cjs` (>8000 records/680tool results, large records/pages, append/resume/compaction ancestry and checksum); actual native fixture with two identical inputs; `ClaudeClarificationReplayTest` through real Android controller; `AgentMessageDeliveryTest`. Native snapshots used for replay are in test-only `claude-clarification-native.json`, excluded from APK assets. Targeted JVM result: `.verification/test-2.1.5-test1/clarification-ui.log`.

## What remains unconfirmed

The developer's actual clarification UUID, task boundary, runtime version/launch route and receive/replay timestamps were unavailable. No working message was resent or moved. The screenshot alone cannot select between the queued and absorbed cases; the long real task is not reproduced by a20s local fixture. Actual phone display/reopen and real subscription authentication remain separate checks. This finding is independent of timeline styling and quota/reset links.

---

# Private test15 — native active input

The actual pinned Claude2.1.296 supports bidirectional streaming input and emits
`command_lifecycle` keyed by `command_uuid`. Follow-ups during responses or a
pending tool use that existing process; native queued/started acknowledgements,
not stdin.write success, control Android admission and composer clearing.
Busy input requires `msg_lifecycle_v1` and `interrupt_cancel_queued_v1`. An older
unsupported launch preserves the draft and explains the limitation.

Queued messages may be merged by Claude into its next input. They are labelled
as queued, never promised as immediate steering of the current response. The
same-process projection retains the separate admitted messages without duplicating
merged replay text; after full restart native history remains canonical. Same-ID
reconnect retrieves the recorded admission instead of writing again. Interruption
uses the existing native control path with its supported cancel_queued receipt;
rights and tool permission nonces are unchanged. No auth data is read by tests.

Actual pinned CLI + loopback synthetic provider fixtures cover response/tool busy
input, ordered follow-ups, completion races, cancellation, reconnect, permissions,
history resume and unrelated-process survival. This is protocol evidence, not
live cloud-model/phone acceptance. See [test15 validation](VALIDATION-test15-audit-fixes.md).
Official references: [CLI streaming flags](https://code.claude.com/docs/en/cli-reference),
[Agent SDK types](https://code.claude.com/docs/en/agent-sdk/typescript).

---

# Claude Android chat — v2.0.8

The shared native chat UI supports Developer Markdown, outlined code Copy,
downloadable file cards, saved file sorting, long-press attachment multi-selection,
system gallery photo/video selection, scoped Stop and keyboard dismissal after
acknowledged message sends. Attachments remain a draft until Send. Native CLI
account/project/permission/history ownership remains unchanged. Restart an existing
session with its saved chat when new attachment helpers are needed.

See [current usage](USAGE.md), [release](RELEASE-v2.0.8.md) and
[validation and device limits](VALIDATION-v2.0.8.md). The implementation records
below are historical and retain the scope of their named versions.

---

# Claude: Android chat (private v1.5.21-test)

New Claude launches share AgentChatView / AgentChatUi and the bounded presentation
cache introduced in v1.5.20. ClaudeLiveUi renders the model, native permission mode,
connection state and last-request token usage. No subscription quota is invented.
Codex, SSH and ordinary terminals keep their current interfaces. OpenCode and
Gemini retain their own transports and native permission mechanisms.

## Native protocol and ownership

The existing pinned Claude Code 2.1.296 binary runs with native bidirectional
stream-json input/output, partial messages and the stdio permission callback.
No additional SDK binary or model prompt override is installed. The protocol is
based on the official Anthropic Agent SDK Python transport/query sources and
verified with the pinned CLI. claude-control-v2.js owns one private Unix socket,
one selected native conversation and its child processes; claude-chat.cjs projects
native text, reasoning, tools, permission requests and supported questions into
stable Android rows. Subagent/foreign-session events do not replace the root chat.

History remains native Claude JSONL, scoped to the exact conversation ID in its
own configuration directory. The main parentUuid branch is read; unrelated,
sidechain and team messages are excluded. Old running Claude controllers remain
unchanged: restart Claude with the same saved conversation to load this adapter.
No data reset or native history format migration is required. New and existing
chats, explicit IDs and native history selection are retained.

Native modes are manual, acceptEdits and plan, with their own Claude semantics.
Model/mode changes are stored in the captured launch profile only after native
acknowledgement. Native can_use_tool requests require a fresh request ID, session,
run epoch and nonce. Android supports allow once / deny and supported structured
questions; unknown requests are rejected. Approval receipts describe the decision,
not whether the tool succeeded. Repeated send IDs cannot blindly repeat an action.
Claude CLI print mode bypasses its interactive trust dialog; this does not add
filesystem isolation or Android privileges. Native tool policy remains in force.

## Terminal and Shell

The robot opens the same compact terminal dialog and large configured native keys
as OpenCode/Gemini. There is no duplicate small key row. Idle SDK operation hands
over to native Claude; a request, approval, Shell command or active task blocks
handoff until it finishes. No model request is interrupted to open the terminal.

For unauthenticated Claude the terminal runs native `claude auth login`; after
login exits, SDK chat reconnects automatically. A full native CLI or history
picker stays alive when Back hides its window. Enter `/exit` or Ctrl+D inside that
CLI to end it and resume SDK chat. Back never kills the native process, including
when native notification hooks are disabled. Both the terminal window and main
chat explain this requirement in RU/EN. Hooks only notify session/lifecycle state;
they never grant permissions or inject prompts. If history selection cannot be
identified safely, the adapter requests an explicit ID instead of guessing from
other conversations.

The separate Shell field requires interactive input and executes an explicit user
command in the selected Ubuntu directory without model inference. Its output is
saved in a bounded private application sidecar, separate from Claude history.
It is not injected into the model's conversation/context. Shell is unavailable
while Claude is busy or the full native CLI owns the terminal.

Stopping the session terminates only owned processes, using captured process-group
identities before forced cleanup. Ordinary tab changes and hiding the native
window do not stop Claude. Unrelated sessions/processes are not targeted.

## Verification and limits

See [VALIDATION-v1.5.21-test.md](VALIDATION-v1.5.21-test.md). Real pinned CLI tests
use isolated configuration and a synthetic loopback provider: they verify wire
protocol, native file-tool approval/denial and native history resume without using
an account or claiming real model quality. Physical OAuth/browser, terminal keys,
IME, gestures and provider/account errors require developer device testing.

Status, presets and routine diagnostics do not receive auth files or raw chat
content. Transcript/detail views necessarily contain selected conversation text.
Display caching does not retain live approval controls or change model context.
