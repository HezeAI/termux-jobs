# v2.1.2 — Agent skills, Claude history and Ubuntu AI setup

Manage Codex and Claude instructions and skills, prepare the shared Ubuntu AI from GitHub, and browse older Claude messages. This release also improves agent chat controls and animated local file previews.

**[Download APK](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.2/Termux-Jobs-v2.1.2-arm64.apk)**

## Changes since stable v2.1.1

- **Instructions and skills:** add Claude Code support alongside Codex, shared project memory in `LEARNING.md`, optional project instruction creation and session-scoped access. Import skills from public GitHub repositories, folders or ZIP files, or create them in the app. Preview the source and destination before confirming; app-managed skills support updates, conflict checks, rollback and removal.
- **Ubuntu AI preparation:** install a missing environment from a pinned, SHA-256-checked clean GitHub Components image without Docker Hub. All four agents share Ubuntu AI and add only missing components. Show preparation stages, measured download progress and redacted details in the window. Reopening joins the current operation; closing the window does not cancel it.
- **Agent controls:** improve instruction and skill manager scrolling, template preview/application, project file creation and RU/EN guidance. Place the paperclip above the star in a shared outline; keep Send beside the message field. Remove the separate command-without-model field and disable the interactive-input header control in agent chat. Keep ordinary terminal input and native terminal access. Make working icon animations clearer while retaining distinct states and Stop confirmation.
- **Claude history:** load earlier messages in bounded pages, preserve loaded history and the reading position during refresh, and open complete long answers through Show fully. Avoid duplicate messages when reopening or continuing a conversation. Native history files remain read only.
- **Local file previews:** open links and cards through the same viewer in the originating chat environment for all four agents. Support HTML animation/JavaScript controls and animated GIFs, explain opening errors and keep original-file downloads. HTML has no application command bridge or arbitrary environment-file access.
- **Project information:** clarify the independent unofficial fork, include required third-party notices in About/the APK, and document data flows, signing compatibility and the scope of license/source checks.

## Requirements and update compatibility

- **ARM64; Android 8 or later. X11 requires Android 11 or later.** Agent component readiness does not establish account authorization or model availability.
- Compatible updates require the package ID and signing certificate to match a previous **Termux Job’s** installation. This release retains `com.termux` and increases `versionCode` to 1476. Compatibility over official Termux from GitHub, F-Droid or Google Play is not verified; matching `com.termux` alone is insufficient.
- No data reset, incompatible settings migration or environment reinstallation is required. Existing Ubuntu installations, CLI versions, projects, history, custom instructions and legacy environment bindings are preserved. New AI Agents sessions default to Ubuntu AI. Incomplete or incompatible environments are reported rather than silently deleted.
- Finish important work before updating and keep a suitable backup. Uninstalling can delete private data and is not a normal update step.

## Important limits

- Restart an idle agent explicitly, with conversation continuation, to load changed adapters. Saving instructions or installing skills does not prove that a running agent has loaded them; sessions are not automatically restarted.
- Third-party skills can guide agent actions. Import does not execute their scripts or install dependencies. Private GitHub repositories and a broad online catalog are not included.
- Claude history has a 32 MiB per-record safety bound. Unresolvable cross-file parent links cannot be reconstructed automatically. Keyboard/layout, animated previews and scrolling on additional phones remain separate device checks.
- No independent security audit is confirmed. Signature checks and test counts are not safety guarantees. The [security document](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/SECURITY.md) explains data flows and agent access; the [license/source review](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/docs/fork/LICENSE-REVIEW-2026-11-03.md) records verified coverage and unresolved Components questions.

See the [instructions guide](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/docs/fork/AGENT-INSTRUCTIONS.md) and [skills guide](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/docs/fork/AGENT-SKILLS.md) for usage.

<details>
<summary>Technical verification, CI history and artifact details</summary>

### Published release APK — local checks

