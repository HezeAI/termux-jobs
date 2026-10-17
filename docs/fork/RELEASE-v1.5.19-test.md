# v1.5.19-test — fitted LiteRT cache and bounded completion checks

Private build, com.termux, versionCode 1156, ARM64, minSDK 26. No GitHub publication. No persistence reset/migration or model changes. Package/certificate and increased code must be verified before delivery.

## LiteRT

Successful owned text-only SDK conversations may retain the actual fitted history even when it differs from the full canonical input. The adapter keeps canonical expected append and actual SDK history separately. Only the exact same canonical history followed by one new message can reuse; changes to any earlier canonical text (including dropped turns), rights/system/scope/schema/protocol/sampling/reply limits, cancellation, failed or malformed output and media still invalidate. Before reuse, actual runtime history including retained feedback is budgeted and its prefix must remain exact. Further compaction creates a new conversation when the existing runtime prefix no longer matches. `retained_compacted` is diagnostic evidence of retention after fitting, not an exact count of cached NPU tokens. No SDK version or vendor binary changes.

## Both local engines

A local, per-turn check recognizes a limited set of explicit English/Russian file-operation claims and generic completion wording for explicitly requested write/read/delete/install operations. Only successful tool results recorded during this turn count. Old chat messages and folder discovery do not establish file write/read. One compact correction is sent on an unsupported claim; if repeated, the app returns completion-not-confirmed rather than the model's success claim. Normal requests add no permanent prompt or extra model call. The correction consumes context/inference only when triggered. Chat-only mode does not retry forbidden operations. Existing validation/authorization/path/approval/cancellation/loop guards remain in the actual execution path; the app invents no tool calls.

This is conservative wording/tool-kind matching, not a universal intent, target, content, ordering or truth verifier. Negatives/examples/discussion and ordinary prose writing are excluded where recognized. A successful arbitrary command may perform file operations; this check does not interpret shell semantics and does not certify its side effects. It does not prove that a successful read was for the correct file or that readback followed the final write. Model quality, unrelated unsupported claims and ambiguous phrasing remain limitations. No LoRA training, history index, new weights or speculative decoding in this build.

## Motivation

Developer v1.5.18 Qwen report6105602387459541652 shows only locations executed, despite a final saved/read claim. Total55361ms,2passes; this is not faster successful task completion. LiteRT reports9056831775559766615 /1754627108666915028 complete folder/write/read in8403ms and reread in7468ms including an additional history-summary generation. `history_compacted` explains why no SDK conversation was retained. These are diagnostic observations, not a controlled speed comparison.

## Device check

Repeat the same Qwen task and Gemma two-request test on this APK. Export reports and screenshots of any completion warning. Inspect actual tool cards, SDK retention/reuse reasons and timing. Hardware speed/quality and user interaction need device verification.
