# v2.0.8 — Developer Markdown & agent chat improvements

Working with AI agents through CLI directly in Termux on your phone is now even more convenient. Read formatted replies, attach project files and receive ready-to-use results in the chat.

- **Developer-style Markdown** in Codex, Claude, Gemini and OpenCode: headings, emphasis, quotes, lists, task markers, horizontally scrollable tables and code cards with syntax colours, line numbers, expand/collapse and an outlined **Copy** button.
- **Downloadable file cards** for archives, 3MF/STL, EXE/BAT and other regular files. Share project inputs in the chat and receive generated files there without manually copying phone paths. Tap a card to view supported content or save the original to Download; files are never automatically executed or unpacked.
- **Multiple attachments:** long-press a file to select several, then confirm them into the message draft. A new **From device gallery** option opens the system photo/video picker. Nothing is sent until Send is pressed.
- **Saved file sorting** beside Home in the built-in file browser and its pickers: name, size or date, ascending or descending, with folders first.
- **Stop current agent work** with the hand button between model/limits and Server + Agent. Idle is grey and crossed out. The activity line loops around the button and uses display-synchronized updates targeting 60 FPS. Completed file changes are not rolled back.
- **Keyboard dismissal after acknowledged message sends.** Failed sends retain their draft; a changed draft or another selected session is not cleared. The upper Shell field keeps its command behaviour.

Raw chat history, accounts, presets, projects and installed environments retain their existing formats. Same com.termux/signing identity, ARM64, Android 8+ app and Android 11+ X11; code 1413. **No data reset, account migration or environment reinstallation is required.** Finish important work before updating. Existing agent processes may need a normal session restart with the same chat after updating to load the new attachment helpers. This update leaves X11 renderers, driver payloads and native libraries unchanged.

Up to four attachments per message; existing limits remain 100 MiB per original image and 5 GiB for other files. Model/CLI support determines which contents can be interpreted. Gallery codecs, keyboard behaviour and actual animation frame pacing remain device dependent; 60 FPS is a target, not a measured guarantee for every phone.

Validation: 109 relevant JVM cases and eight Node suites pass; modern lint covers all five modules with zero errors and 217 reviewed warnings. The signed APK is checked for integrity, package/version, ABI, certificate, runtime definitions and bundled assets. See [validation](https://github.com/HezeAI/termux-jobs/blob/v2.0.8/docs/fork/VALIDATION-v2.0.8.md) for evidence and remaining device-check limits.

[Usage](https://github.com/HezeAI/termux-jobs/blob/v2.0.8/docs/fork/USAGE.md) · [Sources and licenses](https://github.com/HezeAI/termux-jobs/blob/v2.0.8/docs/fork/THIRD-PARTY.md) · [Graphics components](https://github.com/HezeAI/termux-jobs-components#readme)

AI development assistance: OpenAI Codex
