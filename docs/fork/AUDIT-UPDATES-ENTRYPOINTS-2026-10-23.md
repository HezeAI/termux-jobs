# Update and external-file entrypoint audit — 2026-10-23

Private2.0.8-test/code1337 adds targeted fixes to the preceding
[persistence](AUDIT-PERSISTENCE-2026-10-23.md),
[transfer](AUDIT-TRANSFERS-2026-10-23.md),
[storage](AUDIT-STORAGE-2026-10-23.md),
[environment/X11](AUDIT-ENVIRONMENTS-X11-2026-10-23.md) and
[agent-chat](AUDIT-AGENT-CHATS-2026-10-23.md) audits.
Public stable remains v2.0.2/code1331. This report concerns source inspection,
repository-private fixtures and an isolated APK build; it does not claim an
end-to-end phone update, fresh-device setup or exhaustive Android security review.

## Findings and changes

**External file names could escape the receive directory or overwrite files.**
The exported share receiver used a provider's display name/editable title as a
relative path, then opened it with truncation. A user still had to confirm saving,
but a hostile suggested name could point outside `$HOME/downloads`; an existing
file, symbolic link or hard link could also be overwritten by an ordinary name.

Receive now requires a single nonempty basename (no dot/dot-dot, separators or
NUL). The final file is created with the Android `Os.open` contract
`O_WRONLY|O_CREAT|O_EXCL|O_NOFOLLOW`, mode0600. Existing destinations, including
links, fail without modification. Choose another name to save a duplicate: this
is an intentional change from silent replacement. A failed write removes only
the newly created partial file. Streams close on save, rejection and Activity
destruction/cancel. The receive-directory root remains the user's configured
Termux home location; this does not attempt to sandbox malicious same-UID shell
processes that can already alter the application's private directories.

**Updater cancellation only set a flag and connection exceptions could leak.**
A flag was examined after reads, so a blocked connect/read or redirect chain
continued after Cancel. `getResponseCode()` could also throw before disconnect.
Each operation now owns its `UpdateConnection`: cancel marks that operation and
requests disconnection of its active transport; every redirect, HTTP error and
exception releases the connection. Cancellation is checked before opening,
after attachment and around response delivery. Another operation is unaffected.
The existing15s connect/20s read timeouts remain fallback bounds, not a promise
that every Android network stack interrupts immediately. The updater's busy gate
remains held until its worker exits. Global shell proxies/hosts are not changed.

**Cached APK bytes were not rehashed after an install-permission round trip.**
The old Install callback repeated package/version/signer checks, but not SHA-256.
The final handoff now rechecks exact size/hash and current package/version/signer
on a background worker, then grants the Android installer read-only content-URI
access with ClipData. A deleted/changed cache, now-obsolete version or closed
screen cannot use that handoff. Failure explains that the update must be downloaded
again. Android retains its normal final signature verification/user confirmation.

## Boundary review

- Release metadata remains tied to the exact HezeAI repository/tag/asset names.
  Redirects remain HTTPS-only and restricted to the four existing GitHub hosts;
  credentials in URLs, alternate ports, lookalike hosts and HTTP are refused.
  TLS certificate validation was not disabled or replaced.
- The fork FileProvider remains nonexported and exposes only `workspace-share`,
  `workspace-updates` and `chat-apk-artifacts` under app cache. Installer handoff
  grants read access only; arbitrary private files remain outside its roots.
- RUN_COMMAND service remains guarded by the existing dangerous permission and
  `allow-external-apps` check. The upstream open provider also checks that policy;
  DocumentsProvider retains the system MANAGE_DOCUMENTS guard. Exported settings,
  launcher and user-confirmed share activity remain intentional upstream entrypoints.
- No manifest permission, package identity, target SDK, rootfs, agent account,
  server configuration or process/session stop mechanism was changed.

## Evidence

Final code passed **35 scoped tests in7 classes**, with no failures/errors/skips.
APK `build/apk/Termux-Jobs-v2.0.8-test-arm64.apk`:45,836,026bytes,
SHA-256 `32821ff38c881ca0fa73e5302d06c136fee0b92e8a5148b2994d33a4c528515f`.
ZIP integrity, package/version/ARM64, signature continuity with1336, required runtime
references and bounded secret-pattern checks passed. All assets/native libraries
match1336. The build succeeded; existing custom-lint/Kotlin metadata warnings
remain and are not reported as clean lint. Tested/staged production bytes match.
The preceding full938-test suite is1336 evidence, not a full suite rerun for1337.

Detailed checks are recorded in VALIDATION.md and the private
`.verification/update-entrypoints-audit-20261023/` directory. Prior-code receiver
reproduction: five of six scenarios failed, including traversal, existing/link
replacement and failed-write residue; ordinary Unicode-name saving passed.

The first fixed-code run exposed a fixture limitation: Robolectric4.4's `Os.open`
shadow ignores exclusive/no-follow flags. The receiver fixture now models that
specific syscall contract explicitly and asserts the requested flags/mode; real
private files still exercise the surrounding write, failure and cleanup logic.
A separate host POSIX check verifies preservation of regular/symbolic/hard-link
files, exclusive concurrent creation (exactly one winner) and0600 mode. This is
not relabeled as physical Android syscall/GUI verification.

Network fixtures use fake HttpURLConnection objects: pre-open cancellation,
cancel during attachment/blocked response, trusted/untrusted redirects, redirect
loops, response exceptions and operation isolation. They contact no live server.
APK fixtures cover size/hash replacement/deletion, current version/signer/package
validation and installer handoff with mocked package metadata. A synthetic ZIP is
not a cryptographically signed Android APK. Final build verification separately
uses apksigner on the actual generated artifact.

## Logical inspection and limits

Traced share intent → name confirmation → exclusive create → copy/error/close,
and check/download → redirect/read/cancel → verification → permission settings →
background revalidation → read-only installer handoff. No automatic APK install,
data wipe, replacement of installed files or publishing was performed by these
code paths or fixtures.

Cache can be evicted by Android or a later updater download while an old offer is
open; the repeated verification fails closed and asks for a new download. This
stage does not implement durable pending installer ownership or resumable updates.
Existing external share copies still run synchronously; responsiveness for very
large/slow-provider files needs a separate lifecycle-aware I/O change and phone
testing. Activity/process death can discard dialogs and in-progress updates.
Request cancellation does not cancel arbitrary other application network traffic.
The broad upstream RUN_COMMAND capability remains powerful when explicitly
granted/enabled; this report does not establish a sandbox for trusted plugins.

Minimal phone follow-up: share a small file twice (second save must refuse
replacement), cancel an update check/download and retry after its worker ends,
and verify the Android permission/installer screen while keeping account/workspace
data. Actual installation and data survival require the developer's device check.
No developer environment was changed to manufacture those results.

Compatibility notes for a future stable updater (include both languages):
- EN: Valid workspace/account/environment data requires no reset. Incoming shared
  files no longer overwrite existing names; choose another name. Malformed
  duplicate-ID workspace files retain the earlier audit's read-only handling.
- RU: Корректные данные рабочих областей, аккаунтов и окружений не требуют сброса.
  Входящие файлы больше не перезаписывают существующие имена: выберите другое имя.
  Для повреждённых библиотек с повторяющимися ID сохраняется режим только чтения.

Developer: HezeAI. AI development assistance: OpenAI Codex
