# Unreleased

- OpenCode: render a verified same-action user denial as neutral “Action declined”
  instead of a failed tool/session error. Keep unrelated aborts and provider errors
  visible. Store directory and action decisions separately, including directory
  Allow once followed by Read/Edit denial. Preserve both decisions on reopen.
  Already lost historical decisions are not guessed from error text.
  Persisted-history reopen and the next request are covered locally;
  phone verification of the new presentation is pending.

- Instructions: add the full supplied project template for Codex (AGENTS.md) and
  Claude Code (CLAUDE.md), with explicit project selection, editable preview,
  reviewed save/replacement and revision-conflict protection. Local only;
  phone verification pending. See [editor](docs/fork/AGENT-INSTRUCTIONS.md).


- Docs: put release highlights and Download APK first, followed by requirements
  and compatibility; fold CI history and technical evidence into details.

- Docs: distinguish internal debug CI/tests from the release build and final
  signed APK verification; no debug APK is required as a release asset.

- CI: prepare verified pinned X11 inputs on clean GitHub runners; preserve the
  mandatory hash guard and allow explicit checks of older source commits with
  separately recorded workflow identity. See [CI inputs](docs/fork/CI-X11.md).

# 2026-11-04 — 2.1.3-test7 (versionCode 1483)

- Gemini: disable native automatic installation through supported per-run system
  settings; retain version checks and update notifications. CLI remains 0.64.0.
  Update notices use a separate source tag and do not become session errors;
  real API/auth/tool errors remain visible.
- Resolve the runtime's canonical path for both child argv and the pinned loader.
  A symlink in runtime/HOME must not silently disconnect application handlers.
  Missing UI handlers explicitly refuse chat submission.
- Retain test6 backup/audit fixes and old profiles, permissions, authorization and
  installed Ubuntu AI. External system policy is never silently overwritten.
- Synthetic-key HTTP 401 and normal/symlink first/repeat launches pass locally;
  Android/Keystore internals remain unverified. Developer phone acceptance on
  test7 confirms API test-profile startup, HTTP 400 / API_KEY_INVALID, normal
  stop/relaunch and no update-error interference. Subsequent developer phone
  messages report Gemini shell/direct Write Deny and Allow once inside the
  artificial project. Outside-project actions, broader file preservation, real
  login and crash recovery remain unverified on the phone. Assistant fixture
  results are recorded separately. Attachment accumulation/disconnections
  remain **unfixed** in this APK.
- See [validation and phone checks](docs/fork/VALIDATION-2.1.3-test7.md).

# 2026-11-04 — 2.1.3-test6 (versionCode 1482)

- Backup: add a separate, default-off choice for OpenCode connection settings,
  authorization, native history, logs, internal project snapshots and receipts.
  Show RU/EN plaintext-secret and incomplete-recovery warnings before export.
- Restore selected OpenCode state to a fresh connection UUID; preserve existing
  connections. Continue reading v1/v2 archives. Connection exports use manifest
  v3 and require the updated reader. Restored receipts do not execute actions
  or carry one-time approvals in the checked native fixtures.
- Include the previous audit fixes: run-bound Gemini key handoff, confirmed new
  Codex profile policy and safe environment preparation output/download limits.
  Old profiles and installed Ubuntu AI remain unchanged.
- Gemini saved-profile recovery, cancellation, API/Vertex mode and process-owned
  key transport are checked with artificial data; Android/Keystore, real account
  access and phone recovery still require device acceptance.
- Attachment accumulation/disconnections are **not fixed in this build**.
  Images, video, documents and tool results across all four agents remain a
  separate investigation. See [validation](docs/fork/VALIDATION-2.1.3-test6.md).

# 2026-11-04 — 2.1.3-test5 (versionCode 1481)

- Codex: fix new-profile app-server startup with CLI 0.166.0. Bootstrap with a
  supported server setting; apply and verify the requested `untrusted` policy
  through the native thread API before allowing chat requests. No silent downgrade.
- Startup: report fixed safe failure reasons and exit status; drain server output
  without publishing arbitrary stderr. Unsupported access policies fail closed.
- Preserve old profiles, their permission policies, installed environments and
  authorization. Include the previous audit/backup changes from test4.
- Permission verification uses artificial profiles/files and native CLI tools;
  Android/proot startup and actual phone approval cards require phone acceptance.
- **Attachment accumulation/disconnection fix is not included.** Images, video,
  text and tool results remain a separate four-agent investigation.
- Validation: [test5 report](docs/fork/VALIDATION-2.1.3-test5.md).

# 2026-11-04 — 2.1.3-test4 (versionCode 1480)

- Backup: explicitly warn in Russian and English that full and selected backups
  are not encrypted and can contain secrets; preserve the existing consent flow.

- Security: replace the Gemini API-key plaintext handoff file with a one-shot
  local broker bound to the launch ID and verified process ancestry/start time.
  Decrypt only for the matching consumer; retain old profiles and existing files.

- Security: new Codex profiles request approval for untrusted commands using the
  native `untrusted` policy. Preserve saved profiles and their effective policy.
  This is an approval policy, not project isolation.

- Private test release; real Android/proot key transport, process recovery and
  permission cards require separate phone acceptance. Old profiles, authorization,
  environment images and component versions are not migrated.
- Image/tool-result accumulation and chat disconnects remain a separate open task.

[Validation and phone plan](docs/fork/VALIDATION-2.1.3-test4.md)

# 2026-11-03 — 2.1.3-test3 (versionCode 1479)

- Preparation: add a plan-stage count and overall bar, plus separate measured
  download progress or a busy indicator for the current stage. Exclude known-ready
  work; fill the plan only after a successful final component inventory.
- Keep a live compact preparation card in session settings, including queue,
  failure and readiness. Closing/reopening details observes the same job; retry
  remains explicit. Preserve the shared queue for all four agents and Gemini modes.
- Match the opaque horizontal star / paperclip group to the initial one-line field
  height; keep it fixed and bottom-aligned as the draft grows. Keep separate ripples.
- Add a secondary HH:mm clock only to action/command timeline-group headers, in
  the device timezone. Use the first event's known time; retain live event times
  across refresh/restart. Unknown historical times stay hidden. User messages,
  answers and attachments do not receive new clocks.
- No component version, environment, permission, signer or native-history change.
  Automatic fixtures do not establish a successful real installation or phone UI.

[Validation and device plan](docs/fork/VALIDATION-2.1.3-test3.md)

# 2026-11-03 — 2.1.3-test2 (versionCode 1478)

- Preparation: show the actual Termux-package stage before installing proot-distro,
  accept bounded prerequisite events, distinguish Ubuntu AI extraction from download,
  and announce the final inventory check. Notify stage changes immediately; retain
  the exact failed phase and the existing retry action. Use measured image bytes;
  other phases stay indeterminate. No raw stdout/stderr capture or logs return.
- Keep Gemini API and Vertex active preparations distinct while retaining the
  single shared installation queue and per-operation duplicate protection.
- Give only the horizontal star / paperclip group an opaque theme-colour fill.
  Preserve independent ripple targets and transparency of the remaining composer.
- Component versions/URLs/hashes, existing environments, permissions and histories
  remain unchanged. Same package/signer; higher versionCode for a private release APK.
- Checks use isolated artificial tools/workers, with no real package transactions
  or new phone acceptance claimed. See the detailed matrix and remaining checks.

[Validation and device plan](docs/fork/VALIDATION-2.1.3-test2.md)

# 2026-11-03 — 2.1.3-test1 (versionCode 1477)

- Agent chat: extend messages behind a transparent composer row; arrange the
  existing star / paperclip buttons horizontally with independent ripple targets.
  Keep the field background, Send on the right, dynamic bottom scroll clearance,
  draft/attachments/anchors, and restore ordinary-terminal geometry on exit.

- Release signing: require the existing keystore and pinned public certificate
  before release packaging; refuse missing/mismatched signer without generation.

- Environment setup: retain/export only bounded structured status and safe errors;
  remove full-output capture and legacy-log loading/writing/deletion. Preserve
  stream draining, measured progress, owned-child waiting, cancel and retry.

- Bound managed agent CLI/Node/Cloud archive downloads: 45-second connect and
  180-second low-speed checks, two-hour attempt ceiling, at most three attempts
  and fixed backoff. Preserve URLs/SHA checks and wait for owned-child exit before
  releasing preparation; window closing still does not cancel preparation.
  These limits do not cover the entire environment setup.

- Agent preparation: discard arbitrary stdout/stderr after bounded streaming
  parsing; retain only allowlisted stages/components, measured image progress,
  exit codes and safe RU/EN errors. Remove the expandable preparation log and
  new operation-log writes; preserve old files, chat/terminal/CLI histories.
  Inventory checks also discard raw failure reports. Explicit retry remains.

- Private release test APK, same com.termux package/public certificate and storage
  schema. Existing environments, credentials, history and agent permission settings
  are not migrated or reset. No automatic session restart or old-log cleanup.
- User-reported phone acceptance: chat layout, keyboard, buttons, scrolling and
  checking the existing Ubuntu AI work in 2.1.3-test1.
- Limits: clean installation, missing-component installation, network failures and
  production-length mobile-network timers remain unverified on the phone. Detailed
  unreported UI edge cases remain separate checks. No independent audit claimed.

[Validation and phone plan](docs/fork/VALIDATION-2.1.3-test1.md)

# 2026-11-03 — v2.1.2 (versionCode 1476)

Changes since stable v2.1.1:

- Add Claude Code instruction sources/diagnostics, shared LEARNING.md, project-file creation and session-scoped access to Instructions and Skills.
- Add general/project skills for Codex and Claude: reviewed GitHub/folder/ZIP import, in-app creation, explicit managed updates, conflict detection, rollback and removal.
- Install missing Ubuntu AI from a pinned clean GitHub Components image through a separate proot-distro5.9.0 local-archive installer. Reuse existing environments and CLI components; show preparation stages, progress and redacted logs without starting a model conversation.
- Simplify new AI session defaults while preserving legacy bindings. Improve manager scrolling, template preview/application, RU/EN memory/documentation guidance, vertical attachment/star actions and individual agent animations.
- Fix older Claude history disappearing during refresh; retain bounded paging/scroll anchors and full long-answer access. Fix local links/cards and animated HTML/GIF opening in the originating chat environment for all four agents.
- Include required third-party notices in the APK/About, preserve upstream authorship and publish a separate exact Ubuntu source supplement without changing existing Components assets. Extend release/Git/license checks.
- Clarify the independent unofficial fork, RU/EN data/security boundaries and public reporting channel. Publish the verified certificate digest separately from APK hashes; update compatibility is with matching-package/certificate Termux Job’s versions, not an unverified official-Termux installation. No independent security audit is claimed.
- Same Termux Job’s package/certificate/schema8; ARM64, app Android8+, X11 Android11+. No reset, incompatible migration or automatic environment replacement. Existing processes need an explicit idle restart to load changed adapters. Skill discovery and component readiness do not prove model/account access. The previous Docker Hub failure cause remains unknown.

[English release notes](docs/fork/RELEASE-v2.1.2.md) · [Validation](docs/fork/VALIDATION-v2.1.2.md)

# 2026-11-03 — 2.1.1-test40 (versionCode 1475)

- Remove voice input entirely: microphone, hold menu/system recognition activity, speech callbacks and speech diagnostics. Remove recording permission, microphone hardware and speech-service/activity visibility declarations; no other app recording consumer was found.
- Agent composer: stack paperclip above star in the same outline with independent48dp feedback targets. Reuse the existing field/draft and attachment/saved-command actions; expand remaining input width and keep Send at the bottom right. Ordinary terminal layout remains.
- Project instructions: distinguish Edit AGENTS.md / Edit CLAUDE.md from Create project instructions using actual base-file presence. Existing files focus the current editor/keyboard without an added status-row resize; missing files retain the creation form. Preserve drafts, selection, project binding and captured save revision. No separate diagnostic menu or artificial delay. Retain test39 history/anchor fixes.
- RU/EN labels retained. Package, signing identity and user storage formats remain; slash-message behavior is unchanged. Phone acceptance of the new composer and project action remains separate.

[Validation](docs/fork/VALIDATION-test40-composer-instructions.md)

# 2026-11-03 — 2.1.1-test39 (versionCode 1474)

- Claude history: use bounded read-only JSONL pages with continuation cursors, stable record UUIDs and partial snapshots. Earlier rows remain after polls; dense tool records continue across pages instead of dropping rows beyond the snapshot limit. Full older responses are reread from native history instead of relying on the live preview cache. Common history, scroll-position/cache and detail UI remain shared with Codex.
- Project instructions: queue a creation request tapped during file loading, coalesce repeated opens into one form and preserve its text. Existing files stay in the editor; missing files use the confirmed source/revision. Show loading/errors and retain draft/CAS safeguards for Codex/Claude from settings and session menus. No artificial opening delay.
- Retain test38 Codex microphone diagnostics and explicit Android recognition window, plus test37 Send alignment and previous features. Package/signer unchanged; instructions helper cache is instructions-1474 and saved user instructions are not replaced.
- Limits: phone acceptance remains separate for history/scrolling and window flicker/opening. Existing Claude helpers need an explicit idle restart with saved conversation to load the new adapter; no session restarts automatically. Native user history is read only. Voice provider error5 remains unexplained; stable voice publication is held pending the developer's phone check.

[Validation](docs/fork/VALIDATION-test39-history-instructions.md)

# 2026-11-03 — 2.1.1-test38 (versionCode 1473)

- Codex voice input: hold the microphone for Embedded recognition, Android recognition window or copyable diagnostics. The bounded journal shows configured/visible services, permission at startup, launch/callback/cleanup order and cancellation reasons without audio, dictation, drafts or chat identifiers.
- Add an explicit system recognition activity alternative. Results enter only the original draft, never auto-send; cancellation, unavailable handlers and chat changes preserve text. Pending recognition prevents duplicate launches. Completed embedded results/errors release without an extra cancel.
- Retain test37 Send alignment and previous composer, attachments, agent indicators, stop confirmation, instruction/skills and environment behavior. Slash work remains deferred. Package/signer retained; helper cache uses instructions-1473 without replacing saved instructions.
- Limits: the developer confirms Send alignment but still reports error5 in test37. Its cause remains unknown. Actual microphone/provider behavior and system-window success await phone acceptance; model checks do not establish them. Stable publication with voice input is held pending that check.

[Validation](docs/fork/VALIDATION-test38-voice-diagnostics.md)

# 2026-11-03 — 2.1.1-test37 (versionCode 1472)

- Keep Send at the bottom right beside the message field, independently of the floating Codex microphone. Disable chat-row baseline alignment and restore the original setting for ordinary terminals.
- Codex voice input: continue once after microphone permission grant and foreground resume, coalesce repeat taps during the permission prompt, recheck permission and original chat, and preserve drafts on cancellation/errors. Distinguish Android client error5 from local startup exceptions in RU/EN and safe diagnostics.
- Retain test36 composer, attachments, four-agent indicators, stop confirmation and previous instruction/skills features. Slash changes remain deferred. Package/signer retained; helper cache uses instructions-1472 without replacing user instructions.
- Limits: original device error5 cause remains unconfirmed; actual Ask-every-time permission/provider flow, microphone and IME appearance need phone acceptance. No automatic environment conversion, CLI update or migration is included.

[Validation](docs/fork/VALIDATION-test37-voice-permission.md)

# 2026-11-02 — 2.1.1-test36 (versionCode 1471)

- Agent composer: remove the separate Command without model field and return its height to the transcript; keep the existing attachment picker on a vector paperclip beside the star, in one outlined group with separate48dp targets and stronger press feedback.
- Codex-only voice input: a filled microphone floats above Send without a border, permanent panel or full-width row. Explicit Android microphone permission, listening/error status, final-only draft insertion, cancellation and original-chat isolation preserve text and never auto-send. Scroll clearance keeps the final message reachable above the icon.
- Disable the existing direct-interactive header button in agent chat while retaining native agent terminal access and ordinary terminal/desktop input. RU/EN updated. Slash-message changes remain deferred.
- Attachment removal stays outside the microphone target; the48dp action area and56dp end-of-scroll clearance remain fixed across chat zoom. Successful dictation clears its status row. Ordinary terminal and desktop controls retain their layout and behavior.
- Retain all test35 instruction/skill-manager features, prior agent indicators, native-terminal access and stop confirmation. No automatic instruction replacement, environment conversion, CLI update, helper experiment or robot mockup.
- Compatibility: com.termux and previous signer retained; Android microphone permission is requested only on explicit Codex voice input. Microphone hardware remains optional. The instruction helper cache uses instructions-1471 without migrating user files.
- Limits: actual speech provider, permission dialog, microphone, IME/touch and appearance require phone acceptance. The system provider may require network access; offline recognition is not promised.

[Validation](docs/fork/VALIDATION-test36-agent-composer.md)

# 2026-11-02 — 2.1.1-test35 (versionCode 1470)

- Agent resources: Codex and Claude session gears open AGENTS.md & SKILL.md / CLAUDE.md & SKILL.md immediately after Restart. A pinned session-context window opens the existing instruction editor on Project and the existing full skills manager; chat switching never retargets its source. Unsupported agents do not expose another agent's files.
- Instructions: move selected-file reload to an accessible vector Refresh button in the fixed title, retaining draft confirmation; remove Reload file and Addition from the menu. Missing project files are identified and point to Create project instructions. RU/EN updated.
- Keep all test34 project-instruction creation, explicit general-template application, authored skills and higher-contrast four-agent icons. Existing manager actions, storage formats, environment bindings and stop protections remain.
- Compatibility: package and signing certificate retained; no automatic instruction replacement, environment conversion or CLI update. The helper cache uses instructions-1470, independently of user files. No assistant experiment or robot mockup is included.
- Limitations: real-device touch, keyboard, narrow/large-font appearance, file-picker interaction and animation acceptance remain separate from model checks. The user-confirmed Claude success with the GitHub Ubuntu AI image does not establish the cause of the former Docker Hub refusal.

[Validation](docs/fork/VALIDATION-test35-agent-resources.md)

# 2026-11-02 — 2.1.1-test34 (versionCode 1469)

- Instructions: move the existing explicit Apply template action into View app template, below the scrolling text. Remove the duplicate menu entry; preserve history, revision conflicts and drafts. View/apply are available only on General; Project/Diagnostics do not redirect to a general file.
- Project instructions: optional purpose, paths, constraints, verification and documentation/memory fields build only project-specific text, with exact AGENTS.md/CLAUDE.md preview and explicit atomic/CAS save. Existing files open in the editor; opening never creates files or extra documents.
- Skills: Add → Create skill accepts a validated name, description and instructions for Codex/Claude, previews the exact selected-scope/environment path and SKILL.md, and uses staged atomic installation without silent overwrite. Errors retain form text; successful creation refreshes the list without claiming discovery by a running agent.
- Agent stop icons: brighter working highlights and deeper smooth dim phases for all four existing glyphs; unchanged cadence, states, hit area, stop protection and reduced-motion behavior. Robot mockups remain separate.
- Device evidence: the user confirmed Claude working on the other phone after switching to the GitHub-hosted Ubuntu AI image. This does not establish the cause of the prior Docker Hub refusal or cover the other agents.
- Compatibility: previous package/signer retained; no instruction replacement, environment migration or automatic CLI update. Touch, keyboard and visual smoothness remain separate phone checks.

[Validation](docs/fork/VALIDATION-test34-agent-managers.md)

# 2026-11-02 — 2.1.1-test33 (versionCode 1468)

