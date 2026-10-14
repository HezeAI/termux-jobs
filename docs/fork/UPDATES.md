# GitHub updates — v1.1.0

Common gear → Check for updates / Проверить обновления. Manual checks remain
available at any time. Optional automatic checks run on activity resume if ten
hours have elapsed since the last attempt (including failed attempts); default on.
They do not schedule a service/alarm while closed or interrupt the user with a
popup: a gear dot/menu version indicates an available update. Failed automatic
requests stay silent; manual checks can retry without waiting. Only the public HezeAI/termux-jobs latest stable release is consulted;
no GitHub login, CLI token, shell command or user data is sent. GitHub receives
ordinary network request metadata such as the IP address.

A release must include update.json (schema 1), the ARM64 APK and SHA256SUMS.
The updater compares Android versionCode, displays localized highlights, checks
Android/ABI compatibility and downloads on demand into a private cache. Size and
SHA-256 must match metadata; package, actual APK version and signer must match the
installed fork, with a strictly increasing code. Redirects are HTTPS-only and
restricted to GitHub API/release asset hosts. Android performs the final APK
signature verification and asks the user to confirm installation. Matching a
hash from the same GitHub release is an integrity check, not an independent trust
anchor; the installed signing certificate and Android installer are the boundary.

Cancelling a check/download stops its result; a blocking network read may finish
or time out before the worker exits. Failed/partial downloads are removed. There
is one worker at a time; no service, wake lock or terminal process is started.
Leaving the activity does not make installation automatic. Verified APKs remain
in cache until the next download or Android cache cleanup; no reports/projects
are removed. Pending Android installer flows should be finished before starting
another download. Android 8+ may require allowing installs from this app; then
return and press Install again. The permission is not granted silently.

In-place updates normally preserve HOME, PREFIX, Ubuntu, presets, projects and
agent authorization; uninstall/clear-data does not. No destructive data migration is added in v1.1.0; clearing libraries/history is
not required. Installation may terminate running app processes, so finish important
jobs first. The app never uninstalls itself or promises universal migration
compatibility. A fork with another signing key cannot update this build.

First install of v1.0.19 is manual; older builds cannot acquire this feature
without an APK update. A release missing metadata is reported explicitly.
End-to-end upgrade still needs a genuine later release and device confirmation.

The duplicate OpenCode connection entry is removed from global app settings;
session/preset connection paths remain. App logger default changes to OFF, and a
one-time startup migration changes the previous NORMAL level to OFF. Explicit
DEBUG/VERBOSE levels survive, and later user choices are preserved. This does not
stop Android's system logging or session diagnostics. Key event logging is a
separate existing preference and is not enabled by this change.

Each release must include explicit compatibility highlights in both languages.
The generator merges compatibility text into changes for existing v1.0.19 readers.
The first end-to-end in-app upgrade v1.0.19→v1.1.0 is pending user verification.
