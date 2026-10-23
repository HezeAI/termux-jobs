# Agent Ubuntu runtimes and migration

Existing schema8 codexEnvironment stores ubuntu, ubuntu-ai or legacy Codex termux. All
other agent flags retain their established environment=ubuntu representation. A single
allowlist, AgentEnvironments, controls runtime aliases; it rejects arbitrary shell values.
All four launchers, preparation/check inventory, Codex live/history/project normalization,
OpenCode editor model/chat queries, Gemini/Claude stop, ProjectFiles and package profiles
route to the chosen root. Existing profiles remain unchanged until explicit selection or
confirmed bulk switch. No chat rendering, tool permissions or Android sandbox policy changes.

## Copy transaction

AgentEnvironmentManager shows plan/size/free/reserve and explicit secret-copy notice.
agent-environment.py runs natively under Termux Python, never inside the source guest.
Requires native proot-distro5 constants/layout and locks path. Acquires nonblocking flock
on the same source/destination files used by distro login/install/remove; no fail-open
lock fallback. Live login/run inherits a shared lock, so any such session blocks copying
or deletion. A supplementary own-UID /proc check rejects raw proot arguments naming the
root; unreadable own process metadata refuses the operation. No session is signalled/killed.
Application operation gates also block launches while its migration/removal worker runs.

The root/container and all directory components are opened O_NOFOLLOW. Snapshot hashes
cover relative names and lstat identities including inode/type/size/mtime/ctime. Confirmation
is bound to that token and checked again before copying and publishing. Only ubuntu→ubuntu-ai
is allowed. Full container metadata/manifest and rootfs files are copied into a hidden UUID
stage in containers, not an archive in shared storage. User secrets are never printed.
Regular files are independently copied and SHA-256 read-back verified; in-copy source
identity is checked. Hardlinks are recreated only within the new tree, never to the source.
Symlinks are preserved without following them, with absolute source-container prefixes
rewritten to the new container for PRoot link2symlink compatibility. Mode/time and file/
directory xattrs are preserved; security.selinux is inherited from the destination Android
app directory, not overridden. Runtime sockets are omitted; other unsupported special
files fail closed. Source files are not edited; migration preparation may remove only the empty mode000 /workspace placeholder as described below. External mounts/phone storage are not
traversed. Owner IDs are the same Android app UID, no Android root ownership is promised.

After verification and source recheck, atomic renameat2(RENAME_NOREPLACE) publishes the
new alias and the parent is fsynced. An existing file/link/directory is never overwritten.
Cancellation is cooperative via a private UUID marker; owned unpublished stage is removed
with descriptor-safe rmtree, including read-only directory cleanup. Activity destruction
requests cancellation and waits for the owned helper, without terminating other processes.
Abrupt OS/process kill can leave a hidden unpublished .jobs-ai-* stage consuming space;
it is excluded from selectable environments. No automatic deletion of arbitrary stale
folders is implemented. Pending stages require deliberate cleanup/review. No claim of
power-loss atomicity of a full environment clone beyond publication/retaining the source.

## Switching and deletion

The current editor switch is saved with its usual confirmation. Bulk switch updates
inactive agent presets on ubuntu only; live scopes are skipped. Original and copied
histories/auth files are independent afterward. Native CLI state uses the same guest
paths, but hardcoded source names/host paths inside scripts/prompts are not globally
rewritten. TERMUX_JOBS_AGENT_ENVIRONMENT and JOBS_AGENT_UBUNTU identify the selected
runtime to subprocesses; this is metadata, not a new model system prompt. General OpenCode
account connection management remains app-global (backend prefers an installed Ubuntu AI,
so it still works after the ordinary Ubuntu is removed); no per-runtime credential isolation is
claimed for data held in the app's control directory or cloud services.

Installed ordinary/imported environment rows expose trash; base Termux has no trash.
Library bindings and active sessions are checked twice around confirmation; native lock
is authoritative for external CLI/desktop/package operations. Only one validated container
alias can be removed. A fresh snapshot token and exclusive lock are required. The scanned
inode is atomically quarantined and its identity rechecked before no-follow deletion;
a replaced inode is restored without deleting its data. No shell command strings, force,
recursive dependency removal or process-killing API. Rootfs removal is irreversible after
quarantine; archives/shared storage/app-owned libraries are retained. Manually embedded
alias names in arbitrary startup commands cannot be automatically tracked.

## Compatibility

