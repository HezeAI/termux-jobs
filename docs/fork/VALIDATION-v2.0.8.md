# v2.0.8 validation

This report distinguishes relevant automated checks, source/state review and
developer device feedback. Earlier full-audit totals belong to their named builds.

## Final artifact and compatibility

- File: `Termux-Jobs-v2.0.8-arm64.apk`; 39,773,192 bytes.
- Package/version: `com.termux`, `2.0.8`, versionCode **1413**.
- SHA-256: `5dc7d350bf237a5c1eccd4487abd2d6814dc4cfde07c124cc1d64cee3773b952`.
- Signing certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ARM64 only; Android8+ app, Android11+ X11; min26/target28/compile35 and schema8.
- The higher code permits updating from stable1410 and previews1411/1412 with the
  existing certificate. No data reset, account migration or rootfs reinstallation
  is introduced. Histories, accounts, presets, projects and installed environments
  keep their existing formats. A normal agent-session restart with the saved chat
  may be needed for new attachment helpers; the app does not stop current work to
  force that refresh. Finish important work before installing an update.

ZIP integrity, non-debuggable release flag, package/version, ABI and certificate
checks pass. All78 source assets match; all asset and native-library entries are
byte-identical to test2. The three JNI libraries and X11 runtime also match stable
v2.0.7. Forbidden private filenames/build directories and asset/DEX secret-pattern
scans report no findings. Required runtime definitions pass:13,753 defined classes,
31 reviewed optional/framework references and no unreviewed missing types. This
inventory is not proof of every method/API/device path.

## Relevant automated checks

- **109 JVM cases across16 classes**, zero failures/errors/skips, on the final
  release source/version. Coverage includes Markdown/code copy/task literals,
  tables, preserved streamed-block identity and input focus, file cards, sort and
  selection retention, gallery URI limits and bounded staging, multi-select
  confirmation, scoped Stop, overlay lifecycle, cache/browser ownership and
  existing keyboard policy.
- **Eight Node suites** pass on the final source: generic output cards through all
  four providers, Codex/shared attachments, OpenCode socket attachments and each
  agent transcript/controller. Synthetic transports and files exercise size,
  scope, symlink, duplicate, missing-file and recovery paths; no account/model API
  request or running-session interruption is used.
- **45 scoped Python X11 regression checks** pass without skips: actual optional-helper
  entry points accept Android private-directory aliases and reject unrelated roots;
  used-driver deletion preserves external symlink targets, original ZIPs and protected
  defaults. A real private XFconf/D-Bus fixture verifies live scale and restoration
  of unset DPI/cursor leaves. Fixture HOME/cache/runtime paths are inside the repository;
  no installed desktop settings or active session is modified. A short fixture path
  avoids the Unix socket pathname limit of the deeper source checkout.
- **Modern lint7.4.2:0 errors/217 warnings**, all five modules analyzed, no rule or
  infrastructure-loading diagnostics. Categories include existing SDK/dependency,
  text/localization, resource, private-API/native-layout and accessibility notices.
  The two media-picker InlinedApi warnings are guarded by the Android33 check with
  document-picker fallback. No broad suppression or new baseline was added.
- The compileSDK35/AGP tested-SDK warning remains. The legacy APK-chain lint also
  emits its known old-registry/Kotlin compatibility diagnostics; it is not used as
  the source-analysis result. The independent compatible lint gate above passes.
- Isolated signed ARM64 assembleRelease, APK runtime-definition verification,
  source/content review and product whitespace checks pass. The verbatim upstream
  Markdown notice retains its final blank line; vendored patch whitespace is not
  rewritten. Build tools, caches and outputs remain repository-local.

The cadence test checks decisions for synthetic60/90/120/144Hz timestamps and
stall/reset recovery. It does not measure phone FPS. Robolectric draw-state tests
verify registered pressed/idle button assets and invoke the custom drawing
callback for lifecycle coverage; they do not establish physical paint, gestures,
IME or compositor timing. Gallery staging uses a synthetic ContentResolver stream;
actual gallery providers/codecs remain Android/device-dependent.

## Source and state review

Markdown is native presentation; original provider messages and stored raw history
are not rewritten. Stable earlier blocks are reused during streaming. Code copies
its exact literal without line numbers or task-marker substitutions. Tapped links
allow HTTP(S); Markdown has no WebView, automatic remote-image fetch or execution.

Generic output cards inspect metadata for explicitly linked local regular files.
They do not execute or unpack files or start APK hashing. Explicit open/save uses
bounded per-environment transport. File formats do not guarantee model decoding.
Gallery copies only selected content URIs into the original chat run's private
staging area, with size/pixel limits and cleanup after failures. Late results do
not attach to a different run; multi-selection confirms into a draft and includes
existing attachments in its four-file limit. Gallery requests no broad-storage
permission. Sorting changes display order rather than file paths or content.

Stop reuses the existing native-session interrupt with generation/profile guards
and direct-command confirmation. It does not add a global kill or server shutdown.
The overlay retains a one-pixel layout height; VSync drives an average60FPS target,
with cached geometry and no self-invalidating draw loop. Idle/hidden/detached
states remove callbacks and overlays. Server/robot border indicators remain on
their existing16ms scheduling; they are not a measured fixed60FPS reference.

Acknowledged message sends clear/hide the composer only for the same terminal and
unchanged submitted draft. Failed sends and a newly edited draft retain input.
Upper Shell behaviour is unchanged. Unrelated X11/native/driver/session-launch
sources are unchanged from test2; no installed environment or tunnel was modified.

## Device evidence and remaining limits

HezeAI confirmed preview installation, inspected Markdown and requested the final
Copy/animation refinements, then authorized publication. This is developer
acceptance evidence; it does not establish every gallery provider, model/CLI,
keyboard/Android combination, clean device or a measured60FPS frame rate.
All-provider authorized-account workflows, interruption during a real request,
IME/gallery interaction and animation appearance still depend on device testing.
Existing RedMagic10Pro/SamsungS26Ultra desktop reports remain historical evidence;
this release does not claim a new full X11/hardware matrix run.

The release contains the APK, `update.json`, `SHA256SUMS` and `validation.json`.
The schema1 updater metadata includes RU/EN changes and compatibility before
installation, preserving the format consumed by installed v1.0.19 clients. Public
verification downloads all four assets and checks their hashes, release status,
latest-stable metadata, source tag and documentation links.

## Publication verification

[v2.0.8](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.8) is the latest stable release. The immutable source tag
points to `a22aa3bfcb64e9f2e5f19e5181c5186c7d357526`. All four assets were downloaded anonymously over
verified HTTPS and matched local lengths, SHA-256 and GitHub digests. The APK
verification copy resumed via HTTP Range after an interrupted stream; its final
complete hash matched. Downloaded
checksums/updater/validation records agree. The legacy-compatible latest-stable
updater response, RU/EN compatibility, release body/credit, tagged documentation
and component information link were checked. This documentation record does not
alter the published tag or APK.
