# OpenCode Android chat — v2.0.8

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

# OpenCode: Android chat (private v1.5.20-test)

This private prototype replaces the always-running mini frontend with an Android
transcript. It applies to newly launched OpenCode sessions only. Existing running
sessions are not replaced. Codex, SSH and ordinary shells keep their existing interfaces and native
permission mechanisms. Claude uses its own native SDK transport with the shared
renderer: see [CLAUDE-ANDROID-CHAT.md](CLAUDE-ANDROID-CHAT.md). Gemini now shares the Android renderer through its own
native CLI adapter: see [GEMINI-ANDROID-CHAT.md](GEMINI-ANDROID-CHAT.md).

## Ownership and protocol

`AiAgents.command` selects `run-chat`. The existing bridge owns one authenticated
OpenCode2.0.28 server, a private0600 Unix socket and the original native session ID.
Native connection roots, session aliases, presets, access rules and project policy
are unchanged. A pinned server version is required. No user-data reset/migration.

`opencode-chat.cjs` projects native paged messages into stable message/part keys.
One authenticated SSE subscription collects current text/reasoning deltas; ended
parts and reconnects are reconciled against native projected history. Events for
other sessions are ignored. Native admission IDs prevent duplicate message sends;
shell admission waits for the native started event or message projection, not for
command completion. An ambiguous shell admission is never blindly re-executed.

`AgentChatView` is an agent-neutral Android row renderer; `AgentChatUi` supplies agent-specific
transport, forms and lifecycle. Text updates retain neighbouring View
objects. Above-bottom scroll uses a stable row anchor, bottom following occurs only
when already at the bottom or after a confirmed send, and touch gestures supersede posted scroll adjustments.
Upper workspace navigation preserves the active chat view; switching to a previously viewed
session restores its bounded presentation immediately, then refreshes native data. Older messages can be requested explicitly.

Android polls the local socket only while this chat is visible (500ms active,
2s idle). Rows are changed only when their content/position changes. Hidden chat
polling and the terminal view's drawing/resize updates are suspended. The owned
server and bounded SSE state continue so completed output remains available.
This is not a measured performance comparison with the old frontend.

## Interface

The existing top card now shows the selected model instead of the connection
name. Native snapshots supply allowlisted model ID/provider/variant fields; until
that is available, the launch profile selection is shown. The connection remains
available in settings. Model changes invalidate the visible cached model label. The transcript starts at
the top of its surface without a Stop row or a redundant working-status label.
While resolving a selected OpenCode session, an empty chat surface covers the PTY
until its interface is known; unrelated terminal sessions remain unchanged. The former packages button
is the same48dp button and position, with the existing vector AI robot. In a
structured OpenCode session it opens the official mini on demand. A clockwise
paint-only border runs only while a known unsupported request needs the terminal
and the chat is visible. The chat also explains that a terminal choice is needed.
The robot is available even before capability/connection resolution; its local
terminal window does not wait for a successful bridge RPC. If the bridge is down,
it shows the session terminal and explains that agent startup/login should be
checked there. OpenCode native mini still needs a working bridge to start.
Other agents/ordinary sessions keep their package button. Long-press also offers OpenCode terminal.

Ordinary responses are selectable text prefixed with `OpenCode:`. Ordinary taps
do not open a duplicate dialog; truncated text can still open its full detail. Reasoning, tools and permission receipts
are compact rounded Android pills. Details open in a scrollable dialog with Back;
large detail text is paged. Reasoning shown by OpenCode can update in its open
window while no text selection is active. This does not expose unavailable or
provider-hidden reasoning. Full Markdown/media/subagent UI is not claimed here.

There are two separate composers:

- The original bottom field always sends a message to the OpenCode model. A request
  such as “list Download” is a model request; native permissions and project rules
  govern the tools it chooses.
- A compact Shell field above it sends an explicit non-interactive command through
  native session.shell. No model inference is requested by this action. Results are
  retained by OpenCode and shown in this chat. Native synthetic shell notices
  are compact inspectable cards; a notice sharing a projected shell ID is not
  duplicated in the same page. This is a user command, not an agent
  tool call: it is not constrained by agent tool permission rules. The label says
  it executes without the model. Shell drafts are separate from the model composer. Shell input and execution
  are disabled unless interactive input is enabled; disabling preserves the draft.

There is no implicit recipient switching. Native permission cards offer once/deny;
full permission bodies can be inspected. Confirmed receipts are anchored to their
tool or associated message rather than replayed at the bottom. Permission is not
execution success. Native security/MCP scope requests stay in the official mini.

