# Agent-chat stability audit — 2026-10-23

Status: private `2.0.3-test`, versionCode1332. Public stable remains v2.0.2/code1331.
This is an agent-chat audit, not an exhaustive application/environment/X11 audit.
No new GitHub publication is part of this work.

## Confirmed problems and fixes

1. **Permission buttons could remain disabled after a transport failure.**
   The unchanged native prompt did not rebuild its buttons on the next poll.
   Failure now restores only the buttons still mounted in the current UI
   generation. No approval is automatic. A four-provider regression test first
   failed against the old behavior and now checks a failed reply followed by
   an explicit successful retry. A separate form test checks that OpenCode uses
   the resolved native conversation and retains the answer after failure.
2. **OpenCode admitted stale plain-text requests.** Its conversation guard had
   applied only to attachments, after the dedup lookup. Send, detail, form and
   interrupt now check the native session first. Regression tests reject stale
   and unscoped sends before POST/admission, refuse old interrupts without
   deleting a shell, and preserve same-ID dedup and a valid owned interrupt.
3. **Gemini could retain an ID for an action never dispatched.** If native UI
   readiness/Shell idle changed between reservation and the native callback,
   same-ID retry remained blocked. Only a failed guard *before* the callback
   now removes that reservation. Ambiguous native callback failures remain
   reserved and are not blindly re-executed. Tests cover both outcomes.
4. **Delayed close could address a later run.** OpenCode reused the profile-ID
   control socket, and queued work read a mutable profile. New launches now
   allocate independent private socket UUIDs; chat/status/close operations
   capture their run before scheduling. An OS-socket test starts two production
   controllers with the same connection/chat but different sockets: stopping
   the old one and repeating its delayed stop leave the next one available.
   The Shell interrupt confirmation also captures its UI generation/run: an old
   dialog cannot interrupt a new run of the same native chat. This regression
   first reproduced an incorrect interrupt, before the owner guard was applied.
   Codex also captures its stop-marker socket before enqueueing; a regression
   mutates the profile before executing the queued action and verifies only the
   original marker. Gemini/Claude already capture run IDs and now also capture
   the environment rather than reading it later from a mutable profile.

An additional documentation discrepancy was corrected: minimizing the Codex
login terminal keeps its pending device-code login. Explicit Return to the Codex
chat cancels the owned login attempt; success still returns automatically. The
login fixture now exercises the actual terminal command handlers.

## Scenario coverage and limits

| Flow | Evidence | Remaining device/provider check |
| --- | --- | --- |
| Create/preset launch and ownership | JVM profile/store/shell/preparation tests; independent OpenCode run addresses; queued Codex marker test | Fresh app + real CLI setup per provider; no installed rootfs was reset |
| First/existing authentication | Codex device-code handler fixture; Gemini key encryption/handoff and Vertex fixtures; pinned Gemini hook transforms; private native fixture homes | Real browser/device-code login, token refresh, real Gemini key/Vertex, real Claude login |
| Text and streaming | Real Codex0.163.0 app-server and Claude2.1.296 CLI against synthetic loopback providers; all four adapter tests | Live paid/free provider behavior and each model family |
| Files with/without text | Shared projection tests, four-provider JVM composer/draft tests, Codex native local-image fixture, Claude native multipart, OpenCode2.0.28 native local-file URI/inbox | Real camera/gallery/video/code/binary tasks on each agent; Gemini native file-access approval |
| Permission allow/deny/forms | Four-provider failed-reply UI regression; native Codex permission fixtures; real Claude tool allow-once/deny fixture; Gemini callbacks; OpenCode form nonce/session tests | Device placement/scrolling and provider-specific interactive requests |
| Terminal handoff and return | Android UI mocks, controller Unix/IPC fixtures, Claude owned native/SDK process lifecycle, actual Codex login handlers | Android keyboard, arrows, gestures, IME/focus and native TUI behavior |
| Tabs, drafts, history | JVM same-view/draft/session/cache/pagination tests; Codex native history cursors; Claude fixture history; OpenCode real durable inbox | Long real histories and switching while a real provider streams |
| Errors and recovery | Stale session/run rejection, readiness-before-dispatch retry, retained uncertain admissions, failed approval retry, owned stop tests | Actual device network loss/recovery and third-party outage behavior |

“Native” above means the installed CLI executable ran with an isolated fixture
HOME/config and synthetic local data/provider. It does not mean a live account
or remote model was tested. OpenCode's native attachment fixture uses
`resume=false`: it proves media admission/persistence, not model inference.
Gemini Unix/IPC tests use production controllers with synthetic native callbacks;
SDK hook tests verify pinned source transforms without a real model call.

## Automated results

- Baseline:897 app JVM tests passed;154 unchanged terminal-emulator results were
  reused by Gradle. Baseline Node selection:28 suites passed.
- New regressions reproduced the approval, OpenCode session, Gemini retry and
  OpenCode reused-run bugs before their respective fixes.
- Final broad JVM run:903 app tests and154 terminal-emulator tests passed
  with bounded worker lifetime/768MiB heaps. After the last UI-only owner guard,
  the complete35-test AgentChatUiTest suite passed separately (904 unique app
  tests covered across these runs; no claim of a single904-test final run).
