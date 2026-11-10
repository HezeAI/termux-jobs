# Timeline action cards and permissions icon — 2026-11-10

Private candidate **2.1.8-test3 / 1539**, based on phone-accepted test2 /1538.
No installation, publication, real agent invocation, profile migration or process stop.

## Implementation and boundaries

Each timeline action keeps one stable clickable card: friendly title, actual command
in muted 11sp monospace (maximum two lines), then existing status/time metadata.
The detail window keeps native command/output, duration and supplied exit code.
Exit 0 is explicitly a successful command exit, not proof that the whole check passed.
Missing exit codes are labelled as not supplied rather than inferred.

Native tool descriptions take priority. Without one, AgentCommandTitle conservatively
recognizes reading, searching, git status/diff, common test and build invocations.
Unknown calls have a generic title; shell operators/substitutions receive a multiple-command
title. No model calls, command execution, permission changes or RTK rewrites are added.
Malformed/truncated input falls back to a generic title. This is not a complete shell parser.

Codex, Claude, Gemini and OpenCode adapters forward native descriptions and mark
truncated command previews. Gemini now retains its actual shell command in detail text;
its output offset accounts for that prefix. Existing detail paging/size limits remain.
The sidebar Permissions and approvals item alone uses the new outlined shield vector.
Other gears, actions, status labels and selection styling remain unchanged.

## Verification scope

Evidence directory: `.verification/timeline-actions/` in the canonical checkout.
Seven Node suites passed: action live events, event times, native previews, timeline
projection, Gemini, Claude and OpenCode. Projection fixtures verify native descriptions,
long command preview truncation, full command/output retention and original-input integrity.
These are local fixtures, not real-provider tests.
Localization: PASS, 587 workspace strings plus settings keys/format arguments.

Initial compile failures from converting text rows into a composite ActionRow were fixed.
Initial assertions depended on old row casts, hidden commands and exact status strings;
they were updated for the requested presentation. The classifier test now uses Robolectric
for JSONObject, and the Russian-caption test explicitly selects Russian.
Initial failed logs remain in the evidence directory; final results below are authoritative.

## Final artifact

100 targeted JVM/Robolectric tests passed (7 classes, zero failures/errors/skips).
Full modern Lint 7.4.2 passed for all five modules: zero errors, 291 warnings.
One new ViewConstructor warning concerns the programmatically created ActionRow;
it is not inflated from XML. The legacy assemble Lint printed incompatible Kotlin
metadata diagnostics; it is not counted as successful analysis. Modern Lint is separate.
Release assembly and final checks passed: com.termux, 2.1.8-test3 /1539,
ARM64 only, non-debuggable, valid ZIP and compatible release signing certificate.
Production assets/staged sources match, runtime class checks pass, all seven native ELF
payloads match stable 2.1.7. RTK remains the public jobs.1 pin without an embedded archive.
APK: `/sdcard/Download/Termux-Jobs-v2.1.8-test3-arm64.apk` (40677933 bytes).
SHA-256: `cb63f6294e47f5c814aad04b0b3a341f259866b3d6294eae541a63031b1cbeec`.
The Download copy matches the audited artifact. No APK was installed or published.

Native source/build inputs match v2.1.7. Existing native payloads are reused, not rebuilt;
this UI task does not claim fresh native compilation or full CI execution.
The APK contains the compiled shield vector.

## Phone validation still required

Check the shield and unchanged sidebar behavior; short/long/compound commands,
large font, two-line ellipsis, opening full details, updates during arrival animation,
errors/duration/exit code and reading above the bottom of the chat. Check all four agents.
Local Robolectric and adapter fixtures do not replace this visual phone validation.
