# v2.0.5 — Agent attachments and sign-in (unreleased)

- Send files from the local environment or phone storage in Codex, Claude, Gemini
  and OpenCode chats. Review/remove before sending, with text or as file-only input.
  Local media thumbnails and compact previews are shared. APK output cards retain
  Download and explicit Android installer actions.
- Preserve original files for tools. Use native image blocks/URIs or Gemini @ file
  handling; videos supply up to12 timestamped frames without audio. Logs include a
  bounded sample and original path; generic binaries/APKs are referenced by path.
- Offer Codex sign-in in chat when native authorization is missing. URL/code stay
  in the native terminal; successful login returns to chat. Existing authorization
  is reused. Account/device-code eligibility and network access still apply.
- Add a masked optional Gemini API key below the API auth choice. Encrypt stored
  keys with Android Keystore. Blank preserves an existing key; otherwise the CLI's
  existing auth or terminal flow remains available. Replace/remove explicitly;
  changes apply on the next launch.
- Make the attachment remove cross slightly larger/red with unchanged touch bounds.
  Context-compaction notices open in a text-sized window without Back, dismissed by
  outside tap. Show italic v2.0 next to the italic app name.

Includes the earlier development attachment, thumbnail and APK-menu changes since
stable v2.0.1. Android8+/ARM64, unchanged com.termux/signing identity/schema8,
versionCode1329. No data reset or automatic environment migration. Gemini keys are
bound to this device and profile; duplicates/imports need a new key. Finish important
work before updating because Android may stop app processes. Restart agent sessions
with their saved chats to load updated helpers; active helpers are not hot-patched.

Source and isolated native-fixture checks are described in VALIDATION-v2.0.5.md.
Real account login, API key authorization, model media interpretation and Android
keyboard/permission/layout behavior still need phone verification. No live account
requests were used for automated checks.

AI development assistance: OpenAI Codex
