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