- Agent instructions: “View app template” replaces environment selection and opens the selected agent’s RU/EN template without writing or replacing the draft. Explicit application remains separately in the menu, with history and conflict protection.
- Instructions and skills default to Ubuntu AI while retaining the actual environment of legacy sessions. Read/save requests, diagnostics and project browsing use that same displayed environment; ordinary Ubuntu instructions are not copied into Ubuntu AI. Native Termux Codex uses its native file helper; unsupported Claude-in-Termux instruction editing reports the limit.
- Compatibility: previous package and signing certificate retained; no settings/history migration or automatic replacement of user instructions. The included instruction/skill helpers use a new versioned asset folder. Existing Ubuntu AI preparation behavior from test32 is retained.
- Limitations: layout, keyboard and native legacy-environment acceptance remain phone checks. Existing remote-network and clean-install limitations are unchanged.

[Validation](docs/fork/VALIDATION-test33-agent-managers.md)

# 2026-11-02 — 2.1.1-test32 (versionCode 1467)

- Ubuntu AI: new installations download the fixed clean Ubuntu 26.04.1 LTS ARM64 OCI base from Termux Job’s Components, with SHA-256, byte progress and resumable downloads. A separate bundled proot-distro 5.9.0 installs the local archive without Docker Hub. System proot-distro is not downgraded.
- Codex, Claude Code, Gemini and OpenCode share one Ubuntu AI. Existing environments are reused; missing components use the existing versioned recipes. Incomplete environments are refused without automatic removal. Existing incompatible OpenCode binaries are preserved instead of automatically replaced.
- AI Agents forms use a fixed Ubuntu AI default for new sessions and show a plain runtime explanation instead of an environment selector. Existing ordinary-Ubuntu and direct-Termux Codex bindings survive editing and saving.
- “Launch environment check” can prepare missing components in a shared serialized operation, with inline stages, measured image progress and a redacted expandable log. Reopening joins the active operation; closing its window does not cancel preparation. This action never starts a model conversation. Vertex component preparation does not start Google sign-in.
- Preparation states replace the generic unavailable-control-channel notice while preparing an agent. CLI startup is checked before normal launch; account sign-in and model connectivity remain separate.
- Compatibility: package, signer, sessions/presets and user-data formats retained; no environment rebase or data migration. Pinning 5.9.0 is a selected distribution policy, not a proven fix for the reported remote network failure.
- Limits: native Termux clean installation and four-agent acceptance on the other phone remain unverified; real 5.9.0 local-install API extraction and model/control-flow checks are reported separately. UI and background behavior need phone acceptance.

[Validation](docs/fork/VALIDATION-test32-ubuntu-ai.md)

# 2026-11-02 — 2.1.1-test31 (versionCode 1466)

- Give Agent instructions and Agent skills the same library-style dialog: fixed title/tabs, one vertical content viewport, and a compact fixed footer. Use the available window/IME space, with wrapping action labels and no fixed screen-height percentage.
- Let the instruction editor expand into available space and grow with long text; remove the nested 14-line scroll limit. Preserve draft guards, atomic saving, imports, history, conflict handling, diagnostics and feedback visibility.
- Rename skill scopes to General / Project (Общие / Проектные). Only Project shows the project-folder selector; General displays actual shared roots from the selected agent's Ubuntu AI and no longer searches project/ancestor skill directories. Installation destinations and existing skill files stay unchanged.
- Keep data/session formats, rights, working chats, CLI versions and all test30 local-file viewing behavior. No robot mockups, helper or user-facing agent migration included.
- Limits: keyboard/IME, touch, selection and narrow/large-font acceptance on a real phone remain separate from modelled layout checks.

[Validation](docs/fork/VALIDATION-test31-agent-managers.md)

# 2026-11-02 — 2.1.1-test30 (versionCode 1465)

- Fix absolute local file links in Codex, Claude Code, Gemini and OpenCode messages. Links and cards use one viewer, bound to the originating chat's Ubuntu/environment even after switching chats or while reading finishes.
- Render self-contained HTML/HTM with JavaScript controls and animated GIF through isolated Android System WebView. No app bridge, command access, arbitrary file/content access, network, file chooser or external resource loading.
- Keep original-file saving; show missing/access, unsupported preview, size-limit and WebView errors instead of silent rejection. Preserve HTTP/HTTPS link actions and text selection.
- Compatibility: com.termux, same signer, ARM64; no session/history/settings migration or instruction replacement. No helper or user-facing agent migration restored.
- Limits: HTML previews up to 4 MiB; GIF up to 16 MiB/16 million pixels. Self-contained resources only. Real Android WebView animation, controls, zoom/taps and narrow/large-font acceptance require phone verification.

[Validation](docs/fork/VALIDATION-test30-local-files.md)

# 2026-11-02 — 2.1.1-test29 (versionCode 1464)

- Replace agents' flying point and inner bounces with app-designed individual glyph animations: rotating short Codex arc and soft >_ glow, sequential Claude rays, Gemini ray shimmer/soft scaling, and sequential OpenCode pixel illumination. Central glyphs stay upright; no layout/hit-area changes.
- Retain real request/wait/reconnect/error/idle states, amber reconnect details, calm user-wait pulse, static reduced-motion states and the existing shared frame clock. No new polling, transport changes or official-brand animation claims.
- Preserve request-scoped stop confirmation, chat execution dots, native-terminal attention and active Server + Agent transfer borders, ordinary terminal/SSH activity line, all test28 skills/instructions features and existing data formats.
- Limits: real-device smoothness, contrast and touch/TalkBack acceptance remain separate from model tests. No helper or user-facing agent migration restored.

[Validation](docs/fork/VALIDATION-test29-agent-icons.md)

# 2026-11-02 — 2.1.1-test28 (versionCode 1463)

- Add Agent skills beside Agent instructions: Codex/Claude Code, Ubuntu AI, personal/project scope, exact paths and installed-skill details.
- Add public GitHub commit-pinned, local-folder and ZIP preparation; candidate selection and explicit per-skill previews with SKILL.md, declarations, file hashes and destination. Imported scripts/dependencies never run automatically.
- App-managed packages support explicit diff/update, local-change conflicts, one outside-scan rollback, interrupted-transaction recovery and confirmed safe removal. External/bundled/plugin packages remain read-only.
- Preserve all existing user instructions, chats, presets, rights and auth. Discovery/functionality remain explicitly unconfirmed for working sessions. Claude 2.1.296 supports native /reload-skills; versions and custom launchers may differ. No helper feature or user-facing agent migration is restored.
- Limits: Ubuntu AI only; conservative YAML subset and bounded archives; no private GitHub/auth; local sources must be accessible inside the selected Ubuntu. Phone acceptance remains separate.

[Guide](docs/fork/AGENT-SKILLS.md) · [Validation](docs/fork/VALIDATION-test28-agent-skills.md)

# 2026-11-02 — 2.1.1-test27 (versionCode 1462)

- Add Codex / Claude Code selection to Agent instructions. Claude supports explicit Ubuntu AI / Ubuntu selection, actual CLAUDE_CONFIG_DIR sources, CLAUDE.md, additive CLAUDE.local.md, .claude/CLAUDE.md and shared LEARNING.md.
- Reuse atomic saves, conflict checks, import through the built-in picker, history/recovery, localized previews and guarded switching. Preserve existing Codex targets and the shared per-environment project-memory path; no user-file replacement at launch.
- Add latest-equivalent RU/EN Claude templates and agent-specific explanations/diagnostics. Mark AGENTS.md fallback/settings dependence and absence of AGENTS.override.md support. Keep native auto memory distinct from LEARNING.md.
- Native Claude 2.1.296 loopback tests confirm new instructions after save/restart-resume/new chat, retained history, no ordinary next-request reload, additive local rules and AGENTS fallback. These do not prove real model obedience or all CLI versions.
- Preserve package, signer, data formats, agent launch/permissions/connections and working sessions. Real phone keyboard/touch acceptance remains separate.

[Validation](docs/fork/VALIDATION-test27-claude-instructions.md)

---

# 2026-11-02 — 2.1.1-test26 (versionCode 1461)

- Update only the RU/EN project-memory Updating subsection: require a pre-completion lesson assessment and save confirmed lessons that prevent recurrence or materially help future project work, across code, documents, 3D, data, tools and workflows.
- Specify recording grounds, evidence/date/applicability, topic deduplication and excluded trivial/external/general observations. Preserve secrecy, accuracy, freshness, permissions and no-background-training restrictions without repeated guidance.
- Keep other template sections and saved user instructions unchanged. Preserve explicit preview/apply, history/conflict safeguards, package, signer and data formats.
- Real phone IME/touch acceptance remains separate.

[Validation](docs/fork/VALIDATION-test26-memory-update.md)

---

# 2026-11-02 — 2.1.1-test25 (versionCode 1460)

- Replace the complete RU/EN documentation-organization section with the approved text: above 20,480 bytes, reorganize toward no more than 15,360 bytes while retaining project purpose, current paths, LEARNING.md location, mandatory restrictions and required-reading navigation.
- Require topic-document reading before corresponding work, keep new details in topical docs and confirmed lessons in project memory, and avoid returning moved sections unnecessarily.
- Preserve requirements and their visibility regardless of target size; report before/after size and preservation/link checks, with explanations only for obstacles or unsafe target attainment.
- No automatic replacement of saved user instructions; keep explicit preview/apply, history, conflict safeguards, package, signer and data formats. Real phone keyboard/touch acceptance remains separate.

[Validation](docs/fork/VALIDATION-test25-template-target.md)

---

# 2026-11-02 — 2.1.1-test24 (versionCode 1459)

- Update RU/EN templates to require project AGENTS.md reorganization above 20,480 bytes, reconciling unique safety requirements and preserving mandatory entry-point constraints and applicability conditions.
- Replace optional assessment/deferral with five required steps, specific blocking conditions and before/after size/document/check reporting. Retain below-threshold discretion and all other general rules.
- Preserve explicit template preview/apply, history/conflicts, user files, package, signer and data formats. No background reorganization or automatic replacement of saved instructions is implemented.
- Actual phone keyboard/touch acceptance remains separate.

[Validation](docs/fork/VALIDATION-test24-mandatory-reorganization.md)

---

# 2026-11-02 — 2.1.1-test23 (versionCode 1458)

- Replace the RU/EN documentation-organization section with the approved revision: separate byte measurement from mandatory four-step structural assessment above 20,480 bytes.
- Require a concrete split/deferral decision, preserve explicit prohibitions and prohibit claiming assessment from measurement alone. Remove repeated guidance and keep correct Markdown list spacing.
- Preserve every other template section, user instructions, package/signature, data formats and explicit preview/apply/history/conflict safeguards. No project AGENTS.md reorganization.
- Real phone keyboard/touch acceptance remains separate from model preview tests.

[Validation](docs/fork/VALIDATION-test23-template-assessment.md)

---

# 2026-11-02 — 2.1.1-test22 (versionCode 1457)

- Clarify RU/EN general templates: measure project AGENTS.md bytes on first access per task; reassess splitting above 20,480 bytes, without repeat measurement while unchanged.
- Split only for demonstrated benefit; briefly explain deferral in the result. Preserve every other rule and explicit template-apply/history/conflict behavior.
- No automatic user-instruction replacement or project documentation reorganization. Keep package, signer and data formats.

[Validation](docs/fork/VALIDATION-test22-template-size-rule.md)

---

# 2026-11-02 — 2.1.1-test21 (versionCode 1456)

- Replace RU general template with the complete newly agreed attachment; align EN with all rules, including the 20 KiB (20,480-byte) structural review threshold and permitted documentation reorganization without changing requirement meaning.
- Preserve existing user instructions. Applying a template still requires preview and explicit confirmation, with history and conflict protection.
- Verify that RU/EN storage-addition previews exclude memory and documentation sections. Retain the LEARNING.md editor from test20; no project-instruction splitting is performed.
- Keep package/signature and data formats. Device keyboard/touch checks remain separate.

[Validation](docs/fork/VALIDATION-test21-general-template.md)

---

# 2026-11-02 — 2.1.1-test20 (versionCode 1455)

- Refresh built-in RU/EN general templates from the reviewed instructions, including project memory and project-documentation organization; preserve user text until explicit application.
- Add LEARNING.md editing, import, history and recovery, with missing-file template creation, exact paths and conflict/draft protection.
- Support literal project memory references and explicit existing-file selection without duplicating shared memory. Keep instruction priority separate from memory.
- Preserve package, signer and existing data formats. No automatic learning, cleanup, session restart or user-file replacement.
- Real phone keyboard, touch and large-font usability need device acceptance.

[Validation](docs/fork/VALIDATION-test20-instructions-learning.md)

---

# 2026-11-02 — 2.1.1 (versionCode 1454)

Stable release compared with public v2.1.0.

- Add global/project Codex instruction editing, import, revision recovery, source diagnostics and localized template previews.
- Support native ordinary-mode Codex questions and Claude follow-up queueing. Show ID-bound delivery evidence without claiming model uptake or replaying ambiguous sends.
- Add scoped Stop confirmation, agent indicators, real reconnect presentation, transient execution status and reliable final-answer timing.
- Confirm form writes before closing; retain failed drafts. Protect instruction drafts and simplify agent settings/navigation.
- Align image recognition/fallback and outline clickable web-page previews. Clarify basic-settings backup coverage.
- Retain package/signer/schema8 without a data reset. Remove the legacy user-facing Ubuntu AI migration action; retain ordinary environment and backup mechanisms.

[Release notes](docs/fork/RELEASE-v2.1.1.md) · [Validation](docs/fork/VALIDATION-v2.1.1.md)

---

# Historical development notes before v2.1.1

- Web-page previews in action details now use outlined, padded full-width link cards. Existing view/copy/open actions and available titles/URLs are retained; no extra network fetches.

- Agent delivery: compact per-message acknowledgements for Claude, Gemini and OpenCode. Keep API admission, CLI queue and current-turn evidence distinct; retain rejected/unconfirmed sends and drafts.
- Restore native-terminal attention borders independently from the removed agent chat line. Server transfer ownership/listeners remain unchanged.
- Add a transient execution footer driven by the existing frame clock: sequential dots while a request runs, waiting/reconnect text otherwise, static accessible text with disabled animations. No history entries, new CLI polls or forced scrolling.

- Pause the optional Codex helper experiment and exclude it from the next public release. Preserve its complete source snapshot in local Git branch `archive/codex-helper-test17-20261102`; remove only helper-specific UI, controller integration and adapter protocol from the active source. Keep historical private-test17 records and artifacts.
- Retain the other agreed fixes, including removal of user-facing agent rootfs migration. The later test18 design replaces the heart trajectory with a compact agent indicator. Session/preset formats and existing user data remain unchanged.
- No APK build, installation or publication accompanies this source separation. Existing version metadata is unchanged; the next APK must exceed reserve1451. [Separation and verification](docs/fork/HELPER-EXPERIMENT-PAUSED.md).

- Development policy now requires dated, versioned CHANGELOG entries and a linked validation report for every delivered APK, with separate checked/reused/unverified evidence. Documentation-only edits do not trigger APK rebuilds.

# 2026-11-02 — 2.1.1-test19 (versionCode 1453)

Private test build. Compact per-message delivery captions for Claude, Gemini and
OpenCode preserve distinct API/queue/current-request evidence and ID correlation.
Restore native-terminal attention borders. Add transient execution dots using the
existing frame clock and request states. Outline clickable web-page previews.
Preserve Codex delivery, ordinary terminal/SSH lines, server transfer ownership,
agent stop scope, package/signer/schema8. Helper remains excluded. Receipt metadata
from a running adapter can be lost after that adapter restarts. Real phone animation,
keyboard and accessibility acceptance remain separate.

APK SHA-256: `e3dd6057109a3787531b85213179dbc28d301480b024634ff3dfb398b025d50e`.

[Validation](docs/fork/VALIDATION-test19-chat-status.md)

---

# 2026-11-02 — 2.1.1-test18 (versionCode 1452)

Private test APK; not a public release.

- All four agent chats: replace the heart with a monochrome Codex `>_`, Claude ray star, Gemini sparkle or pixel OC. Keep the same circular button/hit area and the request-scoped Yes/No stop confirmation.
- Keep agent motion inside the indicator: orbit with short tail, six asymmetric inner contacts and contact flashes; calm waiting pulse, amber dashed reconnect, muted idle and red error outline. Drive it with the existing display frame clock, preserve static states when system animations are disabled and retain the ordinary terminal/SSH activity line. Native retry details/counters remain unchanged; CLI readiness never proves model connectivity.
- One stable timing summary below each final request answer: “Работал 16 с · 05:03” / “Worked for 16s · 5:03 AM”, following app language and device clock/timezone. Use confirmed Codex native turn start/end/duration; otherwise observe matching native request lifecycle events. Unknown start omits duration; old history without reliable completion data has no fabricated time. Tap for full date/time and explanation. Keep a small append-only per-conversation summary journal, not a rewritten copy of history.
- Exclude the archived helper experiment. Retain removal of the user-facing agent migration feature and the other agreed fixes. Preserve com.termux, signer, ARM64 and workspace/schema8; no data reset or incompatible settings migration. New summaries are optional presentation data ignored by earlier APKs and are not added to existing backup/export formats.
- Limits: actual phone glyph/trajectory/flash readability, frame smoothness, IME, large-font and touch acceptance remain separate from isolated models. Existing running CLI adapters load this code only after an explicit normal agent restart with history continuation. Observed durations include tool/network/user waits and are not CPU measurements. No old-history timing is inferred from creation timestamps.

- Final APK SHA-256: `ecde761d2b94b09522684d3093769c4dc9c405d0fdadcebc81c81b6a796a708a`. Final ZIP/package/version/ARM64/debug/signature/runtime checks passed; modern Lint has0 errors/234 warnings. Device visual acceptance remains separate.

[Validation](docs/fork/VALIDATION-test18-agent-indicator.md)

---

# 2026-11-02 — 2.1.1-test17 (versionCode 1450)

Private test APK; not a public release.

- Codex: optional helper selected explicitly in session/preset settings. A new helper derives a separate saved conversation from a Codex preset; an existing helper retains its native ID/history and uses its active channel, or the normal verified resume path. Prevent self-linking and concurrent helper ownership; keep privileges and connections separate.
- Compact helper history/composer, minimize/reopen, earlier-message loading, independent scoped Stop and explicit personal-answer forwarding. Opening/minimizing does not send model requests. Show questions/approvals as requiring the ordinary helper session.
- Review button on working Codex messages: freeze source/context, persist review/admission/turn IDs, queue busy helpers without steer, and automatically deliver only the exact completed successful final answer with Helper authorship/source. Distinguish app queue, CLI admission, rejection and unknown state; admission does not prove model uptake. Protect duplicate clicks, changed chats, cancellation, disable/re-enable and process recovery from automatic replay.
- Old helper channels give a capability reason and an explicit helper-only restart/resume action. Preserve its ID/history, do not restart the worker, and require a fresh explicit review after upgrade.
- Remove the user-facing agent rootfs migration button/clone path and its active instructions. Retain ordinary environment selection, preparation, installation, safe deletion, imports, backups and internal update-data compatibility. Existing sessions, histories and authorization are not changed by this removal.
- Heart activity line: enter from its incoming side, six asymmetric inner-circle contacts with the final contact leading into the existing outer loop; contact-aligned stronger pulses. Retain the existing frame clock, disabled-animation static state and scoped stop/confirmation.
- Compatibility: com.termux, signing identity, ARM64 and workspace/schema8 retained. Helper links/delivery states use a separate atomic sidecar; ordinary helper sessions/history remain readable by reserve test16/1451. No incompatible data migration or reset. Existing backup/export formats do not yet include the helper-link sidecar. Target1450 is below reserve1451; every later build must account for both and use1452 or higher.
- Limits: exact final-result protocol exercised with actual Codex0.166.0 and a synthetic local provider. Pending reviews are cancelled/unknown after an application-process restart rather than silently replayed; already-submitted deliveries cannot be undone. At most256 review records are admitted locally. Real phone overlay/IME/accessibility, live providers and visible trajectory/pulse quality require device acceptance.

