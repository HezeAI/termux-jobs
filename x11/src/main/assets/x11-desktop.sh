#!/usr/bin/env bash
# Only invoked by the private desktop service; no commands arrive from profiles.
set -euo pipefail
op=${1:-}; target=${2:-}; control=${3:-}; desktop=${4:-xfce}
case $desktop in xfce) ;; *) echo ERROR:DESKTOP_CHOICE;exit 2;;esac
[[ $op == scale || $op == check || $op == install || $op == repair-launchers || $op == firefox-compat-on || $op == firefox-compat-off || $op == run ]] || exit 2
case $target in termux|ubuntu|jobs-x11-ubuntu|debian|alpine|fedora|opensuse|void|archlinux) ;; *) echo 'ERROR:UNSUPPORTED_DESKTOP';exit 2;;esac
: "${PREFIX:?}"
case ${JOBS_X11_STYLE:-0} in 0|1) ;;*) echo ERROR:STYLE_VALUE;exit 2;;esac
case ${JOBS_X11_SCALE:-100} in [5-9][0-9]|1[0-4][0-9]|150) ;; *) echo ERROR:X11_SCALE;exit 2;;esac
if [[ ${JOBS_X11_STYLE:-0} == 1 ]];then
 case $target in termux|ubuntu|debian|alpine|fedora|opensuse|void|archlinux) ;;*) echo ERROR:STYLE_TARGET;exit 2;;esac
fi
if [[ ${JOBS_X11_RENDERER:-cpu} == bionic-glibc && $target != ubuntu && $target != debian && $target != fedora && $target != archlinux && $target != opensuse && $target != void ]];then echo ERROR:BIONIC_TARGET_UNSUPPORTED;exit 2;fi
if [[ ${JOBS_X11_RENDERER:-cpu} == wrapper && $target != termux ]];then echo ERROR:WRAPPER_TERMUX_ONLY;exit 2;fi
jobs_bionic_root="${PREFIX%/usr}/workspace-x11-bionic-glibc"
if [[ $target == debian || $target == fedora || $target == archlinux || $target == opensuse || $target == void ]];then jobs_bionic_root+="-$target";fi
# Android bind mounts can expose the same directory under different paths.
# Compare filesystem identity, not pwd/realpath text. Never accept a different directory.
case $target in termux) expected="${PREFIX%/usr}/workspace-x11";; *) expected="${PREFIX%/usr}/workspace-x11-$target";;esac
# Exact old private control directory remains readable for upgrade/fixture compatibility.
legacy="${PREFIX%/usr}/workspace-x11"
if [[ -d $legacy && $control -ef $legacy ]];then expected=$legacy;fi
[[ -d $control && -d $expected && $control -ef $expected ]] || { echo 'ERROR:CONTROL_PATH';exit 2; }
# Actual app filesDir is /data/data/com.termux/files, PREFIX is files/usr.
source "$(dirname "${BASH_SOURCE[0]}")/x11-audio.sh"
fs_root="$PREFIX/var/lib/proot-distro"
if [[ $target != termux ]];then
for candidate in "$fs_root/containers/$target" "$fs_root/containers/$target/rootfs" "$fs_root/installed-rootfs/$target";do
    [[ ! -L $candidate ]] || { echo 'ERROR:ENVIRONMENT_IS_A_LINK';exit 2; }
