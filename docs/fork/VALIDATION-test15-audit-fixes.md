# Test15 audit fixes and test14 reserve — 2026-11-01

Private test15 implementation and verification. Both final APKs verified and available in Download; artifact results below.

## Reserved version numbers

Existing APK inventory: 23 local Termux APKs, max1446. Corrected test15:1447; reserve based on test14:1448; subsequent builds must use1449 or higher. Same com.termux, certificate, ARM64 and schema8; no data rollback/migration mechanism is added.

## Verified reserve APK

Termux-Jobs-test14-reserve-1448.apk is repacked from the delivered test14 SHA25644fb5b1db1cbad3e8f691be2362756ab1667351399e2588be6c3e814b3faab0e. Only binary manifest versionCode and signing/ZIP metadata change. versionName remains2.1.1-test14. All1103 other non-signature entries, including all DEX/resources/assets/native payloads, are byte-identical. The Android package version is1448; embedded BuildConfig remains test14/1446 by design to preserve all executable content. Future packages must exceed1448. This is an application-binary reserve, not a snapshot of changing user data. No data deletion or migration is performed.

Certificate SHA256: dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644.
Reserve SHA256: f4d82df8dfa8b675088f3e74f870528eec8377d16014fed02a63336fed25ed97. ZIP integrity, package/version, non-debug, ARM64, v2 signature and unchanged payload passed. No APK installed or published.

Evidence: [.verification/audit-fixes-test15](../../.verification/audit-fixes-test15/), reserve-artifact.json, reserve-signature.txt, reserve-badging.txt and versions.json.

## Scope

F01, confirm F02 before changing, F03/F04, U01, F05 wording only, F06 decoder fallback with O03 deferred after ownership review. U02 remains intentional. Saved documentation/WebView/history/scroll/fast return remain. No other audit optimization or assistant feature.

## O03: deferred after operation-ownership review

No lifecycle/retention change ships for O03. The preliminary picker-list removal was reverted before testing or building. Retained documentation tabs are intentional. `WorkspaceDocumentationUi` keeps `READERS`/`Reader` (WebView) and `PAGES`/`Page` (URL, back-forward state, scroll); reopening uses those objects (lines 20–35, 127–150).

| Operation | Owner after hiding/selection | Return / end | Evidence and limits |
| --- | --- | --- | --- |
| Server upload | Controller → WorkspaceServers → WorkspaceServerUploads → worker task, document snapshots, source stream, SFTP/connection; ServerTransferPresentation retains progress window | Background calls `window.hide()`, not dismiss/cancel. Transfer card → foreground; repeated pick while active → foreground. Explicit Stop/disconnect/owner close cancels transport. Completion finishes presentation. | WorkspaceServers.java:40,57–75; WorkspaceServerUploads.java:16–45,52–65; ServerTransferPresentation.java:20–23. Code traced, no live server operation. |
| Server download | WorkspaceServers → WorkspaceServerDownloads → worker, destination, connection/channel; destination profile/path copied independently from picker | Same hide/foreground card/repeated download; Stop closes destination/connection; completion finishes presentation | WorkspaceServerDownloads.java:13–52,69–95. No network side effects used. |
| Local/SSH browser copy or move | WorkspaceFileBrowser → worker → TransferBackend/ProjectFiles. During transfer ordinary dismiss is disabled. | Completion refreshes directory and enables closing; no background-return feature added or removed | WorkspaceFileBrowser.java:130–131,186–199. Forced owner close uses shutdownNow, not proof that worker/remote operation has terminated. |
| Local/SSH editor write | Browser or WorkspaceServers → WorkspaceTextEditor plus shared ProjectFiles/worker; save task captures editor and snapshot | Editor rejects close while saving; server panel owns editors separately. Browser forced close closes editors/resources | WorkspaceTextEditor.java:35–46; WorkspaceServers.java:564–566,582. Worker completion and forced parent close remain a lifecycle risk requiring fixtures before any release change. |
| Browser export/download/share | Browser worker → ProjectFiles; export/save checks closed; successful share intentionally retains URI cache for recipient | No hidden-progress panel here; close marks closed and interrupts worker. External URI read must outlive share dialog | WorkspaceFileBrowser.java:212–240. No files exported in working environment. |
| Picker after selection | Consumer receives profile/entry/path; server upload creates independent document snapshots, download captures destination; picker browser itself closes | New selection creates a new browser. Existing list retains closed browsers until cancelSelection/owner close | WorkspaceLocalFilePicker.java:35–73. closed callback runs before worker termination; no post-quiescence ownership proof for every path. |

