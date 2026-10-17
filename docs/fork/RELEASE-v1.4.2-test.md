# v1.4.2-test — local test build

Local model tool requests are now recognized in complete json/tool/unlabelled code
fences as well as plain JSON. Only structured tool objects with the required string
arguments are accepted; prose, executable code fences, trailing text/objects and
unexpected fields are rejected. This addresses a reproducible parser gap, not a
claim that every response from a small model will correctly call tools.

Removed New chat, Choose chat and Model and access from the open-session selector.
Long-press local session now lists New chat and Choose chat directly above Settings. Model/access remain in Settings. Changing chats retains previous saved history and reuses the loaded engine; a stopped engine still needs Start. Existing settings contain model,
acceleration, new/saved chat, project/access and the independent path-check toggle.
Full access and path-check semantics from v1.4.1-test are unchanged.

Package com.termux, test code1105. No data reset or new storage migration. Models,
chat files and optional library fields stay in place. Refreshed pre-feature
v1.3.4 recovery code1106; later updates after that recovery require code>=1107.
The old recovery code1104 cannot update over this candidate. No GitHub publication.

Local chat presentation: black background, no colored message cards, inline
You:/AI: speaker labels, preserved Markdown and a compact expandable Reasoning
line. Terminal sessions and cloud-agent output rendering are unchanged.
