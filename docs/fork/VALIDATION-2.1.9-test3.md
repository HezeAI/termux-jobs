# Termux Job’s 2.1.9-test3 build and validation

Private ARM64 build, 2026-11-21. Developer: HezeAI.
AI development assistance: OpenAI Codex.

## Source and changes

Imported the user-provided update against the verified 2.1.9-test2 source
snapshot. All 117 affected paths matched the recorded baselines before the
import and the requested state after it. See [the changelog](../../CHANGELOG.md)
for the shipped changes and [the handoff](../../CONTINUE.md) for device checks.

The initial Java build, Lint and JVM test staging runs hit an ambiguous
`ClipboardManager` type in the newly imported Markdown copy action.
The copy action now requests `android.content.ClipboardManager` directly.
Retained the initial logs and repeated the three affected checks. Verified
656 original Java, manifest, resource and asset files against all three
staged trees. Updated the private version to 2.1.9-test3 / 1543.

## Checked anew

- Isolated ARM64 release assembly with the project SDK 35, JDK 11, Gradle 7.2
  and compatible AAPT2. The release signer, Guava and pinned X11 guards ran.
- Modern Lint 7.4.2 across the five-module result: **0 errors, 298 warnings**.
  The app was reanalyzed. Legacy Lint diagnostics in the packaging log were
  not counted as a pass.
- Final APK ZIP integrity and 408 applicable stored entry offsets, package
  ID, version, SDK levels, Android permission declarations, non-debuggable
  status, ARM64 ABI, signature and SHA-256. The DEX inventory found 14,663
  definitions. The 31 unresolved types have existing review notes. All
  12 changed or added assets matched production bytes and all seven
  intended removals were absent. RTK's app classes were not packaged.
- Shell, JavaScript and Python syntax and localization checks passed.
  Offline fixtures exercised Codex and Claude chat and history flows, Codex
  context settings, the CLI updater, Antigravity preparation and worker
  protocol, and instruction storage. The instruction tests used the
  established test-only linkat compatibility path. The delivery fixture
  passed on retry after supplying the matching Gemini fixture chunk at the
  correct path. The stop fixtures verified that a mismatched PID or start
  time prevents signaling. The loopback download fixture verified cancellation,
  queue cleanup and retry. See the [fixture run log](../../.verification/test3-build/AGENT-FIXTURES.md).
- The focused JVM run completed 237 tests, with 220 passing and 17 failing.
  Some failures repeat older expectations for grouped timeline actions.
  The run also found failures in action dialogs and instruction screens, and
  six failures in scroll positioning and history anchors. Chat UI acceptance
  remains open. See the [test names and errors](../../.verification/test3-build/jvm-results.json).

## Reused and not checked

The three packaged native libraries match the preceding signed APK byte
for byte. Their sources and X11 build settings were compared with the
recorded baseline. Their ELF LOAD segments satisfy 16 KiB alignment. Local
build and analysis caches were reused for unchanged inputs.

Installation and phone UI, actual account sign-in, live provider calls and
working-environment Antigravity updates remain unverified. Existing Gemini
profile bindings keep their original route. Older Codex profiles use defaults
for new optional settings. The APK does not require a data reset. No public
update metadata was changed.

## Delivered artifact

- `/sdcard/Download/Termux-Jobs-v2.1.9-test3-arm64.apk`
- 40,687,423 bytes. SHA-256
  `c7eda44b3bdc839c381cdba2f0b54bc6b3067d620ee49d143eaffd667b49e038`.
- `com.termux`, arm64-v8a, min SDK 26, target SDK 28, compile SDK 35.
- Certificate SHA-256
  `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`,
  verified against test2. The copied bytes match the verified local APK.
- Raw logs and comparison results: `.verification/test3-build/`.