- Final APK SHA-256: `6e3a26ae50f69e774a2e9e7abba6d0041a0cbe2e988e6e273803b0426f0496fd`. Checked final72 JVM cases,40 Python cases, actual CLI0.166 local-provider exchange/cancellation and full modern Lint0 errors/233 warnings; phone acceptance remains separate.

[Validation](docs/fork/VALIDATION-test17-codex-helper.md) · [Helper guide](docs/fork/CODEX-HELPER.md)

---

# 2026-11-01 — 2.1.1-test16 reserve (versionCode 1451)

Private reserve based on the installed test16/1449, prepared before helper and
migration-removal changes. Package com.termux and signer are unchanged. Only
manifest versionCode and APK signing/container metadata change; all 1103 other
ZIP entries are byte-identical, including DEX, native libraries, resources and assets.
Internal BuildConfig remains1449. Source SHA-256:
`67fce840fde3d1c5b3f758ce5d5216f5b08cd1ca82d4c986c06fd0069d89f3bb`.
Reserve SHA-256:
`7456bc269d180acf4857fdba43a2326842ec45c19e852654cd74648011f67bdd`.
Target test17 uses1450; every later build must account for reserve1451 and use1452
or greater. Installing this APK does not restore old data.

[Validation](docs/fork/VALIDATION-test17-codex-helper.md)

---

# 2026-11-01 — 2.1.1-test16 (versionCode 1449)

Private test APK; not a public release.

- Codex: show native reconnect progress above the transcript with the reported attempt/limit and tappable reason. Separate local CLI readiness from model response connectivity; steering acknowledgement does not hide an outage. Clear the banner on resumed output, successful completion or confirmed interruption, retain actual final failure/unknown local connection state, and isolate it by chat/request. Keep native retry timing and limits.
- Codex: attach successful message admission to its corresponding user message or attachment as a compact caption; tap for the admission/uptake distinction. Remove the successful message's bottom delivery notice. Keep failed/unconfirmed delivery noticeable and preserve text/attachments. Correlate by native clientId, including duplicate text; do not change Shell deduplication or other agents' send protocols. Avoid duplicate terminal-error history entries from native error/completed notifications.
- Instruction editor: horizontal scrolling single-line scope/file tabs, persistent selected state and actual-source caption. Scroll long project/editor content while retaining save/menu/close controls. Bound long status messages; keep preview RU/EN controls and apply/cancel available, with long paths/warnings inside the preview scroll area. Preserve system font sizing and user instruction text.
- Compatibility: same com.termux, certificate, ARM64 and settings/schema8; no migration/reset, auth/config edit, process restart or network policy change. Code1449 exceeds corrected1447 and reserve1448; subsequent builds must exceed1449. The old reserve1448 cannot be installed directly over1449 as an ordinary update.
- Limits: the isolated actual-CLI fault reproduces hidden retry state, not an established cause on the other phone. Codex0.164.0 is tested with a synthetic loopback provider; live caption metadata can be lost on adapter restart. Existing running adapters require an explicit restart with conversation resume to load changed helper code. Narrow/large-font/IME layouts are modeled; real phone keyboard, touch, font rendering and cloud transport fallback remain for acceptance.

[Validation](docs/fork/VALIDATION-test16-chat-recovery-layout.md)

---

# 2026-11-01 — 2.1.1-test15 (versionCode 1447)

Private test APK based on test14, for developer phone acceptance.

- F01: close command/note/alias/session/preset forms and show success only after the asynchronous library write is acknowledged. Keep an accessible draft and storage error on failure; do not move file writes to the UI thread.
- F02: distinguish confirmed active Codex process directory/access/approvals from edited settings for its next launch. Missing legacy process metadata is shown as unknown. No automatic restart or privilege expansion.
- F03/F04: distinguish current-turn admission, queue admission, new-turn admission, rejection and unconfirmed delivery for all four agents. Clear the composer and attachments only after admission; preserve them on failure. A cached ambiguous Codex retry explicitly says it did not resend; keep Shell deduplication.
- Claude 2.1.296: admit follow-up messages during responses and pending tools through its native stream and command lifecycle acknowledgement. Label the native queue accurately; preserve ordering, cancellation and same-ID reconnect deduplication. Older launches lacking the required capabilities fail closed without restarting.
- Codex 0.164.0: enable the supported `default_mode_request_user_input` launch feature when present, without writing this feature into user config or switching to Plan. Native question cards support unselected options, free text and several questions. Reply through the original request ID/nonce/session channel, independently of a pending Send; show CLI resolution and an answer receipt, protect late/duplicate replies and retain failed drafts. Ordinary lists remain text.
- Replace the stop hand with a pulsing heart driven by the existing display frame clock, including a five-bend interior line segment and stronger beats on traversal. Keep the external border route, layout and hit area; respect disabled system animations. Show a single RU/EN Stop [agent]? confirmation with Yes on the left and No on the right; bind it to the selected run/request. Back, outside tap and No do not stop anything.
- U01: protect unsaved instruction text from Back/outside dismissal with an explicit discard choice; saving blocks dismissal and the first Back closes a visible keyboard. Explicit Cancel remains a discard action.
- Persistently highlight the selected General/Project/Diagnostics tab and edited AGENTS file with accent outline, fill and text, including accessibility selection. Keep the separate statement of which file Codex actually reads.
- F05: clarify the narrow basic-settings backup scope and exclusions; do not expand backup content or change its format.
- F06: share image extension/MIME recognition across file management and attachments. If the platform cannot decode a recognized image, retain and offer the original as a file instead of reporting it missing; decoder support remains device-dependent.
- U02 is intentional: agent messages still enter Recent commands. O03 is deferred: preserve documentation pages/history/scroll, background transfers and retained windows until operation quiescence is established.
- Compatibility: same com.termux, certificate, ARM64 and schema8; no data migration/reset. The existing bounded receipt store gains an optional answered-question receipt; test14 ignores this unfamiliar receipt outcome while native chat history remains unchanged. Existing processes retain their old adapters until an explicit restart with conversation resume.
- Version order: corrected1447, test14 reserve1448; every later build must exceed1448. Installing the reserve does not restore old user data.
- Limits: visual heart/line smoothness, actual phone keyboard/accessibility, decoder availability, live authenticated provider/network failures and update acceptance require phone testing. Native protocol tests use actual pinned CLIs with synthetic loopback providers, not cloud-model inference. CLI changes can change feature support and semantics.

[Validation](docs/fork/VALIDATION-test15-audit-fixes.md)

---

# 2026-11-01 — 2.1.1-test14 reserve (versionCode 1448)

Private reserve APK based on the already delivered test14; not a new functional build.

- Raise only AndroidManifest versionCode1446→1448 and re-sign with the same certificate. Keep package com.termux, versionName2.1.1-test14 and every non-manifest executable/resource/asset/native entry byte-identical.
- Reserve1447 for corrected test15; future builds must use1449 or higher. No settings/schema8 migration, data rollback mechanism or cleanup. Existing user data is not snapshotted by this APK.
- Limitation: embedded BuildConfig stays test14/1446 because code is unchanged; installed Android metadata is1448. On-device update/rollback acceptance remains to be tested by the developer. No fixes from test15 are included.

[Validation](docs/fork/VALIDATION-test15-audit-fixes.md)

---

# 2026-11-01 — 2.1.1-test14 (versionCode 1446)

Private APK for developer phone testing; not published on GitHub.

- Add **Advanced / Дополнительно** to the main application menu. Move Backup and restore, Check environment updates, Clean up storage, and Errors and solutions there in that order; remove the original duplicate rows.
- Rename the existing App settings action to **About / О приложении** and place it last, retaining its existing activity destination. Preserve the original workspace menu title, styling, update/language/instruction/history entries.
- Keep the main menu beneath Advanced and Advanced beneath its child windows, so Close/Back returns without recreating menus or rerunning actions. Close the navigation layers before an explicitly chosen terminal insertion or backup package launch. Keep maintenance engines, confirmations and storage/cleanup guards intact.
- Hide the extended-debug switch and its explanation for Codex, Claude, Gemini and OpenCode in new/existing session and preset forms. Ordinary terminal profiles retain their switch and stored setting.
- Previously saved agent extended-debug=true values are normalized to false on read, copy and serialization. Launch guards also ignore stale in-memory flags: the baseline collector stays active, extended startup events are off, and Gemini receives JOBS_GEMINI_DEBUG=0. Normal errors and explicit temporary diagnostic detail controls remain. Existing log files are retained; already running agent processes are not restarted or reconfigured.
- Simplify RU/EN agent help: show the selected environment and actual editable working folder, applicable access guidance and separate write/confirmation settings. The working folder does not imply an access boundary without enforcement. Retain Android-root and sandbox-failure warnings; move native Codex parameters and component details into collapsible Details.
- Hide generic terminal/SSH startup guidance in agent forms; shorten the connection launcher explanation. Permission, approval, connection and startup behavior remain unchanged apart from the explicitly disabled agent debug setting.
- Compatibility: same com.termux/certificate/ARM64/schema8 and Android requirements, versionCode1445→1446. No data reset or environment/account migration. The intentional compatibility change is disabling the old agent verbose flag; ordinary terminal flags remain valid.
- Limits: actual phone navigation, keyboard/scrolling and disclosure layout still need developer acceptance. Tests open maintenance windows with read-only worker requests intercepted; they do not perform cleanup, restoration or environment upgrades.

[Validation](docs/fork/VALIDATION-test14-settings-menu.md)

---

# 2026-11-01 — 2.1.1-test13 (versionCode 1445)

Private APK for developer phone testing; not published on GitHub.

- Bound built-in template, file-storage addition and revision-history previews to the available window height. Long text and its notice scroll together; Apply/Cancel stay in the bottom button panel.
- Add RU/EN selectors to template and file-storage previews, defaulting to the UI language. Use the existing asset texts; show the chosen language and exact target file. Switching changes only the preview; replacement/appending requires the explicit apply action.
- Prevent another standard storage section in either language when its RU or EN heading is already present, including after editing a draft or changing preview language. No automatic translation or existing-text cleanup. Keep the storage-addition action in General's menu; remove its duplicate main-screen button.
- Add General → Import instructions and Project → Import AGENTS.md using the built-in environment/phone file dialog. Validate bounded UTF-8 text, preview the exact destination, require explicit confirmation and retain previous revisions through the existing atomic/conflict-checked save path. Unsaved drafts require Save, Discard or Cancel first.
- Shared import targets the actual active CODEX_HOME file, including its override. Project import targets only the selected project's AGENTS.md and warns when its override remains active. Do not translate file contents, change other instruction/config/auth files or restart chats. Refresh the editor/source state and regular save notice after import.
- Verify RU/EN menus, history, diagnostics, confirmations, errors, both language selectors and preservation of exact user/history text when the UI language changes.
- Keep explicit apply/save feedback, atomic storage, previous revisions and conflict handling. Cancelling a preview leaves the file and editor draft unchanged. Selectors and Apply/Cancel remain outside the scrolling text.
- Translate project AGENTS.md into Russian and consolidate all active root/checkout requirements, including X11 and clean-install checks, in the active checkout. The outer AGENTS.md now points to this single full source; update workspace documentation links. Exact technical literals and English public release-note policy remain. The APK is rebuilt for UI/helper changes, not for documentation edits.
- Add a compact RU/EN Project explanation of AGENTS.md versus AGENTS.override.md, including continued shared instructions. A separate state line reports which non-empty file is selected by Codex's standard precedence in this folder, or that neither contains instructions.
- Label the editable path separately; choosing a file to edit does not enable it or change the current rules. Refresh the reported state on read/save.
- Hide Install packages and Session diagnostics in the session gear menu for Codex, Claude, Gemini and OpenCode. Ordinary/base terminals retain diagnostic access and have direct package access in that menu. Keep environment preparation, internal diagnostics and Agent instructions → Diagnostics intact.
- Compatibility: same com.termux, certificate, ARM64, schema8 and Android requirements; versionCode increases from1444 to1445. No user-data reset, rootfs migration, automatic instruction replacement or working-agent restart is introduced.
- Limits: phone keyboard/scrolling/rotation and real small-screen visibility need developer acceptance. The source state describes standard file precedence at the latest read/save, not instructions already captured by a running request; custom launch/configuration caveats remain.

[Editor behavior](docs/fork/AGENT-INSTRUCTIONS.md) · [Validation](docs/fork/VALIDATION-test13-agent-instructions-ui.md)

---

# 2026-11-01 — 2.1.1-test12 (versionCode 1444)

Private APK for developer phone testing; not published on GitHub.

- Add **global settings → Agent instructions → Codex → Ubuntu AI**, with General, Project and Diagnostics tabs. The current chat only suggests a project directory.
- Edit the actual resolved CODEX_HOME AGENTS source, including its active override, and the chosen project's AGENTS.md/AGENTS.override.md. Show masked project files, source diagnostics and actual CLI version.
- Save exact UTF-8 text atomically, retain previous revisions, preview/restore history and refuse conflicting edits. Closing/cancelling does not save a draft.
- Add universal RU/EN guidance for programs, websites, APKs, 3D models, documents, file processing and diagnostics. Initialize only absent global files; preserve existing and empty user files on upgrade.
- Preview a built-in template before explicit replacement, or append the **File and temporary data storage** section while retaining existing text. No automatic cleanup, disk quota or retention enforcement is introduced.
- Saving works independently of CLI version. Diagnostics distinguishes tested Codex0.163/0.164 refresh behavior from unverified versions. A running request is unchanged; restarting with resumed history rereads shared/project rules in the tested versions. Existing installed Codex is retained by missing-only preparation; Codex Ubuntu AI now declares its Python dependency.
- Compatibility: same com.termux, signer, ARM64, Android8+ app / Android11+ X11 and workspace schema8. No data reset or account/rootfs migration is required. Existing chats and connections are not restarted by the editor. Other agents/ordinary Ubuntu/Termux are outside this initial instructions integration.
- Limits: phone editor/keyboard/scrolling/rotation and post-restart persistence still need device testing. Custom connection executables can alter CODEX_HOME beyond the standard shell inspected by diagnostics. Codex instruction size limits and version-dependent behavior remain applicable.

[Feature/source behavior](docs/fork/AGENT-INSTRUCTIONS.md) · [Validation](docs/fork/VALIDATION-test12-agent-instructions.md)

---

# v2.1.0 — More environments, controlled updates & better agent chats

- Add Kali Linux and Manjaro environments, X11 integration, programs, aliases and target-specific graphics components.
- Pin nine ordinary base images and matching XFCE package sets through verified Components assets; preserve Ubuntu AI policy and user-controlled package updates.
- Add scoped environment package checks/upgrades, base restoration, complete checkpoints, rollback and cancellation/recovery.
- Add official ARM64 image discovery and confirmed initial installation/replacement, with optional homes and the previous system retained for rollback; keep checks in the selected menu.
- Add in-app ArchWiki, Forum and environment documentation with process-lifetime reader state and explicit link actions.
- Improve live agent command/image/file/web action details, compact dialogs and shared link handling.
- Move session controls into a full-width bordered card; correct Kali Evince, Manjaro LibreOffice and Fedora/openSUSE update queries.
- Same package/signer/schema8/ARM64 and Android requirements; code1443. Updating the APK requires no reset or rootfs migration. Explicit image replacement separately changes the selected OS/program set.
- [Release](docs/fork/RELEASE-v2.1.0.md) · [Validation](docs/fork/VALIDATION-v2.1.0.md).

---

# v2.0.9 — Environment aliases & agent chat scale

- Add persistent aliases to eight installed terminal environments, excluding Ubuntu AI.
- Add creation, environment/category selection, managed-name replacement confirmation, editing and deletion.
- Separate All commands / All aliases and their category/favourite filters; preserve previous library data.
- Add Commands / Alias / Notes tabs, independent searches and alias previews in the star picker.
- Add separate alias import/export with selection, full previews, preserved environment/category bindings and confirmed replacements.
- Show tab-restart guidance only for Termux alias creation and deletion; keep other environments' session guidance.
- Combine alias transfer into four shell-file/JSON import and export actions; parse literal definitions without executing startup files and export one environment per shell file.
- Add per-session 50–150% agent-chat scale, default100%, with live layout scaling, 100% reset and restoration of shared input geometry outside agent chats.
- Keep running shells, agent sessions, X11 and graphics components unchanged.
- Same package/signer/schema8/ARM64 and Android requirements; code1417. No data reset or environment/account migration. [Release](docs/fork/RELEASE-v2.0.9.md) · [Validation](docs/fork/VALIDATION-v2.0.9.md).

---

# v2.0.8 — Developer Markdown & agent chat improvements

- Native Developer-style Markdown for Codex, Claude, Gemini and OpenCode, with an outlined Copy action and pressed feedback.
- Downloadable generic file cards, including archives, 3D-print formats and executables; explicit user actions only.
- Saved name/size/date sorting in the shared file browser and pickers.
- Long-press attachment multi-selection and native gallery photo/video selection into the draft.
- Current-agent Stop control with a reversed circular activity route and VSync-driven60FPS target, plus keyboard dismissal after acknowledged sends.
- No account, rootfs or raw-history migration; unchanged X11/native payloads. Code1413. See [release](docs/fork/RELEASE-v2.0.8.md) and [validation](docs/fork/VALIDATION-v2.0.8.md) for checks and device limits.

---

# Historical v2.0.7 — Mesa 26.2.5 and on-demand X11 components

Use Mesa 26.2.5 for seven PRoot Turnip/Zink runtimes and the private bridge frontend. Add pinned optional Kali/Alpine VirGL downloads, a deferred protected a6xx-a7xx-a8xx default, and confirmed imported-driver removal. Correct Android private-directory aliases, used-driver cache cleanup, unset XFCE scale restoration and portrait viewport geometry. Preserve package/signer/schema8/ARM64/min26/target28/compile35; code1410. No data reset, account migration or rootfs reinstall. [Release](docs/fork/RELEASE-v2.0.7.md) · [Validation](docs/fork/VALIDATION-v2.0.7.md).

## v2.0.6 — X11 scaling, renderer setup and chat files

Fix live interface-scale Apply across all seven PRoot XFCE session buses while preserving native Termux’s working path. Keep custom X11 geometry stable with the keyboard open. Raise streamed chat-file transfers to5GiB and original images to100MiB, retaining bounded model-ready images.

Fix the removed Termux Vulkan Loader download affecting Turnip+Zink and Wrapper. Mirror eleven fixed graphics archives with unchanged payloads (except the loader update1.4.365→1.4.366) to the separate components repository; pin hashes/sizes and retain source/licenses. Download the unchanged Bionic-for-glibc runtime on first use instead of bundling it in the APK; reuse valid existing runtimes offline. Present concise sanitized setup errors.