done
fi
installed() { [[ -d $fs_root/containers/$target/rootfs || -d $fs_root/installed-rootfs/$target ]]; }
# Arch's imported root can be 0555. Fake-root permission probes then generate
# IN_ATTRIB feedback in Thunar. Inspect/repair only this root directory from
# native Termux, before starting a guest; never recursively change its contents.
arch_root_permissions() {
    [[ $target == archlinux ]] || return 0
    [[ -x $PREFIX/bin/python3 ]] || { echo MISSING:python3;return 1; }
    "$PREFIX/bin/python3" - "$fs_root" "$1" <<'ROOT_PERMISSIONS'
import os,stat,sys
base,operation=sys.argv[1:]
if operation not in ('check','repair'):raise SystemExit(2)
fd=None
try:
    # Open each selected-container component without following a rootfs link.
    for relative in ('containers/archlinux/rootfs','installed-rootfs/archlinux'):
        opened=[]
        try:
            parent=os.open(base,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW);opened.append(parent)
            for part in relative.split('/'):
                parent=os.open(part,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=parent);opened.append(parent)
            fd=opened.pop();break
        except FileNotFoundError:continue
        finally:
            for item in reversed(opened):os.close(item)
    if fd is None:raise ValueError('missing rootfs')
    info=os.fstat(fd)
    if not stat.S_ISDIR(info.st_mode) or info.st_uid!=os.getuid():raise ValueError('rootfs ownership')
    mode=stat.S_IMODE(info.st_mode)
    if mode & 0o700 != 0o700:
        if operation=='check':raise SystemExit(1)
        os.fchmod(fd,mode | 0o700)
        print('JOBS_X11_ROOT_PERMISSIONS=owner-access-restored',flush=True)
    if os.fstat(fd).st_mode & 0o700 != 0o700:raise ValueError('rootfs access')
except (OSError,ValueError):
    print('ERROR:ARCH_ROOTFS_OWNER_ACCESS',flush=True);raise SystemExit(1)
finally:
    if fd is not None:os.close(fd)
ROOT_PERMISSIONS
}
gpu_bind=()
jobs_native_bridge_env=
if [[ ( $target == fedora || $target == archlinux || $target == opensuse || $target == void ) && ${JOBS_X11_RENDERER:-cpu} == bionic-glibc ]];then jobs_native_bridge_env="JOBS_X11_RENDERER=bionic-glibc ";fi
# Void OCI has /bin/sh but its passwd still names absent /bin/bash.
# run executes our explicit command; login wraps it in that absent shell.
proot_entry=(login)
if [[ $target == void ]];then proot_entry=(run --work-dir /root);fi
# PRoot normally appends Termux bin. Graphical guest sessions must not use it
# as a fallback for missing guest programs or D-Bus services.
guest_environment='export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games; export XDG_DATA_DIRS=/usr/local/share:/usr/share XDG_CONFIG_DIRS=/etc/xdg; unset LD_LIBRARY_PATH LD_PRELOAD DBUS_SYSTEM_BUS_ADDRESS; '
guest() { proot-distro "${proot_entry[@]}" --bind "$PREFIX/tmp:/tmp" --bind "$control:/tmp/termux-jobs-x11" "${gpu_bind[@]}" "$target" -- /bin/sh -c "$guest_environment$1"; }
if [[ $op == scale ]];then
    [[ ${JOBS_X11_FOCUS_TOKEN:-} =~ ^[a-f0-9-]{36}$ ]] || { echo ERROR:FOCUS_TOKEN;exit 2; }
    if [[ $target == termux ]];then
        export JOBS_X11_TARGET=termux
        exec "$PREFIX/bin/python3" -B "$control/x11-scale-live.py" apply "${JOBS_X11_SCALE:-100}"
    else
        installed || { echo MISSING:environment;exit 1; }
        guest "export JOBS_X11_TARGET=$target JOBS_X11_FOCUS_TOKEN=$JOBS_X11_FOCUS_TOKEN; exec python3 -B /tmp/termux-jobs-x11/x11-scale-live.py apply ${JOBS_X11_SCALE:-100}"
    fi
    exit
fi
components='configured() { case "$(dpkg-query -W -f="\${Status}" "$1" 2>/dev/null)" in *" ok installed") return 0;; *) return 1;; esac; }; configured libpulse0 && configured gstreamer1.0-pulseaudio && test -s /etc/ssl/certs/ca-certificates.crt && configured ca-certificates && command -v python3 >/dev/null && command -v thunar >/dev/null && command -v xfce4-terminal >/dev/null && command -v xwininfo >/dev/null && command -v xprop >/dev/null && command -v xfdesktop >/dev/null && command -v xfce4-panel >/dev/null && command -v xfsettingsd >/dev/null && command -v xfconf-query >/dev/null && command -v xfwm4 >/dev/null && command -v xfce4-session >/dev/null && command -v dbus-run-session >/dev/null && command -v xauth >/dev/null && command -v fcitx5 >/dev/null && command -v dbus-monitor >/dev/null && configured fcitx5-frontend-gtk3 && configured fcitx5-frontend-qt5 && configured fonts-dejavu-core && configured fonts-liberation && configured adwaita-icon-theme && configured hicolor-icon-theme && { fcitx5 --version || echo WARNING:X11_INPUT_COMPONENT_UNAVAILABLE; }'
packages='xfce4 xfdesktop4 xfce4-panel xfce4-session xfce4-settings xfconf thunar xfwm4 xfce4-terminal'
if [[ ( $target == ubuntu || $target == debian ) && ${JOBS_X11_RENDERER:-cpu} == bionic-glibc ]];then
    jobs_bionic_packages='libgl1-mesa-dri libegl-mesa0 libglx-mesa0 libgl1 libegl1 libstdc++6 libvulkan1 libxcb1 libxcb-present0 libxcb-dri3-0 libx11-xcb1 libdrm2 zlib1g libxcb-randr0 libxcb-xfixes0 libxcb-sync1 libxshmfence1 libxcb-glx0 libxcb-shm0 libx11-6 libxext6 libxxf86vm1'
    packages+=" $jobs_bionic_packages"
    for jobs_bionic_package in $jobs_bionic_packages;do components+=" && configured $jobs_bionic_package";done
