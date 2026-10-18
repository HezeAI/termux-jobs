> Historical experimental feature: removed from the current v1.5.22-test source/APK. This document is retained as research evidence, not current functionality. See [removal notes](RELEASE-v1.5.22-test.md).

# Public availability and private testing

Public v1.5.0 disables local-model creation/startup while testing continues; existing experimental data remains. The implementation below describes private test builds. Private v1.5.1-test includes Qualcomm runtime for the pinned Gemma4E2BSM8750 model; installed-app NPU initialization/inference still requires device verification. See [test notes](RELEASE-v1.5.1-test.md).

# Local AI — v1.4.4-test

Experimental native chat, separate from Codex/OpenCode/Claude/Gemini and from PTYs.
Create a session or launch preset and select **Local AI / Локальный ИИ**.
Choose a model, acceleration and a new or saved local chat. Launch opens the chat
and asks for confirmation before downloading a missing model. Existing files are
reused. Stop interrupts generation/download and unloads the model after inference
has stopped; partial catalogue downloads can be resumed with Start.
Long-press the local session to save its launch preset or close it.

## Local chat controls and project permissions

Start/Stop is in the terminal status row. Long-press the local session: New chat, Choose chat, then Settings. Model/access
and chat selection are also available in Settings. Switching chats preserves saved
history and reuses a loaded model; a stopped model needs Start. The session selector only lists sessions and Create new session. Reasoning is collapsed, not deleted from history.
Empty reasoning blocks are hidden, including streamed partial think tags.
Markdown is rendered offline; code copying is explicit, HTML and remote images
are not executed/loaded.

Three modes: Chat only (default), Read project, Work on project / full access.
Full access executes file writes and shell commands WITHOUT confirmation by default; the separate Confirm actions option can enable Yes/No cards.
The bottom Path checks checkbox controls file-path confinement independently:
on limits file tools to the project; off permits other app-accessible paths.
It does not disable Android permissions or the selected mode. With this checkbox enabled, arbitrary shell commands are disabled even in Full access. With it off, shell commands require Full access and are not project-confined. This is not a
security sandbox; concurrent changes to parent directories can race path checks.
Files/tool outputs are untrusted context and small models can follow them wrongly.
Do not grant full access to untrusted projects expecting the model to enforce a
security boundary. Read mode still denies file writes and command tools.

Choose the project/environment in the existing picker (including shared storage
and installed proot distributions). No distribution is installed automatically.
JSON tool requests can be plain or wrapped in a complete json/tool/unlabelled fence. Mixed prose, trailing objects, extra fields and wrong argument types are rejected. Requests are checked by the application; prose/code blocks are never
implicitly executed. The app uses its selected environment and Android UID.
Tool text is bounded to 64 KiB; runtime previews are further bounded for context.
Command timeout is 10 minutes. Cancellation targets the owned timeout process
group, never existing terminal processes. Detached jobs can survive: foreground
commands only. No arbitrary number-of-actions cap is added; Stop remains available.

## Models and engines

The bundled catalogue contains five model families/sizes and six pinned files:
Qwen3.5 2B/4B/9B GGUF Q4_K_M via llama.cpp b9222; Gemma 4 E2B/E4B via
LiteRT-LM 0.17.0; and an experimental E2B SM8750 NPU variant.
Each catalogue file has its repository revision, exact byte size and SHA-256.
Only the selected weights are downloaded. Models are NOT included in the APK.
The Android native engines and their Java dependencies ARE included in the APK.

Import your own model using the app file manager: Termux, installed proot roots or shared storage. The shared-storage picker checks/requests Android access. Copying shows a progress bar, percent, bytes, speed and estimated remaining time (not elapsed time). Outside taps and Back cannot dismiss the progress dialog; use Cancel. Header signatures determine
GGUF vs LITERTLM, not the filename extension. Data is copied to a unique private
file with cancellable progress and a computed checksum. The selected original is
never modified. Architecture/template/backend compatibility is validated at load;
a recognized container is not proof that an arbitrary model will run.
An import checksum without a trusted expected hash is not an authenticity check.
Hold a model card to remove local weights; chat history is retained.

