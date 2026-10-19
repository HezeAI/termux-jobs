# Historical private build records — v1.5.13-test

Private version names were reused during development. These are distinct builds;
versionCode, APK hash and the specific report identify each artifact. Neither
record below is validation of current v1.6.0; see VALIDATION-v1.6.0.md.

## Earlier record preserved from public v1.5.3

# v1.5.13-test validation

## Source and logical review

Trace: authorization → repeated-work check → optional approval wait → cancellation check → actual runner → recorded result. The new repeated-write warning blocks that proposed call before approval or execution. Read verification remains possible. Different writes, command/install attempts, failed writes and fresh reads showing changed target contents reset the write assumption; cached annotations do not. Scope/access/approval settings are unchanged. There is no global tool-call cap and no process-stop change.

The reported false Not executed card is reproduced by a successful action record, an accepted approval record, then its tool result. Rendering now pairs nearest matching action/tool/output across UI-only messages. New user messages and unmatched outputs remain boundaries; old unmatched legacy results remain visible. Approval history is not changed or cleared. Decision pills are read-only; disabled Yes/No cannot trigger again. Permission granted is not a claim of successful execution.

Pending cards retain request, APT plan and caveats in a scroll area. Only presentation size and collapsed decision display change. UI tests do not prove phone layout/taps/gestures.

## Build and artifact

Release ARM64 build and required runtime-class audit passed. Existing Kotlin metadata compatibility diagnostics remain in the successful build log. ZIP CRC, duplicates/traversal, package com.termux, minSDK26, private versionCode1150, ARM64-only ABI, v2 certificate, private AI flag, seven QNN/LiteRT payloads, limited embedded-secret scan, unchanged earlier native libraries and external-agent assets passed. Previous signing identity retained. No reset/migration, publication, working-session or installed-environment changes.

16 real-SDK interface checks with fake inference passed. This does not test NPU hardware. Final Java results are recorded below after suite completion.

SHA-256: `d1e05f7095337bfba05ea78ef60f0549e4ce23a1d540eebdea9a26fe69fdc205`
Size: 104890056 bytes.

## Device checks still needed

Repeat locations/write/read in the selected local environment and inspect the final file. If the model repeats work, verify the redundant call is blocked and it either reads/finishes or stops clearly. Test Yes, No, cancellation, restored history and opening decision details on the actual device. Verify no misleading duplicate appears after a successful approved write. No claim of exhaustive model-loop detection: aliases/changing contents/arbitrary commands need separate evidence.

## Final verification and delivery

Full Java suite passed: 650 app cases and145 unchanged terminal-emulator cases, zero failures/errors/skips. Ten new cases cover write-stall logic, changing listing metadata, verification after warning, explicit repeats, state invalidation, fresh work, compact/read-only approval decisions, exact history pairing across approval and actual waiting for the click. Log: `.verification/local-ai-v1513/tests.log`; XML counts saved in `test-counts.json`. Final Download copy independently SHA-256 verified.


---

## Subsequent development record

# v1.5.13-test — Android chat for OpenCode

Private code1175; public release remains v1.5.3/code1165. No GitHub publication.
Source/build checks and developer device verification are separate.

## Change and compatibility

New OpenCode launches use the existing owned2.0.28 server without launching mini.
Android renders messages, reasoning/tool/permission pills and supported native
forms. The original composer always addresses the model; a separate Shell field
executes direct non-interactive commands and shows results in chat. The existing
upper card remains. The packages button becomes the same-size vector robot for
structured OpenCode, with a paint-only border for known native-only requests.

The official mini opens on demand in a dialog on the same chat; chat/terminal
are not displayed together. Closing it keeps the server/history. The custom
jobs2 bundle is absent from this APK. Historical sources/templates are preserved.
Read OPENCODE-ANDROID-CHAT.md for supported forms and remaining interface gaps.

Native histories, connection roots, preset schema, access policies and project
rules are unchanged. No data clearing/migration. Existing running old frontends
are not converted; testing requires a new/restarted OpenCode launch. Package/cert
identity and version increase are checked on the final APK. Next stable must use
code>1175 to update this private build.

## Evidence

Ignored materials: `.verification/opencode-chat-v1.5.13/`.

- Actual pinned native server, fresh isolated HOME/XDG and synthetic imported chat:
  headless history/reasoning, no mini output at startup, no terminal replay under
  four PTY resizes, paged reasoning details, direct printf output without inference,
  model change preserving native ID, native idle interrupt outcome, cancellation
  of an owned sleep command, native message admission/dedup, once permission reply
  and sidecar, native form reply, official mini on demand, closing mini retaining
  queued native input and chat backend, complete owned shutdown: passed.
- The probe forces resume:false for synthetic message admission, so it does not
  consume model inference. This is not a real provider's streaming reply test.
- Node adapter tests: stable message/part ordering, live reasoning, foreign-session
  isolation, stable permission/tool position, duplicate/changed/oversized sends,
  stale form rejection, paged detail and rejection of foreign shell cancellation:
  passed. Shared receipt/Gemini wrapper tests and check-gemini-live: passed.
- Android tests cover stable View identity during text updates, receipt insertion
  without moving subsequent answers, late-response session guards, API-only model
  submission, dialog keyboard restoration and the headless launch command.

Final checks:

-559 JVM tests (405 app +154 emulator),0 failures/errors/skips. Includes separate
  model-vs-Shell composer routing and acknowledgement-only Shell draft clearing.
-Release ARM64 build: BUILD SUCCESSFUL.12976 defined runtime classes,31 reviewed
  optional unresolved types. Existing Linux/aarch64 JNA, Vendor/UAST and Kotlin
  metadata diagnostics remain; this is not a clean full Lint claim.
-APK `Termux-Jobs-v1.5.13-test-arm64.apk`, 36573650 bytes (~36.6MB).
-SHA-256 `f6bb4ff6d2ae9ab66c1ddd4cf0904c8cf6c23d07429c7199fa1a8125b1de0f3a`.
-Package com.termux,1.5.13-test/code1175,minSdk26,ARM64 only; certificate SHA-256
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`. ZIP integrity/v2 signature checked, same identity as
  v1.5.12/code1174 and a higher versionCode.
-All common assets except the OpenCode bridge are byte-identical to1.5.12.
  The new opencode-chat module matches source; only the3 historical jobs2 assets
  disappeared. Native libraries are byte-identical. New chat classes exist in DEX;
  retired local-model code, private auth/config files and secret markers are absent.
-Copied APK and SHA sidecar delivered to Download; copied hash verified.
-Git diff whitespace check passed. GitHub unchanged.

## Remaining device checks

The developer must test a new OpenCode launch with their own provider: normal
messages and a request to inspect Download, streamed reasoning, scrolling while
answering, opening/closing the keyboard, switching upper workspace tabs and to
other terminal sessions while typing. Check both independent composers/drafts,
model card, once/deny, common question choices, robot animation/native fallback,
and Back from native terminal. API/protocol/mocked Android tests cannot certify
physical keyboard/gestures/DPI, all auth providers or all complex forms.

A full Markdown/media/subagent UI, arbitrary interactive shell-program embedding
and automatic recognition of every possible native prompt are not implemented.
The direct Shell API is non-interactive; the fallback dialog is official mini.
Native-only requests remain available there; raw TTY programs use ordinary sessions.
No claim is made that every graphical edge case is fixed before device testing.
