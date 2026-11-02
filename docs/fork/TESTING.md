# Choosing verification scope

This policy applies to development checks, intermediate APKs and public releases.
It supplements [AGENTS.md](../../AGENTS.md), [build notes](../../FORK.md),
[Lint](LINT.md) and [release requirements](RELEASING.md). It does not authorize
installing an APK, changing a user's environment, stopping sessions or publishing.

## Start with the change and its dependencies

Before editing, identify the affected behavior, entry points, callers, shared
helpers, build/runtime dependencies and meaningful risks. Include indirect
dependencies: chat layout can share host geometry with X11; an instruction editor
can share persistence or permission/command execution code. A filename or feature
label alone is not a sufficient scope decision. Record a short verification plan.

Run tests for those paths and meaningful failures. Use the full relevant suite
for broad changes, changes to common mechanisms, or dependencies that cannot be
bounded confidently. Expand the plan when review or a failure exposes another
affected path. A full automated suite does not mean launching every desktop,
installing every distribution or exercising every driver without relevance or
authorization.

Do not launch X11, environment installation, GPU/driver or component integration
checks solely because an instruction editor or chat formatting changed. Include
them when a traced dependency warrants them. Build-time verification of packaged
X11 hashes is different from running an X11 server or a GPU test; do not remove
packaging guards to avoid an unrelated functional test.

Changes to process ownership, privileges, cancellation, termination, recovery or
session isolation retain their meaningful security and lifecycle checks, including
unrelated-process survival and error/cancel races. A small diff does not reduce
these obligations. Known critical regressions or unresolved termination safety
block completion.

## Intermediate checks and static analysis

| Change | Minimum scope to assess and record |
| --- | --- |
| Documentation only | Review wording against implementation, links, diff/whitespace and preservation of existing edits. No APK rebuild or Lint run solely for prose. |
| Local Java/UI/resource behavior | Compile affected code/resources, relevant JVM/UI tests, source/security review, applicable locale/resource checks. Choose compatible targeted Lint where its coverage is understood; otherwise use the existing full modern Lint runner. |
| Python/Node/shell helper | Relevant helper tests and syntax/static checks; include callers, trust boundaries and packaging when changed. No unrelated desktop/driver execution. |
| Manifest, Gradle, shared dependencies, runtime or common lifecycle | Broaden to affected consumers and variants, dependency/runtime review and appropriate modern Lint. Use the full relevant suite when impact is broad or uncertain. |
| Native/desktop/installer/driver behavior | Relevant build, archive/ABI and integration checks, including clean fixtures and failure/cancel cases where applicable. Distinguish fixtures from real device graphics. |

For each intermediate build, state which static checks were selected and why,
what they cover, and what remains unchecked. Successful compilation is not Lint.
The current modern-Lint runner always analyzes all five modules; it is a valid
conservative choice, not a claimed incremental analyzer. An unavailable or omitted
check is recorded as unverified, never as PASS.

Before every public release, run the full compatible modern Lint gate described
in [LINT.md](LINT.md), including all five modules, dependency analysis and report
infrastructure checks. Targeted intermediate analysis and old Lint reports do
not replace this gate. Preserve warnings and toolchain limitations in the report.

## Reusing evidence

Reuse a previous result only after confirming equality of the inputs significant
to that result: relevant production and test/fixture/helper sources, resources,
manifests, configuration and build flags, resolved dependencies, toolchain and
native inputs, ABI/variant, and relevant execution conditions. Include dirty and
new files; commit ID, timestamps, Gradle UP-TO-DATE or an old success log alone
do not prove this equality. Use small input inventories with paths/hashes and
sanitized tool/condition summaries rather than copying an SDK or a rootfs.

For example, a JNI output can be reused if its sources, headers/sysroot, compiler,
linker, flags and input archives match its recorded build and its output hash
still matches. Functional graphics evidence additionally depends on renderer,
driver, server, environment and device conditions. Identical sources cannot prove
that an upstream repository, network, Android permission or live server is still
unchanged. Recheck such state when the current claim depends on it.

