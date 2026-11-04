# Private test APK 2.1.3-test7

2026-11-04. VersionCode 1483. Signed release, no push/publication/device install.
Includes test6's audit/backup changes. Published v2.1.2, its tag, update metadata,
Components and the delivered test6 binary remain unchanged.

**Gemini stays at 0.64.0. No working CLI or authorization was upgraded.**
**Attachment accumulation/disconnections remain unfixed and outside this APK.**
The [unified audit plan](SECURITY-AUDIT-STAGE2.md) remains open for phone acceptance.

Local runtime commits: `9b881f3a` (settings/canonical path) и `e78f855d`
(notification persistence). Build/version/docs commit recorded with the artifact.

## Fixes and cause

Native Gemini defaults enable both automatic installation and notifications.
Our earlier run policy selected auth only; installation detection can fall back
to global npm. The native asynchronous updater adds a failed-update error to UI
history, independently of session startup. That explains its classification;
it does not establish the cause of a particular phone's startup failure.

The supported per-run system policy now enforces `general.enableAutoUpdate=false`
and `general.enableAutoUpdateNotification=true`. Native precedence is checked,
including conflicting fake user/project settings and all four auth modes.
User files are unchanged. Auto auth has no forced auth type. External system
policy is refused rather than silently replaced. Updates require explicit action;
the pinned runtime is not replaced just because another version is advertised.

Updater callback items are tagged at their source and use native `type:info`
before recording. Native recording drops unknown metadata, so the info type is
required for reopen. Arbitrary error text does not decide classification. Genuine
API/auth/tool errors remain errors. Historical untagged error records are not
rewritten or guessed to be update notices; use a new test profile for acceptance.

The installer extracts a directory; it does not itself create a runtime symlink.
But runtime/HOME aliases are accepted by the launch path. A logical path could
mismatch Node's canonical module URL, skipping our loader hooks. The controller
now resolves one canonical binary path for argv and the loader environment.
The phone's actual path has not been inspected; a symlink there is not assumed.

## Checked anew

Evidence: `.verification/gemini-managed-updates/`, including failed initial
probes; `.verification/release-2.1.3-test7/` for the final artifact.

- Supported settings: native Gemini schema/loadSettings, 4 modes × first/repeat,
  system precedence and unchanged fake user/project files. Exact native update
  check still advertises synthetic 0.66.0; automatic installation spawn count is 0.
- Notification/error distinction: exact updater callback, three pinned UI layouts,
  native useHistory/recordMessage and native history-to-UI conversion. A recorded
  update remains info; a recorded synthetic HTTP 401 remains error on reopen.
- Ten primary Node suites plus the native-replay and existing chat projection checks: update/settings, missing handlers, auth modes, broker, IPC chat,
  recovery, permissions, live modes and hooks; owned-listener kernel guard enforced.
- Real bootstrap/controller/Gemini CLI in a PTY: normal path and runtime alias,
  first and repeat (4 launches), without a canonical-path workaround in the alias
  harness. Fake broker delivered the run-bound artificial key; native chat accepted
  a message; the local provider received that key and replied HTTP 401. The refusal
  stayed visible as error. Stop acknowledgment follows the owned CLI child's exit;
  the controller also exited. Installed runtime hashes remained unchanged.
- The native “Authenticated with gemini-api-key” banner and `authError=false`
  identify selected mode, not server acceptance. These four real requests failed
  as intended. This is not successful Google login or phone handoff acceptance.
- Real controller normal/alias path, absent UI binding and unsupported-layout
  fixtures reject chat-send with `NOT_READY` and never return accepted. An alive
  process alone does not establish readiness. Unsupported paths do not weaken policy.
- Gemini API/Vertex new-profile native shell/write No/Allow regression: 16 cases,
  one explicit JVM integration method after the final notice change. See final
  result XML/log. Debug is internal testing; the delivered APK is release.
- Syntax, RU/EN notices, 586 mapped localization strings and format contracts pass.
- Full modern Lint 7.4.2 for all five modules: 0 errors, 236 warnings, no rule-loading
  failure. Compile SDK beyond AGP's tested range remains a limitation.

The initial fixture had an unregistered Unix root; a later alias probe skipped
hooks. A temporary harness-only realpath isolated that mechanism, then was removed
in favor of the production fix. The missing-handler fixture initially omitted
native Config.setModel and got STALE_SESSION; corrected and repeated. Those
failures remain recorded and do not count as passes. An initial nonfinal APK was
retained locally before the notification persistence correction; it was not issued.

