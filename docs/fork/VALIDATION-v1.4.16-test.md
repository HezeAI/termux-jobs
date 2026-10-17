# Validation — v1.4.16-test / code1122

624 Java tests passed (0 failures/errors/skips), including native progress metadata
validation, stale/invalid update rejection, request reset, first-token transition,
LiteRT unknown counters and no generated text in the timing report. 45 of those
cover LocalAiLimitsTest/LocalAiWorkspaceTest; they are a subset, not extra tests.

Real CPU tiny-GGUF checks pass: fresh vs reused-prefix next-token logits, forced
sequence snapshot restoration vs fresh logits, shorter and mismatched inputs,
chat/system resets, disabled scope, snapshot budget and invalidation. Native bridge
and ARM64 APK rebuilt. Runtime DEX gate passes with15896 defined classes and30
explicitly reviewed unresolved types. Existing build-tool warnings remain; this
is not a claim of warning-free Lint.

Review: cache scope sent through private IPC is validated and prefixed by model
owner. Exact system instructions and chat scope must match. Errors/cancellation
clear cache; model release frees context and snapshots. Cache is RAM-only, stored
snapshot budget128MiB (temporary allocations additional). Unsupported rollback,
oversize snapshots and failed restore use full prefill. No new disk history,
authorization, external-agent adapter, network or process-shutdown paths added.
Performance IPC callbacks return without completing the request latch.

Final archive CRC/paths/duplicates, com.termux/code1122/1.4.16-test/minSdk26/ARM64,
matching signing certificate, source-to-build-stage match and limited secret/file
name scan passed. Non-llama native libraries and external agent assets unchanged
from1.4.15-test. No data/schema reset. Evidence: .verification/local-ai-v1.4.16.

Qwen3.5 hybrid rollback/snapshot behavior and actual cold/warm timings require
in-app testing. Tiny-model tests do not establish Qwen speed gains or Android UI,
keyboard, lifecycle behavior. Compare diagnostics for cold and follow-up requests.

APK SHA-256: e52bd7f4ab0f29acb68e37998db45d9035ff7186c99675b5003abb38b9253f89
