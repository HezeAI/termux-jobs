# Environment and X11 audit — 2026-10-23

Private2.0.4-test/code1333, following the
[agent-chat audit](AUDIT-AGENT-CHATS-2026-10-23.md). Both audits' fixes are included.
Public stable remains v2.0.2/code1331. This work does not publish a new release.

## Findings and changes

**X11 component cancellation finished too early in the state machine.** The
service immediately published `cancelled` after closing the guardian pipe.
Although its component worker serialized its own jobs, a program operation uses
a separate worker and could be admitted before the old package job exited.
The service now publishes `cancelling`, refuses new component/program/renderer
work during cleanup, and publishes `cancelled` only after that owned worker
has reaped its guardian. Repeated Cancel is idempotent. State polling, detach,
scoped session-close and explicit Stop remain available. Old-generation cleanup
cannot unlock a different operation. The UI retains its progress and disabled
cancel control while the operation is stopping.

**A component output failure also released the operation too early.** Exception
handling posted retryable `error` before the `finally` block had finished the
owned job, and that block closed its pipe without waiting. The completion/error
callback is now posted after pipe close and wait. The same cleanup helper is used
when a desktop output reader fails. It restores a thread's interrupted flag after
finishing cleanup; it does not infer or signal foreign PIDs. Guardian process-group
creation and TERM/KILL escalation are unchanged.

**Ordinary Ubuntu had lost launcher maintenance.** The UI allowlist and APT shell
branch still restricted repair to the old test Ubuntu and Debian. Ordinary Ubuntu
now exposes Repair launcher permissions under installed desktop programs and
routes it to its existing `ubuntu` rootfs. Repair still requires explicit consent
and a stopped desktop. It retains checksum matching and original permission
backups; this audit adds no recursive permission rewrite or automatic repair.
Ubuntu AI remains outside the desktop allowlist.

## Coverage

| Area | Performed check | Evidence/limit |
| --- | --- | --- |
| Environment installation | Native proot CLI argument selection, selected alias, failure propagation, existing-root protection, Arch SHA-256 refusal/cleanup, legacy alias behavior | Isolated shell fixtures; no actual image installation or current repository health check |
| Installer cancellation | Real PTY supervisor with owned process group, cancellation before launch, SIGINT cleanup, TERM-resistant child, unrelated-process survival | Existing environment supervisor fixtures; no live user terminal/process affected |
| Clean desktop requirements | Complete package request for each manager; missing desktop/panel/settings/D-Bus/certificate/audio components reject readiness | Fake managers/files, including absent-component cases; not a fresh-device GUI installation |
| Rootfs/display isolation | Separate fixed process/display/control path and wrong-target rejection; all eight current targets plus retained legacy test slot | Java/shell contracts; multiple real GUI surfaces not opened in this audit |
| Renderers | CPU/Turnip/VirGL per-target routing and poisoned prior environment state; native adapter declarations; package/hash/archive refusal | Existing hermetic suites; no new hardware/GL benchmark |
| Wrapper | Termux-only admission, archive traversal/link/ABI/hash checks, private install and selected-driver state | Isolated file/archive fixtures; imported real driver execution not repeated |
| Programs | Per-environment check/catalog/cache, install/resume consent journal, dependency-safe removal, Firefox/PDF/media/GPU helper paths | Mock managers/metadata and JVM controller/UI tests; current real package availability not asserted |
| Stop/close | Real guardian + routing script ends a TERM-resistant process tree for Termux, Ubuntu, Debian, Alpine, Fedora, openSUSE, Void, Arch and the legacy test alias; unrelated fixture survives | No native graphical server or installed rootfs was started |
| New error/cancel barrier | Real service worker with controlled child callbacks; delayed child exit, old generation, interrupted wait and UI progress | JVM mocks; separate real guardian test covers EOF cleanup after a synthetic output-consumer failure |
| Ubuntu repair | Missing allowlist/route reproduced before fix; selected-root command capture and unchanged sentinel; explicit UI action | Actual chmod/checksum helper tested on temporary files, not on installed desktop shortcuts |

## Results

- Python baseline:40 suites,332 tests passed. The final desktop suite includes one
  additional Ubuntu repair regression and expands owned shutdown to Void/Arch;
  together with the unchanged suites this covers333 tests, all passing.
  Clean-matrix and launcher-helper follow-ups passed separately.
- Before the fix, Ubuntu repair failed with `ERROR:REPAIR_TEST_ONLY`; the new
  service tests also rejected the old maintenance allowlist and immediate
  `cancelled` state as expected.
