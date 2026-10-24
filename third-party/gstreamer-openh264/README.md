# Private Fedora Parole GStreamer OpenH264 bridge

Unmodified ARM64 libgstopenh264.so from the pinned Debian package in
[provenance.json](provenance.json). Source and upstream links are recorded there;
full Debian copyright/per-file license notices are in [COPYRIGHT](COPYRIGHT)
and embedded in the APK as licenses/GStreamer-OpenH264-copyright.txt.

scripts/prepare-x11.py extracts exactly this plugin and its notices, validates
its SHA-256/ARM64 identity and places it in the repo-local build cache. It never
installs the Debian package into Fedora. x11-openh264.py installs it only in the
selected Fedora HOME for Parole, with a pinned OpenH2642.6 library, private
GST_PLUGIN_PATH_1_0 and dedicated registry. The distro RPM stub/system plugins
and other environments remain untouched. GStreamer1.26+ is required; failed
runtime component checks do not mark the setup ready. No universal MP4/HEVC or
hardware video-decoding support is implied.
