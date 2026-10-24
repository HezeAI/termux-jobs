# X11 presentation audit — private candidate, 2026-10-24

This audit concerns frame pacing and the new Ubuntu/Debian Bionic-for-glibc renderer. It does not change the embedded display's advertised refresh rate, globally disable synchronization, or remove the v2.0.1 clipped-copy fix.

## Configuration paths inspected

All eight desktop targets use the same embedded X11 view, which currently advertises 60 Hz. Android's actual display refresh and an application's submitted frames per second are separate values. A reported 60 Hz is not proof that every application is capped at 60 submitted frames per second.

| Renderer | Targets | XFCE compositor | X11 offloaded GPU Present copy |
| --- | --- | --- | --- |
| CPU | All eight | Enabled | Normal default |
| Turnip + Zink | Termux | Enabled | Normal default |
| Turnip + Zink | Ubuntu, Debian, Alpine, Fedora, openSUSE, Void, Arch | Disabled | Disabled: retain clipped copy |
| VirGL | All eight | Disabled | Disabled: retain clipped copy |
| Wrapper | Termux | Disabled | Disabled: retain clipped copy |
| Bionic for glibc | Ubuntu, Debian, Fedora, Arch, openSUSE and Void | Disabled | Disabled: retain clipped copy |

VirGL's Android-side server selects ANGLE/Vulkan for Termux, Debian, Fedora, Void and Arch, and ANGLE/OpenGL for Ubuntu, Alpine and openSUSE. Those are separate from the guest application's Vulkan swapchain. CPU and VirGL do not use the new bridge's WSI layer. Other renderers do not receive the bridge's library paths or imported driver selection.

The pinned X11 core schedules redraw work from Android's choreographer. Its Present counters advance before the attached-surface availability check, so a covered or background window need not have identical presentation costs. This observation alone does not establish the cause of a particular phone's 60-to-120 change.

## Identified differences

