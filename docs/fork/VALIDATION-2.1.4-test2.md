# 2.1.4-test2 / 1487 — private release APK validation

2026-11-05. Canonical checkout:
`/root/termux-fork/.verification/rollback-live-actions/source`.
Branch `work/test-file-dates-templates`. Last published stable baseline:
v2.1.3 / 1485 (`308c88955fd3b0691ded560c78ee3f945538381e`).
Private test1 / 1486 remains available and its APK is unchanged.

## Included behavior / Состав

- File modification time in both file windows, fresh metadata on refresh, retained
  selection/sorting; all four exact supplied common instruction assets from test1.
  [Original implementation checks](VALIDATION-2.1.4-test1.md). Existing saved user
  instructions, including empty files, are not migrated automatically.
- The **collapsed Codex chip is unchanged**. At the bottom of expanded status,
  current context uses only native `thread/tokenUsage/updated` → `last.totalTokens`
  and `modelContextWindow`. Lifetime `total` is ignored. Cached input tokens stay
  included. The bar is clamped visually, but the label retains the actual ratio.
  Missing/invalid counts show «Нет данных» / “No data”, never an invented zero.
- A bounded 8 KiB, mode-0600 context checkpoint stores thread/model/counts and
  operation state separately from native history. Cached counts are stale after
  reopen until a fresh CLI event; poll responses alone do not make them fresh.
  Actual model changes invalidate the old model's count/window. Pending selections
  mark the previous estimate stale. No effective auto-compaction threshold was
  reported by this protocol; none is inferred or displayed.
- «Сократить контекст» / “Compact context” uses native `thread/compact/start` after
  the exact warning: «Сократить рабочий контекст? История чата сохранится, но часть
  деталей может не попасть в краткое содержание». No manual session-file edits.
  It requires a connected idle run, no pending approvals/native terminal/settings
  operation, and confirmed protocol support (Codex 0.166.0). Other/unconfirmed
  helpers keep it disabled with an explanation. Server rejection is explicit.
- The start reply is only admission. “Compacting…” remains until a matching native
  contextCompaction item and completed turn, or a failure/unknown-outcome result.
  Duplicate clicks are rejected in both UI and adapter. A local timeout/disconnect
  never becomes success or an automatic replay. Reopened status retains operation
  state; matching native history can confirm a completion missed on disconnect.
  Otherwise a repeat remains blocked until the run's state is confirmed. A later
  successful ordinary turn can establish that this run is free while preserving
  the warning that the earlier compaction outcome was not confirmed.
- Confirmation/requests capture the original profile, run and thread. Switching
  selection cannot retarget them. Dismissal/detachment stops UI observation and
  worker activity, without sending cancellation to the native operation.
  Existing compaction messages, model controls, scrolling and native history stay.
- All **four structured chats** show `••• 00:37`, then `••• 1:02:15` for a request
  with a known native start. This is total elapsed work, including tools, approval
  waits and retries, not CPU/model-compute time. A process merely being alive or
  busy does not establish a start. Unknown starts have no invented duration.
  Native snapshots carry the existing request's ID/elapsedMs; timing adds no CLI
  or model queries. Backend `process.hrtime.bigint()` and Android elapsedRealtime
  are monotonic. UI anchors survive session/tab switches and hiding; a visible-only
  one-second tick works independently of animation frames. Dot geometry and fixed
  footer height remain unchanged; waiting/retry labels retain their behavior.

## Whole-request clock boundaries

| Agent | Native begin | End | Tool/continuation behavior | Evidence scope |
| --- | --- | --- | --- | --- |
| Codex | scoped turn/started | matching turn/completed or terminal error | command/file/tool events do not start new clocks; willRetry retains the clock | real CLI + adapter + guarded provider; native-event fixtures |
| Claude | tracked user command_lifecycle started | completed/cancelled/discarded/refused or root result | only admitted user UUIDs can start; Read/Write/Bash and nested tool/subagent results do not reset/stop it | SDK event fixtures; genuine event path traced in source, no new native CLI/phone run |
| Gemini | accepted native user prompt_id, before startNewPrompt | native final stream/error/cancel/stop handlers | isContinuation and auto-nudge keep the same clock; transient idle between tools is not a request boundary | bridge fixtures + transformed syntax for all three pinned 0.64.0 UI bundles; no new phone run |
| OpenCode | scoped session.execution.started | succeeded/failed/interrupted | shell/tool events cannot create/reset the user-query clock; duplicate lifecycle events are ignored | native-event fixtures and existing SSE path; no new native CLI/phone run |

