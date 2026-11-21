# Release validation — 2.1.9 / 1549

## Scope

Stable release candidate based on the private test8 build. Changes since
v2.1.8 cover chat timelines, Codex child-agent history, Ubuntu AI CLI updates,
Antigravity startup, common instructions, skills and project Git. The stable
candidate preserves trailing spaces in a selected project path and excludes
the private Git path and digest used by the test build.

The developer provided a phone screenshot that showed the project in the
Ubuntu AI file browser. The corresponding folder had a trailing space. The
test8 editor dropped that byte when saving the working directory. The clean
phone startup, the repaired editor on a phone and live provider coverage
remain pending.

## Signed APK candidate

Version 2.1.9, code 1549. Package com.termux, min API 26, target API 28,
ARM64 only, non-debuggable. Size 40,701,499 bytes. APK SHA-256
`48e69cc28c36500e25acb089bb99bc4bdb66ce3d77c0e277643f092293018bed`.
Certificate SHA-256
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

The candidate has passed package/ABI/signature and ZIP integrity checks, the
three direct ARM64 library segment alignment checks, required DEX/runtime
definition checks, and an asset hash comparison against test8. The two builds
have the same packaged assets. The stable candidate does not carry the
private Git pin. Its update manifest uses the APK's name, size and SHA-256
and supplies compatibility text in English and Russian.

## Checks and release status

Release checks, independent source reproduction, exact source commit, CI
and public download verification will be recorded here when completed.
The current candidate has not been published.

[Release notes](RELEASE-v2.1.9.md) ·
[Prior stable validation](VALIDATION-v2.1.8.md) ·
[Private test8 evidence](VALIDATION-2.1.9-test8.md)
