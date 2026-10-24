# Settings lifecycle audit — 2026-10-24

Private2.0.14-test/code1343 follows the
[cumulative regression pass](AUDIT-REGRESSION-2026-10-24.md), retaining its earlier
fixes. Public stable remains v2.0.2/code1331. Evidence lives in
`.verification/settings-audit-20261024/`. The production delta is12 settings source
files and version metadata; no runtime script, resource, native/process-stop,
agent, rootfs, permission or account mechanism is changed.

## Finding and implementation

Twelve package-private PreferenceDataStore adapters cached mInstance statically.
Their first Context came from the settings screen. That retained the original
Activity after destruction and reused its adapter on subsequent screens. Five
logging adapters also used that retained Context for feedback, so opening the
next screen in another language could still show the first screen's message.
A failed package-preference initialization also left a singleton with a null
backend until process exit.

Each fragment now binds a newly created adapter through PreferenceManager.
There is no global adapter cache, weak singleton or retention registry. The
SharedPreferences backend classes, package lookup, filenames, keys and modes
remain unchanged; stored values do not belong to the adapter object. Seven unused
duplicate Context fields are removed. Five logging adapters intentionally keep
their owning screen's Context while that screen is alive, for current-language
feedback. All preference factories still receive that live UI Context for the
existing package-error dialog; it was not replaced with an Application context.

The internal getInstance factories become stateless create methods. Their only
production callers were the fragments in the same files, which are updated
together. Public fragment class names/XML navigation and shared preference APIs
are unchanged. No setting is cleared, rewritten as a migration, or transferred
to another file/package. Screen creation does not start a session, install an
addon or introduce a worker/background timer.

## Before/after evidence

The corrected old-code run failed **6 of7** targeted cases. It reported all12
closed-screen adapters reused, found their static roots, demonstrated the stale
English log-level toast after reopening in Russian, and failed fresh-screen/
value-owner/missing-package recovery checks. This deterministically establishes
the retained-reference path; it is not a phone heap-size or GC timing measurement.
The keyboard/value cases failed ownership assertions; they do not demonstrate
prior preference data loss.

The first fixture run had an additional unsupported-operation failure: it tried
to put values through root adapters that intentionally implement no setter.
The fixture was corrected to use only each adapter's implemented operations;
its package-routing/unknown-key check then passed on old code. That test-only
correction is kept separately from the six reproduced ownership failures.

Final code passed **15 scoped JVM tests in4 classes**, no failures/errors/skips:

- Seven new lifecycle cases cover all12 adapters, a destroyed/reopened screen,
  absence of static adapter/Context roots, two simultaneous owners sharing the
  existing preference values, a real Fragment/SwitchPreference reopen, all five
  logging adapters' current-language feedback, missing-addon error/recovery, and
  exact package/CONTEXT_RESTRICTED lookup routing with unknown-key preservation.
- Four existing workspace-language cases retain resource/choice and input/draft
  behavior; two keyboard and two diagnostic-UI cases exercise adjacent UI paths.

The fixture uses real preference widgets/backends in a Robolectric screen with
private synthetic package contexts. Writes go to its test SharedPreferences;
no real addon installation/account/shared storage is used. The package-error
shadow records the live Context rather than dismissing a real process-exit
error dialog. It proves routing, not physical dialog/window/permission behavior.
No flaky force-GC assertion is used as a substitute for ownership evidence.

Built-in release lint completes with **0 errors/124 warnings**, down from136;
all12 StaticFieldLeak reports for these settings adapters are gone without a new
suppression. Existing AndroidX custom-registry/Kotlin incompatibilities remain,
so a full clean-lint result is not claimed. Aligning that tooling remains one of
the next two main audit tasks, after locale/document-provider paths.

Verified APK: `build/apk/Termux-Jobs-v2.0.14-test-arm64.apk`, 45,845,766 bytes;
SHA-256 `daf7d4d3515034b082d1211da6b95a3d0c05193a9fd763aa5d8e5828fbdb84b9`.
ZIP integrity, com.termux/code1343/name2.0.14-test/ARM64, signer continuity,
required runtime types, tested-source/APK-stage matching and bounded secret-
pattern checks passed. All assets/native libraries match1342. There are no
required missing runtime classes;31 reviewed optional types remain. Production
changes match the12 recorded settings files only, apart from version metadata.
The release build succeeded with existing Kotlin/custom-lint warnings, not a
complete clean-lint or installed-device/upgrade claim.

## Review and logical inspection

Traced fragment creation → live Context/package lookup → PreferenceManager binding
→ reads/writes/toasts → fragment/Activity destruction → a new screen/factory.
The former static roots are removed; a new Activity cannot obtain the first
screen's adapter. Normal references while a fragment/back stack is alive are
intentional. Backend package/keys and shared values remain the same across fresh
adapters, and independent live screens do not replace each other's UI Context.

Traced missing package/context failure and subsequent fresh initialization. The
same live UI Context reaches the existing error route; no Application-context
window-token substitution or new dialog/exit behavior is introduced. The test
isolates that route from actual app/process exit. Public APIs, theme/resource
inflation, preference visibility, log-level validation, permissions, user data
and session/process signaling are unchanged. New adapters are lightweight screen
objects; no custom disk cache or recurring/background task is added.

This removes the demonstrated settings singleton roots, not every possible
Android/framework/native retention path. Toast/looper/back-stack references can
remain during their ordinary lifetime. Garbage collection timing, physical RAM
usage and device frame time were not measured or promised. A backend or third-
party library retaining a Context independently needs its own evidence; this
stage does not claim to have profiled every object graph.

## Device boundary and remaining work

The1172-JVM/560-Python/31-Node cumulative results remain historical evidence for
1342, not a renamed full1343 rerun. This change is covered by the targeted final
suite and source/packaged-stage review. Real addon cross-package permissions,
actual error-dialog window behavior, installed upgrade data survival, physical
IME/gestures and heap profiling remain phone checks.

Minimum phone check: change a keyboard setting, close/reopen settings and verify
the value; reopen logging settings after changing app language and check feedback.
Existing workspaces, native sessions and environments need no reset/reinstallation.
Two main source audit tasks remain: locale/document-provider path handling, then
lint-toolchain compatibility and warning triage. The separate existing limits of
remote descendant termination/durable job recovery/target-SDK or application-ID
migration are not silently declared solved by this settings patch.

Compatibility EN: unchanged com.termux/signing identity/schema8, preference files
and keys, ARM64/minSDK26/target28, embedded X11 min30; no valid-data reset, account
migration or environment replacement. Installing an update during active work
still has no established live-operation migration guarantee.

Совместимость RU: com.termux, подпись, схема8, файлы и ключи настроек, требования
Android/ABI сохранены. Сброс валидных данных, перенос аккаунтов и замена окружений
не требуются. Перенос выполнявшихся операций между версиями не гарантируется.
