# Sourced only inside the owned desktop process group. Native Android server,
# separate short socket per desktop; never attach to an existing global server.
jobs_virgl_start() {
    [[ ${JOBS_X11_RENDERER:-cpu} == virgl ]] || return 2
    case $target in termux|ubuntu|debian|alpine|fedora|opensuse|void|archlinux) ;; *) return 2;;esac
    [[ -x $PREFIX/bin/virgl_test_server_android ]] || { echo ERROR:VIRGL_COMPONENTS_REQUIRED;return 1; }
    jobs_virgl_dir=$(mktemp -d "$control/v.XXXXXX") || return 1
    chmod 700 "$jobs_virgl_dir" || return 1
    jobs_virgl_socket="$jobs_virgl_dir/sock"
    [[ ${#jobs_virgl_socket} -lt 108 ]] || { echo ERROR:VIRGL_SOCKET_PATH;return 1; }
    local jobs_backend=gl
    case $target in void|fedora|debian|archlinux|termux) jobs_backend=vulkan;;esac
    local jobs_server_flags=(--no-fork --multi-clients)
    # These guests use upstream per-connection fork mode. It supports older
    # vtest protocols and avoids sharing ANGLE contexts between applications.
    # Children retain this guardian's group: no daemon, setsid or PID adoption.
    case $target in debian|alpine|fedora|opensuse|void|archlinux|termux) jobs_server_flags=();;esac
    # A subshell drops guest driver overrides before loading native Android EGL.
    (
        unset LIBGL_ALWAYS_SOFTWARE LIBGL_KOPPER_DISABLE GALLIUM_DRIVER MESA_LOADER_DRIVER_OVERRIDE LIBGL_DRIVERS_PATH
        unset VK_DRIVER_FILES VK_ICD_FILENAMES __EGL_VENDOR_LIBRARY_FILENAMES LD_PRELOAD LD_LIBRARY_PATH
        unset VTEST_SOCKET_NAME VTEST_RENDERER_SOCKET_NAME VTEST_USE_GLES VTEST_USE_COMPATIBILITY_PROFILE VTEST_RENDERNODE
        exec "$PREFIX/bin/virgl_test_server_android" "${jobs_server_flags[@]}" "--angle-$jobs_backend" --socket-path "$jobs_virgl_socket"
    ) &
    jobs_virgl_pid=$!
    for ((jobs_virgl_i=0;jobs_virgl_i<100;jobs_virgl_i++));do
        if [[ -S $jobs_virgl_socket ]];then echo JOBS_GPU_RENDERER=virgl-angle;echo "JOBS_GPU_BACKEND=$jobs_backend";return 0;fi
        kill -0 "$jobs_virgl_pid" 2>/dev/null || { echo ERROR:VIRGL_SERVER_EXITED;return 1; }
        sleep .05
    done
    echo ERROR:VIRGL_SERVER_TIMEOUT;return 1
}
jobs_virgl_cleanup() {
    # The guardian owns shutdown. Do not signal a PID that could be reused.
    # Only unlink paths in the unique directory created by this launch.
    if [[ -n ${jobs_virgl_dir:-} ]];then rm -f -- "$jobs_virgl_dir/sock";rmdir -- "$jobs_virgl_dir" 2>/dev/null || true;fi
}
jobs_desktop_run() {
    if [[ ${JOBS_X11_RENDERER:-cpu} != virgl ]];then "$@";return;fi
    "$@" &
    local jobs_session_pid=$! jobs_finished= jobs_status=0
    # Both children belong to this shell. Never wait on/adopt another desktop.
    wait -n -p jobs_finished "$jobs_virgl_pid" "$jobs_session_pid" || jobs_status=$?
    if [[ $jobs_finished == "$jobs_virgl_pid" ]];then
        echo ERROR:VIRGL_SERVER_EXITED;return 1
    fi
    return "$jobs_status"
}
