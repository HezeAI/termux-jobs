# v1.4.1-test — local testing only

Local AI: a compact header Start/Stop toggle replaces large chat controls. The
session selector contains New chat, Choose chat, and Model and access. The chat
keeps the existing input field. Markdown headings, lists, bold, inline/fenced code
and HTTP(S) links are rendered offline; fenced code has a copy action. HTML and
remote images are not executed/loaded. Thinking remains in raw saved history and
is shown in an optional collapsed Reasoning block; empty blocks are hidden.

Three per-session modes: Chat only, Read project, Work on project / full access.
Tools browse/search/read files and, in full access, write files/run commands.
Full access runs actions automatically, without confirmation, as requested.
Path checks is a separate enabled-by-default checkbox. On: file tools are confined
to the selected project using canonical paths. Off: they can access other paths
allowed by Android and the selected environment. Permission modes still apply.
Shell commands are not a project sandbox even when path checks are enabled.
Model-produced paths/actions can be wrong; small models may fail tool protocols.

Choose Termux, an installed proot environment, or shared storage in the existing
project picker; Android storage access is requested by the picker. Tools run in
independent owned subprocesses, not by typing into an existing terminal.
Custom model imports use the app file manager and existing storage-access checks. Copying shows percent/bytes/speed and estimated remaining time; only Cancel dismisses an active progress dialog. Original files are not modified.

No arbitrary limit on tool-call rounds is added. Existing 4096-token context and
768-token generation limits remain; tool data uses bounded text previews. Large
files (>64 KiB) are not handled by the simple read/write tools; use commands in
full access when appropriate. Tool commands have a 10-minute hang timeout. Do not
start detached/background jobs through this experimental tool runner: they can
outlive cancellation. Foreground cancellation was checked with isolated fixtures.

Package/signing identity retained. Test code1103; refreshed pre-feature v1.3.4
rollback code1104. After that rollback, future builds need code1105 or greater.
No automatic data reset. Saved optional fields default to Chat only and path
checks enabled when absent. Models/history from v1.4.0-test remain in place.
Rollback limitations are documented in LOCAL-AI.md. No GitHub publication.
