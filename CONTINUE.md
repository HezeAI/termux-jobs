## Published v1.6.5 fix /1198

Stable/latest: https://github.com/HezeAI/termux-jobs/releases/tag/v1.6.5
Source/tag commit: e8bdf258064b1722054e1df1dd8dc23e044b7437. Previous public1.6.0/code1192.
APK: Termux-Jobs-v1.6.5-arm64.apk, 36669474 bytes,
SHA-256 eabb1e69bdf4c99e8a560fc6576a2da254838529e13a08e36cea4bf00e8c4d51.
Same com.termux/signer/Android8+/ARM64, schema8/server/backup formats unchanged.
No reset from1.6.0/private1.6.x; old pre1.5.3 local-AI cleanup warning remains in
RU/EN update highlights and release notes. Final update.json schema1/code1198
is above developer's private1.6.5-test/code1197.

Developer confirmed input focus, other-agent cards and OpenCode component check
on private builds. Stable assets/native libraries exactly match that private
build. Full644 JVM tests,119 Python tests and20 Node/native fixture suites pass;
release runtime/ZIP/identity/version/signature/SHA and source credential scan pass.
All four uploaded files verified against GitHub digest/size; all four downloaded
back and matched local bytes/hash. Anonymous latest and updater endpoint verified;
public tagged documentation checked. Full Lint clean and real-account Claude/
Vertex completion are not claimed. Known Codex native terminal stale startup text/
mouse escape echo is deferred at user's request. No GitHub feedback service or
local AI restoration added. No working user environment/session was changed.

Evidence: .verification/release-v1.6.5/. Stable APK/checksum also in Download.
Always use --repo HezeAI/termux-jobs with gh because upstream Termux can otherwise
be inferred as default. Remaining design/ PNG is an untracked concept, not release
source; it was deliberately not published or removed. Subsequent source must use
versionCode>1198 for compatible forward updates. Do not move the published tag.

## Historical private checkpoints

## Current private v1.6.5-test /1197

Developer confirmed1.6.4 composer-focus fix and action cards in other agents.
Reports component check always says missing opencode-cli, including presets,
creation and manual environment check; relaunch can run OpenCode.
Found and reproduced inventory/launch path mismatch: bootstrap prepends private
agents/bin, private Node/bin and agents/node_modules/.bin; inventory did not.
Added exactly those paths inside the inventory subshell for OpenCode only.
Isolated fixture before fix falsely reports missing despite compatible old npm
layout; after fix direct/npm/private Node succeed, wrong pinned version and
really missing CLI still fail. PATH does not escape check subshell, no installs.
Actual installed guest runtime was not visible at standard local paths in this
session; developer's particular installation cause still needs device confirmation.
Do not claim every possible missing-component cause fixed. Core pinned CLI itself
reports expected version twice in isolated copies. Current private build only;
public stable still1.6.0. No native terminal deferred issue changes.

## Current private v1.6.4-test /1196

Shared action-card presentation enabled for OpenCode/Gemini/Claude; Codex keeps
its existing grouping/history fix. Existing Codex* card class names are internal;
providers project typed toolName/status/path/command metadata without changing
execution, approvals, prompts or lifecycle. Generic grouping stops at transcript
barriers; explicit successful structured Read of absolute */SKILL.md gives skill
chronological context, never proof of causal application. Gemini only exposes
native name/status, so no inferred skill path there. Unknown tool/status preserved.

Urgent developer report: streamed cards/answers steal composer focus, not text.
Reproduced by regression test before fix. AgentChatView.bottomAfterLayout used
ScrollView.fullScroll(FOCUS_DOWN), which selects a focusable transcript child.
Changed to scrollTo(bottom); regression now preserves composer focus, draft,
selection and IME composing span. Shared by all four native agent chats.
Terminal stale component-check text/mouse tracking escape echo remains deferred
at user's request; no terminal/process changes were made for this task.

Focused143 JVM tests pass; final labels/card targeted checks and APK verification
recorded in docs/fork/VALIDATION-v1.6.4-test.md and .verification/agent-cards-v1.6.4.
No GitHub publication authorized for this private build. Public latest1.6.0.

# Latest stable release: v1.6.0

## Private v1.6.3-test/code1195 — compact Codex skill/action clouds delivered

Keeps1.6.2 one-turn/32MiB-history wire/disconnect fix (codex-control.js byte-identical).
Compact successful timeline (summary hidden); explicit failed-command/action text,
no generic app-error heading. Codex-only RU/EN friendly image view/generation/web
labels. Unknown tool names/details preserved.

Skill clouds: arbitrary explicit native skill inputs or conservative literal
single-file cat of absolute SKILL.md (bash/sh wrapper/quoted path/-- supported).
No interpolation/compound/echo guesses, no extra file read/execution. Attached or
reading is not successful application. Subsequent same-turn tools carry context
until new user/new turn/new skill; failed reads do not propagate it. This is
chronological context, not proof of causality. Text/receipt/reasoning barriers
stay ordered. Cloud expands to steps and original details, collapsed content width.
Native generic tool status follows events/history; phase annotations excluded from
raw JSON. Other agents, prompts/rights/native execution unchanged. No new requests.

138 focused JVM tests + adapter/native loopback/history-wire fixtures pass.
Final build/runtime/pkg/ZIP/ABI/cert/privacy/source audit passes. Existing Lint
warnings remain. APK Termux-Jobs-v1.6.3-test-arm64.apk,36667970bytes,
SHA256 4bdea959c553380188bb1a97c0f0a30fb163478de082d6a9b8c8606d303399d1.
Download copy/hash verified; relaunch same history for new metadata. Phone checks
of cloud/gestures/DPI still needed. No installation/publication/runtime changes.
Latest stable1.6.0; next stable code must exceed1195. See
VALIDATION-v1.6.3-test.md and .verification/codex-skills-v1.6.3/.

## Private v1.6.2-test/code1194 — large-history startup fix

Developer screenshots showed Codex disconnected after1.6.1. Read-only snapshot
confirmed failed history load. Isolated copied-history native reproduction:
eight turns2,898,482bytes > old2MiB limit; one turn142,233bytes. Copy removed.
History now pages one turn; chat-only32MiB wire bound, idempotent failures,
no false ready state after disconnect. No user helper/session/environment edits.
Wire/adapter/native-loopback/permissions/status fixtures pass, signed isolated
build and runtime/ZIP/pkg/version/ABI/cert/privacy/source checks pass.
Same com.termux/certificate/schema8, code1194 exceeds1193. APK/checksum in
Download, destination verified. SHA256 dab6c2453074500a01195c455b05cc7cf7b0bb6952920fd63c8d80cc369e4c49.
Class definitions/resources/libs match1.6.1; generated version fields updated;
grouping retained. Phone reconnect/send/
Shell/history/card checks pending. See VALIDATION-v1.6.2-test.md.
Latest stable remains1.6.0; future stable must exceed1194.

## Private v1.6.1-test/code1193 — Codex shell chronology delivered

User requested grouped Shell actions for Codex only, bilingual RU/EN. Consecutive
same-turn commandExecution rows -> one collapsed/expandable card, current command,
native state/exit/optional duration, original detail/output per step. Text, user,
reasoning, receipts/errors remain ordered barriers. Compound command stays one
step. No new model call, protocol, permission or execution behavior. Other agents
keep existing presentation. Shared renderer map now holds View (TextView/card),
with existing stable row/scroll anchoring; expansion survives native updates.

135 focused JVM tests + Codex adapter/native loopback fixture pass. Final assembly,
runtime/ZIP/pkg/ABI/cert/privacy/source/staging audit passes. Native libs and all
assets except codex-chat.cjs identical to1.6.0. Existing Lint warnings remain.
APK Termux-Jobs-v1.6.1-test-arm64.apk,36665742bytes, SHA256
70a106060dda294f8d7cc1dd0906cd44154f39eb07fe804c3c645b9c8f5f8d89.
Download copy/checksum verified; relaunch same history for typed metadata. Old
helper rows can already group through Shell title (no invented time/exit fields).
Device card interaction/gestures still need developer check. No install/GitHub
publication/user-process changes. Latest stable remains1.6.0/code1192; a future
stable update must exceed1193. See VALIDATION-v1.6.1-test.md and
.verification/codex-timeline-v1.6.1/. The PNG concepts remain untracked previews.

## Stable v1.6.0/code1192 — published and verified

User explicitly authorized publication of current agreed work; anonymous feedback
relay/API/Issues service was discussed then deferred. No feedback component is
included, no server deployment/token setup authorized as part of this release.

Compared with public1.5.3: Android agent chats for Codex/Gemini/Claude/OpenCode,
robot terminal with large keys/Enter, separate Minimize/Return and IME fix;
capability/model/permission/thinking cards, accurate weekly-only Codex quota;
confirmed missing-component environment setup and guided Gemini Vertex;
session file copy/move/select-all and shared server selection/navigation actions.
Existing SSH/server-agent/media/backup behavior remains as documented.

com.termux, schema8, server store unchanged, signer retained, Android8+/ARM64.
APK Termux-Jobs-v1.6.0-arm64.apk,36663894bytes; SHA256
2df1b376677e98e2f7c4da4dcda00229f7c9ae33846648fbe2ae858e1ba87cf3.
All APK assets/native libraries byte-identical to developer-tested1.5.33-test.
630 full JVM tests + additional24 WorkspaceStore tests including new optional-field
compatibility case,119 Python tests,19 Node fixture suites pass. Real Codex/Claude
CLI tested with loopback synthetic provider, no real cloud accounts/inference.
APK/runtime/pkg/ABI/certificate/privacy/source/docs/update-schema audits pass.
Existing Lint warnings remain. Developer confirmed Codex chat/keyboard/minimize/
return; exhaustive Android/upgrade/Claude/Vertex account checks are not claimed.

Localized update notes explicitly state no general reset from1.5.3, new UI on
next launch, active Android processes not guaranteed through install and retained
local-AI removal behavior for upgrades from before1.5.3. Older private version
names were reused; old/public and later report text preserved separately.
See RELEASE-v1.6.0.md, VALIDATION-v1.6.0.md, .verification/release-v1.6.0/.

Published https://github.com/HezeAI/termux-jobs/releases/tag/v1.6.0 as stable/latest,
source commit3e1632b621bba1e7b450913ebe888147194bb97d (annotated tag unchanged).
Four release assets downloaded and byte-compared: APK/SHA256SUMS/update.json/notes.
Unauthenticated releases/latest reports1.6.0, update.json/checksums and public docs
fetch successfully. Updater schema1/code1192/hash/size/compatibility match the APK.
Release body matches reviewed notes. Same APK/checksum copied to Download.
No anonymous-feedback service, no user app installation or server process changes.
Keep the existing untracked design/ image; it was not part of the release.

## Private v1.5.33-test/code1191 — agent terminal controls delivered

APK `Termux-Jobs-v1.5.33-test-arm64.apk` (36663950 bytes), SHA256
`d5c85e59dc2a85c99b8182bf577e697c955750ea599a6fe60a0a2d709dd45a24`. Download copy/checksum verified.
Same com.termux certificate, Android8+/ARM64, no schema/data clearing.

Developer resumed this Codex chat successfully in1.5.32, then found robot CLI
retained control after hiding and tapping its composer did not open IME.
1.5.33 adds Minimize plus Return to [agent] chat. Codex terminal-close is
visibility-only; explicit terminal-exit closes its owned native UI tree at idle,
or after its active turn completes. Server/RPC/history remain running. Claude
explicit exit uses existing SDK resume lifecycle, refuses while nativeBusy.
Gemini/OpenCode reuse existing terminal-close. Chat provides terminal guidance
and explicit Return button; no generic /exit text is injected.
Dialog IME flags cleared; mouse-tracking touch callback opens IME only in
reparented terminal dialogs. Main terminals keep their old path.

151 JVM tests, Node Codex/Claude adapter and Claude lifecycle, existing Codex quota
fixtures pass. Final runtime/ZIP/package/version/ABI/certificate/privacy/assets
and build staging audits pass. Existing Lint warnings remain. Device keyboard,
Minimize/Return, active turn and subsequent send need developer verification.
See VALIDATION-v1.5.33-test.md and .verification/codex-chat-v1.5.33.
Do not publish/install without instructions. Future stable code must exceed1191.

## Private v1.5.32-test/code1190 — Codex Android chat delivered

APK `Termux-Jobs-v1.5.32-test-arm64.apk` (36662098 bytes), SHA256
`26c51c92e8c6def4ed026c07d45893b4f97ef5b3d356f742fcad57286bfe941f`. Download APK/checksum copy verified.
com.termux/certificate retained, Android8+/ARM64, library schema8/no reset.
No GitHub publication, APK installation or modification of the running user chat.

New Ubuntu Codex runs opt into shared AgentChatUi/View via CodexChatControl private
per-run marker. Old running sessions and legacy Termux-Codex keep old presentation.
Same native app-server/history/policy/cwd; no added prompt, account or config edits.
Reopen same chat ID after updating to see normal transcript and expandable native
reasoning/tools, approvals/forms, robot compact terminal with native large keys.
New controller codex-chat.cjs uses a separate immutable wire copy exported from the
existing codex-control.js asset; old bridge0.2.21 remains unchanged on device.

