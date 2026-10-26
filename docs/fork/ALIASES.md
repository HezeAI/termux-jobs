# Environment aliases (v2.0.9)

The Commands/Notes library has a separate **All aliases** filter. **+** opens two
outlined choices: **New command** and **New alias**. An alias stores its short
name, command, installed environment, category and optional favourite marker.
The category picker separates **Command categories** and **Alias categories**;
each contains its own virtual **Favorites** filter. Existing commands, notes,
categories and favourite markers are preserved.

The star picker uses **Commands / Alias / Notes**, independent search text for
each tab, and a full preview before insertion, editing or confirmed deletion.
An alias inserts its short name into the composer of a running terminal profile
for its selected environment. It does not execute it or send it to an agent.
A running shell opened before the alias was saved needs to be reopened first.
Only the Termux editor shows **Restart the tab to load the alias** immediately
below its environment selector. Only Termux deletion asks to restart the tab;
other environments retain the existing close/reopen-session explanation.
Profile matching cannot detect a different shell manually entered inside a PTY.

Supported targets are installed Termux, Ubuntu, Debian, Alpine, Arch Linux ARM,
Fedora, openSUSE and Void Linux. Ubuntu AI, SSH and imported custom rootfs aliases
are excluded. Saving never installs a missing environment or falls back to the
host when a guest is unavailable. Aliases may use different package-manager
commands in different environments.

## Storage and shell integration

Metadata is an optional `aliases` array in `workspace-v1.json` schema8. A UUID,
name, command, environment, category and favourite flag are retained. Legacy
libraries without the array still load. Invalid definitions or duplicate names
in the same environment reject decoding before changing the in-memory library.
Commands and notes are independent. Existing app backup snapshots retain the raw
workspace JSON and selected HOME/rootfs files.

## Explicit import and export

Backup and restore has one **Import/export aliases** card with four actions:
**Import from shell file**, **Import from JSON/backup**, **Export to shell file**
and **Export to JSON**. JSON export previews and selects records, then writes a fresh `Termux-Jobs-Alias-*.json`
file into the selected directory (Download by default). It preserves each selected
definition's environment, category and favorite marker. Commands, notes, presets,
shell histories, settings, keys and agent authorization are not included. Alias
bodies themselves can contain private information; review before sharing.

JSON import accepts this JSON, a `workspace-v1.json` library or a backup manifest.
It also has an entry in the existing restore plan. Preview and selection do not
execute commands or inspect environment startup files. Only explicitly selected
managed records are imported. Missing environments are disabled; installation
is not automatic. The same eight-target whitelist still excludes Ubuntu AI and
custom imported rootfs names. Empty categories become General (Общее in Russian).

The confirmation lists matching name/environment replacements; it does not clear
unselected aliases. A replaced record retains its local UUID; new definitions
receive new UUIDs. The backup is inspected again before applying, and a changed
source or changed destination record requires a new selection/confirmation.
An owned helper writes definitions into each selected installed environment,
without executing their bodies or restarting existing shells. Sequential imports
compensate completed helper changes on a later failure. Metadata is saved after
successful helpers and checked against the persisted library. An explicit error
reports failed compensation or unconfirmed persistence rather than success.
Private pre-import alias metadata is retained in `workspace-import-recovery`.
Process/device death between helper and metadata commits is not an atomic
transaction; these recovery records may require manual review.

Aliases defined manually in shell files are not automatically discovered or added
to the library. The older explicitly chosen aliases/scripts-to-commands backup
action remains separate and does not create managed aliases.

### Shell files and ordinary Termux

Manual shell import reads one explicitly chosen UTF-8 file, such as `.bashrc`,
`.bash_aliases`, `.zshrc` or an exported `.sh`. Choose the installed destination
environment before inspection; preview/select the definitions before confirming.
The file is never sourced or executed. Literal single/double-quoted definitions,
escaped characters, multiline bodies, line continuations, multiple literal
assignments and an optional trailing semicolon are supported. Dynamic expansion,
command substitution and command chains are rejected rather than evaluated.
Unsupported alias statements are counted. The last supported literal definition
of a name wins in the preview; this is not an emulation of arbitrary startup code.
Input is bounded to1MiB. Categories default to General; favorite flags start off.

Shell export first chooses one environment, then selects its records. It writes
a fresh `Termux-Jobs-Alias-*.sh` with safely quoted `alias name='command'` statements,
usable from Bash/Zsh in ordinary Termux or another compatible shell. It never
overwrites or automatically edits `.bashrc`, `.bash_aliases` or `.zshrc`.
Categories, favorites and target bindings are retained by JSON, not the shell file.
The source fingerprint is rechecked before either kind of import is applied.

On save, a separate owned helper process writes a quoted definition into
`$HOME/.termux-jobs-aliases/<UUID>.sh` in the selected environment. It adds one
marked loader block to `.bashrc`, `.zshrc` and `.profile`, plus `.bash_profile`
only when the latter already exists. It never creates a Bash login profile that
would hide `.profile`. The application also loads managed aliases from its
portable interactive `/bin/sh` startup for supported guests without Bash.
An inert `_loader.sh` keeps an empty collection safe for strict glob handling,
and the startup guard tolerates deletion of the managed directory. Standard
Bash/Zsh startup files are supported; custom `ZDOTDIR`, other shells and
arbitrary external startup wrappers are not automatically configured.

Alias bodies are quoted data, not evaluated during creation or startup. Alias
files use an escaped `alias` builtin so another alias cannot intercept loading.
The loader's cleanup builtin is escaped as well. Definitions are owner-private,
written by rename under an environment-local operation lock. Existing startup
contents are preserved; symlink/non-regular targets and unmanaged definition
files are rejected. Files larger than1MiB are not rewritten. No unrelated paths,
rootfs installations, desktop components, running terminal sessions, packages,
agent authentication or server connections are modified.

The creation UI confirms replacement of another managed alias with the same name
in the same environment. Unmanaged/computed shell aliases are not executed or
imported to discover them; normal shell order decides precedence. Saving a move
first installs the new definition and then removes the old one. Failed later
steps compensate completed steps; a failed compensation is reported explicitly.
The workspace record changes only after successful helper operations. A crash
between shell and metadata writes is not a cross-process atomic transaction.

If an environment has already been removed, deleting its library record succeeds
without falling back to the host. Edit and delete affect future shell startup. Existing shells keep their already
loaded definitions until reopened; the application never injects `unalias`,
sources files into a live shell or restarts an active agent to synchronize them.
The alias command is limited to16,384 characters and its name to64 ASCII letters,
digits, `_` or `-`, beginning with a letter or `_`. Shell reserved words are
rejected. Commands can contain quotes and line breaks, but not NUL or CR.

Device verification must check creation, restart, rename/replacement, deletion,
category/favourite filtering and the star picker in the intended environments.
Isolated shell/Robolectric checks do not prove every installed shell or provider.
