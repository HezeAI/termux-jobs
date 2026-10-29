# v2.1.0 validation

Release authorization: HezeAI explicitly requested publication as v2.1.0 after
the private test11 delivery. HezeAI is the fork developer; Codex assisted with
implementation and verification. Automated evidence and reported device tests
are distinguished below; this is not a claim of exhaustive device validation.

## Final APK

| Field | Value |
| --- | --- |
| File | Termux-Jobs-v2.1.0-arm64.apk |
| Package | com.termux |
| versionName / versionCode | 2.1.0 / 1443 |
| Bytes | 39,903,643 |
| SHA-256 | e835b7425a97eb33dcea9bc71d40c1588182b77610ed067b0f49dc53e3842b89 |
| Certificate SHA-256 | dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644 |
| ABI | arm64-v8a only |
| Requirements | Android8+ app / Android11+ X11, min26/target28/compile35 |
| Workspace data | schema8 retained |

ZIP CRC, non-debug manifest, ABI, signing identity, required DEX definitions,
96 source assets, runtime payload pins and forbidden-file/secret-pattern checks
passed. All three JNI libraries retain the previous baseline. Every assets/lib/res
entry matches test11 byte-for-byte; the stable application change is release
version metadata. Local AI, VM and retired Wayland payloads are absent.

## Relevant checks

- Stable isolated release build succeeded and repeated **128 JVM checks** with
  zero failures/errors. Selected-menu state, official-image request/confirmation,
  update service, X11 profiles, documentation, chats and aliases are included.
- Stable source repeated **41 Python checks**: official initial/replacement paths,
  complete checkpoints/homes/rollback, missing required desktop rejection,
  expired/changed plans, cancellation, recovery, private path aliases, managed
  links and owned process-group termination with an unrelated guard surviving.
- **Four Node suites** repeated: agent action previews/live events, attachments
  and generated-file cards. No real provider authentication was used by fixtures.
- Independent modern lint7.4.2/Gradle7.5.1 covered **all five modules**: **0 errors,
  225 warnings**, no rule-loading/infrastructure failures. Warnings include
  existing UI/localization/accessibility/resource/API patterns. SDK28/package-ID
  architectural limitations remain; no blanket warning baseline was added.
- All **35 unique pinned Components assets** matched their published URLs, sizes
  and GitHub SHA-256 metadata across five immutable releases. This check did not
  redownload all large component archives or alter any published component.
- Candidate evidence retained in [test11 validation](VALIDATION-test11-official-install.md):
  117 Python checks plus its final41-case rerun, real isolated APT/dpkg
  transactions from empty fixture databases/caches, a repeat complete required
  desktop-package request, actual upstream ARM64 OCI download/native extraction,
  and official Arch signature verification with tampered-content refusal.
  APT packages were script-free fixtures with executable placeholders, not a
  full GUI. Native extraction prohibited guest subprocesses and is not a nested
  PRoot CLI-startup test. The full Arch rootfs was not downloaded for the small
  signed-package proof.
- Source/runtime asset comparison, tracked plus new-file secret review, release
  links, metadata consistency and git diff whitespace checks were performed.

Build/lint/fixture evidence is kept in project-private
`.verification/release-v2.1.0/` and `.verification/official-install/`.

## Sources and trust

Base/XFCE provenance is documented in [pinned environments](PINNED-ENVIRONMENTS.md)
and [Components](https://github.com/HezeAI/termux-jobs-components#readme).
Official OCI blobs require matching SHA-256 and LinuxARM64 metadata; Manjaro
archives require exact published hashes/sizes. Arch Linux ARM uses the official
public signing key, fingerprint `68B3537F39A313B3E574D06777193F152BDBE6A6`, from
[official keyring git blob](https://api.github.com/repos/archlinuxarm/archlinuxarm-keyring/git/blobs/6b352c02e42b64d1867e265558e582f921ca05e8).
Its bundled key asset SHA-256 is
`4e4202772328674af68f90f675e077d192337fc8ced15cddcd35875a9de5b5f9`.
Only this public key is bundled. HTTPS verification stays enabled and no
third-party CA or credentials are added.

## Data compatibility and device evidence

The APK keeps package/signing identity and schema8. No automatic reset, account
migration, rootfs replacement or reinstallation is required. Installing the APK
alone preserves histories, presets, projects, commands, notes, drivers and
environments. Image replacement is a separate explicit operation: its confirmation
explains the changed OS/program set, optional home carry and complete old-system
checkpoint. Snapshots may contain credentials and consume substantial storage.
Native Termux and Ubuntu AI are excluded from these replacement/update controls.

HezeAI reported working Kali/Manjaro desktop/program/graphics integration during
private testing. Automated tests do not establish fresh-device keyboard, gestures,
WebView rendering, permission/notification behavior or all GPU/driver combinations.
Successful online DNF/Zypper/XBPS/APK/Pacman transactions and GUI/renderers for
every new upstream image remain separate acceptance checks. Official-image
initial install/update, Stop/retry and rollback on a disposable stopped target
should be checked while an unrelated environment stays open. Refresh installed
program lists if their cached view predates a root replacement. Upstream package
names/ABIs can change; mandatory checks abort before publishing an incomplete
replacement. Missing GnuPG is reported as a prerequisite, never bypassed.

## Release assets and update path

APK, SHA256SUMS, update.json and validation.json use the final hash/version above.
RU/EN updater highlights explicitly describe preserved app data and the separate
image-replacement behavior. Public assets, latest-release metadata and tagged
links are downloaded/checked after publication; that result is recorded in the
release handoff without changing the tagged APK.

AI development assistance: OpenAI Codex
