# Claude permission-mode phone fixture — test23 / 1518

Prepared at the user's request, using the same local permission-test provider
mechanism as the earlier Claude/image stand. This is a new isolated fixture;
previous fixture profiles, files and running sessions were not modified.

Create a **new** Claude chat and set these values before its first launch:

- Name: `CLAUDE MODES TEST`
- Environment: Ubuntu AI
- Model: Sonnet
- Working folder: `/root/termux-fork/.verification/claude-mode-phone/project`
- Connection command: `/root/termux-fork/.verification/claude-mode-phone/launch-claude`
- Initial mode: Confirm actions / manual

No real sign-in is needed for this fixture. It supplies an isolated HOME/config,
synthetic API key and local provider, guarded against external network access.
These paths exist on the current phone only. Do not assign an existing Claude
conversation ID: that conversation does not exist in the fixture HOME.

## Manual acceptance

1. Send `AUDIT SHELL INSIDE 31`. Verify a permission request appears. Deny it first;
   the test file must not become `AUDIT_SHELL_31`.
2. Send `AUDIT SHELL INSIDE 32`, allow once. In the project folder, `shell.txt`
   should contain `AUDIT_SHELL_32`.
3. Open Permissions and approvals. Enable Auto and save. The active session is
   unchanged. Restart **only this test chat manually** to apply the setting.
4. Send `AUDIT SHELL INSIDE 33`. Inspect the action and `shell.txt`. The native
   CLI can execute this harmless command in Auto on the synthetic stand; a real
   classifier may make different decisions on other requests.
5. Reopen Permissions. Auto stays checked. Disable it: the prior ordinary mode
   returns. Also test the prior Accept edits and Plan/read choices.
6. Bypass permissions is disabled here with the native root/proot restriction.
   This is expected. No UID spoofing or native safety-check override is installed.

## Independently checked, not phone UI

Native Claude 2.1.296 with the current production controller, local provider and
network guard: manual raised one approval, marker remained unchanged until allow,
then became AUDIT_SHELL_26. Auto had zero pending approvals and wrote the different
marker AUDIT_SHELL_27. Bypass refused before the worker started due to UID0.
All test-owned workers exited through the controller cleanup; no working session
was restarted. Provider logs are bounded metadata, no real keys.

First probe's Auto marker matched the preceding manual marker and was inconclusive.
The rerun waits for completion and uses distinct markers; only the rerun is counted.
Evidence: `.verification/test-2.1.5-test23/fixture-probe-final.log`, `fixture-root.txt`,
`fixture-probe.cjs`; fixture `permission-mode-probe.json` and `runs/provider-*.json`.

This does **not** validate a real Claude model's classifier, actual account policies,
phone checkbox rendering or permissions behavior under a non-root Ubuntu account.
The synthetic provider never forwards requests to Anthropic.
