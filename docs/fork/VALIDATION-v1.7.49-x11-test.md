# Private v1.7.49-x11-test / code1264

Developer screenshot143640 still shows SCAN_ACCESS_DENIED13 after48 and reports only
baseTermux open. Migration root cause in actual APK context remains unresolved. Source
busy is not established. New UI exposes existing scan-relative path and operation that
were previously dropped, including BuildConfig version. No source exclusions expanded,
source permissions altered, installed app/session modified or real migration performed.
Read-only guest lookup of exported host app helper returned ENOENT; absence in this guest
namespace does not establish absence/version of the helper in Android context.

44 Python fixture tests passed, including exact relative path and operation on a forced
fd-relative directory denial; original exception text/absolute filename stay hidden.
26 migration tests also passed under native Termux Python through this Ubuntu/proot using
repo-only fixtures. These do not validate native APK process access. JVM tests check
rendered detail, rejection of parent/absolute paths and escaped control characters.
Field path capped512 codepoints, JSON safely below8192 line bound. Detail is local only;
no content, raw exception text, network reporting or diagnostic file added. Existing source
retention, no-follow/busy/atomic publication/cancel guards remain. Final results below.

Device check needed: retry migration once and inspect new operation/path/version in the
error dialog. No successful migration is claimed. This build improves diagnosis only.

10 selected JVM tests in 2 suites passed. git diff --check passed.

Final isolated ARM64 build succeeded45s. Final APK ZIP integrity/duplicates, com.termux,
1.7.49-x11-test/code1264, minSdk26, non-debuggable, ARM64 ABI, v2 signing certificate,
source/staged Java and migration assets checked. Native X11/Alpine helpers and JNI28/1/5
unchanged. Runtime definitions13427 /31 reviewed optional unresolved types. Limited private
key/token marker scan passed. Inherited Kotlin metadata/Lint dependency warnings remain;
clean Lint is not claimed. Bounded JSON check with astral-character path passed8192 limit.

Artifact `Termux-Jobs-v1.7.49-x11-test-arm64.apk`: 39033102 bytes.
SHA-256 `4ab29a82828c92ab2363e2e2539ac3fa23a2693cffc8bc840b2f60a1c57b480e`.
Certificate SHA-256 `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Download copy rehashed and `.apk.sha256` supplied. No APK installation or publication.
Migration remains unresolved pending actual app failure path.
