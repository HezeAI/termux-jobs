#!/usr/bin/env bash
# Allowlisted APT programs, installed only after an explicit UI confirmation.
set -euo pipefail
op=${1:-}; resume=0
if [[ $op == resume ]];then op=install;resume=1;fi
target=${2:-}; control=${3:-}; program=${4:-}; language=${5:-en}
[[ $op == check || $op == install || $op == catalog || $op == catalog-install || $op == installed || $op == remove || $op == firefox-compat-on || $op == firefox-compat-off || $op == firefox-dns-on || $op == firefox-dns-off ]] || exit 2
case $target in ubuntu|jobs-x11-ubuntu|debian|alpine|termux|fedora|opensuse|void|archlinux) ;; *) echo 'ERROR:UNSUPPORTED_DESKTOP';exit 2;;esac
case $program in ''|files|terminal|editor|archives|images|fonts|browser|office|pdf|media|gpu-tools|gpu-test|catalog-tools) ;; pkg:*) [[ ${program#pkg:} =~ ^[A-Za-z0-9][A-Za-z0-9+_.-]{0,127}$ ]] || exit 2;; *) echo 'ERROR:UNKNOWN_PROGRAM';exit 2;;esac
[[ $op != install || -n $program ]] || exit 2
[[ $op != remove || $program == pkg:* ]] || exit 2
case $language in ru|en) ;; *) exit 2;;esac
[[ $op != catalog-install || $program == catalog-tools ]] || exit 2
: "${PREFIX:?}"
case $target in termux) expected="${PREFIX%/usr}/workspace-x11";; *) expected="${PREFIX%/usr}/workspace-x11-$target";;esac
# Exact old private control directory remains readable for upgrade/fixture compatibility.
legacy="${PREFIX%/usr}/workspace-x11"
if [[ -d $legacy && $control -ef $legacy ]];then expected=$legacy;fi
[[ -d $control && -d $expected && $control -ef $expected ]] || { echo 'ERROR:CONTROL_PATH';exit 2; }
if [[ $target == termux ]];then
    export JOBS_X11_NATIVE_TARGET=termux JOBS_X11_LANGUAGE=$language JOBS_X11_PROGRAM_RESUME=$resume
    exec python3 "$control/x11-native-programs.py" "$op" "$program" 3>&1
fi
fs_root="$PREFIX/var/lib/proot-distro"
for candidate in "$fs_root/containers/$target" "$fs_root/containers/$target/rootfs" "$fs_root/installed-rootfs/$target";do
    [[ ! -L $candidate ]] || { echo 'ERROR:ENVIRONMENT_IS_A_LINK';exit 2; }
done
[[ -d $fs_root/containers/$target/rootfs || -d $fs_root/installed-rootfs/$target ]] || { echo 'ERROR:INSTALL_DESKTOP_FIRST';exit 1; }
command -v proot-distro >/dev/null || { echo 'ERROR:PROOT_DISTRO_REQUIRED';exit 1; }
# The guest receives only the fixed code below and an allowlisted identifier.
proot_entry=(login)
if [[ $target == void ]];then proot_entry=(run --work-dir /root);fi
proot-distro "${proot_entry[@]}" --bind "$PREFIX/tmp:/tmp" --bind "$control:/tmp/termux-jobs-x11" "$target" -- /bin/sh -s -- "$op" "$program" "$language" "$target" "$resume" <<'GUEST'
set -eu
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export XDG_DATA_DIRS=/usr/local/share:/usr/share XDG_CONFIG_DIRS=/etc/xdg; unset LD_LIBRARY_PATH LD_PRELOAD DBUS_SYSTEM_BUS_ADDRESS;
export LC_ALL=C
op=$1; program=$2; language=$3; target=$4
export JOBS_X11_PROGRAM_RESUME=$5
export JOBS_X11_NATIVE_TARGET=$target
if [ "$target" = alpine ] || [ "$target" = fedora ] || [ "$target" = opensuse ] || [ "$target" = void ] || [ "$target" = archlinux ];then
 export JOBS_X11_NATIVE_TARGET=$target JOBS_X11_LANGUAGE=$language
 exec python3 /tmp/termux-jobs-x11/x11-native-programs.py "$op" "$program" 3>&1
fi
packages() {
 case $1 in
 files) echo thunar;;terminal) echo xfce4-terminal;;editor) echo mousepad;;
 archives) echo 'xarchiver unzip zip 7zip';;images) echo 'ristretto tumbler';;
 fonts) echo 'fonts-dejavu-core fonts-liberation adwaita-icon-theme';;
 gpu-tools) echo 'glmark2-x11 mesa-utils';;
 gpu-test) echo 'python3 libx11-6 libgl1 libglx-mesa0';;
 browser) echo firefox;;office) echo 'libreoffice-writer libreoffice-calc libreoffice-impress libreoffice-gtk3';;
 media|pkg:parole) echo 'parole gstreamer1.0-libav gstreamer1.0-plugins-good gstreamer1.0-plugins-bad gstreamer1.0-pulseaudio gstreamer1.0-x';;
 pdf) echo evince;;catalog-tools) echo 'appstream python3-gi gir1.2-appstream-1.0 python3-apt';;pkg:ristretto) echo 'ristretto tumbler';;pkg:*) printf '%s\n' "${1#pkg:}";;*) exit 2;;esac
}
# Require configured packages. p7zip-full is virtual in newer Ubuntu; inspect
# the real 7zip provider, accepting an already-installed legacy p7zip-full too.
# Selection (install/hold/deinstall) is independent of the actual configured state.
configured() { case "$(dpkg-query -W -f='${Status}' "$1" 2>/dev/null)" in *' ok installed') return 0;; *) return 1;; esac; }
ready() {
 if [ "$1" = browser ];then python3 /tmp/termux-jobs-x11/x11-firefox.py --check;return;fi
 for pkg in $(packages "$1");do
  if [ "$pkg" = 7zip ];then configured 7zip || configured p7zip-full || return 1
  else configured "$pkg" || return 1;fi
 done
 if [ "$1" = gpu-test ];then python3 -B /tmp/termux-jobs-x11/x11-gpu-test-setup.py check "$target" || return 1;fi
 if [ "$1" = gpu-tools ];then command -v glxinfo >/dev/null && command -v glmark2 >/dev/null || return 1;fi
}
if [ "$op" = firefox-compat-on ] || [ "$op" = firefox-compat-off ];then
 case $target in ubuntu|jobs-x11-ubuntu|debian) ;; *) echo ERROR:COMPAT_TEST_ONLY;exit 2;;esac
 JOBS_X11_MAINTENANCE=1 python3 /tmp/termux-jobs-x11/x11-maintenance.py "$op"
