# Private 2.1.7-test2 /1535 — RTK receipt matching

No application release or automatic installation is authorized. Includes the
Arch/removal and offline-restart fixes from test1. Only RTK measurement matching,
recall-help classification and version metadata change from test1.

## Diagnosis of the reported 216 → 242 commands

The first242 stored events match the phone screenshot exactly: received1649830B,
known original68154B. Every event217–242 has an empty nativeArgsHash. This is not
26 unsupported RTK commands: one counted event can contain several shell commands.
The native history database records actual RTK execution and sidecars contain
measurements for constituent commands. Per-chat totals span several run IDs.

Examples from the current conversation, correlated with the native journal:

- `git status --short; rg -n ...; sed -n ...`: the native hook rewrites Git/search,
  but the old observer cannot match a whole shell result to one argv/hash.
- `rg ... --glob '*.py'`: the old literal parser rejects `*` even inside quotes.
  An isolated rewrite-only probe reproduced RTK execution preparation with an
  empty observer fingerprint. The fixed parser preserves quoted arguments.
- `rg ... | head -65`: RTK may rewrite search, but the downstream head changes the
  scope. Summing the pre-head RTK baseline would give the wrong comparison.
- `sed -n ...` is not rewritten by the tested native hook. `git diff --check`
  can run through RTK passthrough without an exact-measurement sidecar. Neither
  should be called a measured saving.

The screenshot's26 events were selected by count and verified aggregate bytes.
Shell text attribution uses timestamps, normally within0.2–0.5s; two commands
inside one exec block require caution. No guessed percentages were backfilled.

## Implementation

Changed **the application's Python observer**, `rtk-mode.py`, not RTK filters or
execution/permission rules. Quoted literal patterns now obtain the correct argv
fingerprint. Literal `;`/newline sequences can match each RTK invocation to a unique
byte/hash segment in the delivered result; unchanged gaps stay unchanged. Every
RTK invocation must have an unambiguous measurement. Matching work is bounded.
The final segment accepts measured CLI newline normalization. Old event files,
chat settings and recovery files are retained. Receipt replay remains idempotent.
`rtk recall --help`, `-h` and `--list` do not count as recovered command output.

Pipelines, redirects, substitutions, glob expansion, shell control flow, ambiguous
matches, empty sequence segments, unmatched/truncated results and uncovered native
commands remain unknown. A quoted glob passed to rg is different from shell glob
expansion. No command is rerun to manufacture a baseline. Background execution
through exec_command/yield followed by write_stdin is supported when the native
PostToolUse supplies its complete final result; detached shell `&` is not covered.

## Checked anew: this live chat

A temporary bridge selected only this profile; other profiles used the original
helper. Native RTK jobs.1 and permissions were unchanged. The hook reported
`permission_mode=bypassPermissions`; no claim is made for other permission modes.
This was the current real chat, not a fake model. The returned tool bodies were
received by the model and matched to native sidecars and persisted app statistics.

| Case | Original → delivered bytes | Result |
| --- | ---: | --- |
| Search with quoted regex/glob,750 matches |120480 →14612|Measured compression before delivery|
| Short search,30 matches |4800 →4800|No reduction|
| Separate file read |1125 →1125|No reduction|
| head -n5 |225 →225|0% reduction, selected window only|
| cat; head sequence |1350 →1350|Both measurements matched one result|
| sleep3; cat, yielded then write_stdin |1125 →1125|Final receipt matched after polling|
| rg ... pipe head5 |unknown →235|Unknown retained|
| Same large search with CLI output limit |unknown →7307|Truncation did not become a guessed baseline|
| recall --lines5 |+800 separately|Not added to command saving|

The known totals rose from68154→7240B to197259→30477B during these checks.
All pre-existing event records remained byte-equivalent as JSON values.
The first recall attempt used incompatible --from/--lines flags; its151B error
was separately received. A617B help response exposed the old help-classification
bug, now excluded for future calls by a dedicated test. Historical counters were
not rewritten to hide those attempts.

The installed bridge was already byte-identical to its original backup at final
restore verification. The app's existing status refresh can replace this helper;
no extra overwrite was necessary. No user session was stopped or restarted.

## Checked anew: Claude CLI with a synthetic provider

Actual Claude Code2.1.296 via the app SDK adapter, manual/default mode, isolated
fake credentials and local provider. This is NOT a real Anthropic model test.

- Quoted-pattern search:23781→1526B; provider-request tool_result is exactly1526B.
- Compound `cat sample.txt; head -n5 sample.txt`:17294→17294B after Claude trimming;
 observer and provider agree, one event counted once.
- A simultaneously isolated chat with RTK off retained the ordinary command output
 and had zero RTK command events. Settings/rule files were not changed by the hook.

## Component and compatibility

The Components archive contains only rtk, LICENSE and THIRD_PARTY_NOTICES.txt.
Its binary and pin are unchanged (1.1.0-jobs.1); no new component revision or
publication is necessary. The Python observer is shipped by the APK and refreshed
by the existing RTK status/installation path. No new download or binary replacement,
statistics reset or recall migration is introduced.

## Local checks and final artifact

- 61 RTK helper tests passed, including isolation, preservation, receipt replay,
 quoted patterns, compound matching, normalization, ambiguity and unsupported paths.
- 95 native build inputs compared anew and matched; unchanged JNI payloads reused.
- Full Modern Lint7.4.2, all five modules:0errors,290warnings; infrastructure clean.
 The final additional explicit-RTK branch is Python-only and covered by the final
 helper suite; Java/resources/version inputs match the completed Lint run.
- Final signed APK: com.termux,2.1.7-test2/code1535,arm64-v8a,non-debuggable,
 ZIP/runtime/DEX/assets/signature checks passed;638production source files matched.
 All7packaged ELF files are ARM64. No embedded RTK archive or temporary live wrapper.
 Signer SHA256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
 APK: `/sdcard/Download/Termux-Jobs-v2.1.7-test2-arm64.apk`, 40675153bytes.
 SHA256: `39b13ee632b1d326657d2a7f9a58f4c501f87c18f19956bd4fe7553d1d13909c`. Download copy independently matched.

## Not checked

New APK installation and visual RTK dialog verification on the phone remain for
the user. Backend statistics through the helper are checked; this is not a claim
of having tapped the new APK's Android dialog. No new GitHub CI run or real Claude
model test was performed. Other permission modes are outside this live check.

Evidence: `.verification/rtk-current-audit/` (events217–242, rewrite probe, scoped
live hook receipts, before/after statistics, tests, native input identities and APK).
