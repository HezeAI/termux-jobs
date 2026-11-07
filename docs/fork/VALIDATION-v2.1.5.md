# v2.1.5 release validation

Prepared 2026-11-07. Compared with published stable v2.1.4, not a test APK.
The developer authorized publication after mandatory checks. No APK was installed
and no working agent or terminal was restarted during release preparation.

## Final signed artifacts

| Artifact | Version / code | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| New interface | 2.1.5 / 1520 | 40,627,301 | `6b093a90ae7e23e4576efdf86d100fe5979508c404b805ac2899896f2f8dd33e` |
| Manual old-interface rollback | 2.1.5-rollback / 1521 | 40,533,234 | `13e2c402ea3655fea3d42b6a1c9bc1750277ea1ebc8a23404b94c9fce338a3cb` |

Both: `com.termux`, ARM64 only, non-debuggable, valid ZIP and v2 signature.
Certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
The package and signer match the existing stable release. Main source tag is
`v2.1.5`; rollback source tag is `v2.1.5-rollback`.

The updater manifest points only to code 1520. Rollback is a separate manual asset.
Both use schema 8. No data reset is required for the main update. Before rollback,
disable Auto/Bypass in **every saved Claude profile**: the older validator rejects
those values. Actual installation, rollback and data retention on Android were
not verified. A later main update must exceed code 1521. Reserve1499 and previous
APKs/releases are unchanged. Do not uninstall or clear data to work around version checks.

## Checked anew — signed release APKs, local

- Java/resources/assets were rebuilt for both release variants. Final-file audits
  checked version, package, signer, ABI, debug flag, ZIP, DEX/runtime references,
  required assets/native payloads and accidental private-file patterns.
- Main APK matches 127 packaged assets; rollback matches 124. All seven ELF
  payloads match the existing stable inputs. No third-party component was updated.
- Full Modern Lint 7.4.2 analyzed all five modules and dependencies for both trees:
  **main: 0 errors, 281 warnings; rollback: 0 errors, 250 warnings**. Infrastructure
  diagnostics are empty. Retained warnings include deprecations, localization,
  accessibility and Android API concerns; compileSdk exceeds AGP's tested range.
- 33 Node fixture suites passed: 26 existing regression suites and seven added
  Claude/timeline/image-action suites. These exercise synthetic events, not real models.
- 59 Python unittest cases passed for backup parsing/validation, Lint-report gate,
  verified X11 preparation and native chat catalog pagination. Localization checked
  587 strings. These do not install environments or exercise a live desktop.
- AI-INSTRUCTIONS.zip contains only the four exact common templates from the final
  main APK and its README. No project instructions, memory or private data are included.

## Checked anew — CI / debug

Main [CI run 42387377279](https://github.com/HezeAI/termux-jobs/actions/runs/42387377279)
passed all 1,909 executed JVM cases, with 8 skipped, and built the internal debug
APK. Tested source/workflow: `a186203139723c85ad380674f29c9f42f4ac2001`.
Rollback [CI run 42385931862](https://github.com/HezeAI/termux-jobs/actions/runs/42385931862)
passed 1,774 cases, with 8 skipped, and built debug from `dd632c3b`. The rollback
subsequent commit only adds pipeline failure propagation; app/test/build inputs
are unchanged. Final main documentation-only changes are compared against the
CI source before tagging.

CI debug builds are internal verification artifacts. They are not the signed
release APKs above. Eight ServerSftpTest cases require the Termux-local OpenSSH
subsystem, absent in GitHub's Ubuntu runner; skips are not passes.

Initial main attempts failed downloading an existing pinned build input (HTTP 404).
A pipeline previously masked the preparation exit code; `pipefail` now propagates
it. A later attempt fetched and verified the same pinned input without changing
its dependency/version. The next full suite found 14 outdated expectations across
six test classes: old key count, flat action cards, sidebar geometry, removed
navigation buttons/documentation, and generic image errors. Tests now exercise
the completed UI and precise errors, preserving draft/no-send/file-access checks.
Original failures are retained; they are not relabeled successful.

The initial local full run also found the same 14 stale assertions. After updating
tests, all **102 targeted cases across seven classes** passed locally, including
all eight SFTP fixture cases using the existing local subsystem with synthetic
files. No SSH account, server profile or real remote connection was used. The
full local run is retained as failed; the subsequent full green run is GitHub CI.

## Reused with input comparison

- Three native JNI libraries are reused after fresh equality checks of all 95
  recorded compiler/linker/source inputs and output hashes. Other ELF assets are
  checked byte-for-byte against stable. No new GPU/native execution is claimed.
- Rollback derives from accepted legacy source `dfbf111f`. Its assets, native
  libraries and compiled resources match the accepted pre-redesign APK in 128
  entries. Version metadata is new; legacy application behavior is preserved.
- Claude CLI 2.1.296 evidence from [test24](VALIDATION-2.1.5-test24.md) is reused
  after confirming unchanged permission/launch sources and tests. `--help` lists
  Auto and Bypass; isolated native Auto initialize/set-mode returned ACK. Native
  Bypass under UID 0 refused startup. No real account/model authorization was used.
  CLI acceptance is not proof of the Auto classifier's real decisions.

## Device evidence and unverified scenarios

The developer reported that image original opening worked in the test chat.
That report is not an assistant-run phone test or proof for all agents/formats.
The assistant did not install this release. No connected ADB device was available
for independent UI validation during candidate work.

Not checked on the final APK: real keyboard animation/first-frame layering,
rapid transitions on physical phones, gallery/Download Android permission flows,
real application restart persistence, actual installer/rollback data retention,
Claude real-model Auto decisions, low-level Android crash/handoff recovery and
backup export/restore. See [audit scope](SECURITY-AUDIT-STAGE2.md). Local UI fixtures
are not phone tests. MCP/plugins and independent security certification are excluded.

## Codex WebSocket investigation

[Upstream issue](https://github.com/openai/codex/issues/55730) and
[HezeAI's published report](https://github.com/openai/codex/issues/55730#issuecomment-6645086819).
The public 11,008-byte attachment matches the local report exactly:
`30616711756094abee12d46ca06b5418da3f87fbdc7b679fd649341831722104`.
The old STATUS-RU.txt records an earlier failed attempt, not the subsequent post.

The recorded direct native CLI reproduction excludes the Android renderer/Node
adapter as a necessary cause. It does not identify every remote closer or rule
out network-route variation. No universal 16 MiB cutoff was independently proven.
Image-heavy cold replay retried in 12/12 recorded 20–35 MB cases; ten completed
through automatic HTTP fallback and two reached the 240-second research deadline.
Three approximately 10 MB controls completed without retries. Manual compaction
helped one context, but images accumulated again. The issue remains open/unfixed.
The historical experiments were not repeated to prepare this release.

## Publication evidence

The release's validation.json records exact source/CI identities, artifact checks
and remaining limits. Public download verification is performed after publication;
its local evidence is kept under `.verification/release-v2.1.5/` with initial failures,
Lint reports, source inventories and checksums. This report alone does not assert
that a still-unpublished asset has already been downloaded publicly.
