# Private chat-link test1 validation — 2026-10-26

## Artifact

- `build/apk/Termux-Jobs-test1.apk`: **2.0.10-test1 / code1418**.
- Package `com.termux`; ARM64; same signing identity as stable v2.0.9.
- Size **39800325bytes**; SHA-256 **134dc9966d8488ef3ba44714feb7ef776a97281bdcc744adade437a44efda5f2**.
- Certificate SHA-256 **dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644**.
- ZIP, signature, non-debuggable flag, package/version/ABI checks pass.
- All79 source assets match; all3 JNI libraries and X11 runtime assets match the
  stable inputs. No graphics/driver/theme payload changes.
- Runtime:13,854 defined classes,31 previously reviewed optional/framework gaps;
  no new unreviewed missing definitions. Forbidden-file/secret-pattern scans pass.
- Schema8, min26/target28/compile35, Android8+ app / Android11+ X11 and header v2.0
  remain. No data reset or account/environment migration. Code1418 can update
  stable1417; next update over this preview must exceed1418.

## Cause and implementation

The selectable-text flag installs ArrowKeyMovementMethod. Markwon4.6.2 CorePlugin
only installs LinkMovementMethod when no movement method exists. The earlier
span.onClick tests bypassed actual pointer dispatch and therefore did not cover
this interaction. A short URL tap is now handled by AgentLinkTextView while
Android retains selectable-text movement. The AppCompat base follows the app's
widget compatibility requirements without adding lint suppressions.

The common resolver shows a compact panel with the complete HTTP(S) address,
Copy on the left and Open on the right. Copy retains the exact URL; Open invokes
Android's external browser chooser. Outside tap dismisses. No browser navigation
occurs on the initial link tap. Named links, bare URLs, normal transcript rows,
Markdown blocks, table cells and detail Markdown share the behavior across Codex,
Claude, Gemini and OpenCode.

## Relevant evidence

- Final **236 JVM cases /26 classes**, zero failures/errors/skips.
- Eight new link tests exercise actual DOWN/UP dispatch through shared provider
  rows; full-address preview/copy, explicit browser chooser, named/bare links,
  rich/table links after50/100/150 scaling and streaming, gesture cancellation,
  replacement, long press/drag rejection, whitespace, non-conflicting detail
  actions, accessibility span actions, missing-browser handling and inert code/
  invalid scheme/address paths. Existing Markdown/chat/scale/backup/alias tests
  also pass on final source.
- Required compatible five-module modern lint: **0errors /220warnings**; no new
  suppression or rule-loader/infrastructure issue. Existing warning/SDK-tooling
  limits remain those recorded for v2.0.9.
- Final isolated release build and APK/runtime/asset/privacy verification pass.

## Review and limitations

No command, shell, agent request, tunnel, account or new Android permission is
involved. Browser navigation needs the explicit Open action. Raw controls or
whitespace, missing hosts, credential-bearing URLs and non-HTTP(S) schemes are
refused. File cards remain on their existing paths. Gesture candidates reset
on CANCEL, slop, long press, multiple pointers, detached views and replaced text;
link actions do not also invoke the generic truncated-row click.

Robolectric4.4 lacks native glyph metrics, so pointer tests use deterministic
Layout metrics. Negative gesture classification is isolated from its unavailable
selection-handle rendering; provider-row tests retain selectable text. These
fixtures do not prove physical Android text handles, font shaping, hit bounds,
actual browser selection, IME or gesture behavior. Device follow-up: tap a named
link and bare URL; Copy/Open/outside dismissal; long-press selection and scroll;
then try a table link at reduced chat scale. No publication is performed by this
private test. Stable v2.0.9 remains unchanged.
