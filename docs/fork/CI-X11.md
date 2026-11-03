# Pinned X11 inputs in GitHub CI

The workflow prepares inputs before invoking the unchanged mandatory
`:x11:verifyX11Inputs` guard. No developer X11 cache is restored. Initial cache
absence, source commit and CI-driver commit are recorded in the run artifacts.
`workflow_dispatch.source_ref` permits checking an immutable older source commit
with the current CI driver; this does not change that source commit or its old runs.

## Ready-made inputs and compilation

`scripts/prepare-x11.py` uses its existing size/SHA-256-verified downloader.
`scripts/x11-build-inputs.json` records additional exact download identities.

- Termux:X11 APK: existing immutable Components mirror; size/SHA must also match
  the tested source's `third-party/termux-x11/provenance.json`. Extracted ARM64
  `libXlorie.so` and its Java commit marker are checked.
- Keyboard ZIP: extracted from the fixed published Jobs v2.1.2 APK, verified at
  both APK and ZIP level. The original pinned Termux xkeyboard-config DEB URL
  returns HTTP 404. No newer keyboard data replaces it. COPYRIGHT is retained.
- XFCE style ZIP: existing immutable Components asset, checked against the tested
  source's runtime pin. It is not embedded in the APK. Host-dependent Cairo
  regeneration is a separate `prepare-x11-style.py` operation.
- OpenH264 and GStreamer plugin: extracted from the original pinned ARM64 Debian
  DEBs; copyright files are retained. No host or guest package is installed.
- Musl and glibc/Linux target headers: pinned archives extracted locally solely
  for compilation. Their versions/size/SHA are in the script/build manifest.
- `x11-alpine-compat.c` and `x11-fedora-glycin.c`: compiled from the **tested
  source checkout**, using explicit ARM64 musl/glibc targets and LLD. CI uses the
  pinned NDK 22.1.7171670 Clang; local ARM64 Clang is a separate check. Both ELF
  machine IDs must be AArch64. Source/output hashes, compiler and flags are saved
  in `.tools/x11-runtime/verified.json`. These locally compiled shims are not
  claimed byte-identical to the already published APK's shims.
- Existing tracked VirGL/bridge archives retain their source runtime pins and
  exclusion from APK payloads. No published Components asset changes.

CI additionally selects build-tools 35.0.0 AAPT2 through repository-local Gradle
properties: the AGP4.2.2 default AAPT2 cannot parse platform35 android.jar.
NDK22.1.7171670 and build-tools30.0.3 remain installed as required by the source.

JVM tests use the same four check.sh suites plus X11. A short runner-local
symlink points into source/.tools/tmp for Unix socket fixtures; long workspace
paths can exceed sockaddr_un limits. Actual temporary files stay in the checkout.
Test XML and hidden preparation identity reports are explicitly included in the
artifact; no .git directory, credentials or signing material is uploaded.

The normal Gradle build still uses the tested source's build files and dependency
versions. CI's debug APK is a build check, not a replacement of the signed stable
APK. Modern release Lint and final signed APK verification remain separate gates.
No application source change or new stable release is implied by this CI fix.

## Run identities

The original v2.1.2 source is
`c0073b462311bba5fd2dcc301b048d69e407f3f1`.
Its [original failed run](https://github.com/HezeAI/termux-jobs/actions/runs/41578152018)
failed because pinned X11 runtime files were not prepared. That result remains
failed. The corrected workflow's results must be listed separately with both
source and workflow commits. A rerun is not a rewrite of historical CI status.
