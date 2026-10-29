# Private test6: Manjaro Bionic-for-glibc compatibility

Date: 2026-10-29. Private application candidate `2.0.10-test6`, code1437.
HezeAI is the fork developer; OpenAI Codex assisted with implementation.

## Scope and clean baseline

Manjaro ARM gains Bionic for glibc using a **separately rebuilt** libhybris,
Android q linker, Jobs ICD, WSI layer and upstream Mesa **26.2.5**. The source
revisions and compatibility/presentation patches remain the same. All seven
ELFs are built with GCC12.1/binutils2.38 against glibc2.35. No native X11 core,
Android JNI library, guest system libc or installed environment was replaced.

The existing bridge ZIP remains SHA-256
`e05b6abb19916e007330e19566b8833f10505133505d786bee127a71f1e4aa1d`.
Other guests retain glibc>=2.38 and their original URL/pin/cache. Manjaro uses
`workspace-x11-bionic-glibc-manjaro`, its dedicated service and display107.
CPU/VirGL behavior and existing application workarounds remain unchanged.
Manjaro Turnip+Zink is a different runtime and remains unavailable.

Manjaro's runtime ZIP is pinned separately and stays outside APK assets. Its
nine members are seven runtime ELFs and the two unchanged Vulkan JSON manifests.
Import/select/delete, running-desktop locks and the built-in optional driver
remain the existing implementation. The default-driver helper forwards the
selected target so it cannot install the ordinary >=2.38 bundle in Manjaro.

## Automated evidence

Detailed private evidence: `.verification/manjaro-gpu/`.

- GCC12 toolchain and development dependencies downloaded from official Manjaro
  mirrors with exact SHA-256/size pins; no package hooks or system installation.
- All seven **packaged** libraries loaded with `RTLD_NOW` using the private
  glibc2.35 loader and the compiler sysroot; host loader cache disabled.
- Repeated with libraries extracted from the exact published clean Manjaro base
  (SHA-256 `1ae540a80e21e1ca55e0576df9618fd2865cf4596b8204d20780f225aca5164a`)
  plus initial XFCE package bundle (SHA-256
  `07d396cbff3c1f9b9d467a5c14d8d589af597b7d2520ae039ff61f8d9f4f3660`).
  Every mapped shared library was checked to belong to the private runtime or
  that fixture, preventing accidental reliance on installed phone/build-host
  libraries. This is clean-library/runtime evidence, not a complete GUI install.
- Required symbol versions fit glibc<=2.35, GLIBCXX<=3.4.30 and CXXABI<=1.3.13.
  No private glibc ABI. The runtime has no build-only libhybris filename dependency.
- **103 Python tests passed**: real ZIP extraction/first install, repeat/offline
  reuse, wrong-bundle rejection, corrupted-runtime repair, imported selection
  preservation, independent guest locks/caches, cancel/checksum failures,
  driver deletion safeguards, actual helper directory-alias entry points,
  bridge-only dependencies and unchanged application launch compatibility.

- **64 focused JVM/Robolectric tests passed**, including Manjaro renderer UI,
  dedicated-service validation, rejection of live renderer changes and existing
  wrapper/GPU/service regressions. These are mocked UI tests, not phone tests.
- Independent lint7.4.2 passed for all five modules: **0 errors**, 223 existing
  warnings, no infrastructure diagnostics. The old AGP lint is not the evidence.
- ARM64 release build succeeded. ZIP, package `com.termux`, non-debuggable flag,
  code1437, original signing certificate, 86 source assets, required runtime
  definitions and no secret-pattern findings were checked. All three JNI
  libraries remain byte-identical to rollback1432.
- All 12,803 present Mesa source files matched the official pinned archive.
  Five non-build CI files were absent in the source cache; none of the compiled
  source content was changed. The report records these exclusions explicitly.

The final URL-only APK rebuild succeeded and was reverified: **39,833,687 bytes**,
SHA-256 `1194d5d3d3e91930e3f2667d92002376dbccd200f607787cd4161a226833c88f`. Signer SHA-256
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`. The Manjaro URL targets additional files in the existing
`xfce-base-20261028` component release, as requested by the developer; old assets
are preserved. No app GitHub release or in-app update manifest is published.
All five new component assets were downloaded back over public HTTPS and
matched exactly. The real shipped helper installed the bridge from the public
URL into an empty private cache, then repeated successfully with network access
blocked. Imported selection and a files-directory alias were preserved. All
30 pre-existing release assets retained their IDs, sizes, update times and URLs;
release body/flags and old component pins were unchanged. No new tag was made.
Evidence: `public-component-verification.json`.

## Review and lifecycle inspection

The UI, dedicated service and desktop process all admit the new target. Existing
service ownership/cross-target checks remain in place. Starting a bridge checks
Manjaro's own runtime marker, passes only its bind/driver selection into its
process tree and holds its lifetime lock. Guest minimum glibc2.35 applies only
to Manjaro. Required native libraries are checked/installed through Manjaro's
own package path. No global shell profiles/environment, X11 services, Telegram
bot or SSH tunnels were changed by these build and fixture checks.

No new process shutdown mechanism is introduced. Failure/cancel occurs before
activation; a partial/wrong archive cannot replace a ready runtime or selected
driver. An unsupported runtime cannot fall back silently to another guest's
bundle. No automatic user-data reset or rebase/migration is introduced. Existing
workspace schema8, package ID and signing identity are retained; installed-app
update/Android graphics behavior still requires device verification.

## Required phone check

Stop only the Manjaro desktop using its normal app control, update to test6,
select **Bionic for glibc**, choose the default/imported known-working Turnip,
and launch. First launch downloads/verifies its runtime. Check GPU INFO, GPU
Test, VSync on/off, a browser and repeated stop/start. Check another desktop can
stay running independently. These checks have **not** been performed by the
build/loader fixtures. Fresh-phone Android permissions/GUI and other GPU/driver
combinations remain unverified; import acceptance is not runtime compatibility.

The previous nested PRoot graphical test queue remains paused after its ptrace
assertion. This candidate does not restart it. The temporary clean-library
fixture is removed after verification; reports remain. Installed guest files
and the developer's active sessions are not modified.
