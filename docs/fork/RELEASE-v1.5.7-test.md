# v1.5.7-test — private sampled-video analysis

Package com.termux, versionCode1144, ARM64, Android8+. Existing signing identity and user data retained; no settings/library migration or model-file changes. Not published to GitHub. Earlier1.5.6-test APK retained in Download.

view_video keeps the ordinary user request -> tool -> result -> final chat response chain. After the existing authorized file/frame preparation, a separate model conversation uses only the current video question and timestamped sampled frames. It has no tool schemas or prior chat. Normal chat schemas/cache scope are restored in finally after success, error and cancellation. No command or other tool is executed by this sub-conversation. Images, audio and other tool paths remain unchanged.

First attempt submits all prepared frames together. If attachment counts show omissions, or input context fails, the joint description is discarded and each prepared frame is analyzed separately. Each single-frame pass must confirm one attachment and produce a text response; otherwise analysis fails explicitly. Counts mean confirmed submission plus completed inference, not proof of visual understanding. The main chat receives ordered textual observations, not image attachments, and continues with its original task, permissions, history and tools.

Progress shows joint-frame analysis or frame N of M. Diagnostic passes distinguish joint/single-frame analysis from main chat and retain attachment counts. Current tool history stores full internal descriptions; a compact, explicitly excerpted result is passed to the main model. A protected current video-result flag avoids silently cropping these compact observations; reply reserve can adapt or the request fails if it cannot fit.

Only vision-capable runtimes can enter this path. Current llama.cpp build remains text-only without multimodal projector support; importing a vision GGUF does not activate video. Current verified Gemma LiteRT can be tested. Video is sampled frames without audio or continuous motion; requested sample times may not equal exact decoded keyframe times. Observations/comparison can be inaccurate and excerpts may omit detail. No hardware success claim until developer tests on the device.

Device check: repeat the same video request in a new Gemma chat and save diagnostics. Inspect submitted counts and inference purpose. Then ask an ordinary file-reading question in that chat to verify normal tools continue to work. Test cancellation through existing Stop/Start behavior separately.
