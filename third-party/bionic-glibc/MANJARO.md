# Manjaro ARM compatibility build

This is a separate build of the existing Bionic-for-glibc bridge for the pinned
Manjaro ARM base (glibc 2.35, GCC runtime 12.1). It does not replace the shared
runtime used by Ubuntu, Debian, Kali, Fedora, Arch or openSUSE/Void. A newer user
Manjaro remains eligible; no libc downgrade, environment rebase or system-library
replacement is performed.

The source revisions, ICD, libhybris hooks and WSI presentation patch are the
same as the existing bridge in `provenance.json`. Mesa remains unmodified upstream
26.2.5 with the same Zink/GLX/EGL options. The difference is the build toolchain
and dependency baseline, not a new rendering protocol. Driver ZIP import,
selection, locking and deletion keep the existing validated implementation.
No promise of universal Turnip/Qualcomm driver or device compatibility is made.

## Rebuilding

Keep all build inputs under the repository. Native ARM64 builds do not require
nested PRoot, package installation in a phone environment, or writes to a shared
SDK. `manjaro-build-packages.json` pins the official Manjaro GCC 12.1, binutils
2.38, glibc 2.35 and development-library packages by complete SHA-256 and size.
The compiler runs on the build host with an explicit private target sysroot.
No target libc is added to the build host's library search path.

1. Prepare pinned libhybris/Android headers/WSI, local autotools, CMake, Ninja,
   Meson and Mako as described in [README](README.md). Preserve the upstream
   revisions and apply `libhybris.patch` and `wsi.patch` exactly. Put the clean
   patched trees at `.tools/manjaro-bridge/libhybris` and `wsi-layer`.
2. Put the hash-verified unmodified Mesa 26.2.5 source at
   `.tools/mesa-26.2.5/mesa-26.2.5`. WSI uses the same Mesa 26.1.6 Vulkan/Android
   stub headers as the existing bridge; these build-only headers contain no
   GL/Vulkan runtime. Its source archive SHA-256 is
   `5296b88a0f1e012e2cb9ada150a2bbadf728ca81e5a4fb2ab43c83a4d2158606`.
3. Run `python3 scripts/prepare-manjaro-bridge-sysroot.py` to download and verify
   official build packages and extract them under `.tools/manjaro-bridge/sysroot`.
   This executes no package hooks. It also creates private `gcc`/`g++` wrappers
   that invoke that toolchain with `--sysroot` and `-B<sysroot>/usr/bin/`. Binutils' matching
   libbfd/libopcodes go in a build-only `host-libs` directory; the wrappers set
   LD_LIBRARY_PATH to that directory only, never to the target libc directory.
   Private `ar`/`strip` wrappers use the same build-only binutils libraries.
4. Run `bash third-party/mesa-container/build-manjaro-bridge.sh`. The optional
   `JOBS_BRIDGE_INPUT_ROOT` selects an existing read-only input tree. Outputs
   always remain in the current repository. Autoconf's disabled X11/Wayland
   defaults are deliberately selected by omitting their enable/disable flags:
   this upstream configure script checks those optional packages even with an
   explicit `--disable-*` argument. No change to runtime functionality results.
5. Run `python3 scripts/package-manjaro-bridge.py`. It strips the seven ELF
   libraries, preserves the two fixed runtime JSON manifests, checks the archive
   file set and updates only Manjaro's pin/size and separate provenance.

A successful compiler/linker run alone does not prove target compatibility.
Before distribution, check each ELF's needed symbol versions against the pinned
Manjaro libraries and load every runtime ELF using the private glibc 2.35 loader
with immediate symbol resolution. Keep X11/GPU device checks distinct from this
loader verification. The main bridge remains byte-identical to its published
component; no old tag/asset should be overwritten.

## Installation and isolation

The application downloads the separate archive only when starting/preparing
Manjaro with Bionic for glibc selected. It verifies the exact size/SHA-256 before
atomic activation. The private directory is `workspace-x11-bionic-glibc-manjaro`;
Manjaro has display 107 and its own X11 service. Its running desktop holds its
own shared operation lock. An interrupted/failed download cannot replace a ready
runtime or the user's selected driver. Future launches reuse the verified cache
offline. GPU import may be selected before installing the desktop.

Only Manjaro's minimum guest check becomes glibc >=2.35. Other guests retain
>=2.38 and their original component pins. CPU and VirGL paths do not change.
Turnip + Zink for Manjaro remains unavailable in this change: that separate
container-Mesa runtime has not been rebuilt against Manjaro's older baseline.

See `docs/fork/VALIDATION-test6-manjaro-bridge.md` for the recorded checks and
remaining phone tests. Source and license attribution remain in README.md,
provenance files and the accompanying source archive.