fi
if [[ $target == alpine || $target == fedora || $target == opensuse || $target == void || $target == archlinux ]];then components="sh /tmp/termux-jobs-x11/x11-native-desktop.sh check $target";if [[ ( $target == fedora || $target == archlinux || $target == opensuse || $target == void ) && ${JOBS_X11_RENDERER:-cpu} == bionic-glibc ]];then components="JOBS_X11_RENDERER=bionic-glibc $components";fi;fi
if [[ $target == termux ]];then components="sh $control/x11-native-desktop.sh check termux $control";fi
if [[ ( $target == alpine || $target == termux || $target == fedora || $target == opensuse || $target == void || $target == archlinux ) && ( $op == repair-launchers || $op == firefox-compat-on || $op == firefox-compat-off ) ]];then
    if [[ $target == alpine || $target == fedora || $target == opensuse || $target == void || $target == archlinux ]];then installed || { echo MISSING:environment;exit 1; };guest "sh /tmp/termux-jobs-x11/x11-native-desktop.sh $op $target"
    else sh "$control/x11-native-desktop.sh" "$op" termux "$control";fi
    echo READY:desktop;exit
fi
if [[ $op == firefox-compat-on || $op == firefox-compat-off ]];then
    [[ $target == ubuntu || $target == jobs-x11-ubuntu || $target == debian ]] || { echo ERROR:COMPAT_TEST_ONLY;exit 2; }
    installed || { echo MISSING:environment;exit 1; }
    if [[ $op == firefox-compat-on ]];then guest 'JOBS_X11_MAINTENANCE=1 python3 /tmp/termux-jobs-x11/x11-maintenance.py firefox-compat-on'
    else guest 'JOBS_X11_MAINTENANCE=1 python3 /tmp/termux-jobs-x11/x11-maintenance.py firefox-compat-off';fi
    echo READY:desktop;exit
fi
if [[ $op == repair-launchers ]];then
    [[ $target == ubuntu || $target == jobs-x11-ubuntu || $target == debian ]] || { echo 'ERROR:REPAIR_TARGET';exit 2; }
    installed || { echo 'MISSING:environment';exit 1; }
    guest 'JOBS_X11_LAUNCHERS_REPAIR=1 python3 /tmp/termux-jobs-x11/x11-launchers.py --repair'
    guest "$components"
    echo 'READY:desktop';exit
fi
# Authoritative read-only renderer check, separate from desktop readiness.
# A missing/stale cache must offer renderer repair, not reinstall the desktop.
check_renderer() {
    local mode=${JOBS_X11_RENDERER:-cpu}
    case $mode in
        cpu|wrapper) echo "READY:renderer:$mode";; # Wrapper has its own validated preparation.
        bionic-glibc)
            if "$PREFIX/bin/python3" -B "$control/x11-bionic-glibc.py" check "$target" "$jobs_bionic_root" - 0 -;then echo READY:renderer:bionic-glibc
            else echo MISSING:renderer:bionic-glibc;fi;;
        turnip)
            if "$PREFIX/bin/python3" -B "$control/x11-gpu.py" check "$target" "${PREFIX%/usr}/workspace-x11-gpu";then echo READY:renderer:turnip
            else echo MISSING:renderer:turnip;fi;;
        virgl)
            if bash "$control/x11-virgl-setup.sh" check "$target" "$control";then echo READY:renderer:virgl
            else echo MISSING:renderer:virgl;fi;;
        *) echo ERROR:INVALID_RENDERER;return 2;;
    esac
}
if [[ $op == check ]]; then
    if [[ $target == archlinux ]] && installed && ! arch_root_permissions check;then echo MISSING:root-permissions;exit 0;fi
    command -v setsid >/dev/null || { echo 'MISSING:setsid';exit; }
    if [[ ${JOBS_X11_STYLE:-0} == 1 && ! -x $PREFIX/bin/python3 ]];then echo MISSING:style-python;exit;fi
    desktop_ready=false
    if [[ $target == termux ]];then
        if eval "$components" && jobs_audio_available;then desktop_ready=true;else echo 'MISSING:xfce';fi
    elif ! command -v proot-distro >/dev/null || ! installed;then echo 'MISSING:environment'
    elif guest "$components";then if jobs_audio_available;then desktop_ready=true;else echo 'MISSING:audio';fi;else echo 'MISSING:xfce';fi
    if $desktop_ready;then check_renderer;echo 'READY:desktop';fi
    exit
