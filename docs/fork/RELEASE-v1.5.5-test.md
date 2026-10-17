# v1.5.5-test — private local-AI verification build

Package com.termux; ARM64, Android 8+, versionCode1142. Compatible signing identity retained. No automatic data reset. New optional behavior fields default to previous behavior for old libraries; the separate tools-only flag preserves the selected behavior/custom text. Original chat records are not removed by summarization. No GitHub publication or updater asset for this private build.

## Behavior and context

The existing Behavior instructions entry stays in its original settings position. Inside: Standard / Brief / Custom file, then Tools only checkbox and explanation. Tools only disables the behavior chooser without losing the previous choice. View current and writing help remain available; Back and Rights prompt are at the bottom. Works with llama.cpp and LiteRT. Custom rights text and code-enforced authorization remain separate; disabling behavior never grants access.

Context budget in settings provides an explicitly approximate preview plus the last active-chat input report when available. llama.cpp runtime totals use its tokenizer; LiteRT totals remain estimates. When the requested response reserve cannot fit even after normal compaction, a bounded fallback reduces it to available space (minimum128 tokens) and shows a chat notice. The configured maximum is unchanged. If the current task itself still cannot fit, it fails explicitly instead of truncating the task.

A model-generated current-chat summary is prepared when sufficiently large older history accumulates, keeping recent turns and originals. Summary input uses bounded, marked excerpts, so it is not a lossless memory. Summary and historical files/results cannot grant permissions. A failed summary falls back to existing context shortening, with a notice; no tools execute during summary generation. Creating summaries adds inference work and needs device evaluation.

Native SDK tool descriptions use shorter complete descriptions instead of arbitrary character truncation. command environment guidance distinguishes ubuntu from the internal proot:jobs-local-ai identifier. Failed write/command results cannot be presented as confirmed successful changes. Unconfirmed duplicate JSON draft cards are hidden; actual operation/error cards remain.

## Engine and media

GPU selection is disabled in llama settings; saved GPU sessions can explicitly return to CPU. CPU ARM mode and thread settings remain. The LiteRT operating-mode tile shows the selected accelerator and is disabled (NPU for the pinned SM8750 Gemma). Existing library state is not silently rewritten.

Diagnostics include requested/effective response reserve, image/frame preparation/attachment/omission counts, audio fragment count, and completion reason. LiteRT sdk_done confirms SDK callback completion but does not establish whether the model hit its output-token maximum. Sampled video frames include requested timestamps in the message. Frames can still be omitted under the context limit; this must be checked in device reports. Video means sampled frames without sound; experimental audio means the first5 seconds only.

No model file or NPU graph was patched. Expanded-context SM8750 export compatibility is not established. Hardware behavior and answer quality of the three instruction modes require developer testing; structural unit/SDK tests do not replace it.
