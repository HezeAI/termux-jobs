# Experimental Bionic for glibc bridge

Optional integration for **ordinary Ubuntu, Debian, Fedora, Arch Linux, openSUSE and Void**, retaining the established bridge core with a Mesa 26.2.5 test frontend. This is a Vulkan HAL/ABI and X11 presentation bridge, not a compiler that converts arbitrary Android libraries to glibc. It uses libhybris (including the TAWC/ARLinux fork's Android TLS work), an application ICD and the xMeM/Arm Vulkan WSI layer. It contains neither Vortek nor Venus. No vendor driver or Universal Adreno package is bundled.

Pins and per-file hashes are in `provenance.json`. Local changes are provided in `libhybris.patch`, `wsi.patch` and `../../x11/src/bridge/bionic-icd.c`. The imported driver remains executable native code under the application's UID. Format/hash validation is an integrity check, not a sandbox or compatibility guarantee.

The bundle is 64-bit glibc code downloaded from the pinned Termux Job’s component release when the selected desktop first needs it, not loaded by the Android application process. It requires glibc >= 2.38, GLIBCXX_3.4.32 and the selected guest's Vulkan loader, GLVND and X11/xcb dependencies. A private upstream Mesa 26.2.5 Zink/GLX/EGL build is used only for this renderer. It avoids the Mesa 26.0.8 swap-interval regression without replacing installed guest libraries. Only the selected Ubuntu, Debian, Fedora, Arch Linux, openSUSE or Void desktop receives bridge environment variables and Android vendor-library binds. Termux's existing Wrapper and other renderers retain their own caches and startup paths.

## Build inputs and recipe

The checked-in ZIP was built locally on ARM64 Ubuntu with GCC/G++ 15.2 (glibc toolchain, not the Android NDK), local autotools (autoconf 2.72, automake 1.18.1, libtool 2.5.4), CMake 4.4.4 and Ninja. Vulkan/Android stub headers came from Mesa 26.1.6; X11/xcb development headers and link libraries came from an isolated Debian ARM64 sysroot. These are build inputs only; those SDK libraries and the host's Mesa are not bundled. Binary-reproducible output across toolchain versions is not claimed.

All working files below belong in `.tools/bionic-bridge`. Fetch the three pinned source revisions from `provenance.json`, then apply the two matching patches. Build `libhybris/hybris` with:

```sh
NOCONFIGURE=1 ./autogen.sh
./configure --prefix="$FORK_BRIDGE_ROOT/install" \
  --libdir="$FORK_BRIDGE_ROOT/install/lib" \
  --with-android-headers="$FORK_BRIDGE_ROOT/android-headers" \
  --enable-arch=arm64 --enable-adreno-quirks --enable-property-cache \
  --with-default-hybris-ld-library-path=/system/lib64:/vendor/lib64:/apex/com.android.runtime/lib64/bionic
make -C common -j4 libhybris-common.la
make -C common/q -j4
```

Copy `common/.libs/libhybris-common.so.1.0.0` as `runtime/libhybris-common.so.1`, and `common/q/.libs/q.so` as `runtime/linker/q.so`. Compile the ICD as a glibc shared library with `-fPIC -shared -O2`, the libhybris/Vulkan/Android-stub include directories, and:

```sh
-lhybris-common -ldl -pthread -Wl,-soname,libjobs-bionic.so \
  '-Wl,-rpath,$ORIGIN' -Wl,-z,nodelete
```

Create a build-only `runtime/libandroid.so` symlink to `libjobs-bionic.so`. Configure the patched WSI layer with CMake/Ninja, `BUILD_WSI_X11=ON`, `BUILD_WSI_WAYLAND=OFF`, `BUILD_WSI_DISPLAY=OFF`, and the Vulkan/Android-stub and X11/xcb include paths. Link against the isolated sysroot and our `runtime` directory (`-L` and `-Wl,-rpath-link` as appropriate). Its Android AHardwareBuffer calls resolve to our ICD exports, which dispatch through libhybris. The build-only symlink is not packaged.

Build unmodified **Mesa 26.2.5** from the archive pinned in `provenance.json`. Place the verified archive in `.tools/mesa-26.2.5/mesa-26.2.5.tar.xz`; keep extracted sources and build/install trees under this repository. Use a glibc ARM64 compiler and a private X11/xcb/libdrm/GLVND development sysroot. Compile `x11/src/bridge/mesa-libm-compat.c` to a PIC object; include that object plus `-Wl,--wrap=atan2f,--wrap=asinf,--wrap=acosf` and `-lm` in both C/C++ link arguments. This binds atan2f, asinf and acosf to ARM64’s existing GLIBC_2.17 exports rather than the new default exports of a glibc 2.43 build host; it does not modify libm. The runtime is checked to require no GLIBC version newer than 2.38. Configure Meson with these options (supply the local compiler/sysroot through a native file):

```sh
meson setup mesa-build mesa-src/mesa-26.2.5 --native-file glibc-native.ini \
  --prefix=/opt/termux-jobs-bionic-mesa --libdir=lib --buildtype=release \
  --wrap-mode=nodownload -Db_ndebug=true -Dgallium-drivers=zink \
  -Dvulkan-drivers=[] -Dplatforms=x11 -Dllvm=disabled -Dglvnd=enabled \
  -Dgbm=disabled -Degl=enabled -Dglx=dri -Dgles1=enabled -Dgles2=enabled \
  -Dgallium-va=disabled -Dgallium-rusticl=false -Dvideo-codecs=[] \
  -Dxmlconfig=disabled -Dshader-cache=enabled -Dzstd=disabled -Dbuild-tests=false
ninja -C mesa-build -j4
meson install -C mesa-build --no-rebuild --destdir "$FORK_BRIDGE_ROOT/mesa-stage"
```

Only `libGLX_mesa.so.0`, `libEGL_mesa.so.0` and `libgallium-26.2.5.so` from that install are packaged in `runtime/mesa`. Mesa 26.2 directly links the matching Gallium frontend; it does not need a second copy of Gallium under a DRI alias. No LLVM, vendor Vulkan driver or abandoned Universal-driver changes are included. For Qualcomm proprietary drivers, the WSI patch allocates BGRA UNORM Android hardware buffers through our ICD exports and imports them with queried Vulkan memory properties. Turnip keeps the export path. The native-import surface does not advertise sRGB formats whose exact buffer-format requirement is not met. The ICD exports allocate/describe in addition to release/socket-transfer and retains NODELETE. No vendor driver is bundled. See candidate 1365 evidence and limitations in `../../docs/fork/BIONIC_GLIBC.md`.

The WSI patch adds the immediate/async path while leaving FIFO synchronized. GLX/EGL applications retain control of their swap interval; no global `vblank_mode=0` override is applied.

The X11 backend waits for each current GPU present payload before sending its AHardwareBuffer pixmap: this protocol carries no imported GPU wait fence. Waiting for GPU completion must not be confused with waiting for vblank. All three modes present from the caller. FIFO retains synchronous X11 Present options and target-MSC pacing; the additional FIFO worker is omitted. The separate completion/idle reader must remain running while base initialization is still VK_NOT_READY; it terminates only on explicit stop/connection loss. Configure events use an extent snapshot initialized before the reader starts. A GPU-wait timeout marks device loss without returning the busy image to the free pool. The delayed-render regression and device-evidence boundaries are documented in `../../docs/fork/X11_PRESENTATION_AUDIT.md`.

For this Mesa-only candidate, `python3 scripts/package-bionic-glibc.py` verifies the immutable v2.0.6 baseline archive, preserves its six non-Mesa files exactly, strips and replaces only the three Mesa frontend libraries, then updates the helper's pinned archive hash and provenance. The exact current frontend configuration is also recorded in `../mesa-container/build-bridge.sh`. Native protocol checks passed; Android GUI acceptance remains pending. It does not install anything on the phone. Review the new hashes before committing. `python3 scripts/prepare-x11.py` verifies the pin and keeps the ZIP out of APK assets. The downloader enforces SHA-256 and exact size before atomically caching it; valid existing runtime installations work offline.

The application can replace only its own bridge runtime and private driver cache. There is no system libc patch, root requirement, shared shell configuration change, global `LD_PRELOAD`, or replacement of installed guest Mesa files. The selected desktop must be stopped before driver import/switch. Ubuntu, Debian, Fedora, Arch Linux, openSUSE and Void have separate driver/runtime directories and locks; their X11 services already have separate processes and displays. A shared cache lock remains held during that desktop's lifetime.

## Licenses

libhybris is predominantly LGPL-2.1 with additional BSD, MIT, ISC and Apache-2.0 components, including its Android linker sources. The Vulkan WSI layer is MIT licensed. Mesa has component-specific licenses, predominantly MIT with SGI-B for GLX; its upstream overview and license-text collection are retained in `MESA-LICENSE.txt`. Included notices retain upstream copyright/license texts. Corresponding upstream revisions, local patches, ICD source and build instructions are supplied with this tree. Before distributing a modified runtime publicly, preserve those source references and notices alongside the matching artifact.

See [validation and limitations](../../docs/fork/BIONIC_GLIBC.md).
