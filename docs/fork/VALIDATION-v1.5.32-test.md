# v1.5.32-test — Codex Android chat

Private APK, not a GitHub release. Package com.termux, code1190 (previous1189),
Android8+/ARM64, same intended signing identity. Library schema8 without clearing
or converting user history. Final identity/ZIP/ABI/hash audit is recorded below.

## Source, test and logical review

The existing app-server launcher/policy/cwd checks are retained. New Ubuntu Codex
runs opt into CodexChatControl with a per-run private marker; already running
sessions and legacy Termux-Codex remain on the old path. Old immutable status/proxy
bridge remains available. The codex-control.js asset changes only its import/
entrypoint footer so a separate new wire-helper copy can export Connection; its
status-worker logic stays unchanged. No global account/config/library reset.

150 focused JVM tests passed with zero failures/errors/skips. Coverage includes
new-run vs old/other-run markers, private helper preparation, launch arguments,
resume ID/original directory, Codex native-thread message/ACK routing, plus existing
chat lifecycle/composer/keys/navigation/status/model/auth and other-agent tests.
An initial new fixture used a Robolectric API absent from the repository version;
it was corrected to that version's RuntimeEnvironment.application.

Node adapter fixtures passed: root-thread filtering, stable streamed/completed
rows, reasoning summary projection, no approval before answer, stale nonce/thread/
turn rejection, questions/options, requested-permissions denial, stable receipts,
retry deduplication, expected-turn steering, native catalog validation, busy-model
refusal, explicit direct Shell and unsupported server requests failing closed.
An owned-process-tree termination fixture verified its descendant stops while an
unrelated child survives; signals are guarded by captured PID start times.
The existing status/limits fixture passed, including Pro weekly-only windows,
account metadata redaction and no inference/permission calls by polling.

Exact installed Codex0.163.0 generated the app-server JSON schema into an ignored
repository directory. A separately launched native app-server with private
CODEX_HOME executed an explicit Shell command in a private fixture project;
its file and command completion reached the adapter. No user process was used.
A second native test used a loopback-only synthetic Responses provider: two real
app-server turns streamed expected assistant rows, history was read, and selected
high effort reached both native HTTP requests. No external model service, real
account or paid inference was used. These are protocol checks, not model quality.

Native tests caught two issues that were corrected: new empty threads have no
materialized history yet, so they start empty without a misleading history error;
loaded thread/resume does not reliably acknowledge reasoning config updates, so
model/reasoning choices are marked pending and passed explicitly to turn/start.
A prototype method name also shadowed a model field; it was renamed before final
native/adapter checks. Native Unix-socket fixture paths could not be connected in
this proot verification location; actual native protocol tests therefore used
stdio. The existing wire/private-Unix status fixture passed; on-device host/guest
chat socket connectivity remains a required developer check.

## Limits and device checks

Official app-server documentation was read. No Codex prompt/schema/model weights,
global SDK/system packages, installed Ubuntu, user sessions or authentication files
were changed. Public reasoning data is displayed only when provided by Codex.
MCP elicitation/secret/custom server requests are not silently accepted; unknown
requests return an error and guide use of native CLI after the turn. Native history
responses are bounded by the existing wire limit and presentation cache; extremely
large unsupported history may require the native terminal. Full unbounded history
or all MCP workflows are not claimed supported in the Android surface.

Native Shell deliberately executes an explicit user command with Ubuntu privileges,
outside the model's sandbox, as Codex's own thread/shellCommand specifies. It is
gated by the existing interactive-input UI and explained in the terminal/status
panel. This does not grant Android root. The agent's own commands retain native
sandbox/approval enforcement. Direct terminal handoff requires idle/no pending
requests; Back hides it, /exit or Ctrl+D ends it. Only the original selected root
chat is owned by the Android controller.

Developer should update, reopen an existing Codex history, send text, test streamed
reasoning/tool rows, allow/deny a harmless prompted action, compare the native
transcript, change model/effort and send a message, switch sessions/screens, open/
close keyboard, use robot/large arrows/Enter, exit full CLI to chat, and close the
new session without affecting another one. Fresh account login, native TUI handoff,
actual Android private-socket access, phone rendering/DPI/selection and all approval
variants are not proven by mocked UI or protocol fixtures. Preserve the previous
APK; a failed device check is not evidence that user history should be cleared.
Vertex/Claude end-to-end checking remains deferred due to account/billing access.

## Artifact

Release assembly and runtime-definition audit passed; 31 optional unresolved
types retain existing explicit review notes. The known Kotlin/UAST/JNA Lint
compatibility warnings were emitted; this is not a clean Lint claim. Final Java
sources match both tested and assembled staging. ZIP integrity, API26, ARM64,
package, increased versionCode and signing certificate passed. Native libraries
and all unrelated assets are byte-identical to v1.5.31. Changed asset:
codex-control.js (import/entrypoint footer only); new asset: codex-chat.cjs.
Private-data filenames/key markers and development artifacts were absent in the
artifact scan; this is not proof against every possible secret.

APK `Termux-Jobs-v1.5.32-test-arm64.apk`, 36662098 bytes.
SHA256 `26c51c92e8c6def4ed026c07d45893b4f97ef5b3d356f742fcad57286bfe941f`.
Certificate SHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Identity permits updating over code1189; actual installation/UI still needs
developer checking. Reproducible fixtures: scripts/tests/test_codex_chat.cjs,
scripts/tests/test_codex_chat_native.cjs and test_codex_status_limits.cjs.
Evidence: `.verification/codex-chat-v1.5.32/`.
