# v1.2.1 опубликована — далее дебаг-меню

Latest https://github.com/HezeAI/termux-jobs/releases/tag/v1.2.1
Tag source f14ebe02. Code1072/com.termux, прежняя подпись, без сброса.
APK build/apk и /sdcard/Download/Termux-Jobs-v1.2.1-arm64.apk
SHA256 a546c80a8660b0585b964150c4f471280b1d19e7be4f93d074262eaa2801cbaa
APK/SHA256SUMS/update.json downloaded back and matched, public Latest/unauth
metadata checked.388JVM tests +source/build/ZIP/signature audit verified.
User/developer HezeAI confirmed4–5x upload acceleration on his connection;
release notes qualify network-dependent gain. Agent/native unchanged.
SFTP8x32KiB single channel; helper pool2+2queue, private monitor SSH, foreground
shared operations serial, late poll guarded by manualEpoch/generation.
No change to server-side task queue or download transport. See RELEASE/VALIDATION/
PUBLICATION-v1.2.1 and .verification/servers-v1.2.1 benchmark.

Next agreed topic: debug menu and informative session/server diagnostics.
Screenshot_20261016_030335.jpg is ordinary Codex session diagnostic, not server
monitoring. Visible COLLECTOR/START and running state, no error shown.
WorkspaceDiagnostics.report hardcodes diagnostics v1.0.18 (header label, not app
version). Proposed next: real app version separated from report format; elapsed/
restart/exit/error details, collection state and last event; separate server
queue wait/SSH duration/response bytes/transfer metrics without secrets/commands/
paths/transcripts. Idle events do not establish hung process. Discuss design
before new implementation; do not fold untested diagnostics into speed release.

HezeAI is developer, Codex assistant. Do not add claims developer did not install
APK. Preserve workspace boundaries; no APK install/process or server changes.
Future GitHub releases require README/docs and schema1 RU/EN compatibility.
Design/ untracked predates task, leave untouched. Published tags/APKs immutable.
