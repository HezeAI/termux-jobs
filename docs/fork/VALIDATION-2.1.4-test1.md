# 2.1.4-test1 / 1486 — private signed release APK

Date: 2026-11-05. Checkout:
`/root/termux-fork/.verification/rollback-live-actions/source`.
Branch: `work/test-file-dates-templates`; research checkpoint: `30141820`.
Comparison baseline: last published stable v2.1.3 / 1485,
`308c88955fd3b0691ded560c78ee3f945538381e`.

## Scope and implementation

- Both built-in file manager and chat attachment picker use `WorkspaceFileBrowser`.
  Every entry now shows modification time as `dd.MM.yyyy HH:mm:ss` using the phone's
  current time zone. Metadata has secondary styling; names and sizes remain visible.
  A negative/missing timestamp shows «Дата неизвестна» / “Date unknown”; epoch zero
  remains valid. No current time is substituted.
- Refresh reads a fresh listing through the existing `ProjectFiles` transport.
  Selection remains keyed by path, with refreshed size/time for present selected
  files. Existing sort order, filters, selection actions and scroll position remain.
  Refresh is disabled while the browser is busy. The sort button keeps its position.
- Four common instruction assets are exact copies of the supplied ZIP. Codex uses
  AGENTS.md (an existing AGENTS.override.md keeps precedence); Claude uses CLAUDE.md.
  RU/EN have matching rules; the Claude variant changes the heading and file name.
  LEARNING.md and CONTINUE.md remain common. Full preview and explicit application
  remain required. Existing saved files, including empty files, are not migrated.
  Cancel writes nothing; stale revisions are refused without discarding the draft.
- The separate project instruction template is unchanged: 7,882 bytes,
  SHA-256 `3554f8178cfbc032b50bc07f3e937236a66a10dea420da84f3b1a6ed7c6a39e4`.

## Exact templates in the final APK

Supplied ZIP SHA-256:
`e013dd9829fc252ba8b997e8eb3b1e2afb412c10edb2e677bd2f248a1951aacb`.
All four ZIP entries equal their source assets and final APK entries byte for byte.

| Asset | Bytes | SHA-256 |
| --- | ---: | --- |
| [agent-instructions-ubuntu-ai.md](../../app/src/main/assets/agent-instructions-ubuntu-ai.md) | 28861 | `f0ee1dabfdd25388231f5a5b7c483259dd2fa2f6d6049b8539245128081df6d6` |
| [agent-instructions-ubuntu-ai.en.md](../../app/src/main/assets/agent-instructions-ubuntu-ai.en.md) | 15499 | `98af576b97c8b71865539ed30c8166be8e189a650cb9e5aaee4e5958430beb0b` |
| [agent-instructions-claude.md](../../app/src/main/assets/agent-instructions-claude.md) | 28873 | `56a96b8c94ab11eeb397cbb338d078aee9917810f48162dc1f598f7bd75d36ea` |
| [agent-instructions-claude.en.md](../../app/src/main/assets/agent-instructions-claude.en.md) | 15511 | `42ef6e255c619315c196a013e0208f0d29e9969b507b07eabc28a7c5fae1794b` |

## Checked anew / Проверено заново

- 150 JVM tests in 13 suites: no failures, errors or skips. These are internal
  **debug** unit/Robolectric checks, not a debug release or Android phone acceptance.
  `WorkspaceFileMetadataUiTest` separately exercises both windows: initial creation,
  overwrite, fresh listing, retained selection with updated size/time, sort retention,
  two time zones, long names, RU/EN unknown dates and valid epoch zero.
- `AgentInstructionsUiTest` includes the four-agent/language template combinations:
  exact complete preview, cancel, explicit application, correct destination and
  conflict preserving user text. Other instruction/project template/editor tests
  remain in the 150-test run.
- 57 Python storage tests on artificial homes/projects. The added four-template
  matrix proves that existing empty general files are retained, external changes
  reject stale saves, and an explicit fresh save stores the complete selected text.
  Existing tests cover atomic write errors and preservation of earlier revisions.
- Full modern release Lint gate (AGP 7.4.2 / Gradle 7.5.1 / JDK 11), all five
  modules requested: **0 errors, 235 warnings**, no infrastructure diagnostics.
  App/X11 analysis executed; unchanged terminal-emulator, terminal-view and
  termux-shared analysis was up-to-date. This is not five freshly repeated analyses.
  Source/configuration equivalence for reused inputs was checked. The older
  packaging toolchain's AndroidX/Kotlin lint-loading errors are not a Lint pass;
  the independent modern gate is the applicable analysis result.
- Final **release** APK built with AGP 4.2.2 / Gradle 7.2 / JDK 11. Signing gate and
  `verifyX11Inputs` remain enabled. ZIP integrity, package/version/SDK/ABI/debug flags,
  v2 signature and certificate, DEX runtime inventory, ARM64 ELF payloads, notices,
  asset/source equality and stale outer ZIP-entry checks passed.
