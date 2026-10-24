# v2.0.3 final audit — 2026-10-24

Scope: cumulative changes since public v2.0.2/code1331, including the stability
review, fullscreen landscape X11, optional appearance in eight environments and
Fedora Parole MP4 setup. HezeAI approved publication after reporting that the
latest candidate works, including MP4. Release code1352 changes version fields;
all494 production source/resource files, packaged runtime assets and Android JNI
libraries match the developer-tested code1351. This does not turn that check
into exhaustive fresh-device or renderer validation.

## Findings included

| Area | Corrected behavior and evidence |
| --- | --- |
| Agent chats | Run/session-scoped OpenCode control, stale-send refusal, safe pre-dispatch Gemini retry and Codex login retry; [review](AUDIT-AGENT-CHATS-2026-10-23.md). |
| Environment and X11 operations | Cancellation/exception paths wait for owned-worker cleanup before allowing another package operation; [X11 review](AUDIT-ENVIRONMENTS-X11-2026-10-23.md), [installer review](AUDIT-INSTALLERS-2026-10-23.md). |
| Backups and cleanup | Confirmation follows the current selection; cancelled/closed jobs cannot commit stale results; [storage review](AUDIT-STORAGE-2026-10-23.md). |
| Persistence | Shared FIFO AtomicFile writer, snapshots across overlapping Activity lifetimes and read-only malformed duplicate-ID libraries; [review](AUDIT-PERSISTENCE-2026-10-23.md). |
| Files, transfers and recovery | Exclusive outputs preserve existing files, completion is validated and interrupted remote Copy/Move is never silently replayed; [transfers](AUDIT-TRANSFERS-2026-10-23.md), [file manager](AUDIT-FILE-MANAGER-2026-10-24.md), [transport](AUDIT-FILE-TRANSPORT-2026-10-24.md), [recovery](AUDIT-FILE-RECOVERY-2026-10-24.md). |
| Updates and entrypoints | Operation-owned cancellable HTTPS, trusted redirect hosts, final APK identity/signature checks and bounded external files; [review](AUDIT-UPDATES-ENTRYPOINTS-2026-10-23.md). |
| Android providers and settings | Descriptor/root/name validation, HOME-only SAF, protected property-file aliases and removal of Activity-retaining preference singletons; [providers](AUDIT-PROVIDERS-2026-10-24.md), [settings](AUDIT-SETTINGS-2026-10-24.md). |
| Locale, images, notifications and analysis | Locale-independent machine values, updated EXIF parser, accessible image clicks, immutable fixed notification actions and independent five-module lint; [review](AUDIT-LINT-2026-10-24.md). |
| Landscape and appearance | Same desktop host moves to an immersive window; keyboard changes crop, Back/portrait restores it, controls retain input state. Styles use each target's own HOME/defaults and preserve edits; [landscape](AUDIT-X11-LANDSCAPE-2026-10-24.md), [rotation](AUDIT-X11-ROTATION-FIX-2026-10-24.md). |
| Fedora MP4 | Pinned private OpenH264/GStreamer bridge, explicit runtime packages, private registry and pending-to-verified readiness; [focused review](AUDIT-FEDORA-MP4-2026-10-24.md). |

## Final review and state transitions

Reviewed startup, retry, cancellation, exception cleanup, tab/window changes,
closing and recovery against the focused audit findings and regression fixtures.
The fullscreen window reparents the existing host and cannot start/stop an X11
process. Only its visible desktop owns orientation and input overlay; closing
removes observers/windows and restores the previous policy. CPU/Turnip/VirGL/
wrapper selection and renderer binaries are retained.

Component jobs retain their cancelling/busy state until their guardian is reaped;
file transports only terminate their directly owned Process object, never a
borrowed process-group ID. Existing guardian ownership/protocols remain. This is
not a guarantee that every remote SSH/proot descendant stops after disconnection:
that uncertainty is explicitly shown/documented rather than automatically retried.

The release retains package/signing identity, schema8, agent account stores and
rootfs locations. No valid-data reset or automatic migration is introduced. The
intentional HOME-only documents-provider restriction, exclusive export behavior,
read-only malformed libraries and uncertain remote results are described in both
RU/EN updater compatibility notes before installation.

Pinned third-party assets and notices are tracked; no personal key, auth store,
local SDK, generated fixture, private server address or build cache is included
in release inputs. Bounded source scans only found existing synthetic secret
markers in four test fixtures; artifact scans found no corresponding secrets.
This is a scoped review, not a guarantee against every possible secret pattern.

## Checks and limits

The [release validation](VALIDATION-v2.0.3.md) records final test counts, modern
lint findings, package/runtime checks, APK hash and signer. Historical per-stage
reports above describe their own builds; they are not relabelled as fresh runs.
The final broad suites and source-stage comparison bind current checks to this
release. Native Fedora decoding reached EOS with5video/47audio buffers; the
subsequent developer check confirmed the reported MP4 works in the app.

Outstanding work remains: target-SDK/package-ID/sharedUserId architecture,
accessibility, documented lint debt, force-kill/power-loss durability, live-provider
account-specific recovery, every clean device and Mali/Dimensity/other-driver
combinations. The source targets clean installations with explicit required
components; existing-phone success cannot prove a fresh-phone install.
No known blocking regression or unresolved change to the process-stop safety
mechanism was found in this final scope. Public release requires all recorded
final commands and artifact checks to pass; authorization was given by HezeAI.
