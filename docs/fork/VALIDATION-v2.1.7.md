# Stable 2.1.7 /1536 verification

The developer confirmed phone testing of 2.1.7-test2/1535 and authorized publication.
Production inputs are byte-identical to that accepted candidate; only versionName
and versionCode change. Published release2.1.6/1533 is the change baseline.

## Checked anew

Final signed APK, full Modern Lint and updater metadata checks are in progress.
GitHub CI will be recorded before publication. No publication is complete yet.

## Reused after source identity verification

61 RTK tests and live Codex receipts, Claude2.1.296 SDK synthetic-provider fixtures,
Arch native Termux deletion/cancel/neighbour checks,40 JVM restart/environment tests
and launcher process fixtures. See the test1/test2 reports.638 production source
identities match the accepted candidate;95 native build inputs match their pins.
No dependencies, permission rules, storage formats or RTK binary changed.

## Scope

Live Codex checks used the reported bypassPermissions hook mode. Claude fixtures
used manual/default SDK approval with a local synthetic provider. No all-mode or
cloud-Claude validation is claimed. Unknown pipeline/ambiguous/truncated bases remain
unknown. No user environment or session is deleted or stopped during release work.
No new phone installation is performed by the assistant. Final APK differs from
the phone-tested candidate only in release metadata and resulting build/signature bytes.
