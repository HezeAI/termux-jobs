# Focused Fedora MP4 / input-overlay review — code1351

This focused change precedes the developer's new phone check and the planned final
application audit/publication. It is not an exhaustive fresh-device GUI audit.
Evidence: `.verification/fedora-media-20261024/`.

## Diagnosis and implementation

A read-only inspection of the existing Fedora root found the libav GStreamer
plugin and the noopenh264 ABI stub, but no openh264dec plugin. In isolated probes,
AAC/demux/parsers were present; libav did not expose an H.264 decoder. Simply
changing the OpenH264 library search path did not supply a decoder factory.
Fedora's [OpenH264 documentation](https://fedoraproject.org/wiki/OpenH264) describes
the separate codec/plugin components and Cisco repository dependency.

Fedora Video and audio/pkg:parole now explicitly requests Parole, GStreamer tools,
libstdc++/libgcc, libav and base/good/bad-free plugins. DNF excludes Cisco for the
individual commands, preserving repository files and TLS/GPG verification. No
external repo, package swap, allowerasing or system-library replacement is used.

The existing pinned OpenH2642.6.0 library is paired with an unmodified ARM64
GStreamer1.26+ bridge extracted from a pinned Debian package. Download/member/
asset hashes and full notices are in [provenance](../../third-party/gstreamer-openh264/).
The Debian package is never installed into Fedora. Its two codec assets are in
the APK; normal Fedora package downloads still need internet.

The helper installs in the selected Fedora HOME and creates a private Parole
launcher. It preserves inherited library/plugin paths and uses a dedicated registry
so the private plugin does not enter other apps' usual cache. Destinations must
match owned hashes; user edits/symlinks are refused. It writes a pending proof,
commits files, creates a real decoder and probes openh264dec/avdec_aac/qtdemux.
Only successful checks produce a digest-bound ready proof. Failed/interrupted
setup stays missing for retry. The existing openSUSE three-file mode is retained.
Fedora's recommended-status cache changes schema2→3 in the same filename, forcing
one fresh check after upgrading without discarding catalogs or other targets.

The fullscreen control moves24dp lower, to48dp plus the top cutout inset. Window
alpha0.65 makes it translucent; focus/outside-touch policy and enabled fill remain.
No new redraw timer, renderer flags, native process-stop or agent change is added.

## Evidence

- 72 scoped JVM tests/6classes pass: focus/window lifecycle, alpha/offset/fill,
  stable v2.0 header, private-service routing and recommended-cache invalidation/
  persistence. Surrogate views do not prove physical IME or composition.
- 66 Python cases pass, including existing openSUSE behavior, owned-file upgrade,
  foreign edits/symlinks, corrupt plugin input, failed-probe pending state/retry,
  no codec install during checks, fixed runtime requests and DNF trust flags.
- Native tests use Fedora's installed binaries/libraries read-only, loaded with
  its dynamic linker into subprocesses. HOME/cache/config/plugin fixtures and all
  generated media live under the repository. No installed rootfs/session is changed.
- A synthetic one-second H.264 High-profile/B-frame video plus AAC MP4 fails with
  the ABI stub (GStreamer ERROR,0video/0audio buffers). With the private decoder/
  plugin it reaches EOS, producing **5 video buffers and47 audio buffers**.
  Output uses fakesinks: this is actual decoding, not a Parole window/audio-device
  test. Files: native/baseline-buffers.json and native/private-buffers.json.
- The real helper runs in an isolated HOME with Fedora libraries, validates real
  decoder creation and GStreamer factories, reports ready and repeats successfully:
  native/install-proof.json. The test's launcher/tool paths are fixture paths,
  not a fresh Fedora installation or physical app-menu check.
- Five-module lint0errors/210warnings. The extra UnsafeNativeCodeLocation warning
  is the deliberately bundled GNU plugin, pinned and loaded inside Fedora rather
  than Android JNI. [Warning review](LINT-WARNINGS-2026-10-24.md).
- Final APK uses fresh packaging. Incremental ZIP dead space was discarded without
  changing source; inter-entry gaps0,8,192bytes between the last entry and central
  directory for signing. ZIP/signature/package/code/ABI/runtime/source-stage/secret
  checks and exact APK/update.json/SHA256SUMS agreement are recorded separately.

## Remaining check

Update, open Fedora Programs→Recommended→Video and audio and install again; old
cached installed status is rejected. Close any old Parole window and relaunch it
from the applications menu, then open the reported MP4. Copied old launchers or
direct /usr/bin/parole do not inherit the private wrapper environment. Check actual
video/audio output and the lower translucent | button. A clean phone still needs
its own full package-download/GUI check. H.264/AAC success does not guarantee all
MP4/HEVC formats or hardware decoding. The final broad audit and GitHub release
remain after the developer's check.
