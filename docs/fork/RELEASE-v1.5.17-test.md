# v1.5.17-test — local engine cache checkpoints and LiteRT conversation reuse

Private test build, versionCode1154. No GitHub publication.

## llama.cpp

JNI and Termux/Ubuntu CPU runners receive an explicit final app-feedback position. A conservative checkpoint is computed from the exact token prefix shared by the real prompt and a probe changing only that final role. It replaces the near-end snapshot when it lies beyond the system checkpoint. Later reuse still requires identical owner/chat/system and exact token-prefix matching; snapshot budget, cancellation and failure invalidation remain unchanged. No substring matching, prompt truncation or native tool execution is introduced.

The change targets repeated processing of completed tool history on hybrid/recurrent models. It also applies to other compatible llama models/locations; actual gain depends on prompt/cache layout. Model weights and quantization are unchanged. Bundled JNI signatures changed together with all six CPU JNI/runner binaries; update the APK as a whole rather than mixing old libraries.

## CPU input processing

Existing diagnostic tests already cover lengths, threads and batch256/128 versus512/256. Developer report525790314148916479 showed overlapping results on Qwen CPU6/Ubuntu (256/128:25.77/28.14s;512/256:25.39/27.20s for2048tokens). Baseline256/128 and the selected thread count are preserved; no automatic switch to8threads or larger batches. Normal per-pass diagnostics now show the actual configured thread/batch/ubatch values. No new duplicate benchmark menu. This build claims reduced repeat work, not demonstrated higher cold-prefill throughput.

## LiteRT-LM adapter

Enable the installed SDK's experimental benchmark collection in the isolated inference process before engine initialization. If supported, report SDK prefill/decode token counts/rates and current KV token count. Unsupported readings do not fail inference; unavailable remains explicit, never reported as zero cache hits. Reusing a Conversation is identified separately from exact cached-token counts, which this SDK does not provide here.

For owned, non-media chats, keep a live SDK Conversation only for an exact semantic append of one new message with unchanged owner/chat, mandatory system/rights, schemas, protocol, sampler and response settings. App progress/correction is merged into the new incoming user/tool-result content as clearly labeled application feedback. Original tool-result text and structured role are preserved, automatic SDK tool execution stays disabled, and all prior runtime feedback remains counted against context. Feedback is never written into saved history. Imported history cannot forge internal cache annotations.

Recreate after changed/non-prefix or fitted history, changed rights/tools/chat/settings, changed effective reply reserve, media inference, stop/cancellation, SDK errors or malformed native calls. Discard excess retained runtime feedback and re-fit the original canonical history when necessary. No cross-chat reuse and no background inference. A retained model/conversation consumes memory until normal stop/close; real NPU compatibility and speed require developer verification.

This is a patch to our adapter and native wrapper, not a change to Gemma/Qwen weights, a vendor Qualcomm runtime modification or a new upstream SDK version.

## Session dropdown

Restore v1.5.11-era dimensions/padding: popup up to340dp with screen bound, natural row height (48dp minimum),14sp text,13/8dp padding and12dp corners. Keep the current22dp vector icons, model subtitle and active-session highlight. Do not change the top current-session selector, Server + Agent, session/preset creation labels or long-tap actions.

## Compatibility and test

Same com.termux package and signing certificate; higher private code1154 for an update over1.5.16-test. No data reset, history deletion or model migration. Repeat Qwen locations/write/read with the same CPU ARM/6threads/Ubuntu settings and export diagnostics. Compare later prefix_snapshot reuse and input times; the first cold pass remains expected. Test a new Gemma native chat with two consecutive tool actions; inspect the new SDK section, final file, stop and new-chat behavior. Unsupported SDK metrics must not interrupt the chat. See VALIDATION-v1.5.17-test.md for actual automated evidence and device gaps.
