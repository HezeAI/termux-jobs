#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Run this maintained script from the repository scripts directory.
ndkbase=/data/data/com.termux/files/home/Android/Sdk/ndk/28.1.13356709/toolchains/llvm/prebuilt/linux-x86_64
sysroot=$ndkbase/sysroot
cpplibs=$sysroot/usr/lib/aarch64-linux-android
common="--target=aarch64-linux-android26 --sysroot=$sysroot -fPIC -O2"
cxx="$common -nostdinc++ -isystem $sysroot/usr/include/c++/v1"
link="--no-default-config -Wl,--no-undefined -L$ndkbase/lib/clang/19/lib/linux/aarch64 --target=aarch64-linux-android26 --sysroot=$sysroot -nostdlib++ $cpplibs/libc++_static.a $cpplibs/libc++abi.a $ndkbase/lib/clang/19/lib/linux/aarch64/libunwind.a -ldl -lm -Wl,-z,max-page-size=16384"
mkdir -p .tools/local-ai/native-build
for cpu_variant in compatible fast; do
 cpu_fast=OFF;cpu_build=.tools/local-ai/repro-build;cpu_target=jobs_llama
 if [[ "$cpu_variant" == fast ]]; then cpu_fast=ON;cpu_build=.tools/local-ai/repro-build-fast;cpu_target=jobs_llama_fast; fi
.tools/local-ai/cmake/cmake/data/bin/cmake -S app/src/main/cpp/local-ai -B "$cpu_build" \
 -DJOBS_LLAMA_SOURCE="$PWD/.tools/local-ai/llama.cpp-b9222" -DJOBS_FAST_CPU="$cpu_fast" -DCMAKE_SKIP_RPATH=TRUE -DCMAKE_SYSTEM_NAME=Linux -DCMAKE_SYSTEM_PROCESSOR=aarch64 \
 -DCMAKE_C_COMPILER=/data/data/com.termux/files/usr/bin/clang \
 -DCMAKE_CXX_COMPILER=/data/data/com.termux/files/usr/bin/clang++ \
 -DCMAKE_ASM_COMPILER=/data/data/com.termux/files/usr/bin/clang \
 -DCMAKE_C_FLAGS="$common" -DCMAKE_CXX_FLAGS="$cxx" -DCMAKE_ASM_FLAGS="$common" \
 -DCMAKE_SHARED_LINKER_FLAGS="$link" -DCMAKE_EXE_LINKER_FLAGS="$link" \
 -DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY -DCMAKE_BUILD_TYPE=Release
.tools/local-ai/cmake/cmake/data/bin/cmake --build "$cpu_build" --target "$cpu_target" -j 4

 python3 scripts/clear-native-runpath.py "$cpu_build/lib$cpu_target.so"
 cp "$cpu_build/lib$cpu_target.so" .tools/local-ai/native-build/
done
