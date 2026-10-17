# v1.4.5-test — local AI chat presentation

Private test build, versionCode 1111. No GitHub publication.

- Small independently animated activity dots between Start/Stop and Server + Agent; generation phase text is removed. Model download/start failures retain their informative text and progress.
- Local tool requests/results have compact localized cards with expandable original JSON/output and factual completion/error/rejection status. Legacy requests without execution metadata are explicitly unconfirmed; legacy output remains available.
- User messages have a thin turquoise left accent; black background, selectable text and reasoning dialogs remain.

Package/signing identity unchanged; optional history presentation metadata only, no reset or data migration. Permissions and path-check policy unchanged. VersionCode exceeds recovery code 1110. Local chat/model backup limitations remain as documented in LOCAL-AI.md.

Device check: verify animation does not move the chat/selection/input, stops when idle, and resumes on return to the tab; inspect successful, failed and denied tool cards, raw output and older history. These are device checks, separate from automated verification.
