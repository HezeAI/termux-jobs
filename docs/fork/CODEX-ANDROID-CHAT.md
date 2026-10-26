# Codex Android chat — v2.0.8

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

# Agent chat — public v2.0.2

The current release combines private chat builds1326–1330, including the OpenCode
attachment-root correction. Those development labels are archived under
[development/chat-2026-10-23](development/chat-2026-10-23/README.md). They are not
public releases later than2.0.2. Current file input/output, auth and ownership
behavior is described in the v2.0.5/v2.0.6 implementation sections below;
[release validation](VALIDATION-v2.0.2.md) records final evidence and device limits.

The unreleased [agent-chat audit](AUDIT-AGENT-CHATS-2026-10-23.md) covers four
providers and adds recovery/ownership guards in private2.0.3-test. Codex device
login stays pending when its terminal is minimized; explicit Return to the Codex
chat cancels only the owned login attempt. Completion still returns automatically.

Earlier implementation history follows.

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

### Private v1.6.1-test: shell chronology

Consecutive commandExecution rows in one turn are projected into an expandable
Android card, enabled only for Codex. Ordinary messages/reasoning/receipts/errors
remain ordered barriers. Each native item stays one step; compound commands are
not split into invented subtasks. Status/exit and optional duration are typed native
metadata. No summarization model, inference or extra native request is added.
The card keeps expansion while steps update, and each step opens the original
detail/output. RU/EN captions; raw commands/output unchanged. Old live helpers
can group existing Shell rows but do not gain nonexistent timing/exit metadata.
See VALIDATION-v1.6.1-test.md. Stable GitHub latest remains v1.6.0.

### Private v1.6.2-test: large-history startup

History pages now contain one native turn instead of eight. The chat connection
opts into a bounded 32 MiB wire message limit; status and legacy clients retain
2 MiB. The Android snapshot/detail/cache bounds stay unchanged. A connection
failure is emitted once and prevents startup from marking a disconnected chat
ready. Size/timeout causes use fixed diagnostic text without raw server content.

A copied developer history reproduced an eight-turn response above 2 MiB;
one turn fit the previous limit. New launches use the fixed helper. Reopen
the same history after updating; no history reset or schema migration is needed.
See VALIDATION-v1.6.2-test.md for automated evidence and pending phone checks.

### Private v1.6.3-test: compact action and skill clouds

Timeline groups hide the redundant completed summary and name command failures
explicitly. Codex-only RU/EN names describe image viewing/generation/web search.
Explicit native skill input or a conservatively recognized literal SKILL.md read
starts a skill context for subsequent same-turn tools. Clouds show that context,
not proof the skill caused every tool call or completed successfully. Text and
permission receipt boundaries are preserved; unsupported read forms are not guessed.
No model instructions, native execution, rights or quota behavior changes. Large
history/disconnect fixes from1.6.2 are retained. See VALIDATION-v1.6.3-test.md.


## Shared action-card presentation (v1.6.5)

OpenCode, Gemini and Claude now reuse the compact expandable action card. This
is a view projection, not shared agent execution or policy. Codex retains its
existing timeline and history-wire behavior. Others group consecutive tool rows;
user/text/reasoning/receipt/error boundaries and original message/part identities
remain intact. Native success/error/executing states are localized, unknown
states remain unknown. Claude tool-use status is associated with its exact
native tool_result ID; an unresolved use is not assumed complete. Direct Claude
Shell carries its actual command/status/exit code rather than output as command.

Successful structured Read/read/read_file with an absolute */SKILL.md path can
identify a skill name from its parent and context for following tools until a
new user/turn/read. It describes chronology rather than causal skill execution;
failed/relative/dynamic unsupported reads are not attributed. Gemini's current
history projection supplies name/status/description but no verified file argument,
so it receives no guessed skill context. No file inspection/inference is added.

Streaming viewport-follow uses scrollTo, not fullScroll(FOCUS_DOWN), because the
latter moves keyboard focus into selectable transcript text. Neither composer
text nor selection/composition is reset for transcript changes. This view path
is shared by Codex, Gemini, Claude and OpenCode. Native-terminal stale startup
text and mouse-mode escape echo are separately deferred, not fixed by this UI.

