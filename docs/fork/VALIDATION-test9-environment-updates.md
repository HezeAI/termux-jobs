# Private test9: environment updates and embedded links

2026-10-29. Candidate 2.0.10-test9, versionCode1440. HezeAI is the developer;
OpenAI Codex assisted with implementation. Private candidate only.

## Changes

Settings gains explicit package checks/upgrades, Components base-image discovery,
restoring the APK-pinned baseline, private checkpoints and rollback for nine
ordinary environments. Native Termux and Ubuntu AI retain their existing policy.
Applying changes requires native exclusive alias locks; it never stops the user's
sessions or bypasses package-manager locks. An upgrade runs in a full copy. One
atomic exchange publishes it; the complete previous container is retained.
Rollback can preserve current /root and /home and retains the complete current
state in another checkpoint. See [behavior and limits](ENVIRONMENT-UPDATES.md).

Arch/Manjaro plain rootfs archives have no OCI manifest. Their helper path uses
the public native login command; OCI containers use run with a checked empty
entrypoint. Treating every native container as an OCI image would incorrectly
hide the plain-rootfs installations and prevent operations on them; this case is
covered separately. Android filesDir aliases are accepted by filesystem identity,
while foreign PREFIX redirects and impostor anchors are rejected.

The shared Wiki toolbar gains a bordered Forum button immediately before Reload.
HTTPS links from every documentation source stay inside the reader. Long-press
offers Copy link / Open in browser, including correct anchor resolution for an
image link. Agent chat link previews gain Copy / Show / Browser; Show loads the
explicit HTTPS URL in an independent reader history without first fetching an
older saved page. HTTP retains explicit Copy/Browser and cannot use embedded
Show. Local files/content/intents/scripts, TLS bypasses, native JavaScript bridges
and automatic external application launches remain unavailable.

Kali's four added Nmap/Wireshark/Burp Suite/Apktool recommendation cards are
removed as requested. Installed packages, installer/cache compatibility and
existing launchers remain. The old Manjaro CPU/VirGL-only assertion is updated
to the already added separate Turnip and Bionic builds; this does not change
their renderer binaries or pins.

## Automated source checks

Evidence is in .verification/environment-updates-1440/:

- filesystem-tests.log:25 tests of verified copies/atomic exchange, upgrade
  failure, source mutation, held locks, expired plans, home-preserving and exact
  rollback, reverse safety checkpoints, collision refusal, internal l2s
  relocation, no-follow deletion with nested caches/external symlinks,
  hardlink/control boundaries, interruption before/after exchange and after
  moving the old tree, plain-rootfs routing and bounded package/catalog formats.
- process-tests.log:4 actual owned-process-group tests: cancellation,
  descendants retaining stdout after their leader exits, timeout/signal-before-
  reap ordering, output bounds and an unrelated guard process remaining alive.
- native-apt-tests.log:real APT/dpkg, script-free synthetic packages and an
  initially empty private package database/cache. Install1.0, check2.0, upgrade
  the working copy, recheck, rollback. All configuration, state, root, logs,
  repositories and caches are redirected to disposable project fixtures.
  The host dpkg status SHA-256 is unchanged. No phone rootfs or nested PRoot GUI
  is used to run these transactions.
- helper-entry-tests.log:the actual Python CLI accepts an equivalent filesDir
  alias and refuses an impostor, without mutating native installed environments.
- Focused JVM tests cover update UI states/confirmation/dismissal/early and stale
  Stop requests, owned alias visibility through private-directory aliases,
  documentation layouts, in-reader navigation/retention, explicit chat-link
  loading, long-click/anchor callbacks, copy/browser actions, four-provider chat
  link behavior, current session actions and Kali recommendation removal.

The WebView test provider does not implement Chromium drawing, geometry or
native touch delegation. The existing bounds shadow and the new long-click
View delegate are explicit test adapters. The tests validate application
decisions and callback identity, not real-device rendering/gesture recognition.

## Review and logical inspection

Mutation paths hold the original native alias lock, copy and verify files,
recheck approved candidates, execute only in the temporary alias, validate,
then acquire its exclusive lock before exchange. A changed source identity or
file tree refuses publication. Cancellation markers affect only their own job.
An old notification/Stop request cannot cancel a subsequent job. Service shutdown
and releasing the operation slot are ordered to avoid stopping the next job's
foreground service. UI dismissal stops polling, not the owned job.

Journal recovery distinguishes old/new directory identities and never uses a
two-rename gap. Unknown identities and interrupted native installations without
a recorded temporary identity are retained for manual review. Errors after an
exchange can require receipt recovery even though the new tree is already active.
Only typed stages/errors/package candidates are emitted; raw package stdout,
credential files and source URLs with credentials are not copied into UI logs.
No checkpoint is published/uploaded automatically.

The viewer opens remote web pages only following user navigation or Show.
Ordinary Markdown remains native. HTTPS-only policy, certificate checks, blocked
file/content access, permission denial and no automatic external intents apply
to both documentation and chat links.

## Compatibility and remaining device checks

Packagecom.termux, stable signing identity, ARM64 and workspace schema8 remain.
Code1440 is higher than delivered test8/code1439. There is no data reset and no
automatic environment/package update. Existing image/desktop/renderer pins and
all three JNI libraries remain unchanged from test8.

Real online transactions for APK/DNF/Zypper/XBPS/Pacman, full baseline replacement
through native PRoot and every distro's fresh-device GUI are not established by
the APT/filesystem fixtures. Phone checks remain for notification/closing-app
lifecycle, opening/closing sessions during an update, live rollback/restart,
forum/site rendering, long-touch recognition and the three chat-link buttons.
The smallest first phone trial is a stopped disposable environment: check,
upgrade, reopen, rollback and reopen. Keep unrelated sessions open to verify
they remain unaffected. Do not treat this private candidate as an exhaustive
cross-distribution acceptance result.

## Final candidate results

- 54 focused release JVM tests passed, including the first-open viewport and
  stale callback layout cases, embedded links, program recommendation removal
  and update confirmation/cancellation states.
- Independent lint7.4.2:zero errors, 225 warnings. All five modules analysed;
  no infrastructure diagnostics. Final production source matches the analysed
  stage and the APK build stage.
- Release APK assembled; ZIP integrity, nondebuggable package/version, ARM64,
  stable certificate, required runtime types,92source assets and secret-pattern
  checks passed. All94previous assets and all3JNI libraries are byte-identical
  to test8; the only added assets are the six update helper modules.
- Live Components metadata discovery returned rootfs-arm64-20261028 with all
  nine aliases and exact matches to the APK's SHA-256 pins.
- Final packaging starts from a fresh package output rather than carrying
  incremental ZIP slack. No source or binary behavior changed during repackaging.

APK:Termux-Jobs-test9.apk, 39881535 bytes, versionName
2.0.10-test9, code1440.
SHA-256:acf56abce3d322af79fb7b8315cee5401576118d3c705bbb3f248b768e52a036.
Certificate SHA-256:dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644.

After a system replacement, program/catalog displays can still use their
existing cache. Use Programs → Check installed programs (or refresh the catalog)
to obtain current package state. This existing cache policy also applies to
manual package changes in the terminal; the updater does not claim to refresh
every live X11 service's program cache automatically.
