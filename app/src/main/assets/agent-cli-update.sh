#!/bin/bash
# Guest-side entry point. The caller selects and enters the Ubuntu AI root.
if [[ ${JOBS_AGENT_UBUNTU:-} != ubuntu-ai ]]; then
    printf 'ERROR:UNSUPPORTED_ENVIRONMENT\n'
    exit 1
fi
if ! command -v python3 >/dev/null; then
    printf 'ERROR:DEPENDENCIES\n'
    exit 1
fi
exec python3 -I "${BASH_SOURCE[0]%/*}/agent-cli-update.py" "$@" 2>/dev/null