Discontinue/remove the Wayland/KDE experiment, native compositor and separate Arch program groups. Preserve X11 renderers, imports, themes, extra keys and screen settings. Same com.termux/signer/schema8/ARM64/min26/target28/compile35, code1395; no reset or rootfs migration. Previously installed experimental packages are not automatically removed.
[Release](docs/fork/RELEASE-v2.0.6.md) · [Validation](docs/fork/VALIDATION-v2.0.6.md).

---

## v2.0.5 — X11 screen controls and input fixes

Add per-environment logical resolution, safe margins, automatic reset and
reversible 50–150% XFCE/Thunar scaling with bounded live Apply. Preserve orientation
and fill embedded/full-screen views. Unchecking Auto setup prefills
1600×720/75%/5dp while reopening saved manual values preserves them. Gate automatic
keyboard requests on active normal window/Fcitx metadata; manual | remains.
Compact the floating extra-key panel to 400×104dp with reflow and held-key cleanup.
Keep experimental Arch Wayland/KDE and all its driver controls visibly gray and
unavailable in public builds; guard backend launch/install/runtime preparation,
ignore retained preview selection without deleting it. Private preview builds can
opt in explicitly. Native X11 core and existing driver payloads unchanged.
Same com.termux/signer/ARM64/schema8/min26/target28/compile35; code1376, intentionally
matching retained test.10 while exceeding public1366. No reset/migration.
[Release](docs/fork/RELEASE-v2.0.5.md) · [Validation](docs/fork/VALIDATION-v2.0.5.md).

---

## v2.0.4 — Bionic for glibc bridge and X11 tools

Add optional private Bionic Vulkan HAL loading for Ubuntu, Debian, Fedora, Arch,
openSUSE and Void using pinned libhybris/Arm-xMeM WSI, a private Mesa26.1.6 frontend
and our ICD/cache/environment integration. Separate target caches, locks and
service displays; imported Turnip and compatible QCOM drivers never replace
installed libraries. Correct Present reader initialization, GPU wait and geometry
lifetime; preserve FIFO pacing and explicit uncapped requests. QCOM native BGRA
import supports the tested891.11 path; its concurrent interval-transition timeout
remains an experimental-driver limitation. Export source/runtime/patches/licenses
and document upstream authors plus driver sources StevenMXZ and The412Banner.

Add X11 extra keys with movable/resizable panels in embedded/fullscreen views.
Add optional Jobs GPU Test with scenes, renderer/VSync comparison, reports and
per-target dependencies/desktop shortcuts in eight systems. Reduce optional Kali
style metrics about8%, preserving restoration and distribution menu plugins;
Whisker bottom clearance is pending a visual acceptance check. Improve validated
Bionic ZIP library-name handling for vendor drivers. Existing other renderer,
Termux Wrapper, agent/account/rootfs paths remain. Same com.termux/signer/schema8,
min26/target28/compile35/ARM64, X11 min30; code1366. No reset/migration.
[Release](docs/fork/RELEASE-v2.0.4.md) · [Validation](docs/fork/VALIDATION-v2.0.4.md).

## v2.0.3 — X11 landscape and cumulative stability fixes

Add full-screen landscape presentation for all eight X11 environments. Reparent
the existing display instead of restarting the desktop. Restore its embedded
window on portrait rotation or Android Back; retain the Interactive input | button
in a nonfocusable sibling window above the native surface. Restore the application's orientation policy when
leaving X11. Use physical landscape resolution up to2880px wide, preserving aspect ratio
and keyboard crop stability. Native cores/drivers unchanged. Lower the input button slightly and show its
enabled fill without recreating windows. Extend the optional bundled Kali-inspired
style to all eight supported desktops with independent preferences and HOME-scoped
restoration. Respect system XFConf panel defaults and inherited empty properties
(including openSUSE), preserving distribution menu plugins and user overrides.
Make the input control translucent and lower it for easier access. Fedora media setup
adds explicit GStreamer/C++ runtimes plus the pinned private OpenH264 decoder
and GStreamer plugin, scoped to Parole with a dedicated registry. Probe decoder
creation and H.264/AAC/MP4 factories before marking ready; failed/interrupted
setup stays missing and can be retried. Filter Cisco only for these DNF commands
without changing repositories or replacing RPM libraries.

Include the agent, X11/installer, storage/backup, persistence, transfer, updater,
settings/provider and lint fixes from the private audit stages below. Same
com.termux/signer/schema8, min26/target28/ARM64 and X11 min30; code1352.
No valid-data reset or environment migration. Intentional HOME-only SAF access,
exclusive output naming and malformed-library read-only handling are disclosed
in RU/EN update metadata and the [release notes](docs/fork/RELEASE-v2.0.3.md).
HezeAI confirmed the latest candidate works, including Fedora MP4 playback.
Automated source/build checks and device results are recorded separately.

## Historical private audit — lint alignment and source-audit completion (2.0.16-test)

Add isolated five-module AGP7.4.2/Gradle7.5.1 lint, failure/stale-result guards and
warning triage. Fix 11 newly visible errors, shared-locale conversions and an
API26 constant reference. Make existing notification actions immutable and declare
three existing component exports explicitly. Use AndroidX EXIF1.4.2 for thumbnails,
exclusive creation for attachment Download, and proper image-preview click events.
Preserve native launch/stop ownership, assets, window geometry and permissions.
Private code1345, same package/signer/schema8/SDK/ABI; no valid-data reset/migration.
Full 1,209-JVM confirmation and eight lint-checker tests pass. Modern lint: 0 errors/
209 retained warnings; tool-version, SDK and device limits remain explicit.
Public stable stays v2.0.2. [Evidence](docs/fork/AUDIT-LINT-2026-10-24.md).

## Earlier private audit — locale and provider boundaries (2.0.15-test)

Validate canonical HOME/Termux-files/shared-storage boundaries and opened file
handles before truncation. Pin parent handles for SAF creation/deletion; reject
unsafe names, external links, root deletion and special/missing share files.
Bound search cycles, preserve existing IDs/internal alias names, handle Android
mount aliases by inode identity and keep property-file aliases read-only.
Use locale-independent MIME/search/tag normalization, encode share URI paths,
return long sizes and apply the advertised invalid-action VIEW fallback.
Private code1344, same package/signer/schema8; no valid-data reset or migration.
Existing manifest permissions and allow-external-apps policy remain. Outside-HOME
SAF grants are intentionally no longer usable; Android storage sources remain an
alternative. Workspace browsing and agent/process/runtime paths are unchanged.
42 scoped JVM cases pass; built-in lint: 0 errors/118 warnings, with known tooling
limitations. Stable remains v2.0.2. [Evidence](docs/fork/AUDIT-PROVIDERS-2026-10-24.md).

## Earlier private audit — settings lifecycle audit (2.0.14-test)

Create the12 preference adapters per screen instead of caching the first Activity
in static singletons. Remove seven unused duplicate Context fields. Preserve the
existing SharedPreferences namespace, values, package lookup and live-screen
error/localization paths; reopening can initialize a new adapter after a failed
lookup. No application-context substitution or weak global cache is introduced.
Private code1343, same package/signer/schema8; no valid-data reset or migration.
All earlier audit fixes retained; stable remains v2.0.2. Targeted lifecycle/value/
language/failure fixtures pass, with the existing lint-toolchain limits recorded.
[Evidence and remaining checks](docs/fork/AUDIT-SETTINGS-2026-10-24.md).

## Earlier private audit — cumulative regression audit (2.0.13-test)

Run cumulative JVM/module, Python and Node fixture checks. Make Android30 guards
explicit around X11 diagnostic/keyboard calls, replace numeric orientation values
with framework constants and reuse the icon-badge Paint. Document the deliberate
platform icon widget with one scoped lint exception; do not switch its superclass
or change dimensions/styles. Clarify Android8+ app versus Android11+ embedded X11
requirements and correct the stable APK filename in installation documentation.
Private code1342, same package/signer/schema8; all earlier private audit fixes
retained. No data reset or migration. Public stable remains v2.0.2. Executed lint
checks have no errors; remaining warnings and incompatible AndroidX custom-check
loading prevent a complete/clean-lint claim.
[Evidence, remaining work and phone checks](docs/fork/AUDIT-REGRESSION-2026-10-24.md).

## Earlier private audit — file transfer recovery audit (2.0.12-test)

Require ordered per-invocation Copy/Move progress, a final completion receipt and
a zero transport exit before reporting success. Bound and validate the response;
ignore old/foreign progress and reject truncated, repeated or reordered own
records. Explain uncertain outcomes with an acknowledged lower bound and no
automatic replay. Add protocol SSH keepalives only to ProjectFiles connections.
Private code1341, same package/signer/schema8, previous audit fixes retained.
No reset or migration; stable remains 2.0.2. Existing descendants may survive a
lost local SSH/proot process; no remote cancellation or rollback claim is added.
[Evidence and limits](docs/fork/AUDIT-FILE-RECOVERY-2026-10-24.md).

## Earlier private audit — file transport lifetime audit (2.0.11-test)

Keep one directly owned child per ProjectFiles transport until cleanup/exit.
Close and timeout target that operation, including unread streams; old stream
close cannot stop a replacement operation. Enforce byte limits during read/skip,
retain interruption, and reject overlapping operations before launching a child.
SSH host-key checking, file destinations and terminal sessions remain unchanged.
Private code1340, same package/signer/schema8; previous audit fixes retained.
No data reset or migration. Public stable remains v2.0.2. This stage does not
establish cancellation of remote/proot descendants or rollback of completed work.
[Evidence and limits](docs/fork/AUDIT-FILE-TRANSPORT-2026-10-24.md).

## Earlier private audit — file manager confirmation/export audit (2.0.10-test)

Bind Copy/Move menus, preflight and confirmations to the current browser choice;
refuse dismissed, replaced, changed-selection and replayed callbacks. Create
Download exports exclusively without following destination links; number name
collisions, enforce byte limits and check cooperative close/interruption. Clean
private export staging on early/error/download exits while retaining successful
Share URI files for recipient access. Existing batch transfer script/layout and
transport shutdown remain unchanged. Private code1339, same package/signer/schema8;
all previous audit fixes retained. No data reset or migration; stable remains 2.0.2.
[Evidence and limits](docs/fork/AUDIT-FILE-MANAGER-2026-10-24.md).

## Earlier private audit — environment/package installer audit (2.0.9-test)

Observe installer exit without reaping its leader before final group cleanup;
remove owned descendants after success, failure, output errors and cancellation.
Keep the Java operation occupied until the started process exits, including
read/callback/stdin-close failures and interruption. Stage scripts/cancel markers
per invocation and preserve retained native download caches. Resolve shell links
inside the selected guest rootfs and refuse missing/nonexecutable/looping targets.
Private code1338, same package/signer/schema8; no data reset or automatic rootfs
replacement. Previous audit fixes remain included. Public stable remains v2.0.2.
[Evidence and device limits](docs/fork/AUDIT-INSTALLERS-2026-10-23.md).

## Earlier private audit — updater and external-file audit (2.0.8-test)

Require incoming shared files to use a basename; create without overwriting files
or links, clean failed partial writes and close received streams. Give each update
request an owned cancellable HTTPS connection with cleanup on redirects/errors.
Recheck cached size/SHA-256 and current package/version/signer in the background
before Android installer handoff; use read-only URI grants with ClipData.
Private code1337, same package/signer/schema8; previous audit fixes retained.
Public stable remains v2.0.2. No data reset; duplicate incoming names now require
renaming. [Evidence and limits](docs/fork/AUDIT-UPDATES-ENTRYPOINTS-2026-10-23.md).

## Earlier private audit — workspace persistence audit (2.0.7-test)

Share one FIFO AtomicFile writer and pending snapshot per workspace file across
screen recreation. Finish queued writes after close while rejecting late saves
and callbacks to a closed store. Route write failures to remaining live owners.
Retire the writer when its last owner and queued write are gone. Reject duplicate
command/session/note IDs on load and encode; leave malformed source libraries
intact/read-only rather than overwriting them. Valid schema8 data needs no reset
or migration. Private code1336, same package/signer; public stable remains v2.0.2.
[Validation and limits](docs/fork/AUDIT-PERSISTENCE-2026-10-23.md).

## Earlier private audit — transfer/attachment audit (2.0.6-test)

Refuse raced-in directories and directory symlinks when publishing downloaded
files. Bind download/upload/move confirmations to their current selection;
dismissal, replacement, disconnection and controller close invalidate old choices.
Freeze connection settings and selected move paths. Preserve pinned SSH trust,
SFTP wire behavior, account storage, completed files and previous audit fixes.
Private code1335, same package/signer/schema8. Public stable remains v2.0.2.
[Evidence and device limits](docs/fork/AUDIT-TRANSFERS-2026-10-23.md).

## Earlier private audit — storage lifecycle audit (2.0.5-test)

Keep backup/restore/cleanup mutually exclusive until the owned Python worker has
exited, including delayed or interrupted cleanup. Send termination only once;
publish the terminal result after cleanup and keep cancellation progress visible.
A late Cancel cannot discard a completed restore result. Preserve user data,
archive format, schema8, package and signer; private code1334. Public stable
remains v2.0.2. [Evidence and limits](docs/fork/AUDIT-STORAGE-2026-10-23.md).

## Earlier private audit — X11 lifecycle audit (2.0.4-test)

Keep component cancellation busy until its owned process group exits; do not
publish retryable failure before cleanup. Wait after desktop output-reader
errors. Restore explicit launcher repair for ordinary Ubuntu. Preserve existing
renderers, rootfs, user data and agent-chat fixes. Private code1333, same package/
signer/schema8. [Audit and device limits](docs/fork/AUDIT-ENVIRONMENTS-X11-2026-10-23.md).

## Earlier private audit — agent chats (2.0.3-test)

Restore permission buttons after failed replies across all four agents. Reject
stale OpenCode sends/forms/details/interrupts and isolate its control socket per
launch. Permit Gemini same-ID retry only when readiness fails before native
dispatch. Capture queued close/interrupt ownership before a session can restart.
Preserve native history, connection choices and auth storage. Private code1332,
same package/signer/schema8; public stable remains v2.0.2.
[Audit, compatibility and validation limits](docs/fork/AUDIT-AGENT-CHATS-2026-10-23.md).

## v2.0.2 — Agent chat improvements

Send varied files directly to Codex, OpenCode, Claude and Gemini chats, from phone
storage or a selected local environment, with optional message text. Keep originals
available to tools; use native media routes for images and timestamped video frames
without audio. Return local outputs as file/media/APK cards for preview, saving and
explicit Android installation. Add local media thumbnails, compact viewers and APK
size/SHA-256 metadata. Add Codex first-login guidance and an optional Android
Keystore-protected Gemini API-key field. Fix OpenCode staging-root integration and
bounded native media replies. Refine the red remove cross, context notice and
italic app/version header. No X11/renderer or unrelated session runtime changes.

Android8+/ARM64, unchanged com.termux/signer/schema8, code1331. No reset or automatic
environment migration. Restart agents with saved chats; duplicate/import requires
Gemini key re-entry. HezeAI confirmed Codex image input and OpenCode file sending.
Private development builds1326–1330 are archived under docs/fork/development; their
labels are not public releases. [Notes](docs/fork/RELEASE-v2.0.2.md).

## v2.0.1 — Fix Wrapper

Fix GPU window overlap with clipped presentation, preserve restored preset/history
identities and verify saved settings. Validate renderer components before skipping
setup and offer consented Turnip cache repair. Improve isolated test fixtures.
Android8+/ARM64; unchanged package/signer/schema8, code1325, no data reset.
Repeated imports create independent preset copies. GPU frame copying can cost
more CPU. [Release notes](docs/fork/RELEASE-v2.0.1.md),
[validation](docs/fork/VALIDATION-v2.0.1.md).

## v2.0.0 — Termux-job's X11 AVAILABLE

A major release since v1.7.5: integrated XFCE for eight systems; CPU,
Turnip+Zink, VirGL and a Termux-only custom-driver Wrapper; per-target desktop
services, keyboard/audio/storage integration, a program manager with progress,
Stop/Continue, removal and cached catalogs. Optional Kali-inspired Termux style.
Independent Ubuntu AI, opt-in verified migration and selected-environment
files/packages; native environment installation/error reports; agent chat,
focus and per-session input fixes; visible session gears and an optional
connection command. Italic app header with v2.0.

Tested on RedMagic10Pro and SamsungS26Ultra; other devices require testing.
No blanket Mali/Dimensity or all-combinations claim. Android8+/ARM64,
unchanged package/signer, code1320, schema8, no general reset from1.7.5.
Migration is optional; the original Ubuntu is not automatically removed.
[English release notes](docs/fork/RELEASE-v2.0.0.md),
[validation](docs/fork/VALIDATION-v2.0.0.md),
[upstream credits](docs/fork/THIRD-PARTY.md).

# v1.7.5 — Storage cleanup, package history and chat fixes

- Add storage overview/selective cleanup for library data, HOME entries, package
  caches, reports, recognised backups and complete supported Linux instances.
  Require explicit consent, protect busy environments and native remove locks;
  PREFIX remains view-only. Unify restored-copy removal in cleanup; show inline
  scanning progress. See STORAGE-CLEANUP.md for irreversible/partial behaviour.
- Record per-environment package history and operation results, including errors,
  cancellation and unknown outcomes; add APK/DNF/Zypper version selection.
- Fix older-chat pagination, queued clicks, retained rows, cache trimming/frontier,
  retry/end cursor and prepend scrolling without changing cache budgets/drafts.
- Protect Codex new→id binding until history is materialized; preserve explicit
  next-chat settings against old-run status, including store reload. Explain
  unavailable history without silently replacing it.
- code1220, same package/signer/schema8; no general reset from1.7.0. Private
  1.7.5-test also uses1220; no artificial update code for testers. Retain previously
  announced pre1.5.3 local-AI private-data cleanup on upgrades from those versions.

# v1.7.0 — Linux environments, native packages and backups

- Add confirmed removal of restored instances using nonblocking native proot
  session/removal locks; preserve Download archives/originals/reports, clean linked
  presets after success. Unknown copies cannot be removed by name alone. Track
  new partial restores and successful deletion tombstones. Remove testing labels.
  code1215; same package/signing identity/schema8, no data reset.
- Include an empty private .l2s runtime directory in freshly restored rootfs
  when absent, before restrictive root modes are applied. Preserve Arch0555
  root permissions; refuse foreign file/symlink at the runtime path. code1214.
- Permit literal systemd hex-escaped filenames in archives (backslash-xHH),
  without decoding them. Preserve traversal/link and nonstandard backslash
  rejection. Fix false unsafe-path failure creating Arch backups. code1213.
- Recognize regular bin/busybox and usr/bin/busybox ELF for Alpine archive
  architecture. Do not dereference sh/env symlinks; mismatched/unknown binaries
  remain blocked. code1212. Debian/Void restored and original restarts confirmed.
- Fix proot archive rebasing: regenerate PAX path/linkpath headers from new
  jobs-* names rather than retaining source aliases on long paths. Normalize
  sparse inputs to regular logical file data. Existing backup files unchanged.
  code1211; prior failed native restore may leave an incomplete fresh instance.
- Rebuild restored proot launch metadata from safe defaults instead of foreign
  OCI commands/environment. Preserve rootfs/passwd; allow sh-only Void to use
  native run in the fresh isolated restored instance. No live rootfs replacement.
- Show separate Install/disabled Installed badges beside environment names.
  code1210 during development; full six-system restore cycles subsequently verified by the developer.
- Accept valid XBPS versions starting with letters (e.g. mksh R59c_2),
  preserving the required revision suffix and package-name validation. Fix
  Void catalogue rejection caused by13 such entries; checked14204 cached rows.
  code1209. Fedora/openSUSE tree installation and execution confirmed on device.
