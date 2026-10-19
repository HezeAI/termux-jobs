#!/usr/bin/env bash
# Separate immutable frontend only; never replace official OpenCode or connection data.
jobs_mini_prepare() {
    local jobs_mini_root jobs_mini_stage jobs_mini_bundle
    jobs_mini_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
    jobs_mini_bundle="$jobs_mini_root/opencode-mini-v2.0.28-jobs2.bundle"
    exec 8>"$jobs_mini_root/.mini-install.lock"
    flock -w 300 8 || return 1
    if [[ -f "$jobs_mini_root/opencode-mini-v2.0.28-jobs2/opencode-mini" ]]; then
        printf '%s  %s\n' '2fe8edc95696c14fd4048720427e01d32fff29b31462b1ee54fcd4b4cf6275fb' "$jobs_mini_root/opencode-mini-v2.0.28-jobs2/opencode-mini" | sha256sum -c - >/dev/null || return 1
    else
        printf '%s  %s\n' 'f404aafb7d78aa9fae870d556b55a3484457d9444e71f5e31bcc4667ce920d0b' "$jobs_mini_bundle" | sha256sum -c - >/dev/null || return 1
        jobs_mini_stage=$(mktemp -d "$jobs_mini_root/.mini-jobs2.XXXXXXXX")
        trap "rm -rf -- $(printf '%q' "$jobs_mini_stage")" EXIT
        tar -xzf "$jobs_mini_bundle" -C "$jobs_mini_stage" || return 1
        printf '%s  %s\n' '2fe8edc95696c14fd4048720427e01d32fff29b31462b1ee54fcd4b4cf6275fb' "$jobs_mini_stage/opencode-mini" | sha256sum -c - >/dev/null || return 1
        chmod 700 "$jobs_mini_stage" "$jobs_mini_stage/opencode-mini"
        [[ ! -e "$jobs_mini_root/opencode-mini-v2.0.28-jobs2" ]] || return 1
        mv -- "$jobs_mini_stage" "$jobs_mini_root/opencode-mini-v2.0.28-jobs2"
        trap - EXIT
    fi
    flock -u 8;exec 8>&-
}
jobs_mini_prepare
