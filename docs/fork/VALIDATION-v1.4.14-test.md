# Validation — v1.4.14-test

Local test delivery. No GitHub publication or device installation is part of this task.

## Changed behavior and relevant tests

LocalAiContextTest covers capacity/reply-reserve budgeting, preservation of current
system rights and user task, old-turn removal, marked tool excerpts, reasoning
removal, unchanged saved input, too-large current input, current-chat search and
pagination, UI-event exclusion, archive snapshot boundaries, cancellation,
read-cache reuse/invalidation/explicit refresh, history schema/permissions and
recognition of retryable LiteRT context errors. LocalAiRecentTest now verifies
that a simple latest-text read completes without a model generation.
LocalAiWorkspaceTest checks restored service labels and literal file content,
including tool JSON/think text which must not become an execution preview.

Native library built from repository source. The shared prompt tokenizer is tested
against a real tiny GGUF vocabulary in an isolated test process: UTF-8 system
permissions, all 31 messages (no hidden 20-message clipping), exact vector equality
with separately formatted/tokenized chatml and shorter-input counts. This is not
a test of Qwen generation on the installed app. The existing Qwen non-thinking
prompt-policy check passed. JNI counting/generation share the same helper; exports
are checked. No running model/process or environment was changed by these tests.

## Review and logical paths

Saved history is not rewritten by context reduction. A private working copy uses
the configured runtime capacity and answer reserve. Whole old turns/reasoning go
first, then intermediate current-turn tools and marked excerpts. Media omissions are explicitly marked and the latest image is retained. Current rights
and latest user text are not silently cut. Exact GGUF counting precedes inference;
LiteRT uses an estimate and at most one recognized-overflow retry. Service
begin/done/failed signals are request-ID filtered and do not release the pending
RPC as a final answer. Notices are saved outside model context, once per turn.

History lookup uses only a clone of this chat before the current user request,
never another file/chat/environment; results are untrusted. Existing read/full
permissions and environment validation apply; Chat-only has no tools. Read cache
is per turn, only successful reads, invalidated by writes/commands/installations;
refresh explicitly bypasses it. Direct file output follows a successful normally
authorized read, is rendered literally and is not replayed as an assistant's
instruction (the original untrusted tool result remains available in history).
Cancellation and ordinary terminal/external agent paths are unchanged.

## Limits and device checks

Currently all local runtimes are configured at 4096; architectural maxima are not
used. Reply maximum remains 768. LiteRT estimates/image allowances cannot guarantee
every input fits, and an oversized single user request can still fail explicitly.
No AI summary or semantic-memory index is created. History lookup is literal and
model-driven; existing 8 MiB history-loading limit remains. Real models, LiteRT
vision/overflow responses, actual Android keyboard/scroll and long-chat latency
require device testing. No measured speedup is claimed; simple direct display
skips inference by construction. Existing Kotlin-metadata/Lint warnings remain.

Final machine artifact evidence is stored under .verification/local-ai-v1.4.14.

## Final results

613 Java tests passed (0 failures/errors/skips), ARM64 release build and runtime gate passed (15,885 definitions, 30 explicitly reviewed unresolved types). Real tiny-vocabulary token equality and Qwen prompt-policy checks passed. ZIP CRC, package com.termux, version 1.4.14-test/code1120, minSdk26, ARM64-only, v2 signature and previous certificate verified. Source/staging match and limited secret-pattern scan passed. Rebuilt llama bridge matches an independently stripped copy; all other native payloads and external agent controller assets match v1.4.13. APK: 49350670 bytes, SHA-256 `bb6e8cc53323e7376bb6f4dcaa6eb208f383bc12e95b26bb6d27afcb132dd216`. Download APK/README/SHA copies hash-verified. No device result is inferred from these checks.
