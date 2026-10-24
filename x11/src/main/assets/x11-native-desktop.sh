#!/bin/sh
# Fixed desktop operations for non-APT guests and native Termux.
set -eu
export LC_ALL=C
op=${1:-};target=${2:-};base=${3:-/tmp/termux-jobs-x11}
case $target in alpine)
 pkgs='xfce4 xfdesktop xfce4-panel xfce4-session xfce4-settings xfconf thunar xfwm4 xfce4-terminal python3 xauth xprop xwininfo dbus dbus-x11 fcitx5 fcitx5-gtk fcitx5-qt font-dejavu font-liberation adwaita-icon-theme hicolor-icon-theme ca-certificates libpulse gst-plugins-good'
 ;;
 fedora)
 pkgs='xfce4-about xfce4-session xfce4-panel xfdesktop xfwm4 xfce4-settings Thunar xfce4-terminal python3 xorg-x11-xauth xprop xwininfo dbus-daemon dbus-x11 dbus-tools fcitx5 fcitx5-gtk3 fcitx5-qt5 dejavu-sans-fonts liberation-sans-fonts adwaita-icon-theme hicolor-icon-theme ca-certificates pulseaudio-libs gstreamer1-plugins-good vulkan-loader libglvnd-egl mesa-libEGL mesa-libGL mesa-dri-drivers mesa-vulkan-drivers'
 ;;
 opensuse)
 pkgs='xfce4-pulseaudio-plugin xfce4-session xfce4-panel xfdesktop xfwm4 xfce4-settings thunar xfce4-terminal xfce4-appfinder libxfce4ui-tools python3 xauth xprop xwininfo dbus-1-daemon dbus-1-tools dbus-1-x11 fcitx5 fcitx5-gtk3 fcitx5-qt5 dejavu-fonts liberation-fonts adwaita-icon-theme hicolor-icon-theme ca-certificates ca-certificates-mozilla libpulse0 gstreamer-plugins-good libvulkan1 Mesa-dri Mesa-libGL1 Mesa-libEGL1'
 ;;
 void)
 pkgs='xfce4-session xfce4-panel xfdesktop xfwm4 xfce4-settings Thunar xfce4-terminal xfce4-appfinder libxfce4ui xfce4-pulseaudio-plugin python3 xauth xprop xwininfo dbus fcitx5 fcitx5-gtk fcitx5-qt dejavu-fonts-ttf liberation-fonts-ttf adwaita-icon-theme hicolor-icon-theme ca-certificates libpulseaudio gst-plugins-good1 vulkan-loader libglvnd mesa-dri libllvm18'
 ;;
 archlinux)
 pkgs='xfce4-session xfce4-panel xfdesktop xfwm4 xfce4-settings xfconf thunar xfce4-terminal xfce4-appfinder libxfce4ui python xorg-xauth xorg-xprop xorg-xwininfo dbus fcitx5 fcitx5-gtk fcitx5-qt ttf-dejavu ttf-liberation adwaita-icon-theme hicolor-icon-theme ca-certificates ca-certificates-utils libpulse gst-plugins-good mesa libglvnd vulkan-icd-loader llvm22-libs spirv-tools lm_sensors libdisplay-info xcb-util-keysyms'
 ;;
 termux)
 pkgs='xfce4 xfdesktop xfce4-panel xfce4-session xfce4-settings xfconf thunar xfwm4 xfce4-terminal exo gtk3 libx11 libc++ python xorg-xauth xorg-xprop xorg-xwininfo dbus fcitx5 fcitx5-gtk3 fcitx5-qt ttf-dejavu adwaita-icon-theme hicolor-icon-theme ca-certificates pulseaudio gst-plugins-good'
 : "${PREFIX:?}"
 if [ "${JOBS_X11_RENDERER:-cpu}" = wrapper ];then
  pkgs="$pkgs mesa libandroid-shmem libc++ libdrm libx11 libxcb libxshmfence libwayland zlib zstd util-linux"
 fi
 ;;*)echo ERROR:UNSUPPORTED_DESKTOP;exit 2;;esac
# Private bridge dependencies belong to this selected RPM rootfs only.
if [ "$target" = fedora ] && [ "${JOBS_X11_RENDERER:-cpu}" = bionic-glibc ];then
 pkgs="$pkgs libglvnd-glx libstdc++ libgcc libxcb libX11-xcb libXext libXxf86vm libdrm libxshmfence zlib-ng-compat"
fi
if [ "$target" = archlinux ] && [ "${JOBS_X11_RENDERER:-cpu}" = bionic-glibc ];then
 pkgs="$pkgs gcc-libs libx11 libxcb libxext libxxf86vm libdrm libxshmfence zlib"
