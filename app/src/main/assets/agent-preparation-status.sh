#!/usr/bin/env bash
# One small status per session, never per frame/request. No secrets or terminal text.
jobs_agent_stage() {
    local stage=$1 file=${JOBS_AGENT_PREPARATION_STATUS:-} temp
    case $stage in checking-ubuntu|preparing-termux-packages|verifying-components|installing-ubuntu|failed-ubuntu|checking-components|installing-components|checking-cli|failed-components|failed-cli|ready) ;; *) return 2;; esac
    printf 'PREPARATION:%s\n' "$stage"
    [[ -n $file ]] || return 0
    temp=$(mktemp "${file}.XXXXXXXX") || return 0
    if printf '%s\n' "$stage" > "$temp" && chmod 600 "$temp"; then mv -f -- "$temp" "$file" || rm -f -- "$temp"; else rm -f -- "$temp";fi
    return 0
}

jobs_agent_stage_failure() {
    local stage=''
    [[ -n ${JOBS_AGENT_PREPARATION_STATUS:-} ]] && read -r stage < "$JOBS_AGENT_PREPARATION_STATUS" || true
    case $stage in ready) return 0;; checking-cli) jobs_agent_stage failed-cli;; *) jobs_agent_stage failed-components;; esac
}