Messages use turn/start or expected-turn turn/steer. Explicit interactive Shell
uses native thread/shellCommand (Ubuntu user privileges, outside agent sandbox),
explained in UI. Model/reasoning are native-catalog-validated choices, marked as
next response and passed explicitly to subsequent turn/start. Loaded thread/resume
ignored reasoning config in the native probe, so it is not used to fake a model
change ACK. Pending clears on accepted native turn. Status/account limits stay on
old read-only worker; new card overlays current-run choice/pending state only.

Approvals require current native thread/turn/request nonce, allow once/deny only;
permission deny grants empty subset. Structured non-secret questions supported.
Unknown/secret/MCP/custom requests fail closed with guidance to native CLI after
turn. Dedup never blindly repeats an unconfirmed request. Stable receipts attach
to original tool items. History pages8 turns, bounded wire/cache; huge/unsupported
history may need native terminal. New unmaterialized chats start empty normally.
Native full CLI handoff requires idle, /exit or Ctrl+D resumes chat; Back hides only.
Controller closes only owned captured PID/start-time descendants, no broad kill.

150 JVM tests pass, adapter/status fixtures pass. Exact Codex0.163.0 generated
schemas; isolated native stdio app-server executed Shell and two turns via a
loopback synthetic model with correct streaming/history/high effort on both requests.
No real account/model API used. Native Unix socket verification path was inaccessible
in this proot location; existing private-Unix wire fixture passes, actual phone
chat socket/UI/login/TUI handoff still require developer checks. Source/build/pkg/
ZIP/ABI/cert/privacy/runtime audits pass; existing Lint warnings, not clean Lint.
See CODEX-ANDROID-CHAT.md, VALIDATION-v1.5.32-test.md and .verification/codex-chat-v1.5.32.
Future stable release code must exceed1190. Preserve prior1.5.31 APK.

## Developer checking: Vertex and Claude deferred

Developer reached native Google Cloud browser sign-in and Vertex API responses:
first SERVICE_DISABLED, then BILLING_DISABLED after API enablement. Cloud Console
billing setup then failed with OR_BACR2_59 (reported screenshot). The exact reason
for that Cloud billing failure is unknown. No successful Vertex model response was
verified. User explicitly chose to leave Vertex and Claude available as implemented
and defer their end-to-end checks because account/billing access prevents testing.
Do not describe either as fully device-validated in a future release. Do not
change billing/accounts or remove/disable these integrations on that basis.

## Private v1.5.31-test/code1189 — Vertex guided project setup

Delivered `Termux-Jobs-v1.5.31-test-arm64.apk`, 36651111 bytes, SHA256
`eededc180a9dedc11cb1fd42efb72dd1bed78745f7947267a070adddbca9efc6`. Download copy verified. Package/cert unchanged,
API26/ARM64, no library schema/data reset. No GitHub publication or APK install.

User requested connection-level project/location reuse, NOT saving the chat or
creating a preset. New VertexConnection keeps non-secret application-private
Vertex defaults; selected Vertex with blank project inherits, explicit profile
project wins, API/other agents do not load these settings. Editor nonempty Vertex
metadata also updates defaults. These are app-wide, not per-Google-account.

After existing ADC/bootstrap, new vertex-project.sh requests missing project
through per-run private Gemini UUID request/response files. AgentChatUi's existing
poller shows a localized chat notice and ID/location dialog (hides mini-terminal).
Save and continue validates, remembers defaults, atomically answers only current
waiting run. No eval/source of response, no OAuth code in defaults. Bounded15min
shell wait; Express key or existing project bypasses it. Gemini command no longer
overrides newly selected metadata with old blank/global values. Back leaves
startup waiting and chat notice reopens prompt. Stale/changed run callbacks are
refused. No changes to agent permissions, native bridges or process shutdown.

133 JVM tests pass plus isolated shell handshake/injection fixtures and pinned
native Gemini Vertex auth regression. Final assembly/runtime/ZIP/pkg/ABI/cert/
privacy/source-vs-test/build audits pass. Prior assets and native libs unchanged;
one new shell asset. Existing Lint Kotlin/UAST/JNA warnings, not clean Lint.
Real Cloud auth/project/IAM/model access and Android UI require developer checks:
update then restart Gemini Vertex once, fill project, Save and continue, check
next launch reuses metadata. Existing valid ADC should avoid another browser login.
See docs/fork/VALIDATION-v1.5.31-test.md and AI-AGENTS.md. Future stable code>1189.

## Private v1.5.30-test/code1188 — prepared for developer checking

Delivered `Termux-Jobs-v1.5.30-test-arm64.apk` (36646157 bytes), SHA256
`ce5a143cca15a45501aef073c8323d021adf6d200757f7b706315b58a4980545`. Download copy checksum verified. Package/certificate retained;
code1188 exceeds last private1187. Android8+/ARM64, schema8, no data reset.
No GitHub publication or APK installation in this task.

Manual environment check row follows environment selection in AI agent session/
preset editors; right vector checkmark means found components, not valid account
or quota. Agent/auth/cloud changes clear it. Launch checks again and offers explicit
missing-only setup. Transient consent is neither copied nor serialized. Standard
Termux/Ubuntu/SSH and legacy Codex-Termux bypass setup. Existing Ubuntu/global
Codex/credentials are preserved; new CLI tools are private. Missing base APT packages
are shared in Ubuntu. Vertex can install gcloud589/Python and open native ADC
browser sign-in; project/IAM/APIs/billing remain manual. Common region selector
plus custom location; stale Google-free hint removed.

98 JVM tests pass, isolated shell and Vertex native fixtures pass, owned PTY-close
probe leaves no fixture worker/child. Final build/runtime/ZIP/pkg/ABI/cert/privacy/
source-vs-test/build staging audits pass. Existing native libs/all previous assets
byte-identical; four new setup assets only. Known Lint Kotlin/UAST/JNA warnings,
not clean Lint. Real Android clean-install/preparation interruption, UI/checkmark,
Cloud browser sign-in and real model requests still need developer checking.
Details: docs/fork/VALIDATION-v1.5.30-test.md and AI-AGENTS.md.

Pinned new references in ignored .tools/downloads/agent-setup; not bundled.
Codex0.163.0 npm integrity and gcloud589 embedded VERSION verified (downloads.json).
Future stable release must exceed1188 and include updater assets/docs per AGENTS.
After user checks, next requested work is Codex chat redesign; not started.

## Private Gemini authentication update: v1.5.29-test (code1187)

Delivered Termux-Jobs-v1.5.29-test-arm64.apk, 36628736 bytes,
SHA256 9d96a0086f9f8c2666420c7366569d977b0d46fbdb388b79deaad76def02ff0e. Download copy verified.
Google sign-in dimmed/disabled with current-client explanation; new profiles use
Gemini API. Choose in terminal replaced with Vertex AI; old auto/Google preserved.
Vertex has separate cloud project/location fields, scoped exports and enforced
native vertex-ai. Needs existing Cloud ADC/service-account or Express key;
no auth credentials/system packages installed, no real Cloud request made.
89 JVM tests, Node auth/live regressions and exact pinned native auth/generator
fixtures pass. Build/runtime/package/signature/privacy/asset/source checks pass.
Only Gemini controller asset changed from1.5.28; unrelated assets/libs identical.
Same package/cert, schema8, no data reset. New vertex enum is not guaranteed
readable by older builds: switch Vertex presets to API and back up before downgrade.
Device/provider checks pending; no installation/GitHub publication. Next stable
code must exceed1187. See VALIDATION-v1.5.29-test.md, AI-AGENTS.md and evidence
.verification/gemini-auth-v1.5.29. Codex chat redesign remains after user checks.

## Private agent status update: v1.5.28-test (code1186)

Delivered Termux-Jobs-v1.5.28-test-arm64.apk, 36626000 bytes, SHA256
67bee5f46fd9618728da9d41d2641cbf4d47cfc9fdf0fd4bb7d173bb0d431015.
Download copy/checksum verified. Shared agent card presentation; OpenCode live
rights/Build-Plan with ACK+revision+idle guards and same scope; Claude native effort
catalog/apply_flag_settings; Gemini main-chat thinkingConfig with model-specific
levels/budgets. Choices persist after ACK. No Codex rights switch, no chat rewrite.
126 focused JVM tests and14 Node suites pass; real pinned Claude loopback/native
OpenCode no-inference probes pass; build/runtime/ZIP/package/version/signature/
privacy/source staging checks pass. Unrelated assets/native libraries unchanged
from1.5.23. Device/provider checks pending. User will check then requests Codex chat
redesign. No GitHub publication or install authorized. Schema8/default optional
fields; same package/cert; next stable code must exceed1186. Historical private
1.5.24–27 reports/artifacts are preserved. See VALIDATION-v1.5.28-test.md,
AI-AGENTS.md and .verification/agent-cards-v1.5.24 evidence.

## Private Select all update: v1.5.23-test (code1185)

Delivered `Termux-Jobs-v1.5.23-test-arm64.apk`, 36615662 bytes, SHA256
`e5f8c94e3de5d1aa76948fdf90c61a46cf2aeb8df54dd384d7d68889229a3ff2`; Download copy verified. Same package/certificate; increased code,
no reset/schema migration. Select all appears only during active selection.
Session browser: left footer opposite Close, applies current type/search filter,
retains selections from other folders, capped50 with notice; does not dismiss.
Pickers retain their existing neutral buttons. Server: vector Select all and × N
share the transfer row to the right; arrows narrow. Separate slots/8dp spacing,
no overlap measured at296dp usable width with enlarged count text and at least48dp
arrow targets. Adds current displayed folder entries to the same downloadSelection
map used by download/move/delete; retains earlier folder entries, cap500 with notice.
Prepared move disables all action until completed/cancelled. Controls hide at zero.
No new filesystem operation or poller; transfers/policy/agent adapters unchanged.

136 targeted JVM tests pass, including filtered and limit selection, visibility,
no-dismiss footer, row geometry and all-selected download validation. Build/runtime/
ZIP/package/ABI/signature/privacy/staged-source/resource checks pass. ALL packaged
assets/native libraries identical to1.5.22. Known Lint tooling warnings remain.
Device geometry/gesture and real transfer checks remain developer testing. No app
installation/GitHub publication. Next stable code must exceed1185.
See FILE-SELECTION.md, VALIDATION-v1.5.23-test.md and
.verification/select-all-v1.5.23 evidence.

## Previous private file browser update: v1.5.22-test (code1184)

Delivered `Termux-Jobs-v1.5.22-test-arm64.apk`, 36614894 bytes, SHA256
`797e5e3c9e5ed1f420e18ea325bcbecb20609d0d5ac0058c6069a1c06859d734`; Download copy verified. Same package/certificate, increased code.
No reset/schema migration. User decided NOT to add cross-environment destinations.
Session browser: left parent vector, destination arrow below Home/search, long press
and independent checkboxes, navigation/filter-persistent selection, × N clear button.
Row taps still open folders/file menus. Copy here / Move here / Back and confirmation
menus reject outside dismissal. Whole-batch path preflight, staged copies, native
no-clobber moves, confirmed conflict suffixes retaining extensions;50 selections.
Self-descendant/overlap/missing paths rejected. Completed batches are not rolled back
on interruption; no automatic retry. Browser dismissal disabled during mutation.

Server: same row/checkbox separation, persistent same-profile navigation selection,
× N also clears prepared move. Unchecking last entry ends delete selection and its
outlines; × N hidden at zero. Empty trash-initiated mode can still be toggled off by
trash. Multiple-source-parent deletion groups keep existing safe server validation,
explicit full-path confirmation and partial/error reporting. SFTP/download/move,
agent/server assets, monitoring and AI transports remain unchanged.
131 focused JVM tests pass, including real shell fixtures and UI/adjacent server/
chat regressions. Build/runtime/ZIP/package/ABI/signature/privacy/staged-source
checks pass. ALL existing APK assets/native libraries identical to1.5.21; Claude/
shared agent integration source hashes unchanged. Existing Lint tooling warnings.
Phone geometry/gestures/filesystem and real-server checks remain developer tests;
no actual server action, app installation or GitHub publication. Next stable code
must exceed1184. See FILE-SELECTION.md and VALIDATION-v1.5.22-test.md.
Evidence .verification/file-actions-v1.5.22; baseline source restore map retained.

## Previous private agent update: v1.5.21-test (code1183)

Delivered Claude Android chat for newly launched Claude sessions, sharing the
OpenCode/Gemini view, cache and native key dialog through its own stream-json
controller. Existing Claude binary/auth/history/policies and all existing agent
assets/native libraries are unchanged. No reset or history-format migration.
Restart an old Claude session with the same saved chat to use the new controller.
Top card supports acknowledged model/native mode changes and last-request usage.
Main input sends native user messages; interactive-only Shell stores separate
application output, not model-context history. Native tools/questions/permissions
project to pills/forms with nonce/session-scoped once/deny callbacks.

