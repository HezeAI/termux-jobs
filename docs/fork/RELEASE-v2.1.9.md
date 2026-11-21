# Termux Job’s 2.1.9

Termux Job’s 2.1.9 updates agent timelines, adds CLI management in the
sidebar, and prepares Git for new chat projects in Ubuntu AI.
HezeAI maintains this independent Termux fork.

**[Download APK](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.9/Termux-Jobs-v2.1.9-arm64.apk)**

## Changes since 2.1.8

- Agent timelines show compact action rows and detailed output on tap. Earlier
  conversation pages are accessible from the chat.
- Open subagent details from the parent action. Codex can read linked child
  history. Claude shows text directly linked by the CLI to that action and
  preserves bounded excerpts. Antigravity shows reported participant types,
  roles and identifiers. Full native child history is unavailable in the
  Claude and Antigravity bridges.
- Check and install Codex, Claude and Antigravity CLI updates from **Chat
  management** in Ubuntu AI. Installations require confirmation. A running
  chat picks up the new CLI at its next launch.
- New Gemini chats on Ubuntu AI use Antigravity and show the sign-in prompt
  before the first message. Gemini has instruction and skill forms. App-provided
  common instruction templates preserve files that users already created.
- New Codex, Claude and Antigravity chats can initialize Git in the selected
  project folder. Setup requests confirmation for a missing Git package.
  The Changes tab works before the first commit, and exact project paths
  remain intact when selecting a folder.
- The app removes its built-in RTK integration. Existing histories, runtime
  files and user-owned hooks remain in place.

## Compatibility

Android 8 or later, ARM64, version code 1549. The Android package and signing
certificate match v2.1.8. This update requests no data reset or environment
reinstall. Android may stop running app processes during installation.

Existing Gemini profiles retain their connection and can still select API or
Vertex. A new Antigravity profile uses its own Google sign-in. Committing
in a project requires write permission
and an author name and email. Profiles previously saved with a truncated
directory can reselect the original folder.

<details>
<summary>Verification and build details</summary>

The signed ARM64 APK passed metadata, signing, ZIP and runtime checks.
Modern Lint completed across all five modules with zero errors and 299
warnings. The independent unsigned build matched all 1,145 APK entries and
the signature-normalized ZIP. Local JVM tests used the final update metadata.
Claude's linked child response was checked with a real CLI and a synthetic
provider. Antigravity child events were checked with a protocol fixture.
These checks leave clean-phone startup and live provider behavior unverified.

APK SHA-256 `04b532d9c592c18abb2c3213e96ba9c5c6a56aa3923331d464094dc9aaf44271`.

[Release validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/docs/fork/VALIDATION-v2.1.9.md)
· [Reproducibility](https://github.com/HezeAI/termux-jobs/blob/v2.1.9/docs/fork/REPRODUCIBILITY-v2.1.9.md)
· [Source](https://github.com/HezeAI/termux-jobs/tree/v2.1.9)
· [Checksums](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.9/SHA256SUMS)

</details>

AI development assistance: OpenAI Codex
