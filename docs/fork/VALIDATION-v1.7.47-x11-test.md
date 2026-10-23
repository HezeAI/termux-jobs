# Private v1.7.47-x11-test / code1262

## Scope and evidence

Automatic keyboard checkbox added to all supported desktop menus, saved per environment,
default on. Disabled automatic focus/tap events update their serial baselines without
opening or hiding keyboard; manual toggle remains independent. Activity IME dismissal,
view reset and agent chat behavior remain unchanged. No second renderer/timer was added.
Native X11 libraries/server assets remain unchanged.

Migration errno13 remains unproven: a 25-second partial read-only native interpreter walk
from current Ubuntu/proot visited 28681 directories without errors; this is neither full
source traversal nor native Android app-context verification. No real lock acquisition,
clone, source permission change, package installation or session stop occurred. New fixed
access-stage diagnostics preserve nested failure location and disclose no private names,
file content or exception text. No key exclusion or permission/busy bypass was added.

39 Python fixture tests passed, including lock-file, /proc listing and snapshot access
failure classification and secret-free serialized diagnostics. Existing copy/cancellation/
atomic publish/removal safety checks remain covered. JVM/build/artifact results below.

Device checks still needed: retry migration to identify actual failing stage; toggle auto
keyboard off/on in X11 and verify manual button, field taps, Back and environment changes.
No successful actual migration or device keyboard check is claimed for this build.

145 JVM tests in 21 selected suites passed, including automatic-keyboard disable/manual
input behavior, per-environment persistence/recreation, existing desktop/program UI, agent
environment/store/routing and Codex behavior. These are fixtures/Robolectric, not device
keyboard validation. git diff --check passed.

## Final artifact

Isolated ARM64 release build succeeded (1m14s). APK ZIP integrity/no duplicate entries,
com.termux identity, version1.7.47-x11-test/code1262, minSdk26, non-debuggable, ARM64 ABI,
v2 signature and retained certificate verified. APK matches final migration asset and
Java staging. Pinned X11 library and Alpine helper unchanged. Runtime definitions13427,
31 reviewed optional unresolved types. Limited private-key/token marker scan passed.
Build emits inherited Kotlin metadata/Lint dependency warnings; clean Lint is not claimed.

Artifact: `Termux-Jobs-v1.7.47-x11-test-arm64.apk` (39031054 bytes).
SHA-256: `ce783539e9b3ee2467ec2ad305d03c5d3202a007570e873d1ec3a2d0edfe6a35`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Download copy rehashed and matching `.apk.sha256` supplied. No GitHub publication.
