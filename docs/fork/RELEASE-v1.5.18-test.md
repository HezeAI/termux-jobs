# v1.5.18-test — local adapter and conversation diagnostics

Private APK, com.termux, versionCode1155, ARM64, minSDK26. Not a GitHub release. Package/signing identity must match1154 before delivery. No persistence migration, reset or weight modification.

## Changes

- LiteRT tries shortening/removing optional current app progress before compacting canonical history. Rights, schemas, required corrections and user/tool messages remain budgeted; actual retained runtime prefix and reply reserve must still match. Full context fitting remains the fallback. This can avoid unnecessary conversation recreation; device speed is not yet established.
- Per-pass LiteRT diagnostics record fixed metadata reason codes for selection/recreation and post-response retention. They distinguish scope/instruction/tool/protocol/sampler/reply changes, history changes, media, runtime budget/prefix mismatch and discarded output. No prompt/settings/history contents are logged in these fields.
- Qwen native adapter accepts a short plain explanation before one terminal complete native call, matching the installed GGUF template. Fences/recognized examples, trailing text, multiple calls, nested tags, unknown tools/fields and malformed types remain rejected. This is a formatting compatibility change, not an intent detector. Existing tool authorization, path checks, approvals and loop limits still apply.

## Developer measurements that motivated this build

v1.5.17 Qwen report9199935208846332528: total70375ms,5passes, cached1939/2673/2975/3053tokens after cold2003. Previous v1.5.16 report7281398394140515414:95777ms and fixed1423cached tokens. Different histories/actions prevent claiming a controlled speedup. New screenshot shows rejected leading prose, then successful write/read.

v1.5.17 LiteRT reports630168159897407664 and1537323068246402412:10605ms/4passes and2763ms/2passes, completed. One SDK conversation reuse observed, others recreated alongside compaction. SDK metrics are available; exact cached-token count remains unavailable. These two different requests are not a before/after benchmark.

## Model inspection

Read only the first16MiB of the user-exported installed Qwen GGUF copy; no new duplicate of the1.4GB payload. GGUFv3,335tensors,1942653248tensor elements, qwen35,25blocks, hybrid recurrent/full-attention every4blocks, metadata context262144 (not a supported mobile setting). Template7755characters explicitly allows natural-language reasoning BEFORE a function call, not after. Largest tensor is token_embd.weight with508559360elements (about26%of tensor elements); its size alone does not establish the prefill bottleneck. Original prior full-hash verification exists; this build did not rehash all weights or inspect individual learned values for semantic behavior. Weights are not editable instructions. No LoRA/training/export is included.

## Check on device

Repeat the Qwen file write/read and the same two Gemma/NPU requests. Export reports to inspect reason codes and timing. Model quality, real NPU speed, retained SDK cache details, keyboard/gestures are not established by fake inference or source checks.
