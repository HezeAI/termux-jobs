# Retired X11 diagnostics

The GTK observer, Python metadata recorder and failed Garcon overlay were used
while diagnosing XFCE flicker. They are not loaded or packaged in v1.7.34 onward.
The renderer, focus-based keyboard and bounded startup readiness remain active.

Python recorder: x11-observe.py here. GTK/Garcon C sources remain under
x11/src/main/cpp and build/reproduction scripts under scripts for historical study.
Garcon license is retained here, outside APK resources. Historical reports refer
to paths that existed at the time. Do not restore these modules to production
without an explicit request and new validation.
