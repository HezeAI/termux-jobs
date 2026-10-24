# v2.0.4 reference drivers and download sources

Driver downloads:

- [StevenMXZ / Adreno-Tools-Drivers](https://github.com/StevenMXZ/Adreno-Tools-Drivers/releases).
- [The412Banner / Banners-Turnip](https://github.com/The412Banner/Banners-Turnip/releases) (the account sometimes referred to as412Thebanner).

The two separate reference ZIP assets below are byte-for-byte copies of StevenMXZ
releases. The412Banner is an additional source, not the author of these copies.
They are not included in the APK or bridge runtime/source export. Native drivers
execute with app privileges; use trusted sources and check GPU/API/firmware
compatibility. Turnip/Bionic ZIPs and Linux/glibc ZIPs are different artifacts;
import the Bionic/AdrenoTools variant for this bridge, not the Linux variant.

## 891.11.zip

- [Original download](https://github.com/StevenMXZ/Adreno-Tools-Drivers/releases/download/v891.11/891.11.zip) · [Upstream release](https://github.com/StevenMXZ/Adreno-Tools-Drivers/releases/tag/v891.11).
- Bytes: 34254704; SHA-256: `750491dae3b82f5e8503988c1571a5666ec628c3bbebbf696d02346e6edc5223`.
- Archive metadata: author **Qualcomm**, vendor **Qualcomm**, driver version **891.11**, minimum Android API **36**.
- Credits: Qualcomm; StevenMXZ packaging/distribution; Xiaomi sources; OnickerMeow credit in upstream release.

## Turnip_Gen8_V38.zip

- [Original download](https://github.com/StevenMXZ/Adreno-Tools-Drivers/releases/download/v27.0.0-R1/Turnip_Gen8_V38.zip) · [Upstream release](https://github.com/StevenMXZ/Adreno-Tools-Drivers/releases/tag/v27.0.0-R1).
- Bytes: 2765405; SHA-256: `642cb514534dc6b712ce0fb16aa76db566359b6c8542f0577f8ba6dd3aff8674`.
- Archive metadata: author **StevenMXZ**, vendor **Mesa**, driver version **Vulkan 1.4.363**, minimum Android API **28**.
- Credits: Mesa/Turnip contributors; whitebelyash gen8; StevenMXZ build/distribution.

Both supplied archives target A8xx according to their source releases. That does
not imply every A8xx firmware/device works. Turnip Gen8 V38 is the unpatched
reference ZIP, not StevenMXZ's separately named Patched or OneUI files. Its source
release links Mesa upstream, whitebelyash's gen8 branch and patch contributors;
see those original references for the build lineage. Qualcomm891.11 is proprietary,
extracted from Xiaomi sources per the upstream release and requires API36.
Original copyrights and component terms remain; these files are not developed
or relicensed by Termux Jobs. There is no Qualcomm driver source in our bridge
export and no claim that proprietary binaries have become open source.

Test boundary: Turnip has developer-reported bridge desktop checks.891.11 passed
private native Vulkan/GLX checks on API36/Adreno830; full XFCE/browser/game behavior
is not confirmed. One concurrent VSync-transition stress timeout remains
unresolved despite passing retries. See [release limitations](RELEASE-v2.0.4.md).

The glibc bridge's private Mesa26.1.6 Zink frontend is independent of the Mesa
version used by an imported Turnip Vulkan driver. CPU, existing Turnip+Zink,
VirGL and Termux's separate AdrenoTools Wrapper retain their own paths.
