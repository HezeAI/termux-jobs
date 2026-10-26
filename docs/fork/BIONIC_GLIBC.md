# Current distribution — v2.0.7

The private Mesa Zink/GLX/EGL frontend now uses26.2.5. Its six non-Mesa bridge core files retain their prior bytes. The6,103,917-byte ZIP is downloaded from the pinned [Mesa26.2.5 component release](https://github.com/HezeAI/termux-jobs-components/releases/tag/mesa-26.2.5), never into guest system libraries. Bundle/file validation triggers selected-target preparation when an older cache differs; existing imported drivers remain selected. Matching archives/runtimes work offline. Stop/start the desktop to load updated libraries.

A protected built-in a6xx-a7xx-a8xx choice is available before preparation. Imported rows have confirmed removal while the target is stopped; other targets and the source ZIP remain intact. Source/build attribution is in [third-party/bionic-glibc](../../third-party/bionic-glibc/README.md) and the [components repository](https://github.com/HezeAI/termux-jobs-components#readme). No data reset or account/environment migration is required. The following versioned sections are historical.

---

# Historical distribution — v2.0.6

The established runtime bytes and imported-driver formats are unchanged. The ZIP is no longer embedded in APK assets: when missing, starting the selected supported glibc desktop downloads6,053,026bytes from the pinned [component release](https://github.com/HezeAI/termux-jobs-components/releases/tag/graphics-20261025), verifies its exact SHA-256/size, and prepares that target’s runtime under its existing lock. Select/import the driver before launch. Already valid runtime installations work offline without redownloading. A verified archive can repair that cache; download cancellation/failure or mismatch keeps the previous runtime and selected driver. Different targets retain separate caches and displays.

Corresponding source, patches and licenses accompany the component release. Ordinary guest dependencies still use native package managers. Historical sections below describe the bundles in their named candidates; they are not a claim that every driver/device was verified. See [distribution details](GRAPHICS-COMPONENTS.md).

---

# Bionic for glibc — v2.0.4

The bridge is included in v2.0.4 for six ordinary glibc desktops. See
[release notes](RELEASE-v2.0.4.md), [driver sources](DRIVER-SOURCES-v2.0.4.md) and
[release validation](VALIDATION-v2.0.4.md). The implementation/candidate history
below is retained with its original versions and evidence boundaries. QCOM891.11
remains experimental with one unresolved concurrent stress timeout; other driver
and GUI combinations are not implied by native probe success.

---

# Bionic for glibc — private experimental candidate

This candidate starts from v2.0.3 and adds X11 extra keys plus an optional bridge for ordinary Ubuntu, Debian, Fedora, Arch Linux, openSUSE and Void. It is not a stable release announcement.

## Use

Stop the selected ordinary Ubuntu, Debian, Fedora, Arch Linux, openSUSE or Void desktop. In **Terminal / Desktop → Rendering**, select **Bionic for glibc (libhybris + Jobs bridge)**. Use the folder beside that row to import a trusted ARM64 Bionic driver ZIP with `meta.json`. The row below restores the selected driver's name and opens the saved library; equal ZIPs reuse their content identity. Start the desktop normally. Missing graphics packages use the existing installation/confirmation flow. The bridge runtime is bundled; vendor drivers are imported by the user.

CPU, Turnip + Zink and VirGL remain available. Switching to one of them while the desktop is stopped disables this bridge for subsequent launches. Imported drivers are retained for reuse and never replace system/vendor libraries. All six supported glibc targets use different app cache/driver directories, service processes and X11 displays and can run concurrently. Existing Ubuntu driver imports keep their original directory. Fedora, Arch, openSUSE and Void use their existing rootfs and explicit native package-manager runtime dependencies. Agents, Ubuntu AI and other environments are outside these experimental targets.

The X11 extra keys panel is available in embedded and landscape fullscreen desktops: Esc, Tab, modifiers, arrows, Home/End, PgUp/PgDn, Delete and Enter. Modifiers latch; closing/switching/detaching releases held keys. It is independent of the interactive Android keyboard and does not send text into an agent chat. A small left tab resizes it in both dimensions; keys reflow within the panel without replacing their views or releasing latched modifiers. The right four-arrow tab moves the panel. Both gestures remain inside the visible desktop, including after viewport changes; ordinary key touches do not drag it. These preferences last for the current opening of the panel.

## Architecture

The selected desktop's OpenGL calls use an application-private upstream Mesa 26.1.6 Zink/GLX/EGL stack, then the guest's Vulkan loader. Installed guest Mesa files are not replaced. Our ICD dispatches into the explicitly imported Bionic Vulkan HAL through libhybris. The WSI layer shares Android hardware buffers through the pinned X11 server's private DRI3 protocol. Native driver code runs with the application's privileges. This is not arbitrary binary recompilation and does not establish compatibility of every ZIP.

Sources and attribution: [libhybris](https://github.com/libhybris/libhybris), [TAWC/ARLinux fork](https://github.com/taowen/libhybris), [TAWC GPU work](https://github.com/wmww/tawc/blob/main/notes/gpu-strategy.md), [Arm/xMeM WSI layer](https://github.com/xMeM/vulkan-wsi-layer). [android-vulkan-bridge](https://github.com/kde-yyds/android-vulkan-bridge) informed the AHardwareBuffer/WSI approach; its system-libc patch was not applied. No Vortek, Venus, Universal Adreno binary, Mesa modifications from the abandoned experiment, or vendor driver is included in the APK.

Our changes initialize missing WSI memory/presentation state, retain the requested image format, bound the X11 hardware-buffer handshake, link AHardwareBuffer dispatch directly, enlarge the isolated Bionic TLS reserve, and add process-private pthread barrier hooks. The second candidate also adds the WSI immediate/async path and uses private Mesa to avoid the 26.0.8 interval-setter regression; see the [presentation audit](X11_PRESENTATION_AUDIT.md). Explicit barrier attributes/process-shared barriers are not implemented. A per-process glibc static TLS reserve is required. The layer currently advertises Vulkan 1.3; the tested driver exposes 1.4, and the loader emits a version warning. No unsupported API compatibility is claimed.

## Validation evidence (2026-10-24)

The local isolated native probe used Android API 36 / Adreno 830v1 and the developer-provided **Turnip Gen8 V38** archive (SHA-256 `642cb514534dc6b712ce0fb16aa76db566359b6c8542f0577f8ba6dd3aff8674`). A dedicated X11 core process used a random private display, a private authentication cookie and no Android Activity or installed-app service. Its children were stopped on completion. Working terminals/desktops were not adopted or restarted.

Verified with real driver execution:

- A GPU command filled 4,096 bytes and all 1,024 words were read back correctly through glibc.
- A Vulkan X11 window presented a known color; X11 readback matched all RGB channels.
- Standard glibc OpenGL/GLX reported Zink over Adreno 830 using the imported Turnip. Tests verified two-color orientation, odd widths, window resize and front-buffer flush with X11 pixel readback.
- The stripped, bundled runtime and the same driver re-imported into an isolated application-cache fixture passed the Vulkan and GLX tests. The bridge did not require a global preload.

This is native protocol/rendering evidence, not a full XFCE screen/device acceptance check. No benchmark or improvement over existing Turnip/VirGL is claimed. The developer subsequently verified Ubuntu XFCE/GPU INFO with another Bionic Turnip ZIP. Debian XFCE, new panel move/resize gestures, repeated desktop stop/start, actual visible frame pacing and fresh-device installation still need device checks. Compatibility with other drivers, GPUs, firmware, glibc distributions and Android versions remains unverified.

Automated cache tests exercise offline setup, integrity failures and repair, missing/changed drivers, path links, cancellation, running-desktop locks, target isolation, and safe environment export. Source/JVM/build/artifact results are recorded separately in the private candidate evidence; they do not prove device behavior.

## Second candidate build result

`2.0.4-bionic-glibc-test.2`, versionCode **1358**, package `com.termux`, ARM64, 51,847,271 bytes. SHA-256: `7b3a6c06f2856def8a96b5d4a799db136a4b3c1c3b8b6f5482e4a3c59f7c4e5c`. Same signing identity; no automatic data reset, environment migration or vendor-driver replacement. The existing Ubuntu driver library remains at its previous path. Debian uses a separate app cache and lock; installing the bridge runtime refresh preserves imported drivers.

Added in this candidate:

- Movable extra-key panel with a right four-arrow tab and a left two-headed resize tab. Keys reflow without replacing the held-key views. The panel remains within its desktop viewport.
- Bionic-for-glibc support for ordinary Debian as well as Ubuntu, with separate services, displays, runtime/driver directories and preferences. The code/bundle is shared; loaded instances and writable state are separate.
- Private upstream Mesa 26.1.6 avoids the Ubuntu Mesa 26.0.8 swap-interval setter regression. The WSI layer exposes immediate/async presentation, drains late IdleNotify events, synchronizes retired image payload access, and uses a private Present event connection whose read can be interrupted without closing the application's Xlib connection. VSync is controlled by the application rather than forced off globally.
- Mesa labels for Turnip + Zink: Termux's pinned Turnip is 26.2.5; the Lfdevs guest builds are 26.3.0-devel, with Arch's payload marked 26.3.0. The selected Lfdevs mode displays its Android-adaptation note. Termux Wrapper's picker label no longer says test.

Final evidence: 84 targeted JVM tests and 33 Python tests passed; localization passed (584 checks); five-module modern lint passed with zero errors and the same 210 warnings. Initial tests with the previous three-choice Debian fixture failed; the fixture was updated and the full targeted 84-test set passed. APK archive, package/version, ARM64 ABI, signing, runtime definitions and forbidden-file/secret-pattern checks passed. Android JNI matches v2.0.3 byte-for-byte. Seven bundled ARM64 ELF files were checked to require no GLIBC newer than 2.38. Mesa's build-only atan2f compatibility forwarder preserves the existing ARM64 symbol ABI without replacing libc/libm.

Two repeated native paired runs passed against separate authenticated private X11 servers, driver/runtime cache copies and bridge client processes. Each checked Vulkan pixels, GLX interval changes **1 → 0 → 1**, an occluding window's unchanged pixels and final RGB readback. The second server/client remained active after the first completed. A separate GLX run passed odd widths, orientation, resize and front-buffer flush. Interrupting an owned private server made its test client exit with the expected GLX connection error within the bounded check; no child server was left running.

These paired probes execute on the build host with isolated fixtures, not inside two installed Ubuntu/Debian desktop UIs. They prove real native protocol execution and cache/process separation, not full Debian XFCE or device gesture acceptance. Rates vary with phone state and concurrent load; the workload is a simple clear/swap loop, not a glmark2 score. No 1,000-FPS or arbitrary-driver compatibility guarantee is made. The developer should check the new panel tabs in embedded/landscape modes and run the windowed GPU test in Ubuntu and Debian. Full fresh-device installation, the new Debian desktop mode and other phones remain unverified for this candidate.

## AdrenoTools-format driver import follow-up

Candidate `2.0.4-bionic-glibc-test.3` / **1359** (51,847,151 bytes; SHA-256 `67aa183b87277db89c0a3ace66c33ac8b03ab6ff400fd07cd94be887b70f3c6d`) removes an unintended `lib` filename-prefix requirement. The importer and UI metadata reader now accept bounded ASCII `.so` basenames such as `vulkan.adr891.so`, `vkbgsl.so` and `vkb891.qti.hardware.hexlp-V2-ndk.so`. Archive traversal, symlink, duplicate, expanded-size, ELF/ARM64/Bionic, minimum-API, active-desktop lock and per-file hash checks remain in effect. Import still copies only validated libraries and does not execute archive scripts or load the driver.

Termux's label is **Wrapper · Bionic (AdrenoTools)**. The guest label is **Bionic for glibc (libhybris + Jobs bridge)**. A note between rendering and selected-driver rows reads **Bionic Turnip or Qualcomm drivers (AdrenoTools ZIP format).** This describes accepted file format; import acceptance does not establish driver/runtime compatibility. The guest bridge does not use AdrenoTools for loading.

The developer's exact `891.11.zip` (SHA-256 `750491dae3b82f5e8503988c1571a5666ec628c3bbebbf696d02346e6edc5223`) reproduced `WRAPPER_DRIVER_METADATA` before the fix. After the fix, isolated import, selection and hash verification passed for its main library and nine dependencies. Its metadata requires Android API 36; this phone reports API 36. No code from this archive was executed, and none of its proprietary binaries is included in the APK. Termux Wrapper and Ubuntu/Debian libhybris compatibility with this Qualcomm driver still require separate runtime tests.

Synthetic regression checks cover the new basename format, dependency preservation, catalogue/selection, tampering, old Turnip imports, invalid/non-ARM64 files, traversal, API mismatch and preserving the previous selection on failure. The existing bridge runtime and native payload are unchanged by this follow-up. The final 22-test JVM set and 36-test Python set passed. Localization passed (584 checks); five-module modern lint passed with zero errors and 210 existing warnings. APK build, identity/signature/version, archive, source/ABI/runtime-definition and secret-pattern checks passed. Its bridge runtime ZIP and JNI match candidate 1358 byte-for-byte.


## Current-frame synchronization fix — candidate 1360

`2.0.4-bionic-glibc-test.4`, versionCode **1360**, package `com.termux`, ARM64, 51,847,319 bytes. SHA-256: `9dbbd1828bb6ca716058bde02d972e7db32d9f7df3c4c4ce18df2e9b47752084`. The signing certificate is unchanged. No user data reset, rootfs migration or driver-library reset is introduced; the bundled private runtime refresh keeps imported drivers and selections.

The developer reported flickering/corrupted windowed glmark2 geometry in both Ubuntu and Debian with Turnip. A deterministic native test reproduced a missing wait for the current GPU render payload in the WSI direct-present path. The fix waits for completed GPU work before X11 copies the image, retains immediate/ASYNC presentation without introducing a forced vblank interval, and reports a five-second GPU wait timeout without freeing that still-busy image. Only the WSI ELF differs from the preceding bridge bundle; Mesa, libhybris, the ICD, Android JNI and the other renderers retain their previous binaries/paths. Details and benchmark comparison boundaries are in the presentation audit.

The final native runtime passes delayed GPU writes in IMMEDIATE, MAILBOX and FIFO, plus an injected 5.5-second GPU delay that reaches device loss and clean shutdown after release. Separate paired Ubuntu/Debian fixtures pass Vulkan pixel readback, GLX interval changes 1 → 0 → 1, occlusion, and final RGB values; the second private server remains alive after the first exits. The final resize/odd-width/front-buffer test also passes. Interrupting an owned X11 server yields the expected bounded GLX failure rather than a hang. These are isolated native protocol tests without an Android Activity/surface, not a completed visual glmark2 acceptance run.

The 36-test Python cache/import suite and 584 localization checks pass. Native WSI compilation and the release APK build pass. Final APK CRC, package/version, ARM64 ABI, unchanged signer, runtime definitions, asset/source consistency and forbidden-file/secret-pattern checks pass. JVM UI and modern lint were not rerun for this native-only change; their earlier candidate results above remain historical. The developer should stop and relaunch the selected Ubuntu/Debian desktop after updating, choose the working Turnip, and check the normal windowed GPU test for corruption and frame rate.

The developer also reported that Qualcomm `891.11.zip` loads far enough to identify `QUALCOMM_PROPRIETARY` in both guest environments, but swapchain memory allocation returns `VK_ERROR_OUT_OF_HOST_MEMORY`. This is a driver/bridge allocation failure, not evidence by itself that the phone has exhausted physical RAM. Guest Qualcomm compatibility is deferred; accepted ZIP format is not a runtime support claim. The synchronization fix is validated with Turnip only.

## Developer device follow-up for candidate 1360

The developer reports that the visible flickering is gone in Ubuntu and Debian. Four screenshots from 2026-10-24 show the same Zink/Adreno 830 Turnip renderer, private Mesa 26.1.6 and 800×600 surface configuration. On the first three displayed scenes, Ubuntu reports 236/231/232 FPS windowed and 746/723/714 with `glmark2 --off-screen --size 800x600`; Debian reports 233/232/235 windowed and 763/732/725 offscreen. These are individual scene rates, not final benchmark scores. The offscreen command bypasses presentation and is not a VSync-unlock command. The windowed runs exceed 120 FPS, ruling out a 60/120 submission cap in those particular runs.

The close results across both guests are consistent with costs in their shared bridge/presentation path. They do not isolate how much time belongs to GPU work, waiting, copies, X11 or Android presentation. The precise imported Turnip build, thermal state and background workloads were not recorded; the earlier reported 2,000+ result is not a controlled comparison. Static screenshots show intact geometry, while absence of temporal flicker is attributed to the developer's observation. No further runtime change or APK rebuild was made for this evidence-only follow-up.

## Candidate 1361: geometry queries and Jobs GPU Test

`2.0.4-bionic-glibc-test.5`, versionCode **1361**, package `com.termux`, ARM64, 51,856,433 bytes. SHA-256: `8ef7a1548933989816f2e21f4b636461d2ecefc510b33be0b0fe647ba8536f69`. The signer and Android JNI are unchanged. No user-data reset, environment migration or imported-driver reset is introduced.

The selected Ubuntu/Debian bridge now initializes window geometry once and updates it from ConfigureNotify/DestroyNotify events on an owned X11 connection. It does not consume the application's events or change its event mask. Capability queries use a mutex-protected snapshot and nonblocking event drain, with no new polling timer or helper thread. Initial queued events older than the geometry snapshot are ignored. Both surface-capabilities APIs propagate surface loss. GPU completion waits and immediate/async presentation remain unchanged from 1360.

A native test passes 24 alternating odd-size changes across two distinct windows, checks both capability APIs, verifies that a destroyed sibling surface is lost while the live surface remains valid, then presents/readbacks GPU pixels. The geometry query cost is reduced, but an interleaved A/B comparison has overlapping FPS ranges: **a substantial end-to-end FPS increase is not established**. Early profiling overlapped the developer's offscreen test and was excluded from speed conclusions. These are private-server protocol measurements, not visible glmark2 device results.

[Jobs GPU Test](JOBS_GPU_TEST.md) is an optional recommended program for all eight X11 environments, with a GPU TEST shortcut, three scenes, frame statistics, a per-window VSync switch and a manual JSON report. It uses the selected desktop renderer. Native tests ran every scene through the real Turnip bridge and verified X11 pixels. They do not establish GUI/package-install compatibility on every distribution or phone. Existing GPU and GPU INFO shortcuts remain available.

Eight new Python tests, 56 existing program tests, and 26 targeted Java program/resume tests pass. The release APK build, identity/signature, archive/ABI/runtime definitions, source-asset and secret-pattern checks pass. The final private bridge also passes independent paired Ubuntu/Debian servers, interval changes, occlusion and delayed GPU rendering. See the private verification reports for exact logs; phone acceptance of the new app and optimization remains pending.

## First candidate build result (historical: 1357)

`2.0.4-bionic-glibc-test`, versionCode **1357**, package `com.termux`, ARM64, 46,223,907 bytes. SHA-256: `dfc1f6c8a84bbc871fdd242c8c303845ecfbb8db03df5307312039113dbafe8a`. APK integrity/signature/runtime checks passed; the signer and Android JNI payload match v2.0.3. No data reset or automatic rootfs migration is introduced.

The targeted JVM run exercised 140 tests. Its only failure was an old three-choice Ubuntu menu expectation; after updating that fixture, all 11 renderer tests passed on the focused rerun. The 73-test Python regression set passed, followed by a 12-test bridge rerun including a new missing-dependency gate test. Localization checks passed; five-module modern lint completed with zero errors and 210 existing warnings. These results remain separate from the device checks above.

## Candidate 1362 — FIFO, Fedora and local test installation

The same packaged WSI correction applies to Ubuntu, Debian and Fedora. Presentation now uses the calling thread in IMMEDIATE, MAILBOX and FIFO, avoiding an extra FIFO presentation-worker handoff. It still waits for the current GPU payload, retains bounded timeout/device-loss handling and never returns an unfinished image to the free pool. FIFO retains synchronous X11 Present options and target-MSC pacing; no global VSync override or change to another application's requested interval is made. Private Mesa remains 26.1.6. A same-driver isolated comparison with the existing Lfdevs 26.3 prebuilt did not demonstrate an improvement; it was not substituted into the APK. Deep optimization is deferred.

Fedora's fourth renderer choice uses `workspace-x11-bionic-glibc-fedora`, separate from the retained Ubuntu and Debian paths. Java, service, shell and helper guards accept only these three glibc targets. Fedora's package checks and installation explicitly include the bridge's GLVND, X11/xcb, libstdc++, libgcc, libdrm, libxshmfence and zlib compatibility packages in that Fedora rootfs. Selected-renderer installation receives the explicit renderer value. Nothing changes its other renderers or creates a second distribution.

In the developer's Termux installation screenshot, APT stopped at `mirror.rinarin.dev` because the downloaded package index size differed from signed metadata (`587349 != 588169`). No checksum/signature bypass was introduced. Jobs GPU Test no longer depends on Termux's glmark2/mesa-demos. When its complete runtime package set is already configured, the local payload/shortcuts install without an APT update or other network command; missing dependencies still use the existing authenticated transaction. The same payload-only path works for configured APT guests and native targets. Shared dependencies/reports are retained on removal.

Native probes pass delayed GPU writes in IMMEDIATE/MAILBOX/FIFO, injected GPU timeout/device loss, interval changes, known-pixel readback and occlusion. A separate probe uses the installed Fedora loader/libraries read-only, with all writable fixtures in the repository; it passes Vulkan/GLX rendering. Three private servers, displays and driver/runtime caches run together; the Ubuntu probe exits while Debian and Fedora remain alive. These are native protocol tests without an Android Activity/surface, not a Fedora XFCE acceptance test or a promise of a particular panel FPS. See `.verification/bridge-mesa-next` for private logs.

Update keeps com.termux, the signing certificate and schema8, raises versionCode from1361 to1362, and performs no data reset/migration. Existing imports are retained; an outdated bridge bundle is repaired only in its selected cache while that desktop is stopped. Stop/relaunch the chosen desktop after updating. Device checks still needed: Fedora launch with a trusted Turnip ZIP, Termux local GPU TEST installation, and the actual VSync/visual behavior in Ubuntu, Debian and Fedora. Other drivers and clean-device downloads remain unverified.

Final candidate checks:68 focused Python tests,57 JVM tests and584 localization checks pass. The five-module modern lint run reports zero errors and210 existing warnings. Resize/caps, destroyed-surface and owned-server disconnect regressions pass; expected disconnect returns a bounded GLX failure. Release build and final archive/package/version/ABI/signature/asset/private-data checks pass. The additional Termux-native headless GUI attempt was blocked by Android's linker namespace resolving the development preload to `/root`; no installed-app GUI result is claimed. Installer tests cover the actual offline entrypoint and missing-dependency failure path, including the APT guest path.

## Candidate1363 — confirmed event-reader startup bug and Arch

The developer reported that glibc-bridge VSync did not limit FPS in Ubuntu/Debian/Fedora after1362; Termux's separate Wrapper worked. The preserved Fedora reports have requested intervals0 and1 with similar frame rates. Those requests alone were insufficient to establish actual synchronization. The following finding supersedes the earlier explanation attributing the slowdown solely to a FIFO-worker handoff.

`init_platform()` starts the X11 Present event reader before `swapchain_base::init()` has changed its constructor status from `VK_NOT_READY` to `VK_SUCCESS`. The reader's initial `error_has_occured()` test interpreted `VK_NOT_READY` as fatal and exited. Complete/Idle events were not consumed; buffers were not reused, the swapchain was repeatedly recreated, and FIFO's target MSC stayed zero. This both defeated VSync and added substantial allocation/teardown work. The reader now uses its owned connection/stop lifetime rather than interpreting the unfinished constructor as a display failure. Configure events compare against an immutable extent captured before subscription, not base image metadata populated later.

A deterministic Vulkan regression reuses one swapchain for24 frames. The old runtime fails acquisition with `VK_ERROR_OUT_OF_DATE_KHR`; the fixed runtime completes all24 with known-pixel readback. In an isolated GLX run, interval1→0→1 changes about118→800→118 submitted FPS, preserving known pixels and the black covering window. These rates are a private clear/swap protocol diagnostic without an Android Activity/surface, not a game benchmark or promised phone rate. Delayed GPU completion/timeout protections are retained; there is no synthetic FPS cap or global VSync setting.

Arch Linux now offers the fourth Bionic-for-glibc renderer. Its `workspace-x11-bionic-glibc-archlinux` cache, driver selection, lock, preferences, X11 service and display are independent of Ubuntu/Debian/Fedora. The selected existing Arch rootfs receives its own reviewed runtime dependencies through pacman, including gcc-libs, X11/xcb, libdrm, libxshmfence and zlib. CPU, Turnip+Zink, VirGL, Termux Wrapper and agents are unchanged. Runtime imports remain in each existing target's private path.

Jobs GPU Test1.1 saves a new timestamped, uniquely named JSON for every manual Report press, including `vsync-0`, `vsync-1` or `vsync-na`. Earlier fixed-name reports are preserved. Explicit command-line `--report PATH` keeps intentional overwrite semantics for automation. Existing test installations need their optional payload refreshed/reinstalled to get this behavior.

Update retains com.termux, the signer and schema8, increments1362→1363 and resets no user data. Stop/relaunch the selected desktop to load the new private bridge; its pinned bundle repair preserves imported drivers and does not alter installed system Mesa. Device confirmation still needed for visible VSync/tearing, the Arch desktop and imported ZIP, and new manual report filenames. No arbitrary-driver or fresh-device compatibility claim is made.

## Developer acceptance of1363 and candidate1364 — openSUSE/Void

After installing1363 the developer reports approximately double the prior FPS and working VSync through the glibc bridge. This records their tested setup, without extending acceptance to every target/driver/device. The1364 bundle has exactly the same per-file hashes and ZIP SHA-256 as1363: no Mesa, WSI, ICD, libhybris or Android JNI binary changed for the two new integrations.

openSUSE and Void now offer the fourth Bionic-for-glibc renderer in their existing non-agent desktop. Each owns a `workspace-x11-bionic-glibc-<target>` driver/runtime cache and lock, preferences, service/display and selected-guest binds. Both native dependency checks and installation receive the selected bridge mode. openSUSE requests its native C++/GCC, X11/xcb, libdrm, libxshmfence and zlib runtime names with zypper; Void requests its corresponding native packages with XBPS. Dependencies are checked explicitly for a clean desktop baseline; missing bridge-only packages fail readiness. Existing Termux Wrapper, CPU, Turnip+Zink, VirGL, agents and Ubuntu AI paths retain their previous behavior.

All six isolated final-bundle driver/cache/display/server fixtures pass Vulkan/GLX known-pixel rendering, interval changes and occlusion. Fedora, Arch, openSUSE and Void probes use their guest loader/libraries read-only. The first server exits while the other five remain alive. These are native protocol checks without an Android Activity/surface. They do not replace phone GUI or fresh-device download acceptance for openSUSE/Void.

75 relevant Python tests pass, including new-target cache locks, all30 cross-target cache rejection pairs, missing native dependencies and unchanged other-renderer readiness.59 JVM tests cover choices, target UI/cache/preference channels, invalid-target rejection and existing program/presentation behavior. Localization validates584 strings. The release build and final APK identity/signature/version/ABI/CRC/asset/runtime definitions and secret-pattern checks pass. Exact evidence is retained privately in `.verification/bridge-next`.

Update remains com.termux/schema8 with the same signing certificate and increments1363→1364. No data reset or migration occurs; existing imports and caches are preserved. On the phone, stop the selected new target's desktop, choose Bionic for glibc, import a trusted Bionic ARM64 Turnip ZIP and launch normally. Native required dependencies use that selected target's existing confirmation/install flow. Visible launch/VSync and driver compatibility still need developer checks for each new target. Among the existing eight non-agent desktops, Alpine is the remaining integration because it uses musl; it cannot load this glibc bundle directly.

## Candidate 1365 — smaller theme and Qualcomm 891.11 investigation

The optional Kali-inspired theme uses desktop icons 74 instead of 80, bottom
panel 81 instead of 88, panel icons 52 instead of 56, top panel 44 instead of
48, and rounded font/cursor sizes about 8% smaller. The same theme helper is
used by all eight ordinary X11 targets; this does not change the X11 framebuffer
resolution, Android UI density or a theme-disabled desktop. Existing theme state
is upgraded on the next desktop launch; disabling the theme restores the saved
original settings. Whisker menu content receives 12 px of bottom padding to
separate its last category from the panel. Actual openSUSE popup placement needs
a developer screenshot/device check. No GTK observer module or input filter is
introduced, and distribution-specific menu plugins/favorites are preserved.

The supplied Qualcomm **891.11.zip**, SHA-256
`750491dae3b82f5e8503988c1571a5666ec628c3bbebbf696d02346e6edc5223`,
imports into a private fixture and loads all ten ELF members on Android 16 /
Adreno 830. The earlier failure occurs at Vulkan-exported BGRA Android-buffer
allocation, despite reported export support. RGBA allocation works but swaps
red/blue in the existing clipped X11 copy path; that shortcut is rejected.
Allocating a BGRA buffer with Android and importing it into Vulkan preserves
all RGB channels. The candidate selects this allocation path by
`VK_DRIVER_ID_QUALCOMM_PROPRIETARY`, not by the shared Qualcomm vendor ID;
Bionic Turnip retains its existing export path. Native import advertises only
BGRA UNORM, matching the exact buffer format required by the Vulkan import
rules. Recommended Android usage and external import support are queried;
allocation size and memory type come from the actual buffer properties. CPU
read/write usage keeps the existing clipped X11 copy usable. Failure tears down
only the affected swapchain and owned reader/connection; no driver or library is
written into an installed environment.

The normal direct Vulkan and GLX checks verify known RGB values, reuse of one
swapchain for 24 frames, requested intervals 1 → 0 → 1 and covered-window
clipping. One concurrent stress attempt timed out during a Qualcomm GLX interval
transition; that observation is retained in private evidence, rather than
counted as a passing test. Follow-up outcomes are recorded separately. These
checks have no visible Android surface and do not establish general Qualcomm
ZIP compatibility or finished browser/game/XFCE acceptance. The driver remains
user-imported; its binaries are not included in the APK. Six glibc targets receive
the same private runtime on their next stopped-desktop preparation; Termux's
AdrenoTools Wrapper, Alpine, other renderers and AI-agent sessions retain their
existing paths. No data reset or environment migration is required.

Follow-up for this private candidate: the paired retry and three additional
sequential Qualcomm runs passed; the stress timeout remains unexplained. Native
checks also passed odd-width/resize/front-buffer rendering, delayed GPU writes
in all three present modes, bounded GPU-wait failure, an injected native-buffer
allocation failure and private-server interruption. Runtime/theme tests: 75 +
29 passed; localization: 584 strings; APK and runtime-definition validation
passed. This does not supersede the driver and GUI limitations above.

Final private APK: `2.0.4-bionic-glibc-test.9`, versionCode **1365**, ARM64,
51,859,773 bytes, SHA-256
`056d7edefeb467141dd7adeee12bc9a9d9f5c7fbdc639ae441667606a353d700`.
Package/signing identity is retained and code increases from the preceding
1364 candidate. No reset/migration is performed; update-data compatibility is
source-reviewed, while phone acceptance remains the developer's next check.
