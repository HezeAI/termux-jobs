Historical source-only verification before packaging test33. For the delivered APK see [test33 validation](VALIDATION-test33-agent-managers.md).

# Instruction/skill manager environment and template preview — pending source update

Date:2026-11-02. No new APK was requested or produced for this follow-up. Delivered test32 remains unchanged (SHA-256 `e585d9d3aa0a3ec858c7e667ba83e29107b52b6725b2e390014b50ffc29af127`). The changes below need a subsequent versionCode when packaged, because helpers are versioned assets.

## Changes

- Both Codex and Claude Code default to Ubuntu AI. The Claude Ubuntu/Ubuntu AI chooser is removed. A legacy agent session retains its actual Ubuntu or native-Termux binding; switching agents does not silently change that environment or copy instructions.
- Instructions replace the old selector plate with **Посмотреть шаблон приложения / View app template**. The existing bounded, scrollable preview is reused. It starts in the app language and retains RU/EN inspection. The positive Apply button is absent; closing, viewing and language switching do not save or change a draft. The single explicit **Применить шаблон приложения… / Apply app template…** menu action preserves the previous preview, destination, history and conflict controls. The duplicate general-screen template action is removed.
- A noninteractive environment caption and legacy explanation remain visible. Agent selection, tabs, folder selection and diagnostics are preserved.
- Instructions/skills requests and project-file browsing use the same retained environment. The native Termux Codex route uses a native helper, not a proot login; ordinary Ubuntu uses that alias. Unsupported Claude-in-Termux instruction editing reports its limitation without redirecting to Ubuntu AI. Memory paths remain shared between agents within their actual project/environment.
- Diagnostics no longer hardcode Ubuntu AI for a request made in another environment. Existing file/history/import mechanisms are retained; no authorization file is read by the new routing logic.

## Checked anew

- **73 JVM tests**: instruction editor, skills manager, common manager layout and helper command generation. RU/EN read-only viewing for both agents preserves exact unsaved text and file identity with zero writes; Apply remains explicit. Legacy Ubuntu requests retain their environment for reads/saves, skills list/mutations keep theirs, and the environment caption is not clickable. Existing import, conflict, history, draft and layout checks passed.
- The new native Termux command uses an interactive shell like the established Ubuntu command for normal `.bashrc` environment discovery. After that change and explicit retention of ordinary-Ubuntu terminal context, all **73 JVM tests** were rerun successfully on the final routing/editor sources.
- **92 Python tests**: isolated instruction, Claude file-semantics and skills fixtures. Real fixture file operations test storage/history/atomicity; no working environment or authorization is accessed.
- Python syntax and source review: no automatic environment migration, template write or native-session restart was added. The file profile and request routing are derived from the same environment value.
- The delivered test32 APK hash is unchanged. No APK installation, publication, package preparation or environment launch was performed for this follow-up.

## Limits and evidence

Model/fixture checks do not establish physical phone UI behavior or a real native Termux helper run. Native CODEX_HOME and custom launcher behavior on a particular legacy session still require device acceptance; existing diagnostics retain that caveat. No agent/model request was run. Full Lint and APK packaging were not repeated: this scoped source update used compilation, targeted JVM tests, Python regressions and source/security review under TESTING.md. The next APK must pass its own final-artifact checks.

Evidence: `.verification/agent-manager-environment/` (source identity, successful JVM/Python logs and summary, targeted native-command rerun). Initial JVM failures are preserved in `jvm.log`: one project-suggestion regression was corrected; one menu expectation was updated for the intentionally renamed Apply action. They are not relabelled successful runs.
