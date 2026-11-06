# 2.1.5-test3 /1496 — v35 corrections

Private main APK requested by the developer. Checkout:
`/root/termux-fork/.verification/rollback-live-actions/source`, branch
`work/redesign-v35-1494` (historical name). No installation, working-session restart,
push, tag or publication. Standby1497 is built separately from the accepted old UI:
[backup report](VALIDATION-backup-1497.md). Prior1493/1494/1495 artifacts remain unchanged.

## Included and retained

- Common quiet graphite palette, final v35 dimensions and vector icons. Compact filled-square Stop above Send, plain model control, paired quota strips and text header navigation.
- Native draggable sidebar: title expands its own actions; only explicit Open selects the session. Base Termux is first/protected and opens at ordinary launch. Return from managers preserves the selected session. Recent commands is separate from presets.
- Commands/Aliases/Notes have independent entries without type tabs. Existing search/categories/favorites/CRUD remain; composer pickers have no creation and only insert. Aliases stay environment-filtered.
- Agent terminal is a full native screen with name/path/Return to chat, shared rounded terminal composer and horizontal input/extra-key switches. Existing PTY, authorization and process are retained; view transitions do not restart them.
- Full-screen grouped settings under the persistent header, anchored language selection, scale50/100/150 and preview. Existing Advanced/About, instructions/skills, environment, backup, update and server operations remain available.
- Native bottom-sheet creation for all four agents: Name → Folder with embedded button → Model → Chat → Permissions → optional connection path. Environment/readiness and detailed rights are collapsed. Back keeps entered values while selecting the agent again; cancellation must not persist or launch. Existing chat compatibility, busy-chat protection, preparation and explicit launch callbacks remain.
- The connection field still names one executable inside the selected environment, with no arguments; empty uses normal startup. Codex has no existing prelaunch dynamic model picker: the new model row explicitly uses the CLI profile default, while live model selection remains after connecting. Other agents retain their existing selectors. No fictitious model list or model request was introduced.
- Custom `+` menu above its anchor with vector icons, graphite surface and scrolling when space is limited. All four sources remain: gallery, device, project, other environments. Opening/selecting files preserves existing handlers and does not itself send them to the model.
- Drawer edge drag is disabled while IME is visible and in X11. Android29+ receives only a limited central200dp left-edge exclusion; Android may restrict it further. Right-edge system Back is retained. Keyboard Back is distinct from a later drawer gesture.
- Terminal header asks before switching to X11. Running desktops are reused; absent desktops follow existing readiness/start checks without automatic component installation. Return changes the view only. Stop remains separate, environment-scoped and guarded by shared ownership. Desktop settings begin with Themes; landscape control is retained.
- Existing text reveal, session/request timers, timeline expansion, bounded older-history loading, Files/Attachments/Git, preview/Gallery and exact common templates are retained. Text-scale position restoration scales the row offset instead of carrying an offset beyond the shortened row.

## Visual evidence and its limits

