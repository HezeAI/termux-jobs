# Private test APK 2.1.3-test5

2026-11-04. VersionCode 1481. Signed release variant for phone acceptance.
No push or publication. Developer-reported phone acceptance is recorded below.
Published v2.1.2, test4 and Components remain unchanged. Old profiles, permissions,
authorization and Ubuntu AI remain.

**Attachment accumulation/disconnection fix is not included.** The images/video/
large text/tool-result investigation remains separate for all four agents.

## Changes and scope

Fix Codex 0.166.0 new-profile app-server startup through supported bootstrap and
verified typed thread approval, without weakening the requested `untrusted` policy.
Add safe startup reasons/exit status and reject unsupported policies before launch
for all four agents. Include unchanged test4 audit/key-handoff/backup warnings.

See the [permission compatibility matrix](PERMISSIONS-COMPATIBILITY-TEST5.md) for
exact CLI versions, initial failed runs, actual No/Allow tool effects and limits.
The default-policy matrix is distinct from a complete alternate-mode sandbox audit.

## Verification

Evidence: `.verification/codex-startup-test5/` and
`.verification/release-2.1.3-test5/`. Final packaging identity and check totals are
recorded below after verification. JVM tests use debug internally; delivered APK
is signed release. Robolectric/native fixtures are not phone acceptance.

No new third-party runtime or component version is introduced. Reused native
payloads require matching all 95 recorded native source/config inputs, compile/link
parameters and output hashes. Java/resources/assets are rebuilt with the local
SDK/caches. No fresh graphics execution, environment install or clean-room
third-party dependency download is claimed.

## Phone plan

Codex startup and shell/direct-edit No/Allow have now passed developer phone
acceptance, as recorded below. The remaining steps are not closed by that result.
Follow the [new-profile checks](PERMISSIONS-COMPATIBILITY-TEST5.md#phone-acceptance-still-required)
for Codex, Claude, Gemini API/Vertex and OpenCode: save/reopen, actual launch,
shell/direct file No then Allow, old-profile preservation and stop/retry.
Also retain test4's [key broker/backup acceptance](VALIDATION-2.1.3-test4.md):
Gemini API authorization, cancel/retry, app-process recovery with artificial data,
RU/EN unencrypted backup warning. Real account access is not proven by fake keys.

## Checked anew and final artifact

- **88 selected JVM test methods**, no final failures/errors/skips. This includes
  seven explicit integration methods executing **65 native tool scenarios**:
  25 earlier UI-chain cases repeated plus the new 40-case profile matrix.
  New matrix: 36 target UI answers, 18 No and 18 Allow, four native Gemini outside
  path denials. Counts are nested; do not add method and scenario totals together.
- Actual editor creation/save/reopen/save for all four agents passes. Legacy
  Codex policy preservation passes for full/project/read, and the artificial old
  Termux profile remains unchanged through each new-profile native case.
- Initial broader run: 87/88 methods passed; one test still searched a superseded
  accessibility label. It now uses the existing stable control tag. Both methods
  in that class executed again and passed. Original failure/XML are retained.
- Unsupported/mismatched runtime policy tests leave Codex unready without starting
  a turn or retrying with weaker approval. All four Java launch builders reject
  unsupported permission selections without changing the profile.
- Node regression checks pass for Codex permission RPC (14 synthetic cases), chat,
  login, safe launcher/cancel/retry, Claude chat, Gemini permissions/modes and
  OpenCode settings. The owned launcher child terminates before the cancel result; an unrelated
  artificial child remains alive;
  safe error tests do not expose artificial stdout/stderr markers or write logs.
- Kernel network guard negatives pass anew: external TCP/UDP/raw/IPv6 and foreign
  local listeners denied; owned private Unix alias allowed; foreign Unix,
  traversal, nested path, nonprivate root and invalid Codex hash denied.
- RU/EN localization check: 586 mapped strings and format contracts pass.
- Full modern Lint 7.4.2, all five modules: **0 errors, 236 warnings**, no rule-
  loading/infrastructure failure. The compile SDK exceeds AGP's tested range.
  Legacy packaging Lint still emits incompatible Kotlin/rule API diagnostics;
  those are not a successful lint check. The independent modern gate above passes.
- Final APK: **com.termux**, **2.1.3-test5**, **versionCode 1481**, ARM64 only,
  non-debuggable release, **40,465,190 bytes**. APK v2 signature verifies.
- APK SHA-256: `5b996cbffffcc66528194ee1017b350c29c368b7916a858dc089dbab174b4580`.
- Certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Final ZIP CRC/outer-container checks pass. Seven ELF payloads are ARM64;
  all **121 assets** match current source/pinned payload bytes. **572** selected
  production Java/resource/asset inputs match the packaging stage. Runtime/DEX
  audit: 14,196 definitions, 31 explicitly reviewed optional unresolved types.
  Notices present; no microphone code, test fixtures, keys or caches packaged.
- Mandatory `verifyX11Inputs` and `verifyReleaseSigner` ran and passed. The new
  launcher asset is present; the original `CodexChat.verify` gate remains included.

**Reused:** three unchanged native libraries, after comparing all 95 recorded
source/config inputs, compile/link parameters and payload hashes. Existing SDK,
Gradle, bootstrap and dependency caches are reused; Java/resources/assets are
rebuilt. This is not a new native/GPU execution or clean-room dependency download.

**Not checked:** phone startup/cards for Claude, Gemini and OpenCode; production
account/model authorization beyond the reported Codex operations; restricted
sandbox isolation, device key-broker lifecycle, process recovery and backup restore
acceptance. Codex phone startup and shell/direct-edit No/Allow are now separately
confirmed by the developer below. No GitHub CI or publication was performed.
The new-profile defaults matrix does not validate every alternate permission mode.
See the phone plan above. Existing working sessions/profiles were not exercised.


## Local source commits

Branch: `work/codex-startup-test5`. Runtime fix: `4d377c7f`; native profile matrix,
UI persistence and audit correction: `db033888`. Version/build record follows in
a separate local commit. Final packaging source hashes are compared to that final
commit in `.verification/release-2.1.3-test5/commit-source-proof.json`.
No remote branch/tag/channel is updated. Published v2.1.2 and delivered test4 APK
hashes were compared again and remain unchanged.


## Developer phone acceptance — 2026-11-04

Source: HezeAI's report in this conversation for **2.1.3-test5 / 1481**.
The developer confirms the following checks passed on the phone:

| Check | Result | Evidence source |
| --- | --- | --- |
| Start a new Codex profile | Passed | Developer report |
| Reject a shell command | Passed | Developer report |
| Allow a shell command once | Passed | Developer report |
| Reject direct file editing | Passed | Developer report |
| Allow direct file editing once | Passed | Developer report |

These are user-performed device checks, separate from the automated/native
fixture results above. The report does not specify an inside/outside path matrix,
independent file-byte inspection, profile save/reopen coverage or CLI re-probing;
none of those details is inferred. The test4 startup failure remains historical;
the new-profile startup regression is resolved for this tested test5 phone path.

Still open on the phone: Claude/Gemini/OpenCode startup and permission decisions;
Gemini API/Vertex authorization, key broker, cancel/retry and process recovery;
restore and backup behavior, including RU/EN secret warnings. Codex alternate
policies, project isolation, legacy profile lifecycle, chat-switch races and
recovery are not established by this acceptance. Attachment accumulation remains
an independent unresolved task. No APK rebuild or binary change accompanies this
acceptance record.
