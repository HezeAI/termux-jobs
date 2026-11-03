# Private test27 — Claude Code instructions editor

VersionName 2.1.1-test27; versionCode1462. Codex/Claude Code selection uses the
existing General/Project/Diagnostics editor. Claude chooses Ubuntu AI/Ubuntu;
Codex remains Ubuntu AI. No chat engine, permissions, launch transport, user auth
or session data format was changed. Details: [editor guide](AGENT-INSTRUCTIONS.md).

## Checked anew

- 54 Python backend cases passed: Codex regression; Claude actual config path, absent files, explicit creation, additive/local files, nested .claude files, atomic saves/history/recovery/conflicts, exact text, shared-memory paths and @ references, symlink refusal, settings-value filtering and authentication exclusion.
- 64 Java/UI cases passed: 53 editor cases, 4 helper-command cases and 7 agent-environment cases. Coverage includes both selectors, RU/EN preview/apply/cancel, imports/history, retained conflicting drafts, explicit agent/environment switching, shared memory preferences, stale-preview rejection and narrow/large-font/reduced-height layouts. These are model tests, not phone acceptance.
- Native Claude 2.1.296 plus the app control adapter used an isolated HOME/config and a loopback synthetic provider. Markers were observed in actual outgoing provider requests. General/project changes used the actual Python atomic-save/history backend. Same-process next request kept old markers; restart/resume loaded new markers and retained history; a fresh conversation loaded new markers. Root/local/.claude rules loaded together, unimported LEARNING and AGENTS.override did not. A separate fresh conversation without project Claude files confirmed AGENTS.md fallback. See native-instructions-final.log; no real model obedience is claimed.
- The installed Ubuntu AI binary was read-only hashed and matches the tested pinned binary: f1f6e96e0d8342b9dbf41d7e88255397a6a52ce3d8736ad6a4c6b59c9b62fefa. The launcher source pins the same 2.1.296 binary. No installed CLI was replaced or working process restarted.
- Native Codex 0.163.0 app-server/local-provider regression passed through the current backend: initial sources, hot shared changes, project changes on resume/new process and retained history. The checked helper hash matches the packaged helper. This does not generalize behavior to every version.
- Fresh modern Lint 7.4.2 analyzed all five modules: 0 errors, 235 warnings, no infrastructure diagnostics. Warnings remain visible; compileSdk/plugin and existing Kotlin metadata limitations were not hidden.
- Final build succeeded. Exact APK ZIP, package/version, ARM64-only ABI, non-debuggable status, signature/certificate, SHA-256, assets and runtime inventory passed. No required or unreviewed missing runtime types. Packaged templates/helper match current sources; no auth/key filenames were included.
- Saved general/user and project instructions are unchanged by hash. Opening/switching/launching does not create Claude instructions; replacement requires explicit user preview/apply/import/save. Source/path names are agent-specific, not Codex precedence relabeled as Claude.

## Exact artifact

Termux-Jobs-test27.apk; 39989665 bytes; com.termux; 2.1.1-test27/1462; ARM64 only.
SHA-256: `b3d6da5500c0bc2b95550189db4151f9c52eb5769758b77acf4484d14cfaeed3`.
Signer SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` (same as v2.1.1/test26).
VersionCode1462 exceeds test26/1461 and earlier reserves. No data reset, incompatible
migration or automatic instruction overwrite. Existing history/settings persist
in their existing formats; device update acceptance remains separate.

## Reused

Existing SDK/dependency caches and three packaged native libraries were reused;
native bytes match delivered test26. Other unchanged agents/graphics/installers
were traced as unaffected and not reinstalled or launched. Codex general templates
are byte-identical; Claude RU/EN templates are equivalent with filename/agent
adaptations. The shared LEARNING template names both instruction conventions;
actual memory is never copied into per-agent files. Memory history uses each
selected editor's config history store; the physical project file is shared.

## Limits and phone checks

CLI instruction sources depend on settings, managed policy, imports, conditional
rules and custom connection executables. Diagnostics shows standard-startup file
candidates, rule-directory presence and safe known setting values, not a merged
active-process prompt. Arbitrary imports/credentials are not expanded. Project/local
settings can contain an instructionFiles-looking value without governing the
built-in plugin's user/managed setting; diagnostics reports observed values, not
an assertion that those values are effective. Unknown CLI versions remain labeled
unverified. Native auto memory (MEMORY.md) is distinct from shared LEARNING.md and
its settings were not changed. Other environments' CLI installs were not assumed.

Phone keyboard, touch, font shaping, accessibility and switching during real usage
still require acceptance. No working chats/tunnels/servers were used or restarted;
no APK installation/publication or external transport fix is claimed. Project
AGENTS.md remains 19,109 bytes, below the restructuring trigger.

## Initial failures retained

Early native-fixture attempts used an invalid socket basename and input ID; those
were corrected to the existing adapter contract. Initial logs are retained and
not treated as product failures or PASS. The first UI run expected the former Codex
project-chooser title; the original Codex title was restored and reruns passed.
Final evidence/logs/input hashes: .verification/claude-instructions-test27/.
