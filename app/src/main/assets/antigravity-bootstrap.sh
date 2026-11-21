#!/usr/bin/env bash
# Google Antigravity CLI for Ubuntu AI. The fallback is pinned to the verified
# Linux ARM64 package. The managed updater can activate a newer verified build.
# Upstream manifest: https://antigravity-cli-auto-updater-974169037036.us-central1.run.app/manifests/linux_arm64.json
jobs_agy_pinned_version() { printf '%s\n' 1.5.13; }

jobs_agy_pinned_binary() {
    printf '%s\n' "$HOME/.local/share/termux-jobs/antigravity-cli/$(jobs_agy_pinned_version)/agy"
}

jobs_agy_binary() {
    local binary
    jobs_agent_cli_managed antigravity && return
    binary=$(jobs_agy_pinned_binary)
    [[ -d ${binary%/*} && ! -L ${binary%/*} && -f $binary && ! -L $binary && -x $binary ]] || return 1
    printf '%s\n' "$binary"
}

jobs_agy_node() {
    local path version_check='const [major, minor] = process.versions.node.split(".").map(Number);
        process.exit(major > 20 || (major === 20 && minor >= 8) ? 0 : 1)'
    path=$(type -P node) || path=
    if [[ $path == /* ]] && "$path" -e "$version_check" >/dev/null 2>&1; then
        printf '%s\n' "$path"
        return
    fi
    path=$(jobs_agent_node_path)
    [[ -x $path ]] && "$path" -e "$version_check" >/dev/null 2>&1 || return 1
    printf '%s\n' "$path"
}

jobs_agy_probe_output() {
    local binary=$1 probe_home=$2 arg=$3 limit=$4
    (
        cd -- "$probe_home" || exit $?
        # Antigravity CLI 1.5.13 writes successful help to stderr.
        if [[ $arg == --help ]]; then
            exec 2>&1
        else
            exec 2>/dev/null
        fi
        exec env -i HOME="$probe_home" XDG_CONFIG_HOME="$probe_home/config" XDG_CACHE_HOME="$probe_home/cache" \
            PATH="$PATH" LANG=C AGY_CLI_DISABLE_AUTO_UPDATE=true \
            timeout -k 1 8 "$binary" "$arg" </dev/null
    ) | head -c "$limit"
}

jobs_agy_probe() (
    set -o pipefail
    local binary=$1 expected=$2 probe_home=$3 version help flag
    mkdir -p -- "$probe_home" || exit $?
    version=$(jobs_agy_probe_output "$binary" "$probe_home" --version 257) || exit $?
    [[ ${#version} -le 256 && ! $version =~ [[:cntrl:]] ]] || exit 1
    [[ $version == "$expected" || $version == *" $expected" ]] || exit 1
    help=$(jobs_agy_probe_output "$binary" "$probe_home" --help 32769) || exit $?
    [[ ${#help} -le 32768 ]] || exit 1
    for flag in stream-json --input-format --output-format --mode --conversation; do
        [[ $help == *"$flag"* ]] || exit 1
    done
)

jobs_agy_install_pinned() (
    set -o pipefail
    local target=$1 version=$2 stage
    local directory=${target%/*} root=${target%/*/*}
    local url='https://storage.googleapis.com/antigravity-public/antigravity-cli/1.5.13-5999302598852608/linux-arm/cli_linux_arm64.tar.gz'
    local sha512=dccea2eb455099f5175f0c87cc47b38fcefe4cb0cb44dea3927469bb4bbf68ac3c0b30fc04affb24c17e90959386a93c3785e8e4b8b1a3338ca4e4d57b80057b
    [[ $(uname -m) == aarch64 ]] || { printf '%s\n' 'Antigravity CLI requires ARM64 Ubuntu AI.' >&2; exit 1; }
    umask 077
    mkdir -p -- "$root" || exit $?
    chmod 700 -- "$root" || exit $?
    exec 6>"$root/install.lock" || exit $?
    flock -w 300 6 || exit $?
    if [[ -d $directory && ! -L $directory && -f $target && ! -L $target && -x $target ]]; then exit 0; fi
    [[ ! -e $directory && ! -L $directory ]] || { printf '%s\n' 'Existing unusable Antigravity CLI preserved.' >&2; exit 1; }
    jobs_agent_stage installing-components
    stage=$(mktemp -d "$root/.install-XXXXXXXX") || exit $?
    trap 'rm -rf -- "$stage"' EXIT
    jobs_agent_download agy-cli "$stage/agy.tar.gz" "$url" || exit $?
    printf '%s  %s\n' "$sha512" "$stage/agy.tar.gz" | sha512sum -c - >/dev/null ||
        { printf 'ERROR:COMPONENT_INTEGRITY_FAILED\n' >&2; exit 1; }
    mkdir -- "$stage/runtime" || exit $?
    # The pinned digest identifies a tarball containing one regular file.
    # Stream its bytes instead of extracting archive paths into the filesystem.
    tar -xOzf "$stage/agy.tar.gz" antigravity > "$stage/runtime/agy" || exit $?
    chmod 700 -- "$stage/runtime/agy" || exit $?
    jobs_agent_stage checking-cli
    jobs_agy_probe "$stage/runtime/agy" "$version" "$stage/probe" ||
        { printf 'ERROR:CLI_STARTUP_FAILED\n' >&2; exit 1; }
    mv -T -- "$stage/runtime" "$directory" || exit $?
)

jobs_agy_prepare() {
    [[ ${JOBS_AGENT_UBUNTU:-} == ubuntu-ai ]] || return 2
    source "${BASH_SOURCE[0]%/*}/agent-download.sh" || return $?
    local binary node
    if ! node=$(jobs_agy_node); then
        [[ ${JOBS_AGENT_SETUP_ALLOWED:-0} == 1 ]] || return 1
        jobs_agent_prepare_private_node || return $?
        node=$(jobs_agy_node) || return $?
    fi
    if ! binary=$(jobs_agy_binary); then
        [[ ${JOBS_AGENT_SETUP_ALLOWED:-0} == 1 ]] || return 1
        binary=$(jobs_agy_pinned_binary)
        jobs_agy_install_pinned "$binary" "$(jobs_agy_pinned_version)" || return $?
    fi
    export JOBS_AGY_BIN="$binary" JOBS_AGY_NODE="$node" AGY_CLI_DISABLE_AUTO_UPDATE=true
    jobs_agent_stage ready
}
