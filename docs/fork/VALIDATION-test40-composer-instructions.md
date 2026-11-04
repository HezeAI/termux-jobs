# test40 — vertical composer and project-file action

Date: 2026-11-03. Final APK: `/sdcard/Download/Termux-Jobs-test40.apk`.

| Property | Verified value |
| --- | --- |
| Package | `com.termux` |
| Version | `2.1.1-test40` / **1475** |
| Bytes | **40,436,820** |
| SHA-256 | `f2c37a4d03a3b85a8792b26256a57efd3c74752fe111dda46ddbaf5a79cccf46` |
| Certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| ABI / debug / signature | ARM64 ELF64 AArch64 / nondebuggable / v2 verified |

Sidecar: `/sdcard/Download/Termux-Jobs-test40.apk.sha256`, independently checked
from Download. Issued/reserve APK identities were inspected;1475 exceeds their
codes. Package and signer match test39. No storage migration or automatic session
restart is introduced. No publication is part of this delivery.

## Result / Результат

RU: Голосовой ввод полностью удалён: кнопка, меню удержания, системное окно,
обработчики, диагностика, разрешение записи и объявления speech/microphone.
Других потребителей записи аудио в коде приложения не обнаружено. X11 playback
не изменён. Слева от поля сообщения скрепка расположена над звездой в общей
обводке, с отдельными48dp областями и ripple. Существующий EditText, черновик,
выделение и действия вложений/звезды сохраняются. Отправка остаётся справа внизу.
Обычный терминал восстанавливает свою компоновку. Поведение сообщений с `/`
не изменено. Исправления истории Claude/Codex из test39 сохранены.

EN: Voice input and its Android declarations are removed. Agent chat uses a
vertical paperclip-above-star stack with independent48dp targets, shared outline
and visible press feedback. The existing input, draft, selection and actions are
retained; Send stays at the bottom right. Ordinary terminals keep their layout.
Slash-message behavior remains unchanged; test39 history fixes remain included.

## Confirmed project-file finding

The developer's latest video showed an existing `/root/termux-fork/AGENTS.md`
already displayed by Codex and a missing `CLAUDE.md` for Claude. The existing-file
branch never constructed a creation dialog: it focused the current editor and
added a status row that changed the parent's size. A read-only invocation of the
real Python handler reproduced both file states. The developer accepted this
explanation. The existing AGENTS.md SHA remained unchanged.

The action now says Edit AGENTS.md / Edit CLAUDE.md when the base exists,
including when a local/override file is selected. It focuses the existing base
editor and keyboard without adding that status row. A missing base keeps Create
project instructions; loading uses a neutral Open project instructions caption.
No artificial delay or separate diagnostics feature is added.

Creation keeps its captured destination/revision, single child form and queued
loading action. Parent refresh retains form text; unexpected child dismissal
retains its draft within the editor. Closing a dirty parent follows the same
confirmation guard as Back. Preview/opening does not write files.

## Проверено заново / Checked anew

- Release compilation/packaging and **221** selected JVM/UI tests passed without
  failures/errors/skips. Includes68instruction UI cases; common chat/history,
  attachments, keyboard/navigation, manager layout and all-four-agent composer
  routes. The geometry fixture covers RU/EN, widths240/280/360, font sizes14/24/36,
  one/multiline drafts,48dp vertical targets, bottom Send, retained selection and
  independent actions. These are model checks, not phone/IME acceptance.
- **56** Codex/Claude Python instruction tests passed, including base-file presence
  independent of the selected edit target, safe writes, conflicts and history.
- Claude bounded-history fixture passed: >8000records, >500messages,680tool/result
  pairs,520parts in one response, full Unicode answer,69+MiB native JSONL, cursor
  replay/reset, append/partial tail, restart and read-only checksums.
- Localization check passed:584workspace strings, app/shared keys and format args.
- Full modern **Lint7.4.2** analyzed all five modules and dependencies: **0errors,
  234warnings**, no rule-loading/infrastructure failures. Existing compileSDK35
  beyond AGP's tested range warning remains. Legacy build-Lint Kotlin diagnostics
  remain in the packaging log; they are not reported as a successful Lint gate.
- Final Download APK: ZIP integrity, identity, increasing code, nondebuggable
  status, ABI, certificate/v2 signature, SHA and byte equality with build output.
  Runtime audit:14173defined classes, no required/unreviewed missing types.
  All118assets match current source/pinned inputs; all three native libraries
  match expected SHA and ELF64/AArch64. Compiled paperclip resolves to a vector.
  No voice/window-trace classes, recording permission, speech queries, microphone
  feature/vector or former speech diagnostic markers remain. Checked unwanted
  keystore/pycache/task files are absent.
- Source/security/logical review: original-session binding and revision guards
  retained; no new command execution, permission, model request, session stop,
  environment installation or instruction replacement path added.

Earlier intermediate compile errors and an obsolete dirty-close test assertion
were corrected before the final221test run. Original logs remain in
`.verification/composer-no-voice/`; no failed intermediate run is counted as PASS.

## Переиспользовано / Reused

Native payloads from signed test39 were reused only after all95recorded relevant
source/configuration input hashes matched, and their packaged SHA values were
checked again. `native-reuse.json` records inputs and baseline APK identity.
Cached SDK/JDK/Gradle dependencies were reused; Java/resources were restaged and
compiled. Previous native CLI/provider findings remain historical evidence in
[test39](VALIDATION-test39-history-instructions.md), not a fresh phone check.

## Не проверено / Not checked

Actual phone taps/IME, font accessibility and visual geometry of test40 await
installation by the developer. Test the clarified Edit/Create action from both
entry points and real Claude history paging/full text/scroll anchors. The prior
video establishes the old existing-file behavior, not acceptance of this APK.
Existing Claude helpers need an explicit idle restart with the saved conversation
to load test39's history adapter if not already done; nothing restarts automatically.
X11/installer/network integration was not executed: these paths were not changed.
Native JSONL per-record32MiB and unresolved cross-file-parent limits remain as
documented in test39. Existing environments, history files and user instructions
were not modified by these checks.

Evidence: `.verification/composer-no-voice-test40/` contains source identities,
commands/scope, build/test logs, JVM summary, Python/history/localization results,
Lint summary, final manifest/resources/signature/runtime and artifact audits.
Read-only video/backend comparison: `.verification/instruction-window-trace/`.


## Историческое уточнение порядка выпуска — 2026-11-04

Из RELEASING.md перенесён исторический контекст: после test39 разработчик
поручил полностью удалить голосовой ввод. В этом указании «следующая сборка»
относилась к test40; микрофон, распознавание, обработчики, диагностика и разрешение
были удалены. Прежнее ожидание проверки голосового ввода на телефоне не означало
разрешение вернуть функцию. Проверки истории Claude и открытия окна на телефоне
оставались отдельными, а публикация stable требовала отдельной авторизации.

Исходные результаты test39/test40, APK и их идентичности этой справкой не меняются.
Действующий запрет на возвращение функции хранится в начале
[порядка выпуска](RELEASING.md), а не определяется исторической формулировкой.
