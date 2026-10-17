# v1.5.8-test — task continuity and compact context notices

Private ARM64 test build, com.termux, versionCode1145, Android8+. Existing signing identity; no history/settings/library migration or automatic data reset. No GitHub publication. Previous private APK retained.

When context shortening is necessary, a bounded excerpt of the preceding user request, assistant answer, recent tool target/status and available saved summary is carried alongside the unchanged current user request only after previous turns were actually dropped and if the retained request/results plus that excerpt fit. A complete recent exchange is never displaced to make room for its own excerpt. It is explicitly untrusted historical evidence, never permissions, a new tool action or proof that a file still exists. The full saved transcript is untouched. No extra model inference or semantic-history search is added. Excerpts can omit details and do not guarantee model recall or correct interpretation. When there is no room, ordinary compaction/error handling remains; a too-large current request is never silently cut.

The history tool accepts explicit null for its optional query/start/limit as an omitted field and uses existing defaults. Unknown fields, invalid types, limits outside1..10 and required null arguments remain rejected. Existing current-chat scope and access checks are unchanged; semantic retrieval, recall policy and other tool schemas have not been expanded.

Summary, earlier-result excerpts, reply-reserve adjustment and context-shortening notices share one persisted expandable Context and history card per user turn. Repeated updates replace the same status within the card. It has a scrollable detail window and is excluded from model input, like earlier context events. Existing historical notice records remain readable. Tool progress, approval/error cards and loop warnings remain separate.

Applies to shared local-AI conversation/context code used by llama.cpp and LiteRT. Video sub-analysis from1.5.7 is retained. No model, native runtime or system environment was changed. Real model recall, card rendering/navigation, keyboard and cancellation still require device checks.
