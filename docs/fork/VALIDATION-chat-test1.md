# Historical agent chat test1 validation — 2026-10-26

Private test build, not a stable release or a publication approval. Base: published
v2.0.7 plus its documentation commit b832b681. The abandoned native rotation
experiments from the main worktree are excluded.

## Artifact

- File: Termux-Jobs-test1.apk; 39,772,688 bytes.
- Version: 2.0.8-test1 /1411; package com.termux; ARM64 only.
- SHA-256: `c0531f55c36023fb3b92ac802824d8a339b795aceb30ee6cb58939302807df6c`.
- Signing certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
- ZIP integrity, release/debug flag, package, version, ABI and certificate checks pass.
- All78 source assets match. X11 runtime assets and all three JNI libraries are
  byte-identical to stable v2.0.7. No Wayland payload is included.
- New Markdown/table classes and notices are present; required runtime definitions
  pass the existing DEX inventory. Its31 reviewed optional/platform references
  remain; inventory does not prove every method/API/device path.
- Forbidden private filenames/build directories and secret-pattern scans of assets
  and DEX report no findings. No authorization files are bundled.

## Automated evidence

-105 JVM cases across16 relevant classes: zero failures/errors/skips. Covers
  sorting and preserved selection, local-file metadata, native output cards,
  Markdown/code copy/task literals/table rendering/stream identity/focus, gallery
  intent and content-URI limits, bounded staging with original preservation,
  multi-select confirmation, scoped Stop, activity overlay lifecycle, existing
  chat cache, browser ownership and keyboard policy.
- Eight Node suites pass: generic output projection and the four providers,
  attachment adapters, OpenCode socket attachment workflow. Tests use synthetic
  transports/files; no account/model API requests or active-session interruption.
- Modern lint7.4.2 analyzes all five modules: zero errors,217 warnings; no rule or
  infrastructure loading errors. The two new InlinedApi notices refer to media
  picker constants selected behind the Android33 guard with ActivityNotFound
  fallback. Existing compileSDK35/toolchain support limits remain. No blanket
  suppression or new baseline is introduced.
- Signed isolated ARM64 assembleRelease succeeds. Product-source whitespace checks
  pass; the verbatim upstream license notice retains its final blank line.
  Build tools, caches and outputs remain inside the repository.

The lifecycle test calls the custom view's drawing callback because Robolectric's
View.draw shadow does not dispatch it. It confirms overlay ownership/cleanup,
not native graphics timing or a measured60fps rate. Gallery staging tests use a
synthetic ContentResolver stream; HEIC codec/device behaviour remains unverified.

## Source review and state inspection

Markdown is native presentation, with no WebView or remote-image fetch; tapped
links accept HTTP(S) only. Generic output cards inspect metadata and do not execute
files, unpack archives or begin APK hashing. Explicit save uses the existing
bounded file transport. Sorting is a separate display preference.

Gallery requests grant only selected content URIs. Copies are bounded, owned by
the original chat run and cleaned on staging failure/close. Late results do not
attach to a different selected run. Multi-selection remains a draft until Send;
its limit includes existing attachments. Phone storage keeps its own permission
flow, and gallery does not request broad storage access.

Stop reuses chat-interrupt for the captured native session; existing generation,
profile and OpenCode direct-command confirmation checks remain. No new global
process kill, server shutdown or tunnel change is introduced. The activity overlay
retains the one-pixel layout height and releases on idle/hidden/detach.

The acknowledged-send callback clears/hides the message input only for the same
terminal and unchanged submitted draft. Failed sends and a newly edited draft
retain input. Upper Shell command behaviour is unchanged. No store schema/account,
raw transcript, rootfs or desktop migration/reset is required by these source
changes; an upgrade on the actual device still needs developer acceptance.

## Minimum developer phone checks before publication

1. Update over the installed stable app and confirm histories/accounts remain.
2. In all four agent chats: send a file-only/mixed draft, use long-press multiple
   selection, gallery photos/video, and download ZIP/3MF/STL/EXE output cards.
3. Sort files by size/date in the shared picker; confirm selections remain.
4. Review headings/code/tables during a streaming answer, copy code, expand long
   code, scroll history and confirm composer focus remains stable.
5. Send a message with the keyboard open; verify acknowledged send hides it,
   while failed send keeps the draft. Verify upper Shell behaviour remains.
6. Interrupt a disposable request using Stop; verify the neighbour stays active,
   idle is crossed/disabled, and the activity line has the intended appearance.
7. Briefly open an existing X11 desktop as a stable-baseline smoke check.

No device IME/gallery/gesture/frame-timing acceptance is claimed by these tests.
Publish only after HezeAI accepts this test APK. A later stable release needs a
higher versionCode, localized updater notes/update.json and final release docs.
