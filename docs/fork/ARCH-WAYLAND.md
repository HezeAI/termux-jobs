# Retired Wayland/KDE experiment

The experimental Arch Wayland/KDE desktop was discontinued in v2.0.6 at the developer’s request. Its UI, compositor integration, native build inputs and separate program catalogue are removed. All eight ordinary environments retain their X11/XFCE desktops.

Published v2.0.5 contained disabled preview controls. Its original architecture and validation are historical and remain available in the [v2.0.5 source tag](https://github.com/HezeAI/termux-jobs/blob/v2.0.5/docs/fork/ARCH-WAYLAND.md). A previous experimental installation on another device is not automatically uninstalled by the new APK: the app does not delete rootfs packages or user files on update. Shared Qt/Wayland libraries required by ordinary applications or Mesa are not a Jobs Wayland desktop.

See [v2.0.6 release notes](RELEASE-v2.0.6.md) and [current validation](VALIDATION-v2.0.6.md).
