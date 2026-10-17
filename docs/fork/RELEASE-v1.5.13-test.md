# v1.5.13-test — repeated writes and compact approvals

Private local-AI test build; no GitHub publication. Applies to both llama.cpp and LiteRT-LM local chats, including imported models. External AI CLI sessions are unchanged.

## Loop recovery

The developer's report1176495361588709667 showed37 generations in49 seconds with repeated valid native tool calls. The requested shared-storage file existed and contained the exact expected text, confirming successful writing; this was a model loop rather than an access failure.

The existing exact-cycle detector is retained. An additional per-turn guard detects the same successful write proposed again after two identical successful writes, even when intervening directory metadata varies. It first blocks the redundant write and reminds the model to read/verify or finish. A subsequent identical proposal stops the turn. After that warning, repeatedly rediscovering the same folders also stops rather than looping indefinitely; fresh paths and actual reads remain available.

Explicit requested repetition counts are respected. A changed write, a command/install attempt (which can mutate state even when it fails), a failed write, or an actual fresh read showing changed target content clears the duplicate-write assumption. Cached-read annotations do not clear it. This is not a sandbox or a universal semantic loop detector: differently spelled paths, changing contents and arbitrary command effects remain distinct cases. No blanket limit on all tool calls added. No token/context/model/default permission changes.

## Approval UI

Pending permission cards have smaller padding/text and a bounded scroll area for long details; Yes/No hit targets remain44dp. The full request/APT plan and installation caveats are retained. After a decision, the card becomes an oval pill: Permission granted / Permission denied / Request interrupted (localized RU/EN). Tap it to view read-only details; it cannot replay the decision. Approval does not mean an action succeeded.

Fixed a misleading duplicate Not executed card after approval. History rendering previously paired tool results only with the immediately previous message. Approval/UI records in between caused an already successful result to appear again as an unconfirmed legacy call. The nearest matching recorded action is now recognized across UI-only records, with exact tool name/output matching and conservative boundaries so unrelated legacy output is preserved. The existing approval wait remains; a dedicated test checks it does not return before the user clicks.

## Compatibility

Private versionCode1150, package/signing identity retained. No data reset/migration. Existing approval records restore in collapsed form; historical requests remain readable. Models/weights, configured output caps and installed environments are untouched. Device checks remain necessary for layout, taps, cancellation and actual model behavior.
