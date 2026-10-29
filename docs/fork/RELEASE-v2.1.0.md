# v2.1.0 — More environments, controlled updates & better agent chats

- **Kali Linux and Manjaro:** separate terminal/X11 environments, presets, aliases, programs and desktop controls. Manjaro includes compatible Mesa 26.2.5 Turnip + Zink and Bionic-for-glibc downloads alongside CPU/VirGL options.
- **Reproducible first installation:** nine ordinary ARM64 base images and matching XFCE package sets are pinned to verified assets in the Components repository. Existing user-updated packages are not silently downgraded when a desktop starts. Ubuntu AI keeps its existing installation policy.
- **Environment updates and rollback:** check and upgrade repository packages, check Components images, restore the pinned base, and manage complete environment checkpoints. Changes require the selected environment to be stopped; unrelated sessions are not forcibly closed. Closing the progress window does not cancel an active operation.
- **New official images:** check each distribution's upstream ARM64 image and explicitly install it or replace an existing environment. Package and image checks stay in the selected environment menu. Downloads verify OCI digests, published checksums or the Arch Linux ARM signature before installation.
- **Documentation inside the app:** ArchWiki, a Forum shortcut and official environment documentation. HTTPS links can stay in the reader; long-press offers copy/browser actions. The shared Wiki page and position remain available until the app closes.
- **Clearer agent activity:** live command output in action details, image previews, clearer file and website actions, compact detail panels, and reliable links with Copy / Show / Browser actions across the shared agent chats.
- **Interface and application fixes:** a full-width session card with bordered controls inside, Kali Evince and Manjaro LibreOffice launcher fixes, Fedora DNF5 and openSUSE package-check corrections.

## Compatibility and update behavior

Android **8+** for the app, **11+** for X11; ARM64. Same `com.termux`, signing identity and workspace schema8; versionCode **1443**. **No data reset, account migration or environment reinstallation is required to update the APK.** Existing chats, presets, commands, notes, projects and environments are retained. Finish important work before updating the application.

Environment upgrades are explicit actions. **Replacing an environment with an official or base image changes its OS and application set.** The confirmation offers preservation of `/root` and `/home`; additional applications may need reinstalling. A complete previous system is retained for rollback. Checkpoints can contain credentials and consume substantial private storage. Native Termux and Ubuntu AI are excluded from the new environment replacement/update menu.

Pinned images describe the initial baseline, not a restriction on using distribution repositories. You can still install and update your own packages normally. New upstream images and user-updated packages can require additional compatibility work; a failed required-component check does not count as a successful desktop installation.

## Verification

The candidate's automated checks cover update/recovery/cancellation boundaries, image trust, selected-menu behavior, environment routing, documentation and agent chat regressions. The stable build repeats **128 JVM checks**. Supporting candidate evidence includes **117 Python checks** plus a final **41-case core rerun**, **four Node suites**, real isolated APT transactions, native OCI extraction and signature/tamper checks. Modern lint covers all five modules; its remaining warnings are documented.

Fresh-device GUI behavior and online package/renderer compatibility for every new official image are not established by fixtures. See [validation](https://github.com/HezeAI/termux-jobs/blob/v2.1.0/docs/fork/VALIDATION-v2.1.0.md) for the exact artifact, results and remaining checks.

[Environment updates and rollback](https://github.com/HezeAI/termux-jobs/blob/v2.1.0/docs/fork/ENVIRONMENT-UPDATES.md) · [Pinned environments](https://github.com/HezeAI/termux-jobs/blob/v2.1.0/docs/fork/PINNED-ENVIRONMENTS.md) · [Documentation reader](https://github.com/HezeAI/termux-jobs/blob/v2.1.0/docs/fork/DOCUMENTATION-READER.md) · [Sources and licenses](https://github.com/HezeAI/termux-jobs/blob/v2.1.0/docs/fork/THIRD-PARTY.md) · [Components](https://github.com/HezeAI/termux-jobs-components#readme)

AI development assistance: OpenAI Codex
