# Private test13 validation — instruction previews, imports and session menus

Date: 2026-11-01. Private build for developer phone acceptance. HezeAI is the
developer; Codex assisted with implementation. [Changes](../../CHANGELOG.md),
[editor behavior](AGENT-INSTRUCTIONS.md), [project instructions](../../AGENTS.md).

## Final artifact

| Field | Value |
| --- | --- |
| File / destination | Termux-Jobs-test13.apk / /sdcard/Download/Termux-Jobs-test13.apk |
| package ID | com.termux |
| versionName / versionCode | 2.1.1-test13 / 1445 (test12:1444) |
| ABI / debug | arm64-v8a only / non-debug |
| Bytes | 39,934,880 |
| SHA-256 | bd7c73764ed96726f7116207f7775841a08a4ba9bd28d57099c628b9d9e011f2 |
| Certificate SHA-256 | dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644 |
| Compatibility | same package/signer, schema8, min26/target28/compile35 |

ZIP integrity, signature/certificate, manifest versions/package/debug/ABI, SHA-256,
required DEX/runtime classes and packaged native/assets were checked on this final
file. All 99 source assets match; 109 native/asset entries other than the deliberately
changed instruction helper match delivered test12. Instruction/import/reader classes
are present; no unintended credential/private-file patterns were found. No reset,
account/rootfs migration or working-chat restart is introduced. Persistence and
navigation checks passed; actual Android update/device acceptance remains separate.

## Checked anew / Проверено заново

- **179 JVM tests**, zero failures/errors/skips: bounded template/storage/history
  previews in portrait/landscape fixtures and larger text; fixed RU/EN selectors
  and Apply/Cancel; source precedence independently of edit choice; explicit apply,
  cancel and conflict behavior; import creation/replacement/confirmation, exact
  destination, override warnings, dirty Save/Discard/Cancel, safe read failures,
  bounds/binary rejection and dismissal during a pending read. Late completion does
  not request a target, save or reopen the dismissed editor.
- Both interface languages: section menus, history/restoration, diagnostics for
  known/unknown CLI, technical disclosure, confirmations/errors, project chooser,
  template and storage language selectors, exact user/history text through language
  changes. The storage addition remains in the menu; the duplicate main row is absent.
- All four agent session menus omit package/diagnostic rows; ordinary/base terminal
  menus retain diagnostics and direct package access. Agent preparation, internal
  diagnostic implementations and instruction diagnostics remain available. Existing
  launch/connection, persistence/navigation and owned-process checks passed.
- Existing bounded UTF-8 file-reader/editor and transport tests were included:
  malformed/binary data, unsafe links and changed sources, conflicts and ownership/
  cancellation/timeout boundaries. Mock processes intercept destruction; real child
  fixtures are created and closed by their own test handles. User sessions are not
  used as fixtures or signalled.
- **33 Python storage tests**: exact UTF-8/BOM/CRLF/empty files, create/replace/history/
  restoration, actual CODEX_HOME/overrides, concurrent/external conflicts, write
  failures and unsafe links, shared/project import boundaries, cancelled previews,
  bilingual storage deduplication, clean initialization/user-text retention and
  auth-free diagnostics. These use task-private filesystem fixtures.
- Modern compatible Lint 7.4.2 runner across all five modules: **0 errors /
  226 warnings**, no infrastructure/rule-loading failures.
  The inline bilingual template-language label adds one SetTextI18n warning versus
  test12; both label variants are explicitly provided and tested. Existing warning
  categories and compileSDK35/AGP-tested33 limitation remain; no broad suppression.
  Legacy APK-toolchain Lint registry/Kotlin diagnostics are retained in build logs
  and are not presented as successful static analysis; the separate modern gate passed.
- Localization consistency: 584 workspace resource strings, settings keys and format
  arguments; new inline bilingual UI paths additionally exercised above. Final source
  matches both APK and modern-Lint stages (**776 source files**).
- Code/logical review: import reads only an explicitly picked source, stages no new
  authorization/configuration, writes only the backend-resolved destination after
  confirmation and retains revision checks/history. Project import fixes the target
  to AGENTS.md; shared import follows active global source. Existing agent startup,
  socket/connection, store schema and X11 execution paths are unchanged.
- Project AGENTS translation/consolidation: all 44 root and 39 checkout source blocks
  were mapped and reviewed, retaining technical literals, commands, links and numeric
  requirements. The sole complete Russian policy is now the active checkout file,
  including both X11 regression and clean-install transaction requirements. The short
  outer file explicitly directs reading that exact path. Actual Git roots/readability
  and both entry paths were checked; documentation links were updated. A Markdown link
  is an explicit read directive, not an automatic textual include or proof that every
  future model obeys it. Public GitHub release notes remain English.

Intermediate failed runs are retained in `.verification/agent-instructions-test13/`.
The layout failures led to bounded/fixed controls and a compact header. Later fixture
issues (queued dialog callbacks, global locale leakage and an overly long temporary
socket path in a standalone run) were corrected without weakening production guards;
the final full selected run above passed with the normal project-local short TMPDIR.

## Reused / Переиспользовано

Existing repository-local SDK/JDK/Gradle/dependency caches, bootstrap and verified
JNI/X11 outputs are reused. The task-start inventory compares 951
production/test/script inputs, including untracked feature files. The only changed
existing inputs are version metadata, the instruction UI/local preview helpers,
read-only Python view metadata, session-menu availability and their tests. Source
identity and final APK payload equality are recorded in reuse-final.json.

Historical real Codex0.163/0.164 new/next/resumed-process instruction-delivery proof
remains test12 evidence. Source-resolution, initialization, save/history/lock and
agent-connection functions were compared unchanged; only Python view metadata changed.
No new real CLI/model request or GUI/performance result is claimed for test13.

## Not checked / Не проверено

On the phone: actual small-screen/large-font viewport, keyboard, scrolling/rotation,
built-in environment/phone picker and Android storage permission, selecting real
UTF-8 files, history restoration and persistence after app restart. Confirm no live
chat restart and the installed CLI/custom connection behavior. Imported/source text
and history can be private; the feature does not export or translate them. Detection
of an already present standard storage section uses either canonical RU/EN heading;
it does not rewrite/delete existing manual duplicates or infer arbitrary paraphrases.

X11/driver/desktop launches, environment/package installation and unrelated server/
network checks were outside this UI/import change and were not run. No new SDK,
rootfs or model copy was created. Native buffer/phone GUI timing is not established
by these JVM/fixture checks. No public release is part of this delivery.

Evidence: `.verification/agent-instructions-test13/` (plan, JVM/Python/Lint logs,
source inventories, translation/consolidation mapping, final artifact/runtime audit
and Download read-back). Historical test12 proof is retained separately.

AI development assistance: OpenAI Codex
