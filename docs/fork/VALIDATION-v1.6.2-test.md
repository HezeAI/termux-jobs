> Historical private checkpoint. Current stable evidence: [v1.6.5](VALIDATION-v1.6.5.md).

# Private v1.6.2-test — large-history startup fix

Package com.termux, versionName1.6.2-test, versionCode1194 (>1193).
Android8+/ARM64; same certificate, schema8, no migration/reset.
Published stable remains1.6.0.

## Reproduction and checks

- Read-only phone snapshot confirmed disconnected controller with history-load
  failure. Working sessions and installed helpers were left untouched.
- Native Codex0.163.0 on a separate copied history: eight-turn response2,898,482
  bytes exceeded old2,097,152-byte limit; one-turn response142,233 bytes.
  Copy was removed after checking. No account/model API was used.
- New wire regression: split3 MiB response accepted for chat, refused by default
  clients;33 MiB header refused; repeated failure emits once.
- Adapter fixtures: one-turn pagination, history disconnect cannot set ready or
  permit sending, fixed size diagnostic; existing approval/dedup/model/Shell/
  streaming/history/owned-process fixtures pass.11 permission RPC scenarios and
  status/limits fixtures pass. Native loopback provider fixture passes two turns.
- Source review: no credential/config/history edits, new network endpoints or
  shutdown changes. Only chat opts into32 MiB; default clients retain2 MiB.
  Android snapshot/detail/cache limits are unchanged. Startup/error/pagination/
  close paths inspected; previous grouping behavior retained.
- Isolated signed release assembly succeeds. Existing Kotlin/UAST/JNA Lint
  tooling diagnostics remain; this is not a clean full Lint result.
- ZIP integrity, package/version, minSdk/ABI/non-debug, signature and SHA pass.
  Runtime:13,113 classes, no required missing types,31 reviewed optional types
  against actual compileSdk35. Class definitions/resources/native libraries match private1.6.1;
  generated BuildConfig contains the new version.
  Only codex-chat.cjs and codex-control.js assets differ from stable1.6.0.
- APK scan found no private authorization files, signing keys or tested secret
  markers. Packaged modified helpers match source; unrelated assets unchanged.

## Artifact

`Termux-Jobs-v1.6.2-test-arm64.apk`, 36665902 bytes.

SHA-256: `dab6c2453074500a01195c455b05cc7cf7b0bb6952920fd63c8d80cc369e4c49`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

APK and checksum delivered to Download; destination hash verified.
Evidence: `.verification/codex-startup-fix/`.

## Phone verification pending

Update, reopen the same Codex history, send a message and a Shell command,
then check grouped-card expansion and Previous messages. Android installation
and actual UI reconnect on the phone are not established by the automated checks.
One native turn loads per history page; a message over32 MiB still fails with a
size diagnostic. Extremely large output remains subject to existing UI/cache bounds.
