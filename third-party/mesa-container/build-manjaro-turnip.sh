#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
MAIN=${JOBS_BRIDGE_INPUT_ROOT:-$PWD}
WORK=$PWD/.tools/manjaro-turnip
LOG=$PWD/.verification/manjaro-turnip
TOOLCHAIN=$PWD/.tools/manjaro-bridge
export PATH=$MAIN/.tools/universal-mesa/sysroot/usr/bin:$TOOLCHAIN/bin:$MAIN/.tools/virgl-fix/ninja-wheel/bin:$PATH
export BISON_PKGDATADIR=/data/data/com.termux/files/usr/share/bison
export PYTHONPATH=$MAIN/.tools/virgl-fix/python PYTHONDONTWRITEBYTECODE=1
export PKG_CONFIG_SYSROOT_DIR=$TOOLCHAIN/sysroot PKG_CONFIG_LIBDIR=$TOOLCHAIN/sysroot/usr/lib/pkgconfig:$TOOLCHAIN/sysroot/usr/share/pkgconfig
export TMPDIR=$WORK/tmp XDG_CACHE_HOME=$WORK/cache LD_LIBRARY_PATH=$TOOLCHAIN/host-libs
mkdir -p "$WORK" "$LOG" "$TMPDIR" "$XDG_CACHE_HOME"
exec 8> "$TOOLCHAIN/sysroot.lock"
flock -n 8 || { echo 'Manjaro build sysroot busy';exit 1; }
if [[ ! -d $WORK/mesa-26.2.5 ]];then
 sha256sum "$MAIN/.tools/mesa-26.2.5/mesa-26.2.5.tar.xz" | awk '$1!="794b3d1729075dae7ebd47c7283b7ca0cdb5dec668f7f11cc0a5d82f6138a0a5"{exit 1}'
 tar -xf "$MAIN/.tools/mesa-26.2.5/mesa-26.2.5.tar.xz" -C "$WORK"
 (cd "$WORK/mesa-26.2.5";patch -p1 --fuzz=0 < "$OLDPWD/third-party/mesa-container/lfdevs-android-26.2.5.patch") > "$LOG/source-patch.log" 2>&1
fi
python3 - "$WORK" "$TOOLCHAIN" <<'PY'
from pathlib import Path
import sys
w,t=map(Path,sys.argv[1:]);(w/'native.ini').write_text("[binaries]\nc = "+repr(str(t/'bin/gcc'))+"\ncpp = "+repr(str(t/'bin/g++'))+"\nar = "+repr(str(t/'bin/ar'))+"\nstrip = "+repr(str(t/'bin/strip'))+"\npkg-config = '/data/data/com.termux/files/usr/bin/pkg-config'\n[built-in options]\nc_args = ['-D_Nonnull=', '-D_Nullable=', '-D_Null_unspecified=']\ncpp_args = ['-D_Nonnull=', '-D_Nullable=', '-D_Null_unspecified=']\n")
PY
CONFIGURE=()
if [[ -f $WORK/build/meson-private/coredata.dat ]];then CONFIGURE=(--reconfigure);fi
python3 -m mesonbuild.mesonmain setup "${CONFIGURE[@]}" "$WORK/build" "$WORK/mesa-26.2.5" --native-file "$WORK/native.ini" --prefix=/usr --libdir=lib --buildtype=release --wrap-mode=nodownload -Db_ndebug=true -Dgallium-drivers=zink,freedreno,virgl,softpipe -Dvulkan-drivers=freedreno -Dfreedreno-kmds=kgsl -Dplatforms=x11 -Dllvm=disabled -Dglvnd=enabled -Dgbm=enabled -Degl=enabled -Dglx=dri -Dgles1=enabled -Dgles2=enabled -Dgallium-va=disabled -Dgallium-rusticl=false -Dvideo-codecs=[] -Dxmlconfig=enabled -Dshader-cache=enabled -Dzstd=enabled -Dbuild-tests=false > "$LOG/configure.log" 2>&1
ninja -C "$WORK/build" -j3 > "$LOG/build.log" 2>&1
python3 -m mesonbuild.mesonmain install -C "$WORK/build" --no-rebuild --destdir "$WORK/stage" > "$LOG/install.log" 2>&1
printf '%s\n' MANJARO_TURNIP_BUILD_COMPLETE > "$LOG/complete"
