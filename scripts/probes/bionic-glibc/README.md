# Isolated native bridge probes

These are development tests, not app startup actions. Compile with an ARM64 **glibc** compiler and guest X11/Vulkan/OpenGL headers. `probe-window.c` needs XCB and libdl; `probe-glx.c` needs X11 and libGL.so.1. Both verify actual X11 pixel readback and resize/presentation paths, not only an advertised renderer name. The Vulkan test takes a private abstract socket path and a private 16-byte cookie file through `JOBS_X11_SOCKET` and `JOBS_X11_COOKIE_PATH`; never point it at somebody else's desktop.

The Java harness starts a separate copy of the fork's pinned X11 core using a repository-private `app_process` class loader, a private temporary directory, random private display, `-nolisten tcp`, an ephemeral MIT-MAGIC-COOKIE-1 authority and `-disable-gpu-present`. Its app.jar contains DEX from the candidate APK, not installed-app data. Build the harness against the matching X11 classes/Android platform, D8 it, and make both classpath JARs read-only before starting ART. Keep all logs, sockets, ART caches and generated files inside `.verification/bionic-bridge`. Always terminate/wait for the exact child PID and remove the ephemeral cookie in a `finally` block. It must not launch an installed Activity/service or reuse a current terminal/server.

`x11-connect.c` is a **test-only** Xlib socket redirect for the private server's nonstandard temporary path. It redirects only the exact chosen test display, passing every other socket through. It is not shipped. Production proot binds already map the environment's Termux temporary directory to guest `/tmp`.

Bridge environment setup is documented in `x11-bionic-glibc-env.sh`. For a repository-only probe, use absolute runtime/driver paths inside the fixture rather than its fixed production guest bind. The implicit WSI manifest must be in a private XDG data directory with `JOBS_BIONIC_ENABLED=1`; registering only an explicit layer hides X11 extensions from Zink's normal discovery. No global shell/preload/hosts changes or system certificate/driver changes are needed.

The initial successful run is documented in `docs/fork/BIONIC_GLIBC.md`. Test success does not establish complete XFCE/device compatibility or performance.

`probe-swap.c` issues swap-interval one and zero requests against the same GLX window, covers/un-covers that window, and verifies a known RGB pixel. `JOBS_START_ZERO=1` starts with a zero request instead. Treat its rates as a clear/swap workload, not a glmark2 score. With a private server lacking an Android surface they are not visible display refresh measurements. Log the actual loaded renderer and interval mode; a GLX-reported interval alone did not expose the Mesa 26.0.8 Zink setter regression. Do not run two harnesses against the same fixture/cookie directory concurrently.

`probe-synchronization.c` additionally needs pthreads. It initializes the swapchain image to black, then gates the next GPU color write behind a host-signaled Vulkan event. Presentation waits on that submission's semaphore. X11 must eventually read the new RGB value without a second present. `JOBS_SYNC_MODE=0|1|2` selects IMMEDIATE, MAILBOX or FIFO; the default gate is 300 ms. The old direct-present path copied black before the gated write and failed this test. To exercise bounded GPU-wait failure, use `JOBS_SYNC_DELAY_MS=5500 JOBS_EXPECT_TIMEOUT=1` with IMMEDIATE: the following acquire must report device loss, and cleanup waits until the owned helper releases the GPU gate. Run only against the authenticated private harness, under an external process deadline; the test is not an installed-desktop action.

`probe-window` also prints advertised surface formats. Set `JOBS_REUSE_FRAMES=24`
to exercise reuse; `JOBS_EXPECT_SWAPCHAIN_ERROR=<VkResult>` is for an externally
injected allocation failure and verifies orderly device/surface cleanup. These
are probe controls only, not switches compiled into the shipped bridge.
