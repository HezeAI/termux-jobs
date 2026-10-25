# v2.0.6 — X11 scaling, renderer setup and chat files

- **Live X11 scaling:** Apply now reaches the running XFCE settings service in Ubuntu, Debian, Alpine, Fedora, openSUSE, Void and Arch. Native Termux keeps its working path.
- **Keyboard geometry:** manually sized X11 desktops retain their full geometry when the Android keyboard opens.
- **Larger chat files:** send files up to **5 GiB** and original images up to **100 MiB**. Large exports stream; model image input remains separately bounded.
- **Reliable renderer downloads:** fix the removed Termux Vulkan Loader package that blocked Turnip+Zink and Wrapper setup. Eleven fixed graphics archives now use the separate **Termux Job’s Components** repository, with exact versions, SHA-256, original sources and licenses. Errors show concise actionable details.
- **Bionic for glibc on demand:** choose the renderer and saved/imported Turnip before launch. If missing, its unchanged approximately6.1MB runtime downloads and prepares automatically. Valid existing installations work offline; cancellation or failed verification preserves the previous runtime and driver selection. Ordinary Linux packages still use native repositories.
- **Wayland experiment discontinued:** remove the experimental KDE/Wayland UI, compositor/runtime integration and separate Arch program groups. All eight ordinary environments retain X11/XFCE and their existing supported renderers, programs, extra keys, themes and screen controls.

## Compatibility

Same **com.termux**, signing certificate, ARM64 and workspace schema8; Android8+ app / Android11+ embedded X11. VersionCode **1395** exceeds public1376 and private1394. No user-data reset, account migration or environment reinstallation is required. Existing presets, agent authentication, imported drivers and screen preferences retain their formats. Finish important work before updating.

Missing Bionic-for-glibc runtimes now require internet for first preparation. Existing valid runtimes need no redownload; changing the download URL alone does not force replacement. Previously installed experimental KDE packages are **not** automatically deleted from users’ environments. APK rollback does not roll back Linux packages.

## Checks and limits

338 targeted JVM/Robolectric tests, 114 scoped Python tests, both Node attachment suites and584 localization checks passed. Modern lint covered all five modules: **0errors,210 existing warnings**. Isolated real settings-bus fixtures verified75%/50%/100% scale across separate PRoot invocations in all seven targets. HezeAI confirmed the Arch live-scale and custom-resolution keyboard corrections on the phone.

APK archive, identity, ARM64 ABI, unchanged signer/native JNI, runtime definitions, source assets and reviewed secret-pattern checks passed. Fresh-device GUI/download acceptance, end-to-end5GiB phone transfers and the new Wrapper setup on Adreno740/750 remain specific follow-up checks. Fixing a package download does not establish compatibility with every driver ZIP or GPU.

[Validation](https://github.com/HezeAI/termux-jobs/blob/v2.0.6/docs/fork/VALIDATION-v2.0.6.md) · [Graphics distribution and upstream credits](https://github.com/HezeAI/termux-jobs/blob/v2.0.6/docs/fork/GRAPHICS-COMPONENTS.md) · [Component release](https://github.com/HezeAI/termux-jobs-components/releases/tag/graphics-20261025).

Developed by **HezeAI**.

AI development assistance: OpenAI Codex
