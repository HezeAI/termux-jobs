# Validation — v1.4.19-test / code1125

Changed mechanism: guarded alternative native llama CPU backend plus local engine
selector, persistence, IPC and safe idle reload. Model files/weights are unchanged.

Real tiny GGUF checks pass for both variants: fresh/warm next-token logits, forced
snapshot restoration, scope/instruction reset, shorter/mismatched input, snapshot
budget, cancellation invalidation and disabled scope. A separate real-model decode
check reports baseline dotprod/fp16/i8mm=0 and optimized=1, with identical greedy
token sequences across three runs in each mode. Host workload/thermal conditions
were not controlled; these timings are not Qwen speed evidence.

Actual Android-native dlopen test passes: baseline hardware probe returns true on
this host, both libraries load, JNI namespaces differ, internal llama symbols are
hidden. Policy assertions cover absent required hardware flags and missing i8mm.
No unsupported-instruction backend is loaded on a negative capability result.
Actual old-device fallback and Java/JNI model generation remain device checks.

Java coverage includes optional old-profile default, strict enum/type validation,
persistence/copy, independent engine selection, disabled ARM selector for LiteRT
and existing busy/cancel/reload/history paths. A final targeted UI/limits run covers
the explanatory hint placement after the full suite.

Review: hardware check runs in the generic baseline library before fast loading;
load/tokenize/generate/free all select the same owning library. Both backends hide
internal symbols; no handle or sampler state crosses variants. Missing optimized
library falls back at load time; arbitrary native crashes are not recoverable in
this way. Private service validates mode, close clears variant state; reload keeps
chat, preserves ownership and retains stop/panel guards. No external agent adapter,
Ubuntu, shell permissions, process-control or dependency-installation changes.

Final evidence: .verification/local-ai-v1.4.19. Tiny-model/probe checks do not prove
Qwen acceleration, visual Android UI, keyboard/lifecycle or older-device behavior.
For Qwen compare same cold prompt in new chats with fixed model/context/threads,
first Compatible CPU then Automatic; verify actual backend in exported diagnostics.

Final:635 Java tests passed,0 failures/errors/skips;50 targeted final UI/limits tests
also passed after the hint-placement adjustment. ARM64 APK build and runtime gate
passed (15899 definitions,30 reviewed unresolved types). Existing Kotlin metadata/
build-tool warnings remain; no warning-free Lint claim. Signature, com.termux,
1.4.19-test/code1125/minSdk26/ARM64, CRC/paths/duplicates, source-stage match and
limited secret scan passed. Both stripped CPU libraries match packaged payload;
other native payload and external-agent assets match1.4.18-test. Hardware guard
includes RDM/CRC32 in addition to dotprod, scalar/vector FP16, atomics and i8mm.
Final APK SHA-256: aae37ac694f54e387d78010ea41d7ff7cbe2f41cb9c1ab66f0085e2f06a7957b
