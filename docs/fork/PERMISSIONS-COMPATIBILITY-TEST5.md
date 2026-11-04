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
[current audit plan](PERMISSIONS-COMPATIBILITY-TEST5.md#пользовательская-приёмка-codex-вне-проекта--2026-11-04).


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


## История этапа 2: проход 2 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Единый актуальный план аудита — 2026-11-04

Это основной план оставшейся работы, составленный по существующим отчётам.
Исторические разделы ниже сохраняют результаты на дату своего прохода; текущий
статус задаёт эта таблица и последующие датированные уточнения. Уже выполненные
проверки не становятся новыми задачами. Новые повторы нужны только при изменении
значимых входов, обнаружении ошибки или уточнении непокрытого сценария.

**Разделение версий.** Опубликованная v2.1.2 — `c0073b462311bba5fd2dcc301b048d69e407f3f1`;
её APK и Components не исправлялись. Пользовательская приёмка Codex относится к
**test5 / 1481**, исходники сборки `757f45c9`, запись `0d28b77a`.
OpenCode исправлен **только локально** в `e676dd425c5eabeb8d364d30ab0de09e52e2de42`.
Текущая **test7 / 1483** сохраняет исправления test6 и добавляет управление
обновлениями/путём Gemini. Пользователь подтвердил API-тест с искусственным
ключом и штатный повторный запуск; остальные пункты телефона не завершены.
Идентичность и проверки текущего release APK: [отчёт test7](VALIDATION-2.1.3-test7.md).
Статусы «исправлено локально», «проверено на стенде» и «принято на телефоне» различны.

Проверяющий **помощник** работает с кодом/артефактами/изолированными фикстурами.
**Разработчик на телефоне** проверяет только отдельные тестовые профили и файлы;
завершение процесса допустимо на изолированном устройстве без рабочих сессий.
[Безопасный порядок](VALIDATION-2.1.3-test6.md#безопасное-завершение-процесса-для-проверки)
различает штатную остановку одного тестового агента и аварию процесса приложения.

| Пункт / приоритет | Текущий статус | Доказательства и версия/коммит | Оставшееся действие | Кто проверяет |
| --- | --- | --- | --- | --- |
| Идентичность APK и происхождение публичного signer | Публичная непрерывность подтверждена. Происхождение/хранение закрытого ключа не доказано. | v2.1.2 `c0073b46`, `identity.json`, `upstream-public-fingerprint-comparison.json`, `REPORT.md` в локальной `.verification/security-v2.1.2-c0073b46/`; SHA APK/сертификата ниже. | Не повторять сравнение старых APK. При отдельном расширении охвата получить описание владения/резервирования ключа без чтения ключа и паролей. | Разработчик предоставляет сведения; помощник сопоставляет публичные данные. Не блокирует текущий этап 2. |
| F-LOG-01 / P3, условно P2 | Исправлено локально: новый полный вывод подготовки не сохраняется; старые журналы оставлены. | `8c5f88e6`; [структурированный вывод](VALIDATION-preparation-structured-status.md), искусственные потоковые/process-тесты. В v2.1.2 дефект остаётся. | На тестовом устройстве проверить безопасную ошибку/повтор реальной подготовки; не читать и не удалять старые журналы. Не повторять пройденные secret/chunk fixtures. | Разработчик на телефоне; помощник разбирает безопасный результат. |
| F-LOG-02 / P3 | Исправлено локально в EnvironmentSetup, включая результат/UI/экспорт. | `9045f699`; [отчёт](VALIDATION-environment-structured-status.md), 69 JVM и 7 pipe-проверок, искусственные multiline/EOF-секреты. | Телефон: отчёт ошибки/повтор без сырого вывода. Проверка существующей Ubuntu из test1 не равна чистой установке. | Разработчик на телефоне. |
| F-NET-01 / P3 | Исправлено локально; очередь ждёт завершения своего child. | `ed59dbe9`; [9 TLS/process/queue сценариев](VALIDATION-preparation-download-bounds.md), stop-body и медленная успешная передача раздельны. | Мобильная сеть/реальная загрузка только в отдельной тестовой установке. 45 с connect, 180 с low-speed, 7200 с на попытку, 3 попытки — не timeout всей подготовки. | Разработчик на тестовом телефоне; помощник разбирает этап/код без секретов. Отдельно от завершения этапа 2. |
| F-SIGN-01 / P3 | Исправлено локально; отсутствие/несовпадение signer останавливает release. | `d3ea24ae`; [8 искусственных сценариев](VALIDATION-release-signer-gate.md); gate и публичная подпись проверяются на финальном test6. | Для каждого нового APK проверять его signer/package/versionCode. Не менять ключ или pin. Старые fixture-тесты без изменений не повторять. | Помощник. Не требует проверки приватного ключа пользователем. |
| A-ACCESS-01 + T-CODEX-START-01 / P2 и регрессия test4 | Локально исправлено. На test5 пользователь подтвердил новый запуск и No/Allow once для shell и прямого редактирования. | `752b1b1d`, `4d377c7f`, `db033888`; [матрица CLI](PERMISSIONS-COMPATIBILITY-TEST5.md), [приёмка test5](VALIDATION-2.1.3-test5.md#developer-phone-acceptance--2026-11-04). Codex 0.166.0. | Повтор этой приёмки не нужен без изменений пути. Не закрывать ею alternate policy, изоляцию или остальных агентов. | Уже проверено разработчиком; помощник сохраняет ограниченный статус. |
| Сохранение политики новых/старых профилей всех 4 агентов | На стенде пройдено: создание → save/reopen → штатный generated launch → effective policy. Старые профили не мигрируют. | `db033888`; matrix 40 + chain 25, повтор test6: `.verification/audit-test6/native-xml/`. Claude 2.1.296, Gemini 0.64.0, OpenCode 2.0.28. | На телефоне проверить старый тестовый профиль без изменения политики и новые Claude/Gemini/OpenCode. | Разработчик на телефоне. Повтор native-стенда не запланирован. |
| Отказ/разрешение shell и файловых инструментов Claude/Gemini/OpenCode | Стенд помощника: UI → bridge → native scheduler/tool → конечные байты, внутри/вне проекта. Телефон: пользователь явно подтвердил все четыре Claude/Gemini shell и Write No/Allow once внутри тестового проекта, включая проверку после каждого отказа; финальные маркеры присланы. | [Матрица](PERMISSIONS-COMPATIBILITY-TEST5.md), `.verification/security-stage2-followup/`, 65 свежих сценариев test6 под kernel guard. | Телефон: Claude и Gemini inside No/Allow once явно подтверждены разработчиком; не повторять. Четыре OpenCode inside также подтверждены последующим сообщением пользователя; новое отображение отказов, действия вне проекта и отдельная сверка внепроектных файлов открыты. Отдельно учитывать path-validator / external_directory; отсутствие карточки не считать успешным отказом. | Разработчик на телефоне. |
| A-ACCESS-02 / A-ISOLATION-01 / P2 | Ограничение архитектуры сохраняется: PRoot/папка проекта не создают отдельную границу UID. Не найден обход отказа в проверенных инструментах. | Раздел «Фактические права», native-пути внутри/вне; карта `NETWORK-ACCESS.md` первого этапа. | На телефоне уточнить реальные mounts/grants искусственными файлами, не обещать изоляцию. Отдельную OS-песочницу не внедряем в этом этапе. | Помощник анализирует результаты; разработчик проверяет доступ тестового профиля. |
| Gemini после test6: версия и runtime path | Исправлено локально. Пользователь test7 подтвердил запуск, HTTP 400 / API_KEY_INVALID, штатную остановку/повтор и отсутствие помехи updater. | `9b881f3a`, `e78f855d`, APK source `0a51e24c`; [приёмка test7](VALIDATION-2.1.3-test7.md#developer-phone-acceptance--2026-11-04). Это отчёт разработчика, отдельно от normal/alias fixtures. | Не повторять принятый штатный путь. Телефон: отдельно проверить информационный вид уведомления после reopen; путь normal/alias на телефоне не установлен. | Разработчик на телефоне; помощник фиксирует результат. |
| A-AUTH-01 / P3, Gemini key broker | Исправлено локально. Функциональный путь искусственного API-ключа принят пользователем на test7: запрос получает HTTP 400 / API_KEY_INVALID до и после штатного перезапуска. Полная приёмка broker не закрыта; новый Node-тест partial/stall/child-death пройден, см. датированное уточнение ниже. | `68413a86`, `687674b2`; fixtures и [пользовательская проверка test7](VALIDATION-2.1.3-test7.md#developer-phone-acceptance--2026-11-04). Это не независимая проверка содержимого ключа, peer credentials или Keystore. | Низкоуровневые Android handoff/ownership/crash не проверены и согласованно отложены: нет изолированного экземпляра. Принятый функциональный fake-key/API/retry не сбрасывается. Настоящий вход отдельно не проверен. | Разработчик на телефоне; помощник разбирает безопасный результат. |
| Gemini API/Vertex авторизация | Выбор режима/конфиг/отсутствие fallback подтверждены; искусственный API-ключ на test7 отвергнут с HTTP 400 / API_KEY_INVALID по отчёту пользователя. Настоящий вход не проверен. | Gemini 0.64.0, `test_gemini_vertex_auth.cjs`, `check-gemini-auth.js`, final-node test6 под kernel guard. | Отдельный тестовый аккаунт только если нужен реальный вход; Fake-key rejection уже принят; это не «успех входа». Не заменять рабочую авторизацию. | Разработчик на телефоне при наличии тестового аккаунта; без него статус остаётся «не проверено». |
| Восстановление процесса/контекста запуска | Broker owner death, fresh run, ciphertext save/reopen, dedup/retry до/после native dispatch пройдены на стенде. Пользователь test7 подтвердил только штатную остановку/повтор. Аварийный Android lifecycle не проверен. | Gemini broker/vault tests, Gemini/OpenCode chat recovery tests; `.verification/audit-test6/`. | Не проверено на Android и сейчас не выполняется по согласованию: отдельного экземпляра нет. Стендовые результаты сохранены; закрытие окна не считается crash. | Разработчик на изолированном телефоне. |
| A-BACKUP-01 / P2 | Незашифрованный явный экспорт остаётся возможностью; RU/EN предупреждение усилено. | `fbcfee27`; [состав](BACKUP-RESTORE.md), fake archive/restore и consent tests. | Телефон/SAF: предупреждение до экспорта, явное согласие, безопасное прерывание/нехватка места; только искусственные данные. Шифрование не заявлять. | Разработчик на отдельном тестовом экземпляре. |
| A-BACKUP-02 / P2, OpenCode | Исправлено локально; приёмка телефона не завершена. Выбор данных подключения отдельный, default off. | `e676dd42`; 89 Python, 41 JVM; два real OpenCode export → clean restore → save/reopen → continuation цикла. `.verification/opencode-backup-fix/`, [отчёт](BACKUP-RESTORE.md). | Телефон: RU выборочная форма частично показана на скриншотах пользователя; остальные ветви предупреждений/выбор, полная форма/EN и export/clean restore открыты; source-подключение не заменяется. | Разработчик на телефоне. Помощник не повторяет завершённые циклы без изменения входов. |
| Старые архивы и квитанции | Совместимость v1/v2 проверена настоящим helper-ом v2.1.2; новый v3 требует нового reader. Квитанции пассивны, one-time approval не переносится. | `e676dd42`, legacy/Python/native shell+Write результаты в `.verification/opencode-backup-fix/`. | Телефон: старый fake архив, восстановление с/без connection data; импорт не выполняет команду, следующий shell/write опять спрашивает. | Разработчик на чистом тестовом экземпляре. |
| A-UI-01 / P3 | OpenCode scope gate сохраняется отдельно; его native отказ проверен. | OpenCode 2.0.28; guarded native matrix. | Телефон: различить native external_directory и нашу карточку действия; не разрешать native gate автоматически. | Разработчик на телефоне. |
| F-TEST-NET-01 и O-NET-01 | Ранняя проба не подтвердила изоляцию; её исторический запрос неизвестен. Поздний kernel guard проверен и блокирует дополнительные подключения. | Раздел ранней пробы ниже, `.verification/security-stage2-followup/network-guard/`, свежие negative/native tests test6. | Не переобъявлять раннюю пробу безопасной. Полный трафик CLI оставить отдельному направлению; не делать новых внешних запросов ради завершения этого этапа. | Помощник сохраняет ограничения; нового повторения ранней пробы нет. |

### Штатные пути запуска и границы доказательств — 2026-11-04

Проверено заново помощником по исходникам `aaeecc6d`: таблица описывает
существующие пути, а не новые режимы. Рабочие CLI, настройки и авторизация
не читались и не запускались. Нативные проверки и сообщения разработчика
о телефоне учитываются отдельно; статический маршрут не является успешной
проверкой отказа. Версии исследуемой интеграции: Codex 0.166.0,
Claude Code 2.1.296, Gemini CLI 0.64.0, OpenCode 2.0.28.

| Путь | Кто задаёт политику | Кто принимает ответ пользователя и исполняет решение | Подтверждённый охват |
| --- | --- | --- | --- |
| Чат приложения: Codex в Ubuntu AI / сохранённой Ubuntu | Поля профиля → `CodexSessionShell.approval/sandbox` → launcher. Для `untrusted` CLI bootstrap использует поддерживаемый `on-request`, затем `CodexChat.params/verify` задаёт и сверяет typed thread policy `untrusted`, cwd и sandbox. | Android-карточка → контроллер Codex → RPC ответа app-server; исполнение и запрет — в native app-server/tool. | Внутрипроектные shell и прямое редактирование No/Allow once приняты разработчиком на test5. Это не проверка изоляции или всех возможных инструментов. |
| Чат приложения: Claude в Ubuntu AI / сохранённой Ubuntu | Профиль `claudeMode` (`manual`, `acceptEdits`, `plan`) → `--permission-mode` SDK-процесса. Дополнительно действуют native настройки CLI; поле интерфейса само по себе не доказывает запрет. | `can_use_tool` → Android-карточка → `control_response` allow/deny; Claude исполняет решение. Контроллер сверяет session, nonce и epoch. | Внутрипроектные Bash/Write No/Allow once приняты разработчиком на телефоне. |
| Чат приложения: Gemini в Ubuntu AI / сохранённой Ubuntu | Профиль `geminiMode` → `--approval-mode default/auto_edit/plan`, native config/scheduler. API/Vertex — выбор авторизации, не отдельная политика файлов. | Hook native confirmation → Android-карточка → `answerPermission` → исходный callback scheduler. Неподдержанный native запрос не разрешается автоматически. | Внутрипроектные shell/Write No/Allow once приняты разработчиком на телефоне. Чтение и доступ вне проекта требуют отдельных результатов. |
| Чат приложения: OpenCode в Ubuntu AI / сохранённой Ubuntu | Профиль Build/Plan, `aiAccess`, `aiProjectOnly` → `accessRules` → создание/PATCH native session permissions. | Native permission API → `OpenCodePermissions` → Android-карточка → reject/once в native API. `external_directory` учитывается отдельно от карточки инструмента. | Четыре внутрипроектных сценария приняты разработчиком. Нейтральное отображение связанного отказа исправлено локально в `aaeecc6d`; на телефоне новой сборки ещё не проверено. |
| Кнопка агента → штатный терминал Codex | Существующий app-server и thread; `codex --remote … resume <thread>`. Это не независимый запуск с настройками по умолчанию. | Native TUI и app-server. Чат откладывает открытие при выполняющемся запросе/ожидающем подтверждении. | Путь открытия прослежен по коду; отдельные No/Allow и границы доступа через TUI на телефоне не засчитаны. |
| Кнопка агента → штатный терминал Claude | Контроллер завершает свой SDK worker и открывает native TUI с тем же `--permission-mode`, моделью и выбранной историей. Возврат снова создаёт SDK worker. | Native Claude TUI; это другой обработчик подтверждений, чем Android-карточка. | Передача аргументов прослежена по коду; полный цикл TUI-отказа и возврата требует отдельной проверки. |
| Кнопка агента → штатный терминал Gemini | Тот же принадлежащий контроллеру native процесс и его approval mode; `terminal-open` переключает представление. | Native интерфейс Gemini и scheduler; Android hook относится к этому процессу, а не к произвольному CLI из shell. | Код пути проверен; чатовая приёмка не доказывает нажатия native TUI. |
| Кнопка агента → штатный терминал OpenCode | `mini --server … --session … --replay`, тот же сервер и session permissions. Закрытие frontend не создаёт другую сессию. | Native mini/server; отдельные native gates не заменяются общим разрешением Android. | По коду; отдельная телефонная проверка native gates остаётся открытой. |
| Обычная оболочка Ubuntu AI → ручной запуск любого из четырёх CLI | Пользовательские аргументы запуска, native config, переменные среды и defaults конкретного CLI. Такой запуск не читает поля прав профиля чата автоматически. | Собственный TUI/interactive API CLI. Android-карточки чата к произвольному процессу не подключаются. Headless-вызов зависит от native режима; отсутствие prompt не означает отказ. | Штатная оболочка существует. Фактическая политика ручного вызова и No/Allow не проверены; рабочие конфиги ради карты не читались. |
| Сохранённый профиль Codex непосредственно в Termux | Существующий dedicated launcher/app-server и сохранённые поля профиля; frontend `--remote`. Android chat bridge включается только для Ubuntu/Ubuntu AI. `untrusted` без chat bridge явно останавливает запуск. | Native Codex TUI/app-server; не Android-карточка Ubuntu-чата. | По коду. Старые профили не переключались. Совместимость/эффективная политика на телефоне требуют отдельного искусственного legacy-профиля. |
| Обычный терминал Termux → ручной CLI | Аргументы, native настройки и окружение установленного пользователем CLI. Профиль чата права не навязывает. | Сам CLI. | Не равнозначно поддерживаемому AI-профилю. Для Claude/Gemini/OpenCode валидаторы профилей требуют Ubuntu/Ubuntu AI; штатного прямого профиля Termux для них нет. Ручную установку или новый режим ради аудита не добавляем. |

**Источники карты:**
[SessionShell](../../app/src/main/java/com/termux/app/workspace/SessionShell.java),
[CodexSessionShell](../../app/src/main/java/com/termux/app/workspace/CodexSessionShell.java),
[CodexChatControl](../../app/src/main/java/com/termux/app/workspace/CodexChatControl.java),
[ClaudeCode](../../app/src/main/java/com/termux/app/workspace/ClaudeCode.java),
[GeminiCli](../../app/src/main/java/com/termux/app/workspace/GeminiCli.java),
[AiAgents](../../app/src/main/java/com/termux/app/workspace/AiAgents.java);
[codex-launch](../../app/src/main/assets/codex-launch.cjs),
[codex-chat](../../app/src/main/assets/codex-chat.cjs),
[claude-chat](../../app/src/main/assets/claude-chat.cjs),
[gemini-control](../../app/src/main/assets/gemini-control.js),
[gemini-live](../../app/src/main/assets/gemini-live.cjs),
[gemini-chat](../../app/src/main/assets/gemini-chat.cjs),
[ai-agents-control](../../app/src/main/assets/ai-agents-control.js),
[opencode-permissions](../../app/src/main/assets/opencode-permissions.cjs),
[opencode-session-settings](../../app/src/main/assets/opencode-session-settings.cjs).

Ручной запуск не обходит права ОС, но и не наследует обещания интерфейса чата.
PRoot и выбранный cwd сами по себе не изолируют проект. Разрешённое чтение вне
проекта фиксируется как ограничение режима; содержимое после явного отказа —
отдельный возможный дефект. При чтении сверяется передача искусственного маркера
в фактический запрос локальному провайдеру, а не только неизменность файла.

### Границы доступа: общая таблица четырёх агентов — 2026-11-04

**Источник всех результатов в таблице — самостоятельный стенд помощника,
не телефон разработчика.** Производственные assets — `aaeecc6d`, карта путей —
`d9cdbc9d`. Применены штатные bootstrap/controller/CLI и их RPC подтверждений,
но Android/proot transport и нажатия Android здесь не воспроизведены.
Использованы новые HOME/config/history и искусственные проекты; провайдер
локальный, внешняя сеть запрещена существующим kernel guard. Это не OS-песочница
файлов: тестовый процесс технически не изолирован отдельным UID.

Для каждого чтения создан отдельный файл с непредсказуемым искусственным маркером.
Маркер отсутствует в сообщении пользователю/провайдеру до выполнения инструмента.
Проверяется его наличие в фактическом следующем HTTP-запросе, включая поздние
запросы после отказа. Для записи проверены отсутствие нового файла после No,
точные байты после Allow и сохранность исходных файлов чтения. Разные маркеры
не позволяют принять старое разрешённое чтение за утечку нового отклонённого.
«Маркер передан» означает получение локальным провайдером; это не проверка
дальнейшего хранения или поведения облачной модели.

| Агент / версия | Инструмент вне проекта | No | Разовое разрешение | Вывод |
| --- | --- | --- | --- | --- |
| Codex 0.166.0 | Отдельный файловый Read | Не выполнялся | Не выполнялся | Отдельный Read не обнаружен в объявленном каталоге этого запуска. Это не запрет чтения: shell указан отдельно. Не заявляем покрытие других каталогов/режимов. |
| Codex 0.166.0 | `apply_patch` — новая запись | Карточка Files; файл не создан | Карточка Files; точные ожидаемые байты | Подтверждение действует для проверенного прямого изменения. |
| Codex 0.166.0 | `exec_command` — чтение | Карточка Command; маркер не передан | Карточка Command; маркер передан | Shell может читать вне проекта после разрешения. |
| Codex 0.166.0 | `exec_command` — запись | Карточка Command; файл не создан | Карточка Command; точные байты | Это подтверждения, не изоляция проекта. |
| Claude 2.1.296 | `Read` | Запрос Read; маркер не передан | Запрос Read; маркер передан | Отказ и разрешение учитываются отдельно от shell. |
| Claude 2.1.296 | `Write` — новый файл | Запрос Write; файл не создан | Запрос Write; точные байты | Проверена прямая запись. |
| Claude 2.1.296 | `Bash` — чтение | Запрос Bash; маркер не передан | Запрос Bash; маркер передан | Внешний доступ возможен после разрешения. |
| Claude 2.1.296 | `Bash` — запись | Запрос Bash; файл не создан | Запрос Bash; точные байты | Пройдено на стенде, не на телефоне. |
| Gemini 0.64.0 | `read_file` | Карточки нет; маркер не передан | Разрешение не запрашивается | Native `Path not in workspace` для обеих независимых попыток. Это техническая проверка пути Read, не пользовательский No. |
| Gemini 0.64.0 | `write_file` | Карточки нет; файл не создан | Разрешение не запрашивается | Native `Path not in workspace` для обеих попыток Write. Не ошибка запуска/тестового провайдера. |
| Gemini 0.64.0 | `run_shell_command` — чтение | Запрос Shell; маркер не передан | Запрос Shell; маркер передан | Ограничение файловых инструментов не ограничивает разрешённый shell. |
| Gemini 0.64.0 | `run_shell_command` — запись | Запрос Shell; файл не создан | Запрос Shell; точные байты | Не называть этот режим песочницей. |
| OpenCode 2.0.28 | `read` | Native external-directory No блокирует действие; после directory Allow once отдельный read No также не передаёт маркер | Directory Allow once → отдельный read Allow once → маркер передан | Два независимых уровня подтверждения. |
| OpenCode 2.0.28 | `write` — новый файл | Native directory No: файла нет; directory Allow once → edit No: файла тоже нет | Directory Allow once → edit Allow once → точные байты | Отказ каталогу не подменён отказом конкретному инструменту. |
| OpenCode 2.0.28 | `shell` — чтение | Запрос shell; маркер не передан | Запрос shell; маркер передан | В этом вызове отдельного directory gate не было; не переносить результат файлового gate на shell. |
| OpenCode 2.0.28 | `shell` — запись | Запрос shell; файла нет | Запрос shell; точные байты | Разрешённый shell имеет внешний доступ. |

**OpenCode: управляемый терминал проверен узко.** Для directory gate использован
настоящий `terminal-open` → native `mini` того же сервера/сессии. На экране
подтверждены `Access external directory`, точный искусственный каталог,
`Allow once / Always allow / Reject`. Reject выполнен через native Esc и
отправку пустого объяснения. Allow once выполнен только после проверки его
выделения; Always allow не выбирался. Затем frontend закрыт, а read/edit No/Allow
отправлены через штатный контроллер. Это стендовая проверка native directory
prompt, не приёмка телефона и не полный аудит действий через native TUI.

**Ограничения и приоритет:** в этих проверках не подтверждено выполнение или
передача маркера после отказа. Разрешённый доступ вне проекта — сохраняющееся
ограничение режима (A-ACCESS-02 / A-ISOLATION-01), а не найденный обход отказа.
Для Gemini подтверждено ограничение отдельных инструментов, не процесса.

Доказательства: локальная `.verification/phone-boundaries/summary.json`
(13 изолированных запусков, 34 записи операций, включая два невыполненных
отдельных Read Codex; это **не** «34 успешных теста»), `collect-results.py`,
`gemini-path-validation-final.json`, `enforcement-final/result.json` и
`result.json / boundary-observations.json / identity.json` перечисленных фикстур.
Сводка содержит SHA скопированных production assets и native binaries.
Сценарии: [probe_boundaries.py](../../scripts/tests/phone-permissions/probe_boundaries.py),
[boundary_provider.py](../../scripts/tests/phone-permissions/boundary_provider.py),
[boundary_fixture.py](../../scripts/tests/phone-permissions/boundary_fixture.py).
Codex завершён через принадлежащий фикстуре runner и существующий
`terminateTree`; отдельно подтверждён выход 32 известных PID/start identities.
Claude/Gemini штатно подтвердили stop и выход отслеженных потомков. У OpenCode
проверен stop/выход принадлежащего запуска; ранние записи содержат только
доступную на том проходе проверку потомков. Это не проверка crash Android.

Неудачные проходы сохранены отдельно и не засчитаны: Gemini-клиент стенда
преждевременно закрывал write-half сокета до асинхронного ответа; Codex получил
невалидный admission ID стенда (`msg_bound_…` вместо `msg_<32 hex>`);
OpenCode auxiliary title-запрос потреблял операцию провайдера без tool catalogue.
Также native Esc открыл форму объяснения отказа: до её отправки No не был завершён.
Исправлены только helpers; исходники приложения не менялись. После финальных
проходов добавлен внешний lock фикстуры; отдельная реальная flock-проверка
подтвердила отказ второму запуску до создания провайдера/CLI. Поведение native
инструментов этим изменением не меняется, повторять их ради lock не требуется.

### Пользовательская приёмка Codex вне проекта — 2026-11-04

Источник — сообщение разработчика о проверке на телефоне, не стенд помощника.
Разработчик подтвердил выполнение всех шести выданных сценариев: shell-read,
apply_patch/write и shell-write, каждый с отказом и разовым разрешением.
Присланная независимая итоговая сверка показывает:

- `read-41.txt`: маркер не передан локальному провайдеру; `read-42.txt`: передан.
- `new-33.txt` и `new-51.txt` отсутствуют.
- `new-34.txt` содержит `AUDIT_WRITE_34`, `new-52.txt` — `AUDIT_WRITE_52`.
- Последнее событие провайдера: `tool-result`, `SHELL_WRITE`, номер 52,
  88776 байт; наблюдавшийся маркер только `read-42.txt`.

Отказ/разрешение подтверждены сообщением разработчика; итоговые маркеры отдельно
подтверждены присланным выводом. Помощник не выполнял эти действия на телефоне.
`read-31/32` не запрашивались; их false не доказывает запрет отдельного Read.
Эта приёмка относится к чату приложения и искусственным путям именно этой
фикстуры. Она не подтверждает сохранность всех файлов вне проекта, native TUI,
старый Codex в Termux, ручной CLI, переключение чата при prompt, сохранение старых
профилей или Android crash/recovery. Версия APK этим сообщением повторно
не установлена; последняя известная выданная сборка — test7, не новая сборка.
Повторять эти шесть принятых сценариев без изменения значимых входов не нужно.

### Пользовательская приёмка Claude вне проекта — 2026-11-04

Источник — явное сообщение разработчика о прохождении восьми сценариев на
телефоне и затем присланный вывод `check-boundary`. Это не стенд помощника.
Приняты No/Allow once отдельно для Read, Write, Bash-read и Bash-write вне проекта.
Итоговая сверка: read-31/read-41 false, read-32/read-42 true; new-33/new-51
отсутствуют; new-34 = AUDIT_WRITE_34, new-52 = AUDIT_WRITE_52. Последний запрос:
106562 байта, tool-result / SHELL_WRITE / 52, видны только маркеры read-32/read-42.
Помощник отдельно перечитал только checker этой искусственной фикстуры:
его результат совпал. Он не выполнял действия пользователя и не проверял
Android UI самостоятельно.

Приёмка относится к чату приложения и конкретным искусственным файлам. Она
не доказывает OS-песочницу, сохранность произвольных внешних файлов, ручной CLI,
native TUI, сохранение старых профилей, переключение чата при prompt или crash.
Версия APK из этого вывода не устанавливается. Принятые восемь сценариев не
повторять без изменения значимых входов. Новое ограничение проектом — отдельная
задача; эта приёмка не разрешает включить соответствующую галочку.

### Claude: исследование ограничения проектом, отдельно от приёмки — 2026-11-04

**Решение разработчика:** работающее ограничение проектом, включая Bash, пока
не подтверждено. Галочка остаётся недоступной. Подтверждения Claude приняты
и не сбрасываются. Реализация нового ограничения, подготовка его зависимостей
и проверка sandbox — отдельная задача; не условие повторения пройденного аудита.
В исходниках `WorkspaceController` контроль «Работать только в пределах проекта ·
недоступно» уже disabled/unchecked. В рамках исследования он не менялся.

Проверено заново помощником только в новых искусственных HOME/config/project,
CLI Claude 2.1.296 с прежним SHA f1f6e96e0d8342b9dbf41d7e88255397a6a52ce3d8736ad6a4c6b59c9b62fefa,
Node 22.22.1, внешний сетевой доступ запрещён прежним kernel guard.
Рабочие настройки/авторизация, принятый телефонный fixture и APK не менялись.
Повтор был необходим из-за **других настроек**; обычные принятые подтверждения
повторно не тестировались ради новой отметки PASS.

1. В отдельном settings.json включены `permissions.blockReadsOutsideWorkingDirectories`
   и исследуемый `blockWritesOutsideWorkingDirectories`, sandbox выключен.
   Native Read двух внешних файлов технически отклонён с явным сообщением,
   называющим первый ключ; маркеры не дошли до провайдера. При этом разрешённый
   Bash прочитал внешний файл и передал его маркер, а разрешённые Write и Bash
   создали ожидаемые внешние файлы. Поэтому в этой версии/проверке второй ключ
   не установил запрет Write; считать его работающей политикой нельзя.
   Отсутствие его строки в binary само по себе не используется как доказательство.
2. Строгий native sandbox проверен отдельно с enabled=true,
   failIfUnavailable=true, autoAllowBashIfSandboxed=false и
   allowUnsandboxedCommands=false. Оба запуска — enableWeakerNestedSandbox=false
   и true — завершили native CLI с кодом 1 **до обращения к провайдеру**:
   `sandbox required but unavailable`, `socat not installed`.
   Это подтверждает отсутствие зависимости в PATH этих запусков. Это **не**
   доказательство несовместимости sandbox с Ubuntu AI, ядром или PRoot.
   Shell под этим native sandbox ещё не проверен, поскольку старт остановлен.
3. Дополнительная низкоуровневая bwrap-проба не заменяет проверку Claude:
   файловая маскировка скрыла искусственный внешний файл в child, сохранила
   чтение внутри проекта и исходный файл родителя. Видимые namespace IDs
   не изменились; вывод о полноценной OS-песочнице не делается.
   Проба с unshare-net остановилась на loopback/NETLINK под ограничивающим
   тестовым guard: её нельзя приписывать несовместимости Ubuntu AI.
   Первое обращение к отсутствующему `/proc/self/ns/user` было ошибкой измерения,
   сохранено отдельно и не засчитано проверкой sandbox.

**Корректное название существующего режима:** «С подтверждениями»; доступ вне
проекта возможен после разрешения. Read-clamp — ограничение отдельных файловых
инструментов, не песочница процесса. Факт принятого отказа не делает UI-галочку
технического ограничения готовой. Настоящий режим должен быть проверен отдельно.

**Порядок отдельной будущей задачи:** сначала подготовить недостающую зависимость
в изолированной тестовой среде, не в рабочем окружении; затем повторить строгий
startup и реальные разрешённые операции внутри/запрещённые снаружи, включая Bash,
дочерние интерпретаторы и прямые файловые инструменты. Проверить отсутствие
unsandboxed fallback и поведения после ошибки. Установка зависимости сама по себе
также не докажет работу ограничения. Сейчас установку не выполняли.

Доказательства: `.verification/claude-project-boundary/tool-clamps-proof.json`,
`native-startup-proof.json`, `platform-proof.json`, `platform-filesystem-proof.json`
и воспроизводящие `probe-*.py`; исходные отрицательные проходы сохранены.
CLI help и identity — в перечисленных fixture roots. После stop все отслеженные
потомки clamp-пробы завершились. Это стенд, не новая телефонная приёмка.

Документация upstream рассмотрена **отдельно** от фактической версии:
[Sandboxing](https://code.claude.com/docs/en/sandboxing) описывает sandbox shell
и отдельные границы файловых инструментов;
[Settings reference](https://code.claude.com/docs/en/settings-reference) описывает
read-clamp, failIfUnavailable и зависимости Linux. Текущая документация может
описывать более новые возможности, чем установленная 2.1.296; принятие ключа
настройки или ссылка на неё не заменяют native-проверку.

### Пользовательская приёмка Gemini вне проекта — 2026-11-04

Источник — сообщение разработчика «работает все», присланный checker и скриншот
двух tool-error карточек. Приняты шесть сценариев: Read/Write — технический отказ
пути; shell-read/shell-write — пользовательские No и Allow once. Итоговая сверка:
read-31/read-41 false, read-42 true; new-33/new-51 отсутствуют; new-52 содержит
AUDIT_WRITE_52. Read-32/new-34 в сценарии не вызывались, их false/absent не
доказывают ограничение. Последний присланный event: WRITE / 33 / tool-result,
57049 байт; набор переданных маркеров — только read-42.txt.

Помощник отдельно прочитал лишь native историю этой искусственной фикстуры:
после повторов есть настоящие read_file/write_file/run_shell_command;
оба файловых отказа прямо содержат `Path not in workspace`, а отмена shell —
`User denied execution`. Это подтверждает причину ошибок отдельно от картинки;
нажатия Android выполнил разработчик. На скриншоте и в native истории успешного
повтора модель **gemini-2.5-flash-lite**, а не первоначально предложенная
помощником gemini-2.5-flash. Название в отчёте берётся из фактических данных.

**Первоначальный проход не принят.** В auto/default с native ответом
Gemini 3.1 Pro Preview пользователь не видел prompts, а маркеры не передавались.
В native истории тогда не было tool calls: один BOUNDARY FINISHED не был
доказательством вызова или отказа инструмента. Read/Write того прохода также
не засчитаны. Помощник воспроизвёл причину отдельно: auto/default сначала
отправляет служебный generateContent к gemini-3.5-flash-lite с JSON response MIME,
без tools (4353 байта), затем основной streamGenerateContent к
Gemini 3.1 Pro Preview с полным каталогом tools (50953 байта). Старый тестовый
провайдер ошибочно помечал операцию выполненной уже на служебном запросе.
Это дефект маршрутизации **фикстуры**, не доказанный дефект подтверждений приложения.
Явный Gemini 3.1 Pro Preview в отдельном стенде не делал этого служебного запроса;
следовательно причина не сводится к запрету инструментов у всей модели 3.1.

Ограничение старого test provider для auto/default остаётся известным:
не переносить успешную приёмку explicit 2.5 Flash Lite на auto-routing и не
выдавать отсутствие вызова за защиту. Перед новым аудитом auto/default исправить
разделение служебных/model-tool запросов и проверить реальный native tool result.
Текущие и старые запущенные фикстуры, источники helpers и рабочие сессии во время
диагностики не менялись. Переход модели в тестовом чате выполнил разработчик.

Доказательства: `.verification/phone-boundaries/gemini-phone-acceptance.json`,
собственная native history фикстуры gemini-ub9ef8tc и
`.verification/gemini-boundary-routing/{observe-entry.py,probe-observed.py,probe-auto-observed.py}`,
логи и request-shapes.json перечисленных свежих roots. В observer сохранялись
только путь API без query, размеры, имена полей/инструментов и MIME, без тела
запроса и ключей. Стендовые observer-запуски штатно остановлены.

Приёмка не подтверждает песочницу Gemini: разрешённый shell читает/пишет вне
проекта, хотя файловые инструменты ограничивают путь. Настоящий API/Vertex вход,
Android handoff/crash, старые профили и управляемый native TUI остаются открытыми.
Принятые шесть сценариев не повторять без изменения значимых условий.

### Пользовательская приёмка OpenCode вне проекта — 2026-11-04

Источник — сообщение разработчика «все пункты сработали как надо» и присланный
checker после всех десяти шагов. Приняты directory No для чтения/записи;
после directory Allow — отдельные read/edit No и Allow once; shell read/write
No и Allow once. Итог: read-31/read-41 false, read-32/read-42 true; new-33/new-51
отсутствуют; new-34 = AUDIT_WRITE_34, new-52 = AUDIT_WRITE_52. Последний event:
SHELL_WRITE / 52 / tool-result, 33088 байт. Это пользовательская приёмка телефона,
не самостоятельно выполненные помощником нажатия. По одному checker она бы
не доказывала выбранные ответы; выбор подтверждён сообщением разработчика.

Красная карточка при отказе — отдельная A-UI-02. Помощник исследовал только копию
этой синтетической истории и её квитанций, без изменения native файлов. Четыре
из шести aborted записей имеют сохранённый cancel proof; у двух action No после
directory Allow старый first-write-wins ключ потерял второе решение. Их блокировка
принята пользователем и маркерами, но утраченная UI-квитанция не угадывается.
Новый локальный c653c620 сохраняет каждое решение по native permission ID.
Четыре свежих native read/write × directory/action No и cold reopen подтверждают
две записи для двух этапов, нейтральную проекцию и отсутствие повторного запроса
модели при reopen. Это не телефонная приёмка нового UI: она нужна в test8.

### Остаток телефона после завершения согласованного этапа

Новый test8 UI OpenCode и оба шаблона — следующая проверка разработчика.
Подробный короткий сценарий находится в [отчёте test8](VALIDATION-2.1.3-test8.md).
Принятые Codex/Claude/Gemini/OpenCode cases выше не сбрасываются.
Android crash/handoff/export/restore сейчас исключены по причине отсутствия
изолированного экземпляра; статусы и другие непроверенные маршруты перечислены
в итоговом разделе в начале документа. Следующие старые команды сохранены как
исторические инструкции к уже принятым проверкам, не как новое задание.

**Подготовленный Codex fixture:** новый HOME/config/project и локальный провайдер,
без рабочей авторизации; kernel guard разрешает только listeners этой операции.
Это конфигурационное разделение, не OS-изоляция файлов. На телефоне fixture проверил разработчик, см. приёмку выше.
Помощник отдельно выполнял только стенд. Запускатель не запускает рабочие профили.

Новый профиль `AUDIT BOUNDARY CODEX`: агент Codex, Ubuntu AI, новый чат,
«Чтение и запись», флажок «Подтверждать недоверенные команды» включён.
Сохранить профиль и повторно открыть настройки; эти значения должны сохраниться.

Поле **«Команда подключения»**:

```text
/root/termux-fork/.verification/audit-boundary-codex-phone/launch-boundary
```

**Рабочая папка** (точный путь, не каталог активного терминала):

```text
/root/termux-fork/.verification/rollback-live-actions/source/.verification/phone-permissions/codex-nswag3y2/project
```

После запуска отправлять по одному, дожидаясь окончания действия:

| Сообщение | Ответ в карточке | Независимая сверка |
| --- | --- | --- |
| `AUDIT SHELL_READ OUTSIDE 41` | Отклонить | `read-41.txt: content reached test provider = false` |
| `AUDIT SHELL_READ OUTSIDE 42` | Разрешить разово | `read-42.txt: content reached test provider = true` |
| `AUDIT WRITE OUTSIDE 33` | Отклонить | `new-33.txt: absent` |
| `AUDIT WRITE OUTSIDE 34` | Разрешить разово | `new-34.txt: AUDIT_WRITE_34` |
| `AUDIT SHELL_WRITE OUTSIDE 51` | Отклонить | `new-51.txt: absent` |
| `AUDIT SHELL_WRITE OUTSIDE 52` | Разрешить разово | `new-52.txt: AUDIT_WRITE_52` |

После **каждого** действия в обычном Ubuntu AI терминале выполнить:

```sh
/root/termux-fork/.verification/audit-boundary-codex-phone/check-boundary
```

`read-31.txt` и `read-32.txt` зарезервированы для отдельного Read и в данном
сценарии не запрашиваются. При отсутствии ожидаемой карточки, иной папке или
неверном результате остановить проверку и прислать номер шага, выбор и вывод
проверки. Секреты/рабочие журналы не присылать. Штатная остановка: шестерёнка
**этого** тестового агента → закрыть сессию; затем повторить команду сверки.
Не выполнять kill/pkill/force-stop и не останавливать рабочие чаты.

Шаблоны обоих агентов и исправление карточек OpenCode остаются в следующей
тестовой сборке после проверок. Вложения открыты, последние по приоритету;
MCP и плагины исключены и не проверены. Публикация не выполняется.

### Исключённые области и отдельные направления

- **A-DRIVER-01 / P2:** содержимое/исполнение драйверов и полный binary audit.
  SHA/ELF подтверждают целостность/формат, не безвредность. Следующий отдельный
  этап — помощник по коду/бинарникам и разработчик на тестовом устройстве; сейчас
  не запускать неизвестные `.so`. Источник: карта первого этапа.
- Полный сетевой трафик сторонних CLI, SDK, транзитивных библиотек и облачного
  провайдера. Текущая карта основных путей не является исчерпывающим перехватом.
- MCP и плагины: исключены из текущего объёма аудита и работ, **не проверены**; импорт настроек не делает их доверенными.
- Хранение/резервирование закрытого ключа подписи, аппаратные гарантии Keystore,
  независимый внешний аудит, враждебные процессы того же UID и разработка новой
  OS-песочницы. Никаких гарантий по отсутствию секретов или компрометации не дано.
- Чистая установка всех окружений, медленная реальная мобильная сеть и driver/GPU
  регрессии — отдельная приёмка, не следуют из проверки существующей Ubuntu.
- Накопление вложений/обрывы — **отдельная незакрытая задача**, без исправления
  в test6. [План и baseline](MEDIA-CONTEXT-INVESTIGATION.md): PNG/JPEG/WebP,
  кадры видео, большие тексты/документы, tool/MCP results, бинарные карточки и
  последующее чтение; Codex сначала, затем отдельные Claude/Gemini/OpenCode.
  Помощник готовит измеряемый кандидат; разработчик проверяет прежний внешний
  маршрут/повторное открытие. Локальный provider не заменяет эту приёмку.
- Рабочие чаты, авторизация, старые журналы, опубликованные APK/теги/Components:
  не обследуем и не изменяем без отдельной необходимости/авторизации.

### Частичная пользовательская проверка резервной копии — 2026-11-04

По уже присланным скриншотам и уточнению разработчика засчитан только частичный
просмотр **выборочной формы резервного копирования на русском языке** на телефоне.
Это сведения пользователя, не самостоятельно выполненная проверка помощника.
Скриншоты не подтверждают экспорт, содержимое архива, восстановление, отсутствие
секретов, совместимость старых архивов или пассивность квитанций. Не установлены
поведение checkbox при новом открытии и все ветви предупреждений. Полная форма,
EN, SAF, ошибки/отмена экспорта и чистое восстановление остаются открытыми.
Повторять уже показанное оформление RU без изменения интерфейса не требуется;
следующая проверка должна покрыть перечисленные непоказанные действия.

### Подготовленное подключение следующей проверки

Подключение для Gemini shell и прямого write внутри тестового проекта подготовлено
в `b08013be`. Четыре native No/Allow once сценария проверены помощником, без Android
UI. Затем пользователь сообщил прохождение этих четырёх шагов на телефоне и
прислал итоговые маркеры. Источники результатов различаются, см. приёмку ниже.
[Точные поля, сообщения, проверка файлов и штатная остановка](PERMISSIONS-COMPATIBILITY-TEST5.md#2026-11-04--prepared-phone-fixture-gemini-permission-buttons).
Рабочие профили не используются; новый HOME и история отдельные. Это не
файловая песочница ОС. Полный file-open trace не снят. Пустое поле ключа нового
профиля намеренно оставляет broker/Keystore вне этой проверки.

Сложные проверки Android/восстановления организуются отдельным проходом:
сначала отдельный Android-профиль или тестовое устройство с чистыми app data и
только искусственными профилями; затем фиксация UID/package/version и состава
тестовых данных; только после этого controlled app death, Keystore/ownership,
SAF и восстановление fake архивов v1/v2/v3. На активном экземпляре такие действия
не выполняются. Отдельный Android стенд пока не подготовлен; эти пункты открыты.

### Пользовательская приёмка Gemini permissions внутри проекта — 2026-11-04

Источник — **сообщения разработчика о телефоне**: после четырёх сообщений AUDIT
он написал «все прошло» и прислал вывод check-markers. Принимаются shell
отказ/разовое разрешение и прямой Write отказ/разовое разрешение внутри
искусственного проекта. Итог: shell `AUDIT_SHELL_2`, write `AUDIT_WRITE_4`.
Сам вывод финальных файлов не доказывает промежуточные отказы; их источник —
сообщение разработчика о прохождении последовательности. Помощник не наблюдал
эти нажатия и не выдаёт свой стенд за телефон.

Строки outside показывают `AUDIT_BEFORE` только у двух названных тестовых файлов
на момент вывода. Отдельная пользовательская сверка идентичности/исходных байтов
внепроекта не описана: сохранность остальных файлов и общая изоляция не приняты.
Действия вне проекта не выполнялись этой последовательностью. Настоящий вход,
Keystore, аварийное восстановление, backup/restore и остальные агенты не закрыты.
[Подробный источник и границы](PERMISSIONS-COMPATIBILITY-TEST5.md#developer-reported-gemini-phone-acceptance--2026-11-04).

### Следующая проверка телефона: Claude, затем OpenCode

Для Claude 2.1.296 подготовлено отдельное локальное подключение. Помощник проверил
четыре новых native Bash/Write No/Allow once сценария, конечные байты и штатный
stop/exit. Подготовлена свежая папка для телефона. Затем пользователь прислал итоговые
маркеры: shell `AUDIT_SHELL_2`, write `AUDIT_WRITE_4`. Это частичная проверка
телефона по его выводу; промежуточные No затем явно подтверждены разработчиком для Claude и Gemini.
[Поля и четыре сообщения](PERMISSIONS-COMPATIBILITY-TEST5.md#2026-11-04--prepared-phone-fixture-claude-permission-buttons).
Следующим будет OpenCode. Рабочие профили/авторизации и готовый Gemini fixture
не меняются. Внепроектные действия, Android/Keystore/crash/restore остаются открытыми.

### Частичный результат Claude на телефоне — 2026-11-04

Источник: присланный разработчиком вывод check-markers подготовленного Claude
fixture. Подтверждены ожидаемые итоговые значения shell/write после шагов с
разовым разрешением. Сам финальный вывод не доказывает отказы; последующее явное сообщение
разработчика подтверждает проверку файлов после каждого отказа у Claude и Gemini. Стенд помощника остаётся
самостоятельным источником, а не пользовательской приёмкой. Значения двух outside
маркеров записаны без вывода о сохранности остальных файлов или об изоляции.
[Точный вывод и границы](PERMISSIONS-COMPATIBILITY-TEST5.md#developer-provided-claude-final-marker-output--2026-11-04).
Разрешённые записи повторять без новой причины не требуется. OpenCode, действия
вне проекта, авторизация, Android lifecycle и восстановление остаются открытыми.

### Уточнение текущей приёмки — Claude и Gemini

Разработчик явно сообщил, что на телефоне прошёл все четыре сценария обоих
агентов и сверял файл после каждого отказа и разрешения. Источник — его сообщение,
а не стенд помощника. Эти inside-project проверки приняты; прежняя неопределённость
промежуточных отказов Claude снята. [Точный источник и ограничения](PERMISSIONS-COMPATIBILITY-TEST5.md#developer-confirmed-claude-and-gemini-four-step-phone-acceptance--2026-11-04).
Следующий пункт — OpenCode на отдельном тестовом подключении. Внепроектные
операции, реальная авторизация, Android/Keystore/crash и backup/restore открыты.

### OpenCode: установлен, тестовый запуск подготовлен

Помощник выполнил штатную read-only проверку: OpenCode 2.0.28 уже установлен через
npm fallback; Node готов. Ничего в общую Ubuntu AI не устанавливалось. Проверка
компонентов не изменена и не обходится. [Точный путь/хеш, стенд и порядок телефона](PERMISSIONS-COMPATIBILITY-TEST5.md#2026-11-04--prepared-opencode-phone-fixture-component-guard-retained).
Четыре новых native No/Allow once сценария и 5 отказов preflight пройдены
помощником, не пользователем. Для телефона подготовлены свежие маркеры, новый HOME
и XDG, запрет основного/существующего подключения и внешней сети. Приложение должно
создать новое именованное тестовое подключение; рабочие подключения не используются.
Телефон OpenCode, действия вне проекта, авторизация, Android/Keystore/crash и
backup/restore остаются открытыми. MCP/плагины исключены, вложения отложены.

### OpenCode: отказ первоначального стенда на телефоне и локальное исправление

Источник — скриншоты разработчика: профиль выбрал AUDIT OpenCode, но первоначальный
стенд отказал из-за существующего каталога подключения. Это **не пройденный запуск**
или проверка разрешений. Кто создал каталог на телефоне, не установлен; приватные
файлы не читались. Само условие отказа установлено по коду/скриншоту и воспроизведено
на искусственном существующем каталоге.

Исправлен только стенд: весь bridge-state направлен в отдельный каталог фикстуры;
исходный каталог подключения не читается и не меняется. Модель/права/сессия и
проверки компонентов сохранены. Помощник заново проверил четыре native No/Allow
once, пять preflight случаев и повторное открытие/stop с сохранением исходного
sentinel. Это не приёмка телефона. Прежние пути запускателя/проекта сохранены;
нужно повторить только заблокированный OpenCode запуск и его четыре шага.
[Причина, ограничения и точный повтор](PERMISSIONS-COMPATIBILITY-TEST5.md#2026-11-04--opencode-phone-preflight-corrected-private-bridge-state).
Claude/Gemini не повторять. Внепроектные действия, Android/crash/Keystore/backup,
MCP/плагины и вложения остаются в прежних статусах.

### OpenCode: пользовательская приёмка и отдельный дефект отображения

Разработчик подтвердил четыре inside-project проверки OpenCode на телефоне:
shell и прямой Write, отказ и разовое разрешение. Источник — его сообщения и
скриншоты, не стенд помощника. После отказов видны лишние красные карточки
`aborted / Step interrupted`: это отдельный дефект представления A-UI-02 / P3,
а не непройденный отказ. Исправление должно опираться на квитанцию того же
действия/сообщения/сессии, а не скрывать все aborted. [Источник и границы](PERMISSIONS-COMPATIBILITY-TEST5.md#developer-opencode-phone-acceptance-and-refusal-card-defect--2026-11-04).
Пройденные четыре шага не повторять без изменения пути. Проверка нового отображения
на телефоне остаётся отдельной; внепроекта, Android/Keystore/crash/backup, MCP/плагины
и вложения этими результатами не закрыты.

### Текущий порядок и проверка телефона — 2026-11-04

1. **Самостоятельно выполнено, новое покрытие A-AUTH-01:** зависший ответ брокера
   после части пакета и смерть принадлежащего фикстуре брокера после приёма части
   пакета потребителем. Безопасный отказ, закрытие клиентского сокета, независимый
   запуск и новый успешный запуск подтверждены датированным уточнением ниже.
   Повтор не требуется без изменения значимых входов.
2. **На телефоне:** новое отображение отказов OpenCode после исправления;
   Claude/Gemini/OpenCode вне проекта и отдельная сверка внепроектных файлов; затем Gemini отмена/повтор
   и аварийное восстановление, затем backup/SAF/restore. Пройденные Claude/Gemini inside
   из последующей явной пользовательской приёмки не повторять.
   Уже принятые Codex test5 и обычный Gemini test7 повторять без изменений не нужно.
3. **После приёмки:** обновить матрицу по фактическим результатам. Настоящий вход
   Gemini API/Vertex либо подтверждается отдельным тестовым аккаунтом, либо остаётся
   явным ограничением охвата. Нельзя закрыть его fake-key HTTP 400/401.

**Короткие сценарии для разработчика.** Только отдельные тестовые профили и
искусственные файлы. Никаких рабочих ключей, проектов или авторизаций в отчёте.

| Действие | Ожидаемый результат | Что сообщить при ошибке |
| --- | --- | --- |
| Для Claude, Gemini и OpenCode создать тестовый профиль с подтверждениями; сохранить и открыть повторно. | Выбранная политика сохраняется. Старые профили не меняются. | Агент/версия, политика до/после, безопасный снимок настроек. |
| В отдельной папке создать файл-маркер. Попросить shell изменить его; нажать «Отказать». Затем повторить с разовым разрешением. Повторить прямым файловым инструментом. | No оставляет файл неизменным; Allow once выполняет действие; следующее отдельное действие снова спрашивает. | Инструмент, было ли окно согласия, выбранный ответ, изменился ли маркер. Без полного приватного журнала. |
| Повторить для искусственного файла в соседней тестовой папке вне проекта. | Технический запрет пути допустим и фиксируется отдельно. Если действие доходит до карточки, No блокирует запись. | Gemini path-validator / OpenCode external_directory или наша карточка; не считать отсутствие карточки проверкой нашего отказа. |
| В новом тестовом API-профиле Gemini начать запуск с искусственным ключом и отменить; затем запустить заново. | Нет второго параллельного запуска или зависшего состояния. Если запрос достиг провайдера, искусственный ключ отвергается. | На каком этапе отмена, состояние после неё и безопасный код ошибки. Это не проверка midpoint handoff без инструментирования. |
| **Только в изолированном экземпляре без рабочих сессий:** завершить приложение через системное управление приложениями во время тестового запуска; открыть и повторить. | Сохранённый профиль доступен, запуск не дублируется, чужая авторизация не используется; искусственный ключ снова отвергается. | Когда завершён процесс, восстановился ли профиль, число запусков, безопасный код ошибки. Штатная остановка не заменяет crash. |
| Экспортировать fake backup сначала без OpenCode connection data, затем с явным выбором. Проверить предупреждение до экспорта. | RU/EN предупреждают о незашифрованных секретах и неполном восстановлении при исключении. Выбор connection data изначально выключен. | Язык/форма, видимость предупреждения, состояние checkbox. Сам архив с секретами не присылать. |
| В чистом тестовом экземпляре восстановить новый fake архив и сохранённый fake v1/v2. Открыть профиль и историю; запросить новое действие. | Источник не заменяется; квитанции не выполняют старые команды; разовое разрешение не переносится; новое действие спрашивает. | Формат архива, состав выбранного восстановления, результат открытия и повторного согласия. |

Для проверки инструментов нужно работоспособное **тестовое** подключение, которое
может запрашивать эти инструменты. API_KEY_INVALID не позволяет проверить отказ
инструмента. Если такого подключения нет, отметить сценарий недоступным; рабочий
профиль и его авторизацию не подменять. Если нет отдельного экземпляра без рабочих
сессий, проверку гибели процесса и чистое восстановление **не выполнять**: это
остаётся условием отдельного безопасного стенда. Low-level Android peer credentials,
Keystore и точный midpoint handoff требуют инструментирования помощником на таком
стенде; ручная отмена их не доказывает.

**Отдельные задачи:** шаблон инструкций уже проверен для Codex → AGENTS.md и
Claude → CLAUDE.md, локальный `8eedf416`; включить в следующую тестовую сборку.
Он не является находкой аудита. Исследование вложений зафиксировано отдельно
в `9c126a8e`, задача открыта. Возобновить её в следующем цикле после стабильного релиза;
сейчас матрицу медиа, MCP и плагинов не расширять. Никакой публикации.

### Условия завершения текущего этапа 2

1. Все входящие исправления имеют локальные коммиты и проверенный финальный
   тестовый release APK; нет известных критических регрессий/утечек или
   неподтверждённой безопасности завершения затронутых процессов.
2. Получены и записаны пользовательские permission-проверки четырёх агентов,
   включая отдельные подтверждённые внепроектные сценарии. Функциональная
   передача fake API-ключа Gemini, отказ API и штатный повтор остаются принятыми.
   По последнему соглашению Android handoff/crash и device export/restore сейчас
   не выполняются: нет отдельного экземпляра без рабочих данных. Они явно
   остаются Not checked; старые test6/test7 checklist считаются историческими,
   а не обязательным поручением трогать рабочее приложение.
3. Для реальной API/Vertex-авторизации указан успешный тестовый вход либо
   **не проверено** с причиной и явно согласованным ограничением охвата. Fake
   provider/error и JVM не закрывают настоящий аккаунт или Android Keystore.
4. Архитектурные ограничения остаются явно принятыми ограничениями; нерешённые
   дефекты имеют приоритет/ответственного/следующее действие. Исключения выше
   не объявляются PASS. Приёмка Codex не распространяется на других агентов.
5. Публикация не входит в завершение этого этапа и требует отдельного разрешения.

**Текущий этап согласован к завершению с ограниченным охватом, указанным в начале
документа.** Обязательная упаковка/проверка test8 подтверждается его отчётом;
новое отображение и шаблоны ожидают приёмки телефона. Непроверенные области
не стали успешными тестами и остаются явными ограничениями.
Первый этап закончен как исследование с ограниченным охватом; его исторические
выводы не переписываются. Настоящий документ фиксирует последующие исправления.


## История этапа 2: проход 3 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Исходный проход этапа 2 (исторический)

Дата: 2026-11-03. Это исследование помощника по исходникам, искусственным
фикстурам и настоящим CLI. Независимый аудит и проверка телефона не проводились.
Исходный отчёт этого этапа сохранён в истории коммита `8a43d391`.


## История этапа 2: проход 4 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Версии и границы результата

- Опубликованная v2.1.2:
  `c0073b462311bba5fd2dcc301b048d69e407f3f1`.
- SHA-256 её APK:
  `2f8b97b2d9083a5b7dcc3966eca54ea6d843fec3e253ba6e279c123921eeddab`.
- SHA-256 публичного сертификата:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Исходная локальная версия перед этапом:
  `307120a33c1d3f88eab5d37106fd72cdb9973c5b` (2.1.3-test3).
- Усиление только новых профилей Codex: `752b1b1d`.
- Одноразовый канал Gemini вместо файла:
  `68413a8686fcf2c0a3507f4db53d2b4522c97391`.
- Предупреждение о незашифрованной копии:
  `fbcfee27a18685d0a3249d59805cd0f0c493399d`.

Правки сохранены локально на `work/rollback-live-actions`. APK с ними не собирался.
Опубликованные APK, тег, Components и настройки рабочих агентов не изменены.
В исследовательских командах не открывались настоящие авторизации и ключи.
Для раннего CLI полного file-open trace нет: отсутствие дополнительного чтения
не доказано. Искусственный HOME не считается файловой изоляцией ОС.
Ограничения ранней сетевой пробы описаны ниже, без заявления о минимальном составе
переданного запроса. Полный трафик обычных CLI, драйверы, MCP и плагины исключены
из этого этапа.

Доказательства первого прохода: `.verification/security-stage2/`.
Доказательства продолжения: `.verification/security-stage2-followup/`.
Это локальные каталоги активного checkout. Манифесты фиксируют исходники,
исполняемые файлы, параметры и результаты. Они не заменяют проверку устройства.


## История этапа 2: проход 5 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Итоговая матрица

P2 — значимый риск доступа/раскрытия. P3 — более узкий риск жизненного цикла
или ограничение интерфейса. Приоритет относится к описанному пути.

| ID | Приоритет | Подтверждено | Текущее состояние |
| --- | --- | --- | --- |
| A-ACCESS-01 | P2 | В v2.1.2 новые Codex-профили получали `full` / эффективное `never`. Рабочая папка не создаёт изоляцию. | Локально новые профили используют native `untrusted`. Пользователь подтвердил на test5 запуск нового Codex и отказ/разовое разрешение shell и прямого редактирования. Это не проверка изоляции или остальных агентов. Старые профили не мигрируют. |
| A-ACCESS-02 | P2, архитектура | Разрешённые инструменты Codex, Claude и OpenCode могут работать вне проекта. Shell Gemini также может писать вне рабочей области. | Сохраняется. Подтверждения CLI не заменяют отдельную границу ОС. Обход отказа в проверенных операциях не обнаружен. |
| T-GEMINI-UPDATE-01 | P3, управление версией/UI | Gemini 0.64.0 по умолчанию проверяет и автоматически устанавливает обновление; ошибка updater становится native error item. В test6 это сообщил пользователь. | Исправлено локально после test6: auto install выключен, version check сохранён, updater items отделены по происхождению. Телефонная проверка ожидается; сам факт красной плашки не доказал блокировку запуска. |
| T-GEMINI-PATH-01 | P3, условный отказ канала | Логический runtime/HOME alias не совпадает с canonical module URL, поэтому loader может не подключить hooks. | Исправлено в контроллере. Обычный путь и ссылка × первый/повторный реальный CLI прошли; при отсутствии UI handlers запрос явно отклонён. Наличие ссылки на телефоне не установлено. |
| A-AUTH-01 | P3 | В опубликованном коде crash до чтения Gemini handoff оставлял приватный plaintext-файл. | Для новых локальных запусков файловый handoff убран. Проверены принадлежность запуску, ошибки и отмена. Старые остатки не обследовались/не удалялись; Пользователь test7 подтвердил функциональный fake-key API путь и штатный повтор. Android ownership/Keystore, авария и настоящий вход не закрыты. |
| A-BACKUP-01 | P2, явный экспорт | HOME-архив может содержать авторизации CLI, SSH-ключи и `.env` без шифрования. | Формат не изменён. RU/EN предупреждение явно говорит о незашифрованной копии и секретах. Это подтверждаемое действие пользователя. |
| A-BACKUP-02 | P2, восстановление | В двух проверенных режимах библиотека сохраняет connection ID OpenCode, но приватные config/data подключения отсутствуют в архиве и восстановленной папке. | В v2.1.2 и test5 сохраняется. Исправлено локально коммитом `e676dd42`, после test5; два чистых native export/restore/profile-цикла и совместимость v1/v2 пройдены. Квитанции пассивны, разовое разрешение не переносится. Приёмка телефона/SAF и реальной авторизации не завершена. Проверка тестового APK фиксируется отдельно. |
| A-UI-01 | P3, граница интерфейса | OpenCode `external_directory` остаётся отдельным native-запросом; наша карточка его не подтверждает. | Сохраняется намеренная граница. Отказ native-запроса проверен. Наши разрешения файлов вне проекта проверены после отдельного разового native-согласия. |
| O-NET-01 | P3, наблюдение | При запуске/инвентаризации OpenCode наблюдались попытки соединения с localhost 1234, 11434, 8000 помимо выбранного провайдера. | В финальном стенде заблокированы до соединения. Обычное приложение не менялось; состав остальных сетевых обращений не объявлен проверенным. |
| T-CODEX-START-01 | P1, регрессия test4 | Новый профиль добавлял CLI `approval_policy="untrusted"`; Codex 0.166.0 завершался до сокета. Ранний RPC-стенд обходил этот этап. | Исправлено в test5: поддерживаемый bootstrap и проверенная thread policy. Пользователь подтвердил запуск нового Codex и shell/direct-edit No/Allow на телефоне. Регрессия закрыта для этого пути test5; не приписывать её опубликованному v2.1.2. |
| F-TEST-NET-01 | P2, стенд | Ранняя проба OpenCode не обеспечила сетевую изоляцию; зарегистрирован provider HTTP 403. Историческое тело запроса неизвестно. | Проба исключена из подтверждений изоляции. Последующие CLI запущены под проверенным kernel-фильтром; ранняя неопределённость не устранена задним числом. Это дефект стенда, не установленная уязвимость APK. |

Динамические проверки относятся к Codex 0.166.0, Claude Code 2.1.296,
Gemini CLI 0.64.0 и OpenCode 2.0.28. Начало затронутого диапазона до v2.1.2
не устанавливалось. Исправление в Git не означает исправление опубликованного APK.


## История этапа 2: проход 6 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Фактические права и инструменты

| Агент | Настройка | Реальная операция и отказ | Границы доказательства |
| --- | --- | --- | --- |
| Codex | Старые профили сохранены; новые `untrusted` | Native `exec_command` и `apply_patch`: отказ не создал новый внешний файл и сохранил байты существующего. Разрешение изменило файл. | Первый проход, синтетический Responses-провайдер. Read-only/workspace-write не запустили bubblewrap в стенде; это не успешный тест изоляции. |
| Claude | `manual` | Native Write внутри/вне проекта: отказ сохранил файл, разрешение записало маркер. Read внутри — без вопроса; Read вне — отказ/разрешение ограничивают передачу содержимого синтетическому провайдеру. | Первый проход, настоящий CLI и локальный провайдер. Отдельная серия shell-команд Claude и телефон не проверялись. |
| Gemini | `default` | Native write_file внутри и shell внутри/вне: кнопки нашего окна разрешают/отклоняют операцию. Сверены конечные байты. | Новый связанный тест UI → bridge → native callback → bus → scheduler → инструмент. Внешние Read/Write отклоняются валидатором пути до карточки; внутренний Read проходит без вопроса. |
| OpenCode | `build`, wildcard `ask` | Настоящие Read, Write существующего файла и Shell внутри/вне: наши кнопки приводят к native once/reject; отказ не меняет файл/не выполняет команду. При Read отмена не передаёт маркер провайдеру. | Native API и синтетический локальный провайдер. Для внешнего файлового пути предварительный external_directory разрешён отдельно разово в фикстуре; его отказ проверен самостоятельным сценарием. |

Нельзя трактовать результат «нет карточки» как отказ пользователя. В матрице
фикстур отдельно отмечены автоматическое чтение Gemini, native-отказ пути и native
scope gate OpenCode. Внешний путь Gemini проверяет конкретный файловый инструмент,
а не весь процесс: разрешённый shell записал за пределами этой рабочей области.

Создание, сохранение, повторное открытие и второе сохранение профилей проверены
в JVM. У Codex дополнительно проверен настоящий редактор настроек и generated
`--ask-for-approval untrusted`. У старых записей отсутствующий флаг сохраняет
`never` / `on-request`. Подпись нового режима — «Подтверждать недоверенные команды» /
“Confirm untrusted commands”, с пояснением об отсутствии границы папки проекта.
Настройки Claude/Gemini/OpenCode не ужесточались предположительными режимами.
Отказ запуска sandbox не отключает подтверждения и не переключает доступ на full.
Полный перебор plan/read/project/auto_edit во всех CLI не выполнен; дополнительные
режимы не объявлены технически ограниченными только по их названиям в интерфейсе.

### Что именно проверяет связанный тест

`scripts/test-native-permission-chain.sh` запускает отдельный integration-набор,
который не включён в обычные JVM/CI-тесты без необходимых native CLI.

- Robolectric создаёт настоящую `GeminiPermissionUi`, общую с OpenCode, получает
  реальный запрос и нажимает `No` либо `Allow once`.
- Подменён только транспорт Android/proot: локальный HTTP соединяет Java-тест с
  изолированной native-фикстурой. Формат permission/answer и проверка контекста
  исходной сессии сохраняются.
- Gemini использует production `permissionReply`, `createBridge.command`, точный
  hook из `gemini-live-loader.mjs` и callback подтверждения закреплённого native UI.
  Далее работают настоящий message bus, policy engine, Scheduler и инструменты.
  Native chat содержит искусственную историю; генератор запрещает inference.
  Рабочая область фикстуры доверенная; native folder-trust на телефоне проверяется
  отдельно, приложение не стало автоматически доверять новым папкам.
- OpenCode использует production `OpenCodePermissions`, настоящий сервер и agent
  loop. Синтетический провайдер вызывает реальные инструменты по их native schema.
- После ответа проверяются native terminal state, исходные байты файла и наличие
  искусственного содержимого в следующем запросе локальному провайдеру.

Итог: **25 native-сценариев**, из них **18 с нажатием наших кнопок** (9 отказов,
9 разрешений), 3 JUnit-метода, без пропусков. Шесть остальных случаев относятся
к правилам Gemini без карточки; ещё один — к отказу native scope gate OpenCode.
Это не проверка нажатия на телефоне. Общий Android UI/proot transport остаётся
частью плана устройства.


## История этапа 2: проход 9 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Ранняя проба: неудачная проверка изоляции

Это ошибка настройки исследовательского стенда. Первые две попытки OpenCode
не имели сетевого запрета, а заданный local baseURL не попал в нормализованные
settings встроенной интеграции. Проба не считается изолированной или успешной.

Подтверждено сохранёнными журналами и метаданными собственной fixture DB:

- Native-сессии `ses_e5dd96ce1ffeY2iBQwMPIh3Fvr` и
  `ses_e5dd839b8ffeCF6ASr46wDraME` выбрали `openai/audit`, agent build и wildcard ask.
- Native error: `provider.auth`, **HTTP 403**,
  `Country, region, or territory not supported`.
- Журнал фиксирует неудачную websocket-попытку и HTTP fallback.
- В нормализованном provider readback — пакет `@opencode/ai/providers/openai`,
  transport websocket, без задуманного локального baseURL.
- В проверенном исполняемом SDK имеется default `https://api.openai.com/v1`.
  Это наиболее обоснованное назначение по настройкам и коду. **Точный фактический
  URL/path, IP, DNS-ответ и влияние прозрачного прокси не сохранены.**
- Локальный искусственный провайдер не получил эти запросы. В fixture history
  есть пользовательская запись длиной 44 символа и ответ с ошибкой; tool items
  и instruction entries отсутствуют. Это не доказывает отсутствие startup-чтения
  файлов или системных инструкций в памяти.

Этот HTTP 403 относится к стенду OpenCode и не устанавливает причину прежнего
отказа Docker Hub при установке Ubuntu AI.

Тело запроса, заголовки и полный список открытых файлов не захватывались.
Нельзя утверждать, что отправился только искусственный ключ или только текст
тестового сообщения. Возможны системный prompt, определения инструментов,
служебные поля и прочитанный CLI контекст. Доказательств их точного состава нет.

### Доступный раннему процессу контекст

| Область | Известное и предел доказательства |
| --- | --- |
| cwd | Собственный `opencode-fixture/project`, тогда без отдельной Git-границы, внутри настоящего checkout. |
| Файлы | HOME/XDG указывали на фикстуру. Это не chroot: процесс мог читать доступные тому же UID файлы через существующие bind-пути. Родительские AGENTS/конфигурация checkout могли участвовать в discovery. Полного file-open trace нет. |
| HOME/config | Сконструированы fake HOME, XDG config/data/cache/state. В собственной fixture credential table 0 записей; значения credentials не читались. |
| Временные данные | TMPDIR указывал на переиспользуемый `.tools/tmp`, без отдельной файловой границы. |
| Переменные | По выполненному коду запуска явно передавались PATH, fake HOME/TMPDIR/XDG, случайный пароль только local server и два флага отключения autoupdate/models fetch. На второй пробе добавлен искусственный OPENAI_API_KEY. Полный runtime env тогда не сохранялся. |
| Прокси | HTTP_PROXY/HTTPS_PROXY/ALL_PROXY не входили в явный whitelist запуска. Это не исключает dotenv, системную конфигурацию, DNS или прозрачное перенаправление. |
| Настройки | Первоначальный legacy `provider` ошибочно сочетался с полями нового `providers`-формата. Текущий файл позднее перезаписан правильной fixture-конфигурацией и не выдаётся за исходный снимок. |
| Права | Ask относится к model tools. Он не ограничивает самостоятельное чтение native CLI при старте и не создаёт ОС-изоляцию. |

Сейчас проверены только метаданные заданных путей и безопасные поля собственной DB.
Содержимое настоящих ключей, `.env`, авторизационных файлов и credential records
не читалось. Исторические неизвестные не заменены результатами повторного запуска.
См. `early-probe-metadata.json`, `early-probe-error-fields.json`,
`early-probe-history-shape.json`, `early-probe-visible-paths.json`.


## История этапа 2: проход 10 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Сетевой запрет последующих проверок

У `unshare` в этой среде exit code 0 не создал изоляцию: namespace остался тем же,
и дочерний процесс достиг собственного sentinel-сервера родителя. Эту проверку
не считают PASS. Возможности ядра проверены отдельными безопасными пробами.

Тестовый `local-network-only.c` использует seccomp user notification и ADDFD.
Он применяется к Node-фикстуре и всему дереву запускаемых CLI, включая exec/fork.
Системный firewall, маршруты, DNS и подключения приложения не меняются.

- Socket descriptors создаёт supervisor и передаёт фильтрованному процессу.
  Перед exec закрываются унаследованные дополнительные descriptors.
- connect/bind выполняются supervisor на сохранённом descriptor, по скопированному
  и проверенному адресу. Для этих вызовов нет CONTINUE и гонки повторного чтения
  sockaddr из памяти CLI.
- Разрешены только 127.0.0.1/::1 и endpoints, которые зарегистрировал bind этого
  же дерева. Чужой localhost-сервис не является разрешённым провайдером.
- Разрешённый Unix transport ограничен собственными зарегистрированными abstract
  endpoints; UDP, raw/packet, нестандартные stream-протоколы, TCP Fast Open,
  новые io_uring и BPF запрещены.
- Неподдерживаемый kernel API или невозможная проверка дают отказ, без обхода
  через прокси, preload или снятие фильтра.
- Контрольные пробы проверяют собственный listener, чужой localhost, TEST-NET IPv4,
  IPv6, UDP, packet, нестандартный stream, Fast Open, прямой native syscall
  и наследование после exec. Локальный
  sentinel вне фильтруемого дерева не получил соединения.

Новый guard также наблюдал и заблокировал обращения OpenCode к чужим localhost
портам **1234, 11434, 8000**. Это фактические попытки в новом запуске, не восстановленный
след ранней пробы. Они показывают, что правильный model baseURL не исключает иных
обращений CLI. Контент таких сервисов не запрашивался после kernel-отказа.

Guard находится только в тестовых скриптах и не входит в APK. Его kernel API
не добавляет требований к установке приложения.

Это ограничитель сети контролируемых тестовых процессов, а не полная изоляция
файловой системы или независимый аудит защиты от намеренно враждебного процесса
того же UID. Полный трафик CLI в обычной среде остаётся отдельной задачей.


## История этапа 2: проход 11 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Проверки и воспроизведение

| Проверка | Результат |
| --- | --- |
| Java/Robolectric: профиль, launch shell, Gemini, backup | 122 теста; 0 ошибок/отказов/пропусков. |
| Native UI chain под kernel guard | 3 метода / 25 сценариев, 18 нажатий наших кнопок; 0 пропусков. |
| Gemini broker | Принадлежность, lazy decrypt, чужие запросы, поздний результат, ошибка, watcher и retry; настоящий Node abstract transport и crash собственного процесса. Android sockets заменены shadows. |
| Backup Python | 44 теста и новый create/inspect/restore с шестью искусственными маркерами. |
| Локализации | 584 строки; RU/EN backup UI проверен отдельно. |
| Modern Lint | Все 5 модулей: 0 ошибок, 236 предупреждений. Это не проверка release APK. |
| Телефон / реальный Keystore / полный Android launch | Не проверены. |

Проверено заново: текущие JVM/Node/Python, native UI chain и kernel guard.
Исторические native-наблюдения Codex/Claude не выдаются за новый прогон: совпадение
адаптеров зафиксировано в `historical-source-equality.json`, но исходный hash Node
первого прохода не сохранялся. Свежий Lint/JVM совпадает с текущими production
входами (`production-inputs.json`). Сертификат переиспользован из первого этапа
при подтверждённом совпадении SHA-256 всего APK; закрытый ключ не читался.

Явный native runner: `bash scripts/test-native-permission-chain.sh`.
Нужны уже имеющиеся закреплённые CLI, SDK/JDK/Gradle и gcc. Скрипт не устанавливает
компоненты. При отсутствии prerequisite он завершает проверку ошибкой.
Native-тесты вынесены из обычного app/src/test; explicit runner запрещает Gradle
reuse/cache тестового task. Результат UP-TO-DATE не засчитывается как новый прогон.

Первоначальные ошибки стенда сохранены отдельно: неправильный env locator,
разбор stdout, неполная native initialization, ожидание продолжения OpenCode после
reject, предварительный external_directory и Gradle UP-TO-DATE. Они исправлены
и повторены. Ни один такой отказ не объявлен дефектом приложения или PASS.

Доказательства: `native-ui-matrix-guarded.json`, `jvm-final-summary.json`,
`native-ui-chain-network-hardened.log`, `network-guard/`, `backup-results.json`,
`evidence-sha256.json` в каталоге продолжения. Исходники сценариев сохранены в
`scripts/tests/fixtures/`, `scripts/tests/native/` и `.verification/`.


## История этапа 2: проход 12 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## План проверки телефона

Выполнять после отдельно запрошенной тестовой сборки, в новых фиктивных профилях
и папках. Не подключать рабочие аккаунты, не открывать реальные секреты, не удалять
окружения или старые файлы. Установка APK этим отчётом не разрешается.

1. Сохранить контрольные параметры старых профилей. Открыть и сохранить их;
   проверить отсутствие миграции среды и политики. Новый Codex — untrusted.
2. Для Gemini с искусственным API-ключом проверить начальный запуск и повтор.
   Ключ должен пройти broker либо дать отдельную понятную ошибку. Недействительный
   искусственный ключ не должен давать успешную авторизацию модели.
3. Проверить настоящий abstract socket, SO_PEERCRED и чтение PID/start identity
   между Android и Ubuntu AI. При невозможности установить принадлежность должен
   быть отказ; не переключаться на handoff-файл или чужую авторизацию.
4. В специально изолированном запуске прервать процесс до/во время/после handoff,
   затем повторить. Подтвердить отсутствие нового *.key. Второй активный fake-запуск
   должен сохранить собственный канал. Не применять широкие сигналы.
5. Повторить разрешение/отказ инструментов через карточки на телефоне с локальным
   провайдером и доказанным сетевым запретом. Для OpenCode отдельно пройти native
   external_directory; не скрывать этот предварительный запрос.
6. Проверить, что смена чата/закрытие окна не отправляет ответ в другой запуск.
   Сверить байты искусственных файлов после отказа и разрешения.
7. Проверить RU/EN полную и выборочную backup-форму, предупреждение и checkbox.
   Экспортировать только fake HOME через SAF; восстановить отдельно и сверить
   шесть маркеров. Проверить нехватку места/прерывание без перезаписи рабочей среды.
8. При обнаружении старого key-файла не читать его и не удалять автоматически.
   Фиксировать только метаданные и устанавливать принадлежность запуску отдельно.

Остаются непроверенными настоящий Keystore/его аппаратные свойства, lifecycle
телефона и native transport Android/proot, Codex sandbox на устройстве, полный
rootfs/SAF restore и полный сетевой профиль сторонних CLI в обычном режиме.
Модельные ответы, число тестов и наличие открытых исходников не гарантируют безопасность.


## История этапа 2: проход 13 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Тестовый APK для приёмки — 2026-11-04

Локально подготовлен signed release `2.1.3-test4` / `1480` с тремя исправлениями
этого этапа. [Проверки финального APK и план телефона](VALIDATION-2.1.3-test4.md).
Это не проверка Android lifecycle, успешной авторизации или интерфейса на устройстве.
Без push, публикации и установки. Опубликованный v2.1.2 не изменён.
Исправление обрывов при накоплении PNG в APK не входит; оно исследуется отдельно.


## История этапа 2: проход 14 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Уточнение проверки запуска и разрешений — 2026-11-04, test5

На телефоне новая сессия Codex в test4 не запустилась. Подтверждена причина:
тот же Codex 0.166.0 отвергает `approval_policy="untrusted"` в CLI/TOML до создания
app-server socket. Ранний стенд передавал `untrusted` только через `thread/start`;
этот поддерживаемый runtime API работал. Прежние native-отказы сохраняют доказательную
силу для стенда, но не подтверждают штатный bootstrap test4. Проверка generated
строки и успешное принятие RPC не выявили отказ CLI. Приёмка подтверждений на
телефоне осталась заблокированной и не считается пройденной.

Отдельное локальное исправление запуска использует поддерживаемый bootstrap,
передаёт исходную политику в thread API и проверяет фактические права до готовности.
Отказ/несовпадение не приводит к `never` или полному доступу. Старые профили сохранены.
CLI-сервер, профиль и действующая политика — разные этапы проверки.

[Матрица совместимости четырёх агентов](PERMISSIONS-COMPATIBILITY-TEST5.md)
фиксирует версии, 40 новых native-сценариев с сохранёнными профилями и реальными
инструментами, отличие UI-кнопок от native-отказов пути и ограничения стенда.
Claude shell теперь проверен отдельно, в дополнение к историческому Write.
Gemini API/Vertex здесь проверяют policy core, не настоящую облачную авторизацию.
Проверки телефона остаются в отдельном плане. Опубликованный v2.1.2, test4,
Components и рабочие среды не изменены. Обрывы при накоплении вложений не исправлены.


## История этапа 2: проход 15 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Пользовательская приёмка test5 и остаток аудита — 2026-11-04

Источник — сообщение разработчика HezeAI в этой беседе: на телефоне в
**2.1.3-test5 / 1481** новый профиль Codex запускается. Отказ и разовое разрешение
работают для shell-команд и прямого редактирования файлов. Эти пять проверок
отмечены как **пройденные пользователем на устройстве**, отдельно от наших
автоматических проверок. Ошибка запуска test4 сохраняется как исторический отказ;
T-CODEX-START-01 закрыт в проверенном пользователем пути test5.

Точные пути внутри/вне проекта, самостоятельная сверка байтов, повторное открытие
профиля и текущая версия CLI в сообщении не перечислены. Эти сведения не добавлены
как подтверждённые. Самостоятельную повторную проверку телефона помощник не проводил.

| Направление | Остаток после приёмки Codex |
| --- | --- |
| Права Codex | Запуск нового профиля, shell/direct-edit отказ и разовое разрешение пройдены пользователем. Альтернативные политики, граница проекта, старые профили, переключение чата и восстановление на телефоне не закрыты. A-ACCESS-02 сохраняется. |
| Claude, Gemini, OpenCode | Native-стенд сохраняет прежний статус. Штатный запуск и разрешение/отказ на телефоне ещё требуют отдельных проверок. |
| Gemini, A-AUTH-01 | Исправление broker проверено локально. API/Vertex-авторизация, Android peer credentials/Keystore, отмена/повтор и аварийный lifecycle на устройстве не подтверждены этим результатом. |
| Восстановление | Восстановление после завершения процесса приложения, повторный запуск и сохранение состояния требуют отдельной приёмки. |
| Резервные копии, A-BACKUP-01 | Фикстуры состава/restore и локальная проверка RU/EN предупреждений сохраняются. Предупреждения, экспорт и восстановление через Android/SAF на телефоне не закрыты. Копия остаётся незашифрованной. |
| Остальные ограничения | Полная сетевая карта сторонних CLI, драйверы, остальные непроверенные области аудита и задача накопления вложений сохраняют прежний статус. Эта приёмка не закрывает этап аудита целиком. |

Подробности: [приёмка test5](VALIDATION-2.1.3-test5.md#developer-phone-acceptance--2026-11-04)
и [матрица совместимости](PERMISSIONS-COMPATIBILITY-TEST5.md).
Изменена только документация. Исходники приложения, настройки профилей, APK,
опубликованные ресурсы и каналы обновления не изменены; push/публикация не выполнялись.


## История этапа 2: проход 16 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Дополнительные самостоятельные проверки после приёмки test5 — 2026-11-04

Приложение, рабочие среды и авторизация не изменялись. Новые проверки выполнены
на искусственных данных в `.verification/audit-test5-followup/`. APK не собирался;
push/публикация не выполнялись. Предыдущая пользовательская приёмка Codex сохраняется.

### Проверено заново

- **43 JVM/Robolectric метода**, без ошибок, отказов и пропусков: Gemini vault/broker
  и backup UI/engine/import. Проверены чужие UID/run/PID, PID reuse, недоступный vault,
  отмена во время decryption, поздний результат, watcher, независимость запусков и
  повтор; backup consent, RU/EN предупреждение, ошибки, очередь, отмена и импорт.
  Android sockets/Keystore здесь заменены фикстурами. Это не приёмка устройства.
- Node под kernel network guard: настоящий abstract Unix transport, искусственный
  ключ, несовпадение run, оборванный ответ, недоступный/аварийно завершённый собственный
  broker и новый успешный запуск. Legacy file-handoff проверен только на новых
  искусственных файлах; старые файлы пользователя не обследовались и не удалялись.
- Vertex: повторены private per-run settings, режим без fallback на Gemini key,
  сохранность исходного env и точные native validation/generator-config функции
  версии 0.64.0. Использованы искусственные значения, без Google login/IAM/inference.
- **44 Python backup-теста** и **30 selective-backup тестов** прошли. Реальный helper
  дополнительно создал, проверил и восстановил два новых синтетических архива.
  Исходные маркеры сохранены, восстановление выполнено в отдельных новых папках.
  Маркеры HOME и истории терминала восстановились. Android/SAF и живой rootfs этим
  не проверены. Два режима новой пробы выявили A-BACKUP-02 ниже.

### A-BACKUP-02 — P2, неполное восстановление OpenCode; не исправлено

**Подтверждено:** `AiAgents.directory/command`, bind в `SessionShell` и
`ai-agents-control.js.start` помещают connection-specific config/data в приватное
`files/.ai-control/connections/<id>/{config,data}`. Это отдельное место от Termux
HOME и домашней папки Ubuntu. API-ключ может находиться в конфигурации провайдера;
настоящие файлы авторизации не читались.

`WorkspaceBackupEngine.base` передаёт корень app files helper-у, но
`workspace-backup.py.create` включает из него только `workspace-sessions` при
выборе библиотеки. `selection_scopes/selection_walk` для Termux обходят HOME/PREFIX,
а не весь app files. `create/inspect/restore` сохраняют `aiConnection` в библиотеке,
но соответствующие приватные config/data не попадают в проверенные архивы.

Проверены режимы:

1. Библиотека + Termux HOME + PREFIX, без Linux-окружений.
2. Библиотека + все доступные группы выборочной копии Termux.

В каждом режиме фальшивый ключ в стандартном XDG config-файле OpenCode и отдельный
маркер private data отсутствуют и в архиве, и в восстановленной папке. Ссылка
профиля на connection ID остаётся в библиотеке. Название `data/probe-only.txt` —
искусственный файл для проверки охвата, не заявление о формате native базы истории.
Эта проба не выполняет Java-импорт профиля или запуск восстановленного OpenCode.
Точная ошибка при таком запуске и состав настоящей базы истории ещё не проверены.

**Влияние:** эти копии нельзя считать полной резервной копией подключений OpenCode.
После потери исходных app data может потребоваться заново настроить подключение
или авторизацию. Сохранность connection-specific истории не подтверждена.
Отсутствие секрета в конкретном архиве не доказывает безопасность остальных архивов:
HOME-маркеры секретов в эту же копию входят без шифрования (A-BACKUP-01 сохраняется).

**Затронутые проверенные версии:** опубликованный v2.1.2 и test5 / 1481. Для обеих
APK побайтово совпадают `workspace-backup.py` и `ai-agents-control.js` с исследуемыми
исходниками. Начало диапазона до v2.1.2 не установлено. Полный backup живой Ubuntu,
прочие режимы экспорта и фактическая авторизация OpenCode не объявлены проверенными.

**Воспроизведение:**

```
python3 scripts/tests/probe_backup_private_scope.py .verification/backup-private-scope-FRESH
```

Нужен новый каталог; проба сохраняет доказательства, не удаляет прежние. В этой
проверке команда выполнялась под kernel guard с запретом внешней сети. Результаты:
`private-scope-final/result.json`, `published-source-match.json`, `jvm-summary.json`
и логи в `.verification/audit-test5-followup/`. Первая проба тоже сохранена; итоговая
использует отдельную папку восстановления для каждого режима, чтобы не включить
предыдущее восстановление в следующий архив.

**Предлагаемое отдельное исправление:** явно выбирать и архивировать принадлежащие
подключению config/data с точными метаданными и предупреждением о секретах. Не
включать runtime-сокеты и кеши. При восстановлении создавать новый connection ID,
проверять связи профилей и атомарно публиковать данные в новом приватном каталоге.
Не заменять существующие подключения. Проверить реальный CLI restore, отмену,
коллизии и оба UI-режима экспорта. Изменение пока не внедрялось.

### Переиспользовано и осталось открытым

- Матрица 40 новых профилей и предыдущие 25 native-сценариев не запускались снова
  без изменения их значимых входов. Все записанные хеши `verified-inputs.json` и
  четыре закреплённых CLI сверены с текущими файлами. Это переиспользование ранее
  выполненных проверок, не новые телефонные проверки других агентов.
- Проверка уже работающего ADB-сервера `127.0.0.1:5037` получила connection refused
  (errno 111). Сервер не запускался, приложение не переключалось. Это результат
  конкретного пути доступа, не утверждение об отсутствии любого доступа к телефону.
- Claude/Gemini/OpenCode на телефоне; Gemini API/Vertex-авторизация, реальный Keystore
  и Android peer credentials; восстановление процесса и Android/SAF backup/restore
  остаются открытыми. A-BACKUP-02 добавлен к остатку, A-ACCESS-02 сохраняется.
- Принудительное завершение общего приложения затронет рабочие сессии. Эту проверку
  проводить только на отдельном устройстве/экземпляре без рабочих задач. Не заменять
  её сворачиванием окна и не считать пройденной по Node crash-фикстуре.

Короткий сценарий: [оставшиеся проверки телефона](VALIDATION-2.1.3-test5.md#remaining-phone-checks).
Драйверы, полный сетевой трафик CLI, MCP/плагины и накопление вложений этим этапом
не проверялись и не объявляются исправленными.


## История этапа 2: проход 18 — перенос 2026-11-04

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

## Обновление test6 — 2026-11-04

Дополнительная проверка и исправления только фикстур: `687674b2`.
Сохранение/reopen ciphertext и новый broker после owner death проверены искусственным
AES и shadows Android socket credentials. Отдельные Unix/process и API/Vertex
проверки повторены под прежним kernel guard после исправления путей.
Старые ошибочные запуски и ограничения сохранены в [отчёте test6](VALIDATION-2.1.3-test6.md).
A-BACKUP-02 остаётся исправленным локально в `e676dd42`; телефон не закрыт.
Полный остаток, исключения и ответственные собраны в едином плане в начале этого
документа. Успех автоматических тестов не меняет статус Android lifecycle,
Keystore, действующего аккаунта или исключённых направлений.


## Урок: 2026-11-03 — Codex untrusted: команды и прямое изменение файла — разные запросы

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Codex untrusted: команды и прямое изменение файла — разные запросы

- Контекст: второй этап аудита, локальный Codex CLI 0.166.0 app-server с loopback-провайдером и искусственными файлами. Аккаунты не использовались.
- Подтверждение: при dangerFullAccess + untrusted exec_command вызвал commandExecution/requestApproval, а apply_patch — fileChange/requestApproval. Отказ сохранил существующий внешний файл и не создал новый; разрешение изменило/создало файлы. Одного теста shell-команды для проверки прямого редактирования недостаточно. Это политика разрешений, не изоляция проекта.
- Совместимость: безопасный default задавать фабрикой новых редакторских профилей, а не полями конструктора, используемого декодером старой библиотеки. Отсутствующий новый флаг должен сохранить старую эффективную политику. Проверены create/save/reopen/second save и copy; локальный коммит 752b1b1d. Телефон и sandbox read-only/workspace-write пока не подтверждены: стенд отказал при запуске bubblewrap.
- Доказательства: docs/fork/SECURITY-AUDIT-STAGE2.md и .verification/security-stage2 активного checkout. Native tool-loop Gemini/OpenCode этим не проверен.


## Урок: 2026-11-04 — Codex 0.166.0: CLI config и typed thread policy проверяются раздельно

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-04 — Codex 0.166.0: CLI config и typed thread policy проверяются раздельно

- Контекст: новый профиль в test4 не запускал app-server. Та же версия с чистым stdin завершалась штатно; прежний стенд проверял только thread/start.
- Подтверждено: бинарник `f2e6…b462a` отвергает CLI/TOML `approval_policy="untrusted"` до сокета, но принимает typed `approvalPolicy: untrusted` в thread/start и фактически блокирует exec_command/apply_patch до ответа. Native No/Allow проверен с искусственными файлами внутри/вне проекта.
- Применение: исполнять generated bootstrap, затем проверять effective cwd/sandbox/approval из RPC. Не считать строку команды, принятый конфиг или запрос без CLI override сквозной проверкой нового профиля. Поддерживаемый bootstrap не заменяет сохранённую thread policy; mismatch/rejection не разрешают fallback.
- Стенд: Unix listen создаёт защищённый physical socket `/tmp/codex-daemon-<uid>/<sha256(rendezvous)>` и ссылку по запрошенному пути даже с --no-daemon. Kernel guard разрешает только точный уникальный fixture socket и соединения с зарегистрированными собственными listeners; не разрешать весь каталог демона.
- Граница: Robolectric/native fixtures не подтверждают Android/proot/авторизацию и нажатия телефона. См. `docs/fork/PERMISSIONS-COMPATIBILITY-TEST5.md` активного checkout. Рабочие профили и секреты не использованы.


## Урок: 2026-11-04 — OpenCode: существование каталога не определяет владение тестом

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-04 — OpenCode: существование каталога не определяет владение тестом

- Контекст: телефон остановился на защитном preflight тестового подключения до запуска CLI. Профиль выбрал AUDIT OpenCode; создатель конкретного каталога на телефоне не установлен.
- Подтверждено по коду: обычный запрос каталога моделей через ai-agents-control start создаёт connection root до inference. Нельзя считать сам факт существования доказательством рабочего подключения или основанием для удаления.
- Для isolated fixture: хранить bridge-state и XDG отдельно; перенаправлять только каталог, проверяя неизменность прочих аргументов и effective policy. Не читать/перезаписывать исходное подключение. Защита основного подключения и component guard сохраняются.
- Проверено помощником: реальный native No/Allow once с заранее существующим искусственным source root, исходные байты/mtime, повторное открытие без replay и owned stop/exit. Телефон после исправления ещё не принят. См. PERMISSIONS-COMPATIBILITY-TEST5.md, раздел private bridge state, активный checkout. Это урок тестового стенда, не изменение приложения или гарантия OS-изоляции.


## Урок: 2026-11-04 — Границы отдельных инструментов не равны песочнице процесса

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-04 — Границы отдельных инструментов не равны песочнице процесса

- Контекст: изолированные native CLI Codex 0.166.0, Claude 2.1.296, Gemini 0.64.0, OpenCode 2.0.28; локальный провайдер под kernel network guard, не телефон.
- Подтверждено: Gemini Read/Write возвращают Path not in workspace для файлов вне проекта, но разрешённый shell читает эти файлы и передаёт искусственный маркер провайдеру. Не описывать такую проверку пути как OS-песочницу.
- OpenCode отдельно запрашивает external directory и разрешение Read/Edit. Native Esc открывает объяснение отказа; до отправки формы отказ ещё не завершён. Проверять обе ступени, не заменять их одним событием.
- Для чтения проверять уникальный искусственный маркер в фактическом запросе: неизменность файла недостаточна. Чат, управляемый TUI и ручной CLI имеют отдельный охват.
- Подтверждение: активный checkout, SECURITY-AUDIT-STAGE2.md / «Границы доступа: общая таблица», .verification/phone-boundaries/summary.json. Актуальные статусы телефона ведутся отдельно в отчёте; стендовые результаты автоматически на телефон не переносить.
- Дополнение исследования Claude 2.1.296: blockReadsOutsideWorkingDirectories технически блокирует файловый Read, но разрешённый Bash всё ещё читает вне проекта. Строгий native sandbox с failIfUnavailable остановился до модели из-за socat not installed; это отсутствие зависимости, не доказательство несовместимости Ubuntu AI. Подготовка зависимости и реальные Bash-границы — отдельная будущая задача. Детали и неудачные пробы сохранены в том же отчёте, раздел исследования Claude.
- Дополнение Gemini 0.64.0: auto/default делает вспомогательный model-routing generateContent без tools до основного tool-enabled запроса. Провайдер фикстуры не должен расходовать тестовую операцию на таком запросе. Исходный телефонный проход без tool calls был недействительным; explicit-model повтор принят отдельно, фактический label gemini-2.5-flash-lite. Проверять наличие native tool call/result, а не только BOUNDARY FINISHED. См. Gemini phone acceptance и .verification/gemini-boundary-routing в текущем плане аудита.
