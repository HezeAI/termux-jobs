#!/bin/sh
# Resolve a version-specific path once per launch. The active link is never
# passed to a controller, so concurrent updates cannot switch its child CLI.
jobs_agent_cli_antigravity_pinned_version() {
    printf '%s\n' 1.5.13
}
jobs_agent_cli_antigravity_pinned_path() {
    printf '%s\n' "$HOME/.local/share/termux-jobs/antigravity-cli/$(jobs_agent_cli_antigravity_pinned_version)/agy"
}
jobs_agent_cli_managed() (
    [ "${JOBS_AGENT_UBUNTU:-}" = ubuntu-ai ] || return 1
    case "$1" in
        codex) suffix=vendor/aarch64-unknown-linux-musl/bin/codex;;
        claude) suffix=claude;;
        antigravity) suffix=agy;;
        *) return 1;;
    esac
    root="$HOME/.local/share/termux-jobs/cli-updates/$1"
    version=$(readlink -- "$root/current") || return 1
    printf '%s\n' "$version" | LC_ALL=C grep -Eq '^(0|[1-9][0-9]{0,8})\.(0|[1-9][0-9]{0,8})\.(0|[1-9][0-9]{0,8})$' || return 1
    # Reject multiline targets even when one line matches the version.
    case "$version" in *[!0-9.]*) return 1;; esac
    binary="$root/$version/$suffix"
    [ -d "$root/$version" ] && [ ! -L "$root/$version" ] &&
        [ -f "$binary" ] && [ ! -L "$binary" ] && [ -x "$binary" ] || return 1
    printf '%s\n' "$binary"
)
jobs_agent_cli_binary() (
    jobs_agent_cli_managed "$1" && return
    case "$1" in
        codex)
            binary=$(command -v codex) || binary=
            case "$binary" in
                /*) [ -x "$binary" ] && { printf '%s\n' "$binary"; return; };;
            esac
            binary="$HOME/.local/share/termux-jobs/codex-cli/0.163.0/vendor/aarch64-unknown-linux-musl/bin/codex";;
        claude)
            binary="$HOME/.local/share/termux-jobs/claude-code/2.1.296/claude";;
        antigravity)
            [ "${JOBS_AGENT_UBUNTU:-}" = ubuntu-ai ] || return 1
            binary=$(jobs_agent_cli_antigravity_pinned_path)
            [ -d "${binary%/*}" ] && [ ! -L "${binary%/*}" ] &&
                [ -f "$binary" ] && [ ! -L "$binary" ] || return 1;;
        *) return 1;;
    esac
    [ -x "$binary" ] && printf '%s\n' "$binary"
)
jobs_agent_cli_codex_path() {
    if ! command -v node >/dev/null 2>&1 || ! node -e 'process.exit(Number(process.versions.node.split(".")[0])>=20?0:1)' >/dev/null 2>&1; then
        export PATH="$HOME/.local/share/termux-jobs/agent-tools/node-v22.22.1-linux-arm64/bin:$PATH"
    fi
    # Keep the normal PATH resolution (including an interactive user's
    # configuration) when a managed version has not been activated.
    if _jobs_cli_binary=$(jobs_agent_cli_managed codex) ||
            { ! command -v codex >/dev/null 2>&1 && _jobs_cli_binary=$(jobs_agent_cli_binary codex); }; then
        _jobs_cli_dir=${_jobs_cli_binary%/*}
        export PATH="$_jobs_cli_dir:${_jobs_cli_dir%/*}/codex-path:$PATH"
    fi
    unset _jobs_cli_binary _jobs_cli_dir
}
