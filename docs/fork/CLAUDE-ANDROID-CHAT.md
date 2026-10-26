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
