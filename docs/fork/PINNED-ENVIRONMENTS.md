# Pinned initial environments and X11 package sets

Available in v2.1.0. This describes the installation baseline; isolated fixture
checks do not establish fresh-device GUI compatibility for every environment.

Ordinary ARM64 environments use the immutable `rootfs-arm64-20261028` Components
release. Ubuntu AI keeps the existing upstream `proot-distro` policy for agents.
The image helper verifies APK-pinned size/SHA-256, retains partial downloads and
passes the verified local OCI/tar archive to the native public installer CLI.
An existing rootfs returns through the readiness check and is never replaced.

The separate `xfce-base-20261028` package set contains original package files,
package signatures where supplied and an APK-pinned internal manifest. Download,
cache and staging paths stay inside private app controls. Safe extraction rejects
links, traversal, duplicates, unexpected files, checksum/size mismatches and a
manifest for another environment. Retry reuses a fixed staging path rather than
accumulating interrupted unpacking directories. Android filesDir/PREFIX aliases
are accepted by filesystem identity; unrelated same-named directories are rejected.

A confirmed desktop installation passes verified local files to the selected
native package manager. Repository disabling is command-local to that offline
transaction. APT runs its local-file acquisition method with repository sources
disabled for that call; `--no-download` is not used for local files outside its
archives cache. Installed-package versions are read in bulk for dpkg, pacman and
RPM to avoid reopening the database for each missing package under PRoot.
No global source, permanent version hold or global hosts file is
changed. Smaller Termux host-audio and host-VirGL closures avoid pulling in host
XFCE when preparing a guest desktop. Initial tooling prerequisites (Python,
proot-distro) retain their existing consented bootstrap path.

This is an initial baseline, not enforcement of old package versions:

- Normal launches test mandatory package/binary readiness, not exact equality
  with the pinned version list. A functional user-updated desktop keeps running.
- Users can install Python, development tools, libraries and other applications
  from official sources, and explicitly upgrade the environment or XFCE.
- Repair retains more recent installed packages. APT includes their installed
  versions as constraints; other native managers resolve against their existing
  package database. Conflicting plans fail without forcing package removals,
  downgrades or a rootfs reset. Configuration files are preserved.
- Ordinary recommended programs remain separate package-manager transactions;
  they may introduce dependencies. The baseline is not a permanent live-system
  snapshot and cannot guarantee compatibility with every future upstream update.
- Explicit package/image update, snapshot and rollback controls are described in
  [Environment updates](ENVIRONMENT-UPDATES.md). These are user-selected actions,
  separate from normal startup readiness checks.

Kali and Manjaro have dedicated environment aliases, X11 service processes,
displays 106/107, controls and desktop settings. Their files and sessions do not
share a graphical service. Manjaro offers CPU, VirGL and separate optional
Turnip + Zink / Bionic-for-glibc builds targeting its glibc 2.35 baseline; other
distributions retain their own compatible component routing.
On a newly created Manjaro only, the obsolete community repository entry is
removed before initializing its package keyrings. Existing roots are untouched.

Validation distinguishes archive checks, real isolated package transactions,
native dependency solver simulation, mocked installer plans and device GUI tests.
See [v2.1.0 validation](VALIDATION-v2.1.0.md) for exact results and remaining checks.
