# Private test23 — 1518 — Claude permission modes

Date: 2026-11-07. Test APK only; no installation, release publication, or restart
of a working chat. Changes are launch preferences, not automatic live-mode changes.

## Behavior

Permissions and approvals now has native square Auto and Bypass permissions
checkboxes with RU/EN descriptions. Only one mode can be selected. Both replace
the ordinary mode; unchecking the special mode restores the previous manual,
acceptEdits or plan mode. Choosing an ordinary mode clears both special choices.
The previous ordinary mode is persisted, copied with profiles and restart snapshots,
and included in the concurrent-policy-change guard. Cancel changes only the draft.
Save keeps the existing explicit next-launch notice and does not restart a session.

Java validation, classic CLI arguments, SDK arguments, native terminal handoff,
profile editors and native mode labels recognize both values. The controller checks
CLI support and refuses Bypass under UID0 before spawning its worker. Auto must
receive an explicit set_permission_mode acknowledgement before readiness. If a
native init event reports a fallback mode, status displays that actual mode and
adds an explanatory error instead of claiming that Auto is active. Closing during
capability probing cannot start another worker. Native permission questions and
exceptions are still handled by the existing permission callback; no auto-Allow
hook, UID spoofing or root-check bypass was added.

The UI probes the selected environment's pinned Claude executable and UID using
read-only commands. Unsupported choices are disabled with a reason; a currently
saved unsupported choice can still be unchecked. This probe does not establish
account/model eligibility. Runtime rejection remains authoritative.

## Native verification and limitations

Installed binary: Claude Code 2.1.296, Linux ARM64. Its --help lists auto and
bypassPermissions. In the current Ubuntu/proot UID0 environment, actual native
`--permission-mode bypassPermissions` exits1 with the root/sudo restriction.
The UI and controller expose this restriction; a non-root deployment was not tested.

Native CLI with isolated empty HOME/config and the existing local-network guard:
initialize succeeded, then set_permission_mode(auto) succeeded. No model request
was sent by this probe. Native auth status for the current environment reported
loggedIn=false; the native CLI alone queried auth status. Raw authorization files were not inspected, copied or logged.
No successful real-model classifier or real-account permission test is claimed.

A new independent local stand uses the existing synthetic permission provider.
With the production controller: manual raised one pending permission, the marker
was unchanged before Allow once, then became AUDIT_SHELL_26. Auto completed with
zero pending host approvals and changed it to AUDIT_SHELL_27. Bypass refused before
worker startup. This is native CLI integration against a **fake model**, not proof
of real Claude classifier decisions. The first Auto probe reused the preceding
marker's value and was inconclusive; only the distinct-marker/completion rerun is
counted. Fixture workers were stopped through the controller; working sessions and
the earlier image fixture were unchanged. [Phone instructions](CLAUDE-MODE-PHONE-FIXTURE.md).

Official reference, used only as a cross-check of mode meanings and restrictions:
[Claude permission modes](https://code.claude.com/docs/en/permission-modes).
Current docs describe newer versions as well; installed-binary evidence takes
precedence for this build. Auto availability can depend on model/account/policy.

## Checks

### Checked anew

- Selected Java/Robolectric suite: **79 tests, zero failures/errors/skips**.
  Mode persistence/copy/reopen, each previous ordinary mode, mutual checkbox
  exclusion, cancellation/save/run identity, root/unavailable support reasons,
  profile validation, editing and launch paths, store/restart regressions.
  RU/EN controls tested locally. Final Java bytes match the unit stage.
- Four Node fixture suites passed: permission modes, chat projection/permissions,
  active input and subscription mapping. The mode suite additionally verifies
  cancellation during probing cannot spawn a worker, and rejected/fallback native
  modes are not reported as applied. Signal APIs are intercepted in synthetic checks.
- Native empty-HOME initialize/Auto ACK, actual root Bypass rejection, and final
  distinct-marker synthetic-provider run described above. No real classifier PASS.
- Modern Lint7.4.2: all five modules, zero errors, 283 warnings, no infrastructure
  diagnostics. Final Java/resources match analyzed inputs; the last JS-only close
  guard was checked by Node and final packaging, not by Lint.
- Final release assembly succeeded. Legacy lintVital emitted known ARM64/custom
  rule loading failures and is not counted as a passed Lint; the separate modern
  run is the static gate.
- APK ZIP integrity/no stale local headers; com.termux; 2.1.5-test23/code1518;
  ARM64-only/non-debuggable; v2 signature, unchanged certificate
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`;
  required DEX/runtime/assets/notices/private-data gates passed. All seven ELF
  payloads are ARM64. 623 production sources matched the final stage. All three
  Claude controller/permission assets also match final source bytes explicitly.
- Preliminary APK audit stopped because its expected build-final.log did not yet
  exist. That candidate is retained as before-close-guard.apk and was not delivered.
  Final build/audit reran successfully; the original failed log remains available.

### Reused

Native payloads from the validated test1 APK: fresh comparison of all 95 recorded
native inputs, unchanged native compile/link flags, final payload hash checks.
No native/GPU rendering claim. Old APKs and reserve1499 were not changed.

Final APK: **40,627,757 bytes**.
SHA-256: `17d3a5dfb97c2c35ae1ed5d936009ede36f324fa6d8206bc77f645738726f497`.
Evidence: `.verification/test-2.1.5-test23/` contains unit-summary.json/XML/logs,
Node logs, native-auto-summary.json, bypass.txt/help.txt, fixture-probe-final.log,
modern-lint-summary.json/XML/log, build-final.log, apk-check-final.log,
signature.txt/runtime.json and native-reuse.json.

Initial Java run: 76 tests, one failure. The old UI test searched the former
ordinary-mode row tag; it was updated to the new row. Its capability probe also
attempted native transport from Robolectric; this test now shadows the probe and
uses explicit synthetic support. The subsequent full selected suite is the result
reported below. The mocked support is not native compatibility evidence.

Native/runtime code tests guard real signal APIs. They cover accepted/rejected mode
values, native-root refusal, explicit Auto ACK, rejected changes preserving the old
mode, native fallback display, and cancellation during probing. Existing chat/input/
subscription fixtures retain permission nonce/session/once/deny handling, tool
ordering, queue cancellation, usage mapping and no-signal/no-model boundaries.

## Compatibility

com.termux, unchanged signer, ARM64, schema8 with one optional previous-mode field.
Existing ordinary profiles retain their mode. No automatic migration or deletion.
Before downgrading to test22 or older, switch saved Claude profiles to an ordinary
mode: those older builds reject auto/bypassPermissions in their profile validator.
Installed-update/data-retention and downgrade behavior were not exercised on phone.

## Not checked on device / with real Claude

ADB has no attached device. Phone checkbox rendering/taps, real app reopening,
manual chat restart and the user-facing unsupported-mode message need acceptance
on the test APK. The real Claude classifier, account policy restrictions, model
eligibility, non-root Bypass execution and its remaining native exceptions were
not verified. The local fake model is not a substitute for those checks.

No X11/GPU/environment installation or other-agent permission behavior was changed.
