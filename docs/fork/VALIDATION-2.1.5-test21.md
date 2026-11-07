# Private test21 — 1516 — restore compact input-history clear action

Date: 2026-11-07. com.termux, ARM64, same signer/schema8/min26/target28.

User rejected test20's full-width Clear button. Historical source inspected:
`a4c6f1d9^` (test18), WorkspaceController.home and section. Clear was the compact
right-hand action of the inner section title row, between explanation and search.
Removing that duplicate title incorrectly replaced the whole row with a full-width
button in test19/test20. This change restores that row's exact button factory,
wrap-content width/height, 22dp top/10dp bottom padding and 80dp minimum height.
A weighted empty spacer takes the former title's width. Duplicate heading stays
removed. Upper explanation and current empty-state wording remain unchanged.

Confirmation, recentCommands-only storage and immediate list/count refresh unchanged.
The button stays disabled when empty as in test20. No agent close/rename/programs,
other data, permissions, processes, or X11 changes.

## Verification scope

Compare actual button left/top/width/height and row height with historical section
layout at 300dp. Confirm right alignment and compact width. Run the existing
cancel/clear/populated-other-data/empty-counter fixture. No real signals or user
history writes in these fixtures. Full modern Lint and final release APK audit.

### Checked anew

- Targeted UI regression: 1 test, zero failures/errors/skips. At 300dp the button's
  left/top/width/height and containing row height match the historical section.
  Right edge equals row width; button width is less than half the row. Cancel,
  confirmation, list/count/empty state and other-data preservation also pass.
- Full modern Lint7.4.2, all five modules: zero errors, 279 warnings. Existing
  compileSDK35 support warning retained. Legacy lintVital in packaging emits old
  registry/Kotlin/platform diagnostics and is not counted as successful analysis.
- Release APK assembled. Final ZIP/package/version1516/test21/ARM64-only/debugfalse,
  signaturev2/certificate, runtime/DEX/assets/private-data checks pass.
  620 production files match staging; seven ELF files match stable2.1.4.
- git diff --check passes. ADB devices list empty; no phone UI check claimed.

### Reused

Native payloads reused only after a fresh comparison of every recorded native
input hash and binary hash. No reused device-graphics claim. Other test20 features
were not retested because this change only restores the history action container.

Evidence: `.verification/test-2.1.5-test21/`.
APK bytes: 40621018. SHA-256:
`7252053d6af2827d9002a3f6d4dc061bdd5b18d161aecbb0d384681d7157cecf`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

## Device

User screenshot establishes test20's undesired full-width button. Test21 geometry
is not yet checked on the phone by the assistant; no APK is installed automatically.