- 575 packaged source/asset inputs match the final build stage. All 122 assets
  match their sources. Relative to v2.1.3, only the four common template assets
  changed; CLI adapters, pinned versions, project template and all native payloads
  are byte-identical. No CLI fork, experimental mode or automatic compaction added.
- README.md and RELEASING.md match the user's pre-task hashes. They are not staged
  in the local commits for this task. Stable update.json, old APKs, tags and
  published Components are unchanged; no network publication was performed.

## Reused / Переиспользовано and initial failures

All 95 recorded native source/config hashes match v2.1.3. Three JNI libraries are
reused from its verified APK. Optional native asset bytes also equal v2.1.3.
Generic input preparation rebuilt an unchanged Fedora helper using Clang 21.1.8
instead of the stable GCC 15.2 payload. The first unissued APK audit caught that
extra binary difference. Stable bytes were restored after verifying unchanged C
source; release packaging and all final APK checks were repeated. This does not
claim identical compiler output or fresh native/GPU execution.

The first UI run exposed a changed navigation-button order; production code was
corrected to preserve the sort/Home positions. Two new layout assertions also
incorrectly assumed that Robolectric would wrap a long name into a fourth line.
The final checks assert the complete name, unrestricted lines, no ellipsis, separate
name/size/time lines and sufficient height for the measured layout. They do not
claim phone glyph wrapping. Initial failed logs are retained separately; only the
final 150-test run above passed.

## Final artifact

- APK: `Termux-Jobs-v2.1.4-test1-arm64.apk`, 40,484,280 bytes.
- Package: `com.termux`; versionName `2.1.4-test1`; versionCode `1486`.
- Android 8.0+ (minSDK 26), targetSDK 28, compileSDK 35; ARM64 only; not debuggable.
- SHA-256: `c3aa7fd796014b61f50503a1cd634d74f8db0112c4431d09eae681f9b29dae9e`.
- Certificate SHA-256 (separate from file hash):
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Package/certificate match the last stable Termux Job’s APK and versionCode grows
  from 1485 to 1486. No data/profile/schema migration. This does not establish
  compatibility with separately signed official Termux distributions.

Local evidence: `.verification/test-2.1.4-test1/`: `build.sh`, `test.sh`, `build.log`,
`jvm-summary.json`, `junit/`, `python.log`, `lint-summary.json`, `lint-lint.xml`,
`signature.txt`, `badging.txt`, `manifest-tree.txt`, `runtime.json`, `artifact.json`,
`extra-artifact-checks.json`, `template-identity.json`, `packaged-source-inputs.json`,
`native-inputs.json`, `native-reuse.json`, `x11-payload-reuse.json`, `SHA256SUMS`.
The final full source SHA and branch are recorded after the local commits in
`source-apk-identity.json`; no test tag or remote release is created.

## Not checked / Не проверено and agreed exclusions

No APK installation, phone acceptance, working-session restart, real environment
installation or GPU test was performed. Previous developer phone acceptance is
retained under its original versions/sources in the [audit plan](SECURITY-AUDIT-STAGE2.md),
not relabelled as acceptance of this APK. Low-level Android key handoff, crash
recovery and device backup export/restore remain unverified in the agreed scope.
Real Gemini API/Vertex authorization is not closed by fake-key testing.
MCP/plugins remain excluded and unverified.

Attachment accumulation/disconnections remain **open and unfixed**. Investigation
is paused at [the recorded comparison](MEDIA-CONTEXT-INVESTIGATION.md). Working
history/originals/auth were untouched. No claim of improved speed or reduced
request size follows from the template update.

## Короткая проверка телефона

1. В отдельной тестовой папке создайте файл с длинным именем. Откройте файловый
   менеджер: имя, размер и дата с секундами должны читаться, дата — по поясу телефона.
2. Выделите файл, перезапишите его из тестового терминала и создайте ещё один.
   Нажмите обновление: новый файл появится, размер/дата изменятся; выбор и сортировка
   сохранятся. Повторите в выборе вложений, затем отмените выбор без отправки.
3. У Codex и Claude откройте общие шаблоны в RU и EN. Просмотрите начало, правила
   памяти/контекста и конец. Отмените: существующий файл не должен измениться.
   Не применяйте тестовый текст к рабочим общим инструкциям. Сохранение/конфликт
   проверены на искусственных файлах; повтор на устройстве требует отдельного
   тестового HOME, иначе остаётся непроверенным.
4. При расхождении сообщите окно, язык/агента, действие и ожидаемую/видимую дату;
   приложите скриншот без секретов. Установка и эти действия выполняются пользователем.
