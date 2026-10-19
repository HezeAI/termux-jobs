#!/usr/bin/env bash
# Invoked by an explicit in-app check or user-confirmed installation.
set -euo pipefail
op=${1:-};alias=${2:-}
[[ $op == check || $op == install ]] || exit 2
[[ $alias == termux || $alias =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$ ]] || exit 2
: "${PREFIX:?Termux PREFIX is required}"
root="$PREFIX/var/lib/proot-distro"
find_root() {
    local candidate
    for candidate in "$root/containers/$alias/rootfs" "$root/installed-rootfs/$alias"; do
        [[ ! -L $candidate && ! -L ${candidate%/rootfs} && -d $candidate ]] || continue
        printf '%s\n' "$candidate";return 0
    done
    return 1
}
check() {
    if [[ $alias == termux ]]; then
        [[ -x $PREFIX/bin/bash ]] && printf 'READY:shell\n' || printf 'MISSING:shell\n'
        printf 'READY:environment\nCHECK:done\n';return
    fi
    command -v proot-distro >/dev/null && printf 'READY:proot-distro\n' || printf 'MISSING:proot-distro\n'
    local fs
    if fs=$(find_root); then
        printf 'READY:environment\n'
        if [[ -x $fs/bin/bash || -x $fs/usr/bin/bash || -x $fs/bin/sh || -L $fs/bin/sh ]]; then printf 'READY:shell\n';else printf 'MISSING:shell\n';fi
    else printf 'MISSING:environment\n';fi
    printf 'CHECK:done\n'
}
if [[ $op == check ]];then check;exit;fi
[[ $alias != termux ]] || { check;exit; }
# Never replace or reset an existing environment, even if installation was raced.
if find_root >/dev/null;then check;exit;fi
case $alias in ubuntu|debian|alpine|archlinux|fedora|opensuse|void) ;;*)printf 'ERROR:UNSUPPORTED_DISTRIBUTION\n';exit 2;;esac
if ! command -v proot-distro >/dev/null;then
    printf 'STAGE:install-proot-distro\n'
    command -v pkg >/dev/null || { printf 'ERROR:PROOT_REQUIRED\n';exit 2; }
    pkg install -y proot-distro
fi
help=$(proot-distro install --help 2>&1 || true)
printf 'STAGE:install-environment\n'
# Arch Linux itself targets x86-64. On this ARM64 build use the Arch Linux ARM
# rootfs published by Termux. URL and SHA-256 match the official v4.35.0 plugin
# and GitHub v4.34.2 release asset digest; never execute a downloaded plugin.
install_arch_arm() {
    case $(uname -m) in aarch64|arm64) ;; *)printf 'ERROR:ARCH_ARM64_REQUIRED\n';return 2;;esac
    command -v curl >/dev/null || { printf 'MISSING:curl\n';return 2; }
    command -v sha256sum >/dev/null || { printf 'MISSING:sha256sum\n';return 2; }
    local download_root=${JOBS_ENVIRONMENT_DOWNLOAD_DIR:-${TMPDIR:-$PREFIX/tmp}}
    mkdir -p -- "$download_root"
    archive_directory=$(mktemp -d -- "$download_root/jobs-arch.XXXXXXXX")
    trap 'rm -rf -- "$archive_directory"' EXIT
    local file="$archive_directory/archlinux-arm64.tar.xz"
    local sha=dabc2382ddcb725969cf7b9e2f3b102ec862ea6e0294198a30c71e9a4b837f81
    printf 'STAGE:download-arch-linux-arm\n'
    curl --fail --location --proto '=https' --proto-redir '=https' --retry 2 --connect-timeout 20 \
        --output "$file" 'https://github.com/termux/proot-distro/releases/download/v4.34.2/archlinux-aarch64-pd-v4.34.2.tar.xz' || { printf 'ERROR:ROOTFS_DOWNLOAD_FAILED\n';return 1; }
    printf 'STAGE:verify-rootfs-sha256\n'
    printf '%s  %s\n' "$sha" "$file" | sha256sum -c - || { printf 'ERROR:ROOTFS_CHECKSUM_MISMATCH\n';return 1; }
    printf 'STAGE:install-environment\n'
    proot-distro install "$file" --name "$alias"
}
if [[ $help == *--name* ]];then
    case $alias in
        ubuntu|debian|alpine|fedora) image=$alias;;
        opensuse) image=opensuse/leap;;
        void) image=ghcr.io/void-linux/void-glibc-full:latest;;
        archlinux) install_arch_arm;printf 'STAGE:verify\n';check;exit;;
        *)printf 'ERROR:DISTRIBUTION_UNAVAILABLE\n';exit 2;;
    esac
    proot-distro install "$image" --name "$alias"
else
    [[ -f $PREFIX/etc/proot-distro/$alias.sh ]] || { printf 'ERROR:DISTRIBUTION_UNAVAILABLE\n';exit 2; }
    proot-distro install "$alias"
fi
printf 'STAGE:verify\n';check
