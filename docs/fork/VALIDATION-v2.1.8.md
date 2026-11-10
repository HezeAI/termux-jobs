# Release validation — 2.1.8 /1540

Preparation in progress; no publication claimed by this source snapshot.

## Scope and acceptance

Changes relative to v2.1.7: timeline title/command layout and local classification,
native description forwarding by four adapters, sidebar states, permissions shield,
notification names and idle X11 control-service cleanup. Data formats, RTK component,
component loaders, permissions, authentication and command execution are unchanged.

The developer accepted test2 /1538 session labels and test3 /1539 action cards on the
phone, including a live 20-second command showing Running. The test3 APK SHA-256 is
`cb63f6294e47f5c814aad04b0b3a341f259866b3d6294eae541a63031b1cbeec`.
Subsequent production changes are only the requested exclamation mark in the shield
and release version metadata. The final stable artifact requires separate checks.

[Timeline checks](VALIDATION-TIMELINE-ACTIONS.md), [session labels](VALIDATION-SIDEBAR-STATUS.md),
[notification lifecycle](VALIDATION-X11-NOTIFICATIONS.md) preserve detailed evidence.
The original Arch service was not stopped: ownership was not established. Its final
state is not claimed. No working environment or chat is stopped by this release work.

## Verification

Final checks and artifact identities will be recorded before publication.

## Limits

Phone feedback is attributed to the developer. No new real-provider/model test,
all-permission-mode test, GPU matrix or full security audit is claimed. The shield
exclamation mark and release metadata have not had a separate phone acceptance.
The original security-audit dates remain unchanged. Existing known limitations
are retained in README and thematic documentation.
