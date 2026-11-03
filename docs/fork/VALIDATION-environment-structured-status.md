# Environment setup: structured state only

2026-11-03. Local remediation of F-LOG-02, after the two earlier local fixes.
Published v2.1.2 and legacy log files remain unchanged.

EnvironmentSetup now drains merged child output using bounded records and keeps
only known stages, components, validated measured image bytes, fixed errors and
completion status. Result construction, injectable-runner UI callbacks, details
and export all sanitize into that finite protocol. Unknown lines, percentages,
URLs, multiline bodies, oversized lines and incomplete stream records are not
retained. There is no raw tail, raw exception or failed-command fallback.

The old Capture/read/save/clear APIs are removed. No new complete log is persisted,
and successful setup no longer deletes an existing legacy log. Details/export
contain only the current structured attempt. Legacy reports are not loaded or
exported, including after chooser recreation. Existing pending/download-state and
safe retry/cancel behavior remain. After recreation, a current report requires a
new check/attempt; the legacy raw report is intentionally unavailable in the UI.

The collector still drains through EOF and joins its own process before releasing
the operation slot. Read/callback/stdin-close errors and interruption retain the
existing private-cancel-marker and wait semantics. No process signal/ownership
implementation, native CLI history, ordinary terminal, permissions or rootfs is
changed. Unmeasured APT/extraction output shows an indeterminate phase.

Verification evidence: `.verification/environment-structured-status/`. Targeted
JVM/UI tests cover filtering at collection/Result/UI/export, large multiline and
unfinished artificial secrets, measured progress, unknown errors, storage-export
failure/retry, old-file preservation, chooser close/reopen, cancel/resume and owned
process wait on errors/interruption. Initial raw-percent fixture expectations were
corrected to the real IMAGE_PROGRESS protocol; no raw-percent fallback was added.

The initial scoped run had two failures from those obsolete fixture expectations;
its log is retained. Final check counts are recorded in the evidence and final
APK validation. Real installation and Android UI remain separate, unperformed
checks. No real credential was read and no working session was changed.
