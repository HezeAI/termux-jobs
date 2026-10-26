# Private alias test1 validation — 2026-10-26

This is a private preview of environment aliases, based on public v2.0.8.
No new GitHub release has been authorized or published for this feature.

## Artifact and compatibility

- `build/apk/Termux-Jobs-test1.apk`: versionName **2.0.9-test1**, versionCode **1414**.
- Package `com.termux`, ARM64, Android8+ app / Android11+ X11; min26/target28/compile35 unchanged.
- Size **39785457 bytes**; SHA-256 **69019c66dee5f4271f68cdb6b05b6b4af67353ec39a86e12dc5b16c8b47cee82**.
- Signing certificate SHA-256 **dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644**, matching stable v2.0.8.
- ZIP integrity, signature, package/version, ABI, non-debuggable flag, source assets and runtime definitions pass.
- All three JNI libraries and the X11 runtime payload match stable v2.0.8.
- 79 source assets match the final APK. No private filenames, credentials or secret-pattern findings.
- The optional `aliases` array keeps schema8; legacy libraries without it reopen. No data reset or rootfs migration.
- Next build must exceed code1414 for an update over this preview. Header remains v2.0.

## Checks

**98 JVM cases /7classes** pass without failures/errors/skips: alias data and profile
routing, actual generated stdin helper execution in a private fixture, replacement
and failed-move compensation, missing-environment deletion / save rejection,
separate library/category/favourite filters, star search and full preview,
delete confirmation/cancel, old data round-trips, FIFO persistence, input and
owned-child transport. Alias UI tests never install into an actual phone rootfs.

**8 shell fixture cases** pass against Bash and POSIX sh: quotes/multiline bodies,
no evaluation of substitutions or cleanup aliases during save/startup, idempotent
loader registration, rename/delete, unrelated records, symlink/unmanaged-target
rejection, busy-lock cleanup, existing rc contents and absent Bash login profile,
empty collection and manually removed managed directory. Files, caches and test
homes remain under the repository. Zsh was not available for a real Zsh run; its
startup registration and strict-glob handling are reviewed, not device-verified.

Compatible **lint7.4.2**, all5modules: **0errors /218warnings**,
no custom-rule infrastructure failure. Relative to stable's217 warnings, one new
SetTextI18n warning is the selected environment label plus a dropdown glyph;
the labels are intentional bilingual UI/data. Legacy APK-chain AGP4.2 lint still
prints its previously documented Vendor/Kotlin-metadata diagnostics; it is not
substituted for the separate compatible lint gate. APK runtime verification:
13,797 definitions,31 explicitly reviewed optional/framework unresolved types.

## Review and state-flow inspection

Saving validates name/body/environment and rechecks installed state before an
owned helper writes the selected HOME. Ubuntu AI, SSH, imported aliases and host
fallback are rejected. Bodies are shell-quoted data sent through stdin, not
commands embedded in an exec argument. Existing rc contents survive; unsafe links,
non-regular files and unmanaged alias targets are not overwritten. The empty
collection has an inert loader and a guard for manual directory removal.

The metadata record changes after successful shell operations. Moving and managed
name replacement compensate completed steps on a later failure; compensation
failure is reported. Caught failures and ordinary helper termination release the
operation lock. Abrupt process/device death is not a transaction spanning shell
files and workspace metadata and can need manual inspection of a stale lock.

Delete requires confirmation explaining the already-loaded shell state. A removed
rootfs permits library-record deletion without writing to the host. Edit/delete
never inject commands into a running terminal. Star insertion checks the selected
local profile and only changes its draft; it neither executes an alias nor sends
it to an agent. Activity close allows an owned write to finish and records success
through a new store if the original controller has closed. Unrelated sessions,
agent authentication, Ubuntu AI, servers and graphics installers stay outside the
mutation path.

## Remaining device checks

Install the preview, create a simple alias in Termux and a Linux environment, open
new terminal sessions and use it. Check rename/replacement, moving between installed
environments, independent favourites/categories, star preview and confirmed delete
followed by reopening the shell. Verify the intended shell's startup conventions.
These checks remain for the developer; no every-device/environment or GUI claim is
inferred from JVM/shell fixtures. Existing selective backup-library import has no
alias selector yet; see [usage and limitations](ALIASES.md).

Evidence: ignored `.verification/aliases/` logs, JSON summaries, final artifact and
runtime inventory. No stable release/tag/assets were modified.
