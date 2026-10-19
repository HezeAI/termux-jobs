# Historical private build records — v1.5.15-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.15-test validation

Full Java suite passed:660 app +145 emulator cases, zero failures/errors/skips. Existing dropdown test now verifies48dp uniform height and196dp popup width as well as selected-session state, vector/model subtitle and switching. Evidence: `.verification/local-ai-v1515/tests.log` and `test-counts.json`. This is a source/UI-structure check, not a phone/DPI readability check.

Review: only sessionStackItem/showSessionStack dimensions changed, plus private version1152. Main selector, server button, tap/long-tap callbacks, selection semantics, inference, history and permissions unchanged. No data migration/reset or process/environment change.

Release build and required runtime-class audit passed. APK ZIPCRC/safe entries, com.termux, minSDK26, ARM64 ABI, private local-AI flag, signing certificate, unchanged native/agent assets and limited secret scan passed. Existing successful-build Kotlin metadata diagnostics retained. Final Download copy SHA independently verified.

SHA-256: `9d46bfabc7d6969e9ae84b223e30bd58a87f35e2e4e47280ddad63c3c8d741f8`.
Size:104892784 bytes. Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Developer check: open session list, inspect row widths/heights and long labels at current DPI, switch active session and long-tap. All row types retain48dp height; full text remains in accessibility descriptions. Device visual checks pending. No GitHub publication.


---

## Subsequent development record

# v1.5.15-test — private OpenCode input refinements

Built and audited private APK; no GitHub publication.

Package `com.termux`, versionName `1.5.15-test`, versionCode1177, minSdk26,
ARM64 only, 36577202 bytes. ZIP/v2 signature verified; certificate matches
v1.5.14-test and versionCode is higher. No data reset/migration.
SHA-256 `c42e816a5298be747fe20f21d62172219e93e249370ec1812273cfaa6758eb55`. Copied to Download and copied hash verified.
All packaged assets and native libraries are byte-identical to v1.5.14-test;
required DEX runtime definitions and private-file/secret-marker checks pass.
Release build succeeds; existing Lint Kotlin/UAST/JNA infrastructure warnings
remain, so this does not claim a warning-free Lint run.
Artifact evidence: build.log, apk-audit.json, signature.txt, badging.txt.
The developer approved the prior layout; new device checks are still pending.

Checks: the full JVM suite passed561 tests (407app+154emulator). After the final
key-layout refinements, all11 focused tests pass:3 transcript,6 chat-adapter
lifecycle/input and2 extra-key routing checks. Key routing tests verify text
insertion, forwarded arrow/control key codes and disabled-field protection; they
do not establish physical cursor movement/IME behavior. The chat adapter test
verifies restoring the native key client/parent after switching sessions.
Node projection regression tests and git diff whitespace checks also pass.
Evidence: java.log/full-suite.json, ui-final-confirmed.log/final-ui-suite.json,
node.log. Earlier failed key test harness attempts are superseded by the final
focused run.

Requested changes from v1.5.14-test:

- Shell field and execution button are unavailable while interactive input is
  disabled. Toggling it preserves the draft; other sessions keep their own input.
- Additional-keys button works in OpenCode Android chat. Only the configured
  extra-keys view is mounted, without the toolbar's alternate raw PTY input page.
  ChatExtraKeys edits the enabled focused Shell field or model composer. It never
  delegates chat key input into the hidden bridge. Native mini/other sessions
  restore the original client/parent/layout/visibility. Native window sizing and
  ordinary input are unchanged.
- Normal model replies say OpenCode: and no longer open duplicate detail windows
  on tap. Reasoning/actions/permissions and truncated full-detail access remain.
- Chat spacing and native window dimensions are unchanged from the developer's
  approved v1.5.14-test layout. No native protocol or provider configuration edits.

Verification evidence: .verification/opencode-chat-v1.5.15/. Device checks remain
necessary for touch/IME, modifier keys, selection/copy, Shell lock/toggle, switching
sessions, and restoring keys after opening/closing the native dialog. Automated
UI checks do not establish physical keyboard/gestures/DPI behavior.