Fresh AI installation uses normal proot-distro Ubuntu image naming (modern --name,
legacy --override-alias); existing systems are not replaced. Migration and trash require
modern containers storage/native locking; an older layout is refused with an update hint.
The private APK upgrades code1259→1260 with unchanged package/certificate. There are no
automatic data resets or automatic environment changes. Old APK readers do not support ubuntu-ai bindings: old Codex validation may refuse the
library, while older other-agent launchers can fall back to ordinary Ubuntu. Return all
agent bindings to Ubuntu before deliberately downgrading. Never remove the source before
actual post-copy validation.
Real large-rootfs migration, each cloud agent's post-copy auth/history, Android lifecycle,
free-space exhaustion and keyboard/touch behavior require developer device checks.

## Private1.7.46 corrections

open_dir traverses Android ancestors with O_PATH|O_DIRECTORY|O_NOFOLLOW rather than
requesting directory-list permission on / or /data. Only the actual final storage base
is opened readable. Execute-only ancestors and refusal of ancestor links are tested.
Plan exclusively locks its source only; cloning exclusively locks source+destination.
Removal of old Ubuntu can proceed while separate Ubuntu AI holds a shared native lock,
provided the old source has no references/active users. No busy guard is disabled.

Missing Python/proot binary or native lock-module contract offers explicit preparation
through environment-runtime.sh's fixed agent-migration-tools branch. It probes tools,
uses pkg only for necessary dependencies after confirmation and rechecks. This branch
never enters a guest. No guest root is silently replaced. Access/process/path errors are
separate from dependency errors and do not offer package installation. errno is shown
without exception messages, paths, file contents or credentials. Helper disables Python
bytecode writes in its environment. A failed preparation never automatically retries.

## Access diagnosis (private1.7.47)

Permission failures carry only fixed stage codes and errno. LOCKS_ACCESS_DENIED is
lock directory/file setup; PROCESS_CHECK_DENIED is process inspection (including /proc
listing); ENVIRONMENT_ACCESS_DENIED is source structure; SCAN_ACCESS_DENIED is metadata
walk; COPY_ACCESS_DENIED is later copy/verification. Nested stages retain the more specific
code. Snapshot does not read regular file content. No key exclusion, source chmod, silent
file skip or weakened busy protection is introduced. Actual screenshot errno13 cause
remains unproven pending retry in Android app context.

## Android mount placeholders (private1.7.48)

ANDROID_MOUNTS is a fixed set of rootfs-relative directory endpoints for Android system
partitions, /dev, /proc, /sys, /sdcard and /storage, Dalvik/application paths and common Termux host home/
prefix/cache binds. Their directory identity is in the snapshot token; children are
intentionally out of scope. Only directories match; symlinks remain un-followed and
regular files retain ordinary copy validation. No prefix-name matching or arbitrary
access-denied skipping. Target directories are created empty and retain source mode/time;
source xattrs are not queried for those placeholders (Android labels inherit destination).
Other source permission failures still block publication. Excluded host payload is disclosed
in migration confirmation. Source remains intact. This is full Ubuntu migration with
host endpoints excluded, not a new selectable-components UI.

Termux Python was measured to lack os.link/listxattr/getxattr/setxattr, with Bionic linkat/
flistxattr/fgetxattr/fsetxattr present. FD-only ctypes fallbacks preserve hardlinks and
non-SELinux xattrs. Attribute reads retry ERANGE at most three times, cap allocation1MiB,
and fail on unreadable attrs; no silent metadata loss. linkat flags0 refuses replacement
and does not follow the final symlink. Fixture tests also run with the actual native Termux
Python through the current Ubuntu/proot; no real installed migration is inferred.

## Local scan failure location (private1.7.49)

Failure messages now display an internally constructed root-relative path and fixed
operation for SCAN_ACCESS_DENIED. Python truncates the path to512 codepoints plus ellipsis
(keeping ASCII JSON below the8192 reader bound even for astral characters); raw exception
filename/text is never used. Java rejects absolute and parent-component paths, escapes
control/bidi characters, bounds display size, adds actual BuildConfig version. Metadata
is shown only in the local failure dialog, not sent or persisted by this mechanism.
This intentionally expands earlier fixed-code-only diagnostics to identify the remaining
APK-context failure. No general access-denied skipping, source chmod or migration bypass.

## App bind endpoints (private1.7.50)

Confirmed APK scan denial: rootfs/tmp/termux-fork-session-<UUID>. SessionShell binds an
app-owned workspace-sessions directory there, with history/bashrc and cancellation files.
That app-owned payload remains in place; new sessions bind it again. Presets live in
WorkspaceStore and agent conversation records in their established source/app locations.
Migration now treats only strict canonical UUID directory names at that exact tmp depth
as mount placeholders. Other exact endpoints, all defined in existing launcher code:
/tmp/termux-jobs-{setup,gemini,claude,ai,codex}. Existing Android endpoints remain excluded.
No tmp-wide prefix exclusion, deletion, source chmod or unknown access-failure skip.
Directory identities remain checked; source is retained; target endpoints are empty and
inherit label. The copy confirmation explains this. Existing bind-backed payload is not
copied or wiped; source Ubuntu ~/.codex/etc normal files still follow ordinary migration.

