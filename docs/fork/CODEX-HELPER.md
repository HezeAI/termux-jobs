> Historical private test17 experiment. Development was stopped on 2026-11-02.
> The helper is excluded from the next public release. This document describes
> the archived experiment, not the active source or a future APK.
> See [separation and preserved fixes](HELPER-EXPERIMENT-PAUSED.md).

# Codex helper — private test17

The helper is optional and initially available only for Codex in Ubuntu / Ubuntu AI.
Session/preset settings contain **Add helper** with a separately saved, explicit
link. No existing conversation is selected automatically.

## Selecting and using a helper

- **New helper:** select a saved Codex preset. The first explicit Open helper or
  message review creates one separate saved helper session from that preset, with
  its own thread and run. Changing the working conversation creates a fresh helper
  for this mode. Selecting a preset alone sends no model request.
- **Existing helper:** explicitly select an application session or a saved native
  conversation. An active session uses its current control channel. A stopped
  session requires a specific saved thread; there is no empty-conversation fallback.
  Native history metadata/cwd verification and the existing launch preparation still
  apply. Another active owner or an unknown active thread blocks duplicate launch.
- A helper cannot be its own worker or serve two working chats concurrently.
  Selecting another working conversation requires an explicit existing-helper choice.
  Model access, sandbox/approvals and the connection launcher are not broadened.

**Open helper** shows the same native history in a compact dialog, with earlier
message loading, draft input, Send, Minimize, Open session and a separate scoped
Stop confirmation. Viewing/minimizing does not send model requests or interrupt
agents. Pending questions/approvals are answered by opening the helper's ordinary
session; they are never approved automatically by the compact window.

Personal questions stay in this helper history. While it is busy, the compact
composer retains a new personal message and asks the user to wait. A personal
answer can be explicitly selected with **Send to working Codex…**, after preview
and confirmation; no helper model request is made for that forwarding action.

## Review and delivery

The **Helper** button below a working Codex message freezes the working run/thread,
source message and bounded context. When supported, the original task preview is read through native metadata
without loading an entire early turn and its potentially large images. The prompt
labels that preview as potentially incomplete. Recent user clarifications and related loaded results are
included within the transport budget. Files/project logs are not swept or attached.
Missing/truncated context is stated in the prompt rather than assumed complete.

One durable review ID binds the helper admission ID/turn, source message and
working conversation. Busy helpers are queued in the application; reviews never
steer into an existing personal/helper request. Only a successful completed turn's
`final_answer` agent message can be forwarded automatically. Commentary, incomplete,
failed, interrupted, mixed or unavailable results are not forwarded. Personal
messages cannot steer into an app-managed review through this controller.

The working CLI receives an explicit advisory envelope, not user authorization.
The UI labels it **Helper**, links its source and distinguishes sending, application
queue, CLI admission, rejection and unconfirmed delivery. Native `turn/start` or
`turn/steer` acknowledgement establishes admission only, not model uptake.

No response starts the next review automatically. Duplicate active clicks share
one record. Unconfirmed admissions are retained without blind replay; check native
history before asking for a new review. Switching the visible chat cannot redirect
a request. The coordinator does local RPC snapshots only while pending work exists;
normal idle viewing uses the existing transcript snapshot path, not model polling.

Disabling/changing the link invalidates pending reviews and prevents subsequent
unsent automatic forwarding. It does not delete history or undo delivery already
submitted to the CLI. Re-enabling does not replay old work. After an application
process restart, pending jobs become cancelled or unknown and require an explicit
new action; there is no durable background-service/exactly-once-delivery promise.

## Existing running adapters

The running test16 adapter can still be used for personal conversation but lacks
the exact-turn final-result capability. Automatic review shows a reason and an
explicit **Restart only the helper…** action. A second confirmation uses the
existing owned-session restart mechanism with the same native thread ID. The
working chat is not restarted. Old reviews are invalidated; press Helper again
for a fresh review after the upgraded channel is ready.

## Storage and rollback

`files/codex-helper-v1.json` is an independently atomic private sidecar for links,
review IDs, bounded context/results and delivery states. It contains no transferred
authorization. Existing `workspace-v1.json` schema8/session/preset serialization
stays unchanged. Helper sessions themselves are ordinary saved Codex profiles.
The ledger currently admits at most256 records and fails visibly at its local
limit; no automatic history deletion is added. Existing backup/export formats are
not expanded to include the new helper-link ledger in this build.

Reserve test16/1451 ignores the sidecar and retains ordinary saved helper sessions
and their native histories. It does not restore data to an earlier time. After
using the reserve, subsequent APKs must use a versionCode above1451.

## По-русски

«Добавить помощника» доступно в настройках сессии/пресета Codex. Новый помощник
создаёт отдельный сохраняемый диалог по пресету; существующий выбирается только
явно и сохраняет ID/историю. Связь сохраняется отдельной кнопкой. Свернуть окно —
не остановить агента. У помощника своя остановка и свои права.

Кнопка под сообщением создаёт одно обращение. Занятый помощник получает его в
очереди приложения; в рабочий чат пересылается только окончательный успешный
ответ на это обращение. Личная переписка автоматически не пересылается. Ручная
отправка выбранного ответа требует отдельного подтверждения.

Отключение связи отменяет ожидающие обращения, история остаётся. После повторного
включения или восстановления приложения старые обращения сами не отправляются.
При старом адаптере предлагается явный перезапуск только помощника с прежним ID.
Принятие сообщения CLI не означает, что модель уже учла замечание.

See [validation and device limits](VALIDATION-test17-codex-helper.md).
