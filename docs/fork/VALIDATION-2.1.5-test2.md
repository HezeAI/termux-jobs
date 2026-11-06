# 2.1.5-test2 /1494 — native v35 redesign

**Historical: the developer rejected the visual implementation.** The successful functional/artifact checks remain evidence for their scope only. Corrections and new visual limitations are recorded in [test3](VALIDATION-2.1.5-test3.md).

This is a private test build. Source checkout: `/root/termux-fork/.verification/rollback-live-actions/source`, branch `work/redesign-v35-1494`. No installation, working-session restart, push, tag or publication is authorized. Application-source commit: `78e013a3b9f23cceda3035c3f4645892571753bd`. Final signed release APK was built and checked locally; device behavior remains unaccepted.

## Scope and compatibility

Native Android sidebar and session-specific menus replace the former primary navigation. A title expands its menu; explicit Open selects the chat/terminal. Base Termux remains first/protected. Existing terminal engine, adapters, native histories, authentication, schema8, package and signer are retained.

Both composers use native controls: agent attachments/notes/queue/model/reasoning/dictation/send, Stop above Send, compact native remaining quota; terminal commands/aliases/notes and independent input/extra-key modes. Drafts and reading positions remain session-scoped. New Android speech recognition returns only into the unchanged originating draft; it never sends a request automatically. It does not add a RECORD_AUDIO permission or an internal recording service.

Global settings retain the existing managers, all backup/alias/server entry points, app/environment update separation, About and licenses. New agent creation retains preparation checks and puts the connection command last. Four RU/EN common templates and saved user instructions are unchanged.

X11 presentation fills the area below the header. View changes do not start/stop the desktop. Desktop/settings remain environment-wide. Shared owners are disclosed, and stop is rejected unless the selected session is the sole confirmed owner; the service repeats the owner check. Existing landscape DesktopFullscreen remains the rotation path. Java ownership checks changed, native JNI/graphics payloads did not.

Text reveal is presentation-only, per grapheme and adaptive to backlog. Old history is not replayed. Markdown tables appear atomically. Terminal ANSI/control bytes are processed immediately; selection, interactive/alternate-screen/control repaint paths favor correct immediate output over animation. Reduced animations and hidden views stop decorative work, not the underlying task.

## Stand results (not phone acceptance)

Evidence directory: `.verification/redesign-v35/`. Initial failed runs are preserved alongside retries in [the function map](REDESIGN-v35.md).

- `routes-4`:101 passed; `regression-fixes-1`:68 passed, no failures/errors/skips. These are scoped runs, not the entire suite.
- `regression-1`:1797 tests,13 failures. Six Unix-socket fixture failures used an overlong temporary directory; socket guards were retained and the repeated run uses the existing short task path. Five tests expected removed navigation/field order. One headless replay test mixed delivery assertions with animation timing. One real regression replaced bound local-link spans during reveal; fixed by binding the captured action before storing the reveal target, with a new tap-after-reveal check.
- `regression-2`:1799 results,1 failure,0 errors/skips. The app reran1645 tests; the other154 module results were up-to-date from the prior full run, not newly executed. The remaining diagnostics fixture bypassed Activity language initialization; it now initializes resources as the real Activity does. `regression-fixes-2`:11 passed, including that case and the new50/100/150% persistence check. Initial failures are retained.
- Localization check:586 workspace strings plus app/shared settings keys and format arguments pass.
- Modern Lint7.4.2:all five modules analyzed,0 errors,258 warnings, no rule-loading/infrastructure diagnostics. New warning keys concern drawable loading and composed display labels; minimum API26 supports the existing drawable call and labels have RU/EN branches. No broad suppression added. The compileSDK35/AGP-tested33 caveat remains. Final release APK checks:passed as detailed below.

Tests use artificial profiles/files/events and mocked Android/native boundaries where required. They do not confirm a real device, GPU, microphone provider, subscription or external connection. No real working session was stopped for these checks.

## Reused code / immutable inputs

The native source/config inventory has94/95 unchanged entries against the accepted baseline. The sole changed inventoried entry is Java X11Service owner validation; native compiler/linker inputs and JNI declarations are unchanged. Three JNI payloads are taken from verified accepted1493; all seven ELF entries match stable v2.1.4 in the final APK; all124 assets match accepted1493. This is byte identity evidence, not a new GPU/runtime-device test.

