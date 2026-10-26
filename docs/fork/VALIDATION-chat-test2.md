# Historical agent chat test2 validation — 2026-10-26

Follow-up to [test1](VALIDATION-chat-test1.md), installed by the developer. Device
feedback requested an outlined Copy action, the opposite circular activity route,
and smoother animation. This test build remains private pending device acceptance.

## Changes and review

Copy uses the existing workspace state-list background, centred text, a visible
outline and pressed/focused feedback. Copy still retains the exact code literal.
The activity route enters the right semicircle, crosses the top toward the left,
returns to the lower tangent and continues right along the line.

A single Choreographer callback replaces the independent16ms timer. Its cadence
uses frame timestamps to target an average60 redraws/second on faster screens.
Geometry comparison avoids per-frame string allocation. Drawing no longer
invalidates itself recursively; scheduled frames invalidate the line/overlay.
Internal gradient segments use flat ends rather than overlapping round caps.
The overlay and its one-pixel layout height remain; no chat/tree rebuild or PTY
resize is introduced. Inactive/hidden/detached states remove pending callbacks
and overlays. Stop still calls the existing scoped interrupt; its protocol and
process ownership are unchanged. Server/robot border animations are unchanged.

The old border indicators use16ms scheduling, not a measured fixed60FPS. Neither
source cadence tests nor VSync prove the phone sustains the target. Physical
smoothness, direction and appearance need developer acceptance.

## Automated checks

-109 JVM cases/16classes pass, with zero errors, failures or skips. New checks
  verify Copy's registered idle/pressed drawables and exact clipboard contents;
  actual route coordinates enter the right half before the top/left; cadence
  decisions target60Hz over synthetic60/90/120/144Hz display timestamps and resume
  after a stall/reset without a catch-up burst. Existing overlay shutdown,
  streaming/focus, attachments, browser, gallery and chat-cache tests pass.
- The drawable-state assertion uses Robolectric's registered state-map API;
  it is not a physical touch/paint check. Route lifecycle tests invoke onDraw
  explicitly because the framework draw shadow does not dispatch it.
- Modern lint7.4.2: all five modules analyzed,0errors/217warnings, no infrastructure
  or rule-loading failure. Warning categories and SDK limits match test1.
- Isolated signed ARM64 assembleRelease and required runtime-definition checks pass.
  Node/provider assets are unchanged since the eight passing test1 suites.
- Package/version/ABI/debug flag, ZIP, signer, source assets, native payloads and
  forbidden-file/secret-pattern checks pass. Product diff whitespace checks pass.

## Final APK

File: Termux-Jobs-test2.apk; 39,773,208bytes. Version2.0.8-test2/code1412,
com.termux, ARM64 only. Signer matches stable and test1.

SHA-256: `90757864d6400360f521326971ce35a8dde27f4081188bd5c409b8a34e6ac17a`.

Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.

All78 source assets match. All three JNI libraries and X11 runtime assets remain
byte-identical to stable v2.0.7. No account/rootfs/history/schema migration/reset is
introduced. Higher code1412 permits an in-place update from test1/1411; actual
installed data preservation still requires developer verification.

## Device acceptance

Update from test1, tap Copy and verify the visible pressed state and clipboard.
Run a disposable agent request and watch the line enter the right half of Stop,
pass across the top toward the left and continue right. Compare smoothness with
Server + Agent/robot, idle and tab switching. Confirm idle stops the indicator and
the rest of the chat retains its position. No physical60FPS measurement is claimed.
Publish only after explicit developer acceptance; a later stable release needs a
code higher than1412, localized updater metadata and the full release review.
