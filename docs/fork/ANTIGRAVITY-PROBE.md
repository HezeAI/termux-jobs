# Antigravity CLI — isolated investigation, 2026-10-14

No integration or APK changes. Official Linux ARM64 manifest resolved version1.3.4;
archive SHA512 verified; executable204492984 bytes. Downloads in .tools/antigravity.
Official installer inspected, NOT executed: it stages under HOME/.cache and runs
`agy install`, so even a custom target is insufficient isolation by itself.

A nested PRoot probe mapped private repository fixtures onto /root and /tmp,
confirmed that /root/.codex was absent, and stripped credential environment.
`--version` produced no output and timed out (30s). A second probe with Go async
preemption disabled and GOMAXPROCS2 PASSED: version1.3.4 and native --help, exit0.
The first own timed-out probe child was killed; no other processes were stopped. No current terminal/user services stopped.
Bubblewrap alternative failed `fchdir to oldroot`; it provided no positive isolation.

The initial timeout was specific to the nested test setup; Go options allowed
version/help to run successfully. Direct startup against actual
HOME was not attempted because it could migrate Gemini config/credentials or write
state outside the allowed workspace. OAuth/model calls/history/permission enforcement are
NOT runtime verified. Native --help confirms --conversation, --continue, --model,
--effort, --mode accept-edits/plan, --sandbox and --dangerously-skip-permissions. No real credentials or inference requests used.

Next smallest useful check: launch this verified executable in a disposable,
single-layer proot guest with private home, runtime/cache/tmp and no credentials;
check --version/--help, then PTY OAuth prompt, then owned shutdown. Integrate only
after independent state paths and native permission/history flags are established.
Do not treat documentation support for Linux as confirmed Android support.

Evidence: .verification/antigravity/result.json, probe.py, probe-go.py;
.verification/antigravity-install-inspect.sh and .tools/antigravity/manifest.json.

Official sources:
- https://antigravity.google/cli/install.sh
- https://antigravity.google/docs/cli/install
- https://antigravity.google/docs/cli/reference
- https://github.com/google-antigravity/antigravity-cli

Docs describe personal-account OAuth, manual URL/code over SSH, /resume, /model,
/permissions and /usage. These remain documentation findings, not device test results.

## Final PTY result

With the same Go options, an isolated PTY launch created only fixture home state,
SQLite conversation_summaries DB and native config folders. Its internal log records
server initialized successfully in5.19s and expected not-logged-in status. Remote
Control was off. However no bytes reached the terminal within25s; Ctrl+C and TERM
did not finish within the grace window, so the own probe was killed. Process check
found no remaining Antigravity probe processes. No OAuth/account/model request made.

Therefore version/help and backend initialization are proven; a usable terminal
login/TUI is not. Integration is not ready. Remaining minimum test is a disposable
single-layer proot PTY run to distinguish nested tracer trouble from native TUI
startup trouble. No APK/source changes were made by this investigation.
