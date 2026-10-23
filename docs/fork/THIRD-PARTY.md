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
| Termux package maintainers | [Mesa recipes](https://github.com/termux/termux-packages/tree/master/packages/mesa) | Optional Termux Mesa26.2.5 Turnip and generic Vulkan loader1.4.365; downloaded and hash checked. |
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
private openSUSE Parole launcher. BSD-2-Clause and per-file notices are retained:
[provenance and copyright](../../third-party/openh264/). It does not replace an
openSUSE RPM or turn every codec into a supported format.

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
