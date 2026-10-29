# Private test7: documentation and Manjaro Turnip + Zink

2026-10-29. Candidate `2.0.10-test7`, code1438. HezeAI develops the fork with
OpenAI Codex assistance. This document is in progress until artifact evidence is
recorded below; it is not a claim of completed phone verification.

## Scope

- ArchWiki book card beside the brand and environment Documentation opposite
  Programs/Back. Global in-memory ArchWiki article/history/scroll, RU/EN official
  sections with honest English fallback. See [reader design](DOCUMENTATION-READER.md).
- Kali-only recommendations: Nmap, Wireshark, Burp Suite and Apktool. Their exact
  package groups and binaries are checked independently from XFCE. Nmap/Apktool
  have owned terminal shortcuts; Wireshark/Burp use package-provided launchers.
  Nmap's shortcut uses unprivileged mode; Android does not acquire raw socket or
  packet-capture privileges. Apktool is not an APK-signing implementation.
- Manjaro-only Turnip + Zink Mesa26.2.5 payload built with the existing lfdevs
  Android/KGSL patch against Manjaro glibc2.35/GCC12. No system Mesa replacement.
  Selection/cache/display/service remain independent of other environments.
  Manjaro's separate Bionic-for-glibc bundle from test6 is unchanged.

## Boundaries and compatibility

Package/signing identity and workspace schema8 are retained; no data reset or
model/VM/Wayland experiment is introduced. Existing GPU component pins are not
replaced. The new Manjaro asset is added to the existing component release with
its own unique name, hash and sources. No installed guest or running user session
is modified by build/fixture checks. Optional tools remain optional, outside the
base desktop installation.

## Evidence and limits

Private logs: `.verification/wiki-reader-1438/` and
`.verification/manjaro-turnip-1438/`. Final results follow after checks finish.

The Kali package names/dependencies were compared with the official ARM64
repository metadata and Kali Tools documentation. This is metadata evidence,
not successful clean APT transactions or GUI launches of the four new tools.
The previous nested PRoot graphical queue caused a ptrace assertion and remains
stopped; it is not used as a new-install acceptance check.

Required developer device checks: header fit in portrait/landscape; real WebView
article navigation, RU/EN, scroll/back/reopen from different sessions, network
failure and keyboard; each optional Kali tool installation/launch; fresh Manjaro
Turnip component download, GPU INFO/test and switching to CPU/VirGL/Bionic only
with that desktop stopped. Source/loader tests do not establish phone rendering
or compatibility/performance on every Adreno.

## Completed local checks

- 142 Python tests passed: GPU/helper install, isolation, cache reuse, cancel,
  corruption/checksum handling, bridge/driver safeguards, Kali groups/shortcuts,
  native renderer/session policy and selected-guest program routing. Package
  managers in the new optional-program tests are simulations, explicitly not
  fresh APT/GUI evidence.
- 101 focused release JVM/Robolectric tests passed, including global reader
  snapshots, normal same-WebView reopen, activity disposal vs finishing, stale
  callbacks, RU/EN fallback, footer/header integration, TLS/file restrictions,
  Kali-only IDs/status/cache/resume and renderer/service isolation.
- Independent lint7.4.2: zero errors, 225 warnings, no infrastructure diagnostics.
  Changed Java files match the analysed final source.
- All seven packaged native ELF libraries passed RTLD_NOW with the pinned clean
  Manjaro base+desktop glibc2.35 libraries and no host-library fallback. This
  does not open a GPU device or prove an Android window/rendered frame.
- ARM64 APK assembled; ZIP/package/version/signature, 86 source assets, runtime
  definitions and absent secret patterns were verified. All three JNI libraries
  are byte-identical to rollback1432; this change does not alter X11 native core.

APK: `Termux-Jobs-test7.apk`, 39844447bytes. SHA-256
`83f061fcae4e97a329ef24ec49b4240cb7a7dedc321d47c0cdf8e1b75b89c703`. Code1438; original signing certificate retained.

## Component publication

Added five uniquely named Manjaro Turnip runtime/source/manifest/validation/hash
assets to existing `xfce-base-20261028`, Components main commit8f8dd1a. All35
previous asset IDs, sizes, URLs and modification timestamps were preserved;
release body/flags were unchanged. Every new public HTTPS asset was downloaded
and compared byte-for-byte. The unmodified shipped GPU helper installed from
the public URL into an empty cache, reused it with blocked networking, accepted
a filesystem-identity alias and rejected a different same-basename directory.
Its selected-guest requirements backend was stubbed to avoid nested PRoot;
actual native ABI resolution was separately checked using the clean loader.
No application APK was published.

Runtime download:9,916,572bytes, SHA-256
`55ba75820036a193d2fbd393ff95c39c52043e54d21846b49f1f9f3644c6ebba`.
The separate test6 Manjaro Bionic bridge remains byte-identical.

Fresh APT transactions and GUI startup of the four new Kali recommendations
remain unverified. A namespace command returned success, but its isolation
identity could not be inspected in the current PRoot proc view; no chroot, mount
or package installation was attempted on that basis. Developer phone tests are
still required, along with Manjaro GPU drawing and real WebView behavior.