## Reused with input checks

- 21 unchanged Java/Robolectric vault/broker/editor/launch-builder methods from the
  22-method run. Java/test hashes match in reused-java-inputs.json; native-profile
  integration reruns separately after the final loader change. Existing SDK,
  dependencies and compile settings reused; no phone/Keystore claim follows.
- The three native libraries match the verified v2.1.2 payloads. All 95 recorded
  native/config inputs and compile/link script match. Java/resources/assets rebuild.
  No fresh GPU execution or dependency clean install is claimed.
- OpenCode clean export/restore, legacy v1/v2 and passive receipts checks retain
  the scope of e676dd42; backup production/tests are unchanged from test6. Other
  agents were not rerun because the fix is Gemini-only.
- Developer Codex test5 phone acceptance is retained with its reported scope.

## Final release APK

- Package `com.termux`; **2.1.3-test7 / 1483**, ARM64 only, non-debuggable.
- Size **40,471,430 bytes**; APK v2 signature verifies.
- APK SHA-256: `1150542d8c71674ee6137554c3bcf1e7edc7c91db56e470e5fd2f55bf0163dc3`.
- Public certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP CRC/outer-container checks, all 121 packaged assets and seven ARM64 ELF
  payloads pass. 573 production Java/resource/asset inputs match packaging;
  14,202 DEX definitions, 31 reviewed optional unresolved types.
- verifyReleaseSigner and verifyX11Inputs ran and passed on the final release.
  Legacy packaging Lint/Kotlin metadata diagnostics remain in final-build.log;
  they are not counted as a successful Lint result. The separate modern gate passes.
- Sources/commit and Download copy are checked after the final local commit.
  SDK/Gradle/bootstrap caches reused. No GitHub CI or public channel change.

## Developer phone acceptance — 2026-11-04

Source: HezeAI's report in this conversation for **test7 / 1483**, built from
`0a51e24cddbcc3e33de2a8869446111854552679`. This is developer-performed acceptance,
not a new test independently run by the assistant.

| Phone check | Reported result |
| --- | --- |
| Start a test Gemini API profile with an artificial key | Passed: profile starts. |
| Send a message | API rejects it with **HTTP 400 / API_KEY_INVALID**. |
| Stop that session normally and launch again | Passed: the same invalid-key response is received. |
| Update error interference | The developer reports it no longer interferes. |

The reported functional artificial-key path reaches an explicit API rejection
on both launches. Phone HTTP 400 is distinct from the local fixture's HTTP 401;
neither response is successful authentication. No packet/key-byte trace or
independent socket/Keystore inspection is inferred from this report.
The report does not establish profile editor save/reopen, mid-handoff cancellation,
new-version notice persistence or the phone's normal/symlink path.

## Remaining phone checks

- Actual API/Vertex sign-in with a separate test account, Android peer credentials,
  lower-level broker ownership/Keystore behavior and mid-handoff cancellation.
- Abnormal app/process termination and recovery on an isolated instance without
  working sessions. Normal session stop/relaunch is not a crash-recovery test.
- Gemini outside-project actions and separate outside-file preservation checks.
  Subsequent developer messages accept inside-project shell/direct Write Deny and
  Allow once; see the dated acceptance below. The invalid-key check alone does not
  cover permissions.
- New-version notification stays informational across history reopen; the current
  report confirms absence of interference, not that specific presentation check.
- SAF/export/clean backup restore and other open items in the unified plan.

Do not repeat the accepted startup/normal-stop/relaunch checks without a relevant
change or new failure. Keep real authorizations and working profiles unchanged.
No independent audit or private-key custody verification is established.
The assistant's preceding CLI tests used local providers and blocked external
connections; they do not describe the developer's untraced phone network route.


## Subsequent developer phone permission acceptance — 2026-11-04

Source: the developer's phone messages, «все прошло» after the four AUDIT steps,
and pasted final marker values. Shell and direct Write Deny/Allow once inside the
artificial project are accepted in that scope. Intermediate denial states are
reported by the developer, not independently observed by the assistant. This
continues test7 testing; no new APK identity was measured in that message.
[Exact evidence and limits](PERMISSIONS-COMPATIBILITY-TEST5.md#developer-reported-gemini-phone-acceptance--2026-11-04).
The two outside marker values are recorded as displayed; broader preservation,
outside-project actions, Google login, broker/Keystore, abnormal recovery and
backups remain unverified on the phone. Native fixtures remain separate assistant
results and are not attributed to the developer. No APK was rebuilt for this note.
