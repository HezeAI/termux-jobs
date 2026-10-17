# Validation: v1.4.10-test / code 1116

563 automated Java tests passed, zero failures/errors/skips, in final tests-verified.log. Nine new tests cover legacy policy migration, optional boolean validation/copy/persistence, independent Codex approvals/sandbox for new/resumed chats, OpenCode launch scope and deny-first rule evaluation, immutable helper preservation, editing/saving unchanged Codex profiles, and explicit-only OpenCode scope saving. Existing native mode/chat UI tests updated for the new labels and independent controls. Intermediate test failures were obsolete expectations and an incomplete test profile; final passing evidence supersedes earlier logs.

11 isolated Node websocket RPC scenarios passed: all full/project/read × legacy/never/on-request resume policies, refusal of mismatched server acknowledgement, and refusal of invalid policy input. No real Codex, authentication or user-history access in these scenarios. The fake server does not prove installed CLI/TUI enforcement on a device.

Final ARM64 build passed; runtime gate: 15856 defined classes, 30 explicitly reviewed unresolved types. Existing Kotlin metadata/Lint limitations remain; no claim of clean Lint. APK ZIP CRC, package com.termux, version 1.4.10-test/1116, min SDK 26, ARM64 ABI, v2 signing continuity, final source-stage match and limited private-file/key/token-pattern artifact scan passed. Native libraries and server-agent asset contents unchanged from v1.4.9-test. Limited scanning is not exhaustive proof of no secrets.

Review: no automatic user process restart or policy escalation. Legacy approval mapping preserved; fail-closed resume acknowledgement check retained. New helper copied atomically under a new versioned filename, preserving the previous helper. No global CLI configuration/auth edits. OpenCode configured deny rules precede saved-allow handling in inspected 2.0.28 permission code; however native path resolution is lexical and does not guarantee symlink-safe confinement. UI/docs explicitly describe native external-path policy, not strict isolation. Unsupported Claude/Gemini project controls disabled.

Pending real-device validation: native sandbox support/enforcement in Android/proot, new and resumed Codex chats with selected policies, OpenCode external paths/symlinks/project worktrees and tool denial, settings appearance/localization. Current source/build checks do not prove all runtime cases. Installing APK has Android lifecycle effects separate from merely saving settings. Downgrades to earlier builds ignore optional new fields and may relax newly selected approval/scope policies; review before launching.

Evidence: .verification/agent-permissions-v1.4.10/{tests-verified.log,build-delivery.log,rpc-tests.log,artifact.json,signature.txt,badging.txt}. Earlier logs are intermediate.

SHA-256: a97b4f88630043a5eaa5b86eb6ff678b293decfeb0ef78ebd1c19105234b51ca
