#!/usr/bin/env bash
# Private missing-only Codex/Node. Existing global tools and accounts are never replaced.
_jobs_codex_prepare() (
    set -euo pipefail
    local root="$HOME/.local/share/termux-jobs/codex-cli" tools="$HOME/.local/share/termux-jobs/agent-tools" stage
    mkdir -p "$root" "$tools" || exit $?;chmod 700 "$root" "$tools" || exit $?
    exec 8>"$root/install.lock" || exit $?;flock -w 300 8 || exit $?
    stage=$(mktemp -d "$root/.install-XXXXXXXX") || exit $?;trap 'rm -rf -- "$stage"' EXIT || exit $?
    if ! jobs_agent_has_node && [[ ! -x $(jobs_agent_node_path) ]]; then
        [[ ! -e $tools/node-v22.22.1-linux-arm64 ]] || { printf '%s\n' 'Existing unusable Node runtime preserved.' >&2;exit 1; }
        jobs_agent_say 'Установка отдельного Node.js для Codex…' 'Installing private Node.js for Codex…'
        curl --fail --location --proto '=https' --tlsv1.2 --connect-timeout 20 --max-time 900 --retry 2 -o "$stage/node.tar.xz" 'https://nodejs.org/dist/v22.22.1/node-v22.22.1-linux-arm64.tar.xz' || exit $?
        printf '%s  %s\n' 0f3550d58d45e5d3cf7103d9e3f69937f09fe82fb5dd474c66a5d816fa58c9ee "$stage/node.tar.xz" | sha256sum -c - || exit $?
        tar -xJf "$stage/node.tar.xz" --no-same-owner -C "$stage" || exit $?
        [[ ! -e $tools/node-v22.22.1-linux-arm64 ]] || { printf '%s\n' 'Existing unusable Node runtime preserved.' >&2;exit 1; }
        mv "$stage/node-v22.22.1-linux-arm64" "$tools/" || exit $?
    fi
    if ! command -v codex >/dev/null && [[ ! -x $root/0.163.0/vendor/aarch64-unknown-linux-musl/bin/codex ]]; then
        [[ ! -e $root/0.163.0 ]] || { printf '%s\n' 'Existing unusable Codex runtime preserved.' >&2;exit 1; }
        jobs_agent_say 'Установка отдельного Codex CLI…' 'Installing private Codex CLI…'
        curl --fail --location --proto '=https' --tlsv1.2 --connect-timeout 20 --max-time 1200 --retry 2 -o "$stage/codex.tgz" 'https://registry.npmjs.org/@openai/codex/-/codex-0.163.0-linux-arm64.tgz' || exit $?
        printf '%s  %s\n' dc6e7c4d6de71c23a0f60d05e2577ff7eef615b76f22630d58ad384b84763522 "$stage/codex.tgz" | sha256sum -c - || exit $?
        mkdir "$stage/runtime" || exit $?;tar -xzf "$stage/codex.tgz" --no-same-owner --strip-components=1 -C "$stage/runtime" || exit $?
        [[ "$("$stage/runtime/vendor/aarch64-unknown-linux-musl/bin/codex" --version)" == 'codex-cli 0.163.0' ]] || exit 1
        [[ ! -e $root/0.163.0 ]] || { printf '%s\n' 'Existing unusable Codex runtime preserved.' >&2;exit 1; }
        mv "$stage/runtime" "$root/0.163.0" || exit $?
    fi
)
_jobs_codex_prepare || return
if ! jobs_agent_has_node; then export PATH="$HOME/.local/share/termux-jobs/agent-tools/node-v22.22.1-linux-arm64/bin:$PATH";fi
if ! command -v codex >/dev/null; then export PATH="$HOME/.local/share/termux-jobs/codex-cli/0.163.0/vendor/aarch64-unknown-linux-musl/bin:$HOME/.local/share/termux-jobs/codex-cli/0.163.0/vendor/aarch64-unknown-linux-musl/codex-path:$PATH";fi
jobs_agent_has_node && command -v codex >/dev/null || return 1
unset -f _jobs_codex_prepare
