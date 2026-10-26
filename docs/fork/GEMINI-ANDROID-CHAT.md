# Gemini Android chat — v2.0.8

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

# Gemini: Android chat (private v1.5.20-test)

New Gemini launches use AgentChatView / AgentChatUi with a Gemini-only transport.
The existing Gemini CLI 0.64.0, native authentication, project, model/mode card,
permissions, shell execution lifecycle and persisted history remain authoritative.
Restart Gemini with the same chat after updating to load the new bridge assets.
No data reset or history-format migration is introduced.

## Native integration

Pinned/hash-checked bundle hooks publish committed React history, pending items,
UI thought summaries and native callback references. Unknown hashes/layouts fall
back to the native terminal. Installed SDK files are not rewritten. gemini-chat.cjs
projects stable message/part keys; it does not parse an ANSI transcript.
The parent forwards allowlisted operations over its private Unix socket and owned
child IPC. Chat text and tools never enter scalar status or launch presets.

Native CLI/Ink remains alive internally. Android hides/suspends its terminal view
in the main chat; this is not a claim that all native rendering work is removed
or that a measured performance improvement has been demonstrated.

The main field invokes native handleFinalSubmit directly. It does not paste into
the terminal or synthesize Enter. Before a model send, an active native shell-input
mode is disabled and its committed update awaited. The separate Shell field
requires interactive input and invokes native handleShellCommand, without asking
the model. Shell requires native idle state and trusted folder. Native tool
permissions remain separate from direct user shell commands.

Admission IDs deduplicate retries. Changed payloads and old native-session tokens
are rejected; a failed/ambiguous callback is not blindly executed again. Native
resume changes invalidate pending Android acknowledgements. Native clear/rewind
removes cached transcript rows that no longer exist.

## Chat and native window

Replies use the Gemini prefix. Tools, reasoning summaries and anchored permission
receipts are compact inspectable pills. Only reasoning exposed by the native UI
is shown; provider-hidden reasoning is unavailable. Existing safe once/deny tool
confirmation cards use the original native confirmation handler and stale-request
checks. Complex dialogs, authentication and unsupported requests use the robot
terminal window. The older bottom permission overlay is hidden only while this
structured chat is showing; legacy sessions retain it.

The robot keeps the former packages button size/location. The window uses the
same dimensions as OpenCode (92% width, 62% height), automatic configured full-size
extra keys, Back to chat on the left and Enter on the right. As of v1.5.18,
when the hidden toolbar has not yet populated those keys, the dialog builds its own
full-size configured panel, with a two-row fallback. Temporary registration for
modifier reads is removed on dismissal; existing key ownership is restored. The duplicate narrow
Esc/Tab/arrow row is removed from both agents. Enter confirms without dismissing;
outside taps cannot dismiss. Closing the Gemini window keeps the same CLI alive.
Keyboard/key owner/parent/layout/visibility are restored on return.

The main chat extra-keys button stays disabled. Interactive input enables Shell;
it does not show another key panel or write into the hidden PTY. Connection and
retry messages appear inside chat; submitting before connection retains drafts.

## Bounds and evidence

Local chat polling only occurs while visible. Projection is lazy, bounded to
500 committed native items plus 25 pending items and about 2MiB of projected body
text. Normal replies use a bounded page; details are paged. UI changes keep stable
rows/scroll anchors. These display limits do not delete native persisted history.

Validation is in VALIDATION-v1.5.20-test.md. Automated JVM/Node/socket tests and an
isolated actual CLI authentication-screen probe do not prove physical IME,
gestures, authenticated generation or real tool permission behavior. Those need
a developer device check. Codex keeps its current interface. Claude now shares the renderer through its
own native SDK transport; see [CLAUDE-ANDROID-CHAT.md](CLAUDE-ANDROID-CHAT.md).

## Connection and error messages (v1.5.19)

An empty connected chat explicitly says it is connected to the local agent session
and invites a message. This does not assert that a remote API request will succeed.
Startup/no-control-connection notices explain the robot terminal and local retry.
Tapping retry checks a snapshot only; it never resends a model prompt/tool action.
An unconfirmed action keeps its draft and explanation across successful polls; it
is not automatically repeated. The notice clears after an acknowledged action or
a change of session. A later native result may still arrive after a lost ACK.

Native error rows display concise RU/EN guidance for region refusal, unavailable
model, quota/billing, authentication/access, context, overload and network errors.
Unknown errors remain explicitly unknown. Tap an error card to inspect its original
native body; raw error metadata is not inserted into presets/scalar status. The
legacy F12/settings hint becomes an explanation of the error detail tap.

The robot appears immediately for selected Gemini/OpenCode sessions, before chat
capability/connection resolution. Its terminal view opens locally without waiting
for terminal-open RPC success. The existing owned bridge control runs asynchronously;
if it is unavailable, a notice explains that the session terminal below can show
startup or login. OpenCode may still need its control bridge to launch its native
mini. Closing queues terminal-close behind the matching open attempt. Owner and
generation checks prevent an old result from updating another window. No reconnect
button restarts a process, grants permissions or blindly retries user actions.

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