## v2.0.2: file and image exchange in the Codex chat (2026-10-23)

Implemented for the updated Codex helper; phone verification remains pending.
Support both directions in the existing Android chat:

- Display assistant-generated images as inline previews. Tapping a preview opens
  a larger viewer with zoom and an explicit Save to Download action.
- Let the user choose an image from phone storage through the app's file manager,
  preview or remove the attachment before sending, and send it with optional text
  as native image input to the active Codex conversation.
- Place a vector folder button at the left of the existing Shell row, replacing
  the word Shell. Reduce the command field's width to accommodate it and retain
  the shell send control. The folder opens the app's file manager for image
  attachment selection; the selected image belongs to the model-message composer
  and is never submitted as a direct shell command. Attachment selection must
  remain available independently of the interactive shell-input toggle.
- Preserve image/message association when loading history and reopening the chat.
  An attachment must reach the agent as image input, rather than only a filename
  or text placeholder. Show a clear failure if loading or sending is unsupported.

The picker first offers Environments or Phone storage. Environments lists only
installed local roots; storage permission is requested only after choosing Phone
storage. File preparation and thumbnail decoding run off the UI thread. At most
12 thumbnail targets retain decoded images; previews use sampled decoding and a
64 MiB disk cache for images fetched from the agent environment.

Up to4 attachments per message: PNG/JPEG/WebP/GIF images up to20 MiB; supported
UTF-8 text/log files and Android-decodable video up to256 MiB. Originals are copied
into private `.codex-control/chat-attachments/<id>/`; the existing control-directory
bind exposes these at `/tmp/termux-jobs-codex/chat-attachments/` in the selected
Ubuntu. Source files remain unchanged. Text/log input includes an explicitly
labelled first8 KiB sample and the complete file path for further reading. Video
input contains up to12 evenly sampled frames with timestamps, no audio; brief
between-frame events may be missed. Playback in the viewer uses Android codecs.
The viewer offers a bounded text preview, image zoom, video playback and explicit
non-overwriting Save to Download. Sending an attachment without text is allowed.

Native `localImage` inputs and generated/tool image outputs are projected into
stable attachment rows, without putting image base64 in Android snapshots. Remote
image URLs are not fetched automatically. Unsent removed drafts are deleted;
files involved in a submitted request are retained even if its acknowledgement
is lost, so retry/history/model reads cannot lose their underlying files. These
private copies consume storage independently of the originals. Missing historical
source images show an unavailable message instead of a successful preview.

The helper checks staged manifests, file paths, symlinks, thread ownership and
attachment count. Selected files are model inputs, never Shell commands. Native
agent permissions remain unchanged. Existing running helpers are not hot-reloaded:
restart the selected session with its saved chat after installing the update.
Other agents retain their existing protocol and do not expose this picker yet.
Installed Codex0.163.0 schema and a real loopback-only app-server fixture confirm
native image input and history projection; Android permissions, zoom, video codecs,
keyboard/focus and switching still require developer checks on a phone.


### v2.0.3 media presentation

The shared local file browser now supplies viewport-only photo/video thumbnails,
with a video badge. Memory cache is bounded to6 MiB per open browser and the shared
thumbnail JPEG cache to16 MiB; proot originals are temporary and deleted after
reading. File-list format2 adds modification time for cache identity; the parser
still accepts the original triple-field format. Reads use a separate ProjectFiles
instance so thumbnail work cannot occupy the browser's listing/transfer reader.
Generation guards reject late results after directory/filter changes or close.
A failed thumbnail leaves file selection available. No background preview is
added for SSH/server listings. Image/video chat viewers fit their aspect ratio
with screen bounds instead of always requesting78 percent of screen height.

### Local APK output cards (v2.0.3)

