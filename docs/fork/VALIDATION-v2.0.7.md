# v2.0.7 validation

This record covers the released source and ARM64 artifact. Automated checks and developer-reported device acceptance are separate evidence.

## Artifact and compatibility

- File: `Termux-Jobs-v2.0.7-arm64.apk`; 39,748,664 bytes.
- Package/version: `com.termux`, `2.0.7`, versionCode **1410**.
- SHA-256: `02f1e77c188f53c5a9cb9c7294d6d5ad5ce04d9822be5a7a97a69c6c2c796938`.
- Signing certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ARM64 only; Android 8+ application, Android 11+ X11. Existing package, certificate, SDK requirements and schema8 remain unchanged. Code1410 also permits an update from the developer's recovery build1409.
- No data reset, account migration or environment reinstallation is required. Presets, project files and selected imported drivers remain in place. Missing/changed optional components prepare on the next selected desktop/renderer launch; a running desktop needs stop/start to load different libraries.

The release build passed ZIP integrity, package/version/ABI, non-debuggable, signing identity, DEX/runtime dependency, source-asset and secret-pattern checks. All77 application assets match their source files. The three JNI libraries are unchanged from the accepted implementation. Retired Wayland/KDE assets and classes are absent. No new native rotation/presentation behavior is claimed.

## Automated checks

- JVM coverage: **1,291 passing cases across193 suites**, zero remaining failures/errors/skips. Full application coverage was combined with the other modules. Four fixture classes were rerun using a short repository-local temporary path after the deep isolated checkout exceeded the Unix socket path limit; the production limit was unchanged. Coverage contains only features present in the release source.
- Targeted Python helpers: **133 passing tests**, including default-driver preparation, scoped imports/removal, used-driver cache cleanup, alias-aware paths, optional extras, renderer setup, bridge, scale and theme behavior.
- Localization: **584 strings**, settings keys and format arguments passed.
- Modern lint7.4.2: all five modules analyzed, **0 errors /210 warnings**, no infrastructure diagnostics. No general baseline or blanket disable was added. Remaining categories include SDK/dependency compatibility, text/localization, resource cleanup, private storage/native-library layout and accessibility; targetSDK28 and the `com.termux` installation conflict remain architectural constraints. The known compileSDK/old-AGP support warning remains recorded.
- The isolated signed release build and APK runtime verification passed:13,705 defined classes,31 reviewed optional/framework unresolved types, required runtime definitions present.
- Product/documentation whitespace checks, staged path/content review and component pin consistency passed. The unchanged vendored Mesa patch retains the required single-space empty context lines of unified diff format; its provenance hash is preserved.

The optional-component tests exercise missing/corrupt caches and failure paths, scope locks, the actual Android private-path alias entry point, protected presets and no-follow deletion with auxiliary files/nested runtime caches. Live XFCE reset logic preserves an unset scalar instead of recursively deleting sibling properties. GUI behavior is not established by mocks alone.

## Component distribution

All **12 pinned production component URLs** were compared with published GitHub release asset sizes and SHA-256 digests. Each URL passed an anonymous HTTPS HEAD request with ordinary TLS verification and an HTTPS final redirect. This check did not re-download every component archive.

The immutable sets are `graphics-20261025`, `mesa-26.2.5` and `x11-extras-20261025` in [Termux Job’s Components](https://github.com/HezeAI/termux-jobs-components#readme). The release uses the stable Mesa tag rather than a candidate tag. Ordinary distribution packages continue to use their native repositories. The imported default Bionic driver retains its upstream version; its metadata name does not make it the Mesa26.2.5 frontend.

## Device evidence and limits

HezeAI reported that driver deletion, theme application and live scale changes work in the accepted build. These checks are device regression evidence, not proof of every clean-device installation, GPU/Android combination or network failure. The established desktop/device checks documented in earlier releases remain historical evidence. Broader fresh-device installation and hardware compatibility still need device testing.

The APK keeps the accepted desktop presentation behavior. A new fix for rotation artifacts is not part of this release. Normal Android installer/updater interaction and third-party GPU driver compatibility cannot be inferred solely from the source/build checks.

The release includes `update.json` with RU/EN highlights and compatibility, `SHA256SUMS` and a machine-readable `validation.json`. Publication verification downloads all four release assets and compares their bytes and updater metadata.

## Publication verification

[v2.0.7](https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.7) is published as the latest stable release. The immutable tag points to `621ce4ccd9e6e8e465324d60d960c2a058e8bd5d`. Anonymous HTTPS downloads of all four assets matched local file lengths, SHA-256 values and GitHub asset digests. The downloaded checksum list and updater metadata agree with the APK, including both localized compatibility sections. Release body/credit and tagged README/release/validation/component documentation were also verified.
