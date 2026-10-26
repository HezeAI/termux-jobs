# Published stable v2.0.9

Active source: /root/termux-fork/.verification/chat-file-cards/source.
Version2.0.9/code1417; Alias and per-session50–150% scale in all4 agent chats.
APK .verification/release-v2.0.9/Termux-Jobs-v2.0.9-arm64.apk,
39798069bytes, SHA25679e98ea329a52158798d420924f41bd14285b92c39841358891b9c5128de69cd.
Same package/signer/schema8/ARM64; no reset. X11/native assets unchanged.
228 JVM,44 Python,8 shell tests pass; modern lint0errors/220warnings.
Published https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.9
Immutable source tag81e9dad958652b31b7877e205e5c830e026a71fc. Latest stable,
not draft/prerelease. All4 assets downloaded and byte-compared; update.json matches
final APK. Public tagged docs/body verified. Feature-only English release notes,
RU/EN updater compatibility and Codex credit. See VALIDATION-v2.0.9.md.
Preserve immutable older app/component releases and root-worktree experiments.

---

Historical preview handoffs below; earlier publication-pending notes describe
those previews, not the authorized v2.0.9 release.

# Private alias/chat-scale test3 ready

Active source /root/termux-fork/.verification/chat-file-cards/source,
branch work/chat-file-cards. APK build/apk/Termux-Jobs-test3.apk,
2.0.9-test3/code1416, 39798181bytes,
SHA256f8555cfdc3dff84635039d1840698f4bb06cc0e1e83f7abeebdd4acc7d2efe9d.
Same com.termux/signer/schema8/ARM64, header v2.0; no reset/rootfs migration.
All3 JNI and X11 runtime assets unchanged. Final APK signature/runtime/assets/
privacy checks pass; compatible lint0errors/220warnings. Broad226 cases plus
final targeted114 pass;228 distinct cases,44 Python backup,8 shell cases.

Combined Import/export Alias menu: manual shell import, JSON/backup import,
single-environment shell export, JSON export. Literal-only file parser, explicit
selection/environment/preview/replacement; no automatic startup-file discovery.
Termux-only tab-restart hints retained. Agent status panels have per-session
50–150% chat scale/default100/reset, measured layout/text/cards/thumbnails/drafts/
Shell and message-input scaling; bounded shared composer restores for terminal/
switch/close. Old profiles default100. See AGENT-CHAT-SCALE.md, ALIASES.md and
VALIDATION-alias-test3.md. Files/tests only in repository fixtures. No actual
phone session, rootfs, tunnel, server, APK installation or shared SDK changed.

Deliver this preview in chat for device acceptance. No GitHub publish yet.
Next versionCode must exceed1416. Preserve stable app/component tags and the
unrelated root worktree rotation experiments; never run the quarantined fixture.

---

# Private alias test2 ready

Active source: /root/termux-fork/.verification/chat-file-cards/source,
branch work/chat-file-cards. Preview2.0.9-test2/code1415. APK:
build/apk/Termux-Jobs-test2.apk, 39792329bytes,
SHA256ba4eadb050af1e254fa8ed0555db05d3809f614804898c48c65c4b3f4d0473ec.
Same package/signer/schema8/ARM64, header v2.0; no reset. All3 JNI/X11 runtime
payloads unchanged from stable2.0.8.132 JVM cases pass,40 Python backup cases,
8 shell fixtures; compatible lint0errors/218warnings; artifact/runtime/privacy
checks pass. See docs/fork/VALIDATION-alias-test2.md and ALIASES.md.

Added explicit import/export cards, selection/full preview, confirmed per-env name
replacement, General fallback, batch compensation and persisted-record checks.
Only Termux shows tab-restart guidance in editor/deletion. No automatic discovery
of aliases in shell files. Ubuntu AI remains excluded. All helper/tests wrote only
private repository fixtures; no actual rootfs/session/server/tunnel/app changed.
Deliver in chat for phone verification. Do NOT publish until accepted and asked.
Next code must exceed1415. Preserve stable app/component releases and root worktree
rotation experiments. Do not run the quarantined native fixture.

