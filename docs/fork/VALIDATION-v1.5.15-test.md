# v1.5.15-test validation

Full Java suite passed:660 app +145 emulator cases, zero failures/errors/skips. Existing dropdown test now verifies48dp uniform height and196dp popup width as well as selected-session state, vector/model subtitle and switching. Evidence: `.verification/local-ai-v1515/tests.log` and `test-counts.json`. This is a source/UI-structure check, not a phone/DPI readability check.

Review: only sessionStackItem/showSessionStack dimensions changed, plus private version1152. Main selector, server button, tap/long-tap callbacks, selection semantics, inference, history and permissions unchanged. No data migration/reset or process/environment change.

Release build and required runtime-class audit passed. APK ZIPCRC/safe entries, com.termux, minSDK26, ARM64 ABI, private local-AI flag, signing certificate, unchanged native/agent assets and limited secret scan passed. Existing successful-build Kotlin metadata diagnostics retained. Final Download copy SHA independently verified.

SHA-256: `9d46bfabc7d6969e9ae84b223e30bd58a87f35e2e4e47280ddad63c3c8d741f8`.
Size:104892784 bytes. Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

Developer check: open session list, inspect row widths/heights and long labels at current DPI, switch active session and long-tap. All row types retain48dp height; full text remains in accessibility descriptions. Device visual checks pending. No GitHub publication.
