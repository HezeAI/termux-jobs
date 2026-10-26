# Private alias test2 validation — 2026-10-26

Private follow-up to alias test1, based on public v2.0.8. Publication is pending
explicit developer authorization after device acceptance.

## Artifact and compatibility

- APK: `build/apk/Termux-Jobs-test2.apk`.
- versionName **2.0.9-test2**, versionCode **1415** (above test1/code1414).
- Package `com.termux`, ARM64; min26/target28/compile35, Android8+ app / Android11+ X11 unchanged.
- Size **39792329 bytes**; SHA-256 **ba4eadb050af1e254fa8ed0555db05d3809f614804898c48c65c4b3f4d0473ec**.
- Signing certificate SHA-256 **dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644**, matching stable v2.0.8 and test1.
- ZIP integrity, non-debuggable flag, signature, package/version and ABI checks pass.
- All79 source assets match. X11 runtime assets and all3 JNI libraries match stable v2.0.8.
- Runtime class check:13,824 defined types,31 previously reviewed optional/framework gaps; no new unreviewed missing types.
- No credential filenames or configured secret-pattern findings in APK assets/DEX.
- Optional schema8 aliases retained; old libraries load without aliases. No data reset or environment migration.
- Header remains v2.0. Next update over this preview must exceed code1415.

## Changed behavior

Separate Import Alias and Export Alias cards in Backup and restore provide explicit
selection and full command preview. Export writes only selected alias records to a
fresh JSON in the chosen directory; no commands, notes, presets, settings, history
or authorization are included. Alias command bodies themselves may contain private
data. Import accepts alias JSON, a workspace library or a backup manifest. It keeps
environment/category/favorite bindings, assigns General for an empty category,
confirms same-name/same-environment replacements, and preserves unselected records.
Manually defined shell aliases do not automatically enter the app library.

Termux alone shows the requested tab-restart text below the environment selector
and in deletion confirmation/success. Switching to a guest hides that editor hint;
other targets retain the original reopen-session guidance. No shell is restarted
or injected into by this feature.

## Relevant checks

- **132 JVM cases /14 classes**,0 failures/errors/skips. Covers import/export
  records across all8 supported targets, old libraries, category fallback, fresh
  UUIDs and environment-scoped replacement, preserved unrelated records, stale
  destination detection, batch failure compensation and explicit rollback failure,
  persisted-value verification, UI selection/full preview/confirmation/Cancel,
  Termux-only hint visibility and switching, and existing backup/forms/persistence/
  navigation/owned-process transport behavior.
- **40 Python backup cases** pass, including standalone export/reinspection,
  retained bindings, excluded unrelated data, unique output names, old libraries,
  invalid alias arrays and combined library-size bounds.
- **8 isolated shell fixture cases** pass against the actual managed-alias helper:
  quoting, no body evaluation, rename/delete, symlink/unmanaged-path rejection,
  loader idempotence, empty collections and startup preservation.
- Compatible AGP7.4.2 lint across5 modules: **0 errors /218 warnings**, matching
  test1's count. The legacy APK build's previously documented lint/Kotlin metadata
  noise is separate from this successful compatible lint gate.

## Review and state-flow inspection

Preview, selection and cancellation never write environment definitions. Fresh
source fingerprints and destination-record snapshots are checked before import.
Only the same8 installed standard targets are accepted; Ubuntu AI, SSH and custom
rootfs aliases are excluded. Missing environments are disabled in the picker and
rechecked by each owned helper, with no fallback to Termux. Imported bodies are
quoted alias data, never executed to discover or apply them. Live sessions,
agent authentication, tunnels, desktop graphics and system packages are outside
the mutation path.

Alias edits and imports share one operation lock. Import progress blocks Back
and outside-tap dismissal. A private pre-import snapshot precedes helper writes.
Handled later helper failures compensate completed writes in reverse order;
compensation failure is explicit. Metadata commits follow successful helpers,
and persisted record values are checked before reporting success. Activity closure
allows owned work to finish, with a fresh metadata store if necessary. A process
or device crash between shell files and metadata is not an atomic transaction;
unconfirmed persistence reports partial state and retains recovery information.

## Device follow-up

No test2 GUI or real-environment import/export is claimed verified by fixtures.
The developer should export two aliases, preview/import them, cancel one replacement
then confirm it, and verify both target environments in newly opened shells (restart
the persistent Termux tab). Check Download access under actual Android permissions
and that guest editors never display Termux's restart hint. Existing terminal alias
state remains until the relevant shell/tab is reopened. Zsh was unavailable for
an actual Zsh run; registration and guard logic are reviewed, not device-tested.
