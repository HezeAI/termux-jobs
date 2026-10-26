# Termux Jobs managed aliases. Run in the selected environment, never source user rc here.
set -eu
umask 077
fail() { printf '%s\n' "$1"; exit 1; }
[ -n "${HOME:-}" ] && [ -d "$HOME" ] || fail ALIAS_HOME_UNAVAILABLE
cd -P -- "$HOME" || fail ALIAS_HOME_UNAVAILABLE
jobs_home=$PWD
jobs_dir=$jobs_home/.termux-jobs-aliases
jobs_action=$1 jobs_id=$2 jobs_name=${3:-} jobs_value=${4:-} jobs_replace=${5:-}
case "$jobs_id" in *[!a-f0-9-]*|'') fail ALIAS_INVALID_ID;; esac
[ "${#jobs_id}" -eq 36 ] || fail ALIAS_INVALID_ID
case "$jobs_replace" in *[!a-f0-9-]*) fail ALIAS_INVALID_ID;; esac
case "$jobs_action" in save|delete) ;; *) fail ALIAS_INVALID_ACTION;; esac
[ ! -L "$jobs_dir" ] || fail ALIAS_SYMLINK
mkdir -p -- "$jobs_dir" || fail ALIAS_DIRECTORY_FAILED
[ -d "$jobs_dir" ] && [ ! -L "$jobs_dir" ] || fail ALIAS_SYMLINK
mkdir -- "$jobs_dir/.lock" 2>/dev/null || fail ALIAS_BUSY
jobs_tmp=
cleanup() { [ -z "$jobs_tmp" ] || rm -f -- "$jobs_tmp"; rmdir -- "$jobs_dir/.lock" 2>/dev/null || :; }
trap cleanup EXIT
trap 'exit 1' HUP INT TERM
jobs_target=$jobs_dir/$jobs_id.sh
managed() {
    [ ! -L "$1" ] || fail ALIAS_SYMLINK
    if [ -e "$1" ]; then
        [ -f "$1" ] || fail ALIAS_NOT_REGULAR
        IFS= read -r jobs_header < "$1" || fail ALIAS_UNMANAGED_FILE
        [ "$jobs_header" = "# Termux Jobs alias v1 $2" ] || fail ALIAS_UNMANAGED_FILE
    fi
}
managed "$jobs_target" "$jobs_id"
if [ "$jobs_action" = delete ]; then
    rm -f -- "$jobs_target" || fail ALIAS_DELETE_FAILED
    printf '%s\n' ALIAS_OK
    exit 0
fi
case "$jobs_name" in ''|[!A-Za-z_]*|*[!A-Za-z0-9_-]*) fail ALIAS_INVALID_NAME;; esac
if [ -n "$jobs_replace" ] && [ "$jobs_replace" != "$jobs_id" ]; then
    [ "${#jobs_replace}" -eq 36 ] || fail ALIAS_INVALID_ID
    managed "$jobs_dir/$jobs_replace.sh" "$jobs_replace"
fi
# Check all startup targets before writing any. Never follow user-config symlinks.
for jobs_rc in .bashrc .zshrc .profile .bash_profile; do
    jobs_path=$jobs_home/$jobs_rc
    [ ! -L "$jobs_path" ] || fail ALIAS_RC_SYMLINK
    [ ! -e "$jobs_path" ] || [ -f "$jobs_path" ] || fail ALIAS_RC_NOT_REGULAR
    [ ! -f "$jobs_path" ] || [ "$(wc -c < "$jobs_path")" -le 1048576 ] || fail ALIAS_RC_TOO_LARGE
done
# Keep one inert file so shells with strict unmatched-glob handling (Zsh) can
# load an empty collection after the final alias is deleted.
jobs_loader=$jobs_dir/_loader.sh
[ ! -L "$jobs_loader" ] || fail ALIAS_SYMLINK
if [ -e "$jobs_loader" ]; then
    [ -f "$jobs_loader" ] || fail ALIAS_NOT_REGULAR
    IFS= read -r jobs_header < "$jobs_loader" || fail ALIAS_UNMANAGED_FILE
    [ "$jobs_header" = '# Termux Jobs aliases loader v1' ] || fail ALIAS_UNMANAGED_FILE
else
    jobs_tmp=$(mktemp "$jobs_dir/.loader.XXXXXXXX") || fail ALIAS_WRITE_FAILED
    printf '%s\n' '# Termux Jobs aliases loader v1' > "$jobs_tmp"
    mv -f -- "$jobs_tmp" "$jobs_loader" || fail ALIAS_WRITE_FAILED
    jobs_tmp=
fi
for jobs_rc in .bashrc .zshrc .profile .bash_profile; do
    jobs_path=$jobs_home/$jobs_rc
    # Do not create a Bash login profile that would mask an existing .profile.
    [ "$jobs_rc" != .bash_profile ] || [ -e "$jobs_path" ] || continue
    if [ -f "$jobs_path" ] && grep -Fqx '# Termux Jobs aliases v1' "$jobs_path"; then continue; fi
    jobs_tmp=$(mktemp "$jobs_dir/.rc.XXXXXXXX") || fail ALIAS_WRITE_FAILED
    if [ -f "$jobs_path" ]; then cat -- "$jobs_path" > "$jobs_tmp" || fail ALIAS_READ_FAILED; fi
    cat >> "$jobs_tmp" <<'HOOK'

# Termux Jobs aliases v1
if [ ! -L "$HOME/.termux-jobs-aliases" ] && [ -f "$HOME/.termux-jobs-aliases/_loader.sh" ] && [ ! -L "$HOME/.termux-jobs-aliases/_loader.sh" ]; then
for _termux_jobs_alias in "$HOME"/.termux-jobs-aliases/*.sh; do
    if [ -f "$_termux_jobs_alias" ] && [ ! -L "$_termux_jobs_alias" ]; then . "$_termux_jobs_alias"; fi
done
\unset _termux_jobs_alias
fi
HOOK
    [ ! -L "$jobs_path" ] || fail ALIAS_RC_SYMLINK
    mv -f -- "$jobs_tmp" "$jobs_path" || fail ALIAS_WRITE_FAILED
    jobs_tmp=
done
jobs_tmp=$(mktemp "$jobs_dir/.alias.XXXXXXXX") || fail ALIAS_WRITE_FAILED
printf '# Termux Jobs alias v1 %s\n' "$jobs_id" > "$jobs_tmp"
# Shell single-quote escaping; the alias body is data, never evaluated on save.
printf '%s' '\alias ' >> "$jobs_tmp"
printf "'" >> "$jobs_tmp"
printf '%s=%s' "$jobs_name" "$jobs_value" | sed "s/'/'\\\\''/g" >> "$jobs_tmp"
printf "'\n" >> "$jobs_tmp"
managed "$jobs_target" "$jobs_id"
mv -f -- "$jobs_tmp" "$jobs_target" || fail ALIAS_WRITE_FAILED
jobs_tmp=
if [ -n "$jobs_replace" ] && [ "$jobs_replace" != "$jobs_id" ]; then
    rm -f -- "$jobs_dir/$jobs_replace.sh" || fail ALIAS_DELETE_FAILED
fi
printf '%s\n' ALIAS_OK
