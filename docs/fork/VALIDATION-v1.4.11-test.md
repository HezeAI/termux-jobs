# Validation: v1.4.11-test / code1117

572 automated Java tests passed, zero failures/errors/skips (tests-verified.log). Nine added tests cover greeting/polite/Russian/English short screenshot requests, questions/negations/exact filename exclusions, pure/fenced/mixed tool previews, partial streaming, escaped braces/quotes, single formatting repair before normal execution, mixed educational examples staying non-executable, read-mode rejection after repair, UI card reuse, literal user JSON and incomplete-response failure presentation. Earlier staged test runs are intermediate; final passing evidence supersedes them.

Final ARM64 build succeeded (build-verified.log), runtime definitions gate passed: 15858 defined classes, 30 explicitly reviewed unresolved types. Existing Kotlin metadata/Lint limitations remain; no claim of clean Lint.

Final APK ZIP CRC, package com.termux, version 1.4.11-test/1117, min SDK26, ARM64 ABI, v2 certificate continuity, SHA-256, production source-stage match and limited private-file/key/token-pattern scan passed. Native payloads and server-agent asset unchanged from v1.4.10-test; Codex, OpenCode, Claude and Gemini controller assets also byte-identical. Limited scans are not exhaustive proof of no secrets.

Review/logic: presentation extraction never invokes tools. Embedded prose/example JSON is not promoted to execution. A permitted action request may receive one bounded formatting correction; only a standalone request proceeds through existing authorization and approval. Preview cards remain unconfirmed; actual tool state/result must come from the runner. User messages and raw history remain unchanged. Streaming preview identity is reused for unchanged tool JSON; malformed/incomplete final requests are not marked completed. Screenshot intent handling stays narrow, uses the selected environment and cannot bypass project scope or chat/read permissions.

Device checks pending: actual model compliance, streamed appearance, focus/keyboard/scroll, English/Russian phrasing on a real device and vision-capable follow-up inspection. Automated UI tests do not prove device behavior. Text-only models still cannot inspect pixels. Generic translated/transliterated-name file search is agreed for a future update, not implemented here. No APK installation or GitHub publication performed as part of this task.

Evidence: .verification/local-ai-v1.4.11/{tests-verified.log,build-verified.log,artifact.json,signature.txt,badging.txt}.

SHA-256: ece3ffb035e57fff4a04b25d0d04073aa5a5ade45cab7c38c8778082be8b2dc1
