> Historical private checkpoint. Current stable evidence: [v1.6.5](VALIDATION-v1.6.5.md).

# Private v1.6.5-test — OpenCode component inventory

com.termux/code1197, Android8+/ARM64, same package/signer/schema. No data reset.
Private APK, not a GitHub release. Retains1.6.4 agent cards/composer-focus fix and
Codex large-history fix. Developer confirmed1.6.4 focus and other-agent cards.

## Report, evidence and change

Developer reports recurring missing opencode-cli during new session creation,
preset launch and manual environment checks; restarting can launch OpenCode.
Found a concrete inventory/launch discrepancy: jobs_ai_prepare prepends private
agents/bin, private Node/bin and the legacy agents/node_modules/.bin. Inventory
looked for a direct binary or ambient PATH, omitting that private search path.
An isolated compatible old-npm-layout fixture reproduced the false MISSING before
the change. Inventory now prepends exactly the launch paths inside its subshell,
for OpenCode only. No bootstrap is sourced, package installed or user environment
persistently changed by inventory. Missing/incorrect pinned CLI remains MISSING;
no readiness is fabricated and no unknown CLI version is accepted.

The specific installed guest runtime was unavailable at standard local paths in
this session; the developer's particular cause still needs phone confirmation.
Other startup failures/version incompatibility are not claimed fixed. Two isolated
--version calls to an existing copied pinned binary both returned opencode v2.0.28.

## Verification

Expanded isolated shell fixtures pass: npm-layout CLI, script-based CLI/private
Node, direct binary, wrong pinned version, truly missing CLI, subshell PATH
nonleakage, plus existing read-only inventory, key privacy, consent guards,
missing-only APT, APT failure, corrupt archive fail-before-extraction, existing
Cloud credentials preservation and host/rootfs consent checks. No fixture runs
real APT/pkg/auth/network installation.

32 focused JVM tests pass, zero failures/errors/skips: AgentPreparation, shared
agent cards and AgentChatUi. This includes the composer-focus regression fix.
Shell syntax check and diff whitespace check pass. No Java UI change here.

Native terminal stale startup text/mouse escape echo remains deferred as agreed.
Existing user sessions, installed runtimes, system packages, shared SDK and remote
servers were not changed. New resources and build outputs stay in the repository.

## APK

Final audit/delivery evidence: `.verification/opencode-startup-check/`.
After installing the APK, run OpenCode environment check, launch from a preset
and create a new session. Verify the false missing notice no longer occurs.

Final release assembly/runtime audit passed:13115 defined classes,31 reviewed
optional unresolved types. Existing Kotlin/UAST/JNA diagnostics remain; no clean
full Lint claim. ZIP/package/version/minSdk/ABI/non-debug/signature/SHA and private
file/secret-marker checks passed. Only assets/agent-dependencies.sh differs from
1.6.4; native libraries and all unrelated assets are byte-identical.

`Termux-Jobs-v1.6.5-test-arm64.apk`, 36669538 bytes.
SHA-256: `a2caef32fb7c8ca30f3cb06c6b569ff663a0a989fb47112e8e7d62ee06515045`.
Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK/checksum copied to Download; destination SHA verified.

Subsequent developer device check confirmed the OpenCode component warning was
fixed. This report otherwise preserves evidence from the private checkpoint.