Explicit local Markdown links such as `[APK](/absolute/build.apk)` (also
`[APK](</absolute/path with (spaces).apk>)`) in Codex assistant messages become
APK cards. The helper only stats regular files, up to256 MiB and four per message;
missing files, symlink files and remote URLs do not become APK cards. It never
reads/hashes the APK on the polling thread. Metadata is cached by native item.
Android copies the artifact through the selected environment on background workers
and shows filename, size and the private copy's full SHA-256 beside an APK badge.
Private artifact cache is bounded to512 MiB, separate from image previews.
Opening a card shows the same metadata and explicit Install / Save to Download.
No installer or Android settings activity starts just by receiving/opening a card.

Installation shares only that cached file through a read-only FileProvider URI;
it does not expose arbitrary private app files. If Android's install-source
permission is missing, an explanation offers Android settings. Return and press
Install again; Android's package installer owns final consent and package checks.
The checksum identifies the file and is not a publisher/signature trust claim.
This does not add APKs to the outgoing image/log/video picker, remote downloads,
background installation or a new agent execution path. Active helpers need a
Codex session restart with the saved chat after updating.

In v2.0.4 APK cards open on tap without the Open/install hint. Their menu uses
To Download on the left and Install on the right. It has no Close button;
tap outside or use Android Back to dismiss. Other media viewers keep their actions.


## v2.0.5: shared attachments and native authentication

This section supersedes the Codex-only attachment scope above. Agent execution,
permission policies, session identity and history stores remain separate.

- Android stages picked originals under each agent's private control directory;
  the already-existing per-agent bind exposes the files in that selected Ubuntu.
  A manifest and atomic claim bind each staged ID to its native conversation.
  Path traversal, symlink, foreign-thread and Shell attachment requests are refused.
- Shared agent-attachments.cjs projects valid markers to cards and stat-checks
  explicit local Markdown output links. Original text/log samples are bounded to
  8KiB with the full path retained. Binary/APK originals are path references.
  Images are sent through native Claude multipart blocks, OpenCode local file URIs
  and Gemini @ references; sampled video frames include timestamps and no audio.
  Other agents use a bounded JPEG model copy for picked images, preserving originals.
  Native CLI permission checks are not bypassed.
- Dedup keys include native session, kind, text and attachment references. Failed
  or ambiguous acknowledgements retain drafts; submitted copies remain for history.
  The outgoing folder flow is distinct from the explicit Shell composer.
- Codex initializes its existing app-server and reads native account status before
  creating/resuming a thread. Missing auth exposes a chat action. Explicit terminal
  opening starts chatgptDeviceCode; URL/code print only there. Only the owned login
  ID can complete/cancel this flow. Account-free providers retain their native path.
  Login completion reconnects the chat and closes only the dedicated login terminal.
- Gemini stored keys use a separate AES-GCM Android Keystore alias and profile AAD,
  with ciphertext in the optional schema8 geminiEncryptedKey field. Duplicates and
  imports discard that field. The masked edit is not saved as Android view state.
  A private0700 directory/0600 one-time file passes a selected key to the owned
  controller; it is consumed/unlinked before spawning the CLI, then held in child
  environment only. No key enters shell arguments, settings/status JSON or chat.
  Bootstrap/setup/proot failures have scoped cleanup; synchronous failed launches
  discard the file. An abrupt kill before consumption can leave a private handoff
  until the next same-profile launch. Backups of runtime/private app data must still
  be treated as sensitive. This does not hide a key from tools with equivalent app
  or agent-environment access. Key replacement does not restart a running agent.

Older running helpers do not gain new attachment support until session restart.
No X11/native-renderer libraries, server tunneling, global auth/configuration or
unrelated shell-session lifecycle changes are included.


### v2.0.6 OpenCode integration correction

Android's OpenCode stage is beside its control socket, while receipts are below
connections/<id>/receipts/. The production controller now supplies the former root
explicitly. The v2.0.5 isolated adapter test had co-located those directories and did
not cover this boundary. A production Unix-controller fixture now uses the nested
layout and checks file-only/mixed sends, dedup, rejection/recovery and bounded media
HTTP replies. Prompt/inbox/history may contain native base64 and use a40MiB cap;
other HTTP routes retain2MiB. The projected Android chat stays bounded and does not
receive raw media bodies. Existing saved chats and receipt locations are preserved.
