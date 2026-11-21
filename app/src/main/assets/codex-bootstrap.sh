#!/usr/bin/env bash
# Private missing-only Codex/Node. Existing global tools and accounts are never replaced.
source "${BASH_SOURCE[0]%/*}/agent-download.sh" || return $?
source "${BASH_SOURCE[0]%/*}/agent-cli-resolver.sh" || return $?
_jobs_codex_prepare() (
    set -euo pipefail
    if declare -F jobs_agent_stage >/dev/null;then jobs_agent_stage installing-components;fi
    local root="$HOME/.local/share/termux-jobs/codex-cli" tools="$HOME/.local/share/termux-jobs/agent-tools" stage
    mkdir -p "$root" "$tools" || exit $?;chmod 700 "$root" "$tools" || exit $?
    exec 8>"$root/install.lock" || exit $?;flock -w 300 8 || exit $?
    stage=$(mktemp -d "$root/.install-XXXXXXXX") || exit $?;trap 'rm -rf -- "$stage"' EXIT || exit $?
    if ! jobs_agent_has_node && [[ ! -x $(jobs_agent_node_path) ]]; then
        jobs_agent_say 'Установка отдельного Node.js для Codex…' 'Installing private Node.js for Codex…'
        jobs_agent_prepare_private_node || exit $?
    fi
    if ! command -v codex >/dev/null && ! jobs_agent_cli_binary codex >/dev/null; then
        [[ ! -e $root/0.163.0 ]] || { printf '%s\n' 'Existing unusable Codex runtime preserved.' >&2;exit 1; }
        jobs_agent_say 'Установка отдельного Codex CLI…' 'Installing private Codex CLI…'
        jobs_agent_download codex-cli "$stage/codex.tgz" 'https://registry.npmjs.org/@openai/codex/-/codex-0.163.0-linux-arm64.tgz' || exit $?
        printf '%s  %s\n' dc6e7c4d6de71c23a0f60d05e2577ff7eef615b76f22630d58ad384b84763522 "$stage/codex.tgz" | sha256sum -c - || exit $?
        mkdir "$stage/runtime" || exit $?;tar -xzf "$stage/codex.tgz" --no-same-owner --strip-components=1 -C "$stage/runtime" || exit $?
        [[ "$("$stage/runtime/vendor/aarch64-unknown-linux-musl/bin/codex" --version)" == 'codex-cli 0.163.0' ]] || exit 1
        [[ ! -e $root/0.163.0 ]] || { printf '%s\n' 'Existing unusable Codex runtime preserved.' >&2;exit 1; }
        mv "$stage/runtime" "$root/0.163.0" || exit $?
    fi
)
_jobs_codex_prepare || return
jobs_agent_cli_codex_path
jobs_agent_has_node && command -v codex >/dev/null || return 1
if declare -F jobs_agent_stage >/dev/null;then jobs_agent_stage checking-cli;fi
codex --version >/dev/null || return $?
if declare -F jobs_agent_stage >/dev/null;then jobs_agent_stage ready;fi
unset -f _jobs_codex_prepare
