# v1.4.6-test validation

518 JVM/Robolectric tests passed, zero failures/errors/skips. Added coverage for active saved-session rename and empty-name validation, keeping loaded model/history when updating action/scope/approval settings, typing in a shown local chat and another local tab during stream view updates, changed permissions on later turns with existing history, a single correction of an obsolete cloud-only denial, selected project/environment mapping, shared-storage path detection and conditional Delete preset visibility.

The initial UI tests clicked before the queued dialog OnShow listener ran; idling the test looper before interaction resolved those fixture failures. The final suite passed. Source review traced name persistence/header refresh, next-turn permission copies, both current-system transports, llama context clearing and LiteRT conversation recreation. App authorization/approval/scope gates remain unchanged. A changed permission does not restart the model; changing model/backend does. Settings cannot change during inference.

Streaming fullScroll calls were replaced by visibility-gated scrollTo, which does not request focus. Hidden panels skip stream view updates; ordinary terminal headers no longer react to local state changes. Pending full-history scroll callbacks also check visibility. Manual file browsing uses an independent profile in the selected local environment and does not elevate AI permissions.

Final isolated ARM64 build passed. Runtime definition gate: 15831 defined classes, 30 explicitly reviewed unresolved types. ZIP CRC, com.termux, version1.4.6-test/code1112, minSdk26, ARM64-only ABI, original signing certificate, source/stage match and limited private-file/secret scan verified. Native libraries and server-agent asset unchanged from1.4.5-test. Legacy Lint Kotlin metadata diagnostics remain; no clean Lint claim.

Device validation remains: real IME/focus/scroll and drafts with simultaneous model inference and ordinary terminal/Codex/SSH tabs; shared-storage grants and Termux/Ubuntu/other installed roots; model behavior after changing permissions in the same chat. Mocked UI/runner tests do not establish these device outcomes or actual model compliance. No user processes, installed app, shared SDK or system environment changed; no GitHub publication or data reset.

APK SHA-256: `d491d2712111618ec05ea93b3c969df4f91d6b955ce3ef5d780570ed4a4bf7d0`
