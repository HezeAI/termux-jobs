# Environment and package installer audit — 2026-10-23

Private2.0.9-test/code1338 extends the preceding
[update/entrypoint](AUDIT-UPDATES-ENTRYPOINTS-2026-10-23.md) audit and retains all
previous audit changes. Public stable remains v2.0.2/code1331. Source edits and
repository-private fixtures do not change installed environments, accounts or
running sessions. This is a targeted audit, not a claim of exhaustive stability.

## Findings and changes

**An exited installer leader could leave a descendant working.** The Python
supervisor used `Popen.poll()`/`wait()` before its last group-cleanup signal. A
normal/failing leader could exit with descendants still alive, and the exception
cleanup could reap a terminated leader before killing TERM-ignoring descendants.
Signaling its numeric process-group ID after reaping would also risk PID reuse.

The supervisor now observes exit with `waitid(..., WNOWAIT)` and does not reap
until the final owned-group signal. Its own `start_new_session` leader remains
reserved throughout cleanup on normal exit, installer error, output error and
cancellation. Exit status is preserved; cancellation retains the existing INT →
TERM → KILL grace. SIGCHLD uses its default disposition so inherited ignore cannot
silently reap the leader. Missing non-reaping-wait APIs fail before starting a
child. PTY descriptors close in a final block after process cleanup. No lookup or
signal of borrowed session PIDs or package-manager lock owners was added.

**A link was sufficient to report a ready shell.** The environment check accepted
any `/bin/sh` symbolic link, including a broken one/loop. Host `-x` could also
follow an absolute guest link into the host's `/bin` instead of the selected rootfs.

Shell readiness now resolves every path component relative to the selected guest
root, including merged-usr directory links. Absolute links restart at that guest's
root; `..` stays inside it; the link count is bounded. The final target must be a
regular executable. This only inspects files; it does not execute an imported
binary, install dependencies, remove an incomplete root or test the guest's loader.
A missing/nonexecutable shell is reported as missing rather than as installed.

**Output/notification failures could release the Java operation too early.**
After starting a process, stdin-close, output-read, progress callback or interrupted
wait errors could leave the worker without waiting for the process it had started.
The UI could then admit another attempt while the previous one still worked.

EnvironmentSetup now collects output through one lifecycle boundary. On failure
it requests cancellation using only that job's private marker, closes its pipes
and waits for actual child exit before propagating failure/releasing the operation.
Interrupted cleanup waits continue and restore the interrupt flag after exit.
Ordinary successful collection preserves output/exit status and does not create a
cancel marker. Prerequisite preparation without a supervisor is awaited naturally;
this change does not invent a broad process-tree kill for that path.

**Screens shared writable runtime/control script paths.** A second controller
could truncate/rewrite `runtime.sh` or the Python supervisor while an earlier
invocation still used them. Every EnvironmentSetup installation/check now gets a separate private
`job-*` execution directory. A running installation's cancel marker is in the same
owned directory as its scripts. Cleanup deletes only that invocation's fixed
files and directory, after child completion. Persistent download/cache and attempt
metadata paths stay at the existing control root; other jobs/retained layers are
not recursively deleted. A changed Activity does not gain ownership of an old
job's cancel file.

## Evidence and review

Evidence is retained under `.verification/installers-audit-20261023/`.
Prior-code reproductions failed five of36 Python cases: live descendants after
successful/failing leaders and false shell readiness for broken/absolute/looping
links. A separate injected supervisor-output failure also reproduced an abandoned
descendant on the prior script. The first Java lifecycle run failed four of five
new cases: read/callback/stdin-close failure and interrupted wait. Normal collection
already passed. Reproductions use private fixtures, not installed package managers.

Final production code passed **52 scoped JVM tests in9 classes** and **80 Python
tests in8 modules**, without failures/errors/skips. APK
`build/apk/Termux-Jobs-v2.0.9-test-arm64.apk`:45,837,706bytes,
SHA-256 `33e376e92bdeba4069ce3e28024efd1ccc26b1301cae8b9a8cee7a98fff542d4`.
ZIP integrity, package/version/ARM64, signer continuity, runtime-reference and
bounded secret-pattern checks passed. Native libraries/all other assets match1337;
only the two intended environment scripts differ. Tested/staged sources match.
The build succeeded with the existing custom-lint/Kotlin metadata warnings, which
are not described as clean lint. See VALIDATION.md for the evidence location.

