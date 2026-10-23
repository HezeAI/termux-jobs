# Workspace boundaries

The user requires all fork development and generated files to stay in
`/root/termux-fork`. Do not change installed Termux, its data, the shared Android
SDK, or the Ubuntu environment. Do not install APKs, replace apps, start/stop the
user's terminal sessions, or install system packages.

Existing tools and SDK components may be read. Put any additional tools, caches,
SDK components and build outputs inside this repository, in ignored `.tools/`
or `.verification/` directories. Use command-local environment variables only.
Use a local SDK copy for Gradle so that automatic package installation cannot
write to the shared SDK. Verification on an installed app requires separate user
instructions; source edits and isolated builds are authorized.

# Release documentation

Write GitHub release descriptions and new public release notes in English by
default, as requested by the developer. Preserve both Russian and English
in-app update highlights and compatibility notes.

The user requires documentation to be reviewed and updated with every GitHub
release, including README.md. Follow docs/fork/RELEASING.md. Update the changelog,
release notes, validation evidence, version and compatibility information, and
any usage/design/architecture documents affected by the final implementation.
Keep historical reports clearly marked and never claim unperformed checks.
This requirement does not independently authorize publishing a new release.

# Refactoring completion evidence

Before declaring a refactoring complete, assess three independent areas:

- Relevant testing: verify changed behavior and meaningful failure cases.
  A passing existing suite alone is not evidence that a new path is covered.
- Code review: inspect regressions, handling of secrets, permission and trust
  boundaries, and effects on unrelated terminal sessions and processes.
- Logical inspection: trace applicable call paths, state transitions, startup,
  switching, closing, cancellation, and error/recovery handling.

Do not invent a numeric confidence percentage. If an assessment is requested,
testing/review/logical inspection may use 40/30/30 as qualitative priorities,
not a calibrated probability or a substitute for evidence.

A known critical regression, secret exposure, or unresolved safety of affected
process shutdown blocks completion regardless of how many other checks pass.
If evidence is incomplete, report what is verified, the main remaining gaps,
and the smallest next checks needed. Distinguish source/build verification
from device verification: keyboard, gestures and other device-dependent behavior
must not be claimed tested on a phone based solely on mocked UI tests.
Scale checks to the change; do not add redundant tests or broad unrelated
refactoring just to improve an assessment. User authorization for installing
APKs or publishing remains separate from technical verification.

# Local phone files and tool access

Codex runs locally in a Termux or Ubuntu/proot environment. Do not assume it
is a remote chat without filesystem access. Actual access depends on the
session sandbox, available tools, Android permissions and proot mounts;
this file does not grant permissions or bypass any of those restrictions.

On this phone, shared storage has been readable from Ubuntu through `/sdcard`.
Useful paths are:

- Screenshots: `/sdcard/Pictures/Screenshots`.
- Downloads and user-requested APK/PNG delivery: `/sdcard/Download`.
- Possible shared-storage alias: `/storage/emulated/0`.

Check paths in the current environment before relying on them. Termux private
paths are not necessarily visible directly inside Ubuntu. Other phones may
have different mounts or Android permissions. Never claim access is unavailable
without trying an appropriate read-only tool; report the actual error if it fails.

When asked to inspect the latest screenshot, list the screenshot directory by
modification time, select the newest image (not a video), and open it with the
available image-viewing tool. For an exact filename, search for that filename
instead of substituting a different recent image. Example read-only listing:

```sh
ls -t /sdcard/Pictures/Screenshots | head -10
```

Use command/file tools for authorized local inspection, source edits and isolated
builds, and image tools for screenshots. Preserve the workspace boundaries above.
If an image tool is unavailable, state that limitation separately from whether
the image file is readable. Ask the user to attach an image only when required
access or viewing tools are actually unavailable.

Read-only access does not authorize modifications. Full-access sessions still
must respect user instructions; do not install APKs, alter running sessions,
send messages, or publish changes without task authorization. Do not print
tokens, private keys or authorization file contents in reports.

# In-app update compatibility

Every future stable release must remain available to the in-app updater: include
an update.json asset generated from the final APK, keep package/signing identity
for compatible updates, and increase versionCode. Before publication, explicitly
review data compatibility. Put required resets, lost/unportable data, migrations
and other breaking changes with their reasons in BOTH localized update highlights
and release notes, so the installed v1.0.19 updater also displays them before
installation. Never clear user data automatically to make an update work.

# Authorship and validation wording

HezeAI (the user) is the developer of this fork; Codex is an AI assistant helping
with development. Public documentation must not describe the developer as having
not installed the APK merely because the assistant did not install it. Attribute
reported device checks to the developer, distinguish them from automated tests,
and identify specific unverified scenarios without claiming exhaustive testing.
Do not add statements about the assistant not installing APKs to release reports.

# Clean-install baseline for every environment

The developer requires installation logic to target a clean app/bootstrap and a
fresh instance of each selected environment. Never infer required components
from what happens to be installed on the development phone or in an existing
rootfs. Maintain explicit required desktop/runtime packages for each supported
target, including components omitted by a distribution metapackage. Request the
complete required set in the package-manager transaction even if some packages
are already installed; let that manager resolve installed state and dependencies.
Keep optional application dependencies with their own application installation.

Checks must reject a missing required desktop binary instead of reporting a
complete installation merely because a metapackage, panel or window manager is
present. Cover absent-component paths with isolated tests. Treat successful
runs on a configured phone as regression evidence only; report fresh-device
installation and GUI verification separately. Do not clear user data or alter
installed environments to simulate a clean installation without authorization.
