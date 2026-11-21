"""Check official releases and install an explicitly selected Linux ARM64 CLI."""
import base64
import fcntl
import hashlib
import hmac
import json
import os
from pathlib import Path
import re
import shutil
import stat
import struct
import subprocess
import sys
import tarfile
import tempfile

REGISTRY = "https://registry.npmjs.org"
AGY_MANIFEST = "https://antigravity-cli-auto-updater-974169037036.us-central1.run.app/manifests/linux_arm64.json"
AGY_ARCHIVES = "https://storage.googleapis.com/antigravity-public/antigravity-cli"
SYSTEM_PATH = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
VERSION = r"(?:0|[1-9][0-9]{0,8})(?:\.(?:0|[1-9][0-9]{0,8})){2}"
MAX_METADATA = 1024 * 1024
MAX_ARCHIVE = 512 * 1024 * 1024
MAX_EXTRACTED = 768 * 1024 * 1024
CODEX_BINARY = "vendor/aarch64-unknown-linux-musl/bin/codex"
CODEX_VOICE = ("package", "vendor", "aarch64-unknown-linux-musl", "codex-resources", "voice")
BINARIES = {"codex": CODEX_BINARY, "claude": "claude", "antigravity": "agy"}


class UpdateFailure(Exception):
    pass


def require(condition, code):
    if not condition:
        raise UpdateFailure(code)


def stable(version):
    return isinstance(version, str) and re.fullmatch(VERSION, version) is not None


def version_key(version):
    return tuple(map(int, version.split(".")))


def download(curl, url, dest, limit, seconds):
    # Ignore curl configuration and credentials, proxy and certificate overrides.
    # Do not follow redirects to a different origin.
    try:
        subprocess.run([curl, "-q", "--fail", "--silent", "--proto", "=https",
                        "--connect-timeout", "10", "--max-time", str(seconds),
                        "--max-filesize", str(limit), "--output", str(dest), "--", url],
                       env={"PATH": SYSTEM_PATH, "LC_ALL": "C"}, cwd=dest.parent,
                       stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL, check=True, timeout=seconds + 3)
        require(0 < dest.stat().st_size <= limit, "REGISTRY")
    except (OSError, subprocess.SubprocessError) as error:
        raise UpdateFailure("REGISTRY") from error


def read_metadata(curl, stage, url):
    file = stage / "metadata.json"
    download(curl, url, file, MAX_METADATA, 13)
    try:
        data = json.loads(file.read_bytes())
    except (ValueError, RecursionError) as error:
        raise UpdateFailure("PACKAGE") from error
    require(isinstance(data, dict), "PACKAGE")
    return data


def metadata(curl, stage, package, tag):
    data = read_metadata(curl, stage, f"{REGISTRY}/{package}/{tag}")
    require(data.get("name") == package, "PACKAGE")
    return data


def antigravity_release(curl, stage, expected):
    manifest = read_metadata(curl, stage, AGY_MANIFEST)
    version = manifest.get("version")
    require(stable(version), "PACKAGE")
    require(expected is None or expected == version, "VERSION_CHANGED")
    url = manifest.get("url")
    require(isinstance(url, str) and re.fullmatch(
        rf"{re.escape(AGY_ARCHIVES + '/' + version)}-[0-9]{{1,30}}/linux-arm/cli_linux_arm64\.tar\.gz", url),
        "PACKAGE")
    digest = manifest.get("sha512")
    require(isinstance(digest, str) and re.fullmatch(r"[a-f0-9]{128}", digest), "PACKAGE")
    return version, url, bytes.fromhex(digest), None


