# Termux Job’s 2.1.9-test8 validation

Private ARM64 build, 2026-11-21. Developer: HezeAI.
AI development assistance: OpenAI Codex.

Artifact: `Download/Termux-Jobs-v2.1.9-test8-arm64.apk` (44,005,975 bytes).
SHA-256 `4a662e0e9cb3df772b0e1a9c64db93da3392565a8e8f0d9e7795e22c51e62529`.

## Scope

The developer's phone showed `.git` in the project folder and a separate
missing-Git message in **Changes**. Repository metadata existed. The file
worker could not find the Git executable on the Ubuntu AI command path.

This private build specifies the already available Ubuntu AI Git executable
and its SHA-256. An absolute Git on the selected environment's `PATH` has
precedence. The fallback is considered for Ubuntu AI Codex, Claude and
Antigravity profiles. The change lists, diffs and metadata use the selected
profile and repository. A matched fallback directory is also available in
the corresponding agent launch process. The APK does not carry the Git binary.
Builds without the private configuration use the environment Git.
The Ubuntu AI readiness check reports missing Git for Codex, Claude and
Antigravity. The existing preparation confirmation authorizes installing the
Ubuntu Git package through APT if no eligible executable is available.

Creating a genuinely new Codex, Claude or Antigravity chat in Ubuntu AI starts
Git initialization in its chosen existing project folder before the agent.
The filesystem root, home and ancestors are excluded. The app preserves
existing repository metadata, including a parent worktree and broken `.git`.
It builds new metadata in a temporary same-directory worktree and publishes
it without replacing an existing `.git`. Initialization uses no templates,
leaves files unstaged and creates no commit. The Changes tab lists status and
diffs before the first commit. Reopening or resuming a chat does not request
initialization. Chats using the same worktree naturally see the same Git changes.
An agent can stage and commit there with effective write permissions and a
configured Git author name and email.

## Checked anew

- The selected Ubuntu AI Git executable is Git 2.53.0, AArch64 ELF with the
  glibc loader. `ldd` resolved libpcre2-8, zlib and libc. The executable ran
  successfully and matched the configured SHA-256.
- Isolated shell checks exercised fallback digest rejection, absolute PATH
  precedence, protected directories, preservation of an existing destination,
  and unborn staged and unstaged diffs with real Git.
- Preparation fixtures checked Git recognition for the three managed agents,
  rejection of a mismatched digest or symlink, preservation of other
  environments, and the approved Git-only package request. They also verified
  that a stale readiness result cannot bypass installation consent.
- Host preparation passed 19 tests and six stage scenarios, including
  guest argument boundaries for a Git path containing spaces and an
  apostrophe. Fixtures now supply the existing Python executable explicitly
  because the simulated host PATH excludes the Termux binary directory.
- In the pinned Ubuntu AI 26.04 ARM64 image, both Claude and Antigravity
  reported missing Git and refused preparation without consent. Real Ubuntu
  APT fetched the packages and installed Git 1:2.53.0-1ubuntu1.1 with the
  package manager pointed at the disposable image. Its GLIBC dependencies
  resolved and native Git initialized a project and listed an untracked file.
  Both agents then reported ready Git. The live package database hash matched
  before and after the fixture. A Git-only APT transaction using the signed
  package cache exited successfully on a second freshly extracted image.
  The loader and native Git operation checks passed there again.
  The local fixture report is `.verification/test8-project-git/clean-apt/result.md`.
- The Java, ARM64 build and modern Lint stages each matched 979 source inputs.
- RU/EN resource consistency passed.
- The focused JVM checks covered 65 tests. All 53 non-Git tests passed in
  the combined run. All 12 Git tests passed after adjusting the fixtures
  for template-free Git and to isolate the checkout's parent repository.
  They verified a first commit in a fresh, separate project with real Git.
- The final ARM64 APK built in 3 minutes 24 seconds. Modern Lint analyzed all
  five modules with zero errors and 298 warnings. The APK verified as a
  non-debuggable `com.termux` release, version `2.1.9-test8` (1548), with the
  same signer and manifest permissions as test7. ZIP integrity and alignment,
  all three ARM64 ELF segment alignments, packaged assets, and required DEX
  runtime definitions passed. The copy in Download matched the verified SHA-256.

The build, fixture failures and retries, command output and final-file
evidence are under `.verification/test8-project-git/` in the build checkout.

## Reused

The ARM64 bootstrap and terminal JNI libraries were reused after comparing
the native source inputs with their recorded baseline. Final archive
verification compares both libraries with the preceding signed APK.

## Not checked

APK installation, phone UI acceptance and live agent behavior remain pending.
The private Git pin applies to the checked Ubuntu AI installation. Other
installations use Git from their selected Ubuntu AI environment.
Full clean environment preparation remains unverified. The direct package
transaction under nested chroot failed on package-file ownership. The
explicit-root retry installed Git but encountered maintainer-script errors
for other packages. The package results do not establish clean phone startup.
