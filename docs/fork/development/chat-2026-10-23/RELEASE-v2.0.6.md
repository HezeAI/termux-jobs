# v2.0.6 — Fix OpenCode attachment sending (unreleased)

Fix a v2.0.5 integration error: the OpenCode controller looked for staged files
beside its nested receipt file instead of the control socket where Android exposes
the attachment directory. File-only sends could therefore fail before admission.
The controller now passes an explicit attachment root; receipt and connection data
stay in their original locations.

Native prompt/inbox/history responses may contain image data larger than the old
2MiB cap. Those routes now have a bounded40MiB cap; other response and Android chat
projection limits stay unchanged. No raw image base64 enters the Android transcript.
Tests reproduce rejection with the old APK's helper, then exercise file-only/mixed
sends through the production Unix controller and native image decoding through an
isolated OpenCode2.0.28 server. No real account/model requests are used.

Includes v2.0.5 agent attachments, sign-in and UI changes. Android8+/ARM64,
unchanged com.termux/signer/schema8, code1330. No reset or environment migration.
Restart OpenCode with its saved chat to load the corrected helper, then select
and send the file again. Gemini key portability restrictions from v2.0.5 still apply:
keys are device/profile-bound and must be re-entered on duplicate/import. Finish
important work before updating because Android may stop app processes. Actual
Android send-button/media behavior remains a phone check.

AI development assistance: OpenAI Codex
