# test32: Ubuntu AI preparation

Date:2026-11-02. VersionName **2.1.1-test32**, versionCode **1467**. Package **com.termux**, ARM64 only, nondebuggable. Signer preserved: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Final APK: `/sdcard/Download/Termux-Jobs-test32.apk`, **43,522,843bytes**.
SHA-256: `e585d9d3aa0a3ec858c7e667ba83e29107b52b6725b2e390014b50ffc29af127`.

## Final behavior

- New Ubuntu AI installations download the clean Ubuntu26.04.1 LTS ARM64 OCI archive from the separate [Components release](https://github.com/HezeAI/termux-jobs-components/releases/tag/ubuntu-ai-base-arm64-20261102). Archive41144320bytes, SHA-256 `c08828893609894e5ab6d80f146961961148afde007ce904292418d8fcd011e0`. The ordinary-environment downloader supplies byte progress, integrity validation, cache and resumption.
- A separate bundled upstream proot-distro5.9.0 source archive is checked against SHA-256 `f3b14ee9e1743b79eee8942366434128bab73014b903504bf0e321f8efa46b85`. The restricted launcher accepts only the reviewed local image and `--name ubuntu-ai`; it never replaces system proot-distro. The local-archive branch does not contact Docker Hub.
- Existing Ubuntu AI is checked and reused before any image download. An incomplete or unopenable environment fails separately; it is not deleted or rebased automatically. Four agents share that same alias. Existing CLI recipes and versions remain: Codex fallback0.163.0 (existing compatible Codex retained), Claude2.1.296, Gemini0.64.0, OpenCode2.0.28, private Node22.22.1 and optional Google Cloud CLI589.0.0.
- Missing components follow the existing recipes. An existing incompatible OpenCode is preserved and reported rather than silently replaced. Claude/Gemini version checks use the recipes' disposable probe profiles. User accounts, projects and history are not migrated.
- The explicit launch-environment check runs a shared serialized preparation job. Reopening an active job joins it; closing its dialog releases UI observers without cancelling package operations. Stages, measured image bytes/percentage, indeterminate stages and a redacted expandable log appear in that window. The current bounded log is stored under `.agent-preparation/operations/<environment>-<agent>.log` in app files. Per-session status is small and atomic.
- Preparation does not execute the chat controller, a model request or sign-in. Vertex component-only mode skips Google authorization; authorization is shown separately and proceeds through the normal agent launch. A CLI process/version check does not prove account or model access.
- The first-launch confirmation invokes the same preparation operation when components are missing. Normal launch retains the existing connection/permission/history architecture and component recipes. Preparation notices replace the generic missing-control-channel message in the terminal while applicable.
- The AI Agents form no longer offers the agent-environment selector. New forms use Ubuntu AI and show the agreed RU/EN explanation. Existing ordinary-Ubuntu and direct-Termux Codex bindings are displayed accurately and survive saving. Ordinary terminal and SSH forms retain their behavior.

## Checked anew

1. **84 JVM tests**, including preparation parsing, four-agent routing, one active job per agent/environment, serialization, close/reopen, explicit retry, broken-environment refusal, secret redaction, measured progress, terminal UI and form save behavior. RU/EN tests open/save existing bindings, confirm the explanation is not clickable and verify the new AI default. These are Robolectric/model checks, not phone acceptance.
2. **52 Python unit tests**, plus the existing isolated agent-preparation shell fixtures. Coverage includes first absence, second agent/repeat without another image download, existing synthetic files/CLI preservation, incomplete installation refusal, consent, resume/range/checksum errors and installer supervisor ownership/cancellation. Added component-only Vertex case confirms no auth command invocation. Syntax checks passed for affected shell helpers.
3. **Real download through the application's downloader from the published GitHub asset into a fresh isolated cache**, complete byte count and SHA-256 verified. Old component tags/assets were not replaced.
4. **Real public install API of the bundled5.9.0**, on an initially empty isolated prefix: every OCI blob/config/manifest digest validated, linux/arm64 confirmed, both layers extracted, resolver/hosts/Android IDs initialized, Ubuntu26.04.1 LTS and Bash/APT files verified. Network calls were forbidden for that archive installation. This deliberately does **not** claim execution through the native CLI entry point or a guest process. The downloaded asset is byte-identical to this API-tested archive; identity proof, not another installation, connects that evidence to the new source.
5. Full modern **Lint7.4.2**, all five modules: **0errors,235warnings**. Existing compileSDK35/tool compatibility warnings remain; no new blanket suppression or baseline.
6. Final delivered APK: ZIP integrity, package/version/ABI/nondebuggable, v2 signature and previous signer, SHA-256, DEX/runtime checks and all packaged source assets.14,145defined classes; no required missing runtime types. Source `.pyc`/`__pycache__` are excluded from the APK. The initial asset-comparison script incorrectly included those generated source-cache files; corrected to the existing packaging exclusions, with no product bypass.
7. Installed system proot-distro pull/transport sources still match the original diagnostic hashes. The pinned launcher neither invokes pip nor overwrites the system executable. Hermetic tests also assert their system-installer fixture remains byte-identical.

## Reused with identity evidence

- Existing SDK/JDK/Gradle and dependency caches, without full copies.
- The two Termux native libraries from verified signed test31; matching native source hashes and final byte-for-byte equality are recorded in `native-reuse.json` and `artifact-content.json`. Pinned libXlorie is likewise byte-identical. No native/driver source changed; this is packaging reuse, not a fresh graphics run.
- Unchanged agent protocol/controllers, persistence and other runtime assets are compared against test31 in `changes-vs-test31.json`. Their previous functional evidence does not prove new native preparation or another device's network state.

## Device confirmation reported by the user — 2026-11-02

The user confirmed that **Claude worked on the other phone after switching Ubuntu AI installation to the GitHub-hosted image**. This is a user-reported device check of that scenario, separate from the automated checks above. The exact APK version and detailed installation/authorization steps were not supplied with this confirmation.

This result does not establish the cause of the earlier Docker Hub refusal. That cause remains **undetermined**. It does not confirm the other three agents, interruption/retry, preservation of an existing environment or all UI/background scenarios on that phone.

## Not checked / remaining device acceptance

**Full native Termux installation and first CLI launches of all four agents remain unverified here.** The tool session is already inside proot, and the genuine CLI rejects nesting. That safety check was not disabled. The install-API extraction above is distinct from a native CLI/guest run; no manual rootfs workaround is called a successful app installation.

Beyond the specific user confirmation above, separately documented checks are still required for fresh Termux bootstrap/prerequisite package transactions, native local-archive CLI installation, real guest APT/component transactions, each agent's first launch, a second agent in that environment, repeat launch, interruption/retry, and system5.10.3 login against the newly created rootfs. UI acceptance must check narrow/large-font layouts, progress/detail readability, close/reopen and Android background behavior. Closing a dialog is covered; recovery from Android forcibly killing the whole application is not a promise of process survival.

The other phone's original auth/manifest HTTP refusal is not reproduced or explained by this change. Pinning5.9.0 and mirroring the image are selected distribution policies, **not a proven fix for that network failure**. APT, CLI and Cloud SDK sources still require their own network access.

## Evidence and initial failures

Compact evidence lives in `.verification/ubuntu-ai-pinned/`: final source identity, bundle provenance, local-image verification, GitHub download, native reuse, Python/JVM logs, Lint summary, artifact/signature/badging/runtime records. Earlier compilation errors (observer inserted into the wrong method, ambiguous Process) were fixed before delivery. A close/reopen test initially asserted before the queued dismiss callback; the fixture now drains the UI loop. Initial catalog fixtures were updated for the new tenth image. Original failed logs are retained, not relabelled PASS.

Only the component image and its public documentation were published. No APK publication was part of this preparation; subsequent device evidence is attributed above. Working sessions, connections, installed environments, authorization and common caches were not stopped, reset or cleaned. Existing data formats remain unchanged; no migration or new data rollback mechanism was added.
