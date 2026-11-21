# Termux Job’s 2.1.9-test7 validation

Private ARM64 build, 2026-11-21. Developer: HezeAI.
AI development assistance: OpenAI Codex.

## Scope

The developer's test6 phone screenshots showed Antigravity offering to send a
message before sign-in. The bridge had yet to start the native CLI. It now
starts its existing streaming worker when the chat socket opens. The bridge
waits for native initialization before forwarding a message and reports sign-in
as soon as the CLI identifies it. The right-side terminal handles interactive
sign-in. Returning to chat starts a fresh streaming worker and refreshes the
state without submitting a message.

The creation form omits **Update CLI**. In the sidebar, Antigravity's
**Chat management** still opens the version checker and confirmation flow.
The change uses the same saved profile and history formats.

## Checked anew

- An isolated probe of the pinned Antigravity 1.5.13 executable held stdin open
  and wrote zero bytes. A clean signed-out home produced a sign-in diagnostic.
  A synthetic Gemini provider configuration produced a native init frame
  without any model or loopback HTTP request.
- The production bridge was tested under the same local-only network guard.
  Both sessions began without user input or history. The signed-out bridge
  reported `authRequired` and `needsTerminal`. The synthetic configuration
  reached `connected`. Both stop requests were acknowledged, both owned
  native children were reaped, and stopping the signed-out bridge left its
  neighbor running and connected. [Sanitized exact-bridge results](../../.verification/test7-antigravity-auth/summary.json).
- Node fixtures check empty stdin during startup, early auth status, draft-safe
  rejection before initialization, terminal handoff, rechecking after closing
  the terminal, history resume and ownership checks before signaling.
- JavaScript syntax and RU/EN resource consistency passed.
- File paths and hashes matched across 657 packaged source inputs and three
  stages. The native source comparison matched test6. Only the Antigravity
  bridge asset and WorkspaceController changed in that file set.
- All 15 focused JVM tests passed. The RU/EN creation form had no updater
  action. The saved Antigravity chat opened the updater from the sidebar
  without starting an agent. Modern Lint reported 0 errors and 298 warnings.
- ARM64 release assembly completed in 4m 22s. ZIP integrity, alignment of
  408 stored entries, the ELF 16 KiB LOAD alignment, package, version and SDK
  metadata, non-debuggable status, signing certificate, runtime dependency
  inspection and private-file scans passed. The replaced bridge asset matches
  the source bytes. The 31 unresolved DEX types have existing review notes.

## Reused

Test6 native binaries and other packaged assets. The three native libraries
and all non-bridge assets were compared byte for byte with the signed test6
APK. The package, signer, SDK levels and manifest permission declarations
match. The update does not change stored profile or history formats.

## Not checked

Phone UI acceptance and a real Google account sign-in remain outside this
build verification. Current native Antigravity capabilities are described by
the [CLI reference](https://antigravity.google/docs/cli/reference/).

## Artifact and logs

- `/sdcard/Download/Termux-Jobs-v2.1.9-test7-arm64.apk`, 40,699,627 bytes.
- `com.termux`, 2.1.9-test7 / 1547, arm64-v8a.
- SHA-256 `6172424573e13f299aa3cb6b0b655df7cf8b54d9b84b8f58af316f76492e4137`.
  The Download copy matches the verified project file.
- [Plan](../../.verification/test7-antigravity-auth/PLAN.md),
  [assembly](../../.verification/test7-antigravity-auth/build.log),
  [JVM tests](../../.verification/test7-antigravity-auth/jvm-results.json),
  [Lint summary](../../.verification/test7-antigravity-auth/modern-lint-summary.json),
  [final file checks](../../.verification/test7-antigravity-auth/apk-validation.json).