fi
if [ "$op" = firefox-dns-on ];then command -v firefox >/dev/null || { echo ERROR:FIREFOX_NOT_INSTALLED;exit 1; };fi
if [ "$op" = firefox-dns-on ] || [ "$op" = firefox-dns-off ];then
 JOBS_X11_MAINTENANCE=1 python3 /tmp/termux-jobs-x11/x11-firefox-dns.py "$op"
fi
# Profile-only status, never installs packages or changes DNS during a check.
JOBS_X11_MAINTENANCE=1 python3 /tmp/termux-jobs-x11/x11-firefox-dns.py state || echo FIREFOX_DNS:unknown
if [ "$op" = installed ];then
 command -v python3 >/dev/null || { echo ERROR:PYTHON3_REQUIRED;exit 3; }
 exec python3 /tmp/termux-jobs-x11/x11-catalog.py "$language" installed
fi
if [ "$op" = remove ] && [ "$program" = pkg:termux-jobs-gpu-test ];then
 python3 -B /tmp/termux-jobs-x11/x11-gpu-test-setup.py remove "$target"
 op=check
fi
if [ "$op" = remove ];then
 pkg=${program#pkg:}
 case $pkg in xfce4|xfce4-*|xfdesktop4|xfdesktop4-*|xfwm4|xfwm4-*|thunar|thunar-*|xfconf|libxfconf-*|fcitx5|fcitx5-*|dbus|dbus-*|libpulse0|gstreamer1.0-pulseaudio|xauth|x11-*|fonts-*|adwaita-icon-theme|hicolor-icon-theme|python3|python3-*|appstream|gir1.2-appstream-*) echo ERROR:PROTECTED_DESKTOP_COMPONENT;exit 2;;esac
 # Dependency checking remains enabled. No other package is removed implicitly,
 # and retained dependencies/configuration can be reused after reinstallation.
 dpkg --remove -- "$pkg"
 configured "$pkg" && { echo ERROR:PROGRAM_STILL_INSTALLED;exit 1; }
fi
needs_install=1
if [ "$op" = catalog-install ] && ready catalog-tools;then needs_install=0;fi
if [ "$op" = install ] || { [ "$op" = catalog-install ] && [ "$needs_install" = 1 ]; };then
 export DEBIAN_FRONTEND=noninteractive
 gpu_test_local=0
 if [ "$program" = gpu-test ];then
  gpu_test_local=1
  for p in $(packages gpu-test);do configured "$p" || gpu_test_local=0;done
 fi
 if [ "$gpu_test_local" = 1 ];then
  echo JOBS_APT_PHASE:install
 elif [ "$program" = browser ];then
  python3 /tmp/termux-jobs-x11/x11-firefox.py --install 3>&1
 else
 if [ "$JOBS_X11_PROGRAM_RESUME" != 1 ];then
 echo JOBS_APT_PHASE:update
 apt-get -o APT::Keep-Downloaded-Packages=true -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 update 3>&1
 fi
 echo JOBS_PROGRAM_INDEX_READY
 echo JOBS_APT_PHASE:install
 # Ordinary groups use distro repositories. Firefox uses the explicit Mozilla flow above.
 # No removals, host upgrade or sandbox-disabling flags.
 apt-get -o APT::Keep-Downloaded-Packages=true -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 install -y --no-remove --no-install-recommends $(packages "$program") 3>&1
 fi
 if [ "$program" = gpu-test ];then python3 -B /tmp/termux-jobs-x11/x11-gpu-test-setup.py prepare "$target";fi
 ready "$program" || { echo 'ERROR:PROGRAM_NOT_CONFIGURED';exit 1; }
 if [ "$program" = gpu-tools ];then python3 -B /tmp/termux-jobs-x11/x11-gpu-tools.py prepare "$target";fi
fi
if [ "$op" = catalog-install ];then
 # AppStream's distro APT configuration enables authenticated DEP-11 indices.
 echo JOBS_APT_PHASE:update
 apt-get -o APT::Keep-Downloaded-Packages=true -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Debug::acquire::progress=true -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 update 3>&1
fi
if [ "$op" = catalog ] || [ "$op" = catalog-install ];then
 ready catalog-tools || { echo ERROR:CATALOG_TOOLS_REQUIRED;exit 3; }
 exec /usr/bin/python3 /tmp/termux-jobs-x11/x11-catalog.py "$language"
fi
for id in files terminal editor archives images fonts browser office pdf media gpu-tools gpu-test;do
 if ready "$id";then echo "PROGRAM:$id:installed";else echo "PROGRAM:$id:missing";fi
done
echo READY:programs
GUEST