- Fix Fedora DNF5 catalogue format: pass an actual tab in queryformat rather
  than backslash-t, which DNF5 prints literally. Preserve strict row parsing.
  code1208. Developer confirmed Alpine tree2.3.2 installation and execution.
- Fix empty Alpine available list: read the cached apk list without the
  --available filter that excludes remote-only packages with --no-network.
  Keep installed/upgradable filters and offline catalogue reads. code1207.
- Fix host Termux APT being falsely detected inside Alpine through proot PATH
  fallback. Restrict guest package discovery, reads and confirmed commands to
  guest Linux paths in a subshell, without changing the interactive shell PATH.
  code1206 during development; six-system package checks subsequently passed.
- Connect environment-local package catalogues/descriptions/confirmed operations for
  APT, APK, DNF, Zypper, XBPS and Pacman. Preserve concrete proot aliases and
  reject package operations after falling back to the host shell. Native catalogues
  use cached metadata; refresh is explicit. Pacman actions use full system upgrade
  with a warning. In-panel version selection remains APT-only. code1205.
- Add the Termux environments launch family, concrete proot instance selection,
  explicit entry command, confirmed missing-distribution setup and readiness check.
- Bind session files and restored ordinary presets to the selected/new instance;
  preserve existing shared Ubuntu and agent startup paths. No migration to ubuntu-ai.
- Replace terminal shortcuts in AI-session long-press menus with Install packages,
  including Codex; retain the robot terminal button above chats.
- Fix entry into the installed Void OCI: root's passwd names missing /bin/bash,
  although /bin/sh exists. Use native OCI run with explicit sh only for this
  missing-Bash configuration and an empty Entrypoint. Files and displayed entry
  commands share this decision. Existing Ubuntu/AI paths remain unchanged.
- Enable modern Void Linux installation from its official multi-architecture OCI;
  enable Arch Linux ARM from a pinned Termux rootfs, with HTTPS/SHA-256 validation,
  cleanup and explicit ARM variant consent. Existing distro and legacy paths retained.
- Test code1204 updates code1203; library format unchanged. Earlier code1203
  superseded an accidentally installed previous1.6.6 rollback/code1202. Older readers preserve the
  library but lose new proot preset fields on resave; see ENVIRONMENTS.md before rollback.

# v1.6.5 fix — changes since public v1.6.0

- Fix Codex large-history startup disconnect with one-turn pages and a bounded
  chat-only32MiB wire limit; preserve the first failure and refuse false ready.
- Fix composer focus loss on streaming messages/action updates in all four chats;
  scroll the viewport without fullScroll focus navigation. Preserve draft/IME.
- Add compact expandable action timelines, RU/EN tool/status labels, original
  details and supported skill-read/attachment clouds. No transcript reordering.
- Share action cards with OpenCode, Gemini and Claude, using native metadata;
  unknown statuses/tools and unsupported skill loading are not guessed.
- Align OpenCode inventory with launch CLI/private Node/legacy npm paths; retain
  pinned-version verification and consent guards.
- Same com.termux/signer, Android8+/ARM64, code1198. schema8/server/backup formats
  unchanged; no reset from1.6.0/private1.6.x. Reopen the same native history after
  finishing important tasks. Pre1.5.3 local-AI cleanup warning remains in metadata.
- Developer confirmed focus/cards/OpenCode check on private builds. Native Codex
  terminal mouse-escape echo/stale startup text is separately deferred.

# v1.6.0 — changes since public v1.5.3

- Android-native chats for Codex, Gemini CLI, Claude Code and OpenCode, with stable
  message rows, expandable reasoning/tools, supported forms and native approvals.
- Compact robot terminal, large extra keys/Enter, separate Minimize and Return to
  agent chat; keyboard works through dialog IME/mouse-tracking touch handling.
- Native model/permission/thinking controls where supported; pending next-turn
  Codex selection and correct Pro weekly-only quota presentation.
- Confirmed launch-environment checks/setup; guided Gemini Vertex project/region
  connection. Native free Google sign-in remains disabled; billing/IAM are external.
- Session file browser: independent selection/navigation, clear/Select all by
  visible filter, Copy/Move here with no automatic overwrite or cross-environment move.
- Server selection shared by download/move/delete, multi-folder safe deletion,
  cancel selection/prepared moves and filter-aware Select all; retained connection
  vs data auto-update controls. Existing agent/SSH/backup payloads retained.
- Same package/certificate, schema8 and server store; optional added fields,
  code1192, no general reset from1.5.3. Earlier local-AI removal remains for older
  installations. New chat UI applies to new launches; native histories retained.
- Developer confirmed Codex chat/keyboard/return. Full real-account Claude/Vertex
  validation remains incomplete; exact automated evidence is in VALIDATION-v1.6.0.md.
- Feedback relay/server and anonymous issue submission are deferred, not included.

---

# v1.5.3 — changes since public v1.5.0

- Remove local-AI code/runtimes/dependencies; migrate away old local profiles and
  delete only app-private local models/chats/prompts/settings/diagnostics on launch.
  Export needed local data first. External agents, environments and other libraries remain.
- Preserve adapters, Native protocols, tool/permission wrapper, context/cache,
  performance/SDK experiments and unfinished LoRA work in LOCAL-AI-RESEARCH.md.
- Add server trash selection and confirmed recursive deletion without following links.
- Add in-app background upload/download, restorable progress windows, card progress
  bars and an animated Server + Agent border. No process-death transfer persistence.
- Add thin session activity line and AI Agent robot icons. Preserve activity across
  keyboard resize/scroll; filter idle redraw and bound frame callbacks to visible views.
- Optimize backup inventories and bounded memory reuse; add title-right Refresh,
  inline scan spinner/status and owned cancellation in full/custom modes.
- Separate gzip1/pigz2/pigz4 from level1/3/9, retain archive format and fresh validation;
  fail rather than omit a missing selected full Ubuntu.
- Same com.termux/certificate/schema8; Android8+/ARM64; code1165 above private1164.
  No general reset. See RELEASE-v1.5.3.md for complete compatibility information.

# Historical intermediate development notes

---

## Private v1.5.27-test

- Backup Refresh at title-right in both modes; inline spinner and one status line.
- Reuse bounded process-memory estimates on repeated entry; fresh manual refresh.
- Close cancels only the owned scan; retain components/destination across refresh.
- Split gzip1/pigz2/pigz4 method from compression level1/3/9; validate thread count.
- Fail if a selected full environment disappeared instead of silently omitting it.
- Same package, certificate, library schema and backup format; no reset.

## Private v1.5.26-test

- Faster full/custom backup inventory: reuse metadata and compile category rules.
- Reuse bounded per-dialog estimates when adding folders; manual fresh refresh.
- Fresh reads/space estimate, SQLite handling and archive validation retained.
- Scrolling preserves ongoing activity-line output, filtering idle redraw.
- No data reset or backup-format change.

## Private v1.5.25-test

- Preserve ongoing output activity when keyboard visibility changes terminal size.
- Keep idle resize/scroll redraw filtering; resizing cannot prolong activity.
- Set terminal-line and server-border animation intervals to16ms (approximately60fps). Server polling unchanged.
- No data migration/reset; all1524 server and UI functions retained.

## Private v1.5.24-test

- Filter activity-line flashes caused by scrolling, tab/session return and resize;
  reduce thickness to1dp and keep genuine available busy-state signals.
- Remove header dots; restore original other-session title Toasts.
- Preserve background transfers, trash and AI Agent robot icons. No data reset.

# v1.5.23-test — server files, background transfers and activity

- Upload/download progress can be hidden with Background and restored from active-only profile rows above Open; simultaneous directions have separate rows. Paint-only clockwise border on Server + Agent while either transfer runs. Transport/cancel semantics retained; no Android foreground service or process-death recovery.
- Paint-only current-session activity line, per-handle PTY output signals and best-effort plain-shell foreground-job probe; no additional AI RPCs. Other-session title Toast replaced by a centered dots/event crossfade and a switchable session popup. Fixed geometry, no terminal/header rebuild on animation/progress.
- Vector robot in opened-session list for declared Codex/Claude/Gemini/OpenCode profiles; normal terminal icons retained.
- Vector trash after Home: toggle selection mode, cancel on a second tap with no selection, otherwise confirm permanent deletion with Yes/No and a scrollable list.
- Reuses long-press multi-selection; both trash and download/move cards highlight while selected. Directory/profile change resets selection mode.
- SSH/Python helper deletes files/folders and unlinks symlinks without following targets. FD-based ancestor traversal, full preflight, changed-object/mount checks, 500 selected roots/20,000 nodes/depth64/25-second limits. No agent update or automatic retry.
- Partial/interrupted deletion is reported; reload before retry. Typed FILE_DELETE diagnostics record operation errors without paths or payloads.
- Same package/signing/schema, code1160; no local AI restored; private delivery, no GitHub publication.

# v1.5.22-test — remove on-device AI

- Remove the complete local-model feature: Java UI/service/tools, native llama runners, LiteRT-LM/QAIRT runtimes, model catalog, import/download/settings and local histories.
- Migration skips retired local-AI profiles instead of launching them as shells. Schema8 and unrelated data/settings remain; first launch deletes only the retired app-private model/history/cache directories and diagnostics preferences, without following symbolic links.
- Keep shared history closing controls, Codex history manager, external CLI agents and their permissions, server connection/file-transfer UI, packages, editor, backup and update features.
- Dedicated jobs-local-ai Ubuntu removed on the developer's device by explicit request; working ubuntu preserved. No generic environment auto-removal in the APK.
- Same com.termux/signing identity; code1159 >1158. Private local delivery, no GitHub publication. APK/package audit and device limitations in VALIDATION-v1.5.22-test.md.

# v1.5.0 — sessions, histories and server file operations

- Unsaved-session closing: keep-history checkbox, Close / Add preset and close; native CLI chat storage remains independent.
- Global supported Codex chat history browser, preview and explicit confirmed deletion through the native API; no silent archival fallback.
- Clearer native AI-agent rights, confirmation and path-scope controls where supported; existing profiles preserved.
- Server multi-selection via long press; two equally sized vector panels ↑ / ↓; Select all / Clear.
- Pipelined bounded-window SFTP downloads, files and recursive folders, destination picker for Termux/proot/device storage, quick Download retained; 10 GiB/file.
- Exclusive destination batch folder preserves names and hierarchy, avoids overwrites. Progress, speed, remaining estimate and cancellation; completed files kept after interruption.
- Same-server nonoverwriting moves via Move / Move here; collision and recursive-self checks; cross-filesystem moves can fail safely without fallback copying.
- Public Local models entry disabled during testing, including existing-preset startup; experimental data retained. Private testing continues separately.
- Existing package/signing/data schema v8; stable code1137, private test1138; no reset. In-app updater metadata included.

# v1.4.30-test

- Operating mode CPU/GPU in Engine settings; GPU hides CPU mode/thread settings and preserves their saved values.
- Separate opt-in OpenCL runner, initially catalog Qwen3.5 2B / SM8750; explicit failure and manual CPU recovery, independent Ubuntu tools.
- Tool validation: original call, named duplicate/invalid argument, selected-tool schema, readable RU/EN details.
- Empty write explicitly does not delete; permission-aware deletion/verification and quoted HTTP command guidance.

# v1.4.29-test

- Optional native Qwen function schemas/call/result adapter for catalog Qwen3.5 2B, with strict parsing and the existing authorization/confirmation pipeline.
- Engine settings protocol selector; persisted per session/preset, JSON default, same-chat reload.
- Identical adapted messages for exact token count and generation across inference locations; native libraries and model weights unchanged.

# v1.4.28-test

- Local-AI Environment: dedicated Ubuntu for new sessions; optional explicit Termux command routing, three independent inference locations; new llama defaults Ubuntu, LiteRT Android only. Legacy environments unchanged until confirmed.
- Behavior instructions below Permissions: bundled/custom UTF-8 TXT/MD import from device storage, private copied text, view, help/template and reset.
- Rights prompt: generated from current settings or explicitly acknowledged custom edit; red risk warning, unchanged code authorization, stale settings detection, auto reset.
- Standard guide explains real command execution, permission-aware dependency installation/HTTP requests, exact deletion targets and multi-step completion. A bounded continuation handles a promised next step after a successful action.
- Unsaved session closing: Keep history checkbox, Close or Add preset and close. Local deletion waits for owned writer/model shutdown; external CLI histories are not deleted by closing.
- Chat histories in global settings: local/Codex catalogs, search/filter/view, exact confirmed deletion, active-chat checks and independent preset bindings. Native Codex thread/delete required; no archive/filesystem fallback.

# v1.4.27-test

- Local llama/LiteRT chats automatically receive a short APK-bundled operating guide, before authoritative current settings.
- Guidance covers chat/read/full-access modes, actual tool execution, dependency checks, filtered listings and deletion target verification.
- No authorization, native engine, history schema or external CLI-agent changes.

# v1.4.21-test — isolated synthetic prefill diagnostics

## v1.4.26-test

Local AI Environment settings separate tools from engine location: Embedded Android, native Termux, or the same dedicated Local-AI Ubuntu. Experimental CPU runners share model files/history and keep existing permissions. Includes compact file-list context and clearer tool-call draft status.

## v1.4.25-test

All local models: compact file-list metadata for inference, full raw results retained; instructions recommend filters and requested count for newest-file tasks. Larger requested lists remain available.

## v1.4.24-test

Local AI: shorter system/tool instructions and smaller old-result excerpts for lower input cost; full saved history retained. Saved preset menu order and model-picker palette aligned with other settings.

## v1.4.23-test

Local AI diagnostics now reports input token composition per inference pass, using the model tokenizer for llama totals and labelled estimates for components. Session/preset button shortened to Engine settings.

## v1.4.22-test

Local AI: Engine settings now groups processor mode and CPU thread selection. Auto preserves the previous four-thread cap; manual options are 2/4/6/8 where supported. Existing chats and presets are retained.

- Input-length, thread-count and batch-size tests in local diagnostics, using temporary native contexts and the selected CPU variant.
- Exact per-trial/per-batch decode wall time, process CPU time, throughput, progress, stop and separate persisted/exportable report.
- Benchmark-only cancellation gate handles early/active/late stop without contaminating chat cancellation; regular settings/cache/history remain intact.
- Same package/certificate, code1127, no reset. Actual Qwen length/thread results require running the test in-app.

# v1.4.20-test — tool validation recovery and repeated-read guard

- Return schema errors to the local model for up to two corrections, without executing invalid calls or bypassing rights/approval.
- Distinguish category filter from extension; preserve non-executable prose handling and cancellation.
- Execution-metadata Cached labels, persisted in history, and bounded repeated cached reads with honest incomplete-task notice.
- Rename Automatic to ARM acceleration (when supported); native backends unchanged. Same identity/code1126, no reset.

# v1.4.19-test — guarded ARM CPU engine variants

- Session/preset Engine selector: Automatic or Compatible CPU for llama.cpp; actual backend/threads in diagnostics.
- Separate guarded dotprod/FP16/i8mm compute library, hidden internal symbols and distinct JNI namespace; generic baseline retained.
- Mode persistence/private IPC validation and safe idle reload with same chat; no rights/history or LiteRT behavior change.
- Same package/certificate, code1125; additional library increases APK size. Qwen speed requires device comparison.

# v1.4.18-test — latest-file routing and previous-result excerpts

- Recognize short folderless latest-file requests and ask for a folder without model inference.
- Latest screenshot phone aliases; text-only discovery returns real paths and honest vision limits, preserving capable-model flow.
- Runtime-only excerpts of older large tool results with source/recall hints and chat notice; full archive and current task/results preserved.
- Same native libraries/settings/permissions, no reset; code1124.

# v1.4.17-test — diagnostics for every local model call

- Keep per-call input/cache/timing/output metadata instead of only the final call.
- Completed, active and interrupted records; bounded archive, journal-pause handling, bilingual reports and honest unknown LiteRT counters.
- No inference or native-library changes, no reset; code1123, same package/signature.

# v1.4.16-test — llama input timing and prefix reuse

- Live native processed/total/cached token counters; separate setup, prefill, cache and first-step timings in existing local-session diagnostics.
- RAM-only prefix reuse scoped to owner/model/chat/exact instructions; bounded stored snapshots for unsupported partial rollback and full-evaluation fallback.
- No data reset; code1122, same package/certificate. Qwen3.5 device speed remains to be checked.

# v1.4.15-test — local budgets and timing diagnostics

- Saved per-session/preset context and reply limits; real IPC/native/SDK wiring, safe idle reload for context changes.
- Phase placeholders and live metadata-only local diagnostics for llama.cpp and LiteRT, with export and last-report persistence.
- Fast RU/EN find-latest-text-and-read without model inference. Defaults4096/768 retained, code1121, same signing identity, no reset.

# v1.4.14-test — local context, recall and direct text

- Runtime-budget context reduction with a chat notice and unchanged full saved transcript; exact GGUF counting, estimated LiteRT budgeting and one overflow retry.
- Current-chat history search tool; no other chats opened and no permission changes.
- Reuse identical successful reads within a turn, explicit refresh support and invalidation after writes/commands/installations.
- Immediate literal display for supported short latest-text read requests, without model inference.
- Same package/certificate, code1120; no reset. Device validation pending.

# v1.4.13-test — latest files in the requested folder

- Shared local-model discovery for short RU/EN latest-file requests with known folder aliases and exact extension filtering before date sorting.
- Read/open text requests continue with the exact discovered file; find-only and binary requests do not automatically read or execute.
- Checked directory visible in listing details; existing rights/scope and named-file selection preserved. Same package/signature, code1119, no reset.

# v1.4.12-test — local file search and selection

- Case/suffix tolerant file-name search with approximate Russian/Latin spelling and explicit alternate names from the model. This is not universal translation or semantic search.
- Multiple matching files, and even one approximate match, pause the local-agent turn for an explicit card-based selection. Cancelling stops continuation; selection grants no execution/install/write permissions.
- Selection opens a scrollable list with names, full paths, sizes and dates. Saved chooser events restore read-only and never silently resume a previous operation.
- Friendly file-search/screenshot result details; raw JSON behind Technical data.
- Shared by all local engines and imported models. Native/external agent controllers unchanged. Same package/signature, code1118; no reset required.

# v1.4.11-test — screenshot phrases and tool request cards

- Recognizes polite/greeting screenshot requests such as “привет посмотри ласт скрин”; questions, negations and explicit filenames/paths keep their own handling.
- Local-model tool JSON is presented as an unconfirmed request card with raw details on tap. User messages remain literal. Completed/failed execution still requires a real tool result.
- A mixed prose+JSON reply can receive one formatting correction for an action request; embedded JSON itself is never executed. Existing authorization and approval checks remain in force.
- Shared by catalog/imported local models; no added vision capability or changes to external agent permissions. Same package/signature, code 1117; no history reset.

# v1.4.10-test — agent permission controls

- Codex native action mode, project write scope and approval policy shown separately; existing profiles preserve their original policies.
- OpenCode adds opt-in external-path tool policy (not symlink-safe filesystem isolation): denies external directories, shell, execution, plugins, delegation and unknown tools while preserving selected file permissions.
- Claude/Gemini retain native modes; unsupported project enforcement is explicitly disabled, rather than advertised as a sandbox.
- Changes apply on the next launch, not to running sessions. No history reset or authentication changes. Optional v8 fields retain backward readability, but older builds ignore new approval/scope settings: review before any downgrade.
- Same signing identity and package; code 1116.

