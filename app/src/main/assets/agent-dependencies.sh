#!/usr/bin/env bash
# Only called by a user-launched Ubuntu agent. check never installs or authenticates.
jobs_agent_valid() { [[ ${1:-} == codex || ${1:-} == claude || ${1:-} == gemini || ${1:-} == opencode ]]; }
jobs_agent_say() { if [[ ${JOBS_AGENT_LANGUAGE:-en} == ru ]]; then printf '%s\n' "$1"; else printf '%s\n' "$2"; fi; }
jobs_agent_base_tools() { printf '%s\n' curl tar sha256sum flock xz find sort xargs; }
jobs_agent_has_node() { command -v node >/dev/null && node -e 'process.exit(Number(process.versions.node.split(".")[0])>=20?0:1)' >/dev/null 2>&1; }
jobs_agent_node_path() { printf '%s\n' "$HOME/.local/share/termux-jobs/agent-tools/node-v22.22.1-linux-arm64/bin/node"; }
jobs_agent_cloud_credentials() { [[ -n ${GOOGLE_API_KEY:-} ]] || [[ -n ${GOOGLE_APPLICATION_CREDENTIALS:-} && -r $GOOGLE_APPLICATION_CREDENTIALS ]] || [[ -r ${CLOUDSDK_CONFIG:-$HOME/.config/gcloud}/application_default_credentials.json ]]; }
jobs_agent_cloud_path() { printf '%s\n' "$HOME/.local/share/termux-jobs/gemini-cli/google-cloud-sdk-589.0.0/bin/gcloud"; }
jobs_agent_check() (
    local agent=${1:-} auth=${2:-api} root="$HOME/.local/share/termux-jobs" tool path node_ok=0
    jobs_agent_valid "$agent" || return 2
    # Match jobs_ai_prepare's private executable/runtime search path. This is a
    # subshell-local inventory only: never source bootstrap or install anything.
    if [[ $agent == opencode ]]; then
        export PATH="$root/agents/bin:$root/agents/node-v22.22.1-linux-arm64/bin:$root/agents/node_modules/.bin:$PATH"
    fi
    for tool in $(jobs_agent_base_tools); do if command -v "$tool" >/dev/null; then printf 'READY:%s\n' "$tool"; else printf 'MISSING:%s\n' "$tool"; fi; done
    if [[ -r /etc/ssl/certs/ca-certificates.crt ]]; then printf 'READY:ca-certificates\n'; else printf 'MISSING:ca-certificates\n'; fi
    if jobs_agent_has_node; then node_ok=1; fi
    case $agent in
        codex) path="$root/codex-cli/0.163.0/vendor/aarch64-unknown-linux-musl/bin/codex"; command -v codex >/dev/null && path=$(command -v codex);;
        claude) path="$root/claude-code/2.1.296/claude";;
        gemini) path="$root/gemini-cli/0.64.0/bundle/gemini.js";node_ok=0;;
        opencode) path="$root/agents/bin/opencode"; if [[ ! -x $path ]] && command -v opencode >/dev/null; then path=$(command -v opencode); fi;;
    esac
    if [[ $agent == gemini && -r $path || $agent != gemini && -x $path ]] && { [[ $agent != opencode ]] || [[ $("$path" --version 2>/dev/null) == "opencode v2.0.28" ]]; }; then printf 'READY:%s-cli\n' "$agent"; else printf 'MISSING:%s-cli\n' "$agent"; fi
    case $agent in
        codex) tool=$(jobs_agent_node_path);;
        claude) tool="$root/claude-code/node-v22.22.1-linux-arm64/bin/node";;
        gemini) tool="$root/gemini-cli/node-v22.22.1-linux-arm64/bin/node";;
        opencode) tool="$root/agents/node-v22.22.1-linux-arm64/bin/node";;
    esac
    if [[ -x $tool ]] && "$tool" -e 'process.exit(Number(process.versions.node.split(".")[0])>=20?0:1)' >/dev/null 2>&1; then node_ok=1;fi
    if ((node_ok)); then printf 'READY:node\n'; else printf 'MISSING:node\n'; fi
    if [[ $agent == gemini && $auth == vertex ]] && ! jobs_agent_cloud_credentials; then
        printf 'AUTH:needed\n'
        if command -v gcloud >/dev/null || [[ -x $(jobs_agent_cloud_path) ]]; then printf 'READY:gcloud\n'; else printf 'MISSING:gcloud\n'; fi
        if command -v python3 >/dev/null; then printf 'READY:python3\n'; else printf 'MISSING:python3\n'; fi
    fi
    printf 'CHECK:done\n'
)
jobs_agent_base_prepare() (
    set -euo pipefail
    local agent=$1 auth=${2:-api} tool package packages=()
    for tool in $(jobs_agent_base_tools); do
        command -v "$tool" >/dev/null && continue
        case $tool in curl) package=curl;;tar) package=tar;;sha256sum|sort) package=coreutils;;flock) package=util-linux;;xz) package=xz-utils;;find|xargs) package=findutils;;esac
        [[ " ${packages[*]} " == *" $package "* ]] || packages+=("$package")
    done
    [[ -r /etc/ssl/certs/ca-certificates.crt ]] || packages+=(ca-certificates)
    if [[ $agent == gemini && $auth == vertex ]] && ! jobs_agent_cloud_credentials && ! command -v python3 >/dev/null; then packages+=(python3);fi
    if ((${#packages[@]})); then
        jobs_agent_say 'Установка недостающих пакетов Ubuntu…' 'Installing missing Ubuntu packages…'
        command -v apt-get >/dev/null || { jobs_agent_say 'В этой среде нет APT. Пакеты не изменены.' 'APT is unavailable in this environment. No packages changed.' >&2;return 1; }
        # Native APT locking only. No lock deletion, upgrades or changes to configured agents.
        apt-get update || return $?
        DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends -o Dpkg::Options::=--force-confold "${packages[@]}"
    fi
)
jobs_agent_prepare() {
    local agent=${1:-} auth=${2:-api} report source=${BASH_SOURCE[0]%/*}
    jobs_agent_valid "$agent" || return 2
    jobs_agent_say 'Проверка компонентов выбранного агента…' 'Checking the selected agent components…'
    report=$(jobs_agent_check "$agent" "$auth") || return
    if [[ $report == *MISSING:* && ${JOBS_AGENT_SETUP_ALLOWED:-0} != 1 ]]; then
        jobs_agent_say 'Не хватает компонентов. Повторите запуск и подтвердите подготовку окружения.' 'Components are missing. Relaunch and confirm environment preparation.' >&2;return 1
    fi
    jobs_agent_base_prepare "$agent" "$auth" || return
    if [[ $agent == codex ]]; then source "$source/codex-bootstrap.sh" || return; fi
}
if [[ ${BASH_SOURCE[0]} == "$0" ]]; then
    [[ ${1:-} == check ]] || exit 2
    jobs_agent_check "${2:-}" "${3:-api}"
fi