Files: private `files/local-ai/models` and `files/local-ai/chats`; per-model cache
under `cache/local-ai`. Models are shared by local sessions without duplication.
Uninstalling/clearing app data removes local models/history. Do not assume a
selective shell-HOME backup includes these private app files.

## Implemented scope and limits

* Text chat, streaming output, saved history and independent input drafts.
* File tools and optional foreground command execution: see permissions below. Audio/image input and MTP are not implemented.
* A non-exported bound Android service in `:local_ai` owns native inference.
  This is crash/process separation, not a separate Android UID/security sandbox.
* One model is loaded at a time. Starting another local model unloads the previous
  one; its tab/history remain. Switching workspace pages does not reload the model.
* Runtime context: 4096 tokens; response budget: up to 768 tokens. IPC/replayed
  history is bounded; full saved history remains on disk. An oversized prompt can
  be rejected instead of silently deleting history.
* llama.cpp: CPU only in this build. Vulkan/GPU acceleration is not compiled in.
* LiteRT-LM: GPU/CPU selection, Auto tries GPU then CPU. Actual driver/model
  compatibility and speed require device testing; no performance claim is made.
* Historical previews before v1.5.1-test: matching model/chip checks exist, but the required Qualcomm
  dispatch/QAIRT runtime was not included. NPU launch was explicitly blocked with
  an explanation before model download. It is not claimed working.
* MTP/speculative decoding is not enabled. It needs separate version/model tests.
* The bound service is not a persistent foreground service. Android may stop it
  under memory pressure/background limits; UI reports disconnection. Native failure
  is separated from TermuxService, but system-wide memory pressure can still affect
  other processes. No availability guarantee is made.
* UI/history/keyboard/DPI and real Gemma/Qwen inference need developer device checks.

## Build

The phone build uses existing read-only NDK/sysroot and Termux Clang tools.
`python3 scripts/prepare-local-ai-tools.py` downloads hash-pinned official AAR,
llama.cpp source and an isolated CMake tool into `.tools/local-ai`.
`scripts/build-local-ai-native.sh` builds the ARM64 CPU JNI library.
Then run the existing isolated Java verification and ARM64 APK build scripts.
R8 8.9.35 is pinned because the SDK contains Java 21 bytecode; the project sources
remain Java 8. SDK API calls are behind a small version-specific Java bridge.
The old Lint Kotlin metadata reader emits incompatibility diagnostics for modern
Kotlin dependencies; successful assembly is not claimed as a clean Lint run.
License/third-party notices are bundled in `workspace-local-ai-notices.txt` and
kept in `docs/fork/third-party`. No code or prebuilt binaries from FlexAI were copied.

## Rollback

New test: `1.4.4-test`, versionCode **1109**.
Recovery: the pre-feature `1.3.4` APK with only manifest versionCode raised to
**1110**, re-signed with the unchanged certificate. Its DEX/resources/native payload
must compare equal to the original, excluding manifest/signing metadata.

The workspace schema stays v8 with optional local fields. Old commands, notes,
SSH and cloud-agent profiles do not require a reset. Local-AI presets are not
understood by 1.3.4: they may appear as plain empty Termux presets; do not use them
there. Saving the library in an older build may discard optional local fields.
Private local chat/model files remain but the old UI cannot open them. Back up
library JSON if you need to preserve local preset parameters through rollback.
A later APK must use versionCode **1111 or higher** to update over recovery.
No automatic clearing/migration of user data is performed.

Local chat presentation: black background, no colored message cards, inline
You:/AI: speaker labels, preserved Markdown and a compact expandable Reasoning
line. Terminal sessions and cloud-agent output rendering are unchanged.

## v1.4.3 action handling and reasoning

All local engines/imported models use LocalAiAgent: act for clear requests allowed
by the current mode, without offering to act or asking again for already-granted
permission. Explicit paths must be preserved. This remains model-dependent:
application instructions cannot guarantee correct action selection by every model.
One corrective generation follows a reasoning-only reply or an unexecuted offer;
this is not a cap on tool-call rounds. Failed attempts remain in raw saved history
but are excluded from future model context. No code from reasoning/prose executes.

