# X11 rotation configuration delivery fix — 2026-10-24

Candidate2.1.0/code1347 supersedes the unpublished code1346 candidate. The developer's
latest screenshot showed landscape app tabs and the desktop confined to its
embedded area. Stable remains2.0.2. Evidence: `.verification/landscape-fix-20261024/`.

## Finding and correction

Fullscreen detection reads Activity orientation, while the compositor host reads
WindowMetrics from its Context. The UI language wrapper copied the complete
configuration at creation, including orientation, window bounds, density and UI
mode. A localized context must override language alone. The old plain-Activity
window tests manually changed its Resources and did not exercise this boundary.

The wrapper and cached English-string context now use locale-only overrides.
TermuxActivity explicitly handles the system configuration callback: after super,
it applies a copy of the delivered configuration with the currently selected UI
language to its existing Resources. It does not recreate the Activity, workspace,
composer, X11 service or terminal sessions. The incoming configuration is not
mutated. A later language choice remains effective on the next configuration
event. Rotation detection and compositor metrics read the updated resources.

No native, driver, desktop package, rootfs, permission, network or account change
is part of this correction. The original per-channel fullscreen presenter,
ownership guards and same-host restoration are retained. This configuration
callback also runs on ordinary app screens; its scope is resource metadata only.

## Testing, review and limits

The scoped configuration/window tests cover language choices RU/EN, real
TermuxActivity callback delivery without running its onCreate/session bootstrap,
portrait/landscape dimensions, density, night mode, a later language choice,
preservation of the incoming event and fullscreen/restoration through a localized
Activity fixture. The corrected scoped run passed13 cases before the last extra
language-switch assertions, which are included in the final confirmation.

A negative control omitted the forwarding call in the disposable test stage;
the new real-Activity configuration case failed on orientation. Production source
was not modified for that control. The first added qualifier fixture had an
invalid Android qualifier order and was corrected. A locale-only change by itself
did not satisfy the host Robolectric propagation test; explicit callback delivery
is tested rather than claiming that RuntimeEnvironment.setQualifiers reproduces
every Android ResourcesManager update. These failures are retained in evidence.

Final confirmation passed1,224 JVM cases/184classes (app1,070 plus emulator154),
no failures/errors/skips. Modern lint:0 errors/209warnings. Eight checker and
six X11-contract cases pass; localization584 plus key/format parity passes. ZIP,
package/code1347/signature/ARM64/runtime/source-stage and bounded secret checks
pass; native/assets match the archived1346 artifact.

Final suite, lint and artifact results are recorded in
[current release validation](VALIDATION-v2.1.0.md). Device validation of1346 failed
and is not counted as a success. The new APK still needs a physical rotation,
Back/portrait restoration and keyboard/settings check. Hardware SurfaceControl,
IME, cutout and GPU composition are not established by the surrogate-window tests.
