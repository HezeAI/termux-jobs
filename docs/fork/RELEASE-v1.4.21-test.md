# v1.4.21-test / code1127

Local-AI diagnostics add Input processing test for a loaded, idle llama.cpp model.
The test runs in the same native inference process/selected CPU library with fixed
synthetic token IDs. No messages, file-tool results, chat templates or prefix cache
are supplied, and no answer tokens are generated. Model weights are reused; each
trial allocates/frees a temporary context without touching the working context.

Choose one test:
- Input length:512/1024/2048/4096 tokens, current thread count (normally4), batch256/ubatch128.
- Threads:2/4/6/8 at2048 tokens, batch256/ubatch128.
- Batches:256/128 versus512/256 at2048 tokens and current thread count.
Each matrix runs forward then backward to expose order/initialization differences.
Context capacity stays that of the loaded model. Test-only settings do not change
ordinary chat settings or automatically select a new thread count.

The report contains per-trial exact input count, context/threads/batch/ubatch,
decode wall time, trial elapsed time, summed inference-process CPU time, throughput
and per-batch timings. Context allocation is excluded from timing. CPU time sums
worker threads, can exceed elapsed time and is not whole-device CPU usage. Decode
is synchronized for measurement; context creation/callback overhead is distinct.
The synthetic repeated pattern is a controlled workload, not proof of speed for
every real conversation/model. Other processes and device scheduling are not controlled.

Stop test cancels at the next decode-batch boundary, preserves partial results and
frees the temporary context. The benchmark has a private cancellation token/gate;
a queued or late test stop cannot leave the next chat generation cancelled.
Service checks model ownership. Starting requires idle local AI; other local model
loads/settings are blocked while it is busy. Closing the report does not stop the
test; Stop also remains available on the selected session. Closing that session/app
cancels then queues release of only its owned local engine. No Linux sessions stop.

Last benchmark report is saved separately from normal timing, can be reopened via
Last report, and uses existing copy/path/Download export. Reports contain metadata,
not chat text or the synthetic token sequence. LiteRT has no equivalent native
prefill benchmark in this build; normal LiteRT timing remains available.

Previous1.4.20 schema recovery, cached-read loop guard and ARM selector wording are
included. Same com.termux/certificate/code1127 above1126, no schema/history reset.
No GitHub publication. Start with Input length on current4 threads, save report,
then Threads and save another report; Batches is optional next comparison. Compare
actual results before changing default thread/batch values.
