#!/usr/bin/env bash
# Termux side. Only prepare may install, after the application's explicit confirmation.
jobs_agent_host_prepare() {
    if ! command -v proot-distro >/dev/null; then
        [[ ${JOBS_AGENT_SETUP_ALLOWED:-0} == 1 ]] || { printf '%s\n' 'Missing proot-distro. Confirm environment preparation in the app.' >&2;return 1; }
        pkg install -y proot-distro || return
    fi
    if ! proot-distro login ubuntu -- /bin/true >/dev/null 2>&1; then
        [[ ${JOBS_AGENT_SETUP_ALLOWED:-0} == 1 ]] || { printf '%s\n' 'Ubuntu is unavailable. Confirm environment preparation in the app.' >&2;return 1; }
        printf '%s\n' 'Installing Ubuntu for AI Agents…'
        # proot-distro refuses an already existing rootfs; never remove/replace one.
        proot-distro install ubuntu || return
    fi
}
if [[ ${BASH_SOURCE[0]} == "$0" ]]; then
    [[ ${1:-} == check ]] || exit 2
    case ${2:-} in codex|claude|gemini|opencode) ;;*) exit 2;;esac
    if ! command -v proot-distro >/dev/null; then printf 'MISSING:proot-distro\nMISSING:ubuntu\nCHECK:done\n';exit;fi
    printf 'READY:proot-distro\n'
    if ! proot-distro login --bind "${BASH_SOURCE[0]%/*}:/tmp/termux-jobs-setup" ubuntu -- /bin/bash --noprofile --norc /tmp/termux-jobs-setup/agent-dependencies.sh check "$2" "${3:-api}"; then printf 'MISSING:ubuntu\nCHECK:done\n';else printf 'READY:ubuntu\n';fi
fi
