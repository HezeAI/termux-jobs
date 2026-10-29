# Private test11: official image installation and selected-menu checks

Private candidate `2.0.10-test11`, versionCode1442. HezeAI is the fork developer;
OpenAI Codex assisted with implementation. No GitHub publication is authorized
by this report. See [behavior](ENVIRONMENT-UPDATES.md) and
[comparison with public v2.0.9](COMPARISON-v2.0.9-working.md).

## Changes and compatibility

Package checks stay in the selected environment dialog. Each target also has
official image discovery and confirmed installation/replacement, with progress,
Stop, a complete old-system checkpoint and optional /root + /home preservation.
The existing Components path remains separate. No settings/schema reset is
required to update the application. A deliberately selected official replacement
changes that environment's OS/application set as explained before confirmation.

Native per-alias locks, a fresh temporary alias, mandatory component checks,
identity checks, atomic publication and journal recovery protect the existing
system. No unrelated sessions, host services or user-selected directories are
stopped, replaced or cleared by the implementation's mutation path.

## Verification evidence

Final automated results: **128 release JVM checks**, **117 Python checks**
(followed by a 41-case final core rerun), **4 Node suites**, and modern
lint7.4.2 across all five modules with **0 errors / 225 existing warnings** and
no analysis-infrastructure failures. All nine upstream metadata sources responded
successfully during the final live check. The final source matches the isolated
APK and lint stages. Project-owned native/OCI test roots were removed after
retaining their hashes and validation logs.

Final APK verification passed ZIP integrity, signature, package/version/ABI,
required DEX runtime definitions, source assets and secret-pattern checks.
All three JNI libraries remain byte-identical to the retained rollback baseline.

| Field | Value |
| --- | --- |
| File | build/apk/Termux-Jobs-test11.apk |
| Package | com.termux |
| Version | 2.0.10-test11 |
| versionCode | 1442 |
| ABI | arm64-v8a |
| Bytes | 39,903,775 |
| SHA-256 | a117fa36b87e4ccd88f0d2032f5f58506901e18ac0683b99a100e5a7f6ab9b29 |
| Signing certificate SHA-256 | dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644 |

`.verification/official-install/` contains the detailed logs:

- Python filesystem/workflow checks: initial install, replacement/home carry,
  full snapshots and rollback, expired/mismatched plans, cancellation and failure,
  mandatory desktop rejection, recovery after initial publication, private path
  aliases, no-follow cleanup, source change refusal and managed PRoot links.
- Actual owned-process tests preserve the group's unreaped leader until its final
  signal and leave an unrelated guard process alive. No broad process scan/kill
  or live PRoot GUI test is used.
- Real offline APT/dpkg transactions use script-free private fixture packages,
  initially empty database/caches and fully redirected roots/config/logs/hooks.
  The new official-desktop install requests and installs the complete declared
  APT set and succeeds on a repeat run. Host dpkg status hashes remain unchanged.
  Executable placeholders test installer/status decisions, not XFCE drawing.
- Actual upstream Alpine ARM64 manifest/config/layer download, digest verification
  and local OCI packaging succeeded. The installed native archive-install handler
  extracted that OCI into a project-only runtime/cache and produced BusyBox/apk.
  Subprocess execution was forbidden in this extraction fixture: no guest/PRoot
  process was started. This does not assert that native CLI startup under a nested
  PRoot is supported.
- The bundled Arch Linux ARM key verified a real official signed keyring package
  (5160 bytes); changing its payload was rejected. The full 829MB Arch rootfs
  archive was not downloaded for this proof.
- JVM tests cover keeping the selected dialog open during checks, scoped official
  requests, live results, absent/existing-target confirmation, Stop and lifecycle,
  plus related X11/profile/chat/alias regressions.
- Node fixtures check live agent action transitions, bounded previews, attachments
  and generated-file cards without contacting real agent services.

Only the public Arch Linux ARM signing key is bundled. Source blob:
https://api.github.com/repos/archlinuxarm/archlinuxarm-keyring/git/blobs/6b352c02e42b64d1867e265558e582f921ca05e8
Public key fingerprint: `68B3537F39A313B3E574D06777193F152BDBE6A6`.
Key asset SHA-256: `4e4202772328674af68f90f675e077d192337fc8ced15cddcd35875a9de5b5f9`.
Official signing documentation: https://archlinuxarm.org/about/downloads
The native installer remains from https://github.com/termux/proot-distro;
new metadata/download/transaction integration uses the application's own helpers.

## Device acceptance still required

Test the final APK on a stopped disposable ordinary environment: check packages
without losing its menu, check its official image, install/update, reopen the
terminal and desktop, then restore the checkpoint. Keep an unrelated environment
open to verify isolation. Repeat Stop/retry and check programs/catalog refresh.
Ubuntu AI must remain outside this operation. Also recheck Fedora/openSUSE package
previews on the phone. The automated fixtures do not prove online package/GUI/
renderer compatibility for every new upstream distribution release.
