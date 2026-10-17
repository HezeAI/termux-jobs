# v1.4.4-test — local test build

Clarifies and enforces independent Local AI action permissions, project scope,
and Yes/No confirmation for all local engines/imports.

Chat only has no tools; Read allows browsing/search/reading without changes or
commands; Full access adds writing and commands. Path checks ON restricts file
tools to the project and disables all arbitrary shell commands, even harmless
names or Full access requests. The rejection precedes approval and process
construction. No separate isolation or executable allowlist is introduced.

Path checks OFF permits file paths beyond the project within actual app and
environment rights; action-mode restrictions still apply. Turning OFF in settings
requires accepting a localized warning about outside-project data and Full-access
changes/deletion/commands. Cancelling or dismissing keeps scope checking enabled.
Yes/No confirmation cannot override the scope or action mode.

Compatibility: no data reset; saved OFF values and existing profiles remain.
Behavior change: saved ON profiles can no longer run shell commands; to run them,
select Full access, turn off Path checks and accept the warning. No Android root
or filesystem permissions are granted by this choice. Code1109; refreshed baseline
recovery1110. Later APKs after recovery need1111+. No GitHub publication.
