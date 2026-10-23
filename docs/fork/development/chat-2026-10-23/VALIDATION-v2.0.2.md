# v2.0.2 development validation — Codex chat attachments

Unreleased build for developer device checks, 2026-10-23. Published stable remains
v2.0.1. Android8+/ARM64; unchanged `com.termux`, signer and schema8. Code1326
exceeds v2.0.1 code1325. No data reset or environment migration is introduced.
Existing helpers are immutable: restart Codex with its saved chat after updating.

## Artifact

- APK: `build/apk/Termux-Jobs-v2.0.2-arm64.apk`
- Size: 45821949 bytes
- SHA-256: `338020eee83b3ffb87dd57315150a61c8aa39fe86c81575a8c833d6654a0c1a6`
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
- ZIP integrity, non-debuggable package/version, ARM64-only ABI, matching previous
  signing certificate and packaged-helper/source equality verified.
- Packaged DEX definitions checked against isolated Android35 SDK: all required
  runtime types present; remaining31 references have existing explicit review notes.
- Native sources are unchanged. Reused Termux/bootstrap libraries were byte-compared
  with the verified v2.0.1 APK before packaging. X11 and renderer code are unchanged.
- APK asset/DEX scans found no private-key headers, configured GitHub/OpenAI key
  patterns or the developer's proxy-server address. Private config/auth/key/keystore
  filenames and development directories were absent from the APK. These are bounded
  checks, not a claim to detect every possible secret format.

## Tests

- Broad app JVM suite:876 tests, zero failures/errors, before the final attachment
  root-alias correction. Final scoped JVM run:55 tests, zero failures/errors,
  including trusted Android root aliases, symlink/traversal refusal, safe draft
  deletion, file-type checks, bounded text previews, phone-only permission trigger,
  image-only sends/failed ACK retention, card identity and focus preservation.
- Fifteen Node chat/receipt/permission/history suites passed. Six final Codex suites
  rerun after media projection caching: adapter, attachments, native image input,
  native chat, pagination and history wire all passed.
- Real installed Codex0.163.0 app-server was exercised with a private CODEX_HOME,
  synthetic PNG and loopback-only fixture model. `localImage` reached the model
  request as `input_image`, and its history returned an attachment row. No real
  account/model API request was made for this fixture.
- Native fixtures cover thread/manifest scope, path traversal and symlinks,
  foreign-thread refusal, Shell/attachment separation, request deduplication,
  explicit log sampling, video timestamps and generated/tool-image projection
  without base64 in Android snapshots. Generated-image projection is fixture
  evidence; it is not a fresh live image-generation check through the updated app.

## Review and logical inspection

Traced chooser cancellation/permission callback ownership, source-environment
export, private staging and existing Ubuntu control bind, native send/steer,
ACK/retry, submitted-file retention, per-run drafts, cached-history restoration,
thumbnail bounds, viewer dismissal and explicit non-overwriting Download export.
Canonicalize trusted Android root aliases, reject descendant symlinks and avoid
following symlinks when deleting unsent drafts. Restore the attachment owner before
rendering cached rows. No process shutdown, server/proxy setup, native agent policy,
working session or environment-install mechanism was modified.

## Required phone checks

1. Update, restart Codex with the saved chat, tap folder and choose Phone storage;
   check actual Android permission grant/refusal/retry. Environment browsing should
   not request phone storage access merely on opening the selector.
2. Attach a PNG from phone storage and a log from Ubuntu AI. Check preview/remove,
   text plus attachment and attachment-only send; keep drafts after a failed send.
3. Switch sessions while preparing/streaming, return and load older messages. Check
   images remain associated with their messages, typing/focus and scrolling remain
   usable, and other agents/terminals retain their existing behavior.
4. Open/zoom/save an image and play an MP4. Actual Android decoder support, gesture
   handling, keyboard behavior and memory pressure remain device checks.

Limits: up to4 files per message; images20 MiB, text/log/video256 MiB. Text input
contains an explicitly labelled first8 KiB sample and complete file path for further
reads. Video analysis uses up to12 sampled timestamped frames without audio and may
miss brief events between frames. Remote image URLs are not fetched automatically.
Submitted originals stay private for history/retry and consume storage. This does
not establish portable image recovery from an older backup without those copies.


### Developer follow-up

The developer subsequently sent a real photo attachment through the installed
Codex chat, and the assistant received and viewed it. This confirms that input
path on this configured device; it does not establish all codecs, permissions,
viewer gestures or every provider/device combination.
