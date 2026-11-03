# Local test release 2.1.3-test2

2026-11-03. VersionCode 1478. Local test release, no publication or installation.
The user's acceptance of test1 remains historical; it does not accept these changes.

## Cause and change

The host emitted `checking-ubuntu` and immediately invoked `pkg install proot-distro`
without a new event. The bounded parser also omitted some legitimate Termux prerequisite
stages. UI notification throttling could suppress a recent stage transition, and a
`failed-ubuntu` marker replaced the more precise failure location. Generic STAGE dispatch
also preceded the component-download translations, displaying protocol identifiers.

The host now emits fixed `preparing-termux-packages` and `COMPONENT:proot-distro`
records before pkg. Known Python/proot/tools phases pass the whitelist. A dedicated
`extracting-environment` event is emitted immediately before the existing local Ubuntu AI
installer; setup/download are no longer labelled extraction. A final structured phase
precedes the existing component inventory. Phase changes/errors notify immediately;
measured byte progress retains throttling. Generic failure markers preserve the preceding
phase, with safe RU/EN errors and retry. A CLI-local `ready` record is displayed as
finishing preparation until the full operation passes its final inventory. Unknown/oversized/unfinished records remain discarded.

Only the star / paperclip group's fill becomes opaque (`#0b1319`, existing workspace
panel colour). The row remains transparent, the field retains its background, and
horizontal order, independent 48 dp ripple targets and scroll insets remain unchanged.

Gemini API and Vertex previously shared an active-job key despite requiring different
components. Their keys now include the selected mode. They still use the same single
executor, so changing modes cannot create simultaneous installations or reuse the wrong
mode's result. No component recipes, download policies, versions, URLs, SHA pins,
rootfs replacement rules, CLI permissions or process wait/cancellation semantics changed.

## Automatic checks and coverage

| Scenario | Codex | Claude Code | Gemini API | Gemini Vertex | OpenCode |
| --- | --- | --- | --- | --- | --- |
| No proot-distro / no Ubuntu AI: stage sequence and measured progress | Fixture | Fixture | Fixture | Fixture | Fixture |
| Existing Ubuntu AI, missing agent components; no image installation | Fixture | Fixture | Fixture | Fixture | Fixture |
| All components ready | Fixture | Fixture | Fixture | Fixture | Fixture |
| Package/download failure, precise phase and explicit retry | Fixture | Fixture | Fixture | Fixture | Fixture |
| Check window / first-launch consent, repeated action and close/reopen | UI model | UI model | UI model | UI model | UI model |
| Shared queue, distinct operation, no parallel workers | Queue model | Queue model | Queue model | Queue model | Queue model |
| Production child collection and correct guest/auth command routing | Fake tools | Fake tools | Fake tools | Fake tools | Fake tools |

These cells describe automatic checks, **not successful real installations**:

- Six shared-host fixtures execute actual `agent-host-setup.sh` and, where applicable,
  `environment-runtime.sh`, with artificial pkg/proot/Python. They assert the event precedes
  pkg/extraction, download failure never extracts, and existing rootfs fixture is preserved.
- The JVM matrix covers five profiles and five inventory/failure scenarios, then success
  on explicit retry. Queue workers are artificial and track a maximum concurrency of one.
- Actual `beforeLaunch` consent and direct `AgentPreparationOperation.show` use injected
  inventories/active workers. Closing/reopening rejoins the same job; the check-button path does not
  launch a conversation; first-launch completion is delivered only after readiness. Ready first-launch inventories bypass preparation. Generated
  session shells retain the right agent, environment and API/Vertex recipe and pass Bash syntax.
  The settings-button connection to `show` is also inspected in WorkspaceController.
- Production `prepare`/Job/drain run over real child pipes with synthetic Android/tool paths:
  seven output/error/cancel/retry scenarios for each of the five variants (35 cases). Fake
  proot records the actual generated guest command; no CLI, model or guest is launched.
  Artificial multiline secrets are not retained; old fixture logs remain byte-for-byte.
- Existing shell recipe regressions cover read-only checks, consent, missing-only packages,
  failed APT/hash aborts, existing credentials, CLI reuse and all agent bootstrap paths.
- Phase/UI tests verify fixed RU/EN labels, immediate changes within the throttle window,
  measured bytes overriding the supplied percentage, indeterminate non-download phases,
  exact failed phase and independent composer actions/opaque group/transparent row.

Final scoped JVM result: **115 tests, 0 failures, 0 errors, 0 skipped**.
Full modern Lint 7.4.2 across all five modules: **0 errors, 234 warnings**.
The full unrelated 1608-test test1 suite was not rerun; its previous result is not
claimed as fresh validation of test2. Native/GPU/history behavior is outside this
change; input identity and final native/runtime packaging are checked separately.
Nine real local TLS/download/owned-process queue regressions also passed, using
shortened policy constants and artificial content. No external endpoint was used.

Two initial test failures were retained and corrected: the generated-shell assertion
initially ignored its extra quoting layer; the expanded reusable stub default caused
the TLS queue probe to give both fixture profiles the same identity. The test now
explicitly chooses Claude and Codex. Production process/queue protections were not
weakened. Final reports/logs replace neither initial failure evidence nor old device
results. The SDK apksigner launcher was non-executable; verification used its existing
JAR through Java, not a replacement signer.

Evidence: `.verification/preparation-stages-test2/` and `.verification/release-2.1.3-test2/`.

## Real installations and device checks

No real pkg/APT/rootfs/CLI installation was performed in this task. The installation
transactions are unchanged; this change instruments and displays them. Existing working
environments and sessions were not modified. Artificial fixture success does not prove
clean installation on Termux or provider authentication. No new device check was performed.

On a separate clean test installation, check all five variants from the environment-check
button and first session launch. Verify visible package preparation before pkg, measured
image progress, extraction, component preparation, final check and ready. Interrupt/fail
only a task-owned fixture transfer; check the phase, retry, duplicate taps and close/reopen.
In an existing test Ubuntu AI, confirm no image download/reinstall and only missing
components are added. During operations for different agents/API/Vertex, verify one
installation at a time. Check listening/auth/model availability separately from component
readiness. On the phone, confirm only the star / paperclip group is opaque and its two
buttons still respond independently with keyboard and multiline input.

## Final artifact

- Package: `com.termux`; versionName `2.1.3-test2`; versionCode `1478`.
- Size: 40,458,097 bytes; ARM64 only; non-debuggable; APK v2 signer verified.
- File SHA-256: `e0208dff4891a599438848c508cbcc5a31d73ec1c8b637fa08c9de43b695963d`.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP integrity, manifest/version/ABI, signature, all 120 asset bytes and
  required notices passed. No old local ZIP headers were found in packaging gaps.
  All seven ELF payloads are ARM64. DEX check: 14,180 defined classes,
  31 reviewed optional unresolved types. These checks are not Android runtime execution.
- All 95 pinned native input hashes and unchanged native build flags match test1/
  v2.1.2 evidence. Three primary native libraries and X11 runtime assets were reused
  byte-for-byte. Java/resources/assets were rebuilt. `verifyX11Inputs` remained enabled.
  543 production source inputs match the ARM64 packaging stage. No native rebuild or
  GPU test was claimed.
- Package/certificate match test1 and v2.1.2; versionCode increases from 1477 to 1478.
  No schema/reset/migration change was introduced. Update/data preservation on a phone
  and downgrade were not checked for test2.
- No push, publication, installation, stable-channel, tag or Components change.


Local functional commits: preparation `c92df2a2`, composer fill `e5155e05`.