# v1.4.9-test — portable local-AI file discovery

- Local-AI settings selectors now use the ordinary session panel colors, text and ripple feedback.
- Shared screenshot finder for all local model engines: mixed image extensions, modification-time sorting, Android storage aliases, known screenshot directories and duplicate suppression.
- Short explicit last-screenshot requests start discovery before model generation; exact filenames and questions are left to the model with clear instructions.
- Reports checked folders and distinguishes absent folders, access denial and project restrictions. No permission bypass or fallback into another environment.
- Expanded known folder hints for downloads, pictures, documents, media and selected environment homes. External CLI agents retain their own tools.
- Same package/signing identity, code 1115; no data reset or schema change.

# v1.4.8-test — dedicated local-AI Ubuntu

Explicit Main Termux / Ubuntu for local AI selection; readiness check and confirmed first-time setup with progress; fresh persistent container with Python3/Git; no fallback to the working Ubuntu; legacy local profiles require environment reselection. Expanded confirmed package catalogue. No data reset or GitHub publication.

# v1.4.7-test — local file tools and media

Real metadata/newest-file search, environment-folder discovery, explicit file errors, APK metadata, experimental Gemma LiteRT images/video frames, and user-confirmed missing-tool installation with APT plan in chat. Local permission labels clarified. No reset/publication.

# v1.4.6-test — local session fixes

Session/preset rename; focus-neutral streaming; local project file browser; current permissions on the next turn without model restart. No reset or publication.

# v1.4.5-test — local chat presentation

Independent activity dots, expandable tool cards with actual execution state, and a left accent on user messages. Permissions unchanged; no data reset. Private test build.

## v1.4.4-test — local testing, not published

- Separate action permissions (Chat only / Read / Full access), project scope and Yes/No confirmation.
- Path checks ON blocks arbitrary shell commands before approval/execution, even in Full access; file tools stay project-scoped.
- Path checks OFF permits external app-accessible paths; only Full access allows writes/commands. Disabling requires an explicit warning confirmation.
- Existing profiles/choices retained; no data reset. Test code1109; refreshed baseline rollback1110. No separate isolation.

## v1.4.3-test — local testing, not published

- All local backends share an action-first turn coordinator with one recovery attempt for reasoning-only or non-executing offers. Explicit user paths are preserved by instructions, not silently rewritten.
- Qwen-compatible ChatML templates now use their official non-thinking generation suffix; native EOS/token-limit finish reason is reported.
- Optional per-session Confirm actions: Yes/No cards for full-access writes/commands; automatic execution remains the default. Path checks are independent.
- Reasoning opens in an independent scrollable dialog from a pill; raw history is retained. Tool results are labelled separately.
- Test code1107; recovery code1108. No data reset or GitHub publication.

## v1.4.2-test — local testing, not published

- Recognize structured tool JSON in json/tool/unlabelled fences; reject trailing prose, multiple objects and invalid fields.
- Move local New chat / Choose chat above Settings in the long-press menu; remove duplicates from the session selector. Model/access stay in Settings.
- Black local chat, inline You:/AI: labels and compact reasoning toggle; no colored cards.
- Package/signing retained, no data reset. Test code1105; refreshed recovery code1106.

## v1.4.1-test — local testing, not published

- Compact Local AI Start/Stop header and new/choose-chat selector actions.
- Offline Markdown/code copying and collapsed, preserved reasoning.
- Chat/read/full-access modes, project picker and independently switchable path checks.
- Full access runs tools automatically; commands are not a project sandbox.
- Test code1103; refreshed pre-feature rollback code1104. No data reset.

## v1.4.0-test — local preview, not published

- Local AI sessions/presets with a separate inference process, local chat history
  and input drafts; no project tools or command execution.
- Five models/six catalogue variants, pinned downloads with SHA-256, resumable
  partial downloads and custom model imports.
- llama.cpp CPU and LiteRT-LM GPU/CPU integration. NPU runtime is not included;
  NPU is explicitly blocked. MTP is not enabled. Device validation pending.
- Recovery APK: pre-feature v1.3.4 with manifest code1102; test code1101.
- Includes the earlier unreleased v1.3.4 search/browser-lifecycle improvements.

# Изменения Termux Job’s

## v1.3.4 — локальная проверка

- Поиск команд/заметок сохраняет панели и фильтры, обновляя результаты.
- Закрытые браузеры пакетов/файлов удаляются из реестров; списки освобождаются.
- Диагностика и фоновые опросы без изменений; данные без миграций.


## v1.3.3 — Обязательное обновление

- Исправлен конфликт вариантов Guava и отсутствие файловых классов в APK.
- Добавлены проверки runtime-зависимостей и определений DEX, тесты очистки и crash-log.
- Минимальная версия Android повышена до8.0/API26. Android7 не поддерживается.
- Обновление поверх, данные без сброса и миграций.


## v1.3.2 — локальная проверка

- В просмотре команды/заметки из звёздочки «Назад» возвращает к списку с прежней вкладкой и поиском; системный Back ведёт туда же.
- Офлайн-справочник:129 карточек, добавлены NOSPLIT/NODATA, netlink Permission denied и ADB после сопряжения. Уточнены signal9 и ограничения proot.
- Синхронизация папок не входит в эту сборку. Данные и форматы не меняются; com.termux/прежний сертификат, code1085.

## v1.3.1 — локальная проверка

Полный просмотр в звёздочке до вставки; редактирование и подтверждаемое удаление
команд/заметок. Поиск и текущая сессия сохранены, отправки при просмотре нет.
Справочник126 записей (+12),8 дополнений, AI-подпись справа в заголовке RU/EN.
Code1084, прежние пакет/сертификат/форматы, без очистки.
[Изменения](docs/fork/RELEASE-v1.3.1.md).

## v1.3.0 — справочник ошибок и улучшенная работа с файлами

Все изменения после публичной1.2.2: новый офлайн-раздел «Ошибки и решения»
с114 карточками RU/EN, поиском, фильтрами и источниками; база будет пополняться
в обновлениях APK. Подтверждённые случаи и нерешённые проблемы разделены.
SSH-ключ выбирается менеджером, импорт из другой среды подтверждается; passphrase
хранится через Keystore. Общий выбор файлов Termux/proot/память устройства,
проверка разрешений, до50 файлов на отправку. Скачивание в выбранную папку
или прежним быстрым пунктом в Download. Прежняя очередь SFTP сохранена.
Прежние пакет/сертификат, code1083, без миграции и очистки данных; update.json
для обновления через приложение. [Подробности](docs/fork/RELEASE-v1.3.0.md).

Ниже1.2.3–1.2.11 — история локальных проверок, не отдельные публичные релизы.

## v1.2.11 — опыт проекта и папка скачивания (локальная проверка)

114 карточек: добавлены36 случаев разработки,4 прежние карточки объединены
с дополнительными причинами и решениями. Источники проекта обозначены отдельно;
нерешённые проблемы содержат диагностику, без обещания исправления.
Скачивание с сервера: быстрый Download сохранён, добавлен выбор папки в Termux,
установленных proot-distro окружениях и памяти устройства с проверкой доступа.
Code1082, прежний пакет/сертификат, без изменения пользовательских форматов.
Новый резервный APK не создаётся, публикация после проверки разработчиком.

## v1.2.9 — локальные источники файлов и доступ к памяти

Проверка/запрос доступа к общей памяти для выбора ключа и отправки файлов.
Общий менеджер: Termux, установленные proot-distro окружения, память устройства.
Для загрузки убран системный picker, добавлено до50 отметок с сохранением между
папками. Прямая передача через прежнюю очередь SFTP, без промежуточной копии.
Основная1080, прежний резерв повышен до1.2.10/code1081, схемы/данные не очищаются.
[Использование](docs/fork/LOCAL-FILE-SOURCES.md).

## v1.2.7 — локальный выбор SSH-ключа сервера

Кнопка папки у пути ключа: Termux, Ubuntu и память устройства. Подтверждаемый
импорт в приватный каталог без перезаписи оригинала. Поле passphrase с Keystore,
явное удаление и разделение запросов пароля/расшифровки ключа. RU/EN.
Прежние профили/данные не очищаются, code1078 выше прежнего резерва1077.
Обновлён резерв v1.2.8/code1079 без библиотеки и нового SSH на прежней базе v1.2.2.
Подробности и ограничения: docs/fork/SERVER-KEY-SELECTION.md.

## v1.2.5 — локальная сверка Reddit и расширение справочника

78 карточек (+8): signal 9, pip, Termux:API, PATH, незакрытые кавычки,
Termux:X11 и окружение tmux после SSH. Советы из обсуждений сверены с первичными
источниками; неоднозначные исправления не включены. Никаких автоисправлений.
Code1076, обновляется поверх code1074/1075 без смены пользовательского формата.
Обновлён резерв без библиотеки: v1.2.6/code1077 (версионное повышение базы v1.2.2).

## v1.2.3 — локальная проверка библиотеки ошибок

70 карточки RU/EN из документации и отобранных обсуждений; поиск по сообщениям,
фильтры ОС/способа запуска/категории, объяснения и ссылки, копирование/вставка
команды проверки без выполнения. В общих настройках «Ошибки и решения».
Отдельная резервная v1.2.4/code1075 убирает библиотеку; первой ставить v1.2.3.
[Описание, источники и откат](docs/fork/ERRORS-AND-SOLUTIONS.md).

## v1.2.2 — обновлённая диагностика сессий и серверов

- Общий интерфейс диагностики сессий: сводка/события, обычный и подробный режим, остановка/возобновление, экспорт.
- Диагностика сервера без дополнительных сетевых запросов, локальные длительности операций/очередей и передач.
- Пауза журнала сервера сохраняется в профиле.
- Галочка напротив адреса разрешает опрос вне вкладки раз в 30 секунд; внутри — 10 секунд. Маленькие точки показывают состояние опроса, ошибки связи отдельно.
- Формат библиотеки не менялся, очистка данных не требуется. [Подробности](docs/fork/DIAGNOSTICS.md).

## v1.2.1 — ускорение отправки и очередей сервера

SFTP отправляет до8блоков по одному SSH-соединению; изолированы очереди
мониторинга и поиска/диагностики. Разработчик подтвердил ускорение отправки
примерно4–5раз на своём подключении; результат зависит от сети и сервера.
Защиты файлов/отмены сохранены, данные без сброса, агент прежний.

[Изменения](docs/fork/RELEASE-v1.2.1.md) · [Проверки](docs/fork/VALIDATION-v1.2.1.md).


## v1.2.0 — Сервер + Agent

Полный список относительно публичного v1.1.4; промежуточные версии локальные.
Старый SSH-вход в создании сессии сохранён.

## Сервер + Agent

- Кнопка во вкладке «Терминал», под пакетами и папкой, справа напротив карточки
  AI-модели/лимитов. Работает и в обычной сессии без карточки AI.
- Несколько профилей: адрес, порт, SSH-пользователь, ключ или пароль,
  автоподключение при открытии выбранного сервера. Пароль защищён Android Keystore.
- Подтверждение отпечатка SSH по независимому каналу и закрепление ключа отдельно
  для профиля. Изменившийся ключ автоматически не принимается.
- Программный агент устанавливается только после подтверждения в собственную
  папку SSH-пользователя. Меню «Настройки агента» объединяет проверку состояния,
  удаление и компоненты. Ошибка связи не считается отсутствием агента.
- Карточки CPU/RAM/диска, возраст снимка, опрос около10сек на экране сервера и30сек
  на других вкладках. В фоне приложения опрос приостанавливается. Автоопрос не
  пересоздаёт страницу и не означает переустановку/перезапуск агента.
- Разделы «Проекты / Задачи / Файлы / Службы». Проект хранит путь и помогает
  открывать SSH/файлы в нужном каталоге.
- «Отключиться» завершает мониторинг и медиатуннель; агент, серверные задачи,
  службы и отдельные SSH-сессии продолжают работать.
- Диагностика доступности порта, доверия SSH, Python и файла агента. Если нельзя
  отличить проблему авторизации от отсутствия Python, интерфейс сообщает об этом.
  Проверка не запускает агент/задачи и не выводит секретный stderr.

## Задачи, команды и расписания

- Запуск Bash-команд на сервере с каталогом, названием и подтверждением.
  До2 одновременно, очередь ограничена, предел выполнения24часа.
- Журнал с обновлением, копирование вывода, состояние, длительность и exitCode.
  Показаны последние32КБ, запись ограничена2МБ на задачу, до40 записей.
- Остановка управляемой задачи с предупреждением для APT. Завершённую/ошибочную/
  остановленную запись можно удалить вместе с журналом; результаты команды
  и файлы остаются. Неподконтрольную запись можно забыть с предупреждением,
  процесс это не останавливает.
- Закреплённые команды: название/каталог/Bash, до50 на профиль. Можно добавить
  вручную или из журнала. Каждое выполнение создаёт отдельную задачу и требует
  подтверждения сервера и текста команды; открытие списка ничего не запускает.
- В запуске задачи доступны «Сейчас / По расписанию». Cron и systemd --user:
  создание, просмотр последнего журнала, включение/отключение, изменение времени,
  удаление собственных расписаний. Показываются серверный часовой пояс и CRON_TZ.
- Расписания работают без приложения, независимо от агента и остаются после
  удаления агента/профиля. Отключение отменяет будущие запуски, не останавливает
  текущий; удалить выполняющееся расписание нельзя. До30 расписаний,1час на запуск,
  без параллельного запуска одного и того же расписания.
- Просмотр служб, журнала и подтверждаемый перезапуск в пределах прав пользователя,
  без автоматического sudo.

## Файлы, редактор и передача

- Просмотр папок, ввод пути, вверх/домой/обновить, векторные значки и сохранение
  прокрутки при открытии папок/разделов. Файловый список не мигает при мониторинге.
- Свойства, безопасное переименование без перезаписи, удаление файла с подтверждением.
  Рекурсивное удаление папок не добавлено.
- Просмотр UTF-8-текста и кнопка редактирования, включая Markdown. До256КБ;
  предупреждение о несохранённом тексте, проверка конфликта перед записью,
  текст остаётся в окне при ошибке. Черновики не сохраняются после убийства процесса.
- Избранные папки/файлы по серверам. Папка: открыть → звёздочка → добавить текущую.
  Файл: меню → «В избранное». Из звёздочки — переход к папке или действия с файлом.
  До100 папок и100 файлов; удаление из избранного не удаляет данные на сервере.
- Лупа: поиск имён в текущем каталоге и подпапках, без учёта регистра, без чтения
  содержимого и намеренного перехода по ссылкам/другим файловым системам.
  Пределы8сек/20000записей/500результатов; неполнота и пропуски показаны явно.
- Отправка выбранных Android-документов по SFTP: отдельное подтверждение замены,
  приватный временный файл, проверка изменения целевого файла, отмена/очистка.
- Скачивание с отдельным SSH-соединением и уникальным именем в Download.
  Прогресс, переданный объём, средняя скорость и примерное время до окончания
  (при известном размере). До2ГБ/файл, скачивание до10мин, без возобновления.
  Не закрывайте приложение до завершения: фоновой Android-службы передачи нет.

## Видео и аудио: важное про плеер и HTTP

- Встроенный плеер AndroidX Media3 с системными кодеками, без комплекта дополнительных
  декодеров. Есть внешний плеер/скачивание и остановка просмотра.
- Сервер отдаёт медиа через временный loopback SSH-туннель со случайным токеном,
  поддержкой HTTP Range/HEAD и сроком до30минут. Публичный HTTP-порт не открывается.
- **HTTP разрешён только для точного127.0.0.1 внутри локального туннеля. Внешний HTTP
  запрещён; участок между приложением и сервером шифруется SSH.** Другие приложения
  устройства могут пытаться подключиться к локальному порту, поэтому временную
  ссылку с токеном нельзя передавать посторонним. Токены не пишутся в access log.
- Исправлена блокировка локального HTTP политикой Android. Ошибки плеера показывают
  безопасный код Media3/HTTP и причину без URL/токена/raw exception. Поддержка
  конкретного контейнера/кодека зависит от Android; универсальная совместимость
  файлов не обещается.
- ffprobe для сведений о медиа, FFmpeg для обработки — необязательные компоненты.
  Для APT доступна отдельная подтверждаемая задача установки; автоматической
  установки при открытии сервера/видео нет.

## Совместимость и ограничения

- **Очистка команд, заметок, пресетов, истории и окружений не требуется.**
  com.termux, прежний сертификат, versionCode1071, библиотекаv8 сохранена.
  Устанавливайте поверх, не удаляя приложение. Завершите важные задачи:
  Android при обновлении может остановить процессы.
- Обновление через приложение поддерживается: ручная проверка и включённая
  автопроверка используют Latest и update.json. Автопроверку можно отключить;
  скрытая установка не выполняется, установку подтверждает пользователь в Android.
- Для нового серверного агента нужны Linux с /proc, Bash и Python3.8+; клиенту —
  OpenSSH в Termux. Агент работает с правами выбранного SSH-пользователя, root
  означает полный доступ. Пакеты и системные службы молча не устанавливаются.
- Агент не настраивает автозапуск ОС. Обычные задачи переживают отключение
  приложения, но не перезагрузку сервера. Не используйте &/nohup для управляемых
  задач. Установка/удаление агента не изменяет проекты и чужие службы.
- Cron требует работающего daemon, systemd — user manager; без linger таймеры
  могут остановиться после выхода/перезагрузки. sudo/linger автоматически не
  включаются. Не редактируйте crontab одновременно другими средствами:
  общий атомарный CAS с внешним редактором отсутствует.
- Профили/избранное/закреплённые команды хранятся локально, не входят в экспорт
  библиотеки. Keystore-пароли не переносятся как обычный файл. Не включайте
  секреты в закреплённые команды; команды/журналы могут содержать приватные данные.
- Старые версии могут потерять новые необязательные поля при перезаписи профилей.
  Для локальныхv1.1.5–13 агент не изменён, переустановка не нужна.
- AI Agents и бэкапы сохраняют прежние ограничения; мерцание OpenCode OC-001 открыто.
  Настоящий cron/systemd, Android UI/кодеки/SSH/SFTP и восстановление требуют
  проверки в вашей среде; локальные тесты не заменяют её.

## Проверки

379 JVM-тестов,6 Python-проверок поиска,20 проверок агента и15 проверок расписаний
прошли; расписания повторно проверены при подготовкеv1.2.0. Финальный APK:
ZIP, com.termux/версия/ARM64/minSDK/debug-флаг, подпись, SHA-256, совпадение
исходников со сборочным stage и ограниченный скан секретов проверены.
Полный чистый Lint не заявляется из-за прежних несовместимостей инструментария.


---


## v1.1.4 — улучшенный менеджер резервных копий и обновления

Главное — улучшение менеджера бэкапов из шестерёнки справа сверху (тестирование).
Включены все локальные изменения v1.1.1–1.1.3 относительно публичной v1.1.0:
выбор компонентов Termux/Ubuntu, Codex/SQLite/история/авторизация/проекты/SSH,
зависимости, недоступные пути, согласие, таймер/полоса/имя файла, gzip/pigz,
уровни1/3/9, подтверждаемая установка pigz, защита всех окон от outside taps.
Проверка структуры/SHA-256, отдельное извлечение и сравнение пакетов сохранены.

- Обновления объединены в одну плашку: ручная проверка по названию и галочка
  «Авто» с пояснением «Проверять при открытии приложения».
