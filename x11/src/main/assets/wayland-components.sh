#!/bin/sh
# Guest Arch only. Explicit fresh-install closure, independent of the phone rootfs.
set -eu
[ "${2:-}" = archlinux ] || { echo ERROR:WAYLAND_ARCH_ONLY;exit 2; }
. /etc/os-release
case $ID in arch|archarm) ;; *) echo ERROR:WAYLAND_GUEST_OS;exit 2;;esac
pkgs='kwin plasma-workspace plasma-desktop plasma-integration kscreen breeze breeze-icons dolphin konsole qt6-wayland wayland-utils xorg-xwayland xorg-xauth dbus python fcitx5 fcitx5-gtk fcitx5-qt ttf-dejavu ttf-liberation noto-fonts noto-fonts-emoji hicolor-icon-theme ca-certificates ca-certificates-utils libpulse mesa libglvnd vulkan-icd-loader libdrm libx11 libxcb xcb-util-keysyms libdisplay-info llvm22-libs spirv-tools lm_sensors xkeyboard-config xdg-utils'
check(){
 pacman -Q -- $pkgs >/dev/null 2>&1 || return 1
 for c in startplasma-wayland kwin_wayland kwin_wayland_wrapper plasmashell dolphin konsole wayland-info Xwayland xauth dbus-run-session dbus-send dbus-monitor python3 fcitx5;do
  command -v "$c" >/dev/null || { echo "MISSING:wayland-binary:$c";return 1; }
 done
 QT_QPA_PLATFORM=offscreen kwin_wayland --help 2>/dev/null | grep -q -- --wayland-display || { echo MISSING:kwin-wayland-backend;return 1; }
 test -s /etc/ssl/certs/ca-certificates.crt || { echo MISSING:wayland-certificates;return 1; }
 test -s /usr/share/X11/xkb/rules/evdev || { echo MISSING:wayland-keyboard;return 1; }
 python3 -c 'import ctypes; [ctypes.CDLL(n) for n in ("libvulkan.so.1","libEGL.so.1","libLLVM.so.22.1","libdisplay-info.so.3","libSPIRV-Tools.so","libsensors.so.5","libwayland-client.so.0")]' || { echo MISSING:wayland-runtime;return 1; }
}
case ${1:-} in
 check) check;;
 install)
  echo JOBS_APT_PHASE:update
  # A full Arch upgrade; no partial -Sy/-S, forced overwrites or removal switches.
  pacman -Syu --needed --noconfirm -- $pkgs
  update-ca-trust
  check;;
 *) echo ERROR:WAYLAND_OPERATION;exit 2;;
esac
