# Private v1.6.2-test (1194)

Fixes Codex chat failing to connect with a large history. A reproduced native
eight-turn response exceeded the 2 MiB connection limit. History now pages one
turn at a time; chat wire accepts bounded messages up to32 MiB. A disconnected
controller no longer reports successful startup. Shell grouping is retained.

Update over1.6.1-test/code1193 with the same com.termux package/certificate.
No data/schema reset; reopen the same Codex history after updating. Active
processes are not guaranteed to survive Android package installation.
No automatic history deletion. Published stable remains1.6.0.

See VALIDATION-v1.6.2-test.md for artifact identity, checks and phone test.