The distinction between reusable hidden server progress and disposable picker windows is established in code. It is not yet established that every browser close callback means its worker/write/export is quiescent. Per user instruction, O03 is postponed rather than changing retention on that incomplete basis. Background transfers, cancellation, server panel fast return and documentation state remain unchanged. Phone and live-network lifecycle checks have not been performed.

## Реализованные исправления

| Пункт | Подтверждение и изменение | Ограничение |
| --- | --- | --- |
| F01 | Persistence per-operation success/error callbacks; command, note, alias, session/preset forms wait for disk ACK. Real failing AtomicFile fixture confirms retained draft, error and retry without duplicate new rows. IO stays on the worker. | Models may already be edited in memory before disk failure. Alias may already be applied to the environment; environment + library is not a new distributed transaction. Existing formats are unchanged. |
| F02 | BEFORE-fix real editor fixture with a fake running session reproduced next-launch rights being presented as active. Bridge now supplies confirmed active cwd/access/approval; panel separates them from next launch. Unknown legacy launch fields remain unknown. | No actual working process is restarted; no fake native metadata is inferred from saved settings. |
| F03/F04 | Native delivery metadata preserved through controls; composer/attachments clear only on admission. Cached ambiguous Codex retry states no new send. Adapter tests confirm single native RPC/Shell invocation under repeated ID. | Transport ACK is not proof of model uptake. Ambiguous ID stays reserved, no automatic replay. |
| U01 | Dirty instruction Back/outside protected, explicit discard retained, saving blocks close. | Explicit Cancel intentionally discards. Real keyboard-first Back, touch and window dismissal need phone acceptance. |
| F05 | Basic-settings wording lists actual scope and exclusions. | No change to archive content, format, settings allowlist or restore mechanism. |
| F06 | Shared MIME recognition for BMP/HEIC/HEIF/AVIF and existing images; unavailable decoder retains original file with an explanation and export/share fallback. | Robolectric's permissive decoder requires an explicit missing-decoder shadow. No claim that every Android device decodes these formats. Existing image/attachment size limits retain their separate purposes. |

## Сообщения во время работы

All rows below describe admission evidence, not evidence that the model already incorporated the text. Four adapters retain their native protocols.

| Agent | Active message destination / receipt | Composer and failure | Tests / limits |
| --- | --- | --- | --- |
| Codex | `turn/steer` with expectedTurnId, native RPC result → current; idle `turn/start` → started. | Clear on `ok && accepted`; rejected and unconfirmed retain text/files. Cached ambiguous retry has an explicit no-resend explanation. | Fake native RPC, exact turn identity, stale session, dedup and Shell once. No live authenticated cloud steering in this task. |
| Claude2.1.296 | Native stream user input → `command_lifecycle` keyed by command_uuid; queued or started. During work the proven path is native queue, not immediate steer. Native may coalesce queued inputs. | Only lifecycle/replay admission clears. Native refusal, timeout or worker loss retains draft; same-ID reconnect retrieves cached result. Queued cancellation updates receipt/state. | Actual installed CLI, byte-identical cached binary, loopback synthetic provider: response + tool busy, multiple follow-ups order, completion race, queue cancellation, disconnected socket, history resume, permissions and unrelated-process survival. |
| Gemini0.64 pinned bundles | Instrumented native submit returns actual steering/current, native queue/queued, started or weaker submitted. | Clear only confirmed callback admission; keep text/files on rejection/error. | All3 supported hash-pinned bundle injections and syntax, production callback extraction with fake state. No cloud request, installed app UI or new CLI version inferred. |
| OpenCode | HTTP prompt admission → queued; preserve native delivery metadata, no promise that a native step-boundary steer is immediate model uptake. | Clear after native endpoint admission; unconfirmed/failed keeps draft; stable-ID duplicate path retained. | Local fake endpoint, native event/recovery fixtures; no live remote session/network-side effect. |

U02 remains intentional: admitted agent text is recorded in Recent commands. Existing recent entries are not removed.

## Сердце и остановка

Existing frame clock, average60Hz ceiling on faster displays, no background task/poll/persistent per-frame writes. Same layout/hit area. Single/double/triple beat groups, stronger pulse during five interior bends; existing external border path returns around the button to the right. System-disabled animation gets a static cue. Drawing timing/contrast/visible bends are phone checks, not an asserted measured FPS improvement.

