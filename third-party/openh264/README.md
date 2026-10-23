# Private Parole OpenH264 decoder

The official openSUSE OSS noopenh264 package is an ABI placeholder. The separate
codec repository returned HTTP403 on the test device; enabling it again blocks
Parole and office installations. Keep the temporary repo filter and system RPMs.

The optional Parole setup uses the ARM64 Debian OpenH2642.6.0 library, extracted
at build time from the pinned official package in provenance.json. The package
is never installed into openSUSE. Its copyright text is included in the APK.
This is Debian's source-built binary, not a Cisco-provided binary; do not imply
Cisco binary patent licensing applies. See COPYRIGHT for the upstream notice.

Only the managed private Parole launcher adds the private codec directory to
LD_LIBRARY_PATH. System/agent sessions and the system library are unchanged.
Local launcher edits and symlink destinations are refused and preserved.

In a bounded separate openSUSE diagnostic process, GStreamer openh264dec decoded
an artificial High Profile/B-frame stream to EOS. This is not exhaustive codec
or GUI playback validation; the developer should check an actual recording.
Reinstall the recommended Video and audio group to add the decoder to an
existing Parole install. Restart the player and recreate old desktop shortcuts
if they still bypass the private application-menu launcher.
