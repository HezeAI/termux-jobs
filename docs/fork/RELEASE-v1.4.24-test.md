# v1.4.24-test — smaller local model input

Local AI system instructions remove repetition while retaining current environment/rights/scope, all tool schemas and options, trust boundaries, confirmation rules and history lookup. Earlier-turn tool outputs over256 characters use a128-character head and48-character tail with an explicit excerpt/history/source marker (previous threshold1024, head384/tail96). Complete saved transcripts are unchanged; current-turn tool results are unchanged by this optimization. Existing context-fit limits still apply.

No new phrase routing, changed permissions, tool execution privileges or model settings. Token diagnostics remain available for before/after measurement. More compact excerpts can omit older detail: retrieve the original with history, or read again for current file content. Exact latency improvements require device measurement.

Saved local-AI preset menu order: Close session, Settings, Delete preset. Terminal-tab chat menus retain their existing order. Model/import cards use the same dark settings-button palette as the surrounding UI.

Compatible update: com.termux/versionCode1130/existing certificate. No schema change or data reset. Not published to GitHub.