Validated native Claude UserPromptSubmit/Stop hooks and explicitly spawned user
shell work can also carry known timing. Legacy/manual/uninstrumented routes with
no genuine start metadata remain unknown; chat tests do not establish every TUI
or manual CLI route. Ordinary terminal input is unchanged.

## Checked anew / Проверено заново

- JVM/UI: full run **244 cases in 23 suites**, zero failures/errors/skips. After
  adding the window-detach cleanup hook, **6 context UI cases** were rerun on the
  final class, replacing the previous 5; **245 distinct accepted cases** overall.
  The other 239 unchanged cases are explicitly reused from that same full run.
  `jvm-inputs.json` / `jvm-final-inputs.json` identify the two changed Java/test files.
- UI covers original-owner capture, cancel/double click, native running/completed/
  failed/unknown phases, foreign-thread refusal, stale/missing data, model-window
  refresh, RU/EN, detach cleanup, independent timer anchors, unknown starts,
  hour formatting, hidden windows and one-second scheduling. Prior file/template
  cases remain covered. These are Robolectric fixtures, not phone acceptance.
- **17 Node fixture suites**, including extended whole-request clock tests and
  existing completion summaries, Claude lifecycle, Gemini bridge/recovery/receipts,
  OpenCode recovery/declines and Codex send/history/attachment/login cases.
  Multiple Claude Read/Write/Bash calls and nested results keep one user timer;
  native final result/cancel ends it. Shell children do not create an OpenCode
  timer when the parent start was missed. Timing snapshots do not call transport.
- **57 Python instruction-storage tests** on artificial files passed. No skips
  are counted as passes. Four template ZIP/source/APK bytes match exactly below.
- Gemini 0.64.0: each of the three version-pinned UI bundles was SHA-256 checked,
  fully instrumented in memory and syntax-checked. Native user-prompt/continuation,
  final-answer, failure, cancellation and loop-decision markers are verified.
  Installed CLI files were not patched on disk and no agent rights were changed.
- Real Codex **0.166.0**, binary SHA-256
  `f2e6c980c937e734644bed1b4a9a018d3e63a0ccbe71aafc85c86a8c6e4b462a`,
  production adapter and Unix socket, artificial HOME/project, existing kernel
  network fence allowing only owned local fixture connections. No real auth.
  Native context: 1,010 / 258,400 before compaction, 13,504 / 258,400 after its
  native estimate, then 1,010 after the next synthetic turn. Synthetic provider
  counters are not realistic token accounting or proof that compaction reduces
  every request. Input 1,000 includes 600 cached tokens: displayed total stays
  1,010, not 410. Model gpt-6.1-sol → gpt-6-sol refreshes correctly.
  Reopen preserves the count with stale=true and does not call the provider.
  An operation held at the local provider exposes its real request ID/timing;
  stopping only its owned native process yields unknown completion and no replay.
  All recorded task-owned children exited. This is not an external-route or
  Android acceptance result, and does not close attachment/disconnection research.
- Full modern release Lint, all five modules requested: **0 errors, 239 warnings**,
  no infrastructure diagnostics. Unchanged dependency analyses may be up-to-date;
  a cached task is not described as freshly repeated analysis. Older packaging
  lint's AndroidX/Kotlin loading failures are not the analysis pass; the independent
  modern gate is. Compile SDK 35 exceeds the old AGP's tested range as documented.

## Final APK — independently checked after the last repackaging

- `Termux-Jobs-v2.1.4-test2-arm64.apk`, 40,494,652 bytes; `com.termux`.
- versionName `2.1.4-test2`, versionCode **1487**, ARM64 only, minSDK 26 / target 28 /
  compile 35, not debuggable. No profile/data/schema migration; schema stays 8.
- File SHA-256: `502da05a6e27a5fc1d4116d07801f5e16658ffcb4641159e6da09bc7c6f9fa80`.
- Certificate SHA-256 (not the file hash):
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
  Same signer/package as previous Termux Job’s; this does not establish update
  compatibility with other independently signed Termux distributions.
- Fresh ZIP integrity, v2 signature/certificate, identity/ABI/debug flags, required
  DEX runtime definitions, reviewed optional types, source/assets equality, notices
  and stale outer ZIP-entry checks passed on this exact file. All seven ARM64 ELF
  payloads equal stable v2.1.3 bytes, including the Fedora helper; the 95 recorded
  native source/config inputs are unchanged. Java/resources/adapters were rebuilt.
  verifyReleaseSigner and verifyX11Inputs remain enabled; no packaging bypass.
- Relative to stable, only 12 expected assets differ: the four common templates
  plus eight adapter/timing modules. Project template and native payloads are
  unchanged. No CLI binary fork, experimental context mode or automatic-compaction
  policy is added. Existing native CLI defaults remain unchanged.

## Exact four templates

