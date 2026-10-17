# v1.4.7-test — local file tools, media and explicit package installation

Private test build (code1113). No GitHub publication.

## File discovery and reading

Local models share list/search metadata: real full paths, types, sizes, modification timestamps, newest-first or name sorting, filters (all/text/image/video/APK), limit1–200 and optional recursion. Search accepts an explicit directory plus query; older search requests with a filename fragment are retained. Directory metadata scans are bounded to2MiB; narrow large searches. No filename or user directory is silently corrected or invented.

`locations` probes common Android folders (Download, Pictures/Screenshots, DCIM, Documents, Movies, Music), project and Linux home candidates in the selected environment. It reports availability, lack of access and project-boundary exclusions; these are hints, not a restriction to those folders. Any explicitly supplied directory still uses the existing canonical scope checks. It does not grant Android storage access; the folder button offers the existing storage permission flow.

Text reading remains UTF-8 with a64KiB limit; NUL-containing binary input is rejected rather than presented as text. Read/list/media scripts distinguish missing files, directory/type errors, access denial, excess size and canonical project escapes. A question about capabilities no longer automatically triggers a failed-action retry; intent detection is heuristic, not a language-understanding guarantee.

## Images and video frames (experimental)

Pinned Gemma4 E2B/E4B LiteRT models advertise image support. `view_image` copies an explicitly permitted source from the selected environment into private temporary storage, decodes/resizes it and passes actual ImageFile content to LiteRT. `view_video` extracts up to three nearby keyframes with requested timestamps, duration/resolution metadata and an explicit no-audio/no-continuous-video notice. Maximum source sizes:32MiB image,256MiB video/APK. Prepared image longest edge1024px, at most three images/frames in the current runtime context. Small screenshot text can lose detail. Decoder/container support depends on Android. Source files are unchanged; temporary originals/frames are removed after the turn or failure, and old history never silently reattaches expired images.

Vision initializes lazily using LiteRT GPU vision, even if the text decoder is CPU. On initialization failure, text restoration is attempted and an error is shown. Actual GPU/model/decoder behavior remains a device test. Qwen llama.cpp has no multimodal projector in this build, so it remains text-only and returns an explicit unsupported response. Imported models are not automatically declared image-capable. Qualcomm NPU support remains unavailable as previously documented. No network media uploads, audio recognition or continuous video analysis were added.

Primary references: [LiteRT-LM Android multimodality](https://developers.google.com/edge/litert-lm/android#multi-modality), pinned [Gemma E2B model card](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1/README.md), [Gemma E4B model card](https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/blob/2eee7ac325f20eb8c9ac1d0e972f7c84663062da/README.md), [llama.cpp multimodal projector requirements](https://github.com/ggml-org/llama.cpp/blob/master/tools/mtmd/README.md).

## APK metadata

`inspect_apk` is a shared read tool: ZIP entries/ABIs, Android package/version/SDK/permission metadata and certificate fingerprints where Android can parse them. It does not install, execute, extract or decompile APK code, and is not independent cryptographic signature verification. Building still requires project sources and a prepared SDK/JDK/Gradle toolchain; no automatic full build-environment installer was added. Full-access shell commands remain available only with Path checks off.

## Missing-tool installation

With Confirm actions ON, read/full-access modes may request the fixed install tool for coreutils/findutils/file/unzip/zip/binutils/aapt/apksigner. The application first runs an APT simulation, shows its plan and selected environment in a chat card, and waits for Yes/No. A failed plan, rejection or cancellation before approval never invokes installation. Once an approved APT process starts, Stop cancels further AI work but does not kill the package transaction; it is allowed to finish. Downloads/lock acquisition have APT timeouts and installation is noninteractive. The app cannot guarantee recovery from Android forcibly killing it or external interruption of the package transaction. `--no-remove` is used; arbitrary package strings/shell syntax are rejected. This is an explicit manual package-install exception to Read/scope policy; it does not enable arbitrary commands or file writes by the model. Path checks ON still block arbitrary commands. Chat-only cannot install; Confirm actions OFF disables this install tool. Installing packages requires network/repositories as needed. This helper supports APT environments; other package managers require separate work.

Scope UI is now named **Work only within the project / Работать только в пределах проекта** and the mode menu **Local AI permissions / Права локального ИИ**. Stored settings/semantics are unchanged.

## Compatibility and device checks

Same com.termux package/signing identity, increased code, no data reset. New optional history media metadata is backward-readable; old app versions will not support new tools. No historical files are deleted, and app backup still does not include local models/chats as previously documented. Existing rollback1110 is below this1113 build; no new recovery requested.

Test real text discovery/reading in arbitrary allowed directories, last TXT/screenshot selection, no false visual claims in Qwen, Gemma image and video frames, unsupported media, permission/path boundaries and retained text use after a vision failure. Review/deny an installation plan first, then test a wanted package in a disposable/prepared environment. Device results are separate from automated fixtures.
