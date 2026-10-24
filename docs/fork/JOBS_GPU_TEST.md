# Jobs GPU Test — private candidate 1363

Open **Programs → Recommended → Jobs GPU Test → Install** in the selected X11 environment. Installation creates **GPU TEST** on that environment's desktop and in its application menu. The existing GPU/glmark2 and GPU INFO tools remain separate. The card is available for Termux, Ubuntu, Debian, Alpine, Fedora, openSUSE, Void and Arch; packages and files stay inside the chosen environment/home. Nothing is installed merely by starting a desktop.

The diagnostic uses Python's standard library, X11 and desktop OpenGL/GLX, inheriting the running desktop's renderer and imported-driver selection. It does not launch the supplied Windows/Direct3D executable, install Wine, select a different driver, or change another application's swap interval. No native library is converted between Bionic, glibc and musl. Ubuntu/Debian explicitly request Python, X11 and GLX packages; Termux requests Python, libx11 and Mesa (plus its existing C++ runtime requirement) without depending on glmark2; the remaining native targets request their reviewed GPU-tool package set plus Python. Their package managers resolve the runtime dependencies normally.

The three scenes are a textured rotating cube, multiple rotating objects, and a moving frame pattern. Use the on-screen **Scene**, **VSync**, **Report**, **Exit** buttons; keys 1–3 select a scene, F11 requests window-manager fullscreen, S saves a report, and Esc closes the test. VSync affects only this test window and is available when its GLX driver exposes the required extension. The test pauses rendering while its X11 window is unmapped/minimized.

The window shows submitted FPS, frame time, a recent-frame graph and the OpenGL renderer. Rates include Python, scene/UI and presentation costs; they are **not glmark2 scores**, a game-performance prediction, or the Android panel refresh rate. A moving pattern is a visual diagnostic, not an automatic assertion that every frame is correct. Compare identical scenes, sizes and drivers, with other GPU tests closed. A sample window can include a scene/VSync transition; allow it to settle before comparison.

**Report** creates a new `Jobs-GPU-Test-<UTC>-vsync-<mode>-<unique>.json` in `/sdcard/Download` when that directory is available, otherwise in the selected environment's home. Manual reports do not overwrite previous modes/runs. It records the renderer/vendor/OpenGL version, current scene, window size, requested swap interval, frames/duration since launch, and statistics from recent samples of the current scene. It includes no full environment dump, tokens, keys, project files, hostname or network request. Report destination errors appear in the test window. The command-line `--report` option supports an explicit overwrite path for isolated verification. The1.1 payload must be refreshed/reinstalled in an existing test installation to get the new manual filenames.

Removal from the recommended card deletes only managed test files and shortcuts. Shared package dependencies and saved reports are retained. Unrecognized edits or linked parent paths are preserved rather than overwritten. The test's reserved removal identifier is handled by our file installer, not sent to a distribution package manager.

## Verification and limits

The three scenes ran against the fork's isolated, authenticated X11 core using the real imported V38 Bionic Turnip, private Mesa 26.1.6 and glibc bridge. X11 pixel readback and JSON reports passed for every scene, followed by orderly private-child cleanup. This is real native rendering without an Android Activity/surface; it is not a device UI acceptance result.

Eight focused tests cover offline/idempotent installation, missing/changed payloads, edited files, symlink boundaries, removal retaining reports, target package selection and report privacy. The existing 56 program/native/shortcut/resume tests and the 26 targeted Java UI/resume tests pass. Fresh package downloads and actual desktop buttons, fullscreen, gestures, and every target/renderer combination still need developer checks. Installed environments were not changed to simulate clean installs.

The source is `x11/src/main/assets/x11-gpu-test.py`; installation is `x11-gpu-test-setup.py`. CLI limits `--seconds`, `--frames`, `--scene`, `--width`, `--height`, `--verify`, and `--report` are available for development probes. The app itself adds roughly 20 KB of source plus a small file installer before APK compression.

## Developer Ubuntu run

The developer launched the installed test in ordinary Ubuntu and used Report. The saved report identifies Zink over Bionic V38 Turnip/Adreno 830, Mesa 26.1.6, a 960×720 textured-cube scene and requested swap interval zero. It records 18,361 frames over 93.866 seconds (about 195.6 submitted FPS since launch); the latest 10,000 samples have median 3.973 ms, p95 12.152 ms and p99 14.355 ms. This confirms launch/render/report execution on this Ubuntu setup, not every UI action or target. The report does not partition GPU work, driver waits, Python, X11 copies or PRoot overhead, so it cannot identify a single bottleneck. Visible flicker is not automatically assessed.

## Developer Turnip + Zink comparison

A subsequent Ubuntu report uses Mesa 26.3.0-devel (git-98f3d6229d), the same Adreno 830 renderer family, 960×720 textured-cube scene and requested interval zero. It records 12,999 frames in 49.049 seconds (about 265.0 FPS); median/p95/p99 recent frame times are 3.400/5.555/7.606 ms. The earlier bridge run averaged 195.6 FPS with 3.973/12.152/14.355 ms. The ordinary stack is about 35.5% faster in these two runs and has lower high-percentile frame times.

This compares complete stacks with different Mesa versions and unequal run lengths, not an isolated measurement of bridge overhead. Temperature, GPU clocks, exact native driver revisions and background workloads were not controlled. Each frame-time summary uses the latest 10,000 samples, whereas the computed FPS uses frames/duration since launch. Both original reports are retained in private verification evidence. No further code/APK change was made for this comparison.

## Local installation with configured dependencies

The installer checks the complete selected-target runtime package set. If already configured, it copies only the local Python diagnostic and its managed shortcuts, without updating a package index or downloading anything. Missing dependencies continue through that target's authenticated package manager. This avoids an unrelated out-of-sync Termux mirror blocking an otherwise self-contained local application. No insecure APT flags, global repository rewrite or change to other program groups is used. Regression tests cover the offline path and refusal to create test files before a missing dependency has been installed.
