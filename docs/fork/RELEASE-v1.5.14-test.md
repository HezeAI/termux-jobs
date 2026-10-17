# v1.5.14-test — local context headroom and session list

Private local-AI test build; not published to GitHub.

## Context handling

Developer report6851273631047612741 showed successful writes/reads and a repeated-work warning followed by CONTEXT_TOO_SMALL at pass12. Shared storage access worked; the task did not finish cleanly. Instruction/tool overhead, current input and app progress/correction competed for the4096-token runtime. Actual native token counts remain unavailable; the report's composition is estimated.

The coordinator now passes base instructions, app progress and correction separately. Remote/service IPC resets these fields for every generation, including ordinary summaries, media sub-analysis and probes. Engine preparation budgets them together with the native schema overhead. Under pressure it compacts progress to recent rows or omits that progress excerpt before lowering the reply reserve. Base rights/behavior instructions, native tool schemas, correction and the current user request are not textually shortened by this service-progress fitter. Existing history/tool-result fitting still applies with explicit excerpts and saved originals. This is not a larger model context or an exact LiteRT tokenizer.

A notice and diagnostic character counts identify service-progress reduction. If an app/native context shortage still occurs after tool attempts, the coordinator stops with an explicit statement that full task completion is unconfirmed and previous results remain in tool cards/history. No next tool is executed. An oversized first request still produces an error; it is never silently truncated. Cancellation and unrelated failures remain errors. Ordinary new requests clear the stale previous-error status label.

Native application validation feedback is marked as app feedback, so it cannot be mistaken for a new user task during context fitting or input accounting. The SDK still receives ordinary user-role application feedback, not a fake executed tool response. Rights, confirmation checks and actual tool routing are unchanged. Both local engines use this path; external AI CLI sessions are unaffected.

## Open-session list

Only the oval rows INSIDE the open-session popup are shorter horizontally; the popup width is unchanged. Rows share height, text size, padding and vector icon bounds. Create new session is not highlighted. Exactly the selected session is highlighted; a background terminal is not selected while a local chat is shown. Local rows show the selected model name and a robot vector icon instead of the generic Local AI subtitle. Long names ellipsize with full accessibility descriptions. Local-only session counts are included in viewport sizing and empty-state checks.

Creation/preset environment labels, including Local AI, remain unchanged. The main selector subtitle is unchanged.

## Compatibility

Private versionCode1151, same package/signing identity. No reset/migration; no model weight, context-cap, sampler, output-limit, installed-environment or process-management changes. Device validation remains necessary for actual Gemma completion, context notices, layout, DPI, switching and long-press actions.
