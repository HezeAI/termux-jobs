# v1.5.16-test validation

## Relevant evidence

Nine service-context tests cover the unchanged LiteRT default path plus stable llama system with separate counted feedback, protected correction under fitting/overflow, unchanged actual user request and original transcript, no false compaction notice, reduced service progress/reserve budgeting and Qwen tool-result ordering. Final full Java suite counts are recorded below after completion.

17 real-SDK interface checks with fake inference passed (`.verification/local-ai-v1516/sdk-results.log`). They exercise the unchanged LiteRT path, actual SDK config/structured functions and error/cancellation handling; they do not measure NPU hardware/model output.

A standalone Java probe uses the production LocalAiServiceContext and LocalAiProcess classes to generate actual request JSON, then a separate packaged Ubuntu ARM runner and tiny stories260K model process evaluate it. Initial675tokens/0cached; changed progress683tokens/650cached/warm_prefix; different chat683tokens/0cached; changed rights in that same chat690tokens/0cached. Logs: `cache-prompts.json`, `cache-results.json`. This proves request construction and cache separation on the test model, not Qwen recurrent/hybrid snapshots or real Qwen latency. First cold pass remains expected.

## Code, trust and state review

Only llama calls prepare(...,stableSystem=true); LiteRT retains the existing system/progress path. The actual counter wraps original conversation with transient feedback before fitting, so role/template overhead is included. Required correction is added outside removable history while counted. The resulting feedback is marked appFeedback and is not mistaken for a new user request; the saved history never receives it. Original base rights/schema text is not edited. No native equality, owner/chat scope, cache budget/snapshot or cancellation checks were weakened. Old state cannot cross changed rights/system/chat. No native build or model mutation.

UI changes are scoped to built-in qwen35-2b Engine settings and dropdown row dimensions. Old GPU value changes only the editable selection when Engine settings opens; applying remains subject to existing save/busy/reload logic. Current engine and stored profile are unchanged merely by opening the dialog. Imported model choices/LiteRT settings, main selector, Server+Agent and session callbacks remain unchanged. No history migration/reset.

## Artifact

Final release build and required packaged runtime-class audit passed. Existing successful-build Kotlin metadata diagnostics retained. ZIP CRC/safe entries, package com.termux, minSDK26, code1153, ARM64-only ABI, private AI flag, seven QNN/LiteRT libraries, v2 certificate, limited secret scan and unchanged native/external-agent payloads passed. APK entry-set comparison with1.5.15 found no additions/removals; compressed payload differences only expected DEX/manifest. Final Download SHA independently verified at delivery.

SHA-256: `aed3f4dfaef77326663a0317e1dcacf5da2802cb24f49768790a60e0f60ceeea`.
Size: 108704426 bytes.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

## Device checks needed

Update over1.5.15-test without clearing data. Repeat the exact Qwen locations/write/read request in a new chat, CPU ARM/6threads/Ubuntu/8192/512, export diagnostics. Compare per-pass cached tokens/cache reason/input time; never infer a percentage speedup from the tiny test. Confirm actual file and answer, no repeated loop, unchanged confirmations, correct CPU tile and dropdown size at device DPI. Real Qwen hybrid cache reuse and UI gestures remain unverified by automation. No GitHub publication, APK installation or working-session/environment changes.

## Final suite and delivery

Final full suite passed:664 app +145 emulator tests, zero failures/errors/skips. Updated fixed-CPU and216dp-popup/52dp-row assertions passed in final XML, alongside9service-context cases. Log: `.verification/local-ai-v1516/tests-final.log`; final counts saved in test-counts.json. Download copy SHA verified independently.