---

# Private environment-alias test1 ready

Active checkout: /root/termux-fork/.verification/chat-file-cards/source,
branch work/chat-file-cards. Preview2.0.9-test1/code1414, same com.termux/signer/
schema8/ARM64; header v2.0. APK: build/apk/Termux-Jobs-test1.apk,
39785457bytes, SHA25669019c66dee5f4271f68cdb6b05b6b4af67353ec39a86e12dc5b16c8b47cee82.
98 JVM cases/7classes,8 isolated shell cases, compatible lint5modules0errors/
218warnings, APK/runtime/assets/privacy checks pass. No physical alias/startup
checks yet. See docs/fork/ALIASES.md and VALIDATION-alias-test1.md.

Implemented all requested alias library/star/category/favourite UI, persistence in
the selected installed environment, replacement/edit/delete and restart warning.
Targets: Termux,Ubuntu,Debian,Alpine,Arch,Fedora,openSUSE,Void. Ubuntu AI excluded
in both chooser and backend. Existing commands/notes survive; no running shell,
rootfs, tunnel, server or graphics payload was modified. Test helper writes went
only to private fixtures. Standard shell startup is supported; already loaded
aliases need a new shell after edits/deletion. Raw backup captures retain metadata,
but the existing selective-import UI does not yet offer alias import.

Deliver this preview in chat for developer checks. Do NOT publish until accepted
and explicitly requested. Next update code must exceed1414. Keep public v2.0.8
and all components immutable. Root worktree rotation experiments remain separate;
do not build, reset or discard them. Do not run the quarantined native fixture.

---

# Stable release: v2.0.8 — published and verified

Release: https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.8
Immutable tag/source commit: a22aa3bfcb64e9f2e5f19e5181c5186c7d357526.
Termux-Jobs-v2.0.8-arm64.apk: 39773192 bytes,
SHA2565dc7d350bf237a5c1eccd4487abd2d6814dc4cfde07c124cc1d64cee3773b952. Code1413/version2.0.8, same com.termux/signer/schema8/ARM64,
Android8+ app/Android11+ X11. Header v2.0 remains. No data reset/account/rootfs
migration is required; existing agent helpers may need a normal session restart.

The release adds Developer Markdown, generic downloadable file cards, saved file
sorting, long-press multi-selection, system gallery, scoped Stop with a reversed
VSync activity route targeting60FPS, and keyboard dismissal after acknowledged
message sends. The opening English note emphasizes easier CLI-agent work directly
in Termux on a phone. HezeAI authorized publication; credit ends every release:
AI development assistance: OpenAI Codex.

109 relevant JVM cases/16classes, eight Node suites and45 scoped Python X11
regressions pass. Modern lint:5modules,0errors/217warnings, no infrastructure
failure. The signed APK passes package/version/ABI/certificate/ZIP/assets/runtime/
secret checks. Native and asset payloads match test2; X11/renderer sources and
component tags are unchanged. Device frame pacing/gallery/IME and every-model
account workflows are not inferred from tests. See release and validation docs.

All four public assets (APK,update.json,SHA256SUMS,validation.json) were anonymously
downloaded via verified HTTPS and matched local sizes/hashes and GitHub digests.
Latest-stable/updater schema1 and RU/EN compatibility match. Tagged documentation,
release body/credit and component README accessibility were verified. The APK
verification copy required HTTP Range resumption after an interrupted stream;
its final complete SHA-256 matched the source and GitHub digest. Preserve
published tag/assets and older releases. Documentation-only follow-ups do not
rebuild an unchanged APK or move its tag. Next versionCode must exceed1413.

Active development source remains the isolated work/chat-file-cards checkout at
/root/termux-fork/.verification/chat-file-cards/source. Do not build or discard the
unrelated root worktree rotation experiments. Preserve active terminals, rootfs, tunnels, server services and the shared SDK
when continuing development.

---

# Historical private agent-chat test2 — superseded by v2.0.8