- A first scoped JVM run covered305 tests:303 passed, while two new lifecycle
  tests had an invalid fixture constructor. The fixture now constructs its
  synthetic X11Runtime explicitly. An expanded Arch shutdown fixture also needed
  its required local Python executable; this was corrected without a product
  change.
- Broad final JVM run:912 app tests,911 passed and one new UI fixture failed
  because it requested a scan instead of supplying a ready installed catalog.
  After correcting that fixture, all61 tests in the complete affected UI,
  service-integration and process-lifecycle classes passed. No product code
  changed after the broad run. This is combined coverage, not a claim of a
  single subsequent912-test green run. The unchanged154 terminal-emulator test
  results were reused by Gradle.
- The real guardian EOF/output-failure test, interrupted-wait test, delayed
  service-child-exit tests and all-target cancellation guard tests passed.
- Source equality confirms the preceding audit's10 changed agent runtime files
  are preserved; no unrelated agent implementation was changed in this step.
- ARM64 release-configuration build passed. Artifact:
  `build/apk/Termux-Jobs-v2.0.4-test-arm64.apk`, 45,830,242 bytes.
  SHA-256: `a4a0ab8365c78ac06477a10ebe21edb35738b14f20687266c7c15316dc51a1ce`.
- ZIP integrity, com.termux/version2.0.4-test/code1333, ARM64-only ABI,
  non-debuggable flag and signature verified. Certificate SHA-256:
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`; matches private1332 and the public signer.
  All packaged native libraries match private1332 byte-for-byte; checked agent
  and X11 assets match the audited sources. Secret/file-pattern screening found
  no matches; this bounded scan is not proof of every possible secret encoding.
- Runtime inventory:13,604 defined classes; no missing required definition or
  unreviewed missing type. Existing31 optional/metadata/JVM-only references have
  explicit review notes. Method/reflection compatibility still needs devices.
- Gradle succeeded with existing lint/Kotlin metadata/tooling errors in its log;
  lint is not claimed fully clean. Code tests and APK verification are separate.
- 246 relative documentation links resolved; `git diff --check` passed.

Local reproducible evidence: `.verification/x11-env-audit-20261023/`, including
per-suite logs/JSON, before/after regressions, Gradle logs, XML, artifact metadata
and runtime inventory. Test sources include `X11IntegrationTest`,
`X11ComponentLifecycleTest`, `DesktopProcessLifecycleTest`,
`WorkspaceDesktopPreparationTest`, `WorkspaceDesktopProgramsTest`,
`test_x11_desktop.py` and the existing environment/X11 suites.

## Review and logical inspection

Traced environment check/install/retry and selected alias resolution; desktop
check/install → renderer preparation → launch → owner attach/switch/close; program
check/catalog/cache/install/resume/remove; and cancellation/error callbacks across
workers. Component state becomes retryable only after the owned group has exited.
Cleanup never changes the guardian's pinned process identity or native termination
rules. No new shell text from a profile becomes a package name/target/process ID.

The renderer binaries, GPU bundles, native X11 server, display numbers, signing
identity and saved user settings are unchanged. No system packages or installed
rootfs were modified by verification. The fresh-install package lists remain
explicit and the tests reject missing mandatory components.

This is a bounded audit, not proof of every package/driver/device combination.
In particular, when Termux Python is absent, the environment installer's initial
`pkg install python` prerequisite runs before the Python cancellation supervisor:
a cancel marker is observed on supervisor admission, not midway through that
prerequisite transaction. That existing bootstrap behavior was not changed or
real-device tested here. Repository outages, Android background process killing,
keyboard/gestures and actual package DB recovery after an interrupted transaction
still require device checks.

## Compatibility and smallest phone check

Same com.termux package, ARM64, Android8+ application baseline and schema8;
embedded desktop still requires Android11+. Code1333 exceeds private1332/public1331.
No data reset, auth migration or rootfs replacement. Existing presets, chats,
program resume journals and renderer choices remain in their existing formats.
The optional OpenCode run field from the preceding audit is retained. An APK
update/downgrade migration has not been tested on a device in this audit.

1. Save the agent chat and update with the private APK. Existing agent audit fixes
   are included; restart a saved agent explicitly to load those helpers.
2. In one stopped desktop, begin an installation/check, press Cancel, and confirm
   the progress stays visible until cancellation completes. Retry only afterward.
3. In ordinary Ubuntu, open Programs → Installed programs and check that Repair
   launcher permissions is present and requires the usual explicit confirmation.
4. Launch one desktop with its previous renderer, switch terminal/desktop/tabs,
   then close its owner. Confirm another environment/agent remains available.

Fresh-device installation should be checked separately on a device without those
components; do not erase a working environment just to manufacture that check.

AI development assistance: OpenAI Codex