fi
if [[ $op == install ]]; then
    # No host upgrade, no reset, no removal. All operations require app consent.
    # Debian is an existing user environment: never create/replace it from X11.
    if [[ $target == debian || $target == alpine || $target == fedora || $target == opensuse || $target == void || $target == archlinux ]] && ! installed;then echo 'ERROR:INSTALL_ENVIRONMENT_FIRST';exit 1;fi
    command -v setsid >/dev/null || { echo 'ERROR:SETSID_REQUIRED';exit 1; }
    if [[ ${JOBS_X11_STYLE:-0} == 1 && ! -x $PREFIX/bin/python3 ]];then pkg install -y python;fi
    if [[ $target == termux ]];then
        pkg install -y x11-repo
        sh "$control/x11-native-desktop.sh" install termux "$control"
    else
        command -v proot-distro >/dev/null || pkg install -y proot-distro
        if ! installed;then
            # Installing an absent ordinary Ubuntu is allowed only after consent too.
            if [[ $target == jobs-x11-ubuntu ]];then
                help=$(proot-distro install --help 2>&1 || true)
                if [[ $help == *--name* ]];then proot-distro install ubuntu --name jobs-x11-ubuntu
                else proot-distro install --override-alias jobs-x11-ubuntu ubuntu;fi
            elif [[ $target == ubuntu ]];then proot-distro install ubuntu
            else echo ERROR:INSTALL_ENVIRONMENT_FIRST;exit 1;fi
        fi
        if [[ $target == archlinux && ! -x $PREFIX/bin/python3 ]];then pkg install -y python;fi
        arch_root_permissions repair || exit 1
        if [[ $target == alpine || $target == fedora || $target == opensuse || $target == void || $target == archlinux ]];then guest "${jobs_native_bridge_env:-}sh /tmp/termux-jobs-x11/x11-native-desktop.sh install $target 3>&1"
        else
        guest "export LC_ALL=C; packages='$packages'; "'export DEBIAN_FRONTEND=noninteractive; echo JOBS_APT_PHASE:update; apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 update 3>&1 && { echo JOBS_APT_PHASE:install; apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 install -y --no-remove --no-install-recommends $packages x11-utils python3 libpulse0 gstreamer1.0-pulseaudio ca-certificates fonts-dejavu-core fonts-liberation adwaita-icon-theme hicolor-icon-theme dbus-x11 xauth fcitx5 fcitx5-frontend-gtk3 fcitx5-frontend-qt5 3>&1 && update-ca-certificates; }'
        fi
    fi
    arch_root_permissions repair || exit 1
    if ! jobs_audio_available;then pkg install -y pulseaudio;fi
    jobs_audio_available || { echo ERROR:AUDIO_COMPONENTS_REQUIRED;exit 1; }
    if [[ $target == termux ]];then eval "$components";else guest "$components";fi
    check_renderer;echo 'READY:desktop';exit
fi
arch_root_permissions check || { echo ERROR:ARCH_ROOTFS_OWNER_ACCESS;exit 1; }
gpu_mode=${JOBS_X11_RENDERER:-cpu}
jobs_desktop_run() { "$@"; }
case $gpu_mode in cpu|turnip|virgl|wrapper|bionic-glibc) ;; *) echo ERROR:INVALID_RENDERER;exit 2;;esac
if [[ $gpu_mode == bionic-glibc ]];then
    [[ -f $jobs_bionic_root/operation.lock && ! -L $jobs_bionic_root/operation.lock ]] || { echo ERROR:BIONIC_LOCK;exit 1; }
    exec {jobs_bionic_lock}<"$jobs_bionic_root/operation.lock"
    flock --shared --nonblock "$jobs_bionic_lock" || { echo ERROR:BIONIC_BUSY;exit 1; }
    "$PREFIX/bin/python3" -B "$control/x11-bionic-glibc.py" check "$target" "$jobs_bionic_root" - 0 - || exit
    gpu_bind=(--bind "$jobs_bionic_root:/tmp/termux-jobs-bionic-glibc")
    for jobs_android_path in /system /vendor /system_ext /apex /odm;do
        if [[ -d $jobs_android_path ]];then gpu_bind+=(--bind "$jobs_android_path:$jobs_android_path");fi
    done
