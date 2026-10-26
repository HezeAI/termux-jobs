# Pinned graphics components — v2.0.7

All fixed graphics archives used by this release are pinned in [HezeAI/termux-jobs-components](https://github.com/HezeAI/termux-jobs-components#readme). The app uses immutable asset URLs, sizes and SHA-256; it does not follow a moving component latest release. Previous assets/tags remain unchanged for older application versions.

| Components | Release |
| --- | --- |
| Mesa 26.2.5 Turnip/Zink for Ubuntu, Debian, Alpine, Fedora, openSUSE, Void, Arch; Bionic-for-glibc Mesa frontend | [mesa-26.2.5](https://github.com/HezeAI/termux-jobs-components/releases/tag/mesa-26.2.5) |
| Native Termux Turnip, Vulkan Loader, Termux Wrapper and private libsensors | [graphics-20261025](https://github.com/HezeAI/termux-jobs-components/releases/tag/graphics-20261025) |
| Kali style, Alpine musl VirGL client and default a6xx-a7xx-a8xx driver | [x11-extras-20261025](https://github.com/HezeAI/termux-jobs-components/releases/tag/x11-extras-20261025) |

## Updates and caches

The next launch of the selected renderer prepares missing/changed components. A running desktop keeps its existing loaded libraries until stopped and started. Valid runtime/archive caches are reused; a URL change alone does not force download of identical bytes. The new bridge ZIP is6,103,917bytes and its bundle hash changes, so older selected runtimes are repaired through the existing staged/locked flow. Imported selections remain intact. Turnip runtimes use versioned private directories; ordinary guest Mesa is untouched.

Kali and Alpine VirGL archives are optional downloads, not embedded APK payloads. They share a verified archive cache while activation stays target-specific. The default driver is downloaded/imported only when needed; choosing an existing import preserves that choice. Its upstream source is StevenMXZ; full sources/licenses remain in the components README/catalogue. User imports are not uploaded automatically.

Normal XFCE, Python, VirGL server/ANGLE and application packages continue using pkg/apt/apk/dnf/zypper/XBPS/pacman. This mirrors fixed graphics archives; it does not replace distribution repositories. Downloads retain normal TLS, size/hash verification, cancellation and no-follow/path/lock protections. A checksum verifies expected bytes, not driver safety or universal GPU compatibility.

---

# Historical graphics components — v2.0.6

Optional fixed graphics archives are distributed from the separate public [HezeAI/termux-jobs-components](https://github.com/HezeAI/termux-jobs-components) repository. Its releases do not appear in the application repository or affect the in-app updater. The app pins the immutable `graphics-20261025` set; it does not query a moving component latest release.

The [English manifest](https://github.com/HezeAI/termux-jobs-components/blob/graphics-20261025/components.json) lists eleven runtime archives, their supported targets, original URLs, exact sizes and SHA-256. It includes six lfdevs Mesa26.3.0-devel-20260824 container bundles (openSUSE uses the Debian bundle plus private libsensors), Termux Turnip26.2.5, Vulkan Loader1.4.366, Termux Wrapper0.0.5r5 and the established Bionic-for-glibc runtime. Mirroring development Mesa does not make it a stable upstream release.

Native distribution dependencies, XFCE, Python, VirGL/ANGLE packages and normal applications continue using apt/apk/dnf/zypper/XBPS/pacman/pkg as appropriate. This repository mirrors fixed renderer archives; it is not a replacement Linux package repository. User-imported driver ZIPs are not uploaded automatically or included in this set.

## Preparation and cache behavior

Choose a supported renderer and import/select a trusted Bionic Turnip before starting the desktop. If Bionic for glibc is missing, the existing launch-preparation flow downloads6,053,026bytes, verifies exact size/SHA-256, caches one verified ZIP and activates an allowlisted staged runtime under the selected target’s exclusive lock. The already valid runtime path returns before any network access. A verified saved ZIP can repair the same target’s runtime without another download. Separate glibc targets retain independent runtime/driver directories, shared desktop locks and displays.

Downloaded bytes must pass integrity checks before activation. Cancellation, HTTP/network failure or mismatch leaves the previous runtime and driver selection intact. Turnip and Termux Wrapper retain their existing verified-cache/repair behavior. Merely changing a URL does not invalidate installed payloads. No global loader, guest/system Mesa or agent environment is replaced. Imported native drivers have the app’s privileges; validation is not a sandbox.

The old pinned Termux Vulkan Loader1.4.365 URL returned404. Its reviewed replacement1.4.366 was downloaded and verified against official Termux package metadata, then mirrored with the same bytes/hash. This addresses installer download failure on any GPU; actual rendering compatibility remains driver/device specific. The developer’s separate Adreno740 screenshot showed manually installed Turnip rendering vkcube, not acceptance of every Wrapper ZIP or Adreno750 setup.

## Upstream sources and licenses

Mesa/Turnip/Zink are upstream projects. Container builds are by **lfdevs**; native Termux packages are by **Termux maintainers**, with **Khronos Vulkan Loader**. The Termux Bionic Wrapper is **Lee Gao’s** implementation, packaged by **sabamdarif**, with **Billy Laws and contributors’ AdrenoTools/linkernsbypass**. Bionic for glibc uses pinned **libhybris**, including **taowen/TAWC/ARLinux** work, the **Arm/xMeM Vulkan WSI layer**, private upstream **Mesa26.1.6**, and this fork’s ICD/integration patches. Debian provides the private libsensors compatibility package. HezeAI maintains the fork; Codex assists development.

Original licenses remain in force. The component release includes license notices, corresponding bridge source/build instructions, exact Mesa26.1.6 source and Debian lm-sensors source/packaging. See [the component README and license collection](https://github.com/HezeAI/termux-jobs-components/blob/graphics-20261025/README.md), [bridge provenance/build recipe](../../third-party/bionic-glibc/README.md) and [third-party credits](THIRD-PARTY.md).

Future component revisions get new immutable tags and explicit app pins. Published archives and checksums must not be overwritten.
