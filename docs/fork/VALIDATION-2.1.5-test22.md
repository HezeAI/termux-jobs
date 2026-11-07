# Private test22 — 1517 — project files, attachments and creation transitions

Date: 2026-11-07. Test APK only. Release publication requires the user's later
verification and confirmation. No APK installation or working-session restart.

## Confirmed causes and changes

The attachment list never decoded thumbnails: it assigned a static image icon.
Availability checked the agent's guest /tmp attachment path through an independent
ProjectFiles proot command that did not bind the private attachment directory.
The agent session had that bind; the separate file browser did not.
Three pictured JPEG originals (1001643103, 1001643098, 1001643119) exist and fully
decode with Pillow at 1216×2688. Metadata/hashes are recorded in
`.verification/test-2.1.5-test22/actual-image-evidence.json`; no private image copies
were added to source or report. This establishes the cause for managed attachments
available in this environment, not every missing file on another device.

Managed originals now use the existing canonicalized app-private mapping. Generic
agent files retain ProjectFiles transport and fingerprinted materialization.
Availability distinguishes missing, unreadable and failed checks. Actual thumbnails
clear the placeholder tint; tap opens the existing original viewer. Historical
references without type now infer it from the name/path. A valid original with
failed thumbnail decoding is not called missing or damaged.

Location browsing binds only the selected agent's chat-attachments directory,
not control sockets. Direct Termux location uses the mapped host folder.
The three buttons are Go to message / To Download / Location. Download exports
original bytes using the existing exclusive-create/NOFOLLOW exporter. Name
collisions receive a suffix, never overwrite. Success includes the saved basename;
errors and cancellation retain the record. No send/model calls were introduced.

All three tabs have smaller cards, controls and spacing, including header,
search, filters, file tiles, pagination and the bottom selection action.
Outlined actions have separate margins and no second enclosing action frame.
Minimum primary control height is 44dp; names and labels wrap.

Language selection requests space below its settings row and opens downward.
Other popovers retain their existing placement behavior.

The old creation flow used hide/show on the form while adding a new chooser
window on every Back. Showing the existing window did not reorder it above the
new chooser. The creation form now hosts both pages in one window with one dim
layer. The opaque, click-consuming form is always the last/top page. It slides
left on selection and right on Back. Touches are blocked during motion; choice
handlers also guard state, and Back/Create are disabled. Fields retain their View
instances across agent selection. Dismiss cancels animation and runs existing
editor cleanup. No session starts until the existing validated Create handler.

## Verification

### Checked anew

- Main targeted suite: 115 tests, initially 114 passed / one fixture failure.
  The language-dropdown fixture used a full-screen anchor; after the posted layout
  there was no space below it. Setting a realistic 44px anchor fixed the fixture.
- Final rerun: 13 tests, zero failures/errors/skips, covering creation steps,
  full creation form binding for every agent/RU/EN, and popovers. Final app source
  bytes match the unit stage and APK stage. Other 102 passing test cases from
  the main suite are unaffected by the final creation-button disabling change.
- Four-agent attachment fixture: managed user and generic agent originals,
  decoded bitmap/no tint, original callback, jump/location, byte-identical exports,
  duplicate-name suffix, retained originals, missing file, two fresh panel instances.
- Compact layout fixtures: 320×600, four agents, RU/EN, light/dark, font scale1/2;
  wrapped labels/names, action hit tests, tab/filter/search/pagination behavior.
  Existing project transport/export, image gallery, reference and settings tests ran.
- Creation fixture: eight repeated cycles across agents, double Back/selection,
  one host/two pages, top opaque form, stable window dim value, retained draft,
  cancellation. Existing form fixture also checks same-agent Back/reselection for
  all four agents and languages. These assertions do not verify rendered frame timing.
- Modern Lint7.4.2: all five modules, zero errors, 280 warnings, no infrastructure
  diagnostics. Includes the final button-disabling code. One extra SdCardPath warning
  corresponds to the existing app convention of /sdcard/Download in this new action.
- Release assembly succeeded. Legacy lintVital emitted ARM64/custom-check loading
  failures; it is not counted as passed. The separate modern Lint is the static gate.
- Final APK: ZIP integrity/no stale local headers, com.termux, 2.1.5-test22/code1517,
  ARM64-only, non-debuggable, v2 signature with unchanged certificate
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
  Required runtime classes/assets/notices and private-data guards passed;
  621 production sources matched stage. All seven ELF payloads are ARM64.
- Earlier first-stage suite: 80 cases, four old UI expectations failed after the
  specified missing-file wording and Open-button removal. Those expectations were
  updated; all four passed in the main suite. Initial logs are retained.

### Reused

Native payloads from the validated test1 build: all 95 recorded source inputs were
freshly hash-compared, native flags unchanged; payload hashes verified in the APK.
No native/GPU execution claim. Data schema8, package and signer remain unchanged;
no migration. Actual installed-update/data retention was not exercised on a phone.

Evidence: `.verification/test-2.1.5-test22/` contains original/final unit summaries
and XML, unit logs, modern-lint-summary.json/XML/run log, build-final.log,
source-final-check.log, runtime.json, signature.txt, native-reuse.json and APK audit.

APK: `Termux-Jobs-v2.1.5-test22-arm64.apk`, 40,624,810 bytes.
SHA-256: `24fdbf7ea64d098d9b737c5fcdf144f710cb100f3184cdc477943d282b7bbc19`.
Local fixtures are not handset tests.

## Not checked on a device

ADB reports no attached device. Not independently checked: actual phone rendering,
first animation frames/black flashes/dim stability, keyboard/rotation/touch on the
handset, real Download permission/storage behavior, original zoom/return from this
list, old and newly created real attachments after app restart, all three tabs and
pagination on the handset. The fresh-panel fixture is not an application restart.
No X11/GPU/environment installation test was run for these UI changes.

Manual acceptance on the test APK: inspect all three tabs at narrow width and
large font; long names and non-overlapping outlines; load further records; old/new
user and agent thumbnail → original → return; all three attachment buttons;
duplicate Download filename; reopen after app restart. Check language dropdown.
Repeat choose → Back → choose several times with same/different agents and rapid
taps, with keyboard open/closed; confirm form covers chooser from the first frame,
one stable dim layer, unchanged draft, cancellation and successful creation.
