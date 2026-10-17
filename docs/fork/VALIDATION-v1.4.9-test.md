# Validation: v1.4.9-test / 1115

554 automated tests passed, zero failures/errors/skips. Nine new screenshot tests cover mixed formats/mtime, video/symlink exclusion, project boundary before probing, multiple Android storage paths, missing/denied/empty status separation, alias deduplication, schema/permissions across model profiles, short request recognition, and recorded tool execution before model generation.

Final ARM64 release build succeeded. ZIP CRC, package com.termux, version 1.4.9-test / 1115, minimum SDK 26, ARM64 ABI, v2 signature and certificate continuity verified. Final source matched packaging stage. Native library contents and server-agent asset matched v1.4.8-test. Limited archive-name/private-key/token-pattern scan passed; this is not exhaustive proof of absence of secrets. Existing Kotlin metadata/Lint limitations remain; no claim of clean Lint.

Evidence: .verification/local-ai-v1.4.9/tests-delivery.log, build-delivery.log, artifact.json, signature.txt, badging.txt. Earlier tests/build logs are intermediate and superseded.

Code review: explicit shell quoting, no nested symlink traversal, project checks before probing other locations, read-only discovery, no environment fallback or new permissions, bounded output and scan depth. Existing ordinary terminal/CLI agents and other local-chat button styles unchanged. Screenshot results locate files; they do not assert image content or universal availability of Android storage.

Device checks still needed: Android storage permissions and mounts on another device, dedicated Ubuntu discovery, visual settings appearance, actual local-model follow-up image inspection. No claim that file discovery adds vision to text-only models. No installed application or working environment was changed during verification.

Final SHA-256: dec36b0f35a60a2646185e27def007a5fdced46c7feb02d96c135827cbc34813
