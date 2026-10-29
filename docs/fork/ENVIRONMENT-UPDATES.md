# Environment updates and rollback

v2.1.0 adds **Settings → Check environment updates** for the nine ordinary
PRoot distributions: Ubuntu, Debian, Kali, Alpine, Fedora, openSUSE, Void, Arch
Linux and Manjaro. Native Termux and Ubuntu AI retain their existing update policy.
Nothing is updated automatically on terminal or desktop launch. Users can still
install and update software with ordinary terminal commands; those commands do
not automatically create one of these checkpoints.

## Checking and applying package updates

Check all installed environments sequentially, or select one and check its
packages. Checking refreshes repository metadata without installing packages.
Arch/Manjaro checks use a separate sync database so that merely checking does not
leave their live package database in a partial-upgrade setup.

Review the package/version candidates, then explicitly confirm an upgrade. This
list is not a complete solver transaction: the distribution's native manager
resolves dependencies, replacements and removals in the working copy. The preview
expires after 30 minutes, and the candidate list is resolved again before upgrade.
A changed list requires another check/confirmation.

Applying changes requires closing every native session of that environment,
including its desktop. The app refuses a busy native alias; it never terminates
the user's sessions or deletes package locks. Upgrades operate on a full private
copy of the complete container, including its native manifest when present. Plain rootfs-tar installations
such as the pinned Arch/Manjaro images have no OCI manifest and use the public
native login command; OCI images use run with an explicitly checked entrypoint. Required system,
package-manager and pre-existing mandatory desktop binaries are validated before
publication. Shell/package-manager execution is probed in the copy. Errors and cancellation before the atomic publication leave the active
environment in place. An interruption during publication may already have
completed the exchange; journal recovery finishes its checkpoint records.

The native package manager uses its configured official/user repositories and
normal signature checks. No unauthenticated or dependency-bypass install flags
are added. Commands run through the public isolated native proot-distro CLI;
there is no new custom PRoot wrapper or change to installed native tools.

## Checkpoints and rollback

The full previous container becomes a private checkpoint when an update is
published. Restoring a checkpoint first copies it, and also retains the complete
current state as a new safety checkpoint. By default the current /root and /home
are carried into the restored system; the confirmation can disable this merge.
Files outside those directories are not silently merged into an older system:
they remain in the complete safety checkpoint. Ordinary shared-storage symlinks
are copied as links without reading or deleting their external targets.

Checkpoints include programs, configuration and potentially keys/passwords.
They remain in private app storage and are not automatically uploaded/exported.
Each can consume roughly another full environment; applying an update needs
additional free space for the working copy. Delete unused checkpoints explicitly
from the same section. A failed identity check retains data for manual review
rather than guessing which directory can be removed.

## Base images and pinned desktop packages

### Official upstream discovery

**New official images** is separate from Components. Opening it starts a read-only
check if no result is cached in this application process; Check official images
rechecks all nine targets. Closing the dialog keeps the foreground job running.
Stop cancels its owned helper; a cancelled scan does not replace prior complete
results. Each source can independently report a network, rate-limit, ARM64 or
metadata error. Failure is never presented as “no update.”

Only bounded HTTPS metadata is requested, with normal TLS verification, fixed
official hosts and no cross-host bearer redirects. Each source runs in a separate
owned process with a 65-second limit. No rootfs archives are downloaded, package
managers executed or installed environment files changed by this discovery.
The selected environment menu offers explicit installation/replacement from the selected
environment's menu, described below. The existing Components path remains
separate.

Sources and comparison rules:

| Environment | Official release/snapshot metadata | ARM64 image confirmation |
| --- | --- | --- |
| Ubuntu | releases.ubuntu.com released numeric directories | Docker Hub library/ubuntu, release series |
| Debian | ftp.debian.org stable Release (Origin/Suite/Architectures) | Docker Hub library/debian:stable |
| Alpine | latest-stable aarch64 minirootfs release metadata | Docker Hub library/alpine, stable series |
| Fedora | fedoraproject.org/releases.json, stable numeric aarch64 Container_Base | Docker Hub library/fedora, stable version |
| openSUSE Leap | get.opensuse.org/api/v0/distributions.json, Stable only | Docker Hub opensuse/leap, stable version |
| Kali | official kalilinux/kali-rolling:latest | Docker Hub Linux ARM64 child digest |
| Void | official GHCR void-linux/void-glibc-full:latest | OCI Linux ARM64 child manifest digest |
| Arch Linux ARM | official de3.mirror.archlinuxarm.org generic aarch64 rootfs listing | exact archive name/date; no invented installed-image comparison |
| Manjaro ARM | official manjaro-arm/rootfs latest non-prerelease release | exact aarch64 archive asset |

Ubuntu/Alpine compare major/minor release branches; Debian/Fedora compare major
branches; openSUSE compares Leap major/minor. Point releases are displayed, with
package updates remaining the normal way to receive fixes in the same branch.
Installed openSUSE Tumbleweed is not compared against Leap as if it were the same
branch. Rolling systems do not have a fabricated “new major version” status.
OCI child digests are compared only with the APK-pinned clean source digest,
never with an installed mutable rootfs or a multi-platform index digest.

Fedora DNF4/5 output uses a literal tab because DNF5's queryformat documents
newline expansion but does not expand a backslash-t separator. RPM fingerprints
include SQLite, NDB Packages.db/Index.db and older Berkeley DB Packages files in
both supported guest paths. Absolute/relative guest database aliases remain
confined to the selected rootfs.

