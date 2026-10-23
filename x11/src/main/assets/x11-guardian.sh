#!/usr/bin/env bash
# Must be created with setsid. EOF of the service-owned pipe ends only this group.
set -uo pipefail
IFS= read -r stat < /proc/$$/stat || exit 2
rest=${stat##*) }; read -r state parent group remaining <<< "$rest"
[[ $group == "$$" ]] || { echo 'ERROR:PRIVATE_PROCESS_GROUP_REQUIRED';exit 2; }
cleanup() {
    trap '' HUP INT TERM
    trap - EXIT
    kill -TERM -- "-$$" 2>/dev/null || true
    sleep 1
    kill -KILL -- "-$$" 2>/dev/null || true
}
trap cleanup EXIT HUP INT TERM
"$@" </dev/null &
child=$!
while true;do
    # A timeout keeps checking child lifetime; EOF means the owner disappeared.
    read -r -t 0.25 message
    rc=$?
    if ((rc == 0)) || ((rc == 1));then break;fi
    if ! jobs -pr | { found=1;while read -r running;do [[ $running == "$child" ]] && found=0;done;exit "$found"; };then
        wait "$child";rc=$?;printf '\nDESKTOP_CHILD_EXIT=%d\n' "$rc";exit "$rc"
    fi
done
exit 0
