#!/bin/sh
# Runs INSIDE the desktop's new dbus-run-session, not the user's existing bus.
set -u
case ${JOBS_X11_FOCUS_TOKEN:-} in ''|*[!a-f0-9-]*) echo 'ERROR:FOCUS_TOKEN';exit 2;;esac
case ${JOBS_X11_DESKTOP:-xfce} in xfce) ;; *) echo ERROR:DESKTOP_CHOICE;exit 2;;esac
jobs_control=${JOBS_X11_CONTROL:-/tmp/termux-jobs-x11}
export JOBS_X11_CONTROL="$jobs_control"
case ${JOBS_X11_TARGET:-} in
 termux|ubuntu|debian|alpine|fedora|opensuse|void|archlinux)
    case ${JOBS_X11_SCALE:-100} in [5-9][0-9]|1[0-4][0-9]|150) ;; *) echo ERROR:X11_SCALE;exit 2;;esac
    if [ -r "$jobs_control/x11-scale.py" ];then python3 -B "$jobs_control/x11-scale.py" restore || exit 1;elif [ "${JOBS_X11_SCALE:-100}" != 100 ];then echo ERROR:X11_SCALE_MISSING;exit 1;fi
    if [ -r "$jobs_control/x11-style.py" ];then
        python3 -B "$jobs_control/x11-style.py" "${JOBS_X11_STYLE:-0}" || exit 1
    elif [ "${JOBS_X11_STYLE:-0}" != 0 ];then echo ERROR:STYLE_MISSING;exit 1;fi
    if [ -r "$jobs_control/x11-scale.py" ];then python3 -B "$jobs_control/x11-scale.py" apply "${JOBS_X11_SCALE:-100}" || exit 1;fi
    if [ "${JOBS_X11_STYLE:-0}" = 1 ];then export GTK_THEME=TermuxJobsStyle XCURSOR_THEME=TermuxJobsCursor XCURSOR_SIZE=$(((32*${JOBS_X11_SCALE:-100}+50)/100));fi
 ;;
 *) [ "${JOBS_X11_STYLE:-0}" = 0 ] || { echo ERROR:STYLE_TARGET;exit 1; };;
esac
# Save only this new bus endpoint for one-shot live settings; never export auth contents.
if [ -r "$jobs_control/x11-scale-live.py" ];then python3 -B "$jobs_control/x11-scale-live.py" register || echo JOBS_X11_SCALE_LIVE=unavailable;fi
# Applied only to Alpine's owned desktop tree. No host/system library replacement.
if [ "${JOBS_X11_ALPINE_COMPAT:-0}" = 1 ];then
    if [ ! -f "$jobs_control/x11-alpine-compat.so" ];then echo ERROR:ALPINE_COMPAT_MISSING;exit 1;fi
    case :${LD_PRELOAD:-}: in
      *":$jobs_control/x11-alpine-compat.so:"*) ;;
      *) export LD_PRELOAD="$jobs_control/x11-alpine-compat.so${LD_PRELOAD:+:$LD_PRELOAD}";;
    esac
    echo JOBS_X11_ALPINE_COMPAT=filesystem-and-image-decoder
fi
# Same Glycin 2 ABI on these glibc guests; preload stays in this desktop tree.
if [ "${JOBS_X11_TARGET:-}" = fedora ] || [ "${JOBS_X11_TARGET:-}" = archlinux ];then
    [ -f "$jobs_control/x11-fedora-glycin.so" ] || { echo ERROR:FEDORA_GLYCIN_MISSING;exit 1; }
    case :${LD_PRELOAD:-}: in
      *":$jobs_control/x11-fedora-glycin.so:"*) ;;
      *) export LD_PRELOAD="$jobs_control/x11-fedora-glycin.so${LD_PRELOAD:+:$LD_PRELOAD}";;
    esac
    if [ "${JOBS_X11_TARGET:-}" = archlinux ];then echo JOBS_X11_ARCH_GLYCIN=proot-image-decoder;else echo JOBS_X11_FEDORA_GLYCIN=proot-image-decoder;fi
fi
# Fixed shortcuts avoid browsing Android root, whose directory listing may be denied.
# Runs once in this selected environment; no storage scan or permission changes.
if [ -r "$jobs_control/x11-bookmarks.py" ];then
    python3 -B "$jobs_control/x11-bookmarks.py" || echo JOBS_X11_BOOKMARKS=unavailable
fi
if [ -r "$jobs_control/x11-gpu-tools.py" ];then
    python3 -B "$jobs_control/x11-gpu-tools.py" prepare "${JOBS_X11_TARGET:-termux}" || echo JOBS_X11_GPU_SHORTCUTS=preserved-unavailable
fi
export GTK_IM_MODULE=fcitx QT_IM_MODULE=fcitx XMODIFIERS=@im=fcitx
# This session's bounded metadata gate rejects desktop/dock/hidden windows and
# disabled input contexts. Manual Android keyboard input remains independent.
if [ -r "$jobs_control/x11-focus.py" ];then
    python3 -B -u "$jobs_control/x11-focus.py" &