### Official installation/replacement

The selected environment menu stays open while checking its packages or official
image. Progress, errors and new candidates update in that same window. Its
official check requests only that target; the global check still scans all nine.
Installation needs a current server-held plan (30 minutes), bound to the selected
target and current container identity. Changing the system or completing a root
replacement requires checking again. Unsupported legacy native layouts remain
refused, not silently migrated by this feature.

An absent environment gets Install official image. An installed environment gets
Update from official image. Confirmation explains that this replaces system
packages/applications; it is not an in-place distribution-release upgrade. Current
/root and /home can be carried over, while files outside them and every previous
application remain in the complete old-system checkpoint. No application data is
automatically cleared by installing the APK itself.

The installer prepares a recorded temporary native alias while the target is
exclusively locked. It never terminates the user's sessions. Native archive
extraction, normal package signatures and mandatory desktop checks precede
publication. Initial publication uses an atomic no-replace rename; replacing an
existing system uses atomic exchange and retains its complete old tree. Recovery
distinguishes absent-target installation from an interrupted replacement and
reconstructs origin records without reinstalling a published system.

- OCI: freeze the ARM64 manifest digest, verify the manifest/config/layers, check
  Linux ARM64 and an empty entrypoint, then pack a local OCI archive. Extraction
  stays in the public proot-distro CLI. This avoids mutable-tag installation and
  the installed CLI's lack of digest-reference parsing.
- Manjaro: exact upstream release asset, published SHA-256 and size required.
- Arch Linux ARM: freeze the detached signature's SHA-256 at preview, then verify
  the archive with the APK's public official signing key. GnuPG is required in
  Termux; a missing verifier gives an explicit prerequisite message, never a
  signature bypass. Official signing fingerprint:
  `68B3537F39A313B3E574D06777193F152BDBE6A6`.
- Anonymous registry bearer headers never cross a CDN redirect. Only HTTPS to
  fixed official registry hosts/known distribution CDNs is used; no TLS bypass.

Cache identity derives from the immutable approved descriptor, so a recheck of
the same content can reuse verified downloads. Successful publication removes
only the recorded cache directory through identity-checked quarantine/deletion.
Cancelled/failed downloads remain private for retry; incomplete streamed OCI or
Arch files may restart, while complete verified blobs are reused.

If XFCE existed, its complete required package set is installed from the new
guest's repositories, without forcing an older pinned closure onto a new release.
Future desktop installation uses an inode-bound private official-origin receipt
to select the guest repository path. It does not change Termux host audio pins,
renderer archives, ordinary pinned installations or Ubuntu AI. Package names or
driver ABI compatibility can still change upstream: a failed required-component
check refuses publication, and fresh GUI/renderer acceptance remains necessary.
PRoot's managed backing links are materialized as real hardlinks before renaming
a working alias; ordinary external symlinks remain links and are not followed.

After a root replacement, use the existing Programs installed/catalog refresh
actions if its prior list is still cached. This feature does not yet invalidate
every live service's program cache automatically.

### Verified Components sets

**Check for new base images** reads published immutable Components release
metadata. **Restore the pinned baseline** is a separate confirmed system
replacement, not an ordinary package upgrade. Both keep the complete current
environment as a safety checkpoint and offer the home-directory option.

Source release, alias, ARM64 architecture, download size and SHA-256 are checked.
The currently pinned image and matching XFCE package closure are those already
defined by the APK's environment-images.json and x11-base-packages.json.
A future image cannot be combined with an unrelated pinned XFCE set. If XFCE is
installed and no matching set is available in this APK, replacement is refused;
the existing environment remains intact. A root's original image provenance is
not inferred from /etc/os-release or from installed package versions.

## Lifecycle and recovery

Closing the window does not cancel the job. The application-owned foreground
service displays its stage and a Stop action; reopening settings shows progress.
Stop requests cooperative cancellation through the job's private marker. The
Python supervisor signals only its owned process group and keeps its leader
unreaped until after the final group signal. It never signals a PID read from a
log, another terminal, or an entire PRoot process list.

An update is published with one atomic renameat2(RENAME_EXCHANGE) of complete
container directories, without a two-rename interval where the alias is absent.
The transaction journal records both inode identities before the exchange.
Interrupted-job recovery distinguishes an unpublished copy from a completed
exchange and finishes the snapshot receipt where safe. Unknown/replaced
identities are refused. Compatible native shared/exclusive alias locking and
one filesystem for working roots/checkpoints are required. Legacy environments
or unsupported native managers are not changed by this feature.

Android can still kill an application or its children. A foreground notification
is not a promise of uninterrupted execution under every device's process policy.
Recovery checks the persisted journal rather than automatically rerunning package
scripts. An interruption during native base installation before a temporary
identity is recorded may require manual review; unknown trees are never deleted
solely because their name resembles a temporary alias.

## Verification limits

See [test9 validation](VALIDATION-test9-environment-updates.md) and
[test10 validation](VALIDATION-test10-official-images.md). Disposable
filesystem/process fixtures and a real APT/dpkg transaction establish specific
source/build behaviors. They do not establish fresh-phone GUI behavior or every
native distribution's online repository transaction. No installed phone
environment is used as a disposable test root.
