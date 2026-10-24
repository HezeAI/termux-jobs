# v2.0.4 — Bionic drivers for glibc desktops

Load ARM64 Bionic Turnip and compatible Qualcomm proprietary Vulkan drivers in
**Ubuntu, Debian, Fedora, Arch Linux, openSUSE and Void** through the new
**Bionic for glibc (libhybris + Jobs bridge)** renderer. Each environment retains
its own imported-driver library, runtime cache, locks, display and X11 process;
supported desktops can run side by side. Alpine/musl is outside this glibc path.
Termux keeps its separate AdrenoTools Wrapper.

### How to use it

1. Stop the selected environment's desktop.
2. Open **Terminal / Desktop → Rendering** and select **Bionic for glibc**.
3. Use the folder beside the renderer to import a trusted, matching Bionic driver
   ZIP. The row below shows the selected driver and opens the saved-driver list.
4. Start the desktop. Missing guest runtime dependencies use the existing
   per-environment package-manager preparation/confirmation flow.
5. Use **Programs → Recommended → Jobs GPU Test** for repeatable scenes, renderer
   information, a VSync comparison and a saved report. Installation creates its
   desktop shortcut. Compare the same scene, resolution, mode and driver.

Use Bionic/AdrenoTools ZIPs, not a glibc Linux-driver ZIP or a Magisk package.
A driver must support your GPU, Android API and firmware. Imported native code
runs with the application's privileges; ZIP/hash checks are not a sandbox.
CPU, Turnip + Zink and VirGL remain available; selecting a different renderer
while stopped restores that renderer's normal path without deleting imports.

### How the bridge was made

This is an ABI/loading and presentation bridge, not binary recompilation:

`guest OpenGL → private Mesa Zink → guest Vulkan loader → Jobs ICD + libhybris → imported Bionic Vulkan HAL → Android hardware buffers → Termux:X11`