[glmark2 2023.01](https://github.com/glmark2/glmark2/blob/2023.01/src/gl-state-glx.cpp) normally requests swap interval zero. An offscreen run omits the window presentation path, so its score cannot be interpreted as a promised visible-window rate.

The bridge's original [xMeM/Arm WSI](https://github.com/xMeM/vulkan-wsi-layer/tree/d5624d42d8b2debbd910ad25662a05c751eb38b7) exposed FIFO and MAILBOX without an immediate X11 Present path. The existing native Mesa and pinned Lfdevs guest Mesa have their own immediate/async paths. Termux Wrapper uses a separate WSI implementation and must not be assumed identical to the new glibc bridge.

A second problem exists in [Mesa 26.0.8's Zink interval setter](https://gitlab.freedesktop.org/mesa/mesa/-/blob/mesa-26.0.8/src/gallium/drivers/zink/zink_kopper.c): it restores the previous mode before updating the swapchain. The local GLX probe reported interval zero while the WSI layer still received FIFO. Merely adding immediate-mode support to the layer did not fix that run. Mesa 26.1.6 retains the new mode and defers an update when an image is acquired.

## Evidence boundaries

The developer reported a working Ubuntu XFCE desktop, accelerated Zink/Adreno 830 GPU INFO, and more than 800 FPS in an offscreen run. A second imported Bionic Turnip ZIP was used for the device report; its exact identity was not established. The windowed cap was later clarified to concern Bionic for glibc, rather than the ordinary Turnip + Zink choice.

Isolated native probes use a private X11 core with no Android Activity or visible Android surface. They check GLX interval requests, a clear/swap workload, window occlusion and RGB readback. Representative CPU and native Termux Turnip runs exceeded 60 submissions per second, while the initial bridge kept FIFO. These are presentation-path checks, not glmark2 scores or complete phone/XFCE benchmarks.

The standalone native Termux Wrapper probe could not load a dependency of Android's libnativewindow from this Ubuntu probe context. That limitation does not establish a regression in the installed application's Wrapper. All 27 supported target/renderer combinations were inspected at source/configuration level; live GUI frame rates were not measured for all of them.

Tests retain private display authentication and exact child-process cleanup. Existing user sessions, installed environments, Android vendor libraries and global shell configuration are not modified for these checks.

## Final candidate checks

The private Mesa/WSI runtime passed application-requested interval changes 1 → 0 → 1. Initial reverse-transition testing found two upstream lifetime issues: IdleNotify could remain unread after the last CompleteNotify, and late events/free-pool scans could encounter retired image payloads. The patch now drains Present events on its own authenticated connection, interrupts only that reader on destruction, protects image payload retirement, and skips retired payloads. No idle polling loop or shared Xlib connection shutdown is used.

Two repeated paired native runs passed with different server processes/displays and different runtime/driver cache directories. The second remained alive after the first stopped. Readback confirmed that a covering black window stayed black during asynchronous rendering. A separate run covered resize, odd widths and front-buffer flush. A private-server interruption exited with an expected GLX error instead of hanging. These are isolated host probes; actual Ubuntu/Debian Android desktop acceptance and visible-window performance remain device checks.

The second candidate bundles unmodified Mesa 26.1.6 source with a small build-only atan2f symbol-baseline forwarder. It uses no Universal-driver changes. The existing CPU, Turnip + Zink, VirGL and Termux Wrapper runtime choices remain on their previous library paths. Source/build hashes, licenses and the forwarder source are retained under `third-party/bionic-glibc` and `x11/src/bridge`.

## Follow-up: incomplete frames in the bridge

The developer subsequently reported corrupted/flickering glmark2 geometry in both Ubuntu and Debian with Bionic Turnip, around 200 windowed FPS. The earlier high-rate command was identified as `glmark2 --off-screen --size 800x600`; that omits window presentation and is not a comparable visible-window benchmark. This distinction does not explain or excuse corrupted frames. The preceding constant-color probes did not cover a GPU workload still in flight when Present starts.

The X11 AHardwareBuffer backend supplies no GPU wait fence to X11 Present. Its direct MAILBOX/IMMEDIATE path waited for the previous payload before resetting it, submitted the current wait-semaphore payload, and then sent the pixmap without waiting for that current GPU work. FIFO's existing worker waited correctly. A new native regression probe initializes an image to black, gates its next GPU update behind a host event for 300 ms, and presents it with a render-complete semaphore. The previous runtime copied black in both MAILBOX and IMMEDIATE; FIFO passed. This reproduced incomplete-frame presentation independently of the GUI report.

Candidate 1360 waits for the current payload before sending that pixmap to X11. A five-second timeout records device loss and never releases the still-busy image for reuse; waiting acquisitions are notified and observe the error. FIFO keeps its existing worker. The direct modes retain their calling path and immediate mode retains ASYNC/zero-MSC scheduling. An alternative that routed all modes through the worker passed pixel checks but caused substantially lower rates in the local probe, so it was rejected.

The final native runtime passes the delayed-write test in FIFO, MAILBOX and IMMEDIATE. A 5.5-second GPU gate verifies the timeout/device-loss path and orderly cleanup after the GPU resumes. Paired isolated Ubuntu/Debian fixtures pass interval changes, occlusion and RGB readback, with different server PIDs/displays and the second still alive after the first exits. These protocol tests have no visible Android surface. They establish the synchronization correction, not a promised glmark2 score or completed on-device flicker acceptance.

## Geometry-query optimization (1361)

Profiling identified repeated synchronous `xcb_get_geometry` capability queries. An owned per-surface connection now selects StructureNotify and initializes a geometry snapshot after event selection. Subsequent calls drain available events without a roundtrip; a mutex protects readers, and destruction/disconnection invalidates the surface. Events older than the initial geometry reply cannot roll that snapshot back. Both capability APIs return surface loss when the window is destroyed. This leaves the current-render-payload GPU wait from 1360 intact.

The native geometry test verifies two windows, 24 alternating odd-size changes, both capability APIs, loss of one window without invalidating its sibling, and subsequent GPU pixel readback. Existing GLX resize/front-buffer, independent-server and delayed-write checks remain relevant. The no-surface clear/swap A/B rates overlap and vary across runs; reduced query overhead is established at protocol level, while a significant visible FPS improvement is not. No claim of matching offscreen rates is made.

## FIFO follow-up — candidate 1362

The developer reports about20 FPS with VSync enabled through the bridge versus about120 with ordinary Turnip+Zink. An isolated bridge run reproduced the slow FIFO path (about17–20 FPS). Removing the extra FIFO worker, while retaining the current-image GPU wait, X11 synchronous options and target-MSC progression, removed that collapse in the private clear/swap probe. The probe reports requested/reported intervals1→0→1, correct pixels and correct occlusion, including with Fedora's own loader/libraries. Because its private X11 server has no visible Android surface, its roughly200+ submitted FPS is not evidence of120Hz pacing on the real display. Phone acceptance is pending.

Mesa26.3 was tested separately with the same imported driver and the new WSI. It did not show an improvement sufficient to justify replacing the private26.1.6 runtime. The shipped candidate therefore changes presentation scheduling only; it retains the GPU-completion and clipped-copy fixes. Jobs GPU Test and glmark2 have different workloads/implementations; compare the same tool, scene, dimensions and driver rather than comparing their raw rates.

## Event-reader finding superseding the FIFO-handoff hypothesis —1363

Device follow-up disproved the assumption that1362's equal-rate headless interval phases represented usable VSync. Instrumentation showed repeated swapchain creation, modeFIFO, targetMSC0 and no completion events. The Present reader started during base initialization, saw the constructor's `VK_NOT_READY`, and exited. It now waits on its own event connection until explicit stop/connection loss; image extents for ConfigureNotify are initialized before thread creation. The native single-swapchain24-frame regression fails against the old runtime and passes against the correction. Normal interval phases now show approximately118/800/118 FPS rather than equal rates, with correct occlusion/readback. This is native protocol evidence; the developer must verify the visible phone path.

Both Fedora device reports were preserved privately before further testing. Termux Wrapper was clarified by the developer to work (120 with VSync,380 without); it is not changed by this correction. The same corrected glibc runtime is packaged for Ubuntu, Debian, Fedora and the newly added Arch target, with separate state/processes.

##1364 scope extension without a presentation-binary change

The developer subsequently reported roughly2× FPS and working VSync in the tested1363 bridge setup. openSUSE and Void integration in1364 reuses that exact runtime ZIP and all native hashes. Separate native probes with each guest's own libraries pass interval transitions, pixel readback and occlusion; six independent cache/display/server fixtures complete without one shutdown stopping another. This does not establish actual screen pacing on every distribution/device. Alpine/musl remains outside this glibc implementation.
