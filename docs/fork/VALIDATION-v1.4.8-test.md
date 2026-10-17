# Validation v1.4.8-test

545 tests passed, zero failures/errors/skips. Relevant coverage includes environment selection, rejection of legacy local aliases, ownership/readiness, isolated login argument transport, shell syntax, cancellation without fallback, and unchanged ordinary terminal routing.

Release ARM64 build succeeded. ZIP CRC, package com.termux, version 1.4.8-test / 1114, min SDK 26, ARM64-only ABI, v2 signing certificate continuity and SHA-256 verified. Native libraries and server-agent asset unchanged from v1.4.7-test. Source staging matched final source. Limited artifact secret/name scan passed; this is not exhaustive proof of absence.

Evidence: .verification/local-ai-v1.4.8/{tests-verified.log,build-verified.log,artifact.json,signature.txt,badging.txt}. Existing lint/tool metadata limitations remain; no claim of clean Lint.

Not verified on device: actual Ubuntu installation/migration across proot-distro versions, package installation, Android shared-storage access, model execution, keyboard and gestures. No environment was installed during source verification. Separate proot routing is not a security sandbox for arbitrary commands.
