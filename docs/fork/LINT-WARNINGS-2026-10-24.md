# Remaining warning categories — lint7.4.2

Current code1351:210 warnings across five analyzed modules. The earlier audit
through1350 had209; the added warning is the pinned private GNU GStreamer plugin
in assets, reviewed below. No new blanket suppression or baseline hides them. These are retained findings/debt, not a claim that every warning is false.

| Category | Count | Review / disposition |
| --- | ---: | --- |
| AppBundleLocaleChanges | 1 | Distribution currently uses a universal APK with both locales, not language-split Play bundles. AAB/store architecture remains separate. |
| ClickableViewAccessibility | 4 | Remaining swipe/parent-interception listeners and terminal/SurfaceView gesture paths need a separate TalkBack/device accessibility pass; image tap performClick fixed here. |
| Deprecated | 1 | sharedUserId/package identity is a separate migration decision for installed Termux/addons; no silent identity split. |
| DiscouragedPrivateApi | 1 | Upstream terminal FileDescriptor reflection retained; changing JNI/target SDK needs its own migration and device tests. |
| GradleDependency | 7 | Pinned dependency baseline retained, except the explicitly tested EXIF update. New-version suggestions are not automatic compatibility/security verdicts. |
| IconLocation | 1 | Upstream densityless banner retained; artwork/DPI cleanup is separate from source stability. |
| InflateParams | 1 | Intentional standalone dialog keyboard layout; parent parameters are assigned by the receiving dialog. Preserve measured keyboard/window geometry. |
| MonochromeLauncherIcon | 2 | Optional themed icon artwork for newer Android; no icon redesign in this pass. |
| NotifyDataSetChanged | 1 | Upstream report-text adapter refresh; no evidence justifies rewriting its update contract in this pass. |
| ObsoleteSdkInt | 39 | Legacy lower-API guards/resources remain for upstream alignment; removing unreachable branches is a separate cleanup, not a runtime fix. |
| Overdraw | 1 | Existing root/theme background fallback retained; transition/focus behavior must be device-checked before altering it. |
| PackageManagerGetSignatures | 1 | Legacy signing fallback belowAPI28 is followed by exact certificate comparisons; modern signing-info path and negative APK tests remain. |
| RedundantLabel | 3 | Redundant manifest labels are harmless and retained for upstream/component identity consistency. |
| SdCardPath | 13 | Intentional Termux/proot ABI and shared-storage aliases. Package-ID/multi-user redesign is separate; never blindly replace guest-visible paths. |
| SetTextI18n | 75 | Retained localization debt/dynamic values: runtime RU/EN helpers, user data and upstream diagnostics. No blanket text-resource rewrite in this stability pass; string/key parity is checked separately. |
| TypographyDashes | 2 | Literal CLI --remote flag in RU/EN help must remain ASCII, not an em dash. |
| UnsafeNativeCodeLocation | 4 | Intentional guest Linux codec/compatibility payloads extracted into the selected environment; these are not Android JNI libraries. Pinned payload hashes remain checked. |
| UnusedResources | 24 | Resource-cleanup candidates retained; shared/upstream themes and names are not removed speculatively. |
| UseCompatLoadingForDrawables | 21 | Platform loading works on minAPI26 and existing vectors; defer cosmetic compatibility-loader churn and verify themes on device before changes. |
| UselessParent | 1 | Upstream text I/O layout structure retained; keyboard/scroll geometry changes require device checks. |
| VectorPath | 1 | Possible static icon optimization, not a measured performance fault; keep existing artwork. |
| ViewConstructor | 6 | Programmatic views require an owner/domain actions; adding incomplete XML constructors could create invalid instances. |
