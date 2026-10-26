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
if [[ -f .tools/mesa-26.2.5/build-glibc/meson-private/coredata.dat ]];then FORK_MESA_CONFIGURE=(--reconfigure);fi
python3 -m mesonbuild.mesonmain setup "${FORK_MESA_CONFIGURE[@]}" .tools/mesa-26.2.5/build-glibc .tools/mesa-26.2.5/container-src --native-file .tools/mesa-26.2.5/glibc-native.ini -Dc_link_args="$FORK_MESA_LINK_ARGS" -Dcpp_link_args="$FORK_MESA_LINK_ARGS" --prefix=/usr --libdir=lib --buildtype=release --wrap-mode=nodownload -Db_ndebug=true -Dgallium-drivers=zink,freedreno,virgl,softpipe -Dvulkan-drivers=freedreno -Dfreedreno-kmds=kgsl -Dplatforms=x11 -Dllvm=disabled -Dglvnd=enabled -Dgbm=enabled -Degl=enabled -Dglx=dri -Dgles1=enabled -Dgles2=enabled -Dgallium-va=disabled -Dgallium-rusticl=false -Dvideo-codecs=[] -Dxmlconfig=enabled -Dshader-cache=enabled -Dzstd=enabled -Dbuild-tests=false > .verification/mesa-26.2.5/glibc-configure.log 2>&1
ninja -C .tools/mesa-26.2.5/build-glibc -j3 > .verification/mesa-26.2.5/glibc-build.log 2>&1
python3 -m mesonbuild.mesonmain install -C .tools/mesa-26.2.5/build-glibc --no-rebuild --destdir "$PWD/.tools/mesa-26.2.5/stage-glibc" > .verification/mesa-26.2.5/glibc-install.log 2>&1
