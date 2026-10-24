# Cumulative regression audit — 2026-10-24

Private2.0.13-test/code1342 retains the preceding private audits; public stable
remains v2.0.2/code1331. This pass combines module/script regression, built-in
Android lint, source/lifecycle review and the final APK checks. Evidence lives in
`.verification/regression-audit-20261024/`. No installed app, system package,
shared SDK, rootfs, working terminal session, account or user's server/bot was
changed. Test processes and generated files remain inside the repository.

## Findings and changes

The first release-lint run reported21 errors and137 warnings:

- 17 orientation annotations rejected numeric0/1 values. Their exact framework
 constants now express HORIZONTAL/VERTICAL without changing layout values.
- 3 API30 warnings could not infer existing X11 creation/lifecycle guards. Explicit
 Android30 checks now precede IME diagnostics and focus-driven keyboard calls.
 This is defensive guarding/static-check clarification, not proof of a reproduced
 crash on an older phone. The app remains minSDK26; embedded X11 already requires30.
- 1 AppCompatCustomView finding conflicted with the deliberate platform TextView
 used for custom icon drawing/tinting. One class-scoped exception documents that
 choice; superclass, dimensions, padding and icon placement are retained. No
 global issue disable, baseline or abortOnError workaround was introduced.

The badge Paint is now kept per icon widget instead of allocated on every badge
draw, removing the DrawAllocation warning. No measured frame-rate improvement is
claimed. The first numeric-constant edit missed a qualification in WorkspaceUpdates
and failed compilation; the constants were fully qualified before the final run.
This intermediate error is retained in the evidence and was not shipped.

Documentation incorrectly named2.0.1 as the APK to download while stable is2.0.2;
that filename is corrected. Installation requirements now explicitly distinguish
Android8+/API26 for the app/terminal from Android11+/API30 for embedded X11. This
clarifies existing support rather than raising minSDK or altering package identity.

Production changes since1341 are limited to16 listed UI/controller source files,
plus version metadata. They contain named layout constants, the explicit guards,
the scoped widget lint annotation and badge Paint reuse. Installer/file-transport/
process-stop logic, scripts, native libraries and data formats are unchanged.

## Automated evidence

Final source passed **1172 JVM tests in177 classes** (1018 app,154 emulator),
with no failures/errors/skips. Python ran560 tests:559 passed and one retired-
overlay case skipped, plus import-level assertions. All31 Node fixture programs
passed. Localization verified584 workspace strings and app/shared format keys.
Executed lint checks completed with0 errors/136 warnings, subject to the custom-
check/toolchain gap described below.

Verified APK: `build/apk/Termux-Jobs-v2.0.13-test-arm64.apk`, 45,846,118 bytes;
SHA-256 `d4644ed709480c6cb2e2f3376be70e604f173954b89c9c23529cf4c6f269931c`.
ZIP integrity, com.termux/code1342/name2.0.13-test/ARM64, same signer as1341,
required runtime definitions, tested-source/APK-stage identity and bounded secret-
pattern checks passed. All assets/native libraries match1341. No required runtime
classes are missing;31 reviewed optional types remain. Native/process-stop sources
are unchanged. Final build and test/lint commands completed successfully with the
noted Kotlin/custom-lint warnings. Source/build results do not establish physical
phone behavior or installed upgrade data survival.

The initial full JVM run on1341 passed1172 tests:1018 app tests in158 classes and
154 terminal-emulator tests in19 classes. Terminal-view, termux-shared and x11
participate in the build but have no separate JVM test sources; X11/controller
fixtures also live in the app suite. This is not coverage of every module method.
The final full run was repeated after the source edits, not inferred from that
initial successful run.

Python discovery covers all52 `scripts/tests/test_*.py` files. Final aggregate:
560 tests run,559 successful and one intentional skip for a retired unsafe Garcon
trial overlay absent from APK inputs; one module additionally executes import-
level shell assertions without unittest test cases. Two runner corrections were
needed, neither indicating a product failure:

- The custom per-file loader initially omitted the repository import path, so
  test_agent_host_environment did not import. Its corrected13-test rerun passed.
- The cleanup module exceeded a120-second runner limit. The unchanged60,005-file/
 40GiB-sparse case passed separately in130.926 seconds; all other28 cases also
 passed. No real40GiB data allocation or user-storage deletion was used. Only the
 known private interrupted fixture was removed afterward. The final aggregation
 preserves both first-run failures and replacement results, without counting
 partial/repeated tests as additional successful coverage. This is not a timing
 benchmark for the installed app/Termux.

