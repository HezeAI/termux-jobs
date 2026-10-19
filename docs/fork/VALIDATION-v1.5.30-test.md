# v1.5.30-test — launch environment preparation

Private update from v1.5.29-test/code1187 to code1188. Package com.termux,
Android8+/API26, ARM64. Library schema remains8. No data clearing or migration.
This is not a GitHub release.

## Behavior and boundaries

A manual Launch environment check row follows the environment selector in the
session/preset editor. It inventories only the selected Ubuntu agent components;
a right checkmark means components found, not valid authentication, billing or
model quota. Changing agent/auth/cloud metadata invalidates the UI result. Closed
editors and cancelled checks ignore late results. The owned read-only subprocess
has a25s timeout plus2s kill grace; cancellation suppresses UI delivery while the
bounded read-only query finishes.

Launch/preset launch repeats inventory and offers missing-component installation
before creating the new terminal. Consent is transient and is neither serialized
nor copied. Host setup can install missing proot-distro/Ubuntu only after approval;
it never removes an existing rootfs. Guest setup installs missing base APT packages
using native locks, without package upgrade or lock deletion, then uses the
existing agent-specific bootstrap. These base packages belong to shared Ubuntu,
while agent CLI runtimes are private under ~/.local/share/termux-jobs.

Existing global Codex is retained; a missing CLI uses a pinned private0.163.0
platform package. Node fallback also serves proxy/history commands, including
when a global Node is too old. Codex permissions and native chat protocol remain
unchanged. Ordinary Termux/Ubuntu/SSH sessions and legacy Codex-Termux sessions
bypass the new setup wrapper.

Vertex without known Cloud credentials can install pinned private gcloud589.0.0
and missing Python, then runs native ADC browser approval in its terminal.
Existing ADC/service-account/Express credentials skip login. This existence check
does not validate the credential. Native auth uses a kernel-held lock and rechecks
after acquiring it. Gemini API keys do not count as Vertex credentials. Region
selection offers common locations plus validated custom input. Cloud projects,
IAM, enabled APIs and billing remain the user's Cloud configuration.

Downloads for new private runtimes use HTTPS and pinned SHA256 verification before
extraction. Codex official npm integrity metadata and gcloud embedded VERSION were
checked in repository-private reference archives; those archives are not APK assets.
Stage directories are unique to each invocation. Failed checks/downloads/extraction
stop setup instead of proceeding to launch. Original per-agent bootstraps are
retained. Existing unusable private Codex/Cloud runtime directories are preserved
and reported rather than overwritten.

## Source and isolated verification

98 focused JVM tests passed, zero failures/errors/skips. New cases cover bounded
allowlisted inventory parsing, incomplete/nonzero checks, conservative missing
components, ignored arbitrary output, auth-needed independent of ready components,
non-inherited consent, ordinary/legacy session bypass, quoted Vertex wrapper and
metadata identity, and cancelled-check late callback suppression. Existing agent
launch/card/auth/editor/navigation cases also passed.

Isolated shell fixtures passed: read-only inventory, no printed credential values,
Gemini key not substituting for Vertex credentials, consent guards, missing-only
APT mapping, update failure stopping install, corrupt Codex archive rejection
before extraction/publication even under conditional-source shell semantics,
existing ADC preservation and host install consent without rootfs deletion.
No real APT/pkg/Cloud auth or inference was used by these fixtures.

Vertex native-auth regression passed, including scoped exports, ADC/Express,
external policy refusal and exact pinned native validation/generator functions.
A repository-owned PTY fixture closed only its own outer shell/PTY; its foreground
worker and child exited. This is Linux PTY evidence, not an Android/proot device
test of package-install interruption. No broad kill-by-name/group was added.

## Device checks still needed

Check the manual row and checkmark in both creation editors. Switch agents/auth
after a successful check and confirm the mark clears. Launch an already prepared
agent and verify no installation confirmation. In a disposable fresh environment,
confirm missing-component consent, visible terminal progress, failure/retry, and
closing the preparation session without affecting another session. Verify native
Vertex browser/ADC login, Cloud project access, region/model availability and a
real message with the developer's own account; these are not covered by fixtures.
No performance improvement or exhaustive device compatibility is claimed.

## Compatibility

No automatic user-data reset; current history/presets remain in the existing
schema. Package/certificate/version identity must pass final artifact audit below.
Historical Vertex downgrade caveat from v1.5.29 remains: older builds may not
understand the new auth enum; switch such profiles back to API and back up before
an intentional downgrade. Runtime files installed by an approved future launch
remain in Ubuntu across app updates; updating alone does not install them.

## Artifact

Release assembly and runtime-definition audit passed: 31 remaining optional types
have existing explicit review notes. Gradle emitted the existing Kotlin/UAST/JNA
Lint compatibility warnings; this is not a claim of clean Lint. Final Java source
matches both test and build staging. ZIP integrity, package, minSdk, ABI, release
signature and versionCode increase passed. Native libraries and all previous
assets are byte-identical to v1.5.29; only four new setup shell assets were added.
Private data filenames/key markers and development archives were absent in the
artifact scan. This scan does not prove the absence of every possible secret.

APK: `Termux-Jobs-v1.5.30-test-arm64.apk`; 36646157 bytes.
SHA256: `ce5a143cca15a45501aef073c8323d021adf6d200757f7b706315b58a4980545`.
Certificate SHA256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Package `com.termux`, versionCode1188, Android8+, ARM64. The identity permits
updating over code1187; actual installation/migration still needs device checking.
Evidence: `.verification/agent-setup-v1.5.30/` (test results, shell fixtures,
PTY probe, downloads provenance, build log, signature and APK audit).