The Python set includes
real short-lived process groups with an unrelated sleep process kept alive, an
installer's KeyboardInterrupt cleanup, escalation against ignored signals, normal
and failing leaders, injected output failure, shell-link layouts and exit status.
Java tests hold a synthetic process behind a latch and verify that error/interrupt
paths remain blocked until release, that no direct PID signal is used, and that
success does not request cancellation. Overlapping execution directories preserve
each other's scripts/markers and retained download data.

The broader fixture matrix checks routing to each selected rootfs, complete
required desktop-package requests despite existing package state, absent required
binaries, recommended-program dependencies, protected removals, resume/index
handling and configured-package validation. The Arch download fixtures verify
HTTPS-only command construction, checksum-before-install ordering, refusal on
reported download/digest/installer failure and temporary archive cleanup. They
stub the downloader/checksummer and do not download or extract an actual image.
OCI installs remain delegated to public proot-distro CLI; the retired custom
prefetch/private callback mechanism is not restored.

An existing loopback-only APT acquire test downloads fixture bytes using apt-helper:
transient503 retries succeed, while a bad digest fails without being retried as a
transient failure. No package is installed. These checks do not prove remote
mirror availability or download speed. Wrapper source retains package-manager
signature/dependency checks and existing bounded network retries; it adds no
`--allow-unauthenticated`, untrusted-package or TLS-bypass flag.

Traced job admission, per-invocation staging, process start, output delivery,
stop marker, INT/TERM/KILL escalation, non-reaping status observation, final group
cleanup/reap, terminal callback, retry/retained cache and readiness inspection.
Only the new invocation's scripts are removed; another environment, agent,
Telegram bot or terminal session is outside that ownership boundary.

## Limits and minimum phone checks

- Process fixtures use this host/proot interpreter. Termux Python's non-reaping-wait
  capability and Android app installation/cancellation UI remain device checks.
  Deliberately detached processes that create a different session are outside the
  owned group; the supervisor does not hunt for arbitrary descendant PIDs.
- If Python must first be installed, that prerequisite precedes the Python
  supervisor. Stop is observed before the environment child starts, but does not
  immediately cancel the prerequisite package transaction. A missing/unwritable
  cancellation marker or hung prerequisite can extend the wait; the operation is
  not prematurely declared stopped/retryable.
- Controller recreation still does not provide a durable global UI job registry.
  Old work may finish with its screen closed; new invocations have independent
  scripts and the native package manager/proot-distro retains its locking rules.
  Force-stop/power loss may leave small job directories; uncertain ownership is
  not used as a reason to purge them automatically.
- Shell readiness is not an execution/ELF-loader/architecture/full-package health
  check. Native CLI caches can reuse complete verified layers; interrupted layers
  may restart. Existing incomplete rootfs removal remains explicit, never automatic.
- Fresh Android installs of every environment/desktop, real upstream archives,
  account login, mirror outages and power-loss recovery were not repeated here.
  Existing configured-phone successes remain regression evidence only.

On the next consented device installation, check inline Stop → Stopping → Resume,
then resume without altering another running environment. Check existing/missing
shell status through the environment button. Do not remove a working rootfs merely
to manufacture a test. All previous audit fixes remain included; valid workspace,
account and environment data needs no reset or automatic migration.

Future stable compatibility wording:
- EN: No data reset or automatic environment replacement. Incomplete environments
  with a missing/broken shell are now reported as not ready; repair or remove them
  explicitly. Previously downloaded native layers remain managed by proot-distro.
- RU: Сброс данных и автоматическая замена окружений не нужны. Незавершённые системы
  с отсутствующей или повреждённой оболочкой теперь отмечаются как неготовые;
  исправление или удаление выполняется явно. Скачанными слоями управляет proot-distro.

Developer: HezeAI. AI development assistance: OpenAI Codex
