# Validation — v1.5.10-test

Evidence: `.verification/local-ai-v1510/`. Relevant JVM tests cover unchanged baseline schema constraints/availability across24 configurations (excluding description text), exact shared descriptions reaching both native schema sets including Tools only, custom behavior preservation, permissions, native parsing, context/history and video. A real SDK interface probe additionally inspects parsed write/command descriptions, using fake inference and no hardware execution.

One-off synthetic prompt sizing uses identical settings to1.5.9; estimates include native schemas but omit history/media/progress addenda. Description expansion intentionally costs context; it does not prove improved model routing. The one-off helper is retained only in the evidence directory after the run.

Final90 relevant JVM cases passed, including one-off sizing, 24-configuration schema fingerprints, descriptions in both native schema sets, parsing/permission/context/video regressions. Initial run89/90 passed; an existing feedback test required the explicit phrase `Execute Bash`, retained in the command description without changing its meaning. Final14 interface tests with real SDK0.17.0 and fake inference passed, including inspection of the SDK-parsed write/command and parameter descriptions. Both builds succeeded; final log is build-final.log. Models, hardware inference and installed apps were not used by these checks. No source/unit/SDK test constitutes a real NPU model, Android input or filesystem-side-effect test.

Synthetic estimated service input1.5.9 ->1.5.10 (full access, example project, same schemas/settings):

| Protocol | Standard | Brief | Tools only |
|---|---:|---:|---:|
| json | 2245 -> 2272 | 2000 -> 2395 | 1746 -> 2140 |
| qwen-native | 2542 -> 2936 | 2157 -> 2551 | 1902 -> 2297 |
| litert-native | 2223 -> 2629 | 2155 -> 2560 | 1927 -> 2332 |

LiteRT descriptors add about405 estimated tokens, traded for explicit task-selection guidance. This is not a speed or model-quality improvement claim. Actual clean-chat developer report4828324737620970227 from1.5.9 had no previous history/compaction,91 estimated current-user tokens, and still selected command for a guide; stale history was not necessary for that failure. New model-routing behavior remains unverified until developer testing.

Final APK 108691324bytes, SHA-256 `287b4643ed501816e5ecd487ca0aa2ab95c9ce956ee8f9426c4a3d2891f8771d`. ZIP CRC/path/duplicate checks, com.termux/1147/1.5.10-test, minSDK26, ARM64, private feature flag, v2 signature and matching existing certificate verified. Limited secret filename/marker scan passes; existing native payloads and controller/server-agent assets match baseline. Download copy independently hashed. Full schema reference exported from final compiled classes.
