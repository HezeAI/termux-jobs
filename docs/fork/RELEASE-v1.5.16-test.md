# v1.5.16-test — keep llama system instructions stable between tools

Private test build, versionCode1153. No GitHub publication.

## Cache correction

Developer report806945256034412762 showed five cold llama/Qwen passes with zero reused tokens; input processing consumed166964 of181406ms. The native cache requires both owner/chat scope and full system equality. App-owned progress changed the system after every tool. Reproduction with the packaged runner/tiny repository test model confirmed unchanged input reuses795/796tokens, changed progress in system resets to0, and separate trailing progress with stable system reuses788tokens. Tiny-model timings are not a Qwen speed prediction.

For llama.cpp only, mandatory base system/rights/tool protocol stays in system; changing progress and required correction are appended as a transient user-role appFeedback message after normal conversation/tool results. They are counted through the actual llama template before fitting, including feedback role overhead. Required correction cannot be removed by history fitting. Existing progress compaction/omission, reply reserve adaptation and explicit overflow stop remain. The feedback is not a new user task, is not written into the saved transcript, and does not itself emit a history-shortening notice.

No native cache equality/scope/snapshot/cancellation checks are relaxed. Changed rights/system/chat still invalidate. No native binary/model/thread/context-default changes. Real Qwen hybrid-state reuse and latency need a developer report after updating. LiteRT retains its previously verified system/progress path and per-inference conversation lifecycle; this release does not claim SDK cache improvements.

## UI adjustments

For built-in qwen35-2b, Engine settings displays CPU as a dimmed disabled operating-mode tile, with no GPU picker. Processor mode and thread controls stay enabled. Opening Engine settings for an old GPU selection changes the editable selection to CPU; the stored profile/active engine is not altered until the user applies settings. Other imported models and LiteRT retain their existing behavior.

Only dropdown session pills are slightly enlarged from180×48 to200×52dp; popup216dp including padding. Main top session selector and Server + Agent are unchanged. Active highlighting, vectors, model subtitle and session actions retained.

## Compatibility

Same com.termux package/certificate, higher private code1153. No chat/history/model deletion, reset or data migration. Update over1.5.15-test. The developer should repeat the same Qwen task with CPU ARM/6threads/Ubuntu and export diagnostics; compare cached tokens, cache status and per-pass input time. First cold pass is expected. Also check CPU tile and dropdown readability on device.
