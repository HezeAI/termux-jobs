# v2.0.6 graphics distribution

Pinned optional archives now use [Termux Job’s Components](https://github.com/HezeAI/termux-jobs-components). Original archive bytes, versions, credits, SHA-256 and sizes are retained; only the unavailable Vulkan Loader package is updated1.4.365→1.4.366. Bionic-for-glibc runtime bytes are unchanged but downloaded on demand rather than embedded in the APK. Source/license archives accompany the component set. This is distribution/integration work by HezeAI with Codex assistance, not authorship of Mesa, libhybris, WSI, AdrenoTools or vendor drivers. See [component manifest and license details](GRAPHICS-COMPONENTS.md). Ordinary package-manager dependencies are outside the mirror.

# Sources, authors and acknowledgements — v2.0

HezeAI is the developer of Termux Job's. Codex is an AI assistant helping with
implementation and investigation. This fork integrates upstream work; it does
not claim authorship of Xorg, XFCE, Mesa, the drivers or third-party programs.

## Base platform and desktop

| Component / authors | Source | How it is used |
| --- | --- | --- |
| Termux contributors | [termux-app](https://github.com/termux/termux-app), [termux-packages](https://github.com/termux/termux-packages) | Base Android terminal and native package repositories; original licenses retained. |
| PRoot / Termux contributors | [PRoot](https://github.com/proot-me/PRoot), [proot-distro](https://github.com/termux/proot-distro) | Selected Linux rootfs installation and execution; does not provide a new kernel or VM. |
| Termux:X11 contributors | [Pinned source fa3a8b4](https://github.com/termux/termux-x11/tree/fa3a8b430e2896a19f44c99a9cb056254615ae06) | Original ARM64 libXlorie from the verified nightly APK; Android adapters integrated in this fork. GPL-3.0. No claim that this native core was rebuilt here. |
| X.Org / xkeyboard-config authors | [xkeyboard-config](https://gitlab.freedesktop.org/xkeyboard-config/xkeyboard-config) | Termux package2.48-1 keyboard data. HPND/MIT notices in x11-keyboard.zip/COPYRIGHT. |
| XFCE contributors | [XFCE sources](https://gitlab.xfce.org/xfce) | XFCE, XFWM, Thunar and desktop programs installed from each distribution's repositories. Licenses remain those of each component. |
| Fcitx contributors | [Fcitx5](https://github.com/fcitx/fcitx5) | Installed input-method components; bridge to the Android keyboard. |
| PulseAudio contributors | [PulseAudio](https://gitlab.freedesktop.org/pulseaudio/pulseaudio) | Termux-side audio server and selected guest client libraries. |

Termux:X11 binary/source provenance, hashes and ABI declarations:
[third-party/termux-x11](../../third-party/termux-x11/). Preparation:
[scripts/prepare-x11.py](../../scripts/prepare-x11.py). The upstream native library
is not modified; the corresponding source tree and its dependency/submodule
references are at the exact pinned commit. Mutable nightly/package URLs are
verified against recorded hashes and fail closed if the pinned input changes.
The cache is not included in the repository, and a clean source build still
needs the documented toolchain and obtainable pinned inputs.

## Graphics and drivers

| Component / authors | Source / package provider | Embedded or downloaded |
| --- | --- | --- |
| Mesa contributors: Turnip, Zink, Gallium/virpipe | [Mesa source](https://gitlab.freedesktop.org/mesa/mesa), [Mesa releases](https://archive.mesa3d.org/) | Most packages downloaded per selected target; original Mesa component licenses apply. |
| lfdevs | [mesa-for-android-container](https://github.com/lfdevs/mesa-for-android-container/releases/tag/mesa-26.3.0-devel-20260824) | Optional pinned Linux-container Mesa26.3.0-devel-20260824 packages. These are lfdevs package builds, not binaries built by this fork. |
| Termux package maintainers | [Mesa recipes](https://github.com/termux/termux-packages/tree/master/packages/mesa) | Optional Termux Mesa26.2.5 Turnip and generic Vulkan loader1.4.366; downloaded and hash checked. |
| virglrenderer contributors | [virglrenderer source](https://gitlab.freedesktop.org/virgl/virglrenderer) | virglrenderer-android from Termux repositories; a private server/socket per desktop. |
| ANGLE contributors | [ANGLE source](https://chromium.googlesource.com/angle/angle) | angle-android from Termux repositories; selected GL or Vulkan backend. |
| leegao | [bionic-vulkan-wrapper](https://github.com/leegao/bionic-vulkan-wrapper), [build recipe](https://github.com/leegao/vulkan_wrapper_termux-packages/blob/dev/wrapper/wrapper/build_termux.sh) | Optional Termux Wrapper runtime, v0.0.5r5. |
| sabamdarif | [termux-desktop wrapper package](https://github.com/sabamdarif/termux-desktop/releases/tag/bionic-vulkan-wrapper-0.0.5r5) | Pinned prebuilt wrapper archive. Its wrapper library matches the author's release asset; reproducible binary build is not claimed. |
| bylaws / libadrenotools contributors | [libadrenotools](https://github.com/bylaws/libadrenotools) | Custom Adreno driver-loading work used by Wrapper. Imported Turnip ZIPs remain the user's selected third-party drivers. |

Actual target URLs, SHA-256 values, ABI paths and package sizes are recorded in
[x11-gpu.py](../../x11/src/main/assets/x11-gpu.py) and
[Wrapper provenance](../../third-party/bionic-wrapper/provenance.json).
For openSUSE, the private compatibility payload uses the recorded Debian Mesa
build plus its pinned libsensors dependency, without replacing RPM libraries.
Driver packages are not silently upgraded to unknown newer binaries.

### The embedded Alpine VirGL client

Alpine's package did not provide the required virpipe client. This fork includes
a minimal **unmodified upstream Mesa26.1.6** build for **aarch64-musl**, with only
Gallium virgl, EGL/GLX and GLES. No Turnip, Zink, Vulkan driver or LLVM is bundled
in this client. It is mounted only into the selected Alpine/VirGL desktop.

- [Original source archive](https://archive.mesa3d.org/mesa-26.1.6.tar.xz).
- [Build options, source/binary hashes and toolchain](../../third-party/virgl-client/build-info.json).
- [Build instructions](../../third-party/virgl-client/README.md).
- [Component license notices](../../third-party/virgl-client/LICENSE.txt), also embedded beside the client archive in the APK.

The source archive is unchanged. The private build and packaging, Android
integration and environment routing are this fork's work; Mesa itself belongs
to its upstream authors.

## Codec, programs and appearance

**OpenH264:** [Cisco source v2.6.0](https://github.com/cisco/openh264/tree/v2.6.0),
[Debian source/package2.6.0+dfsg-2](https://sources.debian.org/src/openh264/2.6.0%2Bdfsg-2/).
The embedded glibc library is extracted from the pinned Debian package for the
private openSUSE/Fedora Parole launcher. BSD-2-Clause and per-file notices are retained:
[provenance and copyright](../../third-party/openh264/). It does not replace an
RPM-managed library or turn every codec into a supported format.

**GStreamer OpenH264 bridge (Fedora Parole):** unmodified ARM64 plugin from
[Debian gst-plugins-bad1.0 1.26.2-3+deb13u3](https://sources.debian.org/src/gst-plugins-bad1.0/1.26.2-3%2Bdeb13u3/),
[upstream component](https://gitlab.freedesktop.org/gstreamer/gstreamer/-/tree/1.26.2/subprojects/gst-plugins-bad/ext/openh264).
Pinned download/member/hash and full per-file license notices are retained in
[third-party/gstreamer-openh264](../../third-party/gstreamer-openh264/), and notices
are included in the APK. It is extracted as a private plugin for Fedora Parole
with GStreamer1.26+, not installed as a Debian package or Android JNI library.
Fedora's [OpenH264 documentation](https://fedoraproject.org/wiki/OpenH264) explains
the decoder/plugin split and its Cisco repository; this integration uses its own
pinned assets and does not disable TLS/GPG checking.

Firefox, LibreOffice, Evince, Atril, Parole, Ristretto, Tumbler, Mousepad,
File Roller/Engrampa and GPU utilities remain upstream projects installed through
the selected distribution's package manager. The app does not claim their code,
icons or distribution packages as original work. See their installed package
metadata for exact versions, source packages and component licenses.

The optional dark XFCE style, window decorations and panel setup are generated
by this fork's [style preparation script](../../scripts/prepare-x11-style.py).
The wallpaper is an AI-generated asset selected by the developer; provenance
and prompt are in [design/x11-kali](../../design/x11-kali/). It is inspired by a
Kali-like visual style, not an official Kali Linux theme, distribution or
endorsement. Standard Adwaita/icons/fonts remain their upstream components.

## Historical experiments

GTK/Garcon diagnostic modules, LXQt and Touch Workspace were development
experiments. They are not active desktop choices or shipped diagnostic guest
libraries in v2.0. Historical reports and retired sources are labelled as such;
references to those experiments are not current feature or validation claims.

Our integration source, session/installer code, tests and compatibility changes
are published in [HezeAI/termux-jobs](https://github.com/HezeAI/termux-jobs).
Third-party licenses are retained; the fork's license does not replace them.


## v2.0.4 Bionic-for-glibc bridge

- libhybris + taowen/TAWC Android TLS work: pinned source07dfa88569d60f00e34a9f127ccae2fcc0730618; LGPL-2.1 and component licenses retained.
- Arm/xMeM Vulkan WSI layer: d5624d42d8b2debbd910ad25662a05c751eb38b7; MIT. Our patch covers memory initialization, GPU/Present synchronization, event/geometry lifetime and buffer handling.
- Mesa26.1.6 upstream Zink/GLX/EGL frontend: private build, unmodified upstream source, small build-time libm symbol forwarder; upstream component licenses retained.
- Halium Android headers:2c6ac3dcc4f8db593dd69906b0ec22822abfed91. TAWC GPU notes and kde-yyds/android-vulkan-bridge informed integration; no system-libc patch, Vortek or Venus is included.
- Jobs native ICD, private import/cache/environment integration and compatibility patches are supplied in this fork. Existing Termux:X11 core is unchanged. [Full pins/build/licenses](../../third-party/bionic-glibc/README.md) and [implementation](BIONIC_GLIBC.md).
- Driver downloads: [StevenMXZ](https://github.com/StevenMXZ/Adreno-Tools-Drivers/releases) and [The412Banner](https://github.com/The412Banner/Banners-Turnip/releases). The two separate reference release ZIPs are unmodified StevenMXZ assets; [exact origins/credits/hashes](DRIVER-SOURCES-v2.0.4.md). No vendor driver is embedded in the APK or bridge export.


## Historical disabled Wayland preview — published v2.0.5

The published v2.0.5 APK includes pinned experimental code/runtime inputs but disables
Wayland/KDE UI and backend operations. This is not a claim of a working public
KDE feature. The native parent derives from
[DroidDeck 0.6.0 / The412Banner and contributors](https://github.com/Droid-Deck/DroidDeck)
(pinned source commit `22eac0831d10671f6bc2ad914a93a0da5046c7df`).
Original component notices/licenses, AdrenoTools/linkernsbypass, Wayland/libffi,
LLVM libc++ and Vulkan headers are retained in the repository/runtime assets.
Jobs-specific build/input/ownership changes and the scoped KWin timer workaround
are documented in [experimental architecture](ARCH-WAYLAND.md) and the pinned
`third-party/droiddeck-wayland/` manifests. KDE/Qt source references identify their
upstream authors; they are not Jobs-owned desktop implementations.


v2.0.6 removes these Wayland-derived sources and binary
inputs. Their original sources/notices remain accessible in the published
[v2.0.5 tag](https://github.com/HezeAI/termux-jobs/tree/v2.0.5/third-party/droiddeck-wayland).
The current APK retains the independent X11/AdrenoTools/libhybris component
licenses; removing the Wayland experiment does not change those integrations.

## Agent Markdown rendering

The chat preview uses the existing [Markwon4.6.2](https://github.com/noties/Markwon/tree/v4.6.2)
core/strikethrough/linkify integration (Apache-2.0), the matching
[CommonMark Java0.13.0](https://github.com/commonmark/commonmark-java/tree/commonmark-parent-0.13.0)
parser and GFM tables extension (BSD-2-Clause). Native code/table views, copying,
streamed block reuse and application styling are fork integration. Notices are
included in `app/src/main/assets/markdown-NOTICE.txt`.