When significant inputs cannot be established, rerun the affected check or record
the gap. Never copy a successful result into a new run as though it executed.
Use these separate labels in every validation summary:

- **Проверено заново / Checked anew:** check, tested input identity, result.
- **Переиспользовано / Reused:** original evidence, compared significant inputs
  and conditions, equality proof, and limits of the carried result.
- **Не проверено / Not checked:** missing/omitted check, reason and smallest
  remaining check. An out-of-scope check is not a failure or an implied success.

## Every delivered APK

Verify the actual final APK after its last packaging/signing operation, including
intermediate builds: archive integrity, package ID, versionName/versionCode, ABI,
debug status, signing certificate and signature, SHA-256, and applicable packaged
runtime/DEX/native/asset requirements. Compare package/certificate and increasing
versionCode for an update. Inspect for unintended private data and credentials.
Functional evidence for unchanged components does not waive this final-file gate.

`scripts/verify-apk-runtime.py` inventories DEX definitions/references and reviewed
optional types; it does not verify all APK identity/signing properties or prove
method linkage/device behavior. Use it on the final file where applicable alongside
archive, manifest, ABI and signature inspection. `build-debug.sh` and
`build-local-arm64.sh` do not implement the whole delivery checklist themselves.
Release updater metadata and checksums must refer to that same verified file.

## Existing entry points and minimal separation proposal

The following describes the inspected scripts, not an implemented new scheduler.
Scripts and their integrity gates remain unchanged by this documentation update.

| Entry point | Current unconditional work and implication |
| --- | --- |
| `scripts/check.sh` | Requests the four modules' JVM suites. Useful as a broad entry point; too broad as the automatic response to a narrow change. It does not cover every Python/Node/native/device check. |
| `scripts/verify-java.sh` | Runs `prepare-x11.py`, recreates the Java source stage, then requests the four modules' JVM suites. Even a narrow chat test therefore prepares graphics inputs and runs unrelated JVM cases. |
| `scripts/prepare-x11.py` | Validates cached/downloaded inputs but also regenerates keyboard/style outputs, extracts headers/codecs and recompiles compatibility shims on each call. It verifies optional bridge archives even when their outputs are not bundled. It does not launch a desktop or install guest packages. |
| `scripts/build-local-arm64.sh` | Always prepares X11, stages sources and compiles/links both Termux JNI libraries, then packages and audits DEX runtime. Unchanged native inputs could avoid rebuilding once their full build identity is proven. It also deletes/replaces named packaging outputs; this is not a general cleanup authorization. |
| `scripts/prepare-java-verification.py` | Recreates staged module `src` trees and copies project sources, preserving build caches. It does not copy an SDK or Linux environment, and does not itself execute tests. Do not restage while a build uses that stage. |
| `scripts/verify-modern-lint.sh` | Recreates the analysis source stage and runs the complete five-module modern Lint check. Required before public release; potentially broader than an intermediate change needs. |
| `.github/workflows/fork-ci.yml` | Requests toolchain setup, localization, broad JVM tests and a debug APK for every matching push/PR, including documentation-only changes. Its debug artifact is not a fully verified release APK. |

Minimal follow-up: keep input preparation, source staging, selected test execution,
APK packaging, final artifact audit and full release Lint as explicit steps.
For an already prepared, input-verified stage, invoke existing Gradle test tasks
with class filters (`--tests`) or the relevant Python/Node test file. Preparing
all graphics assets need not be a side effect of selecting a chat test. When
changing automation, add a fail-closed input/output identity check for reuse;
missing/mismatched inputs trigger explicit preparation or an actionable failure.
Keep required packaging/runtime checks active. Do not introduce unconditional
skip switches, fabricated PASS records or a complex new verification framework.

Reuse the existing repository-local SDK/JDK, dependency caches and verified input
archives across runs. Add only missing required SDK components in the permitted
local tool directory. Do not create full SDK/rootfs copies for each check. Keep
compact input hashes, commands, tool versions, check results, artifact identity
and essential failure excerpts. Preserve existing evidence, generated files and
user data; do not run cleanup or stage/output deletion merely to apply this policy.
