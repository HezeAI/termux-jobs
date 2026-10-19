# v1.5.28-test — common agent cards and native controls

Private update to v1.5.23-test/code1185; package com.termux, code1186,
Android8+/API26, ARM64 only. The name skips historical private1.5.24–27
artifacts (codes1161–1164) to preserve those reports/APKs. No data reset or
library schema migration (schema8); optional reasoning fields default to CLI
settings in existing profiles. Same package/signing identity, higher code.

Shared status presentation for Codex/Claude/Gemini/OpenCode: 12sp text, minimum
34dp, 12/4dp padding,6dp vertical external spacing, at most two lines. Gemini and
Claude retain these previous dimensions; OpenCode adopts the same presentation.
Width remains content-dependent. The dialog separates model, supported native
rights/work modes and reasoning. Codex retains its previous controls; no new
Codex permission changing. The transcript/cache/IME/resize paths are unchanged.

OpenCode native settings are read from the exact own session. Full/ask/read and
Build/Plan use separate confirmed choices. Apply requires idle chat, no native
terminal/direct Shell/inbox/questions/permissions. Native readback + revision
check precedes preset updates. Requests are bounded and deduplicated, never blindly
retried after ambiguous failure. Replacing current native permission rules is
explicit in confirmation; the launch project scope is retained. Prompt admission,
settings and terminal opening share a per-controller serialization queue.

Claude effort comes from the native supported-model catalog. Session-scoped
apply_flag_settings is used; no shared settings/auth/SDK files written. Native
ACK precedes saving. Idle/busy/task/approval/Shell/native-terminal guards prevent
in-flight edits. CLI default can be restored. A persisted choice is applied on
future starts, and retained for SDK/native handoff. Existing status-model/mode
controls remain native. No quota is invented from last-request token usage.

Gemini thinkingConfig changes only main-chat model resolution in its owned CLI.
Supported Gemini3/3.1/new documented model families use level; Gemini2.5 uses
1024/8192/24576-token budgets (Pro high32768). Unsupported/unknown models are
unavailable, not given a speculative catalog. CLI default restores original
resolution. Existing tools, temperature, includeThoughts, service requests and
subagents retain their settings. Native user hooks may subsequently change a
request under their own rules. Busy/stale-model/unsupported guards apply. Choices
persist in session/preset after ACK. Permissions do not change with reasoning.

## Evidence

126 focused JVM tests passed; no failures/errors/skips. New tests cover common
chip geometry, native effort capabilities/busy UI, catalog sanitation, profile
copy/launch persistence, OpenCode confirm-before-apply, native ACK saving,
ambiguous ACK refusal and foreign settings rejection. Claude profile encode/
decode retains effort. Existing Gemini mode ACK/profile persistence and live
usage dialog checks pass. A custom-view test now addresses the actual tagged
info view rather than AlertDialog's hidden message template. A new geometry test
uses TextView.getMinHeight (not the unrelated View minimum-height property).

14 Node suites passed (node-delivery.log). Gemini exact pinned0.64.0
ModelConfigService code was exercised with resolved request config: low/high,
CLI default restoration,2.5 budgets, unchanged other fields/subagents, busy and
stale guards. Other suites exercise pinned native UI hook fragments and private
Unix/IPC chat routing, synthetic callbacks and transcript/permissions/receipts.
No claim of live Google-provider acceptance or physical device testing follows
from these fixtures.

Real pinned Claude2.1.296 ran against a loopback-only synthetic Anthropic
provider. Native initialize/model/rights, text/tools, deny/once, Shell, history
resume and owned shutdown passed; unrelated fixture process survived. Native
requests carried low then high output_config.effort. Separate isolated probe
also confirmed null/auto resetting to CLI default. No real account/model usage.
Lifecycle tests retain native processes on Back and verify owned shutdown.

Real pinned OpenCode2.0.28 ran in a private HOME/XDG project, no inference:
ask→full→Plan→read→Build native changes/readbacks passed. Repeating an admission
ID did not replay; history ID/rows and project scope remained the same. Unit
fixtures additionally test changed/foreign/stale IDs, busy refusal, failed
readback and serialization recovery. These are not an atomic lock against a
separate direct client bypassing the private controller.

Evidence is in .verification/agent-cards-v1.5.24 (the initial work directory name;
final delivered APK is1.5.28). Tests and build use matching original Java/assets;
isolated source/ARM64 staging comparisons passed. No user session, installed
Termux/Ubuntu/shared SDK or server was modified. Isolated test-owned processes
and files only. No installation or GitHub publication performed.

## Artifact

APK: `Termux-Jobs-v1.5.28-test-arm64.apk`, 36626000 bytes.
SHA-256: `67bee5f46fd9618728da9d41d2641cbf4d47cfc9fdf0fd4bb7d173bb0d431015`.
Certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Release assembly/runtime verification passed. APK ZIP, package/version/code,
ABI/API, v2 signature and absence of private-data files/secret markers checked.
Four changed assets match sources; two new assets are gemini-thinking.cjs and
opencode-session-settings.cjs. Every other asset, including Codex controller,
bootstraps/server/backup, and all packaged native libraries match1.5.23 exactly.
Download copy SHA-256 verified. Existing31 reviewed optional runtime types remain;
known Lint/Kotlin-metadata/UAST/JNA tooling warnings remain. No clean-Lint claim.

## Smallest device checks

Update over the current APK without clearing data. Relaunch a Gemini/Claude/
OpenCode session with its saved chat to load the new controller. Check the shared
card geometry, model selection, native rights and per-model reasoning choices,
confirmation/back, busy guards and saved preset after a new launch. Real provider
availability, billing, live Google request acceptance and physical UI/IME/DPI
remain developer checks. After those, chat redesign for Codex is the next task.