def release(curl, stage, agent, expected):
    if agent == "antigravity":
        return antigravity_release(curl, stage, expected)
    parent = "@openai/codex" if agent == "codex" else "@anthropic-ai/claude-code"
    latest = metadata(curl, stage, parent, "latest")
    version = latest.get("version")
    require(stable(version), "PACKAGE")
    require(expected is None or expected == version, "VERSION_CHANGED")
    deps = latest.get("optionalDependencies")
    require(isinstance(deps, dict), "PACKAGE")
    dependency = parent + "-linux-arm64"
    if agent == "codex":
        package = parent
        tag = version + "-linux-arm64"
        require(deps.get(dependency) == f"npm:{package}@{tag}", "PACKAGE")
    else:
        package = dependency
        tag = version
        require(deps.get(dependency) == tag, "PACKAGE")
    native = metadata(curl, stage, package, tag)
    require(native.get("version") == tag and native.get("os") == ["linux"]
            and native.get("cpu") == ["arm64"], "PACKAGE")
    if agent == "claude":
        require(native.get("libc") == ["glibc"], "PACKAGE")
    dist = native.get("dist")
    require(isinstance(dist, dict), "PACKAGE")
    tarball = f"{REGISTRY}/{package}/-/{package.split('/')[1]}-{tag}.tgz"
    require(dist.get("tarball") == tarball, "PACKAGE")
    integrity = dist.get("integrity")
    require(isinstance(integrity, str)
            and re.fullmatch(r"sha512-[A-Za-z0-9+/]{86}==", integrity), "PACKAGE")
    digest = base64.b64decode(integrity[7:], validate=True)
    require(base64.b64encode(digest).decode("ascii") == integrity[7:], "PACKAGE")
    expected_size = dist.get("unpackedSize")
    require(type(expected_size) is int and 0 < expected_size <= MAX_EXTRACTED, "PACKAGE")
    return version, tarball, digest, expected_size


def extract(archive, runtime, expected_size):
    runtime.mkdir(mode=0o700)
    seen = set()
    total = 0
    # Write members ourselves. Do not follow links or restore archive ownership,
    # special modes, or timestamps.
    with tarfile.open(archive, mode="r|gz") as stream:
        for member in stream:
            components = member.name.split("/")
            require(all(re.fullmatch(r"[A-Za-z0-9._+-]{1,180}", name)
                        and name not in (".", "..") for name in components)
                    and components[0] == "package" and len(components) <= 20,
                    "PACKAGE")
            require((member.isdir() or member.isreg()) and not member.issparse()
                    and member.name not in seen and len(seen) < 1024, "PACKAGE")
            seen.add(member.name)
            dest = runtime.joinpath(*components[1:])
            voice_resource = tuple(components[:5]) == CODEX_VOICE
            if member.isdir():
                if not voice_resource:
                    dest.mkdir(mode=0o700, parents=True, exist_ok=True)
                continue
            require(len(components) > 1 and 0 <= member.size <= expected_size, "PACKAGE")
            total += member.size
            require(total <= expected_size, "PACKAGE")
            if voice_resource:
                continue
            dest.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
            with stream.extractfile(member) as source, dest.open("xb") as output:
                shutil.copyfileobj(source, output, 1024 * 1024)
            if member.mode & 0o111:
                dest.chmod(0o700)
    require(total == expected_size, "PACKAGE")


def extract_antigravity(archive, runtime):
    runtime.mkdir(mode=0o700)
    size = 0
    dest = runtime / BINARIES["antigravity"]
    with tarfile.open(archive, mode="r|gz") as stream:
        for count, member in enumerate(stream):
            require(count < 1024 and 0 <= member.size <= MAX_EXTRACTED - size, "PACKAGE")
            size += member.size
            if member.name != "antigravity":
                continue
            require(not dest.exists() and member.isreg() and not member.issparse()
                    and member.size > 0 and member.mode & 0o111, "PACKAGE")
            with stream.extractfile(member) as source, dest.open("xb") as output:
                shutil.copyfileobj(source, output, 1024 * 1024)
    require(dest.is_file(), "PACKAGE")
    dest.chmod(0o700)


def probe_output(binary, stage, node, arguments, limit, *, capture_stderr=False):
    home = stage / "probe"
    home.mkdir(mode=0o700, exist_ok=True)
    env = {"PATH": (str(node.parent) + ":" if node else "") + SYSTEM_PATH,
           "HOME": str(home), "TMPDIR": str(home), "LC_ALL": "C",
           "XDG_CONFIG_HOME": str(home / "config"), "XDG_DATA_HOME": str(home / "data"),
           "XDG_CACHE_HOME": str(home / "cache"), "XDG_STATE_HOME": str(home / "state"),
           "CODEX_HOME": str(home / "codex"), "CLAUDE_CONFIG_DIR": str(home / "claude"),
           "DISABLE_AUTOUPDATER": "1", "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1",
           "AGY_CLI_DISABLE_AUTO_UPDATE": "true", "GODEBUG": "asyncpreemptoff=1", "GOMAXPROCS": "2"}
    with tempfile.TemporaryFile(dir=home) as output:
        try:
            stderr = subprocess.STDOUT if capture_stderr else subprocess.DEVNULL
            subprocess.run([binary, *arguments], env=env, cwd=home, stdin=subprocess.DEVNULL,
                           stdout=output, stderr=stderr, check=True, timeout=10)
            output.seek(0)
            data = output.read(limit + 1)
            require(len(data) <= limit, "VERIFY")
            return data
        except (OSError, subprocess.SubprocessError) as error:
            raise UpdateFailure("VERIFY") from error


