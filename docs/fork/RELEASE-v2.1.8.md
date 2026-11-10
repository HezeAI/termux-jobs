# Termux Job’s 2.1.8

Clearer action cards, session states and notifications.

[Download APK](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.8/Termux-Jobs-v2.1.8-arm64.apk)

## Changes since 2.1.7

- **Readable action cards:** short names for reading files, searching, checking changes,
  running tests and building. The actual command appears below the title in muted
  monospace text, limited to two lines. Tap for command, output and completion details.
- Native tool descriptions take priority. Otherwise, conservative local classification
  supplies the name without another model request; unknown and compound commands use generic labels.
- **Session states:** running chats and terminals have green status labels; stopped
  sessions have red labels. The base terminal is labelled Base. Names and paths keep their colors.
- **Permissions and approvals:** a shield icon with an exclamation mark.
- **Notifications:** use Termux Job’s and Desktop. Idle graphics control services clear
  their notification after work finishes, while active desktop work remains protected.

## Compatibility

Android 8+, ARM64; versionCode **1540**, same `com.termux` package and signing certificate.
No data reset or environment reinstall required. The RTK component is unchanged.
Finish important work before installing: an Android application update can stop its processes.

<details>
<summary>Verification and build details</summary>

The developer checked action cards, running-state display and session labels on a phone.
The exclamation mark inside the shield was added afterward; it is covered by resource/build
checks, not a separate phone acceptance. Full Android desktop lifecycle recovery remains
outside that reported phone check; local lifecycle fixtures are documented separately.

The signed release APK is built and checked locally. Debug CI and local release checks
are separate. See the version report for exact results, artifact hashes and limitations.
Release reproduction is a source/build comparison, not an independent security audit.

[Validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.8/docs/fork/VALIDATION-v2.1.8.md) ·
[Reproduction procedure and results](https://github.com/HezeAI/termux-jobs/blob/v2.1.8/docs/fork/REPRODUCIBILITY-v2.1.8.md) ·
[Checksums](https://github.com/HezeAI/termux-jobs/releases/download/v2.1.8/SHA256SUMS)

</details>

AI development assistance: OpenAI Codex
