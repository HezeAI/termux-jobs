# Termux Job’s 2.1.9-test5 validation

Private ARM64 build, 2026-11-21. Developer: HezeAI.
AI development assistance: OpenAI Codex.

## Antigravity startup and update

The developer's test4 screenshot reports `CLI_STARTUP_FAILED` through its
localized text with exit code 1 and `agy-cli` still missing in Ubuntu AI.
The official Linux ARM64 1.5.13 archive matched the checked-in Google release
pin. On Ubuntu AI, `agy --version` exited 0 with 7 bytes on stdout.
`agy --help` also exited 0 and wrote all 3,319 bytes to stderr. Both production
checks previously discarded that stream and rejected the CLI.

The first-install and update probes now capture both streams for Antigravity
help. Version probes still read stdout. The checks still require successful
exit, bounded output and the expected capabilities before activation. Ubuntu
AI preparation inventories Python 3 for the updater and includes it in the
normal consented package preparation when missing. Archive hashing uses a
bounded read size and works with older Python 3 versions.

The new Gemini form opens **Update CLI** before the profile exists. The same
existing updater still serves **Chat management** once the chat exists.
Installation requires confirmation. Managed selection applies on the next CLI
launch. The change leaves saved profile formats and permissions intact.

Google documents conversation selection via `/resume`, model selection via
`/model`, and quotas via `/usage` in the interactive terminal. Its headless
stream soft-denies actions that need interactive approval. In test5, the app
still displays Antigravity defaults in its creation form and uses the CLI's
terminal for interactive approval and those commands. The Android composer has
no Antigravity catalog or quota reader. [CLI reference](https://antigravity.google/docs/cli/reference/)
and [headless protocol](https://antigravity.google/docs/cli/headless/).

## Checked anew

- Verified the official archive SHA-512, executable ARM64 ELF ABI, glibc loader
  and dependency closure. With a private home and a self-tested syscall guard,
  the exact startup and update probes failed before the fix and passed after.
  The stream argv with `--mode default` reached listener startup. The guard
  blocked its socket. See [startup evidence](../../.verification/agy-startup-real/probe.log).
- Offline bootstrap and updater tests covered missing Python, preparation
  consent, download failure, digest checks, missing or oversized help, a
  nonzero help exit, first managed install and version activation. The protocol
  fixtures covered sign-in handoff and retries, history and owned process stop.
- Verified 656 source inputs by file list and bytes across release, JVM and
  Lint stages. RU/EN resource consistency passed for 587 workspace keys.
- All 23 targeted JVM tests passed. Modern Lint found 0 errors and 298 warnings.
  ARM64 release assembly completed in 3m 55s.
- The final APK passed ZIP, package and signing metadata, DEX dependency,
  native ELF/ZIP alignment and private-file checks. The three changed assets
  matched source bytes. The remaining assets and three native libraries
  matched test4. Installation and live provider acceptance are pending.

## Artifact

- `/sdcard/Download/Termux-Jobs-v2.1.9-test5-arm64.apk`, 40,688,015 bytes.
- `com.termux`, 2.1.9-test5 / 1545, arm64-v8a. Signer and SDK levels match test4.
- SHA-256 `1e3db413529751c5a4400c623d80c7167918fb1022734e6834c4779941f443aa`.
- [Assembly and validation logs](../../.verification/test5-antigravity/build.log).
