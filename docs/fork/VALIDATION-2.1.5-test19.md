# Private test19 — 1514 — image actions, history and Stop

Date: 2026-11-07. Separate private diagnostic/acceptance build.
Same com.termux, ARM64, schema8, min26/target28, existing signer. No data migration,
voice functionality, environment installation or working-session restart.

## Change map

1. History: one window title; unchanged upper explanation. Empty state now reads
   «История пока пуста» and «Здесь появятся записи, отправленные из поля ввода в
   терминал или чат агента». Clear/search/record handlers and storage remain unchanged.
2. Original image failure: **not established as fixed**. Generic damage claim replaced
   by separate messages for decoder failure, 64M-pixel limit, missing/unreadable file,
   permission, empty/oversize file and cache failure. Unknown cause stays unknown.
   Materialization, image sampling and original export mechanisms remain unchanged.
3. Image actions: explicitly restored as standalone «Просмотр изображения» pills for
   all four agents. These split the surrounding chronology instead of being duplicated
   inside it. Claude Read of an image receives the image label. Same native row/key,
   details, thumbnail/path and original-view callback. Other tools retain chronology.
4. Compact Stop glyph is red #FF5252. Only its Paint color changed; dimensions,
   background, hit target, visibility rules and interrupt confirmation are unchanged.

## Codex versus Claude investigation

User video1001643106.mp4 is from another phone with older UI; exact installed code
not given. It shows threads-menu.png thumbnail, original-preview error and app
notifications of gallery/Download saves. Those notifications are user-supplied
observations, not independent inspection of the exported PNG. The PNG is not supplied.
Do not infer damage, working current-build UI or a decoder root cause from the video.

Codex imageView emits actionPreview.image; Claude Read(file_path) emits the same
shape through agent-action-preview.cjs. Both reach AgentActionDetailsView and
ChatAttachmentUi.open → ChatAttachments.materialize → bitmap(edge2048). Thumbnail
uses the same source metadata/cache with bitmap(edge320). Codex supplies its event
cache key; Claude gets the row key in the shared details view. Markdown file links
also mark localFileLink and obtain a current fingerprint; they are a distinct entry
from either agent's tool card. Environment selection is through the selected profile.

An exploratory GIF routing change was removed: it does not explain the reported PNG.
No speculative PNG decoder fallback or file conversion was introduced. Exact failing
original and on-device comparison of card versus link remain required.

## Verification scope

Evidence: .verification/test-2.1.5-test19/. Tests use fake projects/records and callbacks;
no working agent, X11 or model request is started by those fixtures.
Initial Claude adapter fixture expected failed despite an earlier cancelled receipt.
Updated expectation checks declined plus preserved nativeStatus=failed. Production
permission/receipt behavior unchanged. Initial assertion evidence retained in claude.log.
A native CLI trial requested a separate PNG task with --setting-sources empty and no
MCP servers. It returned is_error=true, Not logged in before generating the image.
This does not establish authentication state of the user's app profile or other phone.
No claim that a real Claude model made or inspected the image follows from this trial.

### Checked anew

- Shared JS action-preview and native-event projections: pass, exact image paths and
  bounded metadata, call/result distinction and foreign-event exclusion.
- Claude, Gemini, OpenCode adapter fixtures: pass. Added exact Claude PNG metadata
  and single call assertion. Codex projection covered by action-live fixture.
- Java UI/unit: 94 tests, 0 failures/errors/skips. Includes all-agent standalone
  image pills, call/result deduplication, details, gallery export, history and Stop routing.
  Four enhanced original-rendering tests rechecked separately: 4pass, 0fail/error/skip;
  each agent PNG creates ZoomImage with a drawable, not just enabled export buttons.
  These are synthetic PNG/UI fixtures, not the other phone or its failing original.
- Full modern Lint7.4.2, all5modules: 0 errors, 279 warnings; passed.
  CompileSDK35 beyond AGP tested range warning retained; no suppressions added.
- Final APK: ZIP, packagecom.termux, version1514/test19, ARM64-only, debuggablefalse,
  signaturev2/certificate, runtimeDEX/assets/notices/private-data/outerZIP checks passed.
  620 production source/resource files match staging; 7ELF payloads match stable2.1.4.
  Native inputs reused from the existing validated input manifest; reserve1499 unused.
  Size: 40620530 bytes. SHA-256: `e255dd15e2f1602f86f022dd0c34c9fbf82638d2c640d4b7ee34a7b0eb31c13b`.
  Certificate: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

### Reused

Native binaries/bootstrap use existing verified inputs; final equality and ARM64
payload audit required below. This reuses binary inputs, not device graphics results.

### Not checked on device

No ADB device is connected. Direct Android screenrecord returned ERROR: no display.
No interactive Android card, pinch gesture or screenshot is claimed as performed.
No APK is installed automatically. The separate real-CLI trial stopped at login.
User-facing test instructions: /sdcard/Download/Claude-image-check-test19.txt.
The user can create a new Claude chat, generate and Read a PNG, open its card and
link, pinch/return, export to gallery/Download and send the original plus exact error.

Missing original / permission / unsupported-decoder messages have unit coverage;
this does not establish the specific failing phone's cause. Real exports, permissions,
image zoom and current-build layout still need manual acceptance. Reserve1499 and
previous APKs are preserved; no publication or working process restart.
