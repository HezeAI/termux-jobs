#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
runner_root="$PWD";runner_src="$PWD/.tools/local-ai/llama.cpp-b9222";runner_out="$PWD/.tools/local-ai/native-build"
runner_ndk=/data/data/com.termux/files/home/Android/Sdk/ndk/28.1.13356709/toolchains/llvm/prebuilt/linux-x86_64
runner_sys="$runner_ndk/sysroot";runner_cpp="$runner_sys/usr/lib/aarch64-linux-android"
for runner_variant in compatible fast; do
 runner_build="$PWD/.tools/local-ai/repro-build";runner_suffix="";runner_fast=OFF
 if [[ "$runner_variant" == fast ]]; then runner_build+=-fast;runner_suffix=_fast;runner_fast=ON;fi
 /data/data/com.termux/files/usr/bin/clang++ --no-default-config --target=aarch64-linux-android26 --sysroot="$runner_sys" -std=c++17 -fPIE -pie -O2 -nostdinc++ -isystem "$runner_sys/usr/include/c++/v1" -nostdlib++ \
 -Iapp/src/main/cpp/local-ai -I"$runner_src/include" -I"$runner_src/ggml/include" -I"$runner_src/vendor" app/src/main/cpp/local-ai/runner/jobs-runner.cpp \
 -Wl,--start-group "$runner_build/llama/src/libllama.a" "$runner_build/llama/ggml/src/libggml.a" "$runner_build/llama/ggml/src/libggml-base.a" "$runner_build/llama/ggml/src/libggml-cpu.a" -Wl,--end-group \
 "$runner_cpp/libc++_static.a" "$runner_cpp/libc++abi.a" "$runner_ndk/lib/clang/19/lib/linux/aarch64/libunwind.a" -L"$runner_ndk/lib/clang/19/lib/linux/aarch64" -ldl -lm -Wl,-z,max-page-size=16384 -o "$runner_out/libjobs_runner_termux$runner_suffix.so"
 runner_linux="$PWD/.tools/local-ai/runner-ubuntu$runner_suffix"
 .tools/local-ai/cmake/cmake/data/bin/cmake -S app/src/main/cpp/local-ai/runner -B "$runner_linux" -DJOBS_LLAMA_SOURCE="$runner_src" -DJOBS_FAST_CPU="$runner_fast" -DCMAKE_C_COMPILER=/usr/bin/gcc -DCMAKE_CXX_COMPILER=/usr/bin/g++ -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_FLAGS=-fPIE -DCMAKE_CXX_FLAGS=-fPIE -DCMAKE_EXE_LINKER_FLAGS='-static-pie -Wl,-z,max-page-size=16384'
 .tools/local-ai/cmake/cmake/data/bin/cmake --build "$runner_linux" --target jobs_runner -j4
 cp "$runner_linux/jobs_runner" "$runner_out/libjobs_runner_ubuntu$runner_suffix.so"
done
