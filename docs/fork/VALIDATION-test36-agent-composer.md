# test36 — agent composer and Codex voice input

Date:2026-11-02. Final APK: `/sdcard/Download/Termux-Jobs-test36.apk`.

| Property | Verified value |
| --- | --- |
| Package | `com.termux` |
| versionName / versionCode | `2.1.1-test36` / **1471** |
| Bytes | **40,438,421** |
| SHA-256 | `51617d287cb45f7fab326cd6d7a281fc3874c963c0fcf809b4cf18e328015086` |
| ABI / debug | ARM64 only; ELF64 AArch64 verified / nondebuggable |
| Signature | v2 verified; previous certificate retained |
| Certificate SHA-256 | `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644` |

VersionCode exceeds test35/1470 and backup1451. **133 JVM checks passed**, **95 helper +8 infrastructure checks passed**, full modern Lint: **0 errors, 234 warnings**. All **117 assets** match source/pinned cache. Runtime: **14178 defined classes**, requiredMissing empty; reviewed optional-type handling remains. Voice/control classes, vector icons and packaged microphone permission/query/optional hardware are verified. Checksum sidecar: `/sdcard/Download/Termux-Jobs-test36.apk.sha256`.

## Included behavior

- Remove the separate Command without model field from agent chats and return that height to the transcript. No empty replacement row remains. The existing star and actual attachment-picker action share a bottom outline, with separate48dp targets, stronger press feedback and a vector paperclip.
- Codex alone has a filled microphone glyph on a transparent48dp target floating at the lower right above Send. It has no permanent frame/panel and adds no horizontal input row. The56dp scroll-content clearance lets the final message move fully above it and stays fixed across50–150% chat zoom. Attachment rows leave60dp at the right for their removal action. Other chats have no microphone or clearance placeholder. Normal successful dictation clears its status row.
- Explicit Android speech recognition requires a microphone grant when absent. Service startup/listening/processing and human-readable errors are separate from model state. Partial hypotheses replace a preview; only the final text is inserted once into the draft. Existing text/selection/manual edits survive; nothing is sent automatically. Generation/source checks, repeated-tap cancellation, chat change, hide, manual send and close reject stale callbacks and destroy the recognizer.
- The inspected upper Interactive input (`⌖`, left of extra keys) previously toggled TerminalView input and the former shell field. It is disabled/dimmed while agent chat owns the composer, with a callback guard. Native-agent terminal access, ordinary terminal/desktop input and stop confirmation remain.
- Slash-message work is explicitly deferred: the Codex policy/transport asset matches test35. Retain previous instruction/skill managers, session-bound access, request summaries/dots, agent glyphs, local file viewing and Ubuntu AI preparation. No helper experiment or robot mockup returns.

## Checked anew

-133 JVM cases rerun together on the final version-bumped source. Coverage includes speech permission/service absence and errors, partial/final/duplicate/late callbacks, cancellation/context/close/manual edits, RU/EN; four agent headers and terminal restoration; separate star/paperclip actions, fixed targets and large-font/narrow layout; microphone overlay without composer-height growth; last-message and attachment-removal clearance across50–150%; draft focus/streaming/history, attachments, existing keyboard and desktop-header behavior. These use fake backends/platforms and do not record audio, invoke working agents or inspect authorization files.
-95 Python helper tests plus8 Lint-report infrastructure checks rerun. They validate unchanged instruction/Claude/skills fixture storage, safe import/creation, history and write conflicts; they do not represent microphone or external-network acceptance.
- Full modern Lint7.4.2 across all five modules, chosen conservatively for the shared Activity/controller/manifest changes. Compilation is checked separately. Existing AGP/compileSDK and Kotlin-metadata diagnostics remain in build logs; no new blanket suppression or baseline.
- Exact delivered APK: ZIP integrity, package/version growth, ARM64/nondebuggable, v2 signature/previous certificate, SHA-256, runtime/DEX and source/pinned-cache asset equality. Verify new voice/control classes, both vector resources and actual ELF64 AArch64 headers. Check the packaged RECORD_AUDIO permission, optional microphone hardware and speech-service visibility query. Final Download bytes match the Gradle output, and the sidecar verifies that same file.
- Review: voice inserts only into the captured current draft and never invokes send/CLI; no system-wide permission request on ordinary startup. Header/desktop guards preserve the original routes; controller lifecycles cancel speech. The field is reused rather than recreated, and only that field scales, preserving action targets. No persistent data format, source-agent binding or stop-ownership change.

## Reused with identity proof

Existing local SDK/JDK/Gradle/dependency caches are reused without full copies. JNI/native/X11 source inputs match test35, and app build configuration matches after normalizing only versionName/versionCode. The two Termux libraries are restored from its verified signed APK; all three final native payloads match it byte-for-byte. This is packaging reuse, not a fresh native compile or graphics run.

Unchanged transport, environment/component recipes, manager storage and animation inputs are inventoried against test35. Their previous functional evidence keeps its original limits. No guest installation, model request, working-session restart or X11/driver launch is required for these scoped UI changes. The instruction/skills helper folder is instructions-1471. User instruction files are independent of that folder and are not replaced.

## Remaining phone checks

Actual Android microphone permission/provider behavior, recognition quality/language, IME resize, taps/ripple visibility and narrow/large-font appearance require device acceptance. Model tests do not prove them. The system speech provider may need a network service; offline recognition is not promised. No recording has been performed by these build checks. Package/signer compatibility is verified, but this is not an exhaustive device data-migration test. No publication or automatic migration is included.

The previously reported Claude success on another phone with the GitHub Ubuntu AI image remains attributed user evidence, not acceptance of test36 or proof of the former Docker Hub failure's cause.

## Evidence

`.verification/agent-composer-test36/`: scope, source/config/native identities and delta, stage/build/JVM/Python/Lint logs and summaries, final artifact comparison, badging, manifest tree, signature, DEX/runtime and checksum records. The [preceding source checks](VALIDATION-agent-composer.md) remain historical and retain their initial overlay failures. No product behavior beyond the agreed composer work was introduced during packaging.

The first additional vector audit assumed source filenames survived APK packaging. The existing resource optimizer shortens paths; the corrected audit resolves drawable names through resources.arsc, verifies their ZIP entries and parses both compiled XML files as vector drawables. The APK was unchanged and was not rebuilt for this audit correction.
