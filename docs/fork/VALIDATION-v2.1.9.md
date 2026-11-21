# Release validation — 2.1.9 / 1549

## Scope

Changes since v2.1.8 include compact chat timelines, linked subagent details,
Ubuntu AI CLI updates, Antigravity startup, common instructions, skills and
project Git. Version 2.1.9 preserves exact selected project paths and retains
editing of API and Vertex on existing Gemini profiles.

Codex reads linked child history. Claude retains bounded text excerpts from
messages the current CLI stream links to a parent tool call. Antigravity
records reported participants. Claude and Antigravity indicate when full
native child history is unavailable. No child is started or resumed to read
its details.

The developer showed the selected folder on a phone. Its name ended in a
space, which the previous test editor dropped. The corrected path was
exercised in the editor and Git fixtures. The developer also confirmed Git
2.53.0 launches in the selected environment. These observations apply to the
test build.

## Checked anew

- Stable release source commit `91b45863ac4f97dccdac73a31748258c08acfcd1`. The isolated signed and independent
  build stages used matched production inputs.
- The final signed ARM64 release APK is `com.termux` 2.1.9 (1549), min API 26,
  target API 28, non-debuggable. Size **40,705,872 bytes**.
  SHA-256 `04b532d9c592c18abb2c3213e96ba9c5c6a56aa3923331d464094dc9aaf44271`.
  Certificate SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
  The signer matches v2.1.8 and test8. ZIP integrity and offsets, 16 KiB
  ELF alignment, permissions, DEX definitions, assets and absence of the
  private Git pin passed.
- A clean unsigned rebuild rebuilt the owned JNI and guest shim libraries.
  All 1,145 ZIP entries and the whole container after removal of the verified
  v2 signing block matched. See [reproduction](REPRODUCIBILITY-v2.1.9.md).
  An earlier signed candidate left empty unregistered ZIP records. The final
  build recreated the packaging output and has no unregistered data gaps.
- Real pinned Claude Code 2.1.296, in an isolated home with a synthetic loopback
  provider, emitted the linked child messages and task events. The application
  adapter showed the child response, reopened it from saved excerpts and
  rejected a different session. Internal asynchronous launch metadata was
  excluded. Antigravity was checked against its documented step events in an
  isolated adapter fixture. The tests verified parent action identity,
  cross-session rejection, paging and ordinary launch/interrupt behavior.
- Existing Codex subagent history, scoped stop, project Git, preparation,
  localized strings and agent instructions and skills were checked with relevant
  fixtures. The full-suite baseline found 22 failures and nine skips in 1,825
  app JVM tests before fixes. Reruns addressed stale presentation and scroll
  fixtures, shell quoting, the JDK JPEG dependency, and the legacy Gemini
  connection editor. The final focused Gradle rerun passed 137 tests with
  zero failures or skips. Additional standalone reruns covered the corrected
  scroll, package and timeline fixtures.
- Modern Lint 7.4.2 analyzed the app, terminal-emulator, terminal-view,
  termux-shared and x11. It found zero errors, 299 warnings and zero Lint
  infrastructure diagnostics. The AGP compile SDK compatibility warning
  remains.
- The updater metadata is generated from the signed final APK. It carries
  English and Russian notes, the existing compatible schema, and the exact
  package/version/minimum SDK, download asset, size and SHA-256. Twelve
  updater JVM tests passed with the final manifest supplied, including both
  languages and the preceding-version update check.

## Reused

The signed build reused the prior verified ARM64 native inputs. Their inputs
match the earlier source and the independent build rebuilt the owned native
code. Against v2.1.8, 56 native, X11, installer and license entries match.
The packaged general notices add attribution for HezeAI's supplied DevKit
timeline and instruction templates. Exact source and pinned corresponding
inputs are indexed in [Third-party notices](../../THIRD_PARTY_NOTICES.md).

## Not checked

Clean-phone installation and full setup, native Antigravity delegation,
live model behavior, cross-device data transfer and backup/restore remain
unverified. The local fixture checks establish their stated protocol and
behavior coverage. Phone compatibility follows the existing package/signature
and version code upgrade path. The application requests no data reset or
environment reinstall.

[Release notes](RELEASE-v2.1.9.md) ·
[Prior stable validation](VALIDATION-v2.1.8.md) ·
[Private test8 validation](VALIDATION-2.1.9-test8.md) ·
[Release candidate CI](https://github.com/HezeAI/termux-jobs/actions/runs/44869298619)
