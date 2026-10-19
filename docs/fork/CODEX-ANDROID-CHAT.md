# Codex Android chat — private v1.5.32-test

New Ubuntu Codex launches can use the existing AgentChatView/AgentChatUi surface,
matching Gemini/OpenCode/Claude. User and assistant messages remain normal rows;
reasoning summaries and native tool output are expandable pills. The robot replaces
the packages button and opens the existing compact terminal with native large
extra keys and Enter. Shell input remains gated by interactive input. Codex's top
model/reasoning/account-limit card retains its dimensions and status source.

## Native data and session ownership

The same codex app-server, account, selected project and native conversation store
are used. codex-chat.cjs consumes the documented app-server protocol, not terminal
screen scraping and not an extra model prompt. A new chat uses thread/start;
resume verifies original/confirmed cwd and requests the existing thread with the
chosen sandbox/approval policy. Native acknowledgement of cwd/policy is required.
No instruction, credentials or global Codex configuration file is rewritten.
History uses thread/turns/list pages (8 turns, full items); a new unmaterialized
thread starts empty without an error. Oversized/unsupported native history can
require the native terminal; no claim of unlimited history loading is made.

A private marker tied to the new run's unique socket ID enables the new UI. Already
running Codex sessions and legacy Codex-in-Termux launches keep their terminal
presentation. Updating the APK alone does not restart or convert their processes.
Reopen the same native chat ID to use the new surface; history is not imported or
converted. Default saved-profile behavior is unchanged. Library schema remains8.

The current root thread filters streamed events. Subagent/foreign-thread messages
do not replace the main transcript. Native item IDs give stable row identities;
completion replaces a streamed partial item. Presentation stays bounded, with
older-history loading and detail paging. Receipt metadata is private per native
thread and attached to its tool item, not appended to every refreshed snapshot.
Unknown native item types appear as bounded tool-detail rows.

## Messages, approvals and questions

A persistent controller owns native turns and server requests. Text uses
turn/start; follow-up text to an active model turn uses turn/steer with the expected
turn ID. Explicit user Shell uses native thread/shellCommand, without inference;
like native Codex's direct Shell, it runs with Ubuntu user privileges, outside the
agent sandbox. This distinction is explained in the native terminal and Codex
status panel. Shell/terminal/model operations cannot overlap an active native Shell
or conflicting handoff. No hidden model request is made to poll the transcript.
Slash commands are directed to the native terminal instead of being silently sent
to the model. Normal chat messages remain available in the app composer.

Native command/file-change/permissions requests are shown in the chat with allow
once/deny buttons. A per-request nonce, run and native thread/turn must still match
when answering. Denying a permissions request grants an empty subset; allowing it
grants only the requested profile for that turn. No automatic escalation or new
persistent policy rule is introduced. Supported non-secret requestUserInput
questions use structured chat forms. Secret questions, MCP elicitation and other
unsupported server requests fail closed with a visible explanation; they are not
silently approved. The full CLI can be used after the affected turn ends.
Approval receipts describe the decision, not successful tool execution.

Message IDs and payload hashes deduplicate action retries for the lifetime of the
controller. Unconfirmed native requests are not blindly reissued. After a failure,
check whether an action ran before issuing a new request. No global event replay,
workspace index or conversation-summary prompt is injected by this adapter.

## Model/reasoning and terminal

The model picker still uses the native catalog and its supported reasoning levels.
A selected pair is queued for this running chat and visibly marked as next response;
it is passed explicitly in subsequent native turn/start calls. This avoids relying
on thread/resume config overrides that the loaded native thread can ignore. The
pending marker clears only after native acknowledgement of the submitted turn.
The app does not claim a model change took effect merely because a menu closed.
Model/reasoning changes during a busy/native-terminal operation are refused with
instructions to wait. These choices are for this run; no global config is changed.

At idle, the robot can open native `codex --remote ... resume <same-thread>` in the
same terminal. It is not drawn simultaneously with the Android chat. Busy turns or
pending approvals are not interrupted for handoff. Minimize only hides this window. Use Return to Codex chat to return control to Android chat (v1.5.33). This closes only the owned native UI child. An active turn is allowed to finish before the UI child is closed; the app-server, chat history and other sessions remain running.
Changing the native CLI to another chat does not silently redirect this controller
from its original root thread. The application still shows that explicit root chat.

Normal tab/screen changes do not stop the model or the server. The original shell
launcher retains server/status-worker ownership and its cleanup trap. The new
controller closes only its own RPC/socket and captured native-CLI descendant tree;
PID start times are checked before signals. No kill-by-name or shared process-group
termination was added. Old running helpers remain immutable.

## Evidence and remaining device checks

See [VALIDATION-v1.5.32-test.md](VALIDATION-v1.5.32-test.md). Actual Codex0.163.0
protocol was exercised using repository-private CODEX_HOME and a loopback HTTP
fixture model, with no real account/model call. This does not establish phone
keyboard, gestures, login, native TUI handoff or all permission flows. Developer
checking should cover those paths and reopening the existing conversation ID.

Official protocol: https://developers.openai.com/codex/app-server

### v1.5.33 terminal controls

The agent dialog now separates Minimize from Return to [agent] chat. Codex uses
terminal-close for visibility-only dismissal and terminal-exit for explicit
return. Chat notices offer the same explicit return action. Idle native UI exit
leaves the controller/server/history intact; an active turn finishes first.
Claude returns through its existing SDK resume lifecycle only when idle; Gemini
and OpenCode retain their existing terminal-close path. No generic /exit text is
written to agents or shells. Touch in the dialog can open the Android keyboard
even when a TUI consumes the tap for mouse tracking. Phone IME checks are required.
