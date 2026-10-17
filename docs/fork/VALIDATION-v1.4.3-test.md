# v1.4.3-test — local validation

Package com.termux, versionCode1107, versionName1.4.3-test, minSdk26/targetSdk28,
arm64-v8a only. Certificate SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK 53014041 bytes, SHA-256 `398789666b6d18e7c88210e6e23d110081fe7cdc06c086b15809642612c5d28e`.

## Evidence

- Final full JVM suite: **495 tests, zero failures/errors/skips**. Relevant new
  deterministic coordinator tests exercise tool→result→answer without another user
  turn, explicit absolute paths, default automatic writes, optional approval,
  approval denial, permission-mode gating before approval, cancellation before
  action/after Yes, one repair for offers/thinking-only, preserved raw history,
  removal of UI-only events from model context and independent saved settings.
- UI source tests cover a separate scrollable reasoning dialog with no inline
  expansion, empty-reasoning hiding, one-shot Yes/No decisions, restored pending
  cards disabled, cancelling a waiting approval, and finish metadata transmission.
  Mocks do not prove keyboard/gesture/scroll behavior on Android hardware.
- Actual native test `jobs_prompt_check` applies the real legacy llama.cpp renderer
  to the official Qwen3.5 chat_template.jinja and verifies the official non-thinking
  suffix is restored exactly once; unrelated templates remain unchanged. Test log:
  native-prompt-check.log. This does not prove inference with actual Qwen weights.
- JNI bridge rebuilt; matching Java int/native jint signature reviewed. CPU reports
  EOS vs token budget; SDK only reports callback completion. Host JNI inference
  remains unverified; no claim that SDK completion reveals token-budget exhaustion.
- Release assembly and DEX runtime-definition gate passed. Legacy Lint still emits
  modern Kotlin metadata diagnostics; no clean-Lint claim is made.
- ZIP CRC, duplicate/path checks, package/version/ABI/signature and limited private-
  file/secret-pattern scan passed. Exact Java/resource/asset/local-C++ staging
  comparison passed. ARM64 ELF/16KiB LOAD alignment verified. Native libraries other
  than the changed local llama bridge remain byte-identical to v1.4.2-test.
- Baseline recovery1.3.4/code1108 signature checked; payload equals the original
  except manifest and signing metadata. SHA-256 `7ddcac019943e4e539b53c4072a73081caa9c0a7e05c85f8aa26fab774e5963f`.

## Device checks and limits

All local engines/custom models use the same coordinator. The Qwen template fix
is deliberately template-specific; Gemma does not receive Qwen tags. Clear action
requests get stronger instructions and one repair attempt, not a guarantee that
all models will correctly choose/format every action. Invalid prose and reasoning
remain non-executable. Existing mode/path checks still gate valid requests.

Required device checks: fresh-chat file listing outside project with path checks
off, actual full-access write/command in a disposable project, Yes/No/cancel and
reopening historical cards, separate reasoning window scrolling and chat position,
actual Qwen and LiteRT generation including process death/memory pressure. Check
that a denied action leaves the file unchanged. Do not use a critical project as
an inference test fixture. Existing cloud-agent/PTY sessions are not modified.

No data reset; schema v8 optional confirm flag defaults off, preserving automatic
full access. Read/chat cannot be elevated by approval. Shell commands are not a
project sandbox. Diagnostic logging is independent of authorization. Detached jobs
can survive cancellation; foreground tasks only. Model/chat files remain outside
current backup-manager scope. NPU runtime, llama GPU, image/audio and MTP remain
unavailable. Recovery cannot open local chats and can discard new preset fields;
a later build after recovery must have code>=1109. No GitHub publication.

Sources: official Qwen model card and template linked in LOCAL-AI.md; downloaded
read-only evidence is under this ignored verification directory.
