# v2.0.7 — Mesa 26.2.5 and on-demand X11 components

- Update Turnip + Zink to Mesa 26.2.5 in the seven Linux environments, and refresh the Bionic-for-glibc Mesa frontend.
- Download the Kali theme, Alpine VirGL client and default **a6xx-a7xx-a8xx** driver from [Termux Job’s Components](https://github.com/HezeAI/termux-jobs-components#readme) when needed.
- Add imported-driver removal with a trash button; keep the built-in default protected and preserve existing selections.
- Fix theme preparation, cleanup of used driver folders, live XFCE scale restoration and unwanted portrait panning.

Components use fixed versions with size/SHA-256 verification. Missing or changed components prepare on the next launch of the selected desktop/renderer; matching caches are reused. Restart a running desktop to load updated libraries. Ordinary Linux packages still use their native repositories.

Same package/signature, ARM64, Android 8+ app and Android 11+ X11. No data reset, account migration or environment reinstallation is required. Presets, files and imported drivers are preserved. Driver compatibility remains device specific.

Component information, sources and licenses: https://github.com/HezeAI/termux-jobs-components#readme

AI development assistance: OpenAI Codex
