# v2.0.0 validation — Termux-job's X11 AVAILABLE

Date: 2026-10-23. Release successor to public v1.7.5. Final APK is built from
this release tree, including the italic header and v2.0 label. Developer HezeAI
reported device checks; automated tests and device observations are separate.

## Artifact and update identity

- APK: `Termux-Jobs-v2.0.0-arm64.apk`.
- Bytes: 45794028.
- SHA-256: `492626100bb3500145fa84f28d030b8cac3e00841967ceff64cf080fae6b5dda`.
- Package: com.termux; versionName2.0.0; versionCode1320; minSDK26; ARM64 only.
- Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP CRC/unique entries, package/ABI/version/minSDK, no debuggable flag, v2
  signature and hash checked. Runtime audit:13552 definitions,31 previously
  reviewed unresolved types. Native registrations match JNI28/1/5 contracts.
- All native libraries, Alpine private client and189-entry Kali style pack are
  identical to private1.7.104. Only the expected X11 credit notice and shell whitespace cleanup changed;
  app code/header and version metadata were rebuilt. No new guest diagnostic
  library or retired Touch executable is packaged.
- Known private-key/token/personal-server markers and auth/keystore filenames
  were checked in the APK. No match was found. This is a limited marker/inventory
  audit, not proof that any possible secret can be detected automatically.
- No general data reset, schema8 retained; no automatic source Ubuntu deletion
  or agent migration. Existing environments, auth and histories are retained.
  Package/signer/code support in-place updates; all user-state migrations and
  the Android installation flow are not established by archive checks alone.
- In-app update.json is generated from this exact APK, with RU/EN highlights,
  compatibility notes and the v2.0.0 asset URL. Its size/hash/version must match
  the uploaded APK. Finish active work before installing: Android can terminate
  app processes during an update.

## Automated tests

- Final JVM run: **862 app +154 terminal-emulator tests**, no failures/errors.
  terminal-view and termux-shared have no tests in this configuration. Logs:
  .verification/release-v2.0.0/java-tests-final.log and java-results.json.
- Python full discovery: **544 tests**, including **one skipped** historical or
  host-prerequisite case. Initial discovery found three outdated expectations:
  required Termux exo-open fixture and the added Arch service. Those fixtures
  were corrected without relaxing runtime checks; **34 affected-module tests
  then passed**. This is full discovery plus targeted successful reruns, not a
  claim that a second unchanged complete discovery was executed.
- The JVM diagnostics fixture also still considered openSUSE unsupported. It
  was updated to expect an unbound startup snapshot, preserving its no-create/
  no-bind safety assertion; the subsequent full JVM run passed.
- The Termux native fixture now provides exo-open explicitly rather than
  accidentally finding a host command. Missing exo-open is tested as not ready.
- An additional18 program tests passed after whitespace-only shell cleanup.
  Localization:584 workspace mappings plus all app/shared key/format checks pass.
  git diff --check and changed shell syntax checks pass.
- Prior component reports cover X11 process separation, keyboard/focus/lifecycle,
  selected-rootfs package/install paths, clean-baseline absent components,
  migration staging/locks/hashes/errors, archive validation, renderer routing,
  owned process groups, server failure/cancel and preserving a neighbouring
  desktop. These are fixtures and isolated probes, not all-device GUI proof.

## Developer device checks

**Tested on RedMagic 10 Pro and Samsung S26 Ultra. Other devices require testing.**
Checks were performed by the developer during the preceding private builds;
coverage differs by device. Every renderer/program/fresh-install combination
was not established on both phones. The final header/label is new to this
release artifact and has not been claimed visually tested on-device.

The developer confirmed desktop operation and graphics checks across Termux,
Ubuntu, Debian, Alpine, Fedora, openSUSE, Void and Arch Linux ARM during the
step-by-step cycle. Recent VirGL checks include:

- Alpine/Fedora/Void confirmed after101; Arch/openSUSE confirmed after102.
- Debian103 screenshots113744/113750: real XFCE panels/icons/windows, EGL info
  virgl/ANGLE Vulkan Adreno830, Mesa25.0.7; glmark2 scene and159–241FPS stages.
- Termux103 screenshot113849: GLX direct rendering Yes, Accelerated yes,
  virgl/ANGLE Vulkan Adreno830, Mesa26.2.5. Developer then confirmed GPU test works.
- Ubuntu103 screenshot114203: XFCE and glmark2 scene, virgl/ANGLE Adreno830,
  Mesa26.0.8 and201/214FPS stages. GPU INFO failed BadMatch/X_GetImage.
  Private104 switched only Ubuntu VirGL info to the established bounded EGL
  helper; final GUI recheck of this information query is still pending.

Final benchmark scores, long-duration stability and all clean-phone installation
paths are not inferred from screenshots. The new common VirGL helper passed
all-eight-target route fixtures; Turnip/Wrapper flags are cleared on entry.
Full final CPU/Turnip/Wrapper switching regression and Mali/Dimensity support
remain device-dependent checks. Claude/Vertex end-to-end account workflows
retain their previously documented unverified status.

## Review and logical inspection

- New target operations use selected aliases, private paths and process-local
  renderer variables. PRoot guests retain their own libc/ABI; they are not a
  security sandbox from the Android app's own permissions.
- Guardians own children and groups from their launch. No arbitrary PID adoption
  or global pkill/killall was introduced; target-close/cancel/error routes are
  checked by the lifecycle tests. Android can still terminate the app globally.
- Missing mandatory components fail readiness. Base installers request complete
  required sets; optional fixes/dependencies stay with the selected application.
- Migration/removal block busy source/destination; secret-copy consent, scan
  identity and read-back hashes guard staged publication. Personal configs,
  .tools caches, SSH keys and signing files are excluded from the source release.
- A private server address in a test fixture was replaced with a documentation
  address before publication. Remaining key/token-shaped source matches are
  synthetic log-redaction fixtures, not credentials.
- Per-program Resume reuses native package-manager state. It does not promise
  uninterrupted background work or byte-range resume for every package manager.
- Renderer check → consented install → launch → switch/close/error remains
  target-scoped. Personal connection command affects only a new agent launch.

## Sources and build limits

See [source credits and provenance](THIRD-PARTY.md). Upstream native X11 code is
unmodified and pinned; the fork does not claim a native-core rebuild. The current
isolated build was run on the ARM64 phone toolchain with repository-local caches
and SDK. A fresh remote/x86 CI build was not validated here; historical CI scripts
are not evidence of a complete current X11 cross-toolchain setup. Mutable pinned
upstream URLs fail closed if replaced, rather than silently accepting new input.
The reviewed Alpine client archive and all required source assets are committed.
The cached public1.7.5 APK was also inspected: com.termux, code1220 and the same
certificate match the new1320 release; no uninstall/reset workaround is needed.

The earlier private validation reports are historical evidence, not additional
unperformed checks of this final artifact. No phone reboot or agent-session
restart was used for release preparation. GitHub publication verification is
recorded below once release assets are retrieved and compared.
