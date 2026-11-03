#!/usr/bin/env bash
# Synchronous managed archive transfer. Callers retain SHA verification and staging.
# Three native curl deadlines plus fixed pauses bound the whole retry sequence.
jobs_agent_download() {
    local component=$1 target=$2 url=$3
    local connect=45 idle=180 budget=7200 attempts=3 delay=2
    local attempt code=1 pause
    case $component in codex-cli|claude-cli|gemini-cli|opencode-cli|node|gcloud) ;; *) printf 'ERROR:COMPONENT_DOWNLOAD_FAILED\n' >&2;return 1;; esac
    [[ -n $target && $url == https://* ]] || { printf 'ERROR:COMPONENT_DOWNLOAD_FAILED\n' >&2;return 1; }
    for ((attempt=1;attempt<=attempts;attempt++)); do
        if ((attempt==1)); then printf 'STAGE:download-agent-component\n'; else printf 'STAGE:retry-agent-component\n';fi
        printf 'COMPONENT:%s\n' "$component"
        # No built-in retry/Retry-After delay: the loop owns the finite total budget.
        # Do not background curl or abandon it while it can still write the archive.
        if { curl --disable --silent --fail --location --proto '=https' --proto-redir '=https' --tlsv1.2 \
            --connect-timeout "$connect" --speed-limit 1 --speed-time "$idle" \
            --max-time "$budget" --retry 0 --output "$target" "$url"; } 2>/dev/null; then
            return 0
        else code=$?;fi
        # Retry transient network/HTTP failures only. TLS trust and local I/O fail closed.
        case $code in 5|6|7|18|22|28|35|52|55|56|92) ;; *) break;; esac
        ((attempt<attempts)) || break
        pause=$((delay*attempt))
        if sleep "$pause";then :;else code=$?;break;fi
    done
    case $code in
        28) printf 'ERROR:COMPONENT_DOWNLOAD_TIMEOUT\n' >&2;;
        130|143) printf 'ERROR:COMPONENT_DOWNLOAD_INTERRUPTED\n' >&2;;
        *) printf 'ERROR:COMPONENT_DOWNLOAD_FAILED\n' >&2;;
    esac
    return "$code"
}
