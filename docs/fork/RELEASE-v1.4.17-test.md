# v1.4.17-test / code1123

Existing local-session diagnostics now retain separate metadata records for each
model call within the latest user request. A tool workflow can contain several
model calls even when the user sent one message. Each record includes call number,
completed/in-progress/not-completed state, input/processed/cached/to-evaluate token
counts, cache status, native input/cache/setup/first-token durations, time to first
model text, generation duration and output tokens where supported.

llama.cpp exposes exact native counters. LiteRT-LM receives the same per-call report
with available stages/text timings; native token/cache counters stay unavailable,
not estimated. Per-call history is bounded to64 records and shows omissions. Paused
journaling skips archival records; the existing current summary remains available.
No prompts, generated text or file contents are copied into timing records.
Existing private report persistence and Download export include these records.

This build measures the slow intermediate call; it does not change model prompts,
tool behavior, inference settings or native prefix reuse. Existing template/cache
limitations and cold-start cost remain. Compare per-call input size, cached tokens
and cache status to distinguish prefix misses from processing new tool output.

Same com.termux/certificate, code1123 above1122. No library format/schema/history
reset. Install over the previous local test version. No GitHub publication.
