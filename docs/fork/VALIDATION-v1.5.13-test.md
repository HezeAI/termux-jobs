# v1.5.13-test validation

## Source and logical review

Trace: authorization → repeated-work check → optional approval wait → cancellation check → actual runner → recorded result. The new repeated-write warning blocks that proposed call before approval or execution. Read verification remains possible. Different writes, command/install attempts, failed writes and fresh reads showing changed target contents reset the write assumption; cached annotations do not. Scope/access/approval settings are unchanged. There is no global tool-call cap and no process-stop change.

The reported false Not executed card is reproduced by a successful action record, an accepted approval record, then its tool result. Rendering now pairs nearest matching action/tool/output across UI-only messages. New user messages and unmatched outputs remain boundaries; old unmatched legacy results remain visible. Approval history is not changed or cleared. Decision pills are read-only; disabled Yes/No cannot trigger again. Permission granted is not a claim of successful execution.

Pending cards retain request, APT plan and caveats in a scroll area. Only presentation size and collapsed decision display change. UI tests do not prove phone layout/taps/gestures.

## Build and artifact

Release ARM64 build and required runtime-class audit passed. Existing Kotlin metadata compatibility diagnostics remain in the successful build log. ZIP CRC, duplicates/traversal, package com.termux, minSDK26, private versionCode1150, ARM64-only ABI, v2 certificate, private AI flag, seven QNN/LiteRT payloads, limited embedded-secret scan, unchanged earlier native libraries and external-agent assets passed. Previous signing identity retained. No reset/migration, publication, working-session or installed-environment changes.

16 real-SDK interface checks with fake inference passed. This does not test NPU hardware. Final Java results are recorded below after suite completion.

SHA-256: `d1e05f7095337bfba05ea78ef60f0549e4ce23a1d540eebdea9a26fe69fdc205`
Size: 104890056 bytes.

## Device checks still needed

Repeat locations/write/read in the selected local environment and inspect the final file. If the model repeats work, verify the redundant call is blocked and it either reads/finishes or stops clearly. Test Yes, No, cancellation, restored history and opening decision details on the actual device. Verify no misleading duplicate appears after a successful approved write. No claim of exhaustive model-loop detection: aliases/changing contents/arbitrary commands need separate evidence.

## Final verification and delivery

Full Java suite passed: 650 app cases and145 unchanged terminal-emulator cases, zero failures/errors/skips. Ten new cases cover write-stall logic, changing listing metadata, verification after warning, explicit repeats, state invalidation, fresh work, compact/read-only approval decisions, exact history pairing across approval and actual waiting for the click. Log: `.verification/local-ai-v1513/tests.log`; XML counts saved in `test-counts.json`. Final Download copy independently SHA-256 verified.
