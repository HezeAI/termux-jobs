#!/data/data/com.termux/files/usr/bin/bash
# Prepared only. Activate manually in ~/.termux/boot after reviewing README.txt.
# Do not start this script from inside Ubuntu/proot.
set -eu
jobs_prefix=/data/data/com.termux/files/usr
jobs_native_repo="$jobs_prefix/var/lib/proot-distro/containers/ubuntu-ai/rootfs/root/termux-fork"
jobs_client=/root/termux-fork/scripts/codex-geohide/client.py
test -f "$jobs_native_repo/scripts/codex-geohide/client.py" || {
  echo 'Codex tunnel: Ubuntu AI repository is missing' >&2
  exit 1
}
# Run the existing Ubuntu AI, not a second rootfs. The manager has a singleton lock.
# No wake-lock, global proxy variables, shell profile changes, or authentication files.
nohup "$jobs_prefix/bin/proot-distro" login ubuntu-ai -- \
  /usr/bin/python3 -B "$jobs_client" _daemon \
  </dev/null >>"$jobs_native_repo/.tools/codex-geohide/state/boot.log" 2>&1 &
