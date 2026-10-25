#!/bin/sh
# Inside this Arch's fresh private D-Bus session. No global login/session changes.
set -eu
[ "${JOBS_X11_TARGET:-}:${JOBS_X11_DESKTOP:-}" = archlinux:plasma ] || exit 2
export XDG_CONFIG_HOME="$HOME/.config/termux-jobs-plasma" XDG_CACHE_HOME="$HOME/.cache/termux-jobs-plasma" XDG_STATE_HOME="$HOME/.local/state/termux-jobs-plasma"
python3 -B /tmp/termux-jobs-x11/wayland-ready.py prepare
export XDG_SESSION_TYPE=wayland XDG_CURRENT_DESKTOP=KDE XDG_SESSION_DESKTOP=KDE KDE_FULL_SESSION=true KDE_SESSION_VERSION=6 KDE_APPLICATIONS_AS_SCOPE=0
export QT_FORCE_STDERR_LOGGING=1 QT_ASSUME_STDERR_HAS_CONSOLE=1
export QT_QPA_PLATFORM='wayland;xcb' GDK_BACKEND=wayland,x11 SDL_VIDEODRIVER=wayland,x11
export WAYLAND_DISPLAY=wayland-0 KWIN_RENDER_NODES=/dev/dri/renderD0 KWIN_COMPOSE=O2
export GTK_IM_MODULE=fcitx QT_IM_MODULE=fcitx XMODIFIERS=@im=fcitx
. /tmp/termux-jobs-x11/x11-gpu-env.sh
jobs_gpu_environment archlinux /tmp/termux-jobs-x11-gpu
export GBM_BACKENDS_PATH="$jobs_gpu_lib/gbm" GBM_BACKEND=dri
export LD_PRELOAD=/tmp/termux-jobs-x11/wayland-guest.so
export PATH="$XDG_RUNTIME_DIR/launch:$PATH"
unset DISPLAY SESSION_MANAGER QT_QUICK_BACKEND LIBGL_ALWAYS_SOFTWARE
(
 dbus-monitor --session --profile "type='method_call',interface='org.fcitx.Fcitx.InputContext1',member='FocusIn'" "type='method_call',interface='org.fcitx.Fcitx.InputContext1',member='FocusOut'" "type='method_call',interface='org.fcitx.Fcitx.InputContext1',member='DestroyIC'" |
 while read -r kind time serial sender destination path interface member rest;do
  [ "$kind" = mc ] && [ "$interface" = org.fcitx.Fcitx.InputContext1 ] || continue
  case $member in FocusIn|FocusOut|DestroyIC) printf '\nJOBS_X11_FOCUS_V1 %s %s %s %s\n' "$JOBS_X11_FOCUS_TOKEN" "$member" "$sender" "$path";;esac
 done
) &
fcitx5 &
python3 -u /tmp/termux-jobs-x11/wayland-ready.py watch &
echo JOBS_X11_STARTUP=plasma-session
export QT_LOGGING_RULES="org.kde.startup.debug=true;org.kde.plasma.session.debug=true;kwin_wayland_backend.debug=true;kwin_scene_opengl.debug=true"
exec startplasma-wayland
