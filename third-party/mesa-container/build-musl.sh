#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p .verification/mesa-26.2.5 .tools/tmp
export PYTHONPATH=$PWD/.tools/virgl-fix/python TMPDIR=$PWD/.tools/tmp XDG_CACHE_HOME=$PWD/.tools/mesa-26.2.5/cache-musl
export PATH=$PWD/.tools/virgl-fix/bin:$PWD/.tools/virgl-fix/ninja-wheel/bin:$PATH
FORK_MESA_CONFIGURE=()
if [[ -f .tools/mesa-26.2.5/build-musl/meson-private/coredata.dat ]];then FORK_MESA_CONFIGURE=(--reconfigure);fi
python3 -m mesonbuild.mesonmain setup "${FORK_MESA_CONFIGURE[@]}" .tools/mesa-26.2.5/build-musl .tools/mesa-26.2.5/container-src --cross-file .tools/virgl-fix/cross.ini --prefix=/usr --libdir=lib --buildtype=release --wrap-mode=nodownload -Db_ndebug=true -Dgallium-drivers=zink,freedreno,virgl,softpipe -Dvulkan-drivers=freedreno -Dfreedreno-kmds=kgsl -Dplatforms=x11 -Dllvm=disabled -Dglvnd=disabled -Dgbm=enabled -Degl=enabled -Dglx=dri -Dgles1=enabled -Dgles2=enabled -Dgallium-va=disabled -Dgallium-rusticl=false -Dvideo-codecs=[] -Dxmlconfig=enabled -Dshader-cache=enabled -Dzstd=enabled -Dbuild-tests=false > .verification/mesa-26.2.5/musl-configure.log 2>&1
ninja -C .tools/mesa-26.2.5/build-musl -j2 > .verification/mesa-26.2.5/musl-build.log 2>&1
python3 -m mesonbuild.mesonmain install -C .tools/mesa-26.2.5/build-musl --no-rebuild --destdir "$PWD/.tools/mesa-26.2.5/stage-musl" > .verification/mesa-26.2.5/musl-install.log 2>&1