Important native-window behavior: auth login naturally returns to SDK chat. Full
native CLI keeps working on Back; /exit or Ctrl+D in that CLI resumes SDK chat.
Visible RU/EN explanations exist in both terminal and chat. Busy model/tasks block
native handoff; no implicit interruption. Explicit stop is owned-process-only.
82 focused JVM tests and12 Node regression entrypoints pass; actual pinned native
CLI verified against isolated loopback synthetic provider (native tools/approval,
dedup/history resume); owned lifecycle/Back/descendant safety tests pass.
Assembly/runtime/ZIP/signature/privacy/staged-source audits pass; known Lint tooling
warnings remain. Physical Claude sign-in/IME/gestures/provider interaction still
need developer checks. No installation/GitHub publication. Baseline1.5.20 retained,
including restore map in .verification/baselines/v1.5.20-source.
APK SHA256 `256c594fcebd508f92909c85810343fd91cd1d2360934450a638174ba255a77b`, 36609110 bytes. Copy in Download verified.
Next stable versionCode must exceed1183. See VALIDATION-v1.5.21-test.md and
CLAUDE-ANDROID-CHAT.md. No new release is authorized.

## Accepted baseline: v1.5.20-test (code1182)

Developer accepted v1.5.20-test after phone checking ("отлично!!! фиксируем!!").
This is the agreed working baseline before Claude chat integration. Acceptance
does not establish exhaustive device coverage or authorize GitHub publication.

Delivered `Termux-Jobs-v1.5.20-test-arm64.apk` (36590750 bytes), SHA256
`e4c150ca824a92d06d88d9e88d69511b8bab1443bbb7f01058f19c623581bf07`.
Same package/certificate, increased code. No reset/history migration.
Fix OpenCode/Gemini switching: AgentChatCache holds bounded presentation by exact
profile+thread/run+terminal handle, restores rows/model/status/Shell draft/scroll
anchor or bottom immediately, then visible native refresh. No blank Connecting on
return to known chat. Upper workspace tabs retain the same view. No hidden-session
polling or native process/transport change. Pending forms/permission buttons are NOT
cached; fresh native state required. Gemini actual-session token change clears old
restored data; legacy capability fallback remains available. Native/real transport
failures remain visible alongside preserved messages. Cache bound8inactive entries,
512Ki serialized characters rows/entry,2Mi chars overall plus metadata, in memory
only; eviction affects display, not native history or model context.
73 focused JVM tests pass including stale ownership, cache bounds, position, blocked
transport restore for both agents, fresh permissions, actual Gemini resume and no
hidden polling/stop. Build/runtime/package/ABI/ZIP/signature/privacy/staged-source
audits pass; ALL assets/native libraries identical to1.5.19. Existing Lint tooling
warnings remain. Developer phone switching/scroll/streaming/IME checks pending.
No install/publication. Next stable code must exceed1182. See
VALIDATION-v1.5.20-test.md and architecture docs. User agreed next stage after this
fix/check: same Android chat for Claude; NOT implemented in this APK.

## Previous private agent update: v1.5.19-test (code1181)

Delivered `Termux-Jobs-v1.5.19-test-arm64.apk` (36587686 bytes), SHA256
`e3301d1a66b858517c81d09235fc73b1a429eb2ed7a5abb23723a5f39a9f7515`.
Same package/certificate; code increased. No reset or history migration.
Gemini/OpenCode empty connected chat now explicitly says local session connected /
send a message. No-connection notice explains robot startup/login and a safe
read-only snapshot retry tap. Shared AgentChatErrors presents native errors with
RU/EN region/model/auth/quota/context/network guidance; original details on tap.
Legacy F12 error hint replaced with a tap-details explanation. Unconfirmed actions
keep notice/draft across polls; no automatic action/prompt resend. Local connection
is not a claim that remote API/model is usable; region refusal remains provider-side.
Robot is available immediately for both agents, even before chat capability or
connection resolves. Local terminal dialog opens without waiting for control RPC;
owned terminal-open request runs asynchronously, unavailable bridge is explained
inside window. Existing native large keys/footer/dimensions retained. Dismissal
queues owned close after open; generation/owner result gates remain. Other agent
UI/policy/transport unchanged.65 targeted JVM tests pass. Build/runtime/ZIP/signature/
ABI/privacy/staged-source audits pass; ALL assets/native libs unchanged from1.5.18.
Existing Lint infrastructure warnings remain. Device connection/API/error-detail/
robot-arrow/IME checks pending. No install or publication. Next stable code must
exceed1181. See VALIDATION-v1.5.19-test.md and GEMINI-ANDROID-CHAT.md.

## Previous private agent update: v1.5.18-test (code1180)

Delivered `Termux-Jobs-v1.5.18-test-arm64.apk` (36583538 bytes), SHA256
`0fa4422b25f597ca7fffdd27f109b49397c923b42a85953d20a18ea954893b09`.
Fix both OpenCode/Gemini native robot windows: create fully populated large native
keys if hidden ViewPager has not created them or existing key view is empty.
Use selected config, two-row fallback, actual row height, native key client;
temporary activity reference supports modifiers and restores exact original on
asynchronous dismissal. Existing populated panel reparent/restore retained.
No tiny duplicate row. Back left/Enter right retained.59 focused JVM tests pass,
build/runtime/package/ABI/ZIP/signature/privacy/staged-source audits pass. All assets
and native libraries byte-identical to1.5.17; no history migration/reset. Certificate
unchanged. Device arrow selection/IME checks pending. Gemini unavailable
2.5-flash-lite model and duplicate Thinking are outside task. No session/process
changes, APK installation or GitHub publication. Next stable code must exceed1180.
See VALIDATION-v1.5.18-test.md. Existing Lint infrastructure warnings remain.

## Previous private agent update: v1.5.17-test (code1179)

Built/audited/delivered `Termux-Jobs-v1.5.17-test-arm64.apk` (36583330 bytes), SHA256
`e7cd24f29501e482b75631650ecfc33bc1395bfc20946ce6938f9676bc5aa33a`.
Same package/signing certificate, increased code, no reset/history migration.
Gemini gets shared AgentChatUi/View with its own native callbacks/Unix transport:
main input directly submits to model, interactive-only Shell invokes native shell
lifecycle. Existing Gemini top card and native auth/history/permissions stay.
Robot opens same-size native window; Back left, Enter right. Only full configured
large extra keys remain in BOTH Gemini/OpenCode; duplicate compact row removed.
Native Gemini window close never stops CLI. Permission overlay suppressed only
while structured chat shows; native complex dialogs/auth fallback remain.
Native session guards, stable rows, clear/rewind invalidation, bounded/lazy projection
and batched UTF-8 reads verified.566 full tests before final refinements and58 final
focused tests pass;10 Node scripts plus actual isolated CLI auth-screen probe pass.
Build/package/ABI/ZIP/signature/runtime/staged-source/privacy audit pass. Native libs
and unrelated assets byte-identical to1.5.16. Lint infrastructure warnings remain.
Restart Gemini with same chat after updating. Device-authenticated streaming,
tool confirmation, Shell, keyboard/scroll and native arrows still need user tests.
No installation/publication. Next stable code must exceed1179. See
VALIDATION-v1.5.17-test.md and GEMINI-ANDROID-CHAT.md. Codex chat not ported yet.

## Previous private agent update: v1.5.16-test (code1178)

Built/audited/delivered `Termux-Jobs-v1.5.16-test-arm64.apk` (36578142 bytes), SHA256
`9e5248de3b607cb350a55a6b87d6e533c51f0c8f6d82cca6d52a6f2a3198a3ff`. Same package/cert, increased code, no data migration.
OpenCode: Connecting/failure/retry messages inside transcript, hidden after SSE
connection; pre-connection send keeps draft and never falls through to hidden
PTY. Main-chat extra-keys button DISABLED per final user steering; no automatic
keys on Shell tap/interactive activation. Shell lock preserved. Native dialog
retains configured keys plus large compact Esc/Tab/arrows; Back to chat LEFT,
Enter RIGHT (does not dismiss). Key sends guard captured/current/running session.
Top card now model/variant instead of connection name; metadata allowlists only
provider/id/variant, fallback profile selection, no model inference. Restart same
OpenCode chat for helper reload. Other agents/native libraries/assets unchanged.
565 full JVM tests pass and22 final targeted tests pass including footer/connection
failure/recovery, input guard and model label; Node/syntax/runtime/ZIP/signature/
privacy pass. Phone tests still needed for actual startup, native arrow choice,
Enter/Back placement, IME and model readback. Lint infrastructure warnings remain.
Do not install/publish. Next stable code must exceed1178. See VALIDATION-v1.5.16-test.md.
Future Gemini/Codex Android chat discussed, not implemented.

## Previous private agent update: v1.5.15-test (code1177)

Built/audited/delivered `Termux-Jobs-v1.5.15-test-arm64.apk` (36577202 bytes), SHA256
`c42e816a5298be747fe20f21d62172219e93e249370ec1812273cfaa6758eb55`. Same package/cert, increased code; no data migration.
Shell and send gated by interactive input, draft retained. OpenCode: prefix replaces
AI:; normal reply taps do not duplicate dialogs (truncated/detail pills retained).
Additional keys now enabled in Android chat: mount only existing ExtraKeysView,
ChatExtraKeys routes editing to enabled focused Shell or main model composer;
never hidden bridge PTY or native toolbar alternate raw-input page. Parent/layout/
visibility/native client restored on hide/switch/native dialog/close. The native
window dimensions and native arrows/input remain as approved in v1.5.14.
561 full JVM tests pass; final11 targeted tests pass, including key event routing
and restoration. Build/runtime/ZIP/package/version/ABI/signature/privacy audits
pass; all assets/native libraries identical to1.5.14. Phone IME/caret/gestures still
need developer check; do not infer device behavior from mocked tests. Existing
Lint infrastructure warnings. No installation or GitHub publication. Next stable
code must exceed1177. See VALIDATION-v1.5.15-test.md and OPENCODE-ANDROID-CHAT.md.
Future Gemini/Codex chat adapters discussed but not implemented/authorized yet.

## Previous private agent update: v1.5.14-test (code1176)

Built/audited/delivered `Termux-Jobs-v1.5.14-test-arm64.apk` (36575234 bytes), SHA256
`8678a7da2a2a3f0bd97fa4acc0df7d205307b85330b481013797b4f93e3942e1`. Same package/cert, no data migration; no GitHub publication.
OpenCode Android chat: remove Stop/status rows, follow confirmed sends, cover PTY
until capability resolved (legacy status fallback), smaller native dialog with
normal input and existing extra keys automatically mounted/restored. Interactive
input focuses direct Shell in main chat. Synthetic shell context is an inspectable
pill instead of raw transcript text; duplicate native shell notice suppressed on
same projected page. Other agents/native libraries/bootstrap assets unchanged.
560 full JVM tests pass plus8 final focused tests; Node/syntax/runtime/ZIP/signature/
privacy audit pass. Existing Lint infrastructure warnings, not a clean Lint claim.
Phone keyboard/navigation/scroll checks pending. New OpenCode launch with existing
chat needed for helper update. No APK installation. See VALIDATION-v1.5.14-test.md.
Next stable versionCode must exceed1176.

## Previous private agent update: v1.5.13-test (code1175)

Built/audited/delivered structured Android chat for new OpenCode launches.
APK Termux-Jobs-v1.5.13-test-arm64.apk, 36573650 bytes,
SHA256 f6bb4ff6d2ae9ab66c1ddd4cf0904c8cf6c23d07429c7199fa1a8125b1de0f3a.
559 JVM tests pass, native headless/cancel/protocol probes pass. APK/signature/
package/code/ABI/runtime/privacy checked; physical keyboard/scroll checks pending.
 Existing top model card
retained; packages button becomes same48dp robot only for structured OpenCode,
paint-only border for native-only requests. Main composer always addresses model;
separate Shell field above it executes native non-interactive commands. No mode
switch. Native supported forms/permissions in chat, stable anchored receipts and
Thinking pills/details. Generic AgentChatView + OpenCodeChatUi adapter.
Official mini only on demand; closing mini retains native server/session. Custom
jobs2 bundle removed from APK (historical patch/templates retained). Codex/Gemini/
Claude/SSH/shell paths preserved. Do not publish/install. New launch required.
Native protocol probe passes headless/history/resizes, direct shell/cancel,
model/native ID, idle interrupt, admission/dedup, permissions, forms, official mini,
back/queued-input retention and owned shutdown without model inference.
See OPENCODE-ANDROID-CHAT.md and VALIDATION-v1.5.13-test.md. Next stable code>1175.

## Previous private agent update: v1.5.12-test (code1174)

