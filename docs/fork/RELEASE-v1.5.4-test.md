# v1.5.4-test — private verification build

Not published to GitHub. Package com.termux, versionCode 1141, ARM64, Android 8+. Existing signing identity retained; no automatic data reset or schema migration introduced. Installation remains a developer action.

- LiteRT native invalid arguments are returned to the application coordinator for validation feedback and repair, without executing rejected requests. Repair instructions use the native SDK format.
- LiteRT engine settings expose communication format; llama CPU/threads controls remain hidden. Compiled NPU context remains 4096; no model patch applied.
- Compact current-chat memory hints and one automatic current-chat history lookup before a memory-denial response. Historical paths do not grant permissions or trigger inferred deletion.
- Failed repair drafts are hidden from the chat view; saved records remain available.
- Environment choices in session/preset creation use styled tiles again.
- Server connection toggle explicitly connects/disconnects application-owned transports. Disconnect invalidates delayed replies, cancels related local transfers/pickers/media and closes profile-owned transports. Independent SSH terminal sessions, remote agent/tasks and other server profiles are not stopped. Auto-refresh cadence remains 10 seconds inside the selected server, 30 seconds elsewhere while the app is foregrounded.

Developer checks needed: connect/disconnect/reconnect and delayed file pickers; native tool correction and current-chat memory retrieval; image, video-frame and first-five-second audio input on the actual pinned model. Mock/SDK interface tests do not prove hardware media inference.

Deferred to a future update: disabled GPU item for the prior model and fixed NPU mode tile for LiteRT. Context budget investigation and hardware media checks continue separately; no larger context advertised.
