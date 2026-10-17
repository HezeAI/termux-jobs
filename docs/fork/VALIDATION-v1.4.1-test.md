# v1.4.1-test — local build validation

Package com.termux, versionCode1103, versionName1.4.1-test, minSdk26/targetSdk28,
arm64-v8a only. Certificate remains:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK size52,997,473 bytes, SHA-256:
`1610f0c71d9495406c14d5e97a2a5f86015977bb6710e2309ec494d25c1b499f`.

## Verified

- Final JVM suite: **475 tests, no failures/errors/skips**. Final XML totals include
  all modules; earlier progress counts were incomplete while reports were flushing.
- Release assembly and packaged DEX runtime-definition gate passed. Old Lint emits
  Kotlin metadata compatibility diagnostics; no clean-Lint claim is made.
- Relevant tests: streamed/unfinished think tags, preserved reasoning, offline
  Markdown spans, safe links, permission modes, explicit JSON vs prose, traversal
  rejection, path-toggle persistence and enabled default, shell symlink escape,
  direct access with path checks off, atomic file copy/write, independent drafts,
  header control placement and Start/Stop label, pending-history send rejection,
  manager-import progress/length verification and incomplete-copy cleanup.
- Isolated real-process fixture: GNU timeout owned its process group; terminating
  it terminated its foreground child while an independent fixture remained alive.
  User processes were not used for this check. Detached/background jobs are not
  guaranteed to stop and are explicitly unsupported by the experimental runner.
- C++ JNI bridge rebuilt for dynamic permission/tool instructions. Reviewed matching
  Java/native signature. ELF ARM64/16KiB LOAD alignment verified, NEEDED platform
  libc/libm/libdl only, RUNPATH empty. Actual new-model JNI inference remains a
  device check; the prior small native CPU smoke is historical evidence only.
- APK ZIP CRC, duplicate/path checks, package/version/ABI/signature and limited
  private-file/secret-pattern scan passed. Exact source/stage comparison includes
  Java, resources, assets and local-AI C++ sources.
- Existing Termux native libraries and server-agent asset remain byte-identical to
  pre-feature v1.3.4. ProjectFiles upload keeps its existing limit; model import
  uses a separate size parameter rather than changing SFTP transfer behavior.
- Recovery v1.3.4/code1104 payload matches pre-feature APK byte-for-byte except
  manifest versionCode and signing metadata. Recovery SHA-256:
  `db83631021647a667be2af14872db6855bbe7b796ab7ac15a2d58f813c7a1143`.

## Review and device gaps

Full access executes writes/commands automatically as requested. The checkbox
only controls file-path confinement; Android permissions and the selected mode
still apply. Shell commands are never a project sandbox. Canonical checks are
not an isolation boundary against concurrent parent-directory replacement or
other same-UID processes. File/tool contents are untrusted, and small models can
misinterpret them. No claim of model-level security enforcement is made.

Developer reported Qwen3.5 2B CPU chat working in the preceding v1.4.0-test build.
New tools, permission toggle, Markdown interactions, keyboard/layout, shared-memory
permission requests, multi-gigabyte imports and background/process-death behavior
in this build still require device checks. Try a disposable project first; test
read mode, path on/off, simple file write, `pwd`, cancellation, and switching back
to Codex. Check import cancel/outside tap/Back plus remaining-time display.

NPU runtime, llama GPU, image/audio inputs and MTP remain unavailable. Local-model
files/chats are not currently included in the app backup-manager scope.
No data reset. Schema v8 optional fields default safely when absent. Old rollback
cannot open local chats and can discard new preset fields on library save. Next
build after recovery must have versionCode>=1105. No GitHub publication.

Local evidence: `.verification/local-ai-v1.4.1/`.