The signed **release** APK was built and checked locally. Checks covered full modern Lint of five modules (0 errors, 234 warnings), final ZIP integrity, package/version, ARM64 ABI, disabled `debuggable`, signature/certificate, runtime/DEX/native/assets and SHA-256. These are separate from debug CI and from checks on a phone. See the [release validation report](https://github.com/HezeAI/termux-jobs/blob/v2.1.2/docs/fork/VALIDATION-v2.1.2.md).

The APK and accompanying files were downloaded publicly after publication. Their hashes and `update.json` were checked against the final APK. The documentation/CI follow-up did not replace the APK, tag, `update.json` or existing Components binaries.

- Release source/tag commit: [`c0073b462311bba5fd2dcc301b048d69e407f3f1`](https://github.com/HezeAI/termux-jobs/commit/c0073b462311bba5fd2dcc301b048d69e407f3f1), tag [`v2.1.2`](https://github.com/HezeAI/termux-jobs/tree/v2.1.2). Later documentation/CI commits in `fork/base` do not move that tag.
- Version: **2.1.2**, `versionCode` **1476**; workspace schema **8**.
- APK size: **40,452,595 bytes**.
- APK file SHA-256: `2f8b97b2d9083a5b7dcc3966eca54ea6d843fec3e253ba6e279c123921eeddab`.
- Signing-certificate SHA-256, separately from the file hash: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Published [SHA256SUMS](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.2/SHA256SUMS) and [update.json](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.2/update.json).

### Debug CI — separate source/build checks

CI uses `testDebugUnitTest` and `:app:assembleDebug`. Debug success does **not** verify or reproduce the published signed release APK. Debug APKs are internal Actions artifacts, not stable release assets; issuing a debug APK is not required for each release.

The [original release-source run](https://github.com/HezeAI/termux-jobs/actions/runs/41578152018) failed at `:x11:verifyX11Inputs` because its workflow did not prepare the pinned inputs. That original run remains failed. A [separate successful run](https://github.com/HezeAI/termux-jobs/actions/runs/41581707240) checked the unchanged release source `c0073b462311bba5fd2dcc301b048d69e407f3f1` with corrected workflow commit [`f343be8fa8edbcc700bd3c34977d2d854503281a`](https://github.com/HezeAI/termux-jobs/commit/f343be8fa8edbcc700bd3c34977d2d854503281a). The [workflow-commit source run](https://github.com/HezeAI/termux-jobs/actions/runs/41581706401) also succeeded. A new successful check does not change the historical failed result.

Each successful run reported **1,598 JVM tests: 1,590 passed, 8 conditionally skipped, 0 failures/errors**, plus **4 passed preparation-helper tests**. The skipped SFTP cases require a Termux-specific subsystem path absent on the hosted runner; they are not CI passes. Pinned X11 verification, localization checks and debug packaging passed. No application or test source was overlaid onto the release checkout.

Fresh hosted x86_64 runners had no developer X11 caches. Ready-made X11/codecs/style/keyboard inputs retained their pinned hashes. Two ARM64 compatibility shims were compiled from the tested C sources with **NDK 22.1.7171670 / Clang 11.0.5**. **SDK 35** uses compatible **AAPT2 from build-tools 35.0.0**. Source/workflow identities, input hashes and compiler/source/output hashes are in the runs’ `verification-reports` artifacts. See [CI preparation details](https://github.com/HezeAI/termux-jobs/blob/f343be8fa8edbcc700bd3c34977d2d854503281a/docs/fork/CI-X11.md) for inputs and build parameters; the release validation report records local results separately.

### Device observation and component sources

HezeAI confirmed Claude working on another phone after switching Ubuntu AI installation to the GitHub image. This developer observation is distinct from automated tests and does not establish the cause of the earlier Docker Hub refusal.

Ubuntu AI uses a private **proot-distro 5.9.0** local-archive installer; the system installation is not downgraded. The separate [Ubuntu corresponding-source archive](https://github.com/HezeAI/termux-jobs-components/releases/download/ubuntu-ai-base-arm64-20261102/ubuntu-ai-corresponding-sources-20261103.tar) is not automatically downloaded by the app. The license/source review describes its exact package coverage and remaining questions; it is not a blanket compliance certificate.

</details>

AI development assistance: OpenAI Codex
