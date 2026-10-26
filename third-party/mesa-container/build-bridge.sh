#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p .verification/mesa-26.2.5 .tools/tmp
export PYTHONPATH=$PWD/.tools/virgl-fix/python TMPDIR=$PWD/.tools/tmp XDG_CACHE_HOME=$PWD/.tools/mesa-26.2.5/cache
export PATH=$PWD/.tools/universal-mesa/sysroot/usr/bin:$PWD/.tools/virgl-fix/ninja-wheel/bin:$PATH
export PKG_CONFIG_LIBDIR=$PWD/.tools/universal-mesa/sysroot/usr/lib/aarch64-linux-gnu/pkgconfig:$PWD/.tools/universal-mesa/sysroot/usr/share/pkgconfig
export PKG_CONFIG_SYSROOT_DIR=$PWD/.tools/universal-mesa/sysroot
FORK_MESA_LINK_ARGS="['-Wl,--wrap=atan2f,--wrap=asinf,--wrap=acosf', '$PWD/.tools/mesa-26.2.5/mesa-libm-compat.o', '-lm', '-L$PWD/.tools/universal-mesa/sysroot/usr/lib/aarch64-linux-gnu', '-Wl,-rpath-link,$PWD/.tools/universal-mesa/sysroot/usr/lib/aarch64-linux-gnu']"
FORK_MESA_CONFIGURE=()
if [[ -f .tools/mesa-26.2.5/build-bridge/meson-private/coredata.dat ]];then FORK_MESA_CONFIGURE=(--reconfigure);fi
python3 -m mesonbuild.mesonmain setup "${FORK_MESA_CONFIGURE[@]}" .tools/mesa-26.2.5/build-bridge .tools/mesa-26.2.5/mesa-26.2.5 --native-file .tools/mesa-26.2.5/glibc-native.ini -Dc_link_args="$FORK_MESA_LINK_ARGS" -Dcpp_link_args="$FORK_MESA_LINK_ARGS" --prefix=/opt/termux-jobs-bionic-mesa --libdir=lib --buildtype=release --wrap-mode=nodownload -Db_ndebug=true -Dgallium-drivers=zink -Dvulkan-drivers=[] -Dplatforms=x11 -Dllvm=disabled -Dglvnd=enabled -Dgbm=disabled -Degl=enabled -Dglx=dri -Dgles1=enabled -Dgles2=enabled -Dgallium-va=disabled -Dgallium-rusticl=false -Dvideo-codecs=[] -Dxmlconfig=disabled -Dshader-cache=enabled -Dzstd=disabled -Dbuild-tests=false > .verification/mesa-26.2.5/bridge-configure.log 2>&1
ninja -C .tools/mesa-26.2.5/build-bridge -j3 > .verification/mesa-26.2.5/bridge-build.log 2>&1
python3 -m mesonbuild.mesonmain install -C .tools/mesa-26.2.5/build-bridge --no-rebuild --destdir "$PWD/.tools/mesa-26.2.5/stage-bridge" > .verification/mesa-26.2.5/bridge-install.log 2>&1
