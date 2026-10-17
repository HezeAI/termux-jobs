# v1.4.8-test — explicit local tool environment

Private test build, versionCode1114. No GitHub publication.

Local session/preset creation and active settings now offer **Main Termux** or **Ubuntu for local AI** independently of the project folder. Main Termux intentionally shares existing Termux packages. Ubuntu is a persistent dedicated `jobs-local-ai` container, created from a fresh Ubuntu image; it is not a copy of the working Ubuntu and contains no copied credentials/history/projects. The native model itself still runs in the Android inference process.

## First preparation

Selecting the Ubuntu option checks readiness. If unavailable, the app offers explicit preparation with an installation dialog, indeterminate progress bar, current stage and recent output. The app does not invent a total percentage. If proot-distro is missing, confirmation explicitly permits installing it/dependencies in Main Termux. Then proot-distro installs a fresh custom-name Ubuntu (24.04 OCI image on modern proot-distro, official Ubuntu distro plugin on supported legacy versions) and prepares bash/coreutils/findutils/CA certificates/Python3/Git plus `/root/projects`. Internet/free space are required; keep the app open. Confirmed setup is allowed to finish instead of being killed mid-package transaction. Closing a view does not terminate the installer. Android process termination/external interruption remain outside this guarantee.

Ready containers are reused without reinstalling. A private ownership record and in-container token/ready markers are checked. An existing unowned same-name container or rootfs symlink redirection is refused; no automatic adoption, removal, reset, clone or overwrite is performed. Interrupted preparation may retry package setup when ownership is intact. If interrupted before ownership could be recorded, manual diagnosis is required; existing data is preserved.

## Exact routing

Local tools accept only main Termux or the dedicated alias. Old local profiles pointing to `ubuntu`/other proot aliases retain their data/history but must explicitly reselect an environment; execution is blocked until then, without silently switching to Termux or the new Ubuntu. Changing environments resets the proposed project path to the new environment's default; it does not move files or erase chat history. Project browsing remains in the selected environment, including a shared-storage shortcut, rather than changing environments through the file picker.

Dedicated login uses `--isolated` and explicit readable shared-storage bindings. Working Ubuntu, Termux HOME/PREFIX and shared-tmp are not automatically mounted. Common phone storage remains shared when Android access permits. PRoot shares the Android kernel/UID and is not a strict security boundary against a deliberately hostile process. Arbitrary full-access commands and file scope retain the existing mode policy.

File reads/writes, searches, media/APK source copies, package APT plans/installations and project browsing all route through the selected environment. Missing/unready Ubuntu reports an error, with no fallback. Ordinary terminal/Codex/SSH/other proot sessions retain their previous routing. Only references to the new dedicated alias get the new isolated login.

Python/pip/venv/Git/make/cmake/clang/build-essential/pkg-config/curl/wget are added to the fixed user-confirmed package catalogue. Actual package availability depends on the selected environment's APT repositories; a failed plan does not install. Read-mode installation remains an explicit Yes/No exception with Confirm actions ON; arbitrary shell/file writes are still forbidden in Read mode.

## Compatibility and verification

Same package/certificate, higher code, no reset. Stored legacy local-environment values are preserved but intentionally blocked pending selection. Shared local chat/model files remain as before. Existing backup manager can enumerate proot containers, but restoration may create another alias and does not automatically re-establish this app's private ownership state; automatic local-AI environment reattachment after cross-app restore is not implemented. No new recovery APK requested.

On-device checks: select Ubuntu and confirm first setup/progress; check Python3/Git and a project file; restart and reuse without reinstall; verify Main Termux and dedicated Ubuntu show different package/file state, shared storage grants, failure/retry, old-profile reselection, and no effect on the working Ubuntu. Package/container installation was not run during automated source/build checks.

Reference: [official proot-distro install/login documentation](https://github.com/termux/proot-distro#commands-reference). Existing installed parser/session sources were read without changing them.
