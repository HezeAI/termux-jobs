> Historical private checkpoint. Current stable evidence: [v1.6.5](VALIDATION-v1.6.5.md).

# Private v1.6.4-test — shared agent action cards and composer focus

com.termux/code1196, Android8+/ARM64, same signer. No data-schema migration,
history reset or permission change. Not a GitHub release; public latest1.6.0.
Preserves private1.6.2/1.6.3 Codex one-turn history paging, chat-only32MiB limit,
idempotent failure and existing skill/timeline work.

## Implementation and boundaries

OpenCode/Gemini/Claude use the existing compact, stable expandable action card.
They project native tool name/status metadata. Shell preview comes from the typed
command when available, details retain native message/part coordinates. Native
success/error/executing normalize for presentation; pending/awaiting approval and
unknown statuses do not become successful completion. Claude matches tool_use to
its tool_result by native ID; unresolved use remains unknown. Results remain
inspectable native rows. Existing timers, network calls, prompts, model context,
command dispatch, permissions, data stores and process ownership are unchanged.

Consecutive tool rows group without crossing user/text/reasoning/receipt/error
boundaries. Cards retain expansion and child identity as events update. Unknown
tools keep their original captions. RU/EN labels added for recognized file/search/
web/shell tools. Original raw details remain available on tap.

Successful structured Read/read/read_file of an absolute */SKILL.md can identify
its parent skill folder for Claude/OpenCode. Read path and result must be available;
failed/relative/traversal paths are not attributed. Context resets at new user/turn
or explicit instruction read. This is chronological context, not proof every
following tool was caused by or successfully applied that skill. Gemini currently
exposes native tool names/status/description without verified file arguments, so
no inferred skill read is added there. Codex classifier remains unchanged.

Urgent developer report: chat updates reset input focus, while typed text stays.
A regression test reproduced this before the fix: fullScroll(FOCUS_DOWN) moved
focus to selectable transcript content. AgentChatView now scrolls the viewport
with scrollTo(bottom), without selecting a focus target. Shared path covers all
four agent chats. The test checks focus, draft, selection and IME composing span
through streaming text/action updates. No periodic forced focus or keyboard reset.

Native terminal stale startup component-check text and mouse escape echo on
scrolling remain deferred at the developer's request. These are not fixed here.

## Verification

143 focused JVM tests passed, zero failures/errors/skips; final13 card/label tests
passed after the last caption change. Includes stream identity/expansion/original
detail keys, RU/EN/statuses, skill boundaries/failed read/nonmutation, transcript
barriers, prior chat/navigation/keys/status/lifecycle and composer-focus regression.
The before-fix focus failure is preserved separately in verification evidence.

Nine Node fixture suites passed: Gemini projection/hooks/private socket, OpenCode
projection, Claude projection/real pinned CLI2.1.296 against loopback synthetic
provider/lifecycle, Codex adapter and large-history wire regression. Updated
projection fixtures assert typed metadata, original detail and Claude result IDs.
No real provider account/model API or existing user process used by these checks.

Release assembly/runtime verification passed. Existing Kotlin/UAST/JNA toolchain
diagnostics remain; no claim of clean full Lint. ZIP/package/version/ABI/minSdk,
non-debug state, signature/SHA, reviewed runtime classes, private file/secret-marker
checks passed. Relative to1.6.3, only Claude/Gemini/OpenCode chat asset projections
change; native libraries and all other assets, including Codex controllers,
bootstrap, terminal/backup/server helpers and dispatch controls, are identical.
No new dependency or native binary is added.

## Artifact and device checks

`Termux-Jobs-v1.6.4-test-arm64.apk`, 36669414 bytes. SHA-256: `1e0dd7d0c8a222a397866defaed8ed1f6f9af65435fd7ab271b416fce05fc086`.
Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
APK and checksum delivered to Download, destination hash verified.

On-device verification still required: type an unsent message while each agent
streams an answer/tool card; ensure focus/keyboard and cursor stay put; expand a
card and inspect a native step, scroll history and switch agents. Mocked UI and
loopback fixtures do not establish phone keyboard/gesture/DPI or all real-provider
behavior. No installed application, existing session, Ubuntu/Termux environment,
shared SDK, system package, account or remote server was changed in this task.
Evidence: `.verification/agent-cards-v1.6.4/`.
