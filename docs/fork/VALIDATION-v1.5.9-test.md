# Validation — v1.5.9-test

Evidence directory: `.verification/local-ai-v159-optimization/`.

Baseline was recorded from1.5.8 source/compiled classes before production changes. Synthetic prompt measurements cover JSON, Qwen native and LiteRT native, each with Standard/Brief/Tools only, identical full-access settings and no user data. LiteRT numbers include native schemas; memory/progress/media/history addenda are omitted identically on both sides. Counts use LocalAiContext.estimate, not exact model tokens. No real-model speed claim follows from this comparison.

Schema fingerprints cover24 baseline combinations of chat/read/write, project-only, confirmations, and additional Termux commands. Only description strings are ignored: all names/order, fields, argument types, required lists, bounds, enums and additionalProperties remain part of the comparison. Application authorization/parser/runner paths are unchanged.

Broad run:627 app cases (626 passed; one wording assertion expected the previously explicit `NEVER deletes`). That wording was retained in the shared write description. Final99 relevant cases passed (including a one-off size-measurement case);145 emulator cases passed/cached in the broad run. Final13 interface tests with real SDK0.17.0 and fake inference passed. No real CPU/NPU model was invoked. The one-off size helper is retained under the evidence directory rather than as an ordinary repository test with an absolute report path. Final production source matches the build stage. See full-suite-summary.json, targeted-summary.json, targeted-final.log, sdk-final.log and build-final.log. Device-specific checks in RELEASE-v1.5.9-test.md remain pending. Passing JVM tests does not establish model understanding, rendering/keyboard/gestures or real throughput.

Synthetic estimated service input (before -> after), full access, project-only OFF, confirmations OFF, extra Termux ON, example project:

| Protocol | Standard | Brief | Tools only |
|---|---:|---:|---:|
| JSON | 2531 -> 2245 | 2165 -> 2000 | 1929 -> 1746 |
| Qwen native | 3383 -> 2542 | 2420 -> 2157 | 2184 -> 1902 |
| LiteRT native | 2533 -> 2223 | 2175 -> 2155 | 1965 -> 1927 |

Brief/Tools only LiteRT were already compact, so their savings are small. Complete schemas were not pruned to manufacture a lower count; new deliverable/language/failure guidance is included in Standard/Brief. These are not exact model tokenizer counts or real request timings. See prompt-size-comparison.json.

Final APK: 108689671bytes, SHA-256 `216ee9d139afd16adbff745fe7fcbdfbb841bf4e879f2aa9d07c065903623913`. ZIP CRC/path/duplicate checks pass; com.termux/1146/1.5.9-test, minSDK26, ARM64 only, private Local AI enabled. Existing certificate matches and v2 signature verifies. Limited secret filename/marker scan passes; existing native payloads and controller/server-agent assets match baseline. No model is bundled or modified. Download copy is independently hashed.