## Automatic empty workspace preparation (private1.7.50)

Only migration plan explicitly marked prepare-migration (ubuntu source only) performs this
preparation. Normal plan/removal previews do not. Under source exclusive flock, after raw
idle check and cancellation tick, rootfs is pinned O_NOFOLLOW. Only literal workspace that
is a plain mode000 directory is eligible. fd-relative rmdir is the emptiness test and
removal: kernel refuses any contents with ENOTEMPTY/EEXIST; no recursive traversal/chmod,
symlink follow, file deletion or catch-and-continue for unknown inaccessible directories.
Missing, non-directory, linked, nonempty or ordinary-mode paths are retained. Real failure
remains an error. Successful empty-dir removal is fsynced, then snapshot/token is computed
on the prepared source. Plan reports emptyWorkspaceRemoved; confirmation explains it.
Preparation is idempotent and may remain done if copy is later cancelled or not confirmed;
only empty directory metadata is removed, never user files. Final source state can differ
by this explicitly supported cleanup; original file-data retention guarantees remain.

## Private 1.7.51: Ubuntu AI defaults and test environment removal

New agent sessions/presets select Ubuntu AI by default; saved choices are preserved.
The migration button is disabled when Ubuntu AI is selected. Its environment-list row
says “Used for AI agents”. Ubuntu X11 Test is no longer a built-in catalog entry;
installed legacy aliases remain discoverable for compatibility. Removing its root does
not remove the APK X11 renderer or the ordinary Ubuntu desktop configuration/code.
Desktop packages must be installed separately in ordinary Ubuntu.

## Private 1.7.52: independent agent Ubuntu installation

Selecting Ubuntu AI in an agent session/preset includes installation of that selected
root when absent, after the existing preparation confirmation. Ordinary Ubuntu's
presence does not block it. Migration is a separate optional action, never an automatic
prerequisite. Existing profiles retain their chosen alias, including ordinary Ubuntu.

Host preparation checks container existence independently of login success. It installs
only missing roots through environment-runtime.sh, retaining v5 --name and legacy
--override-alias support. It verifies login after installation; failures abort without
fallback, reset or reinstall. Inventory on an existing but inaccessible/busy root now
returns a check error instead of a missing-Ubuntu plan. Retry after installation finishes.
The standalone host helper defaults to Ubuntu AI; launchers still explicitly pass the
selected alias. No existing environments, live sessions or credentials are changed by
this source/build work. No migration/reset is required to update from1.7.51.

## Inline environment installation (private1.7.52)

The environments chooser stays open when installation is confirmed. The chosen card
shows Stop, a phase progress bar, measured phase ETA when available, and the latest
output line. Missing percentage/throughput is displayed as calculating, not an invented
overall completion time. Successful readiness switches to disabled Installed. Failure
or cancellation does not claim readiness. Tap an installed row to select it explicitly.
Closing the chooser or switching app tabs does not cancel the worker; reopening binds
the same active state. Only the current alias card is updated; the list and terminal are
not rebuilt for progress events. One setup operation runs at a time.

Stop writes a unique private control marker. A native Python supervisor owns a new
installer session/process group, polls that marker, sends TERM only to its own live
child group, then KILL after3 seconds if necessary. No user session, proot container
or unrelated PID is selected for termination. A private PTY exposes real installer
progress without borrowing the user's terminal or stdin. If native Python is absent,
explicitly approved preparation installs it first; a Stop request during that prerequisite
is honored after the package command finishes, before the environment installer starts.
Stopped installations can leave incomplete roots; no root is automatically removed.
No foreground-service persistence across Android killing the app is claimed.

The agent environment selector only records Ubuntu/Ubuntu AI; it no longer starts
migration or installation implicitly on selection. Missing-root preparation happens
at explicit launch, and migration remains on its own button.

## Unstarted temporary records — private v1.7.67

New unsaved profiles are added only when a terminal is actually created, rather
than before asynchronous launch preparation. Creating a terminal rechecks the
migration/removal operation gate and rolls back a new empty record on failure.
Startup reconciliation retires previously loaded unsaved records with no handle
once the terminal service is available. This does not delete their saved history,
saved launch presets, or the base terminal. Drafts created after loading are not
included in this cleanup. The environment deletion reference list ignores empty
unsaved records; saved/attached references and native process locks still apply.
The in-memory workspace is not edited externally to perform this fix.
