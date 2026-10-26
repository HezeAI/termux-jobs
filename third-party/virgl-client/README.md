# Private Alpine VirGL client

This is a minimal upstream Mesa 26.1.6 build for aarch64 Linux/musl, using only
Gallium `virgl`, EGL/GLX and GLES. No LLVM, Vulkan ICD, Turnip or Zink is included.
The source archive URL and hashes, compiler flags and final library hashes are
recorded in build-info.json. The source is unchanged. License notices are in
LICENSE.txt and retained as an APK asset. Since private test2 the client ZIP is
a pinned optional component in termux-jobs-components/x11-extras-20261025, not
an APK asset. Existing valid copies and the verified shared cache work offline.

At runtime the pinned archive is extracted atomically into an application-owned
cache. Only the selected Alpine VirGL desktop sees it through its own PRoot bind
and command-local library paths. The installed Alpine Mesa and other renderers
are not replaced. The archive contains regular ARM64 ET_DYN libraries only;
symlinks/foreign paths, corrupt data and edited cache libraries are rejected.

Build inputs use a repository-local Alpine 3.24 sysroot (musl-dev, libstdc++-dev,
GCC startup objects, X11/XCB/DRM headers) with Clang 21.1.8 and Meson/Ninja. Existing
runtime libraries are read only. No `apk add`, package install or system SDK write
is performed on the development device. `-mno-outline-atomics` avoids linking
Android-specific CPU-discovery code from the Termux compiler builtins. The
installed result is checked for external build RPATHs, libLLVM and Android-only
`__system_property_get` references before packing.

Rebuild options:

```
meson setup build mesa-26.1.6 --cross-file <private-musl-cross.ini> \
  --prefix=/opt/termux-jobs-virgl --libdir=lib --buildtype=release \
  --wrap-mode=nodownload -Db_ndebug=true -Dgallium-drivers=virgl \
  -Dvulkan-drivers=[] -Dplatforms=x11 -Dllvm=disabled -Dglvnd=disabled \
  -Dgbm=disabled -Degl=enabled -Dglx=dri -Dgles1=enabled -Dgles2=enabled \
  -Dgallium-va=disabled -Dgallium-rusticl=false -Dvideo-codecs=[] \
  -Dxmlconfig=disabled -Dshader-cache=disabled -Dzstd=disabled -Dbuild-tests=false
ninja -C build -j6
meson install -C build --no-rebuild --destdir <repository-local-stage>
```

The `swrast_dri.so` name is a copy of the built dril DRI loader entrypoint,
which exports the swrast transport entrypoint even with no CPU Gallium driver.
`GALLIUM_DRIVER=virpipe` selects the remote renderer; there is no silent software
fallback in this private build. Packaging and real-device isolated rendering
checks are documented in docs/fork/VALIDATION-v1.7.100-x11-test.md.