- Одна фоновая попытка при каждом открытии/возврате вместо интервала10часов;
  ошибка не вызывает повторов в том же открытии. Поворот экрана/вкладки не повторяют.
- Автопроверку можно отключить, ручная проверка доступна; без скрытого скачивания.
- com.termux/code1061, прежняя подпись, libraryv8, manifestv1/v2; сброс не требуется.
  Старый v1.1.0 получает этот APK через свой updater, после чего действует новая схема.

## v1.1.3 — прогресс бэкапа и выбор сжатия · локальная проверка

- Время выполнения, полоса текущего этапа, процент при известном объёме и одна
  сменяющаяся строка файла. Для этапов без достоверного итога — анимация без процента.
- Все окна и подменю бэкапа игнорируют тап снаружи; явная отмена сохранена.
- gzip/pigz, уровни 1/3/9, по умолчанию gzip3. pigz до4 потоков; установка только
  после отдельного согласия, в фоне приложения, с проверкой доступности результата.
- Полная Ubuntu: штатный несжатый stdout современного proot-distro с выбранным
  компрессором. На старом proot-distro сохранено штатное сжатие с пояснением.
- «(тестирование)» в названиях, нейтральные описания переноса между окружениями,
  завершение «Бэкап создан и проверен» и путь.
- com.termux/code1060, библиотекаv8 и manifestv1/v2 без миграции/сброса. Не опубликован.

## v1.1.2 — «По выбору» и исправления · локальная проверка

- Недоступный каталог больше не обрывает весь список состава: отмечаются пути и
  неполные компоненты; их выбрать нельзя. При копировании ошибки не игнорируются.
- Служебное .l2s исключено из обычного обхода, внутренние ссылки proot копируются
  переносимыми файлами с ограничением хранилища и открытием через дескрипторы.
- «Создать» выключена до согласия в обоих режимах, путь явно подписан.
- Встроенное обновление: индикатор проверки, полоса/процент/объём загрузки APK,
  стадия проверки подписи; отмена и правила проверки APK сохранены.
- com.termux/code1059, библиотекаv8; очистка данных не требуется. Не опубликован.

## v1.1.1 — выборочный бэкап · локальная проверка

- Раздельный выбор системы, Codex/истории/авторизации, проектов, SSH/Git,
  пользовательских настроек, других AI-агентов и известных каталогов сборок.
- Размеры без двойного учёта, добавление папок HOME, проверка зависимостей
  при создании и проверка принимающего окружения при восстановлении.
- Online backup SQLite Codex по каждой базе, исключение IPC/locks/WAL/SHM.
- Частичные архивы отделены от загрузочной Ubuntu; извлечение без перезаписи
  существующих данных. Новый manifest v2 для выбранных данных, чтение v1 сохранено.
- com.termux/code1058, библиотекаv8; очистка данных не требуется. Не опубликован.

## v1.1.0 — обновление из приложения и бета резервного копирования

Основной новый функционал — **бета создания и импорта бэкапов Termux/совместимых
форков**, доступная из шестерёнки справа сверху. Своё резервное копирование,
SHA-256/проверка сжатия/структуры, выбор HOME/PREFIX/Ubuntu/библиотек/истории;
импорт распознаваемых layouts. HOME/PREFIX только в новую папку; Ubuntu отдельным
alias через штатный proot-distro, с isolated пресетом. Не универсальное 1:1.
Выбор команд/заметок/пресетов, объединение/замена, aliases и скрипты → команды,
защита активных сессий, review импортированных пресетов до запуска, recovery JSON.
Отдельный раздел сравнения пакетов Termux; предварительный APT plan и отдельное
подтверждение перед установкой в новом терминале.

Включена вся ранее локальная v1.0.19: ручной updater, проверка APK, настройки
логирования и удаление дублирующего подключения OpenCode. Дополнительно автоматическая
проверка при открытии, интервал10часов от попытки, переключатель и badge без popup.
Публичные assets теперь включают update.json; формат совместим с v1.0.19 клиентом.
Пакет/сертификат прежние,1.1.0/code1057,v8. **Очистка пользовательских библиотек и
истории не нужна.** Keystore/процессы между чистыми установками не переносятся;
это ограничение восстановления бэкапа, а не потеря авторизации при обновлении APK.
Полное описание: [RELEASE-v1.1.0](docs/fork/RELEASE-v1.1.0.md).

## v1.0.19 — историческая локальная сборка (отдельно не публиковалась)

- Ручное обновление из GitHub через общую шестерёнку: локализованные главные
  изменения, прогресс, отмена, проверки размера/SHA-256/пакета/версии/подписи,
  совместимости Android/ARM64 и подтверждение установки средствами Android.
- Формат update.json и генератор метаданных из финального APK; без фоновых
  проверок, удаления приложения или обращения к shell-авторизации GitHub.
- Убран дублирующий пункт OpenCode «Аккаунты и API-ключи» из общих настроек;
  настройка подключений в сессиях и пресетах сохранена.
- Стандартный лог приложения: «Выключено (default)». Однократный перенос прежнего
  NORMAL в OFF при запуске новой версии, последующий ручной выбор сохраняется.
  DEBUG/VERBOSE сохраняются. Собственная диагностика сессий не изменена.
- com.termux, 1.0.19/code1056, прежний сертификат, библиотека v8.

## v1.0.18 — AI Agents, пресеты и диагностика

Публичный выпуск объединяет изменения локальных v1.0.5–17 после v1.0.4.
Новый выбор Codex/OpenCode/Claude Code/Gemini CLI, подготовка компонентов в Ubuntu,
история/модели/режимы, исправления клавиатуры и переименование пресетов.
AI Agents экспериментальны; известное мерцание OpenCode не объявляется исправленным.
Личный OAuth Gemini отключён Google; реальный API-ответ пока не подтверждён.
Полное описание: [RELEASE-v1.0.18](docs/fork/RELEASE-v1.0.18.md).

- Интерактивный ввод и дополнительные клавиши перенесены из шестерёнки в верхний
  ряд перед общей шестерёнкой, только в терминале: центрированные векторы48dp, подсветка
  включённого режима как у выбранного фильтра. В остальных вкладках скрыты.

- Единая шестерёнка на всех вкладках: язык и настройки приложения; отдельный
  глобус удалён. «Диагностика сессии» в долгом тапе названия терминала. Отмена будущих шагов запуска перенесена
  в правую часть плашки текущей сессии с подтверждением Да/Нет и списком команд,
  только для поддерживаемых custom steps
  Termux/Ubuntu; в выпадающем списке её нет. Диагностика с автообновлением окна,
  копированием пути к актуальному приватному отчёту, экспортом в Download.
- «Расширенный дебаг-журнал» в создании/редактировании сессий и пресетов, сохраняется
  в библиотеке v8 и при копировании. Старым профилям по умолчанию выключен.
- Базовые запуск/выход/ошибка подготовки и наблюдаемые категории ошибок собираются
  независимо от окна; расширенный добавляет временную шкалу активности вывода.
  Для Gemini дополнительно штатный --debug только с запуска выбранной сессии.
- Остановка/продолжение коллектора без управления терминальным процессом. Лимит
  2 МиБ, удаляются старейшие записи; до 64 известных профилей в памяти процесса.
- Не записываются ввод, transcript, команды пользователя, переменные окружения,
  URL, аккаунты и секреты. Коды из вывода — наблюдение, не доказательство ошибки
  конкретного запроса. Другим агентам не навязываются чужие debug-флаги.
- Реальный OAuth/ответ Gemini и UI диагностики требуют проверки на устройстве.

## v1.0.17 — локальное исправление пути Gemini auth (не опубликован)

- Сессионный файл native auth policy перенесён из каталога сокетов под `/tmp`
  в приватный runtime-каталог Gemini (0700, файл 0600). Удаляется при закрытии.
- Native CLI отклонял policy v1.0.16 из-за доступного другим пользователям `/tmp`:
  это подтверждено скриншотом и изолированным воспроизведением.
- Контрольный сокет, команды запуска, история и другие агенты не изменены.
  Права `/tmp`, настройки и токены пользователя не меняются.
- Проверка реального Google-входа и исходной причины переключения v1.0.15 остаётся
  отдельной задачей; исправление снимает доказанный блокер v1.0.16.

## v1.0.16 — локальная проверка авторизации Gemini (не опубликован)

- В настройках Gemini-сессии явный способ входа: Google / API-ключ / выбор в терминале.
- Фиксированный режим использует native security.auth.selectedType/enforcedType
  через отдельный приватный settings-файл запуска, сохраняется при restart199.
- В Google-режиме API не появляется как альтернативный способ при ошибке входа.
  Токены/глобальные настройки не переписываются контроллером, ввод не фильтруется.
- Существующие профили сохраняют штатный режим; новые по умолчанию предлагают Google.
- Внешние системные настройки/политики не подменяются: при их наличии фиксированный
  режим отказывается запускаться и просит выбрать штатный режим.
- Первоначальная причина быстрого выбора API после авторизации на телефоне пока
  не доказана. Это проверяемое ограничение метода входа, не заявление о найденном
  дефекте клавиатуры или исправленной авторизации аккаунта.


## v1.0.15 — локальная проверка Gemini CLI (не опубликован)

- Gemini CLI 0.64.0 в AI Agents, Ubuntu: отдельный официальный runtime и частный
  Node22 устанавливаются при первом запуске; системные/чужие компоненты не меняются.
- Штатный вход Google для доступного бесплатного уровня, Auto/Flash/Pro, режимы
  default/auto_edit/plan, новый чат / выбор истории / UUID чата. История по проекту.
- Выбор чата открывает `/resume` после входа, не молча последний диалог.
  Обработан штатный перезапуск CLI199 после доверия папке и изменения авторизации.
- Тексты разрешений у всех четырёх агентов на RU/EN: в выборе и под режимом.
- OpenCode: автоматическое продолжение истории той же сессии восстановлено;
  отдельный пункт «Сохранить чат сессии» и его обработчик удалены. Существующие
  ручные привязки совместимы, новые независимые чаты выбираются явно.
- Native assets/терминальные screen/resize-механизмы остаются как в v1.0.14.
  Сам CLI Gemini не включён в APK. Вход/диалог пользователя ещё не проверены.


## v1.0.14 — локальная проверка Claude Code (не опубликован)

- Claude Code в AI Agents, только Ubuntu: отдельный официальный ARM64 runtime
  2.1.296 скачивается и проверяется при первом запуске; бинарник не включён в APK.
- Модель по умолчанию/Sonnet/Opus/Haiku; ручные подтверждения, автоматические
  правки или планирование. Обход разрешений и обход ограничения root не добавлены.
- Новый чат, штатный выбор истории в терминале или UUID чата. История сохраняется
  Claude, параметры — в пресете приложения. Последний чат отдельным пунктом не добавлен.
- Отдельный контроллер закрывает принадлежащий ему процесс Claude;
  уникальный ID запуска защищает от остановки более новой сессии.
- RU/EN, справочная плашка с параметрами запуска. Живые модель/квоты не заявлены.
- Codex и OpenCode сохраняют прежние адаптеры и поведение v1.0.13.


Этот журнал описывает основной форк. История оригинального Termux сохранена
в Git; отдельная Desktop Alpha сюда не включена.

## v1.0.13 — возврат OpenCode к v1.0.9 и ручное сохранение чата

- Адаптер смены модели возвращён побайтово к v1.0.9: переоткрытие того же native
  чата, уровни доступа и история при явном продолжении сохранены.
- Убраны экспериментальные DEC2026/resize-буферы, deferred attach и отдельный
  frontend jobs1. Общий terminal code соответствует состоянию до этих экспериментов.
  Нынешние кнопки, надписи, иконки, пресеты и их расположение оставлены.
- Долгий тап OpenCode → «Сохранить чат сессии»: сохраняется конкретный native ID
  и пресет для последующего продолжения. «Добавить в пресеты запуска» сохраняет
  только настройки, без автоматического запоминания текущего чата.
- Без ручной привязки повторный запуск новый; явный выбор существующего чата
  применяется один раз. Старые автоматически принятые чаты не привязываются сами;
  их данные не удаляются и остаются доступны в OpenCode. Codex не меняется.
- Это локальная проверка; возврат к baseline не обещает устранения мерцания.

## v1.0.12 — отдельное восстановление истории OpenCode, не опубликована

- Отдельный frontend OpenCode v2.0.28/jobs1: история при входе и переоткрытии
  модели сохранена, повторный reset/replay при resize отключён только при запуске
  через наше приложение. Изменение геометрии и обновление темы продолжают работать.
- Frontend включён в APK и подготавливается из локального проверенного SHA-256
  архива; официальный сервер, системный OpenCode, подключения и Codex не заменяются.
  Добавляет около71MB к APK и178MB для отдельного runtime в Ubuntu.
- Проверено на искусственном native чате: история после входа/смены модели есть,
  шесть resize не повторяют историю и не очищают scrollback. Android/IME/жесты
  на телефоне требуют пользовательской проверки; выделение отдельно не менялось.

## v1.0.11 — проверка размеров окна OpenCode, не опубликована

- При переключении на OpenCode размер PTY задаётся перед отрисовкой, после
  размещения карточки и поля ввода новой вкладки. Размер предыдущей вкладки
  больше не передаётся этой веткой; скрытый View ждёт появления, устаревшее
  ожидание отменяется при переключении на другую сессию.
- Отключено накопление всего вывода после resize для OpenCode: служебные запросы
  эмулятора и ANSI не ждут дополнительные 350–1200 мс. DEC2026 кадры сохранены.
- Native replay истории, переоткрытие при смене модели и автопрокрутка не менялись.
  Это проверочная сборка: устранение OC-001 на телефоне ещё не подтверждено.

## v1.0.10 — локальная проверка OpenCode и навигации, не опубликована

- Для вкладок OpenCode вывод DEC 2026 поступает в эмулятор целым кадром: неполный
  кадр освобождается через 1 секунду, объём буфера ограничен. При реальном изменении
  окна вывод объединяется до окончания серии обновлений, максимум 1,2 секунды.
  Одинаковый размер PTY не устанавливается повторно при возвращении во вкладку.
  Обычные терминалы и Codex сохраняют непосредственный путь вывода.
- История и переоткрытие при смене модели остаются штатными: adapter/CLI не изменены.
  Возврат просмотра к последнему сообщению сохранён по просьбе пользователя.
- Кнопка пакетов выбранной сессии перенесена из долгого тапа на место + рядом с
  селектором. В списке сессий первый пункт — «Создать новую сессию», ниже открытые.
- Главная: окно + называется «Создать пресет»; терминал: «Создать сессию».
  Русский и английский тексты обновлены; логика сохранения и формат v8 прежние.
- Проверка пользователем на телефоне: мерцание осталось. Важная нерешённая проблема OC-001; дальнейшая работа отложена по просьбе пользователя.

## v1.0.9 — локальная проверка, не опубликована

- Смена модели OpenCode теперь требует подтверждения и переоткрывает только mini
  в той же вкладке, сохраняя сервер, ID чата и историю. При foreground-работе
  или очереди смена отклоняется. Неотправленный текст внутри mini может потеряться;
  черновик поля приложения не изменяется.
- «Сохранённые сессии» переименованы в «Пресеты запуска» / Launch presets;
  действие меню — «Добавить в пресеты запуска». Данные и порядок сохранения прежние.
- Мерцание терминального вывода пока не исправлено. Codex не затронут.

## v1.0.8 — локальная проверка OpenCode, не опубликована

- Строка модели в верхнем меню OpenCode обновляется после серверного readback,
  подтверждённые модель/поведение одновременно сохраняются в профиле.
  Поздний ответ исходного статуса не перезаписывает новый выбор.
- Отдельный доступ OpenCode: полный без подтверждений, с подтверждениями,
  только чтение без shell/правок/субагентов/MCP/неизвестных инструментов.
  Применяется при старте/возобновлении, старые профили получают ask.
- Описания Build/Plan и доступа RU/EN; Plan не является самостоятельной
  защитой от изменений. Для такой задачи нужен уровень только чтение.
- «Основное подключение» и «Аккаунты и API-ключи» вместо «Личное/AI-провайдеры».
  В списках подключений убрана нижняя кнопка возврата, системный Back сохранён.
- Codex не изменён; 265 локальных тестов, native API и APK checks прошли.

## v1.0.7 — локальная проверка клавиатуры, не опубликована

- Главное окно Workspace снимает FLAG_ALT_FOCUSABLE_IM, включая возвращение
  приложения из фона. Старое глобальное отключение клавиатуры Termux не блокирует
  обычные поля Workspace; значения старых настроек не перезаписываются.
- Явное открытие IME при нажатии поля; кнопка клавиатуры вызывает её после
  закрытия контекстного меню. Интерактивный ввод управляет терминальным тапом отдельно.
- Прежние адаптеры Codex/OpenCode, bootstrap, данные и диалоги сохранены.
- 263 локальных теста; появление реальной Android-клавиатуры проверяет пользователь.

## v1.0.6 — локальная проверка, не опубликована

- Создание чатов штатным API OpenCode: native ID вместо вручную заданного.
  Сравнение на Ling 3.1 Flash Free: прежний адаптер — 403, исправленный — ответ.
- Проверка и установка компонентов по «Создать и запустить» в той же вкладке,
  запуск агента после готовности; имеющиеся компоненты не скачиваются заново.
- При необходимости отдельный Node.js 22.22.1 и OpenCode v2.0.28, закреплённые
  SHA-256, временная папка и блокировка установки. Системный Node/Codex не заменяются.
- Сохранение actual native ID при сохранении профиля; ссылки профиля поддерживают resume.
- Старые чаты сохранены; для проверки free после v1.0.5 нужен новый чат.

## v1.0.5 — локальная проверка, не опубликована

- AI Agents вместо отдельного пункта Codex: прежний запуск Codex и OpenCode в Ubuntu.
- Именованные подключения, ключи провайдеров, каталог/поиск моделей и варианты поведения с подтверждением.
- Новый чат, выбор истории и ID OpenCode; отдельные авторизация/базы, управление моделью в активной вкладке.
- Termux и Ubuntu без приписок; «Как раньше» убрано из нового выбора, старые профили читаются.
- Резервный APK прежней v1.0.4 с кодом 1100 для отката поверх 1042.
- RU/EN; отдельные Claude Code/Gemini CLI, OAuth OpenCode и точная permission-card из макета пока не реализованы.

## v1.0.4 — релиз

Публичный предшественник — v1.0. Все изменения локальных v1.0.1–v1.0.3 включены.

- Менеджер пакетов Termux/Ubuntu: установленные, доступные, обновления, поиск,
  версии с предупреждением downgrade и описания с полным просмотром.
- Подтверждение запуска операции в выбранной свободной оболочке либо новой сессии
  того же окружения; пакеты остаются после закрытия сессий.
- Действия команд/заметок в просмотре вместо троеточий; ровные кнопки пакетов и
  плашка шестерёнки рядом с глобусом. Новые меню и сообщения RU/EN.

- Редактировать после Просмотреть в меню распознанного текстового файла, включая .md. Моноширинный многострочный ввод, Сохранить/Отмена, подтверждение потери изменений.
- UTF-8 до 256 КБ, проверка внешних изменений и сохранение прав/владельца через временные файлы в той же папке и атомарную замену. Ошибка сохраняет введённый текст в окне; ссылки и hardlinks не редактируются.