All31 `scripts/tests/test_*.cjs` programs exited successfully, no timeout/skip.
Native CLI fixtures use private homes/configuration and configured loopback model
providers; they do not use a real account's inference quota. These are fixture
program counts, not an invented assertion/test-case total. The Node runner starts
private sessions and keeps their leader unreaped until owned-group cleanup; it
never targets a user's terminal process or a borrowed/reaped PID.

Localization validates584 workspace strings plus app/shared settings keys and
format arguments. XML/secret/signature/runtime checks concern the final APK, not
only a successful test compilation. Source/stage snapshots bind automated results
to the packaged sources. Installed Android upgrade/permissions/IME/gestures remain
a separate device boundary.

## Lint result and tool limitations

The executed built-in checks report no errors and136 warnings after the changes.
Remaining categories include composed UI text, drawable compatibility, context
retention, locale handling, storage aliases and accessibility. They were not
blanket-suppressed or all declared harmless. In particular, settings singleton
contexts and locale-sensitive file handling merit focused follow-up review.

The existing lint/Kotlin toolchain cannot load some AndroidX custom registries
(missing Vendor class) and reports Kotlin metadata-version incompatibilities.
Thus a successful Gradle lint task is **not a complete clean-lint result** and
cannot establish that those custom checks passed. Toolchain alignment is separate
work; no shared SDK upgrade, dependency downgrade or incompatible target/package
migration was performed to hide the problem. The137→136 warning change is the
badge allocation warning, not the disappearance of the other outstanding topics.

## Cumulative scope and logical review

| Area | Current evidence and retained behavior | Still outside the proof |
| --- | --- | --- |
| Agent chats | Full app fixtures and31 Node programs: session/run ownership, focus, approvals, media, history and controlled real-CLI streams | Live provider auth/quotas, physical keyboard and account-specific behavior |
| Environment/X11 | App lifecycle fixtures and isolated per-target clean-component/routing/renderer scripts | Every fresh device, real graphics driver and distribution mirror/version |
| Installers | Non-reaping group supervisor and Java cleanup barriers retain earlier scoped coverage | Bootstrap preparation cancellation, force-killed app recovery and detached sessions |
| Backup/restore/cleanup | Archive validation, explicit consent, selected paths, lock/refusal cases and real large private directory | Full deletion transaction rollback, power loss and all Android filesystem behavior |
| Files/transfers | Current byte limits, exclusive outputs, choice ownership, direct-child cleanup and uncertain receipts | Guaranteed remote/proot descendant termination or exactly-once execution |
| Persistence | Shared FIFO AtomicFile writer and duplicate-ID refusal fixtures | Multi-process merging or durability of writes not committed before SIGKILL |
| Updater/entrypoints | Trusted HTTPS routing/cancel, package/signer/hash checks and bounded incoming files | End-to-end Android installer/data-survival verification on the final phone build |
| Build/compatibility | Same package/signer/schema8, ARM64, target28/min26; X11 guarded at30 | New application ID, target-SDK migration or store acceptance |

Reviewed the16-file diff against1341: named constants retain integer values;
new Android guards precede the annotated calls; no account/profile/permission or
process-stop path changed. Widget tests exercise custom icon placement/tint and
existing layouts; no superclass/theme switch was used to satisfy lint. Existing
read-only URI grants, exported-component restrictions, HTTPS policy and signing
identity are rechecked. Previous audit reports remain historical evidence of
specific fixes, not a claim of exhaustive full-device verification.

## Remaining work and minimum device check

This pass closes the cumulative automated-regression stage; it does not declare
the entire application free of defects. Prioritize settings context retention,
locale/document-provider path handling and lint-toolchain compatibility next.
Keep remote-job supervision separate from direct-child ownership: after a lost
file command, inspect both paths and remaining work before a manual retry.

After an explicitly chosen installation, verify agent input focus while messages
arrive, switch terminal/X11 and open/close the Android keyboard, then Copy/Move and
export a small disposable file. A fresh-device environment/desktop install and
an isolated SSH interruption test remain useful; do not erase a working rootfs
merely to manufacture a clean-install run. These source/build results do not establish installed-device behavior.

Compatibility EN: unchanged com.termux/signing identity/schema8 and Android app
minimum; no valid-data reset, account migration or environment replacement. The
existing duplicate-ID fail-closed behavior still applies to malformed libraries.
A future stable version must exceed1342, include localized compatibility notes
and generate update.json from its final APK. Live-operation migration is unproven.

Совместимость RU: com.termux, подпись, схема8 и минимум Android приложения
сохранены. Сброс валидных данных, перенос аккаунтов и замена окружений не нужны.
Библиотеки с повторяющимися ID по-прежнему отклоняются без очистки исходного файла.
X11 и раньше требовал Android11; перенос активных операций при обновлении не обещан.