Built/audited/delivered correctness rollback after developer reported1.5.11 chat/footer interleaving.
APK Termux-Jobs-v1.5.12-test-arm64.apk, 113533859 bytes,
SHA256 14e7510c82fd50ce4209e7f32f244ced8e1ffb169a7f4993b27e18c329c0e5c1.
552 JVM +75 frontend tests pass; package/cert/code/ABI/ZIP/runtime/privacy verified.
Only production delta vs1.5.11: version and OpenCode frontend env flag0 restoring
upstream height replay. Jobs2 binary unchanged; permissions/receipts and Android
geometry stabilization retained. Keyboard may visibly replay history again.
Real OpenTUI differential reproduced lost second output block without reset/replay,
and ordered retained blocks with reset/replay. Native mini probe verified eight
resizes including post-permission shrink/grow, receipt retention, model reopen,
owned stop. Dynamic external shell probe did not render markers (not a passing
streaming test); native model-streaming/keyboard check still needed on device.
See VALIDATION-v1.5.12-test.md. NO publish/install. Next stable code must exceed1174.

## Previous private agent update: v1.5.11-test (code1173)

Superseded due to reported text interleaving; height replay suppression was unsafe.


Final private APK built/audited/delivered in Download: Termux-Jobs-v1.5.11-test-arm64.apk,
113533795bytes, SHA256712c71282300985233b32b1619292cd454587ed2c727ace2acf8c3cb2cc063b5.
552 JVM tests +107 frontend tests passed, native height/width/reopen/permission and
owned stop passed. Physical keyboard/receipt checks pending developer feedback.
Canonical upstream check blocked by existing oxlint TypeScript-plugin loader;
TUI/CLI typechecks passed. See validation for precise limitations.

User authorized private OpenCode keyboard fix and in-transcript permission receipts
for OpenCode/Gemini. NO GitHub publish/APK install. Developer confirmed1.5.10 fixed
nav return, keyboard/keys remained. Native mini --replay repeats history on resize.
Jobs2 frontend is separate from official serve/runtime: skip only height replay,
retain initial/model-reopen/width replay. Width replay is necessary: native probe
showed history disappears if all resize replay is disabled. Includes ~77MB bundle,
177MB extracted runtime under private .ai-control; generated bundle ignored by Git.
Build/patch/provenance: OPENCODE-MINI-JOBS2.md and patches/opencode-v2.0.28-jobs2.patch.

OpenCode once/deny UI uses owned private socket/direct Java LocalSocket (no proot
polling subprocess), GET native request + recheck + native POST reply. Native complex
requests/always project rules remain in CLI. Gemini confirmed ack hides overlay.
OpenCode permission.replied and Gemini successful native callbacks add small native
transcript receipts, with tap details backed by private per-chat200/2MiB receipt
store, not scalar status/presets. Guard owner, reply staleness, concurrent answers,
preview generations and touch mouse-mode duplicate delivery. Permission != execution.
Codex/Claude were inspected, still native prompts: current bridges lack required
permission integration. Do not claim those two have new cards.
See VALIDATION-v1.5.11-test.md for final evidence and remaining device checks.
Next stable must use code>1173 for updating these private builds.

## Previous private agent update: v1.5.10-test (code1172)

Current private work: OpenCode geometry stabilization. Developer confirmed1.5.9
flicker/incoming-output scrolling fixed; latest recording shows keyboard/top-tab
re-entry replay. Workspace hiding changes layout while TerminalView stays INVISIBLE;
previous path sent intermediate PTY sizes. OpenCode-only stableResize flag ignores
hidden geometry, skips same-size existing-session attach and coalesces visible
changes (96ms quiet,320ms first-event hard limit). Cancel pending resize on hide,
background,detach,session change or close. Fresh PTY initialization is immediate.
Height-only primary resize anchors actual transcript movement for a scrolled reader;
width/alternate-screen/mouse tracking retain their reset policy. During the short
settling interval, primary bottom-follow drawing projects the visible bottom into
the old buffer; touch/scroll first applies final geometry so selection coordinates
match. No text/history replay buffer added; output frame policy remains1.5.9.
Native mini smoke with private HOME/XDG, synthetic history and inference-blocking
proxy: two final resizes, seed visible, balanced frames, owned stop. Java fixture
uses synthetic font metrics/window visibility and a JNI shadow, not device gestures.
Necessary native resize still redraws: keyboard/re-entry and selection need device
checks. See VALIDATION-v1.5.10-test.md for final build/verification evidence.

## Previous private Gemini update: v1.5.9-test (code1171)

Codex rate-limit follow-up: remove hardcoded primary5h/secondary7d labels, display
actual windowDurationMins, omit absent windows, show native account/read plan/type
without personal account data. New immutable Codex helper bridge-v0.2.21.js; never
replace v20/v19 or change working processes. Plan requires a new Codex launch;
old status files already carry windowDurationMins for label correction.
Gemini quota button opens official AI Studio; no fabricated remaining quota or
Google Cloud Monitoring credential integration. Live Gemini mode selection is now authorized/implemented: clickable Permission mode
row opens three choices; use native Config + indicator + onApprovalModeChange hooks,
idle queue and confirmed current-run sync to the same session/preset. No YOLO UI.
Gemini+OpenCode now opt into primary transcript anchoring and bounded DEC2026 frames.
Gemini frame fix confirmed by developer on v1.5.8-test. OpenCode isolated native mini
with frames: 53 complete frames, seed retained through six resizes, owned stop passed,
no inference or user credentials. Android gestures/keyboard remain device checks.

Gemini permission card: native committed ToolConfirmationMessage hook, actual confirm
callback, single-use opaque nonce, only offered once/session/cancel choices. Consume
nonce on native keyboard confirmation too. Guard config/project/body/trust, do not
approve stale/changed/double requests. Complex/native-security warning prompts stay
in CLI. Card is a bottom overlay in terminal surface; no PTY resize/page rebuild.
Preview full command/diff, explain session rule scope (can cover command prefixes),
collapse only matching native ack with once/session/denied result. Permission text
travels only over private RPC and stays in memory, never scalar status/workspace files.
No permanent approval offered. Old Gemini launch needs restart for new hooks.

Implementation and Gemini follow-up fixes authorized on 2026-10-18. Next stable must use code>1173 so the private build can update through the app.
Private APK for device checks; NO GitHub
publication, installation or modifications to working sessions/environments authorized.
Developer confirmed real Gemini response + live model/stats in1166; model picker
failed. Found Android LocalSocket.setSoTimeout before fd creation; now connect first.
Status producer is now a committed native useMemo state instead of temporary consumers.
Open details refresh locally, error categories replace the misleading auth toast,
module.register removes ExperimentalWarning, native totalTokenCount is shown.
GeminiLiveUi reads per-run local scalar snapshots; in-memory pinned CLI instrumentation
provides native model/state/counters + temporary native model setter, queued until idle.
Quota is unavailable, no user credential is embedded. See AI-AGENTS.md and
VALIDATION-v1.5.9-test.md for compatibility, evidence and remaining device checks.

- Codex: remove the Termux/Ubuntu environment selector from new sessions, presets
  and session/preset settings. Use Ubuntu automatically, matching Gemini/Claude/
  OpenCode. Do not add a replacement informational row. Existing Termux Codex
  profiles retain Termux without migration, with an editor notice; creating a new
  profile is required for Ubuntu. Paths, credentials and history are not moved. The current Ubuntu Codex process is untouched.
- Gemini: make one press of the app's bottom Send button submit the message to
  Gemini rather than leave it in the CLI composer. Current sendInput() pastes
  then immediately writes CR. Inspected pinned CLI0.64.0 contains a fast-Enter
  guard30ms and recent-unsafe-paste guard40ms that turn immediate Enter into a
  newline. Implemented Gemini-only delayed single CR160ms, guarded by run/session/input ownership.
  Real-device confirmation remains pending.
- Keep interactive input: with it enabled, tapping TerminalView must still allow
  direct typing into Gemini's own composer, navigation and native confirmations,
  just as for Codex. Keep the native composer visible; no separate API chat UI.
- Restrict any deferred-submit path to Gemini, capture the original session/run,
  and cancel on close/switch/incompatible input instead of typing into another
  session. Do not change Codex, Claude, OpenCode or ordinary shell Send behavior.
  Avoid blindly sending a second Enter; check ownership and meaningful failure
  cases. Validate direct and app-field input, multiline text and session switching.

---

User authorized GitHub publication as stable1.5.3, code1165 (> private1164).
Changes since public1.5.0: complete local-AI removal/data compatibility warning,
research preservation, server trash/background transfers/progress, session activity,
backup scans/cache/inline Refresh and separated compression method/level.
LOCAL-AI-RESEARCH.md covers both adapters, Native, tool/rights/environment wrapper,
context/cache, SDK experiment and cancelled LoRA with historical source links.

Published stable/latest: https://github.com/HezeAI/termux-jobs/releases/tag/v1.5.3
Tag v1.5.3 ->8ada09c5, immutable. APK/SHA256SUMS/update.json/research MD downloaded
back byte-identical; APK signature and updater code1165 verified. Release body
and four public tag-document links verified. See VALIDATION-v1.5.3.md.
Do not install APK or change working sessions/environments/server.
No trained LoRA or model weights to publish. design/ is pre-existing untracked work.
All four assets and the research copy are also available in Download.
Final artifact SHA256:6e5e0b0297dcc1a51e2ebfaa50f721e79ec075cb292c3b0376b1bb93745cad98.
Device-specific checks remain as listed in validation. Developer reports on
intermediate APKs do not establish every final scenario.

## Historical continuation notes

All earlier “Current private work” sections below describe their stated versions,
not the current release authorization or current local-AI feature set.

---

# Current private work: v1.5.27-test

Backup refresh moved into custom title at right in both modes. Header spinner
replaces Refresh during scans, one inline filename/byte status line; no modal scan
progress. Only explicit mode selection/Refresh/add-folder launches scan. Close
cancels its owned worker, ignores late callbacks. Initial placeholder blocks create
until inventory ready. Refresh preserves components/destination; consent is renewed.
Process-memory estimate cache has no Activity/worker/timer/disk persistence,
bounded payload and expiry; gets do not renew expiry, regrouping doesn't either.
Restore/import invalidates it. Fresh source checks still govern archive creation.
Compression method gzip1/pigz2/pigz4 separate from level1/3/9; explicit install
prompt retained. Python validates worker count and records it in metadata.
Missing requested full Ubuntu now fails SOURCE_CHANGED_RETRY before output creation.
Same package/signature/schema, code1164. Private Download delivery only. No GitHub
publication or install authorized. See release/validation1527.

---

# Current private work: v1.5.26-test

Backup inventory optimizations: DirEntry no-follow metadata reused; selective
walker optionally yields its lstat; category prefix classifier compiled once per
traversal; bounded per-dialog directory/file-category aggregates allow custom
folder regrouping without tree walks. No cache service/disk persistence/timer.
Cache never enters create request or supplies archive data/validation. Manual
Refresh sizes drops it. Cache has expiry/root+rule guards/size and row limits;
oversized/expired/changed snapshots fall back to fresh per-scope walks.
Scroll now uses same preserve-active/ignore-idle semantics as keyboard resize;
session/tab navigation retains existing behavior. Same signing/package/schema,
code1163; private APK Download, no GitHub/install authorized. See validation.

---

# Current private work: v1.5.25-test

Keyboard show/hide temporarily reset active shimmer because all viewport changes
cleared output and ignored it1600ms. New separate size callback preserves already
active output; idle resize uses per-handle redraw suppression and never affects
other sessions. Real output extends activity, resize itself does not. Scroll and
session/tab navigation retain1524 behavior. Same package/cert/schema, code1162.
Private Download delivery only; no install or GitHub publication authorized.

---

# Current private work: v1.5.24-test

Correction requested after device testing: reduce activity-line thickness from3dp to1dp,
exclude scroll/viewport-resize/tab-return redraw from output-based activity.
TerminalView default notification is observational only; same input/PTY behavior.
A 1600ms grace resets old output, ignores redraw during grace, and never replays
ignored output after grace. Native Codex busy/local foreground task remain valid.
Remove header dots and restore original other-session title Toast behavior.
Same package/certificate, code1161, no reset/GitHub/install. See validation1524.

---

# Current private work: v1.5.23-test, server trash

В этой приватной сборке также добавлены:

- «В фон» для скачивания и отправки: сохраняется та же передача и окно,
  строки прогресса появляются в карточке сервера только при активных операциях.
  Нажатие возвращает прогресс/отмену; рамка «Сервер + агент» анимируется по часовой.
- Тонкая линия активности во всех терминальных сессиях, с постоянной высотой
  области и без пересоздания терминала при анимации.
- Вместо Toast при изменении заголовка другой сессии — кружочки над вкладками,
  краткий плавный текст события и окно других сессий по нажатию.
- В списке открытых AI Agents — векторный робот; у обычных сессий остаётся терминал.

See USAGE/ARCHITECTURE for telemetry and process-lifetime limitations.

