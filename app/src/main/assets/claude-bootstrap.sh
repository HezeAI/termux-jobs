#!/bin/bash
# Sourced only by a user-created Claude session in Ubuntu. No system installation.
_jobs_claude_prepare() (
    set -eu
    root="$HOME/.local/share/termux-jobs/claude-code"
    mkdir -p "$root"
    chmod 700 "$root"
    say() { if [ "${JOBS_CLAUDE_LANGUAGE:-en}" = ru ]; then printf '%s\n' "$1"; else printf '%s\n' "$2"; fi; }
    for tool in curl tar sha256sum flock; do
        command -v "$tool" >/dev/null || { say "Не найден компонент: $tool" "Missing component: $tool" >&2; exit 1; }
    done
    exec 9>"$root/install.lock"
    flock 9
    stage=$(mktemp -d "$root/.install-XXXXXXXX")
    trap 'rm -rf -- "$stage"' EXIT
    version=2.1.296
    binary="$root/$version/claude"
    binary_hash=f1f6e96e0d8342b9dbf41d7e88255397a6a52ce3d8736ad6a4c6b59c9b62fefa
    if [ ! -e "$root/$version" ]; then
        say 'Установка Claude Code…' 'Installing Claude Code…'
        curl --fail --location --proto '=https' --tlsv1.2 --retry 2 -o "$stage/claude.tgz" 'https://registry.npmjs.org/@anthropic-ai/claude-code-linux-arm64/-/claude-code-linux-arm64-2.1.296.tgz'
        printf '%s  %s\n' 56abbe1cfc85e5b37afeef5ae8b4856ddc32bbe443b82689a3b5f99992045cd1 "$stage/claude.tgz" | sha256sum -c - >/dev/null
        mkdir "$stage/runtime"
        tar -xzf "$stage/claude.tgz" --no-same-owner -C "$stage/runtime" --strip-components=1 package/claude
        printf '%s  %s\n' "$binary_hash" "$stage/runtime/claude" | sha256sum -c - >/dev/null
        chmod 700 "$stage/runtime/claude"
        [ "$(DISABLE_AUTOUPDATER=1 CLAUDE_CONFIG_DIR="$stage/probe" "$stage/runtime/claude" --version)" = '2.1.296 (Claude Code)' ]
        mv "$stage/runtime" "$root/$version"
    fi
    # Preserve any existing mismatched runtime; never silently overwrite it.
    printf '%s  %s\n' "$binary_hash" "$binary" | sha256sum -c - >/dev/null
    node=$(type -P node || true)
    if [ -z "$node" ] || ! "$node" -e 'process.exit(Number(process.versions.node.split(".")[0])>=20?0:1)' >/dev/null 2>&1; then
        node="$root/node-v22.22.1-linux-arm64/bin/node"
        if [ ! -e "$root/node-v22.22.1-linux-arm64" ]; then
            say 'Установка компонента управления Claude…' 'Installing the Claude session controller…'
            curl --fail --location --proto '=https' --tlsv1.2 --retry 2 -o "$stage/node.tar.xz" 'https://nodejs.org/dist/v22.22.1/node-v22.22.1-linux-arm64.tar.xz'
            printf '%s  %s\n' 0f3550d58d45e5d3cf7103d9e3f69937f09fe82fb5dd474c66a5d816fa58c9ee "$stage/node.tar.xz" | sha256sum -c - >/dev/null
            tar -xJf "$stage/node.tar.xz" --no-same-owner -C "$stage"
            mv "$stage/node-v22.22.1-linux-arm64" "$root/"
        fi
        "$node" -e 'process.exit(Number(process.versions.node.split(".")[0])>=20?0:1)'
    fi
    printf '%s\n' "$node" > "$root/controller-node"
    chmod 600 "$root/controller-node"
    say 'Claude Code готов. Авторизация выполняется в терминале.' 'Claude Code is ready. Sign in in the terminal.'
)
_jobs_claude_prepare
_jobs_claude_status=$?
[ "$_jobs_claude_status" -eq 0 ] || return "$_jobs_claude_status"
unset _jobs_claude_status
export JOBS_CLAUDE_BINARY="$HOME/.local/share/termux-jobs/claude-code/2.1.296/claude"
export JOBS_CLAUDE_NODE="$(cat "$HOME/.local/share/termux-jobs/claude-code/controller-node")"
unset -f _jobs_claude_prepare
