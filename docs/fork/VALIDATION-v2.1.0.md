> Historical private candidate (code1351); final release evidence: [v2.0.3](VALIDATION-v2.0.3.md).

# v2.1.0 release-candidate validation — code1351 / 2026-10-24

Public stable remains v2.0.2 until publication. This candidate retains the cumulative
private audit, landscape X11 and optional styles across all eight targets. The
developer reported the earlier result good overall, with Fedora MP4 failure and
a request for a lower/translucent input control. Code1351 addresses those items.
Evidence: `.verification/fedora-media-20261024/`; [focused review](AUDIT-FEDORA-MP4-2026-10-24.md).
[Previous1350 evidence](VALIDATION-v2.1.0-code1350.md) is historical.

## Final APK

| Field | Verified result |
| --- | --- |
| File | `build/apk/Termux-Jobs-v2.1.0-arm64.apk` |
| Version | 2.1.0 /1351; header branding stays v2.0 |
| Package / ABI | com.termux / arm64-v8a only |
| Size | 45,882,878 bytes |
| SHA-256 | `17e9f92b053f4cffffc994d9dc7c9d80b833dcba0db0854aff3137181f77080a` |
| Signer SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |
| Android | min26 / target28 / compile35; embedded X11 requires30 |
| Packaging | Fresh ZIP CRC/signature valid, nondebuggable; same package/signer, higher code |
| Runtime | 13,655 definitions; required missing0, reviewed optional31 |
| Native | Android JNI libraries and existing OpenH264/style payloads match1350 |
| Assets | Changed: x11-native-programs.py/x11-openh264.py; added: pinned GNU GStreamer bridge/full copyright |

Bounded auth/key filename, private-key/token and configured-private-server pattern
checks found no matches in packaged assets/DEX; not a universal secret guarantee.
Changed/new assets match their sources/pins; all494 main files match source-test,
modern-lint and ARM64 build stages. The APK/update.json/SHA256SUMS release copies
agree exactly. Incremental ZIP dead space was removed by fresh packaging; no source
or native code was rebuilt unnecessarily for that packaging step.

## Verification and review

- Fresh **72 JVM cases/6classes**, no failures/errors/skips. Window alpha/offset,
  enabled fill/focus/teardown, exact v2.0 header, service boundaries and Fedora's
  once-only recommended-status schema3 invalidation in the same file are covered.
- Fresh **66 Python cases**, including owned private codec files/proof, partial
  setup and retry, invalid/edited inputs, unchanged openSUSE mode, no setup during
  status checks, explicit media dependencies and command-local DNF repo exclusion.
- Native isolated Fedora tests reproduce baseline decode failure; private setup
  decodes synthetic H.264/AAC MP4 to EOS with5video/47audio buffers. The real helper
  creates a decoder, verifies factories and reports ready on initial/repeated setup
  in a fixture HOME. This is neither a Parole GUI nor fresh-phone install test.
- Five-module lint7.4.2: **0 errors /210 warnings**. The one added warning is the
  intentional GNU GStreamer asset, reviewed with its pin and extraction scope.
  No metadata/rule-loading errors. CompileSDK35 exceeds AGP7.4's tested33, so latest
  API35 lint coverage is not claimed. [Warning categories](LINT-WARNINGS-2026-10-24.md).
- Localization584/key/format parity passes. Previous full JVM1224/184 belongs to
  code1347; code1350's110/61 scoped cases remain historical. Unrelated Python/Node
  runtime suites were not repeated for this narrow fix.

Five main files differ from1350: fullscreen presentation, codec asset routing,
recommended-cache schema selection and two program/codec helpers. Build-input
changes add one pinned GNU plugin and license notices with reproducible extraction.
No permission, renderer, process ownership/stop protocol, auth or workspace schema
change. Fixed per-target paths, ownership/hash/symlink guards, a pending→verified
codec proof and a private registry prevent false readiness or cross-app plugin
registration. DNF signatures/TLS remain enabled; no RPM library replacement or
third-party repo is introduced. Only Fedora's recommendation flags are refreshed;
catalogs, resume state and other environments' metadata remain.

## Device check and compatibility

Stop/relaunch Parole after running Fedora Programs→Recommended→Video and audio→
Install again. Open from the applications menu, then test the reported MP4 and
actual video/audio output. Inspect the lower translucent input control, its fill
and keyboard/Back behavior. A clean phone's complete package-download/GUI path
remains to be checked separately; the installer targets that baseline explicitly
and rejects missing codec proof. Other MP4 codecs/HEVC/hardware decoding and other
devices retain separate limits. Surrogate tests cannot prove IME/composition.

Same package/signer/schema8; no valid-data reset, environment/account migration or
reinstallation required by source/data review. Finish important operations before
updating. Existing HOME-only SAF boundaries, malformed-library read-only behavior,
exclusive outputs and uncertain remote Copy/Move outcomes remain in both localized
update notes and [release notes](RELEASE-v2.1.0.md). Local SSH/proot exit is not proof
of descendant termination. The final broad audit and publication await the new
phone result. No remote release/update asset has been replaced.
