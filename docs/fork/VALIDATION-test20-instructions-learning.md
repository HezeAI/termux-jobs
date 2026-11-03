# Private test20 — general templates and project memory

VersionName 2.1.1-test20; versionCode1455. Candidate built, not installed or published.
The explicitly supplied project-documentation organization section is included in RU/EN. At the user's explicit request it was also appended once to the actual global /root/.codex/AGENTS.md through the existing atomic/history backend. Previous text is retained byte for byte as a prefix, and its previous revision was verified in history. Other users' saved instructions are not automatically replaced. The project AGENTS.md was not split.

## Checked anew

- 42 Python backend tests: missing-file read without creation, creation/save, exact text, per-path history, restoration, stale revision/source changes, custom memory path, relative references without execution, override independence, no automatic replacement on template upgrade, import-size and file safety.
- 49 Java/UI tests passed before the final Markdown-only addition; after it, 11 affected template/static/UI cases were rerun and passed: actual editor/backend requests, template/apply/cancel, import, conflict, history recovery, Back/outside and explicit discard guards, busy-save handling, per-file drafts, memory selection persistence and environment validation.
- RU/EN model layout tests at 280/320/360 dp, font scales 100/130/150 percent and reduced height; all three file names remain single-line, selected and horizontally reachable. These are model checks, not real phone IME/touch tests.
- Final build succeeded. Modern Lint 7.4.2: all five modules, 0 errors, 234 warnings, no infrastructure errors. After the last Markdown-only addition its result was reused against identical production Java/XML/Python/Node sources; no code, configuration or dependency changed. Older plugin/compileSdk warnings remain. The build reported duplicate identical libXlorie inputs; final native bytes match v2.1.1.
- ZIP, signature, package, version, ABI, debug status, assets, runtime inventory and SHA-256 checked on the exact APK. No required or unreviewed missing runtime types.
- Reviewed RU memory matches the supplied template byte for byte. RU instructions retain the supplied text plus the exact new documentation section. EN follows all corresponding rules and sections, with semantic review. The supplied RU template contains an extra final blank line; git diff --check flags that preserved source whitespace, not a code error.

## Artifact

File: Termux-Jobs-test20.apk (39976042 bytes). Package com.termux; ARM64 only; non-debuggable.
SHA-256: `b29cad7985a22f0a8dbd0c5ff144801e90432a692a4fdfd9dc47bd60d7786486`.
Certificate SHA-256: `dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`; same as v2.1.1.
Workspace formats and existing user instruction files remain unchanged. Memory path selections are editor preferences, not automatic instruction loading or an expanded backup format.

## Reused

Three packaged native libraries and 106 other assets match the delivered v2.1.1 APK byte for byte. No native-source changes, new SDK copies, environment installation or graphics execution. Changed assets are the helper, RU/EN general templates and RU/EN memory templates. Existing history/atomic-save infrastructure and Codex instruction-priority behavior remain; memory is excluded from the instruction chain.

## Not checked

Real device keyboard, touch, accessibility speech, actual narrow/large-font usability and restart acceptance need phone testing. No claim of live model adherence or a new CLI-version capability. No working session, tunnel, installed environment or authentication was changed for testing. The separate authorized global-file addition is described above. The shared project memory remains /root/termux-fork/LEARNING.md; no checkout duplicate was created.

## Initial results retained

The first Python run rejected changed EN cleanup wording; explicit authorization wording was restored and the suite passed. The first UI run expected the former shared file explanation; it was updated to check the new per-file explanations, then passed. Initial XML/logs and successful reruns are kept under `.verification/instructions-learning-test20/`. The apksigner launcher lacked execute permission; invoking the existing script through Bash verified the APK without changing tool permissions.
