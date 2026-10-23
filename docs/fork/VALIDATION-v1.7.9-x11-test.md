# Private X11 test 1.7.9/code1224

The developer confirmed XFCE starts in 1.7.8; screenshot
Screenshot_20261020_113702.jpg shows the actual desktop inside the selected session.
Finger movement instead made selection rectangles. Source inspection confirms
ACTION_DOWN immediately pressed the left mouse button. Native button messages
also ignore x/y, so a stationary direct tap did not reposition the cursor.

Finger input now uses relative touchpad gestures: swipe moves the pointer without
a pressed button; tap clicks; two taps generate two clicks for desktop opening;
hold then move drags; two-finger swipe scrolls; two-finger tap right-clicks.
Cancellation/detach removes the hold timer and releases a held drag button.
Mouse/pen keep direct positioning; pointer motion is sent before their button press.
No native ABI/core changes, terminal/agent input changes or per-frame gesture timer.

Relevant tests cover move without press, tap/double tap, held drag with cancel,
no delayed drag after a swipe, two-finger scroll/right-click and canceled gestures.
Existing focused X11/controller/agent tests and pinned JNI checks are retained.
Build, test and final artifact logs: `.verification/x11-desktop/*touchpad*`.
Actual Android gesture feel, speed and XFCE double-click behavior still need a
phone check. Desktop launch was confirmed in the preceding version; this report
does not claim all interactions were device-tested.

Same package/certificate/minSDK26/ARM64, higher code1224. No user data migration.
Private delivery only; stable GitHub and original stable APK backup unchanged.
Device check: open Ubuntu X11 Test desktop, move cursor onto Home, double-tap
without moving the finger; test scrolling and hold-drag, return to terminal and
another AI session. Installation/components do not need reinstalling for this patch.

Final results: 48 focused JVM tests passed (including seven gesture tests); six Python ABI/process-wiring checks passed. Release build, packaged runtime/JNI/ZIP/signature audit passed. Size 38918417 bytes; SHA-256 `2a06e74c74f851eda56855a6d86a9ba0dfbd656029ac974d91d35004e5855d78`. Download copy hash rechecked.
