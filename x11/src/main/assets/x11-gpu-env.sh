# Sourced only into the graphical session. No global shell configuration is changed.
jobs_gpu_environment() {
    case "$1" in
      termux) jobs_gpu_component="$2/termux-mesa26.2.5";jobs_gpu_lib="$jobs_gpu_component/data/data/com.termux/files/usr/lib";;
      ubuntu) jobs_gpu_component="$2/ubuntu-26.04-mesa26.2.5";jobs_gpu_lib="$jobs_gpu_component/usr/lib/aarch64-linux-gnu";;
      debian) jobs_gpu_component="$2/debian-13-mesa26.2.5";jobs_gpu_lib="$jobs_gpu_component/usr/lib/aarch64-linux-gnu";;
      alpine) jobs_gpu_component="$2/alpine-3.24-mesa26.2.5";jobs_gpu_lib="$jobs_gpu_component/usr/lib";;
      fedora) jobs_gpu_component="$2/fedora-44-mesa26.2.5";jobs_gpu_lib="$jobs_gpu_component/usr/lib64";;
      opensuse) jobs_gpu_component="$2/opensuse-16-mesa26.2.5";jobs_gpu_lib="$jobs_gpu_component/usr/lib/aarch64-linux-gnu";;
      void) jobs_gpu_component="$2/void-mesa26.2.5";jobs_gpu_lib="$jobs_gpu_component/usr/lib";;
      archlinux) jobs_gpu_component="$2/archlinux-mesa26.2.5";jobs_gpu_lib="$jobs_gpu_component/usr/lib";;
      *) echo ERROR:GPU_TARGET_UNSUPPORTED;return 2;;
    esac
    test -f "$jobs_gpu_component/ready.json" && test -f "$jobs_gpu_lib/libvulkan_freedreno.so" || { echo ERROR:GPU_COMPONENTS_MISSING;return 1; }
    unset LIBGL_ALWAYS_SOFTWARE LIBGL_KOPPER_DISABLE VTEST_SOCKET_NAME VTEST_RENDERER_SOCKET_NAME
    export LD_LIBRARY_PATH="$jobs_gpu_lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
    export VK_DRIVER_FILES="$jobs_gpu_component/turnip-icd.json" VK_ICD_FILENAMES="$jobs_gpu_component/turnip-icd.json"
    export MESA_LOADER_DRIVER_OVERRIDE=zink GALLIUM_DRIVER=zink
    if [ "$1" != termux ];then
        export LIBGL_DRIVERS_PATH="$jobs_gpu_lib/dri"
        if [ "$1" != alpine ];then export __EGL_VENDOR_LIBRARY_FILENAMES="$jobs_gpu_component/usr/share/glvnd/egl_vendor.d/50_mesa.json";fi
    fi
    echo JOBS_GPU_RENDERER=turnip-zink
}
