# Private v1.7.38-x11-test / code1253

Package com.termux and original certificate retained; code1252→1253. No data reset,
rootfs creation, installed-app manipulation or GitHub publication. APK is for
HezeAI device verification. Earlier developer checks cover preceding versions only.

## Changed behavior and review

Each supported environment has a fixed nonexported service/process/display/control
directory: Termux97, Ubuntu98, Test Ubuntu99, Debian100, Alpine101. Termux retains its
original runtime directory. Each service guards its own discovery port7900+display
without listening, has separate authentication, notification/Stop intent and native
VM. Shared native library code is byte-identical to1.7.37; it is loaded in separate
processes. Guest DISPLAY is explicitly exported and tested without relying on a
shell-inherited target. XDG runtime directories are display-specific. No profile can
supply arbitrary PIDs/process names or display values. Existing exact legacy private
control path remains accepted by scripts for old internal fixtures; production service
routing uses fixed environment-specific directories.

One desktop/server per environment. Additional tabs reuse the same service and are
registered by exact session ID+handle, including during preparation. Stale handles
and cross-environment closure are ignored. Closing one attached owner retains other
owners; last owner closure locks new work and schedules only that slot to stop.
Explicit Stop stops that environment even with multiple owners. A facade hides
inactive Android surfaces before attaching the selected one; native sessions can
remain running. More concurrent environments consume more memory. Activity teardown
unbinds UI without killing unrelated terminal sessions. Guardian cancellation retains
process-group ownership; no global pkill or profile-derived process kill exists.

Renderer connection begins at server-ready under a transparent startup surface,
before XFCE-ready. This avoids waiting for the manual Show action to establish a
renderer. Loading UI still waits for desktop-ready, and IME input readiness excludes
hidden warmup. Actual SurfaceView/keyboard/gesture/startup timing needs device tests;
no measured startup acceleration is claimed.

Firefox PRoot compatibility accepts Alpine's exact /usr/lib/firefox/firefox launcher
and actions; unrelated executables remain rejected. System .desktop files are kept.
Native Termux/Alpine now offer opt-in Firefox DNS HTTPS, confirmed separately. It uses
https://mozilla.cloudflare-dns.com/dns-query with ordinary DNS fallback; site-name
queries go to Cloudflare. It changes only relative local Firefox profiles, not OS DNS,
TLS validation or browser sandbox. Journal stores original user.js and original DNS
keys from prefs.js, so disabling restores only those keys after Firefox use while
preserving other preferences. Custom edits/symlinks/external profiles/corrupt journals
are rejected without replacement. Browser must be closed; first launch creates its
profile. This is a DNS workaround, not proof that all websites/video playback work.
The earlier Alpine Glycin decoder isolation reduction remains documented in1.7.36.

## Verification

107 Python fixtures passed; the final routing fixture also evaluates guest exports
and verifies distinct displays without launching installed environments. Relevant
fixtures include real guardian process-group shutdown with resistant descendants and
an unrelated sentinel, owner closure, supported target routing, catalog/package
names, component/audio checks, readiness limits, native launchers, profile restore,
interrupted journal recovery, user edits and malicious paths. A dedicated routing
rerun passed14 tests after the export assertion was added.

128 JVM tests passed. Selected tests cover native contract/state, private process identity, distinct
slots, repeated launch ownership, last-close/stale handles, UI/channel reuse, startup,
keyboard state, program dialogs/caches, progress and cancellation. Final count is
recorded with the completed JVM log in .verification/x11-separated.

Isolated read-only HTTPS DNS wire-format probe: HTTP200, DNS response code0, eight
answers for www.youtube.com. Native Alpine Python had failed ordinary DNS earlier;
no system resolver or installed Firefox profile was changed for verification.

ARM64 release build and runtime definition audit passed:13384 classes,31 optional
unresolved references with existing review notes. APK ZIP integrity, package/code/
minSdk26/ABI, original signature, JNI28/1/5 and pinned assets verified. Baseline1.7.5
native libraries and other original assets remain identical except the previously
authorized environment-runtime.sh. New asset x11-firefox-dns.py matches source.
Limited private-marker scan passed; it is not an exhaustive privacy guarantee.
Inherited metadata/deprecation warnings remain; no clean Lint claim.

## Minimum device checks

- Start native Termux and Alpine desktops; confirm automatic show and visible icons.
- Use two tabs of one environment; close one then the last and verify shared lifetime.
- Run two different environments; switch and Stop one, confirming the other survives.
- Check return-to-terminal, keyboard, file/program menus, error/cancel/relaunch paths.
- Close Firefox; toggle compatibility in Alpine. For DNS failures, enable DNS HTTPS,
  reopen Firefox and retry; disable and confirm ordinary DNS settings are restored.
- Quick Ubuntu/Debian smoke test; current agent sessions must remain unchanged.

APK: `/sdcard/Download/Termux-Jobs-v1.7.38-x11-test-arm64.apk`

SHA-256: `8858b4b6538ba9436abe04e6c8003cb191e500529a8162125e765d211d1b28b1`

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`
