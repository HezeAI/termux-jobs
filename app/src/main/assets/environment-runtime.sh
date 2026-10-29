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
case $alias in ubuntu-ai|jobs-x11-ubuntu|ubuntu|debian|alpine|archlinux|fedora|opensuse|void|kali|manjaro) ;;*)printf 'ERROR:UNSUPPORTED_DISTRIBUTION\n';exit 2;;esac
if ! command -v proot-distro >/dev/null;then
    printf 'STAGE:install-proot-distro\n'
    command -v pkg >/dev/null || { printf 'ERROR:PROOT_REQUIRED\n';exit 2; }
    pkg install -y proot-distro
fi
printf 'STAGE:check-environment-installer\n'
help=$(proot-distro install --help 2>&1 || true)
printf 'STAGE:install-environment\n'
# Agent Ubuntu intentionally retains its existing upstream installation policy.
if [[ $alias == ubuntu-ai ]];then
    if [[ $help == *--name* ]];then proot-distro install ubuntu --name ubuntu-ai
    else proot-distro install --override-alias ubuntu-ai ubuntu;fi
    printf 'STAGE:verify\n';check;exit
fi
# ARM64 base images are immutable application pins, mirrored in Components.
# Keep extraction/fixups/locking in the public native CLI; no private OCI APIs.
case $(uname -m) in aarch64|arm64) ;; *)printf 'ERROR:IMAGE_ARM64_REQUIRED\n';exit 2;;esac
[[ $help == *--name* ]] || { printf 'ERROR:NATIVE_ARCHIVE_INSTALL_REQUIRED\n';exit 2; }
command -v python3 >/dev/null || { printf 'ERROR:PYTHON3_REQUIRED\n';exit 2; }
image=$(python3 -B "${BASH_SOURCE[0]%/*}/environment-image.py" "$alias") || exit
[[ -f $image && ! -L $image ]] || { printf 'ERROR:ROOTFS_IMAGE_REQUIRED\n';exit 2; }
printf 'STAGE:install-environment\n'
proot-distro install "$image" --name "$alias"
if [[ $alias == archlinux ]];then
    printf 'STAGE:prepare-archlinux-keyring\n'
    proot-distro login archlinux -- /bin/sh -s <<'ARCH_SETUP'
set -eu
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
pacman-key --init
pacman-key --populate archlinuxarm
ARCH_SETUP
fi
if [[ $alias == manjaro ]];then
    printf 'STAGE:prepare-manjaro-keyring\n'
    proot-distro login manjaro -- /bin/sh -s <<'MANJARO_SETUP'
set -eu
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
# The pinned upstream ARM base still lists community, which merged into extra.
# Only this just-created rootfs is adjusted; existing environments return above.
awk '/^\[/{skip=($0=="[community]")} !skip{print}' /etc/pacman.conf > /etc/pacman.conf.jobs
mv /etc/pacman.conf.jobs /etc/pacman.conf
pacman-key --init
pacman-key --populate archlinuxarm manjaro-arm manjaro
MANJARO_SETUP
fi
printf 'STAGE:verify\n';check
