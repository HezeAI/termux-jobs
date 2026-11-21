"""Test Ubuntu AI Antigravity preparation without networking, APT or real accounts."""

import hashlib
import os
from pathlib import Path
import shlex
import shutil
import subprocess
import tarfile
import tempfile


ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets"
BASH = shutil.which("bash")
EVIDENCE = ROOT / ".verification/antigravity-preparation"
EVIDENCE.mkdir(exist_ok=True, parents=True)
# Android emulated storage cannot preserve Unix modes, create symlinks or
# execute the installed test stand-in. A developer can supply an executable
# task-owned temporary root there. Logs still live under project verification.
FIXTURE_ROOT = Path(os.environ.get("JOBS_AGY_TEST_EXEC_DIR", EVIDENCE))
FIXTURE = Path(tempfile.mkdtemp(prefix="fixtures-", dir=FIXTURE_ROOT))
LOGS = EVIDENCE / FIXTURE.name
LOGS.mkdir(exist_ok=True)
COPIED = FIXTURE / "assets"
COPIED.mkdir()
for filename in (
    "agent-cli-resolver.sh", "agent-dependencies.sh", "agent-download.sh",
    "agent-preparation-status.sh", "antigravity-bootstrap.sh",
):
    shutil.copyfile(ASSETS / filename, COPIED / filename)
# The Android host has a different CA directory than Ubuntu. Supply the
# inventory prerequisite through the fixture and make real APT calls fail.
ca = FIXTURE / "ca-certificates.crt"
ca.touch()
dependencies = COPIED / "agent-dependencies.sh"
dependencies.write_text(dependencies.read_text().replace("/etc/ssl/certs/ca-certificates.crt", shlex.quote(str(ca))))
fixture_bin = FIXTURE / "bin"
fixture_bin.mkdir()
guest_python = fixture_bin / "python3"
guest_python.write_text(f"#!{BASH}\nprintf 'unexpected guest Python execution\\n' >&2\nexit 99\n")
guest_python.chmod(0o700)
guest_git = fixture_bin / "git"
guest_git.write_text(f"#!{BASH}\nprintf 'git version 2.53.0\\n'\n")
guest_git.chmod(0o700)

version = "1.5.13"
real_sha512 = (
    "dccea2eb455099f5175f0c87cc47b38fcefe4cb0cb44dea3927469bb4bbf68ac3c0b"
    "30fc04affb24c17e90959386a93c3785e8e4b8b1a3338ca4e4d57b80057b"
)
binary = FIXTURE / "antigravity"
binary.write_text(f"""#!{BASH}
[[ -z ${{SYNTHETIC_PRIVATE:-}} ]] || exit 1
[[ $AGY_CLI_DISABLE_AUTO_UPDATE == true && $PWD == "$HOME" ]] || exit 1
case $1 in
    --version) printf 'agy version {version}\\n';;
    --help) printf '%s\\n' stream-json --input-format --output-format --mode --conversation >&2;;
    *) exit 1;;
esac
""")
archive = FIXTURE / "agy.tar.gz"
with tarfile.open(archive, "w:gz") as tar:
    tar.add(binary, arcname="antigravity")
bootstrap = COPIED / "antigravity-bootstrap.sh"
production = bootstrap.read_text()
assert production.count(real_sha512) == 1
bootstrap.write_text(production.replace(real_sha512, hashlib.sha512(archive.read_bytes()).hexdigest()))

# Simulate only the externally provided commands that differ on the fixture
# host. The production helper still runs digest verification, the staging
# transaction, and the independent CLI probe.
preamble = r'''
set -e
export JOBS_AGENT_UBUNTU=ubuntu-ai SYNTHETIC_PRIVATE=private-value
source "$1/agent-dependencies.sh"
xz() { return 99; }
uname() { [[ $1 == -m ]] && printf '%s\n' aarch64; }
if [[ $(flock --help 2>&1) == *Toybox* ]]; then
    flock() {
        if [[ $1 == -w && $2 == 300 ]]; then shift 2; fi
        command flock -n "$@"
    }
fi
apt-get() { printf 'unexpected APT\n' >&2; return 99; }
curl() {
    local target
    while (($#)); do
        if [[ $1 == --output ]]; then shift; target=$1; fi
        shift
    done
    printf 'download\n' >> "$HOME/downloads"
    case ${MODE:-good} in
        corrupt) printf 'corrupt\n' > "$target";;
        interrupted) return 130;;
        *) cp -- "$TEST_ARCHIVE" "$target";;
    esac
}
'''


def run(label, body, *, home=None, success=True, extra=None):
    home = home or FIXTURE / label
    home.mkdir(exist_ok=True)
    env = {key: os.environ[key] for key in ("PATH", "LD_LIBRARY_PATH", "TMPDIR") if key in os.environ}
    env["PATH"] = f"{fixture_bin}{os.pathsep}{env['PATH']}"
    env.update(HOME=str(home), TEST_ARCHIVE=str(archive), OPENSSL_CONF=os.devnull)
    env.update(extra or {})
    res = subprocess.run(
        [BASH, "--noprofile", "--norc", "-c", preamble + "\n" + body, label, str(COPIED)],
        env=env, cwd=FIXTURE, capture_output=True, text=True, timeout=45,
    )
    (LOGS / (label + ".log")).write_text(f"exit={res.returncode}\n{res.stdout}\n{res.stderr}")
    assert (res.returncode == 0) == success, (label, res.returncode, res.stdout, res.stderr)
    assert "private-value" not in res.stdout + res.stderr
    assert "unexpected APT" not in res.stderr
    assert "unexpected guest Python execution" not in res.stderr
    return home, res.stdout + res.stderr


