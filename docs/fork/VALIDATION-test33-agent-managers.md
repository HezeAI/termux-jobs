# test33 — instruction and skill managers

Date:2026-11-02. Final file: `/sdcard/Download/Termux-Jobs-test33.apk`.

| Property | Verified value |
| --- | --- |
| Package | `com.termux` |
| versionName / versionCode | `2.1.1-test33` / **1468** |
| Bytes | **40,423,501** |
| SHA-256 | `ae067adf0a6a78afe4fde7b9c16fe7c2a0d06f8034c737570131d5dcd1e82fcf` |
| ABI / debug | ARM64 only / nondebuggable |
| Signature | v2 verified; previous certificate retained |
| Certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |

The versionCode exceeds delivered test32/1467 and the previously issued backup1451. Package and signer match test32. User-data formats are unchanged; no data migration, automatic instruction replacement or environment conversion is included.

## Included changes

“View app template” replaces the instruction editor’s environment selector. Its bounded, scrollable preview starts in the app language and permits RU/EN inspection. It has no Apply action and does not alter the file, draft or app language. Explicit “Apply app template…” remains separately in the menu, preserving destination disclosure, history and conflict checks; duplicate review actions were removed.

Instructions and skills default to Ubuntu AI. Legacy agent environments and ordinary-Ubuntu context are retained and explained. Requests and project browsing use the displayed environment, including a native helper route for legacy Codex in Termux. Unsupported Claude-in-Termux instruction editing reports its limitation instead of redirecting. No instructions are copied from ordinary Ubuntu into Ubuntu AI. The helper assets use the new `instructions-1468` folder; saved instruction files are independent of that versioned code folder.

## Checked anew

- **73 JVM tests**, rerun after the version increase: instruction/helper routing, both managers and common layout. Coverage includes RU/EN read-only viewing for both agents with exact draft preservation and zero writes, separate explicit Apply, legacy read/save environment binding, skills list/mutation routing, project profiles, imports, history, restore, conflict and draft guards. Compilation passed. These are model checks, not phone interaction.
- **92 Python tests** for instruction storage, Claude file semantics and skills. They operate on isolated fixtures and cover actual fixture-file atomicity/history/security, without working environments or authorization files.
- Full modern **Lint7.4.2** across all five modules: **0errors,234warnings**. Existing AGP/compileSDK35 compatibility warnings remain; no new blanket suppression was added. Lint report infrastructure tests also passed.
- Exact final APK: ZIP integrity, package/version, ARM64/nondebuggable, signature/certificate, SHA-256 and runtime scan. **14,142defined classes**, no required missing types; existing reviewed optional-type handling remains. All **117assets** match current source bytes. Native payloads match test32 byte-for-byte. No `.pyc`, `__pycache__`, keystore or verification-directory entries are packaged.
- Delivered APK bytes match the checked Gradle output. Checksum sidecar is `/sdcard/Download/Termux-Jobs-test33.apk.sha256`.
- Reviewed new routing and preview paths: viewing has no commit, agent switching retains runtime and uses the existing draft guard, save/import retain destination/revision checks, and native file helpers remain separate from working chat processes.

## Reused with identity proof

Existing local SDK/JDK/Gradle and dependency caches were reused; no full SDK or environment copies were made. Native/X11 inputs were compared with the test32 source inventory. The two Termux native libraries were read from the verified signed test32 APK; all three final native libraries, including pinned libXlorie, are byte-identical to that baseline. Inputs and payload hashes are in `native-reuse.json` and `artifact.json`.

Ubuntu AI image preparation, drivers, terminals and agent protocol behavior outside the manager routes were not rerun: their significant sources/payloads are unchanged, as recorded in `changes-vs-test32.json`. This preserves the limits of test32; it is not fresh evidence of installation, graphics or remote-network behavior.

## Not checked / phone acceptance

The developer will check windows after installation. Narrow-screen appearance, large fonts, keyboard, scrolling, actual preview controls and legacy-environment helpers need phone acceptance. The models do not prove those interactions or custom launcher/CODEX_HOME behavior of a specific active process. No new agent conversation, environment installation, X11/driver launch or working-session restart was performed for this packaging task.

Subsequent user confirmation on2026-11-02: Claude worked on the other phone after switching to the GitHub-hosted Ubuntu AI image; see the [device-check addendum](VALIDATION-test32-ubuntu-ai.md#device-confirmation-reported-by-the-user--2026-11-02). The exact APK version was not specified. The earlier Docker Hub refusal remains unexplained, and full native four-agent clean-install coverage is still incomplete. This APK was not published. The existing test32 APK and older evidence were preserved.

## Evidence

`.verification/agent-manager-test33/`: scope, source identity, changes versus test32, build/JVM/Python/Lint logs, test summary, native reuse, badging, signature, runtime scan and artifact record. The source-only implementation report remains [historical](VALIDATION-agent-manager-environment.md); the final behavior is described in [instructions](AGENT-INSTRUCTIONS.md), [skills](AGENT-SKILLS.md) and [CHANGELOG](../../CHANGELOG.md).
