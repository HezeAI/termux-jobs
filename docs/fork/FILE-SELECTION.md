# File selection and destination actions — v1.6.0

In a session file browser, a normal tap still opens a folder or the usual file
menu. Long press selects an entry and reveals checkboxes. Checkbox taps change
selection independently of navigation. Selected paths survive folder/filter
changes in that browser; they are not transferred to another environment/profile.
Up to50 files/folders may be selected. Pickers for SSH keys, upload files or a
working directory keep their established picker behavior, without copy/move UI.

Parent navigation uses a vector left arrow. A destination arrow sits below Home
at the right of the search row. It is inactive without selection. Beside it,
`× N` clears selection and disappears when empty. Destination actions offer
Copy here, Move here and Back; outside touches do not dismiss that menu or its
confirmation. The destination is the displayed physical folder at menu opening.

Operations use the selected environment through the existing independent file
transport, never by typing into an agent's terminal. Whole-batch path/name
preflight precedes mutation. Copy stages each entry in a private destination
folder and publishes only after cp succeeds; links are copied as links. Move uses
native no-clobber mv (including its cross-filesystem behavior). Existing objects
are never automatically replaced or merged. Name conflicts require confirmation
to allocate new names such as `app (2).apk`, retaining file extensions. A new
conflict after confirmation stops the operation instead of overwriting.

Self-descendant destinations, overlapping parent/child selections, missing sources
and unsupported source types are rejected. Cancellation/dismissal through the
browser is disabled during the file operation; its status remains visible.
Interrupted batches are not transactional: completed entries stay at destination,
and an incomplete result explicitly asks to inspect both folders before retrying.
Abrupt app/process termination cannot promise rollback. Each operation uses the
existing permissions and mounted paths; this is not a new filesystem sandbox.
No cross-environment copy/move picker is introduced by this update.

## Server file browser

The same separation applies: row tap opens folder/file menu; long press and
checkboxes select. Selection survives navigation on the same server, and resets
when changing servers. `× N` clears download/delete selection and any prepared
move. Unchecking the last selected entry also ends delete-selection mode and
removes its active outlines. Existing download destination picker, SFTP transfers and two-step Move /
Move here remain. Server copying was not added.

Selections can contain entries from multiple source folders. Confirmed deletion
shows their full paths and groups requests by original parent, keeping the
existing server-side dir-fd/no-symlink-following validation. A failed group stops
further groups; completed deletions remain. No automatic retry or server agent
replacement is performed. Monitoring, auth, SSH and all AI transports are unchanged.

## Select all (private v1.5.23-test)

The session browser adds Select all to the left footer opposite Close, only while
selection is active. It uses exactly the visible search/type-filter predicate and
retains entries selected in other folders, up to50 total. The action never closes
the browser. Reaching the cap shows a localized explanation.

Server file controls put `× N` and a vector Select all button at the right of the
same transfer row, with8dp spacing. Transfer buttons share the remaining width;
the selection buttons use independent measured slots. The
selection group is hidden at zero selection. Select all adds displayed current-folder entries,
retains earlier selections, and reports the500-entry cap. During an already
prepared move the button is disabled until that move is completed or cancelled;
existing pending-move identity stays unchanged. No filesystem operation is launched
by selection itself. All labels/tooltips are RU/EN.