def private_binary(home):
    return home / ".local/share/termux-jobs/antigravity-cli" / version / "agy"


home, inventory = run("inventory", "jobs_agent_check gemini antigravity")
assert "MISSING:agy-cli" in inventory and "CHECK:done" in inventory
assert "READY:python3" in inventory.splitlines()
assert "READY:git" in inventory.splitlines()
assert "gemini-cli" not in inventory and "AUTH:" not in inventory and "gcloud" not in inventory
assert not list(home.iterdir())

run("legacy-ubuntu-blocked", "JOBS_AGENT_UBUNTU=ubuntu jobs_agent_check gemini antigravity", success=False)
home, _ = run("refused", "jobs_agent_prepare gemini antigravity", success=False)
assert not list(home.iterdir())

missing_python = r'''
command() {
    if [[ $1 == -v && ${2:-} == python3 ]]; then return 1; fi
    builtin command "$@"
}
jobs_agent_check gemini antigravity
'''
home, output = run("missing-python-refused", missing_python + "jobs_agent_prepare gemini antigravity", success=False)
assert "MISSING:python3" in output.splitlines() and not list(home.iterdir())
home, output = run("missing-python-approved", missing_python + r'''
apt-get() { printf '%s\n' "$*" >> "$HOME/apt"; }
JOBS_AGENT_SETUP_ALLOWED=1 jobs_agent_prepare gemini antigravity
''')
assert "PREPARATION:ready" in output.splitlines()
assert (home / "apt").read_text().splitlines() == [
    "update", "install -y --no-install-recommends -o Dpkg::Options::=--force-confold python3",
]

home, output = run("corrupt", r'''
tar() { printf 'archive extracted without integrity verification\n' >&2; return 99; }
JOBS_AGENT_SETUP_ALLOWED=1 jobs_agent_prepare gemini antigravity
''', success=False, extra={"MODE": "corrupt"})
assert "ERROR:COMPONENT_INTEGRITY_FAILED" in output
assert "archive extracted" not in output and not private_binary(home).exists()

home, output = run(
    "interrupted", "JOBS_AGENT_SETUP_ALLOWED=1 jobs_agent_prepare gemini antigravity",
    success=False, extra={"MODE": "interrupted"},
)
assert "ERROR:COMPONENT_DOWNLOAD_INTERRUPTED" in output and not private_binary(home).exists()
assert not list(private_binary(home).parents[1].glob(".install-*"))

home, output = run("installed", r'''
export JOBS_AGENT_SETUP_ALLOWED=1
jobs_agent_prepare gemini antigravity
[[ $JOBS_AGY_BIN == "$(jobs_agy_pinned_binary)" ]]
[[ $JOBS_AGY_NODE == /* && -x $JOBS_AGY_NODE ]]
[[ $AGY_CLI_DISABLE_AUTO_UPDATE == true ]]
jobs_agent_check gemini antigravity
''')
assert "PREPARATION:checking-cli" in output and "PREPARATION:ready" in output
assert "READY:agy-cli" in output and private_binary(home).stat().st_mode & 0o777 == 0o700
assert private_binary(home).read_bytes() == binary.read_bytes()
run("retry", "jobs_agent_prepare gemini antigravity", home=home)
assert (home / "downloads").read_text().splitlines() == ["download"]

managed = home / ".local/share/termux-jobs/cli-updates/antigravity/1.6.0/agy"
managed.parent.mkdir(parents=True)
shutil.copyfile(private_binary(home), managed)
managed.chmod(0o700)
managed.parent.parent.joinpath("current").symlink_to("1.6.0")
run("activation", r'''
jobs_agent_prepare gemini antigravity
[[ $JOBS_AGY_BIN == */cli-updates/antigravity/1.6.0/agy ]]
''', home=home)

broken_home = FIXTURE / "probe-failure"
bad_probe = FIXTURE / "incorrect-version"
bad_probe.write_text(f"#!{BASH}\nprintf 'agy version 1.5.130\\n'\n")
bad_probe.chmod(0o700)
run(
    "probe-failure", 'jobs_agy_probe "$BAD_PROBE" 1.5.13 "$HOME/private"',
    home=broken_home, extra={"BAD_PROBE": str(bad_probe)}, success=False,
)

missing_capability = FIXTURE / "missing-conversation-option"
missing_capability.write_text(binary.read_text().replace(" --conversation", ""))
missing_capability.chmod(0o700)
run(
    "missing-capability", 'jobs_agy_probe "$BAD_PROBE" 1.5.13 "$HOME/private"',
    extra={"BAD_PROBE": str(missing_capability)}, success=False,
)

failed_help = FIXTURE / "failed-help-binary"
failed_help.write_text(binary.read_text().replace("--conversation >&2;;", "--conversation >&2; exit 9;;"))
failed_help.chmod(0o700)
run(
    "failed-help", 'jobs_agy_probe "$BAD_PROBE" 1.5.13 "$HOME/private"',
    extra={"BAD_PROBE": str(failed_help)}, success=False,
)

print("PASS Antigravity preparation: inventory isolation, explicit consent, guest Python dependency, checksum failure, "
      "interruption, private publication, verified CLI startup shape, existing runtime reuse and managed selection")
