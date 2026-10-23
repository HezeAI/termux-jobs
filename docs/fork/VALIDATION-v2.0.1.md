# v2.0.1 — audit-fix validation

Date: 2026-10-23. Developer: HezeAI. Publication authorized for v2.0.1.
Uploaded-asset verification is recorded separately after publication.

## Artifact

- `Termux-Jobs-v2.0.1-arm64.apk`; package `com.termux`; code1325;
  Android8+/ARM64; inline desktop Android11+.
- Bytes: 45794876. SHA-256:
  `a0a2e86abfb3da4f91e24d3e3c824ecc6f0ca537d2f71fa04fb8f86361ebb7ca`.
- ZIP CRC and unique entries passed; ARM64 only. Valid APK v2 signature;
  signer SHA-256
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`,
  matching stable v2.0 and recovery1324. Code1325 is greater than both.
- Native libraries are byte-identical to stable v2.0. The X11 native core is
  not rebuilt/patched. The window-overlap mitigation selects its existing
  `-disable-gpu-present` path for non-composited GPU desktops. GPU application
  rendering, ICDs, ANGLE backends and compositor choices remain unchanged.
- Modified/related X11 assets were compared byte-for-byte with final source.
  Known private-key/token signatures
  and sensitive filenames had no matches; this limited scan cannot prove the
  absence of every arbitrary credential.
- DEX inventory:13551 definitions, required runtime definitions present,
  31 previously reviewed optional unresolved types, no new unreviewed types.
- update.json was generated from this exact APK with RU/EN highlights
  and compatibility notes. Publication/download checks are recorded separately.

Evidence: `.verification/fix-v2.0.1/artifact-review.json`, `apk-badging.log`,
`apk-signature.log`, `update.json`, and `.verification/arm64/apk-runtime.json`.

## Testing and build

- Full Java verification: **869 app +154 terminal-emulator tests**, zero
  failures/errors. Other requested modules have no test sources.
- After the final numeric JSON round-trip correction, **35 backup/import/store
  tests passed**. This targeted rerun covers the final change; a further
  unchanged full Java rerun was not performed.
- Full Python discovery: **546 tests, one skipped**, no failures. After the
  final renderer/cache changes, **303 X11 tests passed**, including consented
  repair, corrupt state, selected-target routing, no-follow paths and owned
  shutdown. The 15-test GPU repair suite also passed independently.
- All **21 Node suites passed**, including synthetic loopback-provider native
  CLI tests. These do not establish real account/network end-to-end behavior.
- Localization:584 workspace mappings plus app/shared keys/formats. Changed
  Python and shell syntax, and git diff whitespace checks passed.
- Final ARM64 release build passed. Existing ARM64 host JNA/filesystem and
  Kotlin metadata warnings were emitted by the old Android lint toolchain;
  some newer AndroidX custom lint registries could not load. This build is not
  evidence of complete AndroidX/custom lint coverage. Toolchain modernization
  was not mixed into the functional fixes.

Evidence: `.verification/fix-v2.0.1/java-final.log`, `java-results.json`,
`backup-final.log`, `backup-results.json`, `python.log`, `x11-final.log`,
`gpu-repair.log`, `renderer-readiness.log`, `node/results.json`,
`build-final.log`. Initial Java verification identified the older fixture that
accepted an empty GPU marker without backend validation; it was changed to
supply explicit authoritative readiness for the valid-cache case. The failed
initial run is retained as `java.log`, not presented as a product success.

## Review and state transitions

- GPU mitigation: the pinned production scheduling function was compiled with
  stubbed X11/Android dependencies. Disabling GPU Present offload prevents an
  unclipped copy from entering its queue; the caller then selects the standard
  X copy path. Per-target Java tests cover the flag, unchanged CPU/composited
  Termux Turnip choices, display/auth arguments and no cross-launch mutation.
  This is not execution of Android rendering or a frame-rate benchmark.
- Backup restoration keeps every incoming preset's fresh ID and its existing
  copied-history path. Same-named agent configurations remain distinct. Saved
  preset content is verified after JSON serialization; numerically equivalent
  Integer/Long values compare correctly. The schema stays8. Command/note merge
  and inactive-preset replacement rules remain intact; previous user histories
  are not overwritten or automatically reassigned/deleted.
- Desktop check/install reports base and renderer readiness separately. Only a
  matching target/renderer's authoritative success can skip Turnip/VirGL setup.
  A marker is a hint, not a replacement for that check. A failed renderer check
  offers its consented setup instead of falsely accepting it as ready.
- Turnip repair holds the cache install lock, rejects symlink/non-directory
  roots, downloads/verifies in a new stage and only then replaces the selected
  managed cache. Download/verification failure preserves old contents. Cleanup
  uses the symlink-resistant rmtree implementation; an unsafe/failed cleanup
  retains old cache data and emits a warning rather than following links.
  System Mesa and other target caches are not replaced.
- Actual desktop launch retains its independent fail-closed renderer check.
  Guardian/owned-process and per-target service ownership were not replaced.
  Existing cancellation, failure and neighbour-preservation fixtures passed.

## Compatibility and required device checks

Package/signer/code permit updating over recovery1324. Existing data is not
reset or migrated automatically; real Android installation/data retention is
not proven by archive checks alone. Repeated backup imports intentionally
create further preset copies, even with identical names/settings. New X11
arguments/scripts apply on the next desktop start.

The developer's historical RedMagic10Pro/SamsungS26Ultra checks apply to the
preceding v2.0 implementation. New device verification must check Termux Wrapper
with overlapping GPU-test and terminal windows, focus/drag/resize/minimize,
keyboard and return from other tabs. Compare frame rate/CPU cost: standard
clipped copying can cost more CPU than native GPU-copy offload. Then check the
non-composited VirGL/guest-Turnip paths and unchanged CPU/composited Termux
Turnip as appropriate. No on-device success or performance improvement is
claimed from mocked/native-fragment tests.
