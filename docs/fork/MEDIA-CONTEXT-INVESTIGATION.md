# Media and tool-result context — investigation in progress

2026-11-04. **No disconnection fix is complete or included in 2.1.3-test4.**
The [test APK](VALIDATION-2.1.3-test4.md) contains the separate audit changes.
No working history, authorization or session was edited. No native history was
manually stripped. No APK/component was installed, replaced or published here.

## 2026-11-04 — Native compaction, transport boundaries and request replay

**Current checkpoint, not a fix:** manual native compaction reduces image-bearing
context in an isolated custom-provider fixture while retaining displayed-history
APIs and originals. New image views make the context grow again. Native WebSocket
continuations differ substantially from the earlier HTTP measurements: they can
send only new items while retaining old images in server-side conversation state.
Cold reopen and reconnection still produce large full-context transfers.

Application/template baseline remains released v2.1.3 / `308c8895`; all changes
here are research harnesses/documentation. Automatic compaction is not enabled.
No working chat is compacted. No native journal is manually edited. The separate
README change belongs to HezeAI and is excluded from research commits.

### Supported mechanism and actual limits

The pinned 0.166.0 binary declares `thread/compact/start`, also described in the
[official app-server documentation](https://learn.chatgpt.com/docs/app-server#trigger-thread-compaction).
The request acknowledges immediately; the fixture waits for contextCompaction
completion **and** turn completion before continuing. This is not a model prompt
containing `/compact`, a rollback, or a manual edit of session files.

Provider behavior matters. The unauthenticated custom fixture uses native local
compaction, whose replacement keeps bounded user text plus a scripted summary.
First-party remote-v2 compaction is a different path: it sends a compaction trigger
and retains selected messages/images under a token budget. Native source inspection
at upstream tag revision `6ff1e1ca3b30fc8b914720681fcf5fe62e95f856` confirms
this distinction; it is not an attestation that the binary was rebuilt here.
Public API `/responses/compact` options do not establish availability or identical
behavior in this CLI/account. `unified_image_budget` and `context_management` are
under-development features, disabled by default; neither was enabled as a fix.
`compaction_image_budget` and HTTP request compression are already enabled by
default. They do not establish a serialized-byte ceiling for accumulated media.

### Manual compaction — checked anew, synthetic provider

Evidence `.verification/media-context/replay-YEfqyo/codex-5660046d/`:

| Native operation | Actual HTTP body bytes | Images in body |
| --- | ---: | ---: |
| Cold reopen before compaction | 28,921,265 | 20 |
| Native compaction request itself | 28,922,081 | 20 |
| Continue after compaction | 58,544 | 0 |
| Process restart/resume after compaction | 58,567 | 0 |
| Requested old-image view result | 1,502,585 | 1 |
| Four views of the same old PNG, then continuation | 5,833,718 | 4 |
| Restart/resume after those four views | 5,834,062 | 4 |

All three synthetic requirements (`REQ_KEEP_ORIGINALS`, `REQ_NO_PUBLISH`,
`REQ_VERIFY_RESULT`) remain in every captured request, including after both
restarts. The old image path remains available; native view_image output has the
same image payload hash as its original admission. All eight original API turns
remain accessible with identical complete contents. Before/after original-turn
hash: `6e122229fdffc04930a42c7736b201cc34e06a9b9e78244ff039ba3a79032851`.
The original 28,997,857-byte journal prefix retains SHA-256
`d53023cc325f6a5484d350b8271f00db0823e6bb93ce89d77c71bb5981a002bc`;
only native append operations grew that fixture journal. Dataset originals and
project copies retain their manifest hashes.

These are **input-retention and native-handler checks**, not proof that a real
model correctly remembers arbitrary requirements. The summary is scripted and
short. Visual details and previous assistant/tool reasoning are no longer directly
present in the compacted model input; important details depend on the summary or
must be read again. The current first-party account/model route, long requirement
sets exceeding native retention budgets and actual vision quality remain to test.
The compaction request itself can be large and can fail on the same slow route.
Consequently this is a controlled mitigation candidate, not a permanent media cap.

### Application adapter versus direct CLI — checked anew

Evidence `.verification/media-context/replay-ToXiDt/codex-24d54628/`.
Native `thread/fork` produced six **identical full history arrays** (hash
`ef85748543907843ff19e558767ad73121d067e385b2bd398e2b191f1bbc1374`),
without copying/editing journal contents. Both paths use the same 0.166.0 binary,
model `gpt-6.1-sol`, low effort, cwd, fixture-only never/full-access policy and the
same local HTTP provider. Actual captured model/effort values match. Fork IDs,
caller metadata and the subsequent test turns differ; entire outgoing bodies are
not claimed byte-identical.

| Path | First cold reopen body received in full | Second reopen body received in full |
| --- | ---: | ---: |
| Production private chat socket → unchanged adapter → native Unix app-server | 28,922,695 bytes / 20 images | 28,923,052 bytes / 20 images |
| Native `codex exec resume` | 28,922,721 bytes / 20 images | 28,923,078 bytes / 20 images |

The chat admission control message is 176 bytes and its reply 68 bytes. Both
large inference bodies reached the provider completely; no natural local failure
occurred. Android Java/UI is bypassed by this native harness and is not a phone UI
pass. The old external terminal/adapter comparisons remain historical evidence.

Two deliberate failures exercise localization, not attribution of the original
failure. On both paths the provider first closed after reading **1,048,576 bytes**
of the advertised 28,922,695/28,922,721-byte body; native retry delivered the full
body. In another pair the provider read the whole body, emitted response.created,
then closed before response.completed; retry delivered a second full body.
Eight path cases completed and every owned child was awaited at exit. A prefix
counter means bytes read by the provider application, not additional bytes queued
in the kernel. A response-phase close is distinct from upload truncation.

### WebSocket physical transfer versus retained model context — checked anew

Evidence `.verification/media-context/replay-nZ7JGC/codex-8a81d5d5/`:

| Native WS operation | JSON payload bytes received | Images newly transferred | Images retained by synthetic provider |
| --- | ---: | ---: | ---: |
| Continue after 20 PNGs on a live connection | 1,834 | 0 | 20 |
| Cold reopen | 28,921,267 | 20 | 20 |
| Old-image view result | 1,445,014 | 1 | 21 |
| Continue after view | 1,842 | 0 | 21 |
| Request whose response is deliberately interrupted | 1,858 | 0 | 21 |
| Native reconnect/retry of that request | 30,365,939 | 21 | 21 |

The pinned native client actually uses previous_response_id for incremental
continuations. A cold reopen may reference a **new empty warmup response** while
still transmitting the whole old image set: the field's presence alone does not
prove reuse of the previous visual context. Warmups (`generate=false`) are counted
separately from inference. All 15 observed requests use WebSocket; HTTP fallback
is forbidden by an assertion. Compression was deliberately not negotiated by the
synthetic server, so these are uncompressed WS JSON payload sizes, not external
compressed/TLS wire sizes. Server-held logical image counts are fixture state,
not proof of real-model perception or service-side billing/token accounting.

Initial WS fixture `replay-ixFg5K` incorrectly replied to upgrades with HTTP/1.0.
Native error identified that invalid upgrade and fallback; the run failed and is
preserved. Correcting the **fixture** to HTTP/1.1, then asserting actual transport,
produced the successful runs. This is not a production transport fix.

This qualifies the earlier HTTP-only prompt A/B conclusion: HTTP sends retained
images repeatedly, whereas a functioning WS connection can reuse them remotely.
No new viewing call is needed for the large resend after reopen/reconnect. New
viewing results independently enlarge retained context. Prompt wording cannot
remove either native restoration behavior or the already-retained visual state.

### Size, buffer and timer map — source inspection

| Segment | Confirmed limits / behavior | Scope |
| --- | --- | --- |
| Android CodexChatControl → private chat socket | Request 65,536 bytes; reply 524,288 bytes; read timeout 15 seconds | Text/references, not historical image body; image files are read by CLI |
| Adapter private socket | 65,536-byte input, 15-second socket timeout | Admission/snapshot protocol; admission does not wait for model completion |
| Adapter ↔ app-server, codex-control Connection | 32 MiB per WS message with largeHistory; legacy default 2 MiB; handshake 20 s; RPC acknowledgement 25 s | Per local message, not aggregate model context or whole model-turn deadline |
| UI history projection | 800 entries / 2 MiB retained projection, paged older history | Display cache; does not reduce native model context |
| Native provider defaults at pinned source | WS connect 15 s; stream send/idle wait 300 s; HTTP retry setting 4, stream reconnect setting 5 (configuration caps 100) | Defaults, not a measured external request's effective overrides or full-turn deadline |
| Native request preparation | HTTP zstd on the matching first-party path; WS negotiates permessage-deflate and supports incremental state | Uncompressed JSON, encrypted transport counters and model tokens are different measures |
| Preserved CONNECT relay source | 8 KiB / 8 s CONNECT header checks; 65,536-byte pipe chunks; configured read-idle timeout; drain wait 15 s | Headers/chunks are not inference-body size caps; live server source/config still needs equality verification |
| Synthetic collector | Existing 160 MiB test-body guard and 90-second fixture turn deadline | Harness bounds, not application/provider production limits |

No production limits were increased. Native source files, hashes and initial 404
path lookups are retained under `.verification/media-context/request-boundaries-source/`;
correct crate/module paths were resolved from the exact pinned upstream tree.
The underlying WS library defaults and actual provider-side input/body limits were
not exhaustively audited. A generic websocket-close message cannot identify which
external hop closed, nor how much plaintext the real provider received.

### External route and next work

The wife-phone diagnostic tunnel is unavailable and was explicitly turned off by
HezeAI. No further attempt to that phone is authorized until access is restored.
A read-only attempt reached the server, but loopback diagnostic port 22443 refused;
a separate allowlisted server check found two key-options rejection events.
No key, expiry, service or network settings were changed. This is a diagnostic
access failure, not the inference disconnection being investigated.

HezeAI subsequently requested testing from the available device through the old
server-side route. An existing launcher was found at
`/root/termux-fork/.tools/codex-geohide-hysteria/agent-via-hysteria`. Its manager
allocates per-agent instances and checks PID/start identity; no working tunnel
should be stopped. That external test is **pending**, including account/model
eligibility for remote-v2 compaction and a precise declaration of any route/device
differences. No external before/after outcome is claimed yet.

Git comparison is also now requested: from first attachment support through
published stable tags, compare image preparation, adapter/history/connection
changes and CLI selection. Initial inventory is preserved in
`.verification/media-context/git-history/release-inputs.json`. All 18 inspected
v1.6.0–v2.1.3 tags retain fallback installer **0.163.0**; missing-only preparation
prefers an existing PATH CLI. Thus the measured existing **0.166.0** must not be
called an automatic APK-bundled upgrade without additional installation evidence.
Next: compare unique stable adapter versions with a fixed native CLI/provider;
only bisect an observed difference, and keep application, CLI and network causes
separate. Old APK installation/data rollback are prohibited.

Consolidated validator passed for compaction/chain/WS roots; summary SHA-256
`b969cb474192f5560d5a6e75b111d3a0b66aacaa72d4570a7057d27296ee48ea`.
Reproduce using codex-compact, codex-chain, codex-websocket modes and the
[safe harness documentation](../../scripts/tests/media-context/README.md).
MCP/plugins remain excluded. No app/CLI setting or common/project prompt is
changed, no APK is built/installed, and nothing is pushed or published here.

## 2026-11-04 — Prompt instructions versus native image replay

**HTTP-fixture conclusion (qualified by the newer WS measurements above):**
Codex 0.166.0 repeats historical image payloads while building the next HTTP
request even when no new viewing call occurs. Removing our
common/project instructions does not remove that behavior. A separate new
`view_image` call adds another historical image result. Neither observation proves
the sole cause of the external disconnections or a completed fix.

### Scope and instruction review — checked anew

Application sources/templates: released `v2.1.3`, commit
`308c88955fd3b0691ded560c78ee3f945538381e`; investigation branch
`work/media-prompt-replay`. Production sources and templates remain unchanged.
Reviewed all four built-in common templates (Codex/Claude, RU/EN), the full shared
project template, and attachment-description generation in `codex-chat.cjs` and
`agent-attachments.cjs`. Saved user instructions and working projects were not read.
Consequently this review does not establish the contents of any user's actual prompt.

- The common rule selects **one latest screenshot when requested**, or the exact
  named file. It does not request opening all images or old attachments on every turn.
- Memory is mandatory at project start when present; that is a text-document rule,
  not an instruction to replay image payloads. The common templates explicitly
  prohibit needless rereading of unchanged documents and loading every document
  before every task. The project template requests relevant project documentation.
- No reviewed template requires rereading conversation history or reopening every
  attachment. Codex/Claude common bodies match after agent filename normalization
  in each language. This static equality is not a Claude runtime experiment.
- Attachment cards describe paths, sizes, samples and frames. They contain no
  instruction to reopen all images. The text-sample suffix **does** say to read the
  remainder in parts, without explicitly conditioning that on relevance. That is
  a possible incentive for extra **text** reads, not evidence of image replay or an
  observed real-model tool choice. A narrow optional wording is: “If the task
  requires omitted details, read the relevant ranges from the original file.”
  No wording or application behavior was changed during this investigation.

Templates are identified by SHA-256 in the captured request evidence:
RU common `38442641898429a370ce0cac7c2e04ec6545bad04f51a02c86423e9c7f5d7266`,
EN common `925f87fb35098cfad3904513160f8d86b1ae8ccf3d2e8429570793d47bbda549`,
project `3554f8178cfbc032b50bc07f3e937236a66a10dea420da84f3b1a6ed7c6a39e4`.
Claude common hashes remain respectively
`ef99e68d47ba84db9ab66150056de3f2e367ee17ddbaa29e97d3d86c830207bd` and
`34cef6e84276e01dd033202f84fa23cf3ba59486fa571e32c1038d8ba17591bc`.

### Controlled native A/B — checked anew

Reused the existing native Codex harness, local scripted provider and kernel
network guard. The guard was compiled and its negative probes passed before any
CLI launch: external/foreign-localhost addresses, UDP/packet, raw-syscall and exec
bypasses were denied. New HOME/CODEX_HOME/project roots, synthetic credentials and
a project `.git` boundary were used. This is a network fence, not a filesystem jail.
No working chats, authorizations, originals, published assets or global settings
were changed. Only fixture-owned children were closed; existing `stop()` waits
for their exit before resume/restart.

Native binary SHA-256:
`f2e6c980c937e734644bed1b4a9a018d3e63a0ccbe71aafc85c86a8c6e4b462a`,
version **0.166.0**, model identifier **gpt-6.1-sol**. No real model or cloud account
was called. Built-in CLI instructions remain in every condition; “none” means
no supplied common/project AGENTS.md. CLI defaults/configuration were otherwise
identical. All requested template text was confirmed **fully present** in every
actual request; comparison did not silently omit or truncate a template.

Each condition ran the same synthetic sequence: short control → two PNGs →
text-only continuation → process stop/resume and text-only continuation → two
scripted native `view_image` calls of the first PNG → text-only continuation →
second process stop/resume and continuation. Full prompt text is copied only into
fixture AGENTS.md files. Across four conditions: **32 completed turns, 40 captured
HTTP requests, eight scripted viewing calls confirmed by native image results**.
Every dataset original and project copy retained its manifest hash.
The summary validator also rejected two artificial corruptions: a missing old
image and an unexpected supplied prompt. Python/shell syntax and documentation
links/diff checks passed; no APK or Lint run is needed for this test/report-only change.

Actual serialized HTTP body sizes in bytes (not disk-history size or TCP wire bytes):

| Supplied instructions | Text-only continuation, 2 old images | Reopen, 2 old images | After two new views, continuation, 4 images | Reopen, 4 images |
| --- | ---: | ---: | ---: | ---: |
| None | 2,942,904 | 2,943,256 | 5,830,999 | 5,831,343 |
| Project only | 2,951,149 | 2,951,501 | 5,839,244 | 5,839,588 |
| RU common + project | 2,978,231 | 2,978,583 | 5,866,326 | 5,866,670 |
| EN common + project | 2,965,699 | 2,966,051 | 5,853,794 | 5,854,138 |

Every no-view continuation/reopen uses one request and **zero new viewing calls**.
The two original images have identical hashes across conditions and are retained
without another file-view call. Each scripted viewing call causes two requests:
2 → 3 images, then 3 → 4 images. Unique image payload hashes stay at **two**;
the same first PNG appears three times after both calls. This confirms two distinct
contributions: native historical replay without viewing, and growth caused by a
new viewing result. The templates add text bytes but do not change these image
counts. This experiment cannot measure whether a real model is more likely to
choose viewing under one prompt: tool choices were scripted and identical.

Evidence root: `.verification/media-context/replay-p1k6mG/`, including enforcement
log, four `latest-*-run.txt` pointers, native per-request image hashes/prompt-presence
metadata, and `prompt-replay-summary.json` (SHA-256
`7be947ae8ae6f28c3c305cd41b6fffbdbaf9ecbe4c32c88aa02ce3e189a39fb1`).
Four raw result hashes, ordered none/project/RU/EN:
`62e544884577f989b6deea90802adce4e56aeb6c645c11227c71dbf20664bb54`,
`e7ae6e8e0a889dff3953509eccfa935ae47688aa21ac144b3d20a425ec26dbc3`,
`86bd75bb556168ed99d53a6f93448c9c3ef1cc3cc94d5ee889421d4b216b5041`,
`58c39bf75c317af2f7e79984381f40b81f90b41cc6f5b7bbc5eae4bd98818417`.
Reproduce with `bash scripts/tests/media-context/run.sh codex-prompts`;
[runner limitations](../../scripts/tests/media-context/README.md) apply.

### Earlier evidence — reused as historical observations

Rechecked all four result SHA-256 values against `initial-summary.json`. The
original Codex no-view continuation and cold reopen each contained the same 20
image hashes: **28,920,368 / 28,920,720 bytes**, respectively. Three later native
views added duplicate results, 46 → 47 → 48 → 49 images, with 46 unique hashes.
Those outputs corroborate the new small controlled experiment; they are not new
phone tests. The old Claude/Gemini/OpenCode values below remain historical local
measurements, not new prompt A/B coverage or proof of identical internals.

### Decision and remaining checks

A prompt-only change cannot stop the native replay demonstrated here. No evidence
justifies removing the existing memory/context rules or shortening the user's
full project template as an image-disconnection fix. An optional anti-redundancy
rule could reduce unnecessary model-selected new views, but its benefit is
**unmeasured** and it would not prevent replay of already admitted image results.

The candidate must act at native model-request construction: bound historical
image payloads, retain durable file references/descriptions and restore relevant
visual detail on explicit demand. Duplicate handling must preserve call/result
relationships and text semantics; do not blindly strip tool events or edit native
session journals. A supported CLI capability or a separately validated CLI change
is required; the Android transcript/history renderer does not own this request.
Server state, cached tokens or a server-side file handle must not be assumed from
public API capabilities without verifying this CLI and login route.

**Not checked:** real-model prompt A/B/tool-planning propensity; new prompt A/B
for Claude, Gemini or OpenCode; the previous external route after a candidate;
phone behavior, visual fidelity after bounded projection, and a completed native
request-budget implementation. MCP/plugins remain excluded. The next step is to
validate a supported Codex request-projection candidate without modifying history,
then test continuation, reopen and requested old-image detail on the original
external route. The disconnection issue stays open.

## Baseline and acceptance

Historical phone evidence: Codex 0.166.0, Hysteria → Selectel → GeoHide,
20 PNGs totalling 20,815,040 original bytes / 27,753,412 base64 bytes in history.
Continuous batches eventually answered; cold reopen did not answer within
240.31 seconds and showed retries through 5/5. That is a bounded failed baseline,
not proof of a terminal CLI failure, a sole root cause or a completed solution.
See the preserved `.verification/wife-route-20261103/png-stress/REPORT.txt` under
`/root/termux-fork` and the project memory. Phone Codex versions differed (0.163.0
working session versus the 0.166.0 probe); no universal version claim follows.

Acceptance still requires a candidate that continues, reopens and recovers after
new media/tool results **without manual compaction on the previous external route**.
A local provider's immediate reply cannot meet that criterion. A read-only `ssh ...
true` availability check succeeded during this work; no external model request was
sent by this new probe and no real authorization file was inspected or copied.

## What is shared, and what is not

`ChatAttachments.java` preserves originals. It limits selection to four files and
samples up to 12 video frames. It prepares JPEG previews at a 1280-pixel long edge
in some paths. In particular, a Codex project-file PNG/JPEG/WebP/GIF at ≤ 20 MiB
can bypass that conversion; gallery and other-agent paths generally prepare a
preview. This source-level difference does not cover native tool-opened images.

The shared `agent-attachments.cjs` emits bounded text samples (8192 bytes), original
paths/metadata, Claude image blocks, Gemini `@` references and OpenCode file URIs.
Codex has its own corresponding admission/serialization path in `codex-chat.cjs`.
ZIP/STL/BLEND cards use binary metadata/path; the card path does not itself read and
send the binary bytes. The corrected adapter fixtures check that contract. It does
not restrict a later shell, file or MCP tool from reading/encoding that file.

Android transcript limits and `trimPreviews` are presentation limits. They do not
limit the CLI's next model request. `withoutImageData` removes media from Claude's
Android projection, not its native session/model context. Codex resumes with
`excludeTurns`, then pages turns; Gemini retains native history and limits its extra
projection; OpenCode pages the native message API. None of those facts proves a
small inference request. A stored-history size is also not a network-request size.

## Native local-provider measurements

Artificial v1 dataset: 20 deterministic 600 × 600 PNGs, **21,642,207 bytes**;
one large JPEG, one large WebP, 24 small PNGs, large code/log/JSON files and opaque
binary stubs. The stubs do not validate real ZIP/STL/BLEND parsing. The preserved
v2 generator adds valid ZIP/STL and small JPEG/WebP; generation is not a passed test.
All recorded v1 originals and the four selected runs' project copies retain their
manifest hashes.

Fresh kernel-fence checks rejected external/foreign loopback destinations,
UDP/packet/non-TCP traffic, deferred/Fast Open, raw-syscall bypass and descendant
exec bypass. Native subprocesses received fake HOME/config/XDG locations and
explicit synthetic credentials. A separate project-root marker stopped ancestor
project discovery. This is a network fence and separate fixtures, **not a kernel
filesystem jail or a complete trace of every file open**. Host network settings
and working connections were not changed.

Every successful response below came from a scripted artificial provider. Times
measure the local fixture operation, including native processing and observations;
they are not mobile-network, real-model inference or vision-quality results.
Artificial token usage is unsuitable for validating real automatic-compaction
thresholds. Counts below are completed cases, not universal scenario coverage.

| Runtime and scope | Completed local cases | Cold reopen after 20 PNGs: request | Saved history / API view | Local completion |
| --- | ---: | ---: | ---: | ---: |
| Codex 0.166.0 app-server | 23 | 28,920,720 bytes; 20 images | 28,996,591 bytes native history | 0.98 s |
| Claude Code 2.1.296 + production SDK adapter | 26 | 8,790,495 bytes; 20 images | 8,898,974 bytes native history | 0.94 s |
| Gemini 0.64.0 core + actual Google SDK, API mode | 26 | 28,858,452 bytes; 20 images | 28,862,439 bytes native session | 0.72 s |
| OpenCode 2.0.28 serve/API, custom compatible provider | 10 | 14,461,431 bytes; 10 images | 28,865,961 bytes API history projection, not DB size | 16.64 s |

Claude's actual cold-reopen values are confirmed in the evidence summary; its
native image preparation differs from Codex/Gemini. The OpenCode reduction is an
observed native projection in this configuration, not an application fix or a
verified global media budget. Its automatic-pruning trigger still needs analysis.
It persisted across serve restart in this probe. Do not declare all four runtimes
affected by the same defect or fixed by the same change.

Codex also replayed the large mixed context after native restart. Three actual
`view_image` calls for the **same file** each added one image to the next request:
46 → 47 → 48 → 49, with approximately 1.44 MB extra per read in this dataset.
The subsequent mixed request reached 39,733,279 bytes. The native large-log command
returned bounded text to the next request; original log bytes stayed on disk.

Claude and Gemini exercised native image/file-content paths, large text results,
frame payloads, continuation and native-history reopening. Gemini uses the native
recording parser; this is not a full interactive CLI/process/Android restart.
Their synthetic stream-break cases retried locally and completed. Codex's explicit
stream break also led to two requests and a successful local reply. These do not
show recovery from the original external service failure.

OpenCode's 12-frame payload reached the provider after the fixture's 20-second
native HTTP timer expired. That case is **not a pass** and not an established
application timeout. The earlier fixture also misread an inbox as a completed-state
log; the native inbox contains pending entries. Completion now uses active state
and a new completed assistant record. A keep-alive socket race in the observation
client was removed by using separate HTTP connections. Initial failures remain
in evidence; their cause is not assigned to production without another reproduction.

## Why view_image was missed, and the actual file route

The first probe searched only the outer request's `tools`. For this model,
Responses Lite places tool declarations in an `additional_tools` item in `input`.
They contain the `functions.exec` code-mode catalogue, including nested
`tools.view_image` and `tools.exec_command`. Absence of a top-level definition was
not evidence of an unavailable tool. The collector now reads both representations.
This is corroborated by upstream [client.rs](https://github.com/openai/codex/blob/6ff1e1ca3b30fc8b914720681fcf5fe62e95f856/codex-rs/core/src/client.rs#L1057), at tag `rust-v0.166.0`, peeled commit
`6ff1e1ca3b30fc8b914720681fcf5fe62e95f856` (inspection, not a rebuilt-binary attestation).

The actual app-server model catalogue reported `gpt-6.1-sol` with `text` and `image`
input modalities. The corrected probe executes native handlers and observes their
results in the **next HTTP request**, rather than treating a tool-call emission as
proof of execution. In the separate latest-file case:

1. A scripted provider requested a native command to find the newest image inside
   the artificial media directory. It consumed the returned path, then invoked
   native `view_image`. Three requests completed; the final request contained one
   image (3,744,925 bytes for the whole request).
2. A native command found the latest artificial MP4. Another native command ran
   real FFmpeg to extract a frame. Native `view_image` opened that frame. Four
   requests completed; the final one contained the retained first image and new
   frame (3,765,683 bytes total).

No paperclip attachment was used in either case. A real model's tool planning,
Android latest-file UI and the previous external route are not covered by this
scripted sequence. The native handlers, path handoff, decoding and serialized image
are covered. Evidence retains the model modalities, tool catalogue and request counts.

## Remaining coverage (not successful tests)

| Scenario | Codex | Claude | Gemini | OpenCode |
| --- | --- | --- | --- | --- |
| Large PNG + accumulation/reopen | Local native measured | Local native measured | Core/SDK measured | Local native measured; observed pruning |
| One large JPEG/WebP | Local native measured | Local native measured | Core/SDK measured | Local native measured |
| Many small PNG | Local native measured | Local native measured | Core/SDK measured | Not reached in completed run |
| Many small JPEG/WebP | Not run | Not run | Not run | Not run |
| Native latest-image/video discovery | Native handlers measured | Not run | Not run | Not run |
| Sampled frame accumulation | Latest MP4 decoding separately; large payload pending | Synthetic 12-frame payload measured | Synthetic 12-frame payload measured | Fixture timed out; incomplete |
| Large code/log/JSON | Native command/log and text measured | Native Read measured | Native content/converter measured | Not reached |
| Documents/PDF and full document tool path | Incomplete | Incomplete | Incomplete | Incomplete |
| Repeated image tool reads | Native view_image measured | One native Read; repeats incomplete | One native file-result conversion; repeats incomplete | Incomplete |
| MCP results and repeated MCP reads | Not run | Not run | Not run | Not run |
| Metadata-only binary cards | Adapter fixture | Adapter fixture | Adapter fixture | Adapter fixture |
| Subsequent genuine binary parsing/shell/MCP | Incomplete | Opaque stub Read only | Opaque stub content reader only | Incomplete |
| Mixed chat + deliberate disconnect | Local native | Local native | Core/SDK | Incomplete |
| Automatic/manual native compaction under real token accounting | Not validated | Not validated | Not validated | Trigger not established |
| Previous external route after a fix | Not run; no fix yet | Not run | Not run | Not run |

## Implementation direction, not an implemented fix

A complete candidate needs a bounded **request projection immediately before model
serialization**, covering user media, tool/MCP image blocks and tool text. It must
leave originals/native history intact, keep stable source references and tool-call
pairing, deduplicate repeated data, retain selected recent previews, and substitute
explicit references for older media or oversized text. Details need bounded crops,
pages or line ranges on demand. Critical system/project instructions must not be
silently discarded to fit a byte budget. Oversized irreducible requests need a
clear error. Budget values still require measured mobile/external-route validation.

App-side preview preparation, frame/selection budgets and truthful metadata can
improve admission; they cannot control an independent native `view_image`, shell
or MCP result. Increasing local bridge buffer sizes or automatic compaction alone
is not the proposed solution. Native request projection requires a distinct,
verified integration for each CLI:

- Codex: current app-server adapter admits turns and observes events. A central
  native serialization hook/patch must cover both direct and code-mode tool images.
  Its experimental `unified_image_budget` must not be enabled as an assumed fix;
  `compaction_image_budget` is already enabled upstream and is not proof of a
  serialized-byte cap or a remedy for this baseline.
- Gemini: the installed core has a BeforeModel path that can accept modified
  contents. Its typed request projection is a promising integration, to test across
  user/tool/MCP parts and API/Vertex without mutating retained history. Current
  [BeforeModel documentation](https://geminicli.com/docs/hooks/reference/#beforemodel)
  is supporting documentation, not proof that an application fix has been wired.
- Claude: current [PostToolUse documentation](https://code.claude.com/docs/en/hooks#posttooluse-decision-control)
  describes schema-matched `updatedToolOutput`, including built-ins, and a legacy
  MCP-only field. This needs verification on pinned 2.1.296. Replacing a fresh tool
  result does not by itself manage old user attachments or the whole next request.
- OpenCode: investigate its observed native image pruning and pinned 2.0.28
  provider/plugin boundary before adding a second limiter. Current general plugin
  documentation does not establish the required hook for that binary version.

No experimental switch, proxy, new CLI binary or provider/auth setting was silently
installed here. There is no completed cross-agent patch yet. The external-route
acceptance, full matrix and implementation work remain open.

## Evidence and reproducibility

Local evidence: `.verification/media-context/initial-summary.json`, v1
`data/manifest.json`, per-agent result pointers, `codex-latest-reference.json`, native
HTTP logs and network-enforcement results. The original 240-second phone baseline
stays separate. Manual source probes and a fresh-fixture runner are in
[scripts/tests/media-context](../../scripts/tests/media-context/README.md).
Their [native inputs](../../scripts/tests/media-context/native-inputs.json) record
actual inspected bytes/versions. The runner itself requires existing prerequisites
and tests the kernel fence before execution; see its limitations before running.

The pre-existing common attachment test had stale simulated admission behavior:
Claude needed a native replay acknowledgment and explicit negative replies rather
than always throwing; Gemini needed the native input/session identity tags. Those
fixture contracts were corrected. The common adapter test now passes. That is a
test repair, not a media-disconnection fix. All application production inputs remain
identical to the delivered audit APK's input manifest.

The newly preserved guarded runner was executed for `codex-latest` with a fresh
v2 dataset (`replay-UJEqWS`): fence negative checks and all three native cases
passed. Other preserved runner variants were syntax-checked; their earlier native
measurements and limitations are recorded above, not relabelled as fresh runner
passes. Node adapter/history baseline: five unchanged suites passed; the stale
common attachment suite passed after the test-contract repair described above.


## Урок: 2026-11-03 — Codex0.166.0 связывает прежний чат с идентификатором провайдера

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Codex0.166.0 связывает прежний чат с идентификатором провайдера

- Контекст: обход обрыва WebSocket на втором телефоне с сохранением прежнего чата.
- Подтверждение: нативный app-server0.166.0 в изолированной фикстуре отказал в resume
  openai-чата при выборе termux_jobs_https: modelProviderMismatch. В списке такого
  провайдера прежний чат также отсутствовал. Реальные аккаунты не использовались.
- Вывод: отдельный HTTP/SSE-провайдер нельзя выдавать как прозрачный запускатель
  прежнего чата. Сетевой запускатель без смены провайдера этого конфликта не создаёт;
  это не подтверждает устранение сетевого обрыва. Условия других версий проверять отдельно.
- Доказательства: .verification/wife-route-20261103/app-server-fixture/result-resume.json
  и .verification/wife-route-20261103/ssh88/REPORT.md. В сетевых пробах были и успехи,
  и TLS-тайм-ауты; не записывать SSH88 как подтверждённое исправление телефона.


## Урок: 2026-11-03 — Context Mode: MCP и полный плагин проверяются отдельно

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Context Mode: MCP и полный плагин проверяются отдельно

- Контекст: исследование context-mode 1.0.169 в Ubuntu ARM64, Node 22.22.1; рабочие аккаунты и сессии не использовались.
- Вывод: Codex 0.166.0 обнаружил MCP через app-server и выполнил индексирование/поиск; Claude 2.1.296 сообщил connected и 11 инструментов через потоковый CLI; Gemini 0.64.0 сообщил Connected через mcp list. Это не подтверждает работу hooks, compaction, реальной модели или интерфейса приложения.
- Установка: start.mjs повторно запускал npm install better-sqlite3 и не уложился в ожидание стенда. Прямой неизменённый server.bundle.mjs работал. Не переносить автоматический self-heal/обновление в запуск каждой сессии без отдельной проверки; подготовку зависимостей отделять от запуска MCP.
- Настройки: в Gemini 0.64.0 основной Storage добавляет /.gemini к GEMINI_CLI_HOME. Изолированный профиль должен учитывать это; первая фикстура в неверном каталоге не обнаружила сервер. В рабочем приложении не подменять HOME/авторизацию ради подключения MCP.
- Границы: дополнительные proot-bind в стенде осложняли завершение процессов; один tracing-stop процесс потребовал адресного завершения. Это не установлена ошибка штатного запуска. При внедрении повторить проверки завершения в настоящих контроллерах.
- Подтверждение: `.verification/context-mode-20261103/REPORT.txt`, `codex-final-result.json`, `claude-result.json`, `gemini-result.json`, `isolation-result.json`. Отдельные CONTEXT_MODE_DIR разделили базы двух одновременных экземпляров одного проекта. Внедрения и APK ещё нет.


## Урок: 2026-11-03 — Короткий drain-timeout CONNECT relay обрывает медленный поток

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Короткий drain-timeout CONNECT relay обрывает медленный поток

- Контекст: server relay SHA-256 `6d770234913a6c2e281fab38a2ccd1e4fbc56b51b8a81d7b818db7b7cf0542bb`, большой поток через GeoHide.
- Подтверждено: `pipe` ограничивает `writer.drain()` 15 сек независимо от TCP-прогресса; искусственный прогрессирующий транспорт воспроизвёл TimeoutError ровно через 15 сек. Кандидат использует общий ограниченный idle_timeout; четыре сценария и 13 прежних сетевых/маршрутных проверок прошли. Позднее настоящий loopback с паузой получателя 18 секунд подтвердил обрыв старого кода и полную передачу 2 MiB новым.
- Граница вывода: серверные счётчики и RST согласуются с этим механизмом; логи не содержат ID запроса и типа исключения, поэтому конкретный запрос телефона и все другие ошибки этим не доказаны. GeoHide также показал отдельные TCP/TLS-отказы коротких публичных HTTPS-проб.
- Применение: не считать короткое ожидание drain контролем отсутствия передачи; большой буфер может медленно освобождаться. Проверять прогресс, конечный бюджет и освобождение соединений. Не менять общий работающий маршрут ради испытания.
- Доказательства исследования: `.verification/wife-route-20261103/recheck-html/REPORT.txt`, `drain-result.json`, `observe-close.json`, `candidate-tests.json`.
- Применение по разрешению пользователя: отдельный relay `wife88` на 18759 обновлён; его исторический маршрут только .88 заменён порядком общего relay .67/.68/.88. Отдельная Hysteria 8443 теперь разрешает также 18759; прежний 18749 сохранён. Перед рестартом этих двух служб проверены PID и отсутствие TCP-соединений; QUIC-сессии отдельно не перечислялись. Общие службы 443/18749 сохранили PID и время активации. Приёмка телефона не выполнена. Прежнее утверждение «кандидат не развёрнут» заменено этим состоянием.
- Выдан отдельный установщик `/root/selectel-fixed` без apt/автозапуска. Восемь проверок установщика и два коротких HTTPS через 8443→18759 прошли; это не проверка большого запроса модели или доступности 8443 с другого телефона. См. `.verification/wife-route-20261103/drain-fix/REPORT.txt`. Общий маршрут 443 по-прежнему содержит старый timeout.
- Повторная проверка после переключения второго телефона: пользователь сообщил reconnect; сервер подтвердил трафик через 8443→18759 и правильный хеш кода. Большой поток продолжался свыше106сек, около66кБ/с, с rwnd_limited≈95%; два других upstream-сокета получили удалённый FIN. CPU≈4.4%, память доступна, прироста CPU-throttling нет в измеренном интервале. Это подтверждает применение фикса, но не устранение проблемы телефона. Конкретный запрос со скриншота не привязан к TCP-потоку; причина за узлом GeoHide не установлена. См. `.verification/wife-route-20261103/reconnect-194104/REPORT.txt`.
- Проверка на втором телефоне по временному SSH: реально запущен штатный Codex0.163.0, наш процесс использует0.166.0. Базовые компоненты присутствуют, watchdog не перезапускал туннель. После разрешённого просмотра истории установлено: около27.8МБ из29.3МБ —20 PNG-результатов view_image; большие ZIP/STL представлены ссылками, а не пользовательскими вложениями. Размер истории не считать размером каждого запроса. CLI также сообщает timeouts списка моделей/лимитов: нельзя объяснять все ошибки только изображениями. См. `.verification/wife-route-20261103/phone-access/PHONE-REPORT.txt`; причина различия поведения версий не проверена.


## Урок: 2026-11-03 — /proc/PID/exe внутри PRoot может быть загрузчиком

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — /proc/PID/exe внутри PRoot может быть загрузчиком

- Контекст: диагностика native Codex в Ubuntu AI на другом телефоне.
- Вывод: ссылка /proc/PID/exe у работающего Codex указывает на Termux libexec/proot/loader. Её отдельный запуск с --version завершился сигналом11; рабочий процесс не затронут. Для проверки версии сначала установить настоящий исполняемый файл по mapped path/пакету и запускать именно его. argv0 может быть только codex без абсолютного пути.
- Подтверждение: штатный бинарник из codex-cli/0.163.0 дал codex-cli0.163.0; это совпало с метаданными истории. Отдельный SSH-login также не воспроизводит дополнительные bind-пути приложения в /tmp. См. `.verification/wife-route-20261103/phone-access/PHONE-REPORT.txt`.


## Урок: 2026-11-03 — DNS и короткий HTTPS не подтверждают восстановление Codex

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — DNS и короткий HTTPS не подтверждают восстановление Codex

- Контекст: второй телефон после переключения с Hysteria на пользовательский VPN. DNS обеих Ubuntu одинаковый, 8.8.8.8/8.8.4.4; настоящий CLI сообщил failed to lookup address information. Поздние DNS-пробы прошли, HTTPS с обходом DNS сначала также не устанавливал TCP.
- После переподключения короткие публичные запросы дали HTTP200/ожидаемый401 без ключа, но новый запрос CLI продолжил Broken pipe/WebSocket close, а история не росла. Проверять свежий turn и завершение ответа отдельно от health/robots.txt; не считать одинаковый resolv.conf доказательством одинаковой доступности DNS.
- Граница: это наблюдение текущего телефона/маршрута, не доказательство дефекта VPN-продукта, CLI или изображений. Полный текст tracing span может содержать случайные цифры502; искать код HTTP по точному сообщению ошибки, не по наличию подстроки.
- Подтверждение: `.verification/wife-route-20261103/vpn-reconnect-2/REPORT.txt`, снимки предыдущей проверки в `vpn-check-1724`. Настройки телефона не изменялись.


## Урок: 2026-11-03 — Большие PNG: проверять продолжение и повторное открытие чата отдельно

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Большие PNG: проверять продолжение и повторное открытие чата отдельно

- Контекст: второй телефон, настоящий Codex 0.166.0, установленные JS-активы, маршрут Hysteria → Selectel → GeoHide. Отдельные тестовые чаты; рабочий Molds не запускался.
- Подтверждено: короткий контроль до/после нагрузки ответил за 8,61/8,63 сек. Первые четыре исходных PNG дали повтор WebSocket и ответ через 231,92 сек. Накопление до 20 PNG порциями прошло; повторное открытие с 20 PNG вызвало 5/5 и не дало ответа за 240 сек. Это предел стенда, не окончательный отказ CLI.
- Граница: PNG поданы как localImage через установленный адаптер, а не через Android-подготовку или исходный tool-output путь. История загрузила все 20 картинок и шесть ответов; Android-отрисовка не проверялась. Серверный TCP показал медленную отправку и ограничение окном получателя, но внутренний источник закрытия соединений не установлен.
- Применение: не считать успешный короткий запрос или непрерывный чат проверкой восстановления большого контекста. Различать передачу модели и небольшую проекцию карточек для интерфейса; размер истории не равен размеру каждого сетевого запроса.
- Доказательства: `.verification/wife-route-20261103/png-stress/REPORT.txt`, матрица `result.json`, пагинация и TCP-снимки. SSH с равным объёмом PNG и полный UI остаются непроверенными.
- Дополнение 2026-11-04: настоящий `codex exec` 0.166.0 без адаптера воспроизвёл тот же WebSocket close с сохранёнными 20 PNG. Через Hysteria/GeoHide ответил за 32,94 сек; ближайшая проверка адаптера — 31,32 сек с повторами. Через VPN Android без наших прокси — 94,89 сек с пятью ошибками, затем ответ; короткие контроли около 6 сек. Compaction не было. Совсем прямой маршрут без VPN не прошёл даже короткий контроль (есть HTTP 403, конечная причина turn.failed не установлена). Не приписывать ошибку только интерфейсу или только GeoHide. Официальный APK Termux и TUI этим не проверены. См. `.verification/wife-route-20261103/terminal-compare-20261104/REPORT.txt`.


## Урок: 2026-11-04 — Codex Responses Lite: отсутствие верхнего tools не означает отсутствия инструмента

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-04 — Codex Responses Lite: отсутствие верхнего tools не означает отсутствия инструмента

- Контекст: отдельные искусственные профили, Codex 0.166.0 / gpt-6.1-sol и локальный провайдер под проверенным kernel network guard. Не телефон и не проверка внешнего сервиса.
- Причина ложного вывода стенда: каталог Responses Lite расположен в `input` → `additional_tools`, включая описание вложенных инструментов code mode. Проверка только верхнего `tools` ошибочно исключала view_image.
- Подтверждение: native model/list объявляет text/image; настоящий exec_command нашёл последний файл, view_image передал изображение в следующий HTTP-запрос. Для MP4 отдельно выполнены поиск, настоящий FFmpeg и просмотр кадра. Три повторных view_image одного PNG в смешанном чате добавили три изображения в последующие запросы этого стенда.
- Применение: проверять объявление инструмента и фактическое выполнение отдельно; не заменять путь инструментов вложением. Историю, размер файла и размер запроса измерять раздельно. Наблюдения Codex не переносить на другие CLI.
- Доказательства и незакрытые области: активный checkout, `docs/fork/MEDIA-CONTEXT-INVESTIGATION.md`, `.verification/media-context/`. Исправление обрывов не готово и не входит в test4; внешний маршрут после исправления ещё не проверен.
