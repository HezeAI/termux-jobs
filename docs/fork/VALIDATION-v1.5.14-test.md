# v1.5.14-test validation

## Relevant testing

Full Java suite passed: 660 app tests and 145 terminal-emulator tests, zero failures/errors/skips. Ten added cases cover separated service progress, preserving mandatory instructions/correction/current request, preferring shorter service progress before a smaller reply reserve, oversized protected input, application feedback boundaries, capacity stop after completed tools versus first-request error, separate generator arguments, service-budget diagnostics and session-dropdown selection/layout.

17 real LiteRT SDK interface checks with fake inference passed, including reduced service progress in the actual conversation configuration, unchanged base system field, retained current request, automatic tool calling disabled, and no stale progress on the next request. These checks do not run a model or NPU hardware.

Evidence: `.verification/local-ai-v1514/tests-final.log`, `test-counts.json`, `sdk-results.log` and `build.log`.

## Code and state review

Both local engines receive base instructions, app-owned progress and required correction separately. Only the explicitly supplied app progress can be abbreviated/omitted; no marker-based stripping of user/custom instructions. Schemas, rights and current request remain mandatory. Existing history/tool-result fitting still uses its previous excerpt policy. Application feedback is not mistaken for a new user task when fitting or summarizing history. Each generate request overwrites the separated service fields, including empty defaults for other callers.

If context capacity fails after tools completed, the coordinator checks cancellation first, ends the turn with context_budget_stop and preserves recorded cards/results without claiming task completion. Capacity failure before any successful action remains an error. No command execution, approval, path scope, model weight, sampler, context-capacity or process-shutdown change.

Session dropdown rows alone receive the shorter shape, explicit vector icon, model subtitle and active-session highlight. Popup width, session creation and preset labels are retained. Normal terminals and external-agent assets are unchanged. No user-data reset or migration.

## Final APK

ARM64 release build and required runtime-class audit passed. Existing Kotlin metadata diagnostics remain in the successful build log. ZIP CRC, unique safe entries, package com.termux, minSDK26, private versionCode1151, ARM64-only ABI, v2 signing certificate, private local-AI flag, seven QNN/LiteRT libraries, limited embedded-secret scan, unchanged existing native payloads and external-agent assets passed.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK SHA-256: `de2d38682c268bea9beed394517eb54f87eaf62da5728b49f7ead1a51e89b592`.
Size: 104892684 bytes.

Download copy independently SHA-256 verified. No publication or changes to running sessions/installed environments.

## Device checks needed

Repeat the original locations/write/read task in a new Gemma chat with the previous settings. Inspect final file, cards and diagnostics; if capacity is exhausted, confirm an explicit stop without false completion or repeated actions. Check the corresponding llama.cpp path separately. Verify dropdown row spacing, robot/model subtitle, active selection, taps and long taps at the developer's DPI. Automated UI and fake-inference tests do not establish device layout or real-model behavior.
