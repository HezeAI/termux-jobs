#!/bin/bash
# Sourced only by a user-created Gemini session in Ubuntu. No system installation.
_jobs_gemini_prepare() (
    set -euo pipefail
    root="$HOME/.local/share/termux-jobs/gemini-cli"
    mkdir -p "$root"
    chmod 700 "$root"
    say() { if [ "${JOBS_GEMINI_LANGUAGE:-en}" = ru ]; then printf '%s\n' "$1"; else printf '%s\n' "$2"; fi; }
    for tool in curl tar sha256sum flock find sort xargs; do
        command -v "$tool" >/dev/null || { say "Не найден компонент: $tool" "Missing component: $tool" >&2; exit 1; }
    done
    exec 9>"$root/install.lock"
    flock 9
    stage=$(mktemp -d "$root/.install-XXXXXXXX")
    trap 'rm -rf -- "$stage"' EXIT
    version=0.64.0
    binary="$root/$version/bundle/gemini.js"
    if [ ! -e "$root/$version" ]; then
        say 'Установка Gemini CLI…' 'Installing Gemini CLI…'
        curl --fail --location --proto '=https' --tlsv1.2 --retry 2 -o "$stage/gemini.tgz" 'https://registry.npmjs.org/@google/gemini-cli/-/gemini-cli-0.64.0.tgz'
        printf '%s  %s\n' 891a794e55c8319b6b78f6f8f13d6b8b2782a4709dfd2fcabf3a1f63ae8365e6 "$stage/gemini.tgz" | sha256sum -c - >/dev/null
        mkdir "$stage/runtime"
        tar -xzf "$stage/gemini.tgz" --no-same-owner -C "$stage/runtime" --strip-components=1
        ( cd "$stage/runtime"; find bundle -type f -print0 | sort -z | xargs -0 sha256sum > .integrity )
        chmod -R go-rwx "$stage/runtime"
        mv "$stage/runtime" "$root/$version"
    fi
    # Check all bundle chunks; preserve a mismatched existing installation.
    ( cd "$root/$version"; sha256sum -c .integrity >/dev/null )
    printf '%s  %s\n' ab113d84c8f1692fd6c95081366718897876d8664328c7bf0ccf9437d688145f "$binary" | sha256sum -c - >/dev/null
    node=""
    if [ -z "$node" ] || ! "$node" -e 'process.exit(Number(process.versions.node.split(".")[0])>=20?0:1)' >/dev/null 2>&1; then
        node="$root/node-v22.22.1-linux-arm64/bin/node"
        if [ ! -e "$root/node-v22.22.1-linux-arm64" ]; then
            say 'Установка компонента управления Gemini…' 'Installing the Gemini session controller…'
            curl --fail --location --proto '=https' --tlsv1.2 --retry 2 -o "$stage/node.tar.xz" 'https://nodejs.org/dist/v22.22.1/node-v22.22.1-linux-arm64.tar.xz'
            printf '%s  %s\n' 0f3550d58d45e5d3cf7103d9e3f69937f09fe82fb5dd474c66a5d816fa58c9ee "$stage/node.tar.xz" | sha256sum -c - >/dev/null
            tar -xJf "$stage/node.tar.xz" --no-same-owner -C "$stage"
            mv "$stage/node-v22.22.1-linux-arm64" "$root/"
        fi
        "$node" -e 'process.exit(Number(process.versions.node.split(".")[0])>=20?0:1)'
    fi
    [ "$(GEMINI_CLI_HOME="$stage/probe" GEMINI_CLI_NO_RELAUNCH=1 "$node" "$binary" --version)" = "0.64.0" ]
    printf '%s\n' "$node" > "$root/controller-node"
    chmod 600 "$root/controller-node"
    say 'Gemini CLI готов. Авторизация выполняется в терминале.' 'Gemini CLI is ready. Sign in in the terminal.'
)
_jobs_gemini_prepare
_jobs_gemini_status=$?
[ "$_jobs_gemini_status" -eq 0 ] || return "$_jobs_gemini_status"
unset _jobs_gemini_status
export JOBS_GEMINI_BINARY="$HOME/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/gemini.js"
export JOBS_GEMINI_NODE="$(cat "$HOME/.local/share/termux-jobs/gemini-cli/controller-node")"
unset -f _jobs_gemini_prepare