Test1 was installed; follow-up feedback requested a Copy outline and a smoother,
reversed Stop activity route. Those changes are implemented. The line now uses
Choreographer with a60FPS target; the existing server/robot borders remain16ms
scheduled and are not a fixed/measured60FPS reference. No X11/native changes.

2.0.8-test2/code1412: build/apk/Termux-Jobs-test2.apk, 39773208bytes,
SHA25690757864d6400360f521326971ce35a8dde27f4081188bd5c409b8a34e6ac17a.
109 JVM cases pass; modern lint5modules0errors/217warnings; isolated build and
final package/ABI/signer/ZIP/runtime/assets/secret checks pass. See
[validation](docs/fork/VALIDATION-chat-test2.md). Physical animation/touch checks
are pending. No GitHub publication without developer acceptance; next stable
must exceedcode1412. Preserve the separate root-worktree rotation experiments.

---

# Historical private agent-chat test1 — superseded by v2.0.8

Active branch work/chat-file-cards is based on stable v2.0.7/b832b681. It adds
Developer Markdown, arbitrary output cards, shared sorting, long-press attachment
multi-selection, native media gallery, scoped Stop with activity route, and
acknowledged-send keyboard dismissal. X11/native graphics inputs are unchanged.

Built 2.0.8-test1/code1411, Termux-Jobs-test1.apk. See
[validation](docs/fork/VALIDATION-chat-test1.md) for hash,105 passing JVM cases,
eight Node suites, modern lint0errors/217warnings and device-check gaps.
Do not publish until the developer tests and accepts. Stable remains v2.0.7.
Next stable versionCode must exceed1411; retain com.termux/signature/schema8.
No installed app, session, rootfs or tunnel changes are part of this work.

---

# Stable release: v2.0.7

HezeAI authorized publication of v2.0.7: Mesa26.2.5 for the seven Linux Turnip+Zink runtimes and Bionic-for-glibc frontend; optional Kali theme, Alpine VirGL client and protected default driver from HezeAI/termux-jobs-components; imported-driver removal, theme preparation, live scale reset and portrait viewport fixes. The header remainsv2.0. No new native rotation/presentation change is included.

VersionCode1410, versionName2.0.7, same com.termux/signer/schema8. Final artifact: Termux-Jobs-v2.0.7-arm64.apk,39,748,664bytes, SHA25602f1e77c188f53c5a9cb9c7294d6d5ad5ce04d9822be5a7a97a69c6c2c796938. See docs/fork/RELEASE-v2.0.7.md, VALIDATION-v2.0.7.md and UPDATE-NOTES-v2.0.7.json. Follow docs/fork/RELEASING.md for public asset verification. No installed app/session changes are authorized by publication.

The component tags graphics-20261025, mesa-26.2.5 and x11-extras-20261025 are immutable; do not overwrite old releases or assets. Twelve production URLs passed published size/digest matching and HTTPS availability checks. Normal distribution packages retain native repositories. Component downloads are lazy and cache-verified; running desktops adopt changed libraries after stop/start. Selected imported drivers and user data are preserved.

Source/build checks:1,291 passing JVM cases with short-path fixture reruns,133 targeted Python tests,584 localization strings; modern lint0errors/210warnings across all five modules. Validation documents distinguish automated results from HezeAI's reported device acceptance and fresh-device/hardware limits. No blanket lint suppression was added.

Keep future work isolated and preserve published artifacts. Review package/data compatibility, update localized updater notes and documentation for each stable release. Next versionCode must exceed1410. GUI, gesture, rotation, DPI, permission and GPU compatibility require device evidence; do not infer these from mocked tests or configured rootfs contents.

Published and publicly verified: https://github.com/HezeAI/termux-jobs/releases/tag/v2.0.7. Immutable tag/source commit621ce4ccd9e6e8e465324d60d960c2a058e8bd5d. All four assets were downloaded anonymously over verified HTTPS; byte lengths, local hashes and GitHub digests matched. Latest-stable lookup, RU/EN legacy-compatible updater metadata, release body/credit and tagged documentation passed. Postpublication documentation changes do not move the tag or alter APK assets.
