> Historical private checkpoint. Current stable evidence: [v1.6.5](VALIDATION-v1.6.5.md).

# Private v1.6.3-test — compact Codex action/skill clouds

com.termux/code1195, Android8+/ARM64, same signer, no reset/schema migration.
Private APK for developer checking, not a GitHub release. Latest stable1.6.0.
Preserves the other Codex's1.6.2 startup fix: one-turn history pages, chat-only
32MiB wire bound, idempotent disconnect and no false ready after failure.

## Behavior and limits

Completed successful shell groups hide the redundant summary line and use a
shorter header/margins. Header stays Timeline; error text specifically names a
failed command/action instead of implying an Android application error. Declined
and interrupted actions have their own labels. Native nonzero exit/status remains
the evidence; a nonzero search result need not mean the app is broken.

CodexActionLabels supplies RU/EN captions for native imageView/view_image,
image generation and web search. Unknown tools keep their original names; raw
native item/command/output/detail identities remain available on tap. Only Codex
uses these names/grouping; other agent transports/presentation remain unchanged.

An explicit native skill input creates an attached-skill row for any supplied
name. A conservative literal single-file cat of absolute */SKILL.md, optionally
wrapped by bash/sh -c/-lc, identifies reading that skill's instructions by parent
folder. Quoted paths and -- are supported. Echoes, compound/redirection commands,
relative/interpolated/dynamic paths and unsupported readers do not get guessed.
No source file is read again or command executed by this classifier. Metadata
states reading/attachment, not proof a skill successfully completed.

The UI groups consecutive tools under Skill: name after that read/attachment,
within the same turn. This is chronological context, not a native causal link:
Codex does not always expose one. It resets at a new user message/new turn or
another explicit skill/read. Failed instruction reads do not attribute following
tools to that skill. Text/reasoning/receipt/error barriers retain their positions;
no conversation is reordered to manufacture one continuous skill execution.
Skill cloud collapses to content width and expands to the chat width. Steps open
the original native detail. Tool states use native status or observed started/
completed events/completed history turn; explicit skill attachment is labelled
Instructions supplied, not Applied. Local phase annotations are non-enumerable
and excluded from original JSON details. No prompt/inference/timer changes.

## Verification

138 focused JVM tests pass, zero failures/errors/skips. Coverage: existing chat/
keys/provider/lifecycle/navigation, stable grouped rows/scroll/expansion/details,
compact card height, command failure vs app error, RU/EN, skill/tool grouping,
message/skill barriers, unknown tool preservation and no fake completion.

Codex adapter fixtures pass: literal skill-read recognition/rejection, explicit
native skill input, tool-event status, no local state in detail JSON, user-message
reset and failed-read nonattribution, plus existing permissions/dedup/turn/model/
Shell/history/disconnect/owned process isolation. History-wire regression passes
3MiB chat acceptance, normal2MiB client refusal,33MiB header rejection and single
failure notification. Real native Codex0.163.0 with loopback synthetic provider
passes two turns/model/streaming/history; no real cloud account/model API used.

Release assembly/runtime verification passed:13114 defined classes,31 existing
reviewed optional types. Existing Kotlin/UAST/JNA Lint diagnostics remain; no
clean full Lint claim. ZIP/package/version/minSdk/ABI/non-debug/signature/SHA,
private-data/secret-marker and final staged source checks passed. All APK assets
except codex-chat.cjs and native libraries match1.6.2 byte-for-byte, including
codex-control.js with the large-history fix.

## Artifact

`Termux-Jobs-v1.6.3-test-arm64.apk`, 36667970 bytes.
SHA-256: `4bdea959c553380188bb1a97c0f0a30fb163478de082d6a9b8c8606d303399d1`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (unchanged).
APK/checksum copied to Download; destination SHA verified. No installed helper,
working session/environment, server service or account was modified.

Phone checks needed: reopen the same history; verify compact timeline, real
SKILL.md read/skill cloud and image-view caption; expand a step during work,
scroll history and switch agents. Not all ways of loading skills are detectable.
Device gestures/keyboard/DPI and real-provider behavior are not established by
mocked UI tests. Evidence: .verification/codex-skills-v1.6.3/.