- Отдельная кнопка выбора категории справа от поля в создании/редактировании команды и заметки. Открывает все категории своего списка, независимо от текста и фильтра автоподсказок.
- Выбор заполняет редактируемое поле; можно ввести новую категорию. Для пустого списка показывается пояснение.
- RU/EN, прежний пакет/сертификат/схема v8, versionCode 1041.

## v1.0.3 — историческая локальная сборка, включена в v1.0.4

- Карточка пакета показывает описание из APT/dpkg: до трёх строк и Подробнее с полным текстом во внутреннем прокручиваемом окне. Чтение только при открытии пакета, ошибки не блокируют действия.
- Действия команд/заметок перенесены из троеточия в окно просмотра: избранное, редактирование, удаление с подтверждением. Отдельные троеточия убраны; вставка команды сохранена.
- Категории в редакторах предлагают существующие категории соответствующей библиотеки; можно свободно вводить новые.
- RU/EN для новых меню, описание из репозитория без автоперевода. VersionCode 1040, формат v8 не меняется.

## v1.0.2 — историческая локальная сборка, включена в v1.0.4

- Карточка пакета → Выбрать версию: версии настроенных репозиториев выбранного Termux/Ubuntu, установка конкретной версии после подтверждения.
- Сравнение установленной и выбранной версии через dpkg; предупреждение о понижении. Версии не закрепляются автоматически и сторонние репозитории не добавляются.
- Кнопки списков и обновления каталога одинаковой высоты, однострочные адаптивные подписи вместо случайного переноса/смещения.
- Шестерёнка получила плашку как у глобуса; между кнопками отдельный отступ 8dp.
- RU/EN, package/certificate и формат v8 сохранены, versionCode 1039.

## v1.0.1 — историческая локальная сборка, включена в v1.0.4

- «Пакеты» в меню долгого тапа по сессии, включая базовую Terminal.
- Установленные, Доступные и Обновления с поиском и версиями: реальные базы dpkg/APT выбранного окружения Termux/Ubuntu, включая профили Codex.
- Установка/обновление/удаление пакета и обновление каталога после подтверждения предлагают выбранную или новую вкладку того же окружения. Предупреждение требует свободный Bash; пользователь определяет это сам. Закрытая/перезапущенная или изменившая окружение выбранная сессия не получает ввод.
- Пояснение: пакеты сохраняются в окружении независимо от сохранения/закрытия сессии.
- RU/EN; библиотека остаётся v8, package ID и ключ подписи прежние, versionCode 1038.
- SSH и редактор файлов в этот этап не включены. Проверка интерфейса на устройстве ожидается.

## v1.0 — релиз

Публичный предшественник — v0.2.17. Изменения v0.2.18–v0.2.20 включены в эту сборку.

- Русский и английский интерфейс: главное меню, команды/заметки, сессии, SSH, файлы, карточка Codex, выбор модели/рассуждения, настройки и сообщения приложения.
- Векторный глобус рядом с шестерёнкой на всех вкладках: Системный / Русский / English. Выбор сохраняется, применяется без перезапуска терминалов. Для прочих системных языков используется английский.
- Верхние плитки показывают только центрированные векторные иконки, с прежними размерами и расположением. Долгий тап не показывает подсказки.
- Проектные каталоги, файлы и базовая незакрываемая сессия Terminal; стопка сессий вместо горизонтального списка.
- Все чаты Codex в выбранной среде, фильтр папки и подтверждённое продолжение чата в другом каталоге с сохранением истории и выбранного доступа.
- Исправлены векторные значки, отступы сохранённых сессий и взаимоисключающий выбор фильтров команд/заметок.
- Названия и содержимое пользовательских команд, заметок, сессий, файлов, вывод терминала и имена моделей не переводятся. Журнал по-прежнему сохраняет до 50 отправок, включая повторы.
- versionName 1.0.0, versionCode 1037, пакет com.termux и прежний сертификат; формат библиотеки v8 сохранён. Пользователь одобрил APK для публикации на GitHub.

## v0.2.20 — локальное исправление

- Фильтры команд и заметок взаимоисключающие: категория снимает «Избранное», «Все» и «Избранное» сбрасывают категорию. Подсвечивается только действующий фильтр.
- Исправлена кнопка выбора рабочего каталога в новой сессии: вектор папки назначается при создании кнопки, без подмены текстовым символом.
- Между запуском и меню сохранённой сессии добавлен отступ 8 dp, обе кнопки 48 × 48 dp.
- Пакет com.termux, versionCode 1036, прежний сертификат и формат данных. На GitHub не опубликовано.

## v0.2.19 — локальная сборка

- История Codex по умолчанию показывает все чаты выбранной среды, с исходным каталогом; есть поиск и фильтр Чаты этой папки.
- Выбор списка/ID и изменение каталога сохранённого чата требуют подтверждения, если папка отличается. Сохраняются ID и выбранные права; файлы не переносятся. Preflight проверяет конкретный подтверждённый исходный каталог и эффективный cwd ответа сервера.
- Иконки звезды, папки, +, вверх и дома — векторные и центрированные независимо от шрифта. В верхней строке терминала оставлен только значок папки без текста.
- Небольшие отступы над карточкой модели/лимитов, под заголовком стопки и между сессиями.
- Фильтры файлов выделены фоном, рамкой и жирным текстом; доступное состояние выбора действует и в общем браузере, и при создании сессии.
- com.termux, versionCode1035, прежний сертификат, библиотекаv8 с необязательным полем подтверждённого исходного каталога. На GitHub не опубликовано. Перевод и обновление с GitHub остаются темами следующего обсуждения.

## v0.2.18 — локальная сборка

- Выбор существующей папки или создание новой рядом с рабочим каталогом, для обычных и Codex-сессий; операции выполняются в выбранной среде.
- Поиск/выбор чатов только в каталоге проекта и среде; ввод ID проверяет каталог. Новый чат связывается с профилем по метаданным своего app-server, сохранение профиля остаётся явным.
- Сохранённый Codex-профиль предлагает продолжение конкретного чата или новый чат в той же папке; история остаётся в .codex. Серверный preflight отклоняет возобновление чата из другой папки до изменения прав/каталога.
- Вертикальная раскрываемая стопка терминалов, + слева рядом, «Файлы» справа. Карточка Codex выровнена слева, отображает модель/поведение и лимиты.
- Браузер Termux/Ubuntu/SSH: папки, фильтры Все/Текст/Изображения/APK, предпросмотр, копирование пути, передача файла и Download без намеренной перезаписи существующих файлов.
- Базовый Terminal: обычный Termux Bash, скрыт из сохранённых профилей, не закрывается через меню. После выхода оболочка запускается вновь, пока сервис работает; защита останавливает цикл после нескольких быстрых отказов.
- package com.termux, versionCode 1034, прежняя подпись и формат библиотеки v8. На GitHub не опубликовано.

## v0.2.17 — 13 октября 2026

Публичный выпуск включает изменения v0.2.7–v0.2.17 после v0.2.6: SSH-сценарии и авторизация, исправления прокрутки, интеграция Codex с выбором истории/доступа, карточкой статуса и лимитов, выбором модели/поведения и подтверждением. Отдельные черновики терминальных вкладок сохраняют текст и выделение при переключении; после отправки поле очищается, после перезапуска приложения черновики не сохраняются.

Пакет com.termux, versionCode 1033, прежняя подпись, библиотека v8 с чтением v1–v8. 203 теста прошли; пользователь подтвердил работу на своём устройстве. [Описание релиза](docs/fork/RELEASE-v0.2.17.md) · [Проверки](docs/fork/VALIDATION.md).

Записи v0.2.7–v0.2.16 ниже — исторические этапы разработки, включённые в этот выпуск; их прежние ограничения и статус «локальная» относятся к тому этапу.

## v0.2.16 — локальная сборка

- Нижнее поле ввода очищается при смене терминальной сессии; обновления и повторное открытие той же вкладки текст не стирают.
- Поиск команд и заметок в звёздочке открывает клавиатуру при нажатии/фокусе. Снят мешающий вводу FLAG_ALT_FOCUSABLE_IM у диалога с поиском в customTitle.
- Нажатие на модель сразу открывает поведение; нажатие на поведение открывает отдельное окно с выбранными параметрами. Применение начинается только после подтверждения «Применить».
- Механизм применения модели v0.2.15 и Node-помощник сохранены; существующий каталог подходит для нового интерфейса.
- package com.termux, versionCode1032, прежняя подпись и формат библиотеки8.

## v0.2.15 — локальная пробная сборка

- В карточке Codex добавлены собственные списки моделей и доступных уровней рассуждения; каталог получен через model/list, названия и варианты не заданы фиксированным списком.
- После выбора приложение ждёт idle, открывает штатное меню /model, проверяет выделение перед каждой отправкой клавиши и применяет выбор через s session. Поле ввода приложения не заменяется.
- Успех показывается только после нового сообщения Model changed to. Незнакомое меню, дополнительное подтверждение, занятый ввод и таймаут останавливают автоматику; доступен ручной выбор в терминале.
- Поддержаны обычные уровни и второй шаг More reasoning для Max/Ultra. Подтверждённый выбор хранится для текущего запуска/чата; формат библиотеки8 сохранён.
- Для каталога нужен новый запуск терминала после обновления. На телефоне применение ещё не проверено. package com.termux, versionCode1031, прежняя подпись.

## v0.2.14 — локальное исправление запуска

- У клиента --remote убран несовместимый --no-daemon. Этот флаг остаётся только у запуска локального app-server и чтения списка истории.
- Отказ thread/resume теперь содержит числовой код RPC и безопасную категорию причины; сырой серверный текст, история и авторизация не выводятся.
- Ошибка возобновления большого реального чата на телефоне ещё не диагностирована полностью; запуск нового клиента исправлен и проверен в PTY с отдельным локальным сервером.
- package com.termux, versionCode 1030, прежняя подпись и формат библиотеки8.

## v0.2.13 — локальная сборка

- Карточка больше не входит в Ubuntu отдельным процессом. Помощник читает статус внутри того же терминала/окружения, где запущен Codex, и атомарно передаёт только метаданные через приватный файл.
- Убран контроль активности по File.exists сокета со стороны Android: ссылка Codex может указывать на путь, видимый только внутри Ubuntu.
- Обновление раз в 30 секунд; кнопка Обновить запрашивает внеочередное чтение. Устаревшие данные отвергаются.
- Помощник принадлежит одному запуску сессии и останавливается его скриптом; текст диалога, токены и авторизация в файле статуса не сохраняются.
- Запуск Codex использует --no-daemon. Этот флаг сам по себе не устраняет внутренние ссылки сокетов; исправление опирается на чтение статуса в исходной среде.
- Для проверки нужен новый терминал Codex после обновления. Уже работающие сессии не изменяются автоматически. package com.termux, versionCode 1029, прежний сертификат и формат библиотеки 8.

## v0.2.12 — локальная диагностическая сборка

- Общая ошибка карточки Codex заменена безопасной диагностикой: этап RPC, тип сбоя и код завершения помощника, если доступен.
- Проверяются Node.js, файл адаптера и Unix-сокет именно внутри выбранной среды. Содержимое stderr, ключей и авторизации не показывается и не сохраняется.
- Запуск помощника из Android без входа в Ubuntu проверен отдельно. Причина отказа карточки на телефоне пока не подтверждена; сборка не объявляется исправлением подключения.
- История, права запуска чатов и установленная среда не меняются. package com.termux, versionCode 1028, прежний сертификат.

## v0.2.11 — локальное исправление, не опубликовано

- Исправлен запуск сохранённого Codex-чата: права применяются и проверяются через thread/resume на сервере до подключения TUI. Запрещённые флаги --sandbox/--ask-for-approval у --remote resume убраны.
- Новый локальный адаптер на Node.js подключает карточку из той же среды Ubuntu/Termux по приватному Unix-WebSocket и передаёт JSONL Android-интерфейсу. Без сетевого порта и сторонних зависимостей.
- В выборе чата оставлены «Новый чат», «Выбрать чат», «Ввести ID чата». Старый профиль «Последний» требует выбора конкретного чата; история не удаляется.
- Остановка своего app-server запрашивается через приватный маркер; PID знает и сигнализирует только создавший его скрипт.
- package com.termux, versionCode 1027, прежний сертификат; формат библиотеки 8 сохранён.

## v0.2.10 — локальная сборка, ещё не опубликована

- Тип «Сессия Codex»: Termux/Ubuntu, полный доступ, в пределах проекта или только чтение.
- Новый чат, последний чат в рабочем каталоге, выбор чата из истории выбранной среды либо по ID; доступен штатный выбор в терминале.
- Компактная карточка с серверным статусом модели/рассуждений и пятичасовым/недельным лимитами; загрузка в фоне раз в 30 секунд.
- Кнопки модели и рассуждения подготавливают /model для штатного меню Codex. Внешняя смена параметров живого TUI не имитируется.
- Локальный app-server по приватному Unix-сокету отдельно для каждого терминала. Нет сетевого слушателя или автоматической установки Codex.
- Формат библиотеки v8, чтение v1–v7. Обновление поверх прежней версии: com.termux, versionCode 1026, прежний сертификат.

## v0.2.9 — локальная сборка, ещё не опубликована

- Исправлен перехват свайпов общей областью над карточками вложенных списков при открытой клавиатуре/поиске.
- Журнал, сохранённые сессии, команды и заметки удерживают жест внутри своей панели; прокрутка на краю списка не передаётся общей области.
- Размеры, выравнивание и логика поиска сохранены.

## v0.2.8 — локальная сборка, ещё не опубликована

- Выбор авторизации SSH: автоматическая, файл ключа или пароль.
- Поле пароля с показать/скрыть и запоминанием через Android Keystore по желанию.
- Одноразовая передача пароля через локальный канал без записи открытого пароля в профиль, скрипты или историю.
- Отдельное подтверждение нового ключа сервера при входе по паролю.

## v0.2.7 — локальная сборка, ещё не опубликована

- Среда «SSH · сервер» в создании и настройках сессии: адрес, пользователь, порт и удалённый каталог.
- До трёх команд запуска выполняются на сервере последовательно; ошибка останавливает дальнейшие шаги.
- После команд можно оставить интерактивный Bash на сервере или завершить подключение.
- Параметры SSH сохраняются в профиле. Пароли и ключи обрабатывает установленный SSH-клиент.

## v0.2.6 — 13 октября 2026

Публичный выпуск включает все изменения после v0.2.1:

- Команды и заметки в двух панелях, общий поиск, независимые категории и избранное, + справа от фильтров.
- Две вкладки команд/заметок в звёздочке с отдельным поиском, избранными первыми и вставкой в текущий терминал без отправки.
- Единый тёмный стиль окон и меню, полный просмотр длинного текста.
- Журнал увеличен с 25 до 50 записей, повторы сохраняются отдельно; троеточия убраны, действия доступны нажатием на карточку.
- Адаптация панелей к клавиатуре, малому окну и системному шрифту, перенос фильтров, гибкая высота верхних плиток, скрытие клавиатуры при смене вкладки.
- Выбор сохранённых профилей из окна новой сессии; при закрытии обычной вкладки выбор сохранения, очистка несохранённого профиля/истории после остановки.
- Формат библиотеки v5 с чтением v1–v4; сохранены пакет и сертификат обновления. Android versionCode 1022.

Проверки: 176 тестов и lintVitalRelease. Адаптацию пользователь подтвердил на Samsung.
[Описание выпуска](docs/fork/RELEASE-v0.2.6.md) · [Проверки](docs/fork/VALIDATION.md).

Промежуточные записи ниже фиксируют локальные этапы разработки, которые теперь включены в v0.2.6.

## v0.2.5 — локальная сборка, не опубликована

- Главная и библиотека адаптируются к доступной высоте: при открытой клавиатуре панели сохраняют минимальную высоту, появляется общая прокрутка.
- Фильтры команд и заметок распределяются по ширине, подписи допускают две строки; кнопки + остаются справа.
- Верхние плитки и поиск учитывают увеличенный системный шрифт.
- При смене вкладки клавиатура скрывается.

## v0.2.4 — локальная сборка, не опубликована

- Журнал до 50 отправленных команд; повторы остаются отдельными записями.
- Тёмные скруглённые окна создания/настроек сессий и редакторов библиотеки.
- Меню действий с карточками, иконками и бирюзовым выделением при нажатии.
- Поиск по названию, тексту и категории в меню звёздочки; избранные первыми,
  клавиатура не открывается автоматически. Выбор только вставляет текст.
- Полный просмотр команды, заметки и недавней команды с внутренней прокруткой.
  Для команд отдельная кнопка вставки; для недавней записи также сохранение и копирование.

## v0.2.3 — локальная сборка, не опубликована

- Вкладка «Команды/Заметки»: две панели с независимой прокруткой.
- Общий поиск по названиям, содержимому и категориям команд и заметок.
- Кнопки добавления справа от строк фильтров; для заметок свои категории и избранное.
- Заметки с названием и многострочным текстом, просмотром, редактированием и удалением.
- Формат библиотеки v5, чтение прежних v1–v4; команды и сессии сохраняются.

## v0.2.2 — локальная сборка, не опубликована

- Кнопка «Сохранённые» в окне новой сессии открывает выбор профиля для запуска.
- При закрытии обычной сессии можно сохранить настройки и историю Bash
  или закрыть без сохранения; отдельной кнопки отмены нет.
- Несохранённые профили и история удаляются после остановки процесса;
  потерянные несохранённые вкладки очищаются при подключении сервиса.
- История Codex и данные сохранённых сессий не удаляются при закрытии вкладки.

## v0.2.1 — 12 октября 2026

### Возможности выпуска

- Новый интерфейс: верхние плитки «Главная / Команды / Терминал»,
  вкладки терминалов и отдельно прокручиваемые панели на главной.
- Библиотека команд с названиями, категориями, поиском и избранным;
  вставка в выбранный терминал и звёздочка для текущего.
- Сохранённые профили с каталогом, выбором среды и до трёх команд;
  ожидание Bash в Termux/Ubuntu, остановка последовательности при ошибке.
- До восьми активных терминалов, подтверждение закрытия и перезапуска.
- Журнал последних 25 отправленных команд с поиском и повторным использованием.
- Настройка прямого интерактивного ввода.

### Изменения относительно предыдущего публичного выпуска

- Навигация перенесена наверх, выровнены заголовки и упрощены меню.
- Обычная сессия больше не сохраняется автоматически: сохранение через
  долгий тап по вкладке или «+» на главной.
- Меню команд сокращено до избранного, редактирования и удаления.
- Разделены меню сохранённого профиля и активной вкладки.
- Закрытие явно останавливает процесс и закрывает вкладку, сохраняя профиль.
- Удалены снимки вывода, их история и автоматические снимки.
- Обновление title больше не пересоздаёт страницы, что устраняет обнаруженный
  источник потери нажатий и прокрутки при активном выводе.
- Отдельные настройки шрифта/интервалов отменены; возвращено прежнее отображение.
- Название выпуска без beta, версия `0.2.1`, versionCode `1017`.
  Сохранены `com.termux` и сертификат предыдущих сборок форка.

Проверки: 166 автоматических тестов, lintVitalRelease, целостность и подпись APK.
Подробности — [исторический отчёт](docs/fork/history/VALIDATION-v0.2.1.md),
[описание выпуска](docs/fork/RELEASE-v0.2.1.md).

## Ранние сборки

До v0.2.1 функции вводились и проверялись в промежуточных сборках.
Их локальные номера `0.7`–`0.21` не являются последовательностью публичных
версий SemVer. Исторические сведения доступны в
[архиве отчётов](docs/fork/history/VALIDATION-PRE-v0.2.1.md) и Git history.
