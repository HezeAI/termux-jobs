# Agent chat scale — v2.0.9

Codex, Claude, Gemini and OpenCode status panels contain **Chat scale**, a slider
from **50% to150%**, and a **100%** reset. The default matches the previous layout.
The setting is independent for each session, retained in session/preset copies
and workspace backups, and takes effect in the existing chat without restarting
the agent or sending a model/tool request.

Scaling changes measured Android layout sizes. Text, Markdown headings/code/tables,
tool/file cards, thumbnails, spacing, attachment drafts and the Shell/message
input rows occupy less or more space. Smaller values fit more conversation on
the screen. The app header/status controls, system keyboard and native terminal
retain their own sizing. The shared message-input row is restored on terminal
presentation, switching away from the chat, hiding the chat and controller close.

The viewport follows the same bottom/reading-anchor policy as before. Existing
rows, code expansion state, drafts, selection and callbacks are retained.
Streaming updates restore the affected row's baseline before applying its new
content, then reapply the selected scale; repeated updates do not multiply it.
New rows/request cards inherit the active value. Unchanged row trees are not
rebuilt on a slider change, and already-applied geometry is not reassigned on
unchanged polls. Input scaling has an explicit bounded container rather than
discovering or transforming an arbitrary parent/root view.

`Session.chatScale` is an optional integer in schema8, defaults to100 for old
libraries and accepts50–150. This is local presentation metadata, excluded from
native agent request snapshots. Slider persistence is debounced and flushed on
release/detach; stale status controls cannot change a different session. No
environment, terminal process, account, transport or desktop configuration changes.

See the current private validation report for automated evidence. Physical phone
checks of readability, touch behavior, font/DPI preferences, IME composition,
scroll anchoring and all four provider panels remain separate from JVM fixtures.
