> Historical v2.0.5 experiment. Wayland/KDE was retired in v2.0.6. These checks do not describe a current desktop mode.

# Historical private candidate validation — test.2

Follow-up: test.3 fixes manual-resolution rotation and adds XFCE interface scaling;
see [validation](VALIDATION-X11-SCREEN-SCALE.md).

Candidate: **2.0.5-wayland-test.2**, versionCode **1368**.
Stable rollback reference: v2.0.4/code 1366. This is not a stable GitHub release.

## Automated and isolated checks

- Selected Java/resource tests: **300 passed**, no failures/errors/skips
  across 41 classes. Includes screen preferences/UI validation,
  profile selection/isolation, bounded input, service ownership and lifecycle paths.
- Full-screen/input regression classes: **32 passed**, no failures/errors/skips.
  These use surrogate Android views and do not prove physical SurfaceControl/IME behavior.
- Wayland isolated Python/shell checks: **10 passed**. Includes missing binary/package
  rejection, full Arch package closure, no-follow control paths, separate driver
  cache, hash/tamper/cancel/import-lock checks, editable KDE defaults, strict launch
  shim, profile lock/release and shipped keyboard compilation.
- Existing X11 launcher tests: 15 passed; wrapper tests: 20 passed. The wrapper fixture
  now reuses identical ZIP bytes for its repeated-import test instead of depending
  on ZIP timestamps. No installation transaction ran in a real environment.
- Native hardware probes in repository-private child processes: Linux Turnip GPU
  DMA-BUF exported and presented by the selected Bionic driver to an Android
  ImageReader; expected RGB 32/160/224 observed; device feedback matches KGSL;
  keyboard FD equals shipped map; arrow and Russian-key events delivered.
  Both client/server exit 0; exact child cleanup recorded. Invalid-driver probe
  failed without a listening ready socket. Arch-library GBM/EGL/OpenGL and buffer
  FD export also passed (Mesa 26.3.0, Adreno 830). These are component probes, **not KDE GUI**.
- Modern lint 7.4.2/AGP 7.4.2, five modules: **0 errors, 213 warnings**.
  Three new reviewed warnings: guest glibc preload intentionally packaged as an
  asset rather than Android JNI; pointer SurfaceView lacks Android performClick
  semantics; canonical decimal dimension text is not locale-formatted. Existing
  toolchain/compileSDK analysis limits remain documented in LINT.md.
- Release APK built using the established isolated Gradle 7.2/AGP 4.2.2 flow.
  Archive CRC, `com.termux`, version 1368, release/non-debug state, ARM64, signing
  identity, native/guest hashes and source assets checked. Existing native libraries
  match stable v2.0.4 byte-for-byte. Required DEX runtime types and Wayland JNI
  methods/callbacks survive shrinking. Personal-key/token/server-IP pattern scans
  found no matches; no private environments, rootfs or driver ZIPs are bundled.

APK bytes: **52703001**. SHA-256:
`1b30d92fb8fe3335a51862b206245354682cedf5172be6d1a2d6fd6670e0d017`

Certificate SHA-256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`

## Review and state inspection

X11 defaults retain automatic mode with zero margins. Fixed dimensions are
validated before native calls; width follows the existing 8-pixel CVT alignment.
Only the attached host gets new geometry. Settings persist per environment and
are passed to its private service; the service does not read another process's
SharedPreferences cache. Manual frame fitting preserves aspect ratio; real Android
surface margins reserve space independently of framebuffer dimensions. Wayland
continues automatic sizing and ignores X11 geometry preferences. No global display,
shell environment, renderer selection or rootfs changes are made by these controls.

Wayland setup is Arch-only, with explicit guest packages and independent Bionic
import state. Its run lock is shared with Arch X11, not with other distributions.
Device/preload changes are limited to the guarded KDE child tree. The Android
native core starts only on a valid owned surface, reports startup failure, refuses
an invalid imported driver without falling back, and retains a shared catalog lock.
Tab/full-screen transitions reattach surfaces; Stop retires the owned child group
and exact dedicated VM. No imported PID, public proxy or public compositor port
is used. Original automatic shared-storage logs/test-input FIFO are disabled.

## Remaining device checks and compatibility

**Actual KDE GUI launch is unverified.** Test with a backed-up Arch rootfs:
install KDE, import/select a compatible Bionic Turnip ZIP, start KDE, then verify
Dolphin/Konsole, pointer, English/Russian IME, extra keys, Xwayland, rotate/tab/return,
stop/restart and the missing-driver/error paths. Check a clean Arch separately.
Other scripts and Xwayland IME need specific validation. Runtime metadata left by
abnormal process death is not garbage-collected yet. Android native component
probes ran on this phone; older Android versions and other GPUs are not verified.

For each X11 environment, test Auto,1300×600 (effective1296×600), e.g. 16–24 dp
margins, portrait/full-screen, pointer targeting at edges, keyboard show/hide,
tab return, apply/cancel and re-open. Source tests do not prove these physical
Android transitions. Existing X11 renderers/agents were not restarted for checks.

Same package/signer/schema 8; no workspace reset or automatic migration. The KDE
installer performs a full Arch upgrade: **raising the APK version to roll back
cannot undo package upgrades**. The stable checkpoint contains APK/source only.
No APK installation or GitHub publication is part of this validation.
