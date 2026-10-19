# Historical private build records — v1.5.22-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.22-test — validation

Private local build, not published. Production local-model Java classes, service,
native sources, tests/assets/catalog and local build helpers removed. Shared CLI
permission/settings UI, native Codex history management and shell keepHistory
remain. Current normal features retained rather than reverting the whole project.

Full final app JVM suite: **327 tests, zero failures/errors/skips**, including
legacy-local-profile filtering among ordinary/Codex/SSH sessions without resetting
commands/notes/recent history, keepHistory round-trip/copy, four selectable startup
environments, active-session highlighting/sizing, and exact cleanup boundaries.
The cleanup test uses actual filesystem links: neither a descendant link nor a
root local-ai link allows deletion of an outside project. Repeated cleanup is safe.
The unchanged emulator suite retains **145 passing tests**, reused as UP-TO-DATE
in this invocation. Logs/XML/test-counts.json preserved under
`.verification/remove-local-ai-v1522/`. Locale validation passes (584 strings).

ARM64 release build and packaged runtime audit pass. Required Guava/filesystem
classes present; all remaining optional missing types have previously reviewed
notes. Build still emits existing Kotlin metadata/toolchain warnings; successful
packaging/runtime inventory does not prove every optional code path at runtime.
Final ZIP CRC, duplicate/path checks, manifest/version/ABI, v2 signature and
limited private-key/token-name/payload scan passed. No LocalAi class or reference,
local service, llama/LiteRT/QNN payload, catalog or local instructions in APK.
Only libtermux.so and libtermux-bootstrap.so remain; both byte-identical to the
previous APK. Codex/OpenCode/Claude/Gemini control and server-agent assets are
byte-identical. Current production/staged Java logic matches (whitespace ignored).

Artifact: `Termux-Jobs-v1.5.22-test-arm64.apk`, **36496098 bytes** (~34.8MiB),
previous104918560 bytes (~100.1MiB): **68422462 fewer bytes (~65.2% smaller)**.
com.termux; versionName1.5.22-test; versionCode1159 >1158; minSDK26; ARM64;
release not debuggable. Certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK SHA-256:
`a354af9861d301bc83ce5f4d52574520680f70a1052c5067b49f444da6481bc1`.

Explicitly authorized local cleanup performed: only canonical inactive dedicated
jobs-local-ai container, with owner/ready markers and no nested kernel mounts,
removed; working ubuntu root inode/device identity unchanged. Its allocated size
was304125KiB. Repository-owned ignored local-ai tooling and cancelled LoRA
preparation removed (1016722KiB +4967924KiB). No model training resumed. Combined
allocated pre-delete sizes:6288771KiB (~6.0GiB); not an exact post-delete Android
storage-accounting measurement. Old research/validation docs remain historical.

Compatibility: general library stays schema8, old local-AI profiles skipped.
**First launch intentionally deletes retired app-private local-model/chat/config
files and cache/preferences**, including downloaded/imported copies. Outside
source model files are not traversed/deleted. Other histories/auth/project files
are not reset. Installed app-private files are not visible from this proot;
their actual removal is pending the first launch and is not claimed verified on
this device. APK does not automatically remove other proot containers.

Minimum developer device checks after update: no local-model entry/preset/history;
old local data storage freed after startup; open existing Codex/normal session and
its history/settings; type and switch tabs; open files and server connection/SFTP.
These Android input/gesture/data-migration interactions have not yet been checked
on this new APK. Removal is not proof of the cause or resolution of the earlier
ANR: no system ANR stack was available. No APK install or GitHub publication.


---

## Subsequent development record

# v1.5.22-test — file selection and copy/move destination actions

Private update from v1.5.21-test/code1183, not a GitHub release.
Session and server file browsers separate row opening from checkbox selection.
Long press enters selection, folder navigation preserves chosen paths and `× N`
clears them. Session browser parent arrow faces left; destination arrow below Home
opens Copy here / Move here / Back. Outside taps do not dismiss action/confirmation
menus. Conflicts require confirmation to allocate new names, retaining extensions.
No implicit overwrite/merge, recursive self-copy or overlapping tree selection.

