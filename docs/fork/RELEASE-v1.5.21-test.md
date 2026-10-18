# v1.5.21-test — return to pre-LoRA local AI

Private ARM64 com.termux test build, code1158, minSDK26. Restores the production app/native sources from v1.5.19-test checkpoint6a9ab547. The newer version/code permit updating over v1.5.20-test without an Android downgrade or data reset. No GitHub publication.

LoRA selection/import, optional field handling, IPC attachment and JNI/runner adapter commands from v1.5.20-test are removed. All six CPU JNI/runner payloads are restored byte-for-byte from the verified v1.5.19-test APK. Existing pre-LoRA local-engine improvements remain: ARM acceleration/thread selection, locations, protocols/tools, history/cache/diagnostics and bounded completion checks. Base model files are unchanged. This restores the older behavior; it does not claim to fix the developer-observed model's incorrect negative statement about a successful file write.

Session/preset/history schema stays8; the removed optional localLora property is ignored when reading a newer profile and disappears on next serialization. No history or model files are erased. Session identity, chat reference, model, rights, selected location and thread count are covered by a compatibility test. These checks do not prove every installed-app interaction or arbitrary external data compatibility.

The owned training-checkpoint downloader was stopped at the developer's request. No LoRA training steps had started and no trained adapter was produced/installed. Isolated preparation files and partial downloads stay under ignored .tools/.verification for now; they are not APK payloads. No user process, Bridge, installed environment, shared SDK or server service changed. Historical v1.5.20 reports remain marked superseded; experimental source is preserved in local Git history.

Final artifact/tests: [validation](VALIDATION-v1.5.21-test.md). After updating, check local session settings have no LoRA entry, existing chat/preset opens, and Qwen/Gemma still run with the saved environment/rights. Device behavior is a developer check, separate from automated tests.
