# v1.4.2-test — local validation

Package com.termux, versionCode1105/versionName1.4.2-test, Android8+/arm64-v8a.
Unchanged certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK: 52999773 bytes, SHA-256 `2d73ff92253662d1be7818892fec56fdd00e1485b4af06bcf78666ce17a6fb34`.

- Final full JVM suite: 479 tests, zero failures/errors/skips. New regressions cover
  json/tool/unlabelled fences, CRLF/case variants, thinking separation, trailing
  prose/multiple-object rejection, wrong argument types/fields, long-press chat
  action ordering and black inline-speaker presentation with preserved Markdown.
- Release assembly and packaged runtime-definition gate passed. Legacy Lint emits
  modern Kotlin metadata compatibility diagnostics; no clean-Lint claim is made.
- ZIP CRC, duplicate/path checks, package/version/ABI/certificate and limited
  private-file/secret-pattern scan passed. Exact Java/resources/assets/local-C++
  source staging comparison passed.
- All native inference/Termux libraries are byte-identical to v1.4.1-test. Existing
  server-agent asset and baseline Termux native payload are unchanged.
- Reviewed the complete parse → permission checks → tool execution → model-result
  path. Only a complete structured request is accepted; arbitrary code/prose is
  not executed. Modes/path-toggle behavior and process cancellation are unchanged.
- Recovery code1106 signature checked. Baseline v1.3.4 payload compares byte-for-byte
  except manifest/signing metadata. SHA-256 `05158a63a0510924d24256ba18cff97072c010d1790321792460a6e1e8fa3579`.

## Device checks

Prior developer testing showed local model responses and a tool JSON displayed
as text. The exact raw history was not accessible for inspection; JSON-fence
parsing was a proven source-level gap and is now covered by regression tests.
Do not claim every small-model response will correctly invoke tools.

This build still needs device verification of actual tool round-trips, Android
storage permission handling, chat switching/history, keyboard, gestures and the
new black presentation. Minimal check: in a disposable project request a list
of /sdcard with path checks off; verify a real tool result and then select a new
chat and return to the saved one. Image understanding remains unavailable.

No data reset or schema change. Full access still executes actions automatically;
path checking constrains file tools only, not arbitrary shell commands. Local
model/chat files remain outside current backup-manager scope. Old recovery cannot
open local chats and can discard optional preset fields when saving its library.
Updates after recovery require code>=1107. No GitHub publication.

Evidence: `.verification/local-ai-v1.4.2/`.
