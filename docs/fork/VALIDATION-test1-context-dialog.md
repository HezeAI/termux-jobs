# Private test1 follow-up: restore compact context-compaction notice

VersionName `2.0.10-test1`, versionCode `1432`.
Builds on [the 1431 action UI](VALIDATION-test1-compact-actions.md).

The shared tool-action handler intercepted context-compaction rows before the
existing compact notice branch, opening them in a large command/output window.
The compact notice is now selected before the generic tool route. It fits its
text, has no Back button and dismisses through outside touch or Android Back.
Image/file-change previews and live command/output dialogs keep their existing
behavior. Truncated notices retain the paged action path instead of hiding text.

The regression test clicks a real Codex tool row with contextCompaction metadata
through the shared chat UI. It verifies wrap-content height, notice text, absent
Back/action-status controls, no unnecessary detail requests and outside dismissal.
Image/file compact-dialog, active timeline and live-output tests run alongside it.
Evidence: `.verification/context-1432/`.

No workspace schema, permissions, bridge JavaScript, X11/native library or
renderer changes. Same package/signer and no data reset. Previous build1431's
live output was confirmed on the phone by the developer. The restored compact
notice still needs a device visual check on this build. Private APK only; no
installation or GitHub publication is performed by this task.

Automated result: **60 targeted JVM tests passed**, no failures, errors
or skips. This includes the new context-routing regression and existing live
action, image/file preview and timeline tests. Android lint passed with
**0 errors and 223 warnings**, unchanged from build1431.

Final release build, ZIP, signature/package/version/ABI and DEX checks passed.
Compared with build1431, only manifest/DEX/signature entries differ; all assets,
resources and native libraries are identical. The packaged secret-pattern scan
reported no findings.

SHA-256: `20ff931bc3b824adef9b0e863f5212ce0a6f21cbc1d4dd66e9b97969b367eb31`.
