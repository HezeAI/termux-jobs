# Validation — v1.4.15-test

Local test delivery; no GitHub publication or installed-app intervention.

## Relevant checks

LocalAiLimitsTest covers allowed context/reply choices, per-engine capability,
strict optional profile numbers/types, old-profile defaults, roundtrip/copy,
RU/EN find-latest-and-read with no model generation, complex task exclusion,
clock-controlled phase/first-text metrics, no prompt content in reports,
unknown LiteRT token counts and bounded paused events. LocalAiWorkspaceTest
covers selectors, answer changes without unload/history replacement, LiteRT4096
and stage placeholders. Existing local chat, model tools, context, external-agent,
ordinary terminal, navigation and emulator tests are included.

Native library rebuilt. The same real tiny-vocabulary prompt count/UTF-8/31-message
and Qwen non-thinking-template tests are run. Native generation's emitted-token
count and finish bit are returned together to its matching new Java caller; the
configured cap is clamped to2048. Actual model completion/throughput at8192 and
large replies are NOT inferred from unit tests or a successful compilation.

## Code review and logical inspection

New optional v8 session integers default to4096/768 and copy through presets.
Service validates incoming values; LiteRT8192 is rejected, not silently downgraded.
Context and answer values are sent across the private IPC into actual allocation
and generation. Reply space is reserved; excessive current input may still fail
clearly. Both engines keep existing authorization, ownership and cancellation.

Idle context changes capture reload need before updating the profile, queue only
that Remote client's model release and preserve chat/history. Busy changes are
blocked. Completion checks panel identity/closed state and cancellation before
prepare, so Stop/close cannot trigger an unwanted restart. Other terminal, SSH,
Ubuntu, Bridge and external-agent process shutdown paths are unchanged.

Stage/metrics replies are request-ID filtered and do not finish the pending RPC.
Timings use monotonic clocks, do not store streamed text, paths, commands or file
contents, and are capped at160 events for the last request. Pausing suppresses new
event entries, not the model or live stage; its preference is per session. Last
reports are written atomically on the worker with validated session filenames;
report restoration is on the worker and dialog refresh reads in-memory strings.
Existing diagnostics export/copy/storage-access flow is reused. The last report
can differ from current settings if no new request has run, as expected.

Only an unambiguous terminal 'find ... and read' suffix is normalized into the
existing fast latest-text parser. Extra analysis/edit/run tasks stay with the
model. File operations use the same permission/path/environment checks and literal
file rendering. No global automatic tool execution is introduced.

## Remaining device verification

Manual checks needed: live context reload and Stop race,8192 RAM/latency per model,
actual LiteRT output caps, phase UI/keyboard/scroll while switching tabs, live
stage elapsed/first-token/throughput in real inference, diagnostics pause/export
and restoration after reopen. Exact native token count is available; LiteRT speed
is unavailable, not estimated as measured. No numerical speedup is claimed.
Existing Kotlin metadata/Lint warnings remain; clean Lint is not claimed.

Final UI-only refinement: hidden reasoning/tool-JSON streams keep the phase
placeholder rather than an empty AI prefix; the first model text updates the stage
once, without a timer-driven chat refresh. All other code from the passing full
suite is unchanged. The final UI and limit suites are rerun after this refinement;
full-suite XML evidence is retained separately rather than mislabelled as a new
full run against this exact UI revision.

## Final results

Full suite623/0 failures/errors/skips; final targeted UI/limits rerun44/0. Native real-vocabulary and Qwen-template checks passed. Final ARM64 build/runtime gate passed (15,896 definitions,30 reviewed unresolved types). Package com.termux,1.4.15-test/code1121,minSdk26,ARM64-only,v2 signature and prior certificate verified. ZIP CRC,source/staging match and limited secret-pattern scan passed. Native bridge matches independently stripped build; other native payload and external controller assets unchanged fromv1.4.14. APK53121576 bytes; SHA-256 `29dc4d416fdc1f0950ad4addae7b57e61ecc198513b79c196b4bacc9bd7c4e0e`. Download copies hash-verified. Device scenarios listed above remain unverified.