User requested vector trash after server Home, shared long-press selection, empty-mode toggle/cancel and Yes/No deletion confirmation; selected trash and download/move cards highlighted. Built privately as code1160, same package/signing. No GitHub publication or APK installation authorized. Existing server agent not modified; helper sent over pinned SSH/Python. Real-server deletion is NOT performed during development, only isolated .verification fixtures. See RELEASE/VALIDATION-v1.5.23-test.md.

---

# Current private work: v1.5.22-test, remove local models

User authorized removal of all on-device AI plus its dedicated jobs-local-ai Ubuntu, preserving all unrelated current features. No GitHub publication or APK installation authorized. Version code1159, package/signature unchanged. Old local profiles skipped; first-launch fixed app-private local-ai directories/cache/preferences removed without following symlinks. See RELEASE/VALIDATION-v1.5.22-test.md for final artifact evidence.

jobs-local-ai was verified inactive, canonical, marked as dedicated, and removed; allocated304125KiB. Working ubuntu root identity retained. No Bridge/normal session/shared SDK/server changes. App-private models/chats are not visible from this proot: cleanup is on first new-APK launch, not claimed already done. Old research/report documents below are historical.

---

# v1.5.0 public / v1.5.1-test handoff (2026-10-17)

Public release preparation: v1.5.0 code1137, local models disabled while testing, existing local data retained. Private v1.5.1-test code1138 enables all local models and experimental Qualcomm SM8750 NPU runtime. Same com.termux/signing; next compatible update code must exceed1138. Publication/delivery status is recorded below after final verification; do not infer completion from this preparation note.

Shared update: keep-history checkbox on unsaved close, presets/history separation and supported Codex history manager; clearer agent permissions; server long-press selection, recursive SFTP download to chosen Termux/proot/device destination (10GiB/file,20k entries,depth64,256GiBbatch,6h transport); exclusive UUID batch destination, random per-file temp names, completed files retained on interruption. Same-server nonoverwriting Move/Move here, self/collision checks, partial batch not rolled back. Final UI: left-pointing parent arrow; two equal lower panels ↑ upload / ↓ download or move separated by8dp, replacing wide upload; no extra action after search. No user app installation/service restart; source/build-only work and isolated real server fixture.

724 Java tests passed in full configuration. 153 public-specific tests passed; final artifacts/report at .verification/server-download-npu-v31/. Read docs/fork/RELEASE-v1.5.0.md and RELEASE-v1.5.1-test.md. Existing CPU native libraries/CLI/server-agent assets unchanged from v1.4.30-test. NPU requires actual device testing; SDK presence is not proof of FastRPC/app namespace/model support. Model verified on server under /root/models/gemma4-e2b-sm8750/, use app Download then Import. SDK tools/binaries remain ignored .tools; public APK excludes QAIRT/dispatch.

Still planned separately, NOT implemented here: bounded command large-output logging/preview/continuation instead of misleading64KiB TOO_MANY_ENTRIES failure. Never auto-repeat a command because output overflow may follow side effects. Applies shared local-model tools when implemented, not external CLI agent tools.

---

# Agreed next update: shared local-AI command output handling

User explicitly asks to record for NEXT update only; do not start implementation/build from this confirmation. Current command output above64KiB throws misleading TOO_MANY_ENTRIES and loses captured result; user screenshot curl GitHub HTML demonstrates this is not proof of missing internet.

Agreed behavior for shared LocalAiTools command path (all local llama.cpp/LiteRT models, not independent Codex/Claude/Gemini/OpenCode CLI tools): stream full output into an app-owned bounded log, continue draining output so large output alone does not fail/kill/block command; return compact beginning/end preview, actual exit/completion/cancel/timeout status, explicit truncation and retrievable result reference. Allow later bounded reading/search of retained output under existing permissions/trust boundaries. Define disk retention/cap behavior explicitly; never claim unlimited/full retention after cap. Large output error must not imply action rollback. Never automatically rerun command due to oversized output, because side effects may already have occurred. Keep previews within model context; no general model-context increase implied. For webpages prefer saved page/API/extracted relevant text rather than injecting full HTML.

GPU for Qwen3.5 2B set aside by user after UI lag; remain CPU ARM/6threads for this model. NPU investigation remains separate. Strict native call vs prose errors are a different issue; raw diagnostics now expose them.

# Device regression report after v1.4.30-test

User reports severe UI lag/jank from first GPU message, clarifies not total freeze. No fresh v30 diagnostic or screenshot available at inspection. Read-only current view saw Ubuntu/CPU runner7154 (no OpenCL mapping), GPU busy16%; this does not capture prior GPU incident or exclude unseen app processes. Source: OpenCL queue uses default priority, GPU1024/512 batches; UI inference is off main thread. GPU/UI resource contention is plausible, NOT established from trace. Lightweight device query confirms cl_qcom_priority_hint support, no cl_khr_priority_hints/throttle_hints. Probe source updated in .verification/local-ai-gpu-probe/probe.c; no model/session stopped or changed. Next GPU responsiveness experiment: investigate Qualcomm low-priority context/queue hint, smaller batches/short yields; keep CPU unchanged, don't promise hints remove jank. User only clarified lag in latest message; no new APK produced yet.

# Delivered v1.4.30-test / code1136

GPU integrated as optional localCompute=cpu(default)/gpu, separate OpenCL Bionic runner. Engine settings Operating mode CPU/GPU; GPU hides CPU variant/threads preserving values, manual CPU return restores CPU location too. Initial gate catalogqwen35-2b+SM8750+ARM capability; GPUAndroidexecution independent of Ubuntu tools. Model selection blocks unsupported until CPU chosen. CPU benchmark blocked inGPU; request diagnostics actualbackendGPU. Existing native CPU/control payload byte-identical v29; one newOpenCLrunner, vendorlibnotbundled. Model cache/cancel/quit ownership retained. No automatic CPU fallback after GPU error; selectCPU manually.

NativeGPU IPC realmodel tests passed load/count/generate/repeat/pre-cancel/active-cancel/post-cancel count/benchmark rejection/quit.706 Java tests passed; release/runtime15990defs/30reviewed, package1136/cert/ZIP/SHA/stage/limited secrets passed. APK Termux-Jobs-v1.4.30-test-arm64.apk 61162136 bytes SHA 65005b933b86c96b1dce976e43bdc7144ccdac5fc542b507465e6192115e5068; Download APK+README+SHA. No install/GitHub. Device APK GPU namespace/coldstart/keyboard/tools still developer checks.

Validation cards now raw original call+named invalid/duplicate parameter+tool-specific schema; model feedback omits redundant raw (assistant already contains original). Strict prose/examples/multiple calls still reject. Emptywrite result says didNOTdelete; guide rm+absence verification only whenallowed, HTTPquote/statusguidance. Model choice can still be wrong; don't promise full semanticfix. Oldcards originalnotrecorded.

NPU requested as next separate experiment, not enabled here. OtherQwenGPU requires validation/allowlist expansion; runner architecture generic. v29 source checkpoint retained .verification/checkpoints/v1.4.29-test-code1135, APK kept build/apk; rollback update needs highercode>=1137 samecert. New optional field defaultsCPU, no autodatareset.

# Next build scope added by user after GPU experiment

User agrees GPU option in current Processor mode selector; suggested UI name Computation mode, preserving ARM(auto)/Compatible CPU and adding GPU(OpenCL,experimental), CPU default unchanged. Integration still pending; standalone result does not establish app namespace/runner integration. Keep model tools environment Ubuntu independent of GPU Android/Bionic execution; explain placement instead of silently relabeling Ubuntu/glibc as GPU.
Three latest screenshots inspected:203249 benchmark events,210721 native tool sequence with validation errors interleaved with executed curl/jq/Python requests,210729 opened error Native call must be one complete block without prose. This proves envelope rejection, not lack of network permission; raw call currently hidden so exact malformed shape not known. Executed command card alone does not prove HTTP2xx/task success (curl -s can exit0 on404; displayed URL query appears unquoted with ampersands; target termux/termux may be mistaken). Original embedded Qwen template permits optional reasoning before call, strict adapter does not accept arbitrary surrounding prose; do not loosen execution to arbitrary examples to mask issue. Next fix should show raw output/exact error and tool-specific repair. Deletion(emptywrite!=delete) remains pending too.

# Completed isolated GPU experiment, 2026-10-17

Real Qwen35-2B Q4_K_M ran on Adreno830/OpenCL3, b9222 separate Android/Bionic runner. Exact exported model copy SHA unchanged. All builds/probes in .verification/local-ai-gpu-probe, no default native payload/app/session changes. docs/fork/EXPERIMENT-QWEN-GPU.md and results.json record timings. CPU6thread2048tok22.184s; GPU batch256/12832.739s,512/25624.676s,1024/51217.461s and repeat17.304s. BestGPU~21–22% less prefill time than single CPU control.16token greedy generation CPU0.730s, bestGPU0.997s; same replyprefix. GPU not universally faster; keepCPUdefault, optional futureexperimentalGPU. Native OpenCL lacks fusedGDN; fallback graph works. Shader/context/load excluded; app namespace/permissions/tool integration not tested. GPU is NOT integrated into APK. User deletion/validation fixes remain agreed next build; don't claim fixed from these compute tests.

# Performance follow-up 2026-10-17

User-approved isolated GPU capability probe compiled under .verification/local-ai-gpu-probe and executed without loading models/changing live app. OpenCL platform/device discovery succeeded: QUALCOMM Adreno830, OpenCL3.0. This proves device enumeration in Ubuntu/proot shell only, not app linker namespace access, model compatibility, kernels or throughput. No GPU backend integrated yet. Existing llama OpenCL source has Q4_K paths and SSM_CONV/SOLVE_TRI; full Qwen35 op/shape coverage still needs checking.
Latest threads report6063779124222160485 duplicated as5529871243138086124: 2048tok,2threads44.14/58.91sec;4threads27.90/32.35;6threads28.29/25.17;8threads26.80/26.00. Difference6vs8mean~1.25%, no robust gain. Keep6default. User asked to run existing batch comparison256/128vs512/256 and send report; no APK needed. Current mode2 benchmark already supports it.

# Agreed next build after v1.4.29-test

Add exact invalid/duplicate native parameter name, original model call and tool-specific repair schema to validation cards/feedback (currently generic message plus list/read schema even for command). Do not accept ambiguous duplicates. Device test showed list works, but delete request used write text="" and falsely claimed deleted; /sdcard/Download/Тест.txt confirmed 0-byte existing. Do not classify deletion/commands as device-verified. User now requests read-only investigation of model/engine speed, not another build yet.

# Delivered local test: v1.4.29-test / code1135

Opt-in Qwen3.5 2B native tool adapter (localProtocol=json default / qwen-native). Schema and strict one-call XML-like parser from verified installed Qwen3.5 2B template; not universal Jinja. Engine count/generate share rendered history, service IPC carries mode. Recorded toolRequest metadata only, grouped tool_response and untrusted boundary escaping. Existing permission/approval/runner path preserved. UI Engine settings third selector; only catalog2B/llama supported; reload same chat, custom rights scope stale on switch. Native libraries and control assets byte-identical v28. 698 tests passed; ARM64 build/runtime15987defs/30reviewed passed; final ZIP/package1135/cert/SHA/stage/limited secret checks passed. SHA 92e30c560b60a867c1e8b6e9cd2a6a2283a3f6b4ff195f9db904915774725ee4. Download APK/README/SHA delivered, no install/GitHub. Device adherence remains unverified. Compare fresh native vs JSON chat, same CPU6/ARM/Ubuntu/rights/context8192recommended. Need versionCode>=1136 next.

Latest screenshot Screenshot_20261017_195840.jpg: model false access denial, tool not called, unsafe/wrong oldest-file ls/sort suggestion; screenshot does not prove permission failure or context-compaction cause. User informed.

# Delivered local test: v1.4.20-test / code1126

Schema errors (filter txt) returned to model with executed=false feedback, at most2
schema retries/turn. Invalid calls never run, corrected calls still authorize and
approve normally; security exceptions not retried. Prose not promoted, cancel
checked, internal finish=tool programmer errors not hidden. Failed validation card
records excluded future model context. Prompt distinguishes filter category vs
extension. Native engine unchanged. Auto UI renamed ARM acceleration (when supported),
stored enum auto unchanged. Trusted toolCached history flag labels actual read-cache
reuse; after2 cached repeats a further same-path non-refresh read stops with honest
no-final-answer message. Explicit refresh/other loops not covered.642 Java tests
passed, final APK audit passed. Download APK+SHA+README. No install/GitHub. Both
CPU libs byte-identical19; user tested ARM optimization: cold1263tok~9.9sec vs
old~55sec, but tests not controlled. User manualstop invalidatescache, next4096tok
report66sec could include build/thermal load. New real model workflow needs device
check; tests do not guarantee model understanding. Next code>=1127.

# Delivered local test: v1.4.19-test / code1125

