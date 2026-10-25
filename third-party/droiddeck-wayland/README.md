# DroidDeck-derived private Wayland runtime

Source: https://github.com/Droid-Deck/DroidDeck at
`22eac0831d10671f6bc2ad914a93a0da5046c7df` (0.6.0), GPL-3.0.
`upstream/` is pristine and inventoried by byte size, Git blob and SHA-256 in
`upstream.json`. `guest/` contains the original DRM/udev helpers with source pins.

Jobs changes are applied to an isolated copy by `scripts/prepare-wayland.py`.
The Android host, lifecycle, private startup, driver validation/locking and guest
routing are in `x11/src/main/java/` and `x11/src/main/assets/wayland-*`.
`x11/src/main/cpp/wayland-guest.c` combines the guest compatibility entry points.
The JNI class retains its original package and names; consumer rules preserve it
through R8. No downloaded rootfs or vendor driver is bundled.

The preparation script verifies pinned upstream files and Vulkan build headers,
builds the Android ARM64/API30 core with the local Termux Clang toolchain, and
builds the ARM64 glibc guest preload with the local compiler. It packages the
pinned upstream Wayland server and the AdrenoTools/hook runtime from:
https://github.com/sabamdarif/termux-desktop/releases/download/bionic-vulkan-wrapper-0.0.5r5/vulkan-wrapper-android-leegaos-fork-aarch64.zip
SHA-256: `c50f3a9f884ca2c54ca76d4eb55709a4012cff77ff279216a0f04920c05bcc55`.
Pinned official Termux libc++/libffi packages and their copyrights are listed in
`runtime-packages.json`. Existing X11 keyboard data supplies the keyboard license.
Khronos build headers come from the unchanged Mesa 26.1.6 archive recorded in
`build-headers.json`, with per-file SHA-256 and Apache-2.0 notices preserved.

The build writes only repository `.tools/` outputs. Reviewed runtime output pins
are in `x11/wayland-inputs.json`; Gradle rejects absent/changed binaries. A native
source/toolchain change requires rebuilding, inspecting the result and deliberately
updating the pins. Cross-toolchain byte-identical reproducibility is not claimed.
Android system APIs and the installed toolchain are read-only build inputs. API30
is the compile target; actual devices/older Android versions still require testing.

See [Arch Wayland](../../docs/fork/ARCH-WAYLAND.md) for architecture, isolation,
probe evidence, rollback limits and remaining KDE/device checks. The normal X11
server, agent runtimes and other distributions are not replaced by this runtime.
