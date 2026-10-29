# Working candidate compared with public v2.0.9

Historical pre-release review, 2026-10-29. Public baseline verified through GitHub:
[v2.0.9](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.9), commit
`81e9dad9`. This document does not announce publication or exhaustive device
validation. The reviewed private candidate was test11, versionCode1442. The resulting stable
release is [v2.1.0](RELEASE-v2.1.0.md), versionCode1443.

## User-visible changes

| Area | Difference from public v2.0.9 |
| --- | --- |
| Kali / Manjaro | Separate environments and X11 services, package/program integration, aliases and environment-specific renderer support. Manjaro has its own compatible bridge/Turnip payloads. |
| Installation baseline | Nine ordinary ARM64 roots and matching XFCE package closures are pinned through immutable Components assets. Ubuntu AI retains its existing policy. Existing user-updated packages are not silently downgraded at startup. |
| Environment updates | Explicit package checks/upgrades, immutable Components base discovery/restore, complete checkpoints and rollback. Native Termux and Ubuntu AI are excluded. |
| Official images | Upstream ARM64 discovery plus confirmed installation/replacement. Checks stay in the selected menu; initial installation, replacement, cancellation and recovery have separate state paths. |
| Documentation | Global ArchWiki, Forum shortcut, per-environment documentation, in-app HTTPS navigation and explicit copy/browser actions. Page state persists in the application process. |
| Session header | Current-session card fills the row, with bordered settings, packages/agent and file actions inside. |
| Agent chats | Reliable link clicks across the shared chat renderers; Copy / Show / Browser; richer live command/image/file/web action details and compact detail dialogs. |
| Application compatibility | Target-specific Kali Evince and Manjaro LibreOffice launcher changes; the four requested Kali recommendation cards are removed without uninstalling existing tools. Fedora DNF5 and openSUSE NDB update-check handling corrected. |

## Review priorities and evidence

Source review follows request → UI state → worker → native target lock → temporary
container → package/image validation → atomic publication → checkpoint/recovery.
Checks include existing sessions remaining untouched, target-specific renderer
and package routing, stale Stop/plan refusal, symlink/managed-link handling,
private checkpoint contents, TLS/redirect trust and data compatibility. The file
inventory includes tracked differences AND new untracked source/test/documents;
a tracked-only diff would omit much of the new implementation.

Automated evidence is recorded in `.verification/official-install/`:
`comparison-files.json`, `source-secret-scan.json`, Python/JVM/Node summaries,
native OCI extraction and GPG proof logs, and lint reports. Relevant private
test1–test10 reports remain historical evidence, not claims that every scenario
was repeated on this candidate. No private auth material is included in the
review report or APK.

## Compatibility and limits before stable publication

- Package ID/signing identity and schema8 remain. Installing the APK does not
  reset commands, notes, presets, chats, drivers or environments.
- Official-image replacement intentionally changes the selected OS/program set;
  its explicit confirmation offers home preservation and keeps the entire old
  system for rollback. It is not a promise to migrate every installed application
  or configuration across distribution releases.
- SDK28/package-ID limitations of the fork remain; this work does not resolve
  independent installation alongside upstream com.termux.
- Fresh-phone GUI/network policies and successful online DNF/Zypper/XBPS/APK/
  Pacman transactions across every target remain distinct acceptance checks.
- Stable publication still needs developer acceptance of the final APK, its final
  hash/version/signature, localized update.json compatibility notes and the
  release-documentation checklist. Nothing is published by this review.

AI development assistance: OpenAI Codex