For Qwen-compatible ChatML templates, the native bridge mirrors the official
`enable_thinking=false` assistant prefix. The old /no_think text instruction was
not a valid substitute. The correction is template-specific, not applied to Gemma.
See [official model card](https://huggingface.co/Qwen/Qwen3.5-2B/blob/main/README.md)
and [generation template](https://huggingface.co/Qwen/Qwen3.5-2B/blob/main/chat_template.jinja).
CPU finish reason distinguishes EOS from token budget; LiteRT only reports sdk_done
when its callback completes, without claiming an exact finish cause.

Confirm actions is a new optional per-session setting, disabled by default.
When enabled, full-access write/command requests produce a real Yes/No card in
chat. The owned worker waits without blocking the UI. No declines/cancellation
execute the action. Approval does not expand read/chat permissions. Stored cards
are historical and never execute again on reload; interrupted pending cards are
shown as interrupted. Path checks and diagnostic logging are independent.

The Reasoning pill opens a separately scrollable dialog. Nothing expands inside
the chat or calls chat fullScroll when opening/closing the dialog. Empty thoughts
remain hidden. Text stays in raw history; explicit tool results are labelled.

## Current scope / action permissions (v1.4.4)

These are independent settings for every local backend/imported model:

| Action mode | File reading/search | File writing | Arbitrary shell commands |
|---|---|---|---|
| Chat only | No | No | No |
| Read | Yes | No | No |
| Full access | Yes | Yes | Only with Path checks OFF |

Path checks ON confines file tools to the selected project and blocks the command
request before approval or process construction. Approval cannot bypass this.
Absolute paths within the project remain valid; external paths/symlink escapes
are rejected by the file tools. No list of supposedly safe executable names is
used: even pwd/git/build scripts are blocked as arbitrary shell requests.

Turning Path checks OFF requires accepting a localized warning. Until acceptance,
the effective checkbox/selection stays ON; Cancel/Back/outside dismissal keeps it
ON. OFF permits app-accessible files outside the project, but never upgrades Read
or Chat mode. Existing saved OFF choices are retained without a reset. The warning
states that Full access may change/delete accessible data and run commands, not
that the app receives Android root or access to every filesystem. Android/storage
permissions and installed environment availability remain applicable.

Confirm actions independently pauses allowed Full-access writes/commands at a
Yes/No card. Arbitrary commands are not a project sandbox; no separate isolation
has been introduced. Canonical file checks are not a security boundary against
malicious concurrent same-UID directory replacement (as noted above).

## v1.4.6-test session behavior

Active local Settings include Session name; rename preserves loaded model and chat ID. Unsaved sessions do not offer Delete preset. The folder button manually opens the current project in the selected environment through the existing file manager, with storage access requested for shared-storage roots. Manual file management is independent of AI action/scope settings.

Streaming scroll is focus-neutral and hidden panels skip stream view updates. Drafts remain editable during inference; a second request is not submitted until the current one ends. Changed permissions take effect on the next turn with a fresh system prompt in both engines; changing model/backend still stops the old model. Existing history is retained and may bias a small model: instructions and a single repair attempt do not guarantee compliance.

## v1.4.7-test — discovery, media and package-plan confirmation

See [current feature/compatibility notes](RELEASE-v1.4.7-test.md). Runtime fields/local permission values remain unchanged; the scope checkbox is now **Work only within the project / Работать только в пределах проекта**, and the mode menu is **Local AI permissions / Права локального ИИ**. Off permits paths beyond the project within actual Android/environment permissions; it does not give Android root. Scope ON still forbids arbitrary commands.

The install tool is a separate explicit user action: Confirm actions must be ON in a non-chat mode, package must be in the fixed tool catalogue, APT simulation must succeed, and the user must press Yes. Read still cannot issue arbitrary shell commands or write files. This manual install can change the selected environment beyond the project and the card states this. No package installation was performed as part of source/build verification.

Images use actual temporary ImageFile contents with the supported pinned Gemma LiteRT models; only three latest images/frames are attached per runtime context, without audio. Sources do not leave the device through this media pipeline. Sources/frames are temporary, removed after the turn; previous-turn history retains text/path metadata and does not silently replay expired images. Qwen/native text path unchanged; separate visual projector is not supplied. Imported models stay text-only until explicitly supported. Real native vision/model/decoder behavior must be checked on a device.

## v1.4.8-test — explicit tool environment

Select Main Termux or Ubuntu for local AI independently of the project folder. Dedicated Ubuntu uses the persistent jobs-local-ai alias with private ownership/ready records; missing setup is explicitly confirmed with stage/log progress. The image is fresh, and working Ubuntu is never used as a fallback. Python3/Git/basic GNU tools are prepared there; later APT installs persist there. Main Termux intentionally remains shared. Older local profiles with ubuntu/other proot aliases retain history/settings but are blocked until reselected.

Local tool execution and file browsing use an internal transient backend marker; ordinary sessions referencing a proot alias keep their old login behavior. Dedicated login is --isolated with readable shared Android storage bindings, without Termux HOME/PREFIX/shared-tmp mounts. It is not a strict security sandbox. A full-access Main Termux command still has the app's normal shared permissions; routing is not a sandbox against a hostile command. See [setup, compatibility and device checks](RELEASE-v1.4.8-test.md). Model imports can still be manually copied from existing environments; that does not select a tool environment.

## Portable discovery (v1.4.9-test)

All local engines share screenshots discovery with multi-extension newest-first results, common Android storage paths, per-location access/scope status, and explicit alternate paths. Short direct screenshot requests trigger discovery before model generation. Results do not imply visual inspection or exhaustive access. External CLI agents use their own tool protocols.

## Screenshot wording and tool previews (v1.4.11-test)

Screenshot discovery accepts polite/greeting short requests. Mixed prose/tool JSON is presentation data, not executable text. It appears as an unconfirmed preview card, with raw details on tap, while one bounded correction may ask the model to return a standalone request. Existing tool authorization, scope and approvals still apply. User text/history stays raw. All local engines use the shared renderer; external CLI agents are unchanged.

### Deferred idea: file-name alternatives

Developer agreed to discuss in a subsequent update: generic file search with translated/transliterated name variants (for example факторио/factorio), suffix-tolerant matching, and displaying multiple candidate names, paths, sizes and dates for selection instead of silently choosing the first. Not implemented in v1.4.11-test; current substring search still depends on the model choosing the query.

## Local file selection (v1.4.12-test)

The previously deferred name-variant search is now implemented as spelling/transliteration matching plus explicit known alternate names from the model, not universal translation. Named searches with multiple candidates or an approximate single match pause for a FileChoiceCard. Its picker passes an exact candidate path back to the turn. Selection is not tool authorization. Cancel/Stop prevents continuation; restored chooser cards are read-only. Friendly result windows keep raw JSON behind Technical data. External CLI agents and vision capabilities are unchanged.

## Latest files (v1.4.13-test)

Short RU/EN requests for the latest typed file in Download(s), Documents, Pictures, Music or Movies use an app-coordinated locations → list → optional text-read sequence. Exact extensions are filtered before mtime sorting/limit. Explicit paths, questions and compound tasks stay with the model. This is conservative intent recognition, not model training or guaranteed semantic understanding. Listing details include checkedPath; empty means only that folder. Every step retains normal rights/scope checks.

## Runtime context and recall (v1.4.14-test)

The current configured context is 4096 tokens, not the advertised model maximum. llama.cpp counts the exact formatted prompt with the loaded tokenizer; LiteRT uses an estimate and a single context-overflow retry. The full transcript stays on disk while old reasoning/turns and large tool outputs may be omitted/excerpted from the model input. A service notice describes this; it is excluded from inference. Current permissions and the latest user request are not silently cut. An oversized current request can still fail clearly.

`{"tool":"history","query":"server","limit":5,"start":0}` searches only this chat before the current turn (read/full modes). Up to 10 literal case-insensitive excerpts are returned, each with an original message index. This is not semantic search or an AI summary. Existing history loading remains limited to 8 MiB.

Successful duplicate reads are reused during a turn unless `refresh:true` is requested; mutations invalidate the cache. Short latest-file read/open requests for supported text formats display literal contents immediately. Complex analysis still needs inference; binary media/APKs are not shown as text or automatically executed.

## Budgets and timing diagnostics (v1.4.15-test)

Local session/preset settings expose context4096/8192 for llama.cpp and4096 for LiteRT; reply caps256/512/768/1024/2048. Defaults stay4096/768. Busy changes are blocked. Context reloads the idle engine preserving chat; reply cap changes next turn without reload. High output caps reserve more of the context and may leave too little input space.

Long-press a local session → Session diagnostics for live stage, elapsed time and completed stage durations, model/runtime/budgets and actual token metrics when exposed (GGUF; LiteRT unknown). Copy/export and last-request private report are supported. Metadata only, not prompt/file/tool content. Phase text replaces empty AI placeholders; timing refresh does not redraw the chat. RU/EN simple find-latest-text-and-read bypasses intermediate model generations.

## Native prefix timing (v1.4.16-test)

See [timing/cache behavior and test sequence](RELEASE-v1.4.16-test.md). Input progress
is reported per native decode batch. Cache snapshots are volatile, not saved chat
history. Switching exact chat scope/system instructions resets model state; a
budget miss or unsupported restore uses full prefill. LiteRT does not claim these
llama-specific counters. The diagnostic pause setting affects journal recording,
not permission checks or generation.

## Per-call diagnostics (v1.4.17-test)

The same diagnostic report retains up to64 model-call records for the latest user
request, including intermediate tool workflows. Current call is shown live; finished
and interrupted calls remain separately identified. Pause skips archival records.
llama.cpp counters include token reuse; LiteRT exposes available timings and keeps
unavailable native counters empty. See [report fields](RELEASE-v1.4.17-test.md).

## Previous results and short latest-file requests (v1.4.18-test)

Large earlier tool results are excerpted only for the next-turn runtime context;
full saved history and literal display remain intact. The latest user request and
current-turn results stay unchanged. A notice marks excerpt use; history can recall
archived output and read can obtain current file contents. Requests without a folder
ask for one rather than assuming a directory. Screenshot text-only discovery lists
actual paths and states that image contents are not visible. No rights are changed.

## CPU engine variants (v1.4.19-test)

Engine in local session/preset settings selects Automatic or Compatible CPU for
llama.cpp. Automatic probes hardware before optimized-library loading; compatible
mode forces baseline. Idle changes reload the same chat; busy changes are blocked.
Diagnostics identify the actual variant and threads. This is independent of tool
environment, rights, context size and LiteRT acceleration. See
[implementation and comparison procedure](RELEASE-v1.4.19-test.md).

## Tool validation and repeated reads (v1.4.20-test)

Invalid structured parameters are reported back to the model (two correction retries
per turn), not executed/coerced. Security denials still block the action. Cached
reads are labelled from trusted execution metadata; a further non-refresh repeat
after two cached repeats stops the loop and keeps prior results. Explicit refresh
and other kinds of loops are not covered by this specific guard. CPU selector
label ARM acceleration (when supported) retains stored value auto and existing
hardware fallback. Both native libraries are unchanged from1.4.19-test.

## Synthetic prefill tests (v1.4.21-test)

Diagnostics → Input processing test requires a loaded, idle llama.cpp model. Choose
lengths, threads or batches; save each report separately to Download. Last report
is separate from chat diagnostics. A temporary context shares the model weights,
not chat KV/recurrent state; partial results survive cancellation. Test Stop uses
its own cancellation gate/token, not the normal chat stop operation. The CPU time
counter is per inference process and summed over threads, not overall device load.
Ordinary chat thread/batch settings are unchanged. See [test details](RELEASE-v1.4.21-test.md).

## Processor settings (v1.4.22-test)
Session/preset → Engine settings: processor mode and thread count. Auto keeps the previous cap of four. Manual choices persist per profile; the service clamps to available CPUs. Saving changes on an idle loaded llama model reloads it without deleting chat history. LiteRT CPU controls are not exposed.

## Input composition (v1.4.23-test)
Diagnostics exports now include composition for each fitted model input. Total llama tokens are exact; component counts are leave-one-group-out estimates with formatting/boundary residual. LiteRT is estimated. Counts contain no prompt text. Compare a fresh chat greeting with a saved chat greeting; cache counts already indicate the part actually re-evaluated. Tokenizer probes do not generate output or reset cache.

## Smaller runtime input (v1.4.24-test)
System instructions are deduplicated. Only earlier-turn tool outputs longer than256 characters are reduced to128/48-character head/tail excerpts plus source/history marker. Original history remains on disk; model can use history to recover omitted details. Current-turn results are not changed by this layer. Existing context fitting still operates independently. Measure actual token counts and latency on the selected model before claiming a speed gain.

## File-list context (v1.4.25-test)
The common local-agent coordinator sends list/search/screenshots metadata as columns/rows after selection. Full paths, type, size, modification time, match and scope/status metadata remain; repeated names/object keys/notices are reduced. All requested rows remain. Saved results and UI use originals. Tool data is untrusted and selections do not grant rights. Latest-file instructions recommend filtering/sorting/limit rather than forcing a hidden one-file cap. Applies to llama and LiteRT; not external CLI agents.

## Engine location experiment (v1.4.26-test)
Environment has Tools and Engine cards. Tools selects main Termux or dedicated jobs-local-ai Ubuntu. Engine selects Android(default), a native Termux subprocess or static GNU/Linux runner in that same dedicated Ubuntu. No working Ubuntu fallback, duplicate distro/model, HTTP listener or implicit permission changes. Engines are bundled in APK and use stdin/stdout. Ubuntu setup, if absent, uses the existing confirmation/progress flow. CPU mode/thread settings remain under Engine settings; LiteRT supports Android only. Switching location reloads idle model and retains chat, but not computation cache. Compare the same model/thread/context/input with the benchmark; no speedup promised.

## Bundled operating instructions (v1.4.27-test)

`app/src/main/assets/local-ai-agent-instructions.md` is loaded from APK assets, cached in process and included on every local-agent turn. It is not a project AGENTS.md loader and never reads arbitrary files as trusted instructions. Current profile settings follow it and remain authoritative. All local llama/LiteRT models share this path; external CLI agents are unchanged. File permissions are enforced in code, not granted by prose.

Full-access guidance includes requested edits/deletions/builds and checking dependencies with allowed shell commands in the selected tool environment. Project-only mode still disables arbitrary commands. Read mode cannot write/delete/run commands; its fixed install-tool exception still requires Confirm actions and explicit APT-plan approval. With Confirm actions off the dedicated install tool remains disabled; full access with project-only off can use command/package manager, subject to Android/environment permissions. No installation policy was weakened by this update.

Instruction compliance and accurate oldest-file selection are not guaranteed by small models. Do not execute model-proposed commands manually without checking their targets. Actual model/device checks remain necessary.

## v1.4.28: prompts and environment

New sessions default to dedicated Ubuntu for actions and llama inference; LiteRT remains Android. Existing selections are preserved. Allow commands in Termux is an extra explicit route for command only, not a file-tool/permission bypass. Requests may use `environment: termux` or `environment: ubuntu`; omitted uses the selected tool environment. Full-access and project-boundary code checks still apply.

Behavior .txt/.md UTF-8 is imported via device-storage browser, max12KiB, retained as private session text. Rights prompt can be generated or custom after a red acknowledged warning; active settings still constrain tools and are included in context. Changed settings mark custom rights stale. Reset automatic to regenerate tool schema/settings. Standard instructions/corrections cannot make small models reliable; validate actual tool cards/results.

Unsaved closing separates presets/history, and global Chat histories manages local/Codex conversation deletion with explicit confirmation. See RELEASE-v1.4.28-test.md for supported catalogs, preview/API limits and external active-process caveats.


## v1.4.30-test operating mode

Engine settings → Operating mode → CPU / GPU (OpenCL, testing). CPU is default; its ARM/compatible settings and threads reappear unchanged after returning from GPU. Initially only catalog Qwen3.5 2B on Snapdragon8EliteSM8750 is supported. GPU inference runs in an Android child process; Ubuntu tool environment remains selected independently. Select CPU if GPU initialization fails. Compare actual request diagnostics; CPU prefill benchmark is unavailable while GPU selected. Error cards now contain original calls and precise schema feedback. Empty write truncates, never deletes. See [release notes](RELEASE-v1.4.30-test.md).