fi
if [[ $gpu_mode == turnip ]];then
    case $target in termux|ubuntu|debian|alpine|fedora|opensuse|void|archlinux) ;; *) echo ERROR:GPU_TARGET_UNSUPPORTED;exit 2;;esac
    gpu_root="${PREFIX%/usr}/workspace-x11-gpu"
    "$PREFIX/bin/python3" -B "$control/x11-gpu.py" check "$target" "$gpu_root" || exit
    if [[ $target != termux ]];then gpu_bind=(--bind "$gpu_root:/tmp/termux-jobs-x11-gpu");fi
fi
if [[ $gpu_mode == virgl ]];then
    bash "$control/x11-virgl-setup.sh" check "$target" "$control" || exit
    source "$control/x11-virgl.sh"
    trap jobs_virgl_cleanup EXIT
    jobs_virgl_client=none
    if [[ $target == alpine ]];then
        jobs_virgl_client=$("$PREFIX/bin/python3" -B "$control/x11-virgl-client.py" check "$control/x11-virgl-alpine.zip") || { echo "$jobs_virgl_client";exit 1; }
        jobs_virgl_client=${jobs_virgl_client#JOBS_VIRGL_CLIENT=}
        [[ $jobs_virgl_client =~ ^client-[a-f0-9]{16}$ ]] || { echo ERROR:VIRGL_CLIENT_ID;exit 1; }
        gpu_bind+=(--bind "${PREFIX%/usr}/workspace-x11-virgl-alpine:/tmp/termux-jobs-virgl");fi
    jobs_virgl_start || exit
fi
unset SESSION_MANAGER DBUS_SESSION_BUS_ADDRESS
case $target in termux) desktop_display=97;;ubuntu) desktop_display=98;;jobs-x11-ubuntu) desktop_display=99;;debian) desktop_display=100;;alpine) desktop_display=101;;fedora) desktop_display=102;;opensuse) desktop_display=103;;void) desktop_display=104;;archlinux) desktop_display=105;;esac
export DISPLAY=:$desktop_display
export XAUTHORITY="$control/authority"
export LIBGL_ALWAYS_SOFTWARE=1
if [[ $target == termux ]];then
    if [[ $gpu_mode == turnip ]];then source "$control/x11-gpu-env.sh";jobs_gpu_environment termux "$gpu_root" || exit
    elif [[ $gpu_mode == wrapper ]];then
        wrapper_root="${PREFIX%/usr}/workspace-x11-wrapper"
        [[ -f $wrapper_root/operation.lock && ! -L $wrapper_root/operation.lock ]] || { echo ERROR:WRAPPER_LOCK;exit 1; }
        exec {jobs_wrapper_lock}<"$wrapper_root/operation.lock"
        flock --shared --nonblock "$jobs_wrapper_lock" || { echo ERROR:WRAPPER_BUSY;exit 1; }
        wrapper_exports=$("$PREFIX/bin/python3" -B "$control/x11-wrapper.py" environment termux "$wrapper_root" - 0 -) || { printf '%s\n' "$wrapper_exports";exit 1; }
        eval "$wrapper_exports"
        unset LIBGL_ALWAYS_SOFTWARE LIBGL_DRIVERS_PATH __EGL_VENDOR_LIBRARY_FILENAMES LIBGL_KOPPER_DISABLE VTEST_SOCKET_NAME VTEST_RENDERER_SOCKET_NAME
        export LD_LIBRARY_PATH="$wrapper_root/runtime:$ADRENOTOOLS_DRIVER_PATH${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
        export MESA_LOADER_DRIVER_OVERRIDE=zink GALLIUM_DRIVER=zink WRAPPER_LOG_LEVEL=none
        echo JOBS_GPU_RENDERER=wrapper-zink
    elif [[ $gpu_mode == virgl ]];then
        source "$control/x11-virgl-env.sh"
        jobs_virgl_environment termux "$jobs_virgl_socket" none || exit
    fi
    jobs_audio_start
    export PULSE_SERVER="unix:${jobs_audio_dir:-$control/audio-unavailable}/native" PULSE_COOKIE="${jobs_audio_dir:-$control/audio-unavailable}/cookie"
    export JOBS_X11_CONTROL="$control" JOBS_X11_TARGET=termux
    export XDG_RUNTIME_DIR="$control/session-runtime";mkdir -p "$XDG_RUNTIME_DIR";chmod 700 "$XDG_RUNTIME_DIR"
    jobs_desktop_run dbus-run-session -- sh "$control/x11-session.sh"
