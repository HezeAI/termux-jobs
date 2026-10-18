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