String, option, boolean, numeric and basic multiselect forms use Android controls.
Conditional/hidden/external/unknown forms and custom multiselects fall back to the
native interface. Interactive input focuses the separate Shell command field; it does not write
into the hidden headless bridge's PTY. The additional-keys button is disabled in Android chat by the developer's final
decision. Neither enabling interactive input nor tapping Shell reveals keys.
The hidden toolbar's alternate raw-input page is not exposed.

Connection state is rendered inside the transcript request area: Connecting at
startup, Could not connect / retry on transport failure, then replaced with a ready-to-type message for an empty connected chat. Once
messages are present the successful connection notice is hidden. A connection
retry tap only re-reads status; it does not resend commands/prompts. Native API
error rows use shared concise RU/EN guidance with original technical details on
tap. See GEMINI-ANDROID-CHAT.md for shared classification and lifecycle details. It updates in place rather than adding history messages.
Before a connection is available, submitting the main composer keeps its draft
and does not write to the hidden PTY. Late responses remain owner/generation gated.

The optional terminal is an on-demand official OpenCode mini, on the same session.
It uses a smaller window (92% width, 62% screen height), with normal keyboard
input enabled immediately and the existing configured extra-keys view shown
automatically inside it. If the hidden toolbar pager has not created a populated key view,
the dialog creates a fully populated native ExtraKeysView with the selected key
configuration (two-row full-size fallback). Its temporary activity registration
supports modifier reads and is removed on dismissal; an existing toolbar view is
restored rather than replaced. The duplicate compact Esc/Tab/arrow row was removed
in v1.5.17; the configured full-size keys remain. Back to chat is at the left of the
footer; Enter at the right sends carriage return without dismissing the window.
Enter writes only while
its captured TerminalSession is still current and running. Outside taps cannot dismiss it. Only the chat or terminal
view is displayed. Dismissal terminates that mini process,
not the owned server; keyboard configuration, terminal parent and extra-keys
parent/layout/visibility are restored.
Terminal resize replay can still occur inside this fallback. This window is the
OpenCode UI, not a general-purpose replacement PTY for arbitrary nano/ssh programs.
The Shell API is non-interactive; arbitrary TTY programs still need an ordinary
Ubuntu/Termux session. Automatic discovery of all possible terminal prompts is not
claimed. The manual robot button remains available when detection is insufficient.

## Cancellation and storage

The dedicated Stop button was removed in v1.5.14 at the developer's request.
The underlying native interruption protocol remains available: direct-shell
cancellation uses only IDs emitted for this session and rechecks the native
shell metadata.sessionID before removing a running shell. Foreign commands are
rejected. Native removal also removes retained output, so the UI asks before
cancelling a direct shell and explains possible loss of partial output. No global
process-name/pid sweeping is used.

Per-chat permission sidecars retain the existing0600,200-record/2MiB bounded
format. Their commands/paths are not added to scalar status or presets. Chat text
and reasoning remain in native history; bounded live fragments are in memory.
Pending forms are revalidated by session, native ID, canonical body and fingerprint
before reply. Late UI responses cannot replace another session's screen.

The custom jobs2 bundle is no longer packaged or installed by new launches.
Historical source patch and packaging templates remain under patches/;
`scripts/package-opencode-mini.py` now writes only to ignored .verification.
Previously extracted jobs2 files are left untouched; this update does not silently
clean installed private files. Validation: VALIDATION-v1.5.20-test.md.

## Per-session presentation on return (v1.5.20)

Switching between OpenCode/Gemini/ordinary sessions retains a bounded in-memory
presentation for each exact profile + native thread/run + terminal handle. Return
restores rows, model metadata, Shell draft and scroll anchor/bottom position before
the next socket response. Switching upper workspace tabs preserves the same view.
A known chat no longer displays a blank Connecting screen just because it was
selected again. Native connection failures still display a retry notice alongside
retained messages. Restored connection metadata is last known, not a new remote
API availability check; fresh native data remains authoritative.

No hidden-session polling is added. The display cache holds at most8 inactive
presentations, with512Ki serialized characters per entry and2Mi serialized
characters overall, plus small metadata. Eviction/trimming affects temporary
display only, never persisted native history or model context. The cache clears
when the workspace controller closes. It is not a backup and survives neither
app recreation nor process death. No cached pending permission/form controls are
replayed: only a fresh native snapshot can create those buttons.

Resume/new actual Gemini session invalidates old restored rows using the native
session token. Recreated terminal handles/thread IDs use a different cache key.
Late reads/actions retain owner/generation gates. Scroll restoration happens after
layout and yields to touch gestures; it does not animate/replay the transcript.
See VALIDATION-v1.5.20-test.md for automated versus required device evidence.
