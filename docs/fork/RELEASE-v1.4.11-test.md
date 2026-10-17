# v1.4.11-test / code1117

Short screenshot discovery now accepts greetings, polite wording, punctuation and simple follow-up analysis requests. Examples: “привет посмотри ласт скрин”, “можешь посмотреть последний скрин?”, “hello, please show latest 3 screenshots”. Explicit filenames/paths, questions about how to find files and negations are not rewritten into latest-file requests. The same selected environment, project boundary and read/tool permission checks still apply. Text-only models can locate files without gaining vision.

Tool-shaped local-model output is collapsed into a native request preview card; raw JSON is available in details. Streaming partial tool JSON shows preparing status. The preview is explicitly unconfirmed, distinct from actual running/completed/failed tool cards. Cached preview identity avoids rebuilding the card for unchanged JSON on every text update. User messages, raw history and non-tool JSON remain available unchanged. Previous mixed outputs are presented with the same renderer when history is reopened.

Mixed prose plus a tool request is not executed as code. For an action request, the coordinator can request one bounded formatting correction, after which only a standalone schema-validated tool request may execute through normal authorization/approval. A quoted example in an ordinary question does not trigger this repair path. No automatic retry loop or automatic elevation is added.

Same com.termux signing identity, versionCode 1117, optional schema unchanged. No data clear or permission migration. This is a private test build, no GitHub publication. Device tests pending for streamed card appearance, focus/scroll and actual model compliance; automated UI tests do not prove those device interactions.

English short forms include last/latest screenshot, please show the last screenshot, look at/check/view and the single-word screenshot. Final incomplete requests are explicitly failed previews with raw response details, not indefinitely preparing or successful cards. Generic translated-name file search remains deferred.
