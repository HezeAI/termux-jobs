# v1.5.12-test — Gemma native tool recovery

Private local-AI test build. No GitHub publication.

An incomplete Gemma native call (`<|tool_call>call:...`) could previously be displayed as an ordinary answer. The turn coordinator did not recognize that syntax, and its action heuristic missed the reported Russian request to write a guide and put it in Downloads. Source-level reproduction confirmed one generation, no tool execution, and unchanged raw output.

Changes:
- Classify leaked leading Gemma calls as `unparsed_native_tool`, distinct from ordinary SDK completion. This does not assert token exhaustion.
- Hide raw leading native-call drafts behind the existing pending/incomplete presentation, retaining raw detail in the diagnostic card.
- For action requests, route unparsed native calls through at most two validation retries. Never parse, complete, execute or infer missing arguments from the fragment.
- Recover with concise structured SDK calls. Errors identify the function for diagnostics; permission and approval checks remain mandatory.
- Native repairs send ordinary application validation feedback. Invalid fragments are not replayed as assistant history or fake tool responses in the same turn.
- In LiteRT Native, plain tool JSON without SDK provenance is rejected instead of executed. Only real structured SDK calls or existing internal coordinator actions can proceed.
- Recognize write/save/copy/move/edit wording in the existing action heuristic. This heuristic is not authorization. Tool-call examples, no-action requests and chat-only mode do not trigger recovery execution for leaked Gemma output.

No weight, schema, permission, configured reply limit, sampler or thinking defaults change. The maximum remains the user's setting, including 256 when selected. The original reason for incomplete model output remains unproven; recovery is bounded, not a guarantee of model obedience.

Package/signing identity retained; private versionCode1149. No data migration/reset. No external-agent or installed-environment changes. Device validation is required using the original short request in a new chat, plus an ordinary read/write follow-up.
