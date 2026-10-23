# v2.0.3 — Media previews (unreleased)

Add photo/video thumbnails throughout the local file browser, including the Codex
attachment picker. Load visible rows in the background with bounded caches and
file-change invalidation. Size chat media viewers to their content and retain zoom,
playback and Save to Download. Local APK output links get compact cards: icon on
the left, filename/size/SHA-256 on the right. Open a card to save it or explicitly
request the Android installer. SHA-256 identifies the private copy, not publisher
trust; Android still requires installation consent.

Includes the unreleased v2.0.2 Codex attachment feature: images, text/log originals
and sampled timestamped video frames. Files are reviewed before explicit send.
Video analysis has no audio and may miss brief between-frame events.

Android8+/ARM64, unchanged package/signer/schema8, code1327. No data reset or
migration. Actual video formats depend on Android codecs. SSH/server lists keep
their existing presentation. Device checking of the new previews and installer interaction is pending.
Restart Codex with its saved chat to load the new helper; older installed builds
cannot display APK cards. The initial update is available as a regular APK file.

AI development assistance: OpenAI Codex