Engine selector in local sessions/presets: auto vs compatible. Optional localCpu
field defaults auto, strict persistence/copy/IPC; busy settings blocked and idle
reload preserves chat and owner/cancel guards. Native generic jobs_llama probes
hardware; fast jobs_llama_fast targets armv8.2-a+dotprod+fp16+i8mm. Check dotprod,
FPHP/ASIMDHP, atomics, RDM, CRC32 and i8mm before loading fast. Separate nested
FastCpu JNI namespace, internal exports hidden; all operations stay in owninglib.
Missing fast library at load-time falls back; no native crash-recovery promise.
CPU4/batches/context/rights/Ubuntu/agents unchanged. Diagnostic backend shows
actual variant and threads.635 full Java tests+50 final UI/limits passed; CPU
loader, guard policy, both cache and real tiny GGUF decode tests passed with same
greedy tokens and expected compiled feature flags. APK audit passed, Download
APK+SHA+comparison instructions. No install/GitHub. Qwen2B actual cold speed and
older-device fallback remain device checks; tiny tests aren't throughput evidence.
Next code>=1126. User has Snapdragon8Elite and wants MODEL tool autonomy, not
more phrase heuristics. v1.4.18 prior-result excerpt/routing changes included.

# Delivered local test: v1.4.18-test / code1124

Folderless latest-file requests clarify folder without inference. Screenshot phone
aliases/text-only real path result; vision-capable sessions retain model flow.
Earlier large tool results excerpted only in next-turn runtime context; archive,
current task/results/rights preserved, chat notice added.632 Java tests and APK
assembly/runtime/signature/archive/source checks passed. Download APK delivered.
No GitHub or installation. Native engine unchanged. User now prioritizes MODEL
tool autonomy and investigating CPU engine performance, not more phrase templates.
CPU baseline generic ARM64, max4 threads, no GPU, no explicit dotprod/i8mm.
Host exposes asimddp/i8mm. Actual installed model is not visible via mounted
/data/data/com.termux/files (only home/usr), so tiny-model CPU tests cannot establish
Qwen gains. Proposed future setting: Auto vs compatible CPU; diagnostic variant/
threads. No engine settings implemented yet. Next code>=1125.

# Delivered local test: v1.4.17-test / code1123

Per-model-call timing records in existing local diagnostics. Metadata only, RU/EN,
64-record bound, active/completed/interrupted status, pause archival behavior.
All local engines use reports; native exact cache/tokens llama-only, LiteRT unknown.
626 Java tests passed; assembly/runtime/artifact checks passed. Native payload and
agent assets identical to1.4.16. No inference/permission/tool behavior changes.
APK and instructions in Download. No publication or installation. Ask for exported
multi-pass report to explain expensive third prefill. Evidence .verification/local-ai-v1.4.17.
Next code>=1124. Data/history unchanged.

# Delivered local test: v1.4.16-test / code1122

Native llama prefill progress, separate setup/cache/first-step timing; RAM prefix
reuse scoped to service owner/chat/exact system, final-token replay for logits,
bounded snapshots for hybrid partial-rollback fallback. Native tiny-model cache
logit/restore/reset tests passed;624 Java tests passed; ARM64 build/runtime/artifact
audit passed. APK in Download. No publication or installation. Qwen3.5 real warm
speed and snapshot support remain device checks. Evidence .verification/local-ai-v1.4.16.
Next code>=1123. Existing session data/history/settings preserved.

# Delivered local test: v1.4.15-test / code1121

User authorized build of local context/reply selectors, progress placeholders, local diagnostic timing/export, and RU/EN fast find-latest-and-read. llama contexts4096/8192; LiteRT4096; reply caps256/512/768/1024/2048. Optional v8 localContext/localMaxTokens defaults4096/768. Safe idle-only reload preserves chat and checks cancel/panel identity before prepare. Native result encodes emitted tokens and finish bit, matched by new Java caller. Stage/metrics IPC replies never complete RPC. Timing metadata only, capped/persisted worker reports with no prompt/tool content. Native build/tests passed. First final suite/build passed prior small idle-label/UI-thread-I/O fixes; rerun final delivery tests/build before audit. Evidence .verification/local-ai-v1.4.15. No installation or publication.

# Delivered: v1.4.14-test / code1120

User approved context reduction with in-chat notice, current-chat history tool, cached reads and immediate literal display of short latest-text requests. Native exact token counting rebuilt with shared prompt-token helper; LiteRT estimated budget plus one overflow retry. Full history is unchanged; latest user/system permissions never silently cut. Mode chat disables history tool. No external agent changes. New optional history flags contextEvent/literal. Source/build evidence in .verification/local-ai-v1.4.14. Final test/build/audit still pending. No GitHub/install authorization.

# Delivered local test: v1.4.13-test / code1119

User approved common latest-file discovery after local model searched /sdcard instead of Download for latest TXT. LocalAiRecent parses conservative short RU/EN requests, runs locations → scoped list with exact extension and mtime sorting → text read only for open/read intents. Questions/paths/named files/compound requests stay model-driven. All normal auth/environment/path checks remain. Details show checkedPath. No native/runtime/external-agent changes. Tests/build/audit evidence: .verification/local-ai-v1.4.13. No installation/publication authorized.

# Текущая проверка: v1.4.12-test / code1118

LocalAiNames: варианты написания/транслитерации + явные variants от модели (не универсальный перевод). LocalAiSearchResult: только absolute file candidates, exact validation, multiple/approx single requires choice. Events.chooseFile + awaitFileChoice/FileChoiceCard: явный выбор, cancel/Stop не продолжают модель; restored read-only. Friendly results + Technical data. Контроллеры внешних агентов не менялись. Проверки .verification/local-ai-v1.4.12. Следующий code>=1119. Без GitHub.

# Текущая проверка: v1.4.11-test / code1117

Приветствия/вежливые фразы screenshot discovery; LocalAiToolText — только presentation, не executable. Смешанный JSON+проза вызывает один repair для action request, только standalone JSON через authorize/approval исполняется. Карточка preview unconfirmed с raw details, key-cache; пользовательский текст/история не меняются. Все локальные модели, внешние агенты не менялись. Проверки .verification/local-ai-v1.4.11. Следующий code>=1118. Без GitHub.

# Текущая проверка: v1.4.10-test / code1116

Настройки внешних AI: Codex read/write + write scope + optional approval; миграция null сохраняетстаруюполитику. OpenCode opt-in project/worktree scope denies shell/external/unknown. Claude/Gemini scope недоступен честно. Применение при следующем запуске, работающие процессы не трогаются. Новый immutable bridge-v0.2.20, старый не перезаписывается. Проверки .verification/agent-permissions-v1.4.10; следующий code>=1117. No GitHub. Downgrade ignores optional new fields and may relax new scope/approvals: warn users.

# Текущая проверка: v1.4.9-test / code1115

Переносимый screenshots tool для всех локальных моделей: несколько Android storage aliases/папок, смешанные форматы изображений, сортировка mtime, dedup dev/inode, отдельные статусы missing/denied/scope. Короткие прямые запросы «3 последних скрина» запускают поиск до генерации. Расширены locations, сохранены выбранная среда/права. CLI других агентов не менялись. Проверки .verification/local-ai-v1.4.9, следующий code>=1116. Без GitHub.

# Текущая проверка: v1.4.8-test / code1114

Выбор окружения инструментов: Основной Termux или выделенная Ubuntu jobs-local-ai. Проверка/предложение первой установки, прогресс по этапам, Python3/Git, владельческие маркеры, --isolated и только общая память, без fallback на рабочую Ubuntu. Устаревшие значения локальных профилей сохранены, но требуют повторного выбора. Обычные сессии не меняются; выделенная маршрутизация через ephemeral backend local-ai-ubuntu. Установка реальной среды во время разработки не выполняется. Проверки `.verification/local-ai-v1.4.8`.

# Текущая проверка: v1.4.7-test / code1113

Готово в Download; 534 теста прошли, SHA/подпись/ABI/ZIP/runtime/source-stage проверены. Следующий APK code>=1114. Важные пробелы: реальные Gemma vision/видеокодеки/APT проверяются разработчиком на устройстве; см. VALIDATION-v1.4.7-test.md.

Локальные инструменты метаданных/папок/ошибок/APK, экспериментальные изображения Gemma LiteRT и кадры видео, фиксированная установка недостающего инструмента после плана APT и Да/Нет, в том числе Read при Confirm actions ON. Qwen всё ещё без визуального проектора. Названия «Права локального ИИ» и «Работать только в пределах проекта», смысл/хранимые поля прежние. Без установки APK/пакетов/моделей и GitHub. Доказательства `.verification/local-ai-v1.4.7`; реальные vision/APT сценарии проверяет разработчик.

# Текущая проверка: v1.4.6-test / code1112

Локальные сессии: название в настройках, потоковая прокрутка без фокуса/скрытого UI, менеджер проекта; актуальные права без перезагрузки модели, один retry ложного отказа. Без GitHub/установки. Проверки `.verification/local-ai-v1.4.6`.

# Текущая проверка: v1.4.5-test / code1111

Локальный чат: независимая анимация точек в строке управления, раскрываемые карточки инструментов с фактическим статусом, бирюзовая линия у пользователя. Разрешения без изменений. Текущие доказательства `.verification/local-ai-v1.4.5`; без публикации/установки. Старый recovery1110 ниже новой сборки; новую recovery не запрашивали.

# Текущая проверка: v1.4.4-test / code1109

Три независимые настройки Local AI: режим действий, Проверка путей, подтверждения.
Chat: нет tools. Read: list/read/search. Full: +write/command, НО command запрещён
при Path checks ON (до approval и в script). При OFF пути вне проекта разрешены
в пределах прав Android/среды; write/command всё ещё требуют Full access.
Отключение Path checks через UI требует явного положительного ответа предупреждения;
до него checkbox и Selection остаются true. Старые false-профили сохраняются.
Без отдельной изоляции, без GitHub/установки APK. Rollback1.3.4 code1110,
после него следующая сборка должна иметь1111+.

## Историческая запись

# Текущая проверка: v1.4.3-test / code1107

LocalAiAgent общий для llama/LiteRT/импорта. Full access по умолчанию авто.
Опция Подтверждать действия включает карточки Да/Нет для write/command; не расширяет
режим read/chat. Проверка путей независима. stop отменяет ожидание карточки.
Reasoning — плашка → отдельное окно с собственной прокруткой.
Исправлен CPU Qwen non-thinking suffix по официальному chat_template.jinja;
JNI nativeGenerate теперь возвращает int finish (eos/token_limit). Не смешивать
старую .so с новым Java. SDK finish — sdk_done, не выдумывать EOS/причину лимита.
Один внутренний retry для reasoning-only/предложения вместо действия; сырые
рассуждения/проза не исполняются. Выполнение конкретными моделями проверить на устройстве.
Без GitHub/установки. Baseline rollback1.3.4 code1108, следующий APK после него>=1109.

## Историческая запись

# Текущая проверка: v1.4.2-test / code1105

Запросы инструментов из блоков json/tool/без подписи распознаются. Структура, режим
и переключатель путей проверяются как прежде; текст/произвольный shell-блок не исполняется.
Долгий тап: Новый чат / Выбрать чат / Настройки. Модель и доступ — в Настройках.
Из списка сессий три дублирующих пункта удалены. Без GitHub и установки APK.
Rollback прежней v1.3.4 обновлён до1106; следующая сборка после него требует1107+.

## Историческая запись

# Текущая проверка: v1.4.1-test / code1103

Разрешены локальная сборка и передача в Download, без GitHub/установки.
Локальный ИИ: Markdown, свёрнутые think, компактное управление, три режима доступа.
Полный доступ — автоматические запись/команды без подтверждений. Галочка
«Проверка путей» отдельно ограничивает файловые инструменты проектом; команды
не являются песочницей. Исходники/истории остальных ассистентов не сбрасывать.
Свежий rollback прежней v1.3.4: code1104; после него нужен code>=1105.
Ограничения и проверки: LOCAL-AI.md, RELEASE/VALIDATION-v1.4.1-test.md.

## Историческая запись

# Текущая работа: локальный ИИ v1.4.0-test / code1101

Пользователь разрешил сборку для проверки, БЕЗ GitHub/установки APK. Явно разрешён
новый rollback текущей v1.3.4 с code1102 (это отменяет прежний запрет создавать его).
Checkpoint: .verification/local-ai-v1.4.0/checkpoint (исходники+Git bundle+patch).
Новая функциональность/ограничения: docs/fork/LOCAL-AI.md. NPU runtime не включён,
llama.cpp CPU, LiteRT GPU/CPU требуют проверки на устройстве. Не выдавать за полную
реализацию NPU/MTP/инструментов агента. Пользователь просит итоговый список пробелов.
После recovery следующая сборка обязана иметь code>=1103. Старые параметры/истории
Codex/Termux не удалять. Документы ниже — исторические записи.

Финальная локальная1.3.4:443 JVM-теста прошли, APK проверен и передан в Download.
SHA-256 d0d97d0ef84c89aae10e18569a5ac7d4742acbfc9ab1f080de2a64db3e2a5eef.
Публикацию1.3.4 пользователь пока не запрашивал, GitHub остаётся1.3.3.
Нужна проверка поиска/прокрутки/IME и повторного открытия окон на устройстве.

