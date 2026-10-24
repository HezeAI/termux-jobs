# Sourced only inside the selected glibc desktop's process tree.
jobs_bionic_glibc_environment() {
    case ${JOBS_X11_TARGET:-} in ubuntu|debian|fedora|archlinux|opensuse|void) ;; *) echo ERROR:BIONIC_TARGET_UNSUPPORTED;return 1;;esac
    jobs_bionic_base=/tmp/termux-jobs-bionic-glibc
    jobs_bionic_exports=$(python3 -B /tmp/termux-jobs-x11/x11-bionic-glibc.py environment "$JOBS_X11_TARGET" "$jobs_bionic_base" - 0 -) || { printf '%s\n' "$jobs_bionic_exports";return 1; }
    eval "$jobs_bionic_exports"
    # Guest dependencies remain installed normally; private Mesa is process-local.
    python3 - <<'PY' || return 1
import ctypes, platform
try:
    version = platform.libc_ver()
    if version[0] != 'glibc' or tuple(map(int,version[1].split('.')[:2])) < (2,38):
        raise RuntimeError('BIONIC_REQUIRES_GLIBC_2_38')
    for name in ('libvulkan.so.1','libGLX_mesa.so.0','libEGL_mesa.so.0','libX11-xcb.so.1','libxcb-present.so.0','libxcb-dri3.so.0'):
        ctypes.CDLL(name)
except (OSError,ValueError,RuntimeError) as error:
    print('ERROR:BIONIC_GUEST_LIBRARIES: '+str(error),flush=True)
    raise SystemExit(1)
PY
    unset LIBGL_ALWAYS_SOFTWARE LIBGL_DRIVERS_PATH __EGL_VENDOR_LIBRARY_FILENAMES LIBGL_KOPPER_DISABLE VK_INSTANCE_LAYERS VK_LAYER_PATH MESA_VK_WSI_DEBUG VTEST_SOCKET_NAME VTEST_RENDERER_SOCKET_NAME
    export JOBS_BIONIC_ENABLED=1
    export HYBRIS_ANDROID_SDK_VERSION="$(/system/bin/getprop ro.build.version.sdk)"
    case $HYBRIS_ANDROID_SDK_VERSION in ''|*[!0-9]*) echo ERROR:BIONIC_ANDROID_API;return 1;;esac
    export HYBRIS_LINKER_DIR="$jobs_bionic_base/runtime/linker"
    export HYBRIS_LD_LIBRARY_PATH="${JOBS_BIONIC_DRIVER%/*}:/system/lib64:/vendor/lib64:/vendor/lib64/hw:/vendor/lib64/egl:/system_ext/lib64:/apex/com.android.runtime/lib64/bionic"
    export LD_LIBRARY_PATH="$jobs_bionic_base/runtime/mesa:$jobs_bionic_base/runtime${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
    export GLIBC_TUNABLES=glibc.rtld.optional_static_tls=65536
    export VK_DRIVER_FILES="$jobs_bionic_base/runtime/bionic-icd.json" VK_ICD_FILENAMES="$jobs_bionic_base/runtime/bionic-icd.json"
    export XDG_DATA_DIRS="$jobs_bionic_base/runtime/share:${XDG_DATA_DIRS:-/usr/local/share:/usr/share}"
    export GALLIUM_DRIVER=zink MESA_LOADER_DRIVER_OVERRIDE=zink LIBGL_KOPPER_DRI2=true __GLX_VENDOR_LIBRARY_NAME=mesa
    echo JOBS_GPU_RENDERER=bionic-glibc-zink
}
