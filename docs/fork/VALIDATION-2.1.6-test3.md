# 2.1.6-test3 /1524 — saved chats and native RTK

Private ARM64 test APK; no automatic installation, publication or restart of user
chats. Same com.termux, signing certificate and workspace schema as1523.
Evidence: `.verification/rtk-offline/`; final identity: `artifact.json` and SHA256SUMS.

## Behavior

Saved stopped chats open Files/attachments/Git using the saved environment and
working directory. The independent reader opens only the selected native journal;
it never launches an agent or sends a model request. Attachment references come
from Claude/Codex JSONL, Gemini JSON and OpenCode parts in its read-only SQLite DB.
Existing loaded references are cached as a fallback. Missing, ambiguous, oversized
or unreadable histories show an error. JSONL records over16MiB are not read.
“Load more” pages journal references. Jumping to a live message is unavailable
while stopped; preview, original export and location use the existing file backend.
Provider-specific journal layouts outside these formats remain unsupported.

Limits/context store passive per-chat snapshots, with an explicit last-known
label and recorded time. A snapshot is not a current quota check. No snapshot means
“No data”. Claude subscription and context, Codex windows and context, and Gemini
context are captured when the running adapter supplies them. OpenCode does not
provide quota/context through this channel. No values are invented for old chats.
Compaction is unavailable until the chat starts. Permissions UI is outside this task.

RTK Lite retains the limited verified integration. RTK native («RTK штатный»)
delegates to `rtk hook codex/claude` from pinned OSS1.1.0, without our command
allowlist. This does not promise compression for every command or include the
non-OSS engine. The component is shared by the environment; desired mode, tracking
and recall are per chat. RTK's own config/data directories are scoped to that chat;
shared RTK custom configuration is not automatically imported.

Stopped-chat enable verifies/installs the component with the existing cancellable
flow, then stores the desired mode. The checkmark means selection; the label says
it applies at startup. Live connection requires the native owner identity and hook
confirmation. Old running adapters without the native dispatcher require an
explicit restart; no automatic restart occurs. Both modes keep full-output recovery.
Native recovery uses RTK recall, with its own capacity/retention limits; it is not
an unlimited archive. Full output is requested explicitly, not automatically sent.
Lite statistics measure bytes; native statistics are RTK's estimated token counts,
not subscription or whole-context savings. No RTK_RECALL=0 or disabled retriever.

## Checks and limits

Checked anew:34 Python installer/state/cancel/deferred/consent checks;3 supported Lite
filter checks;7 native-history reader checks;58 JVM UI/persistence/project tests;
full Modern Lint (0 errors,287 warnings); compilation and final signed APK ZIP/identity/ABI/DEX/assets/hash.
Initial compile failures were a captured-variable typo and an unavailable older
Robolectric accessor. Both were corrected. Four route tests expected the former
terminal fallback without a project panel; updated expectations match this task.
Original failure logs are retained, not counted as successful checks.

Native binaries with isolated synthetic providers (NOT actual model accounts):
Codex0.166.0 danger-full-access/never, Claude2.1.296 SDK/manual(default): four paired
scenarios,24 turns. Two overlapping processes share HOME/project/component; A runs
on/off/on, B stays off. Compression and recall stay isolated. Codex original-prefix
forbids resume when off; rule files remain byte-identical. Native recall --full
retrieves the first and last lines omitted from the provider's reduced result.
Codex danger-full-access/untrusted separately retained its approval requests and
declines. Production native statistics were read separately for both chats.

The earlier research additionally covered Claude file deny versus CLI-only deny
and file ask + broad Bash allow. These are research results, not new repetitions
of every permission combination in this APK. See [research](RTK-NATIVE-RESEARCH.md).
Auto/acceptEdits/plan/Bypass, Codex on-request/read-only/workspace-write are not
confirmed here. Root Bypass restrictions remain; no saved policy is changed.

Reused after input hashes were checked:95 native build inputs and three ARM64 JNI
payloads. No native/GPU/X11 runtime check is claimed. Modern Lint covers unchanged
Java/resources/manifest/build inputs; the final Python-only dispatcher readiness
fix is covered by Python/native tests and final APK asset equality.

Not checked on device: installation/update, stopped-chat UI after app restart,
narrow-screen layout, actual saved user histories, thumbnails/pinch zoom/Download
permissions, installation cancellation on Android. These remain acceptance checks
for this APK. No real cloud-model test was run; fixture-provider output is not proof
of model behavior or external-service reliability.

## Final artifact

- APK: `/sdcard/Download/Termux-Jobs-v2.1.6-test3-arm64.apk`
- SHA-256: `8962bf31ef12e59be4718f830b2426b1d41b6a939b00974f9b344f26f8abf767`
- Size: 40655837 bytes; com.termux,2.1.6-test3/1524,ARM64,non-debuggable.
- Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- Final ZIP/DEX/native/assets checks passed;632 production files match the stage.
  Legacy AGP lintVital emitted Kotlin-metadata compatibility diagnostics; the
  separate full Modern Lint succeeded (0 errors,287 warnings).
