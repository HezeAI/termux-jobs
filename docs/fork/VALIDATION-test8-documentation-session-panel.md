# Private test8: first-open documentation and current session card

2026-10-29. Candidate2.0.10-test8, versionCode1439. HezeAI is the developer;
OpenAI Codex assisted with implementation. Private APK only, no publication.

The developer reports first-open blank documentation across all environments
and ArchWiki, and shared screenshots show blank Manjaro followed by rendered
content after reopening. The old AlertDialog custom-panel layout reproduces
an actual height0 browser host. The same test with the corrected WebView bounds
shadow still fails against the exact test7 reader source. The new themed Dialog
and FrameLayout provide a measured viewport before load/resume/history restore.
See [reader design](DOCUMENTATION-READER.md).

Robolectric4.4's default fake WebView provider returns false from setFrame, so
its WebView's actual bounds remain0 even with a correctly measured parent.
The new test shadow calls Android View's real setFrame to exercise geometry;
it does not emulate Chromium drawing. Layout/state tests and device rendering
are deliberately separate evidence.

Closing/reopening preserves global page/history/scroll. Closing no longer stops
an unfinished document fetch; actual view destruction still stops and destroys
it. Attach/layout activation is cancelled on close/detach/replacement, and stale
visual callbacks cannot draw into another window/navigation. HTTPS/local file,
permission and TLS protections remain unchanged. Kali's official page returns
HTTP200 and expected documentation content with WebView/mobile/browser user-agent
probes; no source/domain change or TLS bypass was introduced.

The current session has one full-row outer card with individually bordered
settings, robot/packages and files inside. Startup cancellation remains available
inside the same card. Existing actions, robot animation and the lower server/
agent row retain their callbacks. The card grows48to56dp inside the existing64dp
row (top/bottom4dp), so this does not change X11/terminal host row geometry.

No component pin, native renderer/JNI library, workspace data schema or installed
environment is changed. Same package/certificate, higher versionCode, no reset.

Evidence: `.verification/wiki-first-open-1439/`. Final automated/artifact results
will be recorded after the candidate checks complete. Phone acceptance remains
required for first-open rendering, background/rotation behavior and header fit.

## Completed checks

- 23 focused release JVM tests passed: first-open layout for all10environment
  sources plus ArchWiki, load/resume after attach+measure, retained in-flight
  fetch, stale visual callback rejection, language/history/scroll lifecycle,
  HTTPS/TLS/file policies, footer behavior and full-row session actions.
- Independent lint7.4.2: zero errors,225warnings; five modules analysed with no
  infrastructure failures. Changed Java source matches that analysed stage.
- ARM64 release APK assembled and verified: ZIP integrity, packagecom.termux,
  version2.0.10-test8/code1439, stable signing certificate,86source assets,
  required runtime definitions and no secret-pattern findings. All3JNI libraries
  remain identical to rollback1432; graphics/base/helper pins match test7.

APK Termux-Jobs-test8.apk, 39846311bytes; SHA-256
`4c5bb3cea79d112d619a8ea908996563a4adbffc25c0d9dce9eb395ddce00fb7`.
