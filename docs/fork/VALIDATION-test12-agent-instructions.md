# Private test12 validation — agent instruction editor

Date: 2026-11-01. Private APK authorized for developer phone testing; no public
release or APK installation. HezeAI is the developer; Codex assisted with this
implementation. [Feature and timing](AGENT-INSTRUCTIONS.md),
[changes](../../CHANGELOG.md) and [verification policy](TESTING.md).

## Final artifact

| Field | Value |
| --- | --- |
| File | Termux-Jobs-test12.apk |
| Download | /sdcard/Download/Termux-Jobs-test12.apk |
| package ID | com.termux |
| versionName / versionCode | 2.1.1-test12 / 1444 |
| ABI | arm64-v8a only |
| Bytes | 39,927,692 |
| SHA-256 | 879c8d659fa2807022e519af9fb2a773d46ae6c3dc243659b89f72913de2608c |
| Signing certificate SHA-256 | dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644 |
| Compatibility | same signer/package, schema8, Android8+ app / Android11+ X11 |

The final archive passed CRC/integrity, non-debug manifest, package/version/ABI,
cryptographic signature/certificate comparison, SHA-256, required DEX runtime,
99 source-asset comparisons, expected X11/runtime pins and secret/forbidden-file
checks. New instruction UI/runtime classes are present. The copy in Download was
read back and matched the final APK size/hash. No automatic app-data reset,
account migration, environment replacement or chat restart is introduced.
Workspace persistence tests passed; Android installer/device acceptance is separate.

## Checked anew / Проверено заново

- **116 JVM tests, zero failures/errors/skips**, covering instruction UI/save/
  cancel/history/template/storage addition/conflicts, actual global-settings
  entry, Codex launch/RPC/control, preparation/connection, environment routing,
  workspace data/navigation and owned-process lifecycle. Earlier UI close test
  needed its posted dismiss callback drained; the corrected complete selected
  suite passed. Mocked UI tests do not establish real IME/viewport behavior.
- **26 Python file-editor tests**, including exact UTF-8/BOM/CRLF/empty text,
  initial seed, existing/empty user-file preservation on upgrade, custom CODEX_HOME,
  active override/masked project files, history and restoration, concurrent and
  external conflicts, write failures, unsafe links, path-bound history, auth-free
  diagnostics, unknown/absent CLI saving, and explicit storage-section addition.
- Existing agent-preparation shell fixtures plus new Python-required Ubuntu AI,
  legacy-Ubuntu scope and existing-Codex preservation cases passed. APT/pkg/auth/
  network are replaced by task-private fixtures; no real package transaction or
  installed environment was changed. Shell syntax checks passed.
- Actual **Codex0.163.0 and0.164.0** app-server processes, isolated CODEX_HOME and
  Git-project fixtures, and a local fake Responses provider confirmed marker
  delivery for initial/new threads, a current-thread next request and a saved
  thread resumed by a new process. The editor wrote the exact source files used
  by Codex. Shared markers refreshed between turns; project markers refreshed on
  new-process resume; older history remained. The fixture used no existing auth
  profile or real model API. This tests CLI instruction delivery, not whether a
  real model semantically obeys arbitrary rules. Unknown versions are not promised
  live refresh and remain editable.
- Full compatible modern Lint across all five modules: **0 errors /225 warnings**,
  no rule-loading/infrastructure failures. This is conservative static coverage
  for the intermediate UI/file-handling change. No blanket suppression was added.
- Localization checks: **584 workspace strings**, settings keys and format arguments.
- Final source matches both APK and Lint stages (**776 source files**), relevant
  source/permission/trust/lifecycle review, documentation links and diff checks.

Detailed compact evidence is in `.verification/agent-instructions/`: final build,
JVM/Python/shell/localization/Lint reports, native fixture results, source identity,
artifact report, reuse inventory and Download verification. Fake-process lifecycle
fixtures intercept destruction; real fixture process termination only uses owned
Popen handles. No process discovery/broad kill is added to the editor.

## Reused / Переиспользовано

Existing repository-local SDK/JDK/Gradle/dependency caches, verified bootstrap and
native outputs are reused. Task-start and final hashes confirm unchanged graphics,
chat transport, connection wrapper, ordinary SessionShell and workspace store
sources. All three JNI libraries and packaged X11 assets match public v2.1.0
byte-for-byte. Packaging/runtime checks were rerun on this final APK. Reuse is
binary/source identity evidence, not a new GPU performance or phone GUI claim.
No new full SDK, Linux rootfs or model copy was created for testing.

## Not checked / Не проверено

Phone acceptance remains: editor visibility/size, keyboard, scrolling, long text,
rotation, actual file access and persistence after app/agent restart. Confirm
new/resumed chats and diagnostics against the installed device's CLI version and
custom connection command. An arbitrary wrapper may change CODEX_HOME after the
standard environment examined by the editor. Project trust, custom fallback files,
configuration replacements and Codex size limits remain distinct from source-file
editing. The diagnostic reports configuration presence without exposing values.

X11/desktop launches, driver tests, environment installations and all-distribution
package transactions were outside this change and were not run. No installed
rootfs, working chat, tunnel or server service was restarted for these checks.
The feature does not implement automatic cleanup, quotas, retention or deletion
of existing instruction history/user files. No APK was installed or published.

AI development assistance: OpenAI Codex