fi
# The guardian owns this whole desktop process group, including these helpers.
fcitx5 &
# Trial overlay disabled after developer device freezes. System Garcon only.
unset JOBS_X11_GARCON_FILTER
# XFCE-only wallpaper repair and menu diagnostics.
if [ -r "$jobs_control/x11-wallpaper.py" ];then
    python3 -u "$jobs_control/x11-wallpaper.py" --quick-start
fi
# Let xfce4-session start and register its own manager. Starting xfwm4 here first
# makes XFCE's duplicate exit and can leave its failsafe registration counter pending.
# Keep renderer-specific compositing through a private executable shim, not xfconf.
jobs_compositor=on
case "${JOBS_X11_TARGET:-}:${JOBS_X11_RENDERER:-cpu}" in ubuntu:bionic-glibc|debian:bionic-glibc|fedora:bionic-glibc|archlinux:bionic-glibc|opensuse:bionic-glibc|void:bionic-glibc|termux:wrapper|*:virgl|ubuntu:turnip|debian:turnip|alpine:turnip|fedora:turnip|opensuse:turnip|void:turnip|archlinux:turnip) jobs_compositor=off;;esac
export JOBS_X11_COMPOSITOR="$jobs_compositor"
JOBS_X11_WM_BINARY=$(command -v xfwm4) || { echo ERROR:WINDOW_MANAGER_MISSING;exit 1; }
export JOBS_X11_WM_BINARY
jobs_shell=$(command -v sh) || exit 1
case "$JOBS_X11_WM_BINARY:$jobs_shell" in /*:/*) ;;*) echo ERROR:WINDOW_MANAGER_PATH;exit 1;;esac
jobs_runtime=${XDG_RUNTIME_DIR:-}
[ -n "$jobs_runtime" ] && [ -d "$jobs_runtime" ] && [ ! -L "$jobs_runtime" ] || { echo ERROR:SESSION_RUNTIME;exit 1; }
jobs_launch="$jobs_runtime/termux-jobs-xfce-launch"
[ ! -L "$jobs_launch" ] || { echo ERROR:SESSION_LAUNCH_LINK;exit 1; }
mkdir -p "$jobs_launch" && chmod 700 "$jobs_launch" || exit 1
if [ "${JOBS_X11_TARGET:-}" = fedora ];then
    # Session-only XDG override. Keep the user's ~/.config and system .desktop
    # untouched; user overrides retain their normal higher priority.
    jobs_xdg="$jobs_runtime/termux-jobs-xdg"
    [ ! -L "$jobs_xdg" ] && [ ! -L "$jobs_xdg/autostart" ] || { echo ERROR:AUTOSTART_LINK;exit 1; }
    mkdir -p "$jobs_xdg/autostart" && chmod 700 "$jobs_xdg" "$jobs_xdg/autostart" || exit 1
    [ ! -L "$jobs_xdg/autostart/xfce-polkit.desktop" ] || { echo ERROR:AUTOSTART_LINK;exit 1; }
    jobs_autostart=$(mktemp "$jobs_xdg/autostart/.polkit.XXXXXX") || exit 1
    printf '[Desktop Entry]\nType=Application\nName=XFCE PolKit\nHidden=true\n' > "$jobs_autostart"
    mv -f "$jobs_autostart" "$jobs_xdg/autostart/xfce-polkit.desktop" || { rm -f "$jobs_autostart";exit 1; }
    export XDG_CONFIG_DIRS="$jobs_xdg:${XDG_CONFIG_DIRS:-/etc/xdg}"
fi
[ ! -L "$jobs_launch/xfwm4" ] || { echo ERROR:WINDOW_MANAGER_LINK;exit 1; }
jobs_wrapper=$(mktemp "$jobs_launch/.xfwm4.XXXXXX") || exit 1
{
 printf '#!%s\n' "$jobs_shell"
 cat <<'WM'
# Private current-desktop wrapper; no commands or flags arrive from UI text.
echo "JOBS_X11_COMPOSITOR=$JOBS_X11_COMPOSITOR"
echo JOBS_X11_STARTUP=managed-window-manager
exec "$JOBS_X11_WM_BINARY" "$@" --compositor="$JOBS_X11_COMPOSITOR"
WM
} > "$jobs_wrapper"
chmod 700 "$jobs_wrapper" && mv -f "$jobs_wrapper" "$jobs_launch/xfwm4" || { rm -f "$jobs_wrapper";exit 1; }
export PATH="$jobs_launch:$PATH"
# Startup-only property probe. It exits on readiness or after a bounded grace;
# timeout reveals manual access but later readiness can still show automatically.
python3 -u "$jobs_control/x11-ready.py" &
echo JOBS_X11_STARTUP=xfce-session
xfce4-session
exit $?
