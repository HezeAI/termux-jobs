# v1.4.23-test — local input composition

Each local inference pass reports token attribution after runtime context fitting: system instructions/tool definitions, prior conversation, tool requests/results, current user message, and formatting/boundary residual. llama.cpp uses the loaded model tokenizer for the rendered total. Components use leave-one-group-out differences, are approximate, and can have a negative formatting residual; they are not independent exact token slices. LiteRT uses an explicitly labelled estimate. Existing cache counts distinguish total input from recomputation.

No prompts, file contents or messages are added to diagnostics. Measurement tokenizes only, without generation or modifying inference cache. Normal inference still receives the original fitted messages. Reports retain each pass within existing bounds.

The session/preset button now reads simply Engine settings / Настройки движка; its two CPU controls remain unchanged.

Compatible update: package com.termux, versionCode1129, existing certificate. No schema changes or data reset. Real model/device verification pending developer test. Not published to GitHub.
