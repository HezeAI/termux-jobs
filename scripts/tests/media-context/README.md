# Manual media/context probes (investigation, not a fix)

Run from the active checkout with existing native CLI installations:

```sh
bash scripts/tests/media-context/run.sh codex-latest
bash scripts/tests/media-context/run.sh codex-prompts
bash scripts/tests/media-context/run.sh codex-compact
bash scripts/tests/media-context/run.sh codex-chain
bash scripts/tests/media-context/run.sh codex-websocket
bash scripts/tests/media-context/run.sh codex
bash scripts/tests/media-context/run.sh claude
bash scripts/tests/media-context/run.sh gemini
bash scripts/tests/media-context/run.sh opencode
```

These commands send **only artificial data to an artificial local provider**.
They do not use cloud accounts or establish that an external disconnection is fixed.
The wrapper checks selected native bytes, compiles the existing kernel network fence,
executes its negative tests, creates new private fixtures, and runs the selected probe
under that fence. Missing prerequisites or fence failure stop the run. No config-only
network fallback is allowed. No packages, global firewall settings or real profiles change.

Evidence goes into a new `.verification/media-context/replay-*` directory; native
profiles live in the existing `fixture-paths.cjs` scratch root. They are preserved.
The fake HOME/XDG/project root is separate; this is not a kernel filesystem jail.
No real auth is copied and native subprocess environments are explicitly constructed.
Only fixture-owned children are stopped. Logs contain synthetic responses/metadata,
not production profiles or provider credentials.

The probes record actual local HTTP body or WebSocket JSON-payload bytes, inline image counts and latency.
Original file bytes and native history bytes have separate meanings. OpenCode's
`historyProjectionBytes` measures a requested API view, not a per-session on-disk DB.
Responses use artificial token usage (or omit it); these are **not valid automatic
compaction-threshold, vision-quality or real-provider latency tests**.

- Codex: actual app-server, native localImage/view_image/exec_command, replay/restart.
  The model uses Responses Lite: tools are declared in `input.additional_tools`.
  Nested code-mode tools may not be top-level tool definitions. `codex-latest`
  checks actual command discovery, view_image and FFmpeg frame extraction. A
  scripted provider drives tool calls; it does not test a real model's planning.
- `codex-prompts`: same two artificial images and scripted turns with no supplied
  instructions, project template only, RU common + project, and EN common + project.
  Checks full prompt discovery in actual requests, matching image hashes, no-view
  continuation/reopen, and two native reads of the same image. The common and
  project files are copied only into fresh fixtures. It does not measure a real
  model's independent propensity to reopen files. The summary validator also checks
  that every generated original and project copy retains its manifest hash.
- `codex-compact`: explicit native `thread/compact/start` in the artificial
  thread only. Checks preserved task markers, old API turns and journal-prefix
  hashes, cold resume, old-image bytes and renewed duplicate growth. This custom
  provider uses native **local** compaction with a scripted summary. It does not
  establish real-model semantic retention or first-party remote-v2 behavior.
  No automatic-compaction setting is changed.
- `codex-chain`: production private chat socket + adapter + actual Unix WebSocket
  app-server versus native `codex exec resume`. Native `thread/fork` gives identical
  initial histories without editing journals. Explicit model/effort/directory/policy
  and local HTTP provider match. Records provider body receipts, normal reopen,
  a deliberate close after a full body and during upload at a 1 MiB read prefix.
  Only the receiver's application-read bytes are counted, not hidden kernel/TLS bytes.
  This bypasses Android Java/UI and the real external route. Owned children are
  awaited at exit. Dropped responses/upload closures are fixture injections.
- `codex-websocket`: native Responses WebSocket with retained artificial server
  state. Counts physical input images separately from provider-held logical
  images; exercises cold reopen, native image viewing and forced reconnection.
  No WebSocket compression is negotiated. Warmup (`generate=false`) is not inference.
  The initial fixture used an invalid HTTP/1.0 upgrade and fell back to HTTP;
  that failed run is retained. The corrected fixture requires HTTP/1.1 and asserts
  every request uses WebSocket, so fallback cannot be misreported as success.
- Claude: production SDK adapter + real CLI, a new/resumed native session and
  the actual Anthropic HTTP client. The separate controller socket is bypassed
  because this fence intentionally denies filesystem Unix listeners. That control
  transport and physical Android UI are not verified by this probe.
- Gemini: real 0.64.0 core, native recording/parser and actual Google HTTP SDK in
  API mode. Native read-file content/conversion is exercised. Full interactive
  CLI restart, permission UI and Vertex authentication are not exercised.
- OpenCode: actual native serve/API/OpenAI-compatible provider. Its 20-second
  fixture HTTP timer can expire on the large 12-frame case. Such a run is a failed
  fixture, not an established production timeout. No timed-out cell is a pass.

Current scripts exercise large PNG/JPEG/WebP, many small PNGs, sampled-frame
payloads, text/tool results and mixed replay to different extents. MCP, many small
JPEG/WebP, PDFs/documents, complete automatic compaction and external recovery
remain incomplete. Generator v2 prepares small JPEG/WebP and valid ZIP/STL;
that fact alone does not mean those cells have run. BLEND is an opaque signature
stub, not a valid Blender document. The first recorded v1 dataset used opaque
binary stubs for all three binary extensions. Do not claim archive parsing.

See [current findings](../../../docs/fork/MEDIA-CONTEXT-INVESTIGATION.md).
These files are test infrastructure only and are not included in 2.1.3-test4.

Validate the three completed research roots without rerunning any CLI:

```sh
python3 scripts/tests/media-context/summarize-context-probes.py COMPACT_ROOT CHAIN_ROOT WS_ROOT
```

This asserts original hashes, retained requirements/history, matching explicit
model settings and provider receipts, and the WebSocket delta/full-replay boundary.
It does not convert a scripted local result into an external-route or phone pass.
