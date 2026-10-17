# v1.4.19-test / code1125

Local llama.cpp settings in sessions and launch presets add Engine: Automatic or
Compatible CPU. Automatic checks actual ARM64 hardware capabilities before loading
a separately compiled dotprod/FP16/i8mm backend; missing required features retain
the baseline CPU. A missing/unloadable optimized library also falls back at the
library-loading step. Compatible CPU deliberately uses the baseline. GPU/NPU are
not added by this setting. LiteRT-LM displays its own engine without ARM-variant
choices and retains existing acceleration selection.

The selected mode is an optional v8 profile field (localCpu, default auto), copied
and saved with sessions/presets and passed through the private IPC. Service validates
the mode. Busy settings changes remain blocked. Changing engine mode on an idle
loaded session reloads only its owned local inference resource with the same chat;
existing cancellation/panel checks prevent restart after Stop or close. No history
reset, tools, rights or Ubuntu changes.

Two native libraries use separate JNI method namespaces. Their internal llama/ggml
symbols are hidden to prevent mixing CPU backends. Every load, tokenize, generate
and free operation uses the library that owns that model handle. Baseline hardware
probe checks dotprod, scalar/vector FP16, atomics/RDM/CRC32 and i8mm before optimized loading.
The optimized compute code targets armv8.2-a+dotprod+fp16+i8mm; baseline remains generic.
A load-time fallback is not a promise to recover from a native process crash.

Diagnostics report actual CPU variant and configured thread count. Existing4-thread
limit, input batches, context/reply limits and prefix cache policy are unchanged,
so this experiment isolates the instruction-set build. The GGUF model is neither
retrained nor modified. Model understanding/tool selection still depends on the
model and correct context/instructions; faster arithmetic is not better reasoning.

Same package/certificate, code1125 above1124, no data/schema reset. Extra native
library increases APK/storage size. Small real-model tests validate execution and
cache safety, not Qwen2B throughput. Compare identical cold requests in new chats
between Compatible CPU and Automatic, keeping model/context/settings constant;
export both diagnostics and verify actual variant. Native inference runs in the
Android service, not in Ubuntu/proot. No GitHub publication.
