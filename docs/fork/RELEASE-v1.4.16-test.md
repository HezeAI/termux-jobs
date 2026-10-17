# v1.4.16-test / code1122

Local llama.cpp diagnostics separate native setup, input-token processing, cache
work and the first generation step. Live progress reports processed/total tokens
and reused prefix tokens. LiteRT retains its stage diagnostics; unsupported native
counters remain unavailable. Metadata contains no prompts or generated text.

llama.cpp can reuse exact token prefixes within the same service owner, model,
chat and exact system instructions. New chats/instructions (including changed
rights) reset state. At least the final prompt token is reevaluated for fresh
logits. If partial rollback is unsupported (including recurrent/hybrid models),
a matching sequence snapshot may be restored; failed restore falls back to full
input evaluation. Cancellation/errors invalidate state. Snapshots stay in RAM,
never on disk, with a combined stored-byte budget of128MiB; temporary capture
allocation can use additional RAM. Oversized snapshots are skipped.

This cannot accelerate a cold first request. Warm reuse depends on template,
prefix stability, model architecture and RAM. Real CPU tiny-model checks cover
warm/fresh logits, forced snapshot restore, shorter/mismatched prompts, scope and
instruction changes, disabled scope, budget and invalidation. Qwen3.5 hybrid
behavior and speed gains require device checks; no acceleration factor is promised.

Same com.termux/signing identity, versionCode1122 (previous1121). No schema or
history migration, no reset required; existing context/reply settings are retained.
External agent adapters and unrelated terminal paths are unchanged.

Test: open a local llama session → long press → Session diagnostics. Compare the
first request and a follow-up in the same chat; export both reports to Download.
Inspect Input processing, Cached input, Input/cache/setup milliseconds and After
input until first token. A cache miss or skipped snapshot is a supported fallback.
This is a local test APK, not a GitHub release.