- **libhybris** supplies the Bionic ABI/loading foundation. We use the pinned
  [taowen/TAWC fork](https://github.com/taowen/libhybris/tree/07dfa88569d60f00e34a9f127ccae2fcc0730618),
  informed by [TAWC GPU integration notes](https://github.com/wmww/tawc/blob/main/notes/gpu-strategy.md).
  Our patch adds the process-private pthread-barrier hooks used by this runtime;
  the selected process gets a static-TLS reserve. No system-libc patch is installed.
- **Arm / xMeM Vulkan WSI layer** supplies the X11/Vulkan presentation foundation:
  [pinned source](https://github.com/xMeM/vulkan-wsi-layer/tree/d5624d42d8b2debbd910ad25662a05c751eb38b7).
  Our patches initialize memory/presentation state, preserve image formats,
  bound the AHardwareBuffer handshake, support immediate presentation, retain
  the current GPU-completion wait and keep the private Complete/Idle reader
  alive during swapchain initialization. Geometry/event/lifetime fixes and
  bounded GPU failure handling are included. FIFO retains synchronized Present
  scheduling; this is separate from waiting for GPU completion.
- **Mesa 26.1.6** supplies the private upstream Zink/GLX/EGL frontend. Installed
  guest Mesa files are not replaced. The imported Turnip driver's own Mesa
  version is independent of this frontend. We build with a small libm symbol
  compatibility forwarder, without carrying the abandoned driver experiment.
- **Our native ICD and app integration** load only the explicitly selected HAL,
  forward Android hardware-buffer calls, validate private caches/imports, prepare
  each target's dependencies and bind Android libraries into that guest process.
  Turnip keeps its working buffer-export path. For Qualcomm proprietary driver
  IDs, native Android BGRA allocation followed by Vulkan import avoids the
  failing BGRA-export path; exact buffer format, usage, size and memory type
  are checked. That path advertises BGRA UNORM only.
- **Termux:X11** provides the existing embedded display and private DRI3 buffer
  transport. The native core is unchanged in this release; our bridge is not a
  drop-in ICD for arbitrary Xorg/Wayland servers. The AHardwareBuffer approach
  was also informed by [android-vulkan-bridge](https://github.com/kde-yyds/android-vulkan-bridge);
  its system-libc patch was not adopted. No Vortek or Venus is included.

The release includes a small runtime archive and a full bridge export containing
pinned upstream source archives, our source/patches, build notes, native probes,
checksums and licenses. The bridge export requires a compatible Termux:X11 host
and normal guest dependencies; it is not a standalone Android installer.

### Driver downloads and included reference ZIPs

- [StevenMXZ / Adreno-Tools-Drivers](https://github.com/StevenMXZ/Adreno-Tools-Drivers/releases)
- [The412Banner / Banners-Turnip](https://github.com/The412Banner/Banners-Turnip/releases)

The separate `Turnip_Gen8_V38.zip` and `891.11.zip` release assets are unmodified
reference copies from StevenMXZ, not drivers developed by Termux Jobs and not
embedded in the APK. V38 credits Mesa and whitebelyash's gen8 source; StevenMXZ's
891.11 release credits Qualcomm/Xiaomi sources and OnickerMeow. The412Banner is
an additional download/build source, not the author of these two mirrored ZIPs.
See [driver sources, exact hashes and compatibility](DRIVER-SOURCES-v2.0.4.md).
Use each project's current compatibility notes; neither archive is universal
across Adreno generations. 891.11 requires Android API36 and a matching A8xx GPU.

### Other desktop improvements

- Extra X11 keys in embedded and landscape views: Esc, Tab, arrows, modifiers,
  navigation, Delete and Enter, independent of the Android keyboard.
- Move the key panel with its right four-arrow tab; resize with its left tab.
  Keys reflow and held modifiers are released when the panel/session closes.
- Optional Jobs GPU Test for all eight X11 targets, with per-system dependencies,
  desktop shortcut and reports; VSync/uncapped testing uses the selected renderer.
- The optional Kali-inspired theme is approximately8% smaller using rounded
  panel/icon/font/cursor metrics. Existing original-theme restoration is retained.
  Whisker receives bottom padding; openSUSE popup placement still needs a visual
  check on the new theme candidate.

### Validation and limitations

The developer reported working Turnip bridge desktops across the supported glibc
systems and an approximately2× FPS improvement with working VSync in the tested
setup. These reports do not establish every GPU/driver/Android combination.
Isolated native tests verify Vulkan/GLX RGB readback, one-swapchain24-frame reuse,
interval changes, occlusion, odd-width/resize/front-buffer paths, gated GPU writes,
bounded GPU-wait failure, injected allocation failure, private-server interruption
and independent driver/cache/display fixtures. These probes have no visible
Android surface and are not phone refresh-rate or glmark2 benchmark measurements.

Qualcomm891.11 passed those private probes on API36 / Adreno830, but one concurrent
stress attempt timed out during a GLX interval transition. A paired retry and
three sequential runs passed; its cause remains unresolved. Treat proprietary
QCOM support as experimental; actual891.11 XFCE/browser/game acceptance and
other devices still need testing. Driver-specific API/firmware constraints apply.
The glibc runtime needs AArch64, glibc>=2.38 and GLIBCXX_3.4.32.

Automated/build details and artifact identity: [validation](VALIDATION-v2.0.4.md).
Historical implementation evidence is retained in the
[bridge notes](BIONIC_GLIBC.md) and [presentation audit](X11_PRESENTATION_AUDIT.md).

### Update compatibility

Same `com.termux`, signing identity, workspace schema8 and ARM64 ABI. APK
versionName **2.0.4**, versionCode **1366**, higher than the private1365 candidate.
Android8+ app / Android11+ X11 requirements remain. No valid-data reset,
account migration, driver deletion or rootfs reinstallation is required.
Stop important work before updating: Android can terminate app processes.
Restart a stopped desktop to load the refreshed private bridge/theme assets;
existing imported drivers stay in their respective caches. Agents, account stores
and their launch environments are not migrated by this release. Existing
provider/backup restrictions fromv2.0.3 remain in place.

The release includes `update.json` generated from the final APK, SHA256SUMS,
validation, the bridge exports and reference driver ZIPs. Native driver code has
its original authors/terms; no driver or upstream component is relicensed here.
HezeAI is the fork developer. Full [source attribution](THIRD-PARTY.md) is retained.

AI development assistance: OpenAI Codex
