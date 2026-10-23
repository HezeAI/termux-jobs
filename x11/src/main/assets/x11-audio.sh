#!/usr/bin/env bash
# Sourced only by the private, guardian-owned desktop child. No global daemon.
jobs_audio_available() {
    [[ -x "$PREFIX/bin/pulseaudio" ]] || return 1
    local modules
    for modules in "$PREFIX/lib/pulseaudio/modules" "$PREFIX"/lib/pulse-*/modules;do
        [[ -r "$modules/module-sles-sink.so" && -r "$modules/module-native-protocol-unix.so" ]] && return 0
    done
    return 1
}
jobs_audio_cleanup() {
    # Only our freshly allocated private runtime, never a user config or PID file.
    # rm does not follow symbolic links in this tree (no trailing slash).
    if [[ ${jobs_audio_dir:-} == "$control"/pulse.* ]];then rm -rf -- "$jobs_audio_dir";fi
}
jobs_audio_start() {
    jobs_audio_guest=/tmp/termux-jobs-x11/audio-unavailable
    if ! jobs_audio_available;then echo JOBS_X11_AUDIO=missing-components;return;fi
    local old_umask
    old_umask=$(umask);umask 077
    jobs_audio_dir=$(mktemp -d "$control/pulse.XXXXXXXX") || { umask "$old_umask";echo JOBS_X11_AUDIO=runtime-failed;return; }
    trap jobs_audio_cleanup EXIT
    trap 'exit 143' TERM HUP INT
    mkdir "$jobs_audio_dir/run" "$jobs_audio_dir/state"
    if ! head -c 256 /dev/urandom > "$jobs_audio_dir/cookie";then umask "$old_umask";echo JOBS_X11_AUDIO=cookie-failed;return;fi
    umask "$old_umask"
    # Foreground mode keeps PulseAudio inside the existing owned process group.
    # No default scripts, TCP listener, microphone source, PID adoption or --kill.
    PULSE_RUNTIME_PATH="$jobs_audio_dir/run" PULSE_STATE_PATH="$jobs_audio_dir/state" \
    "$PREFIX/bin/pulseaudio" -n --daemonize=no --system=no --use-pid-file=no \
        --exit-idle-time=-1 --high-priority=no --realtime=no \
        --disable-shm=yes --enable-memfd=no --log-target=stderr --log-level=warning \
        --disallow-module-loading=yes --disallow-exit=yes \
        --load=module-sles-sink \
        --load="module-native-protocol-unix socket=$jobs_audio_dir/native auth-cookie=$jobs_audio_dir/cookie auth-cookie-enabled=yes auth-anonymous=no" &
    jobs_audio_pid=$!
    for ((jobs_audio_attempt=0;jobs_audio_attempt<100;jobs_audio_attempt++));do
        if [[ -S "$jobs_audio_dir/native" ]];then
            jobs_audio_guest="/tmp/termux-jobs-x11/${jobs_audio_dir##*/}"
            echo JOBS_X11_AUDIO=ready;return
        fi
        kill -0 "$jobs_audio_pid" 2>/dev/null || break
        sleep 0.05
    done
    echo JOBS_X11_AUDIO=start-failed
}