| Asset | Bytes | SHA-256 |
| --- | ---: | --- |
| [agent-instructions-ubuntu-ai.md](../../app/src/main/assets/agent-instructions-ubuntu-ai.md) | 28861 | `f0ee1dabfdd25388231f5a5b7c483259dd2fa2f6d6049b8539245128081df6d6` |
| [agent-instructions-ubuntu-ai.en.md](../../app/src/main/assets/agent-instructions-ubuntu-ai.en.md) | 15499 | `98af576b97c8b71865539ed30c8166be8e189a650cb9e5aaee4e5958430beb0b` |
| [agent-instructions-claude.md](../../app/src/main/assets/agent-instructions-claude.md) | 28873 | `56a96b8c94ab11eeb397cbb338d078aee9917810f48162dc1f598f7bd75d36ea` |
| [agent-instructions-claude.en.md](../../app/src/main/assets/agent-instructions-claude.en.md) | 15511 | `42ef6e255c619315c196a013e0208f0d29e9969b507b07eabc28a7c5fae1794b` |

## Initial failures and reused evidence

The first context UI test caught that Session.copy creates new run/profile IDs.
The owner snapshot now explicitly preserves the original IDs. A cancellation test
also needed the queued Android dismiss callback to run before checking enabled
state. The final native timing probe initially looked for requestTiming in the
status response instead of the chat snapshot. The corrected probe verifies the
real UI-facing snapshot. Initial failures remain under initial-jvm/,
pre-owner-fix/ and native-context/; none is relabelled as success.

Native payload verification/reuse follows test1's source/config checks. File-window
and exact-template implementation is unchanged; the relevant JVM/Python checks
were repeated or explicitly reused as above. No fresh GPU or environment install
was needed. Source commit/branch and final APK identity are recorded after local
commits in `.verification/test-2.1.4-test2/source-apk-identity.json`.

Evidence in that local directory: build.log, jvm-summary.json, junit/,
context-final-jvm.log, python.log, per-suite Node logs, request-clock.log,
gemini-clock-hooks.log, native-context-result.json, native-context/network.jsonl,
lint-summary.json/lint-lint.xml, signature.txt, runtime.json, artifact.json,
template-identity.json, native-inputs.json, packaged-source-inputs.json, SHA256SUMS.

## Limits / Не проверено

No phone acceptance of test2, APK installation, working-session restart, push,
tag or GitHub publication was performed. Prior phone results keep their original
versions and sources in the [audit plan](SECURITY-AUDIT-STAGE2.md); they do not
become acceptance of these timers/controls. Android low-level key handoff/crash
recovery and device backup export/restore remain unverified by agreement.
Real Gemini API/Vertex login and all manual/TUI paths remain outside confirmed
coverage. MCP/plugins remain excluded and unverified.

Attachment accumulation/disconnections remain **unfixed**. Manual compaction can
omit details and is not a solution to unbounded cold replay. It is not automatic.
The [media investigation](MEDIA-CONTEXT-INVESTIGATION.md) stays paused; no working
history, original attachments or authorization was read/rewritten for these tests.
User README.md and RELEASING.md edits, public update.json, old APKs/tags and binary
Components are unchanged. No new library/component versions are introduced.

## Короткая проверка телефона — отдельные тестовые сессии

1. Проверьте даты в обоих файловых окнах и предпросмотр четырёх общих шаблонов.
   Перезапишите искусственный файл и обновите список. Рабочие инструкции не заменяйте.
2. В новом тестовом Codex откройте плашку: отдельно видны лимиты и контекст.
   До первого события допустимы «Нет данных»/пометка устаревания. Отправьте обычное
   сообщение; после ответа проверьте обновление. Смените модель и дождитесь её
   фактического применения в следующем запросе: старое окно не должно стать новым.
3. В свободном искусственном чате нажмите «Сократить контекст», сначала отмените.
   Затем подтвердите. До нативного завершения — «Сокращение…», повтор заблокирован;
   после — исходная история/сообщение о сокращении и новое значение CLI либо
   честная пометка устаревания. Не сокращайте рабочие чаты ради проверки.
4. У каждого из четырёх агентов попросите несколько действий с искусственными
   файлами в одном ответе. Во время подтверждения и инструментов таймер продолжается.
   Переключите вкладку/сессию и вернитесь: отсчёты независимы и не начинают с нуля.
   Завершите или штатно отмените: активный таймер должен исчезнуть. Если начало
   неизвестно, длительность скрыта. Аварийное завершение приложения не требуется.
5. При ошибке сообщите агент/модель, действие, видимый статус/время и приложите
   скриншот без ключей и приватной истории. Проверки телефона не засчитываются заранее.
