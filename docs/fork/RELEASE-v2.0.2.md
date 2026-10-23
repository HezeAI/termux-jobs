# v2.0.2 — Agent chat improvements

Send files directly to your agent chat instead of typing where they are stored on
your phone. Choose files from phone storage or a local environment, attach them
with a message or send them on their own, and work on them in the same conversation.
This makes project work and builds easier: agents can return supported local outputs as
chat cards for preview or saving, and APK cards offer Android installation.

- Attachments in Codex, OpenCode, Claude and Gemini: images, text/logs, binary files,
  APKs and sampled video frames. Originals remain available to agent tools;
  model capabilities and native CLI permissions still apply. Video frames have
  timestamps and no audio.
- Photo/video thumbnails in the local file manager, compact media previews and
  APK cards with filename, size and SHA-256. Review/remove attachments before Send.
- First Codex sign-in starts from chat; the official device code stays in the
  terminal. Gemini API authentication gains an optional protected key field in
  session/preset settings, with an explanation of the normal terminal fallback.
- Fix OpenCode attachment routing and large native image replies. Polish the
  remove cross, context notification and italic app/version header.

**Compatibility:** Android8+/ARM64, unchanged com.termux/signing identity/schema8,
versionCode1331. No data reset or environment migration. Restart agent sessions
with their saved chats after updating to load the new helpers. Stored Gemini keys
are device/profile-bound: re-enter them on preset duplicate/import. Finish important
work before updating because Android may stop app processes.

HezeAI confirmed Codex image input and OpenCode file sending on the phone. Automated
checks cover native CLI admission and isolated controller flows; fresh-account login,
Gemini/Claude live-provider media and all Android permission/layout combinations
are not claimed fully verified.

[Usage](https://github.com/HezeAI/termux-jobs/blob/v2.0.2/docs/fork/USAGE.md) ·
[Validation](https://github.com/HezeAI/termux-jobs/blob/v2.0.2/docs/fork/VALIDATION-v2.0.2.md) ·
[Sources and credits](https://github.com/HezeAI/termux-jobs/blob/v2.0.2/docs/fork/THIRD-PARTY.md).

AI development assistance: OpenAI Codex
