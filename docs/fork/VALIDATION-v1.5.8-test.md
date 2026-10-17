# Validation — v1.5.8-test

Private test build. Evidence: `.verification/local-ai-v158/`. A broad run executed623 app tests:621 passed and2 new continuation assertions failed due to escaped path presentation.145 terminal-emulator tests passed. After correcting path presentation, a targeted run exposed that an optional anchor could displace an otherwise fitting exchange. Anchor insertion was moved after ordinary compaction. The final71 relevant tests all pass (zero failures/errors), including all6 new continuation cases and existing context, summary, coordinator, LiteRT and video tests. Final tested source matches the APK build stage. Initial failed logs are retained; they are not presented as successful checks.

Changed paths: runtime-only context fitting, optional history argument normalization, and local-chat context notice presentation. Review covers unchanged system permission strings, current request preservation, archive immutability, nullable vs invalid arguments, per-turn notice boundaries/reload, and exclusion of notice data from model input.

Device checks to perform: continue a file-specific task after compaction; ask for history with normal permissions; expand the single context card, switch tabs and reopen the chat. Verify ordinary tools and video still work. Passing JVM tests does not establish real model interpretation, Android UI behavior or exhaustive regression coverage.

Final build/identity evidence is recorded in `build-verified.log`, `audit.json` and `1.5.8-test/artifact.json`. Device-specific checks below remain pending.

Final APK: 104884568bytes; SHA-256 `ff3caea6258ded12111ba7f15c64950a7cd73fc9d647e892477618f60fac032e`. ZIP CRC and path/duplicate checks pass, com.termux/1145/1.5.8-test, minSDK26, ARM64 only, private Local AI flag enabled, v2 signature and existing certificate match. Limited secret filename/marker scan passed; existing native payloads and controller/server-agent assets match baseline. Download copy independently hashed.