- Final Node result:30 selected suites passed, plus the new production-controller
  run-isolation suite passed separately (31 suites in total).
- One intermediate full JVM run finished with900 passes and2 resource failures
  (`OutOfMemoryError` in Robolectric APK asset mapping). Both failed X11 tests
  passed in isolation. The final run limits worker lifetime and uses768MiB heaps;
  this is a verification-only setting, not an application change. The extra
  interrupt test also needed an explicit virtual poll before asserting the
  replacement chat was ready; the corrected fixture retains its old-run refusal
  and valid current-run interrupt assertions.
- Release-configuration ARM64 build passed. Artifact:
  `build/apk/Termux-Jobs-v2.0.3-test-arm64.apk`, 45,829,426 bytes.
  SHA-256: `be9020bf1cfcda97efa327c047567417010f4c72a8d551769a6e567c8647d1fe`.
- ZIP integrity, com.termux/version2.0.3-test/code1332, ARM64-only ABI,
  non-debuggable flag and APK signature verified. Signer matches public v2.0.2:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`. All packaged native libraries match public v2.0.2
  byte-for-byte; relevant packaged adapter assets match the audited sources.
- Artifact filename/content screening found no private key/auth files, signing
  keystores, development-tool directories or matches for the checked secret
  patterns. This is a bounded screening, not a proof of absence of all secrets.
- Runtime inventory:13,603 defined classes; zero missing required definitions
  and zero unreviewed missing types. The31 optional/metadata/JVM-only references
  have existing explicit review notes. This does not prove method/reflection
  compatibility on every Android version.
- Build emitted existing lint custom-check/Kotlin metadata compatibility errors
  while Gradle completed successfully. Do not describe lint as fully clean; these
  tooling limitations are separate from the passing tests and package checks.
- 243 relative repository documentation links resolved (absolute syntax examples
  excluded). `git diff --check` passed.

Reproduction/evidence is in ignored
`.verification/agent-chat-audit-20261023/`: baseline/final Gradle logs, XML,
`node-results.json`, per-suite output and before/after regression logs.
Changed checks are in `AgentChatUiTest`, `AiAgentsTest`, `CodexRpcTest`,
`test_opencode_chat_recovery.cjs`, `test_gemini_chat_recovery.cjs`,
`test_opencode_run_isolation.cjs` and `test_codex_chat_login.cjs`.

## Review and logical inspection

- Traced create → prepare run → generate launch script → bind handle/store →
  chat/status reads and actions → tab changes → explicit close/restart. OpenCode
  run identity is allocated before script construction and persisted afterward.
- Readiness loss before native Gemini dispatch allows retry; loss after dispatch
  does not erase dedup. Permission errors do not grant access or silently resend.
- Old OpenCode actions cannot select a new run through a changed profile or
  receive its acknowledgement through the old native conversation token.
- Existing process ownership remains authoritative. No process-name/global kill
  was added. Fixtures only start/stop their own children; the Claude lifecycle
  fixture checks that an unrelated fixture process survives.
- No new public listener, TLS bypass, permission escalation, account copy, shared
  auth file, key logging or package installation was introduced. The optional
  run field contains a UUID only. Gemini key/CLI auth storage is unchanged.
- X11, renderers, environment installers and native libraries have no source
  changes in this audit. Their existing tests are regression evidence, not a
  new full clean-device desktop verification.

## Data/update compatibility and rollback

Package `com.termux`, signer, Android8+/ARM64 and workspace schema8 stay unchanged.
The optional validated `aiSocketId` field defaults empty for existing records,
which retain their old socket until their next explicit launch. Duplicates and
backup imports do not inherit a live run. Provider connection IDs, saved native
history and authentication stores are not rewritten. No reset/migration occurs.
Older versions ignore the new field; restart OpenCode when downgrading rather
than trying to attach to a newer running socket. Full device update/downgrade
behavior remains unverified. An Android downgrade may need a future APK with a
higher versionCode; ordinary installation of public1331 over private1332 will
not be offered as an upgrade.

The corrected packaged helpers apply on the next explicit agent launch. Save the
native chat before restarting an agent after updating. Current sessions were not
stopped/restarted by this audit. To abandon the changes before installing, keep
using public v2.0.2; its tag, APK and updater manifest remain unchanged.

## Smallest phone check

1. Update with the private APK, save the current chat and explicitly restart an
   agent. Verify the correct saved conversation reopens.
2. Send text and one file with/without a caption. Change tabs during the reply;
   verify the draft, focus and history stay with their session.
3. Test allow once and deny on a harmless fixture action. If connection fails,
   verify the pending prompt can be answered again after checking native state.
4. Open/minimize/return from the robot terminal, check keyboard/arrows. Close and
   reopen OpenCode quickly and confirm only the intended run stops.

Real first-login testing is needed only on a genuinely unauthenticated profile;
do not clear the working account to manufacture it. This report claims no new
phone GUI/provider verification by HezeAI yet.

AI development assistance: OpenAI Codex