# Локальная v1.3.4/code1087

Пользователь разрешил освобождение закрытых окон и стабильный поиск. Диагностику
не менять, опрос Codex тоже не входит в эту работу. GitHub остаётся v1.3.3.
После тестов/сборки APK в Download; новой rollback-сборки не создавать.
Проверки .verification/ui-performance-v1.3.4. Без установки APK/работы с сессиями.

Исторические записи ниже.

# Опубликовано v1.3.3 — Обязательное обновление

https://github.com/HezeAI/termux-jobs/releases/tag/v1.3.3
Source/tag dbc90d7f. Latest stable, draft=false/prerelease=false.
APK/SHA256SUMS/update.json скачаны обратно и совпали; публичные metadata и
документы проверены. Android8.0/API26 минимум. 440 JVM-тестов прошли.
Финальный APK в Download, SHA-25672ef607dc8d1005abeeefa014ae985ec107394fc63fe8850d60e0335227592a7.
Подпись/пакет прежние, код1086. Нужна проверка разработчиком закрытия/запуска
после обновления. Ожидаем обратную связь; не устанавливать APK самостоятельно.
Старые локальные записи ниже исторические; новый откат не создавать.

Финальный APK SHA-256: 72ef607dc8d1005abeeefa014ae985ec107394fc63fe8850d60e0335227592a7

# Срочное исправление v1.3.3/code1086

Пользователь разрешил минимумAndroid8.0/API26 и публикацию с названием
«Обязательное обновление», с объяснением нашей ошибки Guava. Отдельного
разрешения публикации не спрашивать; APK не устанавливать.
Штатный FileUtils восстановлен; собственный NioFileDeleter отсутствует в финальном APK (проверено). Guava33.3.1-jre в обоих модулях + согласование транзитивных запросов.
440 JVM-тестов прошли. Проверки .verification/crash-v1.3.3. Важен аудит отсутствия
устаревшего класса в финальном APK и повышение minSDK в RU/EN metadata.
Новой rollback-сборки не создавать; установленную среду/процессы не менять.

Ниже исторические записи.

# Релиз v1.3.2/code1085

Опубликовано: https://github.com/HezeAI/termux-jobs/releases/tag/v1.3.2
Source/tag ae002f7e; latest stable, draft=false/prerelease=false.
APK, SHA256SUMS и update.json скачаны обратно и совпали. Публичные metadata/документы проверены.
APK неизменён относительно переданного в Download.
437 JVM-тестов прошли. Данные без миграций/сброса; пакет/подпись прежние.
Синхронизация отложена. Новой rollback-сборки не создавать.
Публикация: .verification/release-v1.3.2; исходные проверки .verification/xda-v1.3.2.
Ниже исторические записи локальной подготовки.

Финальная1.3.2:437 JVM-тестов прошли, APK собран/проверен, SHA-256
76bb3399711ca5cd4845246784280e2ca73414b8cd0c395436bb9d1845f4da6d.
Пользователь проверяет APK в Download; GitHub публиковать после подтверждения.

## Текущая локальная работа v1.3.2/code1085

Кнопка Back в просмотре звёздочки возвращает к тому же picker, вкладке/поиску/позиции.
Добавлены3 XDA/primary-source карточки (всего129), уточнены signal9/proot-root.
Синхронизация отложена. GitHub остаётся1.3.0. Новая rollback-сборка не нужна.
Доказательства: .verification/xda-v1.3.2; исследование .verification/xda-research-20261016.

# Локальная v1.3.1/code1084 — проверка интерфейса и справочника

GitHub latest остаётся1.3.0/code1083. Пользователь разрешил добавить исследованное
и согласованный просмотр в звёздочке; текущего разрешения публиковать1.3.1 нет.
Новый rollback НЕ создавать. Основной APK1.3.1 в Download, подпись/package прежние.

Терминал → звёздочка: открывается полный текст без вставки. Три действия:
редактировать, вставить в текущий черновик без отправки, удалить с подтверждением.
openInputEntry переиспользует previewBody и прежние редакторы. Основные просмотры
команд/заметок и недавних команд не переделаны. Поиск/переключение списков сохранены.

Справочник126 карточек:12 новых из проверенных первоисточников,8 дополнены без
дублей. Справа от заголовка «Собрано с помощью AI!» / «Compiled with AI!».
435 JVM прошли, финальная сборка/подпись/ZIP/ABI/hash/stage проверены.
На устройстве проверить длинный текст, действия, IME и заголовок RU/EN при DPI.
Доказательства .verification/star-library-v1.3.1/, VALIDATION-v1.3.1.md.

4PDA741456: прямой доступ403, включая st20/st40/st7980 и страницу авторизации.
Поисковый индекс дал часть обсуждений/шапку; все страницы/цели ссылок не прочитаны.
Первичные документы проверены отдельно. Данные для входа не передавались сайту
и не записывались в проект; не переносить учётные данные в контекст/отчёты/коммиты.
Отбор .verification/4pda-741456/ и docs/fork/FORUM-REVIEW-v1.3.1.md.

Прежняя публикация1.3.0: исходникиb73fb904, receipt/audit .verification/release-v1.3.0/.
Релиз опубликован,3asset сверены, updater schema1 совместим. Telegram-пост с3частями
и APK отправлен через существующего бота в настроенную тему; настройки/состояние
серверного бота не менялись. Повторно ничего не отправлять без команды.

Не менять установленный Termux/Ubuntu/общий SDK/Bridge/пользовательские процессы,
не устанавливать APK. Инструменты/кэши только в репозитории. design/ предшествует
задаче. Разработчик HezeAI; Codex помощник. Для публикации RELEASING.md.

Final v1.4.8-test: 545 tests passed; APK audited and delivered to Download. SHA-256 1a009cee2751e1913d55bacfd59cf03b35899a83dadfbe15b109a200e9fb12f8. Next build code >=1115. Device setup testing pending.

Final v1.4.9-test delivered in Download: 554 tests passed, ARM64 APK/source/CRC/signature/SHA verified. SHA-256 dec36b0f35a60a2646185e27def007a5fdced46c7feb02d96c135827cbc34813. Dark local settings included. Next code>=1116. Device checks pending; no publication.

Final v1.4.10-test delivered in Download. 563 Java tests + 11 fake RPC scenarios passed, ZIP/package/version/ABI/signature/SHA/runtime/source-stage checked. SHA-256 a97b4f88630043a5eaa5b86eb6ff678b293decfeb0ef78ebd1c19105234b51ca. Device enforcement tests pending; see VALIDATION-v1.4.10-test.md. Next code>=1117. No publication/installation.

Deferred by developer: next update generic file search with name variants/transliteration, suffix matching and candidate selection (факторио/factorio). Do not claim implemented in v1.4.11-test.

Final v1.4.11-test delivered to Download: 572 tests passed; final APK/source/ZIP/package/ABI/signature/SHA/runtime checks passed. SHA-256 ece3ffb035e57fff4a04b25d0d04073aa5a5ade45cab7c38c8778082be8b2dc1. Native/external agent controller assets unchanged from v1.4.10-test. Device checks pending; next code>=1118. Generic filename alternatives deferred. No GitHub publication.

Final v1.4.12-test delivered to Download. 584 tests passed; APK/source/package/version/ABI/ZIP/signature/SHA/runtime verified. SHA-256 752bbcd954a60a983c1ba947a8a94743497a3f8de268f4723e37768f32c3e949. Native/external agent assets unchanged. Device checks pending, next code>=1119. No GitHub publication or APK installation. Generic spelling variants + choice implemented; universal translation remains unsupported.

Final v1.4.13: 593 tests passed; build/runtime/ZIP/package/ABI/cert/source/limited-secret checks passed. Download APK/README/SHA delivered and hashes verified. SHA256 aeb0992e89e3a9e65515d88d0ab94418cabc386c9b8928b1b523007869c123bd. Next versionCode >=1120. Device tests pending; no GitHub publication.

Final v1.4.14: 613 Java tests passed; native real-vocabulary count/policy tests, build/runtime/ZIP/package/ABI/signature/source/limited-secret audit passed. Download APK/README/SHA delivered and verified. SHA256 bb6e8cc53323e7376bb6f4dcaa6eb208f383bc12e95b26bb6d27afcb132dd216. Next versionCode >=1121. Device checks pending. User deferred configurable context/output limits to next update: keep4096/768 now. No GitHub publication or APK install.

Final v1.4.15 delivered to Download:623 full-suite tests +44 final UI/limits checks passed; native/build/runtime/ZIP/package/ABI/certificate/source/limited-secret checks passed. SHA256 29dc4d416fdc1f0950ad4addae7b57e61ecc198513b79c196b4bacc9bd7c4e0e. Next versionCode>=1122. User device checks pending. No publication or APK install. Settings context/reply wired through saved presets/privateIPC/engine; per-engine context choice (llama4096/8192,LiteRT4096); reply max256..2048. Diagnostics long tap local session, Copy/Download export, status placeholders. No further code changes after final audited build.

## Latest delivery v1.4.22-test /1128
Delivered /sdcard/Download/Termux-Jobs-v1.4.22-test-arm64.apk. Engine settings contains only CPU mode and thread selection; localThreads optional integer 0/2/4/6/8, Auto preserves max4; service clamps to available CPUs. Store/copy/create/preset/settings/reload/IPC paths updated. 653 tests and final audit passed (.verification/local-ai-v1.4.22/artifact.json). No APK installation/publication. Developer should select6 threads, save, test normal Qwen chat and export timing. Next versionCode>=1129.

## Latest delivery v1.4.23-test /1129
Delivered /sdcard/Download/Termux-Jobs-v1.4.23-test-arm64.apk. Input composition after context fitting via loaded llama tokenizer, total exact, leave-one-group-out approximate components: system/tool definitions, previous messages, tool requests/results, current user message, formatting residual. LiteRT estimated. Metadata only via existing performance callback; per-pass timing reports. Attribution failures nonfatal/unavailable; cancellation propagated. Button now simply Engine settings.656 tests/final audit passed (.verification/local-ai-v1.4.23/artifact.json). User should export new-chat greeting and old-chat greeting reports. No device claim/no publication. Next code>=1130.

## Latest delivery v1.4.24-test /1130
Delivered /sdcard/Download/Termux-Jobs-v1.4.24-test-arm64.apk. Shorter tool/system instructions preserving schemas/options and authorization boundaries. Earlier-turn tool outputs >256 chars now128head/48tail plus source/history marker; current-turn outputs and saved archive untouched by this layer. Local saved preset menu Close/Settings/Delete; model/import picker cards use settingsButton palette.658 tests and final artifact audit passed (.verification/local-ai-v1.4.24/artifact.json). Native libraries unchanged. Real model quality, UI and timing require developer checks; asked same-message old/new chat reports with6threads and a file lookup. No install/publication. Next code>=1131.

## Latest delivery v1.4.25-test /1131
Delivered /sdcard/Download/Termux-Jobs-v1.4.25-test-arm64.apk. Shared local-agent list/search/screenshots context projection to columns/rows after real execution/selection; originals retained in history/UI; all requested rows preserved, no8000char cut on valid compactlists. Instructions recommend filter/extension/sort/limit1 for newestTXT, not forcedrewrite. Applies llama/LiteRT, not externalCLI. Added local-vs-web search/build-tool clarification and clearer draft status (not approval).661 cases passing:fullsuite then targeted39 workspace cases after two stale UI-label assertion updates; see validation evidence. Final artifact audit passed; all native payload unchanged. No install/publication. Ask developer to repeat latestTXT task/report and inspect listingrequest params. Next code>=1132.

Next update discussed: engine-location experiment Android embedded(default)/nativeTermux/Ubuntu for localAI, independent of tool environment. Compare same llama commit/model/input/thread/batch first; no promise Ubuntu faster. Do not use working Ubuntu or touch active Codex/Bridge. No engine-location implementation yet.

## Latest delivery v1.4.26-test /1132
Delivered /sdcard/Download/Termux-Jobs-v1.4.26-test-arm64.apk (64618337 bytes), SHA256 9e0bd3f74c33c2e1c66355bfca0101eefb3a319a67c3d9f0cdde785c0944b6cd. Environment settings now has Tools (Termux/dedicated jobs-local-ai) and Engine (android default/termux/ubuntu) cards. Same dedicated Ubuntu for both, never working Ubuntu. localLocation optional v8 field defaults android; copy/store/creation/settings/reload/IPC/instructions updated. LiteRT Android-only. Engine settings still CPU mode+threads. Four bundled local stdin/stdout runners, baseline+fast per Bionic/staticGNU platform. Same GGUF directory mounted into Ubuntu; no duplicate weights, HTTP endpoint or remote calls. Prefix cache/tokenization/sampler/batches/bench share maintained C++ helpers. Own-process request-ID cancellation, EOF/quit, parent-death SIGKILL. Native binaries/source/buildscript: app/src/main/cpp/local-ai/runner and scripts/build-local-ai-runners.sh. Linux static flags must be -static-pie WITHOUT explicit -static-libgcc/-static-libstdc++ (bad combination caused missing interpreter+DT_NEEDED crash, corrected).667 Java tests and all four raw/packaged runners tinyGGUF + cancellation/lifecycle tests passed. Isolated proot smoke passed, not actual installed proot-distro wrapper/Qwen/device UI. Final audit .verification/local-ai-v1.4.26/artifact.json. No install/publication. Developer should test Termux/Ubuntu startup then same benchmark length with6threads across three locations; report actual speed unknown. Models location/copy/move discussed but not implemented: same-file bind already sufficient for engine comparison; physical adjacency is not a compute accelerator. Next code>=1133.

