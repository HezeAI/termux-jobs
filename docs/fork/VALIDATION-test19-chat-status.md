# Private test19 — delivery, attention and transient execution

Scope: shared message delivery presentation, explicit native ID correlation in
Claude/Gemini/OpenCode, restored native-terminal attention borders, transient
execution footer, outlined page previews. Codex native delivery code remains
unchanged. No additional CLI polling or page fetches are introduced.

Gemini transports Android input IDs as presentation metadata through the existing
native callback and queue. Queue entries/prompt strings are unchanged. Combined
native inputs retain per-ID receipts. Pending inputs without a native echo retain
separate admission rows; no text matching assigns a receipt. Receipt correlation
is held by the running adapter; restarting an adapter can lose old admission
captions. Acknowledgement does not prove model uptake. OpenCode HTTP admission
is distinct from native inbox enqueue/deliver evidence.

The execution footer belongs to the view, not native history. It uses the existing
activity frame clock, fixed measured height, nonselectable content and real request
states. Waiting/reconnect replace normal motion; idle/error hide it. No scrolling
is requested by animation frames. Terminal attention remains active when its
window is minimized; native state controls its reset. Server transfer presentation
and listeners remain independent of window visibility.

Evidence: `.verification/compact-delivery-test19/`. Initial test failures and
corrected reruns are retained. The original view-count assertion needed the new
reserved footer; the footer test needed this project's Robolectric API.

Checked anew: Java/UI selected suites, four adapter regressions, pinned Gemini
hook syntax, Codex delivery/reconnect/pagination-related fixtures, scoped Stop
checks with real signals blocked and request timing regression. Final build/Lint
and artifact results are recorded in the final evidence below.

Reused: unchanged native payloads, existing SDK and build caches. No environment
installation, graphics execution or working sessions were used for testing.

Not checked on a phone: visible border/dot smoothness, touch, actual keyboard and
TalkBack. Narrow/large-font web previews are modeled, not a phone GUI test.
No APK installation/publication or working-chat restart is performed.

Final APK: `/sdcard/Download/Termux-Jobs-test19.apk`; 43027337 bytes. SHA-256: `e3dd6057109a3787531b85213179dbc28d301480b024634ff3dfb398b025d50e`.
Version1453, com.termux, ARM64, non-debuggable, previous signer verified. ZIP and DEX runtime checks pass. Modern Lint: zero errors; warnings: {'Warning': 234}.
The initial AppCompatCustomView lint error was fixed by using AppCompatTextView; final footer test/build and modern Lint passed.