Accepted1493 and standby1495 remain unchanged. The explicit1494 exception is recorded in [RELEASING](RELEASING.md#версия-и-совместимость). The standby is an APK, not a copy of application data; future rollback data compatibility is not promised. README.md and RELEASING.md user changes are preserved.

## Final artifact and evidence

- File: `Termux-Jobs-v2.1.5-test2-arm64.apk`, **40562642 bytes**, code1494/name2.1.5-test2.
- SHA-256: `e719a57dd531a4ddf0c23b22f586ce10366e2e0385bae57a89b318a1a10e6a9c`. Copy in `/sdcard/Download/` has the same hash.
- Package `com.termux`, ARM64-only, non-debuggable, min26/target28; signature v2 verifies against the unchanged certificate `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP CRC and outer-entry consistency pass; no stale outer local headers. Runtime/DEX audit finds all required definitions;31 optional/platform references retain explicit review notes. This is a static APK runtime audit, not execution on Android.
- Four common RU/EN templates match the supplied ZIP, repository source, stable and final APK byte-for-byte. The project template is unchanged. All124 assets match accepted1493. All seven ELF entries match stable.
- 581 production text inputs match JVM/Lint/APK stages; the broader final production-source inventory matches599 source files. Signer and X11 packaging guards executed. No synthetic test fixtures, credentials, signing files or new authorization data were packaged. README/RELEASING hashes remain unchanged.
- `module-repeat`:154 terminal-emulator tests executed and passed,0 errors/skips. terminal-view/termux-shared have no standalone tests (NO-SOURCE, not PASS); app integration tests cover TerminalView. This fresh repeat supersedes reliance on those154 UP-TO-DATE results.

Evidence: `.verification/test-2.1.5-test2/` contains artifact/runtime/template identity, signature, production-stage identity, final source hashes, native inventory, build/Lint logs and SHA256SUMS. `.verification/redesign-v35/` retains initial failures and all targeted/full/module retry manifests. One inventory checker initially compared refreshed inputs as a baseline; its expected-one-difference assertion failed. The checker now compares the backup inventory:95/95 current inputs match and94/95 match the baseline, with only Java owner validation changed. This was a checker error, not an app change.

No GitHub CI was run: push/publication are not authorized. No previously issued APK, tag, stable update.json or Components binary was replaced.

## Remaining phone checks and limitations

No new phone acceptance has occurred. The user's acceptance of1493 and standby1495 remains separate. An Android emulator/render backend is not available in the local ARM64 environment; Robolectric4.4 layout checks do not supply trustworthy screenshots or actual typography. No HTML illustration is presented as a native render.

1. Open the drawer; tap a title, then explicit Open. Check base protection, saved/live profiles, outside/Back/swipe dismissal and independent session menus.
2. Check each of four agents, the ordinary terminal and the agent's terminal: drafts, Stop, queue, model/reasoning, limits/context, previous pages, timeline details and local file links. New output should follow only while reading at the bottom.
3. Check both composers with keyboard open, narrow width and50/100/150% text; rotate and return. Confirm the last line is above the composer and older content can move behind it.
4. Check X11 height below the header, landscape buttons/input and return without restarting. Shared desktop/settings must disclose environment-wide scope. Do not test a stop on a desktop needed by another session.
5. Check libraries, settings and create-chat cancellation; connection command stays last. Check speech cancellation/unavailability and draft-only insertion if a speech provider is available.
6. Observe long terminal output and agent text, Unicode, code links and reduced animations. Real frame pacing/memory, Surface timing and physical gestures remain device checks.

Existing audit limitations remain: low-level Android Gemini handoff/crash recovery and device backup export/restore were excluded because there is no separate Android instance. Real Gemini API/Vertex and real Claude subscription coverage remain as recorded in prior reports. Claude's historical user clarification case without message UUID is not retrospectively diagnosed. Image accumulation/large external request disconnects are not fixed by this redesign. MCP/plugins and CLI forks/experimental/automatic compaction are excluded. Working history/authentication are unchanged.
