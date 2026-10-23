#!/usr/bin/env bash
# Termux side. Only prepare may install, after the application's explicit confirmation.
# Existence is deliberately separate from login: a busy/broken root is never reinstalled.
jobs_agent_host_exists() {
    local alias=$1 base="${PREFIX:?Termux PREFIX is required}/var/lib/proot-distro" path
    for path in "$base/containers/$alias" "$base/installed-rootfs/$alias"; do
        [[ ! -e $path && ! -L $path ]] || return 0
    done
    return 1
}
jobs_agent_host_prepare() {
    local alias=${JOBS_AGENT_UBUNTU:-ubuntu-ai}
    case $alias in ubuntu|ubuntu-ai) ;;*) return 2;;esac
    if ! command -v proot-distro >/dev/null; then
        [[ ${JOBS_AGENT_SETUP_ALLOWED:-0} == 1 ]] || { printf '%s\n' 'Missing proot-distro. Confirm environment preparation in the app.' >&2;return 1; }
        pkg install -y proot-distro || return
    fi
    if ! jobs_agent_host_exists "$alias"; then
        [[ ${JOBS_AGENT_SETUP_ALLOWED:-0} == 1 ]] || { printf '%s\n' 'Selected Ubuntu is missing. Confirm environment preparation in the app.' >&2;return 1; }
        printf 'Installing selected agent environment: %s…\n' "$alias"
        bash "${BASH_SOURCE[0]%/*}/environment-runtime.sh" install "$alias" || return
    fi
    # Keep the actual login error visible. Do not install, reset or fall back on failure.
    if proot-distro login "$alias" -- /bin/true; then return 0; fi
    printf '%s\n' 'Selected Ubuntu could not be opened. It may be busy or incomplete. Retry after its installation finishes; no environment was replaced.' >&2
    return 1
}
if [[ ${BASH_SOURCE[0]} == "$0" ]]; then
    [[ ${1:-} == check ]] || exit 2
    alias=${4:-ubuntu-ai};case $alias in ubuntu|ubuntu-ai) ;;*) exit 2;;esac
    case ${2:-} in codex|claude|gemini|opencode) ;;*) exit 2;;esac
    if ! command -v proot-distro >/dev/null; then printf 'MISSING:proot-distro\nMISSING:ubuntu\nCHECK:done\n';exit;fi
    printf 'READY:proot-distro\n'
    if ! jobs_agent_host_exists "$alias";then printf 'MISSING:ubuntu\nCHECK:done\n';exit;fi
    # A failed check of an existing root is an error, not a missing-package plan.
    proot-distro login --bind "${BASH_SOURCE[0]%/*}:/tmp/termux-jobs-setup" "$alias" -- /bin/bash --noprofile --norc /tmp/termux-jobs-setup/agent-dependencies.sh check "$2" "${3:-api}" || exit 1
    printf 'READY:ubuntu\n'
fi
