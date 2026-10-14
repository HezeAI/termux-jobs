#!/usr/bin/env bash
# Private runtimes only. Invoked in the selected Ubuntu when the user launches OpenCode.
set -euo pipefail
jobs_ai_prepare() {
    local jobs_agents_root="${1:-$HOME/.local/share/termux-jobs/agents}"
    local jobs_node_name=node-v22.22.1-linux-arm64
    local jobs_node_sha=0f3550d58d45e5d3cf7103d9e3f69937f09fe82fb5dd474c66a5d816fa58c9ee
    local jobs_oc_sha=575c7049e5d94e61068c9e77b66f1c792773fb9a1f3950c6bcc758a5d9b11d88
    jobs_ai_status() { if [[ "${JOBS_AI_LANGUAGE:-en}" == ru ]]; then printf '%s\n' "$1"; else printf '%s\n' "$2"; fi; }
    jobs_ai_status 'Проверка компонентов OpenCode…' 'Checking OpenCode components…'
    [[ $(uname -m) == aarch64 || $(uname -m) == arm64 ]] || { printf '%s\n' 'OpenCode bootstrap supports Ubuntu ARM64.' >&2; return 1; }
    mkdir -p "$jobs_agents_root" "$jobs_agents_root/bin"
    chmod 700 "$jobs_agents_root"
    # A kernel-held lock releases even if installation is interrupted; never delete APT locks.
    local jobs_missing=()
    command -v flock >/dev/null || jobs_missing+=(util-linux)
    if ((${#jobs_missing[@]})); then apt-get update && apt-get install -y --no-install-recommends "${jobs_missing[@]}"; fi
    exec 9>"$jobs_agents_root/.install.lock"
    flock -w 300 9 || { printf '%s\n' 'OpenCode installation is busy; retry after it finishes.' >&2; return 1; }
    export PATH="$jobs_agents_root/bin:$jobs_agents_root/$jobs_node_name/bin:$jobs_agents_root/node_modules/.bin:$PATH"
    local jobs_need_node=0 jobs_need_oc=0
    node -e 'if(Number(process.versions.node.split(".")[0])<20)process.exit(1)' >/dev/null 2>&1 || jobs_need_node=1
    [[ "$(opencode --version 2>/dev/null || true)" == 'opencode v2.0.28' ]] || jobs_need_oc=1
    if ((jobs_need_node || jobs_need_oc)); then
        jobs_missing=()
        command -v curl >/dev/null || jobs_missing+=(curl ca-certificates)
        command -v tar >/dev/null || jobs_missing+=(tar)
        command -v sha256sum >/dev/null || jobs_missing+=(coreutils)
        if ((jobs_need_node)); then command -v xz >/dev/null || jobs_missing+=(xz-utils); fi
        if ((${#jobs_missing[@]})); then apt-get update && apt-get install -y --no-install-recommends "${jobs_missing[@]}"; fi
        local jobs_download_dir
        jobs_download_dir=$(mktemp -d "$jobs_agents_root/.bootstrap.XXXXXXXX")
        # Only this invocation's staging directory is removed.
        trap "rm -rf -- $(printf '%q' "$jobs_download_dir")" EXIT
        if ((jobs_need_node)); then
            jobs_ai_status 'Установка отдельного Node.js…' 'Installing a private Node.js runtime…'
            curl --fail --location --connect-timeout 20 --max-time 600 --retry 2 --output "$jobs_download_dir/node.tar.xz" "https://nodejs.org/dist/v22.22.1/$jobs_node_name.tar.xz"
            printf '%s  %s\n' "$jobs_node_sha" "$jobs_download_dir/node.tar.xz" | sha256sum -c -
            tar -xJf "$jobs_download_dir/node.tar.xz" -C "$jobs_download_dir"
            "$jobs_download_dir/$jobs_node_name/bin/node" -e 'if(Number(process.versions.node.split(".")[0])<20)process.exit(1)'
            [[ ! -e "$jobs_agents_root/$jobs_node_name" ]] || { printf '%s\n' 'Private Node directory already exists but is unusable; no files replaced.' >&2; return 1; }
            mv -- "$jobs_download_dir/$jobs_node_name" "$jobs_agents_root/$jobs_node_name"
            export PATH="$jobs_agents_root/$jobs_node_name/bin:$PATH"
        fi
        if ((jobs_need_oc)); then
            jobs_ai_status 'Установка OpenCode v2.0.28…' 'Installing OpenCode v2.0.28…'
            curl --fail --location --connect-timeout 20 --max-time 900 --retry 2 --output "$jobs_download_dir/opencode.tgz" 'https://registry.npmjs.org/@opencode/cli-linux-arm64/-/cli-linux-arm64-2.0.28.tgz'
            printf '%s  %s\n' "$jobs_oc_sha" "$jobs_download_dir/opencode.tgz" | sha256sum -c -
            tar -xzf "$jobs_download_dir/opencode.tgz" -C "$jobs_download_dir"
            [[ "$("$jobs_download_dir/package/bin/opencode" --version)" == 'opencode v2.0.28' ]]
            chmod 700 "$jobs_download_dir/package/bin/opencode"
            mv -f -- "$jobs_download_dir/package/bin/opencode" "$jobs_agents_root/bin/opencode"
        fi
        rm -rf -- "$jobs_download_dir"
        trap - EXIT
    fi
    node -e 'if(Number(process.versions.node.split(".")[0])<20)process.exit(1)'
    [[ "$(opencode --version)" == 'opencode v2.0.28' ]]
    flock -u 9;exec 9>&-
    jobs_ai_status 'Компоненты готовы. Запуск OpenCode…' 'Components ready. Starting OpenCode…'
}
jobs_ai_prepare "$@"
