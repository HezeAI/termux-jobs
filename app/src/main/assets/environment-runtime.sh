#!/usr/bin/env bash
# Invoked by an explicit in-app check or user-confirmed installation.
set -euo pipefail
op=${1:-};alias=${2:-}
[[ $op == check || $op == install ]] || exit 2
[[ $alias == termux || $alias =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$ ]] || exit 2
: "${PREFIX:?Termux PREFIX is required}"
root="$PREFIX/var/lib/proot-distro"
# Explicitly consented prerequisite preparation; never enters/modifies a guest rootfs.
if [[ $op == install && $alias == agent-migration-tools ]];then
    needed=()
    command -v python3 >/dev/null || needed+=(python)
    if ! command -v proot-distro >/dev/null || ! python3 -B -c 'from proot_distro.constants import RUNTIME_DIR; from proot_distro.locking import LOCKS_DIR; import os; assert RUNTIME_DIR==os.environ["PREFIX"]+"/var/lib/proot-distro" and LOCKS_DIR==RUNTIME_DIR+"/locks"' >/dev/null 2>&1;then needed+=(proot-distro);fi
    if (( ${#needed[@]} ));then
        command -v pkg >/dev/null || { printf 'ERROR:PKG_REQUIRED\n';exit 2; }
        printf 'STAGE:prepare-migration-tools\n'
        pkg install -y "${needed[@]}"
    fi
    python3 -B -c 'from proot_distro.constants import RUNTIME_DIR; from proot_distro.locking import LOCKS_DIR; import os; assert RUNTIME_DIR==os.environ["PREFIX"]+"/var/lib/proot-distro" and LOCKS_DIR==RUNTIME_DIR+"/locks"' >/dev/null 2>&1 || { printf 'ERROR:NATIVE_LOCKS_REQUIRED\n';exit 1; }
    printf 'READY:environment\nREADY:shell\nCHECK:done\n';exit
fi
find_root() {
    local candidate
    for candidate in "$root/containers/$alias/rootfs" "$root/installed-rootfs/$alias"; do
        [[ ! -L $candidate && ! -L ${candidate%/rootfs} && -d $candidate ]] || continue
        printf '%s\n' "$candidate";return 0
    done
    return 1
}
# Resolve links using the guest's /, never the host's /bin or /usr/bin.
# All path components (including a merged-usr /bin link) must stay in this rootfs.
guest_executable() {
    local fs=$1 component candidate link part hops=0
    local -a pending resolved=() pieces
    IFS=/ read -r -a pending <<< "$2"
    while (( ${#pending[@]} ));do
        component=${pending[0]};pending=("${pending[@]:1}")
        case $component in
            ''|.) continue;;
            ..) if (( ${#resolved[@]} ));then unset "resolved[$((${#resolved[@]}-1))]";fi;continue;;
        esac
        candidate=$fs
        for part in "${resolved[@]}";do candidate+="/$part";done
        candidate+="/$component"
        if [[ -L $candidate ]];then
            hops=$((hops+1));(( hops<=40 )) || return 1
            link=$(readlink -- "$candidate") || return 1
            [[ -n $link ]] || return 1
            if [[ $link == /* ]];then resolved=();fi
            IFS=/ read -r -a pieces <<< "$link"
            pending=("${pieces[@]}" "${pending[@]}")
        else
            resolved+=("$component")
            if (( ${#pending[@]} ));then [[ -d $candidate ]] || return 1
            else [[ -f $candidate && -x $candidate ]] || return 1;fi
        fi
    done
    [[ -n ${candidate:-} && -f $candidate && -x $candidate && ! -L $candidate ]]
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
        if guest_executable "$fs" /bin/bash || guest_executable "$fs" /usr/bin/bash || guest_executable "$fs" /bin/sh;then printf 'READY:shell\n';else printf 'MISSING:shell\n';fi
    else printf 'MISSING:environment\n';fi
    printf 'CHECK:done\n'
}
if [[ $op == check ]];then check;exit;fi
[[ $alias != termux ]] || { check;exit; }
# Never replace or reset an existing environment, even if installation was raced.
if find_root >/dev/null;then
    if [[ ${JOBS_ENVIRONMENT_RETRY:-0} == 1 || -f ${JOBS_ENVIRONMENT_DOWNLOAD_DIR:-$PREFIX/tmp}/downloads/$alias/state.json ]];then
        printf 'ERROR:INCOMPLETE_ROOTFS_REQUIRES_REMOVAL\n';exit 1
    fi
    check;exit
fi
case $alias in ubuntu-ai|jobs-x11-ubuntu|ubuntu|debian|alpine|archlinux|fedora|opensuse|void) ;;*)printf 'ERROR:UNSUPPORTED_DISTRIBUTION\n';exit 2;;esac
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
        ubuntu-ai|jobs-x11-ubuntu) image=ubuntu;;
        ubuntu|debian|alpine|fedora) image=$alias;;
        opensuse) image=opensuse/leap;;
        void) image=ghcr.io/void-linux/void-glibc-full:latest;;
        archlinux) install_arch_arm;printf 'STAGE:verify\n';check;exit;;
        *)printf 'ERROR:DISTRIBUTION_UNAVAILABLE\n';exit 2;;
    esac
    # Use the public CLI directly. No private Python callbacks or custom OCI prefetch.
    proot-distro install "$image" --name "$alias"
else
    if [[ $alias == jobs-x11-ubuntu || $alias == ubuntu-ai ]];then
        proot-distro install --override-alias "$alias" ubuntu
        printf 'STAGE:verify\n';check;exit
    fi
    [[ -f $PREFIX/etc/proot-distro/$alias.sh ]] || { printf 'ERROR:DISTRIBUTION_UNAVAILABLE\n';exit 2; }
    proot-distro install "$alias"
fi
printf 'STAGE:verify\n';check
