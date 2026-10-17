#!/usr/bin/env bash
set -euo pipefail
cd /root/termux-fork
mkdir -p .tools/local-ai/opencl-headers/CL
if [[ ! -f .tools/local-ai/opencl-headers/CL/cl.h ]]; then curl --fail --location --max-time 60 https://raw.githubusercontent.com/KhronosGroup/OpenCL-Headers/v2025.07.22/CL/cl.h -o .tools/local-ai/opencl-headers/CL/cl.h; fi
printf '%s  %s\n' '0e695976bc3014c8376efc6436ef4dafce25deb74ddd7b1c8c07924444f293fc' .tools/local-ai/opencl-headers/CL/cl.h | sha256sum -c -
if [[ ! -f .tools/local-ai/opencl-headers/CL/cl_platform.h ]]; then curl --fail --location --max-time 60 https://raw.githubusercontent.com/KhronosGroup/OpenCL-Headers/v2025.07.22/CL/cl_platform.h -o .tools/local-ai/opencl-headers/CL/cl_platform.h; fi
printf '%s  %s\n' 'be9647d121bfea20cfac9fe2be2e3ac7851a0dd77a141363dcc72f126a1a358c' .tools/local-ai/opencl-headers/CL/cl_platform.h | sha256sum -c -
if [[ ! -f .tools/local-ai/opencl-headers/CL/cl_version.h ]]; then curl --fail --location --max-time 60 https://raw.githubusercontent.com/KhronosGroup/OpenCL-Headers/v2025.07.22/CL/cl_version.h -o .tools/local-ai/opencl-headers/CL/cl_version.h; fi
printf '%s  %s\n' '1b161f73c0f0b07121314055f75df6c19d6a93e3a0d24202c6f3e2dd1615b2a2' .tools/local-ai/opencl-headers/CL/cl_version.h | sha256sum -c -
gpu_ndk=/data/data/com.termux/files/home/Android/Sdk/ndk/28.1.13356709/toolchains/llvm/prebuilt/linux-x86_64
gpu_sys="$gpu_ndk/sysroot"
gpu_cpp="$gpu_sys/usr/lib/aarch64-linux-android"
gpu_common="--target=aarch64-linux-android26 --sysroot=$gpu_sys -fPIC -O2"
gpu_cxx="$gpu_common -nostdinc++ -isystem $gpu_sys/usr/include/c++/v1"
gpu_link="--no-default-config -Wl,--no-undefined -L$gpu_ndk/lib/clang/19/lib/linux/aarch64 --target=aarch64-linux-android26 --sysroot=$gpu_sys -nostdlib++ $gpu_cpp/libc++_static.a $gpu_cpp/libc++abi.a $gpu_ndk/lib/clang/19/lib/linux/aarch64/libunwind.a -ldl -lm -Wl,-z,max-page-size=16384"
.tools/local-ai/cmake/cmake/data/bin/cmake -S app/src/main/cpp/local-ai/runner/opencl -B .tools/local-ai/runner-opencl \
 -DJOBS_LLAMA_SOURCE="$PWD/.tools/local-ai/llama.cpp-b9222" \
 -DCMAKE_SKIP_RPATH=TRUE -DCMAKE_SYSTEM_NAME=Linux -DCMAKE_SYSTEM_PROCESSOR=aarch64 \
 -DCMAKE_C_COMPILER=/data/data/com.termux/files/usr/bin/clang \
 -DCMAKE_CXX_COMPILER=/data/data/com.termux/files/usr/bin/clang++ \
 -DCMAKE_ASM_COMPILER=/data/data/com.termux/files/usr/bin/clang \
 -DCMAKE_C_FLAGS="$gpu_common" -DCMAKE_CXX_FLAGS="$gpu_cxx" -DCMAKE_ASM_FLAGS="$gpu_common" \
 -DCMAKE_SHARED_LINKER_FLAGS="$gpu_link" -DCMAKE_EXE_LINKER_FLAGS="$gpu_link" \
 -DOpenCL_INCLUDE_DIR="$PWD/.tools/local-ai/opencl-headers" -DOpenCL_LIBRARY=/vendor/lib64/libOpenCL.so \
 -DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY -DCMAKE_BUILD_TYPE=Release
.tools/local-ai/cmake/cmake/data/bin/cmake --build .tools/local-ai/runner-opencl --target jobs_runner_opencl -j4
python3 scripts/clear-native-runpath.py .tools/local-ai/runner-opencl/jobs_runner_opencl
cp .tools/local-ai/runner-opencl/jobs_runner_opencl .tools/local-ai/native-build/libjobs_runner_opencl.so
