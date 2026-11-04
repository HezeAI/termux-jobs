# Permission compatibility: isolated new-profile checks

2026-11-04. Applies to the local test5 candidate. Published v2.1.2 and delivered
test4 remain unchanged. No old profile, real authorization or working session
was changed. This is a scoped verification, not a security certification.

## Confirmed test4 startup defect

The phone developer reported Codex 0.166.0, Node 22.22.1 and a successful
`codex app-server </dev/null` exit. That command does not exercise the app's
configuration or Unix transport. The app added
`-c 'approval_policy="untrusted"'` before `app-server --listen unix://…`.
The same pinned binary exits 1 with:

```
approval_policy = "untrusted" is no longer supported; remove this setting
```

This is public CLI/TOML validation before socket creation or the initial RPC
exchange. It is not evidence of failed account authorization. The previous audit
started app-server without this override and supplied `approvalPolicy: untrusted`
in `thread/start`. That typed API accepts it in the same binary. Those native
permission results are valid for that harness, but did not validate test4's full
server bootstrap. Generated-string/JVM checks missed execution of that override.

Test4 production baseline: `0c2ce05e75b119c978f5bb6c20237a749ab7da26`.
The public upstream distinction is visible in the exact
[0.166.0 config source](https://github.com/openai/codex/blob/rust-v0.166.0/codex-rs/core/src/config/mod.rs).
The local binary probe, not that source alone, establishes the observed rejection.

## Local fix and effective policy

`CodexSessionShell.command` stages an immutable `codex-launch-v1.cjs`. It runs
one owned native child with a supported server bootstrap (`on-request` when the
requested thread policy is `untrusted`). This is not an alternative approval
policy for the chat. The production `CodexChat.connect/params/verify` sends the
saved requested policy through `thread/start` or `thread/resume`, and verifies
returned cwd, sandbox type and approval policy before becoming ready. A refusal
or mismatch leaves the chat unready; it never retries with `never` or full access.
An untrusted launch without the prepared chat controller fails closed.

The launcher drains stderr and emits only fixed safe startup classifications and
exit status. It does not publish arbitrary stdout/stderr. Cancellation signals
only its owned child and waits for child termination; bounded escalation remains
owned by that child object. The shell clears a reaped failed launcher PID before
its EXIT handler. Existing saved policies are not migrated or rewritten.

Invalid modes fail before launch for all four agents with a RU/EN explanation:
startup stopped and full access was not enabled. Native rejection remains an
error; accepting an enum in Java is not treated as proof of actual permissions.

## Native identities

Exact pinned inputs are checked before the explicit integration runner:

| CLI | Version | SHA-256 of tested executable/module |
| --- | --- | --- |
| Codex | 0.166.0 | `f2e6c980c937e734644bed1b4a9a018d3e63a0ccbe71aafc85c86a8c6e4b462a` |
| Claude Code | 2.1.296 | `f1f6e96e0d8342b9dbf41d7e88255397a6a52ce3d8736ad6a4c6b59c9b62fefa` |
| Gemini CLI core bundle | 0.64.0 | `d566e81b1df8d4d44a91bf7aeea96a58ec0e6c8934fa424315ef060507dcbb8c` |
| OpenCode | 2.0.28 | `3549c97d73c8046fe06eecd35ce8c928fc8f45e1eae3e4bf017430c168ba04c9` |

Node is 22.22.1. No CLI component/version is installed or updated by these tests.
Gemini's hash identifies the actual bundled module under test, not an APK asset
or a claim that the entire interactive CLI/auth route was executed.

## Default-policy dynamic matrix

Each row uses eight artificial cases: shell/direct write × inside/outside ×
No/Allow intent. Profiles are created by the production new-profile factory,
atomically saved and reloaded before constructing the production launch command.
An artificial legacy Termux Codex profile is retained byte-for-byte in every case.
Separate Robolectric tests exercise actual profile editor creation/save/reopen/save
for all four agents and preserve legacy permission selections.

| Agent | Saved default | Actual tools and result | Boundary |
| --- | --- | --- | --- |
| Codex | `untrusted`, full process scope | Native `exec_command` and `apply_patch`, inside/outside: No keeps original bytes; Allow changes them. Eight actual UI answers. | Executes generated app bash with only guest bind paths translated to private fixture paths, real launcher/Unix WebSocket/private controller/initialize/thread/start. No real Android/proot or account. |
| Claude | `manual` | Native Bash and Write inside/outside: No preserves bytes; Allow writes marker. Eight target UI answers. | Production ClaudeChat SDK controller and real CLI; generated outer bootstrap/proot command is checked, not executed. Write prerequisites use real Read and, where requested, a separate UI Allow. |
| Gemini API | `default` | Native shell inside/outside and write_file inside: six target UI answers with corresponding effects. Both outside Write attempts denied by native path validation before a card. | Production UI reply/bridge/native callback/message bus/policy engine/Scheduler/tools. Native core, artificial history; no live model inference or API-key authorization. |
| Gemini Vertex | `default` | Same eight policy cases with saved Vertex profile fields. Six target UI answers, two native outside-path denials. | Same native policy core. This does not test Google Cloud sign-in, IAM, project billing or Vertex network access. |
| OpenCode | wildcard `ask`, `build` | Real shell and direct Write inside/outside: eight target UI answers; No preserves bytes, Allow changes them. Native session returns the requested rules. | Real server/agent loop and production permissions adapter. Outer bootstrap/proot not executed. Outside scope consent is an additional explicit fixture gate, not silently granted by the app. |

Total: **40 cases, 36 target UI button answers** (18 No / 18 Allow), four native
Gemini path denials. Read prerequisites and OpenCode external-directory consent
are recorded separately, not counted as target answers. There is no model-
simulated file change: native tools execute, and final artificial bytes are checked.

These results cover new-profile defaults. Alternate read/plan/project/auto-edit
modes are not exhaustively validated against real tools here. A project label is
not an OS sandbox: an allowed Gemini shell writes outside its project. Codex
restricted sandbox launch failures from the earlier audit remain unsuccessful
isolation checks; confirmations are not disabled when a restricted launch fails.

## Evidence, isolation and initial failed runs

Runner: `scripts/test-native-permission-chain.sh`; new native test source:
`scripts/tests/native/NativeProfilePermissionTest.java`. It is explicit, not
silently included as ordinary mocked JVM evidence. Android UI widgets run in
Robolectric; only the Java/proot transport is replaced by local test HTTP.

The kernel guard blocks external TCP, UDP/DNS, raw socket and inherited-connect
paths. Only listeners bound by that fixture tree are reachable. Optional Unix
scope is a private owned directory; Codex additionally uses one exact physical
socket hash derived from its unique fixture rendezvous. A private alias resolves
to a registered owned socket; the supervisor connects the checked physical copy.
Foreign Unix/loopback listeners, traversal and nonprivate roots remain denied.
No working proxy/firewall setting is changed, no real credentials are read.

Compact local evidence: `.verification/codex-startup-test5/`, including CLI
identities, config probe, native matrix, XML, guard negatives and safe launcher
unit tests. The original config rejection is in the sibling
`.verification/codex-startup-test4/config-probe.json`. Initial fixture failures
remain recorded: excessive socket path length, missing Claude Read prerequisite,
Codex protected-socket alias blocked by the guard, and a missing artificial Android
chat marker. None is counted as a successful run. The first all-agent native
40-case run passed; a separate new invalid-policy test initially failed due its
fixture socket path before validation. Validation now runs before filesystem setup.
The final native matrix was repeated successfully on the final sources. The broader
88-method set had one stale legacy UI selector failure; its two-method class was
corrected and rerun successfully. All final selected methods pass. See the
[test5 build report](VALIDATION-2.1.3-test5.md).

## Developer-reported Codex phone acceptance — 2026-11-04

HezeAI reports **test5 / 1481** passes new Codex profile startup, shell No/Allow
once and direct-edit No/Allow once on the phone. These device checks are separate
from the native fixture matrix. See the [acceptance record](VALIDATION-2.1.3-test5.md#developer-phone-acceptance--2026-11-04).
The report does not establish exact inside/outside paths, every alternate policy,
profile save/reopen, old-profile compatibility or recovery. No current CLI version
was independently re-probed during this documentation update.

## Phone acceptance still required

Codex startup and the four reported decisions above are complete. The following
plan retains unreported coverage and the other agents; it is not wholly passed.

1. Use separate artificial profiles/projects. Do not change working accounts or
   old profile permissions. Create, save, reopen and start each agent's new profile.
2. Codex startup now passes on the developer phone. Still check startup-error
   reasons, unsupported policies and preservation of confirmations on failure.
3. Request shell and direct file modifications inside/outside the artificial
   project. Press No; independently inspect bytes/command marker. Repeat and Allow.
   Record tool name and actual card; missing card is not proof of a user refusal.
4. For Gemini, record native outside-file rejection separately from shell approval.
   Test API and Vertex authorization separately only with authorized test accounts.
5. Reopen/save an old profile and confirm its actual environment/policy remain.
   Stop/retry only the artificial sessions. Confirm no second server survives stop.

The fixture matrix does not itself verify Android/proot or device cards. The
separate developer report confirms Codex startup and the four reported decisions.
Other agents, full profile lifecycle, Keystore/key broker, authorization and
recovery remain open. Test4 acceptance was blocked by startup and is not
retroactively marked successful. Attachment accumulation/disconnections
remain separate and are not fixed by this change or closed by this report.


## 2026-11-04 — Prepared phone fixture: Gemini permission buttons

This is an explicit local test connection, not a shipped provider or real Google
login. It uses Gemini 0.64.0 / Node 22.22.1 and the existing application's optional
connection executable. The pinned production Gemini assets match test7 source
`0a51e24c`. A new HOME, settings, project, history and synthetic environment are
created under `.verification/phone-permissions/`. No application profile/store is
imported or edited by setup. Runtime symlinks reuse installed binaries read-only;
446 bundle files pass their existing integrity manifest. This is configuration
separation, **not an OS filesystem sandbox** or a complete file-open trace.

Each launch starts a loopback-only deterministic provider inside the existing
kernel network guard. Only listeners registered by that process tree are allowed;
external and unrelated local connections are blocked. Fresh negative guard tests
passed. The provider emits only fixed shell/write calls against its own marker
files, has no forwarding route, caps requests at 2 MiB and retains at most 512
structured metadata events. It does not save HTTP bodies, audio, keys or prompts.
The real CLI may save its own artificial chat history in the private fixture HOME.

Checked anew: native preparation → bootstrap → production controller → main CLI
→ permission answer → actual file bytes. Shell No/Allow once and Write No/Allow
once all pass. Controlled stop is acknowledged, owned processes exit and the
provider closes. This is not Android UI acceptance. Initial fixture failures
(full-message parsing, selecting an earlier command and expecting an absolute
path in the edit-card body) are preserved with final successful evidence under
`.verification/phone-permissions/`. No production app change is claimed.

On this device the fresh phone fixture is linked at
`/root/termux-fork/.verification/audit-gemini-phone`. It starts with four
`AUDIT_BEFORE` files. These paths are local test artifacts, not release resources.
For another environment explicitly run
`scripts/tests/phone-permissions/gemini_fixture.py prepare`; do not replace an
already active fixture or reuse a working profile.

1. Create a **new**, separate Gemini profile (do not copy test7). Ubuntu AI;
   API-key mode; leave the API-key field empty; model `gemini-2.5-flash`;
   **Подтверждать действия / default**. The launcher supplies a synthetic key.
   This intentionally does not exercise Android key-broker/Keystore handoff.
2. Project: `/root/termux-fork/.verification/audit-gemini-phone/project`.
   Connection executable, without arguments:
   `/root/termux-fork/.verification/audit-gemini-phone/launch-gemini`.
   Save, reopen and verify these settings, then launch only this test profile.
3. Send the four messages below sequentially. Wait for each operation to finish
   and inspect the file before sending the next message.

| Message | UI answer | Expected file |
| --- | --- | --- |
| `AUDIT SHELL INSIDE 1` | Deny | `project/shell.txt` remains `AUDIT_BEFORE` |
| `AUDIT SHELL INSIDE 2` | Allow once | `project/shell.txt` becomes `AUDIT_SHELL_2` |
| `AUDIT WRITE INSIDE 3` | Deny | `project/write.txt` remains `AUDIT_BEFORE` |
| `AUDIT WRITE INSIDE 4` | Allow once | `project/write.txt` becomes `AUDIT_WRITE_4` |

Check independently in a separate ordinary **Ubuntu AI terminal**, not the Gemini
chat or its interactive CLI input:
`/root/termux-fork/.verification/audit-gemini-phone/check-markers`.
It reads only the four known synthetic files. Outside markers must remain
`AUDIT_BEFORE`. No card, an unexpected marker/value, or a network/auth error is
not PASS: stop and report message number, visible safe error and marker result;
do not remove the launcher or switch to an unrestricted policy to continue.

After the last operation, use only this test session's gear → **Закрыть сессию**
and confirm. Wait for closure; the owned provider ends with its controller.
Do not force-stop the application or stop other sessions. Closing the dialog alone
is not session stop. Outside-project behavior, other agents, repeat/crash recovery,
real login and backup/restore remain open. Android lifecycle/Keystore and clean
restore require a separate Android instance/device without working sessions;
no destructive test is prepared against the active instance.


## Developer-reported Gemini phone acceptance — 2026-11-04

**Source: developer messages about the phone**, not the assistant's native fixture.
After the four documented messages, the developer reported «все прошло» and pasted
this result from `check-markers`:

```text
project/shell.txt: AUDIT_SHELL_2
project/write.txt: AUDIT_WRITE_4
outside/shell.txt: AUDIT_BEFORE
outside/write.txt: AUDIT_BEFORE
```

Accepted scope: the developer reports completing shell Deny/Allow once and direct
Write Deny/Allow once **inside the artificial project**, using the prepared Gemini
connection. This continues the test7 verification; no new APK/version measurement
was supplied in this message. The pasted final values support the final successful
writes. The intermediate denied states are supported by the developer's statement
that the sequence passed; the assistant did not observe those phone button presses
or independently capture each intermediate file state.

The outside lines record only the two displayed synthetic marker values at the
reported check. No separate phone comparison of outside files' identities/bytes
was reported; general outside-file preservation remains unverified. This is not
an attempted outside-project read/write or an OS-isolation test. The assistant's
separate native checks of outside markers remain attributed to the assistant.

Do not repeat these accepted inside-project phone permission cases without a new
failure or relevant input change. Not covered: saved-profile policy reopen,
permission persistence beyond this sequence, outside-project operations, actual
Google login, Android key handoff/Keystore, crash recovery, backup/restore, Claude
or OpenCode. The final report did not separately confirm session shutdown; do not
attribute the assistant's native stop/exit evidence to the developer.


## 2026-11-04 — Prepared phone fixture: Claude permission buttons

Source: **assistant native checks**, not developer phone acceptance. Claude
2.1.296 (`f1f6e96e0d8342b9dbf41d7e88255397a6a52ce3d8736ad6a4c6b59c9b62fefa`),
Node 22.22.1, production preparation/bootstrap/controller/main CLI, mode `manual`.
The relevant launch/Claude assets still match test7 source `0a51e24c`.
Four new Bash/Write Deny/Allow once cases pass through native control to actual
file bytes; stop is acknowledged and owned processes exit. The existing kernel
network guard is retained and fresh negative checks pass for this prepared fixture.
Only its owned loopback provider is available; no forwarding or real account.
Write first reads the same artificial file using native Read, then requests Write.
That preliminary inside-project Read is not presented as a denied-read test.

A fresh phone fixture has separate HOME, CLAUDE_CONFIG_DIR, history, git project,
synthetic key and fixed markers. It does not import or edit working app profiles.
The installed CLI binary is reused through a version symlink and its hash verified;
no new CLI/Ubuntu installation or update occurs. This is configuration separation,
not an OS filesystem sandbox or a complete file-open trace. The provider is capped
at 2 MiB per request/512 metadata events, records no HTTP bodies/keys/prompts, and
can request only predefined actions against its own marker files. MCP/plugins are
outside this work and are not configured. No working authorizations are supplied.

Create a **new Claude Code** profile, Ubuntu AI, Sonnet, **Подтверждать действия**
(`manual`), new chat. Save/reopen settings and launch only this profile.
Connection command, no arguments:
`/root/termux-fork/.verification/audit-claude-phone/launch-claude`.
Project: `/root/termux-fork/.verification/audit-claude-phone/project`.
No real login/API key is needed: the launcher supplies a synthetic key solely to
its local provider; this does not establish Anthropic authentication.

| Message | UI answer | Expected file |
| --- | --- | --- |
| `AUDIT SHELL INSIDE 1` | Deny | `project/shell.txt` remains `AUDIT_BEFORE` |
| `AUDIT SHELL INSIDE 2` | Allow once | `project/shell.txt` becomes `AUDIT_SHELL_2` |
| `AUDIT WRITE INSIDE 3` | Deny | `project/write.txt` remains `AUDIT_BEFORE` |
| `AUDIT WRITE INSIDE 4` | Allow once | `project/write.txt` becomes `AUDIT_WRITE_4` |

After each action, check independently in an ordinary **Ubuntu AI terminal**:
`/root/termux-fork/.verification/audit-claude-phone/check-markers`.
For phone acceptance report each intermediate result; final values alone cannot
prove both denied states. The two outside values are shown separately; they do
not establish preservation of other files or outside-project access restrictions.
If no permission card appears or an unexpected path/value is shown, stop and report
the message number and safe error; do not switch permissions or remove the launcher.
Then this test session's gear → **Закрыть сессию** → confirm; wait for closure.
No force-stop of the app or other sessions is needed. The provider stops with its
controller. Phone UI, outside-project actions, crash/restore and real login remain
open. Evidence: `.verification/phone-permissions/claude-probe.log`, native proof
referenced by `latest-claude-verified-root.txt`, fresh fixture root referenced by
`claude-phone-root.txt`, and `claude-phone-enforcement.log`. The existing Gemini
fixture and its developer acceptance remain separate and unchanged.


## Developer-provided Claude final marker output — 2026-11-04

Source: the developer pasted phone-terminal output from the prepared Claude
fixture's `check-markers`, not an assistant-run check:

```text
project/shell.txt: AUDIT_SHELL_2
project/write.txt: AUDIT_WRITE_4
outside/shell.txt: AUDIT_BEFORE
outside/write.txt: AUDIT_BEFORE
```

This supports the final expected writes after the two Allow once steps inside
the artificial project. The message does not separately report intermediate
Deny results or confirm every phone approval-button action. Keep shell Deny and
direct Write Deny pending that clarification; do not attribute the assistant's
four native scenarios to the developer. No repeated Allow test is requested.
The outside values are recorded only as displayed, without claiming a separate
before/after comparison, broader file preservation or filesystem isolation.
Profile save/reopen, shutdown, outside-project operations, actual authorization,
Android recovery and backup/restore remain outside this evidence. No APK identity
was newly measured in the message.


## Developer-confirmed Claude and Gemini four-step phone acceptance — 2026-11-04

Source: explicit developer phone report after the marker outputs: «На телефоне
проверил все четыре сценария Claude и Gemini: отказ и разовое разрешение для Bash
и прямого Write. После каждого отказа файл оставался неизменным, после разрешения
— получал ожидаемое содержимое». This clarification completes the earlier partial
Claude record; its previous uncertainty remains historical, not a current blocker.
Both agents' four inside-project Deny/Allow once cases are accepted as developer
checks. Assistant native fixtures remain a separate source. No repeat is needed
without a relevant change or failure. Outside operations/preservation beyond the
shown artificial marker values, saved-profile policy reopening, real login,
shutdown, abnormal Android lifecycle and backup/restore are not inferred.


## 2026-11-04 — Prepared OpenCode phone fixture; component guard retained

**Installed CLI confirmed independently by the assistant**, using the stock
read-only `agent-dependencies.sh check opencode api ubuntu-ai` with a fresh shell.
Result: READY:opencode-cli, READY:node, CHECK:done. The existing npm fallback is
`/root/.local/share/termux-jobs/agents/node_modules/.bin/opencode`, resolving to
`@opencode/cli/bin/opencode.exe`, version **2.0.28**, SHA-256
`3549c97d73c8046fe06eecd35ce8c928fc8f45e1eae3e4bf017430c168ba04c9`.
The absence of `agents/bin/opencode` alone was not absence of the installed CLI.
No component installation/update was performed or requested; production component
checks still match test7 source `0a51e24c`, with no skips or successful fake checks.

New assistant checks use that exact installed executable, Node 22.22.1, ordinary
production preparation/bootstrap/bridge, Build, effective `ask` policy readback,
local model catalogue and supported model-change/readback for `audit/audit`.
Shell and direct Write Deny/Allow once all pass against actual artificial files;
owned controller stop is acknowledged and its launch exits. **Phone pending.**
The preceding tool-cache binary had identical bytes; tests were rerun because the
runtime path changed to the installed executable. Five new synthetic preflight
tests refuse default/existing/foreign connections, full-access mode and a parallel
launch before any provider/child starts. They are fixture tests, not phone results.
Fresh kernel network guard negatives pass. Initial fixture/probe failures (shell
quoting, missing write-half-close in the local RPC client, unset initial model,
`default` variant normalization) remain in evidence, not claimed as app failures.

The connection executable preserves the app's shell command/arguments and actual
permission path. A **private fixture executable shim** forwards native arguments
unchanged and sets the CLI's XDG config/data/cache/state to directories under the
fixture: the production bridge otherwise overrides XDG with its connection path.
Only the per-run control password is forwarded; synthetic provider config is local.
HOME, git project and CLI history are separate; no existing connection is accepted.
The app still creates its ordinary session-link/receipt files under the newly
selected test connection. It is created through the UI by the developer; setup
never reads or edits existing app connections. This is not filesystem isolation
or a full file-open trace. Provider requests are capped at 2 MiB and 512 scalar
metadata events, without saving HTTP bodies, prompts or keys. No cloud route,
MCP or plugin setup is included. Those areas remain excluded/unverified.

**Historical fixture revision:** the first preflight rejected all existing
connection directories. The subsequent phone screenshots exposed that limitation;
see the private-bridge correction below. Use the corrected launcher with the same
paths; do not delete or recreate the selected connection directory.

### Phone setup, in order

1. Create a **new OpenCode profile**, Ubuntu AI, Build, **С подтверждениями**.
   Leave **Ограничивать внешние пути · OpenCode** off for this test; the fixture
   refuses any different permission policy rather than weakening it.
2. Connection → Accounts and API keys → **+ Подключение**. Name it
   **AUDIT OpenCode**. Create, then **close** the key editor with no key and no
   model-catalogue request. Return to Connection and explicitly select that new
   name. Do not use Main or any existing connection. Opening the ordinary key or
   model setup before the guarded launch can create its connection directory;
   an already-existing directory is intentionally refused, not overwritten.
3. Keep model **По умолчанию** before first launch. Project:
   `/root/termux-fork/.verification/audit-opencode-phone/project`.
   Connection executable, no arguments:
   `/root/termux-fork/.verification/audit-opencode-phone/launch-opencode`.
   Save/reopen, check fields, then launch this test profile.
4. In the **running chat's OpenCode status**, open Model. Search `audit`, select
   **Synthetic audit / audit**, default variant, and confirm Apply. This uses the
   existing live proxy/model route tested above, not an unguarded settings query.
   Confirm the displayed provider/model is `audit/audit`. Do not send a test tool
   request before that selection. If it is missing, stop and report the safe error.
5. Send four messages, one at a time, checking the file after each:

| Message | UI answer | Expected file |
| --- | --- | --- |
| `AUDIT SHELL INSIDE 1` | Deny | `project/shell.txt` remains `AUDIT_BEFORE` |
| `AUDIT SHELL INSIDE 2` | Allow once | `project/shell.txt` becomes `AUDIT_SHELL_2` |
| `AUDIT WRITE INSIDE 3` | Deny | `project/write.txt` remains `AUDIT_BEFORE` |
| `AUDIT WRITE INSIDE 4` | Allow once | `project/write.txt` becomes `AUDIT_WRITE_4` |

In a separate ordinary **Ubuntu AI terminal**, check with
`/root/termux-fork/.verification/audit-opencode-phone/check-markers`.
Report intermediate denial checks explicitly; final marker values alone do not
prove them. Outside marker lines are only those files' displayed values, not
proof of broader preservation or an outside-project permission check.
Finish with the test session's gear → **Закрыть сессию** → confirm, wait for close.
No force-stop of the application or other sessions is needed. If startup offers
installation or any guard/model/approval check fails, go Back and report it;
do not install, bypass the guard, select Main, or use Full access to continue.
Evidence: `.verification/phone-permissions/opencode-host-inventory*.log`,
`opencode-installed-probe.log`, `opencode-installed-safety.log`,
`latest-opencode-verified-root.txt`, `opencode-phone-final-root.txt`,
`opencode-phone-final-enforcement.log`. Claude/Gemini fixtures remain unchanged.


## 2026-11-04 — OpenCode phone preflight corrected: private bridge state

Source of phone failure: developer screenshots `1001642848.jpg` /
`1001642849.jpg` (attachments `00ea8c3cb4c24bbab7a5441f1cdeda09` and
`06dfc9acd94748608c719721a5ca7b1e`). Settings visibly select AUDIT OpenCode, Build,
confirmations and the prepared project/launcher. The fixture raised
`Connection already exists: do not reuse a working connection` **before any agent
launch**. This is not a component-version, model or permission-test failure.

Confirmed cause: the fixture equated directory existence with a working connection.
Production `AiAgentsUi.query → AiAgents.call → ai-agents-control.js start` can create
a directory for an ordinary model-catalogue/configuration request, before inference.
The exact phone operation which created this particular directory is **unknown**;
its private files and credentials were not read. The standard app-name metadata
file was not visible from this environment; no broader private-data search followed.

Fix, **test infrastructure only**: keep the source connection ID, but redirect
only the production bridge's connection-directory argument into
`<fixture>/bridge/connections/<same ID>`. Compare parsed arguments to ensure that
session ID, model, mode, chat selection, variant, permission policy and scope remain
unchanged. Existing selected connection files are never inspected, adopted,
replaced or removed. CLI XDG and HOME remain private as before. This explicit
fixture argument redirection is not a change to the shipped app/storage protocol;
app profile context and the normal live control socket remain intact. Main
connection, a different fixture owner, full access and parallel launch remain
refused. Component checks are unchanged; no fallback or installation is introduced.

Checked anew **by the assistant**: five preflight tests, including original-file
bytes/mtime and argument-preservation assertions. The native probe now begins
with an existing source connection containing an artificial sentinel, reproducing
the condition rejected by the initial fixture. Four real shell/Write No/Allow once
cases pass; original source directory/sentinel remain unchanged. Same-fixture
reopen retains the native session/model, effective `ask`, marker values and no
pending/replayed action. Both stops are acknowledged; owned processes exit.
Evidence: `.verification/phone-permissions/opencode-private-state-safety.log`,
`opencode-private-state-restart.log`, and the native/restart proofs referenced by
`latest-opencode-verified-root.txt`. These are assistant results, not developer
phone acceptance. The earlier phone failure is retained, not counted as PASS.

The **same** phone launcher/project paths were refreshed in place after verifying
there is no claimed operation and all four markers still equal AUDIT_BEFORE.
Previous fixture identity is retained separately. No marker reset, app-profile
edit, original connection-file change or APK installation/build took place.
Keep the current TEST OPENCODE profile and AUDIT OpenCode connection. Close only
that failed test session through its gear, launch it again, then choose
`audit/audit` from the **running OpenCode status** model selector. Continue the
four documented messages with an independent marker check after each action.
Do not create another connection or delete the existing directory for this fix.
Phone startup/permission checks are still pending.


## Developer OpenCode phone acceptance and refusal-card defect — 2026-11-04

Source: developer phone messages after the corrected fixture: explicit confirmation
of shell Deny (AUDIT_BEFORE) and Allow once (AUDIT_SHELL_2), then «все проверил
работает» after the remaining Write sequence. Screenshots 1001642854/2855 show
Write INSIDE 3 denied and Write INSIDE 4 allowed once with the final synthetic reply.
Accept the four inside-project shell/Write Deny/Allow once checks as **developer
phone results**. Intermediate Write marker states are reported by completion of
the sequence, not independently captured by the assistant. No phone version
identity was newly measured. Outside operations/preservation, actual login,
Android/crash/Keystore and backup/restore remain open; shutdown is not inferred.

A separate confirmed UI defect remains: a denied tool is also shown as a failed
action and red session error, with native `aborted / Step interrupted`. Assistant
read-only inspection of **only the artificial fixture's** receipts and SQLite
message metadata found native tool error `aborted`, a cancel receipt for the same
tool/message/session, and an assistant-level step-aborted error. It did not read
working credentials or alter native history. Permission enforcement itself passed.
Only that proven same-action rejection should become neutral; unrelated aborts,
network/auth errors and cases without matching evidence must remain visible.


## Local OpenCode refusal presentation fix — 2026-11-04

The projection now checks native tool status `error`, structured error type
`aborted`, and a stored **cancel** receipt whose session ID, source tool ID and
source assistant-message ID match this exact action. No error-message substring
is used as proof. The assistant-level abort becomes neutral only when every tool
in that same message has the matching denial proof. Missing/foreign/stale/malformed
receipts, text-only aborts, mixed failures and API/network errors remain errors.
A missing/trimmed old receipt causes conservative fallback to the native error.

Proven cases project status `declined` and `actionOutcome=user_declined`; the UI
shows **Действие отклонено / Action declined** with neutral action styling. The
same stable history keys are retained. Native error details remain accessible
through tool details; full native error records are not edited or deleted. No
permission rule, confirmation action, authorization or existing profile is changed.

Checked anew by the assistant: 29 JVM tests (AgentChatViewTest 18,
CodexActionTimelineTest 11), four Node scripts including 15 negative correlation
variants, native OpenCode No/Allow once with new projection assertions, next
requests after refusal and cold reopen with both prior refusals neutral. Both
stops are acknowledged and owned launches exit. Read-only SQLite verification
confirms the two original assistant abort records still exist. Modern Lint:
0 errors, 236 warnings, all five modules analyzed. Existing unrelated Node chat,
recovery and receipt tests also pass. The native checks were repeated because
this projection changed; accepted phone permission-enforcement cases are not reset.

Evidence: `.verification/opencode-declined/summary.json`, `native.log`,
`native-originals.json`, Node/JVM/Lint reports. All are assistant results; the
corrected presentation is **not yet checked on the phone** and not in test7.
Include with the project templates in the next test APK after final APK checks;
no APK build/install, push or publication was performed for this change.


## Developer phone acceptance: Codex outside-project access — 2026-11-04

Source: the developer's phone report and pasted `check-boundary` output, not an
assistant-run fixture. All six documented outside-project shell read, direct
write/apply_patch and shell write No/Allow-once scenarios were reported working.
The supplied final check shows read-41 not transmitted, read-42 transmitted,
new-33/new-51 absent and new-34/new-52 with the expected synthetic values.
The final provider event is tool-result / SHELL_WRITE / 52, 88776 bytes.
Read-31/read-32 were not requested. The APK version was not independently
re-established by that message. Native TUI, legacy Termux Codex, manual CLI,
old-profile persistence and chat switching during approval remain separate.
This does not establish preservation of arbitrary outside files. See the
[current audit plan](SECURITY-AUDIT-STAGE2.md#пользовательская-приёмка-codex-вне-проекта--2026-11-04).


## Следующий сценарий телефона: Claude вне проекта — 2026-11-04

Подготовлен новый искусственный HOME/config/project. Существующие профили,
авторизация и прежний принятый Claude fixture не изменены. Провайдер локальный;
существующий kernel guard блокирует внешнюю сеть и чужие listeners. Это не
OS-песочница файлов. Нативная логика сценариев уже проверена помощником;
этот новый экземпляр на телефоне ещё не проверен. Повтор стенда не требуется.

1. Создать новый профиль `AUDIT BOUNDARY CLAUDE`: Claude Code, Ubuntu AI,
   новый чат, права `manual` / с подтверждениями. Не использовать acceptEdits.
2. Поле «Команда подключения»: `/root/termux-fork/.verification/audit-boundary-claude-phone/launch-boundary`.
3. Рабочая папка: `/root/termux-fork/.verification/rollback-live-actions/source/.verification/phone-permissions/claude-lw7nf55o/project`.
   Сохранить и повторно открыть настройки; агент, папка и права должны сохраниться.
4. Запустить и отправлять по одному сообщению из таблицы. Дожидаться окончания
   действия перед следующим. Подтверждать только точный искусственный путь.

| Сообщение | Ответ | Сверка после действия |
| --- | --- | --- |
| `AUDIT READ OUTSIDE 31` | Отклонить | `read-31.txt` → `false` |
| `AUDIT READ OUTSIDE 32` | Разрешить разово | `read-32.txt` → `true` |
| `AUDIT SHELL_READ OUTSIDE 41` | Отклонить | `read-41.txt` → `false` |
| `AUDIT SHELL_READ OUTSIDE 42` | Разрешить разово | `read-42.txt` → `true` |
| `AUDIT WRITE OUTSIDE 33` | Отклонить | `new-33.txt: absent` |
| `AUDIT WRITE OUTSIDE 34` | Разрешить разово | `new-34.txt: AUDIT_WRITE_34` |
| `AUDIT SHELL_WRITE OUTSIDE 51` | Отклонить | `new-51.txt: absent` |
| `AUDIT SHELL_WRITE OUTSIDE 52` | Разрешить разово | `new-52.txt: AUDIT_WRITE_52` |

После каждого действия в обычном терминале Ubuntu AI:

```sh
/root/termux-fork/.verification/audit-boundary-claude-phone/check-boundary
```

При отсутствии ожидаемого запроса или несовпадении остановить проверку;
сообщить номер шага, выбор и результат сверки, без секретов и рабочих журналов.
Штатно закрыть только этот тестовый агент через его шестерёнку, затем повторить
сверку. Не выполнять pkill/force-stop. Приёмка старых профилей и переключения
чата при prompt в этот сценарий не включена.


## Developer phone acceptance: Claude outside-project access — 2026-11-04

The developer explicitly accepted all eight phone scenarios and supplied the
checker output. Read-31/read-41 did not reach the local provider;
read-32/read-42 did. New-33/new-51 are absent; new-34/new-52 contain the expected
AUDIT_WRITE_34/AUDIT_WRITE_52 values. The final event is tool-result /
SHELL_WRITE / 52, 106562 bytes. This accepts Read, Write and Bash read/write
No/Allow once in the app chat. The assistant's separate read-only checker run
matched, but is not the source of the developer's UI acceptance.
This does not accept a project sandbox, managed native terminal, manual CLI,
legacy profiles, chat switching or crash recovery. Do not rerun the accepted
scenarios without changed inputs. The earlier Claude phone instructions above
are now historical; the next phone agent is Gemini.


## Следующий сценарий телефона: Gemini вне проекта — 2026-11-04

После принятого Claude следующий — Gemini 0.64.0. Подготовлены новые
искусственные HOME/config/project и локальный провайдер под прежним kernel
network guard. Старые тестовые/рабочие профили и авторизация не изменены.
Стендовые сценарии не повторялись: исходники helpers и production runtime
не менялись. Проверены только наличие путей, pins helpers и начальные маркеры.
Этот новый экземпляр на телефоне пока не принят.

1. Новый профиль `AUDIT BOUNDARY GEMINI`: Gemini CLI, Ubuntu AI, API,
   новый чат, режим `default` / с подтверждениями. Настоящий ключ не вводить:
   fixture задаёт искусственный ключ и только локальный провайдер.
2. «Команда подключения»: `/root/termux-fork/.verification/audit-boundary-gemini-phone/launch-boundary`.
3. Рабочая папка: `/root/termux-fork/.verification/rollback-live-actions/source/.verification/phone-permissions/gemini-ub9ef8tc/project`.
4. Сохранить, повторно открыть настройки и сверить значения. Затем запустить.
5. Сообщения отправлять по одному. После каждого ждать окончания действия
   и выполнить checker в обычном Ubuntu AI терминале.

| Сообщение | Что сделать | Что проверить |
| --- | --- | --- |
| `AUDIT READ OUTSIDE 31` | Запроса разрешения не ожидается | В подробностях инструмента `Path not in workspace`; read-31 false |
| `AUDIT WRITE OUTSIDE 33` | Запроса разрешения не ожидается | `Path not in workspace`; new-33 absent |
| `AUDIT SHELL_READ OUTSIDE 41` | Отклонить Shell | read-41 false |
| `AUDIT SHELL_READ OUTSIDE 42` | Разрешить разово Shell | read-42 true |
| `AUDIT SHELL_WRITE OUTSIDE 51` | Отклонить Shell | new-51 absent |
| `AUDIT SHELL_WRITE OUTSIDE 52` | Разрешить разово Shell | new-52 AUDIT_WRITE_52 |

```sh
/root/termux-fork/.verification/audit-boundary-gemini-phone/check-boundary
```

Read/Write проверяют **технический отказ пути**, не отказ пользователя.
Если карточки нет, но причина отличается от Path not in workspace, этот шаг
не принимается как защита. При неожиданном запросе Read/Write отклонить его
и остановиться для разбора. Read-32 и new-34 не запрашиваются в этом сценарии;
их false/absent не доказывают блокировку. Shell учитывается отдельно.

После всех шагов штатно закрыть только эту тестовую сессию через шестерёнку
и повторить checker. При ошибке прислать номер шага, выбор, безопасный текст
ошибки и сверку. Не завершать приложение целиком и не трогать рабочие чаты.
Этот сценарий не проверяет настоящий API/Vertex вход, handoff/crash или
восстановление Android.


## Developer phone acceptance: Gemini outside-project access — 2026-11-04

Accepted from the developer's report, checker output and screenshots after the
explicit-model repeat. The displayed/native model is gemini-2.5-flash-lite.
Read/Write failed with Path not in workspace, independently confirmed by the
assistant reading only this synthetic native history. Shell read/write No and
Allow once were accepted by the developer. Read-41 was not transmitted,
read-42 was; new-33/new-51 are absent and new-52 is AUDIT_WRITE_52.
The final supplied event is tool-result / WRITE / 33, 57049 bytes.
These are phone results supplied by the developer, separate from assistant tests.

The earlier auto/default attempt is not accepted: an auxiliary routing request
without tools consumed the legacy fixture operation before the main tool-enabled
request. No native tool ran then. A separate assistant probe reproduced that
fixture defect. Explicit 3.1 and auto-routing differ; do not infer that the whole
3.1 model lacks tools. See the current stage-two plan for evidence and the open
fixture limitation. Do not extend the successful explicit-model phone result to
auto/default, real API/Vertex sign-in, Android crash/handoff or a project sandbox.
The next phone agent is OpenCode; do not repeat accepted cases without changed
inputs. Earlier Gemini setup instructions above are now historical.


## Следующий сценарий телефона: OpenCode вне проекта — 2026-11-04

Подготовлена новая искусственная фикстура с прежним установленным OpenCode
2.0.28, отдельными HOME/XDG/bridge/history и локальным провайдером под kernel
guard. Рабочие подключения и авторизация не читаются. Ранее принятый OpenCode
inside fixture не изменён. Компоненты не устанавливались, guard готовности
приложения не ослаблен. Стендовые проверки не повторялись.

1. Новый профиль `AUDIT BOUNDARY OPENCODE`: OpenCode, Ubuntu AI, новый чат,
   Build, «С подтверждениями», флажок ограничения внешних путей **выключен**.
   Это проверка управляемого внешнего доступа, не режима запрета путей.
2. Создать новое именованное подключение `AUDIT BOUNDARY OpenCode` без ключа.
   Не выбирать основное/рабочее подключение. До guarded запуска не открывать
   самостоятельный каталог моделей через настройки подключения.
3. «Команда подключения»: `/root/termux-fork/.verification/audit-boundary-opencode-phone/launch-boundary`.
4. Рабочая папка: `/root/termux-fork/.verification/rollback-live-actions/source/.verification/phone-permissions/opencode-u0rr7sn1/project`.
5. Сохранить, повторно открыть настройки, сверить значения и запустить.
6. Уже после запуска открыть плашку состояния **этого агента** над чатом →
   Модель → Synthetic audit / audit → вариант default. Это локальный каталог
   работающего guarded сервера. Реального ключа/входа не требуется.

Сначала проверяются два native directory No, до любого разрешения каталога.
Затем проверяются конкретные инструменты. Сообщения отправлять по одному.

| № | Сообщение | Выбор | Сверка |
| --- | --- | --- | --- |
| 1 | `AUDIT READ OUTSIDE 31` | В native terminal отклонить external directory | read-31 false |
| 2 | `AUDIT WRITE OUTSIDE 33` | Отклонить external directory | new-33 absent |
| 3 | `AUDIT READ OUTSIDE 31` | Directory Allow once, затем read Отклонить в чате | read-31 false |
| 4 | `AUDIT READ OUTSIDE 32` | Directory Allow once, если спрашивает; read Разрешить разово | read-32 true |
| 5 | `AUDIT WRITE OUTSIDE 33` | Directory Allow once, если спрашивает; edit Отклонить | new-33 absent |
| 6 | `AUDIT WRITE OUTSIDE 34` | Directory Allow once, если спрашивает; edit Разрешить разово | new-34 AUDIT_WRITE_34 |
| 7 | `AUDIT SHELL_READ OUTSIDE 41` | shell Отклонить | read-41 false |
| 8 | `AUDIT SHELL_READ OUTSIDE 42` | shell Разрешить разово | read-42 true |
| 9 | `AUDIT SHELL_WRITE OUTSIDE 51` | shell Отклонить | new-51 absent |
| 10 | `AUDIT SHELL_WRITE OUTSIDE 52` | shell Разрешить разово | new-52 AUDIT_WRITE_52 |

Native directory prompt открывается **кнопкой агента в терминал**, а не отдельным
ручным CLI. Проверять точный путь `/root/termux-fork/.verification/rollback-live-actions/source/.verification/phone-permissions/opencode-u0rr7sn1/outside`. Для отказа выбрать Reject;
если откроется пояснение, отправить пустое пояснение для завершения отказа.
Для разрешения выбрать только Allow once, **не Always allow**. Затем нажать
«Вернуться в чат OpenCode»; read/edit подтверждать в чате отдельно. Если возврат
недоступен или вместо ожидаемого запроса другая ошибка — остановиться и сообщить
номер шага. Если следующий directory prompt не повторяется, отметить это отдельно;
не считать отсутствие карточки действия успешным отказом.

После каждого завершённого шага выполнить в обычном терминале Ubuntu AI:

```sh
/root/termux-fork/.verification/audit-boundary-opencode-phone/check-boundary
```

После всех шагов штатно закрыть только эту тестовую сессию через её шестерёнку,
затем снова выполнить checker. Не применять pkill/force-stop. Прислать сверку
и отдельно результаты native directory No и последующего read/edit No.
Текущая test7 может показывать отказ красной карточкой; исправление корреляции
отказа остаётся локальным и не заменяет проверку эффекта. Не повторять команды
из-за цвета карточки. Этот сценарий не подтверждает общий native TUI/ручной CLI
или OS-песочницу; он проверяет именно directory gate управляемой сессии и
конкретные действия через чат.


## Local two-stage denial projection fix — 2026-11-04

The developer accepted all ten OpenCode outside-project phone scenarios and
provided the final expected markers. Red error presentation is a separate issue.
Inspection of a copy of that synthetic history found a remaining local defect:
ReceiptStore is deliberately first-write-wins per key. OpenCode used the tool
call ID as the receipt key for both external_directory and read/edit permission.
Directory Allow once therefore occupied the key and the later action No was lost.
No action was executed after denial in the accepted phone scenarios.

The OpenCode event projection now keys each new receipt by native permission ID,
correlates both receipts to the same session/message/tool, displays both decisions
and uses an exact matching cancel proof for neutral “Action declined”. Legacy
receipts keyed by tool ID remain readable. The shared ReceiptStore and other
agents are unchanged. Unknown reply kinds are not treated as user rejection.
Native history, saved rights, existing authorization and CLI versions are untouched.
Old histories that already lost the second decision are not guessed from aborted
text: without a retained cancellation proof their error remains visible. No native
log is rewritten. This limitation needs a new post-update test for UI acceptance.

Checked anew because receipt projection changed: the baseline failed the new
regression (one receipt instead of two); the corrected test covers read/write,
directory No, directory Allow followed by action No, both allowed, cold reopen,
next request, legacy missing proof and eight foreign/invalid-event cases. Existing
same-action tests retain 15 unproven/unrelated/provider errors as errors. The
existing OpenCode chat, recovery and shared receipt suites also pass.
Four real installed OpenCode 2.0.28 runs cover read/write × directory No/action No
after directory Allow; each cold-reopens the same persisted native session, keeps
stable receipt rows and neutral error status, and sends no model request on reopen.
All owned launches and tracked descendants exit after acknowledged stops.
Fifteen backup-reader/legacy tests pass; these are not Android export/restore.

Evidence: `.verification/opencode-two-stage/summary.json`, baseline/unit/native
logs, per-root result.json/reopen-proof.json. Only fake data and a local provider
under the existing kernel guard were used. The copied phone history was read-only
and not modified. The new UI is not yet accepted on a phone. Include this fix and
both project templates in the next private test release APK; no publication.
