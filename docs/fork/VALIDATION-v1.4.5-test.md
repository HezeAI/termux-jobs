# v1.4.5-test validation

- 509 JVM/Robolectric tests passed; failures/errors/skips 0. New coverage: indicator updates preserve message instances; card raw details and unconfirmed state; user-only accent; legacy history/result retention; malformed historical requests; successful/failed/rejected/cancelled tool states.
- Final isolated ARM64 release build passed; runtime-definition gate passed (15827 definitions, 30 explicitly reviewed unresolved types).
- ZIP CRC, package com.termux, version 1.4.5-test/code1111, minSdk26, ARM64 ABI and existing signing certificate verified. Limited private-file/secret pattern scan passed. Native libraries and server-agent payload byte-identical to v1.4.4-test. Staged source matches final sources.
- Legacy Lint Kotlin metadata diagnostics remain; no clean Lint claim.
- Permission authorization still precedes execution and approval; presentation callbacks do not execute commands. Animation invalidates only the small view, stops when hidden/detached and does not rebuild message history. No separate isolation was added.
- Device validation outstanding: animation/layout/scroll/text selection/IME and tab lifecycle, real model/tool live cards. Tests do not establish device behavior or model tool compliance.
- No GitHub publication. No user session/process/environment modification. No data reset. Existing rollback1110 is below new1111 and cannot update this build; no new rollback requested.

APK SHA-256: `12e8d206bd5d6699579fa4930b2a32e5a34e500b5184ac5d7c86848e0778ba43`
