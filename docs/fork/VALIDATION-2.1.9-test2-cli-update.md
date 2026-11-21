# CLI update work log and validation

Private Termux Job’s 2.1.9-test2 / 1542, 2026-11-20.
Developer: HezeAI. AI development assistance: OpenAI Codex.

## Read and traced

- Checked the project root and Git status. Consulted the [project rules](../../AGENTS.md),
  [memory](../../LEARNING.md), [handoff](../../CONTINUE.md), [verification policy](TESTING.md),
  [build notes](../../FORK.md), [Lint procedure](LINT.md), [release rules](RELEASING.md),
  and [Download diagnostics](DIAGNOSTICS.md).
- Traced the sidebar and the header restart, Ubuntu AI profile binding, the
  preparation transport, both bootstrap scripts, and the CLI readers for Codex
  history and agent instructions and permissions.
- Recorded the plan in `.verification/agent-cli-update/PLAN.md`. Developed the
  Android dialog and guest updater in parallel, then reviewed their interface and
  assembled the APK from staged sources.

## Implemented

- Added **Chat management → Update CLI** for Codex and Claude Code chats bound
  to Ubuntu AI, including stopped chats. The dialog shows the version selected
  for launch and the latest stable release. Checking and confirming installation
  are separate actions. Closing and reopening the dialog preserves the operation.
- The updater uses the official npm ARM64 package, rechecks the confirmed version,
  verifies SHA-512 and archive member types, checks ELF ABI, and runs isolated
  version and launch-flag probes. A lock and atomic switch preserve the previous
  selection on failure. Old version directories remain available.
- Connected both launchers and the relevant reads to the version-specific path.
  The live Codex control proxy keeps its independent Node lookup. Existing
  launch preparation handles missing Python with consent. The installed
  proot-distro is invoked through its own shebang. The managed Codex update
  omits the optional upstream voice subtree.
- The APK ships updater code. It does not contain newer agent binaries. An
  installation affects future launches for that agent in Ubuntu AI. A running
  chat can apply it through the separate restart button in the top bar.

## Checked anew

- Four JVM target, command and protocol tests and two Robolectric interaction
  fixtures passed. Checked confirmation, stale profiles, busy state, retry and
  closing/reopening an active operation with fixture transport.
- All six environment routing and profile tests passed in the Robolectric
  fixture, including the separate history and live Codex control paths.
- Offline backend fixtures passed archive path, type, duplicate and size checks,
  digest and origin checks, bad ELF rejection, stale and busy update responses,
  atomic activation, prior runtime preservation and Ubuntu AI-only selection.
- Checked, installed and checked again **Codex 0.171.0** and **Claude Code 2.1.330**
  under isolated homes on Ubuntu/aarch64 with verified official archives.
  Both ran the native version and launch-flag probes. Separately, Codex
  app-server answered initialize and thread/list over stdio in an empty fixture.
- Existing preparation fixtures, six Codex and Claude Node suites, and 46
  instruction-helper tests passed. The latter use the existing test-only
  bridge to real linkat for this Android Python. Localization passed for 587
  workspace strings and app/shared settings keys and format arguments.
- Full signed ARM64 assembly passed with local Gradle 7.2, JDK 11, SDK 35 and
  compatible AAPT2. The 662 staged source inputs matched the checkout.
  Modern Lint 7.4.2 reported **0 errors and 301 warnings**. The app was reanalyzed
  and unchanged dependency module checks were reused. The final Python asset
  changed after Lint staging and was verified separately.
- Final APK checks passed for ZIP integrity, package, versions, SDKs, ABI,
  debuggability, unchanged declared Android permissions, signature, DEX, assets
  and private path exclusions. All 408 stored ZIP offsets aligned. All three
  ARM64 libraries are identical to the preceding APK and satisfy 16 KiB ELF
  LOAD alignment. The DEX inventory found 14,667 definitions. All 31 unresolved
  types have existing review notes. Verified the copy in Download by SHA-256.

## Repeated or diagnosed

- Confirmed the active proot-distro entry point is a Python script. Corrected
  the host invocation before the final Android build and verified argv handling.
- The first Robolectric attempt could not resolve the APK resource IDs in
  legacy mode. Recompiled the fixture dialog with the existing generated R and
  reran both scenarios successfully.
- The environment test runner initially needed the local Robolectric ProxyMaker
  compatibility class on JDK 21. Its second run reached the tests and found an
  overlong fixture socket path. Reran with the existing short project test
  directory and verified all six cases. Both failed attempts are retained.
- The Codex launcher fixture inserted the checkout path's apostrophe into
  a shell trap, leading to a timeout. Used its isolated HOME instead, preserved
  the first failure log and reran the regression suites successfully.
- Repeated installation of the verified official archives in fresh fixture
  homes after adding the final ELF and launch-flag checks.
- The old build plugin emitted legacy Lint rule-loading and Kotlin metadata
  diagnostics. The independent modern check passed and is the applicable Lint
  evidence. The existing SDK 35 support warning remains.

## Reused and not checked

Reused the local toolchain/cache and verified native binaries. Native sources,
build inputs and the packaged libraries match 2.1.9-test1. The device dialog and
actual APK update remain unverified. Live provider sign-in and model responses
were outside the local fixtures. A manually installed Codex npm launcher that
requires a conflicting older Node can fail the isolated version check. The same
package and signer are preserved, with no data schema migration or reset.
This private artifact does not update the public channel.

## Artifact and evidence

- APK: `/sdcard/Download/Termux-Jobs-v2.1.9-test2-arm64.apk`
- Package: `com.termux`, min SDK 26, target SDK 28, ARM64, 40,710,729 bytes.
- SHA-256: `0b595cb0867980a0dee4ed579a31dac29b3cf9cd261973d57a5e131de3227444`.
- Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Raw checks: `.verification/agent-cli-update/`, including the build and
  modern Lint logs, APK metadata and DEX checks, unit and regression logs, and
  archive digest, ELF and version evidence in `registry-*/`.
