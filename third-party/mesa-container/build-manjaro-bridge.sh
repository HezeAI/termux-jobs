#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
MAIN=${JOBS_BRIDGE_INPUT_ROOT:-$PWD}
WORK=$PWD/.tools/manjaro-bridge
LOG=$PWD/.verification/manjaro-gpu
SYSROOT=$WORK/sysroot
exec 8> "$WORK/sysroot.lock"
flock -n 8 || { echo "Private sysroot is in use" >&2;exit 1; }
MESA=$MAIN/.tools/mesa-26.2.5/mesa-26.2.5
export PATH=$MAIN/.tools/universal-mesa/sysroot/usr/bin:$WORK/bin:$MAIN/.tools/bionic-bridge/tools/usr/bin:$MAIN/.tools/virgl-fix/ninja-wheel/bin:$PATH
export BISON_PKGDATADIR=/data/data/com.termux/files/usr/share/bison
export PYTHONPATH=$MAIN/.tools/virgl-fix/python
export LD_LIBRARY_PATH=$WORK/host-libs
export PYTHONDONTWRITEBYTECODE=1 TMPDIR=$WORK/tmp XDG_CACHE_HOME=$WORK/cache
export PKG_CONFIG_SYSROOT_DIR=$SYSROOT PKG_CONFIG_LIBDIR=$SYSROOT/usr/lib/pkgconfig:$SYSROOT/usr/share/pkgconfig
mkdir -p "$LOG" "$TMPDIR" "$XDG_CACHE_HOME" "$WORK/runtime/linker"
export ACLOCAL_PATH=$MAIN/.tools/bionic-bridge/tools/usr/share/aclocal:/data/data/com.termux/files/usr/share/aclocal
for name in config.guess config.sub; do cp -L "$MAIN/.tools/bionic-bridge/tools/usr/share/automake-1.18/$name" "$WORK/libhybris/hybris/$name"; done
if [[ ! -f $WORK/libhybris/hybris/configure ]];then
 (cd "$WORK/libhybris/hybris"; NOCONFIGURE=1 ./autogen.sh) > "$LOG/hybris-autogen.log" 2>&1
fi
(cd "$WORK/libhybris/hybris"
 CC="$WORK/bin/gcc" CXX="$WORK/bin/g++" CFLAGS=-O2 CXXFLAGS=-O2 ./configure --prefix="$WORK/install" --libdir="$WORK/install/lib" --with-android-headers="$MAIN/.tools/bionic-bridge/android-headers" --enable-arch=arm64 --enable-adreno-quirks --enable-property-cache --with-default-hybris-ld-library-path=/system/lib64:/vendor/lib64:/apex/com.android.runtime/lib64/bionic > "$LOG/hybris-configure.log" 2>&1
 make -C common -j2 libhybris-common.la > "$LOG/hybris-build.log" 2>&1
 make -C common/q -j2 > "$LOG/linker-build.log" 2>&1
)
cp "$WORK/libhybris/hybris/common/.libs/libhybris-common.so.1.0.0" "$WORK/runtime/libhybris-common.so.1"
cp "$WORK/libhybris/hybris/common/q/.libs/q.so" "$WORK/runtime/linker/q.so"
ln -sfn libhybris-common.so.1 "$WORK/runtime/libhybris-common.so"
echo HYBRIS_READY
"$WORK/bin/gcc" -fPIC -shared -O2 x11/src/bridge/bionic-icd.c -I"$MAIN/.tools/bionic-bridge/android-headers" -I"$WORK/libhybris/hybris/include" -I"$MESA/include" -I"$MESA/include/android_stub" -L"$WORK/runtime" -lhybris-common -ldl -pthread -Wl,-soname,libjobs-bionic.so '-Wl,-rpath,$ORIGIN' -Wl,-z,nodelete -o "$WORK/runtime/libjobs-bionic.so" > "$LOG/icd-build.log" 2>&1
ln -sfn libjobs-bionic.so "$WORK/runtime/libandroid.so"
echo ICD_READY
CMAKE=$MAIN/.tools/bionic-bridge/cmake/cmake/data/bin/cmake
"$CMAKE" -S "$WORK/wsi-layer" -B "$WORK/wsi-build" -G Ninja -DCMAKE_POLICY_VERSION_MINIMUM=3.5 -DCMAKE_C_COMPILER="$WORK/bin/gcc" -DCMAKE_CXX_COMPILER="$WORK/bin/g++" -DCMAKE_C_FLAGS="-O2 -I$SYSROOT/usr/include" -DCMAKE_CXX_FLAGS="-O2 -I$SYSROOT/usr/include" -DCMAKE_SHARED_LINKER_FLAGS="-L$WORK/runtime -L$SYSROOT/usr/lib -Wl,-rpath-link,$SYSROOT/usr/lib -Wl,-rpath-link,$WORK/runtime" -DVULKAN_CXX_INCLUDE="$MAIN/.tools/virgl-fix/mesa-26.1.6/include;$MAIN/.tools/virgl-fix/mesa-26.1.6/include/android_stub" -DBUILD_WSI_X11=ON -DBUILD_WSI_WAYLAND=OFF -DBUILD_WSI_DISPLAY=OFF > "$LOG/wsi-configure.log" 2>&1
ninja -C "$WORK/wsi-build" -j2 > "$LOG/wsi-build.log" 2>&1
cp "$WORK/wsi-build/libVkLayer_window_system_integration.so" "$WORK/runtime/"
echo WSI_READY
python3 - "$WORK" <<'PY'
from pathlib import Path
import sys
w=Path(sys.argv[1]);s=w/'sysroot'
(w/'mesa-native.ini').write_text('[binaries]\nc = '+repr(str(w/'bin/gcc'))+'\ncpp = '+repr(str(w/'bin/g++'))+'\nar = '+repr(str(w/'bin/ar'))+'\nstrip = '+repr(str(w/'bin/strip'))+'\npkg-config = \'/data/data/com.termux/files/usr/bin/pkg-config\'\n')
PY
python3 -m mesonbuild.mesonmain setup "$WORK/mesa-build" "$MESA" --native-file "$WORK/mesa-native.ini" --prefix=/opt/termux-jobs-bionic-mesa --libdir=lib --buildtype=release --wrap-mode=nodownload -Db_ndebug=true -Dgallium-drivers=zink -Dvulkan-drivers=[] -Dplatforms=x11 -Dllvm=disabled -Dglvnd=enabled -Dgbm=disabled -Degl=enabled -Dglx=dri -Dgles1=enabled -Dgles2=enabled -Dgallium-va=disabled -Dgallium-rusticl=false -Dvideo-codecs=[] -Dxmlconfig=disabled -Dshader-cache=enabled -Dzstd=disabled -Dbuild-tests=false > "$LOG/mesa-configure.log" 2>&1
ninja -C "$WORK/mesa-build" -j3 > "$LOG/mesa-build.log" 2>&1
python3 -m mesonbuild.mesonmain install -C "$WORK/mesa-build" --no-rebuild --destdir "$WORK/mesa-stage" > "$LOG/mesa-install.log" 2>&1
echo MESA_READY
