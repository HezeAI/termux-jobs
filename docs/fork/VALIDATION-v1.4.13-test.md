# Validation — v1.4.13-test

Local test build for HezeAI; source changes limited to shared local-model discovery,
listing details/schema, tests, version and documentation. No GitHub publication.

## Relevant tests

LocalAiRecentTest covers short RU/EN requests, folder aliases, ignored questions,
negations/paths/compound tasks, exact-extension filtering before date ordering,
directories excluded from typed-file results, alias discovery, extension-injection
rejection, locations → list → read coordination, no automatic reading of binary,
empty or denied results, scope guards and no discovery in chat-only mode.
Existing search-choice, local-tool permissions, UI, ordinary terminal and emulator
suites are included by scripts/verify-java.sh.

## Review and logical inspection

The app-generated sequence is per turn and shares the usual authorize/runner path;
selected environment validation, canonical paths, read descriptors, cancellation
and normal approvals remain. No command/install/write is introduced. A typed
list result is filtered before the newest-file limit. Only a successful single
returned regular text file is auto-read for an explicit open/read intent. Failure
or empty result never triggers the deterministic read. A model may subsequently
choose other allowed tools, subject to the same checks. Files are untrusted data.
No schema/data reset; com.termux and the signing certificate are retained.

This recognizer covers known folder aliases and short supported file-type requests,
not all natural language. It checks one available directory, not every storage
location. Real model reliability, storage access, keyboard/scroll and UI still
require device validation. No claims of exhaustive functional testing or clean
Lint; existing build warnings are recorded in logs.

Final machine evidence is recorded in .verification/local-ai-v1.4.13/artifact.json,
with build/test logs, SHA256SUMS, signature.txt and badging.txt.

## Final results

593 tests, 0 failures/errors/skips; ARM64 release build passed. Runtime gate: 15,873 definitions, 30 reviewed unresolved types. ZIP CRC, package com.termux, version 1.4.13-test/code1119, minSdk26, ARM64-only, v2 signature and prior certificate verified. Limited secret-pattern scan and staging-source match passed. Native payload and external agent controller assets match v1.4.12. APK bytes: 49343198. SHA-256: `aeb0992e89e3a9e65515d88d0ab94418cabc386c9b8928b1b523007869c123bd`. Download APK/README/SHA copies hash-verified.
