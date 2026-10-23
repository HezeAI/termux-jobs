#!/usr/bin/env bash
# Explicitly confirmed optional renderer components. Arch uses its full rolling upgrade.
set -euo pipefail
op=${1:-};target=${2:-};control=${3:-}
[[ $op == check || $op == install ]] || exit 2
case $target in termux|ubuntu|debian|alpine|fedora|opensuse|void|archlinux) ;; *) echo ERROR:VIRGL_TARGET;exit 2;;esac
: "${PREFIX:?}"
[[ $PREFIX == /data/data/com.termux/files/usr ]] || exit 2
fail() {
 # Only our own readiness marker. Never delete package data or another target.
 local directory="${PREFIX%/usr}/workspace-x11-virgl-$target"
 if [[ ! -L $directory && ! -L $directory/ready && -f $directory/ready ]];then rm -f -- "$directory/ready";fi
 echo "ERROR:$1";exit 1
}
backend=gl
case $target in void|fedora|debian|archlinux|termux) backend=vulkan;;esac
client_bind=();client_name=none
if [[ $target == alpine ]];then
 client_name=$("$PREFIX/bin/python3" -B "$control/x11-virgl-client.py" "$op" "$control/x11-virgl-alpine.zip") || { echo "$client_name";fail VIRGL_CLIENT_REQUIRED; }
 client_name=${client_name#JOBS_VIRGL_CLIENT=}
 [[ $client_name =~ ^client-[a-f0-9]{16}$ ]] || { echo ERROR:VIRGL_CLIENT_ID;exit 1; }
 client_bind=(--bind "${PREFIX%/usr}/workspace-x11-virgl-alpine:/tmp/termux-jobs-virgl")
fi
host_ready() {
 for package in virglrenderer-android angle-android mesa libc++;do
  case "$(dpkg-query -W -f='${Status}' "$package" 2>/dev/null)" in *' ok installed') ;;*) return 1;;esac
 done
 [[ -x $PREFIX/bin/virgl_test_server_android && -f $PREFIX/opt/angle-android/$backend/libEGL_angle.so && -f $PREFIX/opt/angle-android/$backend/libGLESv2_angle.so ]] || return 1
 local help
 help=$("$PREFIX/bin/virgl_test_server_android" --help 2>&1 || true)
 [[ $help == *--socket-path* && $help == *"--angle-$backend"* && $help == *--multi-clients* ]]
}
# Pass the allowlisted alias and operation as argv, never shell text. The guest
# keeps its own ABI-specific Mesa; the Android server never loads guest libraries.
guest_components() {
 [[ $target == termux ]] && return 0
 local entry=(login)
 # The Void image can name an absent /bin/bash in passwd; bypass login shell.
 if [[ $target == void ]];then entry=(run --work-dir /root);fi
 proot-distro "${entry[@]}" "${client_bind[@]}" "$target" -- /bin/sh -s -- "$1" "$target" "$client_name" <<'GUEST'
set -eu
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export XDG_DATA_DIRS=/usr/local/share:/usr/share XDG_CONFIG_DIRS=/etc/xdg LC_ALL=C
unset LD_LIBRARY_PATH LD_PRELOAD DBUS_SYSTEM_BUS_ADDRESS
op=$1;target=$2;client_name=$3
case $target in
 ubuntu|debian) packages='libgl1-mesa-dri libglx-mesa0 libegl-mesa0';;
 alpine) packages='mesa-dri-gallium mesa-gl mesa-egl';;
 fedora) packages='mesa-dri-drivers mesa-libGL mesa-libEGL';;
 opensuse) packages='Mesa-dri Mesa-libGL1 Mesa-libEGL1';;
 void) packages='mesa-dri libglvnd';;
 archlinux) packages='mesa libglvnd';;
 *) exit 2;;
esac
if [ "$op" = install ];then
 echo JOBS_APT_PHASE:install
 case $target in
  ubuntu|debian)
   export DEBIAN_FRONTEND=noninteractive
   echo JOBS_APT_PHASE:update
   apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 update 3>&1
   echo JOBS_APT_PHASE:install
   apt-get -o APT::Keep-Downloaded-Packages=true -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 install -y --no-remove --no-install-recommends $packages 3>&1;;
  alpine) apk --wait 30 --timeout 30 add $packages;;
  fedora) dnf --setopt=retries=3 --setopt=timeout=30 install -y $packages;;
  opensuse) zypper --non-interactive install --no-recommends $packages;;
  void) xbps-install -Sy $packages;;
  archlinux) pacman -Syu --needed --noconfirm -- $packages;;
 esac
fi
if [ "$target" = alpine ];then
 # Fixed application-owned mount. No global /etc/ld.so or system Mesa changes.
 [ -d "/tmp/termux-jobs-virgl/$client_name/lib" ] || exit 1
 export LD_LIBRARY_PATH="/tmp/termux-jobs-virgl/$client_name/lib" LIBGL_DRIVERS_PATH="/tmp/termux-jobs-virgl/$client_name/lib/dri"
fi
for package in $packages;do
 case $target in
  ubuntu|debian) case "$(dpkg-query -W -f='${Status}' "$package" 2>/dev/null)" in *' ok installed') ;;*) exit 1;;esac;;
  alpine) apk info -e "$package" >/dev/null;;
  fedora|opensuse) rpm -q -- "$package" >/dev/null;;
  void) xbps-query -p pkgver "$package" >/dev/null;;
  archlinux) pacman -Q -- "$package" >/dev/null;;
 esac
done
# Package presence alone is insufficient (Alpine 3.24 omits the virpipe driver).
python3 -B - <<'CAPABILITY'
import ctypes,glob,os
ctypes.CDLL('libGL.so.1')
paths=[os.environ['LD_LIBRARY_PATH']] if os.environ.get('LIBGL_DRIVERS_PATH','').startswith('/tmp/termux-jobs-virgl/') else ['/usr/lib','/usr/lib64','/usr/lib/aarch64-linux-gnu']
found=False
for folder in paths:
 for name in glob.glob(folder+'/libgallium*.so*')+glob.glob(folder+'/dri/swrast_dri.so'):
  try:
   with open(name,'rb') as stream:
    tail=b''
    while data:=stream.read(1024*1024):
     if b'virpipe\0' in tail+data:found=True;break
     tail=data[-8:]
  except OSError:pass
if not found:print('ERROR:VIRGL_GUEST_DRIVER_MISSING',flush=True);raise SystemExit(1)
CAPABILITY
GUEST
}
if [[ $op == install ]];then
 if ! host_ready;then
  echo JOBS_APT_PHASE:update
  apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 update 3>&1
  echo JOBS_APT_PHASE:install
  apt-get -o APT::Keep-Downloaded-Packages=true -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 install -y --no-remove --no-install-recommends virglrenderer-android angle-android mesa libc++ 3>&1
 fi
 if ! guest_components check;then guest_components install;fi
fi
host_ready && guest_components check || fail VIRGL_COMPONENTS_REQUIRED
echo READY:virgl
