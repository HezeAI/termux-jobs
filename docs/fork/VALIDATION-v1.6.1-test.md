> Historical private checkpoint. Current stable evidence: [v1.6.5](VALIDATION-v1.6.5.md).

# Private v1.6.1-test — Codex shell chronology

Not published to GitHub. com.termux/code1193, Android8+/ARM64, same certificate,
no storage/schema migration. User requested only Codex action-card grouping.

Consecutive native commandExecution rows in the same turn form one expandable
Android card. User/assistant text, reasoning, receipts and errors retain their
positions and break groups. A compound shell command remains one native step.
The collapsed card shows the count/current command; each expanded step opens its
original native detail (including paginated output). Running/completed/failed/
declined/interrupted labels use native status and exit code. Time is shown only
when native durationMs exists, never inferred from text. RU/EN labels and numeric
plurals are localized; commands/output remain original. Old running-helper rows
can group through their existing Shell title; missing duration/exit metadata is
not fabricated. Relaunch the same chat for the new native metadata.

CodexActionTimeline is presentation-only; AgentChatUi enables it only for Codex.
Other agent protocols/runtimes/prompts/permissions, terminal, model/limits card,
history and native tool execution are unchanged. No extra inference/RPC/timer is
introduced. Stable card/step identities keep expansion and neighboring rows on
updates. Existing scroll anchor/follow behavior handles grouped cards.

## Evidence

- 135 focused JVM tests, zero failures/errors/skips: same-turn/consecutive
  grouping, barriers, compound command, old helper rows, other-agent rendering,
  current step, stable expansion/step identities, original detail identity/output,
  failure/denial/exit/duration, RU/EN and history reader position; existing shared
  chat/keys/lifecycle/provider/navigation/status tests remain passing.
- Codex adapter fixtures pass including typed native shell metadata. Exact native
  Codex0.163.0 app-server fixture with loopback-only synthetic provider passes;
  no real account/model API was used.
- Release assembly and runtime definitions pass:13113 classes,31 previously
  reviewed optional unresolved types. Existing Kotlin/UAST/JNA warnings remain;
  clean full Lint is not claimed.
- ZIP/package/version/minSdk/ABI/non-debug/signature/SHA/privacy/source staging
  checks pass. All assets except codex-chat.cjs and all native libraries are
  byte-identical to stable1.6.0. New Android classes verified in DEX.
- No running user session, installed helper/environment/shared SDK or account
  data was modified. Publication was deferred at this checkpoint.

## Final artifact

`Termux-Jobs-v1.6.1-test-arm64.apk`, 36665742 bytes.
SHA-256: `70a106060dda294f8d7cc1dd0906cd44154f39eb07fe804c3c645b9c8f5f8d89`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (unchanged).
APK/checksum copied to Download; destination hash verified.

Phone checks needed: relaunch same Codex history, ask a task with several shell
calls, expand/collapse during output, open a step, scroll up while working,
switch to another agent/session and verify its original presentation and input.
JVM UI checks do not establish Samsung keyboard/gesture/DPI behavior.
