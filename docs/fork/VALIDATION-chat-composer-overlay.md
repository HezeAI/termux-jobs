# Chat composer overlay and horizontal tools

2026-11-03. Local UI change; no native CLI, rights or history format change.

In agent chat only, the existing drawer/surface extends to the bottom edge behind
the existing input row. The input row and the common tool outline have transparent
fill; the EditText keeps its readable background. Existing star/paperclip vector
buttons are reused as star / paperclip, with a decorative, non-clickable slash and
separate 48 dp ripple targets. Send stays at the input's bottom-right.

Transcript padding uses the measured input height plus any visible attachment/
extra-key footer and a small 16 dp gap. The footer overlays the scrollable surface
above the input rather than reserving an opaque full-width band. Existing draft,
selection, attachments and stable message anchors are retained. Repeated unchanged
presentation does not rewrite the host geometry. Leaving chat restores the
original terminal ABOVE rule, baseline/gravity, background and action layout.

Checks: narrow widths, larger input text, multiline draft/selection, independent
click feedback, send alignment, simulated keyboard viewport shrink/restore,
last-message clearance, anchor preservation, all-agent controller routing and
terminal restoration. Existing chat/history, keyboard and X11 viewport model
checks also run. Initial tests still asserted the old vertical shape/direct child
hierarchy and were updated to test the new overlay contract. A fixture constructor
argument count was corrected before tests could run; no result from that failed
compile is reported as PASS. Evidence: .verification/chat-composer-overlay/.

These are JVM/Robolectric geometry checks, not actual phone rendering, IME or
scroll smoothness acceptance. The test APK phone plan includes those cases.
No robot, voice input, slash-message change or process-stop change is included.


User-reported acceptance, 2026-11-03: the developer confirmed chat layout, keyboard,
buttons and scrolling on a phone with 2.1.3-test1. This is separate from the JVM
checks above; detailed unreported device/font/agent combinations are not inferred.
See [test APK validation](VALIDATION-2.1.3-test1.md) for scope and remaining checks.