def probe(binary, stage, agent, node):
    data = probe_output(binary, stage, node, ["--version"], 128)
    if agent == "codex":
        pattern = rf"codex-cli ({VERSION})\n"
    elif agent == "claude":
        pattern = rf"({VERSION}) \(Claude Code\)\n"
    else:
        pattern = rf"(?:[\x20-\x7e]{{1,64}} )?({VERSION})\n"
    match = re.fullmatch(pattern.encode("ascii"), data)
    require(match is not None, "VERIFY")
    return match[1].decode("ascii")


def verify_runtime(binary, stage, agent, node, version):
    require(binary.is_file() and binary.resolve() == binary and os.access(binary, os.X_OK),
            "VERIFY")
    with binary.open("rb") as stream:
        header = stream.read(64)
    require(len(header) == 64 and header[:7] == b"\x7fELF\x02\x01\x01"
            and header[7] in (0, 3), "VERIFY")
    elf_type, machine, revision = struct.unpack_from("<HHI", header, 16)
    require(elf_type in (2, 3) and machine == 183 and revision == 1, "VERIFY")
    require(probe(binary, stage, agent, node) == version, "VERIFY")
    if agent == "codex":
        arguments = ["--no-daemon", "app-server", "--stdio", "--help"]
        flags = [b"--stdio", b"--listen"]
    elif agent == "claude":
        arguments = ["--print", "--input-format", "stream-json", "--output-format", "stream-json",
                     "--verbose", "--permission-prompt-tool", "stdio", "--include-partial-messages",
                     "--replay-user-messages", "--permission-mode", "manual", "--help"]
        flags = [b"--input-format", b"--output-format", b"--include-partial-messages",
                 b"--permission-mode", b"--replay-user-messages", b"--session-id"]
    else:
        arguments = ["--help"]
        flags = [b"--input-format", b"--output-format", b"--conversation", b"--mode", b"stream-json"]
    # Antigravity prints CLI help to stderr even when the command succeeds.
    help_output = probe_output(binary, stage, node, arguments, 64 * 1024,
                               capture_stderr=agent == "antigravity")
    require(all(flag in help_output for flag in flags), "VERIFY")


def resolved_cli(agent, home, path):
    resolver = Path(__file__).with_name("agent-cli-resolver.sh")
    run = subprocess.run(["/bin/sh", "-c", '. "$1" || exit 2; jobs_agent_cli_binary "$2"',
                          "agent-cli-resolver", resolver, agent],
                         env={"HOME": str(home), "PATH": path, "LC_ALL": "C",
                              "JOBS_AGENT_UBUNTU": "ubuntu-ai"},
                         stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                         stderr=subprocess.DEVNULL, check=False, timeout=5)
    require(run.returncode in (0, 1) and len(run.stdout) <= 4096, "STATE")
    if run.returncode == 1:
        return None
    binary = Path(os.fsdecode(run.stdout.rstrip(b"\n")))
    require(binary.is_absolute(), "STATE")
    return binary


def status(current, latest, result):
    print(f"CURRENT:{current}\nLATEST:{latest}\nRESULT:{result}")