One confirmation per selected agent. Yes is left/negative, No right/positive; No/Back/outside do not stop. Captured run/owner/session/request token is checked in Android and adapter. Completion/chat switch closes stale dialog; adapters reject replaced tokens before calling the existing native stop. Codex sends exact turnId; Claude uses its original control interrupt plus supported queued-cancel receipt; Gemini invokes native cancel; OpenCode retains its native interrupt/shell-ownership checks. Legacy helpers lacking a current token explain safe-stop unavailability rather than guessing. No process-group ownership/termination algorithm was replaced.

Synthetic all4 tests block real spawn/signal APIs. Actual owned Claude lifecycle tests separately establish unrelated process survival. Third-party native control APIs retain their own interruption semantics; a transport acknowledgement is displayed separately from observed idle/completed state. Native internal scheduling after dispatch is not presented as an Android atomic cancellation guarantee.

## Штатные вопросы Codex в обычном режиме

Verified actual CLI0.164.0, generated installed protocol schemas, isolated CODEX_HOME and loopback synthetic Responses provider. No real account secrets or cloud model used. No collaborationMode/Plan parameter. `CodexSessionShell` probes supported feature key then passes `--enable default_mode_request_user_input` on the existing launch command; user config is not rewritten by application code. Old running helpers continue unchanged until explicit restart with conversation resume. Support is version-specific and experimental.

Native negative control without flag: `request_user_input is unavailable in Default mode`. With flag, optionB, custom text and two questions all produce `item/tool/requestUserInput`, native request response, `serverRequest/resolved`, exact answer in next function_call_output, and the identical started/completed turnId. No ordinary message/turn-steer substitutes for form answer. Actual current CLI requires nonempty options for each question; each has one answer, free text allowed. No multiselect is invented from the native array shape.

UI starts with no selected default, allows custom text, sends explicitly, validates all questions, and keeps drafts across tab switches. Responses use the existing read executor, independent of a pending Send waiting for that response. Native ID + thread + turn + nonce reject stale replies; cached same-answer retries never duplicate ambiguous native writes. CLI question resolution disables repeat sending and adds an answered-question receipt in the existing bounded receipt store. Receipt states resolution after response; `serverRequest/resolved` can also mean a cleared question, so it alone is not a model-uptake guarantee. Pending question closed/completed/foreign session is not redirected into another form.

Two test discoveries retained in failed logs: first config comparison saw the CLI itself add a standard project-trust entry; fixtures then seeded their own known trust before comparing bytes. The launch feature itself is not persisted. A free-form-only second question was rejected by current CLI for missing options; the multiple-question fixture now follows its real options contract, with custom text still tested. These are not reported as successful unsupported modes.

