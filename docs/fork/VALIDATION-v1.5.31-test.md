# v1.5.31-test — guided Vertex project setup

Private build, not a GitHub release. Update from code1188 to1189, package com.termux,
Android8+/ARM64, existing signing identity, library schema8 without clearing data.
Final artifact checks are recorded below when complete.

## Implementation and review

Gemini Vertex startup runs a new data-only project handshake after the existing
Cloud bootstrap/auth step and before launching Gemini CLI. Already configured
project or Vertex Express key bypasses the request. Otherwise a private per-run
marker asks the existing Android chat poller to show localized guidance and a
project/location dialog. The native mini-terminal hides when the project request
arrives. No separate global polling thread or model request was added.

The project prompt can be closed with Back and reopened from the chat notice;
outside taps do not dismiss it. Switching sessions/screens or closing the UI
invalidates/dismisses its old dialog. Submission checks the current run and waiting
marker, validates metadata, saves non-secret Vertex defaults in private app
preferences, then atomically publishes two lines to that run's response file.
The button is disabled after success. No preset/chat is created by this operation.
No token, browser authorization code or credentials file is stored there.

The shell waits at most15 minutes, reads data without eval/source and validates
both fields again before export. The effective project/location reach Gemini
without being overwritten by old empty/default profile exports. Each run uses a
unique Gemini socket UUID for its request/response names; markers are inside the
existing private Gemini control bind. Responded markers are removed. A killed run
can leave its small marker, but a new run never reuses it. Process shutdown code
is unchanged; only foreground shell/sleep work occurs during this bounded wait.

Defaults apply only to Vertex profiles with an empty project. Explicit per-profile
projects win. Editing a nonempty Vertex project also updates new-connection
defaults. Ordinary/API/other-agent connections do not load these preferences.
Defaults are application-wide metadata, not an account-specific authorization;
account changes may require a different project. Global Cloud configuration,
IAM/API enablement and billing are not changed. Credential existence is not a
claim of valid login or model access. Existing browser/native auth remains intact.

## Automated verification

133 focused JVM tests passed, zero failures/errors/skips. New tests cover independent connection preferences,
no saved-session/thread mutation, explicit project priority, API/Codex isolation,
invalid project/location rejection, per-run response isolation, stale callback
refusal, code-vs-project input error and valid dialog save/continue. Existing
agent-chat lifecycle, native terminal/composer, auth and editor checks are included.

Isolated Bash handshake fixtures passed: existing/inherited project, explicit
region, Express bypass, private request permissions, atomic response, exported
JOBS and GOOGLE_CLOUD values, injection and invalid-region refusal, marker cleanup.
Native pinned Gemini Vertex auth regression passed with synthetic inputs and no
network/account operations. No real project/account login or inference used.
Initial dialog fixtures clicked before Android onShow delivery; they were corrected
to drain the main looper before interacting with the controls.

## Developer checks needed

Update the APK, then restart only the Gemini Vertex session that lacked a project.
Existing ADC should avoid repeated browser login. Confirm the project dialog
appears after credentials are found, enter the actual Cloud project ID and region,
then Save and continue. Check a real request, Back/reopen, screen/session switching,
and next launch reusing the defaults without creating a preset. Verify another
explicit project remains selectable in session settings. IAM/API/billing failures
remain possible even when components and metadata are ready. No Android keyboard,
gestures or real Google account behavior is claimed tested by Robolectric.

## Artifact

Release assembly and runtime definition check passed; 31 optional unresolved
runtime types retain existing explicit review notes. Existing Kotlin/UAST/JNA Lint
compatibility warnings were emitted; no clean Lint claim. Final Java sources match
both tested and assembled staging. ZIP integrity, package, API26, ARM64, signature,
versionCode increase and artifact privacy-marker checks passed. Existing assets
and both native libraries are byte-identical to v1.5.30; one new vertex-project.sh
asset was added. The privacy scan is not proof against every possible secret.

APK `Termux-Jobs-v1.5.31-test-arm64.apk`, 36651111 bytes.
SHA256 `eededc180a9dedc11cb1fd42efb72dd1bed78745f7947267a070adddbca9efc6`.
Certificate SHA256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
The same package/certificate and code1189 permit an update over1188; actual
Android installation and first-run behavior remain developer checks.
Evidence is in `.verification/vertex-project-v1.5.31/`.


## Subsequent developer report

Developer reached Cloud sign-in and Vertex API error responses. SERVICE_DISABLED
was followed by BILLING_DISABLED after API activation. Cloud Console then rejected
billing account setup with OR_BACR2_59. Its underlying cause was not determined.
No successful Vertex inference was verified. Developer elected to retain current
Vertex/Claude integrations and postpone their end-to-end device tests due to
account/billing constraints. This is partial connection evidence, not proof of
complete tool/chat/model operation. Documentation-only update; APK unchanged.
