# v2.0.9 validation

## Final artifact and compatibility

- File: `Termux-Jobs-v2.0.9-arm64.apk`; **39798069 bytes**.
- Package `com.termux`; versionName **2.0.9**, versionCode **1417**.
- SHA-256: `79e98ea329a52158798d420924f41bd14285b92c39841358891b9c5128de69cd`.
- Signing certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ARM64 only; min26/target28/compile35, Android8+ app / Android11+ X11.
- Same package and signing identity as v2.0.8 and previews through code1416.
- Schema8 is retained, with optional aliases and per-session chatScale. Old
  libraries load without aliases; old sessions default to100%. No data reset,
  account migration or environment reinstallation is required. Existing commands,
  notes, histories, presets, projects and installed environments are retained.

ZIP integrity, non-debuggable flag, package/version, ABI and signature pass.
All79 source assets match the final APK. All3 JNI libraries and X11 runtime
assets match v2.0.8. Runtime verification finds13,851 defined classes,31 reviewed
optional/framework references and no new unreviewed missing types. Bundled-file
and configured asset/DEX secret-pattern scans find no private credentials.
This is bounded static verification, not proof of every API/device path.

## Relevant automated checks on release source

- **228 JVM cases /25 classes**, zero failures/errors/skips in one release-source
  run. Coverage includes alias target validation, shell integration, replacement
  and rollback, import plans, persistence, backup/navigation, measured chat
  geometry50/100/150, streamed row identity, Markdown/code/table/cards,
  independent session scales and shared-composer restoration.
- **44 Python backup tests** pass: literal parsing without sourcing/expansion,
  rejected dynamic/chained definitions, quoting/multiline roundtrips, explicit
  target binding, one-environment shell export and real Bash declaration checks.
- **8 shell installer fixture tests** pass: managed writes/deletion, startup
  guards, no-follow boundaries and inert loader behavior.
- Required `scripts/verify-modern-lint.sh` passes across all5 modules with
  **0 errors /220 warnings**, no rule-loading/infrastructure errors or blanket
  suppression. Warnings retain reviewed API/tooling, resources, accessibility
  and UI text findings; two intentional percent/dropdown concatenation findings
  were added by the final preview. CompileSDK35 remains beyond the pinned
  verification AGP's tested range; targetSDK28/package separation remains
  architectural work rather than a claim resolved by this release.
- Final APK/runtime/assets/privacy checks and updater generation pass.

## Code and state review

Alias writes are limited to eight supported installed terminal targets. Ubuntu
AI, SSH and custom rootfs targets are refused; missing rootfs cannot fall back
to Termux. Foreign shell files are read as literal data, never sourced/executed.
Selections/full previews and replacement confirmation precede writes. Imports
recheck source and destinations, use owned helpers and compensate completed
steps on failure; persistence/partial-failure errors are explicit. No automatic
scan of existing shell aliases is introduced. Import is not a cross-process
crash-atomic transaction; private recovery snapshots remain available.

Chat scale is presentation/workspace metadata only, bounded to50–150%. It does
not change agent request snapshots or restart processes. Shared composer geometry
restores on chat/terminal/session transitions and close. Drafts/selection/reading
anchors retain their existing ownership rules. X11, graphics components,
permissions, process shutdown, tunnels and services are unchanged.

## Device evidence and remaining limits

The developer approved publication after the private preview. Automated fixtures
are separate from physical Android verification; no exhaustive phone/device or
fresh-rootfs claims are made. Readability with system font/display scaling, all
four live providers, IME/selection, storage permissions and Zsh need separate
device-specific checks. Real Bash was checked; real Zsh was unavailable locally.
Existing manually defined aliases are not probed for collisions. Literal shell
import intentionally skips dynamic/unsupported statements and reports them.

Aliases load in new shells. Existing shells retain loaded definitions until
reopened; the persistent Termux tab explicitly needs a restart after addition or
deletion. Finish important work before updating. The localized updater records
compatibility and no-reset requirements before installation.

## Publication verification

Published as the latest stable release at
https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.9, source tag
`81e9dad958652b31b7877e205e5c830e026a71fc`. APK, update.json, SHA256SUMS and
validation.json were downloaded and compared byte-for-byte with local files.
Updater size/hash/version match the final APK. The public release is neither
draft nor prerelease; its body matches the prepared English notes and tagged
documentation links resolve. Older tags and assets were not modified.
