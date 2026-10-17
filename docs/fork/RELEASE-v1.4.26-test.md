# v1.4.26-test — local engine locations (testing)

Session and launch-preset settings have Environment with two independent dark cards:
- Tools: Main Termux or dedicated Local-AI Ubuntu.
- Engine: Embedded Android (existing/default), native Termux process, or Local-AI Ubuntu process (llama.cpp only).

The same app-owned jobs-local-ai Ubuntu serves both selectors. Selecting it for either purpose offers the existing confirmed setup if needed. No second Ubuntu copy or working-Ubuntu fallback. Engine location does not grant permissions or change the selected tool environment/project. Existing profiles default to Android; model switching to LiteRT resets external location to Android. Location changes on an idle loaded session reload the model and retain chat/history; computation cache starts fresh.

Runners are bundled in APK, built from llama.cpp b9222, with compatible and guarded ARM builds. Termux is a separate Bionic executable; Ubuntu uses a statically linked GNU/Linux executable through proot-distro login --isolated to jobs-local-ai, binding only the engine/model directories for inference. The same GGUF is shared, not copied for each location. No HTTP endpoint, network listener, API key or external download for the engine itself. Optional Ubuntu setup/tools may require network as before.

All variants use CPU, current thread count,256/128 batch/ubatch, same prompt tokenizer, sampler, prefix cache and benchmark implementation. This is an experiment: compiler/libc/process/PRoot differences mean results do not isolate environment alone. No speed advantage promised. Native runners support cancellation by request ID, EOF/quit shutdown and parent-death termination. No broad kill/pkill commands or user terminal session changes.

Includes v1.4.25 compact list context, newest-file limit/filter guidance and clearer draft-vs-approval status. Full tool results/history retained. Applies to local llama/LiteRT shared list handling; external CLI agents unchanged. LiteRT remains in Android; CPU location experiment is llama-only.

Compatible update: com.termux/versionCode1132/existing certificate; optional v8 localLocation defaults android, no data reset. Not published to GitHub. Actual installed-APK startup/proot-distro wrapper, keyboard and Qwen timings require developer verification; native isolated proot and small-GGUF checks are separate evidence.
