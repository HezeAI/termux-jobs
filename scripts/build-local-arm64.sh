#!/usr/bin/env bash
# ARM64 host build: native libraries are linked locally, Gradle packages the fork app.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/build-env.sh"
cd "$FORK_ROOT"
: "${FORK_NATIVE_SYSROOT:?Set FORK_NATIVE_SYSROOT to an existing NDK sysroot (read only)}"
FORK_CLANG="${FORK_NATIVE_CLANG:-clang}"
FORK_LLD="${FORK_NATIVE_LLD:-ld.lld}"
./scripts/create-debug-key.sh
python3 scripts/prepare-x11.py
python3 scripts/prepare-java-verification.py --arm64
FORK_STAGE="$FORK_ROOT/.verification/arm64"
FORK_NATIVE="$FORK_ROOT/.verification/native"
mkdir -p "$FORK_NATIVE" "$FORK_STAGE/app/src/main/jniLibs/arm64-v8a" "$FORK_STAGE/terminal-emulator/src/main/jniLibs/arm64-v8a"
ln -sfn "$FORK_ROOT/.signing" "$FORK_STAGE/.signing"
FORK_BOOTSTRAP="$FORK_ROOT/.tools/downloads/bootstrap-aarch64.zip"
if [[ ! -f "$FORK_BOOTSTRAP" ]]; then
    mkdir -p "$FORK_ROOT/.tools/downloads"
    curl --fail --location --output "$FORK_BOOTSTRAP" 'https://github.com/termux/termux-packages/releases/download/bootstrap-2025.03.28-r1+apt-android-7/bootstrap-aarch64.zip'
fi
printf '%s  %s\n' 'c8d702b6f742935001c37cda81b8ac69504a95d5cf28f2899532dd8cd4b057eb' "$FORK_BOOTSTRAP" | sha256sum -c -
cp "$FORK_BOOTSTRAP" "$FORK_STAGE/app/src/main/cpp/bootstrap-aarch64.zip"
FORK_CFLAGS=(--target=aarch64-linux-android24 --sysroot="$FORK_NATIVE_SYSROOT" -std=c11 -Wall -Wextra -Werror -Os -fPIC -fno-stack-protector)
"$FORK_CLANG" "${FORK_CFLAGS[@]}" -c terminal-emulator/src/main/jni/termux.c -o "$FORK_NATIVE/termux.o"
"$FORK_CLANG" "${FORK_CFLAGS[@]}" -c app/src/main/cpp/termux-bootstrap.c -o "$FORK_NATIVE/bootstrap.o"
(cd "$FORK_STAGE/app/src/main/cpp"; "$FORK_CLANG" --target=aarch64-linux-android24 -c termux-bootstrap-zip.S -o "$FORK_NATIVE/bootstrap-zip.o")
FORK_NATIVE_LIB="$FORK_NATIVE_SYSROOT/usr/lib/aarch64-linux-android/24"
link_native() {
    local name="$1"; shift
    "$FORK_LLD" -shared --no-undefined --gc-sections -z now -z noexecstack -z max-page-size=16384 \
        -soname "$name" "$FORK_NATIVE_LIB/crtbegin_so.o" "$@" \
        -L "$FORK_NATIVE_LIB" -lc -ldl "$FORK_NATIVE_LIB/crtend_so.o" -o "$FORK_NATIVE/$name"
}
link_native libtermux.so "$FORK_NATIVE/termux.o"
link_native libtermux-bootstrap.so "$FORK_NATIVE/bootstrap.o" "$FORK_NATIVE/bootstrap-zip.o"
cp "$FORK_NATIVE/libtermux.so" "$FORK_STAGE/terminal-emulator/src/main/jniLibs/arm64-v8a/"
cp "$FORK_NATIVE/libtermux-bootstrap.so" "$FORK_STAGE/app/src/main/jniLibs/arm64-v8a/"
cd "$FORK_STAGE"
export TERMUX_SPLIT_APKS_FOR_DEBUG_BUILDS=0
export TERMUX_APP_VERSION_NAME="${TERMUX_APP_VERSION_NAME:-1.5.1-test}"
export TERMUX_APK_VERSION_TAG="v${TERMUX_APP_VERSION_NAME}-arm64"
export TERMUX_SPLIT_APKS_FOR_RELEASE_BUILDS=0
FORK_GRADLE="$FORK_ROOT/.tools/gradle-7.2/bin/gradle"
if [[ ! -x "$FORK_GRADLE" ]]; then FORK_GRADLE="$PWD/gradlew"; fi
# Recreate the APK container, retaining compile caches but dropping vacant ZIP blocks.
rm -f "$FORK_STAGE/app/build/outputs/apk/release/termux-app_${TERMUX_APK_VERSION_TAG}_universal.apk"
rm -rf "$FORK_STAGE/app/build/intermediates/incremental/packageRelease"
"$FORK_GRADLE" --no-daemon \
    -Pandroid.aapt2FromMavenOverride="${FORK_AAPT2:-$ANDROID_HOME/build-tools/30.0.3/aapt2}" \
    -x :app:downloadBootstraps :app:assembleRelease
mkdir -p "$FORK_ROOT/build/apk"
cp "$FORK_STAGE/app/build/outputs/apk/release/termux-app_${TERMUX_APK_VERSION_TAG}_universal.apk" "$FORK_ROOT/build/apk/Termux-Jobs-v${TERMUX_APP_VERSION_NAME}-arm64.apk"

# Inspect definitions in the packaged DEX, not merely strings or the JVM test classpath.
FORK_COMPILE_SDK="$(sed -n 's/^compileSdkVersion=//p' "$FORK_ROOT/gradle.properties")"
python3 "$FORK_ROOT/scripts/verify-apk-runtime.py" \
    --apk "$FORK_ROOT/build/apk/Termux-Jobs-v${TERMUX_APP_VERSION_NAME}-arm64.apk" \
    --android-jar "$ANDROID_HOME/platforms/android-$FORK_COMPILE_SDK/android.jar" \
    --output "$FORK_STAGE/apk-runtime.json"