Session copies stage per entry before no-clobber publication; moves use native
no-clobber mv. The operation belongs to the selected environment, independent of
terminal input. Whole-batch preflight runs again when executing. Main browser
normal dismissal is disabled during mutation, restored on completion/failure.
Completed entries remain on interrupted batches; there is no automatic retry or
rollback promise. Cross-environment destination picking was not introduced.
See [FILE-SELECTION.md](FILE-SELECTION.md) for behavior and limits.

Server download/SFTP/move transports remain unchanged. Selection can survive into
another folder on the same server. Row taps keep opening file menus even in delete
selection mode; checkboxes select. Delete confirmation includes exact source paths.
Delete requests are grouped by original parent and retain the existing server-side
safe dir-fd traversal; failure stops subsequent groups and reports partial status.
No server agent update, live server action or installed-app operation was performed.

## Automated verification

131 focused JVM tests pass with zero failures/errors/skips. Includes seven backend tests, with real
shell fixtures for recursive copying, native moves, names with quotes/shell
metacharacters/Unicode, symlink preservation, conflict preflight before mutation,
explicit conflict rename, APK extension retention, missing sources, dangling
collision links, canonical alias destinations, overlapping parent/child rejection,
and server deletion-group validation. Fixtures live under repository verification
temporary directories, not user projects or shared phone files.

UI tests exercise the actual browser dialog controls with listing data injected to
avoid launching installed environments. They verify long press, independent
checkboxes, file-menu availability while selected, directory/filter retention,
current destination in actions, Back retaining selection and the real `× N` clear
handler. Server tests cover navigation retention, selected outlines, checkbox-only
selection in delete mode, confirmation cancellation, pending moves, clear button, automatic last-checkbox exit,
adjacent server workspace/transfers/downloads, local pickers and text editor.
Existing AgentChatUi and cache tests also pass. Mock tests do not establish physical
IME/gestures/DPI or actual filesystem access from Android.

Code review traces independent picker modes, fresh browser identity/destination,
batch preflight, conflict races handled by no-clobber publication, staging cleanup,
partial failure/count reporting, UI busy state/dismissal recovery, server-profile
selection isolation, source-parent grouping and no automatic retry. Agent chat
integration sources match verified1.5.21 hashes; no AI controller asset changed.
No system dependency, installed Ubuntu, shared SDK or user process was modified.

Evidence: `.verification/file-actions-v1.5.22/` — tests-final-cancel.log, tests.json,
tested-source.json and baseline source copies. An initial launcher was expanded
while its process was still running; Gradle tests passed but the shell launcher
reported a trailing parse error. The final launcher/suite pass cleanly. Existing
Robolectric CloseGuard/Gradle filesystem-watching messages are retained in logs.

Device check: select a file and folder, tap through folders, uncheck one entry,
copy into a destination, repeat as a move, check a name conflict and clear selection.
Confirm action menus stay open on outside taps. On a server, verify folder opening
while selected, download/pending move, checkbox-only delete selection and `× N`.
No exhaustive device or real server transfer/deletion verification is claimed.

## Final artifact

Package com.termux, version1.5.22-test/code1184, minSdk26, ARM64 only.
APK `Termux-Jobs-v1.5.22-test-arm64.apk`, 36614894 bytes.
SHA-256: `797e5e3c9e5ed1f420e18ea325bcbecb20609d0d5ac0058c6069a1c06859d734`.
Same certificate as1.5.21/code1183, increased versionCode. No data reset or schema
migration. Final ZIP/ABI/v2 signature/private-file/secret-marker audits pass; release
stage matches tested sources. All packaged assets and native libraries match1.5.21
byte for byte, including every agent adapter and bootstrap. Assembly/runtime required
definition verification passes,31 optional missing types retain review notes.
Existing Lint Kotlin/UAST/JNA tooling warnings remain, not a clean-Lint claim.

Final evidence: build-final.log, staged-source.json, apk-audit.json,
audit-final.log, signature.txt, badging.txt, delivery.json. Download copy hash verified.
New stable release versionCode must exceed1184. No GitHub publication.
