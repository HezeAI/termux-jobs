> Current release: 2.1.6/1532, based on accepted test10/1531. Downloads the pinned jobs.1 component from
> [Components](https://github.com/HezeAI/termux-jobs-components/releases/tag/rtk-1.1.0-jobs.1).
> No embedded RTK archive. Separate revision, SHA-256 and runtime checks distinguish
> this build from upstream1.1.0. Old versions and per-chat data remain.
> RTK Lite retains captured-stdout accounting; native measurement is enabled only
> in native mode. [Checks](VALIDATION-2.1.6-test10.md).
> Historical implementation notes follow.

# RTK Lite — partial integration

UI names in 2.1.6-test3: **RTK Lite** for the app’s limited, verified integration;
**RTK native** (Russian: **RTK штатный**) for the native integration with broader
coverage. Internal `basic`/`advanced` values remain unchanged. The sections below
describe Lite; native preparation and evidence are in
[the research report](RTK-NATIVE-RESEARCH.md).

RTK OSS 1.1.0 is optional. Install it in the selected agent's Ubuntu AI, under
`~/.local/share/termux-jobs/components/rtk/1.1.0-jobs.1`. One copy serves that environment.
Chat state, byte measurements and retained originals are under
`~/.local/share/termux-jobs/rtk/chats/<profile-id>`. Disabling keeps the component.

The APK pins the official archive URL, exact length, archive/binary/license hashes.
Installation rejects symlinks, unexpected archive members and insecure redirects;
it checks ELF ARM64 and real `--version`. No global PATH or profile modifications.
Component versions must be reviewed and pinned with the project's component
update/release procedure; there is no automatic “latest” download or `rtk self-update`.

Native hooks rewrite only supported literal Git commands to a plain pipeline:
`set -o pipefail; <original Git command> | python3 -I <helper> filter <chat> <filter>`.
RTK filters stdout before it reaches the provider. Stderr bypasses RTK unchanged.
No `2>&1` is allowed: in Codex 0.166.0 this redirection prevented its native shell
parser from recognizing Git prefixes. The former candidate bypassed a forbidden
`git diff` rule; the final candidate without redirection preserved the tested rules.
No saved execution rules, approval policy or sandbox policy are changed.

Claude uses scoped SDK/native hooks without `permissionDecision=allow`.
Codex requires that hook decision for updatedInput, then performs native command
approval. Only the app's own session hooks receive matching trust hashes;
no global hook-trust bypass or config file writes are used. Existing configuration
layers retain their own hooks. Connection status requires confirmed hook discovery.

Unsupported commands continue normally. Filtering never truncates originals;
large/binary inputs and failures pass through. Useful compression includes a path
to the complete original stdout. The native tool still reports stderr and exit code.
Before test6, Lite statistics counted stdin/output bytes, including the recovery
hint, for this chat. The versioned accounting below replaces that presentation and
keeps the earlier totals separately. No subscription quota estimate is derived.

Cancellation uses a separate request and a per-attempt nonce protected by a state
lock. It invalidates activation even when the window closes before a worker reply.
An installed component may remain after cancellation; it is not activated by that
cancelled attempt. A running agent is restarted only after explicit confirmation
for that chat, through the existing identity-checked restart path.

Gemini and OpenCode show an unavailable explanation: their integration is not
verified. Tests using real CLI binaries and synthetic providers are not evidence
of actual cloud-model behavior. See the version-specific validation report.

## Expanded coverage in 2.1.6-test2 /1523

The first build was intentionally limited to three Git families while checking
native execution policy. This is still a partial integration, not all RTK commands.
Only single literal shell commands are rewritten. Pipes, redirects, substitutions,
assignments, globs, multiline commands and unrecognized invocations run normally.
Arguments and the original command text are not replaced by an `rtk <command>` call.

| Commands | Output treatment / limits |
| --- | --- |
| Existing git status/diff/log variants | Existing RTK pipe filters |
| rg / grep | RTK grep filter; reduction requires file:line:match output (for example grep -H -n). Other formats may pass through unchanged. JSON, binary, replacement/preprocessor forms excluded. |
| rg --files, non-mutating find forms | RTK find filter; NUL/custom output and exec/delete forms excluded |
| cat / head / tail | RTK read preview of captured stdout; no file access beyond the already authorized command; follow/stdin/binary forms excluded |
| gradle / ./gradlew | RTK read preview of stdout, followed by the final 40 original lines. NOT the dedicated RTK Gradle parser. Continuous/daemon-control forms excluded. |
| pytest / python[3] -m pytest | RTK pytest pipe filter; debugger/watch modes excluded |
| cargo test, go test -json, go build, ctest, vitest run | Respective RTK pipe filters; only captured filter samples tested for these extra runners |

The pinned 1.1.0 `pipe` command has no Gradle or read filter. Its standalone
`gradlew` wrapper executes the build itself; replacing the original invocation
would again hide the original prefix. The app therefore uses `rtk read` only on
its own captured stdout and keeps the original process under native checks.
[Upstream pipe filter list](https://github.com/rtk-ai/rtk/blob/v1.1.0/src/cmds/system/pipe_cmd.rs).

Detected diagnostic words (failure/error/exception/warning etc.) cause full stdout
passthrough; stderr always stays outside RTK. This conservative detector is not a
complete semantic classifier of arbitrary tool output. The original stdout remains
available whenever compressed, and reads of an `originals/` path are not rewritten.
Output without actual byte reduction is returned unchanged.

Opening RTK settings in the new APK atomically refreshes the app-owned installed
Python bridge, without replacing RTK, clearing state or restarting agents. Existing
native hooks still point at that path. New application adapters still require their
normal explicit chat restart if they were not already loaded.

See [test2 validation](VALIDATION-2.1.6-test2.md) for local command runs versus
native-agent fixtures, unsupported sandboxes and remaining phone/model checks.

## Saved stopped chats and native mode in test3

RTK can be prepared before chat startup. The checkmark then means saved selection,
not a connection. Native mode delegates rewriting to the pinned OSS hooks and
requires explicit acknowledgement of its agent-specific prefix/approval warning.
Full output remains available through native `rtk recall --full`; each chat has
its own tracking/recall/config directories. User-global RTK config is not imported.
See [test3 validation](VALIDATION-2.1.6-test3.md) for scope and unverified scenarios.

## Output accounting v2 (test6 /1527)

`statistics-v2/epoch.json` freezes the previous Lite byte totals and native estimated
`history.db` token totals. Old files remain. New per-chat measurements begin at the
first new bridge operation; switching modes does not reset them. Both modes remain
separate. No new estimated-token or subscription-savings percentage is derived.

Lite captures actual original stdout after command flags/filters, never a file size.
The filter event records exact bytes and hashes. A scoped observational PostToolUse
hook matches the returned stdout before accepting a before/after pair. Claude's
trailing-whitespace presentation is accepted only on a matching measured hash and
applied equally to the captured original and filtered text. Stderr is included on
both sides when the CLI supplies it separately. Unknown/truncated mismatches remain
unknown, not zero. Filtering failure and oversized passthrough are recorded too.

Stock RTK1.1.0 `read` tracking can use the entire file size for a head/tail window.
Those token estimates are not reused as a baseline. A verified literal head/tail
rewrite with default unmodified read output is an identity operation (0%). Other
native command baselines are currently unknown; delivered bytes remain measurable.
Neither commands nor original files are reread to manufacture a baseline.

The PostToolUse observer counts UTF-8 tool-result text, excluding transport JSON,
Codex's outer execution metadata, and re-sending older model history. Native CLI
fixtures compare these counts against the subsequent captured provider request.
`rtk recall` and supported reads of this chat's Lite originals count their returned
text separately, including the CLI's output limit. Saving a recovery file does not
increase received bytes. Unsupported response shapes are explicit unknown counts.
Arbitrary custom programs reading recovery files are not attributed automatically.
Other post hooks that later replace a result are outside this verified boundary.

The new hook registration needs the next normal chat start. Older running adapters
keep their execution path; new captures without a receipt say unconfirmed and do not
produce a savings percentage. No automatic restart or saved-permission changes.
The native binary still uses execve, with its existing exit/signal/stream behavior.
The third app-owned Codex hook receives only its own trust entry. Claude SDK and
native terminal settings register the same observational post hook.

## Compact summary (test8 /1529)

The resource dialog shows connection state, a selected-mode summary and a short
description. Details start collapsed and retain both modes, byte counters, recall,
command counts, measurement date, legacy estimates and mode explanations.
Summary tokens are a display-only estimate (UTF-8 bytes/4), not tokenizer results.
Only fully known comparison groups show an overall reduction percentage. Unknown
or partial baselines show received output and unknown reduction; legacy full-file
estimates never substitute for the current baseline. Backend accounting is unchanged.
