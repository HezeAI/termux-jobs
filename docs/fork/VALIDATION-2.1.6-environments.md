# Environment verification for 2.1.6 /1533

## Confirmed origin of the Arch error

The screenshot's error occurs before deletion, when the trash action requests a
scan/plan. `EnvironmentSetup` captures the selected row's `Choice`; the manager
passes `choice.alias` as a separate argument. `archlinux` opens
`containers/archlinux`, using descriptor-relative, no-follow traversal.
The old error text named Ubuntu unconditionally. This was not evidence of Ubuntu
being selected for deletion. A row-recreation JVM test verifies the Arch callback.
The corrected plan also checks the returned alias and container identity.

Read-only inspection found the installed Arch path `rootfs/run/systemd/dissect-root`
to be a directory, not a symlink, mode 000, owned by app UID 10620. Its installed
systemd tmpfiles configuration specifies this mode. The Ubuntu path was absent.
A native Python task launched by TermuxService had UID 10620 and TracerPid 0;
opening that Arch directory returned errno 13. Running Python inside PRoot instead
read it successfully, so that result was explicitly not treated as app evidence.
No working environment was deleted or chmodded by the diagnostic code.

The helper extracted from the published v2.1.5 APK matches the v2.1.5 tag byte for
byte. It reproduces SCAN_ACCESS_DENIED/13 on an isolated Arch-shaped fixture and
on a fresh real Arch install with the same sealed directory added. This is a
pre-existing defect, not a 2.1.6 regression. The screenshot's historical runtime
arguments were not logged; the conclusion combines source tracing and reproduction.

## Narrow fix and safety

Only the exact `rootfs/run/systemd/dissect-root` directory with mode 000 is omitted
from recursive inspection in the deletion plan. Other errors still fail the scan.
Shared tree walkers used by other operations retain their previous default.
The plan/confirmation explicitly disclose unscanned contents and incomplete size.
After the normal token/idle/path checks, kernel `rmdir` only removes an empty
placeholder, without reading it or changing its permissions. If it is nonempty,
the container returns to its original alias before recursive deletion begins.
The UI reports that refusal. No recursive permission workaround was added.

Native Termux fixtures check empty/nonempty cases, cancellation, external symlinks,
changed sources, busy/locked state, restoration and neighbouring environments.
18 native helper cases passed; one hardlink fixture was skipped because Android
rejects native hardlinks. All 19 passed in the local Linux/PRoot test context.
These are separate results, not a claim that the skipped native case passed.

## Real base installation cycles through Termux

Every row below used a separate container in a task-owned prefix, not a working
installation. The native CLI extracted the pinned archive; a shell printed a marker,
architecture and `/etc/os-release`. Cancellation retained the container; the final
plan/remove deleted only that test container and preserved a neighbouring sentinel.
Ubuntu AI used the APK's pinned 5.9.0 installer; shared OCI caches were allowed.
Arch/Manjaro also ran the production keyring setup, including the Manjaro config fix.
The first harness used OCI-only `run` for raw Arch/Manjaro archives and failed.
The corrected `login` path passed; the initial error remains in the evidence.

| UI environment / alias | Verified os-release ID | Install / shell / cancel / remove / neighbour |
| --- | --- | --- |
| ubuntu | ubuntu | PASS |
| debian | debian | PASS |
| alpine | alpine | PASS |
| fedora | fedora | PASS |
| opensuse | opensuse-leap | PASS |
| void | void | PASS |
| kali | kali | PASS |
| archlinux | archarm | PASS |
| manjaro | manjaro-arm | PASS |
| ubuntu-ai | ubuntu | PASS |

A second real Arch install adds the sealed mode-000 path: v2.1.5 refuses,
2.1.6 reports the omitted path and removes the empty placeholder/container.
Cancellation and neighbour preservation pass again. The nonempty case is separately
verified on a small native fixture. No permission broadening of the sealed directory
is used by the fix.

## Public downloads and recovery

All 10 catalog archives were freshly fetched by the production downloader, then
fetched independently through the unsigned public asset API. All sizes and SHA-256
values match the unchanged [catalog](../../app/src/main/assets/environment-images.json).
The downloader validates repository/tag/name/asset size/digest metadata and the
actual bytes before promoting the file. Corrupt data, mismatched ranges/metadata,
unsafe paths and non-HTTPS redirects are rejected. Verified caches are reused.

The first primary requests returned gateway 504; the production fallback succeeded.
A separate direct-only sweep (three attempts per failure) verified: debian, alpine.
Direct-only 504 failures remained for: ubuntu, fedora, opensuse, void, kali, archlinux, manjaro, ubuntu-ai.
Those direct routes are **not reported as passed**. Their complete application
paths passed through the verified fallback. This is observed gateway behavior,
not a claim about its remote cause or every user's connection.

A live Alpine transfer was interrupted after 65,536 bytes. Cancellation retained
the partial file; the next invocation requested Range and received HTTP 206.
A second real transfer with an injected connection reset resumed through the public
API using HTTP 206. Both complete files matched the pinned hash. The fault is
injected at the real HTTPS stream; it is not an uncontrolled network-outage test.
Local deterministic tests additionally cover truncation, repeated download,
server-ignored Range, corrupt archives, cancellation and invalid metadata.

## CI-only musl headers

[Components header release](https://github.com/HezeAI/termux-jobs-components/releases/tag/x11-build-headers-musl-1.2.6-r2)
contains the original musl-dev 1.2.6-r2 APK plus license, manifest and checksums.
Outer archive SHA-256:
`bb06e5670c50a87cebd3d7d1446909567f5165ee717f880e723515f697f5d765`.
Inner original APK SHA-256:
`ca72b84f4eb3c36bbbec04d919eb10159d414c7a43413cc2253841d301fa9486`.
The inner bytes equal the previous release's cached input. Alpine removed that r2
URL; pinned app rootfs/XFCE archives remain in Components and use runtime musl r3.
CI verifies the full Alpine rootfs/XFCE archive and all 378 package members.
The build-header archive is not an application release asset and is not installed
into Alpine. No existing component archive was replaced or duplicated.

## Scope and evidence

- **Phone UI:** the developer accepted test10. The new deletion dialog and final
  APK are not claimed as independently clicked/installed on the phone.
- **Native Termux on the phone:** real UID 10620, TracerPid 0; read-only original
  metadata/open probe, isolated base installs, basic shell launches and deletion.
  These were background app-service tasks, not GUI interaction or root-shell tests.
- **Isolated local/CI:** Java row-selection/error tests; downloader integrity and
  cancellation fixtures; Linux helper and installer-supervision regression tests.
- No desktop/GPU launch, real-model call, rollback installation or full factory-reset
  bootstrap was performed. No working session was restarted or stopped.

Detailed local evidence: `.verification/release-v2.1.6/environment-audit/`:
`baseline-identity.json`, `native-probe.json`, `native-removal.json`,
`native-real-arch-sealed.json`, `environment-summary.json`, `all-downloads.json`,
`canonical-downloads.json`, `live-network-recovery.json`, and native/local logs.
Release gates and final CI results: [main validation](VALIDATION-v2.1.6.md).
