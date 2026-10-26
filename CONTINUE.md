# Private agent-chat test2 — pending developer acceptance

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

# Private agent-chat test1 — pending developer acceptance

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
