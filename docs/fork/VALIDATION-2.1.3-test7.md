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

## Phone check (not yet passed)

Use a new Gemini API test profile and disposable project with an artificial key.
Keep old profiles, working sessions and real authorization intact.

1. Save/reopen the test profile. Start, stop only this test session using its normal
   confirmed stop action, and start it again. The CLI should remain 0.64.0; the
   requested auth mode and permission policy should survive.
2. If a newer-version notice appears, it must be information, not a red session
   error. No automatic npm installation should start. Lack of a notice does not
   itself prove network availability or disablement of version checks.
3. Send a short artificial message. Expect an explicit provider rejection of the
   invalid key (401 or another clear invalid-key status), separate from update
   notices. Do not count a ready banner or disappearance of the update error as
   handoff success. Report stage/code and sequence without sending the key.
4. Full Android process-death/Keystore/SAF backup checks remain in the unified plan.
   Never force-stop the shared working app. Use an isolated instance without
   working sessions for that separate check; skipping it leaves it unverified.

No real provider login, Android peer credentials, phone path/handoff, private-key
custody or independent audit is established here. Actual Google traffic was not
used; external connections were blocked by the fixture kernel guard.
