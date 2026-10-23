# v2.0.1 — Fix Wrapper

- Fix GPU content drawing over other windows in Termux Wrapper, VirGL and Linux-guest Turnip desktops using clipped presentation.
- Fix backup preset merging and detached histories; verify restored settings after saving.
- Validate renderer components before launch and offer consented repair for damaged Turnip caches.

Android8+/ARM64; same package/signature, code1325, no data reset. Repeated imports create separate preset copies. Restart the desktop after updating. Frame copying may use more CPU.

[Checks and compatibility](https://github.com/HezeAI/termux-jobs/blob/v2.0.1/docs/fork/VALIDATION-v2.0.1.md).

AI development assistance: OpenAI Codex