Official references: [OpenAI app-server](https://learn.chatgpt.com/docs/app-server), [Claude CLI](https://code.claude.com/docs/en/cli-reference), [Claude Agent SDK types](https://code.claude.com/docs/en/agent-sdk/typescript). Installed binaries and native fixtures establish the specific behavior claimed here.

## Выделение вкладок инструкций

General/Project/Diagnostics and the edited AGENTS.md/AGENTS.override.md keep pressed-style accent outline/fill/bold accent text. Selected accessibility state is exposed. RU/EN + enlarged font fixtures cover switch/reopen (General default), while the separate actual Codex instruction-source label stays unchanged. This does not change which instructions Codex reads or when instructions apply.

## Доказательства и охват

Evidence is in `.verification/audit-fixes-test15`, compact XML/JSON/logs, no copied SDK/rootfs. Baseline inventory includes dirty and new sources; task uses the existing checkout at `58fbdb411e6a2bd8fbfd4d86700d3dcc05a7a3fe`. The delivered test14 and new test15 are distinct artifacts. Existing repository changes preceding this task remain preserved.

- **Проверено заново:** targeted Java/UI/storage/error tests, native and fake CLI adapter tests, instruction backend33 tests, RU/EN584 resource checks, syntax, selected-source/security/lifecycle review. Full modern Lint selected because persistence and transports are shared. Exact counts, reports and final files follow below.
- **Переиспользовано:** existing toolchain/cache and verified native/bootstrap payloads. Current X11/native inputs equal task baseline; JNI final payload must equal test14 and pinned hashes. This carries payload identity, not a newly measured graphics result or unproven compiler reproduction. No graphics/environment installation behaviour changed, so no guest or desktop test is claimed from an old log.
- **Не проверено:** actual phone editing/IME, TalkBack, large-font window layout, pulse/line contrast and smoothness, decoder availability, authenticated cloud agents/network outages, APK update/rollback acceptance and long-running use. No real X11 desktop/install/update/cleanup or live file-server operation was run for this task. O03 remains deferred.

Known early test failures are retained separately, not counted as PASS. Final successful results must refer to final changed inputs; the last small question-return callback change requires its own AgentChatUi rerun.

## Финальные результаты — 2026-11-01

- **Проверено заново:** 271 tests / 37 classes, zero failures/errors/skips (`final-jvm.log`, `final-jvm-results`, `jvm-summary.json`). After the final question-return callback change, all46 AgentChatUi tests rerun successfully (`final-return-test.log`, `final-return-ui-results.xml`). These46 are a repeat subset, not46 additional unique tests.
- **Проверено заново:** 33 instruction backend tests, 584 localization resource keys and format checks; 11 final Node suites (`node-verification-summary.json`). Earlier failing assertions are preserved; final Claude unit correction expects structured refusals and verifies no extra input write. Actual CLI protocol results: `codex-questions-native-result.json` (negative control + option + custom + multiple questions), `claude-active-native-final-result.json` and binary hashes in `native-cli-identity.json`.
- **Проверено заново:** full modern Lint all5 modules, AGP7.4.2/Gradle7.5.1, no errors or infrastructure diagnostics,232 warnings. Previous report had230; two new ObsoleteSdkInt warnings concern conservative pre26 animation guards, unreachable at this APK's minSdk26. Other categories retain known existing warnings, not a clean zero-warning claim. compileSdk35 exceeds AGP's tested compileSdk range; recorded tool limitation. See `modern-lint-summary.json`, `final-lint.xml`, `final-lint.html`, `lint-new-findings.json`.
- **Проверено заново:** package/signature/CRC/SHA/runtime/private-pattern audit for both final files. test15 verifies99 source assets and X11 runtime assets; all three JNI libraries equal test14 byte-for-byte. DEX inventory: test15 has14043 definitions, reserve14018; both have31 reviewed optional unresolved types, no missing required or unreviewed types. This is type inventory, not proof of all runtime linkage/device behavior. `artifact.json`, `runtime.json`, `reserve-runtime.json`, `apk-signature.txt`, `reserve-final-signature.txt`, `final-delivery.json`.
- **Переиспользовано:** existing local SDK/JDK/Gradle/dependency caches, verified prebuilt Termux JNI and bootstrap. Equality evidence: `native-reuse.json`, `reuse-source-identity.json`, `packaged-stage-source-identity.json`, packaged native comparison. No compiler-rebuild equivalence or newly measured graphics performance is claimed. All app/graphics/native source trees remain in their existing locations.
- **Не проверено:** phone scenarios listed above. No APK installation/publication, real environment update/install/cleanup, working session stop or data deletion performed. O03 remains deferred and U02 intentionally unchanged.

| File in Download | Android version | Bytes | SHA-256 |
| --- | --- | --- | --- |
| Termux-Jobs-test15.apk | 2.1.1-test15 / 1447 | 39952780 | d42c7b9a01c6ce332ecbb7c5899e30b0f1655a93062cee51c57e25d1a4cc90c0 |
| Termux-Jobs-test14-reserve-1448.apk | 2.1.1-test14 / 1448 | 39942145 | f4d82df8dfa8b675088f3e74f870528eec8377d16014fed02a63336fed25ed97 |

Both com.termux, non-debug, ARM64, same certificate SHA256dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644. Final Download readback hashes match. Fixed APK verifies v2; reserve verifies v2+v3. Archive of test14 reserve differs only by one manifest versionCode byte at8584 plus signatures/ZIP metadata; all1103 non-manifest/non-signature entries unchanged, including non-signature META-INF metadata. Reserve retains the embedded test14 BuildConfig1446 and does not undo user-data changes. Future codes must be1449 or greater.

Code review covered asynchronous disk acknowledgements/draft failures, immutable active settings versus saved settings, admission/retry dedup, native question ownership and settlement after tab switches, scoped stop confirmation and backend guard, rights/approval preservation, decoder fallback, UI-thread IO and animation allocations. No additional dependencies, permissions, data schema, automatic restarts or new process-owner mechanism added. The existing bounded receipt store accepts a new optional answered outcome; test14 ignores that unfamiliar outcome rather than changing native history.

The final source inventory includes uncommitted/new files; task-source-delta records the changes relative to the starting inventory. Existing retained-browser/transfer/data-owner source identities remain unchanged. Only disposable task fixtures and compact reports were added; no full checkout, SDK, rootfs or model copy was created.