## Latest delivery v1.4.27-test /1133
Delivered /sdcard/Download/Termux-Jobs-v1.4.27-test-arm64.apk. SHA256 928588abea13cd73d8887a118cd7c9857175c52b557a3d418b03e99f8ef8ba37. Fixed bundled local-ai-agent-instructions.md read/cached via LocalAiInstructions, injected before current settings through Events on every local llama/LiteRT agent turn. No project-file instruction loader. Guidance chat/read/full rights, real access/result checks, missing dependencies, oldest/newest target checks, filtered listings not whole directories. No rights/approval/native/schema/CLI/process changes. Dedicated install tool still requires Confirm actions and explicit plan; full unscoped commands may install using selected env package manager.670 tests/full build/runtime/artifact audit passed. First test compile failed due old Robolectric getApplication API, corrected to application. Device/Qwen compliance unverified. Need test existing/new chat actual tool behavior; no guaranteed compliance/speed; added system tokens reset prefix cache. README/SHA/instructions copied Download; no install/publication. Next code>=1134. Evidence .verification/local-ai-v1.4.27/.

## Latest delivery v1.4.28-test /1134
Delivered /sdcard/Download/Termux-Jobs-v1.4.28-test-arm64.apk, SHA256 f4eda64c97532163e4b5f4fcd6799e271d4018be6a7a5aaba13509aa84b62756.686 final Java tests passed; build/runtime/artifact audit passed (.verification/local-ai-v1.4.28/). Native payload/adapters unchanged fromv27. No install/publication/environment/process changes. Settings custom behavior/rights, explicit optional Termux commands, new-local Ubuntu tool/llama default and LiteRT Android; legacy sessions preserved. LocalAiPrompts validates TXT/MD UTF8 max12KiB; stores copied text in optional v8 fields, stale custom rights scope blocks until review/reset; app real settings still enforced. LocalAiTools command-only optional environment ubuntu|termux, original authorization then derived Termux HOME, no fallback. UI direct shared-storage root-bounded picker with access request. Standard prompt action examples, any dependencies through allowed full shell, local-vs-web distinction, bounded continuation after promised next step. Close unsaved local/normal checkbox Keep history defaulton, Close/Addpreset+close; CLI-native histories independent. LocalAiUi serial close+delete after writer tasks, shared active-chat/closing guards. History manager global settings local+Codex MainTermux/workingUbuntu, native thread/delete only, read includeTurns with bounded preview; unsupported methods fail, no archive or file fallback. Parent dismissal does not cancel successful persistent deletion updates. Store binding cleared only after successful deletion. Codex protocol inspected via installed CLI-generated schema (official docs fetch403), no user app-server/history operated during development. Device checks needed particularly local/Termux routing, keyboard/storage/DPI, plain-language Qwen behavior, Codex version/API/history support. Existing external CLI chats need user close too (cannot exhaustively detect outside app). Docs/readme/changelog/release/validation updated. Previous v27 checkpoint retained. Next code>=1135.

## Post-v1.4.28 diagnosis and next agreed experiment
User exported installed qwen35-2b.gguf to /sdcard/Download/Qwen3.5-2B-installed-copy.gguf. Actual full SHA25657a1085840f497d764a7fc5d346922dbde961efb54cc792ea81d694fd846a1d8 and1396198496bytes match pinned catalog; first16MiB upstream identical. Independent verified copy .verification/local-ai-v1.4.28/diagnosis/models/qwen35-2b-original.gguf, do not alter original installed/exported copy. Parsed actual metadata/chat-template and delivered JSON/TXT Download. Source Jinja uses tools/<tool_call><function=...><parameter=...> and <tool_response>, current C legacy ChatML+plainJSON differs; causal effect on refusals unproven. Think-disabled suffix matches source.3 isolated navigation probes passed; switching views/onStop do not unload, loading another local model intentionally unloads previous. Activity destruction closes engine. Observed live PID16411 with qwen model mapping, not a continuity test. Next update agreed: Engine settings -> Format of communication with model -> Current JSON / Native model template (testing), Qwen first, keep fallback/rights unchanged. NOT IMPLEMENTED. User asks copy inspection fulfilled after export; original source/APK/model/process unchanged by diagnosis, except this note and verification artifacts. Need compare disposable create/read/delete tasks with native vs current protocol, not more permission expansion.


## Publication 2026-10-17: v1.5.0 / private v1.5.1-test
Public stable v1.5.0 (1137) published at https://github.com/HezeAI/termux-jobs/releases/tag/v1.5.0 from commit981cb020. APK63573526bytes SHA2569f550c5f0cead995bce7b353b8e3a128997781e1015c7f18fd00ce31b4f51b9c. GitHub asset digest matches final APK; update.json and SHA256SUMS included. Public local-model entry intentionally disabled for testing; existing data preserved. Private enabled/NPU testing APK /sdcard/Download/Termux-Jobs-v1.5.1-test-arm64.apk code1138 SHA2564aeba58f2b776eb75976903f1039f35f317a541531cec6d73ec3123beb286d78. Same package/certificate, next update code must exceed1138. 724 Java tests,153 public targeted tests,41 Python tests, native permissions test, real isolated SFTP smoke and final artifact checks passed; actual NPU inference remains unverified on device.

Next update ONLY, agreed server controls: replace background-poll checkbox with Connect/Disconnect button, status Connected/Disconnected, rename polling Auto-update server data. Disconnect must close this app's connections to selected server and stop auto-update, without stopping agent, remote tasks or independent SSH sessions. Reconnect restores panel connection and updates. Preserve current10s inside/30s outside cadence while app active; implement owned-connection cancellation safely. Not implemented in v1.5.0/private1.5.1.

Post-publication: downloaded all three public release assets as read-only HTTPS streams and independently verified SHA256; GitHub latest stable confirms v1.5.0.

## Private v1.5.2-test context fix
Delivered /sdcard/Download/Termux-Jobs-v1.5.2-test-arm64.apk,1139,SHA25660d045080b488e4f36a79b33346004ac3d01cabd891299655846e78b9647d327.725 Java tests passed. Startup LiteRT budget failure reproduced with full actual default behavior+permissions+runtime notice+short hello: read3296/full3420 >3264. Bundled behavior compacted4343→2208chars retaining rules/examples, permission code/tool schemas/custom prompts untouched; new full-prompt12-combination regression has512token headroom. Final APK audit passed, same signer, native/QNN/CLI assets unchanged. NPU real inference remains unverified. Public v1.5.0 unchanged, next private code>=1140.
User also requested inspect LiteRT native protocol. Official google-ai-edge/LiteRT-LM docs/api/kotlin/getting_started.md and models/gemma4/chat_template_e2b_e4b.jinja inspected through GitHub API (web search403). Native dialogue template handled by SDK; Gemma4 tool template uses <|tool_call>call:...<tool_call|>, not Qwen function blocks. Current adapter uses SDK system/history/user roles but empty tools with textual JSON requests. Native tool support uses tools schema + automaticToolCalling=false + Message.tool/Content.ToolResponse, and callbacks' structured toolCalls. This is NOT implemented in1.5.2; must validate against pinned0.17.0 actual classes/model metadata before adopting. Current main upstream docs alone do not prove pinned binary compatibility. Keep existing app authorization/approval/coordinator on any future native migration. Evidence saved .verification/litert-context-diagnosis/. User has not authorized public release of this fix.
Pending next-update UI: restore styled environment-choice tiles in new-session and preset menus (standard radio rows inadvertently introduced by public feature gate). Do not conflate with context fix.

## Gemma downloaded model read-only inspection + multimodal request
Inspected actual downloaded3016294400byte model in Download/download-a4c34ed7-6488-4b21-aa5b-2ed047013d97/gemma4-e2b-sm8750/,SHA matches pinned41dd...:container1.5.0,12sections,10TFLite submodels,SentencePiece+LlmMetadata,actual11995charJinja extracted. No model/installed data edits. Actual template uses Gemma turn/tool_call/tool_response, no forced bogus chat tool. ParsedTFLiteinterfaces:prefill128/decode1,KV4096positions;cannot raisecompiledcontextbysettingalone. Includesimage/audioencoders/adapters;no native video decoder. Evidence .verification/litert-context-diagnosis/model-inspection/{model-structure.json,model-template-10.jinja,tflite-interfaces.json,FINDINGS-RU.txt}.SDK0.17AAR inspected;audioContent.AudioFile and audioBackend exist. Current NPU visionexplicitlydisabled, audiobackendnull. User requests experimental image/audio andvideo-viaframes testing; not implemented. Must enable only verifiedcompatiblemodel, preservefileauthorization/cacheboundedpaths/cancel/modelrestore, testinputsandhardwarebackendsbeforeclaiming. No audio generationpromised;NPUall-modal executionnotassumed. Current chat/toolformat issue still needs fix/A-B as recorded in COMMUNICATION-FINDINGS.md. Shared ToolCard/reasoning/Markdown UI already applies to all local models. User report1.5.2 completedNPU935msfirsttext435msbutbogustool_namechat;contextfailuregone. No liveinference done by assistant in this diagnosis.

## Delivered private v1.5.3-test /1140 — LiteRT native tools + media
Download/Termux-Jobs-v1.5.3-test-arm64.apk delivered/rehash verified:108643365bytes,SHA25630a581e810be2bd1fb5a26d21844756b56ba66da063784a7c25f6ebb3116ffe4. Next private code>=1141. Same signer/package; no reset, no GitHub publication. User does NOT intend rollback; compatibility storage is preventative, not a rollback build/model change.
Native LiteRT selectable in Engine settings → communication format, JSON/nativeLiteRT; experimental opt-in for all LiteRT models, not a guarantee all support tools/media. Newly selecting pinned Gemma defaults native; existing JSON sessions stay JSON (tell user explicitly select native and new chat). Native Qwen protocol unchanged. Store persists old-readable localProtocol=json plus optional localLiteRtNative bool, reconstructs native in memory; presets/session copy paths use existing localProtocol. New validator/strictbool/conflict checks, rights custom scope changes still require review.
LocalAiLiteRt reflection bridge checked against actual0.17.0classes. SDK structured schemas/providers/calls/tool_response, automaticToolCalling=false with hard-throw execute proxy; coordinator retains authorization/approvals. Single call per response, multiple calls fail with no execution. SDK model(Contents) overload does NOT exist: use3args(Contents,List,Map); caught by realSDK probe and fixed. Numeric whole limit/start normalized, reserved tool discriminator rejected. Plain greetings explicitly normal text for all default behavior; native has compact non-JSON prompt. Tool schemas included in approximate context budget.
Pinned Gemma view_image/video now experimental viaGPU vision with NPU text; listen_audio viaCPU audio, Android MediaExtractor/MediaCodec first5seconds -> bounded mono16k WAV in owned cache, no cloud/speechgeneration/video sound. Separate declaredcapability checks; unverifiedNPU/import models not silently given media. Lazy backend init and text restoration on media init failure; source file authorization unchanged, checked owned media paths, cancellation/decode60sdeadline/sourcecopybounds/cleanup. Media success flag marksattachment not completedmodelanalysis.
Full Java suite passed after initial changes; final relevant menu tests passed;83 final storage/UI/permissions/Qwen tests passed.10 separate realSDK adapter tests on existing Java21 passed (fake inference, realclasses; not hardwareproof). Actual AARclassfile65 cannot be instrumented by oldJava11Robolectric; probe stays ignored verification, not app. Final isolatedbuild+ZIP/runtime/signature/privategate/QNN/nativepayload/limitedsecretchecks passed. Evidence .verification/litert-native-v153/. Device NPU native conversation/image/audio/video still unverified; request newchat greeting/question,readfile, screenshot,audio then diagnostics. README delivered. Installed apps/user processes/Ubuntu/sharedSDK unchanged.
Deferred UI environment tiles and server connect/disconnect plan remain pending. Current user requested no rollback; do not create another backup/rollback APK unnecessarily.

Карточки активной передачи сервера теперь включают полосу прогресса обеих направлений.
При неизвестном размере используется indeterminate; обновляются только собственные View.

Линия над терминалом и бегущая рамка передачи сервера обновляются примерно60 раз/с. Частота данных сервера не менялась.

У каждого индикатора не более одного ожидающего кадра. Скрытие/отключение
отменяет кадр; рамка сервера останавливается вне терминала и при сворачивании.
Это не останавливает передачу файлов и не меняет частоту сетевых запросов.