else
    token=${JOBS_X11_FOCUS_TOKEN:-}
    [[ $token =~ ^[a-f0-9-]{36}$ ]] || { echo 'ERROR:FOCUS_TOKEN';exit 2; }
    jobs_audio_start
    alpine_compat=0;if [[ $target == alpine ]];then alpine_compat=1;fi
    jobs_virgl_name=none;if [[ $gpu_mode == virgl ]];then jobs_virgl_name=${jobs_virgl_dir##*/};fi
    jobs_app_uid=$(id -u)
    [[ $jobs_app_uid =~ ^[0-9]{1,10}$ ]] || { echo ERROR:APP_UID;exit 1; }
    jobs_desktop_run guest "export JOBS_X11_APP_UID=$jobs_app_uid; export JOBS_X11_DISPLAY=$desktop_display JOBS_X11_VIRGL_NAME=$jobs_virgl_name JOBS_X11_VIRGL_CLIENT=${jobs_virgl_client:-none} JOBS_X11_ALPINE_COMPAT=$alpine_compat JOBS_X11_RENDERER=$gpu_mode JOBS_X11_TARGET=$target JOBS_X11_STYLE=${JOBS_X11_STYLE:-0} JOBS_X11_SCALE=${JOBS_X11_SCALE:-100}; export PULSE_SERVER=unix:$jobs_audio_guest/native PULSE_COOKIE=$jobs_audio_guest/cookie; export JOBS_X11_FOCUS_TOKEN=$token JOBS_X11_DESKTOP=$desktop; "'unset SESSION_MANAGER DBUS_SESSION_BUS_ADDRESS; export DISPLAY=:$JOBS_X11_DISPLAY XAUTHORITY=/tmp/termux-jobs-x11/authority; if [ "$JOBS_X11_RENDERER" = turnip ];then . /tmp/termux-jobs-x11/x11-gpu-env.sh; jobs_gpu_environment "$JOBS_X11_TARGET" /tmp/termux-jobs-x11-gpu || exit;elif [ "$JOBS_X11_RENDERER" = bionic-glibc ];then . /tmp/termux-jobs-x11/x11-bionic-glibc-env.sh;jobs_bionic_glibc_environment || exit;elif [ "$JOBS_X11_RENDERER" = virgl ];then . /tmp/termux-jobs-x11/x11-virgl-env.sh;jobs_virgl_environment "$JOBS_X11_TARGET" "/tmp/termux-jobs-x11/$JOBS_X11_VIRGL_NAME/sock" "$JOBS_X11_VIRGL_CLIENT" || exit;else export LIBGL_ALWAYS_SOFTWARE=1;fi; export XDG_RUNTIME_DIR=/tmp/jobs-x11-runtime-$JOBS_X11_DISPLAY; mkdir -p "$XDG_RUNTIME_DIR"; chmod 700 "$XDG_RUNTIME_DIR"; if [ "$JOBS_X11_ALPINE_COMPAT" = 1 ];then test -f /tmp/termux-jobs-x11/x11-alpine-compat.so || { echo ERROR:ALPINE_COMPAT_MISSING;exit 1; }; export LD_PRELOAD="/tmp/termux-jobs-x11/x11-alpine-compat.so${LD_PRELOAD:+:$LD_PRELOAD}";fi; if [ "$JOBS_X11_TARGET" = fedora ] || [ "$JOBS_X11_TARGET" = archlinux ];then test -f /tmp/termux-jobs-x11/x11-fedora-glycin.so || { echo ERROR:FEDORA_GLYCIN_MISSING;exit 1; }; export LD_PRELOAD="/tmp/termux-jobs-x11/x11-fedora-glycin.so${LD_PRELOAD:+:$LD_PRELOAD}";fi; jobs_bus_config=$(python3 -B /tmp/termux-jobs-x11/x11-session-bus.py) || exit; exec dbus-run-session --config-file="$jobs_bus_config" -- /bin/sh /tmp/termux-jobs-x11/x11-session.sh';fi
