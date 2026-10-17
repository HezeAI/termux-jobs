# v1.4.9-test (1115)

Portable screenshot discovery for all local AI models. The shared read-only `screenshots` tool probes common Android storage aliases and Pictures/Screenshots, DCIM/Screenshots, Screenshots and ScreenShots variants. It merges supported image extensions, orders by modification time and suppresses duplicate device/inode records. An explicit alternate directory can be selected with path; query and limit are supported. Scanning is bounded to the screenshot directory and one child level, not an exhaustive scan of device storage. Actual storage permissions and selected environment mounts still apply.

Short unambiguous requests such as “3 последних скрина” start the normal recorded tool flow immediately. Models receive real paths and per-folder statuses; exact filename requests must not be replaced by a recent unrelated file. Text-only models can locate images but do not gain vision. Android codec support for image formats remains device dependent. Codex/Claude/Gemini/OpenCode CLI tools are unchanged.

Project scope, action permissions and selected local environment remain authoritative. No package installation or automatic permission grant is added. No schema migration or data reset. Same com.termux package and certificate as prior tests; versionCode increases to 1115. Device checks pending: Android storage permission denial, real aliases in dedicated Ubuntu and model follow-up image inspection.

Local-AI settings selector cards now match ordinary session buttons: dark panel, neutral border/text, same typography and touch ripple. Other local-chat action/approval cards keep their current styling.
