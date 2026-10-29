#!/bin/sh
# Install hash-verified local packages; preserve newer user-installed versions.
set -eu
export LC_ALL=C
base=${1:?};target=${2:?}
case $target in termux|ubuntu|debian|kali|alpine|fedora|opensuse|void|archlinux|manjaro) ;;*)echo ERROR:BASE_INSTALL_TARGET;exit 2;;esac
[ -d "$base" ] && [ ! -L "$base" ] || { echo ERROR:BASE_INSTALL_DIRECTORY;exit 2; }
manifest="${base%/packages}/versions.tsv"
[ -f "$manifest" ] && [ ! -L "$manifest" ] || { echo ERROR:BASE_INSTALL_MANIFEST;exit 2; }
# Refuse unsafe cache entries before querying a manager. Never follow links.
for file in "$base"/*;do [ -f "$file" ] && [ ! -L "$file" ] || { echo ERROR:BASE_INSTALL_FILE;exit 2; };done
# Read each database once. Reopening a large pacman/dpkg/RPM database for every
# missing package is especially expensive under PRoot on a fresh installation.
installed=
case $target in
 termux|ubuntu|debian|kali)architecture=$(dpkg --print-architecture);installed=$(dpkg-query -W -f='${Package}\t${Architecture}\t${Version}\n');;
 archlinux|manjaro)installed=$(pacman -Q);;
 fedora|opensuse)installed=$(rpm -qa --queryformat '%{NAME}\t%{EPOCHNUM}:%{VERSION}-%{RELEASE}\n');;
esac
newline='
'
tab=$(printf '\t')
installed_version(){
 current=
 separator=$tab
 case $target in archlinux|manjaro)separator=' ';;esac
 needle="$newline$name$separator"
 table="$newline$installed$newline"
 case $target in
  termux|ubuntu|debian|kali)
   needle="$newline$name$tab$architecture$tab"
   case $table in *"$needle"*) ;;*)needle="$newline$name${tab}all$tab";;esac;;
 esac
 case $table in
  *"$needle"*)current=${table#*"$needle"};current=${current%%"$newline"*};;
 esac
}
# dpkg journals can remain after an interrupted unpack/configuration. Recover
# only packages from this verified set, never configure unrelated pending work,
# remove journal files, delete locks or force a downgrade. Missing dependencies
# may make this first configuration fail; the local APT transaction then supplies
# the remaining verified packages and reports any persistent failure normally.
recover_apt(){
 database=${DPKG_ADMINDIR:-/var/lib/dpkg}
 if [ "$target" = termux ];then database=${DPKG_ADMINDIR:-${PREFIX:?}/var/lib/dpkg};fi
 pending=0
 for update in "$database"/updates/[0-9]*;do
  if [ -f "$update" ] && [ ! -L "$update" ];then pending=1;break;fi
 done
 [ "$pending" = 1 ] || return 0
 installed=$(dpkg-query -W -f='${Package}\t${Architecture}\t${db:Status-Status}\n')
 set --
 fallback=
 while IFS="$tab" read -r name pin filename;do
  [ -n "$name" ] || continue
  installed_version
  if [ -n "$current" ] && [ -z "$fallback" ];then fallback=$name;fi
  case $current in unpacked|half-configured|half-installed|triggers-awaited|triggers-pending)set -- "$@" "$name";;esac
 done < "$manifest"
 # Even an already configured package permits dpkg to consolidate its journal
 # before APT can reopen it. dpkg may report "already configured"; do not hide it.
 if [ "$#" = 0 ] && [ -n "$fallback" ];then set -- "$fallback";fi
 [ "$#" -gt 0 ] || { echo ERROR:BASE_APT_RECOVERY_REQUIRED;return 1; }
 echo BASE_STAGE:recovering-apt
 if dpkg --force-confold --status-fd=3 --configure -- "$@" 3>&1;then :
 else echo WARNING:BASE_APT_CONFIGURATION_PENDING;fi
}
echo "BASE_STAGE:checking:$target"
set --
count=0
while IFS="$(printf '\t')" read -r name pin filename;do
 [ -n "$name" ] || continue
 case $name in *[!A-Za-z0-9+_.-]*|'')echo ERROR:BASE_PACKAGE_NAME;exit 2;;esac
 case $pin in *[!A-Za-z0-9+_.:~^=-]*|'')echo ERROR:BASE_PACKAGE_VERSION;exit 2;;esac
 case $filename in *[!A-Za-z0-9._+:%~^=-]*|'')echo ERROR:BASE_PACKAGE_FILE;exit 2;;esac
 file="$base/$filename"
 [ -f "$file" ] && [ ! -L "$file" ] || { echo ERROR:BASE_INSTALL_FILE;exit 2; }
 case $target:$filename in
  termux:*.deb|ubuntu:*.deb|debian:*.deb|kali:*.deb|alpine:*.apk|fedora:*.rpm|opensuse:*.rpm|void:*.xbps|archlinux:*.pkg.tar.xz|archlinux:*.pkg.tar.zst|manjaro:*.pkg.tar.xz|manjaro:*.pkg.tar.zst) ;;
  *)echo ERROR:BASE_PACKAGE_FORMAT;exit 2;;
 esac
 count=$((count+1));newer=0
 case $target in
  termux|ubuntu|debian|kali)
   installed_version
   if [ -n "$current" ] && dpkg --compare-versions "$current" gt "$pin";then newer=1;fi;;
  archlinux|manjaro)
   installed_version
   if [ -n "$current" ] && [ "$(vercmp "$current" "$pin")" -gt 0 ];then newer=1;fi;;
  alpine)
   current=$(apk list --installed "$name" 2>/dev/null | head -n 1);current=${current%% *};current=${current#"$name"-}
   if [ -n "$current" ] && [ "$(apk version -t "$current" "$pin")" = '>' ];then newer=1;fi;;
  fedora|opensuse)
   installed_version
   case $current in *[!A-Za-z0-9+_.:~^=-]*)echo ERROR:BASE_RPM_VERSION;exit 1;;esac
   if [ -n "$current" ] && [ "$(rpm --eval "%{lua:print(rpm.vercmp(\"$current\",\"$pin\"))}")" -gt 0 ];then newer=1;fi;;
  void)
   current=$(xbps-query -p pkgver "$name" 2>/dev/null) || current=
   current=${current#"$name"-}
   if [ -n "$current" ];then
    cmp=0;xbps-uhelper cmpver "$current" "$pin" || cmp=$?
    case $cmp in 0|255) ;;1)newer=1;;*)echo ERROR:BASE_XBPS_VERSION;exit 1;;esac
   fi;;
 esac
 if [ "$newer" = 1 ];then
  echo "BASE_PACKAGE_RETAINED:$name"
  # APT can include the installed version as an explicit dependency constraint.
  # Other local-file managers resolve against their installed package database.
  case $target in termux|ubuntu|debian|kali)set -- "$@" "$name=$current";;esac
 elif [ "$target" = void ];then set -- "$@" "$name-$pin"
 else set -- "$@" "$file";fi
done < "$manifest"
[ "$count" -gt 0 ] || { echo ERROR:BASE_INSTALL_EMPTY;exit 2; }
# Every pinned package may already have a newer installed version.
[ "$#" -gt 0 ] || { echo READY:base-packages-installed;exit 0; }
echo JOBS_APT_PHASE:install
case $target in
 termux|ubuntu|debian|kali)
  export DEBIAN_FRONTEND=noninteractive
  recover_apt
  echo JOBS_APT_PHASE:install
  # APT must run its local-file acquire method before invoking dpkg. With
  # --no-download, fresh files outside its archives cache can retain a bare
  # filename and fail only after dependency simulation has already succeeded.
  # Disable repository sources for this invocation instead: local DEBs and the
  # installed database are the entire plan, with no upstream fallback. Leave
  # the user's source files and package caches unchanged.
  apt-get -o Dir::Etc::sourcelist=/dev/null -o Dir::Etc::sourceparts=/dev/null -o Dir::Cache::pkgcache= -o Dir::Cache::srcpkgcache= -o Dpkg::Options::=--force-confold -o APT::Status-Fd=3 -o Dpkg::Use-Pty=0 install -y --no-remove --no-install-recommends "$@" 3>&1;;
 archlinux|manjaro)
  # Local archive installation does not initialize a distro's package keyring.
  # Also cover a rootfs created before the environment preparer gained this step.
  if [ ! -s /etc/pacman.d/gnupg/pubring.gpg ] && [ ! -s /etc/pacman.d/gnupg/pubring.kbx ];then
   echo "BASE_STAGE:prepare-keyring:$target"
   pacman-key --init
   if [ "$target" = manjaro ];then pacman-key --populate archlinuxarm manjaro-arm manjaro
   else pacman-key --populate archlinuxarm;fi
  fi
  pacman -U --needed --noconfirm -- "$@";;
 alpine)apk add --no-network --no-progress --progress-fd 3 "$@" 3>&1;;
 fedora)
  # The pinned Fedora image carries its official signing key; local-RPM
  # transactions cannot fetch a key from a disabled upstream repository.
  if [ -f /etc/pki/rpm-gpg/RPM-GPG-KEY-fedora-44-aarch64 ];then rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-fedora-44-aarch64;fi
  dnf -y --disablerepo='*' --setopt=localpkg_gpgcheck=True --setopt=install_weak_deps=False install "$@";;
 opensuse)
  repos="${base%/packages}/empty-repositories"
  [ -d "$repos" ] && [ ! -L "$repos" ] || { echo ERROR:BASE_ZYPP_REPOSITORIES;exit 1; }
  for entry in "$repos"/* "$repos"/.[!.]* "$repos"/..?*;do
   if [ -e "$entry" ] || [ -L "$entry" ];then echo ERROR:BASE_ZYPP_REPOSITORIES;exit 1;fi
  done
  # --disable-repositories also disables Zypper's temporary local-RPM repo.
  # An empty per-call repos directory keeps that repo usable without upstreams.
  zypper --non-interactive --no-refresh --reposd-dir "$repos" install --no-recommends "$@";;
 void)
  # Original signed upstream metadata only; no unsigned generated repository.
  [ -f "$base/aarch64-repodata" ] || { echo ERROR:BASE_XBPS_REPODATA;exit 1; }
  FORCE_IPV4=1 xbps-install --ignore-conf-repos --repository "$base" -y -- "$@";;
esac