def install(curl, root, agent, expected, home, path, node):
    directory = home
    for part in root.relative_to(home).parts:
        directory /= part
        require(not directory.is_symlink(), "STATE")
        directory.mkdir(mode=0o700, exist_ok=True)
        require(directory.is_dir() and directory.resolve() == directory, "STATE")
    # A persistent lock inode lets independent chats serialize updates without
    # interfering with the existing bootstrap or any live agent processes.
    fd = os.open(root / "install.lock", os.O_WRONLY | os.O_CREAT | os.O_NOFOLLOW | os.O_NONBLOCK, 0o600)
    with os.fdopen(fd, "wb") as lock:
        require(stat.S_ISREG(os.fstat(lock.fileno()).st_mode), "STATE")
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise UpdateFailure("BUSY") from error
        active = root / "current"
        require(active.is_symlink() or not active.exists(), "STATE")
        with tempfile.TemporaryDirectory(prefix=".stage-", dir=root) as temp:
            stage = Path(temp)
            version, url, digest, unpacked = release(curl, stage, agent, expected)
            previous = resolved_cli(agent, home, path)
            if previous:
                current = probe(previous, stage, agent, node)
                require(version_key(current) <= version_key(version), "VERSION_CHANGED")
                if current == version:
                    status(version, version, "installed")
                    return
            target = root / version
            if not target.exists() and not target.is_symlink():
                archive = stage / "payload.tgz"
                download(curl, url, archive, MAX_ARCHIVE, 360)
                actual = hashlib.sha512()
                with archive.open("rb") as stream:
                    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                        actual.update(chunk)
                require(hmac.compare_digest(actual.digest(), digest), "PACKAGE")
                runtime = stage / "runtime"
                if agent == "antigravity":
                    extract_antigravity(archive, runtime)
                else:
                    extract(archive, runtime, unpacked)
                verify_runtime(runtime / BINARIES[agent], stage, agent, node, version)
                (runtime / ".archive-sha512").write_text(digest.hex() + "\n", encoding="ascii")
                runtime.rename(target)
            else:
                receipt = target / ".archive-sha512"
                require(target.is_dir() and not target.is_symlink()
                        and receipt.is_file() and not receipt.is_symlink()
                        and receipt.stat().st_size == 129
                        and receipt.read_text(encoding="ascii") == digest.hex() + "\n", "STATE")
                verify_runtime(target / BINARIES[agent], stage, agent, node, version)
            link = stage / "active"
            link.symlink_to(version)
            link.replace(active)
            status(version, version, "installed")


def main(argv):
    require(os.uname().machine == "aarch64" and os.environ.get("JOBS_AGENT_UBUNTU") == "ubuntu-ai",
            "UNSUPPORTED_ENVIRONMENT")
    require(len(argv) in (2, 3) and argv[1] in BINARIES
            and (argv[0] == "check" and len(argv) == 2
                 or argv[0] == "install" and len(argv) == 3 and stable(argv[2])), "STATE")
    operation, agent = argv[:2]
    home = Path(os.environ["HOME"]).resolve(strict=True)
    path = os.environ.get("PATH", SYSTEM_PATH)
    curl = shutil.which("curl", path=path)
    require(curl is not None and Path(curl).is_absolute(), "DEPENDENCIES")
    root = home / ".local/share/termux-jobs/cli-updates" / agent
    tools = home / ".local/share/termux-jobs/agent-tools"
    private_node = tools / "node-v22.22.1-linux-arm64/bin/node"
    local_node = shutil.which("node", path=path)
    node = Path(local_node) if local_node and Path(local_node).is_absolute() else None
    if node is None and private_node.is_file() and os.access(private_node, os.X_OK):
        node = private_node
    os.umask(0o077)
    if operation == "install":
        install(curl, root, agent, argv[2], home, path, node)
        return
    with tempfile.TemporaryDirectory(prefix="jobs-cli-check-", dir="/tmp") as temp:
        stage = Path(temp)
        latest, _, _, _ = release(curl, stage, agent, None)
        binary = resolved_cli(agent, home, path)
        current = probe(binary, stage, agent, node) if binary else "none"
        available = current == "none" or version_key(current) < version_key(latest)
        status(current, latest, "available" if available else "current")


if __name__ == "__main__":
    try:
        main(sys.argv[1:])
    except UpdateFailure as failure:
        print(f"ERROR:{failure}")
        sys.exit(1)
    except (tarfile.TarError, EOFError, UnicodeError):
        print("ERROR:PACKAGE")
        sys.exit(1)
    except (OSError, KeyError, ValueError, subprocess.SubprocessError):
        print("ERROR:INSTALL")
        sys.exit(1)
