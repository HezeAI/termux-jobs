# Private candidate: X11 Auto setup

Candidate **2.0.5-wayland-test.7 / code 1373**, following test.6/code1372.
HezeAI reported the preceding scaling candidate works and requested a single
reset control. This follow-up changes the shared X11 screen-settings dialog.

The former Automatic resolution checkbox is renamed **Автонастройка / Auto setup**.
Checking it resets the form to automatic resolution, scale 100%, horizontal margin 0
and vertical margin 0. All five manual inputs and their labels are disabled/dimmed;
resolution inputs show an Auto hint. Unchecking enables the manual controls again.
Apply uses the canonical defaults object and the existing live acknowledgement
path; it does not parse any disabled input. Cancel leaves stored and running values
unchanged. During an apply operation the checkbox and inputs are disabled too,
so the form cannot change underneath an in-flight request. Failure unlocks the form
according to the checkbox state and preserves previous preferences.

The checkbox is initially selected only if all screen settings are defaults.
Older automatic-resolution configurations with a custom scale or margin open with
Auto setup off, preserving their values until the developer explicitly applies a
choice. No automatic migration or data reset is added. The setting remains scoped
to the selected X11 environment. Style, renderer, packages, agent sessions and the
experimental Wayland profile are unaffected.

Verification: 16 focused Java/Robolectric cases pass, including reset of every
field, disabled/dimmed controls, live acknowledgement/error preservation, cancel,
legacy configuration opening and the existing screen model/preferences checks.
`git diff --check` passes. Lint, assembly and final APK checks are recorded under
`.verification/x11-auto-setup/`; final artifact evidence is appended after checks.
Native/runtime scripts are unchanged from the preceding candidate. No additional
package installation or native/GUI stress runs are required for this dialog change.

Phone checks still needed for this candidate: from manual 75% and nonzero margins,
check Auto setup and Apply; verify scale 100%, zero margins and automatic geometry,
then check reopening/manual enablement and Cancel. Automated UI tests do not prove
physical rendering or keyboard behavior. Prior Wayland and toolkit-specific scale
limitations remain as documented in X11-SCREEN.md.

Same package/signing identity, ARM64 and schema; versionCode increases. No new
permission, migration or installer/process-stop behavior. APK update validation is
separate from an installed-device check or authorization to publish.

Modern lint 7.4.2 passed with zero errors and 212 existing warnings (one fewer
SetTextI18n warning after using explicit ROOT-locale formatting for numeric
input values). No lint suppression was added. Focused tests: 16, zero failures,
zero errors, zero skips. Review confirms that the reset changes only the selected
X11 screen form and uses the existing acknowledgement path; it adds no filesystem,
network, permission or process-management operation.

Final release APK assembled successfully. Verified package `com.termux`,
versionName `2.0.5-wayland-test.7`, versionCode 1373, ARM64 only,
ZIP integrity and the unchanged signing certificate. Existing stable JNI files
and pinned Wayland native inputs match; all packaged source assets match. Runtime
class/JNI callback checks passed. The private-file-name and secret-pattern scan
found no matches (not a proof against every possible secret).

APK: `Termux-Jobs-v2.0.5-wayland-test.7-arm64.apk` (52718993 bytes).
SHA-256: `5361f40b7efb69a6411cc50c7c06a054c71ec62087fcb82b6170a7c93dd686c7`.
