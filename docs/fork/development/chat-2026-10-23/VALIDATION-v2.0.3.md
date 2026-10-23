# v2.0.3 development validation — media previews and APK cards

Unreleased developer build, 2026-10-23. Published stable remains v2.0.1.
Android8+/ARM64; unchanged com.termux, signing identity and schema8. Code1327
exceeds the developer's v2.0.2 code1326. No data reset or environment migration.
Restart Codex with its saved chat after updating to load the new helper.

## Final artifact

- APK: `build/apk/Termux-Jobs-v2.0.3-arm64.apk`
- Size: 47335238 bytes
- SHA-256: `bddbee8620a93d031a7e673f502f6e3fb2217037b456241b0f88366ede56415b`
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
- ZIP integrity, package/version, non-debuggable flag, ARM64-only ABI and matching
  prior certificate verified. Packaged Codex helper equals the source asset.
- All packaged native libraries byte-match the verified v2.0.2 APK. No X11,
  renderer, environment, server/proxy or working-session changes in this build.
- DEX/runtime verification:13597 defined classes, required definitions present;
  remaining31 optional references have existing explicit review notes.
- Bounded asset/DEX pattern scan found no private-key headers, configured
  GitHub/OpenAI token patterns or private proxy address. Private auth/config/key/
  keystore filenames and development directories were absent. This is not a
  claim to detect all possible secrets.
- Release Gradle build succeeded. Existing Kotlin metadata/custom lint-registry
  incompatibility warnings limit custom lint coverage; do not interpret this as
  a clean full-lint result. No system/SDK components were installed outside the
  repository.

## Relevant automated checks

103 final targeted JVM tests, zero failures/errors/skips, covering file operations,
file-list format2/backward compatibility/mtime and real portable shell listing;
viewport-only decode, cache identity, directory/close stale results, bounded JPEG
cache pruning, generated PNG thumbnail and unchanged source; media aspect fitting
and an actual Robolectric landscape viewer; attachment/card identity, send/ACK,
selection and composer focus; APK horizontal badge/metadata layout, private-copy
SHA-256, read-only content URI, refusal to expose other private app files, compact
APK details and absence of automatic installer activity on open.

Six Codex Node suites passed: adapter, attachments, native image input, native
chat, pagination and history wire. APK fixtures cover explicit local links with
spaces/parentheses, metadata, stable repeated snapshots, missing/symlink/remote
link refusal and no file content in snapshots. Existing native fixtures use the
real installed Codex0.163.0 app-server with a private CODEX_HOME and loopback-only
synthetic model, including streaming, input_image, persistence and pagination;
no real model/account API request was used for those fixtures.

Evidence: `.verification/media-thumbnails/tests-final.log`, `codex-tests.log`,
`build.log`, `apk-runtime.json`, `apk-badging.txt`, `apk-signature.txt`,
`artifact.json` and the final-APK-derived `update.json` in that directory.

## Review and logical inspection

Traced listing/filter/folder changes, scrolling, visible-row decode and close;
independent readers, canceled/stale generations, bounded bitmap/JPEG caches and
proot temporary-copy cleanup. Thumbnail loads update only row images and do not
select or send files. Existing selection/transfer behavior is retained. Local
browsers cover phone storage, Termux and installed proot environments; SSH/server
lists have no automatic thumbnails. Proot thumbnail source limits:20 MiB images,
64 MiB video; larger or unsupported-codec media retain selectable placeholders.
Memory cache6 MiB per browser, shared decoded-JPEG disk cache16 MiB.

Traced APK explicit-link recognition, selected-environment copy on background
workers, size cap256 MiB and private-cache cap512 MiB, streaming checksum, viewer,
explicit install click, unknown-source settings explanation/retry and content URI
with read-only permission. APK hashing never runs on the Codex polling/UI thread.
Opening a card starts no installer/settings activity; remote URLs are never
fetched. SHA-256 identifies the exact private copy and does not establish publisher
trust. Android owns final installation permission/package checks. APK output cards
do not enable APK input or automatic execution. Other agents' protocols and all
existing process shutdown mechanisms remain unchanged.

## Device evidence and remaining checks

The developer confirmed real photo attachment input in installed v2.0.2. That is
regression evidence for image input, not phone verification of these new changes.
The new v2.0.3 thumbnail/viewer/APK installer behavior needs device checking:

1. Update and restart Codex with its saved chat. Browse Phone storage and Ubuntu AI:
   photo/video previews, fast scrolling, folder switching, selection and return.
2. Open portrait/landscape images and video: compact dialog, zoom/playback and
   correct rotation. Actual Android codecs, gestures/DPI and memory pressure
   remain device checks; unsupported frames must leave files selectable.
3. Receive an explicit local APK link, check icon-left/details-right layout and
   size/SHA against the final artifact; open details and explicitly press Install.
   Check Android permission refusal/grant/return and cancel the installer if
   desired. No promise of installed-package migration testing is made.
4. While previews load, switch sessions, type a draft and load history: focus,
   draft and scroll must remain usable. Existing helpers cannot emit APK cards;
   the initial update remains a regular file in Download.