fi
if [ "$target" = opensuse ] && [ "${JOBS_X11_RENDERER:-cpu}" = bionic-glibc ];then
 pkgs="$pkgs libstdc++6 libgcc_s1 libX11-xcb1 libXext6 libXxf86vm1 libxcb1 libxcb-present0 libxcb-dri3-0 libxcb-randr0 libxcb-xfixes0 libxcb-sync1 libxcb-shm0 libxcb-glx0 libdrm2 libxshmfence1 libz1"
fi
if [ "$target" = void ] && [ "${JOBS_X11_RENDERER:-cpu}" = bionic-glibc ];then
 pkgs="$pkgs libstdc++ libgcc libX11 libXext libXxf86vm libxcb libxshmfence libdrm zlib"
fi
# Request-local workaround for libfetch choosing an unreachable IPv6 route.
# Do not change the system network, repository configuration or desktop environment.
xbps_command() { ( unset FORCE_IPV6; FORCE_IPV4=1 "$@"; ); }
check() {
 if [ "$target" = alpine ];then apk info -e $pkgs >/dev/null 2>&1 || return 1
 elif [ "$target" = archlinux ];then pacman -Q -- $pkgs >/dev/null 2>&1 || return 1
 elif [ "$target" = void ];then
  for p in $pkgs;do [ "$(xbps_command xbps-query -p state -- "$p" 2>/dev/null)" = installed ] || return 1;done
 elif [ "$target" = fedora ] || [ "$target" = opensuse ];then rpm -q $pkgs >/dev/null 2>&1 || return 1
 else
  for p in $pkgs;do case "$(dpkg-query -W -f='${Status}' "$p" 2>/dev/null)" in *' ok installed') ;; *) return 1;; esac;done
 fi
 for c in xfdesktop xfce4-panel xfsettingsd xfconf-query python3 thunar xfce4-terminal xwininfo xprop xfwm4 xfce4-session dbus-run-session dbus-monitor fcitx5 xauth;do command -v "$c" >/dev/null || return 1;done
 if [ "$target" = termux ];then command -v exo-open >/dev/null || return 1;fi
 if [ "$target" = termux ] && [ "${JOBS_X11_RENDERER:-cpu}" = wrapper ];then command -v flock >/dev/null || return 1;fi
 # Manual X11 input remains usable if this optional automatic-input bridge fails.
 fcitx5 --version || echo WARNING:X11_INPUT_COMPONENT_UNAVAILABLE
}
case $op in
 check) check;;
 install)
  echo JOBS_APT_PHASE:update
  if [ "$target" = alpine ];then
   apk update --no-progress --progress-fd 3
   echo JOBS_APT_PHASE:install
   apk add --no-progress --progress-fd 3 $pkgs
   update-ca-certificates
  elif [ "$target" = archlinux ];then
   # A rolling rootfs must not receive a partial upgrade after database refresh.
   echo JOBS_APT_PHASE:install
   pacman -Syu --needed --noconfirm -- $pkgs
   update-ca-trust
  elif [ "$target" = void ];then
   xbps_command xbps-install -S
   echo JOBS_APT_PHASE:install
   xbps_command xbps-install -y -- $pkgs
   update-ca-certificates
  elif [ "$target" = opensuse ];then
   zypper --non-interactive refresh
   echo JOBS_APT_PHASE:install
   zypper --non-interactive install --no-recommends $pkgs
   update-ca-certificates
  elif [ "$target" = fedora ];then
   # Cisco's codec server returns regional 403s. Fedora's own noopenh264
   # satisfies libheif's ABI dependency; keep this repo choice command-local.
   # It is a stub, not an H.264 decoder. Never disable signature verification.
   dnf -y --disable-repo=fedora-cisco-openh264 --setopt=retries=3 --setopt=timeout=30 makecache
   echo JOBS_APT_PHASE:install
   dnf -y --disable-repo=fedora-cisco-openh264 --setopt=retries=3 --setopt=timeout=30 --setopt=install_weak_deps=False --setopt=max_parallel_downloads=6 install $pkgs
   update-ca-trust
  else
   export DEBIAN_FRONTEND=noninteractive
   apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 update 3>&1
   echo JOBS_APT_PHASE:install
   apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 install -y --no-remove --no-install-recommends $pkgs 3>&1
  fi
  check;;
 repair-launchers|firefox-compat-on|firefox-compat-off)
  export JOBS_X11_NATIVE_TARGET=$target
  if [ "$target" = termux ];then export JOBS_X11_NATIVE_PREFIX=$PREFIX;fi
  python3 "$base/x11-native-programs.py" "$op";;
 *)echo ERROR:UNSUPPORTED_DESKTOP_OPERATION;exit 2;;esac
