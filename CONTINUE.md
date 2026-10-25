# Current work: v2.0.6

Developer authorized stable v2.0.6 publication, including Wayland removal, chat/IME fixes, live scaling for seven PRoot targets and isolated graphics downloads from the separate HezeAI/termux-jobs-components repository. Final release code1395; header staysv2.0. No APK installation or unrelated session/process changes are authorized.

Follow docs/fork/RELEASING.md. Final artifact/evidence is in .verification/release-v2.0.6 and build/apk/Termux-Jobs-v2.0.6-arm64.apk. Component source archives/manifests/licenses are prepared separately. Do not replace published immutable component bytes/tags. Verify public download hashes and updater metadata after publishing. Device acceptance still needed for the new component-download path on fresh installs and Adreno740/750 Wrapper setups; do not infer universal GPU compatibility.

Temporary developer-authorized live Arch scale test added only the exact marked allow-UID file /etc/dbus-1/session-local.conf to installed Arch while it was stopped. Evidence and exact hash are in .verification/x11-scale-investigation/arch-live-patch.json. Production v2.0.6 uses a per-desktop private config instead. Consider guarded cleanup of that one test file only after the developer has updated and verified production scaling; do not break the working pre-update APK, edit other policy or stop sessions.

Private Wayland investigation files are retained only under ignored .verification/release-v2.0.6/retired-wayland-docs. Published v2.0.5 history remains available by tag. Never reset user data or uninstall shared Qt/Mesa dependencies to remove the experiment.

Component repository published: https://github.com/HezeAI/termux-jobs-components; immutable graphics-20261025,20assets. Public download/hash checks and real production helper preparations passed; see components-public-verification.json. Final APK45953627bytes, SHAa61d7b8db2c5a690a25572cd9d2c21422ab7a6d9d4a00973e0386d5d24c8a27b, unchanged signer.338JVM/114Python/2Node/584localization checks pass; lint0errors/210warnings. Production-source/build artifact gates pass. Prepare stable main tag/release and verify all app assets/updater after publication.
