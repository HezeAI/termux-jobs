# Private alias/chat-scale test3 validation — 2026-10-26

Private follow-up to test2; no public release has been authorized for this preview.

## Artifact

- `build/apk/Termux-Jobs-test3.apk`: **2.0.9-test3 / code1416**.
- Package `com.termux`, ARM64, same signing identity as stable v2.0.8/test2.
- Size **39798181 bytes**; SHA-256 **f8555cfdc3dff84635039d1840698f4bb06cc0e1e83f7abeebdd4acc7d2efe9d**.
- Certificate SHA-256 **dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644**.
- ZIP integrity, signature, non-debuggable flag, package/version and ABI pass.
- All79 source assets match; all3 JNI libraries and X11 runtime assets match stable v2.0.8.
- Runtime:13,851 definitions,31 previously reviewed optional/framework gaps; no unreviewed missing types.
- No forbidden private filenames or configured secret-pattern findings in APK assets/DEX.
- min26/target28/compile35, Android8+ app / Android11+ X11, header v2.0 unchanged.
- Schema8 remains. Old profiles default to100% chat scale; aliases/commands/notes remain independent.
- No data reset or rootfs/account migration. Next update over this preview must exceed code1416.

## Features and behavior

The single Import/export Alias card has four explicit actions: shell import,
JSON/backup import, shell export and JSON export. Shell input is bounded to1MiB,
parsed as literal data and never sourced/executed. Quoting, multiline literals,
continuations, multiple assignments, comments and an optional final semicolon
are supported; dynamic statements and command chains are skipped and counted.
The destination environment is explicit, followed by record selection/preview
and replacement confirmation. No automatic discovery from environment startup
files is added. Shell export selects one environment and writes a fresh quoted
`.sh`; JSON retains environment/category/favorite metadata. Existing target
whitelisting, source/destination rechecks, compensation and recovery paths remain.

All four agent status panels expose per-session50–150% chat scale, default100%,
and a100% reset. Measured transcript geometry, Markdown/code/tables, tool/file
cards, thumbnails, drafts and Shell/message composers scale without recreating
the chat or sending agent requests. The shared composer has an explicit bounded
container and restores outside structured chat/on terminal presentation/close.
Drafts, selection, code expansion, stable row identities and reading anchors are
retained. Streaming reuses a restored baseline rather than compounding scale.
Session copies and workspace JSON/backup retain the optional chatScale field.

## Evidence

- A broad **226-case JVM run** passed; after the composer extension, a final
  **114-case targeted run** passed. Across retained unchanged results and final
  reruns, **228 distinct cases /25 classes**,0 failures/errors/skips.
  Changed paths include measured50/100/150 layouts and exact restoration, stable
  Markdown/code/table/thumbnail geometry while streaming, expanded tool cards,
  reading anchors, independent session scales, persisted/copy/default/range rules,
  guarded controls/reset, shared-composer restoration/draft selection, transfer
  menus, environment-scoped shell export and existing chat/backup/navigation flows.
- **44 Python backup cases** pass, including literal import without Popen,
  unsupported/dynamic/chain rejection, quotes/multiline/one-environment export,
  real Bash reading of generated declarations without executing their bodies,
  source fingerprints, invalid/oversize inputs and prior backup regressions.
- **8 shell fixture cases** pass for the managed alias installer and startup guards.
- Final compatible AGP7.4.2 lint across5 modules: **0 errors /220 warnings**.
  Two warnings above test2's218 are intentional bilingual readout/dropdown string
  concatenation in AgentChatScale and the manual-import environment selector;
  no lint suppression was added. Legacy APK-chain diagnostics are not treated as
  substitutes for this separate compatible lint gate.
- Final signed APK/runtime/assets/privacy verification passes.

## Review and limitations

Parsing never uses foreign shell code or expands source variables/substitutions.
Alias bodies remain explicitly selected command data and are quoted by the
existing owned helper. Missing rootfs rechecks cannot fall back to Termux. Ubuntu
AI/SSH/custom aliases remain excluded from managed alias destinations. Existing
manual aliases outside the library are not probed for collisions. The last
supported literal definition wins; this does not emulate arbitrary startup code.

Scale changes only presentation and workspace metadata; native request snapshots,
permissions, tunnels and processes are unaffected. Provider dialogs guard their
bound profile, queued persistence flushes on release/detach, shared-input restoration
is explicit, and unchanged polls do not reassign already-applied geometry. No X11,
driver/theme/runtime or process-shutdown implementation changed.

JVM/mock/fixture checks are not physical Android/GUI proof. The developer should
check70–80% readability and all four agent panels on the phone, long live replies,
scroll position, selection/IME and chat-to-terminal restoration. Export/import a
small shell file from ordinary Termux, compare its full preview, cancel/confirm a
replacement, and verify the chosen environment in a fresh shell (restart the
persistent Termux tab). Check actual storage permissions and Zsh separately.
Import file/helper/metadata commits are not a cross-process crash transaction;
existing recovery records/explicit partial-failure reporting remain necessary.
