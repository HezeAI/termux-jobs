# POSIX shell, shared by the native and selected guest VirGL sessions only.
# GLX may otherwise try Kopper/Zink before Gallium honors GALLIUM_DRIVER.
jobs_virgl_environment() {
    case $1 in termux|ubuntu|debian|alpine|fedora|opensuse|void|archlinux) ;; *) return 2;;esac
    if [ "$1" = alpine ];then
        case ${3:-} in client-????????????????) ;; *) echo ERROR:VIRGL_CLIENT_ID;return 1;;esac
        case ${3#client-} in *[!a-f0-9]*) echo ERROR:VIRGL_CLIENT_ID;return 1;;esac
        [ -d "/tmp/termux-jobs-virgl/$3/lib" ] || { echo ERROR:VIRGL_CLIENT_REQUIRED;return 1; }
    fi
    unset MESA_LOADER_DRIVER_OVERRIDE LIBGL_DRIVERS_PATH LD_LIBRARY_PATH
    unset VK_DRIVER_FILES VK_ICD_FILENAMES __EGL_VENDOR_LIBRARY_FILENAMES VTEST_RENDERER_SOCKET_NAME
    # "software" selects Mesa's swrast loader, whose Gallium driver is virpipe.
    # Rendering stays on the remote Android GPU, not llvmpipe/softpipe.
    export LIBGL_ALWAYS_SOFTWARE=true GALLIUM_DRIVER=virpipe VTEST_SOCKET_NAME="$2" LIBGL_KOPPER_DISABLE=true
    if [ "$1" = alpine ];then
        export LD_LIBRARY_PATH="/tmp/termux-jobs-virgl/$3/lib"
        export LIBGL_DRIVERS_PATH="/tmp/termux-jobs-virgl/$3/lib/dri"
    fi
    echo JOBS_GPU_GLX=drisw-virpipe
}
