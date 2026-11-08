# Stable 2.1.7 /1536 verification

The developer confirmed phone testing of 2.1.7-test2/1535 and authorized publication.
Production inputs are byte-identical to that accepted candidate; only versionName
and versionCode change. Published release2.1.6/1533 is the change baseline.

## Checked anew

Final signed release APK: com.termux, 2.1.7 / versionCode 1536, ARM64 only,
non-debuggable; ZIP, signature, DEX/runtime/assets and all seven ELF files checked.
Size: 40,675,217 bytes. SHA-256:
`f321c19652d80dd91cf96a46e4fcefffd1f309faebb61e3cc64ab4f0fe5bbebb`.
Signer: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Full Modern Lint 7.4.2: all five modules, 0 errors, 290 warnings, no infrastructure
failures. Compile SDK 35 exceeds the Lint/AGP tested level; warning retained.
Final update.json generated from this APK; 16 local debug JVM updater/package
installation tests passed with no failures or skips, including final RU/EN metadata.
All packaged assets/native libraries match the developer-tested APK. Production
source identity comparison is preserved in accepted-comparison.json.
Initial CI [42475835531](https://github.com/HezeAI/termux-jobs/actions/runs/42475835531)
failed one stale test assertion: AgentComposerControllerTest expected no restart
button after an agent exited. The accepted recovery fix intentionally retains it.
Only that test expectation was corrected to require the clickable restart button;
production code and the signed APK are unchanged. The repeated full CI [42476694520](https://github.com/HezeAI/termux-jobs/actions/runs/42476694520)
passed all stages, including debug APK assembly. Source and workflow commit:
`997ceb4d3a9a601d0bf92d96433a22f2023839e2`. 1,949 cases: 1,940 passed, 9 skipped,
0 failures/errors. Eight SFTP fixture cases are skipped; the final-release manifest
case lacks its environment in CI and passed separately in the 16 local updater tests.
The corrected composer test plus 21 restart tests also passed locally.
The original failure and skipped downstream debug build are retained in the evidence.
The stable release publishes only the signed APK, SHA256SUMS, update.json and
validation.json. No rollback APK, debug APK or component archive is included.

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