[Common parameters and source comparison](DESIGN.md#v35--единые-параметры-переноса-2026-11-06)
use final computed CSS, including late overrides. HTML v35 was actually rendered
with Firefox158.0.1 in an isolated profile;15 states at440×824 CSSpx are saved under
`.verification/redesign-v35-correction/html-render/`. Phone geometry was read as
1216×2688, density override442. Height is approximate and browser zoom was not
measured. The initial500px desktop viewport is not presented as phone-width proof.

The developer's screenshots establish concrete1494 discrepancies: black/turquoise
mixed palette, outlined header controls, obsolete status/model card and quota
layout, standard attachment popup, modal agent terminal with vertical checkboxes,
button-list settings and expanded creation form. Corresponding native structures
and shared styles were changed. HTML mock data was not copied into the app.

**Android rendering is unavailable in this stand.** Robolectric4.4 checks do not
establish actual font metrics, native screenshots, frame timing or visual acceptance.
The developer's accepted old1493 scenarios remain separate;1494 was visually rejected.
1496 awaits phone comparison. Existing managers absent from HTML keep their functions;
a common palette is not proof of complete visual equivalence.

## Checks and retained failures

Evidence: `.verification/redesign-v35-correction/` and
`.verification/test-2.1.5-test3/`. Tests use artificial sessions/files and existing
mock/native boundaries; they do not touch working chats, credentials or agents.

Initial full regression:1646 tests/40 failures; retained in `regression-1-results`.
Subsequent targeted runs preserve their initial UI-fixture, compiler and geometry
failures. Fixtures were adapted for intentional fullscreen/navigation changes;
composer height assertions were not weakened with pixel tolerances.
Full `regression-2`:1653 app tests/3 failures. One old settings-modal expectation,
one uninitialized test language and one genuine reading-anchor shift during text
scaling. The latter changed production offset restoration. `targeted-7` failed
compilation because its fixture used missing imports/an unavailable Robolectric API;
no tests are claimed for it. `targeted-8`:123 tests/1 failure, cancellation after
returning to agent selection. The fixture issued its next click before the previous picker dismiss/show event
settled and selected the wrong latest dialog. `targeted-10` identifies that exact
identity mismatch; `targeted-11` drains the UI loop between real user actions and
passes all four tests (including8 agent/language paths). Production also explicitly
marks cancellation before dismiss callbacks, and the close icon uses cancellation.
The earlier failure is not claimed as a proven phone defect. Original logs/XML remain.

Final modern Lint7.4.2: all five modules,0 errors/265 warnings, no infrastructure
or rule-loading errors. No new blanket suppression/baseline. Native Switch/API26,
custom view constructors and touch/accessibility warnings remain reviewed limits;
RU/EN labels and actual font/gesture behavior remain separate checks. The known
compileSDK35 versus AGP-tested33 caveat remains. Locale resources:586 workspace
strings plus app/shared keys and format arguments pass.
`regression-3`:1655 app tests,1 failure,0 errors/skips.1654 passed in that run;
the remaining file-filter accessibility fixture assumed Russian without selecting
a language. It now checks exact RU and EN labels with restored language state.
`final-targeted`:28 passed,0 failures/errors/skips, including that fixture plus
creation, editor, scale, popover, edge and desktop ownership. This is a successful
scoped retry, not a claim that the original full run had zero failures. Production
source did not change for this last test correction; no APK rebuild was needed.
No GitHub CI is run: push/publication are outside this task.

## Phone acceptance still required

1. Compare sidebar, four chats, both terminal screens, settings, creation and `+` against v35 at normal/large fonts. Verify labels and tap targets on a narrow screen.
2. Open/close keyboard and drawer: a Back gesture hides only the keyboard and keeps the draft. A separate later left-edge drag opens the drawer. Test outside tap, leftward close and right-edge Back.
3. Create/cancel for each agent; expand advanced fields; switch back to selection without losing fields. Check existing-chat restrictions and optional launcher path. Do not create a working session merely to test cancellation.
4. Check independent library entries and composer insertion without creation/execution; navigate recent commands and return to the selected terminal.
5. Check chat → native terminal → chat with preserved process/draft, extra keys and interactive mode. Test scrolling/older pages, collapsed timeline, live output and text scale without jumping to another message.
6. Check X11 confirmation, full area below header, portrait/landscape controls and input, then return without stopping. Shared-desktop stop remains a separate protected action.
7. Verify popup with keyboard visible, dismissal and preserved draft. Recheck file/Gallery actions and speech-provider cancellation if used.

## Explicit exclusions

Image accumulation and large-request external disconnects remain unresolved. No
CLI fork, unsupported experimental context mode, new automatic-compaction policy,
MCP or plugins. Low-level Android handoff, crash recovery and device backup export/
restore remain unverified/excluded as agreed. Accepted phone results for functional
artificial Gemini key transfer/API refusal/normal retry remain accepted; they do
not establish low-level crash behavior. Real Claude subscription coverage, the
original clarification case without UUID, real graphics/IME/speech and future-data
rollback retain the limits of prior reports. Backup APK is not a user-data backup.
Working history, authorization, saved instructions and CLI/Components are unchanged.

## Final signed APK — checked anew

Source commit: `46b88f87c4852f7db1990475a9e93fc8ffea5e6c`.
File: `Termux-Jobs-v2.1.5-test3-arm64.apk`,40573510 bytes.
SHA-256: `1969cfbd1e546fbab8d9856a25bed9885aa620a0c03329b5be681b2bf5b462d2`.
Package com.termux; version2.1.5-test3/code1496; ARM64 only; min26/target28;
non-debuggable; v2 signature verifies against unchanged certificate
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

ZIP CRC/outer-entry integrity passed; no stale outer local headers. Static runtime:
14450 definitions, no required or unreviewed missing types,31 reviewed optional
platform/dependency types. This is not execution on an installed Android package.
Signer/X11 packaging guards ran. All seven ELF match stablev2.1.4. All124 assets
match current source; four common RU/EN templates match the agreed archive hashes
and final APK exactly.604 production input files match JVM/Lint/APK stages.
No packaged signing files, environment files, test bytecode or diagnostic fixtures
were found by the artifact checks. No user authorization files were read.

Three JNI payloads were reused from accepted1493 after native-input verification;
GPU/driver/device behavior is not newly tested. Existing adapter/CLI assets and
user-data schema remain unchanged. Original README remains unchanged; RELEASING
preserves its rules and records the new explicit1496/1497 exception.

Download: `/sdcard/Download/Termux-Jobs-v2.1.5-test3-arm64.apk`; exclusive new file,
read-back SHA matches the final artifact, including the /storage/emulated/0 alias.
Its `.sha256` sidecar is saved. Immutable1493/1494/1495 source APK hashes were
checked again and remain unchanged. Separate standby1497 source commit269a63cf
and hash are in the linked backup report.

`module-final`:154 terminal-emulator tests executed anew with --rerun-tasks and
passed,0 errors/skips. terminal-view and termux-shared have no standalone tests
(NO-SOURCE, not PASS); the full app run covers their UI/integration consumers.
