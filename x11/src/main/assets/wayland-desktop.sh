#!/usr/bin/env bash
# Private Arch KDE launcher. Does not enter Ubuntu or start an X11 server.
set -euo pipefail
op=${1:-};target=${2:-};control=${3:-}
[[ $target == archlinux && $op =~ ^(check|install|run)$ ]] || { echo ERROR:WAYLAND_ARGUMENTS;exit 2; }
: "${PREFIX:?}"
expected="${PREFIX%/usr}/workspace-x11-archlinux"
[[ ! -L $control && -d $control && -d $expected && $control -ef $expected ]] || { echo ERROR:WAYLAND_CONTROL;exit 2; }
for relative in containers/archlinux containers/archlinux/rootfs installed-rootfs/archlinux;do
 [[ ! -L "$PREFIX/var/lib/proot-distro/$relative" ]] || { echo ERROR:WAYLAND_ROOTFS_LINK;exit 2; }
done
[[ -d "$PREFIX/var/lib/proot-distro/containers/archlinux/rootfs" || -d "$PREFIX/var/lib/proot-distro/installed-rootfs/archlinux" ]] || { echo ERROR:INSTALL_ARCH_FIRST;exit 1; }
command -v proot-distro >/dev/null || { echo MISSING:proot-distro;exit 1; }
source "$control/x11-audio.sh"
# No host LD paths, display or bus can leak into the selected guest.
guest_env='export TMPDIR=/tmp; export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export XDG_DATA_DIRS=/usr/local/share:/usr/share XDG_CONFIG_DIRS=/etc/xdg; unset LD_PRELOAD LD_LIBRARY_PATH DISPLAY WAYLAND_DISPLAY DBUS_SESSION_BUS_ADDRESS DBUS_SYSTEM_BUS_ADDRESS SESSION_MANAGER; '
binds=(--bind "$control:/tmp/termux-jobs-x11")
guest(){ proot-distro login "${binds[@]}" archlinux -- /bin/sh -c "$guest_env$1"; }
if [[ $op == check ]];then
 [[ -x $PREFIX/bin/python3 ]] || { echo MISSING:host-python;exit; }
 if ! "$PREFIX/bin/python3" -B "$control/wayland-root.py" check-root "$control";then echo MISSING:root-permissions;exit;fi
 if guest 'sh /tmp/termux-jobs-x11/wayland-components.sh check archlinux' && jobs_audio_available;then
  if "$PREFIX/bin/python3" -B "$control/x11-gpu.py" check archlinux "${PREFIX%/usr}/workspace-x11-gpu";then echo READY:renderer:turnip;else echo MISSING:renderer:turnip;fi
  echo READY:desktop
 else echo MISSING:wayland;fi
 exit
fi
if [[ $op == install ]];then
 if [[ ! -x $PREFIX/bin/python3 ]];then pkg install -y python;fi
 "$PREFIX/bin/python3" -B "$control/wayland-root.py" repair-root "$control"
 if ! command -v flock >/dev/null;then pkg install -y util-linux;fi
 guest 'sh /tmp/termux-jobs-x11/wayland-components.sh install archlinux'
 if ! jobs_audio_available;then pkg install -y pulseaudio;fi
 jobs_audio_available || { echo ERROR:WAYLAND_AUDIO;exit 1; }
 echo READY:desktop;exit
fi
runtime=${JOBS_WAYLAND_RUNTIME:-}
[[ $runtime =~ /wl-[a-f0-9-]{36}$ && ${runtime%/*} -ef $control && -d $runtime && ! -L $runtime ]] || { echo ERROR:WAYLAND_RUNTIME;exit 2; }
[[ ${JOBS_X11_FOCUS_TOKEN:-} =~ ^[a-f0-9-]{36}$ ]] || { echo ERROR:WAYLAND_TOKEN;exit 2; }
"$PREFIX/bin/python3" -B "$control/wayland-root.py" check-root "$control"
"$PREFIX/bin/python3" -B "$control/wayland-root.py" prepare-device "$control" "$runtime"
gpu="${PREFIX%/usr}/workspace-x11-gpu"
"$PREFIX/bin/python3" -B "$control/x11-gpu.py" check archlinux "$gpu"
guest_tmp_id=${runtime##*/};guest_tmp_id=${guest_tmp_id#wl-};guest_tmp_id=${guest_tmp_id//-/}
guest_tmp="$control/wt-${guest_tmp_id:0:16}"
[[ -d $guest_tmp && ! -L $guest_tmp ]] || { echo ERROR:WAYLAND_RUNTIME;exit 2; }
binds=(--bind "$guest_tmp:/tmp" --bind "$control:/tmp/termux-jobs-x11" --bind "$runtime:/run/termux-jobs-wayland" --bind "$gpu:/tmp/termux-jobs-x11-gpu" --bind "$runtime/drm/dri:/dev/dri" --bind "/dev/kgsl-3d0:/dev/dri/renderD0" --bind "$runtime/drm/sys:/sys/dev/char" --bind "$runtime/drm/class:/sys/class/drm")
jobs_audio_start
# Only fixed controlled values enter the shell; token has been validated above.
guest "export JOBS_X11_FOCUS_TOKEN=$JOBS_X11_FOCUS_TOKEN JOBS_X11_DESKTOP=plasma JOBS_X11_TARGET=archlinux XDG_RUNTIME_DIR=/run/termux-jobs-wayland; export PULSE_SERVER=unix:$jobs_audio_guest/native PULSE_COOKIE=$jobs_audio_guest/cookie; exec dbus-run-session -- sh /tmp/termux-jobs-x11/wayland-session.sh"
